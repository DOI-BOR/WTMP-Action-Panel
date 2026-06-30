package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList; // Use ArrayList for resizable collection storage of forecast-specific objects
import java.util.Collection; // Interface defining common behavior of all Java collections
import java.util.HashMap; // Map class implementing hash table backed by array list storage
import java.util.Iterator; // Standard interface for element-by-element iteration over collections
import java.util.LinkedHashSet; // Set implementation that preserves insertion order for predictable iteration
import java.util.List; // Standard list interface representing ordered sequence of elements
import java.util.Map; // Two-dimensional key-value storage interface used throughout the class
import java.util.Objects; // Utility methods for null-safe equals comparisons
import java.util.Set; // Interface defining common set collection behavior (no duplicate elements)

import com.rma.util.XMLUtilities; // Helper class for saving/loading named types and child elements in XML format

import hec2.wat.model.WatSimulation; // Base class representing a computable WAT simulation instance

import org.jdom.Element; // Core JDOM element class representing XML elements in the document structure

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup; // Parent abstract class with group-level loading/saving


/**
 * ForecastSimGroup is a simulation group subclass specifically designed for forecast analysis workflows.
 * It extends AbstractSimulationGroup to manage forecast-specific data including boundary conditions, initial conditions,
 * meteorological data, operations data, and ensemble sets with temperature target configurations. The class handles
 * XML persistence of non-simulation-level data that must be loaded or saved before individual simulation settings are processed.
 *
 * This group manages:
 *
 *   Boundary Condition (BC) data objects linked to meteorological and operations sources
 *   Initial Conditions configuration for ensemble member starting states
 *   Meteorology data definitions for boundary condition overrides
 *   Operations data definitions for model parameterization choices
 *   Temperature Target Set configurations for specific alternative mappings
 *   Ensemble Sets keyed by simulation name and BC+TT combinations with collection indexing
 *
 *
 * The group supports:
 *
 *   Loading configuration from XML files during project load operations
 *   Saving updated configuration back to disk before simulation compute
 *   Managing ensemble collection sequences with automatic index assignment
 *   Removing and replacing forecast configurations while maintaining references
 *
 */

public class ForecastSimGroup extends AbstractSimulationGroup {
	// List holding temperature target set objects for each model alternative in the forecast configuration
	private List<TemperatureTargetSet> _tempTargetSets = new ArrayList<>(); // Collection of temp target configs

	// Single initial conditions object representing starting state data for ensemble members
	private InitialConditions _initConditions = new InitialConditions(); // Default-initialized instance for ICs

	// List holding meteorological data definitions used for boundary condition configuration
	private List<MeteorlogicData> _metData = new ArrayList<>(); // Collection of MET data objects

	// List holding operations data definitions for model alternative configurations
	private List<OperationsData> _opsData = new ArrayList<>(); // Collection of ops data objects

	// List holding boundary condition data objects linked to specific MET and OPS configurations
	private List<BcData> _bcData = new ArrayList<>(); // Collection of BC configuration objects

	// Map associating simulation names with their ensemble sets organized by BC+TT combinations
	private Map<String, List<EnsembleSet>> _ensembleSets = new HashMap<>(); // Dictionary: simName -> [BC+TT configurations]

	/** simulation/ensemble set to what its F-Part collection range is */
	private Map<String, Map<String, int[]>> _ensembleSetIndexing = new HashMap<>(); // Nested map: simName -> (enSetName -> [startCollection, endCollection])

	// Default constructor with empty initialization
	public ForecastSimGroup() {
		super(); // Invoke superclass default constructor
	}

	/**
	 * Called at the end of the XML loading process to load data that's not simulation specific.
	 * This method loads all boundary condition, initial condition, temperature target, and ensemble set definitions
	 * from the JDOM element before individual simulation settings are processed during their own loading phase.
	 *
	 * @param root The JDOM root element containing serialized forecast group configuration data
	 */
	@Override
	protected void finishLoading(Element root) {
		loadTempTargetSets(root); // Load temperature target set definitions from XML child elements
		loadInitialConditions(root); // Load initial conditions configuration object
		loadOpsData(root); // Load operations data definition objects
		loadMetData(root); // Load meteorology data definition objects
		loadBcData(root); // Load boundary condition configuration objects
		loadEnsembleSets(root); // Load ensemble sets for all simulations from XML
	}

