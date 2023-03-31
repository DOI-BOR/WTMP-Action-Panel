
package usbr.wat.plugins.actionpanel.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Element;

import com.rma.util.XMLUtilities;

/**
 * class that holds a group of Simulations
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SimulationGroup extends AbstractSimulationGroup
{
	public static final String FILE_EXT = "simgrp";
	


	
	private Map<String, IterationSettings>_iterationsSettings = new HashMap<>();
	private Map<String, PositionAnalysisSettings>_positionAnalysisSettings = new HashMap<>();
	
	private Map<String, ComputeType>_computeTypeSettings = new HashMap();

	public SimulationGroup()
	{
		super();
	}
	
	
	/**
	 * @param elem
	 */
	@Override
	protected void finishSaving(Element elem)
	{
		saveComputeTypes(elem);
	}
	/**
	 * @param simelem
	 * @param name
	 */
	@Override
	protected void saveSimulationSettings(Element simelem, String simName)
	{
		saveIterationSettings(simelem, simName);
		savePositionAnalysisSettings(simelem, simName);
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
	protected void finishLoading(Element root)
	{
		loadComputeTypes(root);
	}
	@Override
	protected void initForLoading()
	{
		_iterationsSettings.clear();
		_positionAnalysisSettings.clear();
		_computeTypeSettings.clear();
	}
	@Override
	protected String getSimulationGroupType()
	{
		return "SimulationGroup";
	}
	/**
	 * @param simElem
	 * @param simName
	 */
	@Override
	protected void loadSimulationSettings(Element simElem,
			String simName)
	{
		loadIterationSettings(simElem, simName);
		loadPositionAnalysisSettings(simElem, simName);
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
