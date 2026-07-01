package usbr.wat.plugins.actionpanel.gitIntegration.utils;

import java.awt.Window;             // AWT Window used as the parent for error dialogs in getRepoUrl()
import java.io.BufferedReader;      // Buffered character reader for reading the git remote get-url process output
import java.io.File;                // Represents a file system path; used for the .git directory and process working directory
import java.io.IOException;         // Checked exception for process launch and repository open failures
import java.io.InputStream;         // Raw byte stream from the git process stdout and stderr
import java.io.InputStreamReader;   // Wraps a process InputStream as a character reader
import java.util.ArrayList;        // Resizable-array List for building command lists and accumulating repos
import java.util.Collection;       // Collection interface used for listing remote refs from JGit
import java.util.Iterator;         // Iterator for walking configuration sections
import java.util.List;             // Ordered collection interface for repo and output line lists
import java.util.Set;              // Set of configuration section names returned by JGit StoredConfig
import java.util.Vector;           // Synchronized growable array for collecting process output lines
import java.util.concurrent.ExecutionException; // Exception wrapping a SwingWorker background computation failure
import java.util.prefs.BackingStoreException;   // Checked exception thrown when the preferences backing store fails
import java.util.prefs.Preferences;             // Persistent hierarchical key-value store for saving repo definitions

import javax.swing.JOptionPane;    // Provides standard confirmation and informational dialogs
import javax.swing.SwingWorker;    // Background worker for running Git status checks off the EDT

import org.eclipse.jgit.api.Git;                         // JGit API entry point for Git operations (used in main() for ls-remote)
import org.eclipse.jgit.api.errors.GitAPIException;      // Exception thrown by JGit API operations
import org.eclipse.jgit.lib.Ref;                         // JGit reference (branch, tag, etc.) returned by ls-remote
import org.eclipse.jgit.lib.Repository;                  // JGit Repository object representing an open Git repo
import org.eclipse.jgit.lib.StoredConfig;                // JGit configuration reader for the .git/config file
import org.eclipse.jgit.storage.file.FileRepositoryBuilder; // JGit builder for opening a local file-based repository
import org.eclipse.jgit.transport.CredentialsProvider;      // JGit interface for supplying authentication credentials
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider; // JGit concrete credentials implementation

import com.google.common.flogger.FluentLogger;   // Google Flogger for structured logging of process launch failures
import com.rma.client.BrowserPreferences;        // RMA browser preferences providing access to the WAT preference tree
import com.rma.io.FileManagerImpl;               // RMA file manager for checking file/directory existence

import hec.io.ProcessOutputLine;   // HEC wrapper for a single process output line with stream-type metadata
import hec.io.ProcessOutputReader; // HEC utility for reading process output into a list on a background thread

import hec2.wat.WAT;               // HEC-WAT application entry point providing access to the browser frame

import rma.util.RMAIO;             // RMA I/O utility for path comparison, concatenation, and other string helpers

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                    // Provides the singleton plugin instance for accessing the ActionsWindow
import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;    // Provides system property keys and the static showErrorMsg/getErrorMessage methods
import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;  // Used by checkRepoOutofDateStatus() to trigger a download after prompt
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;   // Used by checkRepoOutofDateStatus() to check for pending commits
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;               // Data model holding the name, local path, and source URL of a saved repository

/**
 * Utility class for managing the list of Git repository definitions saved in the
 * WAT project preferences, and for performing low-level Git operations using both
 * JGit and the native git command-line tool.
 *
 * Responsibilities:
 *   - Load, add, update, and delete RepoInfo definitions from the WAT preferences
 *     tree under the "gitrepos" node.
 *   - Check whether a local folder is a known Git repository (via the in-memory list
 *     or the presence of a .git directory).
 *   - Read the remote origin URL from a local clone's .git/config via JGit.
 *   - Retrieve the remote origin URL by running "git remote get-url origin" as a
 *     subprocess and parsing the stdout output.
 *   - Background-check a project's repository for pending commits on project open,
 *     prompting the user to download if changes are found.
 *
 * The in-memory _repos list acts as a lazy-loaded cache that is populated from
 * preferences on the first call to getReposList() and is kept synchronized by
 * addRepo() and deleteRepo().
 *
 * This class is not instantiable; all functionality is accessed via static methods.
 *
 */
