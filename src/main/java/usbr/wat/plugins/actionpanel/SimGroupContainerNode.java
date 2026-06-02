package usbr.wat.plugins.actionpanel;

import java.awt.EventQueue;                                                 // Provides EventQueue for safely dispatching UI updates on the Event Dispatch Thread

import java.util.Objects;                                                   // Provides Objects utility class for null-safe operations such as requireNonNull

import javax.swing.Icon;                                                    // Provides the Icon interface used for tree node icons
import javax.swing.ImageIcon;                                               // Provides ImageIcon for loading and displaying image-based icons
import javax.swing.tree.MutableTreeNode;                                    // Provides MutableTreeNode interface for nodes that can be modified in a tree structure
import javax.swing.tree.TreeNode;                                           // Provides TreeNode interface representing a node in a tree data model

import com.rma.client.Browser;                                              // Provides Browser, the RMA application browser/shell
import com.rma.event.ProjectAdapter;                                        // Provides ProjectAdapter, a convenience adapter for ProjectManagerListener with empty default implementations
import com.rma.event.ProjectManagerListener;                                // Provides ProjectManagerListener for listening to manager add/remove events on a project
import com.rma.factories.ProjectNodeFactory;                                // Provides ProjectNodeFactory for creating UI tree nodes from manager proxies
import com.rma.model.ManagerProxy;                                          // Provides ManagerProxy, a proxy wrapper around a project manager object
import com.rma.model.Project;                                               // Provides Project, the top-level project model
import com.rma.ui.AbstractContainerNode;                                    // Provides AbstractContainerNode, the base class for container nodes in the project tree
import com.rma.ui.IconNode;                                                 // Provides IconNode interface for tree nodes that display a custom icon
import com.rma.ui.ProjectTree;                                              // Provides ProjectTree, the Swing tree component displaying the project hierarchy

import rma.swing.RmaImage;                                                  // Provides RmaImage for loading application image resources

import usbr.wat.plugins.actionpanel.model.SimulationGroup;                  // Provides SimulationGroup, the domain model for a group of simulations
import usbr.wat.plugins.actionpanel.ui.ActionsProjectTab;                   // Provides ActionsProjectTab, the UI tab that hosts the action panel project view
import usbr.wat.plugins.actionpanel.ui.SimulationGroupNode;                 // Provides SimulationGroupNode, the tree node UI representation of a SimulationGroup
import usbr.wat.plugins.actionpanel.ui.WtmpTreeNode;                        // Provides WtmpTreeNode, the base tree node type used throughout the WTMP plugin

/**
 * A container node displayed in the Study Tree that holds and manages
 * Simulation Group nodes. It listens for manager add/remove events on
 * the current project and keeps the tree in sync accordingly.
 *
 * This node also creates a special transitory "Not in a Group" child node
 * that displays all simulations not yet assigned to any group.
 *
 */
