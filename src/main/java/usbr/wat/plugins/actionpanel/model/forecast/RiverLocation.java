/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import hec.lang.NamedType;


public final class RiverLocation extends NamedType
{
    public RiverLocation()
    {
        // Default constructor needed for Jackson deserialization
    }

    public RiverLocation(String name, int id)
    {
        super(name);
        setIndex(id);
    }

    @JsonProperty("Name")
    @Override
    public String getName()
    {
        return super.getName();
    }

    @JsonProperty("Id")
    @Override
    public int getIndex()
    {
        return super.getIndex();
    }
}
