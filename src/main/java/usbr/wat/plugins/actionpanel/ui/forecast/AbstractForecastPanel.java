package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Color;                                              // Provides Color for setting background colors on enabled/disabled table components
import java.awt.Cursor;                                             // Provides Cursor for switching to a wait cursor during long-running operations
import java.awt.Dimension;                                          // Provides Dimension for constraining the preferred scrollable viewport height
import java.awt.GridBagConstraints;                                 // Provides GridBagConstraints for specifying layout parameters in a GridBagLayout
import java.awt.GridBagLayout;                                      // Provides GridBagLayout as the layout manager for the panel and its table container
import java.awt.Point;                                              // Provides Point for capturing the screen coordinate of a mouse click
import java.awt.event.MouseAdapter;                                 // Provides MouseAdapter as a no-op base class for mouse event listeners
import java.awt.event.MouseEvent;                                   // Provides MouseEvent carrying click location, button type, and modifier state
import java.awt.event.MouseListener;                                // Provides the MouseListener interface for registering mouse event callbacks

import java.util.ArrayList;                                         // Provides ArrayList as the resizable-array implementation used for panel and table lists
import java.util.Collections;                                       // Provides Collections for the singletonList utility used when inserting a row vector
import java.util.List;                                              // Provides the List interface for ordered collections of panels, tables, and data items
import java.util.Vector;                                            // Provides Vector as the row data container required by RmaJTable's row insertion methods
import java.util.stream.Collectors;                                 // Provides Collectors for terminal stream operations such as collecting names into a List

import javax.swing.DefaultListSelectionModel;                       // Provides DefaultListSelectionModel for retrieving and restoring selection listeners
import javax.swing.JMenuItem;                                       // Provides JMenuItem for the "Delete..." popup menu item added to each forecast table
import javax.swing.JOptionPane;                                     // Provides JOptionPane for displaying delete/overwrite confirmation dialogs
import javax.swing.JSeparator;                                      // Provides JSeparator for the horizontal divider between the table panel and lower panel
import javax.swing.ListSelectionModel;                              // Provides the ListSelectionModel interface for sharing a single selection model across panels
import javax.swing.SwingUtilities;                                  // Provides SwingUtilities for right-mouse-button detection in mouse event handlers
import javax.swing.UIManager;                                       // Provides UIManager for retrieving the look-and-feel's default table background color
import javax.swing.border.Border;                                   // Provides the Border interface used to store and restore a table's default scroll pane border
import javax.swing.border.LineBorder;                               // Provides LineBorder for drawing a two-pixel black highlight border on the active table
import javax.swing.event.ListSelectionEvent;                        // Provides ListSelectionEvent carrying the range of row indices affected by a selection change
import javax.swing.event.ListSelectionListener;                     // Provides the ListSelectionListener interface for responding to table row selection changes
import hec.lang.NamedType;                                          // Provides NamedType as the bound for the generic type parameter T and for name extraction in streams

import rma.swing.EnabledJPanel;                                     // Provides EnabledJPanel as a panel subclass that supports enable/disable propagation to children
import rma.swing.RmaInsets;                                         // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJPanel;                                         // Provides RmaJPanel as the base Swing panel class this component extends
import rma.swing.RmaJTable;                                         // Provides RmaJTable as the base Swing table class used by the inner ForecastTable class
import rma.swing.table.RmaTableModel;                               // Provides RmaTableModel as the typed table model shared across all instances via static fields

import usbr.wat.plugins.actionpanel.model.forecast.BcData;                                  // Provides BcData for identifying boundary condition sets affected by a delete or overwrite
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;                             // Provides EnsembleSet for identifying ensemble sets that must also be deleted when dependent data is removed
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                        // Provides ForecastSimGroup as the top-level data container populated into and read from the panel tables
import usbr.wat.plugins.actionpanel.ui.forecast.temptarget.TempTargetForecastTableModel;    // Provides TempTargetForecastTableModel as the specialised table model for the temperature target table

/**
 * Abstract base class for all forecast data panels displayed within the WTMP action panel UI.
 *
 * {@code AbstractForecastPanel} is a generic Swing panel parameterised by {@code T}, the type
 * of data item managed by the panel (e.g., a meteorology record or a temperature target set).
 * It provides the shared table layout, selection synchronisation, import/delete logic, and
 * listener wiring that all concrete forecast panels inherit.
 *
 * The panel displays five side-by-side {@link ForecastTable} instances in a shared upper
 * table region:
 *
 *   Initial Conditions
 *   Operations
 *   Meteorology
 *   Boundary Condition Sets
 *   Temperature Target Sets
 *
 * All five tables share static {@link RmaTableModel} and {@link ListSelectionModel} instances
 * so that their contents and selections remain synchronised across all {@code AbstractForecastPanel}
 * subclass instances shown simultaneously.
 *
 * A lower panel area below a horizontal separator is delegated to subclasses via
 * {@link #buildLowerPanel(EnabledJPanel)}.
 *
 * Concrete subclasses must implement the abstract methods that define data-specific
 * behaviour: {@link #tableRowSelected(int)}, {@link #tableRowDeleteClicked(int)},
 * {@link #delete(Object, boolean)}, {@link #removeData(ForecastSimGroup, Object)},
 * {@link #importForecastData(ImportForecastWindow)}, {@link #fillPanel(ForecastSimGroup)},
 * {@link #savePanel()}, {@link #clearPanel()}, and {@link #getTableForPanel()}.
 *
 * @param <T> the type of data item managed by this panel; must extend {@link NamedType}
 *
 * @see ForecastTable
 * @see ForecastPanel
 * @see ForecastSimGroup
 */
public abstract class AbstractForecastPanel<T extends NamedType> extends RmaJPanel {
	// Tracks all AbstractForecastPanel instances created during the session so that
	// getPanelForTable can locate the owning panel for any given ForecastTable by name
	protected static List<AbstractForecastPanel> _panels = new ArrayList<>();

	// The parent forecast panel that hosts this component and manages tab selection
	protected ForecastPanel _forecastPanel;

