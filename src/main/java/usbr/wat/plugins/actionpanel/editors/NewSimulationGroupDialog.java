package usbr.wat.plugins.actionpanel.editors;

import java.awt.Cursor;                                                             // Provides cursor types for changing the mouse pointer (e.g., wait cursor)
import java.awt.GridBagConstraints;                                                 // Defines constraint parameters for components in a GridBagLayout
import java.awt.GridBagLayout;                                                      // Flexible grid-based layout manager for arranging UI components
import java.awt.event.ActionEvent;                                                  // Represents an action event fired by user interaction (e.g., button click)
import java.lang.reflect.Constructor;                                               // Provides reflective access to a class constructor at runtime
import java.lang.reflect.InvocationTargetException;                                 // Wraps exceptions thrown by a reflectively invoked constructor or method

import java.util.ArrayList;                                                         // Resizable-array implementation of the List interface
import java.util.HashSet;                                                           // Hash table-based implementation of the Set interface for unique elements
import java.util.Iterator;                                                          // Iterator interface for traversing a collection element by element
import java.util.List;                                                              // Ordered collection (sequence) interface
import java.util.Set;                                                               // Collection interface that contains no duplicate elements
import java.util.Vector;                                                            // Synchronized, growable array used for building table rows
import java.util.stream.Collectors;                                                 // Utility class providing Collector implementations for stream reduction

import javax.swing.JButton;                                                         // Standard Swing push-button component
import javax.swing.JLabel;                                                          // Non-interactive text or image display component
import javax.swing.JOptionPane;                                                     // Provides standard dialog boxes for user messages and confirmations

import com.rma.client.Browser;                                                      // RMA application browser providing access to the main frame
import com.rma.factories.DeleteManagerFactory;                                      // Factory for deleting RMA Manager objects from the project
import com.rma.io.FileManagerImpl;                                                  // Concrete implementation of the RMA file manager
import com.rma.io.RmaFile;                                                          // RMA abstraction representing a file or directory path
import com.rma.model.Manager;                                                       // Base interface for RMA managed data objects
import com.rma.model.Project;                                                       // Represents the currently loaded RMA project and its data

import hec.gui.NameDescriptionPanel;                                                // HEC GUI panel for entering a name and description pair

import hec2.wat.factories.NewAnalysisPeriodFactory;                                 // Factory for creating new WAT analysis period objects
import hec2.wat.model.WatAnalysisPeriod;                                            // Represents a WAT analysis period (time window for a simulation)
import hec2.wat.model.WatSimulation;                                                // Represents a single WAT simulation configuration

import rma.swing.ButtonCmdPanel;                                                    // Panel containing standard command buttons (OK, Cancel, etc.)
import rma.swing.ButtonCmdPanelListener;                                            // Listener interface for command button panel events
import rma.swing.RmaInsets;                                                         // Pre-defined Insets constants for consistent UI spacing
import rma.swing.RmaJComboBox;                                                      // RMA-extended combo box component with generic type support
import rma.swing.RmaJDialog;                                                        // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTable;                                                         // RMA-extended table component with additional utility methods
import rma.swing.list.RmaListModel;                                                 // List model backed by RMA data for use in combo boxes and lists
import rma.util.RMAIO;                                                              // RMA I/O utility methods for string/path/boolean operations
import usbr.wat.plugins.actionpanel.ActionsWindow;                                  // The parent Actions Window panel
import usbr.wat.plugins.actionpanel.commands.AbstractNewSimulationGroupCmd;         // Abstract command for creating a new simulation group
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;                  // Base class for all simulation group types
import usbr.wat.plugins.actionpanel.model.SimulationGroup;                          // Standard (non-forecast) simulation group model
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;                // Forecast-specific simulation group model

