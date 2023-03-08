/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.extract.model;

import java.awt.Window;

import usbr.wat.plugins.actionpanel.ui.LoginDialog;

/**
 * @author mark
 *
 */
public class ExtractLoginInfo
{
	private static String _userName;
	private static String _password;
	
	private ExtractLoginInfo()
	{
		super();
	}
	
	public static boolean askForLoginInfo(Window parent, String infoString)
	{
		LoginDialog dlg = new LoginDialog(parent,"Enter Login Info");
		dlg.setInfo(infoString);
				//"Enter login information for "+GRAB_DATA_URL);
		dlg.setUserName(_userName);
		dlg.setPassword(_password);
		
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return false;
		}
		_userName = dlg.getUserName();
		_password = dlg.getPassword();
		return true;
			
	}
	
	public static String getUserName()
	{
		return _userName;
	}
	
	public static String getPassword()
	{
		return _password;
	}
}
