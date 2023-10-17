/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.rma.model.Project;

import java.io.IOException;

public final class FileNameSerializer extends JsonSerializer<String>
{
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeString(Project.getCurrentProject().getRelativePath(value));
    }
}
