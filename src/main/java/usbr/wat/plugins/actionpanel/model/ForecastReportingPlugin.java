package usbr.wat.plugins.actionpanel.model;

import java.util.List; // Import List interface for holding collections of ensemble report information objects in method parameters

import javax.swing.Action; // Import Action class representing a menu item or toolbar button to trigger the reporting action within the UI

import usbr.wat.plugins.actionpanel.ActionsWindow; // Import main window container class where available actions are registered and managed
import usbr.wat.plugins.actionpanel.io.ReportOptions; // Import options class controlling report output settings such as format, headers/footers, and type
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleReportInfo; // Import data model representing information about a specific ensemble member in the forecast context
import usbr.wat.plugins.actionpanel.ui.UsbrPanel; // Import UI panel component where the action will be anchored or displayed to trigger report generation


/**
 * ForecastReportingPlugin is an interface extending ReportPlugin that defines the contract for forecast-specific reporting functionality.
 * It enables plugins to define how reports are generated for ensemble simulations by providing a method to create the UI Action
 * object and a method to execute the actual report creation logic with provided data and options.
 *
 * Implementations of this interface must:
 *
 *   Create an Action object that can be added to the ActionsWindow menu for user access<
 *   Implement createReport() to generate reports based on simulation info, ensemble data, and formatting options<
 *
 */

public interface ForecastReportingPlugin extends ReportPlugin {
	/**
	 * Creates and returns the Action object that triggers the forecast report generation.
	 * This method is responsible for constructing the UI action (e.g., menu item) that will be registered
	 * with the ActionsWindow parent to make the report command available to the user.
	 *
	 * @param parent The ActionsWindow container where this action should be registered in its menu bar
	 * @param parentPanel The UsbrPanel UI component serving as the parent context for the action binding
	 * @return Action object representing the report generation command to be displayed and executed
	 */
	Action getReportAction(ActionsWindow parent, UsbrPanel parentPanel); // Public abstract method returns Swing Action for UI

	/**
	 * Executes the core logic to create a forecast report using provided simulation and ensemble data.
	 * This method takes the simulation information (single or base), list of ensemble report info objects
	 * representing multiple runs, and options specifying output format and display preferences.
	 * It returns a boolean status indicating whether the report creation process completed successfully.
	 *
	 * @param sims Information object containing details for the primary simulation being reported on
	 * @param ensembleReportInfos List of report info objects for each ensemble member contributing to the forecast output
	 * @param options Configuration options controlling report type (PDF, HTML), headers/footers, and file naming
	 * @return Boolean flag indicating true if report was successfully created or cancelled, false if an error occurred
	 */
	public boolean createReport(SimulationReportInfo sims, List<EnsembleReportInfo> ensembleReportInfos, ReportOptions options); // Public abstract method executes report generation logic

}