	// Private method loads ensemble set configurations from XML structure
	private void loadEnsembleSets(Element root) {
		_ensembleSets.clear(); // Clear existing ensemble maps before loading new data

		Element esetsElem = root.getChild("EnsembleSets"); // Find the EnsembleSets container element

		// Check if EnsembleSets element exists in XML hierarchy
		if ( esetsElem == null ) {
			return; // Exit early if no ensemble sets are defined to load
		}

		List simKids = esetsElem.getChildren(); // Get all child elements within EnsembleSets container
		EnsembleSet eset; // Declare loop variable for processing each ensemble set object
		String simName; // Variable to store the simulation name from XML element

		// Iterate through each child of EnsembleSets
		for (int i = 0;i < simKids.size(); i++ ) {
			Element simChild = (Element) simKids.get(i); // Get current child as Element object

			// Check if this is a Simulation sub-element
			if ( !"Simulation".equals(simChild.getName())) {
				continue; // Skip any elements that are not direct simulation containers
			}

			simName = simChild.getTextTrim(); // Extract and trim the simulation name from text content
			List<EnsembleSet>ensembleSets = new ArrayList<>(); // Create new list for this simulation's sets

			List kids = simChild.getChildren(); // Get all children of current Simulation element

			// Iterate through each child inside the Simulation element
			for (int k = 0; k < kids.size(); k++ ) {
				Element child = (Element) kids.get(k); // Get next child element from inner list
				eset = new EnsembleSet(); // Create new instance of ensemble set object

				// Attempt to load ensemble set data into object
				if (eset.loadData(child)) {
					ensembleSets.add(eset); // Add successfully loaded set to collection
				}
			}

			_ensembleSets.put(simName, ensembleSets); // Store list in map keyed by simulation name

			BcData bcData; // Declare variable for BC data object lookup

			// Loop through each loaded ensemble set
			for (int e = 0;e < ensembleSets.size();e++ ) {
				eset = ensembleSets.get(e); // Get current ensemble set from list

				String bcDataName = eset.getBcDataName(); // Extract BC data name string from set
				bcData = getBcData(bcDataName); // Look up BC data object using its stored name
				eset.setSelectedBcData(bcData); // Set the selected BC object reference in ensemble set

				String ttsName = eset.getTemperatureTargetSetName(); // Extract temperature target set name from set
				TemperatureTargetSet ttset = getTemperatureTargetSet(ttsName); // Look up TTTS object by name
				eset.setSelectedTemperatureTargetSets(ttset); // Set the selected temp target set in ensemble set
			}
		}

		loadEnsembleSetsIndexing(root); // After all ensembles loaded, process collection index mappings
	}

	// Private method loads collection index from XML
	private void loadEnsembleSetsIndexing(Element root) {
		_ensembleSetIndexing.clear(); // Clear existing indexing map before loading new data

		Element esiElem = root.getChild("EnsembleSetIndexing"); // Find the EnsembleSetIndexing container element

		// Check if EnsembleSetIndexing element exists in XML hierarchy
		if ( esiElem == null ) {
			return; // Exit early if no ensemble set indexing is defined to load
		}

		List esiKids = esiElem.getChildren("Simulation"); // Get all child Simulation elements under Indexing container

		// Iterate through each indexed simulation element
		for (int i = 0; i < esiKids.size(); i++ ) {
			Element simElem = (Element) esiKids.get(i); // Get current simulation element
			String simname = simElem.getAttributeValue("Name"); // Get Name attribute value from element

			Map<String, int[]>esetMap = new HashMap<>(); // Create new map to hold this simulation's collection indices
			_ensembleSetIndexing.put(simname, esetMap); // Store map in outer dictionary using sim name

			List simKids = simElem.getChildren("EnsembleSet"); // Get all EnsembleSet children of this simulation

			// Iterate through each indexed ensemble set
			for (int s = 0; s < simKids.size(); s++ ) {
				Element esetElem = (Element) simKids.get(s); // Get current ensemble set element
				String esetName = esetElem.getAttributeValue("Name"); // Get Name attribute value from element

				int[] indexes = new int[2]; // Create array to hold start and end collection indices

				int start = XMLUtilities.getChildElementAsInt(esetElem, "CollectionStart", -1); // Extract CollectionStart value or default to -1
				int end = XMLUtilities.getChildElementAsInt(esetElem, "CollectionEnd", -1); // Extract CollectionEnd value or default to -1

				indexes[0] = start; // Store start value in array first element
				indexes[1] = end; // Store end value in array second element

				// Only add to map if both values were valid positive integers
				if ( start > -1 && end > -1 ) {
					esetMap.put(esetName, indexes); // Add collection index mapping to simulation's map
				}
			}
		}
	}

	// Private method loads boundary condition data from XML structure
	private void loadBcData(Element root) {
		_bcData.clear(); // Clear existing BC list before loading new data

		Element bcDataElem = root.getChild("BoundaryConditions"); // Find the BoundaryConditions container element

		// Check if BoundaryConditions element exists in XML hierarchy
		if ( bcDataElem == null ) {
			return; // Exit early if no boundary condition definitions are present to load
		}

		List kids = bcDataElem.getChildren(); // Get all child elements under BoundaryConditions container

		// Iterate through each BC child element
		for (int i = 0;i < kids.size(); i++ ) {
			Element child = (Element) kids.get(i); // Get current child as Element object
			BcData bcData = new BcData(); // Create new instance of boundary condition data

			// Attempt to load BC data into new object
			if ( bcData.loadData(child)) {
				_bcData.add(bcData); // Add successfully loaded BC object to collection
			}
		}

		BcData bcData; // Declare variable for BC data lookup

		// Loop through each loaded BC configuration
		for (int i = 0;i < _bcData.size();i++ ) {
			bcData = _bcData.get(i); // Get current BC object from list

			String metDataName = bcData.getMetDataName(); // Extract meteorology data name from BC object
			MeteorlogicData metData = getMetData(metDataName); // Look up MET data object using stored name
			bcData.setSelectedMet(metData); // Set the selected MET object reference in BC configuration

			String opsDataName = bcData.getOpsDataName(); // Extract operations data name from BC object
			OperationsData opsData = getOpsData(opsDataName); // Look up OPS data object using stored name
			bcData.setSelectedOps(opsData); // Set the selected OPS object reference in BC configuration
		}
	}

	// Private helper method retrieves OPS data by name string
	private OperationsData getOpsData(String opsDataName) {
		// Return null immediately if no name is provided to search for
		if ( opsDataName == null ) {
			return null; // Cannot lookup with null or empty identifier
		}

		OperationsData opsData; // Declare variable for loop iteration

		// Iterate through all OPS data objects in collection
		for (int i = 0;i < _opsData.size(); i++ ) {
			opsData = _opsData.get(i); // Get current OPS object from list

			// Check if name matches current object's stored name
			if ( opsDataName.equals(opsData.getName())) {
				return opsData; // Return matching OPS data object on successful match
			}
		}

		return null; // Return null if no matching OPS data found

	}