/**
 * Dialog for creating or editing a Simulation Group within the WTMP Action Panel.
 *
 * This dialog allows users to define a new simulation group or modify an existing one
 * by specifying a name, description, analysis period, and one or more WAT simulations.
 * It supports both standard SimulationGroup and ForecastSimGroup types by accepting
 * the relevant class and command class at runtime via setters.
 *
 * When creating a new group, the dialog uses reflection to instantiate the appropriate
 * AbstractNewSimulationGroupCmd subclass. When editing an existing group, it handles
 * addition and removal of simulations, including confirmation prompts and cascading
 * analysis period updates.
 *
 * This class extends RmaJDialog and is suppressed for serialization warnings
 * because Swing components are not consistently serializable.
 */
@SuppressWarnings("serial")
public class NewSimulationGroupDialog extends RmaJDialog {

	// Column index for the checkbox (selected/unselected) column in the simulations table
	private static final int SELECTED_COLUMN = 0;

	// Column index for the WatSimulation object column in the simulations table
	private static final int SIMULATION_COLUMN = 1;

	// Placeholder constant; intended to identify a forecast simulation group type (currently unused/null)
	private static final String ForecastSimulationGroup = null;


	// Panel for entering the simulation group name and description
	private NameDescriptionPanel _nameDescPanel;

	// Combo box for selecting an existing analysis period
	private RmaJComboBox<WatAnalysisPeriod> _apCombo;

	// Button to open the dialog for creating a new analysis period
	private JButton _newApButton;

	// Table displaying available simulations with a selection checkbox
	private RmaJTable _simTable;

	// Panel containing OK and Cancel buttons
	private ButtonCmdPanel _cmdPanel;

	// Flag indicating whether the dialog was closed via Cancel (true) or OK (false)
	protected boolean _canceled;

	// The simulation group being edited; null when creating a new group
	private AbstractSimulationGroup _simGroup;

	// The concrete class type of the simulation group to create (e.g., SimulationGroup or ForecastSimGroup)
	private Class<? extends AbstractSimulationGroup> _simGroupClass;

	// The concrete command class used to execute the group creation
	private Class<? extends AbstractNewSimulationGroupCmd> _simGroupCmdClass;

	// Flag indicating whether a data extract step should be run when creating child simulations
	private boolean _runExtract;

	/**
	 * Constructs a NewSimulationGroupDialog with the specified parent, modality, and title.
	 * <p>
	 * Builds all UI controls, attaches event listeners, sizes the dialog to its preferred
	 * size, and centers it relative to the parent window.
	 *
	 * @param parent the ActionsWindow that owns this dialog
	 * @param modal  true if the dialog should block input to other windows while open
	 * @param title  the text to display in the dialog title bar
	 */
	public NewSimulationGroupDialog(ActionsWindow parent, boolean modal, String title) {
		// Initialize the parent RmaJDialog with modality setting
		super(parent, modal);

		// Apply the provided title to the dialog window
		setTitle(title);

		// Build and arrange all UI components
		buildControls();

		// Attach action and event listeners to interactive components
		addListeners();

		// Resize the dialog to fit its preferred layout size
		pack();

		// Center the dialog relative to its parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 * <p>
	 * Uses a GridBagLayout to arrange: a name/description panel, an analysis period
	 * label and combo box with a "new" button, a simulation selection table, and an
	 * OK/Cancel button panel.
	 */
	private void buildControls() {
		// Set the content pane to use GridBagLayout for flexible component placement
		getContentPane().setLayout(new GridBagLayout());

		// Create the name and description input panel
		_nameDescPanel = new NameDescriptionPanel();

		// Initialize GridBagConstraints for layout positioning
		GridBagConstraints gbc = new GridBagConstraints();

		// Position the name/description panel to span the full row width
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5005;
		getContentPane().add(_nameDescPanel, gbc);


		// Create the "Analysis Period:" label for the combo box row
		JLabel label = new JLabel("Analysis Period:");

		// Configure the label to occupy one cell without horizontal expansion
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the analysis period combo box
		_apCombo = new RmaJComboBox<>();

		// Associate the label with the combo box for accessibility
		label.setLabelFor(_apCombo);

		// Configure the combo box to expand horizontally and fill available space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_apCombo, gbc);

		// Create the "..." button for launching the new analysis period creator
		_newApButton = new JButton("...");
		_newApButton.setToolTipText("Create New Analysis Period");

		// Position the new analysis period button at the end of the combo row
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_newApButton, gbc);

		// Define the column headers for the simulation selection table
		String[] headers = new String[]{"Select", "Simulation", "Description"};

		// Create the simulation table with the defined headers
		_simTable = new RmaJTable(this, headers);

		// Add extra row height for readability
		_simTable.setRowHeight(_simTable.getRowHeight() + 5);

		// Enable checkbox-based selection in the first (Select) column
		_simTable.setCheckBoxCellEditor(0);

		// Make the Simulation and Description columns read-only
		_simTable.setColumnEnabled(false, 1);
		_simTable.setColumnEnabled(false, 2);

		// Configure the table to fill all remaining space in the dialog
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_simTable.getScrollPane(), gbc);


