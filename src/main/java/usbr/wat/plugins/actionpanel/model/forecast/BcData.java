/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.List;
import com.rma.util.XMLUtilities;
import hec.lang.NamedType;
import org.jdom.Element;

public class BcData extends NamedType
{
	private String _opsDataName = "";
	private String _metDataName = "";
	public BcData()
	{
		super();
	}

	public void saveData(Element parent)
	{
		Element myElem = new Element("BcData");
		parent.addContent(myElem);
		XMLUtilities.saveNamedType(myElem, this);

		Element opsElem = new Element("Operations");
		myElem.addContent(opsElem);
		opsElem.setText(_opsDataName);

		Element metElem = new Element("Meteorology");
		myElem.addContent(metElem);
		metElem.setText(_metDataName);
	}

	public boolean loadData(Element myElem)
	{
		if ( myElem == null )
		{
			return false;
		}
		XMLUtilities.loadNamedType(myElem, this);

		_opsDataName = XMLUtilities.getChildElementAsString(myElem, "Operations",  "");

		_metDataName = XMLUtilities.getChildElementAsString(myElem,"Meteorology", "");

		return true;
	}

	public void setSelectedOps(OperationsData opsData)
	{
		_opsDataName = "";
		if ( opsData != null )
		{
			_opsDataName = opsData.getName();
		}
	}

	public void setSelectedMet(MeteorlogicData metData)
	{
		_metDataName = "";
		if ( metData != null )
		{
			_metDataName = metData.getName();
		}
	}

	public String getOpsDataName()
	{
		return _opsDataName;
	}

	public String getMetDataName()
	{
		return _metDataName;
	}
}
