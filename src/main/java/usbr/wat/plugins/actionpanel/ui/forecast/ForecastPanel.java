package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.Component;                                                      // Provides Component as the generic type returned by JTabbedPane.getSelectedComponent()
import java.awt.GridBagConstraints;                                             // Provides GridBagConstraints for specifying layout parameters within the GridBagLayout
import java.awt.GridBagLayout;                                                  // Provides GridBagLayout as the layout manager for this panel

import java.util.List;                                                          // Provides the List interface for ordered collections of simulations and results data

import javax.swing.JTabbedPane;                                                 // Provides JTabbedPane for the multi-tab forecast data navigation area

import com.rma.model.ManagerProxy;                                              // Provides ManagerProxy as the handle passed to the simulation-group-deleted notification
import hec2.wat.model.WatSimulation;                                            // Provides WatSimulation for representing the currently selected WAT simulation

import rma.swing.RmaInsets;                                                     // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJPanel;                                                     // Provides RmaJPanel as the base Swing panel class this component extends

import usbr.wat.plugins.actionpanel.ActionsWindow;                              // Provides ActionsWindow as the parent window that hosts this forecast panel
import usbr.wat.plugins.actionpanel.model.ResultsData;                          // Provides ResultsData for returning the list of selected simulation results
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Provides ForecastSimGroup as the top-level data container for all forecast data
import usbr.wat.plugins.actionpanel.ui.SimulationGroupPanel;                    // Provides SimulationGroupPanel for the simulation group selection combo box above the tabs
import usbr.wat.plugins.actionpanel.ui.forecast.temptarget.TempTargetPanel;     // Provides TempTargetPanel as the temperature targets tab panel

/**
 * The top-level forecast panel that hosts all forecast data entry and review tabs
 * within the WTMP action panel UI.
 *
 * {@code ForecastPanel} extends {@link RmaJPanel} and acts as the central coordinator
 * for all forecast-related sub-panels. It consists of:
 *
 *   A {@link SimulationGroupPanel} above the tabs that provides a combo box for
 *       selecting the active forecast simulation group.
 *   A {@link JTabbedPane} containing six tabs: Initial Conditions, Operations,
 *       Meteorology, Boundary Conditions, Temperature Targets, and Simulation.
 *
 * The tab placement (left, right, top, or bottom) is configurable via the JVM system
 * property {@code WTMP.ForecastTabs.Placement}; it defaults to {@code LEFT} if the
 * property is absent or unrecognised.
 *
 * When the active tab changes, the previously active panel's state is saved and the
 * newly active panel is notified via {@link AbstractForecastPanel#panelActivated()}.
 * When a new {@link ForecastSimGroup} is set, all sub-panels are populated with its
 * data. Setting the group to {@code null} clears all panels.
 *
 * @see AbstractForecastPanel
 * @see SimulationGroupPanel
 * @see ForecastSimGroup
 */
public class ForecastPanel extends RmaJPanel {
	// The parent ActionsWindow that hosts this forecast panel
	private ActionsWindow _parent;

	// The simulation group selection panel displayed above the tabbed pane
	private SimulationGroupPanel _simGroupPanel;

	// The tabbed pane containing all six forecast sub-panel tabs
	private JTabbedPane _tabbedPane;

	// The Initial Conditions tab panel
	private InitialConditionsPanel _initialConditionsPanel;

	// The Operations tab panel
	private OperationsPanel _operationsPanel;

	// The Meteorology tab panel
	private MeteorologyPanel _metPanel;

	// The Boundary Conditions tab panel
	private BcPanel _bcPanel;

	// The Temperature Targets tab panel
	private TempTargetPanel _tempTargetsPanel;

	// The Simulation tab panel
	private SimulationPanel _simulationPanel;

	// The currently active forecast simulation group; null when no group is selected
	private ForecastSimGroup _simGroup;

	// The AbstractForecastPanel tab that is currently selected; used to save state on tab switch
	private AbstractForecastPanel _currentPanel;

