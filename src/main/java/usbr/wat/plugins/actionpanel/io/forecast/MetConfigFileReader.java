package usbr.wat.plugins.actionpanel.io.forecast;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import usbr.wat.plugins.actionpanel.model.forecast.DssPathMap;

public class MetConfigFileReader extends DssPathMap
{
	private final String _configFile;

	public MetConfigFileReader(String configFile)
	{
		super(null, configFile);
		_configFile = configFile;
	}


}
