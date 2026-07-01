package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.core.JsonParser;                   // Provides JsonParser for reading the raw token text from the JSON input stream
import com.fasterxml.jackson.core.JsonProcessingException;      // Provides JsonProcessingException for signalling JSON-specific parsing errors (required by interface)
import com.fasterxml.jackson.databind.DeserializationContext;   // Provides DeserializationContext for accessing contextual information during JSON deserialization
import com.fasterxml.jackson.databind.JsonDeserializer;         // Provides JsonDeserializer, the Jackson base class for custom type deserializers

import java.io.IOException;                                     // Provides IOException, the checked exception declared by the deserialize contract

/**
 * A Jackson custom deserializer that trims leading and trailing whitespace from
 * JSON string values before binding them to their target Java field. This prevents
 * fields populated from user-authored or externally generated JSON from carrying
 * unintentional surrounding spaces into the model.
 *
 * Register this deserializer on a String field or class using the Jackson
 * annotation @JsonDeserialize(using = TrimDeserializer.class).
 *
 * This class is package-private and final; it is not intended for use outside
 * the forecast UI package or for subclassing.
 *
 * @see com.fasterxml.jackson.databind.JsonDeserializer
 */
final class TrimDeserializer extends JsonDeserializer<String> {
    /**
     * Deserializes the current JSON token as a String and trims any leading or
     * trailing whitespace from the result. Returns null when the parser is null,
     * which guards against unexpected null parser references passed by the framework.
     *
     * @param p    the JsonParser positioned at the string token to deserialize;
     *             may be null, in which case null is returned
     * @param ctxt the DeserializationContext providing contextual deserialization state
     * @return the trimmed string value of the current token, or null if the parser is null
     * @throws IOException if an error occurs while reading the token text from the parser
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Return null when no parser is available; otherwise trim the raw token text
        return p == null ? null : p.getText().trim();
    }
}
