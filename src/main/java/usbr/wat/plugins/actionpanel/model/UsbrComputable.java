package usbr.wat.plugins.actionpanel.model;

import com.rma.model.Computable; // RMA Computable interface marking an object as something that can be selected for a compute run

/**
 * Marker interface identifying USBR-specific computable items within the WTMP
 * Action Panel's Git integration compute selector.
 *
 * Extends the RMA Computable interface without adding any additional methods.
 * Its primary purpose is to provide a USBR-specific type that can be distinguished
 * from other Computable implementations at runtime via instanceof checks and
 * generics.
 *
 * Instances of this interface are displayed in the UsgsComputeSelectorDialog
 * tree and stored in the simulation group's compute selection lists.
 */
public interface UsbrComputable extends Computable {
    // No additional methods; the interface inherits all Computable members and
    // serves purely as a USBR-scoped type marker.
}
