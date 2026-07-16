package usbr.wat.plugins.actionpanel.model.planning;

import java.io.File;                          // Represents the on-disk XML file this container is saved to/loaded from
import java.io.FileOutputStream;              // Output stream used to write the serialized XML document to disk
import java.io.FileInputStream;               // Input stream used to read the serialized XML document from disk
import java.io.IOException;                   // Thrown by file I/O failures during save/load
import java.nio.file.Path;                    // Immutable file-system path type used for resolving the config file location
import java.nio.file.Paths;                   // Factory for constructing Path instances from string segments
import java.util.ArrayList;                   // Backing list implementation for the managed Sets
import java.util.Collections;                 // Provides an unmodifiable view of the managed Sets list
import java.util.List;                        // Ordered collection interface for the managed Sets

import org.jdom.Document;                     // JDOM document type wrapping the root "PlanningSets" element
import org.jdom.Element;                      // JDOM XML element type
import org.jdom.JDOMException;                // Thrown by SAXBuilder on malformed XML
import org.jdom.input.SAXBuilder;             // Parses an XML file back into a JDOM Document
import org.jdom.output.Format;                // Formatting options (pretty-print) for XMLOutputter
import org.jdom.output.XMLOutputter;          // Serializes a JDOM Document to an output stream

import com.rma.model.Project;                 // Represents the currently loaded RMA project; used to resolve the absolute file path

/**
 * Holds the full list of {@link PlanningSet} instances defined within the current WAT
 * project's Planning workflow, and persists them to a single project-relative XML file.
 *
 * This mirrors the lightweight persistence pattern used by
 * {@code usbr.wat.plugins.actionpanel.model.SharedConfigFiles} for path resolution, but
 * (unlike the {@code SimulationGroup}/{@code ForecastSimGroup} hierarchy, which is
 * registered as a full {@code AbstractXMLManager} in the project's manager list) Sets are
 * saved as a single flat XML file rather than one file per Set. This keeps the Planning
 * workflow's persistence self-contained and easy to test without requiring integration
 * with the project's manager/proxy framework.
 *
 * The default file location is {@code planning/config/planning_sets.xml} under the
 * project directory, overridable via the {@code WTMP.planningSetsFile} system property.
 */
public class PlanningSetContainer {

	// Base relative directory for Planning workflow configuration files
	private static final Path BASE_FOLDER = Paths.get("planning/config");

	// Default file name for the saved list of Planning Sets
	private static final String DEFAULT_FILE_NAME = "planning_sets.xml";

	// System property key for overriding the Planning Sets file path
	private static final String PLANNING_SETS_FILE_PROPERTY = "WTMP.planningSetsFile";

	// Root XML element name for the saved document
	private static final String ROOT_ELEMENT_NAME = "PlanningSets";

	// The Sets currently loaded/managed by this container
	private final List<PlanningSet> _sets = new ArrayList<>();

	/**
	 * Constructs an empty container. Call {@link #load()} to populate it from disk.
	 */
	public PlanningSetContainer() {
		// Intentionally empty; _sets starts empty until load() is called
	}

	/**
	 * Returns the project-relative path to the Planning Sets file.
	 *
	 * Checks the {@code WTMP.planningSetsFile} system property first; if not set, uses
	 * the default path: {@code planning/config/planning_sets.xml}.
	 *
	 * @return the project-relative Path to the Planning Sets file
	 */
	public static Path getRelativePlanningSetsFile() {
		// Use the system property override if provided; otherwise fall back to the default path
		String file = System.getProperty(PLANNING_SETS_FILE_PROPERTY,
				BASE_FOLDER.resolve(DEFAULT_FILE_NAME).toString());
		return Paths.get(file); // Convert the resolved string into a Path instance
	}

	/**
	 * Returns the absolute path to the Planning Sets file by resolving the relative path
	 * against the current project directory.
	 *
	 * @return the absolute Path to the Planning Sets file
	 */
	public static Path getPlanningSetsFile() {
		// Resolve the relative path against the currently open project's directory
		String absFile = Project.getCurrentProject().getAbsolutePath(getRelativePlanningSetsFile().toString());
		return Paths.get(absFile); // Convert the resolved absolute string into a Path instance
	}

