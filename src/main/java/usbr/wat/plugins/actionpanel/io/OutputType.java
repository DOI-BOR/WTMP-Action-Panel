package usbr.wat.plugins.actionpanel.io;

import net.sf.jasperreports.engine.DefaultJasperReportsContext; // Import Jasper Reports context instance used for export operations
import net.sf.jasperreports.engine.JRExporter; // Import exporter interface for generating output files from Jasper templates
import net.sf.jasperreports.engine.JasperPrint; // Import filled print object containing rendered report data
import net.sf.jasperreports.engine.export.HtmlExporter; // Import HTML exporter class for creating web page output formats
import net.sf.jasperreports.engine.export.JRPdfExporter; // Import PDF exporter class for creating PDF document files
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter; // Import DOCX exporter for Microsoft Word format support
import net.sf.jasperreports.export.DocxReportConfiguration; // Import configuration properties for DOCX export settings
import net.sf.jasperreports.export.SimpleExporterInput; // Import input wrapper for Jasper print data during export
import net.sf.jasperreports.export.SimpleHtmlExporterOutput; // Import output wrapper for HTML export destination
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput; // Import output wrapper for PDF/DOCX file stream writing

import rma.util.RMAIO; // Import RMA utility for file path manipulation operations


/**
 * OutputType is an enumeration defining supported export formats for Jasper Reports.
 * Each output type provides its own exporter configuration and extension handling.
 * The enum supports three formats: PDF, Word Document (DOCX), and HTML.
 *
 * Each concrete instance extends the base enum with format-specific buildExporter logic.
 */

public enum OutputType {
	PDF // Enum constant for PDF document output type
			{
				@Override
				public JRExporter buildExporter(JasperPrint jp, String partial) {
					// Generate full output filename from partial path including extension
					String filename = getFilename(partial);

					// Create PDF exporter with Jasper context instance
					JRPdfExporter exporter = new JRPdfExporter(DefaultJasperReportsContext.getInstance());

					// Set the filled print as input for export process
					exporter.setExporterInput(new SimpleExporterInput(jp));

					// Return configured PDF exporter instance
					return exporter;
				}

				@Override
				public String getFileExtension() {
					return ".pdf"; // Return standard PDF extension
				}

				@Override
				public String toString() {
					// Return human-readable name for console/debug output
					return "PDF";
				}
			},

	Doc // Enum constant for Word Document output type (DOCX format)
			{
				@Override
				public JRExporter buildExporter(JasperPrint jp, String partial) {
					// Generate full output filename from partial path
					String filename = getFilename(partial);

					// Get global Jasper reports context instance
					DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();

					// Configure DOCX exporter to disable frame nesting in tables
					context.setProperty(DocxReportConfiguration.PROPERTY_FRAMES_AS_NESTED_TABLES, "false");

					// Create DOCX exporter with configured context
					JRDocxExporter exporter = new JRDocxExporter(context);

					// Set the filled print as input for export
					exporter.setExporterInput(new SimpleExporterInput(jp));

					// Set output stream file path
					exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(filename));

					// Return configured DOCX exporter instance
					return exporter;
				}

				@Override
				public String getFileExtension() {
					// Return Microsoft Word Open XML document extension
					return ".docx";
				}

				@Override
				public String toString() {
					// Return human-readable name
					return "Word Document";
				}
			},

	Html // Enum constant for HTML output type (web page format)
			{
				@Override
				public JRExporter buildExporter(JasperPrint jp, String partial) {
					// Generate full output filename from partial path
					String filename = getFilename(partial);

					// Create HTML exporter with Jasper context
					HtmlExporter exporter = new HtmlExporter(DefaultJasperReportsContext.getInstance());

					// Set the filled print as input for export
					exporter.setExporterInput(new SimpleExporterInput(jp));

					// Set HTML output wrapper with filename
					exporter.setExporterOutput(new SimpleHtmlExporterOutput(filename));

					// Return configured HTML exporter instance
					return exporter;
				}

				@Override
				public String getFileExtension() {
					// Return HTML file extension
					return ".htm";
				}

				@Override
				public String toString() {
					// Return human-readable name
					return "HTML";
				}
			};

	/**
	 * Returns a properly configured JRExporter instance capable of exporting JasperPrint into this specific output format.
	 * Each subclass provides its own exporter type (PDF, DOCX, or HTML).
	 *
	 * @param jp      Filled JasperPrint object containing the rendered report with data populated
	 *                Exporters typically want to set the JasperPrint as a parameter on their internal configuration.
	 * @param partial Path to the output file or folder where the exported document will be saved
	 * @return JRExporter instance ready for the export() method to generate the output file
	 */
	public abstract JRExporter buildExporter(JasperPrint jp, String partial);

	/**
	 * Calculates the full filename for exported files based on the provided partial path and type-specific extension.
	 * Extracts directory from path, gets base filename without extension, then appends the appropriate extension.
	 *
	 * @param partialName Partial path to the output file or directory (e.g., "folder/report")
	 * @return Fully qualified output filename with correct extension (e.g., "folder/report.pdf" or "folder/report.docx")
	 */
	public String getFilename(String partialName) {
		// Extract directory portion from input path
		String directory = RMAIO.getDirectoryFromPath(partialName);

		// Get base filename without extension
		String fileNameNoExtension = RMAIO.getFileNameNoExtension(partialName);

		// Combine directory, name and type-specific extension
		return RMAIO.concatPath(directory, fileNameNoExtension) + getFileExtension();
	}

	/**
	 * Provides the file extension string appropriate for this output type.
	 * Used in conjunction with getFilename() to construct complete output paths.
	 *
	 * @return String representation of the file extension (e.g., ".pdf", ".docx", ".htm")
	 */
	public abstract String getFileExtension();

	/**
	 * Factory method that parses an export extension string and returns corresponding OutputType constant.
	 * Used for type-safe output format selection based on file extensions.
	 * Falls back to PDF if extension is null or unrecognized.
	 *
	 * @param outputExt File extension string (e.g., ".pdf", ".docx", ".htm")
	 * @return OutputType enum value corresponding to the requested format
	 */
	public static OutputType getOutputType(String outputExt) {
		// If no extension provided, default to PDF format
		if (outputExt == null) {
			return OutputType.PDF;
		}

		// Find the correct format
		if (outputExt.equalsIgnoreCase(Doc.getFileExtension())) {
			// Check if extension matches Word Document type (case-insensitive)
			return Doc;
		} else if (outputExt.equalsIgnoreCase(Html.getFileExtension())) {
			// Check if extension matches HTML type (case-insensitive)
			return Html;
		} else {
			// For any other extension or unrecognized format, default to PDF
			return OutputType.PDF;
		}
	}
}