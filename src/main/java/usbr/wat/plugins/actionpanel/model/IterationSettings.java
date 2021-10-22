
package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Element;

import com.rma.util.XMLUtilities;

import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPlugin;
import hec2.wat.plugin.WatPluginManager;

/**
 * @author Mark Ackerman
 *A
 *
 * iteration compute settings for a simulation
 */
public class IterationSettings
{
	private boolean _isIterative;
	
	private Map<String, ModelAltIterationSettings>_modelAltSettings = new HashMap<>();

	private int[] _membersToCompute;

	private int _maxMember;
	private SensitivitySettings _sensitivitySettings = new SensitivitySettings();
	
	public IterationSettings()
	{
		super();
	}
	

	/**
	 * @return the isIterative
	 */

	public boolean isIterative()
	{
		return _isIterative;
	}


	/**
	 * @param isIterative the isIterative to set
	 */

	public void setIterative(boolean isIterative)
	{
		_isIterative = isIterative;
	}


	/**
	 * @return the modelAltSettings
	 */
	public ModelAltIterationSettings getModelAltSettings(ModelAlternative modelAlt)
	{
		if ( modelAlt == null )
		{
			return null;
		}
		ModelAltIterationSettings mAltSettings = _modelAltSettings.get(getKey(modelAlt));
		if ( mAltSettings == null || mAltSettings.getDataLocations().size() == 0 )
		{
			mAltSettings = createDefaultModelAltInterationSettings(modelAlt);
		}
		else
		{
			syncDataLocations(modelAlt, mAltSettings);
			mAltSettings.setModelAlternative(modelAlt);
		}
		return mAltSettings;
	}
	

	/**
	 * sync the data locations read from disk with what the plugin returns to make sure we have the latest list
	 * @param modelAlt
	 * @param mAltSettings
	 */
	private void syncDataLocations(ModelAlternative modelAlt,
			ModelAltIterationSettings mAltSettings)
	{
		List<DataLocation> pluginDataLocs = getDataLocations(modelAlt);
		List<DataLocation> savedDataLocs = mAltSettings.getDataLocations();
		
		Set<DataLocation> pluginDataLocsSet = new HashSet<>(pluginDataLocs);
		Set<DataLocation> savedDataLocsSet = new HashSet<>(savedDataLocs);
		
		// find new DataLocations
		pluginDataLocsSet.removeAll(savedDataLocs);
		savedDataLocs.addAll(pluginDataLocsSet);
		
		
		// find deleted DataLocations
		savedDataLocsSet.removeAll(pluginDataLocs);
		savedDataLocs.removeAll(savedDataLocsSet);
		
		
	}


	/**
	 * @param modelAlt
	 * @return
	 */
	
	public static String getKey(ModelAlternative modelAlt)
	{
		return modelAlt.getProgram()+"-"+modelAlt.getName();
	}
	

	/**
	 * @param modelAlt
	 * @return
	 */
	
	private ModelAltIterationSettings createDefaultModelAltInterationSettings(
			ModelAlternative modelAlt)
	{
		
		if( modelAlt == null )
		{
			return null;
		}
		List<DataLocation>dataLocs = getDataLocations(modelAlt);
		if (dataLocs != null )
		{
			ModelAltIterationSettings settings = new ModelAltIterationSettings(dataLocs);
			_modelAltSettings.put(getKey(modelAlt), settings);
			return settings;
		}
		return null;
		
	}
	

	/**
	 * @param modelAlt
	 * @return
	 */
	private List<DataLocation> getDataLocations(ModelAlternative modelAlt)
	{
		String program = modelAlt.getProgram();
		
		SimpleWatPlugin splugin = WatPluginManager.getPlugin(program);
		if ( splugin instanceof WatPlugin )
		{
			WatPlugin plugin = (WatPlugin) splugin;
			List<DataLocation> dataLocs = plugin.getDataLocations(modelAlt, DataLocation.INPUT_LOCATIONS);
			dataLocs = filterDataLocs(dataLocs);
			return dataLocs;
		}
		return null;
	}


	/**
	 * @param dataLocs
	 * @return
	 */
	
	private List<DataLocation> filterDataLocs(List<DataLocation> dataLocs)
	{
		
		List<DataLocation>dssDataLocs = new ArrayList<>();
		if ( dataLocs == null )
		{
			return dssDataLocs;
		}
		DataLocation dataLoc;
		for (int i = 0; i < dataLocs.size();i++  )
		{
			dataLoc = dataLocs.get(i);
			if ( dataLoc.getLinkedToLocation() instanceof DssDataLocation )
			{
				dssDataLocs.add(dataLoc);
			}
		}
		return dssDataLocs;
		
	}
	

