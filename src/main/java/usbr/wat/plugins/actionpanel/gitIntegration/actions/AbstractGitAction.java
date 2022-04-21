/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.actions;

import java.awt.Cursor;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;

import hec.io.ProcessOutputLine;
import hec.io.ProcessOutputReader;

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
	protected static final String GIT_PYTHON_EXE = "WAT_GIT_Tool_v2.exe";
	
	protected static final String LOCAL_FOLDER = "--folder";
	
	protected static final String DO_NOTHING = "--donothing";


	public static final Object STUDY_MODULE = "Study";
	
	




	
	private Window _parent;


	private List<ProcessOutputLine> _output;
	
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
		boolean echoOutput = Boolean.getBoolean(DEBUG_OUTPUT_PROP);
		
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
			if ( rv != 0 )
			{
				String msg = getErrorMessage(cmd, _output);
				_logger.info(cmd.get(0)+" exit code="+rv);
				_logger.info(msg);
				
				// this might make too big a message and needs put in a panel with a text area
				if ( _parent != null )
				{
					
					JOptionPane.showMessageDialog(_parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
				}
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
	 * 
	 * @param output2 
	 * @param outputReader
	 * @param errorReader
	 * @return
	 */
	private static String getErrorMessage(List<String>cmd, List<ProcessOutputLine> output)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Error Running: ");
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

}
