package usbr.wat.plugins.actionpanel.model.forecast;

/**
 * Enumeration of the supported meteorological data source types used by the
 * WTMP Action Panel forecast compute subsystem.
 *
 * Each constant represents a distinct meteorological dataset category that can
 * be associated with a MeteorlogicData entry in a ForecastSimGroup:
 *
 *   Historic: observed historical meteorological records.
 *   L3MTO:    Level-3 Model Training Output; bias-corrected model-derived data.
 *   NCAR:     National Center for Atmospheric Research reanalysis or forecast data.
 *
 * The toString() method returns the human-readable type name string, which is
 * used when displaying the type in the forecast configuration UI.
 */
public enum MetDataType {
	// Observed historical meteorological records
	Historic("Historic"),

	// Level-3 Model Training Output; bias-corrected, model-derived meteorological data
	L3MTO("L3MTO"),

	// National Center for Atmospheric Research reanalysis or forecast dataset
	NCAR("NCAR");

	// The human-readable name string shown in the UI and used for serialization
	private String _typeName;

	/**
	 * Constructs a MetDataType enum constant with the given display name.
	 *
	 * @param typeName the human-readable type name string
	 */
	private MetDataType(String typeName) {
		_typeName = typeName;
	}

	/**
	 * Returns the human-readable type name string for this meteorological data type.
	 *
	 * @return the type name string (e.g., "Historic", "L3MTO", or "NCAR")
	 */
	public String toString() {
		return _typeName;
	}
}
