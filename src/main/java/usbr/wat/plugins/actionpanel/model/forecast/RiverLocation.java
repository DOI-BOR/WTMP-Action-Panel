package usbr.wat.plugins.actionpanel.model.forecast;

import com.fasterxml.jackson.annotation.JsonIgnore;                     // Marks a field or method so that Jackson excludes it from serialization and deserialization
import com.fasterxml.jackson.annotation.JsonProperty;                   // Maps a Java field or method to a specific JSON key during serialization and deserialization
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;       // Specifies a custom deserializer class to use when reading a field from JSON
import com.fasterxml.jackson.databind.annotation.JsonSerialize;         // Specifies a custom serializer class to use when writing a field to JSON

import hec.lang.NamedType;                                              // Base class providing a named, indexed type with name and integer index fields

import usbr.wat.plugins.actionpanel.ui.forecast.FileNameDeserializer;   // Custom Jackson deserializer that resolves a DSS file name from a relative or abbreviated path
import usbr.wat.plugins.actionpanel.ui.forecast.FileNameSerializer;     // Custom Jackson serializer that converts a DSS file name to its relative or abbreviated form

/**
 * Represents a named river location used within the WTMP forecast action panel.
 *
 * A {@code RiverLocation} extends {@link NamedType} to carry a human-readable name
 * and an integer identifier, and additionally holds references to the HEC-DSS file
 * and pathname that back the location's time-series or paired data.
 *
 * Instances are serialized to and from JSON via Jackson. The DSS file name is
 * processed through custom serializer/deserializer pairs ({@link FileNameSerializer}
 * and {@link FileNameDeserializer}) to handle path abbreviation. The raw DSS fields
 * are excluded from JSON output at the getter level with {@code @JsonIgnore} because
 * the class-level {@code @JsonProperty} annotations on the fields already govern
 * their JSON representation.
 *
 * This class is declared {@code final} to prevent subclassing.
 *
 * @see hec.lang.NamedType
 * @see FileNameSerializer
 * @see FileNameDeserializer
 */
public final class RiverLocation extends NamedType {
    // The HEC-DSS file path for this location; serialized under the "DSSFileName" JSON key
    // and processed by the custom FileNameSerializer/FileNameDeserializer pair
    @JsonProperty("DSSFileName")
    @JsonDeserialize(using = FileNameDeserializer.class)
    @JsonSerialize(using = FileNameSerializer.class)
    private String _dssFileName;

    // The full HEC-DSS pathname (A/B/C/D/E/F parts) identifying the record within the DSS file;
    // serialized under the "DSSPathName" JSON key using default string handling
    @JsonProperty("DSSPathName")
    private String _dssPathName;

    /**
     * Default no-argument constructor required by the Jackson framework to instantiate
     * this class during JSON deserialization before populating its fields.
     */
    public RiverLocation() {
        // Default constructor needed for Jackson deserialization
    }

    /**
     * Constructs a fully initialised {@code RiverLocation} with all fields provided.
     *
     * @param name        the human-readable name for this river location; passed to
     *                    the {@link NamedType} superclass constructor
     * @param id          the integer identifier (index) for this location
     * @param dssFileName the absolute or relative path to the HEC-DSS file backing
     *                    this location's data
     * @param dssPathName the full HEC-DSS pathname (A/B/C/D/E/F) identifying the
     *                    specific record within the DSS file
     */
    public RiverLocation(String name, int id, String dssFileName, String dssPathName) {
        // Delegate name storage to the NamedType superclass
        super(name);

        // Store the integer identifier via the inherited setIndex method
        setIndex(id);

        // Assign the DSS file path for this location
        _dssFileName = dssFileName;

        // Assign the full DSS pathname for this location
        _dssPathName = dssPathName;
    }

    /**
     * Returns the human-readable name of this river location.
     *
     * Overrides {@link NamedType#getName()} to expose the value under the {@code "Name"}
     * JSON key during serialization.
     *
     * @return the name of this river location; never {@code null} if set via the
     * parameterised constructor
     */
    @JsonProperty("Name")
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Returns the integer identifier (index) of this river location.
     *
     * Overrides {@link NamedType#getIndex()} to expose the value under the {@code "Id"}
     * JSON key during serialization.
     *
     * @return the integer index of this river location
     */
    @JsonProperty("Id")
    @Override
    public int getIndex() {
        return super.getIndex();
    }

    /**
     * Returns the path to the HEC-DSS file associated with this river location.
     *
     * Annotated with {@code @JsonIgnore} because the DSS file name is already governed
     * by the {@code @JsonProperty("DSSFileName")} annotation on the backing field, and
     * exposing it again via this getter would cause duplicate JSON output.
     *
     * @return the HEC-DSS file path string, or {@code null} if not set
     */
    @JsonIgnore
    public String getDssFileName() {
        return _dssFileName;
    }

    /**
     * Returns the full HEC-DSS pathname that identifies this location's record within
     * the DSS file.
     *
     * Annotated with {@code @JsonIgnore} for the same reason as {@link #getDssFileName()}:
     * the field-level {@code @JsonProperty("DSSPathName")} annotation already handles
     * JSON representation, so the getter must be suppressed to avoid duplication.
     *
     * @return the full HEC-DSS path string (A/B/C/D/E/F), or {@code null} if not set
     */
    @JsonIgnore
    public String getDssPathName() {
        return _dssPathName;
    }
}
