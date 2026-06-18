package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.EventQueue;                         // Provides EventQueue for dispatching UI updates to the Swing Event Dispatch Thread
import java.awt.GridBagConstraints;                 // Provides GridBagConstraints for controlling component placement within a GridBagLayout
import java.awt.GridBagLayout;                      // Provides GridBagLayout, a flexible grid-based layout manager for Swing panels
import java.awt.event.MouseEvent;                   // Provides MouseEvent for receiving mouse interaction events on the simulation table

import java.util.ArrayList;                         // Provides ArrayList for mutable lists of EnsembleSet objects and selected row indices
import java.util.Arrays;                            // Provides Arrays for converting a String array to a List when processing member sets
import java.util.Collections;                       // Provides Collections for sorting the computed ensemble member list numerically
import java.util.HashSet;                           // Provides HashSet for deduplicating ensemble member strings before sorting
import java.util.Iterator;                          // Provides Iterator for traversing the sorted member list when building a CSV string
import java.util.List;                              // Provides the List interface for ordered collections of EnsembleSet and related types
import java.util.Set;                               // Provides the Set interface used for deduplicating computed ensemble members
import java.util.StringTokenizer;                   // Provides StringTokenizer (imported but reserved for potential classpath parsing use)
import java.util.Vector;                            // Provides Vector for constructing table row data passed to RmaJTable.appendRow

import javax.swing.JButton;                         // Provides JButton for the Edit Ensemble Set action button
import javax.swing.JLabel;                          // Provides JLabel for displaying Analysis Period name, start, and end time fields
import javax.swing.JMenuItem;                       // Provides JMenuItem for items in the Analysis Period right-click popup menu
import javax.swing.JPanel;                          // Provides JPanel for the button/controls sub-panel below the ensemble table
import javax.swing.JPopupMenu;                      // Provides JPopupMenu for the right-click context menu on the Analysis Period label
import javax.swing.JSeparator;                      // Provides JSeparator for the horizontal rule drawn between the AP fields and the simulation table
import javax.swing.ListSelectionModel;              // Provides ListSelectionModel for configuring single-row selection on the simulation table
import javax.swing.SwingConstants;                  // Provides SwingConstants for the RIGHT alignment constant used in the ensemble table
import javax.swing.event.DocumentEvent;             // Provides DocumentEvent for receiving text change events from the ensemble members field
import javax.swing.event.DocumentListener;          // Provides DocumentListener for tracking text insertions and removals in the members field
import javax.swing.event.ListSelectionEvent;        // Provides ListSelectionEvent for detecting row selection changes in the simulation table
import javax.swing.table.TableColumn;               // Provides TableColumn for accessing and configuring individual columns in the ensemble table
import javax.swing.table.TableColumnModel;          // Provides TableColumnModel for accessing the ordered set of columns in a JTable
import javax.swing.tree.MutableTreeNode;            // Provides MutableTreeNode for locating the Analysis Period node in the WAT project tree

import com.rma.client.Browser;                      // Provides Browser for accessing the WAT application frame and its project tree
import com.rma.event.ModifiableListener;            // Provides ModifiableListener for responding to Analysis Period modification events
import hec.util.NumericComparator;                  // Provides NumericComparator for sorting ensemble member strings by numeric value
import hec2.wat.client.WatFrame;                    // Provides WatFrame for casting the browser frame to access the WAT project tree
import hec2.wat.model.WatAnalysisPeriod;            // Provides WatAnalysisPeriod for reading the run time window start and end times
import hec2.wat.model.WatSimulation;                // Provides WatSimulation for representing a WAT simulation selected from the tree table
import hec2.wat.ui.WatAnalysisPeriodNode;           // Provides WatAnalysisPeriodNode for opening the Analysis Period editor dialog
import rma.lang.Modifiable;                         // Provides Modifiable, the interface whose implementation signals that an object has changed
import rma.swing.EnabledJPanel;                     // Provides EnabledJPanel, a JPanel that propagates enable/disable state to its children
import rma.swing.RmaInsets;                         // Provides RmaInsets constants for consistent padding used in GridBagConstraints
import rma.swing.RmaJCheckBox;                      // Provides RmaJCheckBox for the "Recompute All" checkbox with RMA enable/disable support
import rma.swing.RmaJIntegerField;                  // Provides RmaJIntegerField (imported for integer field support in related table editors)
import rma.swing.RmaJIntegerSetField;               // Provides RmaJIntegerSetField for the comma-separated integer set editor in the ensemble table
import rma.swing.RmaJTable;                         // Provides RmaJTable, an RMA-extended JTable with row management and editor conveniences
import rma.swing.table.ColumnGroup;                 // Provides ColumnGroup for grouping all ensemble table columns under a shared simulation name header
import rma.swing.table.GroupableTableHeader;        // Provides GroupableTableHeader for rendering multi-level column group headers in the ensemble table
import rma.swing.table.MleHeadRenderer;             // Provides MleHeadRenderer for multi-line column header rendering in the ensemble table
import rma.swing.table.RmaCellEditor;               // Provides RmaCellEditor for wrapping RmaJIntegerSetField as a table cell editor
import rma.swing.table.RmaTableModel;               // Provides RmaTableModel for firing row-update events after ensemble member edits
import rma.swing.table.RmaTableModelInterface;      // Provides RmaTableModelInterface for setting column class metadata on the table model

