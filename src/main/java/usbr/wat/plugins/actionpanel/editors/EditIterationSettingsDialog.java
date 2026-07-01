package usbr.wat.plugins.actionpanel.editors;

import java.awt.CardLayout;                                                     // Layout that swaps panels by name for compute-type views
import java.awt.Cursor;                                                         // AWT cursor utility used to display wait and default cursors during long operations
import java.awt.EventQueue;                                                     // AWT event dispatch utility to schedule tasks on the Event Dispatch Thread (EDT)
import java.awt.GridBagConstraints;                                             // Layout constraints object for positioning components in a grid-based layout
import java.awt.GridBagLayout;                                                  // Grid-based layout manager for arranging components in rows and columns
import java.awt.event.ActionEvent;                                              // Event class for action callbacks (e.g., button presses)
import java.awt.event.ItemEvent;                                                // Event class for item selection changes in combo boxes

import java.util.ArrayList;                                                     // Resizable list used to collect editor panels
import java.util.List;                                                          // Collections interface used for lists of simulations and alternatives

import javax.swing.JLabel;                                                      // Swing label component used for field captions
import javax.swing.JOptionPane;                                                 // Swing utility for showing information dialogs
import javax.swing.JPanel;                                                      // Swing container panel used to group controls
import javax.swing.JSeparator;                                                  // Swing separator for visual grouping of controls
import javax.swing.JTabbedPane;                                                 // Swing tabbed container used to host compute panels

import hec.gui.AbstractEditorPanel;                                             // Base editor panel from HEC GUI framework
import hec2.plugin.model.ModelAlternative;                                      // WAT model type representing a modeling alternative
import hec2.wat.model.WatSimulation;                                            // WAT model type representing a single simulation scenario or run
import hec2.wat.ui.ModelAltListCellRenderer;                                    // Renderer for model alternatives in list or combo boxes

import rma.swing.ButtonCmdPanel;                                                // RMA command panel with OK, Apply, Cancel buttons
import rma.swing.ButtonCmdPanelListener;                                        // Listener interface for button command panels
import rma.swing.EnabledJPanel;                                                 // Panel base class that supports enabled/disabled state propagation
import rma.swing.RmaInsets;                                                     // Standardized insets utility for consistent component padding and spacing
import rma.swing.RmaJComboBox;                                                  // RMA combo box with convenience features (editable, modifiable)
import rma.swing.RmaJDescriptionField;                                          // RMA multi-line description text field
import rma.swing.RmaJDialog;                                                    // Base RMA dialog with modality and convenience helpers
import rma.swing.RmaJTabbedPane;                                                // RMA tabbed pane
import rma.swing.RmaJTextField;                                                 // RMA single-line text field
import rma.swing.list.RmaListModel;                                             // RMA list model used for combo box models and lists

import usbr.wat.plugins.actionpanel.ActionsWindow;                                  // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.iterationCompute.IterationPanel;        // Panel for iterative compute configuration
import usbr.wat.plugins.actionpanel.editors.iterationCompute.PositionAnalysisPanel; // Panel for position analysis configuration
import usbr.wat.plugins.actionpanel.model.ComputeType;                              // Enumeration defining compute types (Standard, Iterative, PositionAnalysis)
import usbr.wat.plugins.actionpanel.model.IterationSettings;                        // Settings controlling iterative compute behavior
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;                 // Settings for position analysis compute behavior
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                          // Concrete type representing a simulation group managed within the plugin

/**
 * Dialog that lets users edit compute settings for a simulation within a simulation group.
 *
 * Responsibilities:
 * - Display selected simulation and group metadata
 * - Select compute type (Standard, Iterative, Position Analysis)
 * - Show the appropriate settings panel via a CardLayout
 * - Manage per-model-alternative settings
 * - Validate and save changes back to the simulation group
 */
@SuppressWarnings("serial")
public class EditIterationSettingsDialog extends RmaJDialog {

	/**
	 * Read-only text field showing the simulation group name.
	 */
	private RmaJTextField _simGroupFld;

	/** Combo box listing simulations in the group. */
	private RmaJComboBox<WatSimulation>_simCombo;

	/**
	 * Read-only description field for the selected simulation.
	 */
	private RmaJDescriptionField _simDescFld;

	/** Combo box selecting the compute type for the simulation. */
	private RmaJComboBox<ComputeType>_computeTypeCombo;

