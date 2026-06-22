package usbr.wat.plugins.actionpanel.ui.tree;

import javax.swing.JPopupMenu;          // Provides JPopupMenu for the context menu that implementations populate with node-specific items
import javax.swing.tree.TreePath;       // Provides TreePath for identifying a node's position within the tree hierarchy

/**
 * Contract for nodes that appear in the Actions tree-table. Implementations
 * must supply a tooltip string, a TreePath locating the node in the tree
 * hierarchy, and a method for contributing context menu items to a shared
 * JPopupMenu when the node is right-clicked.
 *
 * This interface is implemented by concrete node types in the Actions tree-table
 * (such as simulation group nodes and simulation nodes) to allow the tree-table
 * to delegate node-specific behaviour without coupling to concrete node types.
 *
 * @author mark
 * @see SimulationTreeTable
 */
public interface ActionsTreeTableNode {
	/**
	 * Returns the tooltip text to display when the mouse hovers over this node
	 * in the Actions tree-table.
	 *
	 * @return a non-null tooltip string; may be empty if no tooltip is required
	 */
	String getToolTipText();

	/**
	 * Returns the TreePath that identifies this node's location within the
	 * tree-table model hierarchy. Used by the tree-table to scroll to, select,
	 * and expand nodes programmatically.
	 *
	 * @return the TreePath from the root to this node; must not be null
	 */
	TreePath getPath();

	/**
	 * Adds node-specific items to the given JPopupMenu. Called by the tree-table
	 * when the user right-clicks on this node, allowing each node type to contribute
	 * its own context menu actions without the tree-table needing to know the
	 * concrete node type.
	 *
	 * @param popup the JPopupMenu to which this node's menu items should be added;
	 *              must not be null
	 */
	void addPopupMenuItems(JPopupMenu popup);
}
