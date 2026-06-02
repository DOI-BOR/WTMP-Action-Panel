package usbr.wat.plugins.actionpanel.actions;

import java.io.BufferedReader;                                              // Reader for text input streams, used to capture process output from external tools
import java.io.File;                                                        // File I/O type representing filesystem paths and directories
import java.io.IOException;                                                 // Exception type for I/O failures during file and process operations
import java.io.InputStreamReader;                                           // Reader that converts byte streams to character streams for process output

import java.util.ArrayList;                                                 // Resizable list used to build command arguments
import java.util.Collections;                                               // Utility for producing fixed-size, unmodifiable lists and helpers
import java.util.List;                                                      // Collections interface used for lists of simulations
import java.util.Map;                                                       // Map interface used to pass report parameters into JasperReports

import javax.swing.AbstractAction;                                          // Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;                                             // Swing utility for showing information dialogs and error messages

import com.rma.io.FileManagerImpl;                                          // File manager implementation for reading, writing, and listing files
import com.rma.io.RmaFile;                                                  // Abstraction for a file within the RMA file system utilities
import com.rma.model.Project;                                               // Accessor for the current project and project-level operations

import hec2.plugin.model.ModelAlternative;                                  // WAT model type representing a modeling alternative used during reporting
import hec2.wat.io.ProcessOutputReader;                                     // Threaded reader for process output streams, with listener support
import hec2.wat.model.WatSimulation;                                        // WAT model type representing a single simulation scenario or run

import net.sf.jasperreports.engine.JRException;                             // JasperReports exception type used for load, compile, fill, and export operations
import net.sf.jasperreports.engine.JRPropertiesUtil;                        // JasperReports utility for setting report engine properties
import net.sf.jasperreports.engine.JasperCompileManager;                    // JasperReports manager for compiling report designs (JRXML)
import net.sf.jasperreports.engine.JasperFillManager;                       // JasperReports manager for filling reports with parameters and data sources
import net.sf.jasperreports.engine.JasperPrint;                             // JasperReports printable representation of a compiled and filled report
import net.sf.jasperreports.engine.JasperReport;                            // JasperReports compiled report object
import net.sf.jasperreports.engine.SimpleJasperReportsContext;              // JasperReports context used to register repository and persistence services
import net.sf.jasperreports.engine.data.JRXmlDataSource;                    // JasperReports XML data source used to feed data into reports
import net.sf.jasperreports.engine.export.JRPdfExporter;                    // JasperReports exporter that writes a JasperPrint to PDF
import net.sf.jasperreports.engine.util.JRLoader;                           // JasperReports loader utility for compiled reports and resources
import net.sf.jasperreports.engine.util.JRXmlUtils;                         // JasperReports XML utilities including DOM parsing helpers
import net.sf.jasperreports.export.SimpleExporterInput;                     // JasperReports exporter input wrapper for JasperPrint instances
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;        // JasperReports exporter output that writes to a file stream
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;   // JasperReports factory for file-based repository persistence services
import net.sf.jasperreports.repo.FileRepositoryService;                     // JasperReports file-based repository service implementation
import net.sf.jasperreports.repo.PersistenceServiceFactory;                 // JasperReports factory interface for persistence services
import net.sf.jasperreports.repo.RepositoryService;                         // JasperReports repository service interface for locating report resources

import rma.util.RMAFilenameFilter;                                          // Filename filter used to select JRXML files for compilation
import rma.util.RMAIO;                                                      // RMA I/O utility helpers for path operations and safe concatenation
import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window for the WTMP plugin used as a UI parent

/**
 * Abstract base class for actions that create Jasper-based reports for WTMP simulations.
 *
 * Provides common workflow utilities:
 * - Validates selections and orchestrates report creation
 * - Compiles JRXML designs when needed
 * - Fills Jasper reports with XML data and parameters
 * - Exports reports to PDF
 * - Optionally invokes an external Python-based report generator
 *
 * Subclasses implement {@link #createReport(List)} to generate specific reports.
 */
