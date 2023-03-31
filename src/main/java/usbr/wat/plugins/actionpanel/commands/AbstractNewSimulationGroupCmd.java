/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.commands;

import java.util.List;

import com.rma.commands.AbstractNewManagerCommand;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;

/**
 * @author mark
 *
 */
public abstract class AbstractNewSimulationGroupCmd extends AbstractNewManagerCommand
{

	private List<WatSimulation> _sims;
	private WatAnalysisPeriod _ap;
	
	public AbstractNewSimulationGroupCmd(Project project, String name, String descr,
			RmaFile dir, WatAnalysisPeriod ap, List<WatSimulation>sims)
	{
		super(project, name, descr, dir);
		_ap = ap;
		_sims = sims;
		
	}
	
	/**
	 * @return
	 */
	public AbstractSimulationGroup getSimulationGroup()
	{
		return (AbstractSimulationGroup) getManager();
	}
	
	@Override
	public boolean doCommand()
	{
		boolean rv = super.doCommand();
		AbstractSimulationGroup simgroup = getSimulationGroup();
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
	public static WatSimulation createSimulation(WatSimulation sim, AbstractSimulationGroup simgroup, Project project, WatAnalysisPeriod ap)
	{
		SaveSimulationToGroupCmd cmd = new SaveSimulationToGroupCmd(sim, null,null, simgroup, project, ap);
		cmd.doCommand();
		WatSimulation newSim = cmd.getSimulation();
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
