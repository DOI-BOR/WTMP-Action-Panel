package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;                                             // Provides GridBagConstraints for specifying layout parameters in a GridBagLayout
import java.awt.GridBagLayout;                                                  // Provides GridBagLayout as the layout manager for the dialog content pane
import java.awt.Window;                                                         // Provides Window as the parent component type accepted by the superclass constructor
import java.awt.event.ActionEvent;                                              // Provides ActionEvent for the ButtonCmdPanelListener Close button callback

import java.util.Iterator;                                                      // Provides Iterator for traversing the ensemble set indexing map entries
import java.util.Map;                                                           // Provides Map for holding the ensemble set name-to-index-array mappings returned by the sim group
import java.util.Set;                                                           // Provides Set for accessing the entry set of the ensemble indexing map
import java.util.Vector;                                                        // Provides Vector as the row data container required by RmaJTable's row append methods

import javax.swing.JLabel;                                                      // Provides JLabel for the "Simulation:" header and the dynamic simulation name label
import javax.swing.table.TableModel;                                            // Provides TableModel as the type parameter for the TableRowSorter applied to the ensemble table
import javax.swing.table.TableRowSorter;                                        // Provides TableRowSorter for enabling column-based sorting on the ensemble table

import hec.util.NumericComparator;                                              // Provides NumericComparator for correctly sorting the integer F-part index columns numerically
import hec2.wat.model.WatSimulation;                                            // Provides WatSimulation for identifying which simulation's ensemble F-part indexing is displayed

import rma.swing.ButtonCmdPanel;                                                // Provides ButtonCmdPanel for the Close button row at the bottom of the dialog
import rma.swing.ButtonCmdPanelListener;                                        // Provides ButtonCmdPanelListener for handling the Close button click event
import rma.swing.RmaInsets;                                                     // Provides RmaInsets for standard inset constants used in GridBagConstraints
import rma.swing.RmaJDialog;                                                    // Provides RmaJDialog as the base non-modal dialog class this window extends
import rma.swing.RmaJTable;                                                     // Provides RmaJTable as the base Swing table class for the ensemble F-part display table

import usbr.wat.plugins.actionpanel.ActionsWindow;                              // Provides ActionsWindow; retained for potential future reference within this package
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;            // Provides ForecastSimGroup for retrieving the ensemble set F-part index map

/**
 * A non-modal informational dialog that displays the HEC-DSS F-part collection start
 * and end indices for each ensemble set associated with a given WAT simulation.
 *
 * The dialog presents a three-column read-only table showing:
 *
 *   Ensemble Set — the name of the ensemble set.
 *   Collection Start — the integer F-part index at which the ensemble collection begins.
 *   Collection End — the integer F-part index at which the ensemble collection ends.
 *
 * Both index columns use a {@link NumericComparator} so that column sorting treats
 * their values as numbers rather than strings. The dialog has a single Close button
 * and does not modify any data.
 *
 * @see ForecastSimGroup#getSimulationEnsembleSetIndexing(WatSimulation)
 * @see NumericComparator
 */
public class EnsembleFPartMapDlg extends RmaJDialog {
	// Dynamic label that shows the name of the simulation whose F-part mapping is displayed
	private JLabel _simulationLabel;

	// Read-only three-column table displaying ensemble set names and their F-part index ranges
	private RmaJTable _ensembleTable;

	// Close button row at the bottom of the dialog
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Constructs an {@code EnsembleFPartMapDlg} non-modal dialog, builds all controls,
	 * wires the Close button listener, packs the dialog to its preferred size, and
	 * centres it over the parent window.
	 *
	 * @param parent the {@link Window} over which this dialog is centred; passed to the
	 *               {@link RmaJDialog} superclass constructor; {@code false} is passed
	 *               for the modal flag so the dialog is non-blocking
	 */
	public EnsembleFPartMapDlg(Window parent) {
		// Initialise the superclass as a non-modal dialog
		super(parent, false);

		// Build and lay out all Swing controls
		buildControls();

		// Attach the Close button listener
		addListeners();

		// Size the dialog to its preferred dimensions and centre it over the parent
		pack();
		setLocationRelativeTo(getParent());
	}

