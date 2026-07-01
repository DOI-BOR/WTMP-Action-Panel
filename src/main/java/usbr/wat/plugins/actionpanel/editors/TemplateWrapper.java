package usbr.wat.plugins.actionpanel.editors;

import rma.util.RMAIO; // RMA I/O utility providing path and string manipulation helpers, including file name extraction

/**
 * Lightweight wrapper around a report template file path.
 *
 * Stores the full path to a template file and derives a human-readable
 * display name by extracting just the file name from the path. Instances
 * of this class are used to populate combo boxes and selection lists where
 * templates must be presented by name but resolved by their full path.
 *
 * Both the name and path are immutable once the object is constructed.
 */
public class TemplateWrapper {
	// The file name extracted from the template path, used as the display label
	private final String _name;

	// The full path to the template file
	private final String _path;

	/**
	 * Constructs a TemplateWrapper for the given template file path.
	 *
	 * Extracts the file name from the path using RMAIO and stores both
	 * the name and the full path for later retrieval.
	 *
	 * @param templatePath the full file system path to the template file
	 */
	public TemplateWrapper(String templatePath) {
		// Invoke the Object superclass constructor (explicit for clarity)
		super();

		// Extract just the file name portion from the full template path
		_name = RMAIO.getFileFromPath(templatePath);

		// Store the complete path for later use when opening the template
		_path = templatePath;
	}

	/**
	 * Returns the full file system path to the template file.
	 *
	 * @return the template file path as provided at construction time
	 */
	public String getPath() {
		return _path;
	}

	/**
	 * Returns the display name of this template, derived from its file name.
	 *
	 * This value is used when the wrapper is displayed in a combo box or list,
	 * showing only the file name rather than the full path.
	 *
	 * @return the file name portion of the template path
	 */
	public String toString() {
		return _name;
	}
}
