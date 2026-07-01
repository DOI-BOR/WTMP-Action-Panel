package usbr.wat.plugins.actionpanel.editors;

import java.awt.BorderLayout;             // Layout manager used to position the location panel at the top of the dialog
import java.awt.GridBagConstraints;       // Defines positioning and sizing constraints for components in a GridBagLayout
import java.awt.GridBagLayout;            // Flexible grid-based layout manager for arranging UI components
import java.awt.event.ItemEvent;          // Event fired when a combo box selection changes
import java.awt.event.MouseEvent;         // Mouse event used to retrieve the tooltip for the DSS path combo box
import java.io.BufferedReader;            // Buffered character-stream reader used for efficient line-by-line reading of the DSS paths file
import java.io.IOException;              // Checked exception thrown for I/O failures during file reading
import java.util.ArrayList;              // Resizable-array implementation of the List interface used for DSS item lists
import java.util.List;                   // Ordered collection interface for DSS identifier and item lists
import java.util.Vector;                 // Synchronized growable array used to collect G2dObject instances for plotting

import javax.swing.JLabel;               // Non-interactive text label component used for the "Location:" label
import javax.swing.JPanel;               // Generic lightweight container used to group the location controls

import com.rma.io.DssFileManagerImpl;    // RMA concrete implementation of the DSS file manager for reading time-series and paired data
import com.rma.io.FileManagerImpl;       // RMA file manager implementation for resolving RmaFile references from paths
import com.rma.io.RmaFile;              // RMA abstraction representing a file path for opening the DSS paths CSV
import com.rma.model.Project;           // Represents the currently loaded RMA project, providing the project directory path

import hec.gfx2d.G2dDialog;             // HEC base dialog class for 2D graphical plot display
import hec.gfx2d.G2dObject;             // Abstract base for all plottable 2D data objects
import hec.gfx2d.PairedDataSet;         // G2dObject implementation for rendering paired (X/Y) data sets
import hec.gfx2d.TimeSeriesDataSet;     // G2dObject implementation for rendering time-series data sets
import hec.io.DSSIdentifier;            // Encapsulates a DSS file name and path for identifying a specific DSS record
import hec.io.DataContainer;            // Abstract base for all data containers returned by the DSS file manager
import hec.io.PairedDataContainer;      // Container holding a paired (X/Y) data set read from a DSS file
import hec.io.TimeSeriesContainer;      // Container holding a time-series data set read from a DSS file

import hec2.wat.model.WatAnalysisPeriod; // Represents a WAT analysis period, providing the run time window for clipping DSS queries

import rma.swing.RmaInsets;              // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJComboBox;           // RMA-extended combo box with generic type support and tooltip override capability
import rma.swing.RmaNavigationPanel;     // RMA navigation panel providing previous/next navigation linked to a combo box
import rma.swing.list.RmaListModel;      // RMA list model backed by a collection for use in combo boxes
import rma.util.RMAIO;                   // RMA I/O utility providing path concatenation and string helpers

import usbr.wat.plugins.actionpanel.ActionsWindow; // Parent Actions Window providing the current analysis period

