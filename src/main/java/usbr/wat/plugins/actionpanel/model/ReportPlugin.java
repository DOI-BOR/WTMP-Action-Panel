/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.model;

/**
 * @author Mark Ackerman
 *
 */
public interface ReportPlugin
{

	/**
	 * @return 
	 * 
	 */
	boolean createReport();

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

}
