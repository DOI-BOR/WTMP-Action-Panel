package usbr.wat.plugins.actionpanel.model;

/**
 * Position analysis compute settings for a single WAT simulation within the
 * WTMP Action Panel.
 *
 * Extends BaseComputeSettings to inherit the compute-member indices, maximum member
 * value, and per-model-alternative BC DSS assignments. Position analysis differs from
 * iterative compute in that it does not include sensitivity (pre/post-compute script)
 * settings, and the Maximum member field is automatically derived from the DSS time
 * ranges of the configured boundary condition inputs rather than being entered manually.
 *
 * Results from a position analysis run are collected in the DSS file named
 * "positionAnalysisResults.dss" in the simulation output folder.
 *
 * The XML element tag name for this settings type is "PositionAnalysisSetting".
 *
 */
public class PositionAnalysisSettings extends BaseComputeSettings {
	/**
	 * Constructs a PositionAnalysisSettings with default values.
	 * <p>
	 * Delegates to the BaseComputeSettings superclass constructor.
	 */
	public PositionAnalysisSettings() {
		super();
	}

	/**
	 * Returns the XML element tag name used to identify this settings type during serialization.
	 *
	 * @return "PositionAnalysisSetting"
	 */
	@Override
	protected String getSettingElementString() {
		return "PositionAnalysisSetting";
	}

	/**
	 * Returns the file name of the DSS file where position analysis results are collected.
	 *
	 * @return "positionAnalysisResults.dss"
	 */
	@Override
	public String getCollectionDssFilename() {
		return "positionAnalysisResults.dss";
	}
}
