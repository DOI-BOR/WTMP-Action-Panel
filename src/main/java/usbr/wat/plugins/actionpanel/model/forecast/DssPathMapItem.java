package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList; // Import ArrayList for creating dynamic lists to store destination DSS item entries
import java.util.HashMap; // Import HashMap for mapping destination identifiers to source identifiers during lookups
import java.util.List; // Import List interface for working with collections of DSSItem objects
import java.util.Map; // Import Map interface for key-value storage between DSSIdentifier objects

import hec.heclib.dss.DSSPathname; // Import DSSPathname utility class for constructing and manipulating record path names with components
import hec.io.DSSIdentifier; // Import identifier class representing a specific time series record by file and path combination
import hec.lang.NamedType; // Import NamedType base class providing common object identification and persistence functionality

import rma.util.RMAIO; // Import utility class for string parsing operations including parseInt and path comparison methods

/**
 * DssPathMapItem is an inner structure representing a single source-to-destination mapping rule in the DSS paths map file.
 * Each instance represents one row from the CSV configuration file defining how a source DSS record should be copied or used
 * for multiple destination records (location, parameter combinations). Extends NamedType to support optional XML persistence.
 *
 * The item contains:
 *
 *   Source DSS file and path identifiers
 *   List of destination records with their file and path components
 *   Map for quick lookup of source identifier given a destination location
 *
 */

public class DssPathMapItem extends NamedType {
	// Field storing the functional part string that identifies which dataset within the source file to use
	private String _srcDssFpart; // Stores FPart component for the source DSS file used in path construction

	// Field storing the filename portion of the source DSS record identifier
	private String _srcDssFile; // Contains base filename string from configuration (e.g., "sim1.dss")

	// Field storing the directory path portion of the source DSS record identifier
	private String _srcDssPath; // Contains directory path string from configuration (e.g., "/dir/data/records")

	// List of destination item objects each containing a destination file and path pair
	private List<DssItem> _destDssItems = new ArrayList<>(); // Collection holding multiple destination records for this source

	/**
	 * Map of destination DSS identifiers to source DSS identifier
	 */
	private Map<DSSIdentifier, DSSIdentifier> _dssIdMap = new HashMap<>(); // Lookup map from dest ID to corresponding source ID for efficient retrieval

	public DssPathMapItem(String sourceDssFile, String sourceDssFPart) {
		super(); // Invoke superclass default constructor
		_srcDssFile = sourceDssFile; // Store source filename from parameter
		_srcDssFpart = sourceDssFPart; // Store source FPart from parameter (functional part identifier)
	}

	public boolean parseLine(String[] parts) {
		// Validate that array is not null and has minimum required field count
		if (parts == null || parts.length < 7) {
			return false; // Return failure if insufficient fields provided for parsing

		}

		//Location, parameter, Source DSS file, Source DSS record, Number of Destinations, Destination DSS file, Destination DSS record, ...
		// 0         1               2                3                   4                     5                       6

		setName(parts[0].trim() + "-" + parts[1].trim()); // Set name field by concatenating location and parameter with hyphen

		// Check if source file is not yet initialized
		if (_srcDssFile == null) {
			_srcDssFile = parts[2].trim(); // Use CSV field value to populate source filename
		}

		_srcDssPath = parts[3].trim(); // Extract directory path from CSV field 3

		// Check if FPart string was provided in constructor or set elsewhere
		if (_srcDssFpart != null) {
			DSSPathname pathname = new DSSPathname(); // Create temporary pathname object for reconstructing full record identifier
			pathname.setPathname(_srcDssPath); // Set directory path component on object
			pathname.setFPart(_srcDssFpart); // Set functional part component on object
			_srcDssPath = pathname.getPathname(); // Update path field with reconstructed full name
		}

		int numDests = RMAIO.parseInt(parts[4].trim()); // Parse the number of destination records defined for this source

		// Loop through destination fields in pairs (file at i, path at i+1)
		for (int i = 5; i < 5 + numDests; i += 2) {
			DssItem dssItem = new DssItem(parts[i].trim(), parts[i + 1].trim()); // Create new item from trimmed file and path strings
			_destDssItems.add(dssItem); // Add newly created item to collection
		}

		return true; // Return success after parsing completed without errors
	}

	public void setSourceDssFile(String dssFile) {
		_srcDssFile = dssFile; // Assign new filename string to instance variable
	}

	public String getSrcDssPath() {
		return _srcDssPath; // Return the current path field value
	}

	public String getSrcDssFile() {
		return _srcDssFile; // Return the current file name field value
	}

	public int getNumberOfDests() {
		return _destDssItems.size(); // Return size of destination list collection
	}

	public String getDestDssFile(int num) {
		return _destDssItems.get(num).getDssFile(); // Get file name from DssItem object at given position
	}

