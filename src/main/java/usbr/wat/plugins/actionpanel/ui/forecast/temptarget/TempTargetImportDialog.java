/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import com.rma.swing.RmaFileChooserField;
import hec.data.Parameter;
import hec.data.Units;
import hec.heclib.dss.CondensedReference;
import hec.heclib.dss.DSSPathname;
import hec.lang.NamedType;
import rma.swing.ButtonCmdPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJIntegerField;
import rma.swing.RmaJPanel;
import rma.swing.RmaJRadioButton;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import rma.swing.table.RmaTableModel;
import rma.util.RMAFilenameFilter;
import usbr.wat.plugins.actionpanel.model.SharedConfigFiles;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.RiverLocation;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;
import usbr.wat.plugins.actionpanel.ui.forecast.CsvReader;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class TempTargetImportDialog extends RmaJDialog
{
    private static final Logger LOGGER = Logger.getLogger(TempTargetImportDialog.class.getName());
    private static final String IMPORT_PANEL_ID = "IMPORT_PANEL";
    private static final String CREATE_PANEL_ID = "CREATE_PANEL";
    private static final int MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET = 12;
    private final TempTargetConsumer _consumeTempTargetSetAction;
    private final List<String> _existingSetNames;
    private final ForecastSimGroup _fsg;
    private RmaJRadioButton _importFromExistingRadioButton;
    private RmaJRadioButton _createNewRadioButton;
    private ButtonCmdPanel _okCancelPanel;
    private RmaJPanel _cardPanel;
    private RmaFileChooserField _importFileChooserField;
    private RmaJTextField _nameTextField;
    private RmaJDescriptionField _descriptionField;
    private final Map<String, List<DSSPathname>> _dssCollectionMapping = new TreeMap<>();
    private boolean _ignoreFocusLost;
    private final List<String> _invalidFilesToDelete = new ArrayList<>();
    private ButtonGroup _importCreateButtonGroup;
    private RmaJTable _temperatureSetsTable;
    private JCheckBox _checkBoxEditorCheckBox;
    private RmaJDescriptionField _descriptionFieldImport;
    private RmaJIntegerField _numberTempTargetsField;
    private RmaJComboBox<RiverLocation> _riverLocationCombo;
    private JComboBox<RiverLocation> _riverLocationTableCombo;
    private RmaJComboBox<String> _unitsComboBox;

    public TempTargetImportDialog(Window parent, List<String> existingSetNames, ForecastSimGroup fsg, TempTargetConsumer consumeTempTargetSetAction)
    {
        super(parent, true);
        setTitle("Select Temperature Target Set");
        getContentPane().setLayout(new GridBagLayout());
        _existingSetNames = existingSetNames;
        _fsg = fsg;
        _consumeTempTargetSetAction = consumeTempTargetSetAction;
        buildControls();
        fillRiverLocations();
        addListeners();
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(getSize());
        setSize(new Dimension(400, 350));
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void addListeners()
    {
        _nameTextField.addKeyListener(getValidateKeyListener());
        _importFileChooserField.addKeyListener(getValidateKeyListener());
        _numberTempTargetsField.addKeyListener(getNumberTempTargetsKeyListener());
        _importFileChooserField.addFocusListener(getImportFocusListener());
        _importFileChooserField.addFileSelectedListener(f -> dssFileSelected());
        ((JButton)_importFileChooserField.getComponents()[0]).addActionListener(e -> ellipsesPressed());
        _importFromExistingRadioButton.addActionListener(e -> importRadioAction());
        _createNewRadioButton.addActionListener(e -> createNewRadioAction());
        _okCancelPanel.getButton(ButtonCmdPanel.CANCEL_BUTTON).addActionListener(e -> closeDialogAction());
        _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON).addActionListener(e -> okAction());
        addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                closeDialogAction();
            }
        });
        _temperatureSetsTable.getModel().addTableModelListener(e -> validateOkButton());
        _riverLocationCombo.addActionListener(e -> validateOkButton());
        _temperatureSetsTable.addTableModelListener(e -> validateOkButton());
        _checkBoxEditorCheckBox.addActionListener(e -> checkBoxSelected());
    }

    private void checkBoxSelected()
    {
        ((RmaTableModel)_temperatureSetsTable.getModel()).fireTableDataChanged();
    }

    private KeyListener getNumberTempTargetsKeyListener()
    {
        return new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                int value = _numberTempTargetsField.getValue();
                if(value < 1)
                {
                    _numberTempTargetsField.setValue(1);
                }
                else if(value > MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET)
                {
                    _numberTempTargetsField.setValue(MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET);
                }
            }
        };
    }

    private void ellipsesPressed()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
    }

    private KeyListener getValidateKeyListener()
    {
        return new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                validateOkButton();
            }
        };
    }

    private FocusListener getImportFocusListener()
    {
        return new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                Component focusGainedComponent = e.getOppositeComponent();
                if(!_ignoreFocusLost && focusGainedComponent != null && SwingUtilities.getWindowAncestor(focusGainedComponent).isAncestorOf(e.getComponent()))
                {
                    dssFileSelected();
                }
            }
        };
    }

    private void fillRiverLocations()
    {
        Path riverLocationConfig = SharedConfigFiles.getRiverLocationsFile();
        try
        {
            List<RiverLocation> riverLocations = CsvReader.readCsv(riverLocationConfig, RiverLocation.class);
            riverLocations.removeIf(rl -> rl.getName().isEmpty());
            RmaListModel<RiverLocation> riverLocationComboModel = new RmaListModel<>(true, riverLocations);
            _riverLocationCombo.setModel(riverLocationComboModel);
            _riverLocationCombo.setSelectedIndex(-1);
            RmaListModel<RiverLocation> riverLocationTableComboModel = new RmaListModel<>(true, riverLocations);
            _riverLocationTableCombo.setModel(riverLocationTableComboModel);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, e, () -> "Error reading river locations from " + riverLocationConfig);
        }
    }

    private void dssFileSelected()
    {
        String fileNameTxt = _importFileChooserField.getText();
        if(fileNameTxt == null || fileNameTxt.trim().isEmpty())
        {
            return;
        }
        try
        {
            _importFileChooserField.setDefaultPath(Project.getCurrentProject().getAbsolutePath(fileNameTxt));
            deleteInvalidFiles(_invalidFilesToDelete);
            fileNameTxt = Project.getCurrentProject().getRelativePath(fileNameTxt);
            _importFileChooserField.setText(fileNameTxt);
            String fileName = validateDssFileName(fileNameTxt);
            _invalidFilesToDelete.clear();
            List<CondensedReference> catalog = new ArrayList<>(DssFileManagerImpl.getDssFileManager().getCondensedCatalog(Project.getCurrentProject().getAbsolutePath(fileName)));
            Map<String, List<CondensedReference>> refMapping = catalog.stream()
                    .collect(Collectors.groupingBy(this::getCollectionId));
            Map<String, List<DSSPathname>> collectionMapping = refMapping.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                            .map(cr -> new DSSPathname(cr.getNominalPathname())) // create new DssPathname objects from each CondensedReference
                            .collect(Collectors.toList())));
            _dssCollectionMapping.clear();
            _dssCollectionMapping.putAll(collectionMapping);
            updateAvailableTempSets();
            validateOkButton();
        }
        catch (NonexistentFileException | InvalidDssFileTypeException e)
        {
            _ignoreFocusLost = true;
            _invalidFilesToDelete.add(fileNameTxt);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid DSS File", JOptionPane.ERROR_MESSAGE);
            _importCreateButtonGroup.setSelected(_importFromExistingRadioButton.getModel(), true);
            importRadioAction();
            deleteInvalidFiles(_invalidFilesToDelete);
            _importFileChooserField.requestFocus();
            _ignoreFocusLost = false;
        }
        finally
        {
            DssFileManagerImpl.getDssFileManager().close(Project.getCurrentProject().getAbsolutePath(fileNameTxt));
        }
    }

    private void deleteInvalidFiles(List<String> invalidFileToDeletes)
    {
        if(!_invalidFilesToDelete.isEmpty())
        {
            try
            {
                for(String invalidFileToDelete : invalidFileToDeletes)
                {
                    Files.deleteIfExists(Paths.get(invalidFileToDelete));
                }
            }
            catch (IOException e)
            {
                LOGGER.log(Level.CONFIG, e, () -> "Failed to delete invalid file: " + invalidFileToDeletes);
            }
        }
    }

    private void updateAvailableTempSets()
    {
        SortedSet<String> keys = new TreeSet<>(_dssCollectionMapping.keySet());
        for(int row = _temperatureSetsTable.getRowCount()-1; row >=0; row--)
        {
            _temperatureSetsTable.deleteRow(row);
        }
        for(String collectionId : keys)
        {
            ((RmaTableModel)_temperatureSetsTable.getModel()).addRow(new Vector<>(Arrays.asList(false, collectionId)));
        }
    }

    private String getCollectionId(CondensedReference ref)
    {
        DSSPathname pathname = new DSSPathname(ref.getNominalPathname());
        String fPart = pathname.getFPart();
        String collectionId = fPart;
        if(fPart.contains("|"))
        {
            String[] split = fPart.split("\\|");
            if(split.length > 1)
            {
                collectionId = split[1];
            }
        }
        String retVal = collectionId;
        String bPart = pathname.getBPart();
        if(bPart != null && !bPart.trim().isEmpty())
        {
            retVal = bPart + " - " + collectionId;
        }
        return retVal;
    }

    private String validateDssFileName(String fileName) throws NonexistentFileException, InvalidDssFileTypeException
    {
        Path filePath = Paths.get(fileName);
        if(!filePath.isAbsolute())
        {
            filePath = Paths.get(Project.getCurrentProject().getAbsolutePath(fileName));
        }
        if(!filePath.toFile().exists())
        {
            throw new NonexistentFileException(fileName);
        }
        else if(!(".dss".equalsIgnoreCase(getFileType(filePath))))
        {
            throw new InvalidDssFileTypeException(fileName);
        }
        return fileName;
    }

    private String getFileType(Path path)
    {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0)
        {
            return fileName.substring(dotIndex);
        }
        else
        {
            return "";
        }
    }

    private void validateOkButton()
    {
        //commented out validation of river location selected until river location config file is ready
        //Once config is ready, lines should be un-commented
        JButton okButton = _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON);
        boolean invalidCreate = _nameTextField.getText() == null || _nameTextField.getText().trim().isEmpty();
