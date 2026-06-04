package usbr.wat.plugins.actionpanel.gitIntegration.event;

/**
 * Listener interface for receiving repository selection change notifications
 * from a RepoJTree component.
 *
 * Implementors are notified via repoSelectionChanged() whenever the user selects
 * a different node in the repository browser tree. The event carries the selected
 * node's display name, full URL, and root-relative path.
 *
 * Listeners are registered via RepoJTree.addRepoSelectionListener() and removed
 * via RepoJTree.removeRepoSelectionListener().
 */
public interface RepoSelectionListener {
	/**
	 * Called when the user selects a different repository node in the RepoJTree.
	 *
	 * @param event the RepoSelectionEvent carrying the selected node's name, URL, and path
	 */
	void repoSelectionChanged(RepoSelectionEvent event);
}
