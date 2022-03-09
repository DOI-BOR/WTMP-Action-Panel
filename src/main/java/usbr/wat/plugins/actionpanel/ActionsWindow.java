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
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.rma.client.Browser;
import com.rma.client.LookAndFeel;
import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.model.Project;
import com.rma.util.PlugInLoader;

import hec.gui.NameDescriptionPanel;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import rma.swing.ColorIcon;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJList;
import rma.swing.RmaJTable;
import rma.swing.list.RmaListModel;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ActionsWindow extends RmaJDialog
{
	private static final int SELECTED_COLUMN = 0;
	private static final int SIMULATION_COLUMN = 1;
	private static final Color NOT_COMPUTED_COLOR = Color.BLUE;
	private static final Color COMPUTED_COLOR = Color.GREEN.darker();
	private static final Color COMPUTED_ERROR_COLOR = Color.RED;
	private static final Color NEEDS_TO_COMPUTE_COLOR = Color.BLACK;
	
	private ActionsPanel _actionsPanel;
	private JPanel _rightPanel;
	private JLabel _apLabel;
	private RmaJTable _simulationTable;
	private RmaJList _statusList;
	private NameDescriptionPanel _nameDescPanel;
	private JLabel _apStartLabel;
	private JLabel _apEndLabel;
	private SimulationGroup _sg;

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
		gbc.insets    = RmaInsets.INSETS5505;
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
		gbc.insets    = RmaInsets.INSETS5505;
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
		_simulationTable = new RmaJTable(this, headers)
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
		_simulationTable.setColumnWidths(150,350,110,110);
		_simulationTable.removePopupMenuSumOptions();
		_simulationTable.setRowHeight(_simulationTable.getRowHeight()+5);
		_simulationTable.setCheckBoxCellEditor(0);
		JButton button = _simulationTable.setButtonCellEditor(2);
		button.addActionListener(e->displaySimulationInMap());
		button.setText("Show on Map");
		button = _simulationTable.setButtonCellEditor(3);
		button.setText("View Report");
		button.addActionListener(e->displayReport());
		_simulationTable.deleteCells();
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
		
		_statusList = new RmaJList<>(new RmaListModel<String>(false));
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
		if ( col == SIMULATION_COLUMN )
		{
			WatSimulation sim = (WatSimulation) _simulationTable.getValueAt(row, col);
			List<ModelAlternative> modelAlts = sim.getAllModelAlternativeList();
			StringBuilder tip = new StringBuilder();
			ModelAlternative modelAlt;
			tip.append("<html>");
			for (int i = 0;i < modelAlts.size();i++)
			{
				modelAlt = modelAlts.get(i);
				if ( modelAlt != null )
				{
					tip.append(modelAlt.getProgram());
					tip.append(" : ");
					tip.append(modelAlt.getName());
					tip.append("<br>");
				}
			}
			tip.append("</html>");
			return tip.toString().trim();
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
	private void displayReport()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		WatSimulation sim = (WatSimulation) _simulationTable.getValueAt(row, SIMULATION_COLUMN);	
		
		DisplayReportAction action = new DisplayReportAction(this);
		action.displayReportAction(sim);
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
	private void displaySimulationInMap()
	{
		int row = _simulationTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		WatSimulation sim = (WatSimulation) _simulationTable.getValueAt(row, SIMULATION_COLUMN);
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
				checkRepoOutofDateStatus();
			}
		});
		
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
			_simulationTable.deleteCells();
			_sg = sg;
			if ( sg != null )
			{
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
		List<WatSimulation>selectedSims = new ArrayList<>();
		int rowCnt = _simulationTable.getRowCount();
		Object obj;
		WatSimulation sim;
		for (int r = 0;r < rowCnt; r++  )
		{
			obj = _simulationTable.getValueAt(r, SELECTED_COLUMN);
			if (obj == null )
			{
				continue;
			}
			
			if ( RMAIO.parseBoolean(obj.toString(), false))
			{
				sim = (WatSimulation) _simulationTable.getValueAt(r, SIMULATION_COLUMN);
				selectedSims.add(sim);
			}
		}
		return selectedSims;
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
		for(int r = 0;r < _simulationTable.getRowCount();r++)
		{
			sim = (WatSimulation) _simulationTable.getValueAt(r, SIMULATION_COLUMN);
			fgColor = getSimForegroundColor(sim);
			_simulationTable.setRowForeground(r, fgColor);
		}
		
	}
	
}
