/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;

import hec.io.FileManagerImpl;

import hec2.wat.model.WatSimulation;

import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;

/**
 * display the report for the simulation
 * 
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DisplayReportAction extends AbstractAction
{
	private ActionsWindow _parent;

	public DisplayReportAction(ActionsWindow parent)
	{
		super("Display Report...");
		_parent = parent;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		displayReportAction();
	}

	/**
	 * 
	 */
	public void displayReportAction()
	{
		List<WatSimulation> sims = _parent.getSelectedSimulations();
		if ( sims == null || sims.isEmpty() )
		{
			return;
		}
		for (int i = 0;i < sims.size(); i++)
		{
			
			WatSimulation sim = sims.get(i);
			displayReportAction(sim);
		}
	}
	public void displayReportAction(WatSimulation sim)
	{
		if ( sim != null )
		{
			String rptDir = RMAIO.concatPath(sim.getSimulationDirectory(), BaseReportAction.REPORT_DIR);
			String latestFile = findLatestReportFile(rptDir);
			if ( latestFile != null )
			{
				_parent.displayFile(latestFile);
			}
			
		}
	}

	/**
	 * @param rptFile
	 * @return
	 */
	private String findLatestReportFile(String folder)
	{
		RMAFilenameFilter filter = new RMAFilenameFilter("pdf");
		List<String> reportFiles = FileManagerImpl.getFileManager().list(folder, filter, false);
		if ( reportFiles != null && !reportFiles.isEmpty())
		{
			Collections.sort(reportFiles);
			return reportFiles.get(reportFiles.size()-1);
		}
		return null;
				
	}
}
