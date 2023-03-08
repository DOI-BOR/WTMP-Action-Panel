/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.extract.action;

import java.awt.EventQueue;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.JOptionPane;

import hec.ui.ProgressListener;

import gov.usbr.wq.merlindataexchange.DataExchangeEngine;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeEngineBuilder;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeStatus;
import gov.usbr.wq.merlindataexchange.parameters.AuthenticationParametersBuilder;
import gov.usbr.wq.merlindataexchange.parameters.MerlinParameters;
import gov.usbr.wq.merlindataexchange.parameters.MerlinParametersBuilder;
import usbr.wat.plugins.actionpanel.extract.model.ExtractLoginInfo;
import usbr.wat.plugins.actionpanel.ui.ProgressListenerDialog;

/**
 * @author mark
 *
 */
public class RunExtractAction
{

	private Window _parent;
	private MerlinDataExchangeStatus _status;

	/**
	 * @param extractDialog
	 */
	public RunExtractAction(Window parent)
	{
		super();
		_parent = parent;
	}

	/**
	 * @param selectedFiles
	 * @param dateTime
	 * @param dateTime2
	 * @param selectedItem
	 */
	public void extract(Window parent, MerlinParameters params, List<Path> selectedFiles, String infoString, String url)
	{
		ProgressListener progress = createProjectListener();
		
		DataExchangeEngine dataExchangeEngine = new MerlinDataExchangeEngineBuilder()
                .withConfigurationFiles(selectedFiles)
                .withParameters(params)
                .withProgressListener(progress)
                .build();	
		
		((ProgressListenerDialog)progress).setVisible(true);
		
		CompletableFuture<MerlinDataExchangeStatus> future = dataExchangeEngine.runExtract();
		
		Thread t = new Thread("Extract Wait Thread")
		{
			@Override
			public void run()
			{
				_status = future.join();
				if ( MerlinDataExchangeStatus.AUTHENTICATION_FAILURE  == _status )
				{
					redoExtractWithNewLoginInfo(parent, params, selectedFiles, infoString, url);
				}
				else if ( MerlinDataExchangeStatus.FAILURE  == _status )
				{
					EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
							"Extract Failed. See Log File for Details", "Extract Failed", JOptionPane.INFORMATION_MESSAGE));
				}
				else if ( MerlinDataExchangeStatus.PARTIAL_SUCCESS  == _status )
				{
					EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
							"Extract didn't extract all time series. See Log File for Details", "Partial Success", JOptionPane.INFORMATION_MESSAGE));
					
				}
			}
		};
		t.start();
	}
	/**
	 * @param parent
	 * @param params
	 * @param selectedFiles
	 * @param infoString
	 * @param url
	 */
	protected void redoExtractWithNewLoginInfo(Window parent,
			MerlinParameters params, List<Path> selectedFiles,
			String infoString, String url)
	{
		
		if ( _status== MerlinDataExchangeStatus.AUTHENTICATION_FAILURE )
		{
			if ( ExtractLoginInfo.askForLoginInfo(parent, infoString))
			{
				MerlinParameters params2 = new MerlinParametersBuilder()
		                .withWatershedDirectory(params.getWatershedDirectory())
		                .withLogFileDirectory(params.getLogFileDirectory())
		                .withAuthenticationParameters(new AuthenticationParametersBuilder()
		                        .forUrl(url)
		                        .setUsername(ExtractLoginInfo.getUserName())
		                        .andPassword(ExtractLoginInfo.getPassword().toCharArray())
		                        .build())
		                .withStoreOption(params.getStoreOption())
		                .withStart(params.getStart())
		                .withEnd(params.getEnd())
		                .withFPartOverride(params.getFPartOverride())
		                .build();
				extract(parent, params2, selectedFiles, infoString, url);
			}
		}
	}
	
	public MerlinDataExchangeStatus getExtractStatus()
	{
		return _status;
	}

	/**
	 * @return
	 */
	private ProgressListener createProjectListener()
	{
		if ( _parent == null )
		{
			return null;//new ProgressListenerFile();
		}
		return new ProgressListenerDialog(_parent, "Extract ");
	}

}
