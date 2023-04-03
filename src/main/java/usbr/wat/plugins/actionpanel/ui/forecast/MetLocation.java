/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.ui.forecast;

import java.util.ArrayList;
import java.util.List;
import hec.lang.NamedType;

public class MetLocation extends NamedType
{
	private List<DssLocation> _dssLocations = new ArrayList<>();

	public MetLocation()
	{
		super();
	}

	public List<DssLocation> getDssLocations()
	{
		return _dssLocations;
	}

	public void setDssLocations(List<DssLocation>locations)
	{
		_dssLocations.clear();
		if ( locations != null )
		{
			_dssLocations.addAll(locations);
		}
	}

	public void addDssLocation(DssLocation location)
	{
		_dssLocations.add(location);
	}




}
