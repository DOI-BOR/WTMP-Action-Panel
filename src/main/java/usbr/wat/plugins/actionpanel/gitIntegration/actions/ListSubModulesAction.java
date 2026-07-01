package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;            // AWT Window used as the parent for Git tool invocation
import java.awt.event.ActionEvent; // Represents an action event fired when the action is triggered
import java.util.ArrayList;       // Resizable-array List for building command arguments and accumulating results
import java.util.Iterator;        // Iterator for walking the output list during header-stripping parsing
import java.util.List;            // Ordered collection interface for command argument and submodule name lists

import hec.io.ProcessOutputLine;  // HEC wrapper for a single line of process output; used during output parsing

import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog; // Dialog providing the currently selected repository (used by the convenience constructor)
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;     // Data model holding the local path of the target repository

/**
 * Action for listing the Git submodules defined within a WAT study repository.
 *
 * Invokes the WAT Git tool with the --listsubmodules command for the given
 * repository's local folder. Parses the raw output to strip preamble lines,
 * retaining only the lines that follow the "Submodules in main repo:" sentinel
 * or the "No Submodules detected." sentinel (which indicates an empty list).
 *
 * The parsed submodule names are used by DownloadStudyAction, RestoreStudyAction,
 * and UploadStudyAction to populate the --submodule flags when operating on
 * individual submodules rather than the entire repository.
 *
 * The static parseOutput() method is package-accessible so that ShowChangesActions
 * and other action classes can reuse the same header-stripping logic.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ListSubModulesAction extends AbstractGitAction {
	// Sentinel string emitted when no submodules are detected in the repository
	public static final String NO_SUBMODULES = "No Submodules detected.";

	// Sentinel string marking the start of the submodule list in the Git tool output
	public static final String SUBMODULES_START = "Submodules in main repo:";

	// Git tool command flag for listing all submodules in the repository
	private static final String LIST_SUBMODULES_CMD = "--listsubmodules";

	// The repository whose submodules should be listed
	private RepoInfo _repo;

	/**
	 * Constructs a ListSubModulesAction using the currently selected repo from the given dialog.
	 *
	 * Convenience constructor that delegates to the primary constructor.
	 *
	 * @param parent the Window used as the owner for the Git tool wait cursor
	 * @param ssd    the StudyStorageDialog providing the currently selected repository
	 */
	public ListSubModulesAction(Window parent, StudyStorageDialog ssd) {
		this(parent, ssd.getSelectedRepo());
	}

	/**
	 * Constructs a ListSubModulesAction for the given parent window and repository.
	 *
	 * @param parent the Window used as the owner for the Git tool wait cursor
	 * @param repo   the RepoInfo describing the repository whose submodules should be listed
	 */
	public ListSubModulesAction(Window parent, RepoInfo repo) {
		super("List Submodules", parent);
		_repo = repo;
	}

	/**
	 * Invoked when the action is triggered.
	 *
	 * Delegates to getSubModules() to run the submodule listing.
	 *
	 * @param e the ActionEvent fired by the button or menu item
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		getSubModules();
	}

	/**
	 * Lists all submodules in the configured repository and returns their names.
	 *
	 * Logs a message and returns an empty list if no repository is configured.
	 * Invokes the --listsubmodules Git tool command, parses the output to strip
	 * the preamble, and returns the remaining lines as submodule names.
	 * Returns an empty list if the Git call fails.
	 *
	 * @return a List of submodule name strings; empty if no repo is set or the call fails
	 */
	public List<String> getSubModules() {
		RepoInfo repo = _repo;

		if (repo == null) {
			// Log and return an empty list if no repository is configured
			_logger.info("No Repo selected. Can't get submodules");
			return new ArrayList<>();
		}

		// Build the --listsubmodules command with the repo's local folder path
		List<String> cmd = new ArrayList<>();
		cmd.add(LIST_SUBMODULES_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());

		boolean rv = callGit(cmd);

		if (rv) {
			// Strip preamble lines and return the submodule name lines
			List<ProcessOutputLine> output = parseOutput(getOutput(), SUBMODULES_START, NO_SUBMODULES);
			return getOutputLines(output);
		}

		// Return an empty list if the Git call failed
		return new ArrayList<>();
	}


	/**
	 * Strips preamble lines from the Git tool output, retaining only lines
	 * that follow one of the two sentinel strings.
	 *
	 * Iterates the output list, removing each line until the changesStart or
	 * noChanges sentinel is found. The sentinel line itself is removed; everything
	 * after it is retained. If neither sentinel is found, all lines are returned
	 * (the full output is treated as relevant content).
	 *
	 * This method is package-static so it can be reused by ShowChangesActions
	 * and other action classes with different sentinel strings.
	 *
	 * @param output       the full list of ProcessOutputLine objects from the Git tool
	 * @param changesStart the sentinel string marking the start of the relevant content
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
