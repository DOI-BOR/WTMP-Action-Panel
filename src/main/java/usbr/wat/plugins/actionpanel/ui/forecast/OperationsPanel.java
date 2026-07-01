package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Cursor;                                         // Provides Cursor for switching the UI to a wait cursor during slow file-load operations
import java.awt.Dimension;                                      // Provides Dimension for specifying the preferred scrollable viewport height of the info table
import java.awt.GridBagConstraints;                             // Provides GridBagConstraints for controlling component placement within a GridBagLayout

import java.io.BufferedReader;                                  // Provides BufferedReader for efficient line-by-line reading of CSV and XLSX-derived text
import java.io.FileReader;                                      // Provides FileReader for opening a local CSV file as a character stream
import java.io.IOException;                                     // Provides IOException for handling errors during file read and stream operations
import java.io.StringReader;                                    // Provides StringReader for wrapping an in-memory CSV string as a character stream
import java.nio.file.Files;                                     // Provides Files for opening an InputStream from a file path when loading XLSX workbooks
import java.nio.file.Paths;                                     // Provides Paths for constructing Path instances used when opening XLSX files
import java.text.DecimalFormat;                                 // Provides DecimalFormat for formatting numeric cell values with controlled decimal places
import java.time.LocalDateTime;                                 // Provides LocalDateTime for representing dates parsed from Excel numeric date serial values
import java.time.format.DateTimeFormatter;                      // Provides DateTimeFormatter for formatting LocalDateTime values using converted Excel patterns

import java.util.List;                                          // Provides the List interface for ordered collections of OperationsData and related types
import java.util.Vector;                                        // Provides Vector for constructing table row data passed to RmaJTable.appendRow
import java.util.logging.Level;                                 // Provides Logger for JUL-based logging of file read and conversion errors
import java.util.logging.Logger;                                // Provides Level for specifying log severity (SEVERE, CONFIG, FINE) in Logger calls
import java.util.regex.Pattern;                                 // Provides Pattern for pre-compiling regex patterns used in Excel-to-Java date format conversion
import java.util.stream.Collectors;                             // Provides Collectors for terminal stream operations such as collecting cascade dependents to a List

import javax.swing.JButton;                                     // Provides JButton for the "Import..." action button in the lower panel
import javax.swing.JOptionPane;                                 // Provides JOptionPane for displaying modal error and confirmation dialogs to the user
import javax.swing.UIManager;                                   // Provides UIManager for retrieving the current Swing look-and-feel table font

import com.rma.model.Project;                                   // Provides Project for resolving relative operations file paths to absolute paths
import com.rma.swing.excel.ExcelTable;                          // Provides ExcelTable, an RMA Swing component that renders a POI Sheet as a styled table

import hec.lang.NamedType;                                      // Provides NamedType, the HEC base class supplying name/description fields (used by related types)
import org.apache.poi.ss.usermodel.Cell;                        // Provides Cell, the POI interface for reading and writing individual spreadsheet cells
import org.apache.poi.ss.usermodel.CellStyle;                   // Provides CellStyle for applying font formatting to cells within a POI workbook
import org.apache.poi.ss.usermodel.DateUtil;                    // Provides DateUtil for detecting whether a numeric POI cell contains an Excel date serial value
import org.apache.poi.ss.usermodel.Font;                        // Provides Font for specifying typeface and size in a POI workbook cell style
import org.apache.poi.ss.usermodel.Row;                         // Provides Row, the POI interface for iterating over and creating rows within a Sheet
import org.apache.poi.ss.usermodel.Sheet;                       // Provides Sheet, the POI interface representing a single worksheet within a Workbook
import org.apache.poi.ss.usermodel.Workbook;                    // Provides Workbook, the POI interface for creating and accessing Excel workbooks
import org.apache.poi.xssf.usermodel.XSSFCell;                  // Provides XSSFCell, the OOXML-specific POI cell type used for type-safe cell value access
import org.apache.poi.xssf.usermodel.XSSFWorkbook;              // Provides XSSFWorkbook for opening and parsing .xlsx (OOXML) Excel files via Apache POI

