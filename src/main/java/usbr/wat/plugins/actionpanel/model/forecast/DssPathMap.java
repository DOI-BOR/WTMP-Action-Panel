package usbr.wat.plugins.actionpanel.model.forecast;

import java.io.BufferedReader; // Import BufferedReader for reading text content from the DSS Paths Map configuration file line by line
import java.io.IOException; // Import IOException to handle exceptions that occur during file reading operations
import java.nio.file.Path; // Import Path interface for representing filesystem paths (not directly used but included in original imports)

import java.util.ArrayList; // Import ArrayList for creating dynamic lists to store DSS path mapping items
import java.util.HashMap; // Import HashMap for creating maps to associate source and destination DSS identifiers
import java.util.List; // Import List interface for working with collections of DSSIdentifier objects
import java.util.Map; // Import Map interface for key-value storage mappings between DSS identifiers

import com.google.common.flogger.FluentLogger; // Import FluentLogger from Guava Flogger library for logging warnings and errors during map operations

import com.rma.io.FileManagerImpl; // Import file manager implementation utility class to handle file access and validation
import com.rma.io.RmaFile; // Import RMA File wrapper object used to wrap file paths for the application's file system interface
import com.rma.model.Project; // Import Project model for accessing current project context (used in broader package context)

import hec.heclib.dss.DSSPathname; // Import DSSPathname utility class for constructing and modifying DSS record path names
import hec.hecmath.DSS; // Import DSS utility class for manipulating DSS data records (not directly used but kept for compatibility)
import hec.io.DSSIdentifier; // Import DSS identifier class representing a specific time series record by file and path
import hec2.model.DataLocation; // Import DataLocation model type representing an input or output data source in a simulation
import hec2.model.DssDataLocation; // Import concrete implementation of DataLocation that links to actual DSS files
import hec2.wat.model.WatSimulation; // Import WAT simulation base class used for addErrorMessage notifications and logging

import rma.util.RMAIO; // Import utility class for file path concatenation and string operations (part of RMA utility package)

/**
 * DssPathMap is a utility class that manages mappings between source and destination DSS records.
 * It reads configuration from a text-based map file defining how DSS records should be transformed or copied
 * for boundary condition override scenarios in forecast analysis workflows.
 *
 * This class provides methods to:
 *
 *   Read and parse the DSS Paths Map configuration file
 *   Retrieve source DSS identifiers for destination locations
 *   Build maps of source-to-destination relationships for bulk copy operations
 *   Filter mappings by specific source paths
 *
 *
 * The map configuration file uses CSV format with fields:
 * Location, parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record
 */

public class DssPathMap {
	// FluentLogger instance for logging warnings and errors from this class's operations
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass(); // Initialize logger using fluent API pattern

	// Reference to the WAT simulation object used for addErrorMessage notifications to the simulation model
	private final WatSimulation _sim; // Simulation instance used to report error messages to user

	// Path string to the configuration file that defines source-to-destination mappings
	private final String _configFile; // Configuration filename path used when reading the map file

	// List of DssPathMapItem objects parsed from the configuration file storing mapping rules
	List<DssPathMapItem> _dssPathMapList = new ArrayList<>(); // Collection of mapping rule objects parsed from file

	// Path string to the source DSS file that contains data for boundary condition override
	private String _sourceDssFile; // File path to original source DSS data file

	// Functional part identifier for the source DSS file used in record path construction
	private String _sourceDssFPart; // Functional part name identifying which dataset within source file to use

	public DssPathMap(WatSimulation sim, String configFile) {
		super(); // Invoke superclass default constructor
		_configFile = configFile; // Store configuration file path string in instance variable for later file access
		_sim = sim; // Reference to WatSimulation is final so cannot be modified after assignment
	}

