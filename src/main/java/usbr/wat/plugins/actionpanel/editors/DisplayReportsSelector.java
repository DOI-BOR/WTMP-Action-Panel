package usbr.wat.plugins.actionpanel.editors;

import java.awt.Color;                                                      // Color utility used to style UI elements and glass pane overlays
import java.awt.Component;                                                  // Base type for Swing/AWT components used as parents or containers
import java.awt.Cursor;                                                     // AWT cursor utility used to display wait and default cursors during long operations
import java.awt.EventQueue;                                                 // AWT event dispatch utility to schedule tasks on the Event Dispatch Thread (EDT)
import java.awt.GridBagConstraints;                                         // Layout constraints object for positioning components in a grid-based layout
import java.awt.GridBagLayout;                                              // Grid-based layout manager for arranging components in rows and columns
import java.awt.event.ActionEvent;                                          // Event class for action callbacks (e.g., button presses)
import java.awt.event.ItemEvent;                                            // Event class for item selection changes in combo boxes
import java.awt.event.MouseEvent;                                           // Mouse event class used for tooltips and node selection in trees
import java.awt.event.WindowAdapter;                                        // Window adapter providing hooks for open/close lifecycle events
import java.awt.event.WindowEvent;                                          // Window event class used when the dialog is closing

import java.util.ArrayList;                                                 // Resizable list used to collect report plugins and selected rows
import java.util.Enumeration;                                               // Enumeration type used while traversing tree nodes
import java.util.HashMap;                                                   // Map implementation used to store plugin-to-report mappings
import java.util.Iterator;                                                  // Iterator used to traverse selections and map entries
import java.util.List;                                                      // Collections interface used for lists of report infos and templates
import java.util.Map;                                                       // Map interface for collecting selected report configurations
import java.util.Map.Entry;                                                 // Map entry interface used during iteration over plugin-report mappings
import java.util.Set;                                                       // Set interface used to iterate over plugin keys
import java.util.Vector;                                                    // Legacy vector class used by some RMA UI helpers for combo models
import java.util.concurrent.Callable;                                       // Concurrency interface representing a task that returns a value
import java.util.concurrent.ExecutionException;                             // Concurrency exception type indicating a task execution failure
import java.util.concurrent.ExecutorService;                                // Thread pool interface for running report creation tasks concurrently
import java.util.concurrent.Executors;                                      // Factory for creating thread pools
import java.util.concurrent.Future;                                         // Result handle returned by asynchronous tasks
import java.util.concurrent.TimeUnit;                                       // Time unit constants used when awaiting termination
import java.util.logging.Logger;                                            // JDK logging API used for diagnostics
import java.util.prefs.Preferences;                                         // Preferences API used to persist user selections across sessions
import java.util.stream.Collectors;                                         // Stream helpers for transforming collections (e.g., file lists to wrapper objects)

import javax.swing.Icon;                                                    // Swing icon base type used for table cell rendering
import javax.swing.ImageIcon;                                               // Swing image icon used with label/icon objects
import javax.swing.JButton;                                                 // Swing push button component used for command panels
import javax.swing.JComboBox;                                               // Swing combo box used for report type and template selection
import javax.swing.JOptionPane;                                             // Swing utility for showing information and confirmation dialogs
import javax.swing.JPanel;                                                  // Swing container panel used to group controls
import javax.swing.JTree;                                                   // Swing tree component used for checkbox trees and tooltips
import javax.swing.SwingWorker;                                             // Swing worker used to perform background tasks with EDT callbacks
import javax.swing.event.TableModelEvent;                                   // Swing table event used to detect edits and changes
import javax.swing.tree.TreeNode;                                           // Swing tree node interface used by custom checkbox trees
import javax.swing.tree.TreePath;                                           // Swing tree path used to navigate and expand/collapse nodes

import com.rma.io.FileManagerImpl;                                          // File manager implementation that provides filesystem operations
import com.rma.model.Project;                                               // Accessor for the current project and project-level operations
import com.rma.swing.tree.DefaultCheckBoxNode;                              // Checkbox node implementation used by the RMA checkbox tree
import rma.swing.ButtonCmdPanel;                                            // RMA command panel with OK/Close buttons
import rma.swing.ButtonCmdPanelListener;                                    // Listener interface for button command panels
import rma.swing.RmaImage;                                                  // RMA image utility for loading icons by name
import rma.swing.RmaInsets;                                                 // Standardized insets utility for consistent component padding and spacing
import rma.swing.RmaJComboBox;                                              // Base dialog class with RMA-specific behaviors used for plugin windows
import rma.swing.RmaJDialog;                                                // Base RMA dialog with modality and convenience helpers
import rma.swing.RmaJTable;                                                 // RMA table component providing convenience editors and models
import rma.swing.list.RmaListModel;                                         // RMA list model used for combo box models and lists
import rma.swing.tree.CheckBoxTreeRenderer;                                 // Renderer for checkbox trees
import rma.swing.tree.LabelIconObject;                                      // Interface for label/icon objects displayed in trees and tables
import rma.swing.tree.NodeSelectionListener;                                // Listener that toggles checkbox selection when clicking nodes

