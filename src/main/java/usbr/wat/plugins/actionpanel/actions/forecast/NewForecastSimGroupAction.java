/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.commands.NewForecastSimulationGroupCmd;
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;

/**
 * @author mark
 *
 */
public class NewForecastSimGroupAction extends AbstractAction
{
	private AbstractSimulationPanel _parent;
	public NewForecastSimGroupAction(AbstractSimulationPanel simulationPanel)
	{
		super("New...");
		setEnabled(false);
		_parent = simulationPanel;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(ActionPanelPlugin.getInstance().getActionsWindow(), true, 
			"New Forecast Simulation Group");
		dlg.setSimulationGroupClass(ForecastSimGroup.class);
		dlg.setSimulationGroupFactory(NewForecastSimulationGroupCmd.class);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		AbstractSimulationGroup sg = dlg.getSimulationGroup();
		_parent.setSimulationGroup(sg);
	}

}
