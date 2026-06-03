package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;                                              // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;                                                          // Collections interface used for lists of manager proxies and simulations
import javax.swing.AbstractAction;                                              // Swing base class for encapsulating an action that can be attached to UI components

import com.rma.client.ObjectChooser;                                            // Dialog utility that presents objects for selection or deletion
import com.rma.factories.DeleteManagerFactory;                                  // Factory providing deletion operations for managers and their proxies
import com.rma.model.Manager;                                                   // Base manager type representing a managed model object
import com.rma.model.ManagerProxy;                                              // Proxy wrapper that exposes manager instances and metadata
import com.rma.model.Project;                                                   // Accessor for the current project and project-level operations

import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.ActionsWindow;                              // Main actions window used as the UI parent for dialogs and status updates
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;              // Base type representing a simulation group used by the actions
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Forecast-specific simulation group type used by the forecast panel
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;                // Panel exposing common simulation-group functionality and flags

/**
 * Action that deletes one or more forecast simulation groups selected by the user.
 *
 * Presents an object chooser dialog filtered to {@link ForecastSimGroup} proxies,
 * performs deletion of the selected group managers and their simulations, and
 * notifies the UI to refresh.
 */
public class DeleteForecastSimGroupAction extends AbstractAction {
	/**
	 * Owning actions window used as the dialog parent and context source.
	 */
	private final ActionsWindow _parent;

	/**
	 * Panel that hosts forecast simulation groups and receives deletion notifications.
	 */
	private final BaseSimulationGroupPanel _parentPanel;

	/**
	 * Creates the delete-forecast-simulation-group action with a user-visible name.
	 *
	 * @param parentPanel the panel that will be notified when groups are deleted
	 * @param parent      the actions window used as the dialog parent
	 */
	public DeleteForecastSimGroupAction(BaseSimulationGroupPanel parentPanel, ActionsWindow parent) {
		// Initialize the action with its display label
		super("Delete...");

		// Store the parent window reference
		_parent = parent;

		// Store the parent panel reference
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to delete selected forecast simulation groups.
	 * <p>
	 * Shows an {@code ObjectChooser} in delete mode for {@link ForecastSimGroup} entries,
	 * deletes each selected group and its simulations, notifies the parent panel, and
	 * refreshes the forecast panel's group list.
	 *
	 * @param e the action event initiating the deletion request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Retrieve all manager proxies for ForecastSimGroup from the current project
		List<ManagerProxy> simGroups = Project.getCurrentProject().getManagerProxyListForType(ForecastSimGroup.class);

		// Create the chooser dialog in delete mode with the available proxies
		ObjectChooser chooser = new ObjectChooser(ActionPanelPlugin.getInstance().getActionsWindow(), true, simGroups, ObjectChooser.DELETE);

		// Title the chooser appropriately for deletion
		chooser.setTitle("Delete Simulation Groups");

		// Display the chooser dialog
		chooser.setVisible(true);

		// Abort if the user cancels the dialog
		if (chooser.isCanceled()) {
			return;
		}

		// Retrieve the selected objects to delete
		Object[] objects = chooser.getSelectedObjects();

		// If nothing was selected, do nothing
		if (objects == null) {
			return;
		}

		// Access the current project (not used below, but retained from original logic)
		Project prj = Project.getCurrentProject();

		// Loop variables for proxy and manager operations
		ManagerProxy proxy = null;
		Manager manager;

		// Iterate over each selected proxy and perform deletion
		for (int i = 0; i < objects.length; i++) {
			// Cast the selected object to a manager proxy
			proxy = (ManagerProxy) objects[i];

			// Load the concrete manager from the proxy
			manager = proxy.loadManager();

			// Only operate on simulation groups
			if (manager instanceof AbstractSimulationGroup) {
				// Collect simulations before deleting the group
				List<WatSimulation> sims = ((AbstractSimulationGroup) manager).getSimulations();

				// Delete the group manager first
				DeleteManagerFactory.deleteManager(manager);

				// Delete each simulation manager (iterate backward to avoid index shifting)
				for (int n = sims.size() - 1; n >= 0; n--) {
					DeleteManagerFactory.deleteManager(sims.get(n));
				}
			}
		}

		// Notify the parent panel of the deletion, if available
		if (_parentPanel != null) {
			_parentPanel.simulationGroupDeleted(proxy);
		}

		// Refresh the forecast panel's simulation group combo/list
		ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().loadSimulationGroupCombo();

	}
}