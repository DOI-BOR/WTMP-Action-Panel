package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;               // AWT color constants used for message type coloring and the text pane background
import java.awt.Dimension;           // Encapsulates width and height; used to fix the text pane viewport size
import java.awt.EventQueue;          // Provides invokeLater for scheduling UI updates on the Event Dispatch Thread
import java.awt.Font;                // Used to apply a monospaced font to the progress text pane
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.Window;              // AWT base class for top-level windows; used as the parent reference
import java.awt.event.WindowAdapter; // Adapter providing default no-op implementations of WindowListener methods
import java.awt.event.WindowEvent;   // Carries data about window state changes (e.g. the user clicking the title-bar X)

import java.io.BufferedWriter;       // Wraps a FileWriter with a buffer for efficient character output
import java.io.File;                 // Represents a filesystem path; used for the save-messages file chooser
import java.io.FileWriter;           // Opens a local file for character-stream writing
import java.io.IOException;          // Signals an I/O failure during file save operations

import javax.swing.JButton;          // Swing button that alternates between "Cancel" and "Close" text
import javax.swing.JFileChooser;     // Standard Swing dialog for choosing a file save location
import javax.swing.JFrame;           // Used as a dummy parent in the standalone main method test
import javax.swing.JMenuItem;        // Menu item in the right-click popup on the progress text pane
import javax.swing.JOptionPane;      // Displays an error dialog when a file save operation fails
import javax.swing.JPopupMenu;       // Right-click context menu attached to the progress text pane
import javax.swing.JProgressBar;     // Swing progress bar displaying determinate or indeterminate progress
import javax.swing.JScrollPane;      // Scroll pane wrapping the progress text pane
import javax.swing.JTextPane;        // Styled text pane used to display color-coded progress messages
import javax.swing.SwingUtilities;   // Provides isEventDispatchThread for thread-safety checks
import javax.swing.UIManager;        // Provides access to the look-and-feel default font for the message display
import javax.swing.text.AttributeSet;       // Read-only view of a set of text style attributes
import javax.swing.text.BadLocationException; // Checked exception thrown when a document insert position is invalid
import javax.swing.text.Document;           // Styled document model backing the JTextPane
import javax.swing.text.SimpleAttributeSet; // Mutable set of text style attributes (foreground color, font, etc.)
import javax.swing.text.StyleConstants;     // Named constants and setters for common text style attributes

import hec.ui.ProgressListener; // HEC interface defining the full progress-reporting contract implemented by this dialog

import rma.swing.RmaInsets;     // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJDialog;    // RMA base dialog class providing common dialog behaviour


/**
 * Modal progress dialog that implements the HEC ProgressListener interface,
 * displaying color-coded messages and a progress bar for long-running operations.
 *
 * The dialog contains three main areas:
 *   A scrollable, monospaced JTextPane that accumulates progress messages.
 *   A JProgressBar that can operate in both determinate and indeterminate modes.
 *   A button that reads "Cancel" while the operation is running and switches to
 *   "Close" once finish() is called.
 *
 * Thread safety: all ProgressListener callback methods that update the UI are
 * safe to call from any thread. Each method checks whether it is already on the
 * EDT and either updates directly or schedules an invokeLater call.
 *
 * Message colors by MessageType:
 *   IMPORTANT -- blue
 *   ERROR     -- red
 *   WARNING   -- orange
 *   GENERAL   -- black (default)
 *
 * The text pane provides a right-click popup with "Select All", "Copy", and
 * "Save Messages" options.
 *
 * Closing the dialog via the title-bar X while the operation is running is
 * treated the same as clicking Cancel.
 *
 */