	/**
	 * Constructs a {@code ForecastPanel}, lays it out with a {@link GridBagLayout},
	 * stores the parent {@link ActionsWindow} reference, builds all sub-panel controls,
	 * and wires the tab change listener.
	 *
	 * @param parent the {@link ActionsWindow} that owns and hosts this panel;
	 *               must not be {@code null}
	 */
	public ForecastPanel(ActionsWindow parent) {
		// Initialise the base RmaJPanel with a GridBagLayout
		super(new GridBagLayout());

		// Store the parent reference for use by sub-panels that need the actions window
		_parent = parent;

		// Build and lay out all sub-panels and the tabbed pane
		buildControls();

		// Attach the tab change listener
		addListeners();
	}

	/**
	 * Builds and lays out all Swing controls within this panel.
	 *
	 * Constructs all six sub-panels (all initially disabled), wraps the simulation
	 * panel in a {@link SimulationGroupPanel}, adds the group panel above a
	 * {@link JTabbedPane}, and adds each sub-panel as a named tab. The tab placement
	 * is read from the {@code WTMP.ForecastTabs.Placement} system property and
	 * defaults to {@link JTabbedPane#LEFT} if unset or unrecognised.
	 */
	private void buildControls() {
		// Instantiate all six forecast sub-panels
		_simulationPanel = new SimulationPanel(_parent, this);
		_initialConditionsPanel = new InitialConditionsPanel(this);
		_operationsPanel = new OperationsPanel(this);
		_metPanel = new MeteorologyPanel(this);
		_tempTargetsPanel = new TempTargetPanel(this);
		_bcPanel = new BcPanel(this);

		// Disable all sub-panels until a simulation group is loaded
		_simulationPanel.setEnabled(false);
		_initialConditionsPanel.setEnabled(false);
		_operationsPanel.setEnabled(false);
		_metPanel.setEnabled(false);
		_tempTargetsPanel.setEnabled(false);
		_bcPanel.setEnabled(false);

		// Wrap the simulation panel in the group selection combo box panel
		_simGroupPanel = new SimulationGroupPanel(_simulationPanel);

		// Add the simulation group panel at the top; it does not claim vertical space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simGroupPanel, gbc);

		// Create the tabbed pane and configure its tab placement from the system property
		_tabbedPane = new JTabbedPane();
		String pos = System.getProperty("WTMP.ForecastTabs.Placement");

		// Default to LEFT placement; override if a recognised value is specified
		int tabPlacement = JTabbedPane.LEFT;
		if ("left".equalsIgnoreCase(pos)) {
			tabPlacement = JTabbedPane.LEFT;

		} else if ("right".equalsIgnoreCase(pos)) {
			tabPlacement = JTabbedPane.RIGHT;

		} else if ("bottom".equalsIgnoreCase(pos)) {
			tabPlacement = JTabbedPane.BOTTOM;

		} else if ("top".equalsIgnoreCase(pos)) {
			tabPlacement = JTabbedPane.TOP;
		}

		_tabbedPane.setTabPlacement(tabPlacement);

		// Add the tabbed pane below the group panel; it claims all remaining vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);

		// Register each sub-panel as a named tab in display order
		_tabbedPane.addTab("Initial Conditions", _initialConditionsPanel);
		_tabbedPane.addTab("Operations", _operationsPanel);
		_tabbedPane.addTab("Meteorology", _metPanel);
		_tabbedPane.addTab("Boundary Conditions", _bcPanel);
		_tabbedPane.addTab("Temperature Targets", _tempTargetsPanel);
		_tabbedPane.addTab("Simulation", _simulationPanel);

		// Capture the initially selected tab as the current panel
		_currentPanel = (AbstractForecastPanel) _tabbedPane.getSelectedComponent();
	}

	/**
	 * Registers the tab change listener that saves the previously active panel's state
	 * and activates the newly selected panel when the user switches tabs.
	 */
	private void addListeners() {
		// Delegate all tab selection changes to the tabSelectionChanged handler
		_tabbedPane.addChangeListener(e -> tabSelectionChanged());
	}

