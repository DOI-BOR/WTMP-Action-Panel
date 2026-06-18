package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;          // Provides invokeLater for scheduling work on the Event Dispatch Thread
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.event.ItemEvent;     // Carries combo-box selection change data for the item listener

import java.util.List;               // Generic ordered collection interface used for simulation group proxy lists

import javax.swing.Action;           // Swing Action interface returned by the abstract action factory methods
import javax.swing.JButton;          // Swing button component bound to Actions for edit, new, delete, and update
import javax.swing.JLabel;           // Swing label for the "Simulation Group:" and "Description:" field labels
import javax.swing.JTabbedPane;      // Tabbed pane field reserved for subclass use (currently unused at this level)

import com.rma.event.ProjectAdapter; // Convenience adapter providing no-op implementations of ProjectListener methods
import com.rma.event.ProjectEvent;   // Carries the Project reference delivered by open/close notifications
import com.rma.model.ManagerProxy;   // Lightweight proxy wrapping a managed model object; used as combo-box items
import com.rma.model.Project;        // Represents the currently open WAT study; provides manager and proxy lookups

import rma.swing.EnabledJPanel;          // RMA JPanel subclass with built-in enabled/disabled visual state support
import rma.swing.RmaInsets;              // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJComboBox;           // RMA-enhanced combo box with typed model support
import rma.swing.RmaJDescriptionField;   // RMA text field intended for multi-line description display and editing
import rma.swing.list.RmaListModel;      // RMA list model used as the combo-box backing model
import rma.util.RMASort;                 // RMA utility providing a quicksort implementation for manager proxy lists

import usbr.wat.plugins.actionpanel.ActionPanelPlugin;                              // Singleton plugin entry point providing access to the actions window
import usbr.wat.plugins.actionpanel.actions.UpdateModelsAction;                     // Action that triggers a model update for the current actions window
import usbr.wat.plugins.actionpanel.actions.forecast.DeleteForecastSimGroupAction;  // Action to delete a forecast simulation group (used by forecast subclass)
import usbr.wat.plugins.actionpanel.actions.forecast.EditForecastSimGroupAction;    // Action to edit a forecast simulation group (used by forecast subclass)
import usbr.wat.plugins.actionpanel.actions.forecast.NewForecastSimGroupAction;     // Action to create a new forecast simulation group (used by forecast subclass)
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;                  // Base model class shared by all simulation group types
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                // Forecast-specific simulation group model (imported for subclass context)


/**
 * Abstract base panel providing the shared toolbar and combo-box UI for selecting
 * and managing simulation groups within the WTMP action panel.
 *
 * This class renders a horizontal control bar containing:
 *   A labeled combo box listing all simulation groups of the type managed by the subclass.
 *   Edit, New, and Delete buttons whose Actions are supplied by concrete subclasses.
 *   An "Update Models" button that is conditionally shown based on the GIT_DASH_D_FLAG
 *   system property, enabling Git-aware deployments to refresh model files.
 *   A description field (currently commented out) reserved for future display of the
 *   selected group's description text.
 *
 * Concrete subclasses must implement:
 *   getEditSimGroupAction()  -- returns the Action bound to the Edit button.
 *   getNewSimGroupAction()   -- returns the Action bound to the New button.
 *   getDeleteSimGroupAction(BaseSimulationGroupPanel) -- returns the Action bound to Delete.
 *   simGroupSelected(ItemEvent) -- reacts to combo-box selection changes.
 *   getSimGroupClass()       -- returns the Class used to query the project's manager list.
 *
 * Project open and close events are handled via a static ProjectAdapter so the combo
 * box is populated or cleared automatically as the active study changes.
 */
public abstract class BaseSimulationGroupPanel extends EnabledJPanel {
	/**
	 * System property key whose boolean value controls whether the "Update Models"
	 * button and Git-specific layout adjustments are included at startup.
	 * Set to true in environments where Git integration is available.
	 */
	public static final String GIT_DASH_D_FLAG = "WTMP.HasGit";


	// --- Child components ---

	/**
	 * The parent simulation panel that owns this control bar.
	 */
	protected AbstractSimulationPanel _parent;

	/**
	 * Combo box listing all simulation group proxies of the type managed by this panel.
	 * Protected so subclasses can read the selection without going through a method call.
	 */
	protected RmaJComboBox<ManagerProxy> _simulationGroupCombo;

