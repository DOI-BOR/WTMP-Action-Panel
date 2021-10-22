package usbr.wat.plugins.actionpanel.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Element;

import com.rma.util.XMLUtilities;

import hec.lang.NamedType;

import hec2.plugin.model.ModelAlternative;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ComputeSettings extends NamedType
{
	private Map<String, ScriptSettings> _scriptSettings = new HashMap<>();
	
	public ComputeSettings()
	{
		super();
	}
	/**
	 * @param modelAlt
	 * @return
	 */
	public String getScriptFor(ModelAlternative modelAlt)
	{
		String key = getKey(modelAlt);
		ScriptSettings settings = _scriptSettings.get(key);
		if ( settings != null )
		{
			return settings.script;
		}
		return null;
	}

	
	/**
	 * @param modelAlt
	 * @return
	 */
	public boolean shouldRunScriptFor(ModelAlternative modelAlt)
	{
		String key = getKey(modelAlt);
		ScriptSettings settings = _scriptSettings.get(key);
		if ( settings != null )
		{
			return settings.runScript;
		}
		return false;
	}
	
	/**
	 * @param modelAlt
	 * @return
	 */
	private static String getKey(ModelAlternative modelAlt)
	{
		return IterationSettings.getKey(modelAlt);
	}
	
	
	/**
	 * @param modelAlt
	 * @param script
	 */
	public void setScriptFor(ModelAlternative modelAlt, String script)
	{
		if ( modelAlt == null )
		{
			return;
		}
		
		String key = getKey(modelAlt);
		ScriptSettings settings = _scriptSettings.get(key);
		if ( settings == null )
		{
			settings = new ScriptSettings();
			_scriptSettings.put(key, settings);
		}
		settings.script = script;
	}
	/**
	 * @param modelAlt
	 * @param runScript
	 */
	public void setRunScriptFor(ModelAlternative modelAlt, boolean runScript)
	{
		if ( modelAlt == null )
		{
			return;
		}
		
		String key = getKey(modelAlt);
		ScriptSettings settings = _scriptSettings.get(key);
		if ( settings == null )
		{
			settings = new ScriptSettings();
			_scriptSettings.put(key, settings);
		}
		settings.runScript = runScript;
	}
	/**
	 * @param preComputeElem
	 */
	public void saveData(Element element)
	{
		Set<Entry<String, ScriptSettings>> entrySet = _scriptSettings.entrySet();
		Iterator<Entry<String, ScriptSettings>> iter = entrySet.iterator();
		Entry<String, ScriptSettings> entry;
		String maInfo;
		ScriptSettings scriptSetting;
		Element scriptElement;
		while (iter.hasNext())
		{
			entry = iter.next();
			maInfo = entry.getKey();
			scriptSetting = entry.getValue();
			
			scriptElement = new Element("ScriptSetting");
			element.addContent(scriptElement);
			XMLUtilities.addChildContent(scriptElement, "ModelAlternative", maInfo);
			scriptSetting.saveData(scriptElement);
			
		}
	}
	
	public void loadData(Element element)
	{
		_scriptSettings.clear();
		String maInfo;
		ScriptSettings scriptSetting;
		Element scriptElement;
		List kidElements = element.getChildren("ScriptSetting");
		
		for(int i =0;i < kidElements.size(); i++ )
		{
			scriptElement = (Element) kidElements.get(i);
			maInfo = XMLUtilities.getChildElementAsString(scriptElement, "ModelAlternative", null);
			scriptSetting = new ScriptSettings();
			scriptSetting.loadData(scriptElement);
			if ( maInfo != null )
			{
				_scriptSettings.put(maInfo, scriptSetting);
			}
		}	
	}
	
	
	class ScriptSettings 
	{
		String script;
		boolean runScript;
		/**
		 * @param element
		 */
		public void saveData(Element element)
		{
			XMLUtilities.addChildContent(element, "Script", script!=null?script:"");
			XMLUtilities.addChildContent(element, "RunScript", runScript);
		}
		
		public void loadData(Element element)
		{
			script = XMLUtilities.getChildElementAsString(element, "Script", "");
			runScript = XMLUtilities.getChildElementAsBoolean(element, "RunScript", false);
		}
	}


}
