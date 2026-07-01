package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;            // Provides cursor types; used to show a wait cursor during repo creation
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building the Git command argument list
import java.util.List;            // Ordered collection interface for command argument lists

import javax.swing.JOptionPane;   // Provides standard informational dialogs for success and failure feedback

import com.rma.io.FileManagerImpl; // RMA file manager for deleting the save-as marker file after repo creation
import com.rma.io.RmaFile;         // RMA abstraction representing a file system path
import com.rma.model.Project;      // Represents the currently loaded RMA project and its directory

import rma.util.RMAIO;             // RMA I/O utility for path concatenation

import usbr.wat.plugins.actionpanel.gitIntegration.SaveStudyAsAction;      // Provides the SAVED_STUDY_AS_FILE marker file name constant
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;     // Parent dialog providing context and cursor management
import usbr.wat.plugins.actionpanel.gitIntegration.model.GitToken;         // Utility for prompting the user for a GitLab authentication token
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;         // Data model holding the name, local path, and URL of a Git repository
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CreateRepoDialog;    // Dialog for collecting new repo parameters from the user
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;     // Utility class for retrieving and saving repo definitions

/**
 * Action for creating a new Git repository from the current WAT study within the
 * WTMP Action Panel's Git integration.
 *
 * Presents a CreateRepoDialog to collect: repository name, local folder, remote
 * location (GitLab group/namespace path), parent URL, description, and a flag for
 * whether to keep the existing Git history. Prompts the user for a GitLab
 * authentication token, then invokes the WAT Git tool with the --createrepo command.
 *
 * On success, deletes the SaveStudyAsAction marker file from the project directory
 * (indicating the study is now tracked by Git rather than being a saved-as copy),
 * reads the newly created repo's server URL via GitRepoUtils, and registers the
 * new RepoInfo in the saved repo list.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class CreateRepoAction extends AbstractGitAction {
	// Git tool command flag for the create-repo operation
	private static final String CREATE_REPO_CMD = "--createrepo";

	// Git tool flag for specifying the new repository's display name
	private static final String REPO_NAME = "--newreponame";

	// Git tool flag for specifying the remote GitLab location (group/namespace path)
	private static final String REMOTE_LOCATION = "--remotelocation";

	// Git tool flag for specifying the parent GitLab group URL
	private static final String PARENT_URL = "--parenturl";

	// Git tool flag for adding a text description to the new remote repository
	private static final String DESCRIPTION = "--description";

	// Prompt string to watch for in the Git tool output before sending the auth token
	private static final String LOOK_FOR_TEXT = "Please Enter token";

	// Git tool flag for suppressing history import when creating the repo from a flat copy
	private static final String NO_HISTORY = "--nohistory";

	// Reference to the parent StudyStorageDialog for cursor management and dialog ownership
	private StudyStorageDialog _studyStorageDialog;

	/**
	 * Constructs a CreateRepoAction associated with the given StudyStorageDialog.
	 *
	 * Sets the action's display name to "Create Repo...".
	 *
	 * @param studyStorageDialog the dialog that owns this action; used for cursor and dialog context
	 */
	public CreateRepoAction(StudyStorageDialog studyStorageDialog) {
		// Pass the dialog as both the parent window and the display name target
		super("Create Repo...", studyStorageDialog);
		_studyStorageDialog = studyStorageDialog;
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to createRepoAction() to run the full create-repo workflow.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		createRepoAction();
	}

	/**
	 * Runs the interactive create-repo workflow.
	 *
	 * Opens the CreateRepoDialog to collect repo parameters from the user. If the
	 * user cancels, returns false immediately. Otherwise delegates to the parameterized
	 * overload with the collected values.
	 *
	 * @return true if the repo was successfully created; false if the user canceled or creation failed
	 */
	public boolean createRepoAction() {
		// Open the create-repo dialog pre-filled with the current project's info
		CreateRepoDialog dlg = new CreateRepoDialog(_studyStorageDialog);
		dlg.fillForm(Project.getCurrentProject());
		dlg.setVisible(true);

		// Return false immediately if the user canceled without confirming
		if (dlg.isCanceled()) {
			return false;
		}

		// Collect all parameter values from the confirmed dialog
		String repoName = dlg.getRepoName();
		String localFolder = dlg.getLocalFolder();
		String remoteLocation = dlg.getRepoPath();
		String parentUrl = dlg.getParentUrl();
		String description = dlg.getRepoDescription();
		boolean keepHistory = dlg.shouldKeepHistory();

		// Delegate to the parameterized overload with the collected values
		return createRepoAction(repoName, localFolder, remoteLocation, parentUrl, description, keepHistory);
	}

	/**
	 * Executes the Git create-repo operation with the given parameters.
	 *
	 * Builds the Git tool command argument list from the provided parameters, prompts
	 * the user for a GitLab authentication token (sent to the process stdin when the
	 * tool requests it), and invokes callGit(). On success, deletes the save-as marker
	 * file and registers the new repo in the saved repo list. Shows a result dialog
	 * in either case.
	 *
	 * Shows a wait cursor on the parent dialog for the duration of the operation.
	 *
	 * @param repoName       the display name for the new repository
	 * @param localFolder    the local file system path where the study lives
	 * @param remoteLocation the GitLab group/namespace path for the new remote repo
	 * @param parentUrl      the GitLab parent group URL under which the repo will be created
	 * @param description    an optional text description for the remote repo (null or empty to omit)
	 * @param keepHistory    true to preserve the existing Git history; false to create a clean history
	 * @return true if the repo was created successfully; false otherwise
	 */
	public boolean createRepoAction(String repoName, String localFolder, String remoteLocation,
	                                String parentUrl, String description, boolean keepHistory) {
		// Show the wait cursor on the parent dialog during the potentially slow Git operation
		_studyStorageDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Determine the path of the save-as marker file in the current project directory
			String dir = Project.getCurrentProject().getProjectDirectory();
			String markerFilename = RMAIO.concatPath(dir, SaveStudyAsAction.SAVED_STUDY_AS_FILE);

			// Build the Git tool command argument list
			List<String> cmd = new ArrayList<>();
			cmd.add(CREATE_REPO_CMD);
			cmd.add(REPO_NAME);
			cmd.add(quoteString(repoName));
			cmd.add(LOCAL_FOLDER);
			cmd.add(quoteString(localFolder));
			cmd.add(REMOTE_LOCATION);
			cmd.add(quoteString(remoteLocation));
			cmd.add(PARENT_URL);
			cmd.add(parentUrl);

			// Only include the description flag if a non-empty description was provided
			if (description != null && !description.isEmpty()) {
				cmd.add(DESCRIPTION);
				cmd.add(quoteString(description));
			}

			// Include the no-history flag if the user chose to start with a clean history
			if (!keepHistory) {
				cmd.add(NO_HISTORY);
			}

			// Prompt the user for the GitLab authentication token to send to the tool
			String token = GitToken.getGitToken(_studyStorageDialog);

			// Execute the Git tool, sending the token when the tool prompts for it
			boolean rv = callGit(cmd, LOOK_FOR_TEXT, token);

			String msg = "Repo Created successfully";
			String title = "Success";

			if (!rv) {
				// Report the failure to the user
				msg = "Repo Creation Failed";
				title = "Failed";
			} else {
				// Delete the save-as marker file since the study is now tracked by Git
				RmaFile markerFile = FileManagerImpl.getFileManager().getFile(markerFilename);
				markerFile.delete();

				// Build the .git folder path for URL lookup
				String gitFolder = RMAIO.concatPath(dir, ".git");

				// Read the newly created repo's server URL from the local Git configuration
				String serverUrl = GitRepoUtils.getRepoUrl(_studyStorageDialog, dir);

				if (serverUrl != null) {
					// Register the new repository in the saved repo list
					RepoInfo info = new RepoInfo();
					info.setLocalPath(localFolder);
					info.setSourceUrl(serverUrl);
					info.setName(repoName);
					GitRepoUtils.addRepo(info, false);
				}
			}

			// Show the result message to the user
			JOptionPane.showMessageDialog(_studyStorageDialog, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return rv;
		} finally {
			// Always restore the default cursor when the operation completes
			_studyStorageDialog.setCursor(Cursor.getDefaultCursor());
		}
	}
}
