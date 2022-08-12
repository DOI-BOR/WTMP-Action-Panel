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

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ActionWindowAction extends AbstractAction
{
	public ActionWindowAction()
	{
		super("WTMP Actions Window");
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		ActionPanelPlugin.getInstance().displayActionsWindow();
	}

}
