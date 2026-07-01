package usbr.wat.plugins.actionpanel.actions;

import java.awt.Cursor;                                                         // Event type delivered when a user triggers a bound action (for example, a button press)
import java.awt.event.ActionEvent;                                              // Event type used by Swing to signal action invocations

import java.io.File;                                                            // File object representing filesystem paths and directories
import java.io.FilenameFilter;                                                  // Filename filter interface used to accept or reject files during listing

import java.util.List;                                                          // Collections interface used for lists of simulations

import javax.swing.AbstractAction;                                              // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                                 // Swing utility for showing information dialogs to the user
import javax.swing.UIManager;                                                   // Swing manager for UI defaults; used to set progress monitor text

import com.rma.io.CopyDirProgressCallbackImpl;                                  // Callback implementation that drives directory copy progress and UI updates
import com.rma.io.CopyListener;                                                 // Listener interface notified upon completion of directory copy operations
import com.rma.io.FileManagerImpl;                                              // File manager implementation that provides filesystem operations (existence checks, create directory, delete)

import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run

import rma.util.RMAIO;                                                          // RMA I/O utility helpers for path operations and safe concatenation

import usbr.wat.plugins.actionpanel.ActionsWindow;                              // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.model.ResultsData;                          // Data model representing a single results entry, including its folder and metadata
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                      // Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.ui.ResultsDataDialog;                       // Dialog used to capture metadata about the results being saved (name, description, etc.)
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                               // Base USBR panel type implemented by calibration and forecast panels
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;            // Tree-table node representing a simulation in the UI; used to add or remove results folders

/**
 * Action that saves the current simulation's computed results into a dedicated results folder
 * and registers those results in the UI tree for easy retrieval.
 *
 * Workflow:
 * - Validates that a simulation group and simulations are selected
 * - Prompts the user for results metadata
 * - Creates the destination results folder under the run directory
 * - Copies the run directory contents (excluding the results folder itself) into the destination
 * - Persists metadata and updates the UI tree upon completion
 */
