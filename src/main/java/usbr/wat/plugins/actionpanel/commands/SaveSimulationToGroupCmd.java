package usbr.wat.plugins.actionpanel.commands;

import java.awt.Color;                                                          // Color utility used for message styling (e.g., error in red)
import com.google.common.flogger.FluentLogger;                                  // Structured, fluent logging API from Google for leveled logs and context

import java.util.List;                                                          // Collections interface used for lists of simulations and model alternatives

import com.rma.commands.AbstractNewManagerCommand;                              // Base command for creating and registering new manager instances with the project
import com.rma.io.FileManagerImpl;                                              // File manager implementation that provides filesystem operations (e.g., getFile)
import com.rma.message.Message;                                                 // Message model used to post notifications to the WAT frame
import com.rma.model.Project;                                                   // Accessor for the current project and project-level operations
import rma.util.RMAIO;                                                          // RMA I/O utility helpers for path operations and safe concatenation

import hec2.model.DataLocation;                                                 // Data location mapping used by the model linking manager
import hec2.plugin.model.ModelAlternative;                                      // WAT model type representing a modeling alternative
import hec2.wat.WAT;                                                            // WAT application entry point (used for UI message posting)
import hec2.wat.model.WatAnalysisPeriod;                                        // WAT model type representing the analysis period associated with a simulation group
import hec2.wat.model.WatProject;                                               // WAT project type, used for accessing the model linking manager
import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run
import hec2.wat.model.WatSimulationContainer;                                   // Container manager for simulations within a project
import hec2.wat.model.WatModelLinkingManager;                                   // Manager for linking models and data locations across simulations

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Plugin entry to access the actions window (used indirectly during updates)
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;                   // Action to invoke data extraction/update for a simulation group
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;              // Base type representing a simulation group used by the actions and commands

/**
 * Command that copies a source {@link WatSimulation} into a target {@link AbstractSimulationGroup},
 * creating a new simulation and its container, optionally running data extract, and copying model
 * linking (data locations) from the source to the new simulation.
 *
 * Workflow:
 * - Build a new {@link WatSimulationContainer} and set its file, analysis period, and alternative
 * - Create a new {@link WatSimulation}, read data from the source, and assign to the container
 * - Add the container and simulation to the project
 * - Copy model linking from the source simulation to the new simulation
 * - Optionally run data extraction/update for the group
 */
public class SaveSimulationToGroupCmd extends AbstractNewManagerCommand {
	/**
	 * Logger for diagnostics and warnings in this command.
	 */
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	/**
	 * Flag indicating whether to run extract after creating the new simulation.
	 */
	private final boolean _runExtract;

	/**
	 * Source simulation being copied.
	 */
	private WatSimulation _srcSim;

	/**
	 * Target simulation group that will own the new simulation.
	 */
	private AbstractSimulationGroup _simGroup;

	/**
	 * Analysis period to assign to the new container and simulation.
	 */
	private WatAnalysisPeriod _ap;

	/**
	 * Newly created simulation instance (result of the command).
	 */
	private WatSimulation _newSim;

	/**
	 * Optional description to apply to the new simulation.
	 */
	private String _newDesc;

	/**
	 * Optional name to apply to the new simulation; if null, a default combined name is used.
	 */
	private String _newName;

	/**
	 * Constructs the command to copy a simulation into a simulation group.
	 *
	 * @param srcSim     the source simulation to copy
	 * @param newName    the new simulation name (null to auto-generate)
	 * @param newDesc    the new simulation description (nullable)
	 * @param simGroup   the destination simulation group
	 * @param project    the current project
	 * @param ap         the analysis period to assign
	 * @param runExtract true to run extract after creation; false otherwise
	 */
	public SaveSimulationToGroupCmd(WatSimulation srcSim, String newName, String newDesc, AbstractSimulationGroup simGroup,
	                                Project project, WatAnalysisPeriod ap, boolean runExtract) {
		// Initialize the base command; parameters are placeholders since we manage our own objects
		super(project, "", "", null); // not using what the super does

		// Store the source simulation reference
		_srcSim = srcSim;

		// Store optional new description
		_newDesc = newDesc;

		// Store optional new name (may be null)
		_newName = newName;

		// Store the target simulation group
		_simGroup = simGroup;

		// Store project reference for manager registration and file paths
		_project = project;

		// Store the analysis period for the new container/simulation
		_ap = ap;

		// Record whether to run extract after creating the simulation
		_runExtract = runExtract;
	}

