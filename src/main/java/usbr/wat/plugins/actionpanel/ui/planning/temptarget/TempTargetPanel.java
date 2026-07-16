package usbr.wat.plugins.actionpanel.ui.planning.temptarget;

import java.awt.CardLayout;                     // Switches the detail area between the three input-mode controls
import java.awt.GridBagConstraints;              // Layout constraints for positioning controls within this panel
import java.io.File;                             // Represents the file selected for the timeseries/Jython-script modes

import javax.swing.ButtonGroup;                  // Groups the three mode radio buttons so exactly one is selectable
import javax.swing.DefaultListModel;              // Empty backing model returned by getSummaryListModel() (see note below)
import javax.swing.JButton;                       // Save button, and Browse buttons for the timeseries/Jython-script modes
import javax.swing.JFileChooser;                  // File chooser used by both the timeseries and Jython-script Browse buttons
import javax.swing.JFormattedTextField;            // Numeric entry field for the fixed-value mode
import javax.swing.JLabel;                        // Field labels within each mode's detail controls
import javax.swing.JOptionPane;                    // Used to report a validation error if Save is clicked with no Set selected
import javax.swing.JPanel;                        // Container for each mode's detail controls, switched via CardLayout
import javax.swing.JRadioButton;                  // The three mutually-exclusive mode selectors
import javax.swing.JTextField;                     // Read-only fields showing the currently selected timeseries/Jython-script paths
import javax.swing.ListModel;                      // Return type of getSummaryListModel()
import javax.swing.filechooser.FileFilter;          // Restricts the Jython-script chooser to .py files

import rma.swing.RmaInsets;                        // Standard GridBagConstraints insets constants
import rma.util.RMAFilenameFilter;                  // Existing RMA file filter implementation, reused to restrict the script chooser to .py files

import com.rma.model.Project;                       // Used to seed both file choosers in the current project's directory

import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;              // The Set whose temperature target this panel edits
import usbr.wat.plugins.actionpanel.model.planning.PlanningTempTarget;       // The model object this panel edits
import usbr.wat.plugins.actionpanel.model.planning.PlanningTempTargetMode;   // The three mutually-exclusive input modes
import usbr.wat.plugins.actionpanel.ui.planning.AbstractPlanningPanel;      // Base class shared by all six Planning sub-tabs
import usbr.wat.plugins.actionpanel.ui.planning.PlanningPanel;              // The owning top-level Planning panel

/**
 * The "Temperature Targets" sub-tab of the Planning tab.
 *
 * Because temperature targets are expected to vary significantly between Sets, this tab
 * lets the user pick exactly one of three mutually-exclusive input modes
 * (see {@link PlanningTempTargetMode}) rather than presenting the Forecast Conditions
 * workflow's list-of-named-target-sets structure:
 * <ul>
 *   <li><b>Fixed Value</b> — a single temperature value applied across all years of the
 *       simulation.</li>
 *   <li><b>Timeseries</b> — a single imported time series, expected to be the same length
 *       as the simulation period.</li>
 *   <li><b>Jython Script</b> — a {@code .py} file containing custom target logic, run via
 *       {@code usbr.wat.plugins.actionpanel.ui.forecast.PythonScriptUtil} during
 *       simulation.</li>
 * </ul>
 * Selecting a mode's radio button switches the detail area (via {@link CardLayout}) to
 * that mode's specific controls. Clicking Save commits the current mode and its value(s)
 * to the active Set's {@link PlanningTempTarget}.
 */
@SuppressWarnings("serial")
public class TempTargetPanel extends AbstractPlanningPanel {

	// CardLayout keys, matching PlanningTempTargetMode's enum constant names
	private static final String FIXED_VALUE_CARD = PlanningTempTargetMode.FIXED_VALUE.name();
	private static final String TIMESERIES_CARD = PlanningTempTargetMode.TIMESERIES.name();
	private static final String JYTHON_SCRIPT_CARD = PlanningTempTargetMode.JYTHON_SCRIPT.name();

	// The three mode selectors
	private JRadioButton _fixedValueRadio;
	private JRadioButton _timeseriesRadio;
	private JRadioButton _jythonScriptRadio;

	// Detail area switched between the three modes' controls
	private JPanel _detailPanel;
	private CardLayout _detailLayout;

	// Fixed Value mode controls
	private JFormattedTextField _fixedValueField;

	// Timeseries mode controls
	private JTextField _timeseriesField;
	private JButton _timeseriesBrowseButton;

	// Jython Script mode controls
	private JTextField _jythonScriptField;
	private JButton _jythonScriptBrowseButton;

	// Commits the current mode/value(s) to the active Set's PlanningTempTarget
	private JButton _saveButton;

	// Last directory browsed to, so repeated browses start from the same place
	private String _lastDir;

