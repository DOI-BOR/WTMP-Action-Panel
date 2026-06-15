package usbr.wat.plugins.actionpanel.model.forecast;

import java.text.ParseException;  // Checked exception thrown when a Profile date string cannot be parsed
import java.util.ArrayList;       // Resizable-array List used to return the reservoir name key set as a copy
import java.util.HashMap;         // Hash map backing the reservoir-name-to-Profile lookup table
import java.util.Iterator;        // Iterator for walking the IC map entries during XML serialization
import java.util.List;            // Ordered collection interface for the reservoir name list
import java.util.Map;             // Map interface for the reservoir-name-to-Profile data store
import java.util.Map.Entry;       // Map entry type used when iterating during saveData
import java.util.Set;             // Set of map entries returned by entrySet()

import com.google.common.flogger.FluentLogger; // Google Flogger for structured warning logging on parse failures
import com.rma.model.Project;                  // Represents the currently loaded RMA project; used to relativize/absolutize DSS file paths
import hec.heclib.dss.DSSPathname;             // HEC DSS pathname; used to handle null/empty DSS paths during save
import org.jdom.Element;                       // JDOM XML Element used for serializing and deserializing IC data

import hec.lang.NamedType; // HEC base class providing a name field; the name stores the IC configuration name

/**
 * Named data object mapping each reservoir name to the IC (Initial Conditions)
 * Profile selected for a forecast compute run within a ForecastSimGroup.
 *
 * The internal map (_icMap) stores one Profile per reservoir. A Profile carries the
 * DSS file name and DSS path from which the reservoir's initial conditions should be
 * read at the start of each ensemble member compute.
 *
 * The map is populated via putSelectedProfile() when the user makes a selection in the
 * IC editor, and is persisted to XML via saveData()/loadData(). During save, DSS file
 * paths are stored relative to the current project directory; during load they are
 * resolved back to absolute paths.
 *
 * Profile dates (encoded in the profile name) are parsed during load; failures are
 * logged at warning level and the entry is skipped.
 *
 */
public class InitialConditions extends NamedType {
	// Logger for recording profile date parse failures during loadData()
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	// Maps each reservoir name to the IC Profile selected for it
	private Map<String, Profile> _icMap = new HashMap<>();

	/**
	 * Constructs an empty InitialConditions with no reservoir-to-profile mappings.
	 */
	public InitialConditions() {
		super();
	}

	/**
	 * Associates the given reservoir name with the selected IC Profile.
	 *
	 * Has no effect if either argument is null.
	 *
	 * @param resName         the reservoir name to map
	 * @param selectedProfile the Profile to associate with that reservoir
	 */
	public void putSelectedProfile(String resName, Profile selectedProfile) {
		if (resName != null && selectedProfile != null) {
			_icMap.put(resName, selectedProfile);
		}
	}

	/**
	 * Returns the IC Profile currently selected for the given reservoir name.
	 *
	 * @param resName the reservoir name to look up
	 * @return the selected Profile, or null if no profile has been selected for this reservoir
	 */
	public Profile getSelectedProfile(String resName) {
		return _icMap.get(resName);
	}

	/**
	 * Returns a copy of the set of reservoir names that have profiles assigned.
	 *
	 * Returns a snapshot; changes to the returned list do not affect the internal map.
	 *
	 * @return a List of reservoir name strings with assigned IC profiles
	 */
	public List<String> getReservoirs() {
		return new ArrayList<>(_icMap.keySet());
	}


