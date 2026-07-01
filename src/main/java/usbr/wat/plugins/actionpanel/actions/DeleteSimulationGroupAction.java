package usbr.wat.plugins.actionpanel.actions;

import java.awt.Cursor;                                                     // AWT cursor utility used to display wait and default cursors during long operations
import java.awt.event.ActionEvent;                                          // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;                                                      // Collections interface used for lists of simulations

import javax.swing.AbstractAction;                                          // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                             // Swing utility for showing information and confirmation dialogs

import com.rma.factories.DeleteManagerFactory;                              // Factory providing deletion operations and utilities for managers
import com.rma.model.ManagerProxy;                                          // Proxy wrapper that exposes manager instances and metadata
import com.rma.model.Project;                                               // Accessor for the current project and project-level operations

import hec2.wat.model.WatSimulation;                                        // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                      // Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window used as the UI parent for dialogs and status updates
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;          // Base type representing a simulation group used by the actions
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                  // Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Forecast-specific simulation group type used by the forecast panel
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;            // Panel exposing common simulation-group functionality and flags


/**
 * Action that deletes a selected Simulation Group and all its simulations.
 *
 * Validates that a group is selected, confirms with the user, deletes the group's
 * simulations and the group itself via the manager factory, and cleans up related UI.
 */
public class DeleteSimulationGroupAction extends AbstractAction {

	/** Parent panel providing context; may be calibration or forecast panel. */
	private final BaseSimulationGroupPanel _parentPanel;

	/**
	 * Creates the delete action with a user-visible name and an initial disabled state.
	 *
	 * @param parentPanel the panel hosting the action and providing context
	 */
	public DeleteSimulationGroupAction(BaseSimulationGroupPanel parentPanel) {
		// Initialize the action with its display label
		super("Delete");

		// Start disabled until a valid selection enables it
		setEnabled(false);

		// Save the parent panel reference
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to delete the active Simulation Group.
	 *
	 * @param arg0 the action event initiating the deletion request
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Obtain the actions window as the UI parent
		ActionsWindow parent = ActionPanelPlugin.getInstance().getActionsWindow();

		// Get the currently selected simulation group from the window
		AbstractSimulationGroup simGroup = parent.getSimulationGroup();

		// Require a selected simulation group
		if ( simGroup == null ) {
			// Inform the user to select or create a simulation group
			JOptionPane.showMessageDialog(parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Confirm deletion of the group and all its simulations
		int opt = JOptionPane.showConfirmDialog(parent, "<html>Do you want to delete Simulation Group <b>"
				+simGroup.getName()+"</b> and all its simulations", "Confirm Delete", JOptionPane.YES_NO_OPTION);

		// Abort if the user chooses not to proceed
		if ( opt != JOptionPane.YES_OPTION ) {
			return;
		}

		// Perform the deletion and update UI accordingly
		boolean rv = deleteSimulationGroup(simGroup);

		if ( rv ) {
			// Clear the window's selection after successful deletion
			parent.setSimulationGroup(null);

		} else {
			// Inform the user that some part of the deletion failed
			JOptionPane.showMessageDialog(parent, "Failed to completely delete Simulation Group "
					+simGroup.getName(),"Delete Failed", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Deletes the given Simulation Group and all of its simulations.
	 *
	 * Sets a wait cursor during the operation, removes each simulation manager,
	 * deletes the group manager, and cleans up the UI.
	 *
	 * @param simGroup the simulation group to delete
	 * @return true when all deletions succeed, false otherwise
	 */
	public boolean deleteSimulationGroup(AbstractSimulationGroup simGroup) {
		// Validate input
		if ( simGroup == null ) {
			return false;
		}

		// Indicate work in progress to the user
		ActionPanelPlugin.getInstance().getActionsWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Obtain the manager proxy to support UI cleanup routing
		ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(simGroup);

		// Delete all child managers first
		boolean rv = deleteManagers(simGroup);

		try {
			// Delete the simulation group manager itself
			rv &= DeleteManagerFactory.deleteManager(simGroup);

			// Notify appropriate panel to update UI
			cleanUI(proxy);

			return rv;

		} finally {
			// Restore default cursor after operation completes
			ActionPanelPlugin.getInstance().getActionsWindow().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Updates UI components after a simulation group is deleted.
	 *
	 * Routes the deletion notification to the forecast or calibration panel
	 * based on the proxy's class name.
	 *
	 * @param proxy the manager proxy associated with the deleted group
	 */
	private void cleanUI(ManagerProxy proxy) {

		if ( proxy.getClassName().equals(ForecastSimGroup.class.getName()) ) {
			// Notify forecast panel if the deleted group is a ForecastSimGroup
			ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().simulationGroupDeleted(proxy);

		} else if ( proxy.getClassName().equals(SimulationGroup.class.getName()) ) {
			// Notify calibration panel if the deleted group is a regular SimulationGroup
			ActionPanelPlugin.getInstance().getActionsWindow().getCalibrationPanel().getSimulationPanel().simulationGroupDeleted(proxy);
		}
	}

	/**
	 * Deletes all WatSimulation managers contained in the given group.
	 *
	 * Iterates over the group's simulations and deletes each manager, returning
	 * false if any deletion fails.
	 *
	 * @param simGroup the group whose simulations will be deleted
	 * @return true if all simulation managers were deleted, false otherwise
	 */
	private boolean deleteManagers(AbstractSimulationGroup simGroup) {
		// Retrieve all simulations from the group
		List<WatSimulation> sims = simGroup.getSimulations();
		WatSimulation sim;

		boolean rv = true;

		// Attempt to delete each simulation manager
		for(int i = 0; i < sims.size(); i++) {
			sim = sims.get(i);

			if (!DeleteManagerFactory.deleteManager(sim)) {
				// Track failure but continue processing remaining simulations
				rv = false;
			}
		}

		return rv;
	}

	/**
	 * Deletes a Simulation Group using its manager proxy and cleans up UI.
	 *
	 * Sets a wait cursor during the operation, removes all child managers, deletes
	 * the group manager using the proxy, and notifies the appropriate panel for UI updates.
	 *
	 * @param proxy the manager proxy representing the simulation group to delete
	 * @return true when all deletions succeed, false otherwise
	 */
	public boolean deleteSimulationGroup(ManagerProxy proxy) {
		// Validate input
		if ( proxy == null ) {
			return false;
		}

		// Indicate work in progress to the user
		ActionPanelPlugin.getInstance().getActionsWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Delete all child managers first
		boolean rv = deleteManagers((AbstractSimulationGroup) proxy.getManager());

		try {
			// Delete the simulation group manager represented by the proxy
			rv &= DeleteManagerFactory.deleteManager(proxy.getManager());

			// Notify appropriate panel to update UI
			cleanUI(proxy);

			return rv;

		} finally {
			// Restore default cursor after operation completes
			ActionPanelPlugin.getInstance().getActionsWindow().setCursor(Cursor.getDefaultCursor());
		}
	}
}