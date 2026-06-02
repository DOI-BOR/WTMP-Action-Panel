package usbr.wat.plugins.actionpanel;

import java.awt.GridBagConstraints;                                             // Layout constraints object for positioning components in a grid-based layout
import java.awt.GridBagLayout;                                                  // Grid-based layout manager for arranging components in rows and columns

import javax.swing.JButton;                                                     // Swing push button component used for clickable actions
import javax.swing.JPanel;                                                      // Swing container component serving as the base class for custom panels
import javax.swing.JPopupMenu;                                                  // Swing popup menu used to attach a dropdown of actions to a button
import org.jdesktop.swingx.JXDropButton;                                        // SwingX component that provides a button with an attached dropdown menu

import com.rma.event.ProjectAdapter;                                            // Listener adapter with default implementations for project lifecycle events
import com.rma.event.ProjectEvent;                                              // Event object representing changes in the project lifecycle
import com.rma.model.Project;                                                   // Accessor for the current project and project-level operations

import rma.swing.RmaInsets;                                                     // Standardized insets utility for consistent component padding and spacing

import usbr.wat.plugins.actionpanel.actions.AboutAction;                        // Action that opens an About dialog with plugin information
import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;        // Action to delete a simulation group (import present even if unused in this class)
import usbr.wat.plugins.actionpanel.actions.EditInterativeSimulationAction;     // Action to edit interactive simulation settings and controls
import usbr.wat.plugins.actionpanel.actions.EditSimulationGroupAction;          // Action to edit the properties of a simulation group
import usbr.wat.plugins.actionpanel.actions.NewSimulationGroupAction;           // Action to create a new simulation group (import present even if unused in this class)
import usbr.wat.plugins.actionpanel.actions.PostResultsAction;                  // Action to post or publish results to an external target
import usbr.wat.plugins.actionpanel.actions.ReviewDataAction;                   // Action to review loaded or processed data prior to running simulations
import usbr.wat.plugins.actionpanel.actions.SelectAlternativesAction;           // Action to select modeling alternatives (import present even if unused in this class)
import usbr.wat.plugins.actionpanel.actions.SelectSimulationGroupAction;        // Action to select an existing simulation group (import present even if unused in this class)
import usbr.wat.plugins.actionpanel.actions.UpdateDataAction;                   // Action to update data sources and refresh datasets
import usbr.wat.plugins.actionpanel.actions.UpdateModelsAction;                 // Action to update model artifacts and dependencies
import usbr.wat.plugins.actionpanel.actions.ViewIterationResultsAction;         // Action to view iteration results from interactive simulation runs
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;              // Base type representing a simulation group used by the actions panel
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;                        // Parent calibration panel that owns or interacts with this actions panel

/**
 * Panel that hosts user-facing actions for data review, interactive simulations,
 * posting results, and related operations in the WTMP Action Panel plugin.
 *
 * This panel constructs the action controls, wires event listeners to respond
 * to project lifecycle changes, and provides enablement toggles based on the
 * presence of a selected simulation group.
 *
 */
@SuppressWarnings("serial")
public class ActionsPanel extends JPanel {
	/** Reference to the owning actions window for dialog and UI coordination. */
	private ActionsWindow _parent;

	/** Action for updating model artifacts and dependencies. */
	private UpdateModelsAction _updateModelsAction;

	/** Action for selecting modeling alternatives for a run or configuration. */
	private SelectAlternativesAction _selectAlternativeAction;

	/** Action for updating data sources and refreshing datasets. */
	private UpdateDataAction _updateDataAction;

	/** Action for reviewing data prior to analysis or simulation. */
	private ReviewDataAction _reviewDataAction;

	/** Action for posting or publishing results after analysis. */
	private PostResultsAction _postResultsAction;

	/** Action for editing a simulation group. */
	private EditSimulationGroupAction _editSimulationAction;

	/** Action for editing interactive simulation settings and controls. */
	private EditInterativeSimulationAction _editInterativeSimAction;

	/** Action for opening the About dialog. */
	private AboutAction _aboutAction;

	/** Parent calibration panel that owns this actions panel. */
	private CalibrationPanel _parentPanel;

