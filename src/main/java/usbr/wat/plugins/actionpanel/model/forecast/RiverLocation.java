/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import hec.lang.NamedType;
import usbr.wat.plugins.actionpanel.ui.forecast.FileNameDeserializer;
import usbr.wat.plugins.actionpanel.ui.forecast.FileNameSerializer;


public final class RiverLocation extends NamedType
{

    @JsonProperty("DSSFileName")
    @JsonDeserialize(using = FileNameDeserializer.class)
    @JsonSerialize(using = FileNameSerializer.class)
    private String _dssFileName;
    @JsonProperty("DSSPathName")
    private String _dssPathName;

    public RiverLocation()
    {
        // Default constructor needed for Jackson deserialization
    }

    public RiverLocation(String name, int id, String dssFileName, String dssPathName)
    {
        super(name);
        setIndex(id);
        _dssFileName = dssFileName;
        _dssPathName = dssPathName;
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

    @JsonIgnore
    public String getDssFileName()
    {
        return _dssFileName;
    }

    @JsonIgnore
    public String getDssPathName()
    {
        return _dssPathName;
    }
}
