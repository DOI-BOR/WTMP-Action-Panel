package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import com.rma.io.DssFileManagerImpl;                           // Provides DssFileManagerImpl for reading and closing HEC-DSS files by file path
import com.rma.model.Project;                                   // Provides Project for resolving relative and absolute file paths within the current project
import com.rma.swing.RmaFileChooserField;                       // Provides RmaFileChooserField for the file browse-and-text-entry component used to select a DSS file

import hec.data.Parameter;                                      // Provides Parameter for looking up temperature unit strings by unit system (SI, English)
import hec.data.Units;                                          // Provides Units for unit-system constants (SI_ID, ENGLISH_ID) used when populating the units combo
import hec.heclib.dss.CondensedReference;                       // Provides CondensedReference for entries in the DSS condensed catalogue used to build collection mappings
import hec.heclib.dss.DSSPathname;                              // Provides DSSPathname for parsing and accessing individual path parts (B-part, F-part) of DSS records
import hec.lang.NamedType;                                      // Provides NamedType for extracting names from EnsembleSet objects via method references

import rma.swing.ButtonCmdPanel;                                // Provides ButtonCmdPanel for the standard OK/Cancel button row at the bottom of the dialog
import rma.swing.RmaInsets;                                     // Provides RmaInsets constants for consistent padding values used in GridBagConstraints
import rma.swing.RmaJComboBox;                                  // Provides RmaJComboBox, an RMA-extended combo box used for river location and units selection
import rma.swing.RmaJDescriptionField;                          // Provides RmaJDescriptionField for multi-line description text entry in both panels
import rma.swing.RmaJDialog;                                    // Provides RmaJDialog, the RMA base modal dialog class that this dialog extends
import rma.swing.RmaJIntegerField;                              // Provides RmaJIntegerField for the bounded integer entry field for the number of temperature targets
import rma.swing.RmaJPanel;                                     // Provides RmaJPanel, an RMA-extended JPanel used for the card and sub-panels
import rma.swing.RmaJRadioButton;                               // Provides RmaJRadioButton for the "Import" and "Create New" mode selection radio buttons
import rma.swing.RmaJTable;                                     // Provides RmaJTable for the temperature sets selection table in the import panel
import rma.swing.RmaJTextField;                                 // Provides RmaJTextField for the name text entry field in the create panel
import rma.swing.list.RmaListModel;                             // Provides RmaListModel for constructing combo box models with optional blank first items
import rma.swing.table.RmaTableModel;                           // Provides RmaTableModel for accessing addRow and fireTableDataChanged on the table model
import rma.util.RMAFilenameFilter;                              // Provides RMAFilenameFilter for restricting the file chooser to .dss files only

import usbr.wat.plugins.actionpanel.model.SharedConfigFiles;                // Provides SharedConfigFiles for resolving the shared river locations config file path
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;             // Provides EnsembleSet for identifying ensemble sets that depend on a temperature target set being overwritten
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;        // Provides ForecastSimGroup for looking up existing temperature target sets and their dependents
import usbr.wat.plugins.actionpanel.model.forecast.RiverLocation;           // Provides RiverLocation, the model object populated in the river location combo boxes
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;    // Provides TemperatureTargetSet, the model object constructed and returned by this dialog
import usbr.wat.plugins.actionpanel.ui.forecast.CsvReader;                  // Provides CsvReader for reading the river location config CSV into typed RiverLocation objects

import javax.swing.ButtonGroup;                                 // Provides ButtonGroup for grouping the Import and Create New radio buttons as mutually exclusive
import javax.swing.DefaultComboBoxModel;                        // Provides DefaultComboBoxModel (imported for potential combo model construction)
import javax.swing.JButton;                                     // Provides JButton for the OK, Cancel, and file chooser ellipsis buttons
import javax.swing.JCheckBox;                                   // Provides JCheckBox for the internal checkbox editor reference used to detect checkbox state changes
import javax.swing.JComboBox;                                   // Provides JComboBox for the river location combo box embedded in the import table column
import javax.swing.JLabel;                                      // Provides JLabel for static descriptive labels throughout both panels
import javax.swing.JOptionPane;                                 // Provides JOptionPane for displaying error, warning, and confirmation dialogs
import javax.swing.SwingUtilities;                              // Provides SwingUtilities for walking the component hierarchy during focus-lost validation
import javax.swing.table.TableColumn;                           // Provides TableColumn for configuring the preferred and maximum width of the checkbox column
import javax.swing.table.TableColumnModel;                      // Provides TableColumnModel for accessing individual columns in the temperature sets table

import java.awt.BorderLayout;                                   // Provides BorderLayout for the radio button panel that holds the Import and Create buttons
import java.awt.CardLayout;                                     // Provides CardLayout for switching between the import panel and the create panel
import java.awt.Component;                                      // Provides Component for identifying the focus-gaining component during focus-lost events
import java.awt.Cursor;                                         // Provides Cursor for switching to a wait cursor during the (potentially slow) DSS file read
import java.awt.Dimension;                                      // Provides Dimension for setting the preferred viewport height of the table and the dialog size
import java.awt.GridBagConstraints;                             // Provides GridBagConstraints for controlling component placement within GridBagLayout containers
import java.awt.GridBagLayout;                                  // Provides GridBagLayout, a flexible layout manager used throughout both card panels
import java.awt.Window;                                         // Provides Window for accepting the parent window reference in the constructor
import java.awt.event.FocusAdapter;                             // Provides FocusAdapter for implementing the focus-lost handler on the file chooser field
import java.awt.event.FocusEvent;                               // Provides FocusEvent for accessing the opposite (focus-gaining) component during focus transitions
import java.awt.event.FocusListener;                            // Provides FocusListener as the interface type returned by getImportFocusListener
import java.awt.event.KeyAdapter;                               // Provides KeyAdapter for implementing the key-released validation listener on text fields
import java.awt.event.KeyEvent;                                 // Provides KeyEvent for receiving key-released events in the validate key listener
import java.awt.event.KeyListener;                              // Provides KeyListener as the interface type returned by getValidateKeyListener
import java.awt.event.WindowAdapter;                            // Provides WindowAdapter for intercepting the window-closing event to trigger closeDialogAction
import java.awt.event.WindowEvent;                              // Provides WindowEvent for the window-closing event delivered to the WindowAdapter

import java.io.IOException;                                     // Provides IOException for handling errors during river location CSV reading and invalid file deletion
import java.nio.file.Files;                                     // Provides Files for deleting invalid DSS files from disk via deleteIfExists
import java.nio.file.Path;                                      // Provides Path for representing and resolving file system paths
import java.nio.file.Paths;                                     // Provides Paths for constructing Path instances from file name strings

