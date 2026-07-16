package usbr.wat.plugins.actionpanel.editors.planning;

import java.awt.GridBagConstraints;                         // Layout constraints for positioning components in a GridBagLayout
import java.awt.GridBagLayout;                               // Flexible grid-based layout manager
import java.awt.Window;                                      // Parent window type accepted by the RmaJDialog superclass constructor
import java.awt.event.ActionEvent;                           // Event type delivered to the OK/Cancel command panel listener

import javax.swing.DefaultComboBoxModel;                      // Backing model for the CalSim/Climate/Hydrology combo boxes
import javax.swing.JButton;                                    // Edit/New/Delete buttons for each of the three data rows
import javax.swing.JComboBox;                                  // CalSim Data / Climate Scenario / Hydrology / Schema selectors
import javax.swing.JLabel;                                     // Row labels
import javax.swing.JOptionPane;                                 // Used to show a validation error if Name or any required selection is missing

import hec.gui.NameDescriptionPanel;                           // Standard HEC Name/Description input pair, matching the mockup's Name/Description fields

import rma.swing.ButtonCmdPanel;                               // OK/Cancel button row
import rma.swing.ButtonCmdPanelListener;                       // Listener interface for the OK/Cancel button row
import rma.swing.RmaInsets;                                    // Standard GridBagConstraints insets constants
import rma.swing.RmaJDialog;                                   // Base dialog class used throughout this plugin

import usbr.wat.plugins.actionpanel.model.planning.CalSimData;              // Model type listed in the CalSim Data row
import usbr.wat.plugins.actionpanel.model.planning.ClimateScenario;         // Model type listed in the Climate Scenario row
import usbr.wat.plugins.actionpanel.model.planning.HydrologyData;           // Model type listed in the Hydrology row
import usbr.wat.plugins.actionpanel.model.planning.PlanningSessionRegistry; // Session-lifetime source of "already in the system" entries for all three rows
import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;             // The Set this dialog creates or edits

/**
 * "New Planning Set" / "Edit Planning Set" dialog, opened from the Set row's New/Edit
 * buttons at the top of the Planning tab ({@code usbr.wat.plugins.actionpanel.ui.planning.PlanningPanel}).
 *
 * Matches the mockup: Name, Description, a WAT Schema selector, and three rows — CalSim
 * Data, Climate Scenario, and Hydrology — each with a drop-down of existing entries plus
 * Edit/New/Delete buttons. Selecting an existing entry from a row's drop-down reuses data
 * already entered this WAT session (see {@link PlanningSessionRegistry}); clicking New
 * opens that row's dedicated creation dialog ({@link NewCalSimDataDialog},
 * {@link NewClimateScenarioDialog}, or {@link NewHydrologyDataDialog}) and, on success,
 * both selects the new entry here and registers it in the session registry for reuse by
 * later Sets.
 *
 * The mockup's on-screen dialog title reads "New Forecast Set" (an apparent artifact from
 * the Forecast workflow it was adapted from); this implementation titles the dialog
 * "New Planning Set" / "Edit Planning Set" instead.
 */
@SuppressWarnings("serial")
public class NewPlanningSetDialog extends RmaJDialog {

	// Name/Description input pair
	private NameDescriptionPanel _nameDescPanel;

	// WAT Schema selector
	private JComboBox<String> _schemaCombo;

	// CalSim Data row controls
	private JComboBox<CalSimData> _calSimCombo;
	private JButton _calSimEditButton;
	private JButton _calSimNewButton;
	private JButton _calSimDeleteButton;

	// Climate Scenario row controls
	private JComboBox<ClimateScenario> _climateCombo;
	private JButton _climateEditButton;
	private JButton _climateNewButton;
	private JButton _climateDeleteButton;

	// Hydrology row controls
	private JComboBox<HydrologyData> _hydrologyCombo;
	private JButton _hydrologyEditButton;
	private JButton _hydrologyNewButton;
	private JButton _hydrologyDeleteButton;

