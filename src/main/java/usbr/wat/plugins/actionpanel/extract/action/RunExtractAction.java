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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import gov.usbr.wq.merlindataexchange.MerlinConfigParseException;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeParser;
import gov.usbr.wq.merlindataexchange.configuration.DataExchangeConfiguration;
import gov.usbr.wq.merlindataexchange.configuration.DataExchangeSet;
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParameters;
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParametersBuilder;
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParameters;
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParametersBuilder;
import hec.ui.ProgressListener;

import gov.usbr.wq.merlindataexchange.DataExchangeEngine;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeEngineBuilder;
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeStatus;
import gov.usbr.wq.merlindataexchange.parameters.AuthenticationParametersBuilder;
import usbr.wat.plugins.actionpanel.extract.model.ExtractLoginInfo;
import usbr.wat.plugins.actionpanel.ui.ProgressListenerDialog;

import static gov.usbr.wq.merlindataexchange.io.MerlinDataExchangeTimeSeriesReader.TIMESERIES;
import static gov.usbr.wq.merlindataexchange.io.wq.MerlinDataExchangeProfileReader.PROFILE;

/**
 * @author mark
 *
 */
public class RunExtractAction
{

	private static final Logger LOGGER = Logger.getLogger(RunExtractAction.class.getName());
	private final Window _parent;
	private MerlinDataExchangeStatus _status;

	/**
	 * @param parent
	 */
	public RunExtractAction(Window parent)
	{
		super();
		_parent = parent;
	}

	/**
	 * @param parent
	 * @param tsParams
	 * @param profileParams
	 * @param selectedFiles
	 * @param infoString
	 * @param url
	 * @return
	 */
	public CompletableFuture<Void> extract(Window parent, MerlinTimeSeriesParameters tsParams, MerlinProfileParameters profileParams, List<Path> selectedFiles, String infoString, String url)
	{
		CompletableFuture<Void> retVal = new CompletableFuture<>();
		ProgressListener progress = createProgressListener();
		if(progress == null)
		{
			return retVal;
		}

		try
		{
			List<Path> timeSeriesFiles = getConfigsOfType(selectedFiles, TIMESERIES);
			List<Path> profileFiles = getConfigsOfType(selectedFiles, PROFILE);

			((ProgressListenerDialog)progress).setVisible(true);

			retVal = CompletableFuture.runAsync(() ->
			{
				boolean failed = false;
				if(!timeSeriesFiles.isEmpty())
				{
					DataExchangeEngine dataExchangeEngineTS = new MerlinDataExchangeEngineBuilder()
							.withConfigurationFiles(timeSeriesFiles)
							.withParameters(tsParams)
							.withProgressListener(progress)
							.build();
					CompletableFuture<MerlinDataExchangeStatus> futureTS = dataExchangeEngineTS.runExtract();
					_status = futureTS.join();
					failed = handleTSExtractStatus(parent, tsParams, profileParams, selectedFiles, infoString, url);
				}
				if(!failed && !profileFiles.isEmpty())
				{
					DataExchangeEngine dataExchangeEngineProfiles = new MerlinDataExchangeEngineBuilder()
							.withConfigurationFiles(profileFiles)
							.withParameters(profileParams)
							.withProgressListener(progress)
							.build();
					CompletableFuture<MerlinDataExchangeStatus> futureProfiles = dataExchangeEngineProfiles.runExtract();
					_status = futureProfiles.join();
					handleProfileExtractStatus(parent, tsParams, profileParams, selectedFiles, infoString, url);
				}
			});
		}
		catch (MerlinConfigParseException e)
		{
			JOptionPane.showMessageDialog(parent, e.getMessage(), "Invalid Config", JOptionPane.ERROR_MESSAGE);
			LOGGER.log(Level.CONFIG, e, () -> "Failed to parse config to determine parameter object to use");
		}
		return retVal;
	}

