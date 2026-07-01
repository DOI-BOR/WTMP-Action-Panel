package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import hec.heclib.util.HecTime;                                             // Provides HecTime for converting HEC integer time values to formatted date strings and back
import hec.io.TimeSeriesContainer;                                          // Provides TimeSeriesContainer, the HEC data structure holding time and value arrays for a single record

import rma.swing.table.RmaTableModel;                                       // Provides RmaTableModel, the RMA base table model whose row and column lifecycle this class extends
import rma.util.RMAConst;                                                   // Provides RMAConst for the HEC_UNDEFINED_DOUBLE and HEC_UNDEFINED_INT sentinel values

import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Provides ForecastSimGroup for accessing the analysis period run time window during data load
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;    // Provides TemperatureTargetSet, the model object whose time-series data populates this table model

import java.time.LocalDate;                                                 // Provides LocalDate for parsing ISO date strings entered in the Date column
import java.time.LocalTime;                                                 // Provides LocalTime for constructing a midnight-plus-one-minute time when converting dates to HEC time
import java.time.ZoneId;                                                    // Provides ZoneId for obtaining the system default time zone during date-to-HecTime conversion
import java.time.ZonedDateTime;                                             // Provides ZonedDateTime for combining a LocalDate and LocalTime into a zoned instant for HecTime

import java.util.ArrayList;                                                 // Provides ArrayList for the mutable backing list of TempTargetRowData objects
import java.util.List;                                                      // Provides the List interface for the typed backing collection of row data

/**
 * Table model backing the temperature target time-series table in TempTargetPanel.
 * Each row represents one time step in the analysis window and is stored as a
 * TempTargetRowData object. Columns are dynamic: column 0 is always the Date column
 * and columns 1..N each correspond to one temperature target time series within the
 * selected TemperatureTargetSet.
 *
 * The model assumes that all temperature target time series within a given set share
 * the same time stamps. Row data is initialised from the first time series in the set
 * and subsequent series fill in their values by matching on HEC integer time.
 *
 * Temperature values are displayed rounded to one decimal place; undefined values
 * (RMAConst.HEC_UNDEFINED_DOUBLE) are rendered as null (blank). Date cells are
 * formatted using HecTime with the YEAR_DATE_MONTH_STYLE constant and are parsed
 * back via TempTargetRowData.DATE_FORMATTER on edit.
 *
 * This class is package-private and final; it is not intended for use outside the
 * temperature target UI package or for subclassing.
 *
 * @see TempTargetRowData
 * @see TempTargetPanel
 * @see TemperatureTargetSet
 */
final class TempTargetTableModel extends RmaTableModel {
    /**
     * Zero-based column index of the Date column; always the first column in the table.
     */
    static final int DATE_COL_INDEX = 0;

    /**
     * HecTime date formatting style constant that produces dates in the
     * "DDMMMYYYY" format (e.g. "01Jan2023"). Negative values select non-standard
     * HecTime format styles; -13 corresponds to the year-date-month style.
     */
    private static final int YEAR_DATE_MONTH_STYLE = -13;

    /**
     * Ordered list of TempTargetRowData objects, one per time step. Row indices in
     * this list correspond directly to row indices in the table.
     */
    private final List<TempTargetRowData> _rowDataList = new ArrayList<>();

    /**
     * Shared HecTime instance used for date-string formatting and HEC integer time
     * conversion. Reused across calls to avoid repeated object allocation.
     */
    HecTime _hecTime = new HecTime();

    /**
     * Clears all row data from both the superclass model and the backing row list.
     * Called when the table needs to be fully reset before loading new data.
     */
    @Override
    public void clearAll() {
        // Clear the superclass cell data before clearing the typed backing list
        super.clearAll();
        _rowDataList.clear();
    }

    /**
     * Returns the TempTargetRowData for the given row index, providing direct access
     * to the HEC time value and per-column temperature values for that row.
     *
     * @param row the zero-based row index to retrieve
     * @return the TempTargetRowData at the specified row index
     */
    public TempTargetRowData getTempTargetRowData(int row) {
        return _rowDataList.get(row);
    }

