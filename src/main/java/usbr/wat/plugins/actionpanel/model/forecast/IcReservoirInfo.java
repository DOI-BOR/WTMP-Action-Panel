package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList; // Resizable-array List for accumulating IC profile file names
import java.util.List;      // Ordered collection interface for the profile file name list

/**
 * Data object associating a reservoir name with the set of available Initial
 * Conditions (IC) profile file names for that reservoir.
 *
 * Read from the IC reservoirs CSV file (ForecastConfigFiles.IC_RESERVOIRS_FILENAME).
 * Each row in that file identifies one reservoir and lists the profile files (e.g.,
 * DSS input records or config files) from which the user can select an initial
 * conditions profile in the forecast editor.
 *
 * Profile file names are added via addProfileFileName(); blank or null names are
 * silently ignored.
 *
 */
public class IcReservoirInfo {
	// The display name of the reservoir; matches the name used in the IC path map config
	private String _reservoirName;

	// List of profile file names available for selection for this reservoir
	private List<String> _profileFileNames = new ArrayList<>();

	/**
	 * Constructs an empty IcReservoirInfo with no reservoir name or profile files.
	 */
	public IcReservoirInfo() {
		super();
	}

	/**
	 * Sets the name of the reservoir for this IC info entry.
	 *
	 * @param reservoirName the reservoir display name string
	 */
	public void setReservoirName(String reservoirName) {
		_reservoirName = reservoirName;
	}

	/**
	 * Returns the name of the reservoir for this IC info entry.
	 *
	 * @return the reservoir name string, or null if not set
	 */
	public String getReservoirName() {
		return _reservoirName;
	}

	/**
	 * Adds a profile file name to this reservoir's available profile list.
	 *
	 * Null values and blank strings are silently ignored.
	 *
	 * @param fileName the profile file name to add; ignored if null or blank
	 */
	public void addProfileFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return;
		}
		_profileFileNames.add(fileName);
	}

	/**
	 * Returns the list of available profile file names for this reservoir.
	 *
	 * @return the List of profile file name strings; empty if none have been added
	 */
	public List<String> getProfileFileNames() {
		return _profileFileNames;
	}
}