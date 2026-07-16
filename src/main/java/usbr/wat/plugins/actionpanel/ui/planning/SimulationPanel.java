package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagConstraints;             // Layout constraints for positioning controls within this panel

import javax.swing.DefaultListModel;             // Empty backing model returned by getSummaryListModel() (this tab has no summary-strip box)
import javax.swing.JButton;                      // Placeholder "Run" trigger
import javax.swing.JLabel;                       // Read-only summary of the active Set/Simulation Group pairing
import javax.swing.ListModel;                    // Return type of getSummaryListModel()

import rma.swing.RmaInsets;                       // Standard GridBagConstraints insets constants

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;   // The paired Simulation Group summarized here
import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;      // The Set summarized here

/**
 * The "Simulation" sub-tab of the Planning tab, mirroring the Forecast Conditions
 * workflow's own Simulation tab. Per the mockup, this is the one left-hand tab that does
 * <i>not</i> get a corresponding box in the upper category summary strip.
 *
 * Shows a read-only summary of the currently active (Simulation Group, Set) pairing and a
 * Run trigger.
 *
 * <p><b>Extension point:</b> actually launching a Planning simulation run was not
 * specified in detail. {@link #runAction()} is a clearly marked integration point —
 * wire it to the equivalent of
 * {@code usbr.wat.plugins.actionpanel.actions.RunSimulationAction} once the Planning
 * workflow's run behavior (which model schema to invoke, how the Set's derived
 * meteorologic data is fed in, etc.) is defined.</p>
 */
@SuppressWarnings("serial")
public class SimulationPanel extends AbstractPlanningPanel {

	// Read-only summary of the active Set
	private JLabel _setSummaryLabel;

	// Read-only summary of the paired Simulation Group
	private JLabel _simGroupSummaryLabel;

	// Placeholder trigger for running the simulation
	private JButton _runButton;

	/**
	 * Constructs the panel and builds its summary/run controls.
	 *
	 * @param parent the owning PlanningPanel
	 */
	public SimulationPanel(PlanningPanel parent) {
		super(parent); // Store the owning PlanningPanel and initialize the GridBagLayout
		buildControls(); // Lay out the summary labels and Run button
	}

	/**
	 * Builds and lays out the summary labels and Run button.
	 */
	private void buildControls() {
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row

		_setSummaryLabel = new JLabel("Set: (none selected)"); // Placeholder text until a Set is chosen
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the entire row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for this row
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		add(_setSummaryLabel, gbc); // Place the Set summary label

		_simGroupSummaryLabel = new JLabel("Simulation Group: (none selected)"); // Placeholder text until a group is chosen
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the entire row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for this row
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		add(_simGroupSummaryLabel, gbc); // Place the Simulation Group summary label

		_runButton = new JButton("Run"); // Placeholder trigger; see class javadoc extension-point note
		_runButton.addActionListener(e -> runAction()); // Delegate to the (currently placeholder) run handler
		gbc.gridx = 0; // First column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.weighty = 1.0; // Absorb remaining vertical space so the button doesn't stretch tall
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		add(_runButton, gbc); // Place the Run button
	}

	@Override
	public void setPlanningSet(PlanningSet planningSet) {
		super.setPlanningSet(planningSet); // Update the shared _planningSet field and enabled state
		// Reflect the newly selected Set's name, or the placeholder text if none is selected
		_setSummaryLabel.setText("Set: " + (planningSet != null ? planningSet.getName() : "(none selected)"));
	}

	@Override
	public void setSimulationGroup(AbstractSimulationGroup simulationGroup) {
		super.setSimulationGroup(simulationGroup); // Update the shared _simulationGroup field
		// Reflect the newly paired group's name, or the placeholder text if none is selected
		_simGroupSummaryLabel.setText("Simulation Group: "
				+ (simulationGroup != null ? simulationGroup.getName() : "(none selected)"));
	}

	/**
	 * Placeholder Run handler. See the class-level javadoc extension-point note for what
	 * needs to be wired up once the Planning workflow's run behavior is defined.
	 */
	private void runAction() {
		// TODO: wire up the real Planning simulation run behavior once defined.
	}

	@Override
	public String getTabName() {
		return "Simulation"; // Fixed label used for the tab itself (no corresponding summary-strip box)
	}

	@Override
	public ListModel<Object> getSummaryListModel() {
		// The Simulation tab has no corresponding box in the category summary strip.
		return new DefaultListModel<>(); // Return an always-empty model since this tab is never shown in the strip
	}
}
