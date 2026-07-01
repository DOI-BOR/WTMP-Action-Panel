package usbr.wat.plugins.actionpanel.model.forecast;

import hec.heclib.dss.DSSPathname;  // Provides the DSSPathname class for constructing and parsing HEC-DSS file path components
import hec.io.PairedDataContainer;  // Provides PairedDataContainer for storing paired (X-Y) dataset records read from HEC-DSS

import java.text.ParseException;    // Provides ParseException for signaling failures when parsing date strings
import java.text.SimpleDateFormat;  // Provides SimpleDateFormat for formatting and parsing date strings according to a pattern
import java.util.Date;              // Provides the Date class for representing a specific instant in time
import java.util.Objects;           // Provides the Objects utility class for null-safe equality checks and hash code generation

/**
 * Represents a single water-temperature profile associated with a specific forecast date.
 *
 * A Profile holds a parsed date, a display name derived from that date, and optional
 * references to the HEC-DSS file and path that back the profile's paired data. Instances
 * are naturally ordered in reverse-chronological order (most recent first) via
 * {@link Comparable}.
 *
 * @see hec.io.PairedDataContainer
 */
public class Profile implements Comparable<Profile> {
    // Date format used for both parsing input date strings and formatting the profile display name
    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // The parsed date this profile represents; immutable after construction
    private final Date _date;

    // The paired data container holding the X-Y dataset for this profile (may be null until set)
    private PairedDataContainer _pdc;

    // Human-readable display name, defaulting to the ISO-formatted date string
    private String _name;

    // Absolute path to the HEC-DSS file that contains this profile's data
    private String _dssFileName;

    // Full HEC-DSS pathname (A/B/C/D/E/F parts) identifying the record within the DSS file
    private String _dssPath;

    /**
     * Constructs a Profile by parsing the supplied date string.
     *
     * The date string must conform to the {@code yyyy-MM-dd} pattern. The profile's
     * display name is initialised to the same ISO-formatted representation of the
     * parsed date.
     *
     * @param date a date string in {@code yyyy-MM-dd} format representing the
     *             forecast date for this profile
     * @throws ParseException if {@code date} does not match the expected format
     */
    public Profile(String date) throws ParseException {
        // Parse the raw date string into a Date object
        _date = parseDate(date);

        // Initialise the display name from the canonical ISO representation of the parsed date
        _name = OUTPUT_DATE_FORMAT.format(_date);
    }

    /**
     * Returns the display name of this profile, which defaults to the ISO date string
     * ({@code yyyy-MM-dd}) but may be overridden via {@link #setName(String)}.
     *
     * @return the display name of this profile
     */
    @Override
    public String toString() {
        return _name;
    }

    /**
     * Compares this profile to another for ordering purposes.
     *
     * Profiles are sorted in reverse-chronological order so that the most recent
     * profile appears first in sorted collections. A {@code null} argument is
     * treated as less than any non-null profile, placing it at the end of the list.
     *
     * @param other the profile to compare against; may be {@code null}
     * @return a negative integer, zero, or a positive integer as this profile's date
     * is later than, equal to, or earlier than {@code other}'s date;
     * returns {@code 1} when {@code other} is {@code null}
     */
    @Override
    public int compareTo(Profile other) {
        // Default return value treats this instance as greater than a null argument
        int retVal = 1;

        if (other != null) {
            // Delegate to Date.compareTo; note the reversed receiver/argument order
            // (other._date.compareTo(_date)) to achieve descending (newest-first) ordering
            retVal = other._date.compareTo(_date);
        }

        return retVal;
    }

    /**
     * Indicates whether this profile is equal to another object.
     *
     * Two profiles are considered equal when their dates and names are both equal.
     * This definition is consistent with {@link #hashCode()}.
     *
     * @param o the object to compare with this profile
     * @return {@code true} if {@code o} is a {@code Profile} with the same date and
     * name as this instance; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        // A profile is always equal to itself
        if (this == o) {
            return true;
        }

        // Null or different runtime type means not equal
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Safe to cast now that the class has been verified
        Profile profile = (Profile) o;

        // Equality requires both the date and the display name to match
        return Objects.equals(_date, profile._date) && Objects.equals(_name, profile._name);
    }

    /**
     * Returns a hash code derived from the profile's date and name fields,
     * consistent with the equality contract defined by {@link #equals(Object)}.
     *
     * @return an integer hash code for this profile
     */
    @Override
    public int hashCode() {
        return Objects.hash(_date, _name);
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format into a {@link Date} object.
     *
     * @param dateString the date string to parse; must match the {@code yyyy-MM-dd} pattern
     * @return the {@link Date} represented by {@code dateString}
     * @throws ParseException if {@code dateString} does not conform to the expected format
     */
    private Date parseDate(String dateString) throws ParseException {
        return OUTPUT_DATE_FORMAT.parse(dateString);
    }

    /**
     * Returns the forecast date this profile represents.
     *
     * @return the profile date; never {@code null}
     */
    public Date getDate() {
        return _date;
    }

    /**
     * Returns the paired data container associated with this profile, or {@code null}
     * if none has been set yet.
     *
     * @return the {@link PairedDataContainer} for this profile, or {@code null}
     */
    public PairedDataContainer getPdc() {
        return _pdc;
    }

    /**
     * Sets the paired data container for this profile and extracts the backing DSS file
     * name and full DSS pathname from it.
     *
     * @param pdc the {@link PairedDataContainer} to associate with this profile;
     *            must not be {@code null}
     */
    public void setPdc(PairedDataContainer pdc) {
        // Store the paired data container reference
        _pdc = pdc;

        // Extract and cache the DSS file name from the container metadata
        _dssFileName = pdc.fileName;

        // Extract and cache the full HEC-DSS pathname from the container metadata
        _dssPath = pdc.fullName;
    }

    /**
     * Returns the display name of this profile.
     *
     * @return the profile display name; defaults to an ISO-formatted date string
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets a custom display name for this profile, overriding the default ISO date string.
     *
     * @param name the new display name; should not be {@code null} or empty
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Returns the absolute path to the HEC-DSS file backing this profile's data,
     * or {@code null} if no DSS file has been associated yet.
     *
     * @return the HEC-DSS file path string, or {@code null}
     */
    public String getDssFileName() {
        return _dssFileName;
    }

    /**
     * Returns the full HEC-DSS pathname (A/B/C/D/E/F parts) that identifies this
     * profile's record within the DSS file, or {@code null} if not yet set.
     *
     * @return the full HEC-DSS path string, or {@code null}
     */
    public String getDssPath() {
        return _dssPath;
    }

    /**
     * Sets the absolute path to the HEC-DSS file that contains this profile's data.
     *
     * @param dssFileName the file-system path to the HEC-DSS file; should not be
     *                    {@code null} or empty
     */
    public void setDssFileName(String dssFileName) {
        _dssFileName = dssFileName;
    }

    /**
     * Sets the full HEC-DSS pathname that identifies this profile's record within
     * the DSS file.
     *
     * @param dssPath the complete HEC-DSS path string (A/B/C/D/E/F); should not be
     *                {@code null} or empty
     */
    public void setDssPath(String dssPath) {
        _dssPath = dssPath;
    }
}
