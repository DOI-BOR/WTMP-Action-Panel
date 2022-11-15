
package usbr.wat.plugins.actionpanel.editors;

import java.awt.CardLayout;
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
import rma.swing.RmaJComboBox;
import rma.swing.RmaJDescriptionField;
import rma.swing.RmaJDialog;
import rma.swing.RmaJTabbedPane;
import rma.swing.RmaJTextField;
import rma.swing.list.RmaListModel;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.IterationPanel;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.PositionAnalysisPanel;
import usbr.wat.plugins.actionpanel.model.ComputeType;
import usbr.wat.plugins.actionpanel.model.IterationSettings;
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;
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
	private RmaJComboBox<ComputeType> _computeTypeCombo;
	
	private RmaJComboBox<ModelAlternative> _modelAltCombo;
	private JTabbedPane _iterationPanelForTabs;
	private ButtonCmdPanel _cmdPanel;
	private SimulationGroup _simGroup;
	private WatSimulation _selectedSim;
	private EnabledJPanel _bottomPanel;
	private ModelAlternative _selectedModelAlt;
	

	private JPanel _cardPanel;
	private IterationPanel _iterationPanel;
	private PositionAnalysisPanel _posAnalysisPanel;
	private boolean _fillSimInfo;
	/**
	 * @param parent
	 */
	public EditIterationSettingsDialog(ActionsWindow parent)
	{
		super(parent, true);
		buildControls();
		addListeners();
		pack();
		setSize(800,700);
		setLocationRelativeTo(getParent());
	}

	/**
	 * 
	 */
	private void buildControls()
	{
		getContentPane().setLayout(new GridBagLayout());
		setTitle("Edit Compute Settings");
		
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
	
		
		computeTypeChanged();
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
		_simCombo.setModifiable(false);
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
		
		label = new JLabel("Compute Type:");
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1; 
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(label, gbc);	
		
		_computeTypeCombo = new RmaJComboBox<>(ComputeType.values());
		_computeTypeCombo.setEnabled(false);
		_computeTypeCombo.setModifiable(true);
		label.setLabelFor(_computeTypeCombo);
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 0.0;
		gbc.weighty   = 0.0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_computeTypeCombo, gbc);	
		
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
		
			
		JLabel label = new JLabel("Model Alternative:");
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
		
		_cardPanel = new JPanel(new CardLayout());
		gbc.gridx     = GridBagConstraints.RELATIVE;
		gbc.gridy     = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx   = 1.0;
		gbc.weighty   = 1.0;
		gbc.anchor    = GridBagConstraints.NORTHWEST;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.insets    = RmaInsets.INSETS5505;
		panel.add(_cardPanel, gbc);
		
		
		_cardPanel.add(new JPanel(), ComputeType.Standard.getName());
		
		_iterationPanel = new IterationPanel(this);
		_cardPanel.add(_iterationPanel, ComputeType.Iterative.getName());
		
		_posAnalysisPanel = new PositionAnalysisPanel(this);
		_cardPanel.add(_posAnalysisPanel, ComputeType.PositionAnalysis.getName());
		
		((CardLayout)_cardPanel.getLayout()).show(_cardPanel, ComputeType.Standard.getName());
		
	}
	/**
	 * 
	 */
	private void addListeners()
	{
		_simCombo.addItemListener(e->simComboChange(e));
		_modelAltCombo.addItemListener(e->modelAltComboChanged(e));
		_computeTypeCombo.addActionListener(e->computeTypeChanged());
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
						break;
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
	private void computeTypeChanged()
	{
		double numYears = 0.;
		if ( _selectedSim != null )
		{	
			numYears = _selectedSim.getRunTimeWindow().getNumberOfYears();
		}
		ComputeType type = (ComputeType) _computeTypeCombo.getSelectedItem();
		if ( numYears > 1.0 && type == ComputeType.PositionAnalysis )
		{
			JOptionPane.showMessageDialog(this, "Position Analysis Computes can only be performed on Simulations with a time window of 1 year or less.","Time Window too long", JOptionPane.INFORMATION_MESSAGE );
			EventQueue.invokeLater(()->_computeTypeCombo.setSelectedItem(ComputeType.Standard));
			return;
		}
		boolean enabled = type != ComputeType.Standard;
		
		_bottomPanel.setEnabled(enabled);
		
		((CardLayout)_cardPanel.getLayout()).show(_cardPanel, type.getName());
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
		if ( isModified() && _selectedModelAlt != null )
		{
			if (_selectedModelAlt != null && shouldSaveChanges(_selectedModelAlt.getName()))
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
		_iterationPanel.fillPanel(modelAlt, iterationSettings);
		
		
		PositionAnalysisSettings posAnalysisSettings = _simGroup.getPositionAnalysisSettings(_selectedSim.getName());
		_posAnalysisPanel.fillPanel(modelAlt, posAnalysisSettings);
		EventQueue.invokeLater(()->setModified(false));
		
	}

	
	/**
	 * 
	 */
	private void clearTabPanels()
	{
		
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
		EventQueue.invokeLater(()->simComboChanged());
	}
	private void simComboChanged ()
	{
		if ( isModified()&& _selectedSim != null )
		{
			if (shouldSaveChanges(_selectedSim.getName()))
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
			_fillSimInfo = true;
			_selectedSim = selectedSim;
			_iterationPanel.setSimulation(_selectedSim);
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
				
				ComputeType computeType = _simGroup.getComputeType(_selectedSim.getName());
				_computeTypeCombo.setSelectedItem(computeType);
				_computeTypeCombo.setEnabled(true);
				computeTypeChanged();
				 
				_selectedModelAlt = null;
				EventQueue.invokeLater(()->setModified(false));
			}
		}
		finally
		{
			_fillSimInfo = false;
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * 
	 */
	private boolean saveSimInfo()
	{
		if ( isValidForm())
		{
			ComputeType computeType = (ComputeType) _computeTypeCombo.getSelectedItem();
			_simGroup.setComputeType(_selectedSim.getName(), computeType);
			
			IterationSettings iterationSettings = _simGroup.getIterationSettings(_selectedSim.getName());
			_iterationPanel.savePanel(iterationSettings);
			
			
			PositionAnalysisSettings posAnalysisSettings = _simGroup.getPositionAnalysisSettings(_selectedSim.getName());
			_posAnalysisPanel.savePanel(posAnalysisSettings);
			
			_simGroup.setModified(true);
			setModified(false);
			return true;
		}
		return false;
		
	}

	/**
	 * @return
	 */
	private boolean isValidForm()
	{
		if ( _selectedSim != null )
		{
			if ( _computeTypeCombo.getSelectedIndex() < 0 )
			{
				return true;
			}
			ComputeType computeType = (ComputeType) _computeTypeCombo.getSelectedItem();
			
			if ( computeType == ComputeType.Iterative && !_iterationPanel.isValidForm())
			{
				return false;
			}
			else if ( computeType == ComputeType.PositionAnalysis )
			{
				
			}
			
			return true;
			
		}
		return false;
	}

	/**
	 * @return
	 */
	private boolean shouldSaveChanges(String name)
	{
		if ( _fillSimInfo )
		{
			return false;
		}
		if ( isModified() )
		{
			int opt = JOptionPane.showConfirmDialog(this, "There are changes for "+name+". Save Changes?", "Confirm Changes", JOptionPane.YES_NO_OPTION);
			return opt == JOptionPane.YES_OPTION;
		}
		return false;
	}

	/**
	 * 
	 */
	protected boolean saveForm()
	{
		if(!saveSimInfo())
		{
			return false;
		}
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
		if ( simList.size() == 1 )
		{
			_simCombo.setSelectedIndex(0);
		}
		EventQueue.invokeLater(()->setModified(false));
	}
	
	@Override
	public void setModified(boolean modified)
	{
		super.setModified(modified);
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

	

	/**
	 * @return
	 */
	public WatSimulation getSelectedSimulation()
	{
		return _selectedSim;
	}

	/**
	 * @return
	 */
	public ModelAlternative getSelectedModelAlternative()
	{
		return _selectedModelAlt;
	}
}
