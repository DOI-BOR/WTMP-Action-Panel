package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList; // Import ArrayList for creating dynamic lists when filtering data location entries
import java.util.HashMap; // Import HashMap for storing configuration keyed by model alternative identifiers
import java.util.HashSet; // Import HashSet for efficient deduplication of data location references during sync
import java.util.Iterator; // Import Iterator for sequential traversal of configuration entry sets during save/load
import java.util.List; // Import List interface for maintaining ordered collections of data locations
import java.util.Map; // Import Map interface for storing model alternative settings by key string
import java.util.Map.Entry; // Import Entry class for accessing key-value pairs in model settings map
import java.util.Set; // Import Set interface for managing unique collections during data location filtering

import org.jdom.Element; // Import Element class for creating child XML elements during serialization operations

import com.rma.util.XMLUtilities; // Import utility helper class for persisting Java objects to/from JDOM XML structures

import hec2.model.DataLocation; // Import model alternative data location definition representing input/output sources
import hec2.plugin.model.ModelAlternative; // Import model alternative representing a specific simulation configuration
import hec2.wat.plugin.SimpleWatPlugin; // Import simple plugin implementation for retrieving default directories
import hec2.wat.plugin.WatPlugin; // Import generic plugin interface for model program type identification
import hec2.wat.plugin.WatPluginManager; // Import manager class to retrieve plugin instances by program name


/**
 * BaseComputeSettings is an abstract base class defining the core structure for computing settings used in iterative
 * sensitivity analysis and position analysis workflows. It maintains configuration for multiple model alternatives,
 * tracks which ensemble members should be computed, and handles synchronization of data locations between persistent
 * XML storage and runtime plugin configurations.
 *
 * This abstract class provides:
 *
 *   Map-based storage for model alternative-specific iteration settings
 *   Synchronization logic to reconcile data location changes detected during compute sessions
 *   XML persistence methods for saving and loading settings from disk
 *
 *
 * Subclasses must implement:
 *
 *   getSettingElementString() - Returns the XML element name used to wrap model alternative settings
 *   getCollectionDssFilename() - Returns the filename for storing computed ensemble outputs
 *
 */

public abstract class BaseComputeSettings {
	// Map storing ModelAltIterationSettings keyed by model alternative program+name combination
	private Map<String, ModelAltIterationSettings> _modelAltSettings = new HashMap<>(); // Configuration dictionary for each model alternative

	// Array of ensemble member indices to include in the computation run (e.g., 0, 1, 2 for 3-member ensemble)
	private int[] _membersToCompute; // List of iteration member numbers to process during sensitivity analysis

	// Maximum member index defining the total size of the ensemble to compute over (0-indexed upper bound)
	private int _maxMember; // Upper limit on iteration member count for array bounds checking

	public BaseComputeSettings() {
		super();
	}

	/**
	 * Retrieves configuration settings for a specific model alternative, creating default settings if needed.
	 * Handles synchronization of data locations between persisted XML values and current plugin metadata
	 * to ensure consistency when models have been added or removed since last save. Returns null if model is invalid.
	 *
	 * @param modelAlt The ModelAlternative object whose settings should be retrieved
	 * @return ModelAltIterationSettings containing all configuration for this alternative, or null if model is invalid
	 */
	public ModelAltIterationSettings getModelAltSettings(ModelAlternative modelAlt) {
		// Return null immediately if model alternative reference is missing
		if (modelAlt == null) {
			return null; // Cannot create settings for null reference
		}

		ModelAltIterationSettings mAltSettings = _modelAltSettings.get(getKey(modelAlt)); // Look up existing settings by key

		// If settings missing or have no data locations yet
		if (mAltSettings == null || mAltSettings.getDataLocations().size() == 0) {
			// Create fresh settings populated from current plugin metadata
			mAltSettings = createDefaultModelAltInterationSettings(modelAlt);

		} else {
			// If settings exist with data, ensure they're in sync with plugin state
			syncDataLocations(modelAlt, mAltSettings); // Call helper to reconcile any changes
			mAltSettings.setModelAlternative(modelAlt); // Link settings back to model alternative reference
		}

		return mAltSettings; // Return the initialized or retrieved settings object
	}

