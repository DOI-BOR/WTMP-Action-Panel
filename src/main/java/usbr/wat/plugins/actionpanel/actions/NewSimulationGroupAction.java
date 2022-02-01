/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class NewSimulationGroupAction extends BaseActionsPanelAction
{
	private ActionsWindow _parent;
	public NewSimulationGroupAction(ActionsWindow parent)
	{
		super("Create Simulation Group...");
		setEnabled(false);
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		SimulationGroup sg = dlg.getSimulationGroup();
		_parent.setSimulationGroup(sg);
	}

}
