/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import hec.util.AnimatedWaitGlassPane;

import hec2.wat.WAT;
import hec2.wat.model.WatSimulation;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.ReportPlugin;
import usbr.wat.plugins.actionpanel.model.ReportsManager;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class DisplayReportsSelector extends RmaJDialog
{
	

	private static final int SELECTED_COL = 0;
	private static final int REPORT_COL = 1;
	private static final int REPORT_DESC_COL = 2;
	
	private static final String PREF_NODE = "usbrReports";
	
	private RmaJTable _reportTable;
	private ButtonCmdPanel _cmdPanel;
	private AnimatedWaitGlassPane _agp;
	private Component _glassPane;
	private ActionsWindow _parent;
	private boolean _isCanceled;

	public DisplayReportsSelector(ActionsWindow parent)
	{
		super(parent, true);
		_parent =parent;
		buildControls();
		addListeners();
		fillForm();
		pack();
		setSize(500,500);
		setLocationRelativeTo(getParent());
		
	}

	

	/**
	 * 
	 */
	protected void buildControls()
	{
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Select Reports");
		
		((JComponent)getContentPane()).setBorder(BorderFactory.createTitledBorder("Available Reports"));
	
		String[] headers = new String[] {"Select", "Report", "Description"};
		_reportTable = new RmaJTable(this, headers);
		_reportTable.setCheckBoxCellEditor(0);
		_reportTable.setRowHeight(_reportTable.getRowHeight()+5);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_reportTable.getScrollPane(), gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_BUTTON|ButtonCmdPanel.CLOSE_BUTTON);
		JButton button = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		button.setText("Create Reports");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_cmdPanel, gbc);
		
		
	}

	/**
	 * 
	 */
	private void addListeners()
	{
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				_isCanceled = false;
				setVisible(false);
			}
		});
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						_isCanceled = false;
						createReports();
						break;
					case ButtonCmdPanel.CLOSE_BUTTON :
						_isCanceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * 
	 */
	private void fillForm()
	{
		
		List<ReportPlugin> plugins = ReportsManager.getPlugins();
		SimulationGroup simGroup = _parent.getSimulationGroup();
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		boolean canBeComparisionReport = sims.size()>1;
		
		List<ReportPlugin>reportsToUse = new ArrayList<>();
		ReportPlugin reportPlugin;
		for (int i = 0; i < plugins.size(); i++ )
		{
			reportPlugin = plugins.get(i);
			if ( reportPlugin.isComparisonReport() )
			{
				if ( canBeComparisionReport )
				{
					reportsToUse.add(reportPlugin);
				}
			}
			else
			{
				reportsToUse.add(reportPlugin);
			}
		}
		fillTable(reportsToUse);
	}
	/**
	 * @param reportsToUse
	 */
	private void fillTable(List<ReportPlugin> reportsToUse)
	{
		_reportTable.deleteCells();
		Vector row;
		ReportPlugin plugin;
		for(int i = 0;i < reportsToUse.size();i++ )
		{
			row = new Vector();
			plugin = reportsToUse.get(i);
			row.add(shouldBeSelected(plugin));
			row.add(plugin);
			row.add(plugin.getDescription());
			_reportTable.appendRow(row);
		}
	}



	/**
	 * @param plugin
	 * @return
	 */
	private boolean shouldBeSelected(ReportPlugin plugin)
	{
		Preferences node = WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(PREF_NODE);
		int idx = 0;
		String selectedReportName;
		String pluginName = plugin.getName();
		while (true )
		{
			selectedReportName = node.get("SelectedReport"+idx, null);
			if (pluginName.equalsIgnoreCase(selectedReportName) )
			{
				return true;
			}
			else if ( selectedReportName == null )
			{
				return false;
			}
			idx++;
		}
		/*
		String[] kidNodeNames;
		try
		{
			kidNodeNames = node.childrenNames();
		}
		catch (BackingStoreException e)
		{
			Logger.getLogger(DisplayReportsSelector.class.getName()).info("Failed to get list of selected reports " + e);
			return false;
		}
		if ( kidNodeNames != null )
		{
			String kidName;
			for (int i = 0;i < kidNodeNames.length; i++ )
			{
				kidName = node.get(kidNodeNames[i], "");
				if ( pluginName.equalsIgnoreCase(kidName))
				{
					return true;
				}
				
			}
		}
		return false;
		*/
	}



	/**
	 * @return
	 */
	protected List<ReportPlugin> getSelectedReports()
	{
		List<ReportPlugin>reportPlugins = new ArrayList<>();
		int rowCnt = _reportTable.getRowCount();
		Object obj;
		for (int r = 0;r < rowCnt; r++)
		{
			obj = _reportTable.getValueAt(r, SELECTED_COL);
			if ( obj != null )
			{
				if  ( RMAIO.parseBoolean(obj.toString(), false))
				{
					reportPlugins.add((ReportPlugin) _reportTable.getValueAt(r, REPORT_COL));
				}
			}
		}
		return reportPlugins;
	}
	/**
	 * 
	 */
	protected void createReports()
	{
		_agp = new AnimatedWaitGlassPane();
		_agp.setTransparency(0.8f);
		setGlassPane("Creating Reports...");

		try
		{
			List<ReportPlugin> plugins = ReportsManager.getPlugins();
			List<ReportPlugin> pluginReports = getSelectedReports();
			int maxThreads = Math.min(pluginReports.size(), Runtime.getRuntime().availableProcessors());
			if ( maxThreads < 1 )
			{
				maxThreads = 1;
			}
			ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		

			SwingWorker<Void, Future<ReportCreator>> worker = new SwingWorker<Void, Future<ReportCreator>>()
			{
				@Override
				public Void doInBackground()
				{
					
					for (int i = 0; i< pluginReports.size();i++ )
					{
						Future<ReportCreator> future = createReport(pluginReports.get(i));
						this.publish(future);
					}

					return null;
				}
				private Future<ReportCreator> createReport( ReportPlugin reportPlugin)
				{
					ReportCreator rc = new ReportCreator(reportPlugin); 
					Future< ? > future = threadPool.submit(rc);
					return (Future<ReportCreator>) future;
				}
				@Override
				public void process(List<Future<ReportCreator>> chunks)
				{
					if ( chunks == null || chunks.isEmpty())
					{
						return;
					}
					for (int i = 0;i < chunks.size(); i++ )
					{
						ReportCreator rc;
						try
						{
							rc = chunks.get(i).get();
							if ( rc != null )
							{
								if ( !rc.wasReportSuccessFul())
								{
									_agp.setMessage("Failed to create report "+rc.getReportPlugin()); 
								}
							}
							else
							{
								_agp.setMessage("Failed to create report ");
							}
						}
						catch (InterruptedException | ExecutionException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
				@Override
				public void done() 
				{ 
					_agp.setMessage("Reports Complete");
					try
					{
						threadPool.awaitTermination(30, TimeUnit.SECONDS);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
					}
					try
					{
						
					}
					finally
					{
						resetGlassPane();
					}
				}
			};
			worker.execute();

		}
		catch (Exception e )
		{
			Logger.getLogger(DisplayReportsSelector.class.getName()).warning("Exception running reports " + e);
			resetGlassPane();
		}
		finally
		{


		}
	}
	

	public void setGlassPane(String msg)
	{
		_glassPane = getGlassPane();
		_agp = new AnimatedWaitGlassPane();
		_agp.setColor(Color.BLACK);
		setGlassPane(_agp);
		_agp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_agp.setMessage(msg);
		_agp.setActive(true);
		_agp.setVisible(true);

	}

	public void resetGlassPane()
	{
		if(_agp != null && _glassPane != null)
		{
			_agp.setCursor(Cursor.getDefaultCursor());
			_agp.setActive(false);
			_agp.setVisible(false);
			setGlassPane(_glassPane);
			_agp = null;
			_glassPane = null;
		}
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		if ( visible )
		{
			if ( !checkSims())
			{
				return;
			}
		}
		super.setVisible(visible);
		if ( !visible)
		{
			if ( !_isCanceled)
			{
				saveSelectedReports();
			}
		}
	}
	private boolean checkSims()
	{
		if ( _parent.getSimulationGroup() == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
			
		}
		
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		if ( sims.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to create reports for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 */
	private void saveSelectedReports()
	{
		Preferences node = WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(PREF_NODE);
		try
		{
			node.clear();
		}
		catch (BackingStoreException e)
		{
			Logger.getLogger(DisplayReportsSelector.class.getName()).info("Failed to clear node "+node.absolutePath()+" Error:"+e);
		}
		List<ReportPlugin> selectedReports = getSelectedReports();
		for (int i = 0;i < selectedReports.size();i++ )
		{
			String pluginName =selectedReports.get(i).getName();
			node.put("SelectedReport"+i, pluginName);
		}
	}

	/**
	 * @author Mark Ackerman
	 *
	 */
	public class ReportCreator
		implements Runnable
	{

		private ReportPlugin _reportPlugin;
		private boolean _reportRv;

		/**
		 * @param reportPlugin
		 */
		public ReportCreator(ReportPlugin reportPlugin)
		{
			super();
			_reportPlugin = reportPlugin;
		}

		@Override
		public void run()
		{
			_agp.setMessage("Creating report for "+_reportPlugin.getName());
			try
			{
				_reportRv = _reportPlugin.createReport();
			}
			catch ( Exception e )
			{
				Logger.getLogger(DisplayReportsSelector.class.getName()).info("Failed to run report "+_reportPlugin.getName()
						+" Error:"+e);
				_reportRv = false;
			}
		}

		public boolean wasReportSuccessFul()
		{
			return _reportRv;
		}

		/**
		 * @return
		 */
		public ReportPlugin getReportPlugin()
		{
			return _reportPlugin;
		}

	}
}
