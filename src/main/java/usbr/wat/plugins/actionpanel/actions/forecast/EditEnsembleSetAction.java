package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;                                              // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;                                                          // Collections interface used for lists of ensemble sets

import javax.swing.AbstractAction;                                              // Swing base class for encapsulating an action that can be attached to UI components

import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Plugin entry point used to obtain the Actions window and global context
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;                 // Forecast model representing a group of simulations and its ensemble sets
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Forecast-specific simulation group type used by the forecast panel
import usbr.wat.plugins.actionpanel.ui.forecast.EditEnsembleSetWindow;          // Dialog window for editing ensemble sets tied to a forecast simulation
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;                // Panel for forecast workflows that displays and manages simulations and ensemble sets

/**
 * Action that opens the "Edit Ensemble Set" workflow for forecast simulations.
 *
 * When invoked, this action:
 * - Creates and shows the ensemble-set editing window
 * - Pre-fills the dialog with the current forecast simulation group and selected simulation
 * - Applies user changes back to the parent panel by updating its displayed ensemble sets
 */
public class EditEnsembleSetAction extends AbstractAction {
	/**
	 * Forecast simulation panel that owns this action and displays ensemble sets.
	 */
	private final SimulationPanel _parentPanel;

	/**
	 * Creates the edit-ensemble-set action with a user-visible name.
	 *
	 * @param parentPanel the forecast simulation panel that will be updated with edited ensemble sets
	 */
	public EditEnsembleSetAction(SimulationPanel parentPanel) {
		// Set the action's display label used by Swing components
		super("Edit Ensemble Set...");

		// Store the parent panel reference for later updates
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to edit ensemble sets.
	 *
	 * @param e the action event initiating the request
	 */
	public void actionPerformed(ActionEvent e) {
		// Delegate to the core editing workflow
		editEnsembleSetAction();
	}

	/**
	 * Opens the ensemble-set editing dialog and applies user selections to the UI.
	 *
	 * Creates the dialog, populates it with the current forecast simulation group
	 * and the selected simulation, then upon confirmation sets the selected ensemble
	 * sets back onto the parent panel for display.
	 */
	public void editEnsembleSetAction() {
		// Create the edit dialog using the plugin's actions window as the parent
		EditEnsembleSetWindow dlg = new EditEnsembleSetWindow(ActionPanelPlugin.getInstance().getActionsWindow());

		// Retrieve the active forecast simulation group from the forecast panel
		ForecastSimGroup simGroup = ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationGroup();

		// Retrieve the currently selected forecast simulation
		WatSimulation simulation = ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSelectedSimulation();

		// Pre-populate the dialog with the simulation group and selected simulation
		dlg.fillForm(simGroup, simulation);

		// Display the dialog to the user
		dlg.setVisible(true);

		// If the user canceled, do not apply changes
		if (dlg.isCanceled()) {
			return;
		}

		// Fill the parent panel with the ensemble sets selected/edited in the dialog
		List<EnsembleSet> ensembleSets = simGroup.getEnsembleSets(simulation);
		_parentPanel.setEnsembleSets(ensembleSets);
	}
}