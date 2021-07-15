/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.commands;

import java.util.List;

import com.rma.commands.AbstractNewManagerCommand;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;
import hec2.wat.model.WatSimulationContainer;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
public class NewSimulationGroupCmd extends AbstractNewManagerCommand
{
	private List<WatSimulation> _sims;
	private WatAnalysisPeriod _ap;

	/**
	 * @param project
	 * @param name
	 * @param descr
	 * @param dir
	 */
	public NewSimulationGroupCmd(Project project, String name, String descr,
			RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims)
	{
		super(project, name, descr, dir);
		_ap = ap;
		_sims = sims;
		
	}

	@Override
	public String getExtension()
	{
		return SimulationGroup.FILE_EXT;
	}

	@Override
	public String getManagerClass()
	{
		return SimulationGroup.class.getName();
	}

	@Override
	public String getManagerType()
	{
		return "Simulation Group";
	}

	/**
	 * @return
	 */
	public SimulationGroup getSimulationGroup()
	{
		return (SimulationGroup) getManager();
	}

	@Override
	public boolean doCommand()
	{
		boolean rv = super.doCommand();
		SimulationGroup simgroup = getSimulationGroup();
		if ( simgroup != null )
		{
			simgroup.setAnalysisPeriod(_ap);
		}
		WatSimulation sim;
		WatSimulation newSim;
		for (int i = 0;i < _sims.size(); i++ )
		{
			sim = _sims.get(i);
			newSim = createSimulation(sim, simgroup, _project, _ap);
			
			
			
			simgroup.addSimulation(newSim);
		}	
		_project.saveProject();
		return rv;
	}

	/**
	 * @param sim
	 * @param simgroup 
	 */
	public static WatSimulation createSimulation(WatSimulation sim, SimulationGroup simgroup, Project project, WatAnalysisPeriod ap)
	{
		WatSimulationContainer container ;
		String newSimName = getGroupSimName(sim.getName(), simgroup.getName());
		container = new WatSimulationContainer();
		container.setProgramOrder(sim.getProgramOrder());
		container.setName(newSimName);
		container.setProject(project);
		String fileName = project.getProjectDirectory();
		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".container"));
		container.setFile(FileManagerImpl.getFileManager().getFile(fileName));
		container.setAnalysisPeriod(ap);
		container.setAlternative(sim.getContainerParent().getAlternative());
		project.addManager(container);
		
		WatSimulation newSim = new WatSimulation();
		sim.setProject(project);
		// read in the original simulation's data
		newSim.setFile(sim.getFile());
		newSim.readData();
		newSim.setName(newSimName);
		fileName = project.getProjectDirectory();
		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".simulation"));
		
		newSim.setFile(FileManagerImpl.getFileManager().getFile(fileName));
		
		newSim.setSimulationContainer(container);
		container.addSimulation(newSim);
		project.addManager(newSim);
		
		return newSim;
	}

	/**
	 * @param name
	 * @param string 
	 * @return
	 */
	public static String getGroupSimName(String simName, String simGroupName)
	{
		return simName+"-"+simGroupName;
	}

}
