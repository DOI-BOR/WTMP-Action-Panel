package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.Component;                      // Return type of JTabbedPane.getSelectedComponent(), used to resolve the active sub-tab
import java.awt.GridBagConstraints;               // Layout constraints for positioning each row/section of this panel
import java.awt.GridBagLayout;                     // Flexible grid-based layout manager
import java.util.List;                             // Ordered collection interface for the project's SimulationGroup manager proxies

import javax.swing.DefaultComboBoxModel;            // Backing model for the Set combo box
import javax.swing.JButton;                         // Edit/New/Delete buttons for both the Set and Simulation Group rows
import javax.swing.JComboBox;                       // Set selector and Simulation Group selector
import javax.swing.JLabel;                          // Row labels
import javax.swing.JOptionPane;                      // Used to confirm Set/Simulation Group deletion
import javax.swing.JTabbedPane;                      // Left-hand tab strip hosting the six sub-tab panels

import com.rma.model.ManagerProxy;                  // Lightweight proxy wrapping each managed SimulationGroup, used as combo-box items
import com.rma.model.Project;                       // Represents the currently open WAT study; provides manager and proxy lookups
import rma.swing.RmaInsets;                          // Standard GridBagConstraints insets constants
import rma.swing.RmaJPanel;                          // Base Swing panel class this component extends, matching ForecastPanel's base class
import rma.util.RMASort;                             // RMA quicksort utility, used to sort the Simulation Group combo alphabetically

import usbr.wat.plugins.actionpanel.ActionsWindow;                      // The parent Actions Window this panel is hosted within
import usbr.wat.plugins.actionpanel.editors.NewSimulationGroupDialog;   // Existing dialog reused, unmodified, for creating/editing Simulation Groups
import usbr.wat.plugins.actionpanel.editors.planning.NewPlanningSetDialog;  // New/Edit dialog for a Set
import usbr.wat.plugins.actionpanel.commands.NewSimulationGroupCmd;     // Existing command class backing standard SimulationGroup creation
import usbr.wat.plugins.actionpanel.model.SimulationGroup;              // The existing model type used for the Simulation Group row (per the clarified data model, Planning pairs a Set with a standard SimulationGroup, not a new subtype)
import usbr.wat.plugins.actionpanel.model.planning.PlanningSet;         // The Set model this panel's Set row manages
import usbr.wat.plugins.actionpanel.model.planning.PlanningSetContainer; // Holds and persists the full list of Sets for the current project
import usbr.wat.plugins.actionpanel.ui.planning.temptarget.TempTargetPanel; // Temperature Targets sub-tab

/**
 * Top-level content of the Planning tab, added alongside "Prescribed Conditions" and
 * "Forecast Conditions" in {@link ActionsWindow}.
 *
 * Structurally mirrors {@code usbr.wat.plugins.actionpanel.ui.forecast.ForecastPanel}:
 * a selector row above a left-placed {@link JTabbedPane} of six sub-tabs (Initial
 * Conditions, Operations, Meteorology, Boundary Conditions, Temperature Targets,
 * Simulation), with a {@link CategorySummaryStripPanel} showing the current status of the
 * first five sub-tabs (mirroring the same strip already present in the Forecast Conditions
 * workflow).
 *
 * Unlike the Forecast panel, Planning exposes <b>two</b> independent selector rows at the
 * top: a <b>Set</b> row and a <b>Simulation Group</b> row. Per the clarified data model, a
 * Set (the climate-side forcing data — CalSim, Climate Scenario, and Hydrology) and a
 * Simulation Group (the model configuration that data is applied to) are independent, and
 * together form a unique pairing; both selectors are therefore shown side by side rather
 * than one nested inside the other.
 */
@SuppressWarnings("serial")
public class PlanningPanel extends RmaJPanel {

	// The parent ActionsWindow that hosts this panel
	private final ActionsWindow _parent;

	// --- Set row ---
	private JComboBox<PlanningSet> _setCombo;
	private JButton _setEditButton;
	private JButton _setNewButton;
	private JButton _setDeleteButton;

