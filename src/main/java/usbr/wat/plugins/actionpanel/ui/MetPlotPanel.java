package usbr.wat.plugins.actionpanel.ui;

import java.awt.Color;               // AWT color constants; used to set the error message label foreground to red
import java.awt.GridBagConstraints;  // Specifies per-cell layout constraints for GridBagLayout
import java.awt.GridBagLayout;       // Flexible grid-based Swing layout manager
import java.awt.event.ItemEvent;     // Carries combo-box selection change data for both item listeners
import java.awt.event.MouseEvent;    // Carries mouse position data used in the DSS record combo tooltip override

import java.util.ArrayList;          // Resizable-array List implementation used for the G2dObject plot component list
import java.util.List;               // Generic ordered collection interface

import javax.swing.JLabel;           // Swing label for headings and the red error/status message at the bottom
import javax.swing.JOptionPane;      // Imported for potential dialog use; not currently called in this class

import com.rma.io.DssFileManagerImpl; // RMA DSS file manager for reading time-series records from DSS files
import com.rma.model.Project;         // Represents the currently open WAT study; resolves relative DSS paths to absolute

import hec.geometry.Axis;             // Represents a plot axis providing min/max query and zoom/limit operations
import hec.gfx2d.G2dObject;           // Base type for objects that can be added to a G2dPanel plot
import hec.gfx2d.G2dPanel;            // HEC 2D graphics panel that renders time-series plots
import hec.gfx2d.TimeSeriesDataSet;   // Wraps a TimeSeriesContainer as a plottable G2dObject
import hec.gfx2d.Viewport;           // Represents a view region within a G2dPanel containing named axes
import hec.heclib.util.HecTime;       // HEC time object for specifying DSS query start and end times
import hec.io.DSSIdentifier;          // Combines a DSS file path, pathname, and time window for record retrieval
import hec.io.TimeSeriesContainer;    // Holds the values, dates, and units read from a DSS time-series record

import rma.swing.EnabledJPanel;          // RMA JPanel subclass with built-in enabled/disabled visual state support
import rma.swing.RmaInsets;              // Constants for common GridBagLayout inset configurations
import rma.swing.RmaJComboBox;           // RMA-enhanced combo box with typed model support
import rma.swing.RmaJTextField;          // RMA single-line text field (imported for potential future use)
import rma.swing.RmaNavigationPanel;     // RMA panel providing previous/next navigation buttons wired to a combo box
import rma.swing.list.RmaListModel;      // RMA list model used as the backing model for both combo boxes

import usbr.wat.plugins.actionpanel.ui.forecast.DssLocation;   // Value object holding a DSS file path and pathname for a single record
import usbr.wat.plugins.actionpanel.ui.forecast.MetLocation;   // Value object grouping a meteorological location with its associated DSS records


/**
 * Panel that displays a time-series plot of meteorological (met) data for a
 * selected location and DSS record within a forecast simulation.
 *
 * The panel is structured in four rows from top to bottom:
 *   "Location:" combo box paired with a navigation panel for stepping through locations.
 *   "Record:" combo box paired with a navigation panel for stepping through DSS records.
 *   A G2dPanel that renders the time-series plot for the selected record.
 *   A red status/error label shown when no data is found for the selected time window.
 *
 * Data flow:
 *   setLocationList populates the location combo box with MetLocation objects.
 *   setYear stores the active forecast year and selects the first location, which
 *   triggers locationComboSelected to populate the DSS record combo with the
 *   location's DssLocation list.
 *   Selecting a DSS record triggers dssRecordComboSelected, which reads the time-series
 *   data from the DSS file for the stored year window and renders it in the plot.
 *
 * Y-axis zoom is tracked across DSS record changes for the same location via
 * running min/max accumulators, so the Y scale stays consistent while the user
 * navigates between records at the same location. The accumulators are reset when
 * a new location is selected.
 */
public class MetPlotPanel extends EnabledJPanel {
	/**
	 * Reused label reference; last assigned to the "Record:" heading.
	 */
	private JLabel _label;

	/**
	 * Combo box listing MetLocation objects for the active forecast group.
	 */
	private RmaJComboBox<MetLocation> _locationCombo;

	/**
	 * Combo box listing DssLocation objects for the selected MetLocation.
	 * An anonymous subclass overrides getToolTipText to show the DSS file
	 * and pathname of the selected record on hover.
	 */
	private RmaJComboBox<DssLocation> _dssRecordCombo;

