/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.flogger.FluentLogger;
import com.rma.model.Project;
import hec.heclib.dss.DSSPathname;
import org.jdom.Element;

import hec.lang.NamedType;

/**
 * @author mark
 *
 */
public class InitialConditions extends NamedType
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private Map<String, Profile> _icMap = new HashMap<>();

	public InitialConditions()
	{
		super();
	}

	/**
	 * @param resName
	 * @param selectedProfile
	 */
	public void putSelectedProfile(String resName, Profile selectedProfile)
	{
		if ( resName != null && selectedProfile != null )
		{
			_icMap.put(resName, selectedProfile);
		}
	}

	public Profile getSelectedProfile(String resName)
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
		Set<Entry<String, Profile>> entrySet = _icMap.entrySet();
		Iterator<Entry<String, Profile>> iter = entrySet.iterator();
		String resName;
		Profile profile;
		Element resElem;
		Element profElem;
		while (iter.hasNext())
		{
			Entry<String, Profile> entry = iter.next();
			resName = entry.getKey();
			profile = entry.getValue();
			resElem = new Element("Reservoir");
			resElem.setAttribute("Name", resName);
			icElem.addContent(resElem);
			profElem = new Element("Profile");
			profElem.setAttribute("Name", profile.getName());
			Element fileElem = new Element("Output-DSS-File");
			fileElem.setText(Project.getCurrentProject().getRelativePath(profile.getDssFileName()));
			Element pathnameElement = new Element("DSS-Pathname");
			DSSPathname dssPathname = new DSSPathname();
			if(profile.getDssPath() != null)
			{
				dssPathname = profile.getDssPath();
			}
			pathnameElement.setText(dssPathname.toString());
			profElem.addContent(fileElem);
			profElem.addContent(pathnameElement);
			resElem.addContent(profElem);
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
		Element resElem;
		String reservoirName, profileName;
		for (int r = 0; r < resElems.size(); r++ )
		{
			resElem = (Element) resElems.get(r);
			reservoirName = resElem.getAttributeValue("Name");
			profElems = resElem.getChildren();
			if ( profElems != null && !profElems.isEmpty())
			{
				Element profileElem = (Element) profElems.get(0);
				profileName = profileElem.getAttributeValue("Name");
				Element fileElem = profileElem.getChild("Output-DSS-File");
				String dssFileName = null;
				if(fileElem != null)
				{
					dssFileName = fileElem.getText();
				}
				String dssPathName = null;
				Element pathnameElement = profileElem.getChild("DSS-Pathname");
				if(pathnameElement != null)
				{
					dssPathName = pathnameElement.getText();
				}
				try
				{
					Profile profile = new Profile(profileName);
					if(dssFileName != null)
					{
						profile.setDssFileName(Project.getCurrentProject().getAbsolutePath(dssFileName));
					}
					if(dssPathName != null)
					{
						profile.setDssPath(new DSSPathname(dssPathName));
					}
					_icMap.put(reservoirName, profile);
				}
				catch (ParseException e)
				{
					LOGGER.atWarning().withCause(e).log("Failed to parse profile date: " + profileName);
				}
			}
		}
	}
}
