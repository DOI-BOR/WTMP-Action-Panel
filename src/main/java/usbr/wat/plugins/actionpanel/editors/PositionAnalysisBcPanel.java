/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.editors;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;

import hec.heclib.util.HecTime;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import hec.lang.NamedType;
import hec.model.RunTimeWindow;

import usbr.wat.plugins.actionpanel.editors.iterationCompute.IterationBcPanel;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.PositionAnalysisPanel;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class PositionAnalysisBcPanel extends IterationBcPanel
{


	private PositionAnalysisPanel _parentPanel;

	/**
	 * @param editIterationSettingsDialog
	 */
	public PositionAnalysisBcPanel(EditIterationSettingsDialog editor, PositionAnalysisPanel parent)
	{
		super(editor);
		_parentPanel = parent;
	}

	@Override
	protected void addListeners()
	{
		super.addListeners();
		_bcTable.getModel().addTableModelListener(e->tableModelChanged(e));
	}

	/**
	 * @param e
	 * @return
	 */
	private void tableModelChanged(TableModelEvent e)
	{
		if ( e.getColumn() == DSSID_COL)
		{
			calculateMaxElements();
		}
	}

	/**
	 * 
	 */
	private void calculateMaxElements()
	{
		int numRows = _bcTable.getRowCount();
		Object cellObj;
		DSSIdentifier dssId, dssId2;
		HecTime[] times;
		int years;
		RunTimeWindow rtw = new RunTimeWindow();
		
		int maxElement = Integer.MAX_VALUE;
		dssId2 = new DSSIdentifier();
		List<DSSIdentifier>missingDataDssIds = new ArrayList<>();
		for (int r = 0;r < numRows;r ++ )
		{
			cellObj = _bcTable.getValueAt(r, DSSID_COL);
			if ( !(cellObj instanceof DSSIdentifier) )
			{
				continue;
			}
			dssId = (DSSIdentifier) cellObj;
			if ( dssId.getFileName().isEmpty() || dssId.getDSSPath().isEmpty() )
			{
				continue;
			}
			dssId2.setDSSPath(dssId.getDSSPath());
			dssId2.setFileName(Project.getCurrentProject().getAbsolutePath(dssId.getFileName()));
			times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dssId2, 0);
			if ( times != null && times.length == 2 )
			{
				rtw.setStartTime(times[0]);
				rtw.setEndTime(times[1]);
				years = rtw.getNumberOfYears();
				maxElement = Math.min(maxElement, years);
			}
			else // no times....any data?
			{
				TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId2, true);
				if ( tsc == null || tsc.numberValues == 0 )
				{
					DSSIdentifier missingDssId = new DSSIdentifier(dssId2);
					missingDataDssIds.add(missingDssId);
				}
			}
		}
		if ( maxElement < Integer.MAX_VALUE)
		{
			_parentPanel.setMaxElement(maxElement);
		}
		if ( missingDataDssIds.size() > 0 )
		{
			StringBuilder builder = new StringBuilder();
			builder.append("No Data Found for \n:");
			for (int i = 0;i < missingDataDssIds.size(); i++ )
			{
				builder.append(missingDataDssIds.get(i));
				builder.append("\n");
			}
			JOptionPane.showMessageDialog(this, builder.toString(), "Missing Data", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}
	
	@Override
	public void fillPanel(NamedType obj)
	{
		super.fillPanel(obj);
		calculateMaxElements();
	}
}
