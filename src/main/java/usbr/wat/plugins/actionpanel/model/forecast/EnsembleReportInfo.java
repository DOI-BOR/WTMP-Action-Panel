package usbr.wat.plugins.actionpanel.model.forecast;

// This file defines a data model class representing a subset of an ensemble configuration for reporting purposes.

/**
 * EnsembleReportInfo is a data transfer object holding references to both the parent ensemble set and a specific
 * subset of member indices to be included in a generated report. It encapsulates the scope of the forecast
 * information being reported on, linking which ensemble model alternatives (set) should be considered alongside
 * which specific members from within that set are part of this particular reporting instance.
 */
public class EnsembleReportInfo {

	// Field storing reference to the ensemble set data containing model alternatives and metadata associated with the forecast scenario
	private final EnsembleSet _eset;

	// Array of indices representing the specific members within the parent ensemble included for this specific report output
	private final int[] _memberSet;


	/**
	 * Constructs a new EnsembleReportInfo instance.
	 * Initializes references to the ensemble set and member indices passed as parameters.
	 *
	 * @param eset      The EnsembleSet object representing the collection of model alternatives associated with this data
	 * @param memberSet Array of integer indices specifying which specific members within the set to include in reporting
	 */
	public EnsembleReportInfo(EnsembleSet eset, int[] memberSet) {
		super(); // Call superclass constructor
		_eset = eset; // Assign ensemble set reference to private final field
		_memberSet = memberSet; // Assign member indices array to private final field
	}

	/**
	 * Returns the array of member indices included in this report information.
	 * These indices correspond to positions within the parent ensemble set defined by getEnsembleSet().
	 *
	 * @return int[] Array containing integer values representing the indices of members to be reported on
	 */
	public int[] getMembersToReportOn() {
		return _memberSet; // Return the stored member index array reference
	}

	/**
	 * Returns the parent ensemble set associated with this report information.
	 * This object provides access to all available alternatives and metadata for the forecast model.
	 *
	 * @return EnsembleSet The full EnsembleSet object containing the broader collection of model data
	 */
	public EnsembleSet getEnsembleSet() {
		return _eset; // Return the stored ensemble set reference
	}
}