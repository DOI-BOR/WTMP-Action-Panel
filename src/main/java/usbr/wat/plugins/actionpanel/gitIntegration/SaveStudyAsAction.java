/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.rma.client.Browser;
import com.rma.client.BrowserI18n;
import com.rma.client.Messages;
import com.rma.client.NewBrowserProjectFactory;
import com.rma.client.NewObjectDialog;
import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.factories.NewObjectFactoryList;
import com.rma.factories.NewProjectFactory;
import com.rma.factories.SaveProjectAsFactory;
import com.rma.model.Project;
import com.rma.model.SaveAsProjectCmd;

import hec2.wat.factories.NewWatProjectFactory;

import rma.util.RMAIO;

/**
 * @author mark
 *
 */
public class SaveStudyAsAction extends AbstractAction
{

	private StudyStorageDialog _studyStorageDialog;
	private ProjectAdapter _listener;
	private Project _saveAsProject;

	/**
	 * @param studyStorageDialog
	 */
	public SaveStudyAsAction(StudyStorageDialog studyStorageDialog)
	{
		super("Save Study As...");
		putValue(Action.SHORT_DESCRIPTION, "Saves the Study to a new location,creates a repo for it and uploads it to the new repo");
		_studyStorageDialog = studyStorageDialog;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		_listener = new ProjectAdapter()
		{
			@Override
			public void projectOpened(ProjectEvent e)
			{
				Project.removeStaticProjectListener(_listener);
				finishSaveAs();
			}
		};
		Project.addStaticProjectListener(_listener);
		if ( !saveProjectAsAction() ) 
		{
			Project.removeStaticProjectListener(_listener);
			return;
		}

	}
	public boolean saveProjectAsAction()
	{
		Project project = Project.getCurrentProject();
		project.saveProject(); // write everything out
		NewProjectFactory factory = (NewProjectFactory) NewObjectFactoryList
				.getFactoryFor(Browser.getBrowser().getProjectLabel());
		if (factory == null)
		{
			factory = new NewBrowserProjectFactory(project);
		}
		if ( factory instanceof NewWatProjectFactory )
		{
			((NewWatProjectFactory)factory).setSaveAsPanel(true);
		}
		NewObjectDialog dialog = new NewObjectDialog(_studyStorageDialog, factory);
		String title = BrowserI18n.getI18n(Messages.SAVE_PROJECT_AS_LABEL).format(Browser.getBrowser().getProjectLabel());
		dialog.setTitle(title);
		dialog.setVisible(true);
		if (dialog.isCanceled())
		{
			return false;
		}
		_saveAsProject = (Project) dialog.getNewObject();
		
		SaveAsProjectCmd cmd = SaveProjectAsFactory.getCommand(project, _saveAsProject);
		if ( cmd != null )
		{
			cmd.saveProjectAs(project, _saveAsProject);
			return true;
		}
		return false;
	}
	/**
	 * 
	 */
	protected void finishSaveAs()
	{
		Project currProject = Project.getCurrentProject();
		if ( currProject.isNoProject())
		{
			return;
		}
		if ( currProject == _saveAsProject 
			|| (currProject.getName().equals(_saveAsProject.getName()) && RMAIO.pathsEqual(currProject.getProjectDirectory(), _saveAsProject.getProjectDirectory())) )
		{ // have the save as project open now call git to create the repo
			
		}
	}

}
