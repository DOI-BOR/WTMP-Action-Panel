/*
 * Copyright 2021  Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from HEC
 */
package usbr.wat.plugins.actionpanel.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.io.ProcessOutputReader;
import hec2.wat.model.WatSimulation;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.repo.FileRepositoryPersistenceServiceFactory;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.PersistenceServiceFactory;
import net.sf.jasperreports.repo.RepositoryService;
import rma.util.RMAFilenameFilter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionsWindow;

/**
 * @author Mark Ackerman
 *
 */
public abstract class BaseReportAction extends AbstractAction
{
	public static final String REPORT_INSTALL_FOLDER = "AutomatedReport";
	public static final String JASPER_COMPILED_FILE_EXT = ".jasper";
	public static final String JASPER_SOURCE_FILE_EXT = "jrxml";
	public static final String REPORT_DIR = "reports";
	public static final String PDF_REPORT_FILE_EXT = ".pdf";
	
	private ActionsWindow _parent;

	public BaseReportAction(ActionsWindow parent, String reportName)
	{
		super(reportName);
		_parent = parent;
	}

	public ActionsWindow getActionsWindow()
	{
		return _parent;
	}
	
	public void createReportAction()
	{
		if ( _parent.getSimulationGroup() == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return ;
			
		}
		
		List<WatSimulation>sims = _parent.getSelectedSimulations();
		if ( sims.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to create reports for",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);
			return ;
		}	
		createReport(sims);
	}

	/**
	 * @param sims
	 */
	protected abstract void createReport(List<WatSimulation> sims);
	
	/**
	 * @param reportFile
	 * @param pythonReportBat
	 * @return
	 */
	protected boolean runPythonScript(String exe, String reportFile, String pythonReportBat)
	{
		long t1 = System.currentTimeMillis();
		try
		{
			if ( Boolean.getBoolean("SkipPythonReport"))
			{
				return true;
			}
			List<String>cmdList = new ArrayList<>();
			String dir = System.getProperty("WAT.InstallDir", null);
			if ( dir == null )
			{
				dir = System.getProperty("user.dir");
			}
			dir = RMAIO.concatPath(dir, REPORT_INSTALL_FOLDER);
			
			String batFile = RMAIO.concatPath(dir, exe);
			//cmdList.add("cmd.exe");
			//cmdList.add("/c");
			cmdList.add(batFile);
			cmdList.add(reportFile);

			return runProcess(cmdList, dir);
		}
		finally
		{
			long t2 = System.currentTimeMillis();
			System.out.println("runPythonScript:time to run python for "+reportFile+" is "+(t2-t1)+"ms");
		}
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
			System.out.println("runProcess:launching in folder:"+runInFolder);
			System.out.println("runProcess:launching: "+cmdList);
			Process proc = procBuilder.start();
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			ProcessOutputReader preader1 = new ProcessOutputReader(reader1, true, proc);
			preader1.setEchoOutput(true);
			preader1.start();
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			ProcessOutputReader preader2 = new ProcessOutputReader(reader2, false, proc);
			preader2.setEchoOutput(true);
			preader2.start();
			int rv = proc.waitFor();
			System.out.println("runProcess:rv="+rv);
			return rv == 0;

		}
		catch (IOException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
			
	}
	
