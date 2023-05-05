package usbr.wat.plugins.actionpanel.ui.forecast;

import hec.heclib.dss.DSSPathname;

import java.util.Objects;

public final class BoundaryConditionLocationPair
{
    private String _location;
    private String _parameter;
    private DSSPathname _dssPath;

    public String getLocation()
    {
        return _location;
    }

    public String getParameter()
    {
        return _parameter;
    }

    public DSSPathname getDssPath()
    {
        return _dssPath;
    }

    public void setLocation(String location)
    {
        _location = location;
    }

    public void setParameter(String parameter)
    {
        _parameter = parameter;
    }

    public void setDssPath(DSSPathname dssPath)
    {
        _dssPath = dssPath;
    }

    @Override
    public String toString()
    {
        return _location + " - " + _parameter;
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
        BoundaryConditionLocationPair that = (BoundaryConditionLocationPair) o;
        return Objects.equals(_location, that._location) && Objects.equals(_parameter, that._parameter);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_location, _parameter);
    }
}
