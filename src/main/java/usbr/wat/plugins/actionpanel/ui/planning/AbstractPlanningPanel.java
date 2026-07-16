package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagLayout;               // Layout manager used by every concrete sub-tab panel

import javax.swing.ListModel;                // Model type returned by getSummaryListModel() for the category summary strip

import rma.swing.EnabledJPanel;              // RMA JPanel subclass with built-in enabled/disabled visual state support, matching the forecast sub-tab panels' base class

import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup; // The paired Simulation Group whose WatSimulations/settings this panel may also need
import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;    // The Set whose data this panel displays and edits

/**
 * Abstract base class for the six sub-tab panels hosted by the Planning tab's left-hand
 * {@code JTabbedPane} (Initial Conditions, Operations, Meteorology, Boundary Conditions,
 * Temperature Targets, and Simulation), mirroring the role
 * {@code usbr.wat.plugins.actionpanel.ui.forecast.AbstractForecastPanel} plays for the
 * Forecast Conditions workflow's sub-tabs.
 *
 * Each concrete subclass is responsible for its own detail content; this base class
 * standardizes:
 * <ul>
 *   <li>{@link #setPlanningSet(PlanningSet)} — called whenever the active Set changes
 *       (including to null, when no Set is selected), so every sub-tab panel stays in
 *       sync with the Set/Simulation Group selectors at the top of the Planning tab.</li>
 *   <li>{@link #panelActivated()} — called when this panel's tab becomes the selected tab,
 *       giving subclasses a hook to refresh any content that may have gone stale while a
 *       different tab was active.</li>
 *   <li>{@link #getTabName()} — the label shown both on the left-hand tab and (by
 *       {@code PlanningPanel}) as the corresponding box in the category summary strip.</li>
 * </ul>
 */
@SuppressWarnings("serial") // Swing components are not meaningfully serializable across versions; suppress the warning
public abstract class AbstractPlanningPanel extends EnabledJPanel {
	// Six concrete subclasses exist: InitialConditionsPanel, OperationsPanel, MeteorologyPanel,
	// BcPanel, TempTargetPanel, and SimulationPanel — one per left-hand tab

	// The currently active Set, or null when no Set is selected
	protected PlanningSet _planningSet;

	// The currently paired Simulation Group, or null when none is selected
	protected AbstractSimulationGroup _simulationGroup;

	// Reference to the owning PlanningPanel, for cross-tab coordination (e.g. the summary strip)
	protected final PlanningPanel _parent;

	/**
	 * Constructs the panel with a GridBagLayout and stores the owning PlanningPanel.
	 *
	 * @param parent the PlanningPanel that owns this sub-tab; must not be null
	 */
	protected AbstractPlanningPanel(PlanningPanel parent) {
		super(new GridBagLayout()); // Every sub-tab lays out its controls with GridBagLayout
		_parent = parent; // Remember the owning PlanningPanel for cross-tab coordination
	}

	/**
	 * Updates this panel to reflect the newly selected Set, or clears its content when
	 * {@code planningSet} is null. Concrete subclasses should override this to repopulate
	 * their specific detail controls; they must call {@code super.setPlanningSet(planningSet)}
	 * so the shared {@link #_planningSet} field and enabled state stay correct.
	 *
	 * @param planningSet the newly active Set, or null if none is currently selected
	 */
	public void setPlanningSet(PlanningSet planningSet) {
		_planningSet = planningSet; // Track the newly active Set (or null) for subclasses to use
		setEnabled(planningSet != null); // Disable the whole panel's controls when no Set is selected
	}

	/**
	 * Updates this panel to reflect the newly paired Simulation Group, or clears its
	 * content when {@code simulationGroup} is null. Concrete subclasses that need the
	 * Simulation Group's own data (e.g. its {@code WatSimulation} list) should override
	 * this; they must call {@code super.setSimulationGroup(simulationGroup)} so the shared
	 * {@link #_simulationGroup} field stays correct.
	 *
	 * @param simulationGroup the newly paired Simulation Group, or null if none is
	 *                        currently selected
	 */
	public void setSimulationGroup(AbstractSimulationGroup simulationGroup) {
		_simulationGroup = simulationGroup; // Track the newly paired Simulation Group (or null) for subclasses to use
	}

	/**
	 * Called when this panel's tab becomes the selected tab in the left-hand
	 * {@code JTabbedPane}. The default implementation does nothing; subclasses with
	 * content that can go stale while unselected (e.g. a chart driven by data another tab
	 * may have changed) should override this to refresh themselves.
	 */
	public void panelActivated() {
		// No-op by default; subclasses override as needed. Kept as a real method body (rather
		// than removing the hook) so every sub-tab can opt in without changing this base class.
	}

	/**
	 * Returns the label used for both this panel's left-hand tab and its corresponding box
	 * in the Planning tab's category summary strip.
	 *
	 * @return the tab's display name
	 */
	public abstract String getTabName(); // Implemented by each of the six concrete sub-tab panels

	/**
	 * Returns the list model backing this panel's own detail list, reused by
	 * {@link CategorySummaryStripPanel} to render this tab's box in the summary strip along
	 * the upper portion of the Planning tab. Returning the same model instance the panel
	 * itself displays (rather than a copy) keeps the strip's preview live as the panel's
	 * data changes, with no extra synchronization code required.
	 *
	 * @return this panel's detail list model; never null (an empty model if there is no
	 *         current data)
	 */
	public abstract ListModel<Object> getSummaryListModel(); // Implemented by each of the six concrete sub-tab panels
}
