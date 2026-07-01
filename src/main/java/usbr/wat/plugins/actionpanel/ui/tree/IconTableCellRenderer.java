package usbr.wat.plugins.actionpanel.ui.tree;

import java.awt.Color;                          // Provides Color for reading the row foreground colour from the tree-table for unselected rows
import java.awt.Component;                      // Provides Component as the return type of getTreeCellRendererComponent

import javax.swing.ImageIcon;                   // Provides ImageIcon for the WAT computation and results icon images loaded at construction
import javax.swing.JLabel;                      // Provides JLabel, the component type returned by the default renderer that receives icon and colour overrides
import javax.swing.JTree;                       // Provides JTree, the tree component passed to the renderer by the Swing rendering pipeline
import javax.swing.tree.TreeCellRenderer;       // Provides TreeCellRenderer, the interface this class implements and that the default renderer also satisfies
import rma.swing.RmaImage;                      // Provides RmaImage for loading icon images from the application's image resource paths
import rma.swing.RmaJXTreeTable;                // Provides RmaJXTreeTable for accessing row foreground colours used during unselected-row rendering

/**
 * A custom TreeCellRenderer for the Actions simulation tree-table that overlays
 * node-type-specific icons onto the component produced by the default renderer.
 *
 * The renderer delegates all base rendering (text, selection highlight, expansion
 * indicator) to the wrapped default TreeCellRenderer, then applies one of two icons
 * to the resulting JLabel component:
 *
 * SimulationTreeTableNode rows receive the WAT computation icon (comp16x16.gif).
 * ResultsTreeTableNode rows receive the tabulate/results icon (tabulate18.gif).
 *
 * For unselected rows, the label foreground colour is overridden with the per-row
 * colour provided by RmaJXTreeTable.getRowForeground, which allows the tree-table
 * to apply compute-state colour coding to individual rows.
 *
 * @see SimulationTreeTableNode
 * @see ResultsTreeTableNode
 */
class IconTableCellRenderer
		implements TreeCellRenderer {
	/**
	 * The default TreeCellRenderer that handles all base rendering before icon and colour overrides are applied.
	 */
	private TreeCellRenderer _defTreeRenderer;

	/**
	 * Icon displayed on SimulationTreeTableNode rows; represents a WAT computation alternative.
	 */
	private ImageIcon _watIcon;

	/**
	 * Icon displayed on ResultsTreeTableNode rows; represents a tabulated results entry.
	 */
	private ImageIcon _resultsIcon;

	/**
	 * The RmaJXTreeTable that owns this renderer; used to retrieve per-row foreground colours.
	 */
	private RmaJXTreeTable _treeTable;

	/**
	 * Constructs an IconTableCellRenderer that wraps the given default renderer and
	 * loads the WAT computation and results icon images from the application resource paths.
	 *
	 * @param treeTable   the RmaJXTreeTable that owns this renderer; used for row foreground lookups
	 * @param defRenderer the default TreeCellRenderer to delegate base rendering to
	 */
	public IconTableCellRenderer(RmaJXTreeTable treeTable, TreeCellRenderer defRenderer) {
		super();
		_defTreeRenderer = defRenderer;
		_treeTable = treeTable;

		// Load the icon images once at construction to avoid repeated resource lookups during rendering
		_watIcon = RmaImage.getImageIcon("Images/comp16x16.gif");
		_resultsIcon = RmaImage.getImageIcon("Images/tabulate18.gif");
	}

	/**
	 * {@inheritDoc}
	 *
	 * Renders the tree cell by delegating to the default renderer, then applies a
	 * node-type-specific icon and, for unselected rows, the per-row foreground colour
	 * provided by the owning tree-table. Exceptions are caught and printed to allow
	 * the tree to continue rendering other rows without interruption.
	 *
	 * @param tree     the JTree being rendered
	 * @param value    the tree node value being rendered
	 * @param selected true if the cell is currently selected
	 * @param expanded true if the node is currently expanded
	 * @param isLeaf   true if the node has no children
	 * @param row      the zero-based row index of the cell within the tree
	 * @param hasFocus true if the cell currently has keyboard focus
	 * @return the configured Component to display for this cell
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
	                                              boolean selected, boolean expanded, boolean isLeaf, int row,
	                                              boolean hasFocus) {
		Component comp = null;

		try {
			// Delegate all base rendering (text, highlight, expand indicator) to the default renderer
			comp = _defTreeRenderer.getTreeCellRendererComponent(tree, value,
					selected, expanded, isLeaf, row, hasFocus);

			if (comp instanceof JLabel) {
				JLabel label = (JLabel) comp;

				// isGroup is available for potential subclass use; leaf nodes are simulation members
				boolean isGroup = !isLeaf;

				// Apply the appropriate icon based on the concrete node type
				if (value instanceof SimulationTreeTableNode) {
					label.setIcon(_watIcon);
				} else if (value instanceof ResultsTreeTableNode) {
					label.setIcon(_resultsIcon);
				}

				// Make the label opaque so the background colour is painted correctly
				label.setOpaque(true);

				if (!selected) {
					// Override the foreground with the tree-table's per-row colour for compute-state coding
					Color fg = _treeTable.getRowForeground(row);
					label.setForeground(fg);
				}
			}

			return comp;
		} catch (Exception e) {
			// Print but do not rethrow so a single failing cell does not break the entire tree render
			e.printStackTrace();
		}

		return comp;
	}
}
