/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.ListSelectionModel;

import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SelectSimulationGroupDialog extends RmaJDialog
{


	private static final int SIM_GROUP_COL = 0;
	
	private RmaJTable _simGroupTable;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;

	/**
	 * @param parent
	 * @param b
	 */
	public SelectSimulationGroupDialog(ActionsWindow parent, boolean modal)
	{
		super(parent, modal);
		buildControls();
		addListeners();
		fillForm();
		pack();
		setLocationRelativeTo(getParent());
	}

	
	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		
		setTitle("Select Simulation Group");
		
		String[] headers = new String[] {"Simulation", "Description", "Analysis Period"};
		_simGroupTable = new RmaJTable(this, headers);
		_simGroupTable.setCellSelectionEnabled(false);
		_simGroupTable.setRowSelectionAllowed(true);
		_simGroupTable.setRowHeight(_simGroupTable.getRowHeight()+5);
		_simGroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_simGroupTable.setColumnEnabled(false, 0);
		_simGroupTable.setColumnEnabled(false, 1);
		_simGroupTable.setColumnEnabled(false, 2);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupTable.getScrollPane(), gbc);
	
	
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
		
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						_canceled = false;
						setVisible(false);
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * 
	 */
	private void fillForm()
	{
		_simGroupTable.deleteCells();
		
		Project proj = Project.getCurrentProject();
		List<SimulationGroup> simGroups = proj.getManagerListForType(SimulationGroup.class);
		SimulationGroup sg;
		Vector<Object> row;
		WatAnalysisPeriod ap;
		for(int i = 0;i < simGroups.size(); i++ )
		{
			sg = simGroups.get(i);
			if ( sg.isTransitory())
			{ // not in group simgroup
				continue;
			}
			row = new Vector<>();
			row.add(sg);
			row.add(sg.getDescription());
			ap = sg.getAnalysisPeriod();
			if ( ap != null )
			{
				row.add(ap);
			}
			else
			{
				row.add("<unknown>");
			}
			_simGroupTable.appendRow(row);
			
			
		}
		if ( _simGroupTable.getRowCount() == 1)
		{
			_simGroupTable.setSelectedIndices(0);
		}
	}

	public boolean isCanceled()
	{
		return _canceled;
	}

	public SimulationGroup getSelectedSimulationGroup()
	{
		int row = _simGroupTable.getSelectedRow();
		if ( row > -1 )
		{
			SimulationGroup sg = (SimulationGroup) _simGroupTable.getValueAt(row, SIM_GROUP_COL);
			return sg;
		}
		return null;
	}

	

}
