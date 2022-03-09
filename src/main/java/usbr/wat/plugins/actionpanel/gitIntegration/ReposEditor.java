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
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.rma.swing.RmaFileChooserField;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.EnabledJPanel;
import rma.swing.RmaImage;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ReposEditor extends RmaJDialog
{
	private static final String DEFAULT_REPO_URL = "https://gitlab.rmanet.app/RMA/usbr-water-quality/wtmp-development-study/uppersac.git" ;
	private static final String DEFAULT_REPO_NAME = "Default";
	private RmaJComboBox<RepoInfo> _reposCombo;
	private JButton _addRepoButton;
	private RmaJTextField _repoNameFld;
	private RmaJTextField _srcUrlFld;
	private RmaFileChooserField _destFolderFld;
	private ButtonCmdPanel _cmdPanel;
	private JButton _deleteRepoButton;
	private JPanel _infoPanel;
	private RepoInfo _currentRepo;

	/**
	 * @param parent
	 */
	public ReposEditor(Window parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		fillRepoCombo();
		pack();
		setSize(700, 250);
		setLocationRelativeTo(getParent());
	}

	
	/**
	 * 
	 */
	protected void buildControls()
	{
		setTitle("Repository Settings");
		getContentPane().setLayout(new GridBagLayout());
		
		JLabel label = new JLabel("Repository:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_reposCombo = new RmaJComboBox<>();
		label.setLabelFor(_reposCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_reposCombo, gbc);
		
		_addRepoButton = new JButton("Add");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_addRepoButton, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		
		_infoPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS0000;
		getContentPane().add(_infoPanel, gbc);
		
		label = new JLabel("Repository Name:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
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
		_infoPanel.add(_repoNameFld, gbc);
		
		label = new JLabel("Destination Path:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
		_destFolderFld = new RmaFileChooserField();
		_destFolderFld.setOpenDirectory();
		label.setLabelFor(_destFolderFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		_infoPanel.add(_destFolderFld, gbc);
		
		label = new JLabel("Source URL:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
		_srcUrlFld = new RmaJTextField();
		label.setLabelFor(_srcUrlFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(_srcUrlFld, gbc);
		
		
		
		label = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0001;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
		
		
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0001;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS0000;
		getContentPane().add(bottomPanel, gbc);
		
		
		
		_deleteRepoButton = new JButton(RmaImage.getImageIcon("Images/trashCan.gif"));
		_deleteRepoButton.setToolTipText("Delete the current repository");
		_deleteRepoButton.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		bottomPanel.add(_deleteRepoButton, gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_APPLY_CANCEL_BUTTONS);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHEAST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		bottomPanel.add(_cmdPanel, gbc);
		
		_infoPanel.setEnabled(false);
	}
	/**
	 * 
	 */
	private void fillRepoCombo()
	{
		List<RepoInfo> repos = GitRepoUtils.getReposList();
		RmaListModel<RepoInfo>newModel = new RmaListModel<>(true, repos);
		_reposCombo.setModel(newModel);
		
	}

	/**
	 * 
	 */
	protected void addListeners()
	{
		_destFolderFld.addFocusListener(new FocusListener()
		{

			@Override
			public void focusGained(FocusEvent e)
			{
				// do nothing
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				if ( e.isTemporary() )
				{
					return;
				}
				checkForExistingRepo();
			}
			
		});
		_destFolderFld.addFileSelectedListener(e->checkForExistingRepo());
		
		_reposCombo.addItemListener(e->reposComboChanged(e));
		
		_addRepoButton.addActionListener(e->addRepoAction());
		
		_deleteRepoButton.addActionListener(e->deleteRepoAction());
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.APPLY_BUTTON :
						if ( isValidForm())
						{
							saveForm();
						}
						break;
					case ButtonCmdPanel.OK_BUTTON :
						if ( isValidForm())
						{
							saveForm();
						}
						setVisible(false);
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						setVisible(false);
						break;
				}
			}

			
		});
	}
	
	/**
	 * @return
	 */
	private void deleteRepoAction()
	{
		RepoInfo repo = getSelectedRepo();
		if ( repo != null )
		{
			int opt = JOptionPane.showConfirmDialog(this, "<html>Ok to delete Repository "+repo.getName()
				+"<br>Local Folder : "+repo.getLocalPath()+"<br>Repository URL : "+repo.getSourceUrl()
				+"<br>?<br><br>This will not delete any files on disk.",
					"Confirm Deletion", JOptionPane.YES_NO_OPTION);
			
			if ( opt == JOptionPane.YES_OPTION )
			{
				if ( GitRepoUtils.deleteRepo(repo))
				{
					if ( _currentRepo == repo )
					{
						clearForm();
					}
					_reposCombo.removeItem(repo);
					if ( _reposCombo.getItemCount() == 1)
					{
						_reposCombo.setSelectedIndex(0);
					}
					
				}
			}
		}
	}


	/**
	 * @return
	 */
	private RepoInfo getSelectedRepo()
	{
		return (RepoInfo) _reposCombo.getSelectedItem();
	}


	/**
	 * 
	 */
	protected void checkForExistingRepo()
	{
		String localFolder = _destFolderFld.getPath();
		if ( GitRepoUtils.hasGitRepo(localFolder))
		{
			RepoInfo info = new RepoInfo();
			info.setLocalPath(localFolder);
			GitRepoUtils.getRepoInfo(info);
			if ( info.getSourceUrl() != null )
			{
				_srcUrlFld.setText(info.getSourceUrl());
				_srcUrlFld.setEditable(false);
			}
		}
		else
		{
			_srcUrlFld.setEditable(true);
		}
		
	}

	@Override
	public void clearForm()
	{
		_repoNameFld.clearPerformed();
		_srcUrlFld.clearPerformed();
		_destFolderFld.clearPerformed();		
	}

	/**
	 * @return
	 */
	private void addRepoAction()
	{
		clearForm();
		_infoPanel.setEnabled(true);
		_repoNameFld.setEditable(true);
		_srcUrlFld.setEditable(true);
		_currentRepo = null;
		_reposCombo.setSelectedIndex(-1);
	}

	/**
	 * @param e
	 * @return
	 */
	private void reposComboChanged(ItemEvent e)
	{
		if (ItemEvent.DESELECTED == e.getStateChange())
		{ 
			return;
		}
		RepoInfo repo = (RepoInfo) _reposCombo.getSelectedItem();
		if ( repo != null )
		{
			_infoPanel.setEnabled(true);
			_repoNameFld.setText(repo.getName());
			_repoNameFld.setEditable(false);
			_destFolderFld.setText(repo.getLocalPath());
			_destFolderFld.setEditable(true);
			_srcUrlFld.setText(repo.getSourceUrl());
			_srcUrlFld.setEditable(false);
			_currentRepo = repo;
			_deleteRepoButton.setEnabled(true);
		}
		else 
		{
			_infoPanel.setEnabled(false);
			_repoNameFld.clearPerformed();
			_destFolderFld.clearPerformed();
			_srcUrlFld.clearPerformed();
			_currentRepo = null;
			_deleteRepoButton.setEnabled(false);
		}
		setModified(false);
		
		
	}

	private void saveForm()
	{
		if ( !isModified())
		{
			return;
		}
		if ( _currentRepo == null )
		{
			RepoInfo repo = new RepoInfo();
			repo.setName(_repoNameFld.getText());
			repo.setLocalPath(_destFolderFld.getText());
			repo.setSourceUrl(_srcUrlFld.getText());
			if ( GitRepoUtils.hasGitRepo(repo.getLocalPath()))
			{
				int opt = JOptionPane.showConfirmDialog(this, "<html>The folder "+repo.getLocalPath()
					+" appears to already have been configured to work with Git.<br>Do you want to add it anyway?", "Existing Repo", JOptionPane.YES_NO_OPTION);
				if ( JOptionPane.YES_OPTION != opt )
				{
					return;
				}
			}
			if ( GitRepoUtils.addRepo(repo, true))
			{
				_reposCombo.addItem(repo);
				_reposCombo.setSelectedItem(repo);
				int opt = JOptionPane.showConfirmDialog(this, "Do you want to download the study at " + repo.getSourceUrl(), "Download Repo", JOptionPane.YES_NO_OPTION);
				if ( JOptionPane.YES_OPTION == opt )
				{
					DownloadStudyAction action = new DownloadStudyAction(this, repo);
					String msg = "Download Complete";
					if ( !action.downloadStudyAction())
					{
						msg = "Download Failed";
					}
					JOptionPane.showMessageDialog(this,  msg, "Status", JOptionPane.INFORMATION_MESSAGE);
				}
				setModified(false);
			}
		}
		else
		{
			_currentRepo.setLocalPath(_destFolderFld.getText());
			_currentRepo.setSourceUrl(_srcUrlFld.getText());
			setModified(false);
		}
		_addRepoButton.setEnabled(true);
	}

	/**
	 * @return
	 */
	protected boolean isValidForm()
	{
		String name = _repoNameFld.getText().trim();
		if ( name.isEmpty() )
		{
			return false;
		}
		String remoteUrl = _srcUrlFld.getText();
		if ( remoteUrl.isEmpty() )
		{
			return false;
		}
		if ( !GitRepoUtils.isValidRemoteUrl(remoteUrl))
		{
			return false;
		}
		
		return true;
	}

	/**
	 * 
	 */
	public void fillForm()
	{
		setModified(false);
	}


	/**
	 * 
	 */
	public void configureForAdd()
	{
		setTitle("Add Repository");
		_addRepoButton.doClick();
		if ( _reposCombo.getItemCount() == 0 )
		{
			String defRepoUrl =System.getProperty("DefaultRepo.Url");
			if ( defRepoUrl == null )
			{
				defRepoUrl = DEFAULT_REPO_URL;
			}
			_srcUrlFld.setText(defRepoUrl);
			String defRepoName =System.getProperty("DefaultRepo.Name");
			if ( defRepoName == null )
			{
				defRepoName = DEFAULT_REPO_NAME;
			}
			_repoNameFld.setText(defRepoName);
			_addRepoButton.setEnabled(false);
		}
	}


	/**
	 * @param repo
	 */
	public void setSelectedRepo(RepoInfo repo)
	{
		_reposCombo.setSelectedItem(repo);
	}
	
	
}
