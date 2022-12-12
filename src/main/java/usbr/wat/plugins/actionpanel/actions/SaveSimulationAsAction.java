/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.rma.model.Project;

import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.commands.SaveSimulationToGroupCmd;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.SaveSimulationAsDialog;

/**
 * @author mark
 *
 */
public class SaveSimulationAsAction extends AbstractAction
{
	public SaveSimulationAsAction()
	{
		super("Save Simulation As...");
		
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{ }
	/**
	 * @param simulationGroup
	 * @param sim
	 */
	public boolean saveSimulationAs(SimulationGroup simGroup,
			WatSimulation srcSim)
	{
		if ( simGroup == null || srcSim == null ) 
		{
			return false;
		}
		SaveSimulationAsDialog dlg = new SaveSimulationAsDialog(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(simGroup, srcSim);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return false;
		}
		String newName = dlg.getSaveAsName();
		String newDesc = dlg.getDescription();
		
		SaveSimulationToGroupCmd cmd = new SaveSimulationToGroupCmd(srcSim, newName, newDesc, simGroup, Project.getCurrentProject(), simGroup.getAnalysisPeriod());
		cmd.doCommand();
		WatSimulation newSim = cmd.getSimulation();
		if ( newSim != null )
		{
			simGroup.addSimulation(newSim);
			ActionPanelPlugin.getInstance().getActionsWindow().fillSimulationTable();
			return true;
		}
		
		
		return false;
	}

	
}
