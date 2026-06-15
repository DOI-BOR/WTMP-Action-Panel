package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList; // Imported but not directly referenced; available for subclass or future use
import java.util.List;      // Imported but not directly referenced; available for subclass or future use

import com.rma.util.XMLUtilities; // RMA XML utility for reading and writing named-type data and child elements

import hec.lang.NamedType; // HEC base class providing a name field and modification tracking

import org.jdom.Element; // JDOM XML Element used for serializing and deserializing this object

import rma.util.RMAIO; // RMA I/O utility (imported for potential use by callers or subclasses)

/**
 * Named data object representing a single meteorological dataset configuration
 * within a ForecastSimGroup boundary condition setup.
 *
 * Each MeteorlogicData entry identifies a specific meteorological data source for
 * use in an ensemble forecast compute run. It carries:
 *
 *   - A display name (inherited from NamedType) identifying this dataset in the UI.
 *   - A MetDataType classifying the data source category (Historic, L3MTO, NCAR).
 *   - A calendar year identifying which year's data should be used (e.g., a specific
 *     historical year to use as the meteorological forcing for one ensemble member).
 *   - An optional path to the met config file listing the DSS records for this dataset.
 *     When the config file is null, the default historical met config file from
 *     ForecastConfigFiles is returned for backwards compatibility with older save files.
 *
 * Instances are loaded by ForecastSimGroup from the "Meteorology" XML element and
 * resolved by name when configuring BcData boundary conditions. They are persisted
 * as "MeteorologyData" child elements.
 */
public class MeteorlogicData extends NamedType {
	// The calendar year this met dataset represents; used to select the correct annual record
	private int _year;

	// The data source category for this met dataset; defaults to Historic
	private MetDataType _metDataType = MetDataType.Historic;

	// Optional path to the met config file listing the DSS records for this dataset;
	// null indicates the default historical met config file should be used
	private String _metConfigFile;

	/**
	 * Constructs an empty MeteorlogicData with no name, year 0, Historic type,
	 * and no config file (defaults to the historical met config on read).
	 */
	public MeteorlogicData() {
		super();
	}

	/**
	 * Sets the meteorological data source type for this entry.
	 *
	 * @param datatype the MetDataType categorizing this dataset (Historic, L3MTO, or NCAR)
	 */
	public void setMetDataType(MetDataType datatype) {
		_metDataType = datatype;
	}

	/**
	 * Returns the meteorological data source type for this entry.
	 *
	 * @return the MetDataType; defaults to MetDataType.Historic if never set
	 */
	public MetDataType getMetDataType() {
		return _metDataType;
	}

	/**
	 * Sets the calendar year this meteorological dataset represents.
	 *
	 * @param year the four-digit calendar year (e.g., 1985)
	 */
	public void setYear(int year) {
		_year = year;
	}

	/**
	 * Returns the calendar year this meteorological dataset represents.
	 *
	 * @return the four-digit calendar year; 0 if not set
	 */
	public int getYear() {
		return _year;
	}

	/**
	 * Sets the path to the met config file listing the DSS records for this dataset.
	 *
	 * @param metConfigFile the relative or absolute path to the met config file;
	 *                      null to use the default historical met config file
	 */
	public void setMetConfigFile(String metConfigFile) {
		_metConfigFile = metConfigFile;
	}

	/**
	 * Returns the path to the met config file for this dataset.
	 *
	 * If no config file has been explicitly set (_metConfigFile is null), returns the
	 * default historical met config file path from ForecastConfigFiles for backwards
	 * compatibility with save files that predate the configurable met file feature.
	 *
	 * @return the met config file path; never null
	 */
	public String getMetConfigFile() {
		if (_metConfigFile == null) {
			// Fall back to the default historical met config for backwards compatibility
			return ForecastConfigFiles.getRelativeHistoricalMetFile();
		}
		return _metConfigFile;
	}

	/**
	 * Serializes this MeteorlogicData to XML under the given parent element.
	 *
	 * Creates a "MeteorologyData" child element containing:
	 * - The NamedType name via XMLUtilities.saveNamedType.
	 * - A "MetDataType" element with the enum constant name (e.g., "Historic").
	 * - A "Year" element with the four-digit calendar year as a string.
	 * - An optional "MetConfigFile" element with the config file path, included
	 * only when _metConfigFile is non-null.
	 *
	 * Note: the MetConfigFile element is added before the Year element in the XML.
	 *
	 * @param parentElem the XML Element to add the "MeteorologyData" child to
	 */
	public void saveData(Element parentElem) {
		// Create the container element and add it to the parent
		Element myElem = new Element("MeteorologyData");
		parentElem.addContent(myElem);

		// Persist the display name via the NamedType XML utility
		XMLUtilities.saveNamedType(myElem, this);

		// Persist the data type using the enum constant name (used for valueOf() on load)
		Element dataTypeElem = new Element("MetDataType");
		dataTypeElem.setText(_metDataType.name());
		myElem.addContent(dataTypeElem);

		// Prepare the Year element (added after the optional config file element)
		Element yearElem = new Element("Year");
		yearElem.setText(String.valueOf(_year));

		// Only persist the config file path when one has been explicitly set
		if (_metConfigFile != null) {
			Element configFileElem = new Element("MetConfigFile");
			configFileElem.setText(getMetConfigFile());
			myElem.addContent(configFileElem);
		}

		// Add the Year element last
		myElem.addContent(yearElem);
	}

	/**
	 * Restores this MeteorlogicData from the given XML element.
	 *
	 * Reads:
	 * - The NamedType name via XMLUtilities.loadNamedType.
	 * - The "MetDataType" element text, parsed via MetDataType.valueOf(); the field
	 * retains its default (Historic) if the element is absent.
	 * - The "Year" element content as an integer; defaults to 0 if absent.
	 * - The "MetConfigFile" element text; null if the element is absent (triggers
	 * the default-config-file fallback in getMetConfigFile()).
	 *
	 * @param myElem the XML Element to read from
	 * @return true always (reserved for future error-reporting use)
	 */
	public boolean loadData(Element myElem) {
		// Restore the display name via the NamedType XML utility
		XMLUtilities.loadNamedType(myElem, this);

		// Read and parse the data type enum constant name; retain the default if absent
		Element dataTypeElem = myElem.getChild("MetDataType");
		if (dataTypeElem != null) {
			String dataTypeStr = dataTypeElem.getTextTrim();
			_metDataType = MetDataType.valueOf(dataTypeStr);
		}

		// Read the calendar year; defaults to 0 if the element is absent or empty
		Element yearElem = myElem.getChild("Year");
		_year = XMLUtilities.getContentAsInt(yearElem, 0);

		// Read the optional met config file path; null means use the default on get
		_metConfigFile = XMLUtilities.getChildElementAsString(myElem, "MetConfigFile", null);

		return true;
	}
}