	// The five forecast tables displayed side-by-side in the shared upper table region
	protected ForecastTable _initialConditionsTable;
	protected ForecastTable _opsTable;
	protected ForecastTable _metTable;
	protected ForecastTable _bcTable;
	protected ForecastTable _tempTargetTable;

	// The lower panel area below the horizontal separator; populated by buildLowerPanel
	protected EnabledJPanel _lowerPanel;

	// Ordered list of all five ForecastTable instances; used for bulk operations such as
	// enabling/disabling, clearing, and attaching mouse listeners
	private List<ForecastTable> _tables = new ArrayList<>();

	// The panel that contains all five scroll-pane-wrapped forecast tables
	private EnabledJPanel _tablePanel;

	// Static table models shared across all AbstractForecastPanel instances so that
	// table contents remain synchronised regardless of which subclass is active
	private static RmaTableModel _initialConditionsTableModel;
	private static RmaTableModel _opsTableModel;
	private static RmaTableModel _metTableModel;
	private static RmaTableModel _bcTableModel;
	private static TempTargetForecastTableModel _tempTargetTableModel;

	// Static selection models shared across all instances so that row selection in one
	// panel is immediately reflected in all other panels showing the same table
	private static ListSelectionModel _initialConditionsSelectionModel;
	private static ListSelectionModel _opsSelectionModel;
	private static ListSelectionModel _metSelectionModel;
	private static ListSelectionModel _bcSelectionModel;
	private static ListSelectionModel _tempTargetSelectionModel;

	/**
	 * Constructs an {@code AbstractForecastPanel} and performs all shared initialisation:
	 * lays out the panel with {@link GridBagLayout}, stores the parent
	 * {@link ForecastPanel} reference, builds all controls, attaches all listeners, and
	 * registers this instance in the static panel list.
	 *
	 * @param forecastPanel the {@link ForecastPanel} that owns and hosts this panel; must not be {@code null}
	 */
	public AbstractForecastPanel(ForecastPanel forecastPanel) {
		// Initialise the base RmaJPanel with a GridBagLayout for flexible component placement
		super(new GridBagLayout());

		// Store the parent forecast panel for tab selection and simulation refresh calls
		_forecastPanel = forecastPanel;

		// Build all Swing controls and lay them out
		buildControls();

		// Wire up selection and mouse listeners for all tables
		addListeners();

		// Register this instance so getPanelForTable can locate it by table name
		_panels.add(this);
	}

	/**
	 * Builds and lays out all Swing controls for this panel.
	 *
	 * Constructs the upper table panel containing five ForecastTable instances
	 * (Initial Conditions, Operations, Meteorology, Boundary Condition Sets, and
	 * Temperature Target Sets), a horizontal JSeparator, and the lower panel
	 * area populated by buildLowerPanel(EnabledJPanel).
	 *
	 * For each table, the method reuses the static shared model and selection model if
	 * they have already been created by a previous instance, or creates them on first
	 * use and stores them in the static fields for subsequent instances to share.
	 */
	private void buildControls() {
		// Create the container panel that holds all five table scroll panes
		_tablePanel = new EnabledJPanel(new GridBagLayout());

		// Configure constraints for the table panel: full width, fills half the vertical space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;  // Span the full row width
		gbc.weightx = 1.0;                             // Expand horizontally to fill available space
		gbc.weighty = 0.5;                             // Take up half the vertical space
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_tablePanel, gbc);

		// -------------------------------------------------------------------------
		// --- Initial Conditions Table ---
		// -------------------------------------------------------------------------

		// Reuse the shared model if it exists; otherwise create a new single-column table
		if (_initialConditionsTableModel != null) {
			_initialConditionsTable = new ForecastTable(this, _initialConditionsTableModel);

		} else {
			_initialConditionsTable = new ForecastTable(this, new String[]{"Initial Conditions"});
		}

