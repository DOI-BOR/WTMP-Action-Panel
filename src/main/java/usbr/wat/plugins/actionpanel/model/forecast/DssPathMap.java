/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.flogger.FluentLogger;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import hec.io.DSSIdentifier;
import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.wat.model.WatSimulation;
import rma.util.RMAIO;

public class DssPathMap
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String CONFIG_FILE = System.getProperty("WTMP.bcPathsMapFile", "shared/config/bcPathsMap.config");
	private final WatSimulation _sim;
	List<DssPathMapItem> _dssPathMapList = new ArrayList<>();

	public DssPathMap(WatSimulation sim)
	{
		super();
		_sim = sim;
	}

	public boolean readDssPathsFile()
	{
		String prjDir = Project.getCurrentProject().getProjectDirectory();
		String configPath = RMAIO.concatPath(prjDir, CONFIG_FILE);
		RmaFile file = FileManagerImpl.getFileManager().getFile(configPath);
		if ( !file.exists())
		{
			_sim.addErrorMessage("DSS Paths Map file "+file.getAbsolutePath()+" doesn't exist.");
			return false;
		}
		BufferedReader reader = file.getBufferedReader();
		if ( reader == null )
		{
			_sim.addErrorMessage("Failed to get reader for DSS Paths map file "+file.getAbsolutePath());
			return false;
		}
		String line;
		String[] parts;
		_dssPathMapList = new ArrayList<>();
		DssPathMapItem dssPathMapItem;
		//Location, parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record, ...
		try
		{
			reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				parts = line.split(",");
				if (parts == null || parts.length < 7)
				{
					continue;
				}
				dssPathMapItem = new DssPathMapItem();
				if (dssPathMapItem.parseLine(parts))
				{
					_dssPathMapList.add(dssPathMapItem);
				}
			}
			return true;
		}
		catch ( IOException ioe)
		{
			LOGGER.atWarning().withCause(ioe).log("Error reading "+file.getAbsolutePath());
			_sim.addErrorMessage("Error reading DSS Paths map file "+file.getAbsolutePath()+" error:"+ioe);
			return false;
		}
		finally
		{
			try
			{
				reader.close();
			} catch (IOException e)
			{ }
		}

	}

	public DSSIdentifier getDSSIdentifierFor(DataLocation dataLoc)
	{
		if ( dataLoc == null )
		{
			return null;
		}
		if ( !(dataLoc.getLinkedToLocation() instanceof DssDataLocation) )
		{
			return null;
		}
		DssDataLocation linkedToLoc = (DssDataLocation) dataLoc.getLinkedToLocation();
		String dssPath = linkedToLoc.getDssPath();
		String dssFile = linkedToLoc.get_dssFile();
		DSSIdentifier srcDssId = getSourceDssIdentifierFor(dssFile, dssPath);
		return srcDssId;
	}

	private DSSIdentifier getSourceDssIdentifierFor(String dssFile, String dssPath)
	{
		DssPathMapItem dssMapItem;
		for(int i = 0;i < _dssPathMapList.size();i++ )
		{
			dssMapItem = _dssPathMapList.get(i);
			DSSIdentifier dssId = dssMapItem.hasDestLocation(dssFile, dssPath);
			if ( dssId != null )
			{
				return dssId;
			}
		}
		return null;
	}

	public Map<DSSIdentifier, DSSIdentifier> getDssCopyMap()
	{
		DssPathMapItem dssItem;
		Map<DSSIdentifier, DSSIdentifier>dssCopyMap = new HashMap<>();
		Map<DSSIdentifier, DSSIdentifier>dssIdMap;
		for (int i = 0;i < _dssPathMapList.size(); i++ )
		{
			dssItem = _dssPathMapList.get(i);
			dssIdMap = dssItem.getDssIdMap();
			if ( dssIdMap != null && !dssIdMap.isEmpty())
			{
				dssCopyMap.putAll(dssIdMap);
			}
		}
		return dssCopyMap;
	}
}
