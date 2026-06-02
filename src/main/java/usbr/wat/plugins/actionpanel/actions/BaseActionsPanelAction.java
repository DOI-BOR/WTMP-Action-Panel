package usbr.wat.plugins.actionpanel.actions;

import javax.swing.AbstractAction;									// Swing base class for encapsulating an action that can be attached to UI components

/**
 * Base class for actions used by panels in the WTMP plugin.
 *
 * Provides a common constructor to set the action's display text.
 * Subclasses should implement behavior in {@link javax.swing.AbstractAction#actionPerformed(java.awt.event.ActionEvent)}.
 */
@SuppressWarnings("serial")
public abstract class BaseActionsPanelAction extends AbstractAction {
	/**
	 * Creates a panel action with the specified display text.
	 *
	 * @param text the user-visible label for this action (for example, button or menu item text)
	 */
	public BaseActionsPanelAction(String text) {
		// Initialize the Swing action with the provided label
		super(text);

	}
}