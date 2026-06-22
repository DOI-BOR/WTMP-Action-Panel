package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import java.awt.Dimension;          // Constrains the preferred viewport height of the info table
import java.awt.GridBagConstraints;  // Controls component placement within a GridBagLayout
import java.io.IOException;          // Handles errors when creating the simulation group directory
import java.nio.file.Files;          // Creates the simulation group DSS output directory on disk
import java.nio.file.Path;           // Represents file system paths used for DSS source and output paths
import java.nio.file.Paths;          // Constructs Path instances from string segments
import java.util.ArrayList;          // Mutable lists of TemperatureTargetSet, DSSPathname, and related types

import java.util.Collections;        // Provides singletonList, used when constructing single-element table row Vectors
import java.util.LinkedHashSet;      // Accumulates temperature target sets in insertion-order without duplicates
import java.util.List;               // Ordered collections used throughout this class
import java.util.Vector;             // Constructs row data passed to RmaTableModel row-management methods
import java.util.logging.Level;      // Specifies log severity (SEVERE, CONFIG) in Logger calls
import java.util.logging.Logger;     // JUL-based logging of DSS save and directory creation errors
import java.util.stream.Collectors;  // Collects stream results (e.g. EnsembleSet names) to a List

import javax.swing.JButton;          // The "Import/Create T.T. Set..." button in the lower panel
import javax.swing.JOptionPane;      // Displays confirmation and error modal dialogs
import javax.swing.JScrollPane;      // Provides constants for setting horizontal/vertical scroll bar policies
import javax.swing.JTable;           // Provides the AUTO_RESIZE_OFF constant used when columns exceed 12
import javax.swing.SwingUtilities;   // Obtains the ancestor Window used when opening the import dialog
import javax.swing.table.TableColumnModel; // Removes all columns from the time-series table before rebuilding

import com.rma.io.DssFileManagerImpl;      // Reads, writes, and closes HEC-DSS files
import com.rma.model.Project;              // Resolves project-relative paths to absolute paths

import hec.data.Parameter;                 // Provides the PARAMID_TEMP constant used when building time-series column arrays
import hec.data.Units;                     // Matches raw unit strings to canonical display unit strings
import hec.heclib.dss.DSSPathname;         // Constructs and modifies DSS path D-part and E-part fields
import hec.hecmath.HecMath;                // Base type for the result of the interpolation operation
import hec.hecmath.HecMathException;       // Errors thrown during time-series interpolation
import hec.hecmath.TimeSeriesMath;         // Interpolates native time-series data to regular hourly intervals
import hec.io.TimeSeriesContainer;         // HEC data structure storing time-series values and metadata
import hec.io.impl.StoreOptionImpl;        // Configures DSS write behaviour (native time step storage)
import hec.lang.NamedType;                 // Extracts names from EnsembleSet objects via method references
import hec.model.RunTimeWindow;            // Retrieves the analysis period start and end times
import hec2.wat.model.WatAnalysisPeriod;   // Accesses the simulation group's run time window

import rma.swing.EnabledJPanel;            // A JPanel that propagates enable/disable state to its children
import rma.swing.RmaInsets;               // Consistent padding values used in GridBagConstraints
import rma.swing.RmaJTable;               // RMA-extended JTable with row management and editor conveniences
import rma.swing.table.RmaTableModel;     // Provides addRow, insertRow, deleteRow, and fireTableDataChanged
import rma.util.RMAConst;                 // Provides the HEC_UNDEFINED_DOUBLE sentinel value used in user-defined value arrays

import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;              // Identifies ensemble sets that depend on a temperature target set
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;         // Top-level model holding temperature target sets and analysis period
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;     // Model object displayed and saved by this panel
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetTimeStep; // Provides the REGULAR_HOURLY constant used during interpolation
import usbr.wat.plugins.actionpanel.ui.forecast.AbstractForecastPanel;       // Base class supplying the upper table and shared forecast UI
import usbr.wat.plugins.actionpanel.ui.forecast.ImportForecastWindow;        // Base dialog type accepted by importForecastData
import usbr.wat.plugins.actionpanel.ui.forecast.ForecastPanel;               // Parent panel that owns this tab and holds the simulation group

/**
 * Panel that displays and manages the Temperature Target tab within the Forecast
 * Action Panel. It presents:
 *
 * 1. An upper table (inherited _tempTargetTable) listing all TemperatureTargetSet
 *    objects belonging to the active ForecastSimGroup.
 * 2. A read-only info table (_ttInfoTable) showing the selected set's name,
 *    description, and river location.
 * 3. An "Import/Create T.T. Set..." button that opens the TempTargetImportDialog.
 * 4. A time-series data table (_ttTable) driven by TempTargetTableModel, showing
 *    date and per-target temperature values for the selected set.
 *
 * Imported sets are saved to a project-relative DSS file under
 * "forecast/simGroups/{groupName}/{setName}.dss". Both native-timestep and
 * regular-hourly variants of each time series are written to DSS on save.
 * User-defined sets can be edited inline and are written back to DSS on panel save.
 *
 * @see TemperatureTargetSet
 * @see TempTargetTableModel
 * @see TempTargetImportDialog
 */
public class TempTargetPanel extends AbstractForecastPanel<TemperatureTargetSet> {
	/**
	 * Minimum pixel width applied to the Date column in the time-series table.
	 */
	private static final int DATE_COL_MIN_WIDTH = 100;

	/**
	 * JUL logger scoped to this class for DSS write and directory creation error reporting.
	 */
	private static final Logger LOGGER = Logger.getLogger(TempTargetPanel.class.getName());

	/**
	 * Read-only table showing the selected set's name, description, and river location.
	 */
	private RmaJTable _ttInfoTable;

	/**
	 * Button that opens the TempTargetImportDialog to import or create a temperature target set.
	 */
	private JButton _createButton;

	/**
	 * Table displaying date rows and per-target temperature columns for the selected set.
	 */
	private RmaJTable _ttTable;

	/**
	 * Table model backing _ttTable; rebuilt each time a new TemperatureTargetSet is selected.
	 */
	private TempTargetTableModel _ttTableModel;

