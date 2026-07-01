package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Cursor;                                                     // Provides Cursor for switching to a wait cursor during long-running script and import operations
import java.awt.Dimension;                                                  // Provides Dimension for constraining the preferred scrollable viewport height of the info table
import java.awt.GridBagConstraints;                                         // Provides GridBagConstraints for specifying layout parameters within the lower panel GridBagLayout

import java.io.IOException;                                                 // Provides IOException for catching file-system errors when creating script and simulation directories
import java.nio.file.Files;                                                 // Provides Files for creating directories on the file system
import java.nio.file.Path;                                                  // Provides Path for representing file-system paths in a platform-independent way
import java.nio.file.Paths;                                                 // Provides Paths for constructing Path instances from string segments

import java.util.ArrayList;                                                 // Provides ArrayList for constructing empty lists passed to the delete confirmation dialog
import java.util.List;                                                      // Provides the List interface for ordered collections of BcData items and ensemble sets
import java.util.Vector;                                                    // Provides Vector as the row data container required by RmaJTable's row insertion methods
import java.util.logging.Level;                                             // Provides Level for categorising log messages (CONFIG used for script result logging)
import java.util.logging.Logger;                                            // Provides Logger for recording script execution results and directory creation failures
import java.util.stream.Collectors;                                         // Provides Collectors for terminal stream operations (not directly used; retained for potential use)

import javax.swing.JButton;                                                 // Provides JButton for the "Create B.C. Sets..." button in the lower panel
import javax.swing.JOptionPane;                                             // Provides JOptionPane for displaying error and confirmation dialogs

import com.rma.model.Project;                                               // Provides Project for resolving relative file paths against the current WAT project directory
import hec.heclib.util.HecTime;                                             // Provides HecTime for constructing start and end time objects passed to the boundary condition script
import hec.lang.NamedType;                                                  // Provides NamedType as the supertype bound used implicitly through the AbstractForecastPanel generic
import hec2.wat.model.WatAnalysisPeriod;                                    // Provides WatAnalysisPeriod for retrieving the run-time window start and end times

import rma.swing.EnabledJPanel;                                             // Provides EnabledJPanel as the container type for the lower panel passed to buildLowerPanel
import rma.swing.RmaInsets;                                                 // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJTable;                                                 // Provides RmaJTable as the base class for the boundary condition info table in the lower panel
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                      // Provides ActionPanelPlugin for accessing the singleton plugin instance and its actions window

import usbr.wat.plugins.actionpanel.model.forecast.BcData;                  // Provides BcData as the typed data item this panel manages (boundary condition set records)
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;             // Provides EnsembleSet for identifying ensemble sets that depend on boundary condition data being deleted
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;     // Provides ForecastConfigFiles for resolving the project-relative flow pattern configuration file path
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Provides ForecastSimGroup as the top-level data container holding all forecast data for a simulation
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;         // Provides MeteorlogicData for looking up the meteorology record referenced by a boundary condition set
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;          // Provides OperationsData for looking up the operations record referenced by a boundary condition set
import usbr.wat.plugins.actionpanel.ui.BoundaryConditionPlotPanel;          // Provides BoundaryConditionPlotPanel for rendering the time-series plot of the selected BC set

/**
 * The forecast panel responsible for displaying, creating, and deleting boundary
 * condition (BC) sets within the WTMP action panel UI.
 *
 * {@code BcPanel} extends {@link AbstractForecastPanel} typed to {@link BcData} and
 * provides the following functionality:
 *
 *   A lower panel containing a single-row info table showing the selected BC set
 *       name, a "Create B.C. Sets..." button, and a
 *       {@link BoundaryConditionPlotPanel} that renders the time-series data for the selected set.
 *   BC set creation via {@link CreateBcWindow}, which collects user input and
 *       invokes a Python script ({@code BoundaryConditionScript.py}) to generate the
 *       underlying DSS data.
 *   Delete logic that checks for dependent ensemble sets and presents a cascaded
 *       confirmation dialog before removal.
 *
 * The {@code @SuppressWarnings("serial")} annotation suppresses the compiler warning
 * about the missing {@code serialVersionUID} field; this class is not intended for
 * Java object serialization.
 *
 * @see AbstractForecastPanel
 * @see BcData
 * @see CreateBcWindow
 * @see BoundaryConditionPlotPanel
 */
