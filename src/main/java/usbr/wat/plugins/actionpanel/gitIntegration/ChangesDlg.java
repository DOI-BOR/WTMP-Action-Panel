package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.GridBagConstraints;    // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;         // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;                // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent;     // Represents an action event fired when the Close button is clicked
import java.util.List;                 // Ordered collection interface for the list of changed file path strings

import javax.swing.JLabel;             // Non-interactive label displaying the "Changed Files:" caption
import javax.swing.JScrollPane;        // Scroll container wrapping the changes list for long file lists

import rma.swing.ButtonCmdPanel;       // Panel containing the Close button
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaInsets;            // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJDialog;           // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJList;             // RMA-extended list component for displaying string items
import rma.swing.list.RmaListModel;    // RMA list model backed by a collection for use in RmaJList

/**
 * Read-only dialog that displays the list of Git-changed files detected for the
 * currently selected repository within the WTMP Action Panel.
 *
 * Presents a scrollable RmaJList populated with the file path strings provided
 * at construction time. The dialog is purely informational and provides only a
 * Close button; no editing or selection actions are available.
 *
 * Typically opened from DownloadConfirmDialog or EnterCommentsDlg when the user
 * clicks the "..." button beside the local-changes count label.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class ChangesDlg extends RmaJDialog {
	// Scrollable list displaying the changed file path strings
	private RmaJList<String> _changesList;

	// Panel containing the single Close button
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Constructs a ChangesDlg owned by the given window and populated with the
	 * provided list of changed file paths.
	 *
	 * Builds all UI controls, attaches the Close button listener, populates the
	 * list, packs the dialog to its preferred size, overrides the size to a fixed
	 * 400×400, and centers it relative to the parent window.
	 *
	 * @param window  the Window that will own this modal dialog
	 * @param changes the list of changed file path strings to display
	 */
	public ChangesDlg(Window window, List<String> changes) {
		// Initialize the base RmaJDialog as modal
		super(window, true);

		// Build and arrange all UI controls
		buildControls();

		// Attach the Close button listener
		addListeners();

		// Populate the list with the provided changed file paths
		fillForm(changes);

		// Size the dialog to its preferred layout dimensions
		pack();

		// Override with a fixed dialog size for consistent appearance
		setSize(400, 400);

		// Center the dialog relative to the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Places a "Changed Files:" label, a scrollable list of changed file paths,
	 * and a Close button panel using a GridBagLayout.
	 */
	private void buildControls() {
		// Apply GridBagLayout to the content pane
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Files that have changed");

		// Create the "Changed Files:" label above the list
		JLabel label = new JLabel("Changed Files:");
		GridBagConstraints gbc = new GridBagConstraints();

		// Position the label to span the full row with no vertical expansion
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the scrollable list for displaying changed file paths
		_changesList = new RmaJList<>();

		// Position the list to fill all remaining space in the dialog
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_changesList), gbc);

		// Create the Close button panel at the bottom of the dialog
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
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
	 * Attaches a ButtonCmdPanelListener to the Close button that hides the dialog.
	 */
	private void addListeners() {
		// Hide the dialog when the Close button is clicked
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.CLOSE_BUTTON:
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Populates the changes list with the provided file path strings.
	 *
	 * Replaces the list model entirely so any previous content is discarded.
	 *
	 * @param changes the list of changed file path strings to display; may be empty
	 */
	private void fillForm(List<String> changes) {
		// Build a new list model (no blank entry) from the provided changes list
		RmaListModel<String> newModel = new RmaListModel<>(false, changes);
		_changesList.setModel(newModel);
	}
}
