package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;                                         // Provides GridBagConstraints for specifying layout parameters within the GridBagLayout
import java.awt.GridBagLayout;                                              // Provides GridBagLayout as the layout manager for the dialog content pane
import java.awt.Window;                                                     // Provides Window as the parent component type accepted by the superclass constructor
import java.awt.event.ActionEvent;                                          // Provides ActionEvent for the ButtonCmdPanelListener OK/Cancel button callback

import java.util.ArrayList;                                                 // Provides ArrayList for building the list of file filters applied to the file chooser
import java.util.List;                                                      // Provides the List interface for the ordered collection of FileFilter objects

import javax.swing.JLabel;                                                  // Provides JLabel for the "Operations Name:", "Description:", and "Operations File:" field labels
import javax.swing.JOptionPane;                                             // Provides JOptionPane for displaying validation error messages when required fields are empty
import javax.swing.filechooser.FileFilter;                                  // Provides FileFilter as the base type for the file extension filters applied to the file chooser

import com.rma.model.Project;                                               // Provides Project for converting the selected absolute file path to a project-relative path
import com.rma.swing.RmaFileChooserField;                                   // Provides RmaFileChooserField for the combined file path text field and browse button
import rma.swing.ButtonCmdPanel;                                            // Provides ButtonCmdPanel for the OK/Cancel button row at the bottom of the dialog
import rma.swing.ButtonCmdPanelListener;                                    // Provides ButtonCmdPanelListener for handling OK and Cancel button click events
import rma.swing.RmaInsets;                                                 // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJDescriptionField;                                      // Provides RmaJDescriptionField for the multi-line description text field
import rma.swing.RmaJTextField;                                             // Provides RmaJTextField for the operations name text field
import rma.util.RMAFilenameFilter;                                          // Provides RMAFilenameFilter for constructing file extension filters (xlsx, csv) for the file chooser

import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Provides ForecastSimGroup as the simulation group passed to fillForm (used to reset the cancelled state)
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;          // Provides OperationsData as the output data type constructed from the completed form fields

/**
 * A modal import dialog that allows the user to supply a name, an optional description,
 * and a file path (Excel or CSV) for a new operations data record within the WTMP
 * forecast action panel.
 *
 * The dialog presents:
 *
 *   An "Operations Name:" text field for the user-supplied identifier.
 *   A "Description:" multi-line text field for an optional description[
 *   An "Operations File:" file chooser field pre-configured to accept {@code .xlsx}, {@code .csv}, and all file types.
 *   OK and Cancel buttons; OK validates the form and, on success, hides the
 *       dialog with {@code _canceled = false}.
 *
 * After the dialog is dismissed, callers retrieve the resulting {@link OperationsData}
 * object via {@link #getOperationsData()}, which converts the selected absolute file
 * path to a project-relative path before storing it.
 *
 * Extends {@link ImportForecastWindow} to integrate with the common
 * {@link AbstractForecastPanel} import workflow.
 *
 * @see OperationsData
 * @see ImportForecastWindow
 */
public class ImportOperationsWindow extends ImportForecastWindow {
	// Text field for the user-supplied name of the operations data record
	private RmaJTextField _nameFld;

	// Multi-line text field for an optional description of the operations data record
	private RmaJDescriptionField _descFld;

	// Combined file path text field and browse button for selecting the operations file
	private RmaFileChooserField _opsFileFld;