	/**
	 * Handles tab selection changes in the tabbed pane.
	 *
	 * When the newly selected component is an {@link AbstractForecastPanel}:
	 *
	 *   Saves the previously active panel's state via {@link AbstractForecastPanel#savePanel()}.
	 *   Notifies the newly selected panel via {@link AbstractForecastPanel#panelActivated()} so it can update the
	 *       enabled/highlighted state of the shared upper tables.
	 *   Updates {@code _currentPanel} to the new panel.
	 *
	 * Non-{@code AbstractForecastPanel} components (if any) are ignored.
	 */
	private void tabSelectionChanged() {
		Component comp = _tabbedPane.getSelectedComponent();

		if (comp instanceof AbstractForecastPanel) {
			// Save the previously active panel's state before switching away from it
			if (_currentPanel != null) {
				_currentPanel.savePanel();
			}

			// Notify the newly selected panel so it updates its enabled table highlight
			AbstractForecastPanel panel = (AbstractForecastPanel) comp;
			panel.panelActivated();

			// Track the newly active panel for the next tab switch
			_currentPanel = panel;
		}
	}

	/**
	 * Returns the currently active {@link ForecastSimGroup}, or {@code null} if no
	 * simulation group has been selected.
	 *
	 * @return the active {@link ForecastSimGroup}, or {@code null}
	 */
	public ForecastSimGroup getSimulationGroup() {
		return _simGroup;
	}

	/**
	 * Sets the active {@link ForecastSimGroup} and propagates it to all sub-panels.
	 *
	 * If {@code fsg} is non-null, all sub-panels are populated with its data. If
	 * {@code fsg} is {@code null}, all sub-panels are cleared and
	 * {@link #clearPanel()} is called to reset all lower-panel controls.
	 *
	 * @param fsg the {@link ForecastSimGroup} to display, or {@code null} to clear
	 *            all panels
	 */
	public void setSimulationGroup(ForecastSimGroup fsg) {
		_simGroup = fsg;

		if (fsg != null) {
			// Populate every sub-panel with the new simulation group's data
			_simGroupPanel.setSimulationGroup(fsg);
			_simulationPanel.setSimulationGroup(fsg, false);
			_initialConditionsPanel.setSimulationGroup(fsg);
			_operationsPanel.setSimulationGroup(fsg);
			_metPanel.setSimulationGroup(fsg);
			_tempTargetsPanel.setSimulationGroup(fsg);
			_bcPanel.setSimulationGroup(fsg);

		} else {
			// Clear all sub-panels by passing null as the simulation group
			_simulationPanel.setSimulationGroup(null, false);
			_initialConditionsPanel.setSimulationGroup(null);
			_operationsPanel.setSimulationGroup(null);
			_metPanel.setSimulationGroup(null);
			_tempTargetsPanel.setSimulationGroup(null);
			_bcPanel.setSimulationGroup(null);

			// Reset all lower-panel controls to their empty state
			clearPanel();
		}
	}

	/**
	 * Resets the lower-panel controls of all sub-panels and clears the shared upper
	 * tables of the currently active panel.
	 *
	 * Called when the simulation group is set to {@code null} or when the panel
	 * becomes visible with no simulation group loaded.
	 */
	private void clearPanel() {
		// Reset every sub-panel's lower-panel content to its empty default state
		_initialConditionsPanel.clearPanel();
		_operationsPanel.clearPanel();
		_metPanel.clearPanel();
		_bcPanel.clearPanel();
		_tempTargetsPanel.clearPanel();

		// Clear all rows from the shared upper tables of the currently active panel
		_currentPanel.clearTables();
	}

	/**
	 * Reloads the simulation group combo box in the {@link SimulationGroupPanel} to
	 * reflect any changes in the available simulation groups.
	 */
	public void loadSimulationGroupCombo() {
		_simGroupPanel.loadSimulationGroupCombo();
	}

