package usbr.wat.plugins.actionpanel.model.planning;

/**
 * The three mutually-exclusive ways a Planning workflow temperature target can be
 * defined, selected by the user on the Temperature Targets tab.
 */
public enum PlanningTempTargetMode {

	/** A single fixed temperature value applied across all years of the simulation. */
	FIXED_VALUE("Fixed Value"), // Enum constant for the "one value for every year" mode

	/** A single time series, the same length as the simulation period. */
	TIMESERIES("Timeseries"), // Enum constant for the "imported timeseries" mode

	/** A Jython (.py) file containing custom target logic evaluated during simulation. */
	JYTHON_SCRIPT("Jython Script"); // Enum constant for the "custom script logic" mode

	// Human-readable label shown in the mode selector UI
	private final String _displayName;

	// Enum constructor; runs once per constant declared above
	PlanningTempTargetMode(String displayName) {
		_displayName = displayName; // Store the label passed in by the constant declaration
	}

	/**
	 * Returns the human-readable label for this mode, suitable for use in radio buttons
	 * or combo box entries.
	 *
	 * @return the display label for this mode
	 */
	public String getDisplayName() {
		return _displayName; // Simple accessor, no transformation needed
	}

	// Overridden so this mode reads sensibly wherever Swing calls toString() (e.g. combo boxes)
	@Override
	public String toString() {
		return _displayName; // Reuse the same label used by getDisplayName()
	}
}
