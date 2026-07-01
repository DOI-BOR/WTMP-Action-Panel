package usbr.wat.plugins.actionpanel.actions;

import java.awt.EventQueue;                                                         // AWT utility to schedule tasks on the Event Dispatch Thread (EDT)
import java.awt.event.ActionEvent;                                                  // Event type delivered when a user triggers a bound action (for example, button press)

import java.io.BufferedReader;                                                      // Reader for text input streams, used to capture process output
import java.io.File;                                                                // File I/O type representing filesystem paths
import java.io.IOException;                                                         // Exception type for I/O failures
import java.io.InputStreamReader;                                                   // Reader that converts byte streams to character streams
import java.text.SimpleDateFormat;                                                  // Date-time formatting utility used for report timestamps

import java.util.ArrayList;                                                         // Resizable list used to build command arguments and error messages
import java.util.Arrays;                                                            // Convenience methods for working with arrays
import java.util.Collections;                                                       // Utility for producing fixed-size, unmodifiable lists
import java.util.Date;                                                              // Date representation for last-computed times
import java.util.HashMap;                                                           // Map implementation used to store report parameters
import java.util.List;                                                              // Collections interface used for lists of report plugins and strings
import java.util.Map;                                                               // Map interface used for report parameters
import java.util.logging.Logger;                                                    // JDK logging API used for diagnostics and timing information

import javax.swing.AbstractAction;                                                  // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                                     // Swing utility for showing information and error dialogs

import org.jdom.Document;                                                           // JDOM document representation for XML content
import org.jdom.Element;                                                            // JDOM element representation for XML nodes

import org.python.google.common.io.Files;                                           // File utility used to move compiled files; note the nonstandard package path present in source

import com.rma.io.FileManagerImpl;                                                  // File manager implementation for reading, writing, and listing files
import com.rma.io.RmaFile;                                                          // Abstraction for a file within the RMA file system utilities
import com.rma.model.Project;                                                       // Accessor for the current project and project-level operations
import com.rma.util.XMLUtilities;                                                   // XML utility helpers for loading and saving JDOM documents
import rma.swing.RmaJDialog;                                                        // RMA dialog base class used to get the active dialog for message ownership
import rma.util.RMAFilenameFilter;                                                  // Filename filter used to select JRXML files for compilation
import rma.util.RMAIO;                                                              // RMA I/O utility helpers for path operations and safe concatenation

import hec2.plugin.model.ModelAlternative;                                          // WAT model type representing a modeling alternative used during reporting
import hec2.wat.event.MessageListener;                                              // Listener interface for messages produced during background operations
import hec2.wat.io.ProcessOutputReader;                                             // Threaded reader for process output streams, with listener support
import hec2.wat.model.WatSimulation;                                                // WAT model type representing a single simulation scenario or run

import net.sf.jasperreports.engine.JRException;                                     // JasperReports exception type used for load, compile, and fill operations
import net.sf.jasperreports.engine.JasperCompileManager;                            // JasperReports manager for compiling report designs
import net.sf.jasperreports.engine.JasperFillManager;                               // JasperReports manager for filling reports with data sources and parameters
import net.sf.jasperreports.engine.JasperPrint;                                     // JasperReports printable representation of a compiled and filled report
import net.sf.jasperreports.engine.JasperReport;                                    // JasperReports compiled report object
import net.sf.jasperreports.engine.SimpleJasperReportsContext;                      // JasperReports context used to register repository and persistence services
import net.sf.jasperreports.engine.data.JRXmlDataSource;                            // JasperReports XML data source used to feed data into reports
import net.sf.jasperreports.engine.design.JasperDesign;                             // JasperReports design object representing a report defined in JRXML
import net.sf.jasperreports.engine.util.JRLoader;                                   // JasperReports loader utility for compiled reports and resources
import net.sf.jasperreports.engine.util.JRXmlUtils;                                 // JasperReports XML utilities including DOM parsing helpers
import net.sf.jasperreports.engine.xml.JRXmlLoader;                                 // JasperReports loader for JRXML designs
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;           // JasperReports factory for file-based repository persistence services
import net.sf.jasperreports.repo.FileRepositoryService;                             // JasperReports file-based repository service implementation
import net.sf.jasperreports.repo.PersistenceServiceFactory;                         // JasperReports factory interface for persistence services
import net.sf.jasperreports.repo.RepositoryService;                                 // JasperReports repository service interface for locating report resources

