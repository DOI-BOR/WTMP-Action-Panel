/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import hec.heclib.dss.DSSPathname;
import hec.io.PairedDataContainer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Profile implements Comparable<Profile>
{
    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final Date _date;
    private PairedDataContainer _pdc;
    private String _name;
    private String _dssFileName;
    private DSSPathname _dssPath;

    /**
     * @param date
     */
    public Profile(String date) throws ParseException
    {
        _date = parseDate(date);
        _name = OUTPUT_DATE_FORMAT.format(_date);
    }

    @Override
    public String toString()
    {
        return _name;
    }

    @Override
    public int compareTo(Profile other)
    {
        int retVal = 1;
        if (other != null)
        {
            retVal = other._date.compareTo(_date);
        }
        return retVal;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Profile profile = (Profile) o;
        return Objects.equals(_date, profile._date) && Objects.equals(_name, profile._name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_date, _name);
    }

    private Date parseDate(String dateString) throws ParseException
    {
        return OUTPUT_DATE_FORMAT.parse(dateString);
    }

    public Date getDate()
    {
        return _date;
    }

    public PairedDataContainer getPdc()
    {
        return _pdc;
    }

    public void setPdc(PairedDataContainer pdc)
    {
        _pdc = pdc;
        _dssFileName = pdc.fileName;
        _dssPath = new DSSPathname(pdc.fullName);
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDssFileName()
    {
        return _dssFileName;
    }

    public DSSPathname getDssPath()
    {
        return _dssPath;
    }

    public void setDssFileName(String dssFileName)
    {
        _dssFileName = dssFileName;
    }

    public void setDssPath(DSSPathname dssPath)
    {
        _dssPath = dssPath;
    }
}
