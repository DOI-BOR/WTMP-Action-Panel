/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.commands;

import java.util.List;

import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
public class NewSimulationGroupCmd extends AbstractNewSimulationGroupCmd
{

	/**
	 * @param project
	 * @param name
	 * @param descr
	 * @param dir
	 */
	public NewSimulationGroupCmd(Project project, String name, String descr,
			RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims)
	{
		super(project, name, descr, dir, ap, sims);
		
	}

	@Override
	public String getExtension()
	{
		return SimulationGroup.FILE_EXT;
	}

	@Override
	public String getManagerClass()
	{
		return SimulationGroup.class.getName();
	}

	@Override
	public String getManagerType()
	{
		return "Simulation Group";
	}

	/**
	 * @return
	 */
	@Override
	public SimulationGroup getSimulationGroup()
	{
		return (SimulationGroup) super.getSimulationGroup();
	}

	

}
