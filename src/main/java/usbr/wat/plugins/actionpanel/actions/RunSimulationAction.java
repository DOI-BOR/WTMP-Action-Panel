/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.client.Browser;

import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;
import usbr.wat.plugins.actionpanel.model.ActionComputable;
import usbr.wat.plugins.actionpanel.model.IterationSettings;
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

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
		boolean recomputeAll = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		
		SimulationGroup simGroup = _parent.getSimulationGroup();
		if ( simGroup == null )
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
		List<ActionComputable>computables = new ArrayList<>();
		Iterator<WatSimulation> iter = sims.iterator();
		ActionComputable computable;
		WatSimulation sim ;
		IterationSettings iterSettings;
		PositionAnalysisSettings posAnalysisSettings;
		UsgsComputeSelectorDialog computeDlg = new UsgsComputeSelectorDialog(Browser.getBrowserFrame(),WatSimulation.class);
		while ( iter.hasNext())
		{
			sim = iter.next();
			iterSettings = simGroup.getIterationSettings(sim.getName());
			posAnalysisSettings = simGroup.getPositionAnalysisSettings(sim.getName());
			computable = new ActionComputable(sim, iterSettings, posAnalysisSettings, simGroup.getComputeType(sim.getName()));
			computable.setProgressDialog(computeDlg);
			computables.add(computable);
		}
		//computeDlg.setRecomputeAll(recomputeAll);
		computeDlg.setSelectedComputables(computables);
		computeDlg.setSelectOutOfDate(false);
		computeDlg.setComputeOnOpen(true);
		computeDlg.setVisible(true);
		_parent.updateComputeStates();
	}

}