	/** Combo box listing model alternatives for the selected simulation. */
	private RmaJComboBox<ModelAlternative>_modelAltCombo;

	/**
	 * Tabbed container used by nested panels that require tabs.
	 */
	private JTabbedPane _iterationPanelForTabs;

	/**
	 * Command panel with OK, Apply, Cancel buttons.
	 */
	private ButtonCmdPanel _cmdPanel;

	/**
	 * The simulation group being edited.
	 */
	private SimulationGroup _simGroup;

	/**
	 * The currently selected simulation.
	 */
	private WatSimulation _selectedSim;

	/**
	 * Bottom panel that enables/disables based on compute type.
	 */
	private EnabledJPanel _bottomPanel;

	/**
	 * The currently selected model alternative.
	 */
	private ModelAlternative _selectedModelAlt;

	/**
	 * Card container that swaps compute setting panels by type.
	 */
	private JPanel _cardPanel;

	/**
	 * Panel for iterative compute settings.
	 */
	private IterationPanel _iterationPanel;

	/**
	 * Panel for position analysis compute settings.
	 */
	private PositionAnalysisPanel _posAnalysisPanel;

	/**
	 * Flag to suppress prompts when programmatically filling simulation info.
	 */
	private boolean _fillSimInfo;

	/**
	 * Creates the compute settings dialog for the given parent window.
	 *
	 * @param parent the actions window used as the dialog parent
	 */
	public EditIterationSettingsDialog(ActionsWindow parent) {
		// Initialize modal dialog with parent
		super(parent, true);

		// Build UI controls and layout
		buildControls();

		// Attach listeners for UI interactions and commands
		addListeners();

		// Size and position the dialog
		pack();
		setSize(800, 700);
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out top section, bottom section, and command panel.
	 *
	 * Sets up fields for group, simulation, compute type, and description,
	 * then configures the card-based panels and buttons.
	 */
	private void buildControls() {
		// Use a grid bag layout for flexible placement of controls
		getContentPane().setLayout(new GridBagLayout());

		setTitle("Edit Compute Settings");

		// Top section: group, simulation, compute type, description
		JPanel topPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 0.0;

		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;

		getContentPane().add(topPanel, gbc);

		// Populate top controls
		buildTopPanel(topPanel);

		// Bottom section: model alternative and card panels
		_bottomPanel = new EnabledJPanel(new GridBagLayout());

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;

		getContentPane().add(_bottomPanel, gbc);

		// Populate bottom controls
		buildBottomPanel(_bottomPanel);

		// Command panel with OK, Apply, Cancel
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_APPLY_CANCEL_BUTTONS);

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 0.0;

		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;

		getContentPane().add(_cmdPanel, gbc);

		// Initialize card view based on compute type selection
		computeTypeChanged();
	}



	/**
	 * Builds the top section of the dialog that displays and configures
	 * high-level simulation metadata:
	 * - Simulation Group (read-only)
	 * - Simulation selector
	 * - Compute Type selector
	 * - Simulation Description (read-only)
	 *
	 * Layout:
	 * Uses a single GridBagConstraints instance reused for each added control.
	 * Each control is labeled and the label is associated with its input via setLabelFor
	 * to improve accessibility.
	 *
	 * @param panel the container panel to populate with the top controls
	 */
	private void buildTopPanel(JPanel panel) {
		// Label: Simulation Group
		JLabel label = new JLabel("Simulation Group:");

		// Shared GridBagConstraints used for each control in the top section
		GridBagConstraints gbc = new GridBagConstraints();

		// Place the label
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(label, gbc);

		// Field: read-only text for the Simulation Group name
		_simGroupFld = new RmaJTextField();
		_simGroupFld.setEditable(false);
		label.setLabelFor(_simGroupFld);

		// Place the group field
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_simGroupFld, gbc);

		// Label: Simulation
		label = new JLabel("Simulation:");

		// Place the label
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(label, gbc);

		// Combo: list of simulations in the group (read-only selection)
		_simCombo = new RmaJComboBox<>();
		_simCombo.setEditable(false);
		_simCombo.setModifiable(false);
		label.setLabelFor(_simCombo);

