package usbr.wat.plugins.actionpanel.editors.planning;

import java.awt.Cursor;                                    // Cursor types for showing a wait cursor during import
import java.awt.GridBagConstraints;                         // Layout constraints for positioning components in a GridBagLayout
import java.awt.GridBagLayout;                               // Flexible grid-based layout manager
import java.awt.Window;                                      // Parent window type accepted by the RmaJDialog superclass constructor
import java.awt.event.ActionEvent;                           // Event type delivered to the OK/Cancel command panel listener
import java.io.File;                                          // Represents the file selected via the Import file chooser
import java.util.ArrayList;                                   // Backing list used to build rows read from the imported file
import java.util.List;                                        // Ordered collection interface for the imported rows

import javax.swing.JButton;                                   // Import button
import javax.swing.JFileChooser;                              // File chooser dialog used to select the file to import
import javax.swing.JOptionPane;                                // Used to show a validation error if Name is left blank
import javax.swing.JScrollPane;                                // Scroll container for the data grid
import javax.swing.JTable;                                    // Grid displaying imported Date/Flow1/Flow2/Flow3 rows
import javax.swing.table.DefaultTableModel;                   // Editable table model backing the data grid

import hec.gui.NameDescriptionPanel;                          // Standard HEC Name/Description input pair, matching the mockup's Name/Description fields

import rma.swing.ButtonCmdPanel;                              // OK/Cancel button row
import rma.swing.ButtonCmdPanelListener;                      // Listener interface for the OK/Cancel button row
import rma.swing.RmaInsets;                                   // Standard GridBagConstraints insets constants
import rma.swing.RmaJDialog;                                  // Base dialog class used throughout this plugin

import com.rma.model.Project;                                 // Used to seed the file chooser in the current project's directory

import usbr.wat.plugins.actionpanel.model.planning.CalSimData;          // The model object this dialog creates or edits
import usbr.wat.plugins.actionpanel.model.planning.PlanningSessionRegistry; // Session-lifetime registry the new/edited dataset is registered into

/**
 * "New CalSim Data" / "Edit CalSim Data" dialog, opened from the CalSim Data row of
 * {@link NewPlanningSetDialog}.
 *
 * Matches the mockup: Name, Description, an Import button, and a Date/Flow 1/Flow 2/Flow 3
 * grid. Import opens a file chooser and populates the grid from the selected file. The
 * actual file parsing format is not yet defined by the source system, so
 * {@link #importFile(File)} is a clearly marked extension point — swap in the real CalSim
 * data reader there once the format is finalized.
 *
 * On OK, the resulting {@link CalSimData} is also registered in the
 * {@link PlanningSessionRegistry} so it remains selectable for other Sets created later in
 * the same WAT session.
 */
@SuppressWarnings("serial")
public class NewCalSimDataDialog extends RmaJDialog {

	// Column headers for the data grid, matching the mockup exactly
	private static final String[] COLUMN_NAMES = {"Date", "Flow 1", "Flow 2", "Flow 3"};

	// Name/Description input pair
	private NameDescriptionPanel _nameDescPanel;

	// Triggers the file import
	private JButton _importButton;

	// Backing model for the data grid
	private DefaultTableModel _tableModel;

	// Displays the imported rows
	private JTable _table;

	// Standard OK/Cancel button row
	private ButtonCmdPanel _cmdPanel;

	// True unless the user successfully completes the dialog via OK
	private boolean _canceled = true;

	// The dataset being created or edited; non-null only when editing an existing entry
	private CalSimData _calSimData;

	// Last directory browsed to, so repeated imports start from the same place
	private String _lastDir;

	/**
	 * Constructs the dialog for creating a new CalSim dataset.
	 *
	 * @param parent the owning window
	 */
	public NewCalSimDataDialog(Window parent) {
		this(parent, null); // Delegate to the edit-capable constructor with a null (new-entry) dataset
	}

