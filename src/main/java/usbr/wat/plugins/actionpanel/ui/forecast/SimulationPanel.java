/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import hec.util.NumericComparator;
import hec2.wat.model.WatAnalysisPeriod;

import hec2.wat.model.WatSimulation;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJIntegerField;
import rma.swing.RmaJIntegerSetField;
import rma.swing.RmaJTable;
import rma.swing.table.ColumnGroup;
import rma.swing.table.GroupableTableHeader;
import rma.swing.table.MleHeadRenderer;
import rma.swing.table.RmaCellEditor;
import rma.swing.table.RmaTableModel;
import rma.swing.table.RmaTableModelInterface;
import rma.util.IntArray;
import rma.util.IntVector;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;
import usbr.wat.plugins.actionpanel.actions.forecast.EditEnsembleSetAction;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.AbstractSimulationPanel;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;

/**
 * @author mark
 *
 */
public class SimulationPanel extends AbstractSimulationPanel
	implements UsbrPanel
{


	private static final int COMPUTED_MEMBERS_COL = 4;
	private EnabledJPanel _topPanel;
	private JLabel _apLabel;
	private JLabel _apStartLabel;
	private JLabel _apEndLabel;
	private ForecastPanel _parentPanel;
	
	private RmaJTable _simEnsembleTable;
	private JButton _editEnsembleButton;
	private ColumnGroup _columnGroup;
	private RmaJCheckBox _recomputeAllChk;
	private List<EnsembleSet> _esetsInTable = new ArrayList<>();
	private RmaJIntegerSetField _ensembleMembersFld;
	private boolean _enabledCheckBox;

	public SimulationPanel(ActionsWindow parentWindow, ForecastPanel parentPanel)
	{
		super(parentWindow);
		_parentPanel = parentPanel;
		buildControls();
		addListeners();
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		_topPanel = new EnabledJPanel(new GridBagLayout());
		buildTopPanel(_topPanel);
	}
	private void buildTopPanel(JPanel topPanel)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.5;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(topPanel, gbc);
		
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);
		
		_apLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apLabel, gbc);
		
		label = new JLabel("Start Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		topPanel.add(label, gbc);
		
		_apStartLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apStartLabel, gbc);
		
		label = new JLabel("End Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		topPanel.add(label, gbc);
		
		_apEndLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_apEndLabel, gbc);
		
	
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JSeparator(), gbc);
		
		label = new JLabel("Simulations:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth =  GridBagConstraints.REMAINDER; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(label, gbc);	
		
		String[] headers = new String[] {"Selected", "Simulation", "Map", "Report"};
		_simulationTable = new SimulationTreeTable(this)
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				return getTableToolTipText(e);
			}
			@Override
			public void setValueAt(Object value, int row, int col)
			{
				super.setValueAt(value, row, col);
				if ( col == SimulationTreeTableModel.SELECTED_COLUMN )
				{
					tableCheckBoxAction();
				}
			}
		};
		
		_simulationTable.setColumnWidths(350,150,110,110);
		
		_simulationTable.setRowHeight(_simulationTable.getRowHeight()+5);
		_simulationTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


		JButton button = _simulationTable.setButtonCellEditor(2);
		button.addActionListener(e->displaySimulationInMap());
		button.setText("Show on Map");
		button = _simulationTable.setButtonCellEditor(3);
		button.setText("View Report");
		button.addActionListener(e->displayReport());
		//_simulationTable.deleteCells();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		topPanel.add(_simulationTable.getScrollPane(), gbc);
		
		if (! Boolean.getBoolean("NoSimulationComputeState"))
		{
			JPanel legendPanel = buildLegendPanel();
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx   = 1.0;
			gbc.weighty   = 0.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.HORIZONTAL;
			gbc.insets    = RmaInsets.INSETS5505;
			topPanel.add(legendPanel, gbc);
		
		}
		

	
		headers = new String[] {"Selected\nto Run", "Boundary Conditions", "Temperature Target Set", "Target Members\nTo Run", "Target Members\nPreviously Run"};
		_simEnsembleTable = new RmaJTable(this, headers)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				if ( column == 0 )
				{
					Object obj = getValueAt(row, 3);
					if ( obj == null )
					{
						return false;
					}
					if ( getEditingRow() == row )
					{

					}
					if ( obj instanceof String )
					{
						String str = (String) obj;
						boolean hasData = str.length() > 0;
						if ( getEditingRow() == row )
						{
							return hasData || _enabledCheckBox;
						}
						return hasData;
					}
				}
				return column == 3;
			}
			public void setValueAt(Object obj, int row, int col)
			{
				super.setValueAt(obj, row, col);
				if ( col == 3)
				{
					RmaTableModel model = (RmaTableModel) getModel();
					model.fireTableRowsUpdated(row, row);
					if (obj == null )
					{
						super.setValueAt(Boolean.FALSE, row, 0);
					}
					else if ( obj instanceof String )
					{
						String str = (String) obj;
						if ( str.isEmpty() )
						{
							super.setValueAt(Boolean.FALSE, row, 0);
						}
					}
				}
			}
		};
		JMenuItem ensembleIndexingMenu = new JMenuItem("Ensemble F-Part Mapping...");
		ensembleIndexingMenu.addActionListener(e->displayEnsembleFPartIndexing());
		_simEnsembleTable.addPopupItem(ensembleIndexingMenu, 0);
		_simEnsembleTable.setRowHeight(_simEnsembleTable.getRowHeight()+5);
		MleHeadRenderer renderer = _simEnsembleTable.setMlHeaderRenderer();
		_ensembleMembersFld = setIntegerSetCellEditor(_simEnsembleTable, 3);

		setIntegerSetCellEditor(_simEnsembleTable, 4);
		_simEnsembleTable.setTableHeader(new GroupableTableHeader(_simEnsembleTable.getColumnModel()));
		TableColumnModel cm = _simEnsembleTable.getColumnModel();
		_columnGroup = new ColumnGroup(renderer, "Simulation Name");
		_columnGroup.add(cm.getColumn(0));
		_columnGroup.add(cm.getColumn(1));
		_columnGroup.add(cm.getColumn(2));
		_columnGroup.add(cm.getColumn(3));
		_columnGroup.add(cm.getColumn(4));
		_simEnsembleTable.setCheckBoxCellEditor(0);
		GroupableTableHeader header = (GroupableTableHeader)_simEnsembleTable.getTableHeader();
		header.addColumnGroup(_columnGroup);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5555;
		add(_simEnsembleTable.getScrollPane(), gbc);



		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0000;
		add(panel, gbc);

		EditEnsembleSetAction editAction = new EditEnsembleSetAction(this);
		_editEnsembleButton = new JButton(editAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(20,5,0,5);
		panel.add(_editEnsembleButton, gbc);
		
		_recomputeAllChk = new RmaJCheckBox("Recompute All");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(20,5,0,5);
		panel.add(_recomputeAllChk, gbc);

		_simActionsPanel = new SimulationActionsPanel(_parentWindow, this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		add(_simActionsPanel, gbc);
	
	}

	private void displayEnsembleFPartIndexing()
	{
		EnsembleFPartMapDlg dlg = new EnsembleFPartMapDlg(_parentWindow);
		dlg.fillForm(_parentPanel.getSimulationGroup(), _parentPanel.getSelectedSimulation());
		dlg.setVisible(true);
	}

	private RmaJIntegerSetField setIntegerSetCellEditor(RmaJTable table, int col)
	{
		TableColumnModel tcm = table.getColumnModel();
		if (col < tcm.getColumnCount() && col >= 0)
		{
			TableColumn tc = table.getColumnModel().getColumn(col);
			if (tc == null)
			{
				return null;
			}
			else
			{
				RmaJIntegerSetField df = new RmaJIntegerSetField();
				//df.setHorizontalAlignment(4);
				df.addMouseListener(table);
				RmaCellEditor dcf = new RmaCellEditor(df);
				dcf.setDisplayUnitSystem(table.getDisplayUnitSystem());
				/*
				RmaJTable.ParameterScale ps = (RmaJTable.ParameterScale)this._parameterScaleTable.get(new Integer(this.convertColumnIndexToModel(col)));
				if (ps != null) {
					dcf.setDisplayScaleFactor(ps.paramId, ps.scale);
				}
				*/


				dcf.setClickCountToStart(table.getClickCountToStart());
				tc.setCellEditor(dcf);
				table.setHorizontalAlignment(SwingConstants.RIGHT, col);
				if (table.getModel() instanceof RmaTableModelInterface)
				{
					((RmaTableModelInterface)table.getModel()).setColumnClass(col, Number.class);
				}

				return df;
			}
		}
		return null;
	}


	/**
	 * 
	 */
	private void addListeners()
	{
		_ensembleMembersFld.getDocument().addDocumentListener(new DocumentListener()
		{

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				isRowCheckable();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				isRowCheckable();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{

			}
			private void isRowCheckable()
			{
				int editingRow = _simEnsembleTable.getEditingRow();
				if ( editingRow == -1 )
				{
					return;
				}
				String txt = _ensembleMembersFld.getText();
				_enabledCheckBox =  !txt.isEmpty();
				if ( !_enabledCheckBox )
				{
					_simEnsembleTable.setValueAt(Boolean.FALSE, editingRow, 0);
				}
				else
				{
					_simEnsembleTable.repaint();
				}
			}
		});
		_simulationTable.getSelectionModel().addListSelectionListener(e->tableSelectionChanged(e));

	}

	private void tableSelectionChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting())
		{
			return;
		}
		tableSelectionChanged();
	}
	private void tableSelectionChanged()
	{
		// save ensemble table settings
		_simEnsembleTable.commitEdit(true);
		int rowCnt = _esetsInTable.size();
		if (  rowCnt > 0 )
		{
			for (int r = 0; r < rowCnt; r++ )
			{
				String members = (String) _simEnsembleTable.getValueAt(r, 3);
				_esetsInTable.get(r).setMemberSetToCompute(members);
			}
			_parentPanel.getSimulationGroup().setModified(true);
		}
		int row = _simulationTable.getSelectedRow();
		_simEnsembleTable.deleteCells();
		_esetsInTable.clear();
		if ( row == -1 )
		{
			_columnGroup.setHeaderValue("No Simulation Selected");
			_editEnsembleButton.setEnabled(false);
			_recomputeAllChk.setEnabled(false);

		}
		else
		{
			WatSimulation simulation = (WatSimulation) _simulationTable.getValueAt(row, 0);
			_columnGroup.setHeaderValue(simulation.getName());
			ForecastSimGroup simGroup = _parentPanel.getSimulationGroup();
			List<EnsembleSet> esets = simGroup.getEnsembleSetsFor(simulation);
			_editEnsembleButton.setEnabled(true);
			_recomputeAllChk.setEnabled(true);

			if (esets != null)
			{
				setEnsembleSets(esets);
			}
		}

		_simEnsembleTable.revalidate();
		_simEnsembleTable.getTableHeader().revalidate();
		_simEnsembleTable.getTableHeader().repaint();
		revalidate();
	}


	public boolean shouldRecomputeAll()
	{
		return _recomputeAllChk.isSelected();
	}

	/**
	 * @param asg
	 */
	@Override
	public void setSimulationGroup(AbstractSimulationGroup asg)
	{
		if ( asg instanceof ForecastSimGroup )
		{
			ForecastSimGroup fsg = (ForecastSimGroup) asg;
			_parentPanel.setSimulationGroup(fsg);
			WatSimulation simulation = getSelectedSimulation();
			fillSimulationTable();
			setEnsembleSets(fsg.getEnsembleSets(simulation));
			fillAnalysisWindow();
			setEnabled(true);
			if ( _simulationTable.getRowCount() > 0 )
			{
				_simulationTable.setRowSelectionInterval(0,0);
			}
		}
		else
		{
			_parentPanel.setSimulationGroup(null);
			fillSimulationTable();
			setEnsembleSets(null);
			fillAnalysisWindow();
			setEnabled(true);
		}
		tableSelectionChanged();
	}

	/**
	 * 
	 */
	private void fillAnalysisWindow()
	{
		clearApLabels();
		AbstractSimulationGroup fsg = getSimulationGroup();
		if ( fsg != null )
		{
			WatAnalysisPeriod ap = fsg.getAnalysisPeriod();
			if ( ap != null )
			{
				_apLabel.setText(ap.getName());
				_apStartLabel.setText(ap.getRunTimeWindow().getStartTime().toString());
				_apEndLabel.setText(ap.getRunTimeWindow().getEndTime().toString());
			}
		}
	}

	/**
	 * 
	 */
	private void clearApLabels()
	{
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");
		
	}

	/**
	 * 
	 */


	@Override
	public AbstractSimulationGroup getSimulationGroup()
	{
		return _parentPanel.getSimulationGroup();
	}

	public List<ResultsData> getSelectedResults()
	{
		return _simulationTable.getSelectedResults();
	}

	public void setEnsembleSets(List<EnsembleSet> ensembleSets)
	{
		_simEnsembleTable.deleteCells();
		_esetsInTable.clear();
		if ( ensembleSets == null )
		{
			return;
		}
		_esetsInTable.addAll(ensembleSets);
		Vector<Object> row;
		EnsembleSet eset;
		for(int i = 0; i < ensembleSets.size(); i++ )
		{
			eset = ensembleSets.get(i);
			row = new Vector();
			row.add(Boolean.FALSE);
			row.add(eset.getBcData());
			row.add(eset.getTemperatureTargetSet());
			row.add(eset.getMemberSetToCompute());
			String cmStr = getComputedEnsembleString(eset);

			row.add(cmStr);

			_simEnsembleTable.appendRow(row);
		}
	}

	private String getComputedEnsembleString(EnsembleSet eset)
	{
		IntVector computedMembers = eset.getComputedMembers();
		String cmStr = computedMembers.toString();
		cmStr = RMAIO.removeChar(cmStr, '[');
		cmStr = 	RMAIO.removeChar(cmStr, ']');
		return cmStr;
	}

	public List<EnsembleSet>getSelectedEnsembleSets()
	{
		_simEnsembleTable.commitEdit(true);
		List<EnsembleSet>selectedEsets = new ArrayList<>();
		if ( _esetsInTable == null )
		{
			return selectedEsets;
		}
		int rowCnt = _simEnsembleTable.getRowCount();
		Object obj;
		EnsembleSet eset;
		String members;
		for(int r = 0;r < rowCnt; r++ )
		{
			obj = _simEnsembleTable.getValueAt(r, 0);
			if ( obj == Boolean.TRUE || "true".equals(obj.toString()) )
			{
				eset = _esetsInTable.get(r);
				selectedEsets.add(eset);
				members = (String)_simEnsembleTable.getValueAt(r,3);

				eset.setMemberSetToCompute(members);
			}
		}
		return selectedEsets;
	}
	private List<Integer> getSelectedEnsembleRows()
	{
		_simEnsembleTable.commitEdit(true);
		List<Integer>selectedRows = new ArrayList<>();
		if ( _esetsInTable == null )
		{
			return selectedRows;
		}
		int rowCnt = _simEnsembleTable.getRowCount();
		Object obj;
		for(int r = 0;r < rowCnt; r++ )
		{
			obj = _simEnsembleTable.getValueAt(r, 0);
			if ( obj == Boolean.TRUE || "true".equals(obj.toString()) )
			{
				selectedRows.add(r);
			}
		}
		return selectedRows;
	}

	public void updateComputeStates()
	{
		super.updateComputeStates();
		List<Integer> selectedRows = getSelectedEnsembleRows();
		int row;
		EnsembleSet eset;
		String cmStr;
		for(int r = 0; r < selectedRows.size(); r++ )
		{
			row = selectedRows.get(r);
			eset = _esetsInTable.get(r);
			cmStr = getComputedEnsembleString(eset);
			_simEnsembleTable.setValueAt(cmStr, row, 4);
		}
	}

	/**
	 * the highlighted simulation in the table
	 * @return
	 */
	public WatSimulation getSelectedSimulation()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row == -1 )
		{
			return null;
		}
		return (WatSimulation) _simulationTable.getValueAt(row, 0);
	}

	public void addComputedMember(WatSimulation sim, EnsembleSet ensembleSet, int computedMember)
	{
		if ( sim == null || ensembleSet == null )
		{
			return;
		}
		if ( sim != getSelectedSimulation() )
		{
			return;
		}
		int rowCnt = _esetsInTable.size();
		for (int r = 0;r < rowCnt; r++ )
		{
			if ( _esetsInTable.get(r) == ensembleSet )
			{
				String computedMembers = (String) _simEnsembleTable.getValueAt(r,COMPUTED_MEMBERS_COL );
				computedMembers = setComputedMember(computedMembers, computedMember);
				setComputedMembers(r, computedMembers);
			}
		}
	}

	private void setComputedMembers(int r, String computedMembers)
	{
		EventQueue.invokeLater(()->_simEnsembleTable.setValueAt(computedMembers, r, COMPUTED_MEMBERS_COL));
	}

	private String setComputedMember(String computedMembers, int computedMember)
	{
		if ( computedMembers == null || computedMembers.isEmpty() )
		{
			return String.valueOf(computedMember);
		}
		String[] members = computedMembers.split(",");
		if ( members == null || members.length == 0  || computedMember < 0 )
		{
			return String.valueOf(computedMember);
		}
		List<String> membersList = Arrays.asList(members);
		Set<String>membersSet = new HashSet<>(membersList);
		membersSet.add(String.valueOf(computedMember));
		membersList = new ArrayList<>();
		membersList.addAll(membersSet);
		Collections.sort(membersList, new NumericComparator());
		StringBuilder builder = new StringBuilder();
		Iterator<String> iter = membersList.iterator();
		while(iter.hasNext())
		{
			builder.append(iter.next());
			if ( iter.hasNext())
			{
				builder.append(",");
			}
		}
		return builder.toString();
	}
}
