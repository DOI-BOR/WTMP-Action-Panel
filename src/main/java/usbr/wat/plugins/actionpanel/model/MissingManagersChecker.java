package usbr.wat.plugins.actionpanel.model;

import java.util.List;          // Ordered collection interface for lists of file paths returned by the file manager
import java.util.logging.Logger; // JDK logger for recording when managers are re-added to the project

import com.rma.io.FileManagerImpl; // RMA file manager for listing files in project sub-directories
import com.rma.io.RmaFile;         // RMA file abstraction used when setting the manager's file reference
import com.rma.model.Manager;      // Base interface for all RMA managed data objects registered in a project
import com.rma.model.ManagerProxy; // Lightweight proxy used to check whether a file is already known to the project
import com.rma.model.Project;      // Represents the currently loaded RMA project and its manager registry

import hec2.wat.model.WatAlternative;          // WAT alternative manager type corresponding to .walt files
import hec2.wat.model.WatAnalysisPeriod;       // WAT analysis period manager type corresponding to .wap files
import hec2.wat.model.WatSimulation;           // WAT simulation manager type corresponding to .simulation files
import hec2.wat.model.WatSimulationContainer;  // WAT simulation container manager type corresponding to .container files

import rma.util.RMAFilenameFilter; // RMA file filter for restricting directory listings to a specific extension
import rma.util.RMAIO;             // RMA I/O utility for path concatenation

/**
 * Utility class that scans the study's WAT sub-directory structure for manager
 * files that exist on disk but are not registered in the project's .sty file.
 *
 * This situation can occur after a Git download that restores files without
 * updating the project descriptor. The checker detects and re-adds the following
 * manager types:
 *
 *   - WatAnalysisPeriod   (.wap files in wat/aps/)
 *   - WatAlternative      (.walt files in wat/alts/)
 *   - WatSimulation       (.simulation files in wat/sims/)
 *   - WatSimulationContainer (.container files in wat/sims/)
 *   - SimulationGroup     (.simgrp files in wat/simGroups/)
 *
 * Each missing manager is instantiated, associated with its file, and loaded via
 * readData(). Successfully loaded managers are added back to the project registry.
 */
public class MissingManagersChecker {
	// Sub-directory name for WAT simulation files relative to the WAT directory
	private static final String SIMS_FOLDER = "sims";

	// Sub-directory name for WAT alternative files relative to the WAT directory
	private static final String ALTS_FOLDER = "alts";

	// Sub-directory name for WAT analysis period files relative to the WAT directory
	private static final String APS_FOLDER = "aps";

	// Top-level WAT directory name within the project directory
	private static final String WAT_DIR = "wat";

	// Sub-directory name for simulation group files relative to the WAT directory
	private static final String SIM_GROUPS_FOLDER = "simGroups";

	// The project being checked; set by checkForMissingManagers() before any sub-checks run
	private Project _project;

	// Logger for recording when managers are discovered and re-added
	private Logger _logger = Logger.getLogger(MissingManagersChecker.class.getName());

	/**
	 * Constructs a MissingManagersChecker with no project pre-assigned.
	 * <p>
	 * The project is assigned during checkForMissingManagers().
	 */
	public MissingManagersChecker() {
		super();
	}

	/**
	 * Scans the project's WAT sub-directories for manager files that are not registered
	 * in the project and re-adds any that are found.
	 * <p>
	 * If no project is provided, uses the currently active project. Has no effect if the
	 * project is the "no project" placeholder. Runs checks for analysis periods, alternatives,
	 * simulations, simulation containers, and simulation groups in that order.
	 *
	 * @param project the Project to check; if null, uses Project.getCurrentProject()
	 */
	public void checkForMissingManagers(Project project) {
		// Fall back to the currently active project if none was provided
		if (project == null) {
			project = Project.getCurrentProject();
		}
		_project = project;

		// Skip the check if no real project is loaded
		if (_project.isNoProject()) {
			return;
		}

		// Run each sub-check in turn; order follows dependencies (APs before sims, etc.)
		checkForAndAddAnalysisPeriods();
		checkForAndAddAlternatives();
		checkForAndAddSimulations();
		checkForAndAddSimulationGroups();
	}


	/**
	 * Scans the simGroups directory for .simgrp files and re-adds any that are not
	 * already registered in the project.
	 */
	private void checkForAndAddSimulationGroups() {
		String folderToCheck = getWatFolder(SIM_GROUPS_FOLDER);
		RMAFilenameFilter simGroupFilter = new RMAFilenameFilter("simgrp");
		List<String> simGroupFiles = FileManagerImpl.getFileManager().list(folderToCheck, simGroupFilter);

		String path;
		ManagerProxy proxy;
		for (int i = 0; i < simGroupFiles.size(); i++) {
			path = simGroupFiles.get(i);

			// Only add the simulation group if it is not already known to the project
			proxy = _project.getManagerProxyByPath(path, SimulationGroup.class);
			if (proxy == null) {
				addSimulationGroup(path);
			}
		}
	}

	/**
	 * Creates a new SimulationGroup, reads it from the given file path, and registers it.
	 *
	 * @param path the absolute path to the .simgrp file to load
	 */
	private void addSimulationGroup(String path) {
		SimulationGroup simGroup = new SimulationGroup();
		addManager(simGroup, path);
	}