	// Public method retrieves BC data object by name string
	public BcData getBcData(String bcDataName) {
		// Return null immediately if no name is provided to search for
		if ( bcDataName == null ) {
			return null; // Cannot lookup with null or empty identifier
		}

		BcData bcData; // Declare variable for loop iteration

		// Iterate through all BC data objects in collection
		for (int i = 0;i < _bcData.size(); i++ ) {
			bcData = _bcData.get(i); // Get current BC object from list

			// Check if name matches current object's stored name
			if ( bcDataName.equals(bcData.getName())) {
				return bcData; // Return matching BC data object on successful match
			}
		}

		return null; // Return null if no matching BC data found
	}

	// Public method retrieves MET data by name string
	public MeteorlogicData getMetData(String metDataName) {
		// Return null immediately if no name is provided to search for
		if ( metDataName == null ) {
			return null; // Cannot lookup with null or empty identifier
		}

		MeteorlogicData metData; // Declare variable for loop iteration

		// Iterate through all MET data objects in collection
		for (int i = 0;i < _metData.size(); i++ ) {
			metData = _metData.get(i); // Get current MET object from list

			// Check if name matches current object's stored name
			if ( metDataName.equals(metData.getName())) {
				return metData; // Return matching MET data object on successful match
			}
		}

		return null; // Return null if no matching MET data found

	}

	// Public method retrieves TTTS by name string
	public TemperatureTargetSet getTemperatureTargetSet(String ttsName) {
		// Return null immediately if no name is provided to search for
		if ( ttsName == null ) {
			return null; // Cannot lookup with null or empty identifier
		}

		TemperatureTargetSet tts; // Declare variable for loop iteration

		// Iterate through all TTTS objects in collection
		for (int i = 0;i < _tempTargetSets.size(); i++ ) {
			tts = _tempTargetSets.get(i); // Get current TTTS object from list

			// Check if name matches current object's stored name
			if ( ttsName.equals(tts.getName())) {
				return tts; // Return matching temperature target set on successful match
			}
		}

		return null; // Return null if no matching temperature target set found
	}

	// Private method loads operations data from XML structure
	private void loadOpsData(Element root) {
		_opsData.clear(); // Clear existing OPS list before loading new data

		Element opsDataElem = root.getChild("Operations"); // Find the Operations container element

		// Check if Operations element exists in XML hierarchy
		if ( opsDataElem == null ) {
			return; // Exit early if no operations definitions are present to load
		}

		List kids = opsDataElem.getChildren(); // Get all child elements under Operations container

		// Iterate through each OPS child element
		for (int i = 0;i < kids.size(); i++ ) {
			Element child = (Element) kids.get(i); // Get current child as Element object

			OperationsData opsData = new OperationsData(); // Create new instance of operations data

			// Attempt to load OPS data into new object
			if ( opsData.loadData(child)) {
				_opsData.add(opsData); // Add successfully loaded OPS object to collection
			}
		}
	}

	// Private method loads meteorology data from XML structure
	private void loadMetData(Element root) {
		_metData.clear(); // Clear existing MET list before loading new data

		Element metDataElem = root.getChild("Meteorology"); // Find the Meteorology container element

		// Check if Meteorology element exists in XML hierarchy
		if ( metDataElem == null ) {
			return; // Exit early if no meteorology definitions are present to load
		}

		List kids = metDataElem.getChildren(); // Get all child elements under Meteorology container

		// Iterate through each MET child element
		for (int i = 0;i < kids.size(); i++ ) {
			Element child = (Element) kids.get(i); // Get current child as Element object

			MeteorlogicData metData = new MeteorlogicData(); // Create new instance of meteorology data

			// Attempt to load MET data into new object
			if ( metData.loadData(child)) {
				_metData.add(metData); // Add successfully loaded MET object to collection
			}
		}
	}

	/**
	 * Loads initial conditions configuration from XML during group loading phase.
	 * Called automatically by finishLoading after parent class has loaded simulation data.
	 *
	 * @param root The JDOM element containing serialized initial conditions configuration
	 */
	private void loadInitialConditions(Element root) {
		Element icElem = root.getChild("InitialConditions"); // Find the InitialConditions container element

		// Check if InitialConditions element exists in XML hierarchy
		if ( icElem == null ) {
			return; // Exit early if no initial conditions definitions are present to load
		}

		List kids = icElem.getChildren(); // Get all child elements under InitialConditions container

		Element child; // Declare variable for inner loop iteration

		InitialConditions ic = new InitialConditions(); // Create new instance of initial conditions
		ic.loadData(icElem); // Load configuration into the new IC object
		_initConditions = ic; // Replace existing reference with newly loaded data
	}


	/**
	 * Loads temperature target set definitions from XML during group loading phase.
	 * Called automatically by finishLoading after parent class has loaded simulation data.
	 *
	 * @param root The JDOM element containing serialized temperature target set configurations
	 */
	private void loadTempTargetSets(Element root) {
		Element tempTargetsElem = root.getChild("TemperatureTargetSets"); // Find the TemperatureTargetSets container element

		// Check if TemperatureTargetSets element exists in XML hierarchy
		if ( tempTargetsElem == null ) {
			return; // Exit early if no temperature target set definitions are present to load
		}

		List kids = tempTargetsElem.getChildren(); // Get all child elements under TemperatureTargetSets container
		Element child; // Declare variable for inner loop iteration

		TemperatureTargetSet tt; // Declare variable for TTTS object reference in loop

		// Iterate through each TTTS child element
		for (int i = 0;i < kids.size(); i++ ) {
			tt = new TemperatureTargetSet(); // Create new instance of temperature target set
			child = (Element) kids.get(i); // Get current child from collection

			// Attempt to load TTTS data into new object
			if ( tt.loadData(child)) {
				_tempTargetSets.add(tt); // Add successfully loaded TTTS object to collection
			}
		}
	}


