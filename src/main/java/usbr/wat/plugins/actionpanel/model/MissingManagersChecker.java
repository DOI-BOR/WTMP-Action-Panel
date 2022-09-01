/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model;

import java.util.List;
import java.util.logging.Logger;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Manager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;

import hec2.wat.model.WatAlternative;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;
import hec2.wat.model.WatSimulationContainer;

import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;

/**
 * check for managers who's files exist out in the study folders but aren't in the study's sty file so the study doesn't know about them.
 * manager's can get lost after a download from git.
 * 
 *
 */
public class MissingManagersChecker
{
	private static final String SIMS_FOLDER = "sims";
	private static final String ALTS_FOLDER = "alts";
	private static final String APS_FOLDER = "aps";
	private static final String WAT_DIR = "wat";
	private static final String SIM_GROUPS_FOLDER = "simGroups";
	
	private Project _project;
	private Logger _logger = Logger.getLogger(MissingManagersChecker.class.getName());
	

	public MissingManagersChecker()
	{
		super();
	}
	
	/**
	 * @param project
	 */
	public void checkForMissingManagers(Project project)
	{
		if ( project == null ) 
		{
			project = Project.getCurrentProject();
		}
		_project = project;
		if ( _project.isNoProject())
		{
			return;
		}
		checkForAndAddAnalysisPeriods();
		checkForAndAddAlternatives();
		checkForAndAddSimulations();
		checkForAndAddSimulationGroups();
	}
	
	
	
	/**
	 * 
	 */
	private void checkForAndAddSimulationGroups()
	{
		String folderToCheck = getWatFolder(SIM_GROUPS_FOLDER);
		RMAFilenameFilter simGroupFilter = new RMAFilenameFilter("simgrp");
		List<String> simGroupFiles = FileManagerImpl.getFileManager().list(folderToCheck,simGroupFilter);
		
		String path;
		ManagerProxy proxy;
		for (int i = 0;i < simGroupFiles.size(); i++ )
		{
			path = simGroupFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, SimulationGroup.class);
			if ( proxy == null )
			{
				addSimulationGroup(path);
			}
		}	
	}

	/**
	 * @param path
	 */
	private void addSimulationGroup(String path)
	{
		SimulationGroup simGroup = new SimulationGroup();
		addManager(simGroup, path);
	}

	/**
	 * 
	 */
	private void checkForAndAddSimulations()
	{
		String folderToCheck = getWatFolder(SIMS_FOLDER);
		RMAFilenameFilter containerFilter = new RMAFilenameFilter("container");
		List<String> containerFiles = FileManagerImpl.getFileManager().list(folderToCheck,containerFilter);
		RMAFilenameFilter simFilter = new RMAFilenameFilter("simulation");
		List<String> simFiles = FileManagerImpl.getFileManager().list(folderToCheck,simFilter);
		
		String path;
		ManagerProxy proxy;
		for (int i = 0;i < simFiles.size(); i++ )
		{
			path = simFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatSimulation.class);
			if ( proxy == null )
			{
				addSimulation(path);
			}
		}	
		for (int i = 0;i < containerFiles.size(); i++ )
		{
			path = containerFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatSimulationContainer.class);
			if ( proxy == null )
			{
				addSimulationContainer(path);
			}
		}
		
	}

	/**
	 * @param path
	 */
	private void addSimulationContainer(String path)
	{
		WatSimulationContainer container = new WatSimulationContainer();
		addManager(container, path);
	}
	/**
	 * 
	 * @param manager
	 * @param path
	 */
	private void addManager(Manager manager, String path)
	{
		RmaFile file = FileManagerImpl.getFileManager().getFile(path);
		manager.setFile(file);
		manager.setProject(_project);
		manager.setFile(file);
		if ( manager.readData())
		{
			_project.addManager(manager);
			_logger.info("Readded "+manager.getClass().getName()+" "+ manager.getName()+" from file "+path);
		}
		
	}

	/**
	 * @param path
	 */
	private void addSimulation(String path)
	{
		WatSimulation sim = new WatSimulation();
		addManager(sim, path);
	}

	/**
	 * @param simsFolder
	 * @return
	 */
	private String getWatFolder(String subfolder)
	{
		String projectDir = _project.getProjectDirectory();
		String dir = RMAIO.concatPath(projectDir,WAT_DIR);
		dir = RMAIO.concatPath(dir, subfolder);
		return dir;
	}

	/**
	 * 
	 */
	private void checkForAndAddAlternatives()
	{
		String folderToCheck = getWatFolder(ALTS_FOLDER);
		RMAFilenameFilter altsFilter = new RMAFilenameFilter("walt");
		List<String> altsFiles = FileManagerImpl.getFileManager().list(folderToCheck, altsFilter);
		
		String path;
		ManagerProxy proxy;
		for (int i = 0;i < altsFiles.size(); i++ )
		{
			path = altsFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatAlternative.class);
			if ( proxy == null )
			{
				addAlternative(path);
			}
		}
	}

	/**
	 * @param path
	 */
	private void addAlternative(String path)
	{
		WatAlternative alt = new WatAlternative();
		addManager(alt, path);
		
	}

	/**
	 * 
	 */
	private void checkForAndAddAnalysisPeriods()
	{
		String folderToCheck = getWatFolder(APS_FOLDER);
		RMAFilenameFilter apFilter = new RMAFilenameFilter("wap");
		List<String> apFiles = FileManagerImpl.getFileManager().list(folderToCheck, apFilter);
		
		String path;
		ManagerProxy proxy;
		for (int i = 0;i < apFiles.size(); i++ )
		{
			path = apFiles.get(i);
			proxy = _project.getManagerProxyByPath(path, WatAnalysisPeriod.class);
			if ( proxy == null )
			{
				addAnalysisPeriod(path);
			}
		}
	}

	/**
	 * @param path
	 */
	private void addAnalysisPeriod(String path)
	{
		WatAnalysisPeriod ap = new WatAnalysisPeriod();
		addManager(ap, path);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		System.out.println("main TODO implement me");

	}

}
