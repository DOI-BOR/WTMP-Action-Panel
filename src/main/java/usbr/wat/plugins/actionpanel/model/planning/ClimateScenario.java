package usbr.wat.plugins.actionpanel.model.planning;

import org.jdom.Element;                        // JDOM XML element type used for saving and loading this object

import com.rma.util.XMLUtilities;               // Utility helper for serializing NamedType fields and simple child content to/from JDOM

import hec.lang.NamedType;                      // Base class supplying a display name and integer index, and standard modified-state tracking

/**
 * Represents a single named Climate Scenario dataset used as one of the three inputs
 * (alongside {@link CalSimData} and {@link HydrologyData}) that make up a {@link PlanningSet}.
 *
 * A climate scenario is defined by:
 * <ul>
 *   <li>{@link #getHistoricalDataPath()} — a <b>folder</b> containing historical data used
 *       as the basis for the scenario.</li>
 *   <li>{@link #getClimatePerturbationScriptPath()} — a <b>file</b> path pointing directly
 *       at the climate perturbation script applied to that historical data.</li>
 * </ul>
 *
 * The perturbation script is run via
 * {@code usbr.wat.plugins.actionpanel.ui.forecast.PythonScriptUtil}, and its progress or
 * outcome is reflected in {@link #getStatus()} for display in the "New Climate Scenario"
 * dialog's Status field.
 *
 * Instances are held directly by the owning {@link PlanningSet} (embedded, not referenced
 * by name) so that a saved Set remains fully self-contained and reloadable. They are also
 * added to the session-lifetime {@link PlanningSessionRegistry} so the same scenario can be
 * re-selected for another Set without re-running the script, for as long as the current
 * WAT session remains open.
 */
// Extends NamedType so this scenario gets standard name/index/modified-state bookkeeping for free
public class ClimateScenario extends NamedType {

	// All string fields below default to empty rather than null, simplifying UI display logic
	// The Status field is updated externally by NewClimateScenarioDialog after each script run

	// Free-text description of this scenario, shown in the New/Edit dialog's Description field
	private String _description = "";

	// Path to the folder containing the historical data this scenario is built from
	private String _historicalDataPath = "";

	// Path directly to the climate perturbation script (.py) applied to the historical data
	private String _climatePerturbationScriptPath = "";

	// Most recent status/result text from running the perturbation script
	private String _status = "";

	/**
	 * Constructs an empty climate scenario with no name, paths, or status.
	 */
	public ClimateScenario() {
		// No fields need explicit initialization beyond their declared defaults above
		super(); // Invoke NamedType's default constructor to initialize name/index bookkeeping
	}

	/**
	 * Returns the description text entered for this scenario.
	 *
	 * @return the description, or an empty string if none was entered
	 */
	public String getDescription() {
		return _description; // Simple accessor
	}

	/**
	 * Sets the description text for this scenario and marks it modified.
	 *
	 * @param description the new description text; null is stored as an empty string
	 */
	public void setDescription(String description) {
		_description = description == null ? "" : description; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the folder path containing this scenario's historical data.
	 *
	 * @return the historical data folder path, or an empty string if not yet set
	 */
	public String getHistoricalDataPath() {
		return _historicalDataPath; // Simple accessor
	}

	/**
	 * Sets the folder path containing this scenario's historical data and marks it modified.
	 *
	 * @param path a directory path; null is stored as an empty string
	 */
	public void setHistoricalDataPath(String path) {
		_historicalDataPath = path == null ? "" : path; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the file path of the climate perturbation script applied to the historical data.
	 *
	 * @return the script's file path, or an empty string if not yet set
	 */
	public String getClimatePerturbationScriptPath() {
		return _climatePerturbationScriptPath; // Simple accessor
	}

	/**
	 * Sets the file path of the climate perturbation script and marks this scenario modified.
	 *
	 * @param path a path directly to the script file; null is stored as an empty string
	 */
	public void setClimatePerturbationScriptPath(String path) {
		_climatePerturbationScriptPath = path == null ? "" : path; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the most recent status text from running the perturbation script.
	 *
	 * @return the status text, or an empty string if the script has not yet been run
	 */
	public String getStatus() {
		return _status; // Simple accessor
	}

	/**
	 * Sets the status text shown in the dialog's Status field, typically updated after
	 * a Run Script action completes (or fails).
	 *
	 * @param status the new status text; null is stored as an empty string
	 */
	public void setStatus(String status) {
		_status = status == null ? "" : status; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Persists this scenario's fields to a child "ClimateScenario" element under the
	 * given parent.
	 *
	 * @param parent the JDOM element to which the new "ClimateScenario" element is appended
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("ClimateScenario"); // Create the root element for this scenario's data
		parent.addContent(myElem); // Attach it under the caller-supplied parent element

		XMLUtilities.saveNamedType(myElem, this); // Persist the inherited name/index fields

		// Persist each simple string field as its own child element
		XMLUtilities.addChildContent(myElem, "Description", _description);
		XMLUtilities.addChildContent(myElem, "HistoricalDataPath", _historicalDataPath);
		XMLUtilities.addChildContent(myElem, "ClimatePerturbationScriptPath", _climatePerturbationScriptPath);
		XMLUtilities.addChildContent(myElem, "Status", _status);
	}

	/**
	 * Restores this scenario's fields from a "ClimateScenario" element previously written
	 * by {@link #saveData(Element)}.
	 *
	 * @param myElem the "ClimateScenario" element to load from
	 * @return true if the element was non-null and loading proceeded; false otherwise
	 */
	public boolean loadData(Element myElem) {
		if (myElem == null) {
			return false; // Nothing to load from; signal failure to the caller
		}

		XMLUtilities.loadNamedType(myElem, this); // Restore the inherited name/index fields

		// Restore each simple string field, defaulting to an empty string if missing
		_description = XMLUtilities.getChildElementAsString(myElem, "Description", "");
		_historicalDataPath = XMLUtilities.getChildElementAsString(myElem, "HistoricalDataPath", "");
		_climatePerturbationScriptPath = XMLUtilities.getChildElementAsString(
				myElem, "ClimatePerturbationScriptPath", "");
		_status = XMLUtilities.getChildElementAsString(myElem, "Status", "");

		return true; // Loading completed successfully
	}

	/**
	 * Returns this scenario's name, so it displays sensibly in combo boxes and lists.
	 *
	 * @return the scenario's name
	 */
	@Override
	public String toString() {
		return getName(); // Delegate to the inherited NamedType name accessor -- keeps combo-box display consistent
	}
}
