package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;                                                     // Provides GridBagConstraints for specifying layout parameters in a GridBagLayout
import java.awt.GridBagLayout;                                                          // Provides GridBagLayout as the layout manager for the dialog content pane and sub-panels
import java.awt.Window;                                                                 // Provides Window as the parent component type accepted by the superclass constructor
import java.awt.event.ActionEvent;                                                      // Provides ActionEvent for the ButtonCmdPanelListener command button callback

import java.util.ArrayList;                                                             // Provides ArrayList for building and managing the add/delete pending ensemble set lists
import java.util.List;                                                                  // Provides the List interface for ordered collections of ensemble sets, BC data, and temp target sets
import java.util.Vector;                                                                // Provides Vector as the row data container required by RmaJTable's row append methods

import javax.swing.JButton;                                                             // Provides JButton for the "Add EnsembleSet" and "Remove EnsembleSet" action buttons
import javax.swing.JCheckBox;                                                           // Provides JCheckBox as the type returned by setCheckBoxCellEditor, used to detect checkbox changes
import javax.swing.JLabel;                                                              // Provides JLabel for the "Selected Ensemble Sets" header label and the info/status label
import javax.swing.JOptionPane;                                                         // Provides JOptionPane for displaying confirmation dialogs for delete and cancel-with-changes scenarios
import javax.swing.JPanel;                                                              // Provides JPanel as the container for the add/remove button rows
import javax.swing.JScrollPane;                                                         // Provides JScrollPane for wrapping the ensemble set selection list

import hec2.wat.model.WatSimulation;                                                    // Provides WatSimulation for identifying which simulation's ensemble sets are being edited
import rma.swing.ButtonCmdPanel;                                                        // Provides ButtonCmdPanel for the standard OK/Cancel button row at the bottom of the dialog
import rma.swing.ButtonCmdPanelListener;                                                // Provides ButtonCmdPanelListener for handling OK and Cancel button click events
import rma.swing.RmaInsets;                                                             // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJDialog;                                                            // Provides RmaJDialog as the base modal dialog class this window extends
import rma.swing.RmaJList;                                                              // Provides RmaJList for displaying and selecting ensemble sets in the current selection list
import rma.swing.RmaJTable;                                                             // Provides RmaJTable as the base table class for the BC and temperature target selection tables
import rma.swing.list.RmaListModel;                                                     // Provides RmaListModel as the typed list model used by the ensemble set selection list

import usbr.wat.plugins.actionpanel.model.forecast.BcData;                              // Provides BcData as the boundary condition data type displayed in the BC selection table
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;                         // Provides EnsembleSet as the data type managed and displayed in the selection list
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                    // Provides ForecastSimGroup as the top-level container for BC data and ensemble sets
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;                // Provides TemperatureTargetSet as the data type displayed in the temperature target selection table

/**
 * A modal dialog for creating and removing EnsembleSet instances within a
 * specific WAT simulation in the WTMP forecast action panel.
 *
 * The dialog presents two side-by-side checkbox tables — one for boundary condition
 * (BcData) sets and one for temperature target (TemperatureTargetSet) sets — from
 * which the user selects combinations. Each checked BC x temperature target pair that
 * does not already exist in the simulation group is eligible to be added as a new
 * EnsembleSet. The user clicks "Add EnsembleSet" to stage the new sets in the
 * selection list, and "Remove EnsembleSet" to stage deletions.
 *
 * Pending additions and deletions are accumulated in _esetsToAdd and _esetsToDelete
 * respectively and are not committed to the simulation group until the user clicks OK
 * (saveForm()). Clicking Cancel with unsaved changes prompts the user to confirm
 * discarding those changes.
 *
 * @see EnsembleSet
 * @see BcData
 * @see TemperatureTargetSet
 * @see ForecastSimGroup
 */
public class EditEnsembleSetWindow extends RmaJDialog {
	// Dynamic status label; reserved for future use (currently updated by the disabled tableRowsSelected path)
	private JLabel _infoLabel;

	// OK/Cancel button row at the bottom of the dialog
	private ButtonCmdPanel _cmdPanel;

	// Checkbox table listing all available boundary condition sets for selection
	private RmaJTable _bcTable;

	// Checkbox table listing all available temperature target sets for selection
	private RmaJTable _tempTargetSetTable;

