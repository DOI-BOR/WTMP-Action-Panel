package usbr.wat.plugins.actionpanel.model;

import java.io.IOException;     // Checked exception thrown when the results folder cannot be renamed
import java.nio.file.Files;     // NIO Files utility for moving (renaming) a directory path
import java.nio.file.Path;      // NIO immutable file system path; used for rename operations
import java.nio.file.Paths;     // Factory for constructing Path instances from string paths
import java.util.Date;          // Represents the date and time at which this results data was saved
import java.util.logging.Logger; // JDK logger for recording rename failures

import org.jdom.Document;  // JDOM XML Document that wraps the root element for serialization
import org.jdom.Element;   // JDOM XML Element used for reading and writing results metadata

import com.rma.io.FileManagerImpl; // RMA file manager for resolving RmaFile references from paths
import com.rma.io.RmaFile;         // RMA abstraction representing a file path for the results XML file
import com.rma.util.XMLUtilities;  // RMA XML utility for reading and writing named-type data and child elements

import hec.lang.NamedType;         // HEC base class providing a name field and named-type XML support

import hec2.wat.model.WatSimulation; // WAT simulation associated with these results

import rma.util.RMAIO;             // RMA I/O utility for path concatenation and file-name sanitization

/**
 * Data object representing the metadata associated with a saved simulation result set
 * within the WTMP Action Panel.
 *
 * Stores the display name, the username of the person who saved the results, the save
 * date/time, and the timestamp of the most recent compute run. The associated simulation
 * and results folder path are held as transient fields (not persisted in the XML file).
 *
 * Instances are persisted to a fixed-name XML file ("resultsData") inside the simulation's
 * results directory via saveDataToFolder(). They are restored via loadDataFromFolder().
 *
 * The renameTo() method renames the results directory on disk and updates all internal
 * path and name references accordingly.
 *
 */
public class ResultsData extends NamedType {
	// Fixed file name (no extension) for the XML results metadata file inside each results directory
	private static final String RESULTS_DATA_FILE = "resultsData";

	// Username of the person who saved these results
	private String _savedBy;

	// Date and time at which these results were saved
	private Date _savedAtDate;

	// The associated WAT simulation; transient — not included in the XML serialization
	private transient WatSimulation _simulation;

	// The absolute path to the results directory; transient — not included in the XML serialization
	private transient String _folder;

	// Unix-epoch timestamp (milliseconds) of the most recent compute run for this result set
	private long _lastComputedDate;

	/**
	 * Constructs a ResultsData object for the given simulation and results directory path.
	 *
	 * @param simulation the WatSimulation associated with these results
	 * @param folder     the absolute path to the results directory
	 */
	public ResultsData(WatSimulation simulation, String folder) {
		super();
		_simulation = simulation;
		_folder = folder;
	}

	/**
	 * Returns the username of the person who saved these results.
	 *
	 * @return the saved-by username string, or null if not yet set
	 */
	public String getSavedBy() {
		return _savedBy;
	}

	/**
	 * Sets the username of the person saving these results.
	 *
	 * @param savedBy the username string to record
	 */
	public void setSavedBy(String savedBy) {
		_savedBy = savedBy;
	}

	/**
	 * Sets the date and time at which these results were saved.
	 *
	 * @param date the save date/time
	 */
	public void setSavedAt(Date date) {
		_savedAtDate = date;
	}

	/**
	 * Returns the date and time at which these results were saved.
	 *
	 * @return the save date, or null if not set
	 */
	public Date getSavedAt() {
		return _savedAtDate;
	}

	/**
	 * Returns the WAT simulation associated with these results.
	 *
	 * @return the associated WatSimulation instance
	 */
	public WatSimulation getSimulation() {
		return _simulation;
	}

	/**
	 * Returns the absolute path to the results directory.
	 *
	 * @return the results folder path string
	 */
	public String getFolder() {
		return _folder;
	}

	/**
	 * Serializes the results metadata to an XML file named "resultsData" inside the
	 * given results directory.
	 *
	 * Writes the NamedType name, saved-by username, save date, and last computed
	 * timestamp as XML child elements under a root "Results" element.
	 *
	 * @param resultsDir the absolute path to the results directory; returns false if null
	 * @return true if the XML file was written successfully; false otherwise
	 */
	public boolean saveDataToFolder(String resultsDir) {
		// Return immediately if the data is invalid
		if (resultsDir == null) {
			return false;
		}

		// Resolve the results metadata file path within the given directory
		String resultsFileName = RMAIO.concatPath(resultsDir, RESULTS_DATA_FILE);
		RmaFile resultsFile = FileManagerImpl.getFileManager().getFile(resultsFileName);

		// Build the XML document structure
		Element root = new Element("Results");
		Document doc = new Document(root);

		// Write the NamedType name, save metadata, and compute timestamp
		XMLUtilities.saveNamedType(root, this);
		XMLUtilities.addChildContent(root, "SavedBy", _savedBy);
		XMLUtilities.addChildContent(root, "SavedOn", _savedAtDate.toString());
		XMLUtilities.addChildContent(root, "LastComputedDate", _lastComputedDate);

		return XMLUtilities.saveDocument(doc, resultsFile);
	}