	/**
	 * Synchronizes the data locations list between what was read from persistent XML storage and what
	 * the current plugin reports, detecting both additions (new locations added) and deletions
	 * (old locations removed). Updates the settings collection to reflect only locations that currently exist.
	 *
	 * @param modelAlt     The ModelAlternative object to use for retrieving current plugin data location list
	 * @param mAltSettings The ModelAltIterationSettings object whose data locations will be updated in-place
	 */
	private void syncDataLocations(ModelAlternative modelAlt, ModelAltIterationSettings mAltSettings) {
		List<DataLocation> pluginDataLocs = getDataLocations(modelAlt); // Retrieve fresh list of all data locations from plugin
		List<DataLocation> savedDataLocs = mAltSettings.getDataLocations(); // Get list currently stored in settings object

		Set<DataLocation> pluginDataLocsSet = new HashSet<>(pluginDataLocs); // Convert plugin list to set for efficient operations
		Set<DataLocation> savedDataLocsSet = new HashSet<>(savedDataLocs); // Convert saved list to set for comparison

		// find new DataLocations - Remove entries from saved that are missing in current plugin state
		pluginDataLocsSet.removeAll(savedDataLocs); // Compute difference: locations added since last save

		savedDataLocs.addAll(pluginDataLocsSet); // Add newly discovered locations to settings list

		// find deleted DataLocations - Remove entries from saved that exist in plugin but not in saved
		savedDataLocsSet.removeAll(pluginDataLocs); // Compute difference: locations removed since last save
		savedDataLocs.removeAll(savedDataLocsSet); // Remove deleted locations from settings list to clean up
		mAltSettings.updateDataLocations(savedDataLocs); // Call setter on settings to replace with synchronized list
	}

	/**
	 * Generates a unique key string for a model alternative by combining its program type and name.
	 * Used as the map key for storing and retrieving alternative-specific configuration settings.
	 *
	 * @param modelAlt The ModelAlternative object from which to extract identifier components
	 * @return String key in format "Program-Name" (e.g., "CE-Qual-W2-Model1")
	 */

	public static String getKey(ModelAlternative modelAlt) {
		return modelAlt.getProgram() + "-" + modelAlt.getName(); // Concatenate program and name into single identifier
	}

	/**
	 * Creates default ModelAltIterationSettings for a given model alternative by reading data location
	 * information from the associated plugin. Stores the new settings in the internal map after creation.
	 * Only called when no settings exist yet for a particular model alternative. Returns null if no valid
	 * plugin can provide data location list or model is invalid.
	 *
	 * @param modelAlt The ModelAlternative object to create settings for
	 * @return Newly created ModelAltIterationSettings with populated data locations, or null if creation fails
	 */

	private ModelAltIterationSettings createDefaultModelAltInterationSettings(ModelAlternative modelAlt) {

		// Return null immediately if model alternative is missing
		if (modelAlt == null) {
			return null; // Cannot create settings for null reference
		}

		List<DataLocation> dataLocs = getDataLocations(modelAlt); // Get list of data locations from current plugin state

		// Only proceed if plugin returned a valid data location list
		if (dataLocs != null) {
			ModelAltIterationSettings settings = new ModelAltIterationSettings(dataLocs); // Create settings object with populated data
			_modelAltSettings.put(getKey(modelAlt), settings); // Store in map using composite key as index
			return settings; // Return newly created settings to caller
		}

		return null; // Return null if no data locations were available from plugin
	}

	/**
	 * Retrieves the list of data locations that can be used for sensitivity analysis or position analysis.
	 * Queries the appropriate WatPlugin implementation based on model alternative program type and filters
	 * to only include DSS-linked locations (not abstract base class instances). Returns null if no plugin found.
	 *
	 * @param modelAlt The ModelAlternative object whose data locations should be retrieved
	 * @return List of DataLocation objects representing input/output paths for this model alternative, or null if none available
	 */
	private List<DataLocation> getDataLocations(ModelAlternative modelAlt) {
		String program = modelAlt.getProgram(); // Extract program type identifier string

		SimpleWatPlugin splugin = WatPluginManager.getPlugin(program); // Retrieve plugin object from manager by program name

		// Verify retrieved plugin matches expected generic interface
		if (splugin instanceof WatPlugin) {
			WatPlugin plugin = (WatPlugin) splugin; // Safe cast to generic interface for use in method calls

			List<DataLocation> dataLocs = plugin.getDataLocations(modelAlt, DataLocation.INPUT_LOCATIONS); // Query plugin for available input locations
			dataLocs = filterDataLocs(dataLocs); // Filter list to only include DSS-linked locations

			return dataLocs; // Return filtered list of data location objects
		}

		return null; // Return null if plugin was not found or casting failed
	}

