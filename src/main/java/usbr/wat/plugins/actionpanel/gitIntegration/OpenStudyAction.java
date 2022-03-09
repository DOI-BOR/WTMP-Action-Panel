/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel.gitIntegration;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;

import hec2.wat.WAT;

import rma.util.RMAFilenameFilter;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class OpenStudyAction extends AbstractAction
{
	private StudyStorageDialog _studyStorageDialog;

	public OpenStudyAction(StudyStorageDialog studyStorageDialog)
	{
		super("Open Study");
		putValue(Action.SHORT_DESCRIPTION, "Opens the Study for the selected Repository");
		_studyStorageDialog = studyStorageDialog;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		openStudyAction();
		
	}

	/**
	 * 
	 */
	public void openStudyAction()
	{
		RepoInfo repo = _studyStorageDialog.getSelectedRepo();
		String studyFolder = repo.getLocalPath();
		RMAFilenameFilter filter = new RMAFilenameFilter("sty");
		filter.setAcceptDirectories(false);
		List<String> studyFiles = FileManagerImpl.getFileManager().list(studyFolder, filter, false);
		if ( studyFiles != null && !studyFiles.isEmpty())
		{
			WAT.getBrowserFrame().projectOpen(studyFiles.get(0));
		}
		else
		{
			JOptionPane.showMessageDialog(_studyStorageDialog, "No Study files found in "+studyFolder, 
					"No Study Opened", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}
}
