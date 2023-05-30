/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.table.TableRowSorter;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec.util.NumericComparator;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MetDataType;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;

/**
 * @author mark
 *
 */
public class ImportMetDataWindow extends RmaJDialog
{
	private static final String AVE_TEMP_FILE = ForecastConfigFiles.getRelativeYearlyTempDataFile();
			;
	private RmaJTextField _nameFld;
	private RmaJDescriptionField _descFld;
	private RmaJComboBox _importTypeCombo;
	private RmaJTable _metTable;
	private ButtonCmdPanel _cmdPanel;
	protected boolean _canceled;

	public ImportMetDataWindow(Window parent)
	{
		super(parent,  "Import Met Data", true);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
		fillAveTempData();
	}

	

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		
		JLabel label = new JLabel("Meteorology Name:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_nameFld = new RmaJTextField();
		label.setLabelFor(_nameFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_nameFld, gbc);
		
		label = new JLabel("Description:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_descFld = new RmaJDescriptionField();
		label.setLabelFor(_descFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_descFld, gbc);
		
		label = new JLabel("Data Source:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		MetDataType[] types = MetDataType.values();
		_importTypeCombo = new RmaJComboBox<>(types);
		label.setLabelFor(_importTypeCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_importTypeCombo, gbc);
		
		String[] headers = getHeadersFromConfigFile();
		_metTable = new RmaJTable(this, headers)
		{
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return col == 0;
			}
		};
		_metTable.setCheckBoxCellEditor(0);
		int cols = _metTable.getColumnCount();
		for (int i = 1;i < cols; i++ )
		{
			_metTable.setDoubleCellEditor(i);
		}
		_metTable.setRowHeight(_metTable.getRowHeight()+5);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_metTable.getScrollPane(), gbc);
		
	
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
	 * @return
	 */
	private String[] getHeadersFromConfigFile()
	{
		
		String configFile = getConfigFileName();
		
		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);
		
		String[] defaultHeader = new String[] {"Selected", "Year"};
		if ( file == null )
		{
			return defaultHeader;
		}
		BufferedReader reader = file.getBufferedReader();
		if ( reader == null )
		{
			return defaultHeader;
		}
		
		String headerLine;
		try
		{
			headerLine = reader.readLine();
			if ( headerLine != null )
			{
				String[] fileHeaders = headerLine.split(",");
				String[] headers = new String[fileHeaders.length+1];
				headers[0] = "Select";
				System.arraycopy(fileHeaders, 0, headers, 1, fileHeaders.length);
				return headers;
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
		
		
		return defaultHeader;
	}

	/**
	 * @return
	 */
	private String getConfigFileName()
	{
		String prjDir = Project.getCurrentProject().getProjectDirectory();
		String configFile = RMAIO.concatPath(prjDir, AVE_TEMP_FILE);
		
		return configFile;
		
		
	}
	/**
	 * 
	 */
	private void fillAveTempData()
	{
		_metTable.deleteCells();
		
		String configFile = getConfigFileName();
		
		RmaFile file = FileManagerImpl.getFileManager().getFile(configFile);
		
		if ( file == null )
		{
			return;
		}
		BufferedReader reader = file.getBufferedReader();
		if ( reader == null )
		{
			return;
		}
		
		String headerLine;
		try
		{
			Vector line = null;
			reader.readLine(); // skip first header line
			while ((headerLine = reader.readLine()) != null )
			{
				line = new Vector();
				String[] values = headerLine.split(",");
				line.add(Boolean.FALSE);
				for (int i = 0;i < values.length;i++ )
				{
					line.add(values[i]);
				}
				
				_metTable.appendRow(line);
			}
		}
		catch (IOException e)
		{
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
		TableRowSorter sorter = new TableRowSorter(_metTable.getModel());
		int cols = _metTable.getColumnCount();
		for (int c = 0;c < cols; c++ )
		{
			sorter.setComparator(c, new NumericComparator());
		}
		_metTable.setRowSorter(sorter);
		
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						if ( isValidForm())
						{
							saveForm();
							setVisible(false);
							_canceled = false;
						}
						break;
					case ButtonCmdPanel.CANCEL_BUTTON :
						setVisible(false);
						_canceled = true;
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
		// TODO Auto-generated method stub
		System.out.println("saveForm TODO implement me");
		
	}



	/**
	 * @return
	 */
	protected boolean isValidForm()
	{
		String name = _nameFld.getText().trim();
		if ( name.isEmpty() )
		{
			JOptionPane.showMessageDialog(this, "Please enter a name", "No Name", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		if ( _importTypeCombo.getSelectedItem() == null )
		{
			JOptionPane.showMessageDialog(this, "Please Select a Data Source", "No Data Source", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		List<Integer>selectedYears = getSelectedYears();
		if ( selectedYears.isEmpty() )
		{
			JOptionPane.showMessageDialog(this, "Please Select one or more years", "No Years Selected", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		
		return true;
	}

	public List<MeteorlogicData> getMetData()
	{
		List<MeteorlogicData>metDataSets = new ArrayList<>();
		List<Integer> years = getSelectedYears();
		importData(years);
		int year;
		for (int i=0; i < years.size(); i++ )
		{
			year = years.get(i);
			MeteorlogicData metData = new MeteorlogicData();
			metData.setName(_nameFld.getText().trim()+"-"+year);
			metData.setDescription(_descFld.getText().trim());
			metData.setMetDataType((MetDataType) _importTypeCombo.getSelectedItem());

			metData.setYear(year);
			metDataSets.add(metData);
		}
		return metDataSets;
	}

	/**
	 * import the data from DSS, shifting it to the selected dates
	 */
	private void importData(List<Integer>year)
	{

	}


	/**
	 * @return
	 */
	private List<Integer> getSelectedYears()
	{
		int rowCnt = _metTable.getRowCount();
		Object obj;
		List<Integer>selectedYears = new ArrayList<>();
		for (int r = 0;r < rowCnt;r++)
		{
			obj = _metTable.getValueAt(r, 0);
			if ( Boolean.TRUE == obj || "true".equalsIgnoreCase(obj.toString()))
			{
				String year = (String) _metTable.getValueAt(r, 1);
				selectedYears.add(RMAIO.parseInt(year));
			}
		}
		return selectedYears;
	}



	/**
	 * @param fsg
	 */
	public void fillForm(ForecastSimGroup fsg)
	{

	}



	/**
	 * @return
	 */
	public boolean isCanceled()
	{
		return _canceled;
	}
}
