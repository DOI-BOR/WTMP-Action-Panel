package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;  // Resizable-array List for accumulating year integers, invalid-year DSS IDs, and errors
import java.util.HashMap;    // Hash map for the year-to-missing-DSS-IDs lookup table
import java.util.List;       // Ordered collection interface for DSS entry, year, and error message lists
import java.util.Map;        // Map interface for the quarter averages and invalid-years result maps
import java.util.Set;        // Set interface (imported for potential future use)

import com.google.common.flogger.FluentLogger; // Google Flogger for structured fine/info-level diagnostic logging

import com.rma.io.DssFileManager;     // DSS file manager interface (imported for reference)
import com.rma.io.DssFileManagerImpl; // Singleton DSS file manager used to read time series and get time ranges

import hec.heclib.util.HecTime;                     // HEC time object for setting start/end of the annual read window
import hec.hecmath.computation.ComputationException; // Exception thrown when the missing-values count calculation fails
import hec.hecmath.functions.TimeSeriesFunctions;   // HEC utility for counting missing values in a time series
import hec.io.DSSIdentifier;                        // Encapsulates a DSS file name and path for identifying a record
import hec.io.TimeSeriesContainer;                  // Container holding a read time series including values and units

import org.jfree.data.time.TimeSeries; // JFreeChart time series (imported but not referenced in this class)

import rma.util.RMAConst; // RMA constants; used to check whether a value is a valid (non-missing) data point

/**
 * Validates whether a list of meteorological DSS time-series records contain
 * complete, gap-free data across all calendar years in the records' time range.
 *
 * On construction, the validator reads the time range of the first DSS entry to
 * determine the start and end year. For each year in that range it checks every
 * DSS entry for the presence of non-missing data via hasYear(). Years for which
 * one or more entries have missing or absent data are recorded in _invalidYears.
 * All diagnostic messages are accumulated in _errors for caller inspection.
 *
 * In addition to validation, provides methods to compute seasonal averages
 * (spring, summer, fall) from the first DSS entry over the identified year range.
 * The seasonal averages use the configurable getQuarterAverages() method so callers
 * can request any contiguous month range.
 *
 * The units string of the first successfully read time series is stored in _dssUnits
 * for display in the UI.
 */
public class MetDataDssValidator {
	// Logger for fine-level data-check diagnostics and info-level read failures
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	// The list of calendar years spanning the DSS records' time range; populated by getYears() at construction
	private final List<Integer> _years;

	// The DSS entries to validate; each entry identifies a meteorological time-series record
	private List<DSSIdentifier> _dssEntries;

	// Maps each calendar year to the list of DSS identifier strings that are missing data for that year
	private HashMap<Integer, List<String>> _invalidYears;

	// Reusable HecTime for the start of each annual read window; initialized to a safe sentinel date
	private HecTime _startTime = new HecTime("01Jan1900 0000");

	// Reusable HecTime for the end of each annual read window; initialized to a safe sentinel date
	private HecTime _endTime = new HecTime("31Dec1900 2400");

	// Units string from the first successfully read time series; used for display in the UI
	private String _dssUnits = "";

	// Accumulates all diagnostic and error messages encountered during validation and averaging
	private List<String> _errors = new ArrayList<>();

	/**
	 * Constructs a MetDataDssValidator for the given list of DSS entries and
	 * immediately determines the year range and validates data coverage.
	 *
	 * The _years field is populated (or left null if no entries or no time range)
	 * during construction via getYears(). Any validation errors encountered are
	 * recorded in _errors and the invalid-years map.
	 *
	 * @param dssEntries the List of DSSIdentifier objects identifying the met time-series records to validate
	 */
	public MetDataDssValidator(List<DSSIdentifier> dssEntries) {
		super();
		_dssEntries = dssEntries;

		// Determine the year range and validate data coverage immediately at construction
		_years = getYears();
	}

	/**
	 * Returns the list of calendar years spanning the time range of the DSS records.
	 *
	 * If the year list has already been computed (_years is non-null), returns it directly.
	 * Returns null if the entry list is empty or if no time range can be read from the
	 * first entry. For each year in the range, validates data coverage for all entries
	 * via checkYearForData() and populates _invalidYears with any gaps found.
	 *
	 * The units string (_dssUnits) is captured from the first year's first successful read.
	 *
	 * @return the List of Integer year values spanning the records' time range; null if unavailable
	 */
	public List<Integer> getYears() {
		// Return the cached year list if it has already been computed
		if (_years != null) {
			return _years;
		}

		// Cannot determine a year range with no DSS entries
		if (_dssEntries.size() == 0) {
			return null;
		}

		// Use the first entry to determine the overall time range of the met data
		DSSIdentifier dssId = _dssEntries.get(0);
		HecTime[] timeRange = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dssId, 0);
		if (timeRange == null) {
			// Log and record that no time range could be found for the first entry
			LOGGER.atInfo().log("No TimeRange found for Time Series %s", dssId);
			_errors.add("No Time Window found to determine years for Time Series " + dssId);
			return null;
		}