public class GitRepoUtils {
	// The name of the .git directory used to detect an initialized Git clone
	public static final String GIT_FOLDER = ".git";

	// Preferences key storing the local file system path for a saved repository
	private static final String LOCAL_FOLDER = "LocalFolder";

	// Preferences key storing the remote source URL for a saved repository
	private static final String REPO_URL = "RepoUrl";

	// JGit config section name for the remote configuration
	private static final String REMOTE_SECTION = "remote";

	// JGit config subsection name for the primary (origin) remote
	private static final String ORIGIN_SUB_SECTION = "origin";

	// JGit config key name for the remote URL within the remote/origin subsection
	private static final String URL_NAME = "url";

	// Cached JGit credentials provider (currently unused; placeholder for future integration)
	private static CredentialsProvider _credentials;

	// In-memory cache of loaded RepoInfo definitions; populated lazily by getReposList()
	public static List<RepoInfo> _repos = new ArrayList<>();

	/**
	 * Private constructor preventing instantiation of this utility class.
	 */
	private GitRepoUtils() {
	}

	/**
	 * Returns the list of saved repository definitions.
	 *
	 * If the in-memory cache is non-empty, it is returned directly. Otherwise,
	 * loads all RepoInfo instances from the WAT preferences under the "gitrepos"
	 * node. Each child node name is the repository's display name; the node holds
	 * LocalFolder and RepoUrl keys. Nodes missing a LocalFolder are skipped.
	 *
	 * @return the list of loaded RepoInfo instances; may be empty if no repos are defined
	 */
	public static List<RepoInfo> getReposList() {
		// Return the cached list if it has already been populated
		if (_repos.size() > 0) {
			return _repos;
		}

		Preferences repoNode = getRepoNode();

		// Load all child node names (each is a saved repo's display name)
		String[] repoNames = null;
		try {
			repoNames = repoNode.childrenNames();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return _repos;
		}

		Preferences kidNode;
		RepoInfo info;
		String localFolder, repoUrl;

		for (int i = 0; i < repoNames.length; i++) {
			kidNode = repoNode.node(repoNames[i]);

			// Read the local folder path; skip entries without one
			localFolder = kidNode.get(LOCAL_FOLDER, null);
			if (localFolder == null) {
				continue;
			}

			// Read the remote URL (may be null for manually-created entries)
			repoUrl = kidNode.get(REPO_URL, null);

			// Build and cache the RepoInfo object
			info = new RepoInfo();
			info.setName(repoNames[i]);
			info.setLocalPath(localFolder);
			info.setSourceUrl(repoUrl);
			_repos.add(info);
		}

		return _repos;
	}

	/**
	 * Initializes the cached credentials provider if not already set.
	 *
	 * Currently a no-op stub; the GitCredentialManager integration is not yet complete.
	 *
	 * @return true if credentials are available; false if initialization failed
	 */
	public static boolean initCreditials() {
		if (_credentials == null) {
			// TODO: _credentials = GitCredentialManager.init();
		}
		return _credentials != null;
	}

	/**
	 * Returns the WAT preferences node under which all repo definitions are stored.
	 *
	 * Navigates to: [WAT browser preferences]/gitrepos
	 *
	 * @return the Preferences node for the repo list
	 */
	private static Preferences getRepoNode() {
		BrowserPreferences prefs = WAT.getBrowserFrame().getPreferences();
		Preferences repoNode = prefs.getNode("gitrepos");
		return repoNode;
	}

	/**
	 * Checks whether the given local folder is a known Git repository.
	 *
	 * Returns true if the folder path matches any entry in the saved repo list
	 * (path-equality comparison) or if a .git directory exists within the folder.
	 * Returns false immediately if localFolder is null.
	 *
	 * @param localFolder the absolute local path to check
	 * @return true if the folder is a known or detected Git repository; false otherwise
	 */
	public static boolean hasGitRepo(String localFolder) {
		if (localFolder == null) {
			return false;
		}

		// Check whether any saved repo has the same local path
		List<RepoInfo> repos = getReposList();
		RepoInfo info;
		for (int i = 0; i < repos.size(); i++) {
			info = repos.get(i);
			if (RMAIO.pathsEqual(localFolder, info.getLocalPath())) {
				return true;
			}
		}

		// Fall back to checking for a .git directory in the given folder
		String gitFolder = RMAIO.concatPath(localFolder, GIT_FOLDER);
		return FileManagerImpl.getFileManager().fileExists(gitFolder);
	}