import usbr.wat.plugins.actionpanel.io.OutputType;                                  // Enumeration defining report output types (for example, PDF)
import usbr.wat.plugins.actionpanel.io.ReportOptions;                               // Options bean for report generation settings
import usbr.wat.plugins.actionpanel.model.ReportPlugin;                             // Interface implemented by report actions to provide plugin metadata
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;                     // Model holding per-simulation report information and paths
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                                   // Base USBR panel type implemented by calibration and forecast panels

/**
 * Base class for actions that generate reports for WTMP simulations.
 *
 * Implements common behavior to compile JasperReports designs, fill reports
 * with XML data, invoke an external report generator, and manage parameters.
 * Subclasses provide specific action names and may extend the report workflow.
 *
 * This class also implements a message listener so it can capture and surface
 * errors emitted from background processes during report generation.
 */
@SuppressWarnings("serial")
public abstract class AbstractReportAction extends AbstractAction
		implements ReportPlugin, MessageListener {

	/** Logger used for diagnostics, timing information, and status messages. */
	private static Logger _logger = Logger.getLogger(AbstractReportAction.class.getName());

	/** Relative folder name under a simulation or study where reports are stored. */
	public static final String REPORT_DIR = "reports";

	/** Subfolder name under the report directory where Jasper resources are located. */
	public static final String JASPER_DIR = "jasper";

	/** File extension used by compiled Jasper report files. */
	public static final String JASPER_COMPILED_FILE_EXT = ".jasper";

	/** File extension for Jasper report source files (JRXML). */
	private static final String JASPER_SOURCE_FILE_EXT = "jrxml";

	/** Subfolder name containing XML data sources for reports. */
	public static final String DATA_SOURCES_DIR = "Datasources";

	/** Folder name within the installation where automated report components reside. */
	public static final String REPORT_INSTALL_FOLDER = "AutomatedReport";

	/** Executable name for the external report generator invoked by this action. */
	public static final String PYTHON_REPORT_BAT = "WAT_Report_Generator.exe";

	/** Default Jasper source file used as the primary report design. */
	public static final String JASPER_FILE = "USBR_Draft_Validation.jrxml";

	/** Folder name for observed data relative to the study directory. */
	public static final String OBS_DATA_FOLDER   = "shared";

	/** Parameter key for the watershed name used in Jasper report filling. */
	public static final String WATERSHED_NAME_PARAM = "watershedName";

	/** Parameter key for the simulation name used in Jasper report filling. */
	public static final String SIMULATION_NAME_PARAM = "simulationName";

	/** Parameter key for the analysis period start time. */
	public static final String ANALYSIS_START_TIME_PARAM = "analysisStartTime";

	/** Parameter key for the analysis period end time. */
	public static final String ANALYSIS_END_TIME_PARAM = "analysisEndTime";

	/** Parameter key for the simulation's last computed date formatted for display. */
	public static final String SIMULATION_LAST_COMPUTED_DATE_PARAM = "simulationDate";

	/** Parameter key that controls whether report headers and footers are printed. */
	public static final String PRINT_HEADER_FOOTER_PARAM = "printHeaderAndFooter";

	/** Parameter key for the study's report directory path. */
	private static final String REPORT_DIR_PARAM = "REPORT_DIR";

	/** Default XML data adapter file name within the study report structure. */
	public static final String XML_DATA_DOCUMENT = "USBRAutomatedReportDataAdapter.xml";

	/** Default XML output file name written per simulation run. */
	public static final String XML_DATA_OUTPUT = "USBRAutomatedReportOutput.xml";

	/** Parameter key for the WAT installation directory path. */
	private static final String WAT_INSTALL_DIR_PARAM = "Install_Dir";

	/** Parameter key indicating the location of the data adapter file. */
	private static final String DATA_ADAPTER_FILE_PARAM = "DataAdapterLocation";

	/** Parameter key indicating the simulation run's report directory path. */
	private static final String SIM_REPORT_DIR_PARAM = "RUN_DIR";

	/** Collector for error messages reported by background processes. */
	private List<String> _errMsgs= new ArrayList<>();

	/** Parent panel used for retrieving simulation report information. */
	private UsbrPanel _parentPanel;

	/**
	 * Creates an abstract report action with a display name and parent panel reference.
	 *
	 * @param name the user-visible action name
	 * @param parentPanel the owning panel that provides simulation report information
	 */
	public AbstractReportAction(String name, UsbrPanel parentPanel) {
		// Initialize the Swing action with the provided name
		super(name);

		// Store the parent panel for use during report generation
		_parentPanel = parentPanel;
	}

	/**
	 * Triggers report generation with default options when the action is performed.
	 *
	 * Creates a PDF report for the simulations provided by the parent panel and
	 * hands off to the report creation workflow implemented by subclasses.
	 *
	 * @param e the action event initiating report generation
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Initialize default report options
		ReportOptions options = new ReportOptions();

		// Use PDF as the output type by default
		options.setOutputType(OutputType.PDF);

		// Generate reports for all simulation infos provided by the parent panel
		createReport(_parentPanel.getSimulationReportInfos(), options );
	}

	/**
	 * Runs the external report generator executable with the provided XML file.
	 *
	 * Builds a process command, launches it in the installation directory, captures
	 * output for error reporting, and returns success or failure.
	 *
	 * @param reportXmlFile path to the XML input file for the report generator
	 * @return true if the process completed with exit code 0, false otherwise
	 */
	protected boolean runPythonScript(String reportXmlFile) {
		// Clear any previously collected error messages
		_errMsgs.clear();

		// Capture start time for performance logging
		long t1 = System.currentTimeMillis();

		try {
			// Allow skipping the external generator via a system property
			if ( Boolean.getBoolean("SkipPythonReport")) {
				return true;
			}

			// Build the process command list
			List<String>cmdList = new ArrayList<>();

			// Determine the directory where the report generator resides
			String dir = getDirectoryToUse();

			// Full path to the report-generator executable
			String exeFile = RMAIO.concatPath(dir, PYTHON_REPORT_BAT);

			// Add executable and its XML argument to the command
			cmdList.add(exeFile);
			cmdList.add(reportXmlFile);

			// Launch the process and return success based on its exit value
			return runProcess(cmdList, dir);

		} finally {
			// Log the elapsed time for launching the report generator
			long t2 = System.currentTimeMillis();
			_logger.info("runPythonScript:time to run python for "+reportXmlFile+" is "+(t2-t1)+"ms");
		}
	}

	/**
	 * Determines the installation directory used to locate report resources.
	 *
	 * Prefers the WAT.InstallDir system property. If not set, falls back to the
	 * user directory and logs which directory is being used.
	 *
	 * @return the absolute path to the report installation directory
	 */
	public static String getDirectoryToUse() {
		// Preferred directory from system property
		String dir = System.getProperty("WAT.InstallDir", null);

		// Fallback to the current user directory if not specified
		if ( dir == null || dir.isEmpty()) {
			dir = System.getProperty("user.dir");

			_logger.info("getDirectoryToUse:WAT.InstallDir not set using "+dir);

		} else {
			_logger.info("getDirectoryToUse:WAT.InstallDir set to "+dir);
		}

		// Append the automated report subfolder
		dir = RMAIO.concatPath(dir, REPORT_INSTALL_FOLDER);

		return dir;
	}

	/**
	 * Launches an external process in the specified directory and streams output.
	 *
	 * Creates a {@link ProcessBuilder}, sets the working directory, starts the process,
	 * streams both stdout and stderr through {@link ProcessOutputReader} with this
	 * instance as a listener, and reports success via the exit code.
	 *
	 * @param cmdList the command and arguments to execute
	 * @param runInFolder the directory in which to run the process
	 * @return true if the process exited with code 0, false otherwise
	 */
	protected boolean runProcess(List<String> cmdList, String runInFolder) {
		// Copy command list into an array for ProcessBuilder
		String[] cmdArray = new String[cmdList.size()];

		cmdList.toArray(cmdArray);

		// Construct the process builder for the provided command
		ProcessBuilder procBuilder = new ProcessBuilder(cmdArray);

		// Ensure the working directory exists
		File f = new File(runInFolder);

		if (!f.exists()) {
			f.mkdirs();
		}

		// Set the working directory for the process
		procBuilder.directory(f);

		try {
			_logger.info("runProcess:launching in folder:"+runInFolder);

			_logger.info("runProcess:launching: "+cmdList);

			// Start the process
			Process proc = procBuilder.start();

			// Create a reader for stderr
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			// Stream stderr with echo and listener callbacks
			ProcessOutputReader preader1 = new ProcessOutputReader(reader1, true, proc);
			preader1.addListener(this);
			preader1.setEchoOutput(true);
			preader1.start();

			// Create a reader for stdout
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			// Stream stdout with echo
			ProcessOutputReader preader2 = new ProcessOutputReader(reader2, false, proc);
			preader2.setEchoOutput(true);
			preader2.start();

			// Wait for the process to complete
			int rv = proc.waitFor();

			_logger.info("runProcess:rv="+rv);

			// If the process failed and there are error messages, show them to the user
			if ( rv != 0 && !_errMsgs.isEmpty()) {
				StringBuilder builder = new StringBuilder();

				// Note: original code builds an HTML string; we preserve it as-is
				builder.append("<html>");

				for(int i = 0; i < _errMsgs.size();i ++ ) {
					builder.append(_errMsgs.get(i));
					builder.append("<br>");
				}

				// Use the active dialog as the message owner
				RmaJDialog parent = RmaJDialog.getActiveDialog();

				// Display the error message on the EDT
				EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent, builder.toString(), "Error Running Report", JOptionPane.ERROR_MESSAGE));
			}

			// Success when exit code is zero
			return rv == 0;

		} catch (IOException | InterruptedException e) {
			// Log and propagate failure
			e.printStackTrace();

			return false;
		}


	}

	/**
	 * Populates Jasper report parameters based on project and simulation metadata.
	 *
	 * Adds project name, simulation name, analysis window, last computed date,
	 * directory paths for reporting, and the location of the data adapter file.
	 *
	 * @param params map to populate with parameter values
	 * @param jasperRepoDir repository directory used within Jasper
	 * @param sim simulation report info containing names and paths
	 * @param options report options controlling headers, footers, and output type
	 */
	protected void setParameters(Map<String, Object> params, String jasperRepoDir,
	                             SimulationReportInfo sim, ReportOptions options) {
		// Path to the Jasper report folder
		params.put("p_ReportFolder", jasperRepoDir);

		// Watershed/project name
		params.put(WATERSHED_NAME_PARAM, Project.getCurrentProject().getName());

		// Simulation name
		params.put(SIMULATION_NAME_PARAM, sim.getName());

		// Analysis start and end times
		params.put(ANALYSIS_START_TIME_PARAM, sim.getSimulation().getRunTimeWindow().getStartTime().toString());
		params.put(ANALYSIS_END_TIME_PARAM, sim.getSimulation().getRunTimeWindow().getEndTime().toString());

		// Control whether headers and footers are printed
		params.put(PRINT_HEADER_FOOTER_PARAM, options.shouldPrintHeadersFooters());

		// Study-level report directory
		String reportDir = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(),REPORT_DIR);
		params.put(REPORT_DIR_PARAM, reportDir);

		// Simulation run's report directory
		String simReportDir = RMAIO.concatPath(sim.getSimFolder(), REPORT_DIR);
		params.put(SIM_REPORT_DIR_PARAM, simReportDir);

		// Install directory resolved from user.dir
		String installDir= System.getProperty("user.dir");

		installDir = RMAIO.getDirectoryFromPath(installDir);
		params.put(WAT_INSTALL_DIR_PARAM, installDir);

		// Last computed date formatted for display
		Date date = new Date(sim.getLastComputedDate());

		SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
		params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));

		// Location of the data adapter file at the study level
		String dataAdapterFile = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(), REPORT_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, DATA_SOURCES_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, XML_DATA_DOCUMENT);
		params.put(DATA_ADAPTER_FILE_PARAM, dataAdapterFile);

		_logger.fine("Report Parameters are:"+params);
	}

	/**
	 * Returns the absolute path to the observed data folder relative to the study.
	 *
	 * @param studyDir absolute path to the study directory
	 * @return absolute path to the observed data folder
	 */
	protected String getObsDataPath(String studyDir) {
		return RMAIO.concatPath(studyDir, OBS_DATA_FOLDER);
	}

	/**
	 * Returns the relative Jasper folder path under the report directory.
	 *
	 * @return relative path to the Jasper folder
	 */
	protected String getJasperRelativeFolder() {
		String jasperReportFolder = RMAIO.concatPath(REPORT_DIR, JASPER_DIR);
		return jasperReportFolder;
	}

	/**
	 * Edits the study-level data adapter file and writes an updated copy into the simulation run folder.
	 *
	 * Reads the XML adapter, updates the output location to point at the simulation's
	 * DataSources folder, and saves the modified adapter into the simulation folder.
	 *
	 * @param simRunFolder absolute path to the simulation run folder
	 * @return true if the adapter was successfully saved, false otherwise
	 */
	protected boolean editDataAdapterFile(String simRunFolder) {
		// Study directory
		String studyDir = Project.getCurrentProject().getProjectDirectory();

		// Path to study-level DataSources folder
		String dataSourceFolder = RMAIO.concatPath(studyDir, REPORT_DIR);
		dataSourceFolder = RMAIO.concatPath(dataSourceFolder, DATA_SOURCES_DIR);

		// Absolute path to the study-level data adapter file
		String dataAdapterPath = RMAIO.concatPath(dataSourceFolder, XML_DATA_DOCUMENT);

		// Open the adapter file via the RMA file manager
		RmaFile dataAdapterFile = FileManagerImpl.getFileManager().getFile(dataAdapterPath);

		// Load the XML document
		Document dataAdapterDoc = XMLUtilities.loadDocument(dataAdapterFile);

		if ( dataAdapterDoc == null ) {
			_logger.info("failed to read " + dataAdapterFile.getAbsolutePath());

			return false;
		}

		// Navigate to dataFile element
		Element root = dataAdapterDoc.getRootElement();
		Element dataFileElem = root.getChild("dataFile");

		if ( dataFileElem == null ) {
			_logger.info("failed to find dataFile element in " + dataAdapterFile.getAbsolutePath());
			return false;
		}

		// Navigate to location element
		Element locationElem = dataFileElem.getChild("location");
		if (locationElem == null ) {
			_logger.info("failed to find Location element in " + dataAdapterFile.getAbsolutePath());
			return false;
		}

		// Compose the simulation DataSources folder
		String simFolder = RMAIO.concatPath(simRunFolder, REPORT_DIR);
		simFolder = RMAIO.concatPath(simFolder, DATA_SOURCES_DIR);

		// Update the adapter to point at the per-simulation output file
		String newOutputLocation = RMAIO.concatPath(simFolder, XML_DATA_OUTPUT);
		locationElem.setText(newOutputLocation);

		// Save the modified adapter into the simulation DataSources folder
		String newAdapterLocation = RMAIO.concatPath(simFolder, XML_DATA_DOCUMENT);
		RmaFile newDataAdapterFile = FileManagerImpl.getFileManager().getFile(newAdapterLocation);

		return XMLUtilities.saveDocument(dataAdapterDoc, newDataAdapterFile);

	}

	/**
	 * Fills a Jasper report for a single simulation using the XML data adapter.
	 *
	 * Configures repository services for study-level Jasper resources and
	 * simulation-level data sources, loads the compiled Jasper file, and fills it.
	 *
	 * @param context JasperReports context used to register repositories
	 * @param studyDir absolute path to the study directory
	 * @param installDir absolute path to the installation directory
	 * @param jasperReportFolder relative Jasper folder path under report directory
	 * @param info simulation report information for file paths and names
	 * @param options report options controlling headers, footers, and output type
	 * @return a filled {@link JasperPrint} when successful, otherwise null
	 */
	protected JasperPrint fillReport(SimpleJasperReportsContext context, String studyDir, String installDir,
	                                 String jasperReportFolder,  SimulationReportInfo info, ReportOptions options ) {
		// Capture start time for performance logging
		long t1 = System.currentTimeMillis();

		try {
			// Study-level Jasper compiled files directory (suffix "C")
			String studyJasperDir = RMAIO.concatPath(studyDir, getJasperRelativeFolder());
			studyJasperDir = studyJasperDir+"C";

			// Repository service for compiled Jasper files
			FileRepositoryService jasperFileRepository = new FileRepositoryService(context, studyJasperDir, true);

			// Simulation-level DataSources directory
			String simReportsDir = RMAIO.concatPath(info.getSimFolder(), REPORT_DIR);
			simReportsDir = RMAIO.concatPath(simReportsDir, DATA_SOURCES_DIR);

			// Repository service for XML data sources
			FileRepositoryService reportsFileRepository = new FileRepositoryService(context, simReportsDir, true);

			// Register repository services with the Jasper context
			context.setExtensions(RepositoryService.class, Arrays.asList(jasperFileRepository, reportsFileRepository));

			// Register persistence services with the Jasper context
			context.setExtensions(PersistenceServiceFactory.class, Collections.singletonList(FileRepositoryPersistenceServiceFactory.getInstance()));

			// Placeholder for the compiled Jasper report
			JasperReport jasperReport;

			// Full path to the compiled Jasper file
			String inJasperFile = null;

			try {
				// Derive the compiled file name from the JRXML source
				int idx = JASPER_FILE.lastIndexOf('.');

				String jasperCompiledFile = JASPER_FILE.substring(0,idx);
				jasperCompiledFile = jasperCompiledFile.concat(".jasper");

				inJasperFile = RMAIO.concatPath(studyJasperDir, jasperCompiledFile);

				// Load the compiled report object
				jasperReport = (JasperReport)JRLoader.loadObject(new File(inJasperFile));

			}

			catch (JRException e) {
				// Loading the compiled report failed
				e.printStackTrace();

				return null;
			}

			// Prepare parameters for filling
			Map<String, Object>params = new HashMap<>();
			setParameters(params, jasperReportFolder, info, options);

			// Path to the simulation-level XML data adapter
			String xmlDataDoc = RMAIO.concatPath(info.getSimFolder(), REPORT_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, DATA_SOURCES_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, XML_DATA_DOCUMENT);

			// Fill the compiled report using the XML data source
			JasperPrint jasperPrint;

			_logger.info("fillReport:filling report "+inJasperFile+ " DataSource="+xmlDataDoc);

			JRXmlDataSource dataSource;

			try {
				// Load and parse the XML data adapter via the Jasper context
				dataSource = new JRXmlDataSource(context, JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataDoc)));
			} catch (JRException e1) {
				// Data source parsing failed
				e1.printStackTrace();

				return null;
			}

			// Validate data source
			if ( dataSource == null ) {
				_logger.info("fillReport:failed to load DataAdapter file "+xmlDataDoc);

				return null;
			}

			try {
				// Fill the report with parameters and XML data
				jasperPrint = JasperFillManager.getInstance(context).fill(jasperReport, params, dataSource);

				return jasperPrint;

			} catch (JRException e) {
				// Filling failed
				e.printStackTrace();

				return null;
			}
		} finally {
			// Log elapsed time for filling
			long t2 = System.currentTimeMillis();

			_logger.info("fillReport:time to fill jasper report for "+info.getName()+ "is "+(t2-t1)+"ms");
		}
	}

	/**
	 * Compiles Jasper report source files and writes compiled outputs to the destination.
	 *
	 * Scans the installation and study folders for JRXML files, compiles those that
	 * need recompilation or when forced, and moves the compiled files into the target
	 * compiled folder.
	 *
	 * @param studyDir absolute path to the study directory
	 * @param installDir absolute path to the installation directory
	 * @param jasperRelDir relative path to the Jasper folder under the report directory
	 * @return true if all compilation tasks succeeded, false otherwise
	 */
	protected boolean compileJasperFiles(String studyDir, String installDir, String jasperRelDir) {
		// Capture start time for performance logging
		long t1 = System.currentTimeMillis();

		// Filter used to select JRXML files
		RMAFilenameFilter filter= new RMAFilenameFilter(JASPER_SOURCE_FILE_EXT);
		filter.setAcceptDirectories(false);

		// Paths to installation and study Jasper folders
		String jasperInstallFolder = RMAIO.concatPath(installDir, jasperRelDir);
		String jasperStudyFolder = RMAIO.concatPath(studyDir, jasperRelDir); // reports/JasperC

		// Collection of JRXML file paths
		List<String>jasperFiles;

		// Temporary variables for file names
		String srcFile, destFile;

		// System property to always force recompilation
		boolean alwaysCompile = Boolean.getBoolean("CompileJasperFiles");

		_logger.info("compileJasperFiles:report repositories: install:"+jasperInstallFolder+" study:"+jasperStudyFolder);

		boolean success = true;

		// Prefer compiling from installation folder when available
		if ( FileManagerImpl.getFileManager().fileExists(jasperInstallFolder)) {
			// List installation JRXML files
			jasperFiles = FileManagerImpl.getFileManager().list(jasperInstallFolder, filter);

			for(int i = 0; i < jasperFiles.size();i++ ) {
				srcFile = jasperFiles.get(i);

				// Get just the file name from the path
				String srcFileName = RMAIO.getFileFromPath(srcFile);

				// Determine whether to compile from study or installation copy
				String fileToCompile = findSourceFile(srcFileName, jasperStudyFolder, jasperInstallFolder);

				// Target study file path for record-keeping (not used directly in compile)
				String studyFile = RMAIO.concatPath(jasperStudyFolder, srcFileName);

				if ( compileJasperFile(fileToCompile, alwaysCompile)== null ) {
					success = false;
				}
			}

		} else {
			// No installation Jasper files; compile from study folder contents
			jasperFiles = FileManagerImpl.getFileManager().list(jasperStudyFolder, filter);

			for (int i = 0;i < jasperFiles.size(); i++ ) {
				srcFile = jasperFiles.get(i);

				if ( compileJasperFile(srcFile, alwaysCompile) == null ) {
					success = false;
				}
			}
		}

		// Log elapsed time for compilation
		long t2 = System.currentTimeMillis();
		_logger.info("time to compile jasper files is " + (t2-t1)+" ms.");

		return success;

	}

	/**
	 * Chooses the JRXML source file from either study or installation folders.
	 *
	 * Prefers the study copy when present; otherwise uses the installation copy.
	 *
	 * @param jasperFileName file name of the JRXML source
	 * @param jasperStudyFolder absolute path to the study Jasper folder
	 * @param jasperInstallFolder absolute path to the installation Jasper folder
	 * @return absolute path to the selected JRXML source file
	 */
	protected String findSourceFile(String jasperFileName, String jasperStudyFolder, String jasperInstallFolder) {
		// Study-level file path
		String studyFile = RMAIO.concatPath(jasperStudyFolder, jasperFileName);

		// Prefer study file when available
		if ( FileManagerImpl.getFileManager().fileExists(studyFile)) {
			return studyFile;

		} else {
			// Fall back to installation file
			return RMAIO.concatPath(jasperInstallFolder, jasperFileName);
		}
	}

	/**
	 * Compiles a single JRXML file when required and returns the destination compiled path.
	 *
	 * Uses JasperCompileManager to compile the source, then moves the compiled .jasper
	 * to the destination. When compilation is not needed, returns the existing destination.
	 *
	 * @param jasperFile absolute path to the JRXML source file
	 * @param alwaysCompile true to force compilation regardless of timestamps
	 * @return destination compiled file path, or null on failure
	 */
	private String compileJasperFile(String jasperFile, boolean alwaysCompile) {
		// Destination .jasper file path
		String destFile = getJasperDestFile(jasperFile);

		// Determine whether compilation is required
		if ( needsToCompile(jasperFile, destFile) || alwaysCompile ) {
			_logger.info("compileJasperFile:compiling "+jasperFile +" to "+destFile);

			try {
				// Load the JRXML design (primarily to validate availability)
				JasperDesign design = JRXmlLoader.load(jasperFile);

				// Compile the JRXML to a temporary .jasper file and get its path
				String rv = JasperCompileManager.compileReportToFile(jasperFile);

				if ( rv != null ) {
					try {
						// Move the compiled .jasper file to the destination folder
						Files.move(new File(rv), new File(destFile));
						_logger.info("compileJasperFiles: compiled to "+destFile);

					} catch (IOException e) {
						_logger.info("compileJasperFile:failed to move file "+e);
						return null;
					}

					return destFile;
				}

				// Compilation did not produce an output path
				return null;

			} catch (JRException e) {
				// Compilation failed
				e.printStackTrace();
				return null;
			}
		}

		// No compilation needed; return the destination path
		return destFile;
	}

	/**
	 * Determines whether a JRXML source must be compiled based on timestamps.
	 *
	 * @param src absolute path to the JRXML source file
	 * @param dest absolute path to the compiled .jasper file
	 * @return true if compilation is needed, false otherwise
	 */
	private static boolean needsToCompile(String src, String dest) {
		// Wrap paths with RmaFile for filesystem checks
		RmaFile srcFile = FileManagerImpl.getFileManager().getFile(src);
		RmaFile destFile = FileManagerImpl.getFileManager().getFile(dest);

		// If compiled file exists, compare timestamps
		if ( destFile.exists() ) {
			if ( srcFile.lastModified() > destFile.lastModified() ) {
				return true;
			}

			return false;
		}

		// No compiled file present means compilation is needed
		return true;
	}

	/**
	 * Computes the destination compiled file path for a given JRXML source.
	 *
	 * Depending on a system property, the compiled file is either written to the
	 * same folder as the source or to the study's JasperC folder.
	 *
	 * @param srcFile absolute path to the JRXML source file
	 * @return absolute path to the compiled .jasper file, or null when unsupported
	 */
	private String getJasperDestFile(String srcFile) {
		// Compile to the same folder as the source when the property is set
		if ( Boolean.getBoolean("JasperCompilesToSameFolder")) {
			int idx = srcFile.lastIndexOf('.');

			if ( idx > -1 ) {
				String destFile = srcFile.substring(0,idx);
				destFile = destFile.concat(JASPER_COMPILED_FILE_EXT);
				return destFile;
			}

		} else {
			// Otherwise, compile to the study's JasperC folder
			String srcFileName = RMAIO.getFileNameNoExtension(srcFile);
			String    destFile = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(), getJasperRelativeFolder()+"C");

			destFile = RMAIO.concatPath(destFile, srcFileName);
			destFile = destFile.concat(JASPER_COMPILED_FILE_EXT);

			return destFile;
		}

		// Unable to compute destination path
		return null;
	}

	/**
	 * Computes the sanitized F-part string for Python report generation.
	 *
	 * Uses the simulation and model alternative to obtain the F-part and then
	 * converts it to a filesystem-safe string.
	 *
	 * @param sim the simulation providing the F-part
	 * @param modelAlt the model alternative associated with the simulation
	 * @return a filesystem-safe F-part string
	 */
	protected String findFpartForPython(WatSimulation sim, ModelAlternative modelAlt) {
		// Retrieve F-part from the simulation
		String fpart = sim.getFPart(modelAlt);

		// Sanitize F-part for use as a filename
		return RMAIO.userNameToFileName(fpart);
	}

	/**
	 * Receives messages emitted from {@link ProcessOutputReader} and records them as errors.
	 *
	 * @param msg the message captured from process output
	 */
	public void messageRecieved(String msg) {
		_errMsgs.add(msg);
	}
}