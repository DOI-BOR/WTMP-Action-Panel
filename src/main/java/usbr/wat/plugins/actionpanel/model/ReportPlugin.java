package usbr.wat.plugins.actionpanel.model;

import java.util.List; // Ordered collection interface for the list of SimulationReportInfo objects passed to createReport

import usbr.wat.plugins.actionpanel.io.ReportOptions; // Carries user-selected output type and header/footer flag for the report

/**
 * Plugin interface for report generators within the WTMP Action Panel.
 *
 * Implementations of this interface are registered with ReportsManager and
 * presented to the user as report format options. Each plugin encapsulates a
 * single report style (e.g., a specific Excel template or PDF layout) and
 * declares whether it is applicable to comparison reports (multiple simulations
 * side-by-side) or iteration reports (showing results across compute iterations).
 *
 * Plugins are discovered via Maven/classloader registration and identified by
 * name, description, and Maven artifact path.
 *
 */
public interface ReportPlugin {
	/**
	 * Generates a report for the given list of simulation report information objects
	 * using the provided output options.
	 *
	 * @param sris    the list of SimulationReportInfo objects describing each simulation to include
	 * @param options the user-selected output type (PDF, Excel, etc.) and header/footer settings
	 * @return true if the report was created successfully; false otherwise
	 */
	boolean createReport(List<SimulationReportInfo> sris, ReportOptions options);

	/**
	 * Returns the display name of this report plugin as shown in the report selection UI.
	 *
	 * @return the plugin's display name string
	 */
	String getName();

	/**
	 * Returns a description of this report plugin's output format or content.
	 *
	 * @return the plugin's description string
	 */
	String getDescription();

	/**
	 * Returns whether this plugin generates a comparison report (comparing multiple simulations).
	 *
	 * @return true if this plugin produces a comparison report; false otherwise
	 */
	boolean isComparisonReport();

	/**
	 * Returns whether this plugin generates an iteration report (showing results across compute iterations).
	 *
	 * @return true if this plugin produces an iteration report; false otherwise
	 */
	boolean isIterationReport();

	/**
	 * Returns the Maven artifact path used to locate and load this plugin.
	 *
	 * @return the Maven path string for this plugin's artifact
	 */
	String getMavenPath();
}
