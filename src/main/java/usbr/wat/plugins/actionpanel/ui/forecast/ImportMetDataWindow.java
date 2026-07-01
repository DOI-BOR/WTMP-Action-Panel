package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Dimension;                                          // Provides Dimension for constraining the preferred size of the error display editor pane
import java.awt.EventQueue;                                         // Provides EventQueue for scheduling error dialogs on the AWT Event Dispatch Thread
import java.awt.GridBagConstraints;                                 // Provides GridBagConstraints for specifying layout parameters within the GridBagLayout
import java.awt.GridBagLayout;                                      // Provides GridBagLayout as the layout manager for the dialog content pane
import java.awt.Point;                                              // Provides Point for capturing the mouse cursor position when computing tooltip row/column
import java.awt.Window;                                             // Provides Window as the parent component type accepted by the superclass constructor
import java.awt.event.ActionEvent;                                  // Provides ActionEvent for the ButtonCmdPanelListener and AbstractAction callbacks
import java.awt.event.ItemEvent;                                    // Provides ItemEvent for detecting when the data source combo box selection changes
import java.awt.event.MouseEvent;                                   // Provides MouseEvent for obtaining cursor coordinates used in tooltip row/column lookup

import java.io.BufferedReader;                                      // Provides BufferedReader for reading the met config file line by line
import java.io.IOException;                                         // Provides IOException for catching file read errors in the config and CSV reading methods

import java.util.ArrayList;                                         // Provides ArrayList for building lists of met data sets, config file entries, and selected years
import java.util.HashMap;                                           // Provides HashMap for caching MetTableModel instances keyed by config file path
import java.util.List;                                              // Provides the List interface for ordered collections of years, identifiers, and data sets
import java.util.Map;                                               // Provides the Map interface for keyed lookups of table models, averages, and selection state
import java.util.Set;                                               // Provides Set; retained for potential use with map entry sets
import java.util.Vector;                                            // Provides Vector as the row data container required by RmaJTable's row append methods

import javax.swing.AbstractAction;                                  // Provides AbstractAction as the base class for the "Copy to Clipboard" and "Select All" popup actions
import javax.swing.Action;                                          // Provides the Action interface implemented by the popup menu actions in the error display
import javax.swing.JEditorPane;                                     // Provides JEditorPane for rendering HTML-formatted error content in the error display dialog
import javax.swing.JLabel;                                          // Provides JLabel for the "Meteorology Name:", "Description:", and "Data Source:" field labels
import javax.swing.JOptionPane;                                     // Provides JOptionPane for validation error messages, error confirmation, and error detail dialogs
import javax.swing.JPopupMenu;                                      // Provides JPopupMenu for the right-click context menu on the error display editor pane
import javax.swing.JScrollPane;                                     // Provides JScrollPane for wrapping the error editor pane with always-on horizontal scrolling
import javax.swing.JTextArea;                                       // Provides JTextArea; retained as a common Swing text component import
import javax.swing.ScrollPaneConstants;                             // Provides ScrollPaneConstants for the HORIZONTAL_SCROLLBAR_ALWAYS policy on the error scroll pane
import javax.swing.table.JTableHeader;                              // Provides JTableHeader as the base type checked when applying tooltip strings to the table header
import javax.swing.table.TableModel;                                // Provides TableModel as the type parameter for the TableRowSorter applied to the met table
import javax.swing.table.TableRowSorter;                            // Provides TableRowSorter for enabling column-based numeric sorting on the average temperature table

import com.rma.io.FileManagerImpl;                                  // Provides FileManagerImpl for resolving and opening project-relative config files
import com.rma.io.RmaFile;                                          // Provides RmaFile as the file abstraction returned by the file manager
import com.rma.model.Project;                                       // Provides Project for resolving relative file paths and obtaining the project's unit system

import hec.data.ParamDouble;                                        // Provides ParamDouble for formatting and displaying temperature values with correct units and precision
import hec.data.Parameter;                                          // Provides Parameter for accessing the temperature parameter ID and unit system constants
import hec.io.DSSIdentifier;                                        // Provides DSSIdentifier for pairing a DSS file name with a DSS record pathname
import hec.io.Identifier;                                           // Provides Identifier as the base type used for data source combo box entries
import hec.util.NumericComparator;                                  // Provides NumericComparator for numerically sorting the Year and average temperature columns

import rma.swing.ButtonCmdPanel;                                    // Provides ButtonCmdPanel for the OK/Cancel button row at the bottom of the dialog
import rma.swing.ButtonCmdPanelListener;                            // Provides ButtonCmdPanelListener for handling OK and Cancel button click events
import rma.swing.RmaInsets;                                         // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJComboBox;                                      // Provides RmaJComboBox for the data source (met config file) selection combo box
import rma.swing.RmaJDescriptionField;                              // Provides RmaJDescriptionField for the multi-line description text field
import rma.swing.RmaJTable;                                         // Provides RmaJTable as the base Swing table class for the meteorology data year table
import rma.swing.RmaJTextField;                                     // Provides RmaJTextField for the meteorology name text field
import rma.swing.list.RmaListModel;                                 // Provides RmaListModel as the typed list model used to populate the data source combo box
import rma.swing.table.RmaTableModel;                               // Provides RmaTableModel as the base class for the inner MetTableModel
import rma.swing.table.ToolTipHeader;                               // Provides ToolTipHeader for setting per-column tooltip strings on the met table header
import rma.util.RMAConst;                                           // Provides RMAConst for the undefined double sentinel used when an average value is missing
import rma.util.RMAFilenameFilter;                                  // Provides RMAFilenameFilter for listing only ".config" files in the met config files folder
import rma.util.RMAIO;                                              // Provides RMAIO for path utilities: concatenating paths, checking absolute paths, and stripping extensions

