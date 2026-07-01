package usbr.wat.plugins.actionpanel.editors;

import java.awt.Component;                           // Base class for all AWT/Swing UI components; used when iterating menu children
import java.awt.GridBagConstraints;                  // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;                       // Flexible grid-based layout manager for arranging UI components
import java.awt.event.ActionEvent;                   // Represents an action event fired when a button or menu item is activated
import java.awt.event.MouseEvent;                    // Represents a mouse event used for tooltip lookup on table hover
import java.util.List;                               // Ordered collection interface for simulation group lists
import java.util.Objects;                            // Utility class providing null-safe operations; used to filter null entries
import java.util.Vector;                             // Synchronized growable array used for constructing table rows
import java.util.logging.Logger;                     // JDK logging utility for recording sort-exception diagnostics
import java.util.prefs.BackingStoreException;        // Checked exception thrown when the preferences backing store fails
import java.util.prefs.Preferences;                  // Persistent hierarchical key-value store for saving recent simulation groups
import java.util.stream.Collectors;                  // Stream utility providing collection-reduction Collector implementations

import javax.swing.JMenu;                            // Swing menu component holding a list of JMenuItems
import javax.swing.JMenuBar;                         // Swing menu bar that holds one or more JMenu instances
import javax.swing.JMenuItem;                        // Clickable item within a JMenu
import javax.swing.ListSelectionModel;               // Defines selection modes for list and table components
import javax.swing.RowFilter;                        // Abstract filter for hiding rows in a sorted/filtered table
import javax.swing.table.TableRowSorter;             // Provides sorting and filtering for JTable using its model

import com.rma.client.Browser;                       // RMA application browser providing access to the main frame and preferences
import com.rma.model.Manager;                        // Base interface for all RMA managed data objects
import com.rma.model.Project;                        // Represents the currently loaded RMA project and its contents

import hec2.wat.model.WatAnalysisPeriod;             // Represents a WAT analysis period associated with a simulation group
import hec2.wat.model.WatSimulation;                 // Represents a single WAT simulation; used when building the tooltip simulation list

import rma.swing.ButtonCmdPanel;                     // Panel containing standard command buttons (OK, Cancel, etc.)
import rma.swing.ButtonCmdPanelListener;             // Listener interface for command button panel action events
import rma.swing.RmaInsets;                          // Pre-defined Insets constants for consistent UI component spacing
import rma.swing.RmaJDialog;                         // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTable;                          // RMA-extended table component with utility methods for row management
import rma.swing.RmaJTextField;                      // RMA-extended text field used here for the filter input
import rma.util.RMASort;                             // RMA utility providing a quicksort implementation for Manager lists

import usbr.wat.plugins.actionpanel.ActionsWindow;   // Parent Actions Window panel that owns this dialog
import usbr.wat.plugins.actionpanel.model.SimulationGroup; // Model representing a named group of WAT simulations

