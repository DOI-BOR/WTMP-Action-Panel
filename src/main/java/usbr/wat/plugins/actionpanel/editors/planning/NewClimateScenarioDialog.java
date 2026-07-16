package usbr.wat.plugins.actionpanel.editors.planning;

import java.awt.Cursor;                                    // Cursor types for showing a wait cursor while the perturbation script runs
import java.awt.GridBagConstraints;                         // Layout constraints for positioning components in a GridBagLayout
import java.awt.GridBagLayout;                               // Flexible grid-based layout manager
import java.awt.Window;                                      // Parent window type accepted by the RmaJDialog superclass constructor
import java.awt.event.ActionEvent;                           // Event type delivered to the OK/Cancel command panel listener
import java.io.File;                                          // Represents the selected historical-data folder and perturbation-script file
import java.nio.file.Paths;                                    // Used to build the Path argument PythonScriptUtil.runScript expects

import javax.swing.JButton;                                    // Historical Data / Climate Perturbation Script / Run Script buttons
import javax.swing.JFileChooser;                               // File/folder chooser used for both path fields
import javax.swing.JLabel;                                     // Status field label
import javax.swing.JOptionPane;                                 // Used to show a validation error if Name is left blank
import javax.swing.JTextField;                                  // Read-only fields showing the currently selected paths and status
import javax.swing.filechooser.FileFilter;                      // Restricts the script chooser to .py files

import hec.gui.NameDescriptionPanel;                           // Standard HEC Name/Description input pair, matching the mockup's Name/Description fields

import rma.swing.ButtonCmdPanel;                               // OK/Cancel button row
import rma.swing.ButtonCmdPanelListener;                       // Listener interface for the OK/Cancel button row
import rma.swing.RmaInsets;                                    // Standard GridBagConstraints insets constants
import rma.swing.RmaJDialog;                                   // Base dialog class used throughout this plugin
import rma.util.RMAFilenameFilter;                              // Existing RMA file filter implementation, reused to restrict the script chooser to .py files

import com.rma.model.Project;                                  // Used to seed both file choosers in the current project's directory

import usbr.wat.plugins.actionpanel.model.planning.ClimateScenario;          // The model object this dialog creates or edits
import usbr.wat.plugins.actionpanel.model.planning.PlanningSessionRegistry;  // Session-lifetime registry the new/edited scenario is registered into
import usbr.wat.plugins.actionpanel.ui.forecast.PythonScriptUtil;            // Existing Jython execution utility, used to run the climate perturbation script

/**
 * "New Climate Scenario" / "Edit Climate Scenario" dialog, opened from the Climate
 * Scenario row of {@link NewPlanningSetDialog}.
 *
 * Matches the mockup: Name, Description, a "Historical Data" folder picker, a "Climate
 * Perturbation Script" file picker, a Status field, and Run Script / OK / Cancel buttons.
 * Per the specified requirements, the Historical Data field always refers to a
 * <b>folder</b> (selected via {@link JFileChooser#DIRECTORIES_ONLY}) while the Climate
 * Perturbation Script field always refers directly to a <b>file</b>.
 *
 * Run Script invokes the selected script via {@link PythonScriptUtil}, passing the
 * historical data folder as an argument; the actual perturbation function's name and
 * contract are project-specific, so {@link #PERTURBATION_FUNCTION_NAME} is a clearly
 * marked extension point.
 *
 * On OK, the resulting {@link ClimateScenario} is also registered in the
 * {@link PlanningSessionRegistry} so it remains selectable for other Sets created later in
 * the same WAT session.
 */
@SuppressWarnings("serial")
public class NewClimateScenarioDialog extends RmaJDialog {

	// Name of the function the climate perturbation script is expected to expose.
	// TODO: confirm/replace with the real function name once the script contract is finalized.
	private static final String PERTURBATION_FUNCTION_NAME = "runClimatePerturbation";

