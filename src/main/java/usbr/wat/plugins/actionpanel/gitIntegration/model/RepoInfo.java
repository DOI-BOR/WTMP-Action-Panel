/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.model;

import hec.lang.NamedType;

/**
 * class to hold the Git Repository info
 * 
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class RepoInfo extends NamedType
{
	private String _sourceUrl;
	private String _localPath;
	
	
	public RepoInfo()
	{
		super();
	}


	/**
	 * @return the sourceUrl
	 */
	public String getSourceUrl()
	{
		return _sourceUrl;
	}


	/**
	 * @param sourceUrl the sourceUrl to set
	 */
	public void setSourceUrl(String sourceUrl)
	{
		_sourceUrl = sourceUrl;
	}


	/**
	 * @return the localPath
	 */
	public String getLocalPath()
	{
		return _localPath;
	}


	/**
	 * @param localPath the localPath to set
	 */
	public void setLocalPath(String localPath)
	{
		_localPath = localPath;
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
}
