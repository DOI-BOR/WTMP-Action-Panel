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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Project;

import hec2.plugin.model.ModelAlternative;
import hec2.wat.event.MessageListener;
import hec2.wat.io.ProcessOutputReader;
import hec2.wat.model.WatSimulation;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
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
	public static final String JASPER_COMPILED_FILE_EXT = ".jasper";
	private static final String JASPER_SOURCE_FILE_EXT = "jrxml";
	
	public static final String DATA_SOURCES_DIR = "DataSources";
	public static final String REPORT_INSTALL_FOLDER = "AutomatedReport";
	public static final String PYTHON_REPORT_BAT = "WAT_Report_Generator.exe";
	
	public static final String OBS_DATA_FOLDER   = "shared";
	
	public static final String WATERSHED_NAME_PARAM = "watershedName";
	public static final String SIMULATION_NAME_PARAM = "simulationName";
	public static final String ANALYSIS_START_TIME_PARAM = "analysisStartTime";
	public static final String ANALYSIS_END_TIME_PARAM = "analysisEndTime";
	public static final String SIMULATION_LAST_COMPUTED_DATE_PARAM = "simulationDate";
	public static final String PRINT_HEADER_FOOTER_PARAM = "printHeaderAndFooter";
	private static final String REPORT_DIR_PARAM = "REPORT_DIR";
	
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
			System.out.println("runPythonScript:time to run python for "+reportXmlFile+" is "+(t2-t1)+"ms");
		}
	}
	/**
	 * @return
	 */
	protected String getDirectoryToUse()
	{
		String dir = System.getProperty("WAT.InstallDir", null);
		if ( dir == null || dir.isEmpty())
		{
			dir = System.getProperty("user.dir");
			System.out.println("getDirectoryToUse:WAT.InstallDir not set using "+dir);
		}
		else
		{
			System.out.println("getDirectoryToUse:WAT.InstallDir set to "+dir);
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
			System.out.println("runProcess:launching in folder:"+runInFolder);
			System.out.println("runProcess:launching: "+cmdList);
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
			System.out.println("runProcess:rv="+rv);
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
		params.put(REPORT_DIR_PARAM, sim.getSimulation().getRunDirectory());
		Date date = new Date(sim.getLastComputedDate());
		SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy HH:mm");

		params.put(SIMULATION_LAST_COMPUTED_DATE_PARAM, fmt.format(date));
	}
	/**
	 * @param studyDir
	 * @return
	 */
	protected String getObsDataPath(String studyDir)
	{
		return RMAIO.concatPath(studyDir, OBS_DATA_FOLDER);
	}
	/**
	 * @param directoryFromPath
	 */
	protected void compileJasperFiles(String jasperDir)
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
	private static String getJasperDestFile(String srcFile)
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
