package usbr.wat.plugins.actionpanel.ui.tree;

import java.util.List;                                                      // Provides the List interface for the ordered collection of WatSimulation objects from the group

import javax.swing.event.TreeModelEvent;                                    // Provides TreeModelEvent for constructing node-change notifications sent to registered listeners
import javax.swing.event.TreeModelListener;                                 // Provides TreeModelListener for iterating over and notifying all registered tree model listeners
import javax.swing.tree.TreeNode;                                           // Provides TreeNode for casting child objects when calling getIndex during child index lookup

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;                // Provides AbstractTreeTableModel, the SwingX base class that manages listener registration
import org.jdesktop.swingx.treetable.TreeTableNode;                         // Provides TreeTableNode for accessing typed child, column, and value APIs on model nodes

import hec2.wat.model.WatSimulation;                                        // Provides WatSimulation for populating the model with simulation entries from the group

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;          // Provides AbstractSimulationGroup for obtaining the list of WAT simulations at construction

/**
 * The TreeTableModel that backs the SimulationTreeTable. The tree has a single root
 * (a SimulationTreeTableNode with no simulation) whose direct children are one
 * SimulationTreeTableNode per WAT simulation in the supplied AbstractSimulationGroup.
 * Each SimulationTreeTableNode may in turn have ResultsTreeTableNode children for
 * its associated results sets.
 *
 * The model exposes four columns, identified by the public index constants:
 *
 * SIMULATION_COLUMN (0)     — the simulation or results name; read-only.
 * SELECTED_COLUMN   (1)     — a boolean checkbox; always editable.
 * DISPLAY_IN_MAPS_COLUMN (2)— a button; editable only on SimulationTreeTableNode rows.
 * VIEW_REPORT_COLUMN (3)    — a button; always editable.
 *
 * Mutation notifications are dispatched to all registered TreeModelListeners via the
 * fire* family of methods. Each notification constructs a TreeModelEvent using the
 * affected node's TreePath, obtained through the ActionsTreeTableNode interface.
 *
 * @see SimulationTreeTable
 * @see SimulationTreeTableNode
 * @see ResultsTreeTableNode
 */
public class SimulationTreeTableModel extends AbstractTreeTableModel {
	/**
	 * Zero-based column index for the checkbox that selects a simulation or results row.
	 */
	public static final int SELECTED_COLUMN = 1;

	/**
	 * Zero-based column index for the simulation or results name (tree expand column).
	 */
	public static final int SIMULATION_COLUMN = 0;

	/**
	 * Zero-based column index for the "Show on Map" button column.
	 */
	public static final int DISPLAY_IN_MAPS_COLUMN = 2;

	/**
	 * Zero-based column index for the "View Report" button column.
	 */
	public static final int VIEW_REPORT_COLUMN = 3;

	/**
	 * Shared array of column header strings, indexed by the column constants above.
	 * Referenced by node classes to keep column counts consistent with the model.
	 */
	public static final String[] _headers = new String[]
			{
					"Simulation", "Selected", "Map", "Report"
			};

	/**
	 * The invisible root SimulationTreeTableNode; its children are the top-level simulations.
	 */
	private SimulationTreeTableNode _root;

	/**
	 * Constructs a SimulationTreeTableModel populated with one SimulationTreeTableNode
	 * per WAT simulation in the given group. When simGroup is null, the model is created
	 * with only the invisible root node and no simulation children.
	 *
	 * @param simGroup the AbstractSimulationGroup whose simulations populate the tree,
	 *                 or null to produce an empty model
	 */
	public SimulationTreeTableModel(AbstractSimulationGroup simGroup) {
		// Create the invisible root node; its simulation reference is null
		super(new SimulationTreeTableNode(null, null));
		_root = (SimulationTreeTableNode) getRoot();

		// Register this model on the root so nodes can fire change notifications
		_root.setSimulationTreeModel(this);

		if (simGroup != null) {
			// Add one child node per simulation in the group, in list order
			List<WatSimulation> sims = simGroup.getSimulations();
			for (int i = 0; i < sims.size(); i++) {
				SimulationTreeTableNode node = new SimulationTreeTableNode(this, sims.get(i));
				_root.add(node);
			}
		}
	}

