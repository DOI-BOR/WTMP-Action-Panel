/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.rma.client.Browser;
import com.rma.client.LookAndFeel;
import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import com.rma.util.PlugInLoader;

import hec.gui.NameDescriptionPanel;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import rma.swing.ColorIcon;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJList;
import rma.swing.list.RmaListModel;
import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.tree.ResultsTreeTableNode;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ActionsWindow extends RmaJDialog
{
	private static final Color NOT_COMPUTED_COLOR = Color.BLUE;
	private static final Color COMPUTED_COLOR = Color.GREEN.darker();
	private static final Color COMPUTED_ERROR_COLOR = Color.RED;
	private static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;
	
	private ActionsPanel _actionsPanel;
	private JPanel _rightPanel;
	private JLabel _apLabel;
	private SimulationTreeTable _simulationTable;
	private RmaJList _statusList;
	private NameDescriptionPanel _nameDescPanel;
	private JLabel _apStartLabel;
	private JLabel _apEndLabel;
	private SimulationGroup _sg;
	private SimulationActionsPanel _simActionsPanel;

	public ActionsWindow(Frame parent)
	{
		super(parent);
		setSystemClosable(false);
		buildControls();
		addListeners();
		loadPlugins();
		pack();
		setSize(950, 550);
		setLocationRelativeTo(Browser.getBrowserFrame());
	}

	
	/**
	 * 
	 */
	private void buildControls()
	{
		setTitle("Model Calibration-Validation Action");
		getContentPane().setLayout(new GridBagLayout());
		
		_actionsPanel = new ActionsPanel(this);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.VERTICAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_actionsPanel, gbc);
		
		_rightPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = GridBagConstraints.REMAINDER;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_rightPanel, gbc);
	
		buildRightPanel();
		
	}

	

	
	/**
	 * 
	 */
	private void buildRightPanel()
	{
		_nameDescPanel = new NameDescriptionPanel();
		_nameDescPanel.setPanelEditable(false);
		_nameDescPanel.setNameLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5005;
		_rightPanel.add(_nameDescPanel, gbc);
		
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(label, gbc);
		
		_apLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apLabel, gbc);
		
		label = new JLabel("Start Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		_rightPanel.add(label, gbc);
		
		_apStartLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apStartLabel, gbc);
		
		label = new JLabel("End Time:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.insets(5,15,0,5);
		_rightPanel.add(label, gbc);
		
		_apEndLabel = new JLabel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_apEndLabel, gbc);
		
	
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		label = new JLabel("Simulations:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth =  GridBagConstraints.REMAINDER; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(label, gbc);	
		
		String[] headers = new String[] {"Selected", "Simulation", "Map", "Report"};
		_simulationTable = new SimulationTreeTable()
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
		/*
		{
			@Override
			public Object getValueAt(int row, int col)
			{
				if ( col == 2 )
				{
					return "Show on Map";
				}
				else if ( col == 3 )
				{
					return "View Report";
				}
				else
				{
					return super.getValueAt(row, col);
				}
			}
			@Override
			
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return col != 1;
			}
			@Override
			public String getToolTipText(MouseEvent e)
			{
				return getTableToolTipText(e);
			}
		};
		*/
		
		
		_simulationTable.setColumnWidths(350,150,110,110);
		
		_simulationTable.setRowHeight(_simulationTable.getRowHeight()+5);
		
		
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
		_rightPanel.add(_simulationTable.getScrollPane(), gbc);
		
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
			_rightPanel.add(legendPanel, gbc);
		
		}
		
		_simActionsPanel = new SimulationActionsPanel(this);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		_rightPanel.add(_simActionsPanel, gbc);
		
		_statusList = new RmaJList<>(new RmaListModel<>(false));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5555;
		_rightPanel.add(new JScrollPane(_statusList), gbc);
	}
	/**
	 * @return
	 */
	private void tableCheckBoxAction()
	{
		EventQueue.invokeLater(()->_simActionsPanel.updateActions());
	}


	/**
	 * @param e 
	 * @return
	 */
	public void showInProjectTreeAction()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row < 0 )
		{
			return;
		}
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);
		if ( obj instanceof ResultsData )
		{
			obj = ((ResultsData)obj).getSimulation();
		}
		if ( obj instanceof WatSimulation )
		{
			MutableTreeNode simNode = Browser.getBrowserFrame().getProjectTree().getNodeForManager((WatSimulation)obj);
			if ( simNode != null )
			{
				Browser.getBrowserFrame().getProjectTree().setSelectedNode(simNode);
			}
		}
	}


	/**
	 * @param e
	 * @return
	 */
	protected String getTableToolTipText(MouseEvent e)
	{
		Point pt = e.getPoint();
		int row = _simulationTable.rowAtPoint(pt);
		int col = _simulationTable.columnAtPoint(pt);
		if ( row == -1 || col == -1)
		{
			return null;
		}
		if ( col == SimulationTreeTableModel.SIMULATION_COLUMN )
		{
			TreePath treePath = _simulationTable.getPathForRow(row);
			if ( treePath != null )
			{
				Object lastComp = treePath.getLastPathComponent();
				if ( lastComp instanceof SimulationTreeTableNode )
				{
					SimulationTreeTableNode simNode = (SimulationTreeTableNode) lastComp;
					return simNode.getToolTipText();
				}
				else if ( lastComp instanceof ResultsTreeTableNode )
				{
					ResultsTreeTableNode resultsNode = (ResultsTreeTableNode) lastComp;
					
					return resultsNode.getToolTipText();
				}
			}
			
		}
		return null;
	}

	/**
	 * @return
	 */
	private JPanel buildLegendPanel()
	{
		JPanel legendPanel = new JPanel(new GridBagLayout());
		legendPanel.setBorder(BorderFactory.createTitledBorder(""));
		
		ColorIcon icon = new ColorIcon(NOT_COMPUTED_COLOR);
		JLabel label = new JLabel("Not Computed");
		label.setIcon(icon);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);
		
		icon = new ColorIcon(NEEDS_TO_COMPUTE_COLOR);
		label = new JLabel("Out of Date");
		label.setIcon(icon);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);
		
		icon = new ColorIcon(COMPUTED_COLOR);
		label = new JLabel("Computed");
		label.setIcon(icon);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);
		
		icon = new ColorIcon(COMPUTED_ERROR_COLOR);
		label = new JLabel("Compute Error");
		label.setIcon(icon);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;
		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		legendPanel.add(label, gbc);
		
		
		
		return legendPanel;
	}

	
	/**
	 * 
	 */
	private void loadPlugins()
	{
		PlugInLoader.loadPlugIns("ReportPlugin");
	}

	/**
	 * @return
	 */
	public void displayReport()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);	
		if ( obj instanceof WatSimulation )
		{
			WatSimulation sim = (WatSimulation) obj;
			displayReport(sim.getSimulationDirectory());
		}
		else if ( obj instanceof ResultsData )
		{
			ResultsData rd = (ResultsData)obj;
			displayReport(rd.getFolder());
		}
	}
	public void displayReport(String simulationDirectory )
	{
		DisplayReportAction action = new DisplayReportAction(this);
		action.displayReportAction(simulationDirectory);
	}

	/**
	 * @param rptFile
	 */
	public  void displayFile(String rptFile)
	{
		if ( Desktop.isDesktopSupported())
		{
			File f = new File(rptFile);
			if ( f.exists())
			{
				try
				{
					Desktop.getDesktop().open(f);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "<html>Error displaying the report at " 
						+ rptFile +"<br> Error:"+e.getMessage(), "Error", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			else
			{
				JOptionPane.showMessageDialog(this, "The report doesn't exist.  Please create the report first",
						"No Report", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * @return
	 */
	public void displaySimulationInMap()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		WatSimulation sim = (WatSimulation) _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);
		displaySimulationInMap(sim);
	}
	public void displaySimulationInMap(WatSimulation sim)
	{
		if ( sim != null )
		{
			Browser.getBrowserFrame().displayManager(sim);
		}
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		Project.addStaticProjectListener(new ProjectAdapter()
		{
			@Override
			public void projectOpened(ProjectEvent e )
			{
				clearForm();
				checkRepoOutofDateStatus();
			}
		});
		
	}
	@Override
	public void clearForm()
	{
		super.clearForm();
		_apLabel.setText("");
		_apStartLabel.setText("");
		_apEndLabel.setText("");
		_simulationTable.setTreeTableModel(new SimulationTreeTableModel(null));
		//_simulationTable.deleteCells();
	}
	
	/**
	 * 
	 */
	protected void checkRepoOutofDateStatus()
	{
		EventQueue.invokeLater(()->GitRepoUtils.checkRepoOutofDateStatus(Project.getCurrentProject().getProjectDirectory()));
	}


	public static void main(String[] args)
	{
		LookAndFeel.setLookAndFeel();
		new ActionsWindow(new JFrame()).setVisible(true);
	}

	/**
	 * @param sg
	 */
	public void setSimulationGroup(SimulationGroup sg)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			clearForm();
			//_simulationTable.deleteCells();
			_sg = sg;
			if ( sg != null )
			{
				SimulationTreeTableModel newModel = new SimulationTreeTableModel(sg);
				_simulationTable.setTreeTableModel(newModel);
				_simulationTable.clearColors();
				int rowCnt = _simulationTable.getRowCount();
				for (int r = 0; r < rowCnt; r++ )
				{
					Object val = _simulationTable.getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);
					if ( val instanceof WatSimulation )
					{
						WatSimulation sim = (WatSimulation) val;
						Color color = getSimForegroundColor(sim);
						_simulationTable.setRowForeground(r, color);
					}
				}

				_nameDescPanel.setName(sg.getName());
				_nameDescPanel.setDescription(sg.getDescription());
				WatAnalysisPeriod ap = sg.getAnalysisPeriod();
				String apName = "";
				String apStart = "";
				String apEnd = "";
				if ( ap!= null )
				{
					apName = ap.getName();
					apStart = ap.getRunTimeWindow().getStartTime().toString();
					apEnd = ap.getRunTimeWindow().getEndTime().toString();
				}
				_apLabel.setText(apName);
				_apStartLabel.setText(apStart);
				_apEndLabel.setText(apEnd);
				/*
				List<WatSimulation> sims= sg.getSimulations();
				Vector row;
				WatSimulation sim;
				Color bgColor;
				for (int i = 0;i < sims.size(); i++ )
				{
					row = new Vector();
					sim = sims.get(i);
					row.add(Boolean.FALSE);
					row.add(sim);
					row.add("Show on Map");
					row.add("View Report");
					_simulationTable.appendRow(row);
					if (! Boolean.getBoolean("NoSimulationComputeState"))
					{
						Color color = getSimForegroundColor(sim);
						_simulationTable.setRowForeground(_simulationTable.getRowCount()-1, color);
					}
				}
				*/
			}
			else
			{
				_apLabel.setText("");
				_apStartLabel.setText("");
				_apEndLabel.setText("");	
			}
			_actionsPanel.setSimulationGroup(sg);
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	/**
	 * @param sim
	 * @return
	 */
	private Color getSimForegroundColor(WatSimulation sim)
	{
		if ( !sim.isComputable())
		{
			return NOT_COMPUTED_COLOR;
		}
		else if ( sim.hasComputeError() )
		{
			return COMPUTED_ERROR_COLOR;
		}
		else if( sim.hasComputed() && sim.needToCompute())
		{
			return NEEDS_TO_COMPUTE_COLOR;
		}
		else if ( sim.isComputable() && sim.hasComputed())
		{
			return COMPUTED_COLOR;
		}
		else 
		{
			return NOT_COMPUTED_COLOR;
		}
	}

	public void addMessage(String message)
	{
		if ( message == null )
		{
			((RmaListModel)_statusList.getModel()).addElement("");
		}
		else
		{
			((RmaListModel)_statusList.getModel()).addElement(message);
		}
	}

	/**
	 * @return
	 */
	public List<WatSimulation> getSelectedSimulations()
	{
		return _simulationTable.getSelectedSimulations();
		
	}

	/**
	 * @return
	 */
	public SimulationGroup getSimulationGroup()
	{
		return _sg;
	}

	/**
	 * @return
	 */
	public WatAnalysisPeriod getAnalysisPeriod()
	{
		if ( _sg != null )
		{
			return _sg.getAnalysisPeriod();
		}
		return null;
	}

	/**
	 * 
	 */
	public void updateComputeStates()
	{
		_simulationTable.clearColors();
		if ( Boolean.getBoolean("NoSimulationComputeState"))
		{
			return;
		}
		WatSimulation sim;
		Color fgColor ;
		Object val;
		_simulationTable.clearColors();
		for(int r = 0;r < _simulationTable.getRowCount();r++)
		{
			val = _simulationTable.getValueAt(r, SimulationTreeTableModel.SIMULATION_COLUMN);
			if ( val instanceof WatSimulation )
			{
				sim = (WatSimulation) val;
				fgColor = getSimForegroundColor(sim);
				_simulationTable.setRowForeground(r, fgColor);
			}
		}
		_simulationTable.repaint();
		
	}


	/**
	 * @return
	 */
	public SimulationTreeTable getSimulationTreeTable()
	{
		return _simulationTable;
	}


	/**
	 * @return 
	 * @return
	 */
	public List<ResultsData> getSelectedResults()
	{
		return _simulationTable.getSelectedResults();
	}


	/**
	 * get the info needed from the selected rows in the table to generate a report
	 * @return
	 */
	public List<SimulationReportInfo> getSimulationReportInfos()
	{
		List<SimulationReportInfo>simInfos = new ArrayList<>();
		List<WatSimulation> selectedSims = getSelectedSimulations();
		List<ResultsData> selectedResults = getSelectedResults();
		SimulationReportInfo simInfo;
		WatSimulation sim;
		ResultsData results;
		for (int i = 0;i < selectedSims.size(); i++ )
		{
			sim = selectedSims.get(i);
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(sim);
			simInfo.setSimDssFile(sim.getSimulationDssFile());
			simInfo.setSimFolder(sim.getSimulationDirectory());
			simInfo.setName(sim.getName());
			simInfo.setLastComputedDate(new Date(sim.getLastComputedDate()).toString());
			
			simInfos.add(simInfo);
		}
		for (int i = 0;i < selectedResults.size(); i++ )
		{
			results = selectedResults.get(i);
			simInfo = new SimulationReportInfo();
			simInfo.setSimulation(results.getSimulation());
			simInfo.setSimDssFile(findSimulationDssFile(results.getFolder(),results.getSimulation().getSimulationDssFile()));
			simInfo.setSimFolder(results.getFolder());
			String name = results.getSimulation().getName().concat(" - ").concat(results.getName());
			simInfo.setName(name);
			simInfo.setLastComputedDate(new Date(results.getLastComputedTime()).toString());
			
			simInfos.add(simInfo);	
		}
		
		
		
		
		return simInfos;
	}


	/**
	 * @param folder
	 * @param simulationDssFile
	 * @return
	 */
	private String findSimulationDssFile(String folder,
			String simulationDssFile)
	{
		String lookForDssFile = RMAIO.getFileFromPath(simulationDssFile);
		RmaFile folderFile = FileManagerImpl.getFileManager().getFile(folder);
		RMAFilenameFilter filter = new RMAFilenameFilter("dss");
		filter.setAcceptDirectories(false);
		File[] dssFiles = folderFile.listFiles(filter);
		if ( dssFiles != null )
		{
			for (int i = 0;i < dssFiles.length;)
			{
				String name = dssFiles[i].getName();
				if ( name.equalsIgnoreCase(lookForDssFile))
				{
					return dssFiles[i].getAbsolutePath();
				}
			}
		}
		return "";
	}
	
}
