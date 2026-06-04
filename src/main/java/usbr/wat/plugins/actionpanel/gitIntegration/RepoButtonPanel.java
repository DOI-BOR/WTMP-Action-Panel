package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.GridBagConstraints;   // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;        // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;               // AWT Window used as the parent reference for actions that open child dialogs

import javax.swing.JButton;           // Standard Swing push-button component backed by an AbstractAction
import javax.swing.JLabel;            // Empty label used as a vertical spacer at the bottom of the panel
import javax.swing.JPanel;            // Base Swing panel class that this panel extends

import com.rma.event.ProjectAdapter;  // Adapter providing empty implementations of project lifecycle events
import com.rma.event.ProjectEvent;    // Project lifecycle event carrying the opened or closed Project reference
import com.rma.model.Project;         // Represents the currently loaded RMA project; used to check no-project state

import hec.io.FileManagerImpl;        // HEC file manager for checking whether the save-as marker file exists

import rma.swing.RmaInsets;           // Pre-defined Insets constants for consistent component spacing
import rma.util.RMAIO;               // RMA I/O utility providing path concatenation helpers

import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction; // Action for downloading the study from the selected repo
import usbr.wat.plugins.actionpanel.gitIntegration.actions.RestoreStudyAction;  // Action for restoring the study to its last downloaded state
import usbr.wat.plugins.actionpanel.gitIntegration.actions.UploadStudyAction;   // Action for uploading the study to the selected repo
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;              // Data model holding the local path and URL of a Git repository

