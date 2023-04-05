/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ui.forecast.EditEnsembleSetWindow;

public class EditEnsembleSetAction extends AbstractAction
{
	public EditEnsembleSetAction()
	{
		super("Edit Ensemble Set...");
	}

	public void actionPerformed(ActionEvent e)
	{
		editEnsembleSetAction();
	}

	public void editEnsembleSetAction()
	{
		EditEnsembleSetWindow dlg = new EditEnsembleSetWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationGroup());
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
	}


}
