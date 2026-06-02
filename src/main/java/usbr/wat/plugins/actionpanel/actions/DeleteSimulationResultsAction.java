package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                                  // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.List;                                                              // Collections interface used for lists of results

import javax.swing.AbstractAction;                                                  // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                                     // Swing utility for showing information and confirmation dialogs

import com.rma.io.FileManagerImpl;                                                  // File manager implementation that provides filesystem operations (delete directory, etc.)

import usbr.wat.plugins.actionpanel.ActionsWindow;                                  // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.model.ResultsData;                              // Data model representing a single results entry, including its folder and metadata
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                                   // Base USBR panel type implemented by calibration and forecast panels
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;                // Tree-table node representing a simulation in the UI, used to remove results from the view

/**
 * Action that deletes selected simulation results from disk and updates the UI.
 *
 * Validates that results are selected, confirms with the user, deletes the
 * corresponding results folders, and removes the results from the simulation tree.
 */
public class DeleteSimulationResultsAction extends AbstractAction {

	/** Owning actions window used as parent for dialogs and to access selections. */
	private ActionsWindow _parent;

	/** Panel that hosts the simulation tree; used to update the visible nodes after deletion. */
	private UsbrPanel _parentPanel;

	/**
	 * Creates the delete-results action with a user-visible name and an initial disabled state.
	 *
	 * @param parent the actions window used as the UI parent and selection source
	 * @param parentPanel the panel that contains the simulation tree and results
	 */
	public DeleteSimulationResultsAction(ActionsWindow parent, UsbrPanel parentPanel) {
		// Initialize the action with its display label
		super("Delete Results");

		// Start disabled until the selection enables it
		setEnabled(false);

		// Store references to the owning window and panel
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to delete selected simulation results.
	 *
	 * @param e the action event initiating the deletion request
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Delegate to the deletion workflow
		deleteResults();
	}

	/**
	 * Performs the deletion of selected results with user confirmation.
	 *
	 * Builds a confirmation message listing selected results, prompts the user,
	 * deletes each results folder, and updates the simulation tree to remove
	 * the corresponding results entries.
	 */
	private void deleteResults() {
		// Gather the results currently selected in the actions window
		List<ResultsData> results = _parent.getSelectedResults();

		// No-op when nothing is selected
		if ( results.isEmpty()) {
			return;
		}

		// Build a confirmation message listing the selected results
		String msg = "&lt;html&gt;Do you want to delete the following results:&lt;br&gt;";

		for(int i = 0;i < results.size();i++ ) {
			// Append each result's display name to the message
			msg = msg.concat(results.get(i).getName());

			msg = msg.concat("&lt;br&gt;");
		}

		msg = msg.concat("&lt;/html&gt;");

		// Prompt the user to confirm deletions
		String title = "Confirm Deletions";

		int opt = JOptionPane.showConfirmDialog(_parent, msg, title, JOptionPane.YES_NO_OPTION);

		// Abort if the user declines
		if ( JOptionPane.YES_OPTION != opt ) {
			return;
		}

		// Proceed to delete each selected results folder
		ResultsData result;

		for (int i = 0;i < results.size(); i++ ) {
			result = results.get(i);

			// Attempt to remove the results directory on disk
			if ( deleteResultsFolder(result.getFolder())) {
				// Update the simulation node to remove the deleted results entry from the UI
				SimulationTreeTableNode simNode = _parentPanel.getSimulationTreeTable().getSimulationNodeFor(result.getSimulation());

				simNode.removeResultsFor(result);

			} else {
				// Inform the user that a particular folder could not be removed
				JOptionPane.showMessageDialog(_parent, "Failed to remove folder "
						+result.getFolder()+" for "+result.getName(), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * Deletes the specified results folder from disk.
	 *
	 * @param resultsFolder absolute path to the results folder to delete
	 * @return true if the folder was successfully deleted, false otherwise
	 */
	private boolean deleteResultsFolder(String resultsFolder) {
		// Delegate deletion to the file manager implementation
		return FileManagerImpl.getFileManager().deleteDirectory(resultsFolder);
	}
}