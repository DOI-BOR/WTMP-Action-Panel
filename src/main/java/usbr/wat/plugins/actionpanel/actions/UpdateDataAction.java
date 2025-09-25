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
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UpdateDataAction extends AbstractAction
{
	public static final String DASH_D_FLAG = "WTMP.HasUpdateData";

	private final Runnable _postUpdateAction;

	public UpdateDataAction()
	{
		this(null);
	}

	public UpdateDataAction(Runnable postUpdateAction)
	{
		super("Get/Update Data");
		setEnabled(false);
		_postUpdateAction = postUpdateAction;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		updateData(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationGroup());
	}
	/**
	 * @param simulationGroup
	 */
	public void updateData(AbstractSimulationGroup simulationGroup)
	{
		ExtractDialog dlg = new ExtractDialog(ActionPanelPlugin.getInstance().getActionsWindow(), simulationGroup, _postUpdateAction);
		dlg.setVisible(true);
	}

}
