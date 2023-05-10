/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import hec.util.NumericComparator;
import hec2.wat.model.WatSimulation;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

public class EnsembleFPartMapDlg extends RmaJDialog
{
	private JLabel _simulationLabel;
	private RmaJTable _ensembleTable;
	private ButtonCmdPanel _cmdPanel;

	public EnsembleFPartMapDlg(Window parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
	}

	private void buildControls()
	{
		setTitle("Ensemble DSS F-Parts");

		getContentPane().setLayout(new GridBagLayout());

		JLabel label = new JLabel("Simulation:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; //GridBagConstraints.REMAINDER
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		_simulationLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simulationLabel, gbc);


		String[] headers = new String[]{"Ensemble Set", "Collection Start", "Collection End"};
		_ensembleTable = new RmaJTable(this, headers)
		{
			public boolean isCellEditable(int row, int col)
			{
				return false;
			}
		};
		_ensembleTable.setIntegerCellEditor(1);
		_ensembleTable.setIntegerCellEditor(2);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_ensembleTable.getScrollPane(), gbc);

		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
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

	private void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.CLOSE_BUTTON:
						setVisible(false);
						break;
				}
			}
		});
	}

	public void fillForm(ForecastSimGroup simGroup, WatSimulation simulation)
	{
		_ensembleTable.deleteCells();
		Map<String, int[]> esetIndexing = simGroup.getSimulationEnsembleSetIndexing(simulation);
		if ( esetIndexing != null )
		{
			Vector row;
			Set<Map.Entry<String, int[]>> entrySet = esetIndexing.entrySet();
			Iterator<Map.Entry<String, int[]>> iter = entrySet.iterator();
			while(iter.hasNext())
			{
				Map.Entry<String, int[]> entry = iter.next();
				String esetName = entry.getKey();
				int[] fpartIndexs = entry.getValue();
				row = new Vector();
				row.add(esetName);
				row.add(fpartIndexs[0]);
				row.add(fpartIndexs[1]);
				_ensembleTable.appendRow(row);
			}
		}
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(_ensembleTable.getModel());
		sorter.setComparator(1, new NumericComparator());
		sorter.setComparator(2, new NumericComparator());
		_ensembleTable.setRowSorter(sorter);
	}
}