import rma.swing.EnabledJPanel;                                 // Provides EnabledJPanel, a JPanel that propagates enable/disable state to its children
import rma.swing.RmaInsets;                                     // Provides RmaInsets constants for consistent padding values used in GridBagConstraints
import rma.swing.RmaJTable;                                     // Provides RmaJTable, an RMA-extended JTable with row management convenience methods

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                  // Provides ActionPanelPlugin for accessing the singleton plugin instance and its window
import usbr.wat.plugins.actionpanel.model.forecast.BcData;              // Provides BcData, the boundary condition data model used to detect operations data dependencies
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;         // Provides EnsembleSet for identifying ensemble sets that depend on boundary condition data
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;    // Provides ForecastSimGroup, the top-level model grouping all forecast simulation data
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;      // Provides OperationsData, the model object representing a single operations dataset and its file path

/**
 * Panel that displays and manages the Operations tab within the Forecast Action Panel.
 * It presents a table of available operations datasets loaded from the active
 * ForecastSimGroup, a detail info table showing the selected dataset's metadata,
 * and an ExcelTable for rendering the contents of the associated operations file
 * (either a .csv or a .xlsx converted to CSV on the fly).
 *
 * Users can import new operations datasets via the "Import..." button, select rows
 * to view their file contents, and delete datasets with cascading removal of dependent
 * BcData and EnsembleSet entries.
 *
 * Excel date format patterns stored in .xlsx files use a different token syntax from
 * Java's DateTimeFormatter; this class converts between the two using a set of
 * pre-compiled regex Patterns applied in sequence.
 *
 * @see AbstractForecastPanel
 * @see OperationsData
 * @see ExcelTable
 */
public class OperationsPanel extends AbstractForecastPanel<OperationsData> {
	/**
	 * JUL logger scoped to this class for file read and conversion error reporting.
	 */
	private static Logger LOGGER = Logger.getLogger(OperationsPanel.class.getName());

	/**
	 * System property key for overriding the font size scale applied when rendering the ops table.
	 */
	private static final String OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT = "WTMP.OperationsPanel.Font.ConversionScale";

	/**
	 * Default font scale percentage applied to the Swing table font when rendering CSV/XLSX content.
	 */
	private static final int DEFAULT_OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT = 60;

	/**
	 * Excel's numeric date serial origin: day 1 is January 1, 1900 in Excel's model,
	 * but Excel incorrectly treats 1900 as a leap year, so the base is set to
	 * December 31, 1899 to compensate when converting serial values to LocalDateTime.
	 */
	private static final LocalDateTime EXCEL_BASE_DATE = LocalDateTime.of(1899, 12, 31, 0, 0);

	/**
	 * Pattern matching the Excel abbreviated month token "mmm", replaced with Java's "MMM".
	 */
	private static final Pattern EXCEL_PATTERN_MMM = Pattern.compile("mmm");

	/**
	 * Pattern matching the Excel hour token "h", replaced with "HH" or "hh" depending on AM/PM presence.
	 */
	private static final Pattern EXCEL_PATTERN_H = Pattern.compile("h");

	/**
	 * Pattern matching the Excel AM/PM token, replaced with Java's "a" token.
	 */
	private static final Pattern EXCEL_PATTERN_AM_PM = Pattern.compile("AM/PM");

	/**
	 * Pattern matching the Excel abbreviated day-of-week token "ddd", replaced with Java's "EEE".
	 */
	private static final Pattern EXCEL_PATTERN_DDD = Pattern.compile("ddd");

	/**
	 * Pattern matching the Excel full day-of-week token "dddd", replaced with Java's "EEEE".
	 */
	private static final Pattern EXCEL_PATTERN_DDDD = Pattern.compile("dddd");

	/**
	 * Pattern matching a single "m" not immediately preceded or followed by another "m".
	 * Used to replace a lone month token with Java's "M" without affecting "mm", "mmm", etc.
	 */
	private static final Pattern EXCEL_PATTERN_SINGLE_M = Pattern.compile("(?<!m)m(?!m)");

