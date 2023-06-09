/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JButton;

import com.google.common.flogger.FluentLogger;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;
import usbr.wat.plugins.actionpanel.ui.MetPlotPanel;

/**
 * @author mark
 *
 */
public class MeteorologyPanel extends AbstractForecastPanel
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String CONFIG_FILE = ForecastConfigFiles.getRelativeMetEditorFile();

	private RmaJTable _metInfoTable;
	private JButton _importButton;
	private MetPlotPanel _plotPanel;
	private ForecastSimGroup _fsg;

	/**
	 * @param forecastPanel
	 */
	public MeteorologyPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
	}

	

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Met Forecast Name", "Type", "Description", "Forecast Date"};

		_metInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() *1;
				return d;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return false;
			}
		};
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.01;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_metInfoTable.getScrollPane(), gbc);

		_importButton = new JButton("Import...");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_importButton, gbc);
		
		_plotPanel = new MetPlotPanel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_plotPanel, gbc);
		
		

	}
	
	/**
	 * 
	 */
	protected void addListeners()
	{
		super.addListeners();
		_importButton.addActionListener(e->displayImportDataWindow());
		getTableForPanel().getSelectionModel().addListSelectionListener(e->tableRowSelected(_metTable.getSelectedRow()));
	}

	/**
	 * @return
	 */
	private void displayImportDataWindow()
	{
		ImportMetDataWindow dlg = new ImportMetDataWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(_fsg);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		List<MeteorlogicData> metData = dlg.getMetData();
		for (int i = 0;i <metData.size(); i++ )
		{
			ForecastTable metTable = getTableForPanel();
			Vector<MeteorlogicData> row = new Vector<>();
			row.add(metData.get(i));
			metTable.appendRow(row);
			_fsg.getMeteorlogyData().add(metData.get(i));
			_fsg.setModified(true);
		}

	}



	@Override
	public ForecastTable getTableForPanel()
	{
		return _metTable;
	}

	@Override
	protected void savePanel()
	{
		if ( _fsg != null )
		{
			List<MeteorlogicData> metDataList = new ArrayList<>();
			ForecastTable table = getTableForPanel();
			int numRows = table.getNumRows();
			MeteorlogicData metData;
			for (int r = 0; r < numRows; r++)
			{
				metData = (MeteorlogicData) table.getValueAt(r, 0);
				metDataList.add(metData);
			}
			_fsg.setMeteorlogyData(metDataList);
		}
	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		setEnabled(fsg != null);
		_fsg = fsg;
		getTableForPanel().deleteCells();
		_plotPanel.setEnabled(false);
		if ( _fsg != null )
		{
			List<MeteorlogicData> data = _fsg.getMeteorlogyData();
			_metTable.deleteCells();
			Vector<MeteorlogicData> row;
			for (int i = 0; i < data.size(); i++ )
			{
				row = new Vector<>();
				row.add(data.get(i));
				_metTable.appendRow(row);
			}
			fillNavPanel();
		}
	}

	/**
	 * TODO only supports ResSim model alternatives right now
	 */
	private void fillNavPanel()
	{
		_plotPanel.setLocationList(null);
		Project prj = Project.getCurrentProject();

		String prjDir = prj.getProjectDirectory();
		String configPath = RMAIO.concatPath(prjDir, CONFIG_FILE);
		RmaFile configFile = FileManagerImpl.getFileManager().getFile(configPath);
		if ( configFile == null )
		{
			return;
		}
		BufferedReader reader = configFile.getBufferedReader();

		if ( reader == null )
		{
			return;
		}
		String line;
		try
		{
			//Met Station Location, Met parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record
			reader.readLine(); // skip first line
			Map<String, MetLocation> locationInfo = new HashMap<>();
			String name;
			while ((line = reader.readLine()) != null)
			{
				String[] metInfoArray = line.split(",");
				if ( metInfoArray == null || metInfoArray.length == 0 )
				{
					continue;
				}
				MetLocation metLoc = locationInfo.get(metInfoArray[0]);
				if ( metLoc == null )
				{
					metLoc = new MetLocation();
					name = metInfoArray[0].trim();
					metLoc.setName(name);
					locationInfo.put(name, metLoc);
				}
				DssLocation dssLoc = new DssLocation(metInfoArray[1].trim(), metInfoArray[2].trim(), metInfoArray[3].trim());
				metLoc.addDssLocation(dssLoc);
			}
			_plotPanel.setLocationList(locationInfo.values().stream().collect(Collectors.toList()));
		}
		catch ( IOException ioe)
		{
			LOGGER.atWarning().withCause(ioe).log("Error reading file "+configFile.getAbsolutePath());
		}
		finally
		{
			try
			{
				reader.close();
			} catch (IOException e)
			{
			}
		}

	}




	@Override
	protected void tableRowSelected(int selRow)
	{
		if ( _fsg != null )
		{
			ForecastTable table = getTableForPanel();
			_metInfoTable.deleteCells();
			if (selRow > -1)
			{
				MeteorlogicData metData = (MeteorlogicData) table.getValueAt(selRow, 0);
				Vector row = new Vector();
				row.add(metData.getName());
				row.add(metData.getMetDataType());
				row.add(metData.getDescription());

				_metInfoTable.appendRow(row);
				_plotPanel.setYear(metData.getYear());
				_plotPanel.setEnabled(true);
			}
			else
			{
				_plotPanel.setEnabled(false);
			}
			_plotPanel.fillPlotPanel();
		}
	}

	@Override
	public void tableRowDeleteClicked(int selectedRow)
	{
		//TODO
	}

}
