/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import hec.geometry.Axis;
import hec.gfx2d.G2dObject;
import hec.gfx2d.G2dPanel;

import hec.gfx2d.TimeSeriesDataSet;
import hec.gfx2d.Viewport;
import hec.heclib.dss.DSSPathname;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaNavigationPanel;
import usbr.wat.plugins.actionpanel.model.forecast.BcData;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.forecast.BoundaryConditionLocationPair;

/**
 * @author mark
 *
 */
public class BoundaryConditionPlotPanel extends EnabledJPanel
{
	private static final Logger LOGGER = Logger.getLogger(BoundaryConditionPlotPanel.class.getName());
	private RmaJComboBox<BoundaryConditionLocationPair> _dssPathCombo;
	private G2dPanel _plotPanel;
	private BcData _bcData;
	private ForecastSimGroup _fsg;
	private BoundaryConditionLocationPair _selectedLocationPair;

	public BoundaryConditionPlotPanel()
	{
		super(new GridBagLayout());
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		JLabel label = new JLabel("Location:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		
		_dssPathCombo = new RmaJComboBox<>();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_dssPathCombo, gbc);


		RmaNavigationPanel navPanel = new RmaNavigationPanel();
		navPanel.fillForm(_dssPathCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(navPanel, gbc);
		
		_plotPanel = new G2dPanel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_plotPanel, gbc);
	}
	
	/**
	 * 
	 */
	private void addListeners()
	{
		_dssPathCombo.addItemListener(this::dssRecordComboSelected);
	}

