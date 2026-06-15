package usbr.wat.plugins.actionpanel.model.forecast;

// Provides DSSPathname for reading and constructing HEC-DSS A/B/C/D/E/F path components
import hec.heclib.dss.DSSPathname;

// Provides DSSIdentifier for pairing a DSS file path with a record pathname during lookups
import hec.io.DSSIdentifier;

// Provides WatSimulation for accessing the current WAT simulation context and its file paths
import hec2.wat.model.WatSimulation;

// Provides the List interface for ordered collections of DSS identifiers and path map items
import java.util.List;

/**
 * A specialised {@link DssPathMap} that builds its source-to-destination DSS path
 * mappings from the pathnames stored in a {@link TemperatureTargetSet} rather than
 * reading them verbatim from the configuration file.
 *
 * When {@link #readDssPathsFile()} is called, the base-class implementation reads the
 * raw configuration file to obtain the destination DSS file and path template for the
 * first mapping entry. This class then discards all raw entries and rebuilds the map so
 * that each {@link DSSPathname} in the associated {@link TemperatureTargetSet} becomes a
 * separate source entry that fans out to the same set of destinations defined in the
 * original first mapping.
 *
 * This class is declared {@code final} to prevent subclassing.
 *
 * @see DssPathMap
 * @see TemperatureTargetSet
 * @see DssPathMapItem
 */
public final class TempTargetDssPathMap extends DssPathMap {
    // The temperature target set whose DSS pathnames are used as the source entries
    // in the rebuilt path map
    private final TemperatureTargetSet _tempTargetSet;

    /**
     * Constructs a {@code TempTargetDssPathMap} for the given WAT simulation, configuration
     * file, and temperature target set.
     *
     * @param sim                  the current {@link WatSimulation} context; used by the
     *                             base class to resolve relative file paths
     * @param configFile           the path to the DSS path-map configuration file read by
     *                             {@link #readDssPathsFile()}
     * @param temperatureTargetSet the {@link TemperatureTargetSet} whose stored DSS pathnames
     *                             are expanded into individual source map entries; must not
     *                             be {@code null}
     */
    public TempTargetDssPathMap(WatSimulation sim, String configFile, TemperatureTargetSet temperatureTargetSet) {
        // Delegate file path and simulation context initialisation to the base class
        super(sim, configFile);

        // Store the temperature target set for use when rebuilding the path map
        _tempTargetSet = temperatureTargetSet;
    }

    /**
     * Returns the list of destination {@link DSSIdentifier} objects that correspond to
     * the given source DSS path string and time step.
     *
     * Converts the {@link TemperatureTargetTimeStep} to its DSS E-part string and
     * delegates to the base-class overload that accepts a plain string time step.
     *
     * @param srcDssPath the full source DSS pathname string (A/B/C/D/E/F) to look up
     * @param timeStep   the {@link TemperatureTargetTimeStep} whose string representation
     *                   is used to match or filter destination entries
     * @return a {@link List} of {@link DSSIdentifier} objects representing the mapped
     * destination records; may be empty if no matching mapping exists
     */
    public List<DSSIdentifier> getDestDssIdentifiersFor(String srcDssPath, TemperatureTargetTimeStep timeStep) {
        // Convert the enum constant to its DSS E-part string before delegating
        return getDestDssIdentifiersFor(srcDssPath, timeStep.toString());
    }

    /**
     * Reads the DSS path-map configuration file via the base-class implementation and
     * then rebuilds the internal mapping list so that each pathname in the associated
     * {@link TemperatureTargetSet} becomes an individual source entry.
     *
     * The rebuild process:
     *
     *   Calls the base-class {@code readDssPathsFile()} to populate the raw map list
     *       from the configuration file.
     *   If the read succeeded and at least one raw mapping exists, captures the
     *       destination count and destination file/path values from the first raw entry
     *       (only the first mapping is used for temperature targets).
     *   Clears all raw entries from the internal list.
     *   Iterates over every {@link DSSPathname} in the temperature target set
     *       (using the hourly E-part as a canonical form) and creates a new
     *       {@link DssPathMapItem} for each, fanning out to all destinations defined
     *       in the original first mapping.
     *
     * @return {@code true} if the base-class file read succeeded; {@code false} otherwise
     */
    @Override
    public boolean readDssPathsFile() {
        // Perform the base-class read to populate the raw configuration entries
        boolean retVal = super.readDssPathsFile();

        if (retVal && !_dssPathMapList.isEmpty()) {
            // Capture the first mapping entry to extract the destination template;
            // only the first mapping in the config file is used for temperature targets
            DssPathMapItem mapping = _dssPathMapList.get(0);

            // Record how many destination entries are defined in the template mapping
            int numberOfDests = mapping.getNumberOfDests();

            // Discard all raw entries; the list will be rebuilt from the target set pathnames
            _dssPathMapList.clear();

            // Retrieve all DSS pathnames from the target set, using the hourly E-part as
            // the canonical interval for building the map entries
            List<DSSPathname> dssPathNames = _tempTargetSet.getDssPathNames(TemperatureTargetTimeStep.REGULAR_HOURLY);

            for (DSSPathname pathName : dssPathNames) {
                // Create a new map item using the target set's source file and F-part label
                DssPathMapItem item = new DssPathMapItem(_tempTargetSet.getDssSourcePath().toString(), _tempTargetSet.getFPartWithoutCollection());

                // Fan out to each destination defined in the original template mapping
                for (int i = 0; i < numberOfDests; i++) {
                    String destFile = mapping.getDestDssFile(i);
                    String destPath = mapping.getDestDssPath(i);

                    // Assign the current target pathname as the source for this map item
                    item.setSourceDssPath(pathName.getPathname());

                    // Register the destination file and path for this fan-out entry
                    item.addMapping(destFile, destPath);
                }

                // Add the fully configured map item to the rebuilt list
                _dssPathMapList.add(item);
            }
        }

        return retVal;
    }
}
