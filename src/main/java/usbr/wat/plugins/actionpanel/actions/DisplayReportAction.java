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
import rma.util.RMAFilenameFilterSet;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.io.OutputType;

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
			displayReportAction(sim.getSimulationDirectory());
		}
	}
	public void displayReportAction(String simulationDirectory)
	{
		if ( simulationDirectory != null )
		{
			String rptDir = RMAIO.concatPath(simulationDirectory, BaseReportAction.REPORT_DIR);
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
	private static String findLatestReportFile(String folder)
	{
		OutputType[] outputTypes = OutputType.values();
		RMAFilenameFilterSet filterSet = new RMAFilenameFilterSet("report file types");
		for (int i = 0;i < outputTypes.length;i++ )
		{
			String ext = outputTypes[i].getFileExtension();
			if ( ext.startsWith("."))
			{
				ext = ext.substring(1);
			}
			RMAFilenameFilter filter = new RMAFilenameFilter(ext);
			filterSet.addFilter(filter);
			
		}
		List<String> reportFiles = FileManagerImpl.getFileManager().list(folder, filterSet, false);
		if ( reportFiles != null && !reportFiles.isEmpty())
		{
			Collections.sort(reportFiles);
			return reportFiles.get(reportFiles.size()-1);
		}
		return null;
				
	}
}
