package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;                                                              // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.ArrayList;                                                                     // Resizable lists and related collection utilities
import java.util.HashMap;                                                                       // Map implementation used to store keyed collections (import present even if unused here)
import java.util.Iterator;                                                                      // Iterator used to traverse collections
import java.util.List;                                                                          // Collections interface used for lists of simulations or ensemble sets
import java.util.Map;                                                                           // Map interface (import present even if unused here)
import java.util.Set;                                                                           // Set interface (import present even if unused here)
import java.util.StringTokenizer;                                                               // Tokenizer for parsing comma- and dash-separated numeric ranges

import javax.swing.AbstractAction;                                                              // Swing base class for encapsulating an action that can be attached to UI components
import javax.swing.JOptionPane;                                                                 // Swing utility for showing information dialogs

import com.rma.client.Browser;                                                                  // Host application's main browser frame used to parent dialogs
import com.rma.editors.ComputeProgressDialog;                                                   // Dialog to monitor compute progress for forecast computations
import rma.util.IntArray;                                                                       // Utility for dynamically sized arrays of primitive integers

import hec2.wat.model.WatSimulation;                                                            // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.ActionsWindow;                                              // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;         // Dialog that lets users select computations to run and monitors progress (import present even if unused here)
import usbr.wat.plugins.actionpanel.model.UsbrComputable;                                       // Interface for computable tasks consumed by forecast compute workflows (import present even if unused here)
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;                                 // Forecast model representing a collection of ensemble members grouped for computation
import usbr.wat.plugins.actionpanel.model.forecast.ForecastActionComputable;                    // Computable wrapper used to initiate forecast calculations from actions
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                            // Forecast-specific simulation group type used by the forecast panel
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;                                // Panel for forecast workflows that displays and manages simulations and ensemble sets

/**
 * Action that runs forecast computations for the selected simulation and ensemble sets.
 *
 * Detects whether a full recompute is requested using the Control key,
 * validates required selections, constructs a {@code ForecastActionComputable},
 * shows a progress dialog, and triggers the computation. After completion,
 * the parent panel updates its compute states.
 */
public class RunForecastSimulationAction extends AbstractAction {
	/**
	 * Owning actions window used as the parent for dialogs and to access selections.
	 */
	private final ActionsWindow _parent;

	/**
	 * Forecast simulation panel that provides selection context and receives UI updates.
	 */
	private final SimulationPanel _parentPanel;

	/**
	 * Progress dialog that displays compute status for forecast actions.
	 */
	private ComputeProgressDialog _computeDialog;

	/**
	 * Creates the run-forecast-simulation action with a user-visible name and initial disabled state.
	 *
	 * @param parent      the actions window used as the dialog parent and context source
	 * @param parentPanel the forecast simulation panel that triggers updates after compute
	 */
	public RunForecastSimulationAction(ActionsWindow parent, SimulationPanel parentPanel) {
		// Initialize the action with its display label
		super("Run Simulation");

		// Start disabled until the UI logic enables it (for example, when a simulation and ensemble sets are selected)
		setEnabled(false);

		// Store references to the owning window and parent panel
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to compute the selected forecast simulation and ensemble sets.
	 *
	 * The Control key indicates a full recompute of all components (recomputeAll).
	 *
	 * @param e the action event initiating the compute request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Detect whether Control key is pressed to request full recompute behavior
		boolean recomputeAll = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;

		// Delegate to the compute workflow with the recompute flag
		computeSimulationAction(recomputeAll);
	}

