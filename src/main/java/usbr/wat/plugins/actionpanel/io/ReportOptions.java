/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.io;

/**
 * options that are sent to the report when creating one
 * @author mark
 *
 */
public class ReportOptions
{
	private OutputType _outputType = OutputType.PDF;
	private boolean _printHeadersAndFooters;

	public ReportOptions()
	{
		super();
	}

	/**
	 * @param selectedItem
	 */
	public void setOutputType(OutputType outputType)
	{
		_outputType = outputType;
	}
	public OutputType getOutputType()
	{
		return _outputType;
	}

	/**
	 * @param selected
	 */
	public void setPrintHeadersFooters(boolean printHeadersFooters)
	{
		_printHeadersAndFooters = printHeadersFooters;
	}
	
	public boolean shouldPrintHeadersFooters()
	{
		return _printHeadersAndFooters;
	}
}
