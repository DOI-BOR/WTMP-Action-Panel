package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagConstraints;             // Layout constraints for positioning the list within this panel

import javax.swing.DefaultListModel;             // Backing model for the meteorology list, also reused by the summary strip box
import javax.swing.JList;                        // Displays the Set's derived meteorologic entry (its Climate Scenario)
import javax.swing.JScrollPane;                  // Scroll container for the list
import javax.swing.ListModel;                    // Return type of getSummaryListModel()

import rma.swing.RmaInsets;                       // Standard GridBagConstraints insets constants

import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;   // The Set whose Climate Scenario this panel displays

/**
 * The "Meteorology" sub-tab of the Planning tab, mirroring the Forecast Conditions
 * workflow's own Meteorology tab (e.g. "SSP4.5_1935-2020" in the mockup).
 *
 * Per the clarified data model, a Set's meteorologic data is derived from its
 * {@link usbr.wat.plugins.actionpanel.model.planning.ClimateScenario} — the Simulation
 * Group is the model that data is ultimately applied to. This panel therefore lists the
 * active Set's Climate Scenario as its single meteorology entry.
 *
 * <p><b>Extension point:</b> the Forecast Conditions Meteorology tab additionally plots
 * the selected forecast's time-series data (Location/Record navigation plus a chart, per
 * the baseline mockup slide). That level of detail was not specified for the Planning
 * workflow; this panel currently exposes only the list-of-entries structure with the
 * correct tab wiring and summary-strip integration in place. Add the chart/detail area
 * here once that requirement is confirmed.</p>
 */
@SuppressWarnings("serial")
public class MeteorologyPanel extends AbstractPlanningPanel {

	// Backing model for the meteorology list, shared with the summary strip's mini box
	private final DefaultListModel<Object> _listModel = new DefaultListModel<>();

	// Displays the current Set's derived meteorology entry
	private JList<Object> _list;

	/**
	 * Constructs the panel and builds its list control.
	 *
	 * @param parent the owning PlanningPanel
	 */
	public MeteorologyPanel(PlanningPanel parent) {
		super(parent); // Store the owning PlanningPanel and initialize the GridBagLayout
		buildControls(); // Lay out the list control
	}

	/**
	 * Builds and lays out the meteorology list.
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
		add(new JScrollPane(_list), gbc); // Wrap in a scroll pane for consistency with the other tabs
	}

	/**
	 * Refreshes the list to show the newly active Set's Climate Scenario, since the Set's
	 * meteorologic data is derived from it.
	 *
	 * @param planningSet the newly active Set, or null if none is selected
	 */
	@Override
	public void setPlanningSet(PlanningSet planningSet) {
		super.setPlanningSet(planningSet); // Update the shared _planningSet field and enabled state

		_listModel.clear(); // Discard whatever was shown for the previous Set
		if (planningSet != null && planningSet.getClimateScenario() != null) {
			_listModel.addElement(planningSet.getClimateScenario()); // Show the Set's single derived meteorology entry
		}
	}

	@Override
	public String getTabName() {
		return "Meteorology"; // Fixed label used for both the tab and its summary-strip box
	}

	@Override
	public ListModel<Object> getSummaryListModel() {
		return _listModel; // Reuse the same model the list itself displays, so the strip stays live
	}
}