	/**
	 * Filters the list of DataLocation objects to retain only those that are linked to actual DSS files.
	 * Excludes abstract base class instances and keeps only concrete implementations with valid linked-to data sources.
	 *
	 * @param dataLocs The complete list of data locations returned by the plugin, possibly including non-DSS types
	 * @return Filtered list containing only DSS-linked DataLocation objects
	 */

	private List<DataLocation> filterDataLocs(List<DataLocation> dataLocs) {

		List<DataLocation> dssDataLocs = new ArrayList<>(); // Create empty list to collect valid results

		// Return empty list if input collection is null
		if (dataLocs == null) {
			return dssDataLocs; // Cannot filter from null reference
		}

		DataLocation dataLoc; // Declare loop variable for each element in collection

		// Iterate through each entry in the list
		for (int i = 0; i < dataLocs.size(); i++) {
			dataLoc = dataLocs.get(i); // Get current data location object

			// Check if not a base class instance
			if (!dataLoc.getClass().equals(DataLocation.class)) {
				continue; // Skip abstract base class types, only want concrete subclasses
			}

			// Only keep entries with valid linked-to location set
			if (dataLoc.getLinkedToLocation() instanceof DataLocation) {
				dssDataLocs.add(dataLoc); // Add to results list if it has a linked DSS source
			}
		}

		return dssDataLocs; // Return filtered collection with only DSS-linked data locations

	}

	/**
	 * Replaces the entire internal map of model alternative settings with provided values.
	 * Clears existing configuration and populates with new settings from input parameters, allowing bulk updates.
	 *
	 * @param modelAltSettings Map containing new settings keyed by model alternative composite identifier strings
	 */
	public void setModelAltSettings(Map<String, ModelAltIterationSettings> modelAltSettings) {
		_modelAltSettings.clear(); // Empty the existing configuration map

		// Only proceed if input collection is not null
		if (modelAltSettings != null) {
			_modelAltSettings.putAll(modelAltSettings); // Copy all entries from input map to internal storage
		}
	}

	/**
	 * Sets the array of member indices that should be included when running an iterative or position analysis compute.
	 * Controls which ensemble members (iterations) will have their data processed and stored in collection files.
	 *
	 * @param computeMembers Array of integer indices representing which ensemble members to process (e.g., {0,1,2} for 3-member ensemble)
	 */
	public void setMembersToCompute(int[] computeMembers) {
		_membersToCompute = computeMembers; // Copy array reference from parameter to instance variable
	}

	/**
	 * Retrieves the configured array of member indices for this compute settings instance.
	 * Returns null if no members have been explicitly set via setMembersToCompute().
	 *
	 * @return Array of integer member indices, or null if not yet configured
	 */
	public int[] getMembersToCompute() {
		return _membersToCompute; // Return stored reference to configuration array
	}

	/**
	 * Sets the maximum member index value defining the total size of the ensemble being computed.
	 * Used for validation and boundary checking during iterative compute operations.
	 *
	 * @param maxMember The upper bound member index (0-indexed) representing total ensemble size
	 */
	public void setMaximumMember(int maxMember) {
		_maxMember = maxMember; // Assign value to instance variable
	}

	/**
	 * Retrieves the maximum member index for this compute settings instance.
	 * Returns 0 if no maximum has been explicitly set via setMaximumMember().
	 *
	 * @return Integer representing upper bound of ensemble members, or 0 if not configured
	 */
	public int getMaximumMember() {
		return _maxMember; // Return stored configuration value
	}

	/**
	 * Persists all compute settings to an XML element, including max member count, member selection array,
	 * and model alternative specific configurations. Creates nested elements for each alternative's data locations.
	 *
	 * @param iterElem The parent Element object that will contain the serialized iteration settings data
	 */
	public void saveData(Element iterElem) {
		XMLUtilities.saveChildElement(iterElem, "maxMember", _maxMember); // Add max member as child element

		Element memberElem = new Element("MembersToCompute"); // Create container element for member array
		iterElem.addContent(memberElem); // Append members element to parent object

		XMLUtilities.createArrayElements(memberElem, _membersToCompute); // Populate with array values as repeated children

		saveModelAltSettings(iterElem); // Call protected method to serialize each model alternative configuration
	}

