package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;               // AWT color constants used for compute-state row coloring
import java.awt.Cursor;              // Predefined cursors; used to show a wait cursor during slow operations
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.event.MouseEvent;    // Carries mouse position data for tooltip hit-testing in the table

import java.util.ArrayList;          // Resizable-array List implementation for collecting report info objects
import java.util.Date;               // Converts epoch milliseconds to a formatted date string
import java.util.List;               // Generic ordered collection interface

import javax.swing.JButton;          // Swing button used for "Show on Map" and "View Report" cell editors
import javax.swing.JLabel;           // Swing label for static field headings and dynamic value display
import javax.swing.JMenuItem;        // Menu item in the analysis period right-click popup
import javax.swing.JPanel;           // General-purpose Swing container for the right-side detail panel
import javax.swing.JPopupMenu;       // Right-click context menu attached to the analysis period label
import javax.swing.JScrollPane;      // Scroll pane wrapping the status list
import javax.swing.JSeparator;       // Horizontal visual divider between the group info and simulation table sections
import javax.swing.tree.MutableTreeNode; // Tree node interface used to locate the analysis period node in the project tree

import com.rma.client.Browser;           // Provides access to the RMA browser frame and project tree
import com.rma.event.ModifiableListener; // Listener interface notified when a Modifiable object's state changes
import com.rma.model.ManagerProxy;       // Lightweight proxy wrapping a managed model object; used in deletion callbacks

import hec.gui.NameDescriptionPanel;     // HEC panel displaying Name and Description fields for a model object

import hec2.wat.client.WatFrame;         // WAT application frame providing access to the project tree for AP node lookup
import hec2.wat.model.WatAnalysisPeriod; // WAT analysis period model; provides run-time window start and end times
import hec2.wat.model.WatSimulation;     // WAT simulation model object containing compute state, paths, and metadata
import hec2.wat.ui.WatAnalysisPeriodNode; // Project tree node for a WatAnalysisPeriod; used to open its editor

import rma.lang.Modifiable;              // Interface implemented by objects that can notify listeners of modifications
import rma.swing.RmaInsets;              // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJList;               // RMA-enhanced JList component
import rma.swing.list.RmaListModel;      // RMA list model used as the backing model for the status list

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;              // Singleton plugin entry point providing access to the actions window
import usbr.wat.plugins.actionpanel.ActionsPanel;                   // Left-side panel containing action buttons for the simulation group
import usbr.wat.plugins.actionpanel.ActionsWindow;                  // Top-level WTMP actions window that owns this panel
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;         // Bottom panel hosting simulation-level action buttons
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;  // Base model class for all simulation group types
import usbr.wat.plugins.actionpanel.model.ResultsData;              // Model object representing a saved simulation results snapshot
import usbr.wat.plugins.actionpanel.model.SimulationGroup;          // Standard (non-forecast) simulation group model
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;     // Data transfer object carrying per-simulation report metadata
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;    // Custom tree-table component displaying simulations and their results
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel; // Tree-table model backing the simulation table


/**
 * Panel that displays calibration simulation group details and provides controls
 * for running and reporting on calibration simulations.
 *
 * This panel extends AbstractSimulationPanel and composes the full calibration
 * workflow UI into two horizontal regions:
 *
 *   Left region  -- ActionsPanel providing group-level action buttons.
 *   Right region -- A detail panel containing:
 *     A read-only name/description display for the selected simulation group.
 *     Analysis period name, start time, and end time labels with a right-click
 *     "Edit..." popup that opens the analysis period editor.
 *     A simulation tree-table with "Show on Map" and "View Report" button columns.
 *     A conditional color-coded compute-state legend.
 *     A SimulationActionsPanel with simulation-level action buttons.
 *     A scrollable status list for output messages.
 *
 * The panel subscribes to modification events on the active WatAnalysisPeriod so
 * that its displayed start and end times update automatically if the period is edited
 * while this panel is visible.
 *
 */
@SuppressWarnings("serial")
public class CalibrationPanel extends AbstractSimulationPanel {
	// --- Compute-state row foreground colors (shadow the constants in the superclass) ---

	/**
	 * Row color indicating a simulation has never been computed.
	 */
	private static final Color NOT_COMPUTED_COLOR = Color.BLUE;

