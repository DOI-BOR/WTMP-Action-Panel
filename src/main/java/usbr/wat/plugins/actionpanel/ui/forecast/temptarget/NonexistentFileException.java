package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

/**
 * Checked exception thrown when a file path that is required for an operation
 * does not correspond to an existing file on the file system. Callers that
 * encounter this exception should inform the user that the specified file could
 * not be found and prompt them to verify the path or select a valid file.
 *
 * This class is package-private and final; it is not intended for use outside
 * the temperature target UI package or for subclassing.
 */
final class NonexistentFileException extends Exception {
    /**
     * Constructs a new NonexistentFileException with a detail message that
     * identifies the missing file by name or path.
     *
     * @param fileName the name or path of the file that could not be found;
     *                 included verbatim in the exception detail message
     */
    NonexistentFileException(String fileName) {
        // Compose a human-readable message that names the missing file
        super(fileName + " does not exist");
    }
}
