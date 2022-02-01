/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UpdateModelsAction extends BaseActionsPanelAction
{
	
	private ActionsWindow _parent;
	public UpdateModelsAction(ActionsWindow parent)
	{
		super("Get/Update Models");
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		displayUpdateDialog();

	}
	/**
	 * 
	 */
	private void displayUpdateDialog()
	{
		StudyStorageDialog dlg = new StudyStorageDialog(_parent);
		dlg.setVisible(true);
	}

}
