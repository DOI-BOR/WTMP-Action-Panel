/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.editors;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.rma.client.Browser;
import com.rma.factories.DeleteManagerFactory;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Manager;
import com.rma.model.Project;

import hec.gui.NameDescriptionPanel;

import hec2.wat.factories.NewAnalysisPeriodFactory;
import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.list.RmaListModel;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.commands.AbstractNewSimulationGroupCmd;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class NewSimulationGroupDialog extends RmaJDialog
{

	private static final int SELECTED_COLUMN = 0;
	private static final int SIMULATION_COLUMN = 1;
	private static final String ForecastSimulationGroup = null;
	
	
	private NameDescriptionPanel _nameDescPanel;
	private RmaJComboBox<WatAnalysisPeriod> _apCombo;
	private JButton _newApButton;
	private RmaJTable _simTable;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;
	private AbstractSimulationGroup _simGroup;
	private Class< ? extends AbstractSimulationGroup> _simGroupClass;
	private Class< ? extends AbstractNewSimulationGroupCmd> _simGroupCmdClass;

	private boolean _runExtract;

	/**
	 * @param parent
	 * @param modal
	 */
	public NewSimulationGroupDialog(ActionsWindow parent, boolean modal, String title)
	{
		super(parent, modal);
		setTitle(title);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		
		_nameDescPanel = new NameDescriptionPanel();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5005;
		getContentPane().add(_nameDescPanel, gbc);
		
		
		JLabel label = new JLabel("Analysis Period:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_apCombo = new RmaJComboBox<>();
		label.setLabelFor(_apCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_apCombo, gbc);
		
		_newApButton = new JButton("...");
		_newApButton.setToolTipText("Create New Analysis Period");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_newApButton, gbc);
	
		String[] headers = new String[] {"Select", "Simulation", "Description"};
		_simTable = new RmaJTable(this, headers);
		_simTable.setRowHeight(_simTable.getRowHeight()+5);
		_simTable.setCheckBoxCellEditor(0);
		_simTable.setColumnEnabled(false,  1);
		_simTable.setColumnEnabled(false,  2);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_simTable.getScrollPane(), gbc);
	
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
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
		_newApButton.addActionListener(e->createAnalysisPeriodAction());
		
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						saveForm();
						
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}
	
	/**
	 * 
	 */
	protected void saveForm()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
		if ( isValidData())
		{
			if ( _simGroup == null )
			{
				if ( createSimulationGroup())
				{
					_canceled = false;
					setVisible(false);
				}
			}
			else
			{
				if ( updateSimulationGroup())
				{
					_canceled = false;
					setVisible(false);
				}
			}
		}
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}



	


	/**
	 * @return
	 */
	private void createAnalysisPeriodAction()
	{
		Manager mgr = Browser.getBrowserFrame().createNewManager(new NewAnalysisPeriodFactory());
		fillAnalysisPeriodCombo();
		/*  not sure why this code isn't working
		if ( mgr instanceof WatAnalysisPeriod )
		{
			RmaListModel<WatAnalysisPeriod> model = (RmaListModel<WatAnalysisPeriod>) _apCombo.getModel();
			model.addElement((WatAnalysisPeriod) mgr);
			_apCombo.setSelectedItem(mgr);
		}
		*/
	}



	/**
	 * 
	 */
	public void fillForm()
	{
		fillAnalysisPeriodCombo();
	
		fillTable();
	}
	private void fillTable()
	{
		
		Project proj = Project.getCurrentProject();
		_simTable.deleteCells();
		List<WatSimulation> sims = proj.getManagerListForType(WatSimulation.class);
		WatSimulation sim;
		Vector row;
		List<SimulationGroup>simGroups = proj.getManagerListForType(SimulationGroup.class);
		List<ForecastSimGroup>fsimGroups = proj.getManagerListForType(ForecastSimGroup.class);
		List<AbstractSimulationGroup>allSimGroups = new ArrayList<>();
		allSimGroups.addAll(simGroups);
		allSimGroups.addAll(fsimGroups);
		for (int i = 0;i < sims.size(); i++ )
		{
			sim = sims.get(i);
			if ( sim.getClass().equals(WatSimulation.class)&& !simPartOfGroup(sim, allSimGroups))   // no FRA simulations for now
			{
				row = new Vector();
				row.add(Boolean.FALSE);
				row.add(sim);
				row.add(sim.getDescription());
				_simTable.appendRow(row);
			}
		}
	}
	/**
	 * @param simGroup
	 */
	public void fillForm(AbstractSimulationGroup simGroup)
	{
		fillAnalysisPeriodCombo();
		_simGroup = simGroup;
		fillTable();
		
		setTitle("Edit Simulation Group ");
		_nameDescPanel.setName(simGroup.getName());
		_nameDescPanel.setNameEditable(false);
		_nameDescPanel.setDescription(simGroup.getDescription());
		
		List<WatSimulation> sims = _simGroup.getSimulations();
		WatSimulation sim;
		for(int i = 0;i < sims.size(); i++ )
		{
			sim = sims.get(i);
			selectSimulation(sim);
		}
		
		WatAnalysisPeriod ap = _simGroup.getAnalysisPeriod();
		_apCombo.setSelectedItem(ap);
		
	}	
	/**
	 * @param sim
	 */
	private void selectSimulation(WatSimulation sim)
	{
		int rows = _simTable.getRowCount();
		String origSimName = getOriginalSimName(sim.getName());
		WatSimulation tblSim;
		for (int r = 0;r < rows; r++ )
		{
			tblSim= (WatSimulation) _simTable.getValueAt(r, SIMULATION_COLUMN);
			if (  tblSim.getName().equals(origSimName) )
			{
				_simTable.setValueAt(Boolean.TRUE, r, SELECTED_COLUMN);
				return;
			}
		}
	}



	/**
	 * 
	 */
	private void fillAnalysisPeriodCombo()
	{
		Project proj = Project.getCurrentProject();
		List<WatAnalysisPeriod> aps = proj.getManagerListForType(WatAnalysisPeriod.class);
		RmaListModel newmodel = new RmaListModel(true, aps);
		_apCombo.setModel(newmodel);
		
	}



	/**
	 * check to see if the simulation is already part of a group 
	 * @param sim
	 * @return true if it is part of a simulation group
	 */
	private boolean simPartOfGroup(WatSimulation sim, List<AbstractSimulationGroup>simGroups)
	{
		int size = simGroups.size();
		AbstractSimulationGroup simGroup;
		for (int i = 0;i < size; i++ )
		{
			simGroup = simGroups.get(i);
			if ( simGroup == null )
			{
				continue;
			}
			if (simGroup.containsSimulation(sim))
			{
				return true;
			}
		}
		return false;
	}



	/**
	 * @return
	 */
	protected boolean createSimulationGroup()
	{
		String name = _nameDescPanel.getName();
		String desc = _nameDescPanel.getDescription();
		RmaFile file = getSimulationGroupFolder();
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();
		List<WatSimulation> sims =  getSelectedSimulations();
		Class<?>paramClasses[] = new Class[] {Project.class, String.class, String.class, RmaFile.class, WatAnalysisPeriod.class,
				List.class};
		
		Constructor< ? extends AbstractNewSimulationGroupCmd> ctor;
		try
		{
			ctor = _simGroupCmdClass.getConstructor(paramClasses);
		}
		catch (NoSuchMethodException | SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		AbstractNewSimulationGroupCmd cmd;
		try
		{
			cmd = ctor.newInstance(Project.getCurrentProject(),name, desc,file, ap, sims);
		}
		catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		cmd.doCommand();
		_simGroup = cmd.getSimulationGroup();
		return _simGroup != null;
	}
	/**
	 * @return
	 */
	private boolean updateSimulationGroup()
	{
		String desc = _nameDescPanel.getDescription();
		_simGroup.setDescription(desc);
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();
		List<WatSimulation> sgSims = _simGroup.getSimulations();
		if ( ap != _simGroup.getAnalysisPeriod() )
		{
			_simGroup.setAnalysisPeriod(ap);
			WatSimulation sim;
			for (int i = 0;i < sgSims.size(); i++ )
			{
				sim = sgSims.get(i);
				sim.getContainerParent().setAnalysisPeriod(ap);
				sim.getContainerParent().setModified(true);
				sim.clearHasComputed();
			}
		}
		List<WatSimulation> selectedSims =  getSelectedSimulations();
		
		List<String> selectedSimNames = selectedSims.stream().map(s->s.getName()).collect(Collectors.toList());
		List<String> existingSimNames =sgSims.stream().map(s->s.getName()).collect(Collectors.toList());
		
		List<String>origExistingSimNames = getOriginalNames(existingSimNames);
		
		Set<String>removedSet = new HashSet<>(origExistingSimNames);
		removedSet.removeAll(selectedSimNames);
		if ( !removedSet.isEmpty() ) 
		{
			StringBuilder msg = new StringBuilder();
			msg.append("The following Simulations are being removed from the Simulation Group "+_simGroup.getName()+":\n");
			Iterator<String> iter = removedSet.iterator();
			while (iter.hasNext())
			{
				msg.append("\n");
				msg.append(iter.next());
			}
			msg.append("\n\nThis will remove any results that have the simulation may have produced.\nDo you want to continue?");
			
			int opt = JOptionPane.showConfirmDialog(this, msg, "Confirm Removal", JOptionPane.YES_NO_OPTION);
			if ( opt != JOptionPane.YES_OPTION )
			{
				return false;
			}
		}
		Set<String>set = new HashSet<>(selectedSimNames);
		set.removeAll(origExistingSimNames);
		//whats left in the set are any new simulations
		Iterator<String> iter = set.iterator();
		WatSimulation simToAdd, newSim;
		Project proj = Project.getCurrentProject();
		while (iter.hasNext())
		{
			String newSimName = iter.next();
			simToAdd = findSimulationInTable(newSimName);
			if ( simToAdd != null )
			{
				newSim = AbstractNewSimulationGroupCmd.createSimulation(simToAdd, _simGroup, proj, ap, _runExtract);
				if ( newSim != null )
				{
					_simGroup.addSimulation(newSim);
				}
			}
		}
		
		// delete any unselected simulations 
		set = new HashSet<>(origExistingSimNames);
		set.removeAll(selectedSimNames);
		iter= set.iterator();
		// what's left in the set are simulations to delete
		while ( iter.hasNext())
		{
			String delSimName = iter.next();
			WatSimulation simToDel = findSimGroupSimulationByOrigName(delSimName);
			if ( simToDel != null )
			{
				if ( DeleteManagerFactory.deleteManager(simToDel))
				{
					_simGroup.removeSimulation(simToDel);
				}
			}
		}
		_simGroup.setModified(true);
		
		return true;
	}


	/**
	 * @param baseSimName
	 * @return
	 */
	private WatSimulation findSimGroupSimulationByOrigName(String baseSimName)
	{
		List<WatSimulation> sims = _simGroup.getSimulations();
		String groupSimName = AbstractNewSimulationGroupCmd.getGroupSimName(baseSimName, _simGroup.getName());
		WatSimulation sim;
		for (int i = 0;i < sims.size();i ++ )
		{
			sim = sims.get(i);
			if ( groupSimName.equals(sim.getName()))
			{
				return sim;
			}
		}
		return null;
	}



	/**
	 * @param newSimName
	 * @return
	 */
	private WatSimulation findSimulationInTable(String newSimName)
	{
		int numRows = _simTable.getRowCount();
		WatSimulation sim;
		for (int r = 0;r < numRows; r++)
		{
			sim = (WatSimulation) _simTable.getValueAt(r,  SIMULATION_COLUMN);
			if ( newSimName.equals(sim.getName()))
			{
				return sim;
			}
		}
		return null;
	}



	/**
	 * @param existingSimNames
	 * @return
	 */
	private List<String> getOriginalNames(List<String> existingSimNames)
	{
		List<String>baseSimNames = new ArrayList<>();
		for (int i = 0;i < existingSimNames.size();i++ ) 
		{
			String simName = existingSimNames.get(i);
			String baseSimulationName = getOriginalSimName(simName);
			baseSimNames.add(baseSimulationName);
		}
		return baseSimNames;
	}



	/**
	 * @param simName
	 * @return
	 */
	private String getOriginalSimName(String simName)
	{
		String groupName = _simGroup.getName();
		return  RMAIO.replace(simName, "-"+groupName, "");	
	}



	/**
	 * @return
	 */
	private static RmaFile getSimulationGroupFolder()
	{
		String dir = Project.getCurrentProject().getProjectDirectory();
		dir = RMAIO.concatPath(dir, "wat");
		dir = RMAIO.concatPath(dir, "simGroups");
		return FileManagerImpl.getFileManager().getFile(dir);
	}

	public boolean isCanceled()
	{
		return _canceled;
	}
	/**
	 * @return
	 */
	protected boolean isValidData()
	{
		Project proj = Project.getCurrentProject();
		if ( _simGroup == null )
		{
			String name = _nameDescPanel.getName();
			if ( proj.getManagerProxy(name, SimulationGroup.class) != null || 
				proj.getManagerProxy(name, ForecastSimGroup.class) != null )
			{
				JOptionPane.showMessageDialog(this, "A Simulation Group named "+name+" already exists. Please enter a unique name", 
						"Duplicate Name", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();
		if ( ap == null )
		{
			JOptionPane.showMessageDialog(this, "No Analysis Period has been selected. Please select an Analysis Period",
					"No Analaysis Period", JOptionPane.INFORMATION_MESSAGE);
			return false;
			
		}
		List<WatSimulation> selectedSimulations =  getSelectedSimulations();
		if ( selectedSimulations.isEmpty() )
		{
			JOptionPane.showMessageDialog(this, "No Simulations have been selected. Please select at least one Simulation",
					"No Simulations", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		
		
		return true;
	}

	/**
	 * @return
	 */
	private List<WatSimulation> getSelectedSimulations()
	{
		List<WatSimulation>selectedSims = new ArrayList<>();
		int rowCnt = _simTable.getRowCount();
		Object obj;
		WatSimulation sim;
		for (int r = 0;r < rowCnt; r++  )
		{
			obj = _simTable.getValueAt(r, SELECTED_COLUMN);
			if (obj == null )
			{
				continue;
			}
			
			if ( RMAIO.parseBoolean(obj.toString(), false))
			{
				sim = (WatSimulation) _simTable.getValueAt(r, SIMULATION_COLUMN);
				selectedSims.add(sim);
			}
		}
		return selectedSims;
	}

	/**
	 * @return
	 */
	public AbstractSimulationGroup getSimulationGroup()
	{
		return _simGroup;
	}



	/**
	 * @param simGroupClass
	 */
	public void setSimulationGroupClass(Class<? extends AbstractSimulationGroup> simGroupClass)
	{
		_simGroupClass = simGroupClass;
	}
	public void setSimulationGroupFactory(Class<? extends AbstractNewSimulationGroupCmd> cmdClass)
	{
		_simGroupCmdClass = cmdClass;
	}

	public void setRunExtract(boolean runExtract)
	{
		_runExtract = runExtract;
	}




	

}
