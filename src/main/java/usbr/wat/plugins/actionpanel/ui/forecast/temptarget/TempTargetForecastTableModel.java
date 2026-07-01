package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import rma.swing.table.RmaTableModel;                                           // Provides RmaTableModel, the RMA base table model whose row/column lifecycle this class extends

import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;        // Provides TemperatureTargetSet, the model object stored in each row of this table model

import java.util.ArrayList;                                                     // Provides ArrayList for the mutable, ordered backing list of TemperatureTargetSet objects
import java.util.List;                                                          // Provides the List interface for the typed backing collection
import java.util.Optional;                                                      // Provides Optional (imported for potential future use in optional-value operations)
import java.util.Vector;                                                        // Provides Vector for receiving row data from the RmaTableModel row-management API

/**
 * A specialised RmaTableModel that manages a single-column table of
 * TemperatureTargetSet objects for the temperature target forecast UI.
 *
 * The model maintains a parallel ArrayList (_sets) alongside the parent
 * RmaTableModel's internal storage to provide strongly typed, index-aligned
 * access to TemperatureTargetSet instances. All mutating operations keep the
 * two stores in sync: rows added, inserted, or deleted in the superclass are
 * mirrored immediately in _sets.
 *
 * Only Vector rows whose first element is a TemperatureTargetSet are accepted
 * by addRow and insertRow; all other types are silently ignored.
 *
 * @see RmaTableModel
 * @see TemperatureTargetSet
 */
public final class TempTargetForecastTableModel extends RmaTableModel {
    /**
     * Backing list of TemperatureTargetSet objects, parallel to the rows stored in
     * the superclass. Row indices in this list always correspond to the same row
     * indices in the parent RmaTableModel.
     */
    private final List<TemperatureTargetSet> _sets = new ArrayList<>();

    /**
     * Constructs a new TempTargetForecastTableModel with a single column
     * labelled "Temperature Target Sets".
     */
    public TempTargetForecastTableModel() {
        super(new String[]{"Temperature Target Sets"});
    }

    /**
     * Returns the number of rows currently in the model, equal to the number of
     * TemperatureTargetSet objects in the backing list.
     *
     * @return the current row count; zero when the model is empty
     */
    @Override
    public int getRowCount() {
        return _sets.size();
    }

    /**
     * Appends a new row to the end of the model. The row is accepted only when
     * its first element is a TemperatureTargetSet; all other types are silently
     * ignored. Both the superclass row store and the backing list are updated.
     *
     * @param newRow a Vector whose first element must be a TemperatureTargetSet
     */
    @Override
    public void addRow(Vector newRow) {
        Object val = newRow.get(0);

        // Only accept rows whose first element is a TemperatureTargetSet
        if (val instanceof TemperatureTargetSet) {
            // Add to both the superclass store and the typed backing list
            super.addRow(newRow);
            _sets.add((TemperatureTargetSet) val);
        }
    }

    /**
     * Inserts a new row at the specified index, shifting all subsequent rows down.
     * The row is accepted only when its first element is a TemperatureTargetSet;
     * all other types are silently ignored. Both the superclass row store and the
     * backing list are updated at the same index.
     *
     * @param row     the zero-based index at which the new row should be inserted
     * @param rowData a Vector whose first element must be a TemperatureTargetSet
     */
    @Override
    public void insertRow(int row, Vector rowData) {
        Object val = rowData.get(0);

        // Only accept rows whose first element is a TemperatureTargetSet
        if (val instanceof TemperatureTargetSet) {
            // Insert at the same index in both the superclass store and the backing list
            super.insertRow(row, rowData);
            _sets.add(row, (TemperatureTargetSet) val);
        }
    }

    /**
     * Removes the row at the specified index from both the superclass row store
     * and the backing list. Subsequent rows are shifted up to fill the gap.
     *
     * @param index the zero-based index of the row to remove
     */
    @Override
    public void deleteRow(int index) {
        // Remove from both stores in the same operation to keep them in sync
        super.deleteRow(index);
        _sets.remove(index);
    }

    /**
     * Returns the TemperatureTargetSet stored at the given row. The column
     * parameter is accepted but ignored because the model has a single logical
     * value per row. Returns null when the row or column index is out of bounds.
     *
     * @param row the zero-based row index
     * @param col the zero-based column index (ignored; any valid column returns the set)
     * @return the TemperatureTargetSet at the specified row, or null if out of bounds
     */
    @Override
    public Object getValueAt(int row, int col) {
        Object retVal = null;

        // Guard against out-of-bounds row and column indices before accessing the list
        if (row >= 0 && row < _sets.size() && col >= 0 && col < getColumnCount()) {
            retVal = _sets.get(row);
        }
        return retVal;
    }

    /**
     * Replaces the TemperatureTargetSet at the specified row with the given value.
     * Uses an add-then-remove strategy to perform an in-place replacement at the
     * correct index. Silently returns without modification when any index is out of
     * bounds, the value is null, or the value is not a TemperatureTargetSet.
     * Fires a full table data changed event after a successful replacement.
     *
     * @param aValue the new value to set; must be a non-null TemperatureTargetSet
     * @param row    the zero-based row index to replace
     * @param col    the zero-based column index (accepted but not used for dispatch)
     */
    @Override
    public void setValueAt(Object aValue, int row, int col) {
        // Guard: reject null values and out-of-bounds indices before modifying state
        if (row < 0 || row >= _sets.size() || col < 0 || col >= getColumnCount() || aValue == null) {
            return;
        }

        if (aValue instanceof TemperatureTargetSet) {
            // Insert the new value at the target row, then remove the displaced old value
            _sets.add(row, (TemperatureTargetSet) aValue);
            _sets.remove(row + 1);
        }

        // Notify all listeners that the table data has changed
        fireTableDataChanged();
    }

    /**
     * Updates the name field of the TemperatureTargetSet at the specified row.
     * Does nothing when the row index is out of bounds.
     *
     * @param name the new name to assign to the set at the given row
     * @param row  the zero-based row index of the set to update
     */
    void updateName(String name, int row) {
        // Guard against out-of-bounds row indices before mutating the set
        if (row >= 0 && row < _sets.size()) {
            _sets.get(row).setName(name);
        }
    }

    /**
     * Removes all TemperatureTargetSet entries from the model and fires a full
     * table data changed event to notify all registered listeners.
     */
    public void clearTempTargets() {
        // Clear the backing list and refresh all table listeners
        _sets.clear();
        fireTableDataChanged();
    }

    /**
     * Updates the description field of the TemperatureTargetSet at the specified row.
     * Does nothing when the row index is out of bounds.
     *
     * @param desc the new description to assign to the set at the given row
     * @param row  the zero-based row index of the set to update
     */
    void updatedDescription(String desc, int row) {
        // Guard against out-of-bounds row indices before mutating the set
        if (row >= 0 && row < _sets.size()) {
            _sets.get(row).setDescription(desc);
        }
    }
}
