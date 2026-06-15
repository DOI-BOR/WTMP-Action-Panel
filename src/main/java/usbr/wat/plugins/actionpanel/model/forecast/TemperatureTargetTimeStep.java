package usbr.wat.plugins.actionpanel.model.forecast;

/**
 * Enumerates the supported HEC-DSS time-step interval strings for water-temperature
 * target time series within the WTMP forecast action panel.
 *
 * Each constant maps a logical time-step name to the corresponding HEC-DSS E-part
 * string (e.g., {@code "1Week"}). The {@link #toString()} method returns this E-part
 * string directly, allowing constants to be used wherever a DSS interval string is
 * expected — for example, when constructing or modifying {@link hec.heclib.dss.DSSPathname}
 * E-parts or when performing backwards-compatible DSS record lookups.
 *
 * @see usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet
 */
public enum TemperatureTargetTimeStep {
    /**
     * Represents a one-hour regular time step; DSS E-part value: {@code "1Hour"}.
     */
    REGULAR_HOURLY("1Hour"),

    /**
     * Represents a one-week regular time step; DSS E-part value: {@code "1Week"}.
     * This is the default interval used when saving user-defined temperature targets.
     */
    REGULAR_WEEKLY("1Week"),

    /**
     * Represents a one-month regular time step; DSS E-part value: {@code "1Month"}.
     */
    REGULAR_MONTHLY("1Month"),

    /**
     * Represents a one-day regular time step; DSS E-part value: {@code "1Day"}.
     */
    REGULAR_DAILY("1Day");

    // The HEC-DSS E-part string that this time step maps to (e.g., "1Week")
    private final String _displayName;

    /**
     * Constructs a {@code TemperatureTargetTimeStep} with the given HEC-DSS E-part string.
     *
     * @param displayName the DSS E-part interval string corresponding to this time step;
     *                    must not be {@code null} or empty
     */
    TemperatureTargetTimeStep(String displayName) {
        // Store the DSS E-part string for use in toString and DSS path construction
        _displayName = displayName;
    }

    /**
     * Returns the HEC-DSS E-part interval string for this time step.
     *
     * This allows enum constants to be used directly wherever a DSS E-part string
     * is required, such as in {@link hec.heclib.dss.DSSPathname#setEPart(String)}.
     *
     * @return the DSS E-part string (e.g., {@code "1Week"}); never {@code null}
     */
    @Override
    public String toString() {
        return _displayName;
    }
}
