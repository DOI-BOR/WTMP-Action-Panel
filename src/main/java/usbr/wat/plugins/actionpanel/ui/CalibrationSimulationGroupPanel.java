/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import javax.swing.Action;
import javax.swing.JSeparator;
import com.rma.model.ManagerProxy;
import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.EditSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.NewSimulationGroupAction;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

public class CalibrationSimulationGroupPanel extends BaseSimulationGroupPanel
{
	public CalibrationSimulationGroupPanel(CalibrationPanel calibrationPanel)
	{
		super(calibrationPanel);
	}

	protected void buildControls()
	{
		super.buildControls();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);
	}


	@Override
	protected Action getDeleteSimGroupAction()
	{
		return new DeleteSimulationGroupAction(ActionPanelPlugin.getInstance().getActionsWindow());
	}

	@Override
	protected Action getNewSimGroupAction()
	{
		return new NewSimulationGroupAction((CalibrationPanel) _parent);
	}

	@Override
	protected Action getEditSimGroupAction()
	{
		return new EditSimulationGroupAction(ActionPanelPlugin.getInstance().getActionsWindow(), _parent);
	}

	@Override
	protected void simGroupSelected(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		ManagerProxy proxy = (ManagerProxy) _simulationGroupCombo.getSelectedItem();
		SimulationGroup simGroup = (SimulationGroup) proxy.loadManager();
		fillForm(simGroup);
	}
	private void fillForm(SimulationGroup simGroup)
	{
		boolean enabled = simGroup != null;
		_editButton.setEnabled(enabled);
		_parent.setSimulationGroup(simGroup);
	}

	@Override
	protected Class getSimGroupClass()
	{
		return SimulationGroup.class;
	}
}
