package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Window;              // AWT base class for top-level windows; used as the parent reference
import java.awt.event.ActionEvent;   // Event object fired when a button is activated

import javax.swing.JLabel;           // Swing label for static headings, dynamic read-only values, and a spacer
import javax.swing.JPanel;           // General-purpose container for the lower editable-fields sub-panel
import javax.swing.JSeparator;       // Horizontal visual divider between the info section and the editable fields

import hec2.wat.model.WatSimulation; // WAT simulation model object whose name and description are copied as defaults

import rma.swing.ButtonCmdPanel;          // RMA panel providing standard OK and Cancel buttons
import rma.swing.ButtonCmdPanelListener;  // Listener interface for ButtonCmdPanel button events
import rma.swing.RmaInsets;               // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJDescriptionField;    // RMA text field intended for description input
import rma.swing.RmaJDialog;              // RMA base dialog class providing common dialog behaviour
import rma.swing.RmaJTextField;           // RMA single-line text field for the new simulation name prefix

import usbr.wat.plugins.actionpanel.model.SimulationGroup; // Simulation group whose name is appended as a suffix to the new simulation name


/**
 * Modal dialog for saving a simulation under a new name within the same simulation group.
 *
 * The dialog is divided into two visual sections separated by a horizontal line:
 *
 *   Info section (read-only) -- displays the current simulation group name and the
 *   name of the source simulation being copied.
 *
 *   Edit section -- provides:
 *     A "Name:" text field where the user enters a prefix for the new simulation name.
 *     A read-only label showing the group name suffix that will be appended automatically
 *     (e.g. "-GroupName"), so the user can see the full resulting name.
 *     A "Description:" field pre-populated from the source simulation.
 *
 * After fillForm is called to populate the read-only labels and defaults, the dialog
 * is shown modally. On OK, callers should check isCanceled() and, if false, retrieve
 * the composed name via getSaveAsName() and the description via getDescription().
 *
 * The dialog is fixed at 400 x 200 pixels and is centred relative to its parent window.
 *
 */
public class SaveSimulationAsDialog extends RmaJDialog {
	/**
	 * Read-only label displaying the name of the target simulation group.
	 */
	private JLabel _simGroupLbl;

	/**
	 * Read-only label displaying the name of the source simulation being copied.
	 */
	private JLabel _srcSimLbl;

	/**
	 * Text field where the user enters the prefix portion of the new simulation name.
	 */
	private RmaJTextField _newNameFld;

	/**
	 * Read-only label showing the group name suffix (e.g. "-GroupName") that will be
	 * appended to the user-entered prefix to form the full new simulation name.
	 * Its tooltip reminds the user that this text will be appended automatically.
	 */
	private JLabel _simGrpNamePartLbl;

	/**
	 * Description field pre-populated from the source simulation; editable by the user.
	 */
	private RmaJDescriptionField _newDescFld;

	/**
	 * Panel containing the OK and Cancel buttons.
	 */
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Flag indicating whether the user dismissed the dialog via Cancel or OK.
	 * Defaults to false; set to true when Cancel is clicked and to false when
	 * OK is clicked after successful validation.
	 */
	protected boolean _canceled;


	/**
	 * Constructs the dialog, builds all controls, attaches listeners, and positions
	 * it relative to the parent window.
	 *
	 * After construction, call fillForm(SimulationGroup, WatSimulation) before
	 * making the dialog visible to pre-populate the read-only info labels and defaults.
	 *
	 * @param parent the owning window used to centre the dialog and establish modality
	 */
	public SaveSimulationAsDialog(Window parent) {
		// Initialise the RMA base dialog as modal
		super(parent, true);

		buildControls();
		addListeners();

		// Size to preferred layout then override to a fixed compact size
		pack();
		setSize(400, 200);

		// Centre the dialog over the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Constructs and lays out all child controls using GridBagLayout.
	 *
	 * The layout is split into two regions added to the content pane:
	 *
	 * Upper info region (directly in the content pane):
	 * "Simulation Group:" label and read-only value label.
	 * "Source Simulation:" label and read-only value label.
	 * A horizontal JSeparator.
	 *
	 * Lower edit region (a nested JPanel):
	 * "Name:" label, editable name prefix field, and read-only group-name suffix label.
	 * "Description:" label and editable description field.
	 * A zero-height spacer label that absorbs any extra vertical space.
	 *
	 * OK/Cancel button panel anchored at the bottom of the content pane.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Save Simulation As");

		// --- "Simulation Group:" static label ---
		JLabel label = new JLabel("Simulation Group :");
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

		// --- Simulation group name value label (populated by fillForm) ---
		_simGroupLbl = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupLbl, gbc);

