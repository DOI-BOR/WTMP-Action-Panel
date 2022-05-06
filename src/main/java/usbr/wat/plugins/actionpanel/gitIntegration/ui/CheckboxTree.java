/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.rma.swing.tree.DefaultCheckBoxNode;

import rma.swing.tree.CheckBoxTreeRenderer;
import rma.swing.tree.NodeSelectionListener;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ListSubModulesAction;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class CheckboxTree extends JTree
{
	private DefaultMutableTreeNode _root;
	private RepoInfo _repo;
	private boolean _hasSubModules;

	public CheckboxTree(TreeModel model, StudyStorageDialog studyStorageDialog)
	{
		this(model, studyStorageDialog.getSelectedRepo());
	}
	
	public CheckboxTree(TreeModel model, RepoInfo repo)
	{
		super(model);
		_root = (DefaultMutableTreeNode) model.getRoot();
		_repo = repo;
		setRootVisible(false);
		setCellRenderer(new CheckBoxTreeRenderer());
		addMouseListener(new NodeSelectionListener(this));
		setToolTipText("");
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		Point pt = e.getPoint();
		TreePath path = getPathForLocation(pt.x, pt.y);
		if ( path == null )
		{
			return null;
		}
		SubModuleNode node = (SubModuleNode) path.getLastPathComponent();
		int commitsBehind = node.getCommitsBehind();
		if ( commitsBehind > 0 )
		{
			return node.getSubModuleName().concat(" is ").concat(String.valueOf(commitsBehind)).concat(" commit behind");
		}
		return node.getSubModuleName().concat(" is up to date");
	}
	/**
	 * @return
	 */
	public List<String> getCheckedSubmodules()
	{
		List<String> l = new ArrayList<>();
		getCheckedSubModules(l, _root);

		return l;
	}
	
	/**
	 * @param l
	 * @param root
	 */
	private void getCheckedSubModules(List<String> l, DefaultMutableTreeNode parent)
	{
		int cnt = parent.getChildCount();
		DefaultCheckBoxNode child;
		Object obj;
		for (int i = 0;i < cnt; i++ )
		{
			child = (DefaultCheckBoxNode) parent.getChildAt(i);
			if ( child.isSelected())
			{
				obj = child.getUserObject();
				if ( obj instanceof String)
				{
					l.add((String)obj);
				}
			}
			if ( !child.isLeaf())
			{
				getCheckedSubModules(l, child);
			}
			
		}
	}
	

	/**
	 * 
	 */
	public void fillSubModules()
	{
		ListSubModulesAction action = new ListSubModulesAction(SwingUtilities.windowForComponent(this), _repo);
		List<String>subModules = action.getSubModules();
		RepoInfo repo = _repo;
		if ( repo != null )
		{
			DefaultCheckBoxNode parent = new SubModuleNode(AbstractGitAction.STUDY_MODULE, this);
			parent.setSelected(true);
			_root.add(parent);
			_hasSubModules = !subModules.isEmpty();
			for (int i = 0; i < subModules.size();i++)
			{
				DefaultCheckBoxNode node = new SubModuleNode(subModules.get(i), this);
				node.setSelected(true);
				_root.add(node);
			}
			DefaultTreeModel treeModel = (DefaultTreeModel) getModel();
			treeModel.nodeStructureChanged(_root);
			expandAll(true);
		}
	}
	
	public void expandAll(boolean expand) 
	{
		TreeNode root = (TreeNode)this.getModel().getRoot();
		expandAll(new TreePath(root), expand);
	}

	public void expandAll(TreePath parent, boolean expand) 
	{
		// Traverse children
		TreeNode node = (TreeNode)parent.getLastPathComponent();
		if (node.getChildCount() >= 0) 
		{
			for (Enumeration e=node.children(); e.hasMoreElements(); ) 
			{
				TreeNode n = (TreeNode)e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(path, expand);
			}
		}
		// Expansion or collapse must be done bottom-up
		if (expand) 
		{
			this.expandPath(parent);
		} 
		else 
		{
			this.collapsePath(parent);
		}
	}

	/**
	 * @return
	 */
	public boolean hasSubModules()
	{
		return _hasSubModules;
	}
	public void setCommitsBehind(String submodule, int commitsBehind)
	{
		SubModuleNode node = findNode(submodule);
		if ( node != null )
		{
			node.setCommitsBehind(commitsBehind);
		}
	}
	
	/**
	 * @param submodule
	 * @return
	 */
	private SubModuleNode findNode(String submodule)
	{
		int count = _root.getChildCount();
		for (int i  = 0;i < count; i++ )
		{
			SubModuleNode node = (SubModuleNode) _root.getChildAt(i);
			if ( submodule.equals(node.getSubModuleName()))
			{
				return node;
			}
		}
		return null;
	}

	class SubModuleNode extends DefaultCheckBoxNode
	{
		private int _commitsBehind;

		SubModuleNode(String subModule, JTree tree)
		{
			super(subModule, tree);
		}
		
		/**
		 * @return
		 */
		public int getCommitsBehind()
		{
			return _commitsBehind;
		}

		public void setCommitsBehind(int commitsBehind)
		{
			_commitsBehind = commitsBehind;
			((DefaultTreeModel)getModel()).nodeChanged(this);
		}
		
		@Override
		public String toString()
		{
			if ( _commitsBehind > 0 )
			{
				return "<html>"+super.toString().concat("<font color=\"red\"> (").concat(String.valueOf(_commitsBehind)).concat("\u2193").concat(")</font></html>");
			}
			return super.toString();
		}
		
		public String getSubModuleName()
		{
			return (String)super.getUserObject();
		}
		
	}
	

}