/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.io;

import java.util.Date;
import java.util.List;

import com.google.common.flogger.FluentLogger;
import org.jdom.Document;
import org.jdom.Element;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.util.XMLUtilities;

import hec.heclib.util.HecTime;
import hec.model.RunTimeWindow;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.ComputeOptions;
import hec2.wat.model.WatSimulation;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPlugin;
import hec2.wat.plugin.WatPluginManager;
import hec2.wat.plugin.ceQualW2.CeQualW2Plugin;
import hec2.wat.plugin.ceQualW2.model.CeQualW2Alt;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.actions.AbstractReportAction;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;

/**
 * @author Mark Ackerman
 *
 *
  <SimulationReport>
	<ReportType>single</ReportType>
	<Study>
		<Directory>J:/studies/study1</Directory>
		<ObservedData>j:/studies/study1/shared</ObservedData>
	</Study>
	<Simulations>
		<Simulation>
			<Name>sim1-group1</Name>
			<BaseName>sim1</BaseName>
			<Directory>j:/studies/study1/runs/al1/ap1</Directory>
			<DSSFile>j:/studies/study1/runs/al1/ap1/sim1.dss</DSSFile>
			<StartTime>01jan2000 1500</StartTime>
			<EndTime>31jan2000 1500</StartTime>
			<LastComputed>7Jul2021 1400</LastComputed>
			<ModelAlternatives>
				<ModelAlternative>
					<Name>alt1</Name>
					<Program>CE-QUAl-W2</Program>
					<FPart>some fpart</FPart>
					<Directory>path to simulation compute folder for the model</Directory>
				</ModelAlternative>
				<ModelAlternative>
					<Name>alt2</Name>
					<Program>ResSim</Program>
					<FPart>some fpart</FPart>
					<Directory>path to simulation compute folder for the model</Directory>
				</ModelAlternative>
			</ModelAlternatives>
		</Simulation>
	</Simulations>
</SimulationReport>
 */
public class ReportXmlFile
{
	// XML elements
	private static final String SIM_REPORT_ELEM = "SimulationReport";
	private static final String REPORT_TYPE_ELEM = "ReportType";
	
	private static final String STUDY_ELEM = "Study";
	private static final String PRJ_DIR_ELEM = "Directory";
	private static final String OBS_DIR_ELEM = "ObservedData";
	
	private static final String SIMS_ELEM = "Simulations";
	private static final String SIM_ELEM = "Simulation";
	private static final String SIM_NAME_ELEM = "Name";
	private static final String ID_ELEM = "ID";
	private static final String BASE_SIM_NAME_ELEM = "BaseName";
	private static final String SIM_DIR_ELEM = "Directory";
	private static final String SIM_DSS_FILE_ELEM = "DSSFile";
	private static final String SIM_START_TIME_ELEM = "StartTime";
	private static final String SIM_END_TIME_ELEM = "EndTime";
	private static final String SIM_LAST_COMPUTED_ELEM = "LastComputed";
	
	private static final String MODEL_ALTS_ELEM = "ModelAlternatives";
	private static final String MODEL_ALT_ELEM = "ModelAlternative";
	private static final String MODEL_ALT_NAME_ELEM = "Name";
	private static final String MODEL_ALT_PROGRAM_ELEM = "Program";
	private static final String MODEL_ALT_FPART_ELEM = "FPart";
	private static final String MODEL_ALT_FOLDER_ELEM = "Directory";
	
	
	// report types
	private static final String SINGLE_TYPE = "single";
	private static final String COMPARISON_TYPE = "alternativecomparison";

	// ID types
	private static final String BASE_ALT = "base";
	private static final String ALT_NUM = "alt_";
	private static final String INSTALL_DIR_ELEM = "InstallDirectory";
	private static final String WRITE_DIR_ELEM = "WriteDirectory";
	
	
	private String _fileName;
	private String _prjDir;
	private String _obsDir;
	private List<SimulationReportInfo> _simulationInfos;
	private String _simGroupName;

	public ReportXmlFile(String fileName)
	{
		super();
		_fileName = fileName;
	}
	
	public void setStudyInfo(String studyDir, String obsDir)
	{
		_prjDir = studyDir;
		_obsDir = obsDir;
	}
	
	public void setSimulationInfo(String simGroupName, List<SimulationReportInfo> infos)
	{
		_simGroupName = simGroupName;
		_simulationInfos = infos;
	}
	
	public boolean createXMLFile()
	{
		Element root = new Element(SIM_REPORT_ELEM);
		Document doc = new Document(root);

		
		String rptType = getReportType();


		XMLUtilities.addChildContent(root, REPORT_TYPE_ELEM, rptType);
		SimulationReportInfo baseSimulation = _simulationInfos.get(0);
		addProjectInfo(root, baseSimulation.getSimFolder());
		
		Element simsElem  = new Element(SIMS_ELEM);
		root.addContent(simsElem);
		for (int i = 0;i < _simulationInfos.size(); i++ )
		{
			addSimulationInfo(simsElem, _simulationInfos.get(i), i);
		}
		
		RmaFile file = FileManagerImpl.getFileManager().getFile(_fileName);
		return XMLUtilities.saveDocument(doc, file);
					
	}

	protected String getReportType()
	{
		String rptType = SINGLE_TYPE;
		if ( _simulationInfos.size() > 1 )
		{
			rptType = COMPARISON_TYPE;
		}
		return rptType;
	}

