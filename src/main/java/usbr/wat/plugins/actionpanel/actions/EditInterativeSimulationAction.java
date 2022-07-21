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

import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class EditInterativeSimulationAction extends AbstractAction
{

	private ActionsWindow _parent;

	/**
	 * @param parent
	 */
	public EditInterativeSimulationAction(ActionsWindow parent)
	{
		super("Edit Iteration Settings...");
		setEnabled(false);
		_parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		SimulationGroup simGroup = _parent.getSimulationGroup();
		if ( simGroup == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
			
		}
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		EditIterationSettingsDialog dlg = new EditIterationSettingsDialog(_parent);
		dlg.fillForm(simGroup);
		if ( sims.size() > 0 )
		{
			dlg.setSelectedSimulation(sims.get(0));
		}
		dlg.setVisible(true);
		
	}

}