	/**
	 * Returns the total number of columns in this model, equal to the length of the
	 * shared _headers array.
	 *
	 * @return the column count; always 4 for this model
	 */
	@Override
	public int getColumnCount() {
		return _headers.length;
	}

	/**
	 * Returns the name of the column at the given index, used to initialise the
	 * table's column header. Column names do not need to be unique.
	 *
	 * @param column the zero-based column index
	 * @return the header string for the specified column
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		return _headers[column];
	}

	/**
	 * Returns the value at the given column for the specified node object. Delegates
	 * to the node's own getValueAt when the node is a TreeTableNode; returns null
	 * for unrecognised node types.
	 *
	 * @param node   the tree node object whose column value is requested
	 * @param column the zero-based column index
	 * @return the cell value from the node, or null if the node is not a TreeTableNode
	 */
	@Override
	public Object getValueAt(Object node, int column) {
		if (node instanceof TreeTableNode) {
			return ((TreeTableNode) node).getValueAt(column);
		}
		return null;
	}

	/**
	 * Sets the value of the given column on the specified node and notifies listeners
	 * that the node has changed. Throws IllegalArgumentException for out-of-bounds
	 * column indices. Silently ignores attempts to set a column beyond the node's
	 * own column count.
	 *
	 * @param value  the new value to set on the node
	 * @param node   the tree node object to update; must be a TreeTableNode
	 * @param column the zero-based column index to update
	 * @throws IllegalArgumentException if the column index is outside [0, getColumnCount())
	 */
	@Override
	public void setValueAt(Object value, Object node, int column) {
		// Reject clearly invalid column indices before touching any node state
		if (column < 0 || column >= getColumnCount()) {
			throw new IllegalArgumentException("column must be a valid index");
		}

		TreeTableNode ttn = (TreeTableNode) node;

		if (column < ttn.getColumnCount()) {
			// Delegate the actual value assignment to the node and then notify listeners
			ttn.setValueAt(value, column);
			fireNodeChanged(ttn);
		}
	}

	/**
	 * Returns the child of the given parent node at the specified index. Delegates to
	 * TreeTableNode.getChildAt when the parent is a TreeTableNode; returns null otherwise.
	 *
	 * @param parentObj the parent node object
	 * @param childIdx  the zero-based index of the child to retrieve
	 * @return the child TreeTableNode at the given index, or null for unrecognised parents
	 */
	@Override
	public Object getChild(Object parentObj, int childIdx) {
		if (parentObj instanceof TreeTableNode) {
			return ((TreeTableNode) parentObj).getChildAt(childIdx);
		}
		return null;
	}

	/**
	 * Returns the number of children of the given node. Delegates to
	 * TreeTableNode.getChildCount when the node is a TreeTableNode; returns 0 otherwise.
	 *
	 * @param obj the node object whose child count is requested
	 * @return the child count, or 0 for unrecognised node types
	 */
	@Override
	public int getChildCount(Object obj) {
		if (obj instanceof TreeTableNode) {
			return ((TreeTableNode) obj).getChildCount();
		}
		return 0;
	}

	/**
	 * Returns the index of the given child within its parent's child list. Delegates
	 * to TreeTableNode.getIndex when the parent is a TreeTableNode. Note: the result
	 * of the delegate call is not returned; this method always returns 0, which is a
	 * known limitation.
	 *
	 * @param parentObj the parent node object
	 * @param childObj  the child node object whose index is requested
	 * @return 0 always (the delegate result is not captured; see implementation note)
	 */
	@Override
	public int getIndexOfChild(Object parentObj, Object childObj) {
		if (parentObj instanceof TreeTableNode) {
			// Note: the result of getIndex is not returned; this is a known limitation
			((TreeTableNode) parentObj).getIndex((TreeNode) childObj);
		}
		return 0;
	}

