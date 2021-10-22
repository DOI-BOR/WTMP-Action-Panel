/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.python.google.common.io.Files;

import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.model.Project;

import rma.util.RMAIO;

/**
 * @author Mark Ackerman
 *
 */
public class ModelFileStorage
{
	private static final String MODEL_SAVE_FOLDER ="savedModelFiles";
	
	private static String _basePath;
	private static Map<File, File>_savedFiles = new HashMap<>();
	
	static
	{
		Project.addStaticProjectListener(new ProjectAdapter()
		{
			@Override
			public void projectLoaded(ProjectEvent e)
			{
				initialize(e.getProject().getProjectDirectory());
			}
			
		});
	}
	
	
	private ModelFileStorage()
	{
		
	}
	
	/**
	 * @param projectDirectory
	 */
	protected static void initialize(String projectDirectory)
	{
		_basePath = RMAIO.concatPath(projectDirectory, MODEL_SAVE_FOLDER);
		_savedFiles.clear();
	}	
	

	/**
	 * 
	 * @param file
	 * @return
	 */
	public static boolean saveFile(String file)
	{
		if ( file == null )
		{
			return false;
		}
		String absFile = file;
		if ( !RMAIO.isFullPath(file))
		{
			absFile = Project.getCurrentProject().getAbsolutePath(file);
		}
		File srcFile = new File(absFile);
		return saveFile(srcFile);
		
	}
	public static boolean saveFiles(List<String>files)
	{
		if ( files == null )
		{
			return false;
		}
		boolean rv = true;
		for (int i = 0;i < files.size(); i++ )
		{
			rv &= saveFile(files.get(i));
		}
		return rv;
	}
	
	/*
	public static boolean saveFiles(List<File>files)
	{
		if ( files == null )
		{
			return false;
		}
		boolean rv = true;
		for (int i = 0;i < files.size(); i++ )
		{
			rv &= saveFile(files.get(i));
		}
		return rv;	
	}
	*/
	/**
	 *  save the specified file to the saved file location
	 * @param srcFile
	 * @return
	 */
	public static boolean saveFile(File srcFile)
	{
		File destPath = buildDestPath(srcFile);
		
		try
		{
			Files.copy(srcFile, destPath);
			_savedFiles.put(srcFile, destPath);
			return true;
		}
		catch ( IOException ioe )
		{
			Logger.getLogger("ModelFileStorage").info("Failed to copy file "+srcFile.getAbsolutePath()+" to "+destPath.getAbsolutePath()+" error:"+ioe);
			return false;
		}	
	}	
	/**
	 * @param f
	 * @return
	 */
	private static File buildDestPath(File srcFile)
	{
		String prjDir = Project.getCurrentProject().getProjectDirectory();
		String relDir = RMAIO.getRelativePath(prjDir, srcFile.getAbsolutePath());
		String destPath = RMAIO.concatPath(_basePath, relDir);
		
		String destFolder = RMAIO.getDirectoryFromPath(destPath);
		File f = new File(destFolder);
		if ( f.mkdirs())
		{
			return new File(destPath);
		}
		return null;
	}

	

	/**
	 * restore all files that have been saved
	 * @return true if all files were restored
	 */
	public static boolean restoreAllFiles()
	{
		if ( _savedFiles.isEmpty())
		{
			rebuildSavedFilesMap();
		}
		Set<Entry<File, File>> entrySet = _savedFiles.entrySet();
		Iterator<Entry<File, File>> iter = entrySet.iterator();
		File origFile, savedFile;
		Entry<File, File> entry;
		boolean rv = true, brv;;
		while (iter.hasNext())
		{
			entry = iter.next();
			
			origFile = entry.getKey();
			savedFile = entry.getValue();
			
			if ( restoreFile(origFile, savedFile))
			{
				_savedFiles.remove(origFile);
				rv &= true;
			}
			else
			{
				rv &= false;
			}
			
		}
		if ( _savedFiles.size() > 0 )
		{
			Logger.getLogger("ModelFileStorage").info("Failed to restore "+_savedFiles.size()+" files");
		}
		
		return rv;	
	}
	


	/**
	 * @param origFile
	 * @param savedFile
	 * @return
	 */
	private static boolean restoreFile(File origFile, File savedFile)
	{
		try
		{
			Files.copy(savedFile, origFile);
		}
		catch (IOException ioe)
		{
			Logger.getLogger("ModelFileStorage").info("Failed to restore file "+savedFile.getAbsolutePath()+" to "+origFile.getAbsolutePath()+" error:"+ioe);
			return false;
		}
		return true;
	}
	/**
	 * restore a single file back to its original location
	 * @param fileToRestore
	 * @return true if the file was restored 
	 */
	public static boolean restoreFile(String fileToRestore)
	{
		if ( fileToRestore == null )
		{
			return false;
		}
		String absFileToRestore = fileToRestore;
		if ( !RMAIO.isFullPath(fileToRestore))
		{
			absFileToRestore = Project.getCurrentProject().getAbsolutePath(fileToRestore);
		}
		File requestedFile = new File(absFileToRestore);
		File savedFile = _savedFiles.get(requestedFile);
		if ( savedFile != null )
		{
			return restoreFile(requestedFile, savedFile);
		}
		return false;
	}
	
	/**
	 * 
	 */
	private static void rebuildSavedFilesMap()
	{
		// TODO Auto-generated method stub
		System.out.println("rebuildSavedFilesMap TODO implement me");
		
	}
	
}
