package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.EventQueue;           // Swing event dispatch queue; used to defer the repo tree population to the EDT
import java.awt.GridBagConstraints;   // Defines positioning and sizing constraints for GridBagLayout components
import java.awt.GridBagLayout;        // Flexible grid-based layout manager for arranging UI components
import java.awt.Window;               // AWT Window used as the owner for this modal dialog
import java.awt.event.ActionEvent;    // Represents an action event fired by buttons and command panel
import java.awt.event.FocusEvent;     // Focus event fired when the destination folder field loses focus
import java.awt.event.FocusListener;  // Listener interface for focus gain and loss events on the folder field
import java.awt.event.ItemEvent;      // Event fired when the repo combo box selection changes
import java.awt.event.MouseEvent;     // Mouse event used for the repo combo box tooltip override
import java.awt.event.WindowAdapter;  // Adapter for window lifecycle events; used to populate the tree on open
import java.awt.event.WindowEvent;    // Window event fired when the dialog opens
import java.util.List;                // Ordered collection interface for the repo list

import javax.swing.AbstractAction;    // Base class for Swing actions; used for the commented-out default URL action
import javax.swing.JButton;           // Standard Swing push-button for New and Delete Repo
import javax.swing.JLabel;            // Non-interactive label for field captions and the warning label
import javax.swing.JOptionPane;       // Provides informational and confirmation dialog boxes
import javax.swing.JPanel;            // Generic lightweight container for the bottom panel
import javax.swing.JSeparator;        // Horizontal visual divider between the combo row and the info panel

import com.rma.swing.RmaFileChooserField; // RMA file chooser field for selecting the destination directory

import rma.swing.ButtonCmdPanel;          // Panel containing OK, Apply, and Cancel buttons
import rma.swing.ButtonCmdPanelListener;  // Listener interface for command button panel action events
import rma.swing.EnabledJPanel;           // JPanel subclass that propagates setEnabled() to all child components
import rma.swing.RmaImage;                // RMA utility for loading image icons from classpath resources
import rma.swing.RmaInsets;              // Pre-defined Insets constants for consistent component spacing
import rma.swing.RmaJComboBox;           // RMA-extended combo box with generic type support and tooltip override
import rma.swing.RmaJDialog;             // Base class for RMA modal/non-modal dialog windows
import rma.swing.RmaJTextField;          // RMA-extended text field for the repository name
import rma.swing.list.RmaListModel;      // RMA list model backed by a collection for use in RmaJComboBox

import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;    // Action for downloading the study after adding a new repo
import usbr.wat.plugins.actionpanel.gitIntegration.event.RepoSelectionEvent;       // Event fired when a repo URL is selected in the repo tree
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;                 // Data model holding the name, local path, and URL of a Git repository
import usbr.wat.plugins.actionpanel.gitIntegration.ui.RepoJTree;                   // Tree component displaying available remote repositories for URL selection
import usbr.wat.plugins.actionpanel.gitIntegration.utils.GitRepoUtils;             // Utility class for CRUD operations on saved repo definitions

/**
 * Dialog for adding, editing, and deleting Git repository definitions within
 * the WTMP Action Panel's Git integration.
 *
 * Presents a repo combo box for selecting an existing repo definition and a "New"
 * button for creating one. Below a separator, an EnabledJPanel (which propagates
 * enabled/disabled state to all children) contains:
 *
 *   - A Repository Name field (editable only when adding; read-only when editing).
 *   - A Destination Path field with a file chooser; triggers an existing-repo check
 *     when focus is lost or a file is selected.
 *   - A warning label for displaying repo-validation messages.
 *   - A RepoJTree for browsing and selecting the remote repository URL
 *     (disabled when an existing repo is selected; only the destination path is editable).
 *
 * A bottom panel holds a Delete button (with a trash-can icon) and an OK/Apply/Cancel
 * command panel. Apply saves without closing; OK saves and closes.
 *
 * When adding a new repo and the destination folder already contains a Git clone,
 * the source URL is auto-detected from the existing clone and the tree is disabled.
 * After a successful add the user is optionally prompted to download the study.
 *
 * This class is suppressed for serialization warnings because Swing components
 * are not consistently serializable.
 */