	/**
	 * Restores the results metadata from the XML file named "resultsData" inside the
	 * given results directory (path-string overload).
	 *
	 * @param resultsDir the absolute path to the results directory; returns false if null
	 * @return true if the file was found and loaded successfully; false otherwise
	 */
	public boolean loadDataFromFolder(String resultsDir) {
		// Return immediately if the data is invalid
		if (resultsDir == null) {
			return false;
		}

		// Resolve the results metadata file and delegate to the RmaFile overload
		String resultsFileName = RMAIO.concatPath(resultsDir, RESULTS_DATA_FILE);
		RmaFile resultsFile = FileManagerImpl.getFileManager().getFile(resultsFileName);
		return loadDataFromFolder(resultsFile);
	}

	/**
	 * Restores the results metadata from the given RmaFile.
	 *
	 * Reads the NamedType name, saved-by username, save date string, and last
	 * computed timestamp from the XML document. Returns false if the file cannot
	 * be parsed as a valid XML document.
	 *
	 * @param resultsFile the RmaFile pointing to the "resultsData" XML file
	 * @return true if the file was parsed and loaded successfully; false otherwise
	 */
	public boolean loadDataFromFolder(RmaFile resultsFile) {
		// Return immediately if the document is invalid
		Document doc = XMLUtilities.loadDocument(resultsFile);
		if (doc == null) {
			return false;
		}

		// Get the root reference from the document
		Element root = doc.getRootElement();

		// Restore the NamedType name from the XML
		XMLUtilities.loadNamedType(root, this);

		// Read the saved-by username, defaulting to the current value if absent
		_savedBy = XMLUtilities.getChildElementAsString(root, "SavedBy", _savedBy);

		// Parse the save date from its string representation
		String saveOnStr = XMLUtilities.getChildElementAsString(root, "SavedOn", null);
		if (saveOnStr != null) {
			_savedAtDate = new Date(saveOnStr);
		}

		// Read the last-computed timestamp
		_lastComputedDate = XMLUtilities.getChildElementAsLong(root, "LastComputedDate", 0);

		return true;
	}

	/**
	 * Sets the Unix-epoch timestamp (milliseconds) of the most recent compute run.
	 *
	 * @param lastComputedDate the compute timestamp in milliseconds since epoch
	 */
	public void setLastComputedTime(long lastComputedDate) {
		_lastComputedDate = lastComputedDate;
	}

	/**
	 * Returns the Unix-epoch timestamp (milliseconds) of the most recent compute run.
	 *
	 * @return the last computed time in milliseconds since epoch; 0 if never computed
	 */
	public long getLastComputedTime() {
		return _lastComputedDate;
	}

	/**
	 * Renames the results directory on disk to a sanitized version of the new name
	 * and updates the internal name and folder path accordingly.
	 *
	 * The new directory name is derived from newName via RMAIO.userNameToFileName()
	 * to ensure it is safe for use as a file system path. On success, updates the
	 * NamedType name, the _folder path, and re-saves the metadata XML file.
	 *
	 * @param newName the new display name for this result set; must be non-null and non-empty
	 * @return true if the rename succeeded; false if newName is invalid or the move fails
	 */
	public boolean renameTo(String newName) {
		// Return immediately if the data is invalid
		if (newName == null || newName.isEmpty()) {
			return false;
		}

		// Get the folder name
		String folderName = getFolder();

		// Construct the source NIO Path from the current folder string
		Path src = Paths.get(folderName);

		try {
			// Move (rename) the directory to a sibling path derived from the new name
			Path newPath = Files.move(src, src.resolveSibling(RMAIO.userNameToFileName(newName)));

			boolean rv = newPath != null;
			if (rv) {
				// Update internal state and re-save the metadata XML with the new name
				setName(newName);
				_folder = newPath.toString();
				saveDataToFolder(newPath.toString());
			}

			return rv;

		} catch (IOException e) {
			// Log the failure; the rename did not succeed
			Logger.getLogger(ResultsData.class.getName()).info("Failed to rename " + getName() + " Error:" + e);
			return false;
		}
	}
}