	/**
	 * Executes the simulation copy command.
	 *
	 * Steps:
	 * 1) Create and configure a {@link WatSimulationContainer} for the new simulation
	 * 2) Create a new {@link WatSimulation}, load data from the source, and associate with the container
	 * 3) Add the container and simulation to the project
	 * 4) Copy model linking (data locations) from the source to the new simulation
	 * 5) Optionally run data extraction/update for the group
	 *
	 * @return false (the original implementation returns false; command success is implied through side effects)
	 */
	@Override
	public boolean doCommand() {
		// Local container reference
		WatSimulationContainer container;

		// Determine the new simulation name (use provided or derive from group and source)
		String newSimName = _newName;

		if (newSimName == null) {
			newSimName = getGroupSimName(_srcSim.getName(), _simGroup.getName());
		}

		// Create and configure the simulation container
		container = new WatSimulationContainer();

		// Preserve program order from source simulation
		container.setProgramOrder(_srcSim.getProgramOrder());

		// Set container name
		container.setName(newSimName);

		// Associate container with the current project
		container.setProject(_project);

		// Build the container file path under project/wat/sims
		String fileName = _project.getProjectDirectory();

		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".container"));

		// Assign the container file
		container.setFile(FileManagerImpl.getFileManager().getFile(fileName));

		// Set analysis period on the container
		container.setAnalysisPeriod(_ap);

		// Preserve modeling alternative from source container
		container.setAlternative(_srcSim.getContainerParent().getAlternative());

		// Register the container with the project
		_project.addManager(container);

		// Create the new simulation
		_newSim = new WatSimulation();

		// Associate simulation with the current project
		_newSim.setProject(_project);

		// Read the source simulation's data into the new simulation
		_newSim.setFile(_srcSim.getFile());
		_newSim.readData();

		// Set the new simulation name
		_newSim.setName(newSimName);

		// Optionally set the description
		if (_newDesc != null) {
			_newSim.setDescription(_newDesc);
		}

		// Build the simulation file path under project/wat/sims
		fileName = _project.getProjectDirectory();

		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".simulation"));

		// Assign the simulation file
		_newSim.setFile(FileManagerImpl.getFileManager().getFile(fileName));

		// Connect the simulation to its container
		_newSim.setSimulationContainer(container);

		// Add simulation to container
		container.addSimulation(_newSim);

		// Register the simulation with the project
		_project.addManager(_newSim);

		// Copy model linking (data locations) from source simulation to new simulation
		if (!copyModelLinking(_srcSim, _newSim)) {
			// Post an error message to the WAT frame if copy failed
			Message msg = new Message("Failed to copy Model Linking for simulation " + _newSim.getName() + ". Check log file for details", Color.RED);

			WAT.getWatFrame().addMessage(msg);
		}

		// Optionally run data extract/update for the simulation group
		if (_runExtract) {
			new UpdateDataAction().updateData(_simGroup);
		}

		// Original implementation returns false; side effects indicate success/failure
		return false;

	}

	/**
	 * Copies model linking data locations from the source simulation to the new simulation.
	 *
	 * Iterates all model alternatives associated with the source simulation, preserves
	 * variant name, retrieves data locations from the model linking manager, and sets
	 * them for the new simulation. Logs warnings when copying fails for any alternative.
	 *
	 * @param srcSim the source simulation
	 * @param newSim the destination simulation
	 * @return true if all applicable data locations were copied; false if any failed
	 */
	private boolean copyModelLinking(WatSimulation srcSim, WatSimulation newSim) {
		// Obtain the model linking manager from the current project
		WatModelLinkingManager mlm = getModelLinkingManager();

		// List of model alternatives associated with the source simulation
		List<ModelAlternative> modelAlts = srcSim.getAllModelAlternativeList();

		// Names used for retrieving and setting data locations
		String srcSimName = srcSim.getName();
		String newSimName = newSim.getName();

		// Loop local for current alternative
		ModelAlternative modelAlt;

		// Data locations for the current alternative
		List<DataLocation> dataLocs;

		// Track overall success status
		boolean rv = true;

		// Iterate through each model alternative and copy data locations
		for (int i = 0; i < modelAlts.size(); i++) {
			modelAlt = modelAlts.get(i);

			// Skip null alternatives
			if (modelAlt == null) {
				continue;
			}

			// Preserve variant name from source simulation
			modelAlt.setVariantName(srcSim.getVariantName());

			// Retrieve data locations for the source simulation and current alternative
			dataLocs = mlm.getDataLocationsFor(srcSimName, modelAlt);

			// Copy to the new simulation when available
			if (dataLocs != null) {
				if (!mlm.setDataLocationsFor(newSimName, modelAlt, dataLocs)) {
					// Log a warning when copying fails for a specific alternative
					LOGGER.atWarning().log("Failed to copy DataLocations for Simulation:" + newSimName + " ModelAlternative:" + modelAlt);

					rv = false;
				}
			}
		}

		return rv;
	}

	/**
	 * Returns the model linking manager from the current project.
	 *
	 * @return the {@link WatModelLinkingManager} instance
	 */
	private WatModelLinkingManager getModelLinkingManager() {
		// Cast to WatProject to access the model linking manager
		WatProject prj = (WatProject) Project.getCurrentProject();

		return prj.getModelLinkingManager();
	}

	/**
	 * Produces a combined name using simulation and group names.
	 *
	 * @param simName      the original simulation name
	 * @param simGroupName the simulation group name
	 * @return a combined name of the form "simulationName-groupName"
	 */
	public static String getGroupSimName(String simName, String simGroupName) {
		return simName + "-" + simGroupName;
	}

	/**
	 * Returns the file extension used for the simulation manager file.
	 *
	 * @return the string "simulation"
	 */
	@Override
	public String getExtension() {
		return "simulation";
	}

	/**
	 * Returns the fully qualified manager class name for {@link WatSimulation}.
	 *
	 * @return the manager class name
	 */
	@Override
	public String getManagerClass() {
		return WatSimulation.class.getName();
	}

	/**
	 * Returns a descriptive manager type string for UI and logging.
	 *
	 * @return "Simulation"
	 */
	@Override
	public String getManagerType() {
		return "Simulation";
	}

	/**
	 * Returns the newly created simulation, or null if not yet created.
	 *
	 * @return the new {@link WatSimulation} instance
	 */
	public WatSimulation getSimulation() {
		return _newSim;
	}
}