	/**
	 * Constructs the dialog for creating or editing a CalSim dataset.
	 *
	 * @param parent      the owning window
	 * @param calSimData  the dataset to edit, or null to create a new one
	 */
	public NewCalSimDataDialog(Window parent, CalSimData calSimData) {
		super(parent, true); // Modal dialog, blocking the parent window while shown
		_calSimData = calSimData; // Remember which dataset (if any) we are editing

		setTitle(calSimData == null ? "New CalSim Data" : "Edit CalSim Data"); // Title reflects create vs. edit mode

		buildControls(); // Lay out all Swing components
		addListeners(); // Wire up button/table behavior
		fillForm(); // Populate fields if editing an existing dataset

		pack(); // Size the dialog to fit its preferred layout
		setLocationRelativeTo(parent); // Center the dialog over its parent window
	}

	/**
	 * Builds and lays out all dialog controls using GridBagLayout, matching the mockup's
	 * Name/Description row, Import button + grid row, and OK/Cancel row.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout()); // Use GridBagLayout for flexible row-based placement

		_nameDescPanel = new NameDescriptionPanel(); // Combined Name + Description input widget
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row
		gbc.gridx = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the column
		gbc.gridy = GridBagConstraints.RELATIVE; // Let the layout manager auto-advance the row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // This component takes up the rest of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for this row
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5005; // Standard spacing around the name/description panel
		getContentPane().add(_nameDescPanel, gbc); // Place the name/description panel at the top

		_importButton = new JButton("Import..."); // Triggers the file-import workflow
		gbc.gridx = 0; // Start of a new row, first column
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row down
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the button itself
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the button
		getContentPane().add(_importButton, gbc); // Place the Import button

		_tableModel = new DefaultTableModel(COLUMN_NAMES, 0); // Start with zero rows and the four named columns
		_table = new JTable(_tableModel); // Swing table bound to the model above
		JScrollPane scrollPane = new JScrollPane(_table); // Make the grid scrollable once it has many rows
		scrollPane.setPreferredSize(new java.awt.Dimension(600, 300)); // Give the grid a sensible default size
		gbc.gridx = GridBagConstraints.RELATIVE; // Continue on the same row as Import, next column
		gbc.gridy = GridBagConstraints.RELATIVE; // Same row as Import
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Allow vertical growth so the grid absorbs extra dialog height
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the grid
		getContentPane().add(scrollPane, gbc); // Place the scrollable grid

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS); // Standard OK/Cancel button row
		gbc.gridx = GridBagConstraints.RELATIVE; // Next row
		gbc.gridy = GridBagConstraints.RELATIVE; // Next row
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Fill the remainder of the row
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth for the button row
		gbc.anchor = GridBagConstraints.SOUTHWEST; // Anchor to the bottom-left, matching dialog conventions
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch horizontally
		gbc.insets = RmaInsets.INSETS5555; // Standard spacing around the button row
		getContentPane().add(_cmdPanel, gbc); // Place the OK/Cancel row at the bottom
	}

	/**
	 * Attaches the Import button and OK/Cancel listeners.
	 */
	private void addListeners() {
		_importButton.addActionListener(e -> importAction()); // Clicking Import triggers the file chooser + parse

		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) { // Dispatch based on which button in the command row was clicked
					case ButtonCmdPanel.OK_BUTTON:
						saveForm(); // Validate and commit the form's current state
						break;
					case ButtonCmdPanel.CANCEL_BUTTON:
						_canceled = true; // Record that the user backed out without saving
						setVisible(false); // Hide the dialog, returning control to the caller
						break;
				}
			}
		});
	}

	/**
	 * Populates the form fields from the dataset being edited, if any.
	 */
	private void fillForm() {
		if (_calSimData == null) {
			return; // Nothing to pre-populate when creating a brand-new dataset
		}

		_nameDescPanel.setName(_calSimData.getName()); // Show the existing name
		_nameDescPanel.setDescription(_calSimData.getDescription()); // Show the existing description

		_tableModel.setRowCount(0); // Clear any placeholder rows before repopulating
		for (Object[] row : _calSimData.getRows()) { // Walk every row already stored on the dataset
			_tableModel.addRow(row); // Add it to the visible grid
		}
	}

	/**
	 * Opens a file chooser and imports the selected file's data into the grid.
	 */
	private void importAction() {
		// Start browsing from the last-used directory, or the project directory on first use
		String dir = _lastDir != null ? _lastDir : Project.getCurrentProject().getProjectDirectory();

		JFileChooser chooser = new JFileChooser(dir); // Standard Swing file picker
		int opt = chooser.showOpenDialog(this); // Block until the user picks a file or cancels
		if (opt != JFileChooser.APPROVE_OPTION) {
			return; // User cancelled; nothing to import
		}

		File file = chooser.getSelectedFile(); // The file the user chose
		if (file == null) {
			return; // Defensive guard; should not normally happen after APPROVE_OPTION
		}
		_lastDir = file.getParent(); // Remember the directory for the next import

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // Signal that work is in progress
		try {
			List<Object[]> rows = importFile(file); // Delegate the actual parsing to the extension point
			_tableModel.setRowCount(0); // Clear the grid before loading the freshly imported rows
			for (Object[] row : rows) { // Walk every parsed row
				_tableModel.addRow(row); // Add it to the visible grid
			}
			if (_nameDescPanel.getName() == null || _nameDescPanel.getName().isEmpty()) {
				_nameDescPanel.setName(stripExtension(file.getName())); // Default the name from the file, if not already set
			}
		} finally {
			setCursor(Cursor.getDefaultCursor()); // Always restore the normal cursor, even on failure
		}
	}

	/**
	 * Reads the given file and returns its contents as {@code [date, flow1, flow2, flow3]}
	 * rows for display in the grid.
	 *
	 * <p><b>Extension point:</b> the exact CalSim output format was not specified when this
	 * dialog was implemented. Replace this method's body with a call into the real CalSim
	 * data reader once that format is finalized. In the meantime this returns an empty list
	 * so the dialog remains usable for manually-entered data.</p>
	 *
	 * @param file the file selected via the Import button
	 * @return the parsed rows, or an empty list if the format is not yet wired up
	 */
	protected List<Object[]> importFile(File file) {
		// TODO: wire up the actual CalSim data reader for this project once its format is defined.
		return new ArrayList<>(); // Placeholder: no rows parsed until the real reader is implemented
	}

	/**
	 * Validates and saves the form, creating a new {@link CalSimData} (or updating the one
	 * being edited), registering it in the session registry, and closing the dialog.
	 */
	private void saveForm() {
		String name = _nameDescPanel.getName(); // Read the entered name
		if (name == null || name.trim().isEmpty()) {
			// Block saving until a name is provided; a dataset without a name cannot be selected later
			JOptionPane.showMessageDialog(this, "Please enter a name.", "Name Required", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (_calSimData == null) {
			_calSimData = new CalSimData(); // First save of a brand-new dataset
		}
		_calSimData.setName(name.trim()); // Store the trimmed name
		_calSimData.setDescription(_nameDescPanel.getDescription()); // Store the description as entered

		List<Object[]> rows = new ArrayList<>(); // Collect the grid's current contents
		for (int i = 0; i < _tableModel.getRowCount(); i++) { // Walk every row currently in the grid
			Object[] row = new Object[COLUMN_NAMES.length]; // One array per row, sized to the column count
			for (int c = 0; c < COLUMN_NAMES.length; c++) { // Walk every column within the row
				row[c] = _tableModel.getValueAt(i, c); // Copy the cell's current value
			}
			rows.add(row); // Add the reconstructed row to the list
		}
		_calSimData.setRows(rows); // Replace the dataset's rows with the grid's current contents

		PlanningSessionRegistry.getInstance().addCalSimData(_calSimData); // Make this dataset reusable for other Sets this session

		_canceled = false; // Record that the user completed the dialog successfully
		setVisible(false); // Hide the dialog, returning control to the caller
	}

	/**
	 * Returns whether the dialog was closed without successfully saving.
	 *
	 * @return true if the user cancelled or closed the dialog without clicking OK
	 */
	public boolean isCanceled() {
		return _canceled; // Simple accessor
	}

	/**
	 * Returns the CalSim dataset created or edited by this dialog.
	 *
	 * @return the resulting dataset, or null if the dialog was cancelled before ever saving
	 */
	public CalSimData getCalSimData() {
		return _calSimData; // Simple accessor
	}

	// Removes a trailing file extension (e.g. "data.csv" -> "data") for use as a default name
	private static String stripExtension(String fileName) {
		int dot = fileName.lastIndexOf('.'); // Locate the final dot, if any
		return dot > 0 ? fileName.substring(0, dot) : fileName; // Strip everything from the dot onward, if found
	}
}
