/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJList;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangedLocalFilesAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CheckboxTree;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DownloadConfirmDialog extends RmaJDialog
{

	private RmaJList<String> _changesList;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled = true;
	private JLabel _fileChangesLabel;
	private JButton _fileChangesBtn;
	private List<String> _changedFiles = new ArrayList<>();
	private RmaJTextField _studyFolderFld;
	private RmaJTextField _remoteUrlFld;
	private RepoInfo _repo;
	private CheckboxTree _submoduleTree;
	private JScrollPane _treeScrollPane;
	private boolean _isRestore;

	/**
	 * @param studyStorageDialog
	 */
	public DownloadConfirmDialog(Window parent,RepoInfo repo)
	{
		this(parent, repo, false);
		
	}

	

	/**
	 * @param parent
	 * @param repo
	 * @param b
	 */
	public DownloadConfirmDialog(Window parent, RepoInfo repo, boolean isRestore)
	{
		super(parent, true);
		_repo = repo;
		_isRestore = isRestore;
		buildControls();
		addListeners();
		setSize(600,600);
		pack();
		setLocationRelativeTo(getParent());
	}



	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		if ( _isRestore )
		{
			setTitle("Restore");
		}
		else
		{
			setTitle("Download Changes");
		}
	
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS0000;
		getContentPane().add(panel, gbc);
		
		JLabel label = new JLabel("Study Folder:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);	
		
		_studyFolderFld = new RmaJTextField();
		_studyFolderFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_studyFolderFld, gbc);
		
		label = new JLabel("Remote URL:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);	
		
		_remoteUrlFld = new RmaJTextField();
		_remoteUrlFld.setToolTipText("The Repo's URL being downloaded from");
		_remoteUrlFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_remoteUrlFld, gbc);

		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
		_submoduleTree = new CheckboxTree(model, _repo) 
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowCount()*getRowHeight()+10;
				return d;
			}
		};
		_submoduleTree.setRowHeight(_submoduleTree.getRowHeight()+5);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = .75;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_treeScrollPane = new JScrollPane(_submoduleTree);
		getContentPane().add(_treeScrollPane, gbc);
	
		label = new JLabel("Changes Since Last Download");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_changesList = new RmaJList<>();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_changesList), gbc);
		
		_fileChangesLabel = new JLabel("There are -- local files that have been changed");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_fileChangesLabel, gbc);
		
		_fileChangesBtn = new JButton("...");
		_fileChangesBtn.setToolTipText("Show local changed files");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_fileChangesBtn, gbc);
		
		
		String s;
		if ( _isRestore )
		{
			s = "Restoring will update the files in the Study to the last successful download, possibly overwriting changes";
		}
		else
		{
			s = "Downloading will update the files in the Study, possibly overwriting changes";
		}
		label = new JLabel(s);
		Font f = label.getFont();
		f = f.deriveFont(Font.BOLD);
		label.setFont(f);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(label, gbc);
		
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		if ( _isRestore )
		{
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setText("Restore");
		}
		else
		{
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setText("Download");
		}
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
		_fileChangesBtn.addActionListener(e->showLocalChangedFilesAction());
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						if (isValidForm())
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
	}
	
	/**
	 * @return
	 */
	protected boolean isValidForm()
	{
		if ( _treeScrollPane.isVisible())
		{
			List<String>submodules = getSelectedSubmodules();
			if ( submodules.size() == 0 )
			{
				String msg = "Please select which area of the study to "+ (_isRestore?"restore":"download");
				String title = "No area selected";
				JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
				return false;	
			}
		}
		return true;
	}



	/**
	 * 
	 */
	private void fillForm()
	{
		RmaListModel<String>newModel = new RmaListModel(false);
		newModel.addElement("Checking for Changes...");
		_changesList.setModel(newModel);
		_submoduleTree.fillSubModules();
		_treeScrollPane.setVisible(_submoduleTree.hasSubModules()); 
		getChanges();
	}
	
	/**
	 * @return
	 */
	private void showLocalChangedFilesAction()
	{
		ChangesDlg dlg = new ChangesDlg(this,_changedFiles);
		dlg.setVisible(true);
	}

	public boolean isCanceled()
	{
		return _canceled;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		if ( visible )
		{
			fillForm();
		}
		super.setVisible(visible);
	}

	/**
	 * 
	 */
	private void getChanges()
	{
		RepoInfo repo = _repo;
		if ( repo == null )
		{
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
			
			return;
		}
		_studyFolderFld.setText(repo.getLocalPath());
		_remoteUrlFld.setText(repo.getSourceUrl());
		
		EventQueue.invokeLater(()-> getOutstandingCommits());
		EventQueue.invokeLater(()-> getChangedFiles());
		
	}

	/**
	 * 
	 */
	private void getOutstandingCommits()
	{
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		try
		{
			ShowChangesActions fetchAction = new ShowChangesActions((Window)getParent(), _repo, ShowChangesActions.ChangeType.Commits);
			List<String> changes = fetchAction.getChanges();
			if ( changes != null )
			{
				checkForSubModuleCommitsBehind(changes);
				RmaListModel<String>newModel = new RmaListModel(false, changes);
				_changesList.setModel(newModel);
			}
			else
			{
				RmaListModel<String>newModel = new RmaListModel(false);
				newModel.addElement("No Changes Detected.");
				_changesList.setModel(newModel);
				
			}
		}
		finally
		{
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
		
		
	}
	
	/**
	 * @param changes
	 */
	private void checkForSubModuleCommitsBehind(List<String> changes)
	{
		Map<String, Integer>subModuleCommitsBehindMap = new HashMap<>();
		String line, subModule;
		Integer cnt;
		int idx;
		for (int i = 0;i < changes.size(); i++ )
		{
			line = changes.get(i);
			idx = line.indexOf(':');
			if ( idx > -1 )
			{
				subModule = line.substring(0,idx).trim();
				cnt = subModuleCommitsBehindMap.get(subModule);
				if ( cnt == null )
				{
					cnt = 1;
					subModuleCommitsBehindMap.put(subModule, cnt);
				}
				else
				{
					cnt++;
					subModuleCommitsBehindMap.put(subModule, cnt);
				}
			}
		}
		updateTreeWithCommitsBehind(subModuleCommitsBehindMap);
	}



	/**
	 * @param subModuleCommitsBehindMap
	 */
	private void updateTreeWithCommitsBehind( Map<String, Integer> subModuleCommitsBehindMap)
	{
		Set<Entry<String, Integer>> entries = subModuleCommitsBehindMap.entrySet();
		Iterator<Entry<String, Integer>> iter = entries.iterator();
		Entry<String, Integer> entry;
		String subModule;
		int cnt;
		while (iter.hasNext())
		{
			entry = iter.next();
			subModule = entry.getKey();
			cnt = entry.getValue();
			_submoduleTree.setCommitsBehind(subModule, cnt);
		}
	}



	private void getChangedFiles()
	{
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		try
		{
			ShowChangedLocalFilesAction fetchAction = new ShowChangedLocalFilesAction((Window)getParent(), _repo);
			List<String> changes = fetchAction.getChanges();
			if ( changes != null && !changes.isEmpty())
			{
				_changedFiles.addAll(changes);
				if ( _changedFiles.size() == 1 )
				{
					_fileChangesLabel.setText("There is "+_changedFiles.size()+" local file that has been changed");
				}
				else
				{
					_fileChangesLabel.setText("There are "+_changedFiles.size()+" local files that have been changed");
				}
				_fileChangesBtn.setEnabled(true);
			}
			else
			{
				_fileChangesLabel.setText("There are no local files that have been changed");
				_fileChangesBtn.setEnabled(false);
			}
		}
		finally
		{
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
	}
	
	public List<String>getSelectedSubmodules()
	{
		return _submoduleTree.getCheckedSubmodules();
	}

}
