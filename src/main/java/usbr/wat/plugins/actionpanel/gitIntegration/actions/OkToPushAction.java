/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hec.io.ProcessOutputLine;

import usbr.wat.plugins.actionpanel.gitIntegration.StudyStorageDialog;

/**
 * check to see if its ok to push changes to the server
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class OkToPushAction extends AbstractGitAction
{

	private static final String OK_TO_PUSH_CMD = "--okToPush";
	private static final String ERROR = "ERROR:";
	private static final String CONNECTED_TO_GIT = "Connected to GIT Repo!";
	private List<String> _cmdBeingRun;

	/**
	 * @param cmd
	 * @param studyStorageDialog 
	 */
	public OkToPushAction(List<String> cmd, StudyStorageDialog studyStorageDialog)
	{
		super("Check if Upload is Ok", studyStorageDialog);
		_cmdBeingRun = new ArrayList<>();
		_cmdBeingRun.addAll(cmd);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		isOkToPush();
	}

	/**
	 * @return
	 */
	public boolean isOkToPush()
	{
		_cmdBeingRun.remove(UploadStudyAction.UPLOAD_CMD);
		_cmdBeingRun.add(0,OK_TO_PUSH_CMD);
		if (!_cmdBeingRun.contains(FetchAction.FETCH_CMD))
		{
			_cmdBeingRun.add(1,FetchAction.FETCH_CMD);
		}
		setShowFailedCallMessage(false);
		boolean rv =  callGit(_cmdBeingRun);
		if ( !rv )
		{
			List<ProcessOutputLine> output = parseOutput(getOutput());
			List<String> lines =  getOutputLines(output);
			String msg = getErrorMessage(null, lines);
			String title ="Can't Upload to Server";
			int idx = _cmdBeingRun.indexOf(SUB_MODULE);
			if ( idx > -1 )
			{
				title ="Can't Upload "+_cmdBeingRun.get(idx+1)+" to Server";
			}
			showErrorMsg(title, msg);
			return false;
		}
		return true;
	}
	
	private static List<ProcessOutputLine> parseOutput(List<ProcessOutputLine> output)
	{
		Iterator<ProcessOutputLine> iter = output.iterator();
		boolean foundStart = false;
		ProcessOutputLine line;
		List<ProcessOutputLine>data = new ArrayList<>(output);
		while (iter.hasNext())
		{
			line = iter.next();
			if ( line.getLine().startsWith(CONNECTED_TO_GIT))
			{
				iter.remove();
				foundStart = true;
				break;
			}
			else
			{
				iter.remove();
			}
		}
		if (!foundStart )
		{
			return data;
		}
		return output;
	}
	
	protected static String getErrorMessage(String header, List<String>msgLines)
	{
		StringBuilder builder = new StringBuilder();
		if ( header != null )
		{
			builder.append(header);
		}
		for (int i = 0;i < msgLines.size(); i++ )
		{
			builder.append("\n");
			builder.append(msgLines.get(i));
		}
		return builder.toString();
	}
	

}
