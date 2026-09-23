package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;          // Provides invokeLater for scheduling work on the Event Dispatch Thread
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Point;               // Represents an (x, y) coordinate, used for mouse hit-testing
import java.awt.event.MouseEvent;    // Carries mouse interaction data including cursor position
import java.io.File;                 // Represents a filesystem path, used when scanning for DSS files
import java.util.ArrayList;         // Resizable-array List implementation for collecting report info objects
import java.util.Date;               // Converts epoch milliseconds to a formatted date string
import java.util.List;               // Generic ordered collection interface

import javax.swing.JPanel;              // General-purpose Swing container used for sub-panels
import javax.swing.tree.MutableTreeNode; // Interface for tree nodes that can be modified, used for project tree selection
import javax.swing.tree.TreePath;        // Represents the path from the tree root to a selected node

import com.rma.client.Browser;           // Provides access to the RMA browser frame and project tree
import com.rma.io.FileManagerImpl;       // RMA implementation for obtaining managed file and directory references
import com.rma.io.RmaFile;               // RMA abstraction over a filesystem file or directory

import hec2.wat.WAT;                     // Entry point for accessing top-level WAT framework objects (e.g. WatFrame)
import hec2.wat.model.WatSimulation;     // WAT simulation model object containing compute state, paths, and metadata

import rma.swing.EnabledJPanel;          // RMA JPanel subclass with built-in enabled/disabled visual state support
import rma.util.RMAFilenameFilter;       // Filename filter that accepts files matching a given extension
import rma.util.RMAIO;                   // RMA file I/O utilities including path and filename manipulation

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;              // Singleton plugin entry point providing access to the actions window
import usbr.wat.plugins.actionpanel.ActionsWindow;                  // Top-level WTMP actions window that owns simulation panel instances
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;         // Panel hosting action buttons whose enabled state depends on table selection
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;    // Action that locates and opens a simulation report file
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;  // Base model class for a group of simulations with a shared analysis period
import usbr.wat.plugins.actionpanel.model.ResultsData;              // Model object representing a saved simulation results snapshot
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;     // Data transfer object carrying per-simulation report metadata
import usbr.wat.plugins.actionpanel.ui.tree.ResultsTreeTableNode;   // Tree-table node representing a saved results entry
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;    // Custom tree-table component displaying simulations and their results
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel; // Tree-table model that backs SimulationTreeTable with simulation data
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;  // Tree-table node representing a single WatSimulation


