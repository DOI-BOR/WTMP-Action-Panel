/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import com.rma.model.Project;
import hec2.wat.model.WatAnalysisPeriod;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.BcData;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;
import usbr.wat.plugins.actionpanel.ui.NavPlotPanel;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class BcPanel extends AbstractForecastPanel
{
	private static final Logger LOGGER = Logger.getLogger(BcPanel.class.getName());
	private RmaJTable _bcInfoTable;
	private JButton _createButton;
	private NavPlotPanel _plotPanel;
	private ForecastSimGroup _fsg;

	/**
	 * @param forecastPanel
	 */
	public BcPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		
		String[] headers = new String[] {"Boundary Condition Set"};

		_bcInfoTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() *1;
				return d;
			}
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
		lowerPanel.add(_bcInfoTable.getScrollPane(), gbc);

		_createButton = new JButton("Create B.C. Sets...");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_createButton, gbc);
		
		_plotPanel = new NavPlotPanel();
		_plotPanel.getPlotPanel().buildDefaultComponents();
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

	@Override
	protected void addListeners()
	{
		super.addListeners();
		_createButton.addActionListener(e->createBcAction());
		getTableForPanel().getSelectionModel().addListSelectionListener(e->tableRowSelected());
	}

	private void createBcAction()
	{
		CreateBcWindow dlg = new CreateBcWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(_fsg);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		List<BcData> bcDataList = dlg.getBcData();
		ForecastTable bcTable = getTableForPanel();
		try
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			WatAnalysisPeriod analysisPeriod = _fsg.getAnalysisPeriod();
			if(analysisPeriod == null)
			{
				JOptionPane.showMessageDialog(this, "Failed to find analysis period for simulation: "
						+ _fsg.getName(), "Analysis Period Not Found", JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				applyScriptToBCDataWithAnalysisPeriod(bcDataList, bcTable);
			}
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
		_fsg.setModified(true);

	}

	private void applyScriptToBCDataWithAnalysisPeriod(List<BcData> bcDataList, ForecastTable bcTable)
	{
		Path scriptFile = Paths.get("forecast/scripts/BoundaryConditionScript.py");
		if(!Paths.get(Project.getCurrentProject().getAbsolutePath(scriptFile.toString())).toFile().exists())
		{
			JOptionPane.showMessageDialog(this, "Failed to find script file: \n"
					+ Project.getCurrentProject().getAbsolutePath(scriptFile.toString()), "Script Not Found", JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			runScriptOnBCDataList(bcDataList, bcTable, scriptFile);
		}
	}

	private void runScriptOnBCDataList(List<BcData> bcDataList, ForecastTable bcTable, Path scriptFile)
	{
		for (BcData bcData : bcDataList)
		{
			runScript(bcData, scriptFile);
			Vector<BcData> row = new Vector<>();
			row.add(bcData);
			bcTable.appendRow(row);
			_fsg.getBcData().add(bcData);
		}
	}

	private void runScript(BcData bcData, Path scriptFile)
	{
		createScriptsDir();
		WatAnalysisPeriod analysisPeriod = _fsg.getAnalysisPeriod();
		if(analysisPeriod != null)
		{
			OperationsData opsData = _fsg.getOperationsData().stream()
					.filter(ops -> ops.getName().equalsIgnoreCase(bcData.getOpsDataName()))
					.findFirst()
					.orElse(null);
			MeteorlogicData metData = _fsg.getMeteorlogyData().stream()
					.filter(met -> met.getName().equalsIgnoreCase(bcData.getMetDataName()))
					.findFirst()
					.orElse(null);
			if(opsData != null && metData != null)
			{
				int targetYear = analysisPeriod.getRunTimeWindow().getStartTime().year();
				String bcFPart = bcData.getName();
				String bcOutputDssFile = Project.getCurrentProject().getAbsolutePath("forecast/" + _fsg.getName() + "/bc.dss");
				String opsFileName = opsData.getOperationsFile();
				String dssMapFile = Project.getCurrentProject().getAbsolutePath("forecast/" + _fsg.getName() + "/" + bcData.getName() + ".txt");
				int positionAnalysisYear = metData.getYear();
				String positionalAnalysisConfigFile = Project.getCurrentProject().getAbsolutePath("shared/config/historical_met.config");
				String metFPart = bcFPart;
				String metOutputDssFileName = bcOutputDssFile;
				String opsImportFPart = bcFPart;
				String flowPatternConfigFile = Project.getCurrentProject().getAbsolutePath("shared/config/flow_pattern.config");
				Integer result = PythonScriptUtil.runScript(scriptFile, "build_BC_data_sets", Integer.class,
						targetYear, bcFPart, bcOutputDssFile, opsFileName, dssMapFile, positionAnalysisYear, positionalAnalysisConfigFile,
						metFPart, metOutputDssFileName, flowPatternConfigFile, opsImportFPart);
				LOGGER.log(Level.CONFIG, () -> "Result from " + scriptFile + ": " + result);
			}
		}
	}

	private void createScriptsDir()
	{
		String forecastSimGroupDirectory = "forecast/" + _fsg.getName();
		String scriptsDir = "forecast/scripts";
		try
		{
			Path absSimGroupDirectory = Paths.get(Project.getCurrentProject().getAbsolutePath(forecastSimGroupDirectory));
			Files.createDirectories(absSimGroupDirectory);
			Path absScriptDir = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptsDir));
			Files.createDirectories(absScriptDir);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + forecastSimGroupDirectory + " directories");
		}
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _bcTable;
	}

	@Override
	protected void savePanel()
	{

	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		setEnabled(fsg != null);
		_fsg = fsg;
		ForecastTable table = getTableForPanel();
		table.deleteCells();
		_plotPanel.setEnabled(false);
		if ( _fsg != null )
		{
			List<BcData> data = _fsg.getBcData();
			Vector<BcData> row;
			for (int i = 0; i < data.size(); i++ )
			{
				row = new Vector<>();
				row.add(data.get(i));
				table.appendRow(row);
			}
			fillNavPanel();
		}
	}

	private void fillNavPanel()
	{
	}

	@Override
	protected void tableRowSelected()
	{
		if ( _fsg != null )
		{
			ForecastTable table = getTableForPanel();
			int selRow = table.getSelectedRow();
			_bcInfoTable.deleteCells();
			if (selRow > -1)
			{
				BcData bcData = (BcData) table.getValueAt(selRow, 0);
				Vector row = new Vector();
				row.add(bcData.getName());

				_bcInfoTable.appendRow(row);
			}
		}
	}

}