	// OK/Cancel button row at the bottom of the dialog
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Constructs an {@code ImportOperationsWindow} modal dialog, builds all controls,
	 * wires listeners, packs the dialog, sets a fixed size of 500 × 200 pixels, and
	 * centres it over the parent window.
	 *
	 * @param parent the {@link Window} over which this dialog is centred and to which
	 *               it is modal; passed to the {@link ImportForecastWindow} superclass
	 */
	public ImportOperationsWindow(Window parent) {
		// Initialise the superclass as a modal dialog titled "Import Operations Data"
		super(parent, "Import Operations Data", true);

		// Build and lay out all Swing controls
		buildControls();

		// Attach OK/Cancel button listeners
		addListeners();

		// Size the dialog and centre it over the parent
		pack();
		setSize(500, 200);
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out all Swing controls within the dialog's content pane.
	 *
	 * The layout consists, top to bottom, of:
	 *
	 *   An "Operations Name:" label and name text field.
	 *   A "Description:" label and multi-line description text field.
	 *   An "Operations File:" label and a file chooser field pre-configured with
	 *       Excel ({@code .xlsx}) and CSV ({@code .csv}) file extension filters.
	 *   An OK/Cancel {@link ButtonCmdPanel}.
	 *
	 */
	protected void buildControls() {
		// Use GridBagLayout for flexible component placement in the content pane
		getContentPane().setLayout(new GridBagLayout());

		// --- Operations Name row ---
		JLabel label = new JLabel("Operations Name:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

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
		add(_nameFld, gbc);

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
		add(label, gbc);

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
		add(_descFld, gbc);

		// --- Operations File row ---
		label = new JLabel("Operations File:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Configure the file chooser field with Excel and CSV extension filters
		_opsFileFld = new RmaFileChooserField();
		_opsFileFld.setAcceptAllFileFilterUsed(true);

		List<FileFilter> filters = new ArrayList<>();

		// Add an Excel file filter for .xlsx files
		RMAFilenameFilter filter = new RMAFilenameFilter("xlsx", "Excel Files");
		filters.add(filter);

		// Add a CSV file filter for .csv files
		filter = new RMAFilenameFilter("csv", "Comma Separated Files");
		filters.add(filter);

		_opsFileFld.setFilters(filters);
		label.setLabelFor(_opsFileFld);

		// A small non-zero vertical weight allows the file chooser field to size naturally
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.001;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_opsFileFld, gbc);

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
		add(_cmdPanel, gbc);
	}

	/**
	 * Registers all event listeners required by this dialog. Attaches a
	 * ButtonCmdPanelListener to the command panel that handles OK and Cancel button
	 * activations. The OK button validates all required fields before saving and closing
	 * the dialog, while the Cancel button closes the dialog immediately without saving
	 * and marks it as cancelled.
	 */
	protected void addListeners() {
		// Register a command panel listener to handle OK and Cancel button activations
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				// Determine which button was activated and respond accordingly
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate all required fields before accepting the submission
						if (isValidForm()) {
							// Save the form data, mark the dialog as not canceled, and hide it
							saveForm();
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Close without saving and mark the dialog as cancelled
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Saves the current form state to the underlying data model.
	 *
	 * Currently a no-op; operations data is constructed on demand in
	 * {@link #getOperationsData()} rather than stored at submission time.
	 */
	private void saveForm() {
		// No additional state to persist at save time; data is built lazily in getOperationsData()
	}

	/**
	 * Validates that the form can be submitted.
	 *
	 * Checks that:
	 *
	 *   The name field is not empty.
	 *   The operations file path is not empty.
	 *
	 * A plain-message {@link JOptionPane} dialog is shown for each failed check.
	 *
	 * @return {@code true} if both required fields are populated; {@code false} otherwise
	 */
	private boolean isValidForm() {
		// Require a non-empty name
		String name = _nameFld.getText().trim();
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a name",
					"No Name", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		// Require an operations file path to be selected
		String path = _opsFileFld.getPath();
		if (path.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please Select an Operations File",
					"No File Source", JOptionPane.PLAIN_MESSAGE);
			return false;
		}

		return true;
	}

	/**
	 * Returns whether this dialog was closed without a successful form submission.
	 *
	 * @return {@code true} if the dialog was cancelled or closed without submitting;
	 * {@code false} after a valid OK submission
	 */
	@Override
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Resets the dialog to a fresh state in preparation for a new user interaction.
	 *
	 * Sets {@code _canceled = true} so that closing the dialog without clicking OK is
	 * treated as a cancellation. The form fields are not cleared, allowing the user to
	 * review or edit the previously entered values.
	 *
	 * @param fsg the {@link ForecastSimGroup} for the current simulation group; not
	 *            used directly in the current implementation
	 */
	public void fillForm(ForecastSimGroup fsg) {
		// Reset the cancelled flag; it will be cleared to false only on a valid OK submission
		_canceled = true;
	}

	/**
	 * Constructs and returns an {@link OperationsData} object populated from the
	 * current form field values.
	 *
	 * The operations file path stored on the returned object is converted from the
	 * absolute path selected in the file chooser to a project-relative path via
	 * {@link Project#getRelativePath(String)}, making the record portable across
	 * different installation directories.
	 *
	 * @return a new {@link OperationsData} instance with the name, description, and
	 * project-relative operations file path from the form
	 */
	public OperationsData getOperationsData() {
		OperationsData opsData = new OperationsData();

		// Set the name and description from the trimmed text field values
		opsData.setName(_nameFld.getText().trim());
		opsData.setDescription(_descFld.getText().trim());

		// Convert the selected absolute operations file path to a project-relative path
		String opsPath = _opsFileFld.getPath().trim();
		String relOpsPath = Project.getCurrentProject().getRelativePath(opsPath);
		opsData.setOperationsFile(relOpsPath);

		return opsData;
	}
}
