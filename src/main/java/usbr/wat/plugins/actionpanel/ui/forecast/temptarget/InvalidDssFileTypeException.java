package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

/**
 * Thrown when a file path is provided that does not have a ".dss" extension.
 * Used by DSS file validation logic to signal that the given file is not a
 * valid DSS file type before any read or write operations are attempted.
 * Package-private; intended for use only within the temptarget package.
 */
final class InvalidDssFileTypeException extends Exception {
    /**
     * Constructs an InvalidDssFileTypeException with a message identifying the
     * file that failed the DSS type check.
     *
     * @param fileName the path or name of the file that is not a DSS file
     */
    InvalidDssFileTypeException(String fileName) {
        // Build the exception message by appending the file name to a fixed description
        super(fileName + " is not a DSS file");
    }
}