package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;                                             // Provides GridBagConstraints for specifying layout parameters in a GridBagLayout
import java.awt.GridBagLayout;                                                  // Provides GridBagLayout as the layout manager for the dialog content pane and tables panel
import java.awt.Window;                                                         // Provides Window as the parent component type accepted by the superclass constructor

import java.util.ArrayList;                                                     // Provides ArrayList for building the lists of selected operations and meteorology data
import java.util.List;                                                          // Provides the List interface for ordered collections of BC, operations, and meteorology data items
import java.util.Vector;                                                        // Provides Vector as the row data container required by RmaJTable's row append methods

import javax.swing.JLabel;                                                      // Provides JLabel for displaying the dynamic BC set count information message
import javax.swing.JOptionPane;                                                 // Provides JOptionPane for showing validation error messages when required selections are missing
import javax.swing.JPanel;                                                      // Provides JPanel as the container for the side-by-side operations and meteorology tables

import rma.swing.ButtonCmdPanel;                                                // Provides ButtonCmdPanel for the standard OK/Cancel button row at the bottom of the dialog
import rma.swing.RmaInsets;                                                     // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJTable;                                                     // Provides RmaJTable as the base Swing table class for the operations and meteorology selection tables

import usbr.wat.plugins.actionpanel.model.forecast.BcData;                      // Provides BcData as the output data type constructed from each selected operations-meteorology pair
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Provides ForecastSimGroup for retrieving the available operations and meteorology data to display
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;             // Provides MeteorlogicData as the data type for rows in the meteorology selection table
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;              // Provides OperationsData as the data type for rows in the operations selection table

/**
 * A modal dialog that allows the user to select one or more operations records and one
 * or more meteorology records in order to generate boundary condition (BC) sets for the
 * WTMP forecast action panel.
 *
 * The dialog presents two side-by-side {@link RmaJTable} instances — one for operations
 * and one for meteorology — each with a checkbox column in the first position. Every
 * checked operations–meteorology combination produces one {@link BcData} item named
 * {@code "<ops-name>-<met-name>"}. A live info label below the tables displays the
 * number of BC sets that the current selections would generate.
 *
 * The OK button is enabled at all times but triggers validation: at least one operations
 * row and one meteorology row must be checked before the form can be submitted. Clicking
 * Cancel sets the {@code _canceled} flag and hides the dialog without producing any data.
 *
 * Extends {@link ImportForecastWindow} to integrate with the common
 * {@link AbstractForecastPanel} import workflow.
 *
 * @see BcPanel
 * @see BcData
 * @see ImportForecastWindow
 */
public class CreateBcWindow extends ImportForecastWindow {
	// The forecast simulation group from which available operations and meteorology data are drawn
	private final ForecastSimGroup _fsg;

	// The OK/Cancel button row at the bottom of the dialog
	private ButtonCmdPanel _cmdPanel;

	// The checkbox table listing all available operations records for selection
	private RmaJTable _opsTable;

	// The checkbox table listing all available meteorology records for selection
	private RmaJTable _metTable;

	// Label that dynamically displays the number of BC sets that will be created based on selections
	private JLabel _infoLabel;

