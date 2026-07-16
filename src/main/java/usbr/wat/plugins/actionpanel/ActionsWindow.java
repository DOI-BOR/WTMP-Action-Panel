package usbr.wat.plugins.actionpanel;

import java.awt.Component;                                                              // AWT UI component base type used for parent references and container operations
import java.awt.Cursor;                                                                 // AWT cursor utility used to display wait and default cursors during long operations
import java.awt.Desktop;                                                                // Desktop integration API used to open files with the system default application
import java.awt.EventQueue;                                                             // AWT event dispatch utility to schedule tasks on the Event Dispatch Thread (EDT)
import java.awt.Frame;                                                                  // AWT top-level window type used as the parent for this dialog
import java.awt.GridBagConstraints;                                                     // Layout constraints object for positioning components in a grid-based layout
import java.awt.GridBagLayout;                                                          // Grid-based layout manager for arranging components in rows and columns

import java.io.File;                                                                    // File I/O type representing filesystem paths used to open reports
import java.io.IOException;                                                             // Exception type for I/O failures when opening files via the desktop

import java.util.List;                                                                  // Collections interface used for lists of simulations and results

import javax.swing.JFrame;                                                              // Swing top-level window used for test launching and parent passing

import javax.swing.JOptionPane;                                                         // Swing utility for showing information and confirmation dialogs
import javax.swing.JTabbedPane;                                                         // Swing tabbed container used to host calibration and forecast panels

import com.rma.client.Browser;                                                          // Host application's main browser frame for look and feel and docking behavior
import com.rma.client.LookAndFeel;                                                      // Host application's look-and-feel utility used to set UI theme
import com.rma.event.ProjectAdapter;                                                    // Listener adapter with default implementations for project lifecycle events
import com.rma.event.ProjectEvent;                                                      // Event object representing changes in the project lifecycle
import com.rma.event.ProjectManagerListener;                                            // Listener interface for manager add/delete events tied to a project
import com.rma.factories.DeleteManagerFactory;                                          // Factory providing deletion operations and utilities for managers (import may be used indirectly)
import com.rma.factories.ProjectNodeFactory;                                            // Factory used to register object-to-node mappings for the project tree
import com.rma.model.ManagerProxy;                                                      // Proxy wrapper that exposes manager instances and metadata
import com.rma.model.Project;                                                           // Accessor for the current project and project-level operations
import com.rma.util.PlugInLoader;                                                       // Plugin loader utility for dynamically discovering and initializing plugins

import hec2.wat.model.WatAnalysisPeriod;                                                // WAT model type representing the analysis period associated with a simulation group
import hec2.wat.model.WatSimulation;                                                    // WAT model type representing a single simulation scenario or run

import rma.swing.RmaInsets;                                                             // Standardized insets utility for consistent component padding and spacing
import rma.swing.RmaJDialog;                                                            // Base dialog class with RMA-specific behaviors used for plugin windows

import usbr.wat.plugins.actionpanel.actions.DeleteSimulationGroupAction;                // Action that deletes a simulation group and coordinates UI updates
import usbr.wat.plugins.actionpanel.actions.forecast.DeleteForecastSimGroupAction;      // Action for deleting forecast simulation groups (import may be used by forecast UI components)
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;                  // Utility for checking repository status and out-of-date conditions relative to Git
import usbr.wat.plugins.actionpanel.listener.AnalysisPeriodRenameListener;              // Listener that tracks and applies changes when analysis periods are renamed
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;                      // Base type representing a simulation group used by the actions window
import usbr.wat.plugins.actionpanel.model.MissingManagersChecker;                       // Utility that checks for required managers and reports missing ones
import usbr.wat.plugins.actionpanel.model.ResultsData;                                  // Data model representing results entries available for selection
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                              // Concrete type representing a simulation group managed within the plugin
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                    // Forecast-specific simulation group type used by the forecast panel
import usbr.wat.plugins.actionpanel.ui.ActionsProjectTab;                               // Project tab that surfaces WTMP workflow actions within the host application
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;                        // Panel exposing common simulation-group functionality and flags
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;                                // Panel for prescribed conditions workflows including data review and simulation editing
import usbr.wat.plugins.actionpanel.ui.SimulationGroupNode;                             // Node type used in the project tree to represent a simulation group
import usbr.wat.plugins.actionpanel.ui.forecast.ForecastPanel;                          // Panel for forecast conditions workflows including forecast-specific simulations
import usbr.wat.plugins.actionpanel.ui.planning.PlanningPanel;                          // Panel for the Planning workflow, hosting the Set/Simulation Group pairing and its six sub-tabs

