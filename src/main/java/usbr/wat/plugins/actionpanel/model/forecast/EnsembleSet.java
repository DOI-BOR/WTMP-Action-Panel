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

public class EnsembleSet extends NamedType
{
	private BcData _bcData;
	private TemperatureTargetSet _tempTargetSet;
	private String _bcDataName;
	private String _tempTargetSetName;

	public EnsembleSet()
	{
		super();
	}

	public void setSelectedBcData(BcData bc)
	{
		_bcData = bc;
		if ( _bcData != null )
		{
			_bcDataName = _bcData.getName();
		}
		else
		{
			_bcDataName = "";
		}
	}

	public void setSelectedTempuratureTargetSets(TemperatureTargetSet tts)
	{
		_tempTargetSet = tts;
		if ( _tempTargetSet != null )
		{
			_tempTargetSetName = _tempTargetSet.getName();
		}
		else
		{
			_tempTargetSetName = "";
		}
	}
	public void saveData(Element parent)
	{
		Element myElem = new Element("EnsembleSet");
		parent.addContent(myElem);
		XMLUtilities.saveNamedType(myElem, this);

		Element bcElem = new Element("BcData");
		myElem.addContent(bcElem);
		bcElem.setText(_bcDataName);

		Element ttsElem = new Element("TemperatureTargetSet");
		myElem.addContent(ttsElem);
		ttsElem.setText(_tempTargetSetName);
	}

	public boolean loadData(Element myElem)
	{
		if ( myElem == null )
		{
			return false;
		}
		XMLUtilities.loadNamedType(myElem, this);

		_bcDataName = XMLUtilities.getChildElementAsString(myElem, "BcData",  "");

		_tempTargetSetName = XMLUtilities.getChildElementAsString(myElem,"TemperatureTargetSet", "");

		return true;
	}
}
