/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.gitIntegration.ui;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;

import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJTextField;
import rma.swing.tree.RmaJTree;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.gitIntegration.model.GitToken;


/**
 * @author mark
 *
 */
public class RepoJTree extends EnabledJPanel
{
	
	public enum SelectionType
	{
		Folder,
		Project
	};
	
	public enum SubRepos
	{
		hec5q("5q"),
		ras("ras"),
		reports("reports"),
		rss("rss"),
		scripts("scripts"),
		shared("shared"),
		cequal_w2("cequal-w2");
	
		private String _name;
		SubRepos(String name)
		{
			_name = name;
		}
		
		public String getName()
		{
			return _name;
		}
		
	}
	private static final String RMA_GIT_URL = "https://gitlab.rmanet.app";
	private static final String GIT_URL = System.getProperty("GitUrl",RMA_GIT_URL);
	
	private static final String RMA_GIT_ROOT_PATH = "RMA/usbr-water-quality/wat-studies";
	
	private static final String GIT_ROOT_PATH = System.getProperty("GitRootPath",RMA_GIT_ROOT_PATH);
	
	private static int _rootPathParts;
	static 
	{
		_rootPathParts = new StringTokenizer(RMA_GIT_ROOT_PATH, "/").countTokens();
	}
	
	private Logger _logger = Logger.getLogger(RepoJTree.class.getName());
	
	private RmaJTree _repoLocationTree;
	private RmaJTextField _repoPathLabel;
	private FolderNode _root;
	private SelectionType _selectionType = SelectionType.Folder;
	private boolean _fillingTree;
	private String _selection;
	
	
	public RepoJTree(SelectionType type)
	{
		super(new GridBagLayout());
		_selectionType = type;
		buildControls();
		addListeners();
	}
	
