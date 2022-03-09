/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import hec.io.ProcessOutputLine;

import usbr.wat.plugins.actionpanel.gitIntegration.ChangesDlg;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class FetchAction extends AbstractGitAction
{
	private static final String FETCH_CMD = "--fetch";
	private static final String CHANGES_START = "Pending Changes:";
	private static final String NO_CHANGES = "No files changed";
	private StudyStorageDialog _studyStorageDialog;

	public FetchAction(Dialog parent, StudyStorageDialog studyStorageDialog)
	{
		super("Fetch...",parent);
		_studyStorageDialog = studyStorageDialog;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		fetchAction();
	}
	/**
	 * 
	 */
	public List<String> fetchAction()
	{
		List<String>changes = getChanges();
		ChangesDlg dlg= new ChangesDlg(getParent(), changes);
		dlg.setVisible(true);
		return changes;
	}
	
	/**
	 * @return
	 */
	public List<String> getChanges()
	{
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();
		if ( repo == null )
		{
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to see changes for", 
				"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return new ArrayList<>();
		}
		List<String>cmd = new ArrayList<>();
		cmd.add(FETCH_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());
		
		boolean rv = callGit(cmd);
		if ( rv )
		{
			List<ProcessOutputLine> output = parseOutput(getOutput());
			return getOutputLines(output);
		}
		return new ArrayList<>();
	}

	/**
	 * @param output
	 * @return
	 */
	private static List<String> getOutputLines(List<ProcessOutputLine> output)
	{
		return output.stream().map(l->l.getLine()).collect(Collectors.toList());
	}

	/**
	 * @param output
	 * @return
	 */
	private List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output)
	{
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;
		List<ProcessOutputLine>data = new ArrayList<>(output);
		while (iter.hasNext())
		{
			line = iter.next();
			if ( line.getLine().startsWith(CHANGES_START) || line.getLine().startsWith(NO_CHANGES))
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
		{
			return data;
		}
		return output;
	}

}
