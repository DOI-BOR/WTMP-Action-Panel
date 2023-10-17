/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rma.model.Project;

import java.io.IOException;

public final class FileNameDeserializer extends JsonDeserializer<String>
{
    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        String fileName = jp.getValueAsString();
        return Project.getCurrentProject().getAbsolutePath(fileName);
    }
}
