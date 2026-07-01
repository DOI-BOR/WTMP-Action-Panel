package usbr.wat.plugins.actionpanel.io;

/**
 * ReportOptions is a configuration class that holds options passed to Jasper Reports when creating a report.
 * Controls output format (PDF, DOCX, HTML) and whether headers/footers should be included in the report.
 *
 */

public class ReportOptions {
	// Default output type is PDF unless otherwise specified
	private OutputType _outputType = OutputType.PDF; // Current export format setting (PDF, DOCX, or HTML)

	// Flag controlling inclusion of document headers and footers in exported reports
	private boolean _printHeadersAndFooters; // Boolean flag for whether to include headers and footers in output

	public ReportOptions() {
		super();
	}

	/**
	 * Sets the desired output format type for report generation.
	 * Available types: PDF, Word Document (DOCX), or HTML.
	 *
	 * @param outputType Enum value specifying the export format
	 */
	public void setOutputType(OutputType outputType) {
		_outputType = outputType; // Assign new export type to instance variable
	}

	// Method to retrieve current output type setting
	public OutputType getOutputType() {
		return _outputType; // Return currently configured output format
	}

	/**
	 * Sets whether the exported report should include headers and footers.
	 * Headers typically contain report title, page numbers, or company branding.
	 * Footers may contain document properties, date/time stamps, or contact information.
	 *
	 * @param printHeadersFooters Boolean value indicating whether to print headers and footers
	 */
	public void setPrintHeadersFooters(boolean printHeadersFooters) {
		_printHeadersAndFooters = printHeadersFooters; // Update the headers/footers flag
	}

	// Helper method for checking if headers/footers are enabled
	public boolean shouldPrintHeadersFooters() {
		return _printHeadersAndFooters; // Return current setting of the headers/footers flag
	}
}