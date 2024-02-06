/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.commands;

import java.awt.Color;
import java.util.List;
import com.google.common.flogger.FluentLogger;
import com.rma.commands.AbstractNewManagerCommand;
import com.rma.io.FileManagerImpl;
import com.rma.message.Message;
import com.rma.model.Project;

import hec2.model.DataLocation;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.WAT;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatProject;
import hec2.wat.model.WatSimulation;
import hec2.wat.model.WatSimulationContainer;
import hec2.wat.model.WatModelLinkingManager;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;

/**
 * @author mark
 *
 */
public class SaveSimulationToGroupCmd extends AbstractNewManagerCommand
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	private final boolean _runExtract;
	private WatSimulation _srcSim;
	private AbstractSimulationGroup _simGroup;
	private WatAnalysisPeriod _ap;
	private WatSimulation _newSim;
	private String _newDesc;
	private String _newName;

	/**
	 * @param newDesc
	 * @param srcSim
	 * @param simGroup
	 * @param project
	 * @param ap
	 */
	public SaveSimulationToGroupCmd(WatSimulation srcSim, String newName, String newDesc, AbstractSimulationGroup simGroup,
			Project project, WatAnalysisPeriod ap, boolean runExtract)
	{
		super(project,"", "", null); // not using what the super does
		_srcSim = srcSim;
		_newDesc = newDesc;
		_newName = newName;
		_simGroup = simGroup;
		_project = project;
		_ap = ap;
		_runExtract = runExtract;
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
		if ( !copyModelLinking(_srcSim, _newSim))
		{
			Message msg = new Message("Failed to copy Model Linking for simulation "+_newSim.getName()+". Check log file for details", Color.RED);
			WAT.getWatFrame().addMessage(msg);
		}
		if ( _runExtract)
		{
			new UpdateDataAction().updateData(_simGroup);
		}
		
		return false;
		
	}

	private boolean copyModelLinking(WatSimulation srcSim, WatSimulation newSim)
	{
		WatModelLinkingManager mlm = getModelLinkingManager();
		List<ModelAlternative> modelAlts = srcSim.getAllModelAlternativeList();
		String srcSimName = srcSim.getName();
		String newSimName = newSim.getName();
		ModelAlternative modelAlt;
		List<DataLocation>dataLocs;
		boolean rv = true;
		for (int i = 0;i <modelAlts.size(); i++  )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			modelAlt.setVariantName(srcSim.getVariantName());
			dataLocs = mlm.getDataLocationsFor(srcSimName, modelAlt);
			if (dataLocs != null )
			{
				if ( !mlm.setDataLocationsFor(newSimName, modelAlt, dataLocs))
				{
					LOGGER.atWarning().log("Failed to copy DataLocations for Simulation:"+newSimName+" ModelAlternative:"+modelAlt);
					rv = false;
				}
			}
		}
		return rv;
	}
	private WatModelLinkingManager getModelLinkingManager()
	{
		WatProject prj = (WatProject) Project.getCurrentProject();
		return prj.getModelLinkingManager();
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
