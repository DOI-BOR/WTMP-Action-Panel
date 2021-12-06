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
import usbr.wat.plugins.actionpanel.editors.DisplayReportsSelector;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DisplayReportSelectorAction extends AbstractAction
{
	private ActionsWindow _parent;
	public DisplayReportSelectorAction(ActionsWindow parent)
	{
		super("Create Report...");
		setEnabled(false);
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		displayReportSelector();
	}
	/**
	 * 
	 */
	private void displayReportSelector()
	{
		DisplayReportsSelector selector = new DisplayReportsSelector(_parent);
		selector.setVisible(true);
	}

}
