/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.util.XMLUtilities;

import hec.lang.NamedType;

import hec2.wat.model.WatSimulation;

import rma.util.RMAIO;

/**
 * @author mark
 *
 */
public class ResultsData extends NamedType
{
	private static final String RESULTS_DATA_FILE = "resultsData";
	private String _savedBy;
	private Date _savedAtDate;
	private transient WatSimulation _simulation;
	private transient String _folder;
	private long _lastComputedDate;
	
	public ResultsData(WatSimulation simulation, String folder)
	{
		super();
		_simulation = simulation;
		_folder = folder;
	}
	
	public String getSavedBy()
	{
		return _savedBy;
	}
	
	public void setSavedBy(String savedBy)
	{
		_savedBy = savedBy;
	}

	/**
	 * @param date
	 */
	public void setSavedAt(Date date)
	{
		_savedAtDate = date;
	}
	
	public Date getSavedAt()
	{
		return _savedAtDate;
	}
	
	public WatSimulation getSimulation()
	{
		return _simulation;
	}
	
	public String getFolder()
	{
		return _folder;
	}

	/**
	 * @param resultsDir
	 */
	public boolean saveDataToFolder(String resultsDir)
	{
		if ( resultsDir == null )
		{
			return false;
		}
		String resultsFileName = RMAIO.concatPath(resultsDir, RESULTS_DATA_FILE);
		RmaFile resultsFile = FileManagerImpl.getFileManager().getFile(resultsFileName);
		Element root = new Element("Results");
		Document doc = new Document(root);
		
		XMLUtilities.saveNamedType(root, this);
		XMLUtilities.addChildContent(root, "SavedBy", _savedBy);
		XMLUtilities.addChildContent(root, "SavedOn", _savedAtDate.toString());
		XMLUtilities.addChildContent(root, "LastComputedDate", _lastComputedDate);
		return XMLUtilities.saveDocument(doc, resultsFile);
	}
	
	/**
	 * @param resultsDir
	 */
	public boolean loadDataFromFolder(String resultsDir)
	{
		if ( resultsDir == null )
		{
			return false;
		}
		String resultsFileName = RMAIO.concatPath(resultsDir, RESULTS_DATA_FILE);
		RmaFile resultsFile = FileManagerImpl.getFileManager().getFile(resultsFileName);
		return loadDataFromFolder(resultsFile);
	}
	/**
	 * 
	 * @param resultsFile
	 * @return
	 */
	public boolean loadDataFromFolder(RmaFile resultsFile)
	{
		Document doc = XMLUtilities.loadDocument(resultsFile);
		if ( doc == null )
		{
			return false;
		}
		Element root = doc.getRootElement();
		
		
		XMLUtilities.loadNamedType(root, this);
		_savedBy = XMLUtilities.getChildElementAsString(root, "SavedBy", _savedBy);
		String saveOnStr = XMLUtilities.getChildElementAsString(root, "SavedOn", null);
		if ( saveOnStr != null )
		{
			_savedAtDate = new Date(saveOnStr);
		}
		_lastComputedDate = XMLUtilities.getChildElementAsLong(root, "LastComputedDate", 0);
		return true;
	}

	/**
	 * @param lastComputedDate
	 */
	public void setLastComputedTime(long lastComputedDate)
	{
		_lastComputedDate = lastComputedDate;
	}
	
	public long getLastComputedTime()
	{
		return _lastComputedDate;
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean renameTo(String newName)
	{
		if ( newName == null || newName.isEmpty() )
		{
			return false;
		}
		String folderName = getFolder();
		
		Path src = Paths.get(folderName);
		try
		{
			Path newPath = Files.move(src, src.resolveSibling(RMAIO.userNameToFileName(newName)));
			
			boolean rv =  newPath != null;
			if ( rv )
			{
				setName(newName);
				_folder = newPath.toString();
				saveDataToFolder(newPath.toString());
			}
			return rv;
		}
		catch (IOException e)
		{
			Logger.getLogger(ResultsData.class.getName()).info("Failed to rename "+ getName()+" Error:"+e);
			return false;
		}
	}

}
	
