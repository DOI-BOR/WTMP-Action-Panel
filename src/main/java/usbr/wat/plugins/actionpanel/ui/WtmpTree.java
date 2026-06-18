package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;                             // Provides EventQueue for scheduling UI updates on the AWT Event Dispatch Thread
import javax.swing.tree.DefaultMutableTreeNode;         // Provides DefaultMutableTreeNode as the standard mutable tree node used for the root
import javax.swing.tree.MutableTreeNode;                // Provides the MutableTreeNode interface for nodes that can be added to or removed from a tree
import javax.swing.tree.TreeNode;                       // Provides the TreeNode interface, used as the type for the root node parameter in createModel

import com.rma.event.ProjectAdapter;                    // Provides ProjectAdapter as a no-op base implementation of the project listener interface
import com.rma.event.ProjectEvent;                      // Provides ProjectEvent carrying the newly opened Project when a project-open event fires
import com.rma.model.Project;                           // Provides Project for accessing the current project and registering static project listeners
import com.rma.ui.ContentTree;                          // Provides ContentTree as the parent content panel that hosts this project tree
import com.rma.ui.ProjectTree;                          // Provides ProjectTree as the base Swing tree class this component extends
import com.rma.ui.ProjectTreeModel;                     // Provides ProjectTreeModel as the Swing tree model that manages project node structure
import com.rma.ui.ProjectTreeNode;                      // Provides ProjectTreeNode as the typed tree node interface for project-aware nodes

/**
 * The WTMP-specific project navigation tree displayed in the WTMP action panel UI.
 *
 * {@code WtmpTree} extends {@link ProjectTree} to provide a customised Swing tree
 * component tailored to the Water Temperature Modeling Platform (WTMP). It:
 *
 *   Hides the invisible root node so only the WTMP content nodes are visible.
 *   Limits initial expansion to three rows via {@link #setExpansionRowCount(int)}.
 *   Automatically expands all nodes whenever a new project is opened, scheduling
 *       the expansion on the AWT Event Dispatch Thread to avoid threading issues.
 *   Overrides {@link #createRootNode()} to supply a {@link WtmpTreeNode} as the
 *       tree's root.
 *   Overrides {@link #createModel(TreeNode)} to supply a {@link ProjectTreeModel}
 *       that handles project-open events by replacing the current root child with a
 *       fresh {@link WtmpTreeNode} and notifying Swing of the structural change.
 *
 * The {@code @SuppressWarnings("serial")} annotation suppresses the compiler warning
 * about the missing {@code serialVersionUID} field; this class is not intended for
 * Java object serialization.
 *
 * @see ProjectTree
 * @see WtmpTreeNode
 * @see ProjectTreeModel
 */
@SuppressWarnings("serial")
public class WtmpTree extends ProjectTree {
	/**
	 * Constructs a {@code WtmpTree} and attaches it to the supplied content tree panel.
	 *
	 * During construction:
	 *
	 *   Delegates core initialisation (model creation, root node creation) to the
	 *       {@link ProjectTree} superclass.
	 *   Sets the initial expansion row count to {@code 3} so the tree is partially
	 *       expanded on first display.
	 *   Hides the invisible root node so the top-level WTMP nodes appear at the
	 *       outermost indentation level.
	 *   Registers a static {@link ProjectAdapter} that calls {@link #expandAll()}
	 *       on the Event Dispatch Thread whenever a project is opened.
	 *
	 *
	 * @param contentTree the {@link ContentTree} panel that hosts this navigation tree;
	 *                    passed to the superclass constructor
	 */
	public WtmpTree(ContentTree contentTree) {
		// Initialise the base ProjectTree, which creates the model and root node
		super(contentTree);

		// Show only the top three levels of the tree on initial display
		setExpansionRowCount(3);

		// Hide the synthetic invisible root so top-level WTMP nodes appear at the top
		setRootVisible(false);

		// Register a project listener to auto-expand the tree when a project is opened
		Project.addStaticProjectListener(new ProjectAdapter() {
			@Override
			public void projectOpened(ProjectEvent e) {
				// Schedule expandAll on the EDT to ensure the tree model is fully
				// updated before expansion is attempted
				EventQueue.invokeLater(() -> expandAll());
			}
		});
	}

