package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Component;                                                  // Provides the base Component class used for Swing UI hierarchy traversal
import java.awt.GridBagConstraints;                                         // Provides GridBagConstraints for controlling component layout within a GridBagLayout
import java.awt.GridBagLayout;                                              // Provides GridBagLayout, a flexible layout manager using a grid of cells
import java.awt.Rectangle;                                                  // Provides Rectangle for representing cell bounds during table scroll operations

import java.io.BufferedReader;                                              // Provides BufferedReader for efficient line-by-line reading of text files
import java.io.IOException;                                                 // Provides IOException for handling general input/output errors
import java.nio.file.Path;                                                  // Provides Path for representing file system paths in a platform-independent way
import java.nio.file.Paths;                                                 // Provides Paths for constructing Path instances from string segments

import java.text.ParseException;                                            // Provides ParseException for signaling failures when parsing date strings
import java.util.ArrayList;                                                 // Provides ArrayList, a resizable-array implementation of the List interface
import java.util.Collection;                                                // Provides the Collection interface, the root of the Java collections hierarchy
import java.util.Collections;                                               // Provides Collections for static utility methods such as singletonList
import java.util.HashMap;                                                   // Provides HashMap for storing key-value pairs with O(1) average access time
import java.util.Iterator;                                                  // Provides Iterator for traversing elements in a collection
import java.util.List;                                                      // Provides the List interface for ordered sequences of elements
import java.util.Map;                                                       // Provides the Map interface for key-to-value mappings
import java.util.Map.Entry;                                                 // Provides Map.Entry for iterating over key-value pairs in a Map
import java.util.Set;                                                       // Provides the Set interface for collections of unique elements
import java.util.TreeSet;                                                   // Provides TreeSet, a sorted set backed by a TreeMap for ordered profile storage
import java.util.Vector;                                                    // Provides Vector, a synchronized resizable array used for building table rows
import java.util.regex.Matcher;                                             // Provides Matcher for applying a compiled regex Pattern to an input string
import java.util.regex.Pattern;                                             // Provides Pattern for compiling and reusing regular expressions

import javax.swing.FocusManager;                                            // Provides FocusManager for querying which Swing component currently has keyboard focus
import javax.swing.JButton;                                                 // Provides JButton for creating clickable button components in the Swing UI
import javax.swing.JOptionPane;                                             // Provides JOptionPane for displaying standard modal dialog boxes
import javax.swing.JPanel;                                                  // Provides JPanel, a generic lightweight container for grouping Swing components
import javax.swing.SwingUtilities;                                          // Provides SwingUtilities for thread-safe utilities and component hierarchy traversal
import javax.swing.event.TableModelEvent;                                   // Provides TableModelEvent for notifying listeners of changes to a table's data model
import javax.swing.table.TableColumnModel;                                  // Provides TableColumnModel for managing the ordered set of columns in a JTable

import com.google.common.flogger.FluentLogger;                              // Provides FluentLogger, a structured logging API from the Flogger library
import com.rma.io.DssFileManagerImpl;                                       // Provides DssFileManagerImpl for reading and writing HEC-DSS binary data files
import com.rma.io.FileManagerImpl;                                          // Provides FileManagerImpl for obtaining managed file references within the RMA framework
import com.rma.io.RmaFile;                                                  // Provides RmaFile, an abstraction over a file resource managed by the RMA framework
import com.rma.model.Project;                                               // Provides Project for accessing the current project's metadata and directory structure

import hec.data.DataSetIllegalArgumentException;                            // Provides DataSetIllegalArgumentException for invalid arguments in HEC data operations
import hec.data.Parameter;                                                  // Provides Parameter for accessing HEC parameter identifiers and unit string lookups
import hec.data.Units;                                                      // Provides Units for unit-system constants (SI, English) and unit conversion logic
import hec.data.UnitsConversionException;                                   // Provides UnitsConversionException for errors during unit conversion operations
import hec.geometry.Axis;                                                   // Provides Axis for controlling axis properties (min, max, reversed) in a G2d plot
import hec.gfx2d.G2dData;                                                   // Provides G2dData for global display-unit settings used during plot construction
import hec.gfx2d.G2dPanel;                                                  // Provides G2dPanel, the HEC 2D graphics panel used to render temperature-depth plots
import hec.gfx2d.PairedDataSet;                                             // Provides PairedDataSet for wrapping a PairedDataContainer for display in G2dPanel
import hec.gfx2d.Viewport;                                                  // Provides Viewport for accessing and configuring individual plot axes within a G2dPanel
import hec.heclib.dss.DSSPathname;                                          // Provides DSSPathname for constructing and parsing HEC-DSS path strings (A-F parts)
import hec.heclib.util.Unit;                                                // Provides Unit constants (e.g., SI_ID) for use in unit-system comparisons
import hec.io.PairedDataContainer;                                          // Provides PairedDataContainer, the data structure for storing paired (X,Y) HEC data

import rma.swing.EnabledJPanel;                                             // Provides EnabledJPanel, a JPanel that supports enable/disable propagation to children
import rma.swing.RmaInsets;                                                 // Provides RmaInsets constants for consistent padding/insets across the RMA UI
import rma.swing.RmaJTable;                                                 // Provides RmaJTable, an RMA-extended JTable with convenience methods for row management
import rma.swing.table.ColumnGroup;                                         // Provides ColumnGroup for grouping table columns under a shared header label
import rma.swing.table.GroupableTableHeader;                                // Provides GroupableTableHeader for rendering multi-level column group headers in a table
import rma.swing.table.RmaTableModel;                                       // Provides RmaTableModel, the data model backing RmaJTable instances
import rma.util.RMAIO;                                                      // Provides RMAIO for file path utilities and string-to-double parsing helpers

import usbr.wat.plugins.actionpanel.actions.ReviewDataAction;               // Provides ReviewDataAction, the Swing Action for triggering a data review workflow
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;               // Provides UpdateDataAction, the Swing Action for triggering a data update workflow
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;     // Provides ForecastConfigFiles for resolving config file paths relative to the project
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Provides ForecastSimGroup, the top-level model grouping forecast simulations together
import usbr.wat.plugins.actionpanel.model.forecast.IcReservoirInfo;         // Provides IcReservoirInfo, a model object holding per-reservoir initial condition metadata
import usbr.wat.plugins.actionpanel.model.forecast.InitialConditions;       // Provides InitialConditions, the model object storing selected temperature-depth profiles
import usbr.wat.plugins.actionpanel.model.forecast.Profile;                 // Provides Profile, representing a single dated temperature-depth profile read from CSV

