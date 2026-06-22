package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

/**
 * Checked exception thrown when a temperature target set fails to save its
 * time-series data to a HEC-DSS file. Two constructors are provided:
 *
 * The three-argument form is used when a DSS write operation returns a non-zero
 * status code; it composes a detail message identifying the failing DSS record
 * pathname, the target file, and the numeric error status.
 *
 * The single-argument form is used for higher-level failures (e.g. a missing
 * analysis period) where a plain descriptive message is sufficient.
 *
 * Callers that catch this exception should display its message to the user and
 * log it at an appropriate severity level.
 *
 * @see TempTargetPanel
 * @see TempTargetConsumer
 */
public final class TempTargetSaveFailedException extends Exception {
    /**
     * Constructs a TempTargetSaveFailedException with a detail message that
     * identifies the failing DSS record, the target file, and the DSS error
     * status code returned by the write operation.
     *
     * @param error      the DSS record pathname (fullName) that could not be written
     * @param fileName   the absolute path of the DSS file that was being written to
     * @param statusCode the non-zero integer status code returned by the DSS writer,
     *                   as a string
     */
    public TempTargetSaveFailedException(String error, String fileName, String statusCode) {
        // Compose a multi-line message naming the record, file, and error code
        super("Error writing " + error + "\n to " + fileName + "\n Error Status: " + statusCode);
    }

    /**
     * Constructs a TempTargetSaveFailedException with a plain descriptive message.
     * Used for high-level failures such as a missing or unset analysis period where
     * no DSS record pathname or status code is available.
     *
     * @param error a human-readable description of the failure condition
     */
    public TempTargetSaveFailedException(String error) {
        // Pass the error description directly as the exception detail message
        super(error);
    }
}