/**
 * Main window for the WTMP Actions plugin.
 *
 * Hosts tabbed panels for prescribed and forecast workflows, integrates with
 * the project lifecycle, manages simulation group selection, and coordinates
 * repository status checks and plugin loading.
 */
@SuppressWarnings("serial")
public class ActionsWindow extends RmaJDialog {
	// Static initialization registers the SimulationGroup node mapping and sets system properties
	static {
		// Map SimulationGroup objects to SimulationGroupNode in the project tree
		ProjectNodeFactory.addObjectToNodeMapping(SimulationGroup.class, SimulationGroupNode.class);

		// Use simulation names for the runs folder to improve clarity
		System.setProperty("UseSimNameInRunsFolder", "true");

		// Allow simulations to exceed analysis periods when necessary
		System.setProperty("SimNode.AllowSimsToExceedAPs", "true");
	}


	// Tabs container holding calibration and forecast panels
	private JTabbedPane _tabbedPane;

	// Currently selected simulation group, if any
	private SimulationGroup _sg;

	// Listener for WatSimulation add/delete events
	private ProjectSimulationListener _projectSimulationListener;

	// Actions tab integrated into the project pane
	private ActionsProjectTab _actionsProjTab;

	// Listener for SimulationGroup add/delete events
	private ProjectSimulationGroupListener _projectSimulationGroupListener;

	// Panel for prescribed conditions workflows
	private CalibrationPanel _calibrationPanel;

	// Panel for forecast conditions workflows
	private ForecastPanel _forecastPanel;

	// Panel for the Planning workflow
	private PlanningPanel _planningPanel;

	// Listener that handles renames of analysis periods within the project
	private AnalysisPeriodRenameListener _analysisPeriodListener;

	/**
	 * Constructs the actions window and initializes UI, listeners, and plugins.
	 *
	 * @param parent the parent frame used for modality and positioning
	 */
	public ActionsWindow(Frame parent) {
		// Initialize the base RmaJDialog with the given parent
		super(parent);

		// Prevent closing via system decorations to ensure proper workflow handling
		setSystemClosable(false);

		// Build the panels, tabs, and layout
		buildControls();

		// Register project lifecycle listeners
		addListeners();

		// Load any dependent plugins required by this window
		loadPlugins();

		// Size the dialog to fit components and set preferred dimensions
		pack();

		setSize(1000, 700);

		// Center the window relative to the main browser frame
		setLocationRelativeTo(Browser.getBrowserFrame());

		// Insert the WTMP tab into the application's project pane
		addTabToProjectPane();
	}

	/**
	 * Builds the controls and layout for the actions window.
	 *
	 * Initializes the tabbed pane, adds calibration and forecast panels,
	 * and applies layout constraints using GridBagLayout.
	 */
	private void buildControls() {
		// Title shown in the window's caption
		setTitle("WTMP Actions Window");

		// Use a grid bag layout for flexible panel placement
		getContentPane().setLayout(new GridBagLayout());

		// Initialize the tabbed pane for switching between workflows
		_tabbedPane = new JTabbedPane ();

		// Layout constraints for placing the tabbed pane
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;

		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;

		// Add the tabbed pane to the dialog content area
		getContentPane().add(_tabbedPane, gbc);

		// Create and add the prescribed conditions panel
		_calibrationPanel = new CalibrationPanel(this);
		_tabbedPane.addTab("Prescribed Conditions", _calibrationPanel);

		// Create and add the forecast conditions panel
		_forecastPanel = new ForecastPanel(this);
		_tabbedPane.addTab("Forecast Conditions", _forecastPanel);

		// Create and add the planning workflow panel
		_planningPanel = new PlanningPanel(this);
		_tabbedPane.addTab("Planning", _planningPanel);
	}

