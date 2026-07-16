package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagConstraints;             // Layout constraints for positioning the list within this panel
import java.util.List;                          // Ordered collection interface for the paired Simulation Group's WatSimulations

import javax.swing.DefaultListModel;             // Backing model for the initial-conditions list, also reused by the summary strip box
import javax.swing.JList;                        // Displays the paired Simulation Group's WatSimulation entries
import javax.swing.JScrollPane;                  // Scroll container for the list
import javax.swing.ListModel;                    // Return type of getSummaryListModel()

import hec2.wat.model.WatSimulation;             // Represents each simulation entry shown in the list

import rma.swing.RmaInsets;                       // Standard GridBagConstraints insets constants

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;  // Supplies the WatSimulation list this panel displays
import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;     // The Set whose data this panel displays

/**
 * The "Initial Conditions" sub-tab of the Planning tab.
 *
 * Lists the {@code WatSimulation} entries belonging to the currently paired Simulation
 * Group (e.g. "Natoma (2003-09-17)", "Folsom (2024-09-17)" in the mockup), mirroring the
 * Forecast Conditions workflow's own Initial Conditions tab. The Set/Simulation Group
 * selectors themselves live one level up, in {@link PlanningPanel}, since — per the
 * intended design — they apply across all six sub-tabs, not just this one.
 */
@SuppressWarnings("serial")
public class InitialConditionsPanel extends AbstractPlanningPanel {

	// Backing model for the WatSimulation list, shared with the summary strip's mini box
	private final DefaultListModel<Object> _listModel = new DefaultListModel<>();

	// Displays the current Simulation Group's WatSimulation entries
	private JList<Object> _list;

	/**
	 * Constructs the panel and builds its list control.
	 *
	 * @param parent the owning PlanningPanel
	 */
	public InitialConditionsPanel(PlanningPanel parent) {
		super(parent); // Store the owning PlanningPanel and initialize the GridBagLayout
		buildControls(); // Lay out the list control
	}

	/**
	 * Builds and lays out the WatSimulation list.
	 */
	private void buildControls() {
		_list = new JList<>(_listModel); // Bind the visible list to the shared backing model

		GridBagConstraints gbc = new GridBagConstraints(); // Constraints for the single list control
		gbc.gridx = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the column
		gbc.gridy = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // The list fills the entire row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Allow vertical growth so the list fills the tab's available space
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the list
		add(new JScrollPane(_list), gbc); // Wrap in a scroll pane in case there are many simulations
	}

	/**
	 * Refreshes the list from the newly paired Simulation Group's WatSimulation entries.
	 *
	 * @param simulationGroup the newly paired Simulation Group, or null if none is selected
	 */
	@Override
	public void setSimulationGroup(AbstractSimulationGroup simulationGroup) {
		super.setSimulationGroup(simulationGroup); // Let the base class record the new reference

		_listModel.clear(); // Discard whatever was shown for the previous Simulation Group
		if (simulationGroup == null) {
			return; // Nothing more to do; leave the list empty
		}

		List<WatSimulation> sims = simulationGroup.getSimulations(); // Pull the group's simulation list
		if (sims != null) {
			for (WatSimulation sim : sims) { // Walk every simulation in the group
				_listModel.addElement(sim); // Add it to the visible list
			}
		}
	}

	@Override
	public void setPlanningSet(PlanningSet planningSet) {
		super.setPlanningSet(planningSet); // Update the shared _planningSet field and enabled state
		// Initial Conditions entries come from the paired Simulation Group, not the Set
		// itself, so no additional refresh is needed here beyond the enabled-state update
		// performed by the superclass.
	}

	@Override
	public String getTabName() {
		return "Initial Conditions"; // Fixed label used for both the tab and its summary-strip box
	}

	@Override
	public ListModel<Object> getSummaryListModel() {
		return _listModel; // Reuse the same model the list itself displays, so the strip stays live
	}
}
