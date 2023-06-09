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
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JButton;

import com.rma.model.Project;
import com.rma.swing.excel.ExcelTable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;

/**
 * @author mark
 *
 */
public class OperationsPanel extends AbstractForecastPanel
{
	private static Logger LOGGER = Logger.getLogger(OperationsPanel.class.getName());
	private static final LocalDateTime EXCEL_BASE_DATE = LocalDateTime.of(1899, 12, 31, 0, 0);
	private static final Pattern EXCEL_PATTERN_MMM = Pattern.compile("mmm");
	private static final Pattern EXCEL_PATTERN_H = Pattern.compile("h");
	private static final Pattern EXCEL_PATTERN_AM_PM = Pattern.compile("AM/PM");
	private static final Pattern EXCEL_PATTERN_DDD = Pattern.compile("ddd");
	private static final Pattern EXCEL_PATTERN_DDDD = Pattern.compile("dddd");
	private static final Pattern EXCEL_PATTERN_SINGLE_M = Pattern.compile("(?<!m)m(?!m)");
	private static final Pattern EXCEL_PATTERN_FRACTIONAL_SECONDS = Pattern.compile("0+");
	private static final Pattern EXCEL_PATTERN_TIME_ZONE = Pattern.compile("Z|ZZZ");
	private RmaJTable _opInfoTable;
	private JButton _importButton;
	private ExcelTable _reservoirTable;
	private RmaJTable _excelTable;
	private ForecastSimGroup _fsg;

	/**
	 * @param forecastPanel
	 */
	public OperationsPanel(ForecastPanel forecastPanel)
	{
		super(forecastPanel);
	}

	@Override
	protected void buildLowerPanel(EnabledJPanel lowerPanel)
	{
		String[] headers = new String[] {"Operations", "File Path", "Description", "Forecast Date"};

		_opInfoTable = new RmaJTable(this, headers)
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
		gbc.weighty   = 0.1;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_opInfoTable.getScrollPane(), gbc);

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