	// Standard OK/Cancel button row
	private ButtonCmdPanel _cmdPanel;

	// True unless the user successfully completes the dialog via OK
	private boolean _canceled = true;

	// The Set being created or edited; non-null only when editing an existing Set
	private PlanningSet _planningSet;

	/**
	 * Constructs the dialog for creating a new Planning Set.
	 *
	 * @param parent the owning window
	 */
	public NewPlanningSetDialog(Window parent) {
		this(parent, null); // Delegate to the edit-capable constructor with a null (new-entry) Set
	}

	/**
	 * Constructs the dialog for creating or editing a Planning Set.
	 *
	 * @param parent      the owning window
	 * @param planningSet the Set to edit, or null to create a new one
	 */
	public NewPlanningSetDialog(Window parent, PlanningSet planningSet) {
		super(parent, true); // Modal dialog, blocking the parent window while shown
		_planningSet = planningSet; // Remember which Set (if any) we are editing

		setTitle(planningSet == null ? "New Planning Set" : "Edit Planning Set"); // Title reflects create vs. edit mode

		buildControls(); // Lay out all Swing components
		addListeners(); // Wire up button/combo behavior
		reloadCombos(); // Populate the three data-row combos from the session registry
		fillForm(); // Populate fields if editing an existing Set

		pack(); // Size the dialog to fit its preferred layout
		setLocationRelativeTo(parent); // Center the dialog over its parent window
	}

	/**
	 * Builds and lays out all dialog controls using GridBagLayout, matching the mockup's
	 * Name/Description row, Schema row, and CalSim Data / Climate Scenario / Hydrology rows.
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

		// Schema row
		JLabel schemaLabel = new JLabel("Schema:"); // Label preceding the schema combo
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the label
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the label
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		getContentPane().add(schemaLabel, gbc); // Place the Schema label

		// TODO: populate from the real list of available WAT schemas once that source is defined.
		_schemaCombo = new JComboBox<>(new String[]{}); // Empty for now; editable so a value can still be typed
		_schemaCombo.setEditable(true); // Allow free-text entry until a real schema source exists
		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the label
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the combo
		getContentPane().add(_schemaCombo, gbc); // Place the Schema combo

		// CalSim Data row
		_calSimCombo = new JComboBox<>(); // Lists CalSim datasets available this session
		_calSimEditButton = new JButton("Edit..."); // Edits the currently selected dataset
		_calSimNewButton = new JButton("New..."); // Opens the New CalSim Data dialog
		_calSimDeleteButton = new JButton("Delete..."); // Removes the selected dataset from the session registry
		addDataRow("CalSim Data:", _calSimCombo, _calSimEditButton, _calSimNewButton, _calSimDeleteButton, gbc);

		// Climate Scenario row
		_climateCombo = new JComboBox<>(); // Lists climate scenarios available this session
		_climateEditButton = new JButton("Edit..."); // Edits the currently selected scenario
		_climateNewButton = new JButton("New..."); // Opens the New Climate Scenario dialog
		_climateDeleteButton = new JButton("Delete..."); // Removes the selected scenario from the session registry
		addDataRow("Climate Scenario:", _climateCombo, _climateEditButton, _climateNewButton, _climateDeleteButton, gbc);

		// Hydrology row
		_hydrologyCombo = new JComboBox<>(); // Lists hydrology datasets available this session
		_hydrologyEditButton = new JButton("Edit..."); // Edits the currently selected dataset
		_hydrologyNewButton = new JButton("New..."); // Opens the New Hydrology Data dialog
		_hydrologyDeleteButton = new JButton("Delete..."); // Removes the selected dataset from the session registry
		addDataRow("Hydrology:", _hydrologyCombo, _hydrologyEditButton, _hydrologyNewButton, _hydrologyDeleteButton, gbc);

		// Spacer row that absorbs extra vertical space, matching the mockup's empty lower area
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Absorb all extra vertical space so the OK/Cancel row stays pinned near the bottom
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the spacer
		getContentPane().add(new JLabel(""), gbc); // Empty label used purely as a layout spacer

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS); // Standard OK/Cancel button row
		gbc.gridx = GridBagConstraints.RELATIVE; // Next row
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for the button row
		gbc.anchor = GridBagConstraints.SOUTHEAST; // Anchor to the bottom-right, matching dialog conventions
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button row
		gbc.insets = RmaInsets.INSETS5555; // Standard spacing around the button row
		getContentPane().add(_cmdPanel, gbc); // Place the OK/Cancel row at the bottom
	}

	/**
	 * Adds one labeled combo-box-plus-three-buttons row (used identically for the CalSim
	 * Data, Climate Scenario, and Hydrology rows) to the content pane using the given
	 * shared GridBagConstraints instance.
	 *
	 * @param labelText    the row's label text (e.g. "CalSim Data:")
	 * @param combo        the row's combo box
	 * @param editButton   the row's Edit button
	 * @param newButton    the row's New button
	 * @param deleteButton the row's Delete button
	 * @param gbc          the shared constraints instance to reuse and mutate for this row
	 */
	private void addDataRow(String labelText, JComboBox<?> combo, JButton editButton,
	                         JButton newButton, JButton deleteButton, GridBagConstraints gbc) {
		JLabel label = new JLabel(labelText); // The row's leading label (e.g. "CalSim Data:")
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the label
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the label
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		getContentPane().add(label, gbc); // Place the row label

		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the label
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 1.0; // Allow the combo to absorb extra horizontal space
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the combo
		getContentPane().add(combo, gbc); // Place the row's combo box

		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as the combo
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5500; // Tighter spacing so the three buttons sit close together
		getContentPane().add(editButton, gbc); // Place the row's Edit button

		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as Edit
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5500; // Tighter spacing so the three buttons sit close together
		getContentPane().add(newButton, gbc); // Place the row's New button

		gbc.gridx = GridBagConstraints.RELATIVE; // Same row, final column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as Edit/New
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5500; // Tighter spacing so the three buttons sit close together
		getContentPane().add(deleteButton, gbc); // Place the row's Delete button
	}

