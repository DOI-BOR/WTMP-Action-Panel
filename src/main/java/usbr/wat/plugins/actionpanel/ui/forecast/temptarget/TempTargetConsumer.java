package usbr.wat.plugins.actionpanel.ui.forecast.temptarget;

import usbr.wat.plugins.actionpanel.model.forecast.TemperatureTargetSet;    // Provides TemperatureTargetSet, the model object representing a set of temperature targets

import java.util.List;                                                      // Provides the List interface for the ordered collection of TemperatureTargetSet objects

/**
 * A checked consumer adapter that forwards a list of selected TemperatureTargetSet
 * objects to a TempTargetPanel. It bridges the gap between selection callbacks
 * (which cannot declare checked exceptions) and TempTargetPanel.tempTargetSetsSelected,
 * which may throw TempTargetSaveFailedException.
 *
 * This class is package-private and final; it is not intended for use outside
 * the temperature target UI package or for subclassing.
 *
 * @see TempTargetPanel
 * @see TemperatureTargetSet
 */
final class TempTargetConsumer {
    /**
     * The TempTargetPanel to which accepted temperature target set selections are forwarded.
     */
    private final TempTargetPanel _panel;

    /**
     * Constructs a new TempTargetConsumer that delegates accepted values to the
     * given TempTargetPanel.
     *
     * @param panel the TempTargetPanel that will handle the accepted selection;
     *              must not be null
     */
    TempTargetConsumer(TempTargetPanel panel) {
        _panel = panel;
    }

    /**
     * Forwards the given list of TemperatureTargetSet objects to the associated
     * TempTargetPanel for processing and persistence.
     *
     * @param t the List of TemperatureTargetSet objects selected by the user;
     *          must not be null
     * @throws TempTargetSaveFailedException if the panel fails to save the
     *                                       accepted temperature target sets
     */
    public void accept(List<TemperatureTargetSet> t) throws TempTargetSaveFailedException {
        // Delegate the accepted selection to the panel's handler method
        _panel.tempTargetSetsSelected(t);
    }
}
