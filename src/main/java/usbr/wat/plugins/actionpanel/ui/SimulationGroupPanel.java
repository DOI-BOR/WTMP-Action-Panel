/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;

import javax.swing.Action;
import javax.swing.JTabbedPane;

import com.rma.model.ManagerProxy;

import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.forecast.DeleteForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.actions.forecast.EditForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.actions.forecast.NewForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class SimulationGroupPanel extends BaseSimulationGroupPanel
{
	private AbstractSimulationPanel _parent;

	private JTabbedPane _tabbedPane;

	public SimulationGroupPanel(AbstractSimulationPanel parent)
	{
		super(parent);
		_parent = parent;
	}

	/**
	 * 
	 */
	protected void buildControls()
	{
		super.buildControls();;

		_tabbedPane = new JTabbedPane();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);
		
	}

	@Override
	protected Action getDeleteSimGroupAction()
	{
		return new DeleteForecastSimGroupAction(ActionPanelPlugin.getInstance().getActionsWindow());
	}

	@Override
	protected Action getNewSimGroupAction()
	{
		return new NewForecastSimGroupAction(_parent, this);
	}

	@Override
	protected Action getEditSimGroupAction()
	{
		return new EditForecastSimGroupAction();
	}


	/**
	 * @param e
	 * @return
	 */
	protected void simGroupSelected(ItemEvent e)
	{
		ManagerProxy proxy = (ManagerProxy) _simulationGroupCombo.getSelectedItem();
		if ( e != null && ItemEvent.DESELECTED == e.getStateChange() && proxy != null)
		{
			return;
		}
		ForecastSimGroup simGroup = null;
		if(proxy != null)
		{
			simGroup = (ForecastSimGroup) proxy.loadManager();
		}
		fillForm(simGroup);
	}

	@Override
	protected Class getSimGroupClass()
	{
		return ForecastSimGroup.class;
	}

	/**
	 * @param simGroup
	 */
	private void fillForm(ForecastSimGroup simGroup)
	{
		boolean enabled = simGroup != null;
		_editButton.setEnabled(enabled);
		_parent.setSimulationGroup(simGroup);
	}





}
