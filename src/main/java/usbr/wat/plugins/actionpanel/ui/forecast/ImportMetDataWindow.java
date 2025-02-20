/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec.data.ParamDouble;
import hec.data.Parameter;
import hec.io.DSSIdentifier;
import hec.io.Identifier;
import hec.util.NumericComparator;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import rma.swing.table.RmaTableModel;
import rma.swing.table.ToolTipHeader;
import rma.util.RMAConst;
import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.io.forecast.MetConfigFileReader;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.MetDataDssValidator;
import usbr.wat.plugins.actionpanel.model.forecast.MetDataType;
import usbr.wat.plugins.actionpanel.model.forecast.MeteorlogicData;

/**
 * @author mark
 *
 */
public class ImportMetDataWindow extends ImportForecastWindow
{
	private static final String AVE_TEMP_FILE = ForecastConfigFiles.getRelativeYearlyTempDataFile();
	private RmaJTextField _nameFld;
	private RmaJDescriptionField _descFld;
	private RmaJComboBox<Identifier> _importTypeCombo;
	private RmaJTable _metTable;
	private ButtonCmdPanel _cmdPanel;
	private Map<String, MetTableModel> _metTables = new HashMap<>();

	public ImportMetDataWindow(Window parent)
	{
		super(parent,  "Import Met Data", true);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
		//fillAveTempData();
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


		_importTypeCombo = new RmaJComboBox<>();
		label.setLabelFor(_importTypeCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.5;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_importTypeCombo, gbc);
		
		String[] headers = new String[]{"Selected", "Year", "Spring Ave|Air Temp",
			"Summer Ave|Air Temp", "Fall Ave|Air Temp"};
		_metTable = new RmaJTable(this, headers)
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				TableModel model = getModel();
				if ( !(model instanceof MetTableModel))
				{
					return super.getToolTipText(e);
				}
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				if ( row == -1 )
				{
					return super.getToolTipText(e);
				}
				int col = columnAtPoint(p);
				if ( col == -1 )
				{
					return super.getToolTipText(e);
				}
				MetTableModel metModel = (MetTableModel) model;
				if ( !metModel.isCellEditable(row,0))
				{
					List<String> missingRecords = metModel.getMissingRecordsForRow(row);
					if ( missingRecords != null && !missingRecords.isEmpty())
					{
						StringBuilder sb = new StringBuilder();
						sb.append("<html>The Following DSS Records are missing data for the year ");
						sb.append(getValueAt(row,1));
						sb.append("<br>");
						for(int i =0;i < missingRecords.size();i++)
						{
							sb.append(missingRecords.get(i));
							sb.append("<br>");
						}
						return sb.toString();
					}
				}
				return super.getToolTipText(e);
			}
		};
		setTableEditors();
		_metTable.deleteCells();
		_metTable.setRowHeight(_metTable.getRowHeight()+5);
		_metTable.removePopupMenuSumOptions();
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


	private void setTableEditors()
	{
		_metTable.setCheckBoxCellEditor(0);
		int cols = _metTable.getColumnCount();
		for (int i = 1;i < cols; i++ )
		{
			_metTable.setDoubleCellEditor(i);
		}
		_metTable.setMlHeaderRenderer().setSeparatorToken("|");
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
		_importTypeCombo.addItemListener(e->metComboChanged(e));
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

	private void metComboChanged(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange())
		{
			return;
		}
		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();
		MetTableModel table = _metTables.get(id.getPath());
		if ( table == null )
		{
			table = readMetConfigFile();
			_metTables.put(id.getPath(), table);
		}
		_metTable.setModel(table);
		_importTypeCombo.setToolTipText("<html><b>Config File:</b> "+id.getPath()+
				"<br><b>Temperature Values from:</b> "+table.getDssRecord());
		JTableHeader tableHeader = _metTable.getTableHeader();
		if (tableHeader instanceof ToolTipHeader )
		{
			ToolTipHeader ttheader = (ToolTipHeader) tableHeader;
			ttheader.setToolTipStrings(new String[]{"Select the Year", "The Year", "Mar, Apr, May", "Jun, Jul, Aug", "Sept, Oct, Nov"});
		}
		setTableEditors();
	}

	private MetTableModel readMetConfigFile()
	{
		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();
		MetConfigFileReader reader = new MetConfigFileReader(id.getPath());
		if ( reader.readDssPathsFile())
		{
			List<DSSIdentifier> srcDssEntries = reader.getSourceDssIdentifiers();
			fixupDssFilePaths(srcDssEntries);
			MetDataDssValidator validator = new MetDataDssValidator(srcDssEntries);
			List<Integer> years = validator.getYears();
			Map<Integer, List<String>> invalidYears = validator.getInvalidYears();
			Map<Integer, Double> springAverages = validator.getSpringAverages();
			Map<Integer, Double> summerAverages = validator.getSummerAverages();
			Map<Integer, Double> fallAverages = validator.getFallAverages();
			String units = validator.getDssUnits();

			List<String> errors = validator.getErrors();
			if ( !errors.isEmpty())
			{
				EventQueue.invokeLater(()->displayErrors(id, errors));


			}

			MetTableModel newModel = new MetTableModel(years, invalidYears, springAverages, summerAverages, fallAverages, units);
			newModel.setDssRecord(srcDssEntries.get(0));
			return newModel;
		}
		return new MetTableModel(null, null, null, null, null, "");
	}

	private void displayErrors(Identifier id, List<String> errors)
	{
		int opt = JOptionPane.showConfirmDialog(this, "<html>There were errors with processing the Time Series Records specified in the config file.<br>Do you want to see the errors?",
				"Errors Occurred", JOptionPane.YES_NO_OPTION);
		if ( JOptionPane.YES_OPTION == opt )
		{
			final JEditorPane txtArea = new JEditorPane()
			{
				public Dimension getPreferredScrollableViewportSize()
				{
					Dimension d = super.getPreferredScrollableViewportSize();
					d.width = 400;
					d.height = 300;
					return d;
				}
			};
			JPopupMenu popup = new JPopupMenu();

			Action action = new AbstractAction("Copy to Clipboard")
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					txtArea.copy();
				}
			};
			popup.add(action);
			action = new AbstractAction("Select All")
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					txtArea.selectAll();
				}
			};
			popup.add(action);
			txtArea.setComponentPopupMenu(popup);
			StringBuffer sb = new StringBuffer();
			sb.append("<html><b>The Following Errors Occurred during the Processing of the Time Series Records");
			sb.append("<br>File:</b>"+id.getPath());
			sb.append("<br><b>Import Issues:</b>");
			for (int e = 0;e < errors.size();e++)
			{
				sb.append("<br>");
				sb.append(errors.get(e));
			}
			sb.append("</html>");
			txtArea.setContentType("text/html");
			txtArea.setText(sb.toString());
			JScrollPane sp = new JScrollPane(txtArea);
			sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			txtArea.setCaretPosition(0);
			JOptionPane.showMessageDialog(null, sp, "Import Issues", JOptionPane.INFORMATION_MESSAGE);
		}

	}

	private void fixupDssFilePaths(List<DSSIdentifier> srcDssEntries)
	{
		DSSIdentifier dssId;
		String fullPath;
		for (int i = 0; i < srcDssEntries.size(); i++)
		{
			dssId = srcDssEntries.get(i);
			if ( !RMAIO.isFullPath(dssId.getFileName()))
			{
				fullPath = Project.getCurrentProject().getAbsolutePath(dssId.getFileName());
				dssId.setFileName(fullPath);
			}
		}
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
			metData.setMetConfigFile(Project.getCurrentProject().getRelativePath(getSelectedMetConfigFile()));

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
				Integer year = (Integer) _metTable.getValueAt(r, 1);
				selectedYears.add(year);
			}
		}
		return selectedYears;
	}



	/**
	 * @param fsg
	 */
	public void fillForm(ForecastSimGroup fsg)
	{
		fillMetDataCombo();
		_canceled = true;
	}

	/**
	 *
	 */
	private void fillMetDataCombo()
	{
		String dir = ForecastConfigFiles.getMetConfigFilesFolder();
		RMAFilenameFilter filter = new RMAFilenameFilter("config");
		List<String> configFiles = FileManagerImpl.getFileManager().list(dir, filter);
		if ( configFiles == null || configFiles.isEmpty())
		{
			String msg;
			if ( !FileManagerImpl.getFileManager().fileExists(dir))
			{
				msg = "Missing the following folder "+dir;
			}
			else
			{
				msg ="No Met Config Files found in "+dir ;
			}

			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(this, msg, "No Met Config Files",
					JOptionPane.INFORMATION_MESSAGE));
			return;
		}
		List<Identifier>ids = getConfigFileEntries(configFiles);
		RmaListModel<Identifier> newModel = new RmaListModel<>(true, ids);
		_importTypeCombo.setModel(newModel);
	}

	/**
	 *
	 * @param configFiles
	 * @return
	 */
	private List<Identifier> getConfigFileEntries(List<String> configFiles)
	{
		List<Identifier>ids = new ArrayList<>();
		if ( configFiles == null || configFiles.isEmpty() )
		{
			return ids;
		}
		String file, path;
		Identifier id;
		for(int i = 0;i < configFiles.size();i++)
		{
			path = configFiles.get(i);
			id = new Identifier(path);
			id.setName(RMAIO.getFileNameNoExtension(path));
			ids.add(id);
		}

		return ids;
	}


	/**
	 * @return
	 */
	@Override
	public boolean isCanceled()
	{
		return _canceled;
	}

	/**
	 * get the selected file for the met config file
	 * @return
	 */
	public String getSelectedMetConfigFile()
	{
		Identifier id = (Identifier) _importTypeCombo.getSelectedItem();
		return id.getPath();
	}
	/**
	 * table model for the met data
	 */
	public class MetTableModel extends RmaTableModel
	{
		private static final int SELECTION_COL = 0;
		private static final int YEAR_COL = 1;
		private static final int SPRING_AVE_COL = 2;
		private static final int SUMMER_AVE_COL = 3;
		private static final int FALL_AVE_COL = 4;

		private final List<Integer> _years;
		private final Map<Integer, List<String>> _invalidYears;
		private final ParamDouble _paramDouble;
		private Map<Integer, Double> _springAverages;
		private Map<Integer, Double> _summerAverages;
		private Map<Integer, Double> _fallAverages;
		private Map<Integer, Boolean> _selected = new HashMap<>();
		private DSSIdentifier _dssId;

		public MetTableModel(List<Integer> years, Map<Integer, List<String>>invalidYears, Map<Integer, Double> springAverages,
							 Map<Integer, Double> summerAverages, Map<Integer, Double> fallAverages, String units)
		{
			super(new String[] {"Selected", "Year", "Spring Ave|Air Temp (%s)".replace("%s", units),
					"Summer Ave|Air Temp (%s)".replace("%s",units), "Fall Ave|Air Temp (%s)".replace("%s",units)});
			_years = years;
			_invalidYears =  invalidYears;
			_springAverages = springAverages;
			_summerAverages = summerAverages;
			_fallAverages = fallAverages;
			_paramDouble = new ParamDouble(0, Parameter.PARAMID_TEMP, Project.getCurrentProject().getUnitSystem(), 1);
		}
		public int getRowCount()
		{
			if ( _years != null )
			{
				return _years.size();
			}
			return 0;
		}

	

		public boolean isCellEditable(int row, int col)
		{
			if ( _years == null )
			{
				return false;
			}
			if ( col == 0)
			{
				int obj = _years.get(row);
				if ( _invalidYears.get(obj) == null)
				{
					return true;
				}
			}
			return false;
		}

		public Object getValueAt(final int row, final int col)
		{
			if ( _years == null )
			{
				return null;
			}
			int year = _years.get(row);
			double val;
			switch ( col )
			{
				case SELECTION_COL: // selected
					Boolean selected = _selected.get(row);
					if ( selected == null )
					{
						return Boolean.FALSE;
					}
					return selected;
				case YEAR_COL:
					return year;
				case SPRING_AVE_COL:
					val = RMAConst.UNDEF_DOUBLE ;
					if ( _springAverages != null )
					{
						Double value = _springAverages.get(year);
						if ( value != null )
						{
							val = value;
						}
					}
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try
					{
						return _paramDouble.clone();
					} catch (CloneNotSupportedException e)
					{
						return val;
					}

				case SUMMER_AVE_COL:
					val = RMAConst.UNDEF_DOUBLE ;
					if ( _summerAverages != null )
					{
						Double value = _summerAverages.get(year);
						if ( value != null )
						{
							val = value;
						}
					}
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try
					{
						return _paramDouble.clone();
					} catch (CloneNotSupportedException e)
					{
						return val;
					}
				case FALL_AVE_COL:
					val = RMAConst.UNDEF_DOUBLE ;
					if ( _summerAverages != null )
					{
						Double value = _fallAverages.get(year);
						if ( value != null )
						{
							val = value;
						}
					}
					_paramDouble.setValue(val);
					_paramDouble.setPrecision(1);
					try
					{
						return _paramDouble.clone();
					}
					catch (CloneNotSupportedException e)
					{
						return val;
					}
			}
			return null;
		}

		public void setValueAt(Object obj, final int row, final int col)
		{
			if ( col == SELECTION_COL )
			{
				_selected.put(row, Boolean.parseBoolean(obj.toString()));
				fireTableCellUpdated(row, col);
			}
		}

		public List<String> getMissingRecordsForRow(int row)
		{
			Object year = getValueAt(row, YEAR_COL);
			return _invalidYears.get(year);
		}

		public void setDssRecord(DSSIdentifier dssId)
		{
			_dssId = dssId;
		}
		public DSSIdentifier getDssRecord()
		{
			return _dssId;
		}
	}

}
