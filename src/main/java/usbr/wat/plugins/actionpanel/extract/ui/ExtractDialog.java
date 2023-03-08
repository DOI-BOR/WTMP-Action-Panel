/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.extract.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.rma.client.Browser;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec.io.Identifier;
import hec.io.impl.StoreOptionImpl;
import hec.model.RunTimeWindow;

import gov.usbr.wq.merlindataexchange.MerlinConfigParseException;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeParser;
import gov.usbr.wq.merlindataexchange.parameters.AuthenticationParametersBuilder;
import gov.usbr.wq.merlindataexchange.parameters.MerlinParameters;
import gov.usbr.wq.merlindataexchange.parameters.MerlinParametersBuilder;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.DateTimePanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.swing.RmaJTextField;
import rma.swing.text.DateDocument;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.extract.action.RunExtractAction;
import usbr.wat.plugins.actionpanel.extract.model.ExtractLoginInfo;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class ExtractDialog extends RmaJDialog
{
	private static final String SHARED_DIR = "shared";
	private static final String EXTRACT_LOGS_DIR = "extract/logs";
	private static final String EXTRACT_CONFIG_DIR = "extract/config";
	private static final String XML_EXTENSION = "xml";
	private static final String GRAB_DATA_URL = System.getProperty("Extract.Url", "https://www.grabdata2.com");
	
	
	private RmaJTextField _simGroupFld;
	private DateTimePanel _startDateTimePanel;
	private DateTimePanel _endDateTimePanel;
	private RmaJComboBox _storeRuleCombo;
	private RmaJTextField _dssFpartFld;
	private RmaJTable _extractTable;
	private JButton _viewLogBtn;
	private ButtonCmdPanel _cmdPanel;
	private JMenuItem _viewConfigFileMenu;
	private JMenuItem _validateConfigFileMenu;

	public ExtractDialog(Window parent, SimulationGroup simulationGroup)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
		fillForm(simulationGroup);
	}

	

	


	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Run Extract");
		
		JLabel label = new JLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_simGroupFld = new RmaJTextField();
		_simGroupFld.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_simGroupFld, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JSeparator(), gbc);
		
		JPanel timeWindowPanel = new JPanel(new GridBagLayout());
		timeWindowPanel.setBorder(BorderFactory.createTitledBorder("Time Window"));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(timeWindowPanel, gbc);
		
		_startDateTimePanel= new DateTimePanel(DateTimePanel.HORIZONTAL_LAYOUT, "Start Date:", "Time:", DateDocument.DDMMMYYYY);
		_startDateTimePanel.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		timeWindowPanel.add(_startDateTimePanel, gbc);
		
		_endDateTimePanel= new DateTimePanel(DateTimePanel.HORIZONTAL_LAYOUT, "End Date:", "Time:", DateDocument.DDMMMYYYY);
		_endDateTimePanel.setEditable(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		timeWindowPanel.add(_endDateTimePanel, gbc);
		
		label = new JLabel("DSS Store Rule:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		Vector<String>storeOptions = new Vector<>();
		storeOptions.add("0-replace-all");
		storeOptions.add("1-replace-missing-values-only");
		storeOptions.add("2-replace-all-create");
		storeOptions.add("3-replace-all-delete");
		storeOptions.add("4-replace-with-non-missing");
		storeOptions.add("delete-insert");
		storeOptions.add("do-not-replace");
		storeOptions.add("replace-all");
		storeOptions.add("replace-missing-values-only");
		storeOptions.add("replace-with-non-missing");
		_storeRuleCombo = new RmaJComboBox<>(storeOptions);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_storeRuleCombo, gbc);

		label = new JLabel("DSS F-Part (optional):");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		_dssFpartFld = new RmaJTextField();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_dssFpartFld, gbc);
		
		
		label = new JLabel("Extract Configuration Files:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(label, gbc);
		
		
		String[] headers = new String[] {"Select", "Extract File"};
		_extractTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight()*6;
				return d;
			}
			
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return column == 0;
			}
			@Override
			public String getToolTipText(MouseEvent e)
			{
				int row = rowAtPoint(e.getPoint());
				if ( row == -1 )
				{
					return super.getToolTipText(e);
				}
				int col = columnAtPoint(e.getPoint());
				if ( col == 1 )
				{
					Identifier id = (Identifier) getValueAt(row, col);
					return id.getPath();
				}
				return super.getToolTipText(e);
				
			}
		};
		
		_viewConfigFileMenu = new JMenuItem("View File...");
		 
		if ( Desktop.isDesktopSupported() )
		{
			_extractTable.addPopupItem(_viewConfigFileMenu, 0);
		}
		_validateConfigFileMenu = new JMenuItem("Validate File...");
		 
		_extractTable.addPopupItem(_validateConfigFileMenu, 0);
		
		_extractTable.setCheckBoxCellEditor(0);
		_extractTable.setRowHeight(_extractTable.getRowHeight()+5);
		_extractTable.setColumnWidths(115, 335);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_extractTable.getScrollPane(), gbc);
		
		
		
		_viewLogBtn = new JButton("Extract Log...");
		_viewLogBtn.setToolTipText("View the extract logs");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_viewLogBtn, gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_BUTTON| ButtonCmdPanel.CLOSE_BUTTON);
		JButton okButton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		okButton.setText("Extract");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}
	
	

	/**
	 * 
	 */
	private void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						
						runExtract();
						break;
					case ButtonCmdPanel.CLOSE_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
		_viewLogBtn.addActionListener(e->viewExtractLog());
		_viewConfigFileMenu.addActionListener(e->viewConfigFile());
		_validateConfigFileMenu.addActionListener(e->validateConfigFile());
	}
	
	






	/**
	 * @return
	 */
	private void validateConfigFile()
	{
		int row = _extractTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		Identifier id = (Identifier) _extractTable.getValueAt(row, 1);
		if ( id == null )
		{
			return;
		}
		
		RmaFile file = FileManagerImpl.getFileManager().getFile(id.getPath());
		Path path = file.toPath();
		
		try
		{
			MerlinDataExchangeParser.parseXmlFile(path);
			JOptionPane.showMessageDialog(this, "File Format Validation Successful", "Success", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (MerlinConfigParseException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Parsing Failed", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}






	/**
	 * @param simulationGroup 
	 * 
	 */
	private void fillForm(SimulationGroup simulationGroup)
	{
		_simGroupFld.setText(simulationGroup.getName());
		JButton okbutton = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		if ( simulationGroup.getAnalysisPeriod() != null )
		{
			RunTimeWindow rtw = simulationGroup.getAnalysisPeriod().getRunTimeWindow();
			_startDateTimePanel.setDateTime(rtw.getStartTime(), Project.getCurrentProject().getTimeZone());
			_endDateTimePanel.setDateTime(rtw.getEndTime(), Project.getCurrentProject().getTimeZone());
			okbutton.setToolTipText("Run the data extract");
			okbutton.setEnabled(true);
		}
		else
		{
			okbutton.setEnabled(false);
			okbutton.setToolTipText("No Time Window for Simulation Group");
		}
		
		_extractTable.deleteCells();
		
		String dir = Project.getCurrentProject().getProjectDirectory();
		dir = RMAIO.concatPath(dir, SHARED_DIR);
		dir = RMAIO.concatPath(dir, EXTRACT_CONFIG_DIR);
		List<Identifier> configFiles = FileManagerImpl.getFileManager().getFileList(dir, XML_EXTENSION, true);
		Identifier id;
		Vector<Object> row;
		for (int i = 0;i < configFiles.size();i++ )
		{
			row = new Vector<>(2);
			row.add(Boolean.FALSE);
			row.add(configFiles.get(i));
			_extractTable.appendRow(row);
		}
	}	
	/**
	 * @return
	 */
	private void viewExtractLog()
	{
		String dir = Project.getCurrentProject().getProjectDirectory();
		dir = RMAIO.concatPath(dir, SHARED_DIR);
		dir = RMAIO.concatPath(dir, EXTRACT_LOGS_DIR);
		JFileChooser chooser = new JFileChooser(dir);
		int opt = chooser.showOpenDialog(this);
		if ( opt != JFileChooser.APPROVE_OPTION )
		{
			return;
		}
		File file = chooser.getSelectedFile();
		if ( file == null )
		{
			return;
		}
		if ( Desktop.isDesktopSupported() )
		{
			try
			{
				Desktop.getDesktop().open(file);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Browser.getBrowserFrame().openFile(file, false);
		}
	}
	
	/**
	 * @return
	 */
	private void viewConfigFile()
	{
		int row = _extractTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		Identifier id = (Identifier) _extractTable.getValueAt(row, 1);
		if ( id == null )
		{
			return;
		}
		RmaFile file = FileManagerImpl.getFileManager().getFile(id.getPath());
		
		
		if ( Desktop.isDesktopSupported() )
		{
			try
			{
				Desktop.getDesktop().open(file);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
		
	}
	
	public List<String>getSelectedFiles()
	{
		int rows = _extractTable.getNumRows();
		List<String>files = new ArrayList<>(rows);
		Object selected;
		Identifier id;
		for (int r = 0;r < rows; r++ )
		{
			selected = _extractTable.getValueAt(r, 0);
			if ( selected == Boolean.TRUE || Boolean.parseBoolean(selected.toString()) )
			{
				id = (Identifier) _extractTable.getValueAt(r,1);
				files.add(id.getPath());
			}
		}
		return files;
	}
	public List<Path>getSelectedPaths()
	{
		List<Path>paths = new ArrayList<>();
		List<String> files = getSelectedFiles();
		Path path;
		for(int i=0; i< files.size();i++)
		{
			path = Paths.get(files.get(i));
			paths.add(path);
		}
		return paths;
	}
	/**
	 * 
	 */
	protected void runExtract()
	{
		List<Path> selectedPaths = getSelectedPaths();
		if (selectedPaths.isEmpty() )
		{
			JOptionPane.showMessageDialog(this,  "Please select the extract configuration files to use.", "No Files Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String userName = ExtractLoginInfo.getUserName();
		String password = ExtractLoginInfo.getPassword();
		
		if ( userName == null || password == null )
		{
			if ( !ExtractLoginInfo.askForLoginInfo(this, "Enter login information for "+GRAB_DATA_URL))
			{
				return;
			}
		}
		userName = ExtractLoginInfo.getUserName();
		password = ExtractLoginInfo.getPassword();
		
		String prjFolder = Project.getCurrentProject().getProjectDirectory();
		Path prjPath = Paths.get(prjFolder);
		String logFolder = RMAIO.concatPath(prjFolder, SHARED_DIR);
		logFolder = RMAIO.concatPath(logFolder, EXTRACT_LOGS_DIR);
		FileManagerImpl.getFileManager().createDirectory(logFolder);
		Path logPath = Paths.get(logFolder);
		Instant startInstant = _startDateTimePanel.getDateTime().elementAt(0).getJavaDate(0).toInstant();
		Instant endInstant = _endDateTimePanel.getDateTime().elementAt(0).getJavaDate(0).toInstant();
		String fpart = _dssFpartFld.getText().trim();
		if ( fpart.isEmpty())
		{
			fpart = null;
		}
		
		MerlinParameters params = new MerlinParametersBuilder()
	                .withWatershedDirectory(prjPath)
	                .withLogFileDirectory(logPath)
	                .withAuthenticationParameters(new AuthenticationParametersBuilder()
	                        .forUrl(GRAB_DATA_URL)
	                        .setUsername(userName)
	                        .andPassword(password.toCharArray())
	                        .build())
	                .withStoreOption(getStoreOption())
	                .withStart(startInstant)
	                .withEnd(endInstant)
	                .withFPartOverride(fpart)
	                .build();
		 

		RunExtractAction action = new RunExtractAction(this);
		action.extract(this, params, selectedPaths, 
				"Enter login information for "+GRAB_DATA_URL, GRAB_DATA_URL);
		
	}





	/**
	 * @return
	 */
	private StoreOptionImpl getStoreOption()
	{
		StoreOptionImpl soi = new StoreOptionImpl();
		soi.setRegular((String)_storeRuleCombo.getSelectedItem());
		return soi;
	}

}