	/**
	 * Performs initialization for loading configuration from XML files.
	 * Clears ensemble sets list to ensure fresh load during deserialization.
	 */
	@Override
	protected void initForLoading() {
		_tempTargetSets.clear(); // Clear TTTS collection before loading new configuration data
	}

	/**
	 * Returns the element name string that should be used for this simulation group type in XML.
	 * Required by parent class to properly serialize and deserialize this specific group type.
	 */
	@Override
	protected String getSimulationGroupType() {
		return "ForecastSimulationGroup"; // Return string literal identifying this group's XML root tag
	}

	/**
	 * Loads settings for specific simulations from the configuration file.
	 * This placeholder method is called during individual simulation loading but contains no logic
	 * as forecast-specific configuration is handled at the group level rather than per-simulation.
	 */
	@Override
	protected void loadSimulationSettings(Element simElem, String simName) {

	}

	/**
	 * Called at the end of the XML saving process to save data that's not simulation specific.
	 * This method persists boundary condition, initial condition, temperature target, and ensemble set
	 * definitions before individual simulation settings are written by each simulation object.
	 */
	@Override
	protected void finishSaving(Element root) {
		saveTempTargets(root); // Save temperature target set configurations to XML element
		saveInitialConditions(root); // Save initial conditions configuration to XML element
		saveOpsData(root); // Save operations data definitions to XML element
		saveMetData(root); // Save meteorology data definitions to XML element
		saveBcData(root); // Save boundary condition configurations to XML element
		saveEnsembleSets(root); // Save ensemble sets collection mapping to XML element
	}

	private void saveEnsembleSets(Element root) {
		Element ecElem = new Element("EnsembleSets"); // Create container element for all ensemble set data
		root.addContent(ecElem); // Append EnsembleSets element to parent container object

		EnsembleSet eSet; // Declare variable for loop iteration during saving
		Set<Map.Entry<String, List<EnsembleSet>>> entrySet = _ensembleSets.entrySet(); // Get all key-value pairs from map

		Iterator<Map.Entry<String, List<EnsembleSet>>> iter = entrySet.iterator(); // Create iterator for sequential traversal

		// Loop through each simulation name and its ensemble sets collection
		while (iter.hasNext()) {
			Map.Entry<String, List<EnsembleSet>> next = iter.next(); // Get next entry from iterator
			String simName = next.getKey(); // Extract simulation name from map key value

			Element simElem = new Element("Simulation"); // Create element for current simulation's data
			simElem.setText(simName); // Set text content to the simulation name string
			ecElem.addContent(simElem); // Append Simulation element to EnsembleSets container object

			List<EnsembleSet> ensembleSets = next.getValue(); // Get collection of ensemble sets for this simulation

			// Iterate through each ensemble set belonging to this simulation
			for (int i = 0; i < ensembleSets.size(); i++) {
				eSet = ensembleSets.get(i); // Get current ensemble set from list
				eSet.saveData(simElem); // Delegate save operation to nested class implementation
			}

			System.out.println("simElem has "+ simElem.getChildren("EnsembleSet").size() + " ensembleSets"); // Debug output for development visibility
		}

		saveEnsembleSetsIndexing(root); // After all ensembles saved, persist collection index mappings
	}

	// Private method saves collection index mapping to XML
	private void saveEnsembleSetsIndexing(Element root) {
		Element esiElem = new Element("EnsembleSetIndexing"); // Create container element for all ensemble set index data
		root.addContent(esiElem); // Append EnsembleSetIndexing element to parent container object

		Set<Map.Entry<String, Map<String, int[]>>> entrySet2 = _ensembleSetIndexing.entrySet(); // Get all key-value pairs from nested map
		Iterator<Map.Entry<String, Map<String, int[]>>> iter2 = entrySet2.iterator(); // Create iterator for sequential traversal

		// Loop through each simulation name and its ensemble set index mapping
		while (iter2.hasNext()) {
			Map.Entry<String, Map<String, int[]>> simEsetMap = iter2.next(); // Get next entry from iterator
			String simName = simEsetMap.getKey(); // Extract simulation name from nested map key value

			Element simElem = new Element("Simulation"); // Create element for current simulation's index data
			simElem.setAttribute("Name", simName); // Set Name attribute on the Simulation element object

			esiElem.addContent(simElem); // Append Simulation element to EnsembleSetIndexing container object
			Map<String, int[]> esetMap = simEsetMap.getValue(); // Get nested map holding ensemble set index values

			Set<Map.Entry<String, int[]>> esetSet = esetMap.entrySet(); // Get all key-value pairs from inner map
			Iterator<Map.Entry<String, int[]>> esetIter = esetSet.iterator(); // Create iterator for sequential traversal

			// Loop through each ensemble set name and its index array pair
			while (esetIter.hasNext()) {
				Map.Entry<String, int[]> esetEntry = esetIter.next(); // Get next entry from iterator
				String esetName = esetEntry.getKey(); // Extract ensemble set name from nested map key value

				int[] esetIndexes = esetEntry.getValue(); // Get array holding start and end collection indices

				Element esetElem  = new Element("EnsembleSet"); // Create element for this single indexed set

				esetElem.setAttribute("Name", esetName); // Set Name attribute on the EnsembleSet element object

				simElem.addContent(esetElem); // Append EnsembleSet element to Simulation element object

				Element startElem = new Element("CollectionStart"); // Create element for start collection index
				startElem.setText(String.valueOf(esetIndexes[0])); // Convert array first value to string and set as text content

				esetElem.addContent(startElem); // Append CollectionStart element to EnsembleSet container object

				Element endElem = new Element("CollectionEnd"); // Create element for end collection index

				endElem.setText(String.valueOf(esetIndexes[1])); // Convert array second value to string and set as text content
				esetElem.addContent(endElem); // Append CollectionEnd element to EnsembleSet container object
			}
		}
	}