	/**
	 * Button that opens the edit dialog for the selected simulation group.
	 * Protected so subclasses can adjust its enabled state independently.
	 */
	protected JButton _editButton;

	/**
	 * Button that opens the new simulation group creation dialog.
	 */
	private JButton _newButton;

	/**
	 * Button that deletes the currently selected simulation group.
	 */
	private JButton _deleteButton;

	/**
	 * Button that triggers a model update for the current actions window.
	 */
	private JButton _updateModelsBtn;

	/**
	 * Read-only description field showing the selected group's description text.
	 * Currently excluded from the layout (add call is commented out) but retained
	 * for potential future re-enablement.
	 */
	private RmaJDescriptionField _descFld;

	/**
	 * Tabbed pane field reserved for subclass use; not populated at this level.
	 */
	private JTabbedPane _tabbedPane;

	/**
	 * Label for the simulation group combo box.
	 */
	private JLabel _simGroupLabel;


	/**
	 * Constructs the panel with a GridBagLayout, stores the parent reference, then
	 * builds all controls and attaches listeners.
	 *
	 * @param parent the AbstractSimulationPanel that owns this toolbar panel; must not be null
	 */
	public BaseSimulationGroupPanel(AbstractSimulationPanel parent) {
		super(new GridBagLayout());

		// Store the owning simulation panel for delegation and context access
		_parent = parent;

		// Build and add all child components
		buildControls();

		// Wire up project and combo-box listeners
		addListeners();
	}


	/**
	 * Constructs and lays out all child components using GridBagLayout.
	 *
	 * The layout order from left to right is:
	 * "Simulation Group:" label, combo box, Edit button, New button, Delete button,
	 * and (conditionally) the "Update Models" button when GIT_DASH_D_FLAG is true.
	 *
	 * Button initial state: Edit, New, and Delete are all disabled at construction and
	 * are re-enabled when a project is opened or a selection is made.
	 *
	 * The Description label and field are constructed but currently commented out of
	 * the layout; they remain in the code for potential future use.
	 */
	protected void buildControls() {
		// --- "Simulation Group:" label ---
		_simGroupLabel = new JLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simGroupLabel, gbc);

