package usbr.wat.plugins.actionpanel.io;

import java.io.File; // Import File class for handling file system operations and path manipulations
import java.io.IOException; // Import IOException for handling errors during file copy/restore operations
import java.util.HashMap; // Import HashMap utility to map source files to their saved locations
import java.util.Iterator; // Import Iterator for traversing collections of saved file entries
import java.util.List; // Import List interface for maintaining ordered collections of file paths
import java.util.Map; // Import Map interface for key-value storage of original to saved file mappings
import java.util.Map.Entry; // Import Entry class for accessing key-value pairs in the saved files map
import java.util.Set; // Import Set for managing unique entries from the saved files collection
import java.util.logging.Logger; // Import Logger utility for logging informational and error messages

import org.python.google.common.io.Files; // Import Files utility for copying and moving file system operations

import com.rma.event.ProjectAdapter; // Import ProjectAdapter event listener to handle project lifecycle events
import com.rma.event.ProjectEvent; // Import ProjectEvent event object passed when project events occur
import com.rma.model.Project; // Import Project model for accessing project directory and path information

import rma.util.RMAIO; // Import RMA utility class for handling file paths and directory operations

/**
 * ModelFileStorage is a utility class that manages saving and restoring of model files during simulation runs.
 * It provides functionality to automatically save files to a temporary storage location during analysis,
 * and restore them afterward while maintaining their original directory structure relative to the project root.
 *
 * The class maintains a static map of file mappings between original paths and saved locations.</p>
 */

public class ModelFileStorage {
	// Storage folder name for temporary model file storage
	private static final String MODEL_SAVE_FOLDER = "savedModelFiles"; // Folder name where saved model files will be stored

	// Class-level field storing the base path for saving all temporary files
	private static String _basePath; // Base directory path where all saved model files are stored

	// Map tracking original file paths to their corresponding saved locations
	private static Map<File, File> _savedFiles = new HashMap<>(); // Static map of File objects keyed by original file, value is saved file location

	static {
		// Add project adapter listener to handle project load event
		Project.addStaticProjectListener(new ProjectAdapter() {
			@Override
			public void projectLoaded(ProjectEvent e) {
				initialize(e.getProject().getProjectDirectory()); // Initialize storage with current project directory path
			}

		});
	}

	// Private constructor prevents instantiation of storage singleton
	private ModelFileStorage() { }

	/**
	 * Initializes the model file storage by setting the base path for saved files.
	 * Clears any existing saved file mappings to ensure clean state on project load.
	 *
	 * @param projectDirectory Path to the project directory where files will be saved
	 */
	protected static void initialize(String projectDirectory) {
		// Set base storage path by concatenating project directory with save folder name
		_basePath = RMAIO.concatPath(projectDirectory, MODEL_SAVE_FOLDER);

		// Clear any existing file mappings before reinitialization
		_savedFiles.clear();
	}

	/**
	 * Saves a single file to the saved files storage location.
	 * Converts relative paths to absolute paths based on the current project context.
	 * Uses the built-in Files utility to copy the file while preserving directory structure.
	 *
	 * @param file Path string representing the file to be saved (can be relative or absolute)
	 * @return True if file was successfully saved, false otherwise
	 */
	public static boolean saveFile(String file) {
		// Return false if input file path is null
		if (file == null) {
			return false;
		}

		// Use provided file path as absolute initially
		String absFile = file;

		// Check if the provided path is not already an absolute full path
		if (!RMAIO.isFullPath(file)) {
			// Convert relative path to absolute using project context
			absFile = Project.getCurrentProject().getAbsolutePath(file);
		}

		// Create File object from absolute file path
		File srcFile = new File(absFile);

		// Delegate to overloaded method accepting File object
		return saveFile(srcFile);
	}

	// Method for saving multiple files listed in a collection
	public static boolean saveFiles(List<String> files) {
		// Return false if input list is null
		if (files == null) {
			return false;
		}

		// Initialize return value flag to track success
		boolean rv = true;

		// Iterate through each file path in the list
		for (int i = 0; i < files.size(); i++) {
			// Call individual save method and AND with current result
			rv &= saveFile(files.get(i));
		}

		// Return combined result indicating all files were successfully saved
		return rv;
	}

	// Primary implementation for saving a single file from File object
	public static boolean saveFile(File srcFile) {
		File destPath = buildDestPath(srcFile); // Create destination path for the file being saved

		// Attempt to copy file using Files utility
		try {
			// Copy source file to destination path while preserving directory structure
			Files.copy(srcFile, destPath);

			// Store mapping of original file to its saved location in static map
			_savedFiles.put(srcFile, destPath);

			// Return success indication
			return true;

		} catch (IOException ioe) {
			// Log detailed error message with both paths and exception details
			Logger.getLogger("ModelFileStorage").info("Failed to copy file " + srcFile.getAbsolutePath() + " to " + destPath.getAbsolutePath() + " error:" + ioe);

			// Return failure indication
			return false;
		}
	}

