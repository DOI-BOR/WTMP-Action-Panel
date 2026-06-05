package usbr.wat.plugins.actionpanel.model;

import java.util.HashMap;   // Hash map used for the iteration, position-analysis, and compute-type settings maps
import java.util.Iterator;  // Iterator for walking map entry sets during XML serialization
import java.util.List;      // Ordered collection interface for child XML element lists during loading
import java.util.Map;       // Map interface for the three settings maps
import java.util.Map.Entry; // Map entry type used when iterating during saveComputeTypes
import java.util.Set;       // Set of map entries returned by entrySet()

import org.jdom.Element;    // JDOM XML Element used for serializing and deserializing all settings

import com.rma.util.XMLUtilities; // RMA XML utility for reading and writing child elements

/**
 * Concrete simulation group that supports iterative compute, position analysis,
 * and standard (non-iterative) compute types for each member simulation.
 *
 * Extends AbstractSimulationGroup to inherit simulation membership management,
 * analysis period association, and the core XML persistence framework. Adds three
 * additional per-simulation settings maps:
 *
 *   - _iterationsSettings:        maps simulation name → IterationSettings
 *   - _positionAnalysisSettings:  maps simulation name → PositionAnalysisSettings
 *   - _computeTypeSettings:       maps simulation name → ComputeType (Standard, Iterative, PositionAnalysis)
 *
 * If no settings have been explicitly created for a simulation name, the getters
 * create and cache a default instance. The compute type defaults to Standard.
 *
 * Serialized as a .simgrp file (extension defined by FILE_EXT). The XML structure
 * for the additional settings is:
 *
 *   <ComputeTypes>
 *     <Simulation>simName<ComputeType>Iterative</ComputeType></Simulation>
 *     ...
 *   </ComputeTypes>
 *   <Simulation>
 *     <IterationSettings>...</IterationSettings>
 *     <PositionAnalysisSettings>...</PositionAnalysisSettings>
 *   </Simulation>
 *
 * This class is suppressed for serialization warnings because JPanel ancestors
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class SimulationGroup extends AbstractSimulationGroup {
	// File extension used when persisting simulation group files to disk
	public static final String FILE_EXT = "simgrp";

	// Maps each simulation name to its iterative compute settings (BC DSS assignments + member config)
	private Map<String, IterationSettings> _iterationsSettings = new HashMap<>();

	// Maps each simulation name to its position analysis settings (BC DSS assignments + member config)
	private Map<String, PositionAnalysisSettings> _positionAnalysisSettings = new HashMap<>();

	// Maps each simulation name to its chosen compute type (Standard, Iterative, or PositionAnalysis)
	private Map<String, ComputeType> _computeTypeSettings = new HashMap();

	/**
	 * Constructs an empty SimulationGroup.
	 */
	public SimulationGroup() {
		super();
	}


	/**
	 * Finalizes the XML save by appending the compute type assignments element.
	 *
	 * Called by the base class saveData() after all simulation membership data has been written.
	 *
	 * @param elem the root XML Element of the simulation group document being saved
	 */
	@Override
	protected void finishSaving(Element elem) {
		saveComputeTypes(elem);
	}

	/**
	 * Serializes the iteration and position-analysis settings for the given simulation.
	 *
	 * Called by the base class for each simulation name during saveData(). Only serializes
	 * settings that have been explicitly configured for the simulation.
	 *
	 * @param simelem the XML Element for this simulation
	 * @param simName the name of the simulation whose settings should be saved
	 */
	@Override
	protected void saveSimulationSettings(Element simelem, String simName) {
		saveIterationSettings(simelem, simName);
		savePositionAnalysisSettings(simelem, simName);
	}

	/**
	 * Serialises the compute type settings map to XML by writing one "Simulation" element
	 * per entry under a shared "ComputeTypes" parent element. Each "Simulation" element
	 * contains the simulation name as its text value and a "ComputeType" child element
	 * holding the enum name of the assigned compute type.
	 *
	 * @param elem the parent XML Element to which the "ComputeTypes" element is added
	 */
	private void saveComputeTypes(Element elem) {
		// Retrieve all simulation-name-to-compute-type mappings for serialisation
		Set<Entry<String, ComputeType>> computeTypes = _computeTypeSettings.entrySet();

		// Create the "ComputeTypes" container element and attach it to the parent
		Element computeTypesElem = new Element("ComputeTypes");
		elem.addContent(computeTypesElem);

		// Obtain an iterator to traverse each simulation-to-compute-type entry
		Iterator<Entry<String, ComputeType>> iter = computeTypes.iterator();
		Element simElem;

		while (iter.hasNext()) {
			// Advance to the next simulation name and compute type pairing
			Entry<String, ComputeType> next = iter.next();

			// Write the simulation name as the element text and the compute type as a child
			simElem = XMLUtilities.saveChildElement(computeTypesElem, "Simulation", next.getKey());
			XMLUtilities.saveChildElement(simElem, "ComputeType", next.getValue().name());
		}
	}

	/**
	 * Deserialises the compute type settings from the given XML root element, rebuilding
	 * the _computeTypeSettings map from the stored simulation name and compute type pairs.
	 * Each "Simulation" child element is expected to contain a "ComputeType" child whose
	 * text matches a valid ComputeType enum constant name. Entries with a missing
	 * "ComputeType" child are silently skipped. Does nothing when no "ComputeTypes"
	 * element is present under the root.
	 *
	 * @param root the XML Element containing the "ComputeTypes" child element to read from
	 */
	private void loadComputeTypes(Element root) {
		// Locate the parent element that holds all Simulation compute type entries
		Element computeTypesElem = root.getChild("ComputeTypes");

		// Abort early when the ComputeTypes element is absent from the XML
		if (computeTypesElem == null) {
			return;
		}

		// Retrieve the list of individual Simulation child elements to iterate over
		List kids = computeTypesElem.getChildren("Simulation");

		Element simElem, ctElem;

		for (int i = 0; i < kids.size(); i++) {
			// Cast the current list entry to an XML Element for child access
			simElem = (Element) kids.get(i);

			// Locate the ComputeType child element within this Simulation entry
			ctElem = simElem.getChild("ComputeType");

			if (ctElem != null) {
				// Map the simulation name to the parsed compute type enum constant
				_computeTypeSettings.put(simElem.getTextTrim(), ComputeType.valueOf(ctElem.getTextTrim()));
			}
		}
	}

	/**
	 * Serializes the position-analysis settings for the given simulation, if any.
	 *
	 * Has no effect if no position-analysis settings have been configured for the simulation.
	 *
	 * @param simElem the XML Element for this simulation
	 * @param simName the simulation name whose position-analysis settings should be saved
	 */
	private void savePositionAnalysisSettings(Element simElem, String simName) {
		// Return immediately if settings is not available
		PositionAnalysisSettings settings = _positionAnalysisSettings.get(simName);
		if (settings == null) {
			return;
		}

		// Append the position-analysis settings as a child element of the simulation element
		Element paElem = new Element("PositionAnalysisSettings");
		simElem.addContent(paElem);
		settings.saveData(paElem);
	}

	/**
	 * Serializes the iteration settings for the given simulation, if any.
	 *
	 * Has no effect if no iteration settings have been configured for the simulation.
	 *
	 * @param simElem the XML Element for this simulation
	 * @param simName the simulation name whose iteration settings should be saved
	 */
	private void saveIterationSettings(Element simElem, String simName) {
		// Return immediately if settings is not available
		IterationSettings settings = _iterationsSettings.get(simName);
		if (settings == null) {
			return;
		}

		// Append the iteration settings as a child element of the simulation element
		Element iterElem = new Element("IterationSettings");
		simElem.addContent(iterElem);
		settings.saveData(iterElem);
	}

	/**
	 * Finalizes the XML load by reading the compute type assignments.
	 *
	 * Called by the base class loadData() after all simulation membership data has been read.
	 *
	 * @param root the root XML Element of the simulation group document being loaded
	 */
	@Override
	protected void finishLoading(Element root) {
		loadComputeTypes(root);
	}

	/**
	 * Clears all settings maps before loading new data from XML.
	 *
	 * Called by the base class loadData() before reading begins to ensure a clean state.
	 */
	@Override
	protected void initForLoading() {
		_iterationsSettings.clear();
		_positionAnalysisSettings.clear();
		_computeTypeSettings.clear();
	}

	/**
	 * Returns the XML type identifier string for this simulation group subclass.
	 *
	 * @return "SimulationGroup"
	 */
	@Override
	protected String getSimulationGroupType() {
		return "SimulationGroup";
	}

	/**
	 * Restores per-simulation settings from the given simulation XML element.
	 *
	 * Called by the base class for each simulation element during loadData().
	 *
	 * @param simElem the XML Element for this simulation
	 * @param simName the name of the simulation being loaded
	 */
	@Override
	protected void loadSimulationSettings(Element simElem, String simName) {
		loadIterationSettings(simElem, simName);
		loadPositionAnalysisSettings(simElem, simName);
	}

	/**
	 * Restores position-analysis settings for the given simulation from the given XML element.
	 *
	 * Has no effect if the "PositionAnalysisSettings" child element is absent.
	 *
	 * @param simElem the XML Element for this simulation
	 * @param simName the simulation name to map the loaded settings to
	 */
	private void loadPositionAnalysisSettings(Element simElem, String simName) {
		Element iterElem = simElem.getChild("PositionAnalysisSettings");
		if (iterElem == null) {
			return;
		}

		// Create, populate, and cache the position-analysis settings for this simulation
		PositionAnalysisSettings settings = new PositionAnalysisSettings();
		settings.loadData(iterElem);
		_positionAnalysisSettings.put(simName, settings);
	}

	/**
	 * Restores iteration settings for the given simulation from the given XML element.
	 *
	 * Has no effect if the "IterationSettings" child element is absent.
	 *
	 * @param simElem the XML Element for this simulation
	 * @param simName the simulation name to map the loaded settings to
	 */
	private void loadIterationSettings(Element simElem, String simName) {
		// Return immediately if settings is not available
		Element iterElem = simElem.getChild("IterationSettings");
		if (iterElem == null) {
			return;
		}

		// Create, populate, and cache the iteration settings for this simulation
		IterationSettings settings = new IterationSettings();
		settings.loadData(iterElem);
		_iterationsSettings.put(simName, settings);
	}


	/**
	 * Returns the IterationSettings for the given simulation, creating and caching
	 * a default instance if none has been configured.
	 *
	 * @param simName the simulation name to retrieve iteration settings for
	 * @return the IterationSettings for the simulation; never null
	 */
	public IterationSettings getIterationSettings(String simName) {
		// Get the settings
		IterationSettings settings = _iterationsSettings.get(simName);

		// Act only if settings is available
		if (settings == null) {
			// Create and cache a default IterationSettings for this simulation
			settings = new IterationSettings();
			_iterationsSettings.put(simName, settings);
		}

		// Retrun the settings to calling function
		return settings;
	}

	/**
	 * Returns the PositionAnalysisSettings for the given simulation, creating and caching
	 * a default instance if none has been configured.
	 *
	 * @param simName the simulation name to retrieve position-analysis settings for
	 * @return the PositionAnalysisSettings for the simulation; never null
	 */
	public PositionAnalysisSettings getPositionAnalysisSettings(String simName) {
		// Get the settings object
		PositionAnalysisSettings settings = _positionAnalysisSettings.get(simName);

		// Act only if settings is available
		if (settings == null) {
			// Create and cache a default PositionAnalysisSettings for this simulation
			settings = new PositionAnalysisSettings();
			_positionAnalysisSettings.put(simName, settings);
		}

		// Return settings to the calling function
		return settings;
	}

	/**
	 * Returns the ComputeType assigned to the given simulation.
	 *
	 * Defaults to ComputeType.Standard if no compute type has been explicitly set.
	 *
	 * @param simName the simulation name to retrieve the compute type for
	 * @return the assigned ComputeType, or ComputeType.Standard if not set
	 */
	public ComputeType getComputeType(String simName) {
		// Get the compute type
		ComputeType computeType = _computeTypeSettings.get(simName);

		// Set the compute type if the object is valid
		if (computeType == null) {
			computeType = ComputeType.Standard;
		}

		// Return the compute type to the calling function
		return computeType;
	}

	/**
	 * Sets the ComputeType for the given simulation.
	 *
	 * Has no effect if either argument is null.
	 *
	 * @param simName the simulation name to assign the compute type to
	 * @param ct      the ComputeType to assign
	 */
	public void setComputeType(String simName, ComputeType ct) {
		if (simName != null && ct != null) {
			_computeTypeSettings.put(simName, ct);
		}
	}

	/**
	 * Returns the BaseComputeSettings appropriate for the given simulation and compute type.
	 *
	 * Returns the IterationSettings for Iterative, the PositionAnalysisSettings for
	 * PositionAnalysis, and null for Standard (which has no additional compute settings).
	 *
	 * @param simName     the simulation name to retrieve compute settings for
	 * @param computeType the compute type determining which settings to return
	 * @return the relevant BaseComputeSettings, or null for Standard compute type
	 */
	public BaseComputeSettings getComputeSettings(String simName, ComputeType computeType) {
		switch (computeType) {
			case Iterative:
				return getIterationSettings(simName);
			case PositionAnalysis:
				return getPositionAnalysisSettings(simName);
			default:
				// Standard compute type has no additional settings
				return null;
		}
	}
}