	// Flag indicating whether the dialog was closed without committing changes
	private boolean _canceled;

	// The forecast simulation group whose ensemble sets are being edited
	private ForecastSimGroup _simGroup;

	// The scrollable list displaying the ensemble sets currently staged for the simulation
	private RmaJList<EnsembleSet> _selectionList;

	// Button that stages the selected BC x temperature target combinations as new ensemble sets
	private JButton _addButton;

	// Button that stages the selected ensemble sets in the list for deletion
	private JButton _deleteButton;

	// The JCheckBox component returned by the BC table's checkbox cell editor; used to detect check state changes
	private JCheckBox _bcTableCheckBox;

	// The JCheckBox component returned by the temperature target table's checkbox cell editor
	private JCheckBox _tempTargetTableCheckbox;

	// Accumulates ensemble sets staged for deletion; committed to the simulation group on OK
	private List<EnsembleSet> _esetsToDelete = new ArrayList<>();

	// Accumulates ensemble sets staged for addition; committed to the simulation group on OK
	private List<EnsembleSet> _esetsToAdd = new ArrayList<>();

	// The WAT simulation that scopes which ensemble sets are displayed and modified
	private WatSimulation _simulation;

	/**
	 * Constructs an EditEnsembleSetWindow modal dialog, builds all controls,
	 * wires listeners, packs the dialog to its preferred size, and centres it over the
	 * parent window.
	 *
	 * @param parent the Window over which this dialog is centred and to which it is
	 *               modal; passed to the RmaJDialog superclass constructor
	 */
	public EditEnsembleSetWindow(Window parent) {
		// Initialise the superclass as a modal dialog titled "Edit Ensemble Set"
		super(parent, "Edit Ensemble Set", true);

		// Build and lay out all Swing controls
		buildControls();

		// Attach checkbox, button, selection, and command panel listeners
		addListeners();

		// Size the dialog to its preferred dimensions and centre it over the parent
		pack();
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out all Swing controls within the dialog's content pane.
	 *
	 * The layout consists, top to bottom, of:
	 *   1. A tables panel containing the BC and temperature target checkbox tables
	 *      (built by buildTables(JPanel)).
	 *   2. An "Add EnsembleSet" button panel (initially disabled).
	 *   3. A "Selected Ensemble Sets" label and scrollable selection list.
	 *   4. A "Remove EnsembleSet" button panel (initially disabled).
	 *   5. A dynamic info label.
	 *   6. An OK/Cancel ButtonCmdPanel.
	 */
	private void buildControls() {
		// Use GridBagLayout for flexible component sizing across the content pane
		getContentPane().setLayout(new GridBagLayout());

		// Create and add the panel that hosts the two selection tables
		JPanel tablesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// Configure the tables panel to span the full row and fill all available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS0000;
		add(tablesPanel, gbc);

		// Populate the tables panel with the BC and temperature target checkbox tables
		buildTables(tablesPanel);

		// Create the first button panel for the "Add EnsembleSet" button
		JPanel buttonPanel = new JPanel(new GridBagLayout());

		// Configure the button panel to span the full row but take no vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0000;
		add(buttonPanel, gbc);

		// Add the "Add EnsembleSet" button; disabled until valid checkbox selections are made
		_addButton = new JButton("Add EnsembleSet");
		_addButton.setEnabled(false);

		// Configure the Add button constraints to not expand and remain anchored to the top
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		buttonPanel.add(_addButton, gbc);

		// Add the "Selected Ensemble Sets" header label above the selection list
		JLabel label = new JLabel("Selected Ensemble Sets");

		// Configure the label constraints to not expand and anchor to the top-left
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Create the scrollable ensemble set selection list and add it to the dialog
		_selectionList = new RmaJList<>();

		// Configure the selection list to expand and fill all available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JScrollPane(_selectionList), gbc);

		// Create the second button panel for the "Remove EnsembleSet" button
		buttonPanel = new JPanel(new GridBagLayout());

		// Configure the remove button panel to span the full row but take no vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0000;
		add(buttonPanel, gbc);

		// Add the "Remove EnsembleSet" button; disabled until an item in the list is selected
		_deleteButton = new JButton("Remove EnsembleSet");
		_deleteButton.setEnabled(false);

		// Configure the Remove button constraints to not expand and remain anchored to the top
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		buttonPanel.add(_deleteButton, gbc);

		// Add the dynamic info label below the delete button
		_infoLabel = new JLabel();

		// Configure the info label to span the full row width but take no vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_infoLabel, gbc);

		// Add the OK/Cancel command panel anchored to the bottom-left of its cell
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);

		// Configure the command panel to span the full row and fill horizontally
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		add(_cmdPanel, gbc);
	}