import java.util.ArrayList;                                     // Provides ArrayList for mutable lists of TemperatureTargetSet, DSSPathname, and invalid file names
import java.util.Arrays;                                        // Provides Arrays for wrapping varargs into a List when constructing table row Vectors
import java.util.List;                                          // Provides the List interface for ordered collections used throughout this class
import java.util.Map;                                           // Provides the Map interface for the DSS collection mapping keyed by collection ID string
import java.util.SortedSet;                                     // Provides SortedSet for iterating over collection IDs in alphabetical order when populating the table
import java.util.TreeMap;                                       // Provides TreeMap for the sorted DSS collection mapping that orders entries by collection ID
import java.util.TreeSet;                                       // Provides TreeSet for obtaining an alphabetically ordered view of the collection mapping keys
import java.util.Vector;                                        // Provides Vector for constructing row data passed to RmaTableModel.addRow
import java.util.logging.Level;                                 // Provides Logger for JUL-based logging of file errors and save failures
import java.util.logging.Logger;                                // Provides Level for specifying log severity (WARNING, CONFIG) in Logger calls
import java.util.stream.Collectors;                             // Provides Collectors for grouping DSS catalogue entries and collecting stream results to Lists

/**
 * A modal dialog that allows the user to either import temperature target sets from an
 * existing HEC-DSS file or create a new user-defined temperature target set from scratch.
 *
 * The dialog uses a CardLayout to switch between two panels:
 *
 * Import panel (IMPORT_PANEL_ID) — the user selects a .dss file via a file chooser,
 * the DSS catalogue is read and grouped into named collections, each collection appears
 * as a checkable row in the temperature sets table, and the user optionally assigns a
 * river location to each checked row before clicking OK.
 *
 * Create panel (CREATE_PANEL_ID) — the user enters a name, description, river location,
 * number of temperature targets (up to MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET), and
 * preferred units for a brand-new user-defined set.
 *
 * On OK, duplicate names are checked against the existing set list; if a duplicate is
 * found the user is prompted to confirm an overwrite, which also cascades to removing
 * dependent EnsembleSets. The resulting TemperatureTargetSet list is forwarded to the
 * caller via the TempTargetConsumer callback.
 *
 * @see TemperatureTargetSet
 * @see TempTargetConsumer
 * @see ForecastSimGroup
 */
public final class TempTargetImportDialog extends RmaJDialog {
    /**
     * JUL logger scoped to this class for file read errors and save failure reporting.
     */
    private static final Logger LOGGER = Logger.getLogger(TempTargetImportDialog.class.getName());

    /**
     * CardLayout key identifying the import-from-DSS card panel.
     */
    private static final String IMPORT_PANEL_ID = "IMPORT_PANEL";

    /**
     * CardLayout key identifying the create-new-set card panel.
     */
    private static final String CREATE_PANEL_ID = "CREATE_PANEL";

    /**
     * Maximum number of temperature targets allowed in a single user-defined set.
     */
    private static final int MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET = 12;

    /**
     * Callback that receives and processes the list of TemperatureTargetSet objects on OK.
     */
    private final TempTargetConsumer _consumeTempTargetSetAction;

    /**
     * Names of temperature target sets already present in the simulation group; used for duplicate detection.
     */
    private final List<String> _existingSetNames;

    /**
     * The active ForecastSimGroup; used to check for existing sets and their EnsembleSet dependents.
     */
    private final ForecastSimGroup _fsg;

    /**
     * Radio button for selecting the "Import Set From Existing" (DSS file) mode.
     */
    private RmaJRadioButton _importFromExistingRadioButton;

    /**
     * Radio button for selecting the "Create New Set" (user-defined) mode.
     */
    private RmaJRadioButton _createNewRadioButton;

    /**
     * Panel containing the OK and Cancel buttons at the bottom of the dialog.
     */
    private ButtonCmdPanel _okCancelPanel;

    /**
     * Panel with a CardLayout that hosts the import and create sub-panels.
     */
    private RmaJPanel _cardPanel;

    /**
     * File chooser field for selecting the source .dss file in the import panel.
     */
    private RmaFileChooserField _importFileChooserField;

    /**
     * Text field for entering the name of the new temperature target set in the create panel.
     */
    private RmaJTextField _nameTextField;

    /**
     * Description field for the new temperature target set in the create panel.
     */
    private RmaJDescriptionField _descriptionField;

    /**
     * Sorted mapping from collection ID string to the list of DSSPathname objects in that collection.
     * Built from the DSS condensed catalogue each time a new DSS file is selected.
     * Uses a TreeMap so keys are returned in alphabetical order.
     */
    private final Map<String, List<DSSPathname>> _dssCollectionMapping = new TreeMap<>();

    /**
     * Guard flag that suppresses the focusLost handler on the file chooser field while an
     * error dialog is showing, preventing a recursive call to dssFileSelected.
     */
    private boolean _ignoreFocusLost;

    /**
     * Accumulates relative paths of DSS files that failed validation and should be deleted from disk.
     */
    private final List<String> _invalidFilesToDelete = new ArrayList<>();

    /**
     * Button group ensuring that only one of the Import / Create New radio buttons is selected at a time.
     */
    private ButtonGroup _importCreateButtonGroup;

    /**
     * Table in the import panel that lists available temperature target set collections from the DSS file.
     */
    private RmaJTable _temperatureSetsTable;

    /**
     * Reference to the JCheckBox used as the cell editor in column 0 of the temperature sets table.
     * Kept to detect checkbox state changes and re-validate the OK button.
     */
    private JCheckBox _checkBoxEditorCheckBox;

    /**
     * Description field for the imported temperature target set in the import panel.
     */
    private RmaJDescriptionField _descriptionFieldImport;

    /**
     * Integer field for specifying the number of user-defined temperature targets in the create panel.
     */
    private RmaJIntegerField _numberTempTargetsField;

    /**
     * River location combo box displayed in the create panel for assigning a location to the new set.
     */
    private RmaJComboBox<RiverLocation> _riverLocationCombo;

    /**
     * River location combo box embedded in column 2 of the import table for per-row location assignment.
     */
    private JComboBox<RiverLocation> _riverLocationTableCombo;

    /**
     * Units combo box in the create panel, populated with English (°F) and SI (°C) temperature unit strings.
     */
    private RmaJComboBox<String> _unitsComboBox;

