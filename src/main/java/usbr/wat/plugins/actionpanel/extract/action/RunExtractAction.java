package usbr.wat.plugins.actionpanel.extract.action;

import java.awt.EventQueue;                      // Swing event dispatch queue; used to post dialog messages safely onto the EDT
import java.awt.Window;                          // AWT Window used as the parent for progress and message dialogs
import java.nio.file.Path;                       // Represents a file system path to a Merlin configuration XML file
import java.util.ArrayList;                      // Resizable-array List used to accumulate filtered configuration file paths
import java.util.List;                           // Ordered collection interface for configuration file path lists
import java.util.concurrent.CompletableFuture;   // Asynchronous computation handle used to run the extract off the EDT and signal completion
import java.util.logging.Level;                  // Log level constants; used to classify configuration-parse exceptions
import java.util.logging.Logger;                 // JDK logger for recording diagnostic and error messages

import javax.swing.JOptionPane;                  // Provides standard error and information dialog boxes shown to the user

import gov.usbr.wq.merlindataexchange.MerlinConfigParseException;                    // Checked exception thrown when a Merlin XML configuration file cannot be parsed
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeParser;                      // Parses Merlin XML configuration files into DataExchangeConfiguration objects
import gov.usbr.wq.merlindataexchange.configuration.DataExchangeConfiguration;       // Represents the parsed content of a single Merlin configuration XML file
import gov.usbr.wq.merlindataexchange.configuration.DataExchangeSet;                 // Represents a single data exchange set within a configuration, carrying its data type
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParameters;            // Immutable parameter object specifying credentials and options for a profile extract
import gov.usbr.wq.merlindataexchange.parameters.MerlinProfileParametersBuilder;     // Builder for constructing MerlinProfileParameters instances
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParameters;         // Immutable parameter object specifying credentials and options for a time-series extract
import gov.usbr.wq.merlindataexchange.parameters.MerlinTimeSeriesParametersBuilder;  // Builder for constructing MerlinTimeSeriesParameters instances
import hec.ui.ProgressListener;                                                       // HEC interface for receiving progress updates during a long-running operation

import gov.usbr.wq.merlindataexchange.DataExchangeEngine;                            // Executes a Merlin data extract for a configured set of files and parameters
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeEngineBuilder;               // Builder for constructing DataExchangeEngine instances
import gov.usbr.wq.merlindataexchange.MerlinDataExchangeStatus;                      // Enum representing the outcome of an extract run (SUCCESS, FAILURE, PARTIAL_SUCCESS, AUTHENTICATION_FAILURE)
import gov.usbr.wq.merlindataexchange.parameters.AuthenticationParametersBuilder;    // Builder for constructing authentication parameters (URL, username, password)

import usbr.wat.plugins.actionpanel.extract.model.ExtractLoginInfo;      // Utility class for prompting the user for login credentials and caching the result
import usbr.wat.plugins.actionpanel.ui.ProgressListenerDialog;           // Dialog implementation of ProgressListener that shows extract progress to the user

import static gov.usbr.wq.merlindataexchange.io.MerlinDataExchangeTimeSeriesReader.TIMESERIES; // String constant identifying a time-series data exchange type in configuration files
import static gov.usbr.wq.merlindataexchange.io.wq.MerlinDataExchangeProfileReader.PROFILE;    // String constant identifying a profile data exchange type in configuration files

/**
 * Action class responsible for executing a Merlin data extract operation within
 * the WTMP Action Panel.
 *
 * Accepts a list of Merlin XML configuration files and splits them into two groups
 * based on their declared data type: time-series files (TIMESERIES) and profile
 * files (PROFILE). Each group is processed by a separately constructed
 * DataExchangeEngine running asynchronously via CompletableFuture.
 *
 * The time-series extract is always attempted first. If it fails due to an
 * authentication error, the entire extract is re-attempted after prompting the
 * user for new credentials via ExtractLoginInfo. If the time-series extract fails
 * for any other reason, the profile extract is skipped. Non-fatal outcomes
 * (PARTIAL_SUCCESS) are surfaced to the user as informational dialogs on the EDT.
 *
 * A ProgressListenerDialog is created before the asynchronous work begins to
 * provide visual feedback during the extract. If no parent window is available,
 * the extract is aborted immediately.
 */
public class RunExtractAction {

	// Logger for this class; used to record configuration-parse failures and other diagnostics
	private static final Logger LOGGER = Logger.getLogger(RunExtractAction.class.getName());

