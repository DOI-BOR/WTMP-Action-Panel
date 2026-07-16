package usbr.wat.plugins.actionpanel.model.planning;

import java.util.ArrayList;                    // Backing list implementation for the imported data rows
import java.util.List;                          // Ordered collection interface for the imported data rows

import org.jdom.Element;                        // JDOM XML element type used for saving and loading this object

import com.rma.util.XMLUtilities;               // Utility helper for serializing NamedType fields and simple child content to/from JDOM

import hec.lang.NamedType;                      // Base class supplying a display name and integer index, and standard modified-state tracking

/**
 * Represents a single named Hydrology dataset used as one of the three inputs (alongside
 * {@link CalSimData} and {@link ClimateScenario}) that make up a {@link PlanningSet}.
 *
 * A Hydrology dataset is populated by importing the output of an external hydrology
 * model. As of this implementation the exact hydrology modeling workflow is not yet
 * defined, so the import is invoked as a placeholder script call (see
 * {@code usbr.wat.plugins.actionpanel.editors.planning.NewHydrologyDataDialog}); once the
 * script runs, its output populates this dataset's rows, each row being
 * {@code [date, flow1, flow2, flow3]}, matching the "New Hydrology Data" dialog's grid.
 *
 * Instances are held directly by the owning {@link PlanningSet} (embedded, not referenced
 * by name) so that a saved Set remains fully self-contained and reloadable. They are also
 * added to the session-lifetime {@link PlanningSessionRegistry} so the same dataset can be
 * re-selected for another Set without re-importing it, for as long as the current WAT
 * session remains open.
 */
public class HydrologyData extends NamedType {

	// Free-text description of this dataset, shown in the New/Edit dialog's Description field
	private String _description = "";

	// Absolute or project-relative path to the file/output this dataset was last imported from
	private String _importedFilePath = "";

	// Imported/entered data rows: each row is {date, flow1, flow2, flow3}
	private final List<Object[]> _rows = new ArrayList<>();

	/**
	 * Constructs an empty Hydrology dataset with no name, description, or rows.
	 */
	public HydrologyData() {
		super(); // Invoke NamedType's default constructor to initialize name/index bookkeeping
	}

	/**
	 * Returns the description text entered for this dataset.
	 *
	 * @return the description, or an empty string if none was entered
	 */
	public String getDescription() {
		return _description; // Simple accessor
	}

	/**
	 * Sets the description text for this dataset and marks it modified.
	 *
	 * @param description the new description text; null is stored as an empty string
	 */
	public void setDescription(String description) {
		_description = description == null ? "" : description; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the path last used to import this dataset (e.g. the hydrology model's
	 * output location read by the placeholder import script).
	 *
	 * @return the imported file path, or an empty string if not yet imported
	 */
	public String getImportedFilePath() {
		return _importedFilePath; // Simple accessor
	}

	/**
	 * Records the path this dataset was imported from and marks it modified.
	 *
	 * @param path the source path; null is stored as an empty string
	 */
	public void setImportedFilePath(String path) {
		_importedFilePath = path == null ? "" : path; // Normalize null to empty string
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Returns the live list of data rows backing this dataset. Each row is a four-element
	 * array of {@code [date, flow1, flow2, flow3]}. Callers that mutate this list directly
	 * should call {@link #setModified(boolean)} themselves afterward.
	 *
	 * @return the mutable row list backing this dataset
	 */
	public List<Object[]> getRows() {
		return _rows; // Return the live list, not a defensive copy, so callers can mutate it directly
	}

	/**
	 * Replaces all data rows with the given list and marks this dataset modified.
	 *
	 * @param rows the new rows; null is treated as an empty list
	 */
	public void setRows(List<Object[]> rows) {
		_rows.clear(); // Discard whatever rows were previously stored
		if (rows != null) {
			_rows.addAll(rows); // Copy in the new rows, preserving their order
		}
		setModified(true); // Flag this object as changed so it gets re-saved
	}

	/**
	 * Persists this dataset's fields, including all data rows, to a child "HydrologyData"
	 * element under the given parent.
	 *
	 * @param parent the JDOM element to which the new "HydrologyData" element is appended
	 */
	public void saveData(Element parent) {
		Element myElem = new Element("HydrologyData"); // Create the root element for this dataset's data
		parent.addContent(myElem); // Attach it under the caller-supplied parent element

		XMLUtilities.saveNamedType(myElem, this); // Persist the inherited name/index fields

		// Persist the two simple string fields as child elements
		XMLUtilities.addChildContent(myElem, "Description", _description);
		XMLUtilities.addChildContent(myElem, "ImportedFilePath", _importedFilePath);

		Element rowsElem = new Element("Rows"); // Container element that holds every data row
		myElem.addContent(rowsElem); // Attach the rows container under this dataset's element
		for (Object[] row : _rows) { // Walk every row currently stored
			Element rowElem = new Element("Row"); // One element per row
			rowsElem.addContent(rowElem); // Attach the row under the rows container
			for (Object value : row) { // Walk every cell within the row, in column order
				Element cell = new Element("Cell"); // One element per cell
				cell.setText(value == null ? "" : value.toString()); // Store the cell's text, guarding against null
				rowElem.addContent(cell); // Attach the cell under its row
			}
		}
	}

	/**
	 * Restores this dataset's fields, including all data rows, from a "HydrologyData"
	 * element previously written by {@link #saveData(Element)}.
	 *
	 * @param myElem the "HydrologyData" element to load from
	 * @return true if the element was non-null and loading proceeded; false otherwise
	 */
	public boolean loadData(Element myElem) {
		if (myElem == null) {
			return false; // Nothing to load from; signal failure to the caller
		}

		XMLUtilities.loadNamedType(myElem, this); // Restore the inherited name/index fields
		// Restore the two simple string fields, defaulting to empty strings if missing
		_description = XMLUtilities.getChildElementAsString(myElem, "Description", "");
		_importedFilePath = XMLUtilities.getChildElementAsString(myElem, "ImportedFilePath", "");

		_rows.clear(); // Start from an empty row list before repopulating it
		Element rowsElem = myElem.getChild("Rows"); // Locate the rows container, if any
		if (rowsElem != null) {
			for (Object rowObj : rowsElem.getChildren("Row")) { // Walk every stored row element
				Element rowElem = (Element) rowObj; // JDOM returns raw Objects from getChildren(); cast back to Element
				List<?> cells = rowElem.getChildren("Cell"); // Grab this row's cell elements, in column order
				Object[] row = new Object[cells.size()]; // Allocate an array sized to match the number of cells
				for (int i = 0; i < cells.size(); i++) { // Walk each cell by index to preserve column order
					row[i] = ((Element) cells.get(i)).getText(); // Extract the cell's stored text
				}
				_rows.add(row); // Add the reconstructed row to this dataset
			}
		}

		return true; // Loading completed successfully
	}

	/**
	 * Returns this dataset's name, so it displays sensibly in combo boxes and lists.
	 *
	 * @return the dataset's name
	 */
	@Override
	public String toString() {
		return getName(); // Delegate to the inherited NamedType name accessor
	}
}
