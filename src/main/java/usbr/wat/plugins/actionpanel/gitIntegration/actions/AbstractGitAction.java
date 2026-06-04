package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;             // Provides cursor types; used to show a wait cursor on the parent window during Git calls
import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for the error dialog's GridBagLayout
import java.awt.GridBagLayout;      // Flexible grid-based layout manager for the error message dialog content pane
import java.awt.Window;             // AWT Window used as the parent for the wait cursor and error dialogs
import java.io.BufferedReader;      // Buffered character reader for reading the process stdout and stderr streams
import java.io.BufferedWriter;      // Buffered character writer for sending reply text to the process stdin
import java.io.IOException;         // Checked exception thrown when starting the Git process or writing to its input fails
import java.io.InputStream;         // Raw byte stream from the spawned Git process (stdout and stderr)
import java.io.InputStreamReader;   // Wraps an InputStream as a character reader for line-by-line reading
import java.io.OutputStreamWriter;  // Wraps the process output stream as a character writer for stdin interaction
import java.util.List;              // Ordered collection interface for command argument lists and output line lists
import java.util.Vector;            // Synchronized growable array used as the mutable output line accumulator
import java.util.logging.Level;     // Log level constants; used to toggle between FINE (debug) and INFO (normal) logging
import java.util.logging.Logger;    // JDK logger for recording command details and error diagnostics
import java.util.stream.Collectors; // Stream utility for mapping ProcessOutputLine lists to plain String lists

import javax.swing.AbstractAction;  // Base class for Swing Action implementations
import javax.swing.JOptionPane;     // Provides standard error dialog boxes shown when the Git process fails to launch
import javax.swing.JScrollPane;     // Scroll container wrapping the error text area in the error message dialog
import javax.swing.RootPaneContainer; // Interface implemented by Windows; used to set a wait cursor on the content pane

import hec.io.ProcessOutputLine;     // HEC wrapper for a single line of process output, carrying the line text and stream type
import hec.io.ProcessOutputReader;   // HEC utility that reads a process stream on a background thread into a List

import rma.swing.ButtonCmdPanel;     // Panel containing a Close button for the error message dialog
import rma.swing.RmaInsets;          // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJDialog;         // Base class for RMA modal/non-modal dialog windows; used for the error dialog
import rma.swing.RmaJTextArea;       // RMA-extended multi-line text area used to display the error message
import rma.util.RMAIO;               // RMA I/O utility for path concatenation; used to build the Git tool path