public abstract class BaseReportAction extends AbstractAction {
	/** Folder name within the installation where automated report components reside. */
	public static final String REPORT_INSTALL_FOLDER = "AutomatedReport";

	/** File extension used by compiled Jasper report files. */
	public static final String JASPER_COMPILED_FILE_EXT = ".jasper";

	/** File extension for Jasper report source files (JRXML). */
	public static final String JASPER_SOURCE_FILE_EXT = "jrxml";

	/** Relative folder name under a simulation or study where reports are stored. */
	public static final String REPORT_DIR = "reports";

	/** File extension used for exported PDF reports. */
	public static final String PDF_REPORT_FILE_EXT = ".pdf";

	/** Owning actions window used to coordinate UI operations and context. */
	private ActionsWindow _parent;

	/**
	 * Constructs a base report action with a user-visible name and parent window.
	 *
	 * @param parent the actions window used as a UI parent and context source
	 * @param reportName the name of the report action displayed to the user
	 */
	public BaseReportAction(ActionsWindow parent, String reportName) {
		// Initialize the Swing action with the provided name
		super(reportName);

		// Store the parent actions window for later use
		_parent = parent;
	}

	/**
	 * Returns the actions window associated with this report action.
	 *
	 * @return the actions window
	 */
	public ActionsWindow getActionsWindow() {
		return _parent;
	}

	/**
	 * Validates selections and invokes report creation for the chosen simulations.
	 *
	 * Shows informative dialogs when no simulation group is active or no simulations
	 * are selected; otherwise delegates to {@link #createReport(List)}.
	 */
	public void createReportAction() {
		// Ensure a simulation group is active
		if ( _parent.getSimulationGroup() == null ) {
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return ;

		}

		// Retrieve selected simulations from the parent window
		List<WatSimulation> sims = _parent.getSelectedSimulations();

		// Prompt if none are selected
		if (sims.isEmpty()) {
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to create reports for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);

			return ;
		}

