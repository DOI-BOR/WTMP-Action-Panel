package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagConstraints;       // Defines positioning and sizing constraints for components in a GridBagLayout
import java.awt.GridBagLayout;            // Flexible grid-based layout manager for arranging UI components
import java.awt.event.ActionEvent;        // Represents an action event fired when a button is clicked
import java.awt.event.ItemEvent;          // Event fired when the selector panel's selection changes
import java.util.ArrayList;              // Resizable-array List implementation used for data location and row-index collections
import java.util.List;                   // Ordered collection interface for data location lists
import java.util.Vector;                 // Synchronized growable array used to collect time-series data for plotting

import javax.swing.JButton;              // Standard Swing push-button component
import javax.swing.JLabel;               // Non-interactive text label component
import javax.swing.JOptionPane;          // Provides standard confirmation and informational dialog boxes
import javax.swing.JPanel;              // Generic lightweight container for grouping action buttons
import javax.swing.JSeparator;          // Horizontal visual divider used between the model DSS and iteration DSS sections
import javax.swing.SwingUtilities;       // Swing utility methods, used here for locating the owning window

import com.rma.io.DssFileManagerImpl;    // RMA concrete DSS file manager for reading time-series records
import com.rma.model.Project;            // Represents the currently loaded RMA project and its file paths

import hec.gfx2d.G2dDialog;             // HEC dialog for displaying 2D graphical time-series plots
import hec.gui.SelectorPanel;           // HEC panel providing a scrollable, named-item selection list
import hec.heclib.dss.HecTimeSeries;    // HEC DSS time-series reader for fetching records within a time window
import hec.heclib.util.HecTime;         // HEC time representation used for DSS time-range endpoints
import hec.io.DSSIdentifier;            // Encapsulates a DSS file name and path for identifying a DSS record
import hec.io.TimeSeriesCollectionContainer; // Container holding multiple time-series records read in a collection read
import hec.io.TimeSeriesContainer;      // Container holding a single time-series dataset read from a DSS file
import hec.lang.NamedType;             // Base class for named objects; extended by the inner DataLocationNamedType

import hec2.model.DataLocation;         // Represents a model data location (input/output point) in HEC2

import rma.swing.ButtonCmdPanel;        // Panel containing standard command buttons (Close, OK, Cancel, etc.)
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaImage;              // RMA utility for loading image icons from classpath resources
import rma.swing.RmaInsets;             // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJDialog;            // Base class for RMA dialog windows
import rma.swing.RmaJTable;             // RMA-extended table component with utility row management methods
import rma.swing.RmaJTextField;         // RMA-extended text field component
import rma.util.RMASort;                // RMA quicksort utility for sorting parallel lists by a key list


