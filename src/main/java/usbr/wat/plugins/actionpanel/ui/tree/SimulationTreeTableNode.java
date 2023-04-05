/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatSimulation;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;
import usbr.wat.plugins.actionpanel.model.ComputeType;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author mark
 *
 */
public class SimulationTreeTableNode extends AbstractMutableTreeTableNode
	implements ActionsTreeTableNode
{
	private SimulationTreeTableModel _treeModel;
	private String[] _headers;
	private WatSimulation _sim;
	private boolean _selected;

	public SimulationTreeTableNode(SimulationTreeTableModel model, WatSimulation sim)
	{
		super(sim);
		_treeModel = model;
		_sim = sim;
		_headers = SimulationTreeTableModel._headers;
		addResultsNodes();
	}
	/**
	 * 
	 */
	private void addResultsNodes()
	{
		if ( _sim == null )
		{
			return;
		}
		String runDir = _sim.getRunDirectory();
		String resultsParentDir = RMAIO.concatPath(runDir, SaveSimulationResultsAction.RESULTS_DIR);
		if ( !FileManagerImpl.getFileManager().fileExists(resultsParentDir))
		{
			return;
		}
		RmaFile resultsDir = FileManagerImpl.getFileManager().getFile(resultsParentDir);
		File[] resultsKids = resultsDir.listFiles();
		Arrays.sort(resultsKids);
		if ( resultsKids != null )
		{
			for (int i = 0;i < resultsKids.length;i++ )
			{
				if ( resultsKids[i].isDirectory())
				{
					addResultsFolder(resultsKids[i]);
				}
			}
		}
	}
	/**
	 * @param resultsFolder
	 */
	private void addResultsFolder(File resultsFolder)
	{
		String absPath = resultsFolder.getAbsolutePath();
		ResultsData resultsData = new ResultsData(_sim, absPath);
		if ( resultsData.loadDataFromFolder(absPath))
		{
			ResultsTreeTableNode kidNode = new ResultsTreeTableNode(resultsData);
			add(kidNode);
			_treeModel.fireNodeInserted(kidNode);
			_treeModel.fireNodeStructureChanged(this);
			
		}
	}
	/**
	 * @param results
	 */
	public void removeResultsFor(ResultsData results)
	{
		String absPath = results.getFolder();
		int kidCount = getChildCount();
		for (int i = 0;i < kidCount; i++ )
		{
			TreeTableNode child = getChildAt(i);
			if ( child instanceof ResultsTreeTableNode )
			{
				ResultsTreeTableNode rnode = (ResultsTreeTableNode) child;
				if ( rnode.getResultsData() == results )
				{
					remove(rnode);
					_treeModel.fireNodeRemoved(rnode);
					_treeModel.fireNodeStructureChanged(this);
					return;
				}
			}
			
		}
	}
	/**
	 * @param currentResultsDir
	 */
	public void addResultsFolder(String currentResultsDir)
	{
		RmaFile resultsDir = FileManagerImpl.getFileManager().getFile(currentResultsDir);
		addResultsFolder(resultsDir);
	}
	@Override
	public int getColumnCount()
	{
		return _headers.length;
	}

	@Override
	public Object getValueAt(int column)
	{
		if ( _sim == null )
		{
			return "";
		}
		WatSimulation sim = _sim;

		switch (column)
		{
			case SimulationTreeTableModel.SELECTED_COLUMN:
				return _selected;
			case SimulationTreeTableModel.SIMULATION_COLUMN:
				return sim;
			case SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN:
				return "Display In Map";
			case SimulationTreeTableModel.VIEW_REPORT_COLUMN:
				return "View";
		}
		return null;
	}
	@Override
	public void setValueAt(Object obj, int col)
	{
		if (col < 0 || col >= _headers.length)
		{
			return;
		}
		if (obj == null)
		{
			obj = "";
		}
		
		switch (col)
		{
			case SimulationTreeTableModel.SELECTED_COLUMN:
				 _selected = RMAIO.parseBoolean(obj.toString(), false);
				 break;
			case SimulationTreeTableModel.SIMULATION_COLUMN:
				break;
			case SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN:
				break;
			case SimulationTreeTableModel.VIEW_REPORT_COLUMN:
				break;
		}
	}
	/**
	 * @param simulationTreeTableModel
	 */
	public void setSimulationTreeModel(SimulationTreeTableModel simulationTreeTableModel)
	{
		_treeModel = simulationTreeTableModel;
	}
	
	/**
	 * get the TreePath to the root node for the TreeNode
	 * @return the TreePath to root
	 */
	public TreePath getPath() 
	{
	    List<TreeNode> list = new ArrayList<>();
	    TreeNode node = this;
	    while (node != null) 
	    {
	        list.add(node);
	        node = node.getParent();
	    }
	    Collections.reverse(list);

	    return new TreePath(list.toArray());
	}
	/**
	 * @return
	 */
	public WatSimulation getSimulation()
	{
		return _sim;
	}
	public String getToolTipText()
	{
		return getToolTipText(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());
	}
	/**
	 * @param simGroup
	 * @return
	 */
	public String getToolTipText(AbstractSimulationGroup simGroup)
	{
		if ( _sim != null )
		{
			
			List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
			StringBuilder tip = new StringBuilder();
			tip.append("<html>");
			String desc = _sim.getDescription();
			if ( desc != null && !desc.isEmpty() )
			{
				tip.append("<b>Description: </b>");
				tip.append(desc);
				tip.append("<br>");
			}
			tip.append("<b>Models</b><br>");
			ModelAlternative modelAlt;
			for (int i = 0;i < modelAlts.size();i++)
			{
				modelAlt = modelAlts.get(i);
				if ( modelAlt != null )
				{
					tip.append(modelAlt.getProgram());
					tip.append(" : ");
					tip.append(modelAlt.getName());
					tip.append("<br>");
				}
			}
			if ( simGroup instanceof SimulationGroup )
			{
				SimulationGroup simulationGroup = (SimulationGroup) simGroup;
				ComputeType computeType = simulationGroup.getComputeType(_sim.getName());
				tip.append("<b>Compute Type: </b>");
				tip.append(computeType.toString());
				if ( computeType != ComputeType.Standard )
				{
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
	@Override
	public void addPopupMenuItems(JPopupMenu popup)
	{
	}
	/**
	 * @return
	 */
	public List<String> getResultsNames()
	{
		List<String>resultsNames = new ArrayList<>();
		int childCnt = getChildCount();
		for(int i = 0;i < childCnt; i++ )
		{
			TreeTableNode child = getChildAt(i);
			resultsNames.add(child.toString());
		}
		return resultsNames;
		
	}
	
	public SimulationTreeTableModel getTreeTableModel()
	{
		return _treeModel;
	}

	
}