	/**
	 * Attaches listeners for the three New/Edit/Delete button triples and the OK/Cancel row.
	 */
	private void addListeners() {
		_calSimNewButton.addActionListener(e -> newCalSimData()); // Opens the New CalSim Data dialog
		_calSimEditButton.addActionListener(e -> editCalSimData()); // Opens the Edit CalSim Data dialog
		_calSimDeleteButton.addActionListener(e -> deleteCalSimData()); // Removes the selected CalSim dataset

		_climateNewButton.addActionListener(e -> newClimateScenario()); // Opens the New Climate Scenario dialog
		_climateEditButton.addActionListener(e -> editClimateScenario()); // Opens the Edit Climate Scenario dialog
		_climateDeleteButton.addActionListener(e -> deleteClimateScenario()); // Removes the selected climate scenario

		_hydrologyNewButton.addActionListener(e -> newHydrologyData()); // Opens the New Hydrology Data dialog
		_hydrologyEditButton.addActionListener(e -> editHydrologyData()); // Opens the Edit Hydrology Data dialog
		_hydrologyDeleteButton.addActionListener(e -> deleteHydrologyData()); // Removes the selected hydrology dataset

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
	 * Reloads all three combo boxes from the current contents of the
	 * {@link PlanningSessionRegistry}, i.e. every CalSim/Climate/Hydrology entry created so
	 * far this WAT session ("If there is information already in the system, the user may
	 * select these from the drop down rather than setting new path data").
	 */
	private void reloadCombos() {
		PlanningSessionRegistry registry = PlanningSessionRegistry.getInstance(); // Shared session-lifetime store

		// Rebuild each combo's model from the registry's current contents, converted to arrays
		_calSimCombo.setModel(new DefaultComboBoxModel<>(registry.getCalSimData().toArray(new CalSimData[0])));
		_climateCombo.setModel(new DefaultComboBoxModel<>(registry.getClimateScenarios().toArray(new ClimateScenario[0])));
		_hydrologyCombo.setModel(new DefaultComboBoxModel<>(registry.getHydrologyData().toArray(new HydrologyData[0])));
	}

	/**
	 * Populates the form fields from the Set being edited, if any.
	 */
	private void fillForm() {
		if (_planningSet == null) {
			return; // Nothing to pre-populate when creating a brand-new Set
		}

		_nameDescPanel.setName(_planningSet.getName()); // Show the existing name
		_nameDescPanel.setDescription(_planningSet.getDescription()); // Show the existing description
		_schemaCombo.setSelectedItem(_planningSet.getWatSchema()); // Show the existing schema selection
		_calSimCombo.setSelectedItem(_planningSet.getCalSimData()); // Show the existing CalSim selection
		_climateCombo.setSelectedItem(_planningSet.getClimateScenario()); // Show the existing climate scenario selection
		_hydrologyCombo.setSelectedItem(_planningSet.getHydrologyData()); // Show the existing hydrology selection
	}

	// --- CalSim Data row actions ---

	// Opens the New CalSim Data dialog and, on success, selects the new entry here
	private void newCalSimData() {
		NewCalSimDataDialog dlg = new NewCalSimDataDialog(this); // Child dialog, modal over this one
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up the newly registered dataset
			_calSimCombo.setSelectedItem(dlg.getCalSimData()); // Select it immediately for convenience
		}
	}

	// Opens the Edit CalSim Data dialog for the currently selected entry
	private void editCalSimData() {
		CalSimData selected = (CalSimData) _calSimCombo.getSelectedItem(); // Nothing to edit if none selected
		if (selected == null) {
			return;
		}
		NewCalSimDataDialog dlg = new NewCalSimDataDialog(this, selected); // Pre-populated with the selected entry
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up any edits made to the dataset
			_calSimCombo.setSelectedItem(dlg.getCalSimData()); // Re-select the (possibly renamed) entry
		}
	}

