package usbr.wat.plugins.actionpanel.ui;

import java.awt.Dimension;          // Encapsulates width and height for sizing UI components
import java.awt.GridBagConstraints; // Specifies layout constraints for GridBagLayout cells
import java.awt.GridBagLayout;      // Flexible grid-based Swing layout manager
import java.awt.Window;             // AWT base class for top-level windows, used as the parent reference
import java.awt.event.ActionEvent;  // Event object fired when a button or menu item is activated

import java.io.BufferedReader;      // Wraps a Reader with a buffer for efficient character reading
import java.io.IOException;         // Signals an I/O failure (stream read, process launch, etc.)
import java.io.InputStream;         // Raw byte stream, used to read classpath resources and process output
import java.io.InputStreamReader;   // Bridge from byte stream to character stream for text processing
import java.io.Reader;              // Abstract character-stream reader, accepted by SAXBuilder

import java.util.ArrayList;         // Resizable-array List implementation used for command and row data
import java.util.List;              // Generic ordered collection interface
import java.util.Vector;            // Thread-safe legacy list used by ProcessOutputReader and table rows
import java.util.logging.Level;     // Severity constants (INFO, WARNING, FINE, etc.) for log records
import java.util.logging.Logger;    // Java standard logging framework

import javax.swing.JLabel;          // Swing label component for displaying static text or images
import javax.swing.JPanel;          // General-purpose Swing container for grouping sub-components
import javax.swing.SwingWorker;     // Executes long-running tasks on a background thread, with EDT callbacks

import org.jdom.Document;           // JDOM in-memory representation of a parsed XML document
import org.jdom.Element;            // Represents a single XML element within a JDOM tree
import org.jdom.JDOMException;      // Checked exception thrown when JDOM XML parsing fails
import org.jdom.Namespace;          // Represents an XML namespace used to qualify element lookups
import org.jdom.input.SAXBuilder;   // SAX-based JDOM parser that builds a Document from a Reader or stream

import com.rma.swing.UrlLabel;      // RMA Swing label that renders as a clickable hyperlink

import hec.io.ProcessOutputLine;    // Wraps a single line of output captured from an external process
import hec.io.ProcessOutputReader;  // Reads stdout/stderr lines from a Process into a shared output Vector

import rma.swing.ButtonCmdPanel;         // RMA panel containing standard command buttons (e.g. Close)
import rma.swing.ButtonCmdPanelListener; // Listener interface for ButtonCmdPanel button events
import rma.swing.RmaImage;               // RMA utility for loading image icons from the classpath
import rma.swing.RmaInsets;              // RMA constants for common GridBagLayout inset configurations
import rma.swing.RmaJDialog;             // RMA base dialog class providing common dialog behaviour
import rma.swing.RmaJTable;              // RMA-enhanced JTable with scrollpane and row-append support
import rma.util.RMAIO;                   // RMA file I/O utilities including path concatenation

import usbr.wat.plugins.actionpanel.actions.AbstractReportAction;          // Provides report executable path and file name constants
import usbr.wat.plugins.actionpanel.gitIntegration.actions.VersionAction;  // Queries the Git/Python executable version string
import usbr.wat.plugins.actionpanel.model.ReportPlugin;                    // Model object describing a registered report plugin and its Maven path
import usbr.wat.plugins.actionpanel.model.ReportsManager;                  // Registry that provides the list of installed report plugins


/**
 * Modal "About WTMP" dialog that displays version information for the plugin
 * and its associated executable components.
 *
 * The dialog is laid out in two columns using {@link GridBagLayout}:
 *
 *   Left panel — USBR logo image.
 *   Right panel — product name, build version, office attribution, contact hyperlink, and a version table.
 *
 *
 * The version table is populated in two stages:
 *
 *   {@link #fillPluginTable()} runs synchronously on the EDT and adds one row
 *       per registered {@link ReportPlugin}, reading version strings from the
 *       embedded {@code pom.xml} files on the classpath.
 *   {@link #fillExeTable()} runs on a background {@link SwingWorker} thread,
 *       querying external executables (Git/Python, the report batch file) for their
 *       version strings and publishing rows back to the EDT as results arrive.
 *
 */
