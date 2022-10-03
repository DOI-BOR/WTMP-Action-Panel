/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.editors;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import com.rma.client.Browser;
import com.rma.model.Manager;
import com.rma.model.Project;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;
import rma.util.RMASort;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SelectSimulationGroupDialog extends RmaJDialog
{


	private static final int SIM_GROUP_COL = 0;

	private static final int MAX_RECENT = 5;

	private static final String SIM_GROUP_KEY = "SimGroup";
	
	private RmaJTable _simGroupTable;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;

	private RmaJTextField _filterTextField;

	private JMenu _recentMenu;

	private ActionsWindow _parent;

	/**
	 * @param parent
	 * @param b
	 */
	public SelectSimulationGroupDialog(ActionsWindow parent, boolean modal)
	{
		super(parent, modal);
		_parent = parent;
		buildControls();
		addListeners();
		fillForm();
		buildMenus();
		pack();
		setLocationRelativeTo(getParent());
	}

	
	


	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		
		setTitle("Select Simulation Group");
	
		_filterTextField=new RmaJTextField();
		_filterTextField.setToolTipText("Filter Table Contents by Simulation Group Name");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_filterTextField, gbc);
		
		String[] headers = new String[] {"Simulation Group", "Description", "Analysis Period"};
		_simGroupTable = new RmaJTable(this, headers)
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				int row = _simGroupTable.rowAtPoint(e.getPoint());
				if ( row == -1 )
				{
					return super.getToolTipText(e);
				}
				int col = _simGroupTable.columnAtPoint(e.getPoint());
				if ( col == SIM_GROUP_COL )
				{
					return getSimGroupToolTip(row, col);
				}
				return super.getToolTipText(e);
				
			}
		};
		_simGroupTable.removePopupMenuSumOptions();
		_simGroupTable.setCellSelectionEnabled(false);
		_simGroupTable.setRowSelectionAllowed(true);
		_simGroupTable.setRowHeight(_simGroupTable.getRowHeight()+5);
		_simGroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_simGroupTable.setColumnEnabled(false, 0);
		_simGroupTable.setColumnEnabled(false, 1);
		_simGroupTable.setColumnEnabled(false, 2);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupTable.getScrollPane(), gbc);
	
	
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
	private void buildMenus()
	{
		JMenuBar mbar = new JMenuBar();
		
		_recentMenu = new JMenu("Recent");
		_recentMenu.setMnemonic('R');
		mbar.add(_recentMenu);
		
		setJMenuBar(mbar);
		
		updateRecentMenu();
		
		
	}
	/**
	 * @param row
	 * @param col
	 * @return
	 */
	protected String getSimGroupToolTip(int row, int col)
	{
		Object obj = _simGroupTable.getValueAt(row,  col);
		if ( obj instanceof SimulationGroup )
		{
			SimulationGroup simGroup = (SimulationGroup) obj;
			StringBuffer buf = new StringBuffer();
			buf.append("<html>");
			buf.append("Simulations in <b>"+ simGroup+"</b>:");
			List<WatSimulation> sims = simGroup.getSimulations();
			WatSimulation sim;
			if ( sims.isEmpty())
			{
				buf.append("<br>None");
			}
			else
			{
				buf.append("<p style = \"margin-left: 10px\">");
				for (int i = 0;i < sims.size();i++)
				{
					sim = sims.get(i);
					if ( sim != null )
					{
						if ( i > 0 )
						{
							buf.append("<br>");
						}
						buf.append("- ");
						buf.append(sim.getName());
					}
				}
				buf.append("</p>");
			}
			buf.append("</html>");
			return buf.toString();
		}
		return null;
	}


	/**
	 * 
	 */
	private void addListeners()
	{
		
		_filterTextField.addActionListener(e->filterTable());
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						_canceled = false;
						addSimultionGroupToRecentMenu(getSelectedSimulationGroup());
						setVisible(false);
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
	 * @return
	 */
	protected void updateRecentMenu()
	{
		_recentMenu.removeAll();
		
		Preferences recentSimGroupNode = getRecentNode();
		String simGroupName;
		JMenuItem menuItem;
		for (int i = 0;i < MAX_RECENT; i++ )
		{
			simGroupName = recentSimGroupNode.get(SIM_GROUP_KEY+i, null);
			if ( simGroupName == null )
			{
				break;
			}
			if ( !hasSimGroup(simGroupName))
			{
				continue;
			}
			menuItem= new JMenuItem(simGroupName);
			menuItem.addActionListener(e->openSimulationGroup(e));
			_recentMenu.add(menuItem);
			
		}
		
	}





	/**
	 * @param simGroupName
	 * @return
	 */
	private boolean hasSimGroup(String simGroupName)
	{
		int rowCnt = _simGroupTable.getRowCount();
		
		SimulationGroup simGroup;
		for (int r = 0; r < rowCnt; r++ )
		{
			simGroup =  (SimulationGroup) _simGroupTable.getValueAt(r, 0);
			if (  simGroup.getName().equals(simGroupName) ) 
			{
				return true;
			}
		}
		return false;
	}





	/**
	 * @param e
	 * @return
	 */
	private void openSimulationGroup(ActionEvent e)
	{
		if ( e.getSource() instanceof JMenuItem)
		{
			JMenuItem mi = (JMenuItem) e.getSource();
			openSimulationGroup(mi.getText());
		}
	}





	/**
	 * @param text
	 */
	private void openSimulationGroup(String simGroupName)
	{
		Manager simGroup = Project.getCurrentProject().getManager(simGroupName, SimulationGroup.class);
		if ( simGroup instanceof SimulationGroup )
		{
			//_parent.setSimulationGroup((SimulationGroup) simGroup);
			if ( setSelectedSimGroup(simGroup))
			{
				addSimultionGroupToRecentMenu((SimulationGroup)simGroup);
				_canceled = false;
				setVisible(false);
			}
		}
		
	}





	/**
	 * @param simGroup
	 */
	private boolean setSelectedSimGroup(Manager simGroup)
	{
		int rowCnt = _simGroupTable.getRowCount();
		for (int r = 0; r < rowCnt; r++ )
		{
			if ( _simGroupTable.getValueAt(r, 0) == simGroup)
			{
				_simGroupTable.updateSelection(r, 0, false, false);
				return true;
			}
		}
		return false;
		
	}





	/**
	 * @return
	 */
	private static Preferences getRecentNode()
	{
		Preferences prjPrefsNode = Browser.getBrowserFrame().getPreferences().getProjectPreferenceNode();
		Preferences wtmpNode = prjPrefsNode.node("wtmp");
		Preferences recentSimGroupNode = wtmpNode.node("recentSimGroup");
		return recentSimGroupNode;
	}

	public void addSimultionGroupToRecentMenu(SimulationGroup simGroup)
	{
		if ( simGroup == null )
		{
			return;
		}
		String simGroupName = simGroup.getName();
		JMenuItem menu = findSimGroupMenu(simGroupName);
		if ( menu != null )
		{
			_recentMenu.remove(menu);
		}
		else
		{
			menu = new JMenuItem(simGroupName);
			menu.addActionListener(e->openSimulationGroup(e));
		}
		if ( _recentMenu.getMenuComponentCount() >= MAX_RECENT )
		{
			_recentMenu.remove(MAX_RECENT-1);
		}
		_recentMenu.insert(menu, 0);
		saveRecentMenu();
	}



	/**
	 * 
	 */
	private void saveRecentMenu()
	{
		Preferences simGroupNode = getRecentNode();
		try
		{
			simGroupNode.clear();
		}
		catch (BackingStoreException e)
		{
			e.printStackTrace();
		}
		
		Component[] comps = _recentMenu.getMenuComponents();
		int idx = 0;
		String name;
		for (int i = 0;i < comps.length; i++)
		{
			if ( comps[i] instanceof JMenuItem )
			{
				JMenuItem mi = (JMenuItem) comps[i];
				name = mi.getText();
				simGroupNode.put(SIM_GROUP_KEY+idx, name);
				idx++;
			}
		}
		try
		{
			simGroupNode.flush();
		}
		catch (BackingStoreException e)
		{
			e.printStackTrace();
		}
	}





	/**
	 * @param simGroupName
	 * @return
	 */
	private JMenuItem findSimGroupMenu(String simGroupName)
	{
		if ( simGroupName == null )
		{
			return null;
		}
		Component[] comps = _recentMenu.getMenuComponents();
		for (int i = 0;i < comps.length; i++)
		{
			if ( comps[i] instanceof JMenuItem )
			{
				JMenuItem mi = (JMenuItem) comps[i];
				if (simGroupName.equals(mi.getText()))
				{
					return mi;
				}
			}
		}
		return null;
	}





	/**
	 * @return
	 */
	private void filterTable()
	{
		String filter = _filterTextField.getText().toLowerCase();
		TableRowSorter sorter = (TableRowSorter) _simGroupTable.getRowSorter();
		if ( filter.trim().isEmpty())
		{
			sorter.setRowFilter(null);
		}
		else
		{
			RowFilter rowFilter = new RowFilter()
			{

				@Override
				public boolean include(Entry entry)
				{
					String value = entry.getStringValue(SIM_GROUP_COL);
					if ( value.toLowerCase().contains(filter))
					{
						return true;
					}
					return false;
				}
				
			};
			sorter.setRowFilter(rowFilter);
		}
	}


	/**
	 * 
	 */
	private void fillForm()
	{
		_simGroupTable.deleteCells();
		
		Project proj = Project.getCurrentProject();
		List<SimulationGroup> simGroups = proj.getManagerListForType(SimulationGroup.class);
		RMASort.quickSort(simGroups);
		SimulationGroup sg;
		Vector<Object> row;
		WatAnalysisPeriod ap;
		for(int i = 0;i < simGroups.size(); i++ )
		{
			sg = simGroups.get(i);
			if ( sg.isTransitory())
			{ // not in group simgroup
				continue;
			}
			row = new Vector<>();
			row.add(sg);
			row.add(sg.getDescription());
			ap = sg.getAnalysisPeriod();
			if ( ap != null )
			{
				row.add(ap.getName());
			}
			else
			{
				row.add("<unknown>");
			}
			_simGroupTable.appendRow(row);
			
			
		}
		if ( _simGroupTable.getRowCount() == 1)
		{
			_simGroupTable.setSelectedIndices(0);
		}
		_simGroupTable.setRowSorter(new TableRowSorter(_simGroupTable.getModel()));
	}

	public boolean isCanceled()
	{
		return _canceled;
	}

	public SimulationGroup getSelectedSimulationGroup()
	{
		int row = _simGroupTable.getSelectedRow();
		if ( row > -1 )
		{
			SimulationGroup sg = (SimulationGroup) _simGroupTable.getValueAt(row, SIM_GROUP_COL);
			return sg;
		}
		return null;
	}

	

}
