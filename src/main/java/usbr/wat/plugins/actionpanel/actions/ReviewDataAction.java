package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;									// Event type delivered when a user triggers a bound action (for example, a button press)

import javax.swing.AbstractAction;									// Swing base class for encapsulating an action that can be attached to UI components
import javax.swing.JOptionPane;										// Swing utility for showing information dialogs

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;				// Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.ActionsWindow;					// Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.UsbrG2dDialog;			// Dialog used to review data associated with the current Simulation Group


/**
 * Action that opens the "Review Data" dialog.
 *
 * Validates that a simulation group is selected in the actions window and,
 * if present, displays the data review dialog. Starts disabled by default
 * and should be enabled when a valid selection exists.
 */
@SuppressWarnings("serial")
public class ReviewDataAction extends AbstractAction {
	/**
	 * Creates the review-data action with a user-visible name and initial disabled state.
	 */
	public ReviewDataAction() {
		// Set the action's display label used by Swing components
		super("Review Data");

		// Start disabled until a simulation group is selected
		setEnabled(false);
	}

	/**
	 * Handles the user-triggered event to review data for the active simulation group.
	 * <p>
	 * Retrieves the actions window, checks for a selected simulation group,
	 * and shows the data review dialog when available.
	 *
	 * @param e the action event that initiated this operation
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Obtain the actions window to use as the UI parent for dialogs
		ActionsWindow parent = ActionPanelPlugin.getInstance().getActionsWindow();

		// Require that a simulation group is selected before proceeding
		if (parent.getSimulationGroup() == null) {
			// Inform the user that a simulation group is needed
			JOptionPane.showMessageDialog(parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Construct the data review dialog with the actions window as parent
		UsbrG2dDialog dialog = new UsbrG2dDialog(parent);

		// Display the dialog to the user
		dialog.setVisible(true);
	}
}