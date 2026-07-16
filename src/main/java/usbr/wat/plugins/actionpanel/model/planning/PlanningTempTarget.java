package usbr.wat.plugins.actionpanel.model.planning;

import org.jdom.Element;                        // JDOM XML element type used for saving and loading this object

import com.rma.util.XMLUtilities;               // Utility helper for serializing NamedType fields and simple child content to/from JDOM

import hec.lang.NamedType;                      // Base class supplying a display name and integer index, and standard modified-state tracking

/**
 * A named temperature target definition for the Planning workflow's Temperature Targets
 * tab. Unlike the Forecast workflow's temperature target sets, a Planning temperature
 * target is defined in exactly one of three mutually-exclusive modes (see
 * {@link PlanningTempTargetMode}), because targets are expected to vary significantly
 * between Sets:
 * <ul>
 *   <li>{@link PlanningTempTargetMode#FIXED_VALUE} — a single value applied across all
 *       years; see {@link #getFixedValue()}.</li>
 *   <li>{@link PlanningTempTargetMode#TIMESERIES} — a single imported time series the same
 *       length as the simulation period; see {@link #getTimeseriesFilePath()}.</li>
 *   <li>{@link PlanningTempTargetMode#JYTHON_SCRIPT} — a Jython (.py) file containing
 *       custom target logic, run via
 *       {@code usbr.wat.plugins.actionpanel.ui.forecast.PythonScriptUtil}; see
 *       {@link #getJythonScriptPath()}.</li>
 * </ul>
 * Only the fields relevant to the currently selected {@link #getMode()} are expected to
 * be populated; the others are retained but ignored.
 */
public class PlanningTempTarget extends NamedType {

	// Which of the three input modes is currently active for this target
	private PlanningTempTargetMode _mode = PlanningTempTargetMode.FIXED_VALUE;

	// Value used when _mode == FIXED_VALUE, applied uniformly across all simulation years
	private double _fixedValue = 0.0;

	// File path used when _mode == TIMESERIES; expected to span the full simulation period
	private String _timeseriesFilePath = "";

	// File path used when _mode == JYTHON_SCRIPT, pointing directly at the .py logic file
	private String _jythonScriptPath = "";

	/**
	 * Constructs a temperature target defaulted to fixed-value mode with a value of zero.
	 */
	public PlanningTempTarget() {
		super(); // Invoke NamedType's default constructor to initialize name/index bookkeeping
	}

	/**
	 * Returns which of the three mutually-exclusive input modes is active.
	 *
	 * @return the active mode
	 */
	public PlanningTempTargetMode getMode() {
		return _mode; // Simple accessor
	}

