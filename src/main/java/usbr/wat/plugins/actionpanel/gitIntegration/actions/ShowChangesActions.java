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

import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;

import hec.io.ProcessOutputLine;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.ChangesDlg;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ShowChangesActions extends AbstractGitAction
{
	private static final String CHANGES_CMD = "--compare-to-remote";
	private static final String FETCH_CMD   = "--fetch";
	
	private static final String COMMIT_CHANGES_START = "Pending Commits";//"Changed tracked files:";
	private static final String NO_COMMIT_CHANGES    = "No new commits.";
	private static final String FILES_CHANGED_START  = "Pending Files:";
	private static final String NO_FILES_CHANGED     = "No files changed.";
	public enum ChangeType
	{
		Files("files"),
		Commits("commits");
		
		private String _name;

		ChangeType(String name)
		{
			_name = name;
		}
		
		@Override
		public String toString()
		{
			return _name;
		}
	}
	
	private ChangeType _changeType;
	private RepoInfo _repo;
	
	public ShowChangesActions(Window parent, StudyStorageDialog studyStorageDialog, ChangeType changeType)
	{
		this(parent, studyStorageDialog.getSelectedRepo(), changeType);
	}
	
	public ShowChangesActions(Window parent, RepoInfo repo, ChangeType changeType)
	{
		super("Changes...", parent);
		_repo = repo;
		_changeType = changeType;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		
		showChangesAction();

	}
	/**
	 * 
	 */
	private void showChangesAction()
	{
		List<String>changes = getChanges();
		
		ChangesDlg dlg= new ChangesDlg(getParent(), changes);
		dlg.setVisible(true);
		
		
	}
	/**
	 * @return the number of changes behind, or null if the folder doesn't have a git folder.
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
		String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);
		if ( !FileManagerImpl.getFileManager().fileExists(gitFolder))
		{
			return null;
		}
		List<String>cmd = new ArrayList<>();
		cmd.add(FETCH_CMD);
		cmd.add(CHANGES_CMD);
		cmd.add(_changeType.toString());
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());
		
		boolean rv = callGit(cmd);
		if ( rv )
		{
			String changesStart, noChanges;
			if ( _changeType == ChangeType.Commits )
			{
				changesStart = COMMIT_CHANGES_START;
				noChanges = NO_COMMIT_CHANGES;
			}
			else
			{
				changesStart = FILES_CHANGED_START;
				noChanges = NO_FILES_CHANGED;
			}
			List<ProcessOutputLine> output = parseOutput(getOutput(), changesStart, noChanges);
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
