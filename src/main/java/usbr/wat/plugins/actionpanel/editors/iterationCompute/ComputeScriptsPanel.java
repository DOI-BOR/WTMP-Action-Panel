package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Dimension;          // Used to override the table's preferred scroll viewport height
import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;      // Flexible grid-based layout manager for arranging UI components
import java.io.File;                // Represents a file system path returned by the JFileChooser

import java.util.List;             // Ordered collection interface for model alternative lists
import java.util.Vector;           // Synchronized growable array for building table rows

import javax.swing.BorderFactory;              // Factory for creating titled and other border styles
import javax.swing.JButton;                    // Standard Swing push-button component
import javax.swing.JFileChooser;               // Dialog for browsing and selecting a script file
import javax.swing.JPanel;                     // Generic lightweight container for the Browse button row
import javax.swing.border.TitledBorder;        // Titled border used to label the pre/post compute panel
import javax.swing.filechooser.FileFilter;     // Abstract file filter for restricting JFileChooser to .py files

import com.rma.model.Project;       // Represents the currently loaded RMA project; used for path resolution

import hec2.plugin.model.ModelAlternative; // Represents a model alternative configuration within a WAT simulation
import hec2.wat.model.WatSimulation;       // Represents a WAT simulation whose model alternatives populate the table

import rma.swing.RmaInsets;         // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJTable;         // RMA-extended table component with utility row management methods
import rma.util.RMAFilenameFilter;   // RMA file filter implementation for filtering by extension in JFileChooser

import usbr.wat.plugins.actionpanel.model.ComputeSettings; // Data model holding script paths and run-script flags per model alternative