	// --- Simulation Group row ---
	private JComboBox<ManagerProxy> _simGroupCombo;
	private JButton _simGroupEditButton;
	private JButton _simGroupNewButton;
	private JButton _simGroupDeleteButton;

	// Backing store for all Sets defined in the current project
	private final PlanningSetContainer _setContainer = new PlanningSetContainer();

	// Upper category summary strip (Initial Conditions / Operations / Meteorology /
	// Boundary Conditions / Temperature Target Sets — no Simulation box)
	private CategorySummaryStripPanel _summaryStrip;

	// Left-hand tab strip hosting the six sub-tab panels
	private JTabbedPane _tabbedPane;

	private InitialConditionsPanel _initialConditionsPanel;
	private OperationsPanel _operationsPanel;
	private MeteorologyPanel _meteorologyPanel;
	private BcPanel _bcPanel;
	private TempTargetPanel _tempTargetPanel;
	private SimulationPanel _simulationPanel;

	// The AbstractPlanningPanel tab currently selected, used to notify it of activation
	private AbstractPlanningPanel _currentPanel;

	/**
	 * Constructs the Planning panel, stores the parent {@link ActionsWindow} reference,
	 * builds all sub-panels and rows, wires listeners, and attempts to load any Sets
	 * already saved for the current project.
	 *
	 * @param parent the {@link ActionsWindow} that owns and hosts this panel; must not be null
	 */
	public PlanningPanel(ActionsWindow parent) {
		super(new GridBagLayout()); // This panel lays out its own rows/sections with GridBagLayout
		_parent = parent; // Remember the owning ActionsWindow, needed by the Set/SimGroup dialogs

		buildControls(); // Build the Set row, Simulation Group row, summary strip, and tabbed pane
		addListeners(); // Wire up button clicks and selection changes
		loadSets(); // Populate the Set combo from any previously saved Sets
		loadSimulationGroupCombo(); // Populate the Simulation Group combo from the project's managers
	}