	// Name/Description input pair
	private NameDescriptionPanel _nameDescPanel;

	// Opens a folder chooser for the historical data location
	private JButton _historicalDataButton;

	// Read-only display of the currently selected historical data folder
	private JTextField _historicalDataField;

	// Opens a file chooser for the climate perturbation script
	private JButton _scriptButton;

	// Read-only display of the currently selected script file
	private JTextField _scriptField;

	// Triggers execution of the selected perturbation script
	private JButton _runScriptButton;

	// Displays the most recent run status/result
	private JTextField _statusField;

	// Standard OK/Cancel button row
	private ButtonCmdPanel _cmdPanel;

	// True unless the user successfully completes the dialog via OK
	private boolean _canceled = true;

	// The scenario being created or edited; non-null only when editing an existing entry
	private ClimateScenario _climateScenario;

	// Last directory browsed to, so repeated browses start from the same place
	private String _lastDir;

	/**
	 * Constructs the dialog for creating a new climate scenario.
	 *
	 * @param parent the owning window
	 */
	public NewClimateScenarioDialog(Window parent) {
		this(parent, null); // Delegate to the edit-capable constructor with a null (new-entry) scenario
	}

	/**
	 * Constructs the dialog for creating or editing a climate scenario.
	 *
	 * @param parent          the owning window
	 * @param climateScenario the scenario to edit, or null to create a new one
	 */
	public NewClimateScenarioDialog(Window parent, ClimateScenario climateScenario) {
		super(parent, true); // Modal dialog, blocking the parent window while shown
		_climateScenario = climateScenario; // Remember which scenario (if any) we are editing

		setTitle(climateScenario == null ? "New Climate Scenario" : "Edit Climate Scenario"); // Title reflects create vs. edit mode

		buildControls(); // Lay out all Swing components
		addListeners(); // Wire up button behavior
		fillForm(); // Populate fields if editing an existing scenario

		pack(); // Size the dialog to fit its preferred layout
		setLocationRelativeTo(parent); // Center the dialog over its parent window
	}

