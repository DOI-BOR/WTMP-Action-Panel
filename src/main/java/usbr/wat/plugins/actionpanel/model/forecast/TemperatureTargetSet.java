package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.io.DssFileManagerImpl;                               // Provides DssFileManagerImpl for reading time-series records from HEC-DSS files
import com.rma.model.Project;                                       // Provides the Project class for resolving relative file paths against the current project directory
import com.rma.util.XMLUtilities;                                   // Provides XMLUtilities for saving and loading NamedType objects to and from JDOM elements

import hec.data.Parameter;                                          // Provides the Parameter class for retrieving standard unit strings by parameter ID
import hec.data.Units;                                              // Provides the Units class for unit system constants (e.g., ENGLISH_ID, SI_ID)
import hec.heclib.dss.DSSPathname;                                  // Provides DSSPathname for constructing and manipulating HEC-DSS A/B/C/D/E/F path components
import hec.heclib.dss.HecTimeSeriesBase;                            // Provides HecTimeSeriesBase for DSS time-series constants such as interval lookup from an E-part string
import hec.heclib.util.HecTime;                                     // Provides HecTime for representing and manipulating HEC integer-encoded time values
import hec.heclib.util.HecTimeArray;                                // Provides HecTimeArray for storing arrays of HecTime values on a TimeSeriesContainer
import hec.io.DSSIdentifier;                                        // Provides DSSIdentifier for specifying the file name and DSS path when reading from a DSS file
import hec.io.TimeSeriesContainer;                                  // Provides TimeSeriesContainer as the primary data structure for HEC time-series records
import hec.lang.NamedType;                                          // Provides NamedType as the base class supplying a name and integer index
import hec.model.RunTimeWindow;                                     // Provides RunTimeWindow for obtaining the start and end times of a WAT analysis run

import org.jdom.Element;                                            // Provides Element as the JDOM XML element type used for saving and loading configuration data
import rma.util.RMAConst;                                           // Provides RMAConst for HEC undefined/missing-value sentinel constants

import java.nio.file.Path;                                          // Provides Path for representing file-system paths in a platform-independent way
import java.nio.file.Paths;                                         // Provides Paths for constructing Path instances from string representations
import java.time.Duration;                                          // Provides Duration for calculating elapsed time between two temporal objects
import java.time.LocalDate;                                         // Provides LocalDate for representing a calendar date without time or timezone
import java.time.LocalDateTime;                                     // Provides LocalDateTime for representing a date and time without a timezone
import java.time.LocalTime;                                         // Provides LocalTime for representing a time-of-day without a date or timezone
import java.time.ZoneId;                                            // Provides ZoneId for identifying a time-zone such as UTC
import java.time.ZonedDateTime;                                     // Provides ZonedDateTime for a date-time with a full timezone context
import java.time.temporal.ChronoUnit;                               // Provides ChronoUnit for date-based unit arithmetic (e.g., counting weeks between two dates)

import java.util.ArrayList;                                         // Provides ArrayList as the resizable-array implementation of List used throughout this class
import java.util.HashMap;                                           // Provides HashMap for mapping integer time keys to double values during data alignment
import java.util.List;                                              // Provides the List interface for ordered collections of time-series and DSS pathname objects
import java.util.Map;                                               // Provides the Map interface for key-value associations
import java.util.Objects;                                           // Provides Objects for null-safe hash code generation
import java.util.stream.Collectors;                                 // Provides Collectors for terminal stream operations such as collecting elements into a List
import java.util.stream.IntStream;                                  // Provides IntStream for generating integer ranges used when building missing-value arrays

/**
 * Represents a named set of water-temperature target time series associated with a
 * specific river location within the WTMP forecast action panel.
 *
 * A {@code TemperatureTargetSet} extends {@link NamedType} to carry a display name and
 * integer index, and additionally manages one or more {@link TimeSeriesContainer} records
 * read from or built against HEC-DSS files. It supports two operational modes:
 *
 *
 *   DSS-backed — time-series data is read from a source DSS file using stored
 *       {@link DSSPathname} references and then shifted and trimmed to align with the
 *       analysis run-time window.
 *   User-defined — synthetic weekly time-series containers pre-filled with
 *       missing-value sentinels are generated from a fixed grid, and any previously saved
 *       values are overlaid from the source DSS file when available.
 *
 * Instances can be persisted to and restored from JDOM XML elements via
 * {@link #saveData(Element)} and {@link #loadData(Element)}.
 *
 * This class is declared {@code final} to prevent subclassing.
 *
 * @see NamedType
 * @see TimeSeriesContainer
 * @see DSSPathname
 * @see RiverLocation
 */
public final class TemperatureTargetSet extends NamedType {
    // XML element name for the child element that stores the output DSS file path
    private static final String OUTPUT_DSS_FILE_ELEM_ID = "Output-DSS-File";

    // XML attribute name that flags whether this target set was defined by the user
    private static final String USER_DEFINED_ATTRIBUTE_NAME = "user-defined";

    // XML element name for the child element that stores the source DSS file path
    private static final String FILE_PATH_ELEM_NAME = "file-path";

    // XML element name for the container element that holds all DSS pathname child elements
    private static final String PATH_NAMES_ELEM_NAME = "dss-pathnames";

    // XML element name for each individual DSS pathname child element
    private static final String PATH_NAME_ELEM_NAME = "dss-pathname";

    // XML element name for the child element that stores the associated river location
    private static final String RIVER_LOC_ELEM_NAME = "River-Location";

    // In-memory cache of loaded time-series containers; populated on demand by loadTimeSeriesData
    private final List<TimeSeriesContainer> _timeSeriesData = new ArrayList<>();

