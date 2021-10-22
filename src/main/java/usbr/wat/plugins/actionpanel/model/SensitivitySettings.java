
package usbr.wat.plugins.actionpanel.model;

import org.jdom.Element;

import hec.lang.NamedType;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class SensitivitySettings extends NamedType
{
	private ComputeSettings _preComputeSettings = new ComputeSettings();
	private ComputeSettings _postComputeSettings = new ComputeSettings();
	
	public SensitivitySettings()
	{
		super();
	}

	/**
	 * @return
	 */
	public ComputeSettings getPreComputeSettings()
	{
		
		return _preComputeSettings;
	}

	/**
	 * @return
	 */
	public ComputeSettings getPostComputeSettings()
	{
		return _postComputeSettings;
	}

	/**
	 * @param sensitivityElem
	 */
	public void saveData(Element sensitivityElem)
	{
		Element preComputeElem = new Element("PreCompute");
		sensitivityElem.addContent(preComputeElem);
		_preComputeSettings.saveData(preComputeElem);
		
		Element postComputeElem = new Element("PostCompute");
		sensitivityElem.addContent(postComputeElem);
		_postComputeSettings.saveData(postComputeElem);
		
	}

	/**
	 * @param sensitivityElem
	 */
	public void loadData(Element sensitivityElem)
	{
		Element preComputeElem = sensitivityElem.getChild("PreCompute");
		if ( preComputeElem != null )
		{
			_preComputeSettings.loadData(preComputeElem);
		}
		
		Element postComputeElem = sensitivityElem.getChild("PostCompute");
		if ( postComputeElem != null )
		{
			_postComputeSettings.loadData(postComputeElem);	
		}
	}
}
