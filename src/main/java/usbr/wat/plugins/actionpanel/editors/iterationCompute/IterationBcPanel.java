
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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.rma.editors.DSSListSelector;
import com.rma.editors.DSSListSelectorParent;
import com.rma.io.DssFileManagerImpl;
import com.rma.model.Project;

import hec.gfx2d.G2dDialog;
import hec.gui.AbstractEditorPanel;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.HecTime;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesCollectionContainer;
import hec.io.TimeSeriesContainer;
import hec.lang.NamedType;
import hec.util.NumericComparator;

import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.client.WatMessages;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPlugin;
import hec2.wat.plugin.WatPluginManager;
import hec2.wat.util.WatI18n;

import rma.swing.RmaImage;
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
	private static final String DEFAULT_BROWSER_TITLE = "Select DSS Pathname";
	
	public static final int INDEX_COL = 0;
	public static final int DATALOCATION_COL = 1;
	public static final int PARAMETER_COL = 2;
	public static final int MODEL_DSS_COL = 3;
	public static final int DSSID_COL = 4;
	
	
	protected RmaJTable _bcTable;
	private JButton _selectDssBtn;
	private ModelAltIterationSettings _modelAltSettings;
	private boolean _listSelectorOpened;
	private String _lastDssFile;
	private int _editingRow;
	private BcEntryDialog _bcEditor;
	private JButton _clearDssBtn;
	private JButton _plotBtn;
	protected EditIterationSettingsDialog _parent;
	private DSSBrowser _browser;
	/**
	 * @param editIterationSettingsDialog
	 */
	public IterationBcPanel(EditIterationSettingsDialog parent)
	{
		super(new GridBagLayout());
		_parent = parent;
		buildControls();
		addListeners();
	}

	

	/**
	 * 
	 */
	protected void buildControls()
	{
		String[] headers = new String[] {"Index", "Location", "Parameter", "Model DSS Record", "Selected DSS Record"};
		_bcTable = new RmaJTable(this, headers)
		{
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return false;
			}
			@Override
			public void setEnabled(boolean enabled)
			{
			}
		};
		_bcTable.setIntegerCellEditor(INDEX_COL);
		_bcTable.removePopupMenuSumOptions();
		_bcTable.setAddRemoveEnabled(false);
		_bcTable.setRowHeight(_bcTable.getRowHeight()+5);
		_bcTable.setColumnWidths(75,220, 135, 260, 260);
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
		
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		add(panel, gbc);
		
		_plotBtn = new JButton(RmaImage.getImageIcon("Images/plot18.gif"));
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_plotBtn, gbc);
		
		_selectDssBtn = new JButton("Browse DSS");
		_selectDssBtn.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_selectDssBtn, gbc);
		
		_clearDssBtn = new JButton("Clear DSS Entries");
		_clearDssBtn.setToolTipText("Clear the Selected DSS Record information for the selected rows");
		_selectDssBtn.setEnabled(false);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTH;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_clearDssBtn, gbc);
		
		tableRowSelected();
		
	}
	/**
	 * 
	 */
	protected void addListeners()
	{
		_selectDssBtn.addActionListener(e->browseDSSAction( SwingUtilities.windowForComponent(this)));
		_clearDssBtn.addActionListener(e->clearSelectedRowsAction());
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
		_plotBtn.addActionListener(e->plotRecords());
		
	}
	/**
	 * @return
	 */
	private void plotRecords()
	{
		int[] rows = _bcTable.getSelectedRows();
		DataLocation dl;
		DSSIdentifier origDssId, iterDssId;
		TimeSeriesContainer origTs;
		for (int r = 0; r < rows.length;r++ )
		{
			dl = (DataLocation) _bcTable.getValueAt(rows[r], DATALOCATION_COL);
			origDssId = (DSSIdentifier) _bcTable.getValueAt(rows[r], MODEL_DSS_COL);
			iterDssId = (DSSIdentifier) _bcTable.getValueAt(rows[r], DSSID_COL);
			origDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(origDssId.getFileName()), origDssId.getDSSPath());
			origTs = DssFileManagerImpl.getDssFileManager().readTS(origDssId, true);
			Vector data = new Vector();
			if ( origTs.numberValues > 0 )
			{
				data.add(origTs);
			}
			if ( iterDssId.getDSSPath() != null && !iterDssId.getDSSPath().isEmpty())
			{
				DSSIdentifier dss2 = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(iterDssId.getFileName()), iterDssId.getDSSPath());
				HecTime[] times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dss2, 0);
				if(times!=null && times.length==2)
				{
					HecTimeSeries hecTs= new HecTimeSeries(dss2.getFileName());
					hecTs.setTimeWindow(times[0], times[1]);
					hecTs.setPathname(iterDssId.getDSSPath());
					TimeSeriesCollectionContainer tscc = new TimeSeriesCollectionContainer();

					if ( hecTs.read(tscc, true, false) == 0 )
					{
						TimeSeriesContainer[] tscs = tscc.get();
						if ( tscs != null )
						{
							for(int i = 0;i < tscs.length;i++ )
							{
								data.add(tscs[i]);
							}
						}
					}
				}
				
			}
		
			G2dDialog g2dDlg = new G2dDialog( null, dl.getName(), false, data);
			RmaJDialog dlg = new RmaJDialog(SwingUtilities.windowForComponent(this), dl.getName(), true);
			dlg.pack();
			dlg.setSize(500,500);
			dlg.setJMenuBar(g2dDlg.getJMenuBar());
			dlg.setContentPane(g2dDlg.getContentPane());
			
			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		}
	}



	/**
	 * @return
	 */
	private void clearSelectedRowsAction()
	{
		int[] rows = _bcTable.getSelectedRows();
		if ( rows == null || rows.length == 0 )
		{
			return;
		}
		int opt = JOptionPane.showConfirmDialog(this, "Do you want to clear the Selected DSS Record for the selected rows?","Comfirm", JOptionPane.YES_NO_OPTION);
		if ( opt != JOptionPane.YES_OPTION )
		{
			return;
		}
		for (int r = 0; r < rows.length; r++ )
		{
			clearRow(rows[r]);
		}
	}
	public void clearRow(int row)
	{
		DSSIdentifier dssId = (DSSIdentifier) _bcTable.getValueAt(row, DSSID_COL);
		dssId.setFileName("");
		dssId.setDSSPath("");
		_bcTable.setValueAt(dssId, row, DSSID_COL);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		EventQueue.invokeLater(()-> tableRowSelected());
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
		boolean enabled = row > -1 && isEnabled();
		_selectDssBtn.setEnabled(enabled);
		_clearDssBtn.setEnabled(enabled);
		_plotBtn.setEnabled(enabled);
		if ( enabled )
		{
			updateDssBrowserTitle();
		}
	}



	/**
	 * @return
	 */
	void browseDSSAction(Window parent)
	{
		_browser = new DSSBrowser(parent);
		updateDssBrowserTitle();
		
		_browser.setLocationRelativeTo(this);
		_browser.setVisible(true);
	}

	
	/**
	 * 
	 */
	private void updateDssBrowserTitle()
	{
		if ( _browser == null )
		{
			return;
		}
		int row = _bcTable.getSelectedRow();
		String title = DEFAULT_BROWSER_TITLE;
		if ( row > -1 )
		{
			DSSIdentifier dssId = (DSSIdentifier) _bcTable.getValueAt(row, MODEL_DSS_COL);
			title = title + " for "+dssId.getDSSPath();
		}
		_browser.setTitle(title);
	}



	@Override
	public String getTabname()
	{
		return TAB_NAME;
	}

	@Override
	public void fillPanel(NamedType obj)
	{
		_bcTable.setRowSorter(null);
		_bcTable.deleteCells();
		if ( obj instanceof ModelAltIterationSettings )
		{
			_modelAltSettings = (ModelAltIterationSettings)obj;
			List<DataLocation> dataLocs = _modelAltSettings.getDataLocations();
			Vector row;
			DataLocation dl, dl2;
			DSSIdentifier dssId;
			DataLocation linkedToDl;
			String dssFile;
			for (int i = 0; i< dataLocs.size();i++ )
			{
				 dl = dataLocs.get(i);
				 if ( !dl.getClass().equals(DataLocation.class))
				 {
					 continue;
				 }
				 dl2 = dl.getLinkedToLocation();
				 if (  dl2 instanceof DataLocation )
				 {
					 linkedToDl = dl2;
					 row = new Vector(5);
					 row.add(i+1);
					 row.add(dl);
					 row.add(dl.getParameter());
					 if ( linkedToDl instanceof DssDataLocation )
					 {
						 dssFile = ((DssDataLocation)linkedToDl).get_dssFile();
					 }
					 else
					 {
						 dssFile = "";
					 }
					 dssId = new DSSIdentifier(dssFile, linkedToDl.getDssPath());
					 row.add(dssId);
					 dssId = _modelAltSettings.getDSSIdentifierFor(dl);
					 if ( dssId == null )
					 {
						 dssId = new DSSIdentifier("","");
					 }
					 else
					 {
						 dssId = new DSSIdentifier(dssId);
					 }
					 row.add(dssId);
					 _bcTable.appendRow(row);
				 }
				 
			}
				
		}
		tableRowSelected();
		TableModel tm = _bcTable.getModel();
		TableRowSorter<TableModel> trs = new TableRowSorter<>(tm);
		trs.setComparator(INDEX_COL, new NumericComparator(0.0));
		_bcTable.setRowSorter(trs);
		setModified(false);
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
					if ( fileName != null && dssPath != null /*&& !fileName.trim().isEmpty()  && !dssPath.trim().isEmpty()*/)
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
			Object dlObj = _bcTable.getValueAt(_bcTable.getSelectedRow(), DATALOCATION_COL);
			String dssFile = _lastDssFile, cPart = null;
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
				String dssPath = dl.getDssPath();
				DSSPathname dssPathname = new DSSPathname(dssPath);
				cPart = dssPathname.cPart();

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
			if ( cPart != null )
			{
				_listSelector.getSelectionAndFilterPanel().setFilter("///"+cPart+"////");
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
						_bcTable.setValueAt(dssId, rows[i], DSSID_COL);
						setModified(true);
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
