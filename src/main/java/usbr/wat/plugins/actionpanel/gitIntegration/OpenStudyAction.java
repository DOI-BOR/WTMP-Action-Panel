package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.event.ActionEvent;    // Represents an action event fired when the Open Study button is clicked
import java.util.List;                // Ordered collection interface for the list of .sty file paths found in the repo folder

import javax.swing.AbstractAction;   // Base class for Swing Action implementations used to back a JButton
import javax.swing.Action;           // Interface defining action properties; used to set the SHORT_DESCRIPTION tooltip
import javax.swing.JOptionPane;      // Provides informational dialog boxes shown when no study file is found

import com.rma.io.FileManagerImpl;   // RMA file manager for listing files by extension within a directory

import hec2.wat.WAT;                 // HEC-WAT application entry point; used to open a study (project) file

import rma.util.RMAFilenameFilter;   // RMA file filter for restricting the file listing to a specific extension (.sty)

import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo; // Data model holding the local path and URL of a Git repository

/**
 * Swing Action for opening the WAT study file (.sty) found in the local folder
 * of the currently selected Git repository.
 *
 * Searches the repository's local directory for a .sty file (non-recursively,
 * excluding sub-directories). If exactly one or more .sty files are found, the
 * first is opened via the WAT browser frame. If none are found, an informational
 * dialog notifies the user.
 *
 * This action is backed by the Open Study button in RepoButtonPanel and is
 * enabled/disabled along with the other repository action buttons whenever the
 * selected repository changes.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class OpenStudyAction extends AbstractAction {
	// Reference to the parent StudyStorageDialog for retrieving the currently selected repository
	private StudyStorageDialog _studyStorageDialog;

	/**
	 * Constructs an OpenStudyAction associated with the given StudyStorageDialog.
	 *
	 * Sets the action's display name to "Open Study" and its short description
	 * (tooltip) to describe the action's purpose.
	 *
	 * @param studyStorageDialog the dialog from which the selected repository is retrieved
	 */
	public OpenStudyAction(StudyStorageDialog studyStorageDialog) {
		// Set the button label for this action
		super("Open Study");

		// Set the tooltip text shown when hovering over the button
		putValue(Action.SHORT_DESCRIPTION, "Opens the Study for the selected Repository");

		// Store the dialog reference for later repo retrieval
		_studyStorageDialog = studyStorageDialog;
	}

	/**
	 * Invoked when the Open Study button is clicked.
	 *
	 * Delegates to openStudyAction() to perform the file search and project open.
	 *
	 * @param e the ActionEvent fired by the button
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		openStudyAction();
	}

	/**
	 * Searches the selected repository's local folder for a WAT study file (.sty)
	 * and opens the first one found via the WAT browser frame.
	 *
	 * Constructs a file filter for .sty files (directories excluded), lists matching
	 * files in the repository's local path, and opens the first result. If no .sty
	 * file is found, an informational dialog is shown to the user.
	 */
	public void openStudyAction() {
		// Retrieve the currently selected repository from the parent dialog
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();

		// Get the local file system path for this repository
		String studyFolder = repo.getLocalPath();

		// Create a file filter restricted to .sty files; exclude directories from the listing
		RMAFilenameFilter filter = new RMAFilenameFilter("sty");
		filter.setAcceptDirectories(false);

		// List all .sty files in the study folder (non-recursive)
		List<String> studyFiles = FileManagerImpl.getFileManager().list(studyFolder, filter, false);

		if (studyFiles != null && !studyFiles.isEmpty()) {
			// Open the first .sty file found in the repository's local folder
			WAT.getBrowserFrame().projectOpen(studyFiles.get(0));
		} else {
			// Inform the user that no study file was found in the expected location
			JOptionPane.showMessageDialog(_studyStorageDialog, "No Study files found in " + studyFolder,
					"No Study Opened", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