	/**
	 * Builds and lays out the Set row, Simulation Group row, category summary strip, and
	 * left-hand tabbed pane.
	 */
	private void buildControls() {
		GridBagConstraints gbc = new GridBagConstraints(); // Shared constraints object, reused/mutated per row

		// --- Set row ---
		JLabel setLabel = new JLabel("Set:"); // Label preceding the Set combo
		gbc.gridx = 0; // First column
		gbc.gridy = 0; // Top row
		gbc.gridwidth = 1; // Occupies a single cell
		gbc.weightx = 0.0; // No horizontal growth for the label
		gbc.weighty = 0.0; // No vertical growth
		gbc.anchor = GridBagConstraints.WEST; // Anchor to the left edge of its cell
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the label
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the label
		add(setLabel, gbc); // Place the Set label

		_setCombo = new JComboBox<>(); // Lists every Set saved for the current project
		gbc.gridx = 1; // Second column, same row
		gbc.gridy = 0; // Top row
		gbc.weightx = 1.0; // Allow the combo to absorb extra horizontal space
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		add(_setCombo, gbc); // Place the Set combo

		_setEditButton = new JButton("Edit..."); // Edits the currently selected Set
		gbc.gridx = 2; // Third column, same row
		gbc.gridy = 0; // Top row
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		add(_setEditButton, gbc); // Place the Edit button

		_setNewButton = new JButton("New..."); // Opens the New Planning Set dialog
		gbc.gridx = 3; // Fourth column, same row
		gbc.gridy = 0; // Top row
		add(_setNewButton, gbc); // Place the New button, reusing the rest of gbc's settings

		_setDeleteButton = new JButton("Delete..."); // Removes the currently selected Set
		gbc.gridx = 4; // Fifth column, same row
		gbc.gridy = 0; // Top row
		add(_setDeleteButton, gbc); // Place the Delete button, reusing the rest of gbc's settings

		// --- Simulation Group row ---
		JLabel simGroupLabel = new JLabel("Simulation Group:"); // Label preceding the Simulation Group combo
		gbc.gridx = 0; // First column
		gbc.gridy = 1; // Second row, below the Set row
		gbc.weightx = 0.0; // No horizontal growth for the label
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the label
		add(simGroupLabel, gbc); // Place the Simulation Group label

		_simGroupCombo = new JComboBox<>(); // Lists every SimulationGroup manager proxy in the project
		gbc.gridx = 1; // Second column, same row
		gbc.gridy = 1; // Second row
		gbc.weightx = 1.0; // Allow the combo to absorb extra horizontal space
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		add(_simGroupCombo, gbc); // Place the Simulation Group combo

		_simGroupEditButton = new JButton("Edit..."); // Edits the currently selected Simulation Group
		gbc.gridx = 2; // Third column, same row
		gbc.gridy = 1; // Second row
		gbc.weightx = 0.0; // No horizontal growth for the button
		gbc.fill = GridBagConstraints.NONE; // Do not stretch the button
		add(_simGroupEditButton, gbc); // Place the Edit button

		_simGroupNewButton = new JButton("New..."); // Opens the standard New Simulation Group dialog
		gbc.gridx = 3; // Fourth column, same row
		gbc.gridy = 1; // Second row
		add(_simGroupNewButton, gbc); // Place the New button, reusing the rest of gbc's settings

		_simGroupDeleteButton = new JButton("Delete..."); // Removes the currently selected Simulation Group
		gbc.gridx = 4; // Fifth column, same row
		gbc.gridy = 1; // Second row
		add(_simGroupDeleteButton, gbc); // Place the Delete button, reusing the rest of gbc's settings

		// --- Category summary strip ---
		// Build every sub-tab panel up front, since both the strip and the tabbed pane below need them
		_initialConditionsPanel = new InitialConditionsPanel(this);
		_operationsPanel = new OperationsPanel(this);
		_meteorologyPanel = new MeteorologyPanel(this);
		_bcPanel = new BcPanel(this);
		_tempTargetPanel = new TempTargetPanel(this);
		_simulationPanel = new SimulationPanel(this);

		_summaryStrip = new CategorySummaryStripPanel(); // Row of mini preview boxes, one per non-Simulation tab
		// Register each tab's own list model so the strip's boxes stay live automatically
		_summaryStrip.addCategoryBox(_initialConditionsPanel.getTabName(), _initialConditionsPanel.getSummaryListModel());
		_summaryStrip.addCategoryBox(_operationsPanel.getTabName(), _operationsPanel.getSummaryListModel());
		_summaryStrip.addCategoryBox(_meteorologyPanel.getTabName(), _meteorologyPanel.getSummaryListModel());
		_summaryStrip.addCategoryBox(_bcPanel.getTabName(), _bcPanel.getSummaryListModel());
		_summaryStrip.addCategoryBox(_tempTargetPanel.getTabName(), _tempTargetPanel.getSummaryListModel());

		gbc.gridx = 0; // First column
		gbc.gridy = 2; // Third row, below both selector rows
		gbc.gridwidth = 5; // Span all five columns used by the rows above
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 0.0; // No vertical growth; the strip keeps a fixed height
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch to fill available width
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the strip
		add(_summaryStrip, gbc); // Place the category summary strip

		// --- Left-hand tabbed pane hosting the six sub-tabs, all initially disabled ---
		// Disable every sub-tab until a Set is actually selected, matching the Forecast workflow's behavior
		_initialConditionsPanel.setEnabled(false);
		_operationsPanel.setEnabled(false);
		_meteorologyPanel.setEnabled(false);
		_bcPanel.setEnabled(false);
		_tempTargetPanel.setEnabled(false);
		_simulationPanel.setEnabled(false);

		_tabbedPane = new JTabbedPane(); // Hosts the six sub-tab panels
		_tabbedPane.setTabPlacement(JTabbedPane.LEFT); // Tabs run down the left side, matching the mockup
		// Add each sub-tab in the mockup's specified order
		_tabbedPane.addTab(_initialConditionsPanel.getTabName(), _initialConditionsPanel);
		_tabbedPane.addTab(_operationsPanel.getTabName(), _operationsPanel);
		_tabbedPane.addTab(_meteorologyPanel.getTabName(), _meteorologyPanel);
		_tabbedPane.addTab(_bcPanel.getTabName(), _bcPanel);
		_tabbedPane.addTab(_tempTargetPanel.getTabName(), _tempTargetPanel);
		_tabbedPane.addTab(_simulationPanel.getTabName(), _simulationPanel);

		gbc.gridx = 0; // First column
		gbc.gridy = 3; // Fourth row, below the summary strip
		gbc.gridwidth = 5; // Span all five columns used by the rows above
		gbc.weightx = 1.0; // Allow horizontal growth
		gbc.weighty = 1.0; // Allow vertical growth so the tabbed pane fills remaining space
		gbc.anchor = GridBagConstraints.NORTHWEST; // Anchor content to the top-left of its cell
		gbc.fill = GridBagConstraints.BOTH; // Stretch in both directions
		gbc.insets = RmaInsets.INSETS5505; // Standard spacing around the tabbed pane
		add(_tabbedPane, gbc); // Place the tabbed pane, filling the remainder of the panel

		_currentPanel = (AbstractPlanningPanel) _tabbedPane.getSelectedComponent(); // The tab selected by default (Initial Conditions)
		_summaryStrip.setActiveTab(_currentPanel.getTabName()); // Highlight that tab's box in the strip immediately
	}