	// The parent window used to anchor progress dialogs and message boxes
	private final Window _parent;

	// The outcome of the most recent extract run; updated after each engine completes
	private MerlinDataExchangeStatus _status;

	/**
	 * Constructs a RunExtractAction with the given parent window.
	 *
	 * The parent window is used as the owner for the ProgressListenerDialog
	 * and for any message dialogs shown during or after the extract.
	 *
	 * @param parent the Window that will own all dialogs created by this action
	 */
	public RunExtractAction(Window parent) {
		// Invoke the default Object constructor
		super();

		// Store the parent window reference for dialog ownership
		_parent = parent;
	}

	/**
	 * Executes a Merlin data extract asynchronously for the provided configuration files.
	 *
	 * Separates the given file list into time-series and profile configuration groups,
	 * creates a ProgressListenerDialog for user feedback, and runs both extracts
	 * on a background thread via CompletableFuture. Time-series files are processed
	 * first; if the time-series extract fails, the profile extract is skipped.
	 * Authentication failures in either phase trigger a credential re-prompt and retry.
	 *
	 * Returns an already-completed (but empty) CompletableFuture immediately if no
	 * progress listener can be created (i.e., no parent window is set), or re-raises
	 * configuration parse errors as a JOptionPane error dialog.
	 *
	 * @param parent        the Window to use as owner for any dialogs shown during the extract
	 * @param tsParams      the time-series extract parameters (credentials, store options, F-part)
	 * @param profileParams the profile extract parameters (credentials)
	 * @param selectedFiles the list of Merlin XML configuration file paths to process
	 * @param infoString    a descriptive string shown in the login prompt if re-authentication is needed
	 * @param url           the Merlin service URL used when rebuilding authentication parameters
	 * @return a CompletableFuture that completes when both extract phases finish
	 */
	public CompletableFuture<Void> extract(Window parent, MerlinTimeSeriesParameters tsParams,
	                                       MerlinProfileParameters profileParams, List<Path> selectedFiles, String infoString, String url) {
		// Create a CompletableFuture that will be completed when the async work finishes
		CompletableFuture<Void> retVal = new CompletableFuture<>();

		// Create the progress dialog; returns null if no parent window is available
		ProgressListener progress = createProgressListener();
		if (progress == null) {
			// Cannot show progress without a parent window; return the incomplete future immediately
			return retVal;
		}

		try {
			// Partition the selected files into time-series and profile configuration groups
			List<Path> timeSeriesFiles = getConfigsOfType(selectedFiles, TIMESERIES);
			List<Path> profileFiles = getConfigsOfType(selectedFiles, PROFILE);

			// Make the progress dialog visible before starting the background work
			((ProgressListenerDialog) progress).setVisible(true);

			// Run both extract phases asynchronously on the common fork-join pool
			retVal = CompletableFuture.runAsync(() ->
			{
				boolean failed = false;

				// Process time-series files first if any were found
				if (!timeSeriesFiles.isEmpty()) {
					// Build the time-series extract engine with the config files, parameters, and progress listener
					DataExchangeEngine dataExchangeEngineTS = new MerlinDataExchangeEngineBuilder()
							.withConfigurationFiles(timeSeriesFiles)
							.withParameters(tsParams)
							.withProgressListener(progress)
							.build();

					// Run the time-series extract and block until it completes
					CompletableFuture<MerlinDataExchangeStatus> futureTS = dataExchangeEngineTS.runExtract();
					_status = futureTS.join();

					// Inspect the result; set failed=true on auth failure or hard failure
					failed = handleTSExtractStatus(parent, tsParams, profileParams, selectedFiles, infoString, url);
				}

				// Only process profile files if the time-series phase succeeded (or was skipped)
				if (!failed && !profileFiles.isEmpty()) {
					// Build the profile extract engine with its own config files and parameters
					DataExchangeEngine dataExchangeEngineProfiles = new MerlinDataExchangeEngineBuilder()
							.withConfigurationFiles(profileFiles)
							.withParameters(profileParams)
							.withProgressListener(progress)
							.build();

					// Run the profile extract and block until it completes
					CompletableFuture<MerlinDataExchangeStatus> futureProfiles = dataExchangeEngineProfiles.runExtract();
					_status = futureProfiles.join();

					// Inspect the profile result; auth failures trigger a re-prompt
					handleProfileExtractStatus(parent, tsParams, profileParams, selectedFiles, infoString, url);
				}
			});
		} catch (MerlinConfigParseException e) {
			// Show a modal error dialog if the configuration files cannot be parsed
			JOptionPane.showMessageDialog(parent, e.getMessage(), "Invalid Config", JOptionPane.ERROR_MESSAGE);

			// Log the parse failure at CONFIG level with the exception for diagnostic purposes
			LOGGER.log(Level.CONFIG, e, () -> "Failed to parse config to determine parameter object to use");
		}

		return retVal;
	}

