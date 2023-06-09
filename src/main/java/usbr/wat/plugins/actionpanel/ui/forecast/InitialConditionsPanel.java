/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;

import com.google.common.flogger.FluentLogger;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Parameter;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.geometry.Axis;
import hec.gfx2d.G2dPanel;
import hec.gfx2d.PairedDataSet;
import hec.gfx2d.Viewport;
import hec.heclib.dss.DSSPathname;
import hec.heclib.util.Unit;
import hec.io.PairedDataContainer;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import rma.swing.table.ColumnGroup;
import rma.swing.table.GroupableTableHeader;
import rma.swing.table.RmaTableModel;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.ReviewDataAction;
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.IcReservoirInfo;
import usbr.wat.plugins.actionpanel.model.forecast.InitialConditions;

/**
 * @author mark
 *
 */
public class InitialConditionsPanel extends AbstractForecastPanel
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String CONFIG_CSV_FILE = ForecastConfigFiles.getRelativeIcReservoirsFile();

	private static final Pattern UNIT_PATTERN = Pattern.compile("\\((.*?)\\)");
	private static final String TEMP_PLOT_MIN_PROPERTY = "WTMP.Forecast.LowerTempPlotMLimit.Celsius";
	private static final String TEMP_PLOT_MAX_PROPERTY = "WTMP.Forecast.UpperTempPlotLimit.Celsius";
	private static final int DEFAULT_TEMP_PLOT_MIN_C = 0;
	private static final int DEFAULT_TEMP_PLOT_MAX_C = 30;
	private static final String DEFAULT_TEMP_AXIS_LABEL = "temp (C)";
	private static final String DEFAULT_DEPTH_AXIS_LABEL = "depth (ft)";
	private EnabledJPanel _plotsPanel;
	private EnabledJPanel _buttonPanel;
	private UpdateDataAction _updateDataAction;
	private ReviewDataAction _reviewDataAction;
	private Map<String, ResComponents>_resComponents = new HashMap<>();
	private boolean _ignoreTableModification = false;

	/**
	 * @param forecastPanel
	 */
	public InitialConditionsPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
		addListeners();
		
	}

	

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		
		_plotsPanel = new EnabledJPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_plotsPanel, gbc);
		
		_buttonPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_buttonPanel, gbc);
	
		buildButtonPanel(_buttonPanel);
	}
	/**
	 * @param plotsPanel
	 */
	private void buildPlotsPanel(EnabledJPanel plotsPanel)
	{
		_plotsPanel.removeAll();
		
		List<IcReservoirInfo> reservoirInfos = getReservoirInfo();
		IcReservoirInfo resInfo;
		for (int i = 0;i < reservoirInfos.size(); i++ )
		{
			resInfo = reservoirInfos.get(i);
			RmaJTable table = new RmaJTable(this, new String[] {"Select", "Date"})
			{
				@Override
				public boolean isCellEditable(int row, int col)
				{
					return col == 0 && InitialConditionsPanel.this.isEnabled();
				}
			};
			table.setRowHeight(table.getRowHeight()+5);
			table.setName(resInfo.getReservoirName());
			table.setTableHeader(new GroupableTableHeader(table.getColumnModel()));
			TableColumnModel cm = table.getColumnModel();
			ColumnGroup columnGroup = new ColumnGroup(resInfo.getReservoirName());
			columnGroup.add(cm.getColumn(0));
			columnGroup.add(cm.getColumn(1));
			table.setCheckBoxCellEditor(0);
			GroupableTableHeader header = (GroupableTableHeader)table.getTableHeader();
			header.addColumnGroup(columnGroup);
			GridBagConstraints gbc = new GridBagConstraints();
			int width = (i==reservoirInfos.size()-1?GridBagConstraints.REMAINDER:1);
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = width;
			gbc.weightx   = 1.0;
			gbc.weighty   = 1.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.BOTH;
			gbc.insets    = RmaInsets.INSETS5505;
			_plotsPanel.add(table.getScrollPane(), gbc);

			fillTable(table, resInfo);
			table.getModel().addTableModelListener(this::tableModelChanged);
			_resComponents.put(resInfo.getReservoirName(), new ResComponents(table, null));
		}
		for (int i = 0;i < reservoirInfos.size(); i++ )
		{
			resInfo = reservoirInfos.get(i);
			int width = (i==reservoirInfos.size()-1?GridBagConstraints.REMAINDER:1);
		
			G2dPanel plotPanel = new G2dPanel();
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = width;
			gbc.weightx   = 1.0;
			gbc.weighty   = 1.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.BOTH;
			gbc.insets    = RmaInsets.INSETS5505;
			_plotsPanel.add(plotPanel, gbc);
			ResComponents comps = _resComponents.get(resInfo.getReservoirName());
			comps.plotPanel = plotPanel;
		}
		revalidate();
	}
	/**
	 * @param e
	 * @return
	 */
	private void tableModelChanged(TableModelEvent e)
	{
		if (_ignoreTableModification || e.getType() != TableModelEvent.UPDATE )
		{
			return;
		}
		setModified(true);
		RmaTableModel tableModel = (RmaTableModel) e.getSource();
		Component focusedComp = FocusManager.getCurrentManager().getFocusOwner();
		RmaJTable table = (RmaJTable) SwingUtilities.getAncestorOfClass(RmaJTable.class, focusedComp);
		if(table != null)
		{
			int row = e.getFirstRow();
			Object checkedObj = tableModel.getValueAt(row, 0);
			if(checkedObj instanceof Boolean && ((Boolean)checkedObj))
			{
				_ignoreTableModification = true;
				clearTableSelection(table);
				tableModel.setValueAt(true, row, 0);
				_ignoreTableModification = false;
			}
			buildTablePlot(table);
		}
	}
	private void buildTablePlot(RmaJTable table)
	{
		if ( table != null )
		{
			int rows = table.getRowCount();
			Object obj;
			Profile profile;
			List<PairedDataSet>pdcsToPlot = new ArrayList<>();
			for (int r = 0; r < rows; r++ )
			{
				obj = table.getValueAt(r,0);
				if ( "true".equalsIgnoreCase(obj.toString())||obj == Boolean.TRUE)
				{
					profile = (Profile) table.getValueAt(r, 1);
					pdcsToPlot.add(new PairedDataSet(profile._pdc));
				}
			}
			String resName = table.getName();
			ResComponents comps = _resComponents.get(resName);
			if ( pdcsToPlot.isEmpty() )
			{
				comps.plotPanel.clearPanel();
			}
			else
			{
				comps.plotPanel.buildComponents(pdcsToPlot,false, false);
				Viewport[] viewports = comps.plotPanel.getViewports();
				if ( viewports != null && viewports.length > 0 )
				{
					viewports[0].getAxis("Y1").setReversed(false);
				}
			}
			revalidate();
			fixZoom(comps, pdcsToPlot);
		}
	}

	private void fixZoom(ResComponents comps, List<PairedDataSet> pdcsToPlot)
	{
		Viewport[] viewports = comps.plotPanel.getViewports();
		if ( viewports != null && viewports.length > 0 )
		{
			viewports[0].getAxis("Y1").setReversed(false);
			Axis xaxis = viewports[0].getAxis("x1");
			String xUnit = extractContentWithinParentheses(pdcsToPlot.get(0).getXAxisName());
			try
			{
				int min = getLowerTempPlotLimit(xUnit);
				int max = getUpperTempPlotLimit(xUnit);
				xaxis.setMinimumLimit(min);
				xaxis.setMaximumLimit(max);
				xaxis.setViewLimits(min, max);
				xaxis.setMajorTicInterval(5);
				comps.plotPanel.setVisible(true);
				comps.plotPanel.repaint();
			}
			catch (DataSetIllegalArgumentException | UnitsConversionException e)
			{
				LOGGER.atConfig().withCause(e).log("Failed to determine units from label " + pdcsToPlot.get(0).getXAxisName());
			}
		}
	}

	private String extractContentWithinParentheses(String input)
	{
		String retVal = null;
		Matcher matcher = UNIT_PATTERN.matcher(input);
		if (matcher.find())
		{
			retVal = matcher.group(1);
		}
		return retVal;
	}



	


	/**
	 * @param table
	 * @param resInfo
	 */
	private void fillTable(RmaJTable table, IcReservoirInfo resInfo)
	{
		Project prj = Project.getCurrentProject();
		List<String> profileFileNames = resInfo.getProfileFileNames();
		String fileName;
		Vector row;
		table.deleteCells();
		for (int i = 0;i < profileFileNames.size();i++ )
		{
			fileName = profileFileNames.get(i);
			fileName = prj.getAbsolutePath(fileName);
			Set<Profile> profiles = readProfileFile(fileName);
			for (Profile profile : profiles)
			{
				row = new Vector();
				row.add(Boolean.FALSE);
				row.add(profile);
				table.appendRow(row);
			}
		}
		
	}



	/**
	 * @param fileName
	 * @return
	 */
	private Set<Profile> readProfileFile(String fileName)
	{
		Set<Profile>profiles = new TreeSet<>();
		RmaFile file = FileManagerImpl.getFileManager().getFile(fileName);
		if ( file == null )
		{
			LOGGER.atInfo().log("Failed to find file:"+fileName);
			return profiles;
		}
		BufferedReader reader = file.getBufferedReader();
		if ( reader == null )
		{
			LOGGER.atInfo().log("Failed to get Reader for file:"+fileName);
			return profiles;
		}
		String line;
		PairedDataContainer pdc;
		String currDate = null, date;
		String[] parts;
		DSSPathname pathname = new DSSPathname();
		Profile profile = null;
		List<String>temps = new ArrayList<>();
		List<String>depths = new ArrayList<>();
		
		try
		{
			reader.readLine(); //toss the first line
			while ((line=reader.readLine())!= null )
			{
				parts = line.split(",");
				if ( parts.length != 3 )
				{
					continue;
				}
				date = parts[0];
				if ( currDate == null || !date.equals(currDate))
				{
					if ( profile != null )
					{
						fillInProfilePdc(profile, temps, depths);
					}
					currDate = date;
					temps.clear();
					depths.clear();
					pdc = new PairedDataContainer();
					pathname.setCPart("TEMP-ELEV");
					pathname.setFPart(date);
					pdc.fullName = pathname.getPathname();
					pdc.fileName = fileName;
					int idx = date.indexOf(' ');
					if ( idx > -1 )
					{
						date = date.substring(0,idx);
					}
					profile = new Profile(date);
					profiles.add(profile);
					profile._pdc = pdc;
				}
				temps.add(parts[1]);
				depths.add(parts[2]);

			}
			if ( profile != null )
			{
				fillInProfilePdc(profile, temps, depths);
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
			}
		}
		return profiles;
	}



	/**
	 * @param profile
	 * @param tempsList
	 * @param depthsList
	 */
	private void fillInProfilePdc(Profile profile, List<String> tempsList,
			List<String> depthsList)
	{
		profile._pdc.setNumberCurves(1);
		profile._pdc.setNumberOrdinates(tempsList.size());
		//these are always C and ft. In future csv file should provide units.
		profile._pdc.xunits = DEFAULT_TEMP_AXIS_LABEL;
		profile._pdc.yunits = DEFAULT_DEPTH_AXIS_LABEL;
		double[]temps = toDoubleArray(tempsList);
		double[]depths = toDoubleArray(depthsList);
		double[][] depths2 = new double[1][0];
		depths2[0] = depths;
		profile._pdc.setValues(temps, depths2);
	}


	private int getLowerTempPlotLimit(String units) throws DataSetIllegalArgumentException, UnitsConversionException
	{
		int min = Integer.getInteger(TEMP_PLOT_MIN_PROPERTY, DEFAULT_TEMP_PLOT_MIN_C);
		String siTempUnits = Parameter.getParameter(Parameter.PARAMID_TEMP).getUnitsStringForSystem(Unit.SI_ID);
		min = (int) Units.convertUnits(min, siTempUnits, units);
		return min;
	}

	private int getUpperTempPlotLimit(String units) throws DataSetIllegalArgumentException, UnitsConversionException
	{
		int max = Integer.getInteger(TEMP_PLOT_MAX_PROPERTY, DEFAULT_TEMP_PLOT_MAX_C);
		String siTempUnits = Parameter.getParameter(Parameter.PARAMID_TEMP).getUnitsStringForSystem(Unit.SI_ID);
		max = (int) Units.convertUnits(max, siTempUnits, units);
		return max;
	}



	/**
	 * @param valuesList
	 * @return
	 */
	private double[] toDoubleArray(List<String> valuesList)
	{
		double[] values = new double[valuesList.size()];
		for(int i = 0; i < valuesList.size(); i++ )
		{
			values[i] = RMAIO.parseDouble(valuesList.get(i));
		}
		return values;
	}



	/**
	 * 
	 */
	protected void addListeners()
	{
		addUpperTableListeners();
	}

	/**
	 * @return
	 */
	private List<IcReservoirInfo> getReservoirInfo()
	{
		List<IcReservoirInfo>resInfos = new ArrayList<>();
		
		String configFile = getInitialConditionsConfigFile();
		if ( configFile == null )
		{
			JOptionPane.showMessageDialog(this,  "Failed to find file "+configFile+" to populate Initial Conditions File", "Missing File", JOptionPane.PLAIN_MESSAGE);
			return resInfos;
		}
		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);
		
		BufferedReader reader = file.getBufferedReader();
		if ( reader == null )
		{
			JOptionPane.showMessageDialog(this,  "Failed to read file "+configFile+" to populate Initial Conditions File", "Missing File", JOptionPane.PLAIN_MESSAGE);
			return resInfos;
		}
		String line;
		IcReservoirInfo resInfo;
		try
		{
			while ( (line = reader.readLine()) != null )
			{
				String[] parts = line.split(",");
				if ( parts != null && parts.length > 0 )
				{
					resInfo = new IcReservoirInfo();
					resInfo.setReservoirName(parts[0]);
					if ( parts.length > 1 )
					{
						for (int i = 1; i < parts.length;i++ )
						{
							resInfo.addProfileFileName(parts[i]);
						}
					}
					resInfos.add(resInfo);
				}
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{ }
		}
		
		return resInfos;
	}

	/**
	 * @return
	 */
	private String getInitialConditionsConfigFile()
	{
		Project prj = Project.getCurrentProject();
		if ( prj.isNoProject())
		{
			return null;
		}
		String dir = prj.getProjectDirectory();
		String configFile = RMAIO.concatPath(dir, CONFIG_CSV_FILE);
		return configFile;
	}

	/**
	 * @param buttonPanel 
	 * 
	 */
	private void buildButtonPanel(JPanel buttonPanel)
	{
		_updateDataAction = new UpdateDataAction(ActionPanelPlugin.getInstance().getActionsWindow());
		GridBagConstraints gbc = new GridBagConstraints();
		JButton button = new JButton(_updateDataAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		buttonPanel.add(button, gbc);
	
		_reviewDataAction = new ReviewDataAction(ActionPanelPlugin.getInstance().getActionsWindow());
		button = new JButton(_reviewDataAction);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		buttonPanel.add(button, gbc);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return null;
	}
	
	@Override
	protected void savePanel()
	{
		ForecastSimGroup simGrp = _forecastPanel.getSimulationGroup();
		if(simGrp != null)
		{
			InitialConditions ics = new InitialConditions();

			Set<String> keySet = _resComponents.keySet();
			Iterator<String> keyIter = keySet.iterator();
			String resName;
			ResComponents comp;
			List<String>selectedProfileNames;
			while (keyIter.hasNext())
			{
				resName = keyIter.next();
				comp = _resComponents.get(resName);
				selectedProfileNames = findSelectedRows(comp.table);
				if ( selectedProfileNames.size() > 0 )
				{
					ics.putSelectedProfiles(resName, selectedProfileNames);
				}
			}
			simGrp.setInitialConditions(ics);
		}

	}
	
	/**
	 * @param table
	 * @return
	 */
	private List<String> findSelectedRows(RmaJTable table)
	{
		int rowCnt = table.getRowCount();
		Object obj;
		
		List<String>selectedProfileNames = new ArrayList<>();
		Profile profile;
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = table.getValueAt(r, 0);
			if ( obj == Boolean.TRUE || "true".equalsIgnoreCase(obj.toString()))
			{
				profile = (Profile) table.getValueAt(r, 1);
				selectedProfileNames.add(profile._name);
			}
		}
		
		
		return selectedProfileNames;
	}
	/**
	 * @param fsg
	 */
	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		setEnabled(fsg != null);
		clearTableSelections();
		if ( fsg != null )
		{
			buildPlotsPanel(_plotsPanel);
			InitialConditions ic = fsg.getInitialConditions();
			Set<Entry<String, ResComponents>> entrySet = _resComponents.entrySet();
			Iterator<Entry<String, ResComponents>> iter = entrySet.iterator();
			Entry<String, ResComponents> entry;
			String resName;
			List<String> selectedProfiles;
			while (iter.hasNext())
			{
				entry = iter.next();
				resName = entry.getKey();
				selectedProfiles = ic.getSelectedProfiles(resName);
				fillTableSelections(entry.getValue().table, selectedProfiles);
				buildTablePlot(entry.getValue().table);
			}
		}
		setModified(false);
	}



	/**
	 * 
	 */
	private void clearTableSelections()
	{
		Collection<ResComponents> comps = _resComponents.values();
		Iterator<ResComponents> iter = comps.iterator();
		ResComponents comp;
		while (iter.hasNext())
		{
			comp = iter.next();
			clearTableSelection(comp.table);
		}
	}



	/**
	 * @param table
	 */
	private void clearTableSelection(RmaJTable table)
	{
		if(table != null)
		{
			int rowCnt = table.getRowCount();
			for(int r = 0;r < rowCnt;r++ )
			{
				table.setValueAt(Boolean.FALSE, r, 0);
			}
		}
	}



	/**
	 * @param table
	 * @param selectedProfiles
	 */
	private void fillTableSelections(RmaJTable table,
			List<String> selectedProfiles)
	{
		if ( selectedProfiles == null )
		{
			return;
		}
		String profileName;
		int rowCnt = table.getRowCount();
		Profile profile;
		for (int p = 0; p < selectedProfiles.size(); p++ )
		{
			profileName = selectedProfiles.get(p);
			for (int r = 0; r < rowCnt; r++ )
			{
				profile = (Profile) table.getValueAt(r, 1);;
				if ( profileName.equals(profile._name))
				{
					table.setValueAt(Boolean.TRUE, r, 0);
					break;
				}
			}
		}
	}

	private class ResComponents
	{
		RmaJTable table;
		G2dPanel plotPanel;
		ResComponents(RmaJTable t, G2dPanel p)
		{
			table = t;
			plotPanel = p;
		}
	}

	private class Profile implements Comparable<Profile>
	{
		private final Date _date;
		PairedDataContainer _pdc;
		String _name;
		
		/**
		 * @param date
		 */
		public Profile(String date)
		{
			_name = date;
			_date = parseDate(date);
		}

		@Override
		public String toString()
		{
			return _name;
		}

		@Override
		public int compareTo(Profile other)
		{
			int retVal = 1;
			if(other != null)
			{
				retVal = other._date.compareTo(_date);
			}
			return retVal;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (o == null || getClass() != o.getClass())
			{
				return false;
			}
			Profile profile = (Profile) o;
			return Objects.equals(_date, profile._date) && Objects.equals(_name, profile._name);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(_date, _name);
		}

		private Date parseDate(String dateString)
		{
			Date retVal = new Date(Long.MIN_VALUE);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			try
			{
				retVal = dateFormat.parse(dateString);
			}
			catch (ParseException e)
			{
				LOGGER.atInfo().withCause(e).log("Failed to parse date " + _name);
			}
			return retVal;
		}
	}

	@Override
	protected void tableRowSelected(int selectedRow)
	{
		// no table for IC panel so nothing to do here
	}

	@Override
	public void tableRowDeleteClicked(int selectedRow)
	{
		//no table to delete from for IC panel
	}

	
}