	/**
	 * The TemperatureTargetSet currently displayed in the info and time-series tables; null when cleared.
	 */
	private TemperatureTargetSet _selectedTempTargetSet;

	/**
	 * Row index of the currently selected row in the upper temperature target table.
	 */
	private int _topTableRowSelected;

	/**
	 * The ForecastSimGroup currently loaded into the panel; null when no group is active.
	 */
	private ForecastSimGroup _fsg;

	/**
	 * The default auto-resize mode of the time-series table, captured at construction
	 * and restored when the column count is 12 or fewer.
	 */
	private int _defaultResizeMode;

	/**
	 * Constructs a new TempTargetPanel wired to the given parent ForecastPanel,
	 * builds all UI controls via the superclass, and registers panel-level listeners.
	 *
	 * @param forecastPanel the parent ForecastPanel that owns and displays this tab
	 */
	public TempTargetPanel(ForecastPanel forecastPanel) {
		// Delegate control building to the superclass before adding panel-specific listeners
		super(forecastPanel);

		// Wire the Import/Create button listener after controls have been built
		addPanelListeners();
	}

	/**
	 * Registers the action listener on the Import/Create button. Called once from the
	 * constructor after the superclass has built all UI controls.
	 */
	private void addPanelListeners() {
		// Open the TempTargetImportDialog when the Import/Create button is clicked
		_createButton.addActionListener(e -> importForecastData(null));
	}

	/**
	 * Opens a new TempTargetImportDialog to let the user import a temperature target
	 * set from an existing DSS file or create a new user-defined one. The dialog
	 * forwards its result to this panel via a TempTargetConsumer callback.
	 * The dlg parameter is accepted but ignored; the dialog is always created fresh.
	 *
	 * @param dlg accepted for interface compatibility but unused; pass null
	 */
	@Override
	protected void importForecastData(ImportForecastWindow dlg) {
		// Always create a new dialog; existing set names are passed to detect duplicates
		new TempTargetImportDialog(SwingUtilities.getWindowAncestor(this), getExistingSetNames(), _fsg, new TempTargetConsumer(this));
	}

	/**
	 * Collects the names of all TemperatureTargetSet objects currently listed in the
	 * upper temperature target table. Used by the import dialog to detect duplicate names.
	 *
	 * @return a List of existing set name strings; empty when the table has no rows
	 */
	private List<String> getExistingSetNames() {
		// Initialize the list that will accumulate all non-blank set names from the table
		List<String> retVal = new ArrayList<>();

		// Iterate over every row in the upper table to collect temperature target set names
		for (int row = 0; row < _tempTargetTable.getRowCount(); row++) {
			// Retrieve the value stored in column 0 of the current row
			Object val = _tempTargetTable.getValueAt(row, 0);

			// Include only non-null, non-blank name entries
			if (val != null && !val.toString().trim().isEmpty()) {
				retVal.add(val.toString());
			}
		}

		// Return the accumulated list of existing set names
		return retVal;
	}

	/**
	 * Called by TempTargetConsumer after the import dialog confirms its selection.
	 * Saves the new sets to DSS, inserts or replaces rows in the upper table,
	 * updates the selection, and refreshes the info and time-series tables. If the
	 * upper table ends up with no selection, the panel is cleared.
	 *
	 * @param tempTargetSets the list of TemperatureTargetSet objects accepted from the dialog
	 * @throws TempTargetSaveFailedException if saving any set to DSS fails
	 */
	void tempTargetSetsSelected(List<TemperatureTargetSet> tempTargetSets) throws TempTargetSaveFailedException {
		// Retrieve the upper table's backing model for row insertion and deletion
		RmaTableModel upperTableModel = (RmaTableModel) getTableForPanel().getModel();

		// Save all new sets to DSS and add them to the simulation group before updating the UI
		initializeSaveOfNewTempTargets(tempTargetSets);

		// Process each imported set, either replacing an existing row or appending a new one
		for (TemperatureTargetSet tempTargetSet : tempTargetSets) {
			// Check whether a row with the same name already exists in the upper table
			Integer rowThatContainsName = getRowThatContainsName(upperTableModel, tempTargetSet);

			if (rowThatContainsName != null) {
				// Replace the existing row in-place using an insert-then-delete strategy
				upperTableModel.insertRow(rowThatContainsName, new Vector<>(Collections.singletonList(tempTargetSet)));
				upperTableModel.deleteRow(rowThatContainsName + 1);

				// Update the table's selection highlight to the replaced row
				_tempTargetTable.setRowSelectionInterval(rowThatContainsName, rowThatContainsName, false);
				_tempTargetTable.updateSelection(rowThatContainsName, 0, false, false);

			} else {
				// Append the new set as a new row at the bottom of the upper table
				upperTableModel.addRow(new Vector<>(Collections.singletonList(tempTargetSet)));
				int lastRowIndex = upperTableModel.getRowCount() - 1;

				// Update the table's selection highlight to the newly appended row
				_tempTargetTable.setRowSelectionInterval(lastRowIndex, lastRowIndex, false);
				_tempTargetTable.updateSelection(lastRowIndex, 0, false, false);
			}
		}

		if (!tempTargetSets.isEmpty()) {
			// Refresh the simulation panel to reflect any newly added ensemble set options
			_forecastPanel.refreshSimulationPanel(_fsg);

			// Display the last imported set in the info and time-series tables
			TemperatureTargetSet selectedSet = tempTargetSets.get(tempTargetSets.size() - 1);
			_selectedTempTargetSet = selectedSet;
			fillTempTargetInfoTable(selectedSet);
			fillTempTargetTable(selectedSet);
		}

		// Clear the panel if the table ended up with no valid selection
		if (_tempTargetTable.getSelectedRow() < 0) {
			clearPanel();
		}
	}

