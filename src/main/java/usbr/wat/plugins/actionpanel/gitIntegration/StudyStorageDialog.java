/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class StudyStorageDialog extends RmaJDialog
{
	public static final String TITLE = "Data Storage";
	
	private RmaJComboBox<RepoInfo> _repoCombo;
	private JButton _editReposButton;
	private ButtonCmdPanel _cmdPanel;
	private RepoButtonPanel _repoButtonPanel;
	private Window _parent;
	private JLabel _msgLabel;
	private JCheckBoxMenuItem _doNothingMenu;

	private Timer _timer;

	private JCheckBoxMenuItem _debugMenu;

	private JCheckBoxMenuItem _refreshMenu;

	protected boolean _refreshChanges = true;

	private JMenuItem _browseLocalMenu;

	private boolean _firstTime;

	public StudyStorageDialog(Window parent)
	{
		super(parent, true);
		_parent = parent;
		buildControls();
		addListeners();
		pack();
		loadRepos();
		setSize(550, 400);
		setLocationRelativeTo(getParent());
		
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		setTitle(TITLE);
		getContentPane().setLayout(new GridBagLayout());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		JLabel label = new JLabel("Repository:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_repoCombo = new RmaJComboBox<RepoInfo>()
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				RepoInfo repo = (RepoInfo) getSelectedItem();
				if ( repo == null )
				{
					return super.getToolTipText(e);
				}
				StringBuilder builder = new StringBuilder();
				builder.append("<html>");
				builder.append("<b>Name:</b>");
				builder.append(repo.getName());
				builder.append("<br><b>Repo URL:</b>");
				builder.append(repo.getSourceUrl());
				builder.append("<br><b>Local Folder:</b>");
				builder.append(repo.getLocalPath());
				builder.append("<html>");
				
				return builder.toString();
				
				
			}
		};
		label.setLabelFor(_repoCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_repoCombo, gbc);
		
		_editReposButton = new JButton("...");
		_editReposButton.setToolTipText("Add or Delete Repos");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_editReposButton, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		_repoButtonPanel = new RepoButtonPanel(_parent, this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_repoButtonPanel, gbc);
		
		
		_msgLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_msgLabel, gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
		
		JPopupMenu popup = new JPopupMenu();
		_doNothingMenu = new JCheckBoxMenuItem("Test");
		_doNothingMenu.setToolTipText("Performs the commands but does not actually send the commands all the way to Git.");
		if ( Boolean.getBoolean(AbstractGitAction.DO_NOTHING_PROP))
		{
			_doNothingMenu.setSelected(true);
			setTitle(TITLE+"-Test");
		}
		_doNothingMenu.addActionListener(e->doNothingAction());
		popup.add(_doNothingMenu);
		
		_debugMenu = new JCheckBoxMenuItem("Debug Git Output");
		_debugMenu.setToolTipText("Logs the output of the Git commands to the console log");
		if ( Boolean.getBoolean(AbstractGitAction.DEBUG_OUTPUT_PROP))
		{
			_debugMenu.setSelected(true);
		}
		_debugMenu.addActionListener(e->debugAction());
		popup.add(_debugMenu);
		
		_refreshMenu = new JCheckBoxMenuItem("Refresh Status ");
		_refreshMenu.setSelected(true);
		_refreshMenu.addActionListener(e->refreshAction());
		popup.add(_refreshMenu);
		
		
		_browseLocalMenu = new JMenuItem("Browse Local Folder...");
		_browseLocalMenu.setSelected(true);
		_browseLocalMenu.addActionListener(e->browseLocalFolderAction());
		_browseLocalMenu.setEnabled(false);
		popup.add(_browseLocalMenu);
		
		((JPanel)getContentPane()).setComponentPopupMenu(popup);
	
		_firstTime = true;
	}

	/**
	 * @return
	 */
	private void browseLocalFolderAction()
	{
		RepoInfo repo = getSelectedRepo();
		if ( repo == null )
		{
			_browseLocalMenu.setEnabled(false);
			return;
		}
		File f = new File(repo.getLocalPath());
		try
		{
			Runtime.getRuntime().exec("explorer.exe " + f.getAbsolutePath());
		}
		catch(IOException ioe)
		{
			Logger.getLogger(getClass().getName()).info("Failed to launch explorer for "+f.getAbsolutePath()+" Error:"+ioe);
		}
	}



	/**
	 * @return
	 */
	private void refreshAction()
	{
		_refreshChanges = _refreshMenu.isSelected();
	}



	/**
	 * @return
	 */
	private void debugAction()
	{
		if (_debugMenu.isSelected() )
		{
			System.setProperty(AbstractGitAction.DEBUG_OUTPUT_PROP, "true");
		}
		else
		{
			System.clearProperty(AbstractGitAction.DEBUG_OUTPUT_PROP);
		}
	}



	/**
	 * @return
	 */
	private void doNothingAction()
	{
		if (_doNothingMenu.isSelected() )
		{
			System.setProperty(AbstractGitAction.DO_NOTHING_PROP, "true");
			setTitle(TITLE+"-Test");
		}
		else
		{
			System.clearProperty(AbstractGitAction.DO_NOTHING_PROP);
			setTitle(TITLE);
		}
	}



	/**
	 * 
	 */
	private void addListeners()
	{
		_repoCombo.addItemListener(e->repoComboChanged(e));
		
		_editReposButton.addActionListener(e->editReposAction(false));
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.CLOSE_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
		
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				setVisible(false);
			}
		});
	}
	/**
	 * @param e
	 * @return
	 */
	private void repoComboChanged(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		RepoInfo repo = getSelectedRepo();
		
		_repoButtonPanel.setRepoInfo(repo);
		
		_browseLocalMenu.setEnabled(repo!=null);
		if ( repo != null )
		{
			_msgLabel.setText("");
		}
		checkOutOfDate();
	}



	/**
	 * 
	 */
	private void checkOutOfDate()
	{
		ActionListener al = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refreshChangesAction();
				
			}
		};
		if ( _timer != null )
		{
			_timer.stop();
			_timer = null;
		}
		_timer = new Timer(3*1000*60, al); // every 3 minutes check
		_timer.start();
		
		SwingWorker<List<String>, Void> worker= new SwingWorker<List<String>, Void>()
		{
			

			@Override
			protected List<String> doInBackground() throws Exception
			{
				return getChanges();
				
			}

			@Override
			protected void done()
			{
				try
				{
					List<String> changes = get();
					displayChanges(changes);
					
				}
				catch (InterruptedException | ExecutionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		};
		worker.execute();
	}

	/**
	 * 
	 */
	public void refreshChangesAction()
	{
		if ( _refreshChanges )
		{
			List<String>changes = getChanges();
			displayChanges(changes);
		}	
	}

	/**
	 * @param changes
	 */
	protected synchronized void displayChanges(List<String> changes)
	{
		if ( changes == null )
		{
			_msgLabel.setText("No Local Repository detected.");
		}
		else if ( changes.size() == 1 )
		{
			_msgLabel.setText("Local copy is " + changes.size()+  " change behind.");
			_msgLabel.setToolTipText(changes.get(0));
		}
		else if (changes.size() > 1 )
		{
			_msgLabel.setText("Local copy is " + changes.size()+  " changes behind.");
			StringBuilder builder = new StringBuilder();
			builder.append("<html>");
			for(int i = 0;i < changes.size(); i++ )
			{
				builder.append(changes.get(i));
				builder.append("<br>");
			}
			builder.append("<html>");
			_msgLabel.setToolTipText(builder.toString());
		}
		else
		{
			_msgLabel.setText("Local copy is up to date");
			_msgLabel.setToolTipText(null);
		}
	}



	/**
	 * @return
	 */
	protected synchronized List<String> getChanges()
	{
		RepoInfo repo = getSelectedRepo();
		ShowChangesActions action = new ShowChangesActions(_parent, repo, ShowChangesActions.ChangeType.Commits);
		List<String>changes = action.getChanges();
		return changes;
	}
	
	



	/**
	 * 
	 */
	private void loadRepos()
	{
		List<RepoInfo> repos = GitRepoUtils.getReposList();
		if ( repos.isEmpty() && _firstTime )
		{
			EventQueue.invokeLater(()->editReposAction(true));
			_firstTime = false; // don't keep asking the user if they want to define a repo
			return;
		}
		RmaListModel<RepoInfo>newModel = new RmaListModel<>(true, repos);
		_repoCombo.setModel(newModel);
		if ( newModel.size() == 1 )
		{
			_repoCombo.setSelectedIndex(0);
		}
		else
		{
			_msgLabel.setText("Select a Repository to work with");
		}
		
	}
	/**
	 * @return
	 */
	private void editReposAction(boolean configureForAdd)
	{
		ReposEditor editor = new ReposEditor(_parent);
		editor.fillForm();
		RepoInfo repo = getSelectedRepo();
		if ( repo != null )
		{
			editor.setSelectedRepo(repo); 
		}
		if ( configureForAdd )
		{
			editor.configureForAdd();
		}
		editor.setVisible(true);
		
		loadRepos();
		if ( ((RmaListModel)_repoCombo.getModel()).contains(repo))
		{
			_repoCombo.setSelectedItem(repo);
		}
	}



	/**
	 * @return
	 */
	public RepoInfo getSelectedRepo()
	{
		return (RepoInfo) _repoCombo.getSelectedItem();
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if ( !visible)
		{
			if ( _timer != null )
			{
				_timer.stop();
				_timer = null;
			}
		}
		
	}
}