		// --- "Source Simulation:" static label ---
		label = new JLabel("Source Simulation :");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- Source simulation name value label (populated by fillForm) ---
		_srcSimLbl = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_srcSimLbl, gbc);

		// --- Horizontal separator dividing the read-only info from the editable fields ---
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// --- Lower edit panel containing Name, suffix label, and Description ---
		// Small positive weighty keeps this panel anchored near the separator
		JPanel lowerPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.01;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(lowerPanel, gbc);

		// --- "Name:" label within the lower panel ---
		label = new JLabel("Name :");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(label, gbc);

		// --- Editable name prefix field (expands horizontally) ---
		_newNameFld = new RmaJTextField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5500;
		lowerPanel.add(_newNameFld, gbc);

		// --- Read-only group name suffix label shown directly after the name field ---
		// Tooltip reminds the user this text is appended automatically to the entered prefix
		_simGrpNamePartLbl = new JLabel();
		_simGrpNamePartLbl.setToolTipText("Will be appended to the name entered");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5005;
		lowerPanel.add(_simGrpNamePartLbl, gbc);

		// --- "Description:" label within the lower panel ---
		label = new JLabel("Description :");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(label, gbc);

		// --- Editable description field (expands horizontally) ---
		_newDescFld = new RmaJDescriptionField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(_newDescFld, gbc);

		// --- Invisible spacer label absorbing any remaining vertical space in lowerPanel ---
		label = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0001;  // Tiny positive weight pushes the button panel to the very bottom
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		lowerPanel.add(label, gbc);

		// --- OK/Cancel button panel anchored to the bottom of the content pane ---
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
	 * Registers all event listeners required by this dialog. Currently attaches a
	 * ButtonCmdPanelListener to the command panel that handles OK and Cancel button
	 * activations. The OK button closes the dialog only if form validation passes,
	 * while the Cancel button discards any input and closes the dialog immediately.
	 */
	private void addListeners() {
		// Attach a command panel listener to handle button activations
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			/**
			 * Dispatched on the EDT when any button in the command panel is activated.
			 *
			 * @param e the action event carrying the button ID via ActionEvent.getID()
			 */
			public void buttonCmdActionPerformed(ActionEvent e) {
				// Determine which button was activated and respond accordingly
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Only close the dialog if the entered data passes validation
						if (isValidForm()) {
							// Mark the dialog as not canceled and hide it
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Discard any input and close the dialog immediately
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Validates the form fields before allowing the dialog to close on OK.
	 *
	 * Currently always returns true; validation logic is pending implementation.
	 * Future checks should include verifying that the name field is not blank and
	 * that the composed simulation name does not already exist in the group.
	 *
	 * @return true always (validation not yet implemented)
	 */
	private boolean isValidForm() {
		return true;
	}


	/**
	 * Populates the read-only info labels and pre-fills editable fields with defaults
	 * derived from the given simulation group and source simulation.
	 *
	 * Should be called before the dialog is made visible. After this call:
	 * The simulation group label shows simGroup.getName().
	 * The source simulation label shows srcSim.getName().
	 * The description field is pre-filled with srcSim.getDescription().
	 * The suffix label shows "-" + simGroup.getName() to preview the full name.
	 *
	 * @param simGroup the simulation group that will own the new simulation
	 * @param srcSim   the source simulation being copied; its name and description are
	 *                 used as display values and defaults
	 */
	public void fillForm(SimulationGroup simGroup, WatSimulation srcSim) {
		// Display the group name in the read-only info row
		_simGroupLbl.setText(simGroup.getName());

		// Display the source simulation name in the read-only info row
		_srcSimLbl.setText(srcSim.getName());

		// Pre-fill the description with the source simulation's existing description
		_newDescFld.setText(srcSim.getDescription());

		// Show the group name suffix so the user can preview the full composed name
		_simGrpNamePartLbl.setText("-" + simGroup.getName());
	}


	/**
	 * Returns the full new simulation name by concatenating the user-entered prefix
	 * with the group name suffix shown in the suffix label.
	 *
	 * For example, if the user types "Run01" and the group name is "Calibration",
	 * this method returns "Run01-Calibration".
	 *
	 * @return the composed simulation name; never null but may be empty if the
	 * name field was left blank
	 */
	public String getSaveAsName() {
		// Combine the user-entered prefix with the automatically appended group name suffix
		String name = _newNameFld.getText();
		name = name + _simGrpNamePartLbl.getText();
		return name;
	}


	/**
	 * Returns the description text entered by the user.
	 *
	 * @return the description string from the description field; never null but may
	 * be empty if the field was cleared
	 */
	public String getDescription() {
		return _newDescFld.getText();
	}


	/**
	 * Returns whether the dialog was dismissed via the Cancel button.
	 *
	 * @return true if the user clicked Cancel; false if the user clicked OK and
	 * the form passed validation
	 */
	public boolean isCanceled() {
		return _canceled;
	}

}