    /**
     * Constructs and displays the TempTargetImportDialog. Builds all UI controls,
     * populates river location combo boxes from the shared config CSV, registers all
     * event listeners, packs the dialog, and makes it visible. The dialog is modal
     * and blocks the calling thread until it is closed.
     *
     * @param parent                     the parent Window used for dialog positioning
     * @param existingSetNames           names of temperature target sets already in the simulation group;
     *                                   used for duplicate name detection on OK
     * @param fsg                        the active ForecastSimGroup providing existing set lookups
     * @param consumeTempTargetSetAction callback that receives the selected/created sets on OK
     */
    public TempTargetImportDialog(Window parent, List<String> existingSetNames, ForecastSimGroup fsg, TempTargetConsumer consumeTempTargetSetAction) {
        super(parent, true);
        setTitle("Select Temperature Target Set");
        getContentPane().setLayout(new GridBagLayout());

        _existingSetNames = existingSetNames;
        _fsg = fsg;
        _consumeTempTargetSetAction = consumeTempTargetSetAction;

        // Build all controls, then populate data, then wire listeners
        buildControls();
        fillRiverLocations();
        addListeners();

        // Size, position, and show the dialog
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(getSize());
        setSize(new Dimension(400, 350));
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /**
     * Registers all event listeners required by this dialog: key listeners for
     * OK-button validation, a focus listener and file-selected listener on the
     * file chooser, radio button action listeners for card switching, OK/Cancel
     * button actions, a window closing listener, table model listeners, and the
     * checkbox action listener.
     */
    private void addListeners() {
        // Re-validate the OK button whenever text is typed in the name or file chooser fields
        _nameTextField.addKeyListener(getValidateKeyListener());
        _importFileChooserField.addKeyListener(getValidateKeyListener());

        // Trigger DSS file processing when focus leaves the file chooser text field
        _importFileChooserField.addFocusListener(getImportFocusListener());

        // Trigger DSS file processing when a file is selected via the chooser dialog
        _importFileChooserField.addFileSelectedListener(f -> dssFileSelected());

        // Clean up invalid files when the ellipsis (browse) button is pressed
        ((JButton) _importFileChooserField.getComponents()[0]).addActionListener(e -> ellipsesPressed());

        // Switch to the import card and re-validate when the Import radio is selected
        _importFromExistingRadioButton.addActionListener(e -> importRadioAction());

        // Switch to the create card and re-validate when the Create New radio is selected
        _createNewRadioButton.addActionListener(e -> createNewRadioAction());

        // Close the dialog (with unsaved-changes guard) when Cancel is clicked
        _okCancelPanel.getButton(ButtonCmdPanel.CANCEL_BUTTON).addActionListener(e -> closeDialogAction());

        // Process and forward the selected sets when OK is clicked
        _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON).addActionListener(e -> okAction());

        // Intercept the window-close button (X) to run the same guarded close logic
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialogAction();
            }
        });

        // Re-validate the OK button whenever the temperature sets table data changes
        _temperatureSetsTable.getModel().addTableModelListener(e -> validateOkButton());

        // Re-validate when the river location combo selection changes
        _riverLocationCombo.addActionListener(e -> validateOkButton());

        // Re-validate on any table model event (belt-and-suspenders with the above listener)
        _temperatureSetsTable.addTableModelListener(e -> validateOkButton());

        // Refresh the table to reflect the new checkbox state when a checkbox is toggled
        _checkBoxEditorCheckBox.addActionListener(e -> checkBoxSelected());
    }

    /**
     * Fires a full table data changed event when a checkbox in the temperature sets
     * table is toggled. This forces the table to re-evaluate cell editability for the
     * river location column, which is only enabled when the row's checkbox is checked.
     */
    private void checkBoxSelected() {
        ((RmaTableModel) _temperatureSetsTable.getModel()).fireTableDataChanged();
    }

    /**
     * Cleans up any invalid DSS files accumulated since the last selection when the
     * file chooser ellipsis button is pressed (before the native file dialog opens).
     */
    private void ellipsesPressed() {
        deleteInvalidFiles(_invalidFilesToDelete);
    }

    /**
     * Returns a KeyListener that re-validates the OK button on every key release.
     * Used on the name text field and the file chooser text field.
     *
     * @return a KeyAdapter whose keyReleased implementation calls validateOkButton
     */
    private KeyListener getValidateKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validateOkButton();
            }
        };
    }

    /**
     * Returns a FocusListener that triggers DSS file processing when the file chooser
     * field loses focus to another component within the same dialog window. The
     * _ignoreFocusLost guard prevents recursive calls while an error dialog is open.
     *
     * @return a FocusAdapter whose focusLost implementation calls dssFileSelected
     */
    private FocusListener getImportFocusListener() {
        return new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Component focusGainedComponent = e.getOppositeComponent();

                // Only process the file when focus moves to another component within this dialog
                if (!_ignoreFocusLost && focusGainedComponent != null && SwingUtilities.getWindowAncestor(focusGainedComponent).isAncestorOf(e.getComponent())) {
                    dssFileSelected();
                }
            }
        };
    }

    /**
     * Reads the shared river locations CSV config file and populates both river
     * location combo boxes (the create-panel combo and the import-table combo).
     * Entries with empty names are removed before the model is set. Logs a WARNING
     * if the config file cannot be read.
     */
    private void fillRiverLocations() {
        Path riverLocationConfig = SharedConfigFiles.getRiverLocationsFile();

        try {
            // Read all RiverLocation objects from the shared config CSV
            List<RiverLocation> riverLocations = CsvReader.readCsv(riverLocationConfig, RiverLocation.class);

            // Remove any entries whose name is blank (e.g. header or empty rows)
            riverLocations.removeIf(rl -> rl.getName().isEmpty());

            // Populate the create-panel combo with a blank first item (selectedIndex -1 default)
            RmaListModel<RiverLocation> riverLocationComboModel = new RmaListModel<>(true, riverLocations);
            _riverLocationCombo.setModel(riverLocationComboModel);
            _riverLocationCombo.setSelectedIndex(-1);

            // Populate the import-table combo with the same locations
            RmaListModel<RiverLocation> riverLocationTableComboModel = new RmaListModel<>(true, riverLocations);
            _riverLocationTableCombo.setModel(riverLocationTableComboModel);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "Error reading river locations from " + riverLocationConfig);
        }
    }

    /**
     * Processes the file path currently entered in the file chooser field. Resolves
     * and relativises the path, validates that it exists and is a .dss file, reads
     * the condensed DSS catalogue, groups the entries into named collections by F-part
     * (and optionally B-part), and refreshes the temperature sets table. Shows an error
     * dialog and restores the import radio state if validation fails. Always closes the
     * DSS file in a finally block.
     */
    private void dssFileSelected() {
        String fileNameTxt = _importFileChooserField.getText();

        // Abort if the field is blank or contains only whitespace
        if (fileNameTxt == null || fileNameTxt.trim().isEmpty()) {
            return;
        }

        try {
            // Update the default browse path to the directory of the chosen file
            _importFileChooserField.setDefaultPath(Project.getCurrentProject().getAbsolutePath(fileNameTxt));

            // Remove any previously accumulated invalid files before processing the new selection
            deleteInvalidFiles(_invalidFilesToDelete);

            // Normalise the path to project-relative form for storage
            fileNameTxt = Project.getCurrentProject().getRelativePath(fileNameTxt);
            _importFileChooserField.setText(fileNameTxt);

            // Validate that the file exists and has a .dss extension
            String fileName = validateDssFileName(fileNameTxt);
            _invalidFilesToDelete.clear();

            // Read the condensed catalogue from the DSS file
            List<CondensedReference> catalog = new ArrayList<>(DssFileManagerImpl.getDssFileManager().getCondensedCatalog(Project.getCurrentProject().getAbsolutePath(fileName)));

            // Group catalogue entries by collection ID (derived from the F-part and B-part)
            Map<String, List<CondensedReference>> refMapping = catalog.stream()
                    .collect(Collectors.groupingBy(this::getCollectionId));

            // Convert each group's CondensedReferences into DSSPathname objects
            Map<String, List<DSSPathname>> collectionMapping = refMapping.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                            .map(cr -> new DSSPathname(cr.getNominalPathname())) // create new DssPathname objects from each CondensedReference
                            .collect(Collectors.toList())));

            // Replace the current collection mapping with the freshly built one
            _dssCollectionMapping.clear();
            _dssCollectionMapping.putAll(collectionMapping);

            // Repopulate the table rows with the new collection IDs
            updateAvailableTempSets();
            validateOkButton();

        } catch (NonexistentFileException | InvalidDssFileTypeException e) {
            // Suppress the focusLost handler while the error dialog is open to prevent re-entry
            _ignoreFocusLost = true;
            _invalidFilesToDelete.add(fileNameTxt);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid DSS File", JOptionPane.ERROR_MESSAGE);

            // Restore the import radio selection and re-show the import panel
            _importCreateButtonGroup.setSelected(_importFromExistingRadioButton.getModel(), true);
            importRadioAction();
            deleteInvalidFiles(_invalidFilesToDelete);
            _importFileChooserField.requestFocus();
            _ignoreFocusLost = false;

        } finally {
            // Always close the DSS file to release its file handle, even on error
            DssFileManagerImpl.getDssFileManager().close(Project.getCurrentProject().getAbsolutePath(fileNameTxt));
        }
    }

    /**
     * Deletes each file in the provided list from the file system using Files.deleteIfExists,
     * silently ignoring files that do not exist. Logs a CONFIG-level message on IOException.
     *
     * @param invalidFileToDeletes the list of file path strings to attempt to delete
     */
    private void deleteInvalidFiles(List<String> invalidFileToDeletes) {
        if (!_invalidFilesToDelete.isEmpty()) {
            try {
                for (String invalidFileToDelete : invalidFileToDeletes) {
                    // Use deleteIfExists to avoid errors when the file was never created
                    Files.deleteIfExists(Paths.get(invalidFileToDelete));
                }
            } catch (IOException e) {
                LOGGER.log(Level.CONFIG, e, () -> "Failed to delete invalid file: " + invalidFileToDeletes);
            }
        }
    }

    /**
     * Rebuilds the temperature sets table by removing all existing rows and adding
     * one unchecked row per collection ID in alphabetical order. Called whenever
     * a new DSS file is selected and the collection mapping has been updated.
     */
    private void updateAvailableTempSets() {
        // Iterate keys in sorted order for consistent alphabetical display
        SortedSet<String> keys = new TreeSet<>(_dssCollectionMapping.keySet());

        // Remove all existing rows in reverse index order to avoid shifting issues
        for (int row = _temperatureSetsTable.getRowCount() - 1; row >= 0; row--) {
            _temperatureSetsTable.deleteRow(row);
        }

        // Add a new unchecked row for each collection ID in alphabetical order
        for (String collectionId : keys) {
            ((RmaTableModel) _temperatureSetsTable.getModel()).addRow(new Vector<>(Arrays.asList(false, collectionId)));
        }
    }

    /**
     * Derives a human-readable collection ID from the nominal DSS pathname of the given
     * CondensedReference. The collection ID is taken from the F-part of the pathname; if
     * the F-part contains a pipe character, the segment after the pipe is used instead.
     * The B-part (location name) is prepended to the collection ID with a " - " separator
     * when it is non-blank, producing a display string in the form "B-Part - collectionId".
     *
     * @param ref the CondensedReference whose nominal pathname is parsed to derive the ID
     * @return    a collection ID string derived from the F-part, optionally prefixed with
     *            the B-part location name
     */
    private String getCollectionId(CondensedReference ref) {
        // Parse the nominal pathname into a DSSPathname to access individual path parts
        DSSPathname pathname = new DSSPathname(ref.getNominalPathname());

        // Retrieve the F-part of the pathname as the initial collection ID candidate
        String fPart = pathname.getFPart();
        String collectionId = fPart;

        // If the F-part contains a pipe, use the segment after it as the collection ID
        if (fPart.contains("|")) {
            // Split the F-part on the pipe character to isolate the collection ID segment
            String[] split = fPart.split("\\|");

            // Use the second segment if the split produced more than one part
            if (split.length > 1) {
                collectionId = split[1];
            }
        }

        // Default the return value to the derived collection ID before checking the B-part
        String retVal = collectionId;

        // Retrieve the B-part to use as a location name prefix
        String bPart = pathname.getBPart();

        // Prepend the B-part (location name) when it is non-blank
        if (bPart != null && !bPart.trim().isEmpty()) {
            // Combine the B-part and collection ID with a separator for display
            retVal = bPart + " - " + collectionId;
        }

        // Return the fully constructed collection ID string
        return retVal;
    }

    /**
     * Validates that the given DSS file name refers to an existing file with a ".dss"
     * extension. Relative paths are resolved to absolute paths using the current project
     * directory before validation. Throws NonexistentFileException if the file does not
     * exist on disk, or InvalidDssFileTypeException if the file extension is not ".dss".
     * Returns the original file name string unchanged if validation passes.
     *
     * @param fileName the DSS file name or path string to validate
     * @return         the original fileName string if the file exists and has a ".dss" extension
     * @throws NonexistentFileException    if no file exists at the resolved path
     * @throws InvalidDssFileTypeException if the file exists but does not have a ".dss" extension
     */
    private String validateDssFileName(String fileName) throws NonexistentFileException, InvalidDssFileTypeException {
        // Wrap the file name string in a Path object for file system operations
        Path filePath = Paths.get(fileName);

        // Resolve relative paths to absolute using the current project directory
        if (!filePath.isAbsolute()) {
            filePath = Paths.get(Project.getCurrentProject().getAbsolutePath(fileName));
        }

        if (!filePath.toFile().exists()) {
            // The file does not exist at the resolved path; throw an appropriate exception
            throw new NonexistentFileException(fileName);
        } else if (!(".dss".equalsIgnoreCase(getFileType(filePath)))) {
            // The file exists but does not have a ".dss" extension; throw an appropriate exception
            throw new InvalidDssFileTypeException(fileName);
        }

        // Validation passed; return the original file name unchanged
        return fileName;
    }

    /**
     * Extracts and returns the file extension from the given Path, including the leading
     * dot (e.g. ".dss"). If the file name contains no dot, or the dot is the first
     * character (indicating a hidden file with no extension), an empty string is returned.
     *
     * @param path the Path whose file extension will be extracted
     * @return     the file extension including the leading dot, or an empty string if
     *             no extension is present
     */
    private String getFileType(Path path) {
        // Extract the file name component from the path as a string
        String fileName = path.getFileName().toString();

        // Find the index of the last dot in the file name to locate the extension boundary
        int dotIndex = fileName.lastIndexOf('.');

        // Return the substring from the last dot onwards, or empty if no dot exists
        if (dotIndex > 0) {
            // A dot was found beyond the first character; extract the extension including the dot
            return fileName.substring(dotIndex);
        } else {
            // No dot found, or the dot is the leading character; return empty to indicate no extension
            return "";
        }
    }

    /**
     * Enables or disables the OK button based on the current dialog mode and input
     * validity. In create mode, the OK button requires a non-blank name. In import mode,
     * it requires the chosen file to exist and at least one table row to be checked.
     * River location validation is commented out pending config file readiness.
     * Also triggers cleanup of any accumulated invalid files.
     */
    private void validateOkButton() {
        //commented out validation of river location selected until river location config file is ready
        //Once config is ready, lines should be un-commented
        JButton okButton = _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON);

        // Create mode: invalid when the name field is blank
        boolean invalidCreate = _nameTextField.getText() == null || _nameTextField.getText().trim().isEmpty();
