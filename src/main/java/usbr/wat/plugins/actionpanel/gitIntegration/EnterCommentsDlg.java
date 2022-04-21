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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
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
import rma.swing.RmaJTextArea;
import rma.swing.RmaJTextField;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangedLocalFilesAction;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CheckboxTree;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class EnterCommentsDlg extends RmaJDialog
{
	private RmaJTextArea _textArea;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;
	private RmaJTextField _studyFld;
	private RmaJTextField _remoteUrlFld;
	private RmaJTextField _descriptionFld;
	private JButton _changesBtn;
	private StudyStorageDialog _studyStorageDialog;
	private JLabel _fileChangesLabel;
	private List<String> _changedFiles = new ArrayList<>();
	private CheckboxTree _submoduleTree;
	private JScrollPane _treeScrollPane;

	
	/**
	 * @param modelsLockingTester
	 * @param string
	 * @param b
	 */
	public EnterCommentsDlg(StudyStorageDialog parent, String title,
			boolean modal)
	{
		super(parent,title, modal);
		_studyStorageDialog = parent;
		buildControls();
		addListeners();
		fillForm();
		pack();
		setSize(400,550);
		setLocationRelativeTo(parent);
	}

	
	/**
	 * 
	 */
	protected void buildControls()
	{
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());
	
		JLabel label = new JLabel("Study Folder:");
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
		
		_studyFld = new RmaJTextField();
		_studyFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_studyFld, gbc);
		
		label = new JLabel("Remote URL:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_remoteUrlFld = new RmaJTextField();
		_remoteUrlFld.setToolTipText("The Repo's URL being uploaded to");
		_remoteUrlFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_remoteUrlFld, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS0000;
		getContentPane().add(panel, gbc);
		
		_fileChangesLabel = new JLabel("There are -- local files that have been changed");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.001;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		panel.add(_fileChangesLabel, gbc);
		
		_changesBtn = new JButton("...");
		_changesBtn.setToolTipText("Show local changed files");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		panel.add(_changesBtn, gbc);
	
		DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
		_submoduleTree = new CheckboxTree(model, _studyStorageDialog)
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
		gbc.weighty   = 0.75;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_treeScrollPane = new JScrollPane(_submoduleTree);
		getContentPane().add(_treeScrollPane, gbc);
		
	
		JPanel descriptionPanel = new JPanel(new GridBagLayout());
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Change Description"));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(descriptionPanel, gbc);
		
		label = new JLabel("Description:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		descriptionPanel.add(label, gbc);
		
		_descriptionFld = new RmaJTextField();
		_descriptionFld.setMaxLength(120);
		_descriptionFld.setToolTipText("Brief description of changes for the upload");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		descriptionPanel.add(_descriptionFld, gbc);
		
		label = new JLabel("Comments:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		descriptionPanel.add(label, gbc);
		
		_textArea = new RmaJTextArea();
		_textArea.setToolTipText("Detailed description of the changes being uploaded");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		descriptionPanel.add(new JScrollPane(_textArea), gbc);
		
		
	
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		JButton okButton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		okButton.setText("Upload");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * 
	 */
	protected void addListeners()
	{
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				_canceled = true;
				setVisible(false);
			}
		});
		
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						if ( dlgStateOk() )
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
		_changesBtn.addActionListener(e->showChangesAction());
		
	}
	/**
	 * @return
	 */
	private void showChangesAction()
	{
		ChangesDlg dlg = new ChangesDlg(this,_changedFiles);
		dlg.setVisible(true);
	}


	/**
	 * 
	 */
	private void fillForm()
	{
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();
		if ( repo != null )
		{
			_remoteUrlFld.setText(repo.getSourceUrl());
			_studyFld.setText(repo.getLocalPath());
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(true);
		}
		else
		{
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
		}
		_submoduleTree.fillSubModules();
		_treeScrollPane.setVisible(_submoduleTree.hasSubModules()); 
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		if ( visible )
		{
			getChanges();
		}
		super.setVisible(visible);
	}
	/**
	 * 
	 */
	private void getChanges()
	{
		EventQueue.invokeLater(()-> getChangedFiles());
	}

	private void getChangedFiles()
	{
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		try
		{
			ShowChangedLocalFilesAction fetchAction = new ShowChangedLocalFilesAction(_studyStorageDialog, _studyStorageDialog.getSelectedRepo());
			List<String> changes = fetchAction.getChanges();
			if ( changes != null && !changes.isEmpty())
			{
				_changedFiles.addAll(changes);
				if ( changes.size() == 1 )
				{
					_fileChangesLabel.setText("There is "+_changedFiles.size()+" local file that has been changed");
				}
				else
				{
					_fileChangesLabel.setText("There are "+_changedFiles.size()+" local files that have been changed");
				}
				_changesBtn.setEnabled(true);
			}
			else
			{
				_fileChangesLabel.setText("There are no local files that have been changed");
				_changesBtn.setEnabled(false);
			}
		}
		finally
		{
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
	}
	/**
	 * @return
	 */
	protected boolean dlgStateOk()
	{
		String text = _descriptionFld.getText();
		
		if ( text == null || text.isEmpty())
		{
			String msg = "A Description must be entered to upload files";
			//"You Must Enter a Comment to Upload Files";
			String title ="Missing Description";
			//"Missing Comment";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;	
		}
		if ( _treeScrollPane.isVisible())
		{
			List<String>submodules = getSelectedSubmodules();
			if ( submodules.size() == 0 )
			{
				String msg = "Please select which area of the study to upload";
				String title = "No area selected";
				JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
				return false;	
			}
		}
				
		/*   should we force the user to enter more than a one liner?
		text = _textArea.getText();
		if ( text == null || text.isEmpty() ) 
		{
			String msg = "Comments must be entered to upload files";
					//"You Must Enter a Comment to Upload Files";
			String title ="Missing Comments";
					//"Missing Comment";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		*/
		return true;
	}

	public void setComments(String text)
	{
		_textArea.setText(text);
	}
	
	public String getComments()
	{
		StringBuilder builder = new StringBuilder();
		String comments = _textArea.getText();
		if ( comments != null && !comments.isEmpty())
		{
			builder.append(comments);
		}
		
		return builder.toString();
	}

	public String getDescription()
	{
		return _descriptionFld.getText();
	}
	public boolean isCanceled()
	{
		return _canceled;
	}
	
	public List<String>getSelectedSubmodules()
	{
		return _submoduleTree.getCheckedSubmodules();
	}
	
	
}
