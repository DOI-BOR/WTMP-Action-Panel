/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model;

/**
 * @author mark
 *
 */
public enum ComputeType
{
	Standard("Standard"),
	Iterative("Iterative"),
	PositionAnalysis("Position Analysis");

	private String _name;

	/**
	 * @param string
	 */
	ComputeType(String name)
	{
		_name = name;
	}
	
	public String getName()
	{
		return _name;
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
}
