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
public class DownloadStudyAction extends AbstractStudyGitAction
{
	public static final String DOWNLOAD_CMD = "--download";
	public static final String CLONE_CMD = "--clone";
	
	private static final String OK_TO_PULL = "--okToPull";
	private static final String REMOTE = "--remote";
	private static final String SOFTOVERWRITE = "--softoverwrite";
	
	private DownloadConfirmDialog _confirmDlg;
	private RepoInfo _repo;
	private boolean _softoverwrite;
	
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
			Window parent = getParent();
			if ( parent instanceof StudyStorageDialog )
			{
				StudyStorageDialog ssd = (StudyStorageDialog) parent;
				EventQueue.invokeLater(()->ssd.refreshChangesAction());
			}
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
		try
		{
			getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	
			
			List<String>cmd = new ArrayList<>();
			boolean isClone = false;
			String gitFolder = RMAIO.concatPath(_repo.getLocalPath(), GitRepoUtils.GIT_FOLDER);
			if ( FileManagerImpl.getFileManager().fileExists(gitFolder))
			{   // folder exists, so its a download.
				cmd.add(FetchAction.FETCH_CMD);
				cmd.add(OK_TO_PULL);
				cmd.add(DOWNLOAD_CMD);
				if ( !showDownloadDialog())
				{
					return false;
				}
				if ( !askToCloseStudy())
				{
					return false;
				}
				if ( !closeStudy())
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
				cmd.add(CLONE_CMD);
				isClone = true;
			}
			cmd.add(LOCAL_FOLDER);
			cmd.add(_repo.getLocalPath());
			if ( isClone )
			{
				cmd.add(REMOTE);
				cmd.add(quoteString(_repo.getSourceUrl()));
			}
			else if ( _softoverwrite )
			{
				cmd.add(SOFTOVERWRITE);
			}
			setShowFailedCallMessage(true);
			boolean rv = callGit(cmd);
			if (rv)
			{
				return rv;
			}
			return downloadStudyAction();
		}
		finally
		{

			getParent().setCursor(Cursor.getDefaultCursor());
			openStudy();
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
	private boolean showDownloadDialog()
	{
		_confirmDlg = new DownloadConfirmDialog(getParent(), _repo);
		_confirmDlg.setVisible(true);
		_softoverwrite = _confirmDlg.shouldSoftOverWrite();
		return !_confirmDlg.isCanceled();
	}
	
	@Override
	public String getType()
	{
		return "Downloading";
	}

	/**
	 * @param softoverwrite
	 */
	public void setSoftOverwriteOnDownLoad(boolean softoverwrite)
	{
		_softoverwrite = softoverwrite;
	}

}