	/**
	 * Programmatically selects the tab that hosts the given {@link AbstractForecastPanel},
	 * switching the visible tab to that panel.
	 *
	 * Does nothing if {@code panel} is {@code null}.
	 *
	 * @param panel the {@link AbstractForecastPanel} tab to select; must be one of the
	 *              panels registered as a tab in the tabbed pane
	 */
	public void setSelectedTab(AbstractForecastPanel panel) {
		if (panel != null) {
			// Switch the tabbed pane's selection to the specified panel component
			_tabbedPane.setSelectedComponent(panel);
		}
	}

	/**
	 * Returns the list of {@link WatSimulation} instances currently selected in the
	 * Simulation tab's simulation table.
	 *
	 * @return a {@link List} of selected {@link WatSimulation} objects; may be empty
	 * but never {@code null}
	 */
	public List<WatSimulation> getSelectedSimulations() {
		return _simulationPanel.getSelectedSimulations();
	}

	/**
	 * Returns the list of {@link ResultsData} items currently selected in the
	 * Simulation tab's results table.
	 *
	 * @return a {@link List} of selected {@link ResultsData} objects; may be empty
	 * but never {@code null}
	 */
	public List<ResultsData> getSelectedResults() {
		return _simulationPanel.getSelectedResults();
	}

	/**
	 * Returns the {@link SimulationPanel} that is hosted in the Simulation tab.
	 *
	 * @return the {@link SimulationPanel}; never {@code null} after construction
	 */
	public SimulationPanel getSimulationPanel() {
		return _simulationPanel;
	}

	/**
	 * Notifies the {@link SimulationGroupPanel} that the simulation group represented
	 * by the given {@link ManagerProxy} has been deleted, so it can update the combo
	 * box accordingly.
	 *
	 * @param proxy the {@link ManagerProxy} representing the deleted simulation group
	 */
	public void simulationGroupDeleted(ManagerProxy proxy) {
		_simGroupPanel.simulationGroupDeleted(proxy);
	}

	/**
	 * Returns the single {@link WatSimulation} currently highlighted (selected) in the
	 * Simulation tab's simulation table.
	 *
	 * @return the highlighted {@link WatSimulation}, or {@code null} if none is selected
	 */
	public WatSimulation getSelectedSimulation() {
		return _simulationPanel.getSelectedSimulation();
	}

	/**
	 * Refreshes the Simulation tab's table, ensemble set list, and analysis window
	 * after a change to the given {@link ForecastSimGroup}'s ensemble sets, then
	 * re-enables the panel and restores the simulation table selection.
	 *
	 * Called after a boundary condition set deletion or other operation that causes
	 * ensemble sets to be added or removed as a side effect.
	 *
	 * @param fsg the {@link ForecastSimGroup} whose updated ensemble sets should be
	 *            reflected in the Simulation tab
	 */
	public void refreshSimulationPanel(ForecastSimGroup fsg) {
		// Capture the currently highlighted simulation before the table is refreshed
		WatSimulation simulation = getSelectedSimulation();

		// Reload the simulation table rows from the updated simulation group data
		_simulationPanel.fillSimulationTable();

		// Update the ensemble set list for the currently selected simulation
		_simulationPanel.setEnsembleSets(fsg.getEnsembleSets(simulation));

		// Refresh the analysis window to reflect the updated ensemble set state
		_simulationPanel.fillAnalysisWindow();

		// Re-enable the panel now that the refresh is complete
		setEnabled(true);

		// Restore the previously highlighted row in the simulation table
		_simulationPanel.refreshSimTableSelection();
	}

	/**
	 * Extends the superclass visibility handling to clear all panels and notify the
	 * simulation panel of a closing event when this panel becomes visible with no
	 * simulation group loaded.
	 *
	 * This guards against the panel being shown in a stale state after a project close
	 * or simulation group removal.
	 *
	 * @param visible {@code true} to show the panel; {@code false} to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible && _simGroup == null) {
			// No simulation group is loaded; reset all panels to their empty state
			clearPanel();

			// Notify the simulation panel that it is closing so it can clean up
			_simulationPanel.closing();
		}

		super.setVisible(visible);
	}
}