@SuppressWarnings("serial")
public class SimGroupContainerNode extends AbstractContainerNode
		implements IconNode {
	/** Icon representing the simulation group container folder in the project tree. */
	private static ImageIcon _folderIcon;

	// Static initializer block: loads the folder icon once when the class is first used
	static {
		_folderIcon = RmaImage.getImageIcon("Images/simGroupContainer.png");
	}

	/** The project tree this container node belongs to, used for notifying tree structure changes. */
	private final ProjectTree _tree;

	/** Listener that responds to project-level manager add/remove events. */
	private ProjectAdapter _projectListener;

	/**
	 * Constructs a SimGroupContainerNode and attaches it to the given project tree.
	 * Registers a manager listener on the current project and adds the default
	 * "Not in a Group" node as the first child.
	 *
	 * @param tree the ProjectTree this node will be displayed in; must not be null
	 * @throws NullPointerException if tree is null
	 */
	public SimGroupContainerNode(ProjectTree tree) {
		// Call parent constructor with the display label for this container
		super("Simulation Groups");

		// Store the tree reference, throwing if null to enforce the contract
		_tree = Objects.requireNonNull(tree, "Project tree must be specified");

		// Register a listener so new/removed SimulationGroup managers update the tree
		addManagerListener();

		// Add the special "Not in a Group" child node to represent ungrouped simulations
		addNoGroupNode();
	}

	/**
	 * Registers a ProjectManagerListener on the current project to respond
	 * when SimulationGroup managers are added or removed. When a manager is added,
	 * a corresponding tree node is created. When one is removed, its node is
	 * deleted from the tree and the tree is notified.
	 */
	private void addManagerListener() {
		Project.getCurrentProject().addManagerListener(new ProjectManagerListener() {

			/**
			 * Called when a new manager is added to the project.
			 * Delegates to addSimulationGroup to create a corresponding tree node.
			 *
			 * @param proxy the manager proxy representing the newly added manager
			 */
			@Override
			public void managerAdded(ManagerProxy proxy) {
				// Attempt to add a tree node for this newly registered manager
				addSimulationGroup(proxy);
			}

			/**
			 * Called when a manager is removed from the project.
			 * Finds the corresponding tree node, removes it from the container,
			 * and notifies the tree of the removal.
			 *
			 * @param proxy the manager proxy representing the removed manager
			 */
			@Override
			public void managerDeleted(ManagerProxy proxy) {
				// Look up the tree node currently representing this manager
				MutableTreeNode node = findNodeForManager(proxy);

				if ( node != null ) {
					// Get the index of the node within this container before removal
					int idx = getIndex(node);

					// Remove the node from this container at the found index
					remove(idx);

					// Notify the tree that a node was removed so it can refresh the UI
					_tree.nodesWereRemoved(SimGroupContainerNode.this, new TreeNode[] {node});
				}
			}

			/**
			 * Declares that this listener only cares about SimulationGroup managers.
			 *
			 * @return the SimulationGroup class, used to filter manager events
			 */
			@Override
			public Class getManagerClass() {
				return SimulationGroup.class;
			}

		});
	}

	/**
	 * Creates and adds a tree node for the given manager proxy if one does not
	 * already exist. If the node is a SimulationGroupNode, it is configured
	 * with the project tree reference. The tree is notified of the insertion
	 * on the Event Dispatch Thread.
	 *
	 * @param proxy the manager proxy for the SimulationGroup to add
	 * @return true if a new node was created and added; false if it already existed
	 */
	public boolean addSimulationGroup(ManagerProxy proxy) {
		// Check whether a node for this manager already exists in the tree
		MutableTreeNode node = findNodeForManager(proxy);

		if ( node == null ) {
			// Create the appropriate tree node for this manager proxy
			MutableTreeNode mgrNode = ProjectNodeFactory.getProjectNode(proxy, this);

			// Add the new node as a child of this container
			add(mgrNode);

			// If the new node is a SimulationGroupNode, configure it with the tree reference
			if(mgrNode instanceof SimulationGroupNode) {
				SimulationGroupNode sgNode = (SimulationGroupNode) mgrNode;

				// Associate the tree so the node can trigger tree structure notifications
				sgNode.setTree(_tree);
			}

			// Schedule a tree notification on the EDT; note: 'node' is null here so the
			// tree will handle the insertion based on the container's current state
			EventQueue.invokeLater(()->_tree.nodesWereInserted(this, new TreeNode[] {node}));

			return true;
		}

		// A node already exists for this proxy; no action needed
		return false;
	}

	/**
	 * Returns the project tree associated with this container node.
	 *
	 * @return the ProjectTree instance
	 */
	private ProjectTree getTree() {
		return _tree;
	}

	/**
	 * Adds a "Not in a Group" node to this container using the current project.
	 * Delegates to the overloaded form that accepts a Project argument.
	 */
	private void addNoGroupNode() {
		// Delegate to the project-specific overload using the active project
		addNoGroupNode(Project.getCurrentProject());
	}

	/**
	 * Creates and adds a special transitory SimulationGroup node labeled "Not in a Group"
	 * to the given project. This node represents all simulations that have not been
	 * assigned to any named group. It is marked as transitory and read-only so it
	 * does not trigger persistence or modification events.
	 *
	 * @param prj the Project to add the transitory group manager to
	 */
	private void addNoGroupNode(Project prj) {
		// Create an anonymous SimulationGroup that is always considered clean and readable
		SimulationGroup  simGroup = new SimulationGroup() {
			/**
			 * Always returns true since this group requires no data loading.
			 *
			 * @return true unconditionally
			 */
			@Override
			public boolean readData() {
				return true;
			}

			/**
			 * Returns false so that the "Not in a Group" node remains editable in context.
			 *
			 * @return false unconditionally
			 */
			@Override
			public boolean isReadOnly() {
				return false;
			}

			/**
			 * Returns false so this group never triggers unsaved-changes prompts.
			 *
			 * @return false unconditionally
			 */
			@Override
			public boolean isModified() {
				return false;
			}
		};

		// Set the display name shown in the project tree
		simGroup.setName("Not in a Group");

		// Mark as transitory so it is not persisted to the project file
		simGroup.setIsTransitory(true);

		// Suppress modification events so this group does not affect dirty state
		simGroup.setIgnoreModifiedEvents(true);

		// Register the transitory group with the project so it appears in the manager list
		prj.addManager(simGroup);

		// Create the corresponding tree node via the factory
		MutableTreeNode node = ProjectNodeFactory.getProjectNode(simGroup, this);

		if ( node instanceof SimulationGroupNode ) {
			SimulationGroupNode sgNode = (SimulationGroupNode) node;

			// Associate the tree so the node can fire structure change notifications
			sgNode.setTree(_tree);

			// Configure this node to automatically include simulations not in any other group
			sgNode.setAddSimsNotInGroup();

			// Show the simulation count as part of the node's label
			sgNode.setShowCount(true);

			// Provide the manager proxy so the node can interact with the project model
			sgNode.setManagerProxy(Project.getCurrentProject().getManagerProxy(simGroup));

			// Add the configured node as a child of this container
			add(node);
		}
	}

	/**
	 * Returns the fully qualified class name of the manager type this container handles.
	 * Used by the framework to associate managers with this container node.
	 *
	 * @return the class name of SimulationGroup
	 */
	@Override
	public String getManagerType() {
		return SimulationGroup.class.getName();
	}

	/**
	 * Adds a manager proxy to this container node.
	 * Delegates to the superclass implementation.
	 *
	 * @param proxy the manager proxy to add
	 */
	@Override
	public void addManager(ManagerProxy proxy) {
		super.addManager(proxy);
	}

	/**
	 * Returns the icon displayed for this container node in the project tree.
	 *
	 * @return the folder icon representing the simulation groups container
	 */
	@Override
	public Icon getIcon() {
		return _folderIcon;
	}

	/**
	 * Checks whether the given SimulationGroup already has a corresponding tree node,
	 * and if not, retrieves its manager proxy and adds it to the tree.
	 *
	 * @param simGrp the SimulationGroup to check and potentially add
	 * @return true if a new node was added; false if the group was null or already present
	 */
	public boolean checkAndAddSimGroup(SimulationGroup simGrp) {
		// Guard against null input; nothing to add
		if ( simGrp == null ) {
			return false;
		}

		// Look up the manager proxy for this simulation group in the current project
		ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(simGrp);

		if ( proxy != null ) {
			// Proxy exists; attempt to add a tree node for it
			return addSimulationGroup(proxy);
		}

		// No proxy found; group is not registered with the project
		return false;
	}
}