	/**
	 * @param modelAltSettings the modelAltSettings to set
	 */

	public void setModelAltSettings( Map<String, ModelAltIterationSettings> modelAltSettings)
	{
		_modelAltSettings.clear();
		if ( modelAltSettings != null )
		{
			_modelAltSettings.putAll(modelAltSettings);
		}
	}


	/**
	 * @param computeMembers
	 */
	public void setMembersToCompute(int[] computeMembers)
	{
		_membersToCompute = computeMembers;
	}
	
	public int[] getMembersToCompute()
	{
		return _membersToCompute;
	}


	/**
	 * @param value
	 */
	public void setMaximumMember(int maxMember)
	{
		_maxMember = maxMember;
	}
	
	public int getMaximumMember()
	{
		return _maxMember;
	}


	/**
	 * @return
	 */
	public SensitivitySettings getSensitivitySettings()
	{
		return _sensitivitySettings;
	}


	/**
	 * @param iterElem
	 */
	public void saveData(Element iterElem)
	{
		XMLUtilities.saveChildElement(iterElem, "isIterative", _isIterative);
		XMLUtilities.saveChildElement(iterElem, "maxMember", _maxMember);
		Element memberElem = new Element("MembersToCompute");
		iterElem.addContent(memberElem);
		XMLUtilities.createArrayElements(memberElem, _membersToCompute);
		saveModelAltSettings(iterElem);
		saveSensitivitySettings(iterElem);
	}


	/**
	 * @param iterElem
	 */
	private void saveModelAltSettings(Element iterElem)
	{
		Element modelAltElem = new Element("ModelAlternativeSettings");
		iterElem.addContent(modelAltElem);
		Set<Entry<String, ModelAltIterationSettings>> entrySet = _modelAltSettings.entrySet();
		Iterator<Entry<String, ModelAltIterationSettings>> iter = entrySet.iterator();
		
		Entry<String, ModelAltIterationSettings> entry;
		while ( iter.hasNext())
		{
			entry = iter.next();
			Element entryElem = new Element("IterationSetting");
			modelAltElem.addContent(entryElem);
			XMLUtilities.addChildContent(entryElem, "ModelAlternative", entry.getKey());
			ModelAltIterationSettings maSettings = entry.getValue();
			maSettings.saveData(entryElem);
		}
		
		
	}


	/**
	 * @param iterElem
	 */
	private void saveSensitivitySettings(Element iterElem)
	{
		Element sensitivityElem = new Element("SensitivitySettings");
		iterElem.addContent(sensitivityElem);
		_sensitivitySettings.saveData(sensitivityElem);
		
	}


	/**
	 * @param iterElem
	 */
	public void loadData(Element iterElem)
	{
		_isIterative = XMLUtilities.getChildElementAsBoolean(iterElem, "isIterative", _isIterative);
		_maxMember = XMLUtilities.getChildElementAsInt(iterElem, "maxMember", _maxMember);
		Element memberElem = iterElem.getChild("MembersToCompute");
		_membersToCompute = XMLUtilities.getIntArrayElements(memberElem);
		loadModelAltSettings(iterElem);
		loadSensitivitySettings(iterElem);
	}


	
	/**
	 * @param iterElem
	 */
	private void loadModelAltSettings(Element iterElem)
	{
		Element modelAltElem = iterElem.getChild("ModelAlternativeSettings");
		if ( modelAltElem != null )
		{
			_modelAltSettings.clear();
			List kids = modelAltElem.getChildren("IterationSetting");
			for (int i = 0;i < kids.size(); i++ )
			{
				Element iterKid = (Element) kids.get(i);
				ModelAltIterationSettings maSettings = new ModelAltIterationSettings();
				maSettings.loadData(iterKid);
				String maKey = XMLUtilities.getChildElementAsString(iterKid, "ModelAlternative", null);
				if ( maKey != null )
				{
					_modelAltSettings.put(maKey, maSettings);
				}
				
			}
			
		}
		
	}
	
	/**
	 * @param iterElem
	 */
	private void loadSensitivitySettings(Element iterElem)
	{
		Element sensitivityElem = iterElem.getChild("SensitivitySettings");
		if ( sensitivityElem != null )
		{
			_sensitivitySettings.loadData(sensitivityElem);
		}
	}



}