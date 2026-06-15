package usbr.wat.plugins.actionpanel.model.forecast;

import java.awt.Component; // Import Component class for UI component identification during progress panel manipulation
import java.awt.EventQueue; // Import EventQueue for scheduling GUI updates on EDT thread in event-driven operations
import java.io.BufferedReader; // Import BufferedReader for reading text file content line by line from external scripts
import java.io.File; // Import File class for accessing filesystem paths and converting between relative and absolute references
import java.io.IOException; // Import IOException to handle exceptions occurring during file read/write operations
import java.nio.file.Files; // Import Files utility for high-level file operations like writing and truncating
import java.nio.file.Path; // Import Path interface for representing filesystem paths in modern Java API
import java.nio.file.Paths; // Import Paths factory class for creating Path objects from string literals or other paths
import java.nio.file.StandardOpenOption; // Import standard open options enum for controlling file write behavior (CREATE, TRUNCATE)
import java.util.ArrayList; // Import ArrayList for creating dynamic list collections to store data identifiers
import java.util.HashMap; // Import HashMap for key-value storage mappings between DSS identifier pairs
import java.util.HashSet; // Import HashSet for managing unique sets of identifiers during save/restore operations
import java.util.Iterator; // Import Iterator interface for sequential traversal of collection entries
import java.util.List; // Import List interface for working with ordered collections of items and paths
import java.util.Map; // Import Map interface for key-value storage between source and destination identifiers
import java.util.Set; // Import Set interface for managing unique collections during set operations
import java.util.StringTokenizer; // Import StringTokenizer for splitting strings by delimiters (like classpath separation)
import java.util.Vector; // Import Vector legacy list type when thread synchronization is needed or original design used it
import java.util.logging.Logger; // Import Logger utility from java.util package for standard logging output

import javax.swing.JOptionPane; // Import JOptionPane for displaying dialog messages to user during debug operations
import javax.swing.JProgressBar; // Import JProgressBar class for updating progress bar strings in UI panels
import com.google.common.flogger.FluentLogger; // Import FluentLogger from Guava library for fluent-style warning and error logging
import com.rma.client.Browser; // Import Browser model for accessing the main application frame component
import com.rma.editors.ComputeProgressDialog; // Import dialog editor class that manages the compute progress panel
import com.rma.io.DssFileManager; // Import DSS file manager interface defining methods for DSS data operations
import com.rma.io.DssFileManagerImpl; // Import implementation class for accessing actual DSS file management functionality
import com.rma.io.FileManagerImpl; // Import generic file manager utility for handling regular file system operations
import com.rma.io.RmaFile; // Import RMA File wrapper object used throughout the application for file abstraction
import com.rma.model.ComputeProgressListener; // Import listener interface for receiving compute progress notifications
import com.rma.model.ComputeProgressListener2; // Import enhanced listener interface for advanced progress management capabilities
import com.rma.model.Project; // Import Project model for accessing project directory and file path utilities
import com.rma.model.RealizationComputable; // Import computational interface defining realization-specific computation methods
import com.rma.ui.ComputeProgressPanel; // Import UI panel component displaying compute progress information

import hec.heclib.dss.DSSPathname; // Import DSS pathname utility for constructing and manipulating record path identifiers
import hec.heclib.dss.HecDSSFileDataManager; // Import file data manager for opening and closing DSS files properly
import hec.heclib.dss.HecDSSUtilities; // Import utilities class for cross-file DSS record operations like copy/delete
import hec.heclib.dss.HecDataManager; // Import manager class for writing DSS container objects to files
import hec.heclib.util.HecTime; // Import HEC Time utility class for creating and manipulating time objects
import hec.hecmath.HecMathException; // Import exception class thrown when mathematical operations fail on time series
import hec.hecmath.TimeSeriesMath; // Import math container for time series transformations like shifting or scaling
import hec.io.DSSIdentifier; // Import identifier class representing a specific DSS record by filename and path
import hec.io.PairedDataContainer; // Import container type for paired data (multiple variables at same location)
import hec.io.TextContainer; // Import container type for text data stored in DSS records
import hec.io.TimeSeriesContainer; // Import container type for time series data in DSS format
import hec.model.RunTimeWindow; // Import runtime window model defining simulation start and end times
import hec2.model.DataLocation; // Import data location interface representing input or output sources
import hec2.model.DssDataLocation; // Import concrete implementation of data location linked to DSS files
import hec2.plugin.model.ComputeOptions; // Import compute options class for configuring how a simulation executes
import hec2.plugin.model.ModelAlternative; // Import model alternative class representing a specific program configuration
import hec2.wat.model.WatSimulation; // Import WAT simulation base class for all simulation model objects
import hec2.wat.plugin.SimpleWatPlugin; // Import simple plugin interface used to retrieve default run directories
import hec2.wat.plugin.WatPlugin; // Import generic plugin interface for model program identification
import hec2.wat.plugin.WatPluginManager; // Import manager class for retrieving specific plugin instances by name

import org.python.core.Py; // Import Py utility for converting Java objects to/from Python objects via Jython
import org.python.core.PyCode; // Import compiled bytecode object representing executable Python script code
import org.python.core.PyException; // Import exception type specific to Python interpreter runtime errors
import org.python.core.PyObject; // Import base class for all Python objects in the Jython system
import org.python.core.PyString; // Import string wrapper class used within the Python interpreter context
import org.python.core.PyStringMap; // Import map-like dictionary structure for local variable storage in Python
import org.python.core.PySystemState; // Import system state object for managing packages and directories in Python
import org.python.util.PythonInterpreter; // Import interpreter interface for initializing and executing Jython scripts

import rma.util.RMAIO; // Import utility class for file path concatenation and string manipulation operations

import usbr.wat.plugins.actionpanel.ActionPanelPlugin; // Import action panel plugin singleton for accessing UI components
import usbr.wat.plugins.actionpanel.actions.forecast.RunForecastSimulationAction; // Import forecast simulation action class with helper methods for parsing member sets
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings; // Import base settings interface for iteration configuration access
import usbr.wat.plugins.actionpanel.model.ComputeSettings; // Import compute settings class managing Python script configurations
import usbr.wat.plugins.actionpanel.model.ComputeType; // Import enum type specifying standard/iterative/position analysis modes
import usbr.wat.plugins.actionpanel.model.IcPathMap; // Import initial condition path mapping utility for IC data copy operations
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings; // Import iteration settings class defining member array configuration
import usbr.wat.plugins.actionpanel.model.UsbrComputable; // Import user-specific computable interface extending base computational methods
import usbr.wat.plugins.actionpanel.ui.forecast.CsvReader; // Import CSV reader utility for parsing configuration files


/**
 * ForecastActionComputable implements the computational logic for forecast ensemble simulations including sensitivity analysis,
 * position analysis workflows, and iterative runs with boundary condition overrides. It manages multiple ensemble sets, each containing
 * member arrays to process, while copying DSS data before/after simulation compute operations.
 *
 * This class handles:
 *   Loading and parsing boundary condition, initial condition, and temperature target configuration files
 *   Copying source data (BCs, ICs, temp targets) with optional save/restore for ensemble generation
 *   Executing pre-compute and post-compute Python scripts via Jython interpreter
 *   Managing multiple ensemble sets through sequential processing loop<
 *   Capturing compute results into collection DSS files with proper FPart identifiers

 *
 * The class extends UsbrComputable and RealizationComputable interfaces to support progress tracking and UI integration.
 */

public class ForecastActionComputable implements UsbrComputable, RealizationComputable {
	// FluentLogger instance for logging warnings and errors from this class's operations
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass(); // Initialize logger using fluent API pattern

	// System property controlling whether to save original DSS records during compute (for ensemble debugging)
	private static final String SAVE_DSS_RECORDS_PROP = "ForecastCompute.SaveDssRecords"; // Configuration flag for save behavior

	// Path string to the boundary condition configuration file read from relative path in ForecastConfigFiles utility
	private static final String BC_CONFIG_FILE = ForecastConfigFiles.getRelativeBCConfigFile(); // Location of BC data mapping CSV file

	// Path string to the initial condition configuration file referenced in forecast workflow
	private static final String IC_CONFIG_FILE = ForecastConfigFiles.getRelativeICConfigFile(); // Location of IC data mapping CSV file

	// Path string to temperature target set configuration file used for model alternative configurations
	private static final String TEMP_TARGET_CONFIG_FILE = ForecastConfigFiles.getRelativeTempTargetConfigFile(); // Location of temp target config

	// Path string to temperature target control locations configuration file for location-specific mappings
	private static final String TEMP_TARGET_CONTROL_LOCS_FILE = ForecastConfigFiles.getRelativeTempTargetControlLocsFile(); // Control loc mapping CSV

	// Suffix appended to DSS record paths when saving original data for ensemble analysis tracking
	private static final String SAVE_SUFFEX = "-save"; // Constant suffix marker for backed-up records

	// Name of the iteration results file storing intermediate ensemble outputs during processing
	public static final String ITERATION_DSS_FILE = "iterationResults.dss"; // Constant filename definition

	// Text label displayed in reports identifying which DSS file stores simulation data
	public static final String DSSFILE = "DSS File"; // Report XML label constant

	// Expected method signature string that Python scripts must implement for compatibility with WAT compute options
	public static final String METHOD_SIGNATURE = "runIteration(modelAlternative, currentIteration, maxIteration)"; // Required method name