	/**
	 * Constructs a {@code CreateBcWindow} modal dialog, builds all controls, wires
	 * listeners, packs the dialog to its preferred size, and centres it over the
	 * parent window.
	 *
	 * @param fsg    the {@link ForecastSimGroup} whose operations and meteorology data
	 *               are populated into the selection tables; must not be {@code null}
	 * @param parent the {@link Window} over which this dialog is centred; passed to
	 *               the {@link ImportForecastWindow} superclass constructor
	 */
	public CreateBcWindow(ForecastSimGroup fsg, Window parent) {
		// Initialise the superclass as a modal dialog with the given title
		super(parent, "Create Boundary Conditions", true);

		// Store the simulation group for use when filling the tables and building BC data
		_fsg = fsg;

		// Build and lay out all Swing controls
		buildControls();

		// Attach selection and command button listeners
		addListeners();

		// Size the dialog to its preferred dimensions and centre it over the parent
		pack();
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out all Swing controls within the dialog's content pane.
	 *
	 * The layout consists of three vertically stacked regions:
	 *
	 *   A tables panel containing the side-by-side operations and meteorology
	 *       selection tables (built by {@link #buildTables(JPanel)}).
	 *   An info label that reports the number of BC sets the current selections
	 *       will generate.
	 *   A {@link ButtonCmdPanel} providing OK and Cancel buttons.
	 *
	 */
	private void buildControls() {
		// Use GridBagLayout for the content pane to allow flexible component sizing
		getContentPane().setLayout(new GridBagLayout());

		// Create the panel that holds the two selection tables side by side
		JPanel tablesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS0000;
		add(tablesPanel, gbc);

		// Populate the tables panel with the operations and meteorology selection tables
		buildTables(tablesPanel);

		// Add the info label below the tables; it does not claim vertical space
		_infoLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_infoLabel, gbc);

		// Add the OK/Cancel button panel anchored to the bottom-left of its cell
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
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
	 * Builds the two checkbox-enabled selection tables — one for operations and one for
	 * meteorology — and adds them side by side to the supplied tables panel.
	 *
	 * Each table has two columns: a checkbox column (column 0, editable) and a data
	 * column (column 1, read-only). Row height is increased by 5 pixels for readability,
	 * and superfluous popup menu options (fill, insert/append, sum) are removed.
	 *
	 * @param tablesPanel the {@link JPanel} into which both table scroll panes are added
	 */
	private void buildTables(JPanel tablesPanel) {
		// --- Operations selection table ---

		// Only the checkbox column (col 0) is editable; the operations name column is read-only
		String[] headers = new String[]{"Select", "Operations"};
		_opsTable = new RmaJTable(this, headers) {
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}
		};

		// Increase row height slightly and configure column 0 as a checkbox editor
		_opsTable.setRowHeight(_opsTable.getRowHeight() + 5);
		_opsTable.setCheckBoxCellEditor(0);

		// Remove popup menu options that are not relevant for a selection-only table
		_opsTable.removePopuMenuFillOptions();
		_opsTable.removePopupMenuInsertAppendOnly();
		_opsTable.removePopupMenuSumOptions();

		// Add the operations table to the left half of the tables panel
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_opsTable.getScrollPane(), gbc);

		// --- Meteorology selection table ---

		// Only the checkbox column (col 0) is editable; the meteorology name column is read-only
		headers = new String[]{"Select", "Meteorology"};
		_metTable = new RmaJTable(this, headers) {
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}
		};

		// Increase row height slightly and configure column 0 as a checkbox editor
		_metTable.setRowHeight(_metTable.getRowHeight() + 5);
		_metTable.setCheckBoxCellEditor(0);

		// Remove popup menu options that are not relevant for a selection-only table
		_metTable.removePopuMenuFillOptions();
		_metTable.removePopupMenuInsertAppendOnly();
		_metTable.removePopupMenuSumOptions();

		// Add the meteorology table to the right half of the tables panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_metTable.getScrollPane(), gbc);
	}

	/**
	 * Wires all event listeners for the dialog:
	 *
	 *   Both table selection models call {@link #tableRowsSelected()} when any row
	 *       is selected or deselected, updating the info label.
	 *   The OK button validates the form and, if valid, saves it and closes the
	 *       dialog with {@code _canceled = false}.
	 *   The Cancel button sets {@code _canceled = true} and hides the dialog.
	 *
	 */
	private void addListeners() {
		// Update the info label whenever the operations table selection changes
		_opsTable.getSelectionModel().addListSelectionListener(e -> tableRowsSelected());

		// Update the info label whenever the meteorology table selection changes
		_metTable.getSelectionModel().addListSelectionListener(e -> tableRowsSelected());

		_cmdPanel.addCmdPanelListener(e ->
		{
			switch (e.getID()) {
				case ButtonCmdPanel.OK_BUTTON:
					// Validate that at least one ops and one met row are checked before submitting
					if (validForm()) {
						saveForm();
						_canceled = false;
						setVisible(false);
					}
					break;

				case ButtonCmdPanel.CANCEL_BUTTON:
					// Mark as cancelled and hide the dialog without producing any BC data
					_canceled = true;
					setVisible(false);
					break;
			}
		});
	}

