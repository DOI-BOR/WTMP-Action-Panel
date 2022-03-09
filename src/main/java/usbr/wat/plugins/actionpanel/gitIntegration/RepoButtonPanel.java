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
import javax.swing.JPanel;

import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;
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
	private JButton _downloadStudyButton;
	private UploadStudyAction _uploadStudyAction;
	private JButton _uploadStudyButton;
	private OpenStudyAction _openStudyAction;
	private Window _parent;
	private StudyStorageDialog _studyStorageDialog;

	public RepoButtonPanel(Window parent, StudyStorageDialog studyStorageDialog)
	{
		super(new GridBagLayout());
		_parent = parent;
		_studyStorageDialog = studyStorageDialog;
		buildControls();
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
		
		_openStudyAction = new OpenStudyAction(_studyStorageDialog);
		_uploadStudyButton =  new JButton(_openStudyAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_uploadStudyButton, gbc);
		
	}
	public void setRepoInfo(RepoInfo repo)
	{
		_downloadStudyAction.setRepoInfo(repo);
	}
}