	// Private method saves boundary condition configurations to XML
	private void saveBcData(Element root) {
		Element bcElem = new Element("BoundaryConditions"); // Create container element for all BC data
		root.addContent(bcElem); // Append BoundaryConditions element to parent container object

		BcData bcData; // Declare variable for loop iteration during saving

		// Iterate through each BC configuration in collection
		for (int i = 0;i < _bcData.size(); i++ ) {
			bcData = _bcData.get(i); // Get current BC object from list
			bcData.saveData(bcElem); // Delegate save operation to inner class implementation
		}
	}

	// Private method saves operations data definitions to XML
	private void saveOpsData(Element root) {
		Element opsElem = new Element("Operations"); // Create container element for all OPS data
		root.addContent(opsElem); // Append Operations element to parent container object

		OperationsData opsData; // Declare variable for loop iteration during saving

		// Iterate through each OPS definition in collection
		for (int i = 0;i < _opsData.size(); i++ ) {
			opsData = _opsData.get(i); // Get current OPS object from list
			opsData.saveData(opsElem); // Delegate save operation to inner class implementation
		}
	}

	// Private method saves meteorology data definitions to XML
	private void saveMetData(Element root) {
		Element metElem = new Element("Meteorology"); // Create container element for all MET data
		root.addContent(metElem); // Append Meteorology element to parent container object

		MeteorlogicData metData; // Declare variable for loop iteration during saving

		// Iterate through each MET definition in collection
		for (int i = 0;i < _metData.size(); i++ ) {
			metData = _metData.get(i); // Get current MET object from list
			metData.saveData(metElem); // Delegate save operation to inner class implementation
		}
	}


	/**
	 * Saves initial conditions configuration to XML during group saving phase.
	 * Called automatically by finishSaving after parent class has saved simulation data.
	 *
	 * @param root The JDOM element that should contain the serialized initial conditions data
	 */
	private void saveInitialConditions(Element root) {
		// Check if initial conditions object has been loaded from file or set manually
		if (_initConditions != null ) {
			Element icElem = new Element("InitialConditions"); // Create container element for IC configuration
			root.addContent(icElem); // Append InitialConditions element to parent container object
			_initConditions.saveData(icElem); // Delegate save operation to IC data class implementation
		}
	}


	/**
	 * Saves temperature target set configurations to XML during group saving phase.
	 * Called automatically by finishSaving after parent class has saved simulation data.
	 *
	 * @param root The JDOM element that should contain the serialized temperature target set data
	 */
	private void saveTempTargets(Element root) {
		Element tempTargetsElem = new Element("TemperatureTargetSets"); // Create container element for all TTTS
		root.addContent(tempTargetsElem); // Append TemperatureTargetSets element to parent container object

		TemperatureTargetSet tt; // Declare variable for loop iteration during saving

		// Iterate through each temperature target set in collection
		for (int i = 0; i < _tempTargetSets.size(); i++ ) {
			tt = _tempTargetSets.get(i); // Get current TTTS object from list

			// Verify object reference exists before attempting to save
			if(tt != null) {
				tt.saveData(tempTargetsElem); // Delegate save operation to TTTS class implementation
			}
		}
	}

	/**
	 * Saves settings for specific simulations to XML during group saving phase.
	 * This placeholder method is called during individual simulation saving but contains no logic
	 * as forecast-specific configuration is handled at the group level rather than per-simulation.
	 */
	@Override
	protected void saveSimulationSettings(Element simelem, String simName) {

	}

	/**
	 * Sets initial conditions object for this forecast simulation group.
	 * Marks the group as modified after assignment to trigger persistence on next save operation.
	 *
	 * @param ics The InitialConditions object representing starting state configuration
	 */
	public void setInitialConditions(InitialConditions ics) {
		setModified(true); // Mark as modified to indicate data changes requiring save
		_initConditions = ics; // Assign new initial conditions object to instance variable
	}

	/**
	 * Returns the current initial conditions object for this forecast simulation group.
	 *
	 * @return The InitialConditions object, or null if not yet configured
	 */
	public InitialConditions getInitialConditions() {
		return _initConditions; // Return stored initial conditions object reference
	}

	/**
	 * Sets a new collection of temperature target sets for this forecast simulation group.
	 * Clears existing TTTS list and adds new items if provided, then marks as modified.
	 *
	 * @param tt The List<EnsembleSet> of TemperatureTargetSet objects to set
	 */
	public void setTemperatureTargetSets(List<TemperatureTargetSet> tt) {
		setModified(true); // Mark as modified to indicate data changes requiring save
		_tempTargetSets.clear(); // Clear existing temperature target sets before adding new ones

		// Only proceed if incoming collection is not null
		if ( tt != null ) {
			_tempTargetSets.addAll(tt); // Add all TTTS objects from parameter to instance collection
		}
	}

