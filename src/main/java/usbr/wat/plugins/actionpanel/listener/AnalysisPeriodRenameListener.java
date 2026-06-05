package usbr.wat.plugins.actionpanel.listener;

import java.util.ArrayList;           // Resizable-array implementation of List, used for local AP tracking
import java.util.List;                 // Generic ordered collection interface
import java.util.Observable;           // Legacy observable subject; WatAnalysisPeriod extends this
import java.util.Observer;             // Callback interface invoked when an Observable fires a change

import com.rma.event.ProjectManagerListener; // RMA interface for reacting to manager add/delete events on a Project
import com.rma.model.Manager;                // Base interface for RMA managed model objects
import com.rma.model.ManagerProxy;           // Wrapper carrying a Manager reference delivered in listener callbacks
import com.rma.model.Project;                // Represents the currently open WAT study/project

import hec.lang.NamedType;                   // Provides named-change event constants (e.g. NAME_CHANGED, RENAME_EVENT)
import hec.model.AbstractSimulation;         // Base class for HEC simulation model objects (unused directly; kept for context)

import hec2.wat.model.WatAnalysisPeriod;     // WAT analysis period model object; the subject being observed for renames

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup; // Base class for all simulation groups holding an analysis period reference
import usbr.wat.plugins.actionpanel.model.SimulationGroup;         // Standard (non-forecast) simulation group implementation
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup; // Forecast-specific simulation group implementation


/**
 * Listens for analysis period rename events and propagates name changes to all
 * simulation groups that reference the renamed period.
 *
 * This class plays two complementary roles:
 *
 *   As an {@link Observer} it receives property-change notifications fired by
 *       individual {@link WatAnalysisPeriod} objects whenever their name changes.
 *   As a {@link ProjectManagerListener} it is notified when analysis periods are
 *       added to or removed from the project, allowing the listener to attach or detach
 *       itself from newly registered periods dynamically.
 *
 *
 * A parallel name cache ({@code _localApList} / {@code _localApNameList}) is maintained
 * to handle the case where a simulation group holds a stale name string rather than a
 * live object reference to its analysis period. When a rename is detected, both
 * {@link SimulationGroup} and {@link ForecastSimGroup} instances are updated so they
 * reflect the current period name and are marked as modified.
 *
 * Call {@link #stopListening()} when the listener is no longer needed to prevent memory
 * leaks from lingering observer registrations.
 */
