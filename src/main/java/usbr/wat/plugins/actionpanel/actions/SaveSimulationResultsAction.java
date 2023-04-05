/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.rma.io.CopyDirProgressCallbackImpl;
import com.rma.io.CopyListener;
import com.rma.io.FileManagerImpl;

import hec2.wat.model.WatSimulation;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.ui.ResultsDataDialog;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;

/**
 * @author mark
 *
 */
public class SaveSimulationResultsAction extends AbstractAction 
	implements CopyListener
{

	public static final String RESULTS_DIR = ".saveResults";
	private ActionsWindow _parent;
	private WatSimulation _currentSim;
	private String _currentResultsDir;
	private ResultsData _currentResultsData;
	private UsbrPanel _parentPanel;

	/**
	 * @param parent
	 */
	public SaveSimulationResultsAction(ActionsWindow parent, UsbrPanel parentPanel)
	{
		super("Save Results");
		setEnabled(false);
		_parent = parent;
		_parentPanel = parentPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		saveSimulationResults();
	}
	
	public void saveSimulationResults()
	{
		SimulationGroup simGroup = _parent.getCalibrationPanel().getSimulationGroup();
		if ( simGroup == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
			
		}
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		if ( sims.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to save results for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}	
		for(int i = 0;i < sims.size(); i++ )
		{
			saveResults(sims.get(i));
		}
	}

	/**
	 * @param sim
	 */
	public void saveResults(WatSimulation sim)
	{
		if ( sim == null )
		{
			return;
		}
		_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String origTitle = UIManager.getString("ProgressMonitor.progressText");
		if( origTitle == null )
		{
			origTitle = "Progress";
		}
		try
		{
			UIManager.put("ProgressMonitor.progressText", "Saving Results");
			long t1 = System.currentTimeMillis();
			System.out.println("saveResults:start...");
			_currentSim = sim;
			String runDir = sim.getRunDirectory();
			if ( !FileManagerImpl.getFileManager().fileExists(runDir))
			{
				JOptionPane.showMessageDialog(_parent, "No Results found for "+sim.getName(), "No Results", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			String resultsParentDir = RMAIO.concatPath(runDir, RESULTS_DIR);
			if ( !FileManagerImpl.getFileManager().fileExists(resultsParentDir))
			{
				FileManagerImpl.getFileManager().createDirectory(resultsParentDir);
			}
			ResultsData resultsData = getResultsData(sim);
			if ( resultsData == null )
			{
				return;
			}
			String resultsDir = RMAIO.concatPath(resultsParentDir,RMAIO.userNameToFileName(resultsData.getName()));

			FileManagerImpl.getFileManager().createDirectory(resultsDir);

			SimpleFileFilter excludeFilter = new SimpleFileFilter(RESULTS_DIR);
			_currentResultsDir = resultsDir;
			
			long t2 = System.currentTimeMillis();
			System.out.println("saveResults:alling copy handler after "+(t2-t1)+" ms");
			
			CopyDirProgressCallbackImpl callback = new CopyDirProgressCallbackImpl(10, runDir, 
					resultsDir, "Copying Results", "Copying Results for "+resultsData.getName(), excludeFilter, this);
			_currentResultsData = resultsData;
		}
		finally
		{
			//UIManager.put("ProgressMonitor.progressText", origTitle);
			_parent.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * @return
	 */
	private ResultsData getResultsData(WatSimulation sim)
	{
		
		ResultsDataDialog dlg = new ResultsDataDialog(_parent, sim);
		dlg.setVisible(true);
		if (dlg.isCanceled())
		{
			return null;
		}
		return dlg.getResultsData();
	}

	@Override
	public void copyFinished(int totalCopied)
	{
		_currentResultsData.saveDataToFolder(_currentResultsDir);
		SimulationTreeTableNode node = _parentPanel.getSimulationTreeTable().getSimulationNodeFor(_currentSim);
		node.addResultsFolder(_currentResultsDir);
		
		_currentSim = null;
		_currentResultsDir = null;
		_currentResultsData = null;
	}

	/**
	 * @param sim
	 * @param name
	 */
	public static String getResultsFolder(WatSimulation sim, String name)
	{
		String runDir = sim.getRunDirectory();
		String resultsParentDir = RMAIO.concatPath(runDir, RESULTS_DIR);
		String resultsDir = RMAIO.concatPath(resultsParentDir, RMAIO.userNameToFileName(name));
		return resultsDir;
	}
	/**
	 * 
	 * @author mark
	 *
	 */
	public class SimpleFileFilter implements FilenameFilter 
	{
		private String _name;

		public SimpleFileFilter(String name)
		{
			super();
			_name = name;
		}

		@Override
		public boolean accept(File dir, String name)
		{
			return _name.equalsIgnoreCase(name);
		}
	}

}