		// Reuse the shared selection model if it exists; otherwise initialise it from this table
		if (_initialConditionsSelectionModel != null) {
			_initialConditionsTable.setSelectionModel(_initialConditionsSelectionModel);

		} else {
			// Create and configure a new single-selection model for the initial conditions table
			_initialConditionsSelectionModel = _initialConditionsTable.getSelectionModel();
			_initialConditionsSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		// Capture the table model into the static field if this is the first instance
		if (_initialConditionsTableModel == null) {
			_initialConditionsTableModel = (RmaTableModel) _initialConditionsTable.getModel();
		}

		// Name the table, remove the delete menu item (IC rows are not user-deletable), and clear stale data
		_initialConditionsTable.setName("Initial Conditions");
		_initialConditionsTable.getPopupMenu().remove(_initialConditionsTable.getDeleteMenuItem());
		_initialConditionsTableModel.clearAll();

		// Configure constraints for each individual table column: equal weight, fills all space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;     // Each table occupies one column unit of the grid
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;     // Allow each table to expand vertically within the table panel
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_tablePanel.add(_initialConditionsTable.getScrollPane(), gbc);

		// Register the table in the shared list for coordinated access later
		_tables.add(_initialConditionsTable);

		// -------------------------------------------------------------------------
		// --- Operations Table ---
		// -------------------------------------------------------------------------

		// Reuse the shared operations model if it exists; otherwise create a new single-column table
		if (_opsTableModel != null) {
			_opsTable = new ForecastTable(this, _opsTableModel);

		} else {
			_opsTable = new ForecastTable(this, new String[]{"Operations"});
		}

		// Reuse the shared operations selection model if it exists; otherwise initialise it from this table
		if (_opsSelectionModel != null) {
			_opsTable.setSelectionModel(_opsSelectionModel);

		} else {
			// Create and configure a new single-selection model for the operations table
			_opsSelectionModel = _opsTable.getSelectionModel();
			_opsSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		// Capture the operations table model into the static field if this is the first instance
		if (_opsTableModel == null) {
			_opsTableModel = (RmaTableModel) _opsTable.getModel();
		}

		// Name the table and add it to the table panel with the same column constraints as above
		_opsTable.setName("Operations");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_tablePanel.add(_opsTable.getScrollPane(), gbc);

		// Register the operations table in the shared list
		_tables.add(_opsTable);

		// -------------------------------------------------------------------------
		// --- Meteorology Table ---
		// -------------------------------------------------------------------------

		// Reuse the shared meteorology model if it exists; otherwise create a new single-column table
		if (_metTableModel != null) {
			_metTable = new ForecastTable(this, _metTableModel);
		} else {
			_metTable = new ForecastTable(this, new String[]{"Meteorology"});
		}

		// Reuse the shared meteorology selection model if it exists; otherwise initialise it from this table
		if (_metSelectionModel != null) {
			_metTable.setSelectionModel(_metSelectionModel);
		} else {
			// Create and configure a new single-selection model for the meteorology table
			_metSelectionModel = _metTable.getSelectionModel();
			_metSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		// Capture the meteorology table model into the static field if this is the first instance
		if (_metTableModel == null) {
			_metTableModel = (RmaTableModel) _metTable.getModel();
		}

		// Name the table and add it to the table panel with the same column constraints as above
		_metTable.setName("Meteorology");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_tablePanel.add(_metTable.getScrollPane(), gbc);

		// Register the meteorology table in the shared list
		_tables.add(_metTable);

		// -------------------------------------------------------------------------
		// --- Boundary Condition Sets Table ---
		// -------------------------------------------------------------------------

		// Reuse the shared boundary condition model if it exists; otherwise create a new single-column table
		if (_bcTableModel != null) {
			_bcTable = new ForecastTable(this, _bcTableModel);
		} else {
			_bcTable = new ForecastTable(this, new String[]{"Boundary Condition Sets"});
		}

		// Reuse the shared boundary condition selection model if it exists; otherwise initialise it from this table
		if (_bcSelectionModel != null) {
			_bcTable.setSelectionModel(_bcSelectionModel);
		} else {
			// Create and configure a new single-selection model for the boundary condition table
			_bcSelectionModel = _bcTable.getSelectionModel();
			_bcSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		// Capture the boundary condition table model into the static field if this is the first instance
		if (_bcTableModel == null) {
			_bcTableModel = (RmaTableModel) _bcTable.getModel();
		}

		// Clear any stale boundary condition cells from a previous load
		_bcTable.setName("Boundary Condition Sets");
		_bcTable.deleteCells();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_tablePanel.add(_bcTable.getScrollPane(), gbc);

		// Register the boundary condition table in the shared list
		_tables.add(_bcTable);

		// -------------------------------------------------------------------------
		// --- Temperature Target Sets Table ---
		// -------------------------------------------------------------------------

		// The temperature target table uses an anonymous subclass that overrides deleteCells
		// to also clear the temperature target data from the specialised table model
		if (_tempTargetTableModel != null) {
			_tempTargetTable = new ForecastTable(this, _tempTargetTableModel) {
				@Override
				public void deleteCells() {
					// Clear temperature target data from the model before clearing the visual cells
					_tempTargetTableModel.clearTempTargets();
					super.deleteCells();
				}
			};
		} else {
			_tempTargetTable = new ForecastTable(this, new String[]{"Temperature Target Sets"}) {
				@Override
				public void deleteCells() {
					// Clear temperature target data from the model before clearing the visual cells
					_tempTargetTableModel.clearTempTargets();
					super.deleteCells();
				}
			};
		}

		// Reuse the shared temperature target selection model if it exists; otherwise initialise it from this table
		if (_tempTargetSelectionModel != null) {
			_tempTargetTable.setSelectionModel(_tempTargetSelectionModel);
		} else {
			// Create and configure a new single-selection model for the temperature target table
			_tempTargetSelectionModel = _tempTargetTable.getSelectionModel();
			_tempTargetSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		// Initialise the specialised temperature target model on first use and assign it to the table
		if (_tempTargetTableModel == null) {
			_tempTargetTableModel = new TempTargetForecastTableModel();
			_tempTargetTable.setModel(_tempTargetTableModel);
		}

		// The temperature target table spans the remaining width of the table panel row
		_tempTargetTable.setName("Temperature Target Sets");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;  // Span all remaining columns in the row
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_tablePanel.add(_tempTargetTable.getScrollPane(), gbc);

		// Register the temperature target table in the shared list
		_tables.add(_tempTargetTable);

		// -------------------------------------------------------------------------
		// --- Separator and Lower Panel ---
		// -------------------------------------------------------------------------

		// Add a horizontal separator between the table panel and the lower content panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;     // Separator takes no vertical space
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;  // Stretch only horizontally across the full width
		gbc.insets = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);

		// Create the lower panel that will hold subclass-specific content
		_lowerPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;     // Lower panel takes up all remaining vertical space
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_lowerPanel, gbc);

		// Delegate lower-panel content construction to the concrete subclass
		buildLowerPanel(_lowerPanel);
	}

	/**
	 * Registers all event listeners required by this panel. Each of the five forecast
	 * tables has its selection model wired to the shared tableSelected() handler, which
	 * coordinates selection state across all tables. After selection listeners are
	 * registered, addUpperTableListeners() is called to attach mouse and delete action
	 * listeners to each table's viewport, header, and table component.
	 */
	protected void addListeners() {
		// Wire each table's selection model to the shared tableSelected handler
		// so that selecting a row in any table can be handled in a single, consistent location
		_initialConditionsTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _initialConditionsTable));
		_opsTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _opsTable));
		_metTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _metTable));
		_bcTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _bcTable));
		_tempTargetTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _tempTargetTable));

		// Attach mouse listeners and delete action listeners to the table, viewport, and header
		addUpperTableListeners();
	}

	/**
	 * Adds mouse listeners (for tab switching on click) and delete-action listeners
	 * (for the popup "Delete..." menu item) to every forecast table, its scroll pane
	 * viewport, and its column header.
	 *
	 * Each mouse-release event on any part of a table causes the {@link ForecastPanel}
	 * to switch to the tab that owns that table. Each delete menu item click calls
	 * {@link #deleteClicked(ForecastTable)} with the appropriate table.
	 */
	void addUpperTableListeners() {
		for (ForecastTable table : _tables) {
			// Attach a tab-switch listener to the table body, its viewport, and its header
			table.addMouseListener(buildUpperTableMouseListener(table));
			table.getScrollPane().getViewport().addMouseListener(buildUpperTableMouseListener(table));
			table.getTableHeader().addMouseListener(buildUpperTableMouseListener(table));

			// Wire the "Delete..." popup item to the delete handler for this table
			JMenuItem deleteMenuItem = table.getDeleteMenuItem();
			if (deleteMenuItem != null) {
				deleteMenuItem.addActionListener(e -> deleteClicked(table));
			}
		}
	}

	/**
	 * Handles a click on the "Delete..." popup menu item for the given table.
	 *
	 * Looks up the owning panel for the table, then delegates to
	 * {@link #tableRowDeleteClicked(int)} on that panel with the row that was
	 * right-clicked. The cursor is set to a wait cursor for the duration of the
	 * operation and restored in a {@code finally} block.
	 *
	 * @param table the {@link ForecastTable} whose delete popup item was clicked
	 */
	private void deleteClicked(ForecastTable table) {
		// Retrieve the row index that was right-clicked to open the popup menu
		int row = table.getPopupMenuRow();

		// Find the panel that owns this table by matching table names
		AbstractForecastPanel panel = getPanelForTable(table);

		if (row >= 0 && panel != null) {
			try {
				// Show a wait cursor while the delete operation runs
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				panel.tableRowDeleteClicked(row);
			} finally {
				// Always restore the default cursor, even if an exception occurs
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}

	/**
	 * Builds and returns a {@link MouseListener} that switches the {@link ForecastPanel}'s
	 * selected tab to the panel owning the given table whenever a mouse button is released
	 * anywhere on the table.
	 *
	 * @param table the {@link ForecastTable} whose owning panel should be selected on click
	 * @return a {@link MouseListener} that calls {@link ForecastPanel#setSelectedTab} on
	 * mouse release
	 */
	private MouseListener buildUpperTableMouseListener(ForecastTable table) {
		return new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				// Switch the active tab to the panel that owns the clicked table
				_forecastPanel.setSelectedTab(getPanelForTable(table));
			}
		};
	}

	/**
	 * Responds to a row selection change in one of the five forecast tables.
	 *
	 * Ignores events that are still adjusting (i.e., fired mid-drag) to avoid
	 * redundant processing. When the selection is final, switches the active tab
	 * to the owning panel, saves the current panel state, and notifies the panel
	 * of the newly selected row via {@link #tableRowSelected(int)}.
	 *
	 * @param e     the {@link ListSelectionEvent} describing the selection change
	 * @param table the {@link ForecastTable} in which the selection changed
	 */
	private void tableSelected(ListSelectionEvent e, ForecastTable table) {
		// Ignore intermediate selection events fired during a drag or range selection
		if (e.getValueIsAdjusting()) {
			return;
		}

		// Find the panel that owns this table and update the UI accordingly
		AbstractForecastPanel panel = getPanelForTable(table);

		if (panel != null) {
			// Switch the active forecast tab to the panel associated with the selected table
			_forecastPanel.setSelectedTab(panel);

			// Persist the current panel state before switching context
			panel.savePanel();

			// Notify the panel of the newly selected row
			int row = table.getSelectedRow();
			panel.tableRowSelected(row);
		}
	}

	/**
	 * Shows or hides this panel and maintains consistent table selection state across
	 * all five forecast tables when the panel becomes visible.
	 *
	 * When becoming visible:
	 *
	 *   If the panel's own table has no row selected (and this is not an
	 *       {@link InitialConditionsPanel}), {@link #clearPanel()} is called to reset
	 *       the lower panel content.
	 *   For every other table that has a row selected, the selection is re-applied
	 *       via {@link ForecastTable#updateSelection} to keep the shared selection models
	 *       consistent with the visible state.
	 *
	 * @param visible {@code true} to show the panel; {@code false} to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		// If becoming visible with no row selected in the owning table, clear the lower panel
		if (visible && getTableForPanel() != null && getTableForPanel().getSelectedRow() < 0
				&& !(this instanceof InitialConditionsPanel)) {
			clearPanel();
		}

		if (visible) {
			// Re-apply the existing row selection on every table other than the panel's own table
			// so the shared selection models remain consistent with the visible UI state
			for (ForecastTable table : _tables) {
				if (getTableForPanel() != table) {
					int row = table.getSelectedRow();
					if (row >= 0) {
						table.updateSelection(row, 0, false, false);
					}
				}
			}
		}
	}

	/**
	 * Clears the content of the lower panel when no row is selected in the panel's table.
	 * Concrete subclasses implement this to reset all data-specific lower-panel controls.
	 */
	protected abstract void clearPanel();

	/**
	 * Clears all rows from every forecast table by calling {@link ForecastTable#deleteCells()}
	 * on each table in the internal table list.
	 */
	public void clearTables() {
		for (ForecastTable table : _tables) {
			table.deleteCells();
		}
	}

	/**
	 * Searches the list of forecast panels for the one associated with the given
	 * ForecastTable. Panels are matched by comparing the name of their primary table
	 * against the name of the provided table. Returns the first matching panel found,
	 * or null if no panel in the list is associated with a table of that name.
	 *
	 * @param forecastTable the ForecastTable whose associated panel is being looked up
	 * @return              the matching AbstractForecastPanel, or null if no match is found
	 */
	public AbstractForecastPanel getPanelForTable(ForecastTable forecastTable) {
		// Iterate over all registered forecast panels to find the one linked to the given table
		for (int i = 0; i < _panels.size(); i++) {
			// Retrieve the primary table associated with the current panel
			ForecastTable table = _panels.get(i).getTableForPanel();

			// Skip panels that have no associated table
			if (table != null) {
				// Match panels by comparing their primary table's name
				if (table.getName().equals(forecastTable.getName())) {
					// Match found; return the corresponding panel immediately
					return _panels.get(i);
				}
			}
		}

		// No panel was found for the given table; return null to signal absence
		return null;
	}

	/**
	 * Imports a new data item into the given table and data list, handling the case where
	 * an item with the same name already exists.
	 *
	 * If the name is already in use:
	 *
	 *   Retrieves the existing item from the table and calls
	 *       {@link #deleteForOverwrite} to replace it after user confirmation.
	 *   If the user cancels the overwrite, re-opens the import dialog.
	 *
	 * If the name is not in use, appends the new item to the table and data list and
	 * selects its row.
	 *
	 * @param fsg      the {@link ForecastSimGroup} the data belongs to; passed to the
	 *                 delete-for-overwrite path
	 * @param table    the {@link ForecastTable} into which the new item is inserted
	 * @param dlg      the {@link ImportForecastWindow} to re-open if the user cancels overwrite
	 * @param dataList the backing data list to keep in sync with the table
	 * @param newData  the new data item to import; its name is checked for duplicates
	 * @return {@code true} if the import succeeded (either as a new item or confirmed
	 * overwrite); {@code false} if the user cancelled the overwrite
	 */
	@SuppressWarnings("unchecked")
	protected boolean importData(ForecastSimGroup fsg, ForecastTable table, ImportForecastWindow dlg, List<T> dataList, T newData) {
		boolean retVal = true;

		if (table.isNameUsed(newData.getName())) {
			// An item with this name already exists; attempt a user-confirmed overwrite
			int rowToReplace = table.getRowWithName(newData.getName());
			Object value = table.getValueAt(rowToReplace, 0);

			if (value != null) {
				// Retrieve the existing data item for the overwrite comparison
				T existingData = (T) value;

				if (!deleteForOverwrite(table, dataList, existingData, newData, rowToReplace)) {
					// User cancelled the overwrite; re-open the import dialog
					retVal = false;
					importForecastData(dlg);
				}
			}

		} else {
			// Name is not in use; append as a new row and select it
			Vector<T> row = new Vector<>();
			row.add(newData);
			table.appendRow(row);
			dataList.add(newData);

			AbstractForecastPanel panel = getPanelForTable(table);
			panel.tableRowSelected(table.getRowCount() - 1);
		}

		// Mark the simulation group as modified to trigger a save prompt on close
		fsg.setModified(true);

		return retVal;
	}

	/**
	 * Displays a confirmation dialog warning the user about the consequences of deleting
	 * or overwriting the specified data object. If any boundary condition sets reference
	 * the data, the dialog lists them by name and warns that they will also be deleted.
	 * If any ensemble sets reference the data, an additional warning is appended via
	 * appendEnsembleDeleteMessage(). The dialog title and action wording adjust based on
	 * whether the deletion is triggered by an overwrite operation or a direct delete.
	 * Returns true if the user confirms the operation, or false if they cancel.
	 *
	 * @param initialMessage        the base message to display if no cascade deletions apply
	 * @param bcDataUsingData       the list of boundary condition sets that reference this data
	 * @param eSetsUsingData        the list of ensemble sets that reference this data
	 * @param deletingDueToOverwrite true if the deletion is triggered by an overwrite, false for a direct delete
	 * @param data                  the data object being deleted or overwritten
	 * @return                      true if the user confirmed the operation, false if they canceled
	 */
	protected boolean displayDeleteMessage(String initialMessage, List<BcData> bcDataUsingData, List<EnsembleSet> eSetsUsingData, boolean deletingDueToOverwrite, T data) {
		// Default to false; only set to true if the user explicitly confirms
		boolean confirmDelete = false;

		// Start building the confirmation message from the provided initial message
		StringBuilder confirmMessage = new StringBuilder(initialMessage);

		if (!bcDataUsingData.isEmpty()) {
			// Collect the names of all boundary condition sets that will be cascade-deleted
			List<String> bcDataNames = bcDataUsingData.stream()
					.map(NamedType::getName)
					.collect(Collectors.toList());

			// Use "Deleting" or "Overwriting" wording depending on the operation type
			String action = "Deleting";
			if (deletingDueToOverwrite) {
				// Prefix the action with the data name and an overwrite notice
				action = data.getName() + " already exists.\nOverwriting";
			}

			// Reset the message buffer before building the cascade-delete warning
			confirmMessage.setLength(0);

			// Build the cascade-delete warning message with the boundary condition set names
			confirmMessage.append(action).append(" ").append(data.getName())
					.append(" will also delete the following boundary condition sets that use it:")
					.append("\n\n")
					.append(String.join(",\n", bcDataNames));

			// Append ensemble set cascade warning if applicable
			appendEnsembleDeleteMessage(eSetsUsingData, confirmMessage);

			// Prompt the user to confirm or cancel the cascading operation
			confirmMessage.append("\n\nDo you want to continue?");

		} else {
			// No boundary condition sets affected; only append the ensemble set warning if needed
			appendEnsembleDeleteMessage(eSetsUsingData, confirmMessage);
		}

		// Set the dialog title to reflect whether this is a delete or overwrite operation
		String title = "Confirm " + (deletingDueToOverwrite ? "Overwrite" : "Delete");

		// Show the confirmation dialog with the appropriate title and warning icon
		int opt = JOptionPane.showConfirmDialog(this, confirmMessage,
				title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		// If the user clicked Yes, mark the operation as confirmed
		if (opt == JOptionPane.YES_OPTION) {
			confirmDelete = true;
		}

		// Return the user's decision to the caller
		return confirmDelete;
	}

	/**
	 * Executes the deletion of a data item from the simulation group and refreshes any
	 * dependent UI components.
	 *
	 * The cursor is set to a wait cursor for the duration of the operation and restored
	 * in a {@code finally} block. If boundary condition sets or ensemble sets were also
	 * deleted as a cascade, their respective panels are refreshed.
	 *
	 * @param fsg             the {@link ForecastSimGroup} from which {@code data} is removed
	 * @param data            the data item to delete
	 * @param table           the {@link ForecastTable} from which the corresponding row is removed
	 * @param bcDataUsingData boundary condition sets deleted as a cascade; if non-empty, the
	 *                        boundary condition panel is refreshed
	 * @param eSetsUsingData  ensemble sets deleted as a cascade; if non-empty, the simulation
	 *                        panel is refreshed via {@link ForecastPanel#refreshSimulationPanel}
	 */
	protected void performDelete(ForecastSimGroup fsg, T data, ForecastTable table, List<BcData> bcDataUsingData, List<EnsembleSet> eSetsUsingData) {
		try {
			// Show a wait cursor while the delete and save operations run
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			// Find the row index before removal so it can be deleted from the table afterward
			int rowToDelete = table.getRowWithName(data.getName());

			// Remove the data item from the simulation group's internal data structures
			removeData(fsg, data);

			// Persist the updated simulation group to disk
			fsg.saveData();

			// Refresh the boundary condition panel if cascade-deleted BC sets are affected
			if (!bcDataUsingData.isEmpty()) {
				getPanelForTable(_bcTable).setSimulationGroup(fsg);
			}

			// Refresh the simulation panel if cascade-deleted ensemble sets are affected
			if (!eSetsUsingData.isEmpty()) {
				_forecastPanel.refreshSimulationPanel(fsg);
			}

			// Remove the row from the table only if it was found
			if (rowToDelete > -1) {
				table.deleteRow(rowToDelete);
			}

		} finally {
			// Always restore the default cursor, even if an exception occurs
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Appends an ensemble set cascade-delete warning to the confirmation message builder
	 * if the supplied list is non-empty.
	 *
	 * The appended text lists the names of all affected ensemble sets separated by commas
	 * and newlines, explaining that they will be deleted because they reference boundary
	 * condition sets that are being removed.
	 *
	 * @param eSetsUsingData ensemble sets that will be cascade-deleted; no text is
	 *                       appended if this list is empty
	 * @param confirmMessage the {@link StringBuilder} to which the warning text is appended
	 */
	private void appendEnsembleDeleteMessage(List<EnsembleSet> eSetsUsingData, StringBuilder confirmMessage) {
		if (!eSetsUsingData.isEmpty()) {
			// Collect the names of all ensemble sets that will be cascade-deleted
			List<String> eSetNames = eSetsUsingData.stream()
					.map(NamedType::getName)
					.collect(Collectors.toList());

			confirmMessage.append("\n\nIt will also delete the following ensemble sets which use those boundary condition sets:")
					.append("\n\n")
					.append(String.join(",\n", eSetNames));
		}
	}

	/**
	 * Removes the given data item from the supplied {@link ForecastSimGroup}'s internal
	 * data structures.
	 *
	 * Concrete subclasses implement this to perform the type-specific removal logic
	 * (e.g., removing a meteorology record or a temperature target set).
	 *
	 * @param fsg  the {@link ForecastSimGroup} from which {@code data} is removed
	 * @param data the data item to remove
	 */
	protected abstract void removeData(ForecastSimGroup fsg, T data);

	/**
	 * Called when a row in the panel's primary table is selected.
	 *
	 * Concrete subclasses implement this to populate the lower panel with the data
	 * associated with the selected row.
	 *
	 * @param selectedRow the zero-based index of the newly selected row
	 */
	protected abstract void tableRowSelected(int selectedRow);

	/**
	 * Called when the "Delete..." popup menu item is clicked for a row in the panel's
	 * primary table.
	 *
	 * Concrete subclasses implement this to perform the type-specific delete workflow
	 * (confirmation dialog, cascade checks, and removal).
	 *
	 * @param selectedRow the zero-based index of the row to delete
	 */
	public abstract void tableRowDeleteClicked(int selectedRow);

	/**
	 * Populates the lower panel with subclass-specific controls and components.
	 *
	 * Called once during construction after the upper table region has been built.
	 *
	 * @param lowerPanel the {@link EnabledJPanel} that serves as the lower content area;
	 *                   subclasses add their controls directly to this panel
	 */
	protected abstract void buildLowerPanel(EnabledJPanel lowerPanel);

	/**
	 * Deletes the given data item, optionally indicating that the deletion is triggered
	 * by an overwrite rather than a direct user delete action.
	 *
	 * @param data                 the data item to delete
	 * @param deleteDueToOverwrite {@code true} if this delete is part of an overwrite
	 *                             operation; adjusts the confirmation message wording
	 * @return {@code true} if the deletion was confirmed and completed; {@code false}
	 * if the user cancelled
	 */
	protected abstract boolean delete(T data, boolean deleteDueToOverwrite);

	/**
	 * Opens or refreshes the import forecast data dialog for this panel.
	 *
	 * Called when the user must be returned to the import dialog after cancelling an
	 * overwrite confirmation.
	 *
	 * @param dlg the {@link ImportForecastWindow} to open or bring to focus
	 */
	protected abstract void importForecastData(ImportForecastWindow dlg);

	/**
	 * Returns the primary {@link ForecastTable} associated with this panel — the one
	 * table among the five that this panel is responsible for populating and managing.
	 *
	 * @return the primary {@link ForecastTable} for this panel; must not be {@code null}
	 */
	public abstract ForecastTable getTableForPanel();

	/**
	 * Updates the enabled state of all five forecast tables so that only the primary
	 * table for this panel is enabled (highlighted with a black border), while the
	 * remaining four are visually dimmed.
	 *
	 * Called when this panel's tab is activated to provide a visual cue about which
	 * table column is currently active.
	 */
	protected void panelActivated() {
		ForecastTable panelTable = getTableForPanel();

		// Enable only the primary table for this panel; disable all others
		ForecastTable ftable;
		for (int i = 0; i < _tables.size(); i++) {
			ftable = _tables.get(i);
			ftable.setEnabled(ftable == panelTable);
		}
	}

	/**
	 * Replaces an existing data item in the table and data list with a new item of the
	 * same name, after obtaining user confirmation via {@link #delete(Object, boolean)}.
	 *
	 * If the user confirms the overwrite:
	 *
	 *   The existing item is deleted from the simulation group.
	 *   The new item is inserted into the data list at the same index.
	 *   The table row at {@code rowToReplace} is updated with the new item.
	 *   The panel is notified of the replaced row via {@link #tableRowSelected}.
	 *
	 *
	 * @param table                the {@link ForecastTable} containing the row to replace
	 * @param dataList             the backing data list in which the item is replaced
	 * @param dataBeingOverwritten the existing data item that will be deleted
	 * @param newData              the new data item that replaces it
	 * @param rowToReplace         the zero-based table row index at which the replacement occurs
	 * @return {@code true} if the overwrite was confirmed and completed; {@code false} if
	 * the user cancelled
	 */
	private boolean deleteForOverwrite(ForecastTable table, List<T> dataList, T dataBeingOverwritten, T newData, int rowToReplace) {
		boolean retVal = false;

		// Record the list position of the item being replaced before deletion removes it
		int indexToReplace = dataList.indexOf(dataBeingOverwritten);

		if (delete(dataBeingOverwritten, true)) {
			retVal = true;

			// Insert the new item at the same list position as the deleted item
			dataList.add(indexToReplace, newData);

			// Replace the table row with a single-element vector containing the new item
			table.insertRow(new Vector<>(Collections.singletonList(newData)), rowToReplace);

			// Notify the owning panel so the lower panel reflects the replaced item
			AbstractForecastPanel panel = getPanelForTable(table);
			panel.tableRowSelected(rowToReplace);
		}

		return retVal;
	}

	/**
	 * Persists the current state of this panel's lower-panel controls to the underlying
	 * data model.
	 *
	 * Concrete subclasses implement this to write form field values back to the
	 * appropriate data object.
	 */
	protected abstract void savePanel();

	/**
	 * Loads the given {@link ForecastSimGroup} into the panel, replacing the current table
	 * contents and re-attaching selection listeners around the fill operation to prevent
	 * spurious selection events during the data load.
	 *
	 * Selection listeners are temporarily removed before {@link #fillPanel(ForecastSimGroup)}
	 * is called and re-added afterward, ensuring that programmatic row changes during the
	 * fill do not trigger the selection handler.
	 *
	 * @param fsg the {@link ForecastSimGroup} whose data is to be displayed in this panel
	 */
	public void setSimulationGroup(ForecastSimGroup fsg) {
		// Capture the current selection listeners so they can be removed and re-added
		DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) getTableForPanel().getSelectionModel();
		ListSelectionListener[] selectionListeners = selectionModel.getListeners(ListSelectionListener.class);

		// Remove listeners to suppress selection events fired during the data fill
		removeSelectionListeners(selectionListeners);

		// Populate the table with the simulation group's data
		fillPanel(fsg);

		// Restore listeners after the fill is complete
		addSelectionListeners(selectionListeners);
	}

	/**
	 * Re-attaches an array of {@link ListSelectionListener} objects to the primary table's
	 * selection model.
	 *
	 * @param selectionListeners the listeners to re-add; must not be {@code null}
	 */
	private void addSelectionListeners(ListSelectionListener[] selectionListeners) {
		for (ListSelectionListener selectionListener : selectionListeners) {
			getTableForPanel().getSelectionModel().addListSelectionListener(selectionListener);
		}
	}

	/**
	 * Detaches an array of {@link ListSelectionListener} objects from the primary table's
	 * selection model.
	 *
	 * @param selectionListeners the listeners to remove; must not be {@code null}
	 */
	private void removeSelectionListeners(ListSelectionListener[] selectionListeners) {
		for (ListSelectionListener selectionListener : selectionListeners) {
			getTableForPanel().getSelectionModel().removeListSelectionListener(selectionListener);
		}
	}

	/**
	 * Populates this panel's table with data from the given {@link ForecastSimGroup}.
	 * <p>
	 * Concrete subclasses implement this to read the relevant data items from the
	 * simulation group and add them as rows to the primary table.
	 *
	 * @param fsg the {@link ForecastSimGroup} whose data should be displayed
	 */
	public abstract void fillPanel(ForecastSimGroup fsg);

	// -------------------------------------------------------------------------
	// Inner class: ForecastTable
	// -------------------------------------------------------------------------

	/**
	 * A specialised {@link RmaJTable} used as the data-selection table within each
	 * {@link AbstractForecastPanel}.
	 *
	 * {@code ForecastTable} extends {@link RmaJTable} to add:
	 *
	 *   A "Delete..." right-click popup menu item.
	 *   Right-click row tracking via a {@link MouseAdapter} so the popup knows which row was clicked.
	 *   A fixed preferred viewport height of four rows.
	 *   Non-editable cells (all cells return {@code false} from {@link #isCellEditable}).
	 *   A visual enabled/disabled state that highlights the active table with a
	 *       two-pixel black border and dims inactive tables using the panel background color.
	 *   Name-based row lookup utilities ({@link #isNameUsed} and
	 *       {@link #getRowWithName}).
	 *
	 */
	protected class ForecastTable extends RmaJTable {
		// The "Delete..." popup menu item added to the right-click context menu
		private JMenuItem _deleteMenuItem;

		// The AbstractForecastPanel that owns and manages this table
		private AbstractForecastPanel _parentForecastPanel;

		// The default scroll pane border stored during construction; restored when the table is disabled
		private Border _defaultBorder;

		// The row index recorded when the user right-clicks to open the popup menu; -1 if none
		private int _popupMenuRow = -1;

		/**
		 * Constructs a {@code ForecastTable} with a single-column header array and
		 * an auto-generated {@link RmaTableModel}.
		 *
		 * Increases the row height by 5 pixels for readability, stores the default scroll
		 * pane border, builds the right-click popup menu, and attaches the mouse listener
		 * that records the right-clicked row.
		 *
		 * @param parent  the {@link AbstractForecastPanel} that owns this table
		 * @param headers the column header labels; typically a single-element array
		 */
		protected ForecastTable(AbstractForecastPanel parent, String[] headers) {
			// Delegate header and parent initialisation to the base RmaJTable constructor
			super(parent, headers);

			_parentForecastPanel = parent;

			// Increase row height slightly for improved readability
			setRowHeight(getRowHeight() + 5);

			// Cache the default border so it can be restored when the table is disabled
			_defaultBorder = getScrollPane().getBorder();

			// Build and attach the right-click popup menu with a Delete item
			buildPopupMenu();

			// Attach the mouse listener that records which row was right-clicked
			addForecastTableListeners();
		}

		/**
		 * Constructs a {@code ForecastTable} using an existing {@link RmaTableModel}.
		 *
		 * Used when re-using a shared static table model across multiple panel instances.
		 * Does not adjust row height or store the default border (those are handled when
		 * the header-array constructor is used on first creation).
		 *
		 * @param parent     the {@link AbstractForecastPanel} that owns this table
		 * @param tableModel the pre-existing {@link RmaTableModel} to use as this table's model
		 */
		protected ForecastTable(AbstractForecastPanel parent, RmaTableModel tableModel) {
			// Delegate model and parent initialisation to the base RmaJTable constructor
			super(parent, tableModel);

			_parentForecastPanel = parent;

			// Build and attach the popup menu for this table instance
			buildPopupMenu();

			// Attach the mouse listener that records which row was right-clicked
			addForecastTableListeners();
		}

		/**
		 * Registers mouse listeners on this forecast table. Currently attaches a MouseAdapter
		 * that intercepts right-click press events and records the row index under the cursor
		 * in _popupMenuRow so that the popup menu's delete action can target the correct row.
		 */
		private void addForecastTableListeners() {
			// Attach a mouse listener to intercept press events on the table
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					// Capture the screen coordinates of the mouse press
					Point clickPoint = e.getPoint();

					if (SwingUtilities.isRightMouseButton(e)) {
						// Record the row under the right-click point for use by the popup delete action
						_popupMenuRow = rowAtPoint(clickPoint);
					}
				}
			});
		}

		/**
		 * Builds and attaches the right-click popup menu, adding a "Delete..." item at
		 * position 0 and removing the fill, sum, and row-editing options provided by the
		 * base {@link RmaJTable}.
		 */
		private void buildPopupMenu() {
			// Create and register the "Delete..." popup item at the top of the context menu
			_deleteMenuItem = new JMenuItem("Delete...");
			addPopupItem(_deleteMenuItem, 0);

			// Remove base-class popup options that are not applicable to forecast tables
			removePopuMenuFillOptions();
			removePopupMenuSumOptions();
			removePopupMenuRowEditingOptions();
		}

		/**
		 * Returns the "Delete..." popup menu item for this table.
		 *
		 * Used by the parent panel to attach an action listener and by the
		 * initial conditions table setup to remove the item entirely.
		 *
		 * @return the delete {@link JMenuItem}; never {@code null} after construction
		 */
		private JMenuItem getDeleteMenuItem() {
			return _deleteMenuItem;
		}

		/**
		 * Returns the zero-based row index that was right-clicked to open the popup menu,
		 * or {@code -1} if the popup has not been opened yet.
		 *
		 * @return the right-clicked row index, or {@code -1}
		 */
		private int getPopupMenuRow() {
			return _popupMenuRow;
		}

		/**
		 * Returns the preferred scrollable viewport size with the height fixed to exactly
		 * four rows, regardless of the table's actual row count.
		 *
		 * @return a {@link Dimension} whose height equals four times the row height and
		 * whose width is inherited from the base class
		 */
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			Dimension d = super.getPreferredScrollableViewportSize();

			// Fix the viewport height to four rows to keep the upper table region compact
			d.height = getRowHeight() * 4;

			return d;
		}

		/**
		 * Returns {@code false} for all cells, making the entire table read-only.
		 *
		 * Row selection is the only supported interaction; inline editing is not permitted.
		 *
		 * @param row the row index (ignored)
		 * @param col the column index (ignored)
		 * @return {@code false} always
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		/**
		 * Toggles the visual enabled/disabled state of this forecast table.
		 *
		 * When {@code enabled} is {@code true} (i.e., this table is the active one):
		 *
		 *   The scroll pane and header background are set to the look-and-feel's default table background color.
		 *   A two-pixel black {@link LineBorder} is applied to the scroll pane to highlight the active table.
		 *
		 * When {@code enabled} is {@code false}:
		 *
		 *   Backgrounds are set to the parent panel's background color to visually dim the inactive table.
		 *   The default border is restored.
		 *
		 * Note: the {@code enabled} parameter is intentionally inverted when passed to
		 * {@code super.setEnabled} because the base class disables interaction when
		 * {@code enabled} is {@code true} in this context (the active table should be
		 * selectable but not editable).
		 *
		 * @param enabled {@code true} to visually activate this table; {@code false} to dim it
		 */
		@Override
		public void setEnabled(boolean enabled) {
			// Use the look-and-feel table background for active tables; parent background for inactive
			Color bcColor = enabled ? UIManager.getColor("Table.background") : AbstractForecastPanel.this.getBackground();

			getScrollPane().setBackground(bcColor);
			getTableHeader().setBackground(bcColor);
			getTableHeader().setEnabled(enabled);

			// Apply a bold border to the active table; restore the default border for inactive ones
			getScrollPane().setBorder(enabled ? new LineBorder(Color.black, 2) : _defaultBorder);

			// Invert the flag for the base class: the active table must remain interactable
			super.setEnabled(!enabled);
		}

		/**
		 * Returns whether a row with the given name already exists in this table's first
		 * column.
		 *
		 * Performs a case-insensitive comparison against the {@code toString()} value of
		 * each cell in column 0.
		 *
		 * @param name the name to search for; must not be {@code null}
		 * @return {@code true} if a matching row exists; {@code false} otherwise
		 */
		public boolean isNameUsed(String name) {
			return getRowWithName(name) >= 0;
		}

		/**
		 * Searches the first column of the table for a row whose value matches the given name,
		 * using a case-insensitive comparison. Returns the index of the first matching row,
		 * or -1 if no row contains a matching value. Null cell values are safely skipped.
		 *
		 * @param name the name to search for in the first column
		 * @return     the zero-based index of the first matching row, or -1 if no match is found
		 */
		public int getRowWithName(String name) {
			// Default to -1 to indicate no match found
			int retVal = -1;

			// Iterate over every row in the table to find a name match in the first column
			for (int row = 0; row < getRowCount(); row++) {
				// Retrieve the value from the first column of the current row
				Object value = getValueAt(row, 0);

				// Skip null values and compare the cell value against the target name (case-insensitive)
				if (value != null && value.toString().equalsIgnoreCase(name)) {
					// Match found; record the row index and stop searching
					retVal = row;
					break;
				}
			}

			// Return the matching row index, or -1 if no match was found
			return retVal;
		}
	}
}
