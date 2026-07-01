package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                              // Event type delivered when a user triggers a bound action (for example, a button press)

import javax.swing.AbstractAction;                                              // Swing base class for encapsulating an action that can be attached to UI components

import com.rma.model.Project;                                                   // Plugin entry point used to obtain the Actions window and global context

import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Provides access to the plugin singleton and its actions window
import usbr.wat.plugins.actionpanel.commands.SaveSimulationToGroupCmd;          // Command that copies a simulation into a group under a new name/description
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                      // Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.ui.SaveSimulationAsDialog;                  // Dialog used to capture "Save As" inputs (new name and description)
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                               // Base USBR panel type implemented by calibration and forecast panels

/**
 * Action that saves an existing simulation into a simulation group under a new name.
 *
 * This action presents a "Save Simulation As..." dialog to the user, collects
 * the desired name and description, and then executes a command to create a copy
 * of the source simulation in the specified {@link SimulationGroup}.
 */
public class SaveSimulationAsAction extends AbstractAction {
	/**
	 * Parent panel used to refresh the UI (for example, update simulation tables) after saving.
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Creates the "Save Simulation As..." action with a user-visible name.
	 *
	 * @param parentPanel the owning panel used for UI updates after the save completes
	 */
	public SaveSimulationAsAction(UsbrPanel parentPanel) {
		// Initialize the action with its display label
		super("Save Simulation As...");

		// Store the parent panel reference for later UI updates
		_parentPanel = parentPanel;

	}

	/**
	 * Handles the user-triggered event.
	 *
	 * This action is invoked programmatically via {@link #saveSimulationAs(SimulationGroup, WatSimulation)},
	 * so the default actionPerformed is intentionally empty.
	 *
	 * @param e the action event that initiated this operation
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
	}

	/**
	 * Saves the provided source simulation into the given simulation group under a new name.
	 *
	 * Opens the "Save Simulation As" dialog to collect the new name and description,
	 * executes the save command, and refreshes the parent panel UI if successful.
	 *
	 * @param simGroup the target simulation group to receive the new simulation
	 * @param srcSim   the source simulation to copy
	 * @return true if the simulation was saved and added successfully; false otherwise
	 */
	public boolean saveSimulationAs(SimulationGroup simGroup, WatSimulation srcSim) {
		// Validate inputs to ensure both a target group and source simulation are provided
		if (simGroup == null || srcSim == null) {
			return false;
		}

		// Create the dialog and set its parent to the plugin's actions window
		SaveSimulationAsDialog dlg = new SaveSimulationAsDialog(ActionPanelPlugin.getInstance().getActionsWindow());

		// Pre-populate the dialog with the current group and source simulation information
		dlg.fillForm(simGroup, srcSim);

		// Show the dialog to the user
		dlg.setVisible(true);

		// If the user canceled the operation, abort without making changes
		if (dlg.isCanceled()) {
			return false;
		}

		// Retrieve the new name and description entered by the user
		String newName = dlg.getSaveAsName();

		String newDesc = dlg.getDescription();

		// Build the command to create the new simulation within the specified group
		SaveSimulationToGroupCmd cmd = new SaveSimulationToGroupCmd(srcSim, newName, newDesc, simGroup, Project.getCurrentProject(), simGroup.getAnalysisPeriod(), false);

		// Execute the command to perform the copy/save operation
		cmd.doCommand();

		// Obtain the newly created simulation from the command
		WatSimulation newSim = cmd.getSimulation();

		// If a new simulation was created successfully, add it to the group and refresh the UI
		if (newSim != null) {
			// Add the newly saved simulation to the target group
			simGroup.addSimulation(newSim);

			// Refresh UI components that display simulations (for example, tables/lists)
			_parentPanel.fillSimulationTable();

			return true;
		}

		// If creation failed or returned null, indicate failure
		return false;
	}
}