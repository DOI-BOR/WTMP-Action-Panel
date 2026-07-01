package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;												// Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;															// Collections interface used for lists of simulations

import javax.swing.AbstractAction;												// Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;													// Swing utility for showing information dialogs

import hec2.wat.model.WatSimulation;											// WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionsWindow;								// Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;		// Dialog used to edit compute/iteration settings for simulations
import usbr.wat.plugins.actionpanel.model.SimulationGroup;						// Concrete type representing a simulation group managed within the plugin

/**
 * Action that opens the dialog to edit compute settings for interactive simulations.
 *
 * Validates that a simulation group is selected, optionally pre-selects a simulation
 * if one is highlighted in the calibration panel, and displays the iteration settings dialog.
 */
@SuppressWarnings("serial")
public class EditInterativeSimulationAction extends AbstractAction {

	/**
	 * Owning actions window used as parent for dialogs and to access selections.
	 */
	private ActionsWindow _parent;

	/**
	 * Creates the edit-iteration-settings action with a user-visible name and initial disabled state.
	 *
	 * @param parent the actions window used as the dialog parent and context source
	 */
	public EditInterativeSimulationAction(ActionsWindow parent) {
		// Initialize the action with its display label
		super("Edit Compute Settings...");

		// Start disabled until the UI logic enables it (e.g., when a group is selected)
		setEnabled(false);

		// Store the parent actions window reference
		_parent = parent;
	}

	/**
	 * Handles the user-triggered event to open the compute settings dialog.
	 * <p>
	 * Validates that a simulation group exists; if one does, opens the dialog,
	 * populates it with the group's data, and pre-selects the first selected
	 * simulation when available.
	 *
	 * @param arg0 the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Retrieve the active simulation group from the calibration panel
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();

		// Require a selected simulation group
		if (simGroup == null) {
			// Inform the user to select or create a simulation group
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Gather simulations currently selected in the calibration panel
		List<WatSimulation> sims = _parent.getCalibrationPanel().getSelectedSimulations();

		// Construct the iteration settings dialog with the actions window as parent
		EditIterationSettingsDialog dlg = new EditIterationSettingsDialog(_parent);

		// Populate the dialog with the selected simulation group's data
		dlg.fillForm(simGroup);

		// If a simulation is selected, pre-select it in the dialog
		if (sims.size() > 0) {
			dlg.setSelectedSimulation(sims.get(0));
		}

		// Display the dialog to the user
		dlg.setVisible(true);

	}
}