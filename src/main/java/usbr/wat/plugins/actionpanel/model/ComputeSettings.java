package usbr.wat.plugins.actionpanel.model;

import java.util.HashMap; // Import HashMap for storing script configurations keyed by model alternative identifiers
import java.util.Iterator; // Import Iterator interface for traversing configuration map entries during serialization
import java.util.List; // Import List interface for working with collections of XML child elements
import java.util.Map; // Import Map interface for key-value storage of script settings by model alternative
import java.util.Map.Entry; // Import Entry class for accessing key-value pairs in the settings map
import java.util.Set; // Import Set interface for managing unique entries during serialization operations

import org.jdom.Element; // Import Element class for creating child XML elements during data persistence

import com.rma.util.XMLUtilities; // Import utility helper class for serializing Java objects to/from JDOM XML structures

import hec.lang.NamedType; // Import NamedType base class providing common object identification and persistence functionality

import hec2.plugin.model.ModelAlternative; // Import model alternative representing a specific simulation configuration


/**
 * ComputeSettings is a settings class that manages Python script configurations for pre-compute and post-compute phases
 * during iterative sensitivity analysis and ensemble generation workflows. It associates script files with each model
 * alternative and tracks whether scripts should be executed, enabling dynamic modification of inputs/outputs between
 * simulation runs or after compute operations.
 *
 * This class extends NamedType to support persistence and provides:<
 *
 *   Map-based storage for model alternative-specific script configurations<
 *   Methods to retrieve script files and execution flags per model alternative
 *   XML serialization methods for saving/loading configuration from disk
 *
 *
 */

@SuppressWarnings("serial") // Suppress warnings about unchecked serializable implementation in HashMap
public class ComputeSettings extends NamedType {
	// Map storing ScriptSettings keyed by model alternative composite key (Program-Name)
	private Map<String, ScriptSettings> _scriptSettings = new HashMap<>(); // Configuration map for scripts per model alternative

	public ComputeSettings() {
		super();
	}

	/**
	 * Retrieves the script file path associated with a specific model alternative.
	 * Uses the composite key (Program-Name) to look up configuration and returns the stored script path string.
	 * Returns null if no script is configured for this model alternative or if map lookup fails.
	 *
	 * @param modelAlt The ModelAlternative object from which to derive the configuration key
	 * @return String path to the script file for this model alternative, or null if not found
	 */
	public String getScriptFor(ModelAlternative modelAlt) {
		String key = getKey(modelAlt); // Generate composite key from model alternative properties
		ScriptSettings settings = _scriptSettings.get(key); // Look up settings entry by key

		// Only return if settings object exists in map
		if (settings != null) {
			return settings.script; // Return the stored script file path string
		}

		// Return null if no configuration found for this alternative
		return null;
	}


	/**
	 * Checks whether a Python script should be executed for a specific model alternative.
	 * Used to enable/disable scripts dynamically without needing to remove them from configuration.
	 * Returns false if no settings exist or execution flag is not set.
	 *
	 * @param modelAlt The ModelAlternative object from which to derive the configuration key
	 * @return True if script should be run for this model alternative, false otherwise
	 */
	public boolean shouldRunScriptFor(ModelAlternative modelAlt) {
		String key = getKey(modelAlt); // Generate composite key from model alternative properties
		ScriptSettings settings = _scriptSettings.get(key); // Look up settings entry by key

		// Only return flag value if settings object exists in map
		if (settings != null) {
			return settings.runScript; // Return the boolean execution flag value
		}

		return false; // Return default false for any model alternative without configuration
	}

	/**
	 * Generates a unique key string for a model alternative by combining its program type and name.
	 * Delegates to IterationSettings.getKey() for consistency with other settings classes.
	 * Used as the map key for storing and retrieving alternative-specific script configurations.
	 *
	 * @param modelAlt The ModelAlternative object from which to extract identifier components
	 * @return String key in format "Program-Name" (e.g., "CE-Qual-W2-Model1")
	 */
	// Static utility method creates composite key
	private static String getKey(ModelAlternative modelAlt) {
		return IterationSettings.getKey(modelAlt); // Delegate to shared utility method for key generation
	}


	/**
	 * Configures a script file path for a specific model alternative.
	 * Creates a new ScriptSettings entry if one doesn't exist, otherwise updates the existing settings object.
	 * Does not throw exception on null modelAlternative parameter (silently returns).
	 *
	 * @param modelAlt The ModelAlternative object to associate script with
	 * @param script   The path string to the Python script file to be executed during compute phase
	 */
	public void setScriptFor(ModelAlternative modelAlt, String script) {
		// Return early if model alternative reference is missing
		if (modelAlt == null) {
			return; // Cannot configure script for null object
		}

		String key = getKey(modelAlt); // Generate composite key from model alternative properties
		ScriptSettings settings = _scriptSettings.get(key); // Look up existing settings by key

		// If no settings entry exists, create new one
		if (settings == null) {
			settings = new ScriptSettings(); // Instantiate fresh settings object with default values
			_scriptSettings.put(key, settings); // Store in map using composite key as index
		}

		settings.script = script; // Assign script path to settings object
	}