/**
 * Abstract base panel for displaying and interacting with a group of WAT simulations.
 *
 * This class provides the shared UI infrastructure and behaviour used by all concrete
 * simulation panel implementations in the WTMP action panel. Responsibilities include:
 *
 *   Populating and refreshing a SimulationTreeTable from the active simulation group.
 *   Delegating context actions (edit metadata, display log, show in project tree,
 *   display in map, display report) to the appropriate framework components.
 *   Collecting SimulationReportInfo objects for the selected simulations and results.
 *   Locating the DSS output file for a saved results snapshot within its folder.
 *
 * Concrete subclasses must implement getSimulationGroup() to supply the model object
 * and setSimulationGroup(AbstractSimulationGroup) to accept model replacements.
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractSimulationPanel extends EnabledJPanel
		implements UsbrPanel {
	// --- Core child components shared by all concrete subclasses ---

	/**
	 * Panel containing the action buttons whose enabled state tracks the table selection.
	 */
	protected SimulationActionsPanel _simActionsPanel;

	/**
	 * Tree-table component that displays simulations and their saved results.
	 */
	protected SimulationTreeTable _simulationTable;

	/**
	 * Reference to the owning ActionsWindow, used for delegation and context lookups.
	 */
	protected ActionsWindow _parentWindow;


	/**
	 * Constructs the panel with a GridBagLayout and stores the parent window reference.
	 * <p>
	 * Subclasses are responsible for adding child components to this panel after
	 * calling super().
	 *
	 * @param parent the ActionsWindow that owns this panel; must not be null
	 */
	public AbstractSimulationPanel(ActionsWindow parent) {
		super(new GridBagLayout());

		// Store the parent window for later use in context actions and delegation
		_parentWindow = parent;
	}


	/**
	 * Builds and returns the simulation legend panel.
	 * Compute-state colors are intentionally not displayed because those row colors were
	 * unreliable and could appear on the wrong simulation after compute/table updates.
	 *
	 * @return an empty JPanel placeholder for existing callers
	 */
	protected JPanel buildLegendPanel() {
		return new JPanel();
	}


	/**
	 * Responds to a checkbox state change in the simulation table by scheduling an
	 * action-button state refresh on the Event Dispatch Thread.
	 *
	 * Posting via invokeLater ensures the table's selection model has finished
	 * updating before the action panel queries it.
	 */
	protected void tableCheckBoxAction() {
		// Schedule the action-panel refresh after the current EDT event completes
		EventQueue.invokeLater(() -> _simActionsPanel.updateActions());
	}


	/**
	 * Returns a tooltip string for the simulation table cell under the given mouse position.
	 *
	 * Tooltips are only provided for cells in the simulation name column. The text is
	 * sourced from the tree node at the hovered row:
	 * SimulationTreeTableNode nodes delegate to their own getToolTipText implementation.
	 * ResultsTreeTableNode nodes provide a results-specific tooltip.
	 * Returns null for all other columns or when no node is found at the row.
	 *
	 * @param e the mouse event carrying the cursor position within the table
	 * @return the tooltip string for the hovered cell, or null if none applies
	 */
	protected String getTableToolTipText(MouseEvent e) {
		// Convert the mouse position to table row and column indices
		Point pt = e.getPoint();
		int row = _simulationTable.rowAtPoint(pt);
		int col = _simulationTable.columnAtPoint(pt);

		// Return null immediately if the cursor is outside all cells
		if (row == -1 || col == -1) {
			return null;
		}

		// Tooltips are only meaningful for the simulation name column
		if (col == SimulationTreeTableModel.SIMULATION_COLUMN) {
			TreePath treePath = _simulationTable.getPathForRow(row);

			if (treePath != null) {
				Object lastComp = treePath.getLastPathComponent();

				if (lastComp instanceof SimulationTreeTableNode) {
					// Simulation node: delegate tooltip to the node using the current group context
					SimulationTreeTableNode simNode = (SimulationTreeTableNode) lastComp;
					return simNode.getToolTipText(_parentWindow.getSimulationGroup());
				} else if (lastComp instanceof ResultsTreeTableNode) {
					// Results node: use the node's own tooltip text
					ResultsTreeTableNode resultsNode = (ResultsTreeTableNode) lastComp;
					return resultsNode.getToolTipText();
				}
			}
		}

		return null;
	}



	/**
	 * Opens the metadata editor dialog for the simulation selected in the simulation table.
	 * If no row is selected, the method returns immediately without taking any action.
	 * The selected row's object is retrieved and verified to be a WatSimulation instance
	 * before the editor is populated and displayed. This method overrides the base class
	 * implementation to provide simulation-specific editing behavior.
	 */
	@Override
	public void editSimulationMetaData() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Guard: no row selected
		if (row < 0) {
			return;
		}

		// Retrieve the object stored in the simulation column of the selected row
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);

		// Verify that the retrieved object is a WatSimulation before proceeding
		if (obj instanceof WatSimulation) {
			// Open the metadata editor pre-filled with the selected simulation's data
			MetaDataEditor editor = new MetaDataEditor(_parentWindow);

			// Populate the editor form with the selected simulation's existing metadata
			editor.fillForm((WatSimulation) obj);

			// Display the editor dialog to the user
			editor.setVisible(true);
		}
	}


	/**
	 * Opens the compute log file for the simulation selected in the simulation table.
	 * If no row is selected, the method returns immediately without taking any action.
	 * The selected row's object is verified to be a WatSimulation before the log file
	 * path is retrieved and checked for existence on disk. If the file exists, it is
	 * opened in the WAT frame's compute log viewer. This method overrides the base
	 * class implementation to provide simulation-specific log display behavior.
	 */
	@Override
	public void displayComputeLog() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Guard: no row selected
		if (row < 0) {
			return;
		}

		// Retrieve the object stored in the simulation column of the selected row
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);

		// Verify that the retrieved object is a WatSimulation before proceeding
		if (obj instanceof WatSimulation) {
			// Cast the object to WatSimulation to access simulation-specific methods
			WatSimulation sim = (WatSimulation) obj;

			// Retrieve the file path of the compute log associated with this simulation
			String logFile = sim.getLogFile();

			// Only attempt to open the log if the file actually exists on disk
			if (FileManagerImpl.getFileManager().fileExists(logFile)) {
				// Obtain an RmaFile handle for the log file through the file manager
				RmaFile f = FileManagerImpl.getFileManager().getFile(logFile);

				// Open the log file in the WAT frame's compute log viewer
				WAT.getWatFrame().openComputeLog(f);
			}
		}
	}



	/**
	 * Selects and scrolls to the project tree node corresponding to the simulation
	 * chosen in the simulation table. If no row is selected, the method returns
	 * immediately. If the selected row holds a ResultsData object, its parent
	 * simulation is resolved first. The matching tree node is then located and
	 * programmatically selected to bring it into view. This method overrides the
	 * base class implementation to provide simulation-specific project tree navigation.
	 */
	@Override
	public void showInProjectTreeAction() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Guard: no row selected
		if (row < 0) {
			return;
		}

		// Retrieve the object stored in the simulation column of the selected row
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);

		// If the selected row is a results snapshot, resolve its parent simulation instead
		if (obj instanceof ResultsData) {
			// Unwrap the ResultsData to get the WatSimulation it belongs to
			obj = ((ResultsData) obj).getSimulation();
		}

		// Verify the object is a WatSimulation before attempting tree navigation
		if (obj instanceof WatSimulation) {
			// Look up the project tree node corresponding to this simulation
			MutableTreeNode simNode = Browser.getBrowserFrame()
					.getProjectTree()
					.getNodeForManager((WatSimulation) obj);

			if (simNode != null) {
				// Programmatically select the node to scroll it into view
				Browser.getBrowserFrame().getProjectTree().setSelectedNode(simNode);
			}
		}
	}


	/**
	 * Displays the simulation selected in the simulation table on the map view.
	 * If no row is selected, the method returns immediately without taking any action.
	 * The object in the simulation column of the selected row is cast to a WatSimulation
	 * and passed to the overloaded displaySimulationInMap method for rendering. This method
	 * overrides the base class implementation to provide simulation-specific map display behavior.
	 */
	@Override
	public void displaySimulationInMap() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Guard: no row selected
		if (row == -1) {
			return;
		}

		// Retrieve and cast the object in the simulation column to a WatSimulation
		WatSimulation sim = (WatSimulation) _simulationTable.getValueAt(
				row, SimulationTreeTableModel.SIMULATION_COLUMN);

		// Delegate to the overloaded method to render the simulation on the map
		displaySimulationInMap(sim);
	}


	/**
	 * Displays the given simulation in the WAT map view via the browser frame.
	 *
	 * Does nothing if sim is null.
	 *
	 * @param sim the simulation to display on the map; may be null
	 */
	public void displaySimulationInMap(WatSimulation sim) {
		if (sim != null) {
			Browser.getBrowserFrame().displayManager(sim);
		}
	}


	/**
	 * Displays the report for the simulation or results snapshot selected in the simulation table.
	 * If no row is selected, the method returns immediately without taking any action.
	 * If the selected row holds a WatSimulation, its simulation output directory is used as
	 * the report source. If it holds a ResultsData object, the saved results folder is used
	 * instead. In both cases the overloaded displayReport method is called with the resolved
	 * directory path. This method overrides the base class implementation to provide
	 * simulation-specific report display behavior.
	 */
	@Override
	public void displayReport() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Guard: no row selected
		if (row == -1) {
			return;
		}

		// Retrieve the object stored in the simulation column of the selected row
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);

		if (obj instanceof WatSimulation) {
			// Cast the object to WatSimulation to access the simulation output directory
			WatSimulation sim = (WatSimulation) obj;

			// Use the simulation's own output directory as the report source
			displayReport(sim.getSimulationDirectory());

		} else if (obj instanceof ResultsData) {
			// Cast the object to ResultsData to access the saved results folder
			ResultsData rd = (ResultsData) obj;

			// Use the saved results folder as the report source
			displayReport(rd.getFolder());
		}
	}


	/**
	 * Triggers report display for the given simulation directory by constructing and
	 * executing a DisplayReportAction.
	 *
	 * @param simulationDirectory absolute path to the directory that contains the report
	 */
	public void displayReport(String simulationDirectory) {
		DisplayReportAction action = new DisplayReportAction(this);
		action.displayReportAction(simulationDirectory);
	}


	/**
	 * Returns the simulation group model that this panel currently displays.
	 *
	 * Subclasses must implement this method to supply the concrete group type
	 * (SimulationGroup, ForecastSimGroup, etc.) appropriate to their context.
	 *
	 * @return the active AbstractSimulationGroup; must not be null after initialization
	 */
	public abstract AbstractSimulationGroup getSimulationGroup();


	/**
	 * Refreshes the simulation table with the data from the current simulation group.
	 *
	 * Delegates to setSimulationTable(AbstractSimulationGroup) using the group returned
	 * by getSimulationGroup().
	 */
	@Override
	public void fillSimulationTable() {
		setSimulationTable(getSimulationGroup());
	}


	/**
	 * Replaces the simulation table's model with one built from the given group.
	 *
	 * Steps performed:
	 * 1. Build a new SimulationTreeTableModel from the group and apply it to the table.
	 * 2. Clear all existing row color overrides.
	 * 3. Revalidate the table to trigger a layout and repaint pass.
	 *
	 * @param sg the simulation group whose simulations should be shown in the table
	 */
	public void setSimulationTable(AbstractSimulationGroup sg) {
		// Build and install a fresh tree-table model from the provided simulation group
		SimulationTreeTableModel newModel = new SimulationTreeTableModel(sg);
		_simulationTable.setTreeTableModel(newModel);

		// Remove any color overrides left over from the previous model
		_simulationTable.clearColors();

		// Trigger a layout recalculation to account for the new model data
		_simulationTable.revalidate();
	}


	/**
	 * Builds and returns a list of SimulationReportInfo objects for all selected rows
	 * in the simulation table. The method performs two passes: the first collects report
	 * info from selected WatSimulation rows, and the second collects report info from
	 * selected ResultsData rows. Each SimulationReportInfo is populated with the
	 * simulation reference, DSS file path, output folder, name, description, last computed
	 * date, and simulation group. ResultsData entries use a combined display name and
	 * resolve their DSS file path relative to the results folder rather than the simulation
	 * directory. This method overrides the base class implementation to provide
	 * simulation-specific report info gathering behavior.
	 *
	 * @return a list of SimulationReportInfo objects representing all selected simulations
	 *         and results snapshots, in selection order
	 */
	@Override
	public List<SimulationReportInfo> getSimulationReportInfos() {
		// Initialize the list that will hold report info for all selected rows
		List<SimulationReportInfo> simInfos = new ArrayList<>();

		// Collect the two distinct selection types from the table
		List<WatSimulation> selectedSims = getSelectedSimulations();
		List<ResultsData> selectedResults = getSelectedResults();

		// Declare shared variables for building each SimulationReportInfo entry
		SimulationReportInfo simInfo;
		WatSimulation sim;
		ResultsData results;

		// --- First pass: selected simulation rows ---
		for (int i = 0; i < selectedSims.size(); i++) {
			// Retrieve the current selected simulation
			sim = selectedSims.get(i);

			// Create a new report info object and populate it with the simulation's metadata
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(sim);
			simInfo.setSimDssFile(sim.getSimulationDssFile());
			simInfo.setSimFolder(sim.getSimulationDirectory());
			simInfo.setName(sim.getName());
			simInfo.setShortName(sim.getName());
			simInfo.setDescription(sim.getDescription());

			// Convert epoch milliseconds to a human-readable date string
			simInfo.setLastComputedDate(new Date(sim.getLastComputedDate()).toString());

			// Mark this entry as a live simulation rather than a saved results snapshot
			simInfo.setIsSimulation(true);

			// Associate the report info with the current simulation group from the actions window
			simInfo.setSimulationGroup(
					ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

			// Add the fully populated report info to the output list
			simInfos.add(simInfo);
		}

		// --- Second pass: selected results rows ---
		for (int i = 0; i < selectedResults.size(); i++) {
			// Retrieve the current selected results snapshot
			results = selectedResults.get(i);

			// Create a new report info object and link it to the parent simulation
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(results.getSimulation());

			// Locate the DSS file inside the results folder since its path may differ
			// from the simulation's own DSS path
			simInfo.setSimDssFile(findSimulationDssFile(
					results.getFolder(),
					results.getSimulation().getSimulationDssFile()));

			// Use the results folder as the report source directory
			simInfo.setSimFolder(results.getFolder());

			// Combine the simulation and results names for a descriptive display label
			String name = results.getSimulation().getName()
					.concat(" - ")
					.concat(results.getName());
			simInfo.setName(name);

			// Use only the results name as the short display label
			simInfo.setShortName(results.getName());
			simInfo.setDescription(results.getDescription());

			// Convert the results snapshot's last computed time from epoch milliseconds to a date string
			simInfo.setLastComputedDate(new Date(results.getLastComputedTime()).toString());

			// Mark this entry as a saved results snapshot rather than a live simulation
			simInfo.setIsSimulation(false);

			// Associate the report info with the current simulation group from the actions window
			simInfo.setSimulationGroup(
					ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

			// Add the fully populated results report info to the output list
			simInfos.add(simInfo);
		}

		// Return the complete list of report info objects for all selected rows
		return simInfos;
	}


	/**
	 * Returns the list of ResultsData entries currently selected in the simulation table.
	 *
	 * @return a list of selected ResultsData objects; never null but may be empty
	 */
	public List<ResultsData> getSelectedResults() {
		return _simulationTable.getSelectedResults();
	}


	/**
	 * Delegates display of a file to the parent ActionsWindow.
	 *
	 * @param rptFile absolute path to the file to display
	 */
	@Override
	public void displayFile(String rptFile) {
		_parentWindow.displayFile(rptFile);
	}


	/**
	 * Returns the SimulationTreeTable component managed by this panel.
	 *
	 * @return the simulation tree-table; never null after construction
	 */
	@Override
	public SimulationTreeTable getSimulationTreeTable() {
		return _simulationTable;
	}


	/**
	 * Clears simulation row colors. The previous compute-state colors were unreliable because
	 * row foregrounds are tracked by table row index, which can make status colors appear on
	 * the wrong simulation after compute or table updates.
	 */
	@Override
	public void updateComputeStates() {
		// Clear all existing row color overrides
		_simulationTable.clearColors();

		// Flush the cleared colors to the screen
		_simulationTable.repaint();
	}


	/**
	 * Searches the given folder for a DSS file whose name matches the file name
	 * portion of the simulation's known DSS path.
	 *
	 * This is necessary for saved results snapshots where the DSS file may have been
	 * moved or copied to a results-specific subdirectory, making the original absolute
	 * path on the simulation object stale.
	 *
	 * @param folder            absolute path to the directory to search
	 * @param simulationDssFile the original DSS file path from the simulation object;
	 *                          only the file name portion is used for matching
	 * @return the absolute path of the first matching DSS file found in the folder,
	 * or an empty string if no match is found
	 */
	protected String findSimulationDssFile(String folder, String simulationDssFile) {
		// Extract just the file name from the full simulation DSS path for comparison
		String lookForDssFile = RMAIO.getFileFromPath(simulationDssFile);

		// Obtain an RMA-managed reference to the results folder for directory listing
		RmaFile folderFile = FileManagerImpl.getFileManager().getFile(folder);

		// Build a filter that accepts only files with a .dss extension
		RMAFilenameFilter filter = new RMAFilenameFilter("dss");
		filter.setAcceptDirectories(false);

		// List all DSS files in the folder
		File[] dssFiles = folderFile.listFiles(filter);

		if (dssFiles != null) {
			for (int i = 0; i < dssFiles.length; i++) {
				String name = dssFiles[i].getName();

				// Case-insensitive comparison to handle cross-platform filename variations
				if (name.equalsIgnoreCase(lookForDssFile)) {
					return dssFiles[i].getAbsolutePath();
				}
			}
		}

		// No matching DSS file was found in the folder
		return "";
	}


	/**
	 * Returns the list of WatSimulation objects currently selected in the simulation table.
	 *
	 * @return a list of selected WatSimulation objects; never null but may be empty
	 */
	public List<WatSimulation> getSelectedSimulations() {
		return _simulationTable.getSelectedSimulations();
	}


	/**
	 * Replaces the simulation group model that this panel displays.
	 *
	 * Subclasses must implement this method to accept a new group and refresh the UI
	 * accordingly.
	 *
	 * @param simGroup the new simulation group to display; must not be null
	 */
	public abstract void setSimulationGroup(AbstractSimulationGroup simGroup);
}