	/**
	 * Saves each new TemperatureTargetSet to DSS, updates its DSS path names, adds
	 * it to the simulation group's set list (deduplicating via LinkedHashSet), and
	 * persists the group to disk. Does nothing when the group or set list is null/empty.
	 *
	 * @param tempTargetSets the list of new TemperatureTargetSet objects to save
	 * @throws TempTargetSaveFailedException if writing any set's time series to DSS fails
	 */
	private void initializeSaveOfNewTempTargets(List<TemperatureTargetSet> tempTargetSets) throws TempTargetSaveFailedException {
		// Only proceed when a valid simulation group and non-empty set list are available
		if (_fsg != null && tempTargetSets != null && !tempTargetSets.isEmpty()) {
			// Use a LinkedHashSet to preserve insertion order while preventing duplicates
			LinkedHashSet<TemperatureTargetSet> sets = new LinkedHashSet<>(_fsg.getTemperatureTargetSets());

			for (TemperatureTargetSet set : tempTargetSets) {
				// Write the set's time-series data to DSS and capture the resulting pathnames
				List<DSSPathname> pathNames = saveImported(set, _fsg);

				// Store the written pathnames on the set for future DSS access
				set.setDssPathNames(pathNames);
				sets.add(set);
			}

			// Replace the simulation group's set list with the deduplicated, updated collection
			_fsg.setTemperatureTargetSets(new ArrayList<>(sets));

			// Persist the updated simulation group data to disk
			_fsg.saveData();
		}
	}

	/**
	 * Populates the info table with the given TemperatureTargetSet's name, description,
	 * and river location, then disables row editing so the fields become read-only.
	 *
	 * @param tempTargetSet the TemperatureTargetSet whose metadata is displayed in the info table
	 */
	private void fillTempTargetInfoTable(TemperatureTargetSet tempTargetSet) {
		// Commit any pending edits before overwriting the info table values
		_ttInfoTable.commitEdit(true);

		// Populate column 0 with the set object, column 1 with description, column 2 with river location
		_ttInfoTable.setValueAt(tempTargetSet, 0, 0);
		_ttInfoTable.setValueAt(tempTargetSet.getDescription(), 0, 1);
		_ttInfoTable.setValueAt(tempTargetSet.getRiverLocation(), 0, 2);

		// Disable the row so the name, description, and river location cannot be edited directly
		_ttInfoTable.setRowEnabled(false, 0);
	}

	/**
	 * Searches the given table model for a row whose first-column value matches the
	 * name of the provided TemperatureTargetSet (case-insensitive).
	 *
	 * @param tableModel    the RmaTableModel to search
	 * @param tempTargetSet the set whose name is used as the search key
	 * @return the zero-based row index of the matching row, or null if not found
	 */
	private Integer getRowThatContainsName(RmaTableModel tableModel, TemperatureTargetSet tempTargetSet) {
		// Default to null; only assigned when a matching row is found
		Integer retVal = null;

		// Iterate over every row in the model searching for a name match
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			// Retrieve the value stored in column 0 of the current row
			Object val = tableModel.getValueAt(row, 0);

			// Match by name using case-insensitive comparison
			if (val != null && val.toString().equalsIgnoreCase(tempTargetSet.getName())) {
				// Match found; record the row index and stop searching
				retVal = row;
				break;
			}
		}

