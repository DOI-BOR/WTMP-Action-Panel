/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.client.Browser;

import hec2.wat.client.WatComputeSelectorDialog;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.ActionsWindow;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class RunSimulationAction extends AbstractAction
{
	private ActionsWindow _parent;
	public RunSimulationAction(ActionsWindow parent)
	{
		super("Run Simulation");
		setEnabled(false);
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if ( _parent.getSimulationGroup() == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
			
		}
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		if ( sims.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to compute",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		WatComputeSelectorDialog computeDlg = new WatComputeSelectorDialog(Browser.getBrowserFrame(),WatSimulation.class);
		computeDlg.setSelectSimulations(sims);
		computeDlg.setSelectOutOfDate(false);
		computeDlg.setComputeOnOpen(true);
		computeDlg.setVisible(true);
		_parent.updateComputeStates();
	}

}