	/**
	 * Constructs the panel and builds its mode selector and detail controls.
	 *
	 * @param parent the owning PlanningPanel
	 */
	public TempTargetPanel(PlanningPanel parent) {
		super(parent); // Store the owning PlanningPanel and initialize the GridBagLayout
		buildControls(); // Lay out the radio buttons, card panel, and Save button
		addListeners(); // Wire up mode switching, browsing, and saving
	}

	/**
	 * Builds and lays out the three mode radio buttons, the CardLayout detail area, and
	 * the Save button.
	 */
	private void buildControls() {
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row

		_fixedValueRadio = new JRadioButton(PlanningTempTargetMode.FIXED_VALUE.getDisplayName()); // "Fixed Value" option
		_timeseriesRadio = new JRadioButton(PlanningTempTargetMode.TIMESERIES.getDisplayName()); // "Timeseries" option
		_jythonScriptRadio = new JRadioButton(PlanningTempTargetMode.JYTHON_SCRIPT.getDisplayName()); // "Jython Script" option

		ButtonGroup group = new ButtonGroup(); // Ensures only one of the three radios can be selected at a time
		group.add(_fixedValueRadio); // Register each radio with the shared exclusivity group
		group.add(_timeseriesRadio);
		group.add(_jythonScriptRadio);
		_fixedValueRadio.setSelected(true); // Default to Fixed Value mode when the panel first appears

		gbc.gridx = 0; // First column
		gbc.gridy = 0; // Top row
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the radio button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the radio button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the radio button
		add(_fixedValueRadio, gbc); // Place the Fixed Value radio

		gbc.gridx = 1; // Second column, same row
		gbc.gridy = 0; // Top row
		add(_timeseriesRadio, gbc); // Place the Timeseries radio, reusing the rest of gbc's settings

		gbc.gridx = 2; // Third column, same row
		gbc.gridy = 0; // Top row
		add(_jythonScriptRadio, gbc); // Place the Jython Script radio, reusing the rest of gbc's settings

		// --- Detail area, switched via CardLayout ---
		_detailLayout = new CardLayout(); // Only one mode's controls are visible at a time
		_detailPanel = new JPanel(_detailLayout); // Container that CardLayout manages

		_detailPanel.add(buildFixedValueCard(), FIXED_VALUE_CARD); // Register the Fixed Value controls under their key
		_detailPanel.add(buildTimeseriesCard(), TIMESERIES_CARD); // Register the Timeseries controls under their key
		_detailPanel.add(buildJythonScriptCard(), JYTHON_SCRIPT_CARD); // Register the Jython Script controls under their key

		gbc.gridx = 0; // First column
		gbc.gridy = 1; // Second row, below the radio buttons
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the entire row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Allow vertical growth so the detail area fills remaining space
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the detail area
		add(_detailPanel, gbc); // Place the CardLayout-managed detail area

		// --- Save button ---
		_saveButton = new JButton("Save"); // Commits the current mode/value(s) to the active Set
		gbc.gridx = 0; // First column
		gbc.gridy = 2; // Third row, below the detail area
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.SOUTHWEST; // Anchor to the bottom-left of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		add(_saveButton, gbc); // Place the Save button
	}

	/**
	 * Builds the Fixed Value mode's detail controls: a single numeric field applied across
	 * all years of the simulation.
	 */
	private JPanel buildFixedValueCard() {
		JPanel card = new JPanel(new java.awt.GridBagLayout()); // Independent layout for this card's controls
		GridBagConstraints gbc = new GridBagConstraints(); // Constraints local to this card

		gbc.gridx = 0; // First column
		gbc.gridy = 0; // First row
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		card.add(new JLabel("Value (applied across all years):"), gbc); // Describe what the field means

		_fixedValueField = new JFormattedTextField(java.text.NumberFormat.getNumberInstance()); // Numeric-only input
		_fixedValueField.setValue(0.0); // Default to zero until the user (or loaded data) sets a value
		_fixedValueField.setColumns(10); // Reasonable default width for a numeric field
		gbc.gridx = 1; // Second column, same row as the label
		gbc.gridy = 0; // First row
		gbc.fill = GridBagConstraints.NONE; // Fixed-size field; no stretching
		card.add(_fixedValueField, gbc); // Place the numeric input next to its label

		return card; // Hand the assembled card back to buildControls()
	}

