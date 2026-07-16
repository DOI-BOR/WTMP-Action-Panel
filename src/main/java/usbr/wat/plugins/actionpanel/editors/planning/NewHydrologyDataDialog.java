package usbr.wat.plugins.actionpanel.editors.planning;

import java.awt.Cursor;                                    // Cursor types for showing a wait cursor while the import script runs
import java.awt.GridBagConstraints;                         // Layout constraints for positioning components in a GridBagLayout
import java.awt.GridBagLayout;                               // Flexible grid-based layout manager
import java.awt.Window;                                      // Parent window type accepted by the RmaJDialog superclass constructor
import java.awt.event.ActionEvent;                           // Event type delivered to the OK/Cancel command panel listener
import java.nio.file.Path;                                    // Path type for the placeholder import script location
import java.nio.file.Paths;                                   // Factory for constructing the placeholder script Path
import java.util.ArrayList;                                   // Backing list used to build rows returned by the import script
import java.util.List;                                        // Ordered collection interface for the imported rows

import javax.swing.JButton;                                   // Import button
import javax.swing.JOptionPane;                                // Used to show a validation error if Name is left blank, and import failure messages
import javax.swing.JScrollPane;                                // Scroll container for the data grid
import javax.swing.JTable;                                    // Grid displaying imported Date/Flow1/Flow2/Flow3 rows
import javax.swing.table.DefaultTableModel;                   // Editable table model backing the data grid

import hec.gui.NameDescriptionPanel;                          // Standard HEC Name/Description input pair, matching the mockup's Name/Description fields

import rma.swing.ButtonCmdPanel;                              // OK/Cancel button row
import rma.swing.ButtonCmdPanelListener;                      // Listener interface for the OK/Cancel button row
import rma.swing.RmaInsets;                                   // Standard GridBagConstraints insets constants
import rma.swing.RmaJDialog;                                  // Base dialog class used throughout this plugin

import usbr.wat.plugins.actionpanel.model.planning.HydrologyData;             // The model object this dialog creates or edits
import usbr.wat.plugins.actionpanel.model.planning.PlanningSessionRegistry;   // Session-lifetime registry the new/edited dataset is registered into
import usbr.wat.plugins.actionpanel.ui.forecast.PythonScriptUtil;             // Existing Jython execution utility, reused to invoke the placeholder import script

/**
 * "New Hydrology Data" / "Edit Hydrology Data" dialog, opened from the Hydrology row of
 * {@link NewPlanningSetDialog}.
 *
 * Matches the mockup: Name, Description, an Import button, and a Date/Flow 1/Flow 2/Flow 3
 * grid. Unlike {@link NewCalSimDataDialog}, clicking Import does not prompt for a file —
 * the details of the hydrology modeling workflow are not yet defined, so Import instead
 * invokes a placeholder script via {@link PythonScriptUtil}, matching the direction to
 * "assume this is called via a placeholder script for the moment." Replace
 * {@link #PLACEHOLDER_SCRIPT_PATH} and {@link #PLACEHOLDER_FUNCTION_NAME} (or the whole
 * body of {@link #importAction()}) with the real integration once the hydrology modeling
 * details are known.
 *
 * On OK, the resulting {@link HydrologyData} is also registered in the
 * {@link PlanningSessionRegistry} so it remains selectable for other Sets created later in
 * the same WAT session.
 */
@SuppressWarnings("serial")
public class NewHydrologyDataDialog extends RmaJDialog {

	// Column headers for the data grid, matching the mockup exactly
	private static final String[] COLUMN_NAMES = {"Date", "Flow 1", "Flow 2", "Flow 3"};

	// Project-relative path to the placeholder hydrology import script.
	// TODO: replace with the real hydrology import script location once defined.
	private static final Path PLACEHOLDER_SCRIPT_PATH = Paths.get("planning/scripts/hydrology_import_placeholder.py");

	// Name of the function within the placeholder script that performs the import.
	// TODO: replace with the real function name/signature once defined.
	private static final String PLACEHOLDER_FUNCTION_NAME = "importHydrologyData";

	// Name/Description input pair
	private NameDescriptionPanel _nameDescPanel;

	// Triggers the placeholder import script
	private JButton _importButton;

	// Backing model for the data grid
	private DefaultTableModel _tableModel;

	// Displays the imported rows
	private JTable _table;