    /**
     * Returns the display value for the given cell. For the Date column, returns the
     * date formatted as a string using the YEAR_DATE_MONTH_STYLE HecTime format.
     * For temperature columns, returns the value rounded to one decimal place, or null
     * when the value is missing (HEC_UNDEFINED_DOUBLE) or absent.
     * Returns null for any out-of-bounds row or column index.
     *
     * @param row the zero-based row index
     * @param col the zero-based column index
     * @return the formatted cell value, or null for missing or out-of-bounds cells
     */
    @Override
    public Object getValueAt(int row, int col) {
        Object retVal;

        // Guard against out-of-bounds indices before accessing the backing list
        if (row < 0 || col < 0 || row >= _rowDataList.size() || col >= getColumnCount()) {
            return null;
        }

        TempTargetRowData rowData = _rowDataList.get(row);

        if (col == DATE_COL_INDEX) {
            // Format the HEC integer time as a human-readable date string
            retVal = getNormalizedDisplayDateForTime(rowData.getTime());

        } else {
            retVal = null;
            Double val = rowData.getValueForTempTargetColumn(col);

            // Only display the value when it is non-null and not the HEC undefined sentinel
            if (val != null && val != RMAConst.HEC_UNDEFINED_DOUBLE) {
                retVal = roundToNDigits(val, 1);
            }
        }

        return retVal;
    }

    /**
     * Converts a HEC integer time value to a formatted date string using the
     * YEAR_DATE_MONTH_STYLE constant. The shared _hecTime instance is set to the
     * given time before formatting.
     *
     * @param time the HEC integer time value to format
     * @return a date string in the YEAR_DATE_MONTH_STYLE format (e.g. "01Jan2023")
     */
    private String getNormalizedDisplayDateForTime(int time) {
        // Set the shared HecTime instance to the given integer time before formatting
        _hecTime.set(time);
        return _hecTime.date(YEAR_DATE_MONTH_STYLE);
    }

    /**
     * Rounds a double value to the specified number of decimal places using
     * multiply-round-divide arithmetic.
     *
     * @param input the double value to round
     * @param n     the number of decimal places to retain
     * @return the value rounded to n decimal places
     */
    private double roundToNDigits(double input, int n) {
        double powerOf10 = Math.pow(10, n);
        return Math.round(input * powerOf10) / powerOf10;
    }

    /**
     * Sets the value of the given cell. For the Date column, parses the string value
     * into a LocalDate, combines it with a one-minute-past-midnight LocalTime in the
     * system default time zone, converts the result to a HEC integer time, and stores
     * it on the row data. For temperature columns, parses the value as a Double (using
     * HEC_UNDEFINED_DOUBLE for null) and stores it in the row's column-to-value map.
     * Silently returns for any out-of-bounds row or column index.
     *
     * @param aValue the new cell value; may be null for temperature columns
     * @param row    the zero-based row index of the cell to update
     * @param col    the zero-based column index of the cell to update
     */
    @Override
    public void setValueAt(Object aValue, int row, int col) {
        // Guard against out-of-bounds indices before modifying any row data
        if (row < 0 || col < 0 || row >= _rowDataList.size() || col >= getColumnCount()) {
            return;
        }

        TempTargetRowData rowData = _rowDataList.get(row);

        if (col == DATE_COL_INDEX) {
            // Default to the HEC undefined integer when the input is null or blank
            int time = RMAConst.HEC_UNDEFINED_INT;

            if (aValue != null && !aValue.toString().isEmpty()) {
                // Parse the ISO date string and construct a ZonedDateTime at 00:01 local time
                LocalDate localDate = parseLocalDateString(aValue.toString());
                _hecTime.set(aValue.toString());
                LocalTime localTime = LocalTime.of(0, 1);
                ZoneId zoneId = ZoneId.systemDefault();

                // Convert the ZonedDateTime to a HEC integer time value
                time = HecTime.fromZonedDateTime(ZonedDateTime.of(localDate, localTime, zoneId)).value();
            }

            rowData.setTime(time);

        } else {
            // Treat null temperature input as the HEC undefined double sentinel
            if (aValue == null) {
                aValue = RMAConst.HEC_UNDEFINED_DOUBLE;
            }

            rowData.setValueForTempTargetColumn(col, parseDouble(aValue.toString()));
        }
    }

    /**
     * Returns the number of rows currently in the model, equal to the number of
     * TempTargetRowData entries in the backing list.
     *
     * @return the current row count; zero when the model has not been populated
     */
    @Override
    public int getRowCount() {
        return _rowDataList.size();
    }

