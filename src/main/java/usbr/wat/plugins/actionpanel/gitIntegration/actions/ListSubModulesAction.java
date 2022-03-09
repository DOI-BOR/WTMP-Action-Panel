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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import hec.io.ProcessOutputLine;

import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ListSubModulesAction extends AbstractGitAction
{
	public static final String NO_SUBMODULES = "No Submodules detected.";
	public static final String SUBMODULES_START = "Submodules in main repo:";
	private static final String LIST_SUBMODULES_CMD = "--listsubmodules";
	
	private RepoInfo _repo;
	
	public ListSubModulesAction(Window parent, StudyStorageDialog ssd)
	{
		this(parent, ssd.getSelectedRepo());
	}
	public ListSubModulesAction(Window parent, RepoInfo repo)
	{
		super("List Submodules",parent);
		_repo = repo;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		getSubModules();
	}
	/**
	 * @return
	 */
	public List<String> getSubModules()
	{
		RepoInfo repo = _repo;
		if ( repo == null )
		{
			_logger.info("No Repo selected. Can't get submodules");
			return new ArrayList<>();
		}
		List<String>cmd = new ArrayList<>();
		cmd.add(LIST_SUBMODULES_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());
		
		boolean rv = callGit(cmd);
		if ( rv )
		{
			String changesStart, noChanges;
			List<ProcessOutputLine> output = parseOutput(getOutput(), SUBMODULES_START, NO_SUBMODULES);
			return getOutputLines(output);
		}
		return new ArrayList<>();	
	}

	/**
	 * @param output
	 * @return
	 */
	static List<String> getOutputLines(List<ProcessOutputLine> output)
	{
		return output.stream().map(l->l.getLine()).collect(Collectors.toList());
	}

	/**
	 * @param output
	 * @return
	 */
	static List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output, String changesStart, String noChanges)
	{
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;
		List<ProcessOutputLine>data = new ArrayList<>(output);
		while (iter.hasNext())
		{
			line = iter.next();
			if ( line.getLine().startsWith(changesStart) || line.getLine().startsWith(noChanges))
			{
				iter.remove();
				foundStart = true;
				break;
			}
			else
			{
				iter.remove();
			}
		}
		if (!foundStart )
		{ //didn't find what we were looking for so return all the output
			return data;
		}
		return output;
	}
}