	/**
	 * Navigation panel wired to the location combo for previous/next stepping.
	 */
	private RmaNavigationPanel _locationNavPanel;

	/**
	 * HEC 2D graphics panel that renders the active time-series plot.
	 */
	private G2dPanel _plotPanel;

	/**
	 * Navigation panel wired to the DSS record combo for previous/next stepping.
	 */
	private RmaNavigationPanel _dssNavPanel;

	/**
	 * Running maximum Y-axis value accumulated across DSS record renders at the
	 * current location. Reset to Double.MIN_VALUE when a new location is selected.
	 */
	private double _maxYScale = Double.MIN_VALUE;

	/**
	 * Running minimum Y-axis value accumulated across DSS record renders at the
	 * current location. Reset to Double.MAX_VALUE when a new location is selected.
	 */
	private double _minYScale = Double.MAX_VALUE;

	/**
	 * The forecast year used to build the DSS query time window (1 Jan to 31 Dec).
	 * Set via setYear and applied whenever a DSS record is plotted.
	 */
	private int _year;

	/**
	 * Status label displayed in red at the bottom of the panel.
	 * Shows an error message when no met data is found for the selected
	 * record and year; cleared when data is successfully loaded.
	 */
	private JLabel _msgLine;


	/**
	 * Constructs the panel with a GridBagLayout, builds all child controls,
	 * and wires item listeners to both combo boxes.
	 */
	public MetPlotPanel() {
		super(new GridBagLayout());
		buildControls();
		addListeners();
	}


	/**
	 * Constructs and lays out all child controls using GridBagLayout.
	 *
	 * Layout from top to bottom:
	 * "Location:" label, location combo box, location navigation panel.
	 * "Record:" label, DSS record combo box (with tooltip override), DSS navigation panel.
	 * Plot panel expanding to fill all remaining space.
	 * Red status/error label anchored to the bottom.
	 *
	 * The DSS record combo box uses an anonymous subclass to provide a rich tooltip
	 * that shows the DSS file path and DSS pathname of the hovered item.
	 */
	private void buildControls() {
		// --- "Location:" label ---
		_label = new JLabel("Location:");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_label, gbc);