	/**
	 * Reads the DSS Paths Map configuration file and parses it into a list of mapping items.
	 * Validates that the file exists and can be read, then processes each line containing source-to-destination mappings.
	 * Skips comment lines starting with '#' and empty lines during parsing.
	 *
	 * @return True if the configuration file was successfully read and parsed, false otherwise
	 */
	public boolean readDssPathsFile() {
		RmaFile file = FileManagerImpl.getFileManager().getFile(_configFile); // Get RMA File wrapper object for target path

		// Check if the configuration file exists on the filesystem
		if (!file.exists()) {
			addErrorMessage("DSS Paths Map file " + file.getAbsolutePath() + " doesn't exist."); // Log error message to simulation or logger
			return false; // Return failure status after logging missing file error
		}

		BufferedReader reader = file.getBufferedReader(); // Get buffered reader stream for reading text content

		// Check if buffer reader was returned as null
		if (reader == null) {
			addErrorMessage("Failed to get reader for DSS Paths map file " + file.getAbsolutePath()); // Log error about invalid reader object
			return false; // Return failure status when unable to create file reader
		}

		String line; // Variable to hold current line of text from configuration file
		String[] parts; // Variable to hold split CSV fields from each line
		_dssPathMapList = new ArrayList<>(); // Initialize empty list for parsed mapping items
		DssPathMapItem dssPathMapItem; // Declare variable for next item being parsed

		// Attempt reading within try block for resource management
		try {
			reader.readLine(); // Read and discard the first line (header comment)

			// Loop through remaining lines until end of file
			while ((line = reader.readLine()) != null) {
				// Check if line is empty after trimming whitespace or starts with comment marker
				if (line.trim().isEmpty() || line.startsWith("#")) {
					continue; // Skip empty or commented lines without processing
				}

				parts = line.split(","); // Split CSV line into array of string fields

				// Validate that split result is non-null and has minimum required fields
				if (parts == null || parts.length < 7) {
					LOGGER.atWarning().log("Invalid line found: " + line); // Log warning for malformed data line
					continue; // Skip invalid lines to continue processing subsequent entries
				}

				dssPathMapItem = new DssPathMapItem(_sourceDssFile, _sourceDssFPart); // Create mapping item using source file and FPart references

				// Delegate parsing of CSV fields to inner class method
				if (dssPathMapItem.parseLine(parts)) {
					_dssPathMapList.add(dssPathMapItem); // Add successfully parsed item to collection
				}
			}

			return true; // Return success when all lines processed without fatal errors

		} catch (IOException ioe) {
			LOGGER.atWarning().withCause(ioe).log("Error reading " + file.getAbsolutePath()); // Log exception with cause and path info
			_sim.addErrorMessage("Error reading DSS Paths map file " + file.getAbsolutePath() + " error:" + ioe); // Log detailed error to simulation model

			return false; // Return failure status after logging IO exception

		} finally {
			// Ensure resources are closed regardless of success or exception in try block
			// Attempt close operation inside nested try-catch to handle potential secondary exceptions
			try {
				reader.close(); // Close the buffered reader stream to prevent resource leaks

			} catch (IOException e) {
			} // Suppress secondary error since primary file access is already handled by outer catch block
		}

		// Private helper method adds messages to simulation model or logs them if sim is null
		private void addErrorMessage (String s) {
			// Check if simulation reference exists and can receive errors
			if (_sim != null) {
				_sim.addErrorMessage(s); // Send message directly to simulation model component

			} else {
				LOGGER.atSevere().log(s); // Log message at severe level when sim reference not available
			}
		}

		/**
		 * Retrieves a source DSS identifier for a given destination data location.
		 * Checks if the linked-to location is a valid DssDataLocation and searches the path map list.
		 * Returns null if destination mapping cannot be found or invalid link type detected.
		 *
		 * @param dataLoc The DataLocation object whose target source DSS should be found
		 * @return DSSIdentifier representing the source record to use for this destination, or null if not found
		 */
		public DSSIdentifier getDSSIdentifierFor (DataLocation dataLoc) {
			// Check if provided data location parameter is missing
			if (dataLoc == null) {
				return null; // Return null immediately for null input
			}

			// Verify linked location is actual DSS type
			if (!(dataLoc.getLinkedToLocation() instanceof DssDataLocation)) {
				return null; // Return null if link is not to a concrete DSS data location object
			}

			DssDataLocation linkedToLoc = (DssDataLocation) dataLoc.getLinkedToLocation(); // Cast and retrieve actual DSS location reference
			String dssPath = linkedToLoc.getDssPath(); // Extract directory path portion of record identifier
			String dssFile = linkedToLoc.get_dssFile(); // Extract filename portion from destination location object
			DSSIdentifier srcDssId = getSourceDssIdentifierFor(dssFile, dssPath); // Delegate to private method for lookup logic

			return srcDssId; // Return found source identifier or null from helper call
		}

		/**
		 * Searches the DSS path map list to find a source DSS identifier that matches the given destination file and path.
		 * Iterates through all mapping items checking if their destination entries match the provided criteria.
		 *
		 * @param dssFile The base filename for the destination DSS record (used for matching)
		 * @param dssPath The directory path component for the destination DSS record
		 * @return DSSIdentifier representing a source record that maps to this destination, or null if not found
		 */
		private DSSIdentifier getSourceDssIdentifierFor (String dssFile, String dssPath) {
			DssPathMapItem dssMapItem; // Declare loop variable for mapping item access

			// Iterate through all loaded map items in list
			for (int i = 0; i < _dssPathMapList.size(); i++) {
				dssMapItem = _dssPathMapList.get(i); // Get current mapping item from collection
				DSSIdentifier dssId = dssMapItem.hasDestLocation(dssFile, dssPath); // Check if this item has matching dest

				// If a matching source was found for these destination criteria
				if (dssId != null) {
					return dssId; // Return the matched source identifier immediately
				}
			}

			return null; // Return null after iterating through all items without finding a match
		}

		/**
		 * Builds a map associating each destination DSS identifier with its corresponding source identifier.
		 * This map is used during compute operations to track which source data should be copied to which destinations.
		 * The mapping includes both file and path components for complete record identification.
		 */
		public Map<DSSIdentifier, DSSIdentifier> getDssCopyMap () {
			DssPathMapItem dssItem; // Declare loop variable for map item access
			Map<DSSIdentifier, DSSIdentifier> dssCopyMap = new HashMap<>(); // Initialize empty map to store source-destination pairs
			Map<DSSIdentifier, DSSIdentifier> dssIdMap; // Placeholder for additional mapping operation (unused but preserved)

			int numDests; // Variable to hold count of destination records from a single source item
			String dssFile, dssPath; // Variables to hold file and path string components for creating identifiers
			DSSIdentifier srcDssId, destDssId; // Declare identifier objects for source and destination entries

			// Iterate through each mapping rule in the parsed list
			for (int i = 0; i < _dssPathMapList.size(); i++) {
				dssItem = _dssPathMapList.get(i); // Get current mapping item from collection
				srcDssId = new DSSIdentifier(dssItem.getSrcDssFile(), dssItem.getSrcDssPath()); // Create source identifier with name and path
				numDests = dssItem.getNumberOfDests(); // Get count of destination records defined for this source

				// Loop through each destination record associated with this source
				for (int d = 0; d < numDests; d++) {
					dssFile = dssItem.getDestDssFile(d); // Get base filename string for current destination
					dssPath = dssItem.getDestDssPath(d); // Get directory path string for current destination
					destDssId = new DSSIdentifier(dssFile, dssPath); // Create destination identifier combining name and path
					dssCopyMap.put(destDssId, srcDssId); // Map destination key to source value in output collection
				}
			}

			return dssCopyMap; // Return populated map with all destination-to-source mappings
		}

		/**
		 * Builds a complete DSS copy map where each destination identifier is mapped to its source identifier.
		 * Uses direct field access to build the entire mapping from all defined paths in the configuration file.
		 */
		public Map<DSSIdentifier, DSSIdentifier> getAllDssMap () {
			DssPathMapItem dssItem; // Declare loop variable for map item access
			Map<DSSIdentifier, DSSIdentifier> dssCopyMap = new HashMap<>(); // Initialize empty map to store source-destination pairs
			String srcDssFile, srcDssPath; // Variables for storing source file and path information
			String destDssFile, destDssPath; // Variables for storing destination file and path information
			DSSIdentifier srcDssId, destDssId; // Declare identifier objects for source and destination entries

			// Iterate through each mapping rule in the parsed list
			for (int i = 0; i < _dssPathMapList.size(); i++) {
				dssItem = _dssPathMapList.get(i); // Get current mapping item from collection
				srcDssFile = dssItem.getSrcDssFile(); // Extract source file name string from item configuration
				srcDssPath = dssItem.getSrcDssPath(); // Extract source path string from item configuration
				srcDssId = new DSSIdentifier(srcDssFile, srcDssPath); // Create source identifier using name and path

				// Loop through each destination record defined
				for (int d = 0; d < dssItem.getNumberOfDests(); d++) {
					destDssFile = dssItem.getDestDssFile(d); // Get base filename string for current destination
					destDssPath = dssItem.getDestDssPath(d); // Get directory path string for current destination
					destDssId = new DSSIdentifier(destDssFile, destDssPath); // Create destination identifier combining name and path
					dssCopyMap.put(destDssId, srcDssId); // Map destination key to source value in output collection
				}
			}

			return dssCopyMap; // Return populated map with all destination-to-source mappings
		}

		/**
		 * Sets the source DSS file path that will be used as the origin for copy operations.
		 */
		public void setSourceDssFile (String sourceDssFile) {
			_sourceDssFile = sourceDssFile; // Assign parameter value to instance variable
		}

		/**
		 * Sets the functional part string for the source DSS file used in record path construction.
		 * Functional parts allow multiple datasets to coexist within a single DSS file.
		 */
		public void setSourceFPart (String sourceDssFPart) {
			_sourceDssFPart = sourceDssFPart; // Assign parameter value to instance variable
		}

		/**
		 * Retrieves all destination DSS identifiers for a given source path.
		 */
		public List<DSSIdentifier> getDestDssIdentifiersFor (String srcDssPath) {
			return getDestDssIdentifiersFor(srcDssPath, null); // Delegate to overloaded version with null override parameter
		}

		/**
		 * Retrieves all destination DSS identifiers that map from a given source path.
		 * Optionally accepts a time step override for filtering by ePart (time component) when matching records.
		 */
		public List<DSSIdentifier> getDestDssIdentifiersFor (String srcDssPath, String overrideTimeStep) {
			List<DSSIdentifier> destDssIds = new ArrayList<>(); // Initialize empty list for collecting results

			// Check if source path parameter is missing or invalid
			if (srcDssPath == null) {
				return destDssIds; // Return empty list immediately without processing
			}

			DssPathMapItem dssItem; // Declare loop variable for map item access
			DSSIdentifier destDssId; // Declare identifier variable for matching results
			String srcDssItemPath; // Variable to hold path string from mapping item configuration

			// Iterate through each mapping rule in the parsed list
			for (int i = 0; i < _dssPathMapList.size(); i++) {
				dssItem = _dssPathMapList.get(i); // Get current mapping item from collection
				srcDssItemPath = dssItem.getSrcDssPath(); // Extract source path string from item configuration
				DSSPathname srcDssPathname = new DSSPathname(srcDssItemPath); // Create pathname object for manipulation with time step component

				// Check if optional time step override parameter is provided
				if (overrideTimeStep != null) {
					srcDssPathname.setEPart(overrideTimeStep); // Set the time step (ePart) on pathname object using provided value
				}

				srcDssItemPath = srcDssPathname.getPathname(); // Reconstruct full path string with modified ePart component

				// Check if item path matches input path after normalization
				if (DssPathMapItem.dssPathsEqual(srcDssItemPath, srcDssPath)) {
					// Loop through all destinations for this matching source
					for (int j = 0; j < dssItem.getNumberOfDests(); j++) {
						destDssId = new DSSIdentifier(dssItem.getDestDssFile(j), dssItem.getDestDssPath(j)); // Create destination identifier with name and path

						// Check if this destination is not already in the result list
						if (!destDssIds.contains(destDssId)) {
							destDssIds.add(destDssId); // Add to results only once to avoid duplicate entries
						}
					}
				}
			}

			return destDssIds; // Return populated list of matching destination identifiers
		}

		/**
		 * Retrieves all source DSS identifiers that are defined in the configuration file.
		 * This is useful for discovering what input data sources will be read during a copy operation.
		 */
		public List<DSSIdentifier> getSourceDssIdentifiers () {
			List<DSSIdentifier> srcDssIds = new ArrayList<>(); // Initialize empty list for collecting results
			DssPathMapItem dssItem; // Declare loop variable for map item access
			DSSIdentifier destDssId; // Placeholder identifier (unused but declared in original code)
			String srcDssItemPath, srcDssItemFile; // Variables to hold path and file name from mapping configuration

			// Iterate through each mapping rule in the parsed list
			for (int i = 0; i < _dssPathMapList.size(); i++) {
				dssItem = _dssPathMapList.get(i); // Get current mapping item from collection
				srcDssItemFile = dssItem.getSrcDssFile(); // Extract source file name string from item configuration
				srcDssItemPath = dssItem.getSrcDssPath(); // Extract source path string from item configuration
				srcDssIds.add(new DSSIdentifier(srcDssItemFile, srcDssItemPath)); // Add identifier to results collection using name and path
			}

			return srcDssIds; // Return populated list of all unique source identifiers
		}
	}
}
