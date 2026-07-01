package usbr.wat.plugins.actionpanel.model;

/**
 * ComputeType is an enumeration defining the supported compute workflow modes for simulation execution.
 * It specifies how the ActionComputable class should run simulations:
 *
 *   Standard: Performs a single standard computation using default options.
 *   Iterative: Runs sensitivity analysis with multiple ensemble members and pre/post scripts.
 *   PositionAnalysis: Performs boundary condition override for position analysis workflows.
 *
 */

public enum ComputeType {
	Standard("Standard"), // Enum constant representing standard single simulation run mode
	Iterative("Iterative"), // Enum constant representing iterative ensemble sensitivity analysis mode
	PositionAnalysis("Position Analysis"); // Enum constant representing position analysis/override computation mode

	private String _name; // Private field storing the human-readable display name for each compute type

	/**
	 * Constructs a ComputeType instance using the provided string representation.
	 * Stores the name which is later used for user interface display and log messages.
	 *
	 * @param name The string name to be displayed in logs, dialogs, or configurations
	 */
	ComputeType(String name) {
		_name = name; // Assign parameter value to private instance field
	}

	/**
	 * Returns the human-readable name for this compute type.
	 * Used primarily for logging, progress dialogs, and user interface labels.
	 *
	 * @return String containing the display name of the compute type (e.g., "Standard", "Iterative")
	 */
	public String getName() {
		return _name; // Return the initialized name value from instance field
	}

	/**
	 * Returns a string representation suitable for debugging or console output.
	 * Overrides default toString() to simply return the computed type's name.
	 *
	 * @return String representation of this ComputeType instance
	 */
	@Override
	public String toString() {
		return getName(); // Return display name directly as string representation
	}
}