/**
 * Panel that displays and manages the Initial Conditions tab within the Forecast
 * Action Panel. It presents per-reservoir tables of dated temperature-depth profiles
 * (loaded from CSV files), renders the currently selected profile as a paired-data
 * plot, and persists the user's selection back to the ForecastSimGroup model.
 *
 * Each reservoir gets one RmaJTable (for profile selection) and one G2dPanel
 * (for the corresponding temperature-depth plot). Both are stored together in a
 * ResComponents helper object keyed by reservoir name.
 *
 * @see AbstractForecastPanel
 * @see InitialConditions
 * @see ForecastSimGroup
 */
public class InitialConditionsPanel extends AbstractForecastPanel<InitialConditions> {
	/**
	 * Logger instance scoped to this class for structured, levelled log output.
	 */
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	/**
	 * Relative path (within the project) to the CSV config file listing reservoir names and profile file paths.
	 */
	private static final String CONFIG_CSV_FILE = ForecastConfigFiles.getRelativeIcReservoirsFile();

	/**
	 * Regex pattern for extracting the unit string from a parenthesised label, e.g. "Temperature (C)" -> "C".
	 */
	private static final Pattern UNIT_PATTERN = Pattern.compile("\\((.*?)\\)");

	/**
	 * System property key for overriding the lower temperature axis limit (in Celsius).
	 */
	private static final String TEMP_PLOT_MIN_PROPERTY = "WTMP.Forecast.LowerTempPlotMLimit.Celsius";

	/**
	 * System property key for overriding the upper temperature axis limit (in Celsius).
	 */
	private static final String TEMP_PLOT_MAX_PROPERTY = "WTMP.Forecast.UpperTempPlotLimit.Celsius";

	/**
	 * Relative path under the project directory where output DSS files are written.
	 */
	private static final Path OUTPUT_DSS_FILE_RELATIVE_PATH = Paths.get("shared");

	/**
	 * Default lower temperature axis bound in Celsius, used when the system property is absent.
	 */
	private static final int DEFAULT_TEMP_PLOT_MIN_C = 0;

	/**
	 * Default upper temperature axis bound in Celsius, used when the system property is absent.
	 */
	private static final int DEFAULT_TEMP_PLOT_MAX_C = 30;

