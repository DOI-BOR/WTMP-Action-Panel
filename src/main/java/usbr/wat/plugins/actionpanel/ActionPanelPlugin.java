package usbr.wat.plugins.actionpanel;

import java.awt.EventQueue;                                             // Provides access to the AWT event dispatch infrastructure for scheduling tasks on the Event Dispatch Thread (EDT)
import javax.swing.JMenu;                                               // Swing menu component used to access and modify the application's Tools menu
import com.google.common.flogger.FluentLogger;                          // Structured, fluent logging API from Google for leveled logs and context
import com.rma.client.Browser;                                          // Application framework class that exposes the main browser frame and menus

import usbr.git.GitlabConfigurator;                                     // Utility for loading GitLab-related configuration and applying Git setup

import java.io.IOException;                                             // Defines IO-related exception types used when reading configuration resources
import org.jdom.JDOMException;                                          // Exception type thrown by JDOM when XML parsing fails at the library level
import usbr.git.XMLParseException;                                      // Custom exception indicating failures during application-specific XML parsing
import usbr.git.cli.GitCLIUnavailableException;                         // Exception indicating the Git command-line interface is not installed or not accessible

import usbr.wat.plugins.actionpanel.actions.ActionWindowAction;         // Swing action that opens the Actions window from the Tools menu
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;        // UI component that exposes configuration flags (for example, GIT_DASH_D_FLAG) related to simulations

/**
 * WTMP Action Panel plugin entry point and lifecycle manager.
 *
 * Initializes the plugin, optionally configures Git integration, adds
 * the Actions window entry to the Tools menu, and displays the window
 * when the main browser frame becomes ready.
 *
 */
public class ActionPanelPlugin
{
	/** Logger for emitting diagnostic and error information within this plugin. */
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	/** Singleton instance of the plugin for global access. */
	private static ActionPanelPlugin _instance;

	/** Reference to the Actions window that hosts plugin functionality. */
	private ActionsWindow _actionsWindow;

	/**
	 * Creates and initializes the Action Panel plugin.
	 *
	 * Sets the singleton instance, conditionally configures Git based on a flag,
	 * adds the Actions window launcher to the Tools menu, and schedules the
	 * Actions window to be displayed on the Event Dispatch Thread.
	 */
	public ActionPanelPlugin() {
		super();

		// Establish the singleton instance for global access to this plugin
		_instance = this;

		// If the configuration flag is set, perform Git configuration from the provided XML
		if ( Boolean.getBoolean(BaseSimulationGroupPanel.GIT_DASH_D_FLAG))
		{
			configureGitConfiguration();
		}

		// Add the menu item that opens the Actions window to the application Tools menu
		addToToolsMenu();

		// Ensure the Actions window is displayed once the UI thread is ready
		EventQueue.invokeLater(()->displayActionsWindow());
	}

	/**
	 * Adds the Actions window launcher to the application's Tools menu.
	 *
	 * This method checks whether the Tools menu exists before adding the action.
	 * It is safe to call regardless of menu availability.
	 */
	private void addToToolsMenu() {
		// Obtain the Tools menu from the main browser frame
		JMenu toolsMenu = Browser.getBrowserFrame().getToolsMenu();

		// Add the Actions window action if the Tools menu is available
		if ( toolsMenu != null ) {
			toolsMenu.add(new ActionWindowAction());
		}
	}

	/**
	 * Configures Git settings using the plugin's GitLab configuration file.
	 *
	 * Reads the GitLab configuration resource and applies Git configuration
	 * via the GitLab configurator. Errors are logged and do not crash the plugin.
	 *
	 * @implNote Failures are logged and the plugin continues to operate,
	 *           although Git-related features may not function as expected.
	 */
	private void configureGitConfiguration() {
		try {
			// Load the GitLab configuration XML from resources and perform Git setup
			GitlabConfigurator.prepareFromConfigurationFile(getClass().getResource("GitlabConfig.xml")).configureGit();

		} catch (IOException | InterruptedException e) {
			// IO or process execution issues (for example, invoking Git) are non-fatal but degrade functionality
			LOGGER.atWarning().withCause(e).log("Error setting Git configuration! Git operations may not work. Git CLI Communications Failure.");

		} catch (GitCLIUnavailableException e) {
			// Git executable is missing or inaccessible; prompt the user to ensure Git is installed and on PATH
			LOGGER.atSevere().withCause(e).log("Unable to execute Git CLI! Please ensure Git is installed and present on $PATH");

		} catch (JDOMException e) {
			// JDOM encountered a parsing problem with the configuration XML
			LOGGER.atWarning().withCause(e).log("Unable to parse WTMP Gitlab Configuration File! Git operations may not work. JDOM Exception");

		} catch (XMLParseException e) {
			// Application-specific XML parsing failure occurred while reading configuration
			LOGGER.atWarning().withCause(e).log("Unable to parse WTMP Gitlab Configuration File! Git operations may not work. XML Parsing failure");
		}
	}

	/**
	 * Displays the Actions window, deferring until the browser frame is visible if necessary.
	 *
	 * If the application browser frame is not yet visible, this method reschedules itself
	 * on the Event Dispatch Thread to try again later. Once visible, it constructs the
	 * window if needed, positions it relative to the frame, and shows it.
	 */
	public void displayActionsWindow() {
		// If the browser frame is not ready or visible, try again later on the EDT
		if ( Browser.getBrowserFrame()==null || !Browser.getBrowserFrame().isVisible()) {
			EventQueue.invokeLater(()->displayActionsWindow());
			return;
		}

		// Lazily construct the Actions window and position it relative to the browser frame
		if ( _actionsWindow == null ) {
			_actionsWindow = new ActionsWindow(Browser.getBrowserFrame());

			_actionsWindow.setLocationRelativeTo(Browser.getBrowserFrame());
		}

		// Make the Actions window visible to the user
		_actionsWindow.setVisible(true);
	}

	/**
	 * Returns the Actions window instance managed by this plugin.
	 *
	 * @return the active ActionsWindow instance, or null if it has not yet been created
	 */
	public ActionsWindow getActionsWindow() {
		// Provide direct access to the Actions window for other components
		return _actionsWindow;
	}

	/**
	 * Launches the plugin as a standalone entry point.
	 *
	 * In typical application usage, the plugin will be constructed by the host.
	 * This main method facilitates direct invocation for testing or isolated runs.
	 *
	 * @param args command-line arguments (unused)
	 */
	public static void main(String[] args) {
		// Create the plugin instance, which performs initialization and displays the UI
		new ActionPanelPlugin();
	}

	/**
	 * Provides the singleton plugin instance.
	 *
	 * @return the ActionPanelPlugin singleton instance
	 */
	public static ActionPanelPlugin getInstance() {
		// Return the globally accessible plugin instance
		return _instance;
	}
}