/**
 * Panel for associating pre- or post-compute Python scripts with each model
 * alternative in a WAT simulation.
 *
 * Displays a table with columns for the model program name, model alternative,
 * script file path, and a "Run Script" checkbox. A Browse button opens a file
 * chooser to select a Python (.py) script file for the currently selected row.
 *
 * Instances are created by SensitivityPanel with a title of either
 * "Pre-Compute Scripts" or "Post-Compute Scripts".
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ComputeScriptsPanel extends JPanel {
	// Column index for the ModelAlternative object (used for script lookup in fill/save)
	private static final int MODEL_ALT_COL = 1;

	// Column index for the script file path string
	private static final int SCRIPT_COL = 2;

	// Column index for the "Run Script" boolean checkbox
	private static final int RUN_SCRIPT_COL = 3;

	// Number of table rows to show in the viewport before scrolling is required
	protected static final int NUM_ROWS_VISIBLE = 6;

	// Table showing the model alternatives and their associated script entries
	private RmaJTable _modelTable;

	// Button that opens a file chooser to select a Python script for the selected row
	private JButton _browseButton;

	// The directory last used in the file chooser, for re-opening at the same location
	private String _lastDir;

	// The WAT simulation whose model alternatives populate the table
	private WatSimulation _simulation;

	// The ComputeSettings currently loaded in this panel, used as a default save target
	private ComputeSettings _computeSettings;

	/**
	 * Constructs a ComputeScriptsPanel with the given titled border label.
	 *
	 * @param title the label text for the panel's titled border (e.g., "Pre-Compute Scripts")
	 */
	public ComputeScriptsPanel(String title) {
		// Initialize the JPanel with a GridBagLayout for component placement
		super(new GridBagLayout());

		// Build the table, border, and Browse button
		buildControls(title);

		// Attach selection and click listeners
		addListeners();
	}

	/**
	 * Builds and lays out the table, titled border, and Browse button.
	 *
	 * The table is configured with four columns (Model, Model Alternative, Script,
	 * Run Script). The first two columns are read-only; Script and Run Script are
	 * editable. The table's preferred scroll viewport height is fixed to NUM_ROWS_VISIBLE
	 * rows. A Browse button is placed below the table.
	 *
	 * @param title the text to display in the panel's titled border
	 */
	protected void buildControls(String title) {
		// Apply a titled border to identify this as a pre- or post-compute scripts panel
		TitledBorder border = BorderFactory.createTitledBorder(title);
		setBorder(border);

		// Define the column headers for the model scripts table
		String[] headers = {"Model", "Model Alternative", "Script", "Run Script"};

		// Create the model scripts table; overrides preferred viewport height and per-cell editability
		_modelTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Fix the viewport height to exactly NUM_ROWS_VISIBLE row heights
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * NUM_ROWS_VISIBLE;
				return d;
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				// Only the Script (col 2) and Run Script (col 3) columns are editable
				return col > 1;
			}
		};

		// Add extra row height for readability
		_modelTable.setRowHeight(_modelTable.getRowHeight() + 5);

		// Remove the built-in sum/statistics popup menu options
		_modelTable.removePopupMenuSumOptions();

		// Make the Model (col 0) and Model Alternative (col 1) columns read-only
		_modelTable.setColumnEnabled(false, 0);
		_modelTable.setColumnEnabled(false, 1);

		// Enable a checkbox cell editor in the Run Script column
		_modelTable.setCheckBoxCellEditor(3);

		// Configure constraints for the table to fill all available space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_modelTable.getScrollPane(), gbc);

		// Create a sub-panel to center the Browse button below the table
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(panel, gbc);

		// Create the Browse button and place it centered within the sub-panel
		_browseButton = new JButton("Browse");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_browseButton, gbc);

		// Set initial button enabled state based on empty table (no selection)
		tableRowSelected();
	}

	/**
	 * Attaches event listeners to the table selection model and the Browse button.
	 */
	protected void addListeners() {
		// Enable/disable the Browse button whenever the table selection changes
		_modelTable.getSelectionModel().addListSelectionListener(e -> tableRowSelected());

		// Open a file chooser to select a script when Browse is clicked
		_browseButton.addActionListener(e -> browseFilesAction());
	}

	/**
	 * Placeholder for an edit-script action (currently no-op).
	 * <p>
	 * Intended to open a script editor for the currently selected row.
	 */
	private void editScriptAction() {
		int row = _modelTable.getSelectedRow();
		if (row == -1) {
			return;
		}
		// Script editing is not yet implemented
	}

	/**
	 * Updates the Browse button's enabled state based on whether a row is selected.
	 */
	private void tableRowSelected() {
		// Enable the Browse button only when a table row is selected
		_browseButton.setEnabled(_modelTable.getSelectedRow() > -1);
	}

	/**
	 * Opens a JFileChooser filtered to Python (.py) files and writes the selected
	 * file's absolute path into the Script column of the currently selected row.
	 *
	 * Starts the chooser in the last used directory or the project directory if no
	 * prior directory is known. Has no effect if no row is selected or the user
	 * cancels the dialog.
	 */
	private void browseFilesAction() {
		int row = _modelTable.getSelectedRow();

		// Do nothing if no row is selected
		if (row < 0) {
			return;
		}

		// Use the last browsed directory or fall back to the project directory
		String dir = Project.getCurrentProject().getProjectDirectory();
		if (_lastDir != null) {
			dir = _lastDir;
		}

		// Open a file chooser filtered to Python script files
		JFileChooser chooser = new JFileChooser(dir);
		FileFilter filter = new RMAFilenameFilter("py", "Script Files");
		chooser.addChoosableFileFilter(filter);
		chooser.setFileFilter(filter);

		int opt = chooser.showOpenDialog(this);

		// Return immediately if the user did not confirm a selection
		if (opt != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = chooser.getSelectedFile();
		if (file == null) {
			return;
		}

		// Remember the directory for the next browse session
		_lastDir = file.getParent();

		// Write the absolute path of the chosen script into the Script column
		_modelTable.setValueAt(file.getAbsolutePath(), row, 2);

		// Mark the table as modified since the user changed a cell value
		_modelTable.setModified(true);
	}

	/**
	 * Populates the table with the model alternatives from the given simulation.
	 *
	 * Clears the existing rows, then adds one row per model alternative with
	 * the program name and alternative object. Script and Run Script columns are
	 * left empty until fillPanel() is called. Re-applies read-only constraints
	 * on the first two columns after populating.
	 *
	 * @param sim the WatSimulation whose model alternatives should be shown
	 */
	public void setSimulation(WatSimulation sim) {
		_simulation = sim;

		// Clear all existing rows before repopulating
		_modelTable.deleteCells();

		if (_simulation != null) {
			// Retrieve all model alternatives for this simulation
			List<ModelAlternative> modelAlts = _simulation.getAllModelAlternativeList();

			Vector row;
			ModelAlternative modelAlt;
			for (int i = 0; i < modelAlts.size(); i++) {
				// Build a four-element row: program name, model alternative object, (empty script), (empty run flag)
				row = new Vector(4);
				modelAlt = modelAlts.get(i);
				row.add(modelAlt.getProgram());
				row.add(modelAlt);
				_modelTable.appendRow(row);
			}
		}

		// Ensure Model and Model Alternative columns remain read-only after repopulation
		_modelTable.setColumnEnabled(false, 0);
		_modelTable.setColumnEnabled(false, 1);
	}

	/**
	 * Populates the Script and Run Script columns from a ComputeSettings object.
	 *
	 * For each table row, looks up the script path and run-script flag stored
	 * in the settings for the row's model alternative. Script paths are resolved
	 * to absolute paths for display. Refreshes button state afterward.
	 *
	 * @param computeSettings the ComputeSettings to populate from
	 */
	public void fillPanel(ComputeSettings computeSettings) {
		// Remember this settings object as the default save target
		_computeSettings = computeSettings;

		int rowCnt = _modelTable.getRowCount();
		ModelAlternative modelAlt;
		Project proj = Project.getCurrentProject();
		String script, absScript;
		boolean runScript;

		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the model alternative for this row
			modelAlt = (ModelAlternative) _modelTable.getValueAt(r, MODEL_ALT_COL);

			// Look up the stored script path and run-script flag for this alternative
			script = computeSettings.getScriptFor(modelAlt);

			// Resolve the relative script path to an absolute path for display
			absScript = proj.getAbsolutePath(script);
			runScript = computeSettings.shouldRunScriptFor(modelAlt);

			// Write the resolved path and run-flag into the table
			_modelTable.setValueAt(absScript, r, SCRIPT_COL);
			_modelTable.setValueAt(runScript, r, RUN_SCRIPT_COL);
		}

		// Refresh browse-button enabled state
		tableRowSelected();
	}

	/**
	 * Saves the current table contents back to a ComputeSettings object.
	 *
	 * Commits any pending cell edits, then iterates all rows. For each row with
	 * a non-empty script path, converts the absolute path to a relative project
	 * path and stores it along with the run-script flag. Clears both values for
	 * rows whose script field is empty or blank. The provided computeSettings
	 * parameter overrides the internally cached settings object if non-null.
	 *
	 * @param computeSettings the ComputeSettings to save into; uses the cached settings if null
	 * @return true always (no save-failure path in this implementation)
	 */
	public boolean savePanel(ComputeSettings computeSettings) {
		// Commit any in-progress cell edits before reading values
		_modelTable.commitEdit(true);

		// Prefer the provided settings object; fall back to the cached one
		ComputeSettings settings = _computeSettings;
		if (computeSettings != null) {
			settings = computeSettings;
		}

		int rowCnt = _modelTable.getRowCount();
		ModelAlternative modelAlt;
		Project proj = Project.getCurrentProject();
		String relScript;

		for (int r = 0; r < rowCnt; r++) {
			modelAlt = (ModelAlternative) _modelTable.getValueAt(r, MODEL_ALT_COL);
			String script = (String) _modelTable.getValueAt(r, SCRIPT_COL);

			if (script != null) {
				script = script.trim();

				if (!script.isEmpty()) {
					// Convert the absolute script path to a project-relative path for storage
					relScript = proj.getRelativePath(script);
					computeSettings.setScriptFor(modelAlt, relScript);

					// Also save the run-script flag for this alternative
					Object runScriptObj = _modelTable.getValueAt(r, RUN_SCRIPT_COL);
					if (runScriptObj != null) {
						computeSettings.setRunScriptFor(modelAlt, Boolean.parseBoolean(runScriptObj.toString()));
					}
				} else {
					// Script field is blank; clear both the script and the run-script flag
					computeSettings.setScriptFor(modelAlt, null);
					computeSettings.setRunScriptFor(modelAlt, false);
				}
			}
		}

		return true;
	}
}
