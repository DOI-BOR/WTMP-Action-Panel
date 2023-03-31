/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.ui;

import java.util.List;

import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;
import usbr.wat.plugins.actionpanel.ui.tree.SimulationTreeTable;

/**
 * @author mark
 *
 */
public interface UsbrPanel
{

	/**
	 * @return
	 */
	void editSimulationMetaData();

	/**
	 * @return
	 */
	void displayComputeLog();

	/**
	 * @return
	 */
	void showInProjectTreeAction();

	/**
	 * @return
	 */
	void displaySimulationInMap();

	/**
	 * @return
	 */
	void displayReport();

	/**
	 * 
	 */
	void fillSimulationTable();

	/**
	 * @return
	 */
	List<SimulationReportInfo> getSimulationReportInfos();

	/**
	 * @param latestFile
	 */
	void displayFile(String latestFile);

	/**
	 * @return
	 */
	SimulationTreeTable getSimulationTreeTable();

	/**
	 * 
	 */
	void updateComputeStates();

}
