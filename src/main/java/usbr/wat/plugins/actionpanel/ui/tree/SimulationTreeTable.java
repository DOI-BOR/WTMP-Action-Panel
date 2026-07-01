package usbr.wat.plugins.actionpanel.ui.tree;

import java.awt.Color;                                  // Provides Color for the alternating row background and black border colours
import java.awt.Component;                              // Provides Component for popup menu component inspection and renderer return types
import java.awt.Point;                                  // Provides Point for translating mouse click coordinates to tree path locations
import java.awt.event.MouseAdapter;                     // Provides MouseAdapter for the right-click popup menu listener
import java.awt.event.MouseEvent;                       // Provides MouseEvent for inspecting whether a mouse release is a popup trigger

import java.util.ArrayList;                             // Provides ArrayList for collecting selected simulation and results lists
import java.util.List;                                  // Provides the List interface for ordered collections of WatSimulation and ResultsData

import javax.swing.BorderFactory;                       // Provides BorderFactory for creating the default black line border
import javax.swing.JButton;                             // Provides JButton for the "Show on Map" and "View Report" button cell editors
import javax.swing.JCheckBox;                           // Provides JCheckBox as the base class for BooleanRenderer and the checkbox cell editor
import javax.swing.JLabel;                              // Provides JLabel for the CENTER alignment constant used in BooleanRenderer
import javax.swing.JMenuItem;                           // Provides JMenuItem for context menu items added to the popup menu
import javax.swing.JPopupMenu;                          // Provides JPopupMenu for the right-click context menu constructed on mouse release
import javax.swing.JTable;                              // Provides JTable for the table parameter in renderer getTableCellRendererComponent methods
import javax.swing.SwingConstants;                      // Provides SwingConstants for the RIGHT alignment constant in decimal field editors
import javax.swing.SwingUtilities;                      // Provides SwingUtilities for checking whether a mouse button is the right mouse button
import javax.swing.UIManager;                           // Provides UIManager for retrieving LAF-defined colours for table background and selection
import javax.swing.border.Border;                       // Provides Border as the type for the stored default border
import javax.swing.table.TableCellRenderer;             // Provides TableCellRenderer for the renderer interfaces implemented by inner classes
import javax.swing.table.TableColumn;                   // Provides TableColumn for accessing and configuring individual table columns
import javax.swing.table.TableColumnModel;              // Provides TableColumnModel for accessing columns by index during editor/renderer installation
import javax.swing.tree.MutableTreeNode;                // Provides MutableTreeNode for looking up the WAT simulation node in the project tree
import javax.swing.tree.TreeCellRenderer;               // Provides TreeCellRenderer for wrapping the default renderer in IconTableCellRenderer
import javax.swing.tree.TreePath;                       // Provides TreePath for locating the node at a mouse click position and querying editability

import org.jdesktop.swingx.JXButton;                        // Provides JXButton for the button used as a cell editor in button columns
import org.jdesktop.swingx.JXTree;                          // Provides JXTree for the DelegatingRenderer type used when installing the custom tree renderer
import org.jdesktop.swingx.JXTree.DelegatingRenderer;       // Provides DelegatingRenderer for accessing and replacing the inner tree cell renderer
import org.jdesktop.swingx.decorator.AbstractHighlighter;   // Provides AbstractHighlighter as the base class for the ReportRowHighlighter inner class
import org.jdesktop.swingx.decorator.ComponentAdapter;      // Provides ComponentAdapter for accessing row/column information inside the highlighter
import org.jdesktop.swingx.decorator.Highlighter;           // Provides Highlighter as the interface type for the row background/foreground colouring hook
import org.jdesktop.swingx.treetable.TreeTableModel;        // Provides TreeTableModel as the model type passed to the RmaJXTreeTable superclass
import org.jdesktop.swingx.treetable.TreeTableNode;         // Provides TreeTableNode for iterating over root children when searching for a simulation node

import com.rma.client.Browser;                              // Provides Browser for accessing the WAT project tree to look up WatSimulationNode references
import hec2.wat.model.WatSimulation;                        // Provides WatSimulation for identifying selected simulation rows and building popup menus
import hec2.wat.ui.WatSimulationNode;                       // Provides WatSimulationNode for obtaining the WAT-standard popup menu for a simulation

