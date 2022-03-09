/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJList;
import rma.swing.list.RmaListModel;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ChangesDlg extends RmaJDialog
{
	private RmaJList<String> _changesList;
	private ButtonCmdPanel _cmdPanel;

	public ChangesDlg(Window window, List<String> changes)
	{
		super(window, true);
		buildControls();
		addListeners();
		fillForm(changes);
		pack();
		setSize(400,400);
		setLocationRelativeTo(getParent());
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Files that have changed");
		
		JLabel label = new JLabel("Changed Files:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_changesList = new RmaJList<>();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_changesList), gbc);
		
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
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
					case ButtonCmdPanel.CLOSE_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * @param changes
	 */
	private void fillForm(List<String> changes)
	{
		RmaListModel<String> newModel = new RmaListModel<>(false, changes);
		_changesList.setModel(newModel);
	}
}