	// Placeholder string in temp target control locations file used to insert river location name dynamically
	public static final String TEMP_TARGET_CONTROL_LOC_REPLACE = "%location%"; // Placeholder token for location replacement

	// Filename of a text file tracking which ensemble member was last processed (for resumption after interruption)
	private static final String CURRENT_ENSEMBLE_FILE_NAME = "current_ensemble.txt"; // Current ensemble index tracker filename

	// Special error code returned when writing DSS records encounters type mismatch between data and target format
	private final int DSS_WRITE_TYPE_MISMATCH_ERROR_CODE = -534304000; // Specific write error code value

	// Boolean flag controlling whether to recompute all data or only members not yet computed in the ensemble set
	private final boolean _recomputeAll; // Recompute flag determined by user selection at initialization

	// Array storing member indices for current ensemble set being processed (populated from configuration)
	private int[] _members; // Integer array of member numbers to compute for current iteration

	/**
	 * the starting collection number to copy the data to the collections output file
	 */
	private final List<EnsembleSet> _selectedESets; // Collection of selected ensemble sets for processing this forecast scenario

	private ForecastSimGroup _simGroup; // Reference to forecast simulation group object managing initial conditions and structure

	private WatSimulation _sim; // The underlying WAT simulation model instance being computed for this forecast workflow

	// Path string stored temporarily to identify where iteration results are written to disk
	private String _iterDssFile; // File path for storing ensemble iteration outputs

	// Python interpreter instance used to execute pre- and post-compute scripts
	private PythonInterpreter _interp; // Jython Interpreter instance for executing script code

	// Debug flag to enable verbose logging and user interface messages during computation
	private boolean _debug; // Boolean flag to activate debugging mode output

	// Map storing compiled Python code objects keyed by model alternative for pre-compute phase caching
	private transient Map<ModelAlternative, PyCode> _preCodeMap = new HashMap<>(); // Cache of pre-scripts compiled bytecode per model

	// Map storing compiled Python code objects keyed by model alternative for post-compute phase caching
	private transient Map<ModelAlternative, PyCode> _postCodeMap = new HashMap<>(); // Cache of post-scripts compiled bytecode per model

	// Text buffer holding the current script source code being executed or debugged for error reporting
	private String _currentScriptText; // Source text string of currently running Python script

	// Dialog object managing computation progress display (progress bar, messages)
	private ComputeProgressDialog _computeDialog; // Progress dialog instance for user feedback

	// Flag indicating whether the computation process was interrupted by the user or an error condition
	private boolean _canceled; // Boolean flag indicating compute cancellation status

	// Enum specifying which type of computation workflow is currently active (Standard, Iterative, Position Analysis)
	private ComputeType _computeType; // Type of computation mode to use for run() invocation

	// Reference to boundary condition DSS paths map for BC data copy operations during compute
	private DssPathMap _bcDssPathMap; // Path map object for managing BC source/destination relationships

	// Reference to initial condition DSS paths map for IC data copy operations during compute
	private IcPathMap _icDssPathMap; // Path map object for managing IC source/destination relationships

	// Reference to temperature target set DSS paths map for TT data copy operations during compute
	private TempTargetDssPathMap _tempTargetDssPathMap; // Path map object for managing temp target relationships

	// List of mappings for temperature target control locations (location-specific configuration)
	private List<TempTargetControlLocsMapping> _tempTargetControlLocsDssPathMap; // Collection of location mapping objects

	// UI panel component for displaying compute progress information and status messages
	private ComputeProgressPanel _computeProgressPanel; // Progress panel UI reference for status updates

	// Counter tracking total number of ensemble members across all selected sets (for progress display)
	private int _memberCnt; // Total member count across all ensemble sets

	// Index counter tracking current member being processed within each ensemble set
	private int _memberIdx; // Current member index in iteration loop

	// Progress bar reference for lifecycle phase updates during ensemble processing
	private JProgressBar _lifecyclePbar; // Lifecycle progress bar component reference

	// Constructor accepting simulation group and data
	public ForecastActionComputable(ForecastSimGroup simGroup, WatSimulation sim, List<EnsembleSet> selectedESets, boolean recomputeAll) {
		super(); // Invoke superclass default constructor
		_simGroup = simGroup; // Assign forecast simulation group reference instance variable
		_sim = sim; // Assign simulation model instance reference variable
		_selectedESets = selectedESets; // Assign collection of selected ensemble sets for processing
		_recomputeAll = recomputeAll; // Store compute type flag (all or incremental only)
		calculateMemberCount(); // Call helper method to count total members from all sets
	}

	// Private helper counts members across all selected ensemble sets
	private void calculateMemberCount() {
		EnsembleSet eset; // Declare loop variable for current ensemble set
		String memberSet; // Variable for configuration string defining member indices
		int[] members; // Variable for parsed integer array of member numbers

		// Iterate through each configured ensemble set
		for (int i = 0; i < _selectedESets.size(); i++) {
			eset = _selectedESets.get(i); // Get current ensemble set from collection
			memberSet = eset.getMemberSetToCompute(); // Retrieve configuration string for this set
			members = RunForecastSimulationAction.getIntegerSet(memberSet); // Parse comma-separated list into integer array

			// Check if member array is non-empty
			if (members != null && members.length > 0) {
				_memberCnt += members.length; // Add to running total count
			}
		}
	}


	// Implements runnable interface - delegates to compute method
	public void run() {
		compute(); // Call main compute logic entry point
	}

	// Interface method - checks if simulation can currently be computed
	public boolean isComputable() {
		return _sim.isComputable(); // Delegate check to wrapped simulation object status
	}

	// Interface method - registers progress listener callback
	public void addComputeListener(ComputeProgressListener listener) {
		_sim.addComputeListener(listener); // Add listener to simulation object event handler
	}

	// Interface method - removes registered progress listener
	public void removeComputeProgressListener(ComputeProgressListener listener) {

	}

	// Main computation entry point for forecast ensemble simulations
	public boolean compute() {
		return ensembleCompute(); // Delegate to ensemble-specific compute logic method
	}


	/**
	 * Executes a complete forecast ensemble simulation run processing all selected ensemble sets.
	 * Iterates through each ensemble set, extracting member arrays and tracking lifecycle progress via UI panel.
	 * Updates progress bar strings for each set being processed and tracks realizations across members.
	 *
	 * @return True if all ensemble sets were successfully computed without fatal errors, false otherwise
	 */
	private boolean ensembleCompute() {
		_debug = Boolean.getBoolean("ActionComputable.debugCompute"); // Enable debug output if system property set
		_computeProgressPanel = (ComputeProgressPanel) _computeDialog.getContentPane().getComponent(0); // Cast and retrieve UI progress panel reference
		Component[] comps = _computeProgressPanel.getComponents(); // Get all components within the progress panel

		// Iterate through components in reverse order
		for (int i = comps.length - 1; i >= 0; i--) {
			// Check if current component is a progress bar
			if (comps[i] instanceof JProgressBar) {
				JProgressBar pbar = (JProgressBar) comps[i]; // Safe cast to progress bar type

				// Match progress bar for lifecycle phase tracking
				if (pbar.getString().equalsIgnoreCase("Lifecycle")) {
					_lifecyclePbar = pbar; // Store reference to lifecycle-specific progress bar
					_lifecyclePbar.setString("Ensemble Count"); // Update label text to show count of ensembles
					break; // Exit loop once correct progress bar found
				}
			}
		}

		Map<EnsembleSet, int[]> esetMap = new HashMap<>(); // Create map to hold ensemble set keyed by member array
		EnsembleSet eset; // Declare loop variable for current ensemble set
		int[] members; // Variable for member index array from configuration
		String memberSet; // Variable for configuration string defining member indices

		// Iterate through each configured ensemble set
		for (int i = 0; i < _selectedESets.size(); i++) {
			eset = _selectedESets.get(i); // Get current ensemble set from collection
			memberSet = eset.getMemberSetToCompute(); // Retrieve configuration string for this set
			members = RunForecastSimulationAction.getIntegerSet(memberSet); // Parse comma-separated list into integer array

			// Validate that member array exists and is non-empty
			if (members == null || members.length == 0) {
				return false; // Return failure if no valid members configured for this set
			}

			esetMap.put(eset, members); // Store ensemble set with its member array in lookup map
		}

		Map.Entry<EnsembleSet, int[]> esetEntry; // Declare variable to hold map entry during iteration
		Set<Map.Entry<EnsembleSet, int[]>> esetSet = esetMap.entrySet(); // Get all entries from constructed map
		Iterator<Map.Entry<EnsembleSet, int[]>> esetIter = esetSet.iterator(); // Create iterator for sequential traversal

		int esetIdx = 0; // Counter for tracking current ensemble set being processed
		_memberIdx = 0; // Reset member index counter before processing all sets

		// Loop through each ensemble set in map
		while (esetIter.hasNext()) {
			esetEntry = esetIter.next(); // Get next entry from iterator
			_members = esetEntry.getValue(); // Extract member array from current entry
			eset = esetEntry.getKey(); // Get ensemble set object from key

			// If lifecycle progress bar was successfully retrieved earlier
			if (_lifecyclePbar != null) {
				final EnsembleSet fEset = eset; // Capture reference for use in lambda expression
				EventQueue.invokeLater(() -> _lifecyclePbar.setString(fEset.getName())); // Schedule UI update on event thread
			}

			int[] ensembleNums = _simGroup.getEnsembleSetCollectionIndexing(_sim, eset); // Get collection indices from simulation group

			// Call helper to process current ensemble set
			if (!ensembleCompute(eset, _members, ensembleNums[0])) {
				_computeProgressPanel.computeComplete(false); // Mark compute as complete with failure status on progress panel
				return false; // Return failure immediately after reporting error
			}

			esetIdx++; // Increment ensemble set counter
		}

		_sim.setRealizationPosition(_memberCnt); // Set final realization position indicator for UI
		_computeProgressPanel.computeComplete(true); // Mark compute as complete with success status

		return true; // Return success after all sets processed without errors
	}

