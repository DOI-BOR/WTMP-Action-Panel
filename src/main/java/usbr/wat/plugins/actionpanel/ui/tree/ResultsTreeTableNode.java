package usbr.wat.plugins.actionpanel.ui.tree;

import java.util.ArrayList;                                                 // Provides ArrayList for building the ancestor list when constructing the TreePath
import java.util.Collections;                                               // Provides Collections for reversing the ancestor list from leaf-to-root into root-to-leaf order
import java.util.Date;                                                      // Provides Date for formatting the last-computed timestamp displayed in the tooltip
import java.util.List;                                                      // Provides the List interface for the ordered ancestor node list used in getPath

import javax.swing.JMenuItem;                                               // Provides JMenuItem for the "Rename..." context menu item added by addPopupMenuItems
import javax.swing.JOptionPane;                                             // Provides JOptionPane for displaying the rename-failure error dialog
import javax.swing.JPopupMenu;                                              // Provides JPopupMenu for the context menu to which node-specific items are appended
import javax.swing.tree.TreeNode;                                           // Provides TreeNode for walking the parent chain when building the TreePath
import javax.swing.tree.TreePath;                                           // Provides TreePath for representing the path from the root to this node in the tree hierarchy

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;          // Provides AbstractMutableTreeTableNode, the SwingX base class for mutable tree-table nodes
import hec.gui.RenameDlg;                                                   // Provides RenameDlg, the HEC standard rename dialog used to collect a new name and description
import rma.util.RMAIO;                                                      // Provides RMAIO for boolean parsing, HTML string sanitisation, and string replacement utilities

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                      // Provides ActionPanelPlugin for accessing the singleton plugin instance and its actions window
import usbr.wat.plugins.actionpanel.model.ResultsData;                      // Provides ResultsData, the model object representing a set of simulation results held by this node

/**
 * A mutable tree-table node that represents a ResultsData entry in the Actions
 * simulation tree-table. Each node is a leaf child of a SimulationTreeTableNode
 * and exposes the results' name, description, creator, creation date, and last
 * computed timestamp via a multi-column cell model aligned to SimulationTreeTableModel.
 *
 * The node renders four columns:
 * SELECTED_COLUMN   — a boolean checkbox value tracking the user's selection state.
 * SIMULATION_COLUMN — the ResultsData object (rendered by the table as the results name).
 * DISPLAY_IN_MAPS_COLUMN — the fixed string "Display In Map" (triggers a button action).
 * VIEW_REPORT_COLUMN     — the fixed string "View" (triggers a button action).
 *
 * A right-click context menu item ("Rename...") is contributed via addPopupMenuItems,
 * which opens the HEC RenameDlg to collect a new name and description before delegating
 * the rename to ResultsData.renameTo and notifying the tree-table model.
 *
 * @see SimulationTreeTableNode
 * @see ResultsData
 * @see ActionsTreeTableNode
 */
