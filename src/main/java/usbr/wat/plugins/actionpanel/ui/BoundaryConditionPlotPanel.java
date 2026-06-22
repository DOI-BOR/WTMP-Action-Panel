package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.event.ItemEvent;     // Carries combo-box selection change data for the item listener

import java.io.BufferedReader;       // Wraps a FileReader with a buffer for efficient line-by-line reading
import java.io.FileReader;           // Opens a local file as a character stream for CSV parsing
import java.io.IOException;          // Signals an I/O failure during file reading

import java.nio.file.Path;           // Represents a filesystem path in a platform-independent way
import java.nio.file.Paths;          // Factory for constructing Path instances from string segments

import java.util.ArrayList;          // Resizable-array List implementation used for location pair and plot object lists
import java.util.List;               // Generic ordered collection interface

import java.util.logging.Level;      // Severity constants (CONFIG, INFO, WARNING, etc.) for log records
import java.util.logging.Logger;     // Java standard logging framework

import javax.swing.DefaultComboBoxModel; // Standard Swing combo-box model used when populating location pairs
import javax.swing.JLabel;               // Swing label for the "Location:" field label

import com.rma.io.DssFileManagerImpl;    // RMA DSS file manager for reading time-series records and closing DSS files
import com.rma.model.Project;            // Represents the currently open WAT study; resolves relative paths to absolute

import hec.geometry.Axis;               // Represents a plot axis, providing min/max and zoom operations
import hec.gfx2d.G2dObject;             // Base type for objects that can be added to a G2dPanel plot
import hec.gfx2d.G2dPanel;              // HEC 2D graphics panel that renders time-series plots
import hec.gfx2d.TimeSeriesDataSet;     // Wraps a TimeSeriesContainer as a plottable G2dObject
import hec.gfx2d.Viewport;             // Represents a view region within a G2dPanel, containing named axes
import hec.heclib.dss.DSSPathname;      // Parses and represents a HEC-DSS pathname (A/B/C/D/E/F parts)
import hec.io.DSSIdentifier;            // Combines a DSS file path and a pathname string for record retrieval
import hec.io.TimeSeriesContainer;      // Holds the values, dates, and units read from a DSS time-series record

import rma.swing.EnabledJPanel;          // RMA JPanel subclass with built-in enabled/disabled visual state support
import rma.swing.RmaInsets;              // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJComboBox;           // RMA-enhanced combo box with typed model support
import rma.swing.RmaNavigationPanel;     // RMA panel providing previous/next navigation buttons wired to a combo box

import usbr.wat.plugins.actionpanel.model.forecast.BcData;                       // Model object describing a boundary condition data pairing (ops + met)
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;             // Forecast simulation group providing the group name used in the pathname file path
import usbr.wat.plugins.actionpanel.ui.forecast.BoundaryConditionLocationPair;   // Value object pairing a location name, parameter, and DSS pathname


/**
 * Panel that displays a time-series plot of boundary condition data for a selected
 * location within a forecast simulation group.
 *
 * The panel consists of three vertically stacked sections:
 *   A "Location:" combo box paired with a navigation panel for stepping through locations.
 *   A G2dPanel that renders the time-series plot for the selected location.
 *
 * Data flow:
 *   fillPanel is called with a ForecastSimGroup and a BcData object. It builds the
 *   absolute path to a CSV pathname file that lists all boundary condition locations
 *   for the given group and data pairing, reads that file via
 *   readBoundaryConditionLocationPathPairs, and populates the combo box. Selecting
 *   an item triggers dssRecordComboSelected, which reads the corresponding time-series
 *   record from the output DSS file and passes it to updatePlot.
 *
 * Zoom preservation:
 *   When the user navigates to a different location the X-axis zoom is preserved so
 *   the time range stays consistent across locations. When the same location is
 *   reselected both X and Y axis zoom levels are preserved.
 *
 */
public class BoundaryConditionPlotPanel extends EnabledJPanel {
	/**
	 * Logger scoped to this class for recording file-read errors and diagnostics.
	 */
	private static final Logger LOGGER = Logger.getLogger(BoundaryConditionPlotPanel.class.getName());