@SuppressWarnings("serial")
public class BcPanel extends AbstractForecastPanel<BcData> {
	// Logger for recording script execution results and directory creation errors
	private static final Logger LOGGER = Logger.getLogger(BcPanel.class.getName());

	// Single-row read-only table in the lower panel that displays the selected BC set name
	private RmaJTable _bcInfoTable;

	// Button that opens the CreateBcWindow dialog to generate new boundary condition sets
	private JButton _createButton;

	// Plot panel that renders the time-series data for the currently selected BC set
	private BoundaryConditionPlotPanel _plotPanel;

	// The currently active forecast simulation group; set when fillPanel is called
	private ForecastSimGroup _fsg;

	/**
	 * Constructs a {@code BcPanel} and delegates all shared initialisation to the
	 * {@link AbstractForecastPanel} superclass (table layout, listener wiring, and
	 * panel registration).
	 *
	 * @param forecastPanel the {@link ForecastPanel} that owns and hosts this panel;
	 *                      must not be {@code null}
	 */
	public BcPanel(ForecastPanel forecastPanel) {
		super(forecastPanel);
	}

	/**
	 * Populates the lower panel with the BC-specific controls: a single-row info table,
	 * a "Create B.C. Sets..." button, and a {@link BoundaryConditionPlotPanel}.
	 *
	 * The info table is read-only and sized to show exactly one row. The plot panel fills
	 * the remaining vertical space in the lower panel.
	 *
	 * @param lowerPanel the {@link EnabledJPanel} provided by the superclass into which
	 *                   all lower-panel controls are added
	 */
	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel) {
		// Build a single-column read-only info table that shows the selected BC set name
		String[] headers = new String[]{"Boundary Condition Set"};

		_bcInfoTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = super.getPreferredScrollableViewportSize();

				// Fix the viewport height to exactly one row
				d.height = getRowHeight() * 1;

				return d;
			}

			// Prevent all cells from being edited inline
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};

		// Add the info table scroll pane to the lower panel with minimal vertical weight
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.01;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_bcInfoTable.getScrollPane(), gbc);

		// Add the "Create B.C. Sets..." button; it does not stretch horizontally
		_createButton = new JButton("Create B.C. Sets...");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);

		// Build and add the plot panel; it claims all remaining vertical space
		_plotPanel = new BoundaryConditionPlotPanel();
		_plotPanel.getPlotPanel().buildDefaultComponents();
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
	 * Deletes the given {@link BcData} boundary condition set after presenting a
	 * confirmation dialog that includes any ensemble sets that will also be removed
	 * as a cascade.
	 *
	 * If {@code deleteDueToOverwrite} is {@code true}, the dialog uses "overwrite"
	 * wording; otherwise it uses "delete" wording. The actual removal is delegated to
	 * {@link AbstractForecastPanel#performDelete} only if the user confirms.
	 *
	 * @param bcData             the {@link BcData} item to delete; must not be {@code null}
	 * @param deleteDueToOverwrite {@code true} if this deletion is triggered by an
	 *                           overwrite rather than a direct user delete action
	 * @return {@code true} if the user confirmed and the deletion was performed;
	 *         {@code false} if the user cancelled
	 */
	@Override
	protected boolean delete(BcData bcData, boolean deleteDueToOverwrite) {
		boolean retVal = false;

		// Find all ensemble sets that reference this BC set so they can be listed in the warning
		List<EnsembleSet> eSetsUsingBcData = _fsg.getEnsembleSetsUsingBcData(bcData);

		// Build the opening sentence of the confirmation message based on the operation type
		String initialMessage;
		if (deleteDueToOverwrite) {
			initialMessage = bcData.getName() + " already exists." + "Do you want to overwrite it?";

		} else {
			initialMessage = "Do you want to delete boundary condition set " + bcData.getName() + "?";
		}

		// Present the confirmation dialog; an empty BC list is passed because BC sets do not
		// cascade-delete other BC sets (only ensemble sets cascade from a BC delete)
		if (displayDeleteMessage(initialMessage, new ArrayList<>(), eSetsUsingBcData, deleteDueToOverwrite, bcData)) {
			retVal = true;

			// Perform the actual delete and refresh any dependent panels
			performDelete(_fsg, bcData, _bcTable, new ArrayList<>(), eSetsUsingBcData);
		}

		return retVal;
	}

	/**
	 * Extends the superclass listener registration by also attaching an action listener
	 * to the "Create B.C. Sets..." button that opens the BC creation workflow.
	 */
	@Override
	protected void addListeners() {
		// Register all shared table selection and mouse listeners from the superclass
		super.addListeners();

		// Wire the create button to the import workflow (null signals a fresh dialog)
		_createButton.addActionListener(e -> importForecastData(null));
	}

	/**
	 * Opens or re-uses a {@link CreateBcWindow} to collect BC set parameters from the
	 * user, then runs the boundary condition Python script on each resulting
	 * {@link BcData} item and imports the results into the panel table.
	 *
	 * If {@code dlg} is {@code null}, a new {@link CreateBcWindow} is created and
	 * pre-filled with the current simulation group data. If {@code dlg} is non-null
	 * (the user was returned to the dialog after cancelling an overwrite), the existing
	 * dialog instance is reused.
	 *
	 * Wait cursors are applied to all relevant components for the duration of the
	 * script execution and are restored in a {@code finally} block. If the simulation
	 * group has no analysis period, an error dialog is shown and the import is aborted.
	 *
	 * @param dlg an existing {@link ImportForecastWindow} to re-open, or {@code null}
	 *            to create a new {@link CreateBcWindow}
	 */
	@Override
	protected void importForecastData(ImportForecastWindow dlg) {
		// Create a fresh dialog or reuse the existing one if the user was redirected back
		CreateBcWindow createBcWindow;
		if (dlg == null) {
			createBcWindow = new CreateBcWindow(_fsg, ActionPanelPlugin.getInstance().getActionsWindow());
			createBcWindow.fillForm(_fsg);

		} else {
			createBcWindow = (CreateBcWindow) dlg;
		}

		// Display the dialog modally and check if the user cancelled
		createBcWindow.setVisible(true);
		if (createBcWindow.isCanceled()) {
			return;
		}

		// Retrieve the list of BC data items the user configured in the dialog
		List<BcData> bcDataList = createBcWindow.getBcData();

		try {
			// Apply wait cursors to all components that will be busy during script execution
			_plotPanel.getPlotPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			_bcInfoTable.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			_plotPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			WatAnalysisPeriod analysisPeriod = _fsg.getAnalysisPeriod();

			if (analysisPeriod == null) {
				// Cannot proceed without an analysis period; notify the user and abort
				JOptionPane.showMessageDialog(this, "Failed to find analysis period for simulation: "
						+ _fsg.getName(), "Analysis Period Not Found", JOptionPane.ERROR_MESSAGE);

			} else {
				// Validate script files and run the BC generation script for each item
				applyScriptToBCDataWithAnalysisPeriod(createBcWindow, bcDataList, _bcTable);
			}

		} finally {
			// Always restore default cursors, even if an exception occurs during script execution
			setCursor(Cursor.getDefaultCursor());
			_plotPanel.setCursor(Cursor.getDefaultCursor());
			_bcInfoTable.setCursor(Cursor.getDefaultCursor());
			_plotPanel.getPlotPanel().setCursor(Cursor.getDefaultCursor());
		}

		// Mark the simulation group as modified to trigger a save prompt on close
		_fsg.setModified(true);
	}

	/**
	 * Validates that both required Python script files exist in the project before
	 * delegating to {@link #runScriptOnBCDataList}.
	 *
	 * Checks for the presence of:
	 *
	 *   {@code forecast/scripts/BoundaryConditionScript.py} — the main BC generation script
	 *   {@code forecast/scripts/CVP_ops_tools.py} — the CVP operations helper module<
	 *
	 * If either file is missing, an error dialog is shown and the method returns without
	 * running the script.
	 *
	 * @param dlg       the {@link CreateBcWindow} dialog to pass to the script runner for
	 *                  potential overwrite handling
	 * @param bcDataList the list of {@link BcData} items to process
	 * @param bcTable   the {@link ForecastTable} into which successfully imported items are added
	 */
	private void applyScriptToBCDataWithAnalysisPeriod(CreateBcWindow dlg, List<BcData> bcDataList, ForecastTable bcTable) {
		// Define relative paths to the required Python script files
		Path scriptFile = Paths.get("forecast/scripts/BoundaryConditionScript.py");
		Path cvpModuleFile = Paths.get("forecast/scripts/CVP_ops_tools.py");

		if (!Paths.get(Project.getCurrentProject().getAbsolutePath(scriptFile.toString())).toFile().exists()) {
			// Main script not found; show an error and abort
			JOptionPane.showMessageDialog(this, "Failed to find script file: \n"
					+ Project.getCurrentProject().getAbsolutePath(scriptFile.toString()), "Script Not Found", JOptionPane.ERROR_MESSAGE);

		} else if (!Paths.get(Project.getCurrentProject().getAbsolutePath(cvpModuleFile.toString())).toFile().exists()) {
			// CVP helper module not found; show an error and abort
			JOptionPane.showMessageDialog(this, "Failed to find script file: \n"
					+ Project.getCurrentProject().getAbsolutePath(cvpModuleFile.toString()), "Script Not Found", JOptionPane.ERROR_MESSAGE);

		} else {
			// Both files exist; proceed with running the script on each BC data item
			runScriptOnBCDataList(dlg, bcDataList, bcTable, scriptFile);
		}
	}

	/**
	 * Iterates over a list of {@link BcData} items, running the boundary condition
	 * Python script for each and importing the result into the panel table.
	 *
	 * The loop stops early if any import fails (e.g., the user cancels an overwrite
	 * confirmation). If all items are processed successfully, the last row of the table
	 * is selected to reflect the final imported item.
	 *
	 * @param dlg        the {@link CreateBcWindow} dialog, passed to
	 *                   {@link #importData} for overwrite handling
	 * @param bcDataList the list of {@link BcData} items to process; each item is
	 *                   scripted then imported in order
	 * @param bcTable    the {@link ForecastTable} into which imported rows are added
	 * @param scriptFile the {@link Path} to the Python BC generation script
	 */
	private void runScriptOnBCDataList(CreateBcWindow dlg, List<BcData> bcDataList, ForecastTable bcTable, Path scriptFile) {
		boolean success = true;

		for (BcData bcData : bcDataList) {
			// Run the Python script to generate DSS data for this BC set
			runScript(bcData, scriptFile);

			// Import the result into the table; stops the loop if the user cancels an overwrite
			success = importData(_fsg, _bcTable, dlg, _fsg.getBcData(), bcData);

			if (!success) {
				break;
			}
		}

		if (success) {
			// Select the last imported row to reflect the most recently added BC set
			tableRowSelected(bcTable.getRowCount() - 1);
		}
	}

	/**
	 * Runs the boundary condition Python script for a single {@link BcData} item,
	 * supplying all required parameters from the current simulation group, operations
	 * data, meteorology data, and analysis period.
	 *
	 * Before executing the script, the simulation group and scripts directories are
	 * created if they do not already exist. The script is skipped silently if the
	 * analysis period is null, or if the operations or meteorology record referenced
	 * by the BC data item cannot be found in the simulation group.
	 *
	 * The script result (an integer return code) is logged at {@link Level#CONFIG}.
	 *
	 * @param bcData     the {@link BcData} item for which the script is run; its output
	 *                   DSS file path and F-part are set as side effects
	 * @param scriptFile the {@link Path} to the Python BC generation script to execute
	 */
	private void runScript(BcData bcData, Path scriptFile) {
		// Ensure the simulation group and scripts directories exist before running
		createScriptsDir();

		WatAnalysisPeriod analysisPeriod = _fsg.getAnalysisPeriod();

		if (analysisPeriod != null) {
			// Look up the operations record referenced by this BC set (case-insensitive match)
			OperationsData opsData = _fsg.getOperationsData().stream()
					.filter(ops -> ops.getName().equalsIgnoreCase(bcData.getOpsDataName()))
					.findFirst()
					.orElse(null);

			// Look up the meteorology record referenced by this BC set (case-insensitive match)
			MeteorlogicData metData = _fsg.getMeteorlogyData().stream()
					.filter(met -> met.getName().equalsIgnoreCase(bcData.getMetDataName()))
					.findFirst()
					.orElse(null);

			if (opsData != null && metData != null) {
				// Extract start and end times from the analysis period's run-time window
				HecTime startTime = new HecTime(analysisPeriod.getRunTimeWindow().getStartTime());
				HecTime endTime = new HecTime(analysisPeriod.getRunTimeWindow().getEndTime());

				// Use the BC set name as the DSS F-part identifier
				String bcFPart = bcData.getName();
				bcData.setFPart(bcFPart);

				// Define and assign the relative path for the output DSS file
				Path bcOutputDssFileRelativePath = Paths.get("forecast/simGroups/" + _fsg.getName() + "/bc.dss");
				String bcOutputDssFile = Project.getCurrentProject().getAbsolutePath(bcOutputDssFileRelativePath.toString());
				bcData.setOutputDssFile(bcOutputDssFileRelativePath);

				// Resolve all remaining script parameter paths and identifiers
				String opsFileName = opsData.getOperationsFile();
				String dssMapFile = Project.getCurrentProject().getAbsolutePath("forecast/simGroups/" + _fsg.getName() + "/" + bcData.getName() + ".txt");
				int positionAnalysisYear = metData.getYear();

				String positionalAnalysisConfigFile = Project.getCurrentProject().getAbsolutePath(metData.getMetConfigFile());
				String metFPart = bcFPart;
				String metOutputDssFileName = bcOutputDssFile;
				String opsImportFPart = bcFPart;
				String flowPatternConfigFile = Project.getCurrentProject().getAbsolutePath(ForecastConfigFiles.getRelativeFlowPatternFile());

				// Invoke the Python script and capture its integer return code
				Integer result = PythonScriptUtil.runScript(scriptFile, "build_BC_data_sets", Integer.class,
						startTime, endTime, bcFPart, bcOutputDssFile, opsFileName, dssMapFile,
						positionAnalysisYear, positionalAnalysisConfigFile,
						metFPart, metOutputDssFileName, flowPatternConfigFile, opsImportFPart);

				// Log the script result at CONFIG level for diagnostic purposes
				LOGGER.log(Level.CONFIG, () -> "Result from " + scriptFile + ": " + result);
			}
		}
	}

	/**
	 * Creates the simulation group output directory and the scripts directory within
	 * the current project if they do not already exist.
	 *
	 * Directory paths created:
	 *
	 *   {@code forecast/simGroups/<fsg-name>} — output directory for this simulation group
	 *   {@code forecast/scripts} — directory containing the Python script files
	 *
	 * Any {@link IOException} encountered during directory creation is logged at
	 * {@link Level#CONFIG} and does not propagate to the caller.
	 */
	private void createScriptsDir() {
		// Define relative paths for the simulation group output and scripts directories
		String forecastSimGroupDirectory = "forecast/simGroups/" + _fsg.getName();
		String scriptsDir = "forecast/scripts";

		try {
			// Resolve and create the simulation group directory (and any missing parents)
			Path absSimGroupDirectory = Paths.get(Project.getCurrentProject().getAbsolutePath(forecastSimGroupDirectory));
			Files.createDirectories(absSimGroupDirectory);

			// Resolve and create the scripts directory (and any missing parents)
			Path absScriptDir = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptsDir));
			Files.createDirectories(absScriptDir);

		} catch (IOException e) {
			// Log the failure at CONFIG level; directory creation errors are non-fatal
			LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + forecastSimGroupDirectory + " directories");
		}
	}

	/**
	 * Returns the boundary condition {@link ForecastTable} ({@code _bcTable}) as the
	 * primary table managed by this panel.
	 *
	 * @return the BC {@link ForecastTable}; never {@code null} after construction
	 */
	@Override
	public ForecastTable getTableForPanel() {
		return _bcTable;
	}

	/**
	 * Saves the current lower-panel state back to the data model.
	 *
	 * Currently a no-op for the BC panel, as BC set data is persisted during creation
	 * and deletion rather than through an editable lower-panel form.
	 */
	@Override
	protected void savePanel() {
		// No lower-panel fields to save for the boundary condition panel
	}

	/**
	 * Populates the BC table and plot panel with data from the given
	 * {@link ForecastSimGroup}, replacing any previously displayed data.
	 *
	 * Enables or disables the entire panel based on whether {@code fsg} is non-null.
	 * If the simulation group has at least one BC set, the plot panel is pre-populated
	 * with the first item's data.
	 *
	 * @param fsg the {@link ForecastSimGroup} whose BC data is to be displayed, or
	 *            {@code null} to clear and disable the panel
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg) {
		// Enable or disable the panel based on whether a simulation group is present
		setEnabled(fsg != null);
		_fsg = fsg;

		ForecastTable table = getTableForPanel();

		// Clear any previously displayed BC rows before reloading
		table.deleteCells();

		// Disable the plot panel until a BC set is selected
		_plotPanel.setEnabled(false);

		if (_fsg != null) {
			// Append each BC data item as a row in the table
			List<BcData> data = _fsg.getBcData();
			Vector<BcData> row;
			for (int i = 0; i < data.size(); i++) {
				row = new Vector<>();
				row.add(data.get(i));
				table.appendRow(row);
			}

			if (!data.isEmpty()) {
				// Pre-populate the plot with the first BC set so the panel is not empty on load
				_plotPanel.fillPanel(_fsg, data.get(0));
			}
		}
	}

	/**
	 * Responds to a row selection change in the BC table by updating the info table and
	 * plot panel to reflect the selected {@link BcData} item.
	 *
	 * If {@code selRow} is {@code -1} (no selection), {@link #clearPanel()} is called
	 * to reset the lower panel. Otherwise, the selected item's name is shown in the
	 * info table and its data is loaded into the plot panel.
	 *
	 * @param selRow the zero-based index of the newly selected row, or {@code -1} if
	 *               the selection was cleared
	 */
	@Override
	protected void tableRowSelected(int selRow) {
		if (_fsg != null) {
			ForecastTable table = getTableForPanel();

			// Clear the info table before populating it with the selected item's name
			_bcInfoTable.deleteCells();

			if (selRow > -1) {
				Object value = table.getValueAt(selRow, 0);

				if (value instanceof BcData) {
					BcData bcData = (BcData) value;

					// Display the selected BC set name in the single-row info table
					Vector<String> row = new Vector<>();
					row.add(bcData.getName());
					_bcInfoTable.appendRow(row);

					// Update the plot panel with the selected BC set's time-series data
					_plotPanel.fillPanel(_fsg, bcData);

					// Synchronise the table's visual selection with the programmatic selection
					_bcTable.setRowSelectionInterval(selRow, selRow, false);
					_bcTable.updateSelection(selRow, 0, false, false);
				}

			} else {
				// No row selected; reset the lower panel to its empty state
				clearPanel();
			}
		}
	}

	/**
	 * Extends the superclass visibility handling to clear the lower panel whenever
	 * this panel becomes visible with no BC set currently selected.
	 *
	 * @param visible {@code true} to show the panel; {@code false} to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		// Clear the lower panel if becoming visible with no row selected in the BC table
		if (visible && _bcTable.getSelectedRow() < 0) {
			clearPanel();
		}
	}

	/**
	 * Resets the lower panel to its empty state by clearing the plot panel and the
	 * info table, and disabling the entire panel if no simulation group is loaded.
	 */
	@Override
	protected void clearPanel() {
		// Reset the plot panel to its empty/default state
		_plotPanel.clearPanel();

		// Remove any name rows from the info table
		_bcInfoTable.deleteCells();

		if (_fsg == null) {
			// No simulation group is loaded; disable all controls
			setEnabled(false);
		}
	}

	/**
	 * Removes the given {@link BcData} item from the supplied {@link ForecastSimGroup}'s
	 * internal BC data list.
	 *
	 * @param fsg  the {@link ForecastSimGroup} from which the BC data is removed
	 * @param data the {@link BcData} item to remove
	 */
	@Override
	protected void removeData(ForecastSimGroup fsg, BcData data) {
		fsg.removeBcData(data);
	}

	/**
	 * Handles a click on the "Delete..." popup menu item for the given row in the BC table.
	 *
	 * Retrieves the {@link BcData} item at the specified row and delegates to
	 * {@link #delete(BcData, boolean)} with {@code deleteDueToOverwrite} set to
	 * {@code false} (direct user delete action). Does nothing if the simulation group
	 * is null or the cell value is not a {@link BcData} instance.
	 *
	 * @param rowToDelete the zero-based index of the row whose delete item was clicked
	 */
	@Override
	public void tableRowDeleteClicked(int rowToDelete) {
		Object value = _bcTable.getValueAt(rowToDelete, 0);

		if (_fsg != null && value instanceof BcData) {
			// Initiate the delete workflow for the BC set at the clicked row
			delete((BcData) value, false);
		}
	}
}
