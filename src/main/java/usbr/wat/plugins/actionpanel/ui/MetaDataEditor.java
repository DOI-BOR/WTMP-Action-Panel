package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Window;              // AWT base class for top-level windows; used as the parent reference
import java.awt.event.ActionEvent;   // Event object fired when a button is activated

import hec.gui.NameDescriptionPanel; // HEC panel that displays Name and Description fields for a model object

import hec2.wat.model.WatSimulation; // WAT simulation model object whose name and description are displayed

import rma.swing.ButtonCmdPanel;          // RMA panel providing standard OK and Cancel buttons
import rma.swing.ButtonCmdPanelListener;  // Listener interface for ButtonCmdPanel button events
import rma.swing.RmaInsets;               // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJDialog;              // RMA base dialog class providing common dialog behaviour


/**
 * Modal dialog for viewing and editing the metadata (name and description) of a
 * WAT simulation.
 *
 * The dialog presents a NameDescriptionPanel pre-populated with the simulation's
 * current name and description. The name field is read-only; only the description
 * is editable. An OK/Cancel button panel is anchored at the bottom.
 *
 * Clicking OK calls saveForm(), which is currently a stub pending full implementation.
 * Clicking Cancel closes the dialog without making any changes.
 *
 * The dialog is fixed at 350 x 350 pixels and is centred relative to its parent
 * window on construction.
 *
 */
public class MetaDataEditor extends RmaJDialog {
	/**
	 * HEC name/description panel displaying the simulation's name (read-only)
	 * and description (editable).
	 */
	private NameDescriptionPanel _nameDescPanel;

	/**
	 * Panel containing the OK and Cancel buttons.
	 */
	private ButtonCmdPanel _cmdPanel;


	/**
	 * Constructs the metadata editor dialog, builds all controls, attaches listeners,
	 * and positions the dialog relative to the parent window.
	 *
	 * @param parent the owning window used to centre the dialog and establish modality
	 */
	public MetaDataEditor(Window parent) {
		// Initialise the RMA base dialog with a fixed title and modal = true
		super(parent, "Edit Meta Data", true);

		buildControls();
		addListeners();

		// Size to preferred layout then override to a fixed square size
		pack();
		setSize(350, 350);

		// Centre the dialog over the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Constructs and lays out all dialog controls using GridBagLayout.
	 *
	 * Layout from top to bottom:
	 * NameDescriptionPanel occupying the upper portion of the dialog.
	 * OK/Cancel button panel anchored to the bottom.
	 *
	 * The name field within the NameDescriptionPanel is locked as read-only so that
	 * users can only modify the description.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout());

		// --- Name/description panel (name field is read-only; description is editable) ---
		_nameDescPanel = new NameDescriptionPanel();
		_nameDescPanel.setNameEditable(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0001;  // Small positive weight keeps the panel anchored near the top
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_nameDescPanel, gbc);

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
	 * Registers a listener on the command button panel to handle OK and Cancel actions.
	 *
	 * OK     -- calls saveForm() to persist any edits, then hides the dialog.
	 * Cancel -- hides the dialog immediately without saving.
	 */
	private void addListeners() {
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			/**
			 * Dispatched on the EDT when any button in the command panel is activated.
			 *
			 * @param e the action event carrying the button ID via ActionEvent.getID()
			 */
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Persist the edited description before closing
						saveForm();
						setVisible(false);
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Discard any edits and close the dialog
						setVisible(false);
						break;
				}
			}
		});
	}


	/**
	 * Persists the metadata edits made in the dialog back to the simulation model.
	 *
	 * This method is a stub awaiting full implementation. When complete, it should
	 * read the description from the NameDescriptionPanel and apply it to the
	 * simulation object that was passed to fillForm.
	 */
	protected void saveForm() {
		// TODO: implement saving the edited description back to the simulation model
		System.out.println("saveForm TODO implement me");
	}


	/**
	 * Pre-populates the name and description fields with data from the given simulation.
	 *
	 * Should be called before the dialog is made visible so the fields are ready
	 * for the user to review and edit.
	 *
	 * @param sim the WAT simulation whose name and description should be displayed;
	 *            must not be null
	 */
	public void fillForm(WatSimulation sim) {
		// Display the simulation name in the read-only name field
		_nameDescPanel.setName(sim.getName());

		// Display the simulation description in the editable description field
		_nameDescPanel.setDescription(sim.getDescription());
	}

}