	/**
	 * Row color indicating a simulation has been successfully computed.
	 */
	private static final Color COMPUTED_COLOR = Color.GREEN.darker();

	/**
	 * Row color indicating the most recent compute attempt ended with an error.
	 */
	private static final Color COMPUTED_ERROR_COLOR = Color.RED;

	/**
	 * Row color indicating a simulation was previously computed but is now out of date.
	 */
	private static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;


	// --- Child components ---

	/**
	 * Left-side panel that holds the group-level action buttons.
	 */
	private ActionsPanel _actionsPanel;

	/**
	 * Container for the right-side detail content (group info, table, legend, etc.).
	 */
	private JPanel _rightPanel;

	/**
	 * Label displaying the name of the currently selected analysis period.
	 */
	private JLabel _apLabel;

	/**
	 * Scrollable list used to display status and log messages.
	 */
	private RmaJList _statusList;

	/**
	 * Read-only HEC name/description panel showing the simulation group name and description.
	 */
	private NameDescriptionPanel _nameDescPanel;

	/**
	 * Label displaying the analysis period start time.
	 */
	private JLabel _apStartLabel;

	/**
	 * Label displaying the analysis period end time.
	 */
	private JLabel _apEndLabel;

	/**
	 * The simulation group currently displayed by this panel.
	 */
	private SimulationGroup _simGroup;

	/**
	 * Toolbar panel for selecting and managing calibration simulation groups.
	 */
	private CalibrationSimulationGroupPanel _simPanel;

	/**
	 * Listener that refreshes the analysis period display fields whenever the
	 * active WatAnalysisPeriod reports a modification (e.g. a time window change).
	 */
	private ModifiableListener _apModListener;

	/**
	 * The WatAnalysisPeriod currently observed by this panel.
	 * Retained so the listener can be detached before a new period is attached.
	 */
	private WatAnalysisPeriod _ap;


	/**
	 * Constructs the CalibrationPanel and builds all child controls.
	 *
	 * @param parent the ActionsWindow that owns this panel; passed to child components
	 *               that need access to the top-level window
	 */
	public CalibrationPanel(ActionsWindow parent) {
		super(parent);
		buildControls(parent);
	}


	/**
	 * Builds the top-level layout of the panel, consisting of the simulation group
	 * toolbar, the left actions panel, and the right detail panel.
	 *
	 * Layout from top to bottom and left to right:
	 * CalibrationSimulationGroupPanel spanning the full width.
	 * ActionsPanel in the left column, filling vertically.
	 * Right detail panel in the right column, filling both directions.
	 *
	 * @param parent the ActionsWindow passed down to ActionsPanel for button context
	 */
	private void buildControls(ActionsWindow parent) {
		// --- Simulation group selection toolbar (full-width top row) ---
		_simPanel = new CalibrationSimulationGroupPanel(this);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simPanel, gbc);