import rma.util.IntArray;                           // Provides IntArray (imported for potential integer array operations in related utilities)
import rma.util.IntVector;                          // Provides IntVector for storing the list of previously computed ensemble members per EnsembleSet
import rma.util.RMAIO;                              // Provides RMAIO for string manipulation utilities such as removeChar

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Provides ActionPanelPlugin for accessing the singleton plugin instance and its window
import usbr.wat.plugins.actionpanel.ActionsWindow;                              // Provides ActionsWindow, the top-level plugin window that owns this panel
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;                     // Provides SimulationActionsPanel for the compute/run action buttons at the bottom
import usbr.wat.plugins.actionpanel.actions.forecast.EditEnsembleSetAction;     // Provides EditEnsembleSetAction, the Swing Action for the Edit Ensemble Set button
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;              // Provides AbstractSimulationGroup, the base type for simulation groupings
import usbr.wat.plugins.actionpanel.model.ResultsData;                          // Provides ResultsData for retrieving the currently selected simulation results
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;                 // Provides EnsembleSet, the model object linking boundary conditions to a set of ensemble members
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Provides ForecastSimGroup, the top-level model for a forecast simulation group
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;                 // Provides AbstractSimulationPanel, the base class supplying the simulation tree table and legend
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                               // Provides UsbrPanel, the marker interface for USBR-specific panel implementations
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;                // Provides SimulationTreeTable, the custom tree-table used to display WAT simulations
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;           // Provides SimulationTreeTableModel for accessing column index constants on the tree-table model

/**
 * Panel that forms the Simulation tab within the Forecast Action Panel. It displays:
 *
 * 1. An Analysis Period section showing the active AP name, start time, and end time,
 *    with a right-click popup to open the AP editor.
 * 2. A SimulationTreeTable listing all WAT simulations with "Show on Map" and
 *    "View Report" buttons per row.
 * 3. A simulation ensemble table where the user can select EnsembleSets to run,
 *    specify target member indices, and view previously computed members.
 * 4. An "Edit Ensemble Set" button, a "Recompute All" checkbox, and a
 *    SimulationActionsPanel with compute/run action buttons.
 *
 * Selecting a row in the simulation tree-table drives the ensemble table: column
 * headers update to the selected simulation's name and the ensemble rows are
 * refreshed. A DocumentListener on the ensemble members field controls whether the
 * row's "Selected to Run" checkbox may be enabled.
 *
 * @see AbstractSimulationPanel
 * @see EnsembleSet
 * @see ForecastSimGroup
 */
