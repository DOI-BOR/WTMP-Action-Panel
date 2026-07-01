package usbr.wat.plugins.actionpanel.model;

import java.io.BufferedReader;   // Buffered character reader for line-by-line reading of the IC config CSV file
import java.io.IOException;      // Checked exception thrown when the config file cannot be read
import java.util.ArrayList;     // Resizable-array List for accumulating IcPathMapItem entries and destination identifiers
import java.util.HashMap;       // Hash map implementation (imported but not directly used in this class)
import java.util.List;          // Ordered collection interface for DSS identifier and path-map item lists
import java.util.Map;           // Map interface (imported but not directly used in this class)

import com.google.common.flogger.FluentLogger; // Google Flogger for structured warning and info logging

import com.rma.io.FileManagerImpl; // RMA file manager for resolving RmaFile references from path strings
import com.rma.io.RmaFile;         // RMA abstraction for a file path; used to open the config file reader

import hec.heclib.dss.DSSPathname;       // HEC DSS pathname parser (imported for use by IcPathMapItem)
import hec.hecmath.DSS;                  // HEC DSS utility (imported but not directly referenced here)
import hec.io.DSSIdentifier;             // Encapsulates a DSS file name and path for identifying a DSS record
import hec2.model.DataLocation;          // Represents a model data location (imported for potential future use)
import hec2.model.DssDataLocation;       // DSS-specific data location subtype (imported for potential future use)
import hec2.wat.model.WatSimulation;     // WAT simulation object; used for attaching error messages on failure

import usbr.wat.plugins.actionpanel.model.forecast.DssPathMap;      // Forecast DSS path map (imported for context)
import usbr.wat.plugins.actionpanel.model.forecast.DssPathMapItem;   // Forecast DSS path map item (imported for context)
import usbr.wat.plugins.actionpanel.model.forecast.InitialConditions; // Holds reservoir-to-profile mappings; used to resolve source DSS records
import usbr.wat.plugins.actionpanel.model.forecast.Profile;          // Carries the DSS file name and path for a selected initial-conditions profile

/**
 * Reads and provides access to the Initial Conditions (IC) configuration file,
 * which maps reservoir names to their source and destination DSS records.
 *
 * The IC config file is a CSV where each data row specifies a reservoir name,
 * the number of destination DSS entries, and one or more (DSS file, DSS path)
 * destination pairs. The first row (header) is skipped; blank lines and lines
 * starting with '#' are treated as comments.
 *
 * Once parsed via readDssPathsFile(), this class provides:
 *   - getSourceDSSIdentifierFor(reservoirName): looks up the source DSS record
 *     for a reservoir from the associated InitialConditions profile.
 *   - getDestDssIdentifiersFor(reservoirName): returns all destination DSS records
 *     for a reservoir from the parsed path map list.
 */
public class IcPathMap {
	// Logger for recording parse warnings and I/O errors
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	// The WAT simulation; used to attach error messages when the file cannot be read
	private final WatSimulation _sim;

	// Absolute path to the IC config CSV file
	private final String _configFile;

	// The InitialConditions object holding reservoir-to-profile mappings for source DSS lookup
	private final InitialConditions _ic;

	// Parsed list of IC path map items; one entry per reservoir row in the config file
	List<IcPathMapItem> _dssPathMapList = new ArrayList<>();

	// Source DSS file path (currently unused; reserved for future direct source-file tracking)
	private String _sourceDssFile;

	// Source DSS F-part override (currently unused; reserved for future use)
	private String _sourceDssFPart;

	/**
	 * Constructs an IcPathMap for the given simulation, config file path, and initial conditions.
	 *
	 * @param sim          the WAT simulation used to record error messages on failure
	 * @param icConfigPath the absolute path to the IC config CSV file
	 * @param ic           the InitialConditions holding reservoir profile mappings for source DSS lookup
	 */
	public IcPathMap(WatSimulation sim, String icConfigPath, InitialConditions ic) {
		super();
		_configFile = icConfigPath;
		_sim = sim;
		_ic = ic;
	}

