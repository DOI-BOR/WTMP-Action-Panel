package usbr.wat.plugins.actionpanel.editors;

import java.util.ArrayList;                          // Resizable-array implementation of the List interface
import java.util.List;                               // Ordered collection interface used for DSS identifier lists

import javax.swing.JOptionPane;                      // Provides standard dialog boxes for user messages
import javax.swing.event.TableModelEvent;            // Event fired when the data within a table model changes

import com.rma.io.DssFileManagerImpl;                // RMA concrete implementation of the DSS file manager for reading time-series data
import com.rma.model.Project;                        // Represents the currently loaded RMA project and its data

import hec.heclib.util.HecTime;                     // HEC time representation used to store DSS time-range endpoints
import hec.io.DSSIdentifier;                         // Encapsulates a DSS file name and path for identifying a DSS record
import hec.io.TimeSeriesContainer;                   // Container holding a time-series dataset read from a DSS file
import hec.lang.NamedType;                           // Base interface for named objects passed into panel fill operations
import hec.model.RunTimeWindow;                      // Represents a start-to-end time window used for simulation runs

import usbr.wat.plugins.actionpanel.editors.iterationCompute.IterationBcPanel;      // Base panel class providing the BC (boundary condition) table and shared controls
import usbr.wat.plugins.actionpanel.editors.iterationCompute.PositionAnalysisPanel; // Parent panel that manages position-analysis settings, including the max element count

