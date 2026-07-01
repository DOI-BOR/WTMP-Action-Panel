package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.Cursor;              // Provides cursor types; used to show a wait cursor during Git queries
import java.awt.Dimension;           // Represents width/height; used to fit the submodule tree viewport to its rows
import java.awt.EventQueue;          // Swing event dispatch queue; used to defer the local-changes query to the EDT
import java.awt.GridBagConstraints;  // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;       // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;              // AWT Window; used when constructing Git action objects that need a parent window
import java.awt.event.ActionEvent;   // Represents an action event fired by the Upload and Cancel buttons
import java.awt.event.WindowAdapter; // Adapter for window lifecycle events; used to mark the dialog canceled on OS close
import java.awt.event.WindowEvent;   // Window event fired when the OS close button is clicked
import java.util.ArrayList;          // Resizable-array List used to accumulate locally changed file paths
import java.util.HashMap;            // Hash map used to tally per-submodule commit counts from change lines
import java.util.Iterator;           // Iterator for traversing the submodule commit-count map entries
import java.util.List;               // Ordered collection interface for change, submodule, and command argument lists
import java.util.Map;                // Map interface for the submodule-to-commit-count data structure
import java.util.Map.Entry;          // Map entry type for iterating submodule commit-count pairs
import java.util.Set;                // Set of map entries returned by entrySet()
import java.util.concurrent.Executors; // Executor factory; used to run the tree-change query on a background thread

import javax.swing.BorderFactory;    // Factory for creating titled and other border styles
import javax.swing.JButton;          // Standard Swing push-button for the "..." changed-files detail action
import javax.swing.JLabel;           // Non-interactive label for field captions and the changed-files count
import javax.swing.JOptionPane;      // Provides informational dialog boxes for validation failures
import javax.swing.JPanel;           // Generic lightweight container for the file-changes row
import javax.swing.JScrollPane;      // Scroll container for both the text area and the submodule tree
import javax.swing.JSeparator;       // Horizontal visual divider between the repo info and the changes section
import javax.swing.tree.DefaultMutableTreeNode; // Mutable tree node used as the root of the submodule tree model
import javax.swing.tree.DefaultTreeModel;       // Default tree model constructed with the root node

import rma.swing.ButtonCmdPanel;         // Panel containing Upload and Cancel buttons
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaInsets;              // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJDialog;             // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTextArea;           // RMA-extended multi-line text area for entering detailed comments
import rma.swing.RmaJTextField;          // RMA-extended text field for read-only repo info and one-line description

import usbr.wat.plugins.actionpanel.gitIntegration.actions.AbstractGitAction;       // Base Git action providing command constants (e.g., LOCAL_FOLDER, SUB_MODULE)
import usbr.wat.plugins.actionpanel.gitIntegration.actions.FetchAction;             // Git fetch action providing the FETCH_CMD constant
import usbr.wat.plugins.actionpanel.gitIntegration.actions.OkToPushAction;          // Git action that checks whether pushing is safe (no unmerged remote commits)
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangedLocalFilesAction; // Git action for listing locally modified files
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;      // Git action for listing commits ahead of the local HEAD
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;                  // Data model holding the local path and URL of a Git repository
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CheckboxTree;                 // Checkbox tree for selecting which submodules to include in the upload

