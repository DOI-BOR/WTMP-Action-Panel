package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for the tabbed pane added below base controls
import java.awt.event.ItemEvent;     // Carries combo-box selection change data; used to filter DESELECTED events

import javax.swing.Action;           // Swing Action interface returned by the three abstract factory methods
import javax.swing.JTabbedPane;      // Tabbed pane added below the inherited toolbar controls for forecast-specific tabs

import com.rma.model.ManagerProxy;   // Lightweight proxy wrapping a managed model object; selected item in the combo box

import rma.swing.RmaInsets;          // Constants for common GridBagLayout inset configurations

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                              // Singleton plugin entry point providing access to the actions window
import usbr.wat.plugins.actionpanel.actions.forecast.DeleteForecastSimGroupAction;  // Action that deletes the selected forecast simulation group
import usbr.wat.plugins.actionpanel.actions.forecast.EditForecastSimGroupAction;    // Action that opens the editor for the selected forecast simulation group
import usbr.wat.plugins.actionpanel.actions.forecast.NewForecastSimGroupAction;     // Action that opens the creation dialog for a new forecast simulation group
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                // Forecast-specific simulation group model managed by this panel


/**
 * Concrete simulation group toolbar panel for the forecast workflow.
 *
 * This class extends BaseSimulationGroupPanel to provide forecast-specific
 * implementations of the three action factory methods and the combo-box selection
 * handler. It manages ForecastSimGroup objects rather than the standard SimulationGroup
 * type used by CalibrationSimulationGroupPanel.
 *
 * In addition to the controls inherited from BaseSimulationGroupPanel, this panel
 * appends a JTabbedPane below the toolbar row. The tabbed pane is available for
 * forecast-specific tab content to be added by parent or sibling components.
 *
 * When a forecast simulation group is selected from the combo box, the group's manager
 * is loaded from the proxy and passed to the owning AbstractSimulationPanel via
 * fillForm, which also enables or disables the Edit button accordingly. A DESELECTED
 * event with a non-null proxy explicitly clears the parent panel by calling fillForm
 * with null before the new selection is processed.
 *
 */
public class SimulationGroupPanel extends BaseSimulationGroupPanel {
	/**
	 * The parent simulation panel that owns this toolbar.
	 * Stored separately from the base-class _parent field to provide a typed
	 * reference to AbstractSimulationPanel without requiring a cast at each use.
	 */
	private AbstractSimulationPanel _parent;

	/**
	 * Tabbed pane positioned below the inherited toolbar controls.
	 * Available for forecast-specific tab content added by the parent or sibling panels.
	 */
	private JTabbedPane _tabbedPane;


	/**
	 * Constructs the panel and wires it to the given AbstractSimulationPanel as its parent.
	 *
	 * Delegates to BaseSimulationGroupPanel, which calls buildControls and addListeners
	 * during construction. The parent reference is also stored locally for typed access.
	 *
	 * @param parent the AbstractSimulationPanel that owns this toolbar; must not be null
	 */
	public SimulationGroupPanel(AbstractSimulationPanel parent) {
		super(parent);

		// Store a typed reference in addition to the base-class field
		_parent = parent;
	}


	/**
	 * Extends the base toolbar layout by appending a JTabbedPane below the inherited controls.
	 *
	 * Calls super.buildControls() first to ensure the label, combo box, and action buttons
	 * are present before the tabbed pane is added. The pane expands to fill all remaining
	 * horizontal and vertical space.
	 */
	@Override
	protected void buildControls() {
		// Build the inherited label, combo box, and action button controls first
		super.buildControls();

		// Append the tabbed pane below the toolbar, expanding to fill remaining space
		_tabbedPane = new JTabbedPane();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);
	}


	/**
	 * Returns the Action bound to the Delete button for forecast simulation groups.
	 *
	 * Both the parent panel and the current actions window are passed to the action
	 * so it can call back into simulationGroupDeleted and access the window context
	 * after a deletion is confirmed.
	 *
	 * @param parent this panel instance, provided to the action for post-deletion callbacks
	 * @return a DeleteForecastSimGroupAction configured with this panel and the actions window
	 */
	@Override
	protected Action getDeleteSimGroupAction(BaseSimulationGroupPanel parent) {
		return new DeleteForecastSimGroupAction(
				parent, ActionPanelPlugin.getInstance().getActionsWindow());
	}


	/**
	 * Returns the Action bound to the New button for creating forecast simulation groups.
	 *
	 * Both the owning AbstractSimulationPanel and this panel are passed so the action
	 * can add the new group to the combo box and select it after creation.
	 *
	 * @return a NewForecastSimGroupAction configured with the parent panel and this panel
	 */
	@Override
	protected Action getNewSimGroupAction() {
		return new NewForecastSimGroupAction(_parent, this);
	}


	/**
	 * Returns the Action bound to the Edit button for editing forecast simulation groups.
	 *
	 * The EditForecastSimGroupAction obtains its context (the selected group) internally
	 * from the active actions window, so no arguments are needed here.
	 *
	 * @return a new EditForecastSimGroupAction instance
	 */
	@Override
	protected Action getEditSimGroupAction() {
		return new EditForecastSimGroupAction();
	}


	/**
	 * Responds to a combo-box item selection event by loading the selected forecast
	 * simulation group and updating the parent AbstractSimulationPanel.
	 *
	 * The DESELECTED event is handled specially: if a proxy is currently selected when
	 * the deselect fires, the parent panel is cleared immediately by calling fillForm(null)
	 * before the subsequent SELECTED event arrives. This prevents momentary display of
	 * stale group data during a transition.
	 *
	 * If the selected proxy is null (combo box empty or cleared), fillForm is called
	 * with null to clear the panel.
	 *
	 * @param e the item event from the combo box; null is never passed by the base class
	 *          for this implementation
	 */
	@Override
	protected void simGroupSelected(ItemEvent e) {
		ManagerProxy proxy = (ManagerProxy) _simulationGroupCombo.getSelectedItem();

		// On DESELECTED with an active proxy, clear the parent panel immediately
		if (e != null && ItemEvent.DESELECTED == e.getStateChange() && proxy != null) {
			fillForm(null);
			return;
		}

		// Resolve the full ForecastSimGroup from the proxy, or null if nothing is selected
		ForecastSimGroup simGroup = null;
		if (proxy != null) {
			simGroup = (ForecastSimGroup) proxy.loadManager();
		}

		// Update the parent panel and Edit button state with the resolved group
		fillForm(simGroup);
	}


	/**
	 * Returns the Class type used to query the project's manager list when loading
	 * the simulation group combo box.
	 *
	 * Returning ForecastSimGroup.class ensures that only forecast simulation groups
	 * (not standard calibration groups) are shown in this panel's combo box.
	 *
	 * @return ForecastSimGroup.class
	 */
	@Override
	protected Class getSimGroupClass() {
		return ForecastSimGroup.class;
	}


	/**
	 * Updates the Edit button state and notifies the parent AbstractSimulationPanel of
	 * the newly selected forecast simulation group.
	 *
	 * The Edit button is enabled only when a non-null group is provided. The parent
	 * panel is always notified so it can clear or populate its detail content accordingly.
	 *
	 * @param simGroup the forecast simulation group to display; null clears the parent panel
	 */
	private void fillForm(ForecastSimGroup simGroup) {
		// Enable the Edit button only when a valid group is currently selected
		boolean enabled = simGroup != null;
		_editButton.setEnabled(enabled);

		// Notify the parent panel to refresh its forecast group detail content
		_parent.setSimulationGroup(simGroup);
	}

}