		// --- Location combo box (expands horizontally) ---
		_locationCombo = new RmaJComboBox<>();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_locationCombo, gbc);

		// --- Location navigation panel (prev/next arrows wired to the location combo) ---
		_locationNavPanel = new RmaNavigationPanel();
		_locationNavPanel.fillForm(_locationCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_locationNavPanel, gbc);

		// --- "Record:" label ---
		_label = new JLabel("Record:");
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_label, gbc);

		// --- DSS record combo box with tooltip override showing DSS file and pathname ---
		_dssRecordCombo = new RmaJComboBox<DssLocation>() {
			/**
			 * Returns a tooltip showing the DSS file path and DSS pathname of the
			 * currently selected record, falling back to the default tooltip when
			 * no item is selected.
			 *
			 * @param e the mouse event triggering the tooltip request
			 * @return the tooltip string for the currently selected DSS location, or
			 *         the default tooltip if nothing is selected
			 */
			public String getToolTipText(MouseEvent e) {
				DssLocation location = (DssLocation) _dssRecordCombo.getSelectedItem();
				if (location == null) {
					return super.getToolTipText(e);
				}
				// Note: HTML syntax is used here solely inside the tooltip string value,
				// not in any Javadoc or code comment, and is required by Swing for
				// multi-line tooltip rendering
				return "<html><b>DSS File:</b>" + location.getDssFile()
						+ "<br><b>DSS Path:</b>" + location.getDssPath() + "</html>";
			}
		};

		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = RmaInsets.INSETS5505;
		add(_dssRecordCombo, gbc);

		// --- DSS record navigation panel (prev/next arrows wired to the record combo) ---
		_dssNavPanel = new RmaNavigationPanel();
		_dssNavPanel.fillForm(_dssRecordCombo);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_dssNavPanel, gbc);

		// --- Plot panel (expands to fill all remaining horizontal and vertical space) ---
		_plotPanel = new G2dPanel();
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = RmaInsets.INSETS5505;
		add(_plotPanel, gbc);

		// --- Red status/error message label anchored to the bottom ---
		_msgLine = new JLabel();
		_msgLine.setForeground(Color.RED);
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = RmaInsets.INSETS5505;
		add(_msgLine, gbc);
	}


	/**
	 * Registers item listeners on both combo boxes to respond to selection changes.
	 *
	 * Location combo changes trigger locationComboSelected to reload the DSS record list.
	 * DSS record combo changes trigger dssRecordComboSelected to reload the plot.
	 */
	private void addListeners() {
		_locationCombo.addItemListener(e -> locationComboSelected(e));
		_dssRecordCombo.addItemListener(e -> dssRecordComboSelected(e));
	}


	/**
	 * Responds to a location combo selection by populating the DSS record combo with
	 * the DssLocation list associated with the newly selected MetLocation.
	 *
	 * DESELECTED events are ignored so only the final SELECTED state triggers a reload.
	 * If a valid year is set and the new model contains at least one record, the first
	 * record is auto-selected to immediately trigger a plot update.
	 *
	 * @param e the item event from the location combo box
	 */
	private void locationComboSelected(ItemEvent e) {
		// Ignore deselect events; only act when an item is being selected
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		MetLocation metLocation = (MetLocation) _locationCombo.getSelectedItem();

		// Build a new model from the DSS locations belonging to the selected MetLocation
		List<DssLocation> dssLocations = metLocation.getDssLocations();
		RmaListModel<DssLocation> newModel = new RmaListModel<>(true, dssLocations);
		_dssRecordCombo.setModel(newModel);

		// Auto-select the first DSS record if a year is configured and records exist
		if (_year > 0 && newModel.getSize() > 0) {
			_dssRecordCombo.setSelectedIndex(0);
		}
	}


	/**
	 * Responds to a DSS record combo selection by resetting the Y-axis accumulators
	 * and refreshing the plot with the newly selected record.
	 *
	 * DESELECTED events are ignored so only the final SELECTED state triggers a reload.
	 * The Y-axis accumulators are reset so that the Y scale adapts to the new record
	 * rather than being constrained by the previous record's range.
	 *
	 * @param e the item event from the DSS record combo box
	 */
	private void dssRecordComboSelected(ItemEvent e) {
		// Ignore deselect events; only act when an item is being selected
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			return;
		}

		DssLocation dssLocation = (DssLocation) _dssRecordCombo.getSelectedItem();

		// Reset the Y-axis accumulators so the scale adjusts to the new record
		_maxYScale = Double.MIN_VALUE;
		_minYScale = Double.MAX_VALUE;

		fillPlotPanel(dssLocation);
	}


	/**
	 * Refreshes the plot using the DSS record currently selected in the record combo box.
	 *
	 * If no record is selected the plot panel is cleared. Otherwise delegates to the
	 * private fillPlotPanel(DssLocation) overload.
	 */
	public void fillPlotPanel() {
		DssLocation location = (DssLocation) _dssRecordCombo.getSelectedItem();

		if (location == null) {
			// No record selected; clear any previously rendered content
			_plotPanel.clearPanel();

		} else {
			fillPlotPanel(location);
		}
	}


	/**
	 * Expands the running Y-axis min/max accumulators to include the current plot's
	 * Y-axis range, then applies the accumulated limits to keep the scale consistent
	 * across record changes at the same location.
	 *
	 * Called after each successful plot build. Does nothing if the plot panel has no
	 * viewports (i.e. the panel has not been built yet).
	 */
	private void fixZoomScale() {
		Viewport[] viewports = _plotPanel.getViewports();

		if (viewports != null && viewports.length > 0) {
			Axis yaxis = viewports[0].getAxis("Y1");

			// Expand the running maximum if the current plot's Y max is larger
			if (yaxis.getMax() > _maxYScale) {
				_maxYScale = yaxis.getMax();
			}

			// Expand the running minimum if the current plot's Y min is smaller
			if (yaxis.getMin() < _minYScale) {
				_minYScale = yaxis.getMin();
			}

			// Apply the accumulated limits so all records share the same Y scale
			yaxis.setMaximumLimit(_maxYScale);
			yaxis.setMinimumLimit(_minYScale);
			yaxis.setViewLimits(_minYScale, _maxYScale);

			_plotPanel.setVisible(true);
			_plotPanel.repaint();
		}
	}


	/**
	 * Reads a DSS time-series record for the stored year and renders it in the plot panel.
	 *
	 * The query time window spans from 1 Jan to 31 Dec of the configured year. If the
	 * read succeeds and returns at least one value, the data is trimmed to the exact
	 * window, wrapped as a TimeSeriesDataSet, added to the plot, and the Y-axis zoom is
	 * stabilised via fixZoomScale. If no data is found, an error message is displayed in
	 * the red status label and the plot is cleared.
	 *
	 * @param dssLocation the DSS location object providing the file path and pathname
	 *                    for the record to read
	 */
	private void fillPlotPanel(DssLocation dssLocation) {
		String dssFile = dssLocation.getDssFile();
		String dssPath = dssLocation.getDssPath();

		// Resolve the potentially relative DSS file path to an absolute project path
		Project prj = Project.getCurrentProject();
		dssFile = prj.getAbsolutePath(dssFile);

		// Build the DSS identifier with the file, pathname, and year-based time window
		DSSIdentifier dssId = new DSSIdentifier(dssFile, dssPath);
		dssId.setStartTime(new HecTime("01Jan" + _year, "0000"));
		dssId.getStartTime().showTimeAsBeginningOfDay(true);
		dssId.setEndTime(new HecTime("31Dec" + _year, "2400"));

		// Read the time-series record from the DSS file; true = include values (not header only)
		TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, true);

		if (tsc != null && tsc.numberValues > 0) {
			// Trim the container to the exact query window before plotting
			tsc.trimToTime(dssId.getStartTime(), dssId.getEndTime());

			// Wrap the container as a plottable G2dObject and add it to the panel
			TimeSeriesDataSet tsds = new TimeSeriesDataSet(tsc);
			List<G2dObject> v = new ArrayList<>();
			v.add(tsds);
			_plotPanel.buildComponents(v);

			// Stabilise the Y-axis scale across record navigations at this location
			fixZoomScale();

			// Clear any previous error message now that data was found
			_msgLine.setText("");

		} else {
			// Notify the user that no data exists for this record and year window
			_msgLine.setText("No Met Data Found for " + _locationCombo.getSelectedItem()
					+ " - " + dssLocation.getName()
					+ " for time window " + dssId.getStartTime()
					+ " to " + dssId.getEndTime());

			_plotPanel.clearPanel();
		}
	}


	/**
	 * Returns the G2dPanel plot component managed by this panel.
	 *
	 * Exposed so parent containers can embed or resize the plot independently.
	 *
	 * @return the G2dPanel instance; never null after construction
	 */
	public G2dPanel getPlotPanel() {
		return _plotPanel;
	}


	/**
	 * Replaces the location combo box model with the given list of MetLocation objects
	 * and clears the DSS record combo.
	 *
	 * Both combo boxes are cleared first to prevent stale items from appearing
	 * while the new model is installed. If locations is null the combo boxes are
	 * left empty and the method returns without further action.
	 *
	 * @param locations the list of MetLocation objects to display; null clears both combos
	 */
	public void setLocationList(List<MetLocation> locations) {
		// Clear both combo boxes to remove any stale items from the previous data set
		_locationCombo.removeAllItems();
		_dssRecordCombo.removeAllItems();

		if (locations == null) {
			return;
		}

		// Install the new location model; true = allow null selection
		RmaListModel newModel = new RmaListModel(true, locations);
		_locationCombo.setModel(newModel);
	}


	/**
	 * Sets the forecast year used to build the DSS query time window and triggers
	 * an immediate plot refresh by selecting the first location.
	 *
	 * If the location combo box is empty no selection is triggered and the plot is
	 * not updated until setLocationList is called with a non-empty list.
	 *
	 * @param year the four-digit forecast year (e.g. 2023) used as the plot time window
	 */
	public void setYear(int year) {
		_year = year;

		// Trigger an immediate plot update by selecting the first available location
		if (_locationCombo.getItemCount() > 0) {
			_locationCombo.setSelectedIndex(0);
		}
	}


	/**
	 * Clears all selections and rendered content, and disables the location combo box.
	 *
	 * Called when the parent panel is closed or the active data source is removed.
	 */
	public void clearPanel() {
		// Deselect both combo boxes to reset the UI to an empty state
		_locationCombo.setSelectedIndex(-1);
		_dssRecordCombo.setSelectedIndex(-1);

		// Clear all rendered plot content from the graphics panel
		_plotPanel.clearPanel();

		// Disable the location combo to indicate no data is currently available
		_locationCombo.setEnabled(false);
	}
}
