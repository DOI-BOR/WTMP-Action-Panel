/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import hec.io.ProcessOutputLine;

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;

/**
 * @author Mark Ackerman
 *
 */
public class VersionAction extends AbstractGitAction
{
	private static final String VERSION_CMD = "--version";

	public VersionAction()
	{
		super("Version", ActionPanelPlugin.getInstance().getActionsWindow());
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		versionAction();
	}
	
	public String versionAction()
	{
		List<String>cmd = new ArrayList<>();
		cmd.add(VERSION_CMD);
		
		boolean rv = callGit(cmd);
		if ( rv )
		{
			String changesStart, noChanges;
			List<ProcessOutputLine> output = getOutput();
			if ( output.size() > 0 )
			{
				return output.get(0).getLine();
			}
		}
		return "Unknown";
	}
}
