
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Container;              // AWT Container used to transfer the DSS list selector's content pane
import java.awt.EventQueue;             // Swing event dispatch queue; used to defer UI updates to the EDT
import java.awt.GridBagConstraints;     // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;          // Flexible grid-based layout manager for arranging UI components
import java.awt.Point;                  // Represents a 2D point used to determine which row was double-clicked
import java.awt.Window;                 // Abstract base for top-level windows; used as the browser's parent
import java.awt.event.MouseAdapter;     // Adapter for mouse events; used to detect double-clicks on table rows
import java.awt.event.MouseEvent;       // Mouse event providing click count and cursor position information
import java.awt.event.WindowAdapter;    // Adapter for window events; monitors the DSS list selector lifecycle
import java.awt.event.WindowEvent;      // Window event used to detect when the DSS selector closes or opens
import java.util.List;                  // Ordered collection interface for data location lists
import java.util.Vector;               // Synchronized growable array for collecting plot data

import javax.swing.JButton;             // Standard Swing push-button component
import javax.swing.JOptionPane;         // Provides standard confirmation and informational dialog boxes
import javax.swing.JPanel;             // Generic lightweight container for grouping action buttons
import javax.swing.SwingUtilities;      // Swing utilities for locating the owning window of a component
import javax.swing.table.TableModel;    // Interface representing the data model for JTable components
import javax.swing.table.TableRowSorter; // Provides sorting and filtering behavior for JTable rows

import com.rma.editors.DSSListSelector;       // RMA DSS file and path browser/selector component
import com.rma.editors.DSSListSelectorParent; // Callback interface notified when the DSSListSelector is closed
import com.rma.io.DssFileManagerImpl;          // RMA concrete DSS file manager for reading time-series records
import com.rma.model.Project;                  // Represents the currently loaded RMA project and its paths

import hec.gfx2d.G2dDialog;                   // HEC dialog for 2D graphical time-series plot display
import hec.gui.AbstractEditorPanel;            // HEC base class for editor panels with fill/save lifecycle
import hec.heclib.dss.DSSPathname;             // Utility for parsing and constructing DSS path strings
import hec.heclib.dss.HecTimeSeries;           // HEC DSS reader for fetching time-series records within a time window
import hec.heclib.util.HecTime;               // HEC time representation used for DSS query time-range endpoints
import hec.io.DSSIdentifier;                   // Encapsulates a DSS file name and path for identifying a DSS record
import hec.io.TimeSeriesCollectionContainer;   // Container holding multiple time-series records from a collection read
import hec.io.TimeSeriesContainer;             // Container holding a single time-series dataset read from DSS
import hec.lang.NamedType;                     // Base interface for named model objects passed to fillPanel/savePanel
import hec.util.NumericComparator;             // Comparator for sorting table columns containing numeric values

import hec2.model.DataLocation;                // Represents a model data location (input/output point)
import hec2.model.DssDataLocation;             // DataLocation subtype that carries a DSS file reference
import hec2.plugin.model.ModelAlternative;     // Represents a model alternative configuration within a WAT simulation
import hec2.wat.client.WatMessages;            // WAT internationalization message keys for multi-row selection prompts
import hec2.wat.plugin.SimpleWatPlugin;        // Base plugin type; used to check whether a plugin is a full WatPlugin
import hec2.wat.plugin.WatPlugin;              // Full WAT plugin interface providing model-linking support
import hec2.wat.plugin.WatPluginManager;       // Registry for looking up WAT plugin instances by program name
import hec2.wat.util.WatI18n;                  // WAT internationalization utility for retrieving localized messages

import rma.swing.RmaImage;              // RMA utility for loading image icons from classpath resources
import rma.swing.RmaInsets;             // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJDialog;            // Base class for RMA dialog windows
import rma.swing.RmaJTable;             // RMA-extended table with utility row management methods
import rma.util.RMAIO;                  // RMA I/O utility providing path, string, and file helpers

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog; // Dialog that hosts this panel
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;     // Settings model holding BC DSS assignments for a model alternative

