/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
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

import javax.swing.JLabel;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJPasswordField;
import rma.swing.RmaJTextField;

/**
 * @author Mark Ackerman
 *
 */
public class LoginDialog extends RmaJDialog
{

	private JLabel _infoLabel;
	private RmaJTextField _userNameFld;
	private RmaJPasswordField _passwordFld;
	private ButtonCmdPanel _cmdPanel;
	
	private boolean _canceled;

	/**
	 * @param string
	 * @param extractDialog
	 */
	public LoginDialog(Window parent, String title)
	{
		super(parent, true);
		buildControls(title);
		addListeners();
		pack();
		setSize(325,175);
		setLocationRelativeTo(parent);
	}

	/**
	 * @param title
	 */
	private void buildControls(String title)
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle(title);
		
		_infoLabel = new JLabel();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_infoLabel, gbc);
		
		JLabel label = new JLabel("User Name:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_userNameFld = new RmaJTextField();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_userNameFld, gbc);
		
		label = new JLabel("Password:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_passwordFld = new RmaJPasswordField();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_passwordFld, gbc);
		
		
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
	 * @param string
	 */
	public void setInfo(String info)
	{
		if ( info != null )
		{
			_infoLabel.setText(info);
		}
	}

	/**
	 * @return
	 */
	public boolean isCanceled()
	{
		return _canceled;
	}
	public void setUserName(String userName)
	{
		if ( userName != null )
		{
			_userNameFld.setText(userName);
		}
	}
	public void setPassword(String password)
	{
		if ( password != null )
		{
			_passwordFld.setText(password);
		}
	}
	/**
	 * @return
	 */
	public String getUserName()
	{
		return _userNameFld.getText().trim();
	}

	/**
	 * @return
	 */
	public String getPassword()
	{
		return _passwordFld.getText().trim();
	}

}
