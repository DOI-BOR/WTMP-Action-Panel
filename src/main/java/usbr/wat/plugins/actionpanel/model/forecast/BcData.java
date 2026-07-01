package usbr.wat.plugins.actionpanel.model.forecast;

import java.nio.file.Path; // Import Path interface for representing file system paths and location references
import java.nio.file.Paths; // Import Paths factory class for creating Path objects from string representations
import com.rma.util.XMLUtilities; // Import XML utility helper class for persisting Java objects to JDOM elements
import hec.lang.NamedType; // Import NamedType base class providing common object identification and persistence functionality
import org.jdom.Element; // Import Element class for creating child nodes in JDOM XML tree structure

/**
 * BcData is a model class representing boundary condition data for forecast scenarios.
 * It extends NamedType to support XML-based persistence of operations data, meteorology data,
 * output DSS file paths, and functional parts associated with boundary conditions.
 */

public class BcData extends NamedType {

	// Constant string identifier for the XML element storing the Output DSS File path
	private static final String OUTPUT_DSS_FILE_ELEM_ID = "Output-DSS-File";

	// Constant string identifier for the XML element storing the functional part name
	private static final String F_PART_ATTRIBUTE_ID = "f-part";

	// Field storing the display name of the selected operations data object
	private String _opsDataName = "";

	// Field storing the display name of the selected meteorology data object
	private String _metDataName = "";

	// Field storing the reference to the currently selected Operations Data object
	private OperationsData _opsData;

	// Field storing the reference to the currently selected Meteorologic Data object
	private MeteorlogicData _metData;

	// Path field for the output DSS file used in boundary condition override
	private Path _outputDssFile;

	// String field for the functional part identifier associated with this data
	private String _fPart;

	public BcData() {
		super();
	}

	/**
	 * Saves the current state of this object to a child XML element named "BcData".
	 * Creates nested elements for Output DSS File path and functional part, then adds
	 * Operations and Meteorology text children with their respective display names.
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("BcData"); // Create root element for this class
		parent.addContent(myElem); // Append BcData element as child of parent object

		XMLUtilities.saveNamedType(myElem, this); // Call base class save method to persist type information

		Element outputDssFileElem = new Element(OUTPUT_DSS_FILE_ELEM_ID); // Create element for storing DSS file path

		// Check if the output DSS file path has been set
		if (_outputDssFile != null) {
			outputDssFileElem.setText(_outputDssFile.toString()); // Set text content using absolute path string
		}

		myElem.addContent(outputDssFileElem); // Add DSS File element to parent BcData element

		Element fPartElement = new Element(F_PART_ATTRIBUTE_ID); // Create element for storing functional part identifier

		// Check if the functional part string has been set
		if (_fPart != null) {
			fPartElement.setText(_fPart); // Set text content with the functional part value
		}

		myElem.addContent(fPartElement); // Add Functional Part element to parent BcData element

		Element opsElem = new Element("Operations"); // Create nested element for Operations data information
		myElem.addContent(opsElem); // Append Operations container element to BcData element
		opsElem.setText(_opsDataName); // Set text content of Operations element with the name string

		Element metElem = new Element("Meteorology"); // Create nested element for Meteorology data information
		myElem.addContent(metElem); // Append Meteorology container element to BcData element
		metElem.setText(_metDataName); // Set text content of Meteorology element with the name string
	}

	/**
	 * Loads persistence data from an XML Element into this object.
	 * Extracts functional part, output DSS file path, and operations/meteorology names from child elements.
	 *
	 * @param myElem The XML Element containing serialized BcData properties
	 */
	public boolean loadData(Element myElem) {
		// Check if the provided XML element is null
		if (myElem == null) {
			return false; // Return failure for missing input element
		}

		XMLUtilities.loadNamedType(myElem, this); // Load type information into base class from root element

		Element fPartElem = myElem.getChild(F_PART_ATTRIBUTE_ID); // Find child element containing functional part ID

		// Check if the functional part element exists in the document
		if (fPartElem != null) {
			_fPart = fPartElem.getText(); // Extract text value and assign to instance field
		}

		Element outputDssFileElem = myElem.getChild(OUTPUT_DSS_FILE_ELEM_ID); // Find child element containing DSS file ID

		// Check if the Output DSS File element exists in the document
		if (outputDssFileElem != null) {
			String filePath = outputDssFileElem.getText(); // Extract text value from the path element

			// Check if the extracted path string is not empty
			if (filePath != null) {
				_outputDssFile = Paths.get(filePath); // Convert file path string to Path object instance
			}
		}

		_opsDataName = XMLUtilities.getChildElementAsString(myElem, "Operations", ""); // Extract text value from nested Operations element or default
		_metDataName = XMLUtilities.getChildElementAsString(myElem, "Meteorology", ""); // Extract text value from nested Meteorology element or default

		return true; // Indicate successful loading of data
	}

