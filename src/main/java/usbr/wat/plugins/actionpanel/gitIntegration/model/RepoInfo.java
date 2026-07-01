package usbr.wat.plugins.actionpanel.gitIntegration.model;

import hec.lang.NamedType; // HEC base class providing a name field and comparison support for named model objects

/**
 * Data model representing a Git repository definition within the WTMP Action Panel.
 *
 * Stores the three pieces of information needed to identify and locate a repository:
 * the display name (inherited from NamedType), the remote source URL (the GitLab
 * HTTP clone URL), and the local file system path where the study has been cloned.
 *
 * Instances are created when loading saved repo definitions from the WAT project
 * preferences (via GitRepoUtils) and when the user adds a new repo through the
 * ReposEditor dialog. They are displayed by name in the repo combo box of the
 * StudyStorageDialog and passed to action classes to drive Git operations.
 *
 * This class is suppressed for serialization warnings because NamedType is not
 * consistently serializable.
 */
@SuppressWarnings("serial")
public class RepoInfo extends NamedType {
	// The GitLab HTTP clone URL for the remote repository (e.g., https://gitlab.example.com/group/repo.git)
	private String _sourceUrl;

	// The absolute local file system path where the repository has been cloned
	private String _localPath;

	/**
	 * Constructs an empty RepoInfo with no name, URL, or local path set.
	 *
	 * Fields should be populated via the setters before the instance is used.
	 */
	public RepoInfo() {
		super();
	}

	/**
	 * Returns the GitLab HTTP clone URL of this repository.
	 *
	 * @return the remote source URL string
	 */
	public String getSourceUrl() {
		return _sourceUrl;
	}

	/**
	 * Sets the GitLab HTTP clone URL of this repository.
	 *
	 * @param sourceUrl the remote source URL to store
	 */
	public void setSourceUrl(String sourceUrl) {
		_sourceUrl = sourceUrl;
	}

	/**
	 * Returns the absolute local file system path where this repository is cloned.
	 *
	 * @return the local path string
	 */
	public String getLocalPath() {
		return _localPath;
	}

	/**
	 * Sets the absolute local file system path where this repository is cloned.
	 *
	 * @param localPath the local path string to store
	 */
	public void setLocalPath(String localPath) {
		_localPath = localPath;
	}

	/**
	 * Returns the display name of this repository, used when rendering in combo boxes and lists.
	 *
	 * @return the name string inherited from NamedType
	 */
	@Override
	public String toString() {
		return getName();
	}
}
