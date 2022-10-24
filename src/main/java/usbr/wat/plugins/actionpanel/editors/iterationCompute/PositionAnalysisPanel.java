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

import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.editors.PositionAnalysisBcPanel;
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;

/**
 * @author Mark Ackerman
 *
 */
public class PositionAnalysisPanel extends BaseAnalysisJPanel
{


	private PositionAnalysisBcPanel _positionAnalysisBcPanel;

	/**
	 * @param editIterationSettingsDialog
	 */
	public PositionAnalysisPanel(EditIterationSettingsDialog parent)
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
		
		TitledBorder border = BorderFactory.createTitledBorder("Position Analysis Settings");
		setBorder(border);
		
		_positionAnalysisBcPanel = new PositionAnalysisBcPanel(_parent, this);
		
		_tabbedPane.addTab(_positionAnalysisBcPanel.getTabname(),_positionAnalysisBcPanel);
		_maxMembersFld.setEditable(false);
	}
	/**
	 * @param modelAlt
	 * @param iterationSettings
	 */
	public void fillPanel(ModelAlternative modelAlt, PositionAnalysisSettings positionAnalysisSettings)
	{
		super.fillPanel(positionAnalysisSettings);
		String variantName = _parent.getSelectedSimulation().getVariantName();
		modelAlt.setVariantName(variantName);
		ModelAltIterationSettings modelAltSettings = positionAnalysisSettings.getModelAltSettings(modelAlt);
		_positionAnalysisBcPanel.fillPanel(modelAltSettings);
	}

	

	/**
	 * @param iterationSettings
	 */
	public void savePanel(PositionAnalysisSettings iterationSettings)
	{
		super.savePanel(iterationSettings);
		
		ModelAlternative selectedModelAlt = _parent.getSelectedModelAlternative();
		if ( selectedModelAlt != null )
		{
			ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(selectedModelAlt);
			_positionAnalysisBcPanel.savePanel(modelAltSettings);
		}
		
	}
	
	@Override
	protected String getPanelName()
	{
		return "Position Analysis";
	}

}
