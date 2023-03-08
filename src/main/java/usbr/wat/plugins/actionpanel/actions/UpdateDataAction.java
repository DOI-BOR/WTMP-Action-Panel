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

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.extract.ui.ExtractDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UpdateDataAction extends AbstractAction
{
	private ActionsWindow _parent;
	public UpdateDataAction(ActionsWindow parent)
	{
		super("Get/Update Data");
		setEnabled(false);
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		updateData(_parent.getSimulationGroup());
	}
	/**
	 * @param simulationGroup
	 */
	public void updateData(SimulationGroup simulationGroup)
	{
		ExtractDialog dlg = new ExtractDialog(ActionPanelPlugin.getInstance().getActionsWindow(), simulationGroup);
		dlg.setVisible(true);
	}

}