	/**
	 * Updates the info label to reflect the number of boundary condition sets that
	 * the current checkbox selections would generate.
	 *
	 * The count is the Cartesian product of the number of checked operations rows and
	 * the number of checked meteorology rows. If the count is zero (no selections in
	 * either table), the label is cleared.
	 */
	private void tableRowsSelected() {
		List<OperationsData> opsData = getSelectedOpsData();
		List<MeteorlogicData> metData = getSelectedMetData();

		// BC set count equals the number of ops selections multiplied by the number of met selections
		int bcCnt = opsData.size() * metData.size();

		if (bcCnt > 0) {
			// Display the number of BC sets that will be created
			_infoLabel.setText("Selections will create " + bcCnt + " Boundary Condition Sets");
		} else {
			// No complete selection yet; clear the info label
			_infoLabel.setText("");
		}
	}

	/**
	 * Validates that the user has checked at least one operations row and at least one
	 * meteorology row before the form can be submitted.
	 *
	 * Displays a plain-message {@link JOptionPane} dialog if either selection is empty,
	 * prompting the user to make the missing selection.
	 *
	 * @return {@code true} if both tables have at least one checked row; {@code false}
	 *         if validation fails
	 */
	private boolean validForm() {
		// Require at least one operations selection
		List<OperationsData> opsData = getSelectedOpsData();
		if (opsData.size() == 0) {
			JOptionPane.showMessageDialog(this, "Please Select one or more Operations.",
					"No Operations Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		// Require at least one meteorology selection
		List<MeteorlogicData> metData = getSelectedMetData();
		if (metData.size() == 0) {
			JOptionPane.showMessageDialog(this, "Please Select one or more Meteorology.",
					"No Meteorology Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		return true;
	}

	/**
	 * Persists form state after the user clicks OK.
	 *
	 * Currently a no-op; BC data is constructed on demand in {@link #getBcData()} rather
	 * than stored during form submission.
	 */
	private void saveForm() {
		// No additional form state to save; BC items are built lazily in getBcData()
	}

	/**
	 * Populates the operations and meteorology selection tables with data from the
	 * given {@link ForecastSimGroup}, resetting the cancelled state and clearing any
	 * previously displayed rows before loading the new data.
	 *
	 * Each table row contains a {@link Boolean#FALSE} checkbox value in column 0 and
	 * the corresponding data object in column 1.
	 *
	 * @param fsg the {@link ForecastSimGroup} whose operations and meteorology data
	 *            are loaded into the tables; must not be {@code null}
	 */
	public void fillForm(ForecastSimGroup fsg) {
		// Default to cancelled until the user explicitly clicks OK
		_canceled = true;

		// Clear any stale rows from a previous fillForm call
		_opsTable.deleteCells();
		_metTable.deleteCells();

		// Populate the operations table: column 0 = unchecked checkbox, column 1 = data object
		List<OperationsData> opsData = fsg.getOperationsData();
		Vector<Object> row = new Vector<>();
		for (int i = 0; i < opsData.size(); i++) {
			row = new Vector();
			row.add(Boolean.FALSE);
			row.add(opsData.get(i));
			_opsTable.appendRow(row);
		}

		// Populate the meteorology table: column 0 = unchecked checkbox, column 1 = data object
		List<MeteorlogicData> metData = fsg.getMeteorlogyData();
		for (int i = 0; i < metData.size(); i++) {
			row = new Vector();
			row.add(Boolean.FALSE);
			row.add(metData.get(i));
			_metTable.appendRow(row);
		}
	}

	/**
	 * Returns whether the dialog was closed via Cancel (or dismissed without clicking OK).
	 *
	 * @return {@code true} if the user cancelled or the dialog has not been submitted;
	 *         {@code false} if the user clicked OK and the form was valid
	 */
	@Override
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Builds and returns the list of {@link BcData} items corresponding to every
	 * checked operations–meteorology pair in the two selection tables.
	 *
	 * Each BC data item is named {@code "<ops-name>-<met-name>"} and has its selected
	 * operations and meteorology references set. The list is ordered by operations first,
	 * then meteorology (i.e., all meteorology variants for the first operation, then all
	 * for the second, and so on).
	 *
	 * @return a new {@link List} of {@link BcData} items; empty if no rows are checked
	 *         in either table
	 */
	public List<BcData> getBcData() {
		List<BcData> selectedBcs = new ArrayList<>();

		// Retrieve the currently checked selections from both tables
		List<OperationsData> opsData = getSelectedOpsData();
		List<MeteorlogicData> metData = getSelectedMetData();

		OperationsData ops;
		MeteorlogicData met;

		// Build one BcData item for every ops-met combination (Cartesian product)
		for (int o = 0; o < opsData.size(); o++) {
			ops = opsData.get(o);

			for (int m = 0; m < metData.size(); m++) {
				met = metData.get(m);

				// Construct a new BC data item named after the ops-met pair
				BcData bcData = new BcData();
				bcData.setName(ops.getName() + "-" + met.getName());
				bcData.setSelectedOps(ops);
				bcData.setSelectedMet(met);

				selectedBcs.add(bcData);
			}
		}

		return selectedBcs;
	}

	/**
	 * Retrieves all MeteorlogicData objects that are currently selected in the meteorology
	 * table. Selection state is determined by the value in column 0 of each row, which is
	 * expected to be a Boolean or its string representation. For each selected row, the
	 * MeteorlogicData object stored in column 1 is added to the result list.
	 *
	 * @return a list of MeteorlogicData objects corresponding to all selected rows in the
	 *         meteorology table; empty if no rows are selected
	 */
	private List<MeteorlogicData> getSelectedMetData() {
		// Get the total number of rows currently displayed in the meteorology table
		int rowCnt = _metTable.getNumRows();

		// Declare a variable to hold the cell value retrieved during each iteration
		Object obj;

		// Initialize the list that will accumulate all selected meteorology data objects
		List<MeteorlogicData> selectedMetData = new ArrayList<>();

		// Iterate over every row in the meteorology table to check its selection state
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the selection state value from the first column of the current row
			obj = _metTable.getValueAt(r, 0);

			// Accept both the Boolean object and its string representation for robustness
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Column 1 holds the MeteorlogicData object for this row
				selectedMetData.add((MeteorlogicData) _metTable.getValueAt(r, 1));
			}
		}

		// Return the list of all selected meteorology data objects
		return selectedMetData;
	}

	/**
	 * Retrieves all OperationsData objects that are currently selected in the operations
	 * table. Selection state is determined by the value in column 0 of each row, which is
	 * expected to be a Boolean or its string representation. For each selected row, the
	 * OperationsData object stored in column 1 is added to the result list.
	 *
	 * @return a list of OperationsData objects corresponding to all selected rows in the
	 *         operations table; empty if no rows are selected
	 */
	private List<OperationsData> getSelectedOpsData() {
		// Get the total number of rows currently displayed in the operations table
		int rowCnt = _opsTable.getNumRows();

		// Declare a variable to hold the cell value retrieved during each iteration
		Object obj;

		// Initialize the list that will accumulate all selected operations data objects
		List<OperationsData> selectedOpsData = new ArrayList<>();

		// Iterate over every row in the operations table to check its selection state
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the selection state value from the first column of the current row
			obj = _opsTable.getValueAt(r, 0);

			// Accept both the Boolean object and its string representation for robustness
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Column 1 holds the OperationsData object for this row
				selectedOpsData.add((OperationsData) _opsTable.getValueAt(r, 1));
			}
		}

		// Return the list of all selected operations data objects
		return selectedOpsData;
	}
}
