/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.rma.io.FileManagerImpl;
import com.rma.swing.RmaFileChooserField;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTextField;
import rma.util.RMAIO;

/**
 * dialog for the user to select the information necessary to create a repo for a new study
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class CreateRepoDialog extends RmaJDialog
{
	private RmaJTextField _repoNameFld;
	private RmaFileChooserField _studyFolderFld;
	private RepoJTree _repoLocationTree;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;
	private RmaJTextField _repoDescFld;
	private RmaJCheckBox _preserveHistoryCheck;

	
	/**
	 * @param studyStorageDialog
	 */
	public CreateRepoDialog(Window  parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setSize(600, 450);
		setDefaultSize(450,  300);
		setLocationRelativeTo(parent);
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Create Repo");
		
		JLabel label = new JLabel("Study Folder:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_studyFolderFld = new RmaFileChooserField();
		label.setLabelFor(_studyFolderFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_studyFolderFld, gbc);	
		
		label = new JLabel("Repo Name:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_repoNameFld = new RmaJTextField();
		label.setLabelFor(_repoNameFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_repoNameFld, gbc);
		
		label = new JLabel("Repo Description:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_repoDescFld = new RmaJTextField();
		label.setLabelFor(_repoDescFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_repoDescFld, gbc);
		
		_preserveHistoryCheck = new RmaJCheckBox("Preserve History", true);
		_preserveHistoryCheck.setToolTipText("<html>When selected will keep the Git History, which can increase the size of the study.<br>When not selected will start the history with only changes going forward</html>");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_preserveHistoryCheck, gbc);
		
		
		
		_repoLocationTree = new RepoJTree(RepoJTree.SelectionType.Folder);
		label.setLabelFor(_repoLocationTree);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_repoLocationTree, gbc);
		
		
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
				
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						if ( isValidForm())
						{
							_canceled = false;
							setVisible(false);
						}
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				EventQueue.invokeLater(()->_repoLocationTree.fillRepoTree());
			}
		});
		
		
	}
	



	public boolean isCanceled()
	{
		return _canceled;
	}
	
	public void fillForm(com.rma.model.Project prj)
	{
		if ( prj != null )
		{
			_studyFolderFld.setText(prj.getProjectDirectory());
			_studyFolderFld.setEditable(false);
			_repoNameFld.setText(prj.getName());
			String dir = prj.getProjectDirectory();
			String gitDir = RMAIO.concatPath(dir, ".git");
			if ( !FileManagerImpl.getFileManager().fileExists(gitDir))
			{
				_preserveHistoryCheck.setEnabled(false);
				_preserveHistoryCheck.setSelected(false);
			}
				
		}
		else
		{
			_studyFolderFld.setEditable(true);
			_preserveHistoryCheck.setEnabled(false);
			_preserveHistoryCheck.setSelected(false);
		}
	}
	



	/**
	 * 
	 */
	protected boolean isValidForm()
	{
		if ( _repoNameFld.getText().trim().isEmpty())
		{
			return false;
		}
		if ( _studyFolderFld.getPath().isEmpty())
		{
			return false;
		}
		if ( getRepoPath() == null )
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return
	 */
	public String getRepoPath()
	{
		return _repoLocationTree.getRepoPath();
	}

	
	public boolean shouldKeepHistory()
	{
		return _preserveHistoryCheck.isSelected();
	}
	



	/**
	 * @return
	 */
	public String getRepoName()
	{
		return _repoNameFld.getText().trim();
	}
	/**
	 * 
	 * @return
	 */
	public String getRepoDescription()
	{
		return _repoDescFld.getText().trim();
	}


	/**
	 * @return
	 */
	public String getLocalFolder()
	{
		return _studyFolderFld.getPath().trim();
	}



	/**
	 * @return
	 */
	public String getParentUrl()
	{
		return _repoLocationTree.getParentUrl();
	}
	
	public static void main(String[] args)
	{
		CreateRepoDialog dlg = new CreateRepoDialog(new JFrame());
		dlg.setVisible(true);
	}

}
