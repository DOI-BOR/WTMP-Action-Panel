package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Dimension;                                  // Provides Dimension for specifying preferred width/height of the scrollable table viewport
import java.awt.GridBagConstraints;                         // Provides GridBagConstraints for controlling component positioning within a GridBagLayout

import java.io.BufferedReader;                              // Provides BufferedReader for efficient line-by-line reading of the met config CSV file
import java.io.IOException;                                 // Provides IOException for handling errors that occur during file read operations

import java.util.ArrayList;                                 // Provides ArrayList for building mutable lists of MeteorlogicData objects
import java.util.HashMap;                                   // Provides HashMap for mapping met station location names to their MetLocation objects
import java.util.List;                                      // Provides the List interface for ordered collections of MeteorlogicData and related types
import java.util.Map;                                       // Provides the Map interface used to accumulate MetLocation entries keyed by station name
import java.util.Vector;                                    // Provides Vector for constructing table row data passed to RmaJTable.appendRow
import java.util.stream.Collectors;                         // Provides Collectors for terminal stream operations such as collecting to a List

import javax.swing.JButton;                                 // Provides JButton for the "Import..." action button in the lower panel
import javax.swing.JOptionPane;                             // Provides JOptionPane for displaying warning and error modal dialogs to the user

import com.google.common.flogger.FluentLogger;              // Provides FluentLogger for structured, levelled log output scoped to this class
import com.rma.io.FileManagerImpl;                          // Provides FileManagerImpl for obtaining managed RmaFile references from file paths
import com.rma.io.RmaFile;                                  // Provides RmaFile, an abstraction over a file resource within the RMA framework
import com.rma.model.Project;                               // Provides Project for accessing the current project's directory and path utilities

import rma.swing.EnabledJPanel;                             // Provides EnabledJPanel, a JPanel that propagates enable/disable state to its children
import rma.swing.RmaInsets;                                 // Provides RmaInsets constants for consistent padding values used in GridBagConstraints
import rma.swing.RmaJTable;                                 // Provides RmaJTable, an RMA-extended JTable with row management convenience methods
import rma.util.RMAIO;                                      // Provides RMAIO for file path utilities such as concatPath and isFullPath

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                  // Provides ActionPanelPlugin for accessing the singleton plugin instance and its window
import usbr.wat.plugins.actionpanel.model.forecast.BcData;              // Provides BcData, the boundary condition data model used to detect met data dependencies
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;         // Provides EnsembleSet for identifying ensemble sets that depend on boundary condition data
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles; // Provides ForecastConfigFiles for resolving project-relative config file paths
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;    // Provides ForecastSimGroup, the top-level model grouping all forecast simulation data
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;     // Provides MeteorlogicData, the model object representing a single meteorologic dataset
import usbr.wat.plugins.actionpanel.ui.MetPlotPanel;                    // Provides MetPlotPanel for rendering time-series plots of meteorologic station data

/**
 * Panel that displays and manages the Meteorology tab within the Forecast Action Panel.
 * It presents a table of available meteorologic datasets loaded from the active
 * ForecastSimGroup, a detail info table showing the selected dataset's metadata,
 * and a MetPlotPanel for visualising time-series data at individual met stations.
 *
 * Users can import new meteorologic datasets via the "Import..." button, select rows
 * to inspect their plots, and delete datasets with cascading removal of dependent
 * BcData and EnsembleSet entries.
 *
 * @see AbstractForecastPanel
 * @see MeteorlogicData
 * @see MetPlotPanel
 */
public class MeteorologyPanel extends AbstractForecastPanel<MeteorlogicData> {
	/**
	 * Logger instance scoped to this class for structured, levelled log output.
	 */
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	/**
	 * Project-relative path to the default met editor config CSV file.
	 */
	private static final String CONFIG_FILE = ForecastConfigFiles.getRelativeMetEditorFile();

	/**
	 * Read-only info table showing the name, type, description, and date of the selected met dataset.
	 */
	private RmaJTable _metInfoTable;