	/**
	 * Reads and parses the IC config CSV file into the internal path map list.
	 * <p>
	 * Skips the header row, blank lines, and comment lines (starting with '#').
	 * Each data line is split by comma and parsed into an IcPathMapItem. Lines with
	 * fewer than IcPathMapItem.MIN_NUM_PARTS fields are logged as warnings and skipped.
	 * Attaches an error message to the simulation on file-not-found or I/O errors.
	 *
	 * @return true if the file was read and parsed successfully; false if the file does
	 * not exist, cannot be opened, or an I/O error occurs
	 */
	public boolean readDssPathsFile() {
		// Resolve the config file reference and verify it exists
		RmaFile file = FileManagerImpl.getFileManager().getFile(_configFile);

		if (!file.exists()) {
			_sim.addErrorMessage("DSS Paths Map file " + file.getAbsolutePath() + " doesn't exist.");
			return false;
		}

		// Open a buffered reader for line-by-line parsing
		BufferedReader reader = file.getBufferedReader();
		if (reader == null) {
			_sim.addErrorMessage("Failed to get reader for DSS Paths map file " + file.getAbsolutePath());
			return false;
		}

		// Define holders for the file read
		String line;
		String[] parts;
		_dssPathMapList = new ArrayList<>();
		IcPathMapItem dssPathMapItem;

		// CSV column layout:
		// Reservoir, Number of Destinations, Destination DSS file, Destination DSS record
		try {
			// Skip the header row
			reader.readLine();

			// Continue to read while there are liens available
			while ((line = reader.readLine()) != null) {
				// Skip blank lines and comment lines
				if (line.trim().isEmpty() || line.startsWith("#")) {
					continue;
				}

				// Split the line into CSV fields
				parts = line.split(",");

				// Skip lines with too few fields to be valid
				if (parts == null || parts.length < IcPathMapItem.MIN_NUM_PARTS) {
					LOGGER.atWarning().log("Invalid line found: " + line);
					continue;
				}

				// Parse the line into a new IcPathMapItem and add it if parsing succeeds
				dssPathMapItem = new IcPathMapItem();
				if (dssPathMapItem.parseLine(parts)) {
					_dssPathMapList.add(dssPathMapItem);
				}
			}
			return true;

		} catch (IOException ioe) {
			// Log the I/O failure and attach a message to the simulation for reporting
			LOGGER.atWarning().withCause(ioe).log("Error reading " + file.getAbsolutePath());
			_sim.addErrorMessage("Error reading DSS Paths map file " + file.getAbsolutePath() + " error:" + ioe);
			return false;

		} finally {
			// Always close the reader to release the file handle
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Returns the source DSSIdentifier for the given reservoir name by looking up the
	 * reservoir's selected initial-conditions profile in the associated InitialConditions.
	 *
	 * @param reservoirName the name of the reservoir to look up; null returns null
	 * @return a DSSIdentifier carrying the profile's DSS file and path, or null if the name is null
	 */
	public DSSIdentifier getSourceDSSIdentifierFor(String reservoirName) {
		// Return automatically if no reservoir name is provided
		if (reservoirName == null) {
			return null;
		}

		// Retrieve the selected profile for this reservoir from the initial conditions
		Profile profile = _ic.getSelectedProfile(reservoirName);

		// Build and return a DSSIdentifier from the profile's DSS file and path
		String dssFile = profile.getDssFileName();
		String dssPath = profile.getDssPath();
		DSSIdentifier srcDssId = new DSSIdentifier(dssFile, dssPath);
		return srcDssId;
	}


	/**
	 * Returns all destination DSSIdentifiers for the given reservoir name by scanning
	 * the parsed path map list for matching entries.
	 *
	 * Multiple items in the map may share the same reservoir name (e.g., multiple
	 * destination files for the same reservoir). Each (file, path) pair is added to
	 * the result list only if it is not already present (no duplicates).
	 *
	 * @param reservoirName the name of the reservoir to look up; null returns an empty list
	 * @return a List of DSSIdentifier objects representing all unique destinations for the reservoir
	 */
	public List<DSSIdentifier> getDestDssIdentifiersFor(String reservoirName) {
		// List to accumulate unique destination DSS identifiers for the matching reservoir
		List<DSSIdentifier> destDssIds = new ArrayList<>();

		// Return an empty list immediately when no reservoir name was provided
		if (reservoirName == null) {
			return destDssIds;
		}

		// Reusable references declared outside the loop to avoid repeated allocation
		IcPathMapItem dssItem;
		DSSIdentifier destDssId;

		for (int i = 0; i < _dssPathMapList.size(); i++) {
			// Retrieve the current path map entry and its associated reservoir name
			dssItem = _dssPathMapList.get(i);
			String resName = dssItem.getReservoirName();

			// Case-insensitive match on reservoir name
			if (reservoirName.equalsIgnoreCase(resName)) {
				// Add each destination DSS record (file + path) for this item
				for (int j = 0; j < dssItem.getNumberOfDests(); j++) {
					destDssId = new DSSIdentifier(dssItem.getDestDssFile(j), dssItem.getDestDssPath(j));

					// Avoid adding duplicate identifiers
					if (!destDssIds.contains(destDssId)) {
						destDssIds.add(destDssId);
					}
				}
			}
		}

		return destDssIds;
	}
}
