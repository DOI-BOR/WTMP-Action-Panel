package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.Cursor;              // Provides cursor types; used to show a wait cursor during Git queries
import java.awt.Dimension;           // Represents width/height dimensions; used to fit the submodule tree viewport
import java.awt.EventQueue;          // Swing event dispatch queue; used to defer Git queries to the EDT
import java.awt.Font;                // Font utilities; used to bold the overwrite-warning label
import java.awt.GridBagConstraints;  // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;       // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;              // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent;   // Represents an action event fired by buttons and command panel
import java.util.ArrayList;          // Resizable-array List used to accumulate locally changed file paths
import java.util.HashMap;            // Hash map used to tally per-submodule commit counts from change lines
import java.util.Iterator;           // Iterator for traversing the submodule commit-count map entries
import java.util.List;               // Ordered collection interface for change and submodule lists
import java.util.Map;                // Map interface for the submodule-to-commit-count data structure
import java.util.Map.Entry;          // Map entry type for iterating submodule commit-count pairs
import java.util.Set;                // Set of map entries returned by entrySet()

import javax.swing.JButton;          // Standard Swing push-button for showing locally changed files
import javax.swing.JLabel;           // Non-interactive label for field captions and the overwrite warning
import javax.swing.JOptionPane;      // Provides informational dialog boxes for validation failures
import javax.swing.JPanel;           // Generic lightweight container for the bottom button/checkbox row
import javax.swing.JScrollPane;      // Scroll container for both the changes list and the submodule tree
import javax.swing.JSeparator;       // Horizontal visual divider between the repo info and submodule sections
import javax.swing.tree.DefaultMutableTreeNode; // Mutable tree node used as the root of the submodule tree model
import javax.swing.tree.DefaultTreeModel;       // Default tree model constructed with the root node

import rma.swing.ButtonCmdPanel;         // Panel containing Download/Restore and Cancel buttons
import rma.swing.ButtonCmdPanelListener; // Listener interface for command button panel action events
import rma.swing.RmaInsets;              // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJCheckBox;           // RMA-extended checkbox for the "Soft OverWrite" option
import rma.swing.RmaJDialog;             // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJList;              // RMA-extended list component for displaying commit-change strings
import rma.swing.RmaJTextField;          // RMA-extended read-only text field for study folder and remote URL
import rma.swing.list.RmaListModel;      // RMA list model backed by a collection for use in RmaJList

import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangedLocalFilesAction; // Git action for listing locally modified files
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;          // Git action for listing commits ahead of local HEAD
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;                      // Data model holding the local path and URL of a Git repository
import usbr.wat.plugins.actionpanel.gitIntegration.ui.CheckboxTree;                     // Checkbox tree for selecting which submodules to download or restore

/**
 * Confirmation dialog shown before a Git download or restore operation within
 * the WTMP Action Panel's Git integration.
 *
 * Presents read-only fields showing the local study folder and remote URL of
 * the selected repository, a CheckboxTree for selecting which submodules (if any)
 * to include in the operation, a scrollable list of pending remote commits since
 * the last download, a count and detail button for locally modified files, a bold
 * overwrite-warning label, an optional "Soft OverWrite" checkbox (download only),
 * and a Download/Restore + Cancel button pair.
 *
 * The dialog queries Git asynchronously via EventQueue.invokeLater() for both
 * outstanding remote commits and locally changed files. Per-submodule commit counts
 * are parsed from the commit log lines and reflected in the CheckboxTree via
 * setCommitsBehind().
 *
 * Constructed with isRestore=false for a download and isRestore=true for a restore;
 * the title, OK button label, and warning text adapt accordingly.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */
@SuppressWarnings("serial")
public class DownloadConfirmDialog extends RmaJDialog {
	// Scrollable list showing commits that exist on the remote but not in the local clone
	private RmaJList<String> _changesList;

	// Panel containing the primary action (Download/Restore) and Cancel buttons
	private ButtonCmdPanel _cmdPanel;

	// Flag indicating whether the dialog was dismissed without confirming; starts true (safe default)
	protected boolean _canceled = true;

	// Label showing the count of locally modified files ("There are N local files that have been changed")
	private JLabel _fileChangesLabel;

	// Button that opens a ChangesDlg listing the locally modified files in detail
	private JButton _fileChangesBtn;

	// Accumulated list of locally changed file paths; populated by getChangedFiles()
	private List<String> _changedFiles = new ArrayList<>();

	// Read-only field showing the local directory of the selected repository
	private RmaJTextField _studyFolderFld;

	// Read-only field showing the remote URL of the selected repository
	private RmaJTextField _remoteUrlFld;

	// The repository this dialog is operating on
	private RepoInfo _repo;

	// Checkbox tree for selecting which submodules to include in the operation
	private CheckboxTree _submoduleTree;

