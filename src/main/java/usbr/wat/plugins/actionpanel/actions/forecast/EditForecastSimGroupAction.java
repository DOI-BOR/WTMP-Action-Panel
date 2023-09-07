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
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class EditForecastSimGroupAction extends AbstractAction
{
	public EditForecastSimGroupAction()
	{
		super("Edit...");
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(ActionPanelPlugin.getInstance().getActionsWindow(), true, "Edit Simulation Group");

		ForecastSimGroup simGroup = ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationGroup();
		dlg.fillForm(simGroup);

		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		AbstractSimulationGroup sg = dlg.getSimulationGroup();
		ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().setSimulationGroup((ForecastSimGroup) sg);
	}

}
