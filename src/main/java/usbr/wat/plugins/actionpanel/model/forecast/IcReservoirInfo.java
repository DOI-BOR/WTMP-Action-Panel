/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mark
 *
 */
public class IcReservoirInfo
{
	private String _reservoirName;
	private List<String> _profileFileNames = new ArrayList<>();

	public IcReservoirInfo()
	{
		super();
	}

	/**
	 * @param string
	 */
	public void setReservoirName(String reservoirName)
	{
		_reservoirName = reservoirName;
	}
	
	public String getReservoirName()
	{
		return _reservoirName;
	}

	/**
	 * @param string
	 */
	public void addProfileFileName(String fileName)
	{
		if ( fileName == null || fileName.trim().isEmpty())
		{
			return;
		}
		_profileFileNames.add(fileName);
	}
	public List<String>getProfileFileNames()
	{
		return _profileFileNames;
	}
}
