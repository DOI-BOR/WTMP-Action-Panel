package usbr.wat.plugins.actionpanel.actions;

import java.awt.EventQueue;														// AWT utility to schedule tasks on the Event Dispatch Thread (EDT)
import java.awt.event.ActionEvent;												// Event type delivered when a user triggers a bound action (for example, a button press)

import java.util.Iterator;														// Iterator interface for traversing collections (import present even if unused here)
import java.util.List;															// Collections interface used for lists of report info (import present even if unused here)

import javax.swing.AbstractAction;												// Swing base class for encapsulating an action attached to UI components
import javax.swing.JOptionPane;													// Swing utility for showing dialogs (import present even if unused here)

import com.rma.io.FileManagerImpl;												// File manager implementation for filesystem operations (import present even if unused here)

import usbr.wat.plugins.actionpanel.ActionsWindow;								// Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.editors.DisplayReportsSelector;				// Editor dialog that lets users select reports to create or view
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;					// Model holding per-simulation report information and paths (import present even if unused here)
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;								// Base USBR panel type implemented by calibration and forecast panels

/**
 * Action that opens the "Create Report" selector dialog.
 *
 * When triggered, this action creates the selector if needed and shows it,
 * using the provided parent window and panel for context.
 */
@SuppressWarnings("serial")
public class DisplayReportSelectorAction extends AbstractAction {
	/**
	 * Owning actions window used as parent for the selector dialog.
	 */
	private ActionsWindow _parent;

	/**
	 * Dialog that allows the user to select and configure reports.
	 */
	private DisplayReportsSelector _selector;

	/**
	 * Panel that provides context for report creation (calibration or forecast).
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Creates the display-report-selector action with a user-visible name and initial disabled state.
	 *
	 * @param parent      the actions window used as the dialog parent
	 * @param parentPanel the workflow panel that provides context for the selector
	 */
	public DisplayReportSelectorAction(ActionsWindow parent, UsbrPanel parentPanel) {
		// Initialize the action with its display label
		super("Create Report...");

		// Start disabled until the UI logic enables it (e.g., when selections are present)
		setEnabled(false);

		// Store references to the owning window and parent panel
		_parent = parent;
		_parentPanel = parentPanel;
	}

	/**
	 * Handles the user-triggered event to show the report selector.
	 *
	 * @param arg0 the action event initiating the request
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Delegate to the workflow that shows the selector dialog
		displayReportSelector();
	}

	/**
	 * Ensures the selector exists and displays it to the user.
	 * <p>
	 * If the selector has not been created yet, it is instantiated with the
	 * parent window and panel, then made visible.
	 */
	private void displayReportSelector() {
		// Lazily construct the selector dialog to avoid unnecessary initialization
		if (_selector == null) {
			_selector = new DisplayReportsSelector(_parent, _parentPanel);
		}

		// Show the selector dialog
		_selector.setVisible(true);
	}
}