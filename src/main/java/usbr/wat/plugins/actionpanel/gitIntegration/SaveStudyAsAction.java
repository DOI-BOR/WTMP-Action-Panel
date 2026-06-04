package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.Cursor;               // Provides cursor types; used to show a wait cursor during the save-as operation
import java.awt.event.ActionEvent;    // Represents an action event fired when the Save Study As button is clicked
import java.io.BufferedWriter;        // Buffered character writer used for writing the save-as marker file
import java.io.IOException;           // Checked exception thrown when writing the marker file fails
import java.util.Date;                // Provides the current date/time stamp written to the marker file
import java.util.logging.Logger;      // JDK logger for recording marker-file write failures

import javax.swing.AbstractAction;   // Base class for Swing Action implementations that back a JButton
import javax.swing.Action;           // Interface defining action properties; used to set the SHORT_DESCRIPTION tooltip

import com.google.common.flogger.FluentLogger;    // Google Flogger for structured logging of the marker-file path
import com.rma.client.Browser;                    // RMA browser frame; used to retrieve the current project label
import com.rma.client.BrowserI18n;               // RMA internationalization helper for browser message keys
import com.rma.client.Messages;                   // RMA message key constants; provides SAVE_PROJECT_AS_LABEL
import com.rma.client.NewBrowserProjectFactory;   // Fallback project factory used when no specific factory is registered
import com.rma.client.NewObjectDialog;            // Dialog for collecting a name, description, and location for a new object
import com.rma.event.ProjectAdapter;              // Adapter providing empty implementations of project lifecycle events
import com.rma.factories.NewObjectFactoryList;    // Registry for looking up registered new-object factories by label
import com.rma.factories.NewProjectFactory;       // Base factory interface for creating new RMA project objects
import com.rma.io.FileManagerImpl;               // RMA file manager for obtaining RmaFile references and writing files
import com.rma.io.RmaFile;                       // RMA abstraction representing a file system path
import com.rma.model.Project;                    // Represents the currently loaded RMA project
import com.rma.model.SaveAsProjectCmd;           // Base command interface for saving the project to a new location

import hec2.wat.command.WATSaveAsProjectCommand; // HEC-WAT extension of SaveAsProjectCmd that handles WAT-specific copy and project open logic
import hec2.wat.factories.NewWatProjectFactory;  // WAT-specific new project factory; supports enabling the save-as panel

import rma.util.RMAIO;                           // RMA I/O utility providing path concatenation and other helpers

/**
 * Swing Action for saving the currently open WAT study to a new name and location
 * ("Save Study As...") within the WTMP Action Panel's Git integration workflow.
 *
 * Opens a NewObjectDialog pre-configured for saving the project to a new location,
 * then executes a UsbrSaveAsProjectCmd to perform the copy. After copying, a hidden
 * marker file (SAVED_STUDY_AS_FILE) is written to the destination project directory
 * recording the source path, the user who performed the save, and the date/time.
 * This marker file is later used by RepoButtonPanel to determine whether the Upload
 * button should be enabled even without a selected repository.
 *
 * This class is suppressed for serialization warnings because Swing Action
 * implementations are not consistently serializable.
 */
@SuppressWarnings("serial")
public class SaveStudyAsAction extends AbstractAction {
    // Name of the hidden marker file written to the destination project directory after a save-as
    public static final String SAVED_STUDY_AS_FILE = ".savedStudyAs";

    // Reference to the parent StudyStorageDialog; used for cursor management and window ownership
    private StudyStorageDialog _studyStorageDialog;

    // Unused project listener (currently commented out); retained for future use
    private ProjectAdapter _listener;

    // The destination Project object created by the NewObjectDialog; stored for use in finishSaveAs()
    private Project _saveAsProject;

    /**
     * Constructs a SaveStudyAsAction associated with the given StudyStorageDialog.
     *
     * Sets the action's display name to "Save Study As..." and its short description
     * (tooltip) to describe the action's purpose.
     *
     * @param studyStorageDialog the dialog that owns this action; used for cursor management
     */
    public SaveStudyAsAction(StudyStorageDialog studyStorageDialog) {
        // Set the button label for this action
        super("Save Study As...");

        // Set the tooltip text shown when hovering over the button
        putValue(Action.SHORT_DESCRIPTION, "Saves the Study to a new location with a new name");

        // Store the dialog reference for cursor and window management
        _studyStorageDialog = studyStorageDialog;
    }

