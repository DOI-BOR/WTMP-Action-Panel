/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import rma.swing.ButtonCmdPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.forecast.BcData;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;

public class CreateBcWindow extends CancelableWindow<BcData>
{
	private final ForecastSimGroup _fsg;
	private ButtonCmdPanel _cmdPanel;
	private RmaJTable _opsTable;
	private RmaJTable _metTable;
	private boolean _canceled;
	private JLabel _infoLabel;

	public CreateBcWindow(ForecastSimGroup fsg, Window parent)
	{
		super(parent, "Create Boundary Conditions", true);
		_fsg = fsg;
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
	}

	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());


		JPanel tablesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS0000;
		add(tablesPanel, gbc);

		buildTables(tablesPanel);

		_infoLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_infoLabel, gbc);

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		add(_cmdPanel, gbc);
	}

	private void buildTables(JPanel tablesPanel)
	{
		String[] headers = new String[]{"Select", "Operations"};
		_opsTable= new RmaJTable(this, headers)
		{
			public boolean isCellEditable(int row, int col)
			{
				return col == 0;
			}
		};
		_opsTable.setRowHeight(_opsTable.getRowHeight()+5);
		_opsTable.setCheckBoxCellEditor(0);
		_opsTable.removePopuMenuFillOptions();
		_opsTable.removePopupMenuInsertAppendOnly();
		_opsTable.removePopupMenuSumOptions();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_opsTable.getScrollPane(), gbc);

		headers = new String[]{"Select", "Meteorology"};
		_metTable= new RmaJTable(this, headers)
		{
			public boolean isCellEditable(int row, int col)
			{
				return col == 0;
			}
		};
		_metTable.setRowHeight(_metTable.getRowHeight()+5);
		_metTable.setCheckBoxCellEditor(0);
		_metTable.removePopuMenuFillOptions();
		_metTable.removePopupMenuInsertAppendOnly();
		_metTable.removePopupMenuSumOptions();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_metTable.getScrollPane(), gbc);
	}

	private void addListeners()
	{
		_opsTable.getSelectionModel().addListSelectionListener(e->tableRowsSelected());
		_metTable.getSelectionModel().addListSelectionListener(e->tableRowsSelected());
		_cmdPanel.addCmdPanelListener(e ->
		{
			switch (e.getID())
			{
				case ButtonCmdPanel.OK_BUTTON:
					   if ( validForm())
					{
						saveForm();
						_canceled = false;
						setVisible(false);
					}
					break;
				case ButtonCmdPanel.CANCEL_BUTTON:
					_canceled = true;
					setVisible(false);
					break;
			}
		});
	}

	private void tableRowsSelected()
	{
		List<OperationsData>opsData = getSelectedOpsData();
		List<MeteorlogicData>metData = getSelectedMetData();
		int bcCnt = opsData.size()*metData.size();
		if ( bcCnt > 0 )
		{
			_infoLabel.setText("Selections will create " + bcCnt + " Boundary Condition Sets");
		}
		else
		{
			_infoLabel.setText("");
		}
	}

	private boolean validForm()
	{
		List<OperationsData>opsData = getSelectedOpsData();
		if ( opsData.size() == 0 )
		{
			JOptionPane.showMessageDialog(this,"Please Select one or more Operations.", "No Operations Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		List<MeteorlogicData>metData = getSelectedMetData();
		if ( metData.size() == 0 )
		{
			JOptionPane.showMessageDialog(this,"Please Select one or more Meteorology.", "No Meteorology Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		return true;
	}
	private void saveForm()
	{
	}
	public void fillForm(ForecastSimGroup fsg)
	{
		_opsTable.deleteCells();
		_metTable.deleteCells();
		List<OperationsData> opsData = fsg.getOperationsData();
		Vector<Object>row = new Vector<>();
		for (int i = 0; i < opsData.size(); i++ )
		{
			row = new Vector();
			row.add(Boolean.FALSE);
			row.add(opsData.get(i));
			_opsTable.appendRow(row);
		}
		List<MeteorlogicData> metData = fsg.getMeteorlogyData();
		for (int i = 0; i < metData.size(); i++ )
		{
			row = new Vector();
			row.add(Boolean.FALSE);
			row.add(metData.get(i));
			_metTable.appendRow(row);
		}
	}

	@Override
	public boolean isCanceled()
	{
		return _canceled;
	}

	public List<BcData> getBcData()
	{
		List<BcData>selectedBcs = new ArrayList<>();
		List<OperationsData>opsData = getSelectedOpsData();
		List<MeteorlogicData>metData = getSelectedMetData();
		OperationsData ops;
		MeteorlogicData met;
		for(int o = 0;o < opsData.size(); o++ )
		{
			ops = opsData.get(o);
			for(int m = 0;m < metData.size(); m++ )
			{
				met = metData.get(m);
				BcData bcData = new BcData();
				bcData.setName(ops.getName()+"-"+met.getName());
				bcData.setSelectedOps(ops);
				bcData.setSelectedMet(met);
				selectedBcs.add(bcData);
			}
		}
		return selectedBcs;
	}

	private List<MeteorlogicData> getSelectedMetData()
	{
		int rowCnt = _metTable.getNumRows();
		Object obj;
		List<MeteorlogicData>selectedMetData = new ArrayList<>();
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = _metTable.getValueAt(r,0);
			if( obj == Boolean.TRUE || "true".equals(obj.toString()))
			{
				selectedMetData.add((MeteorlogicData) _metTable.getValueAt(r,1));
			}
		}
		return selectedMetData;
	}

	private List<OperationsData> getSelectedOpsData()
	{
		int rowCnt = _opsTable.getNumRows();
		Object obj;
		List<OperationsData>selectedOpsData = new ArrayList<>();
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = _opsTable.getValueAt(r,0);
			if( obj == Boolean.TRUE || "true".equals(obj.toString()))
			{
				selectedOpsData.add((OperationsData) _opsTable.getValueAt(r,1));
			}
		}
		return selectedOpsData;
	}
}
