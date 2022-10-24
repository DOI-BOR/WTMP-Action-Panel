
package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * class that holds a group of Simulations
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SimulationGroup extends AbstractXMLManager
{
	public static final String FILE_EXT = "simgrp";
	
	private List<WatSimulation>_sims = new ArrayList<>();
	private List<SimulationInfo>_simulationInfo = new ArrayList<>();

	private WatAnalysisPeriod _analysisPeriod;

	private String _apName;
	
	private Map<String, IterationSettings>_iterationsSettings = new HashMap<>();
	private Map<String, PositionAnalysisSettings>_positionAnalysisSettings = new HashMap<>();
	
	private Map<String, ComputeType>_computeTypeSettings = new HashMap();

	public SimulationGroup()
	{
		super();
	}
	@Override
	public boolean saveData(RmaFile file)
	{
		if ( file == null )
		{
			System.out.println("SimulationGroup.saveData: No file!");
			return false;
		}
		Element elem = new Element("SimulationGroup");
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
	
	/**
	 * @param elem
	 * @return
	 */
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
			saveIterationSettings(simelem, sim.getName());
			savePositionAnalysisSettings(simelem, sim.getName());
		}
		saveComputeTypes(elem);
		return true;
	}
	private void saveComputeTypes(Element elem)
	{
		Set<Entry<String, ComputeType>> computeTypes = _computeTypeSettings.entrySet();
		Element computeTypesElem = new Element("ComputeTypes");
		elem.addContent(computeTypesElem);
		Iterator<Entry<String, ComputeType>> iter = computeTypes.iterator();
		Element simElem;
		while (iter.hasNext())
		{
			Entry<String, ComputeType> next = iter.next();
			simElem = XMLUtilities.saveChildElement(computeTypesElem, "Simulation", next.getKey());
			XMLUtilities.saveChildElement(simElem, "ComputeType", next.getValue().name());
		}
		
	}
	/**
	 * @param root
	 */
	private void loadComputeTypes(Element root)
	{
		Element computeTypesElem = root.getChild("ComputeTypes");
		if (computeTypesElem == null )
		{
			return;
		}
		List kids = computeTypesElem.getChildren("Simulation");
		Element simElem, ctElem;
		for (int i = 0;i < kids.size(); i++ )
		{
			simElem = (Element) kids.get(i);
			ctElem = simElem.getChild("ComputeType");
			if ( ctElem != null )
			{
				_computeTypeSettings.put(simElem.getTextTrim(), ComputeType.valueOf(ctElem.getTextTrim()));
			}
		}
	}
	/**
	 * @param simelem
	 * @param name
	 */
	private void savePositionAnalysisSettings(Element simElem, String simName)
	{
		PositionAnalysisSettings settings = _positionAnalysisSettings.get(simName);
		if ( settings == null )
		{
			return;
		}
		Element paElem = new Element("PositionAnalysisSettings");
		simElem.addContent(paElem);
		
		settings.saveData(paElem);
	}
	/**
	 * 
	 */
	private void saveIterationSettings(Element simElem, String simName)
	{
		IterationSettings settings = _iterationsSettings.get(simName);
		if ( settings == null )
		{
			return;
		}
		Element iterElem = new Element("IterationSettings");
		simElem.addContent(iterElem);
		
		settings.saveData(iterElem);
		
	}
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
			setIgnoreModifiedEvents(true);
			_iterationsSettings.clear();
			_positionAnalysisSettings.clear();
			_computeTypeSettings.clear();
			if ( "SimulationGroup".equals(root.getName()) )
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
						loadIterationSettings(simElem, simName);
						loadPositionAnalysisSettings(simElem, simName);
					}
				}
				loadComputeTypes(root);
			}

		}
		finally
		{
			
			setIgnoreModifiedEvents(false);
		}
		return true;
		
	}

	
	/**
	 * @param simElem
	 * @param simName
	 */
	private void loadPositionAnalysisSettings(Element simElem, String simName)
	{
		Element iterElem = simElem.getChild("PositionAnalysisSettings");
		if ( iterElem == null )
		{
			return;
		}
		PositionAnalysisSettings settings = new PositionAnalysisSettings();
		settings.loadData(iterElem);
		
		_positionAnalysisSettings.put(simName, settings);
	}
	/**
	 * @param simElem
	 */
	private void loadIterationSettings(Element simElem, String simName)
	{
		Element iterElem = simElem.getChild("IterationSettings");
		if ( iterElem == null )
		{
			return;
		}
		IterationSettings settings = new IterationSettings();
		settings.loadData(iterElem);
		
		_iterationsSettings.put(simName, settings);
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
	/**
	 * @param name
	 * @return
	 */
	public IterationSettings getIterationSettings(String simName)
	{
		IterationSettings settings = _iterationsSettings.get(simName);
		if ( settings == null )
		{
			settings = new IterationSettings();
			_iterationsSettings.put(simName, settings);
		}
		return settings;
	}
	/**
	 * @param name
	 * @return
	 */
	public PositionAnalysisSettings getPositionAnalysisSettings(String simName)
	{
		PositionAnalysisSettings settings = _positionAnalysisSettings.get(simName);
		if ( settings == null )
		{
			settings = new PositionAnalysisSettings();
			_positionAnalysisSettings.put(simName, settings);
		}
		return settings;

	}
	/**
	 * @param name
	 * @return
	 */
	public ComputeType getComputeType(String simName)
	{
		ComputeType computeType = _computeTypeSettings.get(simName);
		if ( computeType == null )
		{
			computeType = ComputeType.Standard;
		}
		return computeType;
	}
	/**
	 * 
	 * @param simName
	 * @param ct
	 */
	public void setComputeType(String simName, ComputeType ct)
	{
		if ( simName != null && ct != null )
		{
			_computeTypeSettings.put(simName, ct);
		}
	}
	/**
	 * @param name
	 * @param computeType
	 * @return
	 */
	public BaseComputeSettings getComputeSettings(String simName,
			ComputeType computeType)
	{
		switch ( computeType )
		{
			case Iterative:
				return getIterationSettings(simName);
			case PositionAnalysis:
				return getPositionAnalysisSettings(simName);
			default:
				return null;
		}
	}
	


	
	
}
