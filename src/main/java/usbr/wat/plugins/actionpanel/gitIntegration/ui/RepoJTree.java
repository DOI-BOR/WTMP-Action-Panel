package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.Cursor;            // Provides cursor types; used to show a wait cursor during the GitLab API call
import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;     // Flexible grid-based layout manager for arranging UI components
import java.awt.event.ActionEvent; // Represents an action event fired by the F5 refresh key binding
import java.awt.event.KeyEvent;    // Key code constants; used to bind F5 to the refresh action
import java.awt.event.MouseEvent;  // Mouse event used for tooltip text lookup in the tree
import java.util.ArrayList;       // Resizable-array List for managing RepoSelectionListeners
import java.util.List;            // Ordered collection interface for the GitLab project list and listener list
import java.util.StringTokenizer; // Tokenizer for splitting slash-delimited paths into individual components
import java.util.logging.Logger;  // JDK logger for recording debug information during tree fill and navigation

import javax.swing.AbstractAction; // Base class for the F5 refresh key-binding action
import javax.swing.Action;         // Swing Action interface for the refresh action binding
import javax.swing.ActionMap;      // Maps action names to Action implementations for key bindings
import javax.swing.InputMap;       // Maps KeyStroke instances to action names for the tree
import javax.swing.JComponent;     // Swing component constant used for the input map scope
import javax.swing.JLabel;         // Non-interactive label for the "Repo Location:" caption
import javax.swing.JOptionPane;    // Provides error dialogs when the GitLab API connection fails
import javax.swing.JScrollPane;    // Scroll container wrapping the tree for long project lists
import javax.swing.KeyStroke;      // Represents the F5 key stroke used to trigger tree refresh
import javax.swing.SwingUtilities; // Utility for locating the owning window for the token prompt
import javax.swing.tree.DefaultMutableTreeNode; // Mutable tree node used as the root and as parent during tree traversal
import javax.swing.tree.DefaultTreeModel;       // Default tree model that drives the JTree display
import javax.swing.tree.TreePath;              // Represents a path from the root to a node for selection operations
import javax.swing.tree.TreeSelectionModel;    // Defines selection modes for the tree

import org.gitlab4j.api.GitLabApi;             // GitLab4J API client for connecting to the GitLab server
import org.gitlab4j.api.GitLabApiException;    // Exception thrown when a GitLab API call fails
import org.gitlab4j.api.models.Project;        // GitLab4J model representing a single GitLab project

import rma.swing.EnabledJPanel;    // JPanel subclass that propagates enabled/disabled state to all children
import rma.swing.RmaInsets;        // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJTextField;    // RMA-extended text field used as a read-only path display label
import rma.swing.tree.RmaJTree;    // RMA-extended JTree with additional utility methods (e.g., expandAll)
import rma.util.RMAIO;             // RMA I/O utility for path and file name manipulation

import usbr.wat.plugins.actionpanel.gitIntegration.event.RepoSelectionEvent;    // Event carrying name, URL, and path for a selection change
import usbr.wat.plugins.actionpanel.gitIntegration.event.RepoSelectionListener; // Listener interface for repo selection change notifications
import usbr.wat.plugins.actionpanel.gitIntegration.model.GitToken;              // Utility for prompting and caching the GitLab authentication token

