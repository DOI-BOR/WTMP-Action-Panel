package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;											// Event type delivered when a user triggers a bound action (for example, a button press)

import javax.swing.AbstractAction;											// Swing base class for encapsulating an action attached to UI components

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;						// Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.ActionsWindow;							// Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;		// Dialog used to create or edit a simulation group’s metadata and settings
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;			// Base type representing a simulation group used by the actions
import usbr.wat.plugins.actionpanel.model.SimulationGroup;					// Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;				// Base panel type that exposes simulation group operations to the UI

/**
 * Action that opens the editor to modify a Simulation Group.
 *
 * Validates that a group exists, opens the editor dialog pre-populated
 * with the current group's data, and applies the user's changes back to
 * the owning panel if the dialog is not canceled.
 *
 */
public class EditSimulationGroupAction extends AbstractAction {

	/**
	 * Owning actions window used as the parent for dialogs and to access selections.
	 */
	private ActionsWindow _parent;

	/**
	 * Panel that hosts and updates the current simulation group in the UI.
	 */
	private AbstractSimulationPanel _parentPanel;

	/**
	 * Creates the edit-simulation-group action with a user-visible name and initial disabled state.
	 *
	 * @param parent      the actions window used as the dialog parent and context source
	 * @param parentPanel the simulation panel that will receive updated group data
	 */
	public EditSimulationGroupAction(ActionsWindow parent, AbstractSimulationPanel parentPanel) {
		// Initialize the action with its display label
		super("Edit ...");

		// Start disabled until the UI logic enables it (for example, when a group is selected)
		setEnabled(false);

		// Store the parent references for later use
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to edit the current simulation group.
	 * <p>
	 * Constructs the edit dialog, populates it with the existing group,
	 * shows it, and if confirmed, applies the changes back to the panel.
	 *
	 * @param arg0 the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Create the dialog in edit mode with a specific title
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true, "Edit Simulation Group");

		// Retrieve the current simulation group from the calibration panel
		SimulationGroup simGroup = ActionPanelPlugin.getInstance().getActionsWindow().getCalibrationPanel().getSimulationGroup();

		// Pre-populate the dialog fields with the existing group data
		dlg.fillForm(simGroup);

		// Display the dialog to the user
		dlg.setVisible(true);

		// If the user cancels, do not apply any changes
		if (dlg.isCanceled()) {
			return;
		}

		// Get the updated simulation group from the dialog
		AbstractSimulationGroup sg = dlg.getSimulationGroup();

		// Apply the updated group back to the owning panel
		_parentPanel.setSimulationGroup(sg);
	}
}