@SuppressWarnings("serial")
public class AboutDialog extends RmaJDialog {
	/** Logger scoped to this class for diagnostics during version resolution. */
	private Logger LOGGER = Logger.getLogger(AboutDialog.class.getName());

	/** Classpath root directory that Maven embeds {@code pom.xml} files under at build time. */
	private static final String META_MAVEN_PATH = "META-INF/maven";

	/** Name of the Maven project descriptor file embedded in the JAR. */
	private static final String POM_XML = "pom.xml";

	/** XML element name for the {@code <parent>} block in a Maven {@code pom.xml}. */
	private static final String PARENT_ELEMENT = "parent";

	/** XML element name for the {@code <version>} field in a Maven {@code pom.xml}. */
	private static final String VERSION_ELEMENT = "version";

	/** Maven artifact path used to locate this plugin's embedded {@code pom.xml}. */
	private static final String MAVEN_PATH = "usbr.wat.plugins/usbr-actionpanel-plugin";

	/** Panel containing the standard Close button at the bottom of the dialog. */
	private ButtonCmdPanel _cmdPanel;

	/** Table displaying component names and their resolved version strings. */
	private RmaJTable _versionTable;

	/** Clickable hyperlink label pointing to the USBR website. */
	private UrlLabel _contactLabel;


	/**
	 * Constructs and displays the About dialog.
	 *
	 * Builds all UI controls, attaches button listeners, synchronously populates the
	 * plugin version rows, kicks off the background executable version queries, then
	 * sizes and centres the dialog relative to its parent window.
	 *
	 * @param parent the owning window; used to position the dialog and establish modality
	 */
	public AboutDialog(Window parent) {
		// Initialise the RMA base dialog with modal = true
		super(parent, true);

		// Build and add all Swing components to the content pane
		buildControls();

		// Wire up button event listeners
		addListeners();

		// Populate the table with plugin version rows (synchronous, classpath-based)
		fillPluginTable();

		// Kick off background threads to query external executables for their versions
		fillExeTable();

		// Size the dialog to its preferred layout dimensions
		pack();

		// Prevent the dialog from being resized below its packed dimensions
		setMinimumSize(getSize());

		// Centre the dialog over the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Constructs and arranges all UI components within the dialog content pane.
	 *
	 * The layout uses two top-level {@link GridBagLayout} columns:
	 *
	 *   Left panel — USBR logo.
	 *   Right panel — title, version, office info, contact link, version table.
	 *
	 * The Close button panel is placed in a full-width row at the bottom of the
	 * content pane.
	 */
	protected void buildControls() {
		setTitle("About WTMP");
		getContentPane().setLayout(new GridBagLayout());

		// --- Left panel: USBR logo ---

		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// Configure constraints for the left panel itself within the content pane
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(leftPanel, gbc);

		// Add the USBR logo image into the left panel
		JLabel label = new JLabel(RmaImage.getImageIcon("Images/usbr.gif"));
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0001;  // Small positive weight keeps the logo anchored near the top
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		leftPanel.add(label, gbc);

		// --- Right panel: text info, version table ---

		JPanel rightPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(rightPanel, gbc);

		// Product name heading
		label = new JLabel("<html><h1>WTMP</h1></html>");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);

		// Build version sub-heading, resolved from the embedded pom.xml at startup
		String version = getBuildVersion(MAVEN_PATH);
		label = new JLabel("<html><h2>Version:" + version + "</h2></html>");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		rightPanel.add(label, gbc);

		// Office attribution line
		label = new JLabel("<html><h2>Central Valley Operations Office</h2></html>");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);

		// Interior region sub-line
		label = new JLabel("Interior Region 10 · California-Great Basin");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS0505;
		rightPanel.add(label, gbc);

