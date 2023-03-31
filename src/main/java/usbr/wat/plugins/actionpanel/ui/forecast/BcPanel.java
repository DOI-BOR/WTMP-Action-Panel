/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

import javax.swing.JButton;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.NavPlotPanel;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class BcPanel extends AbstractForecastPanel
{

	private RmaJTable _bcInfoTable;
	private JButton _createButton;
	private NavPlotPanel _plotPanel;

	/**
	 * @param forecastPanel
	 */
	public BcPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		
		String[] headers = new String[] {"Boundary Condition Set"};

		_bcInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() *1;
				return d;
			}
		};
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.01;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_bcInfoTable.getScrollPane(), gbc);

		_createButton = new JButton("Create B.C. Sets...");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);
		
		_plotPanel = new NavPlotPanel();
		_plotPanel.getPlotPanel().buildDefaultComponents();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_plotPanel, gbc);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _bcTable;
	}

	@Override
	protected void savePanel()
	{
		// TODO Auto-generated method stub
		System.out.println("savePanel TODO implement me");
		
	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		// TODO Auto-generated method stub
		System.out.println("setSimulationGroup TODO implement me");
		
	}

	@Override
	protected void tableRowSelected()
	{
		ForecastTable table = getTableForPanel();
		int row = table.getSelectedRow();
		if ( row == -1 )
		{
			// clear panel
		}
		else
		{
			//fill Panel
		}
	}

}