/**
 * USBR-specific extension of the HEC G2dDialog for displaying DSS time-series
 * and paired-data plots within the WTMP Action Panel.
 *
 * Adds a location selection row above the standard plot panel, consisting of a
 * "Location:" label, a combo box of named DSS items loaded from a shared CSV file,
 * and a navigation panel for stepping through the list. When the user changes the
 * selected location, the dialog reads all associated DSS records (optionally clipped
 * to the current analysis period), builds the corresponding G2dObject instances, and
 * redraws the plot panel.
 *
 * DSS item definitions are read from the project-relative file identified by
 * DSS_PATHS_FILE. Each non-comment CSV line in that file must have exactly three
 * fields: a display name, a relative DSS file path, and a DSS path string. Multiple
 * lines sharing the same name are merged into a single DssItem with multiple paths.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class UsbrG2dDialog extends G2dDialog {
	// Project-relative path to the CSV file defining the available DSS plot locations
	public static final String DSS_PATHS_FILE = "shared/dssPlotRecords.csv";

	// Navigation panel providing previous/next controls linked to the DSS path combo box
	private RmaNavigationPanel _navPanel;

	// Combo box for selecting the named DSS plot location; overrides tooltip to show DSS paths
	private RmaJComboBox<DssItem> _dssPathCombo;

	// Reference to the parent Actions Window, used to retrieve the current analysis period
	private ActionsWindow _parent;

	/**
	 * Constructs a UsbrG2dDialog for the given parent Actions Window.
	 *
	 * Initializes the base G2dDialog, stores the parent reference, builds the
	 * location selection controls, attaches the combo box item listener, and
	 * loads DSS path definitions from the project's shared CSV file.
	 *
	 * @param parent the ActionsWindow that owns this dialog
	 */
	public UsbrG2dDialog(ActionsWindow parent) {
		// Initialize the base HEC G2dDialog
		super();

		// Store a reference to the parent Actions Window for analysis period access
		_parent = parent;

		// Build and add the location selection panel to the dialog
		buildControls();

		// Attach item-change listener to the DSS path combo box
		addListeners();

		// Read the DSS paths CSV file and populate the combo box
		loadDssPaths();
	}


	/**
	 * Builds the location selection panel and inserts it at the top of the dialog.
	 *
	 * Creates a "Location:" label, a DSS path combo box (with a tooltip override that
	 * lists all associated DSS paths for the selected item), and a navigation panel
	 * for stepping through the list. The entire panel is placed in the NORTH region
	 * of the dialog's BorderLayout content pane.
	 */
	private void buildControls() {
		// Create the container panel with a GridBagLayout for the location controls
		JPanel panel = new JPanel(new GridBagLayout());

		// Create the "Location:" label
		JLabel label = new JLabel("Location:");

		// Configure constraints for the label: fixed width, left-aligned, no fill
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(label, gbc);

		// Create the DSS path combo box with a tooltip override showing all DSS paths
		_dssPathCombo = new RmaJComboBox<DssItem>() {
			@Override
			public String getToolTipText(MouseEvent e) {
				// Get the currently selected DssItem to build its tooltip
				DssItem dssItem = (DssItem) _dssPathCombo.getSelectedItem();
				if (dssItem != null) {
					// Delegate to the helper method that formats all paths as HTML
					return getToolTip(dssItem.dssIds);
				}
				// No selection; return no tooltip
				return null;
			}
		};

		// Configure constraints for the combo box: horizontally expanding
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_dssPathCombo, gbc);

		// Create the navigation panel and link it to the DSS path combo box
		_navPanel = new RmaNavigationPanel();
		_navPanel.fillForm(_dssPathCombo);

		// Configure constraints for the navigation panel: fixed width at the end of the row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_navPanel, gbc);

		// Insert the location panel at the top of the dialog's content pane
		this.getContentPane().add(BorderLayout.NORTH, panel);
	}

	/**
	 * Builds an HTML tooltip string listing all DSS identifiers for the given list.
	 *
	 * Each identifier is placed on its own line using an HTML line-break tag.
	 *
	 * @param dssIds the list of DSSIdentifier objects to include in the tooltip
	 * @return an HTML-formatted string enumerating each DSS identifier
	 */
	protected String getToolTip(List<DSSIdentifier> dssIds) {
		StringBuilder builder = new StringBuilder();

		// Open the HTML tooltip block
		builder.append("<html>");

		DSSIdentifier dssId;
		for (int i = 0; i < dssIds.size(); i++) {
			dssId = dssIds.get(i);

			// Append the DSS identifier string followed by an HTML line break
			builder.append(dssId.toString());
			builder.append("<br>");
		}

		return builder.toString();
	}

	/**
	 * Attaches an item listener to the DSS path combo box to trigger a plot refresh
	 * whenever the selected location changes.
	 */
	protected void addListeners() {
		// Refresh the plot whenever the user selects a different DSS location
		_dssPathCombo.addItemListener(e -> dssComboChanged(e));
	}


	/**
	 * Responds to a selection change in the DSS path combo box.
	 *
	 * Ignores DESELECTED events (which fire for the previously selected item).
	 * For SELECTED events, reads all DSS records associated with the new item,
	 * clips them to the current analysis period if one is active, creates the
	 * appropriate G2dObject for each record, and passes the collected objects
	 * to the plot panel for rendering.
	 *
	 * @param e the ItemEvent describing the combo box selection change
	 */
	private void dssComboChanged(ItemEvent e) {
		// Ignore the DESELECTED event that fires for the previously selected item
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Create a vector to collect G2dObject instances for this location
		Vector plotData = new Vector();

		// Retrieve the newly selected DssItem from the combo box
		DssItem dssItem = (DssItem) _dssPathCombo.getSelectedItem();
		List<DSSIdentifier> dssIds = dssItem.dssIds;

		// Get the current analysis period from the parent window (may be null)
		WatAnalysisPeriod ap = _parent.getAnalysisPeriod();

		// Process each DSS identifier associated with this location
		for (int i = 0; i < dssIds.size(); i++) {
			DSSIdentifier dssId = dssIds.get(i);

			// Clip the DSS query to the analysis period's time window if one is set
			if (ap != null) {
				dssId.setStartTime(ap.getRunTimeWindow().getStartTime());
				dssId.setEndTime(ap.getRunTimeWindow().getEndTime());
			}

			// Read the data container from the DSS file for this identifier
			DataContainer dc = DssFileManagerImpl.getDssFileManager().readDataContainer(dssId);

			G2dObject g2dObj = null;

			if (dc instanceof TimeSeriesContainer) {
				// Wrap a time-series container in the appropriate G2d dataset type
				g2dObj = new TimeSeriesDataSet((TimeSeriesContainer) dc);
			} else if (dc instanceof PairedDataContainer) {
				// Wrap a paired-data container in the appropriate G2d dataset type
				g2dObj = new PairedDataSet((PairedDataContainer) dc);
			} else {
				// Log an unknown container type and skip it without adding to the plot
				System.out.println("dssComboChanged:unknown type " + dc);
				continue;
			}

			// Add the G2d object to the plot data collection
			plotData.add(g2dObj);
		}

		// Rebuild the plot panel components with the new data
		getPlotpanel().buildComponents(plotData);

		// Apply line styles consistent with the current display preferences
		getPlotpanel().useLineStyles(_useLineStyles);

		// Update the dialog title to reflect the newly selected location name
		setTitle(dssItem.name);
	}


	/**
	 * Reads the DSS plot record definitions from the project's shared CSV file and
	 * populates the DSS path combo box.
	 *
	 * The CSV file (DSS_PATHS_FILE) must contain lines with exactly three
	 * comma-separated fields: display name, relative DSS file path, and DSS path string.
	 * Lines beginning with '#' are treated as comments and skipped. Multiple lines
	 * sharing the same display name are merged into a single DssItem with multiple
	 * DSSIdentifier entries. If the file cannot be found, a message is printed and
	 * the method returns without populating the combo box.
	 */
	private void loadDssPaths() {
		// Resolve the absolute path to the DSS paths CSV file within the project directory
		String dir = Project.getCurrentProject().getProjectDirectory();
		String pathsFile = RMAIO.concatPath(dir, DSS_PATHS_FILE);

		// Obtain an RmaFile reference and open a buffered reader for it
		RmaFile dssPathFile = FileManagerImpl.getFileManager().getFile(pathsFile);
		BufferedReader reader = dssPathFile.getBufferedReader();

		String line;
		List<DssItem> dssItems = new ArrayList<>();

		// If the file was not found, log a message and exit early
		if (reader == null) {
			System.out.println("loadDssPaths:failed to find file " + pathsFile);
			return;
		}

		try {
			DssItem dssItem;

			// Read the file line by line until EOF
			while ((line = reader.readLine()) != null) {
				// Skip comment lines beginning with '#'
				if (line.startsWith("#")) {
					continue;
				}

				// Split each line into its three CSV fields
				String[] parts = line.split(",");

				// Skip lines that do not have exactly three fields
				if (parts == null || parts.length != 3) {
					continue;
				}

				// Extract and trim the display name, file path, and DSS path
				String name = parts[0].trim();
				String file = parts[1].trim();

				// Resolve the relative file path to an absolute project-relative path
				file = Project.getCurrentProject().getAbsolutePath(file);
				String path = parts[2].trim();

				// Look for an existing DssItem with the same display name
				dssItem = findDssItem(name, dssItems);

				if (dssItem == null) {
					// No existing item: create a new DssItem with this path
					dssItem = new DssItem(parts[0].trim(), file, path);
					dssItems.add(dssItem);
				} else {
					// Existing item found: append the additional DSS path to it
					dssItem.addLocation(file, path);
				}
			}

			// Build a new list model from the collected DssItems (with a blank first entry)
			RmaListModel newModel = new RmaListModel(true, dssItems);
			_dssPathCombo.setModel(newModel);

			// If at least one item was loaded, select the first entry to trigger an initial plot
			if (newModel.size() > 0) {
				_dssPathCombo.setSelectedIndex(0);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// Always close the reader to release the file handle
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Silently ignore close failures
				}
			}
		}
	}


	/**
	 * Searches the provided list for a DssItem whose name matches the given name,
	 * using a case-insensitive comparison.
	 *
	 * @param name     the display name to search for
	 * @param dssItems the list of DssItem objects to search
	 * @return the matching DssItem if found; null otherwise
	 */
	private static DssItem findDssItem(String name, List<DssItem> dssItems) {
		int size = dssItems.size();

		for (int i = 0; i < size; i++) {
			// Use case-insensitive comparison to handle minor name discrepancies
			if (dssItems.get(i).name.equalsIgnoreCase(name)) {
				return dssItems.get(i);
			}
		}

		// No match found in the list
		return null;
	}


	/**
	 * Inner class representing a named group of DSS identifiers for a single plot location.
	 *
	 * A DssItem aggregates one or more DSSIdentifier objects that collectively define
	 * the data sources for a single named location in the plot dialog. Multiple CSV
	 * lines sharing the same name are merged into one DssItem at load time.
	 */
	public class DssItem {
		// The display name for this plot location, shown in the combo box
		String name;

		// The list of DSS identifiers (file + path pairs) associated with this location
		List<DSSIdentifier> dssIds = new ArrayList<>();

		/**
		 * Constructs a DssItem with the given display name and an initial DSS location.
		 *
		 * @param n    the display name for this location
		 * @param file the absolute path to the DSS file containing this record
		 * @param path the DSS path string identifying the record within the file
		 */
		DssItem(String n, String file, String path) {
			// Store the display name
			name = n;

			// Add the initial file/path combination as the first DSS identifier
			addLocation(file, path);
		}

		/**
		 * Adds an additional DSS file and path combination to this item.
		 *
		 * Called when multiple CSV lines share the same display name, allowing
		 * a single location to aggregate data from several DSS records.
		 *
		 * @param file the absolute path to the DSS file
		 * @param path the DSS path string identifying the record within the file
		 */
		public void addLocation(String file, String path) {
			// Create a DSSIdentifier from the file and path and add it to the list
			DSSIdentifier dssId = new DSSIdentifier(file, path);
			dssIds.add(dssId);
		}

		/**
		 * Returns the display name of this DSS item, used when rendering it in the combo box.
		 *
		 * @return the display name for this location
		 */
		@Override
		public String toString() {
			return name;
		}
	}
}
