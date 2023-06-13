/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJPanel;
import rma.swing.RmaJTable;
import rma.swing.table.RmaTableModel;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.forecast.temptarget.TempTargetForecastTableModel;

/**
 * @author mark
 *
 */
public abstract class AbstractForecastPanel extends RmaJPanel
{
	protected static List<AbstractForecastPanel>_panels = new ArrayList<>();
	
	protected ForecastPanel _forecastPanel;
	protected ForecastTable _initialConditionsTable;
	protected ForecastTable _opsTable;
	protected ForecastTable _metTable;
	protected ForecastTable _bcTable;
	protected ForecastTable _tempTargetTable;
	protected EnabledJPanel _lowerPanel;
	
	private List<ForecastTable>_tables = new ArrayList<>();
	private EnabledJPanel _tablePanel;
	private static RmaTableModel _initialConditionsTableModel;
	private static RmaTableModel _opsTableModel;
	private static RmaTableModel _metTableModel;
	private static RmaTableModel _bcTableModel;
	private static RmaTableModel _tempTargetTableModel;

	private static ListSelectionModel _initialConditionsSelectionModel;
	private static ListSelectionModel _opsSelectionModel;
	private static ListSelectionModel _metSelectionModel;
	private static ListSelectionModel _bcSelectionModel;
	private static ListSelectionModel _tempTargetSelectionModel;

	/**
	 * @param forecastPanel
	 */
	public AbstractForecastPanel(ForecastPanel forecastPanel)
	{
		super(new GridBagLayout());
		_forecastPanel = forecastPanel;
		buildControls();
		addListeners();
		_panels.add(this);
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		_tablePanel = new EnabledJPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.5;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_tablePanel, gbc);