	/**
	 * Inspects the profile extract status and responds appropriately.
	 *
	 * If the status indicates an authentication failure, prompts the user for new
	 * credentials and retries the full extract. For a hard FAILURE, shows an error
	 * dialog on the EDT. For PARTIAL_SUCCESS, shows an informational dialog on the EDT.
	 *
	 * @param parent        the Window to use as owner for any dialogs
	 * @param tsParams      the time-series parameters to pass if a retry is needed
	 * @param profileParams the profile parameters to pass if a retry is needed
	 * @param selectedFiles the configuration files to pass if a retry is needed
	 * @param infoString    the description string shown in the login prompt
	 * @param url           the Merlin service URL used when rebuilding credentials
	 */
	private void handleProfileExtractStatus(Window parent, MerlinTimeSeriesParameters tsParams,
	                                        MerlinProfileParameters profileParams, List<Path> selectedFiles, String infoString, String url) {
		// Re-prompt for credentials and retry if authentication failed
		if (MerlinDataExchangeStatus.AUTHENTICATION_FAILURE == _status) {
			redoExtractWithNewLoginInfo(parent, tsParams, profileParams, selectedFiles, infoString, url);
		}

		// Notify the user of a hard failure via an EDT-safe dialog
		if (MerlinDataExchangeStatus.FAILURE == _status) {
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"Extract of profiles Failed. See Log File for Details", "Extract Failed", JOptionPane.INFORMATION_MESSAGE));
		}
		// Notify the user that only some profiles were extracted
		else if (MerlinDataExchangeStatus.PARTIAL_SUCCESS == _status) {
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"Extract didn't extract all profiles. See Log File for Details", "Partial Success", JOptionPane.INFORMATION_MESSAGE));
		}
	}

	/**
	 * Inspects the time-series extract status and responds appropriately.
	 *
	 * Returns true if the extract failed in a way that should prevent the subsequent
	 * profile extract from running (authentication failure or hard failure). Returns
	 * false for success or partial success.
	 *
	 * Authentication failures trigger a credential re-prompt and retry. Hard failures
	 * and partial successes surface an EDT-safe informational dialog to the user.
	 *
	 * @param parent        the Window to use as owner for any dialogs
	 * @param tsParams      the time-series parameters to pass if a retry is needed
	 * @param profileParams the profile parameters to pass if a retry is needed
	 * @param selectedFiles the configuration files to pass if a retry is needed
	 * @param infoString    the description string shown in the login prompt
	 * @param url           the Merlin service URL used when rebuilding credentials
	 * @return true if the time-series extract failed and the profile extract should be skipped; false otherwise
	 */
	private boolean handleTSExtractStatus(Window parent, MerlinTimeSeriesParameters tsParams,
	                                      MerlinProfileParameters profileParams, List<Path> selectedFiles, String infoString, String url) {
		boolean failed = false;

		if (MerlinDataExchangeStatus.AUTHENTICATION_FAILURE == _status) {
			// Re-prompt for credentials and retry the full extract
			redoExtractWithNewLoginInfo(parent, tsParams, profileParams, selectedFiles, infoString, url);

			// Authentication failure should block the profile extract phase
			failed = true;
		} else if (MerlinDataExchangeStatus.FAILURE == _status) {
			// Notify the user of the hard failure via an EDT-safe dialog
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"Extract of time series Failed. See Log File for Details", "Extract Failed", JOptionPane.INFORMATION_MESSAGE));

			// Hard failure should also block the profile extract phase
			failed = true;
		} else if (MerlinDataExchangeStatus.PARTIAL_SUCCESS == _status) {
			// Partial success is non-fatal; notify the user but allow the profile phase to proceed
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"Extract didn't extract all time series. See Log File for Details", "Partial Success", JOptionPane.INFORMATION_MESSAGE));
		}

		return failed;
	}

	/**
	 * Filters the given list of configuration file paths to those that contain at
	 * least one DataExchangeSet whose data type matches the specified type string.
	 *
	 * Each file is parsed using MerlinDataExchangeParser. If any set within the
	 * parsed configuration has a data type equal (case-insensitively) to the target
	 * type, the file is included in the result and the remaining sets are skipped.
	 *
	 * @param selectedFiles the full list of configuration file paths to inspect
	 * @param type          the data type string to match (e.g., TIMESERIES or PROFILE)
	 * @return a List of Path objects whose configurations contain a set of the given type
	 * @throws MerlinConfigParseException if any configuration file cannot be parsed
	 */
	private List<Path> getConfigsOfType(List<Path> selectedFiles, String type) throws MerlinConfigParseException {
		List<Path> retVal = new ArrayList<>();

		for (Path file : selectedFiles) {
			// Parse the XML configuration file into a DataExchangeConfiguration object
			DataExchangeConfiguration config = MerlinDataExchangeParser.parseXmlFile(file);

			for (DataExchangeSet set : config.getDataExchangeSets()) {
				// Check whether this set's data type matches the target type (case-insensitive)
				String setType = set.getDataType();
				if (setType.equalsIgnoreCase(type)) {
					// Include the file and skip remaining sets to avoid adding it more than once
					retVal.add(file);
					break;
				}
			}
		}

		return retVal;
	}

	/**
	 * Prompts the user for new login credentials and retries the full extract.
	 *
	 * Only executes if the current status is AUTHENTICATION_FAILURE. Displays the
	 * ExtractLoginInfo credential dialog; if the user confirms, rebuilds both the
	 * time-series and profile parameter objects with the new credentials and
	 * delegates back to extract() for another attempt.
	 *
	 * A null password from ExtractLoginInfo is replaced with an empty string before
	 * being converted to a char array to avoid NullPointerException in the builder.
	 *
	 * @param parent        the Window to use as owner for the login dialog
	 * @param tsParams      the existing time-series parameters to copy non-credential settings from
	 * @param profileParams the existing profile parameters to copy non-credential settings from
	 * @param selectedFiles the configuration files to re-extract after re-authentication
	 * @param infoString    a descriptive string shown in the login dialog header
	 * @param url           the Merlin service URL used in the new authentication parameters
	 */
	protected void redoExtractWithNewLoginInfo(Window parent,
	                                           MerlinTimeSeriesParameters tsParams, MerlinProfileParameters profileParams,
	                                           List<Path> selectedFiles, String infoString, String url) {
		// Guard: only proceed if the failure was specifically an authentication failure
		if (_status == MerlinDataExchangeStatus.AUTHENTICATION_FAILURE) {
			// Prompt the user for a username and password; returns true if the user confirmed
			if (ExtractLoginInfo.askForLoginInfo(parent, infoString)) {
				// Retrieve the entered password, defaulting to empty string if null
				String pw = ExtractLoginInfo.getPassword();
				if (pw == null) {
					pw = "";
				}

				// Build new time-series parameters copying all existing settings but replacing credentials
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

				// Build new profile parameters copying all existing settings but replacing credentials
				MerlinProfileParameters profileParams2 = new MerlinProfileParametersBuilder()
						.fromExistingParameters(profileParams)
						.withAuthenticationParameters(new AuthenticationParametersBuilder()
								.forUrl(url)
								.setUsername(ExtractLoginInfo.getUserName())
								.andPassword(pw.toCharArray())
								.build())
						.build();

				// Re-run the extract with the updated credentials
				extract(parent, params2, profileParams2, selectedFiles, infoString, url);
			}
		}
	}

	/**
	 * Returns the status of the most recently completed extract phase.
	 *
	 * Reflects the outcome of the last DataExchangeEngine that completed (either
	 * time-series or profile). Returns null if no extract has been run yet.
	 *
	 * @return the MerlinDataExchangeStatus from the last extract phase, or null if none
	 */
	public MerlinDataExchangeStatus getExtractStatus() {
		return _status;
	}

	/**
	 * Creates and returns a ProgressListenerDialog for displaying extract progress.
	 *
	 * Returns null if no parent window has been set, since the dialog requires
	 * an owner window to be displayed. A null return causes extract() to abort
	 * immediately without running the extract.
	 *
	 * @return a new ProgressListenerDialog owned by _parent, or null if _parent is null
	 */
	private ProgressListener createProgressListener() {
		// Cannot create a dialog without a parent window to own it
		if (_parent == null) {
			return null;
		}

		// Create and return the progress dialog with the label "Extract "
		return new ProgressListenerDialog(_parent, "Extract ");
	}
}