    /**
     * Invoked when the Save Study As button is clicked.
     *
     * Delegates to saveProjectAsAction() to perform the save-as workflow.
     *
     * @param e the ActionEvent fired by the button
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        saveProjectAsAction();
    }

    /**
     * Executes the save-as workflow: saves the current project, opens a dialog for
     * the user to specify the new name and location, then executes UsbrSaveAsProjectCmd.
     *
     * Shows a wait cursor on the dialog root pane for the duration of the operation,
     * restoring it in a finally block. Prevents the dialog from being closed by the OS
     * during the operation via setSystemClosable(false).
     *
     * @return true if the save-as succeeded; false if the user canceled or the command failed
     */
    public boolean saveProjectAsAction() {
        // Show the wait cursor to indicate a potentially long-running operation
        _studyStorageDialog.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Prevent the OS close button from dismissing the dialog during save
        _studyStorageDialog.setSystemClosable(false);

        try {
            // Get the currently loaded project and flush all pending changes to disk
            Project project = Project.getCurrentProject();
            project.saveProject();

            // Look up the registered new-project factory for the current project type label
            NewProjectFactory factory = (NewProjectFactory) NewObjectFactoryList
                    .getFactoryFor(Browser.getBrowser().getProjectLabel());

            if (factory == null) {
                // Fall back to a generic browser project factory if no specific one is registered
                factory = new NewBrowserProjectFactory(project);
            }

            if (factory instanceof NewWatProjectFactory) {
                // Enable the save-as panel within the WAT project factory for this workflow
                ((NewWatProjectFactory) factory).setSaveAsPanel(true);
            }

            // Open the save-as dialog with the resolved factory
            NewObjectDialog dialog = new NewObjectDialog(_studyStorageDialog, factory);

            // Set the dialog title using the localized "Save Project As" message format
            String title = BrowserI18n.getI18n(Messages.SAVE_PROJECT_AS_LABEL).format(Browser.getBrowser().getProjectLabel());
            dialog.setTitle(title);
            dialog.setVisible(true);

            // If the user canceled the dialog, return false without performing any copy
            if (dialog.isCanceled()) {
                return false;
            }

            // Retrieve the newly defined destination Project object from the dialog
            _saveAsProject = (Project) dialog.getNewObject();

            // Create the USBR save-as command and execute the copy
            SaveAsProjectCmd cmd = new UsbrSaveAsProjectCmd();
            if (cmd != null) {
                cmd.saveProjectAs(project, _saveAsProject);
                return true;
            }

            return false;
        } finally {
            // Always restore the default cursor when the operation completes
            _studyStorageDialog.getRootPane().setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Post-save-as hook intended to trigger repository creation for the new project.
     *
     * Currently a no-op; the body is commented out pending a decision on whether to
     * automatically create a Git repo for the saved project. Returns immediately if
     * the current project is the "no project" placeholder.
     */
    protected void finishSaveAs() {
        Project currProject = Project.getCurrentProject();

        // Do nothing if no real project is currently loaded
        if (currProject.isNoProject()) {
            return;
        }
    }

    /**
     * USBR-specific extension of WATSaveAsProjectCommand that writes a marker file
     * to the destination project directory after the copy completes.
     *
     * Overrides copyFinished() to call writeSaveAsMarkerLine(), which records the
     * source project path, the user who performed the save, and the current date/time
     * in a hidden file named SAVED_STUDY_AS_FILE. This marker is later checked by
     * RepoButtonPanel.shouldEnableUploadButton() to enable the Upload button.
     *
     * The openProject() override is currently identical to the superclass behavior;
     * a listener-based finishSaveAs() call is commented out for future consideration.
     */
    class UsbrSaveAsProjectCmd extends WATSaveAsProjectCommand {
        /**
         * Constructs a UsbrSaveAsProjectCmd with default settings from the superclass.
         */
        public UsbrSaveAsProjectCmd() {
            super();
        }

        /**
         * Opens the newly saved project after the copy completes.
         *
         * Delegates to the superclass implementation. A project-listener-based call to
         * finishSaveAs() is currently commented out for future consideration.
         */
        @Override
        protected void openProject() {
            // Open the destination project using the standard WAT save-as behavior
            super.openProject();

        }

        /**
         * Called by the superclass after all files have been copied to the destination.
         *
         * Delegates to the superclass, then writes the save-as marker file recording
         * provenance information about this save operation.
         *
         * @param totalCopied the total number of files copied during the save-as operation
         */
        @Override
        public void copyFinished(int totalCopied) {
            // Invoke the standard WAT copy-finished behavior (e.g., progress reporting)
            super.copyFinished(totalCopied);

            // Write the hidden marker file to the destination project directory
            writeSaveAsMarkerLine();
        }

        /**
         * Writes the save-as marker file to the destination project directory.
         *
         * The marker file (SAVED_STUDY_AS_FILE) records the source project path, the
         * username of the person who performed the save, and the current date and time.
         * This information is used for auditing and by the upload-button enable logic.
         *
         * Logs a warning if the write fails; silently ignores errors when closing the writer.
         */
        private void writeSaveAsMarkerLine() {
            // Build the full path to the marker file in the destination project directory
            String dir = _saveAsProject.getProjectDirectory();
            String fname = RMAIO.concatPath(dir, SAVED_STUDY_AS_FILE);

            // Log that the marker file is being written for diagnostic purposes
            FluentLogger.forEnclosingClass().atInfo().log("Writing save as marker file to %s", fname);

            // Obtain an RmaFile reference and open a buffered writer for it
            RmaFile markerFile = FileManagerImpl.getFileManager().getFile(fname);
            BufferedWriter writer = markerFile.getBufferedWriter();

            try {
                // Record the absolute path of the source project file
                writer.write("Saved From:" + _origFile.getAbsolutePath());
                writer.newLine();

                // Record the OS username of the person who performed the save
                writer.write("Saved by:" + System.getProperty("user.name"));
                writer.newLine();

                // Record the current date and time of the save operation
                writer.write("Saved On:" + new Date().toString());
                writer.newLine();

                // Placeholder for future: record the previous Git remote URL if applicable
            } catch (IOException e) {
                // Log a warning if the marker file cannot be written
                Logger.getLogger("SaveStudyAsAction").warning("writeSaveAsMarkerLine:IOException writing " + markerFile.getAbsolutePath() + " error:" + e);
            } finally {
                // Always close the writer to release the file handle
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        // Silently ignore close failures
                    }
                }
            }
        }
    }
}