	/**
	 * Builds the two checkbox-enabled selection tables — one for boundary condition sets
	 * and one for temperature target sets — and adds them side by side to the supplied
	 * tables panel.
	 *
	 * Each table has two columns: a checkbox column (column 0, editable) and a data
	 * column (column 1, read-only). Row height is increased by 5 pixels for readability,
	 * and irrelevant popup menu options (fill, insert/append, sum) are removed. The
	 * JCheckBox component returned by setCheckBoxCellEditor is stored so action listeners
	 * can detect checkbox state changes.
	 *
	 * @param tablesPanel the JPanel into which both table scroll panes are added
	 */
	private void buildTables(JPanel tablesPanel) {
		// --- Boundary Condition Sets table ---

		// Only the checkbox column (col 0) is editable; the BC name column is read-only
		String[] headers = new String[]{"Select", "Boundary Conditions"};
		_bcTable = new RmaJTable(this, headers) {
			public boolean isCellEditable(int row, int col) {
				// Restrict editing to the checkbox column only
				return col == 0;
			}
		};

		// Increase row height slightly for readability and install the checkbox cell editor
		_bcTable.setRowHeight(_bcTable.getRowHeight() + 5);
		_bcTableCheckBox = _bcTable.setCheckBoxCellEditor(0);

		// Remove popup menu options that are not appropriate for a selection-only table
		_bcTable.removePopuMenuFillOptions();
		_bcTable.removePopupMenuInsertAppendOnly();
		_bcTable.removePopupMenuSumOptions();

		// Configure the BC table to fill the left half of the tables panel
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;      // Occupy one column unit; the temperature target table takes the other
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;

		// Add the BC table scroll pane to the left side of the tables panel
		tablesPanel.add(_bcTable.getScrollPane(), gbc);

		// --- Temperature Target Sets table ---

		// Only the checkbox column (col 0) is editable; the temperature target name column is read-only
		headers = new String[]{"Select", "Temperature Target Sets"};
		_tempTargetSetTable = new RmaJTable(this, headers) {
			public boolean isCellEditable(int row, int col) {
				// Restrict editing to the checkbox column only
				return col == 0;
			}
		};

		// Increase row height slightly and install the checkbox cell editor
		_tempTargetSetTable.setRowHeight(_tempTargetSetTable.getRowHeight() + 5);
		_tempTargetTableCheckbox = _tempTargetSetTable.setCheckBoxCellEditor(0);

		// Remove popup menu options that are not appropriate for a selection-only table
		_tempTargetSetTable.removePopuMenuFillOptions();
		_tempTargetSetTable.removePopupMenuInsertAppendOnly();
		_tempTargetSetTable.removePopupMenuSumOptions();

		// Configure the temperature target table to fill the right half of the tables panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;

		// Add the temperature target table scroll pane to the right side of the tables panel
		tablesPanel.add(_tempTargetSetTable.getScrollPane(), gbc);
	}

