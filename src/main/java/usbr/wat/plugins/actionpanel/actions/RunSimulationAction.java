package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                                          // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.ArrayList;                                                                 // Resizable list used to collect computables for execution
import java.util.Iterator;                                                                  // Iterator used to traverse the selected simulations
import java.util.List;                                                                      // Collections interface used for lists of simulations

import javax.swing.AbstractAction;                                                          // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                                             // Swing utility for showing information dialogs

import com.rma.client.Browser;                                                              // Host application's main browser frame used to parent dialogs

import hec2.wat.model.WatSimulation;                                                        // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionsWindow;                                          // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;     // Dialog that lets users select computations to run and monitors progress
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;                          // Base type representing a simulation group used by the actions
import usbr.wat.plugins.actionpanel.model.ActionComputable;                                 // Computable wrapper used to initiate calculations from actions
import usbr.wat.plugins.actionpanel.model.IterationSettings;                                // Settings controlling iterative compute behavior (counts, thresholds, etc.)
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;                         // Settings for position analysis used during computations
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                                  // Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.model.UsbrComputable;                                   // Interface for computable tasks consumed by the compute selector dialog
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                                           // Base USBR panel type implemented by calibration and forecast panels

/**
 * Action that runs computations for the selected simulations in the calibration workflow.
 *
 * Detects whether the user requested a full recompute with the Control key,
 * gathers the selected simulations, builds computable tasks using group-specific
 * iteration and position analysis settings, configures the compute selector dialog,
 * and initiates the compute process. After completion, updates compute states in the UI.
 */
@SuppressWarnings("serial")
public class RunSimulationAction extends AbstractAction {
	/**
	 * Owning actions window used as the parent for dialogs and to access selections.
	 */
	private ActionsWindow _parent;

	/**
	 * Panel providing context and post-compute UI updates.
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Creates the run-simulation action with a user-visible name and initial disabled state.
	 *
	 * @param parent      the actions window used as the dialog parent and context source
	 * @param parentPanel the workflow panel (calibration or forecast) that triggers updates after compute
	 */
	public RunSimulationAction(ActionsWindow parent, UsbrPanel parentPanel) {
		// Initialize the action with its display label
		super("Run Simulation");

		// Start disabled until the UI logic enables it (e.g., when simulations are selected)
		setEnabled(false);

		// Store references to the owning window and parent panel
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to compute selected simulations.
	 * <p>
	 * Validates that a simulation group exists and that simulations are selected,
	 * builds computable tasks using iteration and position analysis settings per simulation,
	 * configures the compute selector dialog, and starts the compute process. The Control key
	 * indicates a full recompute of all components (recomputeAll).
	 *
	 * @param e the action event initiating the compute request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Detect whether the Control key is pressed to request full recompute behavior
		boolean recomputeAll = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;

		// Retrieve the active simulation group from the calibration panel
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();

		// Require a selected simulation group before computing
		if (simGroup == null) {
			// Inform the user that a simulation group is needed
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Gather simulations currently selected in the actions window
		List<WatSimulation> sims = _parent.getSelectedSimulations();

		// Prompt if none are selected
		if (sims.isEmpty()) {
			JOptionPane.showMessageDialog(_parent, "Please select the simulations that you want to compute",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Prepare a list of computable tasks for the compute selector dialog
		List<UsbrComputable> computables = new ArrayList<>();

		// Iterator for the selected simulations
		Iterator<WatSimulation> iter = sims.iterator();

		// Per-simulation computable object built from settings
		ActionComputable computable;

		// Loop locals for current simulation and its settings
		WatSimulation sim;
		IterationSettings iterSettings;
		PositionAnalysisSettings posAnalysisSettings;

		// Create the compute selector dialog; use the browser frame as parent
		UsgsComputeSelectorDialog computeDlg = new UsgsComputeSelectorDialog(Browser.getBrowserFrame(), WatSimulation.class);

		// Build computables from the selected simulations
		while (iter.hasNext()) {
			// Current simulation
			sim = iter.next();

			// Fetch iteration settings specific to this simulation from the group
			iterSettings = simGroup.getIterationSettings(sim.getName());

			// Fetch position-analysis settings specific to this simulation from the group
			posAnalysisSettings = simGroup.getPositionAnalysisSettings(sim.getName());

			// Build an action computable with simulation and settings, using the group's compute type
			computable = new ActionComputable(sim, iterSettings, posAnalysisSettings, simGroup.getComputeType(sim.getName()));

			// Provide the compute selector dialog for progress updates
			computable.setProgressDialog(computeDlg);

			// Add to the list that the dialog will execute
			computables.add(computable);
		}

		// Configure the dialog to recompute all components when requested
		computeDlg.setRecomputeAll(recomputeAll);

		// Supply the selected computables to the dialog
		computeDlg.setSelectedComputables(computables);

		// Do not auto-select out-of-date items; rely on explicit selection
		computeDlg.setSelectOutOfDate(false);

		// Begin computation immediately when the dialog opens
		computeDlg.setComputeOnOpen(true);

		// Display the dialog to initiate compute
		computeDlg.setVisible(true);

		// After compute, update UI states (e.g., enable/disable actions based on results)
		_parentPanel.updateComputeStates();
	}
}