	/**
	 * Returns the current collection of temperature target sets for this forecast simulation group.
	 *
	 * @return List containing TemperatureTargetSet objects currently associated with this group
	 */
	public List<TemperatureTargetSet> getTemperatureTargetSets() {
		return _tempTargetSets; // Return stored temperature target sets collection reference
	}

	/**
	 * Returns the current collection of meteorology data definitions for this forecast simulation group.
	 *
	 * @return List containing MeteorlogicData objects currently associated with this group
	 */
	public List<MeteorlogicData> getMeteorlogyData() {
		return _metData; // Return stored meteorology data collection reference
	}

	/**
	 * Sets a new collection of meteorology data definitions for this forecast simulation group.
	 * Clears existing MET list and adds new items if provided, then marks as modified.
	 *
	 * @param metDataList The List<MeteorlogicData> of MET objects to set
	 */
	public void setMeteorlogyData(List<MeteorlogicData>metDataList) {
		_metData.clear(); // Clear existing meteorology data before adding new ones

		// Only proceed if incoming collection is not null
		if ( metDataList != null ) {
			_metData.addAll(metDataList); // Add all MET objects from parameter to instance collection
		}

		setModified(true); // Mark as modified to indicate data changes requiring save
	}


	/**
	 * Returns the current collection of operations data definitions for this forecast simulation group.
	 *
	 * @return List containing OperationsData objects currently associated with this group
	 */
	public List<OperationsData> getOperationsData() {
		return _opsData; // Return stored operations data collection reference
	}

	/**
	 * Sets a new collection of operations data definitions for this forecast simulation group.
	 * Clears existing OPS list and adds new items if provided, then marks as modified.
	 *
	 * @param opsDataList The List<OperationsData> of OPS objects to set
	 */
	public void setOperationsData(List<OperationsData>opsDataList) {
		_opsData.clear(); // Clear existing operations data before adding new ones

		// Only proceed if incoming collection is not null
		if ( opsDataList != null ) {
			_opsData.addAll(opsDataList); // Add all OPS objects from parameter to instance collection
		}

		setModified(true); // Mark as modified to indicate data changes requiring save
	}

	// Getter method retrieves BC collection
	public List<BcData> getBcData() {
		return _bcData; // Return stored boundary condition configuration collection reference
	}

	// Setter configures ensembles for a specific simulation
	public void setEnsembleSets(WatSimulation sim, List<EnsembleSet> ensembleSets) {
		List<EnsembleSet> currentEnsembleSets = _ensembleSets.get(sim.getName()); // Retrieve existing ensemble list for this simulation

		// Check if no existing collection exists for this simulation
		if ( currentEnsembleSets == null ) {
			List<EnsembleSet>sets = new ArrayList<>(); // Create new empty list to hold ensembles
			sets.addAll(ensembleSets); // Add all provided ensemble sets to new collection
			_ensembleSets.put(sim.getName(), sets); // Store new collection in map using sim name as key

		} else {
			// If existing collection already exists for this simulation
			currentEnsembleSets.clear(); // Clear existing collections before replacing with new ones
			currentEnsembleSets.addAll(ensembleSets); // Add all provided ensemble sets to existing collection
		}

		setModified(true); // Mark as modified to indicate data changes requiring save
	}

	// Getter retrieves ensembles for a specific simulation
	public List<EnsembleSet> getEnsembleSets(WatSimulation sim) {
		// Return empty list if simulation reference is missing
		if ( sim == null ) {
			return new ArrayList<>(); // Return empty list on invalid call
		}

		List<EnsembleSet> ensembleSets = _ensembleSets.get(sim.getName()); // Retrieve collection for this specific simulation

		// Check if no collection exists for this simulation yet
		if ( ensembleSets == null ) {
			return new ArrayList<>(); // Return empty list on missing entry
		}

		return ensembleSets; // Return the retrieved collection reference or empty list
	}

	// Method checks if an ensembleset exists with these configs
	public boolean hasEnsembleSetFor(WatSimulation sim, BcData bc, TemperatureTargetSet tts) {
		// Check if either required configuration object is missing
		if ( bc == null || tts == null ) {
			return false; // Return false on invalid inputs
		}

		EnsembleSet eset = getEnsembleSetFor(sim, bc, tts); // Look up the matching ensemble set using three criteria

		return eset != null ; // Return whether a matching ensemble set was found or not
	}

	// Method retrieves ensembleset by all three configs
	public EnsembleSet getEnsembleSetFor(WatSimulation sim, BcData bc, TemperatureTargetSet tts) {
		// Check if any required parameter object is missing
		if ( sim == null || bc == null || tts == null ) {
			return null; // Return null on invalid inputs
		}

		EnsembleSet eset; // Declare variable for loop iteration

		List<EnsembleSet>ensembleSets = _ensembleSets.get(sim.getName()); // Retrieve collection for this specific simulation

		// Check if no collection exists for this simulation yet
		if (ensembleSets == null ) {
			return null; // Return null on missing entry
		}

		// Iterate through each ensemble set in the collection
		for (int e = 0; e < ensembleSets.size(); e++ ) {
			eset = ensembleSets.get(e); // Get current ensemble set object from list

			// Check if both BC and TTTS references match exactly
			if ( eset.getBcData() == bc && eset.getTemperatureTargetSet() == tts ) {
				return eset; // Return matching ensemble set on successful find
			}
		}

		return null; // Return null after iterating through all ensembles without finding a match
	}