		// --- Left column: action buttons panel, fills remaining vertical space ---
		_actionsPanel = new ActionsPanel(parent, this);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_actionsPanel, gbc);

		// --- Right column: detail panel, expands to fill all remaining space ---
		_rightPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = GridBagConstraints.REMAINDER;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_rightPanel, gbc);

		// Populate the right panel with all detail controls
		buildRightPanel();
	}


	/**
	 * Populates the right detail panel with the name/description display, analysis
	 * period info labels, simulation tree-table, compute-state legend, simulation
	 * action buttons, and status list.
	 *
	 * The compute-state legend is only added when the system property
	 * "NoSimulationComputeState" is not set, allowing deployments that do not
	 * compute simulations to suppress the color legend entirely.
	 *
	 * The simulation table overrides getToolTipText to delegate to the shared
	 * AbstractSimulationPanel tooltip logic, and overrides setValueAt to trigger
	 * an action-button state refresh whenever a checkbox in the selection column changes.
	 */
	private void buildRightPanel() {
		// --- Name/description display for the selected simulation group ---
		_nameDescPanel = new NameDescriptionPanel();
		_nameDescPanel.setPanelEditable(false);
		_nameDescPanel.setNameLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5005;
		_rightPanel.add(_nameDescPanel, gbc);

		// --- "Analysis Period:" static label ---
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(label, gbc);

		// --- Analysis period name label with right-click "Edit..." popup ---
		_apLabel = new JLabel();
		_apLabel.setComponentPopupMenu(getApPopupMenu());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(_apLabel, gbc);

		// --- "Start Time:" label (indented via custom insets) ---
		label = new JLabel("Start Time:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(5, 15, 0, 5);  // Extra left inset visually indents under "Analysis Period:"
		_rightPanel.add(label, gbc);

		// --- Analysis period start time value label ---
		_apStartLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(_apStartLabel, gbc);

		// --- "End Time:" label (indented via custom insets) ---
		label = new JLabel("End Time:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(5, 15, 0, 5);  // Extra left inset mirrors the Start Time indent
		_rightPanel.add(label, gbc);

		// --- Analysis period end time value label ---
		_apEndLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(_apEndLabel, gbc);

		// --- Horizontal separator between the group info block and the simulation table ---
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);

		// --- "Simulations:" section label ---
		label = new JLabel("Simulations:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(label, gbc);

		// --- Simulation tree-table with four columns: Selected, Simulation, Map, Report ---
		// Anonymous subclass overrides tooltip and checkbox-change behaviour
		String[] headers = new String[]{"Selected", "Simulation", "Map", "Report"};
		_simulationTable = new SimulationTreeTable(this) {
			/**
			 * Delegates tooltip text for a hovered cell to the shared
			 * AbstractSimulationPanel implementation which inspects the tree node type.
			 *
			 * @param e the mouse event carrying the cursor position
			 * @return the tooltip string for the cell under the cursor, or null
			 */
			@Override
			public String getToolTipText(MouseEvent e) {
				return getTableToolTipText(e);
			}

			/**
			 * Intercepts value changes so that a checkbox toggle in the selection
			 * column triggers an immediate action-button state refresh.
			 *
			 * @param value the new cell value
			 * @param row   the row index of the edited cell
			 * @param col   the column index of the edited cell
			 */
			@Override
			public void setValueAt(Object value, int row, int col) {
				super.setValueAt(value, row, col);

				// Refresh enabled state of action buttons when a row's checkbox changes
				if (col == SimulationTreeTableModel.SELECTED_COLUMN) {
					tableCheckBoxAction();
				}
			}
		};

		// Set column widths: Simulation column is widest; Map and Report columns are equal
		_simulationTable.setColumnWidths(350, 150, 110, 110);

		// Increase row height slightly for easier readability and click targets
		_simulationTable.setRowHeight(_simulationTable.getRowHeight() + 5);

		// Wire up the "Show on Map" button in the Map column (column index 2)
		JButton button = _simulationTable.setButtonCellEditor(2);
		button.addActionListener(e -> displaySimulationInMap());
		button.setText("Show on Map");

		// Wire up the "View Report" button in the Report column (column index 3)
		button = _simulationTable.setButtonCellEditor(3);
		button.setText("View Report");
		button.addActionListener(e -> displayReport());

		// Add the table inside its scroll pane, expanding to fill remaining space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(_simulationTable.getScrollPane(), gbc);

		// --- Color legend (conditionally shown based on system property) ---
		if (!Boolean.getBoolean("NoSimulationComputeState")) {
			JPanel legendPanel = buildLegendPanel();
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = RmaInsets.INSETS5505;
			_rightPanel.add(legendPanel, gbc);
		}

		// --- Simulation action buttons panel ---
		_simActionsPanel = new SimulationActionsPanel(_parentWindow, this);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		_rightPanel.add(_simActionsPanel, gbc);

		// --- Scrollable status/message list ---
		_statusList = new RmaJList<>(new RmaListModel<>(false));
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5555;
		_rightPanel.add(new JScrollPane(_statusList), gbc);

		// --- Analysis period modification listener ---
		// Refreshes the displayed AP fields whenever the analysis period itself is edited
		_apModListener = new ModifiableListener() {
			/**
			 * Called when the currently observed WatAnalysisPeriod reports a state change.
			 * Re-reads and displays the latest name and time window values.
			 *
			 * @param modifiable the object whose modified state changed
			 * @param b          the new modified flag value
			 */
			@Override
			public void modifiedStateChanged(Modifiable modifiable, boolean b) {
				fillAnalysisPeriodFields(_ap);
			}
		};
	}


	/**
	 * Builds and returns a popup menu for the Analysis Period table entry.
	 * The menu contains a single "Edit..." item that opens the analysis period
	 * editor when clicked. A tooltip is attached to the menu item to describe
	 * its action to the user.
	 *
	 * @return a JPopupMenu containing the Analysis Period edit action
	 */
	private JPopupMenu getApPopupMenu() {
		// Create a new empty popup menu to hold the Analysis Period actions
		JPopupMenu popup = new JPopupMenu();

		// Create the "Edit..." menu item for opening the analysis period editor
		JMenuItem editAp = new JMenuItem("Edit...");

		// Attach a tooltip to describe the menu item's action to the user
		editAp.setToolTipText("Edit the Analysis Period");

		// Register an action listener that invokes the analysis period editor on click
		editAp.addActionListener(e -> editAnalysisPeriod());

		// Add the edit menu item to the popup and return the completed menu
		popup.add(editAp);
		return popup;
	}


	/**
	 * Opens the WAT analysis period editor for the currently displayed analysis period.
	 *
	 * Looks up the tree node for the active WatAnalysisPeriod in the project tree
	 * and calls its editManager method to open the editor dialog. Does nothing if
	 * no analysis period is currently set or if the node is not a WatAnalysisPeriodNode.
	 */
	private void editAnalysisPeriod() {
		// Guard: no analysis period is currently displayed
		if (_ap == null) {
			return;
		}

		// Locate the project tree node corresponding to the active analysis period
		MutableTreeNode node = ((WatFrame) Browser.getBrowserFrame())
				.getProjectTree()
				.getNodeForManager(_ap);

		if (node instanceof WatAnalysisPeriodNode) {
			WatAnalysisPeriodNode apNode = (WatAnalysisPeriodNode) node;

			// Open the analysis period editor through the tree node's built-in mechanism
			apNode.editManager();
		}
	}


	/**
	 * Clears all displayed data from the panel, resetting labels and the simulation
	 * table to an empty state.
	 *
	 * Called before loading a new simulation group to prevent stale data from being
	 * visible during the transition.
	 */
	public void clearForm() {
		// Clear all analysis period display fields
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");

		// Clear the simulation group name and description
		_nameDescPanel.setName("");
		_nameDescPanel.setDescription("");

		// Replace the simulation table model with an empty model (null group = no rows)
		_simulationTable.setTreeTableModel(new SimulationTreeTableModel(null));
	}


	/**
	 * Loads the given simulation group into the panel, populating the name/description
	 * display, analysis period fields, and simulation table.
	 *
	 * A wait cursor is shown for the duration of the load. If asg is not a
	 * SimulationGroup the analysis period fields are blanked. The actions panel is
	 * always updated with the new group regardless of type.
	 *
	 * @param asg the simulation group to display; pass null or a non-SimulationGroup
	 *            type to clear the analysis period fields
	 */
	@Override
	public void setSimulationGroup(AbstractSimulationGroup asg) {
		// Show a wait cursor while the table and fields are being populated
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_simGroup = (SimulationGroup) asg;

		try {
			// Clear any previously displayed data before loading the new group
			clearForm();

			if (asg instanceof SimulationGroup) {
				SimulationGroup sg = (SimulationGroup) asg;

				// Populate the simulation tree-table with the group's simulations
				setSimulationTable(sg);

				// Display the group name and description; make them read-only but enabled
				_nameDescPanel.setName(sg.getName());
				_nameDescPanel.setDescription(sg.getDescription());
				_nameDescPanel.setPanelEditable(true);
				_nameDescPanel.setPanelEnabled(true);

				// Populate analysis period name, start, and end time labels
				WatAnalysisPeriod ap = sg.getAnalysisPeriod();
				fillAnalysisPeriodFields(ap);

			} else {
				// Non-SimulationGroup type provided; clear the analysis period fields
				_apLabel.setText("");
				_apStartLabel.setText("");
				_apEndLabel.setText("");
			}

			// Always update the actions panel so its button states reflect the new group
			_actionsPanel.setSimulationGroup(asg);

		} finally {
			// Restore the default cursor even if an exception occurs during load
			setCursor(Cursor.getDefaultCursor());
		}
	}


	/**
	 * Populates the analysis period display labels with the name, start time, and end time
	 * of the provided WatAnalysisPeriod. If the period is null, all labels are cleared to
	 * empty strings. The method also manages the modifiable listener lifecycle: the listener
	 * is removed from the previously tracked period and added to the new one, ensuring that
	 * stale callbacks are not triggered when the analysis period changes.
	 *
	 * @param ap the WatAnalysisPeriod whose data will be displayed, or null to clear the fields
	 */
	private void fillAnalysisPeriodFields(WatAnalysisPeriod ap) {
		// Initialize display strings to empty in case the provided period is null
		String apName = "";
		String apStart = "";
		String apEnd = "";

		// If a valid period was provided, extract its name and run time window bounds
		if (ap != null) {
			apName = ap.getName();
			apStart = ap.getRunTimeWindow().getStartTime().toString();
			apEnd = ap.getRunTimeWindow().getEndTime().toString();
		}

		// Update the three display labels with the resolved strings
		_apLabel.setText(apName);
		_apStartLabel.setText(apStart);
		_apEndLabel.setText(apEnd);

		// Detach the listener from the previously observed period to prevent stale callbacks
		if (_ap != null) {
			_ap.removeModifiableListener(_apModListener);
		}

		// Update the tracked period reference and attach the listener to the new period
		_ap = ap;

		// Only attach the listener if the new period is non-null
		if (_ap != null) {
			_ap.addModifiableListener(_apModListener);
		}
	}


	/**
	 * Returns the SimulationGroup currently displayed by this panel.
	 *
	 * @return the active SimulationGroup, or null if none is set
	 */
	@Override
	public SimulationGroup getSimulationGroup() {
		return _simGroup;
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
	 * Builds and returns a list of SimulationReportInfo objects for all rows selected
	 * in the simulation table. The method performs two passes: the first collects report
	 * info from selected WatSimulation rows, and the second collects report info from
	 * selected ResultsData rows. Each SimulationReportInfo is populated with the simulation
	 * reference, DSS file path, output folder, name, description, last computed date, and
	 * simulation group. ResultsData entries use a combined display name and resolve their
	 * DSS file path relative to the results folder rather than the simulation directory.
	 * This method overrides the base class implementation to provide simulation-specific
	 * report info gathering behavior.
	 *
	 * @return a list of SimulationReportInfo objects representing all selected simulations
	 *         and results snapshots, in selection order
	 */
	@Override
	public List<SimulationReportInfo> getSimulationReportInfos() {
		// Initialize the list that will hold report info for all selected rows
		List<SimulationReportInfo> simInfos = new ArrayList<>();

		// Collect both selection types from the table
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
			simInfo.setSimulationGroup(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

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

			// Locate the DSS file inside the results folder since the simulation's
			// own DSS path may point to a different directory
			simInfo.setSimDssFile(findSimulationDssFile(results.getFolder(), results.getSimulation().getSimulationDssFile()));

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
			simInfo.setSimulationGroup(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

			// Add the fully populated results report info to the output list
			simInfos.add(simInfo);
		}

		// Return the complete list of report info objects for all selected rows
		return simInfos;
	}


	/**
	 * Returns the CalibrationSimulationGroupPanel toolbar hosted at the top of this panel.
	 *
	 * @return the simulation group selection panel; never null after construction
	 */
	public CalibrationSimulationGroupPanel getSimulationPanel() {
		return _simPanel;
	}


	/**
	 * Handles the deletion of a simulation group by updating the group selection panel
	 * and disabling the name/description display.
	 *
	 * Delegates the combo-box removal to CalibrationSimulationGroupPanel, then disables
	 * the name/description panel to prevent editing a now-invalid group.
	 *
	 * @param proxy the ManagerProxy of the simulation group that was deleted
	 */
	public void simulationGroupDeleted(ManagerProxy proxy) {
		// Remove the deleted group from the combo box in the toolbar panel
		_simPanel.simulationGroupDeleted(proxy);

		// Disable the name/description panel since no valid group is selected
		_nameDescPanel.setEnabled(false);
	}

}
