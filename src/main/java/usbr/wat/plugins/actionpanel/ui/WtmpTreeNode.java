/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import javax.swing.SwingUtilities;
import javax.swing.tree.MutableTreeNode;

import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import com.rma.ui.ProjectTree;
import com.rma.ui.ProjectTreeNode;

import usbr.wat.plugins.actionpanel.SimGroupContainerNode;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class WtmpTreeNode extends ProjectTreeNode
{
	private WtmpTree _tree;
	/**
	 * @param currentProject
	 * @param projectTreeModel 
	 * @param object
	 */
	public WtmpTreeNode(Project currentProject, MutableTreeNode parent, WtmpTree tree)
	{
		super(currentProject, parent);
		_tree = tree;
	}
	@Override
	public void buildTree()
	{
		SwingUtilities.invokeLater(() ->
		{
			removeAllChildren();
			if ( getProject() == null )
			{
				return;
			}
			SimGroupContainerNode containerNode = new SimGroupContainerNode(_tree);
			add(containerNode);

			getProject().addManagerListener(this);
		});
	}
	
	@Override
	protected ProjectTree getProjectTree()
	{
		return _tree;
	}
	@Override
	public void managerAdded(ManagerProxy proxy)
	{
		if ( proxy == null )
		{
			return;
		}
		
		
	}
	@Override
	public void managerDeleted(ManagerProxy proxy)
	{
		if ( proxy == null )
		{
			return;
		}
	}
	
}
