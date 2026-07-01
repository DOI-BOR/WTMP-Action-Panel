package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.core.JsonGenerator;                    // Provides JsonGenerator for writing the serialized string value to the JSON output stream
import com.fasterxml.jackson.databind.JsonSerializer;               // Provides JsonSerializer as the base class for custom Jackson string serializers
import com.fasterxml.jackson.databind.SerializerProvider;           // Provides SerializerProvider carrying configuration and type information for the current serialization operation

import com.rma.model.Project;                                       // Provides Project for converting an absolute file-system path to a project-relative path

import java.io.IOException;                                         // Provides IOException for signaling JSON write failures to the Jackson framework

/**
 * A custom Jackson serializer that converts an absolute DSS file-system path into a
 * relative path before writing it to JSON output, making the serialized form portable
 * across different installation directories.
 *
 * This serializer is registered on the {@code _dssFileName} field of
 * {@link usbr.wat.plugins.actionpanel.model.forecast.RiverLocation} via
 * {@code @JsonSerialize(using = FileNameSerializer.class)}, and is the counterpart to
 * {@link FileNameDeserializer}, which performs the reverse transformation (relative
 * path → absolute path) during deserialization.
 *
 * When Jackson serializes the {@code "DSSFileName"} JSON key, it calls
 * {@link #serialize(String, JsonGenerator, SerializerProvider)} with the absolute path
 * stored in the field. The serializer converts it to a project-relative path via
 * {@link Project#getRelativePath(String)} before writing it to the JSON output stream,
 * ensuring that saved configuration files remain valid when the project is moved or
 * opened on a different machine.
 *
 * This class is declared {@code final} to prevent subclassing.
 *
 * @see FileNameDeserializer
 * @see usbr.wat.plugins.actionpanel.model.forecast.RiverLocation
 */
public final class FileNameSerializer extends JsonSerializer<String> {
    /**
     * Serializes the given absolute file-system path as a project-relative path string
     * in the JSON output.
     *
     * Converts the absolute path to a relative path using
     * {@link Project#getRelativePath(String)}, then writes the result to the JSON
     * output stream via {@link JsonGenerator#writeString(String)}. This ensures that
     * the serialized JSON contains portable relative paths rather than machine-specific
     * absolute paths.
     *
     * @param value       the absolute file-system path to serialize; sourced from the
     *                    {@code _dssFileName} field of the enclosing object
     * @param gen         the {@link JsonGenerator} used to write the string token to
     *                    the JSON output stream
     * @param serializers the {@link SerializerProvider} carrying serialization
     *                    configuration and type information (not used directly)
     * @throws IOException if the string value cannot be written to the JSON output stream
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // Convert the absolute path to a project-relative path, then write it to the JSON output
        gen.writeString(Project.getCurrentProject().getRelativePath(value));
    }
}
