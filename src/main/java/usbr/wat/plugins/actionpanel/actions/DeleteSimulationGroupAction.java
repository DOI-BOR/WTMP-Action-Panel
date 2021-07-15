/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.factories.DeleteManagerFactory;

import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
public class DeleteSimulationGroupAction extends AbstractAction
{

	private ActionsWindow _parent;

	/**
	 * @param parent
	 */
	public DeleteSimulationGroupAction(ActionsWindow parent)
	{
		super("Delete Simulation Group");
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
		int opt = JOptionPane.showConfirmDialog(_parent, "<html>Do you want to delete Simulation Group <b>"
				+simGroup.getName()+"</b> and all its simulations", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if ( opt != JOptionPane.YES_OPTION ) 
		{
			return;
		}
		boolean rv = deleteSimulationGroup(simGroup);
		if ( rv )
		{
			_parent.setSimulationGroup(null);
		}
		else
		{
			JOptionPane.showMessageDialog(_parent, "Failed to completely delete Simulation Group "
					+simGroup.getName(),"Delete Failed", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * @param simGroup
	 */
	public boolean deleteSimulationGroup(SimulationGroup simGroup)
	{
		if ( simGroup == null )
		{
			return false;
		}
		_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			List<WatSimulation> sims = simGroup.getSimulations();
			WatSimulation sim;
			boolean rv = true;
			for(int i = 0; i < sims.size(); i++ )
			{
				sim = sims.get(i);
				if (!DeleteManagerFactory.deleteManager(sim))
				{
					rv = false;
				}
			}
			rv &= DeleteManagerFactory.deleteManager(simGroup);
			return rv;
		}
		finally
		{
			_parent.setCursor(Cursor.getDefaultCursor());	
		}
	}

}
