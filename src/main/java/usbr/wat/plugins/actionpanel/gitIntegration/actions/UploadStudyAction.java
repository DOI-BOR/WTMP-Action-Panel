package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;            // Provides cursor types; used to show a wait cursor during the upload
import java.awt.event.ActionEvent; // Represents an action event fired when the Upload button is clicked
import java.io.BufferedWriter;     // Buffered writer used to write the commit comments to a temp file
import java.io.File;               // Represents the temporary file used for multi-line comments
import java.io.FileWriter;         // Writer for creating the temporary comments file
import java.io.IOException;        // Checked exception thrown when creating or writing the comments file fails
import java.util.ArrayList;       // Resizable-array List for building Git command argument lists
import java.util.List;            // Ordered collection interface for command argument and submodule lists

import javax.swing.JOptionPane;   // Provides standard informational dialogs for upload success/failure

import com.rma.model.Project;     // Represents the currently loaded RMA project; used to check for the save-as marker

import hec.io.FileManagerImpl;    // HEC file manager for checking whether the save-as marker file exists

import rma.util.RMAIO;            // RMA I/O utility for path concatenation

import usbr.wat.plugins.actionpanel.gitIntegration.EnterCommentsDlg;   // Dialog for collecting the commit description, comments, and submodule selection
import usbr.wat.plugins.actionpanel.gitIntegration.SaveStudyAsAction;   // Provides the SAVED_STUDY_AS_FILE marker file name constant
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog; // Parent dialog providing the selected repo and cursor management
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;     // Data model holding the local path of the selected repository

