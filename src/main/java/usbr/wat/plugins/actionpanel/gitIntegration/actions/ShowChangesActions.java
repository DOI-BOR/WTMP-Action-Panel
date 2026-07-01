package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;            // AWT Window used as the parent for informational dialogs
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building command arguments and empty return values
import java.util.Iterator;        // Iterator for walking the output list during header-stripping parsing
import java.util.List;            // Ordered collection interface for command argument and change-line lists

import javax.swing.JOptionPane;   // Provides standard informational dialogs when no repo is selected

import com.rma.io.FileManagerImpl; // RMA file manager for checking whether the .git folder exists
import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used during output parsing

import rma.util.RMAIO;             // RMA I/O utility for path concatenation

import usbr.wat.plugins.actionpanel.gitIntegration.ChangesDlg;         // Dialog for displaying the list of pending changes to the user
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog; // Dialog providing the currently selected repository (convenience constructor)
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;     // Data model holding the local path and source URL of the repository
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils; // Utility providing the GIT_FOLDER constant for .git directory detection

/**
 * Action for querying the number of commits or changed files that exist on the
 * remote Git repository but have not yet been pulled locally.
 *
 * Invokes the WAT Git tool with a --fetch --all --compare-to-remote command for
 * the selected repository. The ChangeType enum (Commits or Files) determines both
 * the command argument and the sentinel strings used to parse the output.
 *
 * Returns null (rather than an empty list) when no .git folder is found in the
 * repository's local path, allowing callers to distinguish between "no changes"
 * and "not a Git repository." This distinction is used by StudyStorageDialog to
 * display "No Local Repository detected." rather than "Local copy is up to date."
 *
 * The static parseOutput() method is package-accessible so it can be reused by
 * ShowChangedLocalFilesAction with its own sentinel strings.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ShowChangesActions extends AbstractGitAction {
	// Git tool command flag for comparing the local HEAD to the remote (shows what is behind)
	private static final String CHANGES_CMD = "--compare-to-remote";

	// Git tool command flag for performing a remote fetch before the comparison
	private static final String FETCH_CMD = "--fetch";

	// Sentinel for the start of commit-based change output (commits behind the remote)
	private static final String COMMIT_CHANGES_START = "Pending Commits";

	// Sentinel emitted when there are no new commits on the remote
	private static final String NO_COMMIT_CHANGES = "No new commits.";

	// Sentinel for the start of file-based change output (files changed on the remote)
	private static final String FILES_CHANGED_START = "Pending Files:";

	// Sentinel emitted when no remote files have changed
	private static final String NO_FILES_CHANGED = "No files changed.";

	/**
	 * Enum representing the type of change comparison to perform.
	 *
	 * Files queries which files differ between local and remote.
	 * Commits queries which commits exist on the remote but not locally.
	 */
	public enum ChangeType {
		// Query files that differ between local and remote
		Files("files"),

		// Query commits that exist on the remote but not locally (commits behind)
		Commits("commits");

		// The string value passed to the Git tool as the change-type argument
		private String _name;

		ChangeType(String name) {
			_name = name;
		}

		/**
		 * Returns the Git tool argument string for this change type.
		 *
		 * @return "files" or "commits"
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	// Whether to compare by commits or by files
	private ChangeType _changeType;

	// The repository to compare against its remote
	private RepoInfo _repo;

	/**
	 * Constructs a ShowChangesActions using the currently selected repo from the given dialog.
	 *
	 * Convenience constructor that delegates to the primary constructor.
	 *
	 * @param parent             the Window used as the parent for informational dialogs
	 * @param studyStorageDialog the dialog providing the currently selected repository
	 * @param changeType         whether to compare commits or files
	 */
	public ShowChangesActions(Window parent, StudyStorageDialog studyStorageDialog, ChangeType changeType) {
		this(parent, studyStorageDialog.getSelectedRepo(), changeType);
	}

	/**
	 * Constructs a ShowChangesActions for the given parent window, repository, and change type.
	 *
	 * @param parent     the Window used as the parent for informational dialogs
	 * @param repo       the RepoInfo describing the repository to compare
	 * @param changeType whether to compare commits or files against the remote
	 */
	public ShowChangesActions(Window parent, RepoInfo repo, ChangeType changeType) {
		super("Changes...", parent);
		_repo = repo;
		_changeType = changeType;
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to showChangesAction() to run the comparison and display the results.
	 *
	 * @param arg0 the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		showChangesAction();
	}

	/**
	 * Fetches changes from the remote and displays them in a ChangesDlg.
	 */
	private void showChangesAction() {
		// Retrieve the change list and show it in the changes dialog
		List<String> changes = getChanges();
		ChangesDlg dlg = new ChangesDlg(getParent(), changes);
		dlg.setVisible(true);
	}

	/**
	 * Compares the local repository against its remote and returns the change descriptions.
	 *
	 * Returns an empty list (with an informational dialog) if no repository is set.
	 * Returns null if the repository's local folder does not contain a .git directory,
	 * signaling to the caller that no local repository exists (not merely that it is up to date).
	 * Invokes --fetch --all --compare-to-remote with the configured change type, parses the
	 * output using the appropriate sentinels, and returns the remaining lines.
	 * Returns an empty list if the Git call fails.
	 *
	 * @return a List of change description strings; null if no local .git folder exists;
	 * an empty list if no repo is selected or the call fails
	 */
	public List<String> getChanges() {
		RepoInfo repo = _repo;

		if (repo == null) {
			// Inform the user that no repo is configured and return an empty list
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to see changes from",
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return new ArrayList<>();
		}

		// Return null if the local folder does not contain a .git directory
		String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);
		if (!FileManagerImpl.getFileManager().fileExists(gitFolder)) {
			// null signals "no local repository" to distinguish from "no changes"
			return null;
		}

		// Build the fetch + compare-to-remote command with the change type argument
		List<String> cmd = new ArrayList<>();
		cmd.add(FETCH_CMD);
		cmd.add(ALL_MODULES);
		cmd.add(CHANGES_CMD);
		cmd.add(_changeType.toString());
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());

		boolean rv = callGit(cmd);

		if (rv) {
			// Select the appropriate sentinel strings based on the change type
			String changesStart, noChanges;
			if (_changeType == ChangeType.Commits) {
				changesStart = COMMIT_CHANGES_START;
				noChanges = NO_COMMIT_CHANGES;
			} else {
				changesStart = FILES_CHANGED_START;
				noChanges = NO_FILES_CHANGED;
			}

			// Strip preamble and return the relevant change-description lines
			List<ProcessOutputLine> output = parseOutput(getOutput(), changesStart, noChanges);
			return getOutputLines(output);
		}

		// Return an empty list if the Git call failed
		return new ArrayList<>();
	}


	/**
	 * Strips preamble lines from the Git tool output, retaining only lines that
	 * follow one of the two sentinel strings.
	 *
	 * Removes all lines until either changesStart or noChanges is found; the sentinel
	 * line itself is also removed and everything afterward is retained. If neither
	 * sentinel is found, the full output is returned as-is.
	 *
	 * Package-static so it can be reused by ShowChangedLocalFilesAction with its
	 * own sentinel strings.
	 *
	 * @param output       the full list of ProcessOutputLine objects from the Git tool
	 * @param changesStart the sentinel string marking the start of relevant content
	 * @param noChanges    the sentinel string marking an empty-result response
	 * @return the filtered list containing only lines after (and not including) the sentinel
	 */
	static List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output, String changesStart, String noChanges) {
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;

		// Preserve a copy of the original list as the fallback return value
		List<ProcessOutputLine> data = new ArrayList<>(output);

		while (iter.hasNext()) {
			line = iter.next();

			// Remove lines until a sentinel is found; then stop removing
			if (line.getLine().startsWith(changesStart) || line.getLine().startsWith(noChanges)) {
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
