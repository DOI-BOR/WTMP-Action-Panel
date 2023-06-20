/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.rma.util.XMLUtilities;
import hec2.wat.model.WatSimulation;
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
	private Map<String, List<EnsembleSet>> _ensembleSets = new HashMap<>();
	/** simulation/ensemble set to what its F-Part collection range is */
	private Map<String, Map<String, int[]>> _ensembleSetIndexing = new HashMap<>();


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
		loadEnsembleSets(root);

		
	}

	private void loadEnsembleSets(Element root)
	{
		_ensembleSets.clear();
		Element esetsElem = root.getChild("EnsembleSets");
		if ( esetsElem == null )
		{
			return;
		}
		List simKids = esetsElem.getChildren();
		EnsembleSet eset;
		String simName;
		for (int i = 0;i < simKids.size(); i++ )
		{
			Element simChild = (Element) simKids.get(i);
			if ( !"Simulation".equals(simChild.getName()))
			{
				continue;
			}
			simName = simChild.getTextTrim();
			List<EnsembleSet>ensembleSets = new ArrayList<>();
			List kids = simChild.getChildren();
			for (int k = 0; k < kids.size(); k++ )
			{
				Element child = (Element) kids.get(k);
				eset = new EnsembleSet();
				if (eset.loadData(child))
				{
					ensembleSets.add(eset);
				}
			}
			_ensembleSets.put(simName, ensembleSets);
			BcData bcData;
			for (int e = 0;e < ensembleSets.size();e++ )
			{
				eset = ensembleSets.get(e);
				String bcDataName = eset.getBcDataName();
				bcData = getBcData(bcDataName);
				eset.setSelectedBcData(bcData);

				String ttsName = eset.getTemperatureTargetSetName();
				TemperatureTargetSet ttset = getTemperatureTargetSet(ttsName);
				eset.setSelectedTemperatureTargetSets(ttset);
			}
		}
		loadEnsembleSetsIndexing(root);
	}
	private void loadEnsembleSetsIndexing(Element root)
	{
		_ensembleSetIndexing.clear();
		Element esiElem = root.getChild("EnsembleSetIndexing");
		if ( esiElem == null )
		{
			return;
		}
		List esiKids = esiElem.getChildren("Simulation");
		for (int i = 0; i < esiKids.size(); i++ )
		{
			Element simElem = (Element) esiKids.get(i);
			String simname = simElem.getAttributeValue("Name");
			Map<String, int[]>esetMap = new HashMap<>();
			_ensembleSetIndexing.put(simname, esetMap);
			List simKids = simElem.getChildren("EnsembleSet");
			for (int s = 0; s < simKids.size(); s++ )
			{
				Element esetElem = (Element) simKids.get(s);
				String esetName = esetElem.getAttributeValue("Name");
				int[] indexes = new int[2];
				int start = XMLUtilities.getChildElementAsInt(esetElem, "CollectionStart", -1);
				int end = XMLUtilities.getChildElementAsInt(esetElem, "CollectionEnd", -1);
				indexes[0] = start;
				indexes[1] = end;
				if ( start > -1 && end > -1 )
				{
					esetMap.put(esetName, indexes);
				}
			}
		}
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
		BcData bcData;
		for (int i = 0;i < _bcData.size();i++ )
		{
			bcData = _bcData.get(i);
			String metDataName = bcData.getMetDataName();
			MeteorlogicData metData = getMetData(metDataName);
			bcData.setSelectedMet(metData);

			String opsDataName = bcData.getOpsDataName();
			OperationsData opsData = getOpsData(opsDataName);
			bcData.setSelectedOps(opsData);
		}
	}

	private OperationsData getOpsData(String opsDataName)
	{
		if ( opsDataName == null )
		{
			return null;
		}
		OperationsData opsData;
		for (int i = 0;i < _opsData.size(); i++ )
		{
			opsData = _opsData.get(i);
			if ( opsDataName.equals(opsData.getName()))
			{
				return opsData;
			}
		}
		return null;
	}
	public BcData getBcData(String bcDataName)
	{
		if ( bcDataName == null )
		{
			return null;
		}
		BcData bcData;
		for (int i = 0;i < _bcData.size(); i++ )
		{
			bcData = _bcData.get(i);
			if ( bcDataName.equals(bcData.getName()))
			{
				return bcData;
			}
		}
		return null;
	}

	public MeteorlogicData getMetData(String metDataName)
	{
		if ( metDataName == null )
		{
			return null;
		}
		MeteorlogicData metData;
		for (int i = 0;i < _metData.size(); i++ )
		{
			metData = _metData.get(i);
			if ( metDataName.equals(metData.getName()))
			{
				return metData;
			}
		}
		return null;
	}

	public TemperatureTargetSet getTemperatureTargetSet(String ttsName)
	{
		if ( ttsName == null )
		{
			return null;
		}
		TemperatureTargetSet tts;
		for (int i = 0;i < _tempTargetSets.size(); i++ )
		{
			tts = _tempTargetSets.get(i);
			if ( ttsName.equals(tts.getName()))
			{
				return tts;
			}
		}
		return null;
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
		saveEnsembleSets(root);
		
	}

	private void saveEnsembleSets(Element root)
	{
		Element ecElem = new Element("EnsembleSets");
		root.addContent(ecElem);
		EnsembleSet eSet;
		Set<Map.Entry<String, List<EnsembleSet>>> entrySet = _ensembleSets.entrySet();
		Iterator<Map.Entry<String, List<EnsembleSet>>> iter = entrySet.iterator();
		while (iter.hasNext())
		{
			Map.Entry<String, List<EnsembleSet>> next = iter.next();
			String simName = next.getKey();
			Element simElem = new Element("Simulation");
			simElem.setText(simName);
			ecElem.addContent(simElem);
			List<EnsembleSet> ensembleSets = next.getValue();
			for (int i = 0; i < ensembleSets.size(); i++)
			{
				eSet = ensembleSets.get(i);
				eSet.saveData(simElem);
			}
			System.out.println("simElem has "+ simElem.getChildren("EnsembleSet").size() + " ensembleSets");
		}
		saveEnsembleSetsIndexing(root);
	}
	private void saveEnsembleSetsIndexing(Element root)
	{
		Element esiElem = new Element("EnsembleSetIndexing");
		root.addContent(esiElem);

		Set<Map.Entry<String, Map<String, int[]>>> entrySet2 = _ensembleSetIndexing.entrySet();
		Iterator<Map.Entry<String, Map<String, int[]>>> iter2 = entrySet2.iterator();
		while (iter2.hasNext())
		{
			Map.Entry<String, Map<String, int[]>> simEsetMap = iter2.next();
			String simName = simEsetMap.getKey();
			Element simElem = new Element("Simulation");
			simElem.setAttribute("Name", simName);
			esiElem.addContent(simElem);
			Map<String, int[]> esetMap = simEsetMap.getValue();
			Set<Map.Entry<String, int[]>> esetSet = esetMap.entrySet();
			Iterator<Map.Entry<String, int[]>> esetIter = esetSet.iterator();
			while (esetIter.hasNext())
			{
				Map.Entry<String, int[]> esetEntry = esetIter.next();
				String esetName = esetEntry.getKey();
				int[] esetIndexes = esetEntry.getValue();
				Element esetElem  = new Element("EnsembleSet");
				esetElem.setAttribute("Name", esetName);
				simElem.addContent(esetElem);
				Element startElem = new Element("CollectionStart");
				startElem.setText(String.valueOf(esetIndexes[0]));
				esetElem.addContent(startElem);
				Element endElem = new Element("CollectionEnd");
				endElem.setText(String.valueOf(esetIndexes[1]));
				esetElem.addContent(endElem);
			}

		}
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

	public void setEnsembleSets(WatSimulation sim, List<EnsembleSet> ensembleSets)
	{
		List<EnsembleSet> currentEnsembleSets = _ensembleSets.get(sim.getName());
		if ( currentEnsembleSets == null )
		{
			List<EnsembleSet>sets = new ArrayList<>();
			sets.addAll(ensembleSets);
			_ensembleSets.put(sim.getName(), sets);
		}
		else
		{
			currentEnsembleSets.clear();
			currentEnsembleSets.addAll(ensembleSets);
		}
		setModified(true);
	}
	public List<EnsembleSet>getEnsembleSets(WatSimulation sim)
	{
		if ( sim == null )
		{
			return new ArrayList<>();
		}
		List<EnsembleSet> ensembleSets = _ensembleSets.get(sim.getName());
		if ( ensembleSets == null )
		{
			return new ArrayList<>();
		}
		return ensembleSets;
	}

	public boolean hasEnsembleSetFor(WatSimulation sim , BcData bc, TemperatureTargetSet tts)
	{
		if ( bc == null || tts == null )
		{
			return false;
		}
		EnsembleSet eset = getEnsembleSetFor(sim, bc, tts);
		return eset != null ;
	}

	public EnsembleSet getEnsembleSetFor(WatSimulation sim, BcData bc, TemperatureTargetSet tts)
	{
		if ( sim == null || bc == null || tts == null )
		{
			return null;
		}
		EnsembleSet eset;
		List<EnsembleSet>ensembleSets = _ensembleSets.get(sim.getName());
		if (ensembleSets == null )
		{
			return null;
		}
		for (int e = 0; e < ensembleSets.size(); e++ )
		{
			eset = ensembleSets.get(e);
			if ( eset.getBcData() == bc && eset.getTemperatureTargetSet() == tts )
			{
				return eset;
			}
		}
		return null;
	}

	private void deleteEnsembleSetsFor(TemperatureTargetSet temperatureTargetSet)
	{
		List<EnsembleSet> esetsToRemove = getEnsembleSetsUsingTempTargetSet(temperatureTargetSet);
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet())
		{
			List<EnsembleSet> esets = entry.getValue();
			esets.removeIf(esetsToRemove::contains);
		}
	}

	private void deleteEnsembleSetsFor(BcData bcData)
	{
		List<EnsembleSet> esetsToRemove = getEnsembleSetsUsingBcData(bcData);
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet())
		{
			List<EnsembleSet> esets = entry.getValue();
			esets.removeIf(esetsToRemove::contains);
		}
	}

	public List<EnsembleSet> getEnsembleSetsUsingTempTargetSet(TemperatureTargetSet temperatureTargetSet)
	{
		LinkedHashSet<EnsembleSet> eSetsUsingTTSet = new LinkedHashSet<>();
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet())
		{
			List<EnsembleSet> esets = entry.getValue();
			for(EnsembleSet eset : esets)
			{
				if(Objects.equals(eset.getTemperatureTargetSet(), temperatureTargetSet))
				{
					eSetsUsingTTSet.add(eset);
				}
			}
		}
		return new ArrayList<>(eSetsUsingTTSet);
	}


	public boolean deleteEnsembleSet(WatSimulation sim, EnsembleSet eset)
	{
		if ( eset == null )
		{
			return false;
		}
		List<EnsembleSet>ensembleSets = _ensembleSets.get(sim.getName());
		if (ensembleSets == null )
		{
			return false;
		}
		boolean rv = ensembleSets.remove(eset);
		if ( rv )
		{
			setModified(true);
		}
		return rv;
	}

	public boolean addEnsembleSet(WatSimulation sim, EnsembleSet ensembleSet)
	{
		if (ensembleSet == null)
		{
			return false;
		}
		EnsembleSet existingESet = getEnsembleSetFor(sim, ensembleSet.getBcData(), ensembleSet.getTemperatureTargetSet());
		if (existingESet != null)
		{
			return false;
		}
		List<EnsembleSet> ensembleSets = _ensembleSets.get(sim.getName());
		if (ensembleSets == null)
		{
			ensembleSets = new ArrayList<>();
			_ensembleSets.put(sim.getName(), ensembleSets);
		}
		ensembleSets.add(ensembleSet);
		Map<String, int[]> ensembleIndexingMap = _ensembleSetIndexing.get(sim.getName());
		int[] indexing = getEnsembleSetCollectionIndexing(sim, ensembleSet);

		setModified(true);
		return true;
	}
	public int[] getEnsembleSetCollectionIndexing(WatSimulation sim, EnsembleSet ensembleSet)
	{
		Map<String, int[]> ensembleIndexingMap = _ensembleSetIndexing.get(sim.getName());
		int[] indexing;
		if (ensembleIndexingMap == null)
		{
			indexing = getNextCollectionIndexing(sim);
			ensembleIndexingMap = new HashMap<>();
			ensembleIndexingMap.put(ensembleSet.getName(), indexing);
			_ensembleSetIndexing.put(sim.getName(), ensembleIndexingMap);
			setModified(true);
		}
		else
		{
			indexing = ensembleIndexingMap.get(ensembleSet.getName());
			if ( indexing == null )
			{
				indexing = getNextCollectionIndexing(sim);
				ensembleIndexingMap.put(ensembleSet.getName(), indexing);
				setModified(true);
			}
		}
		return indexing;
	}

	private int[] getNextCollectionIndexing(WatSimulation simulation)
	{
		Map<String, int[]> ensembleIndexMap = _ensembleSetIndexing.get(simulation.getName());
		if ( ensembleIndexMap == null )
		{
			Integer max = Integer.getInteger("Forecast.EnsembleSetRange", 500);
			return new int []{0,max.intValue()-1};
		}
		Collection<int[]> values = ensembleIndexMap.values();
		Iterator<int[]> iter = values.iterator();
		int max = -1;
		while (iter.hasNext())
		{
			int[] indexes = iter.next();
			max = Math.max(indexes[1], max);
		}
		int[] indexs = new int[2];
		indexs[0] = max+1;
		indexs[1] = indexs[0]+ Integer.getInteger("Forecast.EnsembleSetRange", 500);
		indexs[1]--;
		return indexs;
	}

	public Map<String, int[]>getSimulationEnsembleSetIndexing(WatSimulation simulation)
	{
		Map<String, int[]> ensembleIndexMap = _ensembleSetIndexing.get(simulation.getName());
		return ensembleIndexMap;
	}



	public List<EnsembleSet> getEnsembleSetsFor(WatSimulation simulation)
	{
		return _ensembleSets.get(simulation.getName());
	}

	public void removeTemperatureTargetSet(TemperatureTargetSet set)
	{
		if(_tempTargetSets.remove(set))
		{
			deleteEnsembleSetsFor(set);
		}
	}

	public void removeOperationsData(OperationsData operationsData)
	{
		if(_opsData.remove(operationsData))
		{
			List<BcData> bcDataUsingOpsData = getBcDataUsingOperationsData(operationsData);
			for(BcData bcDataToRemove : bcDataUsingOpsData)
			{
				removeBcData(bcDataToRemove);
			}
		}
	}

	public void removeMetData(MeteorlogicData meteorlogicData)
	{
		if(_metData.remove(meteorlogicData))
		{
			List<BcData> bcDataUsingOpsData = getBcDataUsingMetData(meteorlogicData);
			for(BcData bcDataToRemove : bcDataUsingOpsData)
			{
				removeBcData(bcDataToRemove);
			}
		}
	}

	public void removeBcData(BcData bcData)
	{
		if(_bcData.remove(bcData))
		{
			deleteEnsembleSetsFor(bcData);
		}
	}

	public List<BcData> getBcDataUsingOperationsData(OperationsData operationsData)
	{
		List<BcData> retVal = new ArrayList<>();
		for(BcData bc : _bcData)
		{
			if(bc.getOperationsData() == operationsData)
			{
				retVal.add(bc);
			}
		}
		return retVal;
	}

	public List<BcData> getBcDataUsingMetData(MeteorlogicData metData)
	{
		List<BcData> retVal = new ArrayList<>();
		for(BcData bc : _bcData)
		{
			if(bc.getMeteorogicalData() == metData)
			{
				retVal.add(bc);
			}
		}
		return retVal;
	}

	public List<EnsembleSet> getEnsembleSetsUsingBcData(BcData bcData)
	{
		LinkedHashSet<EnsembleSet> eSetsUsingTTSet = new LinkedHashSet<>();
		for (Map.Entry<String, List<EnsembleSet>> entry : _ensembleSets.entrySet())
		{
			List<EnsembleSet> esets = entry.getValue();
			for(EnsembleSet eset : esets)
			{
				if(Objects.equals(eset.getBcData(), bcData))
				{
					eSetsUsingTTSet.add(eset);
				}
			}
		}
		return new ArrayList<>(eSetsUsingTTSet);
	}
}
