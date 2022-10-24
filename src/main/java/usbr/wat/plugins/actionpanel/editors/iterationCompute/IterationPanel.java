/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.model.IterationSettings;
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;
import usbr.wat.plugins.actionpanel.model.SensitivitySettings;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class IterationPanel extends BaseAnalysisJPanel
{
	
	private IterationBcPanel _bcPanel;
	private SensitivityPanel _sensitivityPanel;

	public IterationPanel(EditIterationSettingsDialog parent)
	{
		super(new GridBagLayout(), parent);
	}

	/**
	 * 
	 */
	@Override
	protected void buildControls()
	{
		super.buildControls();
		
		TitledBorder border = BorderFactory.createTitledBorder("Iteration Settings");
		setBorder(border);	
		
		_bcPanel = new IterationBcPanel(_parent);
		_sensitivityPanel = new SensitivityPanel(_parent);
		
		_tabbedPane.addTab(_bcPanel.getTabname(),_bcPanel);
		_tabbedPane.addTab(_sensitivityPanel.getTabname(), _sensitivityPanel);
		
	}
	
	/**
	 * @param modelAlt
	 * @param iterationSettings
	 */
	public void fillPanel(ModelAlternative modelAlt, IterationSettings iterationSettings)
	{
		super.fillPanel(iterationSettings);
		String variantName = _parent.getSelectedSimulation().getVariantName();
		modelAlt.setVariantName(variantName);
		ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(modelAlt);
		_bcPanel.fillPanel(modelAltSettings);
		SensitivitySettings sSettings = iterationSettings.getSensitivitySettings();
		_sensitivityPanel.fillPanel(sSettings);
	}

	

	/**
	 * @param iterationSettings
	 */
	public void savePanel(IterationSettings iterationSettings)
	{
		super.savePanel(iterationSettings);
		
		ModelAlternative selectedModelAlt = _parent.getSelectedModelAlternative();
		if ( selectedModelAlt != null )
		{
			ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(selectedModelAlt);
			_bcPanel.savePanel(modelAltSettings);
		}
		
		_sensitivityPanel.savePanel(iterationSettings.getSensitivitySettings());
	}

	@Override
	protected String getPanelName()
	{
		return "Iteration";
	}

	@Override
	public void setSimulation(WatSimulation selectedSim)
	{
		super.setSimulation(selectedSim);
		_sensitivityPanel.setSimulation(selectedSim);
	}
	
	

	
}