//                || _riverLocationCombo.getSelectedIndex() < 0; TODO: un-comment once river location config is ready
        String absPath = Project.getCurrentProject().getAbsolutePath(_importFileChooserField.getText());
        boolean invalidImport = !(Paths.get(absPath).toFile().exists()) || getNumCheckedRows() == 0;
//                || !allCheckedRowsHaveRiverLocationAssigned(); TODO: un-comment once river location config is ready
        if(_createNewRadioButton.isSelected())
        {
            okButton.setEnabled(!invalidCreate);
        }
        else
        {
            okButton.setEnabled(!invalidImport);
        }
        deleteInvalidFiles(_invalidFilesToDelete);
    }

    private boolean allCheckedRowsHaveRiverLocationAssigned()
    {
        boolean retVal = true;
        for(int row =0; row < _temperatureSetsTable.getRowCount(); row++)
        {
            Object checkVal = _temperatureSetsTable.getValueAt(row, 0);
            Object riverLocVal = _temperatureSetsTable.getValueAt(row, 2);
            boolean rowChecked = checkVal != null && Boolean.parseBoolean(checkVal.toString());
            if(rowChecked && (riverLocVal == null || riverLocVal.toString().isEmpty()))
            {
                retVal = false;
                break;
            }
        }
        return retVal;
    }

    private int getNumCheckedRows()
    {
        int checkRows = 0;
        for(int row =0; row < _temperatureSetsTable.getRowCount(); row++)
        {
            Object checkVal = _temperatureSetsTable.getValueAt(row, 0);
            if(checkVal != null && Boolean.parseBoolean(checkVal.toString()))
            {
                checkRows++;
            }
        }
        return checkRows;
    }

    private void okAction()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        List<String> setNamesToImport = validateDuplicateNames();
        if(!setNamesToImport.isEmpty())
        {
            try
            {
                List<TemperatureTargetSet> sets = buildTempTargetSets(setNamesToImport);
                _consumeTempTargetSetAction.accept(sets);
                if(setNamesToImport.size() == getExpectedNumberToImport())
                {
                    dispose();
                }
            }
            catch(TempTargetSaveFailedException e)
            {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, e, () -> "Temp Target save failed: " + e.getMessage());
            }
            finally
            {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private List<String> validateDuplicateNames()
    {
        List<String> setNamesToImport = new ArrayList<>();
        if(_importFromExistingRadioButton.isSelected())
        {
            setNamesToImport = validateFromExistingNames();
        }
        else
        {
            String name = _nameTextField.getText();
            if(name != null && !name.trim().isEmpty() && _existingSetNames.contains(name.trim()))
            {
                if(handleDuplicateName(name))
                {
                    setNamesToImport.add(name);
                }
            }
            else if(name != null && !name.trim().isEmpty())
            {
                setNamesToImport.add(name);
            }
        }
        return setNamesToImport;
    }

    private int getExpectedNumberToImport()
    {
        int expected = 0;
        if(_importFromExistingRadioButton.isSelected())
        {
            for(int row =0; row < _temperatureSetsTable.getRowCount(); row++)
            {
                Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);
                if (checkedVal != null && Boolean.parseBoolean(checkedVal.toString()))
                {
                    expected++;
                }
            }
        }
        else //importing user defined set
        {
            expected = 1;
        }
        return expected;
    }

    private List<String> validateFromExistingNames()
    {
        List<String> setNamesToImport = new ArrayList<>();
        for(int row =0; row < _temperatureSetsTable.getRowCount(); row++)
        {
            Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);
            if(checkedVal != null && Boolean.parseBoolean(checkedVal.toString()))
            {
                Object name = _temperatureSetsTable.getValueAt(row, 1);
                if(name != null && !name.toString().trim().isEmpty() && _existingSetNames.contains(name.toString().trim()))
                {
                    boolean overwrite = handleDuplicateName(name.toString());
                    if(overwrite)
                    {
                        setNamesToImport.add(name.toString());
                    }
                    if(!overwrite)
                    {
                        break;
                    }
                }
                else if(name != null)
                {
                    setNamesToImport.add(name.toString());
                }
            }
        }
        return setNamesToImport;
    }

    private boolean handleDuplicateName(String name)
    {
        TemperatureTargetSet existingTTSet = _fsg.getTemperatureTargetSet(name);
        boolean retVal = true;
        if(existingTTSet != null)
        {
            retVal = false;
            List<EnsembleSet> ensembleSetsUsingTTSet = _fsg.getEnsembleSetsUsingTempTargetSet(existingTTSet);
            StringBuilder confirmMessage = new StringBuilder(name + " already exists. ");
            if (!ensembleSetsUsingTTSet.isEmpty())
            {
                List<String> eSetNames = ensembleSetsUsingTTSet.stream()
                        .map(NamedType::getName)
                        .collect(Collectors.toList());
                confirmMessage.append("\nOverwriting ")
                    .append(name)
                    .append(" will also delete the following ensemble sets which use it:")
                    .append("\n\n")
                    .append(String.join(",\n", eSetNames))
                    .append("\n\nDo you want to continue?");
            }
            else
            {
                confirmMessage.append("\nDo you want to overwrite existing temperature target set ")
                    .append(name)
                    .append("?");
            }
            int opt = JOptionPane.showConfirmDialog(this, confirmMessage, "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(opt == JOptionPane.YES_OPTION)
            {
                retVal = true;
                _fsg.removeTemperatureTargetSet(existingTTSet);
                _fsg.saveData();
            }
        }
        return retVal;
    }

    private List<TemperatureTargetSet> buildTempTargetSets(List<String> setNamesToImport)
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<TemperatureTargetSet> retVal = new ArrayList<>();
        if(_importFromExistingRadioButton.isSelected())
        {
            for(int row =0; row < _temperatureSetsTable.getRowCount(); row++)
            {
                TemperatureTargetSet temperatureTargetSet = new TemperatureTargetSet();
                Object checkedVal = _temperatureSetsTable.getValueAt(row, 0);
                if(checkedVal != null && Boolean.parseBoolean(checkedVal.toString()))
                {
                    Object name = _temperatureSetsTable.getValueAt(row, 1);
                    Object riverLocationObj = _temperatureSetsTable.getValueAt(row, 2);
                    if(name != null && !name.toString().trim().isEmpty() && setNamesToImport.contains(name.toString())
                        && riverLocationObj instanceof RiverLocation)
                    {
                        List<DSSPathname> pathnames = getSelectedTempTargetSetPathNames(name.toString());
                        temperatureTargetSet.setDssPathNames(pathnames);
                        temperatureTargetSet.setName(name.toString());
                        temperatureTargetSet.setDescription(_descriptionFieldImport.getText());
                        temperatureTargetSet.setUserDefined(false);
                        temperatureTargetSet.setDssSourcePath(Paths.get(_importFileChooserField.getText()));
                        temperatureTargetSet.setRiverLocation((RiverLocation)riverLocationObj);
                        temperatureTargetSet.setModified(true);
                        retVal.add(temperatureTargetSet);
                    }
                }
            }
        }
        else
        {
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

    @Override
    public void dispose()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        super.dispose();
    }

    private List<DSSPathname> getSelectedTempTargetSetPathNames(String collectionName)
    {
        List<DSSPathname> retVal = new ArrayList<>();
        if(collectionName != null && !collectionName.isEmpty())
        {
            retVal = _dssCollectionMapping.get(collectionName);
        }
        return retVal;
    }

    private void closeDialogAction()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        int opt = JOptionPane.YES_OPTION;
        if(isModified())
        {
            opt = JOptionPane.showConfirmDialog(this, "Cancel Temperature Target Set Import?", "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION);
        }
        if(opt == JOptionPane.YES_OPTION)
        {
            _importFileChooserField.setText("");
            dispose();
        }
    }

    private void createNewRadioAction()
    {
        CardLayout cardLayout = (CardLayout) _cardPanel.getLayout();
        cardLayout.show(_cardPanel, CREATE_PANEL_ID);
        validateOkButton();
    }

    private void importRadioAction()
    {
        CardLayout cardLayout = (CardLayout) _cardPanel.getLayout();
        cardLayout.show(_cardPanel, IMPORT_PANEL_ID);
        validateOkButton();
    }

    private void buildControls()
    {
        _importCreateButtonGroup = new ButtonGroup();
        _importFromExistingRadioButton = new RmaJRadioButton("Import Set From Existing");
        _createNewRadioButton = new RmaJRadioButton("Create New Set");
        _importCreateButtonGroup.add(_importFromExistingRadioButton);
        _importCreateButtonGroup.add(_createNewRadioButton);
        _importCreateButtonGroup.setSelected(_importFromExistingRadioButton.getModel(), true);

        RmaJPanel importCreateRadioButtonPanel = new RmaJPanel(new BorderLayout());
        importCreateRadioButtonPanel.add(_importFromExistingRadioButton, BorderLayout.WEST);
        importCreateRadioButtonPanel.add(_createNewRadioButton, BorderLayout.EAST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        getContentPane().add(importCreateRadioButtonPanel, gbc);

        _cardPanel = new RmaJPanel(new CardLayout());

        _riverLocationCombo = new RmaJComboBox<>();
        RmaJPanel importPanel = buildImportPanel();
        RmaJPanel createPanel = buildCreatePanel();
        _cardPanel.add(importPanel, IMPORT_PANEL_ID);
        _cardPanel.add(createPanel, CREATE_PANEL_ID);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 1.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.BOTH;
        gbc.insets    = RmaInsets.INSETS5505;
        getContentPane().add(_cardPanel, gbc);

        _okCancelPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
        _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.001;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5555;
        getContentPane().add(_okCancelPanel, gbc);
    }

    private RmaJPanel buildCreatePanel()
    {
        RmaJPanel createPanel = new RmaJPanel(new GridBagLayout());
        RmaJPanel nameDescPanel = new RmaJPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("Name:"), gbc);

        _nameTextField = new RmaJTextField();
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(_nameTextField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("Description:"), gbc);

        _descriptionField = new RmaJDescriptionField();
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.001;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(_descriptionField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(new JLabel("River Location:"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.001;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        nameDescPanel.add(_riverLocationCombo, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS0000;
        createPanel.add(nameDescPanel, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Number of Temperature Targets (Max " + MAX_NUM_USER_DEFINED_TEMP_TARGETS_IN_SET + "):"), gbc);

        _numberTempTargetsField = new RmaJIntegerField();
        _numberTempTargetsField.setValue(1);
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(_numberTempTargetsField, gbc);

        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Units:"), gbc);

        _unitsComboBox = new RmaJComboBox<>();
        String ft = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.ENGLISH_ID);
        String celsius = Parameter.getUnitsStringForSystem(Parameter.PARAMID_TEMP, Units.SI_ID);
        RmaListModel<String> unitsModel = new RmaListModel<>(false, ft, celsius);
        _unitsComboBox.setModel(unitsModel);
        _unitsComboBox.setSelectedIndex(0);
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.001;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(_unitsComboBox, gbc);

        return createPanel;
    }

    private RmaJPanel buildImportPanel()
    {
        RmaJPanel importPanel = new RmaJPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(new JLabel("Select file:"), gbc);

        _importFileChooserField = new RmaFileChooserField();
        _importFileChooserField.setDefaultPath(Project.getCurrentProject().getWorkspacePath());
        _importFileChooserField.setSelectedFilter(new RMAFilenameFilter("dss", ".dss"));
        _importFileChooserField.setAcceptAllFileFilterUsed(false);
        gbc = new GridBagConstraints();
        gbc.gridx     = 1;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(_importFileChooserField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(new JLabel("Description:"), gbc);

        _descriptionFieldImport = new RmaJDescriptionField();
        gbc = new GridBagConstraints();
        gbc.gridx     = 1;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(_descriptionFieldImport, gbc);

        _temperatureSetsTable = new RmaJTable(this, new String[]{"", "Temperature Target Set", "River Location"})
        {
            @Override
            public Dimension getPreferredScrollableViewportSize()
            {
                Dimension d = super.getPreferredScrollableViewportSize();
                d.height = getRowHeight() * 4;
                return d;
            }

            @Override
            public boolean isCellEditable(int row, int column)
            {
                boolean retVal = false;
                if(column == 0)
                {
                    retVal = true;
                }
                else if(column == 2)
                {
                    Object checkVal = _temperatureSetsTable.getValueAt(row, 0);
                    retVal = checkVal != null && Boolean.parseBoolean(checkVal.toString());
                }
                return retVal;
            }
        };
        _checkBoxEditorCheckBox = _temperatureSetsTable.setCheckBoxCellEditor(0);
        _riverLocationTableCombo = _temperatureSetsTable.setComboBoxEditor(2, new Object[]{}, true);
        _temperatureSetsTable.setColumnEnabled(false, 1);
        _temperatureSetsTable.setPopupMenuEnabled(true);
        _temperatureSetsTable.removePopupMenuRowEditingOptions();
        _temperatureSetsTable.setRowHeight(_temperatureSetsTable.getRowHeight() + 5);
        TableColumnModel columnModel = _temperatureSetsTable.getColumnModel();
        TableColumn column = columnModel.getColumn(0);
        column.setPreferredWidth(30);
        column.setMaxWidth(30);
        if(_temperatureSetsTable.getRowCount() > 0)
        {
            _temperatureSetsTable.deleteRow(0);
        }
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 0.001;
        gbc.weighty   = 1.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.BOTH;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(_temperatureSetsTable.getScrollPane(), gbc);

        return importPanel;
    }

}
