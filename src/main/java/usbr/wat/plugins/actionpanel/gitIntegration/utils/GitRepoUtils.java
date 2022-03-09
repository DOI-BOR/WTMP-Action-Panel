/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.rma.client.BrowserPreferences;
import com.rma.io.FileManagerImpl;

import hec2.wat.WAT;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.DownloadStudyAction;
import usbr.wat.plugins.actionpanel.gitIntegration.actions.ShowChangesActions;
import usbr.wat.plugins.actionpanel.gitIntegration.model.RepoInfo;

/**
 * 
 * class to handle the repo list displayed in the UI
 * 
 * @author Mark Ackerman
 *
 */
public class GitRepoUtils
{
	public static final String GIT_FOLDER = ".git";
	
	private static final String LOCAL_FOLDER = "LocalFolder";
	private static final String REPO_URL = "RepoUrl";
	
	
	// entries from the .git info 
	private static final String REMOTE_SECTION = "remote";

	private static final String ORIGIN_SUB_SECTION = "origin";

	private static final String URL_NAME = "url";

	
	private static CredentialsProvider _credentials;
	
	public static List<RepoInfo>_repos = new ArrayList<>();

	private GitRepoUtils()
	{
		
	}
	
	public static List<RepoInfo> getReposList()
	{
		if ( _repos.size() > 0 ) 
		{
			return _repos;
		}
		Preferences repoNode = getRepoNode();
		
	
		
		String[] repoNames = null;
		try
		{
			repoNames = repoNode.childrenNames();
		}
		catch (BackingStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return _repos;
		}
		Preferences kidNode;
		RepoInfo info;
		String localFolder, repoUrl;
		for (int i = 0;i < repoNames.length; i++ )
		{
			kidNode = repoNode.node(repoNames[i]);
			localFolder = kidNode.get(LOCAL_FOLDER, null);
			if ( localFolder == null )
			{
				continue;
			}
			repoUrl = kidNode.get(REPO_URL, null);
			info = new RepoInfo();
			info.setName(repoNames[i]);
			info.setLocalPath(localFolder);
			info.setSourceUrl(repoUrl);
			//getRepoInfo(info);
			_repos.add(info);
			
		}
		
		
		return _repos;
	}
	
	public static boolean initCreditials()
	{
		if ( _credentials == null )
		{
			//_credentials = GitCredentialManager.init();
		}
		return _credentials != null;
	}
	/**
	 * @return
	 */
	private static Preferences getRepoNode()
	{
		BrowserPreferences prefs = WAT.getBrowserFrame().getPreferences();
		
		Preferences repoNode = prefs.getNode("gitrepos");
		
		return repoNode;
	}

