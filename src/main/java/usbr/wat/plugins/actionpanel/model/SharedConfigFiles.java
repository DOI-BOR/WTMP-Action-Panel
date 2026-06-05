package usbr.wat.plugins.actionpanel.model;

import com.rma.model.Project; // Represents the currently loaded RMA project; used to resolve the absolute file path

import java.nio.file.Path;   // NIO immutable file system path for the config file location
import java.nio.file.Paths;  // Factory for constructing Path instances from string segments

/**
 * Utility class providing access to shared configuration file paths used by the
 * WTMP Action Panel.
 *
 * All paths are relative to the project directory unless explicitly resolved to
 * absolute paths. The default paths can be overridden at JVM startup via system
 * properties, allowing deployment-specific configuration without code changes.
 *
 * Currently exposes one config file:
 *   - River Locations File: defines downstream control point locations for the study.
 *     Default path: shared/config/downstream_control_pts.config
 *     Override property: WTMP.riverLocationsFile
 *
 * This class is final and not instantiable (enforced by a throwing private constructor).
 */
public final class SharedConfigFiles {
    // Base relative directory for all shared configuration files
    private static final Path BASE_FOLDER = Paths.get("shared/config");

    // Default file name for the river locations configuration file
    private static final String DEFAULT_RIVER_LOCATIONS_FILE = "downstream_control_pts.config";

    // System property key for overriding the river locations file path
    private static final String RIVER_LOCATIONS_FILE_PROPERTY = "WTMP.riverLocationsFile";

    /**
     * Private constructor that prevents instantiation of this utility class.
     *
     * Throws AssertionError if called reflectively.
     */
    private SharedConfigFiles() {
        throw new AssertionError("Utility class. Don't instantiate");
    }

    /**
     * Returns the project-relative path to the river locations configuration file.
     *
     * Checks the WTMP.riverLocationsFile system property first; if not set, uses
     * the default path: shared/config/downstream_control_pts.config.
     *
     * @return the project-relative Path to the river locations file
     */
    public static Path getRelativeRiverLocationsFile() {
        // Use the system property override if provided; otherwise use the default relative path
        String file = System.getProperty(RIVER_LOCATIONS_FILE_PROPERTY,
                BASE_FOLDER.resolve(DEFAULT_RIVER_LOCATIONS_FILE).toString());
        return Paths.get(file);
    }

    /**
     * Returns the absolute path to the river locations configuration file by resolving
     * the relative path against the current project directory.
     *
     * @return the absolute Path to the river locations file
     */
    public static Path getRiverLocationsFile() {
        // Resolve the relative path to an absolute path using the current project directory
        String absFile = Project.getCurrentProject().getAbsolutePath(getRelativeRiverLocationsFile().toString());
        return Paths.get(absFile);
    }
}
