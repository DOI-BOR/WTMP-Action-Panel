package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Dialog;            // AWT Dialog type used as the parent for this action's Git calls
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building command argument lists and accumulating output
import java.util.Iterator;        // Iterator for walking the output list during header-stripping parsing
import java.util.List;            // Ordered collection interface for command arguments and output line lists

import javax.swing.JOptionPane;   // Provides standard informational dialogs when no repo is selected

import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used during output parsing

import usbr.wat.plugins.actionpanel.gitIntegration.ChangesDlg;         // Dialog for displaying the list of pending changes
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog; // Parent dialog providing the currently selected repository
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;     // Data model holding the local path of the selected repository

/**
 * Action for fetching pending change information from the selected Git repository
 * and optionally displaying the results in a ChangesDlg.
 *
 * Invokes the WAT Git tool with the --fetch command for the selected repository's
 * local path. Parses the raw output to strip header/preamble lines, retaining only
 * the lines starting at the "Pending Changes:" or "No files changed" sentinel.
 *
 * The FETCH_CMD constant is also used by other actions (DownloadStudyAction,
 * OkToPushAction, ShowChangesActions) to prefix their own commands for combined
 * fetch-plus-operation calls.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class FetchAction extends AbstractGitAction {
	// Git tool command flag for fetching remote change information
	public static final String FETCH_CMD = "--fetch";

	// Sentinel string marking the start of the relevant change content in the Git tool output
	private static final String CHANGES_START = "Pending Changes:";

	// Sentinel string emitted when there are no pending changes in the output
	private static final String NO_CHANGES = "No files changed";

	// Reference to the parent dialog for retrieving the currently selected repository
	private StudyStorageDialog _studyStorageDialog;

	/**
	 * Constructs a FetchAction with the given parent dialog and owning StudyStorageDialog.
	 *
	 * @param parent             the Dialog used as the owner for this action's Git call wait cursor
	 * @param studyStorageDialog the dialog from which the currently selected repository is retrieved
	 */
	public FetchAction(Dialog parent, StudyStorageDialog studyStorageDialog) {
		super("Fetch...", parent);
		_studyStorageDialog = studyStorageDialog;
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to fetchAction() to run the fetch and show the results dialog.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		fetchAction();
	}

	/**
	 * Fetches pending changes from the selected repo and displays them in a ChangesDlg.
	 *
	 * @return the list of pending change strings, or an empty list if no repo is selected
	 * or the fetch fails
	 */
	public List<String> fetchAction() {
		// Retrieve the change list and show it in the changes dialog
		List<String> changes = getChanges();
		ChangesDlg dlg = new ChangesDlg(getParent(), changes);
		dlg.setVisible(true);
		return changes;
	}

	/**
	 * Fetches pending changes from the selected repository and returns them as a string list.
	 *
	 * Shows an informational dialog and returns an empty list if no repository is selected.
	 * Runs the --fetch command, parses the output to strip header lines, and returns
	 * the remaining content lines. Returns an empty list if the fetch fails.
	 *
	 * @return a List of pending change description strings; empty if no repo is selected or the call fails
	 */
	public List<String> getChanges() {
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();

		if (repo == null) {
			// Inform the user that no repo is selected and return an empty list
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to see changes for",
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return new ArrayList<>();
		}

		// Build the --fetch command with the repo's local folder path
		List<String> cmd = new ArrayList<>();
		cmd.add(FETCH_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());

		boolean rv = callGit(cmd);

		if (rv) {
			// Strip preamble lines and return only the relevant change content
			List<ProcessOutputLine> output = parseOutput(getOutput());
			return getOutputLines(output);
		}

		// Return an empty list if the fetch failed
		return new ArrayList<>();
	}


	/**
	 * Strips preamble lines from the Git tool output, retaining only the lines
	 * that follow the "Pending Changes:" or "No files changed" sentinel.
	 *
	 * Iterates the output list, removing each line until the sentinel is found and
	 * removed. Lines after the sentinel are kept. If the sentinel is never found,
	 * all lines are returned (treating the full output as relevant content).
	 *
	 * @param output the full list of ProcessOutputLine objects from the Git tool
	 * @return the filtered list containing only lines after (and not including) the sentinel
	 */
	private List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output) {
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;

		// Preserve a copy of the original list as the fallback return value
		List<ProcessOutputLine> data = new ArrayList<>(output);

		while (iter.hasNext()) {
			line = iter.next();

			// Remove lines until the sentinel is found; then stop removing
			if (line.getLine().startsWith(CHANGES_START) || line.getLine().startsWith(NO_CHANGES)) {
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
			// Sentinel was never found: return the full output as-is
			return data;
		}

		// Return the remaining lines after the sentinel
		return output;
	}
}
