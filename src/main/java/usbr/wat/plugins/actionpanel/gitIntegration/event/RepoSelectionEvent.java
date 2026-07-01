package usbr.wat.plugins.actionpanel.gitIntegration.event;

/**
 * Immutable event object carrying the details of a repository selection change
 * in the RepoJTree component.
 *
 * Fired by RepoJTree whenever the user selects a different node in the repository
 * browser tree. Carries three related values that allow listeners to respond to
 * both the display name and the full or relative URL of the selected repository:
 *
 *   - repoName:  the display name of the selected project node (null for folder nodes)
 *   - repoUrl:   the full GitLab URL to the selected repository
 *   - repoPath:  the path of the selected node relative to the GitLab root
 *
 * Delivered to all registered RepoSelectionListener instances.
 */
public class RepoSelectionEvent {
	// The display name of the selected project node; null if a folder (non-project) node was selected
	private String _repoName;

	// The full GitLab URL to the selected repository (includes server base URL)
	private String _repoUrl;

	// The path of the selected node relative to the configured GitLab root path
	private String _repoPath;

	/**
	 * Constructs a RepoSelectionEvent with the given name, URL, and path.
	 *
	 * @param repoName the display name of the selected project; null for folder nodes
	 * @param repoUrl  the full GitLab URL to the selected repository
	 * @param repoPath the path relative to the GitLab root for the selected node
	 */
	public RepoSelectionEvent(String repoName, String repoUrl, String repoPath) {
		super();
		_repoName = repoName;
		_repoUrl = repoUrl;
		_repoPath = repoPath;
	}

	/**
	 * Returns the display name of the selected project node.
	 *
	 * @return the project display name, or null if a folder node was selected
	 */
	public String getRepoName() {
		return _repoName;
	}

	/**
	 * Returns the full GitLab URL to the selected repository.
	 *
	 * @return the full URL string including the server base and path
	 */
	public String getRepoUrl() {
		return _repoUrl;
	}

	/**
	 * Returns the path of the selected node relative to the GitLab root path.
	 *
	 * @return the relative path string for the selected tree node
	 */
	public String getRepoPath() {
		return _repoPath;
	}
}