import hec.util.AnimatedWaitGlassPane;                                      // Animated glass pane used to indicate progress and block UI interaction
import hec2.wat.WAT;                                                        // WAT application entry point (used for preferences and frame access)

import net.sf.jasperreports.engine.util.ReportCreator;                      // JasperReports helper providing a task-like creator interface (imported, used via net.sf.jasperreports.engine.util.ReportCreator)

import rma.util.RMAFilenameFilter;                                          // Filename filter used to select CSV report templates
import rma.util.RMAIO;                                                      // RMA I/O utility helpers for path operations and safe concatenation

import usbr.wat.plugins.actionpanel.ActionsWindow;                          // Main actions window used as the UI parent for dialogs and context
import usbr.wat.plugins.actionpanel.actions.DisplayReportAction;            // Action that locates and displays the latest generated report
import usbr.wat.plugins.actionpanel.io.ReportOptions;                       // Options bean for report generation settings
import usbr.wat.plugins.actionpanel.model.ForecastReportingPlugin;          // Reporting plugin interface for forecast-specific report actions
import usbr.wat.plugins.actionpanel.model.ReportPlugin;                     // Base reporting plugin interface used by the reports manager
import usbr.wat.plugins.actionpanel.model.ReportsManager;                   // Manager that provides registered reporting plugins available to the UI
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;             // Model holding per-simulation report information and paths
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;                           // Base USBR panel type implemented by calibration and forecast panels

/**
 * Dialog that lets users select which reports to create for one or more simulations.
 *
 * Responsibilities:
 * - Lists simulations with selection checkboxes
 * - Provides report type and template pickers per simulation
 * - Persists user selections in preferences
 * - Runs report generation in the background with progress feedback
 * - Offers to display generated reports upon completion
 */
@SuppressWarnings("serial")
public class DisplayReportsSelector extends RmaJDialog {

	/**
	 * Zero-based column index for the "Selected" checkbox.
	 */
	private static final int REPORT_SELECTED_COL = 0;

	/**
	 * Zero-based column index for the "Simulation" column holding a {@link ReportObject}.
	 */
	private static final int REPORT_PLUGIN_COL = 1;

	/**
	 * Zero-based column index for the "Report Type" column holding a {@link ReportPlugin}.
	 */
	private static final int REPORT_TYPE_COL = 2;

	/**
	 * Zero-based column index for the "Report Template" column holding a {@link TemplateWrapper}.
	 */
	private static final int REPORT_TEMPLATE_COL = 3;

	/**
	 * Relative folder path under the project directory containing type-specific report templates.
	 */
	public static final String REPORTS_DIRS = "reports/types";


	/**
	 * Table listing simulations and their report selections.
	 */
	private RmaJTable _reportTable;

	/**
	 * Command panel hosting OK and Close buttons.
	 */
	private ButtonCmdPanel _cmdPanel;

	/**
	 * Animated progress glass pane shown while creating reports.
	 */
	private AnimatedWaitGlassPane _agp;

	/**
	 * Reference to the original glass pane to restore later.
	 */
	private Component _glassPane;

	/**
	 * Owning actions window used as the dialog parent and context source.
	 */
	private ActionsWindow _parent;

	/**
	 * Tracks whether the dialog was canceled; used by close logic.
	 */
	private boolean _isCanceled;

	/**
	 * Panel container for report controls.
	 */
	private JPanel _reportPanel;

	/**
	 * Panel container for simulation table.
	 */
	private JPanel _simPanel;

	/**
	 * Table listing simulation rows for report creation.
	 */
	private RmaJTable _simTable;

	/**
	 * Parent workflow panel (calibration or forecast) providing report infos and display capability.
	 */
	private UsbrPanel _parentPanel;

	/**
	 * Panel exposing report options (headers/footers, output type, etc.).
	 */
	private ReportOptionsPanel _optionsPanel;

	/**
	 * Combo box for choosing the report type (plugin).
	 */
	private JComboBox _reportTypeCombo;

	/**
	 * Combo box for choosing the report template (CSV).
	 */
	private JComboBox _reportTemplateCombo;

	/**
	 * Placeholder plugin used when no report type has been selected.
	 */
	private ReportPlugin _emptyReportPlugin;

	/**
	 * Creates the report selection dialog and initializes controls, listeners, and layout.
	 *
	 * @param parent      the actions window used as the dialog parent
	 * @param parentPanel the workflow panel that provides simulation report infos and can display files
	 */
	public DisplayReportsSelector(ActionsWindow parent, UsbrPanel parentPanel) {
		// Initialize base RMA dialog (non-modal)
		super(parent, false);

		// Store references for later use
		_parent = parent;
		_parentPanel = parentPanel;

		// Build UI controls and layout
		buildControls();

		// Attach listeners for table edits and command buttons
		addListeners();

		// Size and position the dialog
		pack();
		setSize(500, 500);
		setLocationRelativeTo(getParent());

	}