	/**
	 * Wires all event listeners for the dialog:
	 *   - The "Add EnsembleSet" button calls addEnsembleSets().
	 *   - The "Remove EnsembleSet" button calls deleteEnsembleSets().
	 *   - Both table selection models call tableRowsSelected() on change.
	 *   - The selection list calls selectionListSelected() to enable/disable the delete button.
	 *   - Both checkbox components call setAddButtonState() on action.
	 *   - The OK button validates and saves; the Cancel button prompts for discard
	 *     confirmation if changes are pending.
	 */
	private void addListeners() {
		// Stage new ensemble sets when the Add button is clicked
		_addButton.addActionListener(e -> addEnsembleSets());

		// Stage selected ensemble sets for deletion when the Remove button is clicked
		_deleteButton.addActionListener(e -> deleteEnsembleSets());

		// Update the info label and add button state when BC table selection changes
		_bcTable.getSelectionModel().addListSelectionListener(e -> tableRowsSelected());

		// Update the info label and add button state when temperature target table selection changes
		_tempTargetSetTable.getSelectionModel().addListSelectionListener(e -> tableRowsSelected());

		// Enable or disable the Remove button based on whether an item is selected in the list
		_selectionList.addListSelectionListener(e -> selectionListSelected());

		// Re-evaluate the Add button enabled state whenever a BC checkbox is toggled
		_bcTableCheckBox.addActionListener(e -> setAddButtonState());

		// Re-evaluate the Add button enabled state whenever a temperature target checkbox is toggled
		_tempTargetTableCheckbox.addActionListener(e -> setAddButtonState());

		// Register a command panel listener to handle OK and Cancel button activations
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				// Determine which button was activated and respond accordingly
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Save pending additions and deletions to the simulation group and close
						if (saveForm()) {
							// Mark the dialog as not canceled and hide it
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Prompt the user to confirm discarding changes if any have been staged
						if (isModified()) {
							int opt = JOptionPane.showConfirmDialog(EditEnsembleSetWindow.this,
									"Ensemble Sets have changed. Discard Changes?",
									"Changes Made", JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE);
							if (opt != JOptionPane.YES_OPTION) {
								// User chose not to discard; return to the dialog without closing
								return;
							}
						}

						// Close the dialog without saving; _canceled remains true from fillForm
						_canceled = false;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Evaluates whether the "Add EnsembleSet" button should be enabled based on the
	 * current checkbox selections in both tables.
	 *
	 * Commits any in-progress cell edits in both tables before reading checkbox values,
	 * then enables the button only when at least one BC set and at least one temperature
	 * target set are checked.
	 */
	private void setAddButtonState() {
		// Commit any pending checkbox edits before reading the current selection state
		_bcTable.commitEdit(true);
		_tempTargetSetTable.commitEdit(true);

		// Retrieve the currently checked items from each table
		List<BcData> bcData = getSelectedBcData();
		List<TemperatureTargetSet> ttSets = getSelectedTempTargetSets();

		// The Add button is only meaningful when at least one item is checked in each table
		boolean enabled = !bcData.isEmpty() && !ttSets.isEmpty();
		_addButton.setEnabled(enabled);
	}

	/**
	 * Enables or disables the "Remove EnsembleSet" button based on whether any item
	 * is currently selected in the ensemble set selection list.
	 */
	private void selectionListSelected() {
		// Retrieve the index of the currently selected item in the ensemble set list
		int idx = _selectionList.getSelectedIndex();

		// Enable the Remove button only when a list item is selected
		_deleteButton.setEnabled(idx > -1);
	}

	/**
	 * Stages the selected ensemble sets in the selection list for deletion and removes
	 * them from the list model after user confirmation.
	 *
	 * A confirmation dialog listing the names of all selected sets is shown before any
	 * removal is performed. Removal iterates in reverse index order to avoid index
	 * shifting. Each removed set is added to _esetsToDelete and the modified flag is set.
	 */
	private void deleteEnsembleSets() {
		// Retrieve all ensemble sets currently selected in the list
		List<EnsembleSet> selectedEsets = _selectionList.getSelectedValuesList();

		// Nothing to delete if no items are selected in the list
		if (selectedEsets == null || selectedEsets.isEmpty()) {
			return;
		}

		// Build a confirmation message listing the names of each selected ensemble set
		StringBuilder builder = new StringBuilder();
		builder.append("<html>Ok to Delete the following Ensemble Sets:<br>");
		for (int i = 0; i < selectedEsets.size(); i++) {
			// Append each selected ensemble set name on its own line
			builder.append(selectedEsets.get(i).getName());
			builder.append("<br>");
		}

		// Display the confirmation dialog before performing any deletions
		int opt = JOptionPane.showConfirmDialog(this, builder.toString(), "Confirm Delete",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (opt == JOptionPane.YES_OPTION) {
			// Retrieve the list model so elements can be removed from the visible list
			RmaListModel<EnsembleSet> esetModel = (RmaListModel<EnsembleSet>) _selectionList.getModel();
			EnsembleSet eset;

			// Iterate in reverse to avoid index-shifting issues when removing elements
			for (int i = selectedEsets.size() - 1; i >= 0; i--) {
				eset = selectedEsets.get(i);

				// Stage the set for deletion and remove it from the visible list model
				_esetsToDelete.add(eset);
				esetModel.removeElement(eset);
				setModified(true);
			}

			// Refresh the list UI to reflect the removals
			_selectionList.revalidate();
		}
	}

	/**
	 * Creates and stages new EnsembleSet instances for every checked BC x temperature
	 * target combination that does not already exist — either in the simulation group or
	 * in the pending-add list — and adds them to the selection list.
	 *
	 * For each BC-temperature target pair, a new EnsembleSet named
	 * "<bc-name>-<tts-name>" is constructed and added to _esetsToAdd and the list model.
	 * Pairs that already exist in the simulation group or in the pending-add staging list
	 * are silently skipped. The modified flag is set for every new set added.
	 */
	private void addEnsembleSets() {
		// Retrieve all currently checked BC and temperature target sets
		List<BcData> bcData = getSelectedBcData();
		List<TemperatureTargetSet> ttsData = getSelectedTempTargetSets();

		// Declare variables for use during the combination loop
		BcData bc;
		TemperatureTargetSet tts;
		EnsembleSet ensembleSet, existingEset;

		// Build one EnsembleSet for every BC x temperature target combination
		for (int o = 0; o < bcData.size(); o++) {
			bc = bcData.get(o);

			for (int m = 0; m < ttsData.size(); m++) {
				tts = ttsData.get(m);

				// Check whether this combination already exists in the simulation group
				existingEset = _simGroup.getEnsembleSetFor(_simulation, bc, tts);

				if (existingEset == null) {
					// Also check whether it has already been staged for addition in this session
					existingEset = getEnsembleSetFor(bc, tts);

					if (existingEset == null) {
						// This combination is genuinely new; create and stage it
						ensembleSet = new EnsembleSet();

						// Name the ensemble set by combining the BC and temperature target names
						ensembleSet.setName(bc.getName() + "-" + tts.getName());
						ensembleSet.setSelectedBcData(bc);
						ensembleSet.setSelectedTemperatureTargetSets(tts);
						_esetsToAdd.add(ensembleSet);

						// Add the new set to the visible selection list model
						((RmaListModel) _selectionList.getModel()).addElement(ensembleSet);
						setModified(true);
					}
				}
			}
		}

		// Refresh the list UI to show the newly added ensemble sets
		_selectionList.revalidate();
	}

	/**
	 * Searches the current selection list model for an EnsembleSet that references the
	 * given BcData and TemperatureTargetSet by identity.
	 *
	 * Used by addEnsembleSets() to avoid staging duplicate sets within the same editing
	 * session, even before they are committed to the simulation group.
	 *
	 * @param bc  the BcData to match by identity
	 * @param tts the TemperatureTargetSet to match by identity
	 * @return    the matching EnsembleSet from the list model, or null if no match is found
	 */
	private EnsembleSet getEnsembleSetFor(BcData bc, TemperatureTargetSet tts) {
		// Retrieve the current list model to search for an existing matching ensemble set
		RmaListModel<EnsembleSet> model = (RmaListModel<EnsembleSet>) _selectionList.getModel();
		EnsembleSet eset;

		// Iterate over every ensemble set currently in the list model
		for (int i = 0; i < model.size(); i++) {
			eset = (EnsembleSet) model.get(i);

			// Use identity comparison (==) since ensemble sets are unique object instances
			if (eset.getBcData() == bc && eset.getTemperatureTargetSet() == tts) {
				// Match found; return the existing ensemble set immediately
				return eset;
			}
		}

		// No matching ensemble set was found in the list model
		return null;
	}

	/**
	 * Updates the info label with the number of new ensemble sets that would be created
	 * by the current BC and temperature target checkbox selections.
	 *
	 * Note: This method is currently disabled by an early return guard (if (true) return;)
	 * and has no effect at runtime. It is retained as a placeholder for future
	 * re-enablement. When active, it would compute the count of BC x temperature target
	 * combinations not already present in the simulation group or the pending-add list,
	 * and display that count in the info label.
	 */
	private void tableRowsSelected() {
		// Early return guard: this method is intentionally disabled pending future re-enablement
		if (true) {
			return;
		}

		// Retrieve all currently checked items from both selection tables
		List<BcData> bcData = getSelectedBcData();
		List<TemperatureTargetSet> ttsData = getSelectedTempTargetSets();

		// Start with the total Cartesian product count and subtract existing combinations
		int esCnt = bcData.size() * ttsData.size();

		// Reduce the count for each combination that already exists in the group or staging list
		for (int o = 0; o < bcData.size(); o++) {
			BcData bc = bcData.get(o);

			for (int m = 0; m < ttsData.size(); m++) {
				TemperatureTargetSet tts = ttsData.get(m);

				// Decrement count if the combination already exists in the simulation group
				if (_simGroup.getEnsembleSetFor(_simulation, bc, tts) != null) {
					esCnt--;
				}
				// Also decrement if the combination has already been staged for addition
				else if (getEnsembleSetFor(bc, tts) != null) {
					esCnt--;
				}
			}
		}

		if (esCnt > 0) {
			// Display how many new ensemble sets the current selections would create
			_infoLabel.setText("Selections will create " + esCnt + " Ensemble Sets");
		} else {
			// No new combinations would be created; clear the info label
			_infoLabel.setText("");
		}
	}

	/**
	 * Commits all pending additions and deletions to the simulation group.
	 *
	 * Iterates _esetsToDelete and calls ForecastSimGroup.deleteEnsembleSet for each,
	 * then iterates _esetsToAdd and calls ForecastSimGroup.addEnsembleSet for each.
	 *
	 * @return true always, indicating the save completed without error
	 */
	private boolean saveForm() {
		// Apply all staged deletions to the simulation group
		for (int i = 0; i < _esetsToDelete.size(); i++) {
			_simGroup.deleteEnsembleSet(_simulation, _esetsToDelete.get(i));
		}

		// Apply all staged additions to the simulation group
		for (int i = 0; i < _esetsToAdd.size(); i++) {
			_simGroup.addEnsembleSet(_simulation, _esetsToAdd.get(i));
		}

		// Indicate that the save operation completed successfully
		return true;
	}

	/**
	 * Populates all controls with data from the given simulation group and WAT simulation,
	 * resetting all staged changes and the modified flag before loading.
	 *
	 * The BC and temperature target tables are cleared and reloaded with unchecked rows.
	 * The selection list is replaced with a new RmaListModel containing the ensemble sets
	 * currently registered for the given simulation. If no ensemble sets exist for the
	 * simulation, an empty list is used.
	 *
	 * @param simulationGroup the ForecastSimGroup whose BC data, temperature target sets,
	 *                        and ensemble sets are loaded into the controls; must not be null
	 * @param sim             the WatSimulation that scopes which ensemble sets are
	 *                        displayed; must not be null
	 */
	public void fillForm(ForecastSimGroup simulationGroup, WatSimulation sim) {
		// Store references to the simulation group and simulation for use in subsequent operations
		_simGroup = simulationGroup;
		_simulation = sim;

		// Retrieve the BC data and temperature target sets from the simulation group
		List<BcData> bcDatas = simulationGroup.getBcData();
		List<TemperatureTargetSet> tempTargetSets = simulationGroup.getTemperatureTargetSets();

		// Clear any previously displayed BC rows and reload from the simulation group
		_bcTable.deleteCells();
		Vector<Object> row;

		// Populate the BC table with one unchecked row per BC data entry
		for (int i = 0; i < bcDatas.size(); i++) {
			row = new Vector<>();

			// Column 0: unchecked checkbox; column 1: the BcData object
			row.add(Boolean.FALSE);
			row.add(bcDatas.get(i));
			_bcTable.appendRow(row);
		}

		// Clear any previously displayed temperature target rows and reload
		_tempTargetSetTable.deleteCells();

		// Populate the temperature target table with one unchecked row per target set entry
		for (int i = 0; i < tempTargetSets.size(); i++) {
			row = new Vector<>();

			// Column 0: unchecked checkbox; column 1: the TemperatureTargetSet object
			row.add(Boolean.FALSE);
			row.add(tempTargetSets.get(i));
			_tempTargetSetTable.appendRow(row);
		}

		// Retrieve the current ensemble sets for this simulation; default to empty list if null
		List<EnsembleSet> esets = simulationGroup.getEnsembleSets(_simulation);
		if (esets == null) {
			esets = new ArrayList<>();
		}

		// Replace the list model with a fresh model built from the current ensemble sets
		RmaListModel<EnsembleSet> newModel = new RmaListModel<>(true, esets);
		_selectionList.setModel(newModel);

		// Reset the modified flag after loading to establish a clean baseline
		setModified(false);
	}

	/**
	 * Checks the checkbox in column 0 of the given table for the row whose column-1
	 * value matches objToSelect by identity.
	 *
	 * Used to programmatically select a specific item in one of the checkbox tables.
	 * Stops at the first matching row; does nothing if no row matches.
	 *
	 * @param table       the RmaJTable in which to find and check the row
	 * @param objToSelect the object to match against column-1 values by identity (==)
	 */
	private void selectTableRow(RmaJTable table, Object objToSelect) {
		// Get the total number of rows to iterate over
		int rowCnt = table.getRowCount();

		// Search each row for a column-1 value that matches the target object by identity
		for (int r = 0; r < rowCnt; r++) {
			if (table.getValueAt(r, 1) == objToSelect) {
				// Set the checkbox in column 0 to true for the matching row
				table.setValueAt(Boolean.TRUE, r, 0);

				// Stop searching after the first match is found
				return;
			}
		}
	}

	/**
	 * Returns whether the dialog was closed without committing changes via OK.
	 *
	 * @return true if the dialog has not been submitted with OK;
	 *         false after a successful OK submission
	 */
	public boolean isCanceled() {
		// Return the canceled flag set during dialog close operations
		return _canceled;
	}

	/**
	 * Builds and returns the complete list of EnsembleSet instances that correspond to
	 * the currently checked BC x temperature target combinations, incorporating any
	 * existing ensemble sets from the simulation group.
	 *
	 * For each checked combination:
	 *   - If an ensemble set already exists in the simulation group for that combination,
	 *     the existing set is included.
	 *   - Otherwise, a new EnsembleSet is created and included.
	 *
	 * Additionally, any ensemble sets in the simulation group whose BC or temperature
	 * target is no longer checked in the respective table are removed from the result.
	 *
	 * @return a List of EnsembleSet objects reflecting the current checkbox selections;
	 *         empty if no combinations are checked
	 */
	public List<EnsembleSet> getEnsembleSets() {
		// Initialize the list that will accumulate all ensemble sets for the current selections
		List<EnsembleSet> selectedEnsembleSets = new ArrayList<>();

		// Retrieve all currently checked BC and temperature target sets
		List<BcData> bcData = getSelectedBcData();
		List<TemperatureTargetSet> ttsData = getSelectedTempTargetSets();

		// Declare variables for use during the combination loop
		BcData bc;
		TemperatureTargetSet tts;
		EnsembleSet ensembleSet, existingEset;

		// For each checked BC x temperature target pair, include the existing or a new ensemble set
		for (int o = 0; o < bcData.size(); o++) {
			bc = bcData.get(o);

			for (int m = 0; m < ttsData.size(); m++) {
				tts = ttsData.get(m);

				// Check whether this BC-target combination already exists in the simulation group
				existingEset = _simGroup.getEnsembleSetFor(_simulation, bc, tts);

				if (existingEset == null) {
					// No existing set; create a new one to represent this combination
					ensembleSet = new EnsembleSet();

					// Name the new set by combining the BC and temperature target names
					ensembleSet.setName(bc.getName() + "-" + tts.getName());
					ensembleSet.setSelectedBcData(bc);
					ensembleSet.setSelectedTemperatureTargetSets(tts);
					selectedEnsembleSets.add(ensembleSet);
				} else {
					// Reuse the existing ensemble set for this combination
					selectedEnsembleSets.add(existingEset);
				}
			}
		}

		// Retrieve all ensemble sets currently registered for this simulation
		List<EnsembleSet> esets = _simGroup.getEnsembleSets(_simulation);
		if (esets == null) {
			esets = new ArrayList<>();
		}

		// Identify simulation-group sets whose BC or temperature target is no longer selected
		List<EnsembleSet> esetsToRemove = new ArrayList<>();
		for (int i = 0; i < esets.size(); i++) {
			ensembleSet = esets.get(i);
			bc = ensembleSet.getBcData();
			tts = ensembleSet.getTemperatureTargetSet();

			if (!isSelected(bc, tts)) {
				// This combination is no longer checked; mark it for exclusion from the result
				esetsToRemove.add(ensembleSet);
			}
		}

		// Remove all deselected simulation-group sets from the result list
		selectedEnsembleSets.removeAll(esetsToRemove);

		// Return the final list of ensemble sets reflecting the current selections
		return selectedEnsembleSets;
	}

	/**
	 * Returns whether both the given BcData and TemperatureTargetSet are currently
	 * checked in their respective tables.
	 *
	 * @param bc  the BcData to check for selection in the BC table
	 * @param tts the TemperatureTargetSet to check for selection in the temperature target table
	 * @return    true if both items are checked in their respective tables; false otherwise
	 */
	private boolean isSelected(BcData bc, TemperatureTargetSet tts) {
		// Both the BC and temperature target must be checked for the combination to be selected
		return isSelected(_bcTable, bc) && isSelected(_tempTargetSetTable, tts);
	}

	/**
	 * Returns whether the given object is checked (column 0 is true) in the specified
	 * table, matching by identity in column 1.
	 *
	 * A row is considered checked if its column-0 value is Boolean.TRUE or its toString()
	 * representation equals "true" (to handle string representations returned by some
	 * table cell editors).
	 *
	 * @param table the RmaJTable to search
	 * @param obj   the object to find by identity in column 1
	 * @return      true if a row with a matching column-1 value has its checkbox checked;
	 *              false if no match is found or the matching row is unchecked
	 */
	private boolean isSelected(RmaJTable table, Object obj) {
		// Get the total number of rows to search in the given table
		int rowCnt = table.getRowCount();
		Object selected;

		// Iterate over each row looking for a column-1 value that matches the target object
		for (int r = 0; r < rowCnt; r++) {
			if (table.getValueAt(r, 1) == obj) {
				// Retrieve the checkbox value from column 0 of the matching row
				selected = table.getValueAt(r, 0);

				// Accept both the Boolean object and its string representation for robustness
				if (selected == Boolean.TRUE || "true".equals(selected.toString())) {
					// The matching row is checked; return true immediately
					return true;
				}
			}
		}

		// No matching checked row was found; return false
		return false;
	}

	/**
	 * Collects and returns the TemperatureTargetSet items from all checked rows in the
	 * temperature target selection table.
	 *
	 * A row is considered checked if its column-0 value is Boolean.TRUE or its toString()
	 * representation equals "true".
	 *
	 * @return a List of TemperatureTargetSet objects for every checked row;
	 *         empty if no rows are checked
	 */
	private List<TemperatureTargetSet> getSelectedTempTargetSets() {
		// Get the total number of rows currently displayed in the temperature target table
		int rowCnt = _tempTargetSetTable.getNumRows();

		// Declare a variable to hold the checkbox cell value during each iteration
		Object obj;

		// Initialize the list that will accumulate all selected temperature target sets
		List<TemperatureTargetSet> selectedTts = new ArrayList<>();

		// Iterate over every row in the temperature target table to check its selection state
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from column 0 of the current row
			obj = _tempTargetSetTable.getValueAt(r, 0);

			// Accept both the Boolean object and its string representation for robustness
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Column 1 holds the TemperatureTargetSet object for this row
				selectedTts.add((TemperatureTargetSet) _tempTargetSetTable.getValueAt(r, 1));
			}
		}

		// Return the list of all checked temperature target sets
		return selectedTts;
	}

	/**
	 * Collects and returns the BcData items from all checked rows in the boundary
	 * condition selection table.
	 *
	 * A row is considered checked if its column-0 value is Boolean.TRUE or its toString()
	 * representation equals "true".
	 *
	 * @return a List of BcData objects for every checked row; empty if no rows are checked
	 */
	private List<BcData> getSelectedBcData() {
		// Get the total number of rows currently displayed in the BC table
		int rowCnt = _bcTable.getNumRows();

		// Declare a variable to hold the checkbox cell value during each iteration
		Object obj;

		// Initialize the list that will accumulate all selected BC data objects
		List<BcData> selectedBcData = new ArrayList<>();

		// Iterate over every row in the BC table to check its selection state
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from column 0 of the current row
			obj = _bcTable.getValueAt(r, 0);

			// Accept both the Boolean object and its string representation for robustness
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Column 1 holds the BcData object for this row
				selectedBcData.add((BcData) _bcTable.getValueAt(r, 1));
			}
		}

		// Return the list of all checked BC data objects
		return selectedBcData;
	}
}