/**
 * Panel housing the repository action buttons — Download, Upload, Restore,
 * Open Study, and (optionally) Save Study As — for the StudyStorageDialog.
 *
 * All buttons are initially disabled. They are enabled or disabled collectively
 * via setRepoInfo() when the user selects a repository in the parent dialog.
 * The Upload button has additional logic: even with no repository selected, it
 * is enabled if the current project was previously saved via "Save Study As"
 * (indicated by the presence of the SAVED_STUDY_AS_FILE marker file).
 *
 * Save Study As is conditionally added to the panel only if the system property
 * "WTMP.HasSaveStudyAs" is set to true.
 *
 * A static ProjectAdapter listener is registered to update the Save Study As
 * button's enabled state when a project is opened or closed; it must be removed
 * by calling closing() when the parent dialog is dismissed to avoid a memory leak.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class RepoButtonPanel extends JPanel {
	// Action for cloning or pulling the study from the remote repository
	private DownloadStudyAction _downloadStudyAction;

	// Action for pushing the study to the remote repository
	private UploadStudyAction _uploadStudyAction;

	// Action for opening the WAT study file in the selected repository's local folder
	private OpenStudyAction _openStudyAction;

	// Action for restoring the study to its last successfully downloaded state
	private RestoreStudyAction _restoreStudyAction;

	// Button backed by _downloadStudyAction
	private JButton _downloadStudyButton;

	// Button backed by _uploadStudyAction
	private JButton _uploadStudyButton;

	// Button backed by _restoreStudyAction
	private JButton _restoreStudyButton;

	// Button backed by _openStudyAction
	private JButton _openStudyButton;

	// Parent window reference passed to actions that open child dialogs
	private Window _parent;

	// Reference to the owning StudyStorageDialog for repo selection and context
	private StudyStorageDialog _studyStorageDialog;

	// Action for saving the current project to a new name and location
	private SaveStudyAsAction _saveStudyAsAction;

	// Button backed by _saveStudyAsAction; only added to the panel when WTMP.HasSaveStudyAs is true
	private JButton _saveStudyAsButton;

	// Static project listener that updates the Save Study As button when a project opens or closes
	private ProjectAdapter _projectListener;

	/**
	 * Constructs a RepoButtonPanel with the given parent window and owning dialog.
	 *
	 * Initializes with a GridBagLayout, stores the parent references, builds the
	 * action buttons, attaches the project listener, and initially disables all buttons.
	 *
	 * @param parent             the Window used as the parent for child dialogs opened by actions
	 * @param studyStorageDialog the StudyStorageDialog that owns this panel
	 */
	public RepoButtonPanel(Window parent, StudyStorageDialog studyStorageDialog) {
		// Initialize the JPanel with a GridBagLayout
		super(new GridBagLayout());

		// Store references for later use by actions and the project listener
		_parent = parent;
		_studyStorageDialog = studyStorageDialog;

		// Build and add all action buttons
		buildControls();

		// Attach the static project listener for Save Study As button state
		addListeners();

		// Disable all buttons until the user selects a repository
		setButtonsEnabled(false);
	}

	/**
	 * Builds and lays out all action buttons within the panel.
	 *
	 * Creates Download, Upload, Restore, Open Study, and (conditionally) Save Study As
	 * buttons. The Save Study As button is added only if the "WTMP.HasSaveStudyAs" system
	 * property is true. An empty JLabel spacer is placed at the bottom to consume
	 * remaining vertical space.
	 */
	private void buildControls() {
		// Create the Download Study action and its button
		_downloadStudyAction = new DownloadStudyAction(_studyStorageDialog);
		_downloadStudyButton = new JButton(_downloadStudyAction);

		// Position the Download button to span the full row
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_downloadStudyButton, gbc);

		// Create the Upload Study action and its button
		_uploadStudyAction = new UploadStudyAction(_studyStorageDialog);
		_uploadStudyButton = new JButton(_uploadStudyAction);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_uploadStudyButton, gbc);

		// Create the Restore Study action and its button
		_restoreStudyAction = new RestoreStudyAction(_studyStorageDialog);
		_restoreStudyButton = new JButton(_restoreStudyAction);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_restoreStudyButton, gbc);

		// Create the Open Study action and its button; extra top inset separates it visually
		_openStudyAction = new OpenStudyAction(_studyStorageDialog);
		_openStudyButton = new JButton(_openStudyAction);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.insets(10, 5, 0, 5);
		add(_openStudyButton, gbc);

		// Create the Save Study As action and button; initial state mirrors the no-project check
		_saveStudyAsAction = new SaveStudyAsAction(_studyStorageDialog);
		_saveStudyAsButton = new JButton(_saveStudyAsAction);
		_saveStudyAsButton.setEnabled(!Project.getCurrentProject().isNoProject());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.insets(10, 5, 0, 5);

		// Only add the Save Study As button if the feature flag is explicitly enabled
		if (Boolean.getBoolean("WTMP.HasSaveStudyAs")) {
			add(_saveStudyAsButton, gbc);
		}

		// Add an empty spacer label to consume remaining vertical space at the bottom
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.001;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JLabel(), gbc);
	}

	/**
	 * Registers a static project listener to update the Save Study As button's
	 * enabled state when a project is opened.
	 *
	 * The listener enables the button only when no project is currently loaded
	 * (isNoProject() == true), which reflects the intended workflow where Save Study As
	 * is used before a project has been formally opened from the repo.
	 */
	private void addListeners() {
		// Create a project adapter that updates the Save Study As button when a project opens
		_projectListener = new ProjectAdapter() {
			@Override
			public void projectOpened(ProjectEvent e) {
				Project prj = e.getProject();

				// Enable Save Study As only when no real project is currently loaded
				boolean enabled = prj.isNoProject();
				_saveStudyAsButton.setEnabled(enabled);
			}
		};

		// Register the listener statically so it receives events for all project changes
		Project.addStaticProjectListener(_projectListener);
	}

	/**
	 * Enables or disables the Download, Restore, Open Study, and Upload buttons.
	 *
	 * The Upload button uses shouldEnableUploadButton() for additional logic that
	 * can enable it even when no repository is selected (if a save-as marker file exists).
	 *
	 * @param enabled true to enable the buttons; false to disable them
	 */
	private void setButtonsEnabled(boolean enabled) {
		// The Upload button has special logic beyond the simple enabled flag
		_uploadStudyAction.setEnabled(shouldEnableUploadButton(enabled));

		_downloadStudyButton.setEnabled(enabled);
		_restoreStudyButton.setEnabled(enabled);
		_openStudyAction.setEnabled(enabled);
	}

	/**
	 * Determines whether the Upload button should be enabled.
	 *
	 * Returns true in any of these cases:
	 * - The base enabled flag is true (a repository is selected), OR
	 * - No repository is selected AND a real project is loaded AND the
	 * SAVED_STUDY_AS_FILE marker file exists in the project directory,
	 * indicating the project was previously saved via "Save Study As".
	 *
	 * @param enabled the base enabled state driven by repository selection
	 * @return true if the Upload button should be enabled; false otherwise
	 */
	private boolean shouldEnableUploadButton(boolean enabled) {
		if (_studyStorageDialog.getSelectedRepo() == null && !Project.getCurrentProject().isNoProject()) {
			// No repo selected and a real project is open: check for the save-as marker file
			String dir = Project.getCurrentProject().getProjectDirectory();
			String checkFile = RMAIO.concatPath(dir, SaveStudyAsAction.SAVED_STUDY_AS_FILE);

			if (FileManagerImpl.getFileManager().fileExists(checkFile)) {
				// Marker file found: enable the Upload button so the user can associate and push
				return true;
			}
		}

		// Fall back to the standard enabled state
		return enabled;
	}

	/**
	 * Updates all action objects with the newly selected repository and adjusts button states.
	 *
	 * Passes the RepoInfo to the Download and Restore actions so they know which repo to target.
	 * Enables all buttons when repo is non-null; disables them when null (no selection).
	 *
	 * @param repo the newly selected RepoInfo, or null if the selection was cleared
	 */
	public void setRepoInfo(RepoInfo repo) {
		// Propagate the selected repo to actions that need it for their operations
		_downloadStudyAction.setRepoInfo(repo);
		_restoreStudyAction.setRepoInfo(repo);

		// Enable or disable buttons based on whether a repo is selected
		setButtonsEnabled(repo != null);
	}

	/**
	 * Sets the soft-overwrite flag on the Download Study action.
	 *
	 * When soft-overwrite is true, the download will not overwrite local files
	 * that have not been modified since the last upload.
	 *
	 * @param softoverwrite true to enable soft-overwrite on download; false to overwrite all files
	 */
	public void setSoftOverwriteOnDownload(boolean softoverwrite) {
		_downloadStudyAction.setSoftOverwriteOnDownLoad(softoverwrite);
	}

	/**
	 * Cleans up resources when the parent StudyStorageDialog is being dismissed.
	 *
	 * Removes the static project listener registered in addListeners() to prevent
	 * a memory leak from the listener holding a reference to this panel after the
	 * dialog is closed.
	 */
	public void closing() {
		// Remove the static listener to allow this panel to be garbage collected
		Project.removeStaticProjectListener(_projectListener);
	}
}
