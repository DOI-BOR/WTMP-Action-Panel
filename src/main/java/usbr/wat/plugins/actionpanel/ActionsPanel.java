/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.model.Project;

import rma.swing.RmaInsets;
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.DisplayReportSelectorAction;
import usbr.wat.plugins.actionpanel.actions.EditInterativeSimulationAction;
import usbr.wat.plugins.actionpanel.actions.EditSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.NewSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.PostResultsAction;
import usbr.wat.plugins.actionpanel.actions.ReviewDataAction;
import usbr.wat.plugins.actionpanel.actions.RunSimulationAction;
import usbr.wat.plugins.actionpanel.actions.SelectAlternativesAction;
import usbr.wat.plugins.actionpanel.actions.SelectSimulationGroupAction;
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;
import usbr.wat.plugins.actionpanel.actions.UpdateModelsAction;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ActionsPanel extends JPanel
{
	private ActionsWindow _parent;
	private UpdateModelsAction _updateModelsAction;
	private SelectAlternativesAction _selectAlternativeAction;
	private UpdateDataAction _updateDataAction;
	private ReviewDataAction _reviewDataAction;
	private RunSimulationAction _runSimulationAction;
	private DisplayReportSelectorAction _displayReportsAction;
	private PostResultsAction _postResultsAction;
	private EditSimulationGroupAction _editSimulationAction;
	private DeleteSimulationGroupAction _deleteSimulationAction;
	private EditInterativeSimulationAction _editInterativeSimAction;
	private NewSimulationGroupAction _newSimGroupAction;
	private SelectSimulationGroupAction _selectSimGroupAction;

	public ActionsPanel(ActionsWindow parent)
	{
		super(new GridBagLayout());
		_parent = parent;
		
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		_newSimGroupAction = new NewSimulationGroupAction(_parent);
		JButton button = new JButton(_newSimGroupAction);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
		
		_selectSimGroupAction = new SelectSimulationGroupAction(_parent);
		button = new JButton(_selectSimGroupAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
	
		_editSimulationAction = new EditSimulationGroupAction(_parent);
		button = new JButton(_editSimulationAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);

		_deleteSimulationAction = new DeleteSimulationGroupAction(_parent);
		button = new JButton(_deleteSimulationAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		add(button, gbc);
		
	
		
		_updateModelsAction = new UpdateModelsAction(_parent);
		button = new JButton(_updateModelsAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
	
		_selectAlternativeAction = new SelectAlternativesAction();
		button = new JButton(_selectAlternativeAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		//add(button, gbc);
	
		_updateDataAction = new UpdateDataAction();
		button = new JButton(_updateDataAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
	
		_reviewDataAction = new ReviewDataAction(_parent);
		button = new JButton(_reviewDataAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		add(button, gbc);
		
		_editInterativeSimAction = new EditInterativeSimulationAction(_parent);
		button = new JButton(_editInterativeSimAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);		
		
		_runSimulationAction = new RunSimulationAction(_parent);
		button = new JButton(_runSimulationAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		add(button, gbc);
	
		_displayReportsAction = new DisplayReportSelectorAction(_parent);
		button = new JButton(_displayReportsAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
		
		_postResultsAction = new PostResultsAction();
		button = new JButton(_postResultsAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(button, gbc);
	}
	/**
	 * 
	 */
	protected void addListeners()
	{
		Project.getCurrentProject().addStaticProjectListener(new ProjectAdapter()
		{

			@Override
			public void projectClosed(ProjectEvent arg0)
			{
				_newSimGroupAction.setEnabled(false);
				_selectSimGroupAction.setEnabled(false);
				enableActions(false);
			}

			@Override
			public void projectOpened(ProjectEvent arg0)
			{
				_newSimGroupAction.setEnabled(true);
				_selectSimGroupAction.setEnabled(true);
				_updateModelsAction.setEnabled(true);
			}
			
		});
	}
	/**
	 * @param sg
	 */
	public void setSimulationGroup(SimulationGroup sg)
	{
		boolean enabled = sg != null;
		enableActions(enabled);
	}
	protected void enableActions(boolean enabled)
	{
		_reviewDataAction.setEnabled(enabled);
		_editInterativeSimAction.setEnabled(enabled);
		_runSimulationAction.setEnabled(enabled);
		_displayReportsAction.setEnabled(enabled);
		_editSimulationAction.setEnabled(enabled);
		_deleteSimulationAction.setEnabled(enabled);
	}
}
