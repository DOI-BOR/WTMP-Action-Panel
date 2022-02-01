/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel;

import java.awt.EventQueue;

import javax.swing.JMenu;

import com.rma.client.Browser;

import usbr.wat.plugins.actionpanel.actions.ActionWindowAction;

/**
 * @author Mark Ackerman
 *
 */
public class ActionPanelPlugin
{
	private static ActionPanelPlugin _instance;
	
	private ActionsWindow _actionsWindow;
	public ActionPanelPlugin()
	{
		super();
		addToToolsMenu();
		EventQueue.invokeLater(()->displayActionsWindow());
	}

	/**
	 * 
	 */
	private void addToToolsMenu()
	{
		JMenu toolsMenu = Browser.getBrowserFrame().getToolsMenu();
		if ( toolsMenu != null )
		{
			toolsMenu.add(new ActionWindowAction());
		}
	}

	
	
	public void displayActionsWindow()
	{
		if ( _actionsWindow == null )
		{
			_actionsWindow = new ActionsWindow(Browser.getBrowserFrame());
			_actionsWindow.setLocationRelativeTo(Browser.getBrowserFrame());
		}
		_actionsWindow.setVisible(true);
	}
	/**
	 * 
	 * @return
	 */
	public ActionsWindow getActionsWindow()
	{
		return _actionsWindow;
	}

	public static void main(String[] args)
	{
		_instance = new ActionPanelPlugin();
		
	}
	
	public static ActionPanelPlugin getInstance()
	{
		return _instance;
	}
}
