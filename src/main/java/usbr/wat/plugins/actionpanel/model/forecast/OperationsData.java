package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.util.XMLUtilities; // RMA XML utility for reading and writing named-type data and child elements
import hec.lang.NamedType;        // HEC base class providing a name field and modification tracking
import org.jdom.Element;          // JDOM XML Element used for serializing and deserializing this object

/**
 * Named data object representing an operations scenario dataset within a
 * ForecastSimGroup boundary condition setup.
 *
 * Each OperationsData entry identifies a specific operations file (typically a
 * HEC-ResSim or similar model operations configuration) to be used as the
 * operational forcing for one or more ensemble forecast compute runs.
 *
 * It carries:
 *   - A display name (inherited from NamedType) identifying this scenario in the UI.
 *   - A path to the operations file (_opsFile) that defines the reservoir operating
 *     rules or flow schedules for this scenario.
 *
 * Instances are loaded by ForecastSimGroup from the "Operations" XML element and
 * resolved by name in BcData when configuring boundary conditions for an ensemble
 * set. They are persisted as "OperationsData" child elements.
 */
public class OperationsData extends NamedType {
	// Path to the operations configuration file for this scenario
	private String _opsFile;

	/**
	 * Constructs an empty OperationsData with no name and no operations file path.
	 */
	public OperationsData() {
		super();
	}

	/**
	 * Restores this OperationsData from the given XML element.
	 *
	 * Reads the NamedType name via XMLUtilities.loadNamedType and the "OperationsFile"
	 * child element text as the operations file path. Defaults the path to an empty
	 * string if the element is absent. Returns false immediately if myElem is null.
	 *
	 * @param myElem the XML Element to read from; returns false if null
	 * @return true if loading succeeded; false if myElem is null
	 */
	public boolean loadData(Element myElem) {
		if (myElem == null) {
			return false;
		}

		// Restore the display name via the NamedType XML utility
		XMLUtilities.loadNamedType(myElem, this);

		// Read the operations file path; defaults to empty string if the element is absent
		_opsFile = XMLUtilities.getChildElementAsString(myElem, "OperationsFile", "");

		return true;
	}

	/**
	 * Serializes this OperationsData to XML under the given parent element.
	 *
	 * Creates an "OperationsData" child element containing the NamedType name
	 * and an "OperationsFile" child element with the operations file path as text.
	 *
	 * @param parent the XML Element to add the "OperationsData" child element to
	 */
	public void saveData(Element parent) {
		// Create the container element and add it to the parent
		Element myElem = new Element("OperationsData");
		parent.addContent(myElem);

		// Persist the display name via the NamedType XML utility
		XMLUtilities.saveNamedType(myElem, this);

		// Persist the operations file path as a child text element
		Element opsFileElem = new Element("OperationsFile");
		opsFileElem.setText(_opsFile);
		myElem.addContent(opsFileElem);
	}

	/**
	 * Sets the path to the operations configuration file for this scenario.
	 *
	 * @param opsFile the relative or absolute path to the operations file
	 */
	public void setOperationsFile(String opsFile) {
		_opsFile = opsFile;
	}

	/**
	 * Returns the path to the operations configuration file for this scenario.
	 *
	 * @return the operations file path string, or null if not set
	 */
	public String getOperationsFile() {
		return _opsFile;
	}
}
