package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                          // Event type delivered when a user triggers a bound action (for example, a button press)

import javax.swing.AbstractAction;                                          // Swing base class for encapsulating an action that can be attached to UI components

import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.SelectSimulationGroupDialog;    // Dialog that allows the user to choose an existing Simulation Group
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                  // Concrete type representing a simulation group managed within the plugin

/**
 * Action that opens a selector to choose an existing Simulation Group.
 *
 * When invoked, the action displays a modal selection dialog. If the user confirms
 * a selection, the chosen {@link SimulationGroup} is set as the active group in
 * the associated {@link ActionsWindow}.
 */
@SuppressWarnings("serial")
public class SelectSimulationGroupAction extends AbstractAction {
	/**
	 * Owning actions window used as the dialog parent and target for setting the selection.
	 */
	private ActionsWindow _parent;

	/**
	 * Creates the select-simulation-group action with a user-visible name and initial disabled state.
	 *
	 * @param parent the actions window used as the dialog parent and context source
	 */
	public SelectSimulationGroupAction(ActionsWindow parent) {
		// Set the action's display label used by Swing components
		super("Select Simulation Group...");

		// Start disabled until UI logic enables it (for example, when groups are available to select)
		setEnabled(false);

		// Store the parent window reference for later use
		_parent = parent;
	}

	/**
	 * Handles the user-triggered event to select a Simulation Group.
	 * <p>
	 * Opens the selection dialog, and if the user confirms, sets the chosen
	 * group as the active selection in the actions window.
	 *
	 * @param e the action event initiating the selection request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Construct the selection dialog with the parent window and modal behavior
		SelectSimulationGroupDialog editor = new SelectSimulationGroupDialog(_parent, true);

		// Display the dialog to the user
		editor.setVisible(true);

		// If the user cancels, do not change the current selection
		if (editor.isCanceled()) {
			return;
		}

		// Retrieve the selected Simulation Group from the dialog
		SimulationGroup sg = editor.getSelectedSimulationGroup();

		// Apply the selected group to the actions window
		_parent.setSimulationGroup(sg);
	}
}