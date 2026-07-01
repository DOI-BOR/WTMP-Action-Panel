package usbr.wat.plugins.actionpanel.model.forecast;

import hec.lang.NamedType; // HEC base class providing a name field and modification tracking

/**
 * Named data object representing a meteorological dataset within a ForecastSimGroup.
 *
 * Acts as a lightweight named placeholder for a meteorological data scenario (such as
 * a historical or synthetic weather dataset) that can be selected when configuring a
 * BcData boundary condition for an ensemble forecast run.
 *
 * The class currently holds only the display name inherited from NamedType. Additional
 * fields for file paths or DSS identifiers may be added in future versions as the
 * meteorological data model is expanded.
 *
 * Instances are loaded by ForecastSimGroup from the "Meteorology" XML element and
 * resolved by name in BcData when loading boundary condition configurations.
 *
 */
public class MetData extends NamedType {
	/**
	 * Constructs an empty MetData with no name set.
	 *
	 * The name should be set via setName() or populated from XML after construction.
	 */
	public MetData() {
		super();
	}
}