	/**
	 * Performs the forecast computation for the selected simulation and ensemble sets.
	 *
	 * Validates that a forecast simulation group exists, a simulation is selected,
	 * and ensemble sets are chosen. Builds a {@link ForecastActionComputable} and
	 * displays a {@link ComputeProgressDialog}. After compute begins, the parent
	 * panel updates its compute states.
	 *
	 * @param recomputeAll true to recompute all components, false to perform standard compute
	 */
	public void computeSimulationAction(boolean recomputeAll) {

		// Retrieve the active forecast simulation group from the forecast panel
		ForecastSimGroup simGroup = _parent.getForecastPanel().getSimulationGroup();

		// Require a selected simulation group before computing
		if (simGroup == null) {
			// Inform the user that a simulation group is needed
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;

		}

		// Retrieve the currently selected forecast simulation
		WatSimulation sim = _parentPanel.getSelectedSimulation();

		// Prompt if no simulation is selected
		if (sim == null) {
			JOptionPane.showMessageDialog(_parent, "Please select the simulation that you want to compute",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Get the ensemble sets chosen in the UI
		List<EnsembleSet> selectedESets = _parentPanel.getSelectedEnsembleSets();

		// Prompt if no ensemble sets are selected
		if (selectedESets.isEmpty()) {
			JOptionPane.showMessageDialog(_parent, "Please select the Ensemble Sets that you want to compute",
					"No Ensemble Sets Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Combine keyboard request with panel setting to determine whether to recompute all
		recomputeAll = recomputeAll || _parentPanel.shouldRecomputeAll();


		// Build the forecast computable with the selected simulation and ensemble sets
		ForecastActionComputable computable = new ForecastActionComputable(simGroup, sim, selectedESets, recomputeAll);

		// Create a progress dialog and connect it to the computable
		_computeDialog = new ComputeProgressDialog(Browser.getBrowserFrame(), computable);
		computable.setComputeDialog(_computeDialog);

		// Show the dialog to begin computation
		_computeDialog.setVisible(true);

		// After compute starts, update UI states (e.g., enable/disable actions based on results)
		_parentPanel.updateComputeStates();
	}

	/**
	 * Parses a string of integers and ranges into an array of unique integers.
	 *
	 * Supported format:
	 * - Comma-separated values (e.g., "1,2,3")
	 * - Dash ranges (e.g., "5-8")
	 * - Combination of both (e.g., "1,3-5,7")
	 * Values are de-duplicated. Non-parsable tokens are ignored.
	 *
	 * @param txt the input string containing integer values and/or ranges
	 * @return an array of parsed integers, or null when the input is empty
	 */
	public static int[] getIntegerSet(String txt) {
		// Collect unique integer values
		IntArray values = new IntArray();

		// Return null for empty input
		if (txt.isEmpty()) {
			return null;
		} else {
			// Tokenize on commas
			StringTokenizer tokenizer = new StringTokenizer(txt, ",");

			// Iterate over tokens until none remain
			for (String word = tokenizer.nextToken().trim(); word != null && !word.isEmpty(); word = tokenizer.nextToken().trim()) {
				// Handle range syntax "start-end"
				if (word.contains("-")) {
					StringTokenizer tokenizer2 = new StringTokenizer(word, "-");

					String word2 = tokenizer2.nextToken().trim();

					// Parse start of range
					if (isParsableToInt(word2)) {
						int startRangeValue = Integer.parseInt(word2);

						// Parse end of range
						word2 = tokenizer2.nextToken().trim();

						if (isParsableToInt(word2)) {
							int endRangeValue = Integer.parseInt(word2);

							// Add all values in the inclusive range
							for (int i = startRangeValue; i <= endRangeValue; ++i) {
								if (!values.contains(i)) {
									values.add(i);
								}
							}
						}
					}
				}

				// Handle single integer values
				if (isParsableToInt(word)) {
					int value = Integer.parseInt(word);

					// Add when not already present
					if (!values.contains(value)) {
						values.add(value);
					}
				}

				// If no more tokens remain, return the collected values
				if (!tokenizer.hasMoreTokens()) {
					return values.toArray();
				}
			}

			// Return collected values when loop completes
			return values.toArray();
		}
	}

	/**
	 * Checks whether the provided string can be parsed as an integer.
	 *
	 * @param i the string to test
	 * @return true if parseable as an integer, false otherwise
	 */
	public static boolean isParsableToInt(String i) {
		try {
			// Attempt to parse as integer
			Integer.parseInt(i);

			return true;
		} catch (NumberFormatException var3) {
			// Not an integer
			return false;
		}
	}
}