package usbr.wat.plugins.actionpanel.model;

import java.util.ArrayList;          // Resizable-array List used as the backing store for registered plugins
import java.util.Collections;        // Utility class; used to wrap the plugin list in an unmodifiable view
import java.util.List;               // Ordered collection interface for the registered plugin list

/**
 * Static registry for ReportPlugin implementations within the WTMP Action Panel.
 *
 * Plugins are registered via register() at startup and retrieved via getPlugins()
 * to populate the report selection UI. The class is not instantiable; all access
 * is through static methods.
 *
 * Duplicate registrations are silently ignored (based on object identity). The
 * list returned by getPlugins() is an unmodifiable snapshot to prevent external
 * modification of the registry.
 *
 */
public class ReportsManager {
	// Static list of registered ReportPlugin instances; persists for the lifetime of the JVM
	private static List<ReportPlugin> _plugins = new ArrayList<>();

	/**
	 * Private constructor preventing instantiation of this static utility class.
	 */
	private ReportsManager() {
		super();
	}

	/**
	 * Registers a ReportPlugin with the manager.
	 *
	 * Has no effect if plugin is null or if the same plugin instance has already
	 * been registered (checked by object identity via List.contains()).
	 *
	 * @param plugin the ReportPlugin to register; ignored if null or already registered
	 */
	public static void register(ReportPlugin plugin) {
		// Ignore null registrations
		if (plugin == null) {
			return;
		}

		// Ignore duplicate registrations (same object reference)
		if (_plugins.contains(plugin)) {
			return;
		}

		_plugins.add(plugin);
	}

	/**
	 * Returns an unmodifiable snapshot of the currently registered ReportPlugin list.
	 *
	 * The returned list is a defensive copy wrapped in an unmodifiable view, so callers
	 * cannot modify the registry by adding or removing elements from it.
	 *
	 * @return an unmodifiable List of all registered ReportPlugin instances
	 */
	public static List<ReportPlugin> getPlugins() {
		return Collections.unmodifiableList(new ArrayList<>(_plugins));
	}
}
