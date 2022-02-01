/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.SelectSimulationGroupDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SelectSimulationGroupAction extends AbstractAction
{
	private ActionsWindow _parent;
	public SelectSimulationGroupAction(ActionsWindow parent)
	{
		super("Select Simulation Group...");
		setEnabled(false);
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		SelectSimulationGroupDialog editor = new SelectSimulationGroupDialog(_parent, true);
		editor.setVisible(true);
		if ( editor.isCanceled())
		{
			return;
		}
		SimulationGroup sg = editor.getSelectedSimulationGroup();
		_parent.setSimulationGroup(sg);
	}

}
