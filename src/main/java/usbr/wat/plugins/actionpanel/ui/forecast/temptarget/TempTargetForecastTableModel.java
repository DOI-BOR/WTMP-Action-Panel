/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import rma.swing.table.RmaTableModel;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public final class TempTargetForecastTableModel extends RmaTableModel
{
    private final List<TemperatureTargetSet> _sets = new ArrayList<>();

    public TempTargetForecastTableModel()
    {
        super(new String[]{"Temperature Target Sets"});
    }
    @Override
    public int getRowCount()
    {
        return _sets.size();
    }

    @Override
    public void addRow(Vector newRow)
    {
        Object val = newRow.get(0);
        if(val instanceof TemperatureTargetSet)
        {
            super.addRow(newRow);
            _sets.add((TemperatureTargetSet) val);
        }
    }

    @Override
    public void insertRow(int row, Vector rowData)
    {
        Object val = rowData.get(0);
        if(val instanceof TemperatureTargetSet)
        {
            super.insertRow(row, rowData);
            _sets.add(row, (TemperatureTargetSet) val);
        }
    }

    @Override
    public void deleteRow(int index)
    {
        super.deleteRow(index);
        _sets.remove(index);
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        Object retVal = null;
        if(row >=0 && row < _sets.size() && col >=0 && col < getColumnCount())
        {
            retVal = _sets.get(row);
        }
        return retVal;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col)
    {
        if(row < 0 || row >= _sets.size() || col < 0 || col >= getColumnCount() || aValue == null)
        {
            return;
        }
        if(aValue instanceof TemperatureTargetSet)
        {
            _sets.add(row, (TemperatureTargetSet) aValue);
            _sets.remove(row + 1);
        }
        fireTableDataChanged();
    }

    void updateName(String name, int row)
    {
        if(row >=0 && row < _sets.size())
        {
            _sets.get(row).setName(name);
        }
    }

    public void clearTempTargets()
    {
        _sets.clear();
        fireTableDataChanged();
    }

    void updatedDescription(String desc, int row)
    {
        if(row >=0 && row < _sets.size())
        {
            _sets.get(row).setDescription(desc);
        }
    }
}

