package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.EventQueue;            // Swing event dispatch queue; used to defer repo selection to the EDT after window open
import java.awt.GridBagConstraints;    // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;         // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;                // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent;     // Represents an action event fired by combo box, buttons, and popup items
import java.awt.event.ActionListener;  // Listener interface used for the periodic timer action
import java.awt.event.ItemEvent;       // Event fired when the repo combo box selection changes
import java.awt.event.MouseEvent;      // Mouse event used for the repo combo box tooltip override
import java.awt.event.WindowAdapter;   // Adapter for window lifecycle events; handles open and closing behaviors
import java.awt.event.WindowEvent;     // Window event fired when the dialog opens or the OS close button is clicked
import java.io.File;                   // Represents a file system path for launching the OS file explorer
import java.io.IOException;            // Checked exception thrown when launching the file explorer fails
import java.util.List;                 // Ordered collection interface for the changes and repo lists
import java.util.concurrent.ExecutionException; // Exception thrown when a SwingWorker's get() is interrupted
import java.util.logging.Logger;       // JDK logger for recording file-explorer launch failures

import javax.swing.Icon;              // Icon interface for the animated spinner image shown during refresh
import javax.swing.JButton;           // Standard Swing push-button for the "..." edit repos button
import javax.swing.JCheckBoxMenuItem; // Checkbox menu item for the Test, Debug, and Refresh popup options
import javax.swing.JLabel;            // Non-interactive label for the "Repository Info:" caption and status message
import javax.swing.JMenuItem;         // Regular menu item for the "Browse Local Folder" popup option
import javax.swing.JPanel;            // Generic container; cast to set the component popup menu
import javax.swing.JPopupMenu;        // Right-click context menu for developer/debug options
import javax.swing.JSeparator;        // Horizontal visual divider between the repo combo and the action buttons
import javax.swing.SwingWorker;       // Background worker for running the Git changes query off the EDT
import javax.swing.Timer;             // Swing timer used for periodic automatic refresh of the change status

import com.rma.model.Project;         // Represents the currently loaded RMA project; used for directory matching

import rma.swing.ButtonCmdPanel;      // Panel containing the Close button
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaImage;            // RMA utility for loading the animated spinner GIF image icon
import rma.swing.RmaInsets;           // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJComboBox;        // RMA-extended combo box with generic type support and tooltip override
import rma.swing.RmaJDialog;          // Base class for RMA modal/non-modal dialog windows
import rma.swing.list.RmaListModel;   // RMA list model backed by a collection for use in RmaJComboBox
import rma.util.RMAIO;               // RMA I/O utility providing path comparison helpers

import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;   // Base Git action providing the DO_NOTHING_PROP and DEBUG_OUTPUT_PROP constants
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;  // Git action for querying commits ahead of the local HEAD
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;              // Data model holding the name, local path, and URL of a Git repository
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;          // Utility class for listing and managing saved repo definitions

