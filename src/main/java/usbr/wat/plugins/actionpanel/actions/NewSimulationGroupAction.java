package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;												// Event type delivered when a user triggers a bound action (for example, a button press)

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;							// Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.commands.NewSimulationGroupCmd;				// Command class that constructs a new SimulationGroup instance for creation workflows
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;			// Dialog used to create or edit a simulation group’s metadata and settings
import usbr.wat.plugins.actionpanel.model.SimulationGroup;						// Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;						// Panel for prescribed conditions providing access to the active simulation group
import usbr.wat.plugins.actionpanel.ui.CalibrationSimulationGroupPanel;			// UI panel that lists and manages SimulationGroup entries in the calibration workflow

/**
 * Action that creates a new Simulation Group in the calibration workflow.
 *
 * Opens the "New Simulation Group" dialog, preconfigures factory and options,
 * and, upon confirmation, adds the newly created group to the UI and sets it
 * as the active selection in the calibration panel.
 */
@SuppressWarnings("serial")
public class NewSimulationGroupAction extends BaseActionsPanelAction {
	/**
	 * Panel that displays and manages simulation groups within calibration.
	 */
	private final CalibrationSimulationGroupPanel _simGroupPanel;

	/**
	 * Owning calibration panel used to set the active simulation group.
	 */
	private CalibrationPanel _parent;

	/**
	 * Creates the new-simulation-group action with a user-visible name and initial disabled state.
	 *
	 * @param calibrationPanel the calibration panel that will receive the newly created group
	 * @param simGroupPanel    the UI panel that lists simulation groups and will add the new one
	 */
	public NewSimulationGroupAction(CalibrationPanel calibrationPanel, CalibrationSimulationGroupPanel simGroupPanel) {
		// Initialize the action with its display label
		super("New...");

		// Start disabled until the UI logic enables it (for example, when valid context exists)
		setEnabled(false);

		// Store references to the owning calibration panel and simulation-group panel
		_parent = calibrationPanel;
		_simGroupPanel = simGroupPanel;
	}

	/**
	 * Handles the user-triggered event to create a new Simulation Group.
	 * <p>
	 * Opens the dialog, configures class and factory, optionally runs extract,
	 * and if confirmed, sets the new group as active and adds it to the list.
	 *
	 * @param e the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Create the dialog for new simulation group creation
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(ActionPanelPlugin.getInstance().getActionsWindow(), true, "New Simulation Group");

		// Specify the concrete SimulationGroup class to be created
		dlg.setSimulationGroupClass(SimulationGroup.class);

		// Provide the factory/command used to instantiate and configure the group
		dlg.setSimulationGroupFactory(NewSimulationGroupCmd.class);

		// Indicate extract step should be run as part of creation
		dlg.setRunExtract(true);

		// Populate default fields and any initial state
		dlg.fillForm();

		// Display the dialog to the user
		dlg.setVisible(true);

		// Abort if the user cancels the dialog
		if (dlg.isCanceled()) {
			return;
		}

		// Retrieve the newly created SimulationGroup
		SimulationGroup sg = (SimulationGroup) dlg.getSimulationGroup();

		// Set the new group as the active selection in the calibration panel, when available
		if (_parent != null) {
			_parent.setSimulationGroup(sg);
		}

		// Add the new group to the UI list and optionally select it
		_simGroupPanel.addSimulationGroup(sg, true);
	}
}