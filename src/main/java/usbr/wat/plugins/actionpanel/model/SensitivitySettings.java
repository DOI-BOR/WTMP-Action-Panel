package usbr.wat.plugins.actionpanel.model;

import org.jdom.Element; // JDOM XML Element used for serializing and deserializing pre/post-compute settings

import hec.lang.NamedType; // HEC base class providing a name field; inherited but not actively used here

/**
 * Holds the pre-compute and post-compute script settings for an iteration
 * configuration within the WTMP Action Panel.
 *
 * Each field is a ComputeSettings object that maps model alternatives to Python
 * script file paths and the "run script" flag. These are serialized as
 * "PreCompute" and "PostCompute" child elements within an enclosing
 * "SensitivitySettings" XML element.
 *
 * Instances are held by IterationSettings and passed to the SensitivityPanel UI
 * for user editing.
 *
 * This class is suppressed for serialization warnings because NamedType is not
 * consistently serializable.
 *
 */
@SuppressWarnings("serial")
public class SensitivitySettings extends NamedType {
	// Script-path and run-flag assignments for each model alternative before the compute run
	private ComputeSettings _preComputeSettings = new ComputeSettings();

	// Script-path and run-flag assignments for each model alternative after the compute run
	private ComputeSettings _postComputeSettings = new ComputeSettings();

	/**
	 * Constructs a SensitivitySettings with empty pre- and post-compute settings.
	 */
	public SensitivitySettings() {
		super();
	}

	/**
	 * Returns the pre-compute script settings (scripts run before each iteration compute).
	 *
	 * @return the ComputeSettings holding pre-compute script paths and flags
	 */
	public ComputeSettings getPreComputeSettings() {
		return _preComputeSettings;
	}

	/**
	 * Returns the post-compute script settings (scripts run after each iteration compute).
	 *
	 * @return the ComputeSettings holding post-compute script paths and flags
	 */
	public ComputeSettings getPostComputeSettings() {
		return _postComputeSettings;
	}

	/**
	 * Serializes the pre- and post-compute settings to XML under the given element.
	 *
	 * Adds "PreCompute" and "PostCompute" child elements, each containing the
	 * serialized ComputeSettings for the corresponding phase.
	 *
	 * @param sensitivityElem the XML Element to serialize into
	 */
	public void saveData(Element sensitivityElem) {
		// Create and populate the PreCompute child element
		Element preComputeElem = new Element("PreCompute");
		sensitivityElem.addContent(preComputeElem);
		_preComputeSettings.saveData(preComputeElem);

		// Create and populate the PostCompute child element
		Element postComputeElem = new Element("PostCompute");
		sensitivityElem.addContent(postComputeElem);
		_postComputeSettings.saveData(postComputeElem);
	}

	/**
	 * Restores the pre- and post-compute settings from XML under the given element.
	 *
	 * Reads the "PreCompute" and "PostCompute" child elements if present; has no
	 * effect for whichever child elements are absent.
	 *
	 * @param sensitivityElem the XML Element containing the settings to load
	 */
	public void loadData(Element sensitivityElem) {
		// Load the pre-compute settings if the child element is present
		Element preComputeElem = sensitivityElem.getChild("PreCompute");
		if (preComputeElem != null) {
			_preComputeSettings.loadData(preComputeElem);
		}

		// Load the post-compute settings if the child element is present
		Element postComputeElem = sensitivityElem.getChild("PostCompute");
		if (postComputeElem != null) {
			_postComputeSettings.loadData(postComputeElem);
		}
	}
}
