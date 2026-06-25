package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                          // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;														// Makes the list structure available

import javax.swing.AbstractAction;                                          // Swing base class for encapsulating an action that can be attached to UI components
import javax.swing.JOptionPane;                                             // Swing utility for showing information dialogs

import com.rma.io.FileManagerImpl;                                          // File manager implementation for filesystem existence checks and operations
import rma.util.RMAIO;                                                      // RMA I/O utility helpers for path operations and safe concatenation

import hec.dssgui.ListSelection;                                            // DSS GUI component that opens and displays DSS files in a selection viewer
import hec2.wat.model.WatSimulation;                                        // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;              // Base settings for compute operations used to locate outputs (e.g., DSS collection file)
import usbr.wat.plugins.actionpanel.model.ComputeType;                      // Enumeration defining compute types (e.g., Standard vs. collection-based runs)
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                  // Concrete type representing a simulation group managed within the plugin

/**
 * Action that opens DSS result files for the selected simulations and displays them
 * in the DSS viewer.
 *
 * Validates a selected Simulation Group, gathers selected simulations, determines
 * the correct DSS file path based on compute type and settings, opens available DSS
 * files in the viewer, and then presents the viewer to the user.
 */
@SuppressWarnings("serial")
public class ViewIterationResultsAction extends AbstractAction {

	/**
	 * Owning actions window used as the UI parent and context source.
	 */
	private ActionsWindow _parent;

	/**
	 * Creates the view-iteration-results action with a user-visible name.
	 *
	 * @param parent the actions window used as the dialog parent and context source
	 */
	public ViewIterationResultsAction(ActionsWindow parent) {
		// Set the action's display label used by Swing components
		super("View DSS Results...");

		// Store the parent window reference
		_parent = parent;
	}

	/**
	 * Handles the user-triggered event to view DSS results for selected simulations.
	 *
	 * Validates that a simulation group is selected, gathers selected simulations,
	 * computes the DSS file path for each based on compute type, opens files found,
	 * and shows the DSS viewer when at least one file is opened.
	 *
	 * @param e the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Retrieve the active simulation group from the calibration panel
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();

		// Require a selected simulation group before proceeding
		if (simGroup == null) {
			// Inform the user that a simulation group is needed
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;

		}

		// Gather simulations currently selected in the actions window
		List<WatSimulation> sims = _parent.getSelectedSimulations();

		// Create a DSS viewer selection window; enable full functionality
		ListSelection dssVue = new ListSelection("Compute Results",
				ListSelection.FULL_FUNCTION, true, false, false);

		// Loop locals
		WatSimulation sim;

		String simDssFile, computeDssFile;

		// Track whether any DSS file was successfully opened
		boolean openedFile = false;

		// Compute type influences which DSS file to open
		ComputeType computeType;

		// Compute settings provide the collection DSS filename when needed
		BaseComputeSettings computeSettings;

		// Iterate over selected simulations and open their corresponding DSS files
		for (int i = 0; i < sims.size(); i++) {
			// Current simulation
			sim = sims.get(i);

			// Determine the compute type configured for this simulation
			computeType = simGroup.getComputeType(sim.getName());

			// Default DSS file produced by the simulation
			simDssFile = sim.getSimulationDssFile();

			// For non-standard compute types, derive the collection DSS file path
			if (computeType != ComputeType.Standard) {
				// Start with the directory containing the simulation DSS file
				simDssFile = RMAIO.getDirectoryFromPath(simDssFile);

				// Obtain compute settings for this simulation under the selected compute type
				computeSettings = simGroup.getComputeSettings(sim.getName(), computeType);

				// Build the path to the collection DSS file using settings
				computeDssFile = RMAIO.concatPath(simDssFile, computeSettings.getCollectionDssFilename());
			} else {
				// For Standard compute type, use the simulation's DSS file directly
				computeDssFile = simDssFile;
			}

			// Diagnostic output for the DSS file being opened
			System.out.println("actionPerformed:Sim compute dss file: " + computeDssFile);

			// If the computed DSS file exists, open it in the viewer
			if (FileManagerImpl.getFileManager().fileExists(computeDssFile)) {
				dssVue.openDSSFile(computeDssFile);

				openedFile = true;
			}
		}

		// If at least one DSS file was opened, show the viewer dialog to the user
		if (openedFile) {
			// Position the viewer relative to the actions window
			dssVue.setLocationRelativeTo(_parent);

			// Display the DSS viewer
			dssVue.setVisible(true);
		}

	}
}