/**
 * Dialog for selecting an existing Simulation Group within the WTMP Action Panel.
 *
 * Displays all non-transitory SimulationGroup instances in the current project in
 * a sortable, filterable table. The user can type into a filter field to narrow
 * the list by group name, or use the Recent menu to quickly re-open a previously
 * selected group.
 *
 * A hovering tooltip over the Simulation Group column lists the simulations contained
 * in that group. Up to MAX_RECENT recently selected groups are remembered in the WAT
 * project preferences store and surfaced in the Recent menu for quick access.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class SelectSimulationGroupDialog extends RmaJDialog {

	// Column index for the SimulationGroup object in the table
	private static final int SIM_GROUP_COL = 0;

	// Maximum number of recently selected simulation groups retained in the Recent menu
	private static final int MAX_RECENT = 5;

	// Preferences key prefix used when storing recent simulation group names
	private static final String SIM_GROUP_KEY = "SimGroup";

	// Table displaying available simulation groups with name, description, and analysis period
	private RmaJTable _simGroupTable;

	// Panel containing OK and Cancel buttons
	private ButtonCmdPanel _cmdPanel;

	// Flag indicating whether the dialog was closed via Cancel (true) or OK (false)
	protected boolean _canceled;

	// Text field for filtering the simulation group table by group name
	private RmaJTextField _filterTextField;

	// Menu listing recently selected simulation groups for quick access
	private JMenu _recentMenu;

	// Reference to the parent Actions Window that launched this dialog
	private ActionsWindow _parent;

	/**
	 * Constructs a SelectSimulationGroupDialog attached to the given parent window.
	 *
	 * Builds all UI controls, attaches event listeners, populates the table with
	 * available simulation groups, builds the menu bar, sizes the dialog to its
	 * preferred size, and centers it relative to the parent window.
	 *
	 * @param parent the ActionsWindow that owns this dialog
	 * @param modal  true if the dialog should block input to other windows while open
	 */
	public SelectSimulationGroupDialog(ActionsWindow parent, boolean modal) {
		// Initialize the parent RmaJDialog with modality setting
		super(parent, modal);

		// Store a reference to the parent window
		_parent = parent;

		// Build and arrange all UI components
		buildControls();

		// Attach action and event listeners to interactive components
		addListeners();

		// Populate the table with simulation groups from the current project
		fillForm();

		// Construct the menu bar with the Recent menu
		buildMenus();

		// Resize the dialog to fit its preferred layout size
		pack();

		// Center the dialog relative to its parent window
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Uses a GridBagLayout to place a filter text field at the top, a simulation
	 * group table in the center, and an OK/Cancel button panel at the bottom.
	 * The table is configured for single-row selection, non-editable columns, and
	 * overrides getToolTipText to show group simulation details on hover.
	 */
	private void buildControls() {
		// Set the content pane to use GridBagLayout for flexible component placement
		getContentPane().setLayout(new GridBagLayout());

		// Set the dialog title
		setTitle("Select Simulation Group");

		// Create the filter text field with a descriptive tooltip
		_filterTextField = new RmaJTextField();
		_filterTextField.setToolTipText("Filter Table Contents by Simulation Group Name");

		// Configure constraints for the filter field to span the full row width
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_filterTextField, gbc);

		// Define the column headers for the simulation group table
		String[] headers = new String[]{"Simulation Group", "Description", "Analysis Period"};

		// Create the simulation group table with a tooltip override for the group name column
		_simGroupTable = new RmaJTable(this, headers) {
			@Override
			public String getToolTipText(MouseEvent e) {
				// Determine which row the mouse is over
				int row = _simGroupTable.rowAtPoint(e.getPoint());
				if (row == -1) {
					// No row under the cursor; fall back to default tooltip
					return super.getToolTipText(e);
				}

				int col = _simGroupTable.columnAtPoint(e.getPoint());

				// Show the custom simulation list tooltip only for the group name column
				if (col == SIM_GROUP_COL) {
					return getSimGroupToolTip(row, col);
				}

				// For other columns use the default tooltip behavior
				return super.getToolTipText(e);
			}
		};

		// Remove the built-in sum/statistics popup menu options
		_simGroupTable.removePopupMenuSumOptions();

		// Disable cell-level selection; only whole rows may be selected
		_simGroupTable.setCellSelectionEnabled(false);
		_simGroupTable.setRowSelectionAllowed(true);

		// Add extra row height for improved readability
		_simGroupTable.setRowHeight(_simGroupTable.getRowHeight() + 5);

		// Restrict selection to a single row at a time
		_simGroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Make all three columns read-only
		_simGroupTable.setColumnEnabled(false, 0);
		_simGroupTable.setColumnEnabled(false, 1);
		_simGroupTable.setColumnEnabled(false, 2);

		// Configure constraints for the table to fill all remaining space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupTable.getScrollPane(), gbc);

		// Create the OK/Cancel button panel
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);

		// Position the button panel at the bottom of the dialog spanning the full width
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * Builds and attaches the dialog's menu bar, including the Recent menu.
	 *
	 * Creates a JMenuBar with a single Recent menu whose items are populated
	 * from the project preferences via updateRecentMenu().
	 */
	private void buildMenus() {
		// Create the menu bar that will be attached to the dialog
		JMenuBar mbar = new JMenuBar();

		// Create the Recent menu with keyboard mnemonic 'R'
		_recentMenu = new JMenu("Recent");
		_recentMenu.setMnemonic('R');
		mbar.add(_recentMenu);

		// Attach the menu bar to the dialog
		setJMenuBar(mbar);

		// Populate the Recent menu with previously saved simulation group names
		updateRecentMenu();
	}

	/**
	 * Builds and returns an HTML tooltip string listing the simulations contained
	 * in the SimulationGroup found at the given table row and column.
	 *
	 * Returns null if the cell value is not a SimulationGroup instance.
	 *
	 * @param row the view row index of the cell being hovered
	 * @param col the view column index of the cell being hovered
	 * @return an HTML-formatted tooltip string, or null if not applicable
	 */
	protected String getSimGroupToolTip(int row, int col) {
		// Retrieve the value from the specified cell
		Object obj = _simGroupTable.getValueAt(row, col);

		if (obj instanceof SimulationGroup) {
			SimulationGroup simGroup = (SimulationGroup) obj;
			StringBuffer buf = new StringBuffer();

			// Open the HTML tooltip block
			buf.append("<html>");
			buf.append("Simulations in <b>" + simGroup + "</b>:");

			// Retrieve the list of simulations in this group
			List<WatSimulation> sims = simGroup.getSimulations();
			WatSimulation sim;

			if (sims.isEmpty()) {
				// Indicate that the group contains no simulations
				buf.append("<br>None");
			} else {
				// Open an indented paragraph for the simulation name list
				buf.append("<p style = \"margin-left: 10px\">");
				for (int i = 0; i < sims.size(); i++) {
					sim = sims.get(i);
					if (sim != null) {
						// Prepend a line break for all entries after the first
						if (i > 0) {
							buf.append("<br>");
						}
						// Add a bullet dash followed by the simulation name
						buf.append("- ");
						buf.append(sim.getName());
					}
				}
				buf.append("</p>");
			}

			// Close the HTML tooltip block
			buf.append("</html>");
			return buf.toString();
		}

		// Cell does not contain a SimulationGroup; no custom tooltip
		return null;
	}


	/**
	 * Attaches event listeners to interactive UI components.
	 *
	 * Registers an action listener on the filter text field to apply table
	 * filtering on Enter, and a ButtonCmdPanelListener on the command panel
	 * to handle OK and Cancel button events.
	 */
	private void addListeners() {
		// Apply table filter when the user presses Enter in the filter field
		_filterTextField.addActionListener(e -> filterTable());

		// Handle OK and Cancel button clicks from the command panel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Record selection, update the recent menu, and close the dialog
						_canceled = false;
						addSimultionGroupToRecentMenu(getSelectedSimulationGroup());
						setVisible(false);
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Mark as canceled and close the dialog without saving
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Rebuilds the Recent menu from the saved project preferences.
	 *
	 * Reads up to MAX_RECENT simulation group names from the preferences node.
	 * Each name that still exists in the current table is added as a menu item.
	 * Names that no longer correspond to a known group are skipped.
	 */
	protected void updateRecentMenu() {
		// Clear all current items from the Recent menu before rebuilding
		_recentMenu.removeAll();

		// Retrieve the preferences node holding recent simulation group names
		Preferences recentSimGroupNode = getRecentNode();

		String simGroupName;
		JMenuItem menuItem;

		// Iterate up to the maximum number of recent entries
		for (int i = 0; i < MAX_RECENT; i++) {
			// Read the i-th saved group name; stop if no entry exists
			simGroupName = recentSimGroupNode.get(SIM_GROUP_KEY + i, null);
			if (simGroupName == null) {
				break;
			}

			// Skip entries that no longer correspond to a group in the table
			if (!hasSimGroup(simGroupName)) {
				continue;
			}

			// Create a menu item for this group and attach the open action
			menuItem = new JMenuItem(simGroupName);
			menuItem.addActionListener(e -> openSimulationGroup(e));
			_recentMenu.add(menuItem);
		}
	}


	/**
	 * Checks whether a simulation group with the given name currently exists in the table.
	 *
	 * Iterates all table rows and compares each group's name to the provided value.
	 *
	 * @param simGroupName the name to search for in the simulation group table
	 * @return true if a matching group is found; false otherwise
	 */
	private boolean hasSimGroup(String simGroupName) {
		int rowCnt = _simGroupTable.getRowCount();

		SimulationGroup simGroup;
		for (int r = 0; r < rowCnt; r++) {
			simGroup = (SimulationGroup) _simGroupTable.getValueAt(r, 0);

			// Return true immediately if this row's group name matches
			if (simGroup.getName().equals(simGroupName)) {
				return true;
			}
		}

		// No matching group found in the table
		return false;
	}


	/**
	 * Handles an action event from a Recent menu item.
	 *
	 * Extracts the simulation group name from the menu item's text and
	 * delegates to the name-based openSimulationGroup overload.
	 *
	 * @param e the ActionEvent fired by the clicked menu item
	 */
	private void openSimulationGroup(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			// Extract the group name from the menu item's text label
			JMenuItem mi = (JMenuItem) e.getSource();
			openSimulationGroup(mi.getText());
		}
	}


	/**
	 * Looks up the named simulation group in the current project, selects it in
	 * the table, updates the recent menu, and closes the dialog.
	 *
	 * Has no effect if the name does not resolve to a SimulationGroup instance.
	 *
	 * @param simGroupName the name of the SimulationGroup to open
	 */
	private void openSimulationGroup(String simGroupName) {
		// Look up the simulation group by name in the current project
		Manager simGroup = Project.getCurrentProject().getManager(simGroupName, SimulationGroup.class);

		if (simGroup instanceof SimulationGroup) {
			// Attempt to highlight the group's row in the table
			if (setSelectedSimGroup(simGroup)) {
				// Add the opened group to the recent menu history
				addSimultionGroupToRecentMenu((SimulationGroup) simGroup);

				// Mark the dialog as confirmed and close it
				_canceled = false;
				setVisible(false);
			}
		}
	}


	/**
	 * Selects the table row whose first-column value matches the given Manager instance.
	 *
	 * Uses reference equality to locate the row, then programmatically updates
	 * the table's selection to that row.
	 *
	 * @param simGroup the Manager (SimulationGroup) to select in the table
	 * @return true if a matching row was found and selected; false otherwise
	 */
	private boolean setSelectedSimGroup(Manager simGroup) {
		int rowCnt = _simGroupTable.getRowCount();

		for (int r = 0; r < rowCnt; r++) {
			// Use reference equality to find the exact row for this group instance
			if (_simGroupTable.getValueAt(r, 0) == simGroup) {
				// Programmatically select this row in the table
				_simGroupTable.updateSelection(r, 0, false, false);
				return true;
			}
		}

		// No matching row found
		return false;
	}


	/**
	 * Returns the preferences sub-node used to store the recent simulation group history.
	 *
	 * The node is located at: [projectPreferenceNode]/wtmp/recentSimGroup
	 *
	 * @return the Preferences node for recent simulation group names
	 */
	private static Preferences getRecentNode() {
		// Navigate to the project preference node
		Preferences prjPrefsNode = Browser.getBrowserFrame().getPreferences().getProjectPreferenceNode();

		// Navigate into the WTMP-specific sub-node
		Preferences wtmpNode = prjPrefsNode.node("wtmp");

		// Navigate into the recent simulation group sub-node
		Preferences recentSimGroupNode = wtmpNode.node("recentSimGroup");
		return recentSimGroupNode;
	}

	/**
	 * Adds the given SimulationGroup to the top of the Recent menu and persists
	 * the updated list to the project preferences store.
	 *
	 * If the group is already in the menu, it is moved to the top. If adding it
	 * would exceed MAX_RECENT entries, the oldest (last) entry is removed first.
	 *
	 * @param simGroup the SimulationGroup to record as recently selected; ignored if null
	 */
	public void addSimultionGroupToRecentMenu(SimulationGroup simGroup) {
		// Do nothing if no group was provided
		if (simGroup == null) {
			return;
		}

		String simGroupName = simGroup.getName();

		// Check if this group already exists as a menu item
		JMenuItem menu = findSimGroupMenu(simGroupName);

		if (menu != null) {
			// Remove the existing entry so it can be re-inserted at the top
			_recentMenu.remove(menu);
		} else {
			// Create a new menu item for this group and attach the open action
			menu = new JMenuItem(simGroupName);
			menu.addActionListener(e -> openSimulationGroup(e));
		}

		// If the menu is already at capacity, remove the oldest (last) entry
		if (_recentMenu.getMenuComponentCount() >= MAX_RECENT) {
			_recentMenu.remove(MAX_RECENT - 1);
		}

		// Insert the group's menu item at the top of the Recent menu
		_recentMenu.insert(menu, 0);

		// Persist the updated recent menu list to preferences
		saveRecentMenu();
	}


	/**
	 * Persists the current Recent menu items to the project preferences store.
	 *
	 * Clears the existing preferences node and writes each JMenuItem's text in
	 * display order. Flushes the node to ensure changes are written immediately.
	 */
	private void saveRecentMenu() {
		Preferences simGroupNode = getRecentNode();

		// Clear previously saved entries before writing the current list
		try {
			simGroupNode.clear();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		// Iterate the current menu components and save each JMenuItem's name
		Component[] comps = _recentMenu.getMenuComponents();
		int idx = 0;
		String name;
		for (int i = 0; i < comps.length; i++) {
			if (comps[i] instanceof JMenuItem) {
				JMenuItem mi = (JMenuItem) comps[i];
				name = mi.getText();

				// Store the name using a zero-based index key
				simGroupNode.put(SIM_GROUP_KEY + idx, name);
				idx++;
			}
		}

		// Flush to ensure the preferences are written to the backing store
		try {
			simGroupNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Searches the Recent menu for an existing JMenuItem whose text matches
	 * the given simulation group name.
	 *
	 * @param simGroupName the simulation group name to search for; returns null if null
	 * @return the matching JMenuItem if found; null otherwise
	 */
	private JMenuItem findSimGroupMenu(String simGroupName) {
		// Return null immediately if no name was provided
		if (simGroupName == null) {
			return null;
		}

		Component[] comps = _recentMenu.getMenuComponents();
		for (int i = 0; i < comps.length; i++) {
			if (comps[i] instanceof JMenuItem) {
				JMenuItem mi = (JMenuItem) comps[i];

				// Return this item if its text matches the target name
				if (simGroupName.equals(mi.getText())) {
					return mi;
				}
			}
		}

		// No matching menu item found
		return null;
	}


	/**
	 * Applies a case-insensitive name filter to the simulation group table.
	 *
	 * Reads the current text from the filter field. If blank, the row filter is
	 * cleared to show all rows. Otherwise, only rows whose Simulation Group column
	 * value contains the filter string (case-insensitively) are displayed.
	 */
	private void filterTable() {
		// Normalize the filter text to lower case for case-insensitive matching
		String filter = _filterTextField.getText().toLowerCase();
		TableRowSorter sorter = (TableRowSorter) _simGroupTable.getRowSorter();

		if (filter.trim().isEmpty()) {
			// Empty filter: remove any active row filter to show all rows
			sorter.setRowFilter(null);
		} else {
			// Build a RowFilter that includes only rows whose group name contains the filter string
			RowFilter rowFilter = new RowFilter() {
				@Override
				public boolean include(Entry entry) {
					// Get the display value of the group name column for this row
					String value = entry.getStringValue(SIM_GROUP_COL);

					// Include the row if its group name contains the filter string
					if (value.toLowerCase().contains(filter)) {
						return true;
					}

					// Exclude rows that do not match the filter
					return false;
				}
			};

			// Apply the filter to the row sorter
			sorter.setRowFilter(rowFilter);
		}
	}


	/**
	 * Populates the simulation group table with all non-transitory SimulationGroup
	 * instances from the current project.
	 *
	 * Filters out null entries, sorts the list alphabetically using RMASort, and
	 * skips groups marked as transitory. Each row contains the SimulationGroup object,
	 * its description, and its analysis period name (or a placeholder if absent).
	 * If only one group is present, it is automatically selected. A TableRowSorter
	 * is installed to support the filter field.
	 */
	private void fillForm() {
		// Clear any existing rows from the table before repopulating
		_simGroupTable.deleteCells();

		Project proj = Project.getCurrentProject();

		// Retrieve all SimulationGroup instances from the current project
		List<SimulationGroup> simGroups = proj.getManagerListForType(SimulationGroup.class);

		// Filter out any null entries that may be returned by the project manager
		simGroups = simGroups.stream().filter(Objects::nonNull).collect(Collectors.toList());

		try {
			// Sort the simulation groups alphabetically for easier navigation
			RMASort.quickSort(simGroups);
		} catch (Exception e) {
			// Log the exception and each group name to aid in diagnosing the sort failure
			Logger.getLogger(getClass().getName()).info("Exception sorting SimGroups " + e);
			for (int i = 0; i < simGroups.size(); i++) {
				Logger.getLogger(getClass().getName()).info("Simulation " + i + " is" + simGroups.get(i));
			}
		}

		SimulationGroup sg;
		Vector<Object> row;
		WatAnalysisPeriod ap;
		for (int i = 0; i < simGroups.size(); i++) {
			sg = simGroups.get(i);

			// Skip transitory groups, which are internal and not meant for user selection
			if (sg.isTransitory()) {
				continue;
			}

			// Build a row vector with the group object, its description, and its analysis period
			row = new Vector<>();
			row.add(sg);
			row.add(sg.getDescription());

			ap = sg.getAnalysisPeriod();
			if (ap != null) {
				// Use the analysis period's display name
				row.add(ap.getName());
			} else {
				// Use a placeholder when no analysis period is assigned
				row.add("<unknown>");
			}

			_simGroupTable.appendRow(row);
		}

		// If there is exactly one group, auto-select it for convenience
		if (_simGroupTable.getRowCount() == 1) {
			_simGroupTable.setSelectedIndices(0);
		}

		// Install a TableRowSorter to enable column sorting and name filtering
		_simGroupTable.setRowSorter(new TableRowSorter(_simGroupTable.getModel()));
	}

	/**
	 * Returns whether the dialog was closed by the user selecting Cancel.
	 *
	 * @return true if the dialog was canceled; false if confirmed with OK
	 */
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Returns the SimulationGroup currently selected in the table.
	 *
	 * @return the selected SimulationGroup, or null if no row is selected
	 */
	public SimulationGroup getSelectedSimulationGroup() {
		int row = _simGroupTable.getSelectedRow();

		if (row > -1) {
			// Retrieve and return the SimulationGroup object from the selected row
			SimulationGroup sg = (SimulationGroup) _simGroupTable.getValueAt(row, SIM_GROUP_COL);
			return sg;
		}

		// No row is selected
		return null;
	}
}