	// Removes the currently selected CalSim dataset from the session registry, after confirmation
	private void deleteCalSimData() {
		CalSimData selected = (CalSimData) _calSimCombo.getSelectedItem(); // Nothing to delete if none selected
		if (selected == null) {
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete CalSim dataset \"" + selected.getName() + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			PlanningSessionRegistry.getInstance().removeCalSimDataByName(selected.getName()); // Drop it from the registry
			reloadCombos(); // Refresh the combo so the deleted entry disappears
		}
	}

	// --- Climate Scenario row actions ---

	// Opens the New Climate Scenario dialog and, on success, selects the new entry here
	private void newClimateScenario() {
		NewClimateScenarioDialog dlg = new NewClimateScenarioDialog(this); // Child dialog, modal over this one
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up the newly registered scenario
			_climateCombo.setSelectedItem(dlg.getClimateScenario()); // Select it immediately for convenience
		}
	}

	// Opens the Edit Climate Scenario dialog for the currently selected entry
	private void editClimateScenario() {
		ClimateScenario selected = (ClimateScenario) _climateCombo.getSelectedItem(); // Nothing to edit if none selected
		if (selected == null) {
			return;
		}
		NewClimateScenarioDialog dlg = new NewClimateScenarioDialog(this, selected); // Pre-populated with the selected entry
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up any edits made to the scenario
			_climateCombo.setSelectedItem(dlg.getClimateScenario()); // Re-select the (possibly renamed) entry
		}
	}

	// Removes the currently selected climate scenario from the session registry, after confirmation
	private void deleteClimateScenario() {
		ClimateScenario selected = (ClimateScenario) _climateCombo.getSelectedItem(); // Nothing to delete if none selected
		if (selected == null) {
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete climate scenario \"" + selected.getName() + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			PlanningSessionRegistry.getInstance().removeClimateScenarioByName(selected.getName()); // Drop it from the registry
			reloadCombos(); // Refresh the combo so the deleted entry disappears
		}
	}

	// --- Hydrology row actions ---

	// Opens the New Hydrology Data dialog and, on success, selects the new entry here
	private void newHydrologyData() {
		NewHydrologyDataDialog dlg = new NewHydrologyDataDialog(this); // Child dialog, modal over this one
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up the newly registered dataset
			_hydrologyCombo.setSelectedItem(dlg.getHydrologyData()); // Select it immediately for convenience
		}
	}

	// Opens the Edit Hydrology Data dialog for the currently selected entry
	private void editHydrologyData() {
		HydrologyData selected = (HydrologyData) _hydrologyCombo.getSelectedItem(); // Nothing to edit if none selected
		if (selected == null) {
			return;
		}
		NewHydrologyDataDialog dlg = new NewHydrologyDataDialog(this, selected); // Pre-populated with the selected entry
		dlg.setVisible(true); // Blocks until the child dialog is closed
		if (!dlg.isCanceled()) {
			reloadCombos(); // Pick up any edits made to the dataset
			_hydrologyCombo.setSelectedItem(dlg.getHydrologyData()); // Re-select the (possibly renamed) entry
		}
	}

	// Removes the currently selected hydrology dataset from the session registry, after confirmation
	private void deleteHydrologyData() {
		HydrologyData selected = (HydrologyData) _hydrologyCombo.getSelectedItem(); // Nothing to delete if none selected
		if (selected == null) {
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete hydrology dataset \"" + selected.getName() + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			PlanningSessionRegistry.getInstance().removeHydrologyDataByName(selected.getName()); // Drop it from the registry
			reloadCombos(); // Refresh the combo so the deleted entry disappears
		}
	}

	/**
	 * Validates that a name and all three of CalSim Data, Climate Scenario, and Hydrology
	 * have been selected, then saves the form, creating a new {@link PlanningSet} (or
	 * updating the one being edited) and closing the dialog.
	 */
	private void saveForm() {
		String name = _nameDescPanel.getName(); // Read the entered name
		if (name == null || name.trim().isEmpty()) {
			// Block saving until a name is provided; a Set without a name cannot be selected later
			JOptionPane.showMessageDialog(this, "Please enter a name.", "Name Required", JOptionPane.WARNING_MESSAGE);
			return;
		}

		CalSimData calSimData = (CalSimData) _calSimCombo.getSelectedItem(); // Currently selected CalSim entry
		ClimateScenario climateScenario = (ClimateScenario) _climateCombo.getSelectedItem(); // Currently selected scenario
		HydrologyData hydrologyData = (HydrologyData) _hydrologyCombo.getSelectedItem(); // Currently selected hydrology entry

		if (calSimData == null || climateScenario == null || hydrologyData == null) {
			// A Set is only meaningful once all three forcing-data pieces are present
			JOptionPane.showMessageDialog(this,
					"Please select or create CalSim Data, a Climate Scenario, and Hydrology data.",
					"Missing Data", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (_planningSet == null) {
			_planningSet = new PlanningSet(); // First save of a brand-new Set
		}
		_planningSet.setName(name.trim()); // Store the trimmed name
		_planningSet.setDescription(_nameDescPanel.getDescription()); // Store the description as entered

		Object schema = _schemaCombo.getSelectedItem(); // May be null if nothing was typed/selected
		_planningSet.setWatSchema(schema == null ? "" : schema.toString()); // Normalize null to empty string

		_planningSet.setCalSimData(calSimData); // Embed the selected CalSim dataset
		_planningSet.setClimateScenario(climateScenario); // Embed the selected climate scenario
		_planningSet.setHydrologyData(hydrologyData); // Embed the selected hydrology dataset

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
	 * Returns the Planning Set created or edited by this dialog.
	 *
	 * @return the resulting Set, or null if the dialog was cancelled before ever saving
	 */
	public PlanningSet getPlanningSet() {
		return _planningSet; // Simple accessor
	}
}
