/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import rma.swing.table.RmaTableModel;
import rma.util.RMAConst;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

final class TempTargetTableModel extends RmaTableModel
{
    static final int DATE_COL_INDEX = 0;
    private static final int YEAR_DATE_MONTH_STYLE = -13;
    private final List<TempTargetRowData> _rowDataList = new ArrayList<>();
    HecTime _hecTime = new HecTime();

    @Override
    public void clearAll()
    {
        super.clearAll();
        _rowDataList.clear();
    }

    public TempTargetRowData getTempTargetRowData(int row)
    {
        return _rowDataList.get(row);
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        Object retVal;
        if(row < 0 || col < 0 || row >= _rowDataList.size() || col >= getColumnCount())
        {
            return null;
        }
        TempTargetRowData rowData = _rowDataList.get(row);
        if(col == DATE_COL_INDEX)
        {
            retVal = getNormalizedDisplayDateForTime(rowData.getTime());
        }
        else
        {
            retVal = null;
            Double val = rowData.getValueForTempTargetColumn(col);
            if(val != null && val != RMAConst.HEC_UNDEFINED_DOUBLE)
            {
                retVal = roundToNDigits(val, 1);
            }
        }
        return retVal;
    }

    private String getNormalizedDisplayDateForTime(int time)
    {
        _hecTime.set(time);
        return _hecTime.date(YEAR_DATE_MONTH_STYLE);
    }

    private double roundToNDigits(double input, int n)
    {
        double powerOf10 = Math.pow(10, n);
        return Math.round(input * powerOf10) / powerOf10;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col)
    {
        if(row < 0 || col < 0 || row >= _rowDataList.size() || col >= getColumnCount())
        {
            return;
        }
        TempTargetRowData rowData = _rowDataList.get(row);
        if(col == DATE_COL_INDEX)
        {
            int time = RMAConst.HEC_UNDEFINED_INT;
            if(aValue != null && !aValue.toString().isEmpty())
            {
                LocalDate localDate = parseLocalDateString(aValue.toString());
                _hecTime.set(aValue.toString());
                LocalTime localTime = LocalTime.of(0,1);
                ZoneId zoneId = ZoneId.systemDefault();
                time = HecTime.fromZonedDateTime(ZonedDateTime.of(localDate,localTime, zoneId)).value();
            }
            rowData.setTime(time);
        }
        else
        {
            if(aValue == null)
            {
                aValue = RMAConst.HEC_UNDEFINED_DOUBLE;
            }
            rowData.setValueForTempTargetColumn(col, parseDouble(aValue.toString()));
        }
    }

    @Override
    public int getRowCount()
    {
        return _rowDataList.size();
    }

    private LocalDate parseLocalDateString(String dateString)
    {
        LocalDate retVal = null;
        if(!dateString.trim().isEmpty())
        {
            retVal = LocalDate.parse(dateString, TempTargetRowData.DATE_FORMATTER);
        }
        return retVal;
    }

    private Double parseDouble(Object obj)
    {
        Double retVal = null;
        if(obj instanceof Double)
        {
            retVal = (Double) obj;
        }
        else if(obj != null && !obj.toString().trim().isEmpty())
        {
            retVal = Double.parseDouble(obj.toString());
        }
        return retVal;
    }

    void setTempTargetSet(TemperatureTargetSet tempTargetSet, ForecastSimGroup fsg)
    {
        _rowDataList.clear();
        List<TimeSeriesContainer> tempTargets = tempTargetSet.getTimeSeriesData(fsg.getAnalysisPeriod().getRunTimeWindow());
        int column = 1;
        initializeRowsWithTimes(tempTargets); //note this is assuming temp targets time series are all using the same times for a given set
        //if that is not a good assumption will need to use an efficient algo for determining if a row has been created for a give time
        for(TimeSeriesContainer tempTargetTimeSeries : tempTargets)
        {
            int columnForTimeSeries = column;
            //add in row values for each temp target in corresponding temp target column
            if(tempTargetTimeSeries != null && tempTargetTimeSeries.times != null)
            {
                for(int i=0; i < tempTargetTimeSeries.times.length; i++)
                {
                    int time = tempTargetTimeSeries.times[i];
                    Double value = tempTargetTimeSeries.values[i];
                    TempTargetRowData foundRowData = findRowDataByTime(time);
                    if(foundRowData != null)
                    {
                        foundRowData.setValueForTempTargetColumn(columnForTimeSeries, value);
                    }
                }
                column++;
            }
        }
    }

    private void initializeRowsWithTimes(List<TimeSeriesContainer> tempTargets)
    {
        if(!tempTargets.isEmpty())
        {
            //initialize rowData using dates
            TimeSeriesContainer tempTargetTimeSeries = tempTargets.get(0);
            if(tempTargetTimeSeries.times != null)
            {
                for (int time : tempTargetTimeSeries.times)
                {
                    _rowDataList.add(new TempTargetRowData(time));
                }
            }
        }
    }

    private TempTargetRowData findRowDataByTime(int time)
    {
        TempTargetRowData retVal = null;
        for(TempTargetRowData rowData : _rowDataList)
        {
            if(rowData.getTime() == time)
            {
                retVal = rowData;
                break;
            }
        }
        return retVal;
    }

}