public class AnalysisPeriodRenameListener
		implements Observer, ProjectManagerListener {
	/**
	 * The project whose analysis periods and simulation groups are being monitored.
	 */
	private final Project _prj;

	/**
	 * Local cache of all {@link WatAnalysisPeriod} objects currently observed.
	 * Kept in sync with {@code _localApNameList} so that index lookups map correctly.
	 */
	private List<WatAnalysisPeriod> _localApList = new ArrayList<>();

	/**
	 * Parallel cache of analysis period names, indexed identically to {@code _localApList}.
	 * Used to match simulation groups that store a period name string rather than a direct
	 * object reference.
	 */
	private List<String> _localApNameList = new ArrayList<>();


	/**
	 * Constructs the listener, registers it with the project manager, and attaches it
	 * as an observer to every analysis period already present in the project.
	 *
	 * @param prj the active WAT project whose analysis periods should be monitored
	 */
	public AnalysisPeriodRenameListener(Project prj) {
		super();

		// Store the project reference for use in listener callbacks
		_prj = prj;

		// Register this instance to receive manager add/delete notifications from the project
		addProjectListener();

		// Attach as an observer to all analysis periods that already exist in the project
		addRenameListeners();
	}


	/**
	 * Registers this listener with the project so it receives {@link #managerAdded} and
	 * {@link #managerDeleted} callbacks for any {@link WatAnalysisPeriod} changes.
	 */
	private void addProjectListener() {
		_prj.addManagerListener(this);
	}


	/**
	 * Iterates over all analysis periods currently in the project and registers this
	 * listener as an observer on each one.
	 */
	private void addRenameListeners() {
		// Retrieve the complete list of analysis periods registered with the project
		List<WatAnalysisPeriod> apList = _prj.getManagerListForType(WatAnalysisPeriod.class);

		WatAnalysisPeriod ap;

		// Attach an observer and cache each analysis period
		for (int i = 0; i < apList.size(); i++) {
			ap = apList.get(i);
			addListenerToAp(ap);
		}
	}


	/**
	 * Handles change notifications fired by observed {@link WatAnalysisPeriod} objects.
	 *
	 * When the observable is a {@link WatAnalysisPeriod} and the argument signals a
	 * name change ({@link NamedType#NAME_CHANGED} or {@link NamedType#RENAME_EVENT}),
	 * both {@link ForecastSimGroup} and {@link SimulationGroup} collections are scanned
	 * and updated to reflect the new period name.
	 *
	 * @param o   the observable that fired the notification; expected to be a
	 *            {@link WatAnalysisPeriod}
	 * @param arg the event argument; rename events use {@link NamedType} string constants
	 */
	@Override
	public void update(Observable o, Object arg) {
		// Only handle notifications from WatAnalysisPeriod objects
		if (o instanceof WatAnalysisPeriod) {
			WatAnalysisPeriod ap = (WatAnalysisPeriod) o;

			// Act only when the notification represents a name-change event
			if (NamedType.NAME_CHANGED.equals(arg) || NamedType.RENAME_EVENT.equals(arg)) {
				// Update all forecast simulation groups that reference the renamed period
				List<ForecastSimGroup> simGroups = _prj.getManagerListForType(ForecastSimGroup.class);
				updateSimGroups(simGroups, ap);

				// Update all standard simulation groups that reference the renamed period
				List<SimulationGroup> simGroups2 = _prj.getManagerListForType(SimulationGroup.class);
				updateSimGroups(simGroups2, ap);
			}
		}
	}


	/**
	 * Propagates a rename of {@code renamedAp} to every simulation group in the supplied list.
	 *
	 * Two resolution strategies are used:
	 *
	 *   If the group has no live period reference (null), its stored period name is looked up
	 *       in the local name cache to find the matching period object, which is then assigned.
	 *   If the group already holds a direct reference to {@code renamedAp}, the reference is
	 *       refreshed and the group is marked modified so the change is persisted.
	 * >
	 * After processing, the local name cache entry for the renamed period is updated to the
	 * new name.
	 *
	 * @param simGroups raw list of simulation groups ({@link AbstractSimulationGroup} subtypes)
	 *                  to inspect and potentially update
	 * @param renamedAp the analysis period whose name has just changed
	 */
	private void updateSimGroups(List simGroups, WatAnalysisPeriod renamedAp) {
		AbstractSimulationGroup simGroup;
		WatAnalysisPeriod ap;

		// Tracks the cache index of the renamed period; -1 means not yet found
		int idx = -1;

		// Tracks whether at least one group was matched and updated via the name cache
		boolean found = false;

		for (int i = 0; i < simGroups.size(); i++) {
			simGroup = (AbstractSimulationGroup) simGroups.get(i);
			ap = simGroup.getAnalysisPeriod();

			if (ap == null) {
				// The group holds only a name string; attempt to resolve it via the local cache
				String apName = simGroup.getAnalysisPeriodName();
				idx = _localApNameList.indexOf(apName);

				if (idx > -1) {
					// Retrieve the cached period object matching the stored name
					ap = _localApList.get(idx);

					// Only reassign if the resolved period is not the one that was just renamed
					// (avoids a redundant update when the name was already current)
					if (ap != renamedAp) {
						simGroup.setAnalysisPeriod(ap);
						simGroup.setModified(true);
						found = true;
					}
				}
			} else if (ap == renamedAp) {
				// The group already holds a direct reference to the renamed period;
				// refresh the reference and mark the group as modified
				simGroup.setAnalysisPeriod(ap);
				simGroup.setModified(true);
			}
		}

		// Update the cached name to the new value so future lookups remain accurate
		if (idx > -1 && found) {
			_localApNameList.set(idx, renamedAp.getName());
		}
	}


	/**
	 * Called by the project when a new manager object is added.
	 *
	 * If the added manager is a {@link WatAnalysisPeriod} not already tracked, this
	 * listener is registered as an observer on it so future renames are detected.
	 *
	 * @param managerProxy proxy wrapping the newly added manager object
	 */
	@Override
	public void managerAdded(ManagerProxy managerProxy) {
		Manager mgr = managerProxy.getManager();

		// Only act on newly added analysis periods
		if (mgr instanceof WatAnalysisPeriod) {
			WatAnalysisPeriod ap = (WatAnalysisPeriod) mgr;

			// Avoid registering duplicate observers for an already-tracked period
			if (!_localApList.contains(ap)) {
				addListenerToAp(ap);
			}
		}
	}


	/**
	 * Registers this instance as an observer on the given analysis period and adds it to
	 * the local tracking caches.
	 *
	 * @param ap the analysis period to begin observing
	 */
	private void addListenerToAp(WatAnalysisPeriod ap) {
		// Subscribe to property-change notifications from this analysis period
		ap.addObserver(this);

		// Add the period object and its current name to the parallel cache lists
		_localApList.add(ap);
		_localApNameList.add(ap.getName());
	}


	/**
	 * Called by the project when a manager object is removed.
	 *
	 * Deregisters this listener from the deleted manager and removes it from the local
	 * tracking caches to avoid stale references.
	 *
	 * @param managerProxy proxy wrapping the manager object being removed
	 */
	@Override
	public void managerDeleted(ManagerProxy managerProxy) {
		Manager mgr = managerProxy.getManager();

		// Deregister this observer from the deleted manager to avoid lingering callbacks
		mgr.deleteObserver(this);

		// Remove the manager from both parallel cache lists using its index
		int idx = _localApList.indexOf(mgr);
		if (idx > -1) {
			_localApList.remove(idx);
			_localApNameList.remove(idx);
		}
	}


	/**
	 * Returns the class type this listener is registered to handle within the project manager.
	 *
	 * The project uses this to filter manager add/delete callbacks so that only
	 * {@link WatAnalysisPeriod} events are delivered to this listener.
	 *
	 * @return {@code WatAnalysisPeriod.class}
	 */
	@Override
	public Class getManagerClass() {
		return WatAnalysisPeriod.class;
	}


	/**
	 * Deregisters this listener from all observed analysis periods and from the project,
	 * then clears the local caches.
	 *
	 * This method should be called when the listener is no longer needed to prevent
	 * memory leaks caused by lingering observer registrations holding references to
	 * this object.
	 */
	public void stopListening() {
		WatAnalysisPeriod ap;

		// Remove this observer from every cached analysis period
		for (int i = 0; i < _localApList.size(); i++) {
			ap = _localApList.get(i);
			ap.deleteObserver(this);
		}

		// Deregister from the project's manager listener list
		Project.getCurrentProject().removeManagerListener(this);

		// Clear both caches to release all held object references
		_localApList.clear();
		_localApNameList.clear();
	}
}