	/**
	 * Configures whether a Python script should be executed for a specific model alternative.
	 * Creates a new ScriptSettings entry if one doesn't exist, otherwise updates the existing settings object.
	 * Does not throw exception on null modelAlternative parameter (silently returns).
	 *
	 * @param modelAlt  The ModelAlternative object to set execution flag for
	 * @param runScript Boolean value indicating whether script should be executed during compute phase
	 */
	public void setRunScriptFor(ModelAlternative modelAlt, boolean runScript) {
		// Return early if model alternative reference is missing
		if (modelAlt == null) {
			return; // Cannot configure settings for null object
		}

		String key = getKey(modelAlt); // Generate composite key from model alternative properties
		ScriptSettings settings = _scriptSettings.get(key); // Look up existing settings by key

		// If no settings entry exists, create new one
		if (settings == null) {
			settings = new ScriptSettings(); // Instantiate fresh settings object with default values
			_scriptSettings.put(key, settings); // Store in map using composite key as index
		}

		settings.runScript = runScript; // Assign boolean flag to settings object
	}

	/**
	 * Persists script configurations for all model alternatives to an XML element.
	 * Creates "ScriptSetting" child elements for each alternative, including ModelAlternative identifier,
	 * Script file path (or empty string if none configured), and RunScript boolean flag.
	 *
	 * @param preComputeElem The parent Element that will contain serialized script configuration data
	 */
	public void saveData(Element element) {
		Set<Entry<String, ScriptSettings>> entrySet = _scriptSettings.entrySet(); // Get all key-value pairs
		Iterator<Entry<String, ScriptSettings>> iter = entrySet.iterator(); // Create iterator for sequential access

		Entry<String, ScriptSettings> entry; // Declare loop variable for current pair
		String maInfo; // Variable to hold model alternative information string
		ScriptSettings scriptSetting; // Variable to hold settings object from map
		Element scriptElement; // Variable for newly created XML element

		// Continue until no more entries remain in set
		while (iter.hasNext()) {
			entry = iter.next(); // Get next entry from iterator
			maInfo = entry.getKey(); // Extract model alternative key string from pair
			scriptSetting = entry.getValue(); // Retrieve settings object from map pairing

			scriptElement = new Element("ScriptSetting"); // Create child element for this configuration
			element.addContent(scriptElement); // Append to parent container object

			XMLUtilities.addChildContent(scriptElement, "ModelAlternative", maInfo); // Add key string as child attribute
			scriptSetting.saveData(scriptElement); // Delegate save of script-specific properties to nested class
		}
	}

	/**
	 * Loads script configurations from an XML element into this instance, populating the settings map.
	 * Clears any existing configuration and rebuilds it by iterating through all "ScriptSetting" child elements.
	 * Only stores entries where ModelAlternative key was successfully extracted.
	 *
	 * @param element The Element containing serialized script configuration data in ScriptSetting format
	 */
	public void loadData(Element element) {
		_scriptSettings.clear(); // Clear existing map before loading to avoid duplicates

		String maInfo; // Variable to hold model alternative information string
		ScriptSettings scriptSetting; // Variable for newly loaded settings object
		Element scriptElement; // Variable for parsed XML element from file

		List kidElements = element.getChildren("ScriptSetting"); // Get all child elements with matching name

		// Iterate through each ScriptSetting element
		for (int i = 0; i < kidElements.size(); i++) {
			scriptElement = (Element) kidElements.get(i); // Cast to Element and get current child
			maInfo = XMLUtilities.getChildElementAsString(scriptElement, "ModelAlternative", null); // Extract key from child attribute

			scriptSetting = new ScriptSettings(); // Create empty settings object
			scriptSetting.loadData(scriptElement); // Delegate loading of script-specific properties to nested class

			// Only store if we successfully read the key identifier
			if (maInfo != null) {
				_scriptSettings.put(maInfo, scriptSetting); // Insert into map using extracted key as index
			}
		}
	}


	/**
	 * Inner static class holding configuration for a single model alternative's Python script.
	 * Stores script file path and execution flag with XML persistence methods implemented inline.
	 */
	class ScriptSettings {
		String script; // Field storing the full path string to the Python script file

		boolean runScript; // Boolean flag indicating whether this script should be executed during compute phase

		/**
		 * Saves script settings to an XML element, including Script file path and RunScript boolean flag.
		 * Uses conditional concatenation to handle null script values gracefully by using empty string.
		 */
		public void saveData(Element element) {
			XMLUtilities.addChildContent(element, "Script", script != null ? script : ""); // Add script property with null-safe handling
			XMLUtilities.addChildContent(element, "RunScript", runScript); // Add boolean flag as child attribute
		}

		/**
		 * Loads script settings from an XML element into the instance fields.
		 * Uses default values (empty string for script path, false for run flag) if elements are missing.
		 */
		public void loadData(Element element) {
			script = XMLUtilities.getChildElementAsString(element, "Script", ""); // Extract script path with empty string default
			runScript = XMLUtilities.getChildElementAsBoolean(element, "RunScript", false); // Extract boolean flag with false default
		}
	}
}