	/**
	 * Constructs the actions panel and initializes its controls and listeners.
	 *
	 * @param parent the owning actions window used for UI coordination
	 * @param parentPanel the parent calibration panel associated with this actions panel
	 */
	public ActionsPanel(ActionsWindow parent, CalibrationPanel parentPanel) {
		// Initialize the JPanel with a GridBagLayout for flexible placement of controls
		super(new GridBagLayout());

		// Store references to the owning window and parent panel for later use
		_parent = parent;

		_parentPanel = parentPanel;

		// Build the action buttons and layout them within the panel
		buildControls();

		// Register project lifecycle listeners to manage action enablement
		addListeners();
	}



	/**
	 * Creates and adds the action controls to the panel using GridBagLayout.
	 *
	 * This method initializes actions and their corresponding buttons,
	 * configures layout constraints, and adds controls to the panel.
	 * Controls are added conditionally when appropriate flags are set.
	 */
	private void buildControls() {

		// Create the Update Data action for refreshing datasets
		_updateDataAction = new UpdateDataAction();

		// Create a Swing button bound to the Update Data action
		JButton button = new JButton(_updateDataAction);

		// Configure layout constraints for the Update Data button
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		// Conditionally add the Update Data button based on the DASH_D_FLAG
		if ( Boolean.getBoolean(UpdateDataAction.DASH_D_FLAG)) {
			add(button, gbc);
		}

		// Create the Review Data action for inspecting datasets
		_reviewDataAction = new ReviewDataAction();

		// Create a button bound to the Review Data action
		button = new JButton(_reviewDataAction);

		// Reconfigure layout constraints for the Review Data button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;

		// Add the Review Data button to the panel
		add(button, gbc);

		// Create the Edit Interactive Simulation action
		_editInterativeSimAction = new EditInterativeSimulationAction(_parent);

		// Use a drop button to attach a popup for related actions (e.g., viewing iteration results)
		JXDropButton jxbutton = new JXDropButton(_editInterativeSimAction);

		// Create a popup menu for the drop button
		JPopupMenu popup = new JPopupMenu();

		// Add the View Iteration Results action to the popup menu
		popup.add(new ViewIterationResultsAction(_parent));

		// Attach the popup menu to the drop button
		jxbutton.setPopupMenu(popup);

		// Configure layout constraints for the drop button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		// Add the drop button to the panel
		add(jxbutton, gbc);

		// Create the Post Results action for publishing outputs
		_postResultsAction = new PostResultsAction();

		// Create a button bound to the Post Results action
		button = new JButton(_postResultsAction);

		// Configure layout constraints for the Post Results button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		// Add the Post Results button to the panel
		add(button, gbc);

		// Create the About action for showing plugin information
		_aboutAction = new AboutAction();

		// Create a button bound to the About action
		button = new JButton(_aboutAction);

		// Configure layout constraints for the About button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.001;

		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		// Add the About button to the panel
		add(button, gbc);
	}

	/**
	 * Registers listeners that respond to project lifecycle events.
	 *
	 * Adds a static project listener to enable or disable actions when
	 * projects are opened or closed.
	 */
	protected void addListeners() {
		// Listen for project-level events to manage action enablement
		Project.getCurrentProject().addStaticProjectListener(new ProjectAdapter() {

			@Override
			public void projectClosed(ProjectEvent arg0) {
				// Clear the active simulation group reference
				setSimulationGroup(null);

				// Disable actions because there is no active simulation group
				enableActions(false);
			}

			@Override
			public void projectOpened(ProjectEvent arg0) {
				// Enable model update action when a project is opened
				// Note: this assumes _updateModelsAction is initialized elsewhere
				_updateModelsAction.setEnabled(true);
			}
		});
	}

	/**
	 * Sets the active simulation group and updates action enablement.
	 *
	 * @param sg the simulation group to use, or null to clear and disable actions
	 */
	public void setSimulationGroup(AbstractSimulationGroup sg) {
		// Actions are enabled when a non-null simulation group is present
		boolean enabled = sg != null;

		// Apply enablement to all relevant actions
		enableActions(enabled);
	}

	/**
	 * Enables or disables actions based on the provided flag.
	 *
	 * @param enabled true to enable user actions, false to disable them
	 */
	protected void enableActions(boolean enabled) {
		// Review Data action availability follows the enabled flag
		_reviewDataAction.setEnabled(enabled);

		// Edit Interactive Simulation action availability follows the enabled flag
		_editInterativeSimAction.setEnabled(enabled);

		// Update Data action availability follows the enabled flag
		_updateDataAction.setEnabled(enabled);
	}
}