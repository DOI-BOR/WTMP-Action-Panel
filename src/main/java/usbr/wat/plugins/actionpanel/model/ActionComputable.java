package usbr.wat.plugins.actionpanel.model;

import java.io.BufferedReader; // Import BufferedReader for reading script file contents line by line
import java.io.File; // Import File class for accessing file system paths and absolute path manipulation
import java.io.IOException; // Import IOException to handle errors during file I/O operations
import java.util.ArrayList; // Import ArrayList for creating dynamic lists of data locations or identifiers
import java.util.HashMap; // Import HashMap for mapping model alternatives to compiled Python code objects
import java.util.List; // Import List interface for collections of data locations, paths, and settings
import java.util.Map; // Import Map interface for storing pre/post scripts keyed by model alternative
import java.util.StringTokenizer; // Import StringTokenizer for parsing Java classpath strings into components
import java.util.Vector; // Import Vector for legacy list storage when concurrent modification safety is not required
import java.util.logging.Logger; // Import Logger for capturing and logging warnings or exceptions during computation

import javax.swing.JOptionPane; // Import JOptionPane for displaying user dialogs during debug mode

import org.python.core.Py; // Import Py utilities for converting Java objects to/from Python objects
import org.python.core.PyCode; // Import PyCode for storing compiled bytecode from Python scripts
import org.python.core.PyException; // Import PyException to catch Python interpreter runtime errors
import org.python.core.PyObject; // Import PyObject as the base class for generic Python objects
import org.python.core.PyString; // Import PyString for creating string objects within the Python interpreter
import org.python.core.PyStringMap; // Import PyStringMap for mapping strings to values in the Python environment
import org.python.core.PySystemState; // Import PySystemState for managing system-level packages in Jython
import org.python.util.PythonInterpreter; // Import PythonInterpreter for initializing and executing Jython scripts

import com.rma.client.Browser; // Import Browser model to access the application's browser frame for dialogs
import com.rma.io.DssFileManagerImpl; // Import DSS file manager implementation for reading/writing DSS data files
import com.rma.io.FileManagerImpl; // Import generic file manager implementation for file path operations
import com.rma.io.RmaFile; // Import RMA File wrapper for representing file paths in the application
import com.rma.model.Computable; // Import Computable interface defining compute logic requirements
import com.rma.model.ComputeProgressListener; // Import listener interface for receiving computation progress updates
import com.rma.model.ComputeProgressListener2; // Import enhanced listener interface for advanced progress management
import com.rma.model.Project; // Import Project model to access current project file and path context
import com.rma.ui.ComputeProgressPanel; // Import UI panel component for displaying compute status

import hec.heclib.dss.DSSPathname; // Import DSS pathname class for managing time series record paths
import hec.heclib.dss.HecDSSFileDataManager; // Import HEC utility to manage and close open DSS files
import hec.heclib.dss.HecDSSUtilities; // Import DSS utilities for cross-file record operations
import hec.heclib.dss.HecDataManager; // Import manager class for writing DSS data objects
import hec.heclib.util.HecTime; // Import HEC Time class for handling simulation time windows and date manipulation
import hec.hecmath.HecMathException; // Import exception class for mathematical operations within DSS tools
import hec.hecmath.TimeSeriesMath; // Import math utility container for shifting or manipulating time series data
import hec.io.DSSIdentifier; // Import identifier class for specifying DSS file and path names
import hec.io.TimeSeriesContainer; // Import container class holding the actual time series data points

import hec2.model.DataLocation; // Import model alternative data location setting definition
import hec2.model.DssDataLocation; // Import specific implementation for DataLocations associated with DSS files
import hec2.plugin.model.ComputeOptions; // Import options model for configuring how a simulation runs
import hec2.plugin.model.ModelAlternative; // Import model alternative to access program and file path settings
import hec2.wat.model.WatSimulation; // Import WAT simulation base class representing a run configuration
import hec2.wat.plugin.SimpleWatPlugin; // Import simple plugin implementation used for retrieving default directories
import hec2.wat.plugin.WatPlugin; // Import generic plugin interface for model program type identification
import hec2.wat.plugin.WatPluginManager; // Import manager class to retrieve plugin instances by program name

import rma.util.RMAIO; // Import utility class for file path concatenation and directory extraction operations

import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog; // Import dialog component for managing computation progress display


/**
 * ActionComputable is a model object that wraps the computation process for a WatSimulation, enabling both standard
 * runs and advanced iterative/position analysis workflows. It supports execution of Python scripts (pre-compute/post-compute)
 * via Jython interpreter to automate data adjustments or results processing before and after simulation runs.
 *
 * Key responsibilities include:
 *
 *   Managing temporary DSS file paths during computation (saving original, renaming).
 *   Initializing the Python Interpreter for script execution.
 *   Copying input data and output results to/from collection DSS files.
 *
 */

public class ActionComputable implements UsbrComputable {
	// Constant defining the suffix added to DSS file parts to mark saved original data paths during iterative runs
	private static final String SAVE_SUFFEX = "-save"; // Suffix appended to FPart to distinguish saved originals from active data

	// Name constant for the iteration results file where intermediate data is stored during ensemble or sensitivity analysis
	public static final String ITERATION_DSS_FILE = "iterationResults.dss"; // Constant defining filename for storing iteration outputs

	// Description text displayed in report XML indicating which DSS file contains simulation data
	private static final String DSSFILE = "DSS File"; // Text label used within generated XML reports

	// Method signature string expected in the Python script function to ensure compatibility
	public static final String METHOD_SIGNATURE = "runIteration(modelAlternative, currentIteration, maxIteration)"; // Expected method name for Jython scripts

	// Field representing the underlying WAT simulation model that this action object represents
	private WatSimulation _sim; // The simulation instance that is being computed or analyzed

	// Settings defining members to be included in iterative computation analysis
	private IterationSettings _iterSettings; // Configuration for iterative sensitivity settings

	// Settings defining members for position analysis (boundary condition override)
	private PositionAnalysisSettings _posAnalysisSettings; // Configuration for position analysis specific settings

	// Path string stored temporarily to identify where iteration results are written to disk
	private String _iterDssFile; // Target filename/path for storing iteration output data

	// Python interpreter instance used to execute pre- and post-compute scripts
	private PythonInterpreter _interp; // Jython Interpreter instance for executing script code

	// Debug flag to enable verbose logging and user interface messages during computation
	private boolean _debug; // Boolean flag to activate debugging mode output

	// Map storing compiled Python code objects keyed by model alternative for pre-compute phase
	private transient Map<ModelAlternative, PyCode> _preCodeMap = new HashMap<>(); // Cache of pre-scripts compiled bytecode per model

	// Map storing compiled Python code objects keyed by model alternative for post-compute phase
	private transient Map<ModelAlternative, PyCode> _postCodeMap = new HashMap<>(); // Cache of post-scripts compiled bytecode per model

	// Text buffer holding the current script source code being executed or debugged
	private String _currentScriptText; // Source text string of currently running Python script

	// UI component for displaying progress bar and messages to user during long-running computes
	private UsgsComputeSelectorDialog _computeDialog; // Dialog object managing computation visualization

	// Flag indicating whether the computation process was interrupted by the user or an error condition
	private boolean _canceled; // Boolean flag indicating compute cancellation status

	// Enum specifying which type of computation workflow is currently active (Standard, Iterative, Position Analysis)
	private ComputeType _computeType; // Type of computation mode to use for run() invocation

	/**
	 * Constructor for ActionComputable that initializes the simulation wrapper and settings.
	 * Creates new HashMaps for caching compiled Python scripts and initializes default values for state flags.
	 *
	 * @param sim                 The WatSimulation model instance to be computed
	 * @param iterSettings        Settings controlling iterative computation members and paths
	 * @param posAnalysisSettings Settings controlling position analysis override behavior
	 */
	public ActionComputable(WatSimulation sim, IterationSettings iterSettings, PositionAnalysisSettings posAnalysisSettings, ComputeType computeType) {
		super(); // Invoke superclass default constructor

		_sim = sim; // Assign simulation model reference instance variable
		_iterSettings = iterSettings; // Save iteration configuration settings object
		_posAnalysisSettings = posAnalysisSettings; // Save position analysis configuration settings object
		_computeType = computeType; // Save the selected computation type (Standard/Iteration/Position)
	}

	/**
	 * Executes the wrapped computation by delegating to the overloaded compute() method.
	 * This is a thin wrapper around the main computational logic for compatibility with Computable interface.
	 */
	@Override
	public void run() {
		compute(); // Call the primary compute logic entry point
	}

	/**
	 * Checks if this computation object can be computed based on the underlying simulation state.
	 *
	 * @return True if the simulation is currently in a valid state for computation, false otherwise
	 */
	@Override
	public boolean isComputable() {
		return _sim.isComputable(); // Delegate check to wrapped simulation object status
	}

	/**
	 * Registers a listener to receive progress updates during the computation process.
	 * Delegates the operation to the underlying simulation's listener management system.
	 *
	 * @param listener The compute progress listener to be added for callback notifications
	 */
	@Override
	public void addComputeListener(ComputeProgressListener listener) {
		_sim.addComputeListener(listener); // Add listener to simulation object event handler
	}

	/**
	 * Removes a registered compute progress listener from the simulation's event handlers.
	 * Used after completion or cancellation to prevent duplicate notification processing.
	 *
	 * @param listener The compute progress listener to be removed from active listeners
	 */
	@Override
	public void removeComputeProgressListener(ComputeProgressListener listener) {
		_sim.removeComputeProgressListener(listener);
	}

