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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.forecast.BcData;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;

public class EditEnsembleSetWindow extends RmaJDialog
{
	private JLabel _infoLabel;
	private ButtonCmdPanel _cmdPanel;
	private RmaJTable _bcTable;
	private RmaJTable _tempTargetSetTable;
	private boolean _canceled;

	public EditEnsembleSetWindow(Window parent)
	{
		super(parent, "Edit Ensemble Set", true);
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
		String[] headers = new String[]{"Select", "Boundary Conditions"};
		_bcTable = new RmaJTable(this, headers)
		{
			public boolean isCellEditable(int row, int col)
			{
				return col == 0;
			}
		};
		_bcTable.setRowHeight(_bcTable.getRowHeight()+5);
		_bcTable.setCheckBoxCellEditor(0);
		_bcTable.removePopuMenuFillOptions();
		_bcTable.removePopupMenuInsertAppendOnly();
		_bcTable.removePopupMenuSumOptions();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_bcTable.getScrollPane(), gbc);

		headers = new String[]{"Select", "Temperature Target Sets"};
		_tempTargetSetTable = new RmaJTable(this, headers)
		{
			public boolean isCellEditable(int row, int col)
			{
				return col == 0;
			}
		};
		_tempTargetSetTable.setRowHeight(_tempTargetSetTable.getRowHeight()+5);
		_tempTargetSetTable.setCheckBoxCellEditor(0);
		_tempTargetSetTable.removePopuMenuFillOptions();
		_tempTargetSetTable.removePopupMenuInsertAppendOnly();
		_tempTargetSetTable.removePopupMenuSumOptions();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		tablesPanel.add(_tempTargetSetTable.getScrollPane(), gbc);
	}

	private void addListeners()
	{
		_bcTable.getSelectionModel().addListSelectionListener(e->tableRowsSelected());
		_tempTargetSetTable.getSelectionModel().addListSelectionListener(e->tableRowsSelected());
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON:
						if ( validForm())
						{
							_canceled = false;
							setVisible(false);
						}
						break;
					case ButtonCmdPanel.CANCEL_BUTTON:
						_canceled = false;
						setVisible(false);
						break;
				}
			}


		});
	}
	private void tableRowsSelected()
	{
		List<BcData> bcData = getSelectedBcData();
		List<TemperatureTargetSet>ttsData = getSelectedTempTargetSets();
		int esCnt = bcData.size()*ttsData.size();
		if ( esCnt > 0 )
		{
			_infoLabel.setText("Selections will create " + esCnt + " Ensemble Sets");
		}
		else
		{
			_infoLabel.setText("");
		}
	}

	private boolean validForm()
	{
		List<BcData>opsData = getSelectedBcData();
		if ( opsData.size() == 0 )
		{
			JOptionPane.showMessageDialog(this,"Please Select one or more Boundary Conditions.", "No Boundary Conditions Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		List<TemperatureTargetSet>metData = getSelectedTempTargetSets();
		if ( metData.size() == 0 )
		{
			JOptionPane.showMessageDialog(this,"Please Select one or more Temperature Target Sets.", "No Temperature Target Set Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		return true;
	}
	public void fillForm(ForecastSimGroup simulationGroup)
	{
		List<BcData> bcData = simulationGroup.getBcData();
		List<TemperatureTargetSet> tempTargetSets = simulationGroup.getTemperatureTargetSets();

		_bcTable.deleteCells();
		Vector<Object> row;
		for (int i = 0;i < bcData.size(); i++ )
		{
			row = new Vector<>();
			row.add(Boolean.FALSE);
			row.add(bcData.get(i));
			_bcTable.appendRow(row);
		}

		_tempTargetSetTable.deleteCells();
		for (int i = 0;i < tempTargetSets.size(); i++ )
		{
			row = new Vector<>();
			row.add(Boolean.FALSE);
			row.add(tempTargetSets.get(i));
			_tempTargetSetTable.appendRow(row);
		}

	}

	public boolean isCanceled()
	{
		return _canceled;
	}
	public List<EnsembleSet> getEnsembleSets()
	{
		List<EnsembleSet>selectedEnsembleSets = new ArrayList<>();
		List<BcData>bcData = getSelectedBcData();
		List<TemperatureTargetSet>ttsData = getSelectedTempTargetSets();
		BcData bc;
		TemperatureTargetSet tts;
		for(int o = 0;o < bcData.size(); o++ )
		{
			bc = bcData.get(o);
			for(int m = 0;m < ttsData.size(); m++ )
			{
				tts = ttsData.get(m);
				EnsembleSet ensembleSet = new EnsembleSet();
				ensembleSet.setName(bc.getName()+"-"+tts.getName());
				ensembleSet.setSelectedBcData(bc);
				ensembleSet.setSelectedTemperatureTargetSets(tts);
				selectedEnsembleSets.add(ensembleSet);
			}
		}
		return selectedEnsembleSets;
	}

	private List<TemperatureTargetSet> getSelectedTempTargetSets()
	{
		int rowCnt = _tempTargetSetTable.getNumRows();
		Object obj;
		List<TemperatureTargetSet>selectedTts = new ArrayList<>();
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = _tempTargetSetTable.getValueAt(r,0);
			if( obj == Boolean.TRUE || "true".equals(obj.toString()))
			{
				selectedTts.add((TemperatureTargetSet) _tempTargetSetTable.getValueAt(r,1));
			}
		}
		return selectedTts;
	}

	private List<BcData> getSelectedBcData()
	{
		int rowCnt = _bcTable.getNumRows();
		Object obj;
		List<BcData>selectedBcData = new ArrayList<>();
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = _bcTable.getValueAt(r,0);
			if( obj == Boolean.TRUE || "true".equals(obj.toString()))
			{
				selectedBcData.add((BcData) _bcTable.getValueAt(r,1));
			}
		}
		return selectedBcData;
	}
}
