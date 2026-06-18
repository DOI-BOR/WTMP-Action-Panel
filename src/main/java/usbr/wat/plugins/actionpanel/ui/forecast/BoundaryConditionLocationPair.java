package usbr.wat.plugins.actionpanel.ui.forecast;

// Provides DSSPathname for storing and manipulating the HEC-DSS A/B/C/D/E/F path components
// associated with this location-parameter pair
import hec.heclib.dss.DSSPathname;

// Provides Objects for null-safe equality checks and hash code generation
import java.util.Objects;

/**
 * Represents a pairing of a physical monitoring location and a measured parameter,
 * together with the HEC-DSS pathname that identifies the corresponding time-series
 * record for that location-parameter combination.
 *
 * Instances of this class are used within the WTMP forecast action panel UI to
 * associate boundary condition data with the specific river locations and parameters
 * (e.g., flow, temperature) they describe.
 *
 * Equality and hash code are based solely on {@code _location} and {@code _parameter};
 * the DSS path is not considered so that two pairs referencing different DSS records for
 * the same location and parameter are still treated as equal. The {@link #toString()}
 * method returns a human-readable label in the form {@code "<location> - <parameter>"}
 * suitable for display in UI components.
 *
 * This class is declared {@code final} to prevent subclassing.
 */
public final class BoundaryConditionLocationPair {
    // The name of the physical monitoring location (e.g., a river station or reservoir)
    private String _location;

    // The name of the measured parameter at this location (e.g., "FLOW" or "TEMP-WATER")
    private String _parameter;

    // The HEC-DSS pathname identifying the time-series record for this location-parameter pair
    private DSSPathname _dssPath;

    /**
     * Returns the name of the physical monitoring location.
     *
     * @return the location name string, or {@code null} if not yet set
     */
    public String getLocation() {
        return _location;
    }

    /**
     * Returns the name of the measured parameter at this location.
     *
     * @return the parameter name string, or {@code null} if not yet set
     */
    public String getParameter() {
        return _parameter;
    }

    /**
     * Returns the HEC-DSS pathname associated with this location-parameter pair.
     *
     * @return the {@link DSSPathname} for this pair's time-series record, or {@code null}
     *         if not yet set
     */
    public DSSPathname getDssPath() {
        return _dssPath;
    }

    /**
     * Sets the name of the physical monitoring location.
     *
     * @param location the location name to assign; should not be {@code null} or empty
     */
    public void setLocation(String location) {
        _location = location;
    }

    /**
     * Sets the name of the measured parameter at this location.
     *
     * @param parameter the parameter name to assign; should not be {@code null} or empty
     */
    public void setParameter(String parameter) {
        _parameter = parameter;
    }

    /**
     * Sets the HEC-DSS pathname identifying the time-series record for this
     * location-parameter pair.
     *
     * @param dssPath the {@link DSSPathname} to associate with this pair; may be
     *                {@code null} to clear the current path
     */
    public void setDssPath(DSSPathname dssPath) {
        _dssPath = dssPath;
    }

    /**
     * Returns a human-readable label for this pair in the form
     * {@code "<location> - <parameter>"}, suitable for display in UI components
     * such as list boxes or combo boxes.
     *
     * @return the combined location-parameter display string
     */
    @Override
    public String toString() {
        return _location + " - " + _parameter;
    }

    /**
     * Indicates whether this pair is equal to another object.
     *
     * Two {@code BoundaryConditionLocationPair} instances are considered equal when
     * both their location and parameter fields are equal. The DSS path is intentionally
     * excluded from the equality check so that pairs referencing the same location and
     * parameter are treated as identical regardless of which DSS record they point to.
     *
     * @param o the object to compare with this pair
     * @return {@code true} if {@code o} is a {@code BoundaryConditionLocationPair} with
     *         the same location and parameter; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        // A pair is always equal to itself
        if (this == o) {
            return true;
        }

        // Null or incompatible type means not equal
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Safe to cast; class has been verified above
        BoundaryConditionLocationPair that = (BoundaryConditionLocationPair) o;

        // Equality is determined by location and parameter only; DSS path is excluded
        return Objects.equals(_location, that._location) && Objects.equals(_parameter, that._parameter);
    }

    /**
     * Returns a hash code derived from the location and parameter fields, consistent
     * with the equality contract defined by {@link #equals(Object)}.
     *
     * @return an integer hash code for this pair
     */
    @Override
    public int hashCode() {
        return Objects.hash(_location, _parameter);
    }
}
