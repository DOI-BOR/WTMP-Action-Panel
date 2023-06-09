/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.model;

import java.util.List;
import javax.swing.Action;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.io.ReportOptions;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleReportInfo;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;

public interface ForecastReportingPlugin extends ReportPlugin
{
	Action getReportAction(ActionsWindow parent, UsbrPanel parentPanel);

	public boolean createReport(SimulationReportInfo sims, List<EnsembleReportInfo> ensembleReportInfos, ReportOptions options);
}
