/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.google.common.flogger.FluentLogger;
import com.rma.client.Browser;
import com.rma.client.BrowserI18n;
import com.rma.client.Messages;
import com.rma.client.NewBrowserProjectFactory;
import com.rma.client.NewObjectDialog;
import com.rma.event.ProjectAdapter;
import com.rma.factories.NewObjectFactoryList;
import com.rma.factories.NewProjectFactory;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import com.rma.model.SaveAsProjectCmd;

import hec2.wat.command.WATSaveAsProjectCommand;
import hec2.wat.factories.NewWatProjectFactory;

import rma.util.RMAIO;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class SaveStudyAsAction extends AbstractAction
{

	public static final String SAVED_STUDY_AS_FILE = ".savedStudyAs";
	private StudyStorageDialog _studyStorageDialog;
	private ProjectAdapter _listener;
	private Project _saveAsProject;

	/**
	 * @param studyStorageDialog
	 */
	public SaveStudyAsAction(StudyStorageDialog studyStorageDialog)
	{
		super("Save Study As...");
		putValue(Action.SHORT_DESCRIPTION, "Saves the Study to a new location with a new name");
		_studyStorageDialog = studyStorageDialog;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		saveProjectAsAction();
	}
	public boolean saveProjectAsAction()
	{
		_studyStorageDialog.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_studyStorageDialog.setSystemClosable(false);
		try
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

			SaveAsProjectCmd cmd = new UsbrSaveAsProjectCmd();
					//SaveProjectAsFactory.getCommand(project, _saveAsProject);
			if ( cmd != null )
			{
				cmd.saveProjectAs(project, _saveAsProject);
				return true;
			}
			return false;
		}
		finally
		{
			_studyStorageDialog.getRootPane().setCursor(Cursor.getDefaultCursor());
		}
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
		/*
		if ( currProject == _saveAsProject 
			|| (currProject.getName().equals(_saveAsProject.getName()) && RMAIO.pathsEqual(currProject.getProjectDirectory(), _saveAsProject.getProjectDirectory())) )
		{ // have the save as project open now call git to create the repo
			CreateRepoAction action = new CreateRepoAction(_studyStorageDialog);
			action.createRepoAction();
			
		}
		*/
	}
	
	class UsbrSaveAsProjectCmd extends WATSaveAsProjectCommand
	{
		public UsbrSaveAsProjectCmd()
		{
			super();
		}
		@Override
		protected void openProject()
		{
			super.openProject();
			/*
			_listener = new ProjectAdapter()
			{
				@Override
				public void projectLoaded(ProjectEvent e)
				{
					Project.removeStaticProjectListener(_listener);
					EventQueue.invokeLater(()->finishSaveAs());
					
				}
			};
			Project.addStaticProjectListener(_listener);
			*/
		}
		@Override
		public void copyFinished(int totalCopied)
		{
			super.copyFinished(totalCopied);
			writeSaveAsMarkerLine();
		}
		/**
		 * 
		 */
		private void writeSaveAsMarkerLine()
		{
			String dir = _saveAsProject.getProjectDirectory();
			String fname = RMAIO.concatPath(dir,SAVED_STUDY_AS_FILE);
			FluentLogger.forEnclosingClass().atInfo().log("Writing save as marker file to %s",fname);
			RmaFile markerFile = FileManagerImpl.getFileManager().getFile(fname);
			BufferedWriter writer = markerFile.getBufferedWriter();
			try
			{
				writer.write("Saved From:"+_origFile.getAbsolutePath());
				writer.newLine();
				writer.write("Saved by:"+ System.getProperty("user.name"));
				writer.newLine();
				writer.write("Saved On:"+ new Date().toString());
				writer.newLine();
				// previous git url?
			}
			catch ( IOException e)
			{
				Logger.getLogger("SaveStudyAsAction").warning("writeSaveAsMarkerLine:IOException writing "+markerFile.getAbsolutePath()+" error:"+e);
			}
			finally
			{
				if (writer != null )
				{
					try
					{
						writer.close();
					}
					catch (IOException e)
					{ }
				}
			}
		}
		
	}

}
