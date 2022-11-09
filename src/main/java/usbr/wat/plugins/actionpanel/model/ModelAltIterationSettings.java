/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.jdom.Element;

import com.rma.util.XMLUtilities;

import hec.io.DSSIdentifier;
import hec.lang.NamedType;

import hec2.model.DataLocation;
import hec2.plugin.model.ModelAlternative;
import hec2.plugin.util.DataLocationUtilities;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ModelAltIterationSettings extends NamedType
{
	private Map<DataLocation, DSSIdentifier>_dataLocationSettings = new HashMap<>();
	
	public ModelAltIterationSettings()
	{
		super();
	}
	/**
	 * @param dataLocs
	 */
	public ModelAltIterationSettings(List<DataLocation> dataLocs)
	{
		this();
		fillSettingsTable(dataLocs);
	}
	/**
	 * @param dataLocs
	 */
	private void fillSettingsTable(List<DataLocation> dataLocs)
	{
		DataLocation dl;
		for (int i = 0;i < dataLocs.size(); i++ )
		{
			dl = dataLocs.get(i);
			if ( dl != null )
			{
				_dataLocationSettings.put(dl, new DSSIdentifier("",""));
			}
		}
	}
	/**
	 * 
	 * @param dataLoc
	 * @return can return null;
	 */
	public DSSIdentifier getDSSIdentifierFor(DataLocation dataLoc)
	{
		if ( dataLoc == null )
		{
			return null;
		}
		DSSIdentifier dssId = _dataLocationSettings.get(dataLoc);
		return dssId;
	}
	
	public List<DataLocation>getDataLocations()
	{
		Set<DataLocation> keys = _dataLocationSettings.keySet();
		List<DataLocation> l = new ArrayList<>(keys);
		return l;
	}
	
	public void setDssIdentifierFor(DataLocation dataLoc, DSSIdentifier dssId)
	{
		if ( dataLoc == null )
		{
			return;
		}
		if ( dssId == null )
		{
			_dataLocationSettings.remove(dataLoc);
		}
		else
		{
			_dataLocationSettings.put(dataLoc, dssId);
		}
	}
	/**
	 * @param entryElem
	 */
	public void saveData(Element parentElem)
	{
		Set<Entry<DataLocation, DSSIdentifier>> entrySet = _dataLocationSettings.entrySet();
		Iterator<Entry<DataLocation, DSSIdentifier>> iter = entrySet.iterator();
		Entry<DataLocation, DSSIdentifier> entry;
		DataLocation dl;
		DSSIdentifier dssId;
		Element entriesElem = new Element("DataLocations");
		parentElem.addContent(entriesElem);
		String fileName;
		String dssPath;
		while (iter.hasNext())
		{
			entry = iter.next();
			dl = entry.getKey();
			dssId = entry.getValue();
			Element entryElem = new Element("DataLocationSetting");
			entriesElem.addContent(entryElem);
			dl.toXML(entryElem);
			fileName = dssId.getFileName();
			dssPath = dssId.getDSSPath();
			XMLUtilities.addChildContent(entryElem, "DssFile", fileName!=null?fileName:"");
			XMLUtilities.addChildContent(entryElem, "DssPath", dssPath!=null?dssPath:"");
		}
	}
	/**
	 * @param iterKid
	 */
	public void loadData(Element iterKid)
	{
		_dataLocationSettings.clear();
		Element entriesElem = iterKid.getChild("DataLocations");
		if ( entriesElem != null )
		{
			
			List entryElems = entriesElem.getChildren("DataLocationSetting");
			String filename, dssPath;
			DataLocation dataLocation;
			Element entryElem;
			for(int i = 0; i < entryElems.size();i++ )
			{
				entryElem = (Element) entryElems.get(i);
				filename = XMLUtilities.getChildElementAsString(entryElem, "DssFile", "");
				dssPath = XMLUtilities.getChildElementAsString(entryElem, "DssPath", "");
				DSSIdentifier dssId = new DSSIdentifier(filename, dssPath);
				Element dlElem = entryElem.getChild("DataLocation");
				dataLocation = DataLocationUtilities.createDataLocation(dlElem);
				if ( dataLocation != null )
				{
					if ( dataLocation.fromXML(dlElem))
					{
						_dataLocationSettings.put(dataLocation, dssId);
					}
				}
			}
			
		}
	}
	
	public void setModelAlternative(ModelAlternative modelAlt)
	{
		Set<DataLocation> keys = _dataLocationSettings.keySet();
		Iterator<DataLocation> iter = keys.iterator();
		DataLocation dl;
		while (iter.hasNext())
		{
			dl = iter.next();
			dl.setModelAlternative(modelAlt);
		}
	}
	/**
	 * @param savedDataLocs
	 */
	public void updateDataLocations(List<DataLocation> dataLocs)
	{
		HashMap<DataLocation, DSSIdentifier>currentSettings = new HashMap<>(_dataLocationSettings);
		_dataLocationSettings.clear();
		DataLocation dl;
		DSSIdentifier dssId;
		for (int i = 0;i < dataLocs.size();i++ )
		{
			dl = dataLocs.get(i);
			dssId = currentSettings.get(dl);
			if (dssId == null )
			{
				Logger.getLogger(getClass().getName()).fine("No DSS File/Info found for "+dl);
				dssId = new DSSIdentifier("","");
			}
			_dataLocationSettings.put(dl, dssId);
		}
		
	}
	
}