	/**
	 * Returns the calibration panel used for prescribed conditions.
	 *
	 * @return the calibration panel
	 */
	public CalibrationPanel getCalibrationPanel()
	{
		return _calibrationPanel;
	}

	/**
	 * Returns the forecast panel used for forecast workflows.
	 *
	 * @return the forecast panel
	 */
	public ForecastPanel getForecastPanel()
	{
		return _forecastPanel;
	}

	/**
	 * Returns the planning panel used for the Planning workflow.
	 *
	 * @return the planning panel
	 */
	public PlanningPanel getPlanningPanel()
	{
		return _planningPanel;
	}

	/**
	 * Inserts the WTMP tab into the host application's project pane.
	 *
	 * Adds the actions project tab at a fixed index for consistent placement.
	 */
	private void addTabToProjectPane() {
		// Create the actions project tab and insert it into the main tabbed pane
		_actionsProjTab = new ActionsProjectTab();

		Browser.getBrowserFrame().getTabbedPane().insertTab("WTMP", null, _actionsProjTab, "WTMP Tab", 1);
	}

	/**
	 * Returns the actions project tab associated with this window.
	 *
	 * @return the actions project tab
	 */
	public ActionsProjectTab getProjectTab()
	{
		return _actionsProjTab;
	}

	/**
	 * Shows the selected item in the project tree.
	 *
	 * Note: The implementation is currently a placeholder and does not invoke tree selection.
	 */
	public void showInProjectTreeAction() {
		// Determine which tab is active
		Component comp = _tabbedPane.getSelectedComponent();

		// Placeholder for a future "show in project tree" implementation
		// comp.showInProjectTreeAction();
	}


	/**
	 * Loads dependent plugins required by the actions window.
	 *
	 * Initializes the report plugin to support reporting features.
	 */
	private void loadPlugins() {
		// Load the report plugin used by WTMP components
		PlugInLoader.loadPlugIns("ReportPlugin");
	}

