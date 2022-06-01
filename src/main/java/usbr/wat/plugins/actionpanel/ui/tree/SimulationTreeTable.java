/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.JXTree.DelegatingRenderer;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

import hec2.wat.model.WatSimulation;

import rma.swing.RmaJCheckBox;
import rma.swing.RmaJXTreeTable;
import rma.swing.table.RmaCellEditor;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.ResultsData;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class SimulationTreeTable extends RmaJXTreeTable
{
	public static final Color _oddRowBackground = new Color(239,247,254);
	
	private Border _defaultBorder;
	private Highlighter _tableRowHighlighter = new ReportRowHighligher();
	
	public SimulationTreeTable()
	{
		super(createTreeModel());
		adPopupListener();
		setRenderers();
		setHighlighters(_tableRowHighlighter); // for now
		
	}
	/**
	 * 
	 */
	private void adPopupListener()
	{
		MouseAdapter ma = new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if ( e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e) )
				{
					Point pt = e.getPoint();
					TreePath path = getPathForLocation((int)pt.getX(), (int)pt.getY());
					if ( path == null )
					{
						return;
					}
					JPopupMenu popup = createPopupMenu();
					Object lastComp = path.getLastPathComponent();
					if ( lastComp instanceof ActionsTreeTableNode )
					{
						((ActionsTreeTableNode)lastComp).addPopupMenuItems(popup);
					}
					popup.show(SimulationTreeTable.this, pt.x, pt.y);
				}
				
				
			}
		};
		addMouseListener(ma);
	}
	/**
	 * 
	 */
	private void setRenderers()
	{
		TreeCellRenderer rend = getTreeCellRenderer();
		if ( rend instanceof JXTree.DelegatingRenderer )
		{
			JXTree.DelegatingRenderer delRend = (DelegatingRenderer) rend;
			TreeCellRenderer treeCellRenderer = delRend.getDelegateRenderer();
			IconTableCellRenderer diffrenderer = new IconTableCellRenderer(this, treeCellRenderer); 
			delRend.setDelegateRenderer(diffrenderer);
			setShowGrid(true, true);
		}
		
	}
	/**
	 * 
	 */
	private JPopupMenu createPopupMenu()
	{
		JPopupMenu popup = new JPopupMenu();
		JMenuItem showInProjectTreeMenu = new JMenuItem("Show In Study Tree");
		showInProjectTreeMenu.addActionListener(e->ActionPanelPlugin.getInstance().getActionsWindow().showInProjectTreeAction());
		popup.add(showInProjectTreeMenu);
		return popup;
	}
	/**
	 * @return
	 */
	private static TreeTableModel createTreeModel()
	{
		return new SimulationTreeTableModel(null);
	}
	@Override
	protected void init()
	{
		super.init();
		if (_defaultBorder == null)
		{
			createDefaultBorder();
		}
	}	
	protected void createDefaultBorder()
	{
		_defaultBorder = BorderFactory.createLineBorder(Color.black);
	}
	public JCheckBox setCheckBoxCellEditor(int col)
	{
		return setCheckBoxCellEditor(col, true);
	}
	/**
	 * for some reason the JXTreeTable doesn't install highlighters like it should
	 * so this is there to do that.
	 */
	@Override
	public void updateUI() 
	{
		if ( _tableRowHighlighter == null )
		{
			super.updateUI();
			return;
		}
        removeHighlighter(_tableRowHighlighter);

        super.updateUI();

        // JTable does this striping automatically but JXTable's default renderer
        // seems to ignore it, so JXTreeTable inherits this broken behaviour.
        addHighlighter(_tableRowHighlighter);
	}
	/**
	 *  sets a TextArea editor for column number col
	 *
	 *  sets a checkbox editor for column number col
	 *
	 *@param  col  The new CheckBoxCellEditor value
	 *@param  useSelectionForegroun true for the CellRenderer to render the selection background when the cell is selection
	 *@return      Description
	 */
	public JCheckBox setCheckBoxCellEditor(int col, boolean useSelectionBackground)
	{
		RmaJCheckBox cb = new RmaJCheckBox();
		//button.setBorder(javax.swing.BorderFactory.createLineBorder(Color.black));
		cb.setFocusPainted(false);
		//button.setBorderPainted(true);
		//button.setBackground(this.getColumnBackground(col));
		RmaCellEditor ce = new RmaCellEditor(cb);
		ce.setClickCountToStart(1);
		getColumnModel().getColumn(col).setCellEditor(ce);
		getColumnModel().getColumn(col).setCellRenderer(createBooleanRenderer(useSelectionBackground));
		return cb;
	}
	protected TableCellRenderer createBooleanRenderer(boolean useSelectionBackground)
	{
		return  new BooleanRenderer(useSelectionBackground);
	}
	/**
	 * Sets a button editor on the specified column. The button is returned to add
	 * ActionListeners or change its appearance.
	 */
	public JButton setButtonCellEditor(int col)
	{
		JXButton button = new JXButton();
		//button.setBorder(javax.swing.BorderFactory.createLineBorder(Color.black));
		button.setFocusPainted(false);
		//button.setBorderPainted(true);
		//button.setBackground(this.getColumnBackground(col));
		RmaCellEditor ce = new RmaCellEditor(button);
		getColumnModel().getColumn(col).setCellEditor(ce);
		getColumnModel().getColumn(col).setCellRenderer(new ButtonRenderer());
		return button;
	}
	@Override
	public Class<?> getColumnClass(int column)
	{
		if ( column == 1 )
		{
			return Boolean.class;
		}
		return super.getColumnClass(column);
	}
	@Override
	public void setTreeTableModel(TreeTableModel newModel)
	{
		super.setTreeTableModel(newModel);
		//setCheckBoxCellEditor(SimulationTreeTableModel.SELECTED_COLUMN);
		
		JButton button = setButtonCellEditor(SimulationTreeTableModel.DISPLAY_IN_MAPS_COLUMN);
		button.setText("Show on Map");
		button.addActionListener(e->ActionPanelPlugin.getInstance().getActionsWindow().displaySimulationInMap());
		
		button = setButtonCellEditor(SimulationTreeTableModel.VIEW_REPORT_COLUMN);
		button.setText("View Report");
		button.addActionListener(e->ActionPanelPlugin.getInstance().getActionsWindow().displayReport());
		setColumnWidths(350,150,110,110);
		expandAll();
	}
	@Override
	public boolean isCellEditable(int row, int column)
	{
		TreePath path = getPathForRow(row);
		if ( path != null )
		{
			return getTreeTableModel().isCellEditable(path.getLastPathComponent(), column);
		}
		return false;
	}
	/**
	 * 
	 * @author mark
	 *
	 */
	protected static class BooleanRenderer extends JCheckBox implements TableCellRenderer
	{
		private boolean _useSelectionBackground;

		public BooleanRenderer(boolean useSelectionBackground)
		{
			super();
			_useSelectionBackground = useSelectionBackground;
			BooleanRenderer.this.setHorizontalAlignment(JLabel.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			/*
			if ( table.isCellEditable( row, column ) )
			{
				setEnabled(true);
				Color c = ((SimulationTreeTable)table).getCellBackground(row,column);
				setBackground(c);//table.getBackground());
			}
			else
			{
				setBackground( ((SimulationTreeTable)table).getDisabledBackground(row, column));
				//UIManager.getDefaults().getColor( "TextField.disabledBackground" ) );
				setEnabled(false);
			}
			*/
			setForeground(table.getForeground());

			if (isSelected)
			{
				setForeground(table.getSelectionForeground());
				if ( _useSelectionBackground)
				{
					super.setBackground(table.getSelectionBackground());
				}
			}

			setSelected((value != null && "true".equalsIgnoreCase(value.toString())));
			return this;
		}
	}
	static class ButtonRenderer extends JButton implements TableCellRenderer
	{
		public ButtonRenderer()
		{
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			boolean cellEditable = table.isCellEditable( row, column );
			if (isSelected)
			{
//				setForeground(table.getSelectionForeground());
//				super.setBackground(table.getSelectionBackground());				
			}
			else
			{
				
				if ( cellEditable )
				{
			//		ButtonRenderer.this.setEnabled(true);
//					setBackground(table.getBackground());
				}
				else
				{
			//		ButtonRenderer.this.setEnabled(false);
//					setBackground( UIManager.getDefaults().getColor( "TextField.disabledBackground" ) );
				}
				setForeground(((SimulationTreeTable)table).getRowForeground(row));
			 }
			ButtonRenderer.this.setEnabled(cellEditable);
			setText(value != null ? value.toString() : "");
			return this;
		}
	}
	
	private class ReportRowHighligher extends AbstractHighlighter
	{
        @Override
        protected Component doHighlight(Component component, ComponentAdapter componentAdapter) 
        {
        	if ( !componentAdapter.isSelected())
        	{
        		Object obj = getValueAt(componentAdapter.row, SimulationTreeTableModel.SIMULATION_COLUMN);
        		
        		if ( obj instanceof WatSimulation )
        		{
        			component.setBackground(_oddRowBackground);
        		}
        		else
        		{
        			component.setBackground(UIManager.getColor("Table.background"));
        		}
        		Color fg = getRowForeground(componentAdapter.row);
        		if ( fg != null )
        		{
        			component.setForeground(fg);
        		}
        	}
        	else if ( componentAdapter.isSelected() )
        	{
        		component.setBackground(UIManager.getColor("Table.selectionBackground"));
        		component.setForeground(UIManager.getColor("Table.selectionForeground"));
        	}
        	
        	return component;
        }
	}

	/**
	 * 
	 */
	public void clearColors()
	{
		_rowForeground.clear();
	}
	/**
	 * @param currentSim
	 * @return
	 */
	public SimulationTreeTableNode getSimulationNodeFor( WatSimulation sim)
	{
		SimulationTreeTableModel model = (SimulationTreeTableModel) getTreeTableModel();
		SimulationTreeTableNode root = (SimulationTreeTableNode) model.getRoot();
		int kidCount = root.getChildCount();
		TreeTableNode child ;
		for (int i = 0;i < kidCount; i++ )
		{
			child = root.getChildAt(i);
			if ( child instanceof SimulationTreeTableNode )
			{
				SimulationTreeTableNode simNode = (SimulationTreeTableNode) child;
				if ( simNode.getSimulation() == sim)
				{
					return simNode;
				}
			}
		}
		return null;
	}
	/**
	 * @return
	 */
	public List<WatSimulation> getSelectedSimulations()
	{
		List<WatSimulation>selectedSims = new ArrayList<>();
		int rowCnt = getRowCount();
		Object obj;
		for (int r = 0;r < rowCnt; r++  )
		{
			obj = getValueAt(r, SimulationTreeTableModel.SELECTED_COLUMN);
			if (obj == null )
			{
				continue;
			}
			
			if ( RMAIO.parseBoolean(obj.toString(), false))
			{
				obj = getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);
				if ( obj instanceof WatSimulation )
				{
					selectedSims.add((WatSimulation)obj);
				}
			}
		}
		return selectedSims;
	}
	public List<ResultsData> getSelectedResults()
	{
		List<ResultsData>selectedResults = new ArrayList<>();
		int rowCnt = getRowCount();
		Object obj;
		for (int r = 0;r < rowCnt; r++  )
		{
			obj = getValueAt(r, SimulationTreeTableModel.SELECTED_COLUMN);
			if (obj == null )
			{
				continue;
			}
			
			if ( RMAIO.parseBoolean(obj.toString(), false))
			{
				obj = getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);
				if ( obj instanceof ResultsData )
				{
					selectedResults.add((ResultsData)obj);
				}
			}
		}
		return selectedResults;
	}

}
