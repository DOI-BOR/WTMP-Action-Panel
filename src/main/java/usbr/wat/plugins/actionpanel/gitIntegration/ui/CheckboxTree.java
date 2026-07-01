package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.Point;             // Represents a 2D screen point for tooltip hit-testing
import java.awt.event.MouseEvent;  // Mouse event used to determine which tree node is under the cursor
import java.util.ArrayList;       // Resizable-array List for accumulating checked submodule names
import java.util.Enumeration;     // Enumeration for iterating child TreeNode collections
import java.util.List;            // Ordered collection interface for checked submodule name lists

import javax.swing.JMenuItem;     // Standard Swing menu item for the Select All / Unselect All popup options
import javax.swing.JPopupMenu;    // Right-click context menu providing batch select/unselect actions
import javax.swing.JTree;         // Swing tree component that this class extends
import javax.swing.SwingUtilities; // Utility for locating the owning window of a component
import javax.swing.tree.DefaultMutableTreeNode; // Mutable tree node used as the root and for searching
import javax.swing.tree.DefaultTreeModel;       // Default tree model that notifies listeners of node changes
import javax.swing.tree.TreeModel;              // Interface for the tree's data model
import javax.swing.tree.TreeNode;               // Base tree node interface used during tree expansion
import javax.swing.tree.TreePath;               // Represents a path from the root to a node in the tree

import com.rma.swing.tree.DefaultCheckBoxNode;  // RMA checkbox tree node supporting selected/unselected state

import rma.swing.tree.CheckBoxTreeRenderer;     // RMA cell renderer that draws checkbox-style tree nodes
import rma.swing.tree.NodeSelectionListener;    // RMA mouse listener that toggles checkbox state on click
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog; // Parent dialog providing the currently selected repo
import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;    // Provides the STUDY_MODULE constant for the main module name
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ListSubModulesAction; // Git action used to query the repository's submodule list
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;               // Data model holding the local path of the repository

