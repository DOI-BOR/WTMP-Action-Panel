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
import java.util.List;
import java.util.Map;
import hec.heclib.dss.DSSPathname;
import hec.io.DSSIdentifier;
import hec.lang.NamedType;
import rma.util.RMAIO;

public class DssPathMapItem extends NamedType
{
	private String _srcDssFpart;
	private String _srcDssFile;
	private String _srcDssPath;

	private List<DssItem> _destDssItems = new ArrayList<>();
	/** dest and source DSSIdentifiers */
	private Map<DSSIdentifier, DSSIdentifier> _dssIdMap = new HashMap<>();

	public DssPathMapItem(String sourceDssFile, String sourceDssFPart)
	{
		super();
		_srcDssFile = sourceDssFile;
		_srcDssFpart = sourceDssFPart;
	}

	public boolean parseLine(String[] parts)
	{
		if ( parts == null || parts.length < 7 )
		{
			return false;
		}
		//Location, parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record, ...
		// 0         1               2                3                   4                     5                       6

		setName(parts[0].trim()+"-"+parts[1].trim());
		if ( _srcDssFile == null )
		{
			_srcDssFile = parts[2].trim();
		}
		_srcDssPath = parts[3].trim();
		if (_srcDssFpart != null )
		{
			DSSPathname pathname = new DSSPathname();
			pathname.setPathname(_srcDssPath);
			pathname.setFPart(_srcDssFpart);
			_srcDssPath = pathname.getPathname();
		}

		int numDests = RMAIO.parseInt(parts[4].trim());
		for(int i =5; i< 5+numDests; i+=2)
		{
			DssItem dssItem = new DssItem(parts[i].trim(),parts[i+1].trim());
			_destDssItems.add(dssItem);
		}
		return true;
	}

	public void setSourceDssFile(String dssFile)
	{
		_srcDssFile = dssFile;
	}

	public String getSrcDssPath()
	{
		return _srcDssPath;
	}

	public String getSrcDssFile()
	{
		return _srcDssFile;
	}

	public int getNumberOfDests()
	{
		return _destDssItems.size();
	}

	public String getDestDssFile(int num)
	{
		return _destDssItems.get(num).getDssFile();
	}

	public String getDestDssPath(int num)
	{
		return _destDssItems.get(num).getDssPath();
	}

	public Map<DSSIdentifier, DSSIdentifier>getDssIdMap()
	{
		return _dssIdMap;
	}
	public DSSIdentifier hasDestLocation(String dssFile, String dssPath)
	{
		if ( dssFile == null || dssPath == null )
		{
			return null;
		}
		DssItem destItem;
		DSSPathname pathname1 = new DSSPathname();
		DSSPathname pathname2 = new DSSPathname();

		pathname1.setPathname(dssPath);

		for(int i = 0;i < _destDssItems.size();i++ )
		{
			destItem = _destDssItems.get(i);

			if (RMAIO.pathsEqual(destItem.getDssFile(), dssFile))
			{
				pathname2.setPathname(destItem.getDssPath());
				if ( dssPathsEqual(pathname1, pathname2))
				{
					DSSIdentifier srcDssId = new DSSIdentifier(_srcDssFile, _srcDssPath);
					DSSIdentifier destDssId = new DSSIdentifier(destItem.getDssFile(), destItem.getDssPath());
					_dssIdMap.put(destDssId, srcDssId);
					return srcDssId;
				}
			}
		}
		return null;
	}

	public static boolean dssPathsEqual(String path1, String path2)
	{
		DSSPathname src1 = new DSSPathname(path1);
		src1.setCollectionSequence(null);
		DSSPathname src2 = new DSSPathname(path2);
		src2.setCollectionSequence(null);
		return dssPathsEqual(src1, src2);
	}

	public static boolean dssPathsEqual(DSSPathname path1, DSSPathname path2)
	{
		return ( path1.getAPart().equalsIgnoreCase(path2.getAPart())
			&& path1.getBPart().equalsIgnoreCase(path2.getBPart())
			&& path1.getCPart().equalsIgnoreCase(path2.getCPart())
			&& path1.getEPart().equalsIgnoreCase(path2.getEPart())
			&& path1.getFPart().equalsIgnoreCase(path2.getFPart()) );
	}

	void setSourceDssPath(String pathname)
	{
		_srcDssPath = pathname;
	}

	void addMapping(String destFile, String destPath)
	{
		_destDssItems.add(new DssItem(destFile, destPath));
	}

	class DssItem
	{
		private String _dssFile;
		private String _dssPath;

		DssItem(String dssFile, String dssPath)
		{
			super();
			_dssFile = dssFile;
			_dssPath = dssPath;
		}

		public String getDssFile()
		{
			return _dssFile;
		}

		public String getDssPath()
		{
			return _dssPath;
		}

	}

}
