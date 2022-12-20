/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.model.Project;

import hec.io.FileManagerImpl;

import rma.swing.RmaInsets;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.RestoreStudyAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.UploadStudyAction;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class RepoButtonPanel extends JPanel
{
	private DownloadStudyAction _downloadStudyAction;
	private UploadStudyAction _uploadStudyAction;
	private OpenStudyAction _openStudyAction;
	private RestoreStudyAction _restoreStudyAction;
	
	private JButton _downloadStudyButton;
	private JButton _uploadStudyButton;
	private JButton _restoreStudyButton;
	private JButton _openStudyButton;
	
	private Window _parent;
	private StudyStorageDialog _studyStorageDialog;
	private SaveStudyAsAction _saveStudyAsAction;
	private JButton _saveStudyAsButton;
	private ProjectAdapter _projectListener;

	public RepoButtonPanel(Window parent, StudyStorageDialog studyStorageDialog)
	{
		super(new GridBagLayout());
		_parent = parent;
		_studyStorageDialog = studyStorageDialog;
		buildControls();
		addListeners();
		setButtonsEnabled(false);
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		_downloadStudyAction = new DownloadStudyAction(_studyStorageDialog);
		_downloadStudyButton =  new JButton(_downloadStudyAction);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_downloadStudyButton, gbc);
		
		_uploadStudyAction = new UploadStudyAction(_studyStorageDialog);
		_uploadStudyButton =  new JButton(_uploadStudyAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_uploadStudyButton, gbc);
		
		_restoreStudyAction = new RestoreStudyAction(_studyStorageDialog);
		_restoreStudyButton = new JButton(_restoreStudyAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_restoreStudyButton, gbc);
		
		_openStudyAction = new OpenStudyAction(_studyStorageDialog);
		_openStudyButton = new JButton(_openStudyAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.insets(10,5,0,5);
		add(_openStudyButton, gbc);
		
		_saveStudyAsAction = new SaveStudyAsAction(_studyStorageDialog);
		_saveStudyAsButton = new JButton(_saveStudyAsAction);
		_saveStudyAsButton.setEnabled(!Project.getCurrentProject().isNoProject());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.insets(10,5,0,5);
		if ( Boolean.getBoolean("WTMP.HasSaveStudyAs"))
		{
			add(_saveStudyAsButton, gbc);
		}
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JLabel(), gbc);
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_projectListener = new ProjectAdapter()
		{
			@Override
			public void projectOpened(ProjectEvent e)
			{
				Project prj = e.getProject();
				boolean enabled = prj.isNoProject();
				_saveStudyAsButton.setEnabled(enabled);
			}
		};
		Project.addStaticProjectListener(_projectListener);
	}	
	/**
	 * @param b
	 */
	private void setButtonsEnabled(boolean enabled)
	{
		_uploadStudyAction.setEnabled(shouldEnableUploadButton(enabled));
		_downloadStudyButton.setEnabled(enabled);
		_restoreStudyButton.setEnabled(enabled);
		_openStudyAction.setEnabled(enabled);
	}
	/**
	 * @param enabled
	 * @return
	 */
	private boolean shouldEnableUploadButton(boolean enabled)
	{
		if ( _studyStorageDialog.getSelectedRepo() == null && !Project.getCurrentProject().isNoProject())
		{  // project opened, no selected repo, is there a saved study as file?
			String dir = Project.getCurrentProject().getProjectDirectory();
			String checkFile = RMAIO.concatPath(dir, SaveStudyAsAction.SAVED_STUDY_AS_FILE);
			if ( FileManagerImpl.getFileManager().fileExists(checkFile))
			{
				return true;
			}
			
		}
		return enabled;
	}

	public void setRepoInfo(RepoInfo repo)
	{
		_downloadStudyAction.setRepoInfo(repo);
		_restoreStudyAction.setRepoInfo(repo);
		setButtonsEnabled( repo != null );
	}



	/**
	 * @param softoverwrite
	 */
	public void setSoftOverwriteOnDownload(boolean softoverwrite)
	{
		_downloadStudyAction.setSoftOverwriteOnDownLoad(softoverwrite);
	}

	/**
	 * 
	 */
	public void closing()
	{
		Project.removeStaticProjectListener(_projectListener);
	}
}
