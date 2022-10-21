/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;

import hec.io.ProcessOutputLine;
import hec.io.ProcessOutputReader;

import rma.swing.ButtonCmdPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTextArea;
import rma.util.RMAIO;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractGitAction extends AbstractAction
{
	protected Logger _logger = Logger.getLogger(AbstractGitAction.class.getName());

	
	public static final String DO_NOTHING_PROP = "UsbrGit.DoNothing";
	public static final String DEBUG_OUTPUT_PROP = "UsbrGit.Debug";
	
	public static final String ALL_MODULES     = "--all";
	public static final String MAIN_MODULE    = "--main";
	public static final String SUB_MODULE      = "--submodule";
	
	private static final String PYTHON_EXE_PROP = "UsbrGit.Python.exe";
	
	private static final String EXE_FOLDER = "../tools"; // folder the .exe lives in relative to where the WAT.exe is
	public static final String GIT_PYTHON_EXE = "WAT_GIT_Tool_v2.exe";
	
	public static final String LOCAL_FOLDER = "--folder";
	
	protected static final String DO_NOTHING = "--donothing";


	public static final String STUDY_MODULE = "Study";
	
	




	
	private Window _parent;


	private List<ProcessOutputLine> _output;


	private boolean _showFailedCallMsg;
	
	/**
	 * @param string
	 */
	public AbstractGitAction(String name,Window parent )
	{
		super(name);
		_parent= parent;
	}
	
	public Window getParent()
	{
		return _parent;
	}
	/**
	 * @param cmd
	 * @return
	 */
	protected boolean callGit(List<String> cmd)
	{
		if ( _parent != null )
		{
			((RootPaneContainer)_parent).getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		try
		{
		String exe = getExe();
		cmd.add(0,exe);
		
		boolean echoOutput = Boolean.getBoolean(DEBUG_OUTPUT_PROP);
		if ( echoOutput)
		{
			_logger.setLevel(Level.FINE);
		}
		else
		{
			_logger.setLevel(Level.INFO);
		}
		if ( Boolean.getBoolean(DO_NOTHING_PROP))
		{
			cmd.add(2, DO_NOTHING);
		}
	
		_logger.fine("Command is:"+cmd);
	
		ProcessBuilder builder = new ProcessBuilder(cmd);
		Process proc;
		try
		{
			proc = builder.start();
		}
		catch (IOException e)
		{
			_logger.info("Failed to launch Git Command " + cmd+" Error:"+e);
			JOptionPane.showMessageDialog(_parent, "Failed to launch Git command " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		InputStream iStream = proc.getInputStream();
		InputStream eStream = proc.getErrorStream();
		BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream));
		BufferedReader eReader = new BufferedReader(new InputStreamReader(iStream));
		
		_output = new Vector<>();
		ProcessOutputReader outputReader = new ProcessOutputReader(iReader, _output,"git stdout", echoOutput,false);
		ProcessOutputReader errorReader = new ProcessOutputReader(eReader, _output,"git stderr", echoOutput,true);
		
	
		try
		{
			int rv = proc.waitFor();
			if ( echoOutput)
			{
				System.out.println(cmd.get(0)+" exit code="+rv);
			}
			if ( rv != 0 )
			{
				showErrorMsg(cmd, _output);
				
				return false;
			}
			else
			{
				return true;
			}
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
			}
			if ( outputReader != null )
			{
				outputReader.close();
			}
			if ( errorReader != null )
			{
				errorReader.close();
			}
		}
		}
		finally
		{
			if ( _parent != null )
			{
				((RootPaneContainer)_parent).getContentPane().setCursor(Cursor.getDefaultCursor());
			}
			
		}
		
		
		return false;
	}
	/**
	 * @param cmd
	 * @param output
	 */
	protected void showErrorMsg(List<String> cmd, List<ProcessOutputLine> output)
	{
		String msg = getErrorMessage("Error Running:", cmd, _output);
		_logger.info(msg);
		if ( !_showFailedCallMsg )
		{
			return;
		}
		// this might make too big a message and needs put in a panel with a text area
		if ( _parent != null )
		{
			showErrorMsg("Error", msg);
			
		}
	}
	/**
	 * @param msg
	 */
	protected void showErrorMsg(String title, String msg)
	{
		RmaJTextArea textArea = new RmaJTextArea(5, 80);
		textArea.setEditable(false);
		JScrollPane sp = new JScrollPane(textArea);
		textArea.setText(msg);
	
		RmaJDialog dlg = new RmaJDialog(_parent, title, true);
		dlg.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		dlg.getContentPane().add(sp, gbc);
		
		ButtonCmdPanel cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		dlg.getContentPane().add(cmdPanel, gbc);
	
		cmdPanel.addCmdPanelListener(e->dlg.setVisible(false));
		
		dlg.pack();
		dlg.setLocationRelativeTo(_parent);
		dlg.setVisible(true);
		//JOptionPane.showMessageDialog(_parent, sp, title, JOptionPane.ERROR_MESSAGE);
	}

	public void setShowFailedCallMessage(boolean showMsg)
	{
		_showFailedCallMsg = showMsg;
	}

	/**
	 * 
	 * @param output2 
	 * @param outputReader
	 * @param errorReader
	 * @return
	 */
	protected static String getErrorMessage(String header, List<String>cmd, List<ProcessOutputLine> output)
	{
		StringBuilder builder = new StringBuilder();
		if ( header != null )
		{
			builder.append(header);//"Error Running: ");
		}
		builder.append(cmd);
		builder.append("\n");
		for (int i = 0;i < output.size(); i++ )
		{
			builder.append("\n");
			builder.append(output.get(i));
		}
		return builder.toString();
	}
	/**
	 * @return
	 */
	private static String getExe()
	{
		String exe = System.getProperty(PYTHON_EXE_PROP);
		if ( exe == null )
		{
			String folder = System.getProperty("user.dir");// where the WAT is running from

			folder = RMAIO.concatPath(folder, EXE_FOLDER);
			exe = RMAIO.concatPath(folder, GIT_PYTHON_EXE);
		}
		
		return exe;
	}
	
	/**
	 * @param comments
	 * @return
	 */
	protected String quoteString(String aString)
	{
		if ( aString.indexOf(' ')== -1)
		{
			return aString;
		}
		StringBuilder builder = new StringBuilder("\"");
		builder.append(aString);
		builder.append("\"");
		return builder.toString();
	}
	
	public List<ProcessOutputLine>getOutput()
	{
		return _output;
	}

	/**
	 * @param output
	 * @return
	 */
	protected static List<String> getOutputLines(List<ProcessOutputLine> output)
	{
		return output.stream().map(l->l.getLine()).collect(Collectors.toList());
	}
}
