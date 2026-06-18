package usbr.wat.plugins.actionpanel.ui;

import java.util.List;  // Generic ordered collection interface; used as the return type of getSimulationReportInfos

import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;     // Data transfer object carrying per-simulation report metadata
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;    // Custom tree-table component displaying simulations and their results


/**
 * Contract that all WTMP simulation panel implementations must satisfy.
 *
 * This interface defines the complete set of actions and queries that the action
 * panel framework can invoke on any panel displaying a simulation group. Concrete
 * implementations include CalibrationPanel and the forecast simulation panel.
 *
 * The methods fall into three categories:
 *
 *   Context actions -- operations triggered by toolbar or right-click menu selections
 *   that act on the currently selected row in the simulation table:
 *     editSimulationMetaData, displayComputeLog, showInProjectTreeAction,
 *     displaySimulationInMap, displayReport, displayFile.
 *
 *   Table management -- methods that populate or refresh the simulation table:
 *     fillSimulationTable, updateComputeStates.
 *
 *   Data access -- queries returning data needed by other components:
 *     getSimulationReportInfos, getSimulationTreeTable.
 *
 * All implementations are expected to obtain the currently selected simulation or
 * results entry from the simulation tree-table and delegate to the appropriate
 * framework component.
 *
 */
public interface UsbrPanel {
	/**
	 * Opens the metadata editor for the simulation currently selected in the table.
	 *
	 * Implementations should open a modal editor pre-populated with the selected
	 * simulation's name and description. Does nothing if no simulation row is selected.
	 */
	void editSimulationMetaData();


	/**
	 * Opens the compute log file for the simulation currently selected in the table.
	 *
	 * Implementations should locate the log file path from the selected simulation
	 * and display it in the WAT log viewer. Does nothing if no simulation row is
	 * selected or if the log file does not exist on disk.
	 */
	void displayComputeLog();


	/**
	 * Selects and reveals the node for the currently selected simulation in the WAT
	 * project tree.
	 *
	 * Implementations should find the corresponding project tree node and scroll it
	 * into view. Does nothing if no row is selected or no matching node is found.
	 */
	void showInProjectTreeAction();


	/**
	 * Displays the currently selected simulation in the WAT map view.
	 *
	 * Implementations should pass the selected simulation to the browser frame's
	 * displayManager method. Does nothing if no row is selected.
	 */
	void displaySimulationInMap();


	/**
	 * Opens the report for the simulation or results entry currently selected in the table.
	 * <p>
	 * Implementations should resolve the appropriate report directory from the selection
	 * and delegate to a DisplayReportAction. Does nothing if no row is selected.
	 */
	void displayReport();


	/**
	 * Populates the simulation table with the data from the panel's current simulation group.
	 *
	 * Implementations should rebuild the tree-table model from the active group and
	 * apply compute-state row colors. Called whenever the active group changes or needs
	 * to be refreshed.
	 */
	void fillSimulationTable();


	/**
	 * Returns a list of SimulationReportInfo objects for all currently selected
	 * simulations and results entries in the table.
	 *
	 * Implementations should iterate both the selected simulation rows and the selected
	 * results rows, constructing a fully populated SimulationReportInfo for each.
	 *
	 * @return a list of report info objects, one per selected item; never null but may
	 * be empty if nothing is selected
	 */
	List<SimulationReportInfo> getSimulationReportInfos();


	/**
	 * Displays the file at the given path using the parent window's file viewer.
	 *
	 * Typically used to open a report file after it has been generated or located.
	 *
	 * @param latestFile the absolute path of the file to display; must not be null
	 */
	void displayFile(String latestFile);


	/**
	 * Returns the SimulationTreeTable component managed by this panel.
	 *
	 * Used by external components (such as SimulationActionsPanel) to query the
	 * current selection state or register additional listeners.
	 *
	 * @return the simulation tree-table; never null after the panel has been constructed
	 */
	SimulationTreeTable getSimulationTreeTable();


	/**
	 * Refreshes the foreground color of every simulation row to reflect current compute states.
	 *
	 * Implementations should re-evaluate each simulation's compute state and reapply
	 * the appropriate color coding. Typically called after a compute operation completes
	 * or when the project notifies that simulation states have changed.
	 */
	void updateComputeStates();

}