	/**
	 * Builds the Timeseries mode's detail controls: a read-only path field plus Browse
	 * button for selecting a single time series expected to span the simulation period.
	 */
	private JPanel buildTimeseriesCard() {
		JPanel card = new JPanel(new java.awt.GridBagLayout()); // Independent layout for this card's controls
		GridBagConstraints gbc = new GridBagConstraints(); // Constraints local to this card

		gbc.gridx = 0; // First column
		gbc.gridy = 0; // First row
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		card.add(new JLabel("Timeseries (same length as the simulation period):"), gbc); // Explain the expected data shape

		_timeseriesField = new JTextField(30); // Shows the path chosen by the user
		_timeseriesField.setEditable(false); // Read-only; changed only via the Browse button
		gbc.gridx = 0; // First column, next row
		gbc.gridy = 1; // Second row, below the label
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.weightx = 1.0; // Allow horizontal growth
		card.add(_timeseriesField, gbc); // Place the path field

		_timeseriesBrowseButton = new JButton("Browse..."); // Opens a file chooser for the timeseries
		gbc.gridx = 1; // Second column, same row as the path field
		gbc.gridy = 1; // Second row
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.weightx = 0.0; // No horizontal growth for the button
		card.add(_timeseriesBrowseButton, gbc); // Place the Browse button

		return card; // Hand the assembled card back to buildControls()
	}

	/**
	 * Builds the Jython Script mode's detail controls: a read-only path field plus Browse
	 * button for selecting the {@code .py} file containing custom target logic.
	 */
	private JPanel buildJythonScriptCard() {
		JPanel card = new JPanel(new java.awt.GridBagLayout()); // Independent layout for this card's controls
		GridBagConstraints gbc = new GridBagConstraints(); // Constraints local to this card

		gbc.gridx = 0; // First column
		gbc.gridy = 0; // First row
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		card.add(new JLabel("Jython script containing custom target logic:"), gbc); // Explain what the file should contain

		_jythonScriptField = new JTextField(30); // Shows the path chosen by the user
		_jythonScriptField.setEditable(false); // Read-only; changed only via the Browse button
		gbc.gridx = 0; // First column, next row
		gbc.gridy = 1; // Second row, below the label
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.weightx = 1.0; // Allow horizontal growth
		card.add(_jythonScriptField, gbc); // Place the path field

		_jythonScriptBrowseButton = new JButton("Browse..."); // Opens a file chooser for the .py script
		gbc.gridx = 1; // Second column, same row as the path field
		gbc.gridy = 1; // Second row
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.weightx = 0.0; // No horizontal growth for the button
		card.add(_jythonScriptBrowseButton, gbc); // Place the Browse button

		return card; // Hand the assembled card back to buildControls()
	}

	/**
	 * Attaches listeners for the mode radio buttons, the two Browse buttons, and Save.
	 */
	private void addListeners() {
		// Selecting a radio button flips the CardLayout to that mode's detail controls
		_fixedValueRadio.addActionListener(e -> _detailLayout.show(_detailPanel, FIXED_VALUE_CARD));
		_timeseriesRadio.addActionListener(e -> _detailLayout.show(_detailPanel, TIMESERIES_CARD));
		_jythonScriptRadio.addActionListener(e -> _detailLayout.show(_detailPanel, JYTHON_SCRIPT_CARD));

		_timeseriesBrowseButton.addActionListener(e -> browseTimeseries()); // Opens the timeseries file chooser
		_jythonScriptBrowseButton.addActionListener(e -> browseJythonScript()); // Opens the Jython script file chooser

		_saveButton.addActionListener(e -> saveAction()); // Commits the current mode/value(s) to the active Set
	}

	/**
	 * Opens a file chooser for the timeseries file.
	 */
	private void browseTimeseries() {
		// Start browsing from the last-used directory, or the project directory on first use
		String dir = _lastDir != null ? _lastDir : Project.getCurrentProject().getProjectDirectory();
		JFileChooser chooser = new JFileChooser(dir); // Standard Swing file picker
		chooser.setDialogTitle("Select Temperature Target Timeseries"); // Clarify the chooser's purpose

		int opt = chooser.showOpenDialog(this); // Block until the user picks a file or cancels
		if (opt != JFileChooser.APPROVE_OPTION) {
			return; // User cancelled; leave the field unchanged
		}
		File file = chooser.getSelectedFile(); // The file the user chose
		if (file == null) {
			return; // Defensive guard; should not normally happen after APPROVE_OPTION
		}
		_lastDir = file.getParent(); // Remember the directory for the next browse
		_timeseriesField.setText(file.getAbsolutePath()); // Reflect the chosen file in the field
	}