		// Create the OK/Cancel button panel using the standard button set
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);

		// Position the button panel at the bottom of the dialog, spanning the full width
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * Attaches event listeners to interactive UI components.
	 *
	 * Registers an action listener on the new analysis period button and a
	 * ButtonCmdPanelListener on the command panel to handle OK and Cancel events.
	 */
	private void addListeners() {
		// Attach a lambda listener to the "..." button to trigger analysis period creation
		_newApButton.addActionListener(e -> createAnalysisPeriodAction());

		// Attach a listener to the OK/Cancel panel to handle user confirmation or cancellation
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate input and save the form when OK is clicked
						saveForm();

						break;
					case ButtonCmdPanel.CANCEL_BUTTON:
						// Mark the dialog as canceled and hide it
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Validates input and saves the form by either creating or updating a simulation group.
	 *
	 * Displays a wait cursor during the operation and restores the default cursor
	 * in a finally block to ensure it is always reset regardless of outcome.
	 */
	protected void saveForm() {
		// Show the wait cursor to indicate a potentially long-running operation
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Proceed only if user-entered data passes validation checks
			if (isValidData()) {
				if (_simGroup == null) {
					// No existing group: attempt to create a new simulation group
					if (createSimulationGroup()) {
						// Creation succeeded; mark as not canceled and close the dialog
						_canceled = false;
						setVisible(false);
					}
				} else {
					// Existing group: attempt to update the simulation group
					if (updateSimulationGroup()) {
						// Update succeeded; mark as not canceled and close the dialog
						_canceled = false;
						setVisible(false);
					}
				}
			}
		} finally {
			// Always restore the default cursor when the operation completes
			setCursor(Cursor.getDefaultCursor());
		}
	}


	/**
	 * Handles the action of creating a new analysis period.
	 *
	 * Invokes the application browser to open the new analysis period creation manager,
	 * then refreshes the analysis period combo box to include any newly created period.
	 *
	 * @return void (declared with void return; return type in original Javadoc was misleading)
	 */
	private void createAnalysisPeriodAction() {
		// Open the new analysis period manager dialog via the browser frame
		Manager mgr = Browser.getBrowserFrame().createNewManager(new NewAnalysisPeriodFactory());

		// Refresh the analysis period combo box to reflect the newly added period
		fillAnalysisPeriodCombo();
	}


	/**
	 * Populates the dialog controls for a new simulation group creation.
	 *
	 * Loads available analysis periods into the combo box and populates the
	 * simulation table with unassigned simulations from the current project.
	 */
	public void fillForm() {
		// Populate the analysis period combo box with current project periods
		fillAnalysisPeriodCombo();

		// Populate the simulation table with available simulations
		fillTable();
	}

	/**
	 * Populates the simulation table with WAT simulations that are not already
	 * assigned to any existing simulation group.
	 *
	 * Retrieves all simulations from the current project and filters out those
	 * that are already part of a SimulationGroup or ForecastSimGroup. Only base
	 * WatSimulation instances (not FRA subclasses) are included.
	 */
	private void fillTable() {
		// Get the currently loaded project
		Project proj = Project.getCurrentProject();

		// Clear any existing rows from the simulation table
		_simTable.deleteCells();

		// Retrieve all WatSimulation instances in the project
		List<WatSimulation> sims = proj.getManagerListForType(WatSimulation.class);

		WatSimulation sim;
		Vector row;

		// Retrieve all standard simulation groups in the project
		List<SimulationGroup> simGroups = proj.getManagerListForType(SimulationGroup.class);

		// Retrieve all forecast simulation groups in the project
		List<ForecastSimGroup> fsimGroups = proj.getManagerListForType(ForecastSimGroup.class);

		// Combine both group types into one list for membership checks
		List<AbstractSimulationGroup> allSimGroups = new ArrayList<>();
		allSimGroups.addAll(simGroups);
		allSimGroups.addAll(fsimGroups);

		// Iterate over all simulations and add only eligible ones to the table
		for (int i = 0; i < sims.size(); i++) {
			sim = sims.get(i);

			// Include only base WatSimulation instances that are not yet in any group
			if (sim.getClass().equals(WatSimulation.class) && !simPartOfGroup(sim, allSimGroups)) {
				// Build a table row with an unchecked checkbox, the simulation, and its description
				row = new Vector();
				row.add(Boolean.FALSE);
				row.add(sim);
				row.add(sim.getDescription());
				_simTable.appendRow(row);
			}
		}
	}

	/**
	 * Populates the dialog for editing an existing simulation group.
	 * <p>
	 * Loads available analysis periods, pre-selects the simulations already in the
	 * group, sets the selected analysis period, and switches the dialog title to
	 * indicate edit mode. The name field is made read-only because renaming a group
	 * is not supported.
	 *
	 * @param simGroup the existing AbstractSimulationGroup to populate the form with
	 */
	public void fillForm(AbstractSimulationGroup simGroup) {
		// Populate the analysis period combo box with current project periods
		fillAnalysisPeriodCombo();

		// Store a reference to the group being edited
		_simGroup = simGroup;

		// Populate the table with eligible simulations
		fillTable();

		// Update the title to reflect that we are editing rather than creating
		setTitle("Edit Simulation Group ");

		// Pre-fill the name and description fields from the existing group
		_nameDescPanel.setName(simGroup.getName());

		// Disable name editing since renaming a group is not supported
		_nameDescPanel.setNameEditable(false);
		_nameDescPanel.setDescription(simGroup.getDescription());

		// Retrieve the simulations already assigned to this group
		List<WatSimulation> sims = _simGroup.getSimulations();
		WatSimulation sim;

		// Check each group simulation's checkbox in the table
		for (int i = 0; i < sims.size(); i++) {
			sim = sims.get(i);
			selectSimulation(sim);
		}

		// Set the combo box to the group's current analysis period
		WatAnalysisPeriod ap = _simGroup.getAnalysisPeriod();
		_apCombo.setSelectedItem(ap);
	}

	/**
	 * Finds the row in the simulation table corresponding to the given simulation
	 * and sets its checkbox to selected.
	 * <p>
	 * The lookup strips the group name suffix from the simulation's current name
	 * to match its original base name as it appears in the table.
	 *
	 * @param sim the WatSimulation to mark as selected in the table
	 */
	private void selectSimulation(WatSimulation sim) {
		// Get the total number of rows currently in the table
		int rows = _simTable.getRowCount();

		// Strip the group name suffix to get the base simulation name used in the table
		String origSimName = getOriginalSimName(sim.getName());

		WatSimulation tblSim;

		// Iterate through table rows to find the matching simulation
		for (int r = 0; r < rows; r++) {
			tblSim = (WatSimulation) _simTable.getValueAt(r, SIMULATION_COLUMN);

			// Check if this row's simulation name matches the base name
			if (tblSim.getName().equals(origSimName)) {
				// Set the checkbox in the Selected column to true
				_simTable.setValueAt(Boolean.TRUE, r, SELECTED_COLUMN);
				return;
			}
		}
	}


	/**
	 * Populates the analysis period combo box with all analysis periods from the current project.
	 * <p>
	 * Replaces the combo box model entirely to reflect any newly added periods.
	 */
	private void fillAnalysisPeriodCombo() {
		// Get the currently loaded project
		Project proj = Project.getCurrentProject();

		// Retrieve all WatAnalysisPeriod instances in the project
		List<WatAnalysisPeriod> aps = proj.getManagerListForType(WatAnalysisPeriod.class);

		// Build a new list model with a blank entry at the top (true = include blank)
		RmaListModel newmodel = new RmaListModel(true, aps);

		// Apply the new model to the combo box, replacing any previous content
		_apCombo.setModel(newmodel);
	}


	/**
	 * Checks whether the given simulation is already assigned to any simulation group.
	 *
	 * @param sim       the WatSimulation to check
	 * @param simGroups the list of all AbstractSimulationGroup instances to search
	 * @return true if the simulation belongs to at least one group; false otherwise
	 */
	private boolean simPartOfGroup(WatSimulation sim, List<AbstractSimulationGroup> simGroups) {
		int size = simGroups.size();
		AbstractSimulationGroup simGroup;

		// Iterate over all simulation groups to look for membership
		for (int i = 0; i < size; i++) {
			simGroup = simGroups.get(i);

			// Skip null entries that may exist in the list
			if (simGroup == null) {
				continue;
			}

			// Return true immediately if the simulation is found in this group
			if (simGroup.containsSimulation(sim)) {
				return true;
			}
		}

		// No group contained the simulation
		return false;
	}


	/**
	 * Creates a new simulation group using the configured command class and the
	 * user's current form inputs.
	 *
	 * Uses Java reflection to instantiate the appropriate AbstractNewSimulationGroupCmd
	 * subclass, executes the command, and stores the resulting group.
	 *
	 * @return true if the group was successfully created; false if an error occurred
	 */
	protected boolean createSimulationGroup() {
		// Collect the user-entered name, description, and selected analysis period
		String name = _nameDescPanel.getName();
		String desc = _nameDescPanel.getDescription();

		// Determine the folder where simulation group files should be stored
		RmaFile file = getSimulationGroupFolder();

		// Get the selected analysis period from the combo box
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();

		// Get the list of simulations the user has checked in the table
		List<WatSimulation> sims = getSelectedSimulations();

		// Define the parameter types expected by the command constructor
		Class<?>[] paramClasses = new Class[]{Project.class, String.class, String.class, RmaFile.class,
				WatAnalysisPeriod.class, List.class};

		// Use reflection to look up the command constructor with the defined parameter types
		Constructor<? extends AbstractNewSimulationGroupCmd> ctor;
		try {
			ctor = _simGroupCmdClass.getConstructor(paramClasses);
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		// Use reflection to instantiate the command with the collected arguments
		AbstractNewSimulationGroupCmd cmd;
		try {
			cmd = ctor.newInstance(Project.getCurrentProject(), name, desc, file, ap, sims);
		} catch (InstantiationException | IllegalAccessException
		         | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		// Execute the command to create the simulation group in the project
		cmd.doCommand();

		// Retrieve the newly created group from the command for later reference
		_simGroup = cmd.getSimulationGroup();

		// Return true only if the group was successfully created
		return _simGroup != null;
	}

	/**
	 * Updates the existing simulation group with modified form values.
	 *
	 * Handles analysis period changes (cascading to child simulations), addition of
	 * newly selected simulations, and deletion of de-selected simulations. Prompts
	 * the user before removing simulations that may have computed results.
	 *
	 * @return true if the update completed successfully; false if the user canceled the removal confirmation
	 */
	private boolean updateSimulationGroup() {
		// Apply the updated description to the simulation group
		String desc = _nameDescPanel.getDescription();
		_simGroup.setDescription(desc);

		// Get the newly selected analysis period from the combo box
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();

		// Get the current list of simulations in the group
		List<WatSimulation> sgSims = _simGroup.getSimulations();

		// If the analysis period has changed, update the group and all child simulations
		if (ap != _simGroup.getAnalysisPeriod()) {
			_simGroup.setAnalysisPeriod(ap);

			WatSimulation sim;
			for (int i = 0; i < sgSims.size(); i++) {
				sim = sgSims.get(i);

				// Propagate the new analysis period to each simulation's container
				sim.getContainerParent().setAnalysisPeriod(ap);
				sim.getContainerParent().setModified(true);

				// Clear any previously computed results since the period has changed
				sim.clearHasComputed();
			}
		}

		// Get the simulations currently checked in the table
		List<WatSimulation> selectedSims = getSelectedSimulations();

		// Build name lists for comparison between selected and existing simulations
		List<String> selectedSimNames = selectedSims.stream().map(s -> s.getName()).collect(Collectors.toList());
		List<String> existingSimNames = sgSims.stream().map(s -> s.getName()).collect(Collectors.toList());

		// Convert existing group simulation names back to their original base names
		List<String> origExistingSimNames = getOriginalNames(existingSimNames);

		// Determine which simulations are being removed (exist in group but not in new selection)
		Set<String> removedSet = new HashSet<>(origExistingSimNames);
		removedSet.removeAll(selectedSimNames);

		// If any simulations are being removed, prompt the user for confirmation
		if (!removedSet.isEmpty()) {
			// Build a confirmation message listing each simulation to be removed
			StringBuilder msg = new StringBuilder();
			msg.append("The following Simulations are being removed from the Simulation Group " + _simGroup.getName() + ":\n");
			Iterator<String> iter = removedSet.iterator();
			while (iter.hasNext()) {
				msg.append("\n");
				msg.append(iter.next());
			}
			msg.append("\n\nThis will remove any results that have the simulation may have produced.\nDo you want to continue?");

			// Show the confirmation dialog and return false if the user declines
			int opt = JOptionPane.showConfirmDialog(this, msg, "Confirm Removal", JOptionPane.YES_NO_OPTION);
			if (opt != JOptionPane.YES_OPTION) {
				return false;
			}
		}

		// Determine which simulations are newly selected (in selection but not in existing group)
		Set<String> set = new HashSet<>(selectedSimNames);
		set.removeAll(origExistingSimNames);

		// Iterate over newly selected simulations and add them to the group
		Iterator<String> iter = set.iterator();
		WatSimulation simToAdd, newSim;
		Project proj = Project.getCurrentProject();
		while (iter.hasNext()) {
			String newSimName = iter.next();

			// Find the simulation object from the table by name
			simToAdd = findSimulationInTable(newSimName);

			if (simToAdd != null) {
				// Create a group-linked copy of the simulation
				newSim = AbstractNewSimulationGroupCmd.createSimulation(simToAdd, _simGroup, proj, ap, _runExtract);

				if (newSim != null) {
					// Add the new simulation to the group
					_simGroup.addSimulation(newSim);
				}
			}
		}

		// Determine which simulations should be deleted (were in the group but are no longer selected)
		set = new HashSet<>(origExistingSimNames);
		set.removeAll(selectedSimNames);
		iter = set.iterator();

		// Iterate over simulations to delete and remove them from the group
		while (iter.hasNext()) {
			String delSimName = iter.next();

			// Find the simulation in the group by its original base name
			WatSimulation simToDel = findSimGroupSimulationByOrigName(delSimName);

			if (simToDel != null) {
				// Delete the simulation via the delete manager and remove it from the group
				if (DeleteManagerFactory.deleteManager(simToDel)) {
					_simGroup.removeSimulation(simToDel);
				}
			}
		}

		// Mark the group as modified so changes are persisted
		_simGroup.setModified(true);

		return true;
	}


	/**
	 * Finds a simulation within the current simulation group by its original base name.
	 *
	 * Constructs the group-qualified simulation name from the base name and group name,
	 * then searches the group's simulation list for a match.
	 *
	 * @param baseSimName the original (pre-group-suffix) simulation name to search for
	 * @return the matching WatSimulation if found; null otherwise
	 */
	private WatSimulation findSimGroupSimulationByOrigName(String baseSimName) {
		// Retrieve all simulations currently in the group
		List<WatSimulation> sims = _simGroup.getSimulations();

		// Construct the expected group-qualified name for this base name
		String groupSimName = AbstractNewSimulationGroupCmd.getGroupSimName(baseSimName, _simGroup.getName());

		WatSimulation sim;
		for (int i = 0; i < sims.size(); i++) {
			sim = sims.get(i);

			// Return the simulation if its name matches the expected group-qualified name
			if (groupSimName.equals(sim.getName())) {
				return sim;
			}
		}

		// No matching simulation found in the group
		return null;
	}


	/**
	 * Searches the simulation table for a row whose simulation name matches the given name.
	 *
	 * @param newSimName the simulation name to search for in the table
	 * @return the matching WatSimulation object if found; null otherwise
	 */
	private WatSimulation findSimulationInTable(String newSimName) {
		int numRows = _simTable.getRowCount();
		WatSimulation sim;

		// Iterate over all rows in the table
		for (int r = 0; r < numRows; r++) {
			sim = (WatSimulation) _simTable.getValueAt(r, SIMULATION_COLUMN);

			// Return the simulation if its name matches the target
			if (newSimName.equals(sim.getName())) {
				return sim;
			}
		}

		// No matching simulation found in the table
		return null;
	}


	/**
	 * Converts a list of group-qualified simulation names back to their original base names
	 * by stripping the group name suffix from each entry.
	 *
	 * @param existingSimNames the list of group-qualified simulation names
	 * @return a list of original base simulation names with group suffixes removed
	 */
	private List<String> getOriginalNames(List<String> existingSimNames) {
		List<String> baseSimNames = new ArrayList<>();

		for (int i = 0; i < existingSimNames.size(); i++) {
			String simName = existingSimNames.get(i);

			// Strip the group name suffix to recover the base simulation name
			String baseSimulationName = getOriginalSimName(simName);
			baseSimNames.add(baseSimulationName);
		}

		return baseSimNames;
	}


	/**
	 * Strips the group name suffix from a simulation name to retrieve its original base name.
	 *
	 * Group-linked simulations are named in the format "baseSimName-groupName". This method
	 * removes the "-groupName" portion to recover the original simulation name.
	 *
	 * @param simName the group-qualified simulation name (e.g., "mySim-myGroup")
	 * @return the original base simulation name with the group suffix removed
	 */
	private String getOriginalSimName(String simName) {
		// Get the name of the current simulation group
		String groupName = _simGroup.getName();

		// Remove the "-groupName" suffix from the simulation name and return the result
		return RMAIO.replace(simName, "-" + groupName, "");
	}


	/**
	 * Returns the RmaFile reference to the directory where simulation group files are stored.
	 *
	 * Constructs the path as: [projectDirectory]/wat/simGroups
	 *
	 * @return an RmaFile representing the simulation group folder
	 */
	private static RmaFile getSimulationGroupFolder() {
		// Start with the current project's root directory
		String dir = Project.getCurrentProject().getProjectDirectory();

		// Append the "wat" subdirectory to the project path
		dir = RMAIO.concatPath(dir, "wat");

		// Append the "simGroups" subdirectory to reach the target folder
		dir = RMAIO.concatPath(dir, "simGroups");

		// Return an RmaFile for the constructed directory path
		return FileManagerImpl.getFileManager().getFile(dir);
	}

	/**
	 * Returns whether the dialog was closed by the user selecting Cancel.
	 *
	 * @return true if the dialog was canceled; false if it was confirmed with OK
	 */
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Validates user-entered form data before a save operation.
	 *
	 * Checks for duplicate group names (when creating), a selected analysis period,
	 * and at least one selected simulation. Displays an informational message dialog
	 * for each validation failure.
	 *
	 * @return true if all validation checks pass; false if any check fails
	 */
	protected boolean isValidData() {
		Project proj = Project.getCurrentProject();

		// Only validate the name for uniqueness when creating a new group (not editing)
		if (_simGroup == null) {
			String name = _nameDescPanel.getName();

			// Check if a SimulationGroup or ForecastSimGroup with this name already exists
			if (proj.getManagerProxy(name, SimulationGroup.class) != null ||
					proj.getManagerProxy(name, ForecastSimGroup.class) != null) {
				// Inform the user that the name is already taken
				JOptionPane.showMessageDialog(this, "A Simulation Group named " + name + " already exists. Please enter a unique name",
						"Duplicate Name", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}

		// Validate that an analysis period has been selected
		WatAnalysisPeriod ap = (WatAnalysisPeriod) _apCombo.getSelectedItem();
		if (ap == null) {
			// Inform the user that an analysis period is required
			JOptionPane.showMessageDialog(this, "No Analysis Period has been selected. Please select an Analysis Period",
					"No Analaysis Period", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Validate that at least one simulation has been selected
		List<WatSimulation> selectedSimulations = getSelectedSimulations();
		if (selectedSimulations.isEmpty()) {
			// Inform the user that at least one simulation must be selected
			JOptionPane.showMessageDialog(this, "No Simulations have been selected. Please select at least one Simulation",
					"No Simulations", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// All validations passed
		return true;
	}

	/**
	 * Retrieves the list of WatSimulation objects whose checkbox is checked in the table.
	 *
	 * Iterates all table rows and collects simulations where the Selected column value
	 * parses to true.
	 *
	 * @return a List of WatSimulation objects currently selected in the table; empty if none
	 */
	private List<WatSimulation> getSelectedSimulations() {
		List<WatSimulation> selectedSims = new ArrayList<>();
		int rowCnt = _simTable.getRowCount();
		Object obj;
		WatSimulation sim;

		// Check each row's selection state
		for (int r = 0; r < rowCnt; r++) {
			obj = _simTable.getValueAt(r, SELECTED_COLUMN);

			// Skip rows with null selection values
			if (obj == null) {
				continue;
			}

			// Add the simulation to the result list if its checkbox is checked
			if (RMAIO.parseBoolean(obj.toString(), false)) {
				sim = (WatSimulation) _simTable.getValueAt(r, SIMULATION_COLUMN);
				selectedSims.add(sim);
			}
		}

		return selectedSims;
	}

	/**
	 * Returns the simulation group that was created or edited by this dialog.
	 *
	 * @return the AbstractSimulationGroup instance; null if the dialog was canceled
	 * or a group was not successfully created
	 */
	public AbstractSimulationGroup getSimulationGroup() {
		return _simGroup;
	}


	/**
	 * Sets the concrete simulation group class that will be instantiated by this dialog.
	 *
	 * @param simGroupClass the Class object for the desired AbstractSimulationGroup subtype
	 */
	public void setSimulationGroupClass(Class<? extends AbstractSimulationGroup> simGroupClass) {
		_simGroupClass = simGroupClass;
	}

	/**
	 * Sets the command class used to execute the simulation group creation.
	 * <p>
	 * The provided class must have a constructor compatible with the parameter types
	 * expected by createSimulationGroup().
	 *
	 * @param cmdClass the Class object for the desired AbstractNewSimulationGroupCmd subtype
	 */
	public void setSimulationGroupFactory(Class<? extends AbstractNewSimulationGroupCmd> cmdClass) {
		_simGroupCmdClass = cmdClass;
	}

	/**
	 * Sets whether a data extract step should be performed when new child simulations
	 * are created during a group update.
	 *
	 * @param runExtract true to run the extract step; false to skip it
	 */
	public void setRunExtract(boolean runExtract) {
		_runExtract = runExtract;
	}
}