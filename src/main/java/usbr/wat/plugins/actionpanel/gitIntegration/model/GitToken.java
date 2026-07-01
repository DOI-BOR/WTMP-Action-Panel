package usbr.wat.plugins.actionpanel.gitIntegration.model;

import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for the token dialog's GridBagLayout
import java.awt.GridBagLayout;      // Flexible grid-based layout manager for the token prompt panel
import java.awt.Window;             // AWT Window used as the parent for the token prompt dialog

import javax.swing.JLabel;          // Non-interactive label for the "Git Token:" field caption
import javax.swing.JOptionPane;     // Provides the OK/Cancel confirm dialog used as the token prompt host
import javax.swing.JPanel;         // Generic lightweight container holding the token prompt controls

import rma.swing.RmaInsets;         // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJCheckBox;      // RMA-extended checkbox for the "Show Token" toggle
import rma.swing.RmaJPasswordField; // RMA-extended password field that masks the token by default

/**
 * Utility class for prompting the user for a GitLab personal access token and
 * caching the result for the duration of the JVM session.
 *
 * The token is stored in two places simultaneously so that it survives across
 * class-loader reloads within the same JVM:
 *   - A static field (PRIVATE_TOKEN) on this class.
 *   - The JVM system property "GitToken" (GIT_TOKEN_PROP), which is also used
 *     to pre-populate the field at startup if it was set via the command line.
 *
 * The prompt dialog contains a password field (token is masked by default) and a
 * "Show Token" checkbox that toggles between masked and plain-text display.
 *
 * This class is not instantiable; all functionality is accessed via static methods.
 */
public class GitToken {
	// System property key used for both reading a pre-set token and caching a prompted token
	private static final String GIT_TOKEN_PROP = "GitToken";

	// Cached token string; initialized from the system property at class-load time
	private static String PRIVATE_TOKEN = System.getProperty(GIT_TOKEN_PROP);

	/**
	 * Private constructor preventing instantiation of this utility class.
	 */
	private GitToken() {
	}

	/**
	 * Returns the current GitLab personal access token, prompting the user if none is cached.
	 *
	 * If a non-empty token is already cached in PRIVATE_TOKEN (either from the system
	 * property or a previous prompt), it is returned immediately without showing a dialog.
	 * Otherwise, displays a modal prompt dialog with a masked password field and a
	 * "Show Token" toggle checkbox. If the user confirms with a non-empty token, the value
	 * is cached in PRIVATE_TOKEN and the GIT_TOKEN_PROP system property, then returned.
	 * Returns null if the user cancels or submits an empty value.
	 *
	 * @param parentForDialog the Window that will own the token prompt dialog
	 * @return the GitLab personal access token string, or null if the user canceled or entered nothing
	 */
	public static String getGitToken(Window parentForDialog) {
		// Return the cached token immediately if one is already available
		if (PRIVATE_TOKEN != null && !PRIVATE_TOKEN.isEmpty()) {
			return PRIVATE_TOKEN;
		}

		// Build the token prompt panel with a GridBagLayout
		JPanel panel = new JPanel(new GridBagLayout());

		// Create the "Git Token:" label
		JLabel label = new JLabel("Git Token:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(label, gbc);

		// Create the password field for masked token entry
		RmaJPasswordField passFld = new RmaJPasswordField();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(passFld, gbc);

		// Add an empty label as a left-column spacer to align the "Show Token" checkbox
		label = new JLabel("");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(label, gbc);

		// Create the "Show Token" toggle checkbox
		RmaJCheckBox showPassChk = new RmaJCheckBox("Show Token");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(showPassChk, gbc);

		// Capture the original echo character so it can be restored when "Show Token" is unchecked
		final char origEchoChar = passFld.getEchoChar();

		// Toggle between masked and plain-text display when the checkbox changes
		showPassChk.addActionListener(e ->
		{
			if (showPassChk.isSelected()) {
				// Remove masking to show the token in plain text
				passFld.setEchoChar((char) 0);
			} else {
				// Restore the original masking character
				passFld.setEchoChar(origEchoChar);
			}
		});

		// Show the token prompt dialog as a plain OK/Cancel option pane
		int opt = JOptionPane.showConfirmDialog(parentForDialog, panel, "Enter Token",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (opt == JOptionPane.OK_OPTION) {
			String token = passFld.getText();
			if (!token.trim().isEmpty()) {
				// Cache the token in the static field and as a system property for session persistence
				PRIVATE_TOKEN = token;
				System.setProperty(GIT_TOKEN_PROP, token);
				return token;
			}
		}

		// User canceled or entered an empty token
		return null;
	}

	/**
	 * Clears the cached GitLab token from both the static field and the system property.
	 *
	 * Should be called after an authentication failure to force re-prompting on the
	 * next request (e.g., when the token has expired or been entered incorrectly).
	 */
	public static void clearGitToken() {
		PRIVATE_TOKEN = null;
		System.clearProperty(GIT_TOKEN_PROP);
	}
}
