package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import com.rma.model.Project;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatSimulation;

import rma.swing.RmaInsets;
import rma.swing.RmaJTable;
import rma.util.RMAFilenameFilter;
import usbr.wat.plugins.actionpanel.model.ComputeSettings;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class ComputeScriptsPanel extends JPanel
{
	private static final int MODEL_ALT_COL = 1;
	private static final int SCRIPT_COL = 2;
	private static final int RUN_SCRIPT_COL = 3;
	
	
	protected static final int NUM_ROWS_VISIBLE = 6;
	private RmaJTable _modelTable;
	private JButton _browseButton;
	private String _lastDir;
	private WatSimulation _simulation;
	private ComputeSettings _computeSettings;

	public ComputeScriptsPanel(String title)
	{
		super(new GridBagLayout());
		buildControls(title);
		addListeners();
	}

	/**
	 * @param title
	 */
	protected void buildControls(String title)
	{
		TitledBorder border = BorderFactory.createTitledBorder(title);
		setBorder(border);
		String[] headers = {"Model", "Model Alternative", "Script", "Run Script"};
		
		_modelTable = new RmaJTable(this, headers)
		{
			@Override
			public Dimension getPreferredScrollableViewportSize()
			{
				Dimension d = super.getPreferredScrollableViewportSize();
				d.height = getRowHeight() * NUM_ROWS_VISIBLE;
				return d;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return col > 1;
			}
		};
		_modelTable.setRowHeight(_modelTable.getRowHeight()+5);
		_modelTable.removePopupMenuSumOptions();
		_modelTable.setColumnEnabled(false, 0);
		_modelTable.setColumnEnabled(false, 1);
		_modelTable.setCheckBoxCellEditor(3);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		add(_modelTable.getScrollPane(), gbc);
		
		JPanel panel = new JPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		add(panel, gbc);
		
		_browseButton = new JButton("Browse");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_browseButton, gbc);
		
		
		tableRowSelected();
	}

	/**
	 * 
	 */
	protected void addListeners()
	{
		_modelTable.getSelectionModel().addListSelectionListener(e->tableRowSelected());
		_browseButton.addActionListener(e->browseFilesAction());
	}

	/**
	 * @return
	 */
	private void editScriptAction()
	{
		int row = _modelTable.getSelectedRow();
		if ( row == -1 )
		{
			return;
		}
		
	}

	/**
	 * @return
	 */
	private void tableRowSelected()
	{
		_browseButton.setEnabled(_modelTable.getSelectedRow() > -1);
	}

	/**
	 * @return
	 */
	private void browseFilesAction()
	{
		int row = _modelTable.getSelectedRow();
		if ( row < 0 )
		{
			return;
		}
		String dir = Project.getCurrentProject().getProjectDirectory();
		if ( _lastDir != null )
		{
			dir = _lastDir;
		}
		JFileChooser chooser = new JFileChooser(dir);
		FileFilter filter = new RMAFilenameFilter("py", "Script Files");
		chooser.addChoosableFileFilter(filter);
		chooser.setFileFilter(filter);
		int opt = chooser.showOpenDialog(this);
		if ( opt != JFileChooser.APPROVE_OPTION )
		{
			return;
		}
		File file = chooser.getSelectedFile();
		if ( file == null )
		{
			return;
		}
		
		_lastDir = file.getParent();
		_modelTable.setValueAt(file.getAbsolutePath(), row, 2);
		_modelTable.setModified(true);
	}
	/**
	 * 
	 * @param sim
	 */
	public void setSimulation(WatSimulation sim)
	{
		_simulation = sim;
		_modelTable.deleteCells();
		if ( _simulation != null )
		{
			List<ModelAlternative> modelAlts = _simulation.getAllModelAlternativeList();
			Vector row;
			ModelAlternative modelAlt;
			for (int i = 0;i < modelAlts.size();i++)
			{
				row = new Vector(4);
				modelAlt = modelAlts.get(i);
				row.add(modelAlt.getProgram());
				row.add(modelAlt);
				_modelTable.appendRow(row);
			}
		}
		_modelTable.setColumnEnabled(false, 0);
		_modelTable.setColumnEnabled(false, 1);
	}
	/**
	 * @param computeSettings
	 */
	public void fillPanel(ComputeSettings computeSettings)
	{
		_computeSettings = computeSettings;
		int rowCnt = _modelTable.getRowCount();
		ModelAlternative modelAlt;
		Project proj = Project.getCurrentProject();
		String script, absScript;
		boolean runScript;
		for(int r = 0;r < rowCnt; r++ )
		{
			modelAlt = (ModelAlternative) _modelTable.getValueAt(r, MODEL_ALT_COL);
			script = computeSettings.getScriptFor(modelAlt);
			absScript = proj.getAbsolutePath(script);
			runScript = computeSettings.shouldRunScriptFor(modelAlt);
			_modelTable.setValueAt(absScript, r, SCRIPT_COL);
			_modelTable.setValueAt(runScript, r, RUN_SCRIPT_COL);
		}
		tableRowSelected();
	}
	
	public boolean savePanel(ComputeSettings computeSettings)
	{
		_modelTable.commitEdit(true);
		ComputeSettings settings = _computeSettings;
		if ( computeSettings != null )
		{
			settings = computeSettings;
		}
		int rowCnt = _modelTable.getRowCount();
		ModelAlternative modelAlt;
		Project proj = Project.getCurrentProject();
		String relScript;
		for(int r = 0;r < rowCnt; r++ )
		{	
			modelAlt = (ModelAlternative) _modelTable.getValueAt(r, MODEL_ALT_COL);
			String script = (String) _modelTable.getValueAt(r, SCRIPT_COL);
			if ( script != null )
			{
				script = script.trim();
				if ( !script.isEmpty())
				{
					relScript = proj.getRelativePath(script);
					computeSettings.setScriptFor(modelAlt, relScript);
					Object runScriptObj = _modelTable.getValueAt(r, RUN_SCRIPT_COL);
					if ( runScriptObj != null )
					{
						computeSettings.setRunScriptFor(modelAlt,Boolean.parseBoolean(runScriptObj.toString()));
					}
				}
				else
				{
					computeSettings.setScriptFor(modelAlt, null);
					computeSettings.setRunScriptFor(modelAlt,false);
				}
			}
		}
		return true;
	}
}
