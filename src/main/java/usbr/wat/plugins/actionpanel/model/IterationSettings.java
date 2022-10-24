
package usbr.wat.plugins.actionpanel.model;

import org.jdom.Element;

/**
 * @author Mark Ackerman
 *A
 *
 * iteration compute settings for a simulation
 */
public class IterationSettings extends BaseComputeSettings
{
	
	private SensitivitySettings _sensitivitySettings = new SensitivitySettings();
	
	public IterationSettings()
	{
		super();
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
	@Override
	public void saveData(Element iterElem)
	{
		super.saveData(iterElem);
		saveSensitivitySettings(iterElem);
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
	@Override
	public void loadData(Element iterElem)
	{
		super.loadData(iterElem);
		loadSensitivitySettings(iterElem);
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

	@Override
	protected String getSettingElementString()
	{
		return "IterationSettings";
	}

	@Override
	public String getCollectionDssFilename()
	{
		return "iterationResults.dss";
	}
}