/**
 * Detail editor dialog for a single Boundary Condition (BC) entry within the
 * WTMP Action Panel's iteration settings.
 *
 * Displays a read-only summary of the model DSS file, model DSS path, and
 * parameter for the selected BC row, alongside editable fields for the
 * iteration DSS file and path. A SelectorPanel on the left lists all available
 * BC locations, allowing the user to navigate between rows without closing the
 * dialog.
 *
 * The dialog exposes a Browse DSS button (which delegates to IterationBcPanel),
 * a Clear DSS button for removing the iteration record, and a plot button for
 * previewing the original and iteration time-series side-by-side in a G2dDialog.
 *
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class BcEntryDialog extends RmaJDialog {
	// The BC table shown in the parent IterationBcPanel; shared with this dialog for selection sync
	private RmaJTable _bcTable;

	// The parent IterationBcPanel that owns this dialog; used to delegate browse and clear actions
	private IterationBcPanel _panel;

	// Side-panel providing a sorted list of all BC data locations for navigation
	private SelectorPanel _selectorPanel;

	// Read-only field showing the model DSS file name for the selected BC row
	private RmaJTextField _modelDssFileFld;

	// Read-only field showing the model DSS path for the selected BC row
	private RmaJTextField _modelDssPathFld;

	// Read-only field showing the iteration DSS file name for the selected BC row
	private RmaJTextField _iterationDssFileFld;

	// Read-only field showing the iteration DSS path for the selected BC row
	private RmaJTextField _iterationDssPathFld;

	// Button that opens the DSS browser to select an iteration DSS record
	private JButton _browseDssBtn;

	// Button that clears the iteration DSS file and path for the current BC row
	private JButton _clearDssBtn;

	// Panel containing the Close button
	private ButtonCmdPanel _cmdPanel;

	// Read-only field showing the parameter name for the selected BC row
	private RmaJTextField _parameterFld;

	// Guard flag preventing re-entrant form-fill operations during programmatic population
	private boolean _fillingForm;

	// Guard flag preventing re-entrant selection synchronization between the table and selector panel
	private boolean _selectionChanging;

	// Button that plots the model and iteration time-series for the selected row
	private JButton _plotBtn;

	/**
	 * Constructs a BcEntryDialog associated with the given BC panel and table,
	 * opening initially to the specified table row.
	 *
	 * Builds all UI controls, populates the selector panel from the table,
	 * selects and fills the form for the initial row, attaches listeners,
	 * sizes the dialog, and centers it relative to its owning window.
	 *
	 * @param iterationBcPanel the parent IterationBcPanel that owns this dialog
	 * @param bcTable          the RmaJTable containing the BC rows to display
	 * @param tableRow         the zero-based row index in the BC table to open initially
	 */
	public BcEntryDialog(IterationBcPanel iterationBcPanel, RmaJTable bcTable,
	                     int tableRow) {
		// Find the owning window of the panel to use as the dialog parent
		super(SwingUtilities.windowForComponent(iterationBcPanel));

		// Store references to the parent panel and shared BC table
		_panel = iterationBcPanel;
		_bcTable = bcTable;

		// Build and arrange all UI components
		buildControls();

		// Populate the selector panel list from all rows in the BC table
		fillForm(bcTable);

		// Programmatically select the target row in the selector panel
		_selectorPanel.setSelectedIndex(tableRow);

		// Populate the DSS fields for the initially selected row
		fillForm(tableRow);

		// Attach action and selection listeners to interactive controls
		addListeners();

		// Size the dialog to preferred layout, then override with a fixed size
		pack();
		setSize(580, 310);

		// Center the dialog relative to its owning window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Uses a GridBagLayout to place: a selector panel spanning the full width,
	 * read-only model DSS file and path fields, a parameter field, a horizontal
	 * separator, read-only iteration DSS file and path fields, a row of action
	 * buttons (plot, browse, clear), and a Close button panel at the bottom.
	 */
	private void buildControls() {
		// Set the dialog title
		setTitle("Edit Boundary Condition");

		// Apply GridBagLayout to the content pane
		getContentPane().setLayout(new GridBagLayout());

		// Create the selector panel in single-line layout mode without sorting or description
		_selectorPanel = new SelectorPanel(SelectorPanel.ONE_LINE_LAYOUT);
		_selectorPanel.setSortingEnabled(false);
		_selectorPanel.setDescriptionPanelVisible(false);

		// Configure constraints for the selector panel to span the full row
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.insets(5, 0, 5, 5);
		getContentPane().add(_selectorPanel, gbc);

		// Create the "Model DSS File:" label
		JLabel label = new JLabel("Model DSS File:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only model DSS file path field
		_modelDssFileFld = new RmaJTextField();
		_modelDssFileFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_modelDssFileFld, gbc);

		// Create the "Model DSS Path:" label
		label = new JLabel("Model DSS Path:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(label, gbc);

		// Create the read-only model DSS path field
		_modelDssPathFld = new RmaJTextField();
		_modelDssPathFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_modelDssPathFld, gbc);

		// Create the "Parameter:" label
		label = new JLabel("Parameter:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only parameter name field
		_parameterFld = new RmaJTextField();
		_parameterFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_parameterFld, gbc);

		// Add a horizontal separator to visually divide the model DSS section from the iteration DSS section
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the "Iteration DSS File:" label
		label = new JLabel("Iteration DSS File:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only iteration DSS file path field
		_iterationDssFileFld = new RmaJTextField();
		_iterationDssFileFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_iterationDssFileFld, gbc);

		// Create the "Iteration DSS Path:" label
		label = new JLabel("Iteration DSS Path:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only iteration DSS path field
		_iterationDssPathFld = new RmaJTextField();
		_iterationDssPathFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_iterationDssPathFld, gbc);

		// Create the action button row panel (plot, browse, clear)
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.001;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(panel, gbc);

		// Create the plot button with the standard plot icon
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

		// Create the Browse DSS button for selecting an iteration DSS record
		_browseDssBtn = new JButton("Browse DSS");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_browseDssBtn, gbc);

		// Create the Clear DSS button with a descriptive tooltip
		_clearDssBtn = new JButton("Clear DSS");
		_clearDssBtn.setToolTipText("Clear the Iteration DSS File and Path");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_clearDssBtn, gbc);

		// Create the Close button panel at the bottom of the dialog
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
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
	 * Registers listeners for: selector panel item changes, Browse DSS and
	 * Clear DSS buttons, BC table row selection, the plot button, and the
	 * Close button via the command panel.
	 */
	private void addListeners() {
		// Respond to selector panel item selection changes
		_selectorPanel.addItemListener(e -> selectionChanged(e));

		// Delegate DSS browsing to the parent IterationBcPanel
		_browseDssBtn.addActionListener(e -> browseDssAction());

		// Prompt user and clear the iteration DSS entry when Clear DSS is clicked
		_clearDssBtn.addActionListener(e -> clearDssAction());

		// Synchronize the selector panel when the BC table selection changes
		_bcTable.getSelectionModel().addListSelectionListener(e -> tableRowSelected());

		// Launch the time-series plot dialog when the plot button is clicked
		_plotBtn.addActionListener(e -> plotRecords());

		// Handle Close button to hide the dialog
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.CLOSE_BUTTON:
						// Hide the dialog without saving; changes are written directly to the table
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Plots the model (original) and iteration time-series records for the
	 * currently selected BC location in a G2dDialog embedded in an RmaJDialog.
	 *
	 * Reads the original time-series directly. For the iteration DSS record,
	 * queries the time range first and uses HecTimeSeries to read all records
	 * within that window (capturing multi-member iteration data). Both datasets
	 * are passed to the G2dDialog for rendering.
	 */
	private void plotRecords() {
		DataLocationNamedType dl;
		DSSIdentifier origDssId, iterDssId;
		TimeSeriesContainer origTs;

		// Get the currently selected data location from the selector panel
		dl = (DataLocationNamedType) _selectorPanel.getSelectedItem();

		// Build DSS identifiers from the currently displayed field values
		origDssId = new DSSIdentifier(_modelDssFileFld.getText(), _modelDssPathFld.getText());
		iterDssId = new DSSIdentifier(_iterationDssFileFld.getText(), _iterationDssPathFld.getText());

		// Read the original (model) time-series record
		origTs = DssFileManagerImpl.getDssFileManager().readTS(origDssId, true);

		// Initialize the plot data vector with the original time-series
		Vector data = new Vector();
		data.add(origTs);

		// Read the iteration time-series collection if a DSS path has been specified
		if (iterDssId.getDSSPath() != null && !iterDssId.getDSSPath().isEmpty()) {
			// Resolve the iteration DSS file to an absolute path
			DSSIdentifier dss2 = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(iterDssId.getFileName()), iterDssId.getDSSPath());

			// Get the time range available for this record
			HecTime[] times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dss2, 0);

			if (times != null && times.length == 2) {
				// Open a HecTimeSeries reader scoped to the full available time window
				HecTimeSeries hecTs = new HecTimeSeries(dss2.getFileName());
				hecTs.setTimeWindow(times[0], times[1]);
				hecTs.setPathname(iterDssId.getDSSPath());

				// Read all matching records into a collection container
				TimeSeriesCollectionContainer tscc = new TimeSeriesCollectionContainer();

				if (hecTs.read(tscc, true, false) == 0) {
					// Add each individual time-series member to the plot data
					TimeSeriesContainer[] tscs = tscc.get();
					if (tscs != null) {
						for (int i = 0; i < tscs.length; i++) {
							data.add(tscs[i]);
						}
					}
				}
			}
		}

		// Create a G2dDialog with the collected data and transfer its content to an RmaJDialog for display
		G2dDialog g2dDlg = new G2dDialog(null, dl.getName(), false, data);
		RmaJDialog dlg = new RmaJDialog(SwingUtilities.windowForComponent(this), dl.getName(), true);
		dlg.pack();
		dlg.setSize(500, 500);

		// Transfer the menu bar and content pane from the G2dDialog to the RmaJDialog wrapper
		dlg.setJMenuBar(g2dDlg.getJMenuBar());
		dlg.setContentPane(g2dDlg.getContentPane());

		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
	}

	/**
	 * Responds to BC table row selection changes by synchronizing the selector panel.
	 *
	 * Guarded by _selectionChanging to prevent re-entrant updates between the
	 * table and selector panel. Clears the selector panel selection if no row
	 * is selected in the table.
	 */
	private void tableRowSelected() {
		// Ignore the event if a programmatic selection change is already in progress
		if (_selectionChanging) {
			return;
		}

		int selectedRow = _bcTable.getSelectedRow();

		if (selectedRow == -1) {
			// No row selected; clear the selector panel selection
			_selectorPanel.setSelectedIndex(-1);
			return;
		}

		// Synchronize the selector panel to the newly selected table row
		_selectorPanel.setSelectedIndex(selectedRow);
	}


	/**
	 * Delegates the DSS browse action to the parent IterationBcPanel.
	 *
	 * The parent panel opens the DSS browser and updates the selected BC row
	 * with the chosen DSS record.
	 */
	private void browseDssAction() {
		_panel.browseDSSAction(this);
	}

	/**
	 * Prompts the user for confirmation and clears the iteration DSS entry
	 * for the currently selected BC row if confirmed.
	 *
	 * Delegates the actual row clear to the parent IterationBcPanel and then
	 * blanks the local iteration DSS field displays.
	 */
	private void clearDssAction() {
		int opt = JOptionPane.showConfirmDialog(this, "Clear the Iteration DSS File and Path?", "Confirm", JOptionPane.YES_NO_OPTION);

		if (opt == JOptionPane.YES_OPTION) {
			// Delegate the clear operation to the parent panel for the selected row
			_panel.clearRow(_selectorPanel.getSelectedIndex());

			// Clear the local field displays to reflect the removal
			_iterationDssFileFld.setText("");
			_iterationDssPathFld.setText("");
		}
	}


	/**
	 * Responds to selector panel item selection changes.
	 *
	 * Ignores DESELECTED events and re-entrant calls while the form is being
	 * populated. Fills the DSS detail fields for the newly selected BC location.
	 *
	 * @param e the ItemEvent describing the selection state change
	 */
	private void selectionChanged(ItemEvent e) {
		// Ignore the DESELECTED event that fires for the previously selected item
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Skip if the form is currently being populated programmatically
		if (_fillingForm) {
			return;
		}

		int idx = _selectorPanel.getSelectedIndex();

		// Set the guard flag before any table interaction to prevent re-entrant events
		_selectionChanging = true;
		try {
			//_bcTable.setSelectedIndices(idx);
		} finally {
			// Always clear the guard flag
			_selectionChanging = false;
		}

		// Populate the detail fields for the newly selected row
		fillForm(idx);
	}


	/**
	 * Populates the DSS detail fields for the given BC table row.
	 *
	 * Reads the parameter, model DSS identifier, and iteration DSS identifier
	 * from the specified row and updates each read-only display field. Clears
	 * the form if the row index is negative.
	 *
	 * @param row the zero-based BC table row index to display
	 */
	private void fillForm(int row) {
		if (row < 0) {
			// No valid row; clear all display fields
			clearForm();
			return;
		}

		// Read the parameter name, model DSS identifier, and iteration DSS identifier from the table row
		String parameter = (String) _bcTable.getValueAt(row, IterationBcPanel.PARAMETER_COL);
		DSSIdentifier modelDssId = (DSSIdentifier) _bcTable.getValueAt(row, IterationBcPanel.MODEL_DSS_COL);
		DSSIdentifier selectedDssId = (DSSIdentifier) _bcTable.getValueAt(row, IterationBcPanel.DSSID_COL);

		// Populate each read-only display field from the retrieved values
		_parameterFld.setText(parameter);
		_modelDssFileFld.setText(modelDssId.getFileName());
		_modelDssPathFld.setText(modelDssId.getDSSPath());
		_iterationDssFileFld.setText(selectedDssId.getFileName());

		// Resolve the iteration DSS file to an absolute project path before displaying
		_iterationDssPathFld.setText(Project.getCurrentProject().getAbsolutePath(selectedDssId.getDSSPath()));

		// Clear the modified flag since this is a programmatic fill, not a user edit
		setModified(false);
	}


	/**
	 * Populates the selector panel list from all rows in the BC table.
	 *
	 * Wraps each row's DataLocation in a DataLocationNamedType for display,
	 * builds a parallel row-index list, sorts both lists by row order, and
	 * sets the result as the selector panel's selection list.
	 *
	 * @param bcTable the RmaJTable containing the BC rows to enumerate
	 */
	private void fillForm(RmaJTable bcTable) {
		// Set the guard flag to suppress selection-change events during programmatic population
		_fillingForm = true;
		try {
			int rowCnt = bcTable.getRowCount();
			List<DataLocationNamedType> dlList = new ArrayList<>(rowCnt);
			List<Integer> rowNum = new ArrayList<>(rowCnt);
			DataLocation dl;

			for (int r = 0; r < rowCnt; r++) {
				// Convert view row index to model row index to account for any active sort order
				int x = _bcTable.convertRowIndexToModel(r);
				rowNum.add(x);

				// Wrap the DataLocation in a NamedType so the selector panel can display its name
				dl = (DataLocation) bcTable.getValueAt(r, IterationBcPanel.DATALOCATION_COL);
				dlList.add(new DataLocationNamedType(dl));
			}

			// Sort both lists together so the selector panel displays locations in model row order
			RMASort.quickSort(rowNum, dlList);

			// Set the sorted location list as the selector panel's content
			_selectorPanel.setSelectionList(dlList);
		} finally {
			// Always clear the guard flag when the fill operation completes
			_fillingForm = false;
		}
	}


	/**
	 * Updates the iteration DSS file and path display fields with a newly selected
	 * DSS identifier.
	 *
	 * Called by the parent IterationBcPanel after the user selects a record in
	 * the DSS browser. Clears both fields if the identifier is null.
	 *
	 * @param selectedDssId the DSSIdentifier chosen by the user; null to clear the fields
	 */
	public void setSelectedDssId(DSSIdentifier selectedDssId) {
		if (selectedDssId == null) {
			// Clear both iteration DSS fields when no identifier is provided
			_iterationDssFileFld.setText("");
			_iterationDssPathFld.setText("");
			return;
		}

		// Display the new iteration DSS file and path from the chosen identifier
		_iterationDssFileFld.setText(selectedDssId.getFileName());
		_iterationDssPathFld.setText(selectedDssId.getDSSPath());
	}

	/**
	 * Thin NamedType wrapper around a DataLocation that exposes the location's
	 * name to the SelectorPanel for display purposes.
	 *
	 * Extends NamedType so the selector panel can retrieve a human-readable name
	 * while retaining access to the underlying DataLocation object.
	 */
	class DataLocationNamedType extends NamedType {
		// The wrapped DataLocation instance
		private DataLocation _dataLocation;

		/**
		 * Constructs a DataLocationNamedType wrapping the given DataLocation.
		 *
		 * Passes the location's name to the NamedType superclass constructor.
		 *
		 * @param dataLocation the DataLocation to wrap
		 */
		DataLocationNamedType(DataLocation dataLocation) {
			// Initialize the NamedType with the location's display name
			super(dataLocation.getName());
			_dataLocation = dataLocation;
		}

		/**
		 * Returns the wrapped DataLocation instance.
		 *
		 * @return the DataLocation associated with this named type
		 */
		public DataLocation getDataLocaiton() {
			return _dataLocation;
		}
	}
}