	/**
	 * Builds and lays out all dialog controls, including the simulations table,
	 * options panel, and command buttons.
	 */
	protected void buildControls() {


		// Prevent default close behavior; we handle saves in setVisible(false)
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Use a grid bag layout for flexible placement of controls
		getContentPane().setLayout(new GridBagLayout());

		// Set the window title
		setTitle("Select Reports to Create");

		// Configure the simulations table and its columns
		String[] headers = new String[]{"Selected", "Simulation", "Report Type", "Report Template"};
		_simTable = new RmaJTable(this, headers);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;

		getContentPane().add(_simTable.getScrollPane(), gbc);

		// The "Simulation" column is label/icon only; disable editing
		_simTable.setColumnEnabled(false, REPORT_PLUGIN_COL);

		// Increase row height slightly for readability
		_simTable.setRowHeight(_simTable.getRowHeight() + 5);

		// Add a checkbox editor to the "Selected" column
		_simTable.setCheckBoxCellEditor(REPORT_SELECTED_COL);

		// Set combo box editors for "Report Type" and "Report Template"
		_reportTypeCombo = _simTable.setComboBoxEditor(REPORT_TYPE_COL, new Vector());

		_reportTemplateCombo = _simTable.setComboBoxEditor(REPORT_TEMPLATE_COL, new Vector());

		// Options panel controls output type and header/footer preferences
		_optionsPanel = new ReportOptionsPanel();

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 0.0;

		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;

		getContentPane().add(_optionsPanel, gbc);

		// Command panel with OK (Create Reports) and Close
		_cmdPanel = new ButtonCmdPanel(ButtonCmdPanel.OK_BUTTON | ButtonCmdPanel.CLOSE_BUTTON);

		JButton button = _cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON);
		button.setText("Create Reports");
		button.setEnabled(false);

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.weightx = 1.0;
		gbc.weighty = 0.0;

		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5555;

