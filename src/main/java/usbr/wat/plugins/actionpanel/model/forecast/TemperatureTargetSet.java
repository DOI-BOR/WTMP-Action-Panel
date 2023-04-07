package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import com.rma.util.XMLUtilities;
import hec.heclib.dss.DSSPathname;
import hec.heclib.util.HecTime;
import hec.heclib.util.HecTimeArray;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import hec.lang.NamedType;
import org.jdom.Element;
import rma.util.RMAConst;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TemperatureTargetSet extends NamedType
{
    private static final String USER_DEFINED_ATTRIBUTE_NAME = "user-defined";
    private static final String FILE_PATH_ELEM_NAME = "file-path";
    private static final String PATH_NAMES_ELEM_NAME = "dss-pathnames";
    private static final String PATH_NAME_ELEM_NAME = "dss-pathname";
    private final List<TimeSeriesContainer> _timeSeriesData = new ArrayList<>();
    private final List<DSSPathname> _dssPathNames = new ArrayList<>();
    private boolean _isUserDefined;
    private Path _filePath;
    private int _numberOfUserDefinedTempTargets;

    public TemperatureTargetSet()
    {
        super();
    }

    public boolean saveData(Element parent)
    {
        Element myElem = new Element("TemperatureTargetSet");
        XMLUtilities.saveNamedType(myElem, this);
        myElem.setAttribute(USER_DEFINED_ATTRIBUTE_NAME, String.valueOf(_isUserDefined));
        Element filePathElem = new Element(FILE_PATH_ELEM_NAME);
        filePathElem.setText(_filePath.toString());
        myElem.addContent(filePathElem);
        Element dssPathnamesElem = new Element(PATH_NAMES_ELEM_NAME);
        for(DSSPathname pathname : _dssPathNames)
        {
            Element dssPathnameElem = new Element(PATH_NAME_ELEM_NAME);
            dssPathnameElem.setText(pathname.getPathname());
            dssPathnamesElem.addContent(dssPathnameElem);
        }
        myElem.addContent(dssPathnamesElem);
        parent.addContent(myElem);
        return true;
    }

    public boolean loadData(Element myElem)
    {
        XMLUtilities.loadNamedType(myElem, this);
        _isUserDefined = Boolean.parseBoolean(myElem.getAttribute(USER_DEFINED_ATTRIBUTE_NAME).getValue());
        _filePath = null;
        Element filePathElem = myElem.getChild(FILE_PATH_ELEM_NAME);
        if(filePathElem != null && filePathElem.getText() != null)
        {
            _filePath = Paths.get(filePathElem.getText());
        }
        Element dssPathNamesElem = myElem.getChild(PATH_NAMES_ELEM_NAME);
        if(dssPathNamesElem != null)
        {
            List<?> dssPathnameChild = dssPathNamesElem.getChildren(PATH_NAME_ELEM_NAME);
            for(Object child : dssPathnameChild)
            {
                if(child instanceof Element)
                {
                    Element pathnameElem = (Element) child;
                    _dssPathNames.add(new DSSPathname(pathnameElem.getText()));
                }
            }
        }
        loadTimeSeriesData();
        return true;
    }

    public List<TimeSeriesContainer> getTimeSeriesData()
    {
        if(_isUserDefined || _timeSeriesData.isEmpty())
        {
            loadTimeSeriesData();
        }
        return new ArrayList<>(_timeSeriesData);
    }

    public boolean isUserDefined()
    {
        return _isUserDefined;
    }

    public Path getFilePath()
    {
        return _filePath;
    }

    public void setUserDefined(boolean userDefined)
    {
        _isUserDefined = userDefined;
    }

    public void setFilePath(Path filePath)
    {
        _filePath = filePath;
    }

    public void setDssPathNames(List<DSSPathname> dssPathNames)
    {
        _dssPathNames.clear();
        _dssPathNames.addAll(dssPathNames);
    }

    private void loadTimeSeriesData()
    {
        _timeSeriesData.clear();
        if(_isUserDefined && _filePath == null)
        {
            for(int i=1; i <= _numberOfUserDefinedTempTargets; i++)
            {
                TimeSeriesContainer fixedTscForUserDefined = buildFixedDataForUserDefined();
                _timeSeriesData.add(fixedTscForUserDefined);
            }
        }
        else
        {
            for(DSSPathname pathname : _dssPathNames)
            {
                TimeSeriesContainer tsc = buildTsFromPathname(pathname);
                if(tsc != null)
                {
                    _timeSeriesData.add(tsc);
                }
            }
        }
    }

    private TimeSeriesContainer buildTsFromPathname(DSSPathname pathname)
    {
        DSSIdentifier dssIdentifier = new DSSIdentifier();
        dssIdentifier.setFileName(Project.getCurrentProject().getAbsolutePath(_filePath.toString()));
        dssIdentifier.setDSSPath(pathname.getPathname());
        return DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, false);
    }

    private TimeSeriesContainer buildFixedDataForUserDefined()
    {
        TimeSeriesContainer tsc = new TimeSeriesContainer();
        int currentYear = LocalDate.now().getYear();
        LocalTime localTime = LocalTime.of(0, 1);
        ZoneId zoneId = ZoneId.systemDefault();
        int[] times = new int[31];
        times[0] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 2, 1), localTime, zoneId)).value();
        times[1] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 3, 31), localTime, zoneId)).value();
        times[2] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 4, 7), localTime, zoneId)).value();
        times[3] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 4, 14), localTime, zoneId)).value();
        times[4] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 4, 21), localTime, zoneId)).value();
        times[5] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 4, 30), localTime, zoneId)).value();
        times[6] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 5, 7), localTime, zoneId)).value();
        times[7] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 5, 14), localTime, zoneId)).value();
        times[8] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 5, 21), localTime, zoneId)).value();
        times[9] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 5, 31), localTime, zoneId)).value();
        times[10] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 6, 7), localTime, zoneId)).value();
        times[11] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 6, 14), localTime, zoneId)).value();
        times[12] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 6, 21), localTime, zoneId)).value();
        times[13] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 6, 30), localTime, zoneId)).value();
        times[14] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 7, 7), localTime, zoneId)).value();
        times[15] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 7, 14), localTime, zoneId)).value();
        times[16] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 7, 21), localTime, zoneId)).value();
        times[17] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 7, 31), localTime, zoneId)).value();
        times[18] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 8, 7), localTime, zoneId)).value();
        times[19] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 8, 14), localTime, zoneId)).value();
        times[20] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 8, 21), localTime, zoneId)).value();
        times[21] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 8, 31), localTime, zoneId)).value();
        times[22] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 9, 7), localTime, zoneId)).value();
        times[23] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 9, 15), localTime, zoneId)).value();
        times[24] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 9, 21), localTime, zoneId)).value();
        times[25] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 9, 30), localTime, zoneId)).value();
        times[26] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 10, 7), localTime, zoneId)).value();
        times[27] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 10, 14), localTime, zoneId)).value();
        times[28] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 10, 21), localTime, zoneId)).value();
        times[29] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 10, 31), localTime, zoneId)).value();
        times[30] = HecTime.fromZonedDateTime(ZonedDateTime.of(LocalDate.of(currentYear, 11, 7), localTime, zoneId)).value();
        tsc.times = times;
        tsc.setTimes(new HecTimeArray(times));
        List<Double> nanList = IntStream.range(0, 31)
                .mapToObj(i -> RMAConst.HEC_UNDEFINED_DOUBLE)
                .collect(Collectors.toList());
        tsc.values = nanList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        tsc.numberValues = nanList.size();
        return tsc;
    }

    public void setNumberOfUserDefinedTempTargets(int value)
    {
        _numberOfUserDefinedTempTargets = value;
    }
}
