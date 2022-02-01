/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UploadStudyAction extends AbstractAction
{
	public UploadStudyAction()
	{
		super("Upload Study...");
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		// TODO Auto-generated method stub
		System.out.println("actionPerformed TODO implement me");

	}

}