	/**
	 * Builds the destination path for a file being saved based on its relative location.
	 * Calculates the relative path from project directory to source file, then appends to base storage path.
	 * Creates parent directories if they do not exist using mkdirs() to ensure nested structure is created.
	 *
	 * @param srcFile Source file object representing the original file to be saved
	 * @return File object representing the destination path in storage (or null if creation failed)
	 */
	private static File buildDestPath(File srcFile) {
		// Get project root directory for relative calculation
		String prjDir = Project.getCurrentProject().getProjectDirectory();

		// Calculate relative path from project dir to source file
		String relDir = RMAIO.getRelativePath(prjDir, srcFile.getAbsolutePath());

		// Combine base storage path with relative directory structure
		String destPath = RMAIO.concatPath(_basePath, relDir);

		// Extract directory portion of destination path without filename
		String destFolder = RMAIO.getDirectoryFromPath(destPath);

		// Create File object for the destination directory
		File f = new File(destFolder);

		// Check if directory was created successfully (returns true on success)
		if (f.mkdirs()) {
			// Return complete destination file path as result
			return new File(destPath);
		}

		// Return null if directory creation failed (already existed or permission denied)
		return null;
	}


	/**
	 * Restores all saved files back to their original locations.
	 * Iterates through the stored file mappings and copies each saved file back to its source location.
	 * Removes successfully restored entries from the tracking map to avoid duplicate operations.
	 * Logs a warning message if any files fail to restore.
	 *
	 * @return True if all files were successfully restored, false if any failed
	 */
	public static boolean restoreAllFiles() {
		// Check if no files are waiting to be restored
		if (_savedFiles.isEmpty()) {
			// TODO: Implement logic to rebuild mapping from file system cache
			rebuildSavedFilesMap();
		}

		Set<Entry<File, File>> entrySet = _savedFiles.entrySet(); // Get set of all entries from saved files map
		Iterator<Entry<File, File>> iter = entrySet.iterator(); // Create iterator for sequential traversal of entries

		File origFile, savedFile; // Declare variables to hold original and saved file objects for iteration
		Entry<File, File> entry; // Declare variable for current entry being processed
		boolean rv = true, brv;

		// Loop through all saved file entries
		while (iter.hasNext()) {
			entry = iter.next(); // Get next entry from iterator

			origFile = entry.getKey(); // Extract original source file from entry
			savedFile = entry.getValue(); // Extract saved destination file from entry

			// Attempt to restore individual file pair
			if (restoreFile(origFile, savedFile)) {
				_savedFiles.remove(origFile); // Remove successfully restored entry from tracking map
				rv &= true; // Maintain success flag

			} else {
				rv &= false; // Fail if any restoration fails (AND with current value)
			}
		}

		// Check if there are still entries remaining in map after loop
		if (_savedFiles.size() > 0) {
			Logger.getLogger("ModelFileStorage").info("Failed to restore " + _savedFiles.size() + " files"); // Log warning about failed restoration count
		}

		return rv;
	}


	/**
	 * Restores a single file from the saved storage back to its original location.
	 * Uses the stored mapping to locate the saved file and copies it back to original path.
	 * Returns false if the requested file was never saved or restoration fails.
	 *
	 * @param fileToRestore Path string of the file to restore (can be relative or absolute)
	 * @return True if the file was successfully restored, false otherwise
	 */
	public static boolean restoreFile(String fileToRestore) {
		// Return false if input path is null
		if (fileToRestore == null) {
			return false;
		}

		// Use provided path as absolute initially
		String absFileToRestore = fileToRestore;

		// Convert relative to absolute using project context
		if (!RMAIO.isFullPath(fileToRestore)) {
			// Get full absolute path from project root
			absFileToRestore = Project.getCurrentProject().getAbsolutePath(fileToRestore);
		}
		// Create File object for requested restoration
		File requestedFile = new File(absFileToRestore);

		// Look up saved location in the storage map using requested file key
		File savedFile = _savedFiles.get(requestedFile);

		// If a saved file mapping exists for this request
		if (savedFile != null) {
			// Delegate to overloaded method with File objects
			return restoreFile(requestedFile, savedFile);
		}
		// Return failure if no saved location was found for this file
		return false;
	}

	/**
	 * Rebuilds the saved files map by scanning the file system for previously saved files.
	 * TODO: Implement logic to detect and restore cached information about saved locations.
	 */
	private static void rebuildSavedFilesMap() {
		// TODO Auto-generated method stub
		// Print placeholder message indicating unimplemented feature
		System.out.println("rebuildSavedFilesMap TODO implement me");
	}
}