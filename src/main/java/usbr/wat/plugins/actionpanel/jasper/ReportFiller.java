package usbr.wat.plugins.actionpanel.jasper;

import java.io.File;                    // Represents a file path on the local filesystem
import java.text.SimpleDateFormat;      // Formats Date objects into human-readable strings
import java.util.Arrays;                // Utility methods for working with arrays (e.g. asList)
import java.util.Collections;           // Utility methods for collections (e.g. singletonList)
import java.util.Date;                  // Represents a specific instant in time
import java.util.HashMap;               // Key-value map used to store JasperReports parameters
import java.util.Map;                   // Map interface for parameter containers
import java.util.logging.Logger;        // Java standard logging framework

import org.jdom.Document;               // JDOM representation of an XML document tree
import org.jdom.Element;                // Represents a single XML element within a JDOM document

import com.rma.io.FileManagerImpl;      // RMA implementation for obtaining managed file references
import com.rma.io.RmaFile;              // RMA abstraction for a file resource
import com.rma.util.XMLUtilities;       // RMA utility methods for reading values from XML elements

import net.sf.jasperreports.engine.JRException;                                         // Checked exception thrown by JasperReports operations
import net.sf.jasperreports.engine.JRExporter;                                          // Interface for exporting a filled JasperPrint to an output format
import net.sf.jasperreports.engine.JRPropertiesUtil;                                    // Utility for setting runtime properties on a JasperReports context
import net.sf.jasperreports.engine.JasperFillManager;                                   // Fills a compiled JasperReport with data to produce a JasperPrint
import net.sf.jasperreports.engine.JasperPrint;                                         // In-memory representation of a filled, ready-to-export report
import net.sf.jasperreports.engine.JasperReport;                                        // Compiled JasperReports report definition (.jasper file)
import net.sf.jasperreports.engine.SimpleJasperReportsContext;                          // Configurable runtime context for JasperReports operations
import net.sf.jasperreports.engine.data.JRXmlDataSource;                                // XML-backed data source for supplying rows to JasperReports
import net.sf.jasperreports.engine.util.JRLoader;                                       // Utility for loading compiled reports and input streams from paths
import net.sf.jasperreports.engine.util.JRXmlUtils;                                     // Utility for parsing XML documents consumed by JasperReports
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;               // Factory that creates persistence services backed by a filesystem directory
import net.sf.jasperreports.repo.FileRepositoryService;                                 // JasperReports repository service backed by a filesystem directory
import net.sf.jasperreports.repo.PersistenceServiceFactory;                             // Extension point for registering persistence service factories in a context
import net.sf.jasperreports.repo.RepositoryService;                                     // Extension point for registering repository services in a context

import rma.util.GetOpt;                 // Command-line option parser (Unix getopt style)
import rma.util.RMAIO;                  // RMA file I/O utilities including path manipulation

import usbr.wat.plugins.actionpanel.io.OutputType;  // Enum that maps file extensions to JRExporter instances


/**
 * Fills and exports a JasperReports report as a standalone, out-of-process operation.
 *
 * This class is designed to be launched as an independent JVM process. It reads all
 * necessary configuration from an XML arguments file, loads a pre-compiled Jasper
 * report (.jasper) from the study's Jasper directory, populates it with data from
 * an XML data source, and writes the finished report to a file whose format is
 * determined by the output file extension.
 *
 * Typical entry point: {@link #main(String[])} accepting a {@code -f <argsFile>}
 * argument. The XML arguments file is expected to contain the elements defined
 * by the public {@code *_ELEMENT} constants of this class.
 *
 */
public class ReportFiller {
	// Static logger scoped to this class for recording progress and errors
	private static Logger _logger = Logger.getLogger(ReportFiller.class.getName());

	// --- Directory and file name constants ---

	/** Subdirectory under a study or simulation folder that holds report artifacts. */
	public static final String REPORT_DIR = "reports";

	/** Subdirectory under the report directory that holds compiled Jasper files. */
	public static final String JASPER_DIR = "jasper";

	/** Subdirectory under the simulation report directory that holds XML data sources. */
	public static final String DATA_SOURCES_DIR = "Datasources";

	/** Default JasperReports template source file name (.jrxml). */
	public static final String JASPER_FILE = "USBR_Draft_Validation.jrxml";


	// --- JasperReports parameter name constants (keys passed in the params Map) ---

	/** Parameter key for the watershed/study name shown in report headers. */
	public static final String WATERSHED_NAME_PARAM = "watershedName";

	/** Parameter key for the simulation name shown in report headers. */
	public static final String SIMULATION_NAME_PARAM = "simulationName";

