package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for the separator added below the base controls
import java.awt.event.ItemEvent;     // Carries combo-box selection change data; used to filter out DESELECTED events

import javax.swing.Action;           // Swing Action interface returned by the three abstract factory methods
import javax.swing.JSeparator;       // Horizontal visual divider appended below the inherited toolbar controls

import com.rma.model.ManagerProxy;   // Lightweight proxy wrapping a managed model object; selected item in the combo box

import rma.swing.RmaInsets;          // Constants for common GridBagLayout inset configurations

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                      // Singleton plugin entry point providing access to the actions window
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;    // Action that deletes the selected calibration simulation group
import usbr.wat.plugins.actionpanel.actions.EditSimulationGroupAction;      // Action that opens the editor for the selected calibration simulation group
import usbr.wat.plugins.actionpanel.actions.NewSimulationGroupAction;       // Action that opens the dialog to create a new calibration simulation group
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                  // Standard (non-forecast) simulation group model managed by this panel
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Forecast simulation group model (imported for type context; not used directly)


/**
 * Concrete simulation group toolbar panel for the calibration workflow.
 *
 * This class extends BaseSimulationGroupPanel to provide the calibration-specific
 * implementations of the three action factory methods and the combo-box selection
 * handler. It manages SimulationGroup objects (as opposed to ForecastSimGroup objects
 * managed by the forecast equivalent).
 *
 * Beyond the controls inherited from BaseSimulationGroupPanel, this panel appends a
 * horizontal JSeparator below the toolbar row to visually separate the group controls
 * from the simulation detail content below.
 *
 * When a simulation group is selected from the combo box, the group's manager is
 * loaded from the proxy and passed to the owning CalibrationPanel via fillForm,
 * which also enables or disables the Edit button based on whether a valid group exists.
 */
public class CalibrationSimulationGroupPanel extends BaseSimulationGroupPanel {
	/**
	 * Constructs the panel and wires it to the given CalibrationPanel as its parent.
	 *
	 * Delegates to BaseSimulationGroupPanel, which calls buildControls and addListeners
	 * during construction.
	 *
	 * @param calibrationPanel the CalibrationPanel that owns this toolbar; must not be null
	 */
	public CalibrationSimulationGroupPanel(CalibrationPanel calibrationPanel) {
		super(calibrationPanel);
	}


	/**
	 * Extends the base toolbar layout by appending a horizontal separator below the
	 * inherited controls.
	 *
	 * Calls super.buildControls() first to ensure all base components (label, combo box,
	 * Edit/New/Delete buttons) are added before the separator is appended.
	 */
	@Override
	protected void buildControls() {
		// Build the inherited label, combo box, and action button controls first
		super.buildControls();

		// Append a full-width horizontal separator to visually divide the toolbar from
		// the simulation detail panel below
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);
	}


	/**
	 * Returns the Action bound to the Delete button for calibration simulation groups.
	 *
	 * The parent panel reference is passed to the action so it can call back into
	 * simulationGroupDeleted(ManagerProxy) after a deletion is confirmed.
	 *
	 * @param parent this panel instance, provided to the action for post-deletion callbacks
	 * @return a DeleteSimulationGroupAction configured for this panel
	 */
	@Override
	protected Action getDeleteSimGroupAction(BaseSimulationGroupPanel parent) {
		return new DeleteSimulationGroupAction(parent);
	}


	/**
	 * Returns the Action bound to the New button for creating calibration simulation groups.
	 *
	 * Both the owning CalibrationPanel and this panel are passed to the action so that
	 * after a group is created it can be added to the combo box and selected.
	 *
	 * @return a NewSimulationGroupAction configured with the parent CalibrationPanel
	 * and this panel
	 */
	@Override
	protected Action getNewSimGroupAction() {
		return new NewSimulationGroupAction((CalibrationPanel) _parent, this);
	}


	/**
	 * Returns the Action bound to the Edit button for editing calibration simulation groups.
	 *
	 * The actions window and the parent simulation panel are passed to the action to
	 * provide the dialog with the necessary context for editing.
	 *
	 * @return an EditSimulationGroupAction configured with the current actions window
	 * and parent panel
	 */
	@Override
	protected Action getEditSimGroupAction() {
		return new EditSimulationGroupAction(
				ActionPanelPlugin.getInstance().getActionsWindow(), _parent);
	}


	/**
	 * Responds to a combo-box item selection event by loading the selected simulation
	 * group and updating the parent CalibrationPanel.
	 *
	 * DESELECTED events are ignored so that only the final SELECTED state triggers a
	 * panel update. If the selected item is null (i.e. the combo box is empty or cleared)
	 * fillForm is called with null to clear the panel.
	 *
	 * @param e the item event from the combo box; null when called programmatically
	 *          to force a refresh of the current selection
	 */
	@Override
	protected void simGroupSelected(ItemEvent e) {
		// Ignore deselect events; only act when an item is being selected
		if (e != null && ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Retrieve the proxy for the newly selected item; may be null if nothing is selected
		ManagerProxy proxy = (ManagerProxy) _simulationGroupCombo.getSelectedItem();

		SimulationGroup simGroup = null;

		if (proxy != null) {
			// Load the full SimulationGroup model object from the lightweight proxy
			simGroup = (SimulationGroup) proxy.loadManager();
		}

		// Update the parent panel and button states with the resolved group
		fillForm(simGroup);
	}


	/**
	 * Updates the Edit button state and notifies the parent CalibrationPanel of the
	 * newly selected simulation group.
	 *
	 * The Edit button is enabled only when a non-null group is provided. The parent
	 * panel is always notified so it can clear or populate its detail fields accordingly.
	 *
	 * @param simGroup the simulation group to display; null clears the parent panel
	 */
	private void fillForm(SimulationGroup simGroup) {
		// Enable the Edit button only when a valid group is selected
		boolean enabled = simGroup != null;
		_editButton.setEnabled(enabled);

		// Notify the parent CalibrationPanel to refresh its displayed group details
		_parent.setSimulationGroup(simGroup);
	}


	/**
	 * Returns the Class type used to query the project's manager list when loading
	 * the simulation group combo box.
	 *
	 * Returning SimulationGroup.class ensures that only standard calibration simulation
	 * groups (not forecast groups) are shown in this panel's combo box.
	 *
	 * @return SimulationGroup.class
	 */
	@Override
	protected Class getSimGroupClass() {
		return SimulationGroup.class;
	}
}
