/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.commands.NewSimulationGroupCmd;
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;
import usbr.wat.plugins.actionpanel.ui.CalibrationSimulationGroupPanel;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class NewSimulationGroupAction extends BaseActionsPanelAction
{
	private final CalibrationSimulationGroupPanel _simGroupPanel;
	private CalibrationPanel _parent;
	public NewSimulationGroupAction(CalibrationPanel calibrationPanel, CalibrationSimulationGroupPanel simGroupPanel)
	{
		super("New...");
		setEnabled(false);
		_parent = calibrationPanel;
		_simGroupPanel = simGroupPanel;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(ActionPanelPlugin.getInstance().getActionsWindow(), true,
				"New Simulation Group");
		dlg.setSimulationGroupClass(SimulationGroup.class);
		dlg.setSimulationGroupFactory(NewSimulationGroupCmd.class);
		dlg.setRunExtract(true);
		dlg.fillForm();
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		SimulationGroup sg = (SimulationGroup) dlg.getSimulationGroup();
		if(_parent != null)
		{
			_parent.setSimulationGroup(sg);
		}
		_simGroupPanel.addSimulationGroup(sg, true);
	}

}