	/** Parameter key for the analysis period start time string. */
	public static final String ANALYSIS_START_TIME_PARAM = "analysisStartTime";

	/** Parameter key for the analysis period end time string. */
	public static final String ANALYSIS_END_TIME_PARAM = "analysisEndTime";

	/** Parameter key for the formatted date the simulation was last computed. */
	public static final String SIMULATION_LAST_COMPUTED_DATE_PARAM = "simulationDate";

	/** Parameter key controlling whether page headers and footers are printed. */
	public static final String PRINT_HEADER_FOOTER_PARAM = "printHeaderAndFooter";

	/** Internal parameter key for the study-level report directory path. */
	private static final String REPORT_DIR_PARAM = "REPORT_DIR";

	/** XML data adapter file that defines the Jasper data source configuration. */
	public static final String XML_DATA_DOCUMENT = "USBRAutomatedReportDataAdapter.xml";

	/** Output XML file name produced by the report filling process. */
	public static final String XML_DATA_OUTPUT = "USBRAutomatedReportOutput.xml";

	/** Internal parameter key for the WAT installation directory. */
	private static final String WAT_INSTALL_DIR_PARAM = "Install_Dir";

	/** Internal parameter key pointing to the XML data adapter file on disk. */
	private static final String DATA_ADAPTER_FILE_PARAM = "DataAdapterLocation";

	/** Internal parameter key for the simulation-level report directory. */
	private static final String SIM_REPORT_DIR_PARAM = "RUN_DIR";


	// --- XML element name constants (tags expected in the args XML file) ---

	/** XML element whose text value is the path of the output report file. */
	public static final String OUTPUT_FILE_ELEM = "OutputFile";

	/** XML element whose text value is the path to the Jasper report folder. */
	public static final String JASPER_REPORT_FOLDER_ELEMENT = "JasperReportFolder";

	/** XML element whose text value is the path to the simulation folder. */
	public static final String SIMULATION_FOLDER_ELEMENT = "SimulationFolder";

	/** XML element whose text value is the WAT installation directory. */
	public static final String INSTALL_FOLDER_ELEMENT = "InstallFolder";

	/** XML element whose text value is the study root directory. */
	public static final String STUDY_FOLDER_ELEMENT = "StudyFolder";

	/** XML element whose text value is the name of the simulation. */
	public static final String SIM_NAME_ELEMENT = "SimulationName";

	/** XML element whose text value is the analysis period start time. */
	public static final String START_TIME_ELEMENT = "StartTime";

	/** XML element whose text value is the analysis period end time. */
	public static final String END_TIME_ELEMENT = "EndTime";

	/** XML element whose boolean text value controls header/footer printing. */
	public static final String PRINT_HEADER_FOOTER_ELEMENT = "PrintHeadersFooters";

	/** XML element whose long text value is the simulation's last compute timestamp (epoch ms). */
	public static final String LAST_COMPUTE_DATE_ELEMENT = "SimulationLastComputeDate";

	/** XML element whose text value is the study (watershed) name. */
	public static final String STUDY_NAME_ELEMENT = "StudyName";


	/**
	 * Application entry point for running the report filler as a standalone process.
	 *
	 * Parses command-line arguments to obtain the path of an XML arguments file, loads
	 * that file, and delegates to {@link #runReport(Element)} to perform report filling
	 * and export. Exits with a non-zero status code on any failure condition.
	 *
	 * Expected usage: {@code ReportFiller -f <path-to-args-xml>}
	 *
	 * Exit codes:
	 *
	 *   0 – report generated successfully
	 *   1 – unrecognised command-line option>
	 *   2 – report fill step returned null (see {@link #fillReport(Element)})
	 *   3 – XML arguments file could not be loaded
	 *
	 * @param args command-line arguments; must include {@code -f <argsFilePath>}
	 */
	public static void main(String[] args) {
		// Holds the path to the XML arguments file supplied via -f
		String argPath = null;

		// Initialise the getopt parser expecting a single option "-f" with a required argument
		GetOpt go = new GetOpt(args, "f:");

		int opt;
		char ch;

		// Tracks whether the user explicitly asked for usage output
		boolean usagePrint = false;

		// Iterate through all provided command-line flags
		while ((opt = go.getopt()) != GetOpt.optEOF) {
			ch = (char)opt;

			// Handle both explicit usage flag and unknown-option sentinel
			if ( ch == 'U' || ch == '?') {
				usagePrint = true;

			} else if ( ch == 'f') {
				// Capture the required file path argument for -f
				argPath = go.optArgGet();

			} else {
				// Any other character is an unrecognised option — log and abort
				_logger.info("Unknown option " + ch);
				System.exit(1);
			}
		}

		// Abort and print usage if requested, or if no file path was provided
		if (usagePrint || argPath == null) {
			System.out.println("Usage: -f argsFile");
			System.exit(0);
		}

		// Obtain an RMA-managed file reference for the XML arguments file
		RmaFile argsFile = FileManagerImpl.getFileManager().getFile(argPath);

		// Parse the XML arguments file into a JDOM Document
		Document doc = XMLUtilities.loadDocument(argsFile);

		// Abort if the XML document could not be loaded from the given path
		if (doc == null) {
			System.out.println("ReportFiller.main: no XML document found at " + argPath);
			System.exit(3);
		}

		// Delegate report execution to the instance method using the document root element
		boolean rv = new ReportFiller().runReport(doc.getRootElement());

		// Exit normally only if the report was produced successfully
		if (rv) {
			System.exit(0);
		}
	}