	/**
	 * Reads the remote origin URL from a local Git clone's .git/config file via JGit.
	 *
	 * Opens the repository at info.getLocalPath()/.git using JGit's FileRepositoryBuilder,
	 * reads the [remote "origin"] url configuration value, and stores it in info.setSourceUrl().
	 * Has no effect if info is null or the repository cannot be opened.
	 *
	 * @param info the RepoInfo whose local path identifies the clone to inspect;
	 *             the sourceUrl field is updated in place on success
	 */
	public static void getRepoInfo(RepoInfo info) {
		if (info == null) {
			return;
		}

		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String folder = info.getLocalPath();
		File repoDir = new File(folder, GIT_FOLDER);
		Repository repo;

		try {
			// Open the JGit repository from the .git directory
			repo = builder.setGitDir(repoDir)
					.readEnvironment()  // scan GIT_* environment variables
					.findGitDir()       // scan up the file system tree if needed
					.build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		try {
			// Read the remote origin URL from the .git/config [remote "origin"] section
			StoredConfig config = repo.getConfig();
			String remoteUrl = config.getString(REMOTE_SECTION, ORIGIN_SUB_SECTION, URL_NAME);
			info.setSourceUrl(remoteUrl);
		} finally {
			// Always close the JGit repository to release file handles
			repo.close();
		}
	}

	/**
	 * Deletes the given repository definition from the WAT preferences and in-memory cache.
	 *
	 * Checks whether the preferences node for the repo's name exists, then removes it.
	 * Also removes the RepoInfo from the _repos cache list.
	 * Returns false if info is null, the preferences node does not exist, or the
	 * delete operation fails.
	 *
	 * @param info the RepoInfo to delete; must have a non-null name
	 * @return true if the repo was deleted successfully; false otherwise
	 */
	public static boolean deleteRepo(RepoInfo info) {
		if (info == null) {
			return false;
		}

		Preferences reposNode = getRepoNode();
		String name = info.getName();

		try {
			if (reposNode.nodeExists(name)) {
				Preferences repoNode = reposNode.node(name);
				try {
					// Remove the preferences node and update the in-memory cache
					repoNode.removeNode();
					_repos.remove(info);
					return true;
				} catch (BackingStoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Adds the given repository definition to the WAT preferences and in-memory cache.
	 *
	 * If addIfExists is false and the local folder is already associated with a known
	 * Git repository, the add is skipped. Otherwise delegates to writeRepo() to persist
	 * the data and adds the RepoInfo to the in-memory cache on success.
	 *
	 * @param info        the RepoInfo to add; must have name, localPath, and sourceUrl set
	 * @param addIfExists true to add even if the local folder already has a Git repo; false to skip
	 * @return true if the repo was successfully added; false otherwise
	 */
	public static boolean addRepo(RepoInfo info, boolean addIfExists) {
		if (info == null) {
			return false;
		}

		// Skip if the local folder is already a Git repo and addIfExists is false
		if (hasGitRepo(info.getLocalPath()) && !addIfExists) {
			return false;
		}

		if (writeRepo(info)) {
			// Add to the in-memory cache only after successful persistence
			_repos.add(info);
			return true;
		}

		return false;
	}

	/**
	 * Persists a repository definition to the WAT preferences store.
	 *
	 * Creates or updates a preferences child node named after the repo's display name
	 * and stores the LocalFolder and RepoUrl key-value pairs.
	 *
	 * @param info the RepoInfo whose data should be written to preferences
	 * @return true if the write succeeded; false on any exception
	 */
	public static boolean writeRepo(RepoInfo info) {
		Preferences reposNode = getRepoNode();
		String name = info.getName();

		try {
			// Create or open the child node for this repo and write both fields
			Preferences repoNode = reposNode.node(name);
			repoNode.put(LOCAL_FOLDER, info.getLocalPath());
			repoNode.put(REPO_URL, info.getSourceUrl());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Validates whether the given remote URL is a reachable Git remote.
	 *
	 * Currently always returns true; a proper implementation would attempt to
	 * connect to the remote and verify its existence.
	 *
	 * @param remoteUrl the URL to validate
	 * @return true always (validation not yet implemented)
	 */
	public static boolean isValidRemoteUrl(String remoteUrl) {
		try {
			// TODO: implement actual URL reachability check
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Standalone test entry point for manually inspecting a local Git repository
	 * using JGit and printing its configuration, branches, and remote refs.
	 *
	 * @param args the local repository folder path (required as args[0])
	 */
	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			System.out.println("main:" + GitRepoUtils.class.getName() + " local-repo-folder");
			System.exit(1);
		}

		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String folder = args[0];
		File repoDir = new File(folder, ".git");
		Repository repo;

		try {
			repo = builder.setGitDir(repoDir).readEnvironment().findGitDir().build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		// Print basic repository information for diagnostic purposes
		System.out.println("repo dir:" + repo.getDirectory());
		try {
			System.out.println("branch:" + repo.getBranch());
		} catch (IOException e) {
		}

		System.out.println("Identifier:" + repo.getIdentifier());
		System.out.println("Index file:" + repo.getIndexFile());
		System.out.println("repo state:" + repo.getRepositoryState());
		System.out.println("work tree:" + repo.getWorkTree());

		StoredConfig config = repo.getConfig();

		// Read and print the origin remote URL
		String remoteUrl = config.getString(REMOTE_SECTION, ORIGIN_SUB_SECTION, URL_NAME);

		try {
			// List all remote refs using placeholder credentials
			CredentialsProvider credientials = new UsernamePasswordCredentialsProvider("username_placeholder", "password_placeholder");
			Collection<Ref> refs = Git.lsRemoteRepository().setCredentialsProvider(credientials).setRemote(remoteUrl).call();
			System.out.println("main:refs=" + refs);
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Enumerate and print all configuration sections, subsections, and keys
		Set<String> sections = config.getSections();
		Iterator<String> sectionIter = sections.iterator();
		while (sectionIter.hasNext()) {
			String section = sectionIter.next();
			System.out.println("Section is " + section);
			Set<String> subsections = config.getSubsections(section);
			Iterator<String> subsectionsIter = subsections.iterator();
			while (subsectionsIter.hasNext()) {
				String subsection = subsectionsIter.next();
				System.out.println("\tSubsection:" + subsection);
				Set<String> names = config.getNames(section, subsection, true);
				Iterator<String> namesIter = names.iterator();
				while (namesIter.hasNext()) {
					String name = namesIter.next();
					String value = config.getString(section, subsection, name);
					System.out.println("\tName is " + name + "=" + value);
				}
			}
		}

		repo.close();
	}

	/**
	 * Checks whether the repository associated with the given project folder is
	 * behind the remote and prompts the user to download if changes are found.
	 *
	 * Scans the saved repo list for a repo whose localPath matches the project folder.
	 * For each match, delegates to the single-repo overload.
	 *
	 * @param projectFolder the absolute path of the project folder to check
	 */
	public static void checkRepoOutofDateStatus(String projectFolder) {
		List<RepoInfo> repos = getReposList();
		RepoInfo repo;

		for (int i = 0; i < repos.size(); i++) {
			repo = repos.get(i);

			// Use path-equality comparison to handle OS case and separator differences
			if (RMAIO.pathsEqual(projectFolder, repo.getLocalPath())) {
				checkRepoOutofDateStatus(repo);
			}
		}
	}

	/**
	 * Runs a commit-behind check for the given repository on a SwingWorker background
	 * thread and prompts the user to download if behind commits are found.
	 *
	 * On completion, if there are pending commits, shows a YES/NO confirmation dialog.
	 * If the user confirms, triggers DownloadStudyAction to pull the changes.
	 *
	 * @param repo the RepoInfo to check for pending remote commits
	 */
	private static void checkRepoOutofDateStatus(RepoInfo repo) {
		SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
			@Override
			protected List<String> doInBackground() throws Exception {
				// Run the commit-behind query on the background thread to avoid blocking the EDT
				ShowChangesActions action = new ShowChangesActions(ActionPanelPlugin.getInstance().getActionsWindow(), repo, ShowChangesActions.ChangeType.Commits);
				List<String> changes = action.getChanges();
				return changes;
			}

			@Override
			protected void done() {
				try {
					List<String> changes = get();

					if (changes.size() > 0) {
						// Prompt the user to download the pending changes
						int opt = JOptionPane.showConfirmDialog(ActionPanelPlugin.getInstance().getActionsWindow(),
								"<html>There are " + changes.size() + " changes that have been made to the Repository since you last updated." +
										"<br>Do you want to update now?", "Changes Available", JOptionPane.YES_NO_OPTION);

						if (opt == JOptionPane.YES_OPTION) {
							// Trigger the download action if the user confirms
							DownloadStudyAction action = new DownloadStudyAction(ActionPanelPlugin.getInstance().getActionsWindow(), repo);
							action.actionPerformed(null);
						}
					}
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		worker.execute();
	}

	/**
	 * Retrieves the remote origin URL for a Git repository by running
	 * "git remote get-url origin" as a subprocess in the given directory.
	 *
	 * Reads stdout via ProcessOutputReader, waits 500 ms for draining, and parses
	 * the first stdout line starting with "http:" as the URL. Shows an error dialog
	 * via AbstractGitAction.showErrorMsg() if the subprocess fails. Returns null on
	 * any failure including launch error, non-zero exit, or no matching output line.
	 *
	 * @param parentForError the Window to use as the parent for any error dialogs
	 * @param dir            the local repository directory to run the command in
	 * @return the HTTP remote origin URL string, or null if it cannot be determined
	 */
	public static String getRepoUrl(Window parentForError, String dir) {
		boolean echoOutput = Boolean.getBoolean(AbstractGitAction.DEBUG_OUTPUT_PROP);

		// Build the "git remote get-url origin" command
		List<String> cmd = new ArrayList<>();
		cmd.add(getGitCmd());
		cmd.add("remote");
		cmd.add("get-url");
		cmd.add("origin");

		ProcessBuilder builder = new ProcessBuilder(cmd);

		// Run the command with its working directory set to the repo folder
		builder.directory(new File(dir));
		Process proc = null;

		try {
			proc = builder.start();
		} catch (IOException ioe) {
			// Log the launch failure and return null
			FluentLogger.forEnclosingClass().atWarning().log("Failed to launch %s, Error %s", cmd, ioe.getMessage());
			return null;
		}

		// Set up concurrent readers for stdout and stderr
		InputStream iStream = proc.getInputStream();
		InputStream eStream = proc.getErrorStream();
		BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream));
		BufferedReader eReader = new BufferedReader(new InputStreamReader(iStream));

		Vector<ProcessOutputLine> output = new Vector<>();
		ProcessOutputReader outputReader = new ProcessOutputReader(iReader, output, "git stdout", echoOutput, false);
		ProcessOutputReader errorReader = new ProcessOutputReader(eReader, output, "git stderr", echoOutput, true);

		try {
			// Block until the process exits
			int rv = proc.waitFor();

			if (echoOutput) {
				System.out.println(cmd.get(0) + " exit code=" + rv);
			}

			if (rv != 0) {
				// Show an error dialog if the command failed
				String msg = AbstractGitAction.getErrorMessage("Error finding git repo url", cmd, output);
				AbstractGitAction.showErrorMsg(parentForError, cmd.toString(), msg);
				return null;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			// Brief pause to allow the output reader threads to finish draining
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			// Close both output reader threads
			if (outputReader != null) {
				outputReader.close();
			}
			if (errorReader != null) {
				errorReader.close();
			}
		}

		// Parse the first HTTP URL from the stdout output
		return parseOutput(output);
	}

	/**
	 * Scans the collected output lines for the first stdout line starting with "http:"
	 * and returns it as the remote URL string.
	 *
	 * @param output the collected process output lines from the git remote get-url command
	 * @return the first HTTP URL found in stdout, or null if none is present
	 */
	private static String parseOutput(Vector<ProcessOutputLine> output) {
		ProcessOutputLine line;
		for (int i = 0; i < output.size(); i++) {
			line = output.get(i);

			// Only inspect stdout lines; skip stderr lines
			if (line.isStdout()) {
				if (line.getLine().startsWith("http:")) {
					return line.getLine();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the path to the git executable to use for subprocess invocations.
	 *
	 * Checks the "Git.Cmd.Path" system property for an explicit override; if not set,
	 * returns "git" to rely on the system PATH.
	 *
	 * @return the git executable path or name
	 */
	private static String getGitCmd() {
		String gitProp = System.getProperty("Git.Cmd.Path");
		if (gitProp != null && !gitProp.isEmpty()) {
			return gitProp;
		}
		return "git";
	}
}
