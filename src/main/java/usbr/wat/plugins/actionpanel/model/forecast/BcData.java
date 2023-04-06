/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import java.nio.file.Path;
import java.nio.file.Paths;
import com.rma.util.XMLUtilities;
import hec.lang.NamedType;
import org.jdom.Element;

public class BcData extends NamedType
{

	private static final String OUTPUT_DSS_FILE_ELEM_ID = "Output-DSS-File";
	private String _opsDataName = "";
	private String _metDataName = "";
	private OperationsData _opsData;
	private MeteorlogicData _metData;
	private Path _outputDssFile;

	public BcData()
	{
		super();
	}

	public void saveData(Element parent)
	{
		Element myElem = new Element("BcData");
		parent.addContent(myElem);
		XMLUtilities.saveNamedType(myElem, this);
		Element outputDssFileElem = new Element(OUTPUT_DSS_FILE_ELEM_ID);
		if(_outputDssFile != null)
		{
			outputDssFileElem.setText(_outputDssFile.toString());
		}
		myElem.addContent(outputDssFileElem);

		Element opsElem = new Element("Operations");
		myElem.addContent(opsElem);
		opsElem.setText(_opsDataName);

		Element metElem = new Element("Meteorology");
		myElem.addContent(metElem);
		metElem.setText(_metDataName);
	}

	public boolean loadData(Element myElem)
	{
		if ( myElem == null )
		{
			return false;
		}
		XMLUtilities.loadNamedType(myElem, this);

		Element outputDssFileElem = myElem.getChild(OUTPUT_DSS_FILE_ELEM_ID);
		if(outputDssFileElem != null)
		{
			String filePath = outputDssFileElem.getText();
			if(filePath != null)
			{
				_outputDssFile = Paths.get(filePath);
			}
		}

		_opsDataName = XMLUtilities.getChildElementAsString(myElem, "Operations",  "");

		_metDataName = XMLUtilities.getChildElementAsString(myElem,"Meteorology", "");

		return true;
	}

	public void setSelectedOps(OperationsData opsData)
	{
		_opsDataName = "";
		_opsData = opsData;
		if ( opsData != null )
		{
			_opsDataName = opsData.getName();
		}
	}

	public void setSelectedMet(MeteorlogicData metData)
	{
		_metDataName = "";
		_metData = metData;
		if ( metData != null )
		{
			_metDataName = metData.getName();
		}
	}

	public String getOpsDataName()
	{
		return _opsDataName;
	}

	public String getMetDataName()
	{
		return _metDataName;
	}

	public OperationsData getOperationsData()
	{
		return _opsData;
	}

	public MeteorlogicData getMeteorogicalData()
	{
		return _metData;
	}

    public void setOutputDssFile(Path outputDssFile)
    {
		_outputDssFile = outputDssFile;
    }

	public Path getOutputDssFile()
	{
		return _outputDssFile;
	}
}
