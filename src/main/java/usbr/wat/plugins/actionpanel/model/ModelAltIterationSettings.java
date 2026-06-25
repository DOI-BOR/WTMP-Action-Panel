package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;        // Resizable-array List for returning the data-location key set as an ordered list
import java.util.HashMap;          // Hash map backing the data-location-to-DSS-identifier settings table
import java.util.Iterator;         // Iterator for walking the settings map entries during save and updateDataLocations
import java.util.List;             // Ordered collection interface for data location lists
import java.util.Map;              // Map interface for the data-location-to-identifier settings table
import java.util.Map.Entry;        // Map entry type used during XML serialization
import java.util.Set;              // Set of map keys returned by entrySet() and keySet()
import java.util.logging.Logger;   // JDK logger for fine-level diagnostic messages in updateDataLocations

import org.jdom.Element;           // JDOM XML Element used for serializing and deserializing settings

import com.rma.util.XMLUtilities;  // RMA XML utility for reading and writing named-type data and child elements

import hec.io.DSSIdentifier;       // Encapsulates a DSS file name and path for identifying an iteration DSS record
import hec.lang.NamedType;         // HEC base class providing a name field; the name stores the model alternative name

import hec2.model.DataLocation;    // Represents a model data location (input boundary point)
import hec2.plugin.model.ModelAlternative;      // Represents a model alternative configuration
import hec2.plugin.util.DataLocationUtilities;  // Utility for reconstructing DataLocation objects from XML elements

/**
 * Per-model-alternative iteration boundary condition settings for a single WAT simulation.
 *
 * Stores a map from each DataLocation (a model input boundary point) to the DSS
 * identifier (file + path) of the iteration time-series record that should be used
 * for that location during an iterative compute run.
 *
 * Instances are keyed by model alternative name (stored as the NamedType name) within
 * a containing IterationSettings or PositionAnalysisSettings object.
 *
 * Supports full XML serialization (saveData/loadData) for persistence within the
 * simulation group's .simgrp file.
 *
 * This class is suppressed for serialization warnings because NamedType is not
 * consistently serializable.
 *
 */
@SuppressWarnings("serial")
public class ModelAltIterationSettings extends NamedType {
	// Map from each DataLocation to its assigned iteration DSSIdentifier;
	// initially populated with empty identifiers by fillSettingsTable()
	private Map<DataLocation, DSSIdentifier> _dataLocationSettings = new HashMap<>();

	/**
	 * Constructs an empty ModelAltIterationSettings with no data locations.
	 */
	public ModelAltIterationSettings() {
		super();
	}

	/**
	 * Constructs a ModelAltIterationSettings pre-populated with empty DSS identifiers
	 * for each of the given data locations.
	 *
	 * @param dataLocs the list of DataLocation objects to register in the settings table
	 */
	public ModelAltIterationSettings(List<DataLocation> dataLocs) {
		this();
		fillSettingsTable(dataLocs);
	}

	/**
	 * Populates the settings table with an entry for each DataLocation in the given list,
	 * initialising each one with a blank DSSIdentifier as a placeholder. Null DataLocation
	 * entries in the list are silently skipped. The resulting map provides a base state
	 * that is later filled in with real DSS file and path values when the user makes selections.
	 *
	 * @param dataLocs the list of DataLocation objects to register in the settings table;
	 *                 null entries within the list are ignored
	 */
	private void fillSettingsTable(List<DataLocation> dataLocs) {
		DataLocation dl;

		for (int i = 0; i < dataLocs.size(); i++) {
			// Retrieve the current DataLocation entry from the list
			dl = dataLocs.get(i);

			if (dl != null) {
				// Initialize each location with a blank DSS identifier as a placeholder
				_dataLocationSettings.put(dl, new DSSIdentifier("", ""));
			}
		}
	}

