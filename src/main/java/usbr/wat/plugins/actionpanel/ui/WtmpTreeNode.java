package usbr.wat.plugins.actionpanel.ui;

import javax.swing.SwingUtilities;                                  // Provides SwingUtilities for scheduling tree-building work on the AWT Event Dispatch Thread
import javax.swing.tree.MutableTreeNode;                            // Provides the MutableTreeNode interface for nodes that support parent/child manipulation

import com.rma.model.ManagerProxy;                                  // Provides ManagerProxy as the handle passed to manager lifecycle listener callbacks
import com.rma.model.Project;                                       // Provides Project for accessing the current project and registering manager listeners
import com.rma.ui.ProjectTree;                                      // Provides ProjectTree as the base Swing tree type returned by getProjectTree
import com.rma.ui.ProjectTreeNode;                                  // Provides ProjectTreeNode as the base class for project-aware tree nodes

import usbr.wat.plugins.actionpanel.SimGroupContainerNode;          // Provides SimGroupContainerNode, the single WTMP child node added under this root node

/**
 * The root project tree node for the WTMP action panel navigation tree.
 *
 * {@code WtmpTreeNode} extends {@link ProjectTreeNode} to provide a WTMP-specific
 * implementation of the project node that appears as the invisible root of the
 * {@link WtmpTree}. Its primary responsibilities are:
 *
 *   Building the tree structure on the AWT Event Dispatch Thread by adding a single
 *       {@link SimGroupContainerNode} as its only child.
 *   Registering itself as a manager listener on the current project so it can respond
 *       to manager addition and deletion events.
 *   Returning the owning {@link WtmpTree} when the base class requests the project
 *       tree reference.
 *
 *
 * The {@code @SuppressWarnings("serial")} annotation suppresses the compiler warning
 * about the missing {@code serialVersionUID} field; this class is not intended for
 * Java object serialization.
 *
 * @see WtmpTree
 * @see SimGroupContainerNode
 * @see ProjectTreeNode
 */
@SuppressWarnings("serial")
public class WtmpTreeNode extends ProjectTreeNode {
	// Reference to the owning WtmpTree; used when constructing child nodes and
	// when the base class requests the project tree via getProjectTree()
	private WtmpTree _tree;

	/**
	 * Constructs a {@code WtmpTreeNode} for the given project, parent node, and
	 * owning tree.
	 *
	 * Delegates project and parent storage to the {@link ProjectTreeNode} superclass,
	 * then retains a reference to the owning {@link WtmpTree} for use during tree
	 * construction and child node creation.
	 *
	 * @param currentProject the {@link Project} this node represents; may be
	 *                       {@code null} if no project is currently open, in which
	 *                       case {@link #buildTree()} will return without adding children
	 * @param parent         the parent {@link MutableTreeNode} of this node, or
	 *                       {@code null} if this node is the root
	 * @param tree           the {@link WtmpTree} that owns this node; must not be
	 *                       {@code null}
	 */
	public WtmpTreeNode(Project currentProject, MutableTreeNode parent, WtmpTree tree) {
		// Delegate project and parent node storage to the superclass
		super(currentProject, parent);

		// Retain the owning tree reference for child node construction and tree access
		_tree = tree;
	}

	/**
	 * Builds the WTMP navigation tree structure by removing any existing children and
	 * adding a single {@link SimGroupContainerNode} under this root node.
	 *
	 * This method is scheduled on the AWT Event Dispatch Thread via
	 * {@link SwingUtilities#invokeLater} to ensure all Swing tree model mutations occur
	 * on the correct thread. If no project is currently open, the method returns
	 * immediately without modifying the tree.
	 *
	 * After adding the container node, this node registers itself as a manager listener
	 * on the current project so it receives {@link #managerAdded} and
	 * {@link #managerDeleted} callbacks when managers are created or removed.
	 */
	@Override
	public void buildTree() {
		// Schedule all tree-structure mutations on the Event Dispatch Thread
		SwingUtilities.invokeLater(() ->
		{
			// Clear any previously built child nodes before rebuilding
			removeAllChildren();

			// Guard against being called when no project is open
			if (getProject() == null) {
				return;
			}

			// Add the single WTMP simulation group container as the only child of this root node
			SimGroupContainerNode containerNode = new SimGroupContainerNode(_tree);
			add(containerNode);

			// Register this node as a manager listener so it is notified when
			// managers are added to or removed from the project
			getProject().addManagerListener(this);
		});
	}

	/**
	 * Returns the {@link WtmpTree} that owns this node.
	 *
	 * Overrides the base-class method to return the WTMP-specific tree reference
	 * stored during construction, allowing the superclass to interact with the
	 * correct tree instance when needed.
	 *
	 * @return the owning {@link WtmpTree}; never {@code null} if the constructor
	 * was called with a valid tree reference
	 */
	@Override
	protected ProjectTree getProjectTree() {
		return _tree;
	}

	/**
	 * Called when a manager is added to the current project.
	 *
	 * Currently a no-op beyond the null guard; intended as an extension point for
	 * future handling of manager addition events (e.g., adding a corresponding child
	 * node to the tree).
	 *
	 * @param proxy the {@link ManagerProxy} representing the newly added manager;
	 *              the method returns immediately if {@code proxy} is {@code null}
	 */
	@Override
	public void managerAdded(ManagerProxy proxy) {
		// Ignore spurious callbacks with a null proxy
		if (proxy == null) {
			return;
		}

		// Intentionally empty: manager addition handling to be implemented as needed
	}

	/**
	 * Called when a manager is deleted from the current project.
	 *
	 * Currently a no-op beyond the null guard; intended as an extension point for
	 * future handling of manager deletion events (e.g., removing the corresponding
	 * child node from the tree).
	 *
	 * @param proxy the {@link ManagerProxy} representing the deleted manager;
	 *              the method returns immediately if {@code proxy} is {@code null}
	 */
	@Override
	public void managerDeleted(ManagerProxy proxy) {
		// Ignore spurious callbacks with a null proxy
		if (proxy == null) {
			return;
		}

		// Intentionally empty: manager deletion handling to be implemented as needed
	}
}
