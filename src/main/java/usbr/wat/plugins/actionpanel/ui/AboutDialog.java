/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import com.rma.swing.UrlLabel;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.RmaImage;
import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import usbr.wat.plugins.actionpanel.model.ReportPlugin;
import usbr.wat.plugins.actionpanel.model.ReportsManager;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class AboutDialog extends RmaJDialog
{
	private Logger LOGGER = Logger.getLogger(AboutDialog.class.getName());
	
	private static final String META_MAVEN_PATH = "META-INF/maven";
	private static final String POM_XML = "pom.xml";

	private static final String PARENT_ELEMENT = "parent";

	private static final String VERSION_ELEMENT = "version";

	private static final String MAVEN_PATH = "usbr.wat.plugins/usbr-actionpanel-plugin";
	private ButtonCmdPanel _cmdPanel;
	private RmaJTable _versionTable;

	private UrlLabel _contactLabel;

	/**
	 * @param actionsWindow
	 */
	public AboutDialog(Window parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		fillPluginTable();
		pack();
		setLocationRelativeTo(getParent());
	}

	

	/**
	 * 
	 */
	protected void buildControls()
	{
		setTitle("About WTMP");
		getContentPane().setLayout(new GridBagLayout());
	
		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.VERTICAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(leftPanel, gbc);
		
		JLabel label = new JLabel(RmaImage.getImageIcon("Images/usbr.gif"));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0001;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		leftPanel.add(label, gbc);
		
		JPanel rightPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.VERTICAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(rightPanel, gbc);	
		
		
		label = new JLabel("<html><h1>WTMP</h1></html>");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);
		
		String version = getBuildVersion(MAVEN_PATH);
		label = new JLabel("<html><h2>Version:"+version+"</h2></html>");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		rightPanel.add(label, gbc);
		
		label = new JLabel("<html><h2>Central Valley Operations Office</h2></html>");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);
		
		label = new JLabel("Interior Region 10 · California-Great Basin");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS0505;
		rightPanel.add(label, gbc);
		
		label = new JLabel("U.S. Bureau of Reclamation");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);
		
		label = new JLabel("");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);		
		
		label = new JLabel("");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(label, gbc);		
		
		_contactLabel = new UrlLabel("www.usbr.gov");
		_contactLabel.setUrl("http://www.usbr.gov");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(_contactLabel, gbc);
		
		String[] headers = new String[] {"Component", "Version"};
		_versionTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight()*4;
				return d;
			}
		};
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		rightPanel.add(_versionTable.getScrollPane(), gbc);
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.CLOSE_BUTTON);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	}
	/**
	 * 
	 */
	private void fillPluginTable()
	{
		_versionTable.deleteCells();
		List<ReportPlugin> reportPlugins = ReportsManager.getPlugins();
		
		ReportPlugin plugin;
		String name, path, version;
		for (int i = 0;i < reportPlugins.size(); i++ )
		{
			plugin = reportPlugins.get(i);
			path = plugin.getMavenPath();
			version = getBuildVersion(path);
			addToTable(plugin.getName(), version);
		}
	}
	/**
	 * @param name
	 * @param version
	 */
	private void addToTable(String name, String version)
	{
		Vector<String> row = new Vector<>();
		row.add(name);
		row.add(version);
		_versionTable.appendRow(row);
	}



	/**
	 * @param string
	 * @return
	 */
	private String getBuildVersion(String mavenPackage)
	{
		String path = META_MAVEN_PATH+"/"+mavenPackage+"/"+POM_XML;
		LOGGER.info("Pom file path:" + path );
		
		InputStream is = getClass().getClassLoader().getResourceAsStream(path);
		if ( is != null )
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			try
			{
				Document doc = loadDocument(reader);
				return getVersionFromDoc(doc);
			}
			finally
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
		System.out.println("getBuildVersion:failed to find resource "+path);
		return "Unknown";
	}
	
	public Document loadDocument(Reader reader)
	{
		Document doc = null;

		if(reader!=null)
		{
			try
			{
				SAXBuilder builder = new SAXBuilder();
				doc = builder.build(reader);
			}
			catch (JDOMException e)
			{
				LOGGER.log(Level.WARNING, "loadDocument:JDOMException occurred reading XML file. Error: {0}", e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);
			}
			catch (NullPointerException e)
			{
				// SAXBuilder.build() can throw NullPointerExceptions with malformed XML blocks, so we catch them
				// here and let caller know, document could not be read by returning null.
				LOGGER.log(Level.WARNING, "loadDocument:NullPointerException occurred reading XLM file. Error: {0}",
						e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, "loadDocument:IOException occurred reading XML File. Error: {0}", e.getMessage());
				LOGGER.log(Level.FINE, "Caused By:", e);
			}
		}
		else
		{
			LOGGER.log(Level.WARNING, "loadDocument: No XLM file given. reader is null.");
		}
		if ( doc == null )
		{
			LOGGER.log(Level.WARNING, "loadDocument: Failed to read document");
		}
		return doc;
	}

	/**
	 * @param doc
	 * @return
	 */
	private String getVersionFromDoc(Document doc)
	{
		if ( doc == null )
		{
			return "unknown";
		}
		Element root = doc.getRootElement();
		
		Namespace nameSpace  = root.getNamespace();
		LOGGER.log(Level.INFO, "loadDocument: namespace is " + nameSpace);
		
		Element parentElem = null;
		if ( nameSpace == null )
		{
			parentElem = root.getChild(PARENT_ELEMENT);
		}
		else
		{
			parentElem = root.getChild(PARENT_ELEMENT, nameSpace);
		}
			
		if ( parentElem != null )
		{
			Element versionElem  = null;

			if( nameSpace ==null )
			{
				versionElem = parentElem.getChild(VERSION_ELEMENT);
			}
			else
			{
				versionElem = parentElem.getChild(VERSION_ELEMENT, nameSpace);
			}
			if ( versionElem != null )
			{
				return versionElem.getTextTrim();
			}
			else
			{
				LOGGER.log(Level.WARNING, "loadDocument: Failed to find version element");	
				logChildren(parentElem);
			}
		}
		else
		{
			LOGGER.log(Level.WARNING, "loadDocument: Failed to find parent element");	
			logChildren(root);
		}
		return "unknown";
				
		
	}

	/**
	 * @param root
	 */
	private void logChildren(Element parent)
	{
		List kids = parent.getChildren();
		Element child;
		for (int i = 0;i < kids.size();i++ )
		{
			child = (Element) kids.get(i);
			LOGGER.log(Level.INFO, "logChildren: child is " + child.getName());
			LOGGER.log(Level.INFO, "logChildren: child namespace is  " + child.getNamespaceURI());
		}
	}

	/**
	 * 
	 */
	protected void addListeners()
	{
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.CLOSE_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
		
	}

}
