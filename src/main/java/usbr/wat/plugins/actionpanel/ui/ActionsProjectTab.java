package usbr.wat.plugins.actionpanel.ui;

import java.awt.EventQueue;          // Provides invokeLater for scheduling work on the Event Dispatch Thread
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout

import javax.swing.JButton;          // Swing button component used to trigger the actions window

import com.rma.event.ProjectEvent;   // Carries the Project reference delivered by open/close notifications
import com.rma.model.Project;        // Represents the currently open WAT study; getNoProject() gives the empty state
import com.rma.ui.ContentTree;       // RMA host component passed to the project tree at construction time
import com.rma.ui.ProjectTab;        // RMA base class for a named tab panel shown in the project browser
import com.rma.ui.ProjectTree;       // RMA tree component that displays the project hierarchy
import com.rma.ui.ProjectTreeModel;  // Tree model whose root node is replaced when a project opens or closes

import rma.swing.RmaInsets;                                         // Constants for common GridBagLayout inset configurations
import usbr.wat.plugins.actionpanel.actions.ActionWindowAction;     // Swing Action that opens the WTMP actions window when triggered


/**
 * Project browser tab that hosts the WTMP project tree and an actions window button.
 *
 * This class extends the RMA ProjectTab to provide the WTMP-specific project
 * navigation panel. It is responsible for:
 *
 *   Creating a WtmpTree as the tab's project tree component.
 *   Rebuilding the tree model root whenever a project is opened or closed,
 *   keeping the displayed hierarchy in sync with the active study.
 *   Adding an "Open Actions Window" button at the bottom of the tab so users
 *   can launch the WTMP actions window directly from the project browser.
 *
 * A single instance is tracked via a static field and exposed through getTab()
 * so other components can obtain a reference without holding one explicitly.
 *
 */
@SuppressWarnings("serial")
public class ActionsProjectTab extends ProjectTab {
	/**
	 * Singleton-style reference to the most recently constructed instance of this tab.
	 */
	private static ActionsProjectTab _instance;

	/**
	 * The WTMP-specific project tree displayed within this tab.
	 */
	private WtmpTree _wtmpTree;


	/**
	 * Constructs the tab and registers this instance as the current static reference.
	 *
	 * The RMA framework instantiates this class reflectively when the plugin registers
	 * its tab contribution, so the constructor stores the instance for later retrieval
	 * via getTab().
	 */
	public ActionsProjectTab() {
		super();

		// Store this instance so other components can retrieve it via the static accessor
		_instance = this;
	}


	/**
	 * Creates and returns the WTMP project tree to be hosted inside this tab.
	 *
	 * Called by the RMA framework during tab initialisation. The WtmpTree is stored
	 * as a field so it can be passed when constructing WtmpTreeNode root nodes during
	 * project open and close events.
	 *
	 * @param contentTree the RMA content tree host provided by the framework
	 * @return the newly created WtmpTree instance
	 */
	@Override
	protected ProjectTree createProjectTree(ContentTree contentTree) {
		// Construct the WTMP tree and retain a reference for use in project event handlers
		_wtmpTree = new WtmpTree(contentTree);
		return _wtmpTree;
	}


	/**
	 * Updates the project tree to reflect a newly opened project and expands all nodes.
	 *
	 * A new WtmpTreeNode rooted at the opened project is set as the tree model's root,
	 * replacing any previously displayed content. The full tree is then expanded on the
	 * EDT via invokeLater so the layout has finished updating before the expand runs.
	 *
	 * @param evt the project event carrying the project that was just opened
	 */
	public void projectOpened(ProjectEvent evt) {
		Project proj = evt.getProject();

		// Replace the tree root with a node built from the newly opened project
		((ProjectTreeModel) getProjectTree().getModel())
				.setRoot(new WtmpTreeNode(proj, null, _wtmpTree));

		// Schedule full expansion after the EDT has processed the model change
		EventQueue.invokeLater(() -> getProjectTree().expandAll());
	}


	/**
	 * Extends the base tab layout by adding an "Open Actions Window" button at the bottom.
	 *
	 * Calls the superclass implementation first to ensure the standard tab controls are
	 * built, then appends the button anchored to the south edge of the tab panel.
	 */
	@Override
	protected void buildControls() {
		// Build the standard RMA project tab controls (tree, toolbar, etc.)
		super.buildControls();

		// Create the action that will open the WTMP actions window when the button is clicked
		ActionWindowAction action = new ActionWindowAction();

		// Place the button in the last row, spanning the full width, anchored to the bottom
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 10;   // High row index ensures the button appears below all tree content
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		add(new JButton(action), gbc);
	}


	/**
	 * Resets the project tree to an empty state when the active project is closed.
	 *
	 * Replaces the tree model root with a WtmpTreeNode built from the framework's
	 * "no project" placeholder, effectively clearing all project-specific nodes
	 * from the tree.
	 *
	 * @param evt the project event signalling that the current project was closed
	 */
	public void projectClosed(ProjectEvent evt) {
		// Obtain the empty-project sentinel used when no study is open
		Project proj = Project.getNoProject();

		// Reset the tree root to an empty node so the hierarchy shows no content
		((ProjectTreeModel) getProjectTree().getModel())
				.setRoot(new WtmpTreeNode(proj, null, _wtmpTree));
	}


	/**
	 * Returns the most recently created instance of this tab.
	 *
	 * This accessor allows other components to obtain the tab reference without
	 * needing to hold an explicit reference. Returns null if no instance has been
	 * constructed yet.
	 *
	 * @return the current ActionsProjectTab instance, or null if not yet initialised
	 */
	public static ActionsProjectTab getTab() {
		return _instance;
	}
}
