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

import com.rma.swing.excel.ExcelTable;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class OperationsPanel extends AbstractForecastPanel
{

	private RmaJTable _opInfoTable;
	private JButton _importButton;
	private ExcelTable _reservoirTable;
	private RmaJTable _excelTable;

	/**
	 * @param forecastPanel
	 */
	public OperationsPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Operations", "File Path", "Description", "Forecast Date"};

		_opInfoTable = new RmaJTable(this, headers)
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
		gbc.weighty   = 0.1;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_opInfoTable.getScrollPane(), gbc);

		_importButton = new JButton("Import...");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_importButton, gbc);

		_excelTable = new RmaJTable(this, new String[]{""});
				//new ExcelTable(this, null);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_excelTable.getScrollPane(), gbc);

	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _opsTable;
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
		// TODO Auto-generated method stub
		System.out.println("tableRowSelected TODO implement me");
		
	}

}