	/**
	 * Combo box listing all boundary condition location/parameter pairs for the active BcData.
	 */
	private RmaJComboBox<BoundaryConditionLocationPair> _dssPathCombo;

	/**
	 * HEC 2D graphics panel that renders the time-series plot.
	 */
	private G2dPanel _plotPanel;

	/**
	 * The boundary condition data model currently displayed by this panel.
	 */
	private BcData _bcData;

	/**
	 * The forecast simulation group that owns the active boundary condition data.
	 */
	private ForecastSimGroup _fsg;

	/**
	 * The location pair that was most recently selected in the combo box.
	 * Retained so that zoom preservation strategy can be chosen when the plot is updated.
	 */
	private BoundaryConditionLocationPair _selectedLocationPair;


	/**
	 * Constructs the panel with a GridBagLayout, builds child components, and wires listeners.
	 */
	public BoundaryConditionPlotPanel() {
		super(new GridBagLayout());

		buildControls();
		addListeners();
	}


	/**
	 * Constructs and lays out the "Location:" label, location combo box, navigation
	 * panel, and plot panel using GridBagLayout.
	 *
	 * The combo box and plot panel expand horizontally to fill available space.
	 * The plot panel also expands vertically, taking all remaining vertical space.
	 */
	private void buildControls() {
		// --- "Location:" label ---
		JLabel label = new JLabel("Location:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// --- Location combo box (expands horizontally) ---
		_dssPathCombo = new RmaJComboBox<>();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_dssPathCombo, gbc);

		// --- Navigation panel (prev/next arrows wired to the combo box) ---
		RmaNavigationPanel navPanel = new RmaNavigationPanel();
		navPanel.fillForm(_dssPathCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(navPanel, gbc);

		// --- Plot panel (expands both horizontally and vertically to fill remaining space) ---
		_plotPanel = new G2dPanel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_plotPanel, gbc);
	}


	/**
	 * Registers an item listener on the location combo box so that selecting a new
	 * location triggers a DSS record read and plot update.
	 */
	private void addListeners() {
		_dssPathCombo.addItemListener(this::dssRecordComboSelected);
	}