	private void handleProfileExtractStatus(Window parent, MerlinTimeSeriesParameters tsParams, MerlinProfileParameters profileParams, List<Path> selectedFiles, String infoString, String url)
	{
		if ( MerlinDataExchangeStatus.AUTHENTICATION_FAILURE  == _status )
		{
			redoExtractWithNewLoginInfo(parent, tsParams, profileParams, selectedFiles, infoString, url);
		}
		if ( MerlinDataExchangeStatus.FAILURE  == _status )
		{
			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
					"Extract of profiles Failed. See Log File for Details", "Extract Failed", JOptionPane.INFORMATION_MESSAGE));
		}
		else if ( MerlinDataExchangeStatus.PARTIAL_SUCCESS  == _status )
		{
			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
					"Extract didn't extract all profiles. See Log File for Details", "Partial Success", JOptionPane.INFORMATION_MESSAGE));
		}
	}

	private boolean handleTSExtractStatus(Window parent, MerlinTimeSeriesParameters tsParams, MerlinProfileParameters profileParams,
										  List<Path> selectedFiles, String infoString, String url)
	{
		boolean failed = false;
		if ( MerlinDataExchangeStatus.AUTHENTICATION_FAILURE  == _status )
		{
			redoExtractWithNewLoginInfo(parent, tsParams, profileParams, selectedFiles, infoString, url);
			failed = true;
		}
		else if ( MerlinDataExchangeStatus.FAILURE  == _status )
		{
			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
					"Extract of time series Failed. See Log File for Details", "Extract Failed", JOptionPane.INFORMATION_MESSAGE));
			failed = true;
		}
		else if ( MerlinDataExchangeStatus.PARTIAL_SUCCESS  == _status )
		{
			EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent,
					"Extract didn't extract all time series. See Log File for Details", "Partial Success", JOptionPane.INFORMATION_MESSAGE));
		}
		return failed;
	}

	private List<Path> getConfigsOfType(List<Path> selectedFiles, String type) throws MerlinConfigParseException
	{
		List<Path> retVal = new ArrayList<>();
		for(Path file : selectedFiles)
		{
			DataExchangeConfiguration config = MerlinDataExchangeParser.parseXmlFile(file);
			for(DataExchangeSet set : config.getDataExchangeSets())
			{
				String setType = set.getDataType();
				if(setType.equalsIgnoreCase(type))
				{
					retVal.add(file);
					break;
				}
			}
		}
		return retVal;
	}

	/**
	 * @param parent
	 * @param tsParams
	 * @param profileParams
	 * @param selectedFiles
	 * @param infoString
	 * @param url
	 */
	protected void redoExtractWithNewLoginInfo(Window parent,
											   MerlinTimeSeriesParameters tsParams, MerlinProfileParameters profileParams, List<Path> selectedFiles,
											   String infoString, String url)
	{
		
		if ( _status== MerlinDataExchangeStatus.AUTHENTICATION_FAILURE )
		{
			if ( ExtractLoginInfo.askForLoginInfo(parent, infoString))
			{
				String pw = ExtractLoginInfo.getPassword();
				if(pw == null)
				{
					pw = "";
				}
				MerlinTimeSeriesParameters params2 = new MerlinTimeSeriesParametersBuilder()
						.fromExistingParameters(tsParams)
		                .withAuthenticationParameters(new AuthenticationParametersBuilder()
		                        .forUrl(url)
		                        .setUsername(ExtractLoginInfo.getUserName())
		                        .andPassword(pw.toCharArray())
		                        .build())
		                .withStoreOption(tsParams.getStoreOption())
		                .withFPartOverride(tsParams.getFPartOverride())
		                .build();

				MerlinProfileParameters profileParams2 = new MerlinProfileParametersBuilder()
						.fromExistingParameters(profileParams)
						.withAuthenticationParameters(new AuthenticationParametersBuilder()
								.forUrl(url)
								.setUsername(ExtractLoginInfo.getUserName())
								.andPassword(pw.toCharArray())
								.build())
						.build();
				extract(parent, params2, profileParams2, selectedFiles, infoString, url);
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
	private ProgressListener createProgressListener()
	{
		if ( _parent == null )
		{
			return null;
		}
		return new ProgressListenerDialog(_parent, "Extract ");
	}

}
