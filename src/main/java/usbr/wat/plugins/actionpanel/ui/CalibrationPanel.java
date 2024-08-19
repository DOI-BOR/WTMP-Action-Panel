/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.tree.MutableTreeNode;

import com.rma.client.Browser;
import com.rma.event.ModifiableListener;
import com.rma.model.ManagerProxy;
import hec.gui.NameDescriptionPanel;

import hec2.wat.client.WatFrame;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import hec2.wat.ui.WatAnalysisPeriodNode;
import rma.lang.Modifiable;
import rma.swing.RmaInsets;
import rma.swing.RmaJList;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ActionsPanel;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class CalibrationPanel extends AbstractSimulationPanel
{
	private static final Color NOT_COMPUTED_COLOR = Color.BLUE;
	private static final Color COMPUTED_COLOR = Color.GREEN.darker();
	private static final Color COMPUTED_ERROR_COLOR = Color.RED;
	private static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;
	
	private ActionsPanel _actionsPanel;
	private JPanel _rightPanel;
	private JLabel _apLabel;
	private RmaJList _statusList;
	private NameDescriptionPanel _nameDescPanel;
	private JLabel _apStartLabel;
	private JLabel _apEndLabel;
	private SimulationGroup _simGroup;
	private CalibrationSimulationGroupPanel _simPanel;
	private ModifiableListener _apModListener;
	private WatAnalysisPeriod _ap;

	public CalibrationPanel(ActionsWindow parent)
	{
		super(parent);
		buildControls(parent);
	}

	/**
	 * 
	 */
	private void buildControls(ActionsWindow parent)
	{
		_simPanel = new CalibrationSimulationGroupPanel(this);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simPanel, gbc);

		_actionsPanel = new ActionsPanel(parent, this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.VERTICAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_actionsPanel, gbc);
		
		_rightPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = GridBagConstraints.REMAINDER;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_rightPanel, gbc);
	
		buildRightPanel();
	}
	/**
	 * 
	 */
	private void buildRightPanel()
	{
		_nameDescPanel = new NameDescriptionPanel();
		_nameDescPanel.setPanelEditable(false);
		_nameDescPanel.setNameLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5005;
		_rightPanel.add(_nameDescPanel, gbc);
		
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(label, gbc);
		
		_apLabel = new JLabel();
		_apLabel.setComponentPopupMenu(getApPopupMenu());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apLabel, gbc);
		
		label = new JLabel("Start Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		_rightPanel.add(label, gbc);
		
		_apStartLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apStartLabel, gbc);
		
		label = new JLabel("End Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		_rightPanel.add(label, gbc);
		
		_apEndLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apEndLabel, gbc);
		
	
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
		_rightPanel.add(label, gbc);	
		
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
		_rightPanel.add(_simulationTable.getScrollPane(), gbc);
		
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
			_rightPanel.add(legendPanel, gbc);
		
		}
		
		_simActionsPanel = new SimulationActionsPanel(_parentWindow, this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_simActionsPanel, gbc);
		
		_statusList = new RmaJList<>(new RmaListModel<>(false));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5555;
		_rightPanel.add(new JScrollPane(_statusList), gbc);

		_apModListener = new ModifiableListener()
		{
			@Override
			public void modifiedStateChanged(Modifiable modifiable, boolean b)
			{
				fillAnalysisPeriodFields(_ap);
			}
		};
	}


	private JPopupMenu getApPopupMenu()
	{
		JPopupMenu popup = new JPopupMenu();
		JMenuItem editAp = new JMenuItem("Edit...");
		editAp.setToolTipText("Edit the Analysis Period");
		editAp.addActionListener(e->editAnalysisPeriod());
		popup.add(editAp);
		return popup;
	}

	private void editAnalysisPeriod()
	{
		if ( _ap == null )
		{
			return;
		}
		MutableTreeNode node = ((WatFrame) Browser.getBrowserFrame()).getProjectTree().getNodeForManager(_ap);
		if ( node instanceof WatAnalysisPeriodNode)
		{
			WatAnalysisPeriodNode apNode = (WatAnalysisPeriodNode) node;
			apNode.editManager();
		}
	}
	
	/**
	 * @return
	 */
	
	
	public void clearForm()
	{
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");
		_nameDescPanel.setName("");
		_nameDescPanel.setDescription("");
		_simulationTable.setTreeTableModel(new SimulationTreeTableModel(null));
		//_simulationTable.deleteCells();
	}
	@Override
	public void setSimulationGroup(AbstractSimulationGroup asg)
	{
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_simGroup = (SimulationGroup) asg;
		try
		{
			clearForm();
			//_simulationTable.deleteCells();
			if ( asg instanceof SimulationGroup )
			{
				SimulationGroup sg = (SimulationGroup) asg;
				setSimulationTable(sg);
				

				_nameDescPanel.setName(sg.getName());
				_nameDescPanel.setDescription(sg.getDescription());
				_nameDescPanel.setPanelEditable(true);
				_nameDescPanel.setPanelEnabled(true);
				WatAnalysisPeriod ap = sg.getAnalysisPeriod();
				fillAnalysisPeriodFields(ap);

				/*
				List<WatSimulation> sims= sg.getSimulations();
				Vector row;
				WatSimulation sim;
				Color bgColor;
				for (int i = 0;i < sims.size(); i++ )
				{
					row = new Vector();
					sim = sims.get(i);
					row.add(Boolean.FALSE);
					row.add(sim);
					row.add("Show on Map");
					row.add("View Report");
					_simulationTable.appendRow(row);
					if (! Boolean.getBoolean("NoSimulationComputeState"))
					{
						Color color = getSimForegroundColor(sim);
						_simulationTable.setRowForeground(_simulationTable.getRowCount()-1, color);
					}
				}
				*/
			}
			else
			{
				_apLabel.setText("");
				_apStartLabel.setText("");
				_apEndLabel.setText("");	
			}
			_actionsPanel.setSimulationGroup(asg);
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}

	private void fillAnalysisPeriodFields(WatAnalysisPeriod ap)
	{
		String apName = "";
		String apStart = "";
		String apEnd = "";
		if ( ap!= null )
		{
			apName = ap.getName();
			apStart = ap.getRunTimeWindow().getStartTime().toString();
			apEnd = ap.getRunTimeWindow().getEndTime().toString();
		}
		_apLabel.setText(apName);
		_apStartLabel.setText(apStart);
		_apEndLabel.setText(apEnd);
		if (_ap != null )
		{
			_ap.removeModifiableListener(_apModListener);
		}
		_ap = ap;
		if ( _ap != null )
		{
			_ap.addModifiableListener(_apModListener);
		}
	}


	/**
	 * @return
	 */
	@Override
	public SimulationGroup getSimulationGroup()
	{
		return _simGroup;
	}
	
	
	

	/**
	 * @return 
	 * @return
	 */
	public List<ResultsData> getSelectedResults()
	{
		return _simulationTable.getSelectedResults();
	}
	/**
	 * get the info needed from the selected rows in the table to generate a report
	 * @return
	 */
	@Override
	public List<SimulationReportInfo> getSimulationReportInfos()
	{
		List<SimulationReportInfo>simInfos = new ArrayList<>();
		List<WatSimulation> selectedSims = getSelectedSimulations();
		List<ResultsData> selectedResults = getSelectedResults();
		SimulationReportInfo simInfo;
		WatSimulation sim;
		ResultsData results;
		for (int i = 0;i < selectedSims.size(); i++ )
		{
			sim = selectedSims.get(i);
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(sim);
			simInfo.setSimDssFile(sim.getSimulationDssFile());
			simInfo.setSimFolder(sim.getSimulationDirectory());
			simInfo.setName(sim.getName());
			simInfo.setShortName(sim.getName());
			simInfo.setDescription(sim.getDescription());
			simInfo.setLastComputedDate(new Date(sim.getLastComputedDate()).toString());
			simInfo.setIsSimulation(true);
			simInfo.setSimulationGroup(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

			simInfos.add(simInfo);
		}
		for (int i = 0;i < selectedResults.size(); i++ )
		{
			results = selectedResults.get(i);
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(results.getSimulation());
			simInfo.setSimDssFile(findSimulationDssFile(results.getFolder(),results.getSimulation().getSimulationDssFile()));
			simInfo.setSimFolder(results.getFolder());
			String name = results.getSimulation().getName().concat(" - ").concat(results.getName());
			simInfo.setName(name);
			simInfo.setShortName(results.getName());
			simInfo.setDescription(results.getDescription());
			simInfo.setLastComputedDate(new Date(results.getLastComputedTime()).toString());
			simInfo.setIsSimulation(false);
			simInfo.setSimulationGroup(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());

			simInfos.add(simInfo);	
		}
		
		
		
		
		return simInfos;
	}
	
	public CalibrationSimulationGroupPanel getSimulationPanel()
	{
		return _simPanel;
	}

	public void simulationGroupDeleted(ManagerProxy proxy)
	{
		_simPanel.simulationGroupDeleted(proxy);
		_nameDescPanel.setEnabled(false);
	}
	

}
