package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;
import com.rma.swing.RmaFileChooserField;
import hec.heclib.dss.CondensedReference;
import hec.heclib.dss.DSSPathname;
import rma.swing.ButtonCmdPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJPanel;
import rma.swing.RmaJRadioButton;
import rma.swing.RmaJTextField;
import rma.util.RMAFilenameFilter;
import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class TempTargetImportDialog extends RmaJDialog
{
    private static final Logger LOGGER = Logger.getLogger(TempTargetImportDialog.class.getName());
    private static final String IMPORT_PANEL_ID = "IMPORT_PANEL";
    private static final String CREATE_PANEL_ID = "CREATE_PANEL";
    private final Consumer<TemperatureTargetSet> _consumeTempTargetSetAction;
    private final List<String> _existingSetNames;
    private RmaJRadioButton _importFromExistingRadioButton;
    private RmaJRadioButton _createNewRadioButton;
    private ButtonCmdPanel _okCancelPanel;
    private RmaJPanel _cardPanel;
    private RmaFileChooserField _importFileChooserField;
    private RmaJTextField _nameTextField;
    private RmaJDescriptionField _descriptionField;
    private RmaJComboBox<String> _temperatureSetsComboBox;
    private final Map<String, List<DSSPathname>> _dssCollectionMapping = new TreeMap<>();
    private boolean _ignoreFocusLost;
    private final List<String> _invalidFilesToDelete = new ArrayList<>();
    private ButtonGroup _importCreateButtonGroup;

    public TempTargetImportDialog(Window parent, List<String> existingSetNames, Consumer<TemperatureTargetSet> consumeTempTargetSetAction)
    {
        super(parent, true);
        setTitle("Select Temperature Target Set");
        getContentPane().setLayout(new GridBagLayout());
        _existingSetNames = existingSetNames;
        _consumeTempTargetSetAction = consumeTempTargetSetAction;
        buildControls();
        addListeners();
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(getSize());
        setSize(new Dimension(400, getPreferredSize().height));
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void addListeners()
    {
        _nameTextField.addKeyListener(getValidateKeyListener());
        _importFileChooserField.addKeyListener(getValidateKeyListener());
        _importFileChooserField.addFocusListener(getImportFocusListener());
        _importFileChooserField.addFileSelectedListener(f -> dssFileSelected());
        ((JButton)_importFileChooserField.getComponents()[0]).addActionListener(e -> ellipsesPressed());
        _temperatureSetsComboBox.addActionListener(e -> validateOkButton());
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

    private void dssFileSelected()
    {
        String fileNameTxt = _importFileChooserField.getText();
        if(fileNameTxt == null || fileNameTxt.trim().isEmpty())
        {
            return;
        }
        try
        {
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
            updateAvailableTempSetsInCombo();
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

    private void updateAvailableTempSetsInCombo()
    {
        DefaultComboBoxModel<String> updatedModel = new DefaultComboBoxModel<>();
        SortedSet<String> keys = new TreeSet<>(_dssCollectionMapping.keySet());
        for(String collectionId : keys)
        {
            updatedModel.addElement(collectionId);
        }
        _temperatureSetsComboBox.setModel(updatedModel);
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
        JButton okButton = _okCancelPanel.getButton(ButtonCmdPanel.OK_BUTTON);
        boolean invalidCreate = _nameTextField.getText() == null || _nameTextField.getText().trim().isEmpty();
        String absPath = Project.getCurrentProject().getAbsolutePath(_importFileChooserField.getText());
        boolean invalidImport = !(Paths.get(absPath).toFile().exists())
                || _temperatureSetsComboBox.getSelectedIndex() < 0 || _temperatureSetsComboBox.getSelectedItem() == null;
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

    private void okAction()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        TemperatureTargetSet set = buildTempTargetSet();
        if(_existingSetNames.contains(set.getName().trim()))
        {
            int opt = JOptionPane.showConfirmDialog(this, set.getName() + " already exists. Overwrite it?", "Confirm Override",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(opt == JOptionPane.YES_OPTION)
            {
                _consumeTempTargetSetAction.accept(set);
                dispose();
            }
        }
        else
        {
            _consumeTempTargetSetAction.accept(set);
            dispose();
        }
    }

    private TemperatureTargetSet buildTempTargetSet()
    {
        TemperatureTargetSet retVal = new TemperatureTargetSet();
        if(_importFromExistingRadioButton.isSelected())
        {
            List<DSSPathname> pathnames = getSelectedTempTargetSetPathNames();
            retVal.setDssPathNames(pathnames);
            if(!pathnames.isEmpty())
            {
                Object collectionId = _temperatureSetsComboBox.getSelectedItem();
                if(collectionId != null)
                {
                    retVal.setName(collectionId.toString());
                }
            }
            retVal.setUserDefined(false);
            retVal.setFilePath(Paths.get(_importFileChooserField.getText()));
            retVal.setModified(true);
        }
        else
        {
            TemperatureTargetSet temperatureTargetSet = new TemperatureTargetSet();
            temperatureTargetSet.setName(_nameTextField.getText());
            temperatureTargetSet.setDescription(_descriptionField.getText());
            temperatureTargetSet.setUserDefined(true);
            temperatureTargetSet.setModified(true);
            retVal = temperatureTargetSet;
        }
        return retVal;
    }

    @Override
    public void dispose()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        super.dispose();
    }

    private List<DSSPathname> getSelectedTempTargetSetPathNames()
    {
        List<DSSPathname> retVal = new ArrayList<>();
        Object selectedSet = _temperatureSetsComboBox.getSelectedItem();
        if(selectedSet != null)
        {
            retVal = _dssCollectionMapping.get(selectedSet.toString());
        }
        return retVal;
    }

    private void closeDialogAction()
    {
        deleteInvalidFiles(_invalidFilesToDelete);
        int opt = JOptionPane.YES_OPTION;
        if(isModified())
        {
            opt = JOptionPane.showConfirmDialog(this, "Cancel Temperature Target Set Selection?", "Confirm Cancel",
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
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Name:"), gbc);

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
        createPanel.add(_nameTextField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        gbc.weighty   = 0.0;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        createPanel.add(new JLabel("Description:"), gbc);

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
        createPanel.add(_descriptionField, gbc);
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
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(new JLabel("Select set:"), gbc);

        _temperatureSetsComboBox = new RmaJComboBox<>();
        gbc = new GridBagConstraints();
        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridy     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 1.0;
        gbc.weighty   = 0.001;
        gbc.anchor    = GridBagConstraints.NORTHWEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = RmaInsets.INSETS5505;
        importPanel.add(_temperatureSetsComboBox, gbc);

        return importPanel;
    }

}
