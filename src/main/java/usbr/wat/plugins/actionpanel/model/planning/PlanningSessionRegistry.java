package usbr.wat.plugins.actionpanel.model.planning;

import java.util.ArrayList;                 // Resizable-array implementation backing each in-memory registry list
import java.util.Collections;                // Provides unmodifiable view wrappers so callers cannot mutate the backing lists directly
import java.util.List;                       // Ordered collection interface used for the three registry lists

/**
 * Session-scoped, in-memory registry of the CalSim, Climate Scenario, and Hydrology
 * datasets a user has entered during the current WAT run.
 *
 * These entries are <b>not</b> persisted to disk as a standalone library. Per the
 * intended workflow, a user re-enters (or re-imports) CalSim/Climate/Hydrology data
 * each time WAT is run; this registry simply keeps whatever the user has already
 * created earlier in the same session available for re-selection in the "New Planning
 * Set" dialog's drop-downs, until it is explicitly removed or the session ends.
 *
 * A {@link PlanningSet} that references one of these entries stores the actual data
 * (see {@link CalSimData}, {@link ClimateScenario}, {@link HydrologyData}) directly
 * within its own saved XML, so the Set itself remains fully reloadable across
 * sessions even though this registry does not persist independently.
 *
 * This class is a simple static singleton; it is intentionally not thread-safe beyond
 * what Swing's single-threaded event dispatch model already guarantees, matching the
 * rest of this plugin's UI-thread-confined data classes.
 */
public final class PlanningSessionRegistry {

	// Singleton instance holding the three in-memory lists for the lifetime of the session
	private static final PlanningSessionRegistry INSTANCE = new PlanningSessionRegistry();

	// CalSim datasets created or imported so far this session
	private final List<CalSimData> _calSimData = new ArrayList<>();

	// Climate scenarios created so far this session
	private final List<ClimateScenario> _climateScenarios = new ArrayList<>();

	// Hydrology datasets created or imported so far this session
	private final List<HydrologyData> _hydrologyData = new ArrayList<>();

	// Private constructor enforces the singleton pattern
	private PlanningSessionRegistry() {
		// Intentionally empty; the three lists above are already initialized at declaration
	}

	/**
	 * Returns the single shared registry instance for the current WAT session.
	 *
	 * @return the session-lifetime registry instance
	 */
	public static PlanningSessionRegistry getInstance() {
		return INSTANCE; // Always return the same static instance
	}

	// --- CalSim data ---

	/**
	 * Adds a CalSim dataset to the session registry, or replaces an existing entry with
	 * the same name so edits are reflected immediately in the "existing entries" list.
	 *
	 * @param data the CalSim dataset to register; ignored if null
	 */
	public void addCalSimData(CalSimData data) {
		if (data == null) {
			return; // Nothing to register
		}
		removeCalSimDataByName(data.getName()); // Drop any prior entry with the same name first
		_calSimData.add(data); // Add the (possibly edited) entry back in
	}

	/**
	 * Removes a CalSim dataset from the session registry by name, if present.
	 *
	 * @param name the name of the entry to remove
	 */
	public void removeCalSimDataByName(String name) {
		// Remove every entry whose name matches; there should be at most one, by construction
		_calSimData.removeIf(d -> d.getName() != null && d.getName().equals(name));
	}

	/**
	 * Returns an unmodifiable view of all CalSim datasets entered so far this session.
	 *
	 * @return the current CalSim dataset entries, in insertion order
	 */
	public List<CalSimData> getCalSimData() {
		return Collections.unmodifiableList(_calSimData); // Prevent external code from mutating the backing list directly
	}

	// --- Climate scenarios ---

	/**
	 * Adds a climate scenario to the session registry, or replaces an existing entry
	 * with the same name.
	 *
	 * @param scenario the climate scenario to register; ignored if null
	 */
	public void addClimateScenario(ClimateScenario scenario) {
		if (scenario == null) {
			return; // Nothing to register
		}
		removeClimateScenarioByName(scenario.getName()); // Drop any prior entry with the same name first
		_climateScenarios.add(scenario); // Add the (possibly edited) entry back in
	}

	/**
	 * Removes a climate scenario from the session registry by name, if present.
	 *
	 * @param name the name of the entry to remove
	 */
	public void removeClimateScenarioByName(String name) {
		// Remove every entry whose name matches; there should be at most one, by construction
		_climateScenarios.removeIf(c -> c.getName() != null && c.getName().equals(name));
	}

	/**
	 * Returns an unmodifiable view of all climate scenarios entered so far this session.
	 *
	 * @return the current climate scenario entries, in insertion order
	 */
	public List<ClimateScenario> getClimateScenarios() {
		return Collections.unmodifiableList(_climateScenarios); // Prevent external code from mutating the backing list directly
	}

	// --- Hydrology data ---

	/**
	 * Adds a hydrology dataset to the session registry, or replaces an existing entry
	 * with the same name.
	 *
	 * @param data the hydrology dataset to register; ignored if null
	 */
	public void addHydrologyData(HydrologyData data) {
		if (data == null) {
			return; // Nothing to register
		}
		removeHydrologyDataByName(data.getName()); // Drop any prior entry with the same name first
		_hydrologyData.add(data); // Add the (possibly edited) entry back in
	}

	/**
	 * Removes a hydrology dataset from the session registry by name, if present.
	 *
	 * @param name the name of the entry to remove
	 */
	public void removeHydrologyDataByName(String name) {
		// Remove every entry whose name matches; there should be at most one, by construction
		_hydrologyData.removeIf(d -> d.getName() != null && d.getName().equals(name));
	}

	/**
	 * Returns an unmodifiable view of all hydrology datasets entered so far this session.
	 *
	 * @return the current hydrology dataset entries, in insertion order
	 */
	public List<HydrologyData> getHydrologyData() {
		return Collections.unmodifiableList(_hydrologyData); // Prevent external code from mutating the backing list directly
	}

	/**
	 * Clears all session entries. Intended for use when a new WAT run/session begins,
	 * or by tests that need a clean registry state.
	 */
	public void clear() {
		_calSimData.clear();       // Drop every registered CalSim dataset
		_climateScenarios.clear(); // Drop every registered climate scenario
		_hydrologyData.clear();    // Drop every registered hydrology dataset
	}
}