@SuppressWarnings("serial")
public class ReposEditor extends RmaJDialog {
	// Default remote repository URL used when no repos exist and no system property overrides it
	private static final String DEFAULT_REPO_URL = "https://gitlab.rmanet.app/RMA/usbr-water-quality/wtmp-development-study/uppersac.git";

	// Default repository name applied when no repos exist and no system property overrides it
	private static final String DEFAULT_REPO_NAME = "Default";

	// Combo box for selecting the existing repository to view or edit
	private RmaJComboBox<RepoInfo> _reposCombo;

	// Button for starting the add-new-repo workflow
	private JButton _addRepoButton;

	// Text field for entering or displaying the repository's display name
	private RmaJTextField _repoNameFld;

	// File chooser field for selecting the local destination directory for the cloned study
	private RmaFileChooserField _destFolderFld;

	// Panel containing the OK, Apply, and Cancel command buttons
	private ButtonCmdPanel _cmdPanel;

	// Button for deleting the currently selected repository definition (enabled only when a repo is selected)
	private JButton _deleteRepoButton;

	// EnabledJPanel containing all repo detail fields; propagates enabled/disabled to all children
	private JPanel _infoPanel;

	// The repo definition currently loaded in the info panel; null when adding a new repo
	private RepoInfo _currentRepo;

	// Placeholder for a "Set Default Source URL" action (currently commented out)
	private AbstractAction _defaultAction;

	// Tree for browsing available remote repositories and selecting a source URL
	private RepoJTree _repoTree;

	// Label for displaying validation warnings (e.g., an existing Git clone detected)
	private JLabel _warningLabel;

	/**
	 * Constructs a ReposEditor owned by the given parent window.
	 *
	 * Builds all UI controls, attaches listeners, populates the repo combo box,
	 * packs the dialog to its preferred size, overrides to 700×460, and centers
	 * it relative to the parent window.
	 *
	 * @param parent the Window that will own this modal dialog
	 */
	public ReposEditor(Window parent) {
		// Initialize the base RmaJDialog as modal
		super(parent, true);

		// Build and arrange all UI controls
		buildControls();

		// Attach combo, button, tree, and window listeners
		addListeners();

		// Populate the combo box with all saved repo definitions
		fillRepoCombo();

		// Size to preferred layout dimensions
		pack();

		// Override with a fixed dialog size for consistent appearance
		setSize(700, 460);

		// Center relative to the parent window
		setLocationRelativeTo(getParent());
	}


