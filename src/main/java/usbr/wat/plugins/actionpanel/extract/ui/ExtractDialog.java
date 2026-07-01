package usbr.wat.plugins.actionpanel.extract.ui;

import java.awt.Desktop;              // AWT Desktop integration for opening files with the OS default application
import java.awt.Dimension;            // Represents width/height dimensions; used to fix the table viewport height
import java.awt.GridBagConstraints;   // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;        // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;               // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent;    // Represents an action event fired by buttons and menu items
import java.awt.event.MouseEvent;     // Mouse event used for tooltip lookup in the table and store-rule combo box
import java.awt.event.WindowAdapter;  // Adapter for window lifecycle events; used to intercept the window-close action
import java.awt.event.WindowEvent;    // Window event describing the window-closing lifecycle step
import java.io.File;                  // Represents a file system path for the log file chooser result
import java.io.IOException;           // Checked exception thrown when opening a file via Desktop fails
import java.nio.file.Path;            // Represents an immutable file system path passed to the Merlin extract engine
import java.nio.file.Paths;           // Factory for constructing Path instances from string paths
import java.time.Instant;             // UTC instant used as the start/end time in Merlin extract parameters
import java.time.LocalDateTime;       // Local date-time without time zone; retrieved from the date-time panels
import java.time.ZoneId;              // Identifies the system default time zone for LocalDateTime-to-Instant conversion
import java.time.ZonedDateTime;       // LocalDateTime paired with a ZoneId; used as an intermediate for Instant conversion
import java.util.ArrayList;           // Resizable-array List used to accumulate selected file paths
import java.util.List;                // Ordered collection interface for file and path lists
import java.util.TimeZone;            // Legacy time zone representation used when setting the date-time panel values
import java.util.Vector;              // Synchronized growable array used to build extract table rows

import javax.swing.BorderFactory;          // Factory for creating titled and other Swing border styles
import javax.swing.JButton;                // Standard Swing push-button component
import javax.swing.JFileChooser;           // Dialog for browsing and selecting a log file to open
import javax.swing.JLabel;                 // Non-interactive text label for field captions
import javax.swing.JMenuItem;             // Clickable menu item in the extract table's right-click popup
import javax.swing.JOptionPane;           // Standard informational and error dialog boxes
import javax.swing.JPanel;               // Generic lightweight container for the time window sub-panel
import javax.swing.JSeparator;           // Horizontal visual divider between the sim-group field and time window section

import com.rma.client.Browser;            // RMA browser frame; fallback for opening files when Desktop is unsupported
import com.rma.io.FileManagerImpl;        // RMA file manager for resolving paths, listing files, and creating directories
import com.rma.io.RmaFile;               // RMA abstraction representing a file system path
import com.rma.model.Project;            // Represents the currently loaded RMA project and its directory and time zone

import hec.io.Identifier;               // HEC identifier object carrying a file path string; returned by the file manager
import hec.io.impl.StoreOptionImpl;     // HEC DSS store option implementation specifying how existing data is overwritten
import hec.model.RunTimeWindow;          // Represents a simulation run time window with start and end HecTime values

import gov.usbr.wq.merlindataexchange.MerlinConfigParseException;          // Checked exception thrown when a Merlin XML config file cannot be parsed
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeParser;            // Parses Merlin XML configuration files for validation
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeStatus;            // Enum representing the final outcome of a Merlin extract run
import gov.usbr.wq.merlindataexchange.parameters.AuthenticationParametersBuilder; // Builder for authentication parameters (URL, username, password)
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParameters;          // Immutable parameters object for the profile extract phase
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParametersBuilder;  // Builder for constructing MerlinProfileParameters instances
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParameters;      // Immutable parameters object for the time-series extract phase
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParametersBuilder; // Builder for constructing MerlinTimeSeriesParameters instances