	/**
	 * Returns the DSSIdentifier currently associated with the given DataLocation in the
	 * settings table. Returns null when dataLoc is null or when no entry exists in the
	 * settings table for the given location (i.e. it was not registered via fillSettingsTable).
	 *
	 * @param dataLoc the DataLocation whose associated DSSIdentifier is requested;
	 *                may be null
	 * @return the DSSIdentifier mapped to the given DataLocation, or null if the location
	 * is null or not found in the settings table
	 */
	public DSSIdentifier getDSSIdentifierFor(DataLocation dataLoc) {
		// Return null immediately when no location was provided
		if (dataLoc == null) {
			return null;
		}

		// Look up and return the DSS identifier associated with this data location
		DSSIdentifier dssId = _dataLocationSettings.get(dataLoc);
		return dssId;
	}

	/**
	 * Returns all data locations registered in this settings object.
	 *
	 * @return a List of DataLocation keys from the settings map
	 */
	public List<DataLocation> getDataLocations() {
		Set<DataLocation> keys = _dataLocationSettings.keySet();
		List<DataLocation> l = new ArrayList<>(keys);
		return l;
	}

	/**
	 * Associates a DSSIdentifier with the given DataLocation in the settings map.
	 * If dssId is null, the existing entry for the DataLocation is removed, effectively
	 * clearing the DSS mapping for that location. If dataLoc is null, the method returns
	 * immediately without modifying the map.
	 *
	 * @param dataLoc the DataLocation key to associate or clear in the settings map;
	 *                no action is taken if null
	 * @param dssId   the DSSIdentifier to associate with the DataLocation, or null to
	 *                remove the existing mapping for that location
	 */
	public void setDssIdentifierFor(DataLocation dataLoc, DSSIdentifier dssId) {
		// Guard against a null DataLocation; there is no valid key to update
		if (dataLoc == null) {
			return;
		}

		if (dssId == null) {
			// A null identifier signals that the mapping should be cleared for this location
			_dataLocationSettings.remove(dataLoc);
		} else {
			// Associate the provided DSS identifier with the given data location
			_dataLocationSettings.put(dataLoc, dssId);
		}
	}

	/**
	 * Serializes all data location settings to XML under the given parent element.
	 *
	 * Creates a "DataLocations" child element containing one "DataLocationSetting"
	 * child per map entry. Each entry includes the DataLocation's XML representation
	 * and the assigned DSS file path and record path as child text elements.
	 *
	 * @param parentElem the XML Element under which the "DataLocations" element is added
	 */
	public void saveData(Element parentElem) {
		Set<Entry<DataLocation, DSSIdentifier>> entrySet = _dataLocationSettings.entrySet();
		Iterator<Entry<DataLocation, DSSIdentifier>> iter = entrySet.iterator();
		Entry<DataLocation, DSSIdentifier> entry;
		DataLocation dl;
		DSSIdentifier dssId;

		// Create the container element for all data location entries
		Element entriesElem = new Element("DataLocations");
		parentElem.addContent(entriesElem);

		String fileName;
		String dssPath;
		while (iter.hasNext()) {
			entry = iter.next();
			dl = entry.getKey();
			dssId = entry.getValue();

			// Create a per-entry element and populate it with the location XML and DSS identifiers
			Element entryElem = new Element("DataLocationSetting");
			entriesElem.addContent(entryElem);

			// Serialize the DataLocation into the entry element
			dl.toXML(entryElem);

			fileName = dssId.getFileName();
			dssPath = dssId.getDSSPath();

			// Write the DSS file and path, defaulting to empty strings for null values
			XMLUtilities.addChildContent(entryElem, "DssFile", fileName != null ? fileName : "");
			XMLUtilities.addChildContent(entryElem, "DssPath", dssPath != null ? dssPath : "");
		}
	}

