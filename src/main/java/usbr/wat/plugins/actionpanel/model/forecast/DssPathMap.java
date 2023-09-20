/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.flogger.FluentLogger;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import hec.heclib.dss.DSSPathname;
import hec.hecmath.DSS;
import hec.io.DSSIdentifier;
import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.wat.model.WatSimulation;
import rma.util.RMAIO;

public class DssPathMap
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private final WatSimulation _sim;
	private final String _configFile;
	List<DssPathMapItem> _dssPathMapList = new ArrayList<>();
	private String _sourceDssFile;
	private String _sourceDssFPart;

	public DssPathMap(WatSimulation sim, String configFile)
	{
		super();
		_configFile = configFile;
		_sim = sim;
	}

	public boolean readDssPathsFile()
	{
		RmaFile file = FileManagerImpl.getFileManager().getFile(_configFile);
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
				if ( line.trim().isEmpty()|| line.startsWith("#")) // empty or comment
				{
					continue;
				}
				parts = line.split(",");
				if (parts == null || parts.length < 7)
				{
					LOGGER.atWarning().log("Invalid line found: "+line);
					continue;
				}
				dssPathMapItem = new DssPathMapItem(_sourceDssFile, _sourceDssFPart);
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

	/** dest to source */
	public Map<DSSIdentifier, DSSIdentifier> getDssCopyMap()
	{
		DssPathMapItem dssItem;
		Map<DSSIdentifier, DSSIdentifier>dssCopyMap = new HashMap<>();
		Map<DSSIdentifier, DSSIdentifier>dssIdMap;
		int numDests;
		String dssFile, dssPath;
		DSSIdentifier srcDssId, destDssId;
		for (int i = 0;i < _dssPathMapList.size(); i++ )
		{
			dssItem = _dssPathMapList.get(i);
			srcDssId = new DSSIdentifier( dssItem.getSrcDssFile(), dssItem.getSrcDssPath());
			numDests = dssItem.getNumberOfDests();
			for (int d = 0; d < numDests; d++ )
			{
				dssFile = dssItem.getDestDssFile(d);
				dssPath = dssItem.getDestDssPath(d);
				destDssId = new DSSIdentifier(dssFile, dssPath);
				dssCopyMap.put(destDssId, srcDssId);
			}
		}
		return dssCopyMap;
	}

	/** dest to source */
	public Map<DSSIdentifier, DSSIdentifier>getAllDssMap()
	{
		DssPathMapItem dssItem;
		Map<DSSIdentifier, DSSIdentifier>dssCopyMap = new HashMap<>();
		String srcDssFile, srcDssPath;
		String destDssFile, destDssPath;
		DSSIdentifier srcDssId, destDssId;
		for (int i = 0;i < _dssPathMapList.size(); i++ )
		{
			dssItem = _dssPathMapList.get(i);
			srcDssFile = dssItem.getSrcDssFile();
			srcDssPath = dssItem.getSrcDssPath();
			srcDssId = new DSSIdentifier(srcDssFile, srcDssPath);
			for (int d = 0; d < dssItem.getNumberOfDests();d++ )
			{
				destDssFile = dssItem.getDestDssFile(d);
				destDssPath = dssItem.getDestDssPath(d);
				destDssId = new DSSIdentifier(destDssFile, destDssPath);
				dssCopyMap.put(destDssId, srcDssId);
			}
		}
		return dssCopyMap;
	}


	public void setSourceDssFile(String sourceDssFile)
	{
		_sourceDssFile = sourceDssFile;
	}

	public void setSourceFPart(String sourceDssFPart)
	{
		_sourceDssFPart = sourceDssFPart;
	}

	public List<DSSIdentifier> getDestDssIdentifiersFor(String srcDssPath)
	{
		return getDestDssIdentifiersFor(srcDssPath, null);
	}

	public List<DSSIdentifier> getDestDssIdentifiersFor(String srcDssPath, String overrideTimeStep)
	{
		List<DSSIdentifier>destDssIds = new ArrayList<>();
		if ( srcDssPath == null )
		{
			return destDssIds;
		}
		DssPathMapItem dssItem;
		DSSIdentifier destDssId;
		String srcDssItemPath;
		for (int i = 0;i < _dssPathMapList.size(); i++ )
		{
			dssItem = _dssPathMapList.get(i);
			srcDssItemPath = dssItem.getSrcDssPath();
			DSSPathname srcDssPathname = new DSSPathname(srcDssItemPath);
			if(overrideTimeStep != null)
			{
				srcDssPathname.setEPart(overrideTimeStep);
			}
			srcDssItemPath = srcDssPathname.getPathname();
			if ( DssPathMapItem.dssPathsEqual(srcDssItemPath, srcDssPath))
			{
				for(int j = 0;j < dssItem.getNumberOfDests(); j++ )
				{
					destDssId = new DSSIdentifier(dssItem.getDestDssFile(j), dssItem.getDestDssPath(j));
					if ( !destDssIds.contains(destDssId))
					{
						destDssIds.add(destDssId);
					}
				}
			}
		}
		return destDssIds;
	}
}
