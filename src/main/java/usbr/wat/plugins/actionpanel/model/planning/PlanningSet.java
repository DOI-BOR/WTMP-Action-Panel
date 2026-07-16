package usbr.wat.plugins.actionpanel.model.planning;

import org.jdom.Element;                        // JDOM XML element type used for saving and loading this object

import com.rma.util.XMLUtilities;               // Utility helper for serializing NamedType fields and simple child content to/from JDOM

import hec.lang.NamedType;                      // Base class supplying a display name and integer index, and standard modified-state tracking

/**
 * A named "Set" within the Planning workflow — the Planning-tab analogue of an
 * "alternative" in the other WTMP workflows.
 *
 * A Set bundles the climate-side forcing data for a planning run:
 * <ul>
 *   <li>a WAT schema selection,</li>
 *   <li>a {@link CalSimData} reference,</li>
 *   <li>a {@link ClimateScenario} reference, and</li>
 *   <li>a {@link HydrologyData} reference,</li>
 * </ul>
 * i.e. everything needed to derive the meteorologic data that is ultimately applied to a
 * model. A Set is deliberately distinct from a Simulation Group: the Simulation Group is
 * the model configuration the Set's derived data gets applied to. The pairing of
 * ({@code Simulation Group}, {@code Set}) is unique — the Planning tab's UI exposes both
 * as independent selectors precisely so a single Set can, over time, be evaluated against
 * different Simulation Groups (and vice versa).
 *
 * The CalSim/Climate/Hydrology data referenced by a Set are embedded directly (not stored
 * by reference only) so a saved Set remains fully self-contained; see
 * {@link #getCalSimData()}, {@link #getClimateScenario()}, and {@link #getHydrologyData()}.
 * The currently paired Simulation Group is stored by name only (mirroring the pattern used
 * elsewhere in this plugin, e.g. {@code EnsembleSet}'s stored BC/temperature-target names),
 * since the live {@code SimulationGroup}/{@code ForecastSimGroup} object is owned and
 * persisted by the project's manager list, not by this class.
 */
public class PlanningSet extends NamedType {

	// Free-text description of this Set, shown in the New/Edit dialog's Description field
	private String _description = "";

	// Name of the selected WAT schema for this Set (see NewPlanningSetDialog's Schema combo)
	private String _watSchema = "";

	// Embedded CalSim data for this Set; never null once the Set has been created via the dialog
	private CalSimData _calSimData;

	// Embedded climate scenario data for this Set
	private ClimateScenario _climateScenario;

	// Embedded hydrology data for this Set
	private HydrologyData _hydrologyData;

	// Embedded temperature target definition for this Set (fixed value / timeseries / jython)
	private PlanningTempTarget _tempTarget;

	// Name of the currently paired Simulation Group, resolved against the project's manager
	// list by the UI layer; empty until a Simulation Group has been selected for this Set
	private String _simulationGroupName = "";

	/**
	 * Constructs an empty Set with no name, schema, or referenced datasets.
	 */
	public PlanningSet() {
		super(); // Invoke NamedType's default constructor to initialize name/index bookkeeping
	}

	/**
	 * Returns the description text entered for this Set.
	 *
	 * @return the description, or an empty string if none was entered
	 */
	public String getDescription() {
		return _description; // Simple accessor
	}

