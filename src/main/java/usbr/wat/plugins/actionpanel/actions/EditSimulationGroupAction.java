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
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;

/**
 * @author Mark Ackerman
 *
 */
public class EditSimulationGroupAction extends AbstractAction
{

	private ActionsWindow _parent;
	private AbstractSimulationPanel _parentPanel;

	/**
	 * @param parent
	 */
	public EditSimulationGroupAction(ActionsWindow parent, AbstractSimulationPanel parentPanel)
	{
		super("Edit Simulation Group...");
		setEnabled(false);
		_parent = parent;
		_parentPanel = parentPanel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true, "Edit Simulation Group");
			
		SimulationGroup simGroup = _parent.getSimulationGroup();
		dlg.fillForm(simGroup);
		
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		AbstractSimulationGroup sg = dlg.getSimulationGroup();
		_parentPanel.setSimulationGroup(sg);
	}

}
