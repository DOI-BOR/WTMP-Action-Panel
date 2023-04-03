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
import rma.util.RMAIO;

public class MeteorlogicData extends NamedType
{
	private int _year;
	private MetDataType _metDataType = MetDataType.Historic;
	public MeteorlogicData()
	{
		super();
	}

	public void setMetDataType(MetDataType datatype)
	{
		_metDataType = datatype;
	}

	public MetDataType getMetDataType()
	{
		return _metDataType;
	}


	public void setYear(int year)
	{
		_year = year;
	}

	public int getYear()
	{
		return _year;
	}


	public void saveData(Element parentElem)
	{
		Element myElem = new Element("MeteorologyData");
		parentElem.addContent(myElem);

		XMLUtilities.saveNamedType(myElem, this);
		Element dataTypeElem = new Element("MetDataType");
		dataTypeElem.setText(_metDataType.name());
		myElem.addContent(dataTypeElem);
		Element yearElem = new Element("Year");
		yearElem.setText(String.valueOf(_year));
		myElem.addContent(yearElem);
	}

	public boolean loadData(Element myElem)
	{
		XMLUtilities.loadNamedType(myElem, this);
		Element dataTypeElem = myElem.getChild("MetDataType");
		if ( dataTypeElem  != null )
		{
			String dataTypeStr = dataTypeElem.getTextTrim();
			_metDataType = MetDataType.valueOf(dataTypeStr);
		}
		Element yearElem = myElem.getChild("Year");
		_year = XMLUtilities.getContentAsInt(yearElem, 0);
		return true;
	}
}
