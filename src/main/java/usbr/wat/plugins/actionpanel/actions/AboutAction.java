package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;									// Event type delivered when a user triggers a bound action (e.g., button press)
import javax.swing.AbstractAction;									// Swing base class for encapsulating an action that can be attached to UI components
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;				// Plugin entry point used to obtain the Actions window as the dialog parent
import usbr.wat.plugins.actionpanel.ui.AboutDialog;					// Dialog that displays "About" information for the WTMP Action Panel plugin

/**
 * Action that opens the About dialog for the WTMP Action Panel plugin.
 *
 * Provides a user-triggered operation that constructs and displays
 * the About dialog, using the plugin's actions window as the parent.
 *
 */
@SuppressWarnings("serial")
public class AboutAction extends AbstractAction {
	/**
	 * Creates the About action with a user-visible name.
	 * <p>
	 * The action's name appears on bound components such as buttons or menu items.
	 */
	public AboutAction() {
		// Set the action's display name used by Swing components
		super("About...");
	}

	/**
	 * Handles the user-triggered event to show the About dialog.
	 *
	 * @param e the action event that initiated this operation
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Construct the About dialog using the plugin's actions window as the parent
		AboutDialog dlg = new AboutDialog(ActionPanelPlugin.getInstance().getActionsWindow());

		// Display the About dialog to the user
		dlg.setVisible(true);
	}
}