	// Scroll pane wrapping the submodule tree; hidden when no submodules are present
	private JScrollPane _treeScrollPane;

	// True when the dialog is in restore mode; false for download mode
	private boolean _isRestore;

	// Checkbox allowing the user to request a soft overwrite (skip unchanged uploaded files); download only
	private RmaJCheckBox _softoverwriteCheck;

	/**
	 * Constructs a DownloadConfirmDialog in download mode (isRestore=false).
	 * <p>
	 * Delegates to the full constructor with isRestore=false.
	 *
	 * @param parent the Window that will own this modal dialog
	 * @param repo   the RepoInfo describing the repository to download from
	 */
	public DownloadConfirmDialog(Window parent, RepoInfo repo) {
		// Delegate to the primary constructor with restore mode disabled
		this(parent, repo, false);
	}

	/**
	 * Constructs a DownloadConfirmDialog in either download or restore mode.
	 *
	 * Builds all UI controls (adapting labels and button text for the mode),
	 * attaches listeners, sets a fixed 600×600 size, packs to preferred dimensions,
	 * and centers relative to the parent window.
	 *
	 * @param parent    the Window that will own this modal dialog
	 * @param repo      the RepoInfo describing the repository to operate on
	 * @param isRestore true to show restore-mode labels and button; false for download mode
	 */
	public DownloadConfirmDialog(Window parent, RepoInfo repo, boolean isRestore) {
		// Initialize the base RmaJDialog as modal
		super(parent, true);

		// Store the repository reference and mode flag for use during build and fill
		_repo = repo;
		_isRestore = isRestore;

		// Build and arrange all UI controls
		buildControls();

		// Attach button and action listeners
		addListeners();

		// Apply a fixed size then pack to the preferred layout for consistent appearance
		setSize(600, 600);
		pack();

		// Center the dialog relative to the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Adapts the title, overwrite warning, and primary button label based on _isRestore.
	 * Lays out: a study folder field, a remote URL field, a separator, the submodule
	 * checkbox tree (hidden when no submodules exist), a commits-behind list, a local-changes
	 * count label and detail button, a bold warning label, a bottom panel with the optional
	 * Soft OverWrite checkbox, and the command button panel.
	 */
	private void buildControls() {
		// Apply GridBagLayout to the dialog content pane
		getContentPane().setLayout(new GridBagLayout());

		// Set the title based on whether this is a restore or download operation
		if (_isRestore) {
			setTitle("Restore");
		} else {
			setTitle("Download Changes");
		}

		// Create a sub-panel for the read-only repo info fields
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS0000;
		getContentPane().add(panel, gbc);

		// Create the "Study Folder:" label
		JLabel label = new JLabel("Study Folder:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(label, gbc);

		// Create the read-only study folder path field
		_studyFolderFld = new RmaJTextField();
		_studyFolderFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_studyFolderFld, gbc);

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
		panel.add(label, gbc);

		// Create the read-only remote URL field with a tooltip
		_remoteUrlFld = new RmaJTextField();
		_remoteUrlFld.setToolTipText("The Repo's URL being downloaded from");
		_remoteUrlFld.setEditable(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		panel.add(_remoteUrlFld, gbc);

		// Add a horizontal separator between the repo info and the submodule tree
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the submodule checkbox tree with a dynamic viewport height
		DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
		_submoduleTree = new CheckboxTree(model, _repo) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// Fit the viewport exactly to the number of rows to avoid wasted space
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowCount() * getRowHeight() + 10;
				return d;
			}
		};

		// Add extra row height for readability
		_submoduleTree.setRowHeight(_submoduleTree.getRowHeight() + 5);

		// Position the tree to take up 75% of available vertical space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = .75;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_treeScrollPane = new JScrollPane(_submoduleTree);
		getContentPane().add(_treeScrollPane, gbc);

		// Create the "Changes Since Last Download" section label
		label = new JLabel("Changes Since Last Download");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);

