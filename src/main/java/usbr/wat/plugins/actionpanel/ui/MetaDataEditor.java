/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import hec.gui.NameDescriptionPanel;

import hec2.wat.model.WatSimulation;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;

/**
 * @author mark
 *
 */
public class MetaDataEditor extends RmaJDialog
{

	private NameDescriptionPanel _nameDescPanel;
	private ButtonCmdPanel _cmdPanel;

	/**
	 * @param parent
	 */
	public MetaDataEditor(Window parent)
	{
		super(parent, "Edit Meta Data", true);
		buildControls();
		addListeners();
		pack();
		setSize(350,350);
		setLocationRelativeTo(getParent());
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		_nameDescPanel = new NameDescriptionPanel();
		_nameDescPanel.setNameEditable(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_nameDescPanel, gbc);
		
		
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
						
						saveForm();
						setVisible(false);
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * 
	 */
	protected void saveForm()
	{
		// TODO Auto-generated method stub
		System.out.println("saveForm TODO implement me");
		
	}

	/**
	 * @param obj
	 */
	public void fillForm(WatSimulation sim)
	{
		_nameDescPanel.setName(sim.getName());
		_nameDescPanel.setDescription(sim.getDescription());
	}

}