	/**
	 * Pattern matching one or more "0" characters used for fractional seconds, replaced with "S".
	 */
	private static final Pattern EXCEL_PATTERN_FRACTIONAL_SECONDS = Pattern.compile("0+");

	/**
	 * Pattern matching Excel timezone tokens "Z" or "ZZZ", replaced with Java's "XXX".
	 */
	private static final Pattern EXCEL_PATTERN_TIME_ZONE = Pattern.compile("Z|ZZZ");

	/**
	 * Read-only info table showing the name, file path, and description of the selected operations dataset.
	 */
	private RmaJTable _opInfoTable;

	/**
	 * Button that opens the ImportOperationsWindow dialog for importing a new operations dataset.
	 */
	private JButton _importButton;

	/**
	 * ExcelTable component used to render the currently selected operations file as a styled grid.
	 */
	private ExcelTable _reservoirTable;

	/**
	 * Plain RmaJTable used as a placeholder and rebuilt on each panel clear or file load.
	 * Replaced by an ExcelTable once a valid operations file has been read.
	 */
	private RmaJTable _excelTable;

	/**
	 * The ForecastSimGroup currently displayed by this panel; null when no simulation is active.
	 */
	private ForecastSimGroup _fsg;

	/**
	 * Constructs a new OperationsPanel and wires it to the parent ForecastPanel.
	 *
	 * @param forecastPanel the parent ForecastPanel that owns and displays this tab
	 */
	public OperationsPanel(ForecastPanel forecastPanel) {
		super(forecastPanel);
	}

