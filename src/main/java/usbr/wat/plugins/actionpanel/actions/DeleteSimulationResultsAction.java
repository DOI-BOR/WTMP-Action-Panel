/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;

import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.model.ResultsData;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTableNode;

/**
 * @author mark
 *
 */
public class DeleteSimulationResultsAction extends AbstractAction
{

	private ActionsWindow _parent;
	private UsbrPanel _parentPanel;

	/**
	 * @param parent
	 */
	public DeleteSimulationResultsAction(ActionsWindow parent, UsbrPanel parentPanel)
	{
		super("Delete Results");
		setEnabled(false);
		_parent = parent;
		_parentPanel = parentPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		deleteResults();
	}

	/**
	 * 
	 */
	private void deleteResults()
	{
		List<ResultsData> results = _parent.getSelectedResults();
		if ( results.isEmpty())
		{
			return;
		}
		String msg = "<html>Do you want to delete the following results:<br>";
		for(int i = 0;i < results.size();i++ )
		{
			msg = msg.concat(results.get(i).getName());
			msg = msg.concat("<br>");
		}
		msg = msg.concat("</html>");
		String title = "Confirm Deletions";
		int opt = JOptionPane.showConfirmDialog(_parent, msg, title, JOptionPane.YES_NO_OPTION);
		if ( JOptionPane.YES_OPTION != opt )
		{
			return;
		}
		ResultsData result;
		for (int i = 0;i < results.size(); i++ )
		{
			result = results.get(i);
			if ( deleteResultsFolder(result.getFolder()))
			{
				SimulationTreeTableNode simNode = _parentPanel.getSimulationTreeTable().getSimulationNodeFor(result.getSimulation());
				simNode.removeResultsFor(result);
			}
			else
			{
				JOptionPane.showMessageDialog(_parent, "Failed to remove folder "
					+result.getFolder()+" for "+result.getName(), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * @param result
	 * @return
	 */
	private boolean deleteResultsFolder(String resultsFolder)
	{
		return FileManagerImpl.getFileManager().deleteDirectory(resultsFolder);
	}

}
