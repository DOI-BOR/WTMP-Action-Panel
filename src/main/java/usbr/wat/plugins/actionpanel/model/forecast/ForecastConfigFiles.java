/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.model.Project;
import rma.util.RMAIO;

/**
 * class to get the various paths to the config and csv files used for the forecast compute
 */
public class ForecastConfigFiles
{
	public static final String BASE_FOLDER = "forecast/config";

	public static final String BC_PATHS_MAP_FILENAME = "bcPathsMap.config";
	public static final String IC_PATHS_MAP_FILENAME = "icPathsMap.config";
	public static final String TEMP_TARGETS_FILENAME = "target_temp.config";
	public static final String TEMP_TARGETS_CONTROL_LOCS_FILENAME = "temp_target_control_locs.config";

	public static final String HISTORICAL_MET_FILENAME = "historic.config";
	public static final String FLOW_PATTERN_FILENAME = "flow_pattern.config";

	public static final String YEARLY_TEMP_FILENAME = "yearly_temperature_data.csv";

	public static final String IC_RESERVOIRS_FILENAME = "icReservoirs.csv";

	public static final String MET_EDITOR_FILENAME = "met_editor.config";
	private static final String MET_CONFIG_FILES_FOLDER = "met";

	private ForecastConfigFiles()
	{ }

	/**
	 *
	 * @return the full relative path to the BC .config file
	 */
	public static String getRelativeBCConfigFile()
	{
		String file = System.getProperty("WTMP.bcPathsMapFile", BASE_FOLDER+"/"+BC_PATHS_MAP_FILENAME);
		return file;
	}
	/**
	 *
	 * @return the full relative path to the IC .config file
	 */
	public static String getRelativeICConfigFile()
	{
		String file = System.getProperty("WTMP.icPathsMapFile", BASE_FOLDER+"/"+IC_PATHS_MAP_FILENAME);
		return file;
	}
	/**
	 *
	 * @return the relative path to the temp targets .config file
	 */
	public static String getRelativeTempTargetConfigFile()
	{
		String file = System.getProperty("WTMP.tempTargetPathsMapFile", BASE_FOLDER+"/"+TEMP_TARGETS_FILENAME);
		return file;
	}

	public static String getRelativeTempTargetControlLocsFile()
	{
		String file = System.getProperty("WTMP.tempTargetControlLocsPathsMapFile", BASE_FOLDER+"/"+TEMP_TARGETS_CONTROL_LOCS_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the forecast BC .config file
	 */
	public static String getBCConfigFile()
	{
		String file = getRelativeBCConfigFile();
		return makeAbsolute(file);
	}
	/**
	 *
	 * @return the full path to the forecast IC .config file
	 */
	public static String getICConfigFile()
	{
		String file = getRelativeICConfigFile();
		return makeAbsolute(file);
	}
	/**
	 *
	 * @return the full path to the temp targets .config file
	 */
	public static String getTempTargetConfigFile()
	{
		String file = getRelativeTempTargetConfigFile();
		return makeAbsolute(file);
	}

	/**
	 *
	 * @param file
	 * @return
	 */
	private static String makeAbsolute(String file)
	{
		String absFile = Project.getCurrentProject().getAbsolutePath(file);
		return absFile;

	}

	/**
	 *
	 * @return the relative path to the historical met .config file
	 */
	public static String getRelativeHistoricalMetFile()
	{
		String file = System.getProperty("WTMP.historicalMetPathsMapFile", BASE_FOLDER+"/met/"+HISTORICAL_MET_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the historical met .config file
	 */
	public static String getHistoricalMetFile()
	{
		String file = getRelativeHistoricalMetFile();
		return makeAbsolute(file);
	}

	/**
	 *
	 * @return the relative path to the flow pattern .config file
	 */
	public static String getRelativeFlowPatternFile()
	{
		String file = System.getProperty("WTMP.FlowPatternMapFile", BASE_FOLDER+"/"+FLOW_PATTERN_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the flow pattern .config file
	 */
	public static String getFlowPatternFile()
	{
		String file = getRelativeFlowPatternFile();
		return makeAbsolute(file);
	}

	/**
	 *
	 * @return the relative path to the yearly temperature .csv file
	 */
	public static String getRelativeYearlyTempDataFile()
	{
		String file = System.getProperty("WTMP.FlowPatternMapFile", BASE_FOLDER+"/"+YEARLY_TEMP_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the yearly temperature .csv file
	 */
	public static String getYearlyTempDataFile()
	{
		String file = getRelativeYearlyTempDataFile();
		return makeAbsolute(file);
	}

	/**
	 *
	 * @return the relative path to the IC Reservoirs .csv file
	 */
	public static String getRelativeIcReservoirsFile()
	{
		String file = System.getProperty("WTMP.IcReservoirsFile", BASE_FOLDER+"/"+IC_RESERVOIRS_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the IC Reservoirs .csv file
	 */
	public static String getIcReservoirsFile()
	{
		String file = getRelativeIcReservoirsFile();
		return makeAbsolute(file);
	}

	/**
	 *
	 * @return the relative path to the Met Editor config file
	 */
	public static String getRelativeMetEditorFile()
	{
		String file = System.getProperty("WTMP.MetEditorFile", BASE_FOLDER+"/"+MET_EDITOR_FILENAME);
		return file;
	}

	/**
	 *
	 * @return the full path to the Met Editor config file
	 */
	public static String getMetEditorFile()
	{
		String file = getRelativeMetEditorFile();
		return makeAbsolute(file);
	}

	public static String getMetConfigFilesFolder()
	{
		String dir = getRelativeMetConfigFilesFolder();
		return makeAbsolute(dir);
	}

	private static String getRelativeMetConfigFilesFolder()
	{
		String dir =  RMAIO.concatPath(BASE_FOLDER, MET_CONFIG_FILES_FOLDER);
		return dir;
	}
}