/**
 * Primary dialog for the WTMP Action Panel's Git integration, providing an
 * interface for managing and interacting with Git repositories ("Data Storage").
 *
 * Displays a repo combo box (with a detail tooltip) and a "..." button for
 * opening the ReposEditor, a horizontal separator, a RepoButtonPanel with
 * Download/Upload/Restore/Open Study/Save Study As buttons, a status message
 * label, and a Close button.
 *
 * When a repository is selected, the dialog automatically queries Git for
 * commits behind the remote using a SwingWorker to avoid blocking the EDT.
 * A Swing Timer fires every 3 minutes to repeat the check. The message label
 * shows an animated spinner icon ("_spinnyIcon") while the check is running,
 * then displays a summary of how many commits behind the local copy is.
 *
 * A right-click context popup exposes developer options:
 *   - "Test": runs Git commands without actually sending them (DO_NOTHING_PROP).
 *   - "Debug Git Output": logs Git command output to the console.
 *   - "Refresh Status": toggles whether the periodic refresh runs.
 *   - "Browse Local Folder...": opens the repo's local directory in Windows Explorer.
 *
 * On first open, if no repos are defined, the ReposEditor is automatically shown
 * to prompt the user to add one.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class StudyStorageDialog extends RmaJDialog {
	// Dialog title shown in the title bar (also used as the base for the Test-mode suffix)
	public static final String TITLE = "Data Storage";

	// Combo box for selecting the active repository; shows name, URL, and local path in its tooltip
	private RmaJComboBox<RepoInfo> _repoCombo;

	// Button that opens the ReposEditor dialog for adding, editing, and deleting repos
	private JButton _editReposButton;

	// Panel containing the Close button
	private ButtonCmdPanel _cmdPanel;

	// Panel containing the Download, Upload, Restore, Open Study, and optional Save Study As buttons
	private RepoButtonPanel _repoButtonPanel;

	// Parent window reference stored for use in Git action constructors
	private Window _parent;

	// Status message label displaying the commit-behind count or operational messages
	private JLabel _msgLabel;

	// Popup menu item for toggling "test mode" (commands execute without touching Git)
	private JCheckBoxMenuItem _doNothingMenu;

	// Swing Timer that triggers a periodic refresh of the commit-behind status every 3 minutes
	private Timer _timer;

	// Popup menu item for toggling detailed Git command output logging to the console
	private JCheckBoxMenuItem _debugMenu;

	// Popup menu item for enabling or disabling the automatic periodic status refresh
	private JCheckBoxMenuItem _refreshMenu;

	// Flag controlling whether the periodic timer triggers a Git status refresh
	protected boolean _refreshChanges = true;

	// Popup menu item for opening the selected repo's local folder in Windows Explorer
	private JMenuItem _browseLocalMenu;

	// Flag indicating whether this is the first time the dialog is loaded (for the auto-add prompt)
	private boolean _firstTime;

	// Animated spinner GIF icon shown in the message label while a Git query is running
	protected Icon _spinnyIcon = RmaImage.getImageIcon("Images/sync1.gif");

	/**
	 * Constructs a StudyStorageDialog owned by the given parent window.
	 *
	 * Builds all UI controls, attaches listeners, packs the dialog, loads and populates
	 * the repo combo, overrides the size to 550×400, and centers it relative to the parent.
	 *
	 * @param parent the Window that will own this modal dialog
	 */
	public StudyStorageDialog(Window parent) {
		// Initialize the base RmaJDialog as modal
		super(parent, true);

		// Store the parent window for use in Git action constructors
		_parent = parent;

		// Build and arrange all UI controls
		buildControls();

		// Attach combo, button, window, and timer listeners
		addListeners();

		// Size the dialog to its preferred layout dimensions
		pack();

		// Load saved repo definitions and populate the combo box
		loadRepos();

		// Override with a fixed dialog size for consistent appearance
		setSize(550, 400);

		// Center relative to the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Arranges: a "Repository Info:" label with the repo combo (with HTML tooltip) and
	 * "..." edit button, a separator, the RepoButtonPanel, a status message label, a
	 * Close button panel, and a right-click popup menu with developer/debug options.
	 * Sets _firstTime to true so the auto-add prompt fires on the first load.
	 */
	private void buildControls() {
		setTitle(TITLE);
		getContentPane().setLayout(new GridBagLayout());

		// Prevent the OS close button from dismissing the dialog without going through setVisible(false)
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Create the "Repository Info:" label for the combo box row
		JLabel label = new JLabel("Repository Info:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the repo combo box with an HTML tooltip showing name, URL, and local path
		_repoCombo = new RmaJComboBox<RepoInfo>() {
			@Override
			public String getToolTipText(MouseEvent e) {
				RepoInfo repo = (RepoInfo) getSelectedItem();
				if (repo == null) {
					return super.getToolTipText(e);
				}

				// Build an HTML tooltip summarizing the selected repo's key properties
				StringBuilder builder = new StringBuilder();
				builder.append("<html>");
				builder.append("<b>Name:</b>");
				builder.append(repo.getName());
				builder.append("<br><b>Repo URL:</b>");
				builder.append(repo.getSourceUrl());
				builder.append("<br><b>Local Folder:</b>");
				builder.append(repo.getLocalPath());
				builder.append("<html>");
				return builder.toString();
			}
		};
		label.setLabelFor(_repoCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_repoCombo, gbc);

		// Create the "..." button for opening the ReposEditor
		_editReposButton = new JButton("...");
		_editReposButton.setToolTipText("Add or Delete Repos");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_editReposButton, gbc);

		// Add a horizontal separator between the repo combo row and the action buttons
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the action buttons panel and position it to fill remaining vertical space
		_repoButtonPanel = new RepoButtonPanel(_parent, this);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_repoButtonPanel, gbc);

		// Create the status message label (commit-behind count or "No Local Repository detected")
		_msgLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_msgLabel, gbc);

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

		// Build the developer/debug right-click popup menu
		JPopupMenu popup = new JPopupMenu();

		// "Test" mode: Git commands are executed but not actually sent to the remote
		_doNothingMenu = new JCheckBoxMenuItem("Test");
		_doNothingMenu.setToolTipText("Performs the commands but does not actually send the commands all the way to Git.");
		if (Boolean.getBoolean(AbstractGitAction.DO_NOTHING_PROP)) {
			// If the property is already set (e.g., from JVM args), reflect that in the UI
			_doNothingMenu.setSelected(true);
			setTitle(TITLE + "-Test");
		}
		_doNothingMenu.addActionListener(e -> doNothingAction());
		popup.add(_doNothingMenu);

		// "Debug Git Output" mode: raw Git command output is logged to the console
		_debugMenu = new JCheckBoxMenuItem("Debug Git Output");
		_debugMenu.setToolTipText("Logs the output of the Git commands to the console log");
		if (Boolean.getBoolean(AbstractGitAction.DEBUG_OUTPUT_PROP)) {
			_debugMenu.setSelected(true);
		}
		_debugMenu.addActionListener(e -> debugAction());
		popup.add(_debugMenu);

		// "Refresh Status" toggle: controls whether the 3-minute timer runs refreshChangesAction()
		_refreshMenu = new JCheckBoxMenuItem("Refresh Status ");
		_refreshMenu.setSelected(true);
		_refreshMenu.addActionListener(e -> refreshAction());
		popup.add(_refreshMenu);

		// "Browse Local Folder": opens the selected repo's local directory in Windows Explorer
		_browseLocalMenu = new JMenuItem("Browse Local Folder...");
		_browseLocalMenu.setSelected(true);
		_browseLocalMenu.addActionListener(e -> browseLocalFolderAction());
		_browseLocalMenu.setEnabled(false); // disabled until a repo with a local path is selected
		popup.add(_browseLocalMenu);

		// Attach the popup menu to the content pane so it appears on right-click anywhere
		((JPanel) getContentPane()).setComponentPopupMenu(popup);

		// Mark as first time so loadRepos() knows to auto-open the ReposEditor if no repos exist
		_firstTime = true;
	}

	/**
	 * Opens the selected repository's local folder in Windows Explorer.
	 *
	 * Disables the browse menu item and returns immediately if no repo is selected.
	 * Logs an informational message if the explorer process cannot be launched.
	 */
	private void browseLocalFolderAction() {
		RepoInfo repo = getSelectedRepo();
		if (repo == null) {
			// No repo selected: disable the menu item and return
			_browseLocalMenu.setEnabled(false);
			return;
		}

		// Build the local file reference and launch Windows Explorer for it
		File f = new File(repo.getLocalPath());
		try {
			Runtime.getRuntime().exec("explorer.exe " + f.getAbsolutePath());
		} catch (IOException ioe) {
			// Log the failure; explorer launch errors are non-fatal
			Logger.getLogger(getClass().getName()).info("Failed to launch explorer for " + f.getAbsolutePath() + " Error:" + ioe);
		}
	}


	/**
	 * Updates the _refreshChanges flag to match the current state of the Refresh Status menu item.
	 *
	 * When _refreshChanges is false, the periodic timer still fires but refreshChangesAction()
	 * returns immediately without querying Git.
	 */
	private void refreshAction() {
		_refreshChanges = _refreshMenu.isSelected();
	}


	/**
	 * Toggles Git debug output logging by setting or clearing the DEBUG_OUTPUT_PROP system property.
	 *
	 * When enabled, Git action subclasses log their raw command output to the console.
	 */
	private void debugAction() {
		if (_debugMenu.isSelected()) {
			// Enable detailed Git command output logging
			System.setProperty(AbstractGitAction.DEBUG_OUTPUT_PROP, "true");
		} else {
			// Disable Git command output logging
			System.clearProperty(AbstractGitAction.DEBUG_OUTPUT_PROP);
		}
	}


	/**
	 * Toggles "test mode" by setting or clearing the DO_NOTHING_PROP system property.
	 *
	 * In test mode, Git command invocations are logged but not executed; the dialog title
	 * is updated to include "-Test" as a visual indicator.
	 */
	private void doNothingAction() {
		if (_doNothingMenu.isSelected()) {
			// Activate test mode and append "-Test" to the dialog title
			System.setProperty(AbstractGitAction.DO_NOTHING_PROP, "true");
			setTitle(TITLE + "-Test");
		} else {
			// Deactivate test mode and restore the normal dialog title
			System.clearProperty(AbstractGitAction.DO_NOTHING_PROP);
			setTitle(TITLE);
		}
	}


	/**
	 * Attaches all event listeners to the dialog's interactive controls.
	 *
	 * Registers: an item listener on the repo combo, an action listener on the edit
	 * repos button, a ButtonCmdPanelListener for the Close button, and a WindowAdapter
	 * for the window-opened (auto-select) and window-closing events.
	 */
	private void addListeners() {
		// Respond to repo combo box selection changes
		_repoCombo.addItemListener(e -> repoComboChanged(e));

		// Open the ReposEditor when the "..." button is clicked (not in configure-for-add mode)
		_editReposButton.addActionListener(e -> editReposAction(false));

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

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				// Defer repo auto-selection to the EDT so the combo is fully rendered first
				EventQueue.invokeLater(() -> selectRepo());
			}

			@Override
			public void windowClosing(WindowEvent e) {
				// Handle OS close button via setVisible(false) for consistent cleanup
				setVisible(false);
			}
		});
	}

	/**
	 * Responds to repo combo box selection changes.
	 *
	 * Ignores DESELECTED events. For a valid selection, updates the action buttons,
	 * enables the browse menu item, clears the status message, and triggers a Git
	 * commit-behind check via checkOutOfDate().
	 *
	 * @param e the ItemEvent describing the combo box selection change
	 */
	private void repoComboChanged(ItemEvent e) {
		// Ignore the DESELECTED event fired for the previously selected item
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		RepoInfo repo = getSelectedRepo();

		// Propagate the selection to the action buttons panel
		_repoButtonPanel.setRepoInfo(repo);

		// Enable or disable the "Browse Local Folder" menu item based on selection
		_browseLocalMenu.setEnabled(repo != null);

		if (repo != null) {
			// Clear any stale status message from the previous selection
			_msgLabel.setText("");
		}

		// Start the commit-behind check and periodic refresh timer
		checkOutOfDate();
	}


	/**
	 * Starts the periodic refresh timer and immediately runs a commit-behind check.
	 *
	 * Stops any existing timer before creating a new one set to fire every 3 minutes.
	 * Runs the initial check asynchronously in a SwingWorker to avoid blocking the EDT.
	 * The SwingWorker shows the spinner icon during the check and calls displayChanges()
	 * with the result when done.
	 */
	private void checkOutOfDate() {
		// Build the timer action that triggers a periodic status refresh
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshChangesAction();
			}
		};

		// Stop any existing timer before starting a new one to prevent duplicate refreshes
		if (_timer != null) {
			_timer.stop();
			_timer = null;
		}

		// Create and start a timer that fires every 3 minutes (3 * 60 * 1000 ms)
		_timer = new Timer(3 * 1000 * 60, al);
		_timer.start();

		// Run the initial commit-behind check on a background thread via SwingWorker
		SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
			@Override
			protected List<String> doInBackground() throws Exception {
				// Show the spinner icon to indicate that a Git query is in progress
				_msgLabel.setIcon(_spinnyIcon);
				_msgLabel.setText("Updating...");
				return getChanges();
			}

			@Override
			protected void done() {
				try {
					// Retrieve the result and update the status label on the EDT
					List<String> changes = get();
					displayChanges(changes);
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		worker.execute();
	}

	/**
	 * Conditionally runs a commit-behind check if the refresh feature is enabled.
	 *
	 * Called by the periodic timer; skips the query if _refreshChanges is false.
	 */
	public void refreshChangesAction() {
		if (_refreshChanges) {
			// Query Git and update the status label with the latest commit-behind count
			List<String> changes = getChanges();
			displayChanges(changes);
		}
	}

	/**
	 * Updates the status message label based on the commit-behind count.
	 *
	 * Clears the spinner icon. Handles four cases: null (no local repo detected),
	 * one change behind (singular), multiple changes behind (plural, tooltip listing
	 * each commit), and zero changes (up to date).
	 * <p>
	 * Synchronized to prevent concurrent SwingWorker and timer updates from racing.
	 *
	 * @param changes the list of commit descriptions returned by getChanges(), or null if no local repo
	 */
	protected synchronized void displayChanges(List<String> changes) {
		// Clear the spinner icon now that the check is complete
		_msgLabel.setIcon(null);

		if (changes == null) {
			// Null indicates no local repository was detected at the selected path
			_msgLabel.setText("No Local Repository detected.");
		} else if (changes.size() == 1) {
			// Exactly one commit behind: use singular phrasing; tooltip shows the single commit
			_msgLabel.setText("Local copy is " + changes.size() + " change behind.");
			_msgLabel.setToolTipText(changes.get(0));
		} else if (changes.size() > 1) {
			// Multiple commits behind: use plural phrasing; tooltip lists all commits as HTML
			_msgLabel.setText("Local copy is " + changes.size() + " changes behind.");
			StringBuilder builder = new StringBuilder();
			builder.append("<html>");
			for (int i = 0; i < changes.size(); i++) {
				builder.append(changes.get(i));
				builder.append("<br>");
			}
			builder.append("<html>");
			_msgLabel.setToolTipText(builder.toString());
		} else {
			// Zero commits behind: local copy is current
			_msgLabel.setText("Local copy is up to date");
			_msgLabel.setToolTipText(null);
		}
	}


	/**
	 * Queries Git for commits that exist on the remote but not in the local clone.
	 *
	 * Creates a ShowChangesActions instance for the selected repository and returns
	 * the list of commit descriptions. Synchronized to prevent concurrent queries.
	 *
	 * @return a List of commit description strings, or null if no local repo was detected
	 */
	protected synchronized List<String> getChanges() {
		RepoInfo repo = getSelectedRepo();

		// Create the Git action for querying commits ahead of the local HEAD
		ShowChangesActions action = new ShowChangesActions(_parent, repo, ShowChangesActions.ChangeType.Commits);
		List<String> changes = action.getChanges();
		return changes;
	}


	/**
	 * Loads the saved repository definitions into the repo combo box.
	 *
	 * On the first call with an empty repo list, automatically opens the ReposEditor
	 * in configure-for-add mode to prompt the user to define a repo. Sets the _firstTime
	 * flag to false afterward to prevent repeated prompts. If exactly one repo exists,
	 * auto-selects it. Otherwise shows a prompt to select a repo.
	 */
	private void loadRepos() {
		List<RepoInfo> repos = GitRepoUtils.getReposList();

		if (repos.isEmpty() && _firstTime) {
			// No repos defined on first load: open the editor in add mode automatically
			EventQueue.invokeLater(() -> editReposAction(true));

			// Prevent repeated auto-open on subsequent loads
			_firstTime = false;
			return;
		}

		// Replace the combo box model with the updated repo list (with a blank first entry)
		RmaListModel<RepoInfo> newModel = new RmaListModel<>(true, repos);
		_repoCombo.setModel(newModel);

		if (newModel.size() == 1) {
			// Exactly one repo: auto-select it for convenience
			_repoCombo.setSelectedIndex(0);
		} else {
			// Multiple repos: prompt the user to choose one
			_msgLabel.setText("Select a Repository to work with");
		}
	}

	/**
	 * Scans the repo combo box for an entry whose local path matches the currently
	 * open project's directory, and selects it automatically.
	 *
	 * Called on window-opened via EventQueue.invokeLater() to ensure the combo is
	 * fully rendered. Has no effect if no project is loaded.
	 */
	private void selectRepo() {
		Project prj = Project.getCurrentProject();

		// Do nothing if no real project is currently loaded
		if (prj.isNoProject()) {
			return;
		}

		String dir = prj.getProjectDirectory();

		int cnt = _repoCombo.getItemCount();
		RepoInfo ri;

		for (int i = 0; i < cnt; i++) {
			ri = _repoCombo.getItemAt(i);

			// Use path-equality comparison to handle OS case and separator differences
			if (RMAIO.pathsEqual(dir, ri.getLocalPath())) {
				// Auto-select the matching repo and stop searching
				_repoCombo.setSelectedIndex(i);
				break;
			}
		}
	}

	/**
	 * Opens the ReposEditor dialog, then reloads the repo combo and restores the
	 * previous selection if the repo is still present.
	 *
	 * @param configureForAdd true to open the editor directly in add mode; false for normal editing mode
	 */
	private void editReposAction(boolean configureForAdd) {
		// Create and prepare the repo editor dialog
		ReposEditor editor = new ReposEditor(_parent);
		editor.fillForm();

		RepoInfo repo = getSelectedRepo();
		if (repo != null) {
			// Pre-select the current repo in the editor so the user sees it highlighted
			editor.setSelectedRepo(repo);
		}

		if (configureForAdd) {
			// Activate the automatic add-new-repo workflow in the editor
			editor.configureForAdd();
		}

		editor.setVisible(true);

		// Reload the combo box after the editor closes to reflect any changes
		loadRepos();

		// Restore the previous selection if the repo still exists in the model
		if (((RmaListModel) _repoCombo.getModel()).contains(repo)) {
			_repoCombo.setSelectedItem(repo);
		}
	}


	/**
	 * Returns the RepoInfo currently selected in the repo combo box.
	 *
	 * @return the selected RepoInfo, or null if no repo is selected
	 */
	public RepoInfo getSelectedRepo() {
		return (RepoInfo) _repoCombo.getSelectedItem();
	}

	/**
	 * Overrides setVisible to stop the refresh timer and clean up listeners when the dialog is hidden.
	 *
	 * When the dialog is hidden: stops and nulls the periodic timer to prevent further Git queries,
	 * and calls _repoButtonPanel.closing() to remove the static project listener registered by the panel.
	 *
	 * @param visible true to show the dialog; false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (!visible) {
			// Stop the periodic refresh timer to prevent further background Git queries
			if (_timer != null) {
				_timer.stop();
				_timer = null;
			}

			// Remove the static project listener registered by the repo button panel
			_repoButtonPanel.closing();
		}
	}
}