	/**
	 * Default temperature unit string for the SI unit system (Celsius).
	 */
	private static final String DEFAULT_TEMP_UNITS = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.SI_ID);

	/**
	 * Default depth unit string for the English unit system (feet).
	 */
	private static final String DEFAULT_DEPTH_UNITS = Parameter.getUnitsStringForSystem(Parameter.PARAMID_ELEV, Units.ENGLISH_ID);

	/**
	 * Panel that holds the per-reservoir selection tables and temperature-depth plots.
	 */
	private EnabledJPanel _plotsPanel;

	/**
	 * Panel that holds the Update Data and Review Data action buttons.
	 */
	private EnabledJPanel _buttonPanel;

	/**
	 * Action responsible for refreshing profile data from the upstream data source.
	 */
	private UpdateDataAction _updateDataAction;

	/**
	 * Action responsible for opening the data review workflow dialog.
	 */
	private ReviewDataAction _reviewDataAction;

	/**
	 * Map from reservoir name to its associated UI components (selection table and plot panel).
	 * Built fresh each time fillPanel is called.
	 */
	private Map<String, ResComponents> _resComponents = new HashMap<>();

	/**
	 * Guard flag used to prevent recursive TableModelEvent handling while the
	 * panel is programmatically modifying checkbox values.
	 */
	private boolean _ignoreTableModification = false;

	/**
	 * The ForecastSimGroup currently displayed by this panel; null when no simulation is selected.
	 */
	private ForecastSimGroup _fsg;

	/**
	 * Constructs a new InitialConditionsPanel and registers all required event listeners.
	 *
	 * @param forecastPanel the parent ForecastPanel that owns this tab
	 */
	public InitialConditionsPanel(ForecastPanel forecastPanel) {
		super(forecastPanel);

		// Register table and other listeners needed to detect user interactions
		addListeners();
	}


	/**
	 * Builds the lower section of the forecast panel layout by creating and arranging
	 * the plots panel (tables + plots) and the button panel side by side.
	 *
	 * @param lowerPanel the EnabledJPanel into which the lower section is constructed
	 */
	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel) {
		// Create the panel that will hold per-reservoir tables and temperature-depth plots
		_plotsPanel = new EnabledJPanel(new GridBagLayout());

		// Configure constraints for the plots panel: fills all available space on the left
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_plotsPanel, gbc);

		// Create the panel that will hold action buttons on the right side
		_buttonPanel = new EnabledJPanel(new GridBagLayout());

		// Configure constraints for the button panel: fixed width, does not expand horizontally
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_buttonPanel, gbc);

		// Populate the button panel with Update and Review action buttons
		buildButtonPanel(_buttonPanel);
	}

	/**
	 * Delete operation is not supported for Initial Conditions; always returns false.
	 *
	 * @param data                 the InitialConditions object that would be deleted
	 * @param deleteDueToOverwrite true if deletion was triggered by an overwrite operation
	 * @return false always, indicating deletion is not performed
	 */
	@Override
	protected boolean delete(InitialConditions data, boolean deleteDueToOverwrite) {
		return false;
	}

	/**
	 * Rebuilds the plots panel from scratch, creating one RmaJTable and one G2dPanel
	 * for each reservoir defined in the IC config CSV. Tables and plots are added to
	 * the panel in two separate passes so that all tables appear above all plots.
	 */
	private void buildPlotsPanel() {
		// Remove any previously built reservoir components before rebuilding
		_plotsPanel.removeAll();

		List<IcReservoirInfo> reservoirInfos = getReservoirInfo();

		// First pass: create and add a selection table for each reservoir
		IcReservoirInfo resInfo;
		for (int i = 0; i < reservoirInfos.size(); i++) {
			resInfo = reservoirInfos.get(i);

			// Create a custom RmaJTable where only the checkbox (col 0) is editable
			RmaJTable table = new RmaJTable(this, new String[]{"Select", "Date"}) {
				@Override
				public boolean isCellEditable(int row, int col) {
					// Only the "Select" column is editable, and only when the panel is enabled
					return col == 0 && InitialConditionsPanel.this.isEnabled();
				}
			};

			// Increase the default row height for better readability
			table.setRowHeight(table.getRowHeight() + 5);
			table.setName(resInfo.getReservoirName());

			// Replace the default table header with a groupable header for multi-level column labels
			table.setTableHeader(new GroupableTableHeader(table.getColumnModel()));
			TableColumnModel cm = table.getColumnModel();

			// Group both columns under the reservoir name to show it as a spanning header
			ColumnGroup columnGroup = new ColumnGroup(resInfo.getReservoirName());
			columnGroup.add(cm.getColumn(0));
			columnGroup.add(cm.getColumn(1));

			// Install a checkbox cell editor in the "Select" column (index 0)
			table.setCheckBoxCellEditor(0);

			GroupableTableHeader header = (GroupableTableHeader) table.getTableHeader();
			header.addColumnGroup(columnGroup);

			// Determine GridBagConstraints width: last column spans the remainder
			GridBagConstraints gbc = new GridBagConstraints();
			int width = (i == reservoirInfos.size() - 1 ? GridBagConstraints.REMAINDER : 1);
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.gridwidth = width;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = RmaInsets.INSETS5505;

			// Add the table's scroll pane (not the raw table) to the plots panel
			_plotsPanel.add(table.getScrollPane(), gbc);

			// Populate the table rows with profiles read from the reservoir's CSV files
			fillTable(table, resInfo);

			// Listen for checkbox changes to trigger plot updates and model saves
			table.getModel().addTableModelListener(this::tableModelChanged);

			// Store the table in the components map; plot panel will be added in the second pass
			_resComponents.put(resInfo.getReservoirName(), new ResComponents(table, null));
		}

		// Second pass: create and add a G2dPanel plot beneath each reservoir's table
		for (int i = 0; i < reservoirInfos.size(); i++) {
			resInfo = reservoirInfos.get(i);
			int width = (i == reservoirInfos.size() - 1 ? GridBagConstraints.REMAINDER : 1);

			G2dPanel plotPanel = new G2dPanel();

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.gridwidth = width;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = RmaInsets.INSETS5505;
			_plotsPanel.add(plotPanel, gbc);

			// Associate the newly created plot panel with its reservoir's ResComponents entry
			ResComponents comps = _resComponents.get(resInfo.getReservoirName());
			comps.plotPanel = plotPanel;
		}

		// Refresh the layout to reflect the newly added components
		revalidate();
	}

	/**
	 * Responds to changes in any table model and enforces single-row selection behavior.
	 * Programmatic updates and non-UPDATE events are ignored to prevent infinite recursion
	 * and unnecessary processing. When a user checks a row, all other rows in the same
	 * table are cleared before the checked row is re-applied, ensuring only one row can
	 * be selected at a time. After handling the selection change, the panel state is saved,
	 * the summary table is refreshed, and the plot is rebuilt for the affected table.
	 *
	 * @param e the TableModelEvent describing the change that occurred in the table model
	 */
	private void tableModelChanged(TableModelEvent e) {
		// Ignore programmatic updates and non-UPDATE events (e.g. structural changes)
		if (_ignoreTableModification || e.getType() != TableModelEvent.UPDATE) {
			return;
		}

		// Mark the panel as modified since the user has changed a table value
		setModified(true);

		// Retrieve the table model that fired the event
		RmaTableModel tableModel = (RmaTableModel) e.getSource();

		// Identify which RmaJTable triggered the event by walking up the focus-owner's hierarchy
		Component focusedComp = FocusManager.getCurrentManager().getFocusOwner();

		// Traverse the component hierarchy from the focused component to find the enclosing RmaJTable
		RmaJTable table = (RmaJTable) SwingUtilities.getAncestorOfClass(RmaJTable.class, focusedComp);

		if (table != null) {
			// Retrieve the row index of the cell that was modified
			int row = e.getFirstRow();

			// Read the current value of the checkbox column for the modified row
			Object checkedObj = tableModel.getValueAt(row, 0);

			// Enforce single-selection: if the user just checked a row, clear all other rows first
			if (checkedObj instanceof Boolean && ((Boolean) checkedObj)) {
				// Suppress recursive events while we programmatically clear other checkboxes
				_ignoreTableModification = true;

				// Clear all other checked rows in the table before re-applying the user's selection
				clearTableSelection(table);

				// Re-apply the user's checked state to the target row after clearing others
				tableModel.setValueAt(true, row, 0);

				// Re-enable table modification events now that the programmatic update is complete
				_ignoreTableModification = false;
			}

			// Persist the new selection to the model, update the summary table, and redraw the plot
			savePanel();
			fillUpperInitialConditionsTable();
			buildTablePlot(table);
		}
	}

	/**
	 * Builds or clears the temperature-depth plot for the given reservoir table.
	 * Iterates over all rows, collects checked profiles, and passes their
	 * PairedDataContainers to the associated G2dPanel. Axis limits are then
	 * applied via fixZoom.
	 *
	 * @param table the RmaJTable whose name identifies the reservoir and whose
	 *              row data provides the profiles to plot
	 */
	private void buildTablePlot(RmaJTable table) {
		if (table != null) {
			// Temporarily disable display-unit conversion so raw SI values are plotted
			boolean useDisplayUnits = G2dData.useDisplayUnits();
			G2dData.setUseDisplayUnits(false);

			int rows = table.getRowCount();
			Object obj;
			Profile profile;

			// Collect PairedDataSets for every row that has its checkbox set to true
			List<PairedDataSet> pdcsToPlot = new ArrayList<>();
			for (int r = 0; r < rows; r++) {
				obj = table.getValueAt(r, 0);
				if ("true".equalsIgnoreCase(obj.toString()) || obj == Boolean.TRUE) {
					profile = (Profile) table.getValueAt(r, 1);
					pdcsToPlot.add(new PairedDataSet(profile.getPdc()));
				}
			}

			// Retrieve the plot panel associated with this reservoir
			String resName = table.getName();
			ResComponents comps = _resComponents.get(resName);

			if (pdcsToPlot.isEmpty()) {
				// No profile selected: clear the plot area entirely
				comps.plotPanel.clearPanel();

			} else {
				// Build the plot with the selected profiles; disable legend and title
				comps.plotPanel.buildComponents(pdcsToPlot, false, false);

				// Ensure the Y-axis is not reversed (depth increases downward by convention)
				Viewport[] viewports = comps.plotPanel.getViewports();
				if (viewports != null && viewports.length > 0) {
					viewports[0].getAxis("Y1").setReversed(false);
				}
			}

			// Trigger a layout refresh and then apply temperature axis zoom limits
			revalidate();
			fixZoom(comps, pdcsToPlot);

			// Restore the original display-unit setting
			G2dData.setUseDisplayUnits(useDisplayUnits);
		}
	}

	/**
	 * Adjusts the X-axis (temperature) limits and tick interval of the plot after
	 * components have been built. Converts the configured Celsius limits to the
	 * units actually used by the plotted data before applying them.
	 *
	 * @param comps      the ResComponents whose plotPanel will be adjusted
	 * @param pdcsToPlot the list of PairedDataSets that were just rendered; used
	 *                   to determine the X-axis unit label
	 */
	private void fixZoom(ResComponents comps, List<PairedDataSet> pdcsToPlot) {
		// Get the viewports that are active
		Viewport[] viewports = comps.plotPanel.getViewports();

		// Take action if a viewport exists
		if (viewports != null && viewports.length > 0) {
			// Keep the Y-axis (depth) in normal ascending order
			viewports[0].getAxis("Y1").setReversed(false);

			Axis xaxis = viewports[0].getAxis("x1");

			// Extract the unit string from the axis label, e.g. "Temperature (C)" -> "C"
			String xUnit = extractContentWithinParentheses(pdcsToPlot.get(0).getXAxisName());

			try {
				// Convert the Celsius-based config limits to whatever unit the axis uses
				int min = getLowerTempPlotLimit(xUnit);
				int max = getUpperTempPlotLimit(xUnit);

				// Apply min/max limits and a 5-degree major tick interval to the X-axis
				xaxis.setMinimumLimit(min);
				xaxis.setMaximumLimit(max);
				xaxis.setViewLimits(min, max);
				xaxis.setMajorTicInterval(5);

				comps.plotPanel.setVisible(true);
				comps.plotPanel.repaint();

			} catch (DataSetIllegalArgumentException | UnitsConversionException e) {
				LOGGER.atConfig().withCause(e).log("Failed to determine units from label " + pdcsToPlot.get(0).getXAxisName());
			}
		}
	}

	/**
	 * Extracts the text enclosed in the first pair of parentheses found in the
	 * given input string. Used to parse unit labels such as "Temperature (C)".
	 *
	 * @param input the string to search, typically an axis label
	 * @return the content between the first '(' and ')', or null if not found
	 */
	private String extractContentWithinParentheses(String input) {
		String retVal = null;

		// Apply the pre-compiled parenthesis pattern to find the first match
		Matcher matcher = UNIT_PATTERN.matcher(input);
		if (matcher.find()) {
			// Group 1 captures everything between the parentheses
			retVal = matcher.group(1);
		}

		return retVal;
	}

	/**
	 * Populates the given RmaJTable with profile rows read from all CSV files
	 * associated with the specified reservoir. Each row contains a Boolean FALSE
	 * checkbox and the Profile object representing a dated temperature-depth profile.
	 *
	 * @param table   the RmaJTable to populate with profile rows
	 * @param resInfo the IcReservoirInfo providing the list of profile CSV file paths
	 */
	private void fillTable(RmaJTable table, IcReservoirInfo resInfo) {
		// Get the project and filenames
		Project prj = Project.getCurrentProject();
		List<String> profileFileNames = resInfo.getProfileFileNames();

		// Define placeholder values
		String fileName;
		Vector row;

		// Clear any previously loaded rows before repopulating
		table.deleteCells();

		for (int i = 0; i < profileFileNames.size(); i++) {
			fileName = profileFileNames.get(i);

			// Resolve the relative path to an absolute path within the current project
			fileName = prj.getAbsolutePath(fileName);

			// Read all dated profiles from this CSV file
			Set<Profile> profiles = readProfileFile(fileName, resInfo);

			for (Profile profile : profiles) {
				// Build a two-column row: unchecked Boolean + the Profile object
				row = new Vector();
				row.add(Boolean.FALSE);
				row.add(profile);
				table.appendRow(row);
			}
		}
	}

	/**
	 * Reads temperature-depth profiles from a CSV file (or a year-stamped variant of it)
	 * and returns them as an ordered set of Profile objects. Each profile corresponds to
	 * a unique date found in the file's first column. Profile data is also written to a
	 * companion DSS file in the project's "shared" directory.
	 *
	 * The method searches backwards from the simulation start year for up to 100 years
	 * to locate a year-stamped file (e.g. "profiles-2023.csv"). If none is found, the
	 * base file name is used as a fallback.
	 *
	 * @param baseFileName the base CSV file path (without year suffix)
	 * @param resInfo      the IcReservoirInfo providing the reservoir name used in DSS pathnames
	 * @return a sorted Set of Profile objects, one per unique date in the file(s) read
	 */
	private Set<Profile> readProfileFile(String baseFileName, IcReservoirInfo resInfo) {
		Set<Profile> profiles = new TreeSet<>();

		if (_fsg != null && _fsg.getAnalysisPeriod() != null) {
			List<String> fileNames = new ArrayList<>();

			// Strip the file extension to construct year-stamped variants (e.g. "file-2023.csv")
			int dotIndex = baseFileName.lastIndexOf(".");
			String baseFileNameNoExtension = baseFileName.substring(0, dotIndex);

			// Determine the simulation start year as the upper bound for the year search
			int yearStart = _fsg.getAnalysisPeriod().getRunTimeWindow().getStartTime().getLocalDateTime().getYear();

			// Search backwards up to 100 years for the most recent matching year-stamped file
			for (int year = yearStart; year >= yearStart - 100; year--) {
				boolean foundData = false;

				// Check for a file stamped with the current search year
				if (Paths.get(baseFileNameNoExtension + "-" + year + ".csv").toFile().exists()) {
					fileNames.add(baseFileNameNoExtension + "-" + year + ".csv");
					foundData = true;
				}

				// Also include the prior-year file if it exists (warm-up / spin-up data)
				if (Paths.get(baseFileNameNoExtension + "-" + (year - 1) + ".csv").toFile().exists()) {
					fileNames.add(baseFileNameNoExtension + "-" + (year - 1) + ".csv");
				}

				// Stop searching once we have found at least the primary year file
				if (foundData) {
					break;
				}
			}

			// Fall back to the un-suffixed base file name when no year-stamped file was found
			if (fileNames.isEmpty()) {
				fileNames.add(baseFileName);
			}

			for (String fileName : fileNames) {
				RmaFile file = FileManagerImpl.getFileManager().getFile(fileName);
				if (file == null) {
					LOGGER.atInfo().log("Failed to find file:" + fileName);
					return profiles;
				}

				BufferedReader reader = file.getBufferedReader();
				if (reader == null) {
					LOGGER.atInfo().log("Failed to get Reader for file:" + fileName);
					return profiles;
				}

				// Per-line parsing state variables
				String line;
				PairedDataContainer pdc;
				String currDate = null;
				String date = null;
				String[] parts;
				DSSPathname pathname = new DSSPathname();
				Profile profile = null;
				List<String> temps = new ArrayList<>();
				List<String> depths = new ArrayList<>();

				try {
					// Discard the CSV header row
					reader.readLine();

					while ((line = reader.readLine()) != null) {
						parts = line.split(",");

						// Skip malformed lines that do not have exactly three columns
						if (parts.length != 3) {
							continue;
						}

						date = parts[0];

						// A new date string signals the start of a new profile block
						if (!date.equals(currDate)) {
							// Finalise and write the completed previous profile before starting a new one
							if (profile != null) {
								fillInProfilePdc(profile, temps, depths);
								writeProfileToDss(profile);
							}

							// Reset accumulators for the new profile
							currDate = date;
							temps.clear();
							depths.clear();

							// Create a new PairedDataContainer and associate it with the output DSS file
							pdc = new PairedDataContainer();
							String pdcFileNameWithExtension = Paths.get(fileName).getFileName().toString();
							String pdcFileName = pdcFileNameWithExtension.substring(0, pdcFileNameWithExtension.lastIndexOf("."));
							pdc.fileName = Project.getCurrentProject().getAbsolutePath(OUTPUT_DSS_FILE_RELATIVE_PATH.resolve(pdcFileName + ".dss").toString());

							// Strip any time component from the date, keeping only the date portion
							int idx = date.indexOf(' ');
							if (idx > -1) {
								date = date.substring(0, idx);
							}

							// Reuse an existing Profile object if one was already selected for this date
							Profile existingProfile = _fsg.getInitialConditions().getSelectedProfile(resInfo.getReservoirName());
							if (existingProfile != null && existingProfile.getName().equalsIgnoreCase(date)) {
								profile = existingProfile;
							} else {
								profile = new Profile(date);
							}

							profiles.add(profile);

							// Build the DSS pathname: B=reservoir, C=DEPTH-TEMP, E=date
							pathname.setBPart(resInfo.getReservoirName());
							String depthParam = Parameter.getParameter(Parameter.PARAMID_DEPTH).getParameter();
							String tempParam = Parameter.getParameter(Parameter.PARAMID_TEMP).getParameter();
							pathname.setCPart(depthParam.toUpperCase() + "-" + tempParam.toUpperCase());
							pathname.setEPart(date);
							pdc.fullName = pathname.getPathname();
							profile.setPdc(pdc);
						}

						// Accumulate temperature and depth values for the current profile
						temps.add(parts[1]);
						depths.add(parts[2]);
					}

					// Finalise and write the last profile in the file
					if (profile != null) {
						fillInProfilePdc(profile, temps, depths);
						writeProfileToDss(profile);
					}
				} catch (IOException | DataSetIllegalArgumentException e) {
					LOGGER.atWarning().withCause(e).log("Failed to read profiles file " + fileName);

				} catch (ParseException e) {
					// Show a modal error and abort reading the current file on a date parse failure
					String msg = "Failed to parse date " + date + "\nin file " + fileName;
					JOptionPane.showMessageDialog(_plotsPanel, msg, "Failed to load initial conditions",
							JOptionPane.ERROR_MESSAGE);
					LOGGER.atWarning().withCause(e).log(msg);
					break;

				} finally {
					// Ensure the reader is closed even if an exception was thrown
					try {
						reader.close();

					} catch (IOException e) {
					}
				}
			}
		}
		return profiles;
	}

	/**
	 * Writes the PairedDataContainer belonging to the given profile to its associated
	 * DSS file. Logs a warning if the write operation returns a non-zero error code.
	 *
	 * @param profile the Profile whose PairedDataContainer is written to DSS
	 */
	private void writeProfileToDss(Profile profile) {
		PairedDataContainer pdc = profile.getPdc();

		// Attempt to write the paired data; a non-zero return value indicates failure
		int success = DssFileManagerImpl.getDssFileManager().write(pdc);
		if (success != 0) {
			LOGGER.atWarning().log("Failed to write " + pdc.fullName + " to " + pdc.fileName + ".  Error code: " + success);
		}
	}

	/**
	 * Populates a Profile's PairedDataContainer with the temperature and depth arrays
	 * accumulated while parsing a CSV profile block. Units are hardcoded to Celsius
	 * and feet respectively; a future enhancement should read units from the CSV header.
	 *
	 * @param profile    the Profile whose PairedDataContainer is filled
	 * @param tempsList  list of raw temperature strings parsed from the CSV
	 * @param depthsList list of raw depth strings parsed from the CSV
	 */
	private void fillInProfilePdc(Profile profile, List<String> tempsList,
	                              List<String> depthsList) {
		try {
			// Configure the container: single curve, number of ordinates equals the depth count
			profile.getPdc().setNumberCurves(1);
			profile.getPdc().setNumberOrdinates(depthsList.size());

			// Units are always C (temperature) and ft (depth) until the CSV provides them
			profile.getPdc().xunits = DEFAULT_DEPTH_UNITS;
			profile.getPdc().xparameter = Parameter.getParameter(Parameter.PARAMID_DEPTH).getParameter();
			profile.getPdc().yparameter = Parameter.getParameter(Parameter.PARAMID_TEMP).getParameter();
			profile.getPdc().yunits = DEFAULT_TEMP_UNITS;

			// Convert the raw string lists to primitive double arrays
			double[] temps = toDoubleArray(tempsList);
			double[] depths = toDoubleArray(depthsList);

			// PairedDataContainer requires a 2-D array for Y values; wrap the 1-D temps array
			double[][] temps2 = new double[1][0];
			temps2[0] = temps;

			profile.getPdc().setValues(depths, temps2);

			// Swap X and Y axes so depth is plotted on the horizontal axis as expected
			profile.getPdc().switchXyAxis = true;
		} catch (DataSetIllegalArgumentException e) {
			LOGGER.atWarning().withCause(e).log("Failed to get Parameter");
		}
	}

	/**
	 * Returns the lower temperature axis limit in the given unit system, reading the
	 * value from a system property and converting from Celsius if necessary.
	 *
	 * @param units the target unit string (e.g. "C" or "F") for the returned value
	 * @return the lower temperature plot limit converted to the specified units
	 * @throws DataSetIllegalArgumentException if the TEMP parameter cannot be retrieved
	 * @throws UnitsConversionException        if the unit conversion fails
	 */
	private int getLowerTempPlotLimit(String units) throws DataSetIllegalArgumentException, UnitsConversionException {
		// Read the configured minimum from the system property, defaulting to 0 C
		int min = Integer.getInteger(TEMP_PLOT_MIN_PROPERTY, DEFAULT_TEMP_PLOT_MIN_C);

		// Determine the SI temperature unit string for use as the conversion source unit
		String siTempUnits = Parameter.getParameter(Parameter.PARAMID_TEMP).getUnitsStringForSystem(Unit.SI_ID);

		// Convert from Celsius to the axis display unit
		min = (int) Units.convertUnits(min, siTempUnits, units);
		return min;
	}

	/**
	 * Returns the upper temperature axis limit in the given unit system, reading the
	 * value from a system property and converting from Celsius if necessary.
	 *
	 * @param units the target unit string (e.g. "C" or "F") for the returned value
	 * @return the upper temperature plot limit converted to the specified units
	 * @throws DataSetIllegalArgumentException if the TEMP parameter cannot be retrieved
	 * @throws UnitsConversionException        if the unit conversion fails
	 */
	private int getUpperTempPlotLimit(String units) throws DataSetIllegalArgumentException, UnitsConversionException {
		// Read the configured maximum from the system property, defaulting to 30 C
		int max = Integer.getInteger(TEMP_PLOT_MAX_PROPERTY, DEFAULT_TEMP_PLOT_MAX_C);

		// Determine the SI temperature unit string for use as the conversion source unit
		String siTempUnits = Parameter.getParameter(Parameter.PARAMID_TEMP).getUnitsStringForSystem(Unit.SI_ID);

		// Convert from Celsius to the axis display unit
		max = (int) Units.convertUnits(max, siTempUnits, units);
		return max;
	}

	/**
	 * Converts a list of numeric string values to a primitive double array,
	 * using the RMA framework's locale-aware double parser.
	 *
	 * @param valuesList the list of string values to convert
	 * @return a double array containing the parsed values in list order
	 */
	private double[] toDoubleArray(List<String> valuesList) {
		double[] values = new double[valuesList.size()];

		for (int i = 0; i < valuesList.size(); i++) {
			// Use RMAIO.parseDouble to handle locale-specific decimal separators
			values[i] = RMAIO.parseDouble(valuesList.get(i));
		}
		return values;
	}

	/**
	 * Registers event listeners required by this panel. Currently delegates to
	 * the superclass to attach listeners to the upper summary table.
	 */
	@Override
	protected void addListeners() {
		addUpperTableListeners();
	}

	/**
	 * Clears all visual content from the plots panel and forces a layout refresh.
	 * Called when the panel needs to be reset (e.g., when the simulation group changes).
	 */
	@Override
	protected void clearPanel() {
		_plotsPanel.removeAll();
		_plotsPanel.revalidate();
		_plotsPanel.repaint();
	}

	/**
	 * Removal of initial condition data from a ForecastSimGroup is not currently
	 * supported; this method intentionally does nothing.
	 *
	 * @param fsg  the ForecastSimGroup from which data would be removed
	 * @param data the InitialConditions object that would be removed
	 */
	@Override
	protected void removeData(ForecastSimGroup fsg, InitialConditions data) {
		//not currently supported
	}

	/**
	 * Reads the IC reservoir configuration CSV and returns a list of IcReservoirInfo
	 * objects, each describing one reservoir and its associated profile CSV file paths.
	 * Shows an error dialog if the config file cannot be found or read.
	 *
	 * @return a List of IcReservoirInfo parsed from the configuration CSV; empty on error
	 */
	private List<IcReservoirInfo> getReservoirInfo() {
		List<IcReservoirInfo> resInfos = new ArrayList<>();

		// Resolve the absolute path of the IC config CSV for the current project
		String configFile = getInitialConditionsConfigFile();
		if (configFile == null) {
			JOptionPane.showMessageDialog(this, "Failed to find file " + configFile + " to populate Initial Conditions File", "Missing File", JOptionPane.PLAIN_MESSAGE);
			return resInfos;
		}

		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);

		BufferedReader reader = file.getBufferedReader();
		if (reader == null) {
			JOptionPane.showMessageDialog(this, "Failed to read file " + configFile + " to populate Initial Conditions File", "Missing File", JOptionPane.PLAIN_MESSAGE);
			return resInfos;
		}

		// Define placeholder variables
		String line;
		IcReservoirInfo resInfo;

		try {
			// Each CSV line: reservoirName, profileFile1, profileFile2, ...
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				if (parts != null && parts.length > 0) {
					resInfo = new IcReservoirInfo();

					// The first column is the reservoir name
					resInfo.setReservoirName(parts[0]);

					// All subsequent columns are relative paths to profile CSV files
					if (parts.length > 1) {
						for (int i = 1; i < parts.length; i++) {
							resInfo.addProfileFileName(parts[i]);
						}
					}
					resInfos.add(resInfo);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {
			// Always close the reader to release the underlying file resource
			try {
				reader.close();

			} catch (IOException e) {
			}
		}

		return resInfos;
	}

	/**
	 * Constructs the absolute path to the Initial Conditions reservoir configuration
	 * CSV file for the currently open project. Returns null if no project is open.
	 *
	 * @return the absolute path string to the IC config CSV, or null if no project is active
	 */
	private String getInitialConditionsConfigFile() {
		Project prj = Project.getCurrentProject();

		// Return null early when no project is loaded so callers can guard appropriately
		if (prj.isNoProject()) {
			return null;
		}

		String dir = prj.getProjectDirectory();

		// Concatenate the project directory and the relative config file path
		String configFile = RMAIO.concatPath(dir, CONFIG_CSV_FILE);
		return configFile;
	}

	/**
	 * Populates the button panel with the Update Data and Review Data action buttons.
	 * The Update Data button is only added if the corresponding system flag is set.
	 *
	 * @param buttonPanel the JPanel into which the action buttons are added
	 */
	private void buildButtonPanel(JPanel buttonPanel) {
		// Create the Update Data action; its callback triggers a full panel refresh
		_updateDataAction = new UpdateDataAction(() -> fillPanel(_fsg));

		GridBagConstraints gbc = new GridBagConstraints();
		JButton button = new JButton(_updateDataAction);

		// Configure layout constraints: button spans the remainder of its row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;

		// Only show the Update Data button when the feature flag system property is enabled
		if (Boolean.getBoolean(UpdateDataAction.DASH_D_FLAG)) {
			buttonPanel.add(button, gbc);
		}

		// Create and add the Review Data button unconditionally
		_reviewDataAction = new ReviewDataAction();
		button = new JButton(_reviewDataAction);

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		buttonPanel.add(button, gbc);
	}

	/**
	 * Returns the upper summary table that displays the currently selected profile
	 * for each reservoir. Required by the AbstractForecastPanel contract.
	 *
	 * @return the ForecastTable used as the upper Initial Conditions summary table
	 */
	@Override
	public ForecastTable getTableForPanel() {
		return _initialConditionsTable;
	}

	/**
	 * Import is not applicable for Initial Conditions, which are read directly from
	 * the mapping CSV file. This method intentionally does nothing.
	 *
	 * @param dlg the ImportForecastWindow dialog (ignored)
	 */
	@Override
	protected void importForecastData(ImportForecastWindow dlg) {
		//no import button for initial conditions (read from mapping file)
	}

	/**
	 * Saves the current initial conditions selection back to the simulation group.
	 * Iterates over every reservoir component, finds the checked profile row in its
	 * table, and records the selected profile in a new InitialConditions object.
	 * Reservoirs with no checked row are skipped. The completed InitialConditions
	 * object is marked as modified and pushed back onto the simulation group.
	 * If no simulation group is available, the method returns without taking any action.
	 * This method overrides the base class implementation to provide initial-conditions-
	 * specific save behavior.
	 */
	@Override
	protected void savePanel() {
		// Retrieve the forecast simulation group that holds the initial conditions
		ForecastSimGroup simGrp = _forecastPanel.getSimulationGroup();

		// Only proceed if a valid simulation group is available
		if (simGrp != null) {
			// Create a fresh InitialConditions object to accumulate the current selections
			InitialConditions ics = new InitialConditions();

			// Iterate over every reservoir and record whichever profile row is checked
			Set<String> keySet = _resComponents.keySet();
			Iterator<String> keyIter = keySet.iterator();

			// Declare variables to hold the current reservoir name and its UI components
			String resName;
			ResComponents comp;

			// Iterate over each reservoir entry in the components map
			while (keyIter.hasNext()) {
				// Retrieve the next reservoir name and its associated UI components
				resName = keyIter.next();
				comp = _resComponents.get(resName);

				// Find the profile corresponding to the checked row in this reservoir's table
				Profile selectedProfile = findSelectedRow(comp.table);

				// Only record the selection if a checked row was found for this reservoir
				if (selectedProfile != null) {
					ics.putSelectedProfile(resName, selectedProfile);
				}
			}

			// Mark the model as modified and push it back onto the simulation group
			ics.setModified(true);
			simGrp.setInitialConditions(ics);
		}
	}

	/**
	 * Searches the given table for the first checked row and returns the Profile object
	 * stored in column 1 of that row. The checkbox state in column 0 is checked against
	 * both Boolean.TRUE and its string representation for robustness. If no checked row
	 * is found, null is returned.
	 *
	 * @param table the RmaJTable to search for a checked row
	 * @return      the Profile from column 1 of the first checked row, or null if no
	 *              row is checked
	 */
	private Profile findSelectedRow(RmaJTable table) {
		// Get the total number of rows currently displayed in the table
		int rowCnt = table.getRowCount();

		// Declare a variable to hold the checkbox cell value during each iteration
		Object obj;

		// Default to null; only set if a checked row is found
		Profile profile = null;

		// Iterate over every row in the table to find the first checked row
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the checkbox value from column 0 of the current row
			obj = table.getValueAt(r, 0);

			// Accept either Boolean.TRUE or the string "true" (defensive equality check)
			if (obj == Boolean.TRUE || "true".equalsIgnoreCase(obj.toString())) {
				// Retrieve the Profile object stored in column 1 of the checked row
				profile = (Profile) table.getValueAt(r, 1);

				// Stop searching after the first checked row is found
				break;
			}
		}

		// Return the matched Profile, or null if no checked row was found
		return profile;
	}

	/**
	 * Loads the panel for the given ForecastSimGroup: rebuilds the plots panel,
	 * restores previously selected profiles to the tables, redraws all plots,
	 * and refreshes the upper summary table.
	 *
	 * @param fsg the ForecastSimGroup whose InitialConditions should be displayed,
	 *            or null to disable the panel
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg) {
		// Disable the entire panel when no simulation group is provided
		setEnabled(fsg != null);
		clearTableSelections();

		if (fsg != null) {
			_fsg = fsg;

			// Rebuild tables and plot panels for all reservoirs in the new group
			buildPlotsPanel();

			InitialConditions ic = fsg.getInitialConditions();

			// Restore the previously selected profile checkbox for each reservoir
			Set<Entry<String, ResComponents>> entrySet = _resComponents.entrySet();
			Iterator<Entry<String, ResComponents>> iter = entrySet.iterator();
			Entry<String, ResComponents> entry;
			String resName;

			while (iter.hasNext()) {
				entry = iter.next();
				resName = entry.getKey();

				Profile selectedProfile = ic.getSelectedProfile(resName);

				// Set the checkbox and scroll to the matching row in each table
				fillTableSelections(entry.getValue().table, selectedProfile);

				// Redraw the temperature-depth plot for the restored selection
				buildTablePlot(entry.getValue().table);
			}

			// Refresh the upper summary table to reflect the loaded selections
			fillUpperInitialConditionsTable();
		}

		setModified(false);
	}

	/**
	 * Rebuilds the upper summary table to show each reservoir's currently selected
	 * profile. Only reservoirs that have a matching checked row in their selection
	 * table are included.
	 */
	private void fillUpperInitialConditionsTable() {
		InitialConditions initialConditions = _fsg.getInitialConditions();

		// Clear the existing summary rows before repopulating
		_initialConditionsTable.deleteCells();

		List<String> reservoirs = initialConditions.getReservoirs();

		for (String reservoir : reservoirs) {
			Profile profile = initialConditions.getSelectedProfile(reservoir);

			// Format the display value as "ReservoirName (ProfileName)"
			String displayValue = reservoir + " (" + profile + ")";

			// Only show the entry if the profile is actually checked in the UI table
			if (tableHasProfileSelected(reservoir, profile)) {
				_initialConditionsTable.appendRow(new Vector<>(Collections.singletonList(displayValue)));
			}
		}
	}

	/**
	 * Returns whether the specified profile is currently selected in the table for the
	 * given reservoir. A row is considered a match when its checkbox column (column 0)
	 * is checked and its date column (column 1) matches the profile name using a
	 * case-insensitive comparison. If the reservoir has no associated UI components or
	 * no matching checked row is found, false is returned.
	 *
	 * @param reservoir the name of the reservoir whose table will be searched
	 * @param profile   the Profile whose name will be matched against the date column
	 * @return          true if a checked row matching the profile name exists in the
	 *                  reservoir's table; false otherwise
	 */
	private boolean tableHasProfileSelected(String reservoir, Profile profile) {
		// Default to false; only set to true if a matching checked row is found
		boolean retVal = false;

		// Retrieve the UI components associated with the given reservoir
		ResComponents resComponent = _resComponents.get(reservoir);

		// Only search the table if valid UI components exist for this reservoir
		if (resComponent != null) {
			// Iterate over every row in the reservoir's table to find a matching selection
			for (int row = 0; row < resComponent.table.getRowCount(); row++) {
				// Retrieve the checkbox state from column 0 of the current row
				Object checked = resComponent.table.getValueAt(row, 0);

				// Retrieve the date value from column 1 of the current row
				Object date = resComponent.table.getValueAt(row, 1);

				// A row matches when its checkbox is true and its date equals the profile name
				if (checked != null && Boolean.parseBoolean(checked.toString())
						&& date != null && date.toString().equalsIgnoreCase(profile.getName())) {
					// Match found; mark the result and stop searching
					retVal = true;
					break;
				}
			}
		}

		// Return true if a matching checked row was found, false otherwise
		return retVal;
	}

	/**
	 * Clears the checkbox selection in every reservoir's table by iterating over all
	 * ResComponents in _resComponents and delegating to clearTableSelection() for each.
	 * This ensures that no row remains checked across any reservoir table after the call.
	 */
	private void clearTableSelections() {
		// Retrieve all ResComponents values from the reservoir components map
		Collection<ResComponents> comps = _resComponents.values();

		// Obtain an iterator to traverse each reservoir's UI components
		Iterator<ResComponents> iter = comps.iterator();

		// Declare a variable to hold the current ResComponents entry during iteration
		ResComponents comp;

		// Iterate over every reservoir component and clear its table selection
		while (iter.hasNext()) {
			// Retrieve the next reservoir's UI components
			comp = iter.next();

			// Clear all checked rows in this reservoir's table
			clearTableSelection(comp.table);
		}
	}

	/**
	 * Clears the checkbox selection in every row of the given table by setting column 0
	 * to Boolean.FALSE for each row. If the provided table is null, the method returns
	 * without taking any action.
	 *
	 * @param table the RmaJTable whose checkbox column will be cleared; no action is
	 *              taken if null
	 */
	private void clearTableSelection(RmaJTable table) {
		// Guard against a null table reference before attempting to modify any cells
		if (table != null) {
			// Get the total number of rows currently displayed in the table
			int rowCnt = table.getRowCount();

			// Iterate over every row and uncheck its checkbox column
			for (int r = 0; r < rowCnt; r++) {
				// Write Boolean.FALSE into the checkbox column for each row
				table.setValueAt(Boolean.FALSE, r, 0);
			}
		}
	}

	/**
	 * Restores a previously selected profile row in the given table by checking the row
	 * whose profile name matches the provided selectedProfile. The match is performed
	 * case-insensitively against the profile name stored in column 1. Once a match is
	 * found, the checkbox in column 0 is set to true and the row is scrolled into the
	 * visible viewport. If selectedProfile is null, the method returns without modifying
	 * the table.
	 *
	 * @param table           the RmaJTable in which to restore the selection
	 * @param selectedProfile the Profile whose name will be matched to find the row to
	 *                        check; no action is taken if null
	 */
	private void fillTableSelections(RmaJTable table, Profile selectedProfile) {
		// If no profile is selected, there is nothing to restore in the table
		if (selectedProfile == null) {
			return;
		}

		// Get the total number of rows currently displayed in the table
		int rowCnt = table.getRowCount();

		// Declare a variable to hold the profile retrieved from each row during iteration
		Profile profile;

		// Iterate over every row to find the one whose profile name matches the selected profile
		for (int r = 0; r < rowCnt; r++) {
			// Retrieve the Profile object stored in column 1 of the current row
			profile = (Profile) table.getValueAt(r, 1);

			// Match by name (case-insensitive) to find the row to restore
			if (selectedProfile.getName().equalsIgnoreCase(profile.getName())) {
				// Check the matching row and scroll it into the visible viewport
				table.setValueAt(Boolean.TRUE, r, 0);

				// Calculate the bounding rectangle of the checked cell for scrolling
				Rectangle cellRect = table.getCellRect(r, 0, true);

				// Scroll the table viewport to bring the newly checked row into view
				table.scrollRectToVisible(cellRect);

				// Stop searching after the first matching row has been restored
				break;
			}
		}
	}

	/**
	 * Simple data holder that associates a reservoir's selection table with its
	 * corresponding temperature-depth plot panel.
	 */
	private class ResComponents {
		/**
		 * The RmaJTable displaying available profile dates for this reservoir.
		 */
		RmaJTable table;

		/**
		 * The G2dPanel rendering the temperature-depth plot for this reservoir.
		 */
		G2dPanel plotPanel;

		/**
		 * Constructs a ResComponents pairing a table and a plot panel.
		 *
		 * @param t the RmaJTable for profile selection
		 * @param p the G2dPanel for profile visualisation
		 */
		ResComponents(RmaJTable t, G2dPanel p) {
			table = t;
			plotPanel = p;
		}
	}

	/**
	 * Called when a row in the upper summary table is selected. The Initial
	 * Conditions panel has no per-row selection behaviour, so this method
	 * intentionally does nothing.
	 *
	 * @param selectedRow the index of the selected row (unused)
	 */
	@Override
	protected void tableRowSelected(int selectedRow) {
		// no table for IC panel so nothing to do here
	}

	/**
	 * Called when the delete button is clicked for a row in the upper summary table.
	 * The Initial Conditions panel does not support row deletion, so this method
	 * intentionally does nothing.
	 *
	 * @param selectedRow the index of the row for which deletion was requested (unused)
	 */
	@Override
	public void tableRowDeleteClicked(int selectedRow) {
		//no table to delete from for IC panel
	}
}