import rma.swing.ButtonCmdPanel;         // Panel containing standard command buttons (OK/Extract and Close)
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.DateTimePanel;          // RMA panel providing date and time entry fields in a configurable layout
import rma.swing.RmaInsets;             // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJComboBox;          // RMA-extended combo box with generic type support and tooltip override
import rma.swing.RmaJDialog;            // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTable;             // RMA-extended table component with utility row management methods
import rma.swing.RmaJTextField;         // RMA-extended text field component
import rma.swing.text.DateDocument;     // Document model constant specifying the DD-MMM-YYYY date format for DateTimePanel
import rma.util.RMAIO;                  // RMA I/O utility providing path concatenation and other string/file helpers

import usbr.wat.plugins.actionpanel.extract.action.RunExtractAction;   // Action class that executes the Merlin extract asynchronously
import usbr.wat.plugins.actionpanel.extract.model.ExtractLoginInfo;    // Utility for prompting and caching Merlin service login credentials
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;     // Base class for simulation groups; provides the analysis period and name

/**
 * Modal dialog for configuring and executing a Merlin data extract operation
 * within the WTMP Action Panel.
 *
 * Presents the following controls to the user:
 *
 *   - A read-only Simulation Group name field derived from the provided group.
 *   - A Time Window panel with start and end date/time fields pre-filled from
 *     the simulation group's analysis period. If no analysis period is set, the
 *     Extract button is disabled.
 *   - A DSS Store Rule combo box (backed by the StoreTypes enum) controlling
 *     how extracted data overwrites existing DSS records.
 *   - An optional DSS F-Part override text field.
 *   - A table listing all Merlin XML configuration files found in the project's
 *     shared/extract/config directory, with a checkbox column for selection.
 *     Right-click menu items allow viewing or validating the selected file.
 *   - An Extract Log button opening a file chooser in the project's log directory.
 *   - An Extract (OK) button that triggers runExtract() and a Close button.
 *
 * The extract runs asynchronously via RunExtractAction. On COMPLETE_SUCCESS or
 * PARTIAL_SUCCESS, a post-update Runnable provided at construction time is
 * executed to refresh downstream UI state.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ExtractDialog extends RmaJDialog {
	/**
	 * Enum representing the available DSS data store rules for the extract operation.
	 *
	 * Each constant carries a combo box display label, a Merlin store rule string
	 * passed to the DSS store option, and a tooltip describing the rule's behavior.
	 */
	enum StoreTypes {
		// Always overwrite existing DSS records with incoming data
		OptionReplaceAll("Replace All", "0-replace-all", "Always replace data"),

		// Only overwrite cells that currently hold missing values
		OptionReplaceMissing("Replace Missing Values Only", "1-replace-missing-values-only", "Only replace missing data"),

		// Write data even if all values are missing (creates a missing record)
		OptionReplaceAllCreate("Replace All Create ", "2-replace-all-create", "Write regardless, even if all missing data (write a missing record)"),

		// If the incoming record is entirely missing, delete the existing DSS record rather than writing
		OptionReplaceAllDelete("Replace All Delete ", "3-replace-all-delete", "If a record is all missing, do not write it, and delete it from disk if it exists."),

		// Do not allow a missing input value to replace a valid existing value
		OptionReplaceWithNonMissing("Replace With Non-Missing", "4-replace-with-non-missing", "Do not allow a missing input data to replace a valid data piece.");

		// The human-readable label displayed in the combo box
		private String _comboText;

		// The Merlin store rule string passed to StoreOptionImpl
		private String _storeRule;

		// The descriptive tooltip shown when hovering over the selected combo item
		private String _tooltip;

		/**
		 * Constructs a StoreTypes constant with its display text, store rule string, and tooltip.
		 *
		 * @param comboText the label shown in the combo box drop-down
		 * @param storeRule the Merlin store rule string consumed by StoreOptionImpl
		 * @param tooltip   the description displayed as a tooltip on the selected item
		 */
		StoreTypes(String comboText, String storeRule, String tooltip) {
			_comboText = comboText;
			_storeRule = storeRule;
			_tooltip = tooltip;
		}

		/**
		 * Returns the combo box display label for this store type.
		 *
		 * @return the human-readable label string
		 */
		@Override
		public String toString() {
			return _comboText;
		}

		/**
		 * Returns the Merlin store rule string for this store type.
		 *
		 * This value is passed to StoreOptionImpl.setRegular() when building
		 * the DSS store option for the extract parameters.
		 *
		 * @return the store rule identifier string
		 */
		public String getStoreRule() {
			return _storeRule;
		}

		/**
		 * Returns the tooltip text describing this store rule's behavior.
		 *
		 * @return the descriptive tooltip string
		 */
		public String getToolTip() {
			return _tooltip;
		}
	}

	// Relative path segment for the project's shared resources folder
	private static final String SHARED_DIR = "shared";

	// Relative path segment for the folder containing extract log files
	private static final String EXTRACT_LOGS_DIR = "extract/logs";

	// Relative path segment for the folder containing Merlin XML configuration files
	private static final String EXTRACT_CONFIG_DIR = "extract/config";

	// File extension filter used when listing configuration files in the config directory
	private static final String XML_EXTENSION = "xml";

	// The Merlin service URL; overridable at JVM startup via -DExtract.Url=<url>
	private static final String GRAB_DATA_URL = System.getProperty("Extract.Url", "https://www.grabdata2.com");

	// Runnable invoked after a successful or partially-successful extract to refresh downstream state
	private final Runnable _postUpdateAction;

	// Read-only field displaying the name of the current simulation group
	private RmaJTextField _simGroupFld;

	// Date/time panel for the extract start date and time
	private DateTimePanel _startDateTimePanel;

	// Date/time panel for the extract end date and time
	private DateTimePanel _endDateTimePanel;

	// Combo box for selecting the DSS data store rule applied during the extract
	private RmaJComboBox<StoreTypes> _storeRuleCombo;

	// Optional text field for overriding the DSS F-Part in extracted records
	private RmaJTextField _dssFpartFld;

	// Table listing available Merlin XML configuration files with a selection checkbox
	private RmaJTable _extractTable;

	// Button that opens a file chooser in the extract logs directory
	private JButton _viewLogBtn;

	// Panel containing the Extract (OK) and Close buttons
	private ButtonCmdPanel _cmdPanel;

	// Right-click table menu item for opening the selected config file in the OS default editor
	private JMenuItem _viewConfigFileMenu;

	// Right-click table menu item for validating the selected config file's XML format
	private JMenuItem _validateConfigFileMenu;

	/**
	 * Constructs a modal ExtractDialog for the given simulation group.
	 *
	 * Builds all UI controls, attaches listeners, sizes the dialog to its preferred
	 * layout, centers it relative to the parent window, and populates the form from
	 * the simulation group's analysis period and the project's configuration files.
	 *
	 * @param parent           the Window that will own this modal dialog
	 * @param simulationGroup  the simulation group whose name and analysis period pre-fill the form
	 * @param postUpdateAction a Runnable executed after a successful extract to refresh downstream UI
	 */
	public ExtractDialog(Window parent, AbstractSimulationGroup simulationGroup, Runnable postUpdateAction) {
		// Initialize the base RmaJDialog as modal
		super(parent, true);

		// Store the post-update callback for execution after a successful extract
		_postUpdateAction = postUpdateAction;

		// Build and arrange all UI components
		buildControls();

		// Attach action and window listeners to interactive controls
		addListeners();

		// Size the dialog to its preferred layout dimensions
		pack();

		// Center the dialog relative to its parent window
		setLocationRelativeTo(getParent());

		// Populate the form fields from the simulation group and project configuration files
		fillForm(simulationGroup);
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Places the following controls using GridBagLayout: a read-only simulation
	 * group field, a horizontal separator, a titled Time Window panel with start
	 * and end date/time fields, a DSS Store Rule combo box with a per-item tooltip,
	 * an optional DSS F-Part field, a configuration file table with right-click menu
	 * items for viewing and validating files, a View Log button, and an Extract/Close
	 * button panel.
	 */
	private void buildControls() {
		// Apply GridBagLayout to the dialog content pane
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Run Extract");

		// Create the "Simulation Group:" label
		JLabel label = new JLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();

		// Position the label with fixed width at the left edge
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only simulation group name field
		_simGroupFld = new RmaJTextField();
		_simGroupFld.setEditable(false);

		// Position the simulation group field to expand horizontally and end the row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupFld, gbc);

		// Add a horizontal separator to visually divide the sim-group field from the time window section
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the titled time window sub-panel containing start and end date/time fields
		JPanel timeWindowPanel = new JPanel(new GridBagLayout());
		timeWindowPanel.setBorder(BorderFactory.createTitledBorder("Time Window"));

		// Position the time window panel to span the full row width
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(timeWindowPanel, gbc);

		// Create the start date/time panel in horizontal layout using the DD-MMM-YYYY date format
		_startDateTimePanel = new DateTimePanel(DateTimePanel.HORIZONTAL_LAYOUT, "Start Date:", "Time:", DateDocument.DDMMMYYYY);

		// Position the start date/time panel within the time window sub-panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		timeWindowPanel.add(_startDateTimePanel, gbc);

		// Create the end date/time panel in the same layout and format as the start panel
		_endDateTimePanel = new DateTimePanel(DateTimePanel.HORIZONTAL_LAYOUT, "End Date:", "Time:", DateDocument.DDMMMYYYY);

		// Position the end date/time panel below the start panel within the time window sub-panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		timeWindowPanel.add(_endDateTimePanel, gbc);

		// Create the "DSS Store Rule:" label for the store rule combo box
		label = new JLabel("DSS Store Rule:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the store rule combo box pre-populated with all StoreTypes enum values;
		// overrides getToolTipText to show the selected item's behavior description
		_storeRuleCombo = new RmaJComboBox<StoreTypes>(StoreTypes.values()) {
			@Override
			public String getToolTipText(MouseEvent e) {
				// Retrieve the currently selected store type to build its tooltip
				StoreTypes st = (StoreTypes) getSelectedItem();
				if (st != null) {
					// Return the store type's descriptive tooltip text
					return st.getToolTip();
				}

				// Fall back to the default tooltip if nothing is selected
				return super.getToolTipText(e);
			}
		};

		// Position the store rule combo box to expand horizontally and end the row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_storeRuleCombo, gbc);

		// Create the "DSS F-Part (optional):" label for the F-Part override field
		label = new JLabel("DSS F-Part (optional):");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the optional DSS F-Part override text field
		_dssFpartFld = new RmaJTextField();

		// Position the F-Part field to expand horizontally and end the row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_dssFpartFld, gbc);

		// Create the "Extract Configuration Files:" label for the config file table
		label = new JLabel("Extract Configuration Files:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Define the column headers for the config file selection table
		String[] headers = new String[]{"Select", "Extract File"};

		// Create the extract configuration file table with custom viewport height, editability, and tooltip
		_extractTable = new RmaJTable(this, headers) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Fix the viewport to exactly six row heights for a compact display
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * 6;
				return d;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				// Only the Select (checkbox) column is directly editable
				return column == 0;
			}

			@Override
			public String getToolTipText(MouseEvent e) {
				// Determine which row the mouse is over
				int row = rowAtPoint(e.getPoint());
				if (row == -1) {
					// No row under the cursor; use the default tooltip
					return super.getToolTipText(e);
				}

				int col = columnAtPoint(e.getPoint());

				// Show the full file path as a tooltip when hovering over the Extract File column
				if (col == 1) {
					Identifier id = (Identifier) getValueAt(row, col);
					return id.getPath();
				}

				// Default tooltip for all other columns
				return super.getToolTipText(e);
			}
		};

		// Create the right-click "View File..." menu item for opening a config file in the OS editor
		_viewConfigFileMenu = new JMenuItem("View File...");

		// Only add the view menu item if the Desktop API is supported on this platform
		if (Desktop.isDesktopSupported()) {
			_extractTable.addPopupItem(_viewConfigFileMenu, 0);
		}

		// Create the right-click "Validate File..." menu item for XML format validation
		_validateConfigFileMenu = new JMenuItem("Validate File...");
		_extractTable.addPopupItem(_validateConfigFileMenu, 0);

		// Enable checkbox-based selection in the Select (column 0) column
		_extractTable.setCheckBoxCellEditor(0);

		// Add extra row height for readability
		_extractTable.setRowHeight(_extractTable.getRowHeight() + 5);

		// Set fixed column widths for the Select and Extract File columns
		_extractTable.setColumnWidths(115, 335);

		// Position the table to fill all remaining vertical and horizontal space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_extractTable.getScrollPane(), gbc);

		// Create the "Extract Log..." button for opening an existing log file
		_viewLogBtn = new JButton("Extract Log...");
		_viewLogBtn.setToolTipText("View the extract logs");

		// Position the log button at the bottom-left corner
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_viewLogBtn, gbc);

		// Create the button panel with both OK (Extract) and Close buttons
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_BUTTON | ButtonCmdPanel.CLOSE_BUTTON);

		// Rename the OK button to "Extract" to better describe its action
		JButton okButton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		okButton.setText("Extract");

		// Position the button panel at the bottom of the dialog spanning the remaining width
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
	 * Attaches event listeners to all interactive controls.
	 *
	 * Registers: a ButtonCmdPanelListener for the Extract and Close buttons,
	 * a WindowAdapter to intercept the window-close action, a lambda listener
	 * on the View Log button, and lambda listeners on both right-click menu items.
	 */
	private void addListeners() {
		// Handle Extract (OK) and Close button clicks from the command panel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Trigger the extract operation when the Extract button is clicked
						runExtract();
						break;

					case ButtonCmdPanel.CLOSE_BUTTON:
						// Hide the dialog when the Close button is clicked
						closeWindow();
						break;
				}
			}
		});

		// Intercept the OS window-close button (X) to use the same closeWindow logic
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
		});

		// Open the extract log file chooser when the View Log button is clicked
		_viewLogBtn.addActionListener(e -> viewExtractLog());

		// Open the selected config file in the OS default application when View File is chosen
		_viewConfigFileMenu.addActionListener(e -> viewConfigFile());

		// Validate the selected config file's XML format when Validate File is chosen
		_validateConfigFileMenu.addActionListener(e -> validateConfigFile());
	}


	/**
	 * Hides the dialog without performing any additional cleanup.
	 *
	 * Called by both the Close button and the OS window-close action so that
	 * both paths share the same dismissal behavior.
	 */
	protected void closeWindow() {
		setVisible(false);
	}


	/**
	 * Validates the Merlin XML configuration file associated with the currently
	 * selected table row.
	 *
	 * Parses the file using MerlinDataExchangeParser. If parsing succeeds, shows
	 * an informational success dialog. If parsing fails, shows the parse exception
	 * message in an informational dialog to help the user locate the problem.
	 * Has no effect if no row is selected or the row's identifier is null.
	 */
	private void validateConfigFile() {
		// Do nothing if no table row is selected
		int row = _extractTable.getSelectedRow();
		if (row == -1) {
			return;
		}

		// Retrieve the file identifier from the Extract File column
		Identifier id = (Identifier) _extractTable.getValueAt(row, 1);
		if (id == null) {
			return;
		}

		// Resolve the identifier's path to an RmaFile and convert to a NIO Path
		RmaFile file = FileManagerImpl.getFileManager().getFile(id.getPath());
		Path path = file.toPath();

		try {
			// Attempt to parse the XML file; throws MerlinConfigParseException if the format is invalid
			MerlinDataExchangeParser.parseXmlFile(path);

			// Notify the user that the file format is valid
			JOptionPane.showMessageDialog(this, "File Format Validation Successful", "Success", JOptionPane.INFORMATION_MESSAGE);
		} catch (MerlinConfigParseException e) {
			// Show the parse error message so the user can identify and fix the problem
			JOptionPane.showMessageDialog(this, e.getMessage(), "Parsing Failed", JOptionPane.INFORMATION_MESSAGE);
		}
	}


	/**
	 * Populates the form controls from the given simulation group and the project's
	 * extract configuration file directory.
	 *
	 * Sets the simulation group name field, pre-fills the start and end date/time
	 * panels from the group's analysis period, and enables or disables the Extract
	 * button based on whether an analysis period is set. Scans the project's
	 * shared/extract/config directory for XML files and adds each as an unchecked
	 * row in the configuration file table.
	 *
	 * @param simulationGroup the simulation group whose name and analysis period populate the form
	 */
	private void fillForm(AbstractSimulationGroup simulationGroup) {
		// Display the simulation group's name in the read-only field
		_simGroupFld.setText(simulationGroup.getName());

		JButton okbutton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);

		if (simulationGroup.getAnalysisPeriod() != null) {
			// Retrieve the run time window and project time zone for date/time panel initialization
			RunTimeWindow rtw = simulationGroup.getAnalysisPeriod().getRunTimeWindow();
			TimeZone tz = Project.getCurrentProject().getTimeZone();

			// Pre-fill both date/time panels from the analysis period's time window
			_startDateTimePanel.setDateTime(rtw.getStartTime(), tz);
			_endDateTimePanel.setDateTime(rtw.getEndTime(), tz);

			// Enable the Extract button and set a helpful tooltip
			okbutton.setToolTipText("Run the data extract");
			okbutton.setEnabled(true);
		} else {
			// No analysis period: disable the Extract button and explain why via tooltip
			okbutton.setEnabled(false);
			okbutton.setToolTipText("No Time Window for Simulation Group");
		}

		// Clear any existing rows before populating the config file table
		_extractTable.deleteCells();

		// Build the path to the project's Merlin XML configuration file directory
		String dir = Project.getCurrentProject().getProjectDirectory();
		dir = RMAIO.concatPath(dir, SHARED_DIR);
		dir = RMAIO.concatPath(dir, EXTRACT_CONFIG_DIR);

		// Retrieve all XML files in the config directory (recursive search)
		List<Identifier> configFiles = FileManagerImpl.getFileManager().getFileList(dir, XML_EXTENSION, true);

		Identifier id;
		Vector<Object> row;
		for (int i = 0; i < configFiles.size(); i++) {
			// Build a row with an unchecked checkbox and the file identifier
			row = new Vector<>(2);
			row.add(Boolean.FALSE);
			row.add(configFiles.get(i));
			_extractTable.appendRow(row);
		}
	}

	/**
	 * Opens a JFileChooser in the project's extract log directory and launches
	 * the selected log file in the OS default application.
	 *
	 * Falls back to the RMA browser frame's openFile method if the Desktop API
	 * is not supported on the current platform. Has no effect if the user cancels
	 * the file chooser or no file is selected.
	 */
	private void viewExtractLog() {
		// Build the path to the extract logs directory
		String dir = Project.getCurrentProject().getProjectDirectory();
		dir = RMAIO.concatPath(dir, SHARED_DIR);
		dir = RMAIO.concatPath(dir, EXTRACT_LOGS_DIR);

		// Open a file chooser starting in the log directory
		JFileChooser chooser = new JFileChooser(dir);
		int opt = chooser.showOpenDialog(this);

		// Return immediately if the user did not confirm a file selection
		if (opt != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = chooser.getSelectedFile();
		if (file == null) {
			return;
		}

		if (Desktop.isDesktopSupported()) {
			try {
				// Open the file with the OS default application for its type
				Desktop.getDesktop().open(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Fall back to the RMA browser frame for platforms without Desktop support
			Browser.getBrowserFrame().openFile(file, false);
		}
	}

	/**
	 * Opens the Merlin XML configuration file associated with the currently
	 * selected table row using the OS default application.
	 *
	 * Only available on platforms where the Desktop API is supported. Has no
	 * effect if no row is selected or the row's identifier is null.
	 */
	private void viewConfigFile() {
		// Do nothing if no row is selected
		int row = _extractTable.getSelectedRow();
		if (row == -1) {
			return;
		}

		// Retrieve the file identifier from the Extract File column
		Identifier id = (Identifier) _extractTable.getValueAt(row, 1);
		if (id == null) {
			return;
		}

		// Resolve the identifier's path to an RmaFile for opening
		RmaFile file = FileManagerImpl.getFileManager().getFile(id.getPath());

		if (Desktop.isDesktopSupported()) {
			try {
				// Open the config file with the OS default XML/text editor
				Desktop.getDesktop().open(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the file paths of all configuration file table rows whose Select
	 * checkbox is checked.
	 *
	 * Iterates all rows and collects the path string from the Extract File column
	 * for any row where the Select column evaluates to true.
	 *
	 * @return a List of absolute file path strings for the selected configuration files
	 */
	public List<String> getSelectedFiles() {
		int rows = _extractTable.getNumRows();
		List<String> files = new ArrayList<>(rows);
		Object selected;
		Identifier id;

		for (int r = 0; r < rows; r++) {
			// Read the Select column value for this row
			selected = _extractTable.getValueAt(r, 0);

			// Include the file if the checkbox is checked (supports both Boolean and String representations)
			if (selected == Boolean.TRUE || Boolean.parseBoolean(selected.toString())) {
				id = (Identifier) _extractTable.getValueAt(r, 1);
				files.add(id.getPath());
			}
		}

		return files;
	}

	/**
	 * Converts the selected file path strings from getSelectedFiles() into NIO
	 * Path objects for use with the Merlin extract engine.
	 *
	 * @return a List of Path objects corresponding to the selected configuration files
	 */
	public List<Path> getSelectedPaths() {
		List<Path> paths = new ArrayList<>();
		List<String> files = getSelectedFiles();
		Path path;

		for (int i = 0; i < files.size(); i++) {
			// Convert each absolute path string to a NIO Path object
			path = Paths.get(files.get(i));
			paths.add(path);
		}

		return paths;
	}

	/**
	 * Validates user input, ensures valid credentials are available, constructs
	 * the Merlin extract parameters, and asynchronously executes the extract.
	 *
	 * Validates that at least one configuration file is selected. If no cached
	 * credentials are available, prompts the user in a do-while loop until both
	 * a username and password have been entered. Constructs the project path and
	 * log directory (creating it if needed), converts the UI date/time values to
	 * UTC Instants using the system time zone, and builds immutable time-series
	 * and profile parameter objects.
	 *
	 * Delegates execution to RunExtractAction.extract(). On COMPLETE_SUCCESS or
	 * PARTIAL_SUCCESS, the postUpdateAction Runnable is invoked to refresh
	 * downstream state.
	 */
	protected void runExtract() {
		// Collect the selected configuration file paths; abort if none are selected
		List<Path> selectedPaths = getSelectedPaths();
		if (selectedPaths.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please select the extract configuration files to use.", "No Files Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Retrieve any previously cached credentials
		String userName = ExtractLoginInfo.getUserName();
		String password = ExtractLoginInfo.getPassword();

		// Keep prompting for credentials until both a username and password are provided
		do {
			if (userName == null || password == null) {
				// Prompt the user; return immediately if they cancel the login dialog
				if (!ExtractLoginInfo.askForLoginInfo(this, "Enter login information for " + GRAB_DATA_URL)) {
					return;
				}

				// Update local variables with the newly entered credentials
				userName = ExtractLoginInfo.getUserName();
				password = ExtractLoginInfo.getPassword();
			}
		}
		while (userName == null || password == null);

		// Resolve project and log directory paths for the extract engine
		String prjFolder = Project.getCurrentProject().getProjectDirectory();
		Path prjPath = Paths.get(prjFolder);

		String logFolder = RMAIO.concatPath(prjFolder, SHARED_DIR);
		logFolder = RMAIO.concatPath(logFolder, EXTRACT_LOGS_DIR);

		// Ensure the log directory exists before the extract engine tries to write to it
		FileManagerImpl.getFileManager().createDirectory(logFolder);
		Path logPath = Paths.get(logFolder);

		// Convert the start date/time from the UI panel to a UTC Instant using the system time zone
		ZoneId localZone = ZoneId.systemDefault();
		LocalDateTime localStart = _startDateTimePanel.getDateTime().elementAt(0).getLocalDateTime();
		ZonedDateTime zonedStartDateTime = localStart.atZone(localZone);
		Instant startInstant = zonedStartDateTime.toInstant();

		// Convert the end date/time from the UI panel to a UTC Instant using the system time zone
		LocalDateTime localEnd = _endDateTimePanel.getDateTime().elementAt(0).getLocalDateTime();
		ZonedDateTime zonedEndDateTime = localEnd.atZone(localZone);
		Instant endInstant = zonedEndDateTime.toInstant();

		// Read the optional F-Part override; treat a blank entry as null (no override)
		String fpart = _dssFpartFld.getText().trim();
		if (fpart.isEmpty()) {
			fpart = null;
		}

		// Build the immutable time-series extract parameters using the builder pattern
		MerlinTimeSeriesParameters tsParams = new MerlinTimeSeriesParametersBuilder()
				.withWatershedDirectory(prjPath)
				.withLogFileDirectory(logPath)
				.withAuthenticationParameters(new AuthenticationParametersBuilder()
						.forUrl(GRAB_DATA_URL)
						.setUsername(userName)
						.andPassword(password.toCharArray())
						.build())
				.withStoreOption(getStoreOption())
				.withStart(startInstant)
				.withEnd(endInstant)
				.withFPartOverride(fpart)
				.build();

		// Build profile parameters by copying time-series parameters (shares credentials and time window)
		MerlinProfileParameters profileParams = new MerlinProfileParametersBuilder()
				.fromExistingParameters(tsParams)
				.build();

		// Create the extract action and run it asynchronously; register a completion callback
		RunExtractAction action = new RunExtractAction(this);
		action.extract(this, tsParams, profileParams, selectedPaths,
						"Enter login information for " + GRAB_DATA_URL, GRAB_DATA_URL)
				.thenRun(() ->
				{
					MerlinDataExchangeStatus status = action.getExtractStatus();

					// Invoke the post-update callback only when the extract fully or partially succeeded
					if (status == MerlinDataExchangeStatus.COMPLETE_SUCCESS || status == MerlinDataExchangeStatus.PARTIAL_SUCCESS) {
						_postUpdateAction.run();
					}
				});
	}


	/**
	 * Constructs and returns a StoreOptionImpl configured with the DSS store rule
	 * selected in the store rule combo box.
	 *
	 * The store rule string from the selected StoreTypes constant is passed to
	 * StoreOptionImpl.setRegular() to control how existing DSS records are handled
	 * during the extract.
	 *
	 * @return a StoreOptionImpl set to the currently selected store rule
	 */
	private StoreOptionImpl getStoreOption() {
		// Create a new store option object and apply the selected rule string
		StoreOptionImpl soi = new StoreOptionImpl();
		StoreTypes st = (StoreTypes) _storeRuleCombo.getSelectedItem();
		soi.setRegular(st.getStoreRule());
		return soi;
	}
}
