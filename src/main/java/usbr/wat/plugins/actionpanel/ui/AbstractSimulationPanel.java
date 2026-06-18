package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;               // AWT color constants and custom color construction
import java.awt.EventQueue;          // Provides invokeLater for scheduling work on the Event Dispatch Thread
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Point;               // Represents an (x, y) coordinate, used for mouse hit-testing
import java.awt.event.MouseEvent;    // Carries mouse interaction data including cursor position
import java.io.File;                 // Represents a filesystem path, used when scanning for DSS files
import java.util.ArrayList;         // Resizable-array List implementation for collecting report info objects
import java.util.Date;               // Converts epoch milliseconds to a formatted date string
import java.util.List;               // Generic ordered collection interface

import javax.swing.BorderFactory;        // Factory for creating Swing border decorations
import javax.swing.JLabel;               // Swing label component for displaying text and icons
import javax.swing.JPanel;              // General-purpose Swing container used for sub-panels
import javax.swing.tree.MutableTreeNode; // Interface for tree nodes that can be modified, used for project tree selection
import javax.swing.tree.TreePath;        // Represents the path from the tree root to a selected node

import com.rma.client.Browser;           // Provides access to the RMA browser frame and project tree
import com.rma.io.FileManagerImpl;       // RMA implementation for obtaining managed file and directory references
import com.rma.io.RmaFile;               // RMA abstraction over a filesystem file or directory

import hec2.wat.WAT;                     // Entry point for accessing top-level WAT framework objects (e.g. WatFrame)
import hec2.wat.model.WatSimulation;     // WAT simulation model object containing compute state, paths, and metadata