	/**
	 * Opens a file path in the system's default application if supported.
	 *
	 * @param rptFile path to the report file to display
	 */
	public  void displayFile(String rptFile) {
		// Verify desktop integration is available
		if ( Desktop.isDesktopSupported()) {
			// Resolve the provided path to a file
			File f = new File(rptFile);

			// Proceed only if the file exists
			if ( f.exists()) {
				try {
					// Use the desktop API to open the file
					Desktop.getDesktop().open(f);

				} catch (IOException e) {
					// Log the exception and show a friendly message to the user
					e.printStackTrace();

					JOptionPane.showMessageDialog(this, "<html>Error displaying the report at "
							+ rptFile +"<br> Error:"+e.getMessage(), "Error", JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				// Inform the user the report has not been generated yet
				JOptionPane.showMessageDialog(this, "The report doesn't exist.  Please create the report first",
						"No Report", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * Registers listeners that respond to project lifecycle and manager events.
	 *
	 * Adds static containers, wires manager listeners, checks for missing managers,
	 * and triggers repository status checks when projects open or close.
	 */
	private void addListeners() {
		// Register the simulation group container node with the project (class in same package)
		Project.addStaticManagerContainer(SimGroupContainerNode.class);

		// Create listeners that respond to simulation and simulation-group manager changes
		_projectSimulationListener = new ProjectSimulationListener();

		_projectSimulationGroupListener = new ProjectSimulationGroupListener();

		// Listen for project open/close events to manage UI and state
		Project.addStaticProjectListener(new ProjectAdapter() {
			@Override
			public void projectLoaded(ProjectEvent e) {
				// No action taken on projectLoaded for this window
			}

			@Override
			public void projectOpened(ProjectEvent e ) {
				// Retrieve the opened project
				Project prj = e.getProject();

				// Avoid operations when a "no project" placeholder is active
				if ( !prj.isNoProject()) {
					// Start listening for analysis period rename events
					startAnalysisPeriodRenameListener(prj);

					// Check whether any required managers are missing
					checkForMissingManagers(e.getProject());

					// Listen for manager add/delete events related to simulations and groups
					prj.addManagerListener(_projectSimulationListener);

					prj.addManagerListener(_projectSimulationGroupListener);
				}

				// Reset the form state and UI selections
				clearForm();

				// Check repository status to determine if the local repo is out-of-date
				checkRepoOutofDateStatus();
			}

			@Override
			public void  projectClosed(ProjectEvent e ) {
				// Clear UI state when the project is closed
				clearForm();

				// Retrieve the closed project
				Project prj = e.getProject();

				// Stop listening to analysis period rename events
				stopAnalysisPeriodRenameListener();

				// Remove manager listeners to avoid leaks and stale callbacks
				e.getProject().removeManagerListener(_projectSimulationListener);

				e.getProject().removeManagerListener(_projectSimulationGroupListener);
			}
		});
	}

	/**
	 * Stops the analysis period rename listener if it is active.
	 */
	private void stopAnalysisPeriodRenameListener() {
		// Safely stop listening to rename events
		if ( _analysisPeriodListener != null ) {
			_analysisPeriodListener.stopListening();
		}

		// Clear the reference
		_analysisPeriodListener = null;
	}

	/**
	 * Starts the analysis period rename listener for the given project.
	 *
	 * @param prj the project to monitor for analysis period rename events
	 */
	private void startAnalysisPeriodRenameListener(Project prj) {
		// Restart listener if one is already active
		if ( _analysisPeriodListener != null ) {
			stopAnalysisPeriodRenameListener();
		}

		// Create and register a new listener
		_analysisPeriodListener = new AnalysisPeriodRenameListener(prj);
	}

	/**
	 * Checks the current project for required managers and reports if any are missing.
	 *
	 * @param project the project to validate
	 */
	protected void checkForMissingManagers(Project project) {
		// Instantiate the checker and run the validation
		MissingManagersChecker checker = new MissingManagersChecker();

		checker.checkForMissingManagers(project);
	}



	/**
	 * Clears the window form and delegates clearing to the calibration panel.
	 */
	@Override
	public void clearForm() {
		// Clear base dialog state
		super.clearForm();

		// Clear the calibration panel selections and fields
		_calibrationPanel.clearForm();
	}

	/**
	 * Schedules a check to determine if the local repository is out-of-date.
	 *
	 * This is invoked on the Event Dispatch Thread to avoid blocking the UI.
	 */
	protected void checkRepoOutofDateStatus() {
		// Defer Git status check to the EDT
		EventQueue.invokeLater(()->GitRepoUtils.checkRepoOutofDateStatus(Project.getCurrentProject().getProjectDirectory()));
	}


	/**
	 * Launches the window standalone for testing or demonstration.
	 *
	 * @param args command-line arguments (unused)
	 */
	public static void main(String[] args) {
		// Apply the application's look and feel
		LookAndFeel.setLookAndFeel();

		// Create and show the actions window with a simple JFrame parent
		new ActionsWindow(new JFrame()).setVisible(true);
	}

	/**
	 * Sets the active simulation group in the calibration panel.
	 *
	 * Shows a wait cursor during updates and restores the default cursor afterward.
	 *
	 * @param sg the simulation group to activate
	 */
	public void setSimulationGroup(SimulationGroup sg) {
		// Indicate work in progress to the user
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Clear the calibration panel form and set the new simulation group
			_calibrationPanel.clearForm();

			_calibrationPanel.setSimulationGroup(sg);
		} finally {
			// Restore the default cursor regardless of success or failure
			setCursor(Cursor.getDefaultCursor());
		}
	}



	/**
	 * Appends a status or informational message to the window's message area.
	 *
	 * Currently a placeholder; the actual list model updates are commented out.
	 *
	 * @param message the message to add, or null for an empty line
	 */
	public void addMessage(String message) {
		if ( message == null ) {
			// Placeholder for adding an empty line to a status list
			// ((RmaListModel)_statusList.getModel()).addElement("");

		} else {
			// Placeholder for adding a message to a status list
			// ((RmaListModel)_statusList.getModel()).addElement(message);
		}
	}

	/**
	 * Returns the simulations selected in the currently active panel.
	 *
	 * @return a list of selected simulations, or null if none are selected
	 */
	public List<WatSimulation> getSelectedSimulations() {
		// Identify which tab is currently active
		Component comp = _tabbedPane.getSelectedComponent();

		if ( comp == _calibrationPanel ) {
			// Delegate to the calibration panel when it is active
			return _calibrationPanel.getSelectedSimulations();

		} else if ( comp == _forecastPanel ) {
			// Delegate to the forecast panel when it is active
			return _forecastPanel.getSelectedSimulations();
		}

		// No active panel providing simulations
		return null;
	}

	/**
	 * Returns the results selected in the currently active panel.
	 *
	 * @return a list of selected results, or null if none are selected
	 */
	public List<ResultsData> getSelectedResults() {
		// Identify which tab is currently active
		Component comp = _tabbedPane.getSelectedComponent();

		if ( comp == _calibrationPanel ) {
			// Delegate to the calibration panel when it is active
			return _calibrationPanel.getSelectedResults();

		} else if ( comp == _forecastPanel ) {
			// Delegate to the forecast panel when it is active
			return _forecastPanel.getSelectedResults();
		}

		// No active panel providing results
		return null;
	}

	/**
	 * Returns the active simulation group from the currently selected panel.
	 *
	 * @return the active simulation group, or null if none is selected
	 */
	public AbstractSimulationGroup getSimulationGroup() {
		// Identify which tab is currently active
		Component comp = _tabbedPane.getSelectedComponent();

		if ( comp == _calibrationPanel ) {
			// Delegate to the calibration panel when it is active
			return _calibrationPanel.getSimulationGroup();

		} else if ( comp == _forecastPanel ) {
			// Delegate to the forecast panel when it is active
			return _forecastPanel.getSimulationGroup();
		}

		// No active panel providing a simulation group
		return null;
	}

	/**
	 * Returns the analysis period for the current simulation group, if any.
	 *
	 * @return the analysis period, or null if no simulation group is set
	 */
	public WatAnalysisPeriod getAnalysisPeriod() {
		// Return the analysis period when a simulation group is present
		if ( _sg != null ) {
			return _sg.getAnalysisPeriod();
		}

		// No simulation group means no analysis period is available
		return null;
	}

	/**
	 * Listener for SimulationGroup manager events.
	 *
	 * Responds to deletion events by clearing the active selection and
	 * invoking the delete action to remove the group from the project.
	 */
	public class ProjectSimulationGroupListener implements ProjectManagerListener {

		/**
		 * Creates a new listener for simulation group events.
		 */
		public ProjectSimulationGroupListener()
		{
			super();
		}

		/**
		 * Invoked when a manager is added.
		 *
		 * No action is taken for simulation group additions in this window.
		 *
		 * @param proxy the manager proxy that was added
		 */
		public void managerAdded(ManagerProxy proxy) {
			// Do nothing on addition
		}

		/**
		 * Returns the manager class this listener handles.
		 *
		 * @return the SimulationGroup manager class
		 */
		@Override
		public Class<?> getManagerClass()
		{
			return SimulationGroup.class;
		}

		/**
		 * Invoked when a manager is deleted.
		 *
		 * Clears the active simulation group if it matches the deleted manager
		 * and triggers the delete action to remove the group from the project.
		 *
		 * @param proxy the manager proxy that was deleted
		 */
		@Override
		public void managerDeleted(ManagerProxy proxy) {
			// Ignore null events
			if ( proxy == null ) {
				return;
			}

			// Retrieve the deleted simulation group
			SimulationGroup simGroup = (SimulationGroup) proxy.getManager();

			// If the deleted manager is currently selected, clear the selection
			if ( proxy.getManager()==getSimulationGroup() ) {
				setSimulationGroup(null);
			}

			// Perform deletion via the corresponding action handler
			new DeleteSimulationGroupAction(null).deleteSimulationGroup(proxy);
		}
	}

	/**
	 * Listener for WatSimulation manager events.
	 *
	 * Responds to deletion events by removing simulations from the active group,
	 * updating the calibration panel, and optionally prompting to delete an empty group.
	 */
	public class ProjectSimulationListener implements ProjectManagerListener {
		/**
		 * Creates a new listener for simulation events.
		 */
		public ProjectSimulationListener()
		{
			super();
		}

		/**
		 * Invoked when a manager is added.
		 *
		 * No action is taken for simulation additions in this window.
		 *
		 * @param proxy the manager proxy that was added
		 */
		@Override
		public void managerAdded(ManagerProxy proxy) {
			// Do nothing on addition
		}

		/**
		 * Invoked when a manager is deleted.
		 *
		 * Removes the deleted simulation from the active group, marks the group modified,
		 * refreshes the table, and prompts the user to delete the group if it becomes empty.
		 *
		 * @param proxy the manager proxy that was deleted
		 */
		@Override
		public void managerDeleted(ManagerProxy proxy) {
			// Ignore null events
			if ( proxy == null ) {
				return;
			}

			// Name of the deleted simulation
			String name = proxy.getName();

			// Retrieve the active simulation group
			AbstractSimulationGroup simGroup = getSimulationGroup();

			// Local variable for iteration over simulations
			WatSimulation sim;

			// Only proceed when a simulation group is active
			if ( simGroup != null ) {
				boolean deleted = false;

				// Iterate through simulations to find the one that matches the deleted manager
				List<WatSimulation> sims = simGroup.getSimulations();

				for (int i = 0;i < sims.size(); i++ ){
					sim = sims.get(i);

					if ( name.equals(sim.getName())) {
						// Remove the simulation from the group
						simGroup.removeSimulation(sim);

						// Mark the group as modified to reflect changes
						simGroup.setModified(true);
						deleted = true;
						break;
					}
				}

				// If a simulation was removed, update the calibration panel and consider group deletion
				if ( deleted ) {
					// Refresh the simulation table to reflect changes
					_calibrationPanel.setSimulationTable(simGroup);

					// If the group's proxy exists and there are no simulations left, prompt to delete the group
					if ( Project.getCurrentProject().getManagerProxy(simGroup) != null && simGroup.getSimulations().isEmpty() ) {
						String msg = "There are no more simulations in the Simulation Group.  Would you like to delete the Simulation Group?";

						String title = "Delete Simulation Group?";

						int opt = JOptionPane.showConfirmDialog(ActionsWindow.this, msg, title, JOptionPane.YES_NO_OPTION);

						if ( opt == JOptionPane.YES_OPTION ) {
							// Delete the simulation group and clear current selection
							if ( new DeleteSimulationGroupAction(null).deleteSimulationGroup(simGroup)) {
								setSimulationGroup(null);
							}
						}
					}
				}
			}
		}

		/**
		 * Returns the manager class this listener handles.
		 *
		 * @return the WatSimulation manager class
		 */
		@Override
		public Class<?> getManagerClass() {
			return WatSimulation.class;
		}
	}
}