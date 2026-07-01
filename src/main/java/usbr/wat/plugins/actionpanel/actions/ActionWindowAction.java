package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;								// Event type delivered when a user triggers a bound action (for example, a menu or button click)
import javax.swing.AbstractAction;								// Swing base class for encapsulating an action that can be attached to UI components
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;			// Plugin entry point used to access and display the Actions window


/**
 * Action that opens the WTMP Actions Window.
 *
 * Provides a user-triggered operation to display the main actions window
 * for the WTMP plugin through the plugin singleton.
 */
@SuppressWarnings("serial")
public class ActionWindowAction extends AbstractAction {
	/**
	 * Creates the action with a descriptive, user-visible name.
	 * <p>
	 * The name appears on bound UI components such as menu items or buttons.
	 */
	public ActionWindowAction() {
		// Set the action's display label used by Swing components
		super("WTMP Actions Window");
	}

	/**
	 * Handles the user-triggered event to show the WTMP Actions Window.
	 *
	 * @param e the action event that initiated the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Delegate to the plugin to display (or bring forward) the actions window
		ActionPanelPlugin.getInstance().displayActionsWindow();
	}
}