	/**
	 * Default no-argument constructor.
	 */
	public ReportFiller() {
		super();
	}


	/**
	 * Orchestrates the full report lifecycle: fill then write.
	 *
	 * Calls {@link #fillReport(Element)} to produce an in-memory {@link JasperPrint},
	 * then passes it to {@link #writeReport(JasperPrint, Element)} to export the final file.
	 * Exits with code 2 if the fill step fails.
	 *
	 * @param root the root XML element of the arguments file containing all report parameters
	 * @return {@code true} if the report was written successfully; {@code false} otherwise
	 */
	protected boolean runReport(Element root) {
		// Attempt to fill the report template with data; returns null on failure
		JasperPrint jasperPrint = fillReport(root);

		if (jasperPrint == null) {
			System.out.println("runReport:failed to fill report");
			System.exit(2);
		}

		// Write the populated in-memory report to a file and return the success flag
		boolean rv = writeReport(jasperPrint, root);
		return rv;
	}


	/**
	 * Exports a filled {@link JasperPrint} to a file on disk.
	 *
	 * The output file path and format are read from the {@value #OUTPUT_FILE_ELEM}
	 * element in {@code root}. The file extension is used to determine the appropriate
	 * {@link JRExporter} via {@link OutputType#getOutputType(String)}.
	 *
	 * @param jasperPrint the filled report ready for export
	 * @param root        the root XML element providing the output file path
	 * @return {@code true} if the report was exported without error; {@code false} if an
	 *         exception was thrown during export
	 */
	private boolean writeReport(JasperPrint jasperPrint, Element root) {
		// Record the wall-clock time at the start of the write operation for later logging
		long t1 = System.currentTimeMillis();

		try {
			// Read the desired output file path from the arguments XML
			String outputFile = XMLUtilities.getChildElementAsString(root, OUTPUT_FILE_ELEM, null);

			// Extract the file extension to determine the export format (e.g. "pdf", "xlsx")
			String outputExt = RMAIO.getFileExtension(outputFile);

			// Resolve the OutputType enum constant and build a JRExporter for that format
			OutputType ot = OutputType.getOutputType(outputExt);
			JRExporter exporter = ot.buildExporter(jasperPrint, outputFile);

			// Capture another timestamp (currently unused; t4 variable retained for future use)
			long t4 = System.currentTimeMillis();

			try {
				// Perform the actual export to the output file
				exporter.exportReport();
				_logger.info("writeReport:simulation report written to " + outputFile);
				return true;

			} catch (JRException e) {
				// Log the stack trace and signal failure via return value
				e.printStackTrace();
			}

			return false;

		} finally {
			// Always log the total elapsed time regardless of success or failure
			long t2 = System.currentTimeMillis();
			_logger.info("writeReport: time to write report " + (t2 - 21) + " ms");
		}
	}


