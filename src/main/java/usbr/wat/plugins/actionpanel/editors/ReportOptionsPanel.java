package usbr.wat.plugins.actionpanel.editors;

import java.awt.GridBagConstraints;         // Defines positioning and sizing constraints for components in a GridBagLayout
import java.awt.GridBagLayout;              // Flexible grid-based layout manager for arranging UI components
import java.util.logging.Logger;            // JDK logging utility for recording informational and error messages
import java.util.prefs.BackingStoreException; // Checked exception thrown when the preferences backing store cannot be accessed
import java.util.prefs.Preferences;         // Persistent, hierarchical key-value store for application and user preferences

import javax.swing.JLabel;                  // Non-interactive label component for displaying text in the UI
import javax.swing.JPanel;                  // Generic lightweight container used to group and lay out UI components

import hec2.wat.WAT;                        // HEC-WAT application entry point providing access to the browser frame and preferences

import rma.swing.RmaInsets;                 // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJCheckBox;              // RMA-extended checkbox component
import rma.swing.RmaJComboBox;              // RMA-extended combo box with generic type support

import usbr.wat.plugins.actionpanel.io.OutputType;     // Enum representing the supported report output file types (e.g., PDF, XLSX)
import usbr.wat.plugins.actionpanel.io.ReportOptions;  // Data object holding the user-selected report generation options

/**
 * Panel for configuring report output options within the WTMP Action Panel.
 *
 * Provides controls for selecting the report output file type (e.g., PDF, Excel)
 * and toggling whether headers and footers are included in the generated report.
 * Settings are persisted to and restored from the WAT project preferences store
 * under the node identified by PREF_NODE.
 *
 * This panel is intended to be embedded within a parent editor dialog and exposes
 * methods for reading the current selections as a ReportOptions object, saving
 * selections to preferences, and programmatically setting the output type.
 */
public class ReportOptionsPanel extends JPanel {

	// Preferences node name under which report options are stored for this project
	public static final String PREF_NODE = "usbrReports";

	// Combo box for selecting the output file type (PDF, Excel, etc.)
	private RmaJComboBox<OutputType> _outputTypeCombo;

	// Checkbox for toggling header and footer inclusion in generated reports
	private RmaJCheckBox _printHeaderFooterCheck;

	/**
	 * Constructs a ReportOptionsPanel, initializes the GridBagLayout, builds
	 * the UI controls, and populates them from saved project preferences.
	 */
	public ReportOptionsPanel() {
		// Initialize the JPanel superclass with a GridBagLayout for control placement
		super(new GridBagLayout());

		// Build and arrange all UI controls within the panel
		buildControls();

		// Restore previously saved settings from project preferences
		fillPanel();
	}

	/**
	 * Restores the panel's controls from the saved project preferences.
	 *
	 * Reads the previously stored output type and header/footer flag from the
	 * preferences node. Falls back to PDF output and headers-on if no values
	 * have been saved yet.
	 */
	private void fillPanel() {
		// Retrieve the preferences node scoped to this report panel
		Preferences node = getPreferencesNode();

		// Read the saved output type name, defaulting to PDF if absent
		String outputTypeName = node.get("ReportType", OutputType.PDF.name());

		// Convert the stored string back to an OutputType enum constant
		OutputType ot = OutputType.valueOf(outputTypeName);

		// Apply the restored output type to the combo box
		_outputTypeCombo.setSelectedItem(ot);

		// Read the saved header/footer toggle, defaulting to true if absent
		boolean printHeaderFooter = node.getBoolean("PrintHeaderFooter", true);

		// Apply the restored header/footer preference to the checkbox
		_printHeaderFooterCheck.setSelected(printHeaderFooter);
	}

	/**
	 * Builds and lays out all UI controls within the panel.
	 *
	 * Adds a nested type panel containing the "File Type:" label and output type
	 * combo box, followed by a header/footer checkbox placed beneath it.
	 */
	protected void buildControls() {
		// Create a nested sub-panel to group the file type label and combo box
		JPanel typePanel = new JPanel(new GridBagLayout());

		// Configure GridBagConstraints for placing the type panel across the full row
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(typePanel, gbc);

		// Create the "File Type:" label for the output type combo box
		JLabel label = new JLabel("File Type:");

		// Position the label to occupy a single cell without horizontal expansion
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		typePanel.add(label, gbc);

		// Create the output type combo box pre-populated with all OutputType enum values
		_outputTypeCombo = new RmaJComboBox<>(OutputType.values());

		// Position the combo box next to the label with minimal horizontal weight
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.001;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		typePanel.add(_outputTypeCombo, gbc);

		// Create the checkbox for toggling header and footer output
		_printHeaderFooterCheck = new RmaJCheckBox("Print Headers and Footers");

		// Position the checkbox below the type panel, spanning the full row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_printHeaderFooterCheck, gbc);
	}

	/**
	 * Programmatically sets the selected output type in the combo box.
	 *
	 * Has no effect if the provided OutputType is null.
	 *
	 * @param ot the OutputType to select; ignored if null
	 */
	public void setOutputType(OutputType ot) {
		// Only update the selection if a non-null output type was provided
		if (ot != null) {
			_outputTypeCombo.setSelectedItem(ot);
		}
	}

	/**
	 * Returns the currently selected output type from the combo box.
	 *
	 * @return the selected OutputType as an Object; cast to OutputType before use
	 */
	public Object getSelectedOutputType() {
		// Cast and return the combo box selection as an OutputType
		OutputType ot = (OutputType) _outputTypeCombo.getSelectedItem();
		return ot;
	}

	/**
	 * Returns the project-scoped preferences node used to persist report options.
	 *
	 * The node is located at: [projectPreferenceNode]/usbrReports
	 *
	 * @return the Preferences node for storing and retrieving report settings
	 */
	public Preferences getPreferencesNode() {
		return WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(PREF_NODE);
	}

	/**
	 * Saves the current panel selections to the project preferences store.
	 *
	 * Clears the existing preferences node before writing the current output type
	 * and header/footer flag. Logs a warning if the clear operation fails due to a
	 * backing store error.
	 */
	public void saveSettings() {
		// Retrieve the preferences node to write settings into
		Preferences node = getPreferencesNode();

		try {
			// Clear any previously saved values before writing fresh settings
			node.clear();
		} catch (BackingStoreException e) {
			// Log the failure to clear the node but continue saving what we can
			Logger.getLogger(ReportOptionsPanel.class.getName()).info("Failed to clear node " + node.absolutePath() + " Error:" + e);
		}

		// Read the currently selected output type and persist its name
		OutputType ot = (OutputType) _outputTypeCombo.getSelectedItem();
		node.put("ReportType", ot.name());

		// Read the current header/footer checkbox state and persist it
		boolean printHeaderFooter = _printHeaderFooterCheck.isSelected();
		node.putBoolean("PrintHeaderFooter", printHeaderFooter);
	}

	/**
	 * Constructs and returns a ReportOptions object reflecting the current panel state.
	 *
	 * Reads the selected output type and header/footer flag from the controls and
	 * packages them into a new ReportOptions instance for use by the report generator.
	 *
	 * @return a ReportOptions instance populated with the user's current selections
	 */
	public ReportOptions getReportOptions() {
		// Create a new options object to hold the current selections
		ReportOptions options = new ReportOptions();

		// Transfer the selected output type to the options object
		options.setOutputType((OutputType) _outputTypeCombo.getSelectedItem());

		// Transfer the header/footer checkbox state to the options object
		options.setPrintHeadersFooters(_printHeaderFooterCheck.isSelected());

		return options;
	}
}