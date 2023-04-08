/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.List;
import com.rma.util.XMLUtilities;
import hec.lang.NamedType;
import org.jdom.Element;
import rma.util.IntVector;

public class EnsembleSet extends NamedType
{
	private BcData _bcData;
	private TemperatureTargetSet _tempTargetSet;
	private String _bcDataName;
	private String _tempTargetSetName;
	private IntVector _computedMembers = new IntVector();
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
			int[] members = _computedMembers.toArray();
			XMLUtilities.createArrayElements(computedMembersElem, members);
		}
	}

	public boolean loadData(Element myElem)
	{
		if ( myElem == null )
		{
			return false;
		}
		_computedMembers.clear();
		XMLUtilities.loadNamedType(myElem, this);

		_bcDataName = XMLUtilities.getChildElementAsString(myElem, "BcData",  "");

		_tempTargetSetName = XMLUtilities.getChildElementAsString(myElem,"TemperatureTargetSet", "");
		Element computedMembersElem = myElem.getChild("ComputedMembers");
		if ( computedMembersElem != null )
		{
			int[] computedMembers = XMLUtilities.getIntArrayElements(computedMembersElem);
			_computedMembers.addAll(computedMembers);
		}

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

	public IntVector getComputedMembers()
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

	public void addComputedMember(int member)
	{
		_computedMembers.add(member);
	}
}
