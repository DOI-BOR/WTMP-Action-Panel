package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagLayout; // Flexible grid-based layout manager passed to the BaseAnalysisJPanel superclass

import javax.swing.BorderFactory;       // Factory for creating titled and other border styles
import javax.swing.border.TitledBorder; // Titled border that labels this panel as "Position Analysis Settings"

import hec2.plugin.model.ModelAlternative; // Represents a model alternative configuration within a WAT simulation

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;  // Hosting dialog providing simulation and alternative context
import usbr.wat.plugins.actionpanel.editors.PositionAnalysisBcPanel;      // Specialized BC panel that also calculates the maximum annual element count
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;      // Per-model-alternative BC settings retrieved from PositionAnalysisSettings
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;       // Data model holding BC assignments and max-element configuration for position analysis

/**
 * Panel for configuring position analysis settings within the WTMP Action Panel.
 *
 * Extends BaseAnalysisJPanel to inherit the Compute Members and Maximum fields
 * and adds a single "Boundary Conditions" tab backed by a PositionAnalysisBcPanel.
 * The Maximum members field is made non-editable because its value is automatically
 * derived from the DSS time ranges of the configured BC inputs.
 *
 * The panel is labeled with a titled border "Position Analysis Settings" and
 * returns "Position Analysis" as its panel name for validation messages.
 */
public class PositionAnalysisPanel extends BaseAnalysisJPanel {
	// Specialized BC panel that populates the BC table and drives the max-element calculation
	private PositionAnalysisBcPanel _positionAnalysisBcPanel;

	/**
	 * Constructs a PositionAnalysisPanel attached to the given parent dialog.
	 *
	 * Passes a GridBagLayout and the parent reference to BaseAnalysisJPanel,
	 * which calls buildControls() to initialize all shared and local controls.
	 *
	 * @param parent the EditIterationSettingsDialog that hosts this panel
	 */
	public PositionAnalysisPanel(EditIterationSettingsDialog parent) {
		// Initialize the base panel with a GridBagLayout and the hosting dialog reference
		super(new GridBagLayout(), parent);
	}

	/**
	 * Builds the position-analysis-specific controls by extending the base class layout.
	 *
	 * Calls super.buildControls() to create the shared fields and tabbed pane,
	 * then applies a titled border, creates the PositionAnalysisBcPanel (passing
	 * this panel as the max-element receiver), adds it as a tab, and makes the
	 * Maximum members field read-only so it can only be set programmatically.
	 */
	@Override
	protected void buildControls() {
		// Add the shared Compute Members, Maximum, and tabbed pane controls
		super.buildControls();

		// Label the panel to identify it as the Position Analysis Settings section
		TitledBorder border = BorderFactory.createTitledBorder("Position Analysis Settings");
		setBorder(border);

		// Create the position analysis BC panel; pass 'this' so it can call setMaxElement()
		_positionAnalysisBcPanel = new PositionAnalysisBcPanel(_parent, this);

		// Add the BC panel as the single tab in the inherited tabbed pane
		_tabbedPane.addTab(_positionAnalysisBcPanel.getTabname(), _positionAnalysisBcPanel);

		// Make the Maximum field read-only; its value is driven by DSS time-range calculation
		_maxMembersFld.setEditable(false);
	}

	/**
	 * Populates the panel from a PositionAnalysisSettings object for the given model alternative.
	 *
	 * Delegates the shared Compute Members and Maximum fields to the superclass,
	 * then fills the position analysis BC panel with the per-alternative settings.
	 *
	 * @param modelAlt                 the ModelAlternative to populate the BC panel for
	 * @param positionAnalysisSettings the PositionAnalysisSettings object containing all BC data
	 */
	public void fillPanel(ModelAlternative modelAlt, PositionAnalysisSettings positionAnalysisSettings) {
		// Populate the shared Compute Members and Maximum fields from the settings object
		super.fillPanel(positionAnalysisSettings);

		// Apply the selected simulation's variant name to the model alternative
		String variantName = _parent.getSelectedSimulation().getVariantName();
		modelAlt.setVariantName(variantName);

		// Retrieve and populate the BC settings for the specific model alternative
		ModelAltIterationSettings modelAltSettings = positionAnalysisSettings.getModelAltSettings(modelAlt);
		_positionAnalysisBcPanel.fillPanel(modelAltSettings);
	}


	/**
	 * Saves the current panel state back to a PositionAnalysisSettings object.
	 *
	 * Delegates the shared fields to the superclass, then saves the BC panel
	 * data for the currently selected model alternative.
	 *
	 * @param iterationSettings the PositionAnalysisSettings object to save into
	 */
	public void savePanel(PositionAnalysisSettings iterationSettings) {
		// Save the shared Compute Members and Maximum fields
		super.savePanel(iterationSettings);

		// Save BC settings only if a model alternative is currently selected
		ModelAlternative selectedModelAlt = _parent.getSelectedModelAlternative();
		if (selectedModelAlt != null) {
			ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(selectedModelAlt);
			_positionAnalysisBcPanel.savePanel(modelAltSettings);
		}
	}

	/**
	 * Returns the human-readable name of this panel, used in validation messages.
	 *
	 * @return "Position Analysis"
	 */
	@Override
	protected String getPanelName() {
		return "Position Analysis";
	}
}
