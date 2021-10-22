
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rma.editors.DSSListSelector;
import com.rma.editors.DSSListSelectorParent;
import com.rma.model.Project;

import hec.gui.AbstractEditorPanel;
import hec.heclib.dss.DSSPathname;
import hec.io.DSSIdentifier;
import hec.lang.NamedType;

import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.client.WatMessages;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPlugin;
import hec2.wat.plugin.WatPluginManager;
import hec2.wat.util.WatI18n;

import rma.swing.RmaInsets;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTable;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.editors.EditIterationSettingsDialog;
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class IterationBcPanel extends AbstractEditorPanel
{
	public static final String TAB_NAME = "Boundary Conditions";
	public static final int DATALOCATION_COL = 0;
	public static final int PARAMETER_COL =1;
	public static final int MODEL_DSS_COL = 2;
	public static final int DSSID_COL = 3;
	
	
	private RmaJTable _bcTable;
	private JButton _selectDssBtn;
	private ModelAltIterationSettings _modelAltSettings;
	private boolean _listSelectorOpened;
	private String _lastDssFile;
	private int _editingRow;
	private BcEntryDialog _bcEditor;
	/**
	 * @param editIterationSettingsDialog
	 */
	public IterationBcPanel(EditIterationSettingsDialog editIterationSettingsDialog)
	{
		super(new GridBagLayout());
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	protected void buildControls()
	{
		String[] headers = new String[] {"Location", "Parameter", "Model DSS Record", "Selected DSS Record"};
		_bcTable = new RmaJTable(this, headers)
		{
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return false;
			}
		};
		_bcTable.removePopupMenuSumOptions();
		_bcTable.setAddRemoveEnabled(false);
		_bcTable.setRowHeight(_bcTable.getRowHeight()+5);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_bcTable.getScrollPane(), gbc);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(buttonPanel, gbc);
		
		_selectDssBtn = new JButton("Browse DSS");
		_selectDssBtn.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_selectDssBtn, gbc);
		
		
		
		tableRowSelected();
		
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_selectDssBtn.addActionListener(e->browseDSSAction( SwingUtilities.windowForComponent(this)));
		_bcTable.getSelectionModel().addListSelectionListener(e -> tableRowSelected());
		_bcTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ( e.getClickCount() != 2 )
				{
					return;
				}
				tableDoubleClickAction(e.getPoint());
			}
		});
		
	}
	/**
	 * @param point
	 */
	protected void tableDoubleClickAction(Point point)
	{
		int row = _bcTable.rowAtPoint(point);
		int col = _bcTable.columnAtPoint(point);
		if ( row == -1 || col == DSSID_COL )
		{
			return;
		}
		_bcEditor = new BcEntryDialog(this, _bcTable, row);
		_bcEditor.setVisible(true);
	}



	/**
	 * @return
	 */
	private void tableRowSelected()
	{
		int row = _bcTable.getSelectedRow();
		_selectDssBtn.setEnabled(row > -1 );
	}



	/**
	 * @return
	 */
	void browseDSSAction(Window parent)
	{
		DSSBrowser browser = new DSSBrowser(parent);
		browser.setTitle("Select DSS Pathname");
		browser.setLocationRelativeTo(this);
		browser.setVisible(true);
	}

	
	@Override
	public String getTabname()
	{
		return TAB_NAME;
	}

	@Override
	public void fillPanel(NamedType obj)
	{
		_bcTable.deleteCells();
		if ( obj instanceof ModelAltIterationSettings )
		{
			_modelAltSettings = (ModelAltIterationSettings)obj;
			List<DataLocation> dataLocs = _modelAltSettings.getDataLocations();
			Vector row;
			DataLocation dl, dl2;
			DSSIdentifier dssId;
			DssDataLocation linkedToDl;
			for (int i = 0; i< dataLocs.size();i++ )
			{
				 dl = dataLocs.get(i);
				 dl2 = dl.getLinkedToLocation();
				 if (  dl2 instanceof DssDataLocation )
				 {
					 linkedToDl = (DssDataLocation) dl2;
					 row = new Vector(4);
					 row.add(dl);
					 row.add(dl.getParameter());
					 dssId = new DSSIdentifier(linkedToDl.get_dssFile(), linkedToDl.getDssPath());
					 row.add(dssId);
					 dssId = _modelAltSettings.getDSSIdentifierFor(dl);
					 if ( dssId == null )
					 {
						 dssId = new DSSIdentifier("","");
					 }
					 row.add(dssId);
					 _bcTable.appendRow(row);
				 }
				 
			}
				
		}
		tableRowSelected();
	}
	@Override
	public boolean savePanel(NamedType obj)
	{
		_bcTable.commitEdit(true);
		if ( obj instanceof ModelAltIterationSettings )
		{
			ModelAltIterationSettings modelAltSettings = (ModelAltIterationSettings)obj;
			int numRows = _bcTable.getRowCount();
			DataLocation dl;
			DSSIdentifier dssId;
			String fileName, dssPath;
			for (int r = 0;r < numRows; r++ )
			{
				dl = (DataLocation) _bcTable.getValueAt(r, DATALOCATION_COL);
				dssId = (DSSIdentifier) _bcTable.getValueAt(r, DSSID_COL);
				if ( dssId != null )
				{
					fileName = dssId.getFileName();
					dssPath  = dssId.getDSSPath();
					if ( fileName != null && !fileName.trim().isEmpty() && dssPath != null && !dssPath.trim().isEmpty())
					{
						modelAltSettings.setDssIdentifierFor(dl, dssId);
					}
				}
			}
		}
		return true;
	}
	/**
	 * @param modelAlternative
	 * @return
	 */
	private static WatPlugin getWatPlugin(ModelAlternative modelAlt)
	{
		if ( modelAlt == null )
		{
			return null;
		}
		String program = modelAlt.getProgram();
		SimpleWatPlugin plugin = WatPluginManager.getPlugin(program);
		if ( plugin instanceof WatPlugin )
		{
			return (WatPlugin)plugin;
		}
		System.out.println("getWatPlugin:failed to find WatPlugin for "+program);
		return null;
	}

	class DSSBrowser extends RmaJDialog
		implements DSSListSelectorParent
	{
		DSSListSelector _listSelector;

		public DSSBrowser(java.awt.Window parent)
		{
			super(parent, false);
			buildControls();
			pack();
		}
		protected void buildControls()
		{
			_listSelector = new DSSListSelector(this, "Select DSS Pathname", DSSListSelector.BROWSER,false, false);
			_listSelector.setPathSelectionMode(DSSListSelector.SINGLE_PATH_SELECTION);
			Object dlObj = _bcTable.getValueAt(_bcTable.getSelectedRow(), 0);
			String dssFile = _lastDssFile;
			if ( dlObj instanceof DataLocation )
			{
				DataLocation dl = (DataLocation) dlObj;
				_listSelector.setTitle(dl.toString());
				if ( dl.getLinkedToLocation() instanceof DssDataLocation )
				{
					DssDataLocation dssDl = (DssDataLocation) dl.getLinkedToLocation();
					dssFile = dssDl.get_dssFile();
					if ( !RMAIO.isFullPath(dssFile))
					{
						dssFile = Project.getCurrentProject().getAbsolutePath(dssFile);
					}
				}

			}
			if ( dssFile != null )
			{
				_listSelector.setDssFilename(dssFile);

			}
			else
			{
				if (_lastDssFile == null)
				{
					_listSelector.setDirectory(Project.getCurrentProject()
							.getProjectDirectory());
				}
				else
				{
					_listSelector.setDssFilename(_lastDssFile);
				}
			}
			_listSelector.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					_listSelectorOpened = false;
				}
				@Override
				public void windowOpened(WindowEvent e)
				{
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							_listSelector.toFront();
						}
					});
				}
			});

			Container contentPane = _listSelector.getContentPane();
			setContentPane(contentPane);
		}
		/* (non-Javadoc)
		 * @see com.rma.editors.DSSListSelectorParent#dssListSelectorClosed(boolean, com.rma.editors.DSSListSelector)
		 */
		@Override
		public void dssListSelectorClosed(boolean closePressed, DSSListSelector theChildDialog)
		{
			if (closePressed)
			{
				setModified(true);
				_listSelectorOpened = false;
				int[] rows = _bcTable.getSelectedRows();
				if ( rows != null && rows.length > 1)
				{
					String msg = WatI18n.getI18n(WatMessages.MODEL_LINKING_EDITOR_MSG_MULTI_ROWS_SELECTED).getText();
					String title = WatI18n.getI18n(WatMessages.MODEL_LINKING_EDITOR_MSG_MULTI_ROWS_SELECTED_TITLE).getText();
					int opt = JOptionPane.showConfirmDialog(SwingUtilities.windowForComponent(this), 
							msg, title, JOptionPane.YES_NO_OPTION);
					if ( opt != JOptionPane.YES_OPTION )
					{
						int row = rows[0];
						rows = new int[1];
						rows[0] = row;
					}
				}
				else if ( rows.length < 1 )
				{
					return;
				}
				List<?> pathnames = theChildDialog.getSelectedPaths();
				if (pathnames.size() > 0)
				{
					DSSPathname dssPathname = new DSSPathname();
					String dssPath = (String) pathnames.get(0);
					dssPathname.setPathname(dssPath);
					dssPathname.setDPart("");  // don't save the D-Part
					String dssFullFile = theChildDialog.getDSSFilename();				
					String dssFile = RMAIO.getRelativePath(Project.getCurrentProject().getProjectDirectory(), dssFullFile);				
					_lastDssFile = dssFile;
					for (int i = 0;i < rows.length;i++ )
					{
						DSSIdentifier dssId = new DSSIdentifier(dssFile, dssPathname.getPathname());
						_bcTable.setValueAt(dssId, rows[i], 3);
						if ( _bcEditor != null && _bcEditor.isVisible())
						{
							_bcEditor.setSelectedDssId(dssId);
						}
					}


				}
			}
			_editingRow = -1;
		}
	}

	

}