		getContentPane().add(_cmdPanel, gbc);
	}

	/**
	 * Enables or disables the "Create Reports" button based on whether
	 * there are any selected rows with both report type and template set.
	 */
	protected void updateCreateReportButtonState() {
		_cmdPanel.getButton(ButtonCmdPanel.OK_BUTTON).setEnabled(!getSelectedReports().isEmpty());
	}


	/**
	 * Attaches listeners for combo box item selection, table edits,
	 * command panel buttons, and window closing events.
	 */
	private void addListeners() {

		// React when the report type combo selection changes
		_reportTypeCombo.addItemListener(e -> reportTypeSelected(e));

		// React when table data changes (e.g., selecting a report type)
		_simTable.getModel().addTableModelListener(e -> tableDataChanged(e));

		// Intercept window close to save selections
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				_isCanceled = false;
				setVisible(false);
			}
		});

		// Wire command panel buttons
		_cmdPanel.addCmdPanelListener(new ButtonCmdPanelListener() {
			public void buttonCmdActionPerformed(ActionEvent e) {
				switch (e.getID()) {
					case ButtonCmdPanel.OK_BUTTON:
						_isCanceled = false;
						createReports();
						break;

					case ButtonCmdPanel.CLOSE_BUTTON:
						setVisible(false);

						break;
				}
			}
		});
	}

	/**
	 * Handles report type selection changes to commit any edits before
	 * dependent controls (templates) are updated.
	 *
	 * @param e the item event signaling a change in selection
	 */
	private void reportTypeSelected(ItemEvent e) {
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		// Commit any pending edits so values can be read reliably
		_simTable.commitEdit(true);
	}

	/**
	 * Responds to changes in the simulations table.
	 * <p>
	 * When the report type changes, fills the corresponding template combo
	 * for the same row. Also updates the state of the "Create Reports" button.
	 *
	 * @param e the table model event indicating what changed
	 */
	private void tableDataChanged(TableModelEvent e) {
		if (e.getColumn() == REPORT_TYPE_COL) {
			int row = e.getFirstRow();

			Object selectedPlugin = _simTable.getValueAt(row, REPORT_TYPE_COL);

			if (selectedPlugin instanceof ReportPlugin) {
				fillReportTemplateCombo(row, (ReportPlugin) selectedPlugin);
			}
		}

		updateCreateReportButtonState();
	}

	/**
	 * Handles item state changes on the report-type combo editor for a row.
	 *
	 * @param e the item event signaling a selection change
	 */
	private void reportTypeComboChanged(ItemEvent e) {
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		int row = _simTable.getEditingRow();

		Object selectedPlugin = _simTable.getValueAt(row, REPORT_TYPE_COL);

		if (selectedPlugin instanceof ReportPlugin) {
			fillReportTemplateCombo(row, (ReportPlugin) selectedPlugin);
		}
	}

	/**
	 * Populates the report-template combo box for the given row based on the selected plugin.
	 * <p>
	 * Looks under the project's reports/types folder for CSV templates belonging
	 * to the selected report type.
	 *
	 * @param row            the table row to update
	 * @param selectedPlugin the plugin whose templates will be listed
	 */
	private void fillReportTemplateCombo(int row, ReportPlugin selectedPlugin) {
		if (selectedPlugin == null || selectedPlugin == _emptyReportPlugin) {
			_simTable.setComboBoxCellEditor(row, REPORT_TEMPLATE_COL, new RmaJComboBox<>());
			_simTable.setValueAt("", row, REPORT_TEMPLATE_COL);

			return;
		}

		String prjDir = Project.getCurrentProject().getProjectDirectory();
		String dir = RMAIO.concatPath(prjDir, REPORTS_DIRS);

		dir = RMAIO.concatPath(dir, RMAIO.userNameToFileName(selectedPlugin.getName()));

		List<String> templates = FileManagerImpl.getFileManager().list(dir, new RMAFilenameFilter("csv", "report CSV files"));

		if (templates != null) {
			List<TemplateWrapper> templatesList = templates.stream().map(e -> new TemplateWrapper(e)).collect(Collectors.toList());
			Vector<TemplateWrapper> vec = new Vector<>(templatesList);

			RmaJComboBox combo = new RmaJComboBox<>(vec);
			_simTable.setComboBoxCellEditor(row, REPORT_TEMPLATE_COL, combo);
		}
	}

	/**
	 * Fills the table with simulation rows and initializes the report type model.
	 *
	 * @param sims the list of simulations for which reports can be created
	 */
	private void fillForm(List<SimulationReportInfo> sims) {

		// Clear any existing rows
		_simTable.deleteCells();

		// Gather available report plugins
		List<ReportPlugin> plugins = ReportsManager.getPlugins();

		boolean canBeComparisionReport = sims.size() > 1;

		ReportPlugin plugin;

		Vector row;

		RmaListModel<ReportPlugin> reportTypeModel = new RmaListModel<>();

		// Placeholder plugin representing "no selection"
		_emptyReportPlugin = new ReportPlugin() {

			@Override
			public boolean createReport(List<SimulationReportInfo> sris, ReportOptions options) {
				return false;
			}

			@Override
			public String getName() {
				return "";
			}

			@Override
			public String getDescription() {
				return "";
			}

			@Override
			public boolean isComparisonReport() {
				return false;
			}

			@Override
			public boolean isIterationReport() {
				return false;
			}

			@Override
			public String getMavenPath() {
				return "";
			}

			public String toString() {
				return "";
			}
		};

		// Populate report type model, excluding forecast-specific plugins
		for (int r = 0; r < plugins.size(); r++) {
			plugin = plugins.get(r);

			if (plugin instanceof ForecastReportingPlugin) {
				continue;
			}

			reportTypeModel.addElement(plugin);
		}

		// Insert the empty plugin at the top
		reportTypeModel.add(0, _emptyReportPlugin);

		// Apply the model to the type combo
		_reportTypeCombo.setModel(reportTypeModel);

		// Add a row for each simulation
		for (int s = 0; s < sims.size(); s++) {
			SimulationReportInfo sim = sims.get(s);

			LabelIconObject lio = new ReportObject(sim);

			row = new Vector(4);
			row.add(Boolean.FALSE);
			row.add(lio);
			row.add(_emptyReportPlugin);

			_simTable.appendRow(row);

		}

		updateCreateReportButtonState();
	}


	/**
	 * Determines whether the given plugin should be selected based on saved preferences.
	 *
	 * @param plugin the report plugin to test
	 * @return true if the plugin appears in previously saved selections; false otherwise
	 */
	private boolean shouldBeSelected(ReportPlugin plugin) {
		Preferences node = WAT.getBrowserFrame().getPreferences().getProjectPreferenceNode().node(ReportOptionsPanel.PREF_NODE);

		int idx = 0;

		String selectedReportName;
		String pluginName = plugin.getName();

		while (true) {
			selectedReportName = node.get("SelectedReport" + idx, null);

			if (pluginName.equalsIgnoreCase(selectedReportName)) {
				return true;
			} else if (selectedReportName == null) {
				return false;
			}

			idx++;
		}
	}

	/**
	 * Returns a mapping of selected report plugins to their associated simulation report infos.
	 *
	 * @return a map from {@link ReportPlugin} to list of {@link SimulationReportInfo}
	 */
	public Map<ReportPlugin, List<SimulationReportInfo>> getSelectedReports() {
		Map<ReportPlugin, List<SimulationReportInfo>> reportsMap = new HashMap<>();
		getSelectedReports(reportsMap);
		return reportsMap;
	}

	/**
	 * Populates the provided map with selected report configurations derived from the table.
	 *
	 * @param reports the map to fill, keyed by plugin and containing report infos
	 * @return the same map instance for chaining
	 */
	protected Map<ReportPlugin, List<SimulationReportInfo>> getSelectedReports(Map<ReportPlugin, List<SimulationReportInfo>> reports) {
		int rowCnt = _simTable.getRowCount();

		Object reportObjectObj;
		Object reportPluginObj;

		List<SimulationReportInfo> sris;

		ReportObject pluginObj;
		ReportPlugin plugin;
		ReportObject ro;

		TemplateWrapper template;

		SimulationReportInfo sri;

		TemplateWrapper reportTemplate;

		Object reportTemplateObj;

		List<Integer> selectedRows = getSelectedRows();

		Iterator<Integer> iter = selectedRows.iterator();

		int r;

		while (iter.hasNext()) {
			r = iter.next();

			reportPluginObj = _simTable.getValueAt(r, REPORT_TYPE_COL);

			if (reportPluginObj == null || reportPluginObj == _emptyReportPlugin) {
				continue;
			}

			reportTemplateObj = _simTable.getValueAt(r, REPORT_TEMPLATE_COL);
			plugin = (ReportPlugin) reportPluginObj;
			sris = reports.get(plugin);

			if (sris == null) {
				sris = new ArrayList<>();
				reports.put(plugin, sris);
			}

			reportObjectObj = _simTable.getValueAt(r, REPORT_PLUGIN_COL);
			ro = (ReportObject) reportObjectObj;
			sri = ro.getSimulationReportInfo();

			if (reportTemplateObj instanceof TemplateWrapper) {
				template = (TemplateWrapper) reportTemplateObj;

				sri.setReportCsvFile(template.getPath());
			}

			sris.add(sri);

		}

		return reports;
	}

	/**
	 * Returns row indices for which the "Selected" checkbox is true and a valid report type is chosen.
	 *
	 * @return a list of selected row indices
	 */
	private List<Integer> getSelectedRows() {
		int rowCnt = _simTable.getRowCount();

		Object reportPluginObj;
		Object selectedObj;
		List<Integer> selectedRows = new ArrayList<>();

		for (int r = 0; r < rowCnt; r++) {
			selectedObj = _simTable.getValueAt(r, REPORT_SELECTED_COL);

			if (selectedObj == null) {
				continue;
			}

			if (!RMAIO.parseBoolean(selectedObj.toString(), false)) {
				continue;
			}

			reportPluginObj = _simTable.getValueAt(r, REPORT_TYPE_COL);

			if (reportPluginObj == null || reportPluginObj == _emptyReportPlugin) {
				continue;
			}

			selectedRows.add(r);
		}

		return selectedRows;
	}

	/**
	 * Runs report creation for the selected plugins and simulations using a background worker.
	 * <p>
	 * Shows an animated glass pane during execution, uses a thread pool to run report tasks,
	 * and provides completion feedback with an option to display the generated reports.
	 */
	protected void createReports() {
		_agp = new AnimatedWaitGlassPane();
		_agp.setTransparency(0.8f);

		setGlassPane("Creating Reports...");

		try {
			// Gather plugins and build mapping for selected reports
			List<ReportPlugin> plugins = ReportsManager.getPlugins();

			Map<ReportPlugin, List<SimulationReportInfo>> pluginReports = getSelectedReports();

			// Size the thread pool to available processors, capped by the number of selected plugins
			int maxThreads = Math.min(pluginReports.size(), Runtime.getRuntime().availableProcessors());

			if (maxThreads < 1) {
				maxThreads = 1;
			}

			ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);


			// Background worker coordinates futures and publishes results to the EDT
			SwingWorker<Void, ReportCreator> worker = new SwingWorker<Void, ReportCreator>() {
				private boolean _successful = true;

				private ReportCreator _failedReport;

				@Override
				public Void doInBackground() {
					// Submit one task per plugin selection
					Set<Entry<ReportPlugin, List<SimulationReportInfo>>> info = pluginReports.entrySet();

					Iterator<Entry<ReportPlugin, List<SimulationReportInfo>>> iter = info.iterator();

					List<Future<ReportCreator>> futures = new ArrayList<>();

					while (iter.hasNext()) {
						Entry<ReportPlugin, List<SimulationReportInfo>> next = iter.next();

						Future<ReportCreator> future = createReport(next.getKey(), next.getValue());

						if (future != null) {
							futures.add(future);
						}

					}

					// Collect results and publish to EDT as they complete
					ReportCreator rv;

					for (int i = 0; i < futures.size(); i++) {
						try {
							rv = futures.get(i).get();

							publish(rv);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					}


					return null;
				}

				// Submits a single report creation task to the thread pool
				private Future<ReportCreator> createReport(ReportPlugin reportPlugin, List<SimulationReportInfo> sris) {
					ReportCreator rc = new ReportCreator(reportPlugin, sris);
					Future<ReportCreator> future = threadPool.submit(rc);
					return future;
				}

				@Override
				public void process(List<ReportCreator> chunks) {
					// Update glass pane messages based on report success/failure
					if (chunks == null || chunks.isEmpty()) {
						return;
					}

					ReportCreator rv;

					for (int i = 0; i < chunks.size(); i++) {
						rv = chunks.get(i);

						if (rv != null) {
							if (!rv.wasReportSuccessFul()) {
								_agp.setMessage("Failed to create report " + rv.getReportPlugin().getName());
								_successful = false;
								_failedReport = rv;
							}
						} else {
							_agp.setMessage("Failed to create report ");
							_successful = false;
							_failedReport = rv;
						}
					}
				}

				@Override
				public void done() {
					// Indicate completion and tear down the glass pane
					_agp.setMessage("Reports Complete");

					try {
						threadPool.awaitTermination(5, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
					}

					try {

					} finally {
						resetGlassPane();
					}

					// Offer to display reports on success; otherwise show an error
					if (_successful) {
						int opt = JOptionPane.showOptionDialog(DisplayReportsSelector.this, "Report Created Successfully",
								"Complete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
								null, new Object[]{"Close", "Display Reports"}, "Close");

						if (opt == 1) {
							DisplayReportAction action = new DisplayReportAction(_parentPanel);

							action.displayReportAction();
						}
					} else {
						JOptionPane.showMessageDialog(DisplayReportsSelector.this,
								"Failed to create report for " + _failedReport._reportPlugin.getName(),
								"Report Failed", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			};

			// Start the background worker
			worker.execute();

		} catch (Exception e) {
			// Log and reset glass pane on failure
			Logger.getLogger(DisplayReportsSelector.class.getName()).warning("Exception running reports " + e);

			resetGlassPane();
		} finally {

		}
	}


	/**
	 * Shows an animated glass pane with a message and wait cursor while work proceeds.
	 *
	 * @param msg the status message to display on the glass pane
	 */
	public void setGlassPane(String msg) {
		// Save current glass pane to restore later
		_glassPane = getGlassPane();

		_agp = new AnimatedWaitGlassPane();
		_agp.setColor(Color.BLACK);
		setGlassPane(_agp);

		_agp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_agp.setMessage(msg);
		_agp.setActive(true);
		_agp.setVisible(true);
	}

	/**
	 * Restores the original glass pane and disables the animated wait pane.
	 */
	public void resetGlassPane() {
		if (_agp != null && _glassPane != null) {
			_agp.setCursor(Cursor.getDefaultCursor());

			_agp.setActive(false);
			_agp.setVisible(false);

			setGlassPane(_glassPane);
			_agp = null;
			_glassPane = null;
		}
	}

	/**
	 * Shows the dialog when simulations with results are available.
	 * Saves selected reports when the dialog is being hidden.
	 *
	 * @param visible true to show the dialog, false to hide it
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			if (!checkSims()) {
				return;
			}
		}

		super.setVisible(visible);

		if (!visible) {
			saveSelectedReports();
		}
	}

	/**
	 * Validates that there is an active simulation group and at least one
	 * selected simulation with existing results, then populates the table.
	 *
	 * Steps:
	 * 1) Verify a simulation group is present; if not, inform the user and stop.
	 * 2) Retrieve selected simulations intended for reporting.
	 * 3) Remove any simulations whose results folder does not exist,
	 *    informing the user for each removal on the Event Dispatch Thread.
	 * 4) If none remain, inform the user that no simulations with results
	 *    are available and stop.
	 * 5) Populate the table with the remaining simulations and proceed.
	 *
	 * @return true when there are one or more simulations with results;
	 *         false otherwise
	 */
	private boolean checkSims() {
		// Ensure a simulation group is selected before proceeding; otherwise, prompt and stop
		if (_parent.getSimulationGroup() == null) {
			JOptionPane.showMessageDialog(_parent, "Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);

			return false;
		}

		// Retrieve the simulations targeted for reporting from the parent panel
		List<SimulationReportInfo> sims = _parentPanel.getSimulationReportInfos();

		// Locals for iteration and folder existence checks
		SimulationReportInfo sri;
		String folder;

		// Filter out simulations that do not have a results folder on disk
		Iterator<SimulationReportInfo> iter = sims.iterator();

		while (iter.hasNext()) {
			sri = iter.next();
			folder = sri.getSimFolder();

			// If results are missing, notify the user on the EDT and remove the simulation from consideration
			if (!FileManagerImpl.getFileManager().fileExists(folder)) {
				final SimulationReportInfo fSri = sri;

				EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(_parent, "Simulation " + fSri.getSimulation().getName()
						+ " has no results so no report can be created for it.", "No Results", JOptionPane.INFORMATION_MESSAGE));

				iter.remove();
			}
		}

		// If no simulations with results remain, inform the user and stop
		if (sims.isEmpty()) {
			EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(_parent, "There are no Simulations with results selected to create reports for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE));

			return false;
		}

		// Populate the dialog table with the remaining valid simulations
		fillForm(sims);

		// Validation passed; allow the dialog to be displayed
		return true;
	}

	/**
	 * Persists the current report option settings and the list of selected
	 * report plugins to the project's preferences.
	 *
	 * Behavior:
	 * 1) Save option values from the options panel (such as output settings).
	 * 2) Retrieve the preferences node associated with the options panel.
	 * 3) Build a mapping of selected report plugins to their simulations.
	 * 4) For each selected plugin, store its name under sequential keys
	 *    (SelectedReport0, SelectedReport1, ...), overwriting any prior values.
	 *
	 * Notes:
	 * - This method only records plugin names, not per-simulation selections.
	 * - Reading the saved selections can be done by iterating the same key pattern.
	 */
	private void saveSelectedReports() {
		// Persist the current report option settings (headers, footers, output type, etc.)
		_optionsPanel.saveSettings();

		// Obtain the preferences node used by the options panel to store selections
		Preferences node = _optionsPanel.getPreferencesNode();

		// Build a map of selected plugins to the simulation reports they will generate
		Map<ReportPlugin, List<SimulationReportInfo>> selectedReports = getSelectedReports();

		// Iterate over the selected plugins (keys)
		Set<ReportPlugin> keys = selectedReports.keySet();
		Iterator<ReportPlugin> iter = keys.iterator();

		// Index used to generate sequential preference keys
		int i = 0;

		// Store each selected plugin name under a numbered preference key
		while (iter.hasNext()) {
			ReportPlugin plugin = iter.next();
			String pluginName = plugin.getName();
			node.put("SelectedReport" + i, pluginName);

			i++;
		}
	}

	/**
	 * Callable task that runs a report via a {@link ReportPlugin} for a set of simulations.
	 *
	 * Provides success/failure status and the plugin used, allowing the UI to update messages.
	 */
	public class ReportCreator
			implements Callable {

		/**
		 * The report plugin to execute.
		 */
		private ReportPlugin _reportPlugin;

		/**
		 * True when the report creation was successful.
		 */
		private boolean _reportRv;

		/**
		 * The simulations included in this report run.
		 */
		private List<SimulationReportInfo> _sris;

		/**
		 * Creates a report task for the specified plugin and simulations.
		 *
		 * @param reportPlugin the plugin that will create the report
		 * @param sris         the simulation report infos to include
		 */
		public ReportCreator(ReportPlugin reportPlugin, List<SimulationReportInfo> sris) {
			super();
			_reportPlugin = reportPlugin;
			_sris = sris;
		}

		/**
		 * Runs the report plugin and returns this object for status inspection.
		 *
		 * @return the same {@code ReportCreator} instance with updated status
		 * @throws Exception if an unexpected error occurs during report creation
		 */
		@Override
		public Object call() throws Exception {
			_agp.setMessage("Creating report for " + _reportPlugin.getName());
			ReportOptions options = _optionsPanel.getReportOptions();

			try {
				_reportRv = _reportPlugin.createReport(_sris, options);
			} catch (Exception e) {
				Logger.getLogger(DisplayReportsSelector.class.getName()).info("Failed to run report " + _reportPlugin.getName()
						+ " Error:" + e);

				e.printStackTrace();
				_reportRv = false;
			}

			return this;
		}

		/**
		 * Indicates whether the report creation was successful.
		 *
		 * @return true if successful; false otherwise
		 */
		public boolean wasReportSuccessFul() {
			return _reportRv;
		}

		/**
		 * Returns the report plugin used for this task.
		 *
		 * @return the report plugin instance
		 */
		public ReportPlugin getReportPlugin() {
			return _reportPlugin;
		}
	}

	/**
	 * Tree component configured for checkbox rendering and node selection.
	 *
	 * Provides tooltip support for report plugin descriptions and bulk expand/collapse.
	 */
	public class CheckboxTree extends JTree {
		/**
		 * Creates the checkbox tree with default renderer and selection listener.
		 */
		public CheckboxTree() {
			super();

			setRowHeight(getRowHeight() + 5);
			setRootVisible(false);
			setCellRenderer(new CheckBoxTreeRenderer());

			addMouseListener(new NodeSelectionListener(this));
			setToolTipText("");
		}


		/**
		 * Returns a tooltip string for the tree node under the mouse cursor.
		 *
		 * If the node at the cursor location is a CheckBoxNode whose user object
		 * is a ReportPlugin, this method returns the plugin's description.
		 * Otherwise, it returns null (no tooltip).
		 *
		 * @param e the mouse event providing the cursor location
		 * @return the description of the ReportPlugin at the cursor, or null if none
		 */
		@Override
		public String getToolTipText(MouseEvent e) {
			// Find the tree path at the mouse coordinates; no tooltip if nothing is under the cursor
			TreePath path = getPathForLocation(e.getX(), e.getY());

			if (path == null) {
				return null;
			}

			// Examine the last path component (the node under the cursor)
			Object pathObj = path.getLastPathComponent();

			// Tooltips are only provided for checkbox nodes that wrap a report plugin
			if (pathObj instanceof CheckBoxNode) {
				// Retrieve the underlying object from the checkbox node
				Object checkBoxObj = ((CheckBoxNode) pathObj).getUserObject();

				// If the wrapped object is a report plugin, return its description as the tooltip
				if (checkBoxObj instanceof ReportPlugin) {
					ReportPlugin ro = (ReportPlugin) checkBoxObj;

					return ro.getDescription();
				}
			}

			// No applicable node or plugin found at the cursor location
			return null;
		}


		/**
		 * Expands or collapses the entire tree starting from the root node.
		 *
		 * This method obtains the root from the tree's model, creates a {@link TreePath}
		 * to that root, and delegates the work to {@link #expandAll(TreePath, boolean)}.
		 *
		 * @param expand true to expand all nodes; false to collapse all nodes
		 */
		public void expandAll(boolean expand) {
			// Obtain the root node from the current tree model
			TreeNode root = (TreeNode) this.getModel().getRoot();

			// Delegate expansion or collapse starting at the root path
			expandAll(new TreePath(root), expand);
		}

		/**
		 * Recursively expands or collapses all nodes beneath the specified parent path.
		 *
		 * Behavior:
		 * 1) Visit the node at the provided path.
		 * 2) For each child, build a child path and recursively process it.
		 * 3) After visiting children, expand or collapse the parent path based on the flag.
		 *
		 * @param parent the path whose node (and descendants) will be expanded or collapsed
		 * @param expand true to expand; false to collapse
		 */
		public void expandAll(TreePath parent, boolean expand) {
			// Resolve the node represented by the provided path
			TreeNode node = (TreeNode) parent.getLastPathComponent();

			// Recursively process all child nodes, if any
			if (node.getChildCount() >= 0) {
				Enumeration e = node.children();

				while (e.hasMoreElements()) {
					// Build a path for the child and recurse
					TreeNode n = (TreeNode) e.nextElement();
					TreePath path = parent.pathByAddingChild(n);
					expandAll(path, expand);
				}
			}

			// Apply the requested expand/collapse state to the current path
			if (expand) {
				expandPath(parent);
			} else {
				collapsePath(parent);
			}
		}
	}

	/**
	 * Label/icon wrapper for a {@link SimulationReportInfo} suitable for table/tree rendering.
	 */
	public class ReportObject implements LabelIconObject {
		/**
		 * Simulation report info displayed by this wrapper.
		 */
		private SimulationReportInfo _sri;

		/**
		 * Icon used to distinguish simulation vs. tabulated result entries.
		 */
		private ImageIcon _icon;


		/**
		 * Creates a new ReportObject for the provided simulation report info.
		 *
		 * Chooses the icon based on whether the info represents a simulation
		 * or a tabulated results entry.
		 *
		 * @param sri the simulation report info to wrap
		 */
		public ReportObject(SimulationReportInfo sri) {
			super();

			// Store the underlying report info
			_sri = sri;

			// Set an icon that reflects the type of entry
			if (_sri.isSimulation()) {
				// Icon used for simulation items
				_icon = RmaImage.getImageIcon("Images/comp16x16.gif");
			} else {
				// Icon used for tabulated results items
				_icon = RmaImage.getImageIcon("Images/tabulate18.gif");
			}
		}

		/**
		 * Returns the underlying simulation report info.
		 *
		 * @return the {@link SimulationReportInfo} instance
		 */
		public SimulationReportInfo getSimulationReportInfo() {
			return _sri;
		}

		/**
		 * Returns the display label used in the table or tree.
		 *
		 * @return the label string
		 */
		@Override
		public String getLabel() {
			return _sri.toString();
		}

		/**
		 * Returns the icon associated with this report object.
		 *
		 * @return the {@link ImageIcon} used to render this item
		 */
		@Override
		public Icon getIcon() {
			return _icon;
		}

		/**
		 * Provides a short name for compact displays.
		 *
		 * @return short label string
		 */
		public String toString() {
			return _sri.getShortName();
		}
	}

	/**
	 * Checkbox node that updates the "Create Reports" button state when selected/deselected.
	 */
	public class CheckBoxNode extends DefaultCheckBoxNode {

		/**
		 * Creates a checkbox node with the given user object in the provided tree.
		 *
		 * @param userObj the object represented by this node
		 * @param tree    the tree hosting the node
		 */
		public CheckBoxNode(Object userObj, JTree tree) {
			super(userObj, tree);
		}

		/**
		 * Updates the "Create Reports" button state when selection changes.
		 *
		 * @param selected true when the node is selected; false otherwise
		 */
		@Override
		public void setSelected(boolean selected) {
			super.setSelected(selected);

			updateCreateReportButtonState();
		}
	}
}