	/**
	 * Determines whether the given node and column combination is editable. The rules are:
	 * SELECTED_COLUMN is always editable.
	 * SIMULATION_COLUMN is never editable.
	 * DISPLAY_IN_MAPS_COLUMN is editable only for SimulationTreeTableNode cells.
	 * VIEW_REPORT_COLUMN is always editable.
	 *
	 * @param cellObj the node object for the cell being queried
	 * @param col     the zero-based column index
	 * @return true if the cell should be editable; false otherwise
	 */
	@Override
	public boolean isCellEditable(Object cellObj, int col) {
		boolean retval = false;

		switch (col) {
			case SELECTED_COLUMN:
				// The checkbox column is editable for all node types
				retval = true;
				break;
			case SIMULATION_COLUMN:
				// The name column is always read-only
				retval = false;
				break;
			case DISPLAY_IN_MAPS_COLUMN:
				// The map button is only active on simulation (parent) rows, not results rows
				if (cellObj instanceof SimulationTreeTableNode) {
					retval = true;
				}
				break;
			case VIEW_REPORT_COLUMN:
				// The report button is editable for both simulation and results rows
				retval = true;
				break;
		}
		return retval;
	}

	/**
	 * Notifies all registered listeners that the entire tree structure has changed
	 * by firing a treeStructureChanged event rooted at the model's root node.
	 * Used when the full simulation list is replaced.
	 */
	public void fireTableDataChanged() {
		fireNodeStructureChanged(_root);
	}

	/**
	 * Fires a treeStructureChanged event for the given node, notifying all registered
	 * listeners that the subtree rooted at that node has been restructured.
	 *
	 * @param node the TreeTableNode whose subtree structure has changed; must also
	 *             implement ActionsTreeTableNode to provide its TreePath
	 */
	public void fireNodeStructureChanged(TreeTableNode node) {
		TreeModelListener[] listeners = getTreeModelListeners();

		// Build the event using the node's own TreePath via the ActionsTreeTableNode interface
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode) node).getPath());

		for (int i = 0; i < listeners.length; i++) {
			listeners[i].treeStructureChanged(event);
		}
	}

	/**
	 * Fires a treeNodesChanged event for the given node, notifying all registered
	 * listeners that the node's data has been updated in place. Does nothing when
	 * node is null.
	 *
	 * @param node the TreeTableNode whose data has changed; must also implement
	 *             ActionsTreeTableNode to provide its TreePath
	 */
	public void fireNodeChanged(TreeTableNode node) {
		if (node == null) {
			return;
		}

		// Construct the event and forward it to the shared multi-listener dispatcher
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode) node).getPath());
		fireNodesChanged(event);
	}

	/**
	 * Dispatches the given TreeModelEvent to all registered listeners via their
	 * treeNodesChanged callback. Does nothing when the event is null.
	 *
	 * @param event the TreeModelEvent to send to all registered listeners; may be null
	 */
	public void fireNodesChanged(TreeModelEvent event) {
		if (event == null) {
			return;
		}

		// Deliver the event to every registered listener in registration order
		TreeModelListener[] listeners = getTreeModelListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].treeNodesChanged(event);
		}
	}

	/**
	 * Fires a treeNodesInserted event for the given node, notifying all registered
	 * listeners that a new node has been added to the tree. Does nothing when node is null.
	 *
	 * @param node the newly inserted TreeTableNode; must also implement
	 *             ActionsTreeTableNode to provide its TreePath
	 */
	public void fireNodeInserted(TreeTableNode node) {
		if (node == null) {
			return;
		}

		TreeModelListener[] listeners = getTreeModelListeners();

		// Build the event using the inserted node's TreePath
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode) node).getPath());

		for (int i = 0; i < listeners.length; i++) {
			listeners[i].treeNodesInserted(event);
		}
	}

	/**
	 * Fires a treeNodesRemoved event for the given node, notifying all registered
	 * listeners that a node has been removed from the tree. Does nothing when node is null.
	 *
	 * @param node the removed TreeTableNode; must also implement ActionsTreeTableNode
	 *             to provide its TreePath
	 */
	public void fireNodeRemoved(TreeTableNode node) {
		if (node == null) {
			return;
		}

		TreeModelListener[] listeners = getTreeModelListeners();

		// Build the event using the removed node's TreePath
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode) node).getPath());

		for (int i = 0; i < listeners.length; i++) {
			listeners[i].treeNodesRemoved(event);
		}
	}
}
