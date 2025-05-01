/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import hec.geometry.Axis;
import hec.gfx2d.G2dObject;
import hec.gfx2d.G2dPanel;
import hec.gfx2d.TimeSeriesDataSet;
import hec.gfx2d.Viewport;
import hec.heclib.util.HecTime;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJTextField;
import rma.swing.RmaNavigationPanel;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.ui.forecast.DssLocation;
import usbr.wat.plugins.actionpanel.ui.forecast.MetLocation;

public class MetPlotPanel extends EnabledJPanel
{
	private JLabel _label;
	private RmaJComboBox<MetLocation> _locationCombo;
	private RmaJComboBox<DssLocation> _dssRecordCombo;
	private RmaNavigationPanel _locationNavPanel;
	private G2dPanel _plotPanel;
	private RmaNavigationPanel _dssNavPanel;
	private double _maxYScale = Double.MIN_VALUE;
	private double _minYScale = Double.MAX_VALUE;
	private int _year;
	private JLabel _msgLine;

	public MetPlotPanel()
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
		_label = new JLabel("Location:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_label, gbc);


		_locationCombo = new RmaJComboBox<>();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_locationCombo, gbc);

		_locationNavPanel = new RmaNavigationPanel();
		_locationNavPanel.fillForm(_locationCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_locationNavPanel, gbc);

		_label = new JLabel("Record:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_label, gbc);


		_dssRecordCombo = new RmaJComboBox<DssLocation>()
		{
			public String getToolTipText(MouseEvent e)
			{
				DssLocation location = (DssLocation) _dssRecordCombo.getSelectedItem();
				if ( location == null )
				{
					return super.getToolTipText(e);
				}
				return "<html><b>DSS File:</b>"+location.getDssFile()+"<br><b>DSS Path:</b>"+location.getDssPath()+"</html>";
			}
		};
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_dssRecordCombo, gbc);


		_dssNavPanel = new RmaNavigationPanel();
		_dssNavPanel.fillForm(_dssRecordCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_dssNavPanel, gbc);

		_plotPanel = new G2dPanel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_plotPanel, gbc);


		_msgLine = new JLabel();
		_msgLine.setForeground(Color.RED);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_msgLine, gbc);
	}

	/**
	 *
	 */
	private void addListeners()
	{
		_locationCombo.addItemListener(e -> locationComboSelected(e));
		_dssRecordCombo.addItemListener(e -> dssRecordComboSelected(e));
	}


	private void locationComboSelected(ItemEvent e)
	{
		if (ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		MetLocation metLocation = (MetLocation) _locationCombo.getSelectedItem();
		List<DssLocation> dssLocations = metLocation.getDssLocations();
		RmaListModel<DssLocation> newModel = new RmaListModel<>(true, dssLocations);
		_dssRecordCombo.setModel(newModel);
		if ( _year > 0 && newModel.getSize() > 0 )
		{
			_dssRecordCombo.setSelectedIndex(0);
		}

	}

	private void dssRecordComboSelected(ItemEvent e)
	{
		if (ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		DssLocation dssLocation = (DssLocation) _dssRecordCombo.getSelectedItem();
		_maxYScale = Double.MIN_VALUE;
		_minYScale = Double.MAX_VALUE;
		fillPlotPanel(dssLocation);
	}
	public void fillPlotPanel()
	{
		DssLocation location = (DssLocation) _dssRecordCombo.getSelectedItem();
		if ( location == null )
		{
			_plotPanel.clearPanel();
		}
		else
		{
			fillPlotPanel(location);
		}
	}

	private void fixZoomScale()
	{
		Viewport[] viewports = _plotPanel.getViewports();
		if ( viewports != null && viewports.length > 0 )
		{
			Axis yaxis = viewports[0].getAxis("Y1");
			if(yaxis.getMax() > _maxYScale)
			{
				_maxYScale = yaxis.getMax();
			}
			if(yaxis.getMin() < _minYScale)
			{
				_minYScale = yaxis.getMin();
			}
			yaxis.setMaximumLimit(_maxYScale);
			yaxis.setMinimumLimit(_minYScale);
			yaxis.setViewLimits(_minYScale, _maxYScale);
			_plotPanel.setVisible(true);
			_plotPanel.repaint();
		}
	}

	private void fillPlotPanel(DssLocation dssLocation)
	{
		String dssFile = dssLocation.getDssFile();
		String dssPath = dssLocation.getDssPath();
		Project prj = Project.getCurrentProject();
		dssFile = prj.getAbsolutePath(dssFile);
		DSSIdentifier dssId = new DSSIdentifier(dssFile, dssPath);
		dssId.setStartTime(new HecTime("01Jan"+_year, "0000"));
		dssId.getStartTime().showTimeAsBeginningOfDay(true);
		dssId.setEndTime(new HecTime("31Dec"+_year, "2400"));
		TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, true);
		if ( tsc != null && tsc.numberValues > 0 )
		{
			tsc.trimToTime(dssId.getStartTime(), dssId.getEndTime());
			TimeSeriesDataSet tsds = new TimeSeriesDataSet(tsc);
			List<G2dObject> v = new ArrayList<>();
			v.add(tsds);
			_plotPanel.buildComponents(v);
			fixZoomScale();
			_msgLine.setText("");
		}
		else
		{
			_msgLine.setText("No Met Data Found for " +_locationCombo.getSelectedItem()+" - "+dssLocation.getName() + " for time window " + dssId.getStartTime()+" to "+dssId.getEndTime());
			_plotPanel.clearPanel();
		}
	}

	public G2dPanel getPlotPanel()
	{
		return _plotPanel;
	}

	public void setLocationList(List<MetLocation> locations)
	{
		_locationCombo.removeAllItems();
		_dssRecordCombo.removeAllItems();
		if ( locations == null )
		{
			return;
		}
		RmaListModel newModel = new RmaListModel(true, locations);
		_locationCombo.setModel(newModel);
	}

	public void setYear(int year)
	{
		_year = year;
		if ( _locationCombo.getItemCount() > 0 )
		{
			_locationCombo.setSelectedIndex(0);
		}
	}

    public void clearPanel()
    {
		_locationCombo.setSelectedIndex(-1);
		_dssRecordCombo.setSelectedIndex(-1);
		_plotPanel.clearPanel();
		_locationCombo.setEnabled(false);
    }
}
