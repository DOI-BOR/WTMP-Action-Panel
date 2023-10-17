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
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.rma.io.DssFileManagerImpl;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Parameter;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.geometry.Axis;
import hec.gfx2d.G2dData;
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
import usbr.wat.plugins.actionpanel.actions.ReviewDataAction;
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.IcReservoirInfo;
import usbr.wat.plugins.actionpanel.model.forecast.InitialConditions;
import usbr.wat.plugins.actionpanel.model.forecast.Profile;

/**
 * @author mark
 *
 */
public class InitialConditionsPanel extends AbstractForecastPanel<InitialConditions>
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String CONFIG_CSV_FILE = ForecastConfigFiles.getRelativeIcReservoirsFile();
	private static final Pattern UNIT_PATTERN = Pattern.compile("\\((.*?)\\)");
	private static final String TEMP_PLOT_MIN_PROPERTY = "WTMP.Forecast.LowerTempPlotMLimit.Celsius";
	private static final String TEMP_PLOT_MAX_PROPERTY = "WTMP.Forecast.UpperTempPlotLimit.Celsius";
	private static final Path OUTPUT_DSS_FILE_RELATIVE_PATH = Paths.get("shared");
	private static final int DEFAULT_TEMP_PLOT_MIN_C = 0;
	private static final int DEFAULT_TEMP_PLOT_MAX_C = 30;
	private static final String DEFAULT_TEMP_UNITS = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.SI_ID);
	private static final String DEFAULT_DEPTH_UNITS = Parameter.getUnitsStringForSystem(Parameter.PARAMID_ELEV, Units.ENGLISH_ID);
	private EnabledJPanel _plotsPanel;
	private EnabledJPanel _buttonPanel;
	private UpdateDataAction _updateDataAction;
	private ReviewDataAction _reviewDataAction;
	private Map<String, ResComponents>_resComponents = new HashMap<>();
	private boolean _ignoreTableModification = false;
	private ForecastSimGroup _fsg;

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

	@Override
	protected boolean delete(InitialConditions data, boolean deleteDueToOverwrite)
	{
		return false;
	}

	private void buildPlotsPanel()
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
			savePanel();
			fillUpperInitialConditionsTable();
			buildTablePlot(table);
		}
	}
	private void buildTablePlot(RmaJTable table)
	{
		if ( table != null )
		{
			boolean useDisplayUnits = G2dData.useDisplayUnits();
			G2dData.setUseDisplayUnits(false);
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
					pdcsToPlot.add(new PairedDataSet(profile.getPdc()));
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
			G2dData.setUseDisplayUnits(useDisplayUnits);
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
			Set<Profile> profiles = readProfileFile(fileName, resInfo);
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
	 * @param baseFileName
	 * @param resInfo
	 * @return
	 */
	private Set<Profile> readProfileFile(String baseFileName, IcReservoirInfo resInfo)
	{
		Set<Profile> profiles = new TreeSet<>();
		if(_fsg != null && _fsg.getAnalysisPeriod() != null)
		{
			List<String> fileNames = new ArrayList<>();
			int dotIndex = baseFileName.lastIndexOf(".");
			String baseFileNameNoExtension = baseFileName.substring(0, dotIndex);
			int year = _fsg.getAnalysisPeriod().getRunTimeWindow().getStartTime().getLocalDateTime().getYear();
			if (Paths.get(baseFileNameNoExtension + "-" + year + ".csv").toFile().exists())
			{
				fileNames.add(baseFileNameNoExtension + "-" + year + ".csv");
			}
			if (Paths.get(baseFileNameNoExtension + "-" + (year - 1) + ".csv").toFile().exists())
			{
				fileNames.add(baseFileNameNoExtension + "-" + (year - 1) + ".csv");
			}
			if (fileNames.isEmpty())
			{
				fileNames.add(baseFileName);
			}
			for (String fileName : fileNames)
			{
				RmaFile file = FileManagerImpl.getFileManager().getFile(fileName);
				if (file == null)
				{
					LOGGER.atInfo().log("Failed to find file:" + fileName);
					return profiles;
				}
				BufferedReader reader = file.getBufferedReader();
				if (reader == null)
				{
					LOGGER.atInfo().log("Failed to get Reader for file:" + fileName);
					return profiles;
				}
				String line;
				PairedDataContainer pdc;
				String currDate = null;
				String date = null;
				String[] parts;
				DSSPathname pathname = new DSSPathname();
				Profile profile = null;
				List<String> temps = new ArrayList<>();
				List<String> depths = new ArrayList<>();

				try
				{
					reader.readLine(); //toss the first line
					while ((line = reader.readLine()) != null)
					{
						parts = line.split(",");
						if (parts.length != 3)
						{
							continue;
						}
						date = parts[0];
						if (!date.equals(currDate))
						{
							if (profile != null)
							{
								fillInProfilePdc(profile, temps, depths);
								writeProfileToDss(profile);
							}
							currDate = date;
							temps.clear();
							depths.clear();
							pdc = new PairedDataContainer();
							String pdcFileNameWithExtension = Paths.get(fileName).getFileName().toString();
							String pdcFileName = pdcFileNameWithExtension.substring(0, pdcFileNameWithExtension.lastIndexOf("."));
							pdc.fileName = Project.getCurrentProject().getAbsolutePath(OUTPUT_DSS_FILE_RELATIVE_PATH.resolve(pdcFileName + ".dss").toString());
							int idx = date.indexOf(' ');
							if (idx > -1)
							{
								date = date.substring(0, idx);
							}
							Profile existingProfile = _fsg.getInitialConditions().getSelectedProfile(resInfo.getReservoirName());
							if(existingProfile.getName().equalsIgnoreCase(date))
							{
								profile = existingProfile;
							}
							else
							{
								profile = new Profile(date);
							}
							profiles.add(profile);
							pathname.setBPart(resInfo.getReservoirName());
							String depthParam = Parameter.getParameter(Parameter.PARAMID_DEPTH).getParameter();
							String tempParam = Parameter.getParameter(Parameter.PARAMID_TEMP).getParameter();
							pathname.setCPart(depthParam.toUpperCase() + "-" + tempParam.toUpperCase());
							pathname.setEPart(date);
							pdc.fullName = pathname.getPathname();
							profile.setPdc(pdc);
						}
						temps.add(parts[1]);
						depths.add(parts[2]);

					}
					if (profile != null)
					{
						fillInProfilePdc(profile, temps, depths);
						writeProfileToDss(profile);
					}
				}
				catch (IOException | DataSetIllegalArgumentException e)
				{
					LOGGER.atWarning().withCause(e).log("Failed to read profiles file " +fileName);
				}
				catch (ParseException e)
				{
						String msg = "Failed to parse date " + date + "\nin file " + fileName;
						JOptionPane.showMessageDialog(_plotsPanel, msg, "Failed to load initial conditions",
								JOptionPane.ERROR_MESSAGE);
						LOGGER.atWarning().withCause(e).log(msg);
						break;
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
			}
		}
		return profiles;
	}

	private void writeProfileToDss(Profile profile)
	{
		PairedDataContainer pdc = profile.getPdc();
		int success = DssFileManagerImpl.getDssFileManager().write(pdc);
		if(success != 0)
		{
			LOGGER.atWarning().log("Failed to write " + pdc.fullName + " to " + pdc.fileName + ".  Error code: " + success);
		}
	}


	/**
	 * @param profile
	 * @param tempsList
	 * @param depthsList
	 */
	private void fillInProfilePdc(Profile profile, List<String> tempsList,
			List<String> depthsList)
	{
		try
		{
			profile.getPdc().setNumberCurves(1);
			profile.getPdc().setNumberOrdinates(depthsList.size());
			//these are always C and ft. In future csv file should provide units.
			profile.getPdc().xunits = DEFAULT_DEPTH_UNITS;
			profile.getPdc().xparameter = Parameter.getParameter(Parameter.PARAMID_DEPTH).getParameter();
			profile.getPdc().yparameter = Parameter.getParameter(Parameter.PARAMID_TEMP).getParameter();
			profile.getPdc().yunits = DEFAULT_TEMP_UNITS;
			double[] temps = toDoubleArray(tempsList);
			double[] depths = toDoubleArray(depthsList);
			double[][] temps2 = new double[1][0];
			temps2[0] = temps;
			profile.getPdc().setValues(depths, temps2);
			profile.getPdc().switchXyAxis = true;
		}
		catch (DataSetIllegalArgumentException e)
		{
			LOGGER.atWarning().withCause(e).log("Failed to get Parameter");
		}

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
	@Override
	protected void addListeners()
	{
		addUpperTableListeners();
	}

	@Override
	protected void clearPanel()
	{
		_plotsPanel.removeAll();
		_plotsPanel.revalidate();
		_plotsPanel.repaint();
	}

	@Override
	protected void removeData(ForecastSimGroup fsg, InitialConditions data)
	{
		//not currently supported
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
		_updateDataAction = new UpdateDataAction(() -> fillPanel(_fsg));
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
	
		_reviewDataAction = new ReviewDataAction();
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
		return _initialConditionsTable;
	}

	@Override
	protected void importForecastData(ImportForecastWindow dlg)
	{
		//no import button for initial conditions (read from mapping file)
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
			while (keyIter.hasNext())
			{
				resName = keyIter.next();
				comp = _resComponents.get(resName);
				Profile selectedProfile = findSelectedRow(comp.table);
				if ( selectedProfile != null )
				{
					ics.putSelectedProfile(resName, selectedProfile);
				}
			}
			ics.setModified(true);
			simGrp.setInitialConditions(ics);
		}

	}
	
	/**
	 * @param table
	 * @return
	 */
	private Profile findSelectedRow(RmaJTable table)
	{
		int rowCnt = table.getRowCount();
		Object obj;

		Profile profile = null;
		for (int r = 0;r < rowCnt; r++ )
		{
			obj = table.getValueAt(r, 0);
			if ( obj == Boolean.TRUE || "true".equalsIgnoreCase(obj.toString()))
			{
				profile = (Profile) table.getValueAt(r, 1);
				break;
			}
		}
		return profile;
	}
	/**
	 * @param fsg
	 */
	@Override
	public void fillPanel(ForecastSimGroup fsg)
	{
		setEnabled(fsg != null);
		clearTableSelections();
		if ( fsg != null )
		{
			_fsg = fsg;
			buildPlotsPanel();
			InitialConditions ic = fsg.getInitialConditions();
			Set<Entry<String, ResComponents>> entrySet = _resComponents.entrySet();
			Iterator<Entry<String, ResComponents>> iter = entrySet.iterator();
			Entry<String, ResComponents> entry;
			String resName;
			while (iter.hasNext())
			{
				entry = iter.next();
				resName = entry.getKey();
				Profile selectedProfile = ic.getSelectedProfile(resName);
				fillTableSelections(entry.getValue().table, selectedProfile);
				buildTablePlot(entry.getValue().table);
			}
			fillUpperInitialConditionsTable();
		}
		setModified(false);
	}

	private void fillUpperInitialConditionsTable()
	{
		InitialConditions initialConditions = _fsg.getInitialConditions();
		_initialConditionsTable.deleteCells();
		List<String> reservoirs = initialConditions.getReservoirs();
		for(String reservoir : reservoirs)
		{
			Profile profile = initialConditions.getSelectedProfile(reservoir);
			String displayValue = reservoir + " (" + profile + ")";
			if(tableHasProfileSelected(reservoir, profile))
			{
				_initialConditionsTable.appendRow(new Vector<>(Collections.singletonList(displayValue)));
			}
		}
	}

	private boolean tableHasProfileSelected(String reservoir, Profile profile)
	{
		boolean retVal = false;
		ResComponents resComponent = _resComponents.get(reservoir);
		if(resComponent != null)
		{
			for(int row=0; row< resComponent.table.getRowCount(); row++)
			{
				Object checked = resComponent.table.getValueAt(row, 0);
				Object date = resComponent.table.getValueAt(row, 1);
				if(checked != null && Boolean.parseBoolean(checked.toString())
					&& date != null && date.toString().equalsIgnoreCase(profile.getName()))
				{
					retVal = true;
					break;
				}
			}
		}
		return retVal;
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
	 * @param selectedProfile
	 */
	private void fillTableSelections(RmaJTable table,
									 Profile selectedProfile)
	{
		if ( selectedProfile == null )
		{
			return;
		}
		int rowCnt = table.getRowCount();
		Profile profile;
		for (int r = 0; r < rowCnt; r++ )
		{
			profile = (Profile) table.getValueAt(r, 1);
			if ( selectedProfile.getName().equalsIgnoreCase(profile.getName()))
			{
				table.setValueAt(Boolean.TRUE, r, 0);
				Rectangle cellRect = table.getCellRect(r, 0, true);
				table.scrollRectToVisible(cellRect);
				break;
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
