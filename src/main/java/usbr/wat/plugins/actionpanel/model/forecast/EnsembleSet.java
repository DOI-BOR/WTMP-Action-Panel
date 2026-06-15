package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.List; // Import List interface (included in original imports for potential future use)

import com.rma.util.XMLUtilities; // Import utility helper class for serializing Java objects to/from JDOM XML elements

import hec.lang.NamedType; // Import NamedType base class providing common object identification and persistence functionality

import org.jdom.Element; // Import Element class for creating child nodes in JDOM XML tree structure

import rma.util.IntVector; // Import IntVector utility class for managing vector collections of integer member indices

/**
 * EnsembleSet is a data model class representing the collection structure used for forecast ensemble configurations.
 * It manages boundary condition data, temperature target sets, and tracks which members have been computed in an
 * iterative or position analysis workflow. Extends NamedType to support XML persistence with child elements for BC
 * Data, Temperature Target Sets, and Computed Members list.
 */

public class EnsembleSet extends NamedType {

	// Reference to boundary condition data object associated with this ensemble configuration
	private BcData _bcData; // Stores the BcData object used in position analysis or forecast scenarios

	// Reference to temperature target set data for model alternative configurations
	private TemperatureTargetSet _tempTargetSet; // Stores TemperatureTargetSet object for specifying temperature overrides

	// Field storing display name string of selected boundary condition data object for UI reporting
	private String _bcDataName; // Contains human-readable name from BcData.getName() or empty if none

	// Field storing display name string of selected temperature target set object for UI reporting
	private String _tempTargetSetName; // Contains human-readable name from TemperatureTargetSet.getName() or empty if none

	// Vector collection tracking which ensemble members have been successfully computed during sensitivity analysis
	private IntVector _computedMembers = new IntVector(); // Stores indices of processed ensemble members

	// String field storing the configuration property identifying which member set to process next
	private String _membersToCompute; // Configuration string for controlling compute sequence

	public EnsembleSet() {
		super(); // Invoke superclass default constructor
	}

	/**
	 * Associates a boundary condition data object with this ensemble configuration.
	 * Extracts and caches the display name from the BcData object for UI reporting.
	 */
	public void setSelectedBcData(BcData bc) {
		_bcData = bc; // Assign boundary condition data reference to private field

		// Check if provided BC data object is not missing
		if (_bcData != null) {
			_bcDataName = _bcData.getName(); // Extract and store name string for display

		} else {
			// Handle case where no BC data was provided
			_bcDataName = ""; // Reset name field to empty string default
		}
	}

	/**
	 * Associates a temperature target set object with this ensemble configuration.
	 * Extracts and caches the display name from the TemperatureTargetSet object for UI reporting.
	 */
	public void setSelectedTemperatureTargetSets(TemperatureTargetSet tts) {
		_tempTargetSet = tts; // Assign temperature target set reference to private field

		// Check if provided temp target set object is not missing
		if (_tempTargetSet != null) {
			_tempTargetSetName = _tempTargetSet.getName(); // Extract and store name string for display

		} else {
			// Handle case where no temperature target data was provided
			_tempTargetSetName = ""; // Reset name field to empty string default
		}
	}