	// Standard OK/Cancel button row
	private ButtonCmdPanel _cmdPanel;

	// True unless the user successfully completes the dialog via OK
	private boolean _canceled = true;

	// The dataset being created or edited; non-null only when editing an existing entry
	private HydrologyData _hydrologyData;

	/**
	 * Constructs the dialog for creating a new hydrology dataset.
	 *
	 * @param parent the owning window
	 */
	public NewHydrologyDataDialog(Window parent) {
		this(parent, null); // Delegate to the edit-capable constructor with a null (new-entry) dataset
	}

	/**
	 * Constructs the dialog for creating or editing a hydrology dataset.
	 *
	 * @param parent        the owning window
	 * @param hydrologyData the dataset to edit, or null to create a new one
	 */
	public NewHydrologyDataDialog(Window parent, HydrologyData hydrologyData) {
		super(parent, true); // Modal dialog, blocking the parent window while shown
		_hydrologyData = hydrologyData; // Remember which dataset (if any) we are editing

		setTitle(hydrologyData == null ? "New Hydrology Data" : "Edit Hydrology Data"); // Title reflects create vs. edit mode

		buildControls(); // Lay out all Swing components
		addListeners(); // Wire up button behavior
		fillForm(); // Populate fields if editing an existing dataset

		pack(); // Size the dialog to fit its preferred layout
		setLocationRelativeTo(parent); // Center the dialog over its parent window
	}

