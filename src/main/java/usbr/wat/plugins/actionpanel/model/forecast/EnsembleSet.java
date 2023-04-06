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
	private int[] _computedMembers;
	private String _membersToCompute;

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

	public void setSelectedTemperatureTargetSets(TemperatureTargetSet tts)
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

		if ( _computedMembers != null )
		{
			Element computedMembersElem = new Element("ComputedMembers");
			XMLUtilities.createArrayElements(computedMembersElem, _computedMembers);
		}
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

	public BcData getBcData()
	{
		return _bcData;
	}

	public TemperatureTargetSet getTemperatureTargetSet()
	{
		return _tempTargetSet;
	}

	public int[] getComputedMembers()
	{
		return _computedMembers;
	}

	public String getBcDataName()
	{
		return _bcDataName;
	}

	public String getTemperatureTargetSetName()
	{
		return _tempTargetSetName;
	}

	public void setMemberSetToCompute(String members)
	{
		_membersToCompute = members;
	}
	public  String getMemberSetToCompute()
	{
		return _membersToCompute;
	}
}