		// Create the scrollable list showing outstanding remote commits
		_changesList = new RmaJList<>();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_changesList), gbc);

		// Create the placeholder label for the locally changed file count
		_fileChangesLabel = new JLabel("There are -- local files that have been changed");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(_fileChangesLabel, gbc);

		// Create the "..." button for opening the ChangesDlg with locally modified file details
		_fileChangesBtn = new JButton("...");
		_fileChangesBtn.setToolTipText("Show local changed files");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_fileChangesBtn, gbc);

		// Build the mode-appropriate overwrite warning text
		String s;
		if (_isRestore) {
			s = "Restoring will update the files in the Study to the last successful download, possibly overwriting changes";
		} else {
			s = "Downloading will update the files in the Study, possibly overwriting changes";
		}

		// Create the bold warning label to alert the user to potential data loss
		label = new JLabel(s);
		Font f = label.getFont();
		f = f.deriveFont(Font.BOLD);
		label.setFont(f);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		getContentPane().add(label, gbc);

		// Create the bottom panel containing the Soft OverWrite checkbox and command buttons
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(bottomPanel, gbc);

		// Create the Soft OverWrite checkbox; only shown in download mode (not restore)
		_softoverwriteCheck = new RmaJCheckBox("Soft OverWrite");
		_softoverwriteCheck.setToolTipText("Don't overwrite unmodified uploaded files when you download");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		if (!_isRestore) {
			// Only show the soft-overwrite option for downloads, not restores
			bottomPanel.add(_softoverwriteCheck, gbc);
		}

		// Create the command panel with OK (Download/Restore) and Cancel buttons
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);

		// Rename the OK button to match the current mode
		if (_isRestore) {
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setText("Restore");
		} else {
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setText("Download");
		}
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		bottomPanel.add(_cmdPanel, gbc);
	}

	/**
	 * Attaches event listeners to all interactive controls.
	 *
	 * Registers: a lambda listener on the file-changes button to open ChangesDlg,
	 * and a ButtonCmdPanelListener for the Download/Restore and Cancel buttons.
	 */
	private void addListeners() {
		// Open the locally changed files detail dialog when "..." is clicked
		_fileChangesBtn.addActionListener(e -> showLocalChangedFilesAction());

		// Handle Download/Restore and Cancel button clicks
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						// Validate that at least one submodule is selected (if the tree is visible)
						if (isValidForm()) {
							_canceled = false;
							setVisible(false);
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Mark as canceled and hide the dialog without performing any operation
						_canceled = true;
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Validates form state before confirming the download or restore.
	 * <p>
	 * If the submodule tree is visible, ensures at least one submodule is checked.
	 * Shows an informational dialog if the validation fails.
	 *
	 * @return true if the form is valid and the operation may proceed; false otherwise
	 */
	protected boolean isValidForm() {
		if (_treeScrollPane.isVisible()) {
			List<String> submodules = getSelectedSubmodules();
			if (submodules.size() == 0) {
				// Require at least one submodule selection when the tree is shown
				String msg = "Please select which area of the study to " + (_isRestore ? "restore" : "download");
				String title = "No area selected";
				JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}
		return true;
	}


	/**
	 * Initializes the dialog's dynamic content when it becomes visible.
	 * <p>
	 * Sets the changes list to a "Checking for Changes..." placeholder, fills the
	 * submodule tree, shows or hides the tree pane based on whether submodules exist,
	 * and triggers the async Git queries via getChanges().
	 */
	private void fillForm() {
		// Show a temporary placeholder while the Git queries are running
		RmaListModel<String> newModel = new RmaListModel(false);
		newModel.addElement("Checking for Changes...");
		_changesList.setModel(newModel);

		// Populate the submodule tree from the repository's submodule configuration
		_submoduleTree.fillSubModules();

		// Show the tree pane only if the repository has submodules
		_treeScrollPane.setVisible(_submoduleTree.hasSubModules());

		// Trigger the async Git queries for remote commits and local changes
		getChanges();
	}

	/**
	 * Opens a ChangesDlg listing the locally modified files in detail.
	 */
	private void showLocalChangedFilesAction() {
		ChangesDlg dlg = new ChangesDlg(this, _changedFiles);
		dlg.setVisible(true);
	}

	/**
	 * Returns whether the dialog was dismissed by the user selecting Cancel.
	 *
	 * @return true if the dialog was canceled; false if the operation was confirmed
	 */
	public boolean isCanceled() {
		return _canceled;
	}

	/**
	 * Overrides setVisible to trigger fillForm() each time the dialog is shown.
	 * <p>
	 * This ensures that the Git queries run and the submodule tree is refreshed
	 * every time the dialog is presented, not just on first construction.
	 *
	 * @param visible true to show the dialog; false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			// Refresh form content and trigger Git queries each time the dialog is shown
			fillForm();
			setSize(600, 600);
		}
		super.setVisible(visible);
	}

	/**
	 * Populates the read-only repo info fields and schedules both Git queries via the EDT.
	 * <p>
	 * If no repository is set, disables the primary action button and returns immediately.
	 * Otherwise fills the study folder and remote URL fields, then schedules the
	 * outstanding-commits query and the local-changes query via EventQueue.invokeLater()
	 * so they run asynchronously without blocking the current EDT cycle.
	 */
	private void getChanges() {
		RepoInfo repo = _repo;

		if (repo == null) {
			// No repo: disable the primary action button since there is nothing to operate on
			_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
			return;
		}

		// Pre-fill the read-only repo info fields from the repository data
		_studyFolderFld.setText(repo.getLocalPath());
		_remoteUrlFld.setText(repo.getSourceUrl());

		// Schedule the two Git queries to run on the EDT after the current event is processed
		EventQueue.invokeLater(() -> getOutstandingCommits());
		EventQueue.invokeLater(() -> getChangedFiles());
	}

	/**
	 * Queries Git for commits that exist on the remote but have not yet been pulled locally,
	 * populates the commits list, and updates the submodule tree with per-submodule counts.
	 *
	 * Displays a wait cursor during the query and restores the default cursor when done.
	 * If the query returns null (e.g., no local repository detected), shows a "No Changes
	 * Detected" placeholder in the list.
	 */
	private void getOutstandingCommits() {
		// Show the wait cursor to indicate a potentially slow Git network operation
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Run the Git fetch and commit-log action for the selected repo
			ShowChangesActions fetchAction = new ShowChangesActions((Window) getParent(), _repo, ShowChangesActions.ChangeType.Commits);
			List<String> changes = fetchAction.getChanges();

			if (changes != null) {
				// Parse the commit lines to build a per-submodule count for the tree display
				checkForSubModuleCommitsBehind(changes);

				// Populate the commits list with the retrieved change descriptions
				RmaListModel<String> newModel = new RmaListModel(false, changes);
				_changesList.setModel(newModel);
			} else {
				// No commits returned: show a placeholder indicating the repo is up to date
				RmaListModel<String> newModel = new RmaListModel(false);
				newModel.addElement("No Changes Detected.");
				_changesList.setModel(newModel);
			}
		} finally {
			// Always restore the default cursor when the query completes
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Parses the commit log lines to count how many commits each submodule is behind,
	 * then updates the submodule tree with those counts.
	 *
	 * Each line is expected to be formatted as "submoduleName: commit description".
	 * Lines without a colon are skipped. The counts are accumulated in a map and
	 * forwarded to updateTreeWithCommitsBehind().
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

			// Look for the colon separator between submodule name and commit description
			idx = line.indexOf(':');
			if (idx > -1) {
				// Extract the submodule name preceding the colon
				subModule = line.substring(0, idx).trim();

				cnt = subModuleCommitsBehindMap.get(subModule);
				if (cnt == null) {
					// First commit seen for this submodule; initialize count to 1
					cnt = 1;
					subModuleCommitsBehindMap.put(subModule, cnt);
				} else {
					// Increment the existing commit count for this submodule
					cnt++;
					subModuleCommitsBehindMap.put(subModule, cnt);
				}
			}
		}

		// Push the per-submodule counts into the checkbox tree
		updateTreeWithCommitsBehind(subModuleCommitsBehindMap);
	}


	/**
	 * Updates each submodule node in the CheckboxTree with its behind-commit count.
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
	 * Queries Git for locally modified files and updates the file-changes label and button.
	 *
	 * Displays a wait cursor during the query. If changes are found, updates the count
	 * label (handling singular/plural) and enables the detail button. If none are found,
	 * shows a "no changed files" message and disables the button.
	 */
	private void getChangedFiles() {
		// Show the wait cursor to indicate a potentially slow Git operation
		getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// Run the local-changes action to get the list of modified file paths
			ShowChangedLocalFilesAction fetchAction = new ShowChangedLocalFilesAction((Window) getParent(), _repo);
			List<String> changes = fetchAction.getChanges();

			if (changes != null && !changes.isEmpty()) {
				// Accumulate the changed files into the backing list for the detail dialog
				_changedFiles.addAll(changes);

				// Update the label with a singular or plural count message
				if (_changedFiles.size() == 1) {
					_fileChangesLabel.setText("There is " + _changedFiles.size() + " local file that has been changed");
				} else {
					_fileChangesLabel.setText("There are " + _changedFiles.size() + " local files that have been changed");
				}

				// Enable the detail button so the user can inspect the changed files
				_fileChangesBtn.setEnabled(true);
			} else {
				// No local changes: update the label and disable the detail button
				_fileChangesLabel.setText("There are no local files that have been changed");
				_fileChangesBtn.setEnabled(false);
			}
		} finally {
			// Always restore the default cursor when the query completes
			getContentPane().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Returns the list of submodule names checked by the user in the submodule tree.
	 *
	 * @return a List of checked submodule name strings; empty if none are selected or the tree is hidden
	 */
	public List<String> getSelectedSubmodules() {
		return _submoduleTree.getCheckedSubmodules();
	}

	/**
	 * Returns whether the Soft OverWrite checkbox is selected.
	 *
	 * When true, the download should skip files that have not been modified since
	 * the last upload rather than overwriting them unconditionally.
	 *
	 * @return true if soft overwrite is requested; false for a full overwrite
	 */
	public boolean shouldSoftOverWrite() {
		return _softoverwriteCheck.isSelected();
	}
}
