package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;            // AWT Window used as the parent for error dialogs
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building the working command list and output accumulation
import java.util.Iterator;        // Iterator for walking the output list during header-stripping parsing
import java.util.List;            // Ordered collection interface for command arguments and output line lists

import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used during output parsing

/**
 * Action that checks whether it is safe to push (upload) changes to the Git server.
 *
 * Takes the upload command argument list assembled by UploadStudyAction or
 * EnterCommentsDlg, replaces the --upload flag with --okToPush, and prepends
 * --fetch if not already present. This causes the WAT Git tool to perform a
 * fetch and verify that the local copy is not behind the remote before allowing
 * an upload.
 *
 * If the check fails, parses the output to strip the "Connected to GIT Repo!"
 * preamble and displays the remaining error lines in a scrollable error dialog via
 * AbstractGitAction.showErrorMsg().
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class OkToPushAction extends AbstractGitAction {
	// Git tool command flag for the push-safety check (replaces --upload in the command)
	private static final String OK_TO_PUSH_CMD = "--okToPush";

	// Sentinel string in the output marking an error condition (currently unused in parsing but retained for reference)
	private static final String ERROR = "ERROR:";

	// Sentinel string marking the start of the relevant error content after the connection confirmation
	private static final String CONNECTED_TO_GIT = "Connected to GIT Repo!";

	// The command argument list to modify and send; pre-built by the caller (UploadStudyAction or EnterCommentsDlg)
	private List<String> _cmdBeingRun;

	/**
	 * Constructs an OkToPushAction with the given command list and parent window.
	 *
	 * The provided command list is copied so the original is not modified.
	 *
	 * @param cmd    the upload command argument list whose --upload flag will be replaced with --okToPush
	 * @param parent the Window used as the parent for any error dialogs shown on failure
	 */
	public OkToPushAction(List<String> cmd, Window parent) {
		super("Check if Upload is Ok", parent);

		// Copy the provided command list to avoid mutating the caller's list
		_cmdBeingRun = new ArrayList<>();
		_cmdBeingRun.addAll(cmd);
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to isOkToPush() to run the push-safety check.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		isOkToPush();
	}

	/**
	 * Runs the push-safety check by modifying the command list and calling the Git tool.
	 *
	 * Removes the --upload flag (if present), replaces it with --okToPush, and prepends
	 * --fetch if not already in the command. Suppresses the automatic failed-call error
	 * dialog so this class can handle error display with a custom message.
	 *
	 * On failure, parses the output to strip the connection preamble and shows the
	 * remaining error lines in a scrollable dialog via showErrorMsg().
	 *
	 * @return true if the push is safe to proceed; false if the check failed
	 */
	public boolean isOkToPush() {
		// Replace the --upload flag with the push-safety check flag
		_cmdBeingRun.remove(UploadStudyAction.UPLOAD_CMD);
		_cmdBeingRun.add(OK_TO_PUSH_CMD);

		// Prepend --fetch if it is not already in the command list
		if (!_cmdBeingRun.contains(FetchAction.FETCH_CMD)) {
			_cmdBeingRun.add(0, FetchAction.FETCH_CMD);
		}

		// Suppress the automatic error dialog so this class handles the error display
		setShowFailedCallMessage(false);

		boolean rv = callGit(_cmdBeingRun);

		if (!rv) {
			// Parse the output to strip the connection preamble and extract the error lines
			List<ProcessOutputLine> output = parseOutput(getOutput());
			List<String> lines = getOutputLines(output);

			// Build and display the error message in a scrollable dialog
			String msg = getErrorMessage(null, lines);
			String title = "Can't Upload to Server";
			showErrorMsg(getParent(), title, msg);
			return false;
		}

		return true;
	}

	/**
	 * Strips preamble lines from the Git tool output, retaining only lines that
	 * follow the "Connected to GIT Repo!" sentinel.
	 *
	 * Lines before and including the sentinel are removed. If the sentinel is never
	 * found, all lines are returned as-is (treated as error content).
	 *
	 * @param output the full list of ProcessOutputLine objects from the Git tool
	 * @return the filtered list containing only lines after the connection sentinel
	 */
	private static List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output) {
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;

		// Preserve a copy of the original list as the fallback return value
		List<ProcessOutputLine> data = new ArrayList<>(output);

		while (iter.hasNext()) {
			line = iter.next();

			// Remove lines until the connection sentinel is found; then stop removing
			if (line.getLine().startsWith(CONNECTED_TO_GIT)) {
				// Remove the sentinel line itself; keep everything after it
				iter.remove();
				foundStart = true;
				break;
			} else {
				// Remove all preamble lines before the sentinel
				iter.remove();
			}
		}

		if (!foundStart) {
			// Sentinel not found: return the full output as-is
			return data;
		}

		// Return the remaining lines after the sentinel
		return output;
	}

	/**
	 * Builds a human-readable error message string from an optional header and a list of message lines.
	 *
	 * Differs from AbstractGitAction.getErrorMessage() in that it accepts plain String lines
	 * rather than ProcessOutputLine objects, and is used specifically for the push-safety error.
	 *
	 * @param header   an optional prefix string; null to omit
	 * @param msgLines the list of plain error message lines to join
	 * @return a newline-separated string combining the header and all message lines
	 */
	protected static String getErrorMessage(String header, List<String> msgLines) {
		StringBuilder builder = new StringBuilder();

		// Prepend the header if one was provided
		if (header != null) {
			builder.append(header);
		}

		// Append each error line on its own line
		for (int i = 0; i < msgLines.size(); i++) {
			builder.append("\n");
			builder.append(msgLines.get(i));
		}

		return builder.toString();
	}
}
