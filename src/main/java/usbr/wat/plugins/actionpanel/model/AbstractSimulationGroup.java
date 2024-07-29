/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import com.rma.io.RmaFile;
import com.rma.model.AbstractXMLManager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import com.rma.util.XMLUtilities;

import hec2.wat.model.WatAnalysisPeriod;
import hec2.wat.model.WatSimulation;


/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractSimulationGroup extends AbstractXMLManager
{
	public static final String FILE_EXT = "fsimgrp";
	private List<WatSimulation>_sims = new ArrayList<>();
	
	private List<SimulationInfo>_simulationInfo = new ArrayList<>();
	private WatAnalysisPeriod _analysisPeriod;
	private String _apName;

	
	public AbstractSimulationGroup()
	{
		super();
	}
	/**
	 * @return
	 */
	public List<WatSimulation> getSimulations()
	{
		if ( _sims.size() != _simulationInfo.size() )
		{
			_sims.clear();
				
			ManagerProxy proxy;
			for (int i = 0; i < _simulationInfo.size(); i++ )
			{
				SimulationInfo info = _simulationInfo.get(i);
				WatSimulation sim = findSimulation(info);
				if(sim != null)
				{
					_sims.add(sim);
				}
			}
		}
		return Collections.unmodifiableList(_sims);
	}
	/**
	 * @param info
	 * @return
	 */
	public WatSimulation findSimulation(SimulationInfo info)
	{
		WatSimulation sim = null;
		Project project = getProject();
		if(project != null){
			sim = (WatSimulation) project.getManager(info.simName, info.className);
		}
		if ( sim != null )
		{
			try
			{
				sim.readData();
				ManagerProxy proxy = project.getManagerProxy(sim);
				if ( proxy != null )
				{
					proxy.setPinned(true);
				}
			}
			catch ( Exception e )
			{
				System.out.println("findSimulation:Exception loading simulation "+info.simName
						+" Error:"+e);
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			System.out.println(getName()+".getSimulationList:failed to find WatSimulation "
					+info.simName);
		}
		return sim;
	}
	/**
	 * @param ap
	 */
	public void setAnalysisPeriod(WatAnalysisPeriod ap)
	{
		_analysisPeriod = ap;
		if (_analysisPeriod != null )
		{
			_apName = _analysisPeriod.getName();
		}
	}
	
	public WatAnalysisPeriod getAnalysisPeriod()
	{
		if ( _analysisPeriod == null && _apName != null )
		{
			_analysisPeriod = (WatAnalysisPeriod) getProject().getManager(_apName, WatAnalysisPeriod.class);
		}
		return _analysisPeriod;
	}
	public String getAnalysisPeriodName()
	{
		return _apName;
	}

	/**
	 * @param newSim
	 */
	public void addSimulation(WatSimulation newSim)
	{
		if (newSim != null )
		{
			_sims.add(newSim);
			_simulationInfo.add(new SimulationInfo(newSim.getName(), newSim.getClass().getName()));
			setModified(true);
		}
	}
	/**
	 * @param sim
	 * @return
	 */
	public boolean containsSimulation(WatSimulation sim)
	{
		List<WatSimulation> sims = getSimulations();
		return sims.contains(sim);
	}
	/**
	 * @param simToDel
	 */
	public boolean removeSimulation(WatSimulation simToDel)
	{
		getSimulations();
		if ( simToDel != null )
		{
			boolean rv = _sims.remove(simToDel);
			if ( rv )
			{
				String name = simToDel.getName();
				SimulationInfo simInfo;
				Iterator<SimulationInfo> iter = _simulationInfo.iterator();
				while (iter.hasNext() )
				{
					simInfo = iter.next();
					if ( simInfo.simName.equals(name))
					{
						iter.remove();
					}
				}
			}
			return rv;
		}
		return false;
	}
	@Override
	public boolean saveData(RmaFile file)
	{
		if ( file == null )
		{
			System.out.println("SimulationGroup.saveData: No file!");
			return false;
		}
		Element elem = new Element(getSimulationGroupType());
		Document doc = new Document(elem);
		if ( saveData(elem))
		{	
			boolean rv = writeXMLFile(doc, file);
			if ( rv )
			{
				setModified(false);
				return rv;
			}
		}
		return false;
	}
	private boolean saveData(Element elem)
	{
		XMLUtilities.saveNamedType(elem, this);
		if ( _analysisPeriod != null )
		{
			XMLUtilities.saveChildElement(elem, "AnalysisPeriod", _analysisPeriod.getName());
		}
		
		Element simsElem = new Element("Simulations");
		elem.addContent(simsElem);
		WatSimulation sim;
		for(int i = 0;i < _sims.size(); i++ )
		{
			sim = _sims.get(i);
			Element simelem= new Element("Simulation");
			simsElem.addContent(simelem);
			XMLUtilities.saveChildElement(simelem, "Name", sim.getName());
			XMLUtilities.saveChildElement(simelem, "Class", sim.getClass().getName());
			saveSimulationSettings(simelem, sim.getName());
		}
		finishSaving(elem);
		return true;
	}
	/**
	 * @param elem
	 */
	protected abstract void finishSaving(Element elem);
	/**
	 * @param simelem
	 * @param name
	 */
	protected abstract void saveSimulationSettings(Element simelem,
			String name);
	@Override
	protected boolean loadDocument(Document doc)
	{
		if ( doc == null )
		{
			return false;
		}
		Element root = doc.getRootElement();
		if ( root == null )
		{
			System.out.println("loadDocument:no root element");
			return false;
		}
		try
		{
			initForLoading();
			setIgnoreModifiedEvents(true);
			if ( getSimulationGroupType().equals(root.getName()) )
			{
				XMLUtilities.loadNamedType(root, this);
				_apName = XMLUtilities.getChildElementAsString(root, "AnalysisPeriod", null);
				Element simsNode = root.getChild("Simulations");
				if ( simsNode != null )
				{
					List simKidNodes = simsNode.getChildren("Simulation");
					Element simElem;
					for(int i = 0;i < simKidNodes.size(); i++ )
					{
						simElem = (Element) simKidNodes.get(i);
						String simName = XMLUtilities.getChildElementAsString(simElem, "Name", true, null);
						String simClass = XMLUtilities.getChildElementAsString(simElem, "Class", true, null);
						_simulationInfo.add(new SimulationInfo(simName, simClass));
						loadSimulationSettings(simElem, simName);
					}
				}
				finishLoading(root);
			}
		}
		finally
		{
			setIgnoreModifiedEvents(true);
		}
		return true;
	
	}

	/**
	 * @param doc
	 */
	protected abstract void finishLoading(Element root);
	/**
	 * 
	 */
	protected abstract void initForLoading();
	/**
	 * @return
	 */
	protected abstract String getSimulationGroupType();
	/**
	 * @param simElem
	 * @param simName
	 */
	protected abstract void loadSimulationSettings(Element simElem, String simName);



	class SimulationInfo
	{
		public String simName;
		public String className;
		
		public SimulationInfo(String name, String clsName)
		{
			simName = name;
			className = clsName;
		}
	}

}
