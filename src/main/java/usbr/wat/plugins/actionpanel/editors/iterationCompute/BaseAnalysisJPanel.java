package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagConstraints;     // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.LayoutManager;          // Interface for layout managers; passed to the JPanel superclass constructor
import java.awt.event.ActionEvent;      // Represents an action event fired when a menu item is toggled
import java.util.ArrayList;            // Resizable-array List used to convert a members int[] to a List<Integer>
import java.util.Arrays;               // Utility for array operations; used to sort compute members before validation
import java.util.List;                 // Ordered collection interface for integer member lists

import javax.swing.JCheckBoxMenuItem;   // Checkbox-style menu item used in the "Ignore Compute Errors" popup
import javax.swing.JLabel;             // Non-interactive text label for the Compute Members and Maximum fields
import javax.swing.JOptionPane;        // Provides informational and error dialog boxes for validation failures
import javax.swing.JPanel;             // Base Swing panel class that this abstract class extends
import javax.swing.JPopupMenu;         // Popup context menu attached to this panel for compute-error settings
import javax.swing.JTabbedPane;        // Tabbed pane hosting sub-panels (BC, sensitivity, etc.)

import hec2.wat.model.WatSimulation;   // Represents a WAT simulation; passed down to sub-panels

import rma.swing.RmaInsets;            // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJIntegerField;     // RMA integer input field used for the Maximum members value
import rma.swing.RmaJIntegerSetField;  // RMA set-entry field for specifying a set of integer member indices

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog; // Hosting dialog that provides simulation context
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;           // Data model holding member indices and maximum member value

/**
 * Abstract base panel for iteration and position-analysis settings within the
 * WTMP Action Panel.
 *
 * Provides the shared "Compute Members" and "Maximum" integer input fields, a
 * JTabbedPane that subclasses populate with their own BC and sensitivity panels,
 * and a right-click popup menu for toggling the "Ignore Compute Errors" system
 * property.
 *
 * Concrete subclasses must implement getPanelName() to supply a label used in
 * validation messages, and override buildControls() to add their own tabs to the
 * shared _tabbedPane after calling super.buildControls().
 *
 */
public abstract class BaseAnalysisJPanel extends JPanel {
	// Tabbed pane populated by subclasses with BC and other analysis-specific tabs
	protected JTabbedPane _tabbedPane;

	// Field for entering the set of integer member indices to compute (e.g., "1,3,5-7")
	protected RmaJIntegerSetField _groupMembersFld;

	// Field for entering the maximum allowable member index
	protected RmaJIntegerField _maxMembersFld;

	// Reference to the hosting EditIterationSettingsDialog for simulation context access
	protected EditIterationSettingsDialog _parent;

	// The currently associated WAT simulation; propagated to sub-panels via setSimulation()
	private WatSimulation _selectedSim;

	/**
	 * Constructs a BaseAnalysisJPanel with the given layout manager and parent dialog.
	 *
	 * Passes the layout to the JPanel superclass, stores the parent reference,
	 * and calls buildControls() to initialize shared UI components.
	 *
	 * @param layout the LayoutManager to apply to this panel
	 * @param parent the EditIterationSettingsDialog that hosts this panel
	 */
	public BaseAnalysisJPanel(LayoutManager layout, EditIterationSettingsDialog parent) {
		// Initialize the JPanel superclass with the provided layout
		super(layout);

		// Store the hosting dialog reference for simulation access
		_parent = parent;

		// Build and add all shared UI controls
		buildControls();
	}

	/**
	 * Builds and lays out the shared UI controls: Compute Members label and set field,
	 * Maximum label and integer field, a JTabbedPane for sub-panel tabs, and a right-click
	 * popup menu for toggling compute-error handling.
	 *
	 * Subclasses should call super.buildControls() and then add their own tabs to _tabbedPane.
	 */
	protected void buildControls() {
		// Create the "Compute Members:" label for the integer set field
		JLabel label = new JLabel("Compute Members:");
		GridBagConstraints gbc = new GridBagConstraints();

		// Position the label with fixed width and no fill
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Create the integer set field for specifying which members to compute
		_groupMembersFld = new RmaJIntegerSetField();
		label.setLabelFor(_groupMembersFld);

		// Position the set field to expand horizontally
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_groupMembersFld, gbc);

		// Create the "Maximum:" label for the max-members integer field
		label = new JLabel("Maximum:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Create the maximum-members integer field
		_maxMembersFld = new RmaJIntegerField();
		label.setLabelFor(_maxMembersFld);

		// Position the max field to expand horizontally and end the row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_maxMembersFld, gbc);

		// Create the tabbed pane that subclasses will populate with their own tabs
		_tabbedPane = new JTabbedPane();

		// Position the tabbed pane to fill all remaining space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_tabbedPane, gbc);

		// Create the right-click popup menu with the "Ignore Compute Errors" toggle item
		JPopupMenu popup = new JPopupMenu();
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Ignore Compute Errors");

		// Initialize the checkbox state from the current system property value
		menuItem.setSelected(Boolean.getBoolean("ActionComputable.ContinueOnError"));
		popup.add(menuItem);

		// Toggle the system property when the menu item is clicked
		menuItem.addActionListener(e -> setIgnoreComputeErrorsAction(e));

		// Attach the popup menu to this panel so it appears on right-click
		setComponentPopupMenu(popup);
	}