public class ResultsTreeTableNode extends AbstractMutableTreeTableNode
		implements ActionsTreeTableNode {
	/**
	 * Column headers shared with SimulationTreeTableModel; used for column count validation.
	 */
	private String[] _headers;

	/**
	 * The ResultsData object this node represents; provides name, description, and timing metadata.
	 */
	private ResultsData _resultsData;

	/**
	 * The current value of the "Selected" checkbox column for this node. Stored as an
	 * Object to satisfy the generic tree-table value API; set as a parsed Boolean.
	 */
	private Object _selected;

	/**
	 * Constructs a new ResultsTreeTableNode wrapping the given ResultsData object.
	 * The node's user object is set to the ResultsData instance and the shared column
	 * headers are obtained from SimulationTreeTableModel.
	 *
	 * @param resultsData the ResultsData object this node represents; must not be null
	 */
	public ResultsTreeTableNode(ResultsData resultsData) {
		super(resultsData);
		_resultsData = resultsData;

		// Reuse the shared header array from the model to keep column counts consistent
		_headers = SimulationTreeTableModel._headers;
	}

	/**
	 * Returns the display value for the given column index. Returns an empty string
	 * when the underlying ResultsData is null. Fixed string labels are returned for
	 * the map and report button columns; the selected checkbox state and the ResultsData
	 * object are returned for their respective columns. Returns null for unrecognised indices.
	 *
	 * @param column the zero-based column index as defined in SimulationTreeTableModel
	 * @return the column value for display; never null for recognised columns when data is present
	 */
	@Override
	public Object getValueAt(int column) {
		if (_resultsData == null) {
			return "";
		}

		switch (column) {
			case SimulationTreeTableModel.SELECTED_COLUMN:
				// Return the current boolean checkbox state for this node
				return _selected;
			case SimulationTreeTableModel.SIMULATION_COLUMN:
				// Return the ResultsData object; the renderer displays its name
				return _resultsData;
			case SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN:
				// Fixed label rendered as a button trigger for the map display action
				return "Display In Map";
			case SimulationTreeTableModel.VIEW_REPORT_COLUMN:
				// Fixed label rendered as a button trigger for the report view action
				return "View";
		}
		return null;
	}

	/**
	 * Sets the value of the given column. Guards against out-of-bounds column indices
	 * and normalises null values to empty strings before dispatching. Only the
	 * SELECTED_COLUMN is writable; all other columns are effectively read-only.
	 *
	 * @param obj the new value to set; null is treated as an empty string
	 * @param col the zero-based column index as defined in SimulationTreeTableModel
	 */
	@Override
	public void setValueAt(Object obj, int col) {
		// Guard against out-of-bounds column indices
		if (col < 0 || col >= _headers.length) {
			return;
		}

		// Normalise null to empty string to simplify downstream handling
		if (obj == null) {
			obj = "";
		}

		switch (col) {
			case SimulationTreeTableModel.SELECTED_COLUMN:
				// Parse the incoming value as a boolean; default to false on parse failure
				_selected = RMAIO.parseBoolean(obj.toString(), false);
				break;
			case SimulationTreeTableModel.SIMULATION_COLUMN:
				// The simulation name column is not user-editable
				break;
			case SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN:
				// The map button column is not user-editable
				break;
			case SimulationTreeTableModel.VIEW_REPORT_COLUMN:
				// The report button column is not user-editable
				break;
		}
	}

	/**
	 * Constructs and returns the TreePath from the root of the tree to this node by
	 * walking the parent chain, reversing the collected ancestors from leaf-to-root
	 * order into root-to-leaf order, and wrapping the result in a TreePath.
	 *
	 * @return the TreePath from the root node to this node; never null
	 */
	public TreePath getPath() {
		List<TreeNode> list = new ArrayList<>();
		TreeNode node = this;

		// Walk up the parent chain, collecting each ancestor in leaf-to-root order
		while (node != null) {
			list.add(node);
			node = node.getParent();
		}

		// Reverse to produce root-to-leaf order as required by TreePath
		Collections.reverse(list);

		return new TreePath(list.toArray());
	}

	/**
	 * Returns the number of columns in this node's data model, equal to the length
	 * of the shared header array from SimulationTreeTableModel.
	 *
	 * @return the total column count for this node
	 */
	@Override
	public int getColumnCount() {
		return _headers.length;
	}

	/**
	 * Returns the ResultsData object wrapped by this node.
	 *
	 * @return the ResultsData this node represents; may be null if not yet initialised
	 */
	public ResultsData getResultsData() {
		return _resultsData;
	}

	/**
	 * Builds and returns an HTML tooltip string for this results node. The tooltip
	 * includes the results description (when non-empty), the creator's name, the
	 * creation date, and the last computed timestamp. HTML tags are stripped from
	 * the description before embedding to prevent malformed nested HTML.
	 *
	 * @return a non-null HTML tooltip string summarising the results metadata
	 */
	public String getToolTipText() {
		StringBuilder tip = new StringBuilder();
		tip.append("<html>");

		String desc = _resultsData.getDescription();

		// Only include the description section when one has been provided
		if (desc != null && !desc.isEmpty()) {
			tip.append("<b>Description:</b><br>");

			// Strip any existing HTML wrapper tags before embedding in the tooltip
			desc = RMAIO.toHtmlString(_resultsData.getDescription());
			desc = RMAIO.replace(desc, "<html>", "");
			desc = RMAIO.replace(desc, "</html>", "");
			tip.append(desc);
			tip.append("<br>");
		}

		// Append creator, creation date, and last computed timestamp
		tip.append("<b>Created by:</b> ");
		tip.append(_resultsData.getSavedBy());
		tip.append("<br><b>Created On:</b> ");
		tip.append(_resultsData.getSavedAt());
		tip.append("<br><b>Last Computed On:</b> ");
		tip.append(new Date(_resultsData.getLastComputedTime()));
		tip.append("</html>");

		return tip.toString().trim();
	}

	/**
	 * Adds a "Rename..." menu item to the given popup menu. When selected, the item
	 * opens the HEC RenameDlg to collect a new name and description for this results entry.
	 *
	 * @param popup the JPopupMenu to which the "Rename..." item is appended; must not be null
	 */
	public void addPopupMenuItems(JPopupMenu popup) {
		JMenuItem renameMenu = new JMenuItem("Rename...");
		renameMenu.addActionListener(e -> renameResults());
		popup.add(renameMenu);
	}

	/**
	 * Opens the HEC RenameDlg pre-populated with the current results name and description.
	 * Existing result names from the parent SimulationTreeTableNode are provided to the
	 * dialog to prevent duplicate name conflicts. On confirmation, delegates the rename to
	 * ResultsData.renameTo, updates the description, refreshes the user object, and
	 * notifies the tree-table model. Shows an error dialog if the rename operation fails.
	 *
	 * @return true if the rename was confirmed and succeeded; false if cancelled or failed
	 */
	private boolean renameResults() {
		SimulationTreeTableNode parent = (SimulationTreeTableNode) getParent();

		ResultsData results = getResultsData();

		// Obtain sibling result names from the parent to prevent duplicate renames
		List<String> resultsNames = parent.getResultsNames();

		// Open the rename dialog anchored to the plugin's main window
		RenameDlg dlg = new RenameDlg(ActionPanelPlugin.getInstance().getActionsWindow(),
				"Rename " + results.getName(), true);

		dlg.setName(results.getName());
		dlg.setDescription(results.getDescription());

		// Hide the file name field since results are identified by name only, not file path
		dlg.setFileNameVisible(false);
		dlg.setExistingNames(resultsNames);
		dlg.setVisible(true);

		// Abort without changes if the user cancelled the dialog
		if (dlg.getCanceled()) {
			return false;
		}

		if (results.renameTo(dlg.getName())) {
			// Rename succeeded: update description, user object, and notify the model
			results.setDescription(dlg.getDescription());
			setUserObject(results);
			parent.getTreeTableModel().fireNodeChanged(this);
			return true;
		}

		// Rename failed: show an informational error dialog
		JOptionPane.showMessageDialog(ActionPanelPlugin.getInstance().getActionsWindow(),
				"Faild to rename " + results.getName() + " to " + dlg.getName(),
				"Rename Failed", JOptionPane.INFORMATION_MESSAGE);
		return false;
	}
}
