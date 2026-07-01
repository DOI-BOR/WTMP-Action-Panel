package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;          // AWT Window used as the parent for confirmation dialogs

import javax.swing.JOptionPane;  // Provides standard confirmation and informational dialogs

import com.rma.client.Browser;   // RMA browser frame for opening and closing WAT study (project) files
import com.rma.model.Project;    // Represents the currently loaded RMA project; used to check and close the study

/**
 * Abstract base class for Git actions that require the currently open WAT study
 * to be closed before the operation can proceed, and optionally reopened afterward.
 *
 * Extends AbstractGitAction to inherit the Git subprocess invocation machinery,
 * and adds three protected lifecycle methods:
 *
 *   - askToCloseStudy(): prompts the user to confirm that the study should be closed.
 *   - closeStudy(): saves the current project file path, then closes the study via the
 *     browser frame. Returns true if no project is open (no-op) or the close succeeds.
 *   - openStudy(): re-opens the project that was closed by closeStudy() using the
 *     saved path; no-op if closeStudy() was never called.
 *
 * Concrete subclasses (e.g., DownloadStudyAction, RestoreStudyAction) must implement
 * getType() to supply a human-readable operation name used in the close-study prompt
 * (e.g., "Downloading", "Restoring").
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public abstract class AbstractStudyGitAction extends AbstractGitAction {
	// Absolute path to the project file that was open before closeStudy() was called;
	// used by openStudy() to re-open the project after the Git operation completes
	private String _lastProjectPath;

	/**
	 * Constructs an AbstractStudyGitAction with the given display name and parent window.
	 *
	 * @param name   the display name for this action (used as the button label)
	 * @param parent the Window used as the owner for close-study confirmation dialogs
	 */
	public AbstractStudyGitAction(String name, Window parent) {
		super(name, parent);
	}

	/**
	 * Re-opens the WAT study project that was closed by a prior call to closeStudy().
	 *
	 * If _lastProjectPath is non-null, opens the saved project file path via the
	 * RMA browser frame and clears the stored path to prevent a second re-open.
	 * Has no effect if closeStudy() was never called or the project was never closed.
	 */
	protected void openStudy() {
		if (_lastProjectPath != null) {
			// Re-open the project that was previously closed
			Browser.getBrowserFrame().projectOpen(_lastProjectPath);

			// Clear the stored path to prevent a redundant re-open on subsequent calls
			_lastProjectPath = null;
		}
	}

	/**
	 * Returns the human-readable operation type name for use in the close-study prompt.
	 *
	 * Concrete subclasses must implement this to return a gerund describing the
	 * operation (e.g., "Downloading", "Restoring").
	 *
	 * @return the operation type name displayed in the close-study confirmation dialog
	 */
	protected abstract String getType();

	/**
	 * Prompts the user to confirm that the currently open study should be closed before
	 * the Git operation proceeds.
	 *
	 * Shows a YES/NO confirmation dialog if a project is currently open. Returns true
	 * immediately (no confirmation needed) if no project is loaded.
	 *
	 * @return true if the user confirmed or no project is open; false if the user declined
	 */
	protected boolean askToCloseStudy() {
		if (!Project.getCurrentProject().isNoProject()) {
			// Build the close-study confirmation message using the subclass operation type
			String msg = getType() + " requires that the Study be closed first.  Continue?";
			String title = "Close Study";
			int opt = JOptionPane.showConfirmDialog(getParent(), msg, title, JOptionPane.YES_NO_OPTION);

			if (JOptionPane.YES_OPTION != opt) {
				// User declined; the Git operation should not proceed
				return false;
			}
		}

		// No project is open or the user confirmed; safe to proceed
		return true;
	}

	/**
	 * Closes the currently open WAT study and saves its file path for later re-opening.
	 *
	 * If a project is currently open, stores its absolute file path in _lastProjectPath
	 * and closes it via the RMA browser frame. Shows an error dialog if the close fails.
	 * Returns true immediately (no-op) if no project is currently loaded.
	 *
	 * @return true if the study was closed successfully or no project was open;
	 * false if the close operation failed
	 */
	protected boolean closeStudy() {
		if (!Project.getCurrentProject().isNoProject()) {
			// Record the absolute path of the current project file for re-opening later
			_lastProjectPath = Project.getCurrentProject().getProjectFile().getAbsolutePath();

			// Attempt to close the project via the browser frame
			boolean rv = Browser.getBrowserFrame().closeProjectAction();

			if (!rv) {
				// Notify the user if the close operation failed
				JOptionPane.showMessageDialog(getParent(), "Failed to close Study.", "Close Failed", JOptionPane.INFORMATION_MESSAGE);
			}

			return rv;
		}

		// No project is open; nothing to close
		return true;
	}
}
