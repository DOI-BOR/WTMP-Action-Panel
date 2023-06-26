/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Cursor;
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
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.rma.model.Project;
import com.rma.swing.excel.ExcelTable;

import hec.lang.NamedType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.model.forecast.BcData;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;

/**
 * @author mark
 *
 */
public class OperationsPanel extends AbstractForecastPanel<OperationsData>
{
	private static Logger LOGGER = Logger.getLogger(OperationsPanel.class.getName());
	private static final String OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT = "WTMP.OperationsPanel.Font.ConversionScale";
	private static final int DEFAULT_OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT = 60;
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
		_importButton.addActionListener(e->importForecastData(null));
	}

	@Override
	protected void importForecastData(ImportForecastWindow dlg)
	{
		ImportOperationsWindow importOpsWindow;
		if(dlg == null)
		{
			importOpsWindow = new ImportOperationsWindow(ActionPanelPlugin.getInstance().getActionsWindow());
			importOpsWindow.fillForm(_fsg);
		}
		else
		{
			importOpsWindow = (ImportOperationsWindow) dlg;
		}
		importOpsWindow.setVisible(true);
		if ( importOpsWindow.isCanceled())
		{
			return;
		}
		importData(_fsg, _opsTable, importOpsWindow, _fsg.getOperationsData(), importOpsWindow.getOperationsData());
	}

	@Override
	public ForecastTable getTableForPanel()
	{
		return _opsTable;
	}

	@Override
	protected void savePanel()
	{

	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if(visible && _opsTable.getSelectedRow() < 0)
		{
			clearPanel();
		}
	}

	@Override
	public void fillPanel(ForecastSimGroup fsg)
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

	@Override
	protected void clearPanel()
	{
		_lowerPanel.remove(_excelTable.getScrollPane());
		_excelTable = new RmaJTable(this, new String[]{""});
		_excelTable.deleteCells();
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
	protected void removeData(ForecastSimGroup fsg, OperationsData data)
	{
		fsg.removeOperationsData(data);
	}

	@Override
	protected void tableRowSelected(int selRow)
	{
		if (_fsg != null )
		{
			_opInfoTable.deleteCells();
			if (selRow > -1)
			{
				OperationsData opsData = (OperationsData) _opsTable.getValueAt(selRow, 0);
				if(opsData == null)
				{
					return;
				}
				Vector row = new Vector();
				row.add(opsData.getName());
				row.add(opsData.getOperationsFile());
				row.add(opsData.getDescription());

				_opInfoTable.appendRow(row);
				displayOpsData(opsData);
				_opsTable.setRowSelectionInterval(selRow, selRow, false);
				_opsTable.updateSelection(selRow, 0, false, false);
			}
			else
			{
				clearPanel();
			}
		}
	}

	@Override
	public void tableRowDeleteClicked(int rowToDelete)
	{
		Object value = _opsTable.getValueAt(rowToDelete, 0);
		if (_fsg != null && value instanceof OperationsData)
		{
			OperationsData operationsData = (OperationsData) value;
			delete(operationsData, false);
		}
	}

	@Override
	public boolean delete(OperationsData operationsData, boolean deletingDueToOverwrite)
	{
		boolean retVal = false;
		List<BcData> bcDataUsingOpsData = _fsg.getBcDataUsingOperationsData(operationsData);
		List<EnsembleSet> eSetsUsingBcData = bcDataUsingOpsData.stream().map(bcData -> _fsg.getEnsembleSetsUsingBcData(bcData))
				.flatMap(List::stream)
				.collect(Collectors.toList());
		String initialMessage;
		if(deletingDueToOverwrite)
		{
			initialMessage = operationsData.getName() + " already exists." + "Do you want to overwrite it?";
		}
		else
		{
			initialMessage = "Do you want to delete operations data " + operationsData.getName() + "?";
		}
		if(displayDeleteMessage(initialMessage, bcDataUsingOpsData, eSetsUsingBcData, deletingDueToOverwrite,
				operationsData))
		{
			retVal = true;
			performDelete(_fsg, operationsData, _opsTable, bcDataUsingOpsData, eSetsUsingBcData);
		}
		return retVal;
	}

	private void displayOpsData(OperationsData opsData)
	{
		String opsFilePath = Project.getCurrentProject().getAbsolutePath(opsData.getOperationsFile());
		Sheet sheet = null;
		if(opsFilePath == null || opsFilePath.isEmpty())
		{
			return;
		}
		try
		{
			if(opsFilePath.endsWith(".csv"))
			{
				sheet = readCsv(opsFilePath);
			}
			else
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
		finally
		{
			setCursor(Cursor.getDefaultCursor());
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
		Font font = workbook.createFont();
		java.awt.Font fontToUse = UIManager.getFont("Table.font");
		int fontSize = fontToUse.getSize();
		Integer scalePercent = Integer.getInteger(OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT, DEFAULT_OPS_TABLE_FONT_CONVERSION_SCALE_PERCENT);
		double scale = scalePercent/100.0;
		font.setFontName(fontToUse.getFontName());
		font.setFontHeightInPoints((short) (fontSize * scale));

		// Apply the font to the cell
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setFont(font);

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
				cell.setCellStyle(cellStyle);
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
