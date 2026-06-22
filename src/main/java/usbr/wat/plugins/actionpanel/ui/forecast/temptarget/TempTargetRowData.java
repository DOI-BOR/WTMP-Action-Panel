package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import java.time.format.DateTimeFormatter;      // Provides DateTimeFormatter for the shared ISO date formatter used when rendering date cells
import java.util.HashMap;                       // Provides HashMap for the column-to-value map storing temperature values keyed by column index
import java.util.Map;                           // Provides the Map interface for the typed column-to-value mapping

/**
 * Data holder representing a single row in the temperature target time-series table.
 * Each row is associated with a HEC integer time value and holds a sparse map of
 * temperature target values keyed by their one-based column index.
 *
 * Instances are created by TempTargetTableModel — one per time step in the analysis
 * window — and are retrieved via TempTargetTableModel.getTempTargetRowData to read
 * HEC time values when building TimeSeriesContainers for DSS output.
 *
 * The column-to-value map is sparse: columns for which no value has been set return
 * null from getValueForTempTargetColumn, which TempTargetPanel converts to the HEC
 * undefined double sentinel before writing to DSS.
 *
 * This class is package-private and final; it is not intended for use outside the
 * temperature target UI package or for subclassing.
 *
 * @see TempTargetTableModel
 * @see TempTargetPanel
 */
final class TempTargetRowData {
    /**
     * Shared ISO date formatter (yyyy-MM-dd) used by TempTargetTableModel when
     * rendering the Date column cells from LocalDate values.
     */
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    /**
     * HEC integer time value representing the date/time of this row within the analysis window.
     */
    private int _time;

    /**
     * Sparse map from one-based temperature target column index to the corresponding
     * double temperature value for this row. Absent entries indicate a missing value.
     */
    private final Map<Integer, Double> _tempTargetColumnToValueMapInRow = new HashMap<>();

    /**
     * Constructs a new TempTargetRowData for the given HEC integer time value.
     * The column-to-value map starts empty; values are added via setValueForTempTargetColumn.
     *
     * @param time the HEC integer time value for this row's date/time position
     */
    TempTargetRowData(int time) {
        _time = time;
    }

    /**
     * Returns the HEC integer time value associated with this row.
     *
     * @return the HEC integer time for this row's date/time position
     */
    int getTime() {
        return _time;
    }

    /**
     * Returns the temperature value stored for the given column index in this row,
     * or null if no value has been set for that column.
     *
     * @param column the one-based column index of the temperature target to retrieve
     * @return the Double temperature value for the column, or null if absent
     */
    Double getValueForTempTargetColumn(Integer column) {
        return _tempTargetColumnToValueMapInRow.get(column);
    }

    /**
     * Sets the HEC integer time value for this row.
     *
     * @param time the new HEC integer time value to assign to this row
     */
    void setTime(int time) {
        _time = time;
    }

    /**
     * Stores a temperature value for the given column index in this row, replacing any
     * previously stored value for that column.
     *
     * @param temperatureTargetColumn the one-based column index of the temperature target
     * @param value                   the Double temperature value to store; may be null
     *                                to represent a missing value
     */
    void setValueForTempTargetColumn(Integer temperatureTargetColumn, Double value) {
        // Insert or overwrite the value for this column in the sparse row map
        _tempTargetColumnToValueMapInRow.put(temperatureTargetColumn, value);
    }
}
