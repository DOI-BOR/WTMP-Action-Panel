package usbr.wat.plugins.actionpanel.model;

import hec2.wat.model.WatSimulation; // WAT simulation object associated with this report entry

/**
 * Data transfer object carrying all information needed to generate a report entry
 * for a single simulation or saved result set within the WTMP Action Panel.
 *
 * Used by ReportPlugin implementations to access the simulation's output folder,
 * DSS file, computed result metadata, and group membership when producing
 * comparison or iteration reports.
 *
 * Distinguishes between live simulations (_isSimulation = true) and previously
 * saved result snapshots (_isSimulation = false) via the setIsSimulation() flag.
 *
 */
public class SimulationReportInfo {
	// The display name for this entry in the report
	private String _name;

	// An abbreviated name used in report column headers or labels where space is limited
	private String _shortName;

	// A descriptive text for this simulation or result set in the report
	private String _description;

	// The WAT simulation associated with this report entry (may be null for saved results)
	private WatSimulation _sim;

	// Absolute path to the simulation's output folder
	private String _simFolder;

	// Absolute path to the simulation's output DSS file
	private String _simDssFile;

	// Human-readable string representation of the last computed date/time
	private String _lastComputedDate;

	// True if this entry represents a live simulation; false for a saved results snapshot
	private boolean _isSimulation;

	// Absolute path to the report CSV data file associated with this entry
	private String _reportCsvFile;

	// The simulation group that this simulation belongs to
	private AbstractSimulationGroup _simulationGroup;

	/**
	 * Constructs an empty SimulationReportInfo; all fields must be populated via setters.
	 */
	public SimulationReportInfo() {
		super();
	}

	/**
	 * Returns the display name for this entry in the report.
	 *
	 * @return the report display name string
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Returns the description for this simulation or result set.
	 *
	 * @return the description string
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * Returns the WAT simulation associated with this report entry.
	 *
	 * @return the WatSimulation, or null for saved-results entries
	 */
	public WatSimulation getSimulation() {
		return _sim;
	}

	/**
	 * Returns the absolute path to the simulation's output folder.
	 *
	 * @return the simulation output folder path string
	 */
	public String getSimFolder() {
		return _simFolder;
	}

	/**
	 * Returns the absolute path to the simulation's output DSS file.
	 *
	 * @return the simulation output DSS file path string
	 */
	public String getSimDssFile() {
		return _simDssFile;
	}

	/**
	 * Returns the human-readable string representation of the last computed date/time.
	 *
	 * @return the last computed date string, or null if not set
	 */
	public String getLastComputedDate() {
		return _lastComputedDate;
	}

	/**
	 * Sets the WAT simulation for this report entry.
	 *
	 * @param sim the WatSimulation to associate with this entry
	 */
	public void setSimulation(WatSimulation sim) {
		_sim = sim;
	}

	/**
	 * Sets the absolute path to the simulation's output DSS file.
	 *
	 * @param simulationDssFile the output DSS file path string
	 */
	public void setSimDssFile(String simulationDssFile) {
		_simDssFile = simulationDssFile;
	}

	/**
	 * Sets the absolute path to the simulation's output folder.
	 *
	 * @param simulationDirectory the output folder path string
	 */
	public void setSimFolder(String simulationDirectory) {
		_simFolder = simulationDirectory;
	}

	/**
	 * Sets the display name for this report entry.
	 *
	 * @param name the report display name string
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * Sets the description for this simulation or result set.
	 *
	 * @param desc the description string
	 */
	public void setDescription(String desc) {
		_description = desc;
	}

	/**
	 * Sets the human-readable string for the last computed date/time.
	 *
	 * @param lastComputedDate the date/time string to display
	 */
	public void setLastComputedDate(String lastComputedDate) {
		_lastComputedDate = lastComputedDate;
	}

	/**
	 * Returns the display name used as the string representation of this entry
	 * (e.g., in combo boxes or list displays).
	 *
	 * @return the display name string
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Sets whether this entry represents a live simulation or a saved results snapshot.
	 *
	 * @param isSim true if this is a live simulation; false if it is a saved results snapshot
	 */
	public void setIsSimulation(boolean isSim) {
		_isSimulation = isSim;
	}

	/**
	 * Returns whether this entry represents a live simulation.
	 *
	 * @return true for live simulations; false for saved results snapshots
	 */
	public boolean isSimulation() {
		return _isSimulation;
	}

	/**
	 * Sets the abbreviated name used in report column headers or space-constrained labels.
	 *
	 * @param name the short name string
	 */
	public void setShortName(String name) {
		_shortName = name;
	}

	/**
	 * Returns the abbreviated name for use in compact report contexts.
	 *
	 * @return the short name string, or null if not set
	 */
	public String getShortName() {
		return _shortName;
	}

	/**
	 * Sets the absolute path to the report CSV data file for this entry.
	 *
	 * @param reportCsv the report CSV file path string
	 */
	public void setReportCsvFile(String reportCsv) {
		_reportCsvFile = reportCsv;
	}

	/**
	 * Returns the absolute path to the report CSV data file for this entry.
	 *
	 * @return the report CSV file path string, or null if not set
	 */
	public String getReportCsvFile() {
		return _reportCsvFile;
	}

	/**
	 * Returns the simulation group that contains the simulation for this entry.
	 *
	 * @return the AbstractSimulationGroup, or null if not associated with a group
	 */
	public AbstractSimulationGroup getSimulationGroup() {
		return _simulationGroup;
	}

	/**
	 * Sets the simulation group that contains the simulation for this entry.
	 *
	 * @param simGroup the AbstractSimulationGroup to associate with this entry
	 */
	public void setSimulationGroup(AbstractSimulationGroup simGroup) {
		_simulationGroup = simGroup;
	}
}
