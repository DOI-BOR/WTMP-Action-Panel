package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.core.JsonParser;                       // Provides JsonParser for reading the raw string value from the current JSON token
import com.fasterxml.jackson.databind.DeserializationContext;       // Provides DeserializationContext carrying configuration and state for the current deserialization operation
import com.fasterxml.jackson.databind.JsonDeserializer;             // Provides JsonDeserializer as the base class for custom Jackson string deserializers

import com.rma.model.Project;                                       // Provides Project for resolving a relative file name to an absolute path within the current WAT project

import java.io.IOException;                                         // Provides IOException for signaling JSON read failures to the Jackson framework

/**
 * A custom Jackson deserializer that converts a relative DSS file name read from JSON
 * into an absolute file-system path by resolving it against the current WAT project
 * directory.
 *
 * This deserializer is registered on the {@code _dssFileName} field of
 * {@link usbr.wat.plugins.actionpanel.model.forecast.RiverLocation} via
 * {@code @JsonDeserialize(using = FileNameDeserializer.class)}, and is the counterpart
 * to {@link FileNameSerializer}, which performs the reverse transformation (absolute
 * path → relative name) during serialization.
 *
 * When Jackson encounters the {@code "DSSFileName"} JSON key, it calls
 * {@link #deserialize(JsonParser, DeserializationContext)} with the raw string value
 * from the JSON token. The deserializer passes that value to
 * {@link Project#getAbsolutePath(String)} to produce the absolute path, which is then
 * bound to the field.
 *
 * This class is declared {@code final} to prevent subclassing.
 *
 * @see FileNameSerializer
 * @see usbr.wat.plugins.actionpanel.model.forecast.RiverLocation
 */
public final class FileNameDeserializer extends JsonDeserializer<String> {
    /**
     * Deserializes the current JSON string token into an absolute file-system path by
     * resolving the raw value against the current WAT project directory.
     *
     * Reads the raw relative file name from the JSON parser, then delegates to
     * {@link Project#getAbsolutePath(String)} to convert it to an absolute path
     * suitable for direct use in file I/O operations.
     *
     * @param jp   the {@link JsonParser} positioned at the string token to deserialize
     * @param ctxt the {@link DeserializationContext} providing configuration and
     *             type information for the current deserialization (not used directly)
     * @return the absolute file-system path corresponding to the relative file name
     * read from the JSON token
     * @throws IOException if the JSON token cannot be read as a string value
     */
    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        // Read the raw relative file name from the current JSON token
        String fileName = jp.getValueAsString();

        // Resolve the relative name to an absolute path using the current project's base directory
        return Project.getCurrentProject().getAbsolutePath(fileName);
    }
}
