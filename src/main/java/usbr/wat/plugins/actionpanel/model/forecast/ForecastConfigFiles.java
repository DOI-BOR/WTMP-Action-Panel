package usbr.wat.plugins.actionpanel.model.forecast;

// Import Project model class to access project root directory for absolute path conversions
import com.rma.model.Project; // Import model representing the current WAT project context for path resolution utilities

// Import RMA IO utility class for handling file path concatenation and directory operations
import rma.util.RMAIO; // Import utility methods for combining path strings and extracting directories


/**
 * ForecastConfigFiles is a utility class that defines and retrieves the various paths to configuration
 * and CSV files used during forecast computation workflows. It encapsulates file naming conventions for
 * boundary conditions, initial conditions, temperature targets, historical meteorological data, and flow patterns.
 *
 * This class provides static factory methods to obtain both relative (system property based) and absolute
 * (project directory rooted) paths for various forecast inputs and configuration files.
 *
 * File paths are determined by system properties which can be overridden, falling back to defined defaults
 * in this utility if no custom property is set.
 */

public class ForecastConfigFiles {
	// Base folder path relative to project directory where forecast configuration files are stored
	public static final String BASE_FOLDER = "forecast/config"; // Root subdirectory for all forecast config files

	// Filename for the boundary condition paths mapping configuration file
	public static final String BC_PATHS_MAP_FILENAME = "bcPathsMap.config"; // Maps source/destination DSS records for BCs

	// Filename for the initial condition paths mapping configuration file
	public static final String IC_PATHS_MAP_FILENAME = "icPathsMap.config"; // Maps source/destination DSS records for Initial Conditions

	// Filename for the temperature targets configuration file
	public static final String TEMP_TARGETS_FILENAME = "target_temp.config"; // Configures model alternative temp target mappings

	// Filename for the temperature target control locations configuration file
	public static final String TEMP_TARGETS_CONTROL_LOCS_FILENAME = "temp_target_control_locs.config"; // Maps control location overrides

	// Filename for historical meteorological data configuration
	public static final String HISTORICAL_MET_FILENAME = "historic.config"; // Config source for historic MET data

	// Filename for flow pattern configuration
	public static final String FLOW_PATTERN_FILENAME = "flow_pattern.config"; // Defines flow pattern settings

	// Filename for yearly temperature data CSV file used in analysis
	public static final String YEARLY_TEMP_FILENAME = "yearly_temperature_data.csv"; // CSV dataset for temporal temp variations

	// Filename for initial condition reservoir definitions CSV file
	public static final String IC_RESERVOIRS_FILENAME = "icReservoirs.csv"; // CSV defining reservoirs for IC mapping

	// Filename for historical meteorological data editor configuration (alternative name)
	public static final String MET_EDITOR_FILENAME = "historic.config"; // Same as HISTORICAL_MET_FILENAME used in specific context

	// Subdirectory within BASE_FOLDER where meteorology configuration files are stored
	private static final String MET_CONFIG_FILES_FOLDER = "met"; // Directory containing MET-specific config subfolder

	// Private constructor prevents instantiation of utility class
	private ForecastConfigFiles() {
	} // Enforce non-instantiability for static-only access methods