		// Agency name line
		label = new JLabel("U.S. Bureau of Reclamation");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);

		// Blank spacer rows to provide visual separation above the contact link
		label = new JLabel("");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);

		label = new JLabel("");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);

		// Clickable hyperlink to the USBR website
		_contactLabel = new UrlLabel("www.usbr.gov");
		_contactLabel.setUrl("http://www.usbr.gov");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(_contactLabel, gbc);

		// Version table with two columns: "Component" and "Version"
		// An anonymous subclass limits the viewport height and makes cells read-only
		String[] headers = new String[]{"Component", "Version"};
		_versionTable = new RmaJTable(this, headers) {
			/**
			 * Constrains the table's preferred scroll viewport height to show exactly
			 * four rows, preventing the dialog from growing too tall.
			 *
			 * @return a {@link Dimension} whose height equals four row heights
			 */
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = super.getPreferredScrollableViewportSize();
				// Limit visible height to four rows so the dialog stays compact
				d.height = getRowHeight() * 4;
				return d;
			}

			/**
			 * Prevents any cell in the version table from being edited by the user.
			 *
			 * @param row the row index of the cell
			 * @param col the column index of the cell
			 * @return always {@code false}
			 */
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};

		// Add the table's scroll pane so it expands to fill available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		rightPanel.add(_versionTable.getScrollPane(), gbc);

		// --- Bottom row: Close button panel spanning the full dialog width ---
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}


	/**
	 * Queries external executables for their version strings on a background thread
	 * and appends a table row for each result as it becomes available.
	 *
	 * Two executables are queried:
	 *
	 *   The Git/Python executable via {@link VersionAction#versionAction()}.
	 *   The Python report batch file, invoked with the {@code --version} flag.
	 *
	 *
	 * Results are published back to the EDT via {@link SwingWorker#publish} so that
	 * {@link #addToTable(String, String)} is always called on the Event Dispatch Thread.
	 */
	private void fillExeTable() {
		SwingWorker<Void, List<String>> worker = new SwingWorker<Void, List<String>>() {
			/**
			 * Runs on a background thread; queries each executable and publishes
			 * {@code [name, version]} pairs for the EDT process() callback to consume.
			 *
			 * @return {@code null} (no result needed; output is delivered via publish)
			 */
			@Override
			public Void doInBackground() {
				// --- Query the Git/Python executable version ---
				VersionAction action = new VersionAction();
				String version = action.versionAction();

				List<String> rowData = new ArrayList<>();
				rowData.add(VersionAction.GIT_PYTHON_EXE);
				rowData.add(version);

				// Send this row to the EDT for table insertion
				publish(rowData);

				// --- Query the Python report batch file version ---
				String dir = AbstractReportAction.getDirectoryToUse();
				String reportExe = RMAIO.concatPath(dir, AbstractReportAction.PYTHON_REPORT_BAT);

				// Build the command list: [exePath, "--version"]
				List<String> cmd = new ArrayList<>(2);
				cmd.add(reportExe);
				cmd.add("--version");

				version = runExe(cmd);

				rowData = new ArrayList<>();
				rowData.add(AbstractReportAction.PYTHON_REPORT_BAT);
				rowData.add(version);

				// Send the report exe row to the EDT for table insertion
				publish(rowData);

				return null;
			}

			/**
			 * Called on the EDT with batched published results; adds each valid
			 * two-element list as a row in the version table.
			 *
			 * @param chunks list of published row-data lists accumulated since the last call
			 */
			@Override
			public void process(List<List<String>> chunks) {
				for (int i = 0; i < chunks.size(); i++) {
					Object chunk = chunks.get(i);

					// Only process chunks that are properly formed two-element lists
					if (chunk instanceof List) {
						List versionInfo = (List) chunk;
						if (versionInfo.size() == 2) {
							addToTable((String) versionInfo.get(0), (String) versionInfo.get(1));
						}
					}
				}
			}
		};

		// Schedule the worker for execution on the background thread pool
		worker.execute();
	}


	/**
	 * Launches an external process from the given command list, waits for it to
	 * complete, and returns the first line of output as the version string.
	 *
	 * Both stdout and stderr are captured using {@link ProcessOutputReader} threads.
	 * A 500 ms sleep in the {@code finally} block gives the output readers time to
	 * drain before they are closed.
	 *
	 * @param cmd ordered list where the first element is the executable path and
	 *            subsequent elements are its arguments
	 * @return the first line of captured output, or {@code "Unknown"} / {@code "unknown"}
	 *         if the process could not be launched or produced no output
	 */
	private String runExe(List<String> cmd) {
		System.out.println("runExe:cmd is " + cmd);

		// Configure the process with the supplied command; inherits the current environment
		ProcessBuilder builder = new ProcessBuilder(cmd);
		Process proc;

		try {
			proc = builder.start();

		} catch (IOException e) {
			// Process could not be launched (executable missing or permission denied)
			return "Unknown";
		}

		// Obtain the stdout and stderr byte streams from the running process
		InputStream iStream = proc.getInputStream();
		InputStream eStream = proc.getErrorStream();

		// Wrap byte streams in character readers for line-by-line processing
		BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream));
		BufferedReader eReader = new BufferedReader(new InputStreamReader(iStream));

		// Shared output vector; both readers append ProcessOutputLine objects to it
		Vector output = new Vector<>();

		// Start background reader threads to drain stdout and stderr concurrently
		ProcessOutputReader outputReader = new ProcessOutputReader(iReader, output, "git stdout", true, false);
		ProcessOutputReader errorReader = new ProcessOutputReader(eReader, output, "git stderr", true, true);

		try {
			// Block until the process exits, capturing its exit code
			int rv = proc.waitFor();

			// Log the exit code regardless of success or failure
			if (true) {
				System.out.println(cmd.get(0) + " exit code=" + rv);
			}

			// Return the first output line for both success (rv == 0) and failure (rv != 0)
			// because --version output may arrive on either stdout or stderr depending on the tool
			if (rv != 0) {
				if (!output.isEmpty()) {
					return ((ProcessOutputLine) output.get(0)).getLine();
				}

			} else {
				if (!output.isEmpty()) {
					return ((ProcessOutputLine) output.get(0)).getLine();
				}
			}

		} catch (InterruptedException e) {
			// Thread was interrupted while waiting for the process to finish
			e.printStackTrace();

		} finally {
			// Allow the output reader threads a brief window to finish draining before closing
			try {
				Thread.sleep(500);

			} catch (InterruptedException e) {
				// Interruption during the drain wait is non-fatal; proceed to close
			}

			// Close the stdout reader if it was successfully created
			if (outputReader != null) {
				outputReader.close();
			}

			// Close the stderr reader if it was successfully created
			if (errorReader != null) {
				errorReader.close();
			}
		}

		return "unknown";
	}


	/**
	 * Clears the version table and populates it with one row per registered
	 * {@link ReportPlugin}, resolving each plugin's version from its embedded
	 * {@code pom.xml} on the classpath.
	 *
	 * This method runs synchronously on the EDT and is called during dialog construction
	 * before the dialog is made visible.
	 */
	private void fillPluginTable() {
		// Clear any existing rows before repopulating
		_versionTable.deleteCells();

		// Retrieve the global list of registered report plugins
		List<ReportPlugin> reportPlugins = ReportsManager.getPlugins();

		ReportPlugin plugin;
		String name, path, version;

		// Loop and process each report plugin
		for (int i = 0; i < reportPlugins.size(); i++) {
			plugin = reportPlugins.get(i);

			// Each plugin declares the Maven artifact path where its pom.xml is embedded
			path = plugin.getMavenPath();

			// Resolve the version string by reading the embedded pom.xml from the classpath
			version = getBuildVersion(path);

			addToTable(plugin.getName(), version);
		}
	}


	/**
	 * Appends a single row to the version table.
	 *
	 * @param name    the component or plugin name to display in the first column
	 * @param version the resolved version string to display in the second column
	 */
	private void addToTable(String name, String version) {
		// Build a two-element row vector matching the "Component" / "Version" column order
		Vector<String> row = new Vector<>();
		row.add(name);
		row.add(version);

		_versionTable.appendRow(row);
	}


	/**
	 * Reads the build version string from the {@code pom.xml} file embedded in the JAR
	 * at the standard Maven metadata path for the specified artifact.
	 *
	 * The classpath path is constructed as:
	 * {@code META-INF/maven/<mavenPackage>/pom.xml}
	 *
	 * @param mavenPackage the Maven group/artifact path segment identifying the artifact
	 *                     (e.g. {@code "usbr.wat.plugins/usbr-actionpanel-plugin"})
	 * @return the trimmed version string extracted from the POM, or {@code "Unknown"} if
	 *         the resource cannot be located or parsed
	 */
	private String getBuildVersion(String mavenPackage) {
		// Construct the full classpath path to the embedded pom.xml
		String path = META_MAVEN_PATH + "/" + mavenPackage + "/" + POM_XML;
		LOGGER.info("Pom file path:" + path);

		// Attempt to open the pom.xml as a classpath resource
		InputStream is = getClass().getClassLoader().getResourceAsStream(path);

		// Parse the input if the stream is not null
		if (is != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));

			try {
				// Parse the pom.xml into a JDOM Document and extract the version element
				Document doc = loadDocument(reader);
				return getVersionFromDoc(doc);

			} finally {
				// Always close the reader to release the classpath resource stream
				try {
					reader.close();

				} catch (IOException e) {
					// Closing failure is non-fatal; resource will be GC'd eventually
				}
			}
		}

		// Resource was not found on the classpath — version cannot be determined
		System.out.println("getBuildVersion:failed to find resource " + path);
		return "Unknown";
	}


	/**
	 * Parses a character stream into a JDOM {@link Document} using a SAX-based parser.
	 *
	 * All checked and unchecked exceptions thrown by the SAX parser are caught and
	 * logged rather than propagated; the method returns {@code null} in those cases.
	 * Notably, {@link NullPointerException} is caught explicitly because
	 * {@link SAXBuilder#build(Reader)} can throw it when the input contains malformed
	 * XML blocks.
	 *
	 * @param reader the character stream containing the XML to parse; must not be {@code null}
	 * @return the parsed {@link Document}, or {@code null} if parsing fails for any reason
	 */
	public Document loadDocument(Reader reader) {
		Document doc = null;

		// Parse the character stream if it exists
		if (reader != null) {
			try {
				// Build the JDOM Document from the character stream using a SAX parser
				SAXBuilder builder = new SAXBuilder();
				doc = builder.build(reader);

			} catch (JDOMException e) {
				// XML is structurally invalid or violates the schema
				LOGGER.log(Level.WARNING, "loadDocument:JDOMException occurred reading XML file. Error: {0}", e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);

			} catch (NullPointerException e) {
				// SAXBuilder.build() can throw NullPointerExceptions with malformed XML blocks,
				// so we catch them here and let the caller know the document could not be read
				// by returning null.
				LOGGER.log(Level.WARNING, "loadDocument:NullPointerException occurred reading XLM file. Error: {0}",
						e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);

			} catch (IOException e) {
				// An I/O error occurred while reading from the underlying stream
				LOGGER.log(Level.WARNING, "loadDocument:IOException occurred reading XML File. Error: {0}", e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);
			}

		} else {
			LOGGER.log(Level.WARNING, "loadDocument: No XLM file given. reader is null.");
		}

		// Log a warning if the document could not be built for any reason
		if (doc == null) {
			LOGGER.log(Level.WARNING, "loadDocument: Failed to read document");
		}

		return doc;
	}


	/**
	 * Extracts the version string from a parsed Maven {@code pom.xml} JDOM document.
	 *
	 * The lookup strategy is:
	 *
	 *   If a {@code <parent>} element exists, the version is read from {@code <parent><version>}.
	 *   Otherwise, the version is read directly from the root-level {@code <version>} element.
	 *
	 * Both lookups are namespace-aware when the root element declares an XML namespace.
	 *
	 * @param doc the parsed pom.xml document; may be {@code null}
	 * @return the trimmed version string, or {@code "unknown"} if the document is null
	 *         or the version element cannot be found
	 */
	private String getVersionFromDoc(Document doc) {
		// Break early if document isn't provided
		if (doc == null) {
			return "unknown";
		}

		// Get the root page
		Element root = doc.getRootElement();

		// Retrieve the XML namespace declared on the root element (may be null for plain POM files)
		Namespace nameSpace = root.getNamespace();
		LOGGER.log(Level.INFO, "loadDocument: namespace is " + nameSpace);

		// Attempt to locate the <parent> element, respecting the namespace if present
		Element parentElem = null;
		if (nameSpace == null) {
			parentElem = root.getChild(PARENT_ELEMENT);

		} else {
			parentElem = root.getChild(PARENT_ELEMENT, nameSpace);
		}

		// Determine where to look for <version>: inside <parent> if it exists, otherwise at root level
		Element versionElem = null;
		if (parentElem == null) {
			// No <parent> block; read the version directly from the root element
			if (nameSpace == null) {
				versionElem = root.getChild(VERSION_ELEMENT);

			} else {
				versionElem = root.getChild(VERSION_ELEMENT, nameSpace);
			}

		} else {
			// <parent> exists; inherit the version from the parent declaration
			if (nameSpace == null) {
				versionElem = parentElem.getChild(VERSION_ELEMENT);

			} else {
				versionElem = parentElem.getChild(VERSION_ELEMENT, nameSpace);
			}
		}

		if (versionElem != null) {
			// Return the trimmed text content of the version element
			return versionElem.getTextTrim();

		} else {
			// Version element was not found; log the available child elements to aid debugging
			LOGGER.log(Level.WARNING, "loadDocument: Failed to find version element");
			logChildren(parentElem);
		}

		return "unknown";
	}


	/**
	 * Logs the name and namespace URI of every direct child element of the given parent element.
	 * Each child is logged at INFO level, which is useful for inspecting XML structure during
	 * development or troubleshooting. Only immediate children are logged; grandchildren and
	 * deeper descendants are not traversed.
	 *
	 * @param parent the XML Element whose direct children will be logged
	 */
	private void logChildren(Element parent) {
		// Retrieve the list of direct child elements from the parent element
		List kids = parent.getChildren();

		// Declare a variable to hold the current child element during iteration
		Element child;

		// Iterate over each child element and log its name and namespace
		for (int i = 0; i < kids.size(); i++) {
			// Cast the raw list entry to an Element for access to XML properties
			child = (Element) kids.get(i);

			// Log the tag name of the current child element at INFO level
			LOGGER.log(Level.INFO, "logChildren: child is " + child.getName());

			// Log the namespace URI of the current child element at INFO level
			LOGGER.log(Level.INFO, "logChildren: child namespace is  " + child.getNamespaceURI());
		}
	}


	/**
	 * Registers event listeners on the dialog's command button panel.
	 *
	 * Currently handles one button event:
	 *
	 *   {@link ButtonCmdPanel#CLOSE_BUTTON} — hides the dialog by calling {@code setVisible(false)}.
	 *
	 */
	protected void addListeners() {
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			/**
			 * Dispatched on the EDT when any button in the command panel is activated.
			 *
			 * @param e the action event carrying the button ID via {@link ActionEvent#getID()}
			 */
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.CLOSE_BUTTON:
						// Hide (but do not dispose) the dialog so it can be re-shown if needed
						setVisible(false);
						break;
				}
			}
		});
	}

}
