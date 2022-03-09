/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import rma.util.RMAIO;

/**
 * @author Mark Ackerman
 *
 */
public class GitCredentialManager
{
	private static Logger _logger = Logger.getLogger(GitCredentialManager.class.getName());
	
	private static final String GIT_CMD = "git";
	private static final String CREDENTIAL = "credential";
	// git credential options
	private static final String FILL = "fill";
	private static final String APPROVE = "approve";
	private static final String REJECT = "reject";
	
	// 
	private static final String URL = "url=";
	private static final String USERNAME = "username=";
	private static final String PASSWORD = "password=";
	
	private static final String USERNAME_FOR = "Username for ";
	private static final String PASSWORD_FOR = "Password for ";


	private GitCredentialManager()
	{
		super();
	}

	/**
	 * The workflow should be
	 * git credential fill
	 * If it successfully gives you back a user/password, you use it to authenticate
	 * If successful, git credential approve with the output of fill
	 * If failed, git credential reject with the output of fill
	 * 
	 * @return the credentials of the user for the url specified
	 */
	public static CredentialsProvider init(String url)
	{
		UserPassword up = new UserPassword();
		
		if ( runFill(url, up))
		{
			runApprove(url, up);
			UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(up.userName, up.password);
		}
		else
		{
			runReject(url, up);
		}
		return null;
	}
	
	
	/**
	 * @param url2
	 * @param up
	 */
	private static void runReject(String url2, UserPassword up)
	{
		// TODO Auto-generated method stub
		System.out.println("runReject TODO implement me");
		
	}

	/**
	 * @param url2
	 * @param up
	 */
	private static void runApprove(String url2, UserPassword up)
	{
		// TODO Auto-generated method stub
		System.out.println("runApprove TODO implement me");
		
	}

	/**
	 * @param url
	 * @param up
	 * @return
	 */
	private static boolean runFill(String url, UserPassword up)
	{
		return runGit(url, up, FILL);
	}


	/**
	 * @param url2
	 * @param up
	 * @param fill2
	 */
	private static boolean runGit(String url, UserPassword up, String credentialCmd)
	{
		List<String>cmds = new ArrayList<>();
		cmds.add(GIT_CMD);
		cmds.add(CREDENTIAL);
		cmds.add(credentialCmd);
		
		_logger.fine("cmd is:"+cmds);
		
		ProcessBuilder builder = new ProcessBuilder(cmds);
		Process proc;
		try
		{
			proc = builder.start();
		}
		catch (IOException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return false;
		}
		InputStream inputStream = proc.getInputStream();
		OutputStream outputStream = proc.getOutputStream();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		try
		{
			writer.write(URL);
			writer.write(url);
			writer.newLine();
			writer.newLine();
			writer.newLine();
		}
		catch (IOException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return false;
		}
		
		String line, userName = null, password = null;
		try
		{
			while ( (line = reader.readLine())!= null )
			{
				_logger.fine("readLine is:"+line);
				if ( line.startsWith(USERNAME_FOR))
				{
					proc.destroyForcibly();
					return false;
				}
				else if ( line.startsWith(USERNAME))
				{
					userName = RMAIO.getParam(line, "=");
				}
				else if ( line.startsWith(PASSWORD))
				{
					password = RMAIO.getParam(line, "=");
				}
				if ( userName != null && password != null )
				{
					_logger.fine("found username and password for "+url);
					up.userName = userName;
					up.password = password;
					return true;
				}
			}
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		finally
		{
			proc.destroyForcibly();
			try
			{
				proc.waitFor(3, TimeUnit.SECONDS);
				if ( writer != null )
				{
					try
					{
						writer.close();
					}
					catch (IOException e)
					{
					}
				}
				if ( reader != null )
				{
					try
					{
						reader.close();
					}
					catch (IOException e)
					{
					}
				}
			}
			catch (InterruptedException e)
			{
			}
		
		}
		return false;
		
		
	}

	public static void main(String[] args)
	{
		UserPassword up = new UserPassword();
		runFill("https://gitlab.rmanet.app/RMA/usbr-water-quality/usbr-wq.git", up);
		System.out.println("main:username = "+up.userName);
	}

	private static class UserPassword
	{
		String userName;
		String password;
		
	}
}
