package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;            // AWT Window used as the parent for informational dialogs
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building command arguments and empty return values
import java.util.List;            // Ordered collection interface for command argument and changed-file lists

import javax.swing.JOptionPane;   // Provides standard informational dialogs when no repo is selected

import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used during output parsing

import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo; // Data model holding the local path of the target repository

/**
 * Action for listing locally modified tracked files in a Git repository within the
 * WTMP Action Panel's Git integration.
 *
 * Invokes the WAT Git tool with the --changes --all command for the given
 * repository's local folder. The --all flag includes all tracked files in the
 * comparison rather than only the files in a specific submodule. Parses the raw
 * output using ShowChangesActions.parseOutput() to strip preamble lines, retaining
 * only the lines that follow the "Files Changed:" or "No tracked files changed"
 * sentinel.
 *
 * The resulting string list is consumed by DownloadConfirmDialog and EnterCommentsDlg
 * to populate their locally-changed-files count labels and detail dialogs.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ShowChangedLocalFilesAction extends AbstractGitAction {
	// Git tool command flag for querying locally modified tracked files
	private static final String CHANGES_CMD = "--changes";

	// Git tool flag for including all tracked files (not just a specific submodule)
	private static final String ALL_FLAG_CMD = "--all";

	// Sentinel string emitted when no tracked files have been locally modified
	private static final String NO_CHANGES = "No tracked files changed";

	// Sentinel string marking the start of the changed-files list in the Git tool output
	private static final String CHANGES_START = "Files Changed:";

	// The repository whose locally modified files should be listed
	private RepoInfo _repo;

	/**
	 * Constructs a ShowChangedLocalFilesAction for the given parent window and repository.
	 *
	 * @param parent the Window used as the owner for informational dialogs
	 * @param repo   the RepoInfo describing the repository to inspect for local changes
	 */
	public ShowChangedLocalFilesAction(Window parent, RepoInfo repo) {
		super("Show Changed Files", parent);
		_repo = repo;
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to getChanges() to run the local-changes query.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		getChanges();
	}

	/**
	 * Queries the Git tool for locally modified tracked files and returns their paths.
	 *
	 * Shows an informational dialog and returns an empty list if no repository is set.
	 * Invokes the --changes --all command, parses the output to strip the preamble,
	 * and returns the remaining lines as modified file path strings.
	 * Returns an empty list if the Git call fails.
	 *
	 * @return a List of locally modified file path strings; empty if no repo is set or the call fails
	 */
	public List<String> getChanges() {
		RepoInfo repo = _repo;

		if (repo == null) {
			// Inform the user that no repo is configured and return an empty list
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to see changes from",
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return new ArrayList<>();
		}

		// Build the --changes --all command with the repo's local folder path
		List<String> cmd = new ArrayList<>();
		cmd.add(CHANGES_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());
		cmd.add(ALL_FLAG_CMD);

		boolean rv = callGit(cmd);

		if (rv) {
			// Strip the preamble and return the relevant changed-file lines
			List<ProcessOutputLine> output = ShowChangesActions.parseOutput(getOutput(), CHANGES_START, NO_CHANGES);
			return ShowChangesActions.getOutputLines(output);
		}

		// Return an empty list if the Git call failed
		return new ArrayList<>();
	}
}
