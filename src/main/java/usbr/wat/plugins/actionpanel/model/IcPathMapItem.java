package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;  // Resizable-array List used to store destination DSS file/path pairs
import java.util.HashMap;   // Hash map (imported for potential future use via _dssIdMap)
import java.util.List;      // Ordered collection interface for the destination DSS item list
import java.util.Map;       // Map interface for the source-to-destination DSSIdentifier map

import hec.heclib.dss.DSSPathname; // HEC DSS pathname parser; used for part-by-part equality comparison
import hec.io.DSSIdentifier;       // Encapsulates a DSS file name and path for identifying a record
import hec.lang.NamedType;         // HEC base class providing a name field; the name stores the reservoir name
import rma.util.RMAIO;             // RMA I/O utility; used for parsing integer values from string tokens

/**
 * Represents a single row in the IC (Initial Conditions) config CSV file.
 *
 * Each row associates a reservoir name with one or more destination DSS records.
 * The CSV layout for a data row is:
 *
 *   Reservoir, Number of Destinations, Dest DSS File 1, Dest DSS Path 1, Dest DSS File 2, Dest DSS Path 2, ...
 *
 * The reservoir name is stored as the NamedType name. Destination entries are stored
 * in a list of DssItem objects. The minimum valid number of CSV fields is defined by
 * MIN_NUM_PARTS.
 *
 * Also provides static utilities for comparing DSS path strings by A/B/C/E/F part
 * equivalence (ignoring the D-part date component).
 *
 * This class is suppressed for serialization warnings because NamedType is not
 * consistently serializable.
 */
@SuppressWarnings("serial")
public class IcPathMapItem extends NamedType {
	// Minimum number of CSV fields required for a valid data row:
	// reservoir name, number-of-destinations, at least one file, at least one path
	public static final int MIN_NUM_PARTS = 4;

	// List of destination DSS file/path pairs for this reservoir
	private List<DssItem> _destDssItems = new ArrayList<>();

	// Map from destination to source DSSIdentifier (currently populated externally; not filled in parseLine)
	private Map<DSSIdentifier, DSSIdentifier> _dssIdMap = new HashMap<>();

	/**
	 * Constructs an empty IcPathMapItem; fields are populated via parseLine().
	 */
	public IcPathMapItem() {
		super();
	}

	/**
	 * Returns the reservoir name for this path map item.
	 *
	 * The reservoir name is stored as the NamedType name and set during parseLine().
	 *
	 * @return the reservoir name string
	 */
	public String getReservoirName() {
		return getName();
	}

	/**
	 * Parses a CSV field array into this IcPathMapItem's reservoir name and destination list.
	 *
	 * Expects the array in the format:
	 * [0] Reservoir name
	 * [1] Number of destination pairs
	 * [2..2+numDests-1] Alternating DSS file path and DSS path strings
	 * <p>
	 * Returns false immediately if the array is null or has fewer than MIN_NUM_PARTS fields.
	 *
	 * @param parts the CSV field array parsed from a single config file line
	 * @return true if parsing succeeded; false if the input is null or too short
	 */
	public boolean parseLine(String[] parts) {
		if (parts == null || parts.length < MIN_NUM_PARTS) {
			return false;
		}

		// Store the trimmed reservoir name as the NamedType name
		setName(parts[0].trim());

		// Read the number of destination DSS pairs from the second field
		int numDests = RMAIO.parseInt(parts[1].trim());

		// Parse each destination DSS file/path pair starting at index 2, stepping by 2
		for (int i = 2; i < 2 + numDests; i += 2) {
			DssItem dssItem = new DssItem(parts[i].trim(), parts[i + 1].trim());
			_destDssItems.add(dssItem);
		}

		return true;
	}

	/**
	 * Returns the number of destination DSS records associated with this reservoir.
	 *
	 * @return the count of DssItem entries in the destination list
	 */
	public int getNumberOfDests() {
		return _destDssItems.size();
	}

	/**
	 * Returns the DSS file path for the given destination index.
	 *
	 * @param num the zero-based index into the destination list
	 * @return the DSS file path string for that destination
	 */
	public String getDestDssFile(int num) {
		return _destDssItems.get(num).getDssFile();
	}

	/**
	 * Returns the DSS path string for the given destination index.
	 *
	 * @param num the zero-based index into the destination list
	 * @return the DSS path string for that destination
	 */
	public String getDestDssPath(int num) {
		return _destDssItems.get(num).getDssPath();
	}

	/**
	 * Returns the destination-to-source DSSIdentifier map.
	 *
	 * This map is not populated by parseLine(); it is intended to be filled externally
	 * by callers that need to track source records alongside destination records.
	 *
	 * @return the Map from destination DSSIdentifier to source DSSIdentifier
	 */
	public Map<DSSIdentifier, DSSIdentifier> getDssIdMap() {
		return _dssIdMap;
	}

	/**
	 * Compares two DSS path strings for equality across the A, B, C, E, and F parts,
	 * ignoring the D-part (date/time component).
	 *
	 * Parses each string into a DSSPathname and delegates to the pathname overload.
	 *
	 * @param path1 the first DSS path string
	 * @param path2 the second DSS path string
	 * @return true if all compared parts are equal (case-insensitive); false otherwise
	 */
	public static boolean dssPathsEqual(String path1, String path2) {
		// Parse both strings and clear their collection-sequence fields before comparing
		DSSPathname src1 = new DSSPathname(path1);
		src1.setCollectionSequence(null);

		DSSPathname src2 = new DSSPathname(path2);
		src2.setCollectionSequence(null);

		return dssPathsEqual(src1, src2);
	}

	/**
	 * Compares two DSSPathname objects for equality across the A, B, C, E, and F parts,
	 * ignoring the D-part (date/time component).
	 *
	 * All comparisons are case-insensitive.
	 *
	 * @param path1 the first DSSPathname to compare
	 * @param path2 the second DSSPathname to compare
	 * @return true if A, B, C, E, and F parts all match; false otherwise
	 */
	public static boolean dssPathsEqual(DSSPathname path1, DSSPathname path2) {
		return (path1.getAPart().equalsIgnoreCase(path2.getAPart())
				&& path1.getBPart().equalsIgnoreCase(path2.getBPart())
				&& path1.getCPart().equalsIgnoreCase(path2.getCPart())
				&& path1.getEPart().equalsIgnoreCase(path2.getEPart())
				&& path1.getFPart().equalsIgnoreCase(path2.getFPart()));
	}

	/**
	 * Simple data holder for a single destination DSS file-and-path pair.
	 *
	 * Created during parseLine() for each destination entry in a config CSV row.
	 */
	class DssItem {
		// The absolute or relative DSS file path for this destination
		private String _dssFile;

		// The DSS path string identifying the record within the DSS file
		private String _dssPath;

		/**
		 * Constructs a DssItem with the given DSS file path and DSS record path.
		 *
		 * @param dssFile the DSS file path string
		 * @param dssPath the DSS record path string
		 */
		DssItem(String dssFile, String dssPath) {
			super();
			_dssFile = dssFile;
			_dssPath = dssPath;
		}

		/**
		 * Returns the DSS file path for this destination.
		 *
		 * @return the DSS file path string
		 */
		public String getDssFile() {
			return _dssFile;
		}

		/**
		 * Returns the DSS record path for this destination.
		 *
		 * @return the DSS record path string
		 */
		public String getDssPath() {
			return _dssPath;
		}
	}
}
