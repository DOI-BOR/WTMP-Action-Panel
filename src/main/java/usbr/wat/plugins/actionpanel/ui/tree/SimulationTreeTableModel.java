/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.tree;

import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;

/**
 * @author mark
 *
 */
public class SimulationTreeTableModel extends AbstractTreeTableModel
{
	public static final int SELECTED_COLUMN = 1;
	public static final int SIMULATION_COLUMN = 0;
	public static final int DISPLAY_IN_MAPS_COLUMN = 2;	
	public static final int VIEW_REPORT_COLUMN = 3;
	/**
	 * table column header values
	 */
	public static final String[]	_headers = new String[]
	{
			"Simulation", "Selected", "Map", "Report"	
	};
	private SimulationTreeTableNode _root;
	
	public SimulationTreeTableModel(AbstractSimulationGroup simGroup)
	{
		super(new SimulationTreeTableNode(null, null));
		_root = (SimulationTreeTableNode) getRoot();
		_root.setSimulationTreeModel(this);
		if ( simGroup != null )
		{
			List<WatSimulation> sims = simGroup.getSimulations();
			for (int i = 0;i < sims.size(); i++ )
			{
				SimulationTreeTableNode node = new SimulationTreeTableNode(this, sims.get(i));
				_root.add(node);
			}
				
		}
	}
	@Override
	public int getColumnCount()
	{
		 return _headers.length;
	}
	/**
     * Returns the name of the column at {@code columnIndex}. This is used to
     * initialize the table's column header name. Note: this name does not need
     * to be unique; two columns in a table can have the same name.
     * 
     * @param column
     *            the index of the column
     * @return the name of the column
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
	@Override
	public String getColumnName(int column) 
	{
		 return _headers[column];
	}
	@Override
	public Object getValueAt(Object node, int column)
	{
		if ( node instanceof TreeTableNode )
		{
			return ((TreeTableNode)node).getValueAt(column);
		}
		return null;
	}
	@Override
	public void setValueAt(Object value, Object node, int column) 
	 {
		 if (column < 0 || column >= getColumnCount()) 
		 {
			 throw new IllegalArgumentException("column must be a valid index");
		 }

		 TreeTableNode ttn = (TreeTableNode) node;

		 if (column < ttn.getColumnCount()) 
		 {
			 ttn.setValueAt(value, column);
			 fireNodeChanged(ttn);
		 }
   }

	@Override
	public Object getChild(Object parentObj, int childIdx)
	{
		if(parentObj instanceof TreeTableNode )
		{
			return ((TreeTableNode)parentObj).getChildAt(childIdx);
		}
		return null;
	}

	@Override
	public int getChildCount(Object obj)
	{
		if(obj instanceof TreeTableNode )
		{
			return ((TreeTableNode)obj).getChildCount();
		}
		return 0;
	}

	@Override
	public int getIndexOfChild(Object parentObj, Object childObj)
	{
		if(parentObj instanceof TreeTableNode )
		{
			((TreeTableNode)parentObj).getIndex((TreeNode)childObj);
		}
		return 0;
	}
	@Override
	public boolean isCellEditable(Object cellObj, int col)
	{
		boolean retval = false;

		switch (col)
		{
			case SELECTED_COLUMN:
				retval = true;
				break;
			case SIMULATION_COLUMN :
				retval = false;
				break;
			case DISPLAY_IN_MAPS_COLUMN :
				if ( cellObj instanceof SimulationTreeTableNode )
				{
					retval = true;
				}
				break;
			case VIEW_REPORT_COLUMN : // location
				retval = true;
				break;
		}
		return retval;
	}
	
	/**
	 *  notify listeners that the the table data has changed 
	 */
	public void fireTableDataChanged()
	{
		fireNodeStructureChanged(_root);
	}
	public void fireNodeStructureChanged(TreeTableNode node )
	{
		TreeModelListener[] listeners = getTreeModelListeners();
		
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode)node).getPath());
		for (int i = 0;i < listeners.length; i++ )
		{
			listeners[i].treeStructureChanged(event);
		}
	}
	/**
	 *  notify listeners that a tree table node has changed
	 * @param node
	 */
	public void fireNodeChanged(TreeTableNode node)
	{
		if ( node == null )
		{
			return;
		}
		
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode)node).getPath());
		fireNodesChanged(event);
	}
	/**
	 * notify listeners of the TreeModelEvent 
	 * @param event the TreeModelEvent to send to listeners
	 */
	public void fireNodesChanged(TreeModelEvent event)
	{
		if ( event == null )
		{
			return;
		}
		TreeModelListener[] listeners = getTreeModelListeners();
		for (int i = 0;i < listeners.length; i++ )
		{
			listeners[i].treeNodesChanged(event);
		}
	}
	/**
	 * notify listeners that a tree table node was inserted
	 * @param node the newly inserted node
	 */
	public void fireNodeInserted(TreeTableNode node)
	{
		if ( node == null )
		{
			return;
		}
		TreeModelListener[] listeners = getTreeModelListeners();
		
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode)node).getPath());
		for (int i = 0;i < listeners.length; i++ )
		{
			listeners[i].treeNodesInserted(event);
		}
	}
	/**
	 * notify listeners that a tree table node was removed
	 * @param node the newly removed node
	 */
	public void fireNodeRemoved(TreeTableNode node)
	{
		if ( node == null )
		{
			return;
		}
		TreeModelListener[] listeners = getTreeModelListeners();
		
		TreeModelEvent event = new TreeModelEvent(this, ((ActionsTreeTableNode)node).getPath());
		for (int i = 0;i < listeners.length; i++ )
		{
			listeners[i].treeNodesRemoved(event);
		}
	}
	

}