/**
 * Panel containing a hierarchical tree browser for selecting a GitLab repository
 * location within the WTMP Action Panel's Git integration.
 *
 * Connects to the configured GitLab server using the GitLab4J API and the user's
 * cached authentication token, retrieves all accessible projects, and builds a
 * tree that mirrors the GitLab group/subgroup/project hierarchy beneath the
 * configured root path (GIT_ROOT_PATH).
 *
 * Two selection modes are supported via the SelectionType enum:
 *   - Folder: the selected path points to a GitLab group/folder (for creating a
 *     new repository inside that group). Project nodes stop the path at their parent.
 *   - Project: the selected path includes the project node and its .git file name
 *     (for identifying an existing project's clone URL).
 *
 * The tree is populated lazily when fillRepoTree() is called (typically deferred to
 * the EDT after the containing dialog opens). Pressing F5 re-triggers the fill.
 *
 * A read-only path display field below the tree shows the full URL of the currently
 * selected node. The getRepoPath(false) / getRepoUrl() accessors return the relative
 * and full URL respectively.
 *
 * RepoSelectionListeners are notified of every selection change via
 * addRepoSelectionListener() / removeRepoSelectionListener().
 *
 * Sub-repositories (branches of the study identified by the SubRepos enum) are
 * filtered out of the tree; only the main study project nodes are shown.
 */
public class RepoJTree extends EnabledJPanel {
	/**
	 * Enum controlling which level of the GitLab hierarchy the selected path targets.
	 *
	 * Folder: selection resolves to the containing group (for new-repo creation).
	 * Project: selection resolves to the project node itself (for clone URL lookup).
	 */
	public enum SelectionType {
		Folder,
		Project
	}

	/**
	 * Enum listing the known sub-repository suffixes that should be excluded from the tree.
	 *
	 * WAT studies are composed of multiple sub-repos (hec5q, ras, reports, etc.).
	 * Only the main study project is shown in the browser.
	 */
	public enum SubRepos {
		hec5q("5q"),
		ras("ras"),
		reports("reports"),
		rss("rss"),
		scripts("scripts"),
		shared("shared"),
		cequal_w2("cequal-w2");

		// The file-name suffix used to identify this sub-repo in its HTTP clone URL
		private String _name;

		SubRepos(String name) {
			_name = name;
		}

		/**
		 * Returns the suffix string used to identify this sub-repository type.
		 *
		 * @return the sub-repo identifier string
		 */
		public String getName() {
			return _name;
		}
	}

	// Default GitLab server base URL; overridable via the GitUrl system property
	private static final String RMA_GIT_URL = "https://gitlab.rmanet.app";

	// Effective GitLab server base URL (may be overridden by the GitUrl system property)
	private static final String GIT_URL = System.getProperty("GitUrl", RMA_GIT_URL);

	// Default GitLab root namespace path under which study projects are browsed
	private static final String RMA_GIT_ROOT_PATH = "RMA/usbr-water-quality/wat-studies";

	// Effective GitLab root path (may be overridden by the GitRootPath system property)
	private static final String GIT_ROOT_PATH = System.getProperty("GitRootPath", RMA_GIT_ROOT_PATH);

	// Number of slash-delimited tokens in GIT_ROOT_PATH; used when skipping root parts during tree build
	private static int _rootPathParts;

	static {
		// Count the tokens in the root path at class-load time; used in addToTree() to skip them
		_rootPathParts = new StringTokenizer(RMA_GIT_ROOT_PATH, "/").countTokens();
	}

	// Logger for recording debug information during tree fill, project listing, and path navigation
	private Logger _logger = Logger.getLogger(RepoJTree.class.getName());

	// The embedded RmaJTree displaying the GitLab hierarchy; shows descriptions and clone URLs in tooltips
	private RmaJTree _repoLocationTree;

	// Read-only field showing the full URL path of the currently selected tree node
	private RmaJTextField _repoPathLabel;

	// Root node of the tree model; all group and project nodes are added beneath it
	private FolderNode _root;

	// Whether the tree selection targets a group folder or a specific project node
	private SelectionType _selectionType = SelectionType.Folder;

	// Guard flag set to true while fillRepoTree() is running to defer setSelectedPath() calls
	private boolean _fillingTree = true;

	// Deferred selection path to apply after fillRepoTree() completes (set via setSelectedPath() during fill)
	private String _selection;

	// True when fillRepoTree() could not retrieve projects and the tree shows an error placeholder
	private boolean _treeNotFilled;

