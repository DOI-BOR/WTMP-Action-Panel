package usbr.wat.plugins.actionpanel.ui.forecast;

import hec.lang.NamedType;          // Provides NamedType as the base class supplying a human-readable name and integer index

/**
 * Represents a named HEC-DSS data location, pairing a display name with the DSS file
 * path and record pathname that together identify a specific time-series or paired-data
 * record within the WTMP forecast action panel.
 *
 * {@code DssLocation} extends {@link NamedType} so that instances can be used directly
 * in UI components (such as combo boxes and tables) that rely on {@code getName()} for
 * display, while also carrying the DSS file and path references needed to read the
 * underlying data.
 *
 * @see hec.lang.NamedType
 */
public class DssLocation extends NamedType {
	// The file-system path to the HEC-DSS file that contains this location's data record
	private String _dssFile;

	// The full HEC-DSS pathname (A/B/C/D/E/F parts) identifying the record within the DSS file
	private String _dssPath;

	/**
	 * Constructs a {@code DssLocation} with the given display name, DSS file path, and
	 * DSS record pathname.
	 *
	 * @param name    the human-readable display name for this location; passed to the
	 *                {@link NamedType} superclass constructor and returned by
	 *                {@link #getName()}
	 * @param dssFile the file-system path to the HEC-DSS file containing this location's
	 *                data; should not be {@code null} or empty
	 * @param dssPath the full HEC-DSS pathname (A/B/C/D/E/F) identifying the specific
	 *                record within the DSS file; should not be {@code null} or empty
	 */
	public DssLocation(String name, String dssFile, String dssPath) {
		// Delegate name storage to the NamedType superclass
		super(name);

		// Store the DSS file path for this location
		_dssFile = dssFile;

		// Store the full DSS pathname for this location's data record
		_dssPath = dssPath;
	}

	/**
	 * Returns the full HEC-DSS pathname (A/B/C/D/E/F parts) that identifies this
	 * location's data record within the DSS file.
	 *
	 * @return the DSS pathname string; not {@code null} if set via the constructor
	 */
	public String getDssPath() {
		return _dssPath;
	}

	/**
	 * Returns the file-system path to the HEC-DSS file that contains this location's
	 * data record.
	 *
	 * @return the DSS file path string; not {@code null} if set via the constructor
	 */
	public String getDssFile() {
		return _dssFile;
	}
}