	/**
	 * Builds the lower section of the forecast panel by creating and laying out
	 * three components: a read-only metadata info table, an Import button, and
	 * a plain RmaJTable placeholder that is later replaced by an ExcelTable when
	 * an operations file is loaded.
	 *
	 * @param lowerPanel the EnabledJPanel into which the lower section components are added
	 */
	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel) {
		String[] headers = new String[]{"Operations", "File Path", "Description", "Forecast Date"};

		// Create a read-only, single-row info table for the currently selected operations dataset
		_opInfoTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Constrain the table's preferred height to exactly one row
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * 1;
				return d;
			}

			public boolean isCellEditable(int row, int col) {
				// The info table is always read-only; no cell may be edited by the user
				return false;
			}
		};

		// Configure layout: info table expands horizontally but takes minimal vertical space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_opInfoTable.getScrollPane(), gbc);

		// Create the Import button; its listener is attached in addListeners()
		_importButton = new JButton("Import...");

		// Configure layout: button sits to the right of the info table, fixed size
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_importButton, gbc);

		// Create the placeholder table that fills the bottom area before any file is loaded
		_excelTable = new RmaJTable(this, new String[]{""});

		// Configure layout: placeholder expands to fill all remaining width and height
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_excelTable.getScrollPane(), gbc);
	}

	/**
	 * Registers all event listeners for this panel. Extends the superclass listener
	 * registration with an Import button action listener that opens the import dialog.
	 */
	@Override
	protected void addListeners() {
		// Register superclass listeners (e.g. delete button, upper table listeners)
		super.addListeners();

		// Open the import dialog when the Import button is clicked
		_importButton.addActionListener(e -> importForecastData(null));
	}

	/**
	 * Opens the ImportOperationsWindow to let the user select and import an operations
	 * dataset into the current ForecastSimGroup. If a pre-constructed dialog is provided
	 * it is used directly; otherwise a new one is created. Aborts silently if the user
	 * cancels the dialog.
	 *
	 * @param dlg a pre-built ImportForecastWindow to reuse, or null to create a new one
	 */
	@Override
	protected void importForecastData(ImportForecastWindow dlg) {
		ImportOperationsWindow importOpsWindow;

		// Build a fresh import window anchored to the plugin's main actions window if none was provided
		if (dlg == null) {
			importOpsWindow = new ImportOperationsWindow(ActionPanelPlugin.getInstance().getActionsWindow());
			importOpsWindow.fillForm(_fsg);
		} else {
			// Reuse the supplied dialog, cast to the concrete operations import type
			importOpsWindow = (ImportOperationsWindow) dlg;
		}

		importOpsWindow.setVisible(true);

		// Abort if the user dismissed the dialog without confirming
		if (importOpsWindow.isCanceled()) {
			return;
		}

		// Delegate the import to the shared helper, passing the current table and existing data list
		importData(_fsg, _opsTable, importOpsWindow, _fsg.getOperationsData(), importOpsWindow.getOperationsData());
	}

	/**
	 * Returns the upper operations data table that serves as the primary selection table
	 * for this panel. Required by the AbstractForecastPanel contract.
	 *
	 * @return the ForecastTable (_opsTable) used as the operations data table
	 */
	@Override
	public ForecastTable getTableForPanel() {
		return _opsTable;
	}

	/**
	 * Persists the current panel state back to the model. Operations data is managed
	 * directly through import/delete actions, so no additional save logic is required here.
	 */
	@Override
	protected void savePanel() {
		// Operations data is managed via import/delete; no explicit save action is needed
	}

	/**
	 * Overrides setVisible to clear the panel display when it becomes visible and no
	 * row is currently selected in the operations table.
	 *
	 * @param visible true to make the panel visible; false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		// Clear stale content when the panel is shown with no active row selection
		if (visible && _opsTable.getSelectedRow() < 0) {
			clearPanel();
		}
	}

	/**
	 * Loads the panel for the given ForecastSimGroup: clears any previous state,
	 * populates the upper operations table with all datasets in the group, and
	 * displays the last dataset's file contents in the ExcelTable area.
	 *
	 * @param fsg the ForecastSimGroup whose operations data should be displayed, or null to disable
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg) {
		// Reset the lower panel before repopulating to avoid stale content
		clearPanel();

		setEnabled(fsg != null);
		_fsg = fsg;

		// Get the forecast panel object
		ForecastTable table = getTableForPanel();

		// Take action if the panel exists
		if (_fsg != null) {
			List<OperationsData> data = _fsg.getOperationsData();

			// Clear the table and repopulate it with the current group's datasets
			table.deleteCells();
			Vector<OperationsData> row;
			for (int i = 0; i < data.size(); i++) {
				// Each row holds a single OperationsData object in its first column
				row = new Vector<>();
				row.add(data.get(i));
				table.appendRow(row);
			}

			// Auto-display the last dataset's file contents when the panel loads
			if (!data.isEmpty()) {
				displayOpsData(data.get(data.size() - 1));
			}
		}
	}

	/**
	 * Resets the lower panel by removing the current ExcelTable, replacing it with a
	 * fresh empty RmaJTable placeholder, and triggering a layout refresh. Also disables
	 * the panel when no ForecastSimGroup is loaded.
	 */
	@Override
	protected void clearPanel() {
		// Remove the current excel/ops table from the layout before replacing it
		_lowerPanel.remove(_excelTable.getScrollPane());

		// Replace the old table with a fresh empty placeholder
		_excelTable = new RmaJTable(this, new String[]{""});
		_excelTable.deleteCells();

		// Disable the panel entirely when there is no active simulation group
		if (_fsg == null) {
			setEnabled(false);
		}

		// Re-add the new placeholder table with the same fill-all layout constraints
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_lowerPanel.add(_excelTable.getScrollPane(), gbc);

		// Force a layout and repaint to make the replacement visible immediately
		_lowerPanel.revalidate();
		_lowerPanel.repaint();
	}

	/**
	 * Removes the given OperationsData entry from the active ForecastSimGroup's
	 * operations data list.
	 *
	 * @param fsg  the ForecastSimGroup from which the data is removed
	 * @param data the OperationsData entry to remove
	 */
	@Override
	protected void removeData(ForecastSimGroup fsg, OperationsData data) {
		fsg.removeOperationsData(data);
	}

	/**
	 * Responds to row selection changes in the upper operations table. Populates the
	 * info table with the selected dataset's metadata, renders the associated file in
	 * the ExcelTable area, and synchronises the table row highlight. Clears the panel
	 * when the selection is cleared.
	 *
	 * @param selRow the index of the newly selected row, or -1 if the selection was cleared
	 */
	@Override
	protected void tableRowSelected(int selRow) {
		if (_fsg != null) {
			// Clear the detail info table before repopulating for the new selection
			_opInfoTable.deleteCells();

			if (selRow > -1) {
				OperationsData opsData = (OperationsData) _opsTable.getValueAt(selRow, 0);

				// Guard against null table values that may occur during rapid selection changes
				if (opsData == null) {
					return;
				}

				// Build a single info row: name, operations file path, description
				Vector row = new Vector();
				row.add(opsData.getName());
				row.add(opsData.getOperationsFile());
				row.add(opsData.getDescription());

				_opInfoTable.appendRow(row);

				// Load and render the operations file contents in the ExcelTable area
				displayOpsData(opsData);

				// Ensure the table selection highlight is kept in sync
				_opsTable.setRowSelectionInterval(selRow, selRow, false);
				_opsTable.updateSelection(selRow, 0, false, false);
			} else {
				// No row selected: reset the lower panel to the empty placeholder state
				clearPanel();
			}
		}
	}

	/**
	 * Called when the delete button is clicked for a row in the upper operations table.
	 * Delegates to the delete method after confirming that the row holds an OperationsData
	 * object and that an active simulation group is present.
	 *
	 * @param rowToDelete the index of the table row whose dataset should be deleted
	 */
	@Override
	public void tableRowDeleteClicked(int rowToDelete) {
		Object value = _opsTable.getValueAt(rowToDelete, 0);

		if (_fsg != null && value instanceof OperationsData) {
			OperationsData operationsData = (OperationsData) value;
			delete(operationsData, false);
		}
	}

	/**
	 * Displays a confirmation dialog before deleting the given OperationsData entry.
	 * Identifies all BcData records and EnsembleSet objects that depend on this entry
	 * so the confirmation message can warn the user of cascading deletions.
	 * Performs the deletion (including dependents) only if the user confirms.
	 *
	 * @param operationsData         the OperationsData entry to delete or overwrite
	 * @param deletingDueToOverwrite true if deletion is triggered by an overwrite operation;
	 *                               changes the wording of the confirmation message
	 * @return true if the deletion was confirmed and performed; false if cancelled
	 */
	@Override
	public boolean delete(OperationsData operationsData, boolean deletingDueToOverwrite) {
		boolean retVal = false;

		// Find all BcData entries that reference this operations dataset
		List<BcData> bcDataUsingOpsData = _fsg.getBcDataUsingOperationsData(operationsData);

		// Find all EnsembleSets that depend on those BcData entries (cascading dependencies)
		List<EnsembleSet> eSetsUsingBcData = bcDataUsingOpsData.stream()
				.map(bcData -> _fsg.getEnsembleSetsUsingBcData(bcData))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// Compose an appropriate confirmation message based on whether this is an overwrite
		String initialMessage;
		if (deletingDueToOverwrite) {
			initialMessage = operationsData.getName() + " already exists." + "Do you want to overwrite it?";
		} else {
			initialMessage = "Do you want to delete operations data " + operationsData.getName() + "?";
		}

		// Show the confirmation dialog; only proceed if the user confirms
		if (displayDeleteMessage(initialMessage, bcDataUsingOpsData, eSetsUsingBcData, deletingDueToOverwrite,
				operationsData)) {
			retVal = true;

			// Execute the deletion of the operations data and all cascading dependents
			performDelete(_fsg, operationsData, _opsTable, bcDataUsingOpsData, eSetsUsingBcData);
		}

		return retVal;
	}

	/**
	 * Reads and displays the operations file associated with the given OperationsData.
	 * For .csv files the file is read directly; for .xlsx files the workbook is first
	 * converted to a CSV string, then parsed into a POI Sheet. The resulting Sheet is
	 * passed to a new ExcelTable that replaces the current placeholder in the lower panel.
	 * A wait cursor is shown while the file is being processed.
	 *
	 * @param opsData the OperationsData whose associated file should be loaded and displayed
	 */
	private void displayOpsData(OperationsData opsData) {
		// Resolve the potentially relative file path to an absolute path for the current project
		String opsFilePath = Project.getCurrentProject().getAbsolutePath(opsData.getOperationsFile());
		Sheet sheet = null;

		// Abort early if no file path is associated with this operations dataset
		if (opsFilePath == null || opsFilePath.isEmpty()) {
			return;
		}

		try {
			if (opsFilePath.endsWith(".csv")) {
				// Read the CSV file directly into a POI Sheet for display
				sheet = readCsv(opsFilePath);
			} else {
				// Show a wait cursor while the potentially large XLSX is converted
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				// Convert the XLSX workbook to a CSV string, then parse it into a Sheet
				String csvString = convertXlsxToCsv(opsFilePath);
				try (BufferedReader reader = new BufferedReader(new StringReader(csvString))) {
					sheet = readIntoSheet(reader);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e, () -> "Failed to read file: " + opsFilePath);
				}
			}

			if (sheet != null) {
				// Remove the old placeholder/ExcelTable before inserting the new one
				_lowerPanel.remove(_excelTable.getScrollPane());

				// Build a new ExcelTable from the loaded Sheet and add it to the layout
				_excelTable = new ExcelTable(this, sheet);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = GridBagConstraints.RELATIVE;
				gbc.gridy = GridBagConstraints.RELATIVE;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbc.weightx = 1.0;
				gbc.weighty = 1.0;
				gbc.anchor = GridBagConstraints.NORTHWEST;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.insets = RmaInsets.INSETS5505;
				_lowerPanel.add(_excelTable.getScrollPane(), gbc);

				// Force a layout refresh to make the new ExcelTable visible immediately
				_lowerPanel.revalidate();
				_lowerPanel.repaint();
			}
		} finally {
			// Always restore the default cursor, even if an exception was thrown
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Reads a CSV file from disk into a POI Sheet. Delegates all parsing logic
	 * to readIntoSheet; logs a SEVERE error if the file cannot be read.
	 *
	 * @param opsFilePath the absolute path to the CSV file to read
	 * @return a populated POI Sheet, or null if an IOException occurs
	 */
	private Sheet readCsv(String opsFilePath) {
		Sheet retVal = null;

		// Open the CSV file and parse it into a POI Sheet; auto-close the reader on exit
		try (BufferedReader reader = new BufferedReader(new FileReader(opsFilePath))) {
			retVal = readIntoSheet(reader);

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Failed to read file: " + opsFilePath);
		}

		return retVal;
	}

	/**
	 * Reads lines from the given BufferedReader and builds a POI Sheet where each
	 * CSV row becomes a spreadsheet row and each comma-separated value becomes a cell.
	 * Cell font size is scaled down from the current Swing table font using the
	 * configured or default conversion scale percentage. A special case strips
	 * everything after "View Results:" in the first column to avoid overly long values.
	 *
	 * @param reader a BufferedReader positioned at the first line to read
	 * @return a POI Sheet populated with styled cells representing the CSV content
	 * @throws IOException if an error occurs while reading from the BufferedReader
	 */
	private Sheet readIntoSheet(BufferedReader reader) throws IOException {
		// Create a new in-memory XLSX workbook and sheet to hold the parsed CSV data
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("My Sheet");

		// Retrieve the current Swing table font to derive a consistent cell font
		Font font = workbook.createFont();
		java.awt.Font fontToUse = UIManager.getFont("Table.font");
		int fontSize = fontToUse.getSize();

		// Apply the configurable scale factor to reduce the font size for the ops table
		Integer scalePercent = Integer.getInteger(OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT, DEFAULT_OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT);
		double scale = scalePercent / 100.0;
		font.setFontName(fontToUse.getFontName());
		font.setFontHeightInPoints((short) (fontSize * scale));

		// Create a shared cell style using the scaled font for all cells in the sheet
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setFont(font);

		// Create placeholder values
		String line;
		int rowNum = 0;

		// Loop over the lines in teh files
		while ((line = reader.readLine()) != null) {
			Row row = sheet.createRow(rowNum);
			rowNum++;

			// Split on comma, preserving empty trailing fields (limit -1)
			String[] values = line.split(",", -1);
			for (int i = 0; i < values.length; i++) {
				Cell cell = row.createCell(i);
				String val = values[i];

				// Truncate the first column to "View Results:" to avoid rendering the full URL
				if (i == 0 && val.contains("View Results:")) {
					val = "View Results:";
				}

				cell.setCellValue(val);
				cell.setCellStyle(cellStyle);
			}
		}

		return sheet;
	}

	/**
	 * Converts the first sheet of an XLSX file at the given path into a CSV-formatted
	 * string. Each row in the sheet is converted to a comma-separated line, with cell
	 * values formatted based on their POI cell type. String, numeric, boolean, and formula
	 * cell types are handled; formula cells are treated as numeric since their cached
	 * results are numeric. Only cells within the defined cell range of each row are
	 * included to avoid gaps from sparse rows. If an IOException occurs during reading,
	 * the error is logged at CONFIG level and an empty string is returned.
	 *
	 * @param xlsxFilePath the absolute or relative file path to the XLSX file to convert
	 * @return             a CSV-formatted string representing the first sheet of the XLSX
	 *                     file, or an empty string if the file could not be read
	 */
	public static String convertXlsxToCsv(String xlsxFilePath) {
		// Default to an empty string; only populated if the conversion succeeds
		String retVal = "";

		try {
			// Open the XLSX workbook and access the first sheet
			Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(xlsxFilePath)));

			// Retrieve the first sheet from the workbook for conversion
			Sheet sheet = workbook.getSheetAt(0);

			// Initialize the string builder that will accumulate the CSV output
			StringBuilder csvString = new StringBuilder();

			// Iterate over every row in the sheet to build the CSV content
			for (Row row : sheet) {
				// Iterate over the defined cell range for this row (avoids gaps from sparse rows)
				for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
					// Retrieve the cell at the current column index
					Cell cell = row.getCell(i);

					// Only process cells that are XSSF instances; skip nulls or unsupported types
					if (cell instanceof XSSFCell) {
						XSSFCell xssfCell = (XSSFCell) cell;

						// Default to an empty string if the cell type is unrecognized
						String cellValue = "";

						// Format the cell value based on its declared POI cell type
						if (xssfCell.getCellType() == Cell.CELL_TYPE_STRING) {
							// Retrieve the raw string value for string-typed cells
							cellValue = xssfCell.getStringCellValue();
						} else if (xssfCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
							// Delegate numeric formatting to the shared helper method
							cellValue = handleNumericFormulaType(xssfCell);
						} else if (xssfCell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
							// Convert the boolean value to its string representation
							cellValue = String.valueOf(xssfCell.getBooleanCellValue());
						} else if (xssfCell.getCellType() == Cell.CELL_TYPE_FORMULA) {
							// Treat formula cells as numeric since their cached result is numeric
							cellValue = handleNumericFormulaType(xssfCell);
						}

						// Append the formatted cell value followed by a comma delimiter
						csvString.append(cellValue).append(",");
					}
				}

				// End each row with a newline to produce valid CSV line breaks
				csvString.append("\n");
			}

			// Store the completed CSV string for return
			retVal = csvString.toString();
		} catch (IOException e) {
			// Log the failure at CONFIG level; retVal remains empty to signal the error to the caller
			LOGGER.log(Level.CONFIG, e, () -> "Failed to convert " + xlsxFilePath + " to csv string");
		}

		// Return the CSV string, or an empty string if conversion failed
		return retVal;
	}

	/**
	 * Formats the value of a numeric or formula XSSFCell as a string. If the cell
	 * uses an Excel date format, the serial number is converted to a LocalDateTime
	 * and formatted using a Java DateTimeFormatter built by translating the cell's
	 * Excel format pattern token by token. Plain numeric values are formatted with
	 * one optional decimal place. Falls back to the raw cell value on error.
	 *
	 * @param xssfCell the XSSFCell whose numeric or formula value should be formatted
	 * @return a formatted string representation of the cell's value
	 */
	private static String handleNumericFormulaType(XSSFCell xssfCell) {
		String cellValue;

		try {
			if (DateUtil.isCellDateFormatted(xssfCell)) {
				// Retrieve the Excel format pattern stored in the cell's data format index
				short dataFormatIndex = xssfCell.getCellStyle().getDataFormat();
				String excelPattern = xssfCell.getSheet().getWorkbook().createDataFormat().getFormat(dataFormatIndex);

				// Translate Excel date format tokens to their Java DateTimeFormatter equivalents
				// "mmm" -> "MMM" (abbreviated month name)
				String javaPattern = EXCEL_PATTERN_MMM.matcher(excelPattern).replaceAll("MMM");

				// Determine whether to use 12-hour ("hh") or 24-hour ("HH") based on AM/PM presence
				String hourReplace = "hh";
				if (!excelPattern.contains(EXCEL_PATTERN_AM_PM.pattern())) {
					hourReplace = "HH";
				}

				// "h" -> "hh" or "HH" (hour of day)
				javaPattern = EXCEL_PATTERN_H.matcher(javaPattern).replaceAll(hourReplace);

				// "AM/PM" -> "a" (AM/PM marker)
				javaPattern = EXCEL_PATTERN_AM_PM.matcher(javaPattern).replaceAll("a");

				// "ddd" -> "EEE" (abbreviated day-of-week name)
				javaPattern = EXCEL_PATTERN_DDD.matcher(javaPattern).replaceAll("EEE");

				// "dddd" -> "EEEE" (full day-of-week name)
				javaPattern = EXCEL_PATTERN_DDDD.matcher(javaPattern).replaceAll("EEEE");

				// Lone "m" -> "M" (month number, not minute)
				javaPattern = EXCEL_PATTERN_SINGLE_M.matcher(javaPattern).replaceAll("M");

				// Fractional seconds "0+" -> "S"
				javaPattern = EXCEL_PATTERN_FRACTIONAL_SECONDS.matcher(javaPattern).replaceAll("S");

				// Timezone "Z" or "ZZZ" -> "XXX" (ISO 8601 offset)
				javaPattern = EXCEL_PATTERN_TIME_ZONE.matcher(javaPattern).replaceAll("XXX");

				// Build the formatter and convert the Excel serial number to a LocalDateTime
				DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(javaPattern);
				LocalDateTime date = convertNumericToDate(xssfCell.getNumericCellValue());
				cellValue = dateFormat.format(date);

			} else {
				// Format plain numeric values with at most one decimal place
				double numeric = xssfCell.getNumericCellValue();
				DecimalFormat decimalFormat = new DecimalFormat("#.#");
				cellValue = decimalFormat.format(numeric);
			}

		} catch (IllegalStateException e) {
			// Fall back to the raw string value when type coercion fails
			cellValue = xssfCell.getRawValue();
			LOGGER.log(Level.FINE, e, () -> "Using raw value of " + xssfCell.getRawValue() + " for cell");
		}

		return cellValue;
	}

	/**
	 * Converts an Excel numeric date serial value to a LocalDateTime. Excel serial
	 * day 1 corresponds to January 1, 1900; however, Excel incorrectly counts 1900 as a
	 * leap year, so subtracting 1 from the serial before adding days to the base date
	 * compensates for this off-by-one offset.
	 *
	 * @param numericDateValue the Excel date serial number to convert
	 * @return the corresponding LocalDateTime at midnight
	 */
	private static LocalDateTime convertNumericToDate(double numericDateValue) {
		// Subtract 1 to account for Excel's erroneous leap-year treatment of 1900
		return EXCEL_BASE_DATE.plusDays((long) numericDateValue - 1);
	}
}