/**
 * Abstract base class for all Git integration actions within the WTMP Action Panel.
 *
 * Encapsulates the mechanics of invoking the external WAT_GIT_Tool_v2.exe (or a
 * Python executable override) as a subprocess, capturing its stdout and stderr
 * output into a list of ProcessOutputLine objects, optionally sending a text reply
 * to the process stdin (e.g., for token prompts), and interpreting the exit code.
 *
 * Subclasses build a command argument list and call one of the callGit() overloads.
 * The executable is located relative to the WAT working directory (../tools/) unless
 * overridden by the UsbrGit.Python.exe system property.
 *
 * Two developer system properties are supported:
 *   - UsbrGit.DoNothing: inserts the --donothing flag into every command, causing
 *     the tool to log operations without actually sending them to Git.
 *   - UsbrGit.Debug: enables FINE-level logging and echoes command output to stdout.
 *
 * Error output is collected in _output and can be displayed via showErrorMsg().
 * The showFailedCallMsg flag controls whether failed calls automatically pop an
 * error dialog; subclasses set this via setShowFailedCallMessage().
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public abstract class AbstractGitAction extends AbstractAction {
	// Logger shared by all subclasses; named after the base class for uniform log filtering
	protected Logger _logger = Logger.getLogger(AbstractGitAction.class.getName());

	// System property key that activates "do nothing" mode (commands logged but not sent to Git)
	public static final String DO_NOTHING_PROP = "UsbrGit.DoNothing";

	// System property key that enables FINE-level debug logging of command output
	public static final String DEBUG_OUTPUT_PROP = "UsbrGit.Debug";

	// Command flag requesting that all submodules be included in the Git operation
	public static final String ALL_MODULES = "--all";

	// Command flag restricting the Git operation to the main (top-level) module only
	public static final String MAIN_MODULE = "--main";

	// Command flag prefix for specifying an individual submodule name
	public static final String SUB_MODULE = "--submodule";

	// System property key for overriding the Git tool executable path
	private static final String PYTHON_EXE_PROP = "UsbrGit.Python.exe";

	// Relative path from the WAT working directory to the tools folder containing the Git executable
	private static final String EXE_FOLDER = "../tools";

	// File name of the WAT Git integration tool executable
	public static final String GIT_PYTHON_EXE = "WAT_GIT_Tool_v2.exe";

	// Command flag specifying the local repository folder path
	public static final String LOCAL_FOLDER = "--folder";

	// Flag inserted into the command when DO_NOTHING_PROP is set; prevents actual Git operations
	protected static final String DO_NOTHING = "--donothing";

	// Logical name used to identify the main study module (as opposed to submodule names)
	public static final String STUDY_MODULE = "Study";

	// The parent window for the wait cursor and error dialogs
	private Window _parent;

	// Accumulated output lines from the most recent Git process invocation (stdout + stderr)
	private List<ProcessOutputLine> _output;

	// Flag controlling whether failed callGit() invocations automatically show an error dialog
	private boolean _showFailedCallMsg;

	/**
	 * Constructs an AbstractGitAction with the given display name and parent window.
	 *
	 * @param name   the display name for this action (used as the button or menu item label)
	 * @param parent the Window used as the owner for wait cursors and error dialogs
	 */
	public AbstractGitAction(String name, Window parent) {
		super(name);
		_parent = parent;
	}

	/**
	 * Returns the parent window associated with this action.
	 *
	 * @return the Window used as the owner for dialogs and cursors
	 */
	public Window getParent() {
		return _parent;
	}

	/**
	 * Invokes the Git tool with the given command arguments, collecting all output.
	 *
	 * Convenience overload that does not send any stdin reply to the process.
	 *
	 * @param cmd the list of command argument strings (the executable path is prepended automatically)
	 * @return true if the process exited with code 0; false otherwise
	 */
	protected boolean callGit(List<String> cmd) {
		return callGit(cmd, null, null);
	}

	/**
	 * Invokes the Git tool with the given command arguments, optionally sending a
	 * text reply to the process stdin when a specific prompt string is detected.
	 *
	 * Prepends the Git executable path to the command, applies debug and do-nothing
	 * flags based on system properties, launches the process, and reads both stdout
	 * and stderr concurrently into _output via ProcessOutputReader threads. If a
	 * lookForText string is provided, waits until that string appears in the output
	 * before sending replyText to the process stdin.
	 * <p>
	 * Shows a wait cursor on the parent window's content pane for the duration of the call.
	 * Returns false if the process fails to start, exits with a non-zero code, or is
	 * interrupted while waiting.
	 *
	 * @param cmd         the list of command argument strings (executable is prepended)
	 * @param lookForText a string to wait for in the process output before sending a reply; null to skip
	 * @param replyText   text to write to the process stdin after lookForText is detected; null to skip
	 * @return true if the process exited with code 0; false on any failure
	 */
	protected boolean callGit(List<String> cmd, String lookForText, String replyText) {
		// Show the wait cursor on the parent window's content pane
		if (_parent != null) {
			((RootPaneContainer) _parent).getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}

		try {
			// Resolve and prepend the Git tool executable path to the command
			String exe = getExe();
			cmd.add(0, exe);

			// Enable FINE logging when debug output is requested
			boolean echoOutput = Boolean.getBoolean(DEBUG_OUTPUT_PROP);
			if (echoOutput) {
				_logger.setLevel(Level.FINE);
			} else {
				_logger.setLevel(Level.INFO);
			}

			// Insert the --donothing flag in position 2 when test mode is active
			if (Boolean.getBoolean(DO_NOTHING_PROP)) {
				cmd.add(2, DO_NOTHING);
			}

			// Log the full command at FINE level for diagnostic purposes
			_logger.fine("Command is:" + cmd);

			// Build and launch the Git tool process
			ProcessBuilder builder = new ProcessBuilder(cmd);
			Process proc;
			try {
				proc = builder.start();
			} catch (IOException e) {
				// Log and show an error dialog if the process cannot be started
				_logger.info("Failed to launch Git Command " + cmd + " Error:" + e);
				JOptionPane.showMessageDialog(_parent, "Failed to launch Git command " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			BufferedWriter inputWriter = null;

			// Obtain the process stdout and stderr streams
			InputStream iStream = proc.getInputStream();
			InputStream eStream = proc.getErrorStream();

			// Wrap both streams as buffered character readers
			BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream));
			BufferedReader eReader = new BufferedReader(new InputStreamReader(iStream));

			// Initialize the shared output accumulator
			_output = new Vector<>();

			// Start background reader threads for stdout and stderr
			ProcessOutputReader outputReader = new ProcessOutputReader(iReader, _output, "git stdout", echoOutput, false);
			ProcessOutputReader errorReader = new ProcessOutputReader(eReader, _output, "git stderr", echoOutput, true);

			if (replyText != null) {
				// Wait for the prompt text to appear in the output before sending the reply
				lookFor(lookForText, _output);
				inputWriter = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
				try {
					// Log only the first two characters of the reply to avoid exposing sensitive data
					_logger.fine("Sending reply text: " + replyText.substring(0, 2) + "....");
					inputWriter.write(replyText);
					inputWriter.newLine();
					inputWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				// Block the current thread until the process exits
				int rv = proc.waitFor();

				if (echoOutput) {
					// Print the exit code to stdout when debug output is enabled
					System.out.println(cmd.get(0) + " exit code=" + rv);
				}

				if (rv != 0) {
					// Non-zero exit code: log and optionally show an error dialog
					showErrorMsg(cmd, _output);
					return false;
				} else {
					return true;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				// Brief pause to allow the output reader threads to finish draining
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

				// Close both output reader threads to release stream resources
				if (outputReader != null) {
					outputReader.close();
				}
				if (errorReader != null) {
					errorReader.close();
				}

				// Close the stdin writer if one was opened for reply-text interaction
				if (inputWriter != null) {
					try {
						inputWriter.close();
					} catch (IOException e) {
					}
				}
			}
		} finally {
			// Always restore the default cursor when the call completes
			if (_parent != null) {
				((RootPaneContainer) _parent).getContentPane().setCursor(Cursor.getDefaultCursor());
			}
		}

		return false;
	}

	/**
	 * Polls the output list for up to 5 × 300 ms intervals waiting for a line that
	 * contains the given text string.
	 *
	 * Used to detect when the Git process emits a prompt (e.g., "Please Enter token")
	 * before writing a reply to its stdin.
	 *
	 * @param lookForText the substring to search for in the accumulated output lines
	 * @param output      the shared output list populated by the ProcessOutputReader threads
	 * @return true if the text was found within the polling window; false if the timeout expired
	 */
	protected boolean lookFor(String lookForText, List<ProcessOutputLine> output) {
		ProcessOutputLine line;
		int cnt = 0;

		do {
			// Scan all lines collected so far for the target text
			for (int i = 0; i < output.size(); i++) {
				line = output.get(i);
				if (line.getLine().contains(lookForText)) {
					_logger.fine("Found " + lookForText);
					return true;
				}
			}

			// Wait 300 ms before the next polling attempt
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}

			cnt++;
		}
		while (cnt < 5);

		// Text not found within the polling window
		_logger.info("Failed to find: " + lookForText);
		return false;
	}

	/**
	 * Logs the error output from a failed Git call and conditionally shows an error dialog.
	 *
	 * Builds an error message string from the command and output lines via getErrorMessage(),
	 * logs it at INFO level, and — if _showFailedCallMsg is true — displays it in a scrollable
	 * error dialog via the static showErrorMsg() overload.
	 *
	 * @param cmd    the command argument list that was executed
	 * @param output the output lines collected from the process
	 */
	protected void showErrorMsg(List<String> cmd, List<ProcessOutputLine> output) {
		// Build the full error message from the command and collected output
		String msg = getErrorMessage("Error Running:", cmd, _output);
		_logger.info(msg);

		// Only show the dialog if the flag is set; avoids spurious popups for background checks
		if (!_showFailedCallMsg) {
			return;
		}

		// Display the error in a scrollable dialog if a parent window is available
		if (_parent != null) {
			showErrorMsg(_parent, "Error", msg);
		}
	}

	/**
	 * Displays a scrollable error message dialog containing the given text.
	 *
	 * Creates a non-editable RmaJTextArea in a JScrollPane, embeds it in a modal
	 * RmaJDialog with a Close button, packs and centers the dialog, then shows it.
	 * This is a static utility method so it can also be called from subclasses that
	 * do not hold a reference to the action instance.
	 *
	 * @param parent the Window that will own the error dialog
	 * @param title  the title bar text for the error dialog
	 * @param msg    the error message text to display in the scrollable text area
	 */
	public static void showErrorMsg(Window parent, String title, String msg) {
		// Create a read-only text area to display the error message
		RmaJTextArea textArea = new RmaJTextArea(5, 80);
		textArea.setEditable(false);
		JScrollPane sp = new JScrollPane(textArea);
		textArea.setText(msg);

		// Build the error dialog with a GridBagLayout content pane
		RmaJDialog dlg = new RmaJDialog(parent, title, true);
		dlg.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// Position the scroll pane to fill all available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		dlg.getContentPane().add(sp, gbc);

		// Create the Close button panel and position it at the bottom of the dialog
		ButtonCmdPanel cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		dlg.getContentPane().add(cmdPanel, gbc);

		// Hide the dialog when the Close button is clicked
		cmdPanel.addCmdPanelListener(e -> dlg.setVisible(false));

		dlg.pack();
		dlg.setLocationRelativeTo(parent);
		dlg.setVisible(true);
	}

	/**
	 * Controls whether a failed callGit() call automatically displays an error dialog.
	 *
	 * Set to true for user-initiated operations where the user should be notified of
	 * failures. Set to false for background checks (e.g., OkToPushAction) where the
	 * caller handles the error display independently.
	 *
	 * @param showMsg true to show an error dialog on failure; false to suppress it
	 */
	public void setShowFailedCallMessage(boolean showMsg) {
		_showFailedCallMsg = showMsg;
	}

	/**
	 * Builds a human-readable error message string from a header, command list, and output lines.
	 *
	 * @param header an optional prefix string (e.g., "Error Running:"); null to omit
	 * @param cmd    the command argument list that was executed
	 * @param output the output lines collected from the process
	 * @return a newline-separated string combining the header, command, and all output lines
	 */
	public static String getErrorMessage(String header, List<String> cmd, List<ProcessOutputLine> output) {
		StringBuilder builder = new StringBuilder();

		// Prepend the header if one was provided
		if (header != null) {
			builder.append(header);
		}

		// Append the full command argument list
		builder.append(cmd);
		builder.append("\n");

		// Append each output line on its own line
		for (int i = 0; i < output.size(); i++) {
			builder.append("\n");
			builder.append(output.get(i));
		}

		return builder.toString();
	}

	/**
	 * Resolves the path to the WAT Git tool executable.
	 *
	 * First checks the UsbrGit.Python.exe system property for an explicit override.
	 * If not set, constructs the path as: [user.dir]/../tools/WAT_GIT_Tool_v2.exe,
	 * where user.dir is the directory from which the WAT application is running.
	 *
	 * @return the absolute path string to the Git tool executable
	 */
	private static String getExe() {
		// Check for an explicit executable path override via system property
		String exe = System.getProperty(PYTHON_EXE_PROP);

		if (exe == null) {
			// Build the default path relative to the WAT working directory
			String folder = System.getProperty("user.dir"); // directory where WAT.exe is running from
			folder = RMAIO.concatPath(folder, EXE_FOLDER);
			exe = RMAIO.concatPath(folder, GIT_PYTHON_EXE);
		}

		return exe;
	}

	/**
	 * Wraps the given string in double quotes if it contains spaces.
	 *
	 * Strings without spaces are returned unchanged. This is used to ensure that
	 * file paths with spaces are correctly interpreted as single arguments by the
	 * Git tool's command parser.
	 *
	 * @param aString the string to conditionally quote
	 * @return the original string, or the string wrapped in double quotes if it contains a space
	 */
	protected String quoteString(String aString) {
		// Return the string unchanged if it does not contain any spaces
		if (aString.indexOf(' ') == -1) {
			return aString;
		}

		// Wrap the string in double quotes for safe command-line passing
		StringBuilder builder = new StringBuilder("\"");
		builder.append(aString);
		builder.append("\"");
		return builder.toString();
	}

	/**
	 * Returns the raw output lines collected from the most recent callGit() invocation.
	 *
	 * The list contains both stdout and stderr lines interleaved in arrival order,
	 * each wrapped in a ProcessOutputLine that carries its source stream type.
	 *
	 * @return the list of ProcessOutputLine objects from the last Git call; may be null before any call
	 */
	public List<ProcessOutputLine> getOutput() {
		return _output;
	}

	/**
	 * Extracts the plain text content from a list of ProcessOutputLine objects.
	 *
	 * Maps each ProcessOutputLine to its line string and collects the results into
	 * a new List, discarding stream-type metadata.
	 *
	 * @param output the list of ProcessOutputLine objects to convert
	 * @return a List of plain String lines in the same order as the input
	 */
	protected static List<String> getOutputLines(List<ProcessOutputLine> output) {
		return output.stream().map(l -> l.getLine()).collect(Collectors.toList());
	}
}
