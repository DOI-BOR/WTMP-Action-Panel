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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.MutableTreeNode;
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

import com.rma.client.Browser;

import hec2.wat.model.WatSimulation;
import hec2.wat.ui.WatSimulationNode;

import rma.swing.RmaJCheckBox;
import rma.swing.RmaJDecimalField;
import rma.swing.RmaJIntegerField;
import rma.swing.RmaJIntegerSetField;
import rma.swing.RmaJTable;
import rma.swing.RmaJXTreeTable;
import rma.swing.table.AlignTableCellRenderer;
import rma.swing.table.DecimalCellRenderer;
import rma.swing.table.RmaCellEditor;
import rma.swing.table.RmaTableModelInterface;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.SaveSimulationAsAction;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;

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

	private UsbrPanel _parentPanel;
	private int _precision = 3;

	public SimulationTreeTable(UsbrPanel parentPanel)
	{
		super(createTreeModel());
		_parentPanel = parentPanel;
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
					Object lastComp = path.getLastPathComponent();
					WatSimulation sim = null;
					if ( lastComp instanceof SimulationTreeTableNode)
					{
						sim = ((SimulationTreeTableNode)lastComp).getSimulation();
					}
					JPopupMenu popup = createPopupMenu(sim);
					
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
	 * @param sim 
	 * 
	 */
	private JPopupMenu createPopupMenu(WatSimulation sim)
	{
		JPopupMenu popup = new JPopupMenu();
		JPopupMenu simPopup = null;
		
		if ( sim != null )
		{
			MutableTreeNode node = Browser.getBrowserFrame().getProjectTree().getNodeForManager(sim);
			if ( node instanceof WatSimulationNode )
			{
				WatSimulationNode simNode = (WatSimulationNode) node;
				simPopup = simNode.buildPopupMenu();
			}
		}
		
		if (simPopup != null )
		{
			int compCnt = simPopup.getComponentCount();
			for (int i=compCnt-1; i >= 0; i-- )
			{
				popup.add(simPopup.getComponent(i),0);
			}
		}
		JMenuItem saveAsMenuItem = new JMenuItem("Save As...");
		saveAsMenuItem.addActionListener(e->saveSimulationAs(sim));
		int idx = findSaveMenuIndex(popup);
		popup.add(saveAsMenuItem,idx);
		popup.addSeparator();
	
		JMenuItem showInProjectTreeMenu = new JMenuItem("Show In Study Tree");
		showInProjectTreeMenu.addActionListener(e->_parentPanel.showInProjectTreeAction());
		popup.add(showInProjectTreeMenu);
		JMenuItem editMetaDataMenu = new JMenuItem("Edit MetaData...");
		editMetaDataMenu.addActionListener(e->_parentPanel.editSimulationMetaData());
		popup.add(editMetaDataMenu);
		JMenuItem displayLogMenu = new JMenuItem("View Compute Log...");
		displayLogMenu.addActionListener(e->_parentPanel.displayComputeLog());
		//popup.add(displayLogMenu);
		return popup;
	}
	/**
	 * @return
	 */
	private void saveSimulationAs(WatSimulation sim)
	{
		SaveSimulationAsAction ssa = new SaveSimulationAsAction(_parentPanel);
		ssa.saveSimulationAs(ActionPanelPlugin.getInstance().getActionsWindow().getCalibrationPanel().getSimulationGroup(),  sim);
	}
	/**
	 * @param popup
	 * @return
	 */
	private int findSaveMenuIndex(JPopupMenu popup)
	{
		int compCnt = popup.getComponentCount();
		for (int i=compCnt-1; i >= 0; i-- )
		{
			Component comp = popup.getComponent(i);
			if ( comp instanceof JMenuItem )
			{
				if ( ((JMenuItem)comp).getText().equals("Save"))
				{
					return i+1;
				}
			}
			
		}
		return compCnt-1;
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
	public RmaJDecimalField setDoubleCellEditor(int col)
	{
		TableColumnModel tcm = getColumnModel();
		//if (showFormatting && _decCellRend == null) {
			//_decCellRend = new DecimalCellRenderer(_precision);
		//}

		if (col < tcm.getColumnCount() && col >= 0) {
			TableColumn tc = getColumnModel().getColumn(col);
			if (tc == null)
			{
				return null;
			}
			else
			{
				RmaJDecimalField df = createDecimalField(col, _precision);
				RmaCellEditor dcf = new RmaCellEditor(df);
				//dcf.setDisplayUnitSystem(getDisplayUnitSystem());
				//RmaJTable.ParameterScale ps = (RmaJTable.ParameterScale)this._parameterScaleTable.get(new Integer(this.convertColumnIndexToModel(col)));
				//if (ps != null) {
				//	dcf.setDisplayScaleFactor(ps.paramId, ps.scale);
				//}

				//dcf.setClickCountToStart(getClickCountToStart());
				tc.setCellEditor(dcf);
				//if (showFormatting) {
				//	tc.setCellRenderer(this._decCellRend);
				//} else {
					setHorizontalAlignment(4, col);
				//}

				if (this.getModel() instanceof RmaTableModelInterface)
				{
					((RmaTableModelInterface)this.getModel()).setColumnClass(col, Number.class);
				}

				return df;
			}
		}
		return null;
	}
	public RmaJIntegerSetField setIntegerSetCellEditor(int col)
	{
		TableColumnModel tcm = this.getColumnModel();
		if (col < tcm.getColumnCount() && col >= 0)
		{
			TableColumn tc = this.getColumnModel().getColumn(col);
			if (tc == null)
			{
				return null;
			}
			else
			{
				RmaJIntegerSetField df = new RmaJIntegerSetField();
				//df.setHorizontalAlignment(SwingConstants.RIGHT);
				df.addMouseListener(this);
				RmaCellEditor dcf = new RmaCellEditor(df);
				//dcf.setDisplayUnitSystem(this.getDisplayUnitSystem());
				//RmaJTable.ParameterScale ps = (RmaJTable.ParameterScale)this._parameterScaleTable.get(new Integer(this.convertColumnIndexToModel(col)));
				//if (ps != null)
				//{
				//	dcf.setDisplayScaleFactor(ps.paramId, ps.scale);
				//}

				//dcf.setClickCountToStart(this._clickCountToStart);
				tc.setCellEditor(dcf);
				setHorizontalAlignment(4, col);
				if (getModel() instanceof RmaTableModelInterface)
				{
					((RmaTableModelInterface)getModel()).setColumnClass(col, Number.class);
				}

				return df;
			}
		}
		return null;
	}
	public void setHorizontalAlignment(int align, int col)
	{
		if (col < this.getColumnCount())
		{
			AlignTableCellRenderer rtcr = createAlignTableCellRenderer(align);
			//rtcr.setDisplayUnitsSystem(this.getDisplayUnitSystem());
			TableColumn tc = this.getColumnModel().getColumn(col);
			if (tc != null)
			{
				//RmaJTable.ParameterScale ps = (RmaJTable.ParameterScale)this._parameterScaleTable.get(new Integer(this.convertColumnIndexToModel(col)));
				//if (ps != null) {
				//	rtcr.setDisplayScaleFactor(col, ps.paramId, ps.scale);
				//}

				tc.setCellRenderer(rtcr);
			}
		}
	}
	protected RmaJDecimalField createDecimalField(int col, int precision)
	{
		RmaJDecimalField df = new RmaJDecimalField(0, 5);
		df.setPrecision(precision);
		df.setHorizontalAlignment(SwingConstants.RIGHT);
		df.addMouseListener(this);
		return df;
	}
	public AlignTableCellRenderer createAlignTableCellRenderer(int align)
	{
		return new AlignTableCellRenderer(align);
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
		button.addActionListener(e->_parentPanel.displaySimulationInMap());
		
		button = setButtonCellEditor(SimulationTreeTableModel.VIEW_REPORT_COLUMN);
		button.setText("View Report");
		button.addActionListener(e->_parentPanel.displayReport());
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
