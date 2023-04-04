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
	private List<TemperatureTargetSet> _tempTargetSets = new ArrayList<>();
	private InitialConditions _initConditions = new InitialConditions();
	private List<MeteorlogicData> _metData = new ArrayList<>();
	private List<OperationsData> _opsData = new ArrayList<>();
	private List<BcData> _bcData = new ArrayList<>();

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
		loadTempTargetSets(root);
		loadInitialConditions(root);
		loadOpsData(root);
		loadMetData(root);
		loadBcData(root);
		
	}

	private void loadBcData(Element root)
	{
		_bcData.clear();
		Element bcDataElem = root.getChild("BoundaryConditions");
		if ( bcDataElem == null )
		{
			return;
		}
		List kids = bcDataElem.getChildren();
		for (int i = 0;i < kids.size(); i++ )
		{
			Element child = (Element) kids.get(i);
			BcData bcData = new BcData();
			if ( bcData.loadData(child))
			{
				_bcData.add(bcData);
			}

		}
	}

	private void loadOpsData(Element root)
	{
		_opsData.clear();
		Element opsDataElem = root.getChild("Operations");
		if ( opsDataElem == null )
		{
			return;
		}
		List kids = opsDataElem.getChildren();
		for (int i = 0;i < kids.size(); i++ )
		{
			Element child = (Element) kids.get(i);
			OperationsData opsData = new OperationsData();
			if ( opsData.loadData(child))
			{
				_opsData.add(opsData);
			}

		}
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
	private void loadTempTargetSets(Element root)
	{
		Element tempTargetsElem = root.getChild("TemperatureTargetSets");
		if ( tempTargetsElem == null )
		{
			return;
		}
		List kids = tempTargetsElem.getChildren();
		Element child;
		TemperatureTargetSet tt;
		for (int i = 0;i < kids.size(); i++ )
		{
			tt = new TemperatureTargetSet();
			child = (Element) kids.get(i);
			if ( tt.loadData(child))
			{
				_tempTargetSets.add(tt);
			}
		}
	}


	/**
	 *  initial things for loading from a file
	 */
	@Override
	protected void initForLoading()
	{
		_tempTargetSets.clear();
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
		saveOpsData(root);
		saveMetData(root);
		saveBcData(root);
		
	}

	private void saveBcData(Element root)
	{
		Element bcElem = new Element("BoundaryConditions");
		root.addContent(bcElem);
		BcData bcData;
		for (int i = 0;i < _bcData.size(); i++ )
		{
			bcData = _bcData.get(i);
			bcData.saveData(bcElem);
		}
	}

	private void saveOpsData(Element root)
	{
		Element opsElem = new Element("Operations");
		root.addContent(opsElem);
		OperationsData opsData;
		for (int i = 0;i < _opsData.size(); i++ )
		{
			opsData = _opsData.get(i);
			opsData.saveData(opsElem);
		}
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
		Element tempTargetsElem = new Element("TemperatureTargetSets");
		root.addContent(tempTargetsElem);
		TemperatureTargetSet tt;
		for (int i = 0; i < _tempTargetSets.size(); i++ )
		{
			tt = _tempTargetSets.get(i);
			if(tt != null)
			{
				tt.saveData(tempTargetsElem);
			}
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
	
	public void setTemperatureTargetSets(List<TemperatureTargetSet> tt)
	{
		setModified(true);
		_tempTargetSets.clear();
		if ( tt != null )
		{
			_tempTargetSets.addAll(tt);
		}
	}
	
	public List<TemperatureTargetSet> getTemperatureTargetSets()
	{
		return _tempTargetSets;
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


	public List<OperationsData> getOperationsData()
	{
		return _opsData;
	}
	public void setOperationsData(List<OperationsData>opsDataList)
	{
		_opsData.clear();
		if ( opsDataList != null )
		{
			_opsData.addAll(opsDataList);
		}
		setModified(true);
	}

	public List<BcData> getBcData()
	{
		return _bcData;
	}
}