		// Return the matching row index, or null if no match was found
		return retVal;
	}

	/**
	 * Builds the lower section of the forecast panel by creating and laying out three
	 * components: a read-only info table (two-row preferred height), an Import/Create
	 * button, and a scrollable time-series data table with a permanent horizontal
	 * scrollbar and a TempTargetTableModel.
	 *
	 * @param lowerPanel the EnabledJPanel into which the lower section is built
	 */
	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel) {
		// Define the column headers for the read-only info table
		String[] headers = new String[]{"Temperature Target Set", "Description", "River Location"};

		// Create the read-only info table with a preferred height of two rows
		_ttInfoTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Constrain the preferred viewport height to exactly two rows
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * 2;
				return d;
			}
		};

		// Suppress the vertical scrollbar on the info table; it never needs to scroll vertically
		_ttInfoTable.getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		// Configure layout: info table expands horizontally but takes minimal vertical space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.06;   // Allocate a small fraction of vertical space to the info table
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_ttInfoTable.getScrollPane(), gbc);

		// Create the Import/Create button; disabled until a simulation group is loaded
		_createButton = new JButton("Import/Create T.T. Set...");
		_createButton.setEnabled(false);

		// Configure layout: button sits to the right of the info table, fixed size
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;   // Button does not expand horizontally
		gbc.weighty = 0.0;   // Button does not expand vertically
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);

		// Create the time-series table and its backing model; columns are added dynamically on selection
		_ttTable = new RmaJTable(this, new String[]{"Date", ""});
		_ttTableModel = new TempTargetTableModel();
		_ttTable.setModel(_ttTableModel);

		// Always show the horizontal scrollbar so wide multi-target tables are navigable
		_ttTable.getScrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		// Capture the default resize mode before any overrides are applied
		_defaultResizeMode = _ttTable.getAutoResizeMode();

		// Remove the default add/edit row popup items; this table uses a custom model
		_ttTable.removePopupMenuRowEditingOptions();

		// Configure layout: time-series table expands to fill all remaining space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;   // Time-series table absorbs all remaining vertical space
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_ttTable.getScrollPane(), gbc);
	}

	/**
	 * Displays a confirmation dialog before deleting the given TemperatureTargetSet.
	 * When dependent EnsembleSets exist, their names are listed in the message as a
	 * warning that they will also be removed. On confirmation, removes the set from
	 * the simulation group, saves the project, refreshes the simulation panel if needed,
	 * and removes the corresponding row from the upper table.
	 *
	 * @param set                  the TemperatureTargetSet to delete
	 * @param deleteDueToOverwrite true when deletion is triggered by an overwrite (unused here)
	 * @return true if the user confirmed and deletion was performed; false if cancelled
	 */
	@Override
	protected boolean delete(TemperatureTargetSet set, boolean deleteDueToOverwrite) {
		// Default to false; set to true only when the user confirms the deletion
		boolean retVal = false;

		// Identify EnsembleSets that reference this temperature target set
		List<EnsembleSet> eSetsUsingTTSet = _fsg.getEnsembleSetsUsingTempTargetSet(set);

		// Start with a simple single-set deletion confirmation message
		String confirmMessage = "Do you want to delete temperature target set " + set.getName() + "?";

		if (!eSetsUsingTTSet.isEmpty()) {
			// Build a multi-line message listing the cascade-deleted EnsembleSets by name
			List<String> eSetNames = eSetsUsingTTSet.stream()
					.map(NamedType::getName)
					.collect(Collectors.toList());

			// Override the message to warn the user about cascade-deleted ensemble sets
			confirmMessage = "Deleting " + set.getName() + " will also delete the following ensemble sets that use it:" +
					"\n\n" + String.join(",\n", eSetNames) + "\n\nDo you want to continue?";
		}

		// Show the appropriate confirmation dialog before making any changes
		int opt = JOptionPane.showConfirmDialog(this, confirmMessage,
				"Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (opt == JOptionPane.YES_OPTION) {
			retVal = true;

			// Locate the row by name and clear the selection before removing the data
			int rowToDelete = _tempTargetTable.getRowWithName(set.getName());
			_selectedTempTargetSet = null;

			// Remove the set from the model and persist the change
			_fsg.removeTemperatureTargetSet(set);
			_fsg.saveData();

			// Refresh the simulation panel when dependent ensemble sets were also removed
			if (!eSetsUsingTTSet.isEmpty()) {
				_forecastPanel.refreshSimulationPanel(_fsg);
			}

			// Remove the row from the upper table after the model has been updated
			_tempTargetTable.deleteRow(rowToDelete);
		}

		// Return true if deletion was confirmed, false if the user cancelled
		return retVal;
	}

	/**
	 * Returns the upper temperature target table that serves as the primary selection
	 * table for this panel. Required by the AbstractForecastPanel contract.
	 *
	 * @return the ForecastTable (_tempTargetTable) listing all TemperatureTargetSet objects
	 */
	@Override
	public ForecastTable getTableForPanel() {
		// Return the inherited upper table used to display all temperature target sets
		return _tempTargetTable;
	}

	/**
	 * Persists the currently displayed TemperatureTargetSet back to the model and DSS.
	 * Only runs when the panel is modified, a simulation group is active, and the
	 * selected set and table have valid content. For user-defined sets whose table has
	 * been modified, writes each column's values back to DSS before updating the name
	 * and description in both the model and the upper table. Clears the panel when
	 * no simulation group is active or the table is empty.
	 */
	@Override
	protected void savePanel() {
		// Retrieve the currently active simulation group from the parent forecast panel
		ForecastSimGroup simGrp = _forecastPanel.getSimulationGroup();

		if (simGrp != null && simGrp.equals(_fsg) && _selectedTempTargetSet != null && _ttTable.getRowCount() > 0 && isModified()) {
			// Commit any pending edits in both tables before reading their values
			_ttInfoTable.commitEdit(true);
			_ttTable.commitEdit(true);

			// Retrieve the current list of temperature target sets from the simulation group
			List<TemperatureTargetSet> sets = new ArrayList<>(simGrp.getTemperatureTargetSets());

			// Ensure the selected set is present in the list even if it was added after load
			if (!sets.contains(_selectedTempTargetSet)) {
				sets.add(_selectedTempTargetSet);
			}

			// Save the edited user-defined table values back to DSS if the table was modified
			if (_selectedTempTargetSet.isUserDefined() && _ttTable.isModified()) {
				List<DSSPathname> pathNames = null;
				try {
					// Mark the set as modified before writing to ensure the DSS record is updated
					_selectedTempTargetSet.setModified(true);
					pathNames = saveUserDefinedTable(_selectedTempTargetSet, simGrp);

					// Clear the table's modified flag after a successful save
					_ttTable.setModified(false);
				} catch (TempTargetSaveFailedException e) {
					// Notify the user of the DSS write failure and log the error
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"DSS Write Failed", JOptionPane.ERROR_MESSAGE);
					LOGGER.log(Level.CONFIG, e, () -> "Temp Target save failed: " + e.getMessage());
				}

				// Update the set's DSS path names with the newly written pathnames
				_selectedTempTargetSet.setDssPathNames(pathNames);
			}

			// Propagate any name or description changes from the info table to the model
			updateSetName();
			updateDescription();

			// Push the updated set list back onto the simulation group
			simGrp.setTemperatureTargetSets(sets);
		} else if (simGrp == null || (_ttTable.getRowCount() < 1)) {
			// Clear the panel when there is no active group or nothing to save
			clearPanel();
		}
	}

	/**
	 * Reads the name from the info table and, if it has changed, updates the selected
	 * TemperatureTargetSet, the simulation group's list, and the upper table model.
	 */
	private void updateSetName() {
		// Retrieve the current name value from column 0 of the info table
		Object val = _ttInfoTable.getValueAt(0, 0);

		// Only update if the name is non-blank and differs from the current set name
		if (val != null && !val.toString().trim().isEmpty() && !val.toString().trim().equalsIgnoreCase(_selectedTempTargetSet.getName())) {
			String name = val.toString().trim();

			// Propagate the new name to the simulation group's copy of this set
			updateNameInSimGroupList(name);

			// Update the selected set's name and reflect it in the upper table model
			_selectedTempTargetSet.setName(name);
			((TempTargetForecastTableModel) _tempTargetTable.getModel()).updateName(name, _topTableRowSelected);
		}
	}

	/**
	 * Reads the description from the info table and, if it has changed, updates the
	 * selected TemperatureTargetSet, the simulation group's list, and the upper table model.
	 */
	private void updateDescription() {
		// Retrieve the current description value from column 1 of the info table
		Object val = _ttInfoTable.getValueAt(0, 1);

		// Only update if the description differs from the current set description
		if (val != null && !val.toString().trim().equalsIgnoreCase(_selectedTempTargetSet.getDescription())) {
			String desc = val.toString().trim();

			// Propagate the new description to the simulation group's copy of this set
			updateDescInSimGroupList(desc);

			// Update the selected set's description and reflect it in the upper table model
			_selectedTempTargetSet.setDescription(desc);
			((TempTargetForecastTableModel) _tempTargetTable.getModel()).updatedDescription(desc, _topTableRowSelected);
		}
	}

	/**
	 * Updates the name field of the TemperatureTargetSet that equals _selectedTempTargetSet
	 * in the simulation group's master list. Ensures the in-memory model stays consistent
	 * with the UI after a name edit.
	 *
	 * @param name the new name to apply to the matching set in the group list
	 */
	private void updateNameInSimGroupList(String name) {
		// Retrieve all temperature target sets from the simulation group
		List<TemperatureTargetSet> sets = _fsg.getTemperatureTargetSets();

		// Iterate over the list to find and update the matching set
		for (TemperatureTargetSet set : sets) {
			// Match by object equality to update only the selected set
			if (set.equals(_selectedTempTargetSet)) {
				set.setName(name);
			}
		}
	}

	/**
	 * Updates the description field of the TemperatureTargetSet that equals
	 * _selectedTempTargetSet in the simulation group's master list.
	 *
	 * @param desc the new description to apply to the matching set in the group list
	 */
	private void updateDescInSimGroupList(String desc) {
		// Retrieve all temperature target sets from the simulation group
		List<TemperatureTargetSet> sets = _fsg.getTemperatureTargetSets();

		// Iterate over the list to find and update the matching set
		for (TemperatureTargetSet set : sets) {
			// Match by object equality to update only the selected set
			if (set.equals(_selectedTempTargetSet)) {
				set.setDescription(desc);
			}
		}
	}

	/**
	 * Saves an imported TemperatureTargetSet's time-series data to a DSS file in the
	 * simulation group's output directory. Each TimeSeriesContainer is written using
	 * saveTimeSeries, which produces both a native-timestep record and an hourly record.
	 * Returns the list of DSS pathnames for all written records.
	 *
	 * @param tempTargetSet the set whose time series should be saved to DSS
	 * @param simGrp        the ForecastSimGroup providing the analysis period and group name
	 * @return a List of DSSPathname objects for all records written to DSS
	 * @throws TempTargetSaveFailedException if the analysis period is unset or a DSS write fails
	 */
	private List<DSSPathname> saveImported(TemperatureTargetSet tempTargetSet, ForecastSimGroup simGrp) throws TempTargetSaveFailedException {
		// Initialize the list that will accumulate DSS pathnames for all written records
		List<DSSPathname> retVal = new ArrayList<>();

		// Retrieve the analysis period to scope the time-series data range
		WatAnalysisPeriod analysisPeriod = simGrp.getAnalysisPeriod();

		if (analysisPeriod != null && analysisPeriod.getRunTimeWindow() != null) {
			// Retrieve all time-series containers within the analysis run time window
			List<TimeSeriesContainer> timeSeriesData = tempTargetSet.getTimeSeriesData(analysisPeriod.getRunTimeWindow());

			// Resolve the output DSS file path within the simulation group's directory
			String forecastSimGroupDirectory = getSimGroupDirectory(simGrp);
			String delim = "/";
			String fileName = forecastSimGroupDirectory + delim + tempTargetSet.getName() + ".dss";

			for (TimeSeriesContainer tsc : timeSeriesData) {
				// Set the output file and clear the D-part so the DSS record uses the canonical path
				tsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
				DSSPathname pathname = new DSSPathname(tsc.fullName);
				pathname.setDPart("");
				tsc.fullName = pathname.getPathname();
				tsc.storedAsdoubles = true;
				retVal.add(pathname);

				// Write the time series to DSS (both native and hourly variants)
				saveTimeSeries(tsc, fileName);

				// For user-defined sets, point the source path to the new output file
				if (tempTargetSet.isUserDefined()) {
					tempTargetSet.setDssSourcePath(Paths.get(fileName));
				}

				// Update the set's output path to the written DSS file
				tempTargetSet.setDssOutputPath(Paths.get(fileName));
			}
		} else {
			// Cannot save without a valid analysis period
			throw new TempTargetSaveFailedException("Analysis Period is not set for simulation!");
		}

		// Return the list of all DSS pathnames written during this save operation
		return retVal;
	}

	/**
	 * Resolves and creates (if absent) the project-relative directory used to store
	 * DSS files for the given simulation group: "forecast/simGroups/{groupName}".
	 * Logs a CONFIG-level message if directory creation fails.
	 *
	 * @param simGrp the ForecastSimGroup whose name determines the subdirectory
	 * @return the project-relative directory path string (without trailing slash)
	 */
	private String getSimGroupDirectory(ForecastSimGroup simGrp) {
		// Construct the project-relative path for the simulation group's DSS output directory
		String forecastSimGroupDirectory = "forecast/simGroups/" + simGrp.getName();

		try {
			// Resolve the relative directory to an absolute path and create it if absent
			Path newDssFilesDirectory = Paths.get(Project.getCurrentProject().getAbsolutePath(forecastSimGroupDirectory));
			Files.createDirectories(newDssFilesDirectory);
		} catch (IOException e) {
			// Log the directory creation failure at CONFIG level; the path string is still returned
			LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + forecastSimGroupDirectory + " directories");
		}

		// Return the project-relative directory path for use in DSS file name construction
		return forecastSimGroupDirectory;
	}

	/**
	 * Saves the user-defined temperature target values currently displayed in _ttTable
	 * back to a DSS file. Reads each column's values from the table model and builds a
	 * TimeSeriesContainer per target column, writing each via saveTimeSeries.
	 *
	 * @param tempTargetSet the user-defined set whose edited values are to be saved
	 * @param simGrp        the ForecastSimGroup providing the analysis period
	 * @return a List of DSSPathname objects for all records written
	 * @throws TempTargetSaveFailedException if any DSS write fails
	 */
	private List<DSSPathname> saveUserDefinedTable(TemperatureTargetSet tempTargetSet, ForecastSimGroup simGrp) throws TempTargetSaveFailedException {
		// Initialize the list that will accumulate DSS pathnames for all written records
		List<DSSPathname> retVal = new ArrayList<>();

		// Collect the HEC time integer for each row from the table model
		int[] times = new int[_ttTableModel.getRowCount()];

		// Resolve the output DSS file path for the user-defined set
		String forecastSimGroupDirectory = getSimGroupDirectory(simGrp);
		String delim = "/";
		String fileName = forecastSimGroupDirectory + delim + tempTargetSet.getName() + ".dss";

		// Update the source path to the new output file location
		tempTargetSet.setDssSourcePath(Paths.get(fileName));

		// Populate the times array from the row data stored in the table model
		for (int row = 0; row < _ttTableModel.getRowCount(); row++) {
			TempTargetRowData rowData = _ttTableModel.getTempTargetRowData(row);
			times[row] = rowData.getTime();
		}

		// Build and write one TimeSeriesContainer per temperature target column
		for (int col = 1; col < _ttTableModel.getColumnCount(); col++) {
			// Ensure the time-series data is initialised for the current analysis window
			tempTargetSet.getTimeSeriesData(_fsg.getAnalysisPeriod().getRunTimeWindow());

			// Build a template container for this column using the set's configured units
			TimeSeriesContainer tsc = TemperatureTargetSet.buildTemplateUserDefinedTSContainer(col, tempTargetSet.getUnits());

			// Populate the container's time boundary and data fields from the table model
			tsc.startTime = times[0];
			tsc.endTime = times[times.length - 1];
			tsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
			tsc.times = times;
			tsc.values = getUserDefinedValues(col);
			tsc.numberValues = tsc.values.length;
			tsc.startHecTime = tsc.getHecTime(0);
			tsc.endHecTime = tsc.getHecTime(times.length - 1);
			tsc.storedAsdoubles = true;

			// Write the time series to DSS and update the set's output and source paths
			saveTimeSeries(tsc, fileName);
			tempTargetSet.setDssOutputPath(Paths.get(Project.getCurrentProject().getRelativePath(fileName)));
			tempTargetSet.setDssSourcePath(Paths.get(Project.getCurrentProject().getRelativePath(fileName)));

			// Record the pathname of the written record in the result list
			retVal.add(new DSSPathname(tsc.fullName));
		}

		// Return the list of all DSS pathnames written during this save operation
		return retVal;
	}

	/**
	 * Writes a TimeSeriesContainer to DSS in two forms: interpolated to regular hourly
	 * intervals (for downstream model use) and at the container's native time step
	 * (for archive fidelity). The E-part of the hourly pathname is set to the
	 * REGULAR_HOURLY token and the D-part is cleared before writing.
	 * Throws TempTargetSaveFailedException if either write returns a non-zero status.
	 * Always closes the DSS file handles in a finally block.
	 *
	 * @param nativeTsc the TimeSeriesContainer at the native time step to write
	 * @param fileName  the project-relative DSS file path to write to
	 * @throws TempTargetSaveFailedException if either the hourly or native write fails
	 */
	private void saveTimeSeries(TimeSeriesContainer nativeTsc, String fileName) throws TempTargetSaveFailedException {
		// Declare the hourly container here so the finally block can close it if non-null
		TimeSeriesContainer hourlyTsc = null;

		// Set the absolute output file path on the native container before writing
		nativeTsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);

		try {
			// Only interpolate and write when the native container has non-missing values
			if (!nativeTsc.allMissing()) {
				// Interpolate the native time series to a regular hourly interval
				TimeSeriesMath timeSeriesMath = new TimeSeriesMath(nativeTsc);
				HecMath hecMath = timeSeriesMath.interpolateDataAtRegularInterval(TemperatureTargetTimeStep.REGULAR_HOURLY.toString(), "0M");
				hourlyTsc = (TimeSeriesContainer) hecMath.getData();

				// Populate time boundary fields on the hourly container
				hourlyTsc.startTime = hourlyTsc.times[0];
				hourlyTsc.startHecTime = hourlyTsc.getHecTime(0);
				hourlyTsc.endTime = hourlyTsc.times[hourlyTsc.times.length - 1];
				hourlyTsc.endHecTime = hourlyTsc.getHecTime(hourlyTsc.numberValues - 1);
				hourlyTsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);

				// Copy units and storage format from the native container to the hourly one
				hourlyTsc.units = nativeTsc.units;
				hourlyTsc.storedAsdoubles = true;
				nativeTsc.storedAsdoubles = true;

				// Update the DSS pathname for the hourly record: clear D-part and set E-part to 1Hour
				DSSPathname pathname = new DSSPathname(nativeTsc.fullName);
				pathname.setDPart("");
				pathname.setEPart(TemperatureTargetTimeStep.REGULAR_HOURLY.toString());
				hourlyTsc.fullName = pathname.getPathname();

				// Write the hourly and native records; capture non-zero status codes as errors
				int hourlyStatus = DssFileManagerImpl.getDssFileManager().write(hourlyTsc);
				int nativeTimeStepStatus = DssFileManagerImpl.getDssFileManager().writeTS(nativeTsc, new StoreOptionImpl());

				// Accumulate error details for any failed write operations
				String errorSpecified = "";
				String statusCode = "";

				if (hourlyStatus != 0 && !hourlyTsc.allMissing()) {
					// Hourly write failed; accumulate its pathname and status code
					errorSpecified += hourlyTsc.fullName;
					statusCode = String.valueOf(hourlyStatus);

					if (nativeTimeStepStatus != 0) {
						// Both writes failed; append the native pathname to the error
						errorSpecified += " and " + nativeTsc.fullName;
						statusCode = String.valueOf(nativeTimeStepStatus);
					}
				} else if (nativeTimeStepStatus != 0 && !nativeTsc.allMissing()) {
					// Only the native write failed; record its details
					errorSpecified = nativeTsc.fullName;
					statusCode = String.valueOf(nativeTimeStepStatus);
				}

				// Throw an exception if any write failed, carrying the error details
				if (!errorSpecified.isEmpty()) {
					throw new TempTargetSaveFailedException(errorSpecified, nativeTsc.fileName, statusCode);
				}
			}

		} catch (HecMathException e) {
			// Log interpolation failures at SEVERE level; the DSS file handles are still closed below
			LOGGER.log(Level.SEVERE, e, () -> "Failed to convert timeseries: " + nativeTsc.fullName + " to hourly");

		} finally {
			// Always close DSS file handles to release resources, even on exception
			if (!nativeTsc.allMissing()) {
				DssFileManagerImpl.getDssFileManager().close(nativeTsc.fileName);
			}

			// Only close the hourly handle if it was successfully created
			if (hourlyTsc != null) {
				DssFileManagerImpl.getDssFileManager().close(hourlyTsc.fileName);
			}
		}
	}

	/**
	 * Reads all row values from the specified column of the time-series table model
	 * and returns them as a primitive double array. Null cell values are replaced with
	 * the HEC undefined double sentinel (RMAConst.HEC_UNDEFINED_DOUBLE).
	 *
	 * @param col the zero-based column index to read from the table model
	 * @return a double array of length equal to the table row count
	 */
	private double[] getUserDefinedValues(int col) {
		// Allocate the result array to match the number of rows in the table model
		double[] retVal = new double[_ttTableModel.getRowCount()];

		// Iterate over every row to read and convert the cell value in the target column
		for (int row = 0; row < _ttTableModel.getRowCount(); row++) {
			// Retrieve the cell value from the specified column of the current row
			Object val = _ttTableModel.getValueAt(row, col);

			// Use the HEC undefined sentinel for missing/null cell values
			double doubleVal = RMAConst.HEC_UNDEFINED_DOUBLE;
			if (val != null) {
				// Parse the non-null cell value to a primitive double
				doubleVal = Double.parseDouble(val.toString());
			}

			retVal[row] = doubleVal;
		}

		// Return the array of parsed double values for the specified column
		return retVal;
	}

	/**
	 * Loads the panel for the given ForecastSimGroup: clears any previous state,
	 * resets the selected set, and populates the upper temperature target table with
	 * all sets from the group. Enables the Import/Create button only when a group is set.
	 *
	 * @param fsg the ForecastSimGroup to display, or null to disable the panel
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg) {
		if (fsg != null) {
			// Clear any stale content from the previous group before repopulating
			clearPanel();
			_selectedTempTargetSet = null;
			_fsg = fsg;

			// Retrieve all temperature target sets from the new simulation group
			List<TemperatureTargetSet> tempTargetSets = fsg.getTemperatureTargetSets();

			// Clear the upper table via the typed model before adding new rows
			((TempTargetForecastTableModel) _tempTargetTable.getModel()).clearTempTargets();

			// Add a single-element row to the upper table for each temperature target set
			List<TemperatureTargetSet> temperatureTargetSets = new ArrayList<>(tempTargetSets);
			for (TemperatureTargetSet set : temperatureTargetSets) {
				// Add each set as a single-element row in the upper table
				((TempTargetForecastTableModel) _tempTargetTable.getModel()).addRow(new Vector<>(Collections.singletonList(set)));
			}
		}

		// The Import/Create button is only meaningful when a simulation group is active
		_createButton.setEnabled(fsg != null);
	}

	/**
	 * Builds the time-series data table for the given TemperatureTargetSet. Removes all
	 * existing columns, rebuilds the table model, adds a column per temperature target
	 * (named from the DSS F-part with units appended), configures double cell editors
	 * for data entry, sets editability based on user-defined status, loads row data from
	 * the model, and adjusts the auto-resize mode based on column count.
	 *
	 * @param temperatureTargetSet the TemperatureTargetSet whose data drives the table
	 */
	private void fillTempTargetTable(TemperatureTargetSet temperatureTargetSet) {
		// Enable the panel controls before populating the table
		setEnabled(true);
		_selectedTempTargetSet = temperatureTargetSet;

		// Remove all existing columns before rebuilding to avoid stale column definitions
		removeAllColumns();

		// Rebuild the table model and re-attach it to the table
		_ttTableModel = new TempTargetTableModel();
		_ttTable.setModel(_ttTableModel);
		_ttTableModel.addColumn("Date");

		// Retrieve time-series containers from the set for the current analysis window
		RunTimeWindow analysisTimeWindow = _fsg.getAnalysisPeriod().getRunTimeWindow();
		List<TimeSeriesContainer> temperatureTargetData = temperatureTargetSet.getTimeSeriesData(analysisTimeWindow);

		// Build a PARAMID_TEMP array for each data column (used for unit system lookups)
		int[] paramIds = new int[temperatureTargetData.size()];
		for (int i = 0; i < temperatureTargetData.size(); i++) {
			paramIds[i] = Parameter.PARAMID_TEMP;
		}

		// Add one column per time-series container, labelled with the F-part and unit string
		for (int column = 1; column <= temperatureTargetData.size(); column++) {
			// Derive a human-readable column name from the DSS F-part
			String columnName = getColumnNameFromFPart(temperatureTargetData.get(column - 1));

			// Fall back to the column index number when the F-part yields no usable name
			if (columnName == null || columnName.trim().isEmpty()) {
				columnName = "" + column;
			}

			// Match the raw unit string to a canonical display string and append to the header
			String units = temperatureTargetData.get(column - 1).units;
			units = Units.getBestMatch(units);
			columnName += " (" + units + ")";
			_ttTableModel.addColumn(columnName);

			// Only user-defined sets allow inline editing of the temperature values
			_ttTableModel.setColEnabled(temperatureTargetSet.isUserDefined(), column);
		}

		// The Date column is always read-only regardless of set type
		_ttTable.setColumnEnabled(false, TempTargetTableModel.DATE_COL_INDEX);

		// Install a double cell editor on every data column to enforce numeric input
		for (int col = 1; col < _ttTableModel.getColumnCount(); col++) {
			_ttTable.setDoubleCellEditor(col);
		}

		// Load all row data from the set into the model and trigger a full structure refresh
		_ttTableModel.setTempTargetSet(temperatureTargetSet, _fsg);
		_ttTableModel.fireTableStructureChanged();

		// Switch to no-auto-resize when there are more than 12 columns to enable horizontal scrolling
		if (_ttTable.getColumnCount() > 12) {
			_ttTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		} else {
			// Restore the default resize mode for tables with 12 or fewer columns
			_ttTable.setAutoResizeMode(_defaultResizeMode);
		}

		// Apply the minimum width to the Date column for readability
		_ttTable.setColumnWidth(TempTargetTableModel.DATE_COL_INDEX, DATE_COL_MIN_WIDTH);
	}

	/**
	 * Derives a human-readable column name from the F-part of the given
	 * TimeSeriesContainer's DSS pathname. If the F-part contains a pipe character,
	 * the segment before it is used as an index number with the "C:" prefix stripped
	 * and leading zeros removed. Returns the full F-part when no pipe is present.
	 *
	 * @param timeSeriesContainer the container whose DSS full name is parsed
	 * @return a column name string derived from the F-part, or null/empty if unavailable
	 */
	private String getColumnNameFromFPart(TimeSeriesContainer timeSeriesContainer) {
		// Parse the container's full DSS name into a pathname object to access individual parts
		DSSPathname pathname = new DSSPathname(timeSeriesContainer.fullName);

		// Retrieve the F-part as the starting point for the column name
		String fPart = pathname.getFPart();
		String retVal = fPart;

		if (fPart != null && fPart.contains("|")) {
			// Split the F-part on the pipe character to separate the index from the collection ID
			String[] split = fPart.split("\\|");
			if (split.length > 1) {
				// Use the segment before the pipe as the numeric index identifier
				String indexNum = split[0];

				// Remove the "C:" prefix added by the DSS collection naming convention
				indexNum = indexNum.replace("C:", "");

				// Strip leading zeros while preserving a single "0" if that is the full value
				String replaceRegex = "^0+(?!$)";
				retVal = indexNum.replaceFirst(replaceRegex, "");
			}
		}

		// Return the derived column name string
		return retVal;
	}

	/**
	 * Removes all columns from the time-series table by iterating the column model
	 * in reverse index order. Called before rebuilding the table for a new selection
	 * to prevent column count mismatches with the new model.
	 */
	private void removeAllColumns() {
		// Retrieve the table's column model to access and remove individual columns
		TableColumnModel columnModel = _ttTable.getColumnModel();

		// Iterate in reverse to avoid index shifting as columns are removed
		for (int col = columnModel.getColumnCount() - 1; col >= 0; col--) {
			columnModel.removeColumn(columnModel.getColumn(col));
		}
	}

	/**
	 * Overrides setVisible to trigger a table row selection refresh when the panel
	 * becomes visible, and to save the panel state when it is hidden.
	 *
	 * @param visible true to show the panel; false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		// Delegate the visibility change to the superclass before applying panel-specific logic
		super.setVisible(visible);

		if (visible) {
			// Re-apply the current table selection to refresh the detail tables on show
			tableRowSelected(_tempTargetTable.getSelectedRow());

		} else {
			// Persist any unsaved changes when the panel is hidden (e.g. tab switch)
			savePanel();
		}
	}

	/**
	 * Responds to row selection changes in the upper temperature target table.
	 * Clears the panel when the selection is cleared or the row value is null.
	 * Otherwise loads the selected TemperatureTargetSet into the info and time-series
	 * tables and synchronises the selection highlight and modified flags.
	 *
	 * @param row the zero-based index of the selected row, or -1 if deselected
	 */
	@Override
	protected void tableRowSelected(int row) {
		if (row == -1) {
			// No row selected; reset the panel to an empty state
			clearPanel();

		} else {
			// Retrieve the object stored in column 0 of the selected row
			Object value = _tempTargetTable.getValueAt(row, 0);

			if (value == null) {
				// Null row value; reset the panel to an empty state
				clearPanel();

			} else {
				// Cast the row value to the expected TemperatureTargetSet type
				TemperatureTargetSet set = (TemperatureTargetSet) value;

				// Populate both detail tables with the selected set's data
				fillTempTargetInfoTable(set);
				fillTempTargetTable(set);

				// Synchronise the table selection highlight to the newly loaded row
				_tempTargetTable.setRowSelectionInterval(row, row, false);
				_tempTargetTable.updateSelection(row, 0, false, false);

				// Clear the modified flag on the set itself after a clean load
				set.setModified(false);
			}
		}

		// Record the selected row index and reset the panel's modified state
		_topTableRowSelected = row;
		setModified(false);
	}

	/**
	 * Called when the delete button is clicked for a row in the upper temperature
	 * target table. Delegates to the delete method when both a simulation group is
	 * active and the row contains a valid value.
	 *
	 * @param rowToDelete the zero-based index of the row whose set should be deleted
	 */
	@Override
	public void tableRowDeleteClicked(int rowToDelete) {
		// Retrieve the value stored in column 0 of the row to be deleted
		Object value = _tempTargetTable.getValueAt(rowToDelete, 0);

		// Only proceed if a valid simulation group is active and the row holds a non-null value
		if (_fsg != null && value != null) {
			// Delegate to the delete method with the cast set and no overwrite flag
			delete((TemperatureTargetSet) value, false);
		}
	}

	/**
	 * Resets the lower panel to an empty state by committing and clearing all edits
	 * in both the info table and the time-series table, removing all columns, and
	 * disabling the panel when no simulation group is active.
	 */
	@Override
	public void clearPanel() {
		// Disable the panel entirely when there is no active simulation group
		if (_fsg == null) {
			setEnabled(false);
		}

		// Commit and discard any pending edits before clearing table content
		_ttTable.commitEdit(true);
		_ttInfoTable.commitEdit(true);

		// Clear all cell data and column definitions from the time-series table
		_ttTable.clearAll();
		removeAllColumns();

		// Clear the info table rows to remove any previously displayed metadata
		_ttInfoTable.deleteCells();
	}

	/**
	 * Removes the given TemperatureTargetSet from the active ForecastSimGroup's
	 * temperature target set list.
	 *
	 * @param fsg  the ForecastSimGroup from which the data is removed
	 * @param data the TemperatureTargetSet to remove
	 */
	@Override
	protected void removeData(ForecastSimGroup fsg, TemperatureTargetSet data) {
		// Delegate the removal to the simulation group's built-in remove method
		fsg.removeTemperatureTargetSet(data);
	}
}