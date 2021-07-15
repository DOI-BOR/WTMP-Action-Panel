/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
public class EditSimulationGroupAction extends AbstractAction
{

	private ActionsWindow _parent;

	/**
	 * @param parent
	 */
	public EditSimulationGroupAction(ActionsWindow parent)
	{
		super("Edit Simulation...");
		setEnabled(false);
		_parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true);
		SimulationGroup simGroup = _parent.getSimulationGroup();
		dlg.fillForm(simGroup);
		
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		SimulationGroup sg = dlg.getSimulationGroup();
		_parent.setSimulationGroup(sg);
	}

}
