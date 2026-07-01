package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building the version command argument list
import java.util.List;            // Ordered collection interface for command argument lists

import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used to read the version string

import usbr.wat.plugins.actionpanel.ActionPanelPlugin; // Provides the singleton plugin instance for retrieving the parent ActionsWindow

/**
 * Action for querying the version of the WAT Git tool (WAT_GIT_Tool_v2.exe).
 *
 * Invokes the Git tool with the --version command and returns the first line of
 * output as the version string. Returns "Unknown" if the tool cannot be invoked
 * or produces no output.
 *
 * Intended for diagnostic purposes (e.g., populating an About dialog or logging
 * the tool version at startup).
 */
public class VersionAction extends AbstractGitAction {
	// Git tool command flag for querying the tool version
	private static final String VERSION_CMD = "--version";

	/**
	 * Constructs a VersionAction using the ActionsWindow from the ActionPanelPlugin singleton
	 * as the parent window.
	 *
	 * The parent window is used for the wait cursor during the Git call.
	 */
	public VersionAction() {
		// Use the singleton plugin's Actions Window as the parent for cursor management
		super("Version", ActionPanelPlugin.getInstance().getActionsWindow());
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to versionAction() to run the version query.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		versionAction();
	}

	/**
	 * Queries the WAT Git tool version and returns it as a string.
	 *
	 * Invokes the --version command and returns the text of the first output line.
	 * Returns "Unknown" if the call fails or the output is empty.
	 *
	 * @return the version string reported by the Git tool, or "Unknown" on failure
	 */
	public String versionAction() {
		// Build the --version command (no additional arguments needed)
		List<String> cmd = new ArrayList<>();
		cmd.add(VERSION_CMD);

		boolean rv = callGit(cmd);

		if (rv) {
			// Retrieve the accumulated output lines from the completed call
			List<ProcessOutputLine> output = getOutput();
			if (output.size() > 0) {
				// Return the first output line as the version string
				return output.get(0).getLine();
			}
		}

		// Return a fallback string if the call failed or produced no output
		return "Unknown";
	}
}
