package usbr.wat.plugins.actionpanel;

import java.awt.GridBagConstraints;                                                     // Layout constraints object for positioning components in a grid-based layout
import java.awt.GridBagLayout;                                                          // Grid-based layout manager for arranging components in rows and columns

import java.util.List;                                                                  // Collections interface used for lists of report plugins

import javax.swing.Action;                                                              // Swing action base type used to encapsulate executable UI behaviors
import javax.swing.JButton;                                                             // Swing push button component used to trigger actions

import rma.swing.EnabledJPanel;                                                         // Panel base class that supports enabled/disabled state propagation
import rma.swing.RmaInsets;                                                             // Standardized insets utility for consistent component padding and spacing

import usbr.wat.plugins.actionpanel.actions.DeleteSimulationResultsAction;              // Action to delete saved simulation results from storage
import usbr.wat.plugins.actionpanel.actions.DisplayReportSelectorAction;                // Action to display a selector for available reports in calibration workflows
import usbr.wat.plugins.actionpanel.actions.RunSimulationAction;                        // Action to run simulations in prescribed conditions workflows
import usbr.wat.plugins.actionpanel.actions.SaveSimulationResultsAction;                // Action to persist generated simulation results
import usbr.wat.plugins.actionpanel.actions.forecast.RunForecastSimulationAction;       // Action to run simulations in forecast workflows
import usbr.wat.plugins.actionpanel.model.ForecastReportingPlugin;                      // Reporting plugin interface for forecast-specific report actions
import usbr.wat.plugins.actionpanel.model.ReportPlugin;                                 // Base reporting plugin interface used by the reports manager
import usbr.wat.plugins.actionpanel.model.ReportsManager;                               // Manager that provides registered reporting plugins available to the UI
import usbr.wat.plugins.actionpanel.ui.CalibrationPanel;                                // Panel type for prescribed conditions workflows
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                                       // Base USBR panel type implemented by calibration and forecast panels
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;                        // Panel type for forecast workflows and ensemble simulations

/**
 * Panel hosting simulation-related actions for the WTMP plugin.
 *
 * Provides buttons for running simulations, selecting or displaying reports,
 * saving results, and deleting results. The set of actions adapts based on
 * whether the parent panel is for prescribed conditions or forecast workflows.
 */
@SuppressWarnings("serial")
public class SimulationActionsPanel extends EnabledJPanel {
	/** Owning actions window used to coordinate UI operations and context. */
	private ActionsWindow _parent;

	/** Action for running a simulation (calibration or forecast depending on context). */
	private Action _runSimulationAction;

	/** Action that displays the report selector for calibration workflows. */
	private DisplayReportSelectorAction _displayReportsSelectorAction;

	/** Action that saves simulation results to persistent storage. */
	private SaveSimulationResultsAction _saveResultsAction;

	/** Action that deletes previously saved simulation results. */
	private DeleteSimulationResultsAction _deleteResultsAction;

	/** The parent workflow panel (calibration or forecast) hosting this actions panel. */
	private UsbrPanel _parentPanel;

	/** Action that displays an ensemble report selector for forecast workflows. */
	private Action _displayEnsembleSelectorAction;

	/**
	 * Constructs the simulation actions panel and initializes its controls.
	 *
	 * @param parent the owning actions window for context and callbacks
	 * @param parentPanel the workflow panel (calibration or forecast) that owns this actions panel
	 */
	public SimulationActionsPanel(ActionsWindow parent, UsbrPanel parentPanel) {
		// Initialize the panel with a GridBagLayout for flexible control placement
		super(new GridBagLayout());

		// Store references to the owning window and parent panel
		_parent = parent;

		_parentPanel = parentPanel;

		// Build the action buttons and add them to the panel
		buildControls();

	}

	/**
	 * Builds and lays out the action controls for running simulations and managing results.
	 *
	 * Chooses between calibration and forecast actions based on the parent panel type,
	 * creates buttons for each action, and positions them using GridBagLayout.
	 */
	private void buildControls() {
		// Create the appropriate "Run Simulation" action depending on workflow context
		if ( _parentPanel instanceof CalibrationPanel) {
			_runSimulationAction = new RunSimulationAction(_parent, _parentPanel);

		} else {
			_runSimulationAction = new RunForecastSimulationAction(_parent, (SimulationPanel)_parentPanel);
		}

		// Button that triggers the run simulation action
		JButton button = new JButton(_runSimulationAction);

		// Layout constraints for positioning buttons in the panel
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;

		// Add the "Run Simulation" button
		add(button, gbc);

		// Retrieve available report plugins from the manager
		List<ReportPlugin> plugins = ReportsManager.getPlugins();

		// Create the appropriate report display action based on workflow context
		if ( _parentPanel instanceof CalibrationPanel) {
			// Calibration: use the generic report selector
			_displayReportsSelectorAction = new DisplayReportSelectorAction(_parent, _parentPanel);

			button = new JButton(_displayReportsSelectorAction);

		} else {
			// Forecast: find a forecast reporting plugin and use its ensemble selector action
			for (int i = 0;i < plugins.size(); i++ ){
				if (plugins.get(i) instanceof ForecastReportingPlugin) {
					ForecastReportingPlugin fplugin = (ForecastReportingPlugin) plugins.get(i);

					_displayEnsembleSelectorAction = fplugin.getReportAction(_parent, _parentPanel);

					button = new JButton(_displayEnsembleSelectorAction);

					break;
				}
			}
		}

		// Position the report-related button, if one was created
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		if ( button != null ) {
			add(button, gbc);
		}

		// Action and button for saving results
		_saveResultsAction = new SaveSimulationResultsAction(_parent, _parentPanel);
		button = new JButton(_saveResultsAction);

		// Position the "Save Results" button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		add(button, gbc);

		// Action and button for deleting results
		_deleteResultsAction = new DeleteSimulationResultsAction(_parent, _parentPanel);
		button = new JButton(_deleteResultsAction);

		// Position the "Delete Results" button
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;

		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		add(button, gbc);
	}


	/**
	 * Updates the enabled state of actions based on current selections.
	 *
	 * Enables or disables run, save, display, and delete actions depending
	 * on whether simulations or results are selected in the parent window.
	 */
	public void updateActions() {
		// If there is no active simulation group, there is nothing to enable
		if ( _parent.getSimulationGroup() == null ) {
			return;
		}

		// Determine whether any simulations are selected
		boolean simActionsEnabled = _parent.getSelectedSimulations().size() > 0;

		// Determine whether any results are selected
		boolean resultsActionsEnabled = _parent.getSelectedResults().size() > 0;

		// Enable run and save actions when simulations are selected
		_runSimulationAction.setEnabled(simActionsEnabled);
		_saveResultsAction.setEnabled(simActionsEnabled);

		// Enable display action when either simulations or results are selected
		if ( _displayReportsSelectorAction != null ) {
			_displayReportsSelectorAction.setEnabled(resultsActionsEnabled || simActionsEnabled);
		}

		// Enable delete results action when results are selected
		_deleteResultsAction.setEnabled(resultsActionsEnabled );
	}
}