		// --- Simulation group combo box (expands horizontally to fill available space) ---
		_simulationGroupCombo = new RmaJComboBox<>();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simulationGroupCombo, gbc);

		// --- Edit button: disabled until a group is selected ---
		_editButton = new JButton(getEditSimGroupAction());
		_editButton.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5500;
		add(_editButton, gbc);

		// --- New button: disabled until a project is open ---
		_newButton = new JButton(getNewSimGroupAction());
		_newButton.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5500;
		add(_newButton, gbc);

		// Check the system property to determine whether the Git-specific button should appear
		boolean hasGitButton = Boolean.getBoolean(GIT_DASH_D_FLAG);

		// --- Delete button: spans to end of row unless the Git button will follow it ---
		_deleteButton = new JButton(getDeleteSimGroupAction(this));
		_deleteButton.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = (hasGitButton ? 1 : GridBagConstraints.REMAINDER);
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_deleteButton, gbc);

		// --- "Update Models" button: only added to the layout in Git-enabled deployments ---
		_updateModelsBtn = new JButton(
				new UpdateModelsAction(ActionPanelPlugin.getInstance().getActionsWindow()));
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.insets(5, 10, 0, 5);
		if (hasGitButton) {
			add(_updateModelsBtn, gbc);
		}

		// --- Description label (constructed but excluded from the layout) ---
		JLabel label = new JLabel("Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		// add(label, gbc);  -- currently disabled; retained for potential future use

		// --- Description field (constructed but excluded from the layout) ---
		_descFld = new RmaJDescriptionField();
		_descFld.setEditable(false);
		_descFld.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		// add(_descFld, gbc);  -- currently disabled; retained for potential future use
	}


	/**
	 * Returns the Action to bind to the Delete button.
	 *
	 * The panel itself is passed as a parameter so the action can call back into
	 * simulationGroupDeleted(ManagerProxy) after a deletion is confirmed.
	 *
	 * @param parent this panel instance, passed to the action for post-deletion callbacks
	 * @return the delete Action appropriate for the concrete subclass's group type
	 */
	protected abstract Action getDeleteSimGroupAction(BaseSimulationGroupPanel parent);


	/**
	 * Returns the Action to bind to the New button.
	 *
	 * @return the new-group Action appropriate for the concrete subclass's group type
	 */
	protected abstract Action getNewSimGroupAction();


	/**
	 * Returns the Action to bind to the Edit button.
	 *
	 * @return the edit Action appropriate for the concrete subclass's group type
	 */
	protected abstract Action getEditSimGroupAction();


	/**
	 * Registers a static project listener for open and close events, and attaches
	 * an item listener to the simulation group combo box.
	 *
	 * Project events are handled on the EDT via invokeLater to ensure that any
	 * model queries issued inside studyOpened and studyClosed run after the framework
	 * has finished updating its own project state.
	 *
	 * The combo-box item listener delegates to the abstract simGroupSelected method
	 * so concrete subclasses can react to selection changes without overriding this method.
	 */
	protected void addListeners() {
		// Register a static project listener so panel updates are triggered automatically
		// whenever any project is opened or closed, regardless of which window is active
		Project.addStaticProjectListener(new ProjectAdapter() {
			@Override
			public void projectOpened(ProjectEvent e) {
				// Defer to the EDT so project state is fully initialized before querying it
				EventQueue.invokeLater(() -> studyOpened());
			}

			@Override
			public void projectClosed(ProjectEvent e) {
				// Defer to the EDT to ensure all project cleanup has completed first
				EventQueue.invokeLater(() -> studyClosed());
			}
		});

		// Forward combo-box selection changes to the abstract handler in the subclass
		_simulationGroupCombo.addItemListener(this::simGroupSelected);
	}


	/**
	 * Called when the simulation group combo box selection changes.
	 *
	 * Subclasses implement this method to update the parent simulation panel and
	 * any dependent UI state when the user picks a different group. A null event
	 * may be passed when the selection is programmatically cleared.
	 *
	 * @param e the item event carrying the new selection; may be null when the
	 *          selection is cleared by loadSimulationGroupCombo
	 */
	protected abstract void simGroupSelected(ItemEvent e);


	/**
	 * Resets the combo box and disables all group-management buttons when a project is closed.
	 *
	 * Called on the EDT after a projectClosed event is received.
	 */
	protected void studyClosed() {
		// Clear all items from the combo box so no stale groups are shown
		_simulationGroupCombo.removeAllItems();

		// Disable all management buttons; they require an open project to function
		_editButton.setEnabled(false);
		_newButton.setEnabled(false);
		_deleteButton.setEnabled(false);
	}


	/**
	 * Enables the New and Delete buttons and reloads the combo box when a project opens.
	 *
	 * The Edit button remains disabled until a specific group is selected.
	 * Called on the EDT after a projectOpened event is received.
	 */
	protected void studyOpened() {
		// Enable the buttons that do not require an existing selection
		_newButton.setEnabled(true);
		_deleteButton.setEnabled(true);

		// Populate the combo box with all simulation groups in the newly opened project
		loadSimulationGroupCombo();
	}


	/**
	 * Programmatically selects the given simulation group in the combo box and updates
	 * the description field to match.
	 *
	 * If fsg is null the combo box is deselected and the description field is cleared.
	 * If the proxy for fsg is not yet in the model it is added before selecting it.
	 * The description field is enabled and made editable only when a valid group is set.
	 *
	 * @param fsg the simulation group to select; pass null to clear the selection
	 */
	public void setSimulationGroup(AbstractSimulationGroup fsg) {
		if (fsg == null) {
			// Clear the selection and reset the description field to an empty disabled state
			_simulationGroupCombo.setSelectedIndex(-1);
			_descFld.setEnabled(false);
			_descFld.setText("");
			return;
		}

		// Enable the description field now that a valid group has been provided
		_descFld.setEnabled(true);
		_descFld.setEditable(true);

		// Obtain the ManagerProxy for this group from the current project
		RmaListModel model = (RmaListModel) _simulationGroupCombo.getModel();
		ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(fsg);

		if (proxy == null) {
			// The project does not have a proxy for this group; deselect and bail out
			_simulationGroupCombo.setSelectedIndex(-1);
			return;
		}

		// Add the proxy to the model if it is not already present (e.g. newly created group)
		if (!model.contains(proxy)) {
			model.addElement(proxy);
		}

		// Only update the selection if it has actually changed, to avoid spurious item events
		if (_simulationGroupCombo.getSelectedItem() != proxy) {
			_simulationGroupCombo.setSelectedItem(proxy);
		}

		// Reflect the group's description text in the read-only field
		_descFld.setText(fsg.getDescription());
	}


	/**
	 * Reloads the simulation group combo box from the current project's manager list.
	 *
	 * The full list of proxies for the subclass's group type is retrieved, sorted
	 * alphabetically, and set as a fresh model. After the reload:
	 * If the previously selected proxy is still in the new model, it is reselected.
	 * If only one group exists, it is auto-selected.
	 * If nothing ends up selected, simGroupSelected is called with null to clear
	 * any dependent UI state.
	 */
	public void loadSimulationGroupCombo() {
		Project prj = Project.getCurrentProject();

		// Remember the currently selected proxy so it can be restored after the reload
		Object curProxy = _simulationGroupCombo.getSelectedItem();

		// Retrieve all proxies for the group type managed by this subclass
		List<ManagerProxy> simGroupProxies = prj.getManagerProxyListForType(getSimGroupClass());

		// Sort alphabetically so the combo box presents groups in a consistent order
		RMASort.quickSort(simGroupProxies);

		// Replace the combo model entirely to avoid stale or duplicate entries
		RmaListModel<ManagerProxy> newModel = new RmaListModel<>(false, simGroupProxies);
		_simulationGroupCombo.setModel(newModel);

		if (newModel.contains(curProxy)) {
			// Restore the previous selection if the group still exists
			_simulationGroupCombo.setSelectedItem(curProxy);
		} else if (_simulationGroupCombo.getItemCount() == 1) {
			// Auto-select the only available group for convenience
			_simulationGroupCombo.setSelectedIndex(0);
		}

		// If nothing is selected after the reload, notify the subclass to clear its state
		if (_simulationGroupCombo.getSelectedIndex() < 0) {
			simGroupSelected(null);
		}
	}


	/**
	 * Returns the Class of the simulation group type that this panel manages.
	 *
	 * Used by loadSimulationGroupCombo to query the correct manager list from the project.
	 * For example, a forecast-specific subclass would return ForecastSimGroup.class.
	 *
	 * @return the concrete simulation group class managed by this panel subclass
	 */
	protected abstract Class getSimGroupClass();


	/**
	 * Adds the given simulation group to the combo box and optionally selects it.
	 *
	 * Typically called after a "New" action successfully creates a group so the
	 * combo box reflects the addition without requiring a full reload.
	 * Does nothing if simGroup is null or if no proxy can be found for it.
	 *
	 * @param simGroup       the newly created simulation group to add; may be null
	 * @param selectSimGroup true to select the group immediately after adding it
	 */
	public void addSimulationGroup(AbstractSimulationGroup simGroup, boolean selectSimGroup) {
		// Check that the simulation group exists
		if (simGroup == null) {
			return;
		}

		// Obtain the project proxy for the new group and add it to the combo model
		ManagerProxy proxy = Project.getCurrentProject().getManagerProxy(simGroup);
		if (proxy != null) {
			_simulationGroupCombo.addItem(proxy);
		}

		// Optionally select the new group so it becomes the active context immediately
		if (selectSimGroup) {
			setSimulationGroup(simGroup);
		}
	}


	/**
	 * Removes the given proxy from the combo box after a simulation group has been deleted.
	 *
	 * If the deleted group was the currently selected item the selection is cleared
	 * by calling setSimulationGroup(null), which also resets dependent UI state.
	 *
	 * @param proxy the ManagerProxy of the group that was deleted; may be null (no-op)
	 */
	public void simulationGroupDeleted(ManagerProxy proxy) {
		// Check that the proxy exists
		if (proxy == null) {
			return;
		}

		// Capture the current selection before removing the item so we can detect if it changed
		Object selectedProxy = _simulationGroupCombo.getSelectedItem();

		_simulationGroupCombo.removeItem(proxy);

		// If the deleted group was selected, clear the panel state to reflect no active group
		if (selectedProxy == proxy) {
			setSimulationGroup(null);
		}
	}
}
