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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
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
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;
import usbr.wat.plugins.actionpanel.ui.forecast.temptarget.TempTargetForecastTableModel;

/**
 * @author mark
 *
 */
public abstract class AbstractForecastPanel extends RmaJPanel
{
	protected static List<AbstractForecastPanel>_panels = new ArrayList<>();
	
	protected ForecastPanel _forecastPanel;
	protected ForecastTable _opsTable;
	protected ForecastTable _metTable;
	protected ForecastTable _bcTable;
	protected ForecastTable _tempTargetTable;
	private EnabledJPanel _lowerPanel;
	
	private List<ForecastTable>_tables = new ArrayList<>();
	private EnabledJPanel _tablePanel;
	private static RmaTableModel _opsTableModel;
	private static RmaTableModel _metTableModel;
	private static RmaTableModel _bcTableModel;
	private static RmaTableModel _tempTargetTableModel;
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
		_opsTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_opsTable));
		_metTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_metTable));
		_bcTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_bcTable));
		_tempTargetTable.getSelectionModel().addListSelectionListener(e->tableSelected(e,_tempTargetTable));
		MouseAdapter opsMa =new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				_forecastPanel.setSelectedTab(getPanelForTable(_opsTable));
			}
		} ;
		_opsTable.addMouseListener(opsMa);
		_opsTable.getScrollPane().addMouseListener(opsMa);
		_opsTable.getTableHeader().addMouseListener(opsMa);

		MouseAdapter metMa = new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				_forecastPanel.setSelectedTab(getPanelForTable(_metTable));
			}
		};
		_metTable.addMouseListener(metMa);
		_metTable.getScrollPane().addMouseListener(metMa);
		_metTable.getTableHeader().addMouseListener(metMa);

		MouseAdapter bcMa =new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				_forecastPanel.setSelectedTab(getPanelForTable(_bcTable));
			}
		};
		_bcTable.addMouseListener(bcMa);
		_bcTable.getScrollPane().addMouseListener(bcMa);
		_bcTable.getTableHeader().addMouseListener(bcMa);

		MouseAdapter ttMa = new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				_forecastPanel.setSelectedTab(getPanelForTable(_tempTargetTable));
			}
		};
		_tempTargetTable.addMouseListener(ttMa);
		_tempTargetTable.getScrollPane().addMouseListener(ttMa);
		_tempTargetTable.getTableHeader().addMouseListener(ttMa);

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
			panel.savePanel();
			panel.tableRowSelected();
		}
	}



	/**
	 * @param forecastTable
	 * @return
	 */
	private AbstractForecastPanel getPanelForTable(ForecastTable forecastTable)
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
	protected abstract void tableRowSelected();



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

		private Border _defaultBorder;
		/**
		 * @param parent
		 * @param headers
		 */
		public ForecastTable(AbstractForecastPanel parent,
				String[] headers)
		{
			super(parent, headers);
			setRowHeight(getRowHeight()+5);
			_defaultBorder = getScrollPane().getBorder();
		}
		public ForecastTable(AbstractForecastPanel parent, RmaTableModel tableModel)
		{
			super(parent,tableModel);
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