    /**
     * Parses an ISO date string (yyyy-MM-dd) into a LocalDate using the shared
     * TempTargetRowData.DATE_FORMATTER. Returns null when the input is blank.
     *
     * @param dateString the date string to parse; must conform to ISO_DATE format
     * @return the parsed LocalDate, or null if the input is blank
     */
    private LocalDate parseLocalDateString(String dateString) {
        LocalDate retVal = null;

        if (!dateString.trim().isEmpty()) {
            retVal = LocalDate.parse(dateString, TempTargetRowData.DATE_FORMATTER);
        }

        return retVal;
    }

    /**
     * Parses an object into a Double. Returns the object directly when it is already
     * a Double, parses it from its string representation when non-null and non-blank,
     * and returns null otherwise.
     *
     * @param obj the value to parse; may be a Double, a numeric string, or null
     * @return the parsed Double value, or null if the input is null or blank
     */
    private Double parseDouble(Object obj) {
        Double retVal = null;

        if (obj instanceof Double) {
            // Avoid unnecessary string conversion when the value is already a Double
            retVal = (Double) obj;

        } else if (obj != null && !obj.toString().trim().isEmpty()) {
            retVal = Double.parseDouble(obj.toString());
        }

        return retVal;
    }

    /**
     * Populates the table model with row data derived from the given TemperatureTargetSet.
     * Clears any existing rows, initialises one TempTargetRowData per time step using
     * the first time series' times array, then iterates over all time series to fill in
     * per-column temperature values by matching each time stamp to its row.
     * <p>
     * This method assumes all time series within the set share the same time stamps.
     * If that assumption does not hold, an efficient time-lookup algorithm would be needed.
     *
     * @param tempTargetSet the TemperatureTargetSet whose time-series data populates the model
     * @param fsg           the ForecastSimGroup providing the analysis period run time window
     */
    void setTempTargetSet(TemperatureTargetSet tempTargetSet, ForecastSimGroup fsg) {
        // Remove all existing row data before loading the new set
        _rowDataList.clear();

        List<TimeSeriesContainer> tempTargets = tempTargetSet.getTimeSeriesData(fsg.getAnalysisPeriod().getRunTimeWindow());

        int column = 1;

        // Create one TempTargetRowData per time step, using the first time series as the template
        // note this is assuming temp targets time series are all using the same times for a given set
        // if that is not a good assumption will need to use an efficient algo for determining if a row has been created for a given time
        initializeRowsWithTimes(tempTargets);

        for (TimeSeriesContainer tempTargetTimeSeries : tempTargets) {
            int columnForTimeSeries = column;

            // Fill in row values for each temperature target in its corresponding column
            if (tempTargetTimeSeries != null && tempTargetTimeSeries.times != null) {
                for (int i = 0; i < tempTargetTimeSeries.times.length; i++) {
                    int time = tempTargetTimeSeries.times[i];
                    Double value = tempTargetTimeSeries.values[i];

                    // Locate the pre-created row for this time stamp and store the column value
                    TempTargetRowData foundRowData = findRowDataByTime(time);
                    if (foundRowData != null) {
                        foundRowData.setValueForTempTargetColumn(columnForTimeSeries, value);
                    }
                }

                column++;
            }
        }
    }

    /**
     * Initialises the backing row list by creating one TempTargetRowData per time
     * step found in the first TimeSeriesContainer. Does nothing when the list is empty
     * or the first container has no times array.
     *
     * @param tempTargets the list of TimeSeriesContainers; only the first is used for timing
     */
    private void initializeRowsWithTimes(List<TimeSeriesContainer> tempTargets) {
        if (!tempTargets.isEmpty()) {
            // Use the first time series as the authoritative source of time stamps
            TimeSeriesContainer tempTargetTimeSeries = tempTargets.get(0);

            if (tempTargetTimeSeries.times != null) {
                // Create one empty row entry per time step in the first time series
                for (int time : tempTargetTimeSeries.times) {
                    _rowDataList.add(new TempTargetRowData(time));
                }
            }
        }
    }

    /**
     * Performs a linear search through the backing row list to find the
     * TempTargetRowData whose HEC integer time matches the given value.
     * Returns the first match found, or null if no row has that time.
     *
     * @param time the HEC integer time value to search for
     * @return the matching TempTargetRowData, or null if not found
     */
    private TempTargetRowData findRowDataByTime(int time) {
        TempTargetRowData retVal = null;

        for (TempTargetRowData rowData : _rowDataList) {
            if (rowData.getTime() == time) {
                retVal = rowData;
                break;
            }
        }

        return retVal;
    }
}