	/**
	 * Serializes the initial conditions map to an XML element structure for project saving.
	 * Iterates over each reservoir-profile pair stored in _icMap and builds a nested XML
	 * tree of Reservoir and Profile elements, including the DSS file path and DSS pathname.
	 * The DSS file path is stored as a relative path for project portability.
	 *
	 * @param icElem the parent XML Element that all Reservoir child elements will be added to
	 */
	public void saveData(Element icElem) {
		// Retrieve the set of reservoir name-to-profile mappings from the initial conditions map
		Set<Entry<String, Profile>> entrySet = _icMap.entrySet();

		// Get an iterator to traverse each reservoir-profile entry
		Iterator<Entry<String, Profile>> iter = entrySet.iterator();

		// Declare variables to hold the current reservoir name, profile, and XML elements
		String resName;
		Profile profile;
		Element resElem;
		Element profElem;

		// Iterate over every reservoir-profile pair in the initial conditions map
		while (iter.hasNext()) {
			Entry<String, Profile> entry = iter.next();

			// Extract the reservoir name (key) and its associated profile (value)
			resName = entry.getKey();
			profile = entry.getValue();

			// Create the Reservoir element with the reservoir name as an attribute
			resElem = new Element("Reservoir");
			resElem.setAttribute("Name", resName);

			// Attach the Reservoir element to the parent initial conditions element
			icElem.addContent(resElem);

			// Create the Profile element with the profile name (encodes the date) as an attribute
			profElem = new Element("Profile");
			profElem.setAttribute("Name", profile.getName());

			// Store the DSS file path relative to the current project directory for portability
			Element fileElem = new Element("Output-DSS-File");
			fileElem.setText(Project.getCurrentProject().getRelativePath(profile.getDssFileName()));

			// Create the DSS-Pathname element to hold the structured DSS path string
			Element pathnameElement = new Element("DSS-Pathname");

			// Default to an empty DSS pathname object in case the profile has no path assigned
			DSSPathname dssPathname = new DSSPathname();

			// If the profile has a DSS path set, construct a DSSPathname object from it
			if (profile.getDssPath() != null) {
				dssPathname = new DSSPathname(profile.getDssPath());
			}

			// Convert the DSSPathname to its string representation and store it in the element
			pathnameElement.setText(dssPathname.toString());

			// Nest the DSS file and pathname elements inside the Profile element
			profElem.addContent(fileElem);
			profElem.addContent(pathnameElement);

			// Nest the completed Profile element inside its parent Reservoir element
			resElem.addContent(profElem);
		}
	}

	/**
	 * Restores reservoir-to-profile mappings from the given XML element.
	 *
	 * Clears the existing map before loading. For each "Reservoir" child element,
	 * reads the reservoir name attribute, then reads the first "Profile" child to
	 * get the profile name, DSS file path, and DSS path. DSS file paths stored as
	 * relative paths are resolved to absolute paths against the current project directory.
	 *
	 * Profile names are parsed by the Profile constructor; entries whose name cannot
	 * be parsed as a date are logged at warning level and skipped.
	 *
	 * Has no effect if icElem is null.
	 *
	 * @param icElem the XML Element containing the reservoir IC entries to load
	 */
	public void loadData(Element icElem) {
		// Clear any previously loaded mappings before reading new data
		_icMap.clear();

		if (icElem == null) {
			return;
		}

		List resElems = icElem.getChildren();
		List profElems;
		Element resElem;
		String reservoirName, profileName;

		for (int r = 0; r < resElems.size(); r++) {
			resElem = (Element) resElems.get(r);

			// Read the reservoir name from the element attribute
			reservoirName = resElem.getAttributeValue("Name");

			profElems = resElem.getChildren();
			if (profElems != null && !profElems.isEmpty()) {
				// Only the first Profile child is used; additional children are ignored
				Element profileElem = (Element) profElems.get(0);

				// The profile name encodes the IC date and is parsed by the Profile constructor
				profileName = profileElem.getAttributeValue("Name");

				// Read the DSS file path (stored relative; resolved to absolute below)
				Element fileElem = profileElem.getChild("Output-DSS-File");
				String dssFileName = null;
				if (fileElem != null) {
					dssFileName = fileElem.getText();
				}

				// Read the DSS record path string
				String dssPathName = null;
				Element pathnameElement = profileElem.getChild("DSS-Pathname");
				if (pathnameElement != null) {
					dssPathName = pathnameElement.getText();
				}

				try {
					// Construct the Profile from its name (which encodes the IC date)
					Profile profile = new Profile(profileName);

					if (dssFileName != null) {
						// Resolve the stored relative path to an absolute path for file I/O
						profile.setDssFileName(Project.getCurrentProject().getAbsolutePath(dssFileName));
					}
					if (dssPathName != null) {
						profile.setDssPath(dssPathName);
					}

					_icMap.put(reservoirName, profile);
				} catch (ParseException e) {
					// Log a warning and skip entries whose profile name cannot be parsed as a date
					LOGGER.atWarning().withCause(e).log("Failed to parse profile date: " + profileName);
				}
			}
		}
	}
}
