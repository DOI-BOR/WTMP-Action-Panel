package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagConstraints; // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;      // Flexible grid-based layout manager for arranging UI components

import javax.swing.JLabel;          // Non-interactive label displaying the "Script Entry Point:" heading

import hec.gui.AbstractEditorPanel; // HEC base class for editor panels with fill/save lifecycle methods
import hec.lang.NamedType;          // Base interface for named model objects passed to fillPanel/savePanel

import hec2.wat.model.WatSimulation; // Represents a WAT simulation; propagated to the compute script panels

import rma.swing.RmaInsets;          // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJTextField;      // RMA-extended text field used as a read-only script entry-point display

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog; // Hosting dialog providing simulation context
import usbr.wat.plugins.actionpanel.model.ActionComputable;              // Provides the required Python method signature constant
import usbr.wat.plugins.actionpanel.model.SensitivitySettings;           // Data model holding pre/post-compute script configurations

/**
 * Panel for configuring sensitivity (pre/post-compute script) settings
 * within the WTMP Action Panel's iteration and position analysis editors.
 *
 * Contains two ComputeScriptsPanels — one for pre-compute scripts and one for
 * post-compute scripts — each of which shows the model alternatives for the
 * current simulation and allows the user to associate a Python (.py) script
 * with each alternative.
 *
 * A read-only text field at the bottom displays the required Python method
 * signature that the script must implement (derived from ActionComputable).
 *
 * This panel appears under the "Scripts" tab in IterationPanel.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class SensitivityPanel extends AbstractEditorPanel {
	// Tab label shown when this panel is embedded in a JTabbedPane
	private static final String TAB_NAME = "Scripts";

	// Reference to the hosting EditIterationSettingsDialog for simulation context
	private EditIterationSettingsDialog _parent;

	// Panel showing pre-compute script assignments for each model alternative
	private ComputeScriptsPanel _preComputePanel;

	// Panel showing post-compute script assignments for each model alternative
	private ComputeScriptsPanel _postComputePanel;

	/**
	 * Constructs a SensitivityPanel attached to the given parent dialog.
	 *
	 * Initializes with a GridBagLayout, stores the parent reference, builds
	 * all controls, and attaches listeners.
	 *
	 * @param parent the EditIterationSettingsDialog that hosts this panel
	 */
	public SensitivityPanel(EditIterationSettingsDialog parent) {
		// Initialize the AbstractEditorPanel with a GridBagLayout
		super(new GridBagLayout());

		// Store the hosting dialog reference for simulation context
		_parent = parent;

		// Build and arrange all UI controls
		buildControls();

		// Attach any required event listeners (currently none beyond construction)
		addListeners();
	}

	/**
	 * Builds and lays out all UI controls within this panel.
	 *
	 * Places the pre-compute scripts panel, the post-compute scripts panel,
	 * a "Script Entry Point:" label, and a read-only text field showing the
	 * required Python method signature.
	 */
	private void buildControls() {
		// Create the pre-compute scripts panel with its titled border label
		_preComputePanel = new ComputeScriptsPanel("Pre-Compute Scripts");

		// Configure constraints for the pre-compute panel to fill available space
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_preComputePanel, gbc);

		// Create the post-compute scripts panel with its titled border label
		_postComputePanel = new ComputeScriptsPanel("Post-Compute Scripts");

		// Configure constraints for the post-compute panel to fill available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_postComputePanel, gbc);

		// Create the "Script Entry Point:" label above the method signature field
		JLabel label = new JLabel("Script Entry Point:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Create a read-only field showing the required Python method signature
		RmaJTextField txtFld = new RmaJTextField("def " + ActionComputable.METHOD_SIGNATURE + ":");
		txtFld.setEditable(false);

		// Position the method signature field to fill horizontal space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(txtFld, gbc);
	}

	/**
	 * Attaches event listeners to this panel's controls.
	 *
	 * Currently a no-op; listeners are managed within the individual
	 * ComputeScriptsPanels.
	 */
	private void addListeners() {
		// No panel-level listeners required; ComputeScriptsPanels register their own
	}

	/**
	 * Propagates the given simulation to both the pre- and post-compute script panels
	 * so that their model alternative tables can be populated.
	 *
	 * @param simulation the WatSimulation whose model alternatives should be shown
	 */
	public void setSimulation(WatSimulation simulation) {
		// Populate the pre-compute panel's table with this simulation's model alternatives
		_preComputePanel.setSimulation(simulation);

		// Populate the post-compute panel's table with this simulation's model alternatives
		_postComputePanel.setSimulation(simulation);
	}

	/**
	 * Populates both compute script panels from a SensitivitySettings object.
	 *
	 * Has no effect if the provided NamedType is not a SensitivitySettings instance.
	 *
	 * @param dobj the NamedType to populate from; expected to be a SensitivitySettings instance
	 */
	@Override
	public void fillPanel(NamedType dobj) {
		SensitivitySettings settings;
		if (dobj instanceof SensitivitySettings) {
			settings = (SensitivitySettings) dobj;

			// Populate the pre-compute scripts panel from the pre-compute settings
			_preComputePanel.fillPanel(settings.getPreComputeSettings());

			// Populate the post-compute scripts panel from the post-compute settings
			_postComputePanel.fillPanel(settings.getPostComputeSettings());
		}
	}

	/**
	 * Returns the tab label for this panel when it is embedded in a JTabbedPane.
	 *
	 * @return the constant TAB_NAME ("Scripts")
	 */
	@Override
	public String getTabname() {
		return TAB_NAME;
	}

	/**
	 * Saves the current script panel state back to a SensitivitySettings object.
	 *
	 * Has no effect if the provided NamedType is not a SensitivitySettings instance.
	 *
	 * @param dobj the NamedType to save into; expected to be a SensitivitySettings instance
	 * @return true always
	 */
	@Override
	public boolean savePanel(NamedType dobj) {
		SensitivitySettings settings;
		if (dobj instanceof SensitivitySettings) {
			settings = (SensitivitySettings) dobj;

			// Save pre-compute script assignments back to the settings object
			_preComputePanel.savePanel(settings.getPreComputeSettings());

			// Save post-compute script assignments back to the settings object
			_postComputePanel.savePanel(settings.getPostComputeSettings());
		}
		return true;
	}
}