import usbr.wat.plugins.actionpanel.io.forecast.MetConfigFileReader;    // Provides MetConfigFileReader for parsing the selected met config file and its DSS source entries
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles; // Provides ForecastConfigFiles for resolving the project-relative paths to config file folders
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;    // Provides ForecastSimGroup as the simulation group passed to fillForm (not currently used directly)
import usbr.wat.plugins.actionpanel.model.forecast.MetDataDssValidator; // Provides MetDataDssValidator for reading DSS records and computing per-year seasonal air temperature averages
import usbr.wat.plugins.actionpanel.model.forecast.MetDataType;         // Provides MetDataType for classifying the type of meteorology data being imported
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;     // Provides MeteorlogicData as the output data type constructed for each selected year

/**
 * A modal import dialog that allows the user to select a meteorology configuration
 * file, review per-year seasonal average air temperature data read from the associated
 * HEC-DSS records, and import one or more year-specific {@link MeteorlogicData} items
 * into the WTMP forecast action panel.
 *
 * The dialog presents:
 *
 *   A name field and a description field for the meteorology data set.
 *   A data source combo box that lists all {@code .config} files found in the
 *       project's met config files folder. Selecting a file reads and validates its
 *       referenced DSS records via {@link MetDataDssValidator}, then displays per-year
 *       spring, summer, and fall average air temperatures in the met table.
 *   A checkbox-based met data table showing available years and their seasonal
 *       averages. Rows with missing DSS data are non-selectable and display a tooltip
 *       listing the missing records.
 *   OK and Cancel buttons; OK validates the form and produces
 *       {@link MeteorlogicData} instances via {@link #getMetData()}.
 *
 * {@link MetTableModel} instances are cached per config file path in {@code _metTables}
 * to avoid re-reading DSS data when the user switches between config files.
 *
 * @see MeteorlogicData
 * @see MetTableModel
 * @see MetDataDssValidator
 * @see ImportForecastWindow
 */
public class ImportMetDataWindow extends ImportForecastWindow {
	// Project-relative path to the yearly average temperature CSV data file
	private static final String AVE_TEMP_FILE = ForecastConfigFiles.getRelativeYearlyTempDataFile();

	// Text field for the user-supplied name of the meteorology data set
	private RmaJTextField _nameFld;

	// Multi-line text field for an optional description of the meteorology data set
	private RmaJDescriptionField _descFld;

	// Combo box listing all available met config files; selection triggers a DSS data read
	private RmaJComboBox<Identifier> _importTypeCombo;

	// The main table displaying available years and their seasonal average air temperatures
	private RmaJTable _metTable;

	// OK/Cancel button row at the bottom of the dialog
	private ButtonCmdPanel _cmdPanel;

	// Cache of MetTableModel instances keyed by config file path; avoids redundant DSS reads
	private Map<String, MetTableModel> _metTables = new HashMap<>();

	/**
	 * Constructs an {@code ImportMetDataWindow} modal dialog, builds all controls,
	 * wires listeners, packs the dialog to its preferred size, and centres it over
	 * the parent window.
	 *
	 * Note: the {@code fillAveTempData()} call is commented out; average temperature
	 * data is now loaded on demand when the user selects a config file from the combo.
	 *
	 * @param parent the {@link Window} over which this dialog is centred and to which
	 *               it is modal; passed to the {@link ImportForecastWindow} superclass
	 */
	public ImportMetDataWindow(Window parent) {
		// Initialise the superclass as a modal dialog titled "Import Met Data"
		super(parent, "Import Met Data", true);

		// Build and lay out all Swing controls
		buildControls();

		// Attach combo box, button, and table listeners
		addListeners();

		// Size the dialog to its preferred dimensions and centre it over the parent
		pack();
		setLocationRelativeTo(getParent());

		// fillAveTempData() is disabled; data loads on combo selection change instead
	}