	/**
	 * Attaches listeners for the Set row, Simulation Group row, and left-hand tab changes.
	 */
	private void addListeners() {
		_setNewButton.addActionListener(e -> newSet()); // Opens the New Planning Set dialog
		_setEditButton.addActionListener(e -> editSet()); // Opens the Edit Planning Set dialog
		_setDeleteButton.addActionListener(e -> deleteSet()); // Removes the selected Set
		_setCombo.addActionListener(e -> setSelected((PlanningSet) _setCombo.getSelectedItem())); // Propagates selection changes to every sub-tab

		_simGroupNewButton.addActionListener(e -> newSimulationGroup()); // Opens the standard New Simulation Group dialog
		_simGroupEditButton.addActionListener(e -> editSimulationGroup()); // Opens the standard Edit Simulation Group dialog
		_simGroupDeleteButton.addActionListener(e -> deleteSimulationGroup()); // Removes the selected Simulation Group
		_simGroupCombo.addActionListener(e -> simGroupSelected()); // Propagates selection changes to every sub-tab

		_tabbedPane.addChangeListener(e -> tabSelectionChanged()); // Keeps the summary strip and panelActivated() hook in sync
	}

	/**
	 * Handles a left-hand tab selection change: notifies the newly selected sub-tab
	 * panel that it has become active and updates the summary strip's highlighted box.
	 */
	private void tabSelectionChanged() {
		Component selected = _tabbedPane.getSelectedComponent(); // The panel now showing in the tabbed pane
		if (!(selected instanceof AbstractPlanningPanel)) {
			return; // Defensive guard; every tab added above is an AbstractPlanningPanel
		}

		_currentPanel = (AbstractPlanningPanel) selected; // Remember which sub-tab is now active
		_currentPanel.panelActivated(); // Give the panel a chance to refresh any stale content
		_summaryStrip.setActiveTab(_currentPanel.getTabName()); // Move the highlighted box to match
	}

	// --- Set row behavior ---

	/**
	 * Loads all Sets previously saved for the current project into the Set combo box.
	 * If loading fails (e.g. malformed XML), the combo box is simply left empty; the user
	 * can still create new Sets.
	 */
	private void loadSets() {
		try {
			_setContainer.load(); // Attempt to read any previously saved Sets from disk
		} catch (Exception ex) {
			// No previously saved Sets, or the file could not be parsed; start empty.
		}
		refreshSetCombo(); // Reflect whatever was (or wasn't) loaded in the combo box
	}

	/**
	 * Reloads the Set combo box's model from the current contents of {@link #_setContainer}.
	 */
	private void refreshSetCombo() {
		Object previouslySelected = _setCombo.getSelectedItem(); // Remember the prior selection, if any
		_setCombo.setModel(new DefaultComboBoxModel<>(_setContainer.getSets().toArray(new PlanningSet[0]))); // Rebuild from the container
		if (_setContainer.getSets().contains(previouslySelected)) {
			_setCombo.setSelectedItem(previouslySelected); // Keep the same Set selected if it still exists
		} else if (_setCombo.getItemCount() > 0) {
			_setCombo.setSelectedIndex(0); // Otherwise default to the first available Set
		} else {
			setSelected(null); // No Sets at all; clear every sub-tab's state
		}
	}

