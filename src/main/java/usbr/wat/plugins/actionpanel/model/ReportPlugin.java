/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.model;

import java.util.List;

import usbr.wat.plugins.actionpanel.io.ReportOptions;

/**
 * @author Mark Ackerman
 *
 */
public interface ReportPlugin
{

	/**
	 * @param sris 
	 * @return 
	 * 
	 */
	boolean createReport(List<SimulationReportInfo> sris, ReportOptions options);

	/**
	 * @return
	 */
	String getName();
	String getDescription();

	/**
	 * @return
	 */
	boolean isComparisonReport();
	boolean isIterationReport();

	/**
	 * @return
	 */
	String getMavenPath();

}
