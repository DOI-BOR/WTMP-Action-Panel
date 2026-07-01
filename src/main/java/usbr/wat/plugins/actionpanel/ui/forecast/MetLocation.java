package usbr.wat.plugins.actionpanel.ui.forecast;

import java.util.ArrayList;         // Provides ArrayList, a resizable-array implementation used to store DssLocation entries
import java.util.List;              // Provides the List interface for the ordered collection of DssLocation objects

import hec.lang.NamedType;          // Provides NamedType, the HEC base class that supplies name/description fields and accessors

/**
 * Represents a meteorological station location within the Forecast Action Panel.
 * Each MetLocation has a name (inherited from NamedType) and owns an ordered list
 * of DssLocation entries, where each DssLocation describes a specific met parameter
 * and its corresponding source DSS file and record path.
 *
 * MetLocation objects are built by MeteorologyPanel when parsing the met config CSV
 * and are passed to MetPlotPanel to populate the station navigator.
 *
 * @see DssLocation
 * @see MeteorologyPanel
 */
public class MetLocation extends NamedType {
	/**
	 * Ordered list of DSS source locations associated with this met station.
	 * Each entry maps a met parameter to a specific DSS file and record pathname.
	 */
	private List<DssLocation> _dssLocations = new ArrayList<>();

	/**
	 * Constructs a new MetLocation with no name and an empty DSS location list.
	 * The name should be set via the inherited NamedType.setName method after construction.
	 */
	public MetLocation() {
		super();
	}

	/**
	 * Returns the list of DSS locations associated with this met station.
	 * The returned list is the live backing list; modifications will affect this object.
	 *
	 * @return the List of DssLocation entries for this met station; never null, may be empty
	 */
	public List<DssLocation> getDssLocations() {
		return _dssLocations;
	}

	/**
	 * Replaces the current DSS location list with the provided collection.
	 * Clears all existing entries before adding the new ones. If the supplied
	 * list is null, the internal list is cleared and left empty.
	 *
	 * @param locations the new List of DssLocation entries to set, or null to clear
	 */
	public void setDssLocations(List<DssLocation> locations) {
		// Remove all existing DSS locations before applying the replacement list
		_dssLocations.clear();

		if (locations != null) {
			_dssLocations.addAll(locations);
		}
	}

	/**
	 * Appends a single DssLocation to the end of this met station's location list.
	 * Used when building the location list incrementally, one CSV row at a time.
	 *
	 * @param location the DssLocation to append; should not be null
	 */
	public void addDssLocation(DssLocation location) {
		_dssLocations.add(location);
	}
}
