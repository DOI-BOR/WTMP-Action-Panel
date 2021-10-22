/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;

import hec.gui.AbstractEditorPanel;
import hec.lang.NamedType;

import hec2.wat.model.WatSimulation;

import rma.swing.RmaInsets;
import rma.swing.RmaJTextField;
import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.model.ActionComputable;
import usbr.wat.plugins.actionpanel.model.SensitivitySettings;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SensitivityPanel extends AbstractEditorPanel
{

	private static final String TAB_NAME = "Scripts";
	
	private EditIterationSettingsDialog _parent;
	
	private ComputeScriptsPanel _preComputePanel;
	private ComputeScriptsPanel _postComputePanel;


	/**
	 * @param editIterationSettingsDialog
	 */
	public SensitivityPanel(EditIterationSettingsDialog parent)
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
		_preComputePanel = new ComputeScriptsPanel("Pre-Compute Scripts");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_preComputePanel, gbc);
		
		_postComputePanel = new ComputeScriptsPanel("Post-Compute Scripts");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_postComputePanel, gbc);	
		
		
		JLabel label = new JLabel("Script Entry Point:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		RmaJTextField txtFld = new RmaJTextField("def "+ActionComputable.METHOD_SIGNATURE+":");
		txtFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(txtFld, gbc);
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		
	}
	
	public void setSimulation(WatSimulation simulation)
	{
		_preComputePanel.setSimulation(simulation);
		_postComputePanel.setSimulation(simulation);
	}
	
	@Override
	public void fillPanel(NamedType dobj)
	{
		SensitivitySettings settings;
		if ( dobj instanceof SensitivitySettings )
		{
			settings  = (SensitivitySettings)dobj;
			_preComputePanel.fillPanel(settings.getPreComputeSettings());
			_postComputePanel.fillPanel(settings.getPostComputeSettings());
		}

	}

	@Override
	public String getTabname()
	{
		return TAB_NAME;
	}

	@Override
	public boolean savePanel(NamedType dobj)
	{
		SensitivitySettings settings;
		if ( dobj instanceof SensitivitySettings )
		{
			settings  = (SensitivitySettings)dobj;
			_preComputePanel.savePanel(settings.getPreComputeSettings());
			_postComputePanel.savePanel(settings.getPostComputeSettings());
		}
		return true;
	}

}