	/**
	 * Retrieves the full relative path string for the boundary condition paths map configuration file.
	 * Checks system property "WTMP.bcPathsMapFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/bcPathsMap.config")
	 */
	public static String getRelativeBCConfigFile() {
		String file = System.getProperty("WTMP.bcPathsMapFile", BASE_FOLDER + "/" + BC_PATHS_MAP_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the full relative path string for the initial condition paths map configuration file.
	 * Checks system property "WTMP.icPathsMapFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/icPathsMap.config")
	 */
	public static String getRelativeICConfigFile() {
		String file = System.getProperty("WTMP.icPathsMapFile", BASE_FOLDER + "/" + IC_PATHS_MAP_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the full relative path string for the temperature targets configuration file.
	 * Checks system property "WTMP.tempTargetPathsMapFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/target_temp.config")
	 */
	public static String getRelativeTempTargetConfigFile() {
		String file = System.getProperty("WTMP.tempTargetPathsMapFile", BASE_FOLDER + "/" + TEMP_TARGETS_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the full relative path string for the temperature target control locations configuration file.
	 * Checks system property "WTMP.tempTargetControlLocsPathsMapFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/temp_target_control_locs.config")
	 */
	public static String getRelativeTempTargetControlLocsFile() {
		String file = System.getProperty("WTMP.tempTargetControlLocsPathsMapFile", BASE_FOLDER + "/" + TEMP_TARGETS_CONTROL_LOCS_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the boundary condition configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeBCConfigFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getBCConfigFile() {
		String file = getRelativeBCConfigFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the absolute file system path for the initial condition configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeICConfigFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getICConfigFile() {
		String file = getRelativeICConfigFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the absolute file system path for the temperature targets configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeTempTargetConfigFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getTempTargetConfigFile() {
		String file = getRelativeTempTargetConfigFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Converts a relative file path string into an absolute file system path based on the current project context.
	 * This ensures files are resolved within the user's active WAT project directory regardless of working directory settings.
	 *
	 * @param file The relative path string to be converted (e.g., "forecast/config/target_temp.config")
	 * @return String containing the absolute path suitable for File objects or FileSystem APIs
	 */
	private static String makeAbsolute(String file) {
		String absFile = Project.getCurrentProject().getAbsolutePath(file); // Convert relative to absolute using project root
		return absFile; // Return absolute path string
	}

	/**
	 * Retrieves the full relative path string for the historical meteorological data configuration file.
	 * Checks system property "WTMP.historicalMetPathsMapFile" first, falling back to default constant if not set.
	 * Includes subfolder 'met' in default path.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/met/historic.config")
	 */
	public static String getRelativeHistoricalMetFile() {
		String file = System.getProperty("WTMP.historicalMetPathsMapFile", BASE_FOLDER + "/" + MET_CONFIG_FILES_FOLDER + "/" + HISTORICAL_MET_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the historical meteorological data configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeHistoricalMetFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getHistoricalMetFile() {
		String file = getRelativeHistoricalMetFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the full relative path string for the flow pattern configuration file.
	 * Checks system property "WTMP.FlowPatternMapFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/flow_pattern.config")
	 */
	public static String getRelativeFlowPatternFile() {
		String file = System.getProperty("WTMP.FlowPatternMapFile", BASE_FOLDER + "/" + FLOW_PATTERN_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the flow pattern configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeFlowPatternFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getFlowPatternFile() {
		String file = getRelativeFlowPatternFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the full relative path string for the yearly temperature data CSV file.
	 * Checks system property "WTMP.FlowPatternMapFile" first (note: matches Flow Pattern property key in original),
	 * falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/yearly_temperature_data.csv")
	 */
	public static String getRelativeYearlyTempDataFile() {
		String file = System.getProperty("WTMP.FlowPatternMapFile", BASE_FOLDER + "/" + YEARLY_TEMP_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the yearly temperature data CSV file.
	 * Uses project root directory combined with the relative path returned by getRelativeYearlyTempDataFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getYearlyTempDataFile() {
		String file = getRelativeYearlyTempDataFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the full relative path string for the IC Reservoirs CSV configuration file.
	 * Checks system property "WTMP.IcReservoirsFile" first, falling back to default constant if not set.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/icReservoirs.csv")
	 */
	public static String getRelativeIcReservoirsFile() {
		String file = System.getProperty("WTMP.IcReservoirsFile", BASE_FOLDER + "/" + IC_RESERVOIRS_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the IC Reservoirs CSV configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeIcReservoirsFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getIcReservoirsFile() {
		String file = getRelativeIcReservoirsFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the full relative path string for the Met Editor configuration file.
	 * Checks system property "WTMP.MetEditorFile" first, falling back to default constant if not set.
	 * Includes subfolder 'met' in default path.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/met/historic.config")
	 */
	public static String getRelativeMetEditorFile() {
		String file = System.getProperty("WTMP.MetEditorFile", BASE_FOLDER + "/" + MET_CONFIG_FILES_FOLDER + "/" + MET_EDITOR_FILENAME); // Retrieve property or fallback
		return file; // Return resulting relative path string
	}

	/**
	 * Retrieves the absolute file system path for the Met Editor configuration file.
	 * Uses project root directory combined with the relative path returned by getRelativeMetEditorFile().
	 *
	 * @return String containing the full absolute path suitable for file I/O operations
	 */
	public static String getMetEditorFile() {
		String file = getRelativeMetEditorFile(); // Get relative path from property
		return makeAbsolute(file); // Convert to absolute path using helper method
	}

	public static String getMetConfigFilesFolder() {
		String dir = getRelativeMetConfigFilesFolder(); // Retrieve subfolder relative path
		return makeAbsolute(dir); // Convert to absolute path using helper method
	}

	/**
	 * Retrieves the relative path string pointing to the 'met' configuration files subfolder.
	 * Combines base folder with met subdirectory constant for consistent path construction.
	 *
	 * @return String containing the relative path (e.g., "forecast/config/met")
	 */
	private static String getRelativeMetConfigFilesFolder() {
		String dir = RMAIO.concatPath(BASE_FOLDER, MET_CONFIG_FILES_FOLDER); // Concatenate base folder and met subfolder
		return dir; // Return constructed relative folder path
	}
}