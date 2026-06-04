package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;            // Provides cursor types; used to show a wait cursor during the restore
import java.awt.EventQueue;        // Swing event dispatch queue; used to defer the status refresh to the EDT
import java.awt.Window;            // AWT Window; inherited through AbstractStudyGitAction for dialog ownership
import java.awt.event.ActionEvent; // Represents an action event fired when the Restore button is clicked
import java.util.ArrayList;       // Resizable-array List for building Git command argument lists
import java.util.List;            // Ordered collection interface for command argument and submodule name lists

import javax.swing.Action;        // Swing Action interface; used to set the SHORT_DESCRIPTION (tooltip) value
import javax.swing.JOptionPane;   // Provides standard informational and warning dialogs for restore results

import com.rma.io.FileManagerImpl; // RMA file manager for checking whether the .git folder exists

import rma.util.RMAIO;             // RMA I/O utility for path concatenation

import usbr.wat.plugins.actionpanel.gitIntegration.DownloadConfirmDialog;  // Confirmation dialog (in restore mode) for submodule selection
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;     // Parent dialog; used for post-restore refresh and as the parent window
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;         // Data model holding the local path of the repository
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;     // Utility providing the GIT_FOLDER constant for .git directory detection

/**
 * Action for restoring a WAT study from the last successfully downloaded state
 * in its Git repository within the WTMP Action Panel's Git integration.
 *
 * Checks that the repository's local folder contains a .git directory, shows the
 * DownloadConfirmDialog in restore mode (isRestore=true) for submodule selection,
 * closes the study, then invokes the WAT Git tool with the --restore command and
 * the user-selected submodule flags. After the restore completes, the study is
 * re-opened via openStudy().
 *
 * The action is named "Restore..." and carries a short description (tooltip)
 * explaining its purpose.
 */
public class RestoreStudyAction extends AbstractStudyGitAction {
	// Git tool command flag for the restore operation
	public static final String RESTORE_CMD = "--restore";

	// The repository to restore; updated via setRepoInfo() when the user selects a different repo
	private RepoInfo _repo;

	// The confirmation dialog shown before a restore; stored for submodule selection retrieval
	private DownloadConfirmDialog _confirmDlg;

	/**
	 * Constructs a RestoreStudyAction associated with the given StudyStorageDialog.
	 *
	 * Sets the action's display name to "Restore..." and its tooltip to describe its purpose.
	 *
	 * @param studyStorageDialog the dialog that owns this action; used as the parent window
	 */
	public RestoreStudyAction(StudyStorageDialog studyStorageDialog) {
		// Pass the dialog as both the parent window and the action name target
		super("Restore...", studyStorageDialog);

		// Set the tooltip text shown when hovering over the Restore button
		putValue(Action.SHORT_DESCRIPTION, "Restore all or part of the study from the repository.");
	}

	/**
	 * Updates the repository this action will restore from.
	 *
	 * @param repo the new RepoInfo to use; null disables the restore
	 */
	public void setRepoInfo(RepoInfo repo) {
		_repo = repo;
	}

	/**
	 * Invoked when the Restore button is clicked.
	 *
	 * Runs the restore operation and shows a success or failure dialog. On success,
	 * triggers a refresh of the StudyStorageDialog's change status. On failure (and
	 * only when the user did not cancel the confirmation dialog), shows a warning.
	 *
	 * @param e the ActionEvent fired by the button
	 */
	public void actionPerformed(ActionEvent e) {
		boolean rv = restoreStudyAction();

		if (rv) {
			// Notify the user that the restore completed successfully
			JOptionPane.showMessageDialog(getParent(), "Restore Complete",
					"Restore", JOptionPane.INFORMATION_MESSAGE);

			// Trigger a Git status refresh on the parent StudyStorageDialog if applicable
			Window parent = getParent();
			if (parent instanceof StudyStorageDialog) {
				StudyStorageDialog ssd = (StudyStorageDialog) parent;
				EventQueue.invokeLater(() -> ssd.refreshChangesAction());
			}
		} else if (_confirmDlg != null && !_confirmDlg.isCanceled()) {
			// The restore failed and the user did not cancel — show a failure warning
			JOptionPane.showMessageDialog(getParent(), "Restore Failed",
					"Restore", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Executes the restore operation for the configured repository.
	 *
	 * Verifies that the repository's .git folder exists, shows the DownloadConfirmDialog
	 * in restore mode for submodule selection, closes the study, then builds and runs the
	 * --restore Git command including the selected submodule flags. Re-opens the study in
	 * the finally block regardless of outcome.
	 *
	 * @return true if the restore succeeded; false if no repo is set, the .git folder is missing,
	 * the user canceled, the study could not be closed, or the Git call failed
	 */
	public boolean restoreStudyAction() {
		if (_repo == null) {
			// No repository configured; inform the user and abort
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to restore from",
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		try {
			// Show the wait cursor on the parent window during the operation
			getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			List<String> cmd = new ArrayList<>();
			String gitCmd = null;

			// Check whether the local folder already has a .git directory
			String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);

			if (FileManagerImpl.getFileManager().fileExists(gitFolder)) {
				// .git folder exists: this is a valid restore target
				gitCmd = RESTORE_CMD;

				// Show the restore confirmation dialog for submodule selection
				if (!showRestoreDialog()) {
					return false;
				}

				// Ask the user to confirm that the study should be closed
				if (!askToCloseStudy()) {
					return false;
				}

				// Close the study and record its path for re-opening after the restore
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
			}

			// Append the restore command and local folder path
			cmd.add(gitCmd);
			cmd.add(LOCAL_FOLDER);
			cmd.add(_repo.getLocalPath());

			// Execute the restore command and return the result
			return callGit(cmd);
		} finally {
			// Always restore the cursor and re-open the study after the operation completes
			getParent().setCursor(Cursor.getDefaultCursor());
			openStudy();
		}
	}

	/**
	 * Returns the list of submodule names selected by the user in the restore confirmation dialog.
	 *
	 * Returns an empty list if the confirmation dialog was never shown.
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
	 * Creates and displays the DownloadConfirmDialog in restore mode (isRestore=true).
	 *
	 * Stores the dialog reference so that submodule selections can be retrieved afterward.
	 *
	 * @return true if the user confirmed the restore; false if they canceled
	 */
	private boolean showRestoreDialog() {
		// Create the confirmation dialog in restore mode to adapt its title and button label
		_confirmDlg = new DownloadConfirmDialog(getParent(), _repo, true);
		_confirmDlg.setVisible(true);

		// Return true only if the user did not cancel the dialog
		return !_confirmDlg.isCanceled();
	}

	/**
	 * Returns the human-readable operation type used in the close-study confirmation prompt.
	 *
	 * @return "Restoring"
	 */
	@Override
	protected String getType() {
		return "Restoring";
	}
}
