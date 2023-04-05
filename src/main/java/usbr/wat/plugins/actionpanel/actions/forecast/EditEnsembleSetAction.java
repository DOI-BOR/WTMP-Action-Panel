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
import java.util.List;
import javax.swing.AbstractAction;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.forecast.EditEnsembleSetWindow;
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;

public class EditEnsembleSetAction extends AbstractAction
{
	private final SimulationPanel _parentPanel;

	public EditEnsembleSetAction(SimulationPanel parentPanel)
	{
		super("Edit Ensemble Set...");
		_parentPanel = parentPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		editEnsembleSetAction();
	}

	public void editEnsembleSetAction()
	{
		EditEnsembleSetWindow dlg = new EditEnsembleSetWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		ForecastSimGroup simGroup = ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationGroup();
		dlg.fillForm(simGroup);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		List<EnsembleSet> ensembleSets = dlg.getEnsembleSets();
		simGroup.setEnsembleSets(ensembleSets);
		_parentPanel.setEnsembleSets(ensembleSets);


	}


}