/**
 * Checkbox-enabled JTree for selecting which submodules (and/or the main study
 * module) to include in a Git download, restore, or upload operation.
 *
 * Populated by fillSubModules(), which queries the repository's submodule list
 * via ListSubModulesAction and adds a node for the main "Study" module (always
 * first) followed by one node per submodule. All nodes are initially checked.
 *
 * Each node is an inner SubModuleNode (extending DefaultCheckBoxNode) that carries
 * a commits-behind count. When commits-behind > 0 and _disallowSelectionForNodesBehind
 * is true (used by EnterCommentsDlg), the node is automatically unchecked and disabled
 * to prevent uploading a submodule that is behind the remote. The count is reflected
 * visually in the node label as a red "(N↓)" suffix.
 *
 * A right-click popup menu provides "Select All" and "Unselect All" batch actions.
 * The tree tooltip shows the commits-behind count or "is up to date" for each node.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class CheckboxTree extends JTree {
	// The root node of the tree model; children are SubModuleNode instances
	private DefaultMutableTreeNode _root;

	// The repository whose submodules this tree displays
	private RepoInfo _repo;

	// True after fillSubModules() completes and at least one submodule was found
	private boolean _hasSubModules;

	// When true, nodes with commits-behind > 0 cannot be selected (upload mode)
	private boolean _disallowSelectionForNodesBehind;

	/**
	 * Constructs a CheckboxTree using the currently selected repo from the given dialog.
	 *
	 * Convenience constructor that delegates to the primary constructor and adds the popup menu.
	 *
	 * @param model              the initial tree model (typically an empty DefaultTreeModel)
	 * @param studyStorageDialog the dialog providing the currently selected repository
	 */
	public CheckboxTree(TreeModel model, StudyStorageDialog studyStorageDialog) {
		this(model, studyStorageDialog.getSelectedRepo());
		addPopupMenu();
	}

	/**
	 * Constructs a CheckboxTree for the given tree model and repository.
	 *
	 * Hides the root node, applies the CheckBoxTreeRenderer, registers the
	 * NodeSelectionListener for click-based checkbox toggling, enables tooltips,
	 * and adds the right-click popup menu.
	 *
	 * @param model the initial tree model
	 * @param repo  the RepoInfo describing the repository whose submodules will be shown
	 */
	public CheckboxTree(TreeModel model, RepoInfo repo) {
		super(model);

		// Store the root node for direct access during selection and fill operations
		_root = (DefaultMutableTreeNode) model.getRoot();
		_repo = repo;

		// Hide the root node; only its children (module nodes) should be visible
		setRootVisible(false);

		// Apply the checkbox cell renderer so nodes display with a checkbox control
		setCellRenderer(new CheckBoxTreeRenderer());

		// Register the click listener that toggles checkbox state on mouse click
		addMouseListener(new NodeSelectionListener(this));

		// Enable tooltip support so getToolTipText() is called on hover
		setToolTipText("");

		// Add the right-click select/unselect popup menu
		addPopupMenu();
	}

	/**
	 * Adds a right-click context menu with "Select All" and "Unselect All" items.
	 *
	 * Both items operate on all nodes in the tree regardless of current state.
	 */
	private void addPopupMenu() {
		JPopupMenu popup = new JPopupMenu();

		// "Select All" checks every node in the tree
		JMenuItem selectAllMenu = new JMenuItem("Select All");
		selectAllMenu.addActionListener(e -> setAllNodesSelected(true));
		popup.add(selectAllMenu);

		// "Unselect All" unchecks every node in the tree
		JMenuItem unSelectAllMenu = new JMenuItem("Unselect All");
		unSelectAllMenu.addActionListener(e -> setAllNodesSelected(false));
		popup.add(unSelectAllMenu);

		setComponentPopupMenu(popup);
	}

	/**
	 * Returns a tooltip for the submodule node under the cursor.
	 *
	 * Shows "[submodule name] is [N] commit[s] behind" when commits are behind,
	 * or "[submodule name] is up to date" when the node is current.
	 * Returns null when the cursor is not over any tree node.
	 *
	 * @param e the MouseEvent providing the cursor position for hit-testing
	 * @return the tooltip string, or null if no node is under the cursor
	 */
	@Override
	public String getToolTipText(MouseEvent e) {
		Point pt = e.getPoint();
		TreePath path = getPathForLocation(pt.x, pt.y);

		if (path == null) {
			return null;
		}

		SubModuleNode node = (SubModuleNode) path.getLastPathComponent();
		int commitsBehind = node.getCommitsBehind();

		if (commitsBehind > 0) {
			// Report the number of commits this submodule is behind the remote
			return node.getSubModuleName().concat(" is ").concat(String.valueOf(commitsBehind)).concat(" commit behind");
		}

		// Submodule is current
		return node.getSubModuleName().concat(" is up to date");
	}

	/**
	 * Returns the names of all submodule nodes (including the Study module) whose
	 * checkbox is currently checked.
	 *
	 * @return a List of checked submodule name strings; empty if none are checked
	 */
	public List<String> getCheckedSubmodules() {
		List<String> l = new ArrayList<>();
		getCheckedSubModules(l, _root);
		return l;
	}

	/**
	 * Recursively traverses the tree from the given parent, collecting the names of
	 * all checked DefaultCheckBoxNode children whose user object is a String.
	 *
	 * @param l      the list to which checked submodule names are added
	 * @param parent the parent node whose children should be inspected
	 */
	private void getCheckedSubModules(List<String> l, DefaultMutableTreeNode parent) {
		int cnt = parent.getChildCount();
		DefaultCheckBoxNode child;
		Object obj;

		for (int i = 0; i < cnt; i++) {
			child = (DefaultCheckBoxNode) parent.getChildAt(i);

			if (child.isSelected()) {
				obj = child.getUserObject();
				if (obj instanceof String) {
					// Add the submodule name to the result list
					l.add((String) obj);
				}
			}

			// Recurse into non-leaf nodes to include nested submodules
			if (!child.isLeaf()) {
				getCheckedSubModules(l, child);
			}
		}
	}

	/**
	 * Sets the selected (checked) state of all nodes in the tree.
	 *
	 * @param selected true to check all nodes; false to uncheck all nodes
	 */
	public void setAllNodesSelected(boolean selected) {
		setAllNodesSelected(selected, _root);
	}

	/**
	 * Recursively sets the selected state of all DefaultCheckBoxNode children
	 * under the given parent node.
	 *
	 * @param selected true to check; false to uncheck
	 * @param parent   the parent node whose children should be updated
	 */
	private void setAllNodesSelected(boolean selected, DefaultMutableTreeNode parent) {
		int cnt = parent.getChildCount();
		DefaultCheckBoxNode child;

		for (int i = 0; i < cnt; i++) {
			child = (DefaultCheckBoxNode) parent.getChildAt(i);

			// Apply the desired selection state to this node
			child.setSelected(selected);

			// Recurse into non-leaf nodes to update nested submodules
			if (!child.isLeaf()) {
				setAllNodesSelected(selected, child);
			}
		}
	}

	/**
	 * Populates the tree with the main Study module and all submodules from the repository.
	 *
	 * Queries the submodule list from the Git tool via ListSubModulesAction. Always
	 * adds a "Study" node as the first entry (the main module). Sets _hasSubModules to
	 * true only if actual submodules (beyond the main Study) were returned. All nodes
	 * are initially checked. The tree model is rebuilt and expanded after population.
	 * If a deferred selection (_selection) was set before the tree was filled, it is
	 * applied immediately after the tree is built.
	 */
	public void fillSubModules() {
		// Query the repository's submodule list via the Git tool
		ListSubModulesAction action = new ListSubModulesAction(SwingUtilities.windowForComponent(this), _repo);
		List<String> subModules = action.getSubModules();

		RepoInfo repo = _repo;
		if (repo != null) {
			// Always add the main Study module as the first entry, pre-checked
			DefaultCheckBoxNode parent = new SubModuleNode(AbstractGitAction.STUDY_MODULE, this);
			parent.setSelected(true);
			_root.add(parent);

			// Record whether any actual submodules were found
			_hasSubModules = !subModules.isEmpty();

			// Add a pre-checked node for each submodule
			for (int i = 0; i < subModules.size(); i++) {
				DefaultCheckBoxNode node = new SubModuleNode(subModules.get(i), this);
				node.setSelected(true);
				_root.add(node);
			}

			// Rebuild the tree model and notify the UI of the structural change
			DefaultTreeModel treeModel = (DefaultTreeModel) getModel();
			treeModel.nodeStructureChanged(_root);

			// Expand all nodes for visibility
			expandAll(true);
		}
	}

	/**
	 * Expands or collapses all nodes in the tree starting from the root.
	 *
	 * @param expand true to expand all nodes; false to collapse all nodes
	 */
	public void expandAll(boolean expand) {
		TreeNode root = (TreeNode) this.getModel().getRoot();
		expandAll(new TreePath(root), expand);
	}

	/**
	 * Recursively expands or collapses all nodes beneath the given tree path.
	 *
	 * Expansion must be performed bottom-up (children before parents) to ensure
	 * that parent nodes remain expanded when their children are expanded.
	 *
	 * @param parent the TreePath from which to start expanding or collapsing
	 * @param expand true to expand; false to collapse
	 */
	public void expandAll(TreePath parent, boolean expand) {
		// Recursively process all children first (bottom-up requirement)
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements(); ) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(path, expand);
			}
		}

		// Expand or collapse this node after all its descendants have been processed
		if (expand) {
			this.expandPath(parent);
		} else {
			this.collapsePath(parent);
		}
	}

	/**
	 * Returns whether any submodules (beyond the main Study module) were found when
	 * fillSubModules() was last called.
	 *
	 * Used by DownloadConfirmDialog and EnterCommentsDlg to determine whether the
	 * tree scroll pane should be shown.
	 *
	 * @return true if the repository has at least one submodule; false if it is a flat repo
	 */
	public boolean hasSubModules() {
		return _hasSubModules;
	}

	/**
	 * Updates the commits-behind count for the named submodule node.
	 *
	 * Locates the node by name and delegates to SubModuleNode.setCommitsBehind(),
	 * which may automatically uncheck and disable the node if
	 * _disallowSelectionForNodesBehind is true.
	 *
	 * @param submodule     the submodule name to update
	 * @param commitsBehind the number of commits this submodule is behind the remote
	 */
	public void setCommitsBehind(String submodule, int commitsBehind) {
		SubModuleNode node = findNode(submodule);
		if (node != null) {
			node.setCommitsBehind(commitsBehind);
		}
	}

	/**
	 * Sets whether nodes with commits behind the remote are prevented from being selected.
	 *
	 * When true (upload mode), any node whose commitsBehind > 0 is automatically
	 * unchecked and disabled. When false (download/restore mode), all nodes remain
	 * selectable regardless of their behind-count.
	 *
	 * @param disallow true to prevent selection of behind-nodes; false to allow it
	 */
	public void setDisallowSelectionForNodesBehind(boolean disallow) {
		_disallowSelectionForNodesBehind = disallow;
	}

	/**
	 * Searches the root's direct children for a SubModuleNode with the given submodule name.
	 *
	 * @param submodule the submodule name to search for
	 * @return the matching SubModuleNode, or null if not found
	 */
	private SubModuleNode findNode(String submodule) {
		int count = _root.getChildCount();
		for (int i = 0; i < count; i++) {
			SubModuleNode node = (SubModuleNode) _root.getChildAt(i);
			if (submodule.equals(node.getSubModuleName())) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Inner class representing a single submodule (or main Study) entry in the CheckboxTree.
	 *
	 * Extends DefaultCheckBoxNode to add a commits-behind count. When the count is
	 * greater than zero and _disallowSelectionForNodesBehind is true, the node
	 * cannot be selected and is visually disabled. The node label shows the count
	 * as a red "(N↓)" HTML suffix to alert the user that the submodule is behind.
	 */
	class SubModuleNode extends DefaultCheckBoxNode {
		// The number of remote commits that this submodule is behind
		private int _commitsBehind;

		/**
		 * Constructs a SubModuleNode with the given submodule name and owning tree.
		 *
		 * @param subModule the name of the submodule (or AbstractGitAction.STUDY_MODULE for the main module)
		 * @param tree      the JTree that owns this node (used by DefaultCheckBoxNode for repainting)
		 */
		SubModuleNode(String subModule, JTree tree) {
			super(subModule, tree);
		}

		/**
		 * Overrides setSelected to block selection when the node is behind the remote
		 * and _disallowSelectionForNodesBehind is true.
		 *
		 * @param selected true to check the node; false to uncheck it
		 */
		@Override
		public void setSelected(boolean selected) {
			// Block selection if this node is behind the remote and selection is disallowed
			if (_disallowSelectionForNodesBehind && _commitsBehind > 0 && selected) {
				return;
			}
			super.setSelected(selected);
		}

		/**
		 * Returns the number of remote commits that this submodule is behind.
		 *
		 * @return the commits-behind count (0 means up to date)
		 */
		public int getCommitsBehind() {
			return _commitsBehind;
		}

		/**
		 * Sets the commits-behind count and updates the node's visual state.
		 *
		 * If the count is greater than zero and _disallowSelectionForNodesBehind is true,
		 * automatically unchecks and disables the node. Notifies the tree model so the
		 * label is repainted with the updated count.
		 *
		 * @param commitsBehind the number of commits this submodule is behind the remote
		 */
		public void setCommitsBehind(int commitsBehind) {
			if (_disallowSelectionForNodesBehind && commitsBehind > 0) {
				// Uncheck and disable to prevent uploading a submodule that is behind
				super.setSelected(false);
				setEnabled(false);
			}
			_commitsBehind = commitsBehind;

			// Notify the tree model to repaint this node's label with the updated count
			((DefaultTreeModel) getModel()).nodeChanged(this);
		}

		/**
		 * Returns the node label, appending a red HTML behind-count indicator when applicable.
		 *
		 * When commitsBehind > 0, the label is wrapped in HTML and suffixed with
		 * a red "(N↓)" string to visually indicate that the submodule is out of date.
		 *
		 * @return the display label string (HTML-formatted when behind)
		 */
		@Override
		public String toString() {
			if (_commitsBehind > 0) {
				// Wrap in HTML and add a red "(N↓)" suffix using the Unicode down-arrow character
				return "<html>" + super.toString().concat("<font color=\"red\"> (")
						.concat(String.valueOf(_commitsBehind))
						.concat("\u2193")
						.concat(")</font></html>");
			}
			return super.toString();
		}

		/**
		 * Returns the submodule name stored as this node's user object.
		 *
		 * @return the submodule name string
		 */
		public String getSubModuleName() {
			return (String) super.getUserObject();
		}
	}
}