	// Private helper processes single ensemble set
	private boolean ensembleCompute(EnsembleSet eset, int[] members, int outputCollectionStart) {

		// Validate member array before processing
		if (members == null || members.length == 0) {
			_sim.addErrorMessage("No Ensemble Members selected to compute"); // Log error message with no members found
			_sim.computeComplete(false); // Mark simulation as complete with failure status
			return false; // Return failure indicator
		}

		// check to see if any of the ensemble members need computed
		boolean doCompute = false; // Flag tracking whether compute is needed for this set

		// If not configured to recompute all data, check for new members
		if (!_recomputeAll) {

			// Loop through member indices in array
			for (int m = 0; m < members.length; m++) {
				// Check if this member has already been marked as computed
				if (!eset.getComputedMembers().contains(members[m])) {
					doCompute = true; // Mark that compute is needed for this set
					break; // Exit loop early after finding first uncomputed member
				}
			}

		} else {
			// If configured to always recompute all data
			doCompute = true; // Always enable compute when recomputeAll flag is true
		}

		// Only proceed if new members exist requiring computation
		if (!doCompute) {
			_computeProgressPanel.addMessage("No Ensemble Members for " + eset + " need computed."); // Add informational message to progress panel
			return true; // Return success since no work needed but operation completed safely
		}

		List<DSSIdentifier> savedDssPaths = new ArrayList<>(); // Initialize empty list to store saved BC paths

		// Attempt pre-compute operations including loading BC/IC/TT config files
		if (!preCompute(eset, members, savedDssPaths)) {
			return false; // Return failure if pre-comute setup failed
		}

		ComputeProgressListener progressListener = _sim.getComputeProgressListener(); // Get primary compute listener reference

		List<ComputeProgressListener> listeners = null; // Initialize list to hold secondary listeners if they exist

		// Check for enhanced listener interface capability
		if (progressListener instanceof ComputeProgressListener2) {
			ComputeProgressListener2 pl2 = (ComputeProgressListener2) progressListener; // Safe cast to enhanced listener type
			listeners = pl2.getListeners(); // Extract secondary listeners list
		}

		// Attempt main processing within try-catch block for error isolation
		try {

			// copy in the new iteration data
			// Show UI update only in debug mode
			if (_debug) {
				JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in boundary condition data "); // Display message dialog
			}

			// Attempt to copy boundary condition data before compute
			if (!copyBcDssData()) {
				return false; // Fail process if BC copy operation failed
			}

			// Attempt to copy initial condition data before compute
			if (!copyICDssData()) {
				return false; // Fail process if IC copy operation failed
			}

			int currentMember; // Variable to track current iteration member index

			// Loop through each member to be processed in this set
			for (int m = 0; m < _members.length; m++) {
				_computeProgressPanel.setRealizationPosition(_memberIdx); // Update progress panel realization counter
				currentMember = _members[m]; // Get current member number from array

				// Check if this member already computed and not recompute-all
				if (eset.getComputedMembers().contains(currentMember) && !_recomputeAll) {
					_computeProgressPanel.addMessage("Ensemble Member " + currentMember + " already computed, skipping"); // Log skip message
					continue; // Skip processing this member iteration
				}

				_computeProgressPanel.addMessage("Computing Ensemble Member " + currentMember); // Update progress message with current member index
				_computeProgressPanel.addMessage("Output will be saved  to F-Part C:" + String.format("%06d", outputCollectionStart + currentMember)); // Log destination collection path
				System.out.println("Computing Ensemble Member " + currentMember + " for " + _sim); // Print status to console for debugging

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Pre Scripts for ensemble member " + currentMember); // Display message dialog
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
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in temp target data for Ensemble member " + currentMember); // Display message dialog
				}

				// Attempt to copy temperature target data
				if (!copyTempTargetMember(eset, currentMember)) {
					return false; // Fail process if TT copy operation failed
				}

				// Attempt to copy temperature target control location data
				if (!copyTempTargetControlLocs()) {
					return false; // Fail process if control loc copy failed
				}

				// Attempt to write current member index to tracking file
				if (!writeCurrentMember(currentMember)) {
					return false; // Fail process if write operation failed
				}

				// Check cancellation flag after write operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// close any DSS files we might have had open to prevent file handle leaks during switch
				HecDSSFileDataManager dm = new HecDSSFileDataManager(); // Create manager instance for cleanup
				dm.closeAllFiles(); // Ensure all previous DSS files are closed cleanly
				_sim.setRecomputeAll(true); // Set flag on simulation object to force full recalculation

				// Compute the simulation with the new copied data now in place
				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Computing Ensemble member " + currentMember); // Display message dialog
				}

				// Check if simulation engine reported success for compute call
				if (!_sim.compute()) {
					// If configured to continue on error, skip this iteration
					if (Boolean.getBoolean("ActionComputable.ContinueOnError")) {
						continue; // Proceed to next member without logging specific failure
					}

					_computeProgressPanel.addErrorMessage("Simulation " + _sim.getName() + " failed to compute"); // Log error message to progress panel

					return false; // Return failure if stop-on-error configured or no continue flag

				} else {
					// If simulation computed successfully without errors
					eset.addComputedMember(currentMember); // Mark member as computed in ensemble set metadata
					ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationPanel().addComputedMember(_sim, eset, currentMember); // Notify UI component of new computation
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
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying results for ensemble member " + currentMember); // Display message dialog
				}

				copyDssResultsToCollectionsDss(currentMember, outputCollectionStart); // Copy computed outputs to collection storage

				// Check cancellation flag after copy operation
				if (_canceled) {
					return false; // Return failure if user cancelled between steps
				}

				// Show UI update only in debug mode
				if (_debug) {
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Post Scripts ensemble member " + currentMember); // Display message dialog
				}

				// Execute post-compute scripts to process outputs
				if (!runPostScripts(currentMember)) {
					return false; // Fail process if post-scripts failed
				}

				_memberIdx++; // Increment current member index for progress tracking
			}

		} catch (Exception e) {
			_computeProgressPanel.addErrorMessage("Exception during Ensemble compute " + e); // Log error message to progress panel
			Logger.getLogger(ForecastActionComputable.class.getName()).warning("Exception during Ensemble compute " + e); // Log warning with class name
			e.printStackTrace(); // Print stack trace for debugging

			return false; // Return failure on exception

		} finally {
			// restore the saved off DSS paths
			// Show UI update only in debug mode
			if (_debug) {
				JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Restoring original DSS data"); // Display message dialog
			}

			// Check if save records property is enabled
			if (Boolean.getBoolean(SAVE_DSS_RECORDS_PROP)) {
				restoreDssPaths(savedDssPaths); // Restore original backed-up files to their paths
			}

			_preCodeMap.clear(); // Clear cached pre-scripts after compute session ends to free memory
			_postCodeMap.clear(); // Clear cached post-scripts to prevent conflicts in next compute session

			// Remove any secondary listeners added during process
			for (int l = 0; l < listeners.size(); l++) {
				_sim.removeComputeProgressListener(progressListener); // Clean up listener references on simulation
			}
		}

		return true; // Return success if all steps completed without fatal errors

	}

	// Private helper writes member index to tracking file
	private boolean writeCurrentMember(int currentMember) {
		_computeProgressPanel.setStatusMessage("Writing current ensemble member..."); // Update status message in progress panel
		boolean success = true; // Flag tracking whether write operation succeeds

		String runDir = Project.getCurrentProject().getAbsolutePath(_sim.getRunDirectory()); // Get absolute path to simulation run directory
		Path currentEnsembleFile = Paths.get(runDir).resolve(CURRENT_ENSEMBLE_FILE_NAME); // Construct full file path with tracker name

		try {
			// Create the file if it doesn't exist, truncate it if it does
			Files.write(currentEnsembleFile, String.valueOf(currentMember).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); // Write single byte integer value to file

			_computeProgressPanel.setStatusMessage("Wrote current ensemble member " + currentMember + " to " + currentEnsembleFile); // Update status message with success confirmation
		} catch (IOException e) {
			_sim.addErrorMessage("Failed to write current ensemble member " + currentMember + " to file " + currentEnsembleFile); // Log high level error message
			LOGGER.atWarning().withCause(e).log("Failed to write current ensemble member " + currentMember + " to file " + currentEnsembleFile); // Log warning with cause detail
			success = false; // Set success flag to indicate operation failure
		}

		return success; // Return final status result of write operation
	}

	// Private helper copies temperature target control location data records
	private boolean copyTempTargetControlLocs() {
		_computeProgressPanel.setStatusMessage("Copying Temperature Targets Control Location Data..."); // Update progress panel status message
		boolean copySuccessful = true; // Flag tracking overall success of copy operation

		// Iterate through each mapping in the control location list
		for (TempTargetControlLocsMapping _mapping : _tempTargetControlLocsDssPathMap) {
			String srcDssFile = _mapping.getSourceDssFile(); // Extract source file path from mapping object
			String srcDssPath = _mapping.getSourceDssRecord(); // Extract source record path component
			String destDssFile = _mapping.getDestinationDssFile(); // Extract destination file path from mapping object
			String destDssPath = _mapping.getDestinationDssRecord(); // Extract destination record path component
			DSSIdentifier srcDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(srcDssFile), srcDssPath); // Create source identifier using absolute path and record
			DSSIdentifier destDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(destDssFile), destDssPath); // Create destination identifier

			_sim.addComputeMessage("Copying Temperature Target Control Location pathname from " + srcDssId + " to " + destDssId); // Log progress message
			copySuccessful = copyTextDssRecord(srcDssId, destDssId); // Delegate copy operation to text data specialist method
		}

		return copySuccessful; // Return final result status of control location copy
	}

	// Private helper for text data record copying operations
	private boolean copyTextDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId) {
		DssFileManager fileManager = DssFileManagerImpl.getDssFileManager(); // Get singleton instance of DSS file manager
		TextContainer copyFromData = fileManager.readTextData(srcDssId); // Read text data from source identifier

		int success = -1; // Initialize with error indicator value

		// Check if container exists and has content
		if (copyFromData != null && !copyFromData.getText().isEmpty()) {
			success = fileManager.writeTextData(destDssId, copyFromData.getText()); // Write text data to destination identifier

			// Check if write operation reported an error code
			if (success < 0) {
				_sim.addWarningMessage("Failed to copy Text DSS Record from " + srcDssId + " to " + destDssId); // Log warning message with paths

			}
		} else {
			// Handle case where source has no data to copy
			_sim.addWarningMessage("No data found in DSS Text Record: " + srcDssId); // Log informational warning
		}

		return success == 0; // Return true only if write completed with no errors
	}

	// Private helper performs all pre-compute setup for ensemble set
	private boolean preCompute(EnsembleSet eset, int[] members, List<DSSIdentifier> savedPaths) {
		// save off the original DSS data
		// Show UI update only in debug mode
		if (_debug) {
			JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Saving Original Data "); // Display message dialog
		}

		String prjDir = Project.getCurrentProject().getProjectDirectory(); // Get absolute path to project root directory
		String bcConfigPath = RMAIO.concatPath(prjDir, BC_CONFIG_FILE); // Construct full path to BC configuration file
		_computeProgressPanel.setStatusMessage("Reading Boundary Condition Data.."); // Update progress panel status message

		_bcDssPathMap = new DssPathMap(_sim, bcConfigPath); // Create BC path map object with reference to config file
		BcData bcData = eset.getBcData(); // Retrieve BC data model from ensemble set
		_bcDssPathMap.setSourceFPart(bcData.getFPart()); // Set functional part identifier for BC source records
		_bcDssPathMap.setSourceDssFile(Project.getCurrentProject().getAbsolutePath(bcData.getOutputDssFile().toString())); // Set full path to BC output DSS file

		// Attempt to read and parse BC configuration file
		if (!_bcDssPathMap.readDssPathsFile()) {
			return false; // Return failure if config file parsing failed
		}

		String icConfigPath = RMAIO.concatPath(prjDir, IC_CONFIG_FILE); // Construct full path to IC configuration file
		_computeProgressPanel.setStatusMessage("Reading Initial Condition Data.."); // Update progress panel status message

		_icDssPathMap = new IcPathMap(_sim, icConfigPath, _simGroup.getInitialConditions()); // Create IC path map object with config path and existing initial conditions
		InitialConditions icData = _simGroup.getInitialConditions(); // Get reference to loaded IC data object

		// Attempt to read and parse IC configuration file
		if (!_icDssPathMap.readDssPathsFile()) {
			return false; // Return failure if config file parsing failed
		}

		String ttConfigPath = RMAIO.concatPath(prjDir, TEMP_TARGET_CONFIG_FILE); // Construct full path to TT configuration file
		_computeProgressPanel.setStatusMessage("Reading Temperature Target Data.."); // Update progress panel status message

		_tempTargetDssPathMap = new TempTargetDssPathMap(_sim, ttConfigPath, eset.getTemperatureTargetSet()); // Create temp target path map object
		_tempTargetDssPathMap.setSourceDssFile(eset.getTemperatureTargetSet().getDssOutputPath().toString()); // Set TT output file path
		_tempTargetDssPathMap.setSourceFPart(eset.getTemperatureTargetSet().getFPartWithoutCollection()); // Set functional part identifier

		// Attempt to read and parse TT configuration file
		if (!_tempTargetDssPathMap.readDssPathsFile()) {
			return false; // Return failure if config file parsing failed
		}

		String ttControlLocConfigPath = RMAIO.concatPath(prjDir, TEMP_TARGET_CONTROL_LOCS_FILE); // Construct full path to control location config
		_computeProgressPanel.setStatusMessage("Reading Temperature Target Control Location Data.."); // Update progress panel status message

		// Attempt reading control locations with error handling
		try {
			_tempTargetControlLocsDssPathMap = readTempTargetControlLocsFile(ttControlLocConfigPath, eset.getTemperatureTargetSet()); // Read and parse control location CSV

		} catch (IOException e) {
			_sim.addWarningMessage("Failed to read Temperature Target Control Location file " + ttControlLocConfigPath); // Log warning message
			LOGGER.atWarning().withCause(e).log("Failed to read Temperature Target Control Location file " + ttControlLocConfigPath); // Log exception details
			return false; // Return failure on I/O exception
		}

		// Check if save records property is enabled
		if (Boolean.getBoolean((SAVE_DSS_RECORDS_PROP))) {
			List<DSSIdentifier> savedDssPaths = saveDssPaths(_bcDssPathMap, _tempTargetDssPathMap); // Save original paths from BC and TT maps

			// Check if saving operation returned error condition
			if (savedDssPaths == null) {
				return false; // Return failure on error return value
			}

			savedPaths.addAll(savedDssPaths); // Add newly saved paths to collection passed in
		}

		_preCodeMap.clear(); // Clear cached pre-scripts before new compute session
		_postCodeMap.clear(); // Clear cached post-scripts before new compute session

		return true; // Return success after all pre-compute setup completed
	}

	// Private helper reads control location CSV file
	private List<TempTargetControlLocsMapping> readTempTargetControlLocsFile(String ttControlLocConfigPath, TemperatureTargetSet ttSet) throws IOException {
		List<TempTargetControlLocsMapping> retVal = CsvReader.readCsv(Paths.get(ttControlLocConfigPath), TempTargetControlLocsMapping.class); // Read all mappings from CSV file

		// Process each reading to realize placeholders with actual river location name
		for (TempTargetControlLocsMapping mapping : retVal) {
			String srcRecord = mapping.getSourceDssRecord(); // Extract raw source record string

			// Check if source record was successfully parsed
			if (srcRecord == null) {
				_sim.addWarningMessage("Read Failed. No Source DSS Record specified in config " + ttControlLocConfigPath); // Log warning message
				return new ArrayList<>(); // Return empty list on read failure
			}

			String realizedSrcRecord = srcRecord.replace(TEMP_TARGET_CONTROL_LOC_REPLACE, ttSet.getRiverLocation().getName()); // Replace placeholder token with actual river location name
			mapping.setSourceDssRecord(realizedSrcRecord); // Set updated record string back into mapping object
		}

		return retVal; // Return list of processed mappings

	}


	/**
	 * Placeholder method currently returning true - scripts for post-computation are not executed yet.
	 * Future implementation would use sensitivity settings to retrieve script configurations per model alternative.
	 *
	 * @param iterNum The iteration/member number currently being processed for context in logs
	 * @return Boolean indicating whether post-scripts ran successfully (always true currently)
	 */
	private boolean runPostScripts(int iterNum) {
		return true; // for now // Currently returns success placeholder
	}

	/**
	 * Placeholder method currently returning true - scripts for pre-computation are not executed yet.
	 * Future implementation would use sensitivity settings to retrieve script configurations per model alternative.
	 *
	 * @param iterNum The iteration/member number currently being processed for context in logs
	 * @return Boolean indicating whether pre-scripts ran successfully (always true currently)
	 */
	private boolean runPreScripts(int iterNum) {
		return true; // for now // Currently returns success placeholder
	}

	/**
	 * Iterates through all model alternatives and executes configured scripts if available.
	 * Handles compilation of Python scripts on first encounter and execution via Jython interpreter.
	 * Skips null model alternatives or configurations with empty script files.
	 *
	 * @param computeSettings Configuration object defining which scripts run for each model alternative
	 * @param iterNum         The iteration/member number currently being processed for context in logs
	 * @param isPreCompute    Boolean flag indicating if this is a pre-compute or post-compute phase
	 * @return True if all non-null alternatives executed successfully, false if any failed
	 */
	private boolean runScripts(ComputeSettings computeSettings, int iterNum, boolean isPreCompute) {
		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Retrieve collection of all possible model alternative objects
		ModelAlternative modelAlt; // Declare variable to hold current model alternative in loop

		// Loop through alternatives, stop early if cancelled
		for (int i = 0; i < modelAlts.size() && !_canceled; i++) {
			modelAlt = modelAlts.get(i); // Get specific model alternative instance

			// Check for null reference to prevent crashes
			if (modelAlt == null) {
				continue; // Skip iteration if alternative is missing
			}

			String scriptFile = computeSettings.getScriptFor(modelAlt); // Retrieve the path to script file from settings config

			// Check if no script is configured for this alternative
			if (scriptFile == null || scriptFile.isEmpty()) {
				continue; // Skip execution as there is nothing to run for this model type
			}

			String scriptText = readScriptFile(scriptFile); // Read full content of the script file into memory

			// If reading file failed, return failure immediately
			if (scriptText == null) {
				return false; // Return false indicating script could not be loaded
			}

			// Log information in debug mode only to reduce console noise
			if (_debug) {
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Found Script for " + modelAlt); // Log success finding script path

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
	private String readScriptFile(String scriptFile) {
		String absScriptFile = Project.getCurrentProject().getAbsolutePath(scriptFile); // Convert relative path to absolute using project context
		RmaFile file = FileManagerImpl.getFileManager().getFile(absScriptFile); // Create RmaFile wrapper for target resource

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
			_sim.addErrorMessage("Error reading script file " + absScriptFile + " Error:" + ioe); // Log error to simulation message queue
			Logger.getLogger(ForecastActionComputable.class.getName()).warning("Error reading script file " + absScriptFile + " Error:" + ioe); // Log warning with class name

		} finally {
			// Check if reader object exists before closing
			if (reader != null) {
				// Attempt close operation inside try-catch to prevent secondary errors
				try {
					reader.close(); // Close the buffered reader stream

				} catch (IOException e) {
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
	 * @param script   The source code text of the Python script to execute
	 * @return True if script executed successfully, false on error or compilation failure
	 */
	private boolean runScript(ModelAlternative modelAlt, String script, int iterNum, boolean isPreCompute) {
		// Check if Python interpreter instance exists in object state
		if (_interp == null) {
			// If not present, initialize the Jython interpreter environment first
			if (!initInterp()) {
				return false; // Return failure on initialization error
			}
		}

		PyCode code = (isPreCompute ? _preCodeMap.get(modelAlt) : _postCodeMap.get(modelAlt)); // Get cached bytecode object based on type and model type

		// If no cached bytecode exists, compilation is needed
		if (code == null) {
			_currentScriptText = script; // Store source text for potential error logging
			code = compileCode(script); // Attempt to compile the raw script string into bytecode object

			// Check if compilation returned null (failed)
			if (code == null) {
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Failed to compile " + (isPreCompute ? "precompute" : "postcompute") + "script for " + modelAlt); // Log warning with phase type
				_sim.addErrorMessage("Failed to compile " + (isPreCompute ? "precompute" : "postcompute") + " script for " + modelAlt); // Add user-friendly error message
				return false; // Return failure on compilation error
			}

			// Store compiled code in pre-script cache if this phase is pre-compute
			if (isPreCompute) {
				_preCodeMap.put(modelAlt, code); // Save bytecode for future use without recompilation

			} else {
				// Otherwise store in post-script cache
				_postCodeMap.put(modelAlt, code); // Save bytecode for future use without recompilation
			}
		}

		_sim.addComputeMessage("Running " + (isPreCompute ? "pre-compute" : "post-compute") + " script for " + modelAlt); // Log execution phase to simulation message queue
		boolean rv = runScript(code, modelAlt, iterNum); // Delegate execution of compiled code object
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
	private boolean runScript(PyCode code, ModelAlternative modelAlt, int iterNum) {
		long t1 = System.currentTimeMillis(); // Capture start time for performance profiling

		{ // Code block scope to manage interpreter state and resource cleanup cleanly

			// Log execution in debug mode only
			if (_debug) {
				Logger.getLogger(ForecastActionComputable.class.getName()).info("running Jython Code for " + modelAlt + " iter=" + iterNum); // Log operation details
			}

			hec2.wat.model.ComputeOptions options = _sim.getOptionsForNextCompute(modelAlt, _sim.getRunTimeWindow(), getWatPlugin(modelAlt)); // Get next compute configuration

			modelAlt.setComputeOptions(options); // Apply new options to model alternative state

			PyStringMap locals = new PyStringMap(); // Create dictionary to hold local variables for script execution

			locals.__setitem__("currentIteration", Py.java2py(iterNum)); // Inject Java loop counter into Python namespace
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
						Logger.getLogger(ForecastActionComputable.class.getName()).info("returning " + obj + " from script"); // Log the actual boolean result
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

				Logger.getLogger(ForecastActionComputable.class.getName()).info("runScript:Error running initialization script " + e); // Log error details
				Logger.getLogger(ForecastActionComputable.class.getName()).info("runScript:initialization script is:"); // Prepare log for source code display
				Logger.getLogger(ForecastActionComputable.class.getName()).info(_currentScriptText); // Log the source text associated with failure

				_sim.addErrorMessage("Error running script " + getName() + 's. Error' + e); // Add high-level error message to simulation queue
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
	private static WatPlugin getWatPlugin(ModelAlternative modelAlt) {
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
	private PyCode compileCode(String script) {
		StringBuilder buffer = new StringBuilder(script); // Create buffer for string manipulation
		buffer.append("ret=" + METHOD_SIGNATURE + "\n"); // Append method signature as default return value
		String updatedScript = buffer.toString(); // Get final compiled script string

		// Attempt compilation within try block
		try {

			// Log in debug mode only
			if (_debug) {
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Compiling script:" + updatedScript); // Log the code being compiled
			}

			PyCode pyCode = (new PythonInterpreter()).compile(updatedScript); // Compile via interpreter API and create new object instance

			return pyCode; // Return the resulting bytecode object to caller for caching or execution

		} catch (Exception e) {
			// Catch compilation exceptions thrown by interpreter
			Logger.getLogger(ForecastActionComputable.class.getName()).warning("Python Compilation Error of Script " + updatedScript + " failed " + e); // Log warning with failure context
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
	private boolean initInterp() {
		// Log start of initialization in debug mode only
		if (_debug) {
			Logger.getLogger(ForecastActionComputable.class.getName()).info("initializing Jython Interpreter"); // Log status update
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
					Logger.getLogger(ForecastActionComputable.class.getName()).info("found jythonlib.jar in classpath" + token); // Log discovery of required library
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
			Logger.getLogger(ForecastActionComputable.class.getName()).info("initInterp(): creating interpreter took " + (System.currentTimeMillis() - t1) + " ms"); // Report time taken to instantiate
		}

		return true; // Return success on initialization completion
	}

	/**
	 * Saves the paths of original DSS records before they are potentially overwritten.
	 * Iterates through all model alternatives and their data locations to rename records
	 * with "-save" suffix so they can be restored later if needed for ensemble generation.
	 *
	 * @param bcDssPathMap         Boundary condition path map from which paths are saved
	 * @param tempTargetDssPathMap Temperature target path map from which paths are saved
	 * @return List of identifiers pointing to the saved (temporarily renamed) DSS records, null if save failed
	 */
	private List<DSSIdentifier> saveDssPaths(DssPathMap bcDssPathMap, DssPathMap tempTargetDssPathMap) {
		_sim.addComputeMessage("Saving BC DSS records ..."); // Log progress of saving phase
		List<DSSIdentifier> pathsRenamed = new ArrayList<>(); // Create empty list to collect saved identifiers
		saveDssPaths(pathsRenamed, bcDssPathMap); // Save paths from boundary condition map

		_sim.addComputeMessage("Saving Temp Target DSS records ..."); // Log progress of saving phase
		saveDssPaths(pathsRenamed, tempTargetDssPathMap); // Save paths from temperature target map
		_sim.addComputeMessage("Saved " + pathsRenamed.size() + " DSS records ..."); // Log summary of saved records count

		return pathsRenamed; // Return collection of renamed identifiers to restore later
	}

	// Private helper saves BC or TT paths
	private void saveDssPaths(List<DSSIdentifier> pathsRenamed, DssPathMap dssPathMap) {
		// dest to source map
		Map<DSSIdentifier, DSSIdentifier> copyMap = dssPathMap.getAllDssMap(); // Get destination-to-source mapping
		Set<DSSIdentifier> destSet = copyMap.keySet(); // Extract keys (destination identifiers) from map
		Iterator<DSSIdentifier> destIter = destSet.iterator(); // Create iterator for sequential traversal

		Set<DSSIdentifier> copyIds = new HashSet(); // Initialize set to track unique identifiers processed
		DSSIdentifier destDssId; // Declare variable for identifier in current iteration

		// Find unique destination identifiers without duplicates
		while (destIter.hasNext()) {
			destDssId = destIter.next(); // Get next destination identifier from iterator
			copyIds.add(destDssId); // Add to set to mark as processed and prevent duplicates
		}

		destIter = copyIds.iterator(); // Reset iterator on deduplicated set

		// Iterate through unique destinations
		while (destIter.hasNext()) {
			destDssId = destIter.next(); // Get next identifier
			destDssId = saveDssPath(destDssId); // Call helper to rename record temporarily

			// If renaming was successful, add to collection
			if (destDssId != null) {
				pathsRenamed.add(destDssId); // Add identifier to tracking list
			}
		}
	}

	/**
	 * Saves a specific DSS record path by renaming it with the save suffix.
	 * Only applies if data location is linked directly to the simulation's main DSS file.
	 *
	 * @param srcDssId The source identifier for the record being saved
	 * @return DSSIdentifier representing the saved record, or null if not applicable
	 */
	private DSSIdentifier saveDssPath(DSSIdentifier srcDssId) {
		Vector<String> srcList = new Vector<>(); // Prepare list for source paths before modification
		Vector<String> destList = new Vector<>(); // Prepare list for destination paths after renaming

		String dssPath = srcDssId.getDSSPath(); // Extract directory path from identifier object
		String dssFile = srcDssId.getFileName(); // Extract filename from identifier object
		String dssFileAbs = Project.getCurrentProject().getAbsolutePath(dssFile); // Convert file reference to absolute system path
		srcDssId.setFileName(dssFileAbs); // Set absolute filename back on source identifier for consistent use

		fillInSrcAndDestList(dssFileAbs, dssPath, srcList, destList, true); // Populate source and destination lists with suffix added

		HecTime[] timeWindow = DssFileManagerImpl.getDssFileManager().getTSTimeRange(srcDssId, 0); // Get time window for this record

		// Validate that valid time range was retrieved
		if (timeWindow != null && timeWindow.length == 2) {
			_sim.addComputeMessage("Renaming " + srcDssId + " for time window " + timeWindow[0] + " to " + timeWindow[1]); // Log renaming operation details
		}

		// Check that records were found in source file
		if (srcList.size() > 0) {
			LOGGER.atInfo().log("Found " + srcList + " records for " + dssPath + " in " + dssFileAbs + " copying to " + destList); // Log detailed path information
			_sim.addComputeMessage("Found " + srcList + " records for " + dssPath + " in " + dssFileAbs); // Add user-facing progress message

			int rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFileAbs, srcList, destList); // Execute rename operation on file system

			// Verify that all requested source records were renamed
			if (rv == srcList.size())  {
				DSSIdentifier dssId = new DSSIdentifier(dssFileAbs, dssPath); // Create identifier with original path
				return dssId; // Return success identifier to caller

			} else {
				LOGGER.atWarning().log("Failed to save off all DSS records for " + dssPath + ".  Expected to save " + srcList.size() + " saved " + rv); // Log warning about mismatch between expected and actual saves
				_sim.addWarningMessage("Failed to save off all DSS records for " + dssPath + ".  Expected to save " + srcList.size() + " saved " + rv); // Add user-facing error message
			}
		} else {
			// If no records found in source file
			LOGGER.atInfo().log("Found no records for " + dssPath + " in " + dssFileAbs + " to save off"); // Log informational message about missing data
		}

		return null; // Return null if renaming failed or no paths found
	}

	/**
	 * Fills in source and destination lists for DSS rename operation.
	 * Searches for records by DSS path string and applies suffix logic to destList based on flag.
	 * Adds all found paths to source list and computes corresponding destination paths.
	 *
	 * @param dssFile       Absolute path to the DSS file containing records
	 * @param dssPath       The base pathname string used to identify records within file
	 * @param srcList       Collection to receive original record paths found in file
	 * @param destList      Collection to receive modified paths (either with -save suffix or without)
	 * @param addSaveSuffix Boolean flag indicating whether to append -save suffix
	 */
	private static void fillInSrcAndDestList(String dssFile, String dssPath, List<String> srcList, List<String> destList, boolean addSaveSuffix) {
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
	private static List<String> findPathnamesFor(String dssFile, String dssPath) {
		DSSPathname pathname = new DSSPathname(); // Create object for defining search criteria
		pathname.setPathname(dssPath); // Set base path in object
		pathname.setDPart("*"); // Specify wildcard for dataset (time) and step variables

		DSSIdentifier dssId = new DSSIdentifier(dssFile, pathname.getPathname()); // Create identifier combining file and search pattern
		Vector pathnames = DssFileManagerImpl.getDssFileManager().searchDSSPaths(dssId); // Query manager to find matching records

		return pathnames; // Return vector of matching pathnames from search result
	}

	/**
	 * Copies boundary condition data from source DSS files to destination locations for ensemble computation.
	 * Iterates through BC paths map and uses copy operations based on configuration. Returns success status after all copies.
	 *
	 * @return True if all BC records copied successfully, false if any failed or operation was cancelled
	 */
	private boolean copyBcDssData() {
		_computeProgressPanel.setStatusMessage("Copying Boundary Condition Data ..."); // Update progress panel status message
		DSSIdentifier srcDssId; // Variable for source identifier in iteration
		boolean copySuccessful = true; // Flag tracking overall success of copy operation

		HecTime[] times = getCopyTimeWindow(); // Get time window for copying (null if no restrictions)

		_sim.addComputeMessage("Copying over Boundary Condition records..."); // Log progress message

		// Copy the boundary condition data from source to destinations according to BC paths map
		Map<DSSIdentifier, DSSIdentifier> copyMap = _bcDssPathMap.getDssCopyMap(); // Get destination-to-source mapping
		Set<Map.Entry<DSSIdentifier, DSSIdentifier>> copyMapSet = copyMap.entrySet(); // Extract entries for iteration
		Iterator<Map.Entry<DSSIdentifier, DSSIdentifier>> copyMapIter = copyMapSet.iterator(); // Create iterator for sequential traversal

		String fullPath; // Variable to hold absolute path string for destination file

		// Iterate through each source-destination pair in map
		while (copyMapIter.hasNext()) {
			Map.Entry<DSSIdentifier, DSSIdentifier> copyMapElement = copyMapIter.next(); // Get next entry from set
			srcDssId = copyMapElement.getValue(); // Extract source identifier from entry

			// Apply time restriction if one was specified
			if (times != null) {
				srcDssId.setStartTime(times[0]); // Set start time on source for read operation
				srcDssId.setEndTime(times[1]); // Set end time on source for read operation
			}

			DSSIdentifier destDssId = copyMapElement.getKey(); // Extract destination identifier from entry
			fullPath = Project.getCurrentProject().getAbsolutePath(destDssId.getFileName()); // Get absolute path to destination file
			destDssId.setFileName(fullPath); // Update destination with absolute reference
			copySuccessful |= copyTsDssRecord(srcDssId, destDssId); // Delegate copy operation to time series specialist method
		}

		return copySuccessful; // Return final status of boundary condition copy
	}

	/**
	 * Copies initial condition data from source DSS files to destination reservoir locations.
	 * Iterates through reservoir list and copies each IC record according to configuration mapping.
	 * Returns true if all copies completed or no records needed copying, false if any failed.
	 *
	 * @return True if all IC records copied successfully, false if copy operations failed
	 */
	private boolean copyICDssData() {
		_computeProgressPanel.setStatusMessage("Copying Initial Condition Data ..."); // Update progress panel status message

		boolean copySuccessful = true; // Flag tracking overall success of copy operation

		_sim.addComputeMessage("Copying over Initial Condition records..."); // Log progress message

		// Copy the initial condition data from source to destinations according to IC paths map
		InitialConditions icData = _simGroup.getInitialConditions(); // Get reference to loaded IC data object
		List<String> reservoirs = icData.getReservoirs(); // Extract list of reservoir names configured for ICs

		String reservoir; // Variable for current reservoir in iteration
		List<DSSIdentifier> destDssIds; // List of destination identifiers for current reservoir
		DSSIdentifier destDssId; // Variable for identifier in inner loop

		// Iterate through each configured reservoir location
		for (int r = 0; r < reservoirs.size(); r++) {
			reservoir = reservoirs.get(r); // Get name of current reservoir from collection
			DSSIdentifier srcDssId = _icDssPathMap.getSourceDSSIdentifierFor(reservoir); // Get source identifier for this reservoir from map

			// Check if source identifier was found in configuration
			if (srcDssId == null) {
				_sim.addWarningMessage("No source DSS record found for " + reservoir + "'s Initial Conditions"); // Log warning message
				continue; // Skip to next reservoir on missing source data
			}

			// Check if source file path is already absolute
			if (!RMAIO.isFullPath(srcDssId.getFileName())) {
				srcDssId.setFileName(Project.getCurrentProject().getAbsolutePath(srcDssId.getFileName())); // Convert to absolute if not
			}

			destDssIds = _icDssPathMap.getDestDssIdentifiersFor(reservoir); // Get list of destination identifiers for this reservoir

			// Check if any destinations defined for this reservoir
			if (destDssIds.isEmpty()) {
				_sim.addWarningMessage("No DSS Destination defined for " + reservoir + "'s Initial Conditions"); // Log warning message
				continue; // Skip to next reservoir on missing destinations
			}

			_sim.addComputeMessage("Copying over Initial Condition record for " + reservoir); // Log progress message

			// Iterate through all destinations for this source
			for (int d = 0; d < destDssIds.size(); d++) {
				destDssId = destDssIds.get(d); // Get destination identifier from list

				// Check if destination file path is already absolute
				if (!RMAIO.isFullPath(destDssId.getFileName())) {
					destDssId.setFileName(Project.getCurrentProject().getAbsolutePath(destDssId.getFileName())); // Convert to absolute if not
				}

				copySuccessful |= copyPdDssRecord(srcDssId, destDssId); // Delegate copy operation to paired data specialist method

			}
		}

		return copySuccessful; // Return final status of initial condition copy
	}

	/**
	 * Checks if a DSSIdentifier is missing required information (file name or path) that would prevent copying.
	 * Returns true if identifier is null, has empty/missing filename, missing path, or placeholder value.
	 *
	 * @param srcDssId The identifier to validate for completeness
	 * @return True if identifier is incomplete or invalid, false if all fields populated properly
	 */
	private boolean missingDssIdentifier(DSSIdentifier srcDssId) {
		if (srcDssId == null || // Check if reference itself is missing
				srcDssId.getFileName() == null || srcDssId.getFileName().trim().isEmpty() || // Check if filename is missing or empty
				srcDssId.getDSSPath() == null || srcDssId.getDSSPath().trim().isEmpty()) // Check if path is missing or empty
		{
			return true; // Return failure indicator for incomplete identifier
		}

		// Check for sentinel/placeholder path value
		if ("///////".equals(srcDssId.getDSSPath())) {
			return true; // Return failure for placeholder path
		}

		return false; // Return success for complete, valid identifier
	}

	/**
	 * Copies paired data records (multiple variables at same location/time) from source to destination.
	 * Handles type mismatch errors by deleting conflicting data first then writing new records.
	 * Returns true if all paired data copied successfully or container was empty without error.
	 *
	 * @param srcDssId  Source identifier containing paired data to copy
	 * @param destDssId Destination identifier where data should be written
	 * @return True if all pairs copied successfully, false on write errors
	 */
	private boolean copyPdDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId) {
		boolean copySuccessful = true; // Flag tracking overall success of copy operation
		PairedDataContainer srcPdc = DssFileManagerImpl.getDssFileManager().readPairedDataContainer(srcDssId); // Read paired data from source identifier

		// Check if container exists and has actual data
		if (srcPdc != null && srcPdc.numberOrdinates > 0) {
			_sim.addMessage("Copying " + srcDssId + " to " + destDssId); // Log operation progress message

			srcPdc.fileName = destDssId.getFileName(); // Set filename on container for output tracking
			srcPdc.fullName = destDssId.getDSSPath(); // Set full path on container for output tracking
			int rv = DssFileManagerImpl.getDssFileManager().write(srcPdc); // Write paired data to destination file

			// Check if write operation reported error code
			if (rv != 0) {
				copySuccessful = false; // Mark overall success flag as failed
				LOGGER.atWarning().log("Failed to write PDC DSS record for " + destDssId + " to " + srcPdc.fileName + " : " + srcPdc.fullName + " rv=" + rv); // Log warning with file details
				_sim.addErrorMessage("Failed to write PDC DSS record for " + destDssId + " to " + srcPdc.fileName + " : " + srcPdc.fullName + " rv=" + rv); // Add high level error message
			} else {
				// If write succeeded with no errors
				DSSPathname pathname = new DSSPathname(); // Create object for modifying full name path

				/// Save off the source DSS data into the collection DSS file with functional part appended with -Forecast suffix

				srcPdc.fileName = getCollectionsOutputDssFile("forecastResults.dss"); // Get collection file path

				pathname.setPathname(srcPdc.fullName); // Set full name on pathname object for modification
				pathname.setFPart(pathname.getFPart() + "-FORECAST"); // Append -FORECAST suffix to functional part
				srcPdc.fullName = pathname.getPathname(); // Update container full name

				rv = DssFileManagerImpl.getDssFileManager().write(srcPdc); // Write data to collection file
				_sim.addComputeMessage("   Copied " + srcDssId + " to " + destDssId); // Log completion message
			}
		} else {
			// If container was empty or failed to read
			LOGGER.atWarning().log("Copying PDC Record, No data found to copy for " + srcDssId); // Log warning about missing paired data
			_sim.addErrorMessage("Copying PDC Record, No Data Found for " + srcDssId); // Add error message to simulation log
		}

		return copySuccessful; // Return final status of paired data copy

	}

	/**
	 * Copies time series records from source DSS file to destination DSS file.
	 * Handles type mismatch errors by deleting conflicting data first then writing new records.
	 * Returns true if all TS records copied successfully, false on write errors.
	 *
	 * @param srcDssId  Source identifier containing time series data to copy
	 * @param destDssId Destination identifier where data should be written
	 * @return True if all time series copied successfully, false on write errors
	 */
	private boolean copyTsDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId) {
		boolean copySuccessful = true; // Flag tracking overall success of copy operation
		TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true); // Read time series data from source identifier

		// Check if container exists and has actual data
		if (srcTsc != null && srcTsc.numberValues > 0) {
			_sim.addMessage("Copying " + srcDssId + " to " + destDssId); // Log operation progress message

			srcTsc.fileName = destDssId.getFileName(); // Set filename on container for output tracking
			srcTsc.fullName = destDssId.getDSSPath(); // Set full path on container for output tracking
			int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write time series data to destination file

			// Check if error code indicates type mismatch
			if (rv == DSS_WRITE_TYPE_MISMATCH_ERROR_CODE) {
				Vector<String> pathnamesToDelete = new Vector<>(); // Create list for conflicting paths
				Vector destPaths = DssFileManagerImpl.getDssFileManager().searchDSSPaths(destDssId); // Search destination for existing records

				// Iterate over all found paths
				for (Object destPath : destPaths) {
					pathnamesToDelete.add((String) destPath); // Add to deletion list
				}

				int deleteResult = DssFileManagerImpl.getDssFileManager().delete(destDssId.getFileName(), pathnamesToDelete); // Delete conflicting records

				// Check if delete operation failed
				if (deleteResult != 0) {
					LOGGER.atWarning().log("Failed to delete TS DSS records for " + destDssId + " rv=" + deleteResult); // Log warning about delete failure
					_sim.addErrorMessage("Failed to delete TS DSS records for " + destDssId + " rv=" + deleteResult); // Add error message to simulation log
				}

				rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Retry write after cleanup
			}

			// Check if write operation reported error code
			if (rv != 0) {
				copySuccessful = false; // Mark overall success flag as failed
				LOGGER.atWarning().log("Failed to write TS DSS record for " + destDssId + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Log warning with file details
				_sim.addErrorMessage("Failed to write TS DSS record for " + destDssId + " to " + srcTsc.fileName + " : " + srcTsc.fullName + " rv=" + rv); // Add high level error message

			} else {
				// If write succeeded with no errors
				DSSPathname pathname = new DSSPathname(); // Create object for modifying full name path

				/// Save off the source DSS data into the collection DSS file with functional part appended with -Forecast suffix

				srcTsc.fileName = getCollectionsOutputDssFile("forecastResults.dss"); // Get collection file path
				pathname.setPathname(srcTsc.fullName); // Set full name on pathname object for modification
				pathname.setFPart(pathname.getFPart() + "-FORECAST"); // Append -FORECAST suffix to functional part
				srcTsc.fullName = pathname.getPathname(); // Update container full name

				rv = DssFileManagerImpl.getDssFileManager().write(srcTsc); // Write data to collection file
				_sim.addComputeMessage("   Copied " + srcDssId + " to " + destDssId); // Log completion message
			}
		} else {
			// If container was empty or failed to read
			// Check if time window information is missing entirely
			if (srcDssId.getTimeWindow() == null) {
				LOGGER.atWarning().log("No data found to copy for " + srcDssId); // Log warning about missing TS data
				_sim.addErrorMessage("No Data Found for " + srcDssId); // Add error message to simulation log

			} else {
				// Time window exists but no data values found in it
				LOGGER.atWarning().log("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime()); // Log warning with time bounds
				_sim.addErrorMessage("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime()); // Add error message to simulation log
			}
		}

		return copySuccessful; // Return final status of time series copy

	}

	/**
	 * Returns time window boundaries for copying DSS records based on system property configuration.
	 * If forecast.onlyCopyTimeWindow is true, computes start/end times by applying day offsets to simulation runtime window.
	 * Otherwise returns null indicating full time range should be copied.
	 *
	 * @return Array containing [start time, end time] objects if restricted copy mode active, null otherwise
	 */
	private HecTime[] getCopyTimeWindow() {
		// Check if configuration property enables time-restricted copying
		if (Boolean.getBoolean("Forecast.OnlyCopyTimeWindow")) {
			HecTime[] times = new HecTime[2]; // Create array to hold start and end time objects

			RunTimeWindow rtw = _sim.getRunTimeWindow(); // Get simulation runtime window reference
			times[0] = (HecTime) rtw.getStartTime().clone(); // Clone start time for modification
			times[1] = (HecTime) rtw.getEndTime().clone(); // Clone end time for modification

			int startDaysToSubtract = -Integer.getInteger("Forecast.StartDaysToSubtract", 0); // Get number of days to subtract from start
			times[0].addDays(startDaysToSubtract); // Apply day adjustment to start time

			int endDaysToAdd = -Integer.getInteger("Forecast.EndDaysToAdd", 0); // Get number of days to add to end
			times[1].addDays(endDaysToAdd); // Apply day adjustment to end time

			return times; // Return computed time window array
		}

		return null; // Return null indicating full time range copy mode
	}

	private boolean copyTempTargetMember(EnsembleSet eset, int currentMember) {
		_computeProgressPanel.setStatusMessage("Copying Temperature Targets Data..."); // Update progress panel status message

		TemperatureTargetSet ttSet = eset.getTemperatureTargetSet(); // Retrieve temperature target set model from ensemble set
		List<DSSPathname> pathnames = ttSet.getDssPathNames(TemperatureTargetTimeStep.REGULAR_HOURLY); // Get list of configured TT record paths

		// Validate that we have enough path entries for current member index
		if (pathnames.size() < currentMember) {
			_sim.addErrorMessage("Temperature Target collection size " + pathnames.size() + " smaller than current member " + currentMember); // Log error message
			return false; // Return failure on insufficient collection entries
		}

		// Adjust zero-based indexing if needed
		if (currentMember > 0) {
			currentMember--; // Decrement to match zero-based list access
		}

		DSSPathname pathname = pathnames.get(currentMember); // Get DSS pathname for this specific member from configured list
		Path filePath = ttSet.getDssOutputPath(); // Retrieve base file path for temperature target data

		List<DSSIdentifier> destDssIdentifiers = _tempTargetDssPathMap.getDestDssIdentifiersFor(pathname.getPathname(), TemperatureTargetTimeStep.REGULAR_HOURLY); // Get destinations from map
		DSSIdentifier srcDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(filePath.toString()), pathname.getPathname()); // Create source identifier with file and path
		HecTime[] times = getCopyTimeWindow(); // Get optional time window restriction
		boolean copySuccessful = true; // Flag tracking overall success of copy operation

		DSSIdentifier destDssId; // Variable for destination identifier in loop

		// Check if any destinations were found in configuration
		if (destDssIdentifiers.size() == 0) {
			_sim.addWarningMessage("No Temperature Targets were found to copy for temp target path " + pathname); // Log warning message
			_sim.addWarningMessage("The temp Target Config file " + TEMP_TARGET_CONFIG_FILE + " source pathnames have to match " + pathname); // Log specific mismatch detail
			return true; // Return success even with no copies made (skipped by design)
		}

		// Iterate through each configured destination for this source path
		for (int i = 0; i < destDssIdentifiers.size(); i++) {
			destDssId = destDssIdentifiers.get(i); // Get next destination identifier from list
			destDssId.setFileName(Project.getCurrentProject().getAbsolutePath(destDssId.getFileName())); // Convert to absolute path
			_sim.addComputeMessage("Copying Temperature Target pathname from " + srcDssId + " to " + destDssId); // Log progress message
			copySuccessful |= copyTsDssRecord(srcDssId, destDssId); // Delegate copy operation
		}

		return copySuccessful; // Return final status of temperature target copy
	}

	/**
	 * Constructs and returns the file path for the forecast results collection DSS file.
	 * Extracts directory from main simulation DSS file and appends '-forecast.dss' suffix.
	 *
	 * @return String path to the collection output file, or derived path using main simulation DSS as base
	 */
	private String getCollectionDssFilename() {
		String dssFile = _sim.getSimulationDssFile(); // Get base simulation DSS filename

		int idx = dssFile.lastIndexOf('.'); // Find position of last dot for filename

		dssFile = dssFile.substring(0, idx); // Extract directory and base name without extension
		dssFile = dssFile.concat("-forecast.dss"); // Append custom suffix to indicate forecast results file

		return dssFile; // Return constructed file path
	}

	/**
	 * Restores all DSS records that were saved in saveDssPaths() to their original locations.
	 * Reverses the renaming logic by deleting saved copies and renaming back originals if needed.
	 *
	 * @param savedDssPaths List of identifiers representing backed-up records to be restored
	 */
	private void restoreDssPaths(List<DSSIdentifier> savedDssPaths) {
		_computeProgressPanel.setStatusMessage("Restoring original records ..."); // Update progress panel status message
		_sim.addComputeMessage("Restoring original " + savedDssPaths.size() + " DSS records ..."); // Log detailed progress message

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

				// Check if delete failed
				if (rv != 0) {
					LOGGER.atWarning().log("Failed to delete DSS records for " + dssFile + ":" + singleDestList.get(0) + " Rv=" + rv); // Log warning with cause and path info
					_sim.addWarningMessage("Failed to delete DSS records for " + dssFile + ":" + singleDestList.get(0)); // Add error message
				}

				_sim.addComputeMessage("Restoring " + singleSrcList + " to " + singleDestList); // Log successful restore message
				rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFile, singleSrcList, singleDestList); // Move saved copy back to original name

				// Check if rename did not match expected count
				if (rv != singleSrcList.size()) {
					LOGGER.atWarning().log("Failed to restore DSS records for " + dssFile + ":" + singleDestList.get(0)); // Log warning with cause and path info
					LOGGER.atWarning().log("Expected " + singleSrcList.size() + " records to be restored. Restored " + rv + " Records."); // Log discrepancy detail

					_sim.addWarningMessage("Failed to restore DSS records for " + dssFile + ":" + singleDestList.get(0)); // Add error message
					_sim.addWarningMessage("Expected " + singleSrcList.size() + " records to be restored. Restored " + rv + " Records."); // Add detailed discrepancy message
				}
			}
		}
	}

	/**
	 * Copies computed results from the simulation DSS file into a collections DSS file.
	 * Calls updateIterationDssWithDssData for each model alternative to ensure all data is captured.
	 *
	 * @param interationId          The member index number of current iteration in the ensemble
	 * @param outputCollectionStart The starting collection sequence number for this ensemble set
	 */
	private void copyDssResultsToCollectionsDss(int interationId, int outputCollectionStart) {
		_sim.addComputeMessage("Saving Computed DSS records to collections"); // Log operation start message

		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList(); // Get collection of all configured alternatives
		ModelAlternative modelAlt; // Declare variable for iteration

		for (int m = 0; m < modelAlts.size() && !_canceled; m++) { // Loop through each alternative
			modelAlt = modelAlts.get(m); // Get current alternative instance

			if (modelAlt == null) { // Check for null reference to avoid crash
				continue; // Skip if alternative object is missing
			}

			updateIterationDssWithDssData(modelAlt, interationId + outputCollectionStart); // Update each file individually with calculated collection ID
		}
	}

	/**
	 * Copies output records from the simulation DSS file into the collection storage.
	 * Uses FPart matching to identify computed results and writes them to collection path.
	 *
	 * @param modelAlt    The specific ModelAlternative object defining where data resides
	 * @param iterationId The calculated collection sequence ID for this ensemble member
	 */
	private boolean updateIterationDssWithDssData(ModelAlternative modelAlt, int iterationId) {
		_sim.addComputeMessage("Saving Computed DSS records to collection " + iterationId + " for " + modelAlt); // Log context of operation

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

		_sim.addComputeMessage("Copying output DSS for " + modelAlt.getProgram() + " model " + modelAlt + " to " + getCollectionDssFilename()); // Log target file information

		DSSPathname pathname = new DSSPathname(); // Create object for building and modifying paths
		Vector<String> destPaths = new Vector<>(srcPaths.size()); // Prepare vector with expected size to avoid resizing

		String path; // Variable for iterating through source paths

		// Loop through each source record
		for (int i = 0; i < srcPaths.size() && !_canceled; i++) {
			path = srcPaths.get(i); // Get next source path
			pathname.setPathname(path); // Set current path string in object
			pathname.setCollectionSequence(iterationId); // Mark as part of ensemble collection member
			destPaths.add(pathname.getPathname()); // Add constructed destination path to list
		}

		String iterDssFile = getCollectionsOutputDssFile(getCollectionDssFilename()); // Get full absolute path for target file
		int rv = copyRecords(dssFile, iterDssFile, srcPaths, destPaths); // Call utility method to copy data between files

		boolean success = rv == srcPaths.size(); // Verify return code matches number of records processed

		// Check if operation reported partial or full failure
		if (!success) {
			_sim.addErrorMessage("Failed to update forecast DSS file with " + modelAlt.getProgram() + " model " + modelAlt + "'s results"); // Log error message
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
	private String getCollectionsOutputDssFile(String dssFileName) {
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
	 * @param fromDssFile The source DSS filename containing records to be copied
	 * @param toDssFile   The target DSS filename where records will be written
	 * @param srcPaths    List of full pathnames (including FPart) in the source file
	 * @param destPaths   List of corresponding full pathnames in the destination file
	 * @return Integer return code indicating number of successfully copied records (matches src size if success)
	 */
	private int copyRecords(String fromDssFile, String toDssFile, Vector<String> srcPaths, Vector<String> destPaths) {
		HecDSSUtilities fromDataManager = new HecDSSUtilities(); // Create utility manager for source file
		fromDataManager.setDSSFileName(fromDssFile); // Set source filename on utility manager object
		HecDataManager toDataManager = new HecDataManager(toDssFile); // Create utility manager for target file

		_sim.addComputeMessage("Copying records from " + fromDssFile); // Log operation start message

		int rv = fromDataManager.copyRecordsFrom(toDataManager, srcPaths, destPaths); // Execute the cross-file copy command

		return rv; // Return result code from manager
	}

	// Override interface method to return model count for progress bars
	public int getModelCount() {
		return _sim.getModelCount(); // Delegate to underlying simulation object for count
	}

	// Override realization counting interface method
	public int getNumberRealizations() {
		return _memberCnt; // Return total member count calculated during initialization
	}

	// Override lifecycle counting interface method
	public int getNumberLifeCycles() {
		return _memberCnt; // Return same value as realizations (ensemble members)
	}

	// Override initial lifecycle index method
	public int getInitialLifeCycle() {
		return 0; // Return first position index for ensemble processing
	}

	// Override cancellation interface method
	public boolean cancelCompute() {
		_canceled = true; // Set cancellation flag in instance variable
		return _sim.cancelCompute(); // Request cancellation on simulation engine and return its result
	}

	// Override log file path getter method
	public String getLogFile() {
		return _sim.getLogFile(); // Delegate to underlying simulation object for path string
	}

	// Override name getter method for UI display
	public String getName() {
		return _sim.getName(); // Delegate to underlying simulation object for name string
	}

	// Override compute-needed check method
	public boolean needToCompute() {
		return _sim.needToCompute(); // Delegate to underlying simulation object for status check
	}

	// Override standard object representation method
	public String toString() {
		return _sim.getName(); // Return simulation name as default string representation

	}

	// Setter method for dialog reference
	public void setComputeDialog(ComputeProgressDialog computeDialog) {
		// Assign incoming dialog object to instance variable
		_computeDialog = computeDialog;
	}
}