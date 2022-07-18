/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.DownloadConfirmDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author mark
 *
 */
public class RestoreStudyAction extends AbstractGitAction
{
	public static final String RESTORE_CMD = "--restore";
	private RepoInfo _repo;
	private DownloadConfirmDialog _confirmDlg;


	/**
	 * @param studyStorageDialog
	 */
	public RestoreStudyAction(StudyStorageDialog studyStorageDialog)
	{
		super("Restore...",studyStorageDialog);
		putValue(Action.SHORT_DESCRIPTION, "Restore all or part of the study from the repository.");
	}
	
	public void setRepoInfo(RepoInfo repo)
	{
		_repo = repo;
	}	
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e)
	{
		boolean rv = restoreStudyAction();
		if ( rv )
		{
			JOptionPane.showMessageDialog(getParent(), "Restore Complete",
					"Restore", JOptionPane.INFORMATION_MESSAGE);
			Window parent = getParent();
			if ( parent instanceof StudyStorageDialog )
			{
				StudyStorageDialog ssd = (StudyStorageDialog) parent;
				EventQueue.invokeLater(()->ssd.refreshChangesAction());
			}
		}
		else if ( _confirmDlg != null && !_confirmDlg.isCanceled() )  
		{ // download failed, user didn't cancel confirm dialog
			JOptionPane.showMessageDialog(getParent(), "Restore Failed",
					"Restore", JOptionPane.WARNING_MESSAGE);
			
		}
	}
	/**
	 * 
	 */
	public boolean restoreStudyAction()
	{
		if ( _repo == null )
		{
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to restore from", 
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		try
		{
			getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	
			List<String>cmd = new ArrayList<>();
			String gitCmd = null;
			String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);
			if ( FileManagerImpl.getFileManager().fileExists(gitFolder))
			{   // folder exists, so its a download.
				gitCmd = RESTORE_CMD;
				if ( !showRestoreDialog())
				{
					return false;
				}
				List<String>modules = getSubModules();
				if ( modules != null && modules.size() > 0 )
				{
					String module;
					for (int i = 0;i < modules.size(); i++ )
					{
						module = modules.get(i);
						if ( AbstractGitAction.STUDY_MODULE.equals(module)) 
						{
							cmd.add(MAIN_MODULE);
						}
						else
						{
							cmd.add(SUB_MODULE);
							cmd.add(module);
						}
					}
				}
			}
			
			cmd.add(gitCmd);
			cmd.add(LOCAL_FOLDER);
			cmd.add(_repo.getLocalPath());
			

			return callGit(cmd);
		}
		finally
		{

			getParent().setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private List<String> getSubModules()
	{
		if ( _confirmDlg == null )
		{
			return new ArrayList<>();
		}
		return _confirmDlg.getSelectedSubmodules();
	}

	/**
	 * @return
	 */
	private boolean showRestoreDialog()
	{
		_confirmDlg = new DownloadConfirmDialog(getParent(), _repo, true);
		
		_confirmDlg.setVisible(true);
		return !_confirmDlg.isCanceled();
	}
}