	/**
	 * Returns an unmodifiable view of all Sets currently managed by this container.
	 *
	 * @return the managed Sets, in the order they were added or loaded
	 */
	public List<PlanningSet> getSets() {
		return Collections.unmodifiableList(_sets); // Prevent external code from mutating the backing list directly
	}

	/**
	 * Returns the Set with the given name, if present.
	 *
	 * @param name the name to search for
	 * @return the matching Set, or null if no Set with that name is managed
	 */
	public PlanningSet getSetByName(String name) {
		if (name == null) {
			return null; // No name to match against
		}
		for (PlanningSet set : _sets) { // Walk every managed Set looking for a name match
			if (name.equals(set.getName())) {
				return set; // Found it; return immediately
			}
		}
		return null; // No Set with that name is currently managed
	}

	/**
	 * Adds a new Set to this container, or replaces an existing Set with the same name.
	 *
	 * @param set the Set to add; ignored if null
	 */
	public void addSet(PlanningSet set) {
		if (set == null) {
			return; // Nothing to add
		}
		removeSetByName(set.getName()); // Drop any prior Set with the same name first
		_sets.add(set); // Add the (possibly edited) Set back in
	}

	/**
	 * Removes the Set with the given name from this container, if present.
	 *
	 * @param name the name of the Set to remove
	 */
	public void removeSetByName(String name) {
		if (name == null) {
			return; // Nothing to remove
		}
		// Remove every Set whose name matches; there should be at most one, by construction
		_sets.removeIf(s -> name.equals(s.getName()));
	}

	/**
	 * Loads all Sets from the Planning Sets file on disk, replacing this container's
	 * current contents. If the file does not yet exist (e.g. no Sets have been created
	 * in this project), this container is simply left empty.
	 *
	 * @throws IOException   if the file exists but cannot be read
	 * @throws JDOMException if the file exists but contains malformed XML
	 */
	public void load() throws IOException, JDOMException {
		_sets.clear(); // Discard whatever was previously loaded before reloading

		File file = getPlanningSetsFile().toFile(); // Resolve the on-disk location for this project
		if (!file.exists()) {
			return; // No Sets have been saved for this project yet; leave the container empty
		}

		// Try-with-resources ensures the stream is closed even if parsing throws
		try (FileInputStream in = new FileInputStream(file)) {
			SAXBuilder builder = new SAXBuilder(); // Standard JDOM XML parser
			Document doc = builder.build(in); // Parse the file into a JDOM document
			Element root = doc.getRootElement(); // The top-level "PlanningSets" element

			for (Object child : root.getChildren("PlanningSet")) { // Walk every saved Set element
				PlanningSet set = new PlanningSet(); // Create a fresh instance to populate
				set.loadData((Element) child); // Delegate to PlanningSet's own loadData
				_sets.add(set); // Add the reconstructed Set to this container
			}
		}
	}

	/**
	 * Saves all currently managed Sets to the Planning Sets file on disk, creating the
	 * containing directory if it does not already exist.
	 *
	 * @throws IOException if the file or its containing directory cannot be written
	 */
	public void save() throws IOException {
		File file = getPlanningSetsFile().toFile(); // Resolve the on-disk location for this project
		File parentDir = file.getParentFile(); // The directory the file lives in
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs(); // Create the directory tree if it does not already exist
		}

		Element root = new Element(ROOT_ELEMENT_NAME); // Create the top-level "PlanningSets" element
		Document doc = new Document(root); // Wrap it in a JDOM document for serialization

		for (PlanningSet set : _sets) { // Walk every managed Set
			set.saveData(root); // Delegate to PlanningSet's own saveData, appending under root
		}

		// Try-with-resources ensures the stream is closed even if writing throws
		try (FileOutputStream out = new FileOutputStream(file)) {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat()); // Human-readable indentation
			outputter.output(doc, out); // Write the document to disk
		}
	}
}
