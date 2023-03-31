/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import org.jdom.Element;

import com.rma.util.XMLUtilities;

import hec.lang.NamedType;

/**
 * class for holding temperature target information
 * 
 * @author mark
 *
 */
public class TemperatureTarget extends NamedType
{
	public TemperatureTarget()
	{
		super();
	}
	
	public boolean saveData(Element parent)
	{
		Element myElem = new Element("TemperatureTarget");
		XMLUtilities.saveNamedType(myElem, this);
		parent.addContent(myElem);
		return true;	
	}
	
	public boolean loadData(Element myElem)
	{
		XMLUtilities.loadNamedType(myElem, this);
		return true;
	}
	
}