	public String getDestDssPath(int num) {
		return _destDssItems.get(num).getDssPath(); // Get directory path from DssItem object at given position
	}

	public Map<DSSIdentifier, DSSIdentifier> getDssIdMap() {
		return _dssIdMap; // Return reference to populated map for use in copy operations
	}

	/**
	 * Searches destinations for a matching location and parameter combination, then returns the corresponding source DSS identifier.
	 * Uses both filename and full path (including APART, BPART, CPART, EPART, FPART components) to validate match.
	 *
	 * @param dssFile Base filename of the destination record being searched for
	 * @param dssPath Directory path string of the destination record
	 * @return DSSIdentifier representing the source record that maps to this destination, or null if not found
	 */
	public DSSIdentifier hasDestLocation(String dssFile, String dssPath) {
		// Validate both parameters are present and non-null
		if (dssFile == null || dssPath == null) {
			return null; // Return null immediately if any parameter is missing
		}

		DssItem destItem; // Declare loop variable for destination item access
		DSSPathname pathname1 = new DSSPathname(); // Create temporary object to construct query pathname with provided values
		DSSPathname pathname2 = new DSSPathname(); // Create second temporary object for comparison after iterating

		pathname1.setPathname(dssPath); // Set directory path component on first pathname object from parameter

		// Iterate through all destination items in collection
		for (int i = 0; i < _destDssItems.size(); i++) {
			destItem = _destDssItems.get(i); // Get current destination item for this iteration

			// Check if filename components match ignoring full path
			if (RMAIO.pathsEqual(destItem.getDssFile(), dssFile)) {
				pathname2.setPathname(destItem.getDssPath()); // Set second pathname with destination directory component

				// Compare full path components after setting both objects
				if (dssPathsEqual(pathname1, pathname2)) {
					DSSIdentifier srcDssId = new DSSIdentifier(_srcDssFile, _srcDssPath); // Create source identifier using internal stored values
					DSSIdentifier destDssId = new DSSIdentifier(destItem.getDssFile(), destItem.getDssPath()); // Create destination identifier from item
					_dssIdMap.put(destDssId, srcDssId); // Store mapping in lookup table before returning result
					return srcDssId; // Return source identifier on successful match
				}
			}
		}

		return null; // Return null after checking all destinations without finding a match
	}

	/**
	 * Compares two path string representations for equality by normalizing both to use collection-aware comparison.
	 * Strips any existing collection sequence from paths before comparing component parts.
	 */
	public static boolean dssPathsEqual(String path1, String path2) {
		DSSPathname src1 = new DSSPathname(path1); // Create first pathname object from string parameter
		src1.setCollectionSequence(null); // Clear any collection sequence to focus on core components

		DSSPathname src2 = new DSSPathname(path2); // Create second pathname object from string parameter
		src2.setCollectionSequence(null); // Clear any collection sequence for fair comparison

		return dssPathsEqual(src1, src2); // Delegate to overloaded method that accepts pathname objects
	}

	/**
	 * Compares two DSSPathname objects for equality by checking all component parts: APART, BPART, CPART, EPART, FPART.
	 * Uses case-insensitive comparison (equalsIgnoreCase) to handle mixed-case path strings consistently.
	 */
	public static boolean dssPathsEqual(DSSPathname path1, DSSPathname path2) {
		return (path1.getAPart().equalsIgnoreCase(path2.getAPart()) // Compare time component part
				&& path1.getBPart().equalsIgnoreCase(path2.getBPart()) // Compare location component part
				&& path1.getCPart().equalsIgnoreCase(path2.getCPart()) // Compare parameter component part
				&& path1.getEPart().equalsIgnoreCase(path2.getEPart()) // Compare time step component part
				&& path1.getFPart().equalsIgnoreCase(path2.getFPart())); // Compare functional part component
	}

	// Setter for source directory path field (void return per original signature)
	void setSourceDssPath(String pathname) {
		_srcDssPath = pathname; // Assign parameter value to instance variable directly
	}

	// Method adds new destination mapping to the list
	void addMapping(String destFile, String destPath) {
		_destDssItems.add(new DssItem(destFile, destPath)); // Create and add new DssItem to collection
	}

	// Inner static class representing a single destination record in the mapping
	class DssItem {
		private String _dssFile; // Field storing the directory path portion of the destination identifier
		private String _dssPath; // Field storing the base filename portion of the destination identifier

		// Constructor accepting both file and path string components
		DssItem(String dssFile, String dssPath) {
			super(); // Invoke superclass default constructor
			_dssFile = dssFile; // Store directory path from first parameter
			_dssPath = dssPath; // Store base filename from second parameter
		}

		// Getter method retrieves stored directory path string
		public String getDssFile() {
			return _dssFile; // Return current path field value
		}

		// Getter method retrieves stored base filename string
		public String getDssPath() {
			return _dssPath; // Return current file name field value
		}
	}
}