	/**
	 * 
	 * @param sim
	 * @param jasperFile
	 * @param jasperOutFile
	 * @param xmlFileName
	 * @param params
	 * @return
	 */
	public boolean runJasperReport(WatSimulation sim, String jasperFile, String jasperOutFile, String xmlDataDoc, Map<String,Object>params)
	{
		long t1 = System.currentTimeMillis();
		try
		{
			//Log log = LogFactory.getLog(JasperFillManager.class);
			String studyDir = Project.getCurrentProject().getProjectDirectory();
			String simDir = sim.getSimulationDirectory();
			String jasperRepoDir = RMAIO.concatPath(studyDir, REPORT_DIR);
			String rptFile = RMAIO.concatPath(jasperRepoDir, jasperFile);
			//rptFile = RMAIO.concatPath(rptFile, JASPER_FILE);


			System.out.println("runReportWithOutputFile:report repository:"+jasperRepoDir);

			SimpleJasperReportsContext context = new SimpleJasperReportsContext();
			FileRepositoryService fileRepository = new FileRepositoryService(context, 
					jasperRepoDir, true);
			context.setExtensions(RepositoryService.class, Collections.singletonList(fileRepository));
			context.setExtensions(PersistenceServiceFactory.class, 
					Collections.singletonList(FileRepositoryPersistenceServiceFactory.getInstance()));
			String inJasperFile = rptFile;

			JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.xpath.executer.factory",
					"net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");



			long t2 = System.currentTimeMillis();
			JasperReport jasperReport;
			try
			{
				compileJasperFiles(RMAIO.getDirectoryFromPath(inJasperFile));
				jasperReport = JasperCompileManager.compileReport(inJasperFile);
			}
			catch (JRException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			long t3 = System.currentTimeMillis();
			System.out.println("runJasperReport:time to compile jasper files for "+sim+ "is "+(t3-t2)+"ms");

			String outputFile = RMAIO.concatPath(simDir, REPORT_DIR);
			RmaFile simDirFile = FileManagerImpl.getFileManager().getFile(outputFile);
			if ( !simDirFile.exists() )
			{
				if ( !simDirFile.mkdirs())
				{
					System.out.println("runJasperReport:failed to create folder "+simDirFile.getAbsolutePath());
				}
			}
			outputFile = RMAIO.concatPath(outputFile, jasperOutFile);
			/*
			Map<String, Object>params = new HashMap<>();
			// define the parameters for the report
			params.put("p_ReportFolder", jasperRepoDir);
			params.put(WATERSHED_NAME_PARAM, Project.getCurrentProject().getName());
			params.put(SIMULATION_NAME_PARAM, sim.getName());
			params.put(ANALYSIS_START_TIME_PARAM, sim.getRunTimeWindow().getStartTime().toString());
			params.put(ANALYSIS_END_TIME_PARAM, sim.getRunTimeWindow().getEndTime().toString());
			Date date = new Date(sim.getLastComputedDate());
			SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");

			params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));
			
			
			
			fmt= new SimpleDateFormat("yyyy.MM.dd-HHmm");
			
			date = new Date();
			outputFile = outputFile.concat(fmt.format(date));
			*/
			outputFile = outputFile.concat(PDF_REPORT_FILE_EXT);
			
			/*
			String xmlDataDoc = RMAIO.concatPath(studyDir, REPORT_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, DATA_SOURCES_DIR);
			xmlDataDoc = RMAIO.concatPath(xmlDataDoc, xmlFileName);
			*/

			JasperPrint jasperPrint;
			System.out.println("runJasperReport:filling report "+inJasperFile);
			JRXmlDataSource dataSource;
			try
			{
				dataSource = new JRXmlDataSource(context, JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataDoc)));
			}
			catch (JRException e1)
			{
				e1.printStackTrace();
				return false;
			}
			try
			{
				jasperPrint = JasperFillManager.getInstance(context).fill(jasperReport, params, dataSource);
			}
			catch (JRException e)
			{
				e.printStackTrace();
				return false;
			}
			long t4 = System.currentTimeMillis();
			System.out.println("runJasperReport:time to fill jasper report for "+sim+ "is "+(t4-t3)+"ms");

			// fills compiled report with parameters and a connection
			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputFile));

			try
			{
				exporter.exportReport();
				System.out.println("runJasperReport:report written to "+outputFile);
			}
			catch (JRException e)
			{
				e.printStackTrace();
				return false;
			}

			long t5 = System.currentTimeMillis();
			System.out.println("runJasperReport:time to write jasper report for "+sim+ "is "+(t5-t4)+"ms");
			return true;
		}
		finally
		{
			long end = System.currentTimeMillis();
			System.out.println("runJasperReport:total time to create jasper report for "+sim+" is "+(end-t1)+"ms");
		}
	}
	/**
	 * @param directoryFromPath
	 */
	private void compileJasperFiles(String jasperDir)
	{
		RMAFilenameFilter filter= new RMAFilenameFilter(JASPER_SOURCE_FILE_EXT);
		filter.setAcceptDirectories(false);
		List<String> jasperFiles = FileManagerImpl.getFileManager().list(jasperDir, filter);
		String srcFile, destFile;
		boolean alwaysCompile = Boolean.getBoolean("CompileJasperFiles");
		for (int i = 0;i < jasperFiles.size(); i++ )
		{
			srcFile = jasperFiles.get(i);
			destFile = getJasperDestFile(srcFile);
			if ( needsToCompile(srcFile, destFile) || alwaysCompile )
			{
				System.out.println("compileJasperFiles:compiling to disk "+srcFile);
				try
				{
					String rv = JasperCompileManager.compileReportToFile(jasperFiles.get(i));
					System.out.println("compileJasperFiles: compiled to "+rv);
				}
				catch (JRException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
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
		int idx = srcFile.lastIndexOf('.');
		if ( idx > -1 )
		{
			String destFile = srcFile.substring(0,idx);
			destFile = destFile.concat(JASPER_COMPILED_FILE_EXT);
			return destFile;
		}
		return null;
	}
	/**
	 * @param sim
	 * @return
	 */
	private String findFpartForPython(WatSimulation sim, ModelAlternative modelAlt)
	{
		String fpart = sim.getFPart(modelAlt);
		return RMAIO.userNameToFileName(fpart);
	}
}
