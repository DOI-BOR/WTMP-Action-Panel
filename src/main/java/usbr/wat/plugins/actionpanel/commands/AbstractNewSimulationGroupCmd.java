package usbr.wat.plugins.actionpanel.commands;

import java.util.List;                                                      // Collections interface used for lists of simulations

import com.rma.commands.AbstractNewManagerCommand;                          // Base command for creating and registering new manager instances with the project
import com.rma.io.RmaFile;                                                  // Abstraction for a file within the RMA file system utilities
import com.rma.model.Project;                                               // Accessor for the current project and project-level operations

import hec2.wat.model.WatAnalysisPeriod;                                    // WAT model type representing the analysis period associated with a simulation group
import hec2.wat.model.WatSimulation;                                        // WAT model type representing a single simulation scenario or run

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;          // Base type representing a simulation group used by the actions

/**
 * Base command for creating a new simulation group and populating it with simulations.
 *
 * This command:
 * - Creates a new manager (the simulation group) via the superclass implementation
 * - Sets the analysis period on the newly created group
 * - Copies selected simulations into the group, optionally running an extract step
 * - Saves the project after modifications
 *
 * Subclasses provide concrete implementations and configuration for specific group types.
 */
public abstract class AbstractNewSimulationGroupCmd extends AbstractNewManagerCommand {

	/**
	 * Flag indicating whether to run extract during simulation creation.
	 */
	private final boolean _runExtract;

	/** List of simulations to be copied into the new group. */
	private List<WatSimulation> _sims;

	/**
	 * Analysis period to assign to the new simulation group.
	 */
	private WatAnalysisPeriod _ap;

	/**
	 * Constructs the command to create a new simulation group and populate it.
	 *
	 * @param project    the project in which the group is created
	 * @param name       the display name of the new group
	 * @param descr      the description of the new group
	 * @param dir        the directory for group-related files
	 * @param ap         the analysis period to set on the group
	 * @param sims       the simulations to copy into the group
	 * @param runExtract true to run extract when copying simulations, false otherwise
	 */
	public AbstractNewSimulationGroupCmd(Project project, String name, String descr, RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims,
	                                     boolean runExtract) {
		// Initialize the base new-manager command
		super(project, name, descr, dir);

		// Store the analysis period for assignment to the new group
		_ap = ap;

		// Store the list of simulations to be copied into the new group
		_sims = sims;

		// Record whether the extract step should be run during simulation copy
		_runExtract = runExtract;

	}

	/**
	 * Returns the newly created simulation group manager.
	 *
	 * @return the simulation group as an {@link AbstractSimulationGroup}
	 */
	public AbstractSimulationGroup getSimulationGroup() {
		// The superclass produces the manager; cast to a simulation group
		return (AbstractSimulationGroup) getManager();
	}

	/**
	 * Executes the command to create and configure the new simulation group.
	 *
	 * Steps:
	 * 1) Delegate creation to the superclass
	 * 2) Set the analysis period on the new group
	 * 3) Copy each selected simulation into the new group
	 * 4) Save the project
	 *
	 * @return true if the command succeeded, false otherwise
	 */
	@Override
	public boolean doCommand() {
		// Create the manager (simulation group) via the base command
		boolean rv = super.doCommand();

		// Retrieve the created simulation group
		AbstractSimulationGroup simgroup = getSimulationGroup();

		// Assign the analysis period if the group exists
		if (simgroup != null) {
			simgroup.setAnalysisPeriod(_ap);
		}

		// Temporary references used during the copy loop
		WatSimulation sim;
		WatSimulation newSim;

		// Copy each selected simulation into the new group
		for (int i = 0; i < _sims.size(); i++ ) {
			// Source simulation to copy
			sim = _sims.get(i);

			// Create the new simulation within the group, optionally running extract
			newSim = createSimulation(sim, simgroup, _project, _ap, _runExtract);

			// Register the new simulation with the group
			simgroup.addSimulation(newSim);
		}

		// Persist changes to the project
		_project.saveProject();

		return rv;
	}

	/**
	 * Creates a copy of the provided simulation in the specified group.
	 *
	 * @param sim        the source simulation to copy
	 * @param simgroup   the target simulation group
	 * @param project    the current project
	 * @param ap         the analysis period to assign to the new simulation
	 * @param runExtract true to run extract during copy, false otherwise
	 * @return the new simulation created in the group
	 */
	public static WatSimulation createSimulation(WatSimulation sim, AbstractSimulationGroup simgroup, Project project, WatAnalysisPeriod ap, boolean runExtract) {
		// Build and execute the command to copy the simulation into the group
		SaveSimulationToGroupCmd cmd = new SaveSimulationToGroupCmd(sim, null, null, simgroup, project, ap, runExtract);
		cmd.doCommand();

		// Retrieve the newly created simulation from the command
		WatSimulation newSim = cmd.getSimulation();

		return newSim;
	}

	/**
	 * Produces a combined name using simulation and group names.
	 *
	 * @param simName      the original simulation name
	 * @param simGroupName the simulation group name
	 * @return a combined name of the form "simulationName-groupName"
	 */
	public static String getGroupSimName(String simName, String simGroupName) {
		// Concatenate simulation and group names using a hyphen
		return simName + "-" + simGroupName;
	}
}