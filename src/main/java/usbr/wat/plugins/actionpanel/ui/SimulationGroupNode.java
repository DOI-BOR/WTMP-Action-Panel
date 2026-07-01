package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;           // Provides invokeLater for scheduling tree mutations on the EDT
import java.awt.event.ActionListener; // Marker interface implemented as part of ProjectPaneActionNode contract
import java.util.ArrayList;           // Resizable-array List used when building content node lists
import java.util.List;                // Generic ordered collection interface

import javax.swing.Icon;              // Icon interface returned by getIcon for the node's folder image
import javax.swing.ImageIcon;         // Concrete Icon implementation loaded from the classpath
import javax.swing.tree.MutableTreeNode; // Interface for tree nodes that can be structurally modified
import javax.swing.tree.TreeNode;        // Read-only tree node interface used for child iteration

import com.rma.client.Browser;              // Provides access to the RMA browser frame and project tree
import com.rma.editors.DataEditor;          // Editor interface returned by getEditor (no editor for this node)
import com.rma.event.ProjectManagerListener; // Listener interface for project manager add/delete events
import com.rma.factories.ProjectNodeFactory; // Creates the appropriate tree node for a given manager object
import com.rma.model.ManagerProxy;          // Lightweight proxy wrapping a managed model object
import com.rma.model.Project;               // Represents the currently open WAT study; provides manager lookups
import com.rma.ui.DefaultContentNode;       // Generic content-tree node with a label and optional icon
import com.rma.ui.HasContentNode;           // Interface marking nodes that supply child content-tree nodes
import com.rma.ui.IconNode;                 // Interface allowing a node to supply its own tree icon
import com.rma.ui.ManagerNode;              // RMA tree node base class that wraps a managed model object
import com.rma.ui.PopupMenuTreeNode;        // Interface marking nodes that support a right-click popup menu
import com.rma.ui.ProjectPaneActionNode;    // Interface for nodes that can be activated from the project pane
import com.rma.ui.ProjectTree;              // RMA project tree component; used to notify of structural changes

import hec.heclib.util.HecTime;      // HEC time object used to format analysis period start and end times
import hec.model.RunTimeWindow;      // Holds the start and end HecTime for an analysis period

import hec2.wat.client.WatMessages;          // Message key constants for internationalised WAT node labels
import hec2.wat.model.WatAnalysisPeriod;     // WAT analysis period model providing the run-time window
import hec2.wat.model.WatSimulation;         // WAT simulation model object displayed as child nodes
import hec2.wat.ui.WatAnalysisPeriodNode;    // Project tree node for a WatAnalysisPeriod
import hec2.wat.ui.WatSimulationNode;        // Project tree node for a WatSimulation
import hec2.wat.util.WatI18n;               // Provides internationalised message strings for WAT node labels

import rma.swing.RmaImage;                                         // Utility for loading image icons from the classpath
import usbr.wat.plugins.actionpanel.SimGroupContainerNode;         // Parent container node that tracks which simulation groups have been added
import usbr.wat.plugins.actionpanel.model.SimulationGroup;         // Simulation group model object wrapped by this node


/**
 * Project tree node that represents a SimulationGroup in the WAT project browser.
 *
 * This node extends ManagerNode and implements several RMA tree interfaces to
 * integrate fully with the WAT project tree and content pane:
 *
 *   HasContentNode    -- supplies a list of content-pane nodes including the analysis
 *                        period, a time-window summary, and the group's simulations.
 *   PopupMenuTreeNode -- enables a right-click context menu on this node.
 *   ActionListener    -- satisfies the ProjectPaneActionNode contract.
 *   ProjectPaneActionNode -- allows the node to be activated from the project pane.
 *   IconNode          -- provides a folder icon for display in the tree.
 *
 * Two construction modes are supported:
 *   Default "Simulation Group" label -- used when this node represents all simulations
 *   not yet assigned to any named group (the "catch-all" node).
 *   Object-initialized -- wraps a specific SimulationGroup manager object.
 *
 * Tree child management:
 *   When the system property "SimGroupNode.HasSimulations" is true, child
 *   WatSimulationNode objects are added to this tree node after the proxy is set.
 *   When setAddSimsNotInGroup is called, the node additionally shows all simulations
 *   not belonging to any other group, and registers a SimGroupProjectManagerListener
 *   to keep those children in sync as simulations are added or removed from the project.
 *
 */
