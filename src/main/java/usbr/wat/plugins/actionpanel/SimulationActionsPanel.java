/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationResultsAction;
import usbr.wat.plugins.actionpanel.actions.DisplayReportSelectorAction;
import usbr.wat.plugins.actionpanel.actions.RunSimulationAction;
import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction;
import usbr.wat.plugins.actionpanel.actions.forecast.RunForecastSimulationAction;
import usbr.wat.plugins.actionpanel.model.ForecastReportingPlugin;
import usbr.wat.plugins.actionpanel.model.ReportPlugin;
import usbr.wat.plugins.actionpanel.model.ReportsManager;
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;

/**
 * panel for the Simulation Actions
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class SimulationActionsPanel extends EnabledJPanel
{
	private ActionsWindow _parent;
	
	private Action _runSimulationAction;
	private DisplayReportSelectorAction _displayReportsSelectorAction;

	private SaveSimulationResultsAction _saveResultsAction;

	private DeleteSimulationResultsAction _deleteResultsAction;

	private UsbrPanel _parentPanel;
	private Action _displayEnsembleSelectorAction;

	public SimulationActionsPanel(ActionsWindow parent, UsbrPanel parentPanel)
	{
		super(new GridBagLayout());
		_parent = parent;
		_parentPanel = parentPanel;
		buildControls();
		
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		if ( _parentPanel instanceof CalibrationPanel)
		{
			_runSimulationAction = new RunSimulationAction(_parent, _parentPanel);
		}
		else
		{
			_runSimulationAction = new RunForecastSimulationAction(_parent, (SimulationPanel)_parentPanel);
		}
		JButton button = new JButton(_runSimulationAction);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		add(button, gbc);
		List<ReportPlugin> plugins = ReportsManager.getPlugins();
		if ( _parentPanel instanceof CalibrationPanel)
		{
			_displayReportsSelectorAction = new DisplayReportSelectorAction(_parent, _parentPanel);
			button = new JButton(_displayReportsSelectorAction);
		}
		else
		{
			for (int i = 0;i < plugins.size(); i++ )
			{
				if (plugins.get(i) instanceof ForecastReportingPlugin)
				{
					ForecastReportingPlugin fplugin = (ForecastReportingPlugin) plugins.get(i);
					_displayEnsembleSelectorAction = fplugin.getReportAction(_parent, _parentPanel);
					button = new JButton(_displayEnsembleSelectorAction);
					break;
				}
			}
		}
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		if ( button != null )
		{
			add(button, gbc);
		}
		
		_saveResultsAction = new SaveSimulationResultsAction(_parent, _parentPanel);
		button = new JButton(_saveResultsAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
		
		_deleteResultsAction = new DeleteSimulationResultsAction(_parent, _parentPanel);
		button = new JButton(_deleteResultsAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
	}
	

	public void updateActions()
	{
		if ( _parent.getSimulationGroup() == null )
		{
			return;
		}
		boolean simActionsEnabled = _parent.getSelectedSimulations().size() > 0;
		boolean resultsActionsEnabled = _parent.getSelectedResults().size() > 0;
		
		_runSimulationAction.setEnabled(simActionsEnabled);
		_saveResultsAction.setEnabled(simActionsEnabled);
		if ( _displayReportsSelectorAction != null )
		{
			_displayReportsSelectorAction.setEnabled(resultsActionsEnabled || simActionsEnabled);
		}
		
		_deleteResultsAction.setEnabled(resultsActionsEnabled );
	}
}