	/**
	 * Persists all stored model alternative settings to their corresponding nested XML elements.
	 * Creates entry elements for each alternative and delegates saveData() call on each ModelAltIterationSettings object.
	 *
	 * @param iterElem The parent Element that should contain the wrapped model alternative settings data
	 */
	protected void saveModelAltSettings(Element iterElem) {
		Element modelAltElem = new Element("ModelAlternativeSettings"); // Create container for all alternative entries
		iterElem.addContent(modelAltElem); // Append to parent object

		Set<Entry<String, ModelAltIterationSettings>> entrySet = _modelAltSettings.entrySet(); // Get all key-value pairs
		Iterator<Entry<String, ModelAltIterationSettings>> iter = entrySet.iterator(); // Create iterator for sequential access

		Entry<String, ModelAltIterationSettings> entry; // Declare loop variable for current pair

		// Continue until no more entries remain
		while (iter.hasNext()) {
			entry = iter.next(); // Get next entry from set

			Element entryElem = new Element(getSettingElementString()); // Create element using subclass-specific name
			modelAltElem.addContent(entryElem); // Append to parent container object
			XMLUtilities.addChildContent(entryElem, "ModelAlternative", entry.getKey()); // Add key string as child attribute

			ModelAltIterationSettings maSettings = entry.getValue(); // Retrieve settings object from map pair

			maSettings.saveData(entryElem); // Delegate save operation to subclass implementation
		}
	}

	/**
	 * Loads all persisted compute settings from an XML element into this instance, populating max member count,
	 * member array, and model alternative configuration maps. Uses reflection or getter methods on ModelAltIterationSettings
	 * to deserialize their properties as well.
	 *
	 * @param iterElem The Element containing serialized iteration settings data in the correct nested structure
	 */
	public void loadData(Element iterElem) {
		_maxMember = XMLUtilities.getChildElementAsInt(iterElem, "maxMember", _maxMember); // Extract max member from child element

		Element memberElem = iterElem.getChild("MembersToCompute"); // Get container element for array

		_membersToCompute = XMLUtilities.getIntArrayElements(memberElem); // Parse repeated child elements into integer array
		loadModelAltSettings(iterElem); // Call protected method to load alternative-specific configurations
	}


	/**
	 * Loads all stored model alternative settings from their corresponding nested XML elements.
	 * Creates new ModelAltIterationSettings objects for each alternative, populates their data locations,
	 * and stores them in the internal map using the key derived from program+name composite string.
	 *
	 * @param iterElem The parent Element containing wrapped model alternative settings children
	 */
	private void loadModelAltSettings(Element iterElem) {
		Element modelAltElem = iterElem.getChild("ModelAlternativeSettings"); // Get container element for all alternatives

		// Only proceed if container element exists in XML hierarchy
		if (modelAltElem != null) {
			_modelAltSettings.clear(); // Clear existing map before loading to avoid duplicates

			List kids = modelAltElem.getChildren(getSettingElementString()); // Get all children matching setting element name

			// Iterate through each alternative entry
			for (int i = 0; i < kids.size(); i++) {
				Element iterKid = (Element) kids.get(i); // Cast to Element and get current child

				ModelAltIterationSettings maSettings = new ModelAltIterationSettings(); // Create empty settings object

				maSettings.loadData(iterKid); // Delegate loading to subclass implementation

				String maKey = XMLUtilities.getChildElementAsString(iterKid, "ModelAlternative", null); // Extract key from child attribute

				// Only store if we successfully read the key identifier
				if (maKey != null) {
					_modelAltSettings.put(maKey, maSettings); // Insert into map using extracted key as index
				}
			}
		}
	}

	/**
	 * Returns the XML element name that should wrap model alternative settings data.
	 * Must be implemented by concrete subclasses to define their own persistence schema (e.g., "cequal" or "ressim").
	 */
	protected abstract String getSettingElementString(); // Abstract method defining custom wrapper name

	/**
	 * Returns the filename where computed ensemble output DSS records are stored for iterative or position analysis.
	 * Must be implemented by concrete subclasses to define their own storage schema (e.g., "iterationResults.dss").
	 */
	public abstract String getCollectionDssFilename(); // Abstract method defining custom collection filename
}