	/**
	 * Deserialises the data location settings from the given XML element, rebuilding the
	 * _dataLocationSettings map from the stored DataLocation and DSSIdentifier pairs.
	 * Each entry is read from a "DataLocationSetting" child element containing the DSS file,
	 * DSS path, and the DataLocation XML definition. Entries whose DataLocation cannot be
	 * reconstructed or whose fromXML call fails are silently skipped.
	 *
	 * @param iterKid the XML Element containing the "DataLocations" child element to read from
	 */
	public void loadData(Element iterKid) {
		// Clear any previously loaded settings before reading new ones
		_dataLocationSettings.clear();

		// Locate the parent element that holds all DataLocationSetting entries
		Element entriesElem = iterKid.getChild("DataLocations");

		if (entriesElem != null) {
			// Retrieve the list of individual DataLocationSetting child elements
			List entryElems = entriesElem.getChildren("DataLocationSetting");

			String filename, dssPath;
			DataLocation dataLocation;
			Element entryElem;

			for (int i = 0; i < entryElems.size(); i++) {
				// Cast the current list entry to an XML Element for child access
				entryElem = (Element) entryElems.get(i);

				// Read the DSS file and path for this entry
				filename = XMLUtilities.getChildElementAsString(entryElem, "DssFile", "");
				dssPath = XMLUtilities.getChildElementAsString(entryElem, "DssPath", "");

				// Construct the DSS identifier from the file and path strings
				DSSIdentifier dssId = new DSSIdentifier(filename, dssPath);

				// Reconstruct the DataLocation from its embedded XML element
				Element dlElem = entryElem.getChild("DataLocation");
				dataLocation = DataLocationUtilities.createDataLocation(dlElem);

				if (dataLocation != null) {
					// Load the full DataLocation state from XML and add the entry if successful
					if (dataLocation.fromXML(dlElem)) {
						_dataLocationSettings.put(dataLocation, dssId);
					}
				}
			}
		}
	}

	/**
	 * Assigns the given ModelAlternative to every DataLocation currently registered in
	 * the settings table. Used when the active model alternative changes so that all
	 * data location entries remain consistent with the new alternative context.
	 *
	 * @param modelAlt the ModelAlternative to assign to each DataLocation in the settings table
	 */
	public void setModelAlternative(ModelAlternative modelAlt) {
		// Retrieve the set of all DataLocation keys currently in the settings table
		Set<DataLocation> keys = _dataLocationSettings.keySet();

		// Obtain an iterator to traverse each DataLocation entry in the key set
		Iterator<DataLocation> iter = keys.iterator();

		DataLocation dl;

		while (iter.hasNext()) {
			// Advance to the next DataLocation in the settings table
			dl = iter.next();

			// Assign the new model alternative to this data location
			dl.setModelAlternative(modelAlt);
		}
	}

	/**
	 * Refreshes the settings table to match the given list of DataLocations, carrying
	 * forward any DSS identifier assignments that were already present for matching entries.
	 * DataLocations in the new list that have no prior assignment are initialised with a
	 * blank DSSIdentifier placeholder. DataLocations that were in the old settings but are
	 * absent from the new list are effectively dropped when the map is cleared.
	 *
	 * @param dataLocs the updated list of DataLocation objects the settings table should reflect
	 */
	public void updateDataLocations(List<DataLocation> dataLocs) {
		// Snapshot the current settings so existing assignments can be carried forward
		HashMap<DataLocation, DSSIdentifier> currentSettings = new HashMap<>(_dataLocationSettings);

		// Clear the live map before repopulating it with the updated location list
		_dataLocationSettings.clear();

		DataLocation dl;
		DSSIdentifier dssId;

		for (int i = 0; i < dataLocs.size(); i++) {
			// Retrieve the current DataLocation from the updated list
			dl = dataLocs.get(i);

			// Look up whether this location already had a DSS identifier assigned
			dssId = currentSettings.get(dl);

			if (dssId == null) {
				// New location with no prior assignment: initialize with a blank identifier
				Logger.getLogger(getClass().getName()).fine("No DSS File/Info found for " + dl);
				dssId = new DSSIdentifier("", "");
			}

			// Re-register the location with its carried-forward or newly created identifier
			_dataLocationSettings.put(dl, dssId);
		}
	}
}
