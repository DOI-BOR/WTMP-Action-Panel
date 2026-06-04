package usbr.wat.plugins.actionpanel.io.forecast;

import com.rma.io.FileManagerImpl; // Import file manager implementation for file I/O operations
import com.rma.io.RmaFile; // Import RMA File wrapper class for managing file paths and streams
import usbr.wat.plugins.actionpanel.model.forecast.DssPathMap; // Import parent DSS path mapping base class

// MetConfigFileReader is a concrete implementation extending base forecast model class
public class MetConfigFileReader extends DssPathMap {
	// Private final field storing the path to MET configuration file
	private final String _configFile;

	// Public constructor accepting configuration file path string
	public MetConfigFileReader(String configFile) {
		super(null, configFile); // Call parent constructor with null as first arg and config path as second
		_configFile = configFile; // Store config file path in instance field
	}
}