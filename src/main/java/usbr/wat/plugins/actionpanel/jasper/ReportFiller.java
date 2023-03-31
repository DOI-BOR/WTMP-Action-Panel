/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.jasper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.util.XMLUtilities;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.PersistenceServiceFactory;
import net.sf.jasperreports.repo.RepositoryService;
import rma.util.GetOpt;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.io.OutputType;

/**
 * class to fill the Jasper report as a separate process
 * 
 * @author mark
 *
 */
public class ReportFiller
{
	private static Logger _logger = Logger.getLogger(ReportFiller.class.getName());
	public static final String REPORT_DIR = "reports";
	public static final String JASPER_DIR = "jasper";
	public static final String DATA_SOURCES_DIR = "Datasources";
	public static final String JASPER_FILE = "USBR_Draft_Validation.jrxml";
	
	public static final String WATERSHED_NAME_PARAM = "watershedName";
	public static final String SIMULATION_NAME_PARAM = "simulationName";
	public static final String ANALYSIS_START_TIME_PARAM = "analysisStartTime";
	public static final String ANALYSIS_END_TIME_PARAM = "analysisEndTime";
	public static final String SIMULATION_LAST_COMPUTED_DATE_PARAM = "simulationDate";
	public static final String PRINT_HEADER_FOOTER_PARAM = "printHeaderAndFooter";
	private static final String REPORT_DIR_PARAM = "REPORT_DIR";
	public static final String XML_DATA_DOCUMENT = "USBRAutomatedReportDataAdapter.xml";
	public static final String XML_DATA_OUTPUT = "USBRAutomatedReportOutput.xml";
	private static final String WAT_INSTALL_DIR_PARAM = "Install_Dir";
	private static final String DATA_ADAPTER_FILE_PARAM = "DataAdapterLocation";
	private static final String SIM_REPORT_DIR_PARAM = "RUN_DIR";
	
	
	
