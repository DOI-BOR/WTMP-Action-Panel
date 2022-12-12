/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.commands;

import com.rma.commands.AbstractNewManagerCommand;
import com.rma.io.FileManagerImpl;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;
import hec2.wat.model.WatSimulationContainer;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author mark
 *
 */
public class SaveSimulationToGroupCmd extends AbstractNewManagerCommand
{

	private WatSimulation _srcSim;
	private SimulationGroup _simGroup;
	private WatAnalysisPeriod _ap;
	private WatSimulation _newSim;
	private String _newDesc;
	private String _newName;

	/**
	 * @param newDesc2 
	 * @param sim
	 * @param simgroup
	 * @param project
	 * @param ap
	 */
	public SaveSimulationToGroupCmd(WatSimulation srcSim, String newName, String newDesc, SimulationGroup simGroup,
			Project project, WatAnalysisPeriod ap)
	{
		super(project,"", "", null); // not using what the super does
		_srcSim = srcSim;
		_newDesc = newDesc;
		_newName = newName;
		_simGroup = simGroup;
		_project = project;
		_ap = ap;
	}

	@Override
	public boolean doCommand()
	{
		WatSimulationContainer container ;
		String newSimName = _newName;
		if ( newSimName == null )
		{
			newSimName = getGroupSimName(_srcSim.getName(), _simGroup.getName());
		}
		container = new WatSimulationContainer();
		container.setProgramOrder(_srcSim.getProgramOrder());
		container.setName(newSimName);
		container.setProject(_project);
		String fileName = _project.getProjectDirectory();
		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".container"));
		container.setFile(FileManagerImpl.getFileManager().getFile(fileName));
		container.setAnalysisPeriod(_ap);
		container.setAlternative(_srcSim.getContainerParent().getAlternative());
		_project.addManager(container);
		
		_newSim = new WatSimulation();
		_newSim.setProject(_project);
		// read in the original simulation's data
		_newSim.setFile(_srcSim.getFile());
		_newSim.readData();
		_newSim.setName(newSimName);
		if ( _newDesc != null )
		{
			_newSim.setDescription(_newDesc);
		}
		fileName = _project.getProjectDirectory();
		fileName = RMAIO.concatPath(fileName, "wat");
		fileName = RMAIO.concatPath(fileName, "sims");
		fileName = RMAIO.concatPath(fileName, RMAIO.userNameToFileName(newSimName).concat(".simulation"));
		
		_newSim.setFile(FileManagerImpl.getFileManager().getFile(fileName));
		
		_newSim.setSimulationContainer(container);
		container.addSimulation(_newSim);
		_project.addManager(_newSim);
		
		new UpdateDataAction(ActionPanelPlugin.getInstance().getActionsWindow()).updateData(_simGroup);
		
		return false;
		
	}
	
	public static String getGroupSimName(String simName, String simGroupName)
	{
		return simName+"-"+simGroupName;
	}
	@Override
	public String getExtension()
	{
		return "simulation";
	}

	@Override
	public String getManagerClass()
	{
		return WatSimulation.class.getName();
	}

	@Override
	public String getManagerType()
	{
		return "Simulation";
	}

	/**
	 * @return
	 */
	public WatSimulation getSimulation()
	{
		return _newSim;
	}

}