/**
 * Panel displaying and managing the Boundary Condition (BC) DSS record
 * assignments for a model alternative within the WTMP iteration settings.
 *
 * Presents a read-only table of BC data locations derived from the model
 * alternative's linked data locations. For each location the table shows an
 * index, the location name, the parameter, the model's DSS record (read-only),
 * and the iteration DSS record selected by the user.
 *
 * Users can select a DSS record via the Browse DSS button, which opens an
 * embedded DSSBrowser (a DSSListSelector wrapped in an RmaJDialog). Double-clicking
 * a row opens a BcEntryDialog for a detailed per-row view with additional
 * context. The Clear DSS Entries button removes the iteration DSS assignment
 * for all selected rows after user confirmation. The plot button overlays the
 * model and iteration time-series in a G2dDialog.
 *
 * This class also exposes public constants for each table column index so that
 * sibling classes (BcEntryDialog, PositionAnalysisBcPanel) can reference columns
 * consistently without hard-coding integer literals.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class IterationBcPanel extends AbstractEditorPanel {
	// Tab label shown when this panel is embedded in a JTabbedPane
	public static final String TAB_NAME = "Boundary Conditions";

	// Default title for the DSS browser window when no specific path is shown
	private static final String DEFAULT_BROWSER_TITLE = "Select DSS Pathname";

	// Column index for the 1-based sequential row number
	public static final int INDEX_COL = 0;

	// Column index for the DataLocation object (location name)
	public static final int DATALOCATION_COL = 1;

	// Column index for the parameter name string
	public static final int PARAMETER_COL = 2;

	// Column index for the model's DSSIdentifier (read-only reference record)
	public static final int MODEL_DSS_COL = 3;

	// Column index for the user-selected iteration DSSIdentifier
	public static final int DSSID_COL = 4;

	// Table displaying all BC rows for the current model alternative
	protected RmaJTable _bcTable;

	// Button that opens the DSS browser to assign an iteration record to selected rows
	private JButton _selectDssBtn;

	// The ModelAltIterationSettings currently loaded in this panel
	private ModelAltIterationSettings _modelAltSettings;

	// Guard flag indicating whether the DSS list selector is currently open
	private boolean _listSelectorOpened;

	// The last DSS file path the user browsed to, used to re-open the browser at the same location
	private String _lastDssFile;

	// The table row being edited when the DSS browser was opened
	private int _editingRow;

	// The currently open BcEntryDialog (row detail editor), or null if none is open
	private BcEntryDialog _bcEditor;

	// Button that clears the iteration DSS assignment for all selected rows
	private JButton _clearDssBtn;

	// Button that plots the model and iteration time-series for the selected rows
	private JButton _plotBtn;

	// Reference to the parent EditIterationSettingsDialog that hosts this panel
	protected EditIterationSettingsDialog _parent;

	// The embedded DSS browser dialog instance
	private DSSBrowser _browser;

	/**
	 * Constructs an IterationBcPanel attached to the given editor dialog.
	 *
	 * Initializes the panel with a GridBagLayout, stores the parent reference,
	 * builds all UI controls, and attaches event listeners.
	 *
	 * @param parent the EditIterationSettingsDialog that hosts this panel
	 */
	public IterationBcPanel(EditIterationSettingsDialog parent) {
		// Initialize the AbstractEditorPanel with a GridBagLayout
		super(new GridBagLayout());

		// Store a reference to the hosting dialog
		_parent = parent;

		// Build and arrange all UI components
		buildControls();

		// Attach action and selection listeners to interactive components
		addListeners();
	}


	/**
	 * Builds and lays out all UI controls within this panel.
	 *
	 * Creates the BC table with five columns (Index, Location, Parameter,
	 * Model DSS Record, Selected DSS Record), configures it as non-editable
	 * with integer sorting on the index column, and places a row of action
	 * buttons (plot, Browse DSS, Clear DSS Entries) below it.
	 */
	protected void buildControls() {
		// Define the column headers for the BC table
		String[] headers = new String[]{"Index", "Location", "Parameter", "Model DSS Record", "Selected DSS Record"};

		// Create the BC table; all cells are non-editable and setEnabled is suppressed
		// to prevent the table from visually greying out when the parent panel is disabled
		_bcTable = new RmaJTable(this, headers) {
			@Override
			public boolean isCellEditable(int row, int col) {
				// All cells are read-only; editing is performed through the BcEntryDialog
				return false;
			}

			@Override
			public void setEnabled(boolean enabled) {
				// Intentionally left empty to prevent inherited visual greying behavior
			}
		};

		// Apply integer cell rendering/editing to the index column
		_bcTable.setIntegerCellEditor(INDEX_COL);

		// Remove the built-in sum/statistics popup menu options
		_bcTable.removePopupMenuSumOptions();

		// Disable add/remove row buttons in the table toolbar
		_bcTable.setAddRemoveEnabled(false);

		// Add extra row height for readability
		_bcTable.setRowHeight(_bcTable.getRowHeight() + 5);

		// Set explicit column widths for consistent display
		_bcTable.setColumnWidths(75, 220, 135, 260, 260);

		// Clear any pre-existing rows before the panel is filled
		_bcTable.deleteCells();

		// Configure table constraints to fill all available space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_bcTable.getScrollPane(), gbc);

		// Create an empty spacer button panel to push the button row to the correct position
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(buttonPanel, gbc);

		// Create the action button row panel
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(panel, gbc);

		// Create the plot button using the standard plot icon
		_plotBtn = new JButton(RmaImage.getImageIcon("Images/plot18.gif"));
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_plotBtn, gbc);

		// Create the Browse DSS button (initially disabled until a row is selected)
		_selectDssBtn = new JButton("Browse DSS");
		_selectDssBtn.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_selectDssBtn, gbc);

		// Create the Clear DSS Entries button with a descriptive tooltip
		_clearDssBtn = new JButton("Clear DSS Entries");
		_clearDssBtn.setToolTipText("Clear the Selected DSS Record information for the selected rows");

		// Note: _selectDssBtn.setEnabled(false) here appears to be a copy-paste artifact in the original
		_selectDssBtn.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_clearDssBtn, gbc);

		// Update button enabled state based on the initial (empty) table selection
		tableRowSelected();
	}

	/**
	 * Attaches event listeners to all interactive controls.
	 *
	 * Registers listeners for: Browse DSS button, Clear DSS Entries button,
	 * table row selection changes, table double-click (opens BcEntryDialog),
	 * and the plot button.
	 */
	protected void addListeners() {
		// Open the DSS browser relative to this panel's owning window
		_selectDssBtn.addActionListener(e -> browseDSSAction(SwingUtilities.windowForComponent(this)));

		// Prompt user confirmation then clear DSS entries for selected rows
		_clearDssBtn.addActionListener(e -> clearSelectedRowsAction());

		// Enable/disable buttons and update the browser title when the row selection changes
		_bcTable.getSelectionModel().addListSelectionListener(e -> tableRowSelected());

		// Open BcEntryDialog on double-click of any non-DSSID column
		_bcTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Only respond to double-clicks
				if (e.getClickCount() != 2) {
					return;
				}
				tableDoubleClickAction(e.getPoint());
			}
		});

		// Launch the plot dialog for the selected rows
		_plotBtn.addActionListener(e -> plotRecords());
	}

	/**
	 * Plots the model and iteration time-series records for all currently
	 * selected BC table rows, opening a separate G2dDialog for each row.
	 *
	 * For each selected row, reads the original (model) time-series and, if
	 * an iteration DSS record is assigned, queries its time range and reads
	 * all members via HecTimeSeries. Both datasets are passed to a G2dDialog
	 * wrapped inside an RmaJDialog for display.
	 */
	private void plotRecords() {
		// Retrieve all currently selected row indices
		int[] rows = _bcTable.getSelectedRows();

		DataLocation dl;
		DSSIdentifier origDssId, iterDssId;
		TimeSeriesContainer origTs;

		for (int r = 0; r < rows.length; r++) {
			// Get the data location, model DSS identifier, and iteration DSS identifier for this row
			dl = (DataLocation) _bcTable.getValueAt(rows[r], DATALOCATION_COL);
			origDssId = (DSSIdentifier) _bcTable.getValueAt(rows[r], MODEL_DSS_COL);
			iterDssId = (DSSIdentifier) _bcTable.getValueAt(rows[r], DSSID_COL);

			// Resolve the model DSS file to an absolute path before reading
			origDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(origDssId.getFileName()), origDssId.getDSSPath());

			// Read the model (original) time-series record
			origTs = DssFileManagerImpl.getDssFileManager().readTS(origDssId, true);

			// Initialize the plot data vector; add the original record only if it has values
			Vector data = new Vector();
			if (origTs.numberValues > 0) {
				data.add(origTs);
			}

			// Read the iteration time-series collection if a DSS path has been assigned
			if (iterDssId.getDSSPath() != null && !iterDssId.getDSSPath().isEmpty()) {
				// Resolve the iteration DSS file to an absolute path
				DSSIdentifier dss2 = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(iterDssId.getFileName()), iterDssId.getDSSPath());

				// Query the available time range for this iteration record
				HecTime[] times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dss2, 0);

				if (times != null && times.length == 2) {
					// Open a HecTimeSeries reader scoped to the full available time window
					HecTimeSeries hecTs = new HecTimeSeries(dss2.getFileName());
					hecTs.setTimeWindow(times[0], times[1]);
					hecTs.setPathname(iterDssId.getDSSPath());

					// Read all matching members into a collection container
					TimeSeriesCollectionContainer tscc = new TimeSeriesCollectionContainer();

					if (hecTs.read(tscc, true, false) == 0) {
						TimeSeriesContainer[] tscs = tscc.get();
						if (tscs != null) {
							// Add each individual time-series member to the plot data
							for (int i = 0; i < tscs.length; i++) {
								data.add(tscs[i]);
							}
						}
					}
				}
			}

			// Create a G2dDialog and embed it inside an RmaJDialog for display
			G2dDialog g2dDlg = new G2dDialog(null, dl.getName(), false, data);
			RmaJDialog dlg = new RmaJDialog(SwingUtilities.windowForComponent(this), dl.getName(), true);
			dlg.pack();
			dlg.setSize(500, 500);

			// Transfer the menu bar and content pane from the G2dDialog wrapper
			dlg.setJMenuBar(g2dDlg.getJMenuBar());
			dlg.setContentPane(g2dDlg.getContentPane());

			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		}
	}


	/**
	 * Prompts the user for confirmation and clears the iteration DSS assignment
	 * for all currently selected BC table rows.
	 *
	 * If no rows are selected the method returns immediately. After user
	 * confirmation, delegates to clearRow() for each selected row index.
	 */
	private void clearSelectedRowsAction() {
		int[] rows = _bcTable.getSelectedRows();

		// Do nothing if no rows are selected
		if (rows == null || rows.length == 0) {
			return;
		}

		// Ask the user to confirm before clearing the DSS entries
		int opt = JOptionPane.showConfirmDialog(this, "Do you want to clear the Selected DSS Record for the selected rows?", "Comfirm", JOptionPane.YES_NO_OPTION);
		if (opt != JOptionPane.YES_OPTION) {
			return;
		}

		// Clear the iteration DSS assignment for each selected row
		for (int r = 0; r < rows.length; r++) {
			clearRow(rows[r]);
		}
	}

	/**
	 * Clears the iteration DSS file and path for the given table row by blanking
	 * both fields of the row's DSSIdentifier object and refreshing the cell.
	 *
	 * @param row the zero-based table row index whose iteration DSS record should be cleared
	 */
	public void clearRow(int row) {
		// Retrieve the existing DSSIdentifier for this row
		DSSIdentifier dssId = (DSSIdentifier) _bcTable.getValueAt(row, DSSID_COL);

		// Blank both the file name and DSS path fields
		dssId.setFileName("");
		dssId.setDSSPath("");

		// Write the modified identifier back to the table cell to trigger a repaint
		_bcTable.setValueAt(dssId, row, DSSID_COL);
	}

	/**
	 * Overrides setEnabled to defer button state updates to the EDT after the
	 * parent enabled state has been applied.
	 *
	 * @param enabled true to enable the panel; false to disable it
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		// Defer button enable/disable update to the event dispatch thread
		EventQueue.invokeLater(() -> tableRowSelected());
	}

	/**
	 * Handles double-click events on the BC table by opening a BcEntryDialog
	 * for the clicked row.
	 *
	 * Ignores clicks that land outside a valid row or directly on the DSSID
	 * column (which has its own inline editor behavior).
	 *
	 * @param point the screen point where the double-click occurred
	 */
	protected void tableDoubleClickAction(Point point) {
		// Determine which row and column the double-click landed on
		int row = _bcTable.rowAtPoint(point);
		int col = _bcTable.columnAtPoint(point);

		// Ignore clicks outside a valid row or on the DSSID column
		if (row == -1 || col == DSSID_COL) {
			return;
		}

		// Open the detail editor dialog for the double-clicked row
		_bcEditor = new BcEntryDialog(this, _bcTable, row);
		_bcEditor.setVisible(true);
	}


	/**
	 * Updates the enabled state of the Browse DSS, Clear DSS Entries, and plot
	 * buttons based on whether a row is selected and the panel is enabled.
	 *
	 * Also updates the DSS browser window title to reflect the currently selected
	 * row's model DSS path when a row is selected.
	 */
	private void tableRowSelected() {
		int row = _bcTable.getSelectedRow();
		boolean enabled = row > -1 && isEnabled();

		// Enable or disable all three action buttons together
		_selectDssBtn.setEnabled(enabled);
		_clearDssBtn.setEnabled(enabled);
		_plotBtn.setEnabled(enabled);

		// Update the browser title only when a valid row is selected
		if (enabled) {
			updateDssBrowserTitle();
		}
	}


	/**
	 * Opens the DSSBrowser dialog for selecting an iteration DSS record.
	 *
	 * Creates a new DSSBrowser anchored to the given parent window, updates
	 * its title to reflect the current selection, positions it relative to
	 * this panel, and makes it visible.
	 *
	 * @param parent the Window to use as the browser's owner
	 */
	void browseDSSAction(Window parent) {
		// Create the DSS browser dialog with the given owner window
		_browser = new DSSBrowser(parent);

		// Set the title to reflect the currently selected row's DSS path
		updateDssBrowserTitle();

		// Position the browser relative to this panel and show it
		_browser.setLocationRelativeTo(this);
		_browser.setVisible(true);
	}


	/**
	 * Updates the DSS browser window title to include the model DSS path of
	 * the currently selected BC table row.
	 *
	 * Has no effect if the browser has not yet been created or no row is selected.
	 */
	private void updateDssBrowserTitle() {
		// Do nothing if the browser has not been created yet
		if (_browser == null) {
			return;
		}

		int row = _bcTable.getSelectedRow();

		// Start with the default browser title
		String title = DEFAULT_BROWSER_TITLE;

		if (row > -1) {
			// Append the model DSS path to the title if the cell holds a DSSIdentifier
			Object obj = _bcTable.getValueAt(row, MODEL_DSS_COL);
			if (obj instanceof DSSIdentifier) {
				DSSIdentifier dssId = (DSSIdentifier) obj;
				title = title + " for " + dssId.getDSSPath();
			}
		}

		_browser.setTitle(title);
	}


	/**
	 * Returns the tab label for this panel when it is embedded in a JTabbedPane.
	 *
	 * @return the constant TAB_NAME ("Boundary Conditions")
	 */
	@Override
	public String getTabname() {
		return TAB_NAME;
	}

	/**
	 * Populates the BC table from a ModelAltIterationSettings object.
	 *
	 * Clears any existing rows, then iterates the settings' data locations,
	 * filtering to include only base DataLocation instances whose linked-to
	 * location is also a DataLocation. Each qualifying row is populated with
	 * the index, location, parameter, model DSS identifier (from the linked
	 * DssDataLocation if applicable), and the iteration DSS identifier stored
	 * in the settings (or an empty identifier if none is assigned). Installs
	 * a numeric TableRowSorter on the index column for consistent sorting.
	 *
	 * @param obj the NamedType to populate from; expected to be a ModelAltIterationSettings instance
	 */
	@Override
	public void fillPanel(NamedType obj) {
		// Remove any active row sorter before clearing to avoid index conflicts
		_bcTable.setRowSorter(null);
		_bcTable.deleteCells();

		if (obj instanceof ModelAltIterationSettings) {
			_modelAltSettings = (ModelAltIterationSettings) obj;
			List<DataLocation> dataLocs = _modelAltSettings.getDataLocations();

			Vector row;
			DataLocation dl, dl2;
			DSSIdentifier dssId;
			DataLocation linkedToDl;
			String dssFile;

			for (int i = 0; i < dataLocs.size(); i++) {
				dl = dataLocs.get(i);

				// Skip any subclasses of DataLocation (e.g., DssDataLocation); only base instances are shown
				if (!dl.getClass().equals(DataLocation.class)) {
					continue;
				}

				dl2 = dl.getLinkedToLocation();

				// Only include locations that have a valid linked-to DataLocation
				if (dl2 instanceof DataLocation) {
					linkedToDl = dl2;

					// Build the table row with five columns
					row = new Vector(5);
					row.add(i + 1);                  // 1-based index
					row.add(dl);                   // DataLocation object
					row.add(dl.getParameter());    // Parameter name string

					// Determine the model DSS file from the linked DssDataLocation if available
					if (linkedToDl instanceof DssDataLocation) {
						dssFile = ((DssDataLocation) linkedToDl).get_dssFile();
					} else {
						dssFile = "";
					}

					// Build the model DSS identifier from the linked location's file and path
					dssId = new DSSIdentifier(dssFile, linkedToDl.getDssPath());
					row.add(dssId);

					// Get the user-assigned iteration DSS identifier, defaulting to empty if not set
					dssId = _modelAltSettings.getDSSIdentifierFor(dl);
					if (dssId == null) {
						// No iteration record assigned yet; create a blank identifier as a placeholder
						dssId = new DSSIdentifier("", "");
					} else {
						// Clone the identifier to avoid modifying the settings object directly
						dssId = new DSSIdentifier(dssId);
					}
					row.add(dssId);

					_bcTable.appendRow(row);
				}
			}
		}

		// Update button enabled states after the table is populated
		tableRowSelected();

		// Install a numeric comparator on the index column for proper integer sorting
		TableModel tm = _bcTable.getModel();
		TableRowSorter<TableModel> trs = new TableRowSorter<>(tm);
		trs.setComparator(INDEX_COL, new NumericComparator(0.0));
		_bcTable.setRowSorter(trs);

		// Clear the modified flag since this is a programmatic fill
		setModified(false);
	}

	/**
	 * Saves the current BC table contents back to a ModelAltIterationSettings object.
	 *
	 * Commits any pending table edits, then iterates all rows and writes each
	 * row's DSSIdentifier (if both file name and path are non-null) to the settings.
	 *
	 * @param obj the NamedType to save into; expected to be a ModelAltIterationSettings instance
	 * @return true always (no validation failures in this implementation)
	 */
	@Override
	public boolean savePanel(NamedType obj) {
		// Commit any in-progress cell edits before reading values
		_bcTable.commitEdit(true);

		if (obj instanceof ModelAltIterationSettings) {
			ModelAltIterationSettings modelAltSettings = (ModelAltIterationSettings) obj;
			int numRows = _bcTable.getRowCount();
			DataLocation dl;
			DSSIdentifier dssId;
			String fileName, dssPath;

			for (int r = 0; r < numRows; r++) {
				// Read the data location and iteration DSS identifier for this row
				dl = (DataLocation) _bcTable.getValueAt(r, DATALOCATION_COL);
				dssId = (DSSIdentifier) _bcTable.getValueAt(r, DSSID_COL);

				if (dssId != null) {
					fileName = dssId.getFileName();
					dssPath = dssId.getDSSPath();

					// Only save when both file name and path are non-null
					if (fileName != null && dssPath != null) {
						modelAltSettings.setDssIdentifierFor(dl, dssId);
					}
				}
			}
		}

		return true;
	}

	/**
	 * Looks up the WatPlugin for the given model alternative by its program name.
	 *
	 * Returns null if the model alternative is null, if no plugin is registered
	 * for the program, or if the registered plugin is not a full WatPlugin instance.
	 *
	 * @param modelAlt the ModelAlternative whose plugin should be retrieved
	 * @return the WatPlugin for the given alternative, or null if not found
	 */
	private static WatPlugin getWatPlugin(ModelAlternative modelAlt) {
		if (modelAlt == null) {
			return null;
		}

		// Look up the plugin by the model alternative's program name
		String program = modelAlt.getProgram();
		SimpleWatPlugin plugin = WatPluginManager.getPlugin(program);

		// Return only if the plugin is a full WatPlugin (not just a SimpleWatPlugin)
		if (plugin instanceof WatPlugin) {
			return (WatPlugin) plugin;
		}

		// Log a diagnostic message if no WatPlugin was found
		System.out.println("getWatPlugin:failed to find WatPlugin for " + program);
		return null;
	}

	/**
	 * Inner DSS browser dialog that wraps a DSSListSelector within an RmaJDialog.
	 *
	 * On construction, resolves the appropriate DSS file and C-part filter from
	 * the currently selected BC row, then presents the DSSListSelector for
	 * single-path selection. When the user confirms a selection, the chosen DSS
	 * path is written back to all selected BC table rows and, if the BcEntryDialog
	 * is open, its displayed DSS identifier is updated as well.
	 *
	 * Implements DSSListSelectorParent to receive the close callback from the selector.
	 */
	class DSSBrowser extends RmaJDialog
			implements DSSListSelectorParent {
		// The embedded DSS list selector component
		DSSListSelector _listSelector;

		/**
		 * Constructs a DSSBrowser anchored to the given parent window.
		 *
		 * Builds the DSSListSelector controls and packs the dialog.
		 *
		 * @param parent the Window to use as the dialog's owner
		 */
		public DSSBrowser(java.awt.Window parent) {
			// Initialize the RmaJDialog as non-modal so the table remains accessible
			super(parent, false);
			buildControls();
			pack();
		}

		/**
		 * Builds the DSSListSelector and configures it based on the selected BC row.
		 *
		 * Determines the DSS file from the row's linked DssDataLocation (or falls
		 * back to the last browsed file or project directory). Extracts the C-part
		 * from the model DSS path and pre-populates the selector filter. Attaches
		 * a WindowListener to track the open/closed lifecycle of the selector.
		 */
		protected void buildControls() {
			// Create a single-path DSS browser selector in browse mode
			_listSelector = new DSSListSelector(this, "Select DSS Pathname", DSSListSelector.BROWSER, false, false);
			_listSelector.setPathSelectionMode(DSSListSelector.SINGLE_PATH_SELECTION);

			// Retrieve the data location from the currently selected table row
			Object dlObj = _bcTable.getValueAt(_bcTable.getSelectedRow(), DATALOCATION_COL);
			String dssFile = _lastDssFile, cPart = null;

			if (dlObj instanceof DataLocation) {
				DataLocation dl = (DataLocation) dlObj;

				// Use the location's display name as the selector title
				_listSelector.setTitle(dl.toString());

				// If the linked location is a DssDataLocation, extract its DSS file path
				if (dl.getLinkedToLocation() instanceof DssDataLocation) {
					DssDataLocation dssDl = (DssDataLocation) dl.getLinkedToLocation();
					dssFile = dssDl.get_dssFile();

					// Resolve to absolute path if the stored path is relative
					if (!RMAIO.isFullPath(dssFile)) {
						dssFile = Project.getCurrentProject().getAbsolutePath(dssFile);
					}
				}

				// Extract the C-part from the model DSS path to use as a pre-filter
				String dssPath = dl.getDssPath();
				DSSPathname dssPathname = new DSSPathname(dssPath);
				cPart = dssPathname.cPart();
			}

			// Set the DSS file in the selector, falling back to the last used file or project directory
			if (dssFile != null) {
				_listSelector.setDssFilename(dssFile);

			} else {
				if (_lastDssFile == null) {
					// No prior file; start the browser in the project directory
					_listSelector.setDirectory(Project.getCurrentProject().getProjectDirectory());
				} else {
					// Use the most recently browsed DSS file
					_listSelector.setDssFilename(_lastDssFile);
				}
			}

			// Apply the C-part filter if one was extracted from the model DSS path
			if (cPart != null) {
				_listSelector.getSelectionAndFilterPanel().setFilter("///" + cPart + "////");
			}

			// Attach a window listener to track when the selector opens and closes
			_listSelector.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					// Clear the flag when the selector window is closing
					_listSelectorOpened = false;
				}

				@Override
				public void windowOpened(WindowEvent e) {
					// Defer bringing the selector to front until after the window is fully shown
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							_listSelector.toFront();
						}
					});
				}
			});

			// Transfer the selector's content pane into this dialog
			Container contentPane = _listSelector.getContentPane();
			setContentPane(contentPane);
		}

		/**
		 * Callback invoked when the DSSListSelector is closed.
		 *
		 * If the user confirmed a selection (closePressed == true), retrieves the
		 * selected DSS pathname, converts the absolute file path to a project-relative
		 * path, and writes the resulting DSSIdentifier to all selected BC table rows.
		 * If the BcEntryDialog is currently visible, its displayed DSS identifier
		 * is also updated.
		 *
		 * When multiple rows are selected, the user is asked to confirm applying the
		 * selection to all of them.
		 *
		 * @param closePressed   true if the user confirmed the selection; false if cancelled
		 * @param theChildDialog the DSSListSelector instance being closed
		 */
		@Override
		public void dssListSelectorClosed(boolean closePressed, DSSListSelector theChildDialog) {
			if (closePressed) {
				// Mark the panel as modified since a DSS selection has been made
				setModified(true);
				_listSelectorOpened = false;

				int[] rows = _bcTable.getSelectedRows();

				// If multiple rows are selected, confirm applying the selection to all of them
				if (rows != null && rows.length > 1) {
					String msg = WatI18n.getI18n(WatMessages.MODEL_LINKING_EDITOR_MSG_MULTI_ROWS_SELECTED).getText();
					String title = WatI18n.getI18n(WatMessages.MODEL_LINKING_EDITOR_MSG_MULTI_ROWS_SELECTED_TITLE).getText();
					int opt = JOptionPane.showConfirmDialog(SwingUtilities.windowForComponent(this), msg, title, JOptionPane.YES_NO_OPTION);

					if (opt != JOptionPane.YES_OPTION) {
						// User declined multi-row apply; restrict to only the first selected row
						int row = rows[0];
						rows = new int[1];
						rows[0] = row;
					}
				} else if (rows.length < 1) {
					// No rows selected; nothing to update
					return;
				}

				List<?> pathnames = theChildDialog.getSelectedPaths();
				if (pathnames.size() > 0) {
					// Parse the selected DSS path and clear the D-part (date) component
					DSSPathname dssPathname = new DSSPathname();
					String dssPath = (String) pathnames.get(0);
					dssPathname.setPathname(dssPath);
					dssPathname.setDPart(""); // don't save the D-Part

					// Convert the absolute DSS file path to a project-relative path
					String dssFullFile = theChildDialog.getDSSFilename();
					String dssFile = RMAIO.getRelativePath(Project.getCurrentProject().getProjectDirectory(), dssFullFile);
					_lastDssFile = dssFile;

					// Write the new DSSIdentifier to each applicable table row
					for (int i = 0; i < rows.length; i++) {
						DSSIdentifier dssId = new DSSIdentifier(dssFile, dssPathname.getPathname());
						_bcTable.setValueAt(dssId, rows[i], DSSID_COL);
						setModified(true);

						// If the BcEntryDialog detail editor is open, update its displayed DSS identifier
						if (_bcEditor != null && _bcEditor.isVisible()) {
							_bcEditor.setSelectedDssId(dssId);
						}
					}
				}
			}

			// Reset the editing row tracker when the browser closes
			_editingRow = -1;
		}
	}
}
