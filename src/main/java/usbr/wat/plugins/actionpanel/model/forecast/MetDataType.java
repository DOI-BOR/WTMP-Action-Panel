/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

public enum MetDataType
{

	Historic("Historic"),
	L3MTO("L3MTO"),
	NCAR("NCAR");

	private String _typeName;

	private MetDataType(String typeName)
	{
		_typeName = typeName;
	}

	public String toString()
	{
		return _typeName;
	}


}
