/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import hec.heclib.dss.DSSPathname;
import hec.hecmath.HecMath;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.io.impl.StoreOptionImpl;
import hec.model.RunTimeWindow;
import hec2.wat.model.WatAnalysisPeriod;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import rma.swing.table.RmaTableModel;
import rma.util.RMAConst;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetTimeStep;
import usbr.wat.plugins.actionpanel.ui.forecast.AbstractForecastPanel;
import usbr.wat.plugins.actionpanel.ui.forecast.ForecastPanel;

/**
 * @author mark
 *
 */
public class TempTargetPanel extends AbstractForecastPanel
{
	private static final Logger LOGGER = Logger.getLogger(TempTargetPanel.class.getName());
	private RmaJTable _ttInfoTable;
	private JButton _createButton;
	private RmaJTable _ttTable;
	private TempTargetTableModel _ttTableModel;
	private TemperatureTargetSet _selectedTempTargetSet;
	private int _topTableRowSelected;
	private ForecastSimGroup _fsg;

	/**
	 * @param forecastPanel - parent forecast panel
	 */
	public TempTargetPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
		addPanelListeners();
	}

	private void addPanelListeners()
	{
		_createButton.addActionListener(e -> new TempTargetImportDialog(SwingUtilities.getWindowAncestor(this), getExistingSetNames(), new TempTargetConsumer(this)));
	}

	private List<String> getExistingSetNames()
	{
		List<String> retVal = new ArrayList<>();
		for(int row =0; row < _tempTargetTable.getRowCount(); row++)
		{
			Object val = _tempTargetTable.getValueAt(row, 0);
			if (val != null && !val.toString().trim().isEmpty())
			{
				retVal.add(val.toString());
			}
		}
		return retVal;
	}

	void tempTargetSetsSelected(List<TemperatureTargetSet> tempTargetSets) throws TempTargetSaveFailedException
	{
		RmaTableModel upperTableModel = (RmaTableModel) getTableForPanel().getModel();
		initializeSaveOfNewTempTargets(tempTargetSets);
		for(TemperatureTargetSet tempTargetSet : tempTargetSets)
		{
			Integer rowThatContainsName = getRowThatContainsName(upperTableModel, tempTargetSet);
			if(rowThatContainsName != null)
			{
				upperTableModel.insertRow(rowThatContainsName, new Vector<>(Collections.singletonList(tempTargetSet)));
				upperTableModel.deleteRow(rowThatContainsName + 1);
			}
			else
			{
				upperTableModel.addRow(new Vector<>(Collections.singletonList(tempTargetSet)));
			}
		}
		if(!tempTargetSets.isEmpty())
		{
			TemperatureTargetSet selectedSet = tempTargetSets.get(tempTargetSets.size() - 1);
			_selectedTempTargetSet = selectedSet;
			fillTempTargetInfoTable(selectedSet);
			fillTempTargetTable(selectedSet);
		}
	}

	private void initializeSaveOfNewTempTargets(List<TemperatureTargetSet> tempTargetSets) throws TempTargetSaveFailedException
	{
		if(_fsg != null && tempTargetSets != null && !tempTargetSets.isEmpty())
		{
			LinkedHashSet<TemperatureTargetSet> sets = new LinkedHashSet<>(_fsg.getTemperatureTargetSets());
			for (TemperatureTargetSet set : tempTargetSets)
			{
				List<DSSPathname> pathNames = saveImported(set, _fsg);
				set.setDssPathNames(pathNames);
				sets.add(set);
			}

			_fsg.setTemperatureTargetSets(new ArrayList<>(sets));
		}
	}

	private void fillTempTargetInfoTable(TemperatureTargetSet tempTargetSet)
	{
		_ttInfoTable.commitEdit(true);
		_ttInfoTable.setValueAt(tempTargetSet, 0, 0);
		_ttInfoTable.setValueAt(tempTargetSet.getDescription(), 0, 1);
		_ttInfoTable.setColumnEnabled(false, 0);
		_ttInfoTable.setColumnEnabled(false, 1);
		_ttInfoTable.setColumnEnabled(false, 2);
	}

	private Integer getRowThatContainsName(RmaTableModel tableModel, TemperatureTargetSet tempTargetSet)
	{
		Integer retVal = null;
		for(int row =0; row < tableModel.getRowCount(); row++)
		{
			Object val = tableModel.getValueAt(row, 0);
			if(val != null && val.toString().equalsIgnoreCase(tempTargetSet.getName()))
			{
				retVal = row;
				break;
			}
		}
		return retVal;
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Temperature Target Set", "Description"};

		_ttInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * 2;
				return d;
			}
		};
		_ttInfoTable.getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.06;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_ttInfoTable.getScrollPane(), gbc);

		_createButton = new JButton("Import/Create T.T. Set...");
		_createButton.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);
		
		_ttTable = new RmaJTable(this, new String[] {"Date",""});
		_ttTableModel = new TempTargetTableModel();
		_ttTable.setModel(_ttTableModel);
		_ttTable.removePopupMenuRowEditingOptions();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_ttTable.getScrollPane(), gbc);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _tempTargetTable;
	}

	@Override
	protected void savePanel()
	{
		ForecastSimGroup simGrp = _forecastPanel.getSimulationGroup();
		if(simGrp != null && simGrp.equals(_fsg) && _selectedTempTargetSet != null && _ttTable.getRowCount() > 0)
		{
			_ttInfoTable.commitEdit(true);
			List<TemperatureTargetSet> sets = new ArrayList<>(simGrp.getTemperatureTargetSets());
			if(!sets.contains(_selectedTempTargetSet))
			{
				sets.add(_selectedTempTargetSet);
			}
			if(_selectedTempTargetSet.isUserDefined() && _ttTable.isModified())
			{
				List<DSSPathname> pathNames = null;
				try
				{
					_selectedTempTargetSet.setModified(true);
					pathNames = saveUserDefinedTable(_selectedTempTargetSet, simGrp);
					_ttTable.setModified(false);
				}
				catch (TempTargetSaveFailedException e)
				{
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"DSS Write Failed", JOptionPane.ERROR_MESSAGE);
					LOGGER.log(Level.CONFIG, e, () -> "Temp Target save failed: " + e.getMessage());
				}
				_selectedTempTargetSet.setDssPathNames(pathNames);
			}
			updateSetName();
			updateDescription();
			simGrp.setTemperatureTargetSets(sets);
		}
		else if (simGrp == null || (_ttTable.getRowCount() <1))
		{
			clearPanel();
		}
	}

	private void updateSetName()
	{
		Object val = _ttInfoTable.getValueAt(0, 0);
		if(val != null && !val.toString().trim().isEmpty() && !val.toString().trim().equalsIgnoreCase(_selectedTempTargetSet.getName()))
		{
			String name = val.toString().trim();
			updateNameInSimGroupList(name);
			_selectedTempTargetSet.setName(name);
			((TempTargetForecastTableModel)_tempTargetTable.getModel()).updateName(name, _topTableRowSelected);
		}
	}

	private void updateDescription()
	{
		Object val = _ttInfoTable.getValueAt(0, 1);
		if(val != null && !val.toString().trim().equalsIgnoreCase(_selectedTempTargetSet.getDescription()))
		{
			String desc = val.toString().trim();
			updateDescInSimGroupList(desc);
			_selectedTempTargetSet.setDescription(desc);
			((TempTargetForecastTableModel)_tempTargetTable.getModel()).updatedDescription(desc, _topTableRowSelected);
		}
	}

	private void updateNameInSimGroupList(String name)
	{
		List<TemperatureTargetSet> sets = _fsg.getTemperatureTargetSets();
		for(TemperatureTargetSet set : sets)
		{
			if(set.equals(_selectedTempTargetSet))
			{
				set.setName(name);
			}
		}
	}

	private void updateDescInSimGroupList(String desc)
	{
		List<TemperatureTargetSet> sets = _fsg.getTemperatureTargetSets();
		for(TemperatureTargetSet set : sets)
		{
			if(set.equals(_selectedTempTargetSet))
			{
				set.setDescription(desc);
			}
		}
	}

	private List<DSSPathname> saveImported(TemperatureTargetSet tempTargetSet, ForecastSimGroup simGrp) throws TempTargetSaveFailedException
	{
		List<DSSPathname> retVal = new ArrayList<>();
		WatAnalysisPeriod analysisPeriod = simGrp.getAnalysisPeriod();
		if(analysisPeriod != null && analysisPeriod.getRunTimeWindow() != null)
		{
			List<TimeSeriesContainer> timeSeriesData = tempTargetSet.getTimeSeriesData(analysisPeriod.getRunTimeWindow());
			String forecastSimGroupDirectory = getSimGroupDirectory(simGrp);
			String delim = "/";
			String fileName = forecastSimGroupDirectory + delim + tempTargetSet.getName() +".dss";
			for(TimeSeriesContainer tsc : timeSeriesData)
			{
				tsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
				DSSPathname pathname = new DSSPathname(tsc.fullName);
				pathname.setDPart("");
				tsc.fullName = pathname.getPathname();
				retVal.add(pathname);
				saveTimeSeries(tsc, fileName);
				tempTargetSet.setDssOutputPath(Paths.get(fileName));
			}
		}
		else
		{
			throw new TempTargetSaveFailedException("Analysis Period is not set for simulation!");
		}
		return retVal;
	}

	private String getSimGroupDirectory(ForecastSimGroup simGrp)
	{
		String forecastSimGroupDirectory = "forecast/simGroups/" + simGrp.getName();
		try
		{
			Path newDssFilesDirectory = Paths.get(Project.getCurrentProject().getAbsolutePath(forecastSimGroupDirectory));
			Files.createDirectories(newDssFilesDirectory);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + forecastSimGroupDirectory + " directories");
		}
		return forecastSimGroupDirectory;
	}

	private List<DSSPathname> saveUserDefinedTable(TemperatureTargetSet tempTargetSet, ForecastSimGroup simGrp) throws TempTargetSaveFailedException
	{
		List<DSSPathname> retVal = new ArrayList<>();
		int[] times = new int[_ttTableModel.getRowCount()];
		String forecastSimGroupDirectory = getSimGroupDirectory(simGrp);
		String delim = "/";
		String fileName = forecastSimGroupDirectory + delim + tempTargetSet.getName() +".dss";
		tempTargetSet.setDssSourcePath(Paths.get(fileName));
		for(int row=0; row < _ttTableModel.getRowCount(); row++)
		{
			TempTargetRowData rowData = _ttTableModel.getTempTargetRowData(row);
			times[row] = rowData.getTime();
		}
		for(int col=1; col < _ttTableModel.getColumnCount(); col++)
		{
			tempTargetSet.getTimeSeriesData(_fsg.getAnalysisPeriod().getRunTimeWindow());
			TimeSeriesContainer tsc = TemperatureTargetSet.buildTemplateUserDefinedTSContainer(col);
			tsc.startTime = times[0];
			tsc.endTime = times[times.length-1];
			tsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
			tsc.times = times;
			tsc.values = getUserDefinedValues(col);
			tsc.numberValues = tsc.values.length;
			tsc.startHecTime = tsc.getHecTime(0);
			tsc.endHecTime = tsc.getHecTime(times.length-1);
			saveTimeSeries(tsc, fileName);
			tempTargetSet.setDssOutputPath(Paths.get(Project.getCurrentProject().getRelativePath(fileName)));
			tempTargetSet.setDssSourcePath(Paths.get(Project.getCurrentProject().getRelativePath(fileName)));
			retVal.add(new DSSPathname(tsc.fullName));
		}
		return retVal;
	}

	private void saveTimeSeries(TimeSeriesContainer weeklyTsc, String fileName) throws TempTargetSaveFailedException
	{
		TimeSeriesContainer hourlyTsc = null;
		weeklyTsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
		try
		{
			if(!weeklyTsc.allMissing())
			{
				TimeSeriesMath timeSeriesMath = new TimeSeriesMath(weeklyTsc);
				HecMath hecMath = timeSeriesMath.interpolateDataAtRegularInterval(TemperatureTargetTimeStep.REGULAR_HOURLY.toString(), "0M");
				hourlyTsc = (TimeSeriesContainer) hecMath.getData();
				hourlyTsc.startTime = hourlyTsc.times[0];
				hourlyTsc.startHecTime = hourlyTsc.getHecTime(0);
				hourlyTsc.endTime = hourlyTsc.times[hourlyTsc.times.length - 1];
				hourlyTsc.endHecTime = hourlyTsc.getHecTime(hourlyTsc.numberValues-1);
			}
			else
			{
				hourlyTsc = new TimeSeriesContainer();
			}
			hourlyTsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
			DSSPathname pathname = new DSSPathname(weeklyTsc.fullName);
			pathname.setDPart("");
			pathname.setEPart(TemperatureTargetTimeStep.REGULAR_HOURLY.toString());
			hourlyTsc.fullName = pathname.getPathname();
			int hourlyStatus = DssFileManagerImpl.getDssFileManager().write(hourlyTsc);
			int weeklyStatus = DssFileManagerImpl.getDssFileManager().writeTS(weeklyTsc, new StoreOptionImpl());
			String errorSpecified = "";
			String statusCode = "";
			if(hourlyStatus != 0 && !hourlyTsc.allMissing())
			{
				errorSpecified += hourlyTsc.fullName;
				statusCode = String.valueOf(hourlyStatus);
				if(weeklyStatus != 0)
				{
					errorSpecified += " and " + weeklyTsc.fullName;
					statusCode = String.valueOf(weeklyStatus);
				}
			}
			else if (weeklyStatus != 0 && !weeklyTsc.allMissing())
			{
				errorSpecified = weeklyTsc.fullName;
				statusCode = String.valueOf(weeklyStatus);
			}
			if(!errorSpecified.isEmpty())
			{
				throw new TempTargetSaveFailedException(errorSpecified, weeklyTsc.fileName, statusCode);
			}

		}
		catch (HecMathException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Failed to convert timeseries: " + weeklyTsc.fullName + " to hourly");
		}
		finally
		{
			DssFileManagerImpl.getDssFileManager().close(weeklyTsc.fileName);
			if(hourlyTsc != null)
			{
				DssFileManagerImpl.getDssFileManager().close(hourlyTsc.fileName);
			}
		}
	}

	private double[] getUserDefinedValues(int col)
	{
		double[] retVal = new double[_ttTableModel.getRowCount()];
		for(int row=0; row < _ttTableModel.getRowCount(); row++)
		{
			Object val = _ttTableModel.getValueAt(row, col);
			double doubleVal = RMAConst.HEC_UNDEFINED_DOUBLE;
			if(val != null)
			{
				doubleVal = Double.parseDouble(val.toString());
			}
			retVal[row] = doubleVal;
		}
		return retVal;
	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		if ( fsg != null )
		{
			clearPanel();
			_selectedTempTargetSet = null;
			_fsg = fsg;
			List<TemperatureTargetSet> tempTargetSets = fsg.getTemperatureTargetSets();
			((TempTargetForecastTableModel)_tempTargetTable.getModel()).clearTempTargets();
			List<TemperatureTargetSet> temperatureTargetSets = new ArrayList<>(tempTargetSets);
			for(TemperatureTargetSet set : temperatureTargetSets)
			{
				((TempTargetForecastTableModel)_tempTargetTable.getModel()).addRow(new Vector<>(Collections.singletonList(set)));
			}
		}

		_createButton.setEnabled(fsg != null);
		
	}

	private void fillTempTargetTable(TemperatureTargetSet temperatureTargetSet)
	{
		setEnabled(true);
		_selectedTempTargetSet = temperatureTargetSet;
		removeAllColumns();
		_ttTableModel = new TempTargetTableModel();
		_ttTable.setModel(_ttTableModel);
		_ttTableModel.addColumn("Date");
		RunTimeWindow analysisTimeWindow = _fsg.getAnalysisPeriod().getRunTimeWindow();
		List<TimeSeriesContainer> temperatureTargetData = temperatureTargetSet.getTimeSeriesData(analysisTimeWindow);
		for(int column = 1; column <= temperatureTargetData.size(); column++)
		{
			String columnName = getColumnNameFromFPart(temperatureTargetData.get(column - 1));
			if(columnName == null || columnName.trim().isEmpty())
			{
				columnName = "" + column;
			}
			_ttTableModel.addColumn(columnName);
			_ttTableModel.setColEnabled(temperatureTargetSet.isUserDefined(), column);
		}
		_ttTable.setColumnEnabled(false, TempTargetTableModel.DATE_COL_INDEX);
		for(int col=1; col < _ttTableModel.getColumnCount(); col++)
		{
			_ttTable.setDoubleCellEditor(col);
		}
		TableColumn dateColumn = _ttTable.getColumnModel().getColumn(TempTargetTableModel.DATE_COL_INDEX);
		dateColumn.setMaxWidth(50);
		_ttTableModel.setTempTargetSet(temperatureTargetSet, _fsg);
		_ttTableModel.fireTableStructureChanged();
	}

	private String getColumnNameFromFPart(TimeSeriesContainer timeSeriesContainer)
	{
		DSSPathname pathname = new DSSPathname(timeSeriesContainer.fullName);
		String fPart = pathname.getFPart();
		String retVal = fPart;
		if(fPart != null && fPart.contains("|"))
		{
			String[] split = fPart.split("\\|");
			if(split.length > 1)
			{
				String indexNum = split[0];
				indexNum = indexNum.replace("C:", "");
				String replaceRegex = "^0+(?!$)";
				retVal = indexNum.replaceFirst(replaceRegex, "");
			}
		}
		return retVal;
	}

	private void removeAllColumns()
	{
		TableColumnModel columnModel = _ttTable.getColumnModel();
		for(int col = columnModel.getColumnCount()-1; col >=0; col--)
		{
			columnModel.removeColumn(columnModel.getColumn(col));
		}
	}

	@Override
	protected void tableRowSelected(int row)
	{
		ForecastTable table = getTableForPanel();
		if ( row == -1 )
		{
			clearPanel();
		}
		else
		{
			Object value = table.getValueAt(row, 0);
			if(value == null)
			{
				clearPanel();
			}
			else
			{
				Optional<TemperatureTargetSet> setOptional = ((TempTargetForecastTableModel) table.getModel()).getTemperatureTargetSetByName(value.toString());
				if(setOptional.isPresent())
				{
					TemperatureTargetSet set = setOptional.get();
					fillTempTargetInfoTable(set);
					fillTempTargetTable(set);
					set.setModified(false);
				}
			}

		}
		_topTableRowSelected = row;
		setModified(false);
	}

	private void clearPanel()
	{
		_ttTable.clearAll();
		removeAllColumns();
		_ttInfoTable.commitEdit(true);
		_ttInfoTable.setValueAt("", 0,0);
	}

}