import rma.swing.RmaJCheckBox;                              // Provides RmaJCheckBox for the checkbox cell editor with RMA enable/disable support
import rma.swing.RmaJDecimalField;                          // Provides RmaJDecimalField for the decimal value cell editor installed in numeric columns
import rma.swing.RmaJIntegerField;                          // Provides RmaJIntegerField (imported for potential integer field use in column editors)
import rma.swing.RmaJIntegerSetField;                       // Provides RmaJIntegerSetField for the comma-separated integer-set cell editor
import rma.swing.RmaJTable;                                 // Provides RmaJTable (imported as a related RMA table type; not directly used here)
import rma.swing.RmaJXTreeTable;                            // Provides RmaJXTreeTable, the RMA SwingX tree-table base class this class extends
import rma.swing.table.AlignTableCellRenderer;              // Provides AlignTableCellRenderer for applying horizontal alignment to numeric columns
import rma.swing.table.DecimalCellRenderer;                 // Provides DecimalCellRenderer (imported for potential decimal rendering; currently commented out)
import rma.swing.table.RmaCellEditor;                       // Provides RmaCellEditor for wrapping field and button components as table cell editors
import rma.swing.table.RmaTableModelInterface;              // Provides RmaTableModelInterface for setting column class metadata on the table model
import rma.util.RMAIO;                                      // Provides RMAIO for boolean parsing used when reading selected-column checkbox values

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;              // Provides ActionPanelPlugin for accessing the singleton plugin instance and its windows
import usbr.wat.plugins.actionpanel.actions.SaveSimulationAsAction; // Provides SaveSimulationAsAction for the "Save As..." context menu action
import usbr.wat.plugins.actionpanel.model.ResultsData;              // Provides ResultsData for identifying selected ResultsData rows in the tree-table
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                   // Provides UsbrPanel for the parent panel reference that drives simulation actions

/**
 * A specialised RmaJXTreeTable that displays WAT simulations and their associated
 * results in a hierarchical tree-table. The tree has two levels:
 *
 * Root children are SimulationTreeTableNode entries, each representing one WAT simulation.
 * Their children are ResultsTreeTableNode entries, each representing a saved results set.
 *
 * The table has four columns (defined in SimulationTreeTableModel):
 * SELECTED_COLUMN        — a checkbox for selecting simulations or results.
 * SIMULATION_COLUMN      — the simulation or results name with the tree expand control.
 * DISPLAY_IN_MAPS_COLUMN — a "Show on Map" button.
 * VIEW_REPORT_COLUMN     — a "View Report" button.
 *
 * A row highlighter (ReportRowHighlighter) applies alternating background colours:
 * simulation rows use _oddRowBackground and results rows use the default table background.
 * Compute-state foreground colours are applied via getRowForeground from the superclass.
 *
 * A right-click popup menu is assembled from the WAT simulation's standard popup
 * (obtained from the project tree node), supplemented with "Save As...",
 * "Show In Study Tree", and "Edit MetaData..." items. Node-type-specific items are
 * contributed by ActionsTreeTableNode.addPopupMenuItems.
 *
 * @see SimulationTreeTableModel
 * @see SimulationTreeTableNode
 * @see ResultsTreeTableNode
 * @see IconTableCellRenderer
 */
@SuppressWarnings("serial")
public class SimulationTreeTable extends RmaJXTreeTable {
	/**
	 * Background colour applied to unselected simulation (parent) rows for visual alternation.
	 */
	public static final Color _oddRowBackground = new Color(239, 247, 254);

	/**
	 * Default black line border stored for potential future use in cell rendering.
	 */
	private Border _defaultBorder;

	/**
	 * Row highlighter that applies alternating background colours and per-row compute-state
	 * foreground colours to unselected rows. Re-applied after each UI update because
	 * JXTreeTable does not reliably reinstall highlighters after a LAF change.
	 */
	private Highlighter _tableRowHighlighter = new ReportRowHighligher();

	/**
	 * The parent UsbrPanel that provides action callbacks (map display, report view, etc.).
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Decimal precision used when constructing decimal cell editors for numeric columns.
	 */
	private int _precision = 3;

	/**
	 * Constructs a new SimulationTreeTable wired to the given parent panel. Creates
	 * the initial SimulationTreeTableModel, registers the right-click popup listener,
	 * installs the custom tree cell renderer, and applies the row highlighter.
	 *
	 * @param parentPanel the UsbrPanel that owns this tree-table and handles action callbacks
	 */
	public SimulationTreeTable(UsbrPanel parentPanel) {
		super(createTreeModel());
		_parentPanel = parentPanel;

		// Register the right-click popup listener before renderers so both are active together
		adPopupListener();
		setRenderers();

		// Install the alternating-row highlighter; re-applied after UI updates in updateUI()
		setHighlighters(_tableRowHighlighter);
	}