	/**
	 * Handles the "Ignore Compute Errors" menu item toggle action.
	 *
	 * Sets the system property "ActionComputable.ContinueOnError" to "true" when
	 * the item is selected, and clears it when deselected.
	 *
	 * @param e the ActionEvent fired by the JCheckBoxMenuItem
	 */
	private static void setIgnoreComputeErrorsAction(ActionEvent e) {
		Object obj = e.getSource();
		if (obj instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) obj;
			boolean selected = mi.isSelected();

			if (selected) {
				// Set the property to allow computation to continue past errors
				System.setProperty("ActionComputable.ContinueOnError", "true");
			} else {
				// Remove the property to restore default error-stopping behavior
				System.clearProperty("ActionComputable.ContinueOnError");
			}
		}
	}

	/**
	 * Sets the maximum members field value.
	 *
	 * Called by PositionAnalysisBcPanel after calculating the maximum available
	 * annual elements across all DSS inputs.
	 *
	 * @param maxElement the maximum member count to display in the field
	 */
	public void setMaxElement(int maxElement) {
		_maxMembersFld.setValue(maxElement);
	}

	/**
	 * Populates the Compute Members and Maximum fields from a BaseComputeSettings object.
	 *
	 * Converts the settings' int[] member array to a List<Integer> for the set field
	 * and sets the maximum member value.
	 *
	 * @param computeSettings the BaseComputeSettings object to populate from
	 */
	public void fillPanel(BaseComputeSettings computeSettings) {
		// Retrieve the array of member indices to compute
		int[] members = computeSettings.getMembersToCompute();
		List<Integer> iterationMembersList = new ArrayList<>();

		if (members != null) {
			// Convert the int[] to a List<Integer> for the RmaJIntegerSetField
			for (int i = 0; i < members.length; i++) {
				iterationMembersList.add(members[i]);
			}
		}

		// Apply the member list and maximum value to the UI fields
		_groupMembersFld.setIntegerSet(iterationMembersList);
		_maxMembersFld.setValue(computeSettings.getMaximumMember());
	}

	/**
	 * Saves the current field values back to a BaseComputeSettings object.
	 *
	 * If the Compute Members field contains "*" (meaning all members), the member
	 * array is not updated. Otherwise, the parsed integer set is written to the settings.
	 * The maximum member value is always saved.
	 *
	 * @param computeSettings the BaseComputeSettings object to save into
	 */
	public void savePanel(BaseComputeSettings computeSettings) {
		String txt = _groupMembersFld.getText();

		if ("*".equals(txt)) {
			// Wildcard entry: leave the members array unchanged in the settings
		} else {
			// Parse the integer set field and store the result
			int[] computeMembers = _groupMembersFld.getIntegerSet();
			computeSettings.setMembersToCompute(computeMembers);
		}

		// Always save the maximum member value
		computeSettings.setMaximumMember(_maxMembersFld.getValue());
	}

	/**
	 * Validates the current field values and shows an informational dialog for
	 * each validation failure.
	 *
	 * Checks that the maximum member field has a value, that at least one compute
	 * member index is entered, and that the maximum is not less than the largest
	 * specified compute member index.
	 *
	 * @return true if all validation checks pass; false if any check fails
	 */
	public boolean isValidForm() {
		// Check that the maximum member field has been filled in
		int max = _maxMembersFld.getValueUndefined(-1);
		if (max == -1) {
			JOptionPane.showMessageDialog(this, "Please enter a Maximum Compute member value for " + getPanelName() + "Settings", "Missing Value", JOptionPane.INFORMATION_MESSAGE);
			_maxMembersFld.requestFocus();
			return false;
		}

		// Check that at least one compute member index has been entered
		int[] computeMembers = _groupMembersFld.getIntegerSet();
		if (computeMembers == null || computeMembers.length == 0) {
			JOptionPane.showMessageDialog(this, "Please enter the Compute Members to compute for " + getPanelName() + " Settings", "Missing Value", JOptionPane.INFORMATION_MESSAGE);
			_groupMembersFld.requestFocus();
			return false;
		}

		// Sort the members so the largest value is at the end for the range check
		Arrays.sort(computeMembers);

		// Ensure the maximum member is not less than the highest compute member index
		if (max < computeMembers[computeMembers.length - 1]) {
			JOptionPane.showMessageDialog(this, "The Maximum " + getPanelName() + " Member must be greater than or equal to the largest Compute Member", "Invalid Value", JOptionPane.INFORMATION_MESSAGE);
			_maxMembersFld.requestFocus();
			return false;
		}

		// All validation checks passed
		return true;
	}

	/**
	 * Sets the WAT simulation associated with this panel.
	 *
	 * Stored locally and propagated to sub-panels by subclass overrides of this method.
	 *
	 * @param selectedSim the WatSimulation to associate with this panel
	 */
	public void setSimulation(WatSimulation selectedSim) {
		_selectedSim = selectedSim;
	}

	/**
	 * Returns the human-readable name of this panel, used in validation messages.
	 *
	 * Concrete subclasses must implement this to return a descriptive name
	 * (e.g., "Iteration" or "Position Analysis").
	 *
	 * @return the display name of this panel
	 */
	protected abstract String getPanelName();
}