	/**
	 * Executes the configured computation logic based on the selected ComputeType.
	 * Branches execution flow into three modes: Standard (direct run), Iterative, or Position Analysis.
	 * Returns true if compute completed successfully, false if errors occurred or cancellation flagged.
	 *
	 * @return True if computation finished without fatal error, False otherwise
	 */
	@Override
	public boolean compute() {
		// Check if workflow mode is iterative ensemble calculation
		if (_computeType == ComputeType.Iterative) {
			return iterativeCompute(); // Delegate to iterative compute logic method

		} else if (_computeType == ComputeType.PositionAnalysis) {
			// Check if workflow mode is position analysis
			return positionAnalysisCompute(); // Delegate to position analysis compute logic method

		} else {
			// Handle default case which corresponds to Standard compute mode
			// Set simulation to recompute all data as configured in the dialog UI
			_sim.setRecomputeAll(_computeDialog.shouldRecomputeAll()); // Trigger full recalculation based on user preference

			return _sim.compute(); // Delegate directly to simulation's built-in computation method
		}
	}


	/**
	 * Executes the position analysis workflow which involves saving original DSS data,
	 * copying new data members for each ensemble member into the time window, running compute,
	 * and restoring original data. This mode is typically used for boundary condition override scenarios.
	 *
	 * @return True if all members processed successfully, false if error occurred or cancelled
	 */
	private boolean positionAnalysisCompute() {
		_debug = Boolean.getBoolean("ActionComputable.debugCompute"); // Enable debug output if system property set

		int[] members = _posAnalysisSettings.getMembersToCompute(); // Retrieve list of iteration member indices

		// Validate that at least one member is selected for processing
		if (members == null || members.length == 0) {
			_sim.addErrorMessage("No Iteration Members selected to compute"); // Log error message if no members defined
			_sim.computeComplete(false); // Signal completion with failure status
			return false; // Return failure indicator
		}

		// Save off the original DSS data before overwriting them with new iteration inputs
		if (_debug) {
			// Display progress dialog only in debug mode
			JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Saving Original Data "); // Show message to user
		}

		List<DSSIdentifier> savedDssPaths = saveDssPaths(_posAnalysisSettings); // Store paths of original DSS records to restore later

		// If saving original data failed (returned null)
		if (savedDssPaths == null) {
			return false; // Fail the entire compute process if originals cannot be backed up
		}

		_preCodeMap.clear(); // Clear any cached pre-scripts as we are in a new compute session

		_postCodeMap.clear(); // Clear any cached post-scripts to prevent conflicts

		ComputeProgressListener progressListener = _sim.getComputeProgressListener(); // Get primary listener reference

		List<ComputeProgressListener> listeners = null; // Initialize list to hold secondary listeners if they exist

		// Check for enhanced listener interface capability
		if (progressListener instanceof ComputeProgressListener2) {
			ComputeProgressListener2 pl2 = (ComputeProgressListener2) progressListener; // Safe cast to enhanced listener
			listeners = pl2.getListeners(); // Extract secondary listeners list
		}

		// Attempt main processing within try-catch block for error isolation
		try {
			int currentMember; // Variable to track current iteration member index

			// Loop through each selected ensemble member
			for (int m = 0; m < members.length; m++) {
				currentMember = members[m]; // Get current member index

				_sim.addComputeMessage("Computing Iteration Member " + currentMember); // Log status for this member
				System.out.println("Computing Iteration Member " + currentMember + " for " + _sim); // Print to console for debugging

				// Check cancellation flag between steps
				if (_canceled) {
					return false; // Return failure if user cancelled the process
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in data for Ensemble member " + currentMember); // Update UI status
				}

				// Copy in the new iteration data by overwriting original paths with computed alternatives
				if (!copyDssMembersForTimeWindow(currentMember, _posAnalysisSettings)) {
					// Execute copy logic for time windows
					return false; // Fail process if copying input data failed
				}

				// Check cancellation flag after copy operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// Close any DSS files we might have had open to prevent file handle leaks during switch
				HecDSSFileDataManager dm = new HecDSSFileDataManager(); // Create manager instance for cleanup
				dm.closeAllFiles(); // Ensure all previous DSS files are closed cleanly

				_sim.setRecomputeAll(true); // Set flag on simulation object to force full recalculation

				// Compute the simulation with the new copied data now in place
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Computing Ensemble member " + currentMember); // Update UI status
				}

				// Run the actual simulation compute operation
				// Check if simulation engine reported success
				if (!_sim.compute()) {
					// If configured to continue on error, skip this iteration
					if (Boolean.getBoolean("ActionComputable.ContinueOnError")) {
						continue; // Proceed to next member without logging specific failure
					}

					return false; // Return failure if configuration is to stop on error
				}

				// Copy the output from the simulation dss file to the iteration dss file
				// Check cancellation flag after compute operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// If secondary listeners were retrieved earlier, re-attach them
				if (listeners != null) {
					// Listeners got removed by the sim at the end of its compute, so put them back
					// Iterate through listener list to restore connections
					for (int l = 0; l < listeners.size(); l++) {
						_sim.addComputeListener(listeners.get(l)); // Re-add listener to simulation event handler

						// Check if listener is a progress panel UI component
						if (listeners.get(l) instanceof ComputeProgressPanel) {
							((ComputeProgressPanel) listeners.get(l)).setModelPosition(0); // Reset position for progress UI updates

							// Clear message text from UI components if available to avoid stale messages

						}
					}
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying results for ensemble member " + currentMember); // Update UI status
				}

				// Copy the computed output results from simulation file to collection storage
				copyDssResultsToCollectionsDss(currentMember, _posAnalysisSettings); // Call method to save results

				// Check cancellation flag after copy operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Post Scripts ensemble member " + currentMember); // Update UI status
				}
				// Main loop iteration completes

			}

		} catch (Exception e) {
			_sim.addErrorMessage("Exception during iterative compute " + e); // Log error to simulation message queue
			Logger.getLogger(ActionComputable.class.getName()).warning("Exception during iterative compute " + e); // Log warning with class name
			e.printStackTrace(); // Print stack trace for debugging

			return false; // Return failure on exception

		} finally {
			// Ensure cleanup logic executes regardless of success or exception
			// Restore the saved off DSS paths to original locations
			// Display message in debug mode only
			if (_debug) {
				JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Restoring original DSS data"); // Inform user of restoration start
			}

			restoreDssPaths(savedDssPaths); // Call method to restore original backed-up files
			_preCodeMap.clear(); // Clear cached pre-scripts after compute session ends
			_postCodeMap.clear(); // Clear cached post-scripts to free memory and reset state

			// Remove any secondary listeners added during process
			for (int l = 0; l < listeners.size(); l++) {
				_sim.removeComputeProgressListener(progressListener); // Clean up listener references on simulation
			}
		}

		return true; // Return success if all steps completed without fatal errors
	}

	/**
	 * Executes the iterative computation workflow which involves running pre-scripts, copying input data for each member,
	 * computing the simulation, and running post-scripts. Used for ensemble generation and sensitivity analysis scenarios.
	 *
	 * @return True if all members processed successfully, false if error occurred or cancelled
	 */
	private boolean iterativeCompute() {
		_debug = Boolean.getBoolean("ActionComputable.debugCompute"); // Enable debug output if system property set

		int[] members = _iterSettings.getMembersToCompute(); // Retrieve list of iteration member indices

		// Validate that at least one member is selected for processing
		if (members == null || members.length == 0) {
			_sim.addErrorMessage("No Iteration Members selected to compute"); // Log error message if no members defined

			_sim.computeComplete(false); // Signal completion with failure status

			return false; // Return failure indicator
		}

		// Save off the original DSS data before overwriting them with new iteration inputs
		// Display progress dialog only in debug mode
		if (_debug) {
			JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Saving Original Data "); // Show message to user
		}

		// Store paths of original DSS records to restore later
		List<DSSIdentifier> savedDssPaths = saveDssPaths(_iterSettings);

		// If saving original data failed (returned null)
		if (savedDssPaths == null) {
			return false; // Fail the entire compute process if originals cannot be backed up
		}

		_preCodeMap.clear(); // Clear any cached pre-scripts as we are in a new compute session
		_postCodeMap.clear(); // Clear any cached post-scripts to prevent conflicts

		ComputeProgressListener progressListener = _sim.getComputeProgressListener(); // Get primary listener reference
		List<ComputeProgressListener> listeners = null; // Initialize list to hold secondary listeners if they exist

		// Check for enhanced listener interface capability
		if (progressListener instanceof ComputeProgressListener2) {
			ComputeProgressListener2 pl2 = (ComputeProgressListener2) progressListener; // Safe cast to enhanced listener
			listeners = pl2.getListeners(); // Extract secondary listeners list
		}

		// Attempt main processing within try-catch block for error isolation
		try {
			int currentMember; // Variable to track current iteration member index

			// Loop through each selected ensemble member
			for (int m = 0; m < members.length; m++) {
				currentMember = members[m]; // Get current member index

				_sim.addComputeMessage("Computing Iteration Member " + currentMember); // Log status for this member
				System.out.println("Computing Iteration Member " + currentMember + " for " + _sim); // Print to console for debugging

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Pre Scripts for ensemble member " + currentMember); // Update UI status
				}

				// Execute pre-compute scripts to modify inputs before run
				if (!runPreScripts(currentMember)) {
					return false; // Fail process if pre-scripts failed
				}

				// Check cancellation flag between steps
				if (_canceled) {
					return false; // Return failure if user cancelled the process
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in data for Ensemble member " + currentMember); // Update UI status
				}

				// Copy in the new iteration data by overwriting original paths with computed alternatives
				// Execute copy logic for time windows
				if (!copyDssMembers(currentMember, _iterSettings)) {
					return false; // Fail process if copying input data failed
				}

				// Check cancellation flag after copy operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// Close any DSS files we might have had open to prevent file handle leaks during switch
				HecDSSFileDataManager dm = new HecDSSFileDataManager(); // Create manager instance for cleanup
				dm.closeAllFiles(); // Ensure all previous DSS files are closed cleanly

				_sim.setRecomputeAll(true); // Set flag on simulation object to force full recalculation

				// Compute the simulation with the new copied data now in place
				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Computing Ensemble member " + currentMember); // Update UI status
				}

				// Run the actual simulation compute operation
				// Check if simulation engine reported success
				if (!_sim.compute()) {
					// If configured to continue on error, skip this iteration
					if (Boolean.getBoolean("ActionComputable.ContinueOnError")) {
						continue; // Proceed to next member without logging specific failure
					}

					return false; // Return failure if configuration is to stop on error
				}

				// Copy the output from the simulation dss file to the iteration dss file
				// Check cancellation flag after compute operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// If secondary listeners were retrieved earlier, re-attach them
				if (listeners != null) {
					// Listeners got removed by the sim at the end of its compute, so put them back
					// Iterate through listener list to restore connections
					for (int l = 0; l < listeners.size(); l++) {
						_sim.addComputeListener(listeners.get(l)); // Re-add listener to simulation event handler

						// Check if listener is a progress panel UI component
						if (listeners.get(l) instanceof ComputeProgressPanel) {
							((ComputeProgressPanel) listeners.get(l)).setModelPosition(0); // Reset position for progress UI updates

							// Clear message text from UI components if available to avoid stale messages

						}
					}
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying results for ensemble member " + currentMember); // Update UI status
				}

				// Copy the computed output results from simulation file to collection storage
				copyDssResultsToCollectionsDss(currentMember, _iterSettings); // Call method to save results

				// Check cancellation flag after copy operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Post Scripts ensemble member " + currentMember); // Update UI status
				}

				// Execute post-compute scripts to process outputs
				if (!runPostScripts(currentMember)) {
					return false; // Fail process if post-scripts failed
				}

				// Main loop iteration completes

			}

			} catch(Exception e ) {
				_sim.addErrorMessage("Exception during iterative compute " + e); // Log error to simulation message queue
				Logger.getLogger(ActionComputable.class.getName()).warning("Exception during iterative compute " + e); // Log warning with class name
				e.printStackTrace(); // Print stack trace for debugging

				return false; // Return failure on exception

			} finally {
				// Ensure cleanup logic executes regardless of success or exception
				// Restore the saved off DSS paths to original locations
				if (_debug) // Display message in debug mode only
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Restoring original DSS data"); // Inform user of restoration start
				}
				restoreDssPaths(savedDssPaths); // Call method to restore original backed-up files

				_preCodeMap.clear(); // Clear cached pre-scripts after compute session ends
				_postCodeMap.clear(); // Clear cached post-scripts to free memory and reset state

				// Remove any secondary listeners added during process
				for (int l = 0; l < listeners.size(); l++) {
					_sim.removeComputeProgressListener(progressListener); // Clean up listener references on simulation
				}
			}

			return true; // Return success if all steps completed without fatal errors
		}


		/**
		 * Executes Python post-compute scripts defined in the configuration for the current iteration member.
		 * Runs after the simulation compute and data copying to allow modification of results.
		 *
		 * @return True if all configured scripts ran successfully, false if any script failed or had error
		 */
		private boolean runPostScripts ( int iterNum) {
			// Retrieve settings container for iteration
			SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();

			// Get configuration specifically for post-processing phase
			ComputeSettings computeSettings = sSettings.getPostComputeSettings();

			// Delegate to general script runner indicating this is NOT pre-compute
			return runScripts(computeSettings, iterNum, false);
		}

		/**
		 * Executes Python pre-compute scripts defined in the configuration for the current iteration member.
		 * Runs before the simulation compute and data copying to allow modification of inputs.
		 *
		 * @return True if all configured scripts ran successfully, false if any script failed or had error
		 */
		private boolean runPreScripts ( int iterNum){
			// Retrieve settings container for iteration
			SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();

			// Get configuration specifically for pre-processing phase
			ComputeSettings computeSettings = sSettings.getPreComputeSettings();

			// Delegate to general script runner indicating this IS pre-compute
			return runScripts(computeSettings, iterNum, true);
		}

		/**
		 * Iterates through all model alternatives and executes configured scripts if available.
		 * Handles compilation of Python scripts on first encounter and execution via Jython interpreter.
		 * Skips null model alternatives or configurations with empty script files.
		 *
		 * @param computeSettings Configuration object defining which scripts run for each model alternative
		 * @param iterNum The iteration/member number currently being processed for context in logs
		 * @param isPreCompute Boolean flag indicating if this is a pre-compute or post-compute phase
		 * @return True if all non-null alternatives executed successfully, false if any failed
		 */
		private boolean runScripts (ComputeSettings computeSettings, int iterNum, boolean isPreCompute) {
			// Retrieve collection of all possible model alternative objects
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();

			// Declare variable to hold current model alternative in loop
			ModelAlternative modelAlt;

			// Loop through alternatives, stop early if cancelled
			for (int i = 0; i < modelAlts.size() && !_canceled; i++) {
				modelAlt = modelAlts.get(i); // Get specific model alternative instance

				// Check for null reference to prevent crashes
				if (modelAlt == null) {
					continue; // Skip iteration if alternative is missing

				}

				// Retrieve the path to script file from settings config
				String scriptFile = computeSettings.getScriptFor(modelAlt);

				// Check if no script is configured for this alternative
				if (scriptFile == null || scriptFile.isEmpty()) {
					continue; // Skip execution as there is nothing to run for this model type

				}

				// Read full content of the script file into memory
				String scriptText = readScriptFile(scriptFile);

				// If reading file failed, return failure immediately
				if (scriptText == null) {
					return false; // Return false indicating script could not be loaded
				}

				// Log information in debug mode only to reduce console noise
				if (_debug) {
					Logger.getLogger(ActionComputable.class.getName()).info("Found Script for " + modelAlt); // Log success finding script path
				}

				// Delegate compilation and execution call
				if (!runScript(modelAlt, scriptText, iterNum, isPreCompute)) {
					return false; // Return failure immediately if this specific script failed to run
				}
			}

			return true; // Return success if loop completed without errors or returns
		}

		/**
		 * Reads the entire content of a script file from disk into a single string.
		 * Opens the file using the RMAFileManager, reads line-by-line into a StringBuilder, and closes resources properly.
		 * Handles IOExceptions by adding error messages to the simulation log.
		 *
		 * @param scriptFile Path string to the script file containing Python code
		 * @return The full text content of the script file, or null if read failed
		 */
		private String readScriptFile (String scriptFile) {
			// Convert relative path to absolute using project context
			String absScriptFile = Project.getCurrentProject().getAbsolutePath(scriptFile);

			// Create RmaFile wrapper for target resource
			RmaFile file = FileManagerImpl.getFileManager().getFile(absScriptFile);

			// Check if file manager failed to create handle
			if (file == null) {
				return null; // Return null if file could not be handled
			}

			BufferedReader reader = file.getBufferedReader(); // Get buffered reader stream for text input
			String line; // Variable to hold individual line of text from file
			StringBuilder buffer = new StringBuilder(); // Create buffer to accumulate full content

			// Attempt reading within try block for resource management
			try {
				// Read each line until EOF
				while ((line = reader.readLine()) != null) {
					buffer.append(line); // Append line content to builder
					buffer.append("\n"); // Add newline character to preserve formatting
				}

				return buffer.toString(); // Return the complete assembled text string

			} catch (IOException ioe) {
				// Catch read errors during I/O operation
				_sim.addErrorMessage("Error reading script file " + absScriptFile + " Error:" + ioe); // Log error to simulation message queue
				Logger.getLogger(ActionComputable.class.getName()).warning("Error reading script file " + absScriptFile + " Error:" + ioe); // Log warning with class name

			} finally {
				// Ensure resources are closed regardless of success or exception
				// Check if reader object exists before closing
				if (reader != null) {
					// Attempt close operation inside try-catch to prevent secondary errors
					try {
						reader.close(); // Close the buffered reader stream

					} catch (IOException e) {
						// Catch specific exception from close() call
						// empty ok // Suppress close error to avoid masking original read error
					}
				}
			}

			return null; // Return null if file content failed to load
		}

		/**
		 * Executes a Python script against the current simulation context.
		 * If this is the first run, compiles the script source into bytecode and caches it in _preCodeMap or _postCodeMap.
		 * Executes compiled code via Jython interpreter and expects boolean return value for success/failure check.
		 *
		 * @param modelAlt The ModelAlternative object whose options are used for this execution context
		 * @param script The source code text of the Python script to execute
		 * @return True if script executed successfully, false on error or compilation failure
		 */
		private boolean runScript (ModelAlternative modelAlt, String script,int iterNum, boolean isPreCompute){
			// Check if Python interpreter instance exists in object state
			if (_interp == null) {
				// If not present, initialize the Jython interpreter environment first
				if (!initInterp()) {
					return false; // Return failure on initialization error
				}
			}

			// Get cached bytecode object based on type and model type
			PyCode code = (isPreCompute ? _preCodeMap.get(modelAlt) : _postCodeMap.get(modelAlt));

			// If no cached bytecode exists, compilation is needed
			if (code == null) {
				_currentScriptText = script; // Store source text for potential error logging
				code = compileCode(script); // Attempt to compile the raw script string into bytecode object

				// Check if compilation returned null (failed)
				if (code == null) {
					Logger.getLogger(ActionComputable.class.getName()).info("Failed to compile " + (isPreCompute ? "precompute" : "postcompute") + "script for " + modelAlt); // Log warning with phase type
					_sim.addErrorMessage("Failed to compile " + (isPreCompute ? "precompute" : "postcompute") + " script for " + modelAlt); // Add user-friendly error message
					return false; // Return failure on compilation error
				}

				// Take action based on the current compute phase
				if (isPreCompute) {
					// Store compiled code in pre-script cache if this phase is pre-compute
					_preCodeMap.put(modelAlt, code); // Save bytecode for future use without recompilation
				} else {
					// Otherwise store in post-script cache
					_postCodeMap.put(modelAlt, code); // Save bytecode for future use without recompilation
				}
			}

			// Log execution phase to simulation message queue
			_sim.addComputeMessage("Running " + (isPreCompute ? "pre-compute" : "post-compute") + " script for " + modelAlt);

			// Delegate execution of compiled code object
			boolean rv = runScript(code, modelAlt, iterNum);

			_currentScriptText = null; // Clear source text buffer after successful execution
			return rv; // Return the boolean result of script execution
		}

		/**
		 * Executes pre-compiled Python bytecode with simulation options set in local variables.
		 * Injects current iteration, max iteration, and model alternative into Python namespace.
		 * Checks return value from 'ret' variable to determine success status if explicitly returned.
		 *
		 * @param code The compiled PyCode object containing the executable script logic
		 * @return True if execution completed without exception or returned true/false explicitly
		 */
		private boolean runScript (PyCode code, ModelAlternative modelAlt,
		int iterNum) {
			long t1 = System.currentTimeMillis(); // Capture start time for performance profiling

			// Code block scope to manage interpreter state and resource cleanup cleanly
			{
				// Log execution in debug mode only
				if (_debug) {
					Logger.getLogger(ActionComputable.class.getName()).info("running Jython Code for " + modelAlt + " iter=" + iterNum); // Log operation details
				}

				hec2.wat.model.ComputeOptions options = _sim.getOptionsForNextCompute(modelAlt, _sim.getRunTimeWindow(), getWatPlugin(modelAlt)); // Get next compute configuration
				modelAlt.setComputeOptions(options); // Apply new options to model alternative state

				PyStringMap locals = new PyStringMap(); // Create dictionary to hold local variables for script execution
				locals.__setitem__("currentIteration", Py.java2py(iterNum)); // Inject Java loop counter into Python namespace
				locals.__setitem__("maxIteration", Py.java2py(_iterSettings.getMaximumMember())); // Inject maximum member count into namespace
				locals.__setitem__("modelAlternative", Py.java2py(modelAlt)); // Inject model alternative object reference into namespace

				_interp.setLocals(locals); // Set local dictionary on interpreter for script access

				// Attempt execution within try block for error handling
				try {
					_interp.exec(code); // Execute compiled bytecode code object

					PyObject outlocals = _interp.getLocals(); // Retrieve variables set during script execution

					// get return value from interpreter // Extract explicit return value if defined in script
					PyObject pyobj = ((PyStringMap) outlocals).__getitem__(new PyString("ret")); // Access 'ret' variable which indicates success status
					Object obj = Py.tojava(pyobj, Boolean.class.getName()); // Convert Python boolean object back to Java Boolean type

					// Check if explicit boolean was returned
					if (obj instanceof Boolean) {
						// Log return value in debug mode only
						if (_debug) {
							Logger.getLogger(ActionComputable.class.getName()).info("returning " + obj + " from script"); // Log the actual boolean result
							_sim.addLogMessage("runScript() returning " + obj); // Add message to simulation log component
						}

						Boolean b = (Boolean) obj; // Cast to standard Java Boolean object

						return b; // Return the boolean value directly
					}
					// If no explicit boolean was returned, assume success by convention
					return true; // Return true if script finished without error or exception

				} catch (Exception e) {
					// Check specifically for Python interpreter errors
					if (e instanceof PyException) {
						PyException pye = (PyException) e; // Cast to specific type for analysis
						pye.normalize(); // Normalize exception message to standardize stack traces
					}

					Logger.getLogger(ActionComputable.class.getName()).info("runScript:Error running initialization script " + e); // Log error details
					Logger.getLogger(ActionComputable.class.getName()).info("runScript:initialization script is:"); // Prepare log for source code display
					Logger.getLogger(ActionComputable.class.getName()).info(_currentScriptText); // Log the source text associated with failure

					_sim.addErrorMessage("Error running script " + getName() + "'s. Error" + e); // Add high-level error message to simulation queue
					_sim.addErrorMessage("Check ComputeLog for details"); // Prompt user to check detailed logs

					_sim.addLogMessage(e.toString()); // Add stack trace string to log component
					_sim.addLogMessage("current script is:"); // Log label for code block
					_sim.addLogMessage("-------------------------------"); // Visual separator in log
					_sim.addLogMessage(_currentScriptText); // Log actual source code text
					_sim.addLogMessage("-------------------------------"); // Closing visual separator

					_currentScriptText = null; // Clear buffer to prevent stale references

					return false; // Return failure on execution error
				} finally {
					// Ensure profiling time calculation happens after try-catch
					// Only log performance stats in debug mode
					if (_debug) {
						_sim.addLogMessage("initializeScript " + getName() + " took:" + (System.currentTimeMillis() - t1) + " ms."); // Log execution duration to simulation log
					}
				}
			}
		}

		/**
		 * Retrieves the WatPlugin manager object for a given model alternative.
		 * Used to configure computation options for specific program types (e.g., ResSim, CE-QUAl-W2).
		 * Performs type checking and safe casting based on the plugin class name.
		 *
		 * @return The casted WatPlugin instance if found, null if plugin not found or incompatible
		 */
		private static WatPlugin getWatPlugin (ModelAlternative modelAlt) {
			String program = modelAlt.getProgram(); // Extract program type identifier string

			SimpleWatPlugin splugin = WatPluginManager.getPlugin(program); // Retrieve plugin object from manager by program name

			// Verify retrieved plugin matches expected generic interface
			if (splugin instanceof WatPlugin) {
				WatPlugin plugin = (WatPlugin) splugin; // Safe cast to generic interface for use in method calls
				return plugin; // Return casted plugin instance
			}

			return null; // Return null if casting or lookup failed
		}

		/**
		 * Compiles a raw Python script string into a bytecode PyCode object.
		 * Appends the expected METHOD_SIGNATURE to ensure compatibility with WAT's compute options framework.
		 * Wraps compilation in try-catch to handle syntax errors or import failures in Jython interpreter.
		 *
		 * @param script The source code text of the Python script to compile
		 * @return Compiled PyCode object ready for execution, null if compilation failed
		 */
		private PyCode compileCode (String script) {
			StringBuilder buffer = new StringBuilder(script); // Create buffer for string manipulation
			buffer.append("ret=" + METHOD_SIGNATURE + "\n"); // Append method signature as default return value
			String updatedScript = buffer.toString(); // Get final compiled script string

			// Attempt compilation within try block
			try {
				// Log in debug mode only
				if (_debug) {
					Logger.getLogger(ActionComputable.class.getName()).info("Compiling script:" + updatedScript); // Log the code being compiled
				}

				PyCode pyCode = (new org.python.util.PythonInterpreter()).compile(updatedScript); // Compile via interpreter API and create new object instance

				return pyCode; // Return the resulting bytecode object to caller for caching or execution
			} catch (Exception e) {
				// Catch compilation exceptions thrown by interpreter
				Logger.getLogger(ActionComputable.class.getName()).warning("Python Compilation Error of Script " + updatedScript + " failed " + e); // Log warning with failure context
				_sim.addErrorMessage("Python Compilation Error of Script failed " + e); // Add high level error message
				_sim.addErrorMessage(" Script is:\n" + updatedScript); // Display script content in error log to help debugging

				return null; // Return null indicating compilation failure
			}
		}

		/**
		 * Initializes the PythonInterpreter instance if one does not exist.
		 * Sets up system paths including Jython libraries and scripts folder.
		 * Adds HEC RSS packages to system state for compatibility with simulation models.
		 *
		 * @return True if initialization succeeded, false if error occurred during setup
		 */
		private boolean initInterp () {
			// Log start of initialization in debug mode only
			if (_debug) {
				Logger.getLogger(ActionComputable.class.getName()).info("initializing Jython Interpreter"); // Log status update
			}

			//------------------------------------------------------//
			// make sure we have a valid application home directory //
			//------------------------------------------------------//
			String appHome = hec.lang.ApplicationProperties.getAppHome(); // Get the application installation root directory path
			if (appHome == null) appHome = "."; // Default to current directory if property is not set

			try { // Attempt absolute path conversion for app home
				appHome = (new File(appHome)).getAbsolutePath(); // Resolve relative paths to absolute system paths

				if (appHome.endsWith(File.separator + ".")) { // Handle trailing dot from file separator
					appHome = appHome.substring(0, appHome.length() - 2); // Remove redundant dot at end of path
				}
			} catch (Exception e) {
			} // Ignore exception if path resolution fails internally


			long t1 = System.currentTimeMillis(); // Capture start time for profiling

			String pythonPath = System.getProperty("python.path"); // Retrieve configured Python libraries path

			// If system property is not already set
			if (pythonPath == null) {
				pythonPath = appHome; // Default to application home directory
				String classpath = System.getProperty("java.class.path"); // Get current Java classpath string

				StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator); // Split classpath by OS separator
				String token = null; // Variable to hold current directory component

				boolean found = false; // Flag to track if Jython lib jar was located in path
				// Loop through each entry in system classpath
				while (tokenizer.hasMoreTokens()) {
					token = tokenizer.nextToken(); // Get next directory string

					// Check if this token contains the Jython library jar
					if (token.indexOf("jythonlib.jar") > -1) {
						found = true; // Mark as found
						Logger.getLogger(ActionComputable.class.getName()).info("found jythonlib.jar in classpath" + token); // Log discovery of required library
						break; // Exit loop once library is located
					}
				}

				// If found jar was identified in path
				if (found) {
					token = token + "/lib"; // Append /lib subdirectory to the directory containing jar

				} else {
					// If jar not found automatically, construct default expected path
					token = appHome + File.separator + "jar" + File.separator + "jythonlib.jar/lib"; // Use standard installation path if auto-detect fails
				}

				if (!pythonPath.endsWith(File.separator)) pythonPath += File.separator; // Ensure path has trailing separator
				pythonPath += "scripts" + File.pathSeparator + token; // Add scripts directory and library jar to search path

				System.setProperty("python.path", pythonPath);
			}

			java.util.Properties props = new java.util.Properties(); // Create properties object for interpreter init
			props.setProperty("python.path", pythonPath); // Store configured path in properties map

			// Initialize interpreter with system, custom props, and default package list
			PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});
			PySystemState sys = Py.getSystemState(); // Get global system state object for Python runtime
			sys.add_package("hec.rss.model"); // Add HEC RSS model packages to available import namespaces

			_interp = new PythonInterpreter(); // Create new instance of interpreter after env setup
			// Log performance stats only if debugging enabled
			if (_debug) {
				Logger.getLogger(ActionComputable.class.getName()).info("initInterp(): creating interpreter took " + (System.currentTimeMillis() - t1) + " ms"); // Report time taken to instantiate
			}

			return true; // Return success on initialization completion
		}

		/**
		 * Saves the paths of original DSS records before they are potentially overwritten.
		 * Iterates through all model alternatives and their data locations to rename records
		 * with "-save" suffix so they can be restored later if needed for ensemble generation.
		 *
		 * @return List of identifiers pointing to the saved (temporarily renamed) DSS records, null if save failed
		 */
		private List<DSSIdentifier> saveDssPaths (BaseComputeSettings computeSettings) {
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
			ModelAlternative modelAlt; // Declare variable for iteration
			ModelAltIterationSettings maSettings; // Variable to hold settings for specific alternative
			List<DataLocation> dataLocs; // List holding all data location definitions for current alt
			DataLocation dataLoc; // Variable to iterate through individual data locations
			DSSIdentifier dssId; // Variable to store identifier after renaming process
			List<DSSIdentifier> pathsRenamed = new ArrayList<>(); // Prepare list to collect successfully saved identifiers
			String variantName = _sim.getVariantName(); // Get current simulation's variant identifier
			_sim.addComputeMessage("Saving DSS records ..."); // Log progress of saving phase

			// Loop through each configured alternative
			for (int i = 0; i < modelAlts.size(); i++) {
				modelAlt = modelAlts.get(i); // Get current alternative instance

				// Check for null reference to avoid crash
				if (modelAlt == null) {
					continue; // Skip if alternative object is missing
				}

				modelAlt.setVariantName(variantName); // Set variant name consistent with simulation state
				maSettings = computeSettings.getModelAltSettings(modelAlt); // Get iteration specific settings for this alternative

				// Check if model-specific settings exist
				if (maSettings == null) {
					continue; // Skip processing if settings are missing for this alt
				}

				dataLocs = maSettings.getDataLocations(); // Retrieve list of all data locations to process

				// Validate that data locations list is populated
				if (dataLocs == null) {
					continue; // Skip if no data locations configured
				}

				// Iterate through each data location record
				for (int d = 0; d < dataLocs.size(); d++) {
					dataLoc = dataLocs.get(d); // Get current data location definition
					dssId = maSettings.getDSSIdentifierFor(dataLoc); // Get original identifier for this data source

					// Check if path is valid and non-empty
					if (dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty()) {
						continue; // Skip if DSS path information is missing
					}

					dssId = saveDssPath(dataLoc); // Call helper to rename record temporarily

					// If renaming was successful, add to collection
					if (dssId != null) {
						pathsRenamed.add(dssId); // Add identifier to tracking list
					}
				}
			}

			_sim.addComputeMessage("Saved " + pathsRenamed.size() + " DSS records ..."); // Log summary of saved records count

			return pathsRenamed; // Return collection of renamed identifiers to restore later

		}

		/**
		 * Saves a specific DSS record path by renaming it with the save suffix.
		 * Only applies if data location is linked directly to the simulation's main DSS file.
		 *
		 * @param dataLoc The DataLocation object for which to preserve original data
		 * @return DSSIdentifier representing the saved record, or null if not applicable
		 */
		private DSSIdentifier saveDssPath (DataLocation dataLoc) {
			Vector<String> srcList = new Vector<>(); // Prepare list for source paths before modification
			Vector<String> destList = new Vector<>(); // Prepare list for destination paths after renaming

			DataLocation linkedToDl = dataLoc.getLinkedToLocation(); // Get the actual DSS file link location

			// Check if it is a direct simulation DSS link
			if (linkedToDl instanceof DssDataLocation && DSSFILE.equals(dataLoc.getModelToLinkTo())) {
				DssDataLocation dssDl = (DssDataLocation) linkedToDl; // Safe cast to concrete implementation
				String dssPath = dssDl.getDssPath(); // Get directory path from data source
				String dssFile = dssDl.get_dssFile(); // Get filename from DSS object definition
				String dssFileAbs = Project.getCurrentProject().getAbsolutePath(dssFile); // Convert file reference to absolute system path

				fillInSrcAndDestList(dssFileAbs, dssPath, srcList, destList, true); // Populate source and destination lists with suffix added
				_sim.addComputeMessage("Found " + srcList + " records for " + dssPath); // Log count of records affected

				int rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFileAbs, srcList, destList); // Execute rename operation on file system

				// Verify that all requested source records were renamed
				if (rv == srcList.size()) {
					DSSIdentifier dssId = new DSSIdentifier(dssFileAbs, dssPath); // Create identifier with new path

					return dssId; // Return success identifier to caller

				} else {
					// Log warning if some records could not be renamed as expected
					_sim.addWarningMessage("Failed to save off all DSS records for " + dssPath + ".  Expected to save " + srcList.size() + " saved " + rv); // Log discrepancy between requested and actual saves
				}
			}

			return null; // Return null if not a direct DSS link

		}

		/**
		 * Fills in source and destination lists for DSS rename operation.
		 * Searches for records by DSS path string and applies suffix logic to destList based on flag.
		 * Adds all found paths to source list and computes corresponding destination paths.
		 *
		 * @param dssFile Absolute path to the DSS file containing records
		 * @param dssPath The full pathname string used to identify records within file
		 * @param srcList Collection to receive original record paths found in file
		 * @param destList Collection to receive modified paths (either with -save suffix or without)
		 * @param addSaveSuffix Boolean flag indicating whether to append -save suffix
		 */
		private static void fillInSrcAndDestList (String dssFile, String dssPath, List <String> srcList, List < String > destList,boolean addSaveSuffix) {
			List<String> paths = findPathnamesFor(dssFile, dssPath); // Retrieve list of matching DSS path objects from file
			srcList.addAll(paths); // Add all found path strings to source collection

			DSSPathname pathname = new DSSPathname(); // Create object for building and modifying paths
			// Iterate through each source record in the list
			for (int i = 0; i < srcList.size(); i++) {
				pathname.setPathname(srcList.get(i)); // Set path name from source string
				String fpart = pathname.getFPart(); // Extract functional part of path identifier

				// If suffix addition requested
				if (addSaveSuffix) {
					fpart = fpart.concat(SAVE_SUFFEX); // Append the -save suffix string to existing part

				} else {
					// If not adding suffix, remove it if already present
					// Check case-insensitively for existing suffix
					if (fpart.toLowerCase().endsWith(SAVE_SUFFEX)) {
						fpart = fpart.toLowerCase().replace(SAVE_SUFFEX, ""); // Remove -save from path to get original
					}
				}

				pathname.setFPart(fpart); // Update object with calculated FPart string
				destList.add(pathname.getPathname()); // Add reconstructed full pathname to destination list
			}
		}

		/**
		 * Searches the DSS file for all records matching a given pathname pattern.
		 * Returns vector of all pathnames found in that specific DSS file.
		 *
		 * @param dssFile Absolute path to the DSS data file being searched
		 * @param dssPath The base path string used as search key (supports wildcards like *)
		 * @return Vector containing all full paths matching the given pattern inside the DSS file
		 */
		private static List<String> findPathnamesFor (String dssFile, String dssPath) {
			DSSPathname pathname = new DSSPathname(); // Create object for defining search criteria
			pathname.setPathname(dssPath); // Set base path in object
			pathname.setDPart("*"); // Specify wildcard for dataset (time) and step variables

			DSSIdentifier dssId = new DSSIdentifier(dssFile, pathname.getPathname()); // Create identifier combining file and search pattern
			Vector pathnames = DssFileManagerImpl.getDssFileManager().searchDSSPaths(dssId); // Query manager to find matching records

			return pathnames; // Return vector of matching pathnames from search result
		}

		/**
		 * Copies data for a specific ensemble member into the simulation's time window.
		 * Adjusts DSS record dates based on starting year and iteration offset logic defined in properties.
		 * Reads TS container, shifts data back to original run time, then writes back.
		 *
		 * @param member The index number of the ensemble member to process (0-based)
		 * @return True if all members copied successfully, false if any failed or file write error occurred
		 */
		private boolean copyDssMembersForTimeWindow (int member, BaseComputeSettings computeSettings) {
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
			ModelAlternative modelAlt; // Declare variable for iteration
			ModelAltIterationSettings maSettings; // Variable to hold settings for specific alternative

			List<DataLocation> dataLocs; // List holding all data location definitions for current alt
			DataLocation dataLoc, linkedDl; // Variables for iterating through locations and their links

			DSSIdentifier dssId, srcDssId = new DSSIdentifier(); // Create identifiers for source operations
			DSSPathname pathname = new DSSPathname(); // Path object for constructing paths

			String dssPath, fileName; // Variables for file path string manipulation
			DssDataLocation dssDl; // Variable to hold concrete implementation of data location

			boolean copySuccessful = true; // Flag to track overall success state of operation

			int startingYear = findStartingYear(computeSettings); // Determine earliest year in dataset

			// Check if start year calculation failed
			if (startingYear < 0) {
				_sim.addErrorMessage("Failed to find common starting year for override DSS data"); // Log error for missing metadata
				return false; // Fail operation if start year cannot be determined
			}

			_sim.addComputeMessage("Common start year for modified BCs:" + startingYear); // Log calculated start info

			RunTimeWindow rtw = _sim.getRunTimeWindow(); // Get current simulation run time window settings
			HecTime startTime = (HecTime) rtw.getStartTime().clone(); // Clone start time to avoid modification of original object

			int year = startingYear; // Start from earliest available year
			year += member; // Increment year for each iteration member offset

			// setYearMonthDay() day is 0 based, day() is 1 based so we have to subtract one to get the correct day
			startTime.setYearMonthDay(year, startTime.month(), startTime.day() - 1); // Set new start date with corrected day index

			int startDaysToSubtract = -Integer.getInteger("PAC.StartDaysToSubtract", 0); // Get days adjustment from system properties
			startTime.addDays(startDaysToSubtract); // Adjust time further by calculated offset

			HecTime endTime = (HecTime) rtw.getEndTime().clone(); // Clone end time to avoid modification of original object

			year = startingYear; // Reset year counter for end date calculation
			year += member; // Increment year for each iteration member offset

			endTime.setYearMonthDay(year, endTime.month(), endTime.day()); // Set new end date with correct indexing

			int endDaysToAdd = Integer.getInteger("PAC.EndDaysToAdd", 1); // Get days adjustment from system properties
			endTime.addDays(endDaysToAdd); // Adjust time forward by calculated offset

			// Loop through each configured alternative
			for (int i = 0; i < modelAlts.size() && !_canceled; i++) {
				modelAlt = modelAlts.get(i); // Get current alternative instance

				// Check for null reference to avoid crash
				if (modelAlt == null) {
					continue; // Skip if alternative object is missing
				}

				maSettings = computeSettings.getModelAltSettings(modelAlt); // Get iteration specific settings for this alternative

				// Check if model-specific settings exist
				if (maSettings == null) {
					continue; // Skip processing if settings are missing for this alt
				}

				dataLocs = maSettings.getDataLocations(); // Retrieve list of all data locations to process

				// Validate that data locations list is populated
				if (dataLocs == null) {
					continue; // Skip if no data locations configured
				}

				// Iterate through each data location record, check cancellation
				for (int d = 0; d < dataLocs.size() && !_canceled; d++) {
					dataLoc = dataLocs.get(d); // Get current data location definition
					dssId = maSettings.getDSSIdentifierFor(dataLoc); // Get original identifier for this data source

					// Check if path is valid and non-empty
					if (dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty()) {
						continue; // Skip if DSS path information is missing
					}

					linkedDl = dataLoc.getLinkedToLocation(); // Get the actual DSS file link location

					// Check if linked data is stored in DSS format
					if (linkedDl instanceof DssDataLocation) {
						dssDl = (DssDataLocation) linkedDl; // Safe cast to concrete implementation
						dssPath = dssId.getDSSPath(); // Get directory path from identifier object
						fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName()); // Convert file reference to absolute system path

						srcDssId.setFileName(fileName); // Set filename in identifier for reading operation
						srcDssId.setDSSPath(dssPath); // Set directory path in identifier for reading operation
						srcDssId.setStartTime(startTime); // Set start time for read range filtering
						srcDssId.setEndTime(endTime); // Set end time for read range filtering

						_sim.addComputeMessage("Copying over Position Analysis DSS records for time window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime()); // Log operation details

						TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true); // Read data from file into memory object

						// Check if data was successfully read
						if (srcTsc != null && srcTsc.numberValues > 0) {
							HecTime srcStart = srcTsc.getStartTime(); // Retrieve original start time of loaded data
							srcStart.showTimeAsBeginningOfDay(true); // Ensure display format is consistent

							_sim.addComputeMessage("Read data for " + srcDssId + " start=" + srcStart + " end=" + srcTsc.getEndTime() + " num values=" + srcTsc.numberValues); // Log read results

							srcTsc.fileName = dssDl.get_dssFile(); // Set original filename reference for container object
							srcTsc.fullName = dssDl.getDssPath(); // Set original path reference for container object

							// no time shift the data back to the original time window
							srcTsc = shiftInTime(srcTsc, startTime, rtw); // Adjust timestamps back to match simulation context

							// Check if shifting operation failed
							if (srcTsc == null) {
								_sim.addErrorMessage("Failed to shift DSS record for " + dataLoc + ", " + srcTsc.fileName + " : " + srcTsc.fullName + " to original time"); // Log error about shifting failure
								return false; // Return failure immediately on shift error
							}

							outputTimeSeries(srcTsc); // Log detailed values from container to simulation log

							int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write shifted data back to source file

							// Check if write operation reported error code
							if (rv != 0) {
								copySuccessful = false; // Mark overall success flag as failed
								Logger.getLogger(ActionComputable.class.getName()).warning("Failed to write DSS record for " + dataLoc + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Log warning
								_sim.addErrorMessage("Failed to write DSS record for " + dataLoc + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Add high level error message

							} else {
								// If write succeeded with no errors
								/// save off the source DSS data into the collection dss file with the F part appended with -PA
								srcTsc.fileName = getCollectionsOutputDssFile(computeSettings.getCollectionDssFilename()); // Get path to collection output file

								pathname.setPathname(srcTsc.fullName); // Set full name on path object for modification
								pathname.setCollectionSequence(member); // Mark as part of ensemble collection member
								pathname.setFPart(pathname.getFPart() + "-PA"); // Append -PA suffix to functional part

								srcTsc.fullName = pathname.getPathname(); // Update container object full name
								rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write data to collection file

								_sim.addComputeMessage("   Copied " + srcDssId + " to " + dssDl.get_dssFile() + ":" + dssDl.getDssPath()); // Log completion message
							}
						} else {
							// If data was empty or read failed
							_sim.addErrorMessage("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime()); // Log warning about missing data
						}
					}
				}
			}

			// Return final result of operation
			return copySuccessful;

		}

		/**
		 * Finds the earliest year available across all input DSS records for modification.
		 * Used to calculate start date offsets before copying data.
		 *
		 * @param computeSettings The base settings containing configuration for each model alternative
		 * @return Integer representing the starting year found in files, -1 if none found
		 */
		private int findStartingYear (BaseComputeSettings computeSettings) {
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
			ModelAlternative modelAlt; // Declare variable for iteration
			ModelAltIterationSettings maSettings; // Variable to hold settings for specific alternative
			List<DataLocation> dataLocs; // List holding all data location definitions for current alt
			DataLocation dataLoc, linkedDl; // Variables for iterating through locations and their links

			DSSIdentifier dssId, srcDssId = new DSSIdentifier(); // Create identifiers for source operations
			DssDataLocation dssDl; // Variable to hold concrete implementation of data location

			String dssPath, fileName; // Variables for file path string manipulation

			HecTime[] times; // Array to receive time range data from manager
			int startYear = -1, year; // Initialize return value and loop counter

			// Loop through each configured alternative
			for (int i = 0; i < modelAlts.size() && !_canceled; i++) {
				modelAlt = modelAlts.get(i); // Get current alternative instance

				// Check for null reference to avoid crash
				if (modelAlt == null) {
					continue; // Skip if alternative object is missing
				}

				maSettings = computeSettings.getModelAltSettings(modelAlt); // Get iteration specific settings for this alternative

				// Check if model-specific settings exist
				if (maSettings == null) {
					continue; // Skip processing if settings are missing for this alt
				}

				dataLocs = maSettings.getDataLocations(); // Retrieve list of all data locations to process

				// Validate that data locations list is populated
				if (dataLocs == null) {
					continue; // Skip if no data locations configured
				}

				// Iterate through each data location record, check cancellation
				for (int d = 0; d < dataLocs.size() && !_canceled; d++) {
					dataLoc = dataLocs.get(d); // Get current data location definition
					dssId = maSettings.getDSSIdentifierFor(dataLoc); // Get original identifier for this data source

					// Check if path is valid and non-empty
					if (dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty()) {
						continue; // Skip if DSS path information is missing
					}

					linkedDl = dataLoc.getLinkedToLocation(); // Get the actual DSS file link location

					// Check if linked data is stored in DSS format
					if (linkedDl instanceof DssDataLocation) {
						dssDl = (DssDataLocation) linkedDl; // Safe cast to concrete implementation
						dssPath = dssId.getDSSPath(); // Get directory path from identifier object

						fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName()); // Convert file reference to absolute system path
						srcDssId.setFileName(fileName); // Set filename in identifier for reading operation
						srcDssId.setDSSPath(dssPath); // Set directory path in identifier for reading operation

						times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(srcDssId, 0); // Get time range from file metadata

						// Check if manager returned time data successfully
						if (times != null) {
							year = times[0].year(); // Extract year from first available time point
							startYear = Math.max(startYear, year); // Track highest start year found (earliest in calendar)
						} else {
							// Log warning if metadata read failed for this record
							_sim.addWarningMessage("Failed to find start time for TS Record " + srcDssId); // Add specific warning message
						}
					}
				}
			}

			// Return final calculated year or -1 if not found
			return startYear;
		}

		/**
		 * Logs a sample of the current time series values to the simulation log for debugging.
		 * Prints first 10 data points with associated timestamp and value.
		 *
		 * @param srcTsc The TimeSeriesContainer holding the data values and times
		 */
		private void outputTimeSeries (TimeSeriesContainer srcTsc) {
			HecTime time = new HecTime(); // Create temporary time object

			// Iterate through first 10 data points of series
			for (int i = 0; i < 10; i++) {
				time.set(srcTsc.times[i]); // Set temporary time to point i in array
				_sim.addLogMessage("Time=" + time + " value=" + srcTsc.values[i]); // Add formatted line to simulation log component
			}
		}

		/**
		 * Adjusts the time stamps of a TimeSeriesContainer to match the original RunTimeWindow.
		 * Calculates difference in minutes between provided start times and shifts data accordingly.
		 * Uses HecMathException handling for invalid shift operations.
		 *
		 * @param srcTsc The TimeSeriesContainer containing data to shift timestamps on
		 * @param startTime The target start time to align all data against
		 * @param rtw The RunTimeWindow defining the simulation's context window
		 * @return Adjusted TimeSeriesContainer with aligned times, null if shifting failed
		 */
		private TimeSeriesContainer shiftInTime (TimeSeriesContainer srcTsc, HecTime startTime, RunTimeWindow rtw) {
			_sim.addComputeMessage("Time shifting " + srcTsc.fullName + " from " + startTime + " to " + rtw.getStartTime()); // Log operation details

			int diff = rtw.getStartTime().value() - startTime.value(); // Calculate minute difference between windows
			String shift = Integer.toString(diff) + " Minute"; // Format message describing the magnitude of shift
			_sim.addComputeMessage("Time shifting " + srcTsc.fullName + " " + shift); // Log confirmation with shift amount

			TimeSeriesMath tsm; // Variable for math manager instance
			// Attempt transformation within try block
			try {
				tsm = new TimeSeriesMath(srcTsc); // Create math manager for this container
				tsm = (TimeSeriesMath) tsm.shiftInTime(shift); // Apply shift transformation to data structure

			} catch (HecMathException e) {
				e.printStackTrace(); // Print full stack trace to console for debugging
				return null; // Return null to indicate failure state
			}

			TimeSeriesContainer shiftedTsc = tsm.getContainer(); // Get resulting container after transformation
			return shiftedTsc; // Return adjusted object containing updated timestamps
		}

		/**
		 * Copies data records from one DSS file to another using file manager utilities.
		 * Handles multiple record paths for bulk transfer within a single call.
		 *
		 * @param m Member index number to be logged in progress messages
		 * @return True if all members processed successfully, false if any failed or cancelled
		 */
		private boolean copyDssMembers ( int member, BaseComputeSettings computeSettings) {
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
			ModelAlternative modelAlt; // Declare variable for iteration
			ModelAltIterationSettings maSettings; // Variable to hold settings for specific alternative

			List<DataLocation> dataLocs; // List holding all data location definitions for current alt
			DataLocation dataLoc, linkedDl; // Variables for iterating through locations and their links

			DSSIdentifier dssId, srcDssId = new DSSIdentifier(); // Create identifiers for source operations
			DSSPathname pathname = new DSSPathname(); // Path object for constructing paths

			String dssPath, fileName; // Variables for file path string manipulation
			DssDataLocation dssDl; // Variable to hold concrete implementation of data location

			boolean copySuccessful = true; // Flag to track overall success state of operation

			// Check system property if only copying within bounds
			if (Boolean.getBoolean("Iteration.OnlyCopyTimeWindow")) {
				RunTimeWindow rtw = _sim.getRunTimeWindow(); // Get current simulation run time window settings

				HecTime startTime = (HecTime) rtw.getStartTime().clone(); // Clone start time to avoid modification of original object
				HecTime endTime = (HecTime) rtw.getEndTime().clone(); // Clone end time to avoid modification of original object

				int startDaysToSubtract = -Integer.getInteger("Iteration.StartDaysToSubtract", 0); // Get days adjustment from system properties
				startTime.addDays(startDaysToSubtract); // Adjust time backward by calculated offset
				srcDssId.setStartTime(startTime); // Set adjusted start time on identifier object

				int endDaysToAdd = -Integer.getInteger("Iteration.EndDaysToAdd", 0); // Get days adjustment from system properties
				endTime.addDays(endDaysToAdd); // Adjust time forward by calculated offset
				srcDssId.setEndTime(endTime); // Set adjusted end time on identifier object
			}

			_sim.addComputeMessage("Copying over iterative DSS records..."); // Log progress start message

			// Loop through each configured alternative
			for (int i = 0; i < modelAlts.size() && !_canceled; i++) {
				modelAlt = modelAlts.get(i); // Get current alternative instance

				// Check for null reference to avoid crash
				if (modelAlt == null) {
					continue; // Skip if alternative object is missing
				}

				maSettings = computeSettings.getModelAltSettings(modelAlt); // Get iteration specific settings for this alternative

				// Check if model-specific settings exist
				if (maSettings == null) {
					continue; // Skip processing if settings are missing for this alt
				}

				dataLocs = maSettings.getDataLocations(); // Retrieve list of all data locations to process

				// Validate that data locations list is populated
				if (dataLocs == null) {
					continue; // Skip if no data locations configured
				}

				// Iterate through each data location record, check cancellation
				for (int d = 0; d < dataLocs.size() && !_canceled; d++) {
					dataLoc = dataLocs.get(d); // Get current data location definition
					dssId = maSettings.getDSSIdentifierFor(dataLoc); // Get original identifier for this data source

					// Check if path is valid and non-empty
					if (dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty()) {
						continue; // Skip if DSS path information is missing
					}

					linkedDl = dataLoc.getLinkedToLocation(); // Get the actual DSS file link location

					// Check if linked data is stored in DSS format
					if (linkedDl instanceof DssDataLocation) {
						dssDl = (DssDataLocation) linkedDl; // Safe cast to concrete implementation
						dssPath = dssId.getDSSPath(); // Get directory path from identifier object

						// Check if this is a collection file
						if (DSSPathname.isaCollectionPath(dssPath)) {
							pathname.setPathname(dssPath); // Set path on pathname object
							pathname.setCollectionSequence(member); // Mark as part of ensemble collection member
							dssPath = pathname.getPathname(); // Update string reference to modified path
						}

						fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName()); // Convert file reference to absolute system path
						srcDssId.setFileName(fileName); // Set filename in identifier for reading operation
						srcDssId.setDSSPath(dssPath); // Set directory path in identifier for reading operation

						TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true); // Read data from file into memory object

						// Check if data was successfully read
						if (srcTsc != null && srcTsc.numberValues > 0) {
							srcTsc.fileName = dssDl.get_dssFile(); // Set original filename reference for container object
							srcTsc.fullName = dssDl.getDssPath(); // Set original path reference for container object

							int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write back to same file if valid (no collection yet)

							// Check if write operation reported error code
							if (rv != 0) {
								copySuccessful = false; // Mark overall success flag as failed
								Logger.getLogger(ActionComputable.class.getName()).warning("Failed to write DSS record for " + dataLoc + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Log warning
								_sim.addErrorMessage("Falied to write DSS record for " + dataLoc + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Add high level error message (note typo preserved in original)

							} else {
								// If write succeeded with no errors
								/// save off the source DSS data into the collection dss file with the F part appended with -ITER
								srcTsc.fileName = getCollectionsOutputDssFile(computeSettings.getCollectionDssFilename()); // Get path to collection output file

								pathname.setPathname(srcTsc.fullName); // Set full name on path object for modification
								pathname.setCollectionSequence(member); // Mark as part of ensemble collection member
								pathname.setFPart(pathname.getFPart() + "-ITER"); // Append -ITER suffix to functional part

								srcTsc.fullName = pathname.getPathname(); // Update container object full name
								rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write data to collection file

								_sim.addComputeMessage("   Copied " + dssPath + " to " + dssDl.get_dssFile() + ":" + dssDl.getDssPath()); // Log completion message
							}
						} else {
							// If data was empty or read failed
							_sim.addErrorMessage("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime()); // Log warning about missing data
						}
					}
				}
			}

			// Return final result of operation
			return copySuccessful;

		}

		/**
		 * Restores all DSS records that were saved in saveDssPaths() to their original locations.
		 * Reverses the renaming logic by deleting saved copies and renaming back originals if needed.
		 *
		 * @param savedDssPaths List of identifiers representing backed-up records to be restored
		 */
		private void restoreDssPaths (List < DSSIdentifier > savedDssPaths) {
			_sim.addComputeMessage("Restoring original " + savedDssPaths.size() + " DSS records ..."); // Log progress start message

			Vector<String> srcList = new Vector<>(); // Prepare list for source paths before modification
			Vector<String> destList = new Vector<>(); // Prepare list for destination paths after renaming

			DSSIdentifier dssId; // Variable to hold current identifier in loop
			String path, dssFile; // Variables for file path string manipulation

			Vector<String> singleSrcList = new Vector(); // Temporary vector for single source record
			Vector<String> singleDestList = new Vector(); // Temporary vector for single dest record

			DSSPathname pathname = new DSSPathname(); // Path object for constructing paths

			// Loop through each saved identifier
			for (int i = 0; i < savedDssPaths.size(); i++) {
				srcList.clear(); // Clear lists before processing new record
				destList.clear();

				dssId = savedDssPaths.get(i); // Get identifier from list

				_sim.addComputeMessage("Restoring DSS path for " + dssId); // Log operation details

				path = dssId.getDSSPath(); // Get directory portion of stored path
				pathname.setPathname(path); // Set full stored pathname in object
				String fpart = pathname.getFPart(); // Extract functional part of path identifier

				fpart = fpart.concat(SAVE_SUFFEX); // Append the -save suffix back for reverse lookup
				pathname.setFPart(fpart); // Update object with modified FPart to identify source

				path = pathname.getPathname(); // Reconstruct full path string from modified parts
				dssFile = dssId.getFileName(); // Get base filename of DSS file containing records

				fillInSrcAndDestList(dssFile, path, srcList, destList, false); // Populate lists by removing -save suffix to find original

				// Check if source and dest list sizes match for safety
				if (destList.size() != srcList.size()) {
					_sim.addWarningMessage("Mismatched source and dest lists for " + dssId); // Log discrepancy warning
					_sim.addWarningMessage("Source List=" + srcList); // Display contents of source list for debug
					_sim.addWarningMessage("Dest List=" + destList); // Display contents of destination list for debug
				}

				int size = Math.min(srcList.size(), destList.size()); // Calculate safe iteration limit to avoid bounds error

				// Loop through calculated subset of records
				for (int s = 0; s < size; s++) {
					singleSrcList.clear(); // Clear single source list

					singleDestList.clear(); // Clear single destination list

					singleSrcList.add(srcList.get(s)); // Add current source record to single list

					singleDestList.add(destList.get(s)); // Add current dest record to single list

					int rv = DssFileManagerImpl.getDssFileManager().delete(dssFile, singleDestList); // Delete the saved copy from file system
					if (rv != 0) { // Check if delete failed
						_sim.addWarningMessage("Failed to delete DSS records for " + dssFile + ":" + singleDestList.get(0)); // Log delete failure warning
					}

					_sim.addComputeMessage("Restoring " + singleSrcList + " to " + singleDestList); // Log successful restore message

					rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFile, singleSrcList, singleDestList); // Move saved copy back to original name
					if (rv != singleSrcList.size()) { // Check if rename did not match expected count
						_sim.addWarningMessage("Failed to restore DSS records for " + dssFile + ":" + singleDestList.get(0)); // Log error about incomplete restore
						_sim.addWarningMessage("Expected " + singleSrcList.size() + " records to be restored. Restored " + rv + " Records."); // Log discrepancy detail
					}
				}
			}
		}

		/**
		 * Copies computed results from the simulation DSS file into a collections DSS file.
		 * Calls updateIterationDssWithDssData for each model alternative to ensure all data is captured.
		 *
		 * @param interationId The member index number of current iteration in the ensemble
		 * @param computeSettings Configuration defining where results should be written
		 */
		private void copyDssResultsToCollectionsDss ( int interationId, BaseComputeSettings computeSettings) {
			_sim.addComputeMessage("Saving Computed DSS records to collections"); // Log operation start message

			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
			ModelAlternative modelAlt; // Declare variable for iteration

			// Loop through each alternative
			for (int m = 0; m < modelAlts.size() && !_canceled; m++) {
				modelAlt = modelAlts.get(m); // Get current alternative instance

				if (modelAlt == null) { // Check for null reference to avoid crash
					continue; // Skip if alternative object is missing
				}

				updateIterationDssWithDssData(modelAlt, interationId, computeSettings); // Update each file individually
			}
		}
		/**
		 * Copies output records from the simulation DSS file into the collection storage.
		 * Uses FPart matching to identify computed results and writes them to collection path.
		 *
		 * @param modelAlt The specific ModelAlternative object defining where data resides
		 */
		private boolean updateIterationDssWithDssData (ModelAlternative modelAlt,int interationId, BaseComputeSettings computeSettings) {
			_sim.addComputeMessage("Saving Computed DSS records to collections for " + modelAlt); // Log context of operation

			String fPart = _sim.getFPart(modelAlt); // Get functional part identifier for this alternative

			ComputeOptions co = modelAlt.getComputeOptions(); // Retrieve compute options for this alternative
			String dssFile = co.getDssFilename(); // Extract DSS filename from options configuration

			DSSIdentifier dssId = new DSSIdentifier(dssFile); // Create identifier to search file contents
			Vector<String> srcPaths = DssFileManagerImpl.getDssFileManager().searchDSSPaths(dssId, "F=" + fPart); // Search for records matching FPart

			// Check if search returned empty or failed
			if (srcPaths == null) {
				// nothing to copy
				_sim.addComputeMessage("No Output DSS records found for " + modelAlt.getProgram() + " model " + modelAlt + " FPart=" + fPart); // Log why operation skipped

				return true; // Return success even if no data existed to save (skipped by design)
			}

			_sim.addComputeMessage("Copying output DSS for " + modelAlt.getProgram() + " model " + modelAlt + " to " + computeSettings.getCollectionDssFilename()); // Log target file information

			DSSPathname pathname = new DSSPathname(); // Create object for building and modifying paths
			Vector<String> destPaths = new Vector<>(srcPaths.size()); // Prepare vector with expected size to avoid resizing

			String path; // Variable for iterating through source paths

			// Loop through each source record
			for (int i = 0; i < srcPaths.size() && !_canceled; i++) {
				path = srcPaths.get(i); // Get next source path

				pathname.setPathname(path); // Set current path string in object
				pathname.setCollectionSequence(interationId); // Mark as part of ensemble collection member

				destPaths.add(pathname.getPathname()); // Add constructed destination path to list
			}

			String iterDssFile = getCollectionsOutputDssFile(computeSettings.getCollectionDssFilename()); // Get full absolute path for target file

			int rv = copyRecords(dssFile, iterDssFile, srcPaths, destPaths); // Call utility method to copy data between files

			// Check if operation reported partial or full failure
			boolean success = rv == srcPaths.size(); // Verify return code matches number of records processed
			if (!success) {
				_sim.addErrorMessage("Failed to update iteration DSS file with " + modelAlt.getProgram() + " model " + modelAlt + "'s results"); // Log error message
			}

			return success; // Return final status of bulk copy operation
		}

		/**
		 * Constructs the absolute path for the collections output DSS file.
		 * Uses the directory of the main simulation DSS file and appends the collection filename.
		 * Caches the result to avoid recomputation on multiple calls.
		 *
		 * @param dssFileName The configuration property defining the collection filename (without extension)
		 * @return Absolute filesystem path to the collections output file
		 */
		private String getCollectionsOutputDssFile (String dssFileName) {
			// Check if cache variable is still empty
			if (_iterDssFile == null) {
				String dssFile = _sim.getSimulationDssFile(); // Get base simulation DSS filename
				String computeFolder = RMAIO.getDirectoryFromPath(dssFile); // Extract directory path containing the file
				_iterDssFile = RMAIO.concatPath(computeFolder, dssFileName); // Combine folder and filename to form full path
			}

			return _iterDssFile; // Return cached or newly constructed path
		}

		/**
		 * Copies records from one DSS file to another in bulk.
		 * Uses HEC utilities to manage cross-file record transfer operations efficiently.
		 *
		 * @param dssFile The source DSS filename containing records to be copied
		 * @param forecastDSSFilename The target DSS filename where records will be written
		 * @param srcPaths List of full pathnames (including FPart) in the source file
		 * @param destPaths List of corresponding full pathnames in the destination file
		 * @return Integer return code indicating number of successfully copied records (matches src size if success)
		 */
		private int copyRecords (String fromDssFile, String toDssFile, Vector < String > srcPaths, Vector < String > destPaths){
			HecDSSUtilities fromDataManager = new HecDSSUtilities(); // Create utility manager for source file
			fromDataManager.setDSSFileName(fromDssFile); // Set source filename on utility manager object
			HecDataManager toDataManager = new HecDataManager(toDssFile); // Create utility manager for target file

			_sim.addComputeMessage("Copying records from " + fromDssFile); // Log operation start message

			int rv = fromDataManager.copyRecordsFrom(toDataManager, srcPaths, destPaths); // Execute the cross-file copy command
			return rv; // Return result code from manager
		}

		@Override // Override interface method to return model count for progress bars
		public int getModelCount () {
			return _sim.getModelCount(); // Delegate to underlying simulation object for count
		}

		@Override // Override interface method to handle cancellation requests during long compute
		public boolean cancelCompute () {
			_canceled = true; // Set cancellation flag in instance variable

			return _sim.cancelCompute(); // Request cancellation on simulation engine and return its result
		}

		@Override // Override interface method to get path of log file for debug output
		public String getLogFile () {
			return _sim.getLogFile(); // Delegate to underlying simulation object for path string
		}

		@Override // Override interface method to return human readable name for UI display
		public String getName () {
			return _sim.getName(); // Delegate to underlying simulation object for name string
		}

		@Override // Override interface method to check if simulation needs actual computation
		public boolean needToCompute () {
			return _sim.needToCompute(); // Delegate to underlying simulation object for status check
		}

		@Override // Override toString method for consistent debugging output format
		public String toString () {
			return _sim.getName(); // Return simulation name as default string representation

		}

		/**
		 * Sets the computation progress dialog reference used during interactive runs.
		 * Allows UI updates to be directed to this specific panel instance.
		 *
		 * @param computeDlg The UsgsComputeSelectorDialog UI component to link with this computable object
		 */
		public void setProgressDialog (UsgsComputeSelectorDialog computeDlg) {
			_computeDialog = computeDlg; // Assign incoming dialog object to instance variable
		}

}