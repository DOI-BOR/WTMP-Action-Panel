/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.DisplayReportsSelector;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DisplayReportSelectorAction extends AbstractAction
{
	private ActionsWindow _parent;
	private DisplayReportsSelector _selector;
	private UsbrPanel _parentPanel;
	public DisplayReportSelectorAction(ActionsWindow parent, UsbrPanel parentPanel)
	{
		super("Create Report...");
		setEnabled(false);
		_parent = parent;
		_parentPanel = parentPanel;
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
		if ( _selector == null )
		{
			_selector = new DisplayReportsSelector(_parent,_parentPanel);
		}
		_selector.setVisible(true);
	}



}
