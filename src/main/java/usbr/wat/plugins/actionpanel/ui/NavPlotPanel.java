/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import hec.gfx2d.G2dPanel;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaNavigationPanel;
import usbr.wat.plugins.actionpanel.editors.UsbrG2dDialog.DssItem;

/**
 * @author mark
 *
 */
public class NavPlotPanel extends EnabledJPanel
{
	private JLabel _label;
	private RmaJComboBox _dssPathCombo;
	private RmaNavigationPanel _navPanel;
	private G2dPanel _plotPanel;

	public NavPlotPanel()
	{
		super(new GridBagLayout());
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		_label = new JLabel("Location:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_label, gbc);
		
		
		_dssPathCombo = new RmaJComboBox()
		{
			@Override
			public String getToolTipText(MouseEvent e )
			{
				DssItem dssItem = (DssItem) _dssPathCombo.getSelectedItem();
				/*
				if ( dssItem != null )
				{
					return getToolTip(dssItem.dssIds);
				}
				*/
				return null;
			}
		};
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_dssPathCombo, gbc);
		
		
		_navPanel = new RmaNavigationPanel();
		_navPanel.fillForm(_dssPathCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_navPanel, gbc);
		
		_plotPanel = new G2dPanel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_plotPanel, gbc);
	}
	
	/**
	 * 
	 */
	private void addListeners()
	{
		
		
	}
	
	public G2dPanel getPlotPanel()
	{
		return _plotPanel;
	}
	
}