//                || _riverLocationCombo.getSelectedIndex() < 0; TODO: un-comment once river location config is ready

        // Import mode: invalid when the chosen file does not exist or no rows are checked
        String absPath = Project.getCurrentProject().getAbsolutePath(_importFileChooserField.getText());
        boolean invalidImport = !(Paths.get(absPath).toFile().exists()) || getNumCheckedRows() == 0;
//                || !allCheckedRowsHaveRiverLocationAssigned(); TODO: un-comment once river location config is ready

        // Enable or disable OK based on whichever mode is active
        if (_createNewRadioButton.isSelected()) {
            okButton.setEnabled(!invalidCreate);
        } else {
            okButton.setEnabled(!invalidImport);
        }

        // Clean up invalid files on every validation pass
        deleteInvalidFiles(_invalidFilesToDelete);
    }

    /**
     * Returns whether every checked row in the temperature sets table has a non-blank
     * river location assigned in column 2. Iterates over all rows and fails fast the
     * moment a checked row is found with a null or empty river location value. Unchecked
     * rows are not evaluated. Returns true if all checked rows have a river location
     * assigned, or if no rows are checked.
     *
     * @return true if all checked rows have a non-blank river location in column 2;
     *         false if any checked row has a null or empty river location
     */
    private boolean allCheckedRowsHaveRiverLocationAssigned() {
        // Default to true; set to false and exit early if any checked row fails the check
        boolean retVal = true;

        // Iterate over every row in the temperature sets table
        for (int row = 0; row < _temperatureSetsTable.getRowCount(); row++) {
            // Retrieve the checkbox value from column 0 of the current row
            Object checkVal = _temperatureSetsTable.getValueAt(row, 0);

            // Retrieve the river location value from column 2 of the current row
            Object riverLocVal = _temperatureSetsTable.getValueAt(row, 2);

            // Parse the checkbox value to a boolean, treating null as unchecked
            boolean rowChecked = checkVal != null && Boolean.parseBoolean(checkVal.toString());

            // A checked row with a null or empty river location fails the check
            if (rowChecked && (riverLocVal == null || riverLocVal.toString().isEmpty())) {
                // Mark the result as failed and stop searching further
                retVal = false;
                break;
            }
        }

        // Return true if all checked rows passed, false if any checked row was missing a river location
        return retVal;
    }

    /**
     * Counts and returns the number of checked rows in the temperature sets table.
     * The checkbox state is read from column 0 of each row; null values are treated
     * as unchecked. Returns zero if no rows are checked.
     *
     * @return the number of rows in the temperature sets table whose column 0 checkbox
     *         is checked; zero if no rows are checked
     */
    private int getNumCheckedRows() {
        // Initialize the counter to zero before scanning the table
        int checkRows = 0;

        // Iterate over every row in the temperature sets table to count checked rows
        for (int row = 0; row < _temperatureSetsTable.getRowCount(); row++) {
            // Retrieve the checkbox value from column 0 of the current row
            Object checkVal = _temperatureSetsTable.getValueAt(row, 0);

            // Increment the counter if the checkbox value is non-null and evaluates to true
            if (checkVal != null && Boolean.parseBoolean(checkVal.toString())) {
                checkRows++;
            }
        }

        // Return the total number of checked rows found in the table
        return checkRows;
    }

    /**
     * Handles the OK button click. Cleans up invalid files, checks for duplicate names
     * (prompting for overwrite confirmation when needed), builds the list of
     * TemperatureTargetSet objects, forwards them to the consumer callback, and closes
     * the dialog if all expected sets were accepted. Shows an error dialog on save failure.
     * Always restores the default cursor in the finally block.
     */
    private void okAction() {
        // Clean up any invalid files before processing the OK action
        deleteInvalidFiles(_invalidFilesToDelete);

        // Determine which set names are safe to import after duplicate checks
        List<String> setNamesToImport = validateDuplicateNames();

        if (!setNamesToImport.isEmpty() && riverLocationSelected()) {
            try {
                // Build the TemperatureTargetSet objects for the validated names
                List<TemperatureTargetSet> sets = buildTempTargetSets(setNamesToImport);

                // Forward the sets to the caller via the consumer callback
                _consumeTempTargetSetAction.accept(sets);

                // Close the dialog only when every expected set was successfully accepted
                if (setNamesToImport.size() == getExpectedNumberToImport()) {
                    dispose();
                }
            } catch (TempTargetSaveFailedException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, e, () -> "Temp Target save failed: " + e.getMessage());
            } finally {
                // Always restore the cursor even if the save or build threw an exception
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     * Validates that a river location has been selected when in create mode. Shows an
     * informational dialog and returns false when none is selected, preventing the OK
     * action from proceeding without a location.
     *
     * @return true if the dialog is in import mode or if a river location is selected;
     * false when in create mode with no location chosen
     */
    private boolean riverLocationSelected() {
        if (_createNewRadioButton.isSelected()) {
            // Prompt the user to select a location when none has been chosen in create mode
            if (_riverLocationCombo.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(this, "Please Select a River Location to Create the Temperature Target Set for",
                        "No River Location", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Collects the names of temperature target sets to import, checking each against
     * the existing set names and prompting for overwrite confirmation on duplicates.
     * Delegates to validateFromExistingNames in import mode; handles the single
     * create-mode name directly.
     *
     * @return a List of set name strings approved for import; empty if cancelled
     */
    private List<String> validateDuplicateNames() {
        List<String> setNamesToImport = new ArrayList<>();

        if (_importFromExistingRadioButton.isSelected()) {
            // Validate all checked rows in the import table against existing set names
            setNamesToImport = validateFromExistingNames();

        } else {
            String name = _nameTextField.getText();

            if (name != null && !name.trim().isEmpty() && _existingSetNames.contains(name.trim())) {
                // Prompt for overwrite confirmation when the name already exists
                if (handleDuplicateName(name)) {
                    setNamesToImport.add(name);
                }
            } else if (name != null && !name.trim().isEmpty()) {
                // Name is unique; add it directly
                setNamesToImport.add(name);
            }
        }
        return setNamesToImport;
    }

    /**
     * Returns the number of temperature target sets the user is expected to import in
     * the current operation. In import mode this equals the number of checked rows;
     * in create mode it is always 1.
     *
     * @return the expected count of sets to be imported or created
     */
    private int getExpectedNumberToImport() {
        int expected = 0;

        if (_importFromExistingRadioButton.isSelected()) {
            // Count the number of checked rows in the import table
            for (int row = 0; row < _temperatureSetsTable.getRowCount(); row++) {
                Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);
                if (checkedVal != null && Boolean.parseBoolean(checkedVal.toString())) {
                    expected++;
                }
            }
        } else {
            // Creating a single user-defined set always counts as one
            expected = 1;
        }
        return expected;
    }

    /**
     * Validates the names of all checked rows in the temperature sets table against the
     * existing set names and returns the list of names approved for import. For each
     * checked row, if the name matches an existing set the user is prompted to confirm
     * an overwrite; the name is only included if the user confirms. If the user declines
     * an overwrite, processing stops immediately and no further rows are evaluated.
     * Rows with unique names are added to the import list without prompting.
     *
     * @return a list of temperature set names approved for import; may be empty if no
     *         rows are checked, all names are duplicates the user declined to overwrite,
     *         or the user cancelled an overwrite prompt
     */
    private List<String> validateFromExistingNames() {
        // Initialize the list that will accumulate names approved for import
        List<String> setNamesToImport = new ArrayList<>();

        // Iterate over every row in the temperature sets table to evaluate checked rows
        for (int row = 0; row < _temperatureSetsTable.getRowCount(); row++) {
            // Retrieve the checkbox value from column 0 of the current row
            Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);

            // Only process rows whose checkbox is non-null and evaluates to true
            if (checkedVal != null && Boolean.parseBoolean(checkedVal.toString())) {
                // Retrieve the temperature set name from column 1 of the current row
                Object name = _temperatureSetsTable.getValueAt(row, 1);

                // Check whether the name is non-blank and conflicts with an existing set name
                if (name != null && !name.toString().trim().isEmpty() && _existingSetNames.contains(name.toString().trim())) {
                    // Prompt the user to confirm overwriting the existing set
                    boolean overwrite = handleDuplicateName(name.toString());

                    // Only add the name to the import list if the user confirmed the overwrite
                    if (overwrite) {
                        setNamesToImport.add(name.toString());
                    }

                    // Stop processing further rows if the user declined an overwrite
                    if (!overwrite) {
                        break;
                    }
                } else if (name != null) {
                    // Name is unique; add it directly without prompting
                    setNamesToImport.add(name.toString());
                }
            }
        }

        // Return the list of names approved for import
        return setNamesToImport;
    }

    /**
     * Handles a duplicate temperature target set name by prompting the user to confirm
     * an overwrite. If no existing set with the given name is found, true is returned
     * immediately. When a duplicate is found, any EnsembleSets that depend on it are
     * listed in the confirmation dialog so the user is fully informed of the cascade
     * deletion. If the user confirms the overwrite, the existing set is removed from
     * the simulation group and the project data is saved before returning true. If the
     * user declines, false is returned and no changes are made.
     *
     * @param name the temperature target set name to check for duplicates
     * @return     true if no duplicate exists or the user confirmed the overwrite;
     *             false if a duplicate exists and the user declined to overwrite it
     */
    private boolean handleDuplicateName(String name) {
        // Look up whether a temperature target set with this name already exists in the group
        TemperatureTargetSet existingTTSet = _fsg.getTemperatureTargetSet(name);

        // Default to true; set to false when a duplicate is found pending user confirmation
        boolean retVal = true;

        if (existingTTSet != null) {
            // A duplicate was found; default to false until the user confirms the overwrite
            retVal = false;

            // Find all EnsembleSets that reference this temperature target set
            List<EnsembleSet> ensembleSetsUsingTTSet = _fsg.getEnsembleSetsUsingTempTargetSet(existingTTSet);

            // Start building the confirmation message with the duplicate name notice
            StringBuilder confirmMessage = new StringBuilder(name + " already exists. ");

            if (!ensembleSetsUsingTTSet.isEmpty()) {
                // List the names of dependent EnsembleSets so the user is fully informed
                List<String> eSetNames = ensembleSetsUsingTTSet.stream()
                        .map(NamedType::getName)
                        .collect(Collectors.toList());

                // Append the cascade-deletion warning listing all dependent ensemble set names
                confirmMessage.append("\nOverwriting ")
                        .append(name)
                        .append(" will also delete the following ensemble sets which use it:")
                        .append("\n\n")
                        .append(String.join(",\n", eSetNames))
                        .append("\n\nDo you want to continue?");
            } else {
                // No dependents: simpler overwrite confirmation message
                confirmMessage.append("\nDo you want to overwrite existing temperature target set ")
                        .append(name)
                        .append("?");
            }

            // Display the confirmation dialog and capture the user's choice
            int opt = JOptionPane.showConfirmDialog(this, confirmMessage, "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (opt == JOptionPane.YES_OPTION) {
                // User confirmed the overwrite; set the return value to true
                retVal = true;

                // Remove the existing set from the simulation group before the new one is added
                _fsg.removeTemperatureTargetSet(existingTTSet);

                // Save the project to persist the removal before the new set is written
                _fsg.saveData();
            }
        }

        // Return true if the overwrite was confirmed or no duplicate existed, false otherwise
        return retVal;
    }

    /**
     * Constructs the list of TemperatureTargetSet objects to be imported or created,
     * based on the current dialog mode. In import mode, builds a set for each checked
     * table row whose name is in setNamesToImport. In create mode, builds a single
     * user-defined set from the create panel fields. Shows a wait cursor during
     * the potentially slow DSS-backed construction.
     *
     * @param setNamesToImport the approved set names to build; used to filter import rows
     * @return a List of fully configured TemperatureTargetSet objects ready for use
     */
    private List<TemperatureTargetSet> buildTempTargetSets(List<String> setNamesToImport) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<TemperatureTargetSet> retVal = new ArrayList<>();

        if (_importFromExistingRadioButton.isSelected()) {
            // Build one TemperatureTargetSet per approved checked row in the import table
            for (int row = 0; row < _temperatureSetsTable.getRowCount(); row++) {
                TemperatureTargetSet temperatureTargetSet = new TemperatureTargetSet();
                Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);

                if (checkedVal != null && Boolean.parseBoolean(checkedVal.toString())) {
                    Object name = _temperatureSetsTable.getValueAt(row, 1);
                    Object riverLocationObj = _temperatureSetsTable.getValueAt(row, 2);

                    // Only build the set when name and river location are valid and the name is approved
                    if (name != null && !name.toString().trim().isEmpty() && setNamesToImport.contains(name.toString())
                            && riverLocationObj instanceof RiverLocation) {
                        // Retrieve the DSS pathnames for this collection from the mapping
                        List<DSSPathname> pathnames = getSelectedTempTargetSetPathNames(name.toString());
                        temperatureTargetSet.setDssPathNames(pathnames);
                        temperatureTargetSet.setName(name.toString());
                        temperatureTargetSet.setDescription(_descriptionFieldImport.getText());
                        temperatureTargetSet.setUserDefined(false);
                        temperatureTargetSet.setDssSourcePath(Paths.get(_importFileChooserField.getText()));
                        temperatureTargetSet.setRiverLocation((RiverLocation) riverLocationObj);
                        temperatureTargetSet.setModified(true);
                        retVal.add(temperatureTargetSet);
                    }
                }
            }
        } else {
            // Build a single user-defined set from the create panel fields
            TemperatureTargetSet temperatureTargetSet = new TemperatureTargetSet();
            temperatureTargetSet.setName(_nameTextField.getText());
            temperatureTargetSet.setDescription(_descriptionField.getText());
            temperatureTargetSet.setUserDefined(true);
            temperatureTargetSet.setNumberOfUserDefinedTempTargets(_numberTempTargetsField.getValue());
            temperatureTargetSet.setRiverLocation((RiverLocation) _riverLocationCombo.getSelectedItem());
            temperatureTargetSet.setUnits((String) _unitsComboBox.getSelectedItem());
            temperatureTargetSet.setModified(true);
            retVal.add(temperatureTargetSet);
        }

        return retVal;
    }

    /**
     * Cleans up any accumulated invalid files and then delegates to the superclass
     * dispose to release all dialog resources.
     */
    @Override
    public void dispose() {
        // Remove any leftover invalid DSS files before the dialog is fully torn down
        deleteInvalidFiles(_invalidFilesToDelete);
        super.dispose();
    }

    /**
     * Returns the list of DSSPathname objects for the given collection name by looking
     * it up in the current DSS collection mapping. Returns an empty list when the name
     * is null, empty, or not found in the mapping.
     *
     * @param collectionName the collection ID key to look up in the DSS mapping
     * @return the list of DSSPathname objects for that collection, or an empty list
     */
    private List<DSSPathname> getSelectedTempTargetSetPathNames(String collectionName) {
        List<DSSPathname> retVal = new ArrayList<>();

        if (collectionName != null && !collectionName.isEmpty()) {
            retVal = _dssCollectionMapping.get(collectionName);
        }

        return retVal;
    }

    /**
     * Handles the Cancel button and window-close (X) actions. Cleans up invalid files,
     * then prompts the user to confirm cancellation when the dialog has been modified.
     * On confirmation (or when unmodified), clears the file chooser field and disposes
     * the dialog.
     */
    private void closeDialogAction() {
        // Clean up invalid files before potentially closing
        deleteInvalidFiles(_invalidFilesToDelete);

        int opt = JOptionPane.YES_OPTION;

        // Ask for confirmation only when the user has made changes
        if (isModified()) {
            opt = JOptionPane.showConfirmDialog(this, "Cancel Temperature Target Set Import?", "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION);
        }

        if (opt == JOptionPane.YES_OPTION) {
            // Clear the file chooser field to avoid stale path state if the dialog is re-opened
            _importFileChooserField.setText("");
            dispose();
        }
    }

    /**
     * Switches the card panel to the "Create New Set" view and re-validates the OK button.
     * Called when the "Create New Set" radio button is selected.
     */
    private void createNewRadioAction() {
        CardLayout cardLayout = (CardLayout) _cardPanel.getLayout();
        cardLayout.show(_cardPanel, CREATE_PANEL_ID);
        validateOkButton();
    }

    /**
     * Switches the card panel to the "Import Set From Existing" view and re-validates
     * the OK button. Called when the "Import Set From Existing" radio button is selected.
     */
    private void importRadioAction() {
        CardLayout cardLayout = (CardLayout) _cardPanel.getLayout();
        cardLayout.show(_cardPanel, IMPORT_PANEL_ID);
        validateOkButton();
    }

    /**
     * Builds all top-level dialog controls: the Import / Create New radio button panel,
     * the CardLayout panel containing the import and create sub-panels, and the
     * OK/Cancel button row. The OK button is initially disabled until valid input exists.
     */
    private void buildControls() {
        // Create the mutually exclusive radio buttons and add them to a button group
        _importCreateButtonGroup = new ButtonGroup();
        _importFromExistingRadioButton = new RmaJRadioButton("Import Set From Existing");
        _createNewRadioButton = new RmaJRadioButton("Create New Set");
        _importCreateButtonGroup.add(_importFromExistingRadioButton);
        _importCreateButtonGroup.add(_createNewRadioButton);

        // Default to the import radio selected
        _importCreateButtonGroup.setSelected(_importFromExistingRadioButton.getModel(), true);

        // Place the two radio buttons in a panel with Import on the left and Create on the right
        RmaJPanel importCreateRadioButtonPanel = new RmaJPanel(new BorderLayout());
        importCreateRadioButtonPanel.add(_importFromExistingRadioButton, BorderLayout.WEST);
        importCreateRadioButtonPanel.add(_createNewRadioButton, BorderLayout.EAST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        getContentPane().add(importCreateRadioButtonPanel, gbc);

        // Initialise the river location combo before the import and create panels are built
        _cardPanel = new RmaJPanel(new CardLayout());
        _riverLocationCombo = new RmaJComboBox<>();

        // Build both card panels and register them with their respective CardLayout keys
        RmaJPanel importPanel = buildImportPanel();
        RmaJPanel createPanel = buildCreatePanel();
        _cardPanel.add(importPanel, IMPORT_PANEL_ID);
        _cardPanel.add(createPanel, CREATE_PANEL_ID);

        // Add the card panel, which expands to fill the main dialog area
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = RmaInsets.INSETS5505;
        getContentPane().add(_cardPanel, gbc);

        // Create the OK/Cancel panel and initially disable OK until input is valid
        _okCancelPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
        _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.001;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5555;
        getContentPane().add(_okCancelPanel, gbc);
    }

    /**
     * Builds and returns the "Create New Set" card panel. Contains: a name text field,
     * a description field, a river location combo box, a bounded integer field for the
     * number of temperature targets, and a units combo box pre-populated with English (°F)
     * and SI (°C) temperature unit strings.
     *
     * @return the fully constructed create-panel RmaJPanel
     */
    private RmaJPanel buildCreatePanel() {
        RmaJPanel createPanel = new RmaJPanel(new GridBagLayout());
        RmaJPanel nameDescPanel = new RmaJPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // "Name:" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("Name:"), gbc);

        // Name text field for the new set's identifier
        _nameTextField = new RmaJTextField();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(_nameTextField, gbc);

        // "Description:" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("Description:"), gbc);

        // Multi-line description field for the new set
        _descriptionField = new RmaJDescriptionField();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.001;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(_descriptionField, gbc);

        // "River Location:" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("River Location:"), gbc);

        // River location combo box; populated by fillRiverLocations after construction
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.001;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        nameDescPanel.add(_riverLocationCombo, gbc);

        // Add the name/description/location sub-panel to the create panel
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS0000;
        createPanel.add(nameDescPanel, gbc);

        // "Number of Temperature Targets (Max N):" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Number of Temperature Targets (Max " + MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET + "):"), gbc);

        // Integer field for the number of targets; bounded to [1, MAX] and defaulting to 1
        _numberTempTargetsField = new RmaJIntegerField();
        _numberTempTargetsField.setValue(1);
        _numberTempTargetsField.setMaxValue(MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET);
        _numberTempTargetsField.setMinValue(1);
        _numberTempTargetsField.setEmptyOk(false);
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        createPanel.add(_numberTempTargetsField, gbc);

        // "Units:" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Units:"), gbc);

        // Units combo box populated with English (°F) as index 0 and SI (°C) as index 1
        _unitsComboBox = new RmaJComboBox<>();
        String ft = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.ENGLISH_ID);
        String celsius = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.SI_ID);
        RmaListModel<String> unitsModel = new RmaListModel<>(false, ft, celsius);
        _unitsComboBox.setModel(unitsModel);
        _unitsComboBox.setSelectedIndex(0);
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.001;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        createPanel.add(_unitsComboBox, gbc);

        return createPanel;
    }

    /**
     * Builds and returns the "Import Set From Existing" card panel. Contains: a file
     * chooser field pre-filtered to .dss files, a description field, and a three-column
     * table (checkbox, set name, river location) that is populated when a DSS file is
     * selected. The checkbox column is fixed at 30px; the river location column uses a
     * combo box editor enabled only when the row's checkbox is checked.
     *
     * @return the fully constructed import-panel RmaJPanel
     */
    private RmaJPanel buildImportPanel() {
        RmaJPanel importPanel = new RmaJPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // "Select file:" label
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        importPanel.add(new JLabel("Select file:"), gbc);

        // File chooser field restricted to .dss files, defaulting to the project workspace path
        _importFileChooserField = new RmaFileChooserField();
        _importFileChooserField.setDefaultPath(Project.getCurrentProject().getWorkspacePath());
        _importFileChooserField.setSelectedFilter(new RMAFilenameFilter("dss", ".dss"));
        _importFileChooserField.setAcceptAllFileFilterUsed(false);
        gbc.gridx = 1;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        importPanel.add(_importFileChooserField, gbc);

        // "Description:" label for the import description field
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = RmaInsets.INSETS5505;
        importPanel.add(new JLabel("Description:"), gbc);

        // Description field shared across all imported sets in this operation
        _descriptionFieldImport = new RmaJDescriptionField();
        gbc.gridx = 1;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = RmaInsets.INSETS5505;
        importPanel.add(_descriptionFieldImport, gbc);

        // Three-column table: checkbox (col 0), set name (col 1, read-only), river location (col 2)
        _temperatureSetsTable = new RmaJTable(this, new String[]{"", "Temperature Target Set", "River Location"}) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                // Constrain the preferred viewport height to exactly four rows
                Dimension d = super.getPreferredScrollableViewportSize();
                d.height = getRowHeight() * 4;
                return d;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                boolean retVal = false;

                if (column == 0) {
                    // The checkbox column is always editable
                    retVal = true;
                } else if (column == 2) {
                    // The river location column is only editable when its row's checkbox is checked
                    Object checkVal = _temperatureSetsTable.getValueAt(row, 0);
                    retVal = checkVal != null && Boolean.parseBoolean(checkVal.toString());
                }
                return retVal;
            }
        };

        // Install a checkbox editor in column 0 and keep a reference for state-change detection
        _checkBoxEditorCheckBox = _temperatureSetsTable.setCheckBoxCellEditor(0);

        // Install a combo box editor in the river location column (col 2) with an empty initial model
        _riverLocationTableCombo = _temperatureSetsTable.setComboBoxEditor(2, new Object[]{}, true);

        // Disable direct editing of the set name column (col 1); it is populated programmatically
        _temperatureSetsTable.setColumnEnabled(false, 1);
        _temperatureSetsTable.setPopupMenuEnabled(true);

        // Remove default row add/edit popup menu items; this table is read-only aside from checkboxes
        _temperatureSetsTable.removePopupMenuRowEditingOptions();

        // Increase row height for readability
        _temperatureSetsTable.setRowHeight(_temperatureSetsTable.getRowHeight() + 5);

        // Fix the checkbox column to a narrow width so it does not consume unnecessary space
        TableColumnModel columnModel = _temperatureSetsTable.getColumnModel();
        TableColumn column = columnModel.getColumn(0);
        column.setPreferredWidth(30);
        column.setMaxWidth(30);

        // Remove any auto-added default row so the table starts empty
        if (_temperatureSetsTable.getRowCount() > 0) {
            _temperatureSetsTable.deleteRow(0);
        }

        // Add the table's scroll pane, expanding to fill all remaining space in the import panel
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.001;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = RmaInsets.INSETS5505;
        importPanel.add(_temperatureSetsTable.getScrollPane(), gbc);

        return importPanel;
    }
}