	// List of registered RepoSelectionListener instances notified on tree selection changes
	private List<RepoSelectionListener> _selectionListeners = new ArrayList<>();


	/**
	 * Constructs a RepoJTree with the given selection type.
	 *
	 * Initializes with a GridBagLayout, stores the selection type, builds all
	 * UI controls, and attaches the tree selection and F5 keyboard listeners.
	 *
	 * @param type whether to select group folders or project nodes
	 */
	public RepoJTree(SelectionType type) {
		super(new GridBagLayout());
		_selectionType = type;
		buildControls();
		addListeners();
	}

	/**
	 * Builds and lays out the "Repo Location:" label, the scrollable tree, and
	 * the read-only path display field.
	 *
	 * The label overrides setEnabled() to remain enabled regardless of the parent
	 * panel's state. The path field overrides setEnabled() for the same reason and
	 * also overrides getToolTipText() to show the full URL as a tooltip.
	 * The tree overrides getToolTipText() to show each node's description and clone URL.
	 */
	private void buildControls() {
		// Create the "Repo Location:" label; setEnabled is no-op so it stays readable when disabled
		JLabel label = new JLabel("Repo Location:") {
			@Override
			public void setEnabled(boolean enabled) {
				// Do nothing; keep the label always enabled for readability
			}
		};

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Create the RmaJTree with a tooltip override showing each node's description and clone URL
		_repoLocationTree = new RmaJTree() {
			@Override
			public String getToolTipText(MouseEvent e) {
				StringBuilder toolTip = new StringBuilder();
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					Object comp = path.getLastPathComponent();
					if (comp instanceof FolderNode) {
						FolderNode fnode = (FolderNode) comp;

						// Add the node description if available
						String desc = fnode.getDescription();
						if (desc != null) {
							toolTip.append(desc);
						}

						// Append the clone URL for project nodes (leaf nodes with a gitFile)
						if (fnode.isProjectNode()) {
							if (toolTip.length() > 0) {
								toolTip.append("<br>");
							}
							toolTip.append(fnode.getGitFile());
						}
					}
				}

				if (toolTip.length() > 0) {
					// Wrap the tooltip in HTML tags for rich formatting
					toolTip.insert(0, "<html>");
					toolTip.append("<html>");
					return toolTip.toString();
				}
				return null;
			}
		};

		// Show a placeholder while the tree is being populated
		_repoLocationTree.setModel(new DefaultTreeModel(new FolderNode("Retrieving Git Repo Info...", "", null, "")));
		_repoLocationTree.setToolTipText(""); // enable tooltip support
		_repoLocationTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		label.setLabelFor(_repoLocationTree);

		// Position the tree to fill all available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JScrollPane(_repoLocationTree), gbc);

		// Create the read-only path display field; setEnabled is no-op and tooltip shows full text
		_repoPathLabel = new RmaJTextField() {
			@Override
			public void setEnabled(boolean enabled) {
				// Do nothing; keep the field always readable even when the panel is disabled
			}

			@Override
			public String getToolTipText(MouseEvent e) {
				// Show the full path as a tooltip for long URLs that may be truncated
				return getText();
			}
		};
		_repoPathLabel.setEditable(false);
		_repoPathLabel.setBorder(null);
		_repoPathLabel.setToolTipText(""); // enable tooltip support

