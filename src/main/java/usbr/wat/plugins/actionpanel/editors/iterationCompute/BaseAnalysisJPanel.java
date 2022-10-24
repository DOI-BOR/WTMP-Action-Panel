/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagConstraints;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import hec2.wat.model.WatSimulation;

import rma.swing.RmaInsets;
import rma.swing.RmaJIntegerField;
import rma.swing.RmaJIntegerSetField;
import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;

/**
 * @author Mark Ackerman
 *
 */
public abstract class BaseAnalysisJPanel extends JPanel
{
	protected JTabbedPane _tabbedPane;
	protected RmaJIntegerSetField _groupMembersFld;
	protected RmaJIntegerField _maxMembersFld;
	protected EditIterationSettingsDialog _parent;
	
	
	private WatSimulation _selectedSim;
	/**
	 * @param gridBagLayout
	 */
	public BaseAnalysisJPanel(LayoutManager layout, EditIterationSettingsDialog parent)
	{
		super(layout);
		_parent = parent;
		buildControls();
	}

	/**
	 * 
	 */
	protected void buildControls()
	{
		
		
		JLabel label = new JLabel("Compute Members:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		_groupMembersFld = new RmaJIntegerSetField();
		label.setLabelFor(_groupMembersFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_groupMembersFld, gbc);
		
		label = new JLabel("Maximum:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		_maxMembersFld = new RmaJIntegerField();
		label.setLabelFor(_maxMembersFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_maxMembersFld, gbc);
		
		_tabbedPane = new JTabbedPane();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);
		
		JPopupMenu popup = new JPopupMenu();
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Ignore Compute Errors");
		menuItem.setSelected( Boolean.getBoolean("ActionComputable.ContinueOnError"));
		popup.add(menuItem);
		menuItem.addActionListener(e->setIgnoreComputeErrorsAction(e));
		
		setComponentPopupMenu(popup);
	}
	
	/**
	 * @param e
	 * @return
	 */
	private static void setIgnoreComputeErrorsAction(ActionEvent e)
	{
		Object obj = e.getSource();
		if ( obj instanceof JCheckBoxMenuItem )
		{
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) obj;
			boolean selected = mi.isSelected();
			if ( selected )
			{
				System.setProperty("ActionComputable.ContinueOnError", "true");
			}
			else
			{
				System.clearProperty("ActionComputable.ContinueOnError");
			}
		}
	}

	/**
	 * @param maxElement
	 */
	public void setMaxElement(int maxElement)
	{
		_maxMembersFld.setValue(maxElement);
	}
	
	public void fillPanel(BaseComputeSettings computeSettings)
	{
		int[] members = computeSettings.getMembersToCompute();
		List<Integer>iterationMembersList = new ArrayList<>();
		if ( members != null )
		{
			for (int i= 0; i < members.length;i++ )
			{
				iterationMembersList.add(members[i]);
			}
		}
		_groupMembersFld.setIntegerSet(iterationMembersList);
		
		_maxMembersFld.setValue(computeSettings.getMaximumMember());
	}
	/**
	 * @param iterationSettings
	 */
	public void savePanel(BaseComputeSettings computeSettings)
	{
		String txt = _groupMembersFld.getText();
		if ( "*".equals(txt))
		{
			
		}
		else
		{
			int[] computeMembers = _groupMembersFld.getIntegerSet();
			computeSettings.setMembersToCompute(computeMembers);
		}
		computeSettings.setMaximumMember(_maxMembersFld.getValue());
	}
	/**
	 * @return
	 */
	public boolean isValidForm()
	{
		int max = _maxMembersFld.getValueUndefined(-1);
		if ( max == -1 )
		{
			JOptionPane.showMessageDialog(this, "Please enter a Maximum Compute member value for "+getPanelName()+"Settings", "Missing Value", JOptionPane.INFORMATION_MESSAGE);
			_maxMembersFld.requestFocus();
			return false;
		}
		int[] computeMembers = _groupMembersFld.getIntegerSet();
		if ( computeMembers == null || computeMembers.length == 0) 
		{
			JOptionPane.showMessageDialog(this, "Please enter the Compute Members to compute for "+getPanelName()+" Settings", "Missing Value", JOptionPane.INFORMATION_MESSAGE);
			_groupMembersFld.requestFocus();
			return false;
			
		}
		Arrays.sort(computeMembers);
		if ( max < computeMembers[computeMembers.length-1])
		{
			JOptionPane.showMessageDialog(this, "The Maximum "+getPanelName()+" Member must be greater than or equal to the largest Compute Member", "Invalid Value", JOptionPane.INFORMATION_MESSAGE);
			_maxMembersFld.requestFocus();
			return false;
			
		}
		return true;
	}
	/**
	 * @param selectedSim
	 */
	public void setSimulation(WatSimulation selectedSim)
	{
		_selectedSim = selectedSim;
	}
	/**
	 * @return
	 */
	protected abstract String getPanelName();

}
