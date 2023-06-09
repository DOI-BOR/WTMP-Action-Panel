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
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.rma.io.FileManagerImpl;
import com.rma.swing.tree.DefaultCheckBoxNode;

import hec.util.AnimatedWaitGlassPane;

import hec2.wat.WAT;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaImage;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.tree.CheckBoxTreeRenderer;
import rma.swing.tree.LabelIconObject;
import rma.swing.tree.NodeSelectionListener;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;
import usbr.wat.plugins.actionpanel.io.OutputType;
import usbr.wat.plugins.actionpanel.io.ReportOptions;
import usbr.wat.plugins.actionpanel.model.ReportPlugin;
import usbr.wat.plugins.actionpanel.model.ReportsManager;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;

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
	

	private RmaJTable _reportTable;
	private ButtonCmdPanel _cmdPanel;
	private AnimatedWaitGlassPane _agp;
	private Component _glassPane;
	private ActionsWindow _parent;
	private boolean _isCanceled;
	private JPanel _reportPanel;
	private JPanel _simPanel;
	private RmaJTable _simTable;
	private CheckboxTree _reportsTree;
	private DefaultMutableTreeNode _rootNode;
	private UsbrPanel _parentPanel;
	private ReportOptionsPanel _optionsPanel;

	public DisplayReportsSelector(ActionsWindow parent, UsbrPanel parentPanel)
	{
		super(parent, false);
		_parent =parent;
		_parentPanel = parentPanel;
		buildControls();
		addListeners();
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
		setTitle("Select Reports to Create");
	
		_reportsTree = new CheckboxTree();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_reportsTree), gbc);
		
		_optionsPanel = new ReportOptionsPanel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_optionsPanel, gbc);
		

		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_BUTTON|ButtonCmdPanel.CLOSE_BUTTON);
		JButton button = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		button.setText("Create Reports");
		button.setEnabled(false);
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
	protected void updateCreateReportButtonState()
	{
		_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled( !getSelectedReports().isEmpty());
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
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * @param sims
	 * 
	 */
	private void fillForm(List<SimulationReportInfo> sims)
	{
		
		_rootNode = new DefaultMutableTreeNode();
		DefaultTreeModel model = new DefaultTreeModel(_rootNode);
		
		List<ReportPlugin> plugins = ReportsManager.getPlugins();
		boolean canBeComparisionReport = sims.size()>1;
		ReportPlugin plugin;
		for(int s = 0;s < sims.size();s++ )
		{
			SimulationReportInfo sim = sims.get(s);
			LabelIconObject lio = new ReportObject(sim);
			DefaultMutableTreeNode simNode = new DefaultMutableTreeNode(lio);
			_rootNode.add(simNode);
			
			for (int r = 0;r < plugins.size();r++  ) 
			{
				plugin = plugins.get(r);
				CheckBoxNode rptNode = new CheckBoxNode(plugins.get(r), _reportsTree);
				rptNode.setSelected(shouldBeSelected(plugin));
				simNode.add(rptNode);
			}
		}
		_reportsTree.setModel(model);
		_reportsTree.expandAll(true);
		
		
		updateCreateReportButtonState();
	}


	/**
	 * @param plugin
	 * @return
	 */
	private boolean shouldBeSelected(ReportPlugin plugin)
	{
		Preferences node = WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(ReportOptionsPanel.PREF_NODE);

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
		
	}

	public Map<ReportPlugin, List<SimulationReportInfo>>getSelectedReports()
	{
		Map<ReportPlugin, List<SimulationReportInfo>>reportsMap = new HashMap<>();
		getSelectedReports(reportsMap, _rootNode);
		return reportsMap;
	}

	/**
	 * @return
	 */
	protected Map<ReportPlugin, List<SimulationReportInfo>> getSelectedReports(Map<ReportPlugin, List<SimulationReportInfo>> reports, DefaultMutableTreeNode parent)
	{
		List<ReportPlugin>reportPlugins = new ArrayList<>();
		int rowCnt = _reportsTree.getRowCount();
		Object obj;
		int cnt = parent.getChildCount();
		DefaultCheckBoxNode child;
		List<SimulationReportInfo> sris;
		ReportObject ro;
		Object childObj;
		for (int i = 0;i < cnt; i++ )
		{
			childObj = parent.getChildAt(i);
			if ( childObj instanceof DefaultCheckBoxNode && ((DefaultCheckBoxNode)childObj).isSelected())
			{
				child = (DefaultCheckBoxNode) childObj;
				obj = child.getUserObject();
				if ( obj instanceof ReportPlugin)
				{
					ReportPlugin plugin = (ReportPlugin) obj;
					sris = reports.get(plugin);
					if ( sris == null )
					{
						sris = new ArrayList<>();
						reports.put(plugin, sris);
					}
					ro = (ReportObject) parent.getUserObject();
					sris.add(ro.getSimulationReportInfo());
				}
			}
			if ( !((TreeNode)childObj).isLeaf())
			{
				getSelectedReports(reports, (DefaultMutableTreeNode)childObj);
			}
		}
		return reports;
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
			Map<ReportPlugin, List<SimulationReportInfo>> pluginReports = getSelectedReports();
			
			int maxThreads = Math.min(pluginReports.size(), Runtime.getRuntime().availableProcessors());
			if ( maxThreads < 1 )
			{
				maxThreads = 1;
			}
			ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		

			SwingWorker<Void, ReportCreator> worker = new SwingWorker<Void, ReportCreator>()
			{
				private boolean _successful = true;
				private ReportCreator _failedReport;
				@Override
				public Void doInBackground()
				{
					Set<Entry<ReportPlugin, List<SimulationReportInfo>>> info = pluginReports.entrySet();
					Iterator<Entry<ReportPlugin, List<SimulationReportInfo>>> iter = info.iterator();
					List<Future<ReportCreator>>futures = new ArrayList<>();
					while (iter.hasNext())
					{
						Entry<ReportPlugin, List<SimulationReportInfo>> next = iter.next();
						Future<ReportCreator> future = createReport(next.getKey(), next.getValue());
						if ( future != null )
						{
							futures.add(future);
						}
						
					}
					ReportCreator rv;
					for (int i = 0;i < futures.size(); i++ )
					{
						try
						{
							rv = futures.get(i).get();
							publish(rv);
						}
						catch (InterruptedException | ExecutionException e)
						{
							e.printStackTrace();
						}
					}
					

					return null;
				}
				private Future<ReportCreator> createReport( ReportPlugin reportPlugin, List<SimulationReportInfo> sris)
				{
					ReportCreator rc = new ReportCreator(reportPlugin, sris); 
					Future<ReportCreator> future = threadPool.submit(rc);
					return future;
				}
				@Override
				public void process(List<ReportCreator> chunks)
				{
					if ( chunks == null || chunks.isEmpty())
					{
						return;
					}
					ReportCreator rv;
					for (int i = 0;i < chunks.size(); i++ )
					{
						rv = chunks.get(i);
						if ( rv != null )
						{
							if ( !rv.wasReportSuccessFul())
							{
								_agp.setMessage("Failed to create report "+rv.getReportPlugin().getName());
								_successful = false;
								_failedReport = rv;
							}
						}
						else
						{
							_agp.setMessage("Failed to create report ");
							_successful = false;
							_failedReport = rv;
						}
					}
				}
				@Override
				public void done() 
				{ 
					_agp.setMessage("Reports Complete");
					try
					{
						threadPool.awaitTermination(5, TimeUnit.SECONDS);
					}
					catch (InterruptedException e)
					{
					}
					try
					{
						
					}
					finally
					{
						resetGlassPane();
					}
					if ( _successful )
					{
						int opt = JOptionPane.showOptionDialog(DisplayReportsSelector.this, "Report Created Successfully",
								"Complete",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
								null, new Object[] {"Close", "Display Reports"}, "Close");
						if ( opt == 1 )
						{
							DisplayReportAction action = new DisplayReportAction(_parentPanel);
							action.displayReportAction();
						}
					}
					else
					{
						JOptionPane.showMessageDialog(DisplayReportsSelector.this, 
						"Failed to create report for "+_failedReport._reportPlugin.getName(),
						"Report Failed", JOptionPane.INFORMATION_MESSAGE);
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
			saveSelectedReports();
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
		
		List<SimulationReportInfo> sims = _parentPanel.getSimulationReportInfos();
		
		SimulationReportInfo sri;
		String folder;
	
		Iterator<SimulationReportInfo> iter = sims.iterator();
		while ( iter.hasNext())
		{
			sri = iter.next();
			folder = sri.getSimFolder();
			if ( !FileManagerImpl.getFileManager().fileExists(folder))
			{
				final SimulationReportInfo fSri = sri;
				EventQueue.invokeLater(()->JOptionPane.showMessageDialog(_parent, "Simulation "+fSri.getSimulation().getName()
						+" has no results so no report can be created for it.","No Results", JOptionPane.INFORMATION_MESSAGE));
				iter.remove();
			}
		}
	
		if ( sims.isEmpty())
		{
			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(_parent,"There are no Simulations with results selected to create reports for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE));
			return false;
		}
		fillForm(sims);
		return true;
	}
	
	/**
	 * 
	 */
	private void saveSelectedReports()
	{
		_optionsPanel.saveSettings();
		Preferences node = _optionsPanel.getPreferencesNode();

		Map<ReportPlugin, List<SimulationReportInfo>> selectedReports = getSelectedReports();
		Set<ReportPlugin> keys = selectedReports.keySet();
		Iterator<ReportPlugin> iter = keys.iterator();
		int i = 0;
		while (iter.hasNext())
		{
			ReportPlugin plugin = iter.next();
			String pluginName =plugin.getName();
			node.put("SelectedReport"+i, pluginName);
			i++;
		}

	}

	/**
	 * @author Mark Ackerman
	 *
	 */
	public class ReportCreator
		implements Callable
	{

		private ReportPlugin _reportPlugin;
		private boolean _reportRv;
		private List<SimulationReportInfo> _sris;

		/**
		 * @param reportPlugin
		 * @param sris 
		 */
		public ReportCreator(ReportPlugin reportPlugin, List<SimulationReportInfo> sris)
		{
			super();
			_reportPlugin = reportPlugin;
			_sris = sris;
		}

		@Override
		public Object call() throws Exception
		{
			_agp.setMessage("Creating report for "+_reportPlugin.getName());

			ReportOptions options = _optionsPanel.getReportOptions();
			try
			{
				_reportRv = _reportPlugin.createReport(_sris, options);
			}
			catch ( Exception e )
			{
				Logger.getLogger(DisplayReportsSelector.class.getName()).info("Failed to run report "+_reportPlugin.getName()
						+" Error:"+e);
				e.printStackTrace();
				_reportRv = false;
			}
			return this;
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
	
	public class CheckboxTree extends JTree
	{
		public CheckboxTree()
		{
			super();
			setRowHeight(getRowHeight()+5);
			setRootVisible(false);
			setCellRenderer(new CheckBoxTreeRenderer());
			addMouseListener(new NodeSelectionListener(this));
			setToolTipText("");
		}
		
		@Override
		public String getToolTipText(MouseEvent e)
		{
			TreePath path = getPathForLocation(e.getX(), e.getY());
			if ( path == null )
			{
				return null;
			}
			Object pathObj = path.getLastPathComponent();
			if ( pathObj instanceof CheckBoxNode )
			{
				Object checkBoxObj = ((CheckBoxNode)pathObj).getUserObject();
				if ( checkBoxObj instanceof ReportPlugin )
				{
					ReportPlugin ro = (ReportPlugin) checkBoxObj;
					return ro.getDescription();
				}
			}
			return null;
		}
		public void expandAll(boolean expand) 
		{
			TreeNode root = (TreeNode) this.getModel().getRoot();
			expandAll(new TreePath(root), expand);
		}

		public void expandAll(TreePath parent, boolean expand) 
		{
			TreeNode node = (TreeNode) parent.getLastPathComponent();
			if (node.getChildCount() >= 0) 
			{
				Enumeration e = node.children();

				while (e.hasMoreElements()) 
				{
					TreeNode n = (TreeNode) e.nextElement();
					TreePath path = parent.pathByAddingChild(n);
					expandAll(path, expand);
				}
			}

			if (expand) 
			{
				expandPath(parent);
			} 
			else 
			{
				collapsePath(parent);
			}

		}
	}
	
	public class ReportObject implements LabelIconObject
	{
		private SimulationReportInfo _sri;
		private ImageIcon _icon;

		public ReportObject(SimulationReportInfo sri)
		{
			super();
			_sri = sri;
			if ( _sri.isSimulation() )
			{
				_icon = RmaImage.getImageIcon("Images/comp16x16.gif");
			}
			else
			{
				_icon = RmaImage.getImageIcon("Images/tabulate18.gif");
			}
		}
		/**
		 * @return
		 */
		public SimulationReportInfo getSimulationReportInfo()
		{
			return _sri;
		}
		@Override
		public String getLabel()
		{
			return _sri.toString();
		}

		@Override
		public Icon getIcon()
		{
			return _icon;
		}
	}
	public class CheckBoxNode extends DefaultCheckBoxNode
	{

		/**
		 * @param userObj
		 * @param tree
		 */
		public CheckBoxNode(Object userObj, JTree tree)
		{
			super(userObj, tree);
		}

		@Override
		public void setSelected(boolean selected)
		{
			super.setSelected(selected);
			updateCreateReportButtonState();
		}
		
		
	}
}
