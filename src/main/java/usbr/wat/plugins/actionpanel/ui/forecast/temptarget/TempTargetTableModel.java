package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import rma.swing.table.RmaTableModel;
import rma.util.RMAConst;
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
        TempTargetRowData rowData = _rowDataList.get(row);
        if(col == DATE_COL_INDEX)
        {
            _hecTime.set(rowData.getTime());
            LocalDate date = _hecTime.getLocalDateTime().toLocalDate();
            retVal = date.toString();
        }
        else
        {
            retVal = null;
            Double val = rowData.getValueForTempTargetColumn(col);
            if(val != null && val != RMAConst.HEC_UNDEFINED_DOUBLE)
            {
                retVal = roundToNDigits(val, 6);
            }
        }
        return retVal;
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

    void setTempTargetSet(TemperatureTargetSet tempTargetSet)
    {
        _rowDataList.clear();
        List<TimeSeriesContainer> tempTargets = tempTargetSet.getTimeSeriesData();
        int column = 1;
        for(TimeSeriesContainer tempTargetTimeSeries : tempTargets)
        {
            //initialize rowData using dates
            if(tempTargetTimeSeries.getTimes() != null)
            {
                for(int time : tempTargetTimeSeries.times)
                {
                    if(_rowDataList.stream().noneMatch(dataForRow -> dataForRow.getTime() == time))
                    {
                        _rowDataList.add(new TempTargetRowData(time));
                    }
                }
                int columnForTimeSeries = column;
                //add in row values for each temp target in corresponding temp target column
                for(int i=0; i < tempTargetTimeSeries.times.length; i++)
                {
                    int time = tempTargetTimeSeries.times[i];
                    Double value = tempTargetTimeSeries.values[i];
                    _rowDataList.stream()
                            .filter(dataForRow -> dataForRow.getTime() == time)
                            .findFirst()
                            .ifPresent(rowData -> rowData.setValueForTempTargetColumn(columnForTimeSeries, value));
                }
                column++;
            }
        }
    }

}
