/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model;

/**
 * @author Mark Ackerman
 *
 */
public class PositionAnalysisSettings extends BaseComputeSettings
{
	
	
	
	public PositionAnalysisSettings()
	{
		super();
	}

	@Override
	protected String getSettingElementString()
	{
		return "PositionAnalysisSetting";
	}

	@Override
	public String getCollectionDssFilename()
	{
		return "positionAnalysisResults.dss";
	}
	

	
	
	
}