		int startYear = timeRange[0].year();
		int endYear = timeRange[1].year();

		// Pre-size the year list based on the known range
		List<Integer> years = new ArrayList<>(endYear - startYear + 1);
		int dssSize = _dssEntries.size();
		DSSIdentifier dssId2;

		// Initialize the invalid-years tracking map before iterating
		_invalidYears = new HashMap<>();

		for (int y = startYear; y <= endYear; y++) {
			// Check the first entry for this year; capture units on the first year only
			checkYearForData(dssId, y, y == startYear);

			// Record this year in the complete year list
			years.add(y);

			// Check all remaining entries for the same year
			for (int d = 0; d < dssSize; d++) {
				dssId2 = _dssEntries.get(d);
				checkYearForData(dssId2, y, false);
			}
		}
		return years;
	}


	/**
	 * Validates that data exists for a given year within the specified DSS time series.
	 * If the year is missing data, the DSS identifier is recorded in _invalidYears and
	 * an error message is added to _errors. A new list is created for the year the first
	 * time a missing entry is detected.
	 *
	 * @param dssId2   the DSS identifier representing the time series being checked
	 * @param y        the calendar year to validate data for
	 * @param setUnits flag indicating whether units should be set during the year check
	 */
	private void checkYearForData(DSSIdentifier dssId2, int y, boolean setUnits) {
		// Check if data exists for the given year in this DSS time series
		if (!hasYear(dssId2, y, setUnits)) {

			// Look up whether any missing entries have already been recorded for this year
			List<String> ids = _invalidYears.get(y);

			if (ids == null) {
				// First missing entry for this year; create the list and record an error message
				ids = new ArrayList<>();
				_invalidYears.put(y, ids);
				_errors.add("Found Missing data for the year " + y + " for Time Series:" + dssId2);
			}

			// Add the DSS identifier to the list of entries missing data for this year
			ids.add(dssId2.toString());
		}
	}

	/**
	 * Checks whether the given DSS entry has complete, non-missing data for the given year.
	 *
	 * Sets the time window on the DSS identifier to Jan 1 through Dec 31 of the specified
	 * year and reads the time series. Returns false if the read fails or the series contains
	 * any missing values. Captures the units string from the result when setUnits is true.
	 *
	 * @param dssId    the DSS entry to read
	 * @param year     the calendar year to check
	 * @param setUnits true if the units string should be captured from this read result
	 * @return true if the entry has complete data for the year; false if data is absent or contains gaps
	 */
	private boolean hasYear(DSSIdentifier dssId, int year, boolean setUnits) {
		// Set the time window to cover the entire calendar year
		_startTime.setYearMonthDay(year, 1, 1);
		_startTime.setTime("0001");
		_endTime.setYearMonthDay(year, 12, 31);
		dssId.setTimeWindow(_startTime, _endTime);

		LOGGER.atFine().log("Check for data for %s for year %d", dssId, year);

		// Attempt to read the time series for this year
		TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, false);
		if (tsc == null || tsc.numberValues == 0) {
			// Read failed or returned no values; record the failure
			LOGGER.atInfo().log("failed to read Time Series %s to determine years", dssId);
			_errors.add("Failed to read Time Series " + dssId + " to determine years");
			return false;
		}

		// Capture the units string from the first successfully read entry
		if (setUnits) {
			_dssUnits = tsc.units;
		}

		// Count any missing values in the read time series
		int missing = 0;
		try {
			missing = TimeSeriesFunctions.numberMissingValues(tsc);
		} catch (ComputationException e) {
			// Log and record the error; treat the entry as having missing data
			LOGGER.atInfo().log("error %s checking for missing values in %s ", e.getMessage(), dssId);
			_errors.add("Error checking for valid data in " + dssId + ". Error:" + e.getMessage());
		}

		// Any missing values means the year is incomplete
		if (missing > 0) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the map of years that have one or more DSS entries with missing data.
	 *
	 * Keys are calendar year integers. Values are lists of DSS identifier strings
	 * identifying the entries that are missing data for that year.
	 *
	 * @return the map of invalid years to their missing DSS entry identifier strings
	 */
	public Map<Integer, List<String>> getInvalidYears() {
		return _invalidYears;
	}

	/**
	 * Returns a map of spring (March through May) daily-average values per year,
	 * computed from the first DSS entry.
	 *
	 * @return a Map from calendar year integer to the mean value for March 1 through May 31
	 */
	public Map<Integer, Double> getSpringAverages() {
		Map<Integer, Double> springAverages = new HashMap<>();
		getQuarterAverages(springAverages, 3, 1, 5, 31);
		return springAverages;
	}

	/**
	 * Computes the average value of the first DSS entry for each year over the given
	 * month/day range and stores the results in the provided map.
	 *
	 * Reads the time series for each year using the shared _startTime and _endTime
	 * fields. Skips years where the read fails or returns no values. Only valid (non-missing)
	 * data points are included in the average calculation.
	 *
	 * Returns the map unchanged if the DSS entry list is empty or the year list is null.
	 *
	 * @param quarterAverages the Map to populate with year-to-average entries
	 * @param startMonth      the first month of the averaging window (1 = January)
	 * @param startDay        the first day of the averaging window
	 * @param endMonth        the last month of the averaging window
	 * @param endDay          the last day of the averaging window
	 * @return the populated quarterAverages map (same reference as the input)
	 */
	public Map<Integer, Double> getQuarterAverages(Map<Integer, Double> quarterAverages, int startMonth, int startDay, int endMonth, int endDay) {
		// Cannot compute averages without DSS entries or a valid year list
		if (_dssEntries.size() == 0 || _years == null) {
			return quarterAverages;
		}

		// All seasonal averages are computed from the first DSS entry only
		DSSIdentifier dssId = _dssEntries.get(0);
		int year;

		for (int y = 0; y < _years.size(); y++) {
			year = _years.get(y);

			// Set the time window to the requested month/day range for this year
			_startTime.setYearMonthDay(year, startMonth, startDay);
			_startTime.setTime("0001");
			_endTime.setYearMonthDay(year, endMonth, endDay);

			// Read the time series; pass true to condense the data to the time window
			TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, true);
			if (tsc != null && tsc.numberValues > 0) {
				// Compute and store the mean of the non-missing values for this year's window
				double avg = average(tsc);
				quarterAverages.put(year, avg);
			} else {
				// Log and record the read failure; no average is stored for this year
				LOGGER.atInfo().log("Failed to read Time Series %s", dssId);
				_errors.add("Failed to read Time Series " + dssId + " to calculate quarterly averages" + dssId);
			}
		}
		return quarterAverages;
	}

	/**
	 * Computes the arithmetic mean of all valid values in a time series container.
	 * Missing or invalid values, as determined by RMAConst.isValidValue(), are excluded
	 * from both the sum and the count to avoid skewing the result. A fine-level log
	 * message is emitted with the computed sum and count before returning.
	 *
	 * @param tsc the TimeSeriesContainer holding the values to average
	 * @return    the mean of all valid values, or NaN if no valid values are present
	 */
	private double average(TimeSeriesContainer tsc) {
		// Initialize the running sum and valid value count to zero
		double sum = 0;
		int cnt = 0;

		// Iterate over every value in the time series
		for (int i = 0; i < tsc.numberValues; i++) {
			// Only include non-missing values in the sum and count
			if (RMAConst.isValidValue(tsc.values[i])) {
				sum += tsc.values[i];
				cnt++;
			}
		}

		// Log the final sum and count at fine level for debugging purposes
		LOGGER.atFine().log("Sum %d, Count %d for %s", sum, cnt, tsc);

		// Divide the total sum by the number of valid values to get the mean
		return sum / cnt;
	}

	/**
	 * Returns a map of summer (June through August) daily-average values per year,
	 * computed from the first DSS entry.
	 *
	 * @return a Map from calendar year integer to the mean value for June 1 through August 31
	 */
	public Map<Integer, Double> getSummerAverages() {
		Map<Integer, Double> summerAverages = new HashMap<>();
		getQuarterAverages(summerAverages, 6, 1, 8, 31);
		return summerAverages;
	}

	/**
	 * Returns a map of fall (September through November) daily-average values per year,
	 * computed from the first DSS entry.
	 *
	 * @return a Map from calendar year integer to the mean value for September 1 through November 30
	 */
	public Map<Integer, Double> getFallAverages() {
		Map<Integer, Double> fallAverages = new HashMap<>();
		getQuarterAverages(fallAverages, 9, 1, 11, 30);
		return fallAverages;
	}

	/**
	 * Returns the units string captured from the first successfully read time series.
	 *
	 * @return the DSS units string; empty if no data has been successfully read
	 */
	public String getDssUnits() {
		return _dssUnits;
	}

	/**
	 * Returns the accumulated list of error and diagnostic messages generated during
	 * validation and averaging.
	 *
	 * @return the List of error message strings; empty if no errors were encountered
	 */
	public List<String> getErrors() {
		return _errors;
	}
}