	/**
	 * Scans the sims directory for .simulation and .container files and re-adds any
	 * that are not already registered in the project.
	 */
	private void checkForAndAddSimulations() {
		String folderToCheck = getWatFolder(SIMS_FOLDER);

		// List both container and simulation files separately
		RMAFilenameFilter containerFilter = new RMAFilenameFilter("container");
		List<String> containerFiles = FileManagerImpl.getFileManager().list(folderToCheck, containerFilter);

		RMAFilenameFilter simFilter = new RMAFilenameFilter("simulation");
		List<String> simFiles = FileManagerImpl.getFileManager().list(folderToCheck, simFilter);

		String path;
		ManagerProxy proxy;

		// Re-add any simulation files not already in the project
		for (int i = 0; i < simFiles.size(); i++) {
			path = simFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatSimulation.class);
			if (proxy == null) {
				addSimulation(path);
			}
		}

		// Re-add any simulation container files not already in the project
		for (int i = 0; i < containerFiles.size(); i++) {
			path = containerFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatSimulationContainer.class);
			if (proxy == null) {
				addSimulationContainer(path);
			}
		}
	}

	/**
	 * Creates a new WatSimulationContainer, reads it from the given file path, and registers it.
	 *
	 * @param path the absolute path to the .container file to load
	 */
	private void addSimulationContainer(String path) {
		WatSimulationContainer container = new WatSimulationContainer();
		addManager(container, path);
	}

	/**
	 * Associates the given manager with the specified file, loads its data, and registers
	 * it with the project if loading succeeds.
	 *
	 * @param manager the uninitialized Manager instance to populate and register
	 * @param path    the absolute path to the manager's data file
	 */
	private void addManager(Manager manager, String path) {
		// Resolve the file path and associate it with the manager
		RmaFile file = FileManagerImpl.getFileManager().getFile(path);
		manager.setFile(file);
		manager.setProject(_project);
		manager.setFile(file);

		// Load the manager's data from disk and register it if successful
		if (manager.readData()) {
			_project.addManager(manager);
			_logger.info("Readded " + manager.getClass().getName() + " " + manager.getName() + " from file " + path);
		}
	}

	/**
	 * Creates a new WatSimulation, reads it from the given file path, and registers it.
	 *
	 * @param path the absolute path to the .simulation file to load
	 */
	private void addSimulation(String path) {
		WatSimulation sim = new WatSimulation();
		addManager(sim, path);
	}

	/**
	 * Constructs the absolute path to a WAT sub-directory within the project directory.
	 *
	 * @param subfolder the name of the sub-directory under the WAT directory
	 * @return the absolute path string: [projectDir]/wat/[subfolder]
	 */
	private String getWatFolder(String subfolder) {
		// Retrieve the root directory of the currently open WAT project
		String projectDir = _project.getProjectDirectory();

		// Append the WAT-specific subdirectory constant to form the WAT root path
		String dir = RMAIO.concatPath(projectDir, WAT_DIR);

		// Append the requested subfolder to produce the final target directory path
		dir = RMAIO.concatPath(dir, subfolder);

		// Return to the calling function
		return dir;
	}

	/**
	 * Scans the WAT alternatives folder for .walt files and registers any that are
	 * not already tracked by the project. For each .walt file found, the project is
	 * queried for an existing ManagerProxy by path; if none exists the alternative is
	 * added to the project via addAlternative.
	 */
	private void checkForAndAddAlternatives() {
		// Resolve the absolute path to the WAT alternatives folder for the current project
		String folderToCheck = getWatFolder(ALTS_FOLDER);

		// Create a filename filter that matches only .walt alternative definition files
		RMAFilenameFilter altsFilter = new RMAFilenameFilter("walt");

		// List all .walt files present in the alternatives folder
		List<String> altsFiles = FileManagerImpl.getFileManager().list(folderToCheck, altsFilter);

		String path;
		ManagerProxy proxy;

		for (int i = 0; i < altsFiles.size(); i++) {
			path = altsFiles.get(i);

			// Check whether the project already has a ManagerProxy registered for this path
			proxy = _project.getManagerProxyByPath(path, WatAlternative.class);

			// Only add the alternative when it has not yet been registered with the project
			if (proxy == null) {
				addAlternative(path);
			}
		}
	}

	/**
	 * Creates a new WatAlternative, reads it from the given file path, and registers it.
	 *
	 * @param path the absolute path to the .walt file to load
	 */
	private void addAlternative(String path) {
		WatAlternative alt = new WatAlternative();
		addManager(alt, path);
	}

	/**
	 * Scans the WAT analysis periods folder for .wap files and registers any that are
	 * not already tracked by the project. For each .wap file found, the project is
	 * queried for an existing ManagerProxy by path; if none exists the analysis period
	 * is added to the project via addAnalysisPeriod.
	 */
	private void checkForAndAddAnalysisPeriods() {
		// Resolve the absolute path to the WAT analysis periods folder for the current project
		String folderToCheck = getWatFolder(APS_FOLDER);

		// Create a filename filter that matches only .wap analysis period definition files
		RMAFilenameFilter apFilter = new RMAFilenameFilter("wap");

		// List all .wap files present in the analysis periods folder
		List<String> apFiles = FileManagerImpl.getFileManager().list(folderToCheck, apFilter);

		String path;
		ManagerProxy proxy;

		for (int i = 0; i < apFiles.size(); i++) {
			path = apFiles.get(i);

			// Check whether the project already has a ManagerProxy registered for this path
			proxy = _project.getManagerProxyByPath(path, WatAnalysisPeriod.class);

			// Only add the analysis period when it has not yet been registered with the project
			if (proxy == null) {
				addAnalysisPeriod(path);
			}
		}
	}

	/**
	 * Creates a new WatAnalysisPeriod, reads it from the given file path, and registers it.
	 *
	 * @param path the absolute path to the .wap file to load
	 */
	private void addAnalysisPeriod(String path) {
		WatAnalysisPeriod ap = new WatAnalysisPeriod();
		addManager(ap, path);
	}

	/**
	 * Standalone test entry point (not yet implemented).
	 *
	 * @param args unused command-line arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("main TODO implement me");
	}
}