@SuppressWarnings("serial")
public class SimulationGroupNode extends ManagerNode
		implements HasContentNode, PopupMenuTreeNode,
		ActionListener, ProjectPaneActionNode, IconNode {
	/**
	 * Shared folder icon displayed in the tree for all SimulationGroupNode instances.
	 * Loaded once from the classpath at class initialisation time.
	 */
	private static ImageIcon _folderIcon;

	static {
		// Load the folder icon once when the class is first referenced
		_folderIcon = RmaImage.getImageIcon("Images/compMulti16x16.gif");
	}

	/**
	 * When true, the toString method appends the current child count in parentheses
	 * to the node label (e.g. "My Group (3)").
	 */
	private boolean _showCnt;

	/**
	 * Reference to the owning ProjectTree; required for notifying the tree model
	 * of structural changes such as node insertions and removals.
	 */
	private ProjectTree _tree;


	/**
	 * Constructs a default node labelled "Simulation Group" with no wrapped manager.
	 *
	 * Used as the catch-all node that aggregates simulations not assigned to any
	 * named group. A project manager listener is registered immediately so that
	 * content-pane refreshes are triggered when a simulation is deleted while this
	 * node is selected.
	 */
	public SimulationGroupNode() {
		super("Simulation Group");
		addManagerListener();
	}


	/**
	 * Constructs a node wrapping the given manager object (expected to be a
	 * SimulationGroup or ManagerProxy).
	 *
	 * The node label is set to an empty string initially; the actual display name
	 * is provided by the wrapped manager via the superclass toString logic. A project
	 * manager listener is registered for content-pane refresh on simulation deletion.
	 *
	 * @param obj the manager object or ManagerProxy to wrap; must not be null
	 */
	public SimulationGroupNode(Object obj) {
		super("");
		setUserObject(obj);
		addManagerListener();
	}


	/**
	 * Registers a project manager listener that refreshes the content pane when a
	 * WatSimulation is deleted while this node is selected in the project tree.
	 *
	 * The listener responds only to deletion events for WatSimulation objects. On
	 * deletion it checks whether this node is currently selected and, if so, requests
	 * a content tree update so the pane reflects the reduced simulation list.
	 */
	private void addManagerListener() {
		Project.getCurrentProject().addManagerListener(new ProjectManagerListener() {
			/**
			 * Called when a new manager is added to the project.
			 * No action is required here; simulation additions are handled separately.
			 *
			 * @param proxy the proxy wrapping the newly added manager
			 */
			@Override
			public void managerAdded(ManagerProxy proxy) {
				// No action needed on addition for this listener
			}

			/**
			 * Called when a manager is deleted from the project. If this node is
			 * currently selected in the project tree, the content pane is refreshed
			 * so the deleted simulation is no longer shown.
			 *
			 * @param proxy the proxy wrapping the deleted manager
			 */
			@Override
			public void managerDeleted(ManagerProxy proxy) {
				if (Browser.getBrowserFrame().getProjectTree().getSelectedNode()
						== SimulationGroupNode.this) {
					// Refresh the content pane to reflect the simulation deletion
					Browser.getBrowserFrame().getProjectTree().updateContentTree();
				}
			}

			/**
			 * Restricts this listener to WatSimulation manager events only.
			 *
			 * @return WatSimulation.class
			 */
			@Override
			public Class getManagerClass() {
				return WatSimulation.class;
			}
		});
	}


	/**
	 * Called by the framework when a ManagerProxy is assigned to this node.
	 *
	 * Delegates to the superclass, then conditionally populates the tree node
	 * with child WatSimulationNode objects if the system property
	 * "SimGroupNode.HasSimulations" is set to true. The child addition is deferred
	 * to the EDT via invokeLater to avoid modifying the tree model from a non-EDT thread.
	 *
	 * @param proxy the ManagerProxy wrapping the SimulationGroup for this node
	 */
	@Override
	public void setManagerProxy(ManagerProxy proxy) {
		super.setManagerProxy(proxy);

		// Conditionally populate child simulation nodes based on a system property
		if (Boolean.getBoolean("SimGroupNode.HasSimulations")) {
			EventQueue.invokeLater(() -> addSimulations());
		}
	}


	/**
	 * Retrieves the current SimulationGroup from the manager and adds all of its
	 * simulations to the display. If no SimulationGroup is currently assigned to
	 * the manager, the method returns without taking any action. Delegates the
	 * actual addition logic to the overloaded addSimulations(List) method.
	 */
	private void addSimulations() {
		// Retrieve the current manager and cast it to a SimulationGroup
		SimulationGroup simGroup = (SimulationGroup) getManager();

		// Only proceed if a valid SimulationGroup is available
		if (simGroup != null) {
			// Retrieve the full list of simulations belonging to this group
			List<WatSimulation> sims = simGroup.getSimulations();

			// Delegate to the overloaded method to add each simulation to the display
			addSimulations(sims);
		}
	}


	/**
	 * Clears all existing simulation entries and repopulates the display with the
	 * provided list of simulations. Existing children are removed first to prevent
	 * duplicate entries before each simulation in the list is added individually
	 * via the addSimulation() method.
	 *
	 * @param sims the list of WatSimulation objects to add to the display
	 */
	private void addSimulations(List<WatSimulation> sims) {
		// Remove all existing children before repopulating to prevent duplicates
		removeAllChildren();

		// Declare a variable to hold the current simulation during iteration
		WatSimulation sim;

		// Iterate over each simulation in the list and add it to the display
		for (int i = 0; i < sims.size(); i++) {
			// Retrieve the simulation at the current index
			sim = sims.get(i);

			// Add the individual simulation to the display
			addSimulation(sim);
		}
	}


	/**
	 * Creates a tree node for the given simulation and adds it as a child of this node.
	 *
	 * The node type is determined by ProjectNodeFactory. Only ManagerNode instances
	 * are added; other node types are silently ignored. The manager proxy is set on
	 * the new node before it is inserted.
	 *
	 * @param sim the simulation to add as a child node; must not be null
	 * @return the newly created and inserted MutableTreeNode, or null if the factory
	 * did not produce a ManagerNode for this simulation
	 */
	private MutableTreeNode addSimulation(WatSimulation sim) {
		// Ask the factory for the appropriate node type for this simulation
		MutableTreeNode node = ProjectNodeFactory.getProjectNode(sim, this);

		if (node instanceof ManagerNode) {
			// Assign the proxy so the node can resolve its display name and editor
			ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(sim);
			((ManagerNode) node).setManagerProxy(proxy);
			add(node);
			return node;
		}

		return null;
	}


	/**
	 * Adds a child node and notifies the project tree of the structural change.
	 *
	 * After inserting the node, the tree model is notified via nodesWereInserted
	 * and nodeChanged so the UI repaints. If this node is configured to always
	 * expand, expandNode is also called.
	 *
	 * @param node the child node to add; must not be null
	 */
	@Override
	public void add(MutableTreeNode node) {
		super.add(node);

		// Notify the tree model so the UI reflects the new child
		_tree.nodesWereInserted(this, new TreeNode[]{node});
		_tree.nodeChanged(this);

		// Auto-expand this node if it is configured to always show its children
		if (this.shouldAlwaysExpand()) {
			_tree.expandNode(this);
		}
	}


	/**
	 * Builds and returns the list of content tree nodes for the current SimulationGroup.
	 * The method constructs nodes across four sections:
	 *   1. For transitory groups, simulation nodes are rebuilt from the current children
	 *      using the project node factory to ensure an up-to-date representation.
	 *   2. An analysis period node is added if a ManagerProxy exists for the group's
	 *      analysis period.
	 *   3. A time-window summary node is added with start and end time child nodes
	 *      if an analysis period is present.
	 *   4. Simulation nodes are created for all simulations in the group and added
	 *      to the content list.
	 *
	 * @return a list of tree nodes representing the content of the simulation group
	 */
	public List getContentNodes() {
		// Initialize the list that will accumulate all content nodes for this group
		List contentNodes = new ArrayList<>();

		// Retrieve the current manager and cast it to a SimulationGroup
		SimulationGroup simGroup = (SimulationGroup) getManager();

		// --- Section 1: transitory group -- rebuild simulation nodes from current children ---
		if (simGroup.isTransitory()) {
			// Get the current number of child nodes to iterate over
			int cnt = getChildCount();

			// Declare variables for use during the child node traversal
			WatSimulation sim;
			TreeNode node;
			WatSimulationNode simNode;

			for (int i = 0; i < cnt; i++) {
				// Retrieve the child node at the current index
				node = getChildAt(i);

				// Only process children that are WatSimulationNode instances
				if (node instanceof WatSimulationNode) {
					simNode = (WatSimulationNode) node;

					// Extract the simulation associated with this node
					sim = simNode.getSimulation();

					// Obtain a fresh node from the factory for an up-to-date representation
					node = ProjectNodeFactory.getProjectNode(sim, this);

					if (node instanceof WatSimulationNode) {
						simNode = (WatSimulationNode) node;

						// Attach the manager proxy for this simulation to the refreshed node
						simNode.setManagerProxy(
								Project.getCurrentProject().getManagerProxy(sim));
						contentNodes.add(node);
					}
				}
			}
		}

		// --- Section 2: analysis period node ---
		// Retrieve the analysis period and its corresponding manager proxy
		WatAnalysisPeriod ap = simGroup.getAnalysisPeriod();
		ManagerProxy apProxy = Project.getCurrentProject().getManagerProxy(ap);

		if (apProxy != null) {
			// Create the AP node without sub-event nodes; mark it as part of the content tree
			WatAnalysisPeriodNode apNode = new WatAnalysisPeriodNode(apProxy, true);

			// Suppress sub-event nodes under the analysis period node
			apNode.setAddEventNodes(false);

			// Flag the node as belonging to the content tree rather than the project tree
			apNode.setInContentTree(true);
			apNode.setManagerProxy(apProxy);
			contentNodes.add(apNode);
		}

		// --- Section 3: time-window summary node (start and end times as children) ---
		MutableTreeNode node;
		if (ap != null) {
			// Create the parent "Time Window" node with a clock icon
			DefaultContentNode cnode = new DefaultContentNode(WatI18n.getI18n(WatMessages.SIMULATION_NODE_TIME_WINDOW_NODE).getText());
			cnode.setIcon(RmaImage.getImageIcon("Images/clock.gif"));

			// Mark this node as a non-leaf so its start and end time children are shown
			cnode.setIsLeaf(false);

			// Retrieve the run time window from the analysis period for start/end time access
			RunTimeWindow rtw = ap.getRunTimeWindow();
			HecTime start = rtw.getStartTime();
			HecTime end = rtw.getEndTime();

			// Start time child node with a green-ball icon
			DefaultContentNode startNode = new DefaultContentNode(WatI18n.getI18n(WatMessages.SIMULATION_NODE_START_TIME_NODE).format(start));
			startNode.setIcon(RmaImage.getImageIcon("Images/green-ball.gif"));
			cnode.add(startNode);

			// End time child node with a red-ball icon
			DefaultContentNode endNode = new DefaultContentNode(WatI18n.getI18n(WatMessages.SIMULATION_NODE_END_TIME_NODE).format(end));
			endNode.setIcon(RmaImage.getImageIcon("Images/red-ball.gif"));
			cnode.add(endNode);

			// Add the completed time-window node with its children to the content list
			contentNodes.add(cnode);
		}

		// --- Section 4: simulation nodes for all simulations in the group ---
		// Retrieve the full list of simulations belonging to this group
		List<WatSimulation> sims = simGroup.getSimulations();
		WatSimulation sim;

		// Create and add a content node for each simulation in the group
		for (int i = 0; i < sims.size(); i++) {
			sim = sims.get(i);

			// Build a project tree node for the current simulation using the factory
			node = ProjectNodeFactory.getProjectNode(sim, this);

			// Only add the node if it is a valid WatSimulationNode
			if (node instanceof WatSimulationNode) {
				WatSimulationNode simNode = (WatSimulationNode) node;

				// Attach the manager proxy for this simulation to its content node
				simNode.setManagerProxy(Project.getCurrentProject().getManagerProxy(sim));
				contentNodes.add(node);
			}
		}

		// Return the fully populated list of content nodes
		return contentNodes;
	}


	/**
	 * Indicates that the content tree should be expanded when this node is selected.
	 *
	 * @return always true so the content pane shows all children on first display
	 */
	@Override
	public boolean shouldExpandContentTree() {
		return true;
	}


	/**
	 * Returns the folder icon used to represent this node in the project tree.
	 *
	 * @return the shared folder ImageIcon loaded at class initialisation; never null
	 */
	@Override
	public Icon getIcon() {
		return _folderIcon;
	}


	/**
	 * Returns the editor for this node.
	 *
	 * SimulationGroupNode does not have a dedicated editor, so null is returned.
	 * Editing of the group is performed through the CalibrationPanel.
	 *
	 * @return always null
	 */
	@Override
	public DataEditor getEditor() {
		return null;
	}


	/**
	 * Returns the list of WatSimulation objects belonging to the wrapped SimulationGroup.
	 *
	 * @return the group's simulation list; never null but may be empty
	 */
	public List<WatSimulation> getSimulations() {
		SimulationGroup simGroup = (SimulationGroup) getManager();
		return simGroup.getSimulations();
	}


	/**
	 * Configures this node to display all simulations not assigned to any other group.
	 *
	 * Schedules buildNode on the EDT. buildNode registers a SimGroupProjectManagerListener
	 * that keeps the "unassigned" simulation children in sync as simulations are added
	 * or removed from the project, then calls addSimsNotInGroup to populate the initial
	 * set of unassigned children.
	 */
	public void setAddSimsNotInGroup() {
		EventQueue.invokeLater(() -> buildNode());
	}


	/**
	 * Registers the unassigned-simulation listener and defers the initial population
	 * to a subsequent EDT pass so that all sibling nodes have been added first.
	 */
	private void buildNode() {
		// Register the listener before populating so no additions are missed
		addNotInGroupManagerListener();

		// Defer population to ensure sibling SimulationGroupNodes are already in the tree
		EventQueue.invokeLater(() -> addSimsNotInGroup());
	}


	/**
	 * Populates this node with all WatSimulation objects that are not members of any
	 * named SimulationGroup in the current project.
	 *
	 * The method:
	 * 1. Retrieves all simulations and all simulation groups from the project.
	 * 2. Skips the group wrapped by this node (it is the "unassigned" catch-all).
	 * 3. For each other group, checks whether the parent SimGroupContainerNode needs
	 * to add the group as a sibling, then removes that group's simulations from
	 * the all-simulations list.
	 * 4. Adds the remaining (unassigned) simulations as children of this node.
	 *
	 * Does nothing if the current project is the "no project" placeholder.
	 */
	private void addSimsNotInGroup() {
		// Guard: no project is open
		if (Project.getCurrentProject().isNoProject()) {
			return;
		}

		List<WatSimulation> allSims = Project.getCurrentProject().getManagerListForType(WatSimulation.class);
		List<SimulationGroup> simGroups = Project.getCurrentProject().getManagerListForType(SimulationGroup.class);

		int cnt = simGroups.size();
		List<WatSimulation> sgSims;

		// Check whether the parent is a SimGroupContainerNode so sibling groups can be added
		TreeNode parentNode = getParent();
		SimGroupContainerNode containerParent = null;
		if (parentNode instanceof SimGroupContainerNode) {
			containerParent = (SimGroupContainerNode) parentNode;
		}

		SimulationGroup simGrp;
		boolean addedNodes = false;

		for (int i = 0; i < cnt; i++) {
			simGrp = simGroups.get(i);

			// Skip the group this node itself represents
			if (simGrp == getSimulationGroup()) {
				continue;
			}

			// Offer each other group to the container so it can add a sibling node
			if (containerParent != null) {
				if (containerParent.checkAndAddSimGroup(simGrp)) {
					addedNodes = true;
				}
			}

			// Remove this group's simulations from the pool so they are not shown here
			sgSims = simGrp.getSimulations();
			allSims.removeAll(sgSims);
		}

		// Add the remaining unassigned simulations as children of this node
		addSimulations(allSims);
	}


	/**
	 * Returns the SimulationGroup wrapped by this node.
	 *
	 * @return the SimulationGroup cast from the underlying manager; may be null if no
	 * manager has been set
	 */
	private SimulationGroup getSimulationGroup() {
		return (SimulationGroup) getManager();
	}


	/**
	 * Registers a SimGroupProjectManagerListener on the current project so that
	 * unassigned simulations are added or removed as they are created or deleted.
	 */
	private void addNotInGroupManagerListener() {
		Project.getCurrentProject().addManagerListener(
				new SimGroupProjectManagerListener());
	}


	/**
	 * Searches the direct children of this node for a WatSimulationNode that wraps
	 * the specified WatSimulation instance. The comparison uses reference equality
	 * rather than equals(), since each simulation is a unique object in the tree.
	 * Returns the matching WatSimulationNode if found, or null if no child node
	 * corresponds to the given simulation.
	 *
	 * @param sim the WatSimulation instance to search for among the child nodes
	 * @return    the matching WatSimulationNode, or null if no match is found
	 */
	protected TreeNode findSimNode(WatSimulation sim) {
		// Get the total number of direct child nodes to iterate over
		int childCnt = getChildCount();

		// Declare variables to hold the current child node and its cast form during iteration
		TreeNode node;
		WatSimulationNode simNode;

		// Iterate over all child nodes to find one that wraps the target simulation
		for (int i = 0; i < childCnt; i++) {
			// Retrieve the child node at the current index
			node = getChildAt(i);

			// Only inspect children that are WatSimulationNode instances
			if (node instanceof WatSimulationNode) {
				simNode = (WatSimulationNode) node;

				// Identity comparison is appropriate; each simulation is a unique object
				if (simNode.getSimulation() == sim) {
					// Match found; return the node immediately
					return simNode;
				}
			}
		}

		// No matching node was found among the children; return null to signal absence
		return null;
	}


	/**
	 * Controls whether the node's toString label includes the current child count.
	 *
	 * When true the label appears as "GroupName (N)" where N is the number of children.
	 *
	 * @param showCount true to append the child count to the display label; false for
	 *                  the plain group name only
	 */
	public void setShowCount(boolean showCount) {
		_showCnt = showCount;
	}


	/**
	 * Returns the display label for this node, optionally including the child count.
	 *
	 * @return the node label from the superclass, with the child count appended in
	 * parentheses if showCount was set to true
	 */
	@Override
	public String toString() {
		if (_showCnt) {
			// Append the current child count in parentheses for quick visibility
			return super.toString() + " (" + getChildCount() + ")";
		}

		return super.toString();
	}


	/**
	 * Stores the owning ProjectTree reference required for tree-model change notifications.
	 *
	 * Must be called before any add, remove, or structural change operations are
	 * performed on this node; otherwise a NullPointerException will occur when the
	 * tree model methods are invoked.
	 *
	 * @param tree the ProjectTree that contains this node; must not be null
	 */
	public void setTree(ProjectTree tree) {
		_tree = tree;
	}


	/**
	 * Inner project manager listener that keeps this node's "unassigned simulations"
	 * children in sync with the project as simulations are created or deleted.
	 *
	 * On addition: the new simulation is added as a child of this node only if no
	 * other sibling SimulationGroupNode already claims it.
	 * On deletion: the matching child WatSimulationNode is located and removed, and
	 * the tree model is notified so the UI updates.
	 */
	private class SimGroupProjectManagerListener implements ProjectManagerListener {
		/**
		 * Responds to a newly added simulation manager by conditionally inserting it into
		 * this node's child list. The sibling SimulationGroupNodes are checked first; if any
		 * of them already contain the simulation, it belongs to an assigned group and is not
		 * added here. If no sibling group claims the simulation, it is treated as unassigned
		 * and added as a child of this node. The tree is then notified of the insertion and
		 * expanded to show the new child. This method overrides the base class implementation
		 * to provide simulation-group-aware manager addition behavior.
		 *
		 * @param proxy the ManagerProxy wrapping the newly added WatSimulation
		 */
		@Override
		public void managerAdded(ManagerProxy proxy) {
			// Unwrap the proxy to obtain the newly added simulation
			WatSimulation sim = (WatSimulation) proxy.getManager();

			// Get the total number of sibling nodes under the shared parent
			int cnt = getParent().getChildCount();

			// Declare variables for iterating over sibling SimulationGroupNodes
			SimulationGroupNode sgNode;
			List<WatSimulation> sgSims;

			// Start at index 1; index 0 is assumed to be this node (the catch-all)
			for (int i = 1; i < cnt; i++) {
				// Retrieve the sibling node at the current index
				TreeNode node = getParent().getChildAt(i);

				// Only check siblings that are SimulationGroupNodes
				if (node instanceof SimulationGroupNode) {
					sgNode = (SimulationGroupNode) node;

					// Retrieve the list of simulations already assigned to this sibling group
					sgSims = sgNode.getSimulations();

					// If another group already contains this simulation, do not add it here
					if (sgSims.contains(sim)) {
						return;
					}
				}
			}

			// No other group claims this simulation; add it as an unassigned child
			MutableTreeNode node = addSimulation(sim);

			if (node != null) {
				// Insert the new simulation node into this node's child list
				add(node);

				// Notify the tree model that a new child node has been inserted
				_tree.nodesWereInserted(SimulationGroupNode.this, new TreeNode[]{node});

				// Notify the tree that this node's appearance may have changed
				_tree.nodeChanged(SimulationGroupNode.this);

				// Expand this node so the newly added simulation is immediately visible
				_tree.expandNode(SimulationGroupNode.this);
			}
		}

		/**
		 * Responds to a deleted simulation manager by removing its corresponding tree node
		 * from this node's child list. The child node is located by simulation reference
		 * using findSimNode(). If no matching child is found, no action is taken. When a
		 * match is found, the node is removed by index to satisfy the tree model notification
		 * contract, and the tree is notified of the removal. This method overrides the base
		 * class implementation to provide simulation-group-aware manager deletion behavior.
		 *
		 * @param proxy the ManagerProxy wrapping the deleted WatSimulation
		 */
		@Override
		public void managerDeleted(ManagerProxy proxy) {
			// Unwrap the proxy to obtain the simulation that was deleted
			WatSimulation sim = (WatSimulation) proxy.getManager();

			// Search the child nodes for the tree node that wraps this simulation
			TreeNode simNode = findSimNode(sim);

			// Only proceed if a matching child node was found
			if (simNode != null) {
				// Remove by index to satisfy the tree model notification contract
				int idx = getIndex(simNode);
				remove(idx);

				// Notify the tree model that the child node has been removed
				_tree.nodesWereRemoved(SimulationGroupNode.this,
						new TreeNode[]{simNode});
			}
		}

		/**
		 * Restricts this listener to WatSimulation manager events only.
		 *
		 * @return WatSimulation.class
		 */
		@Override
		public Class getManagerClass() {
			return WatSimulation.class;
		}
	}
}