	/**
	 * Sets the active input mode and marks this target modified.
	 *
	 * @param mode the new mode; null is treated as {@link PlanningTempTargetMode#FIXED_VALUE}
	 */
	public void setMode(PlanningTempTargetMode mode) {
		_mode = mode == null ? PlanningTempTargetMode.FIXED_VALUE : mode; // Fall back to a safe default rather than storing null
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the fixed value applied across all simulation years, used only when
	 * {@link #getMode()} is {@link PlanningTempTargetMode#FIXED_VALUE}.
	 *
	 * @return the fixed target value
	 */
	public double getFixedValue() {
		return _fixedValue; // Simple accessor
	}

	/**
	 * Sets the fixed value applied across all simulation years and marks this target modified.
	 *
	 * @param value the new fixed target value
	 */
	public void setFixedValue(double value) {
		_fixedValue = value; // Store the new value verbatim; no bounds checking is performed here
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the path to the imported time series, used only when {@link #getMode()} is
	 * {@link PlanningTempTargetMode#TIMESERIES}. The referenced series is expected to span
	 * the full simulation period.
	 *
	 * @return the timeseries file path, or an empty string if not yet set
	 */
	public String getTimeseriesFilePath() {
		return _timeseriesFilePath; // Simple accessor
	}

	/**
	 * Sets the path to the imported time series and marks this target modified.
	 *
	 * @param path the timeseries file path; null is stored as an empty string
	 */
	public void setTimeseriesFilePath(String path) {
		_timeseriesFilePath = path == null ? "" : path; // Normalize null to empty string for safe display/serialization
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the path to the Jython script containing custom target logic, used only
	 * when {@link #getMode()} is {@link PlanningTempTargetMode#JYTHON_SCRIPT}.
	 *
	 * @return the Jython script's file path, or an empty string if not yet set
	 */
	public String getJythonScriptPath() {
		return _jythonScriptPath; // Simple accessor
	}

	/**
	 * Sets the path to the Jython script and marks this target modified.
	 *
	 * @param path a path directly to the .py script file; null is stored as an empty string
	 */
	public void setJythonScriptPath(String path) {
		_jythonScriptPath = path == null ? "" : path; // Normalize null to empty string for safe display/serialization
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Persists this target's fields to a child "PlanningTempTarget" element under the
	 * given parent.
	 *
	 * @param parent the JDOM element to which the new "PlanningTempTarget" element is appended
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("PlanningTempTarget"); // Create the root element for this object's data
		parent.addContent(myElem); // Attach it under the caller-supplied parent element

		XMLUtilities.saveNamedType(myElem, this); // Persist the inherited name/index fields

		// Persist the mode as its enum constant name (e.g. "FIXED_VALUE") for round-trip safety
		XMLUtilities.addChildContent(myElem, "Mode", _mode.name());
		// Persist the fixed value as a string; parsed back with Double.parseDouble on load
		XMLUtilities.addChildContent(myElem, "FixedValue", Double.toString(_fixedValue));
		// Persist the timeseries path verbatim
		XMLUtilities.addChildContent(myElem, "TimeseriesFilePath", _timeseriesFilePath);
		// Persist the Jython script path verbatim
		XMLUtilities.addChildContent(myElem, "JythonScriptPath", _jythonScriptPath);
	}

	/**
	 * Restores this target's fields from a "PlanningTempTarget" element previously written
	 * by {@link #saveData(Element)}.
	 *
	 * @param myElem the "PlanningTempTarget" element to load from
	 * @return true if the element was non-null and loading proceeded; false otherwise
	 */
	public boolean loadData(Element myElem) {
		if (myElem == null) {
			return false; // Nothing to load from; signal failure to the caller
		}

		XMLUtilities.loadNamedType(myElem, this); // Restore the inherited name/index fields

		// Read the stored mode name, defaulting to FIXED_VALUE if the element is missing
		String modeStr = XMLUtilities.getChildElementAsString(myElem, "Mode", PlanningTempTargetMode.FIXED_VALUE.name());
		try {
			_mode = PlanningTempTargetMode.valueOf(modeStr); // Convert the stored name back into the enum constant
		} catch (IllegalArgumentException ex) {
			_mode = PlanningTempTargetMode.FIXED_VALUE; // Unrecognized/corrupted value; fall back to a safe default
		}

		// Read the stored fixed value as a string, defaulting to "0.0" if missing
		String fixedValueStr = XMLUtilities.getChildElementAsString(myElem, "FixedValue", "0.0");
		try {
			_fixedValue = Double.parseDouble(fixedValueStr); // Parse back into a double
		} catch (NumberFormatException ex) {
			_fixedValue = 0.0; // Unparsable/corrupted value; fall back to zero
		}

		// Read the remaining two path fields directly, defaulting to empty strings
		_timeseriesFilePath = XMLUtilities.getChildElementAsString(myElem, "TimeseriesFilePath", "");
		_jythonScriptPath = XMLUtilities.getChildElementAsString(myElem, "JythonScriptPath", "");

		return true; // Loading completed successfully
	}

	/**
	 * Returns this target's name, so it displays sensibly in combo boxes and lists.
	 *
	 * @return the target's name
	 */
	@Override
	public String toString() {
		return getName(); // Delegate to the inherited NamedType name accessor
	}
}
