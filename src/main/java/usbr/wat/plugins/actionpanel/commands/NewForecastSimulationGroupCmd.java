/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.commands;

import java.util.List;

import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class NewForecastSimulationGroupCmd extends AbstractNewSimulationGroupCmd
{

	/**
	 * @param project
	 * @param name
	 * @param descr
	 * @param dir
	 */
	public NewForecastSimulationGroupCmd(Project project, String name, String descr,
			RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims)
	{
		super(project, name, descr, dir, ap, sims);
		
	}

	@Override
	public String getExtension()
	{
		return ForecastSimGroup.FILE_EXT;
	}

	@Override
	public String getManagerClass()
	{
		return ForecastSimGroup.class.getName();
	}

	@Override
	public String getManagerType()
	{
		return "Forecast Simulation Group";
	}

	/**
	 * @return
	 */
	@Override
	public ForecastSimGroup getSimulationGroup()
	{
		return (ForecastSimGroup) super.getSimulationGroup();
	}

}
