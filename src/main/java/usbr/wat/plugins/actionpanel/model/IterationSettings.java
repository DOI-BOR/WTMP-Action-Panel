package usbr.wat.plugins.actionpanel.model;

import org.jdom.Element; // JDOM XML Element used for serializing and deserializing settings to/from XML

/**
 * Iteration compute settings for a single WAT simulation within the WTMP Action Panel.
 *
 * Extends BaseComputeSettings to inherit the compute-member indices, maximum member
 * value, and per-model-alternative BC DSS assignments. Adds a SensitivitySettings
 * field that carries the pre- and post-compute script configurations used during
 * the iterative compute run.
 *
 * Serialized to XML via saveData() and restored via loadData(). The XML element name
 * for this settings type is "IterationSettings". Computed results are collected in the
 * DSS file named "iterationResults.dss" in the simulation output folder.
 *
 */
public class IterationSettings extends BaseComputeSettings {
	// Pre/post-compute script settings for this iteration configuration; initialized to defaults
	private SensitivitySettings _sensitivitySettings = new SensitivitySettings();

	/**
	 * Constructs an IterationSettings with default values.
	 *
	 * Delegates to the BaseComputeSettings superclass constructor.
	 */
	public IterationSettings() {
		super();
	}

	/**
	 * Returns the sensitivity (pre/post-compute script) settings for this iteration configuration.
	 *
	 * @return the SensitivitySettings instance associated with this iteration
	 */
	public SensitivitySettings getSensitivitySettings() {
		return _sensitivitySettings;
	}

	/**
	 * Serializes this IterationSettings to the given XML element.
	 *
	 * Delegates to the superclass to save the shared compute fields, then appends
	 * the sensitivity settings as a child "SensitivitySettings" XML element.
	 *
	 * @param iterElem the XML Element into which this settings object should be saved
	 */
	@Override
	public void saveData(Element iterElem) {
		// Save base compute fields (member indices, maximum, per-alt DSS assignments)
		super.saveData(iterElem);

		// Append the pre/post-compute script settings as a child element
		saveSensitivitySettings(iterElem);
	}

	/**
	 * Creates a "SensitivitySettings" child element under the given parent and serializes
	 * the sensitivity settings into it.
	 *
	 * @param iterElem the parent XML Element to append the SensitivitySettings child to
	 */
	private void saveSensitivitySettings(Element iterElem) {
		// Create the sensitivity settings child element and populate it
		Element sensitivityElem = new Element("SensitivitySettings");
		iterElem.addContent(sensitivityElem);
		_sensitivitySettings.saveData(sensitivityElem);
	}

	/**
	 * Restores this IterationSettings from the given XML element.
	 *
	 * Delegates to the superclass to load the shared compute fields, then reads the
	 * sensitivity settings from the "SensitivitySettings" child element if present.
	 *
	 * @param iterElem the XML Element from which this settings object should be loaded
	 */
	@Override
	public void loadData(Element iterElem) {
		// Load base compute fields (member indices, maximum, per-alt DSS assignments)
		super.loadData(iterElem);

		// Load the pre/post-compute script settings from the child element
		loadSensitivitySettings(iterElem);
	}

	/**
	 * Reads the "SensitivitySettings" child element from the given parent and populates
	 * the sensitivity settings object. Has no effect if the child element is absent.
	 *
	 * @param iterElem the parent XML Element that may contain a "SensitivitySettings" child
	 */
	private void loadSensitivitySettings(Element iterElem) {
		Element sensitivityElem = iterElem.getChild("SensitivitySettings");
		if (sensitivityElem != null) {
			_sensitivitySettings.loadData(sensitivityElem);
		}
	}

	/**
	 * Returns the XML element tag name used to identify this settings type during serialization.
	 *
	 * @return "IterationSettings"
	 */
	@Override
	protected String getSettingElementString() {
		return "IterationSettings";
	}

	/**
	 * Returns the file name of the DSS file where iterative compute results are collected.
	 *
	 * @return "iterationResults.dss"
	 */
	@Override
	public String getCollectionDssFilename() {
		return "iterationResults.dss";
	}
}
