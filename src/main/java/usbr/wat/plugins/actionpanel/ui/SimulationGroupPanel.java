/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.list.RmaListModel;
import rma.util.RMASort;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.UpdateModelsAction;
import usbr.wat.plugins.actionpanel.actions.forecast.DeleteForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.actions.forecast.EditForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.actions.forecast.NewForecastSimGroupAction;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class SimulationGroupPanel extends EnabledJPanel
{
	private AbstractSimulationPanel _parent;
	private RmaJComboBox<ManagerProxy> _simulationGroupCombo;
	private JButton _editButton;
	private JButton _newButton;
	private JButton _deleteButton;
	private JButton _updateModelsBtn;
	private RmaJDescriptionField _descFld;
	private JTabbedPane _tabbedPane;

	public SimulationGroupPanel(AbstractSimulationPanel parent)
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
		
		JLabel label = new JLabel("Simulation Group:");
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
		
		_simulationGroupCombo = new RmaJComboBox<>();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_simulationGroupCombo, gbc);
		
		_editButton = new JButton(new EditForecastSimGroupAction());
		_editButton.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5500;
		add(_editButton, gbc);
		
		_newButton = new JButton(new NewForecastSimGroupAction(_parent));
		_newButton.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5500;
		add(_newButton, gbc);
		
		_deleteButton = new JButton(new DeleteForecastSimGroupAction(ActionPanelPlugin.getInstance().getActionsWindow()));
		_deleteButton.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_deleteButton, gbc);
		
		_updateModelsBtn = new JButton(new UpdateModelsAction(ActionPanelPlugin.getInstance().getActionsWindow()));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.EAST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,10,0,5);
		add(_updateModelsBtn, gbc);
		
		label = new JLabel("Description:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		_descFld = new RmaJDescriptionField();
		_descFld.setEditable(false);
		_descFld.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_descFld, gbc);
		
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
		
	}
	
	protected void addListeners()
	{
		Project.addStaticProjectListener(new ProjectAdapter()
		{
			@Override
			public void projectOpened(ProjectEvent e)
			{
				EventQueue.invokeLater(()->studyOpened());
			}
			@Override
			public void projectClosed(ProjectEvent e)
			{
				EventQueue.invokeLater(()->studyClosed());
			}
		});
		_simulationGroupCombo.addItemListener(e->simGroupSelected(e));

	}

	/**
	 * @param e
	 * @return
	 */
	private void simGroupSelected(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		ManagerProxy proxy = (ManagerProxy) _simulationGroupCombo.getSelectedItem();
		ForecastSimGroup simGroup = (ForecastSimGroup) proxy.loadManager();
		fillForm(simGroup);
	}

	/**
	 * @param simGroup
	 */
	private void fillForm(ForecastSimGroup simGroup)
	{
		boolean enabled = simGroup != null;
		_editButton.setEnabled(enabled);
		_parent.setSimulationGroup(simGroup);
	}

	/**
	 * @return
	 */
	protected void studyClosed()
	{
		_simulationGroupCombo.removeAllItems();
		_editButton.setEnabled(false);
		_newButton.setEnabled(false);
		_deleteButton.setEnabled(false);
	}

	/**
	 * @return
	 */
	protected void studyOpened()
	{
		_newButton.setEnabled(true);
		_deleteButton.setEnabled(true);
		loadSimulationGroupCombo();
	}

	/**
	 * @param fsg
	 */
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		if ( fsg == null )
		{
			_simulationGroupCombo.setSelectedIndex(-1);
			_descFld.setEnabled(false);
			return;
		}
		_descFld.setEnabled(true);
		RmaListModel model = (RmaListModel) _simulationGroupCombo.getModel();
		ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(fsg);
		if ( proxy == null )
		{
			_simulationGroupCombo.setSelectedIndex(-1);
			return;
		}
		if ( !model.contains(proxy))
		{
			model.addElement(proxy);
		}
		if ( _simulationGroupCombo.getSelectedItem() != proxy )
		{
			_simulationGroupCombo.setSelectedItem(proxy);
		}

		_descFld.setText(fsg.getDescription());
	}

	public void loadSimulationGroupCombo()
	{
		Project prj = Project.getCurrentProject();
		Object curProxy = _simulationGroupCombo.getSelectedItem();
		List<ManagerProxy> simGroupProxies = prj.getManagerProxyListForType(ForecastSimGroup.class);
		RMASort.quickSort(simGroupProxies);
		RmaListModel<ManagerProxy> newModel = new RmaListModel<>(false, simGroupProxies);
		_simulationGroupCombo.setModel(newModel);
		if ( newModel.contains(curProxy))
		{
			_simulationGroupCombo.setSelectedItem(curProxy);
		}
		else if ( _simulationGroupCombo.getItemCount() == 1 )
		{
			_simulationGroupCombo.setSelectedIndex(0);
		}
	}
}
