
package usbr.wat.plugins.actionpanel.editors;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import hec.gui.AbstractEditorPanel;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatSimulation;
import hec2.wat.ui.ModelAltListCellRenderer;

import rma.swing.ButtonCmdPanel;
import rma.swing.ButtonCmdPanelListener;
import rma.swing.EnabledJPanel;
import rma.swing.RmaInsets;
import rma.swing.RmaJCheckBox;
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJIntegerField;
import rma.swing.RmaJIntegerSetField;
import rma.swing.RmaJTabbedPane;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.IterationBcPanel;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.SensitivityPanel;
import usbr.wat.plugins.actionpanel.model.IterationSettings;
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;
import usbr.wat.plugins.actionpanel.model.SensitivitySettings;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class EditIterationSettingsDialog extends RmaJDialog
{

	private RmaJTextField _simGroupFld;
	private RmaJComboBox<WatSimulation> _simCombo;
	private RmaJDescriptionField _simDescFld;
	private RmaJCheckBox _useIterativeComputeCheck;
	private RmaJIntegerSetField _groupMembersFld;
	private RmaJComboBox<ModelAlternative> _modelAltCombo;
	private SometimesTabbedPanel _panelForTabs;
	private ButtonCmdPanel _cmdPanel;
	private SimulationGroup _simGroup;
	private WatSimulation _selectedSim;
	private EnabledJPanel _bottomPanel;
	private ModelAlternative _selectedModelAlt;
	private RmaJIntegerField _maxMembersFld;

	private IterationBcPanel _bcPanel;
	private SensitivityPanel _sensitivityPanel;
	/**
	 * @param parent
	 */
	public EditIterationSettingsDialog(ActionsWindow parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setLocationRelativeTo(getParent());
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Edit Iteration Compute Settings");
		
		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(topPanel, gbc);
		
		buildTopPanel(topPanel);
		
		_bottomPanel = new EnabledJPanel(new GridBagLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		getContentPane().add(_bottomPanel, gbc);
		
		buildBottomPanel(_bottomPanel);	
		
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_APPLY_CANCEL_BUTTONS);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.SOUTHWEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5555;
		getContentPane().add(_cmdPanel, gbc);
	
		_bcPanel = new IterationBcPanel(this);
		_sensitivityPanel = new SensitivityPanel(this);
		
		_panelForTabs.addPanel(_bcPanel);
		_panelForTabs.addPanel(_sensitivityPanel);
		
		
		iterativeComputeCheckChanged();
	}

	

	/**
	 * 
	 */
	private void buildTopPanel(JPanel panel)
	{
		JLabel label = new JLabel("Simulation Group:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_simGroupFld = new RmaJTextField();
		_simGroupFld.setEditable(false);
		label.setLabelFor(_simGroupFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_simGroupFld, gbc);
		
		label = new JLabel("Simulation:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_simCombo = new RmaJComboBox<>();
		_simCombo.setEditable(false);
		label.setLabelFor(_simCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_simCombo, gbc);	
		
		_useIterativeComputeCheck= new RmaJCheckBox("Make Iterative Simulation");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_useIterativeComputeCheck, gbc);	
		
		label = new JLabel("Description:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_simDescFld = new RmaJDescriptionField();
		_simDescFld.setEditable(false);
		label.setLabelFor(_simDescFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_simDescFld, gbc);
		
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(new JSeparator(), gbc);
	}

	/**
	 * @param bottomPanel
	 */
	private void buildBottomPanel(JPanel panel)
	{
		JLabel label = new JLabel("Compute Members:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_groupMembersFld = new RmaJIntegerSetField();
		label.setLabelFor(_groupMembersFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_groupMembersFld, gbc);
		
		label = new JLabel("Maximum:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_maxMembersFld = new RmaJIntegerField();
		label.setLabelFor(_maxMembersFld);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_maxMembersFld, gbc);
			
		label = new JLabel("Model Alternative:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);
		
		_modelAltCombo = new RmaJComboBox<>();
		_modelAltCombo.setRenderer(new ModelAltListCellRenderer());
		label.setLabelFor(_modelAltCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_modelAltCombo, gbc);	
		
		_panelForTabs = new SometimesTabbedPanel();
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_panelForTabs, gbc);
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_simCombo.addItemListener(e->simComboChange(e));
		_modelAltCombo.addItemListener(e->modelAltComboChanged(e));
		_useIterativeComputeCheck.addActionListener(e->iterativeComputeCheckChanged());
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener()
		{
			public void buttonCmdActionPerformed(ActionEvent e)
			{
				switch (e.getID())
				{
					case ButtonCmdPanel.OK_BUTTON :
						
						if ( saveForm())
						{
							setVisible(false);
						}
						break;
					case ButtonCmdPanel.APPLY_BUTTON:
						saveForm();
					case ButtonCmdPanel.CANCEL_BUTTON :
						setVisible(false);
						break;
				}
			}
		});
	}

	/**
	 * @return
	 */
	private void iterativeComputeCheckChanged()
	{
		boolean selected = _useIterativeComputeCheck.isSelected();
		_bottomPanel.setEnabled(selected);
	}

	/**
	 * @param e
	 * @return
	 */
	private void modelAltComboChanged(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange() )
		{
			return;
		}
		if ( isModified())
		{
			if (_selectedModelAlt != null && shouldSaveChanges())
			{
				saveSimInfo();
			}
		}
		
		ModelAlternative modelAlt = (ModelAlternative)_modelAltCombo.getSelectedItem();
		fillModelAltInfo(modelAlt);
	}

	/**
	 * @param modelAlt
	 */
	private void fillModelAltInfo(ModelAlternative modelAlt)
	{
		_selectedModelAlt = modelAlt;
		clearTabPanels();
		if ( modelAlt == null )
		{
			return;
		}
		IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());
		String variantName = _selectedSim.getVariantName();
		modelAlt.setVariantName(variantName);
		ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(modelAlt);
		SensitivitySettings sSettings = iterationSettings.getSensitivitySettings();
		if ( modelAltSettings != null )
		{
			fillPanels(modelAltSettings);
		}
	}

	/**
	 * @param modelAltSettings
	 */
	private void fillPanels(ModelAltIterationSettings modelAltSettings)
	{
		_bcPanel.fillPanel(modelAltSettings);
		
	}

	/**
	 * 
	 */
	private void clearTabPanels()
	{
		// TODO Auto-generated method stub
		System.out.println("clearTabPanels TODO implement me");
		
	}

	/**
	 * @param e
	 * @return
	 */
	private void simComboChange(ItemEvent e)
	{
		if ( ItemEvent.DESELECTED == e.getStateChange() )
		{
			return;
		}
		if ( isModified())
		{
			if (shouldSaveChanges())
			{
				saveSimInfo();
			}
		}
		WatSimulation selectedSim = (WatSimulation)_simCombo.getSelectedItem();
		fillSimInfo(selectedSim);
	}

	/**
	 * @param selectedSim
	 */
	private void fillSimInfo(WatSimulation selectedSim)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			_selectedSim = selectedSim;
			_sensitivityPanel.setSimulation(_selectedSim);
			if ( selectedSim != null )
			{
				_simDescFld.setText(selectedSim.getDescription());
				List<ModelAlternative> modelAlts = selectedSim.getAllModelAlternativeList();

				RmaListModel<ModelAlternative>newModel = new RmaListModel<>(false, modelAlts);
				_modelAltCombo.setModel(newModel);
				if ( newModel.size() == 1 )
				{
					EventQueue.invokeLater(()->_modelAltCombo.setSelectedIndex(0));
				}


				IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());

				boolean isIterative = iterationSettings.isIterative();
				_useIterativeComputeCheck.setSelected(isIterative);

				List<Integer>iterationMembersList = new ArrayList<>();
				int[] members = iterationSettings.getMembersToCompute();
				if ( members != null )
				{
					for (int i= 0; i < members.length;i++ )
					{
						iterationMembersList.add(members[i]);
					}
				}
				_groupMembersFld.setIntegerSet(iterationMembersList);
				_maxMembersFld.setValue(iterationSettings.getMaximumMember());
				iterativeComputeCheckChanged();
				_sensitivityPanel.fillPanel(iterationSettings.getSensitivitySettings());
			}
		}
		finally
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * 
	 */
	private void saveSimInfo()
	{
		if ( _selectedSim != null )
		{
			IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());
			boolean enabled = _useIterativeComputeCheck.isSelected();
			iterationSettings.setIterative(enabled);
			String txt = _groupMembersFld.getText();
			if ( "*".equals(txt))
			{
				
			}
			else
			{
				int[] computeMembers = _groupMembersFld.getIntegerSet();
				iterationSettings.setMembersToCompute(computeMembers);
			}
			iterationSettings.setMaximumMember(_maxMembersFld.getValue());
			if ( _selectedModelAlt != null )
			{
				ModelAltIterationSettings modelAltSettings = iterationSettings.getModelAltSettings(_selectedModelAlt);
				_bcPanel.savePanel(modelAltSettings);
			}
			
			_sensitivityPanel.savePanel(iterationSettings.getSensitivitySettings());
			
			_simGroup.setModified(true);
			setModified(false);
		}
		
	}

	/**
	 * @return
	 */
	private boolean shouldSaveChanges()
	{
		if ( _selectedSim == null )
		{
			return false;
		}
		if ( isModified() )
		{
			int opt = JOptionPane.showConfirmDialog(this, "There are changes for "+_selectedSim+". Save Changes?", "Confirm Changes", JOptionPane.YES_NO_OPTION);
			return opt == JOptionPane.YES_OPTION;
		}
		return false;
	}

	/**
	 * 
	 */
	protected boolean saveForm()
	{
		saveSimInfo();
		setModified(false);
		return true;
		
	}

	class SometimesTabbedPanel extends JPanel
	{
		private JTabbedPane _tabPane;
		private List<AbstractEditorPanel>_panelList = new ArrayList<>();
		SometimesTabbedPanel()
		{
			super(new GridBagLayout());
		}
		
		public void addPanel(AbstractEditorPanel panel)
		{
			if ( panel == null ) 
			{
				return;
			}
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx     = GridBagConstraints.RELATIVE;
			gbc.gridy     = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx   = 1.0;
			gbc.weighty   = 1.0;
			gbc.anchor    = GridBagConstraints.NORTHWEST;
			gbc.fill      = GridBagConstraints.BOTH;
			gbc.insets    = RmaInsets.INSETS5505;
			if(getComponentCount() == 0) 
			{
				
				add(panel, gbc);
				//add(panel, BorderLayout.CENTER);
			}
			else
			{
				if(_tabPane == null)
				{
					_tabPane = new RmaJTabbedPane();
					add(_tabPane, gbc);
					//add(_tabPane,BorderLayout.CENTER);
					for(int i = 0; i < _panelList.size(); i++)
					{
						AbstractEditorPanel tPanel = _panelList.get(i);
						_tabPane.add(tPanel,tPanel.getTabname());
					}
				}
				_tabPane.add(panel, panel.getTabname());
			}
			_panelList.add(panel);

		}
	}

	/**
	 * @param simGroup
	 */
	public void fillForm(SimulationGroup simGroup)
	{
		_simGroup = simGroup;
		_simGroupFld.setText(_simGroup.getName());
		List<WatSimulation> sims = _simGroup.getSimulations();
		List<WatSimulation>simList = new ArrayList<>(sims);
		RmaListModel<WatSimulation> newModel = new RmaListModel<>(true, simList);
		_simCombo.setModel(newModel);
		setModified(false);
	}

	/**
	 * @param watSimulation
	 */
	public void setSelectedSimulation(WatSimulation watSimulation)
	{
		if ( watSimulation != null )
		{
			_simCombo.setSelectedItem(watSimulation);
		}
		else
		{
			_simCombo.setSelectedIndex(-1);
		}
	}
}