	/**
	 * Loads, configures, and fills a JasperReports report with data from an XML data source.
	 *
	 * This method:
	 *
	 *   Reads directory paths from the XML arguments element.
	 *   Builds a {@link SimpleJasperReportsContext} with two filesystem repository
	 *       services: one for the compiled Jasper template, one for the XML data source.
	 *   Sets the XPath executor to Jaxen for compatibility with the report's XPath expressions.
	 *   Loads the pre-compiled {@code .jasper} file derived from {@link #JASPER_FILE}.
	 *   Builds the parameter map via {@link #setParameters(Map, String, Element)}.
	 *   Parses the XML data source file and fills the report.
	 *
	 * @param root the root XML element of the arguments file
	 * @return the filled {@link JasperPrint} ready for export, or {@code null} if any step fails
	 */
	protected JasperPrint fillReport(Element root) {
		// Record the start time so fill duration can be logged in the finally block
		long t1 = System.currentTimeMillis();

		try {
			// Read the four key directory paths from the arguments XML
			String jasperReportFolder = XMLUtilities.getChildElementAsString(root, JASPER_REPORT_FOLDER_ELEMENT, null);
			String simFolder          = XMLUtilities.getChildElementAsString(root, SIMULATION_FOLDER_ELEMENT, null);
			String installDir         = XMLUtilities.getChildElementAsString(root, INSTALL_FOLDER_ELEMENT, null);
			String studyDir           = XMLUtilities.getChildElementAsString(root, STUDY_FOLDER_ELEMENT, null);

			// Create a fresh, configurable JasperReports runtime context
			SimpleJasperReportsContext context = new SimpleJasperReportsContext();

			// Override the XPath executor factory to use Jaxen, which supports the report's XPath syntax
			JRPropertiesUtil.getInstance(context).setProperty(
					"net.sf.jasperreports.xpath.executer.factory",
					"net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");

			// Build the path to the compiled Jasper files within the study directory
			// Note: "C" is appended to indicate a compiled sub-folder convention
			String studyJasperDir = RMAIO.concatPath(studyDir, getJasperRelativeFolder());
			studyJasperDir = studyJasperDir + "C";

			// Register a file repository service that JasperReports will use to resolve subreports
			// and resources relative to the compiled Jasper directory
			FileRepositoryService jasperFileRepository = new FileRepositoryService(context,
					studyJasperDir, true);

			// Build the path to the simulation's data source directory
			String simReportsDir = RMAIO.concatPath(simFolder, REPORT_DIR);
			simReportsDir = RMAIO.concatPath(simReportsDir, DATA_SOURCES_DIR);

			// Register a second file repository service pointing at the simulation data directory
			FileRepositoryService reportsFileRepository = new FileRepositoryService(context,
					simReportsDir, true);

			// Register both repository services with the context so JasperReports can find all resources
			context.setExtensions(RepositoryService.class,
					Arrays.asList(jasperFileRepository, reportsFileRepository));

			// Register the filesystem-backed persistence factory for loading binary report objects
			context.setExtensions(PersistenceServiceFactory.class,
					Collections.singletonList(FileRepositoryPersistenceServiceFactory.getInstance()));


			JasperReport jasperReport;
			String inJasperFile = null;

			try {
				// Derive the compiled .jasper file name by replacing the .jrxml extension
				int idx = JASPER_FILE.lastIndexOf('.');
				String jasperCompiledFile = JASPER_FILE.substring(0, idx);
				jasperCompiledFile = jasperCompiledFile.concat(".jasper");

				// Build the absolute path to the compiled report file
				inJasperFile = RMAIO.concatPath(studyJasperDir, jasperCompiledFile);

				// Load the compiled JasperReport object from the .jasper file
				jasperReport = (JasperReport)JRLoader.loadObject(new File(inJasperFile));

			} catch (JRException e) {
				// Report file is missing or corrupt; log and signal failure
				e.printStackTrace();
				return null;
			}

			// Build the parameter map that will be passed to the fill engine
			Map<String, Object> params = new HashMap<>();
			setParameters(params, jasperReportFolder, root);


			// Construct the full path to the XML data source document
			String xmlDataDoc = RMAIO.concatPath(simFolder, REPORT_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, DATA_SOURCES_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, XML_DATA_DOCUMENT);

			_logger.info("fillReport:filling report " + inJasperFile + " DataSource=" + xmlDataDoc);

			// Parse the XML data source file into a DOM document and wrap it in a JRXmlDataSource
			JRXmlDataSource dataSource;
			try {
				dataSource = new JRXmlDataSource(context,
						JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataDoc)));

			} catch (JRException e1) {
				// XML data source is missing or malformed
				e1.printStackTrace();
				return null;
			}

			// Guard against a null data source even if no exception was thrown
			if (dataSource == null) {
				_logger.info("fillReport:failed to load DataAdapter file " + xmlDataDoc);
				return null;
			}

