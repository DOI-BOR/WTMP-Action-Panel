package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                      // Event type delivered when a user triggers a bound action (for example, a button press)

import javax.swing.AbstractAction;                                      // Swing base class for encapsulating an action that can be attached to UI components

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                  // Plugin entry point used to access the singleton and obtain the Actions window
import usbr.wat.plugins.actionpanel.ActionsWindow;                      // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.extract.ui.ExtractDialog;           // Dialog that performs data extract and update operations for a simulation group
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;      // Base type representing a simulation group used by actions and dialogs


/**
 * Action that opens the data extract/update dialog for the current Simulation Group.
 *
 * This action is intended to retrieve or refresh input datasets used by simulations.
 * It can optionally run a follow-up task after the update completes, provided via a
 * {@link Runnable} in the constructor.
 */
@SuppressWarnings("serial")
public class UpdateDataAction extends AbstractAction {
	/**
	 * System property flag that indicates whether the Update Data feature should be available.
	 */
	public static final String DASH_D_FLAG = "WTMP.HasUpdateData";

	/**
	 * Optional task to run after the update dialog finishes (for example, refresh UI or kick off a workflow).
	 */
	private final Runnable _postUpdateAction;

	/**
	 * Creates the update-data action with default behavior and no post-update task.
	 * <p>
	 * The action is initialized disabled; calling code should enable it when
	 * a valid simulation group is available or when the feature is permitted by the environment.
	 */
	public UpdateDataAction() {
		this(null);
	}

	/**
	 * Creates the update-data action with an optional post-update task.
	 *
	 * @param postUpdateAction a runnable to execute after the data update completes; may be null
	 */
	public UpdateDataAction(Runnable postUpdateAction) {
		// Set the action's display label used by Swing components
		super("Get/Update Data");

		// Start disabled until UI logic enables it (for example, when a group is selected)
		setEnabled(false);

		// Store the optional follow-up action to run after the dialog finishes
		_postUpdateAction = postUpdateAction;
	}

	/**
	 * Handles the user-triggered event to update data for the active simulation group.
	 * <p>
	 * Obtains the current simulation group from the actions window and opens the
	 * extract/update dialog.
	 *
	 * @param e the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Retrieve the simulation group from the plugin's actions window and invoke the dialog
		updateData(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());
	}

	/**
	 * Opens the extract/update dialog for the provided simulation group.
	 * <p>
	 * The dialog is responsible for performing data retrieval and updates.
	 * If a post-update runnable was provided, it will be passed to the dialog
	 * and invoked upon completion.
	 *
	 * @param simulationGroup the simulation group whose data should be updated
	 */
	public void updateData(AbstractSimulationGroup simulationGroup) {
		// Construct the extract dialog using the actions window as parent
		ExtractDialog dlg = new ExtractDialog(ActionPanelPlugin.getInstance().getActionsWindow(), simulationGroup, _postUpdateAction);

		// Display the dialog to begin the update workflow
		dlg.setVisible(true);
	}
}