	/**
	 * Builds and lays out all UI controls within the dialog content pane.
	 *
	 * Arranges: a "Repository Info:" label with the repo combo and "New" button,
	 * a separator, an EnabledJPanel containing the name field, destination folder
	 * chooser, warning label, and repo tree, a bottom panel with the Delete button
	 * and OK/Apply/Cancel command panel. The info panel starts disabled until a
	 * repo is selected or a new one is being added.
	 */
	protected void buildControls() {
		setTitle("Repository Settings");
		getContentPane().setLayout(new GridBagLayout());

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

		// Create the repo selection combo box; overrides tooltip to show name, URL, and local path
		_reposCombo = new RmaJComboBox<RepoInfo>() {
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

		label.setLabelFor(_reposCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_reposCombo, gbc);

		// Create the "New" button for starting the add-repo workflow
		_addRepoButton = new JButton("New");
		_addRepoButton.setToolTipText("Adds a new Repository Info to WTMP");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(_addRepoButton, gbc);

		// Add a horizontal separator between the combo row and the detail info panel
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);

		// Create the detail info panel; EnabledJPanel propagates enabled state to all children
		_infoPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS0000;
		getContentPane().add(_infoPanel, gbc);

		// Create the "Repository Name:" label
		label = new JLabel("Repository Name:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);

		// Create the repository name text field (editable when adding, read-only when editing)
		_repoNameFld = new RmaJTextField();
		label.setLabelFor(_repoNameFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		_infoPanel.add(_repoNameFld, gbc);

		// Create the "Destination Path:" label
		label = new JLabel("Destination Path:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);

		// Create the file chooser field configured to select directories only
		_destFolderFld = new RmaFileChooserField();
		_destFolderFld.setOpenDirectory();
		label.setLabelFor(_destFolderFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		_infoPanel.add(_destFolderFld, gbc);

		// Create the warning label for displaying repo-validation messages
		_warningLabel = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		_infoPanel.add(_warningLabel, gbc);

		// Create the "Source URL:" label placeholder (currently commented out in layout)
		label = new JLabel("Source URL:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		// label is not added to the panel; the tree fills this area instead

		// Create the repo tree for browsing available remote repositories (Project selection mode)
		_repoTree = new RepoJTree(RepoJTree.SelectionType.Project);

		// Position the repo tree to fill the remaining info panel space
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		_infoPanel.add(_repoTree, gbc);

		// Add an empty spacer label to consume remaining vertical space in the info panel
		label = new JLabel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0001;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		_infoPanel.add(label, gbc);

		// Create the bottom panel for the Delete button and command panel
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0001;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS0000;
		getContentPane().add(bottomPanel, gbc);

		// Create the Delete button with a trash-can icon; initially disabled
		_deleteRepoButton = new JButton(RmaImage.getImageIcon("Images/delete.gif"));
		_deleteRepoButton.setToolTipText("Delete the current repository");
		_deleteRepoButton.setEnabled(false);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5555;
		bottomPanel.add(_deleteRepoButton, gbc);

		// Create the OK/Apply/Cancel command panel
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_APPLY_CANCEL_BUTTONS);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		bottomPanel.add(_cmdPanel, gbc);

		// Start with the info panel disabled until the user selects or adds a repo
		_infoPanel.setEnabled(false);
	}

	/**
	 * Placeholder for a "Set Default Source URL" action; currently a no-op.
	 *
	 * The source URL field and its associated action are commented out in buildControls().
	 */
	protected void setDefaultSourceUrl() {
		// No-op: source URL field and default action are not currently wired up
	}


	/**
	 * Populates the repo combo box with all saved repository definitions.
	 *
	 * Replaces the combo box model entirely with a new RmaListModel built from the
	 * current saved repos list (with a blank first entry for the "no selection" state).
	 */
	private void fillRepoCombo() {
		// Retrieve all saved repo definitions from the persistent storage
		List<RepoInfo> repos = GitRepoUtils.getReposList();

		// Replace the combo box model with the updated list
		RmaListModel<RepoInfo> newModel = new RmaListModel<>(true, repos);
		_reposCombo.setModel(newModel);
	}

	/**
	 * Attaches all event listeners to the dialog's interactive controls.
	 *
	 * Registers: a FocusListener on the destination folder field (to run the
	 * existing-repo check on focus loss), a file-selected listener on the same field,
	 * a WindowAdapter to populate the repo tree on dialog open, an item listener on
	 * the repo combo, action listeners on the New and Delete buttons, a repo-selection
	 * listener on the tree, and a ButtonCmdPanelListener for OK, Apply, and Cancel.
	 */
	protected void addListeners() {
		// Check for an existing Git clone when the destination folder field loses focus
		_destFolderFld.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// No action needed when the field gains focus
			}

			@Override
			public void focusLost(FocusEvent e) {
				// Skip temporary focus loss events (e.g., when a popup opens)
				if (e.isTemporary()) {
					return;
				}
				checkForExistingRepo();
			}
		});

		// Also check for an existing clone when the user selects a folder via the file chooser
		_destFolderFld.addFileSelectedListener(e -> checkForExistingRepo());

		// Populate the repo tree on the EDT after the dialog has fully opened
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				EventQueue.invokeLater(() -> _repoTree.fillRepoTree());
			}
		});

		// Update the info panel fields when the user selects a different repo in the combo
		_reposCombo.addItemListener(e -> reposComboChanged(e));

		// Start the add-new-repo workflow when "New" is clicked
		_addRepoButton.addActionListener(e -> addRepoAction());

		// Prompt for confirmation and delete the selected repo when Delete is clicked
		_deleteRepoButton.addActionListener(e -> deleteRepoAction());

		// Auto-fill the repo name from the tree selection when adding a new repo
		_repoTree.addRepoSelectionListener(e -> repoTreeSelected(e));

		// Handle OK, Apply, and Cancel button clicks from the command panel
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.APPLY_BUTTON:
						// Validate and save without closing
						if (isValidForm()) {
							saveForm();
						}
						break;

					case ButtonCmdPanel.OK_BUTTON:
						// Validate, save, and close on success
						if (isValidForm()) {
							if (saveForm()) {
								setVisible(false);
							}
						}
						break;

					case ButtonCmdPanel.CANCEL_BUTTON:
						// Close without saving
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * Responds to a repo selection event from the RepoJTree.
	 *
	 * When adding a new repo and the name field has not yet been modified by the user,
	 * auto-fills the name field with the repo name from the tree selection.
	 *
	 * @param e the RepoSelectionEvent carrying the selected repository name
	 */
	private void repoTreeSelected(RepoSelectionEvent e) {
		// Only auto-fill when adding a new repo (no current repo loaded and name is editable)
		if (_currentRepo != null || !_repoNameFld.isEditable()) {
			return;
		}

		String name = e.getRepoName();

		// Auto-fill only if the name field has not been manually edited yet
		if (name != null && !_repoNameFld.isModified()) {
			_repoNameFld.setText(name);
		}
	}


	/**
	 * Prompts for confirmation and deletes the currently selected repository definition.
	 *
	 * Shows a confirmation dialog listing the repo's name, local path, and URL. On
	 * confirmation, removes the repo from the persistent store and from the combo box.
	 * If the deleted repo was the currently loaded one, clears the form.
	 */
	private void deleteRepoAction() {
		RepoInfo repo = getSelectedRepo();

		if (repo != null) {
			// Show a confirmation dialog with a summary of the repo to be deleted
			int opt = JOptionPane.showConfirmDialog(this, "<html>Ok to delete Repository " + repo.getName()
							+ "<br>Local Folder : " + repo.getLocalPath() + "<br>Repository URL : " + repo.getSourceUrl()
							+ "<br>?<br><br>This will not delete any files on disk.",
					"Confirm Deletion", JOptionPane.YES_NO_OPTION);

			if (opt == JOptionPane.YES_OPTION) {
				if (GitRepoUtils.deleteRepo(repo)) {
					// If the deleted repo was the current one, clear all form fields
					if (_currentRepo == repo) {
						clearForm();
					}

					// Remove the repo from the combo box
					_reposCombo.removeItem(repo);

					// If only the blank entry remains, select it to reset the form
					if (_reposCombo.getItemCount() == 1) {
						_reposCombo.setSelectedIndex(0);
					}
				}
			}
		}
	}


	/**
	 * Returns the RepoInfo currently selected in the repo combo box.
	 *
	 * @return the selected RepoInfo, or null if no repo is selected
	 */
	private RepoInfo getSelectedRepo() {
		return (RepoInfo) _reposCombo.getSelectedItem();
	}


	/**
	 * Checks whether the selected destination folder already contains a Git clone.
	 *
	 * If a clone is detected, reads its remote URL and pre-selects it in the repo tree,
	 * then disables the tree (the URL is already known). If no clone is detected,
	 * re-enables the tree for manual URL selection.
	 */
	protected void checkForExistingRepo() {
		String localFolder = _destFolderFld.getPath();

		if (!localFolder.isEmpty() && GitRepoUtils.hasGitRepo(localFolder)) {
			// Existing clone found: read its remote URL and pre-select it in the tree
			RepoInfo info = new RepoInfo();
			info.setLocalPath(localFolder);
			GitRepoUtils.getRepoInfo(info);

			if (info.getSourceUrl() != null) {
				// Pre-select the detected URL and lock the tree since we already know the source
				_repoTree.setSelectedPath(info.getSourceUrl());
				_repoTree.setEnabled(false);
			}
		} else {
			// No existing clone: allow the user to pick a URL from the tree
			_repoTree.setEnabled(true);
		}
	}

	/**
	 * Clears all editable fields in the info panel.
	 *
	 * Resets the name field, destination folder field, and repo tree selection.
	 */
	@Override
	public void clearForm() {
		_repoNameFld.clearPerformed();
		_destFolderFld.clearPerformed();
		_repoTree.clearPerformed();
	}

	/**
	 * Prepares the form for adding a new repository.
	 *
	 * Clears all fields, deselects the combo, enables the info panel and name field,
	 * enables the repo tree, and sets _currentRepo to null.
	 */
	private void addRepoAction() {
		// Clear any previously loaded repo data
		clearForm();

		// Deselect the combo so no existing repo appears to be selected
		_reposCombo.setSelectedIndex(-1);

		// Enable all fields in the info panel for the new-repo workflow
		_infoPanel.setEnabled(true);
		_repoNameFld.setEditable(true);
		_repoTree.setEnabled(true);

		// Mark that no existing repo is being edited
		_currentRepo = null;
	}

	/**
	 * Responds to repo combo box selection changes.
	 *
	 * Ignores DESELECTED events. For a valid selection, populates the info panel with
	 * the repo's name (read-only), destination path (editable), and tree URL (read-only).
	 * For no selection (blank entry), disables the info panel and clears all fields.
	 *
	 * @param e the ItemEvent describing the combo box selection change
	 */
	private void reposComboChanged(ItemEvent e) {
		// Ignore the DESELECTED event fired for the previously selected item
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		RepoInfo repo = (RepoInfo) _reposCombo.getSelectedItem();

		if (repo != null) {
			// Enable the info panel and populate all fields from the selected repo
			_infoPanel.setEnabled(true);
			_repoNameFld.setText(repo.getName());

			// Name is read-only when editing an existing repo (renaming not supported)
			_repoNameFld.setEditable(false);
			_destFolderFld.setText(repo.getLocalPath());

			// Destination path is editable to allow moving the local clone
			_destFolderFld.setEditable(true);

			// Show the repo's URL in the tree (read-only; source URL cannot be changed)
			_repoTree.setSelectedPath(repo.getSourceUrl());
			_repoTree.setEnabled(false);

			// Store the loaded repo for save logic
			_currentRepo = repo;

			// Enable the Delete button since a specific repo is now loaded
			_deleteRepoButton.setEnabled(true);
		} else {
			// No selection: disable all info panel fields and clear any loaded data
			_infoPanel.setEnabled(false);
			_repoNameFld.clearPerformed();
			_destFolderFld.clearPerformed();
			_repoTree.setSelectedPath(null);
			_currentRepo = null;
			_deleteRepoButton.setEnabled(false);
		}

		// Clear the modified flag since this was a programmatic population, not a user edit
		setModified(false);
	}

	/**
	 * Saves the current form state to the persistent repo store.
	 *
	 * If adding a new repo (_currentRepo == null and name is editable): creates a new
	 * RepoInfo, checks for an existing Git clone in the destination, saves via
	 * GitRepoUtils.addRepo(), adds the new repo to the combo, and optionally prompts
	 * the user to immediately download the study. If editing an existing repo: updates
	 * the local path and source URL and writes the changes.
	 *
	 * @return true if the save succeeded or no changes were needed; false if the download failed
	 */
	private boolean saveForm() {
		// No changes to save if the form has not been modified
		if (!isModified()) {
			return true;
		}

		boolean rv = true;

		if (_currentRepo == null && _repoNameFld.isEditable()) {
			// Adding a new repo: build the RepoInfo from the current field values
			RepoInfo repo = new RepoInfo();
			repo.setName(_repoNameFld.getText().trim());
			repo.setLocalPath(_destFolderFld.getText().trim());
			repo.setSourceUrl(_repoTree.getRepoUrl().trim());

			// Warn if the destination already has a Git clone configured
			if (GitRepoUtils.hasGitRepo(repo.getLocalPath())) {
				int opt = JOptionPane.showConfirmDialog(this, "<html>The folder " + repo.getLocalPath()
						+ " appears to already have been configured to work with Git.<br>Do you want to add it anyway?", "Existing Repo", JOptionPane.YES_NO_OPTION);
				if (JOptionPane.YES_OPTION != opt) {
					return rv;
				}
			}

			// Save the new repo and add it to the combo if successful
			if (GitRepoUtils.addRepo(repo, true)) {
				_reposCombo.addItem(repo);
				_reposCombo.setSelectedItem(repo);

				// Offer to download the study immediately after adding the repo
				int opt = JOptionPane.showConfirmDialog(this, "Do you want to download the study at " + repo.getSourceUrl(), "Download Repo", JOptionPane.YES_NO_OPTION);
				if (JOptionPane.YES_OPTION == opt) {
					DownloadStudyAction action = new DownloadStudyAction(this, repo);
					String msg = "Download Complete";

					if (!action.downloadStudyAction()) {
						// Report the failure but do not block the dialog from closing
						msg = "Download Failed";
						rv = false;
					}

					JOptionPane.showMessageDialog(this, msg, "Status", JOptionPane.INFORMATION_MESSAGE);
				}

				setModified(false);
			}
		} else if (_currentRepo != null) {
			// Editing an existing repo: update the mutable fields and persist
			_currentRepo.setLocalPath(_destFolderFld.getText());
			_currentRepo.setSourceUrl(_repoTree.getRepoUrl());
			GitRepoUtils.writeRepo(_currentRepo);
			setModified(false);
		}

		// Re-enable the New button after a save (it may have been disabled during configure-for-add)
		_addRepoButton.setEnabled(true);
		return rv;
	}

	/**
	 * Validates the current form state before saving.
	 *
	 * Skips validation if no editable fields are present (existing repo with no changes).
	 * Otherwise checks that the name field is non-empty, a repository URL is selected in
	 * the tree, and the selected URL passes the GitRepoUtils validity check.
	 *
	 * @return true if the form is valid; false if any check fails
	 */
	protected boolean isValidForm() {
		// Skip validation if no editable name field is present (editing without a name change)
		if (_currentRepo == null && !_repoNameFld.isEditable()) {
			return true;
		}

		// Require a non-empty repo name
		String name = _repoNameFld.getText().trim();
		if (name.isEmpty()) {
			String msg = "Please Enter a name for the Repository";
			String title = "No Name Entered";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Require a selected repo URL from the tree
		String remoteUrl = _repoTree.getRepoPath();
		if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
			String msg = "Please Select a Location for the Repository";
			String title = "No Repository Selected";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// Validate that the selected URL is a reachable Git remote
		if (!GitRepoUtils.isValidRemoteUrl(remoteUrl)) {
			String msg = "Invalid Repository Location Selected";
			String title = "Invalid Repository URL";
			JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		// All checks passed
		return true;
	}

	/**
	 * Resets the modified flag to indicate no unsaved changes.
	 *
	 * Called after a successful save to synchronize the dialog's dirty state.
	 */
	public void fillForm() {
		setModified(false);
	}


	/**
	 * Configures the dialog for the add-new-repo workflow with optional defaults.
	 *
	 * Sets the title to "Add Repository", clicks the New button to clear the form,
	 * and if no repos exist yet, pre-fills the URL from the DefaultRepo.Url system
	 * property (or DEFAULT_REPO_URL) and the name from DefaultRepo.Name (or DEFAULT_REPO_NAME).
	 * Also disables the New button so the user cannot click it again during this flow.
	 */
	public void configureForAdd() {
		// Switch the dialog title to "Add Repository" for this workflow
		setTitle("Add Repository");

		// Simulate a click on the New button to clear the form and enter add mode
		_addRepoButton.doClick();

		if (_reposCombo.getItemCount() == 0) {
			// No existing repos: pre-fill with the default URL from system properties or constant
			String defRepoUrl = System.getProperty("DefaultRepo.Url");
			if (defRepoUrl == null) {
				defRepoUrl = DEFAULT_REPO_URL;
			}
			_repoTree.setSelectedPath(defRepoUrl);

			// Pre-fill the name from system properties or the default constant
			String defRepoName = System.getProperty("DefaultRepo.Name");
			if (defRepoName == null) {
				defRepoName = DEFAULT_REPO_NAME;
			}
			_repoNameFld.setText(defRepoName);

			// Disable New so the user cannot start a second add while this one is in progress
			_addRepoButton.setEnabled(false);
		}
	}


	/**
	 * Programmatically selects the given RepoInfo in the repo combo box.
	 *
	 * @param repo the RepoInfo to select; has no effect if the item is not in the combo model
	 */
	public void setSelectedRepo(RepoInfo repo) {
		_reposCombo.setSelectedItem(repo);
	}
}
