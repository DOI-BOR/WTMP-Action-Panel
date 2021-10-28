
package usbr.wat.plugins.actionpanel.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.rma.io.DssFileManagerImpl;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Computable;
import com.rma.model.ComputeProgressListener;
import com.rma.model.ComputeProgressListener2;
import com.rma.model.Project;
import com.rma.ui.ComputeProgressPanel;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileDataManager;
import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.dss.HecDataManager;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import hec.model.RunTimeWindow;

import hec2.model.DataLocation;
import hec2.model.DssDataLocation;
import hec2.plugin.model.ComputeOptions;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.model.WatSimulation;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPlugin;
import hec2.wat.plugin.WatPluginManager;

import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;

/**
 * @author Mark Ackerman
 *
 */
public class ActionComputable
		implements Computable
{

	private static final String SAVE_SUFFEX = "-save";
	private static final String ITERATION_DSS_FILE = "iterationResults.dss";
	private static final String DSSFILE = "DSS File";
	public static final String METHOD_SIGNATURE = "runIteration(modelAlternative, currentIteration, maxIteration)";
	
	private WatSimulation _sim;
	private IterationSettings _iterSettings;
	private String _iterDssFile;
	private PythonInterpreter _interp;
	private boolean _debug;
	private transient Map<ModelAlternative, PyCode>_preCodeMap = new HashMap<>();
	private transient Map<ModelAlternative, PyCode>_postCodeMap = new HashMap<>();
	private String _currentScriptText;
	private UsgsComputeSelectorDialog _computeDialog;
	private boolean _canceled;
	/**
	 * @param sim
	 * @param iterSettings
	 */
	public ActionComputable(WatSimulation sim, IterationSettings iterSettings)
	{
		super();
		_sim = sim;
		_iterSettings = iterSettings;
	}

	@Override
	public void run()
	{
		compute();
	}

	@Override
	public boolean isComputable()
	{
		return _sim.isComputable();
	}

	@Override
	public void addComputeListener(ComputeProgressListener listener)
	{
		_sim.addComputeListener(listener);
	}

	@Override
	public void removeComputeProgressListener(ComputeProgressListener listener)
	{
		_sim.removeComputeProgressListener(listener);

	}

	@Override
	public boolean compute()
	{
		if ( _iterSettings == null || !_iterSettings.isIterative())
		{
			_sim.setRecomputeAll(_computeDialog.shouldRecomputeAll());
			return _sim.compute();
		}
		return iterativeCompute();
	}

	/**
	 * @return
	 */
	private boolean iterativeCompute()
	{
		_debug = Boolean.getBoolean("ActionComputable.debugCompute");
		int[] members = _iterSettings.getMembersToCompute();
		if ( members == null || members.length == 0 )
		{
			_sim.addErrorMessage("No Iteration Members selected to compute");
			_sim.computeComplete(false);
			return false;
		}
		// save off the original DSS data
		List<DSSIdentifier>savedDssPaths = saveDssPaths();
		if ( savedDssPaths == null )
		{
			return false;
		}
		_preCodeMap.clear();
		_postCodeMap.clear();
		ComputeProgressListener progressListener = _sim.getComputeProgressListener();
		List<ComputeProgressListener> listeners = null;
		if ( progressListener instanceof ComputeProgressListener2 )
		{
			ComputeProgressListener2 pl2 = (ComputeProgressListener2) progressListener;
			listeners = pl2.getListeners();
		}
		try
		{
			int currentMember;
			for(int m = 0; m < members.length;m ++ )
			{
				currentMember = members[m];
				_sim.addComputeMessage("Computing Iteration Member "+currentMember);
				if ( !runPreScripts(currentMember))
				{
					return false;
				}
				if ( _canceled )
				{
					return false;
				}
				// copy in the new iteration data
				if ( !copyDssMembers(currentMember))
				{
					return false;
				}
				if ( _canceled )
				{
					return false;
				}
				// close any DSS files we might have had open
				HecDSSFileDataManager dm = new HecDSSFileDataManager();
				dm.closeAllFiles();
				
				_sim.setRecomputeAll(_computeDialog.shouldRecomputeAll());
				// compute
				if ( !_sim.compute())
				{
					if (Boolean.getBoolean("ActionComputable.ContinueOnError"))
					{
						continue;
					}
					return false;
				}
				//copy the output from the simulation dss file to the iteration dss file
				if ( _canceled )
				{
					return false;
				}
				if ( listeners != null )
				{ //listeners got removed by the sim at the end of its compute, so put them back
					for (int l = 0; l < listeners.size(); l++ )
					{
						_sim.addComputeListener(listeners.get(l));
						if ( listeners.get(l) instanceof ComputeProgressPanel )
						{
							((ComputeProgressPanel)listeners.get(l)).setModelPosition(0);
							//((ComputeProgressPanel)listeners.get(l)).clearMessageText();
							
						}
					}
				}
				copyDssResultsToCollectionsDss(currentMember);
				if ( _canceled )
				{
					return false;
				}
				if ( !runPostScripts(currentMember))
				{
					return false;
				}
				
			}
		}
		catch(Exception e )
		{
			_sim.addErrorMessage("Exception during iterative compute " + e);
			Logger.getLogger(ActionComputable.class.getName()).warning("Exception during iterative compute "+e );
			e.printStackTrace();
			return false;
		}
		finally
		{
			// restore the saved off DSS paths
			restoreDssPaths(savedDssPaths);
			_preCodeMap.clear();
			_postCodeMap.clear();
			for (int l = 0; l < listeners.size(); l++ )
			{
				_sim.removeComputeProgressListener(progressListener);
			}
			
		}
		
		
		return true;
	}
	

	/**
	 * @return
	 */
	private boolean runPostScripts(int iterNum)
	{
		SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();
		ComputeSettings computeSettings = sSettings.getPostComputeSettings();
		return runScripts(computeSettings, iterNum, false);
	}

	/**
	 * @return
	 */
	private boolean runPreScripts(int iterNum)
	{
		SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();
		ComputeSettings computeSettings = sSettings.getPreComputeSettings();
		return runScripts(computeSettings, iterNum, true);
	}

	/**
	 * @param computeSettings
	 * @return
	 */
	private boolean runScripts(ComputeSettings computeSettings, int iterNum, boolean isPreCompute)
	{

		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
		ModelAlternative modelAlt;
		for (int i = 0;i < modelAlts.size() && !_canceled; i ++ )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			String scriptFile = computeSettings.getScriptFor(modelAlt);
			if ( scriptFile == null || scriptFile.isEmpty() )
			{
				continue;
			}
			String scriptText = readScriptFile(scriptFile);
			if ( scriptText == null )
			{
				return false;
			}
			if ( _debug )
			{
				Logger.getLogger(ActionComputable.class.getName()).info("Found Script for " + modelAlt);
			}
			if (!runScript(modelAlt, scriptText, iterNum, isPreCompute))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @param scriptFile
	 * @return
	 */
	private String readScriptFile(String scriptFile)
	{
		String absScriptFile = Project.getCurrentProject().getAbsolutePath(scriptFile);
		RmaFile file = FileManagerImpl.getFileManager().getFile(absScriptFile);
		if ( file == null )
		{
			return null;
		}
		BufferedReader reader = file.getBufferedReader();
		String line;
		StringBuilder buffer = new StringBuilder();
		try
		{
			while ((line = reader.readLine())!=null )
			{
				buffer.append(line);
				buffer.append("\n");
			}
			return buffer.toString();
		}
		catch (IOException ioe)
		{
			_sim.addErrorMessage("Error reading script file "+absScriptFile+" Error:"+ioe);
			Logger.getLogger(ActionComputable.class.getName()).warning("Error reading script file "+absScriptFile+" Error:"+ioe);
		}
		finally
		{
			if ( reader != null )
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					// empty ok
				}
			}
		}
		return null;
	}

	/**
	 * @param modelAlt
	 * @param script
	 */
	private boolean runScript(ModelAlternative modelAlt, String script, int iterNum, boolean isPreCompute)
	{
		if ( _interp == null )
		{
			if ( !initInterp())
			{
				return false;
			}
		}
		PyCode code = (isPreCompute?_preCodeMap.get(modelAlt):_postCodeMap.get(modelAlt));
		if ( code == null )
		{
			_currentScriptText = script;
			code = compileCode(script);
			if ( code == null )
			{
				Logger.getLogger(ActionComputable.class.getName()).info("Failed to compile "+(isPreCompute?"precompute":"postcompute")+"script for "+modelAlt);
				_sim.addErrorMessage("Failed to compile "+(isPreCompute?"precompute":"postcompute")+" script for "+modelAlt);
				return false;
			}
			if ( isPreCompute )
			{
				_preCodeMap.put(modelAlt, code);
			}
			else
			{
				_postCodeMap.put(modelAlt, code);
			}
		}
		_sim.addComputeMessage("Running "+(isPreCompute?"pre-compute":"post-compute")+" script for " + modelAlt);
		boolean rv = runScript(code, modelAlt, iterNum);
		_currentScriptText = null;
		return rv;
		
		
	}

	/**
	 * @param code
	 * @return
	 */
	private boolean runScript(PyCode code, ModelAlternative modelAlt, int iterNum)
	{
		long t1 = System.currentTimeMillis();
		
		
		{
			if ( _debug )
			{
				Logger.getLogger(ActionComputable.class.getName()).info("running Jython Code for "+modelAlt+" iter="+iterNum);
			}
			hec2.wat.model.ComputeOptions options = _sim.getOptionsForNextCompute(modelAlt, _sim.getRunTimeWindow(), getWatPlugin(modelAlt));
			modelAlt.setComputeOptions(options);
			
			PyStringMap locals = new PyStringMap();
			locals.__setitem__("currentIteration", Py.java2py(iterNum));
			locals.__setitem__("maxIteration", Py.java2py(_iterSettings.getMaximumMember()));
			locals.__setitem__("modelAlternative", Py.java2py(modelAlt));
			// set additional variables....
			_interp.setLocals(locals);
			/*
			String script = getInitializeScript();
			if ( script == null || script.length() == 0 )
			{ // no initialization function
				return true;
			}
			*/
			try
			{
				_interp.exec(code);
				PyObject outlocals = _interp.getLocals();
				// get return value from interpreter
				PyObject pyobj = ((PyStringMap) outlocals)
						.__getitem__(new PyString("ret"));
				Object obj = Py.tojava(pyobj, Boolean.class.getName());
				if (obj instanceof Boolean)
				{
					if (_debug)
					{
						Logger.getLogger(ActionComputable.class.getName()).info("returning "+obj+" from script");
						_sim.addLogMessage( "runScript() returning " + obj);
					}
					Boolean b = (Boolean) obj;
					return b;
				}
				// assume since they didn't return us a boolean value, everything is ok.
				return true;
			}
			catch (Exception e)
			{
				if ( e instanceof PyException )
				{
					PyException pye = (PyException) e;
					pye.normalize();
				}
				Logger.getLogger(ActionComputable.class.getName()).info("runScript:Error running initialization script "+e);
				Logger.getLogger(ActionComputable.class.getName()).info("runScript:initialization script is:");
				Logger.getLogger(ActionComputable.class.getName()).info(_currentScriptText);
				_sim.addErrorMessage("Error running script "+getName()
						+"'s. Error"+e);
				_sim.addErrorMessage("Check ComputeLog for details");
				_sim.addLogMessage(e.toString());
				_sim.addLogMessage("current script is:");
				_sim.addLogMessage("-------------------------------");
				_sim.addLogMessage(_currentScriptText);
				_sim.addLogMessage("-------------------------------");
				_currentScriptText = null;
				return false;
			}
			/*
			 * catch ( Exception e) { System.out.println("evaluteRule: Exception
			 * running script "+e); e.printStackTrace(System.out); throw e; }
			 */
			finally
			{
				if (_debug)
				{
					_sim.addLogMessage("initializeScript " + getName() + " took:"
									+ (System.currentTimeMillis() - t1) + " ms.");
				}
			}
		}
		
	}

	/**
	 * @param modelAlt
	 * @return
	 */
	private static WatPlugin getWatPlugin(ModelAlternative modelAlt)
	{
		String program = modelAlt.getProgram();
		
		SimpleWatPlugin splugin = WatPluginManager.getPlugin(program);
		if ( splugin instanceof WatPlugin )
		{
			WatPlugin plugin = (WatPlugin) splugin;
			return plugin;
		}
		return null;
	}

	/**
	 * @param script
	 * @return
	 */
	private PyCode compileCode(String script)
	{
		StringBuilder buffer = new StringBuilder(script);
		buffer.append("ret="+METHOD_SIGNATURE+"\n");
		String updatedScript = buffer.toString();
		try 
		{
			
			if (_debug )
			{
				Logger.getLogger(ActionComputable.class.getName()).info("Compiling script:"+updatedScript);
			}
			PyCode pyCode = (new org.python.util.PythonInterpreter()).compile(updatedScript);
			
			return pyCode;
		}
		catch ( Exception e)
		{
			Logger.getLogger(ActionComputable.class.getName()).warning( "Python Compilation Error of Script " + updatedScript+" failed " + e);
			_sim.addErrorMessage( "Python Compilation Error of Script failed " + e);
			_sim.addErrorMessage(" Script is:\n"+updatedScript);
			return null;
		}
	}

	/**
	 * @return
	 */
	private boolean initInterp()
	{
		if ( _debug )
		{
			Logger.getLogger(ActionComputable.class.getName()).info("initializing Jython Interpreter");
		}
		//------------------------------------------------------//
		// make sure we have a valid application home directory //
		//------------------------------------------------------//
		String appHome = hec.lang.ApplicationProperties.getAppHome();
		if (appHome == null) appHome = ".";
		try {
			appHome = (new File(appHome)).getAbsolutePath();
			if (appHome.endsWith(File.separator+".")) {
				appHome = appHome.substring(0, appHome.length() - 2);
			}
		}
		catch (Exception e) 
		{
		}
		
		long t1 = System.currentTimeMillis();
		String pythonPath = System.getProperty("python.path");
		if (pythonPath == null)
		{
			pythonPath = appHome;
			String classpath = System.getProperty("java.class.path");
			StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
			String token = null;
			boolean found = false;
			while ( tokenizer.hasMoreTokens())
			{
				token = tokenizer.nextToken();
				if ( token.indexOf("jythonlib.jar") > -1 )
				{
					found = true;
					Logger.getLogger(ActionComputable.class.getName()).info("found jythonlib.jar in classpath"+token);
					break;
				}
			}
			if ( found )
			{
				token = token+"/lib";
			}
			else
			{
				token = appHome+File.separator+"jar"+File.separator+"jythonlib.jar/lib";
			}
			if (!pythonPath.endsWith(File.separator)) pythonPath += File.separator;
			pythonPath += "scripts"+File.pathSeparator+token;

			System.setProperty("python.path", pythonPath);
		}
		java.util.Properties props = new java.util.Properties();
		props.setProperty("python.path", pythonPath);

		PythonInterpreter.initialize(System.getProperties(), props,
			new String[] {""});
		PySystemState sys = Py.getSystemState();
		sys.add_package("hec.rss.model");
		//sys.add_classdir("/dev/code");

		_interp = new PythonInterpreter();
		if ( _debug )
		{
			Logger.getLogger(ActionComputable.class.getName()).info("initInterp(): creating interpreter took "
				+(System.currentTimeMillis()-t1)+" ms");
		}
		return true;
	}

	/**
	 * @return
	 */
	private List<DSSIdentifier> saveDssPaths()
	{
		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
		ModelAlternative modelAlt;
		ModelAltIterationSettings maSettings;
		List<DataLocation> dataLocs;
		DataLocation dataLoc;
		DSSIdentifier dssId;
		List<DSSIdentifier>pathsRenamed = new ArrayList<>();
		String variantName = _sim.getVariantName();
		_sim.addComputeMessage("Saving DSS records ...");
		for (int i = 0;i < modelAlts.size();i ++ )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			modelAlt.setVariantName(variantName);
			maSettings = _iterSettings.getModelAltSettings(modelAlt);
			if ( maSettings == null )
			{
				continue;
			}
			dataLocs = maSettings.getDataLocations();
			if ( dataLocs == null )
			{
				continue;
			}
			for(int d = 0;d < dataLocs.size(); d++ )
			{
				dataLoc = dataLocs.get(d);
				dssId = maSettings.getDSSIdentifierFor(dataLoc);
				if ( dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty() )
				{
					continue;
				}
				dssId = saveDssPath(dataLoc);
				if ( dssId != null )
				{
					pathsRenamed.add(dssId);
				}
			}
		}
		_sim.addComputeMessage("Saved "+pathsRenamed.size()+" DSS records ...");
		return pathsRenamed;
	}
	/**
	 * @param dataLoc
	 */
	private static DSSIdentifier saveDssPath(DataLocation dataLoc)
	{
		Vector<String> srcList= new Vector<>();
		Vector<String> destList= new Vector<>();
		
		DataLocation linkedToDl = dataLoc.getLinkedToLocation();
		if ( linkedToDl instanceof DssDataLocation && DSSFILE.equals(dataLoc.getModelToLinkTo()) )
		{
			DssDataLocation dssDl = (DssDataLocation) linkedToDl;
			String dssPath = dssDl.getDssPath();
			String dssFile = dssDl.get_dssFile();
			fillInSrcAndDestList(dssFile, dssPath, srcList, destList);

			int rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFile, srcList, destList);
			
			if  (rv == srcList.size())  // renamed all the records
			{
				DSSIdentifier dssId = new DSSIdentifier(dssFile, dssPath);
				return dssId;
			}
		}
		return null;
	}

	/**
	 * @param dataLoc
	 * @param srcList
	 * @param destList
	 */
	private static void fillInSrcAndDestList(String dssFile, String dssPath,
			List<String> srcList, List<String> destList)
	{
		List<String> paths = findPathnamesFor(dssFile, dssPath);
		srcList.addAll(paths);
		
		
		DSSPathname pathname = new DSSPathname();
		for (int i = 0;i < srcList.size(); i++ )
		{
			pathname.setPathname(srcList.get(i));
			String fpart = pathname.getFPart();
			fpart = fpart.concat(SAVE_SUFFEX);
			pathname.setFPart(fpart);
		
		
			destList.add(pathname.getPathname());
		}
	}

	/**
	 * @param pathname
	 * @return
	 */
	private static List<String> findPathnamesFor(String dssFile, String dssPath)
	{
		DSSPathname pathname = new DSSPathname();
		pathname.setPathname(dssPath);
		pathname.setDPart("*");
		DSSIdentifier dssId = new DSSIdentifier(dssFile, pathname.getPathname());
		Vector pathnames = DssFileManagerImpl.getDssFileManager().searchDSSPaths(dssId);
		return pathnames;
	}

	/**
	 * copy in the new iteration data
	 * @param m 
	 * @return
	 */
	private boolean copyDssMembers(int member)
	{
		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
		ModelAlternative modelAlt;
		ModelAltIterationSettings maSettings;
		List<DataLocation> dataLocs;
		DataLocation dataLoc, linkedDl;
		DSSIdentifier dssId, srcDssId = new DSSIdentifier();

		DSSPathname pathname = new DSSPathname();
		String dssPath, fileName;
		DssDataLocation dssDl;
		boolean copySuccessful = true;
		RunTimeWindow rtw = _sim.getRunTimeWindow();
		srcDssId.setStartTime(rtw.getStartTime());
		srcDssId.setEndTime(rtw.getEndTime());
		_sim.addComputeMessage("Copying over iterative DSS records...");
		for (int i = 0;i < modelAlts.size() && !_canceled;i ++ )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			maSettings = _iterSettings.getModelAltSettings(modelAlt);
			if ( maSettings == null )
			{
				continue;
			}
			dataLocs = maSettings.getDataLocations();
			if ( dataLocs == null )
			{
				continue;
			}
			for(int d = 0;d < dataLocs.size() && !_canceled; d++ )
			{
				dataLoc = dataLocs.get(d);
				dssId = maSettings.getDSSIdentifierFor(dataLoc);
				if ( dssId == null || dssId.getDSSPath() == null || dssId.getDSSPath().isEmpty())
				{
					continue;
				}
				linkedDl = dataLoc.getLinkedToLocation();
		
				if ( linkedDl instanceof DssDataLocation )
				{
					dssDl = (DssDataLocation) linkedDl;
					dssPath = dssId.getDSSPath();
					if ( DSSPathname.isaCollectionPath(dssPath))
					{
						pathname.setPathname(dssPath);
						pathname.setCollectionSequence(member);
						dssPath = pathname.getPathname();
					}
					fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName());
					srcDssId.setFileName(fileName);
					srcDssId.setDSSPath(dssPath);
					TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true);
					if ( srcTsc != null && srcTsc.numberValues > 0 )
					{
						srcTsc.fileName = dssDl.get_dssFile();
						srcTsc.fullName = dssDl.getDssPath();
						int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc);
						if ( rv != 0 )
						{
							copySuccessful= false;
							Logger.getLogger(ActionComputable.class.getName()).warning("Falied to write DSS record for "+dataLoc+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
							_sim.addErrorMessage("Falied to write DSS record for "+dataLoc+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
						}
						else
						{
							_sim.addComputeMessage("   Copied " + dssPath+ " to "+dssDl.get_dssFile()+":"+dssDl.getDssPath());
						}
					}
					else
					{
						_sim.addErrorMessage("No Data Found for "+srcDssId+" for Time Window "+srcDssId.getStartTime()+" to "+srcDssId.getEndTime());
					}
				}
				
			}
		}
		return copySuccessful;
	}

	/**
	 * @param savedDssPaths
	 */
	private void restoreDssPaths(List<DSSIdentifier> savedDssPaths)
	{
		_sim.addComputeMessage("Restoring original "+savedDssPaths.size()+" DSS records ...");
		Vector<String> srcList= new Vector<>();
		Vector<String> destList= new Vector<>();
		DSSIdentifier dssId;
		String path, dssFile;
		for (int i = 0;i < savedDssPaths.size();i ++ )
		{
			dssId = savedDssPaths.get(i);
			_sim.addComputeMessage(" model  Restoring DSS path for "+dssId);
			path = dssId.getDSSPath();
			dssFile = dssId.getFileName();
			fillInSrcAndDestList(dssFile, path, destList, srcList);

			int rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFile, srcList, destList);
			if ( rv != srcList.size() )
			{
				_sim.addWarningMessage("Failed to restore DSS records for "+dssId);
			}
		}
	}

	/**
	 * 
	 */
	private void copyDssResultsToCollectionsDss(int interationId)
	{
		_sim.addComputeMessage("Saving Computed DSS records to collections");
		
		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();
		ModelAlternative modelAlt;	
		for (int m = 0; m < modelAlts.size() && !_canceled;m++ )
		{
			modelAlt = modelAlts.get(m);
			if ( modelAlt == null )
			{
				continue;
			}
			updateIterationDssWithDssData(modelAlt, interationId);
		}
	}	
	/**
	 * copy the computed dss records back from the ensemble dss records to the real 
	 * forecast DSS file
	 * @param modelAlt
	 */
	private boolean updateIterationDssWithDssData(ModelAlternative modelAlt, int interationId)
	{
			
		_sim.addComputeMessage("Saving Computed DSS records to collections for "+modelAlt);
		
		String fPart = _sim.getFPart(modelAlt);
		ComputeOptions co = modelAlt.getComputeOptions();
		String dssFile = co.getDssFilename();
		DSSIdentifier dssId = new DSSIdentifier(dssFile);
		Vector<String> srcPaths = DssFileManagerImpl.getDssFileManager().searchDSSPaths(dssId, 
				"F="+fPart);
		if ( srcPaths == null )
		{ // nothing to copy
			_sim.addComputeMessage("No Output DSS records found for "
					+modelAlt.getProgram()+" model "+modelAlt +" FPart="+fPart);
			return true;
		}
		_sim.addComputeMessage("Copying output DSS for "+modelAlt.getProgram()
			+" model "+modelAlt+" to Iteration.dss");
		DSSPathname pathname = new DSSPathname();
		Vector<String>destPaths = new Vector<>(srcPaths.size());
		String path;
		for(int i = 0; i < srcPaths.size() && !_canceled;i++ )
		{
			path = srcPaths.get(i);
			pathname.setPathname(path);
			pathname.setCollectionSequence(interationId);
			destPaths.add(pathname.getPathname());
		}
		String iterDssFile = getIterationDssFile();
		int rv = copyRecords(dssFile, iterDssFile, srcPaths, destPaths);
		boolean success = rv == srcPaths.size();
		if ( !success )
		{
			_sim.addErrorMessage("Failed to update iteration DSS file with "
					+modelAlt.getProgram()+" model "+modelAlt+"'s results");
		}
		return success;
		
	}
	/**
	 * @return
	 */
	private String getIterationDssFile()
	{
		if ( _iterDssFile == null )
		{
			String dssFile = _sim.getSimulationDssFile();
			String computeFolder = RMAIO.getDirectoryFromPath(dssFile);
			_iterDssFile = RMAIO.concatPath(computeFolder, ITERATION_DSS_FILE);
		}
		return _iterDssFile;
	}

	/**
	 * copy the records from one dss file to another.
	 * @param dssFile
	 * @param forecastDSSFilename
	 * @param srcPaths
	 * @param destPaths
	 * @return
	 */
	private int copyRecords(String fromDssFile, String toDssFile,
			Vector<String> srcPaths, Vector<String> destPaths)
	{
		HecDSSUtilities fromDataManager = new HecDSSUtilities();
		fromDataManager.setDSSFileName(fromDssFile);
		HecDataManager toDataManager = new HecDataManager(toDssFile);
		_sim.addComputeMessage("Copying records from "+fromDssFile);
		int rv =  fromDataManager.copyRecordsFrom (toDataManager, srcPaths, destPaths);
		return rv;
	}

	@Override
	public int getModelCount()
	{
		return _sim.getModelCount();
	}

	@Override
	public boolean cancelCompute()
	{
		_canceled = true;
		return _sim.cancelCompute();
	}

	@Override
	public String getLogFile()
	{
		return _sim.getLogFile();
	}

	@Override
	public String getName()
	{
		return _sim.getName();
	}

	@Override
	public boolean needToCompute()
	{
		return _sim.needToCompute();
	}
	@Override
	public String toString()
	{
		return _sim.getName();
	}

	/**
	 * @param computeDlg
	 */
	public void setProgressDialog(UsgsComputeSelectorDialog computeDlg)
	{
		_computeDialog = computeDlg;
	}
}