	/**
	 * Button that opens the ImportMetDataWindow dialog for importing new met datasets.
	 */
	private JButton _importButton;

	/**
	 * Panel that renders time-series met station plots for the selected dataset.
	 */
	private MetPlotPanel _plotPanel;

	/**
	 * The ForecastSimGroup currently displayed by this panel; null when no simulation is active.
	 */
	private ForecastSimGroup _fsg;

	/**
	 * Constructs a new MeteorologyPanel and wires it to the parent ForecastPanel.
	 *
	 * @param forecastPanel the parent ForecastPanel that owns and displays this tab
	 */
	public MeteorologyPanel(ForecastPanel forecastPanel) {
		super(forecastPanel);
	}

	/**
	 * Builds the lower section of the forecast panel by creating and laying out
	 * three components: a read-only metadata info table, an Import button, and
	 * the MetPlotPanel for time-series visualisation.
	 *
	 * @param lowerPanel the EnabledJPanel into which the lower section components are added
	 */
	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel) {
		String[] headers = new String[]{"Met Forecast Name", "Type", "Description", "Forecast Date"};

		// Create a read-only, single-row info table for the currently selected met dataset
		_metInfoTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Constrain the table's preferred height to exactly one row
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * 1;
				return d;
			}

			@Override
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
		gbc.weighty = 0.01;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_metInfoTable.getScrollPane(), gbc);

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

		// Create the time-series plot panel; it fills all remaining space below the table
		_plotPanel = new MetPlotPanel();

		// Configure layout: plot panel expands to fill all remaining width and height
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_plotPanel, gbc);
	}

	/**
	 * Registers all event listeners for this panel. Extends the superclass listener
	 * registration with an Import button action and a list selection listener on the
	 * upper met table to drive plot updates on row change.
	 */
	@Override
	protected void addListeners() {
		// Register superclass listeners (e.g. delete button, upper table listeners)
		super.addListeners();

		// Open the import dialog when the Import button is clicked
		_importButton.addActionListener(e -> importForecastData(null));

		// Refresh the plot whenever the user selects a different row in the met table
		getTableForPanel().getSelectionModel().addListSelectionListener(e -> tableRowSelected(_metTable.getSelectedRow()));
	}

	/**
	 * Clears the plot panel when the panel is reset. Also disables the panel if
	 * no ForecastSimGroup is currently loaded.
	 */
	@Override
	protected void clearPanel() {
		// Clear the panel
		_plotPanel.clearPanel();

		// Disable the panel entirely when there is no active simulation group
		if (_fsg == null) {
			setEnabled(false);
		}
	}

	/**
	 * Removes the given MeteorlogicData entry from the active ForecastSimGroup's
	 * meteorology data list.
	 *
	 * @param fsg  the ForecastSimGroup from which the data is removed
	 * @param data the MeteorlogicData entry to remove
	 */
	@Override
	protected void removeData(ForecastSimGroup fsg, MeteorlogicData data) {
		fsg.removeMetData(data);
	}

	/**
	 * Opens the ImportMetDataWindow to let the user select and import one or more
	 * meteorologic datasets into the current ForecastSimGroup. If a pre-constructed
	 * dialog is provided it is used directly; otherwise a new one is created.
	 * Each dataset returned by the dialog is passed through the shared importData
	 * helper; import stops early if any individual import fails.
	 *
	 * @param dlg a pre-built ImportForecastWindow to reuse, or null to create a new one
	 */
	@Override
	protected void importForecastData(ImportForecastWindow dlg) {
		ImportMetDataWindow importMetDataWindow;

		// Build a fresh import window anchored to the plugin's main actions window if none was provided
		if (dlg == null) {
			importMetDataWindow = new ImportMetDataWindow(ActionPanelPlugin.getInstance().getActionsWindow());
			importMetDataWindow.fillForm(_fsg);
		} else {
			// Reuse the supplied dialog, cast to the concrete met import type
			importMetDataWindow = (ImportMetDataWindow) dlg;
		}

		// Set the window to be visible
		importMetDataWindow.setVisible(true);

		// Abort if the user dismissed the dialog without confirming
		if (importMetDataWindow.isCanceled()) {
			return;
		}

		// Create the configuration
		List<MeteorlogicData> metData = importMetDataWindow.getMetData();
		String configFile = importMetDataWindow.getSelectedMetConfigFile();

		// Import each selected dataset; stop early if any individual import returns false
		for (int i = 0; i < metData.size(); i++) {
			boolean imported = importData(_fsg, _metTable, importMetDataWindow, _fsg.getMeteorlogyData(), metData.get(i));
			if (!imported) {
				break;
			}
		}
	}

	/**
	 * Returns the upper met data table that serves as the primary selection table
	 * for this panel. Required by the AbstractForecastPanel contract.
	 *
	 * @return the ForecastTable (_metTable) used as the meteorology data table
	 */
	@Override
	public ForecastTable getTableForPanel() {
		return _metTable;
	}

	/**
	 * Saves the current meteorology table contents back to the forecast simulation group.
	 * Iterates over every row in the meteorology table, collects the MeteorlogicData
	 * object stored in column 0 of each row, and persists the resulting list onto the
	 * simulation group via setMeteorlogyData(). If no simulation group is available,
	 * the method returns without taking any action. This method overrides the base class
	 * implementation to provide meteorology-specific save behavior.
	 */
	@Override
	protected void savePanel() {
		// Only proceed if a valid forecast simulation group is available
		if (_fsg != null) {
			// Initialize the list that will accumulate all MeteorlogicData objects from the table
			List<MeteorlogicData> metDataList = new ArrayList<>();

			// Get the total number of rows currently displayed in the meteorology table
			int numRows = _metTable.getNumRows();

			// Declare a variable to hold the MeteorlogicData object retrieved from each row
			MeteorlogicData metData;

			// Collect every MeteorlogicData object from the table's first column
			for (int r = 0; r < numRows; r++) {
				// Retrieve the MeteorlogicData object stored in column 0 of the current row
				metData = (MeteorlogicData) _metTable.getValueAt(r, 0);

				// Add the retrieved object to the accumulator list
				metDataList.add(metData);
			}

			// Persist the collected list back onto the simulation group
			_fsg.setMeteorlogyData(metDataList);
		}
	}

	/**
	 * Loads the panel for the given ForecastSimGroup: populates the upper met table
	 * with all meteorologic datasets stored in the group and refreshes the navigation
	 * panel. Disables the panel when the group is null.
	 *
	 * @param fsg the ForecastSimGroup whose met data should be displayed, or null to disable
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg) {
		setEnabled(fsg != null);
		_fsg = fsg;

		// Clear any previously loaded rows from the table and disable the plot
		getTableForPanel().deleteCells();
		_plotPanel.setEnabled(false);

		if (_fsg != null) {
			List<MeteorlogicData> data = _fsg.getMeteorlogyData();

			// Clear the met table and repopulate it with the current group's datasets
			_metTable.deleteCells();
			Vector<MeteorlogicData> row;
			for (int i = 0; i < data.size(); i++) {
				// Each row holds a single MeteorlogicData object in its first column
				row = new Vector<>();
				row.add(data.get(i));
				_metTable.appendRow(row);
			}

			// Populate the met station navigation / plot panel for the current selection
			fillNavPanel();
		}
	}

	/**
	 * Reads the met config CSV for the currently selected met dataset and builds
	 * the list of MetLocation objects (each containing one or more DssLocation entries)
	 * that the MetPlotPanel uses to populate its station navigator.
	 * <p>
	 * The config CSV format per row is:
	 * Met Station Location, Met parameter, Source DSS file, Source DSS record,
	 * Number of Destinations, Destination DSS file, Destination DSS record
	 * <p>
	 * TODO: only supports ResSim model alternatives at this time.
	 */
	private void fillNavPanel() {
		// Reset the plot panel's location list before repopulating it
		_plotPanel.setLocationList(null);

		Project prj = Project.getCurrentProject();

		// Determine which met config file to use based on the selected table row
		int row = _metTable.getSelectedRow();
		Object tableObj = _metTable.getValueAt(row, 0);
		String metConfigFile = null;
		String configPath = null;

		if (tableObj instanceof MeteorlogicData) {
			// Extract the config file path stored on the selected MeteorlogicData object
			MeteorlogicData metData = (MeteorlogicData) tableObj;
			metConfigFile = metData.getMetConfigFile();
		}

		String prjDir = prj.getProjectDirectory();

		// Fall back to the default met editor config when the data has no specific config
		if (metConfigFile == null) {
			configPath = RMAIO.concatPath(prjDir, CONFIG_FILE);

		} else {
			// Resolve a relative config path against the project directory; use absolute paths as-is
			if (!RMAIO.isFullPath(metConfigFile)) {
				configPath = RMAIO.concatPath(prjDir, metConfigFile);

			} else {
				configPath = metConfigFile;
			}
		}

		RmaFile configFile = FileManagerImpl.getFileManager().getFile(configPath);

		// Abort if the config file cannot be located on disk
		if (configFile == null || !configFile.exists()) {
			LOGGER.atWarning().log("Failed to find met config file file " + configPath);
			JOptionPane.showMessageDialog(this, "<html>The met config file <br>" + configPath + "<br>does not exist.", "Missing File", JOptionPane.WARNING_MESSAGE);
			return;
		}

		BufferedReader reader = configFile.getBufferedReader();

		// Abort if a reader cannot be obtained for the located file
		if (reader == null) {
			LOGGER.atWarning().log("Failed to get reader for  met config file file " + configPath);
			JOptionPane.showMessageDialog(this, "<html>Failed to get reader for the  met config file <br>" + configPath, "Read Failed", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String line;

		try {
			// Skip the CSV header row (column label line)
			// Met Station Location, Met parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record
			reader.readLine();

			// Map from station name to its MetLocation; multiple rows may share the same station
			Map<String, MetLocation> locationInfo = new HashMap<>();
			String name;

			while ((line = reader.readLine()) != null) {
				String[] metInfoArray = line.split(",");

				// Skip blank or malformed lines
				if (metInfoArray == null || metInfoArray.length == 0) {
					continue;
				}

				// Look up an existing MetLocation or create a new one for this station name
				MetLocation metLoc = locationInfo.get(metInfoArray[0]);
				if (metLoc == null) {
					metLoc = new MetLocation();
					name = metInfoArray[0].trim();
					metLoc.setName(name);
					locationInfo.put(name, metLoc);
				}

				// Build a DssLocation from the parameter, source DSS file, and source DSS record columns
				DssLocation dssLoc = new DssLocation(metInfoArray[1].trim(), metInfoArray[2].trim(), metInfoArray[3].trim());
				metLoc.addDssLocation(dssLoc);
			}

			// Pass the assembled location list to the plot panel for station navigation
			_plotPanel.setLocationList(locationInfo.values().stream().collect(Collectors.toList()));

		} catch (IOException ioe) {
			LOGGER.atWarning().withCause(ioe).log("Error reading file " + configFile.getAbsolutePath());

		} finally {
			// Always close the reader to release the underlying file resource
			try {
				reader.close();

			} catch (IOException e) {
			}
		}
	}

	/**
	 * Responds to row selection changes in the upper met table. Populates the
	 * info table with the selected dataset's metadata, refreshes the navigation
	 * panel, sets the plot year, and enables or disables the plot panel accordingly.
	 *
	 * @param selRow the index of the newly selected row, or -1 if the selection was cleared
	 */
	@Override
	protected void tableRowSelected(int selRow) {
		if (_fsg != null) {
			// Clear the detail info table before repopulating for the new selection
			_metInfoTable.deleteCells();

			if (selRow > -1) {
				MeteorlogicData metData = (MeteorlogicData) _metTable.getValueAt(selRow, 0);

				// Build a single info row: name, config file name (no extension), description
				Vector row = new Vector();
				row.add(metData.getName());
				row.add(getMetConfigFileName(metData.getMetConfigFile()));
				row.add(metData.getDescription());

				_metInfoTable.appendRow(row);

				// Rebuild the station location list for the newly selected met dataset
				fillNavPanel();

				// Configure the plot for the selected dataset's forecast year
				_plotPanel.setYear(metData.getYear());
				_plotPanel.setEnabled(true);

				// Ensure the table selection highlight is kept in sync
				_metTable.setRowSelectionInterval(selRow, selRow, false);
				_metTable.updateSelection(selRow, 0, false, false);
			} else {
				// No row selected: clear and disable the plot panel
				_plotPanel.clearPanel();
				_plotPanel.setEnabled(false);
			}

			// Repaint the plot panel to reflect the new selection state
			_plotPanel.fillPlotPanel();
		}
	}

	/**
	 * Returns the display name for the given met config file path. Falls back to
	 * the default historical met config file name when no specific file is set.
	 * The file extension is stripped from the returned name.
	 *
	 * @param metConfigFile the met config file path stored on the dataset, or null
	 * @return the file name without extension, derived from the resolved config path
	 */
	private Object getMetConfigFileName(String metConfigFile) {
		// Use the default historical met config file when no specific config is set
		if (metConfigFile == null) {
			metConfigFile = ForecastConfigFiles.getRelativeHistoricalMetFile();
		}

		// Strip the extension so only the bare file name is shown in the info table
		return RMAIO.getFileNameNoExtension(metConfigFile);
	}

	/**
	 * Called when the delete button is clicked for a row in the upper met table.
	 * Delegates to the delete method after confirming that the row holds a
	 * MeteorlogicData object and that an active simulation group is present.
	 *
	 * @param rowToDelete the index of the table row whose dataset should be deleted
	 */
	@Override
	public void tableRowDeleteClicked(int rowToDelete) {
		Object value = _metTable.getValueAt(rowToDelete, 0);

		if (_fsg != null && value instanceof MeteorlogicData) {
			delete((MeteorlogicData) value, false);
		}
	}

	/**
	 * Displays a confirmation dialog before deleting the given MeteorlogicData entry.
	 * Identifies all BcData records and EnsembleSet objects that depend on this entry
	 * so the confirmation message can warn the user of cascading deletions.
	 * Performs the deletion (including dependents) only if the user confirms.
	 *
	 * @param metData                the MeteorlogicData entry to delete or overwrite
	 * @param deletingDueToOverwrite true if deletion is triggered by an overwrite operation;
	 *                               changes the wording of the confirmation message
	 * @return true if the deletion was confirmed and performed; false if cancelled
	 */
	@Override
	protected boolean delete(MeteorlogicData metData, boolean deletingDueToOverwrite) {
		boolean retVal = false;

		// Find all BcData entries that reference this met dataset
		List<BcData> bcDataUsingMetData = _fsg.getBcDataUsingMetData(metData);

		// Find all EnsembleSets that depend on those BcData entries (cascading dependencies)
		List<EnsembleSet> eSetsUsingBcData = bcDataUsingMetData.stream()
				.map(bcData -> _fsg.getEnsembleSetsUsingBcData(bcData))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// Compose an appropriate confirmation message based on whether this is an overwrite
		String initialMessage;
		if (deletingDueToOverwrite) {
			initialMessage = metData.getName() + " already exists." + "Do you want to overwrite it?";

		} else {
			initialMessage = "Do you want to delete meteorologic data " + metData.getName() + "?";
		}

		// Show the confirmation dialog; only proceed if the user confirms
		if (displayDeleteMessage(initialMessage, bcDataUsingMetData, eSetsUsingBcData, deletingDueToOverwrite,
				metData)) {
			retVal = true;

			// Execute the deletion of the met data and all cascading dependents
			performDelete(_fsg, metData, _metTable, bcDataUsingMetData, eSetsUsingBcData);
		}

		return retVal;
	}
}
