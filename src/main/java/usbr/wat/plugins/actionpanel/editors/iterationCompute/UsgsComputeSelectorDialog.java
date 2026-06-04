package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Frame;           // AWT Frame used as the parent window for this dialog
import java.util.List;           // Ordered collection interface for the list of UsbrComputable objects to select

import javax.swing.tree.DefaultMutableTreeNode; // Mutable tree node used to traverse the compute selector tree

import com.rma.swing.tree.DefaultCheckBoxNode;  // RMA tree node subtype that supports a checked/unchecked selection state

import hec2.wat.client.WatComputeSelectorDialog; // HEC-WAT base dialog for selecting computable items from a checkbox tree
import hec2.wat.model.WatSimulation;             // Represents a WAT simulation; the simClass parameter constrains the tree

import usbr.wat.plugins.actionpanel.model.ActionComputable; // USBR base interface for all computable action items (unused directly here, present for context)
import usbr.wat.plugins.actionpanel.model.UsbrComputable;   // USBR-specific computable type whose name is matched against tree node labels

/**
 * USBR-specific extension of the HEC-WAT WatComputeSelectorDialog that adds
 * support for pre-selecting a list of UsbrComputable items in the checkbox tree.
 *
 * The base WatComputeSelectorDialog presents a tree of available WAT simulations
 * and computable items. This subclass adds setSelectedComputables(), which
 * recursively walks the tree and checks the node whose label matches each
 * UsbrComputable's name, replacing the node's user object with the actual
 * UsbrComputable instance so that it can be retrieved after the dialog closes.
 *
 * Note: the class name uses "Usgs" rather than "Usbr" but operates on USBR
 * computable types — this appears to be a naming inconsistency in the original code.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class UsgsComputeSelectorDialog extends WatComputeSelectorDialog {

	/**
	 * Constructs a UsgsComputeSelectorDialog with the given parent frame and
	 * simulation class constraint.
	 *
	 * Delegates entirely to the WatComputeSelectorDialog superclass constructor.
	 *
	 * @param parent   the Frame that owns this dialog
	 * @param simClass the WatSimulation class used to filter the tree content
	 */
	public UsgsComputeSelectorDialog(Frame parent, Class<WatSimulation> simClass) {
		// Delegate construction to the base HEC-WAT compute selector dialog
		super(parent, simClass);
	}

	/**
	 * Pre-selects the given list of UsbrComputable items in the checkbox tree.
	 *
	 * Clears all existing selections first, then walks the tree to find and
	 * check the node whose label matches each computable's name. The node's
	 * user object is replaced with the UsbrComputable instance so it is
	 * available when the dialog closes. Has no effect if the list is null.
	 *
	 * @param computables the list of UsbrComputable objects to select in the tree
	 */
	public void setSelectedComputables(List<UsbrComputable> computables) {
		// Do nothing if no computables were provided
		if (computables == null) {
			return;
		}

		UsbrComputable computable;

		// Clear all existing checkbox selections before applying the new ones
		deselectAllTreeNodes();

		// Select the tree node corresponding to each computable in the list
		for (int i = 0; i < computables.size(); i++) {
			computable = computables.get(i);
			selectSimulation(computable);
		}
	}

	/**
	 * Initiates a recursive tree search to select the node matching the given computable.
	 *
	 * Starts the search from the tree's root node. Returns true immediately if
	 * the computable is null (treated as already handled).
	 *
	 * @param computable the UsbrComputable whose matching tree node should be selected
	 * @return true if the computable was null or a matching node was found; false otherwise
	 */
	private boolean selectSimulation(UsbrComputable computable) {
		// Null computable is treated as a no-op success
		if (computable == null) {
			return true;
		}

		// Begin the recursive search from the root node
		return selectSimulation(_root, computable);
	}

	/**
	 * Recursively searches the subtree rooted at the given parent node for a
	 * leaf node whose label matches the computable's name.
	 *
	 * For each child node: if the child is not a leaf, the search recurses into
	 * it. If the child's label matches the computable name, the node is checked
	 * and its user object is replaced with the UsbrComputable instance.
	 *
	 * @param parent     the DefaultMutableTreeNode to search beneath
	 * @param computable the UsbrComputable whose name to match against node labels
	 * @return true if a matching node was found and selected; false otherwise
	 */
	private boolean selectSimulation(DefaultMutableTreeNode parent, UsbrComputable computable) {
		DefaultCheckBoxNode item;
		boolean selected = false;

		// The simulation name to match against node labels
		String simName = computable.getName();

		for (int i = 0; i < parent.getChildCount(); i++) {
			item = (DefaultCheckBoxNode) parent.getChildAt(i);

			// Recurse into non-leaf nodes to search their subtrees
			if (!item.isLeaf()) {
				if (selectSimulation(item, computable)) {
					return true;
				}
			}

			// Check if this node's label matches the computable's name
			if (item.getUserObject().toString().equals(simName)) {
				// Mark the node as selected in the checkbox tree
				item.setSelected(true);

				// Replace the generic user object with the actual UsbrComputable instance
				item.setUserObject(computable);
				selected = true;
			}
		}

		return selected;
	}
}