import rma.swing.ColorIcon;              // Renders a solid filled square of a given color as a Swing icon
import rma.swing.EnabledJPanel;          // RMA JPanel subclass with built-in enabled/disabled visual state support
import rma.swing.RmaInsets;              // Constants for common GridBagLayout inset configurations
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
 *   Building a color-coded legend that maps simulation compute states to row colors.
 *   Populating and refreshing a SimulationTreeTable from the active simulation group.
 *   Resolving per-row foreground colors based on a simulation's current compute state.
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
	// --- Compute-state row foreground colors shown in the simulation table ---

	/**
	 * Row color indicating a simulation has never been computed.
	 */
	protected static final Color NOT_COMPUTED_COLOR = Color.BLUE;

	/**
	 * Row color indicating a simulation has been successfully computed.
	 */
	protected static final Color COMPUTED_COLOR = Color.GREEN.darker();

	/**
	 * Row color indicating the most recent compute attempt ended with an error.
	 */
	protected static final Color COMPUTED_ERROR_COLOR = Color.RED;

	/**
	 * Row color indicating a simulation was previously computed but is now out of date.
	 */
	protected static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;


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
	 * Builds and returns a horizontal legend panel mapping compute-state colors to labels.
	 * <p>
	 * The legend contains four entries laid out left to right:
	 * "Not Computed" (blue), "Out of Date" (black), "Computed" (green), "Compute Error" (red).
	 * Each entry pairs a solid color icon with a descriptive text label. The panel is
	 * enclosed in a titled (empty-title) border to visually group it from surrounding content.
	 *
	 * @return a configured JPanel containing the four labeled color legend entries
	 */
	protected JPanel buildLegendPanel() {
		JPanel legendPanel = new JPanel(new GridBagLayout());

		// Surround the legend with an empty-titled border for visual grouping
		legendPanel.setBorder(BorderFactory.createTitledBorder(""));

		// Shared constraint object; individual fields are overridden per entry
		GridBagConstraints gbc = new GridBagConstraints();

		// --- "Not Computed" entry ---
		ColorIcon icon = new ColorIcon(NOT_COMPUTED_COLOR);
		JLabel label = new JLabel("Not Computed");
		label.setIcon(icon);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);

		// --- "Out of Date" entry ---
		icon = new ColorIcon(NEEDS_TO_COMPUTE_COLOR);
		label = new JLabel("Out of Date");
		label.setIcon(icon);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);

		// --- "Computed" entry ---
		icon = new ColorIcon(COMPUTED_COLOR);
		label = new JLabel("Computed");
		label.setIcon(icon);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);

		// --- "Compute Error" entry (REMAINDER spans to end of row) ---
		icon = new ColorIcon(COMPUTED_ERROR_COLOR);
		label = new JLabel("Compute Error");
		label.setIcon(icon);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.001;  // Small positive weight anchors this entry to the top of its cell
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);

		return legendPanel;
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
	 * Replaces the simulation table's model with one built from the given group and
	 * recolors all simulation rows to reflect current compute states.
	 *
	 * Steps performed:
	 * 1. Build a new SimulationTreeTableModel from the group and apply it to the table.
	 * 2. Clear all existing row color overrides.
	 * 3. Iterate every row, resolve the foreground color for WatSimulation rows, and apply it.
	 * 4. Revalidate the table to trigger a layout and repaint pass.
	 *
	 * @param sg the simulation group whose simulations should be shown in the table
	 */
	public void setSimulationTable(AbstractSimulationGroup sg) {
		// Build and install a fresh tree-table model from the provided simulation group
		SimulationTreeTableModel newModel = new SimulationTreeTableModel(sg);
		_simulationTable.setTreeTableModel(newModel);

		// Remove any color overrides left over from the previous model
		_simulationTable.clearColors();

		// Apply compute-state colors to each simulation row
		int rowCnt = _simulationTable.getRowCount();
		for (int r = 0; r < rowCnt; r++) {
			Object val = _simulationTable.getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);

			if (val instanceof WatSimulation) {
				WatSimulation sim = (WatSimulation) val;
				Color color = getSimForegroundColor(sim);
				_simulationTable.setRowForeground(r, color);
			}
		}

		// Trigger a layout recalculation to account for the new model data
		_simulationTable.revalidate();
	}


	/**
	 * Resolves the foreground row color that represents a simulation's current compute state.
	 *
	 * The priority order for state evaluation is:
	 * 1. Not computable             -> NOT_COMPUTED_COLOR  (blue)
	 * 2. Has a compute error        -> COMPUTED_ERROR_COLOR (red)
	 * 3. Computed but out of date   -> NEEDS_TO_COMPUTE_COLOR (black)
	 * 4. Computable and computed    -> COMPUTED_COLOR (green)
	 * 5. Fallthrough (not computed) -> NOT_COMPUTED_COLOR (blue)
	 *
	 * @param sim the simulation whose state should be evaluated; must not be null
	 * @return the Color to use as the row foreground for this simulation
	 */
	private Color getSimForegroundColor(WatSimulation sim) {
		if (!sim.isComputable()) {
			// Simulation cannot be run (e.g. missing required inputs)
			return NOT_COMPUTED_COLOR;

		} else if (sim.hasComputeError()) {
			// Most recent compute attempt failed
			return COMPUTED_ERROR_COLOR;

		} else if (sim.hasComputed() && sim.needToCompute()) {
			// Results exist but inputs have changed since the last run
			return NEEDS_TO_COMPUTE_COLOR;

		} else if (sim.isComputable() && sim.hasComputed()) {
			// Successfully computed and still up to date
			return COMPUTED_COLOR;

		} else {
			// No results yet and no error; treat as not yet computed
			return NOT_COMPUTED_COLOR;
		}
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
	 * Updates the foreground row colors in the simulation table to reflect the current
	 * compute state of each simulation. Rows containing a WatSimulation object are colored
	 * using the simulation's state-derived foreground color. If the system property
	 * "NoSimulationComputeState" is set to true, all coloring is suppressed and the method
	 * returns immediately after clearing existing colors. The table is repainted at the end
	 * to flush all color changes to the screen. This method overrides the base class
	 * implementation to provide simulation-specific compute state visualization.
	 */
	@Override
	public void updateComputeStates() {
		// Clear all existing row color overrides before reapplying
		_simulationTable.clearColors();

		// Allow callers to suppress compute-state coloring via a system property
		if (Boolean.getBoolean("NoSimulationComputeState")) {
			return;
		}

		// Declare variables for the current simulation, its foreground color, and the cell value
		WatSimulation sim;
		Color fgColor;
		Object val;

		// Clear a second time to ensure a clean slate after the property check
		_simulationTable.clearColors();

		// Iterate over every row in the simulation table to apply state-based coloring
		for (int r = 0; r < _simulationTable.getRowCount(); r++) {
			// Retrieve the object in the simulation column for the current row
			val = _simulationTable.getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);

			// Only apply coloring to rows that contain a WatSimulation object
			if (val instanceof WatSimulation) {
				// Cast the cell value to WatSimulation to access its compute state
				sim = (WatSimulation) val;

				// Resolve the foreground color that corresponds to this simulation's compute state
				fgColor = getSimForegroundColor(sim);

				// Apply the resolved color to the current row in the table
				_simulationTable.setRowForeground(r, fgColor);
			}
		}

		// Flush all color changes to the screen
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
