package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagConstraints;             // Layout constraints for positioning the list within this panel

import javax.swing.DefaultListModel;             // Backing model for the boundary condition list, also reused by the summary strip box
import javax.swing.JList;                        // Displays this Set's boundary condition entries
import javax.swing.JScrollPane;                  // Scroll container for the list
import javax.swing.ListModel;                    // Return type of getSummaryListModel()

import rma.swing.RmaInsets;                       // Standard GridBagConstraints insets constants

import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;   // The Set whose data this panel displays

/**
 * The "Boundary Conditions" sub-tab of the Planning tab, mirroring the Forecast
 * Conditions workflow's own Boundary Condition Sets tab
 * (e.g. "SEP90_WY2024-HIST_2010-2010" in the mockup).
 *
 * <p><b>Extension point:</b> the source and shape of Planning-workflow boundary condition
 * data was not specified beyond mirroring the Forecast Conditions tab's structure —
 * plausibly derived from the Set's {@code CalSimData} and/or {@code HydrologyData}, but
 * that mapping was not confirmed. This panel currently renders an empty list with the
 * correct layout, tab wiring, and summary-strip integration already in place; wire
 * {@link #setPlanningSet(PlanningSet)} up to the real boundary condition data source once
 * it is defined.</p>
 */
@SuppressWarnings("serial")
public class BcPanel extends AbstractPlanningPanel {

	// Backing model for the boundary condition list, shared with the summary strip's mini box
	private final DefaultListModel<Object> _listModel = new DefaultListModel<>();

	// Displays the current Set's boundary condition entries
	private JList<Object> _list;

	/**
	 * Constructs the panel and builds its list control.
	 *
	 * @param parent the owning PlanningPanel
	 */
	public BcPanel(PlanningPanel parent) {
		super(parent); // Store the owning PlanningPanel and initialize the GridBagLayout
		buildControls(); // Lay out the list control
	}

	/**
	 * Builds and lays out the boundary condition list.
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
		add(new JScrollPane(_list), gbc); // Wrap in a scroll pane in case there are many entries
	}

	@Override
	public void setPlanningSet(PlanningSet planningSet) {
		super.setPlanningSet(planningSet); // Update the shared _planningSet field and enabled state
		_listModel.clear(); // Discard whatever was shown for the previous Set
		// TODO: populate from the real Planning-workflow boundary condition data source once defined.
	}

	@Override
	public String getTabName() {
		return "Boundary Conditions"; // Fixed label used for both the tab and its summary-strip box
	}

	@Override
	public ListModel<Object> getSummaryListModel() {
		return _listModel; // Reuse the same model the list itself displays, so the strip stays live
	}
}
