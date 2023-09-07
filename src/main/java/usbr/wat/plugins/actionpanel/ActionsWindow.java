/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import com.rma.client.Browser;
import com.rma.client.LookAndFeel;
import com.rma.event.ProjectAdapter;
import com.rma.event.ProjectEvent;
import com.rma.event.ProjectManagerListener;
import com.rma.factories.ProjectNodeFactory;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import com.rma.util.PlugInLoader;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.MissingManagersChecker;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.ActionsProjectTab;
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;
import usbr.wat.plugins.actionpanel.ui.SimulationGroupNode;
import usbr.wat.plugins.actionpanel.ui.forecast.ForecastPanel;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ActionsWindow extends RmaJDialog
{
	static 
	{
		ProjectNodeFactory.addObjectToNodeMapping(SimulationGroup.class, SimulationGroupNode.class);
		System.setProperty("UseSimNameInRunsFolder", "true");
		System.setProperty("SimNode.AllowSimsToExceedAPs", "true");
	}
	

	private JTabbedPane _tabbedPane;
	
	private SimulationGroup _sg;
	private ProjectSimulationListener _projectSimulationListener;
	private ActionsProjectTab _actionsProjTab;
	private ProjectSimulationGroupListener _projectSimulationGroupListener;

	private CalibrationPanel _calibrationPanel;

	private ForecastPanel _forecastPanel;

	public ActionsWindow(Frame parent)
	{
		super(parent);
		setSystemClosable(false);
		buildControls();
		addListeners();
		loadPlugins();
		pack();
		setSize(1000, 700);
		setLocationRelativeTo(Browser.getBrowserFrame());
		
		addTabToProjectPane();
	}

	
	
	/**
	 * 
	 */
	private void buildControls()
	{
		setTitle("WTMP Actions Window");
		getContentPane().setLayout(new GridBagLayout());
	
		_tabbedPane = new JTabbedPane ();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_tabbedPane, gbc);

		_calibrationPanel = new CalibrationPanel(this);
		_tabbedPane.addTab("Prescribed Conditions", _calibrationPanel);
		_forecastPanel = new ForecastPanel(this);
		_tabbedPane.addTab("Forecast Conditions", _forecastPanel);

	}

	public CalibrationPanel getCalibrationPanel()
	{
		return _calibrationPanel;
	}

	public ForecastPanel getForecastPanel()
	{
		return _forecastPanel;
	}	
	
	
	/**
	 * 
	 */
	private void addTabToProjectPane()
	{
		_actionsProjTab = new ActionsProjectTab();
		Browser.getBrowserFrame().getTabbedPane().insertTab("WTMP", null, _actionsProjTab, "WTMP Tab", 1);
		
	}
	
	public ActionsProjectTab getProjectTab()
	{
		return _actionsProjTab;
	}


	


	/**
	 * @param e 
	 * @return
	 */
	public void showInProjectTreeAction()
	{
		Component comp = _tabbedPane.getSelectedComponent();
		//comp.showInProjectTreeAction();
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
	
	
	/**
	 * 
	 */
	private void addListeners()
	{
		Project.addStaticManagerContainer(SimGroupContainerNode.class);  
		
		_projectSimulationListener = new ProjectSimulationListener();
		_projectSimulationGroupListener = new ProjectSimulationGroupListener();
		Project.addStaticProjectListener(new ProjectAdapter()
		{
			@Override
			public void projectLoaded(ProjectEvent e)
			{
			}
			@Override
			public void projectOpened(ProjectEvent e )
			{
				Project prj = e.getProject();
				if ( !prj.isNoProject())
				{
					checkForMissingManagers(e.getProject());
					prj.addManagerListener(_projectSimulationListener);
					prj.addManagerListener(_projectSimulationGroupListener);
				}
				clearForm();
				checkRepoOutofDateStatus();
			}
			@Override
			public void  projectClosed(ProjectEvent e ) 
			{
				clearForm();
				e.getProject().removeManagerListener(_projectSimulationListener);
				e.getProject().removeManagerListener(_projectSimulationGroupListener);
			}
		});
		
	}
	/**
	 * 
	 */
	protected void checkForMissingManagers(Project project)
	{
		MissingManagersChecker checker = new MissingManagersChecker();
		checker.checkForMissingManagers(project);
	}



	@Override
	public void clearForm()
	{
		super.clearForm();
		_calibrationPanel.clearForm();
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
			_calibrationPanel.clearForm();
			_calibrationPanel.setSimulationGroup(sg);
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	

	

	public void addMessage(String message)
	{
		if ( message == null )
		{
			//((RmaListModel)_statusList.getModel()).addElement("");
		}
		else
		{
			//((RmaListModel)_statusList.getModel()).addElement(message);
		}
	}

	/**
	 * @return
	 */
	public List<WatSimulation> getSelectedSimulations()
	{
		Component comp = _tabbedPane.getSelectedComponent();
		if ( comp == _calibrationPanel )
		{
			return _calibrationPanel.getSelectedSimulations();
		}
		else if ( comp == _forecastPanel )
		{
			return _forecastPanel.getSelectedSimulations();
		}
		return null;
	}
	
	public List<ResultsData> getSelectedResults()
	{
		Component comp = _tabbedPane.getSelectedComponent();
		if ( comp == _calibrationPanel )
		{
			return _calibrationPanel.getSelectedResults();
		}
		else if ( comp == _forecastPanel )
		{
			return _forecastPanel.getSelectedResults();
		}
		return null;
	}

	/**
	 * @return
	 */
	public AbstractSimulationGroup getSimulationGroup()
	{
		Component comp = _tabbedPane.getSelectedComponent();
		if ( comp == _calibrationPanel )
		{
			return _calibrationPanel.getSimulationGroup();
		}
		else if ( comp == _forecastPanel )
		{
			return _forecastPanel.getSimulationGroup();
		}
		return null;
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
	

	


	


	
	public class ProjectSimulationGroupListener implements ProjectManagerListener
	{
		
		public ProjectSimulationGroupListener()
		{
			super();
		}
		public void managerAdded(ManagerProxy proxy)
		{
			// do nothing
		}
		@Override
		public Class<?> getManagerClass()
		{
			return SimulationGroup.class;
		}
		@Override
		public void managerDeleted(ManagerProxy proxy)
		{
			if ( proxy == null )
			{
				return;
			}
			SimulationGroup simGroup = (SimulationGroup) proxy.getManager();
			if ( proxy.getManager()==getSimulationGroup() )
			{
				setSimulationGroup(null);
				new DeleteSimulationGroupAction(ActionsWindow.this).deleteSimulationGroup(simGroup);
			}
		}
	}
	public class ProjectSimulationListener implements ProjectManagerListener
	{
		public ProjectSimulationListener()
		{
			super();
		}

		@Override
		public void managerAdded(ManagerProxy proxy)
		{
			// do nothing
		}

		@Override
		public void managerDeleted(ManagerProxy proxy)
		{
			if ( proxy == null )
			{
				return;
			}
			
			String name = proxy.getName();
			AbstractSimulationGroup simGroup = getSimulationGroup();
			WatSimulation sim;
			if ( simGroup != null )
			{
				boolean deleted = false;
				List<WatSimulation> sims = simGroup.getSimulations();
				for (int i = 0;i < sims.size(); i++ )
				{
					sim = sims.get(i);
					if ( name.equals(sim.getName()))
					{
						simGroup.removeSimulation(sim);
						simGroup.setModified(true);
						deleted = true;
						break;
					}
				}
				if ( deleted )
				{
					_calibrationPanel.setSimulationTable(simGroup);
					if ( simGroup.getSimulations().isEmpty() )
					{
						String msg = "There are no more simulations in the Simulation Group.  Would you like to delete the Simulation Group?";
						String title = "Delete Simulation Group?";
						int opt = JOptionPane.showConfirmDialog(ActionsWindow.this, msg, title, JOptionPane.YES_NO_OPTION);
						if ( opt == JOptionPane.YES_OPTION )
						{
							if ( new DeleteSimulationGroupAction(ActionsWindow.this).deleteSimulationGroup(simGroup))
							{
								setSimulationGroup(null);
							}
						}
					}
				}
			}
		}

		@Override
		public Class<?> getManagerClass()
		{
			return WatSimulation.class;
		}
	}
	



	
}
