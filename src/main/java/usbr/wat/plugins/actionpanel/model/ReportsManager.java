/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mark Ackerman
 *
 */
public class ReportsManager
{
	/** list of registered plugins */
	private static List<ReportPlugin> _plugins = new ArrayList<>();
	private ReportsManager()
	{
		super();
	}
	public static void register(ReportPlugin plugin) 
	{
		if ( plugin == null  )
		{
			return;
		}
		if ( _plugins.contains(plugin))
		{
			return;
		}
		
		_plugins.add(plugin);
		
	}
	
	public static List<ReportPlugin> getPlugins()
	{
		return Collections.unmodifiableList(new ArrayList<>(_plugins));
	}
}
