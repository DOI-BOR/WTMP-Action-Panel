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

import javax.swing.JButton;
import javax.swing.JPanel;

import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.actions.ActionWindowAction;

/**
 * @author mark
 *
 */
public class ActionsProjectTab extends JPanel
{
	public ActionsProjectTab()
	{
		super(new GridBagLayout());
		buildControls();
		addListener();
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		ActionWindowAction action = new ActionWindowAction();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.SOUTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JButton(action), gbc);
	}

	/**
	 * 
	 */
	private void addListener()
	{
		// TODO Auto-generated method stub
		System.out.println("addListener TODO implement me");
		
	}
	
}
