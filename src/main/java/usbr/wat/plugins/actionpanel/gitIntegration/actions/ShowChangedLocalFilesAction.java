/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import hec.io.ProcessOutputLine;

import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ShowChangedLocalFilesAction extends AbstractGitAction
{

	private static final String CHANGES_CMD = "--changes";
	private static final String ALL_FLAG_CMD = "--all";

	private static final String NO_CHANGES = "No tracked files changed";
	private static final String CHANGES_START = "Files Changed:";
	

	private RepoInfo _repo;

	/**
	 * @param name
	 * @param parent
	 */
	public ShowChangedLocalFilesAction(Window parent, RepoInfo repo)
	{
		super("Show Changed Files", parent);
		_repo = repo;
		
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		getChanges();
	}

	/**
	 * @return 
	 * 
	 */
	public List<String> getChanges()
	{
		RepoInfo repo = _repo;
		if ( repo == null )
		{
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to see changes from", 
				"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return new ArrayList<>();
		}
		List<String>cmd = new ArrayList<>();
		cmd.add(CHANGES_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());
		cmd.add(ALL_FLAG_CMD);
		
		boolean rv = callGit(cmd);	
		if ( rv )
		{
			List<ProcessOutputLine> output = ShowChangesActions.parseOutput(getOutput(), CHANGES_START, NO_CHANGES);
			return ShowChangesActions.getOutputLines(output);
		}
		return new ArrayList<>();
	}

}
