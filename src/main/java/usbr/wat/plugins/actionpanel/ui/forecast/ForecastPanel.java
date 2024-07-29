/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JTabbedPane;

import hec2.wat.model.WatSimulation;
import rma.swing.RmaInsets;
import rma.swing.RmaJPanel;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.SimulationGroupPanel;
import usbr.wat.plugins.actionpanel.ui.forecast.temptarget.TempTargetPanel;

/**
 * @author mark
 *
 */
public class ForecastPanel extends RmaJPanel
{

	private ActionsWindow _parent;
	private SimulationGroupPanel _simGroupPanel;
	private JTabbedPane _tabbedPane;
	private InitialConditionsPanel _initialConditionsPanel;
	private OperationsPanel _operationsPanel;
	private MeteorologyPanel _metPanel;
	private BcPanel _bcPanel;
	private TempTargetPanel _tempTargetsPanel;
	private SimulationPanel _simulationPanel;
	private ForecastSimGroup _simGroup;
	private AbstractForecastPanel _currentPanel;

	/**
	 * @param parent
	 */
	public ForecastPanel(ActionsWindow parent)
	{
		super(new GridBagLayout());
		_parent = parent;
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		_simulationPanel = new SimulationPanel(_parent,this);
		_initialConditionsPanel = new InitialConditionsPanel(this);
		_operationsPanel = new OperationsPanel(this);
		_metPanel = new MeteorologyPanel(this);
		_tempTargetsPanel = new TempTargetPanel(this);
		_bcPanel = new BcPanel(this);
		
		_simulationPanel.setEnabled(false);
		_initialConditionsPanel.setEnabled(false);
		_operationsPanel.setEnabled(false);
		_metPanel.setEnabled(false);
		_tempTargetsPanel.setEnabled(false);
		_bcPanel.setEnabled(false);
		
		_simGroupPanel = new SimulationGroupPanel(_simulationPanel);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_simGroupPanel, gbc);
		
		_tabbedPane = new JTabbedPane();
		String pos = System.getProperty("WTMP.ForecastTabs.Placement");
		int tabPlacement = JTabbedPane.LEFT;
		if ( "left".equalsIgnoreCase(pos))
		{
			tabPlacement = JTabbedPane.LEFT;
		}
		else if ( "right".equalsIgnoreCase(pos))
		{
			tabPlacement = JTabbedPane.RIGHT;
		}
		else if ("bottom".equalsIgnoreCase(pos))
		{
			tabPlacement = JTabbedPane.BOTTOM;
		}
		else if ( "top".equalsIgnoreCase(pos))
		{
			tabPlacement = JTabbedPane.TOP;
		}
		_tabbedPane.setTabPlacement(tabPlacement);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);
		
		

		_tabbedPane.addTab("Initial Conditions", _initialConditionsPanel);
		_tabbedPane.addTab("Operations", _operationsPanel);
		_tabbedPane.addTab("Meteorology", _metPanel);
		_tabbedPane.addTab("Boundary Conditions", _bcPanel);
		_tabbedPane.addTab("Temperature Targets", _tempTargetsPanel);
		_tabbedPane.addTab("Simulation", _simulationPanel);

		_currentPanel = (AbstractForecastPanel) _tabbedPane.getSelectedComponent();
		
		
	}
	
	/**
	 * 
	 */
	private void addListeners()
	{
		_tabbedPane.addChangeListener(e->tabSelectionChanged());
	}


	/**
	 * @return
	 */
	private void tabSelectionChanged()
	{
		Component comp = _tabbedPane.getSelectedComponent();
		if ( comp instanceof AbstractForecastPanel )
		{
			if ( _currentPanel != null )
			{
				_currentPanel.savePanel();
			}
			AbstractForecastPanel panel = (AbstractForecastPanel) comp;
			panel.panelActivated();
			_currentPanel = panel;
		}
	}



	/**
	 * @return
	 */
	public ForecastSimGroup getSimulationGroup()
	{
		return _simGroup;
	}



	/**
	 * @param fsg
	 */
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		_simGroup = fsg;
		if(fsg != null)
		{
			_simGroupPanel.setSimulationGroup(fsg);
			_initialConditionsPanel.setSimulationGroup(fsg);
			_operationsPanel.setSimulationGroup(fsg);
			_metPanel.setSimulationGroup(fsg);
			_tempTargetsPanel.setSimulationGroup(fsg);
			_bcPanel.setSimulationGroup(fsg);
		}
		else
		{
			clearPanel();
		}
	}

	private void clearPanel()
	{
		_initialConditionsPanel.clearPanel();
		_operationsPanel.clearPanel();
		_metPanel.clearPanel();
		_bcPanel.clearPanel();
		_tempTargetsPanel.clearPanel();
		_currentPanel.clearTables();
	}

	public void loadSimulationGroupCombo()
	{
		_simGroupPanel.loadSimulationGroupCombo();
	}

	public void setSelectedTab(AbstractForecastPanel panel)
	{
		if (panel != null )
		{
			_tabbedPane.setSelectedComponent(panel);
		}
	}

	public List<WatSimulation> getSelectedSimulations()
	{
		return _simulationPanel.getSelectedSimulations();
	}

	public List<ResultsData> getSelectedResults()
	{
		return _simulationPanel.getSelectedResults();
	}

	public SimulationPanel getSimulationPanel()
	{
		return _simulationPanel;
	}

	/**
	 * the highlighted simulation in the table
	 * @return
	 */
	public WatSimulation getSelectedSimulation()
	{
		return _simulationPanel.getSelectedSimulation();
	}

	public void refreshSimulationPanel(ForecastSimGroup fsg)
	{
		WatSimulation simulation = getSelectedSimulation();
		_simulationPanel.fillSimulationTable();
		_simulationPanel.setEnsembleSets(fsg.getEnsembleSets(simulation));
		_simulationPanel.fillAnalysisWindow();
		setEnabled(true);
		_simulationPanel.refreshSimTableSelection();
	}

	@Override
	public void setVisible(boolean visible)
	{
		if(visible && _simGroup == null)
		{
			clearPanel();
			_simulationPanel.closing();
		}
		super.setVisible(visible);

	}
}