		// Position the path label below the tree, spanning the full width
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_repoPathLabel, gbc);
	}

	/**
	 * Attaches a tree selection listener and an F5 key binding for refreshing the tree.
	 *
	 * The tree selection listener calls treePathSelected() whenever the selected path
	 * changes. The F5 key binding (bound to the WHEN_IN_FOCUSED_WINDOW scope) triggers
	 * fillRepoTree() to re-fetch the project list from GitLab.
	 */
	private void addListeners() {
		// Update the path label and fire selection listeners on tree selection changes
		_repoLocationTree.getSelectionModel().addTreeSelectionListener(e -> treePathSelected());

		// Bind F5 to a refresh action that re-populates the tree from GitLab
		InputMap inputMap = _repoLocationTree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = _repoLocationTree.getActionMap();

		Action refreshAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				fillRepoTree();
			}
		};

		// Map F5 to the "refreshTree" action in both the input and action maps
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refreshTree");
		actionMap.put("refreshTree", refreshAction);
	}

	/**
	 * Responds to tree selection changes by updating the path label and firing listeners.
	 *
	 * Reads the current selection path, converts it to a full URL using
	 * getRepoPathFromTreePath(), updates the read-only path label, then notifies
	 * all registered RepoSelectionListeners. The listener notification always fires
	 * in the finally block even if path conversion throws an exception.
	 */
	private void treePathSelected() {
		try {
			TreePath path = _repoLocationTree.getSelectionPath();
			if (path == null) {
				_repoPathLabel.setText("");
				return;
			}

			// Convert the tree path to a full URL and display it in the path label
			String repoPath = getRepoPathFromTreePath(path, true);
			_repoPathLabel.setText(repoPath);
		} finally {
			// Always notify listeners even if an exception occurred above
			fireSelectionListeners();
		}
	}

	/**
	 * Notifies all registered RepoSelectionListeners of the current selection state.
	 * <p>
	 * Iterates the listener list in reverse order (last-in, first-notified) to support
	 * safe removal during notification. Creates a single RepoSelectionEvent carrying
	 * the current name, URL, and path.
	 */
	private void fireSelectionListeners() {
		RepoSelectionEvent event = new RepoSelectionEvent(getRepoName(), getRepoUrl(), getRepoPath());

		// Iterate in reverse to support safe removal during notification
		for (int i = _selectionListeners.size() - 1; i >= 0; i--) {
			_selectionListeners.get(i).repoSelectionChanged(event);
		}
	}

	/**
	 * Returns the display name of the currently selected project node.
	 *
	 * Returns null if no node is selected or the selected node is a folder (not a project).
	 *
	 * @return the project node display name, or null for folder nodes or empty selection
	 */
	private String getRepoName() {
		TreePath path = _repoLocationTree.getSelectionPath();
		if (path == null) {
			return null;
		}

		FolderNode node = (FolderNode) path.getLastPathComponent();
		if (node.isProjectNode()) {
			// Return the trimmed display name of the project node
			return node.toString().trim();
		}

		// Folder nodes do not have a meaningful repo name
		return null;
	}

	/**
	 * Converts a JTree selection TreePath to a repository URL or relative path string.
	 *
	 * Starts by appending GIT_ROOT_PATH (and optionally the GIT_URL prefix for full URLs),
	 * then walks the path array from index 1 (skipping the root placeholder node).
	 * For Folder selection mode, stops before project nodes. For Project mode, includes
	 * the project node and appends the .git file name extracted from the gitFile URL.
	 * Strips a trailing "/" if present.
	 *
	 * @param path     the JTree selection TreePath to convert
	 * @param fullPath true to include the GIT_URL server prefix; false for a root-relative path
	 * @return the constructed URL or path string
	 */
	private String getRepoPathFromTreePath(TreePath path, boolean fullPath) {
		StringBuilder builder = new StringBuilder();

		if (fullPath) {
			// Prepend the full GitLab server URL
			builder.append(GIT_URL);
			builder.append("/");
		}

		// All paths start with the configured root path
		builder.append(GIT_ROOT_PATH);
		builder.append("/");

		Object[] pathArray = path.getPath();
		FolderNode node;

		for (int i = 1; i < pathArray.length; i++) // index 0 is the invisible root; skip it
		{
			node = (FolderNode) pathArray[i];

			if (node.isProjectNode() && _selectionType == SelectionType.Folder) {
				// In Folder mode, stop before adding the project node (select the containing group)
				break;
			}

			// Append the URL path segment for this tree node
			builder.append(node.getPathName());
			builder.append("/");

			if (node.isProjectNode() && _selectionType == SelectionType.Project) {
				// In Project mode, append the .git file name from the clone URL
				builder.append(RMAIO.getFileFromPath(node.getGitFile()));
			}
		}

		// Remove a trailing slash if present
		String retPath = builder.toString();
		if (retPath.endsWith("/")) {
			retPath = retPath.substring(0, retPath.length() - 1);
		}

		return retPath;
	}

	/**
	 * Retrieves all GitLab projects accessible with the current token and populates
	 * the tree with the subset whose namespace path starts with GIT_ROOT_PATH.
	 *
	 * Shows a wait cursor during the API call. On success, adds each qualifying project
	 * to the tree via addToTree(), sets the new tree model, expands all nodes, and
	 * applies any deferred selection (_selection). On failure, shows an error placeholder
	 * node and sets _treeNotFilled to true.
	 */
	public void fillRepoTree() {
		_fillingTree = true;
		try {
			// Fetch all accessible GitLab projects using the current auth token
			List<Project> projects = getRepoProjects();

			if (projects != null) {
				_logger.fine("fillRepoTree:projects = " + projects);

				Project gitPrj;

				// Re-initialize the root node before rebuilding the tree
				_root = new FolderNode("root", "", null, "");

				for (int i = 0; i < projects.size(); i++) {
					gitPrj = projects.get(i);

					// Skip projects that are not under the configured root path
					if (!gitPrj.getPathWithNamespace().startsWith(GIT_ROOT_PATH)) {
						continue;
					}

					// Add the project to the tree hierarchy
					addToTree(_root, gitPrj);
				}

				// Replace the tree model with the newly built hierarchy
				DefaultTreeModel newModel = new DefaultTreeModel(_root);
				_repoLocationTree.setModel(newModel);

				// Expand all nodes for full visibility
				_repoLocationTree.expandAll(true);
				_treeNotFilled = false;
			} else {
				// Projects could not be retrieved; show an error placeholder
				DefaultTreeModel newModel = new DefaultTreeModel(new FolderNode("Unable to Retrieve Repo Projects...", null, null, null));
				_repoLocationTree.setModel(newModel);
				_treeNotFilled = true;
			}
		} finally {
			_fillingTree = false;
		}

		// Apply any deferred selection that was set while the tree was being filled
		if (_selection != null) {
			setSelectedPath(_selection);
			_selection = null;
		}
	}

	/**
	 * Adds the given GitLab project to the tree hierarchy rooted at the given node.
	 *
	 * Parses the project's namespace full path (relative to GIT_ROOT_PATH) and the
	 * display name (relative to the root path) in parallel using two StringTokenizers.
	 * Navigates the existing tree structure, creating new FolderNode children where
	 * needed. At the leaf level, the project's HTTP clone URL is used as the gitFile;
	 * sub-repository projects (identified by isSubRepo()) are skipped so only the
	 * main study project node appears.
	 *
	 * @param root   the root node to add the project into
	 * @param gitPrj the GitLab Project to add
	 */
	private void addToTree(DefaultMutableTreeNode root, Project gitPrj) {
		String fullPath = gitPrj.getNamespace().getFullPath();

		// Compute the path relative to the configured root
		String relPath = fullPath.substring(GIT_ROOT_PATH.length());

		_logger.fine("addToTree:starting with path " + fullPath);
		_logger.fine("addToTree:starting with name " + gitPrj.getNameWithNamespace());

		// Use two parallel tokenizers: one for URL path segments, one for display name segments
		StringTokenizer pathTokenizer = new StringTokenizer(relPath, "/");
		StringTokenizer nameTokenizer = new StringTokenizer(gitPrj.getNameWithNamespace(), "/");

		// Skip the root path parts in the name tokenizer to align with the relative path
		for (int i = 0; i < _rootPathParts; i++) {
			nameTokenizer.nextToken();
		}

		String pathPart, namePart, gitFile;
		DefaultMutableTreeNode child, current = root;

		while (pathTokenizer.hasMoreTokens()) {
			pathPart = pathTokenizer.nextToken();
			namePart = nameTokenizer.nextToken();

			// Look for an existing child node with this path segment to avoid duplicates
			child = getChildNode(current, pathPart);

			if (child == null) {
				_logger.fine("addToTree:adding name=" + namePart + " path=" + pathPart);

				if (!pathTokenizer.hasMoreTokens()) {
					// This is the leaf (project) node: set the git clone URL
					gitFile = gitPrj.getHttpUrlToRepo();

					if (isSubRepo(gitFile)) {
						// Skip sub-repository projects; only the main study project is shown
						continue;
					}
				} else {
					// Intermediate group node: no git file needed
					gitFile = null;
				}

				// Create and attach the new node
				child = new FolderNode(namePart, pathPart, gitFile, gitPrj.getDescription());
				current.add(child);
				current = child;
			} else {
				// Existing node found; navigate into it without creating a duplicate
				_logger.fine("addToTree:found existing node. name=" + namePart + " path=" + pathPart);
				current = child;
			}
		}
	}


	/**
	 * Checks whether the given GitLab clone URL belongs to a known sub-repository.
	 *
	 * Extracts the file name (without extension) from the URL and compares it
	 * case-insensitively against each SubRepos enum constant's name.
	 *
	 * @param gitFile the HTTP clone URL of the GitLab project
	 * @return true if the URL matches a known sub-repository suffix; false otherwise
	 */
	private boolean isSubRepo(String gitFile) {
		// Extract just the file name without its .git extension
		String file = RMAIO.getFileNameNoExtension(gitFile);
		SubRepos[] subRepos = SubRepos.values();

		for (int i = 0; i < subRepos.length; i++) {
			if (subRepos[i].getName().equalsIgnoreCase(file)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Searches the direct children of the given node for a FolderNode with the given path name.
	 *
	 * @param current  the parent node whose children should be searched
	 * @param pathPart the URL path segment to match against each child's pathName
	 * @return the matching child node, or null if not found
	 */
	private static DefaultMutableTreeNode getChildNode(DefaultMutableTreeNode current, String pathPart) {
		int cnt = current.getChildCount();
		FolderNode child;

		for (int i = 0; i < cnt; i++) {
			child = (FolderNode) current.getChildAt(i);
			if (pathPart.equals(child.getPathName())) {
				return child;
			}
		}

		return null;
	}


	/**
	 * Retrieves all accessible GitLab projects using the current authentication token.
	 *
	 * Prompts for the token via GitToken if none is cached. Shows a wait cursor
	 * during the API call. Returns null if no token is available or the API call fails.
	 * Clears the cached token on authentication failure so the user is prompted again.
	 *
	 * @return the list of accessible GitLab Project objects, or null on failure
	 */
	private List<Project> getRepoProjects() {
		// Get the current authentication token; prompt if none is cached
		String token = getGitToken();
		if (token == null || token.trim().isEmpty()) {
			return null;
		}

		// Show the wait cursor while the API call is running
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		GitLabApi gitLabApi = new GitLabApi(GIT_URL, token);
		System.out.println("getRepoProjects:connected to " + gitLabApi.getGitLabServerUrl());

		try {
			try {
				// Fetch the list of all accessible projects from the GitLab server
				List<Project> projects = gitLabApi.getProjectApi().getProjects();
				return projects;
			} catch (GitLabApiException e) {
				// Log the error, show a dialog, and clear the token to force re-prompt next time
				System.out.println("getRepoProjects:exception getting list of projects " + e);
				JOptionPane.showMessageDialog(this, "Failed to connect to GitLab. Error "
						+ e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				GitToken.clearGitToken();
				e.printStackTrace();
			}
			return null;
		} finally {
			// Always close the API client and restore the default cursor
			gitLabApi.close();
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Registers a RepoSelectionListener to receive selection change notifications.
	 *
	 * @param listener the listener to register; ignored if null
	 */
	public void addRepoSelectionListener(RepoSelectionListener listener) {
		if (listener != null) {
			_selectionListeners.add(listener);
		}
	}

	/**
	 * Removes a previously registered RepoSelectionListener.
	 *
	 * @param listener the listener to remove
	 */
	public void removeRepoSelectionListener(RepoSelectionListener listener) {
		_selectionListeners.remove(listener);
	}


	/**
	 * Retrieves the current GitLab authentication token, prompting the user if none is cached.
	 * <p>
	 * Delegates to GitToken.getGitToken() using the owning window as the dialog parent.
	 *
	 * @return the authentication token string, or null if the user canceled
	 */
	private String getGitToken() {
		String token = GitToken.getGitToken(SwingUtilities.windowForComponent(this));
		return token;
	}

	/**
	 * Returns the root-relative path of the currently selected tree node.
	 *
	 * Returns null if the tree was not successfully filled or no node is selected.
	 *
	 * @return the root-relative path string, or null
	 */
	public String getRepoPath() {
		return getRepoPath(false);
	}

	/**
	 * Returns the full GitLab URL to the currently selected tree node.
	 *
	 * Returns null if the tree was not successfully filled or no node is selected.
	 *
	 * @return the full URL string including the server base, or null
	 */
	public String getRepoUrl() {
		return getRepoPath(true);
	}

	/**
	 * Returns the path or full URL of the currently selected tree node.
	 *
	 * Returns null if the tree was not successfully filled (_treeNotFilled) or no
	 * node is selected. Delegates path construction to getRepoPathFromTreePath().
	 *
	 * @param fullUrl true to return the full URL including the server base;
	 *                false to return only the root-relative path
	 * @return the path or full URL string, or null
	 */
	public String getRepoPath(boolean fullUrl) {
		if (_treeNotFilled) {
			return null;
		}

		TreePath path = _repoLocationTree.getSelectionPath();
		if (path == null) {
			return null;
		}

		return getRepoPathFromTreePath(path, fullUrl);
	}

	/**
	 * Inner class representing a single node in the repository location tree.
	 *
	 * Each node carries a URL path segment (pathName), an optional HTTP clone URL
	 * (gitFile; non-null only for project/leaf nodes), and an optional description.
	 * A node is considered a project node when gitFile is non-null.
	 */
	@SuppressWarnings("serial")
	class FolderNode extends DefaultMutableTreeNode {
		// The URL path segment for this node (e.g., "my-study" in the GitLab namespace path)
		private String _pathname;

		// True if this node represents a GitLab project (has a clone URL); false for group folders
		private boolean _projectNode;

		// The optional project description from GitLab (null for group nodes)
		private String _description;

		// The HTTP clone URL for this project node (null for group/folder nodes)
		private String _gitFile;

		/**
		 * Constructs a FolderNode with the given display name, path segment, clone URL, and description.
		 *
		 * @param displayName a human-readable name shown in the tree
		 * @param pathName    the URL path segment for this node
		 * @param gitFile     the HTTP clone URL for project nodes; null for group/folder nodes
		 * @param description the GitLab project description; null if not available
		 */
		FolderNode(String displayName, String pathName, String gitFile, String description) {
			super(displayName);
			_pathname = pathName;
			_projectNode = gitFile != null; // project nodes have a non-null gitFile
			_gitFile = gitFile;
			_description = description;
		}

		/**
		 * Returns true if this node represents a GitLab project (has a clone URL).
		 *
		 * @return true for project nodes; false for group/folder nodes
		 */
		public boolean isProjectNode() {
			return _projectNode;
		}

		/**
		 * Returns the URL path segment for this node.
		 *
		 * @return the path segment string used when constructing the full URL
		 */
		public String getPathName() {
			return _pathname;
		}

		/**
		 * Returns the optional GitLab project description for this node.
		 *
		 * @return the description string, or null if not available
		 */
		public String getDescription() {
			return _description;
		}

		/**
		 * Returns the HTTP clone URL for this project node.
		 *
		 * @return the clone URL string, or null for group/folder nodes
		 */
		public String getGitFile() {
			return _gitFile;
		}
	}

	/**
	 * Programmatically selects the tree node corresponding to the given URL or path.
	 *
	 * If the tree is currently being filled, stores the path for deferred application
	 * after fillRepoTree() completes. Strips the GIT_URL prefix and validates that the
	 * remaining path starts under GIT_ROOT_PATH. Navigates the tree node-by-node using
	 * case-insensitive token comparison and applies the selection.
	 *
	 * @param path the URL or root-relative path string to select; null to clear selection
	 */
	public void setSelectedPath(String path) {
		if (path == null) {
			// Null path: clear the current selection
			_repoLocationTree.clearSelection();
			return;
		}

		if (_fillingTree) {
			// Tree is currently being filled; defer selection until fill is complete
			_selection = path;
			return;
		}

		String pathLower = path.toLowerCase();

		// Strip the server URL prefix if present
		if (pathLower.startsWith(GIT_URL.toLowerCase())) {
			pathLower = pathLower.substring(GIT_URL.length());
		}

		// Validate that the remaining path is under the configured root
		if (!pathLower.startsWith("/" + GIT_ROOT_PATH.toLowerCase())) {
			_repoLocationTree.clearSelection();
			_logger.info("Invalid starting path for " + path + " tree starts with " + GIT_ROOT_PATH);
			_repoPathLabel.setText(pathLower);
			return;
		}

		// Strip the root path prefix to get the selection-specific suffix
		String shortenedPath = pathLower.substring(("/" + GIT_ROOT_PATH).length());

		// Navigate the tree token-by-token using case-insensitive matching
		FolderNode current = (FolderNode) _repoLocationTree.getModel().getRoot(), node;
		StringTokenizer tokenizer = new StringTokenizer(shortenedPath, "/");

		if (tokenizer.hasMoreTokens()) {
			// Skip the first token (it matches the last segment of GIT_ROOT_PATH)
			tokenizer.nextToken();
		}

		String pathPart;
		while (tokenizer.hasMoreTokens() && current != null) {
			pathPart = tokenizer.nextToken();
			node = findNode(current, pathPart);
			if (node != null) {
				current = node;
			}
		}

		// Apply the selection to the deepest matching node
		_repoLocationTree.setSelectionPath(new TreePath(current.getPath()));
	}

	/**
	 * Searches the direct children of the given FolderNode for a child whose path name
	 * matches the given segment (case-insensitive).
	 *
	 * @param startNode the node whose children should be searched
	 * @param pathPart  the URL path segment to match (case-insensitive)
	 * @return the matching FolderNode child, or null if not found
	 */
	private FolderNode findNode(FolderNode startNode, String pathPart) {
		int cnt = startNode.getChildCount();
		FolderNode child;

		for (int i = 0; i < cnt; i++) {
			child = (FolderNode) startNode.getChildAt(i);
			if (child.getPathName().equalsIgnoreCase(pathPart)) {
				return child;
			}
		}

		return null;
	}

	/**
	 * Clears the current tree selection and blanks the path display label.
	 *
	 * Called by ReposEditor.clearForm() when the user clicks New to start adding a repo.
	 */
	public void clearPerformed() {
		_repoLocationTree.clearSelection();
		_repoPathLabel.setText("");
	}

	/**
	 * Returns the GitLab server base URL used as the parent URL for new repository creation.
	 *
	 * @return the GIT_URL constant (e.g., "https://gitlab.rmanet.app")
	 */
	public String getParentUrl() {
		return GIT_URL;
	}
}