		// Delegate to subclass to create the report for the selected simulations
		createReport(sims);
	}

	/**
	 * Implemented by subclasses to create reports for a list of simulations.
	 *
	 * @param sims the simulations to include in the report
	 */
	protected abstract void createReport(List<WatSimulation> sims);

	/**
	 * Runs an external report generation executable (for example, Python-based) with the provided input file.
	 *
	 * Builds the command, sets the working directory to the installation's automated report folder,
	 * streams output, and returns success or failure.
	 *
	 * @param exe the executable file name to run (for example, WAT_Report_Generator.exe)
	 * @param reportFile the input file passed to the executable
	 * @param pythonReportBat deprecated parameter retained for compatibility (unused)
	 * @return true if the process completed with exit code 0, false otherwise
	 */
	protected boolean runPythonScript(String exe, String reportFile, String pythonReportBat) {
		// Capture start time for performance logging
		long t1 = System.currentTimeMillis();

		try {
			// Allow skipping the external generator via a system property
			if ( Boolean.getBoolean("SkipPythonReport")) {
				return true;
			}

			// Build the process command list
			List<String>cmdList = new ArrayList<>();

			// Determine the installation directory (prefer WAT.InstallDir; fall back to user.dir)
			String dir = System.getProperty("WAT.InstallDir", null);

			if ( dir == null ) {
				dir = System.getProperty("user.dir");
			}

			// Append the automated report subfolder
			dir = RMAIO.concatPath(dir, REPORT_INSTALL_FOLDER);

			// Full path to the executable in the installation folder
			String batFile = RMAIO.concatPath(dir, exe);

			// Populate the command arguments
			cmdList.add(batFile);

			cmdList.add(reportFile);

			// Launch the process and return success based on its exit value
			return runProcess(cmdList, dir);

		} finally {
			// Log the elapsed time for launching the report generator
			long t2 = System.currentTimeMillis();
			System.out.println("runPythonScript:time to run python for "+reportFile+" is "+(t2-t1)+"ms");
		}
	}

	/**
	 * Launches an external process in the specified directory and streams output.
	 *
	 * Creates a {@link ProcessBuilder}, sets the working directory, starts the process,
	 * streams both stdout and stderr through {@link ProcessOutputReader}, and reports success via the exit code.
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
			// Log launch information for diagnostics
			System.out.println("runProcess:launching in folder:"+runInFolder);
			System.out.println("runProcess:launching: "+cmdList);

			// Start the process
			Process proc = procBuilder.start();

			// Create a reader for stderr and stream with echo
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			ProcessOutputReader preader1 = new ProcessOutputReader(reader1, true, proc);

			preader1.setEchoOutput(true);
			preader1.start();

			// Create a reader for stdout and stream with echo
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			ProcessOutputReader preader2 = new ProcessOutputReader(reader2, false, proc);

			preader2.setEchoOutput(true);
			preader2.start();

			// Wait for the process to complete and return success status
			int rv = proc.waitFor();

			System.out.println("runProcess:rv="+rv);

			return rv == 0;

		} catch (IOException | InterruptedException e) {
			// Log and propagate failure without throwing
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		}
	}

	/**
	 * Compiles, fills, and exports a Jasper report for a single simulation.
	 *
	 * Sets up repository services, compiles the JRXML, fills the report with parameters and XML data,
	 * and writes the report to a PDF file under the simulation's reports folder.
	 *
	 * @param sim the simulation to report on
	 * @param jasperFile the JRXML design file relative to the study reports folder
	 * @param jasperOutFile the base output file name (without extension) for the PDF
	 * @param xmlDataDoc the XML data document used as the Jasper data source
	 * @param params parameters passed to Jasper during filling
	 * @return true when the report is successfully exported, false otherwise
	 */
	public boolean runJasperReport(WatSimulation sim, String jasperFile, String jasperOutFile, String xmlDataDoc, Map<String,Object>params) {
		// Capture start time for performance logging
		long t1 = System.currentTimeMillis();

		try {
			// Resolve study and simulation directories
			// Log log = LogFactory.getLog(JasperFillManager.class);
			String studyDir = Project.getCurrentProject().getProjectDirectory();
			String simDir = sim.getSimulationDirectory();

			// Compute the Jasper repository directory within the study
			String jasperRepoDir = RMAIO.concatPath(studyDir, REPORT_DIR);
			String rptFile = RMAIO.concatPath(jasperRepoDir, jasperFile);

			System.out.println("runReportWithOutputFile:report repository:"+jasperRepoDir);

			// Initialize Jasper context and file repository services
			SimpleJasperReportsContext context = new SimpleJasperReportsContext();
			FileRepositoryService fileRepository = new FileRepositoryService(context, jasperRepoDir, true);

			context.setExtensions(RepositoryService.class, Collections.singletonList(fileRepository));
			context.setExtensions(PersistenceServiceFactory.class, Collections.singletonList(FileRepositoryPersistenceServiceFactory.getInstance()));

			// Full path to the JRXML design file
			String inJasperFile = rptFile;

			// Set XPath executer property for JRXmlDataSource processing
			JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.xpath.executer.factory",
					"net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");

			// Compile the JRXML design and return a JasperReport
			long t2 = System.currentTimeMillis();

			JasperReport jasperReport;

			try {
				compileJasperFiles(RMAIO.getDirectoryFromPath(inJasperFile));
				jasperReport = JasperCompileManager.compileReport(inJasperFile);

			} catch (JRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				return false;
			}

			long t3 = System.currentTimeMillis();

			System.out.println("runJasperReport:time to compile jasper files for "+sim+ "is "+(t3-t2)+"ms");

			// Ensure the simulation reports folder exists
			String outputFile = RMAIO.concatPath(simDir, REPORT_DIR);
			RmaFile simDirFile = FileManagerImpl.getFileManager().getFile(outputFile);

			if ( !simDirFile.exists() ) {
				if ( !simDirFile.mkdirs()) {
					System.out.println("runJasperReport:failed to create folder "+simDirFile.getAbsolutePath());
				}
			}

			// Construct the final output file path for the PDF
			outputFile = RMAIO.concatPath(outputFile, jasperOutFile);

			// Append the PDF extension
			outputFile = outputFile.concat(PDF_REPORT_FILE_EXT);

			// Fill the compiled report using the XML data source
			JasperPrint jasperPrint;

			System.out.println("runJasperReport:filling report "+inJasperFile);

			JRXmlDataSource dataSource;

			try {
				// Load and parse the XML data adapter via the Jasper context
				dataSource = new JRXmlDataSource(context, JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataDoc)));

			} catch (JRException e1) {
				// Data source parsing failed
				e1.printStackTrace();

				return false;
			}

			try {
				// Fill the report with parameters and XML data
				jasperPrint = JasperFillManager.getInstance(context).fill(jasperReport, params, dataSource);

			} catch (JRException e) {
				e.printStackTrace();

				return false;
			}

			long t4 = System.currentTimeMillis();
			System.out.println("runJasperReport:time to fill jasper report for "+sim+ "is "+(t4-t3)+"ms");

			// Export the filled report to PDF
			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputFile));

			try {
				exporter.exportReport();
				System.out.println("runJasperReport:report written to "+outputFile);

			} catch (JRException e) {
				e.printStackTrace();

				return false;
			}

			// Log final timing for the export step
			long t5 = System.currentTimeMillis();
			System.out.println("runJasperReport:time to write jasper report for "+sim+ "is "+(t5-t4)+"ms");

			return true;

		} finally {
			// Log overall timing for the full report workflow
			long end = System.currentTimeMillis();
			System.out.println("runJasperReport:total time to create jasper report for "+sim+" is "+(end-t1)+"ms");
		}
	}

	/**
	 * Compiles Jasper report source files in the given directory when needed.
	 *
	 * Uses the system property "CompileJasperFiles" to force compilation even when timestamps
	 * indicate it is not necessary.
	 *
	 * @param jasperDir absolute path to the directory containing JRXML files
	 */
	private void compileJasperFiles(String jasperDir) {
		// Filter used to select JRXML files
		RMAFilenameFilter filter= new RMAFilenameFilter(JASPER_SOURCE_FILE_EXT);
		filter.setAcceptDirectories(false);

		// List JRXML files found in the directory
		List<String> jasperFiles = FileManagerImpl.getFileManager().list(jasperDir, filter);

		// Temp variables for source and destination paths
		String srcFile, destFile;

		// System property to always force recompilation
		boolean alwaysCompile = Boolean.getBoolean("CompileJasperFiles");

		for (int i = 0; i < jasperFiles.size(); i++ ) {
			srcFile = jasperFiles.get(i);
			destFile = getJasperDestFile(srcFile);

			// Compile when destination is stale or recompilation is forced
			if ( needsToCompile(srcFile, destFile) || alwaysCompile ) {
				System.out.println("compileJasperFiles:compiling to disk "+srcFile);

				try {
					// Compile JRXML to a .jasper file in the same directory
					String rv = JasperCompileManager.compileReportToFile(jasperFiles.get(i));
					System.out.println("compileJasperFiles: compiled to "+rv);

				} catch (JRException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
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
	 * The compiled file is written to the same folder as the source with a .jasper extension.
	 *
	 * @param srcFile absolute path to the JRXML source file
	 * @return absolute path to the compiled .jasper file, or null when unsupported
	 */
	private String getJasperDestFile(String srcFile) {
		// Find the last dot to strip extension
		int idx = srcFile.lastIndexOf('.');

		if ( idx > -1 ) {
			String destFile = srcFile.substring(0,idx);
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
	private String findFpartForPython(WatSimulation sim, ModelAlternative modelAlt) {
		// Retrieve F-part from the simulation
		String fpart = sim.getFPart(modelAlt);

		// Sanitize F-part for use as a filename
		return RMAIO.userNameToFileName(fpart);
	}
}