	public static final String OUTPUT_FILE_ELEM = "OutputFile";
	public static final String JASPER_REPORT_FOLDER_ELEMENT = "JasperReportFolder";
	public static final String SIMULATION_FOLDER_ELEMENT = "SimulationFolder";
	public static final String INSTALL_FOLDER_ELEMENT = "InstallFolder";
	public static final String STUDY_FOLDER_ELEMENT = "StudyFolder";
	public static final String SIM_NAME_ELEMENT = "SimulationName";
	public static final String START_TIME_ELEMENT = "StartTime";
	public static final String END_TIME_ELEMENT = "EndTime";
	public static final String PRINT_HEADER_FOOTER_ELEMENT = "PrintHeadersFooters";
	public static final String LAST_COMPUTE_DATE_ELEMENT = "SimulationLastComputeDate";
	public static final String STUDY_NAME_ELEMENT = "StudyName";
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String argPath = null;
		GetOpt go = new GetOpt(args, "f:");
		int opt;
		char ch;
		boolean usagePrint = false;
		while ((opt = go.getopt()) != GetOpt.optEOF)
		{
		     ch = (char)opt;
		     if ( ch == 'U' || ch == '?')
		     {
		       usagePrint = true;
		     }
		     else if ( ch == 'f')
		     {
		       argPath = go.optArgGet();
		     }
		     else
		     {
		    	 _logger.info("Unknown option " +ch);
		    	 System.exit(1);
		     }
		     // undefined option
		   }
		   // getopt() returns '?'
		   if (usagePrint || argPath == null )
		   {
		     System.out.println("Usage: -f argsFile");
		     System.exit(0);
		   }
		   RmaFile argsFile = FileManagerImpl.getFileManager().getFile(argPath);
		   Document doc = XMLUtilities.loadDocument(argsFile);
		   if (doc == null )
		   {
			   System.out.println("ReportFiller.main: no XML document found at "+argPath);
			   System.exit(3);
		   }
		   boolean rv = new ReportFiller().runReport(doc.getRootElement());
		   if ( rv )
		   {
			   System.exit(0);
		   }
	}
	
	public ReportFiller()
	{
		super();
	}
	
	protected boolean runReport(Element root)
	{
		JasperPrint jasperPrint = fillReport(root);
		if ( jasperPrint == null )
		{
			System.out.println("runReport:failed to fill report");
			System.exit(2);
		}
		boolean rv = writeReport(jasperPrint, root);
		return rv;

	}
	/**
	 * @param outputFile, outputType 
	 * @param jasperPrint 
	 * 
	 */
	private boolean writeReport(JasperPrint jasperPrint, Element root)
	{
		long t1 = System.currentTimeMillis();
		try
		{
			String outputFile = XMLUtilities.getChildElementAsString(root, OUTPUT_FILE_ELEM, null);
			String outputExt = RMAIO.getFileExtension(outputFile);
			OutputType ot = OutputType.getOutputType(outputExt);

			JRExporter exporter = ot.buildExporter( jasperPrint, outputFile);
			long t4 = System.currentTimeMillis();
			try
			{
				exporter.exportReport();
				_logger.info("writeReport:simulation report written to "+outputFile);
				return true;
			}
			catch (JRException e)
			{
				e.printStackTrace();
			}
			return false;
		}
		finally
		{
			long t2 = System.currentTimeMillis();
			_logger.info("writeReport: time to write report " + (t2-21)+" ms");
		}

	}
	

	protected JasperPrint fillReport(Element root)
	{
		long t1 = System.currentTimeMillis();
		try
		{
			String jasperReportFolder = XMLUtilities.getChildElementAsString(root, JASPER_REPORT_FOLDER_ELEMENT, null);
			String simFolder = XMLUtilities.getChildElementAsString(root, SIMULATION_FOLDER_ELEMENT, null);
			String installDir = XMLUtilities.getChildElementAsString(root, INSTALL_FOLDER_ELEMENT, null); 
			String studyDir = XMLUtilities.getChildElementAsString(root, STUDY_FOLDER_ELEMENT, null); 
			
			// SimulationReportInfo info, ReportOptions options, 
			SimpleJasperReportsContext context = new SimpleJasperReportsContext();

			JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.xpath.executer.factory",
					"net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");
			
			String studyJasperDir = RMAIO.concatPath(studyDir, getJasperRelativeFolder());
			studyJasperDir = studyJasperDir+"C";
			FileRepositoryService jasperFileRepository = new FileRepositoryService(context, 
					studyJasperDir, true);

			String simReportsDir = RMAIO.concatPath(simFolder, REPORT_DIR);
			simReportsDir = RMAIO.concatPath(simReportsDir, DATA_SOURCES_DIR);
			FileRepositoryService reportsFileRepository = new FileRepositoryService(context, 
					simReportsDir, true);

			context.setExtensions(RepositoryService.class, Arrays.asList(jasperFileRepository, reportsFileRepository));

			context.setExtensions(PersistenceServiceFactory.class, 
					Collections.singletonList(FileRepositoryPersistenceServiceFactory.getInstance()));

			JasperReport jasperReport;
			String inJasperFile = null;
			try
			{

				// compiled the files now fill the report
				int idx = JASPER_FILE.lastIndexOf('.');

				
				String jasperCompiledFile = JASPER_FILE.substring(0,idx);
				jasperCompiledFile = jasperCompiledFile.concat(".jasper");

				inJasperFile = RMAIO.concatPath(studyJasperDir, jasperCompiledFile);
				jasperReport = (JasperReport)JRLoader.loadObject(new File(inJasperFile));

			}
			catch (JRException e)
			{
				e.printStackTrace();
				return null;
			}



			Map<String, Object>params = new HashMap<>();
			setParameters(params, jasperReportFolder, root);


			String xmlDataDoc = RMAIO.concatPath(simFolder, REPORT_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, DATA_SOURCES_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, XML_DATA_DOCUMENT);

			JasperPrint jasperPrint;
			_logger.info("fillReport:filling report "+inJasperFile+ " DataSource="+xmlDataDoc);
			JRXmlDataSource dataSource;
			try
			{
				dataSource = new JRXmlDataSource(context, JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataDoc)));
			}
			catch (JRException e1)
			{
				e1.printStackTrace();
				return null;
			}
			if ( dataSource == null )
			{
				_logger.info("fillReport:failed to load DataAdapter file "+xmlDataDoc);
				return null;
			}
			try
			{
				jasperPrint = JasperFillManager.getInstance(context).fill(jasperReport, params, dataSource);
				return jasperPrint;
			}
			catch (JRException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		finally
		{
			long t2 = System.currentTimeMillis();
			_logger.info("fillReport:time to fill jasper report is "+(t2-t1)+"ms");
		}
	}	
	
	protected void setParameters(Map<String, Object> params, String jasperRepoDir, Element root)
	{
		params.put("p_ReportFolder", jasperRepoDir);
		String studyName = XMLUtilities.getChildElementAsString(root, STUDY_NAME_ELEMENT, "");
		params.put(WATERSHED_NAME_PARAM, studyName);
		String simName = XMLUtilities.getChildElementAsString(root, SIM_NAME_ELEMENT, "");
		params.put(SIMULATION_NAME_PARAM, simName);
		String startTime = XMLUtilities.getChildElementAsString(root, START_TIME_ELEMENT, "");
		params.put(ANALYSIS_START_TIME_PARAM, startTime);
		String endTime = XMLUtilities.getChildElementAsString(root, END_TIME_ELEMENT, "");
		params.put(ANALYSIS_END_TIME_PARAM, endTime);
		boolean printHeadersFooters = XMLUtilities.getChildElementAsBoolean(root, PRINT_HEADER_FOOTER_ELEMENT, true);
		params.put(PRINT_HEADER_FOOTER_PARAM, printHeadersFooters);
		
		String studyDir = XMLUtilities.getChildElementAsString(root, STUDY_FOLDER_ELEMENT, null);
		String reportDir = RMAIO.concatPath(studyDir,REPORT_DIR);
		params.put(REPORT_DIR_PARAM, reportDir);
		
		String simFolder = XMLUtilities.getChildElementAsString(root, SIMULATION_FOLDER_ELEMENT, null);
		String simReportDir = RMAIO.concatPath(simFolder, REPORT_DIR);
		params.put(SIM_REPORT_DIR_PARAM, simReportDir);
		
		String installDir= XMLUtilities.getChildElementAsString(root, INSTALL_FOLDER_ELEMENT, System.getProperty("user.dir"));
		//installDir = RMAIO.getDirectoryFromPath(installDir);
		params.put(WAT_INSTALL_DIR_PARAM, installDir);
		long lastComputeDate = XMLUtilities.getChildElementAsLong(root, LAST_COMPUTE_DATE_ELEMENT, 0);
		Date date = new Date(lastComputeDate);
		SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");

		params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));
		
		String dataAdapterFile = RMAIO.concatPath(studyDir, REPORT_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, DATA_SOURCES_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, XML_DATA_DOCUMENT);
		params.put(DATA_ADAPTER_FILE_PARAM, dataAdapterFile);
		
		_logger.fine("Report Parameters are:"+params);
	}
	
	protected String getJasperRelativeFolder()
	{
		String jasperReportFolder = RMAIO.concatPath(REPORT_DIR, JASPER_DIR);
		return jasperReportFolder;
	}

}
