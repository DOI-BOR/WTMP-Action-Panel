/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.ui.forecast;

import hec.lang.NamedType;

public class DssLocation extends NamedType
{
	private String _dssFile;
	private String _dssPath;

	public DssLocation(String name,String dssFile, String dssPath)
	{
		super(name);
		_dssFile = dssFile;
		_dssPath = dssPath;
	}

	public String getDssPath()
	{
		return _dssPath;
	}

	public String getDssFile()
	{
		return _dssFile;
	}


}