	/**
	 * Responds to a location combo-box selection change by reading the corresponding
	 * DSS time-series record and updating the plot.
	 *
	 * The method is skipped when a DESELECTED event fires (i.e. only SELECTED events
	 * and programmatic null triggers cause a plot update). The DSS file is always
	 * closed in the finally block to release the file handle regardless of success.
	 *
	 * @param e the item event from the combo box; null when called programmatically
	 *          to force a refresh of the current selection
	 */
	private void dssRecordComboSelected(ItemEvent e) {
		// Skip deselect events and any calls when no boundary condition data is loaded
		if (_bcData != null && (e == null || ItemEvent.DESELECTED != e.getStateChange())) {
			Path dssFile = _bcData.getOutputDssFile();
			Object bcLocationPairObj = _dssPathCombo.getSelectedItem();

			if (dssFile != null && bcLocationPairObj instanceof BoundaryConditionLocationPair) {
				// Resolve the DSS file path to an absolute path within the current project
				Path dssFileAbsolutePath = Paths.get(
						Project.getCurrentProject().getAbsolutePath(dssFile.toString()));

				try {
					BoundaryConditionLocationPair bcLocationPair =  (BoundaryConditionLocationPair) bcLocationPairObj;
					DSSPathname dssPath = bcLocationPair.getDssPath();

					// Only attempt a read if the DSS file exists on disk and a pathname is available
					if (dssFileAbsolutePath.toFile().exists() && dssPath != null) {
						// Build a DSS identifier combining the file path and the record pathname
						DSSIdentifier dssIdentifier = new DSSIdentifier(dssFileAbsolutePath.toString(), dssPath.toString());

						// Read the time-series record; true = read values (not just header)
						TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, true);

						// Wrap the container as a plottable G2dObject
						TimeSeriesDataSet tsds = new TimeSeriesDataSet(tsc);
						updatePlot(tsds, bcLocationPair);
					}

					// Record the selected location pair so zoom strategy can be determined next time
					_selectedLocationPair = bcLocationPair;

				} finally {
					// Always close the DSS file to release the native file handle
					DssFileManagerImpl.getDssFileManager().close(dssFileAbsolutePath.toString());
				}
			}
		}
	}


	/**
	 * Decides which zoom-preservation strategy to use when refreshing the plot,
	 * then delegates to the appropriate update method.
	 *
	 * If viewports are already present in the plot panel the zoom levels from the
	 * previous render are carried over. Which axes are preserved depends on whether
	 * the user switched to a new location or reselected the same one:
	 * Same location -- both X and Y axis zoom are preserved.
	 * New location  -- only X axis zoom is preserved so the Y axis auto-scales.
	 *
	 * If no viewports exist yet the plot is built from scratch with default zoom.
	 *
	 * @param tsds           the time-series data set to render
	 * @param bcLocationPair the location pair that was just selected
	 */
	private void updatePlot(TimeSeriesDataSet tsds, BoundaryConditionLocationPair bcLocationPair) {
		Viewport[] viewports = _plotPanel.getViewports();

		if (viewports != null && viewports.length > 0) {
			if (bcLocationPair.equals(_selectedLocationPair)) {
				// Same location reselected: preserve both X and Y axis zoom
				updatePlotWithBothAxisZoomPreserved(tsds);

			} else {
				// New location selected: preserve only X axis zoom so Y auto-scales to new data
				updatePlotWithXAxisZoomPreserved(tsds);
			}

		} else {
			// No existing viewports: build the plot fresh with default zoom
			_plotPanel.setVisible(false);
			List<G2dObject> v = new ArrayList<>();
			v.add(tsds);
			_plotPanel.buildComponents(v);
			_plotPanel.setVisible(true);
		}
	}


	/**
	 * Rebuilds the plot with new data while restoring the previous X-axis zoom range.
	 *
	 * The current X-axis actual min and max are captured before the plot is rebuilt.
	 * After rebuilding, the captured range is clamped to the new data's full range so
	 * the zoom does not extend beyond the available data, then re-applied.
	 *
	 * @param tsds the new time-series data set to render
	 */
	private void updatePlotWithXAxisZoomPreserved(TimeSeriesDataSet tsds) {
		Viewport[] viewports = _plotPanel.getViewports();
		Axis xaxis = viewports[0].getAxis("x1");

		// Capture the current X-axis zoom extents before destroying the plot
		double actMin = xaxis.getActMin();
		double actMax = xaxis.getActMax();

		// Rebuild the plot with the new data set
		_plotPanel.setVisible(false);
		List<G2dObject> v = new ArrayList<>();
		v.add(tsds);
		_plotPanel.buildComponents(v);

		// Re-fetch axes from the rebuilt plot
		viewports = _plotPanel.getViewports();
		xaxis = viewports[0].getAxis("x1");

		// Clamp the captured zoom to the new data's full range to avoid out-of-bounds zoom
		double min = xaxis.getMin();
		double max = xaxis.getMax();
		if (actMin < min)
			actMin = min;
		if (actMax > max)
			actMax = max;

		xaxis.zoomIn(actMin, actMax);

		_plotPanel.setVisible(true);
		_plotPanel.repaint();
	}


	/**
	 * Rebuilds the plot with new data while restoring both the X and Y axis zoom ranges.
	 *
	 * Both axis actual min and max values are captured before the plot is rebuilt, then
	 * clamped to the new data's full range and re-applied after rebuilding.
	 *
	 * @param tsds the new time-series data set to render
	 */
	private void updatePlotWithBothAxisZoomPreserved(TimeSeriesDataSet tsds) {
		Viewport[] viewports = _plotPanel.getViewports();
		Axis xaxis = viewports[0].getAxis("x1");
		Axis yaxis = viewports[0].getAxis("y1");

		// Capture the current zoom extents for both axes before destroying the plot
		double actMin = xaxis.getActMin();
		double actMax = xaxis.getActMax();
		double actYMin = yaxis.getActMin();
		double actYMax = yaxis.getActMax();

		// Rebuild the plot with the new data set
		_plotPanel.setVisible(false);
		List<G2dObject> v = new ArrayList<>();
		v.add(tsds);
		_plotPanel.buildComponents(v);

		// Re-fetch axes from the rebuilt plot
		viewports = _plotPanel.getViewports();
		xaxis = viewports[0].getAxis("x1");
		yaxis = viewports[0].getAxis("y1");

		// Clamp the captured X zoom to the new data's full range
		double min = xaxis.getMin();
		double max = xaxis.getMax();
		if (actMin < min)
			actMin = min;
		if (actMax > max)
			actMax = max;
		xaxis.zoomIn(actMin, actMax);

		// Clamp the captured Y zoom to the new data's full range
		min = yaxis.getMin();
		max = yaxis.getMax();
		if (actYMin < min)
			actYMin = min;
		if (actYMax > max)
			actYMax = max;
		yaxis.zoomIn(actYMin, actYMax);

		_plotPanel.setVisible(true);
		_plotPanel.repaint();
	}


	/**
	 * Populates the panel with boundary condition data for the given forecast simulation
	 * group and triggers an immediate plot refresh.
	 *
	 * Stores the group and data references, enables or disables the panel based on
	 * whether bcData is non-null, reloads the location combo box, then programmatically
	 * fires a combo-box selection event to draw the initial plot.
	 *
	 * @param fsg    the forecast simulation group that owns the boundary condition data
	 * @param bcData the boundary condition data to display; pass null to clear the panel
	 */
	public void fillPanel(ForecastSimGroup fsg, BcData bcData) {
		_fsg = fsg;
		_bcData = bcData;

		// Disable all controls when no boundary condition data is available
		setEnabled(bcData != null);

		// Populate the combo box with the location pairs for this data set
		fillCombo(bcData);

		// Trigger an immediate plot update for the newly selected (or default) combo item
		dssRecordComboSelected(null);
	}


	/**
	 * Builds the location combo box model by reading boundary condition location pairs
	 * from the pathname text file associated with the given BcData.
	 *
	 * The pathname file path is constructed as:
	 * forecast/simGroups/{groupName}/{opsDataName}-{metDataName}.txt
	 * and resolved to an absolute path within the current project. If the file cannot
	 * be read a CONFIG-level log entry is written and the combo box is left empty.
	 *
	 * If a location was previously selected it is re-selected in the new model so the
	 * plot is not reset unnecessarily when the panel is refreshed.
	 *
	 * @param bcData the boundary condition data object providing the file name components;
	 *               if null the combo box is populated with an empty model
	 */
	private void fillCombo(BcData bcData) {
		// Create a new array list for the path names
		List<BoundaryConditionLocationPair> pathnames = new ArrayList<>();

		// Proces the boundary condition data if it is not null
		if (bcData != null) {
			// Build the relative path to the CSV pathname file for this group and data pairing
			String delim = "/";
			String pathnameDataFile = "forecast/simGroups/" + _fsg.getName() + delim
					+ bcData.getOpsDataName() + "-" + bcData.getMetDataName() + ".txt";

			// Resolve to an absolute path within the active project directory
			String pathnameDataFileAbs = Project.getCurrentProject().getAbsolutePath(pathnameDataFile);

			try {
				pathnames = readBoundaryConditionLocationPathPairs(pathnameDataFileAbs);

			} catch (IOException e) {
				// Log at CONFIG level since a missing pathname file is an expected configuration issue
				LOGGER.log(Level.CONFIG, e,
						() -> "Error reading pathnames in: " + pathnameDataFileAbs);
			}
		}

		// Build and install the new combo model from the parsed location pairs
		DefaultComboBoxModel<BoundaryConditionLocationPair> comboModel = new DefaultComboBoxModel<>();
		for (BoundaryConditionLocationPair pathname : pathnames) {
			comboModel.addElement(pathname);
		}
		_dssPathCombo.setModel(comboModel);

		// Restore the previous selection if it is still present in the new model
		if (_selectedLocationPair != null) {
			_dssPathCombo.setSelectedItem(_selectedLocationPair);
		}
	}


	/**
	 * Reads a CSV pathname file and returns a list of BoundaryConditionLocationPair objects.
	 *
	 * The file is expected to have a header row containing at minimum the columns
	 * "location", "parameter", and "dss path" (in any order and among any other columns).
	 * If any of these required columns are missing, an empty list is returned immediately.
	 * Data rows with fewer fields than the header are silently skipped.
	 *
	 * @param filePath absolute path to the CSV pathname file to read
	 * @return a list of BoundaryConditionLocationPair objects parsed from the file;
	 * never null but may be empty if the file is missing required columns
	 * @throws IOException if the file cannot be opened or read
	 */
	private List<BoundaryConditionLocationPair> readBoundaryConditionLocationPathPairs(String filePath) throws IOException {
		// The three column names that must be present in the CSV header
		String[] headersToExtract = {"location", "parameter", "dss path"};
		List<BoundaryConditionLocationPair> retVal = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			// Read and split the header line to determine column positions
			String line = reader.readLine();
			String[] headers = line.split(",");

			// Resolve the zero-based column index for each required header
			int[] columnIndices = new int[headersToExtract.length];
			for (int i = 0; i < headersToExtract.length; i++) {
				String header = headersToExtract[i];
				int columnIndex = -1;

				for (int j = 0; j < headers.length; j++) {
					if (headers[j].equals(header)) {
						columnIndex = j;
						break;
					}
				}

				// A required column is absent; the file format is incompatible
				if (columnIndex == -1) {
					return new ArrayList<>();
				}

				columnIndices[i] = columnIndex;
			}

			// Parse each data row into a BoundaryConditionLocationPair
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");

				// Skip rows that do not have enough fields to satisfy the header count
				if (fields.length < headers.length) {
					continue;
				}

				BoundaryConditionLocationPair bcLocation =
						buildBoundaryConditionLocationPair(columnIndices, headersToExtract, fields);
				retVal.add(bcLocation);
			}
		}

		return retVal;
	}


	/**
	 * Constructs a BoundaryConditionLocationPair from a single CSV data row.
	 *
	 * Column values are extracted by index and assigned to the appropriate field on
	 * the pair object using a switch on the header name. Unrecognised headers are ignored.
	 *
	 * @param columnIndices    zero-based column indices for each entry in headersToExtract
	 * @param headersToExtract the required header names, aligned with columnIndices
	 * @param fields           the split field values from a single CSV data row
	 * @return a populated BoundaryConditionLocationPair; never null
	 */
	private BoundaryConditionLocationPair buildBoundaryConditionLocationPair(int[] columnIndices, String[] headersToExtract, String[] fields) {
		BoundaryConditionLocationPair bcLocation = new BoundaryConditionLocationPair();

		for (int i = 0; i < columnIndices.length; i++) {
			int columnIndex = columnIndices[i];
			String header = headersToExtract[i];
			String value = fields[columnIndex];

			// Map each recognised column to its corresponding field on the pair object
			switch (header) {
				case "location":
					bcLocation.setLocation(value);
					break;
				case "parameter":
					bcLocation.setParameter(value);
					break;
				case "dss path":
					// Parse the raw DSS pathname string into a structured DSSPathname object
					bcLocation.setDssPath(new DSSPathname(value));
					break;
				default:
					break;
			}
		}

		return bcLocation;
	}


	/**
	 * Returns the G2dPanel plot component managed by this panel.
	 *
	 * Exposed so parent containers can embed or resize the plot independently
	 * if needed.
	 *
	 * @return the G2dPanel instance; never null after construction
	 */
	public G2dPanel getPlotPanel() {
		return _plotPanel;
	}


	/**
	 * Clears the plot panel and resets the location combo box to a deselected,
	 * disabled state.
	 *
	 * Called when the parent panel is closed or the active data is removed.
	 */
	public void clearPanel() {
		// Clear all rendered plot content from the graphics panel
		_plotPanel.clearPanel();

		// Deselect and disable the combo box to indicate no data is available
		_dssPathCombo.setSelectedIndex(-1);
		_dssPathCombo.setEnabled(false);
	}
}
