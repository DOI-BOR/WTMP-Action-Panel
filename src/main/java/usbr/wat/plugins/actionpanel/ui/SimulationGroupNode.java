/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.rma.client.Browser;
import com.rma.editors.DataEditor;
import com.rma.event.ProjectManagerListener;
import com.rma.factories.ProjectNodeFactory;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import com.rma.ui.DefaultContentNode;
import com.rma.ui.HasContentNode;
import com.rma.ui.IconNode;
import com.rma.ui.ManagerNode;
import com.rma.ui.PopupMenuTreeNode;
import com.rma.ui.ProjectPaneActionNode;
import com.rma.ui.ProjectTree;

import hec.heclib.util.HecTime;
import hec.model.RunTimeWindow;

import hec2.wat.client.WatMessages;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;
import hec2.wat.ui.WatAnalysisPeriodNode;
import hec2.wat.ui.WatSimulationNode;
import hec2.wat.util.WatI18n;

import rma.swing.RmaImage;
import usbr.wat.plugins.actionpanel.SimGroupContainerNode;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class SimulationGroupNode extends ManagerNode
	implements HasContentNode, PopupMenuTreeNode, 
	ActionListener, ProjectPaneActionNode,  IconNode
{
	private static ImageIcon _folderIcon;
	static
	{
		_folderIcon = RmaImage.getImageIcon("Images/compMulti16x16.gif");
	}
	private boolean _showCnt;
	private ProjectTree _tree;

	public SimulationGroupNode()
	{
		super("Simulation Group");
		addManagerListener();
		
	}
	public SimulationGroupNode(Object obj)
	{
		super("");
		setUserObject(obj);
		addManagerListener();
	}
	
	
	/**
	 * 
	 */
	private void addManagerListener()
	{
		Project.getCurrentProject().addManagerListener(new ProjectManagerListener()
		{

			@Override
			public void managerAdded(ManagerProxy proxy)
			{
			}

			@Override
			public void managerDeleted(ManagerProxy proxy)
			{
				if ( Browser.getBrowserFrame().getProjectTree().getSelectedNode() == SimulationGroupNode.this )
				{
					Browser.getBrowserFrame().getProjectTree().updateContentTree();
				}
			}

			@Override
			public Class getManagerClass()
			{
				return WatSimulation.class;
			}
			
		});
	}
	@Override
	public void setManagerProxy(ManagerProxy proxy)
	{
		super.setManagerProxy(proxy);
		if ( Boolean.getBoolean("SimGroupNode.HasSimulations"))
		{
			EventQueue.invokeLater(()->addSimulations());
		}
	}
	private void addSimulations()
	{
		SimulationGroup simGroup = (SimulationGroup) getManager();
		if ( simGroup != null )
		{
			List<WatSimulation> sims = simGroup.getSimulations();
			addSimulations(sims);
		}
	}
	private void addSimulations(List<WatSimulation>sims)
	{
		removeAllChildren();
		WatSimulation sim;
		for (int i = 0;i < sims.size(); i++ )
		{ 
			sim = sims.get(i);
			addSimulation(sim);
		}
	}
	/**
	 * @param sim
	 * @return 
	 */
	private MutableTreeNode addSimulation(WatSimulation sim)
	{
		MutableTreeNode node = ProjectNodeFactory.getProjectNode(sim, this);
		if ( node instanceof ManagerNode )
		{
			ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(sim);
			((ManagerNode)node).setManagerProxy(proxy);
			add(node);
			return node;
		}
		return null;
	}

	@Override
	public void add(MutableTreeNode node)
	{
		super.add(node);
		_tree.nodesWereInserted(this, new TreeNode[]{node});
		_tree.nodeChanged(this);
		if (this.shouldAlwaysExpand())
		{
			_tree.expandNode(this);
		}
	}

	public List getContentNodes()
	{
		List contentNodes = new ArrayList<>();
		SimulationGroup simGroup = (SimulationGroup) getManager();
		if ( simGroup.isTransitory() )
		{
			int cnt = getChildCount();
			WatSimulation sim;
			TreeNode node;
			WatSimulationNode simNode;
			for (int i = 0;i < cnt; i++ )
			{
				node = getChildAt(i);
				if ( node instanceof WatSimulationNode )
				{
					simNode = (WatSimulationNode) node;
					sim = simNode.getSimulation();
					node = ProjectNodeFactory.getProjectNode(sim, this);
					if ( node instanceof WatSimulationNode )
					{
						simNode = (WatSimulationNode) node;
						simNode.setManagerProxy(Project.getCurrentProject().getManagerProxy(sim));
						contentNodes.add(node);
					}
				}	
			}
		}
		WatAnalysisPeriod ap = simGroup.getAnalysisPeriod();
		ManagerProxy apProxy = Project.getCurrentProject().getManagerProxy(ap);
		if ( apProxy != null )
		{
			WatAnalysisPeriodNode apNode = new WatAnalysisPeriodNode(apProxy,true);
			apNode.setAddEventNodes(false);
			apNode.setInContentTree(true);
			apNode.setManagerProxy(apProxy);
			contentNodes.add(apNode);
		}
		MutableTreeNode node;
		if ( ap != null )
		{
			DefaultContentNode cnode = new DefaultContentNode( WatI18n.getI18n(
				WatMessages.SIMULATION_NODE_TIME_WINDOW_NODE).getText());
			cnode.setIcon(RmaImage.getImageIcon("Images/clock.gif"));
			cnode.setIsLeaf(false);
			RunTimeWindow rtw = ap.getRunTimeWindow();
			HecTime start = rtw.getStartTime();
			HecTime end   = rtw.getEndTime();
			DefaultContentNode startNode = new DefaultContentNode(
				WatI18n.getI18n(
					WatMessages.SIMULATION_NODE_START_TIME_NODE).format(start));
			startNode.setIcon(RmaImage.getImageIcon("Images/green-ball.gif"));
			cnode.add(startNode);
			DefaultContentNode endNode = new DefaultContentNode(
				WatI18n.getI18n(
					WatMessages.SIMULATION_NODE_END_TIME_NODE).format(end));
			endNode.setIcon(RmaImage.getImageIcon("Images/red-ball.gif"));
			cnode.add(endNode);
			contentNodes.add(cnode);
		}
		List<WatSimulation> sims = simGroup.getSimulations();
		WatSimulation sim;
		for (int i = 0;i < sims.size(); i++ )
		{
			sim = sims.get(i);
			node = ProjectNodeFactory.getProjectNode(sim, this);
			if ( node instanceof WatSimulationNode )
			{
				WatSimulationNode simNode = (WatSimulationNode) node;
				simNode.setManagerProxy(Project.getCurrentProject().getManagerProxy(sim));
				contentNodes.add(node);
			}
		}
		
		return contentNodes;
	}
	@Override
	public boolean shouldExpandContentTree()
	{
		return true;
	}
	

	@Override
	public Icon getIcon()
	{
		return _folderIcon;
	}

	@Override
	public DataEditor getEditor()
	{
		return null;
	}
	public List<WatSimulation>getSimulations()
	{
		SimulationGroup simGroup = (SimulationGroup) getManager();
		return simGroup.getSimulations();
	}
	/**
	 * 
	 */
	public void setAddSimsNotInGroup()
	{
		EventQueue.invokeLater(()->buildNode());
	}
	/**
	 * @return
	 */
	private void buildNode()
	{
		addNotInGroupManagerListener();
		
		EventQueue.invokeLater(()->addSimsNotInGroup());
	}
	private void addSimsNotInGroup()
	{
		if ( Project.getCurrentProject().isNoProject())
		{
			return;
		}
		List<WatSimulation> allSims = Project.getCurrentProject().getManagerListForType(WatSimulation.class);
		List<SimulationGroup> simGroups = Project.getCurrentProject().getManagerListForType(SimulationGroup.class);
		int cnt = simGroups.size();
		List<WatSimulation> sgSims;
		TreeNode parentNode = getParent();
		SimGroupContainerNode  containerParent = null;
		if ( parentNode instanceof SimGroupContainerNode )
		{
			containerParent = (SimGroupContainerNode) parentNode;
		}
		SimulationGroup simGrp;
		boolean addedNodes = false;
		for (int i = 0; i < cnt; i++ ) // first child should be this
		{
			simGrp = simGroups.get(i);
			if ( simGrp == getSimulationGroup())
			{
				continue;
			}
			if ( containerParent != null )
			{
				if ( containerParent.checkAndAddSimGroup(simGrp))
				{
					addedNodes = true;
				}
			}
			sgSims = simGrp.getSimulations();
			allSims.removeAll(sgSims);
		}
		addSimulations(allSims);
		
	}
	
	/**
	 * @return
	 */
	private SimulationGroup getSimulationGroup()
	{
		return (SimulationGroup) getManager();
	}
	/**
	 * 
	 */
	private void addNotInGroupManagerListener()
	{
		Project.getCurrentProject().addManagerListener(new SimGroupProjectManagerListener());
	}
	/**
	 * @param sim
	 * @return
	 */
	protected TreeNode findSimNode(WatSimulation sim)
	{
		int childCnt = getChildCount();
		TreeNode node;
		WatSimulationNode simNode;
		for (int i = 0;i < childCnt; i++ )
		{
			node = getChildAt(i);
			if ( node instanceof WatSimulationNode )
			{
				simNode = (WatSimulationNode) node;
				if ( simNode.getSimulation() == sim )
				{
					return simNode;
				}
			}
		}
		return null;
	}
	/**
	 * @param b
	 */
	public void setShowCount(boolean showCount)
	{
		_showCnt = showCount;
	}
	
	
	@Override
	public String toString()
	{
		if ( _showCnt )
		{
			return super.toString()+" ("+getChildCount()+")";
		}
		return super.toString();
	}

	public void setTree(ProjectTree tree)
	{
		_tree = tree;
	}


	private class SimGroupProjectManagerListener implements ProjectManagerListener
	{
		@Override
		public void managerAdded(ManagerProxy proxy)
		{
			WatSimulation sim = (WatSimulation) proxy.getManager();

			int cnt = getParent().getChildCount();
			SimulationGroupNode sgNode;
			List<WatSimulation> sgSims;

			for (int i = 1; i < cnt; i++ ) // first child should be this
			{
				TreeNode node = getParent().getChildAt(i);
				if ( node instanceof SimulationGroupNode )
				{
					sgNode = (SimulationGroupNode) node;
					sgSims = sgNode.getSimulations();
					if ( sgSims.contains(sim))
					{
						return;
					}
				}
			}
			MutableTreeNode node = addSimulation(sim);
			if ( node != null )
			{
				add(node);
				_tree.nodesWereInserted(SimulationGroupNode.this, new TreeNode[] {node});
				_tree.nodeChanged(SimulationGroupNode.this);
				_tree.expandNode(SimulationGroupNode.this);
			}
		}

		@Override
		public void managerDeleted(ManagerProxy proxy)
		{
			WatSimulation sim = (WatSimulation) proxy.getManager();
			TreeNode simNode = findSimNode(sim);
			if ( simNode != null )
			{
				int idx = getIndex(simNode);
				remove(idx);
				_tree.nodesWereRemoved(SimulationGroupNode.this,
						new TreeNode[] {simNode});
			}
		}

		@Override
		public Class getManagerClass()
		{
			return WatSimulation.class;
		}
	}
	

}
