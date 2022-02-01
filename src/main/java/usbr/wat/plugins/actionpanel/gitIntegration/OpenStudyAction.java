/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * @author Mark Ackerman
 *
 */
public class OpenStudyAction extends AbstractAction
{
	public OpenStudyAction()
	{
		super("Open Study");
		putValue(Action.SHORT_DESCRIPTION, "Opens the Study for the selected Repository");
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		openStudyAction();
		
	}

	/**
	 * 
	 */
	public void openStudyAction()
	{
		// TODO Auto-generated method stub
		System.out.println("openStudyAction TODO implement me");
		
	}
}
