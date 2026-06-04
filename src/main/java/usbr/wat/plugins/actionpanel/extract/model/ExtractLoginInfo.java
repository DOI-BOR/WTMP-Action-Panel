package usbr.wat.plugins.actionpanel.extract.model;

import java.awt.Window; // AWT Window used as the parent owner for the LoginDialog
import usbr.wat.plugins.actionpanel.ui.LoginDialog; // Modal dialog that collects a username and password from the user

/**
 * Utility class for prompting the user for Merlin service login credentials
 * and caching the result across extract operations.
 *
 * Stores the most recently entered username and password as static fields so
 * that subsequent authentication prompts can pre-populate the login dialog,
 * reducing the need for the user to re-enter credentials after a session
 * timeout or a failed authentication attempt.
 *
 * Both getters return null rather than an empty string when the cached value
 * is blank, allowing callers to distinguish between "no credential entered"
 * and "an empty credential was explicitly submitted".
 *
 * This class is not instantiable; all members are static.
 *
 */
public class ExtractLoginInfo {
	// The most recently entered username; null until the user has confirmed a login dialog
	private static String _userName;

	// The most recently entered password; null until the user has confirmed a login dialog
	private static String _password;

	/**
	 * Private constructor preventing instantiation of this utility class.
	 *
	 * All functionality is accessed through static methods.
	 */
	private ExtractLoginInfo() {
		super();
	}

	/**
	 * Displays a modal login dialog and prompts the user to enter credentials.
	 *
	 * Pre-populates the username and password fields with the most recently
	 * cached values so the user only needs to correct or confirm them. If the
	 * user dismisses the dialog without confirming, no values are updated and
	 * the method returns false. If the user confirms, the trimmed username and
	 * password are stored in the static cache and the method returns true.
	 *
	 * @param parent     the Window that will own the login dialog
	 * @param infoString a descriptive message displayed in the dialog to give
	 *                   the user context about which service they are logging into
	 * @return true if the user confirmed the dialog with credentials; false if canceled
	 */
	public static boolean askForLoginInfo(Window parent, String infoString) {
		// Create the login dialog owned by the given parent window
		LoginDialog dlg = new LoginDialog(parent, "Enter Login Info");

		// Display the context message so the user knows which service requires credentials
		dlg.setInfo(infoString);

		// Pre-populate the fields with any previously cached values to reduce re-entry
		dlg.setUserName(_userName);
		dlg.setPassword(_password);

		// Show the dialog modally; execution blocks here until the user closes it
		dlg.setVisible(true);

		// If the user dismissed the dialog without confirming, return false without updating the cache
		if (dlg.isCanceled()) {
			return false;
		}

		// Cache the confirmed credentials, trimming leading/trailing whitespace from both fields
		_userName = dlg.getUserName().trim();
		_password = dlg.getPassword().trim();

		return true;
	}

	/**
	 * Returns the most recently cached username, or null if none has been entered
	 * or if the cached value is an empty string.
	 *
	 * Returning null for an empty string allows callers to treat a blank username
	 * as absent rather than as an explicitly empty credential.
	 *
	 * @return the cached username string, or null if not set or blank
	 */
	public static String getUserName() {
		// Treat a non-null but empty username as absent and return null
		if (_userName != null && _userName.isEmpty()) {
			return null;
		}

		return _userName;
	}

	/**
	 * Returns the most recently cached password, or null if none has been entered
	 * or if the cached value is an empty string.
	 *
	 * Returning null for an empty string allows callers to substitute a safe
	 * default (such as an empty char array) rather than propagating a blank value.
	 *
	 * @return the cached password string, or null if not set or blank
	 */
	public static String getPassword() {
		// Treat a non-null but empty password as absent and return null
		if (_password != null && _password.isEmpty()) {
			return null;
		}

		return _password;
	}
}
