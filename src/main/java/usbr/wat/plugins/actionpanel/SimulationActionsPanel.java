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

import javax.swing.JButton;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationResultsAction;
import usbr.wat.plugins.actionpanel.actions.DisplayReportSelectorAction;
import usbr.wat.plugins.actionpanel.actions.RunSimulationAction;
import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;

/**
 * panel for the Simulation Actions
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class SimulationActionsPanel extends EnabledJPanel
{
	private ActionsWindow _parent;
	
	private RunSimulationAction _runSimulationAction;
	private DisplayReportSelectorAction _displayReportsSelectorAction;

	private SaveSimulationResultsAction _saveResultsAction;

	private DeleteSimulationResultsAction _deleteResultsAction;

	private UsbrPanel _parentPanel;
	
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
		_runSimulationAction = new RunSimulationAction(_parent, _parentPanel);
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
	
		_displayReportsSelectorAction = new DisplayReportSelectorAction(_parent, _parentPanel);
		button = new JButton(_displayReportsSelectorAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
		
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
		
		_displayReportsSelectorAction.setEnabled(resultsActionsEnabled || simActionsEnabled);
		
		_deleteResultsAction.setEnabled(resultsActionsEnabled );
	}
}
