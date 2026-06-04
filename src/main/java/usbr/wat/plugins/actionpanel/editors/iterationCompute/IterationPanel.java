package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagLayout;  // Flexible grid-based layout manager passed to the BaseAnalysisJPanel superclass

import javax.swing.BorderFactory;       // Factory for creating titled and other border styles
import javax.swing.border.TitledBorder; // Titled border that labels this panel as "Iteration Settings"

import hec2.plugin.model.ModelAlternative; // Represents a model alternative configuration within a WAT simulation
import hec2.wat.model.WatSimulation;       // Represents a WAT simulation; propagated to sub-panels via setSimulation()

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;     // Hosting dialog providing simulation and alternative context
import usbr.wat.plugins.actionpanel.model.IterationSettings;                 // Data model holding BC assignments and sensitivity settings for an iteration
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;         // Per-model-alternative BC settings object retrieved from IterationSettings
import usbr.wat.plugins.actionpanel.model.SensitivitySettings;               // Settings object holding pre/post-compute script configurations

/**
 * Panel for configuring iteration analysis settings within the WTMP Action Panel.
 *
 * Extends BaseAnalysisJPanel to inherit the Compute Members and Maximum fields
 * and adds two tabs to the shared JTabbedPane:
 *
 *   - "Boundary Conditions" (IterationBcPanel): for assigning iteration DSS records
 *     to each model alternative's data locations.
 *   - "Scripts" (SensitivityPanel): for associating pre/post-compute Python scripts
 *     with each model alternative.
 *
 * The panel is labeled with a titled border "Iteration Settings" and returns
 * "Iteration" as its panel name for validation messages.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class IterationPanel extends BaseAnalysisJPanel {
	// BC panel tab showing DSS record assignments for each model alternative's data locations
	private IterationBcPanel _bcPanel;

	// Scripts panel tab showing pre/post-compute script configurations
	private SensitivityPanel _sensitivityPanel;

	/**
	 * Constructs an IterationPanel attached to the given parent dialog.
	 *
	 * Passes a GridBagLayout and the parent reference to BaseAnalysisJPanel,
	 * which calls buildControls() to initialize all shared and local controls.
	 *
	 * @param parent the EditIterationSettingsDialog that hosts this panel
	 */
	public IterationPanel(EditIterationSettingsDialog parent) {
		// Initialize the base panel with a GridBagLayout and the hosting dialog reference
		super(new GridBagLayout(), parent);
	}

	/**
	 * Builds the iteration-specific controls by extending the base class layout.
	 *
	 * Calls super.buildControls() to create the shared fields and tabbed pane,
	 * then applies a titled border, creates the IterationBcPanel and SensitivityPanel,
	 * and adds both as tabs to the inherited _tabbedPane.
	 */
	@Override
	protected void buildControls() {
		// Add the shared Compute Members, Maximum, and tabbed pane controls
		super.buildControls();

		// Label the panel to identify it as the Iteration Settings section
		TitledBorder border = BorderFactory.createTitledBorder("Iteration Settings");
		setBorder(border);

		// Create the Boundary Conditions tab panel
		_bcPanel = new IterationBcPanel(_parent);

		// Create the Scripts (sensitivity) tab panel
		_sensitivityPanel = new SensitivityPanel(_parent);

		// Add both panels as tabs to the inherited tabbed pane
		_tabbedPane.addTab(_bcPanel.getTabname(), _bcPanel);
		_tabbedPane.addTab(_sensitivityPanel.getTabname(), _sensitivityPanel);
	}

	/**
	 * Populates the panel from an IterationSettings object for the given model alternative.
	 *
	 * Delegates the shared Compute Members and Maximum fields to the superclass,
	 * then fills the BC panel with the per-alternative settings and the sensitivity
	 * panel with the iteration-level sensitivity (script) settings.
	 *
	 * @param modelAlt          the ModelAlternative to populate the BC panel for
	 * @param iterationSettings the IterationSettings object containing all iteration data
	 */
	public void fillPanel(ModelAlternative modelAlt, IterationSettings iterationSettings) {
		// Populate the shared Compute Members and Maximum fields
		super.fillPanel(iterationSettings);

		// Apply the selected simulation's variant name to the model alternative
		String variantName = _parent.getSelectedSimulation().getVariantName();
		modelAlt.setVariantName(variantName);

		// Retrieve and populate the BC settings for the specific model alternative
		ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(modelAlt);
		_bcPanel.fillPanel(modelAltSettings);

		// Retrieve and populate the sensitivity (script) settings
		SensitivitySettings sSettings = iterationSettings.getSensitivitySettings();
		_sensitivityPanel.fillPanel(sSettings);
	}


	/**
	 * Saves the current panel state back to an IterationSettings object.
	 *
	 * Delegates the shared fields to the superclass, then saves the BC panel
	 * data for the currently selected model alternative and the sensitivity
	 * (script) settings.
	 *
	 * @param iterationSettings the IterationSettings object to save into
	 */
	public void savePanel(IterationSettings iterationSettings) {
		// Save the shared Compute Members and Maximum fields
		super.savePanel(iterationSettings);

		// Save BC settings only if a model alternative is currently selected
		ModelAlternative selectedModelAlt = _parent.getSelectedModelAlternative();
		if (selectedModelAlt != null) {
			ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(selectedModelAlt);
			_bcPanel.savePanel(modelAltSettings);
		}

		// Save the sensitivity (pre/post-compute script) settings
		_sensitivityPanel.savePanel(iterationSettings.getSensitivitySettings());
	}

	/**
	 * Returns the human-readable name of this panel, used in validation messages.
	 *
	 * @return "Iteration"
	 */
	@Override
	protected String getPanelName() {
		return "Iteration";
	}

	/**
	 * Sets the WAT simulation on this panel and propagates it to the SensitivityPanel.
	 *
	 * The SensitivityPanel uses the simulation to populate its model alternative
	 * script tables. The IterationBcPanel is populated separately via fillPanel().
	 *
	 * @param selectedSim the WatSimulation to associate with this panel
	 */
	@Override
	public void setSimulation(WatSimulation selectedSim) {
		// Store the simulation reference in the base class
		super.setSimulation(selectedSim);

		// Propagate to the sensitivity panel so its script tables can be populated
		_sensitivityPanel.setSimulation(selectedSim);
	}
}