/**
 * Dialog for entering a commit description and optional extended comments before
 * uploading (pushing) a WAT study to the selected Git repository.
 *
 * Presents read-only fields showing the study folder and remote URL of the selected
 * repository, a count and detail button for locally changed files, a CheckboxTree
 * for selecting which submodules to upload (hidden when no submodules exist), a
 * titled "Change Description" panel containing a mandatory short description field
 * (max 120 characters) and an optional multi-line comments text area, and an
 * Upload + Cancel button pair.
 *
 * On show, the dialog queries Git for locally changed files (on the EDT) and for
 * per-submodule commit-behind counts (on a background thread). The commit-count
 * results are reflected in the CheckboxTree to prevent uploading a submodule that
 * is behind the remote.
 *
 * Validation (dlgStateOk()) requires a non-empty description, at least one checked
 * submodule when the tree is visible, and confirmation from OkToPushAction that no
 * unmerged remote commits would be overwritten by the push.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class EnterCommentsDlg extends RmaJDialog {
	// Multi-line text area for entering optional extended commit comments
	private RmaJTextArea _textArea;

	// Panel containing the Upload and Cancel buttons
	private ButtonCmdPanel _cmdPanel;

	// Flag indicating whether the dialog was dismissed without confirming; starts false
	protected boolean _canceled;

	// Read-only field showing the local folder of the selected repository
	private RmaJTextField _studyFld;

	// Read-only field showing the remote URL of the selected repository
	private RmaJTextField _remoteUrlFld;

	// Single-line field for entering the mandatory short commit description (max 120 chars)
	private RmaJTextField _descriptionFld;

	// Button that opens a ChangesDlg listing the locally modified files in detail
	private JButton _changesBtn;

	// Reference to the parent StudyStorageDialog for retrieving the selected repository
	private StudyStorageDialog _studyStorageDialog;

	// Label showing the count of locally modified files
	private JLabel _fileChangesLabel;

	// Accumulated list of locally changed file paths; populated by getChangedFiles()
	private List<String> _changedFiles = new ArrayList<>();

	// Checkbox tree for selecting which submodules to include in the upload
	private CheckboxTree _submoduleTree;

	// Scroll pane wrapping the submodule tree; hidden when no submodules are present
	private JScrollPane _treeScrollPane;

	/**
	 * Constructs an EnterCommentsDlg attached to the given StudyStorageDialog.
	 *
	 * Builds all UI controls, attaches listeners, populates the static form fields,
	 * packs the dialog, overrides the size to 400×550, and centers it relative to
	 * the parent StudyStorageDialog.
	 *
	 * @param parent the StudyStorageDialog that owns this dialog
	 * @param title  the text to display in the dialog title bar
	 * @param modal  true if the dialog should block input to other windows while open
	 */
	public EnterCommentsDlg(StudyStorageDialog parent, String title, boolean modal) {
		// Initialize the base RmaJDialog with the given title and modality
		super(parent, title, modal);

		// Store the parent dialog reference for repo retrieval
		_studyStorageDialog = parent;

		// Build and arrange all UI controls
		buildControls();

		// Attach listeners for window close, button clicks, and the changes button
		addListeners();

		// Populate the static fields from the selected repository
		fillForm();

		// Size the dialog to its preferred layout dimensions
		pack();

		// Override with a fixed dialog size for consistent appearance
		setSize(400, 550);

		// Center relative to the parent StudyStorageDialog
		setLocationRelativeTo(parent);
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Arranges: a study folder field, a remote URL field, a separator, a file-changes
	 * row (label + detail button), a submodule checkbox tree (conditionally visible),
	 * a titled "Change Description" panel with a short-description field and a
	 * comments text area, and an Upload/Cancel button panel.
	 */
	protected void buildControls() {
		// Prevent the OS close button from bypassing the cancel logic
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());

		// Create the "Study Folder:" label
		JLabel label = new JLabel("Study Folder:");
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

		// Create the read-only study folder path field
		_studyFld = new RmaJTextField();
		_studyFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_studyFld, gbc);

		// Create the "Remote URL:" label
		label = new JLabel("Remote URL:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the read-only remote URL field with a descriptive tooltip
		_remoteUrlFld = new RmaJTextField();
		_remoteUrlFld.setToolTipText("The Repo's URL being uploaded to");
		_remoteUrlFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_remoteUrlFld, gbc);

		// Add a horizontal separator between the repo info and the changes section
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the file-changes row panel (count label + detail button side by side)
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS0000;
		getContentPane().add(panel, gbc);

		// Create the placeholder changed-files count label
		_fileChangesLabel = new JLabel("There are -- local files that have been changed");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.001;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		panel.add(_fileChangesLabel, gbc);

		// Create the "..." button for opening the ChangesDlg with file detail
		_changesBtn = new JButton("...");
		_changesBtn.setToolTipText("Show local changed files");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		panel.add(_changesBtn, gbc);

		// Create the submodule checkbox tree with a dynamic viewport height
		DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
		_submoduleTree = new CheckboxTree(model, _studyStorageDialog) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Fit the viewport exactly to the number of visible rows
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowCount() * getRowHeight() + 10;
				return d;
			}
		};

		// Prevent the user from selecting submodules that are behind the remote
		_submoduleTree.setDisallowSelectionForNodesBehind(true);

		// Add extra row height for readability
		_submoduleTree.setRowHeight(_submoduleTree.getRowHeight() + 5);

		// Position the tree to take up 75% of available vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.75;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_treeScrollPane = new JScrollPane(_submoduleTree);
		getContentPane().add(_treeScrollPane, gbc);

		// Create the titled "Change Description" sub-panel for the description and comments fields
		JPanel descriptionPanel = new JPanel(new GridBagLayout());
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Change Description"));
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(descriptionPanel, gbc);

		// Create the "Description:" label (mandatory one-liner, max 120 chars)
		label = new JLabel("Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		descriptionPanel.add(label, gbc);

		// Create the short description field with a max length of 120 characters
		_descriptionFld = new RmaJTextField();
		_descriptionFld.setMaxLength(120);
		_descriptionFld.setToolTipText("Brief description of changes for the upload");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		descriptionPanel.add(_descriptionFld, gbc);

		// Create the "Comments:" label for the optional extended text area
		label = new JLabel("Comments:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		descriptionPanel.add(label, gbc);

		// Create the multi-line comments text area for detailed change notes
		_textArea = new RmaJTextArea();
		_textArea.setToolTipText("Detailed description of the changes being uploaded");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		descriptionPanel.add(new JScrollPane(_textArea), gbc);

		// Create the Upload/Cancel command panel; rename OK button to "Upload"
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		JButton okButton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		okButton.setText("Upload");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * Attaches event listeners to all interactive controls.
	 *
	 * Registers: a WindowAdapter to mark the dialog canceled when the OS close
	 * button is clicked, a ButtonCmdPanelListener for Upload and Cancel, and a
	 * lambda listener on the changes detail button.
	 */
	protected void addListeners() {
		// Mark the dialog canceled and hide it when the OS close button is clicked
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				_canceled = true;
				setVisible(false);
			}
		});

		// Handle Upload and Cancel button clicks from the command panel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate all required fields before allowing the upload to proceed
						if (dlgStateOk()) {
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Mark as canceled and hide the dialog without uploading
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});

		// Open the locally changed files detail dialog when "..." is clicked
		_changesBtn.addActionListener(e -> showChangesAction());
	}

	/**
	 * Opens a ChangesDlg listing the locally modified files in detail.
	 */
	private void showChangesAction() {
		ChangesDlg dlg = new ChangesDlg(this, _changedFiles);
		dlg.setVisible(true);
	}


	/**
	 * Populates the static repo info fields from the currently selected repository
	 * and initializes the submodule tree.
	 *
	 * If no repository is selected, disables the Upload button. Fills the submodule
	 * tree and shows or hides the tree pane based on whether submodules are present.
	 */
	private void fillForm() {
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();

		if (repo != null) {
			// Pre-fill the read-only repo info fields from the selected repository
			_remoteUrlFld.setText(repo.getSourceUrl());
			_studyFld.setText(repo.getLocalPath());
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(true);
		} else {
			// No repo selected: disable the Upload button until one is chosen
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
		}

		// Populate the submodule tree from the repository's submodule configuration
		_submoduleTree.fillSubModules();

		// Show the tree pane only if the repository has submodules
		_treeScrollPane.setVisible(_submoduleTree.hasSubModules());
	}

	/**
	 * Overrides setVisible to trigger the async Git queries each time the dialog is shown.
	 *
	 * @param visible true to show the dialog; false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			// Re-run Git queries each time the dialog becomes visible
			getChanges();
		}
		super.setVisible(visible);
	}

	/**
	 * Schedules both Git change queries when the dialog becomes visible.
	 *
	 * The local-files query runs on the EDT via EventQueue.invokeLater(); the
	 * per-submodule commit-count query runs on a single-thread background executor
	 * to avoid blocking the EDT during a potentially slow Git network operation.
	 */
	private void getChanges() {
		// Schedule the local changed-files query on the EDT
		EventQueue.invokeLater(() -> getChangedFiles());

		// Run the tree commit-count query on a background thread to avoid EDT blocking
		Executors.newSingleThreadExecutor().execute(() -> getChangesForTree());
	}

	/**
	 * Checks whether it is safe to push by running OkToPushAction against all
	 * currently selected submodules.
	 *
	 * Returns false immediately if no repository is selected. Builds the Git command
	 * arguments list from the selected submodule names and delegates to OkToPushAction.
	 *
	 * @return true if the push is safe to proceed; false if there are unmerged remote commits
	 */
	private boolean okToPush() {
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();
		if (repo == null) {
			// Cannot push without a repository selected
			return false;
		}

		List<String> subModules = getSelectedSubmodules();
		boolean rv = true;

		// Build the command argument list: fetch command, local folder, and each selected submodule
		List<String> cmd = new ArrayList<>();
		cmd.add(FetchAction.FETCH_CMD);
		cmd.add(AbstractGitAction.LOCAL_FOLDER);
		cmd.add(repo.getLocalPath());

		for (int i = 0; i < subModules.size(); i++) {
			cmd.add(AbstractGitAction.SUB_MODULE);
			cmd.add(subModules.get(i));
		}

		// Run the push-safety check action with the assembled command
		OkToPushAction okToPush = new OkToPushAction(cmd, this);
		if (!okToPush.isOkToPush()) {
			rv = false;
		}

		return rv;
	}

	/**
	 * Queries Git for locally modified files and updates the file-changes label and button.
	 *
	 * Shows a wait cursor during the query. If changes are found, accumulates them in
	 * _changedFiles, updates the count label (singular or plural), and enables the
	 * detail button. If none are found, shows a "no changed files" message and disables
	 * the detail button.
	 */
	private void getChangedFiles() {
		// Show the wait cursor to indicate a potentially slow Git operation
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Run the local-changes action against the selected repo
			ShowChangedLocalFilesAction fetchAction = new ShowChangedLocalFilesAction(_studyStorageDialog, _studyStorageDialog.getSelectedRepo());
			List<String> changes = fetchAction.getChanges();

			if (changes != null && !changes.isEmpty()) {
				// Accumulate the changed file paths for the detail dialog
				_changedFiles.addAll(changes);

				// Update the label with a singular or plural count message
				if (changes.size() == 1) {
					_fileChangesLabel.setText("There is " + _changedFiles.size() + " local file that has been changed");

				} else {
					_fileChangesLabel.setText("There are " + _changedFiles.size() + " local files that have been changed");
				}

				// Enable the detail button so the user can inspect the files
				_changesBtn.setEnabled(true);

			} else {
				// No local changes: show the clean-state message and disable the detail button
				_fileChangesLabel.setText("There are no local files that have been changed");
				_changesBtn.setEnabled(false);
			}

		} finally {
			// Always restore the default cursor when the query completes
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Queries Git for commits ahead of the selected repository and updates the
	 * submodule tree with per-submodule behind-commit counts.
	 *
	 * Runs on a background thread (scheduled by getChanges()); calls
	 * checkForSubModuleCommitsBehind() to parse the log output, then posts
	 * the tree update back to the EDT via EventQueue.invokeLater().
	 */
	private void getChangesForTree() {
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();

		// Run the Git commits-behind query for the selected repo
		ShowChangesActions fetchAction = new ShowChangesActions((Window) getParent(), repo, ShowChangesActions.ChangeType.Commits);
		List<String> changes = fetchAction.getChanges();

		if (changes != null) {
			// Parse the commit lines and schedule the tree update on the EDT
			checkForSubModuleCommitsBehind(changes);
		}
	}

	/**
	 * Parses the commit log lines to count how many commits each submodule is behind,
	 * then posts the count update to the submodule tree on the EDT.
	 *
	 * Lines without a colon separator are skipped. The per-submodule counts are
	 * accumulated in a map and forwarded to updateTreeWithCommitsBehind() via
	 * EventQueue.invokeLater() to ensure thread safety.
	 *
	 * @param changes the list of commit log strings returned by ShowChangesActions
	 */
	private void checkForSubModuleCommitsBehind(List<String> changes) {
		// Map from submodule name to the number of commits that submodule is behind
		Map<String, Integer> subModuleCommitsBehindMap = new HashMap<>();

		String line, subModule;
		Integer cnt;
		int idx;

		for (int i = 0; i < changes.size(); i++) {
			line = changes.get(i);

			// Extract the submodule name from lines formatted as "submoduleName: description"
			idx = line.indexOf(':');
			if (idx > -1) {
				subModule = line.substring(0, idx).trim();
				cnt = subModuleCommitsBehindMap.get(subModule);

				if (cnt == null) {
					// First commit seen for this submodule; initialize count to 1
					cnt = 1;
					subModuleCommitsBehindMap.put(subModule, cnt);
				} else {
					// Increment the commit count for this submodule
					cnt++;
					subModuleCommitsBehindMap.put(subModule, cnt);
				}
			}
		}

		// Post the tree update to the EDT to ensure it runs on the correct thread
		EventQueue.invokeLater(() -> updateTreeWithCommitsBehind(subModuleCommitsBehindMap));
	}

	/**
	 * Updates each submodule node in the CheckboxTree with its behind-commit count.
	 *
	 * Must be called on the EDT; scheduled via EventQueue.invokeLater() from
	 * checkForSubModuleCommitsBehind().
	 *
	 * @param subModuleCommitsBehindMap map from submodule name to number of commits behind
	 */
	private void updateTreeWithCommitsBehind(Map<String, Integer> subModuleCommitsBehindMap) {
		Set<Entry<String, Integer>> entries = subModuleCommitsBehindMap.entrySet();
		Iterator<Entry<String, Integer>> iter = entries.iterator();
		Entry<String, Integer> entry;
		String subModule;
		int cnt;

		// Iterate all map entries and update the corresponding tree node
		while (iter.hasNext()) {
			entry = iter.next();
			subModule = entry.getKey();
			cnt = entry.getValue();
			_submoduleTree.setCommitsBehind(subModule, cnt);
		}
	}

	/**
	 * Validates the dialog state before allowing the upload to proceed.
	 *
	 * Checks: the description field is non-empty, at least one submodule is checked
	 * when the tree is visible, and OkToPushAction confirms no unmerged remote commits
	 * would be overwritten. Shows an informational dialog for each failed check.
	 *
	 * @return true if all validation checks pass; false if any check fails
	 */
	protected boolean dlgStateOk() {
		String text = _descriptionFld.getText();

		// Require a non-empty description before allowing the upload
		if (text == null || text.isEmpty()) {
			String msg = "A Description must be entered to upload files";
			String title = "Missing Description";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Require at least one submodule selection when the tree is visible
		if (_treeScrollPane.isVisible()) {
			List<String> submodules = getSelectedSubmodules();
			if (submodules.size() == 0) {
				String msg = "Please select which area of the study to upload";
				String title = "No area selected";
				JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}

		// Confirm that the push is safe (no unmerged remote commits ahead of local HEAD)
		if (!okToPush()) {
			return false;
		}

		// All checks passed
		return true;
	}

	/**
	 * Sets the text in the extended comments text area.
	 *
	 * @param text the comment text to display in the text area
	 */
	public void setComments(String text) {
		_textArea.setText(text);
	}

	/**
	 * Returns the current content of the extended comments text area.
	 *
	 * Returns an empty string if the text area is blank.
	 *
	 * @return the comments text entered by the user, or an empty string if none
	 */
	public String getComments() {
		StringBuilder builder = new StringBuilder();
		String comments = _textArea.getText();

		if (comments != null && !comments.isEmpty()) {
			builder.append(comments);
		}

		return builder.toString();
	}

	/**
	 * Returns the text entered in the short description field.
	 *
	 * @return the mandatory commit description text
	 */
	public String getDescription() {
		return _descriptionFld.getText();
	}

	/**
	 * Returns whether the dialog was closed by the user selecting Cancel or the OS close button.
	 *
	 * @return true if the dialog was canceled; false if the upload was confirmed
	 */
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Returns the list of submodule names checked by the user in the submodule tree.
	 *
	 * @return a List of checked submodule name strings; empty if none are selected or the tree is hidden
	 */
	public List<String> getSelectedSubmodules() {
		return _submoduleTree.getCheckedSubmodules();
	}
}
