/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import com.rma.util.XMLUtilities;
import hec.data.Parameter;
import hec.data.Units;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecTimeSeriesBase;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TemperatureTargetSet extends NamedType
{
    private static final String OUTPUT_DSS_FILE_ELEM_ID = "Output-DSS-File";
    private static final String USER_DEFINED_ATTRIBUTE_NAME = "user-defined";
    private static final String FILE_PATH_ELEM_NAME = "file-path";
    private static final String PATH_NAMES_ELEM_NAME = "dss-pathnames";
    private static final String PATH_NAME_ELEM_NAME = "dss-pathname";
    private static final String RIVER_LOC_ELEM_NAME = "River-Location";
    private final List<TimeSeriesContainer> _timeSeriesData = new ArrayList<>();
    private final List<DSSPathname> _dssPathNames = new ArrayList<>();
    private boolean _isUserDefined;
    private Path _dssSourcePath;
    private int _numberOfUserDefinedTempTargets;
    private Path _dssOutputPath;
    private RiverLocation _riverLocation;
    private String _units = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.ENGLISH_ID);

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
        if(_dssSourcePath != null)
        {
            filePathElem.setText(_dssSourcePath.toString());
        }
        myElem.addContent(filePathElem);
        Element dssOutputPathElem = new Element(OUTPUT_DSS_FILE_ELEM_ID);
        if(_dssOutputPath != null)
        {
            dssOutputPathElem.setText(_dssOutputPath.toString());
        }
        myElem.addContent(dssOutputPathElem);
        Element riverLocationElement = new Element(RIVER_LOC_ELEM_NAME);
        if(_riverLocation != null && _riverLocation.getName() != null)
        {
            XMLUtilities.saveNamedType(riverLocationElement, _riverLocation);
        }
        myElem.addContent(riverLocationElement);
        Element dssPathnamesElem = new Element(PATH_NAMES_ELEM_NAME);
        List<DSSPathname> pathnames = getDssPathNames(TemperatureTargetTimeStep.REGULAR_WEEKLY);
        for(DSSPathname pathname : pathnames)
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
        _dssSourcePath = null;
        Element filePathElem = myElem.getChild(FILE_PATH_ELEM_NAME);
        if(filePathElem != null && filePathElem.getText() != null)
        {
            _dssSourcePath = Paths.get(filePathElem.getText());
        }
        Element outputDssFileElem = myElem.getChild(OUTPUT_DSS_FILE_ELEM_ID);
        if(outputDssFileElem != null)
        {
            String filePath = outputDssFileElem.getText();
            if(filePath != null)
            {
                _dssOutputPath = Paths.get(filePath);
            }
        }
        Element riverLocationElem = myElem.getChild(RIVER_LOC_ELEM_NAME);
        if(riverLocationElem != null && !riverLocationElem.getChildren().isEmpty())
        {
            _riverLocation = new RiverLocation();
            XMLUtilities.loadNamedType(riverLocationElem, _riverLocation);
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
        _modified = true;
        return true;
    }

    public List<TimeSeriesContainer> getTimeSeriesData(RunTimeWindow timeWindow)
    {
        if((_isUserDefined && _modified) || _timeSeriesData.isEmpty())
        {
            loadTimeSeriesData(timeWindow);
        }
        return new ArrayList<>(_timeSeriesData);
    }

    private void trimStartDate(RunTimeWindow timeWindow)
    {
        int startYear = timeWindow.getStartTime().year();
        //this is a workaround to odd dss write behavior
        int dayOfMonthToTrimTo = 2;
        if(startYear == 2010)
        {
            dayOfMonthToTrimTo = 3;
        }
        if(startYear == 2020)
        {
            dayOfMonthToTrimTo = 5;
        }
        for(TimeSeriesContainer tsc : _timeSeriesData)
        {
            List<Integer> newTimes = new ArrayList<>();
            List<Double> newValues = new ArrayList<>();
            for (int i = 0; i < tsc.times.length; i++)
            {
                LocalDateTime dateTime = tsc.getHecTime(i).getLocalDateTime();
                if(!newTimes.isEmpty())
                {
                    newValues.add(tsc.values[i]);
                    newTimes.add(tsc.times[i]);
                }
                else if (dateTime.getYear() > startYear
                        || (dateTime.getYear() == startYear && dateTime.getMonth().getValue() > 1)
                        || (dateTime.getYear() == startYear && dateTime.getDayOfMonth() > dayOfMonthToTrimTo)
                        || (dateTime.getYear() == startYear && dateTime.getDayOfMonth() == dayOfMonthToTrimTo && dateTime.getHour() > 1))
                {
                    newTimes.add(tsc.times[i]);
                    newValues.add(tsc.values[i]);
                }
            }
            int[] times = convertListToIntArray(newTimes);
            double[] values = convertListToDoubleArray(newValues);
            tsc.times = times;
            tsc.values = values;
            tsc.numberValues = values.length;
            tsc.startTime = tsc.times[0];
            tsc.startHecTime = tsc.getHecTime(0);
            tsc.endTime = tsc.times[tsc.times.length - 1];
            tsc.endHecTime = tsc.getHecTime(tsc.numberValues - 1);
        }
    }

    private int[] convertListToIntArray(List<Integer> list)
    {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            array[i] = list.get(i);
        }
        return array;
    }

    private double[] convertListToDoubleArray(List<Double> list)
    {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            array[i] = list.get(i);
        }
        return array;
    }

    private void shiftTimeSeriesDataToAnalysisYear(RunTimeWindow timeWindow)
    {
        for(TimeSeriesContainer tsc : _timeSeriesData)
        {
            if (tsc.times.length > 1)
            {
                HecTime startTime = timeWindow.getStartTime();
                HecTime computeTime = new HecTime();
                computeTime.set(tsc.times[1]);
                LocalDate sourceStart = tsc.getStartTime().getLocalDateTime().toLocalDate().withDayOfYear(1);
                LocalDate analysisStart = startTime.getLocalDateTime().toLocalDate().withDayOfYear(1);
                if(tsc.getStartTime().year() == computeTime.year() -1)
                {
                    analysisStart = analysisStart.minusYears(1);
                }
                int diffInMinutes = (int) Duration.between(sourceStart.atStartOfDay(), analysisStart.atStartOfDay()).toMinutes();
                applyShiftToTsc(tsc, diffInMinutes);
                int trimStartYear = timeWindow.getStartTime().year();
                if(!tsc.allMissing())
                {
                    int trimEndYear = trimStartYear;
                    computeTime.set(tsc.times[tsc.times.length-2]);
                    if(tsc.getEndTime().year() == computeTime.year() + 1)
                    {
                        trimEndYear = computeTime.year();
                    }
                    tsc.trimToTime(new HecTime("02Jan"+(trimStartYear), "0100"),new HecTime("07Jan"+(trimEndYear+1), "0100"));
                    trimEnd(tsc, new HecTime("07Jan"+(trimEndYear+1), "0100"));
                }
                tsc.startTime = tsc.times[0];
                tsc.endTime = tsc.times[tsc.times.length-1];
                tsc.startHecTime = tsc.getHecTime(0);
                tsc.endHecTime = tsc.getHecTime(tsc.times.length-1);
            }
        }
    }

    private void trimEnd(TimeSeriesContainer tsc, HecTime trimTo)
    {
        HecTime end = tsc.getHecTime(tsc.times.length - 1);
        if(end.getLocalDateTime().isAfter(trimTo.getLocalDateTime()))
        {
            double[] newVals = new double[tsc.numberValues - 1];
            int[] newTimes = new int[tsc.numberValues -1];
            System.arraycopy(tsc.values, 0, newVals, 0, newVals.length);
            System.arraycopy(tsc.times, 0, newTimes, 0, newTimes.length);
            tsc.values = newVals;
            tsc.times = newTimes;
            tsc.numberValues = newVals.length;
            tsc.startTime = tsc.times[0];
            tsc.endTime = tsc.times[tsc.times.length-1];
            tsc.startHecTime = tsc.getHecTime(0);
            tsc.endHecTime = tsc.getHecTime(tsc.times.length-1);
        }
    }

    public boolean isUserDefined()
    {
        return _isUserDefined;
    }

    public Path getDssSourcePath()
    {
        return _dssSourcePath;
    }

    public void setUserDefined(boolean userDefined)
    {
        _isUserDefined = userDefined;
    }

    public void setDssSourcePath(Path filePath)
    {
        _dssSourcePath = filePath;
    }

    public void setDssPathNames(List<DSSPathname> dssPathNames)
    {
        _dssPathNames.clear();
        if(dssPathNames != null)
        {
            _dssPathNames.addAll(dssPathNames);
        }
    }
    public List<DSSPathname> getDssPathNames()
    {
        return getDssPathNames(null);
    }

    public List<DSSPathname> getDssPathNames(TemperatureTargetTimeStep timeStep)
    {
        List<DSSPathname> retVal = new ArrayList<>(_dssPathNames);
        if(timeStep != null)
        {
            for(DSSPathname pathname : retVal)
            {
                pathname.setEPart(timeStep.toString());
            }
        }
        return retVal;
    }

    private void loadTimeSeriesData(RunTimeWindow timeWindow)
    {
        _timeSeriesData.clear();
        if(_isUserDefined && _dssSourcePath == null)
        {
            for(int i=1; i <= _numberOfUserDefinedTempTargets; i++)
            {
                TimeSeriesContainer fixedTscForUserDefined = buildFixedDataForUserDefined(i, timeWindow);
                _timeSeriesData.add(fixedTscForUserDefined);
            }
        }
        else
        {
            int i=1;
            List<DSSPathname> pathnames = getDssPathNames();
            for(DSSPathname pathname : pathnames)
            {
                TimeSeriesContainer tsc = buildTsFromPathname(i, pathname, timeWindow);
                if(tsc != null)
                {
                    if(tsc.allMissing())
                    {
                        tsc = buildFixedDataForUserDefined(i, timeWindow);
                    }
                    _timeSeriesData.add(tsc);
                }
                i++;
            }
        }
        if(!_isUserDefined && !_timeSeriesData.isEmpty() && _timeSeriesData.get(0).getStartTime().getTimeInMillis() != timeWindow.getStartTime().getTimeInMillis())
        {
            shiftTimeSeriesDataToAnalysisYear(timeWindow);
        }
        if(!_isUserDefined)
        {
            trimStartDate(timeWindow);
        }
    }

    private TimeSeriesContainer buildTsFromPathname(int index, DSSPathname pathname, RunTimeWindow timeWindow)
    {
        DSSIdentifier dssIdentifier = new DSSIdentifier();
        dssIdentifier.setFileName(Project.getCurrentProject().getAbsolutePath(_dssSourcePath.toString()));
        dssIdentifier.setDSSPath(pathname.getPathname());
        TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssIdentifier, false);
        if(tsc != null && isUserDefined())
        {
            Map<Integer, Double> timeValueMap = new HashMap<>();
            for(int i=0; i < tsc.numberValues; i++)
            {
                timeValueMap.put(tsc.times[i], tsc.values[i]);
            }
            TimeSeriesContainer fixedTsc = buildFixedDataForUserDefined(index, timeWindow);
            tsc.times = fixedTsc.times;
            tsc.startTime = fixedTsc.startTime;
            tsc.endTime = fixedTsc.endTime;
            tsc.startHecTime = fixedTsc.startHecTime;
            tsc.endHecTime = fixedTsc.endHecTime;
            tsc.values = fixedTsc.values;
            tsc.numberValues = tsc.values.length;
            for(int i=0; i < tsc.times.length; i++)
            {
                Double value = timeValueMap.get(tsc.times[i]);
                if(value != null)
                {
                    tsc.values[i] = value;
                }
            }
        }
        return tsc;
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

    private TimeSeriesContainer buildFixedDataForUserDefined(int col, RunTimeWindow timeWindow)
    {
        TimeSeriesContainer tsc = buildTemplateUserDefinedTSContainer(col, _units);
        int year = timeWindow.getStartTime().year();
        //this is a workaround to odd dss write behavior
        int dayOfMonthToTrimTo = 2;
        if(year == 2010)
        {
            dayOfMonthToTrimTo = 3;
        }
        if(year == 2020)
        {
            dayOfMonthToTrimTo = 5;
        }
        LocalTime localTime = LocalTime.of(1, 0);
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate startDate = LocalDate.of(year, 1, dayOfMonthToTrimTo); // Start date: January 2nd at 01:00 to workaround dss write bug
        LocalDate endDate = startDate.plusYears(1);
        endDate = endDate.plusWeeks(1);
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
        tsc.startTime = times[0];
        tsc.endTime = times[times.length-1];
        tsc.startHecTime = tsc.getHecTime(0);
        tsc.endHecTime = tsc.getHecTime(tsc.numberValues-1);
        trimEnd(tsc, new HecTime("07Jan" + endDate.getYear(), "0100"));
        tsc.units = _units;
        return tsc;
    }

    private static String getLeadingString(int col)
    {
        String leadingString = "C:00000";
        if(col > 9 && col < 100)
        {
            leadingString = "C:0000";
        }
        else if(col > 99 && col < 1000)
        {
            leadingString = "C:000";
        }
        else if(col > 999 && col < 10000)
        {
            leadingString = "C:00";
        }
        else if(col > 9999 && col < 100_000)
        {
            leadingString = "C:0";
        }
        else if(col > 99999 && col < 1_000_000)
        {
            leadingString = "C:";
        }
        return leadingString;
    }

    public void setNumberOfUserDefinedTempTargets(int value)
    {
        _numberOfUserDefinedTempTargets = value;
    }

    public void setDssOutputPath(Path fileName)
    {
        _dssOutputPath = fileName;
    }
    public Path getDssOutputPath()
    {
        return _dssOutputPath;
    }

    /**
     *
     * @return the f-part string after the | delimiter. For example: "C:000001|TIER 2" will return "TIER 2". If There is no
     * | delimiter, the whole f-part is returned.
     */
    public String getFPartWithoutCollection()
    {
        String retVal = "";
        if(!_dssPathNames.isEmpty())
        {
            retVal = _dssPathNames.get(0).getFPart();
            if(retVal.contains("|"))
            {
                String[] split = retVal.split("\\|");
                if(split.length > 1)
                {
                    retVal = split[1];
                }
            }
        }
        return retVal;
    }

    public static TimeSeriesContainer buildTemplateUserDefinedTSContainer(int col, String units)
    {
        TimeSeriesContainer tsc = new TimeSeriesContainer();
        DSSPathname pathname = new DSSPathname();
        pathname.setBPart("");
        pathname.setCPart("TEMP-WATER-TARGET");
        pathname.setEPart(TemperatureTargetTimeStep.REGULAR_WEEKLY.toString());
        String leadingString = getLeadingString(col);
        pathname.setFPart(leadingString + col + "|USER-DEFINED");
        tsc.fullName = pathname.getPathname().toUpperCase();
        ZoneId dataZoneId = ZoneId.systemDefault();
        tsc.setTimeZoneID(dataZoneId.getId());
        tsc.locationTimezone = dataZoneId.getId();
        tsc.units = units;
        tsc.interval = HecTimeSeriesBase.getIntervalFromEPart(pathname.getEPart());
        tsc.type = "INST-VAL";
        tsc.parameter = pathname.getCPart();
        tsc.location = pathname.bPart();
        tsc.version = pathname.fPart();
        tsc.setStoreAsDoubles(true);
        return tsc;
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
        TemperatureTargetSet that = (TemperatureTargetSet) o;
        return getName().equalsIgnoreCase(that.getName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getName());
    }

    public void setRiverLocation(RiverLocation riverLocation)
    {
        _riverLocation = riverLocation;
    }

    public RiverLocation getRiverLocation()
    {
        return _riverLocation;
    }

    public void setUnits(String units)
    {
        _units = units;
    }

    public String getUnits()
    {
        return _units;
    }
}
