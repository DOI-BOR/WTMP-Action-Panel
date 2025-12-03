/*
* Copyright 2022 United States Bureau of Reclamation (USBR).
* United States Department of the Interior
* All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
* Source may not be released without written approval
* from USBR
*/
package usbr.wat.plugins.actionpanel;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JMenu;

import com.google.common.flogger.FluentLogger;
import com.rma.client.Browser;

import org.jdom.JDOMException;
import usbr.git.GitlabConfigurator;
import usbr.git.XMLParseException;
import usbr.git.cli.GitCLIUnavailableException;
import usbr.wat.plugins.actionpanel.actions.ActionWindowAction;
import usbr.wat.plugins.actionpanel.ui.BaseSimulationGroupPanel;

/**
 * @author Mark Ackerman
 *
 */
public class ActionPanelPlugin
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	private static ActionPanelPlugin _instance;
	
	private ActionsWindow _actionsWindow;
	public ActionPanelPlugin()
	{
		super();
		_instance = this;
		if ( Boolean.getBoolean(BaseSimulationGroupPanel.GIT_DASH_D_FLAG))
		{
			configureGitConfiguration();
		}
		addToToolsMenu();
		EventQueue.invokeLater(()->displayActionsWindow());
	}

	/**
	 * 
	 */
	private void addToToolsMenu()
	{
		JMenu toolsMenu = Browser.getBrowserFrame().getToolsMenu();
		if ( toolsMenu != null )
		{
			toolsMenu.add(new ActionWindowAction());
		}
	}

	private void configureGitConfiguration() {
		try {
			GitlabConfigurator.prepareFromConfigurationFile(getClass().getResource("GitlabConfig.xml")).configureGit();
		} catch (IOException | InterruptedException e) {
			LOGGER.atWarning().withCause(e).log("Error setting Git configuration! Git operations may not work. Git CLI Communications Failure.");
		} catch (GitCLIUnavailableException e) {
			LOGGER.atSevere().withCause(e).log("Unable to execute Git CLI! Please ensure Git is installed and present on $PATH");
		} catch (JDOMException e) {
			LOGGER.atWarning().withCause(e).log("Unable to parse WTMP Gitlab Configuration File! Git operations may not work. JDOM Exception");
		} catch (XMLParseException e) {
			LOGGER.atWarning().withCause(e).log("Unable to parse WTMP Gitlab Configuration File! Git operations may not work. XML Parsing failure");
		}
	}
	
	public void displayActionsWindow()
	{
		if ( Browser.getBrowserFrame()==null || !Browser.getBrowserFrame().isVisible())
		{
			EventQueue.invokeLater(()->displayActionsWindow());
			return;
		}
		if ( _actionsWindow == null )
		{
			_actionsWindow = new ActionsWindow(Browser.getBrowserFrame());
			_actionsWindow.setLocationRelativeTo(Browser.getBrowserFrame());
		}
		
		_actionsWindow.setVisible(true);
	}
	/**
	 * 
	 * @return
	 */
	public ActionsWindow getActionsWindow()
	{
		return _actionsWindow;
	}

	public static void main(String[] args)
	{
		new ActionPanelPlugin();
	}
	
	public static ActionPanelPlugin getInstance()
	{
		return _instance;
	}
}