public class SaveSimulationResultsAction extends AbstractAction
		implements CopyListener {

	/**
	 * Parent folder name under the simulation run directory where saved results are stored.
	 */
	public static final String RESULTS_DIR = ".saveResults";

	/**
	 * Owning actions window used as the UI parent and context source.
	 */
	private ActionsWindow _parent;

	/**
	 * The simulation currently being processed for saving.
	 */
	private WatSimulation _currentSim;

	/**
	 * Destination folder for the currently saved results.
	 */
	private String _currentResultsDir;

	/**
	 * Metadata describing the currently saved results.
	 */
	private ResultsData _currentResultsData;

	/**
	 * Panel used to update the simulation tree after saving results.
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Creates the save-results action with a user-visible name and initial disabled state.
	 *
	 * @param parent      the actions window used as the dialog parent and context source
	 * @param parentPanel the workflow panel (calibration or forecast) that hosts the simulation tree
	 */
	public SaveSimulationResultsAction(ActionsWindow parent, UsbrPanel parentPanel) {
		// Set the action label used by Swing components
		super("Save Results");

		// Start disabled until the UI logic enables it (e.g., when a simulation is selected)
		setEnabled(false);

		// Store references for later use
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to save results for the selected simulations.
	 *
	 * Delegates to {@link #saveSimulationResults()} to perform validations and saving.
	 *
	 * @param e the action event that initiated the request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Begin the results saving workflow
		saveSimulationResults();
	}

	/**
	 * Validates selections and orchestrates saving results for each selected simulation.
	 *
	 * Requires a selected simulation group and one or more selected simulations; otherwise,
	 * shows an informative message to the user.
	 */
	public void saveSimulationResults() {
		// Retrieve the active simulation group from the calibration panel
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();

		// Require a selected simulation group
		if (simGroup == null) {
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Gather simulations currently selected in the actions window
		List<WatSimulation> sims = _parent.getSelectedSimulations();

		// Prompt if none are selected
		if (sims.isEmpty()) {
			JOptionPane.showMessageDialog(_parent, "Please select the simulations that you want to save results for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		// Save results for each selected simulation
		for (int i = 0; i < sims.size(); i++) {
			saveResults(sims.get(i));
		}
	}

	/**
	 * Saves results for a single simulation by copying its run directory into a results folder.
	 *
	 * Prompts for results metadata, creates a destination folder, and initiates a copy operation
	 * that excludes the results parent directory itself. After copy completion, the {@link #copyFinished(int)}
	 * callback persists metadata and updates the UI tree.
	 *
	 * @param sim the simulation whose results should be saved
	 */
	public void saveResults(WatSimulation sim) {
		// Ignore null inputs
		if (sim == null) {
			return;
		}

		// Show a wait cursor while the operation proceeds
		_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Track the original progress monitor text so it can be restored if needed
		String origTitle = UIManager.getString("ProgressMonitor.progressText");

		if (origTitle == null) {
			origTitle = "Progress";
		}

		try {
			// Set a user-friendly progress title for the copy operation
			UIManager.put("ProgressMonitor.progressText", "Saving Results");

			// Performance start timestamp
			long t1 = System.currentTimeMillis();

			System.out.println("saveResults:start...");

			// Track the simulation currently being saved
			_currentSim = sim;

			// The source run directory to copy from
			String runDir = sim.getRunDirectory();

			// Ensure the run directory exists
			if (!FileManagerImpl.getFileManager().fileExists(runDir)) {
				JOptionPane.showMessageDialog(_parent, "No Results found for " + sim.getName(), "No Results", JOptionPane.INFORMATION_MESSAGE);

				return;
			}

			// Parent directory under the run directory where results are saved
			String resultsParentDir = RMAIO.concatPath(runDir, RESULTS_DIR);

			// Create the parent results directory if necessary
			if (!FileManagerImpl.getFileManager().fileExists(resultsParentDir)) {
				FileManagerImpl.getFileManager().createDirectory(resultsParentDir);
			}

			// Prompt the user for results metadata
			ResultsData resultsData = getResultsData(sim);

			// Abort if the dialog was canceled
			if (resultsData == null) {
				return;
			}

			// Destination folder for this specific saved results entry
			String resultsDir = RMAIO.concatPath(resultsParentDir, RMAIO.userNameToFileName(resultsData.getName()));

			// Create the destination folder
			FileManagerImpl.getFileManager().createDirectory(resultsDir);

			// Exclude the results parent directory while copying from the run directory
			SimpleFileFilter excludeFilter = new SimpleFileFilter(RESULTS_DIR);

			// Track the destination folder for use in the copy-finished callback
			_currentResultsDir = resultsDir;

			// Performance mid timestamp
			long t2 = System.currentTimeMillis();
			System.out.println("saveResults:alling copy handler after " + (t2 - t1) + " ms");

			// Start the directory copy with progress callbacks; this will invoke copyFinished upon completion
			CopyDirProgressCallbackImpl callback = new CopyDirProgressCallbackImpl(10, runDir,
					resultsDir, "Copying Results", "Copying Results for " + resultsData.getName(), excludeFilter, this);

			// Track metadata so it can be written after copy completes
			_currentResultsData = resultsData;

		} finally {
			// Restore the default cursor when leaving the method
			// UIManager.put("ProgressMonitor.progressText", origTitle);
			_parent.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Prompts the user for results metadata using a dialog.
	 *
	 * @param sim the simulation providing context for the dialog
	 * @return a {@link ResultsData} instance when confirmed; {@code null} when canceled
	 */
	private ResultsData getResultsData(WatSimulation sim) {

		// Construct the dialog to collect results metadata
		ResultsDataDialog dlg = new ResultsDataDialog(_parent, sim);

		// Display the dialog to the user
		dlg.setVisible(true);

		// Abort when the user cancels
		if (dlg.isCanceled()) {
			return null;
		}

		// Return the captured results data
		return dlg.getResultsData();
	}

	/**
	 * Callback invoked when the directory copy operation finishes.
	 *
	 * Persists results metadata into the destination folder, updates the simulation tree
	 * to include the new results entry, and clears transient state references.
	 *
	 * @param totalCopied the total number of files/directories copied (for informational purposes)
	 */
	@Override
	public void copyFinished(int totalCopied) {
		// Persist metadata into the saved results folder
		_currentResultsData.saveDataToFolder(_currentResultsDir);

		// Update the simulation tree to show the newly saved results
		SimulationTreeTableNode node = _parentPanel.getSimulationTreeTable().getSimulationNodeFor(_currentSim);

		node.addResultsFolder(_currentResultsDir);

		// Clear transient state after completion
		_currentSim = null;
		_currentResultsDir = null;
		_currentResultsData = null;
	}

	/**
	 * Computes the expected results folder path for a simulation and a given results name.
	 *
	 * @param sim  the simulation whose run directory hosts the results
	 * @param name the display name of the results (used to derive a filesystem-safe folder name)
	 * @return absolute path to the results folder under the simulation's run directory
	 */
	public static String getResultsFolder(WatSimulation sim, String name) {
		// Source run directory
		String runDir = sim.getRunDirectory();

		// Parent results directory under the run directory
		String resultsParentDir = RMAIO.concatPath(runDir, RESULTS_DIR);

		// Destination folder for this named results entry
		String resultsDir = RMAIO.concatPath(resultsParentDir, RMAIO.userNameToFileName(name));

		return resultsDir;
	}

	/**
	 * Filename filter that accepts only a specific name (case-insensitive).
	 *
	 * Used to exclude a folder (for example, the results parent directory) during copy operations.
	 *
	 */
	public class SimpleFileFilter implements FilenameFilter {
		/**
		 * Name that this filter matches exactly (case-insensitive).
		 */
		private String _name;

		/**
		 * Constructs a filter that accepts only the given name.
		 *
		 * @param name the name to match
		 */
		public SimpleFileFilter(String name) {
			super();

			_name = name;
		}

		/**
		 * Accepts the file when its name exactly matches the configured name (case-insensitive).
		 *
		 * @param dir  the directory being listed
		 * @param name the filename in that directory
		 * @return true when the name matches; false otherwise
		 */
		@Override
		public boolean accept(File dir, String name) {
			return _name.equalsIgnoreCase(name);
		}
	}
}