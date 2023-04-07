package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

final class TempTargetRowData
{
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private int _time;
    private final Map<Integer, Double> _tempTargetColumnToValueMapInRow = new HashMap<>();

    TempTargetRowData(int time)
    {
        _time = time;
    }

    int getTime()
    {
        return _time;
    }

    Double getValueForTempTargetColumn(Integer column)
    {
        return _tempTargetColumnToValueMapInRow.get(column);
    }

    void setTime(int time)
    {
        _time = time;
    }

    void setValueForTempTargetColumn(Integer temperatureTargetColumn, Double value)
    {
        _tempTargetColumnToValueMapInRow.put(temperatureTargetColumn, value);
    }
}
