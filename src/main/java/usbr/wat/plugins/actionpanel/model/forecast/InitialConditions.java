/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Element;

import hec.lang.NamedType;

/**
 * @author mark
 *
 */
public class InitialConditions extends NamedType
{
	private Map<String, List<String>> _icMap = new HashMap<>();

	public InitialConditions()
	{
		super();
	}

	/**
	 * @param resName
	 * @param selectedProfileNames
	 */
	public void putSelectedProfiles(String resName,
			List<String> selectedProfileNames)
	{
		if ( resName != null && selectedProfileNames != null )
		{
			_icMap.put(resName, selectedProfileNames);
		}
	}
	
	public List<String>getSelectedProfiles(String resName)
	{
		return _icMap.get(resName);
	}

	public List<String> getReservoirs()
	{
		return new ArrayList<>(_icMap.keySet());
	}

	/**
	 * @param icElem
	 */
	public void saveData(Element icElem)
	{
		Set<Entry<String, List<String>>> entrySet = _icMap.entrySet();
		Iterator<Entry<String, List<String>>> iter = entrySet.iterator();
		String resName;
		List<String>profiles;
		Element resElem;
		Element profElem;
		while (iter.hasNext())
		{
			Entry<String, List<String>> entry = iter.next();
			resName = entry.getKey();
			profiles = entry.getValue();
			resElem = new Element("Reservoir");
			resElem.setAttribute("Name", resName);
			icElem.addContent(resElem);
			for (int i = 0;i < profiles.size(); i++ )
			{
				profElem = new Element("Profile");
				profElem.setAttribute("Name", profiles.get(i));
				resElem.addContent(profElem);
			}
			
		}
	}

	/**
	 * @param icElem
	 */
	public void loadData(Element icElem)
	{
		_icMap.clear();
		if ( icElem == null )
		{
			return;
		}
		List resElems = icElem.getChildren();
		List profElems;
		Element resElem, profileElem;
		String reservoirName, profileName;
		for (int r = 0; r < resElems.size(); r++ )
		{
			resElem = (Element) resElems.get(r);
			reservoirName = resElem.getAttributeValue("Name");
			profElems = resElem.getChildren();
			if ( profElems != null )
			{
				List<String>profileNames = new ArrayList<>();
				for(int p = 0; p < profElems.size(); p++ )
				{
					profileElem = (Element) profElems.get(p);
					profileName = profileElem.getAttributeValue("Name");
					profileNames.add(profileName);
					
				}
				_icMap.put(reservoirName, profileNames);
			}
		}
		
		
	}
}