	/**
	 * Creates and returns the root {@link ProjectTreeNode} for this tree.
	 *
	 * Overrides the base-class implementation to supply a {@link WtmpTreeNode} built
	 * from the currently open project. The root node is invisible in the rendered tree
	 * because {@link #setRootVisible(boolean)} is set to {@code false} in the constructor.
	 *
	 * @return a new {@link WtmpTreeNode} representing the current project root;
	 * never {@code null}
	 */
	@Override
	protected ProjectTreeNode createRootNode() {
		// Build a WtmpTreeNode for the current project; null parent since this is the root
		return new WtmpTreeNode(Project.getCurrentProject(), null, this);
	}

	/**
	 * Creates and returns the {@link ProjectTreeModel} that manages the tree's node
	 * structure and responds to project-open events.
	 *
	 * The returned model is an anonymous subclass of {@link ProjectTreeModel} that
	 * overrides two methods:
	 *
	 *   {@code createProjectTreeNode} — returns a fresh {@link WtmpTreeNode} for the
	 *       current project whenever a new project node needs to be instantiated.
	 *   {@code projectOpened} — replaces the existing project child of the invisible
	 *       root with a new {@link WtmpTreeNode} and fires the appropriate Swing tree
	 *       model event ({@code nodeStructureChanged} if a prior project was removed, or
	 *       {@code nodesWereInserted} if no prior project existed).
	 *
	 * The reference to the model is also stored in the inherited {@code _model} field
	 * so the base class can access it.
	 *
	 * @param root the {@link TreeNode} to use as the invisible root of the tree model;
	 *             expected to be the {@link ProjectTreeNode} returned by
	 *             {@link #createRootNode()}
	 * @return the configured {@link ProjectTreeModel}; never {@code null}
	 */
	@Override
	protected ProjectTreeModel createModel(TreeNode root) {
		ProjectTreeModel model = new ProjectTreeModel((ProjectTreeNode) root) {
			/**
			 * Creates a new {@link WtmpTreeNode} for the current project, ignoring the
			 * supplied {@code proj} and {@code node} parameters in favour of the live
			 * {@link Project#getCurrentProject()} reference.
			 *
			 * @param proj the project associated with the new node (not used directly)
			 * @param node the parent mutable tree node (not used directly)
			 * @return a new {@link WtmpTreeNode} for the current project
			 */
			@Override
			protected ProjectTreeNode createProjectTreeNode(Project proj, MutableTreeNode node) {
				// Always build the node from the live current project reference
				return new WtmpTreeNode(Project.getCurrentProject(), null, WtmpTree.this);
			}

			/**
			 * Handles a project-open event by replacing the current project child of the
			 * invisible root with a freshly built {@link WtmpTreeNode}, then notifying
			 * Swing of the structural change so the tree repaints correctly.
			 *
			 * If a prior project node was already present as the first child of the root,
			 * all children are removed and {@code nodeStructureChanged} is fired to trigger
			 * a full re-render. If no prior project child existed, {@code nodesWereInserted}
			 * is fired with the index of the newly added node.
			 *
			 * @param evt the {@link ProjectEvent} carrying the newly opened project
			 */
			@Override
			public void projectOpened(ProjectEvent evt) {
				// Build a fresh WTMP tree node for the newly opened project
				ProjectTreeNode newProject = createProjectTreeNode(evt.getProject(), null);

				// Obtain the invisible root node to manipulate its children
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();

				// Inspect the first child to determine whether a prior project node exists
				Object obj = root.getChildAt(0);

				boolean noProjectRemoved = false;

				if (obj instanceof ProjectTreeNode) {
					// Remove all existing project children before adding the new one
					root.removeAllChildren();
					noProjectRemoved = true;
				}

				// Add the new project node to the (now empty or never-populated) root
				root.add(newProject);

				if (noProjectRemoved) {
					// Fire a full structure-changed event so Swing rebuilds the visible tree
					nodeStructureChanged(root);
				} else {
					// Fire a targeted insertion event for just the newly added node
					int[] indices = new int[]{root.getIndex(newProject)};

					nodesWereInserted(root, indices);
				}
			}
		};

		// Store the model reference in the inherited field for base-class access
		_model = model;

		return model;
	}
}
