/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.io;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.export.DocxReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import rma.util.RMAIO;


/**
 * @author mark
 *
 */
public enum OutputType
{
	PDF
	{
		@Override
		public JRExporter buildExporter(JasperPrint jp, String partial)
		{
			String filename = getFilename(partial);
			JRPdfExporter exporter = new JRPdfExporter(DefaultJasperReportsContext.getInstance());
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(filename));

			//exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			//exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, filename);
			return exporter;
		}

		@Override
		public String getFileExtension()
		{
			return ".pdf";
		}
		@Override
		public String toString()
		{
			return "PDF";
		}
	},
	
	
	
	Doc
	{
		@Override
		public JRExporter buildExporter(JasperPrint jp, String partial)
		{
			String filename = getFilename(partial);
			DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
			context.setProperty(DocxReportConfiguration.PROPERTY_FRAMES_AS_NESTED_TABLES, "false");
			JRDocxExporter exporter = new JRDocxExporter(context);

			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(filename));
			//exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			//exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, filename);

			return exporter;
		}

		@Override
		public String getFileExtension()
		{
			return ".docx";
		}
		@Override
		public String toString()
		{
			return "Word Document";
		}
	},
	
	Html
	{
		@Override
		public JRExporter buildExporter(JasperPrint jp, String partial)
		{
			String filename = getFilename(partial);
			HtmlExporter exporter = new HtmlExporter(DefaultJasperReportsContext.getInstance());
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleHtmlExporterOutput(filename));
			//exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			//exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, filename);
			return exporter;
		}

		@Override
		public String getFileExtension()
		{
			return ".htm";
		}
		@Override
		public String toString()
		{
			return "HTML";
		}
	};

	/**
	 * Returns a JRExporter capable of exprting a JasperPrint into this type of output.
	 *
	 * @param jp
	 *            Filled JasperPrint object. Exporters typically want to set the JasperPrint as a
	 *            parameter.
	 * @param partial
	 *            path to the output file or folder.
	 * @return JRExporter ready for the export() call to be made.
	 */
	public abstract JRExporter buildExporter(JasperPrint jp, String partial);

	/**
	 * OutputTypes are responsible for figuring out where their output should go.
	 *
	 * @param partialName
	 *            path to the output file or directory
	 * @return outputFile name.
	 */
	public String getFilename(String partialName)
	{
		String directory = RMAIO.getDirectoryFromPath(partialName);
		String fileNameNoExtension = RMAIO.getFileNameNoExtension(partialName);
		return RMAIO.concatPath(directory, fileNameNoExtension) + getFileExtension();
	}

	/**
	 * Each type has its own filename extension.
	 *
	 * @return
	 */
	public abstract String getFileExtension();
}
