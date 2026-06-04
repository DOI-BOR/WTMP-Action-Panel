package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;            // Provides cursor types; used to show a wait cursor during the download
import java.awt.EventQueue;        // Swing event dispatch queue; used to defer the status refresh to the EDT
import java.awt.Window;            // AWT Window used as the parent for dialogs in the non-StudyStorageDialog constructor
import java.awt.event.ActionEvent; // Represents an action event fired when the Download button is clicked
import java.util.ArrayList;       // Resizable-array List for building the Git command argument list
import java.util.List;            // Ordered collection interface for command argument and submodule lists

import javax.swing.JOptionPane;   // Provides standard informational and warning dialogs for download results

import com.rma.io.FileManagerImpl; // RMA file manager for checking whether the .git folder exists in the repo directory

import rma.util.RMAIO;             // RMA I/O utility for path concatenation

import usbr.wat.plugins.actionpanel.gitIntegration.DownloadConfirmDialog;  // Confirmation dialog that collects submodule selection and soft-overwrite flag
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;     // Parent dialog; used for post-download refresh and as the parent window
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;         // Data model holding the local path and source URL of the repository
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;     // Utility providing the GIT_FOLDER constant for .git directory detection

/**
 * Action for downloading (pulling or cloning) a WAT study from a Git repository
 * within the WTMP Action Panel's Git integration.
 *
 * Detects whether the repository's local folder already contains a .git directory:
 *   - If it does, builds a fetch + download command, shows the DownloadConfirmDialog
 *     for submodule selection and the soft-overwrite option, closes the study, and
 *     runs the download. On authentication failure the download is automatically retried.
 *   - If it does not, builds a clone command and runs it without a confirmation dialog.
 *
 * After a successful download, re-opens the study via openStudy() and optionally
 * triggers a status refresh on the parent StudyStorageDialog.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class DownloadStudyAction extends AbstractStudyGitAction {
	// Git tool command flag for performing a pull (download) from an existing clone
	public static final String DOWNLOAD_CMD = "--download";

	// Git tool command flag for performing an initial clone from the remote
	public static final String CLONE_CMD = "--clone";

	// Git tool flag inserted before DOWNLOAD_CMD to also check whether the pull is safe
	private static final String OK_TO_PULL = "--okToPull";

	// Git tool flag for specifying the remote source URL (used during clone)
	private static final String REMOTE = "--remote";

	// Git tool flag that skips overwriting local files not changed since the last upload
	private static final String SOFTOVERWRITE = "--softoverwrite";

	// The confirmation dialog shown before a download (not before a clone); also provides submodule selection
	private DownloadConfirmDialog _confirmDlg;

	// The repository to download from; may be null until setRepoInfo() is called
	private RepoInfo _repo;

	// Whether to use soft-overwrite mode (read from the confirmation dialog)
	private boolean _softoverwrite;

	/**
	 * Constructs a DownloadStudyAction using the currently selected repo from the dialog.
	 *
	 * @param studyStorageDialog the StudyStorageDialog that owns this action
	 */
	public DownloadStudyAction(StudyStorageDialog studyStorageDialog) {
		// Delegate to the primary constructor using the dialog's current repo selection
		this(studyStorageDialog, studyStorageDialog.getSelectedRepo());
	}

	/**
	 * Constructs a DownloadStudyAction for the given parent window and repository.
	 *
	 * @param parent the Window that owns this action's dialogs
	 * @param repo   the RepoInfo describing the repository to download from
	 */
	public DownloadStudyAction(Window parent, RepoInfo repo) {
		super("Download Study...", parent);
		_repo = repo;
	}

	/**
	 * Updates the repository this action will download from.
	 *
	 * @param repo the new RepoInfo to use; null disables the download
	 */
	public void setRepoInfo(RepoInfo repo) {
		_repo = repo;
	}

	/**
	 * Invoked when the Download button is clicked.
	 *
	 * Runs the download and shows a success or failure dialog. On success, triggers
	 * a refresh of the StudyStorageDialog's change status. On failure (and only when
	 * the user did not cancel the confirmation dialog), shows a warning dialog.
	 *
	 * @param e the ActionEvent fired by the button
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		boolean rv = downloadStudyAction();

		if (rv) {
			// Notify the user that the download completed successfully
			JOptionPane.showMessageDialog(getParent(), "Download Complete",
					"Download", JOptionPane.INFORMATION_MESSAGE);

			// Trigger a Git status refresh on the parent StudyStorageDialog if applicable
			Window parent = getParent();
			if (parent instanceof StudyStorageDialog) {
				StudyStorageDialog ssd = (StudyStorageDialog) parent;
				EventQueue.invokeLater(() -> ssd.refreshChangesAction());
			}
		} else if (_confirmDlg != null && !_confirmDlg.isCanceled()) {
			// The download failed and the user did not cancel — show a failure warning
			JOptionPane.showMessageDialog(getParent(), "Download Failed",
					"Download", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Executes the download or clone operation for the configured repository.
	 *
	 * Checks whether the repo's local folder already has a .git directory to
	 * determine whether to pull (existing clone) or clone (first-time setup).
	 * For a pull: shows the confirmation dialog, requests study close, then runs
	 * the fetch + download command including the user-selected submodules. If the
	 * download fails (e.g., auth error), the method calls itself recursively to retry.
	 * For a clone: runs the clone command directly without a confirmation dialog.
	 * Re-opens the study in the finally block regardless of the outcome.
	 *
	 * @return true if the download or clone succeeded; false otherwise
	 */
	public boolean downloadStudyAction() {
		if (_repo == null) {
			// No repository is configured; inform the user and abort
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to download from",
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		try {
			// Show the wait cursor on the parent window during the operation
			getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			List<String> cmd = new ArrayList<>();
			boolean isClone = false;

			// Determine whether this is a pull or a first-time clone by checking for .git
			String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);

			if (FileManagerImpl.getFileManager().fileExists(gitFolder)) {
				// Existing clone: build a fetch + okToPull + download command
				cmd.add(FetchAction.FETCH_CMD);
				cmd.add(OK_TO_PULL);
				cmd.add(DOWNLOAD_CMD);

				// Show the confirmation dialog to let the user select submodules and soft-overwrite
				if (!showDownloadDialog()) {
					return false;
				}

				// Ask the user to confirm that the study should be closed
				if (!askToCloseStudy()) {
					return false;
				}

				// Close the study and record its path for re-opening after the download
				if (!closeStudy()) {
					return false;
				}

				// Append the user-selected submodule flags to the command
				List<String> modules = getSubModules();
				if (modules != null && modules.size() > 0) {
					String module;
					for (int i = 0; i < modules.size(); i++) {
						module = modules.get(i);
						if (AbstractGitAction.STUDY_MODULE.equals(module)) {
							// The main study module uses --main rather than --submodule
							cmd.add(MAIN_MODULE);
						} else {
							// Each submodule gets its own --submodule <name> pair
							cmd.add(SUB_MODULE);
							cmd.add(module);
						}
					}
				}
			} else {
				// No .git folder found: perform a first-time clone rather than a pull
				cmd.add(CLONE_CMD);
				isClone = true;
			}

			// Append the local folder path to the command
			cmd.add(LOCAL_FOLDER);
			cmd.add(_repo.getLocalPath());

			if (isClone) {
				// For a clone, add the remote source URL
				cmd.add(REMOTE);
				cmd.add(quoteString(_repo.getSourceUrl()));
			} else if (_softoverwrite) {
				// For a pull with soft-overwrite, add the soft-overwrite flag
				cmd.add(SOFTOVERWRITE);
			}

			// Enable the failed-call error dialog for this user-initiated operation
			setShowFailedCallMessage(true);

			boolean rv = callGit(cmd);

			if (rv) {
				return rv;
			}

			if (!isClone) {
				// Pull failed (possibly an auth error): retry by calling this method again
				return downloadStudyAction();
			}

			return rv;
		} finally {
			// Always restore the cursor and re-open the study (if it was closed) after the operation
			getParent().setCursor(Cursor.getDefaultCursor());
			openStudy();
		}
	}


	/**
	 * Returns the list of submodule names selected by the user in the confirmation dialog.
	 *
	 * Returns an empty list if the confirmation dialog was never shown (e.g., for a clone).
	 *
	 * @return a List of selected submodule name strings; empty if no dialog was shown
	 */
	private List<String> getSubModules() {
		if (_confirmDlg == null) {
			return new ArrayList<>();
		}
		return _confirmDlg.getSelectedSubmodules();
	}

	/**
	 * Creates and displays the DownloadConfirmDialog, capturing the soft-overwrite preference.
	 *
	 * Stores a reference to the dialog so that submodule selections can be retrieved afterward.
	 *
	 * @return true if the user confirmed the download; false if they canceled
	 */
	private boolean showDownloadDialog() {
		_confirmDlg = new DownloadConfirmDialog(getParent(), _repo);
		_confirmDlg.setVisible(true);

		// Capture the soft-overwrite checkbox state from the confirmed dialog
		_softoverwrite = _confirmDlg.shouldSoftOverWrite();

		// Return true only if the user did not cancel the dialog
		return !_confirmDlg.isCanceled();
	}

	/**
	 * Returns the human-readable operation type used in the close-study confirmation prompt.
	 *
	 * @return "Downloading"
	 */
	@Override
	public String getType() {
		return "Downloading";
	}

	/**
	 * Sets whether the download should use soft-overwrite mode.
	 *
	 * When true, files that have not been modified since the last upload are skipped
	 * rather than being overwritten by the incoming download content.
	 *
	 * @param softoverwrite true to enable soft-overwrite; false for a full overwrite
	 */
	public void setSoftOverwriteOnDownLoad(boolean softoverwrite) {
		_softoverwrite = softoverwrite;
	}
}
