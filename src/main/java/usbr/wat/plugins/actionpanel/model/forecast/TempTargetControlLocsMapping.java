package usbr.wat.plugins.actionpanel.model.forecast;

// Marks a getter so that Jackson excludes it from JSON serialization and deserialization,
// preventing duplicate output when the backing field already carries a @JsonProperty annotation
import com.fasterxml.jackson.annotation.JsonIgnore;

// Maps a Java field to a specific JSON key name during serialization and deserialization
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single row in the temperature-target control locations mapping configuration,
 * describing how a source HEC-DSS time-series record is routed to one or more destination
 * DSS records within the WTMP forecast action panel.
 *
 * Each instance captures:
 *
 *   The source DSS file path and record pathname from which temperature target data is read.
 *   The number of destination locations that receive the data.
 *   The destination DSS file path and record pathname to which the data is written.
 *
 * Instances are serialized to and deserialized from JSON via Jackson. All five fields are
 * mapped to JSON keys through {@code @JsonProperty} on the fields. The corresponding
 * getters are annotated with {@code @JsonIgnore} to prevent Jackson from producing
 * duplicate keys during serialization.
 */
public class TempTargetControlLocsMapping {
    // File-system path to the HEC-DSS file from which the source temperature target record is read;
    // serialized under the "Source DSS file" JSON key
    @JsonProperty("Source DSS file")
    private String _sourceDssFile;

    // Full HEC-DSS pathname (A/B/C/D/E/F) identifying the source record within the source DSS file;
    // serialized under the "Source DSS record" JSON key
    @JsonProperty("Source DSS record")
    private String _sourceDssRecord;

    // The count of destination DSS locations that this source record is mapped to;
    // serialized under the "Number of Destinations" JSON key
    @JsonProperty("Number of Destinations")
    private int _numberOfDestinations;

    // File-system path to the HEC-DSS file to which the mapped data is written;
    // serialized under the "Destination DSS file" JSON key
    @JsonProperty("Destination DSS file")
    private String _destinationDssFile;

    // Full HEC-DSS pathname (A/B/C/D/E/F) identifying the destination record within the
    // destination DSS file; serialized under the "Destination DSS record" JSON key
    @JsonProperty("Destination DSS record")
    private String _destinationDssRecord;

    /**
     * Returns the file-system path to the source HEC-DSS file.
     *
     * Annotated with {@code @JsonIgnore} because the backing field already carries
     * {@code @JsonProperty("Source DSS file")}, and exposing this getter to Jackson
     * would produce a duplicate key in the serialized output.
     *
     * @return the source DSS file path string, or {@code null} if not set
     */
    @JsonIgnore
    public String getSourceDssFile() {
        return _sourceDssFile;
    }

    /**
     * Sets the file-system path to the source HEC-DSS file.
     *
     * @param sourceDssFile the path to the source DSS file; should not be {@code null}
     *                      or empty
     */
    public void setSourceDssFile(String sourceDssFile) {
        this._sourceDssFile = sourceDssFile;
    }

    /**
     * Returns the full HEC-DSS pathname identifying the source record within the source
     * DSS file.
     *
     * Annotated with {@code @JsonIgnore} because the backing field already carries
     * {@code @JsonProperty("Source DSS record")}, and exposing this getter to Jackson
     * would produce a duplicate key in the serialized output.
     *
     * @return the source DSS record pathname string, or {@code null} if not set
     */
    @JsonIgnore
    public String getSourceDssRecord() {
        return _sourceDssRecord;
    }

    /**
     * Sets the full HEC-DSS pathname that identifies the source record within the source
     * DSS file.
     *
     * @param sourceDssRecord the complete DSS pathname (A/B/C/D/E/F) for the source
     *                        record; should not be {@code null} or empty
     */
    public void setSourceDssRecord(String sourceDssRecord) {
        this._sourceDssRecord = sourceDssRecord;
    }

    /**
     * Returns the number of destination DSS locations that this source record is mapped to.
     *
     * Annotated with {@code @JsonIgnore} because the backing field already carries
     * {@code @JsonProperty("Number of Destinations")}, and exposing this getter to Jackson
     * would produce a duplicate key in the serialized output.
     *
     * @return the number of destination locations; zero if not set
     */
    @JsonIgnore
    public int getNumberOfDestinations() {
        return _numberOfDestinations;
    }

    /**
     * Sets the number of destination DSS locations that this source record is mapped to.
     *
     * @param numberOfDestinations the destination count; must be greater than or equal
     *                             to zero
     */
    public void setNumberOfDestinations(int numberOfDestinations) {
        this._numberOfDestinations = numberOfDestinations;
    }

    /**
     * Returns the file-system path to the destination HEC-DSS file.
     *
     * Annotated with {@code @JsonIgnore} because the backing field already carries
     * {@code @JsonProperty("Destination DSS file")}, and exposing this getter to Jackson
     * would produce a duplicate key in the serialized output.
     *
     * @return the destination DSS file path string, or {@code null} if not set
     */
    @JsonIgnore
    public String getDestinationDssFile() {
        return _destinationDssFile;
    }

    /**
     * Sets the file-system path to the destination HEC-DSS file.
     *
     * @param destinationDssFile the path to the destination DSS file; should not be
     *                           {@code null} or empty
     */
    public void setDestinationDssFile(String destinationDssFile) {
        this._destinationDssFile = destinationDssFile;
    }

    /**
     * Returns the full HEC-DSS pathname identifying the destination record within the
     * destination DSS file.
     *
     * Annotated with {@code @JsonIgnore} because the backing field already carries
     * {@code @JsonProperty("Destination DSS record")}, and exposing this getter to Jackson
     * would produce a duplicate key in the serialized output.
     *
     * @return the destination DSS record pathname string, or {@code null} if not set
     */
    @JsonIgnore
    public String getDestinationDssRecord() {
        return _destinationDssRecord;
    }

    /**
     * Sets the full HEC-DSS pathname that identifies the destination record within the
     * destination DSS file.
     *
     * @param destinationDssRecord the complete DSS pathname (A/B/C/D/E/F) for the
     *                             destination record; should not be {@code null} or empty
     */
    public void setDestinationDssRecord(String destinationDssRecord) {
        this._destinationDssRecord = destinationDssRecord;
    }

    /**
     * Returns a human-readable string representation of this mapping configuration,
     * listing all five fields by name and value.
     *
     * Intended for debugging and logging purposes only; not suitable for serialization.
     *
     * @return a formatted string in the form:
     * {@code Configuration{sourceDssFile='...', sourceDssRecord='...',
     * numberOfDestinations=N, destinationDssFile='...', destinationDssRecord='...'}}
     */
    @Override
    public String toString() {
        // Build a diagnostic string that includes every field for easy inspection
        return "Configuration{" +
                "sourceDssFile='" + getSourceDssFile() + '\'' +
                ", sourceDssRecord='" + getSourceDssRecord() + '\'' +
                ", numberOfDestinations=" + getNumberOfDestinations() +
                ", destinationDssFile='" + getDestinationDssFile() + '\'' +
                ", destinationDssRecord='" + getDestinationDssRecord() + '\'' +
                '}';
    }
}