	/**
	 * Registers a MouseAdapter that detects right-click (popup trigger) events on the
	 * tree-table. On right-click, the node at the mouse position is identified, a
	 * context popup is constructed, node-specific items are added via ActionsTreeTableNode,
	 * and the popup is displayed at the click location.
	 */
	private void adPopupListener() {
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
					Point pt = e.getPoint();

					// Resolve the click coordinates to a tree path; ignore clicks on empty space
					TreePath path = getPathForLocation((int) pt.getX(), (int) pt.getY());
					if (path == null) {
						return;
					}

					Object lastComp = path.getLastPathComponent();
					WatSimulation sim = null;

					// Extract the WatSimulation from the node if it is a simulation node
					if (lastComp instanceof SimulationTreeTableNode) {
						sim = ((SimulationTreeTableNode) lastComp).getSimulation();
					}

					// Build the base popup (WAT standard items + Save As + Show In Tree)
					JPopupMenu popup = createPopupMenu(sim);

					// Let the node contribute its own context menu items (e.g. Rename)
					if (lastComp instanceof ActionsTreeTableNode) {
						((ActionsTreeTableNode) lastComp).addPopupMenuItems(popup);
					}

					popup.show(SimulationTreeTable.this, pt.x, pt.y);
				}
			}
		};
		addMouseListener(ma);
	}

	/**
	 * Replaces the JXTreeTable's default delegating tree cell renderer with a custom
	 * IconTableCellRenderer that overlays node-type-specific icons and per-row foreground
	 * colours. Also enables the table grid lines. Does nothing when the current renderer
	 * is not a JXTree.DelegatingRenderer.
	 */
	private void setRenderers() {
		TreeCellRenderer rend = getTreeCellRenderer();

		if (rend instanceof JXTree.DelegatingRenderer) {
			JXTree.DelegatingRenderer delRend = (DelegatingRenderer) rend;

			// Unwrap the inner delegate renderer to pass it to the custom renderer
			TreeCellRenderer treeCellRenderer = delRend.getDelegateRenderer();
			IconTableCellRenderer diffrenderer = new IconTableCellRenderer(this, treeCellRenderer);

			// Replace the delegate so all tree cell rendering goes through the custom renderer
			delRend.setDelegateRenderer(diffrenderer);
			setShowGrid(true, true);
		}
	}

	/**
	 * Builds and returns the right-click context popup menu for the given simulation.
	 * The menu is seeded with items from the WAT simulation's standard popup (obtained
	 * from the project tree node), then "Save As...", a separator, "Show In Study Tree",
	 * and "Edit MetaData..." are appended. "View Compute Log..." is created but currently
	 * commented out. When sim is null (e.g. for a results node), the WAT items are omitted.
	 *
	 * @param sim the WatSimulation to build the popup for; may be null for results nodes
	 * @return a fully assembled JPopupMenu ready to display
	 */
	private JPopupMenu createPopupMenu(WatSimulation sim) {
		JPopupMenu popup = new JPopupMenu();
		JPopupMenu simPopup = null;

		if (sim != null) {
			// Look up the simulation's tree node in the WAT project tree to get its standard popup
			MutableTreeNode node = Browser.getBrowserFrame().getProjectTree().getNodeForManager(sim);
			if (node instanceof WatSimulationNode) {
				WatSimulationNode simNode = (WatSimulationNode) node;
				simPopup = simNode.buildPopupMenu();
			}
		}

		if (simPopup != null) {
			// Copy all WAT standard popup items into our popup in their original order
			int compCnt = simPopup.getComponentCount();
			for (int i = compCnt - 1; i >= 0; i--) {
				// Insert at position 0 each time so the reverse-iteration preserves original order
				popup.add(simPopup.getComponent(i), 0);
			}
		}

		// Insert "Save As..." directly after the existing "Save" item if present
		JMenuItem saveAsMenuItem = new JMenuItem("Save As...");
		saveAsMenuItem.addActionListener(e -> saveSimulationAs(sim));
		int idx = findSaveMenuIndex(popup);
		popup.add(saveAsMenuItem, idx);
		popup.addSeparator();

		// Add study tree and metadata items below the separator
		JMenuItem showInProjectTreeMenu = new JMenuItem("Show In Study Tree");
		showInProjectTreeMenu.addActionListener(e -> _parentPanel.showInProjectTreeAction());
		popup.add(showInProjectTreeMenu);

		JMenuItem editMetaDataMenu = new JMenuItem("Edit MetaData...");
		editMetaDataMenu.addActionListener(e -> _parentPanel.editSimulationMetaData());
		popup.add(editMetaDataMenu);

		// View Compute Log is defined but disabled pending further implementation
		JMenuItem displayLogMenu = new JMenuItem("View Compute Log...");
		displayLogMenu.addActionListener(e -> _parentPanel.displayComputeLog());
		//popup.add(displayLogMenu);

		return popup;
	}

	/**
	 * Invokes the SaveSimulationAsAction to save the given WatSimulation under a new name,
	 * using the calibration panel's current simulation group as the source context.
	 *
	 * @param sim the WatSimulation to save as a new copy; may be null (action handles null gracefully)
	 */
	private void saveSimulationAs(WatSimulation sim) {
		SaveSimulationAsAction ssa = new SaveSimulationAsAction(_parentPanel);
		ssa.saveSimulationAs(ActionPanelPlugin.getInstance().getActionsWindow().getCalibrationPanel().getSimulationGroup(), sim);
	}

	/**
	 * Scans the given popup menu in reverse order for a "Save" menu item and returns
	 * the index immediately after it, suitable for inserting "Save As..." adjacent to "Save".
	 * Falls back to the last position when no "Save" item is found.
	 *
	 * @param popup the JPopupMenu to search for a "Save" item
	 * @return the index at which "Save As..." should be inserted
	 */
	private int findSaveMenuIndex(JPopupMenu popup) {
		int compCnt = popup.getComponentCount();

		// Scan in reverse so the last "Save" item wins in case of duplicates
		for (int i = compCnt - 1; i >= 0; i--) {
			Component comp = popup.getComponent(i);
			if (comp instanceof JMenuItem) {
				if (((JMenuItem) comp).getText().equals("Save")) {
					// Return the position after "Save" so "Save As..." follows it
					return i + 1;
				}
			}
		}

		// No "Save" found: append "Save As..." at the end of the current items
		return compCnt - 1;
	}

	/**
	 * Creates and returns the initial SimulationTreeTableModel with a null root, used
	 * as the model argument to the RmaJXTreeTable constructor.
	 *
	 * @return a new SimulationTreeTableModel with no data
	 */
	private static TreeTableModel createTreeModel() {
		return new SimulationTreeTableModel(null);
	}

	/**
	 * Overrides the superclass init to ensure the default border is created during
	 * component initialisation if it has not already been set.
	 */
	@Override
	protected void init() {
		super.init();

		if (_defaultBorder == null) {
			createDefaultBorder();
		}
	}

	/**
	 * Creates and stores a solid black line border as the component's default border.
	 * Available for subclasses that need a consistent border reference.
	 */
	protected void createDefaultBorder() {
		_defaultBorder = BorderFactory.createLineBorder(Color.black);
	}

	/**
	 * Installs a checkbox cell editor on the given column using the default
	 * selection background behaviour (useSelectionBackground = true).
	 *
	 * @param col the zero-based column index to install the checkbox editor on
	 * @return the RmaJCheckBox used as the editor component
	 */
	public JCheckBox setCheckBoxCellEditor(int col) {
		return setCheckBoxCellEditor(col, true);
	}

	/**
	 * Overrides updateUI to work around a JXTreeTable bug where highlighters are not
	 * reinstalled after a look-and-feel change. Removes the row highlighter before
	 * delegating to the superclass, then re-adds it afterwards to ensure it remains active.
	 * <p>
	 * Note: JTable handles striping automatically but JXTable's default renderer ignores it,
	 * so JXTreeTable inherits this broken behaviour and requires the manual re-application here.
	 */
	@Override
	public void updateUI() {
		// Guard against being called before the highlighter field is initialised
		if (_tableRowHighlighter == null) {
			super.updateUI();
			return;
		}

		// Remove before super.updateUI() to prevent a stale duplicate being kept
		removeHighlighter(_tableRowHighlighter);

		super.updateUI();

		// JTable does this striping automatically but JXTable's default renderer
		// seems to ignore it, so JXTreeTable inherits this broken behaviour.
		addHighlighter(_tableRowHighlighter);
	}

	/**
	 * Installs a checkbox cell editor and a BooleanRenderer on the given column.
	 * The editor activates on a single click. The renderer uses the selection background
	 * colour when the cell is selected, controlled by the useSelectionBackground flag.
	 *
	 * @param col                    the zero-based column index to configure
	 * @param useSelectionBackground true to paint the selection background when the cell is selected
	 * @return the RmaJCheckBox used as the editor component
	 */
	public JCheckBox setCheckBoxCellEditor(int col, boolean useSelectionBackground) {
		RmaJCheckBox cb = new RmaJCheckBox();
		cb.setFocusPainted(false);

		// Wrap the checkbox in an RmaCellEditor that starts editing on a single click
		RmaCellEditor ce = new RmaCellEditor(cb);
		ce.setClickCountToStart(1);
		getColumnModel().getColumn(col).setCellEditor(ce);
		getColumnModel().getColumn(col).setCellRenderer(createBooleanRenderer(useSelectionBackground));
		return cb;
	}

	/**
	 * Factory method that creates a BooleanRenderer for use in checkbox columns.
	 * Overridable by subclasses that need a custom boolean cell renderer.
	 *
	 * @param useSelectionBackground true to apply the selection background when the cell is selected
	 * @return a new BooleanRenderer configured with the given selection background flag
	 */
	protected TableCellRenderer createBooleanRenderer(boolean useSelectionBackground) {
		return new BooleanRenderer(useSelectionBackground);
	}

	/**
	 * Installs a JXButton cell editor and a ButtonRenderer on the given column, and
	 * returns the button so the caller can set its text and add ActionListeners.
	 *
	 * @param col the zero-based column index to configure as a button column
	 * @return the JXButton installed as the cell editor component
	 */
	public JButton setButtonCellEditor(int col) {
		JXButton button = new JXButton();
		button.setFocusPainted(false);

		// Wrap the button in an RmaCellEditor and install both editor and renderer
		RmaCellEditor ce = new RmaCellEditor(button);
		getColumnModel().getColumn(col).setCellEditor(ce);
		getColumnModel().getColumn(col).setCellRenderer(new ButtonRenderer());
		return button;
	}

	/**
	 * Installs a decimal field cell editor on the specified column and configures the
	 * column for numeric display and sorting. The editor is created with the table's
	 * configured precision and right-aligned. If the table model implements
	 * RmaTableModelInterface, the column class is declared as Number so the model
	 * sorts and filters the column correctly. Returns null if the column index is out
	 * of bounds or the TableColumn cannot be retrieved.
	 *
	 * @param col the zero-based index of the column on which to install the decimal editor
	 * @return    the RmaJDecimalField installed as the cell editor, or null if the column
	 *            index is invalid or the TableColumn is null
	 */
	public RmaJDecimalField setDoubleCellEditor(int col) {
		// Retrieve the column model to validate the column index and access the target column
		TableColumnModel tcm = getColumnModel();

		// Only proceed if the column index is within the valid range
		if (col < tcm.getColumnCount() && col >= 0) {
			// Retrieve the TableColumn object for the specified column index
			TableColumn tc = getColumnModel().getColumn(col);

			if (tc == null) {
				// No TableColumn exists for this index; return null to signal failure
				return null;
			} else {
				// Create a decimal field with the configured precision and wrap it in an editor
				RmaJDecimalField df = createDecimalField(col, _precision);
				RmaCellEditor dcf = new RmaCellEditor(df);

				// Install the decimal cell editor on the target column
				tc.setCellEditor(dcf);

				// Apply right-alignment to the column (alignment constant 4 = RIGHT)
				setHorizontalAlignment(4, col);

				// Declare the column class as Number so the model sorts and filters correctly
				if (this.getModel() instanceof RmaTableModelInterface) {
					((RmaTableModelInterface) this.getModel()).setColumnClass(col, Number.class);
				}

				// Return the decimal field so the caller can configure it further if needed
				return df;
			}
		}

		// Column index is out of bounds; return null to signal that no editor was installed
		return null;
	}

	/**
	 * Installs an integer-set field cell editor on the specified column and configures
	 * the column for numeric display and sorting. The table is registered as a mouse
	 * listener on the field so it can intercept mouse events during editing. The column
	 * is right-aligned and, if the model implements RmaTableModelInterface, its class is
	 * declared as Number for correct sorting and filtering behavior. Returns null if the
	 * column index is out of bounds or the TableColumn cannot be retrieved.
	 *
	 * @param col the zero-based index of the column on which to install the integer-set editor
	 * @return    the RmaJIntegerSetField installed as the cell editor, or null if the column
	 *            index is invalid or the TableColumn is null
	 */
	public RmaJIntegerSetField setIntegerSetCellEditor(int col) {
		// Retrieve the column model to validate the column index and access the target column
		TableColumnModel tcm = this.getColumnModel();

		// Only proceed if the column index is within the valid range
		if (col < tcm.getColumnCount() && col >= 0) {
			// Retrieve the TableColumn object for the specified column index
			TableColumn tc = this.getColumnModel().getColumn(col);

			if (tc == null) {
				// No TableColumn exists for this index; return null to signal failure
				return null;

			} else {
				// Create the integer-set field and register the table as a mouse listener
				RmaJIntegerSetField df = new RmaJIntegerSetField();
				df.addMouseListener(this);

				// Wrap in an RmaCellEditor and install it on the column
				RmaCellEditor dcf = new RmaCellEditor(df);
				tc.setCellEditor(dcf);

				// Apply right-alignment (constant 4 = RIGHT) to the column renderer
				setHorizontalAlignment(4, col);

				// Declare the column class as Number so the model handles it correctly
				if (getModel() instanceof RmaTableModelInterface) {
					((RmaTableModelInterface) getModel()).setColumnClass(col, Number.class);
				}

				// Return the integer-set field so the caller can configure it further if needed
				return df;
			}
		}

		// Column index is out of bounds; return null to signal that no editor was installed
		return null;
	}

	/**
	 * Sets the horizontal alignment of the cell renderer for the specified column.
	 * An AlignTableCellRenderer is created with the given alignment constant and
	 * installed on the column. If the column index is out of bounds or the TableColumn
	 * cannot be retrieved, no action is taken.
	 *
	 * @param align the horizontal alignment constant to apply (e.g. 2 = LEFT, 0 = CENTER, 4 = RIGHT)
	 * @param col   the zero-based index of the column whose renderer alignment will be set
	 */
	public void setHorizontalAlignment(int align, int col) {
		// Only proceed if the column index is within the valid range
		if (col < this.getColumnCount()) {
			// Create a cell renderer configured with the specified horizontal alignment
			AlignTableCellRenderer rtcr = createAlignTableCellRenderer(align);

			// Retrieve the TableColumn object for the specified column index
			TableColumn tc = this.getColumnModel().getColumn(col);

			if (tc != null) {
				// Install the alignment renderer on the target column
				tc.setCellRenderer(rtcr);
			}
		}
	}

	/**
	 * Creates and returns a configured RmaJDecimalField for use as a table cell editor.
	 * The field is initialized with a default value of 0 and a column width of 5,
	 * then configured with the specified precision, right-aligned, and registered with
	 * this table as a mouse listener so the table can intercept mouse events during editing.
	 *
	 * @param col       the zero-based column index for which the field is being created
	 *                  (accepted for interface compatibility but not used in configuration)
	 * @param precision the number of decimal places the field will display and accept
	 * @return          a fully configured RmaJDecimalField ready to be wrapped in a cell editor
	 */
	protected RmaJDecimalField createDecimalField(int col, int precision) {
		// Create the decimal field with a default value of 0 and a display width of 5 characters
		RmaJDecimalField df = new RmaJDecimalField(0, 5);

		// Set the number of decimal places the field will display and accept
		df.setPrecision(precision);

		// Right-align the field content to match standard numeric display conventions
		df.setHorizontalAlignment(SwingConstants.RIGHT);

		// Register the table as a mouse listener to intercept mouse events during cell editing
		df.addMouseListener(this);

		// Return the fully configured decimal field for installation as a cell editor
		return df;
	}

	/**
	 * Factory method that creates an AlignTableCellRenderer for the given alignment.
	 * Overridable by subclasses that need a custom alignment renderer.
	 *
	 * @param align the SwingConstants alignment value
	 * @return a new AlignTableCellRenderer configured with the given alignment
	 */
	public AlignTableCellRenderer createAlignTableCellRenderer(int align) {
		return new AlignTableCellRenderer(align);
	}

	/**
	 * Overrides getColumnClass to report Boolean for column 1 (the SELECTED_COLUMN),
	 * which ensures the table sorts and renders that column correctly. All other columns
	 * delegate to the superclass.
	 *
	 * @param column the zero-based column index
	 * @return Boolean.class for column 1; the superclass result for all other columns
	 */
	@Override
	public Class<?> getColumnClass(int column) {
		if (column == 1) {
			return Boolean.class;
		}
		return super.getColumnClass(column);
	}

	/**
	 * Overrides setTreeTableModel to install the button cell editors for the map and
	 * report columns, set fixed column widths, and expand all nodes whenever the model
	 * is replaced. The checkbox editor installation is commented out pending a fix.
	 *
	 * @param newModel the new TreeTableModel to install; must not be null
	 */
	@Override
	public void setTreeTableModel(TreeTableModel newModel) {
		super.setTreeTableModel(newModel);

		// Install "Show on Map" button editor on the DISPLAY_IN_MAPS_COLUMN
		JButton button = setButtonCellEditor(SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN);
		button.setText("Show on Map");
		button.addActionListener(e -> _parentPanel.displaySimulationInMap());

		// Install "View Report" button editor on the VIEW_REPORT_COLUMN
		button = setButtonCellEditor(SimulationTreeTableModel.VIEW_REPORT_COLUMN);
		button.setText("View Report");
		button.addActionListener(e -> _parentPanel.displayReport());

		// Apply fixed pixel widths to all four columns
		setColumnWidths(350, 150, 110, 110);

		// Expand all nodes so the full tree is visible when the model loads
		expandAll();
	}

	/**
	 * Overrides isCellEditable to delegate editability to the tree-table model's
	 * node-aware isCellEditable, passing the last path component of the row's TreePath.
	 * Returns false when the row has no associated path.
	 *
	 * @param row    the zero-based view row index
	 * @param column the zero-based column index
	 * @return true if the model reports the cell as editable; false otherwise
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		TreePath path = getPathForRow(row);
		if (path != null) {
			// Delegate to the model using the node at the end of the path for accurate editability
			return getTreeTableModel().isCellEditable(path.getLastPathComponent(), column);
		}
		return false;
	}

	/**
	 * A checkbox-based TableCellRenderer that renders boolean values as centred checkboxes.
	 * When the cell is selected, the foreground is set to the table's selection foreground
	 * and, optionally, the background to the selection background. The checkbox state is
	 * determined by parsing the cell value as a boolean string.
	 */
	protected static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
		/**
		 * When true, the selection background colour is applied to the checkbox component
		 * when the cell is selected; when false, only the foreground changes on selection.
		 */
		private boolean _useSelectionBackground;

		/**
		 * Constructs a BooleanRenderer with the given selection background behaviour.
		 * Centers the checkbox horizontally within its cell.
		 *
		 * @param useSelectionBackground true to paint the selection background when selected
		 */
		public BooleanRenderer(boolean useSelectionBackground) {
			super();
			_useSelectionBackground = useSelectionBackground;

			// Centre the checkbox icon within the cell for consistent visual alignment
			BooleanRenderer.this.setHorizontalAlignment(JLabel.CENTER);
		}

		/**
		 * Configures and returns this checkbox as the renderer component for the given cell.
		 * Sets selection colours when the cell is selected and parses the value as a boolean
		 * to determine the checked state.
		 *
		 * @param table      the JTable being rendered
		 * @param value      the cell value; parsed as a boolean string to set the checked state
		 * @param isSelected true if the cell is currently selected
		 * @param hasFocus   true if the cell currently has keyboard focus
		 * @param row        the zero-based row index of the cell
		 * @param column     the zero-based column index of the cell
		 * @return this BooleanRenderer configured for the given cell
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			setForeground(table.getForeground());

			if (isSelected) {
				setForeground(table.getSelectionForeground());

				// Only paint the selection background when the flag permits it
				if (_useSelectionBackground) {
					super.setBackground(table.getSelectionBackground());
				}
			}

			// Treat any string "true" (case-insensitive) as the checked state
			setSelected((value != null && "true".equalsIgnoreCase(value.toString())));
			return this;
		}
	}

	/**
	 * A button-based TableCellRenderer that displays a JButton in each cell. The button
	 * text is set to the string representation of the cell value. When not selected, the
	 * button foreground is set to the per-row compute-state foreground colour from the
	 * enclosing SimulationTreeTable, and the button is enabled only when the cell is editable.
	 */
	static class ButtonRenderer extends JButton implements TableCellRenderer {
		/**
		 * Constructs a ButtonRenderer with default JButton appearance.
		 */
		public ButtonRenderer() {
			super();
		}

		/**
		 * Configures and returns this button as the renderer component for the given cell.
		 * Sets the button text from the cell value, enables it based on cell editability,
		 * and applies the per-row foreground colour when the cell is not selected.
		 *
		 * @param table      the JTable being rendered; cast to SimulationTreeTable for foreground lookup
		 * @param value      the cell value whose string representation becomes the button label
		 * @param isSelected true if the cell is currently selected
		 * @param hasFocus   true if the cell currently has keyboard focus
		 * @param row        the zero-based row index of the cell
		 * @param column     the zero-based column index of the cell
		 * @return this ButtonRenderer configured for the given cell
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			boolean cellEditable = table.isCellEditable(row, column);

			if (isSelected) {
				// Selection colour application is currently commented out pending design decision
			} else {
				// Apply the per-row compute-state foreground colour from the tree-table
				setForeground(((SimulationTreeTable) table).getRowForeground(row));
			}

			// Enable the button only when the underlying cell is editable
			ButtonRenderer.this.setEnabled(cellEditable);

			// Display the cell value as the button label; use empty string for null values
			setText(value != null ? value.toString() : "");
			return this;
		}
	}

	/**
	 * A SwingX AbstractHighlighter that applies alternating background colours and
	 * per-row compute-state foreground colours to tree-table rows. Simulation (parent)
	 * rows receive _oddRowBackground; results (leaf) rows use the default table background.
	 * Selected rows always receive the standard selection colours.
	 */
	private class ReportRowHighligher extends AbstractHighlighter {
		/**
		 * Applies background and foreground colours to the given component based on
		 * whether the row represents a WatSimulation or a ResultsData entry, and
		 * whether the row is currently selected.
		 *
		 * @param component        the renderer component to colour
		 * @param componentAdapter the adapter providing row, column, and selection context
		 * @return the coloured component
		 */
		@Override
		protected Component doHighlight(Component component, ComponentAdapter componentAdapter) {
			if (!componentAdapter.isSelected()) {
				Object obj = getValueAt(componentAdapter.row, SimulationTreeTableModel.SIMULATION_COLUMN);

				if (obj instanceof WatSimulation) {
					// Simulation rows get the light blue alternating background
					component.setBackground(_oddRowBackground);
				} else {
					// Results rows use the standard table background colour
					component.setBackground(UIManager.getColor("Table.background"));
				}

				// Apply per-row foreground colour for compute-state visual coding
				Color fg = getRowForeground(componentAdapter.row);
				if (fg != null) {
					component.setForeground(fg);
				}
			} else if (componentAdapter.isSelected()) {
				// Selected rows always use the standard selection colours regardless of type
				component.setBackground(UIManager.getColor("Table.selectionBackground"));
				component.setForeground(UIManager.getColor("Table.selectionForeground"));
			}

			return component;
		}
	}

	/**
	 * Clears all per-row foreground colours from the superclass's row foreground map.
	 * Called when the simulation group changes and compute-state colours should be reset.
	 */
	public void clearColors() {
		_rowForeground.clear();
	}

	/**
	 * Searches the direct children of the tree table's root node for the
	 * SimulationTreeTableNode that wraps the given WatSimulation instance.
	 * The match is performed using reference equality rather than equals() since
	 * each simulation is a unique object in the tree. Returns null if no child
	 * node corresponds to the given simulation.
	 *
	 * @param sim the WatSimulation instance to search for among the root's children
	 * @return    the matching SimulationTreeTableNode, or null if no match is found
	 */
	public SimulationTreeTableNode getSimulationNodeFor(WatSimulation sim) {
		// Retrieve the tree table model and cast it to the expected simulation model type
		SimulationTreeTableModel model = (SimulationTreeTableModel) getTreeTableModel();

		// Retrieve the root node from which all simulation nodes descend
		SimulationTreeTableNode root = (SimulationTreeTableNode) model.getRoot();

		// Get the total number of direct children under the root node to bound the search
		int kidCount = root.getChildCount();

		// Declare a variable to hold the current child node during iteration
		TreeTableNode child;

		// Iterate over all direct children of the root to find the matching simulation node
		for (int i = 0; i < kidCount; i++) {
			// Retrieve the child node at the current index
			child = root.getChildAt(i);

			// Only inspect children that are SimulationTreeTableNode instances
			if (child instanceof SimulationTreeTableNode) {
				SimulationTreeTableNode simNode = (SimulationTreeTableNode) child;

				// Match by object identity (==) rather than equals to find the exact node
				if (simNode.getSimulation() == sim) {
					// Match found; return the corresponding tree node immediately
					return simNode;
				}
			}
		}

		// No matching node was found among the root's children; return null to signal absence
		return null;
	}

	/**
	 * Returns all WatSimulation objects whose rows are currently checked in the simulation
	 * table. The checkbox state is read from the SELECTED_COLUMN of each row; null values
	 * are skipped to handle unrendered or collapsed rows. For each checked row, the object
	 * in the SIMULATION_COLUMN is included only when it is a WatSimulation instance.
	 *
	 * @return a list of WatSimulation objects corresponding to all checked rows in the
	 *         simulation table; empty if no rows are checked
	 */
	public List<WatSimulation> getSelectedSimulations() {
		// Initialize the list that will accumulate all selected simulation objects
		List<WatSimulation> selectedSims = new ArrayList<>();

		// Get the total number of rows currently displayed in the simulation table
		int rowCnt = getRowCount();

		// Declare a variable to hold the cell value retrieved during each iteration
		Object obj;

		// Iterate over every row in the table to identify checked simulation rows
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from the SELECTED_COLUMN of the current row
			obj = getValueAt(r, SimulationTreeTableModel.SELECTED_COLUMN);

			// Skip null checkbox values (e.g. unrendered or collapsed rows)
			if (obj == null) {
				continue;
			}

			// Parse the checkbox value to a boolean and process only checked rows
			if (RMAIO.parseBoolean(obj.toString(), false)) {
				// Retrieve the object from the SIMULATION_COLUMN of the checked row
				obj = getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);

				// Only include rows where the simulation column holds a WatSimulation
				if (obj instanceof WatSimulation) {
					// Cast and add the simulation to the result list
					selectedSims.add((WatSimulation) obj);
				}
			}
		}

		// Return the list of all selected WatSimulation objects
		return selectedSims;
	}

	/**
	 * Returns all ResultsData objects whose rows are currently checked in the simulation
	 * table. The checkbox state is read from the SELECTED_COLUMN of each row; null values
	 * are skipped to handle unrendered or collapsed rows. For each checked row, the object
	 * in the SIMULATION_COLUMN is included only when it is a ResultsData instance.
	 *
	 * @return a list of ResultsData objects corresponding to all checked rows in the
	 *         simulation table; empty if no rows are checked
	 */
	public List<ResultsData> getSelectedResults() {
		// Initialize the list that will accumulate all selected results data objects
		List<ResultsData> selectedResults = new ArrayList<>();

		// Get the total number of rows currently displayed in the simulation table
		int rowCnt = getRowCount();

		// Declare a variable to hold the cell value retrieved during each iteration
		Object obj;

		// Iterate over every row in the table to identify checked results rows
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from the SELECTED_COLUMN of the current row
			obj = getValueAt(r, SimulationTreeTableModel.SELECTED_COLUMN);

			// Skip null checkbox values
			if (obj == null) {
				continue;
			}

			// Parse the checkbox value to a boolean and process only checked rows
			if (RMAIO.parseBoolean(obj.toString(), false)) {
				// Retrieve the object from the SIMULATION_COLUMN of the checked row
				obj = getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);

				// Only include rows where the simulation column holds a ResultsData object
				if (obj instanceof ResultsData) {
					// Cast and add the results data object to the result list
					selectedResults.add((ResultsData) obj);
				}
			}
		}

		// Return the list of all selected ResultsData objects
		return selectedResults;
	}
}
