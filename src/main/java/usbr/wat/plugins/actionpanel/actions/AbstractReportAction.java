/*
 * Copyright 2022 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jdom.Document;
import org.jdom.Element;
import org.python.google.common.io.Files;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;
import com.rma.util.XMLUtilities;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.event.MessageListener;
import hec2.wat.io.ProcessOutputReader;
import hec2.wat.model.WatSimulation;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.PersistenceServiceFactory;
import net.sf.jasperreports.repo.RepositoryService;
import rma.swing.RmaJDialog;
import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.io.OutputType;
import usbr.wat.plugins.actionpanel.io.ReportOptions;
import usbr.wat.plugins.actionpanel.model.ReportPlugin;
import usbr.wat.plugins.actionpanel.model.SimulationReportInfo;

/**
 * @author mark
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractReportAction extends AbstractAction
	implements ReportPlugin, MessageListener 
{
	
	private static Logger _logger = Logger.getLogger(AbstractReportAction.class.getName());
	
	public static final String REPORT_DIR = "reports";
	public static final String JASPER_DIR = "jasper";
	public static final String JASPER_COMPILED_FILE_EXT = ".jasper";
	private static final String JASPER_SOURCE_FILE_EXT = "jrxml";
	
	public static final String DATA_SOURCES_DIR = "Datasources";
	public static final String REPORT_INSTALL_FOLDER = "AutomatedReport";
	public static final String PYTHON_REPORT_BAT = "WAT_Report_Generator.exe";
	public static final String JASPER_FILE = "USBR_Draft_Validation.jrxml";
	
	public static final String OBS_DATA_FOLDER   = "shared";
	
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
	
	private List<String> _errMsgs= new ArrayList<>();
	
	public AbstractReportAction(String name)
	{
		super(name);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		ReportOptions options = new ReportOptions();
		options.setOutputType(OutputType.PDF);
		createReport(ActionPanelPlugin.getInstance().getActionsWindow().getSimulationReportInfos(), options );
	}
	/**
	 * @param reportFile
	 * @param pythonReportBat
	 * @return
	 */
	protected boolean runPythonScript(String reportXmlFile)
	{
		_errMsgs.clear();
		long t1 = System.currentTimeMillis();
		try
		{
			if ( Boolean.getBoolean("SkipPythonReport"))
			{
				return true;
			}
			List<String>cmdList = new ArrayList<>();
			String dir = getDirectoryToUse();
			
			String exeFile = RMAIO.concatPath(dir, PYTHON_REPORT_BAT);
			//cmdList.add("cmd.exe");
			//cmdList.add("/c");
			cmdList.add(exeFile);
			cmdList.add(reportXmlFile);

			return runProcess(cmdList, dir);
		}
		finally
		{
			long t2 = System.currentTimeMillis();
			_logger.info("runPythonScript:time to run python for "+reportXmlFile+" is "+(t2-t1)+"ms");
		}
	}
	/**
	 * @return
	 */
	public static String getDirectoryToUse()
	{
		String dir = System.getProperty("WAT.InstallDir", null);
		if ( dir == null || dir.isEmpty())
		{
			dir = System.getProperty("user.dir");
			_logger.info("getDirectoryToUse:WAT.InstallDir not set using "+dir);
		}
		else
		{
			_logger.info("getDirectoryToUse:WAT.InstallDir set to "+dir);
		}
					
		dir = RMAIO.concatPath(dir, REPORT_INSTALL_FOLDER);
		return dir;
	}
	
	protected boolean runProcess(List<String> cmdList, String runInFolder)
	{
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);
		ProcessBuilder procBuilder = new ProcessBuilder(cmdArray);
		
		File f = new File(runInFolder);
		if (!f.exists())
		{
			f.mkdirs();
		}
		procBuilder.directory(f);
		try
		{
			_logger.info("runProcess:launching in folder:"+runInFolder);
			_logger.info("runProcess:launching: "+cmdList);
			Process proc = procBuilder.start();
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			ProcessOutputReader preader1 = new ProcessOutputReader(reader1, true, proc);
			preader1.addListener(this);
			preader1.setEchoOutput(true);
			preader1.start();
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			ProcessOutputReader preader2 = new ProcessOutputReader(reader2, false, proc);
			preader2.setEchoOutput(true);
			preader2.start();
			int rv = proc.waitFor();
			_logger.info("runProcess:rv="+rv);
			if ( rv != 0 && !_errMsgs.isEmpty())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("<html>");
				for(int i = 0; i < _errMsgs.size();i ++ )
				{
					builder.append(_errMsgs.get(i));
					builder.append("<br>");
				}
				RmaJDialog parent = RmaJDialog.getActiveDialog();
				
				EventQueue.invokeLater(()->JOptionPane.showMessageDialog(parent, builder.toString(), "Error Running Report", JOptionPane.ERROR_MESSAGE));
			}
			return rv == 0;

		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			return false;
		}
		
			
	}
	
	/**
	 * @param params
	 * @param jasperRepoDir
	 * @param sim
	 */
	protected void setParameters(Map<String, Object> params, String jasperRepoDir,
			SimulationReportInfo sim, ReportOptions options)
	{
		params.put("p_ReportFolder", jasperRepoDir);
		params.put(WATERSHED_NAME_PARAM, Project.getCurrentProject().getName());
		params.put(SIMULATION_NAME_PARAM, sim.getName());
		params.put(ANALYSIS_START_TIME_PARAM, sim.getSimulation().getRunTimeWindow().getStartTime().toString());
		params.put(ANALYSIS_END_TIME_PARAM, sim.getSimulation().getRunTimeWindow().getEndTime().toString());
		params.put(PRINT_HEADER_FOOTER_PARAM, options.shouldPrintHeadersFooters());
		String reportDir = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(),REPORT_DIR);
		params.put(REPORT_DIR_PARAM, reportDir);
		
		String simReportDir = RMAIO.concatPath(sim.getSimFolder(), REPORT_DIR);
		params.put(SIM_REPORT_DIR_PARAM, simReportDir);
		
		String installDir= System.getProperty("user.dir");
		installDir = RMAIO.getDirectoryFromPath(installDir);
		params.put(WAT_INSTALL_DIR_PARAM, installDir);
		Date date = new Date(sim.getLastComputedDate());
		SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");

		params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));
		
		String dataAdapterFile = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(), REPORT_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, DATA_SOURCES_DIR);
		dataAdapterFile = RMAIO.concatPath(dataAdapterFile, XML_DATA_DOCUMENT);
		params.put(DATA_ADAPTER_FILE_PARAM, dataAdapterFile);
		
		_logger.fine("Report Parameters are:"+params);
	}
	/**
	 * @param studyDir
	 * @return
	 */
	protected String getObsDataPath(String studyDir)
	{
		return RMAIO.concatPath(studyDir, OBS_DATA_FOLDER);
	}
	
	protected String getJasperRelativeFolder()
	{
		String jasperReportFolder = RMAIO.concatPath(REPORT_DIR, JASPER_DIR);
		return jasperReportFolder;
	}
	/**
	 * edit the Data Adapter file in <study>/reports/DataSources folder and write it to <sim run>/reports/DataSources
	 * @param simRunFolder
	 * @return
	 */
	protected boolean editDataAdapterFile(String simRunFolder)
	{
		String studyDir = Project.getCurrentProject().getProjectDirectory();
		String dataSourceFolder = RMAIO.concatPath(studyDir, REPORT_DIR);
		dataSourceFolder = RMAIO.concatPath(dataSourceFolder, DATA_SOURCES_DIR);
		String dataAdapterPath = RMAIO.concatPath(dataSourceFolder, XML_DATA_DOCUMENT);
		
		RmaFile dataAdapterFile = FileManagerImpl.getFileManager().getFile(dataAdapterPath);
		
		Document dataAdapterDoc = XMLUtilities.loadDocument(dataAdapterFile);
		if ( dataAdapterDoc == null )
		{
			_logger.info("failed to read " + dataAdapterFile.getAbsolutePath());
			return false;
		}
		Element root = dataAdapterDoc.getRootElement();
		Element dataFileElem = root.getChild("dataFile");
		if ( dataFileElem == null )
		{
			_logger.info("failed to find dataFile element in " + dataAdapterFile.getAbsolutePath());
			return false;
		}
		Element locationElem = dataFileElem.getChild("location");
		if (locationElem == null )
		{
			_logger.info("failed to find Location element in " + dataAdapterFile.getAbsolutePath());
			return false;
		}
		String simFolder = RMAIO.concatPath(simRunFolder, REPORT_DIR);
		simFolder = RMAIO.concatPath(simFolder, DATA_SOURCES_DIR);
		String newOutputLocation = RMAIO.concatPath(simFolder, XML_DATA_OUTPUT);
		locationElem.setText(newOutputLocation);
		
		String newAdapterLocation = RMAIO.concatPath(simFolder, XML_DATA_DOCUMENT);
		RmaFile newDataAdapterFile = FileManagerImpl.getFileManager().getFile(newAdapterLocation);
		return XMLUtilities.saveDocument(dataAdapterDoc, newDataAdapterFile);
	
	}
	/**
	 * @param info 
	 * @param options 
	 * 
	 */
	protected JasperPrint fillReport(SimpleJasperReportsContext context, String studyDir, String installDir, 
			String jasperReportFolder,  SimulationReportInfo info, ReportOptions options )
	{
		long t1 = System.currentTimeMillis();
		try
		{
			String studyJasperDir = RMAIO.concatPath(studyDir, getJasperRelativeFolder());
			studyJasperDir = studyJasperDir+"C";
			FileRepositoryService jasperFileRepository = new FileRepositoryService(context, 
					studyJasperDir, true);

			String simReportsDir = RMAIO.concatPath(info.getSimFolder(), REPORT_DIR);
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
			setParameters(params, jasperReportFolder, info, options);


			String xmlDataDoc = RMAIO.concatPath(info.getSimFolder(), REPORT_DIR);
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
			_logger.info("fillReport:time to fill jasper report for "+info.getName()+ "is "+(t2-t1)+"ms");
		}
	}	
	/**
	 * @param directoryFromPath
	 */
	protected boolean compileJasperFiles(String studyDir, String installDir, String jasperRelDir)
	{
		long t1 = System.currentTimeMillis();
		RMAFilenameFilter filter= new RMAFilenameFilter(JASPER_SOURCE_FILE_EXT);
		filter.setAcceptDirectories(false);
		String jasperInstallFolder = RMAIO.concatPath(installDir, jasperRelDir);
		String jasperStudyFolder = RMAIO.concatPath(studyDir, jasperRelDir); // reports/JasperC
		List<String>jasperFiles;
		String srcFile, destFile;
		boolean alwaysCompile = Boolean.getBoolean("CompileJasperFiles");
		_logger.info("compileJasperFiles:report repositories: install:"+jasperInstallFolder+" study:"+jasperStudyFolder);
		boolean success = true;
		if ( FileManagerImpl.getFileManager().fileExists(jasperInstallFolder))
		{
			jasperFiles = FileManagerImpl.getFileManager().list(jasperInstallFolder, filter);
			for(int i = 0; i < jasperFiles.size();i++ )
			{
				srcFile = jasperFiles.get(i);
				String srcFileName = RMAIO.getFileFromPath(srcFile);
				String fileToCompile = findSourceFile(srcFileName, jasperStudyFolder, jasperInstallFolder);
				String studyFile = RMAIO.concatPath(jasperStudyFolder, srcFileName);
				if ( compileJasperFile(fileToCompile, alwaysCompile)== null )
				{
					success = false;
				}
				
			}
		}
		else 
		{ // no install jasper files so just go with whats in the study
			jasperFiles = FileManagerImpl.getFileManager().list(jasperStudyFolder, filter);
			for (int i = 0;i < jasperFiles.size(); i++ )
			{
				srcFile = jasperFiles.get(i);
				if ( compileJasperFile(srcFile, alwaysCompile) == null )
				{
					success = false;
				}
			}
		}
		long t2 = System.currentTimeMillis();
		_logger.info("time to compile jasper files is " + (t2-t1)+" ms.");
		return success;
		
	}
	/**
	 * @param srcFileName
	 * @param jasperInstallFolder 
	 * @param jasperStudyFolder 
	 * @return
	 */
	protected String findSourceFile(String jasperFileName, String jasperStudyFolder, String jasperInstallFolder)
	{
		String studyFile = RMAIO.concatPath(jasperStudyFolder, jasperFileName);
		if ( FileManagerImpl.getFileManager().fileExists(studyFile))
		{
			return studyFile;
		}
		else
		{
			return RMAIO.concatPath(jasperInstallFolder, jasperFileName);
		}
	}

	/** 
	 * compile the jasper file
	 * @param studyFile
	 * @param alwaysCompile
	 */
	private String compileJasperFile(String jasperFile, boolean alwaysCompile)
	{
		String destFile = getJasperDestFile(jasperFile);
		if ( needsToCompile(jasperFile, destFile) || alwaysCompile )
		{
			_logger.info("compileJasperFile:compiling "+jasperFile +" to "+destFile);
			try
			{
				JasperDesign design = JRXmlLoader.load(jasperFile);
				//String dataAdapterFile = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(), REPORT_DIR);
				//dataAdapterFile = RMAIO.concatPath(dataAdapterFile, DATA_SOURCES_DIR);
				//dataAdapterFile = RMAIO.concatPath(dataAdapterFile, XML_DATA_DOCUMENT);
				String rv = JasperCompileManager.compileReportToFile(jasperFile);
				if ( rv != null )
				{
					try
					{
						Files.move(new File(rv), new File(destFile));
						_logger.info("compileJasperFiles: compiled to "+destFile);
					}
					catch (IOException e)
					{
						_logger.info("compileJasperFile:failed to move file "+e);
						return null;
					}
					return destFile;
				}
				return null;
			}
			catch (JRException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return destFile;
	}

	/**
	 * 
	 * check to see if the file needs to be compile
	 * @param srcFile
	 * @param destFile
	 * @return
	 */
	private static boolean needsToCompile(String src, String dest)
	{
		RmaFile srcFile = FileManagerImpl.getFileManager().getFile(src);
		RmaFile destFile = FileManagerImpl.getFileManager().getFile(dest);
		if ( destFile.exists() )
		{
			if ( srcFile.lastModified() > destFile.lastModified() )
			{
				return true;
			}
			return false;
		}
		return true;
	}
	/**
	 * @param srcFile
	 * @return
	 */
	private String getJasperDestFile(String srcFile)
	{
		if ( Boolean.getBoolean("JasperCompilesToSameFolder"))
		{
			int idx = srcFile.lastIndexOf('.');
			if ( idx > -1 )
			{
				String destFile = srcFile.substring(0,idx);
				destFile = destFile.concat(JASPER_COMPILED_FILE_EXT);
				return destFile;
			}
		}
		else
		{
			String srcFileName = RMAIO.getFileNameNoExtension(srcFile);
			String	destFile = RMAIO.concatPath(Project.getCurrentProject().getProjectDirectory(), getJasperRelativeFolder()+"C");
			destFile = RMAIO.concatPath(destFile, srcFileName);
			destFile = destFile.concat(JASPER_COMPILED_FILE_EXT);
			return destFile;
		}
		return null;
	}
	/**
	 * @param sim
	 * @return
	 */
	protected String findFpartForPython(WatSimulation sim, ModelAlternative modelAlt)
	{
		String fpart = sim.getFPart(modelAlt);
		return RMAIO.userNameToFileName(fpart);
	}
	
	public void messageRecieved(String msg)
	{
		_errMsgs.add(msg);
	}
}