	/**
	 * Sets the currently selected operations data object and updates its name display.
	 * Clears the previous operations object reference and resets name string to empty before setting new values.
	 *
	 * @param opsData The OperationsData object representing the selected operational configuration
	 */
	public void setSelectedOps(OperationsData opsData) {
		_opsDataName = ""; // Reset name string to empty state before assigning new reference
		_opsData = opsData; // Update internal reference with the provided object

		// Check if the provided operations data object exists
		if (opsData != null) {
			_opsDataName = opsData.getName(); // Extract display name from the object and store in instance field
		}
	}

	/**
	 * Sets the currently selected meteorological data object and updates its name display.
	 * Clears the previous meteorology object reference and resets name string to empty before setting new values.
	 *
	 * @param metData The MeteorologicData object representing the selected meteorological configuration
	 */
	public void setSelectedMet(MeteorlogicData metData) {
		_metDataName = ""; // Reset name string to empty state before assigning new reference
		_metData = metData; // Update internal reference with the provided object

		// Check if the provided meteorology data object exists
		if (metData != null) {
			_metDataName = metData.getName(); // Extract display name from the object and store in instance field
		}
	}

	/**
	 * Returns the display name string for the selected operations data.
	 *
	 * @return String containing the name of the operations data, or empty string if not set
	 */
	public String getOpsDataName() {
		return _opsDataName; // Return the stored name string from instance variable
	}

	/**
	 * Returns the display name string for the selected meteorological data.
	 *
	 * @return String containing the name of the meteorology data, or empty string if not set
	 */
	public String getMetDataName() {
		return _metDataName; // Return the stored name string from instance variable
	}

	/**
	 * Returns the OperationsData object associated with this boundary condition.
	 *
	 * @return The OperationsData object currently selected, or null if not set
	 */
	public OperationsData getOperationsData() {
		return _opsData; // Return stored reference to the operations data object
	}

	/**
	 * Returns the MeteorlogicData object associated with this boundary condition.
	 * Note: Field name typo "Meteorogical" is preserved from original code.
	 *
	 * @return The MeteorologicData object currently selected, or null if not set
	 */
	public MeteorlogicData getMeteorogicalData() {
		return _metData; // Return stored reference to the meteorology data object
	}

	/**
	 * Sets the output DSS file path for boundary condition computations.
	 *
	 * @param outputDssFile The Path object representing the location of the output DSS file
	 */
	public void setOutputDssFile(Path outputDssFile) {
		_outputDssFile = outputDssFile; // Assign path reference to instance variable
	}

	/**
	 * Returns the Path object representing the location of the output DSS file.
	 *
	 * @return The current Path instance storing the output DSS file location, or null if not set
	 */
	public Path getOutputDssFile() {
		return _outputDssFile; // Return stored reference to the output DSS file path
	}

	/**
	 * Sets the functional part string used to identify this boundary condition in data directories.
	 * Functional parts allow multiple configurations to coexist within a single model directory structure.
	 *
	 * @param bcFPart The string identifier for this boundary condition configuration
	 */
	public void setFPart(String bcFPart) {
		_fPart = bcFPart; // Assign provided FPart string to instance variable
	}

	/**
	 * Returns the functional part string used to identify this boundary condition.
	 *
	 * @return The current FPart string, or null if not set
	 */
	public String getFPart() {
		return _fPart; // Return stored reference to the functional part string
	}
}