		_excelTable = new RmaJTable(this, new String[]{""});
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		lowerPanel.add(_excelTable.getScrollPane(), gbc);

	}
	protected void addListeners()
	{
		super.addListeners();
		_importButton.addActionListener(e->importOperationsAction());
	}

	private void importOperationsAction()
	{
		ImportOperationsWindow dlg = new ImportOperationsWindow(ActionPanelPlugin.getInstance().getActionsWindow());
		dlg.fillForm(_fsg);
		dlg.setVisible(true);
		if ( dlg.isCanceled())
		{
			return;
		}
		OperationsData opsData = dlg.getOperationsData();
		ForecastTable opsTable = getTableForPanel();
		Vector<OperationsData> row = new Vector<>();
		row.add(opsData);
		opsTable.appendRow(row);
		_fsg.getOperationsData().add(opsData);
		_fsg.setModified(true);
		tableRowSelected(opsTable.getRowCount() -1);
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _opsTable;
	}

	@Override
	protected void savePanel()
	{
		if ( _fsg != null )
		{
			// TODO Auto-generated method stub
			System.out.println("savePanel TODO implement me");
		}

	}

	@Override
	public void setSimulationGroup(ForecastSimGroup fsg)
	{
		clearPanel();
		setEnabled(fsg != null);
		_fsg = fsg;
		ForecastTable table = getTableForPanel();
		//_excelTable.setEnabled(false);
		if ( _fsg != null )
		{
			List<OperationsData> data = _fsg.getOperationsData();
			table.deleteCells();
			Vector<OperationsData> row;
			for (int i = 0; i < data.size(); i++ )
			{
				row = new Vector<>();
				row.add(data.get(i));
				table.appendRow(row);
			}
			if(!data.isEmpty())
			{
				displayOpsData(data.get(data.size()-1));
			}
		}
	}

	private void clearPanel()
	{
		_lowerPanel.remove(_excelTable.getScrollPane());
		_excelTable = new RmaJTable(this, new String[]{""});
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		_lowerPanel.add(_excelTable.getScrollPane(), gbc);
		_lowerPanel.revalidate();
		_lowerPanel.repaint();
	}

	@Override
	protected void tableRowSelected(int selRow)
	{
		if (_fsg != null )
		{
			ForecastTable table = getTableForPanel();
			_opInfoTable.deleteCells();
			if (selRow > -1)
			{
				OperationsData opsData = (OperationsData) table.getValueAt(selRow, 0);
				Vector row = new Vector();
				row.add(opsData.getName());
				row.add(opsData.getOperationsFile());
				row.add(opsData.getDescription());

				_opInfoTable.appendRow(row);
				displayOpsData(opsData);
			}
		}
	}

	@Override
	public void tableRowDeleteClicked(int selectedRow)
	{
		//TODO
	}

	private void displayOpsData(OperationsData opsData)
	{
		String opsFilePath = Project.getCurrentProject().getAbsolutePath(opsData.getOperationsFile());
		Sheet sheet = null;
		if(opsFilePath == null || opsFilePath.isEmpty())
		{
			return;
		}
		if(opsFilePath.endsWith(".csv"))
		{
			sheet = readCsv(opsFilePath);
		}
		else
		{
			String csvString = convertXlsxToCsv(opsFilePath);
			try (BufferedReader reader = new BufferedReader(new StringReader(csvString)))
			{
				sheet = readIntoSheet(reader);
			}
			catch (IOException e)
			{
				LOGGER.log(Level.SEVERE, e, () -> "Failed to read file: " + opsFilePath);
			}
		}
		if(sheet != null)
		{
			_lowerPanel.remove(_excelTable.getScrollPane());
			_excelTable = new ExcelTable(this, sheet);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx   = 1.0;
			gbc.weighty   = 1.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.BOTH;
			gbc.insets    = RmaInsets.INSETS5505;
			_lowerPanel.add(_excelTable.getScrollPane(), gbc);
			_lowerPanel.revalidate();
			_lowerPanel.repaint();
		}
	}

	private Sheet readCsv(String opsFilePath)
	{
		Sheet retVal = null;
		try(BufferedReader reader = new BufferedReader(new FileReader(opsFilePath)))
		{
			retVal = readIntoSheet(reader);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Failed to read file: " + opsFilePath);
		}
		return retVal;
	}

	private Sheet readIntoSheet(BufferedReader reader) throws IOException
	{
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("My Sheet");
		String line;
		int rowNum = 0;
		while ((line = reader.readLine()) != null)
		{
			Row row = sheet.createRow(rowNum);
			rowNum++;
			String[] values = line.split(",", -1);
			for (int i = 0; i < values.length; i++)
			{
				Cell cell = row.createCell(i);
				String val = values[i];
				if(i == 0 && val.contains("View Results:"))
				{
					val = "View Results:";
				}
				cell.setCellValue(val);
			}
		}
		return sheet;
	}

	public static String convertXlsxToCsv(String xlsxFilePath)
	{
		String retVal = "";
		try
		{
			Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(xlsxFilePath)));
			Sheet sheet = workbook.getSheetAt(0);
			StringBuilder csvString = new StringBuilder();

			for (Row row : sheet)
			{
				for (int i=row.getFirstCellNum(); i < row.getLastCellNum(); i++)
				{
					Cell cell = row.getCell(i);
					if(cell instanceof XSSFCell)
					{
						XSSFCell xssfCell = (XSSFCell) cell;
						String cellValue = "";
						if (xssfCell.getCellType() == Cell.CELL_TYPE_STRING)
						{
							cellValue = xssfCell.getStringCellValue();
						}
						else if (xssfCell.getCellType() == Cell.CELL_TYPE_NUMERIC)
						{
							cellValue = handleNumericFormulaType(xssfCell);
						}
						else if (xssfCell.getCellType() == Cell.CELL_TYPE_BOOLEAN)
						{
							cellValue = String.valueOf(xssfCell.getBooleanCellValue());
						}
						else if (xssfCell.getCellType() == Cell.CELL_TYPE_FORMULA)
						{
							cellValue = handleNumericFormulaType(xssfCell);
						}
						csvString.append(cellValue).append(",");
					}
				}
				csvString.append("\n");
			}
			retVal = csvString.toString();
		}
		catch (IOException e)
		{
			LOGGER.log(Level.CONFIG, e, () -> "Failed to convert " + xlsxFilePath + " to csv string");
		}
		return retVal;
	}

	private static String handleNumericFormulaType(XSSFCell xssfCell)
	{
		String cellValue;
		try
		{
			if(DateUtil.isCellDateFormatted(xssfCell))
			{
				short dataFormatIndex = xssfCell.getCellStyle().getDataFormat();
				String excelPattern = xssfCell.getSheet().getWorkbook().createDataFormat().getFormat(dataFormatIndex);
				String javaPattern = EXCEL_PATTERN_MMM.matcher(excelPattern).replaceAll("MMM");
				String hourReplace = "hh";
				if(!excelPattern.contains(EXCEL_PATTERN_AM_PM.pattern()))
				{
					hourReplace = "HH";
				}
				javaPattern = EXCEL_PATTERN_H.matcher(javaPattern).replaceAll(hourReplace);
				javaPattern = EXCEL_PATTERN_AM_PM.matcher(javaPattern).replaceAll("a");
				javaPattern = EXCEL_PATTERN_DDD.matcher(javaPattern).replaceAll("EEE");
				javaPattern = EXCEL_PATTERN_DDDD.matcher(javaPattern).replaceAll("EEEE");
				javaPattern = EXCEL_PATTERN_SINGLE_M.matcher(javaPattern).replaceAll("M");
				javaPattern = EXCEL_PATTERN_FRACTIONAL_SECONDS.matcher(javaPattern).replaceAll("S");
				javaPattern = EXCEL_PATTERN_TIME_ZONE.matcher(javaPattern).replaceAll("XXX");
				DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(javaPattern);
				LocalDateTime date = convertNumericToDate(xssfCell.getNumericCellValue());
				cellValue = dateFormat.format(date);
			}
			else
			{
				double numeric = xssfCell.getNumericCellValue();
				DecimalFormat decimalFormat = new DecimalFormat("#.#");
				cellValue = decimalFormat.format(numeric);
			}
		}
		catch (IllegalStateException e)
		{
			cellValue = xssfCell.getRawValue();
			LOGGER.log(Level.FINE, e, () -> "Using raw value of " + xssfCell.getRawValue() + " for cell");
		}
		return cellValue;
	}

	private static LocalDateTime convertNumericToDate(double numericDateValue)
	{
		return EXCEL_BASE_DATE.plusDays((long) numericDateValue - 1);
	}

}
