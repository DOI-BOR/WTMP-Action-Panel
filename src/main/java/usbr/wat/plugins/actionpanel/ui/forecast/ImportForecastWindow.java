package usbr.wat.plugins.actionpanel.ui.forecast;

import rma.swing.RmaJDialog;                        // Provides RmaJDialog as the base modal/non-modal dialog class this window extends
import java.awt.Window;                             // Provides Window as the parent component type accepted by the superclass constructor
import java.awt.event.WindowAdapter;                // Provides WindowAdapter as a no-op base class for window event listeners, allowing selective override of only the windowClosing event
import java.awt.event.WindowEvent;                  // Provides WindowEvent carrying the source and type of the window lifecycle event

/**
 * Abstract base class for all forecast data import dialogs in the WTMP action panel UI.
 *
 * {@code ImportForecastWindow} extends {@link RmaJDialog} to provide a common foundation
 * for dialogs that collect user input and produce forecast data (e.g.,
 * {@link CreateBcWindow}). It enforces a consistent cancellation contract across all
 * subclasses:
 *
 *   The {@code _canceled} flag is initialised to {@code true} and must be explicitly
 *       set to {@code false} by a subclass only after the user has successfully submitted the form.
 *   The default close operation is set to {@link #DO_NOTHING_ON_CLOSE} to prevent
 *       the system from disposing the dialog when the user clicks the window's close
 *       button. Instead, a {@link WindowAdapter} intercepts the closing event, sets
 *       {@code _canceled} to {@code true}, and hides the dialog so it can be reused.
 *
 * Concrete subclasses must implement {@link #isCanceled()} to expose the cancellation
 * state to callers, and typically also provide {@code fillForm} and {@code getBcData}
 * (or equivalent) methods for populating and retrieving data.
 *
 * @see CreateBcWindow
 * @see RmaJDialog
 */
public abstract class ImportForecastWindow extends RmaJDialog {
    // Cancellation flag; true by default so that closing without submitting is treated as cancelled.
    // Subclasses must set this to false explicitly upon a successful form submission.
    protected boolean _canceled = true;

    /**
     * Constructs an {@code ImportForecastWindow} with the given parent, title, and
     * modality, suppresses the default window-close dispose behaviour, and installs
     * a window listener that hides the dialog on close while preserving the cancelled
     * state.
     *
     * @param parent the {@link Window} over which this dialog is centred and to which
     *               it is optionally modal; passed to the {@link RmaJDialog} superclass
     * @param title  the text displayed in the dialog's title bar
     * @param modal  {@code true} to create a modal dialog that blocks the parent window;
     *               {@code false} for a non-blocking dialog
     */
    public ImportForecastWindow(Window parent, String title, boolean modal) {
        // Delegate title, modality, and parent window setup to the RmaJDialog superclass
        super(parent, title, modal);

        // Prevent the JVM from disposing this dialog when the user clicks the OS close button;
        // disposal would make the object unusable for subsequent re-show attempts
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Attach the window listener that treats OS-level close as a user cancellation
        addCloseListener();
    }

    /**
     * Registers a {@link WindowAdapter} that intercepts the window-closing event
     * triggered when the user clicks the dialog's OS-level close button.
     *
     * On close, the adapter sets {@code _canceled} to {@code true} and hides (rather
     * than disposes of) the dialog, ensuring the dialog instance remains reusable and
     * that the calling code can detect the cancellation via {@link #isCanceled()}.
     */
    private void addCloseListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Mark the dialog as cancelled when the user closes it via the OS button
                _canceled = true;

                // Hide rather than dispose so the dialog can be re-shown without rebuilding
                setVisible(false);
            }
        });
    }

    /**
     * Returns whether this dialog was closed without a successful form submission.
     *
     * Concrete subclasses implement this method to expose the {@code _canceled} flag.
     * The flag is {@code true} by default and should only be set to {@code false} by
     * a subclass after the user has clicked OK and all validation has passed.
     *
     * @return {@code true} if the dialog was cancelled or closed without submitting;
     * {@code false} if the user successfully submitted the form
     */
    public abstract boolean isCanceled();
}
