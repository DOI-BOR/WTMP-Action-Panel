/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.editors;

import rma.util.RMAIO;

public class TemplateWrapper
{
	private final String _name;
	private final String _path;

	public TemplateWrapper(String templatePath)
	{
		super();
		_name = RMAIO.getFileFromPath(templatePath);
		_path = templatePath;
	}

	public String getPath()
	{
		return _path;
	}

	public String toString()
	{
		return _name;
	}

}