	/**
	 * Builds and lays out all Swing controls within the dialog's content pane.
	 *
	 * The layout consists, top to bottom, of:
	 *
	 *   A "Simulation:" static label and a dynamic simulation name label side by side.
	 *   A read-only three-column {@link RmaJTable} listing ensemble set names and
	 *       their F-part collection start and end indices, with integer cell editors
	 *       installed on the index columns.
	 *   A {@link ButtonCmdPanel} containing a single Close button.
	 *
	 */
	private void buildControls() {
		// Set the dialog title
		setTitle("Ensemble DSS F-Parts");

		// Use GridBagLayout for flexible component placement in the content pane
		getContentPane().setLayout(new GridBagLayout());

		// Add the static "Simulation:" label in the first column of the header row
		JLabel label = new JLabel("Simulation:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		// Add the dynamic simulation name label in the second column; it stretches horizontally
		_simulationLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_simulationLabel, gbc);

		// Build the read-only ensemble table with three columns
		String[] headers = new String[]{"Ensemble Set", "Collection Start", "Collection End"};
		_ensembleTable = new RmaJTable(this, headers) {
			// Prevent all cells from being edited inline
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};

		// Install integer cell editors on the index columns to ensure correct rendering
		_ensembleTable.setIntegerCellEditor(1);
		_ensembleTable.setIntegerCellEditor(2);

		// Add the ensemble table scroll pane; it claims all remaining vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_ensembleTable.getScrollPane(), gbc);

		// Add the Close button panel anchored to the bottom-left of its cell
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		add(_cmdPanel, gbc);
	}

	/**
	 * Wires the Close button listener, which hides the dialog when clicked.
	 *
	 * The dialog is non-modal, so hiding it (rather than disposing of it) allows
	 * the caller to re-show it without rebuilding the controls.
	 */
	private void addListeners() {
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.CLOSE_BUTTON:
						// Hide the dialog; does not dispose it so it can be re-shown
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Populates the ensemble table with the F-part index mapping for the given
	 * simulation group and WAT simulation, then applies a numeric row sorter to
	 * the two index columns.
	 *
	 * The table is cleared before loading. Each entry in the ensemble set indexing
	 * map produces one row containing the ensemble set name and its collection start
	 * and end F-part indices. After all rows are appended, a {@link TableRowSorter}
	 * with a {@link NumericComparator} is installed on both index columns so that
	 * clicking a column header sorts numerically rather than lexicographically.
	 *
	 * @param simGroup   the {@link ForecastSimGroup} that provides the ensemble set
	 *                   F-part index mapping via
	 *                   {@link ForecastSimGroup#getSimulationEnsembleSetIndexing(WatSimulation)};
	 *                   must not be {@code null}
	 * @param simulation the {@link WatSimulation} whose ensemble set indexing is
	 *                   displayed; must not be {@code null}
	 */
	public void fillForm(ForecastSimGroup simGroup, WatSimulation simulation) {
		// Clear any previously displayed rows before reloading
		_ensembleTable.deleteCells();

		// Retrieve the map of ensemble set name → [collectionStart, collectionEnd] index arrays
		Map<String, int[]> esetIndexing = simGroup.getSimulationEnsembleSetIndexing(simulation);

		if (esetIndexing != null) {
			Vector row;

			// Iterate over each ensemble set entry in the map
			Set<Map.Entry<String, int[]>> entrySet = esetIndexing.entrySet();
			Iterator<Map.Entry<String, int[]>> iter = entrySet.iterator();

			while (iter.hasNext()) {
				Map.Entry<String, int[]> entry = iter.next();
				String esetName = entry.getKey();
				int[] fpartIndexs = entry.getValue();

				// Build a row vector: [ensemble set name, collection start index, collection end index]
				row = new Vector();
				row.add(esetName);
				row.add(fpartIndexs[0]);
				row.add(fpartIndexs[1]);
				_ensembleTable.appendRow(row);
			}
		}

		// Install a row sorter with numeric comparators on the two integer index columns
		// so that column-header clicks sort numerically rather than lexicographically
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(_ensembleTable.getModel());
		sorter.setComparator(1, new NumericComparator());
		sorter.setComparator(2, new NumericComparator());
		_ensembleTable.setRowSorter(sorter);
	}
}