	// Private method removes sets using a specific temp target
	private void deleteEnsembleSetsFor(TemperatureTargetSet temperatureTargetSet) {
		List<EnsembleSet> esetsToRemove = getEnsembleSetsUsingTempTargetSet(temperatureTargetSet); // Get list of ensemblesets that use this TTTS

		// Loop through each simulation in the map
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet()) {
			List<EnsembleSet> esets = entry.getValue(); // Get collection for current simulation
			esets.removeIf(esetsToRemove::contains); // Remove any ensembles that are in the removal list using Java 8 predicate
		}
	}

	// Private method removes sets using a specific BC data
	private void deleteEnsembleSetsFor(BcData bcData) {
		List<EnsembleSet> esetsToRemove = getEnsembleSetsUsingBcData(bcData); // Get list of ensemblesets that use this BC

		// Loop through each simulation in the map
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet()) {
			List<EnsembleSet> esets = entry.getValue(); // Get collection for current simulation
			esets.removeIf(esetsToRemove::contains); // Remove any ensembles that are in the removal list using Java 8 predicate
		}
	}

	// Method finds sets using a specific TTTS
	public List<EnsembleSet> getEnsembleSetsUsingTempTargetSet(TemperatureTargetSet temperatureTargetSet) {
		LinkedHashSet<EnsembleSet> eSetsUsingTTSet = new LinkedHashSet<>(); // Create ordered set to maintain insertion order

		// Iterate through each simulation in the map
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet()) {
			List<EnsembleSet> esets = entry.getValue(); // Get collection for current simulation

			// Check each ensemble set belonging to this simulation
			for(EnsembleSet eset : esets) {
				// Compare using null-safe equals method
				if(Objects.equals(eset.getTemperatureTargetSet(), temperatureTargetSet)) {
					eSetsUsingTTSet.add(eset); // Add matching ensemble to ordered collection
				}
			}
		}

		return new ArrayList<>(eSetsUsingTTSet); // Convert back to regular ArrayList before returning
	}

	// Method removes a single ensemble set by reference
	public boolean deleteEnsembleSet(WatSimulation sim, EnsembleSet eset) {
		// Return false if the ensemble set object is null
		if ( eset == null ) {
			return false; // Cannot remove null references
		}

		List<EnsembleSet>ensembleSets = _ensembleSets.get(sim.getName()); // Get collection for this specific simulation

		// Check if no collection exists for this simulation yet
		if (ensembleSets == null ) {
			return false; // Cannot remove from missing collection
		}

		boolean rv = ensembleSets.remove(eset); // Attempt to remove the set and capture result

		// Mark as modified only if removal was successful
		if ( rv ) {
			setModified(true); // Indicate data changes requiring save operation
		}

		return rv; // Return whether the removal operation succeeded or failed
	}

	// Method adds a single ensemble set by reference
	public boolean addEnsembleSet(WatSimulation sim, EnsembleSet ensembleSet) {
		// Return false if the ensemble set object is null
		if (ensembleSet == null) {
			return false; // Cannot add null references
		}

		EnsembleSet existingESet = getEnsembleSetFor(sim, ensembleSet.getBcData(), ensembleSet.getTemperatureTargetSet()); // Check for duplicate using all three criteria

		// Return failure if a matching set already exists for these configurations
		if (existingESet != null) {
			return false; // Cannot add duplicates to collection
		}

		List<EnsembleSet> ensembleSets = _ensembleSets.get(sim.getName()); // Get collection for this specific simulation

		// If no collection exists yet, create one
		if (ensembleSets == null) {
			ensembleSets = new ArrayList<>(); // Create new empty list to hold ensembles
			_ensembleSets.put(sim.getName(), ensembleSets); // Store new collection in map using sim name as key
		}

		ensembleSets.add(ensembleSet); // Add the new ensemble set to the collection

		Map<String, int[]> ensembleIndexingMap = _ensembleSetIndexing.get(sim.getName()); // Get or create indexing map for this simulation

		int[] indexing = getEnsembleSetCollectionIndexing(sim, ensembleSet); // Get appropriate collection index range

		setModified(true); // Mark as modified to indicate data changes requiring save operation

		return true; // Indicate successful addition
	}

	// Method retrieves or creates collection indexing
	public int[] getEnsembleSetCollectionIndexing(WatSimulation sim, EnsembleSet ensembleSet) {
		Map<String, int[]> ensembleIndexingMap = _ensembleSetIndexing.get(sim.getName()); // Get indexing map for this simulation

		int[] indexing; // Declare variable to hold indexing array

		// If no indexing exists yet, create new one
		if (ensembleIndexingMap == null) {
			indexing = getNextCollectionIndexing(sim); // Calculate next available collection range from existing data

			ensembleIndexingMap = new HashMap<>(); // Create new map to hold collection index associations
			ensembleIndexingMap.put(ensembleSet.getName(), indexing); // Store first index mapping using set name as key
			_ensembleSetIndexing.put(sim.getName(), ensembleIndexingMap); // Store new map in nested dictionary
			setModified(true); // Mark as modified after creating new collection index
		} else {
			// If indexing already exists for this simulation
			indexing = ensembleIndexingMap.get(ensembleSet.getName()); // Look up existing index if it's been created before

			// If still no entry exists for this particular set, calculate next available
			if ( indexing == null ) {
				indexing = getNextCollectionIndexing(sim); // Calculate next available collection range from existing data

				ensembleIndexingMap.put(ensembleSet.getName(), indexing); // Store new index mapping using set name as key

				setModified(true); // Mark as modified after creating new collection index
			}
		}

		return indexing; // Return the array containing start and end collection indices for this ensemble
	}

	// Private helper calculates next available range
	private int[] getNextCollectionIndexing(WatSimulation simulation) {
		Map<String, int[]> ensembleIndexMap = _ensembleSetIndexing.get(simulation.getName()); // Get existing indexing map for this simulation

		// If no indexing map exists yet, use default configuration
		if ( ensembleIndexMap == null ) {
			Integer max = Integer.getInteger("Forecast.EnsembleSetRange", 500); // Get configured range or use default value of 500
			return new int []{0,max.intValue()-1}; // Return initial range starting at 0 with configurable end
		}

		Collection<int[]> values = ensembleIndexMap.values(); // Get all existing index arrays from the map
		Iterator<int[]> iter = values.iterator(); // Create iterator to traverse all existing ranges

		int max = -1; // Initialize maximum seen collection index counter

		// Loop through each existing collection range
		while (iter.hasNext()) {
			int[] indexes = iter.next(); // Get next array from collection
			max = Math.max(indexes[1], max); // Track highest end index seen so far
		}

		int[] indexs = new int[2]; // Create new array to hold new start and end values
		indexs[0] = max+1; // Set next start value after highest existing range
		indexs[1] = indexs[0]+ Integer.getInteger("Forecast.EnsembleSetRange", 500); // Set end value using configured range default
		indexs[1]--; // Adjust end to be one less than the range size (exclusive upper bound)

		return indexs; // Return new array containing start and end collection indices
	}

	// Method returns entire indexing map for a simulation
	public Map<String, int[]>getSimulationEnsembleSetIndexing(WatSimulation simulation) {
		Map<String, int[]> ensembleIndexMap = _ensembleSetIndexing.get(simulation.getName()); // Get or null if not present
		return ensembleIndexMap; // Return map or null as-is to caller
	}

	// Getter retrieves ensembles for a specific simulation
	public List<EnsembleSet> getEnsembleSetsFor(WatSimulation simulation) {
		return _ensembleSets.get(simulation.getName()); // Return collection from nested map using sim name as key
	}

	// Method removes a TTTS object and its associated ensembles
	public void removeTemperatureTargetSet(TemperatureTargetSet set) {
		// Remove TTTS from the main list if it was found there
		if(_tempTargetSets.remove(set)) {
			deleteEnsembleSetsFor(set); // After removing TTTS, remove any ensemble sets that used this configuration
		}
	}

	// Method removes OPS data and cascades to related BCs
	public void removeOperationsData(OperationsData operationsData) {
		// Remove OPS from the main list if it was found there
		if(_opsData.remove(operationsData)) {
			List<BcData> bcDataUsingOpsData = getBcDataUsingOperationsData(operationsData); // Get BC objects that reference this OPS

			// Loop through each BC that uses this OPS
			for(BcData bcDataToRemove : bcDataUsingOpsData) {
				removeBcData(bcDataToRemove); // Remove the entire BC including its ensembles
			}
		}
	}

	// Method removes MET data and cascades to related BCs
	public void removeMetData(MeteorlogicData meteorologicData) {
		// Remove MET from the main list if it was found there
		if(_metData.remove(meteorologicData)) {
			List<BcData> bcDataUsingOpsData = getBcDataUsingMetData(meteorologicData); // Get BC objects that reference this MET

			// Loop through each BC that uses this MET
			for(BcData bcDataToRemove : bcDataUsingOpsData) {
				removeBcData(bcDataToRemove); // Remove the entire BC including its ensembles
			}
		}
	}

	// Method removes a BC and deletes associated ensemble sets
	public void removeBcData(BcData bcData) {
		// Remove BC from the main list if it was found there
		if(_bcData.remove(bcData)) {
			deleteEnsembleSetsFor(bcData); // Delete any ensemble sets that used this BC configuration
		}
	}

	// Method finds all BCs using a specific OPS object
	public List<BcData> getBcDataUsingOperationsData(OperationsData operationsData) {
		List<BcData> retVal = new ArrayList<>(); // Create collection to hold matching results

		// Loop through each BC in the collection
		for(BcData bc : _bcData) {
			// Check if this BC's OPS reference matches the parameter exactly
			if(bc.getOperationsData() == operationsData) {
				retVal.add(bc); // Add matching BC to results list
			}
		}

		return retVal; // Return collection of BCs that reference the specified operations data
	}

	// Method finds all BCs using a specific MET object
	public List<BcData> getBcDataUsingMetData(MeteorlogicData metData) {
		List<BcData> retVal = new ArrayList<>(); // Create collection to hold matching results

		// Loop through each BC in the collection
		for(BcData bc : _bcData) {
			// Check if this BC's MET reference matches the parameter exactly (note: typo in original preserved)
			if(bc.getMeteorogicalData() == metData) {
				retVal.add(bc); // Add matching BC to results list
			}
		}

		return retVal; // Return collection of BCs that reference the specified meteorology data
	}

	// Method finds all ensembles using a specific BC
	public List<EnsembleSet> getEnsembleSetsUsingBcData(BcData bcData) {
		LinkedHashSet<EnsembleSet> eSetsUsingTTSet = new LinkedHashSet<>(); // Create ordered set to maintain insertion order

		// Iterate through each simulation in the map
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet()) {
			List<EnsembleSet> esets = entry.getValue(); // Get collection for current simulation

			// Check each ensemble set belonging to this simulation
			for(EnsembleSet eset : esets) {
				// Compare BC reference using null-safe equals method
				if(Objects.equals(eset.getBcData(), bcData)) {
					eSetsUsingTTSet.add(eset); // Add matching ensemble to ordered collection
				}
			}
		}

		return new ArrayList<>(eSetsUsingTTSet); // Convert back to regular ArrayList before returning
	}
}