	/**
	 * @param parent
	 * @param info
	 */
	private void addSimulationInfo(Element parent, SimulationReportInfo info, int simNumber)
	{
		Element simElem = new Element(SIM_ELEM);
		parent.addContent(simElem);
		XMLUtilities.addChildContent(simElem, SIM_NAME_ELEM, info.getShortName());
		XMLUtilities.addChildContent(simElem, ID_ELEM, (simNumber==0?BASE_ALT:ALT_NUM+simNumber));
		
		XMLUtilities.addChildContent(simElem, BASE_SIM_NAME_ELEM, getBaseSimulationName(info.getSimulation().getName()));
		XMLUtilities.addChildContent(simElem, SIM_DIR_ELEM, info.getSimFolder());
		XMLUtilities.addChildContent(simElem, SIM_DSS_FILE_ELEM, info.getSimDssFile());
		
		RunTimeWindow rtw = info.getSimulation().getRunTimeWindow();
		XMLUtilities.addChildContent(simElem, SIM_START_TIME_ELEM, rtw.getStartTime().toString());
		XMLUtilities.addChildContent(simElem, SIM_END_TIME_ELEM, rtw.getEndTime().toString());
		
		Date date = new Date(info.getLastComputedDate());
		HecTime computedDate = new HecTime(date, 0);
		XMLUtilities.addChildContent(simElem, SIM_LAST_COMPUTED_ELEM, computedDate.toString());
		
		List<ModelAlternative> modelAlts = info.getSimulation().getAllModelAlternativeList();
		Element modelAltsElem = new Element(MODEL_ALTS_ELEM);
		simElem.addContent(modelAltsElem);
		for(int i = 0;i < modelAlts.size();i++ )
		{
			addModelAltInfo(modelAltsElem, modelAlts.get(i), info.getSimulation());
		}
		addAdditionalInfoForSim(simElem, info);
	}

	protected void addAdditionalInfoForSim(Element simElem, SimulationReportInfo info)
	{
		// method for other reports to add additional info
	}

	/**
	 * @param parent
	 * @param modelAlt
	 */
	private static void addModelAltInfo(Element parent,
			ModelAlternative modelAlt, WatSimulation sim)
	{
		if ( modelAlt == null ) 
		{
			return;
		}
		Element modelAltElem = new Element(MODEL_ALT_ELEM);
		parent.addContent(modelAltElem);
		XMLUtilities.addChildContent(modelAltElem, MODEL_ALT_NAME_ELEM, modelAlt.getName());
		XMLUtilities.addChildContent(modelAltElem, MODEL_ALT_PROGRAM_ELEM, modelAlt.getProgram());
		XMLUtilities.addChildContent(modelAltElem, MODEL_ALT_FPART_ELEM, sim.getFPart(modelAlt));
		XMLUtilities.addChildContent(modelAltElem, MODEL_ALT_FOLDER_ELEM, getModelFolder(modelAlt, sim));
	}

	/**
	 * @param modelAlt
	 * @param sim
	 * @return
	 */
	private static String getModelFolder(ModelAlternative modelAlt, WatSimulation sim)
	{
		String program = modelAlt.getProgram();
		String runDir = sim.getRunDirectory();
		
		SimpleWatPlugin plugin = WatPluginManager.getPlugin(program);
		if ( plugin instanceof WatPlugin )
		{
			WatPlugin wPlugin = (WatPlugin) plugin;
			String dir = wPlugin.getDirectory();
			if ( RMAIO.isFullPath(dir))
			{
				dir = RMAIO.getFileFromPath(dir);
			}
			runDir = RMAIO.concatPath(runDir, dir);
		}
		try
		{
			if (plugin instanceof CeQualW2Plugin)
			{
				CeQualW2Plugin w2Plugin = (CeQualW2Plugin) plugin;
				CeQualW2Alt w2Alt = w2Plugin.getAlt(modelAlt);
				if (w2Alt != null)
				{
					ComputeOptions co = new ComputeOptions();
					co.setRunDirectory(runDir);
					return w2Alt.getCeQualW2RunPath(co);
				}
			}
		}
		catch (NoClassDefFoundError err)
		{
			FluentLogger.forEnclosingClass().atWarning().log("Failed to find CeQualW2 plugin");
		}
		return runDir;
	}

	/**
	 * @param simName
	 * @return
	 */
	private String getBaseSimulationName(String simName)
	{
		return RMAIO.replace(simName, "-"+_simGroupName, "");
	}

	/**
	 * @param parent
	 */
	private void addProjectInfo(Element parent, String baseSimDir)
	{
		Element prjInfoElem = new Element(STUDY_ELEM);
		parent.addContent(prjInfoElem);
		if ( _prjDir != null )
		{
			XMLUtilities.addChildContent(prjInfoElem, PRJ_DIR_ELEM, _prjDir);
		}
		if ( _obsDir != null )
		{
			XMLUtilities.addChildContent(prjInfoElem, OBS_DIR_ELEM, _obsDir);
		}
		String installDir = System.getProperty("user.dir");
		installDir= RMAIO.getDirectoryFromPath(installDir);
		
		XMLUtilities.addChildContent(prjInfoElem, INSTALL_DIR_ELEM, installDir);
		String simReportsDir = RMAIO.concatPath(baseSimDir, AbstractReportAction.REPORT_DIR);
		XMLUtilities.addChildContent(prjInfoElem, WRITE_DIR_ELEM, simReportsDir);
	}
	
}