	/**
	 * Builds and lays out all Swing controls within the dialog's content pane.
	 *
	 * The layout consists, top to bottom, of:
	 *
	 *   A "Meteorology Name:" label and name text field.
	 *   A "Description:" label and description text field.
	 *   A "Data Source:" label and config file combo box.
	 *   A checkbox-based met data table with custom tooltip support for rows with missing DSS records.
	 *   An OK/Cancel {@link ButtonCmdPanel}.
	 *
	 */
	private void buildControls() {
		// Use GridBagLayout for flexible component placement in the content pane
		getContentPane().setLayout(new GridBagLayout());

		// --- Meteorology Name row ---
		JLabel label = new JLabel("Meteorology Name:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// The name field stretches to fill the remaining row width
		_nameFld = new RmaJTextField();
		label.setLabelFor(_nameFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_nameFld, gbc);

		// --- Description row ---
		label = new JLabel("Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// The description field stretches to fill the remaining row width
		_descFld = new RmaJDescriptionField();
		label.setLabelFor(_descFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_descFld, gbc);

		// --- Data Source row ---
		label = new JLabel("Data Source:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// The config file combo box takes half the remaining row width
		_importTypeCombo = new RmaJComboBox<>();
		label.setLabelFor(_importTypeCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_importTypeCombo, gbc);

		// --- Met data table ---
		// Column headers use "|" as a line-break separator for multi-line rendering
		String[] headers = new String[]{"Selected", "Year", "Spring Ave|Air Temp",
				"Summer Ave|Air Temp", "Fall Ave|Air Temp"};

		// Override getToolTipText to show missing DSS record details for invalid rows
		_metTable = new RmaJTable(this, headers) {
			@Override
			public String getToolTipText(MouseEvent e) {
				TableModel model = getModel();

				// Only provide custom tooltips when the model is a MetTableModel
				if (!(model instanceof MetTableModel)) {
					return super.getToolTipText(e);
				}

				Point p = e.getPoint();
				int row = rowAtPoint(p);

				// No tooltip if the mouse is not over a valid row
				if (row == -1) {
					return super.getToolTipText(e);
				}

				int col = columnAtPoint(p);

				// No tooltip if the mouse is not over a valid column
				if (col == -1) {
					return super.getToolTipText(e);
				}

				MetTableModel metModel = (MetTableModel) model;

				// Only show the missing-records tooltip for non-selectable (invalid) rows
				if (!metModel.isCellEditable(row, 0)) {
					List<String> missingRecords = metModel.getMissingRecordsForRow(row);

					if (missingRecords != null && !missingRecords.isEmpty()) {
						// Build an HTML tooltip listing each missing DSS record for this year
						StringBuilder sb = new StringBuilder();
						sb.append("<html>The Following DSS Records are missing data for the year ");
						sb.append(getValueAt(row, 1));
						sb.append("<br>");

						for (int i = 0; i < missingRecords.size(); i++) {
							sb.append(missingRecords.get(i));
							sb.append("<br>");
						}

						return sb.toString();
					}
				}

				return super.getToolTipText(e);
			}
		};

		// Install checkbox and double cell editors and configure the multi-line header renderer
		setTableEditors();

		// Clear any default placeholder rows and increase row height for readability
		_metTable.deleteCells();
		_metTable.setRowHeight(_metTable.getRowHeight() + 5);

		// Remove sum popup options that are not applicable to this selection table
		_metTable.removePopupMenuSumOptions();

		// Add the met table scroll pane; it claims all remaining vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_metTable.getScrollPane(), gbc);

		// Add the OK/Cancel command panel anchored to the bottom-left of its cell
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
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
	 * Installs the checkbox cell editor on column 0, double cell editors on all
	 * remaining columns, and configures the multi-line column header renderer to
	 * split column labels at the {@code "|"} separator character.
	 */
	private void setTableEditors() {
		// Column 0 uses a checkbox editor for the selection state
		_metTable.setCheckBoxCellEditor(0);

		// All remaining columns use a double editor for the average temperature values
		int cols = _metTable.getColumnCount();
		for (int i = 1; i < cols; i++) {
			_metTable.setDoubleCellEditor(i);
		}

		// Configure the header renderer to split column labels at "|" for multi-line display
		_metTable.setMlHeaderRenderer().setSeparatorToken("|");
	}

	/**
	 * Reads and returns the column headers for the met table from the first line of the
	 * average temperature CSV config file.
	 *
	 * If the config file cannot be found or read, a default two-column header array
	 * ({@code ["Selected", "Year"]}) is returned. The first column in the file header
	 * is replaced with {@code "Select"} and the remaining columns are shifted by one
	 * position to make room for the added selection column.
	 *
	 * @return the column header string array derived from the config file, or the
	 *         default {@code ["Selected", "Year"]} if the file is unavailable
	 */
	private String[] getHeadersFromConfigFile() {
		String configFile = getConfigFileName();

		// Attempt to open the average temperature config file via the file manager
		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);

		String[] defaultHeader = new String[]{"Selected", "Year"};

		// Return the default header if the file cannot be located
		if (file == null) {
			return defaultHeader;
		}

		BufferedReader reader = file.getBufferedReader();

		// Return the default header if the file cannot be opened for reading
		if (reader == null) {
			return defaultHeader;
		}

		String headerLine;
		try {
			headerLine = reader.readLine();

			if (headerLine != null) {
				// Split the header line by comma and prepend the "Select" checkbox column
				String[] fileHeaders = headerLine.split(",");
				String[] headers = new String[fileHeaders.length + 1];
				headers[0] = "Select";
				System.arraycopy(fileHeaders, 0, headers, 1, fileHeaders.length);
				return headers;
			}

		} catch (IOException e) {
			// TODO: replace with proper logging
			e.printStackTrace();

		} finally {
			try {
				// Always close the reader to release the file handle
				reader.close();

			} catch (IOException e) {
				// Swallow close exception; nothing meaningful can be done at this point
			}
		}

		return defaultHeader;
	}

	/**
	 * Returns the absolute file-system path to the yearly average temperature CSV data
	 * file by concatenating the current project directory with the relative path
	 * constant {@link #AVE_TEMP_FILE}.
	 *
	 * @return the absolute path string to the yearly average temperature config file
	 */
	private String getConfigFileName() {
		String prjDir = Project.getCurrentProject().getProjectDirectory();

		// Build the absolute path by joining the project directory and the relative file name
		String configFile = RMAIO.concatPath(prjDir, AVE_TEMP_FILE);

		return configFile;
	}

	/**
	 * Loads average temperature data from the configuration CSV file into the meteorology
	 * table. The first line of the file is treated as a header and skipped; subsequent
	 * lines are parsed as comma-separated values and appended as rows. Each row is
	 * prepended with an unchecked Boolean checkbox value in column 0. After loading,
	 * a TableRowSorter with NumericComparator is applied to all columns so that sorting
	 * treats cell values as numbers rather than strings. If the config file or its reader
	 * cannot be obtained, the method returns without modifying the table.
	 */
	private void fillAveTempData() {
		// Clear any previously displayed rows before reloading
		_metTable.deleteCells();

		// Retrieve the path to the average temperature configuration CSV file
		String configFile = getConfigFileName();

		// Attempt to open the average temperature data file via the file manager
		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);

		// If the file cannot be found or opened, exit without modifying the table
		if (file == null) {
			return;
		}

		// Obtain a buffered reader for line-by-line parsing of the CSV file
		BufferedReader reader = file.getBufferedReader();

		// If no reader is available (e.g. the file is unreadable), exit without modifying the table
		if (reader == null) {
			return;
		}

		String headerLine;
		try {
			// Declare a vector to hold the cell values for each parsed row
			Vector line = null;

			// Skip the first header line; data rows begin on line 2
			reader.readLine();

			// Read and process each remaining line until the end of the file
			while ((headerLine = reader.readLine()) != null) {
				line = new Vector();

				// Split the CSV line into individual string values
				String[] values = headerLine.split(",");

				// Prepend an unchecked checkbox value before the CSV data values
				line.add(Boolean.FALSE);

				// Append each CSV value into the row vector in its original column order
				for (int i = 0; i < values.length; i++) {
					line.add(values[i]);
				}

				// Append the fully populated row to the meteorology table
				_metTable.appendRow(line);
			}
		} catch (IOException e) {
			// Log the stack trace for any IO error encountered during file reading
			e.printStackTrace();
		} finally {
			try {
				// Always close the reader to release the file handle
				reader.close();
			} catch (IOException e) {
				// Swallow close exception; nothing meaningful can be done at this point
			}
		}

		// Apply numeric comparators to all columns so sorting treats values as numbers
		TableRowSorter sorter = new TableRowSorter(_metTable.getModel());

		// Get the total number of columns to apply a comparator to each one
		int cols = _metTable.getColumnCount();

		// Assign a NumericComparator to every column in the sorter
		for (int c = 0; c < cols; c++) {
			sorter.setComparator(c, new NumericComparator());
		}

		// Attach the configured sorter to the meteorology table to enable numeric sorting
		_metTable.setRowSorter(sorter);
	}

	/**
	 * Registers all event listeners required by this dialog. Attaches an item listener
	 * to the import type combo box that reloads the meteorology table when the selection
	 * changes, and attaches a ButtonCmdPanelListener to the command panel that handles
	 * OK and Cancel button activations. The OK button validates the form before saving
	 * and closing, while the Cancel button closes the dialog immediately without saving.
	 */
	private void addListeners() {
		// Reload the met table model whenever the selected config file changes
		_importTypeCombo.addItemListener(e -> metComboChanged(e));

		// Register a command panel listener to handle OK and Cancel button activations
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				// Determine which button was activated and respond accordingly
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate before saving; block submission if validation fails
						if (isValidForm()) {
							// Save the form data, hide the dialog, and mark it as not canceled
							saveForm();
							setVisible(false);
							_canceled = false;
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Close without saving and mark as cancelled
						setVisible(false);
						_canceled = true;
						break;
				}
			}
		});
	}

	/**
	 * Handles a selection change in the data source combo box.
	 *
	 * Ignores {@code DESELECTED} events to avoid processing the outgoing selection.
	 * On {@code SELECTED}, looks up the cached {@link MetTableModel} for the newly
	 * selected config file path, or reads and validates the config file to build a
	 * new model if none is cached. The new model is then applied to the met table,
	 * the combo box tooltip is updated with the config file path and DSS record, and
	 * the table header tooltips are set to describe each column's date range.
	 *
	 * @param e the {@link ItemEvent} fired by the combo box selection change
	 */
	private void metComboChanged(ItemEvent e) {
		// Skip the DESELECTED event; only process the newly selected item
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();

		// Look up the cached model for this config file path to avoid re-reading DSS data
		MetTableModel table = _metTables.get(id.getPath());

		if (table == null) {
			// No cached model; read and validate the config file to build a new model
			table = readMetConfigFile();
			_metTables.put(id.getPath(), table);
		}

		// Apply the model to the met table
		_metTable.setModel(table);

		// Update the combo box tooltip to show the config file path and its DSS source record
		_importTypeCombo.setToolTipText("<html><b>Config File:</b> " + id.getPath() +
				"<br><b>Temperature Values from:</b> " + table.getDssRecord());

		// Set per-column tooltips on the table header to describe each season's date range
		JTableHeader tableHeader = _metTable.getTableHeader();
		if (tableHeader instanceof ToolTipHeader) {
			ToolTipHeader ttheader = (ToolTipHeader) tableHeader;
			ttheader.setToolTipStrings(new String[]{
					"Select the Year", "The Year",
					"Mar, Apr, May",
					"Jun, Jul, Aug",
					"Sept, Oct, Nov"
			});
		}

		// Re-apply cell editors after the model swap, since setModel resets them
		setTableEditors();
	}

	/**
	 * Reads and validates the met config file selected in the data source combo box,
	 * constructing a {@link MetTableModel} populated with per-year seasonal average
	 * air temperatures from the referenced DSS records.
	 *
	 * If validation produces errors, a confirmation dialog is displayed on the EDT
	 * offering to show the details. If the config file cannot be read, an empty
	 * {@link MetTableModel} is returned.
	 *
	 * @return a populated {@link MetTableModel} if the config file is read and
	 *         validated successfully; an empty model otherwise
	 */
	private MetTableModel readMetConfigFile() {
		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();

		// Read the DSS path mappings from the selected met config file
		MetConfigFileReader reader = new MetConfigFileReader(id.getPath());

		if (reader.readDssPathsFile()) {
			// Retrieve the list of source DSS identifiers defined in the config file
			List<DSSIdentifier> srcDssEntries = reader.getSourceDssIdentifiers();

			// Convert any relative DSS file paths to absolute paths for the current project
			fixupDssFilePaths(srcDssEntries);

			// Validate the DSS records and compute per-year seasonal averages
			MetDataDssValidator validator = new MetDataDssValidator(srcDssEntries);
			List<Integer> years = validator.getYears();
			Map<Integer, List<String>> invalidYears = validator.getInvalidYears();
			Map<Integer, Double> springAverages = validator.getSpringAverages();
			Map<Integer, Double> summerAverages = validator.getSummerAverages();
			Map<Integer, Double> fallAverages = validator.getFallAverages();
			String units = validator.getDssUnits();

			List<String> errors = validator.getErrors();

			if (!errors.isEmpty()) {
				// Schedule the error dialog on the EDT to avoid blocking the combo listener
				EventQueue.invokeLater(() -> displayErrors(id, errors));
			}

			// Build the table model with the validated data and set its DSS source record
			MetTableModel newModel = new MetTableModel(years, invalidYears, springAverages,
					summerAverages, fallAverages, units);
			newModel.setDssRecord(srcDssEntries.get(0));
			return newModel;
		}

		// Return an empty model if the config file could not be read
		return new MetTableModel(null, null, null, null, null, "");
	}

	/**
	 * Prompts the user to confirm whether they want to view processing errors, and if so,
	 * displays a scrollable HTML error report in a message dialog. The report lists the
	 * config file path from the given Identifier and all error messages from the provided
	 * list. A right-click popup menu on the editor pane offers Copy to Clipboard and
	 * Select All actions for convenient error extraction. If the user declines to view
	 * the errors, the method returns without displaying the report.
	 *
	 * @param id     the Identifier whose file path is included in the error report header
	 * @param errors the list of error message strings to display in the report body
	 */
	private void displayErrors(Identifier id, List<String> errors) {
		// Ask the user whether they want to see the error details before building the UI
		int opt = JOptionPane.showConfirmDialog(this,
				"<html>There were errors with processing the Time Series Records specified in the config file.<br>Do you want to see the errors?",
				"Errors Occurred", JOptionPane.YES_NO_OPTION);

		// Only build and display the error report if the user clicked Yes
		if (JOptionPane.YES_OPTION == opt) {
			// Build an HTML-capable editor pane with a fixed preferred scroll viewport size
			final JEditorPane txtArea = new JEditorPane() {
				public Dimension getPreferredScrollableViewportSize() {
					// Override the default viewport size to provide a fixed 400x300 display area
					Dimension d = super.getPreferredScrollableViewportSize();
					d.width = 400;
					d.height = 300;
					return d;
				}
			};

			// Build the right-click popup menu with Copy and Select All actions
			JPopupMenu popup = new JPopupMenu();

			// Create a "Copy to Clipboard" action that copies the selected editor pane text
			Action action = new AbstractAction("Copy to Clipboard") {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Copy the currently selected text to the system clipboard
					txtArea.copy();
				}
			};
			popup.add(action);

			// Create a "Select All" action that highlights all text in the editor pane
			action = new AbstractAction("Select All") {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Select all text in the editor pane for bulk copying
					txtArea.selectAll();
				}
			};
			popup.add(action);

			// Attach the popup menu to the editor pane so it appears on right-click
			txtArea.setComponentPopupMenu(popup);

			// Build the HTML error report listing the config file path and all error messages
			StringBuffer sb = new StringBuffer();
			sb.append("<html><b>The Following Errors Occurred during the Processing of the Time Series Records");

			// Append the config file path as a subheading in the error report
			sb.append("<br>File:</b>" + id.getPath());
			sb.append("<br><b>Import Issues:</b>");

			// Append each error message on its own line in the HTML report
			for (int e = 0; e < errors.size(); e++) {
				sb.append("<br>");
				sb.append(errors.get(e));
			}
			sb.append("</html>");

			// Render the error report as HTML inside the editor pane
			txtArea.setContentType("text/html");
			txtArea.setText(sb.toString());

			// Wrap the editor pane in a scroll pane with always-visible horizontal scrolling
			JScrollPane sp = new JScrollPane(txtArea);
			sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

			// Scroll to the top of the error report before displaying it to the user
			txtArea.setCaretPosition(0);

			// Display the scrollable error report in a modal information dialog
			JOptionPane.showMessageDialog(null, sp, "Import Issues", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Resolves any relative DSS file paths in the provided list of DSSIdentifiers to
	 * absolute paths within the current project directory. Identifiers that already
	 * contain an absolute path are left unchanged. This ensures that all DSS file
	 * references are fully qualified before the entries are used for data access.
	 *
	 * @param srcDssEntries the list of DSSIdentifier objects whose file paths will be
	 *                      checked and resolved to absolute paths where necessary
	 */
	private void fixupDssFilePaths(List<DSSIdentifier> srcDssEntries) {
		// Declare variables to hold the current DSS identifier and its resolved absolute path
		DSSIdentifier dssId;
		String fullPath;

		// Iterate over every DSS identifier in the list to check and fix its file path
		for (int i = 0; i < srcDssEntries.size(); i++) {
			// Retrieve the DSS identifier at the current index
			dssId = srcDssEntries.get(i);

			// Only resolve paths that are not already fully qualified absolute paths
			if (!RMAIO.isFullPath(dssId.getFileName())) {
				// Resolve the relative path to an absolute path within the current project
				fullPath = Project.getCurrentProject().getAbsolutePath(dssId.getFileName());

				// Update the DSS identifier with the resolved absolute file path
				dssId.setFileName(fullPath);
			}
		}
	}

	/**
	 * Saves the current form state.
	 *
	 * Currently a stub pending full implementation; logs a message to standard output.
	 */
	protected void saveForm() {
		// TODO: implement form save logic
		System.out.println("saveForm TODO implement me");
	}

	/**
	 * Validates that the form can be submitted.
	 *
	 * Checks that:
	 *
	 *   The name field is not empty.
	 *   A data source (config file) has been selected from the combo box.
	 *   At least one year row is checked in the met table.
	 *
	 * A plain-message {@link JOptionPane} dialog is shown for each failed check.
	 *
	 * @return {@code true} if all validation checks pass; {@code false} otherwise
	 */
	protected boolean isValidForm() {
		// Require a non-empty name
		String name = _nameFld.getText().trim();
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a name", "No Name", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		// Require a data source selection
		if (_importTypeCombo.getSelectedItem() == null) {
			JOptionPane.showMessageDialog(this, "Please Select a Data Source",
					"No Data Source", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		// Require at least one year to be selected in the met table
		List<Integer> selectedYears = getSelectedYears();
		if (selectedYears.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please Select one or more years",
					"No Years Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		return true;
	}

	/**
	 * Constructs and returns the list of {@link MeteorlogicData} items corresponding
	 * to each checked year in the met table.
	 *
	 * Each item is named {@code "<base-name>-<year>"} using the trimmed name field
	 * text, carries the trimmed description, the project-relative met config file path,
	 * and the year value. The data import step ({@link #importData}) is called first
	 * but is currently a no-op stub.
	 *
	 * @return a {@link List} of {@link MeteorlogicData} items, one per selected year;
	 *         empty if no years are checked
	 */
	public List<MeteorlogicData> getMetData() {
		// Create an holder for the met data
		List<MeteorlogicData> metDataSets = new ArrayList<>();

		// Get the years to be operated on
		List<Integer> years = getSelectedYears();

		// Trigger the (currently stubbed) DSS import step for the selected years
		importData(years);

		int year;
		for (int i = 0; i < years.size(); i++) {
			year = years.get(i);

			// Build one MeteorlogicData item per selected year
			MeteorlogicData metData = new MeteorlogicData();
			metData.setName(_nameFld.getText().trim() + "-" + year);
			metData.setDescription(_descFld.getText().trim());
			metData.setMetConfigFile(Project.getCurrentProject().getRelativePath(getSelectedMetConfigFile()));
			metData.setYear(year);
			metDataSets.add(metData);
		}

		return metDataSets;
	}

	/**
	 * Imports meteorology data from HEC-DSS for the selected years, shifting values
	 * to the appropriate analysis dates.
	 *
	 * Currently a stub pending full implementation.
	 *
	 * @param year the list of selected years for which DSS data should be imported
	 */
	private void importData(List<Integer> year) {
		// TODO: implement DSS data import and date-shifting logic
	}

	/**
	 * Retrieves all year values that are currently selected in the meteorology table.
	 * Selection state is determined by the value in column 0 of each row, which is
	 * expected to be a Boolean or its string representation. For each selected row,
	 * the Integer year value stored in column 1 is added to the result list.
	 *
	 * @return a list of Integer year values corresponding to all selected rows in the
	 *         meteorology table; empty if no rows are selected
	 */
	private List<Integer> getSelectedYears() {
		// Get the total number of rows currently displayed in the meteorology table
		int rowCnt = _metTable.getRowCount();

		// Declare a variable to hold the checkbox cell value retrieved during each iteration
		Object obj;

		// Initialize the list that will accumulate all selected year values
		List<Integer> selectedYears = new ArrayList<>();

		// Iterate over every row in the meteorology table to check its selection state
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the selection state value from the first column of the current row
			obj = _metTable.getValueAt(r, 0);

			// Accept both the Boolean object and its string representation for robustness
			if (Boolean.TRUE == obj || "true".equalsIgnoreCase(obj.toString())) {
				// Column 1 holds the Integer year value for this row
				Integer year = (Integer) _metTable.getValueAt(r, 1);

				// Add the selected year to the result list
				selectedYears.add(year);
			}
		}

		// Return the list of all selected year values
		return selectedYears;
	}

	/**
	 * Populates the data source combo box with available met config files and resets
	 * the cancelled state in preparation for a new user interaction.
	 *
	 * @param fsg the {@link ForecastSimGroup} for the current simulation group; not
	 *            used directly in the current implementation but provided for future
	 *            context-sensitive filtering
	 */
	public void fillForm(ForecastSimGroup fsg) {
		// Reload the config file combo with files found in the met config files folder
		fillMetDataCombo();

		// Reset the cancelled flag; it will be cleared to false only on a valid OK submission
		_canceled = true;
	}

	/**
	 * Populates the data source combo box with all {@code .config} files found in the
	 * project's met config files folder.
	 *
	 * If the folder does not exist or contains no config files, an informational
	 * {@link JOptionPane} message is shown on the EDT and the combo box is not updated.
	 * Each config file is wrapped in an {@link Identifier} whose name is the file's
	 * base name (without extension) and whose path is the full file path.
	 */
	private void fillMetDataCombo() {
		// Determine the project-relative folder that contains met config files
		String dir = ForecastConfigFiles.getMetConfigFilesFolder();
		RMAFilenameFilter filter = new RMAFilenameFilter("config");

		// List all .config files in the folder
		List<String> configFiles = FileManagerImpl.getFileManager().list(dir, filter);

		if (configFiles == null || configFiles.isEmpty()) {
			// Build an appropriate error message depending on whether the folder exists
			String msg;
			if (!FileManagerImpl.getFileManager().fileExists(dir)) {
				msg = "Missing the following folder " + dir;
			} else {
				msg = "No Met Config Files found in " + dir;
			}

			// Show the message on the EDT to avoid cross-thread Swing issues
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, msg,
					"No Met Config Files", JOptionPane.INFORMATION_MESSAGE));
			return;
		}

		// Convert the file paths to Identifier objects and populate the combo box model
		List<Identifier> ids = getConfigFileEntries(configFiles);
		RmaListModel<Identifier> newModel = new RmaListModel<>(true, ids);
		_importTypeCombo.setModel(newModel);
	}

	/**
	 * Converts a list of config file paths into a list of Identifier objects, each
	 * carrying the full file path and a display name derived from the file's base name
	 * without its extension. If the provided list is null or empty, an empty list is
	 * returned immediately without further processing.
	 *
	 * @param configFiles the list of config file path strings to convert
	 * @return            a list of Identifier objects, one per config file path, or an
	 *                    empty list if configFiles is null or empty
	 */
	private List<Identifier> getConfigFileEntries(List<String> configFiles) {
		// Initialize the list that will accumulate the resulting Identifier objects
		List<Identifier> ids = new ArrayList<>();

		// Return an empty list immediately if no config file paths were provided
		if (configFiles == null || configFiles.isEmpty()) {
			return ids;
		}

		// Declare variables to hold the current file path and its corresponding Identifier
		String file, path;
		Identifier id;

		// Iterate over each config file path and convert it to an Identifier
		for (int i = 0; i < configFiles.size(); i++) {
			// Retrieve the file path at the current index
			path = configFiles.get(i);

			// Wrap the file path in an Identifier with the base-name-without-extension as display name
			id = new Identifier(path);

			// Strip the file extension from the path to use as the human-readable display name
			id.setName(RMAIO.getFileNameNoExtension(path));

			// Add the fully configured Identifier to the result list
			ids.add(id);
		}

		// Return the complete list of Identifier objects
		return ids;
	}

	/**
	 * Returns whether this dialog was closed without a successful form submission.
	 *
	 * @return {@code true} if the dialog was cancelled or closed without submitting;
	 *         {@code false} after a valid OK submission
	 */
	@Override
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Returns the absolute file-system path of the met config file currently selected
	 * in the data source combo box.
	 *
	 * @return the selected config file path string; never {@code null} if a valid
	 *         item is selected in the combo box
	 */
	public String getSelectedMetConfigFile() {
		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();

		// Return the full path stored in the Identifier for the selected config file
		return id.getPath();
	}

	// -------------------------------------------------------------------------
	// Inner class: MetTableModel
	// -------------------------------------------------------------------------

	/**
	 * A specialised {@link RmaTableModel} that backs the meteorology data year table,
	 * displaying per-year selection state and spring, summer, and fall seasonal average
	 * air temperature values read from HEC-DSS records.
	 *
	 * The model has five columns:
	 *
	 *   Column 0 (Selected) — a checkbox indicating whether this year is
	 *       selected for import. Only editable for years whose DSS records are complete
	 *       (i.e., not in the {@code _invalidYears} map).
	 *   Column 1 (Year) — the calendar year integer.
	 *   Column 2 (Spring Ave Air Temp) — the March–May average, formatted
	 *       as a {@link ParamDouble} with one decimal place.
	 *   Column 3 (Summer Ave Air Temp) — the June–August average.
	 *   Column 4 (Fall Ave Air Temp) — the September–November average.
	 *
	 * Average values are returned as cloned {@link ParamDouble} instances so the table
	 * renderer can format them with correct units and precision. If a value is absent
	 * from the averages map, the HEC undefined sentinel ({@link RMAConst#UNDEF_DOUBLE})
	 * is used.
	 *
	 * The model also stores a {@link DSSIdentifier} reference to the primary DSS source
	 * record, displayed in the data source combo box tooltip.
	 */
	public class MetTableModel extends RmaTableModel {
		// Column index constants for readability
		private static final int SELECTION_COL = 0;
		private static final int YEAR_COL = 1;
		private static final int SPRING_AVE_COL = 2;
		private static final int SUMMER_AVE_COL = 3;
		private static final int FALL_AVE_COL = 4;

		// Ordered list of calendar years; each year corresponds to one table row
		private final List<Integer> _years;

		// Map from year integer to a list of missing DSS record path strings for that year
		private final Map<Integer, List<String>> _invalidYears;

		// Shared ParamDouble used for formatting average temperature values with units/precision
		private final ParamDouble _paramDouble;

		// Per-year spring (Mar-Apr-May) average air temperature values
		private Map<Integer, Double> _springAverages;

		// Per-year summer (Jun-Jul-Aug) average air temperature values
		private Map<Integer, Double> _summerAverages;

		// Per-year fall (Sep-Oct-Nov) average air temperature values
		private Map<Integer, Double> _fallAverages;

		// Per-row selection state; keyed by row index and defaults to false when absent
		private Map<Integer, Boolean> _selected = new HashMap<>();

		// The primary HEC-DSS source record identifier for this config file
		private DSSIdentifier _dssId;

		/**
		 * Constructs a MetTableModel with the given years, invalid year map, seasonal temperature
		 * averages, and unit string. Column headers for the three seasonal average columns are
		 * built dynamically by substituting the unit string into their label templates. A shared
		 * ParamDouble is initialised for temperature value formatting using the current project's
		 * unit system.
		 *
		 * @param years          the ordered list of years to display as rows in the table
		 * @param invalidYears   a map of year to error message list for years with missing data
		 * @param springAverages a map of year to computed spring average air temperature
		 * @param summerAverages a map of year to computed summer average air temperature
		 * @param fallAverages   a map of year to computed fall average air temperature
		 * @param units          the unit string substituted into the seasonal column header labels
		 */
		public MetTableModel(List<Integer> years, Map<Integer, List<String>> invalidYears,
		                     Map<Integer, Double> springAverages, Map<Integer, Double> summerAverages,
		                     Map<Integer, Double> fallAverages, String units) {
			// Build column headers by substituting the unit string into the seasonal labels
			super(new String[]{"Selected", "Year",
					"Spring Ave|Air Temp (%s)".replace("%s", units),
					"Summer Ave|Air Temp (%s)".replace("%s", units),
					"Fall Ave|Air Temp (%s)".replace("%s", units)});

			// Store the list of years that will be displayed as table rows
			_years = years;

			// Store the map of years to their associated error message lists for highlighting invalid rows
			_invalidYears = invalidYears;

			// Store the seasonal average maps for populating the three temperature columns
			_springAverages = springAverages;
			_summerAverages = summerAverages;
			_fallAverages = fallAverages;

			// Initialise the shared ParamDouble for temperature formatting using the current project's unit system
			_paramDouble = new ParamDouble(0, Parameter.PARAMID_TEMP,
					Project.getCurrentProject().getUnitSystem(), 1);
		}

		/**
		 * Returns the number of rows in the table, which corresponds to the number of years
		 * in the _years list. If the list has not been initialized, zero is returned to
		 * indicate an empty table.
		 *
		 * @return the number of years in _years, or 0 if _years is null
		 */
		public int getRowCount() {
			// Return the number of years in the list if it has been initialized
			if (_years != null) {
				return _years.size();
			}

			// _years has not been initialized; return 0 to indicate an empty table
			return 0;
		}

		/**
		 * Returns whether the cell at the given row and column is editable. Only the checkbox
		 * column (column 0) is editable, and only for rows whose year has no missing DSS
		 * records in _invalidYears. All other columns and any rows flagged as invalid are
		 * read-only. If _years has not been initialized, all cells are treated as read-only.
		 *
		 * @param row the zero-based row index of the cell to check
		 * @param col the zero-based column index of the cell to check
		 * @return    true if the cell is the checkbox column and its year has no missing data;
		 *            false otherwise
		 */
		public boolean isCellEditable(int row, int col) {
			// If the years list has not been initialized, no cells are editable
			if (_years == null) {
				return false;
			}

			// Only the checkbox column (col 0) is editable; all other columns are read-only
			if (col == 0) {
				// Retrieve the year value associated with the current row
				int obj = _years.get(row);

				// The row is selectable only if it has no missing DSS records
				if (_invalidYears.get(obj) == null) {
					// No missing data for this year; allow the checkbox to be toggled
					return true;
				}
			}

			// All other cases are read-only: wrong column or year has missing DSS records
			return false;
		}

		/**
		 * Returns the value for the cell at the specified row and column. The selection
		 * column returns a Boolean indicating the checkbox state, defaulting to false if
		 * not yet set. The year column returns the Integer year for the row. The three
		 * seasonal average columns return a cloned ParamDouble formatted to one decimal
		 * place, using RMAConst.UNDEF_DOUBLE as a sentinel when no average is available.
		 * If cloning fails, the raw double value is returned as a fallback. Returns null
		 * if _years has not been initialized or if the column index does not match any
		 * known column.
		 *
		 * @param row the zero-based row index of the cell to retrieve
		 * @param col the zero-based column index of the cell to retrieve
		 * @return    the cell value appropriate for the given column, or null if _years is
		 *            null or the column index is unrecognized
		 */
		public Object getValueAt(final int row, final int col) {
			// Return null for all cells if the years list has not been initialized
			if (_years == null) {
				return null;
			}

			// Retrieve the year associated with the current row
			int year = _years.get(row);

			// Declare a variable to hold the seasonal average value during column processing
			double val;

			// Dispatch to the appropriate return value based on the column index
			switch (col) {
				case SELECTION_COL:
					// Return the stored selection state, defaulting to false if not yet set
					Boolean selected = _selected.get(row);
					if (selected == null) {
						// No selection state has been stored yet; treat the row as unchecked
						return Boolean.FALSE;
					}
					return selected;

				case YEAR_COL:
					// Return the year value for this row directly
					return year;

				case SPRING_AVE_COL:
					// Retrieve the spring average for this year; use undefined sentinel if absent
					val = RMAConst.UNDEF_DOUBLE;
					if (_springAverages != null) {
						Double value = _springAverages.get(year);
						if (value != null) {
							// A spring average exists for this year; use it instead of the sentinel
							val = value;
						}
					}

					// Set the shared ParamDouble to the spring average value at one decimal precision
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try {
						// Return a clone so the table renderer gets an independent instance
						return _paramDouble.clone();
					} catch (CloneNotSupportedException e) {
						// Fall back to the raw double if cloning is not supported
						return val;
					}

				case SUMMER_AVE_COL:
					// Retrieve the summer average for this year; use undefined sentinel if absent
					val = RMAConst.UNDEF_DOUBLE;
					if (_summerAverages != null) {
						Double value = _summerAverages.get(year);
						if (value != null) {
							// A summer average exists for this year; use it instead of the sentinel
							val = value;
						}
					}

					// Set the shared ParamDouble to the summer average value at one decimal precision
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try {
						// Return a clone so the table renderer gets an independent instance
						return _paramDouble.clone();
					} catch (CloneNotSupportedException e) {
						// Fall back to the raw double if cloning is not supported
						return val;
					}

				case FALL_AVE_COL:
					// Retrieve the fall average for this year; use undefined sentinel if absent
					val = RMAConst.UNDEF_DOUBLE;
					if (_summerAverages != null) {
						Double value = _fallAverages.get(year);
						if (value != null) {
							// A fall average exists for this year; use it instead of the sentinel
							val = value;
						}
					}

					// Set the shared ParamDouble to the fall average value at one decimal precision
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try {
						// Return a clone so the table renderer gets an independent instance
						return _paramDouble.clone();
					} catch (CloneNotSupportedException e) {
						// Fall back to the raw double if cloning is not supported
						return val;
					}
			}

			// Column index did not match any known column; return null
			return null;
		}

		/**
		 * Sets the value for a cell in the model.
		 *
		 * Only the selection column (column 0) supports writes. The supplied object
		 * is parsed to a boolean via {@link Boolean#parseBoolean} and stored in the
		 * {@code _selected} map, then a cell-updated notification is fired.
		 *
		 * @param obj the new value to set; its {@code toString()} is parsed as a boolean
		 * @param row the zero-based row index
		 * @param col the zero-based column index; only column 0 is writable
		 */
		public void setValueAt(Object obj, final int row, final int col) {
			if (col == SELECTION_COL) {
				// Parse and store the new selection state, then notify the table of the change
				_selected.put(row, Boolean.parseBoolean(obj.toString()));
				fireTableCellUpdated(row, col);
			}
		}

		/**
		 * Returns the list of missing DSS record path strings for the year at the
		 * given row, or {@code null} if the year has no missing records.
		 * <p>
		 * Used by the table's custom tooltip renderer to show which DSS records are
		 * absent for rows that cannot be selected.
		 *
		 * @param row the zero-based row index
		 * @return the list of missing DSS record paths, or {@code null} if none
		 */
		public List<String> getMissingRecordsForRow(int row) {
			// Look up the year for this row and return its invalid records list (if any)
			Object year = getValueAt(row, YEAR_COL);
			return _invalidYears.get(year);
		}

		/**
		 * Sets the primary HEC-DSS source record identifier associated with this
		 * table model's config file.
		 *
		 * @param dssId the {@link DSSIdentifier} for the primary DSS source record;
		 *              displayed in the data source combo box tooltip
		 */
		public void setDssRecord(DSSIdentifier dssId) {
			_dssId = dssId;
		}

		/**
		 * Returns the primary HEC-DSS source record identifier for this table model's
		 * config file.
		 *
		 * @return the {@link DSSIdentifier} set via {@link #setDssRecord}, or
		 * {@code null} if not yet set
		 */
		public DSSIdentifier getDssRecord() {
			return _dssId;
		}
	}
}