			// Fill the compiled report with the parameter map and XML data source to produce a JasperPrint
			try {
				JasperPrint jasperPrint = JasperFillManager.getInstance(context)
						.fill(jasperReport, params, dataSource);
				return jasperPrint;

			} catch (JRException e) {
				// Report fill failed (e.g. XPath errors, missing parameters)
				e.printStackTrace();
				return null;
			}

		} finally {
			// Log fill duration regardless of the outcome
			long t2 = System.currentTimeMillis();
			_logger.info("fillReport:time to fill jasper report is " + (t2 - t1) + "ms");
		}
	}


	/**
	 * Populates the JasperReports parameter map from the XML arguments element.
	 *
	 * Each entry read from {@code root} is placed into {@code params} under the
	 * corresponding parameter key constant defined on this class. The last-compute
	 * timestamp is converted from epoch milliseconds to a formatted date string using
	 * the pattern {@code "MMMM dd, yyyy HH:mm"}.
	 *
	 * @param params         the mutable map to populate; entries are added, not replaced
	 * @param jasperRepoDir  the path to the Jasper report folder, stored under {@code p_ReportFolder}
	 * @param root           the root XML element of the arguments file
	 */
	protected void setParameters(Map<String, Object> params, String jasperRepoDir, Element root) {
		// Store the Jasper report folder path under the fixed key used by the report template
		params.put("p_ReportFolder", jasperRepoDir);

		// Read the study name and map it to the watershed name parameter
		String studyName = XMLUtilities.getChildElementAsString(root, STUDY_NAME_ELEMENT, "");
		params.put(WATERSHED_NAME_PARAM, studyName);

		// Read the simulation name for report header display
		String simName = XMLUtilities.getChildElementAsString(root, SIM_NAME_ELEMENT, "");
		params.put(SIMULATION_NAME_PARAM, simName);

		// Read the analysis period start and end times
		String startTime = XMLUtilities.getChildElementAsString(root, START_TIME_ELEMENT, "");
		params.put(ANALYSIS_START_TIME_PARAM, startTime);

		String endTime = XMLUtilities.getChildElementAsString(root, END_TIME_ELEMENT, "");
		params.put(ANALYSIS_END_TIME_PARAM, endTime);

		// Read the boolean flag controlling whether page headers and footers are rendered
		boolean printHeadersFooters = XMLUtilities.getChildElementAsBoolean(root, PRINT_HEADER_FOOTER_ELEMENT, true);
		params.put(PRINT_HEADER_FOOTER_PARAM, printHeadersFooters);

		// Build the study-level report directory path and register it with the context
		String studyDir = XMLUtilities.getChildElementAsString(root, STUDY_FOLDER_ELEMENT, null);
		String reportDir = RMAIO.concatPath(studyDir, REPORT_DIR);
		params.put(REPORT_DIR_PARAM, reportDir);

		// Build the simulation-level report directory path
		String simFolder = XMLUtilities.getChildElementAsString(root, SIMULATION_FOLDER_ELEMENT, null);
		String simReportDir = RMAIO.concatPath(simFolder, REPORT_DIR);
		params.put(SIM_REPORT_DIR_PARAM, simReportDir);

		// Read the WAT installation directory; fall back to the JVM's working directory if absent
		String installDir = XMLUtilities.getChildElementAsString(root, INSTALL_FOLDER_ELEMENT,
				System.getProperty("user.dir"));
		// Commented-out alternative: extract just the directory portion from the path
		// installDir = RMAIO.getDirectoryFromPath(installDir);
		params.put(WAT_INSTALL_DIR_PARAM, installDir);

		// Read the last-compute timestamp (stored as epoch milliseconds) and format it for display
		long lastComputeDate = XMLUtilities.getChildElementAsLong(root, LAST_COMPUTE_DATE_ELEMENT, 0);
		Date date = new Date(lastComputeDate);
		SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
		params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));

		// Build the full path to the XML data adapter file for the report template to reference
		String dataAdapterFile = RMAIO.concatPath(studyDir, REPORT_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, DATA_SOURCES_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, XML_DATA_DOCUMENT);
		params.put(DATA_ADAPTER_FILE_PARAM, dataAdapterFile);

		// Log the complete parameter map at FINE level for diagnostic purposes
		_logger.fine("Report Parameters are:" + params);
	}


	/**
	 * Returns the relative path from the study root to the Jasper report directory.
	 *
	 * The path is constructed by joining {@link #REPORT_DIR} and {@link #JASPER_DIR},
	 * producing {@code "reports/jasper"} (or equivalent separator for the platform).
	 *
	 * @return the relative folder path to the Jasper report directory
	 */
	protected String getJasperRelativeFolder() {
		// Combine the report and jasper sub-directory names into a single relative path
		String jasperReportFolder = RMAIO.concatPath(REPORT_DIR, JASPER_DIR);
		return jasperReportFolder;
	}

}
