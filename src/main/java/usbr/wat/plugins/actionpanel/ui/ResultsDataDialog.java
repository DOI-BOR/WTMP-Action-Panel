package usbr.wat.plugins.actionpanel.ui;

import java.awt.Dialog;              // AWT Dialog reference used as the parent for this modal dialog
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.event.ActionEvent;   // Event object fired when a button is activated
import java.util.Date;               // Captures the current timestamp when the results are saved

import javax.swing.JLabel;           // Swing label for field headings and the simulation name banner
import javax.swing.JOptionPane;      // Displays validation error dialogs to the user
import javax.swing.JScrollPane;      // Scroll pane wrapping the multi-line description text area

import com.rma.io.FileManagerImpl;   // RMA file manager used to check whether a results folder already exists

import hec2.wat.model.WatSimulation; // WAT simulation model object whose results this dialog is creating

import rma.swing.ButtonCmdPanel;          // RMA panel providing standard OK and Cancel buttons
import rma.swing.ButtonCmdPanelListener;  // Listener interface for ButtonCmdPanel button events
import rma.swing.RmaInsets;               // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJDialog;              // RMA base dialog class providing common dialog behaviour
import rma.swing.RmaJTextArea;            // RMA multi-line text area used for the description input
import rma.swing.RmaJTextField;           // RMA single-line text field used for the results name input

import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction; // Provides the results folder path for a given simulation and name
import usbr.wat.plugins.actionpanel.model.ResultsData;                   // Model object holding all metadata for a saved simulation results snapshot


/**
 * Modal dialog for capturing the name and description of a new simulation results snapshot.
 *
 * The dialog is presented when the user initiates a "Save Results" action on a
 * WatSimulation. It collects two pieces of information:
 *   Results Name   -- a short identifier that becomes part of the results folder path.
 *   Description    -- an optional free-text description of the results.
 *
 * Validation is performed on OK:
 *   The name field must not be blank.
 *   The derived results folder must not already exist on disk (prevents duplicate names).
 *
 * After dismissal, callers should check isCanceled() and, if false, call
 * getResultsData() to retrieve a fully populated ResultsData object ready for saving.
 *
 * The dialog is fixed at 500 x 300 pixels and is centred relative to its parent.
 *
 */
public class ResultsDataDialog extends RmaJDialog {
	/**
	 * The simulation for which new results are being saved.
	 */
	private WatSimulation _sim;

	/**
	 * Single-line text field for the user-supplied results name.
	 */
	private RmaJTextField _resultsNameFld;

	/**
	 * Multi-line text area for the optional results description.
	 */
	private RmaJTextArea _resultsDescFld;

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
	 * Constructs the dialog for the given simulation, builds all controls, attaches
	 * listeners, and positions it relative to the parent dialog.
	 *
	 * @param parent the owning Dialog used to centre the dialog and establish modality
	 * @param sim    the WatSimulation for which new results are being created;
	 *               must not be null
	 */
	public ResultsDataDialog(Dialog parent, WatSimulation sim) {
		super(parent, "Enter Results Information", true);

		// Store the simulation reference for name display, folder derivation, and metadata
		_sim = sim;

		buildControls();
		addListeners();

		// Size to preferred layout then override to a fixed size
		pack();
		setSize(500, 300);

		// Centre the dialog over the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Constructs and lays out all child controls using GridBagLayout.
	 *
	 * Layout from top to bottom:
	 * Banner label showing the simulation name.
	 * "Results Name:" label paired with a single-line text field.
	 * "Description:" label paired with a scrollable multi-line text area.
	 * OK/Cancel button panel anchored to the bottom.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout());

		// --- Banner label identifying which simulation these results belong to ---
		JLabel label = new JLabel("Create New Results for " + _sim.getName());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- "Results Name:" label ---
		label = new JLabel("Results Name:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- Results name text field (expands horizontally to fill remaining row space) ---
		_resultsNameFld = new RmaJTextField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_resultsNameFld, gbc);

		// --- "Description:" label ---
		label = new JLabel("Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- Description text area in a scroll pane (expands both directions to fill space) ---
		_resultsDescFld = new RmaJTextArea();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_resultsDescFld), gbc);

		// --- OK/Cancel button panel anchored to the bottom of the dialog ---
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
						// Discard any input and close the dialog
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
	 * Two checks are performed in order:
	 * 1. The results name field must not be blank.
	 * 2. The derived results folder path must not already exist on disk, ensuring
	 * each set of results has a unique name within the simulation directory.
	 *
	 * An informational dialog is shown for each failed check so the user knows what
	 * to correct.
	 *
	 * @return true if both checks pass and the dialog may be closed; false otherwise
	 */
	protected boolean isValidForm() {
		String name = _resultsNameFld.getText().trim();

		// Check 1: the results name must not be blank
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please Enter a results name",
					"No Name", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Check 2: a results folder derived from the name must not already exist
		String resultsFolder = SaveSimulationResultsAction.getResultsFolder(_sim, name);
		if (FileManagerImpl.getFileManager().fileExists(resultsFolder)) {
			JOptionPane.showMessageDialog(this,
					"Results for " + name + " already exist. Please enter a unique name",
					"Duplicate Name", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		return true;
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


	/**
	 * Constructs and returns a ResultsData object populated from the dialog's fields
	 * and the associated simulation's metadata.
	 *
	 * Should only be called after the dialog has been dismissed with OK (i.e.
	 * isCanceled() returns false). The populated fields are:
	 * name             -- trimmed text from the results name field.
	 * description      -- trimmed text from the description field.
	 * savedBy          -- the current OS user name from the "user.name" system property.
	 * savedAt          -- the current date and time at the moment of the call.
	 * lastComputedTime -- the simulation's own last-computed timestamp.
	 * resultsFolder    -- the folder path derived from the simulation and name.
	 *
	 * @return a fully populated ResultsData object ready to be persisted; never null
	 */
	public ResultsData getResultsData() {
		String name = _resultsNameFld.getText().trim();
		String resultsFolder = SaveSimulationResultsAction.getResultsFolder(_sim, name);

		// Construct the results data object with the simulation reference and folder path
		ResultsData data = new ResultsData(_sim, resultsFolder);

		data.setName(name);
		data.setDescription(_resultsDescFld.getText().trim());

		// Record the OS-level user who triggered the save
		data.setSavedBy(System.getProperty("user.name"));

		// Capture the current wall-clock time as the save timestamp
		data.setSavedAt(new Date());

		// Carry the simulation's own last-computed time into the results record
		data.setLastComputedTime(_sim.getLastComputedDate());

		return data;
	}
}
