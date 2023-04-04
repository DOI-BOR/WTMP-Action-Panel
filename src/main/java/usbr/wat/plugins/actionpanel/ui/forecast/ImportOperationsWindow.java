/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.ui.forecast;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import com.rma.model.Project;
import com.rma.swing.RmaFileChooserField;
import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaInsets;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTextField;
import rma.util.RMAFilenameFilter;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.model.forecast.OperationsData;

public class ImportOperationsWindow extends RmaJDialog
{
	private RmaJTextField _nameFld;
	private RmaJDescriptionField _descFld;
	private RmaFileChooserField _opsFileFld;
	private ButtonCmdPanel _cmdPanel;
	private boolean _canceled;

	public ImportOperationsWindow(Window parent)
	{
		super(parent, "Import Operations Data", true);
		buildControls();
		addListeners();
		pack();
		setSize(500, 200);
		setLocationRelativeTo(getParent());
	}

	protected void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());

		JLabel label = new JLabel("Operations Name:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		_nameFld = new RmaJTextField();
		label.setLabelFor(_nameFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_nameFld, gbc);

		label = new JLabel("Description:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		_descFld = new RmaJDescriptionField();
		label.setLabelFor(_descFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_descFld, gbc);

		label = new JLabel("Operations File:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(label, gbc);

		_opsFileFld = new RmaFileChooserField();
		_opsFileFld.setAcceptAllFileFilterUsed(true);
		List<FileFilter> filters = new ArrayList<>();
		RMAFilenameFilter filter = new RMAFilenameFilter("xlsx", "Excel Files");
		filters.add(filter);
		filter = new RMAFilenameFilter("csv", "Comma Separated Files");
		filters.add(filter);
		_opsFileFld.setFilters(filters);
		label.setLabelFor(_opsFileFld);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.001;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_opsFileFld, gbc);


		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_CANCEL_BUTTONS);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;
		add(_cmdPanel, gbc);
	}

	protected void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON:
					   	if ( isValidForm())
						{
							saveForm();
							_canceled = false;
							setVisible(false);
						}
						break;
					case ButtonCmdPanel.CANCEL_BUTTON:
						_canceled = false;
						setVisible(false);
						break;
				}
			}
		});
	}

	private void saveForm()
	{
	}

	private boolean isValidForm()
	{
		String name = _nameFld.getText().trim();
		if ( name.isEmpty() )
		{
			JOptionPane.showMessageDialog(this, "Please enter a name", "No Name", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		String path = _opsFileFld.getPath();
		if ( path.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Please Select an Operations File", "No File Source", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		return true;
	}

	public boolean isCanceled()
	{
		return _canceled;
	}

	public void fillForm(ForecastSimGroup fsg)
	{
	}

	public OperationsData getOperationsData()
	{
		OperationsData opsData = new OperationsData();
		opsData.setName(_nameFld.getText().trim());
		opsData.setDescription(_descFld.getText().trim());
		String opsPath = _opsFileFld.getPath().trim();
		String relOpsPath = Project.getCurrentProject().getRelativePath(opsPath);
		opsData.setOperationsFile(relOpsPath);
		return opsData;
	}
}
