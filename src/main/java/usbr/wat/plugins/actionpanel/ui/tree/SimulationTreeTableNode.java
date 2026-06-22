package usbr.wat.plugins.actionpanel.ui.tree;

import java.io.File;                                                            // Provides File for listing and sorting the results subdirectories on disk
import java.util.ArrayList;                                                     // Provides ArrayList for building the ancestor list in getPath and the results name list
import java.util.Arrays;                                                        // Provides Arrays for sorting the results directory array and formatting member arrays in tooltips
import java.util.Collections;                                                   // Provides Collections for reversing the ancestor list from leaf-to-root into root-to-leaf order
import java.util.List;                                                          // Provides the List interface for ordered collections of TreeNode, ModelAlternative, and String

import javax.swing.JPopupMenu;                                                  // Provides JPopupMenu for the addPopupMenuItems contract (no items added by this node type)
import javax.swing.tree.TreeNode;                                               // Provides TreeNode for walking the parent chain when constructing the TreePath
import javax.swing.tree.TreePath;                                               // Provides TreePath for representing the path from the root to this node in the tree hierarchy

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;              // Provides AbstractMutableTreeTableNode, the SwingX base class for mutable tree-table nodes
import org.jdesktop.swingx.treetable.TreeTableNode;                             // Provides TreeTableNode for accessing typed child APIs when iterating over this node's children

import com.rma.io.FileManagerImpl;                                              // Provides FileManagerImpl for checking existence of and obtaining handles to the results directory
import com.rma.io.RmaFile;                                                      // Provides RmaFile for listing child files within the results parent directory

import hec2.plugin.model.ModelAlternative;                                      // Provides ModelAlternative for listing the programs and alternative names shown in the tooltip
import hec2.wat.model.WatSimulation;                                            // Provides WatSimulation for the WAT simulation this node represents and its run directory
import rma.util.RMAIO;                                                          // Provides RMAIO for path concatenation and boolean parsing utilities

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                          // Provides ActionPanelPlugin for accessing the singleton plugin and its current simulation group
import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction;        // Provides SaveSimulationResultsAction for the RESULTS_DIR constant naming the results subdirectory
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;              // Provides AbstractSimulationGroup for the simGroup parameter type in the tooltip builder
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;                  // Provides BaseComputeSettings for retrieving ensemble member settings shown in the tooltip
import usbr.wat.plugins.actionpanel.model.ComputeType;                          // Provides ComputeType for displaying the compute type (Standard, Ensemble, etc.) in the tooltip
import usbr.wat.plugins.actionpanel.model.ResultsData;                          // Provides ResultsData for loading and holding results metadata from a folder on disk
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                      // Provides SimulationGroup for accessing compute type and member settings in the tooltip

/**
 * A mutable tree-table node representing a WAT simulation in the Actions simulation
 * tree-table. This node is a direct child of the invisible root SimulationTreeTableNode
 * and may have ResultsTreeTableNode children loaded from disk at construction time.
 *
 * On construction, the node scans the simulation's run directory for results
 * subdirectories under SaveSimulationResultsAction.RESULTS_DIR, loads each one as a
 * ResultsData object, and adds a ResultsTreeTableNode child for each valid folder found.
 *
 * The node exposes four columns aligned to SimulationTreeTableModel:
 * SELECTED_COLUMN        — a boolean checkbox tracking the user's selection state.
 * SIMULATION_COLUMN      — the WatSimulation object (rendered as its name).
 * DISPLAY_IN_MAPS_COLUMN — the fixed string "Display In Map" (button trigger).
 * VIEW_REPORT_COLUMN     — the fixed string "View" (button trigger).
 *
 * The tooltip displays the simulation's description, the list of model alternatives
 * with their program and name, and (for SimulationGroup instances) the compute type
 * and ensemble members to compute.
 *
 * This node contributes no items to the right-click popup menu; context actions for
 * simulations are provided by the WAT simulation's own popup menu in SimulationTreeTable.
 *
 * @see SimulationTreeTableModel
 * @see ResultsTreeTableNode
 * @see ActionsTreeTableNode
 */
