/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import com.rma.client.ObjectChooser;
import com.rma.factories.DeleteManagerFactory;
import com.rma.model.Manager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import hec2.wat.model.WatSimulation;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.AbstractSimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;

/**
 * @author mark
 *
 */
public class DeleteForecastSimGroupAction extends AbstractAction
{
	private final ActionsWindow _parent;
	private final BaseSimulationGroupPanel _parentPanel;

	public DeleteForecastSimGroupAction(BaseSimulationGroupPanel parentPanel, ActionsWindow parent)
	{
		super("Delete...");
		_parent = parent;
		_parentPanel = parentPanel;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		List<ManagerProxy> simGroups = Project.getCurrentProject().getManagerProxyListForType(ForecastSimGroup.class);
		ObjectChooser chooser = new ObjectChooser(ActionPanelPlugin.getInstance().getActionsWindow(), true, simGroups, ObjectChooser.DELETE);
		chooser.setTitle("Delete Simulation Groups");
		chooser.setVisible(true);
		if ( chooser.isCanceled())
		{
			return;
		}
		Object[] objects = chooser.getSelectedObjects();
		if (objects == null )
		{
			return;
		}
		Project prj = Project.getCurrentProject();
		ManagerProxy proxy = null;
		Manager manager;
		for(int i = 0;i < objects.length; i++ )
		{
			proxy = (ManagerProxy) objects[i];
			manager = proxy.loadManager();
			if ( manager instanceof AbstractSimulationGroup)
			{
				List<WatSimulation> sims = ((AbstractSimulationGroup) manager).getSimulations();
				DeleteManagerFactory.deleteManager(manager);
				for(int n = sims.size()-1; n >= 0; n--)
				{
					DeleteManagerFactory.deleteManager(sims.get(n));
				}
			}
		}
		if (_parentPanel != null )
		{
			_parentPanel.simulationGroupDeleted(proxy);
		}
		ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().loadSimulationGroupCombo();

	}

}