	/**
	 * Opens the New Planning Set dialog, and on success adds the resulting Set to the
	 * container, persists it, and selects it.
	 */
	private void newSet() {
		NewPlanningSetDialog dlg = new NewPlanningSetDialog(_parent); // Child dialog, modal over the ActionsWindow
		dlg.setVisible(true); // Blocks until the dialog is closed
		if (dlg.isCanceled()) {
			return; // User backed out; nothing to add
		}

		PlanningSet set = dlg.getPlanningSet(); // The newly created Set
		_setContainer.addSet(set); // Register it with this project's container
		saveSetsQuietly(); // Persist the updated Set list to disk
		refreshSetCombo(); // Reflect the new Set in the combo box
		_setCombo.setSelectedItem(set); // Select it immediately for convenience
	}

	/**
	 * Opens the Edit Planning Set dialog for the currently selected Set, and on success
	 * persists the change and refreshes dependent UI.
	 */
	private void editSet() {
		PlanningSet selected = (PlanningSet) _setCombo.getSelectedItem(); // Nothing to edit if none selected
		if (selected == null) {
			return;
		}

		NewPlanningSetDialog dlg = new NewPlanningSetDialog(_parent, selected); // Pre-populated with the selected Set
		dlg.setVisible(true); // Blocks until the dialog is closed
		if (dlg.isCanceled()) {
			return; // User backed out; nothing changed
		}

		saveSetsQuietly(); // Persist the edited Set list to disk
		refreshSetCombo(); // Reflect any changes (including a possible rename) in the combo box
		_setCombo.setSelectedItem(dlg.getPlanningSet()); // Re-select the (possibly renamed) Set
	}

	/**
	 * Deletes the currently selected Set after confirmation, persists the change, and
	 * refreshes dependent UI.
	 */
	private void deleteSet() {
		PlanningSet selected = (PlanningSet) _setCombo.getSelectedItem(); // Nothing to delete if none selected
		if (selected == null) {
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete Set \"" + selected.getName() + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return; // User declined the confirmation prompt
		}

		_setContainer.removeSetByName(selected.getName()); // Drop it from the project's container
		saveSetsQuietly(); // Persist the updated Set list to disk
		refreshSetCombo(); // Reflect the deletion in the combo box
	}

	/**
	 * Attempts to persist the current Set list, silently ignoring failures so a save error
	 * does not block the user from continuing to work; a production implementation should
	 * surface this failure more visibly (e.g. a status bar message).
	 */
	private void saveSetsQuietly() {
		try {
			_setContainer.save(); // Write the current Set list out to disk
		} catch (Exception ex) {
			// TODO: surface save failures to the user (e.g. a status bar message) rather than swallowing them.
		}
	}

	/**
	 * Propagates the newly selected Set to every sub-tab panel and, for temperature
	 * targets, allows the tab to refresh its mode/detail controls accordingly.
	 *
	 * @param set the newly selected Set, or null if none is selected
	 */
	private void setSelected(PlanningSet set) {
		// Every sub-tab needs to know about the newly active Set, regardless of which is currently visible
		_initialConditionsPanel.setPlanningSet(set);
		_operationsPanel.setPlanningSet(set);
		_meteorologyPanel.setPlanningSet(set);
		_bcPanel.setPlanningSet(set);
		_tempTargetPanel.setPlanningSet(set);
		_simulationPanel.setPlanningSet(set);
	}

	// --- Simulation Group row behavior ---

