/*
 * Copyright 2024 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import com.rma.event.ProjectManagerListener;
import com.rma.model.Manager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import hec.lang.NamedType;
import hec.model.AbstractSimulation;
import hec2.wat.model.WatAnalysisPeriod;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 *
 */
public class AnalysisPeriodRenameListener
		implements Observer, ProjectManagerListener
{
	private final Project _prj;
	private List<WatAnalysisPeriod>_localApList = new ArrayList<>();
	private List<String>_localApNameList = new ArrayList<>();

	public AnalysisPeriodRenameListener(Project prj)
	{
		super();
		_prj = prj;
		addProjectListener();
		addRenameListeners();
	}

	private void addProjectListener()
	{
		_prj.addManagerListener(this);
	}

	private void addRenameListeners()
	{
		List<WatAnalysisPeriod> apList = _prj.getManagerListForType(WatAnalysisPeriod.class);
		WatAnalysisPeriod ap;
		for (int i = 0;i< apList.size(); i++ )
		{
			ap = apList.get(i);
			addListenerToAp(ap);
		}
	}

	@Override
	public void update(Observable o, Object arg)
	{
		if ( o instanceof WatAnalysisPeriod )
		{
			WatAnalysisPeriod ap = (WatAnalysisPeriod) o;
			if (NamedType.NAME_CHANGED.equals(arg)||NamedType.RENAME_EVENT.equals(arg))
			{
				List<ForecastSimGroup> simGroups = _prj.getManagerListForType(ForecastSimGroup.class);
				updateSimGroups(simGroups, ap);
				List<SimulationGroup> simGroups2 = _prj.getManagerListForType(SimulationGroup.class);
				updateSimGroups(simGroups2, ap);
			}
		}

	}

	private void updateSimGroups(List simGroups, WatAnalysisPeriod renamedAp)
	{
		AbstractSimulationGroup simGroup;
		WatAnalysisPeriod ap;
		int idx = -1;
		boolean found = false;
		for(int i = 0;i < simGroups.size(); i++ )
		{
			simGroup = (AbstractSimulationGroup) simGroups.get(i);
			ap = simGroup.getAnalysisPeriod();
			if ( ap == null )
			{
				String apName = simGroup.getAnalysisPeriodName();
				idx = _localApNameList.indexOf(apName);
				if ( idx > -1 )
				{
					ap = _localApList.get(idx);
					if ( ap != renamedAp  )
					{
						simGroup.setAnalysisPeriod(ap);
						simGroup.setModified(true);
						found = true;
					}
				}
			}
			else if ( ap == renamedAp )
			{
				simGroup.setAnalysisPeriod(ap);
				simGroup.setModified(true);
			}
		}
		if ( idx > -1 && found )
		{
			_localApNameList.set(idx, renamedAp.getName());
		}
	}

	@Override
	public void managerAdded(ManagerProxy managerProxy)
	{
		Manager mgr = managerProxy.getManager();
		if ( mgr instanceof WatAnalysisPeriod )
		{
			WatAnalysisPeriod ap = (WatAnalysisPeriod) mgr;
			if ( !_localApList.contains(ap) )
			{
				addListenerToAp(ap);
			}
		}
	}

	private void addListenerToAp(WatAnalysisPeriod ap)
	{
		ap.addObserver(this);
		_localApList.add(ap);
		_localApNameList.add(ap.getName());
	}

	@Override
	public void managerDeleted(ManagerProxy managerProxy)
	{
		Manager mgr = managerProxy.getManager();
		mgr.deleteObserver(this);
		int idx = _localApList.indexOf(mgr);
		if ( idx > -1 )
		{
			_localApList.remove(idx);
			_localApNameList.remove(idx);
		}
	}

	@Override
	public Class getManagerClass()
	{
		return WatAnalysisPeriod.class;
	}

	public void stopListening()
	{
		WatAnalysisPeriod ap;
		for (int i = 0;i < _localApList.size(); i++ )
		{
			ap = _localApList.get(i);
			ap.deleteObserver(this);
		}
		Project.getCurrentProject().removeManagerListener(this);
		_localApList.clear();
		_localApNameList.clear();
	}
}
