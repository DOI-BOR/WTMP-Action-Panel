/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.rma.client.Browser;
import com.rma.event.ProjectManagerListener;
import com.rma.factories.ProjectNodeFactory;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import com.rma.ui.AbstractContainerNode;
import com.rma.ui.IconNode;

import rma.swing.RmaImage;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.SimulationGroupNode;

/**
 * @author mark
 * class to display the Simulation Groups in the Study Tree
 */
@SuppressWarnings("serial")
public class SimGroupContainerNode extends AbstractContainerNode
	implements IconNode
{
	private static ImageIcon _folderIcon;
	static
	{
		_folderIcon = RmaImage.getImageIcon("Images/simGroupContainer.png");
	}
	public SimGroupContainerNode()
	{
		super("Simulation Groups");
		addManagerListener();
		addNoGroupNode();
	}
	/**
	 * 
	 */
	private void addManagerListener()
	{
		Project.getCurrentProject().addManagerListener(new ProjectManagerListener()
		{

			@Override
			public void managerAdded(ManagerProxy mgr)
			{
			}

			@Override
			public void managerDeleted(ManagerProxy proxy)
			{
				MutableTreeNode node = findNodeForManager(proxy);
				if ( node != null )
				{
					int idx = getIndex(node);
					remove(idx);
					Browser.getBrowserFrame().getProjectTree().nodesWereRemoved(SimGroupContainerNode.this, new TreeNode[] {node});
				}
			}

			@Override
			public Class getManagerClass()
			{
				return SimulationGroup.class;
			}
			
		});
	}
	/**
	 * 
	 */
	private void addNoGroupNode()
	{
		SimulationGroup  simGroup = new SimulationGroup()
		{
			@Override
			public boolean readData()
			{
				return true;
			}
			@Override
			public boolean isReadOnly()
			{
				return false;
			}
			@Override
			public boolean isModified()
			{
				return false;
			}
		};
		simGroup.setName("Not in a Group");
		simGroup.setIsTransitory(true);
		simGroup.setIgnoreModifiedEvents(true);
		Project.getCurrentProject().addManager(simGroup);
		MutableTreeNode node = ProjectNodeFactory.getProjectNode(simGroup, this);
		
		if ( node instanceof SimulationGroupNode )
		{
			SimulationGroupNode sgNode = (SimulationGroupNode) node;
			sgNode.setAddSimsNotInGroup();
			sgNode.setManagerProxy(Project.getCurrentProject().getManagerProxy(simGroup));
			add(node);
		}
	}
	@Override
	public String getManagerType()
	{
		return SimulationGroup.class.getName();
	}
	
	@Override
	public void addManager(ManagerProxy proxy)
	{
		super.addManager(proxy);
	}
	@Override
	public Icon getIcon()
	{
		return _folderIcon;
	}

}