	/**
	 * Opens a file chooser filtered to Python (.py) files for the Jython target-logic script.
	 */
	private void browseJythonScript() {
		// Start browsing from the last-used directory, or the project directory on first use
		String dir = _lastDir != null ? _lastDir : Project.getCurrentProject().getProjectDirectory();
		JFileChooser chooser = new JFileChooser(dir); // Standard Swing file picker
		FileFilter filter = new RMAFilenameFilter("py", "Script Files"); // Only show .py files
		chooser.addChoosableFileFilter(filter); // Register the filter as a selectable option
		chooser.setFileFilter(filter); // Apply the filter by default
		chooser.setDialogTitle("Select Temperature Target Jython Script"); // Clarify the chooser's purpose

		int opt = chooser.showOpenDialog(this); // Block until the user picks a file or cancels
		if (opt != JFileChooser.APPROVE_OPTION) {
			return; // User cancelled; leave the field unchanged
		}
		File file = chooser.getSelectedFile(); // The file the user chose
		if (file == null) {
			return; // Defensive guard; should not normally happen after APPROVE_OPTION
		}
		_lastDir = file.getParent(); // Remember the directory for the next browse
		_jythonScriptField.setText(file.getAbsolutePath()); // Reflect the chosen file in the field
	}

	/**
	 * Commits the currently selected mode and its value(s) to the active Set's
	 * {@link PlanningTempTarget}, creating one if the Set does not already have one.
	 */
	private void saveAction() {
		if (_planningSet == null) {
			// Nothing to save against without an active Set; warn the user instead of failing silently
			JOptionPane.showMessageDialog(this, "Select a Set before saving a temperature target.",
					"No Set Selected", JOptionPane.WARNING_MESSAGE);
			return;
		}

		PlanningTempTarget target = _planningSet.getTempTarget(); // Reuse the Set's existing target, if any
		if (target == null) {
			target = new PlanningTempTarget(); // First save for this Set; create a fresh target
			target.setName(_planningSet.getName() + "_TempTarget"); // Derive a default name from the Set
		}

		if (_fixedValueRadio.isSelected()) { // User chose the Fixed Value mode
			target.setMode(PlanningTempTargetMode.FIXED_VALUE);
			Object value = _fixedValueField.getValue(); // May be a Number or null depending on field state
			target.setFixedValue(value instanceof Number ? ((Number) value).doubleValue() : 0.0); // Guard against a non-numeric value

		} else if (_timeseriesRadio.isSelected()) { // User chose the Timeseries mode
			target.setMode(PlanningTempTargetMode.TIMESERIES);
			target.setTimeseriesFilePath(_timeseriesField.getText()); // Store the chosen file path

		} else { // Only the Jython Script radio remains, since the group is mutually exclusive
			target.setMode(PlanningTempTargetMode.JYTHON_SCRIPT);
			target.setJythonScriptPath(_jythonScriptField.getText()); // Store the chosen script path
		}

		_planningSet.setTempTarget(target); // Attach the (possibly new) target back onto the active Set
	}

	/**
	 * Refreshes the mode selector and detail controls from the newly active Set's
	 * temperature target, if any; otherwise resets to the Fixed Value default.
	 *
	 * @param planningSet the newly active Set, or null if none is selected
	 */
	@Override
	public void setPlanningSet(PlanningSet planningSet) {
		super.setPlanningSet(planningSet); // Update the shared _planningSet field and enabled state

		PlanningTempTarget target = planningSet != null ? planningSet.getTempTarget() : null; // May be null if unset
		if (target == null) {
			// No target defined yet for this Set (or no Set selected); reset the form to defaults
			_fixedValueRadio.setSelected(true);
			_detailLayout.show(_detailPanel, FIXED_VALUE_CARD);
			_fixedValueField.setValue(0.0);
			_timeseriesField.setText("");
			_jythonScriptField.setText("");
			return;
		}

		switch (target.getMode()) { // Select the radio button and card matching the stored mode
			case TIMESERIES:
				_timeseriesRadio.setSelected(true);
				_detailLayout.show(_detailPanel, TIMESERIES_CARD);
				break;
			case JYTHON_SCRIPT:
				_jythonScriptRadio.setSelected(true);
				_detailLayout.show(_detailPanel, JYTHON_SCRIPT_CARD);
				break;
			case FIXED_VALUE:
			default:
				_fixedValueRadio.setSelected(true);
				_detailLayout.show(_detailPanel, FIXED_VALUE_CARD);
				break;
		}

		// Populate every mode's fields regardless of which is active, so switching modes
		// mid-edit doesn't lose previously stored values for the other modes
		_fixedValueField.setValue(target.getFixedValue());
		_timeseriesField.setText(target.getTimeseriesFilePath());
		_jythonScriptField.setText(target.getJythonScriptPath());
	}

	@Override
	public String getTabName() {
		return "Temperature Targets"; // Fixed label used for both the tab and its summary-strip box
	}

	@Override
	public ListModel<Object> getSummaryListModel() {
		// The mockup's summary strip box for this tab is titled "Temperature Target Sets"
		// and, for the Forecast workflow, lists named sets; the Planning workflow instead
		// has exactly one mode-based target per Set, so an empty model is returned here.
		// PlanningPanel still gives this tab a summary box (per the mockup) for visual
		// consistency with the other four category boxes.
		return new DefaultListModel<>(); // Always empty; see the note above for why
	}
}
