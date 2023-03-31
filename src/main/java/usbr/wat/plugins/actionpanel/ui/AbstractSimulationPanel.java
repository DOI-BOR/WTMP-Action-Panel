/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.rma.client.Browser;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;

import hec2.wat.WAT;
import hec2.wat.model.WatSimulation;

import rma.swing.ColorIcon;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.SimulationActionsPanel;
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.tree.ResultsTreeTableNode;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableModel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractSimulationPanel extends EnabledJPanel
	implements UsbrPanel
{
	protected static final Color NOT_COMPUTED_COLOR = Color.BLUE;
	protected static final Color COMPUTED_COLOR = Color.GREEN.darker();
	protected static final Color COMPUTED_ERROR_COLOR = Color.RED;
	protected static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;
	
	protected SimulationActionsPanel _simActionsPanel;
	protected SimulationTreeTable _simulationTable;
	protected ActionsWindow _parentWindow;
	
	public AbstractSimulationPanel(ActionsWindow parent)
	{
		super(new GridBagLayout());
		_parentWindow = parent;
	}
	
	protected JPanel buildLegendPanel()
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
	protected void tableCheckBoxAction()
	{
		EventQueue.invokeLater(()->_simActionsPanel.updateActions());
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
					return simNode.getToolTipText(_parentWindow.getSimulationGroup());
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
	@Override
	public void editSimulationMetaData()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row < 0 )
		{
			return;
		}
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);
		if ( obj instanceof WatSimulation )
		{
			MetaDataEditor editor = new MetaDataEditor(_parentWindow);
			editor.fillForm((WatSimulation)obj);
			editor.setVisible(true);
		}
	}

	/**
	 * @return
	 */
	@Override
	public void displayComputeLog()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row < 0 )
		{
			return;
		}
		Object obj = _simulationTable.getValueAt(row, SimulationTreeTableModel.SIMULATION_COLUMN);
		if ( obj instanceof WatSimulation )
		{
			WatSimulation sim = (WatSimulation) obj;
			String logFile = sim.getLogFile();
			if ( FileManagerImpl.getFileManager().fileExists(logFile))
			{
				RmaFile f = FileManagerImpl.getFileManager().getFile(logFile);
				WAT.getWatFrame().openComputeLog(f);	
			}
		}
	}

	/**
	 * @param e 
	 * @return
	 */
	@Override
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
	@Override
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

	@Override
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
	 * @return
	 */
	public abstract AbstractSimulationGroup getSimulationGroup();
	@Override
	public void fillSimulationTable()
	{
		setSimulationTable(getSimulationGroup());
	}
	public void setSimulationTable(AbstractSimulationGroup sg)
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
		_simulationTable.revalidate();
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
	@Override
	public List<SimulationReportInfo> getSimulationReportInfos()
	{
		// TODO Auto-generated method stub
		System.out.println("getSimulationReportInfos TODO implement me");
		return null;
	}

	@Override
	public  void displayFile(String rptFile)	
	{
		_parentWindow.displayFile(rptFile);
	}

	/**
	 * @return
	 */
	@Override
	public SimulationTreeTable getSimulationTreeTable()
	{
		return _simulationTable;
	}


	@Override
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
	 * @param folder
	 * @param simulationDssFile
	 * @return
	 */
	protected String findSimulationDssFile(String folder, String simulationDssFile)
	{
		String lookForDssFile = RMAIO.getFileFromPath(simulationDssFile);
		RmaFile folderFile = FileManagerImpl.getFileManager().getFile(folder);
		RMAFilenameFilter filter = new RMAFilenameFilter("dss");
		filter.setAcceptDirectories(false);
		File[] dssFiles = folderFile.listFiles(filter);
		if ( dssFiles != null )
		{
			for (int i = 0;i < dssFiles.length;i++)
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
	/**
	 * @return
	 */
	public List<WatSimulation> getSelectedSimulations()
	{
		return _simulationTable.getSelectedSimulations();
		
	}
	
	public abstract void setSimulationGroup(AbstractSimulationGroup simGroup);
}