    // Ordered list of HEC-DSS pathnames identifying each target record within the source DSS file
    private final List<DSSPathname> _dssPathNames = new ArrayList<>();

    // Flag indicating whether this target set was manually entered by the user rather than loaded from DSS
    private boolean _isUserDefined;

    // File-system path to the HEC-DSS file from which source time-series data is read
    private Path _dssSourcePath;

    // Number of user-defined temperature target columns to generate when no source DSS file is present
    private int _numberOfUserDefinedTempTargets;

    // File-system path to the HEC-DSS file to which computed output data is written
    private Path _dssOutputPath;

    // The river location (station) that this temperature target set applies to
    private RiverLocation _riverLocation;

    // Unit string for temperature data; defaults to the English-system unit for the TEMP parameter
    private String _units = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.ENGLISH_ID);

    /**
     * Default no-argument constructor that delegates to the {@link NamedType} superclass.
     * Required for framework-level instantiation (e.g., XML loading).
     */
    public TemperatureTargetSet() {
        super();
    }

    /**
     * Serialises this {@code TemperatureTargetSet} to a JDOM XML element and appends it
     * to the supplied parent element.
     *
     * The following child elements are written:
     *
     *   Name and index via {@code XMLUtilities.saveNamedType}
     *   {@code user-defined} attribute
     *   Source DSS file path ({@code file-path})
     *   Output DSS file path ({@code Output-DSS-File})
     *   Associated river location ({@code River-Location})
     *   All DSS pathnames ({@code dss-pathnames} / {@code dss-pathname})
     *
     * @param parent the JDOM {@link Element} to which the serialised element is added;
     *               must not be {@code null}
     * @return {@code true} always, indicating the save completed without error
     */
    public boolean saveData(Element parent) {
        // Create the root XML element for this target set
        Element myElem = new Element("TemperatureTargetSet");

        // Save the inherited name and index fields to the element
        XMLUtilities.saveNamedType(myElem, this);

        // Record whether this set was user-defined as an XML attribute
        myElem.setAttribute(USER_DEFINED_ATTRIBUTE_NAME, String.valueOf(_isUserDefined));

        // Build and populate the source DSS file path element
        Element filePathElem = new Element(FILE_PATH_ELEM_NAME);
        if (_dssSourcePath != null) {
            filePathElem.setText(_dssSourcePath.toString());
        }
        myElem.addContent(filePathElem);

        // Build and populate the output DSS file path element
        Element dssOutputPathElem = new Element(OUTPUT_DSS_FILE_ELEM_ID);
        if (_dssOutputPath != null) {
            dssOutputPathElem.setText(_dssOutputPath.toString());
        }
        myElem.addContent(dssOutputPathElem);

        // Build and populate the river location element, only if a valid location is present
        Element riverLocationElement = new Element(RIVER_LOC_ELEM_NAME);
        if (_riverLocation != null && _riverLocation.getName() != null) {
            XMLUtilities.saveNamedType(riverLocationElement, _riverLocation);
        }
        myElem.addContent(riverLocationElement);

        // Determine which pathnames to persist based on whether this is user-defined
        Element dssPathnamesElem = new Element(PATH_NAMES_ELEM_NAME);
        List<DSSPathname> pathnames;
        if (isUserDefined()) {
            // User-defined sets are always saved with the weekly time step in the E-part
            pathnames = getDssPathNames(TemperatureTargetTimeStep.REGULAR_WEEKLY);
        } else {
            // DSS-backed sets persist pathnames with their stored E-part unchanged
            pathnames = getDssPathNames();
        }

        // Write each pathname string as a child element of the dss-pathnames container
        for (DSSPathname pathname : pathnames) {
            Element dssPathnameElem = new Element(PATH_NAME_ELEM_NAME);
            dssPathnameElem.setText(pathname.getPathname());
            dssPathnamesElem.addContent(dssPathnameElem);
        }
        myElem.addContent(dssPathnamesElem);

        // Attach the fully built element to the caller-supplied parent
        parent.addContent(myElem);

        return true;
    }

    /**
     * Restores this {@code TemperatureTargetSet} from a JDOM XML element previously
     * written by {@link #saveData(Element)}.
     *
     * Fields populated:
     *
     *   Name and index via {@code XMLUtilities.loadNamedType}
     *   {@code _isUserDefined} from the {@code user-defined} attribute
     *   {@code _dssSourcePath} from the {@code file-path} child element
     *   {@code _dssOutputPath} from the {@code Output-DSS-File} child element
     *   {@code _riverLocation} from the {@code River-Location} child element
     *   {@code _dssPathNames} from each {@code dss-pathname} child element
     *
     * Sets the inherited {@code _modified} flag to {@code true} upon completion to
     * indicate that cached time-series data should be reloaded.
     *
     * @param myElem the JDOM {@link Element} containing the persisted state; must not be {@code null}
     * @return {@code true} always, indicating the load completed without error
     */
    public boolean loadData(Element myElem) {
        // Restore the inherited name and index fields from the element
        XMLUtilities.loadNamedType(myElem, this);

        // Restore the user-defined flag from the XML attribute
        _isUserDefined = Boolean.parseBoolean(myElem.getAttribute(USER_DEFINED_ATTRIBUTE_NAME).getValue());

        // Reset the source path before attempting to read it from XML
        _dssSourcePath = null;
        Element filePathElem = myElem.getChild(FILE_PATH_ELEM_NAME);
        if (filePathElem != null && filePathElem.getText() != null) {
            // Convert the stored string to a Path object
            _dssSourcePath = Paths.get(filePathElem.getText());
        }

        // Restore the output DSS file path if the element and its text are present
        Element outputDssFileElem = myElem.getChild(OUTPUT_DSS_FILE_ELEM_ID);
        if (outputDssFileElem != null) {
            String filePath = outputDssFileElem.getText();
            if (filePath != null) {
                _dssOutputPath = Paths.get(filePath);
            }
        }

        // Restore the river location if the element exists and has child content
        Element riverLocationElem = myElem.getChild(RIVER_LOC_ELEM_NAME);
        if (riverLocationElem != null && !riverLocationElem.getChildren().isEmpty()) {
            _riverLocation = new RiverLocation();
            XMLUtilities.loadNamedType(riverLocationElem, _riverLocation);
        }

        // Restore all stored DSS pathnames from the dss-pathnames container element
        Element dssPathNamesElem = myElem.getChild(PATH_NAMES_ELEM_NAME);
        if (dssPathNamesElem != null) {
            List<?> dssPathnameChild = dssPathNamesElem.getChildren(PATH_NAME_ELEM_NAME);
            for (Object child : dssPathnameChild) {
                if (child instanceof Element) {
                    // Cast is safe because getChildren guarantees Element instances
                    Element pathnameElem = (Element) child;
                    _dssPathNames.add(new DSSPathname(pathnameElem.getText()));
                }
            }
        }

        // Mark the object as modified so cached time-series data is reloaded on next access
        _modified = true;

        return true;
    }

    /**
     * Returns a defensive copy of the time-series data list for the given run-time window,
     * loading or reloading the data from DSS if necessary.
     *
     * Data is reloaded when:
     *
     *   This set is user-defined and the {@code _modified} flag is set, or
     *   The internal cache is empty (first access or after a clear).
     *
     * @param timeWindow the {@link RunTimeWindow} defining the analysis period; used to
     *                   align and trim loaded time-series data
     * @return a new {@link List} containing the loaded {@link TimeSeriesContainer} records;
     * never {@code null} but may be empty if no data is available
     */
    public List<TimeSeriesContainer> getTimeSeriesData(RunTimeWindow timeWindow) {
        // Reload if the user-defined data has changed, or if the cache has never been filled
        if ((_isUserDefined && _modified) || _timeSeriesData.isEmpty()) {
            loadTimeSeriesData(timeWindow);
        }

        // Return a defensive copy to prevent external mutation of the internal cache
        return new ArrayList<>(_timeSeriesData);
    }

    /**
     * Trims the leading time steps from each time series in _timeSeriesData based on the
     * start year of the provided run time window. Data before a year-specific threshold
     * date is discarded to work around irregular DSS write behavior at the start of a run.
     * The trim threshold defaults to January 2nd but is overridden for known problem years
     * (2010 and 2020) that require additional trimming. After trimming, each
     * TimeSeriesContainer's arrays and metadata fields are updated to reflect the new range.
     *
     * @param timeWindow the run time window whose start date determines the trim threshold
     */
    private void trimStartDate(RunTimeWindow timeWindow) {
        // Extract the start year from the run time window to determine the trim threshold
        int startYear = timeWindow.getStartTime().year();

        // Default trim threshold: keep data from January 2nd onward
        // This is a workaround to odd DSS write behavior
        int dayOfMonthToTrimTo = 2;

        // Override for year 2010, which requires an extra day of trim
        if (startYear == 2010) {
            dayOfMonthToTrimTo = 3;
        }

        // Override for year 2020, which requires a larger trim
        if (startYear == 2020) {
            dayOfMonthToTrimTo = 5;
        }

        // Process each time series container stored in _timeSeriesData
        for (TimeSeriesContainer tsc : _timeSeriesData) {
            // Accumulate only the time steps and values that fall within the valid window
            List<Integer> newTimes = new ArrayList<>();
            List<Double> newValues = new ArrayList<>();

            // Iterate over every time step in the current time series
            for (int i = 0; i < tsc.times.length; i++) {
                // Convert the HEC time at index i to a LocalDateTime for date comparison
                LocalDateTime dateTime = tsc.getHecTime(i).getLocalDateTime();

                if (!newTimes.isEmpty()) {
                    // Once the first qualifying time step has been found, include all subsequent ones
                    newValues.add(tsc.values[i]);
                    newTimes.add(tsc.times[i]);
                } else if (dateTime.getYear() > startYear
                        || (dateTime.getYear() == startYear && dateTime.getMonth().getValue() > 1)
                        || (dateTime.getYear() == startYear && dateTime.getDayOfMonth() > dayOfMonthToTrimTo)
                        || (dateTime.getYear() == startYear && dateTime.getDayOfMonth() == dayOfMonthToTrimTo && dateTime.getHour() > 1)) {
                    // This is the first time step that falls within the valid window
                    newTimes.add(tsc.times[i]);
                    newValues.add(tsc.values[i]);
                }
            }

            // Convert the filtered lists back to primitive arrays
            int[] times = convertListToIntArray(newTimes);
            double[] values = convertListToDoubleArray(newValues);

            // Replace the container's arrays and update its metadata to reflect the trim
            tsc.times = times;
            tsc.values = values;

            // Update the value count to match the trimmed array length
            tsc.numberValues = values.length;

            // Reset the start time fields to the first remaining time step
            tsc.startTime = tsc.times[0];
            tsc.startHecTime = tsc.getHecTime(0);

            // Reset the end time fields to the last remaining time step
            tsc.endTime = tsc.times[tsc.times.length - 1];
            tsc.endHecTime = tsc.getHecTime(tsc.numberValues - 1);
        }
    }

    /**
     * Converts a List of Integer objects to a primitive int array.
     * This is necessary because Java does not support direct casting between
     * a List of Integer and a primitive int array, and several DSS API methods
     * require primitive arrays rather than collections.
     *
     * @param list the List of Integer values to convert
     * @return     a primitive int array containing the same values in the same order
     */
    private int[] convertListToIntArray(List<Integer> list) {
        // Allocate a primitive int array sized to match the number of elements in the list
        int[] array = new int[list.size()];

        // Copy each Integer from the list into the array, auto-unboxing to a primitive int
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        // Return the fully populated primitive int array
        return array;
    }


    /**
     * Converts a List of Double objects to a primitive double array.
     * This is necessary because Java does not support direct casting between
     * a List of Double and a primitive double array, and several DSS API methods
     * require primitive arrays rather than collections.
     *
     * @param list the List of Double values to convert
     * @return     a primitive double array containing the same values in the same order
     */
    private double[] convertListToDoubleArray(List<Double> list) {
        // Allocate a primitive double array sized to match the number of elements in the list
        double[] array = new double[list.size()];

        // Copy each Double from the list into the array, auto-unboxing to a primitive double
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        // Return the fully populated primitive double array
        return array;
    }

    /**
     * Shifts the time arrays of all cached time-series containers so that their data
     * aligns with the analysis year defined by the supplied run-time window.
     *
     * The shift is computed as the difference in minutes between January 1st of the
     * source data's year and January 1st of the analysis year. An edge case is handled
     * where the source time series spans the year boundary (i.e., the second time step
     * falls in a different calendar year than the first), in which case the analysis
     * start is moved back by one year before computing the shift.
     *
     * After shifting, each container is trimmed to the analysis year window and its
     * metadata fields ({@code startTime}, {@code endTime}, etc.) are updated.
     *
     * @param timeWindow the {@link RunTimeWindow} defining the target analysis year
     */
    private void shiftTimeSeriesDataToAnalysisYear(RunTimeWindow timeWindow) {
        for (TimeSeriesContainer tsc : _timeSeriesData) {
            if (tsc.times.length > 1) {
                HecTime startTime = timeWindow.getStartTime();

                // Compute the second time step to detect year-boundary spanning
                HecTime computeTime = new HecTime();
                computeTime.set(tsc.times[1]);

                // Determine the January 1st anchor dates for both source and analysis years
                LocalDate sourceStart = tsc.getStartTime().getLocalDateTime().toLocalDate().withDayOfYear(1);
                LocalDate analysisStart = startTime.getLocalDateTime().toLocalDate().withDayOfYear(1);

                // If the series straddles a year boundary, shift the analysis anchor back one year
                if (tsc.getStartTime().year() == computeTime.year() - 1) {
                    analysisStart = analysisStart.minusYears(1);
                }

                // Calculate the minute offset between the two January 1st anchors
                int diffInMinutes = (int) Duration.between(sourceStart.atStartOfDay(), analysisStart.atStartOfDay()).toMinutes();

                // Apply the minute offset to all time values in this container
                applyShiftToTsc(tsc, diffInMinutes);

                int trimStartYear = timeWindow.getStartTime().year();

                if (!tsc.allMissing()) {
                    int trimEndYear = trimStartYear;

                    // Check the second-to-last time step to detect end-of-year boundary spanning
                    computeTime.set(tsc.times[tsc.times.length - 2]);
                    if (tsc.getEndTime().year() == computeTime.year() + 1) {
                        trimEndYear = computeTime.year();
                    }

                    // Trim the container to span from January 2nd of the start year to January 7th of the following year
                    tsc.trimToTime(new HecTime("02Jan" + (trimStartYear), "0100"), new HecTime("07Jan" + (trimEndYear + 1), "0100"));
                    trimEnd(tsc, new HecTime("07Jan" + (trimEndYear + 1), "0100"));
                }

                // Update the container's metadata to reflect the new time range after shifting and trimming
                tsc.startTime = tsc.times[0];
                tsc.endTime = tsc.times[tsc.times.length - 1];
                tsc.startHecTime = tsc.getHecTime(0);
                tsc.endHecTime = tsc.getHecTime(tsc.times.length - 1);
            }
        }
    }

    /**
     * Removes the final time step from a {@link TimeSeriesContainer} if its timestamp
     * falls after the specified trim boundary.
     *
     * This is used to enforce a strict upper bound on the time-series window after
     * trimming and shifting operations that may leave one extra step beyond the intended
     * end date.
     *
     * @param tsc    the {@link TimeSeriesContainer} to trim; modified in place
     * @param trimTo the {@link HecTime} upper boundary; the last step is removed if it
     *               is strictly after this time
     */
    private void trimEnd(TimeSeriesContainer tsc, HecTime trimTo) {
        HecTime end = tsc.getHecTime(tsc.times.length - 1);

        if (end.getLocalDateTime().isAfter(trimTo.getLocalDateTime())) {
            // Allocate new arrays that are one element shorter to drop the trailing step
            double[] newVals = new double[tsc.numberValues - 1];
            int[] newTimes = new int[tsc.numberValues - 1];

            // Copy all but the last element from the existing arrays
            System.arraycopy(tsc.values, 0, newVals, 0, newVals.length);
            System.arraycopy(tsc.times, 0, newTimes, 0, newTimes.length);

            // Replace the container's arrays and update its metadata
            tsc.values = newVals;
            tsc.times = newTimes;
            tsc.numberValues = newVals.length;
            tsc.startTime = tsc.times[0];
            tsc.endTime = tsc.times[tsc.times.length - 1];
            tsc.startHecTime = tsc.getHecTime(0);
            tsc.endHecTime = tsc.getHecTime(tsc.times.length - 1);
        }
    }

    /**
     * Returns whether this target set was entered manually by the user rather than
     * loaded from a HEC-DSS file.
     *
     * @return {@code true} if user-defined; {@code false} if DSS-backed
     */
    public boolean isUserDefined() {
        return _isUserDefined;
    }

    /**
     * Returns the file-system path to the source HEC-DSS file, or {@code null} if not set.
     *
     * @return the source DSS file path, or {@code null}
     */
    public Path getDssSourcePath() {
        return _dssSourcePath;
    }

    /**
     * Sets whether this target set was entered manually by the user.
     *
     * @param userDefined {@code true} to mark as user-defined; {@code false} for DSS-backed
     */
    public void setUserDefined(boolean userDefined) {
        _isUserDefined = userDefined;
    }

    /**
     * Sets the file-system path to the source HEC-DSS file from which time-series data
     * is read.
     *
     * @param filePath the path to the source DSS file; may be {@code null} to indicate
     *                 that synthetic data should be generated
     */
    public void setDssSourcePath(Path filePath) {
        _dssSourcePath = filePath;
    }

    /**
     * Replaces the list of stored HEC-DSS pathnames with the supplied list.
     *
     * The internal list is cleared first, then all non-null entries from the supplied
     * list are added. Passing {@code null} results in an empty internal list.
     *
     * @param dssPathNames the new list of {@link DSSPathname} objects; may be {@code null}
     */
    public void setDssPathNames(List<DSSPathname> dssPathNames) {
        // Clear the existing pathnames before replacing
        _dssPathNames.clear();

        if (dssPathNames != null) {
            _dssPathNames.addAll(dssPathNames);
        }
    }

    /**
     * Returns a copy of all stored HEC-DSS pathnames with their E-parts unchanged.
     *
     * Delegates to {@link #getDssPathNames(TemperatureTargetTimeStep)} with a {@code null}
     * time step to preserve the original E-part values.
     *
     * @return a new {@link List} of {@link DSSPathname} objects; never {@code null}
     */
    public List<DSSPathname> getDssPathNames() {
        return getDssPathNames(null);
    }

    /**
     * Returns a copy of all stored HEC-DSS pathnames, optionally overriding their E-part
     * with the supplied time step.
     *
     * If {@code timeStep} is {@code null}, the E-parts are left as stored. Otherwise,
     * every pathname in the returned copy has its E-part replaced with the string
     * representation of {@code timeStep}.
     *
     * @param timeStep the {@link TemperatureTargetTimeStep} to apply to each pathname's
     *                 E-part, or {@code null} to leave E-parts unchanged
     * @return a new {@link List} of {@link DSSPathname} objects reflecting the requested
     * E-part; never {@code null}
     */
    public List<DSSPathname> getDssPathNames(TemperatureTargetTimeStep timeStep) {
        // Start with a mutable copy of the stored pathnames
        List<DSSPathname> retVal = new ArrayList<>(_dssPathNames);

        if (timeStep != null) {
            // Override the E-part of every pathname with the requested time step string
            for (DSSPathname pathname : retVal) {
                pathname.setEPart(timeStep.toString());
            }
        }

        return retVal;
    }


    /**
     * Loads time series data into _timeSeriesData for the given run time window.
     * If the data source is user-defined with no DSS source file, blank synthetic
     * containers are generated for each target column. Otherwise, data is read from
     * DSS pathnames and, if all values are missing, a backwards-compatibility fallback
     * is attempted before substituting a blank synthetic container.
     * After loading, DSS-backed data is shifted to align with the analysis year if needed,
     * and leading spurious time steps are trimmed from the start of each series.
     *
     * @param timeWindow the run time window defining the start and end of the data to load
     */
    private void loadTimeSeriesData(RunTimeWindow timeWindow) {
        // Clear any previously cached time-series data before reloading
        _timeSeriesData.clear();

        if (_isUserDefined && _dssSourcePath == null) {
            // No source file: generate a blank synthetic container for each target column
            for (int i = 1; i <= _numberOfUserDefinedTempTargets; i++) {
                // Build a fixed synthetic container for user-defined target column i
                TimeSeriesContainer fixedTscForUserDefined = buildFixedDataForUserDefined(i, timeWindow);
                _timeSeriesData.add(fixedTscForUserDefined);
            }

        } else {
            // Index used to track the position of each pathname during DSS loading
            int i = 1;

            // Retrieve all DSS pathnames associated with this data source
            List<DSSPathname> pathnames = getDssPathNames();

            // Attempt to load a time series container for each DSS pathname
            for (DSSPathname pathname : pathnames) {
                TimeSeriesContainer tsc = buildTsFromPathname(i, pathname, timeWindow);

                if (tsc != null) {
                    if (tsc.allMissing()) {
                        // Previously everything was saved as weekly, causing no data to load if daily data was saved.
                        // Save code has been fixed, but adding this code as a means to attempt to find and load that
                        // data so user doesn't have to re-load and re-save data.
                        tsc = backwardsCompatLoadData(i, pathname, timeWindow);

                        if (tsc == null) {
                            // All alternate time steps also returned no data; use a blank synthetic container
                            tsc = buildFixedDataForUserDefined(i, timeWindow);
                        }
                    }

                    // Add the successfully loaded or synthesized container to the data list
                    _timeSeriesData.add(tsc);
                }

                // Advance the pathname index for the next iteration
                i++;
            }
        }

        // Shift DSS-backed data to match the analysis year when the stored start time differs
        if (!_isUserDefined && !_timeSeriesData.isEmpty() && _timeSeriesData.get(0).getStartTime().getTimeInMillis() != timeWindow.getStartTime().getTimeInMillis()) {
            shiftTimeSeriesDataToAnalysisYear(timeWindow);
        }

        // Trim spurious leading time steps for DSS-backed data
        if (!_isUserDefined) {
            trimStartDate(timeWindow);
        }
    }

    /**
     * Attempts to load time series data using alternate time step intervals as a
     * backwards-compatibility fallback for DSS files that were previously saved with
     * a weekly E-part. The method tries daily, monthly, and hourly intervals in order,
     * returning the first container that contains non-missing data. If all alternates
     * return missing data, null is returned to signal that no valid data was found.
     *
     * @param i          the index of the current pathname, used during container construction
     * @param pathname   the original DSS pathname whose E-part will be substituted
     * @param timeWindow the run time window defining the range of data to load
     * @return           a TimeSeriesContainer with valid data, or null if all alternates are missing
     */
    private TimeSeriesContainer backwardsCompatLoadData(int i, DSSPathname pathname, RunTimeWindow timeWindow) {
        // Initialize the return value to null; it remains null if no valid data is found
        TimeSeriesContainer retVal = null;

        // Build the list of alternate time steps to try in order
        List<TemperatureTargetTimeStep> timestepsToSearch = new ArrayList<>();
        timestepsToSearch.add(TemperatureTargetTimeStep.REGULAR_DAILY);
        timestepsToSearch.add(TemperatureTargetTimeStep.REGULAR_MONTHLY);
        timestepsToSearch.add(TemperatureTargetTimeStep.REGULAR_HOURLY);

        // Try each alternate time step until one returns non-missing data
        for (TemperatureTargetTimeStep step : timestepsToSearch) {
            // Substitute the alternate E-part into a copy of the original pathname
            DSSPathname daily = new DSSPathname(pathname.getPathname().replace(TemperatureTargetTimeStep.REGULAR_WEEKLY.toString(), step.toString()));

            // Attempt to load data using the modified pathname
            TimeSeriesContainer tsc = buildTsFromPathname(i, daily, timeWindow);

            if (!tsc.allMissing()) {
                // Valid data was found; store the result and stop searching
                retVal = tsc;
                break;
            }
        }

        // Return the first valid container found, or null if all alternates were missing
        return retVal;
    }

    /**
     * Reads a single time-series record from the source HEC-DSS file and, for
     * user-defined sets, overlays any previously saved values onto a freshly built
     * synthetic weekly grid.
     *
     * For user-defined sets the method:
     *
     *   Reads the DSS record into a temporary map of HEC-integer-time → value.
     *   Generates a blank weekly synthetic container via {@link #buildFixedDataForUserDefined}.
     *   Copies the synthetic container's time grid onto the read container.
     *   Overlays saved values at matching time steps from the temporary map.
     *
     * @param index      the one-based column index used when building user-defined containers
     * @param pathname   the {@link DSSPathname} that identifies the record to read
     * @param timeWindow the {@link RunTimeWindow} used for the synthetic container grid
     * @return the populated {@link TimeSeriesContainer}, or {@code null} if the DSS read
     * returns no data
     */
    private TimeSeriesContainer buildTsFromPathname(int index, DSSPathname pathname, RunTimeWindow timeWindow) {
        // Configure the DSS identifier with the absolute file path and the record pathname
        DSSIdentifier dssIdentifier = new DSSIdentifier();
        dssIdentifier.setFileName(Project.getCurrentProject().getAbsolutePath(_dssSourcePath.toString()));
        dssIdentifier.setDSSPath(pathname.getPathname());

        // Read the time-series record from the DSS file
        TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, false);

        if (tsc != null && isUserDefined()) {
            // Build a lookup map from HEC integer time to saved value for fast overlay
            Map<Integer, Double> timeValueMap = new HashMap<>();
            for (int i = 0; i < tsc.numberValues; i++) {
                timeValueMap.put(tsc.times[i], tsc.values[i]);
            }

            // Generate the canonical weekly grid for this column and analysis year
            TimeSeriesContainer fixedTsc = buildFixedDataForUserDefined(index, timeWindow);

            // Replace the read container's time grid and metadata with the synthetic grid
            tsc.times = fixedTsc.times;
            tsc.startTime = fixedTsc.startTime;
            tsc.endTime = fixedTsc.endTime;
            tsc.startHecTime = fixedTsc.startHecTime;
            tsc.endHecTime = fixedTsc.endHecTime;
            tsc.values = fixedTsc.values;
            tsc.numberValues = tsc.values.length;

            // Overlay previously saved values at matching weekly time steps
            for (int i = 0; i < tsc.times.length; i++) {
                Double value = timeValueMap.get(tsc.times[i]);
                if (value != null) {
                    tsc.values[i] = value;
                }
            }
        }

        return tsc;
    }

    /**
     * Adds a constant integer offset to every time value in the supplied
     * {@link TimeSeriesContainer} and updates its start/end metadata accordingly.
     *
     * The offset is expressed in minutes, matching the HEC integer-time encoding where
     * the unit is minutes since the HEC epoch.
     *
     * @param tsc   the {@link TimeSeriesContainer} to shift; modified in place; no-op if
     *              {@code tsc.times} is {@code null}
     * @param shift the number of minutes to add to each time value; may be negative to
     *              shift backward in time
     */
    private void applyShiftToTsc(TimeSeriesContainer tsc, int shift) {
        if (tsc.times != null) {
            // Add the shift offset to every encoded time value
            for (int i = 0; i < tsc.times.length; i++) {
                tsc.times[i] = tsc.times[i] + shift;
            }

            // Update the start and end metadata to reflect the shifted time range
            tsc.startTime = tsc.times[0];
            tsc.startHecTime = new HecTime(tsc.startTime);
            tsc.endTime = tsc.times[tsc.times.length - 1];
            tsc.endHecTime = new HecTime(tsc.endTime);
        }
    }

    /**
     * Builds a {@link TimeSeriesContainer} with a weekly time grid spanning the analysis
     * year and all values pre-filled with the HEC undefined (missing) sentinel.
     *
     * The grid starts on January 2nd (or an alternate day for known edge-case years) at
     * 01:00 UTC and advances in 7-day increments for approximately one year plus one
     * additional week. Any step that would exceed January 7th of the following year is
     * removed by {@link #trimEnd}.
     *
     * This is a workaround to odd DSS write behavior where writing starts one or more
     * days before the nominal January 1st boundary.
     *
     * @param col        the one-based column index used to build the DSS F-part identifier
     * @param timeWindow the {@link RunTimeWindow} whose start year anchors the grid
     * @return a fully populated {@link TimeSeriesContainer} with a weekly time grid and
     * missing values; never {@code null}
     */
    private TimeSeriesContainer buildFixedDataForUserDefined(int col, RunTimeWindow timeWindow) {
        // Build the template container with DSS metadata for this column
        TimeSeriesContainer tsc = buildTemplateUserDefinedTSContainer(col, _units);

        int year = timeWindow.getStartTime().year();

        // Default start day; workaround to odd DSS write behavior
        int dayOfMonthToTrimTo = 2;

        // Override for year 2010
        if (year == 2010) {
            dayOfMonthToTrimTo = 3;
        }

        // Override for year 2020
        if (year == 2020) {
            dayOfMonthToTrimTo = 5;
        }

        // Define the start time as 01:00 UTC on the computed start day
        LocalTime localTime = LocalTime.of(1, 0);
        ZoneId zoneId = ZoneId.of("UTC");

        // Start date: January 2nd (or adjusted day) at 01:00 UTC to work around the DSS write bug
        LocalDate startDate = LocalDate.of(year, 1, dayOfMonthToTrimTo);

        // End date: one year plus one additional week beyond the start date
        LocalDate endDate = startDate.plusYears(1);
        endDate = endDate.plusWeeks(1);

        // Calculate the number of weekly steps needed to span the date range
        int numWeeks = (int) ChronoUnit.WEEKS.between(startDate, endDate);

        // Build the time array by iterating weekly from the start date to the end date
        LocalDate currentDate = startDate;
        int[] times = new int[numWeeks + 1];
        int i = 0;
        while (currentDate.isBefore(endDate)) {
            times[i] = HecTime.fromZonedDateTime(ZonedDateTime.of(currentDate, localTime, zoneId)).value();
            currentDate = currentDate.plusDays(7);
            i++;
        }

        // Assign the time array and its HecTimeArray wrapper to the container
        tsc.times = times;
        tsc.setTimes(new HecTimeArray(times));

        // Pre-fill all value slots with the HEC undefined (missing) sentinel
        List<Double> nanList = IntStream.range(0, numWeeks + 1)
                .mapToObj(index -> RMAConst.HEC_UNDEFINED_DOUBLE)
                .collect(Collectors.toList());

        tsc.values = nanList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        tsc.numberValues = nanList.size();

        // Set start and end time metadata from the generated time array
        tsc.startTime = times[0];
        tsc.endTime = times[times.length - 1];
        tsc.startHecTime = tsc.getHecTime(0);
        tsc.endHecTime = tsc.getHecTime(tsc.numberValues - 1);

        // Remove any trailing step that extends beyond the intended end boundary
        trimEnd(tsc, new HecTime("07Jan" + endDate.getYear(), "0100"));

        tsc.units = _units;

        return tsc;
    }

    /**
     * Returns the zero-padded column prefix string used when constructing the DSS F-part
     * identifier for user-defined time-series containers.
     *
     * The prefix ensures the column number is always formatted as a six-digit value
     * within the {@code C:XXXXXX} convention (e.g., column 1 → {@code "C:00000"},
     * column 42 → {@code "C:0000"}).
     *
     * @param col the one-based column index; expected to be between 1 and 999,999 inclusive
     * @return the appropriate leading string for the given column magnitude
     */
    private static String getLeadingString(int col) {
        // Default prefix covers single-digit column numbers (1–9)
        String leadingString = "C:00000";

        if (col > 9 && col < 100) {
            leadingString = "C:0000";
        } else if (col > 99 && col < 1000) {
            leadingString = "C:000";
        } else if (col > 999 && col < 10000) {
            leadingString = "C:00";
        } else if (col > 9999 && col < 100_000) {
            leadingString = "C:0";
        } else if (col > 99999 && col < 1_000_000) {
            // Six-digit column number requires no leading zeros beyond the "C:" prefix
            leadingString = "C:";
        }

        return leadingString;
    }

    /**
     * Sets the total number of user-defined temperature target columns to generate when
     * no source DSS file is present.
     *
     * @param value the number of user-defined targets; must be greater than zero
     */
    public void setNumberOfUserDefinedTempTargets(int value) {
        _numberOfUserDefinedTempTargets = value;
    }

    /**
     * Sets the file-system path to the HEC-DSS file to which computed output data is written.
     *
     * @param fileName the output DSS file path; may be {@code null} to clear the setting
     */
    public void setDssOutputPath(Path fileName) {
        _dssOutputPath = fileName;
    }

    /**
     * Returns the file-system path to the HEC-DSS output file, or {@code null} if not set.
     *
     * @return the output DSS file path, or {@code null}
     */
    public Path getDssOutputPath() {
        return _dssOutputPath;
    }

    /**
     * Returns the portion of the F-part that follows the {@code |} delimiter, which
     * represents the tier or label of this target set.
     *
     * For example, if the F-part of the first stored pathname is {@code "C:000001|TIER 2"},
     * this method returns {@code "TIER 2"}. If no {@code |} delimiter is present, the
     * entire F-part string is returned. If no pathnames are stored, an empty string is
     * returned.
     *
     * @return the label portion of the F-part after the {@code |} delimiter, the full
     * F-part if no delimiter exists, or an empty string if no pathnames are stored
     */
    public String getFPartWithoutCollection() {
        String retVal = "";

        if (!_dssPathNames.isEmpty()) {
            retVal = _dssPathNames.get(0).getFPart();

            if (retVal.contains("|")) {
                // Split on the pipe delimiter and return the label that follows it
                String[] split = retVal.split("\\|");
                if (split.length > 1) {
                    retVal = split[1];
                }
            }
        }

        return retVal;
    }

    /**
     * Builds and returns a template {@link TimeSeriesContainer} pre-configured with the
     * DSS metadata required for a user-defined temperature target column.
     *
     * The container is populated with:
     *
     *   A DSS pathname whose C-part is {@code TEMP-WATER-TARGET}, E-part is the
     *       {@link TemperatureTargetTimeStep#REGULAR_WEEKLY} interval string, and F-part
     *       is a zero-padded column identifier suffixed with {@code |USER-DEFINED}.
     *   The system default timezone.
     *   The supplied unit string.
     *   An interval derived from the E-part.
     *   Type {@code "INST-VAL"} (instantaneous value).
     *
     * Time arrays and value arrays are not populated by this method; callers are
     * responsible for assigning those fields.
     *
     * @param col   the one-based column index used to construct the F-part identifier
     * @param units the unit string to assign to the container (e.g., {@code "DEG F"})
     * @return a new template {@link TimeSeriesContainer}; never {@code null}
     */
    public static TimeSeriesContainer buildTemplateUserDefinedTSContainer(int col, String units) {
        TimeSeriesContainer tsc = new TimeSeriesContainer();

        // Build the DSS pathname with standard parts for a user-defined temperature target
        DSSPathname pathname = new DSSPathname();
        pathname.setBPart("");
        pathname.setCPart("TEMP-WATER-TARGET");
        pathname.setEPart(TemperatureTargetTimeStep.REGULAR_WEEKLY.toString());

        // Construct the zero-padded F-part column identifier
        String leadingString = getLeadingString(col);
        pathname.setFPart(leadingString + col + "|USER-DEFINED");

        // Store the full uppercase pathname string on the container
        tsc.fullName = pathname.getPathname().toUpperCase();

        // Apply the system default timezone to the container
        ZoneId dataZoneId = ZoneId.systemDefault();
        tsc.setTimeZoneID(dataZoneId.getId());
        tsc.locationTimezone = dataZoneId.getId();

        // Set units, interval, type, and other metadata fields from the pathname
        tsc.units = units;
        tsc.interval = HecTimeSeriesBase.getIntervalFromEPart(pathname.getEPart());
        tsc.type = "INST-VAL";
        tsc.parameter = pathname.getCPart();
        tsc.location = pathname.bPart();
        tsc.version = pathname.fPart();

        // Ensure values are stored as doubles for precision
        tsc.setStoreAsDoubles(true);

        return tsc;
    }

    /**
     * Indicates whether this target set is equal to another object.
     *
     * Two {@code TemperatureTargetSet} instances are considered equal when their names
     * match, compared case-insensitively. This definition is consistent with
     * {@link #hashCode()}.
     *
     * @param o the object to compare with this instance
     * @return {@code true} if {@code o} is a {@code TemperatureTargetSet} whose name
     * matches this instance's name (case-insensitive); {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        // An object is always equal to itself
        if (this == o) {
            return true;
        }

        // Null or incompatible type means not equal
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Cast is safe; class has been verified above
        TemperatureTargetSet that = (TemperatureTargetSet) o;

        // Names are compared case-insensitively as the sole equality criterion
        return getName().equalsIgnoreCase(that.getName());
    }

    /**
     * Returns a hash code derived from the case-insensitive name of this target set,
     * consistent with the equality contract defined by {@link #equals(Object)}.
     *
     * @return an integer hash code for this target set
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    /**
     * Sets the river location (monitoring station) associated with this target set.
     *
     * @param riverLocation the {@link RiverLocation} to associate; may be {@code null}
     */
    public void setRiverLocation(RiverLocation riverLocation) {
        _riverLocation = riverLocation;
    }

    /**
     * Returns the river location associated with this target set, or {@code null} if
     * none has been assigned.
     *
     * @return the associated {@link RiverLocation}, or {@code null}
     */
    public RiverLocation getRiverLocation() {
        return _riverLocation;
    }

    /**
     * Sets the unit string used for all time-series data in this target set
     * (e.g., {@code "DEG F"} or {@code "DEG C"}).
     *
     * @param units the unit string to apply; should not be {@code null} or empty
     */
    public void setUnits(String units) {
        _units = units;
    }

    /**
     * Returns the unit string for all time-series data in this target set.
     *
     * Defaults to the English-system unit for the {@code TEMP} parameter if not
     * explicitly overridden.
     *
     * @return the unit string; never {@code null}
     */
    public String getUnits() {
        return _units;
    }
}