	/**
	 * Builds and lays out all dialog controls using GridBagLayout, matching the mockup's
	 * Name/Description row, Historical Data / Climate Perturbation Script buttons, Status
	 * field, and Run Script / OK / Cancel row.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout()); // Use GridBagLayout for flexible row-based placement
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row

		_nameDescPanel = new NameDescriptionPanel(); // Combined Name + Description input widget
		gbc.gridx = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the column
		gbc.gridy = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // This component takes up the rest of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for this row
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5005; // Standard spacing around the name/description panel
		getContentPane().add(_nameDescPanel, gbc); // Place the name/description panel at the top

		// Historical Data (folder) row
		_historicalDataButton = new JButton("Historical Data..."); // Opens a folder-only chooser
		gbc.gridx = 0; // Start of a new row, first column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button itself
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		getContentPane().add(_historicalDataButton, gbc); // Place the Historical Data button

		_historicalDataField = new JTextField(); // Shows the folder path chosen by the user
		_historicalDataField.setEditable(false); // Read-only; changed only via the Browse button
		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the button
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the field
		getContentPane().add(_historicalDataField, gbc); // Place the historical data path field

		// Climate Perturbation Script (file) row
		_scriptButton = new JButton("Climate Perturbation Script..."); // Opens a .py file chooser
		gbc.gridx = 0; // Start of a new row, first column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button itself
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		getContentPane().add(_scriptButton, gbc); // Place the script chooser button

		_scriptField = new JTextField(); // Shows the script path chosen by the user
		_scriptField.setEditable(false); // Read-only; changed only via the Browse button
		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the button
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the field
		getContentPane().add(_scriptField, gbc); // Place the script path field

		// Spacer row that absorbs extra vertical space, matching the mockup's empty middle area
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Absorb all extra vertical space so later rows stay pinned near the bottom
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the spacer
		getContentPane().add(new JLabel(""), gbc); // Empty label used purely as a layout spacer

		// Status row
		JLabel statusLabel = new JLabel("Status:"); // Label preceding the status field
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the label
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the label
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		getContentPane().add(statusLabel, gbc); // Place the Status label

		_statusField = new JTextField(); // Shows the most recent run status/result text
		_statusField.setEditable(false); // Read-only; updated only by runScriptAction()
		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the label
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the field
		getContentPane().add(_statusField, gbc); // Place the status field

		// Run Script + OK/Cancel row
		_runScriptButton = new JButton("Run Script"); // Triggers execution of the selected perturbation script
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.SOUTHWEST; // Anchor to the bottom-left of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5555; // Standard spacing around the button
		getContentPane().add(_runScriptButton, gbc); // Place the Run Script button

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS); // Standard OK/Cancel button row
		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, remaining columns
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as Run Script
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for the button row
		gbc.anchor = GridBagConstraints.SOUTHEAST; // Anchor to the bottom-right, matching dialog conventions
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button row
		gbc.insets = RmaInsets.INSETS5555; // Standard spacing around the button row
		getContentPane().add(_cmdPanel, gbc); // Place the OK/Cancel row
	}

	/**
	 * Attaches the Historical Data / Script / Run Script and OK/Cancel listeners.
	 */
	private void addListeners() {
		_historicalDataButton.addActionListener(e -> chooseHistoricalDataFolder()); // Opens the folder chooser
		_scriptButton.addActionListener(e -> choosePerturbationScript()); // Opens the .py file chooser
		_runScriptButton.addActionListener(e -> runScriptAction()); // Runs the selected script

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
	 * Populates the form fields from the scenario being edited, if any.
	 */
	private void fillForm() {
		if (_climateScenario == null) {
			return; // Nothing to pre-populate when creating a brand-new scenario
		}

		_nameDescPanel.setName(_climateScenario.getName()); // Show the existing name
		_nameDescPanel.setDescription(_climateScenario.getDescription()); // Show the existing description
		_historicalDataField.setText(_climateScenario.getHistoricalDataPath()); // Show the existing folder path
		_scriptField.setText(_climateScenario.getClimatePerturbationScriptPath()); // Show the existing script path
		_statusField.setText(_climateScenario.getStatus()); // Show the last recorded status
	}

	/**
	 * Opens a folder-only chooser for the historical data location. Per the specified
	 * requirements this field always refers to a folder, not an individual file.
	 */
	private void chooseHistoricalDataFolder() {
		// Start browsing from the last-used directory, or the project directory on first use
		String dir = _lastDir != null ? _lastDir : Project.getCurrentProject().getProjectDirectory();

		JFileChooser chooser = new JFileChooser(dir); // Standard Swing file/folder picker
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Restrict selection to folders only
		chooser.setDialogTitle("Select Historical Data Folder"); // Clarify the chooser's purpose

		int opt = chooser.showOpenDialog(this); // Block until the user picks a folder or cancels
		if (opt != JFileChooser.APPROVE_OPTION) {
			return; // User cancelled; leave the field unchanged
		}

		File dirFile = chooser.getSelectedFile(); // The folder the user chose
		if (dirFile == null) {
			return; // Defensive guard; should not normally happen after APPROVE_OPTION
		}
		_lastDir = dirFile.getPath(); // Remember the directory for the next browse
		_historicalDataField.setText(dirFile.getAbsolutePath()); // Reflect the chosen folder in the field
	}

	/**
	 * Opens a file chooser filtered to Python (.py) files for the climate perturbation
	 * script. Per the specified requirements this field always points directly at the
	 * script file, not a containing folder.
	 */
	private void choosePerturbationScript() {
		// Start browsing from the last-used directory, or the project directory on first use
		String dir = _lastDir != null ? _lastDir : Project.getCurrentProject().getProjectDirectory();

		JFileChooser chooser = new JFileChooser(dir); // Standard Swing file picker
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // Restrict selection to individual files
		FileFilter filter = new RMAFilenameFilter("py", "Script Files"); // Only show .py files
		chooser.addChoosableFileFilter(filter); // Register the filter as a selectable option
		chooser.setFileFilter(filter); // Apply the filter by default
		chooser.setDialogTitle("Select Climate Perturbation Script"); // Clarify the chooser's purpose

		int opt = chooser.showOpenDialog(this); // Block until the user picks a file or cancels
		if (opt != JFileChooser.APPROVE_OPTION) {
			return; // User cancelled; leave the field unchanged
		}

		File file = chooser.getSelectedFile(); // The file the user chose
		if (file == null) {
			return; // Defensive guard; should not normally happen after APPROVE_OPTION
		}
		_lastDir = file.getParent(); // Remember the directory for the next browse
		_scriptField.setText(file.getAbsolutePath()); // Reflect the chosen script in the field
	}

	/**
	 * Runs the selected climate perturbation script against the selected historical data
	 * folder and reflects the outcome in the Status field.
	 *
	 * <p><b>Extension point:</b> {@link #PERTURBATION_FUNCTION_NAME} and the argument list
	 * passed to {@link PythonScriptUtil#runScript} should be confirmed against the real
	 * perturbation script's contract once it is finalized.</p>
	 */
	private void runScriptAction() {
		String scriptPath = _scriptField.getText(); // The script the user selected
		String historicalDataPath = _historicalDataField.getText(); // The folder the user selected

		if (scriptPath == null || scriptPath.trim().isEmpty()) {
			_statusField.setText("Select a Climate Perturbation Script before running."); // Guide the user to fix the missing input
			return;
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // Signal that work is in progress
		_statusField.setText("Running..."); // Immediate feedback before the (potentially slow) script runs
		try {
			// Invoke the perturbation script, passing the historical data folder as its argument
			String result = PythonScriptUtil.runScript(
					Paths.get(scriptPath), PERTURBATION_FUNCTION_NAME, String.class, historicalDataPath);
			_statusField.setText(result != null ? result : "Completed."); // Show the script's own message, or a generic success note
		} catch (RuntimeException ex) {
			_statusField.setText("Failed: " + ex.getMessage()); // Surface the failure reason to the user
		} finally {
			setCursor(Cursor.getDefaultCursor()); // Always restore the normal cursor, even on failure
		}
	}

	/**
	 * Validates and saves the form, creating a new {@link ClimateScenario} (or updating the
	 * one being edited), registering it in the session registry, and closing the dialog.
	 */
	private void saveForm() {
		String name = _nameDescPanel.getName(); // Read the entered name
		if (name == null || name.trim().isEmpty()) {
			// Block saving until a name is provided; a scenario without a name cannot be selected later
			JOptionPane.showMessageDialog(this, "Please enter a name.", "Name Required", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (_climateScenario == null) {
			_climateScenario = new ClimateScenario(); // First save of a brand-new scenario
		}
		_climateScenario.setName(name.trim()); // Store the trimmed name
		_climateScenario.setDescription(_nameDescPanel.getDescription()); // Store the description as entered
		_climateScenario.setHistoricalDataPath(_historicalDataField.getText()); // Store the selected folder path
		_climateScenario.setClimatePerturbationScriptPath(_scriptField.getText()); // Store the selected script path
		_climateScenario.setStatus(_statusField.getText()); // Store the most recent run status

		PlanningSessionRegistry.getInstance().addClimateScenario(_climateScenario); // Make this scenario reusable for other Sets this session

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
	 * Returns the climate scenario created or edited by this dialog.
	 *
	 * @return the resulting scenario, or null if the dialog was cancelled before ever saving
	 */
	public ClimateScenario getClimateScenario() {
		return _climateScenario; // Simple accessor
	}
}