	private void dssRecordComboSelected(ItemEvent e)
	{
		if(_bcData != null && (e == null || ItemEvent.DESELECTED != e.getStateChange()))
		{
			Path dssFile = _bcData.getOutputDssFile();
			Object bcLocationPairObj = _dssPathCombo.getSelectedItem();
			if(dssFile != null && bcLocationPairObj instanceof BoundaryConditionLocationPair)
			{
				Path dssFileAbsolutePath = Paths.get(Project.getCurrentProject().getAbsolutePath(dssFile.toString()));
				try
				{
					BoundaryConditionLocationPair bcLocationPair = (BoundaryConditionLocationPair) bcLocationPairObj;
					DSSPathname dssPath = bcLocationPair.getDssPath();
					if(dssFileAbsolutePath.toFile().exists() && dssPath != null)
					{
						DSSIdentifier dssIdentifier = new DSSIdentifier(dssFileAbsolutePath.toString(), dssPath.toString());
						TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, true);
						TimeSeriesDataSet tsds = new TimeSeriesDataSet(tsc);
						updatePlot(tsds, bcLocationPair);
					}
					_selectedLocationPair = bcLocationPair;
				}
				finally
				{
					DssFileManagerImpl.getDssFileManager().close(dssFileAbsolutePath.toString());
				}
			}
		}
	}

	private void updatePlot(TimeSeriesDataSet tsds, BoundaryConditionLocationPair bcLocationPair)
	{
		Viewport[] viewports = _plotPanel.getViewports();
		if(viewports != null && viewports.length > 0)
		{
			if(bcLocationPair.equals(_selectedLocationPair))
			{
				updatePlotWithBothAxisZoomPreserved(tsds);
			}
			else
			{
				updatePlotWithXAxisZoomPreserved(tsds);
			}
		}
		else
		{
			_plotPanel.setVisible(false);
			List<G2dObject> v = new ArrayList<>();
			v.add(tsds);
			_plotPanel.buildComponents(v);
			_plotPanel.setVisible(true);
		}
	}

	private void updatePlotWithXAxisZoomPreserved(TimeSeriesDataSet tsds)
	{
		Viewport[] viewports = _plotPanel.getViewports();
		Axis xaxis = viewports[0].getAxis("x1");
		double actMin = xaxis.getActMin();
		double actMax = xaxis.getActMax();

		_plotPanel.setVisible(false);
		List<G2dObject> v = new ArrayList<>();
		v.add(tsds);
		_plotPanel.buildComponents(v);

		viewports = _plotPanel.getViewports();
		xaxis = viewports[0].getAxis("x1");
		double min = xaxis.getMin();
		double max = xaxis.getMax();
		if(actMin < min)
			actMin = min;
		if(actMax > max)
			actMax = max;
		xaxis.zoomIn(actMin, actMax);

		_plotPanel.setVisible(true);
		_plotPanel.repaint();
	}

	private void updatePlotWithBothAxisZoomPreserved(TimeSeriesDataSet tsds)
	{
		Viewport[] viewports = _plotPanel.getViewports();
		Axis xaxis = viewports[0].getAxis("x1");
		Axis yaxis = viewports[0].getAxis("y1");
		double actMin = xaxis.getActMin();
		double actMax = xaxis.getActMax();
		double actYMin = yaxis.getActMin();
		double actYMax = yaxis.getActMax();

		_plotPanel.setVisible(false);
		List<G2dObject> v = new ArrayList<>();
		v.add(tsds);
		_plotPanel.buildComponents(v);

		viewports = _plotPanel.getViewports();
		xaxis = viewports[0].getAxis("x1");
		yaxis = viewports[0].getAxis("y1");
		double min = xaxis.getMin();
		double max = xaxis.getMax();
		if(actMin < min)
			actMin = min;
		if(actMax > max)
			actMax = max;
		xaxis.zoomIn(actMin, actMax);
		min = yaxis.getMin();
		max = yaxis.getMax();
		if(actYMin < min)
			actYMin = min;
		if(actYMax > max)
			actYMax = max;
		yaxis.zoomIn(actYMin, actYMax);

		_plotPanel.setVisible(true);
		_plotPanel.repaint();
	}

	public void fillPanel(ForecastSimGroup fsg, BcData bcData)
	{
		_fsg = fsg;
		_bcData = bcData;
		setEnabled(bcData != null);
		fillCombo(bcData);
		dssRecordComboSelected(null);
	}

	private void fillCombo(BcData bcData)
	{
		List<BoundaryConditionLocationPair> pathnames = new ArrayList<>();
		if(bcData != null)
		{
			String delim = "/";
			String pathnameDataFile = "forecast/simGroups/" + _fsg.getName() + delim + bcData.getOpsDataName() + "-" + bcData.getMetDataName() + ".txt";
			String pathnameDataFileAbs = Project.getCurrentProject().getAbsolutePath(pathnameDataFile);
			try
			{
				pathnames = readBoundaryConditionLocationPathPairs(pathnameDataFileAbs);
			}
			catch (IOException e)
			{
				LOGGER.log(Level.CONFIG, e, () -> "Error reading pathnames in: " + pathnameDataFileAbs);
			}
		}
		DefaultComboBoxModel<BoundaryConditionLocationPair> comboModel = new DefaultComboBoxModel<>();
		for(BoundaryConditionLocationPair pathname : pathnames)
		{
			comboModel.addElement(pathname);
		}
		_dssPathCombo.setModel(comboModel);
		if(_selectedLocationPair != null)
		{
			_dssPathCombo.setSelectedItem(_selectedLocationPair);
		}
	}

	private List<BoundaryConditionLocationPair> readBoundaryConditionLocationPathPairs(String filePath) throws IOException
	{
		String[] headersToExtract = {"location", "parameter", "dss path"};
		List<BoundaryConditionLocationPair> retVal = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			String line = reader.readLine(); // Read the header line
			String[] headers = line.split(","); // Split the header line by commas

			int[] columnIndices = new int[headersToExtract.length];
			for (int i = 0; i < headersToExtract.length; i++)
			{
				String header = headersToExtract[i];
				int columnIndex = -1;
				for (int j = 0; j < headers.length; j++)
				{
					if (headers[j].equals(header))
					{
						columnIndex = j;
						break;
					}
				}
				if (columnIndex == -1)
				{
					return new ArrayList<>();
				}
				columnIndices[i] = columnIndex;
			}

			while ((line = reader.readLine()) != null)
			{
				String[] fields = line.split(","); // Split the line by commas
				if (fields.length < headers.length)
				{
					continue;
				}
				BoundaryConditionLocationPair bcLocation = buildBoundaryConditionLocationPair(columnIndices, headersToExtract, fields);
				retVal.add(bcLocation);
			}
		}
		return retVal;
	}

	private BoundaryConditionLocationPair buildBoundaryConditionLocationPair(int[] columnIndices, String[] headersToExtract, String[] fields)
	{
		BoundaryConditionLocationPair bcLocation = new BoundaryConditionLocationPair();
		for (int i = 0; i < columnIndices.length; i++)
		{
			int columnIndex = columnIndices[i];
			String header = headersToExtract[i];
			String value = fields[columnIndex];
			switch (header)
			{
				case "location":
					bcLocation.setLocation(value);
					break;
				case "parameter":
					bcLocation.setParameter(value);
					break;
				case "dss path":
					bcLocation.setDssPath(new DSSPathname(value));
					break;
				default:
					break;
			}
		}
		return bcLocation;
	}

	public G2dPanel getPlotPanel()
	{
		return _plotPanel;
	}
	
}