public class SimulationPanel extends AbstractSimulationPanel
		implements UsbrPanel {
	/**
	 * Column index in the ensemble table that shows previously computed ensemble members.
	 */
	private static final int COMPUTED_MEMBERS_COL = 4;

	/**
	 * Panel that groups the Analysis Period labels and the simulation tree-table.
	 */
	private EnabledJPanel _topPanel;

	/**
	 * Label showing the name of the currently active Analysis Period.
	 */
	private JLabel _apLabel;

	/**
	 * Label showing the start time of the active Analysis Period's run time window.
	 */
	private JLabel _apStartLabel;

	/**
	 * Label showing the end time of the active Analysis Period's run time window.
	 */
	private JLabel _apEndLabel;

	/**
	 * The parent ForecastPanel that owns this tab and provides access to the simulation group.
	 */
	private ForecastPanel _parentPanel;

	/**
	 * Table displaying ensemble sets for the currently selected simulation. Columns are:
	 * Selected to Run, Boundary Conditions, Temperature Target Set,
	 * Target Members To Run, and Target Members Previously Run.
	 */
	private RmaJTable _simEnsembleTable;

	/**
	 * Button that opens the Edit Ensemble Set dialog for the current simulation.
	 */
	private JButton _editEnsembleButton;

	/**
	 * Column group that spans all ensemble table columns; its header text shows the simulation name.
	 */
	private ColumnGroup _columnGroup;

	/**
	 * Checkbox that, when selected, causes all ensemble members to be recomputed regardless of prior results.
	 */
	private RmaJCheckBox _recomputeAllChk;

	/**
	 * Ordered list of EnsembleSet objects currently displayed in the ensemble table, parallel to its rows.
	 */
	private List<EnsembleSet> _esetsInTable = new ArrayList<>();

	/**
	 * Integer-set cell editor for the "Target Members To Run" column; also used to track live text input.
	 */
	private RmaJIntegerSetField _ensembleMembersFld;

	/**
	 * Tracks whether the "Selected to Run" checkbox for the currently editing row
	 * may be enabled. Set to true when the members field is non-empty.
	 */
	private boolean _enabledCheckBox;

	/**
	 * The WatAnalysisPeriod currently displayed; kept to attach/detach the modification listener.
	 */
	private WatAnalysisPeriod _ap;

	/**
	 * Listener that refreshes the AP label fields whenever the Analysis Period is modified.
	 */
	private ModifiableListener _apModListener;

	/**
	 * Constructs a new SimulationPanel, wires it to the parent window and forecast panel,
	 * builds all UI controls, and registers all event listeners.
	 *
	 * @param parentWindow the ActionsWindow that owns this panel
	 * @param parentPanel  the ForecastPanel that provides the active simulation group
	 */
	public SimulationPanel(ActionsWindow parentWindow, ForecastPanel parentPanel) {
		super(parentWindow);
		_parentPanel = parentPanel;

		// Build all UI components before registering listeners
		buildControls();
		addListeners();
	}

	/**
	 * Creates the top-level EnabledJPanel with a GridBagLayout and delegates
	 * the population of its child controls to buildTopPanel.
	 */
	private void buildControls() {
		_topPanel = new EnabledJPanel(new GridBagLayout());
		buildTopPanel(_topPanel);
	}

	/**
	 * Populates the top panel with all UI components: Analysis Period labels,
	 * the simulation tree-table with "Show on Map" and "View Report" buttons,
	 * an optional compute-state legend, the ensemble set table, the Edit Ensemble
	 * Set button, the Recompute All checkbox, and the SimulationActionsPanel.
	 * Also creates the ModifiableListener that refreshes AP labels on AP changes.
	 *
	 * @param topPanel the JPanel into which all controls are added
	 */
	private void buildTopPanel(JPanel topPanel) {
		// Add the top panel itself to this SimulationPanel, filling all available space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(topPanel, gbc);

		// "Analysis Period:" static label
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);

		// Dynamic label showing the AP name; right-click opens the edit popup menu
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
		topPanel.add(_apLabel, gbc);

		// "Start Time:" static label, indented to visually group it with the AP section
		label = new JLabel("Start Time:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(5, 15, 0, 5);
		topPanel.add(label, gbc);

		// Dynamic label showing the AP run time window start time
		_apStartLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		topPanel.add(_apStartLabel, gbc);

		// "End Time:" static label, indented alongside Start Time
		label = new JLabel("End Time:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(5, 15, 0, 5);
		topPanel.add(label, gbc);

		// Dynamic label showing the AP run time window end time
		_apEndLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		topPanel.add(_apEndLabel, gbc);

		// Horizontal separator dividing the AP section from the simulations section
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);

		// "Simulations:" section header label
		label = new JLabel("Simulations:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);

		// Build the simulation tree-table with tooltip and checkbox-action overrides
		String[] headers = new String[]{"Selected", "Simulation", "Map", "Report"};
		_simulationTable = new SimulationTreeTable(this) {
			@Override
			public String getToolTipText(MouseEvent e) {
				// Delegate tooltip generation to the superclass helper
				return getTableToolTipText(e);
			}

			@Override
			public void setValueAt(Object value, int row, int col) {
				super.setValueAt(value, row, col);

				// When the "Selected" checkbox column changes, trigger the checkbox action handler
				if (col == SimulationTreeTableModel.SELECTED_COLUMN) {
					tableCheckBoxAction();
				}
			}
		};

		// Set column widths for Selected, Simulation, Map, and Report columns
		_simulationTable.setColumnWidths(350, 150, 110, 110);

		// Increase row height slightly for readability
		_simulationTable.setRowHeight(_simulationTable.getRowHeight() + 5);

		// Allow only a single simulation to be selected at a time
		_simulationTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Install the "Show on Map" button in the Map column and wire its action
		JButton button = _simulationTable.setButtonCellEditor(2);
		button.addActionListener(e -> displaySimulationInMap());
		button.setText("Show on Map");

		// Install the "View Report" button in the Report column and wire its action
		button = _simulationTable.setButtonCellEditor(3);
		button.setText("View Report");
		button.addActionListener(e -> displayReport());

		// Add the simulation table's scroll pane, which fills most of the top panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		topPanel.add(_simulationTable.getScrollPane(), gbc);

		// Optionally add the compute-state colour legend unless suppressed by system property
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
			topPanel.add(legendPanel, gbc);
		}

		// Build the ensemble set table with a custom isCellEditable and setValueAt
		headers = new String[]{"Selected\nto Run", "Boundary Conditions", "Temperature Target Set", "Target Members\nTo Run", "Target Members\nPreviously Run"};
		_simEnsembleTable = new RmaJTable(this, headers) {
			@Override
			public boolean isCellEditable(int row, int column) {
				if (column == 0) {
					// The "Selected to Run" checkbox requires a non-null member set to be enabled
					Object obj = getValueAt(row, 3);
					if (obj == null) {
						return false;
					}

					// While the row is being edited, also consult the live _enabledCheckBox flag
					if (getEditingRow() == row) {

					}

					if (obj instanceof String) {
						String str = (String) obj;
						boolean hasData = str.length() > 0;

						// During editing, allow the checkbox if the members field is non-empty
						if (getEditingRow() == row) {
							return hasData || _enabledCheckBox;
						}
						return hasData;
					}
				}

				// Only the "Target Members To Run" column (3) is otherwise editable
				return column == 3;
			}

			public void setValueAt(Object obj, int row, int col) {
				super.setValueAt(obj, row, col);

				if (col == 3) {
					// Notify the model that this row changed so dependent renderers refresh
					RmaTableModel model = (RmaTableModel) getModel();
					model.fireTableRowsUpdated(row, row);

					// Automatically uncheck "Selected to Run" when the member set is cleared
					if (obj == null) {
						super.setValueAt(Boolean.FALSE, row, 0);
					} else if (obj instanceof String) {
						String str = (String) obj;
						if (str.isEmpty()) {
							super.setValueAt(Boolean.FALSE, row, 0);
						}
					}
				}
			}
		};

		// Add the "Ensemble F-Part Mapping..." right-click menu item to the ensemble table
		JMenuItem ensembleIndexingMenu = new JMenuItem("Ensemble F-Part Mapping...");
		ensembleIndexingMenu.addActionListener(e -> displayEnsembleFPartIndexing());
		_simEnsembleTable.addPopupItem(ensembleIndexingMenu, 0);

		// Increase the ensemble table row height for readability
		_simEnsembleTable.setRowHeight(_simEnsembleTable.getRowHeight() + 5);

		// Install a multi-line header renderer so column headers with \n wrap correctly
		MleHeadRenderer renderer = _simEnsembleTable.setMlHeaderRenderer();

		// Install the integer-set cell editor in the "Target Members To Run" column and keep a reference
		_ensembleMembersFld = setIntegerSetCellEditor(_simEnsembleTable, 3);

		// Also install an integer-set editor in the "Previously Run" column (read-only display)
		setIntegerSetCellEditor(_simEnsembleTable, 4);

		// Replace the default table header with a groupable header for multi-level labels
		_simEnsembleTable.setTableHeader(new GroupableTableHeader(_simEnsembleTable.getColumnModel()));
		TableColumnModel cm = _simEnsembleTable.getColumnModel();

		// Group all five columns under a single header that shows the selected simulation's name
		_columnGroup = new ColumnGroup(renderer, "Simulation Name");
		_columnGroup.add(cm.getColumn(0));
		_columnGroup.add(cm.getColumn(1));
		_columnGroup.add(cm.getColumn(2));
		_columnGroup.add(cm.getColumn(3));
		_columnGroup.add(cm.getColumn(4));

		// Install a checkbox cell editor in the "Selected to Run" column (index 0)
		_simEnsembleTable.setCheckBoxCellEditor(0);

		GroupableTableHeader header = (GroupableTableHeader) _simEnsembleTable.getTableHeader();
		header.addColumnGroup(_columnGroup);

		// Add the ensemble table's scroll pane, which expands to fill remaining space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5555;
		add(_simEnsembleTable.getScrollPane(), gbc);

		// Create the sub-panel that holds the Edit button and Recompute All checkbox
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0000;
		add(panel, gbc);

		// Create and add the Edit Ensemble Set button with its associated action
		EditEnsembleSetAction editAction = new EditEnsembleSetAction(this);
		_editEnsembleButton = new JButton(editAction);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(20, 5, 0, 5);
		panel.add(_editEnsembleButton, gbc);

		// Create and add the "Recompute All" checkbox
		_recomputeAllChk = new RmaJCheckBox("Recompute All");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(20, 5, 0, 5);
		panel.add(_recomputeAllChk, gbc);

		// Create and add the SimulationActionsPanel containing the compute/run buttons
		_simActionsPanel = new SimulationActionsPanel(_parentWindow, this);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		add(_simActionsPanel, gbc);

		// Create the AP modification listener that refreshes label text when the AP changes
		_apModListener = new ModifiableListener() {
			@Override
			public void modifiedStateChanged(Modifiable modifiable, boolean b) {
				// Re-read the AP fields whenever the Analysis Period signals a modification
				fillApFields(_ap);
			}
		};
	}

	/**
	 * Builds and returns a JPopupMenu for the Analysis Period label. The menu contains
	 * a single "Edit..." item that opens the WAT Analysis Period editor dialog.
	 *
	 * @return a JPopupMenu with an "Edit..." menu item for the active Analysis Period
	 */
	private JPopupMenu getApPopupMenu() {
		JPopupMenu popup = new JPopupMenu();

		JMenuItem editAp = new JMenuItem("Edit...");
		editAp.setToolTipText("Edit the Analysis Period");
		editAp.addActionListener(e -> editAnalysisPeriod());
		popup.add(editAp);

		return popup;
	}

	/**
	 * Opens the WAT Analysis Period editor for the currently active AP by locating
	 * its node in the WAT project tree and calling editManager on it. Does nothing
	 * when no AP is currently set.
	 */
	private void editAnalysisPeriod() {
		if (_ap == null) {
			return;
		}

		// Retrieve the tree node corresponding to the active AP from the WAT project tree
		MutableTreeNode node = ((WatFrame) Browser.getBrowserFrame()).getProjectTree().getNodeForManager(_ap);

		if (node instanceof WatAnalysisPeriodNode) {
			// Open the AP editor dialog via the tree node
			WatAnalysisPeriodNode apNode = (WatAnalysisPeriodNode) node;
			apNode.editManager();
		}
	}

	/**
	 * Opens the EnsembleFPartMapDlg dialog for configuring the DSS F-Part mapping
	 * between ensemble member indices and their simulation identifiers.
	 */
	private void displayEnsembleFPartIndexing() {
		EnsembleFPartMapDlg dlg = new EnsembleFPartMapDlg(_parentWindow);

		// Pre-fill the dialog with the current simulation group and selected simulation
		dlg.fillForm(_parentPanel.getSimulationGroup(), _parentPanel.getSelectedSimulation());
		dlg.setVisible(true);
	}

	/**
	 * Installs an RmaJIntegerSetField as the cell editor for the specified column
	 * in the given table. Also configures the column's display unit system, click-to-
	 * start count, horizontal alignment, and table model column class.
	 *
	 * @param table the RmaJTable in which to install the cell editor
	 * @param col   the zero-based column index that should receive the integer-set editor
	 * @return the RmaJIntegerSetField installed as the editor, or null if the column
	 * index is out of bounds or the column cannot be retrieved
	 */
	private RmaJIntegerSetField setIntegerSetCellEditor(RmaJTable table, int col) {
		TableColumnModel tcm = table.getColumnModel();

		// Guard against invalid column indices before attempting to configure the editor
		if (col < tcm.getColumnCount() && col >= 0) {
			TableColumn tc = table.getColumnModel().getColumn(col);
			if (tc == null) {
				return null;
			} else {
				// Create the integer-set field and register the table as a mouse listener
				RmaJIntegerSetField df = new RmaJIntegerSetField();
				df.addMouseListener(table);

				// Wrap the field in an RmaCellEditor and apply the table's display unit system
				RmaCellEditor dcf = new RmaCellEditor(df);
				dcf.setDisplayUnitSystem(table.getDisplayUnitSystem());

				// Match the click count required to start editing to the table's global setting
				dcf.setClickCountToStart(table.getClickCountToStart());
				tc.setCellEditor(dcf);

				// Right-align numeric content in the column
				table.setHorizontalAlignment(SwingConstants.RIGHT, col);

				// Declare the column's data type as Number so the model can handle it correctly
				if (table.getModel() instanceof RmaTableModelInterface) {
					((RmaTableModelInterface) table.getModel()).setColumnClass(col, Number.class);
				}

				return df;
			}
		}
		return null;
	}

	/**
	 * Registers all event listeners required by this panel:
	 * a DocumentListener on the ensemble members field to control checkbox enablement,
	 * and a ListSelectionListener on the simulation table to refresh the ensemble rows.
	 */
	private void addListeners() {
		// Listen for text changes in the "Target Members To Run" field to enable/disable the row checkbox
		_ensembleMembersFld.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				isRowCheckable();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				isRowCheckable();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// Style-only changes require no action for integer set fields
			}

			/**
			 * Updates the _enabledCheckBox flag based on whether the members field has text.
			 * If the field is empty, the checkbox for the editing row is unchecked immediately.
			 * If non-empty, the table is repainted so the checkbox renderer reflects the new state.
			 */
			private void isRowCheckable() {
				int editingRow = _simEnsembleTable.getEditingRow();

				// No row is currently being edited; nothing to update
				if (editingRow == -1) {
					return;
				}

				String txt = _ensembleMembersFld.getText();

				// The checkbox is only enabled when the members field contains text
				_enabledCheckBox = !txt.isEmpty();

				if (!_enabledCheckBox) {
					// Immediately uncheck the "Selected to Run" cell if the field is cleared
					_simEnsembleTable.setValueAt(Boolean.FALSE, editingRow, 0);
				} else {
					// Repaint to reflect the newly enabled checkbox state in the renderer
					_simEnsembleTable.repaint();
				}
			}
		});

		// Refresh the ensemble table whenever the simulation table selection changes
		_simulationTable.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged(e));
	}

	/**
	 * Filters out intermediate selection events and delegates to the no-argument
	 * tableSelectionChanged when the selection has settled.
	 *
	 * @param e the ListSelectionEvent from the simulation table's selection model
	 */
	private void tableSelectionChanged(ListSelectionEvent e) {
		// Ignore transient events fired while the selection is still adjusting
		if (e.getValueIsAdjusting()) {
			return;
		}
		tableSelectionChanged();
	}

	/**
	 * Responds to a settled simulation table selection change. Saves the current
	 * ensemble member text back to the model, then rebuilds the ensemble table to
	 * reflect the newly selected simulation. Updates the column group header, the
	 * Edit and Recompute All control states, and triggers a table layout refresh.
	 */
	private void tableSelectionChanged() {
		// Commit any in-progress cell edits in the ensemble table before reading its values
		_simEnsembleTable.commitEdit(true);

		int rowCnt = _esetsInTable.size();

		// Persist the current "Target Members To Run" text for each row back to its EnsembleSet
		if (rowCnt > 0) {
			for (int r = 0; r < rowCnt; r++) {
				String members = (String) _simEnsembleTable.getValueAt(r, 3);
				_esetsInTable.get(r).setMemberSetToCompute(members);
			}

			// Mark the simulation group as modified since member sets may have changed
			ForecastSimGroup simGroup = _parentPanel.getSimulationGroup();
			if (simGroup != null) {
				simGroup.setModified(true);
			}
		}

		// Clear the ensemble table and the tracking list before repopulating for the new selection
		int row = _simulationTable.getSelectedRow();
		_simEnsembleTable.deleteCells();
		_esetsInTable.clear();

		if (row == -1) {
			// No simulation selected: update the column header and disable related controls
			_columnGroup.setHeaderValue("No Simulation Selected");
			_editEnsembleButton.setEnabled(false);
			_recomputeAllChk.setEnabled(false);
		} else {
			// A simulation is selected: update the column header to its name and populate ensemble rows
			WatSimulation simulation = (WatSimulation) _simulationTable.getValueAt(row, 0);
			_columnGroup.setHeaderValue(simulation.getName());

			ForecastSimGroup simGroup = _parentPanel.getSimulationGroup();
			List<EnsembleSet> esets = simGroup.getEnsembleSetsFor(simulation);

			_editEnsembleButton.setEnabled(true);
			_recomputeAllChk.setEnabled(true);

			if (esets != null) {
				setEnsembleSets(esets);
			}
		}

		// Force the ensemble table and its header to re-layout and repaint
		_simEnsembleTable.revalidate();
		_simEnsembleTable.getTableHeader().revalidate();
		_simEnsembleTable.getTableHeader().repaint();

		revalidate();
	}

	/**
	 * Returns true if the "Recompute All" checkbox is currently selected,
	 * indicating that all ensemble members should be recomputed regardless of
	 * prior results.
	 *
	 * @return true if recompute-all mode is active; false otherwise
	 */
	public boolean shouldRecomputeAll() {
		return _recomputeAllChk.isSelected();
	}

	/**
	 * Convenience overload that sets the simulation group and always updates the parent panel.
	 *
	 * @param asg the AbstractSimulationGroup to display; may be null to clear
	 */
	public void setSimulationGroup(AbstractSimulationGroup asg) {
		setSimulationGroup(asg, true);
	}

	/**
	 * Updates the panel to display the given simulation group. When the group is a
	 * ForecastSimGroup, the parent panel is optionally updated, the simulation table
	 * and ensemble table are refreshed, the Analysis Period labels are updated, and
	 * the panel is enabled. When the group is not a ForecastSimGroup (or is null),
	 * the parent and tables are cleared similarly.
	 *
	 * @param asg       the AbstractSimulationGroup to display
	 * @param setParent true to also propagate the group to the parent ForecastPanel
	 */
	public void setSimulationGroup(AbstractSimulationGroup asg, boolean setParent) {
		if (asg instanceof ForecastSimGroup) {
			ForecastSimGroup fsg = (ForecastSimGroup) asg;

			// Propagate the new group to the parent panel so other tabs stay in sync
			if (setParent) {
				_parentPanel.setSimulationGroup(fsg);
			}

			// Refresh the simulation table, populate ensemble sets, update AP labels, and enable
			WatSimulation simulation = getSelectedSimulation();
			fillSimulationTable();
			setEnsembleSets(fsg.getEnsembleSets(simulation));
			fillAnalysisWindow();
			setEnabled(true);

			// Select the first simulation row so the ensemble table is populated immediately
			refreshSimTableSelection();

		} else {
			// Clear the parent panel's simulation group when the group is not a ForecastSimGroup
			if (setParent) {
				_parentPanel.setSimulationGroup(null);
			}

			// Populate simulation table with no group (shows empty or default state)
			fillSimulationTable();
			setEnsembleSets(null);
			fillAnalysisWindow();
			setEnabled(true);
		}

		// Always refresh the ensemble table to reflect the new group state
		tableSelectionChanged();
	}

	/**
	 * Selects the first row in the simulation table if any rows are present.
	 * Called after loading a new simulation group to ensure an initial selection.
	 */
	void refreshSimTableSelection() {
		if (_simulationTable.getRowCount() > 0) {
			_simulationTable.setRowSelectionInterval(0, 0);
		}
	}

	/**
	 * Refreshes the analysis period display fields by clearing the current labels and
	 * repopulating them from the simulation group's analysis period. If no simulation
	 * group is available, the labels remain cleared and no further action is taken.
	 */
	void fillAnalysisWindow() {
		// Clear all existing analysis period label text before reloading
		clearApLabels();

		// Retrieve the current simulation group to access its analysis period
		AbstractSimulationGroup fsg = getSimulationGroup();

		// Only populate the fields if a valid simulation group is available
		if (fsg != null) {
			// Retrieve the analysis period associated with the current simulation group
			WatAnalysisPeriod ap = fsg.getAnalysisPeriod();

			// Populate the analysis period display fields with the retrieved data
			fillApFields(ap);
		}
	}

	/**
	 * Populates the analysis period display labels with the name, start time, and end
	 * time of the provided WatAnalysisPeriod. The modifiable listener is detached from
	 * the previously tracked analysis period before being attached to the new one,
	 * preventing stale callbacks. If the provided analysis period is null, no labels
	 * are updated and no listener changes are made.
	 *
	 * @param ap the WatAnalysisPeriod whose data will be displayed, or null to take no action
	 */
	private void fillApFields(WatAnalysisPeriod ap) {
		// Only populate fields and update listeners if a valid analysis period was provided
		if (ap != null) {
			// Display the analysis period name in the AP label
			_apLabel.setText(ap.getName());

			// Display the run time window start time as a string in the start label
			_apStartLabel.setText(ap.getRunTimeWindow().getStartTime().toString());

			// Display the run time window end time as a string in the end label
			_apEndLabel.setText(ap.getRunTimeWindow().getEndTime().toString());

			// Detach the listener from the old AP before attaching it to the new one
			if (_ap != null) {
				_ap.removeModifiableListener(_apModListener);
			}

			// Update the tracked analysis period reference to the new period
			_ap = ap;

			// Attach the modifiable listener to the new analysis period to receive future change events
			_ap.addModifiableListener(_apModListener);
		}
	}

	/**
	 * Clears all three Analysis Period display labels by setting their text to empty strings.
	 */
	private void clearApLabels() {
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");
	}

	/**
	 * Returns the active simulation group by delegating to the parent ForecastPanel.
	 *
	 * @return the AbstractSimulationGroup currently displayed, or null if none is set
	 */
	@Override
	public AbstractSimulationGroup getSimulationGroup() {
		return _parentPanel.getSimulationGroup();
	}

	/**
	 * Returns the list of ResultsData objects associated with the currently selected
	 * rows in the simulation tree-table.
	 *
	 * @return a List of ResultsData for the selected simulations; may be empty
	 */
	public List<ResultsData> getSelectedResults() {
		return _simulationTable.getSelectedResults();
	}

	/**
	 * Populates the ensemble set table with the provided list of EnsembleSet objects.
	 * The table and its parallel tracking list are cleared before reloading. If the
	 * provided list is null, both are left empty and the method returns immediately.
	 * Each ensemble set is added as a row containing an unchecked checkbox, the BC data,
	 * the temperature target set, the member set to compute, and a comma-separated string
	 * of previously computed members.
	 *
	 * @param ensembleSets the list of EnsembleSet objects to display in the table, or
	 *                     null to clear the table without adding any rows
	 */
	public void setEnsembleSets(List<EnsembleSet> ensembleSets) {
		// Clear both the table rows and the parallel tracking list
		_simEnsembleTable.deleteCells();
		_esetsInTable.clear();

		// If no ensemble sets were provided, leave the table empty and return
		if (ensembleSets == null) {
			return;
		}

		// Populate the parallel tracking list with all provided ensemble sets
		_esetsInTable.addAll(ensembleSets);

		// Declare variables used to construct each table row
		Vector<Object> row;
		EnsembleSet eset;

		// Iterate over each ensemble set and append it as a row in the table
		for (int i = 0; i < ensembleSets.size(); i++) {
			// Retrieve the ensemble set at the current index
			eset = ensembleSets.get(i);

			// Build the row: unchecked, BC data, temp target set, member set, computed members
			row = new Vector();

			// Column 0: unchecked checkbox indicating the row is not currently selected
			row.add(Boolean.FALSE);

			// Column 1: the boundary condition data associated with this ensemble set
			row.add(eset.getBcData());

			// Column 2: the temperature target set associated with this ensemble set
			row.add(eset.getTemperatureTargetSet());

			// Column 3: the member set designated for computation
			row.add(eset.getMemberSetToCompute());

			// Format the previously computed members as a comma-separated string without brackets
			String cmStr = getComputedEnsembleString(eset);

			// Column 4: the formatted string of previously computed ensemble members
			row.add(cmStr);

			// Append the fully constructed row to the ensemble set table
			_simEnsembleTable.appendRow(row);
		}
	}

	/**
	 * Converts the computed members IntVector of the given EnsembleSet to a
	 * comma-separated string, stripping the surrounding square brackets added
	 * by IntVector.toString.
	 *
	 * @param eset the EnsembleSet whose computed members should be formatted
	 * @return a bracket-free, comma-separated string of computed member indices
	 */
	private String getComputedEnsembleString(EnsembleSet eset) {
		IntVector computedMembers = eset.getComputedMembers();
		String cmStr = computedMembers.toString();

		// Remove opening and closing square brackets produced by IntVector.toString
		cmStr = RMAIO.removeChar(cmStr, '[');
		cmStr = RMAIO.removeChar(cmStr, ']');
		return cmStr;
	}

	/**
	 * Returns all EnsembleSet objects whose rows are currently checked in the ensemble
	 * set table. Any in-progress cell edit is committed before reading to ensure the
	 * latest text is included. For each checked row, the corresponding EnsembleSet from
	 * the parallel tracking list is retrieved, and its member set string is updated from
	 * column 3 to persist any edits the user made. If the tracking list has not been
	 * initialized, an empty list is returned immediately.
	 *
	 * @return a list of selected EnsembleSet objects with their member set fields updated
	 *         to reflect any edits made in the table; empty if no rows are checked or if
	 *         the tracking list is null
	 */
	public List<EnsembleSet> getSelectedEnsembleSets() {
		// Commit any in-progress cell edit so the latest text is available
		_simEnsembleTable.commitEdit(true);

		// Initialize the list that will accumulate all selected ensemble sets
		List<EnsembleSet> selectedEsets = new ArrayList<>();

		// If the tracking list has not been initialized, return an empty list immediately
		if (_esetsInTable == null) {
			return selectedEsets;
		}

		// Get the total number of rows currently displayed in the ensemble set table
		int rowCnt = _simEnsembleTable.getRowCount();

		// Declare variables to hold the checkbox value, ensemble set, and member set string during iteration
		Object obj;
		EnsembleSet eset;
		String members;

		// Iterate over every row in the table to identify checked rows
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from column 0 of the current row
			obj = _simEnsembleTable.getValueAt(r, 0);

			// Accept either Boolean.TRUE or the string "true" for defensive equality
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Retrieve the EnsembleSet from the parallel tracking list at the same index
				eset = _esetsInTable.get(r);
				selectedEsets.add(eset);

				// Persist the edited member set string back to the model
				members = (String) _simEnsembleTable.getValueAt(r, 3);

				// Update the ensemble set's member set field with any edits made in the table
				eset.setMemberSetToCompute(members);
			}
		}

		// Return the list of all selected and updated ensemble sets
		return selectedEsets;
	}

	/**
	 * Returns the row indices of all checked rows in the ensemble set table. Any
	 * in-progress cell edit is committed before reading checkbox values to ensure
	 * the latest state is captured. If the parallel tracking list has not been
	 * initialized, an empty list is returned immediately.
	 *
	 * @return a list of zero-based row indices for all checked rows in the ensemble
	 *         set table; empty if no rows are checked or if the tracking list is null
	 */
	private List<Integer> getSelectedEnsembleRows() {
		// Commit any in-progress cell edit before reading checkbox values
		_simEnsembleTable.commitEdit(true);

		// Initialize the list that will accumulate the indices of all checked rows
		List<Integer> selectedRows = new ArrayList<>();

		// If the tracking list has not been initialized, return an empty list immediately
		if (_esetsInTable == null) {
			return selectedRows;
		}

		// Get the total number of rows currently displayed in the ensemble set table
		int rowCnt = _simEnsembleTable.getRowCount();

		// Declare a variable to hold the checkbox cell value during each iteration
		Object obj;

		// Iterate over every row in the table to identify checked rows
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from column 0 of the current row
			obj = _simEnsembleTable.getValueAt(r, 0);

			// Accept either Boolean.TRUE or the string "true" for defensive equality
			if (obj == Boolean.TRUE || "true".equals(obj.toString())) {
				// Record the index of this checked row
				selectedRows.add(r);
			}
		}

		// Return the list of all checked row indices
		return selectedRows;
	}

	/**
	 * Updates the compute state indicators for the simulation tree-table and refreshes
	 * the computed members column for all currently selected ensemble set rows. The
	 * superclass implementation is called first to update the simulation-level indicators.
	 * For each selected row, the latest computed members string is retrieved from the
	 * ensemble set and written back into column 4 of the ensemble set table to reflect
	 * any changes that occurred during the most recent compute operation.
	 */
	public void updateComputeStates() {
		// Let the superclass update the simulation tree-table's compute state indicators
		super.updateComputeStates();

		// Retrieve the indices of all currently checked rows in the ensemble set table
		List<Integer> selectedRows = getSelectedEnsembleRows();

		// Declare variables to hold the current row index, ensemble set, and computed members string
		int row;
		EnsembleSet eset;
		String cmStr;

		// Iterate over each selected row and refresh its computed members display
		for (int r = 0; r < selectedRows.size(); r++) {
			// Retrieve the table row index for the current selected entry
			row = selectedRows.get(r);

			// Retrieve the corresponding EnsembleSet from the parallel tracking list
			eset = _esetsInTable.get(r);

			// Reformat and write the latest computed members string into the table cell
			cmStr = getComputedEnsembleString(eset);

			// Update column 4 of the matching row with the refreshed computed members string
			_simEnsembleTable.setValueAt(cmStr, row, 4);
		}
	}

	/**
	 * Returns the WatSimulation object corresponding to the currently selected row in
	 * the simulation table. If no row is selected, null is returned.
	 *
	 * @return the WatSimulation stored in column 0 of the selected row, or null if no
	 *         row is currently selected
	 */
	public WatSimulation getSelectedSimulation() {
		// Get the index of the currently selected row in the simulation table
		int row = _simulationTable.getSelectedRow();

		// Return null if no row is currently selected
		if (row == -1) {
			return null;
		}

		// Retrieve and return the WatSimulation stored in column 0 of the selected row
		return (WatSimulation) _simulationTable.getValueAt(row, 0);
	}

	/**
	 * Records a newly computed ensemble member index in the ensemble set table for the
	 * given simulation and ensemble set. The update is only applied when the event
	 * corresponds to the currently selected simulation. The new member index is merged
	 * into the existing comma-separated computed members string in the COMPUTED_MEMBERS_COL
	 * column, maintaining a sorted and deduplicated representation. If either sim or
	 * ensembleSet is null, the method returns immediately without making any changes.
	 *
	 * @param sim            the WatSimulation for which the member was computed
	 * @param ensembleSet    the EnsembleSet that produced the computed member
	 * @param computedMember the index of the newly computed ensemble member to record
	 */
	public void addComputedMember(WatSimulation sim, EnsembleSet ensembleSet, int computedMember) {
		// Guard against null arguments to prevent NullPointerExceptions
		if (sim == null || ensembleSet == null) {
			return;
		}

		// Only update the display when the event applies to the currently selected simulation
		if (sim != getSelectedSimulation()) {
			return;
		}

		// Get the total number of ensemble sets currently tracked in the table
		int rowCnt = _esetsInTable.size();

		// Search the tracking list for the row that corresponds to the given ensemble set
		for (int r = 0; r < rowCnt; r++) {
			// Use identity comparison since each ensemble set is a unique object instance
			if (_esetsInTable.get(r) == ensembleSet) {
				// Read the current comma-separated computed members string from the table
				String computedMembers = (String) _simEnsembleTable.getValueAt(r, COMPUTED_MEMBERS_COL);

				// Merge the new member index into the sorted, deduplicated members string
				computedMembers = setComputedMember(computedMembers, computedMember);

				// Write the updated computed members string back to the table cell
				setComputedMembers(r, computedMembers);
			}
		}
	}

	/**
	 * Dispatches a table cell update for the "Previously Run" column to the Swing
	 * Event Dispatch Thread to avoid UI modifications from non-EDT threads.
	 *
	 * @param r               the row index in the ensemble table to update
	 * @param computedMembers the new formatted computed members string to display
	 */
	private void setComputedMembers(int r, String computedMembers) {
		// Ensure the table update occurs on the EDT, regardless of the calling thread
		EventQueue.invokeLater(() -> _simEnsembleTable.setValueAt(computedMembers, r, COMPUTED_MEMBERS_COL));
	}

	/**
	 * Adds a single ensemble member index to an existing comma-separated members string,
	 * deduplicates the result, and returns the entries sorted in numeric order.
	 * Handles null and empty input strings by returning just the new member.
	 *
	 * @param computedMembers the existing comma-separated member string, or null/empty
	 * @param computedMember  the new member index to add; negative values are treated as
	 *                        a reset and returned as the sole element
	 * @return a sorted, deduplicated comma-separated string containing the new member
	 */
	private String setComputedMember(String computedMembers, int computedMember) {
		// Return just the new member when there is no existing string to merge into
		if (computedMembers == null || computedMembers.isEmpty()) {
			return String.valueOf(computedMember);
		}

		String[] members = computedMembers.split(",");

		// Return just the new member when the existing string is empty or the index is negative
		if (members == null || members.length == 0 || computedMember < 0) {
			return String.valueOf(computedMember);
		}

		// Use a HashSet to deduplicate before sorting
		List<String> membersList = Arrays.asList(members);
		Set<String> membersSet = new HashSet<>(membersList);
		membersSet.add(String.valueOf(computedMember));

		// Rebuild as a list so it can be sorted by numeric value
		membersList = new ArrayList<>();
		membersList.addAll(membersSet);
		Collections.sort(membersList, new NumericComparator());

		// Join the sorted members into a comma-separated string
		StringBuilder builder = new StringBuilder();
		Iterator<String> iter = membersList.iterator();
		while (iter.hasNext()) {
			builder.append(iter.next());
			if (iter.hasNext()) {
				builder.append(",");
			}
		}
		return builder.toString();
	}

	/**
	 * Detaches the ModifiableListener from the active Analysis Period when the panel
	 * is closing, preventing memory leaks from retained listener references.
	 */
	public void closing() {
		if (_ap != null) {
			_ap.removeModifiableListener(_apModListener);
		}
	}
}