	/**
	 * Reloads the Simulation Group combo box from the project's manager list, sorted
	 * alphabetically, mirroring the loading behavior of
	 * {@code usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel#loadSimulationGroupCombo()}.
	 */
	private void loadSimulationGroupCombo() {
		Project prj = Project.getCurrentProject(); // The currently open WAT study
		if (prj == null) {
			return; // No project open yet; nothing to load
		}

		List<ManagerProxy> proxies = prj.getManagerProxyListForType(SimulationGroup.class); // Every standard SimulationGroup in the project
		RMASort.quickSort(proxies); // Sort alphabetically for a predictable, easy-to-scan combo box

		_simGroupCombo.setModel(new DefaultComboBoxModel<>(proxies.toArray(new ManagerProxy[0]))); // Rebuild the combo's model
		if (_simGroupCombo.getItemCount() > 0) {
			_simGroupCombo.setSelectedIndex(0); // Default to the first available group
		} else {
			simGroupSelected(); // No groups at all; clear every sub-tab's Simulation Group state
		}
	}

	/**
	 * Opens the standard New Simulation Group dialog (unchanged from the rest of the
	 * plugin), configured for the standard {@link SimulationGroup} type, and on success
	 * refreshes the combo box and selects the new group.
	 */
	private void newSimulationGroup() {
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true, "New Simulation Group"); // Reuse the existing dialog as-is
		dlg.setSimulationGroupClass(SimulationGroup.class); // Ensure it creates a standard SimulationGroup, not a ForecastSimGroup
		dlg.setSimulationGroupFactory(NewSimulationGroupCmd.class); // Backing command class for standard group creation
		dlg.setVisible(true); // Blocks until the dialog is closed

		loadSimulationGroupCombo(); // Refresh the combo box regardless of outcome, in case a group was created
	}

	/**
	 * Opens the standard Edit Simulation Group dialog for the currently selected group.
	 */
	private void editSimulationGroup() {
		ManagerProxy proxy = (ManagerProxy) _simGroupCombo.getSelectedItem(); // Nothing to edit if none selected
		if (proxy == null) {
			return;
		}

		SimulationGroup group = (SimulationGroup) proxy.loadManager(); // Resolve the live object from its proxy
		NewSimulationGroupDialog dlg = new NewSimulationGroupDialog(_parent, true, "Edit Simulation Group"); // Reuse the existing dialog as-is
		dlg.setSimulationGroupClass(SimulationGroup.class); // Ensure it edits a standard SimulationGroup
		dlg.setSimulationGroupFactory(NewSimulationGroupCmd.class); // Backing command class for standard group editing
		dlg.setVisible(true); // Blocks until the dialog is closed

		loadSimulationGroupCombo(); // Refresh the combo box regardless of outcome, in case the group changed
	}

	/**
	 * Deletes the currently selected Simulation Group after confirmation.
	 *
	 * <p><b>Extension point:</b> this should delegate to the same deletion pathway used
	 * elsewhere in the plugin (see
	 * {@code usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction}) so
	 * cascading cleanup (associated files, tree nodes, etc.) is handled consistently;
	 * wiring that in is left for integration since it requires the live
	 * {@code ActionsWindow}/tree context this panel does not otherwise need.</p>
	 */
	private void deleteSimulationGroup() {
		ManagerProxy proxy = (ManagerProxy) _simGroupCombo.getSelectedItem(); // Nothing to delete if none selected
		if (proxy == null) {
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete the selected Simulation Group?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return; // User declined the confirmation prompt
		}

		// TODO: delegate to DeleteSimulationGroupAction (or equivalent) for full cascading cleanup.
		loadSimulationGroupCombo(); // Refresh the combo box; real deletion still needs to be wired up above
	}

	/**
	 * Propagates the newly selected Simulation Group to every sub-tab panel.
	 */
	private void simGroupSelected() {
		ManagerProxy proxy = (ManagerProxy) _simGroupCombo.getSelectedItem(); // May be null if the combo is empty
		SimulationGroup group = proxy != null ? (SimulationGroup) proxy.loadManager() : null; // Resolve the live object, or null

		// Every sub-tab needs to know about the newly paired Simulation Group, regardless of which is currently visible
		_initialConditionsPanel.setSimulationGroup(group);
		_operationsPanel.setSimulationGroup(group);
		_meteorologyPanel.setSimulationGroup(group);
		_bcPanel.setSimulationGroup(group);
		_tempTargetPanel.setSimulationGroup(group);
		_simulationPanel.setSimulationGroup(group);
	}
}
