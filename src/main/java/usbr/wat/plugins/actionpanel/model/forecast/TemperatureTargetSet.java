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
import hec.model.RunTimeWindow;
import org.jdom.Element;
import rma.util.RMAConst;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TemperatureTargetSet extends NamedType
{
    private static final Logger LOGGER = Logger.getLogger(TemperatureTargetSet.class.getName());
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
        return true;
    }

    public List<TimeSeriesContainer> getTimeSeriesData(RunTimeWindow timeWindow)
    {
        if(_isUserDefined || _timeSeriesData.isEmpty())
        {
            loadTimeSeriesData();
        }
        if(!_timeSeriesData.isEmpty() && _timeSeriesData.get(0).getStartTime().getTimeInMillis() != timeWindow.getStartTime().getTimeInMillis())
        {
            shiftTimeSeriesDataToAnalysisYear(timeWindow);
        }
        return new ArrayList<>(_timeSeriesData);
    }

    private void shiftTimeSeriesDataToAnalysisYear(RunTimeWindow timeWindow)
    {
        for(TimeSeriesContainer tsc : _timeSeriesData)
        {
            HecTime startTime = timeWindow.getStartTime();
            LocalDate sourceStart = tsc.getStartTime().getLocalDateTime().toLocalDate();
            LocalDate analysisStart = startTime.getLocalDateTime().toLocalDate();
            int diffInMinutes = (int) Duration.between(sourceStart.atStartOfDay(), analysisStart.atStartOfDay()).toMinutes();
            applyShiftToTsc(tsc, diffInMinutes);
            int trimStartYear = timeWindow.getStartTime().year();
            int trimEndYear = timeWindow.getEndTime().year();
            if(!tsc.allMissing())
            {
                tsc.trimToTime(new HecTime("01Jan"+trimStartYear, "0000"),new HecTime("31Dec"+trimEndYear, "2400"));
            }
        }
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
    public List<DSSPathname>getDssPathNames()
    {
        return _dssPathNames;
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
                    if(tsc.allMissing())
                    {
                        tsc = buildFixedDataForUserDefined();
                    }
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
        return DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, true);
    }

    private void applyShiftToTsc(TimeSeriesContainer tsc, int shift)
    {
        if(tsc.times != null)
        {
            for(int i=0; i < tsc.times.length; i++)
            {
                tsc.times[i] = tsc.times[i] + shift;
            }
            tsc.startTime = tsc.times[0];
            tsc.startHecTime = new HecTime(tsc.startTime);
            tsc.endTime = tsc.times[tsc.times.length - 1];
            tsc.endHecTime = new HecTime(tsc.endTime);
        }
    }

    private TimeSeriesContainer buildFixedDataForUserDefined()
    {
        TimeSeriesContainer tsc = new TimeSeriesContainer();
        int year = 2020;
        LocalTime localTime = LocalTime.of(0, 1);
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate startDate = LocalDate.of(year, 1, 1); // Start date: January 1st, 2020
        LocalDate endDate = LocalDate.of(year+1, 1, 7); // End date: January 3rd, 2021 (first week)
        int numWeeks = (int) ChronoUnit.WEEKS.between(startDate, endDate);
        LocalDate currentDate = startDate;
        int[] times = new int[numWeeks + 1];
        int i=0;
        while (currentDate.isBefore(endDate))
        {
            times[i] = HecTime.fromZonedDateTime(ZonedDateTime.of(currentDate, localTime, zoneId)).value();
            currentDate = currentDate.plusDays(7);
            i++;
        }
        tsc.times = times;
        tsc.setTimes(new HecTimeArray(times));
        List<Double> nanList = IntStream.range(0, numWeeks +1)
                .mapToObj(index -> RMAConst.HEC_UNDEFINED_DOUBLE)
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
