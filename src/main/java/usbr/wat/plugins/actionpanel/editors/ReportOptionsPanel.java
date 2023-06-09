/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JLabel;
import javax.swing.JPanel;
import hec2.wat.WAT;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJComboBox;
import usbr.wat.plugins.actionpanel.io.OutputType;
import usbr.wat.plugins.actionpanel.io.ReportOptions;

public class ReportOptionsPanel extends JPanel
{

	public static final String PREF_NODE = "usbrReports";
	private RmaJComboBox<OutputType> _outputTypeCombo;
	private RmaJCheckBox _printHeaderFooterCheck;

	public ReportOptionsPanel()
	{
		super(new GridBagLayout());
		buildControls();
		fillPanel();
	}

	private void fillPanel()
	{
		Preferences node = getPreferencesNode();


		String outputTypeName = node.get("ReportType", OutputType.PDF.name());
		OutputType ot = OutputType.valueOf(outputTypeName);
		_outputTypeCombo.setSelectedItem(ot);


		boolean printHeaderFooter = node.getBoolean("PrintHeaderFooter", true);
		_printHeaderFooterCheck.setSelected(printHeaderFooter);
	}

	protected void buildControls()
	{
		JPanel typePanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(typePanel, gbc);

		JLabel label = new JLabel("File Type:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		typePanel.add(label, gbc);

		_outputTypeCombo = new RmaJComboBox<>(OutputType.values());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.001;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		typePanel.add(_outputTypeCombo, gbc);

		_printHeaderFooterCheck = new RmaJCheckBox("Print Headers and Footers");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_printHeaderFooterCheck, gbc);
	}

	public void setOutputType(OutputType ot)
	{
		if ( ot != null)
		{
			_outputTypeCombo.setSelectedItem(ot);
		}
	}

	public Object getSelectedOutputType()
	{
		OutputType ot = (OutputType) _outputTypeCombo.getSelectedItem();
		return ot;
	}

	public Preferences getPreferencesNode()
	{
		return WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(PREF_NODE);
	}

	public void saveSettings()
	{
		Preferences node = getPreferencesNode();
		try
		{
			node.clear();
		}
		catch (BackingStoreException e)
		{
			Logger.getLogger(ReportOptionsPanel.class.getName()).info("Failed to clear node "+node.absolutePath()+" Error:"+e);
		}
		OutputType ot = (OutputType)_outputTypeCombo.getSelectedItem();
		node.put("ReportType", ot.name());

		boolean printHeaderFooter = _printHeaderFooterCheck.isSelected();
		node.putBoolean("PrintHeaderFooter", printHeaderFooter);
	}

	public ReportOptions getReportOptions()
	{
		ReportOptions options = new ReportOptions();
		options.setOutputType((OutputType)_outputTypeCombo.getSelectedItem());
		options.setPrintHeadersFooters(_printHeaderFooterCheck.isSelected());
		return options;
	}
}