	/**
	 * Builds and lays out all dialog controls using GridBagLayout, matching the mockup's
	 * Name/Description row, Import button + grid row, and OK/Cancel row.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout()); // Use GridBagLayout for flexible row-based placement

		_nameDescPanel = new NameDescriptionPanel(); // Combined Name + Description input widget
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row
		gbc.gridx = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the column
		gbc.gridy = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // This component takes up the rest of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for this row
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5005; // Standard spacing around the name/description panel
		getContentPane().add(_nameDescPanel, gbc); // Place the name/description panel at the top

		_importButton = new JButton("Import..."); // Triggers the placeholder import script
		gbc.gridx = 0; // Start of a new row, first column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button itself
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		getContentPane().add(_importButton, gbc); // Place the Import button

		_tableModel = new DefaultTableModel(COLUMN_NAMES, 0); // Start with zero rows and the four named columns
		_table = new JTable(_tableModel); // Swing table bound to the model above
		JScrollPane scrollPane = new JScrollPane(_table); // Make the grid scrollable once it has many rows
		scrollPane.setPreferredSize(new java.awt.Dimension(600, 300)); // Give the grid a sensible default size
		gbc.gridx = GridBagConstraints.RELATIVE; // Continue on the same row as Import, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as Import
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Allow vertical growth so the grid absorbs extra dialog height
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the grid
		getContentPane().add(scrollPane, gbc); // Place the scrollable grid

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS); // Standard OK/Cancel button row
		gbc.gridx = GridBagConstraints.RELATIVE; // Next row
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for the button row
		gbc.anchor = GridBagConstraints.SOUTHWEST; // Anchor to the bottom-left, matching dialog conventions
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch horizontally
		gbc.insets = RmaInsets.INSETS5555; // Standard spacing around the button row
		getContentPane().add(_cmdPanel, gbc); // Place the OK/Cancel row at the bottom
	}

	/**
	 * Attaches the Import button and OK/Cancel listeners.
	 */
	private void addListeners() {
		_importButton.addActionListener(e -> importAction()); // Clicking Import triggers the placeholder script

		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) { // Dispatch based on which button in the command row was clicked
					case ButtonCmdPanel.OK_BUTTON:
						saveForm(); // Validate and commit the form's current state
						break;
					case ButtonCmdPanel.CANCEL_BUTTON:
						_canceled = true; // Record that the user backed out without saving
						setVisible(false); // Hide the dialog, returning control to the caller
						break;
				}
			}
		});
	}

	/**
	 * Populates the form fields from the dataset being edited, if any.
	 */
	private void fillForm() {
		if (_hydrologyData == null) {
			return; // Nothing to pre-populate when creating a brand-new dataset
		}

		_nameDescPanel.setName(_hydrologyData.getName()); // Show the existing name
		_nameDescPanel.setDescription(_hydrologyData.getDescription()); // Show the existing description

		_tableModel.setRowCount(0); // Clear any placeholder rows before repopulating
		for (Object[] row : _hydrologyData.getRows()) { // Walk every row already stored on the dataset
			_tableModel.addRow(row); // Add it to the visible grid
		}
	}

	/**
	 * Invokes the placeholder hydrology import script and populates the grid with its
	 * result. The details of the hydrology modeling workflow are not yet defined; this
	 * method is the integration point to replace once they are.
	 *
	 * <p><b>Extension point:</b> {@link #PLACEHOLDER_SCRIPT_PATH} and
	 * {@link #PLACEHOLDER_FUNCTION_NAME} should be updated to point at the real hydrology
	 * import script and function once the modeling workflow is finalized. The script is
	 * expected to return a {@code List<Object[]>} of {@code [date, flow1, flow2, flow3]}
	 * rows; adjust the return handling below if the real script's contract differs.</p>
	 */
	@SuppressWarnings("unchecked")
	private void importAction() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // Signal that work is in progress
		try {
			// Invoke the placeholder script; expected to return a List<Object[]> of rows
			List<Object[]> rows = PythonScriptUtil.runScript(
					PLACEHOLDER_SCRIPT_PATH, PLACEHOLDER_FUNCTION_NAME, List.class);

			if (rows == null) {
				rows = new ArrayList<>(); // Guard against a script that returns nothing
			}

			_tableModel.setRowCount(0); // Clear the grid before loading the freshly imported rows
			for (Object[] row : rows) { // Walk every row returned by the script
				_tableModel.addRow(row); // Add it to the visible grid
			}

			if (_hydrologyData != null) {
				_hydrologyData.setImportedFilePath(PLACEHOLDER_SCRIPT_PATH.toString()); // Record the script used, for traceability
			}
		} catch (RuntimeException ex) {
			// Surface any script failure to the user rather than failing silently
			JOptionPane.showMessageDialog(this,
					"Hydrology import failed: " + ex.getMessage()
							+ "\n(The hydrology import script is a placeholder pending the finalized modeling workflow.)",
					"Import Failed", JOptionPane.ERROR_MESSAGE);
		} finally {
			setCursor(Cursor.getDefaultCursor()); // Always restore the normal cursor, even on failure
		}
	}

	/**
	 * Validates and saves the form, creating a new {@link HydrologyData} (or updating the
	 * one being edited), registering it in the session registry, and closing the dialog.
	 */
	private void saveForm() {
		String name = _nameDescPanel.getName(); // Read the entered name
		if (name == null || name.trim().isEmpty()) {
			// Block saving until a name is provided; a dataset without a name cannot be selected later
			JOptionPane.showMessageDialog(this, "Please enter a name.", "Name Required", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (_hydrologyData == null) {
			_hydrologyData = new HydrologyData(); // First save of a brand-new dataset
		}
		_hydrologyData.setName(name.trim()); // Store the trimmed name
		_hydrologyData.setDescription(_nameDescPanel.getDescription()); // Store the description as entered

		List<Object[]> rows = new ArrayList<>(); // Collect the grid's current contents
		for (int i = 0; i < _tableModel.getRowCount(); i++) { // Walk every row currently in the grid
			Object[] row = new Object[COLUMN_NAMES.length]; // One array per row, sized to the column count
			for (int c = 0; c < COLUMN_NAMES.length; c++) { // Walk every column within the row
				row[c] = _tableModel.getValueAt(i, c); // Copy the cell's current value
			}
			rows.add(row); // Add the reconstructed row to the list
		}
		_hydrologyData.setRows(rows); // Replace the dataset's rows with the grid's current contents

		PlanningSessionRegistry.getInstance().addHydrologyData(_hydrologyData); // Make this dataset reusable for other Sets this session

		_canceled = false; // Record that the user completed the dialog successfully
		setVisible(false); // Hide the dialog, returning control to the caller
	}

	/**
	 * Returns whether the dialog was closed without successfully saving.
	 *
	 * @return true if the user cancelled or closed the dialog without clicking OK
	 */
	public boolean isCanceled() {
		return _canceled; // Simple accessor
	}

	/**
	 * Returns the hydrology dataset created or edited by this dialog.
	 *
	 * @return the resulting dataset, or null if the dialog was cancelled before ever saving
	 */
	public HydrologyData getHydrologyData() {
		return _hydrologyData; // Simple accessor
	}
}
