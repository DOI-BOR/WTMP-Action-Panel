/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.table.TableColumnModel;

import hec2.wat.model.WatAnalysisPeriod;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJTable;
import rma.swing.table.ColumnGroup;
import rma.swing.table.GroupableTableHeader;
import rma.swing.table.MleHeadRenderer;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;
import usbr.wat.plugins.actionpanel.actions.forecast.EditEnsembleSetAction;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;

/**
 * @author mark
 *
 */
public class SimulationPanel extends AbstractSimulationPanel
	implements UsbrPanel
{
	
	
	private EnabledJPanel _topPanel;
	private JLabel _apLabel;
	private JLabel _apStartLabel;
	private JLabel _apEndLabel;
	private ForecastPanel _parentPanel;
	
	private RmaJTable _simEnsembleTable;
	private JButton _editEnsembleButton;
	private ColumnGroup _columnGroup;
	private RmaJCheckBox _recomputeAllChk;

	public SimulationPanel(ActionsWindow parentWindow, ForecastPanel parentPanel)
	{
		super(parentWindow);
		_parentPanel = parentPanel;
		buildControls();
		addListeners();
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		_topPanel = new EnabledJPanel(new GridBagLayout());
		buildTopPanel(_topPanel);
	}
	private void buildTopPanel(JPanel topPanel)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.5;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(topPanel, gbc);
		
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);
		
		_apLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apLabel, gbc);
		
		label = new JLabel("Start Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		topPanel.add(label, gbc);
		
		_apStartLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apStartLabel, gbc);
		
		label = new JLabel("End Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		topPanel.add(label, gbc);
		
		_apEndLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apEndLabel, gbc);
		
	
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);
		
		label = new JLabel("Simulations:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth =  GridBagConstraints.REMAINDER; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);	
		
		String[] headers = new String[] {"Selected", "Simulation", "Map", "Report"};
		_simulationTable = new SimulationTreeTable(this)
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				return getTableToolTipText(e);
			}
			@Override
			public void setValueAt(Object value, int row, int col)
			{
				super.setValueAt(value, row, col);
				if ( col == SimulationTreeTableModel.SELECTED_COLUMN )
				{
					tableCheckBoxAction();
				}
			}
		};
		
		
		
		_simulationTable.setColumnWidths(350,150,110,110);
		
		_simulationTable.setRowHeight(_simulationTable.getRowHeight()+5);
		
		
		JButton button = _simulationTable.setButtonCellEditor(2);
		button.addActionListener(e->displaySimulationInMap());
		button.setText("Show on Map");
		button = _simulationTable.setButtonCellEditor(3);
		button.setText("View Report");
		button.addActionListener(e->displayReport());
		//_simulationTable.deleteCells();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_simulationTable.getScrollPane(), gbc);
		
		if (! Boolean.getBoolean("NoSimulationComputeState"))
		{
			JPanel legendPanel = buildLegendPanel();
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx   = 1.0;
			gbc.weighty   = 0.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.HORIZONTAL;
			gbc.insets    = RmaInsets.INSETS5505;
			topPanel.add(legendPanel, gbc);
		
		}
		
		_simActionsPanel = new SimulationActionsPanel(ActionPanelPlugin.getInstance().getActionsWindow(), this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_simActionsPanel, gbc);	
	
		headers = new String[] {"Selected\nto Run", "Boundary Conditions", "Temperature Target Set", "Target Members\nTo Run", "Target Members\nPreviously Run"};
		_simEnsembleTable = new RmaJTable(this, headers)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return column == 0 || column == 3;
			}
		};
		_simEnsembleTable.setRowHeight(_simEnsembleTable.getRowHeight()+5);
		MleHeadRenderer renderer = _simEnsembleTable.setMlHeaderRenderer();
		_simEnsembleTable.setTableHeader(new GroupableTableHeader(_simEnsembleTable.getColumnModel()));
		TableColumnModel cm = _simEnsembleTable.getColumnModel();
		_columnGroup = new ColumnGroup(renderer, "Simulation Name");
		_columnGroup.add(cm.getColumn(0));
		_columnGroup.add(cm.getColumn(1));
		_columnGroup.add(cm.getColumn(2));
		_columnGroup.add(cm.getColumn(3));
		_columnGroup.add(cm.getColumn(4));
		_simEnsembleTable.setCheckBoxCellEditor(0);
		GroupableTableHeader header = (GroupableTableHeader)_simEnsembleTable.getTableHeader();
		header.addColumnGroup(_columnGroup);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_simEnsembleTable.getScrollPane(), gbc);

		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0000;
		add(panel, gbc);

		EditEnsembleSetAction editAction = new EditEnsembleSetAction();
		_editEnsembleButton = new JButton(editAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(20,5,0,5);
		panel.add(_editEnsembleButton, gbc);
		
		_recomputeAllChk = new RmaJCheckBox("Recompute All");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(20,5,0,5);
		panel.add(_recomputeAllChk, gbc);
	
	}

	
	/**
	 * 
	 */
	private void addListeners()
	{
		// TODO Auto-generated method stub
		System.out.println("addListeners TODO implement me");
		
	}
	
	
	public boolean shouldRecomputeAll()
	{
		return _recomputeAllChk.isSelected();
	}

	/**
	 * @param sg
	 */
	@Override
	public void setSimulationGroup(AbstractSimulationGroup asg)
	{
		if ( asg instanceof ForecastSimGroup )
		{
			ForecastSimGroup fsg = (ForecastSimGroup) asg;
			_parentPanel.setSimulationGroup(fsg);
			fillSimulationTable();
			fillEnsembleTable();
			fillAnalysisWindow();
			setEnabled(true);
		}
		else
		{
			_parentPanel.setSimulationGroup(null);
			fillSimulationTable();
			fillEnsembleTable();
			fillAnalysisWindow();
			setEnabled(true);
		}
	}

	/**
	 * 
	 */
	private void fillAnalysisWindow()
	{
		clearApLabels();
		AbstractSimulationGroup fsg = getSimulationGroup();
		if ( fsg != null )
		{
			WatAnalysisPeriod ap = fsg.getAnalysisPeriod();
			if ( ap != null )
			{
				_apLabel.setText(ap.getName());
				_apStartLabel.setText(ap.getRunTimeWindow().getStartTime().toString());
				_apEndLabel.setText(ap.getRunTimeWindow().getEndTime().toString());
			}
		}
	}

	/**
	 * 
	 */
	private void clearApLabels()
	{
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");
		
	}

	/**
	 * 
	 */
	private void fillEnsembleTable()
	{
		// TODO Auto-generated method stub
		System.out.println("fillEnsembleTable TODO implement me");
		
	}

	@Override
	public AbstractSimulationGroup getSimulationGroup()
	{
		return _parentPanel.getSimulationGroup();
	}

	public List<ResultsData> getSelectedResults()
	{
		return _simulationTable.getSelectedResults();
	}
}