/**
 * Action for uploading (pushing) a WAT study to its Git repository within the
 * WTMP Action Panel's Git integration.
 *
 * Before uploading, checks whether the project directory contains the SaveStudyAsAction
 * marker file. If it does, the project was saved via "Save Study As" and has not yet
 * been set up as a Git repository, so the action delegates to CreateRepoAction instead.
 *
 * Otherwise, presents the EnterCommentsDlg to collect a mandatory commit description
 * and optional multi-line comments along with submodule selection. The combined
 * description + comments are passed to the WAT Git tool via either --comments (for a
 * single-line message) or --commentsfile (for multi-line messages written to a temp file).
 *
 * An OkToPushAction safety check is run before the actual upload to confirm there are
 * no unmerged remote commits that would be overwritten.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class UploadStudyAction extends AbstractGitAction {
	// Git tool command flag for the upload (push) operation
	public static final String UPLOAD_CMD = "--upload";

	// Git tool flag for specifying the path to a temporary file containing multi-line comments
	private static final String COMMENTS_FILE = "--commentsfile";

	// Git tool flag for specifying a single-line commit comment inline in the command
	private static final String COMMENTS = "--comments";

	// Reference to the parent StudyStorageDialog for repo retrieval and cursor management
	private StudyStorageDialog _studyStorageDialog;

	// The comments/submodule dialog shown before the upload; retained for submodule retrieval
	private EnterCommentsDlg _dlg;

	/**
	 * Constructs an UploadStudyAction associated with the given StudyStorageDialog.
	 *
	 * Sets the action's display name to "Upload Study...".
	 *
	 * @param studyStorageDialog the dialog that owns this action; used for repo retrieval and cursor
	 */
	public UploadStudyAction(StudyStorageDialog studyStorageDialog) {
		// Pass the dialog as both the parent window and the action name target
		super("Upload Study...", studyStorageDialog);
		_studyStorageDialog = studyStorageDialog;
	}

	/**
	 * Invoked when the Upload button is clicked.
	 *
	 * Delegates to uploadStudyAction() to run the full upload workflow.
	 *
	 * @param e the ActionEvent fired by the button
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		uploadStudyAction();
	}

	/**
	 * Executes the upload workflow.
	 *
	 * First checks for the save-as marker file; if found, delegates to CreateRepoAction
	 * because the study has not yet been initialized as a Git repository. Otherwise,
	 * collects the commit message via getComments(), writes multi-line comments to a
	 * temp file if needed, runs the OkToPushAction safety check, builds the upload
	 * command with submodule flags, and invokes the Git tool. Shows a result dialog.
	 *
	 * @return true if the upload (or repo creation) succeeded; false otherwise
	 */
	public boolean uploadStudyAction() {
		Project prj = Project.getCurrentProject();
		String dir = prj.getProjectDirectory();

		// Check whether the save-as marker file exists in the project directory
		String markerFileName = RMAIO.concatPath(dir, SaveStudyAsAction.SAVED_STUDY_AS_FILE);
		if (FileManagerImpl.getFileManager().fileExists(markerFileName)) {
			// Project was saved via "Save Study As" — create a new repo instead of uploading
			CreateRepoAction action = new CreateRepoAction(_studyStorageDialog);
			return action.createRepoAction();
		}

		// Collect the commit message from the user via the comments dialog
		String comments = getComments();
		if (comments == null) {
			// User canceled the comments dialog; abort the upload
			return false;
		}

		String commentsFile = null;
		if (comments.contains("\n")) {
			// Write multi-line comments to a temporary file to avoid command-line length issues
			commentsFile = writeComments(comments);
			if (commentsFile == null) {
				// Failed to create the temp file; abort the upload
				return false;
			}
		}

		// Show the wait cursor on the parent dialog during the potentially slow upload
		_studyStorageDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Retrieve the selected repository from the parent dialog
			RepoInfo info = _studyStorageDialog.getSelectedRepo();

			// Build the upload command with the repo's local folder path
			List<String> cmd = new ArrayList<>();
			cmd.add(UPLOAD_CMD);
			cmd.add(LOCAL_FOLDER);
			cmd.add(quoteString(info.getLocalPath()));

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

			// Run the push-safety check before executing the upload
			OkToPushAction okToPush = new OkToPushAction(cmd, _studyStorageDialog);
			if (!okToPush.isOkToPush()) {
				// Push is not safe (e.g., remote has unmerged commits); abort the upload
				return false;
			}

			// Append the commit comments via file or inline depending on line count
			if (commentsFile != null) {
				// Multi-line comments: pass the path to the temp file
				cmd.add(COMMENTS_FILE);
				cmd.add(commentsFile);
			} else {
				// Single-line comments: pass the message directly in the command
				cmd.add(COMMENTS);
				cmd.add(quoteString(comments));
			}

			// Execute the upload command
			boolean rv = callGit(cmd);

			// Show the result to the user
			String msg = "Upload was successful";
			String title = "Success";
			if (!rv) {
				msg = "Upload Failed";
				title = "Failed";
			}

			JOptionPane.showMessageDialog(_studyStorageDialog, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return rv;
		} finally {
			// Always restore the default cursor when the operation completes
			_studyStorageDialog.setCursor(Cursor.getDefaultCursor());
		}
	}


	/**
	 * Writes the given commit comments string to a temporary file for use as the
	 * --commentsfile argument.
	 *
	 * Creates a temp file with the prefix "gitComments" and extension "txt", marks it
	 * for deletion on JVM exit, and writes the comments. Logs a warning and returns
	 * null if the file cannot be created or written.
	 *
	 * @param comments the multi-line commit comments string to write to the temp file
	 * @return the absolute path to the created temp file, or null on failure
	 */
	private String writeComments(String comments) {
		File file;
		try {
			// Create a temporary file; it will be automatically deleted when the JVM exits
			file = File.createTempFile("gitComments", "txt");

		} catch (IOException e1) {
			// Log the failure and return null to abort the upload
			_logger.warning("Failed to write to git comments file error:" + e1);
			return null;
		}

		// Mark the file for deletion when the JVM exits
		file.deleteOnExit();

		BufferedWriter writer = null;
		try {
			// Write the comments to the temp file
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(comments);
			writer.newLine();

		} catch (IOException e) {
			// Log the write failure and return null to abort the upload
			_logger.warning("Failed to write to git comments file " + file.getAbsolutePath() + " error:" + e);
			return null;

		} finally {
			// Always close the writer to release the file handle
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// Silently ignore close failures
				}
			}
		}

		return file.getAbsolutePath();
	}

	/**
	 * Presents the EnterCommentsDlg to collect the commit description and optional comments.
	 *
	 * Combines the mandatory description and the optional multi-line comments into a
	 * single string separated by two newlines. Returns null if the user cancels.
	 *
	 * @return the combined commit message string, or null if the user canceled
	 */
	private String getComments() {
		// Show the comments/submodule dialog modally
		_dlg = new EnterCommentsDlg(_studyStorageDialog, "Enter Comments", true);
		_dlg.setVisible(true);

		if (_dlg.isCanceled()) {
			// User canceled; return null to signal abort
			return null;
		}

		// Combine the mandatory short description with the optional extended comments
		StringBuilder builder = new StringBuilder();
		builder.append(_dlg.getDescription());

		String comments = _dlg.getComments();
		if (comments != null && !comments.isEmpty()) {
			// Separate the description from the extended comments with a blank line
			builder.append("\n");
			builder.append("\n");
			builder.append(comments);
		}

		return builder.toString();
	}

	/**
	 * Returns the list of submodule names selected by the user in the comments dialog.
	 *
	 * Returns an empty list if the comments dialog was never shown.
	 *
	 * @return a List of selected submodule name strings; empty if no dialog was shown
	 */
	private List<String> getSubModules() {
		if (_dlg == null) {
			return new ArrayList<>();
		}
		return _dlg.getSelectedSubmodules();
	}
}
