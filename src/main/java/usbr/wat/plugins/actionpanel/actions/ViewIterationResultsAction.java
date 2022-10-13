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
import usbr.wat.plugins.actionpanel.model.ActionComputable;
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
		super("View Results...");
		_parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		SimulationGroup simGroup = _parent.getSimulationGroup();
		if ( simGroup == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
			
		}
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		ListSelection dssVue = new ListSelection("Iteration Results",
                ListSelection.FULL_FUNCTION, false, false);
		WatSimulation sim;
		String simDssFile, iterDssFile;
		
		boolean openedFile = false;
		for (int i = 0;i < sims.size(); i++ )
		{
			sim = sims.get(i);
			simDssFile = sim.getSimulationDssFile();
			simDssFile = RMAIO.getDirectoryFromPath(simDssFile);
			iterDssFile = RMAIO.concatPath(simDssFile, ActionComputable.ITERATION_DSS_FILE);
	System.out.println("actionPerformed:Sim iter dss file: "+iterDssFile);
			if ( FileManagerImpl.getFileManager().fileExists(iterDssFile))
			{
				dssVue.openDSSFile(iterDssFile);
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