	/**
	 * 
	 */
	private void buildControls()
	{
		JLabel label = new JLabel("Repo Location:")
		{
			@Override
			public void setEnabled(boolean enabled )
			{
				//do nothing
			}
		};
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(label, gbc);
		
		_repoLocationTree = new RmaJTree()
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				StringBuilder toolTip = new StringBuilder();
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if ( path != null )
				{
					Object comp = path.getLastPathComponent();
					if ( comp instanceof FolderNode )
					{
						FolderNode fnode = (FolderNode) comp;
						String desc = fnode.getDescription();
						if ( desc != null )
						{
							toolTip.append(desc);
						}
						if ( fnode.isProjectNode() )
						{
							if ( toolTip.length() > 0 )
							{
								toolTip.append("<br>");
							}
							toolTip.append(fnode.getGitFile());
						}
					}
				}
				if ( toolTip.length() > 0 )
				{
					toolTip.insert(0, "<html>");
					toolTip.append("<html>");
					return toolTip.toString();
				}
				return null;
			}	
		};
		_repoLocationTree.setModel(new DefaultTreeModel(new FolderNode("Retrieving Git Repo Info...","",null,"")));
		_repoLocationTree.setToolTipText("");
		_repoLocationTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		label.setLabelFor(_repoLocationTree);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(new JScrollPane(_repoLocationTree), gbc);
		
		_repoPathLabel = new RmaJTextField()
		{
			@Override
			public void setEnabled(boolean enabled)
			{
				// do nothing
			}
			@Override
			public String getToolTipText(MouseEvent e)
			{
				return getText();
			}
		};
		_repoPathLabel.setEditable(false);
		_repoPathLabel.setBorder(null);
		_repoPathLabel.setToolTipText("");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_repoPathLabel, gbc);	
		
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_repoLocationTree.getSelectionModel().addTreeSelectionListener(e->treePathSelected());
		
		InputMap inputMap = _repoLocationTree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = _repoLocationTree.getActionMap();
		Action refreshAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				fillRepoTree();
			}
		};
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refreshTree");
		actionMap.put("refreshTree", refreshAction);
		
	}	
	
	/**
	 * @return
	 */
	private void treePathSelected()
	{
		TreePath path = _repoLocationTree.getSelectionPath();
		if ( path == null )
		{
			_repoPathLabel.setText("");
			return;
		}
		String repoPath = getRepoPathFromTreePath(path, true);
		_repoPathLabel.setText(repoPath);
	}
	/**
	 * @param path
	 * @return
	 */
	private String getRepoPathFromTreePath(TreePath path, boolean fullPath)
	{
		StringBuilder builder = new StringBuilder();
		if ( fullPath)
		{
			builder.append(GIT_URL);
			builder.append("/");
		}
		builder.append(GIT_ROOT_PATH);
		builder.append("/");
		Object[] pathArray = path.getPath();
		FolderNode node;
		node = (FolderNode) path.getLastPathComponent();
		for (int i = 0;i < pathArray.length;i++ )
		{
			node = (FolderNode) pathArray[i];
			if ( node.isProjectNode()&& _selectionType == SelectionType.Folder)
			{
				break;
			}
			builder.append(node.getPathName());
			if ( node.isProjectNode()&& _selectionType == SelectionType.Project)
			{
				builder.append(RMAIO.getFileFromPath(node.getGitFile()));
			}
			
		}
		String retPath = builder.toString();
		if ( retPath.endsWith("/"))
		{
			retPath = retPath.substring(0,retPath.length()-1);
		}
		return retPath;
	}
	
	/**
	 * 
	 */
	public void fillRepoTree()
	{
		_fillingTree = true;
		try
		{
			List<Project> projects = getRepoProjects();
			_logger.fine("fillRepoTree:projects = "+projects);
			Project gitPrj;
			_root = new FolderNode("WAT Studies", "wat-studies", null, null);
			for (int i = 0; i < projects.size(); i++ )
			{
				gitPrj = projects.get(i);
				if ( !gitPrj.getPathWithNamespace().startsWith(GIT_ROOT_PATH))
				{
					continue;
				}
				addToTree(_root, gitPrj);

			}
			DefaultTreeModel newModel = new DefaultTreeModel(_root);
			_repoLocationTree.setModel(newModel);
			_repoLocationTree.expandAll(true);
		}
		finally
		{
			_fillingTree = false;
		}
		if ( _selection != null )
		{
			setSelectedPath(_selection);
			_selection = null;
		}
	}
	/**
	 * @param root
	 * @param gitPrj
	 */
	private void addToTree(DefaultMutableTreeNode root, Project gitPrj)
	{
		String fullPath = gitPrj.getNamespace().getFullPath();
		String relPath = fullPath.substring(GIT_ROOT_PATH.length());
	
		_logger.fine("addToTree:starting with path "+fullPath);
		_logger.fine("addToTree:starting with name "+gitPrj.getNameWithNamespace());
		
		StringTokenizer pathTokenizer = new StringTokenizer(relPath, "/");
		StringTokenizer nameTokenizer = new StringTokenizer(gitPrj.getNameWithNamespace(), "/");
		for (int i = 0;i < _rootPathParts; i++ )
		{
			nameTokenizer.nextToken();
		}
		String pathPart, namePart, gitFile;
		DefaultMutableTreeNode child, current = root;
		
		while (pathTokenizer.hasMoreTokens())
		{
			pathPart = pathTokenizer.nextToken();
			namePart = nameTokenizer.nextToken();
			child = getChildNode(current, pathPart);
			if ( child == null )
			{
				_logger.fine("addToTree:adding name="+namePart+" path="+pathPart);
				if (!pathTokenizer.hasMoreTokens() )
				{
					gitFile = gitPrj.getHttpUrlToRepo();
					if ( isSubRepo(gitFile))
					{ // don't add the subrepo, just the main repo
						continue;
					}
				}
				else
				{
					gitFile = null;
				}
				child = new FolderNode(namePart, pathPart, gitFile, gitPrj.getDescription());
				current.add(child);
				current = child;
			}
			else
			{
				_logger.fine("addToTree:found existing node. name="+namePart+" path="+pathPart);
				current = child;
			}
		}
		
		
	}



	/**
	 * see if the git file is a subrepo/subproject
	 * @param gitFile
	 * @return
	 */
	private boolean isSubRepo(String gitFile)
	{
		String file = RMAIO.getFileNameNoExtension(gitFile);
		SubRepos[] subRepos = SubRepos.values();
		for (int i = 0;i < subRepos.length;i++ )
		{
			if ( subRepos[i].getName().equalsIgnoreCase(file))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @param current
	 * @param pathPart
	 * @return
	 */
	private static DefaultMutableTreeNode getChildNode(DefaultMutableTreeNode current,
			String pathPart)
	{
		int cnt = current.getChildCount();
		FolderNode child;
		for (int i = 0;i < cnt; i++ )
		{
			child = (FolderNode) current.getChildAt(i);
			if ( pathPart.equals(child.getPathName()))
			{
				return child;
			}
		}
		return null;
	}



	/**
	 * @return
	 */
	private List<Project> getRepoProjects()
	{
		String token = getGitToken();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		GitLabApi gitLabApi = new GitLabApi(GIT_URL, token);
		System.out.println("getRepoProjects:connected to "+gitLabApi.getGitLabServerUrl());
		try
		{
			try
			{
				List<Project> projects = gitLabApi.getProjectApi().getProjects();
				return projects;
			}
			catch (GitLabApiException e)
			{
				System.out.println("getRepoProjects:exception getting list of projects "+e);
				JOptionPane.showMessageDialog(this, "Failed to connect to GitLab. Error "
						+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				GitToken.clearGitToken();
				e.printStackTrace();
			}
			return null;
		}
		finally
		{
			gitLabApi.close();
			setCursor(Cursor.getDefaultCursor());
		}
		
	}



	/**
	 * @return
	 */
	private String getGitToken()
	{
		String token = GitToken.getGitToken(SwingUtilities.windowForComponent(this));
		return token;
	}
	
	
	/**
	 * @return
	 */
	public String getRepoPath()
	{
		TreePath path = _repoLocationTree.getSelectionPath();
		if ( path == null )
		{
			return null;
		}
		return getRepoPathFromTreePath(path, false);
	}
	
	@SuppressWarnings("serial")
	class FolderNode extends DefaultMutableTreeNode
	{
		private String _pathname;
		private boolean _projectNode;
		private String _description;
		private String _gitFile;
		FolderNode(String displayName, String pathName, String gitFile, String description)
		{
			super(displayName);
			_pathname = pathName;
			_projectNode = gitFile!=null;
			_gitFile = gitFile;
			_description = description;
		}
		/**
		 * @return
		 */
		public boolean isProjectNode()
		{
			return _projectNode;
		}
		
		public String getPathName()
		{
			return _pathname;
		}
		public String getDescription()
		{
			return _description;
		}
		public String getGitFile()
		{
			return _gitFile;
		}
	}

	/**
	 * @param sourceUrl
	 */
	public void setSelectedPath(String path)
	{
		if ( path == null )
		{
			_repoLocationTree.clearSelection();
			return;
		}
		if ( _fillingTree )
		{
			_selection = path;
			return;
		}
		String pathLower=path.toLowerCase();
		
		if ( pathLower.startsWith(GIT_URL.toLowerCase()))
		{
			pathLower = pathLower.substring(GIT_URL.length());
		}
		if ( !pathLower.startsWith(GIT_ROOT_PATH.toLowerCase()))
		{
			_repoLocationTree.clearSelection();
			_logger.info("Invalid starting path for " + path+" tree starts with "+GIT_ROOT_PATH);
			_repoPathLabel.setText(pathLower);
			return;
		}
		String shortenedPath = pathLower.substring(GIT_ROOT_PATH.length());
		FolderNode current = _root, node;
		StringTokenizer tokenizer = new StringTokenizer(shortenedPath, "/");
		String pathPart;
		while ( tokenizer.hasMoreTokens() && current != null)
		{
			pathPart = tokenizer.nextToken();
			node = findNode(current, pathPart);
			if ( node != null )
			{
				current = node;
			}
		}
		_repoLocationTree.setSelectionPath(new TreePath(current.getPath()));
	}

	/**
	 * @param pathPart
	 * @return
	 */
	private FolderNode findNode(FolderNode startNode, String pathPart)
	{
		int cnt = startNode.getChildCount();
		FolderNode child;
		for (int i = 0;i < cnt; i++ )
		{
			child = (FolderNode) startNode.getChildAt(i);
			if ( child.getPathName().equalsIgnoreCase(pathPart))
			{
				return child;
			}
		}
		return null;
	}

	/**
	 * 
	 */
	public void clearPerformed()
	{
		_repoLocationTree.clearSelection();
		_repoPathLabel.setText("");
	}

	/**
	 * @return
	 */
	public String getParentUrl()
	{
		return GIT_URL;
	}
}
