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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import hec.data.Units;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecTimeSeriesBase;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import rma.swing.table.RmaCellEditor;
import rma.swing.table.RmaTableModel;
import rma.util.RMAConst;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;
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
		_createButton.addActionListener(e -> new TempTargetImportDialog(SwingUtilities.getWindowAncestor(this), getExistingSetNames(), this::tempTargetSetSelected));
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

	private void tempTargetSetSelected(TemperatureTargetSet tempTargetSet)
	{
		RmaTableModel tableModel = (RmaTableModel) getTableForPanel().getModel();
		Integer rowThatContainsName = getRowThatContainsName(tableModel, tempTargetSet);
		if(rowThatContainsName != null)
		{
			tableModel.insertRow(rowThatContainsName, new Vector<>(Collections.singletonList(tempTargetSet)));
			tableModel.deleteRow(rowThatContainsName + 1);
		}
		else
		{
			tableModel.addRow(new Vector<>(Collections.singletonList(tempTargetSet)));
		}
		fillTempTargetInfoTable(tempTargetSet);
		fillTempTargetTable(tempTargetSet);
	}

	private void fillTempTargetInfoTable(TemperatureTargetSet tempTargetSet)
	{
		_ttInfoTable.setValueAt(tempTargetSet, 0, 0);
		_ttInfoTable.setColumnEnabled(false, 0);
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
		String[] headers = new String[] {"Temperature Target Set"};

		_ttInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight();
				return d;
			}
		};
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.05;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_ttInfoTable.getScrollPane(), gbc);

		_createButton = new JButton("Import/Create T.T. Set...");
		_createButton.setEnabled(true);
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
	public void setVisible(boolean visible)
	{
		if(visible)
		{
			_createButton.setEnabled(true);
		}
		super.setVisible(visible);
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
		if(simGrp != null && _selectedTempTargetSet != null)
		{
			List<TemperatureTargetSet> sets = new ArrayList<>(simGrp.getTemperatureTargetSets());
			if(!sets.contains(_selectedTempTargetSet))
			{
				sets.add(_selectedTempTargetSet);
			}
			if(_selectedTempTargetSet.isUserDefined())
			{
				_selectedTempTargetSet.setDssPathNames(saveUserDefinedTable(_selectedTempTargetSet, simGrp));
			}
			updateSetName();
			simGrp.setTemperatureTargetSets(sets);
		}
	}

	private void updateSetName()
	{
		Object val = _ttInfoTable.getValueAt(0, 0);
		if(val != null && !val.toString().trim().isEmpty() && !val.toString().trim().equalsIgnoreCase(_selectedTempTargetSet.getName()))
		{
			String name = val.toString().trim();
			_fsg.getTemperatureTargetSets().stream().filter(tt -> tt.equals(_selectedTempTargetSet))
					.findFirst()
					.ifPresent(tt -> tt.setName(name));
			_selectedTempTargetSet.setName(name);
			((TempTargetForecastTableModel)_tempTargetTable.getModel()).updateName(name, _topTableRowSelected);
		}
	}

	private List<DSSPathname> saveUserDefinedTable(TemperatureTargetSet tempTargetSet, ForecastSimGroup simGrp)
	{
		List<DSSPathname> retVal = new ArrayList<>();
		int[] times = new int[_ttTableModel.getRowCount()];
		String forecastSimGroupDirectory = "forecastData/" + simGrp.getName();
		try
		{
			Path newDssFilesDirectory = Paths.get(Project.getCurrentProject().getAbsolutePath(forecastSimGroupDirectory));
			Files.createDirectories(newDssFilesDirectory);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + forecastSimGroupDirectory + " directories");
		}
		String delim = "/";
		String fileName = forecastSimGroupDirectory + delim + tempTargetSet.getName() +".dss";
		tempTargetSet.setFilePath(Paths.get(fileName));
		for(int row=0; row < _ttTableModel.getRowCount(); row++)
		{
			TempTargetRowData rowData = _ttTableModel.getTempTargetRowData(row);
			LocalDate date = rowData.getDate();
			LocalTime localTime = LocalTime.of(0, 1);
			ZoneId zoneId = ZoneId.systemDefault();
			times[row] = HecTime.fromZonedDateTime(ZonedDateTime.of(date, localTime, zoneId)).value();
		}
		for(int col=1; col < _ttTableModel.getColumnCount(); col++)
		{
			TimeSeriesContainer tsc = new TimeSeriesContainer();
			DSSPathname pathname = new DSSPathname();
			pathname.setBPart(tempTargetSet.getName());
			pathname.setCPart("TEMP-WATER-TARGET");
			pathname.setEPart("IR-MONTH");
			pathname.setFPart("C:00000" + col + "|USER-DEFINED");
			tsc.fullName = pathname.getPathname();
			ZoneId dataZoneId = ZoneId.systemDefault();
			tsc.setTimeZoneID(dataZoneId.getId());
			tsc.locationTimezone = dataZoneId.getId();
			tsc.units = Units.getBestMatch("C");
			tsc.interval = HecTimeSeriesBase.getIntervalFromEPart(pathname.getEPart());
			tsc.type = "INST-VAL";
			tsc.parameter = pathname.getCPart();
			tsc.location = pathname.bPart();
			tsc.version = pathname.fPart();
			tsc.startTime = times[0];
			tsc.endTime = times[times.length-1];
			tsc.fileName = Project.getCurrentProject().getAbsolutePath(fileName);
			tsc.setStoreAsDoubles(true);
			tsc.times = times;
			tsc.values = getUserDefinedValues(col);
			tsc.numberValues = tsc.values.length;
			saveUserDefinedTimeSeries(tsc);
			retVal.add(pathname);
		}
		return retVal;
	}

	private void saveUserDefinedTimeSeries(TimeSeriesContainer tsc)
	{
		try
		{
			DssFileManagerImpl.getDssFileManager().write(tsc);
		}
		finally
		{
			DssFileManagerImpl.getDssFileManager().close(tsc.fileName);
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
			_fsg = fsg;
			List<TemperatureTargetSet> tempTargetSets = fsg.getTemperatureTargetSets();
			for(int row = _tempTargetTable.getRowCount()-1; row >=0; row--)
			{
				((TempTargetForecastTableModel)_tempTargetTable.getModel()).deleteRow(row);
			}
			if(!tempTargetSets.isEmpty())
			{
				for(TemperatureTargetSet set : tempTargetSets)
				{
					((TempTargetForecastTableModel)_tempTargetTable.getModel()).addRow(new Vector<>(Collections.singletonList(set)));
				}
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
		List<TimeSeriesContainer> temperatureTargetData = temperatureTargetSet.getTimeSeriesData();
		for(int column = 1; column <= temperatureTargetData.size(); column++)
		{
			String columnName = getColumnNameFromFPart(temperatureTargetData.get(column - 1));
			if(columnName == null)
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
		_ttTableModel.setTempTargetSet(temperatureTargetSet);
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
				retVal = indexNum.replaceFirst("^0+(?!$)", "");
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
	protected void tableRowSelected()
	{
		ForecastTable table = getTableForPanel();
		int row = table.getSelectedRow();
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
				}
			}

		}
		_topTableRowSelected = row;
	}

	private void clearPanel()
	{
		_ttTable.clearAll();
		removeAllColumns();
		_ttInfoTable.setValueAt("", 0,0);
	}

}