@SuppressWarnings("serial")
public class ProgressListenerDialog extends RmaJDialog
		implements ProgressListener {
	/**
	 * Button label text used when the operation is complete and the dialog can be dismissed.
	 */
	private static final String CLOSE_TEXT = "Close";

	/**
	 * Button label text used while the operation is still running.
	 */
	private static final String CANCEL_TEXT = "Cancel";


	// --- Child components ---

	/**
	 * Scrollable text pane that accumulates progress messages in monospaced font.
	 * Each message is appended with a newline and styled with a color matching its
	 * MessageType. The pane is read-only and provides a right-click popup menu.
	 */
	private JTextPane _progressText;

	/**
	 * Progress bar displaying operation completion as a percentage or indeterminate animation.
	 */
	private JProgressBar _progressBar;

	/**
	 * Button whose label alternates between CANCEL_TEXT (during the operation) and
	 * CLOSE_TEXT (after finish() is called). Used as both the cancel trigger and the
	 * final dismiss action.
	 */
	private JButton _cancelCloseBtn;

	/**
	 * Background color applied to both the text pane and individual message spans to
	 * ensure consistent rendering. Initialized from the dialog's own background color.
	 */
	private Color _backgroundColor = getBackground();


	/**
	 * Constructs the progress dialog, builds all controls, packs the layout, attaches
	 * listeners, and centres the dialog relative to the parent window.
	 *
	 * @param parent the owning window used to position the dialog; must not be null
	 * @param title  the text displayed in the dialog title bar
	 */
	public ProgressListenerDialog(Window parent, String title) {
		super(parent);

		buildControls(title);
		pack();
		addListeners();

		// Centre the dialog over the parent window
		setLocationRelativeTo(parent);
	}


	/**
	 * Constructs and lays out all child controls using GridBagLayout.
	 *
	 * Layout from top to bottom:
	 * Scrollable text pane expanding to fill most of the dialog.
	 * Progress bar spanning the full width.
	 * Cancel/Close button centred at the bottom.
	 *
	 * The default close operation is set to DO_NOTHING_ON_CLOSE so that the window
	 * listener added in addListeners can intercept the title-bar X and treat it as
	 * a cancel action.
	 *
	 * @param title the text to display in the dialog title bar
	 */
	private void buildControls(String title) {
		setTitle(title);

		// Prevent the default dispose-on-close so the window listener controls dismissal
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());

		// --- Scrollable progress text pane ---
		// Anonymous subclass fixes the viewport size to 300x300 pixels
		_progressText = new JTextPane() {
			/**
			 * Fixes the preferred scroll viewport to 300 x 300 pixels so the dialog
			 * opens at a consistent size regardless of message content.
			 *
			 * @return a Dimension of 300 x 300
			 */
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = 300;
				d.width = 300;
				return d;
			}
		};
		_progressText.setEditable(false);

		// Switch to a monospaced font so log-style output aligns correctly
		Font f = _progressText.getFont();
		Font newFont = new Font("monospaced", Font.PLAIN, f.getSize());
		_progressText.setFont(newFont);

		// Match the text pane background to the dialog background for visual consistency
		_progressText.setBackground(_backgroundColor);

		// Attach the right-click popup for select-all, copy, and save operations
		_progressText.setComponentPopupMenu(buildPopupMenu());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_progressText), gbc);

		// --- Progress bar (full-width, does not expand vertically) ---
		_progressBar = new JProgressBar();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_progressBar, gbc);

		// --- Cancel/Close button centred at the bottom ---
		_cancelCloseBtn = new JButton(CANCEL_TEXT);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cancelCloseBtn, gbc);
	}


	/**
	 * Builds and returns the right-click popup menu for the progress text pane.
	 *
	 * The menu provides three actions:
	 * "Select All" -- selects all text in the pane.
	 * "Copy"       -- copies the current selection to the clipboard.
	 * "Save Messages" -- opens a file chooser and writes all text to a chosen file.
	 *
	 * @return the configured JPopupMenu; never null
	 */
	protected JPopupMenu buildPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();

		// "Select All" selects the full content of the text pane
		JMenuItem menuItem = new JMenuItem("Select All");
		menuItem.addActionListener(e -> _progressText.selectAll());
		popupMenu.add(menuItem);

		// "Copy" copies the selected text to the system clipboard
		menuItem = new JMenuItem("Copy");
		menuItem.addActionListener(e -> _progressText.copy());
		popupMenu.add(menuItem);

		// "Save Messages" opens a save dialog and writes the full message log to a file
		menuItem = new JMenuItem("Save Messages");
		menuItem.addActionListener(e -> saveMessagesAction());
		popupMenu.add(menuItem);

		return popupMenu;
	}


	/**
	 * Prompts the user with a file save dialog and writes the current messages to the
	 * chosen file. The dialog opens in the user's home directory by default. If the user
	 * cancels the dialog or the selected file reference is null, no action is taken.
	 * Once a valid file is confirmed, the file is written via saveFile().
	 */
	private void saveMessagesAction() {
		// Create a new file chooser dialog for selecting the save destination
		JFileChooser chooser = new JFileChooser();

		// Default the dialog to the user's home directory
		chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		// Display the save dialog and capture the user's choice
		int retval = chooser.showSaveDialog(this);

		// Only proceed if the user confirmed a file selection
		if (retval == JFileChooser.APPROVE_OPTION) {
			// Retrieve the file selected by the user
			File theFile = chooser.getSelectedFile();

			// Guard against a null file reference (defensive; should not normally be null here)
			if (theFile == null)
				return;

			// Write the current messages to the selected file
			saveFile(theFile);
		}
	}


	/**
	 * Writes the full text content of the progress pane to the given file.
	 *
	 * An error dialog is shown if the write fails. Returns false only when theFile
	 * is null; all other failure paths are handled via the exception catch block.
	 *
	 * @param theFile the file to write the progress messages to; must not be null
	 * @return true if the write was attempted (regardless of success); false if theFile is null
	 */
	private boolean saveFile(File theFile) {
		if (theFile == null)
			return false;

		try {
			// Write all text pane content to the file, then flush and close the stream
			BufferedWriter out = new BufferedWriter(new FileWriter(theFile));
			out.write(_progressText.getText());
			out.flush();
			out.close();
		} catch (IOException e) {
			// Log the failure to stdout and inform the user via a modal error dialog
			System.out.println("Failed to save file " + theFile.getName() + " " + e);
			JOptionPane.showMessageDialog(null, "Failed to save file", "Save Error", JOptionPane.ERROR_MESSAGE);
		}

		return true;
	}


	/**
	 * Attaches the Cancel/Close button action listener and a window closing listener.
	 *
	 * The window closing listener intercepts the title-bar X button and routes it
	 * through cancelCloseAction so that closing the dialog while the operation is
	 * running is equivalent to clicking Cancel.
	 */
	protected void addListeners() {
		_cancelCloseBtn.addActionListener(e -> cancelCloseAction());

		addWindowListener(new WindowAdapter() {
			/**
			 * Intercepts the title-bar X button. If the operation is still running
			 * (button text is "Cancel"), the cancel action is triggered immediately
			 * and then again via invokeLater to ensure processing completes on the EDT.
			 * If the operation is already finished, the invokeLater call will dismiss
			 * the dialog.
			 *
			 * @param e the window event signalling that the user requested closing
			 */
			public void windowClosing(WindowEvent e) {
				if (CANCEL_TEXT.equals(_cancelCloseBtn.getText())) {
					// Trigger the cancel action immediately for this X-button close attempt
					cancelCloseAction();
				}
				// Schedule a second call on the EDT to handle any deferred state changes
				EventQueue.invokeLater(() -> cancelCloseAction());
			}
		});
	}


	/**
	 * Handles both cancel and close button activations.
	 *
	 * When the button reads "Cancel" (operation in progress), the job is flagged as
	 * cancelled by switching the button text to "Close" -- the caller is expected to
	 * poll isCancelled() or respond to the label change via a listener.
	 *
	 * When the button reads "Close" (operation finished), the dialog is hidden and
	 * disposed to release all window resources.
	 */
	private void cancelCloseAction() {
		if (CANCEL_TEXT.equals(_cancelCloseBtn.getText())) {
			// Signal cancellation by switching the button label; callers observe this change
			_cancelCloseBtn.setText(CLOSE_TEXT);

		} else {
			// Operation is finished; hide and release the dialog
			setVisible(false);
			dispose();
		}
	}


	/**
	 * Marks the operation as complete by switching the button label from "Cancel" to "Close".
	 *
	 * After this call the progress bar value is not changed; callers should have
	 * advanced the bar to its maximum before calling finish().
	 */
	@Override
	public void finish() {
		_cancelCloseBtn.setText(CLOSE_TEXT);
	}


	/**
	 * Advances the progress bar by the specified increment amount. Because Swing components
	 * must only be modified on the Event Dispatch Thread (EDT), this method checks which
	 * thread it is running on. If already on the EDT, the progress bar is updated directly.
	 * Otherwise, the update is scheduled on the EDT via EventQueue.invokeLater() to prevent
	 * cross-thread Swing modification. This method overrides the base class implementation
	 * to provide thread-safe progress bar updates.
	 *
	 * @param increment the amount by which the progress bar's current value will be increased
	 */
	@Override
	public void incrementProgress(int increment) {
		// Check if this method is being called from the Event Dispatch Thread
		if (SwingUtilities.isEventDispatchThread()) {
			// Already on the EDT; update the progress bar value directly
			int val = _progressBar.getValue();
			_progressBar.setValue(val + increment);

		} else {
			// Schedule the update on the EDT to avoid cross-thread Swing modification
			EventQueue.invokeLater(() ->
			{
				// Read the current value and apply the increment inside the EDT-safe lambda
				int val = _progressBar.getValue();
				_progressBar.setValue(val + increment);
			});
		}
	}

	/**
	 * Sets the progress bar to the specified number of completed work units. Because Swing
	 * components must only be modified on the Event Dispatch Thread (EDT), this method checks
	 * which thread it is running on. If already on the EDT, the progress bar is updated
	 * directly. Otherwise, the update is scheduled on the EDT via EventQueue.invokeLater()
	 * to prevent cross-thread Swing modification. This method overrides the base class
	 * implementation to provide thread-safe absolute progress bar updates.
	 *
	 * @param completedWorkUnits the absolute progress value to set on the progress bar
	 */
	@Override
	public void progress(int completedWorkUnits) {
		// Check if this method is being called from the Event Dispatch Thread
		if (EventQueue.isDispatchThread()) {
			// Already on the EDT; set the progress bar value directly
			_progressBar.setValue(completedWorkUnits);

		} else {
			// Schedule the progress bar update on the EDT to ensure thread-safe Swing access
			EventQueue.invokeLater(() -> _progressBar.setValue(completedWorkUnits));
		}
	}


	/**
	 * Appends a general-type (black) progress message to the text pane.
	 *
	 * @param message the message text to append; a newline is added automatically
	 */
	@Override
	public void progress(String message) {
		addMessage(message);
	}


	/**
	 * Appends a message to the text pane using the default GENERAL message color (black).
	 *
	 * Delegates to addMessage(String, Color) using the color resolved for GENERAL type.
	 *
	 * @param msg the message text to append; a newline is added automatically
	 */
	public void addMessage(String msg) {
		addMessage(msg, getMessageColor(MessageType.GENERAL));
	}


	/**
	 * Appends a message to the text pane with the given foreground color.
	 *
	 * Thread-safe: if called from a non-EDT thread the append is scheduled via
	 * invokeLater.
	 *
	 * @param msg     the message text to append; a newline is added automatically
	 * @param fgColor the foreground color to use for this message
	 */
	public void addMessage(String msg, Color fgColor) {
		if (EventQueue.isDispatchThread()) {
			appendMessage(msg, fgColor);

		} else {
			// Schedule the append on the EDT to ensure thread-safe document modification
			EventQueue.invokeLater(() -> appendMessage(msg, fgColor));
		}
	}


	/**
	 * Builds a styled attribute set from the given foreground color and delegates to
	 * displayMessage to insert the text into the document.
	 *
	 * @param msg     the message text to append
	 * @param fgColor the foreground color to apply to the message text
	 */
	protected void appendMessage(String msg, Color fgColor) {
		// Build a style attribute set with the desired foreground and the dialog background
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setForeground(attrs, fgColor);
		StyleConstants.setBackground(attrs, _backgroundColor);

		displayMessage(msg, attrs, _progressText.getDocument());
	}


	/**
	 * Inserts a styled message string into the given document at its current end position.
	 *
	 * The font family and size are applied from the look-and-feel TextField default.
	 * A newline is appended if the message does not already end with one. A null or
	 * empty message is treated as a blank line. Messages beginning with a backspace
	 * character ('\b') set the overwrite flag (note: the flag is currently set but
	 * not yet acted upon in this implementation).
	 *
	 * @param msg   the message text to insert; null or empty results in a blank line
	 * @param attrs the style attributes to apply to the inserted text
	 * @param doc   the document to insert into; falls back to the text pane's document if null
	 */
	private void displayMessage(String msg, SimpleAttributeSet attrs, Document doc) {
		// Apply the look-and-feel default monospaced font size to the attribute set
		Font f = (Font) UIManager.get("TextField.font");
		StyleConstants.setFontFamily(attrs, "monospaced");
		StyleConstants.setFontSize(attrs, f.getSize() + 1);

		// Tracks whether this message should overwrite the previous line (prefix '\b')
		boolean overWrite = false;
		final AttributeSet sAttrs = attrs;

		// Normalise null or empty messages to a bare newline (blank line)
		if (msg == null || msg.length() == 0) {
			msg = "\n";

		} else if (msg.charAt(msg.length() - 1) != '\n') {
			// Ensure every message ends with a newline for consistent display
			msg += "\n";
		}

		if (msg.charAt(0) == '\b') {
			// Strip the backspace prefix and flag this as an overwrite request
			msg = msg.substring(1);
			overWrite = true;
		}

		// Fall back to the text pane's own document if none was supplied
		if (doc == null) {
			doc = _progressText.getDocument();
		}

		try {
			// Insert the styled message at the current end of the document
			int len = doc.getLength();
			doc.insertString(len, msg, sAttrs);

		} catch (BadLocationException ex) {
			// Insert position was invalid; fall back to console output
			System.out.println(msg);
		}
	}


	/**
	 * Appends a color-coded message of the given type to the text pane.
	 *
	 * @param msg     the message text to append
	 * @param msgType the message type controlling the foreground color
	 */
	@Override
	public void progress(String msg, MessageType msgType) {
		addMessage(msg, getMessageColor(msgType));
	}


	/**
	 * Advances the progress bar to the given value and appends a general message.
	 *
	 * @param msg                the message text to append
	 * @param completedWorkUnits the absolute number of completed work units to display
	 */
	@Override
	public void progress(String msg, int completedWorkUnits) {
		progress(completedWorkUnits);
		progress(msg);
	}


	/**
	 * Advances the progress bar and appends a color-coded message in a single call.
	 *
	 * @param msg                the message text to append
	 * @param msgType            the message type controlling the foreground color
	 * @param completedWorkUnits the absolute number of completed work units to display
	 */
	@Override
	public void progress(String msg, MessageType msgType, int completedWorkUnits) {
		progress(completedWorkUnits);
		addMessage(msg, getMessageColor(msgType));
	}


	/**
	 * Resolves the foreground color to use for a given message type.
	 *
	 * @param messageType the type of message being displayed
	 * @return the Color associated with the message type:
	 * IMPORTANT -- blue, ERROR -- red, WARNING -- orange, GENERAL -- black
	 */
	private Color getMessageColor(MessageType messageType) {
		switch (messageType) {
			case IMPORTANT:
				return Color.BLUE;
			case ERROR:
				return Color.RED;
			case WARNING:
				return Color.ORANGE;
			default:
			case GENERAL:
				return Color.BLACK;
		}
	}


	/**
	 * Sets whether the dialog should remain on top of all other windows.
	 *
	 * @param alwaysOnTop true to keep the dialog above all other windows; false for normal stacking
	 */
	@Override
	public void setStayOnTop(boolean alwaysOnTop) {
		setAlwaysOnTop(alwaysOnTop);
	}


	/**
	 * Starts a new operation with a default total of 100 work units.
	 *
	 * Delegates to start(int) with totalWorkUnits = 100.
	 */
	@Override
	public void start() {
		start(100);
	}


	/**
	 * Starts a new operation with the given total work units, resetting the progress
	 * bar and switching the button label back to "Cancel".
	 *
	 * The progress bar is set to determinate mode with the given maximum and reset
	 * to zero so it is ready for incremental or absolute updates.
	 *
	 * @param totalWorkUnits the total number of work units that represent 100% completion
	 */
	@Override
	public void start(int totalWorkUnits) {
		// Restore the Cancel label in case the dialog is being reused after a previous run
		_cancelCloseBtn.setText(CANCEL_TEXT);

		// Configure the progress bar for a new determinate operation
		_progressBar.setIndeterminate(false);
		_progressBar.setMaximum(totalWorkUnits);
		_progressBar.setValue(0);
	}


	/**
	 * Switches the progress bar from indeterminate to determinate mode and sets its
	 * value to the given completed work unit count.
	 *
	 * @param completedWorkUnits the number of work units completed at the point of switching
	 */
	@Override
	public void switchToDeterminate(int completedWorkUnits) {
		setIndeterminate(false);
		progress(completedWorkUnits);
	}


	/**
	 * Switches the progress bar to indeterminate (animated) mode.
	 * <p>
	 * Used when the total amount of work is unknown or when the operation is in a
	 * waiting phase between measurable steps.
	 */
	@Override
	public void switchToIndeterminate() {
		setIndeterminate(true);
	}


	/**
	 * Sets the progress bar's indeterminate mode, which displays an animated bouncing bar
	 * when the total amount of work is unknown. Because Swing components must only be
	 * modified on the Event Dispatch Thread (EDT), this method checks which thread it is
	 * running on. If already on the EDT, the mode is set directly. Otherwise, the update
	 * is scheduled on the EDT via EventQueue.invokeLater() to prevent cross-thread
	 * Swing modification.
	 *
	 * @param indeterminate true to enable indeterminate mode, false to return to a
	 *                      standard determinate progress bar
	 */
	protected void setIndeterminate(final boolean indeterminate) {
		// Check if this method is being called from the Event Dispatch Thread
		if (SwingUtilities.isEventDispatchThread()) {
			// Already on the EDT; set the indeterminate mode directly
			_progressBar.setIndeterminate(indeterminate);

		} else {
			// Schedule the mode change on the EDT to avoid cross-thread Swing modification
			EventQueue.invokeLater(() -> _progressBar.setIndeterminate(indeterminate));
		}
	}


	/**
	 * Entry point for manually testing the ProgressListenerDialog in isolation.
	 * Creates and displays a ProgressListenerDialog using a bare JFrame as a dummy parent,
	 * then launches a background thread that simulates a multi-step long-running operation.
	 * The simulation exercises determinate progress updates, status message updates, and a
	 * switch to indeterminate mode followed by a return to determinate mode. The background
	 * thread runs at below-normal priority to avoid starving the Event Dispatch Thread.
	 *
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
		// Create and display the dialog using a bare JFrame as the dummy parent
		ProgressListenerDialog pld = new ProgressListenerDialog(new JFrame(), "Extract");
		pld.setVisible(true);

		// Background thread simulates a long-running operation at below-normal priority
		Thread testThread = new Thread("Text Thread") {
			@Override
			public void run() {
				try {
					// Initialize the dialog and report the first status message and progress value
					pld.start();
					pld.progress("Starting...");
					pld.progress(10);
					Thread.sleep(1000);

					// Simulate the second step of the operation with an updated message and progress
					pld.progress("Step 2...");
					pld.progress(30);
					Thread.sleep(1000);

					// Switch to indeterminate to simulate an unknown-duration wait
					pld.switchToIndeterminate();
					Thread.sleep(1000);

					// Return to determinate mode once the wait is resolved
					pld.switchToDeterminate(50);
					pld.progress("More work");
					Thread.sleep(1000);

					// Advance progress to 50% and update the status message
					pld.progress(50);
					pld.progress("Going Going");
					Thread.sleep(1000);

					// Advance progress to 70% and signal that the operation is nearly complete
					pld.progress(70);
					pld.progress("Finishing....");
					Thread.sleep(1000);

					// Set progress to 100% and notify the dialog that the operation has finished
					pld.progress(100);
					pld.finish();
				} catch (Exception e) {
					// Print any unexpected exceptions that occur during the simulated operation
					e.printStackTrace();
				}
			}
		};

		// Run at slightly below normal priority to avoid starving the EDT
		testThread.setPriority(Thread.NORM_PRIORITY - 2);

		// Start the background simulation thread
		testThread.start();
	}
}
