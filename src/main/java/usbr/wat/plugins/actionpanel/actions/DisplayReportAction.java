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

import rma.util.RMAFilenameFilter;
import rma.util.RMAFilenameFilterSet;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.io.OutputType;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;

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
		List<SimulationReportInfo> simInfos = _parent.getSimulationReportInfos();
		if ( simInfos == null || simInfos.isEmpty() )
		{
			return;
		}
		for (int i = 0;i < simInfos.size(); i++)
		{
			
			SimulationReportInfo simInfo = simInfos.get(i);
			displayReportAction(simInfo.getSimFolder());
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
			filter.setAcceptDirectories(false);
			filterSet.addFilter(filter);
			
		}
		List<String> reportFiles = FileManagerImpl.getFileManager().list(folder, filterSet, false);
		if ( reportFiles != null && !reportFiles.isEmpty())
		{
			int offset = 1;
			Collections.sort(reportFiles);
			String fullPath, fileName;
			do
			{
				fullPath = reportFiles.get(reportFiles.size()-offset);
				fileName = RMAIO.getFileFromPath(fullPath);
				offset++;
			}
			while ( fileName.startsWith("~$"));
			
			return fullPath;
		}
		return null;
				
	}
}
