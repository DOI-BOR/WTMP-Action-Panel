/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.model;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJPasswordField;

/**
 * @author mark
 *
 */
public class GitToken
{
	
	private static final String GIT_TOKEN_PROP = "GitToken";
	private static String PRIVATE_TOKEN = System.getProperty(GIT_TOKEN_PROP);
	
	private GitToken()
	{
		
	}

	/**
	 * @param studyStorageDialog
	 * @return
	 */
	public static String getGitToken(Window parentForDialog)
	{
		if ( PRIVATE_TOKEN != null && !PRIVATE_TOKEN.isEmpty() )
		{
			return PRIVATE_TOKEN;
		}
		JPanel panel = new JPanel(new GridBagLayout());
		JLabel label = new JLabel("Git Token:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		RmaJPasswordField passFld = new RmaJPasswordField();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(passFld, gbc);
		
		label = new JLabel("");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		RmaJCheckBox showPassChk = new RmaJCheckBox("Show Token");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(showPassChk, gbc);
		
		final char origEchoChar = passFld.getEchoChar();
		showPassChk.addActionListener(e->
		{
			if ( showPassChk.isSelected())
			{
				passFld.setEchoChar((char)0);
			}
			else
			{
				passFld.setEchoChar(origEchoChar);
			}
						
		});
		
		int opt = JOptionPane.showConfirmDialog(parentForDialog, panel, "Enter Token", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if ( opt == JOptionPane.OK_OPTION )
		{
			String token = passFld.getText();
			if ( !token.trim().isEmpty())
			{
				PRIVATE_TOKEN = token;
				System.setProperty(GIT_TOKEN_PROP, token);
				return token;
			}
		}
		return null;
	}
	
	public static void clearGitToken()
	{
		PRIVATE_TOKEN = null;
		System.clearProperty(GIT_TOKEN_PROP);
	}
}