	/**
	 * Persists all ensemble configuration properties to a child XML element named "EnsembleSet".
	 * Creates nested elements for BcData and TemperatureTargetSet with their display names, then serializes
	 * the computed members list as repeated integer array elements within ComputedMembers container.
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("EnsembleSet"); // Create root element for this class
		parent.addContent(myElem); // Append EnsembleSet element as child of parent object
		XMLUtilities.saveNamedType(myElem, this); // Call base class save method to persist type information

		Element bcElem = new Element("BcData"); // Create nested container element for BC data information
		myElem.addContent(bcElem); // Append BC Data element to parent EnsembleSet object
		bcElem.setText(_bcDataName); // Set text content of BcData element with name string or empty if none

		Element ttsElem = new Element("TemperatureTargetSet"); // Create nested container element for temperature target set information
		myElem.addContent(ttsElem); // Append Temperature Target Set element to parent EnsembleSet object
		ttsElem.setText(_tempTargetSetName); // Set text content of TTS element with name string or empty if none

		// Only add ComputedMembers element if collection is initialized
		if (_computedMembers != null) {
			Element computedMembersElem = new Element("ComputedMembers"); // Create nested container for member indices
			myElem.addContent(computedMembersElem); // Append to parent container object
			int[] members = _computedMembers.toArray(); // Convert IntVector to primitive array for XML serialization
			XMLUtilities.createArrayElements(computedMembersElem, members); // Populate with integer values as repeated children
		}

		// Only add MembersToCompute if configuration string is present
		if (_membersToCompute != null) {
			XMLUtilities.addChildContent(myElem, "MembersToCompute", _membersToCompute); // Add as child element to root
		}
	}

	/**
	 * Loads persistence data from an XML Element into this object.
	 * Extracts display names for BC Data and TemperatureTargetSet, then parses ComputedMembers array
	 * back into IntVector collection and reads MembersToCompute configuration string.
	 */
	public boolean loadData(Element myElem) {
		// Check if the provided XML element is missing entirely
		if (myElem == null) {
			return false; // Return failure for missing input element
		}

		_computedMembers.clear(); // Clear existing collection before loading new data to avoid duplication

		XMLUtilities.loadNamedType(myElem, this); // Load type information into base class from root element
		_bcDataName = XMLUtilities.getChildElementAsString(myElem, "BcData", ""); // Extract text value from nested BcData element or default
		_tempTargetSetName = XMLUtilities.getChildElementAsString(myElem, "TemperatureTargetSet", ""); // Extract text value from nested TTS element or default

		Element computedMembersElem = myElem.getChild("ComputedMembers"); // Find child container for member indices

		// Only proceed if ComputedMembers element exists in XML hierarchy
		if (computedMembersElem != null) {
			int[] computedMembers = XMLUtilities.getIntArrayElements(computedMembersElem); // Parse repeated integer values into array
			_computedMembers.addAll(computedMembers); // Add all parsed indices back into vector collection
		}

		_membersToCompute = XMLUtilities.getChildElementAsString(myElem, "MembersToCompute", ""); // Extract configuration string from child or default

		return true; // Indicate successful loading of all persistence data
	}

	// Getter method retrieves stored boundary condition data reference
	public BcData getBcData() {
		return _bcData; // Return currently assigned BC Data object or null if not set
	}

	// Getter method retrieves stored temperature target set reference
	public TemperatureTargetSet getTemperatureTargetSet() {
		return _tempTargetSet; // Return currently assigned TTS object or null if not set
	}

	// Getter method retrieves vector collection of computed indices
	public IntVector getComputedMembers() {
		return _computedMembers; // Return IntVector containing successfully computed member indices
	}

	// Getter method retrieves display name string for BC data
	public String getBcDataName() {
		return _bcDataName; // Return stored name value or empty string default
	}

	// Getter method retrieves display name string for temp target set
	public String getTemperatureTargetSetName() {
		return _tempTargetSetName; // Return stored name value or empty string default
	}

	/**
	 * Sets the configuration string specifying which member indices should be processed next.
	 * Used during sequential compute operations to control iteration order.
	 */
	public void setMemberSetToCompute(String members) {
		_membersToCompute = members; // Assign parameter value to instance variable
	}

	/**
	 * Retrieves the current member configuration string specifying which ensemble set to compute.
	 *
	 * @return String containing the comma-separated list of member indices or empty if not configured
	 */
	public String getMemberSetToCompute() {
		return _membersToCompute; // Return stored configuration string value
	}

	/**
	 * Adds a computed member to the collection and marks this object as modified.
	 * Checks membership first to prevent adding duplicates before marking modification state.
	 */
	public void addComputedMember(int member) {
		// Check if member is not already in computed collection
		if (!_computedMembers.contains(member)) {
			_computedMembers.add(member); // Add new member index to vector if not present
			setModified(true); // Mark object as modified to trigger XML persistence on next save operation
		}
	}

}