	/**
	 * checks to see if we have local folder as a git repo or there's a .git folder in it
	 * @param localFolder
	 * @return
	 */
	public static boolean hasGitRepo(String localFolder)
	{
		if ( localFolder == null )
		{
			return false;
		}
		List<RepoInfo>repos = getReposList();
		RepoInfo info;
		for (int i = 0;i < repos.size();i++ )
		{
			info = repos.get(i);
			if (RMAIO.pathsEqual(localFolder, info.getLocalPath()))
			{
				return true;
			}
		}
		String gitFolder = RMAIO.concatPath(localFolder, GIT_FOLDER);
		return FileManagerImpl.getFileManager().fileExists(gitFolder);
	}
	/**
	 * @param info
	 */
	public static void getRepoInfo(RepoInfo info)
	{
		if ( info == null )
		{
			return;
		}
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String folder = info.getLocalPath();
		File repoDir = new File(folder, GIT_FOLDER);
		Repository repo;
		try
		{
			repo = builder.setGitDir(repoDir).readEnvironment() // scan  environment  GIT_*  variables
					.findGitDir() // scan up the file system tree
					.build();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		try
		{
			/*
			 * Section is remote
			 *    Subsection:origin
			 *       Name is fetch=+refs/heads/staging:refs/remotes/origin/staging
			 *       Name is url=http://peso.rmanet.com/RMA/usbr-water-quality/usbr-wq.git
			 */
			StoredConfig config = repo.getConfig();
			String remoteUrl = config.getString(REMOTE_SECTION, ORIGIN_SUB_SECTION, URL_NAME);
			info.setSourceUrl(remoteUrl);
		}
		finally
		{
			repo.close();
		}
		
	}

	public static boolean deleteRepo(RepoInfo info)
	{
		if ( info == null )
		{
			return false;
		}
		Preferences reposNode = getRepoNode();
		String name = info.getName();
		try
		{
			if ( reposNode.nodeExists(name))
			{
				Preferences repoNode = reposNode.node(name);
				try
				{
					repoNode.removeNode();
					_repos.remove(info);
					return true;
				}
				catch (BackingStoreException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				}
			
			}
		}
		catch (BackingStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
	/**
	 * 
	 * @param info
	 * @param addifExists true to add it even if the local file is already used by Git
	 * @return
	 */
	public static boolean addRepo(RepoInfo info, boolean addIfExists)
	{
		if ( info == null )
		{
			return false;
		}
		if ( hasGitRepo(info.getLocalPath()) && !addIfExists)
		{
			return false;
		}
		if( writeRepo(info))
		{
			_repos.add(info);
			return true;
		}
		return false;
	}

	/**
	 * @param info
	 */
	private static boolean writeRepo(RepoInfo info)
	{
		Preferences reposNode = getRepoNode();	
		String name = info.getName();
		try
		{
			if ( !reposNode.nodeExists(name))
			{
				Preferences repoNode = reposNode.node(name);
				repoNode.put(LOCAL_FOLDER, info.getLocalPath());
				repoNode.put(REPO_URL, info.getSourceUrl());
				return true;
			}
		}
		catch (BackingStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
	/**
	 * @param remoteUrl
	 * @return
	 */
	public static boolean isValidRemoteUrl(String remoteUrl)
	{
		try
		{
			//Collection<Ref> refs = Git.lsRemoteRepository().setRemote(remoteUrl).call();
			return true;
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	public static void main(String[] args)
	{
		if ( args == null || args.length == 0 )
		{
			System.out.println("main:"+GitRepoUtils.class.getName() + " local-repo-folder");
			System.exit(1);
		}
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String folder = args[0];
		File repoDir = new File(folder, ".git");
		Repository repo;
		try
		{
			repo = builder.setGitDir(repoDir).readEnvironment() // scan  environment  GIT_*  variables
					.findGitDir() // scan up the file system tree
					.build();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		System.out.println("repo dir:"+repo.getDirectory());
		try
		{
			System.out.println("branch:"+repo.getBranch());
		}
		catch (IOException e)
		{
		}
		
				
		System.out.println("Identifier:"+repo.getIdentifier());
		System.out.println("Index file:"+repo.getIndexFile());
		System.out.println("repo state:"+repo.getRepositoryState());
		System.out.println("work tree:"+repo.getWorkTree());
		String name, subsection, value ;
		StoredConfig config = repo.getConfig();
		
		String remoteUrl = config.getString(REMOTE_SECTION, ORIGIN_SUB_SECTION, URL_NAME);
		try
		{
			CredentialsProvider credientials = new UsernamePasswordCredentialsProvider("username_placeholder", "password_placeholder");
			Collection<Ref> refs = Git.lsRemoteRepository().setCredentialsProvider(credientials).setRemote(remoteUrl).call();
			System.out.println("main:refs="+refs);
		}
		catch (GitAPIException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Set<String> sections = config.getSections();
		Iterator<String> sectionIter = sections.iterator();
		while ( sectionIter.hasNext())
		{
			String section = sectionIter.next();
			System.out.println("Section is "+section);
			Set<String> subsections = config.getSubsections(section);
			Iterator<String> subsectionsIter = subsections.iterator();
			while (subsectionsIter.hasNext())
			{
				subsection = subsectionsIter.next();
				System.out.println("\tSubsection:"+subsection);
				Set<String> names = config.getNames(section, subsection,true);
				Iterator<String> namesIter = names.iterator();
				while ( namesIter.hasNext())
				{
					name = namesIter.next();
					value = config.getString(section, subsection, name);
					System.out.println("\tName is "+name+"="+value );
				}
			}
			
		}
		repo.close();
	}

	/**
	 * 
	 */
	public static void checkRepoOutofDateStatus(String projectFolder)
	{
		List<RepoInfo> repos = getReposList();
		RepoInfo repo;
		for (int i = 0;i < repos.size(); i++ )
		{
			repo = repos.get(i);
			if (RMAIO.pathsEqual(projectFolder, repo.getLocalPath()))
			{
				checkRepoOutofDateStatus(repo);
			}
		}
	}

	/**
	 * @param repo
	 */
	private static void checkRepoOutofDateStatus(RepoInfo repo)
	{
		SwingWorker<List<String>, Void> worker= new SwingWorker<List<String>, Void>()
		{
			

			@Override
			protected List<String> doInBackground() throws Exception
			{
				ShowChangesActions action = new ShowChangesActions(ActionPanelPlugin.getInstance().getActionsWindow(), repo, ShowChangesActions.ChangeType.Commits);
				List<String>changes = action.getChanges();
				return changes;
			}

			@Override
			protected void done()
			{
				try
				{
					List<String> changes = get();
					if ( changes.size() > 0 )
					{
						int opt = JOptionPane.showConfirmDialog(ActionPanelPlugin.getInstance().getActionsWindow(), 
							"<html>There are "+changes.size()+" changes that have been made to the Repository since you last updated."+
							"<br>Do you want to update now?", "Changes Available", JOptionPane.YES_NO_OPTION);
						if ( opt == JOptionPane.YES_OPTION )
						{
							DownloadStudyAction action = new DownloadStudyAction(ActionPanelPlugin.getInstance().getActionsWindow(),repo);
							action.actionPerformed(null);
						}
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		};
		worker.execute();
	}
}
	
