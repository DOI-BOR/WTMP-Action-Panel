package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;                                      // Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.Collections;                                           // Utility for sorting lists and other collection helpers
import java.util.List;                                                  // Collections interface used for lists of simulation report info

import javax.swing.AbstractAction;                                      // Swing base class for encapsulating an action attached to UI components

import hec.io.FileManagerImpl;                                          // File manager from HEC utilities used to list files via filters

import rma.util.RMAFilenameFilter;                                      // Filename filter used to match a single extension (e.g., pdf)
import rma.util.RMAFilenameFilterSet;                                   // Set of filename filters combined under one descriptor
import rma.util.RMAIO;                                                  // RMA I/O utility helpers for path operations and safe concatenation

import usbr.wat.plugins.actionpanel.io.OutputType;                      // Enumeration of supported output types and their file extensions
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;         // Model holding per-simulation report information and paths
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                       // Base USBR panel type implemented by calibration and forecast panels

/**
 * Action that locates and displays the latest report file for one or more simulations.
 *
 * When invoked, it scans each selected simulation’s reports folder, finds the most
 * recent report using supported output types, and asks the parent panel to display it.
 */
@SuppressWarnings("serial")
public class DisplayReportAction extends AbstractAction {
	/** Panel providing simulation report info and display capabilities. */
	private UsbrPanel _parent;

	/**
	 * Creates the display-report action with a user-visible name.
	 *
	 * @param parent the panel used to obtain simulation report info and display files
	 */
	public DisplayReportAction(UsbrPanel parent) {
		// Initialize the action with its display label
		super("Display Report...");

		// Store the parent panel reference
		_parent = parent;
	}

	/**
	 * Handles the user-triggered event to display reports.
	 *
	 * @param e the action event initiating the display request
	 */
	public void actionPerformed(ActionEvent e) {
		// Delegate to the display workflow for all selected simulations
		displayReportAction();
	}

	/**
	 * Displays the latest report for each available simulation report info from the parent.
	 *
	 * Iterates the simulation report infos, derives each simulation’s folder,
	 * and attempts to display the most recent report found.
	 */
	public void displayReportAction() {
		// Obtain report infos from the parent panel
		List<SimulationReportInfo> simInfos = _parent.getSimulationReportInfos();

		// No-op if none are available
		if (simInfos == null || simInfos.isEmpty()) {
			return;
		}

		// For each simulation, locate and display the most recent report
		for (int i = 0; i < simInfos.size(); i++) {
			SimulationReportInfo simInfo = simInfos.get(i);
			displayReportAction(simInfo.getSimFolder());
		}
	}

	/**
	 * Displays the latest report for a single simulation directory, if present.
	 *
	 * @param simulationDirectory the absolute path to the simulation folder
	 */
	public void displayReportAction(String simulationDirectory) {
		// Proceed only when the folder path is provided
		if ( simulationDirectory != null ) {
			// Build the path to the simulation's reports directory
			String rptDir = RMAIO.concatPath(simulationDirectory, BaseReportAction.REPORT_DIR);

			// Find the most recent report file in that directory
			String latestFile = findLatestReportFile(rptDir);

			// Display the file via the parent panel
			if ( latestFile != null ) {
				_parent.displayFile(latestFile);
			}
		}
	}

	/**
	 * Scans the folder for supported report types and returns the most recent file.
	 *
	 * Supported types are taken from {@link OutputType} values. Files are sorted,
	 * the latest entry is selected, and temporary files (e.g., names starting with "~$")
	 * are skipped.
	 *
	 * @param folder the reports folder to scan
	 * @return the path to the latest report file, or null if none found
	 */
	private static String findLatestReportFile(String folder) {
		// Determine supported output extensions
		OutputType[] outputTypes = OutputType.values();

		// Build a filter set for all supported report file extensions
		RMAFilenameFilterSet filterSet = new RMAFilenameFilterSet("report file types");

		for (int i = 0; i < outputTypes.length; i++) {
			// Convert the extension to a format expected by the filter (strip leading dot)
			String ext = outputTypes[i].getFileExtension();

			if ( ext.startsWith(".")) {
				ext = ext.substring(1);
			}

			// Create and configure a filter for this extension
			RMAFilenameFilter filter = new RMAFilenameFilter(ext);

			filter.setAcceptDirectories(false);

			// Add the filter to the set
			filterSet.addFilter(filter);

		}

		// List report files matching any of the filters (non-recursive)
		List<String> reportFiles = FileManagerImpl.getFileManager().list(folder, filterSet, false);

		// Return the latest non-temporary file if any are found
		if ( reportFiles != null && !reportFiles.isEmpty()) {
			int offset = 1;

			// Sort to get chronological or lexicographic order as needed
			Collections.sort(reportFiles);
			String fullPath, fileName;

			// Walk backward from the end, skipping temporary files
			do {
				fullPath = reportFiles.get(reportFiles.size()-offset);
				fileName = RMAIO.getFileFromPath(fullPath);
				offset++;
			}

			while ( fileName.startsWith("~$"));

			return fullPath;
		}

		// No files found
		return null;

	}
}