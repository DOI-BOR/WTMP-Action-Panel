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
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;

import com.rma.swing.excel.ExcelTable;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;

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
	private ForecastSimGroup _fsg;

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
	protected void addListeners()
	{
		super.addListeners();
		_importButton.addActionListener(e->importOperationsAction());
	}

	private void importOperationsAction()
	{
		ImportOperationsWindow dlg = new ImportOperationsWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(_fsg);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		OperationsData opsData = dlg.getOperationsData();
		ForecastTable opsTable = getTableForPanel();
		Vector<OperationsData> row = new Vector<>();
		row.add(opsData);
		opsTable.appendRow(row);
		_fsg.getOperationsData().add(opsData);
		_fsg.setModified(true);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _opsTable;
	}

	@Override
	protected void savePanel()
	{
		if ( _fsg != null )
		{
			// TODO Auto-generated method stub
			System.out.println("savePanel TODO implement me");

		}

	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		setEnabled(fsg != null);
		_fsg = fsg;
		ForecastTable table = getTableForPanel();
		//_excelTable.setEnabled(false);
		if ( _fsg != null )
		{
			List<OperationsData> data = _fsg.getOperationsData();
			table.deleteCells();
			Vector<OperationsData> row;
			for (int i = 0; i < data.size(); i++ )
			{
				row = new Vector<>();
				row.add(data.get(i));
				table.appendRow(row);
			}
		}
	}

	@Override
	protected void tableRowSelected(int selRow)
	{
		if (_fsg != null )
		{
			ForecastTable table = getTableForPanel();
			_opInfoTable.deleteCells();
			if (selRow > -1)
			{
				OperationsData opsData = (OperationsData) table.getValueAt(selRow, 0);
				Vector row = new Vector();
				row.add(opsData.getName());
				row.add(opsData.getOperationsFile());
				row.add(opsData.getDescription());

				_opInfoTable.appendRow(row);
			}
		}
	}

}