	/**
	 * Sets the description text for this Set and marks it modified.
	 *
	 * @param description the new description text; null is stored as an empty string
	 */
	public void setDescription(String description) {
		_description = description == null ? "" : description; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the name of the WAT schema selected for this Set.
	 *
	 * @return the WAT schema name, or an empty string if none has been selected
	 */
	public String getWatSchema() {
		return _watSchema; // Simple accessor
	}

	/**
	 * Sets the WAT schema for this Set and marks it modified.
	 *
	 * @param watSchema the schema name; null is stored as an empty string
	 */
	public void setWatSchema(String watSchema) {
		_watSchema = watSchema == null ? "" : watSchema; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the CalSim dataset embedded in this Set.
	 *
	 * @return the CalSim data, or null if not yet assigned
	 */
	public CalSimData getCalSimData() {
		return _calSimData; // Simple accessor
	}

	/**
	 * Assigns the CalSim dataset for this Set and marks it modified.
	 *
	 * @param calSimData the CalSim data to embed; may be null while the Set is still being built
	 */
	public void setCalSimData(CalSimData calSimData) {
		_calSimData = calSimData; // Store the reference directly; the dataset itself is owned by this Set
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the climate scenario embedded in this Set.
	 *
	 * @return the climate scenario, or null if not yet assigned
	 */
	public ClimateScenario getClimateScenario() {
		return _climateScenario; // Simple accessor
	}

	/**
	 * Assigns the climate scenario for this Set and marks it modified.
	 *
	 * @param climateScenario the climate scenario to embed; may be null while the Set is
	 *                        still being built
	 */
	public void setClimateScenario(ClimateScenario climateScenario) {
		_climateScenario = climateScenario; // Store the reference directly; the scenario itself is owned by this Set
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the hydrology dataset embedded in this Set.
	 *
	 * @return the hydrology data, or null if not yet assigned
	 */
	public HydrologyData getHydrologyData() {
		return _hydrologyData; // Simple accessor
	}

	/**
	 * Assigns the hydrology dataset for this Set and marks it modified.
	 *
	 * @param hydrologyData the hydrology data to embed; may be null while the Set is still
	 *                      being built
	 */
	public void setHydrologyData(HydrologyData hydrologyData) {
		_hydrologyData = hydrologyData; // Store the reference directly; the dataset itself is owned by this Set
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the temperature target definition embedded in this Set.
	 *
	 * @return the temperature target, or null if not yet defined for this Set
	 */
	public PlanningTempTarget getTempTarget() {
		return _tempTarget; // Simple accessor
	}

	/**
	 * Assigns the temperature target definition for this Set and marks it modified.
	 *
	 * @param tempTarget the temperature target to embed; may be null
	 */
	public void setTempTarget(PlanningTempTarget tempTarget) {
		_tempTarget = tempTarget; // Store the reference directly; the target itself is owned by this Set
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the name of the Simulation Group currently paired with this Set.
	 *
	 * @return the paired Simulation Group's name, or an empty string if none is paired yet
	 */
	public String getSimulationGroupName() {
		return _simulationGroupName; // Simple accessor
	}

	/**
	 * Records the name of the Simulation Group paired with this Set and marks it modified.
	 * The live {@code SimulationGroup} object itself is resolved from the project's manager
	 * list by the UI layer using this name.
	 *
	 * @param simulationGroupName the paired Simulation Group's name; null is stored as an
	 *                            empty string
	 */
	public void setSimulationGroupName(String simulationGroupName) {
		_simulationGroupName = simulationGroupName == null ? "" : simulationGroupName; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Persists this Set's fields, including its embedded CalSim/Climate/Hydrology/temperature
	 * target data, to a child "PlanningSet" element under the given parent.
	 *
	 * @param parent the JDOM element to which the new "PlanningSet" element is appended
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("PlanningSet"); // Create the root element for this Set's data
		parent.addContent(myElem); // Attach it under the caller-supplied parent element

		XMLUtilities.saveNamedType(myElem, this); // Persist the inherited name/index fields

		// Persist the three simple string fields as child elements
		XMLUtilities.addChildContent(myElem, "Description", _description);
		XMLUtilities.addChildContent(myElem, "WatSchema", _watSchema);
		XMLUtilities.addChildContent(myElem, "SimulationGroupName", _simulationGroupName);

		// Each embedded dataset saves itself as its own child element, only if it has been assigned
		if (_calSimData != null) {
			_calSimData.saveData(myElem); // Delegate to CalSimData's own saveData
		}
		if (_climateScenario != null) {
			_climateScenario.saveData(myElem); // Delegate to ClimateScenario's own saveData
		}
		if (_hydrologyData != null) {
			_hydrologyData.saveData(myElem); // Delegate to HydrologyData's own saveData
		}
		if (_tempTarget != null) {
			_tempTarget.saveData(myElem); // Delegate to PlanningTempTarget's own saveData
		}
	}

	/**
	 * Restores this Set's fields, including its embedded CalSim/Climate/Hydrology/temperature
	 * target data, from a "PlanningSet" element previously written by {@link #saveData(Element)}.
	 *
	 * @param myElem the "PlanningSet" element to load from
	 * @return true if the element was non-null and loading proceeded; false otherwise
	 */
	public boolean loadData(Element myElem) {
		if (myElem == null) {
			return false; // Nothing to load from; signal failure to the caller
		}

		XMLUtilities.loadNamedType(myElem, this); // Restore the inherited name/index fields
		// Restore the three simple string fields, defaulting to empty strings if missing
		_description = XMLUtilities.getChildElementAsString(myElem, "Description", "");
		_watSchema = XMLUtilities.getChildElementAsString(myElem, "WatSchema", "");
		_simulationGroupName = XMLUtilities.getChildElementAsString(myElem, "SimulationGroupName", "");

		// Look for a nested CalSimData element and, if present, reconstruct and load it
		Element calSimElem = myElem.getChild("CalSimData");
		if (calSimElem != null) {
			_calSimData = new CalSimData(); // Create a fresh instance to populate
			_calSimData.loadData(calSimElem); // Delegate to CalSimData's own loadData
		}

		// Look for a nested ClimateScenario element and, if present, reconstruct and load it
		Element climateElem = myElem.getChild("ClimateScenario");
		if (climateElem != null) {
			_climateScenario = new ClimateScenario(); // Create a fresh instance to populate
			_climateScenario.loadData(climateElem); // Delegate to ClimateScenario's own loadData
		}

		// Look for a nested HydrologyData element and, if present, reconstruct and load it
		Element hydroElem = myElem.getChild("HydrologyData");
		if (hydroElem != null) {
			_hydrologyData = new HydrologyData(); // Create a fresh instance to populate
			_hydrologyData.loadData(hydroElem); // Delegate to HydrologyData's own loadData
		}

		// Look for a nested PlanningTempTarget element and, if present, reconstruct and load it
		Element tempTargetElem = myElem.getChild("PlanningTempTarget");
		if (tempTargetElem != null) {
			_tempTarget = new PlanningTempTarget(); // Create a fresh instance to populate
			_tempTarget.loadData(tempTargetElem); // Delegate to PlanningTempTarget's own loadData
		}

		return true; // Loading completed successfully
	}

	/**
	 * Returns this Set's name, so it displays sensibly in combo boxes and lists.
	 *
	 * @return the Set's name
	 */
	@Override
	public String toString() {
		return getName(); // Delegate to the inherited NamedType name accessor
	}
}
