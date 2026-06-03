package usbr.wat.plugins.actionpanel.commands;

import java.util.List;                                                              // Collections interface used for lists of simulations to include in the new group

import com.rma.io.RmaFile;                                                          // Abstraction for a file/directory within the RMA file system utilities
import com.rma.model.Project;                                                       // Accessor for the current project and project-level operations

import hec2.wat.model.WatAnalysisPeriod;                                            // WAT model type representing the analysis period associated with a simulation group
import hec2.wat.model.WatSimulation;                                                // WAT model type representing a single simulation scenario or run
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                // Forecast-specific simulation group type used by the forecast panel and commands

/**
 * Command for creating a new {@link ForecastSimGroup} and populating it with simulations.
 *
 *
 * This command extends {@code AbstractNewSimulationGroupCmd} to tailor creation for
 * forecast simulation groups. It supplies the correct manager type, class, and file
 * extension, and defaults to not running extract during creation.
 *
 */
public class NewForecastSimulationGroupCmd extends AbstractNewSimulationGroupCmd {

	/**
	 * Constructs a command to create a new forecast simulation group.
	 *
	 * @param project the project in which the group is created
	 * @param name    the display name of the new forecast group
	 * @param descr   the description of the new forecast group
	 * @param dir     the directory for group-related files
	 * @param ap      the analysis period to set on the group
	 * @param sims    the simulations to copy into the group
	 */
	public NewForecastSimulationGroupCmd(Project project, String name, String descr,
	                                     RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims) {
		// Initialize the base command with "runExtract" set to false for forecast groups
		super(project, name, descr, dir, ap, sims, false);

	}

	/**
	 * Returns the file extension used for forecast simulation group files.
	 *
	 * @return the file extension associated with {@link ForecastSimGroup}
	 */
	@Override
	public String getExtension() {
		// Delegate to the forecast group's static file extension constant
		return ForecastSimGroup.FILE_EXT;
	}

	/**
	 * Returns the fully qualified class name for the forecast simulation group manager.
	 *
	 * @return the manager class name
	 */
	@Override
	public String getManagerClass() {
		// Provide the forecast simulation group's class name to the framework
		return ForecastSimGroup.class.getName();
	}

	/**
	 * Returns the descriptive manager type for this command.
	 *
	 * @return a human-readable manager type string
	 */
	@Override
	public String getManagerType() {
		// Identify this manager type as a Forecast Simulation Group
		return "Forecast Simulation Group";
	}

	/**
	 * Returns the created forecast simulation group.
	 *
	 * @return the {@link ForecastSimGroup} produced by this command
	 */
	@Override
	public ForecastSimGroup getSimulationGroup() {
		// Narrow the base simulation group type to the forecast-specific type
		return (ForecastSimGroup) super.getSimulationGroup();
	}

}