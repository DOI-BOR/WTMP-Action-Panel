/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import hec.ui.ProgressListener;

import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public class ProgressListenerDialog extends RmaJDialog
		implements ProgressListener
{
	private static final String CLOSE_TEXT = "Close";
	private static final String CANCEL_TEXT = "Cancel";
	
	private JTextPane _progressText;
	private JProgressBar _progressBar;
	private JButton _cancelCloseBtn;
	
	private Color _backgroundColor = getBackground();//UIManager.getColor("TextField.disabledBackground");

	public ProgressListenerDialog(Window parent, String title)
	{
		super(parent);
		buildControls(title);
		pack();
		addListeners();
		setLocationRelativeTo(parent);
	}
	/**
	 * 
	 */
	private void buildControls(String title)
	{
		setTitle(title);
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());
		
		_progressText = new JTextPane() 
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = 300;
				d.width = 300;
				return d;
			}
		};
		_progressText.setEditable(false);
		Font f = _progressText.getFont();
		Font newFont = new Font("monospaced", Font.PLAIN, f.getSize());
		_progressText.setFont(newFont);
		_progressText.setBackground(_backgroundColor);
		_progressText.setComponentPopupMenu(buildPopupMenu());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(new JScrollPane(_progressText), gbc);
		
		
		_progressBar = new JProgressBar();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_progressBar, gbc);
				
	
		_cancelCloseBtn = new JButton(CANCEL_TEXT);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cancelCloseBtn, gbc);
		
	}

	protected JPopupMenu buildPopupMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Select All");
		menuItem.addActionListener(e->_progressText.selectAll());
		popupMenu.add(menuItem);
		menuItem = new JMenuItem("Copy");
		menuItem.addActionListener(e->_progressText.copy());
		popupMenu.add(menuItem);
		menuItem = new JMenuItem("Save Messages");
		menuItem.addActionListener(e->saveMessagesAction());
		popupMenu.add(menuItem);
		return popupMenu;
	}
	/**
	 * @return
	 */
	private void saveMessagesAction()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		int retval = chooser.showSaveDialog(this);
		if ( retval == JFileChooser.APPROVE_OPTION )
		{
			File theFile = chooser.getSelectedFile();
			if (theFile == null )
				return;
			saveFile(theFile);
		}
	}
	/**
	 * 
	 * @param theFile
	 * @return
	 */
	private boolean saveFile(File theFile)
	{
		if ( theFile == null )
			return false;
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(theFile));

			out.write(_progressText.getText());
			out.flush();
			out.close();
		}
		catch (IOException e )
		{
			System.out.println("Failed to save file " + theFile.getName() + " " + e );
			JOptionPane.showMessageDialog(null, "Failed to save file", "Save Error", JOptionPane.ERROR_MESSAGE);
		}
		return true;

	}
	/**
	 * 
	 */
	protected void addListeners()
	{
		_cancelCloseBtn.addActionListener(e->cancelCloseAction());
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if ( CANCEL_TEXT.equals(_cancelCloseBtn.getText()))
				{
					cancelCloseAction();
				}
				EventQueue.invokeLater(()->cancelCloseAction());
			}
		});
	}
	/**
	 * @return
	 */
	private void cancelCloseAction()
	{
		if ( CANCEL_TEXT.equals(_cancelCloseBtn.getText()))
		{
			// cancel the job.
			_cancelCloseBtn.setText(CLOSE_TEXT);
		}
		else
		{
			setVisible(false);
			dispose();
		}
	}
	/**
	 * 
	 */
	@Override
	public void finish()
	{
		_cancelCloseBtn.setText(CLOSE_TEXT);
	}
	/**
	 * 
	 */
	@Override
	public void incrementProgress(int increment)
	{
		if ( SwingUtilities.isEventDispatchThread() )
		{
			int val = _progressBar.getValue();
			_progressBar.setValue(val+increment);
		}
		else
		{
			EventQueue.invokeLater(() ->
			{
				int val = _progressBar.getValue();
				_progressBar.setValue(val+increment);
			});
		}	
	}
	/**
	 * 
	 */
	@Override
	public void progress(int completedWorkUnits)
	{
		if ( EventQueue.isDispatchThread())
		{
			_progressBar.setValue(completedWorkUnits);
		}
		else
		{
			EventQueue.invokeLater(()->_progressBar.setValue(completedWorkUnits));
		}
	}
	/**
	 * 
	 */
	@Override
	public void progress(String message)
	{
		addMessage(message);
	}
	/**
	 * 
	 * @param msg
	 */
	public void addMessage(String msg)
	{
		addMessage(msg, getMessageColor(MessageType.GENERAL));
	}
	/**
	 * 
	 * @param msg
	 * @param fgColor
	 */
	public void addMessage(String msg, Color fgColor)
	{
		if ( EventQueue.isDispatchThread())
		{
			appendMessage(msg, fgColor);
		}
		else
		{
			EventQueue.invokeLater(()->appendMessage(msg, fgColor ));
		}
	}
	/**
	 * 
	 * @param msg
	 * @param fgColor
	 */
	protected void appendMessage(String msg, Color fgColor)
	{
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setForeground(attrs, fgColor);
		StyleConstants.setBackground(attrs, _backgroundColor);
		displayMessage(msg, attrs,_progressText.getDocument());
	}
	/**
	 * 
	 * @param msg
	 * @param attrs
	 * @param doc
	 */
	private void displayMessage(String msg, SimpleAttributeSet attrs, Document doc)
	{
		Font f = (Font)UIManager.get("TextField.font");
		StyleConstants.setFontFamily(attrs,"monospaced");
		StyleConstants.setFontSize(attrs, f.getSize()+1);
		boolean overWrite = false;
		final AttributeSet sAttrs = attrs;
		if (msg == null || msg.length() == 0 )
		{
			msg="\n";
		}
		else if ( msg.charAt(msg.length()-1) != '\n' )
		{
			msg+="\n";
		}

		if ( msg.charAt(0) == '\b') // overwrite the previous line
		{
			msg = msg.substring(1);
			overWrite = true;
		}
		if ( doc == null )
		{
			doc = _progressText.getDocument();
		}
		

		try
		{
			int len = doc.getLength();
			doc.insertString(len, msg, sAttrs);
		}
		catch (BadLocationException ex)
		{
			System.out.println(msg);
		}

	}
	/**
	 * 
	 */
	@Override
	public void progress(String msg, MessageType msgType)
	{
		addMessage(msg, getMessageColor(msgType));
	}
	/**
	 * 
	 */
	@Override
	public void progress(String msg, int completedWorkUnits)
	{
		progress(completedWorkUnits);
		progress(msg);

	}
	/**
	 * 
	 */
	@Override
	public void progress(String msg, MessageType msgType, int completedWorkUnits)
	{
		progress(completedWorkUnits);
		addMessage(msg, getMessageColor(msgType));
	}
	/**
	 * 
	 * @param messageType
	 * @return
	 */
	private Color getMessageColor(MessageType messageType)
	{
		switch (messageType)
		{
			case IMPORTANT :
				return Color.BLUE;
			case ERROR :
				return Color.RED;
			case WARNING :
				return Color.ORANGE;
			default :
			case GENERAL :
				return Color.BLACK;
		}
	}
	/**
	 * 
	 */
	@Override
	public void setStayOnTop(boolean alwaysOnTop)
	{
		setAlwaysOnTop(alwaysOnTop);
	}
	/**
	 * 
	 */
	@Override
	public void start()
	{
		start(100);
	}
	/**
	 * 
	 */
	@Override
	public void start(int totalWorkUnits)
	{
		_cancelCloseBtn.setText(CANCEL_TEXT);
		_progressBar.setIndeterminate(false);
		_progressBar.setMaximum(totalWorkUnits);
		_progressBar.setValue(0);

	}
	/**
	 * 
	 */
	@Override
	public void switchToDeterminate(int completedWorkUnits)
	{
		setIndeterminate(false);
		progress(completedWorkUnits);
	}
	/**
	 * 
	 */
	@Override
	public void switchToIndeterminate()
	{
		setIndeterminate(true);
	}
	/**
	 * 
	 * @param indeterminate
	 */
	protected void setIndeterminate(final boolean indeterminate)
	{
		if ( SwingUtilities.isEventDispatchThread())
		{
			_progressBar.setIndeterminate(indeterminate);
		}
		else
		{
			EventQueue.invokeLater(()-> _progressBar.setIndeterminate(indeterminate));
		}
	}
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProgressListenerDialog pld = new ProgressListenerDialog(new JFrame(), "Extract");
		pld.setVisible(true);
		Thread testThread = new Thread("Text Thread")
		{
			@Override
			public void run()
			{
				try
				{
				pld.start();
				pld.progress("Starting...");
				pld.progress(10);
				Thread.sleep(1000);
				pld.progress("Step 2...");
				pld.progress(30);
				Thread.sleep(1000);
				pld.switchToIndeterminate();
				Thread.sleep(1000);
				pld.switchToDeterminate(50);
				pld.progress("More work");
				Thread.sleep(1000);
				pld.progress(50);
				pld.progress("Going Going");
				Thread.sleep(1000);
				pld.progress(70);
				pld.progress("Finishing....");
				Thread.sleep(1000);
				pld.progress(100);
				pld.finish();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		testThread.setPriority(Thread.NORM_PRIORITY-2);
		testThread.start();
				
	}

}
