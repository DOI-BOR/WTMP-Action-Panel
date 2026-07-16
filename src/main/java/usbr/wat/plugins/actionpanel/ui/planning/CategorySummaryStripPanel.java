package usbr.wat.plugins.actionpanel.ui.planning;

import java.awt.GridBagConstraints;          // Layout constraints for positioning each category box in a horizontal row
import java.awt.GridBagLayout;                // Flexible grid-based layout manager
import java.util.LinkedHashMap;               // Preserves box display order (Initial Conditions -> ... -> Temperature Target Sets)
import java.util.Map;                         // Maps each tab name to its corresponding mini-list box

import javax.swing.BorderFactory;             // Builds the plain and highlighted borders used to indicate the active category
import javax.swing.JList;                     // Read-only mini list of item names shown inside each category box
import javax.swing.JPanel;                    // Container for each individual category box (label + list)
import javax.swing.JScrollPane;               // Makes each box's list scrollable when it has more entries than fit
import javax.swing.ListModel;                 // Model type each AbstractPlanningPanel exposes for its box's contents
import javax.swing.border.Border;             // Border type used for the plain/highlighted box borders
import javax.swing.border.TitledBorder;       // Titled border showing each box's category name, matching the mockup

/**
 * The horizontal strip of small summary boxes shown along the upper portion of the
 * Planning tab, directly below the Set and Simulation Group rows, mirroring the same
 * strip already present in the Forecast Conditions workflow.
 *
 * One box is shown for each of the five left-hand sub-tabs other than Simulation —
 * Initial Conditions, Operations, Meteorology, Boundary Conditions, and Temperature Target
 * Sets — each displaying a short, read-only preview list of that category's current
 * entries for the active Set/Simulation Group pairing. The box matching the currently
 * selected left-hand tab is shown with a highlighted border, exactly as in the mockup
 * (where the active tab's box is bordered and enlarged relative to its neighbors).
 */
@SuppressWarnings("serial")
public class CategorySummaryStripPanel extends JPanel {

	// Plain border used for every box except the currently active one
	private static final Border PLAIN_BORDER = BorderFactory.createEtchedBorder();

	// Highlighted border used for the box matching the currently selected left-hand tab
	private static final Border HIGHLIGHT_BORDER = BorderFactory.createLineBorder(java.awt.Color.BLACK, 2);

	// Ordered map of tab name -> the JList used inside that tab's summary box
	private final Map<String, JList<Object>> _listsByTabName = new LinkedHashMap<>();

	// Ordered map of tab name -> the box panel itself, so its border can be toggled
	private final Map<String, JPanel> _boxesByTabName = new LinkedHashMap<>();

	/**
	 * Constructs an empty strip; call {@link #addCategoryBox(String, ListModel)} once for
	 * each of the five non-Simulation sub-tabs, in display order, before this panel is shown.
	 */
	public CategorySummaryStripPanel() {
		super(new GridBagLayout()); // Boxes are laid out left-to-right using GridBagLayout
	}

	/**
	 * Adds one category box to the strip, bound to the given list model so its contents
	 * stay live as the underlying sub-tab panel's data changes.
	 *
	 * @param tabName   the category name (matches the corresponding sub-tab's
	 *                  {@code getTabName()}), used as both the box's title and lookup key
	 * @param listModel the list model backing this box's preview list, typically the same
	 *                  model instance the owning {@code AbstractPlanningPanel} subclass uses
	 *                  for its own detail list
	 */
	public void addCategoryBox(String tabName, ListModel<Object> listModel) {
		JPanel box = new JPanel(new java.awt.BorderLayout()); // One box per category, title + scrollable list
		box.setBorder(new TitledBorder(PLAIN_BORDER, tabName)); // Start out with the plain (non-highlighted) border

		JList<Object> list = new JList<>(listModel); // Bind directly to the caller's model so updates stay live
		list.setEnabled(false); // Read-only preview; selection/interaction happens on the real sub-tab, not here
		box.add(new JScrollPane(list), java.awt.BorderLayout.CENTER); // Wrap in a scroll pane in case of overflow

		GridBagConstraints gbc = new GridBagConstraints(); // Fresh constraints for this box's position in the strip
		gbc.gridx = GridBagConstraints.RELATIVE; // Each box takes the next column to the right
		gbc.gridy = 0; // All boxes sit on a single row
		gbc.weightx = 1.0; // Distribute extra horizontal space evenly across boxes
		gbc.weighty = 1.0; // Allow the box to grow to fill the strip's height
		gbc.fill = GridBagConstraints.BOTH; // Stretch the box to fill its cell in both directions
		gbc.insets = new java.awt.Insets(2, 2, 2, 2); // Small gap between neighboring boxes
		add(box, gbc); // Add the finished box to the strip

		_listsByTabName.put(tabName, list); // Remember the list for potential future lookups
		_boxesByTabName.put(tabName, box); // Remember the box so its border can be toggled later
	}

	/**
	 * Highlights the box matching {@code activeTabName} and returns all others to their
	 * plain border, matching the mockup's behavior of bordering/enlarging the box for the
	 * currently selected left-hand tab.
	 *
	 * @param activeTabName the tab name of the newly selected left-hand tab; boxes for tabs
	 *                      without a corresponding box (i.e. "Simulation") are simply left
	 *                      at their plain border
	 */
	public void setActiveTab(String activeTabName) {
		for (Map.Entry<String, JPanel> entry : _boxesByTabName.entrySet()) { // Walk every registered box
			boolean active = entry.getKey().equals(activeTabName); // True only for the newly selected tab's box
			// Rebuild the border using the highlighted style for the active box, plain for all others
			entry.getValue().setBorder(new TitledBorder(active ? HIGHLIGHT_BORDER : PLAIN_BORDER, entry.getKey()));
		}
	}
}
