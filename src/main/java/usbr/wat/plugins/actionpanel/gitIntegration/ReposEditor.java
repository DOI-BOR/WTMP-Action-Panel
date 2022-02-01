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
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.rma.swing.RmaFileChooserField;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.EnabledJPanel;
import rma.swing.RmaImage;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJPasswordField;
import rma.swing.RmaJTextField;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ReposEditor extends RmaJDialog
{

	private RmaJComboBox<Repository> _reposCombo;
	private JButton _addRepoButton;
	private RmaJTextField _repoNameFld;
	private RmaJTextField _srcUrlFld;
	private RmaFileChooserField _destFolderFld;
	private RmaJTextField _userNameFld;
	private RmaJPasswordField _passwdFld;
	private ButtonCmdPanel _cmdPanel;
	private JComponent _deleteRepoButton;
	private JPanel _infoPanel;

	/**
	 * @param parent
	 */
	public ReposEditor(Window parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setSize(500, 300);
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
		
		
		label = new JLabel("User Name:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
		_userNameFld = new RmaJTextField();
		label.setLabelFor(_userNameFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		_infoPanel.add(_userNameFld, gbc);
		
		label = new JLabel("Password:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);
		
		_passwdFld = new RmaJPasswordField();
		label.setLabelFor(_passwdFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0001;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		_infoPanel.add(_passwdFld, gbc);
		
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
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
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_deleteRepoButton, gbc);
		
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
	protected void addListeners()
	{
		_reposCombo.addItemListener(e->reposComboChanged(e));
		
		_addRepoButton.addActionListener(e->addRepoAction());
		
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
	private void addRepoAction()
	{
		_infoPanel.setEnabled(true);
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
		Repository repo = (Repository) _reposCombo.getSelectedItem();
		StoredConfig config = repo.getConfig();
		
	}

	private void saveForm()
	{
		// TODO Auto-generated method stub
		System.out.println("saveForm TODO implement me");
		
	}

	/**
	 * @return
	 */
	protected boolean isValidForm()
	{
		// TODO Auto-generated method stub
		System.out.println("isValidForm TODO implement me");
		return false;
	}

	/**
	 * 
	 */
	public void fillForm()
	{
		// TODO Auto-generated method stub
		System.out.println("fillForm TODO implement me");
		
	}
	
	public static void main(String[] args)
	{
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String folder = "J:\\RMA Git\\usbr-wq";
		File repoDir = new File(folder, ".git");
		Repository repo;
		try
		{
			repo = builder.setGitDir(repoDir).readEnvironment() // scan  environment  GIT_*  variables
					.findGitDir() // scan up the file system tree
					.build();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		System.out.println("repo dir:"+repo.getDirectory());
		try
		{
			System.out.println("branch:"+repo.getBranch());
		}
		catch (IOException e)
		{
		}
		System.out.println("Identifier:"+repo.getIdentifier());
		System.out.println("Index file:"+repo.getIndexFile());
		System.out.println("repo state:"+repo.getRepositoryState());
		System.out.println("work tree:"+repo.getWorkTree());
		String name, subsection, value ;
		StoredConfig config = repo.getConfig();
		Set<String> sections = config.getSections();
		Iterator<String> sectionIter = sections.iterator();
		while ( sectionIter.hasNext())
		{
			String section = sectionIter.next();
			System.out.println("Section is "+section);
			Set<String> subsections = config.getSubsections(section);
			Iterator<String> subsectionsIter = subsections.iterator();
			while (subsectionsIter.hasNext())
			{
				subsection = subsectionsIter.next();
				System.out.println("\tSubsection:"+subsection);
				Set<String> names = config.getNames(section, subsection,true);
				Iterator<String> namesIter = names.iterator();
				while ( namesIter.hasNext())
				{
					name = namesIter.next();
					value = config.getString(section, subsection, name);
					System.out.println("\tName is "+name+"="+value );
				}
			}
			
		}
		repo.close();
	}

}
