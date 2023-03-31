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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTarget;

/**
 * @author mark
 *
 */
public class TempTargetPanel extends AbstractForecastPanel
{

	private RmaJTable _ttInfoTable;
	private JButton _createButton;
	private RmaJTable _ttTable;

	/**
	 * @param forecastPanel
	 */
	public TempTargetPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Temperature Target Set"};

		_ttInfoTable = new RmaJTable(this, headers)
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
		gbc.weighty   = 0.03;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_ttInfoTable.getScrollPane(), gbc);

		_createButton = new JButton("Import/Create T.T. Set...");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);
		
		_ttTable = new RmaJTable(this, new String[] {"",""});
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_ttTable.getScrollPane(), gbc);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _tempTargetTable;
	}

	@Override
	protected void savePanel()
	{
		ForecastSimGroup simGrp = _forecastPanel.getSimulationGroup();
		
		List<TemperatureTarget>tempTargets = new ArrayList<>();
		// save the temp targets to the list
		simGrp.setTemperaturTargets(tempTargets);
		
	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		clearTempTargetTable();
		if ( fsg != null )
		{
			List<TemperatureTarget> tempTargets = fsg.getTemperaturTargets();
			fillTempTargetTable(tempTargets);
		}
		
	}

	/**
	 * @param tempTargets
	 */
	private void fillTempTargetTable(List<TemperatureTarget> tempTargets)
	{
		// TODO Auto-generated method stub
		System.out.println("fillTempTargetTable TODO implement me");
		
	}

	/**
	 * 
	 */
	private void clearTempTargetTable()
	{
		// TODO Auto-generated method stub
		System.out.println("clearTempTargetTable TODO implement me");
		
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
