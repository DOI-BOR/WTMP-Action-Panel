/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ui.AboutDialog;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class AboutAction extends AbstractAction
{
	public AboutAction()
	{
		super("About...");
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		AboutDialog dlg = new AboutDialog(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.setVisible(true);
	}

}
