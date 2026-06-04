package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.EventQueue;        // Swing event dispatch queue; defers the repo tree population to the EDT after open
import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;     // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;            // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent; // Represents an action event fired by the OK and Cancel buttons
import java.awt.event.WindowAdapter; // Adapter for window lifecycle events; defers repo tree fill to window-opened
import java.awt.event.WindowEvent;  // Window event fired when the dialog is opened

import javax.swing.JFrame;         // Used in the main() test method to create a standalone parent frame
import javax.swing.JLabel;         // Non-interactive label for field captions
import javax.swing.JOptionPane;    // Provides informational dialogs for validation failures

import com.rma.io.FileManagerImpl;       // RMA file manager for checking whether the .git directory exists
import com.rma.swing.RmaFileChooserField; // RMA file chooser field for selecting the study folder directory

import rma.swing.ButtonCmdPanel;         // Panel containing OK and Cancel buttons
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaInsets;             // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJCheckBox;          // RMA-extended checkbox for the "Preserve History" option
import rma.swing.RmaJDialog;            // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTextField;         // RMA-extended text field for the repo name and description
import rma.util.RMAIO;                  // RMA I/O utility for path concatenation

/**
 * Dialog for collecting the information needed to create a new Git repository for
 * a WAT study within the WTMP Action Panel's Git integration.
 *
 * Presents the following fields:
 *   - Study Folder: the local file system path of the study (pre-filled and read-only
 *     when an existing project is passed to fillForm(); editable otherwise).
 *   - Repo Name: the display name for the new remote repository (pre-filled from the
 *     project name when a project is provided).
 *   - Repo Description: an optional text description for the remote repository.
 *   - Preserve History: a checkbox controlling whether the existing Git history is
 *     retained or discarded. Disabled and unchecked when the study has no .git folder
 *     (no prior history to preserve).
 *   - Repo Location: a RepoJTree for selecting the GitLab group/folder where the
 *     remote repository will be created.
 *
 * Validation checks that the repo name is non-empty, the study folder path is
 * non-empty, and a valid repo location has been selected in the tree.
 *
 * The repo location tree is populated asynchronously on the EDT after the dialog opens.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class CreateRepoDialog extends RmaJDialog {
	// Text field for entering the display name of the new repository
	private RmaJTextField _repoNameFld;

	// File chooser field for selecting the local study folder path
	private RmaFileChooserField _studyFolderFld;

	// Tree for browsing and selecting the GitLab group/folder where the repo will be created
	private RepoJTree _repoLocationTree;

	// Panel containing the OK and Cancel buttons
	private ButtonCmdPanel _cmdPanel;

	// Flag indicating whether the dialog was dismissed without confirming
	protected boolean _canceled;

	// Text field for entering an optional description for the remote repository
	private RmaJTextField _repoDescFld;

	// Checkbox controlling whether the existing Git history is retained in the new remote repo
	private RmaJCheckBox _preserveHistoryCheck;

	/**
	 * Constructs a CreateRepoDialog owned by the given parent window.
	 *
	 * Builds all UI controls, attaches listeners, packs to the preferred layout,
	 * overrides to a fixed 600×450 size with a minimum of 450×300, and centers
	 * relative to the parent window.
	 *
	 * @param parent the Window that will own this modal dialog
	 */
	public CreateRepoDialog(Window parent) {
		super(parent, true);

		// Build and arrange all UI controls
		buildControls();

		// Attach button and window-opened listeners
		addListeners();

		// Size to preferred layout dimensions
		pack();

		// Override with fixed and minimum dialog sizes
		setSize(600, 450);
		setDefaultSize(450, 300);

		// Center relative to the parent window
		setLocationRelativeTo(parent);
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Arranges: a study folder file-chooser, a repo name field, a repo description
	 * field, a "Preserve History" checkbox, a RepoJTree for location selection,
	 * and an OK/Cancel button panel.
	 */
	private void buildControls() {
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Create Repo");

		// Create the "Study Folder:" label and file chooser field
		JLabel label = new JLabel("Study Folder:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the study folder file-chooser field
		_studyFolderFld = new RmaFileChooserField();
		label.setLabelFor(_studyFolderFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_studyFolderFld, gbc);

		// Create the "Repo Name:" label and text field
		label = new JLabel("Repo Name:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		_repoNameFld = new RmaJTextField();
		label.setLabelFor(_repoNameFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_repoNameFld, gbc);

		// Create the "Repo Description:" label and text field
		label = new JLabel("Repo Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		_repoDescFld = new RmaJTextField();
		label.setLabelFor(_repoDescFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_repoDescFld, gbc);

		// Create the "Preserve History" checkbox; initially checked
		_preserveHistoryCheck = new RmaJCheckBox("Preserve History", true);
		_preserveHistoryCheck.setToolTipText("<html>When selected will keep the Git History, which can increase the size of the study.<br>When not selected will start the history with only changes going forward</html>");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_preserveHistoryCheck, gbc);

		// Create the RepoJTree in Folder selection mode for choosing the remote location
		_repoLocationTree = new RepoJTree(RepoJTree.SelectionType.Folder);
		label.setLabelFor(_repoLocationTree);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_repoLocationTree, gbc);

		// Create the OK/Cancel button panel
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * Attaches event listeners to the OK/Cancel buttons and the window-opened event.
	 *
	 * Validates the form and closes the dialog when OK is clicked. Cancels and closes
	 * when Cancel is clicked. Populates the repo location tree on the EDT after the
	 * dialog has fully opened.
	 */
	private void addListeners() {
		// Handle OK and Cancel button clicks from the command panel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate all required fields before confirming
						if (isValidForm()) {
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Mark as canceled and close without further action
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});

		// Defer the repo tree population to the EDT after the dialog is fully visible
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				EventQueue.invokeLater(() -> _repoLocationTree.fillRepoTree());
			}
		});
	}


	/**
	 * Returns whether the dialog was closed by the user selecting Cancel.
	 *
	 * @return true if the dialog was canceled; false if the user confirmed with OK
	 */
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Populates the dialog fields from the given RMA project.
	 *
	 * If a project is provided, pre-fills the study folder path (read-only) and repo
	 * name from the project. Checks whether the study already has a .git directory;
	 * if not, disables and unchecks the Preserve History option (no history to keep).
	 * If no project is provided, makes the study folder field editable and disables
	 * Preserve History.
	 *
	 * @param prj the currently open RMA project to pre-fill from; null for a blank form
	 */
	public void fillForm(com.rma.model.Project prj) {
		if (prj != null) {
			// Pre-fill the study folder path and lock it since the project is already open
			_studyFolderFld.setText(prj.getProjectDirectory());
			_studyFolderFld.setEditable(false);

			// Pre-fill the repo name from the project's display name
			_repoNameFld.setText(prj.getName());

			// Check whether a .git directory already exists in the project folder
			String dir = prj.getProjectDirectory();
			String gitDir = RMAIO.concatPath(dir, ".git");
			if (!FileManagerImpl.getFileManager().fileExists(gitDir)) {
				// No existing .git directory: disable and uncheck Preserve History
				_preserveHistoryCheck.setEnabled(false);
				_preserveHistoryCheck.setSelected(false);
			}
		} else {
			// No project: allow folder selection and disable Preserve History
			_studyFolderFld.setEditable(true);
			_preserveHistoryCheck.setEnabled(false);
			_preserveHistoryCheck.setSelected(false);
		}
	}


	/**
	 * Validates the form fields before allowing the dialog to close.
	 *
	 * Checks that the repo name is non-empty, the study folder path is non-empty,
	 * and a valid repo location has been selected in the tree. Shows an informational
	 * dialog for each validation failure.
	 *
	 * @return true if all validation checks pass; false if any check fails
	 */
	protected boolean isValidForm() {
		// Require a non-empty repo name
		if (_repoNameFld.getText().trim().isEmpty()) {
			String msg = "Please Enter a name for the Repository";
			String title = "No Name Entered";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Require a non-empty study folder path
		if (_studyFolderFld.getPath().isEmpty()) {
			String msg = "Please Enter the path for the study";
			String title = "No Study Entered";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Require a valid repo location selection in the tree
		if (getRepoPath() == null) {
			String msg = "Invalid Repository Location Selected";
			String title = "Invalid Repository URL";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// All checks passed
		return true;
	}

	/**
	 * Returns the root-relative path of the selected location in the repo location tree.
	 *
	 * @return the relative path string, or null if no valid location is selected
	 */
	public String getRepoPath() {
		return _repoLocationTree.getRepoPath();
	}

	/**
	 * Returns whether the "Preserve History" checkbox is checked.
	 *
	 * @return true if the existing Git history should be retained; false to start fresh
	 */
	public boolean shouldKeepHistory() {
		return _preserveHistoryCheck.isSelected();
	}

	/**
	 * Returns the trimmed text from the repo name field.
	 *
	 * @return the repository display name entered by the user
	 */
	public String getRepoName() {
		return _repoNameFld.getText().trim();
	}

	/**
	 * Returns the trimmed text from the repo description field.
	 *
	 * @return the optional repository description, or an empty string if not provided
	 */
	public String getRepoDescription() {
		return _repoDescFld.getText().trim();
	}

	/**
	 * Returns the trimmed path from the study folder file-chooser field.
	 *
	 * @return the absolute local path of the study folder
	 */
	public String getLocalFolder() {
		return _studyFolderFld.getPath().trim();
	}

	/**
	 * Returns the parent GitLab server URL from the repo location tree.
	 *
	 * @return the base GitLab server URL (e.g., "https://gitlab.rmanet.app")
	 */
	public String getParentUrl() {
		return _repoLocationTree.getParentUrl();
	}

	/**
	 * Standalone test entry point for visual testing of the dialog without a running WAT application.
	 *
	 * @param args unused command-line arguments
	 */
	public static void main(String[] args) {
		CreateRepoDialog dlg = new CreateRepoDialog(new JFrame());
		dlg.setVisible(true);
	}
}
