package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Window;              // AWT base class for top-level windows; used as the parent reference
import java.awt.event.ActionEvent;   // Event object fired when a button is activated

import javax.swing.JLabel;           // Swing label for static field headings and the dynamic info message

import rma.swing.ButtonCmdPanel;          // RMA panel providing standard OK and Cancel buttons
import rma.swing.ButtonCmdPanelListener;  // Listener interface for ButtonCmdPanel button events
import rma.swing.RmaInsets;               // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJDialog;              // RMA base dialog class providing common dialog behaviour
import rma.swing.RmaJPasswordField;       // RMA password field that masks input characters
import rma.swing.RmaJTextField;           // RMA single-line text field for the username input


/**
 * Modal dialog that collects a username and password from the user.
 *
 * The dialog presents an optional informational message at the top (set via
 * setInfo), followed by "User Name:" and "Password:" labeled input fields, and
 * an OK/Cancel button panel at the bottom.
 *
 * After the dialog is dismissed, callers should check isCanceled() to determine
 * whether the user confirmed or abandoned the login, then retrieve the entered
 * credentials via getUserName() and getPassword().
 *
 * The dialog is fixed at 325 x 175 pixels and is centered relative to its parent
 * window on construction.
 *
 */
public class LoginDialog extends RmaJDialog {
	/**
	 * Optional informational message displayed above the input fields.
	 */
	private JLabel _infoLabel;

	/**
	 * Text field for the username input.
	 */
	private RmaJTextField _userNameFld;

	/**
	 * Password field that masks characters as they are typed.
	 */
	private RmaJPasswordField _passwordFld;

	/**
	 * Panel containing the OK and Cancel buttons.
	 */
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Flag indicating whether the user dismissed the dialog via Cancel.
	 * Defaults to false; set to true only when Cancel is clicked.
	 */
	private boolean _canceled;


	/**
	 * Constructs the login dialog, builds all controls, attaches listeners, and
	 * positions the dialog relative to the parent window.
	 *
	 * @param parent the owning window used to centre the dialog and establish modality
	 * @param title  the text displayed in the dialog title bar
	 */
	public LoginDialog(Window parent, String title) {
		// Initialise the RMA base dialog as modal
		super(parent, true);

		buildControls(title);
		addListeners();

		// Size to preferred layout then override to a fixed compact size
		pack();
		setSize(325, 175);

		// Centre the dialog over the parent window
		setLocationRelativeTo(parent);
	}


	/**
	 * Constructs and lays out all dialog controls using GridBagLayout.
	 *
	 * Layout from top to bottom:
	 * Optional info label spanning the full width.
	 * "User Name:" label paired with the username text field.
	 * "Password:" label paired with the masked password field.
	 * OK/Cancel button panel anchored to the bottom.
	 *
	 * @param title the text to display in the dialog title bar
	 */
	private void buildControls(String title) {
		getContentPane().setLayout(new GridBagLayout());
		setTitle(title);

		// --- Info label: full-width, displays an optional message to the user ---
		_infoLabel = new JLabel();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_infoLabel, gbc);

		// --- "User Name:" label ---
		JLabel label = new JLabel("User Name:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- Username text field (expands horizontally to fill remaining row space) ---
		_userNameFld = new RmaJTextField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_userNameFld, gbc);

		// --- "Password:" label ---
		label = new JLabel("Password:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// --- Password field (small positive weighty absorbs any extra vertical space) ---
		_passwordFld = new RmaJPasswordField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.001;  // Slight positive weight prevents the button panel from drifting upward
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_passwordFld, gbc);

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
	 * OK    -- clears the canceled flag and hides the dialog, allowing the caller to
	 * read the entered credentials.
	 * Cancel -- sets the canceled flag and hides the dialog, signalling that the user
	 * abandoned the login attempt.
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
						// User confirmed; clear the canceled flag and close the dialog
						_canceled = false;
						setVisible(false);
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// User abandoned the login; set the canceled flag and close the dialog
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}


	/**
	 * Sets the informational message displayed above the input fields.
	 *
	 * Typically used to provide context such as the name of the service or
	 * repository requiring authentication. Does nothing if info is null.
	 *
	 * @param info the message text to display; null is silently ignored
	 */
	public void setInfo(String info) {
		if (info != null) {
			_infoLabel.setText(info);
		}
	}


	/**
	 * Returns whether the dialog was dismissed via the Cancel button.
	 *
	 * @return true if the user clicked Cancel; false if the user clicked OK or the
	 * dialog was closed without interaction
	 */
	public boolean isCanceled() {
		return _canceled;
	}


	/**
	 * Pre-populates the username field with the given value.
	 *
	 * Useful when a previously used username is known and should be offered as a
	 * default. Does nothing if userName is null.
	 *
	 * @param userName the username string to display in the field; null is silently ignored
	 */
	public void setUserName(String userName) {
		if (userName != null) {
			_userNameFld.setText(userName);
		}
	}


	/**
	 * Pre-populates the password field with the given value.
	 *
	 * Does nothing if password is null.
	 *
	 * @param password the password string to set in the masked field; null is silently ignored
	 */
	public void setPassword(String password) {
		if (password != null) {
			_passwordFld.setText(password);
		}
	}


	/**
	 * Returns the username entered by the user, with leading and trailing whitespace removed.
	 *
	 * @return the trimmed username string; never null but may be empty if the field was blank
	 */
	public String getUserName() {
		return _userNameFld.getText().trim();
	}


	/**
	 * Returns the password entered by the user, with leading and trailing whitespace removed.
	 *
	 * @return the trimmed password string; never null but may be empty if the field was blank
	 */
	public String getPassword() {
		return _passwordFld.getText().trim();
	}

}
