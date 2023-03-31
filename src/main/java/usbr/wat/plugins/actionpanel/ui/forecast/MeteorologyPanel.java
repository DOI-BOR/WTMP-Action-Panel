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
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MetData;
import usbr.wat.plugins.actionpanel.ui.NavPlotPanel;

/**
 * @author mark
 *
 */
public class MeteorologyPanel extends AbstractForecastPanel
{

	private RmaJTable _metInfoTable;
	private JButton _importButton;
	private NavPlotPanel _plotPanel;
	private ForecastSimGroup _fsg;

	/**
	 * @param forecastPanel
	 */
	public MeteorologyPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
		addListeners();
	}

	

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Met Forecast Name", "Type", "Description", "Forecast Date"};

		_metInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() *1;
				return d;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return false;
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
		lowerPanel.add(_metInfoTable.getScrollPane(), gbc);

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
	
	/**
	 * 
	 */
	private void addListeners()
	{
		_importButton.addActionListener(e->displayImportDataWindow());
	}

	/**
	 * @return
	 */
	private void displayImportDataWindow()
	{
		ImportMetDataWindow dlg = new ImportMetDataWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(_fsg);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
	}



	@Override
	public ForecastTable getTableForPanel()
	{
		return _metTable;
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
		setEnabled(fsg != null);
		_fsg = fsg;
	}



	@Override
	protected void tableRowSelected()
	{
		ForecastTable table =  getTableForPanel();
		int row = table.getSelectedRow();
		if ( row == -1 )
		{
			
		}
		else
		{
			MetData metData = (MetData) table.getValueAt(row,0);
		}
	}

}