/**
 * Panel for managing boundary condition (BC) DSS inputs for a Position Analysis
 * iteration within the WTMP Action Panel.
 *
 * Extends IterationBcPanel to inherit the BC table and its standard controls,
 * and adds logic that listens for DSS identifier changes in the table. When a
 * DSS path is updated, it inspects the time range of every referenced DSS record
 * to determine the maximum number of annual elements available across all inputs,
 * then propagates that value to the parent PositionAnalysisPanel.
 *
 * If any DSS record has no data, an informational dialog is shown listing each
 * missing record so the user can investigate.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class PositionAnalysisBcPanel extends IterationBcPanel {

	// Reference to the parent panel that receives the calculated maximum element count
	private PositionAnalysisPanel _parentPanel;

	/**
	 * Constructs a PositionAnalysisBcPanel attached to the given editor dialog and parent panel.
	 * <p>
	 * Delegates core initialization to the IterationBcPanel superclass and stores a reference
	 * to the parent PositionAnalysisPanel for communicating the computed max element value.
	 *
	 * @param editor the EditIterationSettingsDialog that hosts this panel
	 * @param parent the PositionAnalysisPanel that will receive the max element count update
	 */
	public PositionAnalysisBcPanel(EditIterationSettingsDialog editor, PositionAnalysisPanel parent) {
		// Initialize the base IterationBcPanel with the editor dialog reference
		super(editor);

		// Store a reference to the parent panel for later max-element propagation
		_parentPanel = parent;
	}

	/**
	 * Attaches listeners to this panel's controls, extending the superclass listener setup.
	 *
	 * In addition to the listeners registered by IterationBcPanel, adds a TableModelListener
	 * on the BC table to detect when DSS identifiers are modified by the user.
	 */
	@Override
	protected void addListeners() {
		// Register the base class listeners on all shared controls
		super.addListeners();

		// Listen for any cell-level changes in the BC table model
		_bcTable.getModel().addTableModelListener(e -> tableModelChanged(e));
	}

	/**
	 * Responds to changes in the BC table model.
	 *
	 * Only triggers a max-element recalculation when the changed column is the
	 * DSS identifier column, since other column changes do not affect the time range.
	 *
	 * @param e the TableModelEvent describing which cell or range changed
	 */
	private void tableModelChanged(TableModelEvent e) {
		// Only recalculate when a DSS identifier cell has been modified
		if (e.getColumn() == DSSID_COL) {
			calculateMaxElements();
		}
	}

	/**
	 * Inspects the DSS time ranges for all BC table rows and computes the minimum
	 * number of annual elements available across all valid DSS records.
	 *
	 * For each row containing a fully specified DSSIdentifier, this method queries
	 * the DSS file manager for the record's time range. The number of whole years
	 * spanned by each record is computed, and the minimum across all records is
	 * taken as the maximum usable element count. This value is then sent to the
	 * parent PositionAnalysisPanel.
	 *
	 * If any record is found to have no data at all, its identifier is collected
	 * and the user is notified via an informational message dialog.
	 */
	private void calculateMaxElements() {
		// Get the number of rows currently in the BC table
		int numRows = _bcTable.getRowCount();

		Object cellObj;
		DSSIdentifier dssId, dssId2;
		HecTime[] times;
		int years;

		// Create a time window object for computing the year span of each DSS record
		RunTimeWindow rtw = new RunTimeWindow();

		// Start with the largest possible integer; will be reduced by each valid record
		int maxElement = Integer.MAX_VALUE;

		// Working DSS identifier used when resolving the absolute file path
		dssId2 = new DSSIdentifier();

		// Collect identifiers for any DSS records that have no data
		List<DSSIdentifier> missingDataDssIds = new ArrayList<>();

		// Iterate over every row in the table to inspect each DSS reference
		for (int r = 0; r < numRows; r++) {
			// Retrieve the cell value from the DSS identifier column
			cellObj = _bcTable.getValueAt(r, DSSID_COL);

			// Skip rows that do not contain a DSSIdentifier object
			if (!(cellObj instanceof DSSIdentifier)) {
				continue;
			}

			dssId = (DSSIdentifier) cellObj;

			// Skip rows where the file name or DSS path has not yet been set
			if (dssId.getFileName().isEmpty() || dssId.getDSSPath().isEmpty()) {
				continue;
			}

			// Copy the path and resolve the file name to an absolute project-relative path
			dssId2.setDSSPath(dssId.getDSSPath());
			dssId2.setFileName(Project.getCurrentProject().getAbsolutePath(dssId.getFileName()));

			// Query the DSS file manager for the start and end times of this record
			times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dssId2, 0);

			if (times != null && times.length == 2) {
				// Populate the time window with the retrieved start and end times
				rtw.setStartTime(times[0]);
				rtw.setEndTime(times[1]);

				// Calculate the number of full years spanned by this record
				years = rtw.getNumberOfYears();

				// Update the running minimum to reflect this record's available years
				maxElement = Math.min(maxElement, years);
			} else {
				// No time range was returned; attempt to read the time series directly to check for data
				TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId2, true);

				if (tsc == null || tsc.numberValues == 0) {
					// Record has no usable data; capture a copy of the identifier for reporting
					DSSIdentifier missingDssId = new DSSIdentifier(dssId2);
					missingDataDssIds.add(missingDssId);
				}
			}
		}

		// If at least one valid time range was found, propagate the minimum year count
		if (maxElement < Integer.MAX_VALUE) {
			_parentPanel.setMaxElement(maxElement);
		}

		// If any DSS records were missing data, build and show a notification message
		if (missingDataDssIds.size() > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append("No Data Found for \n:");

			// Append each missing DSS identifier to the message on its own line
			for (int i = 0; i < missingDataDssIds.size(); i++) {
				builder.append(missingDataDssIds.get(i));
				builder.append("\n");
			}

			// Display the missing-data notification to the user
			JOptionPane.showMessageDialog(this, builder.toString(), "Missing Data", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Populates the panel from the given named object, then recalculates the max
	 * element count to reflect any DSS records already present.
	 *
	 * Delegates initial form population to the superclass, then immediately triggers
	 * a DSS time-range scan so the parent panel is updated with the current max element.
	 *
	 * @param obj the NamedType object whose BC data should be loaded into the panel
	 */
	@Override
	public void fillPanel(NamedType obj) {
		// Populate the table rows using the base class implementation
		super.fillPanel(obj);

		// Recalculate the max element count based on any DSS records just loaded
		calculateMaxElements();
	}
}
