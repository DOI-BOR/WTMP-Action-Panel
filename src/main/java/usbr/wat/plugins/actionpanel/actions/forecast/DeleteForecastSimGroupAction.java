/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import com.rma.client.ObjectChooser;
import com.rma.model.Manager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;

/**
 * @author mark
 *
 */
public class DeleteForecastSimGroupAction extends AbstractAction
{
	private final ActionsWindow _parent;

	public DeleteForecastSimGroupAction(ActionsWindow parent)
	{
		super("Delete...");
		_parent = parent;
	}
	@Override
	public void actionPerformed(ActionEvent e)
	{
		List<ManagerProxy> simGroups = Project.getCurrentProject().getManagerProxyListForType(ForecastSimGroup.class);
		ObjectChooser chooser = new ObjectChooser(_parent, true, simGroups, ObjectChooser.DELETE);
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
		ManagerProxy proxy;
		Manager manager;
		for(int i = 0;i < objects.length; i++ )
		{
			proxy = (ManagerProxy) objects[i];
			manager = proxy.loadManager();
			if ( manager != null )
			{
				 prj.removeManager(manager);
			}
		}
		_parent.getForecastPanel().loadSimulationGroupCombo();

	}

}