public class SimulationTreeTableNode extends AbstractMutableTreeTableNode
		implements ActionsTreeTableNode {
	/**
	 * The tree-table model that owns this node; used for firing change notifications.
	 */
	private SimulationTreeTableModel _treeModel;

	/**
	 * Column headers shared with SimulationTreeTableModel; used for column count validation.
	 */
	private String[] _headers;

	/**
	 * The WAT simulation this node represents; null only for the invisible root node.
	 */
	private WatSimulation _sim;

	/**
	 * The current value of the "Selected" checkbox column for this node. Stored as a
	 * primitive boolean and returned as a Boolean wrapper via getValueAt.
	 */
	private boolean _selected;

	/**
	 * Constructs a new SimulationTreeTableNode for the given simulation within the
	 * given tree-table model. Automatically scans the simulation's run directory for
	 * existing results folders and adds a ResultsTreeTableNode child for each one found.
	 *
	 * @param model the SimulationTreeTableModel that owns this node; may be null for the root
	 * @param sim   the WatSimulation this node represents; null only for the invisible root node
	 */
	public SimulationTreeTableNode(SimulationTreeTableModel model, WatSimulation sim) {
		super(sim);
		_treeModel = model;
		_sim = sim;

		// Reuse the shared header array from the model to keep column counts consistent
		_headers = SimulationTreeTableModel._headers;

		// Populate child ResultsTreeTableNodes from existing on-disk results folders
		addResultsNodes();
	}

	/**
	 * Scans the simulation's results directory for subdirectories and adds a
	 * ResultsTreeTableNode child for each valid results folder found. Does nothing
	 * when the simulation is null or the results directory does not exist.
	 */
	private void addResultsNodes() {
		if (_sim == null) {
			return;
		}

		// Build the absolute path to the results parent directory for this simulation
		String runDir = _sim.getRunDirectory();
		String resultsParentDir = RMAIO.concatPath(runDir, SaveSimulationResultsAction.RESULTS_DIR);

		// Abort early if the results directory has not yet been created
		if (!FileManagerImpl.getFileManager().fileExists(resultsParentDir)) {
			return;
		}

		RmaFile resultsDir = FileManagerImpl.getFileManager().getFile(resultsParentDir);
		File[] resultsKids = resultsDir.listFiles();

		// Sort alphabetically so results nodes appear in consistent order
		Arrays.sort(resultsKids);

		if (resultsKids != null) {
			for (int i = 0; i < resultsKids.length; i++) {
				// Only process subdirectories; ignore any stray files in the results folder
				if (resultsKids[i].isDirectory()) {
					addResultsFolder(resultsKids[i]);
				}
			}
		}
	}

	/**
	 * Loads a single results folder from a File reference, creates a ResultsData object
	 * from it, and adds a ResultsTreeTableNode child if the data loads successfully.
	 * Fires node-inserted and node-structure-changed events on the tree model after adding.
	 *
	 * @param resultsFolder the File representing the results subdirectory to load
	 */
	private void addResultsFolder(File resultsFolder) {
		String absPath = resultsFolder.getAbsolutePath();
		ResultsData resultsData = new ResultsData(_sim, absPath);

		// Only add a child node when the results folder contains valid loadable data
		if (resultsData.loadDataFromFolder(absPath)) {
			ResultsTreeTableNode kidNode = new ResultsTreeTableNode(resultsData);
			add(kidNode);

			// Notify the model that the new node was inserted and the structure has changed
			_treeModel.fireNodeInserted(kidNode);
			_treeModel.fireNodeStructureChanged(this);
		}
	}

	/**
	 * Searches this node's children for a ResultsTreeTableNode whose ResultsData
	 * matches the given instance by object identity, removes it, and fires the
	 * appropriate tree model events.
	 *
	 * @param results the ResultsData whose corresponding child node should be removed
	 */
	public void removeResultsFor(ResultsData results) {
		String absPath = results.getFolder();
		int kidCount = getChildCount();

		for (int i = 0; i < kidCount; i++) {
			TreeTableNode child = getChildAt(i);

			if (child instanceof ResultsTreeTableNode) {
				ResultsTreeTableNode rnode = (ResultsTreeTableNode) child;

				// Match by object identity to locate the exact results entry to remove
				if (rnode.getResultsData() == results) {
					remove(rnode);

					// Notify the model that the node was removed and the structure has changed
					_treeModel.fireNodeRemoved(rnode);
					_treeModel.fireNodeStructureChanged(this);
					return;
				}
			}
		}
	}

	/**
	 * Loads a results folder from the given directory path string and adds it as a
	 * child ResultsTreeTableNode. Convenience overload of addResultsFolder(File) that
	 * accepts a path string.
	 *
	 * @param currentResultsDir the absolute path string of the results folder to load
	 */
	public void addResultsFolder(String currentResultsDir) {
		RmaFile resultsDir = FileManagerImpl.getFileManager().getFile(currentResultsDir);
		addResultsFolder(resultsDir);
	}

	/**
	 * Returns the number of columns in this node's data model, equal to the length
	 * of the shared header array.
	 *
	 * @return the total column count for this node
	 */
	@Override
	public int getColumnCount() {
		return _headers.length;
	}

	/**
	 * Returns the display value for the given column. Returns an empty string when
	 * the simulation is null (root node). For valid nodes, returns the boolean selected
	 * state, the WatSimulation object, or fixed button label strings as appropriate.
	 * Returns null for unrecognised column indices.
	 *
	 * @param column the zero-based column index as defined in SimulationTreeTableModel
	 * @return the column value; never null for recognised columns when the simulation is set
	 */
	@Override
	public Object getValueAt(int column) {
		if (_sim == null) {
			return "";
		}

		WatSimulation sim = _sim;

		switch (column) {
			case SimulationTreeTableModel.SELECTED_COLUMN:
				// Return the current checkbox state as a Boolean wrapper
				return _selected;
			case SimulationTreeTableModel.SIMULATION_COLUMN:
				// Return the WatSimulation object; the renderer displays its name
				return sim;
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
	 * Sets the value of the given column. Guards against out-of-bounds indices and
	 * normalises null values to empty strings before dispatching. Only the
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
	 * Sets the SimulationTreeTableModel that this node belongs to. Used when the root
	 * node is created before the model is available and needs to be wired up afterwards.
	 *
	 * @param simulationTreeTableModel the model to associate with this node
	 */
	public void setSimulationTreeModel(SimulationTreeTableModel simulationTreeTableModel) {
		_treeModel = simulationTreeTableModel;
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
	 * Returns the WatSimulation object this node represents.
	 *
	 * @return the WatSimulation for this node; null only for the invisible root node
	 */
	public WatSimulation getSimulation() {
		return _sim;
	}

	/**
	 * Returns a tooltip for this node using the plugin's currently active simulation
	 * group as context for compute type and member information. Delegates to
	 * getToolTipText(AbstractSimulationGroup).
	 *
	 * @return an HTML tooltip string summarising the simulation's metadata, or null
	 * if this node has no simulation
	 */
	public String getToolTipText() {
		return getToolTipText(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());
	}

	/**
	 * Builds and returns an HTML tooltip for this simulation node. The tooltip includes
	 * the simulation's description (when non-empty), a list of model alternatives
	 * with their program name and alternative name, and (for SimulationGroup instances)
	 * the compute type and, for non-standard types, the array of ensemble members to compute.
	 *
	 * @param simGroup the AbstractSimulationGroup providing compute type and member context;
	 *                 cast to SimulationGroup for compute-settings access
	 * @return a trimmed HTML tooltip string, or null if this node's simulation is null
	 */
	public String getToolTipText(AbstractSimulationGroup simGroup) {
		if (_sim != null) {
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
			StringBuilder tip = new StringBuilder();
			tip.append("<html>");

			// Prepend the simulation description when one has been provided
			String desc = _sim.getDescription();
			if (desc != null && !desc.isEmpty()) {
				tip.append("<b>Description: </b>");
				tip.append(desc);
				tip.append("<br>");
			}

			// List each model alternative as "Program : AlternativeName"
			tip.append("<b>Models</b><br>");
			ModelAlternative modelAlt;
			for (int i = 0; i < modelAlts.size(); i++) {
				modelAlt = modelAlts.get(i);
				if (modelAlt != null) {
					tip.append(modelAlt.getProgram());
					tip.append(" : ");
					tip.append(modelAlt.getName());
					tip.append("<br>");
				}
			}

			// For SimulationGroup contexts, append the compute type and ensemble member details
			if (simGroup instanceof SimulationGroup) {
				SimulationGroup simulationGroup = (SimulationGroup) simGroup;
				ComputeType computeType = simulationGroup.getComputeType(_sim.getName());
				tip.append("<b>Compute Type: </b>");
				tip.append(computeType.toString());

				// Only show member details when the compute type is non-standard (e.g. ensemble)
				if (computeType != ComputeType.Standard) {
					BaseComputeSettings computeSettings = simulationGroup.getComputeSettings(_sim.getName(), computeType);
					tip.append("<br><b>Members to Compute: </b>");
					tip.append(Arrays.toString(computeSettings.getMembersToCompute()));
				}
			}

			tip.append("</html>");
			return tip.toString().trim();
		}
		return null;
	}

	/**
	 * Contributes no items to the popup menu. SimulationTreeTableNode context actions
	 * are provided by the WAT simulation's own popup menu, assembled in SimulationTreeTable.
	 *
	 * @param popup the JPopupMenu to which items would be added (none added here)
	 */
	@Override
	public void addPopupMenuItems(JPopupMenu popup) {
		// No node-specific popup items for simulation nodes; actions come from the WAT popup
	}

	/**
	 * Returns the string representations of all child nodes of this simulation node,
	 * used by ResultsTreeTableNode to obtain the list of existing result names for
	 * duplicate detection during a rename operation.
	 *
	 * @return a List of toString() values for each child node; empty if there are no children
	 */
	public List<String> getResultsNames() {
		List<String> resultsNames = new ArrayList<>();
		int childCnt = getChildCount();

		for (int i = 0; i < childCnt; i++) {
			TreeTableNode child = getChildAt(i);

			// Use the node's default toString which returns the results name
			resultsNames.add(child.toString());
		}
		return resultsNames;
	}

	/**
	 * Returns the SimulationTreeTableModel that owns this node. Used by child nodes
	 * (ResultsTreeTableNode) to fire model change events after rename operations.
	 *
	 * @return the SimulationTreeTableModel registered on this node
	 */
	public SimulationTreeTableModel getTreeTableModel() {
		return _treeModel;
	}
}
