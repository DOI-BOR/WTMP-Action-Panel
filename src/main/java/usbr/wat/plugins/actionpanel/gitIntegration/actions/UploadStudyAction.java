/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import usbr.wat.plugins.actionpanel.gitIntegration.EnterCommentsDlg;
import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UploadStudyAction extends AbstractGitAction
{
	private static final String UPLOAD_CMD = "--upload";

	private static final String COMMENTS_FILE = "--commentsfile";

	private static final String COMMENTS = "--comments";


	
	private StudyStorageDialog _studyStorageDialog;

	private EnterCommentsDlg _dlg;
	public UploadStudyAction(StudyStorageDialog studyStorageDialog)
	{
		super("Upload Study...", studyStorageDialog);
		_studyStorageDialog = studyStorageDialog;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		uploadStudyAction();
	}
	/**
	 * 
	 */
	public boolean uploadStudyAction()
	{
		String comments = getComments();
		if ( comments == null )
		{
			return false;
		}
		String commentsFile = null;
		if ( comments.contains("\n"))
		{
			commentsFile = writeComments(comments);
			if ( commentsFile == null )
			{
				return false;
			}
		}
		RepoInfo info = _studyStorageDialog.getSelectedRepo();
		List<String>cmd = new ArrayList<>();
		cmd.add(UPLOAD_CMD);
		cmd.add(LOCAL_FOLDER);
		cmd.add(quoteString(info.getLocalPath()));
		
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
		if ( commentsFile!=null )
		{
			cmd.add(COMMENTS_FILE);
			cmd.add(commentsFile);
		}
		else
		{
			cmd.add(COMMENTS);
			cmd.add(quoteString(comments));
			
		}
		return callGit(cmd);
	}
	
	
	/**
	 * @param comments
	 * @return
	 */
	private String writeComments(String comments)
	{
		File file;
		try
		{
			file = File.createTempFile("gitComments", "txt");
		}
		catch (IOException e1)
		{
			_logger.warning("Failed to write to git comments file error:"+e1);
			return null;
		}
		file.deleteOnExit();
		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(comments);
			writer.newLine();
		}
		catch (IOException e)
		{
			_logger.warning("Failed to write to git comments file " + file.getAbsolutePath()+" error:"+e);
			return null;
		}
		finally
		{
			if ( writer != null )
			{
				try
				{
					writer.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		
		return file.getAbsolutePath();
	}
	/**
	 * @return
	 */
	private String getComments()
	{
		_dlg = new EnterCommentsDlg(_studyStorageDialog, "Enter Comments", true);
		_dlg.setVisible(true);
		if ( _dlg.isCanceled())
		{
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(_dlg.getDescription());
		String comments = _dlg.getComments();
		if ( comments != null && !comments.isEmpty())
		{
			builder.append("\n");
			builder.append("\n");
			builder.append(comments);
		}
				
		return builder.toString();
	}
	
	private List<String> getSubModules()
	{
		if ( _dlg == null )
		{
			return new ArrayList<>();
		}
		return _dlg.getSelectedSubmodules();
	}

}
