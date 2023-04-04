/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.util.XMLUtilities;
import hec.lang.NamedType;
import org.jdom.Element;

public class OperationsData extends NamedType
{
	private String _opsFile;

	public OperationsData()
	{
		super();
	}

	public boolean loadData(Element myElem)
	{
		if ( myElem == null )
		{
			return false;
		}
		XMLUtilities.loadNamedType(myElem, this);
		_opsFile = XMLUtilities.getChildElementAsString(myElem, "OperationsFile", "");
		return true;
	}

	public void saveData(Element parent)
	{
		Element myElem= new Element("OperationsData");
		parent.addContent(myElem);
		XMLUtilities.saveNamedType(myElem, this);
		Element opsFileElem = new Element("OperationsFile");
		opsFileElem.setText(_opsFile);
		myElem.addContent(opsFileElem);
	}

	public void setOperationsFile(String opsFile)
	{
		_opsFile = opsFile;
	}
	public String getOperationsFile()
	{
		return _opsFile;
	}

}
