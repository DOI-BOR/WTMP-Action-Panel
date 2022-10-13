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
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import hec.gui.SelectorPanel;
import hec.io.DSSIdentifier;
import hec.lang.NamedType;

import hec2.model.DataLocation;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;


/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class BcEntryDialog extends RmaJDialog
{
	private RmaJTable _bcTable;
	private IterationBcPanel _panel;

	private SelectorPanel _selectorPanel;
	private RmaJTextField _modelDssFileFld;
	private RmaJTextField _modelDssPathFld;
	private RmaJTextField _iterationDssFileFld;
	private RmaJTextField _iterationDssPathFld;
	private JButton _browseDssBtn;
	private JButton _clearDssBtn;
	private ButtonCmdPanel _cmdPanel;
	private RmaJTextField _parameterFld;
	private boolean _fillingForm;
	private boolean _selectionChanging;

	/**
	 * @param iterationBcPanel
	 * @param bcTable
	 * @param row
	 */
	public BcEntryDialog(IterationBcPanel iterationBcPanel, RmaJTable bcTable,
			int tableRow)
	{
		super(SwingUtilities.windowForComponent(iterationBcPanel));
		_panel = iterationBcPanel;
		_bcTable = bcTable;
		buildControls();
		addListeners();
		fillForm(bcTable);
		_selectorPanel.setSelectedIndex(tableRow);
		pack();
		setSize(580, 310);
		setLocationRelativeTo(getParent());
	}

	
	/**
	 * 
	 */
	private void buildControls()
	{
		setTitle("Edit Boundary Condition");
		
		getContentPane().setLayout(new GridBagLayout());
		_selectorPanel = new SelectorPanel(SelectorPanel.ONE_LINE_LAYOUT);
		_selectorPanel.setSortingEnabled(false);
		_selectorPanel.setDescriptionPanelVisible(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.insets(5,0,5,5);
		getContentPane().add(_selectorPanel, gbc);
		
		JLabel label = new JLabel("Model DSS File:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_modelDssFileFld = new RmaJTextField();
		_modelDssFileFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_modelDssFileFld, gbc);
		
		label = new JLabel("Model DSS Path:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(label, gbc);
		
		_modelDssPathFld = new RmaJTextField();
		_modelDssPathFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_modelDssPathFld, gbc);
		
		label = new JLabel("Parameter:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_parameterFld = new RmaJTextField();
		_parameterFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_parameterFld, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		
		label = new JLabel("Iteration DSS File:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_iterationDssFileFld = new RmaJTextField();
		_iterationDssFileFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_iterationDssFileFld, gbc);
		
		label = new JLabel("Iteration DSS Path:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_iterationDssPathFld = new RmaJTextField();
		_iterationDssPathFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_iterationDssPathFld, gbc);
		
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(panel, gbc);
		
		_browseDssBtn = new JButton("Browse DSS");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_browseDssBtn , gbc);
		
		_clearDssBtn = new JButton("Clear DSS");
		_clearDssBtn.setToolTipText("Clear the Iteration DSS File and Path");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_clearDssBtn , gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		_selectorPanel.addItemListener(e->selectionChanged(e));
		_browseDssBtn.addActionListener(e->browseDssAction());
		_clearDssBtn.addActionListener(e->clearDssAction());
		_bcTable.getSelectionModel().addListSelectionListener(e->tableRowSelected());
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.CLOSE_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * @return
	 */
	private void tableRowSelected()
	{
		if ( _selectionChanging )
		{
			return;
		}
		int selectedRow = _bcTable.getSelectedRow();
		if ( selectedRow == -1 )
		{
			_selectorPanel.setSelectedIndex(-1);
			return;
		}
		_selectorPanel.setSelectedIndex(selectedRow);
	}


	/**
	 * @return
	 */
	private void browseDssAction()
	{
		_panel.browseDSSAction(this);
	}
	
	private void clearDssAction()
	{
		int opt = JOptionPane.showConfirmDialog(this, "Clear the Iteration DSS File and Path?", "Confirm", JOptionPane.YES_NO_OPTION);
		if ( opt == JOptionPane.YES_OPTION )
		{
			_panel.clearRow(_selectorPanel.getSelectedIndex());
			_iterationDssFileFld.setText("");
			_iterationDssPathFld.setText("");
		}
	}


	/**
	 * @return
	 */
	private void selectionChanged(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		if ( _fillingForm ) 
		{
			return;
		}
		int idx = _selectorPanel.getSelectedIndex();
		_selectionChanging = true;
		try
		{
			_bcTable.setSelectedIndices(idx);
		}
		finally
		{
			_selectionChanging = false;
		}
		fillForm(idx);
	}


	/**
	 * @param idx
	 */
	private void fillForm(int row)
	{
		if ( row < 0 )
		{
			clearForm();
			return;
		}
		String parameter = (String) _bcTable.getValueAt(row, IterationBcPanel.PARAMETER_COL);
		DSSIdentifier modelDssId = (DSSIdentifier) _bcTable.getValueAt(row, IterationBcPanel.MODEL_DSS_COL);
		DSSIdentifier selectedDssId = (DSSIdentifier) _bcTable.getValueAt(row, IterationBcPanel.DSSID_COL);
		_parameterFld.setText(parameter);
		_modelDssFileFld.setText(modelDssId.getFileName());
		_modelDssPathFld.setText(modelDssId.getDSSPath());
		_iterationDssFileFld.setText(selectedDssId.getFileName());
		_iterationDssPathFld.setText(selectedDssId.getDSSPath());
		setModified(false);
	}


	/**
	 * @param bcTable
	 */
	private void fillForm(RmaJTable bcTable)
	{
		_fillingForm = true;
		try
		{
			int rowCnt = bcTable.getRowCount();
			List<DataLocationNamedType> dlList = new ArrayList<>(rowCnt);
			DataLocation dl;
			for(int r = 0;r<rowCnt; r++ )
			{
				dl = (DataLocation) bcTable.getValueAt(r, IterationBcPanel.DATALOCATION_COL);
				dlList.add(new DataLocationNamedType(dl));
			}
			_selectorPanel.setSelectionList(dlList);
		}
		finally
		{
			_fillingForm = false;
		}
	}


	/**
	 * @param dssId
	 */
	public void setSelectedDssId(DSSIdentifier selectedDssId)
	{
		if ( selectedDssId == null )
		{
			_iterationDssFileFld.setText("");
			_iterationDssPathFld.setText("");
			return;
		}
		_iterationDssFileFld.setText(selectedDssId.getFileName());
		_iterationDssPathFld.setText(selectedDssId.getDSSPath());
	}

	class DataLocationNamedType extends NamedType
	{
		private DataLocation _dataLocation;

		DataLocationNamedType(DataLocation dataLocation)
		{
			super(dataLocation.getName());
			_dataLocation = dataLocation;
		}
		
		public DataLocation getDataLocaiton()
		{
			return _dataLocation;
		}
	}
}
