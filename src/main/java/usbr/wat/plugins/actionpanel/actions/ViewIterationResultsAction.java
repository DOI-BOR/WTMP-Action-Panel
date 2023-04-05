/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;

import hec.dssgui.ListSelection;

import hec2.wat.model.WatSimulation;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;
import usbr.wat.plugins.actionpanel.model.ComputeType;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class ViewIterationResultsAction extends AbstractAction
{

	private ActionsWindow _parent;

	/**
	 * @param parent
	 */
	public ViewIterationResultsAction(ActionsWindow parent)
	{
		super("View DSS Results...");
		_parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();
		if ( simGroup == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
			
		}
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		
		ListSelection dssVue = new ListSelection("Compute Results",
                ListSelection.FULL_FUNCTION, true, false, false);
		WatSimulation sim;
		String simDssFile, computeDssFile;
		
		boolean openedFile = false;
		ComputeType computeType;
		BaseComputeSettings computeSettings;
		for (int i = 0;i < sims.size(); i++ )
		{
			sim = sims.get(i);
			computeType = simGroup.getComputeType(sim.getName());
			
			simDssFile = sim.getSimulationDssFile();
			if( computeType != ComputeType.Standard )
			{
				simDssFile = RMAIO.getDirectoryFromPath(simDssFile);
				computeSettings = simGroup.getComputeSettings(sim.getName(), computeType);
				computeDssFile = RMAIO.concatPath(simDssFile, computeSettings.getCollectionDssFilename());
			}
			else
			{
				computeDssFile = simDssFile;
			}
	System.out.println("actionPerformed:Sim compute dss file: "+computeDssFile);
			if ( FileManagerImpl.getFileManager().fileExists(computeDssFile))
			{
				dssVue.openDSSFile(computeDssFile);
				openedFile = true;
			}
		}
		if ( openedFile )
		{
			dssVue.setLocationRelativeTo(_parent);
			dssVue.setVisible(true);
		}
		
	}

}