		// Place the simulation combo
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_simCombo, gbc);

		// Label: Compute Type
		label = new JLabel("Compute Type:");

		// Place the label
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(label, gbc);

		// Combo: compute type (disabled until a simulation is selected)
		_computeTypeCombo = new RmaJComboBox<>(ComputeType.values());
		_computeTypeCombo.setEnabled(false);
		_computeTypeCombo.setModifiable(true);
		label.setLabelFor(_computeTypeCombo);

		// Place the compute type combo
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_computeTypeCombo, gbc);

		// Label: Description
		label = new JLabel("Description:");

		// Place the label
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(label, gbc);

		// Field: read-only simulation description
		_simDescFld = new RmaJDescriptionField();
		_simDescFld.setEditable(false);
		label.setLabelFor(_simDescFld);

		// Place the description field
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_simDescFld, gbc);

		// Visual divider between the top and bottom sections
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(new JSeparator(), gbc);

	}


	/**
	 * Builds the bottom section of the dialog that configures compute settings
	 * for the currently selected simulation and model alternative.
	 *
	 * This section contains:
	 * - A Model Alternative selector (combo box with custom renderer)
	 * - A CardLayout-backed content area that swaps between compute setting panels:
	 *   • Standard (empty panel; no extra settings)
	 *   • Iterative (IterationPanel)
	 *   • Position Analysis (PositionAnalysisPanel)
	 *
	 * The CardLayout is keyed by the {@link ComputeType#getName()} values, so that
	 * {@link #computeTypeChanged()} can switch views by name consistently.
	 *
	 * @param panel the container panel to populate with the model alternative
	 *              selector and the card-based compute settings panels
	 */
	private void buildBottomPanel(JPanel panel)
	{
		// Label: Model Alternative
		JLabel label = new JLabel("Model Alternative:");

		// Shared GridBagConstraints used for controls in the bottom section
		GridBagConstraints gbc = new GridBagConstraints();

		// Place the label
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(label, gbc);

		// Combo: list of model alternatives for the selected simulation
		// Uses a custom renderer for readable alternative names and details
		_modelAltCombo = new RmaJComboBox<>();
		_modelAltCombo.setRenderer(new ModelAltListCellRenderer());
		label.setLabelFor(_modelAltCombo);

		// Place the model alternative combo
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_modelAltCombo, gbc);

		// Card container that hosts per-compute-type settings panels
		_cardPanel = new JPanel(new CardLayout());

		// Place the card container; it expands to fill remaining space
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;

		panel.add(_cardPanel, gbc);


		// Standard compute type: no additional settings (empty panel placeholder)
		_cardPanel.add(new JPanel(), ComputeType.Standard.getName());

		// Iterative compute settings panel
		_iterationPanel = new IterationPanel(this);
		_cardPanel.add(_iterationPanel, ComputeType.Iterative.getName());

		// Position analysis compute settings panel
		_posAnalysisPanel = new PositionAnalysisPanel(this);
		_cardPanel.add(_posAnalysisPanel, ComputeType.PositionAnalysis.getName());

		// Default view: Standard (no extra settings shown)
		((CardLayout)_cardPanel.getLayout()).show(_cardPanel, ComputeType.Standard.getName());
	}

	/**
	 * Attaches UI listeners for user interactions in the dialog.
	 *
	 * Listeners:
	 * - Simulation combo: reacts to selection changes and delegates to simComboChange.
	 * - Model alternative combo: reacts to selection changes and delegates to modelAltComboChanged.
	 * - Compute type combo: updates the card view and enabled state via computeTypeChanged.
	 * - Command panel (OK, Apply, Cancel):
	 *     • OK: saveForm, then close if save succeeds.
	 *     • Apply: saveForm without closing.
	 *     • Cancel: close the dialog without saving.
	 */
	private void addListeners() {
		// Listen for simulation selection changes and handle them in simComboChange
		_simCombo.addItemListener(e -> simComboChange(e));

		// Listen for model alternative selection changes and handle them in modelAltComboChanged
		_modelAltCombo.addItemListener(e -> modelAltComboChanged(e));

		// When the compute type changes, update the card layout and bottom panel state
		_computeTypeCombo.addActionListener(e -> computeTypeChanged());

		// Wire command buttons: OK, Apply, Cancel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Save current settings; close only if save succeeds
						if (saveForm()) {
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.APPLY_BUTTON:
						// Save current settings without closing
						saveForm();
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Close the dialog without saving changes
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Updates the card view and bottom panel enabled state based on the selected compute type.
	 *
	 * Ensures position analysis is only allowed when the simulation time window is one year or less.
	 */
	private void computeTypeChanged() {
		double numYears = 0.;

		if (_selectedSim != null) {
			// Determine the length of the simulation time window
			numYears = _selectedSim.getRunTimeWindow().getNumberOfYears();
		}

		ComputeType type = (ComputeType) _computeTypeCombo.getSelectedItem();

		// Position analysis requires a time window of one year or less
		if (numYears > 1.0 && type == ComputeType.PositionAnalysis) {
			JOptionPane.showMessageDialog(this, "Position Analysis Computes can only be performed on Simulations with a time window of 1 year or less.", "Time Window too long", JOptionPane.INFORMATION_MESSAGE);

			// Revert to Standard on the EDT
			EventQueue.invokeLater(() -> _computeTypeCombo.setSelectedItem(ComputeType.Standard));

			return;
		}

		// Enable bottom panel only for non-standard compute types
		boolean enabled = type != ComputeType.Standard;

		_bottomPanel.setEnabled(enabled);

		// Switch card to the selected compute type
		((CardLayout) _cardPanel.getLayout()).show(_cardPanel, type.getName());
	}

	/**
	 * Handles item changes for the model alternative combo.
	 * <p>
	 * Prompts to save pending changes when switching away from an edited alternative,
	 * then fills the panels with settings for the newly selected alternative.
	 *
	 * @param e the item event
	 */
	private void modelAltComboChanged(ItemEvent e) {
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Prompt to save pending changes for the previous alternative
		if (isModified() && _selectedModelAlt != null) {
			if (_selectedModelAlt != null && shouldSaveChanges(_selectedModelAlt.getName())) {
				saveSimInfo();
			}
		}

		// Load settings for the new alternative
		ModelAlternative modelAlt = (ModelAlternative) _modelAltCombo.getSelectedItem();
		fillModelAltInfo(modelAlt);
	}

	/**
	 * Fills the compute panels with settings for the given model alternative.
	 * <p>
	 * Loads iteration and position analysis settings for the selected simulation,
	 * populates both panels, and clears the modified flag.
	 *
	 * @param modelAlt the model alternative to load
	 */
	private void fillModelAltInfo(ModelAlternative modelAlt) {
		_selectedModelAlt = modelAlt;

		clearTabPanels();

		if (modelAlt == null) {
			return;
		}

		// Load iteration settings for the selected simulation
		IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());

		_iterationPanel.fillPanel(modelAlt, iterationSettings);

		// Load position analysis settings for the selected simulation
		PositionAnalysisSettings posAnalysisSettings = _simGroup.getPositionAnalysisSettings(_selectedSim.getName());

		_posAnalysisPanel.fillPanel(modelAlt, posAnalysisSettings);

		// Reset modified flag on EDT
		EventQueue.invokeLater(() -> setModified(false));

	}


	/**
	 * Clears or resets tab panel state as needed.
	 * Currently a placeholder that keeps existing content.
	 */
	private void clearTabPanels() {

	}

	/**
	 * Handles simulation combo item changes by deferring to simComboChanged on the EDT.
	 *
	 * @param e the item event
	 */
	private void simComboChange(ItemEvent e) {
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Switch simulation on the EDT
		EventQueue.invokeLater(() -> simComboChanged());
	}

	/**
	 * Applies any pending changes for the previous simulation, then fills form fields
	 * and panels for the newly selected simulation.
	 */
	private void simComboChanged() {
		// Prompt to save changes for the previously selected simulation
		if (isModified() && _selectedSim != null) {
			if (shouldSaveChanges(_selectedSim.getName())) {
				saveSimInfo();
			}
		}

		// Load the new selection
		WatSimulation selectedSim = (WatSimulation) _simCombo.getSelectedItem();
		fillSimInfo(selectedSim);
	}

	/**
	 * Populates fields and panels based on the selected simulation.
	 *
	 * Loads description, model alternatives, compute type, and updates card view.
	 *
	 * @param selectedSim the simulation to load
	 */
	private void fillSimInfo(WatSimulation selectedSim) {
		// Show wait cursor while loading
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			_fillSimInfo = true;
			_selectedSim = selectedSim;

			// Inform the iteration panel of the current simulation
			_iterationPanel.setSimulation(_selectedSim);

			if (selectedSim != null) {
				// Description
				_simDescFld.setText(selectedSim.getDescription());

				// Populate model alternative list for the simulation
				List<ModelAlternative> modelAlts = selectedSim.getAllModelAlternativeList();
				RmaListModel<ModelAlternative> newModel = new RmaListModel<>(false, modelAlts);
				_modelAltCombo.setModel(newModel);

				if (newModel.size() == 1) {
					// Auto-select the only alternative
					EventQueue.invokeLater(() -> _modelAltCombo.setSelectedIndex(0));
				}

				// Load and apply compute type
				ComputeType computeType = _simGroup.getComputeType(_selectedSim.getName());
				_computeTypeCombo.setSelectedItem(computeType);
				_computeTypeCombo.setEnabled(true);
				computeTypeChanged();

				// Clear the selected alternative until the combo fires
				_selectedModelAlt = null;

				// Reset modified flag on EDT
				EventQueue.invokeLater(() -> setModified(false));
			}
		} finally {
			// Clear flag to re-enable prompts and restore cursor
			_fillSimInfo = false;

			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Validates and writes current panel values to the simulation group for the selected simulation.
	 *
	 * @return true if values were valid and saved; false otherwise
	 */
	private boolean saveSimInfo() {
		if (isValidForm()) {
			// Persist compute type
			ComputeType computeType = (ComputeType) _computeTypeCombo.getSelectedItem();
			_simGroup.setComputeType(_selectedSim.getName(), computeType);

			// Persist iteration settings
			IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());
			_iterationPanel.savePanel(iterationSettings);

			// Persist position analysis settings
			PositionAnalysisSettings posAnalysisSettings = _simGroup.getPositionAnalysisSettings(_selectedSim.getName());
			_posAnalysisPanel.savePanel(posAnalysisSettings);

			// Mark the group modified and clear flag
			_simGroup.setModified(true);
			setModified(false);

			return true;
		}

		return false;
	}

	/**
	 * Form validation for the current selection and compute type.
	 *
	 * Ensures the required fields for the selected compute type are valid.
	 *
	 * @return true when valid; false when a required panel fails validation
	 */
	private boolean isValidForm() {
		if (_selectedSim != null) {
			// No selection means no validation required
			if (_computeTypeCombo.getSelectedIndex() < 0) {
				return true;
			}

			ComputeType computeType = (ComputeType) _computeTypeCombo.getSelectedItem();

			// Iterative requires its panel to be valid
			if (computeType == ComputeType.Iterative && !_iterationPanel.isValidForm()) {
				return false;
			} else if (computeType == ComputeType.PositionAnalysis) {
				// Position analysis panel may have its own validation; placeholder
			}

			return true;

		}

		return false;
	}

	/**
	 * Prompts the user to save changes when switching context away from the given name.
	 *
	 * Suppresses the prompt when programmatic filling is in progress.
	 *
	 * @param name the name of the simulation or model alternative being switched from
	 * @return true if the user chose to save; false otherwise
	 */
	private boolean shouldSaveChanges(String name) {
		// Do not prompt when programmatically populating fields
		if (_fillSimInfo) {
			return false;
		}

		// Prompt to save if modified
		if (isModified()) {
			int opt = JOptionPane.showConfirmDialog(this, "There are changes for " + name + ". Save Changes?", "Confirm Changes", JOptionPane.YES_NO_OPTION);

			return opt == JOptionPane.YES_OPTION;
		}

		return false;
	}

	/**
	 * Saves current values from the form and clears the modified flag.
	 *
	 * @return true if save succeeded; false otherwise
	 */
	protected boolean saveForm() {
		if (!saveSimInfo()) {
			return false;
		}

		setModified(false);

		return true;

	}

	/**
	 * Container that behaves like a single panel until a second panel is added,
	 * at which point it switches to a tabbed presentation automatically.
	 */
	class SometimesTabbedPanel extends JPanel {
		/**
		 * Tab container created on demand when multiple panels are present.
		 */
		private JTabbedPane _tabPane;

		/** Panels added to this container; the list drives tab creation. */
		private List<AbstractEditorPanel> _panelList =new ArrayList<>();

		/**
		 * Constructs the container with grid bag layout.
		 */
		SometimesTabbedPanel() {
			super(new GridBagLayout());
		}


		/**
		 * Adds an editor panel to this container, switching to a tabbed view on demand.
		 *
		 * Behavior:
		 * 1) If this is the first panel added, it is placed directly into the container
		 *    (single-panel mode, no tabs).
		 * 2) If a second (or subsequent) panel is added, a JTabbedPane is created lazily,
		 *    the previously added panels are migrated into tabs, and the new panel is
		 *    appended as an additional tab.
		 *
		 * Notes:
		 * - Panels are tracked in _panelList so that previously added panels can be
		 *   moved into the tabbed pane when switching to tabbed mode.
		 * - GridBagConstraints are reused to ensure consistent sizing and anchoring.
		 *
		 * @param panel the editor panel to add (ignored when null)
		 */
		public void addPanel(AbstractEditorPanel panel) {
			// Ignore null panels to keep container state consistent
			if (panel == null) {
				return;
			}

			// Constraints for full-size placement within this container
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = RmaInsets.INSETS5505;

			// If this is the first panel added, show it directly (no tabs yet)
			if (getComponentCount() == 0) {
				add(panel, gbc);

				// add(panel, BorderLayout.CENTER);  // original optional placement
			} else {
				// Lazily create the tabbed pane on the first transition to multi-panel mode
				if (_tabPane == null) {
					_tabPane = new RmaJTabbedPane();

					add(_tabPane, gbc);

					// add(_tabPane, BorderLayout.CENTER);  // original optional placement

					// Migrate any previously added panels into tabs
					for (int i = 0; i < _panelList.size(); i++) {
						AbstractEditorPanel tPanel = _panelList.get(i);

						_tabPane.add(tPanel, tPanel.getTabname());
					}
				}

				// Append the newly added panel as another tab
				_tabPane.add(panel, panel.getTabname());
			}

			// Track the panel so it can be migrated if/when tabbed mode is enabled
			_panelList.add(panel);
		}
	}


	/**
	 * Populates this dialog with the provided simulation group and its simulations.
	 *
	 * Steps:
	 * 1) Store the group reference and show its name in the read-only field.
	 * 2) Copy the group's simulations into a new mutable list for the combo model.
	 * 3) Build an RmaListModel and set it on the simulation combo box.
	 * 4) If there is exactly one simulation, auto-select it for convenience.
	 * 5) Clear the modified flag asynchronously on the EDT.
	 *
	 * @param simGroup the simulation group to load into the dialog
	 */
	public void fillForm(SimulationGroup simGroup) {
		// Keep a reference to the group being edited
		_simGroup = simGroup;

		// Display the group's name in the header field
		_simGroupFld.setText(_simGroup.getName());

		// Retrieve the group's simulations
		List<WatSimulation> sims = _simGroup.getSimulations();

		// Copy into a new mutable list to avoid side effects on the group's internal list
		List<WatSimulation> simList = new ArrayList<>(sims);

		// Build a list model for the combo; 'true' allows an empty selection
		RmaListModel<WatSimulation> newModel = new RmaListModel<>(true, simList);

		// Apply the model to the simulation selector
		_simCombo.setModel(newModel);

		// If there is only one simulation, select it automatically
		if (simList.size() == 1) {
			_simCombo.setSelectedIndex(0);
		}

		// Clear the modified flag on the EDT to reflect a fresh, loaded state
		EventQueue.invokeLater(() -> setModified(false));
	}

	/**
	 * Sets the modified flag for the dialog.
	 *
	 * @param modified true when the form has unsaved changes; false otherwise
	 */
	@Override
	public void setModified(boolean modified) {
		super.setModified(modified);
	}

	/**
	 * Programmatically selects a simulation in the combo box.
	 *
	 * @param watSimulation the simulation to select; null clears the selection
	 */
	public void setSelectedSimulation(WatSimulation watSimulation) {
		if (watSimulation != null) {
			_simCombo.setSelectedItem(watSimulation);
		} else {
			_simCombo.setSelectedIndex(-1);
		}
	}


	/**
	 * Returns the currently selected simulation.
	 *
	 * @return the selected simulation, or null when none
	 */
	public WatSimulation getSelectedSimulation() {
		return _selectedSim;
	}

	/**
	 * Returns the currently selected model alternative.
	 *
	 * @return the selected model alternative, or null when none
	 */
	public ModelAlternative getSelectedModelAlternative() {
		return _selectedModelAlt;
	}
}