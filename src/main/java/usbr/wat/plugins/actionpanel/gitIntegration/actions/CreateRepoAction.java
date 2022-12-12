/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.SaveStudyAsAction;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.GitToken;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CreateRepoDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class CreateRepoAction extends AbstractGitAction
{
	private static final String CREATE_REPO_CMD = "--createrepo";
	private static final String REPO_NAME = "--newreponame";
	private static final String REMOTE_LOCATION = "--remotelocation";
	private static final String PARENT_URL = "--parenturl";
	private static final String DESCRIPTION = "--description";
	private static final String LOOK_FOR_TEXT = "Please Enter token";
	private static final String NO_HISTORY = "--nohistory";
	
	private StudyStorageDialog _studyStorageDialog;
	public CreateRepoAction(StudyStorageDialog studyStorageDialog)
	{
		super("Create Repo...", studyStorageDialog);
		_studyStorageDialog = studyStorageDialog;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		createRepoAction();
	}
	public boolean createRepoAction()
	{
		CreateRepoDialog dlg = new CreateRepoDialog(_studyStorageDialog);
		dlg.fillForm(Project.getCurrentProject());
		dlg.setVisible(true);
		
		if ( dlg.isCanceled())
		{
			return false;
		}
		String repoName = dlg.getRepoName();
		String localFolder = dlg.getLocalFolder();
		String remoteLocation = dlg.getRepoPath();
		String parentUrl = dlg.getParentUrl();
		String description = dlg.getRepoDescription();
		boolean keepHistory = dlg.shouldKeepHistory();
		
		return  createRepoAction(repoName, localFolder, remoteLocation, parentUrl, description, keepHistory);
	}
	/**
	 * 
	 */
	public boolean createRepoAction(String repoName, String localFolder, String remoteLocation, String parentUrl, String description, boolean keepHistory)
	{
		// get the repo info
		
		_studyStorageDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			String dir = Project.getCurrentProject().getProjectDirectory();
			String markerFilename = RMAIO.concatPath(dir, SaveStudyAsAction.SAVED_STUDY_AS_FILE);
			List<String>cmd = new ArrayList<>();
			cmd.add(CREATE_REPO_CMD);
			cmd.add(REPO_NAME);
			cmd.add(quoteString(repoName));
			cmd.add(LOCAL_FOLDER);
			cmd.add(quoteString(localFolder));
			cmd.add(REMOTE_LOCATION);
			cmd.add(quoteString(remoteLocation));
			cmd.add(PARENT_URL);
			cmd.add(parentUrl);
			if ( description != null && !description.isEmpty())
			{
				cmd.add(DESCRIPTION);
				cmd.add(quoteString(description));
			}
			if ( !keepHistory)
			{
				cmd.add(NO_HISTORY);
			}
			
			String token = GitToken.getGitToken(_studyStorageDialog);
			
			boolean rv = callGit(cmd, LOOK_FOR_TEXT, token);
			String msg = "Repo Created successfully";
			String title = "Success";
			if ( !rv )
			{
				msg = "Repo Creation Failed";
				title = "Failed";
			}
			else
			{
				RmaFile markerFile = FileManagerImpl.getFileManager().getFile(markerFilename);
				markerFile.delete();
				String gitFolder = RMAIO.concatPath(dir, ".git");
				String serverUrl = GitRepoUtils.getRepoUrl(_studyStorageDialog, dir);
				if ( serverUrl != null )
				{
					//add the new repo to the list of repos known about.
					RepoInfo info = new RepoInfo();


					info.setLocalPath(localFolder);
					info.setSourceUrl(serverUrl);
					info.setName(repoName);
					GitRepoUtils.addRepo(info, false);
				}

			}

			JOptionPane.showMessageDialog(_studyStorageDialog, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return rv;
		}
		finally
		{
			_studyStorageDialog.setCursor(Cursor.getDefaultCursor());
		}
	}
	
}
