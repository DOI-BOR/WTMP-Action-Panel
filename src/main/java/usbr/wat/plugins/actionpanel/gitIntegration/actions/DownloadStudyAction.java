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

import com.rma.io.FileManagerImpl;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.DownloadConfirmDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DownloadStudyAction extends AbstractGitAction
{
	public static final String DOWNLOAD_CMD = "--download";
	public static final String CLONE_CMD = "--clone";
	
	private static final String REMOTE = "--remote";
	private DownloadConfirmDialog _confirmDlg;
	private RepoInfo _repo;
	
	public DownloadStudyAction(StudyStorageDialog studyStorageDialog)
	{
		this(studyStorageDialog, studyStorageDialog.getSelectedRepo());
	}
	
	public DownloadStudyAction(Window parent, RepoInfo repo)
	{
		super("Download Study...", parent);
		_repo = repo;
	}
	
	public void setRepoInfo(RepoInfo repo)
	{
		_repo = repo;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		boolean rv = downloadStudyAction();
		if ( rv )
		{
			JOptionPane.showMessageDialog(getParent(), "Download Complete",
					"Download", JOptionPane.INFORMATION_MESSAGE);
		}
		else if ( _confirmDlg != null && !_confirmDlg.isCanceled() )  
		{ // download failed, user didn't cancel confirm dialog
			JOptionPane.showMessageDialog(getParent(), "Download Failed",
					"Download", JOptionPane.WARNING_MESSAGE);
			
		}
	}
	/**
	 * 
	 */
	public boolean downloadStudyAction()
	{
		if ( _repo == null )
		{
			JOptionPane.showMessageDialog(getParent(), "Please select or create a Repo to download from", 
					"No Repo Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		List<String>cmd = new ArrayList<>();
		String gitCmd = null;
		String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);
		if ( FileManagerImpl.getFileManager().fileExists(gitFolder))
		{   // folder exists, so its a download.
			gitCmd = DOWNLOAD_CMD;
			if ( !showDownloadDialog())
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
		else
		{ // no folder so its a clone
			gitCmd = CLONE_CMD;
		}
		cmd.add(gitCmd);
		cmd.add(LOCAL_FOLDER);
		cmd.add(_repo.getLocalPath());
		if ( CLONE_CMD.equals(gitCmd))
		{
			cmd.add(REMOTE);
			cmd.add(quoteString(_repo.getSourceUrl()));
		}
		
		return callGit(cmd);
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
	private boolean showDownloadDialog()
	{
		_confirmDlg = new DownloadConfirmDialog(getParent(), _repo);
		_confirmDlg.setVisible(true);
		return !_confirmDlg.isCanceled();
	}

}
