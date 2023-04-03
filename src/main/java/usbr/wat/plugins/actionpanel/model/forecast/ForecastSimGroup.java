/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;

/**
 * @author mark
 *
 */
public class ForecastSimGroup extends AbstractSimulationGroup
{
	private List<TemperatureTarget> _tempTargets = new ArrayList<>();
	private InitialConditions _initConditions = new InitialConditions();
	private List<MeteorlogicData> _metData = new ArrayList<>();

	public ForecastSimGroup()
	{
		super();
	}
	
	
	/**
	 * 
	 * called late in the loading process to load data that's not simulation specific
	 * @param root
	 */
	@Override
	protected void finishLoading(Element root)
	{
		loadTempTargets(root);
		loadInitialConditions(root);
		loadMetData(root);
		
	}

	private void loadMetData(Element root)
	{
		_metData.clear();
		Element metDataElem = root.getChild("Meteorology");
		if ( metDataElem == null )
		{
			return;
		}
		List kids = metDataElem.getChildren();
		for (int i = 0;i < kids.size(); i++ )
		{
			Element child = (Element) kids.get(i);
			MeteorlogicData metData = new MeteorlogicData();
			if ( metData.loadData(child))
			{
				_metData.add(metData);
			}

		}
	}

	/**
	 * @param root
	 */
	private void loadInitialConditions(Element root)
	{
		Element icElem = root.getChild("InitialConditions");
		if ( icElem == null )
		{
			return;
		}
		List kids = icElem.getChildren();
		Element child;
		InitialConditions ic = new InitialConditions();
		ic.loadData(icElem);
		_initConditions = ic;
	}


	/**
	 * @param root
	 */
	private void loadTempTargets(Element root)
	{
		Element tempTargetsElem = root.getChild("TemperatureTargets");
		if ( tempTargetsElem == null )
		{
			return;
		}
		List kids = tempTargetsElem.getChildren();
		Element child;
		TemperatureTarget tt;
		for (int i = 0;i < kids.size(); i++ )
		{
			tt = new TemperatureTarget();
			child = (Element) kids.get(i);
			if ( tt.loadData(child))
			{
				_tempTargets.add(tt);
			}
		}
		
	}

	@Override
	protected boolean loadDocument(Document doc)
	{
		return super.loadDocument(doc);
	}

	/**
	 *  initial things for loading from a file
	 */
	@Override
	protected void initForLoading()
	{
		_tempTargets.clear();
	}
	/**
	 * @return
	 */
	@Override
	protected String getSimulationGroupType()
	{
		return "ForecastSimulationGroup";
	}
	/**
	 * loads settings for specific simulations
	 * @param simElem
	 * @param simName
	 */
	@Override
	protected void loadSimulationSettings(Element simElem, String simName)
	{
		
	}

	/**
	 * called late in the saving process to save data that's not simulation specific
	 */
	@Override
	protected void finishSaving(Element root)
	{
		saveTempTargets(root);
		saveInitialConditions(root);
		saveMetData(root);
		
	}

	private void saveMetData(Element root)
	{
		Element metElem = new Element("Meteorology");
		root.addContent(metElem);
		MeteorlogicData metData;
		for (int i = 0;i < _metData.size(); i++ )
		{
			metData = _metData.get(i);
			metData.saveData(metElem);
		}

	}


	/**
	 * @param root
	 */
	private void saveInitialConditions(Element root)
	{
		if (_initConditions != null )
		{
			Element icElem = new Element("InitialConditions");
			root.addContent(icElem);
			_initConditions.saveData(icElem);
		}
	}


	/**
	 * @param root
	 */
	private void saveTempTargets(Element root)
	{
		Element tempTargetsElem = new Element("TemperatureTargets");
		root.addContent(tempTargetsElem);
		TemperatureTarget tt;
		for (int i = 0;i < _tempTargets.size(); i++ )
		{
			tt = _tempTargets.get(i);
			tt.saveData(tempTargetsElem);
		}
		
	}

	/**
	 * saves settings for specific simulations
	 */
	@Override
	protected void saveSimulationSettings(Element simelem, String simName)
	{
		
	}


	/**
	 * @param ics
	 */
	public void setInitialConditions(InitialConditions ics)
	{
		setModified(true);
		_initConditions = ics;
	}
	
	public InitialConditions getInitialConditions()
	{
		return _initConditions;
	}
	
	public void setTemperaturTargets(List<TemperatureTarget> tt)
	{
		setModified(true);
		_tempTargets.clear();
		if ( tt != null )
		{
			_tempTargets.addAll(tt);
		}
	}
	
	public List<TemperatureTarget> getTemperaturTargets()
	{
		return _tempTargets;
	}

	public List<MeteorlogicData> getMeteorlogyData()
	{
		return _metData;
	}

	public void setMeteorlogyData(List<MeteorlogicData>metDataList)
	{
		_metData.clear();
		if ( metDataList != null )
		{
			_metData.addAll(metDataList);
		}
		setModified(true);
	}



}
