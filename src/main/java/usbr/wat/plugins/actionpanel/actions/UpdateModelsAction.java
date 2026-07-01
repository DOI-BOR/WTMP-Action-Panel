package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                          // Event type delivered when a user triggers a bound action (for example, a button press)

import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;      // Dialog that manages study storage and model updates (Git-integrated workflow)

/**
 * Action that opens the dialog to get or update models for the current study.
 *
 * When invoked, this action displays the {@code StudyStorageDialog}, which is
 * responsible for fetching and updating model artifacts associated with the study.
 */
@SuppressWarnings("serial")
public class UpdateModelsAction extends BaseActionsPanelAction {
	/**
	 * Owning actions window used as the dialog parent and context source.
	 */
	private ActionsWindow _parent;

	/**
	 * Creates the update-models action with a user-visible name.
	 *
	 * @param parent the actions window used as the dialog parent
	 */
	public UpdateModelsAction(ActionsWindow parent) {
		// Initialize the base action with its display label
		super("Get/Update Models");

		// Store the parent window reference for later use
		_parent = parent;
	}

	/**
	 * Handles the user-triggered event to open the models update dialog.
	 *
	 * @param e the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Delegate to helper to construct and show the update dialog
		displayUpdateDialog();

	}

	/**
	 * Displays the dialog used to manage and update study models.
	 * <p>
	 * Constructs the {@code StudyStorageDialog} using the actions window
	 * as the parent and makes it visible.
	 */
	private void displayUpdateDialog() {
		// Create the study storage dialog
		StudyStorageDialog dlg = new StudyStorageDialog(_parent);

		// Show the dialog to begin the update workflow
		dlg.setVisible(true);
	}
}