		if ( _initialConditionsTableModel != null )
		{
			_initialConditionsTable = new ForecastTable(this, _initialConditionsTableModel);
		}
		else
		{
			_initialConditionsTable = new ForecastTable(this, new String[] {"Initial Conditions"});
		}
		if ( _initialConditionsSelectionModel != null )
		{
			_initialConditionsTable.setSelectionModel(_initialConditionsSelectionModel);
		}
		else
		{
			_initialConditionsSelectionModel = _initialConditionsTable.getSelectionModel();
			_initialConditionsSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		if ( _initialConditionsTableModel == null )
		{
			_initialConditionsTableModel = (RmaTableModel) _initialConditionsTable.getModel();
		}
		_initialConditionsTable.setName("Initial Conditions");
		_initialConditionsTable.getPopupMenu().remove(_initialConditionsTable.getDeleteMenuItem());
		_initialConditionsTableModel.clearAll();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_tablePanel.add(_initialConditionsTable.getScrollPane(), gbc);
		_tables.add(_initialConditionsTable);

		if ( _opsTableModel != null )
		{
			_opsTable = new ForecastTable(this, _opsTableModel);
		}
		else
		{
			_opsTable = new ForecastTable(this, new String[] {"Operations"});
		}
		if ( _opsSelectionModel != null )
		{
			_opsTable.setSelectionModel(_opsSelectionModel);
		}
		else
		{
			_opsSelectionModel = _opsTable.getSelectionModel();
			_opsSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		if ( _opsTableModel == null )
		{
			_opsTableModel = (RmaTableModel) _opsTable.getModel();
		}
		_opsTable.setName("Operations");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_tablePanel.add(_opsTable.getScrollPane(), gbc);
		_tables.add(_opsTable);
		
		if ( _metTableModel != null )
		{
			_metTable = new ForecastTable(this, _metTableModel);
		}
		else
		{
			_metTable = new ForecastTable(this, new String[] {"Meteorology"});
		}
		if ( _metSelectionModel != null )
		{
			_metTable.setSelectionModel(_metSelectionModel);
		}
		else
		{
			_metSelectionModel = _metTable.getSelectionModel();
			_metSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		if ( _metTableModel == null )
		{
			_metTableModel = (RmaTableModel) _metTable.getModel();
		}
		_metTable.setName("Meteorology");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_tablePanel.add(_metTable.getScrollPane(), gbc);
		_tables.add(_metTable);
		
		if ( _bcTableModel != null )
		{
			_bcTable = new ForecastTable(this, _bcTableModel);
		}
		else
		{
			_bcTable = new ForecastTable(this, new String[] {"Boundary Condition Sets"});
		}
		if ( _bcSelectionModel != null )
		{
			_bcTable.setSelectionModel(_bcSelectionModel);
		}
		else
		{
			_bcSelectionModel = _bcTable.getSelectionModel();
			_bcSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		if ( _bcTableModel == null )
		{
			_bcTableModel = (RmaTableModel) _bcTable.getModel();
		}
		_bcTable.setName("Boundary Condition Sets");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_tablePanel.add(_bcTable.getScrollPane(), gbc);	
		_tables.add(_bcTable);
		
		if ( _tempTargetTableModel != null )
		{
			_tempTargetTable = new ForecastTable(this, _tempTargetTableModel);
		}
		else
		{
			_tempTargetTable = new ForecastTable(this, new String[] {"Temperature Target Sets"});
		}
		if ( _tempTargetSelectionModel != null )
		{
			_tempTargetTable.setSelectionModel(_tempTargetSelectionModel);
		}
		else
		{
			_tempTargetSelectionModel = _tempTargetTable.getSelectionModel();
			_tempTargetSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		if ( _tempTargetTableModel == null )
		{
			_tempTargetTableModel = new TempTargetForecastTableModel();
			_tempTargetTable.setModel(_tempTargetTableModel);
		}
		_tempTargetTable.setName("Temperature Target Sets");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_tablePanel.add(_tempTargetTable.getScrollPane(), gbc);	
		_tables.add(_tempTargetTable);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);		
		
		_lowerPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_lowerPanel, gbc);
		
		buildLowerPanel(_lowerPanel);
	
	
	}
	/**
	 * 
	 */
	protected void addListeners()
	{
		_initialConditionsTable.getSelectionModel().addListSelectionListener(e -> tableSelected(e, _initialConditionsTable));
		_opsTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_opsTable));
		_metTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_metTable));
		_bcTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_bcTable));
		_tempTargetTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_tempTargetTable));
		addUpperTableListeners();
	}

	void addUpperTableListeners()
	{
		for(ForecastTable table : _tables)
		{
			table.addMouseListener(buildUpperTableMouseListener(table));
			table.getScrollPane().getViewport().addMouseListener(buildUpperTableMouseListener(table));
			table.getTableHeader().addMouseListener(buildUpperTableMouseListener(table));
			JMenuItem deleteMenuItem = table.getDeleteMenuItem();
			if(deleteMenuItem != null)
			{
				deleteMenuItem.addActionListener(e -> deleteClicked(table));
			}
		}
	}

	private void deleteClicked(ForecastTable table)
	{
		int row = table.getPopupMenuRow();
		AbstractForecastPanel panel = getPanelForTable(table);
		if(row >= 0 && panel != null)
		{
			try
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				panel.tableRowDeleteClicked(row);
			}
			finally
			{
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}

	private MouseListener buildUpperTableMouseListener(ForecastTable table)
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				_forecastPanel.setSelectedTab(getPanelForTable(table));
			}
		};
	}
	/**
	 * @param e 
	 * @param table
	 * @return
	 */
	private void tableSelected(ListSelectionEvent e, ForecastTable table)
	{
		if ( e.getValueIsAdjusting())
		{
			return;
		}
		AbstractForecastPanel panel = getPanelForTable(table);
		if ( panel != null )
		{
			_forecastPanel.setSelectedTab(panel);
			panel.savePanel();
			int row = table.getSelectedRow();
			panel.tableRowSelected(row);
		}
	}



	/**
	 * @param forecastTable
	 * @return
	 */
	public AbstractForecastPanel getPanelForTable(ForecastTable forecastTable)
	{
		for(int i = 0; i < _panels.size(); i++ )
		{
			ForecastTable table = _panels.get(i).getTableForPanel();
			if ( table != null )
			{
				if ( table.getName().equals(forecastTable.getName()))
				{
					return _panels.get(i);
				}
			}
		}
		return null;
	}



	/**
	 * 
	 */
	protected abstract void tableRowSelected(int selectedRow);

	/**
	 *
	 * @param selectedRow - row to delete
	 */
	public abstract void tableRowDeleteClicked(int selectedRow);


	/**
	 * @param lowerPanel 
	 * 
	 */
	protected abstract void buildLowerPanel(EnabledJPanel lowerPanel);
	public abstract ForecastTable getTableForPanel();
	
	/**
	 * 
	 */
	protected void panelActivated()
	{
		ForecastTable panelTable = getTableForPanel();
		ForecastTable ftable;
		for (int i = 0;i < _tables.size();i++ )
		{
			ftable = _tables.get(i);
			ftable.setEnabled(ftable == panelTable);
		}
		
	}

	protected class ForecastTable extends RmaJTable
	{

		private JMenuItem _deleteMenuItem;
		private AbstractForecastPanel _parentForecastPanel;
		private Border _defaultBorder;
		private int _popupMenuRow = -1;

		/**
		 * @param parent
		 * @param headers
		 */
		protected ForecastTable(AbstractForecastPanel parent,
				String[] headers)
		{
			super(parent, headers);
			_parentForecastPanel = parent;
			setRowHeight(getRowHeight()+5);
			_defaultBorder = getScrollPane().getBorder();
			buildPopupMenu();
			addForecastTableListeners();
		}

		protected ForecastTable(AbstractForecastPanel parent, RmaTableModel tableModel)
		{
			super(parent,tableModel);
			_parentForecastPanel = parent;
			buildPopupMenu();
			addForecastTableListeners();
		}

		private void addForecastTableListeners()
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					Point clickPoint = e.getPoint();
					if (SwingUtilities.isRightMouseButton(e))
					{
						_popupMenuRow = rowAtPoint(clickPoint);
					}
				}
			});
		}

		private void buildPopupMenu()
		{
			_deleteMenuItem = new JMenuItem("Delete...");
			addPopupItem(_deleteMenuItem, 0);
			removePopuMenuFillOptions();
			removePopupMenuSumOptions();
			removePopupMenuRowEditingOptions();
		}

		private JMenuItem getDeleteMenuItem()
		{
			return _deleteMenuItem;
		}

		private int getPopupMenuRow()
		{
			return _popupMenuRow;
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			Dimension d = super.getPreferredScrollableViewportSize();
			d.height = getRowHeight()* 4;
			return d;
		}
		@Override
		public boolean isCellEditable(int row, int col)
		{
			return false;
		}
		@Override
		public void setEnabled(boolean enabled)
		{
			Color bcColor = enabled?UIManager.getColor("Table.background"):AbstractForecastPanel.this.getBackground();
			getScrollPane().setBackground(bcColor);
			getTableHeader().setBackground(bcColor);
			getTableHeader().setEnabled(enabled);
			getScrollPane().setBorder(enabled?new LineBorder(Color.black, 2):_defaultBorder);
			super.setEnabled(!enabled);
			
			
		}
		
	}

	/**
	 * 
	 */
	protected abstract void savePanel();
	
	public abstract void setSimulationGroup(ForecastSimGroup fsg);

	
	

}
