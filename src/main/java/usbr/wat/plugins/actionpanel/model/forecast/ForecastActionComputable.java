/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */
package usbr.wat.plugins.actionpanel.model.forecast;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import com.google.common.flogger.FluentLogger;
import com.rma.client.Browser;
import com.rma.editors.ComputeProgressDialog;
import com.rma.io.DssFileManager;
import com.rma.io.DssFileManagerImpl;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.ComputeProgressListener;
import com.rma.model.ComputeProgressListener2;
import com.rma.model.Project;
import com.rma.model.RealizationComputable;
import com.rma.ui.ComputeProgressPanel;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileDataManager;
import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.dss.HecDataManager;
import hec.heclib.util.HecTime;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.DSSIdentifier;
import hec.io.PairedDataContainer;
import hec.io.TextContainer;
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
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import rma.util.RMAIO;
import usbr.wat.plugins.actionpanel.ActionPanelPlugin;
import usbr.wat.plugins.actionpanel.actions.forecast.RunForecastSimulationAction;
import usbr.wat.plugins.actionpanel.model.BaseComputeSettings;
import usbr.wat.plugins.actionpanel.model.ComputeSettings;
import usbr.wat.plugins.actionpanel.model.ComputeType;
import usbr.wat.plugins.actionpanel.model.IcPathMap;
import usbr.wat.plugins.actionpanel.model.ModelAltIterationSettings;
import usbr.wat.plugins.actionpanel.model.UsbrComputable;
import usbr.wat.plugins.actionpanel.ui.forecast.CsvReader;

/**
 * @author Mark Ackerman
 *
 */
public class ForecastActionComputable
		implements UsbrComputable, RealizationComputable
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	private static final String SAVE_DSS_RECORDS_PROP = "ForecastCompute.SaveDssRecords";
	private static final String BC_CONFIG_FILE = ForecastConfigFiles.getRelativeBCConfigFile();
	private static final String IC_CONFIG_FILE = ForecastConfigFiles.getRelativeICConfigFile();
	private static final String TEMP_TARGET_CONFIG_FILE = ForecastConfigFiles.getRelativeTempTargetConfigFile();
	private static final String TEMP_TARGET_CONTROL_LOCS_FILE = ForecastConfigFiles.getRelativeTempTargetControlLocsFile();
	private static final String SAVE_SUFFEX = "-save";
	public static final String ITERATION_DSS_FILE = "iterationResults.dss";
	private static final String DSSFILE = "DSS File";
	public static final String METHOD_SIGNATURE = "runIteration(modelAlternative, currentIteration, maxIteration)";
	public static final String TEMP_TARGET_CONTROL_LOC_REPLACE = "%location%";
	private static final String CURRENT_ENSEMBLE_FILE_NAME = "current_ensemble.txt";
	private final boolean _recomputeAll;
	private int[] _members;
	/** the starting collection number to copy the data to the collections output file */
	private final List<EnsembleSet> _selectedESets;

	private ForecastSimGroup _simGroup;
	private WatSimulation _sim;
	private String _iterDssFile;
	private PythonInterpreter _interp;
	private boolean _debug;
	private transient Map<ModelAlternative, PyCode>_preCodeMap = new HashMap<>();
	private transient Map<ModelAlternative, PyCode>_postCodeMap = new HashMap<>();
	private String _currentScriptText;
	private ComputeProgressDialog _computeDialog;
	private boolean _canceled;
	private ComputeType _computeType;

	private DssPathMap _bcDssPathMap;
	private IcPathMap _icDssPathMap;
	private TempTargetDssPathMap _tempTargetDssPathMap;
	private List<TempTargetControlLocsMapping> _tempTargetControlLocsDssPathMap;
	private ComputeProgressPanel _computeProgressPanel;
	private int _memberCnt;
	private int _memberIdx;
	private JProgressBar _lifecyclePbar;


	public ForecastActionComputable(ForecastSimGroup simGroup, WatSimulation sim, List<EnsembleSet> selectedESets, boolean recomputeAll)
	{
		super();
		_simGroup = simGroup;
		_sim = sim;
		_selectedESets = selectedESets;
		_recomputeAll = recomputeAll;
		calculateMemberCount();
	}

	private void calculateMemberCount()
	{
		EnsembleSet eset;
		String memberSet;
		int[] members;
		for(int i = 0;i < _selectedESets.size(); i++ )
		{
			eset = _selectedESets.get(i);
			memberSet = eset.getMemberSetToCompute();
			members = RunForecastSimulationAction.getIntegerSet(memberSet);
			if ( members != null && members.length > 0 )
			{
				_memberCnt += members.length;
			}
		}

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
		return ensembleCompute();
	}
	



	/**
	 * @return
	 */
	private boolean ensembleCompute()
	{
		_debug = Boolean.getBoolean("ActionComputable.debugCompute");
		_computeProgressPanel = (ComputeProgressPanel) _computeDialog.getContentPane().getComponent(0);
		Component[] comps = _computeProgressPanel.getComponents();
		for (int i = comps.length-1; i >=0; i-- )
		{  // hackish
			if ( comps[i] instanceof JProgressBar )
			{
				JProgressBar pbar = (JProgressBar) comps[i];

				if ( pbar.getString().equalsIgnoreCase("Lifecycle"))
				{
					_lifecyclePbar = pbar;
					_lifecyclePbar.setString("Ensemble Count");
					break;
				}

			}
		}
		Map<EnsembleSet, int[]> esetMap = new HashMap<>();
		EnsembleSet eset;
		int[] members;
		String memberSet;
		for(int i = 0;i < _selectedESets.size(); i++ )
		{
			eset = _selectedESets.get(i);
			memberSet = eset.getMemberSetToCompute();
			members = RunForecastSimulationAction.getIntegerSet(memberSet);
			if ( members == null || members.length == 0 )
			{
				return false;
			}
			esetMap.put(eset, members);
		}
		Map.Entry<EnsembleSet, int[]> esetEntry;
		Set<Map.Entry<EnsembleSet, int[]>> esetSet = esetMap.entrySet();
		Iterator<Map.Entry<EnsembleSet, int[]>> esetIter = esetSet.iterator();
		int esetIdx = 0;
		_memberIdx = 0;
		while (esetIter.hasNext())
		{
			esetEntry = esetIter.next();
			_members = esetEntry.getValue();
			eset = esetEntry.getKey();
			if( _lifecyclePbar != null )
			{
				final EnsembleSet fEset = eset;
				EventQueue.invokeLater(() ->_lifecyclePbar.setString(fEset.getName()));
			}
			int[] ensembleNums = _simGroup.getEnsembleSetCollectionIndexing(_sim, eset);

			if ( !ensembleCompute(eset, _members, ensembleNums[0]))
			{
				_computeProgressPanel.computeComplete(false);
				return false;
			}


			esetIdx++;
		}
		_sim.setRealizationPosition(_memberCnt);
		_computeProgressPanel.computeComplete(true);
		return true;
	}

	private boolean ensembleCompute(EnsembleSet eset, int[] members, int outputCollectionStart)
	{

		if ( members == null || members.length == 0 )
		{
			_sim.addErrorMessage("No Ensemble Members selected to compute");
			_sim.computeComplete(false);
			return false;
		}
		// check to see if any of the ensemble members need computed
		boolean doCompute = false;
		if ( !_recomputeAll )
		{
			for (int m = 0; m < members.length; m++)
			{
				if (!eset.getComputedMembers().contains(members[m]) )
				{
					doCompute = true;
					break;
				}
			}
		}
		else
		{
			doCompute = true;
		}
		if ( !doCompute )
		{
			_computeProgressPanel.addMessage("No Ensemble Members for "+eset+" need computed.");
			return true;
		}
		List<DSSIdentifier>savedDssPaths = new ArrayList<>();
		if ( !preCompute(eset, members, savedDssPaths))
		{
			return false;
		}
		ComputeProgressListener progressListener = _sim.getComputeProgressListener();
		List<ComputeProgressListener> listeners = null;
		if ( progressListener instanceof ComputeProgressListener2 )
		{
			ComputeProgressListener2 pl2 = (ComputeProgressListener2) progressListener;
			listeners = pl2.getListeners();
		}
		try
		{
			// copy in the new iteration data
			if ( _debug )
			{
				JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in boundary condition data ");
			}
			if ( !copyBcDssData())
			{
				return false;
			}
			if ( !copyICDssData())
			{
				return false;
			}
			int currentMember;
			for(int m = 0; m < _members.length;m ++ )
			{

				_computeProgressPanel.setRealizationPosition(_memberIdx);
				currentMember = _members[m];
				if ( eset.getComputedMembers().contains(currentMember) && !_recomputeAll)
				{
					_computeProgressPanel.addMessage("Ensemble Member "+currentMember+" already computed, skipping");
					continue;
				}
				_computeProgressPanel.addMessage("Computing Ensemble Member "+currentMember);
				_computeProgressPanel.addMessage("Output will be saved  to F-Part C:"+ String.format("%06d", outputCollectionStart+currentMember));
				System.out.println("Computing Ensemble Member "+currentMember+" for "+_sim );
				if ( _debug )
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Pre Scripts for ensemble member "+currentMember);
				}
				if ( !runPreScripts(currentMember))
				{
					return false;
				}
				if ( _canceled )
				{
					return false;
				}
				if ( _debug )
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying in temp target data for Ensemble member "+currentMember);
				}
				if ( !copyTempTargetMember(eset, currentMember))
				{
					return false;
				}
				if ( !copyTempTargetControlLocs())
				{
					return false;
				}
				if( !writeCurrentMember(currentMember))
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
				_sim.setRecomputeAll(true);
				// compute
				if ( _debug )
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Computing Ensemble member "+currentMember);
				}
				if ( !_sim.compute())
				{
					if (Boolean.getBoolean("ActionComputable.ContinueOnError"))
					{
						continue;
					}
					_computeProgressPanel.addErrorMessage("Simulation "+_sim.getName()+" failed to compute");
					return false;
				}
				else
				{
					eset.addComputedMember(currentMember);
					ActionPanelPlugin.getInstance().getActionsWindow().getForecastPanel().getSimulationPanel().addComputedMember(_sim, eset, currentMember);

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
				if ( _debug )
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Copying results for ensemble member "+currentMember);
				}
				copyDssResultsToCollectionsDss(currentMember, outputCollectionStart);
				if ( _canceled )
				{
					return false;
				}
				if ( _debug )
				{
					JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Running Post Scripts ensemble member "+currentMember);
				}
				if ( !runPostScripts(currentMember))
				{
					return false;
				}
				_memberIdx++;
			}
		}
		catch(Exception e )
		{
			_computeProgressPanel.addErrorMessage("Exception during Ensemble compute " + e);
			Logger.getLogger(ForecastActionComputable.class.getName()).warning("Exception during Ensemble compute "+e );
			e.printStackTrace();
			return false;
		}
		finally
		{
			// restore the saved off DSS paths
			if ( _debug )
			{
				JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Restoring original DSS data");
			}
			if ( Boolean.getBoolean(SAVE_DSS_RECORDS_PROP))
			{
				restoreDssPaths(savedDssPaths);
			}
			_preCodeMap.clear();
			_postCodeMap.clear();
			for (int l = 0; l < listeners.size(); l++ )
			{
				_sim.removeComputeProgressListener(progressListener);
			}
			
		}
		
		
		return true;
	}

	private boolean writeCurrentMember(int currentMember)
	{
		boolean success = true;
		String runDir = Project.getCurrentProject().getAbsolutePath(_sim.getRunDirectory());
		Path currentEnsembleFile = Paths.get(runDir).resolve(CURRENT_ENSEMBLE_FILE_NAME);
		try
		{
			// Create the file if it doesn't exist, truncate it if it does
			Files.write(currentEnsembleFile, String.valueOf(currentMember).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException e)
		{
			_sim.addErrorMessage("Failed to write current ensemble member " + currentMember + " to file " + currentEnsembleFile);
			LOGGER.atWarning().withCause(e).log("Failed to write current ensemble member " + currentMember + " to file " + currentEnsembleFile);
			success = false;
		}
		return success;
	}

	private boolean copyTempTargetControlLocs()
	{
		_computeProgressPanel.setStatusMessage("Copying Temperature Targets Control Location Data...");
		boolean copySuccessful = true;
		for(TempTargetControlLocsMapping _mapping : _tempTargetControlLocsDssPathMap)
		{
			String srcDssFile = _mapping.getSourceDssFile();
			String srcDssPath = _mapping.getSourceDssRecord();
			String destDssFile = _mapping.getDestinationDssFile();
			String destDssPath = _mapping.getDestinationDssRecord();
			DSSIdentifier srcDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(srcDssFile),srcDssPath);
			DSSIdentifier destDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(destDssFile),destDssPath);
			_sim.addComputeMessage("Copying Temperature Target Control Location pathname from "+srcDssId+" to " + destDssId);
			copySuccessful = copyTextDssRecord(srcDssId, destDssId);
		}
		return copySuccessful;
	}

	private boolean copyTextDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId)
	{
		DssFileManager fileManager = DssFileManagerImpl.getDssFileManager();
		TextContainer copyFromData = fileManager.readTextData(srcDssId);
		int success = -1;
		if(copyFromData != null && !copyFromData.getText().isEmpty())
		{
			success = fileManager.writeTextData(destDssId, copyFromData.getText());
			if(success < 0)
			{
				_sim.addWarningMessage("Failed to copy Text DSS Record from "+srcDssId+" to "+destDssId);
			}
		}
		else
		{
			_sim.addWarningMessage("No data found in DSS Text Record: "+srcDssId);
		}
		return success == 0;
	}

	private boolean preCompute(EnsembleSet eset, int[] members, List<DSSIdentifier>savedPaths)
	{
		// save off the original DSS data
		if ( _debug )
		{
			JOptionPane.showMessageDialog(Browser.getBrowserFrame(), "Saving Original Data ");
		}
		String prjDir = Project.getCurrentProject().getProjectDirectory();
		String bcConfigPath = RMAIO.concatPath(prjDir, BC_CONFIG_FILE);

		_computeProgressPanel.setStatusMessage("Reading Boundary Condition Data..");
		_bcDssPathMap = new DssPathMap(_sim, bcConfigPath);
		BcData bcData = eset.getBcData();
		_bcDssPathMap.setSourceFPart(bcData.getFPart());
		_bcDssPathMap.setSourceDssFile(Project.getCurrentProject().getAbsolutePath(bcData.getOutputDssFile().toString()));
		if ( !_bcDssPathMap.readDssPathsFile())
		{
			return false;
		}

		String icConfigPath = RMAIO.concatPath(prjDir, IC_CONFIG_FILE);
		_computeProgressPanel.setStatusMessage("Reading Initial Condition Data..");
		_icDssPathMap = new IcPathMap(_sim, icConfigPath, _simGroup.getInitialConditions());
		InitialConditions icData = _simGroup.getInitialConditions();
//_icDssPathMap.setSourceFPart(bcData.getFPart());

		if ( !_icDssPathMap.readDssPathsFile())
		{
			return false;
		}



		String ttConfigPath = RMAIO.concatPath(prjDir, TEMP_TARGET_CONFIG_FILE);
		_computeProgressPanel.setStatusMessage("Reading Temperature Target Data..");
		_tempTargetDssPathMap = new TempTargetDssPathMap(_sim, ttConfigPath);
		_tempTargetDssPathMap.setSourceDssFile(eset.getTemperatureTargetSet().getDssOutputPath().toString());
		_tempTargetDssPathMap.setSourceFPart(eset.getTemperatureTargetSet().getFPartWithoutCollection());

		if ( !_tempTargetDssPathMap.readDssPathsFile())
		{
			return false;
		}
		String ttControlLocConfigPath = RMAIO.concatPath(prjDir, TEMP_TARGET_CONTROL_LOCS_FILE);
		_computeProgressPanel.setStatusMessage("Reading Temperature Target Control Location Data..");
		try
		{
			_tempTargetControlLocsDssPathMap = readTempTargetControlLocsFile(ttControlLocConfigPath, eset.getTemperatureTargetSet());
		}
		catch (IOException e)
		{
			_sim.addWarningMessage("Failed to read Temperature Target Control Location file " + ttControlLocConfigPath);
			LOGGER.atWarning().withCause(e).log("Failed to read Temperature Target Control Location file " + ttControlLocConfigPath);
			return false;
		}
		if ( Boolean.getBoolean((SAVE_DSS_RECORDS_PROP))) //)))
		{
			List<DSSIdentifier> savedDssPaths = saveDssPaths(_bcDssPathMap, _tempTargetDssPathMap);
			if (savedDssPaths == null)
			{
				return false;
			}
			savedPaths.addAll(savedDssPaths);
		}
		_preCodeMap.clear();
		_postCodeMap.clear();

		return true;
	}

	private List<TempTargetControlLocsMapping> readTempTargetControlLocsFile(String ttControlLocConfigPath, TemperatureTargetSet ttSet) throws IOException
	{
		List<TempTargetControlLocsMapping> retVal = CsvReader.readCsv(Paths.get(ttControlLocConfigPath), TempTargetControlLocsMapping.class);
		for(TempTargetControlLocsMapping mapping : retVal)
		{
			String srcRecord = mapping.getSourceDssRecord();
			if(srcRecord == null)
			{
				_sim.addWarningMessage("Read Failed. No Source DSS Record specified in config " + ttControlLocConfigPath);
				return new ArrayList<>();
			}
			String realizedSrcRecord = srcRecord.replace(TEMP_TARGET_CONTROL_LOC_REPLACE, ttSet.getRiverLocation().getName());
			mapping.setSourceDssRecord(realizedSrcRecord);
		}
		return retVal;
	}


	/**
	 * @return
	 */
	private boolean runPostScripts(int iterNum)
	{
//SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();
//ComputeSettings computeSettings = sSettings.getPostComputeSettings();
//return runScripts(computeSettings, iterNum, false);
		return true; // for now
	}

	/**
	 * @return
	 */
	private boolean runPreScripts(int iterNum)
	{
//SensitivitySettings sSettings = _iterSettings.getSensitivitySettings();
//ComputeSettings computeSettings = sSettings.getPreComputeSettings();
//return runScripts(computeSettings, iterNum, true);
		return  true; // for now
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
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Found Script for " + modelAlt);
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
			Logger.getLogger(ForecastActionComputable.class.getName()).warning("Error reading script file "+absScriptFile+" Error:"+ioe);
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
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Failed to compile "+(isPreCompute?"precompute":"postcompute")+"script for "+modelAlt);
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
				Logger.getLogger(ForecastActionComputable.class.getName()).info("running Jython Code for "+modelAlt+" iter="+iterNum);
			}
			hec2.wat.model.ComputeOptions options = _sim.getOptionsForNextCompute(modelAlt, _sim.getRunTimeWindow(), getWatPlugin(modelAlt));
			modelAlt.setComputeOptions(options);
			
			PyStringMap locals = new PyStringMap();
			locals.__setitem__("currentIteration", Py.java2py(iterNum));
			//locals.__setitem__("maxIteration", Py.java2py(_iterSettings.getMaximumMember()));
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
						Logger.getLogger(ForecastActionComputable.class.getName()).info("returning "+obj+" from script");
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
				Logger.getLogger(ForecastActionComputable.class.getName()).info("runScript:Error running initialization script "+e);
				Logger.getLogger(ForecastActionComputable.class.getName()).info("runScript:initialization script is:");
				Logger.getLogger(ForecastActionComputable.class.getName()).info(_currentScriptText);
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
				Logger.getLogger(ForecastActionComputable.class.getName()).info("Compiling script:"+updatedScript);
			}
			PyCode pyCode = (new PythonInterpreter()).compile(updatedScript);
			
			return pyCode;
		}
		catch ( Exception e)
		{
			Logger.getLogger(ForecastActionComputable.class.getName()).warning( "Python Compilation Error of Script " + updatedScript+" failed " + e);
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
			Logger.getLogger(ForecastActionComputable.class.getName()).info("initializing Jython Interpreter");
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
					Logger.getLogger(ForecastActionComputable.class.getName()).info("found jythonlib.jar in classpath"+token);
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
			Logger.getLogger(ForecastActionComputable.class.getName()).info("initInterp(): creating interpreter took "
				+(System.currentTimeMillis()-t1)+" ms");
		}
		return true;
	}

	/**
	 * @return
	 */
	private List<DSSIdentifier> saveDssPaths(DssPathMap bcDssPathMap, DssPathMap tempTargetDssPathMap)
	{
		_sim.addComputeMessage("Saving BC DSS records ...");
		List<DSSIdentifier>pathsRenamed = new ArrayList<>();
		saveDssPaths(pathsRenamed, bcDssPathMap);
		_sim.addComputeMessage("Saving Temp Target DSS records ...");
		saveDssPaths(pathsRenamed, tempTargetDssPathMap);



		_sim.addComputeMessage("Saved "+pathsRenamed.size()+" DSS records ...");
		return pathsRenamed;
	}

	private void saveDssPaths(List<DSSIdentifier> pathsRenamed, DssPathMap dssPathMap)
	{
		// dest to source
		Map<DSSIdentifier, DSSIdentifier> copyMap = dssPathMap.getAllDssMap();
		Set<DSSIdentifier> destSet = copyMap.keySet();
		Iterator<DSSIdentifier> destIter = destSet.iterator();
		Set<DSSIdentifier>copyIds = new HashSet();
		DSSIdentifier destDssId;
		while (destIter.hasNext())  // find unique dest DSSIdentifiers
		{
			destDssId = destIter.next();
			copyIds.add(destDssId);
		}
		destIter = copyIds.iterator();
		while ( destIter.hasNext() )
		{
			destDssId = destIter.next();
			destDssId = saveDssPath(destDssId);
			if ( destDssId != null )
			{
				pathsRenamed.add(destDssId);
			}
		}
	}

	/**
	 * @param srcDssId
	 */
	private DSSIdentifier saveDssPath(DSSIdentifier srcDssId)
	{
		Vector<String> srcList= new Vector<>();
		Vector<String> destList= new Vector<>();

		String dssPath = srcDssId.getDSSPath();
		String dssFile = srcDssId.getFileName();
		String dssFileAbs = Project.getCurrentProject().getAbsolutePath(dssFile);
		srcDssId.setFileName(dssFileAbs);
		fillInSrcAndDestList(dssFileAbs, dssPath, srcList, destList, true);
		HecTime[] timeWindow = DssFileManagerImpl.getDssFileManager().getTSTimeRange(srcDssId, 0);
		if ( timeWindow != null && timeWindow.length == 2)
		{
			_sim.addComputeMessage("Renaming "+srcDssId+" for time window "+timeWindow[0] +" to "+timeWindow[1]);
		}
		if ( srcList.size() > 0 )
		{
			LOGGER.atInfo().log("Found " + srcList + " records for " + dssPath + " in " + dssFileAbs + " copying to " + destList);
			_sim.addComputeMessage("Found " + srcList + " records for " + dssPath + " in " + dssFileAbs);

			int rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFileAbs, srcList, destList);

			if (rv == srcList.size())  // renamed all the records
			{
				DSSIdentifier dssId = new DSSIdentifier(dssFileAbs, dssPath);
				return dssId;
			} else
			{
				LOGGER.atWarning().log("Failed to save off all DSS records for " + dssPath + ".  Expected to save " + srcList.size() + " saved " + rv);
				_sim.addWarningMessage("Failed to save off all DSS records for " + dssPath + ".  Expected to save " + srcList.size() + " saved " + rv);
			}
		}
		else
		{
			LOGGER.atInfo().log("Found no records for " + dssPath + " in " + dssFileAbs +" to save off");
		}
		return null;
	}

	/**
	 * @param dssFile
	 * @param srcList
	 * @param destList
	 */
	private static void fillInSrcAndDestList(String dssFile, String dssPath,
			List<String> srcList, List<String> destList, boolean addSaveSuffix)
	{
		List<String> paths = findPathnamesFor(dssFile, dssPath);
		srcList.addAll(paths);
		
		
		DSSPathname pathname = new DSSPathname();
		for (int i = 0;i < srcList.size(); i++ )
		{
			pathname.setPathname(srcList.get(i));
			String fpart = pathname.getFPart();
			if ( addSaveSuffix )
			{
				fpart = fpart.concat(SAVE_SUFFEX);
			}
			else
			{
				if ( fpart.toLowerCase().endsWith(SAVE_SUFFEX))
				{
					fpart = fpart.toLowerCase().replace(SAVE_SUFFEX, "");
				}
			}
			pathname.setFPart(fpart);
		
		
			destList.add(pathname.getPathname());
		}
	}

	/**
	 * @param dssPath
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
	 * 
	 * @param member
	 * @return
	 */
	private boolean copyDssMembersForTimeWindow(int member, BaseComputeSettings computeSettings)
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
		int startingYear = findStartingYear(computeSettings);
		if ( startingYear < 0 )
		{
			_sim.addErrorMessage("Failed to find common starting year for override DSS data");
			return false;
		}
		_sim.addComputeMessage("Common start year for modified BCs:" +startingYear);
		
		RunTimeWindow rtw = _sim.getRunTimeWindow();
		HecTime startTime = (HecTime) rtw.getStartTime().clone();
		int year = startingYear;
		year+=member;
		// setYearMonthDay() day is 0 based, day() is 1 based so we have to subtract one to get the correct day
		startTime.setYearMonthDay(year, startTime.month(), startTime.day()-1);
		int startDaysToSubtract = -Integer.getInteger("PAC.StartDaysToSubtract", 0);
		startTime.addDays(startDaysToSubtract);
		
		HecTime endTime = (HecTime) rtw.getEndTime().clone();
		
		year = startingYear;
		year+=member; // 0 based.  first member doesn't add to the year.
		endTime.setYearMonthDay(year, endTime.month(), endTime.day());	
		int endDaysToAdd = Integer.getInteger("PAC.EndDaysToAdd", 1);
		endTime.addDays(endDaysToAdd); // sometimes DSS read misses a day of data.
		
		for (int i = 0;i < modelAlts.size() && !_canceled;i ++ )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			maSettings = computeSettings.getModelAltSettings(modelAlt);
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
					fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName());
					srcDssId.setFileName(fileName);
					srcDssId.setDSSPath(dssPath);
					srcDssId.setStartTime(startTime);
					srcDssId.setEndTime(endTime);
					_sim.addComputeMessage("Copying over Position Analysis DSS records for time window "+srcDssId.getStartTime()+" to "+srcDssId.getEndTime());
					TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true);
					if ( srcTsc != null && srcTsc.numberValues > 0 )
					{
						HecTime srcStart = srcTsc.getStartTime();
						srcStart.showTimeAsBeginningOfDay(true);
						_sim.addComputeMessage("Read data for " + srcDssId+" start="+srcStart+" end="+srcTsc.getEndTime()+" num values="+srcTsc.numberValues);
						srcTsc.fileName = dssDl.get_dssFile();
						srcTsc.fullName = dssDl.getDssPath();
						//no time shift the data back to the original time window
						srcTsc = shiftInTime(srcTsc, startTime, rtw);
						if ( srcTsc == null )
						{
							_sim.addErrorMessage("Failed to shift DSS record for "+dataLoc+", "+srcTsc.fileName+" : "+srcTsc.fullName+" to original time" );
							return false;
							
						}
						outputTimeSeries(srcTsc);
						int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc);
						if ( rv != 0 )
						{
							copySuccessful= false;
							Logger.getLogger(ForecastActionComputable.class.getName()).warning("Failed to write DSS record for "+dataLoc+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
							_sim.addErrorMessage("Failed to write DSS record for "+dataLoc+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
						}
						else
						{
							/// save off the source DSS data into the collection dss file with the F part appended with -PA
							srcTsc.fileName = getCollectionsOutputDssFile(computeSettings.getCollectionDssFilename()); 
							pathname.setPathname(srcTsc.fullName);
							pathname.setCollectionSequence(member);
							pathname.setFPart(pathname.getFPart()+"-PA");
							srcTsc.fullName = pathname.getPathname();
							rv = DssFileManagerImpl.getDssFileManager().write(srcTsc);
							_sim.addComputeMessage("   Copied " + srcDssId+ " to "+dssDl.get_dssFile()+":"+dssDl.getDssPath());
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
	 * find the common starting year for all the Time Series that are getting changed
	 * @param computeSettings 
	 * @return
	 */
	private int findStartingYear(BaseComputeSettings computeSettings)
	{
		List<ModelAlternative> modelAlts = _sim.getAllModelAlternativeList();

		ModelAlternative modelAlt;
		ModelAltIterationSettings maSettings;
		
		List<DataLocation> dataLocs;
		DataLocation dataLoc, linkedDl;
		
		DSSIdentifier dssId, srcDssId = new DSSIdentifier();
		
		DssDataLocation dssDl;
		
		String dssPath, fileName;
		HecTime[] times;
		int startYear = -1, year;
		for (int i = 0;i < modelAlts.size() && !_canceled;i ++ )
		{
			modelAlt = modelAlts.get(i);
			if ( modelAlt == null )
			{
				continue;
			}
			maSettings = computeSettings.getModelAltSettings(modelAlt);
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
					fileName = Project.getCurrentProject().getAbsolutePath(dssId.getFileName());
					srcDssId.setFileName(fileName);
					srcDssId.setDSSPath(dssPath);
					times = DssFileManagerImpl.getDssFileManager().getTSTimeRange(srcDssId, 0);	
					if ( times != null )
					{
						year = times[0].year();
						startYear = Math.max(startYear, year);
					}
					else
					{
						_sim.addWarningMessage("Failed to find start time for TS Record " + srcDssId);
					}
				}
			}
		}
		return startYear;
	}

	/**
	 * @param srcTsc
	 */
	private void outputTimeSeries(TimeSeriesContainer srcTsc)
	{
		HecTime time = new HecTime();
		for(int i = 0; i < 10; i++ )
		{
			time.set(srcTsc.times[i]);
			_sim.addLogMessage("Time="+time+" value="+srcTsc.values[i]);
		}
	}

	/**
	 * @param srcTsc
	 * @param startTime
	 * @param rtw
	 * @return
	 */
	private TimeSeriesContainer shiftInTime(TimeSeriesContainer srcTsc,
			HecTime startTime, RunTimeWindow rtw)
	{
		_sim.addComputeMessage("Time shifting "+srcTsc.fullName+" from "+startTime + " to " +rtw.getStartTime());
		int diff = rtw.getStartTime().value() -   startTime.value();
		String shift = Integer.toString(diff)+ " Minute";
		_sim.addComputeMessage("Time shifting "+srcTsc.fullName+" "+shift);
		
		TimeSeriesMath tsm;
		try
		{
			tsm = new TimeSeriesMath(srcTsc);
			tsm = (TimeSeriesMath) tsm.shiftInTime(shift);
		}
		catch (HecMathException e)
		{
			e.printStackTrace();
			return null;
		}
		TimeSeriesContainer shiftedTsc = tsm.getContainer();
		return shiftedTsc;
	}
	private boolean copyICDssData()
	{
		_computeProgressPanel.setStatusMessage("Copying Initial Condition Data ...");

		boolean copySuccessful = true;


		_sim.addComputeMessage("Copying over Initial Condition records...");


		//copy the boundary condition data
		// dest to source map
		InitialConditions icData = _simGroup.getInitialConditions();
		List<String> reservoirs = icData.getReservoirs();
		String reservoir;
		List<DSSIdentifier> destDssIds;
		DSSIdentifier destDssId;

		for(int r = 0;r < reservoirs.size(); r++ )
		{
			reservoir = reservoirs.get(r);
			DSSIdentifier srcDssId = _icDssPathMap.getSourceDSSIdentifierFor(reservoir);
			if ( srcDssId == null )
			{
				_sim.addWarningMessage("No source DSS record found for "+reservoir+"'s Initial Conditions");
				continue;
			}
			if (!RMAIO.isFullPath(srcDssId.getFileName()))
			{
				srcDssId.setFileName(Project.getCurrentProject().getAbsolutePath(srcDssId.getFileName()));
			}


			destDssIds = _icDssPathMap.getDestDssIdentifiersFor(reservoir);
			if ( destDssIds.isEmpty() )
			{
				_sim.addWarningMessage("No DSS Destination defined for "+reservoir+"'s Initial Conditions");
				continue;
			}
			_sim.addComputeMessage("Copying over Initial Condition record for " + reservoir);
			for (int d = 0;d < destDssIds.size(); d++ )
			{
				destDssId = destDssIds.get(d);
				if ( !RMAIO.isFullPath(destDssId.getFileName()))
				{
					destDssId.setFileName(Project.getCurrentProject().getAbsolutePath(destDssId.getFileName() ));
				}
				copySuccessful |= copyPdDssRecord(srcDssId, destDssId);
			}
		}
		return copySuccessful;
	}

	/**
	 * do we have a DSSIdentifier that's null or missing its file or path
	 * @param srcDssId
	 * @return true if its missing information
	 */
	private boolean missingDssIdentifier(DSSIdentifier srcDssId)
	{
		if ( srcDssId == null
			|| srcDssId.getFileName() == null || srcDssId.getFileName().trim().isEmpty()
			|| srcDssId.getDSSPath() == null || srcDssId.getDSSPath().trim().isEmpty() || "///////".equals(srcDssId.getDSSPath()))
		{
			return true;
		}
		return false;
	}

	/**
	 * copy in the bc data
	 * @return
	 */
	private boolean copyBcDssData()
	{
		_computeProgressPanel.setStatusMessage("Copying Boundary Condition Data ...");
		DSSIdentifier  srcDssId;

		boolean copySuccessful = true;
		HecTime[] times =  getCopyTimeWindow();

		
		_sim.addComputeMessage("Copying over Boundary Condition records...");


		//copy the boundary condition data
		// dest to source map
		Map<DSSIdentifier, DSSIdentifier> copyMap = _bcDssPathMap.getDssCopyMap();
		Set<Map.Entry<DSSIdentifier, DSSIdentifier>> copyMapSet = copyMap.entrySet();
		Iterator<Map.Entry<DSSIdentifier, DSSIdentifier>> copyMapIter = copyMapSet.iterator();
		String fullPath;
		while ( copyMapIter.hasNext() )
		{
			Map.Entry<DSSIdentifier, DSSIdentifier> copyMapElement = copyMapIter.next();
			srcDssId = copyMapElement.getValue();
			if ( times != null )
			{
				srcDssId.setStartTime(times[0]);
				srcDssId.setEndTime(times[1]);
			}
			DSSIdentifier destDssId = copyMapElement.getKey();
			fullPath = Project.getCurrentProject().getAbsolutePath(destDssId.getFileName());
			destDssId.setFileName(fullPath);
			copySuccessful |= copyTsDssRecord(srcDssId, destDssId);

		}
		return copySuccessful;
	}

	/**
	 *  copy the paired data records from the source DSS file to the dest DSS file
	 * @param srcDssId
	 * @param destDssId
	 * @return
	 */
	private boolean copyPdDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId)
	{
		boolean copySuccessful = true;
		PairedDataContainer srcPdc = DssFileManagerImpl.getDssFileManager().readPairedDataContainer(srcDssId);
		if ( srcPdc != null && srcPdc.numberOrdinates > 0 )
		{
			_sim.addMessage("Copying "+srcDssId+" to "+destDssId);
			srcPdc.fileName = destDssId.getFileName();
			srcPdc.fullName = destDssId.getDSSPath();
			int rv = DssFileManagerImpl.getDssFileManager().write(srcPdc);
			if ( rv != 0 )
			{
				copySuccessful= false;
				LOGGER.atWarning().log("Failed to write PDC DSS record for "+destDssId+" to "+srcPdc.fileName+" : "+srcPdc.fullName+" rv="+rv);
				_sim.addErrorMessage("Failed to write PDC DSS record for "+destDssId+" to "+srcPdc.fileName+" : "+srcPdc.fullName+" rv="+rv);
			}
			else
			{
				DSSPathname pathname = new DSSPathname();
				/// save off the source DSS data into the collection dss file with the F part appended with -Forecast
				srcPdc.fileName = getCollectionsOutputDssFile("forecastResults.dss");
				pathname.setPathname(srcPdc.fullName);
				//pathname.setCollectionSequence(member);
				pathname.setFPart(pathname.getFPart()+"-FORECAST");
				srcPdc.fullName = pathname.getPathname();
				rv = DssFileManagerImpl.getDssFileManager().write(srcPdc);
				_sim.addComputeMessage("   Copied " + srcDssId+ " to "+destDssId);
			}
		}
		else
		{
			LOGGER.atWarning().log("Copying PDC Record, No data found to copy for "+srcDssId);
			_sim.addErrorMessage("Copying PDC Record, No Data Found for " + srcDssId );
		}
		return copySuccessful;
	}
	/**
	 *  copy the time series records from the source DSS file to the dest DSS file
	 * @param srcDssId
	 * @param destDssId
	 * @return
	 */
	private boolean copyTsDssRecord(DSSIdentifier srcDssId, DSSIdentifier destDssId)
	{
		boolean copySuccessful = true;
		TimeSeriesContainer srcTsc = DssFileManagerImpl.getDssFileManager().readTS(srcDssId, true);
		if ( srcTsc != null && srcTsc.numberValues > 0 )
		{
			_sim.addMessage("Copying "+srcDssId+" to "+destDssId);
			srcTsc.fileName = destDssId.getFileName();
			srcTsc.fullName = destDssId.getDSSPath();
			int rv = DssFileManagerImpl.getDssFileManager().write(srcTsc);
			if ( rv != 0 )
			{
				copySuccessful= false;
				LOGGER.atWarning().log("Failed to write TS DSS record for "+destDssId+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
				_sim.addErrorMessage("Failed to write TS DSS record for "+destDssId+" to "+srcTsc.fileName+" : "+srcTsc.fullName+" rv="+rv);
			}
			else
			{
				DSSPathname pathname = new DSSPathname();
				/// save off the source DSS data into the collection dss file with the F part appended with -Forecast
				srcTsc.fileName = getCollectionsOutputDssFile("forecastResults.dss");
				pathname.setPathname(srcTsc.fullName);
				//pathname.setCollectionSequence(member);
				pathname.setFPart(pathname.getFPart()+"-FORECAST");
				srcTsc.fullName = pathname.getPathname();
				rv = DssFileManagerImpl.getDssFileManager().write(srcTsc);
				_sim.addComputeMessage("   Copied " + srcDssId+ " to "+destDssId);
			}
		}
		else
		{
			if (srcDssId.getTimeWindow() == null )
			{
				LOGGER.atWarning().log("No data found to copy for "+srcDssId);
				_sim.addErrorMessage("No Data Found for " + srcDssId );
			}
			else
			{
				LOGGER.atWarning().log("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime());
				_sim.addErrorMessage("No Data Found for " + srcDssId + " for Time Window " + srcDssId.getStartTime() + " to " + srcDssId.getEndTime());
			}
		}
		return copySuccessful;
	}

	/**
	 * [0] = start time
	 * [1] = end time
	 * @return can be null
	 */
	private HecTime[] getCopyTimeWindow()
	{
		if (Boolean.getBoolean("Forecast.OnlyCopyTimeWindow"))
		{
			HecTime[] times = new HecTime[2];
			RunTimeWindow rtw = _sim.getRunTimeWindow();
			times[0] = (HecTime) rtw.getStartTime().clone();
			times[1] = (HecTime) rtw.getEndTime().clone();

			int startDaysToSubtract = -Integer.getInteger("Forecast.StartDaysToSubtract", 0);
			times[0].addDays(startDaysToSubtract);


			int endDaysToAdd = -Integer.getInteger("Forecast.EndDaysToAdd", 0);
			times[1].addDays(endDaysToAdd);
			return times;
		}
		return null;
	}

	private boolean copyTempTargetMember(EnsembleSet eset, int currentMember)
	{
		_computeProgressPanel.setStatusMessage("Copying Temperature Targets Data...");
		TemperatureTargetSet ttSet = eset.getTemperatureTargetSet();
		List<DSSPathname> pathnames = ttSet.getDssPathNames(TemperatureTargetTimeStep.REGULAR_HOURLY);
		if ( pathnames.size() < currentMember )
		{
			_sim.addErrorMessage("Temperature Target collection size "+pathnames.size()+" smaller than current member "+currentMember);
			return false;
		}
		if ( currentMember > 0 )
		{
			currentMember--;
		}
		DSSPathname pathname = pathnames.get(currentMember);

		Path filePath = ttSet.getDssOutputPath();

		List<DSSIdentifier> destDssIdentifiers = _tempTargetDssPathMap.getDestDssIdentifiersFor(pathname.getPathname(), TemperatureTargetTimeStep.REGULAR_HOURLY);

		DSSIdentifier srcDssId = new DSSIdentifier(Project.getCurrentProject().getAbsolutePath(filePath.toString()),pathname.getPathname());
		HecTime[] times = getCopyTimeWindow();
		boolean copySuccessful = true;
		DSSIdentifier destDssId;
		if ( destDssIdentifiers.size() == 0 )
		{
			_sim.addWarningMessage("No Temperature Targets were found to copy for temp target path "+pathname);
			_sim.addWarningMessage("The temp Target Config file "+TEMP_TARGET_CONFIG_FILE+" source pathnames have to match "+pathname);
			return true;
		}
		for (int i = 0;i < destDssIdentifiers.size(); i ++ )
		{
			destDssId = destDssIdentifiers.get(i);
			destDssId.setFileName(Project.getCurrentProject().getAbsolutePath(destDssId.getFileName()));
			_sim.addComputeMessage("Copying Temperature Target pathname from "+srcDssId+" to " + destDssId);
			copySuccessful |= copyTsDssRecord(srcDssId, destDssId);
		}

		return copySuccessful;
	}
	/**
	 * @return  <simulation dss file>-forecast.dss
	 */
	private String getCollectionDssFilename()
	{
		String dssFile = _sim.getSimulationDssFile();
		int idx = dssFile.lastIndexOf('.');
		dssFile = dssFile.substring(0,idx);
		dssFile = dssFile.concat("-forecast.dss");
		return dssFile;

	}

	/**
	 * @param savedDssPaths
	 */
	private void restoreDssPaths(List<DSSIdentifier> savedDssPaths)
	{
		_computeProgressPanel.setStatusMessage("Restoring original records ...");
		_sim.addComputeMessage("Restoring original "+savedDssPaths.size()+" DSS records ...");
		Vector<String> srcList= new Vector<>();
		Vector<String> destList= new Vector<>();
		DSSIdentifier dssId;
		String path, dssFile;
		Vector<String>singleSrcList = new Vector();
		Vector<String>singleDestList = new Vector();
		DSSPathname pathname  = new DSSPathname();
		for (int i = 0;i < savedDssPaths.size();i ++ )
		{
			srcList.clear();
			destList.clear();
			
			dssId = savedDssPaths.get(i);
			_sim.addComputeMessage("Restoring DSS path for "+dssId);
			path = dssId.getDSSPath();
			pathname.setPathname(path);
			String fpart = pathname.getFPart();
			fpart = fpart.concat(SAVE_SUFFEX);
			pathname.setFPart(fpart);
			path = pathname.getPathname();
			
			dssFile = dssId.getFileName();
			
			fillInSrcAndDestList(dssFile, path, srcList, destList, false);
			if ( destList.size() != srcList.size() )
			{
				_sim.addWarningMessage("Mismatched source and dest lists for "+dssId);
				_sim.addWarningMessage("Source List="+srcList);
				_sim.addWarningMessage("Dest List="+destList);
			}
			int size = Math.min(srcList.size(), destList.size());
			for (int s = 0;s < size; s++ )
			{
				singleSrcList.clear();
				singleDestList.clear();
				singleSrcList.add(srcList.get(s));
				singleDestList.add(destList.get(s));
				int rv = DssFileManagerImpl.getDssFileManager().delete(dssFile, singleDestList);
				if ( rv != 0 )
				{
					LOGGER.atWarning().log("Failed to delete DSS records for "+dssFile+":"+singleDestList.get(0)+" Rv="+rv);
					_sim.addWarningMessage("Failed to delete DSS records for "+dssFile+":"+singleDestList.get(0));
				}

				_sim.addComputeMessage("Restoring "+singleSrcList+" to "+singleDestList);
				rv = DssFileManagerImpl.getDssFileManager().renameRecords(dssFile, singleSrcList, singleDestList);
				if ( rv != singleSrcList.size() )
				{
					LOGGER.atWarning().log("Failed to restore DSS records for "+dssFile+":"+singleDestList.get(0));
					LOGGER.atWarning().log("Expected " + singleSrcList.size()+" records to be restored. Restored "+rv+" Records.");

					_sim.addWarningMessage("Failed to restore DSS records for "+dssFile+":"+singleDestList.get(0));
					_sim.addWarningMessage("Expected " + singleSrcList.size()+" records to be restored. Restored "+rv+" Records.");
				}
			}
		}
	}

	/**
	 * 
	 */
	private void copyDssResultsToCollectionsDss(int interationId, int outputCollectionStart)
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
			updateIterationDssWithDssData(modelAlt, interationId+outputCollectionStart);
		}
	}	
	/**
	 * copy the computed dss records back from the ensemble dss records to the real 
	 * forecast DSS file
	 * @param modelAlt
	 */
	private boolean updateIterationDssWithDssData(ModelAlternative modelAlt, int iterationId)
	{
			
		_sim.addComputeMessage("Saving Computed DSS records to collection "+iterationId+" for "+modelAlt);
		
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
			+" model "+modelAlt+" to "+getCollectionDssFilename());
		DSSPathname pathname = new DSSPathname();
		Vector<String>destPaths = new Vector<>(srcPaths.size());
		String path;
		for(int i = 0; i < srcPaths.size() && !_canceled;i++ )
		{
			path = srcPaths.get(i);
			pathname.setPathname(path);
			pathname.setCollectionSequence(iterationId);
			destPaths.add(pathname.getPathname());
		}
		String iterDssFile = getCollectionsOutputDssFile(getCollectionDssFilename());
		int rv = copyRecords(dssFile, iterDssFile, srcPaths, destPaths);
		boolean success = rv == srcPaths.size();
		if ( !success )
		{
			_sim.addErrorMessage("Failed to update forecast DSS file with "
					+modelAlt.getProgram()+" model "+modelAlt+"'s results");
		}
		return success;
		
	}
	/**
	 * @return
	 */
	private String getCollectionsOutputDssFile(String dssFileName)
	{
		if ( _iterDssFile == null )
		{
			String dssFile = _sim.getSimulationDssFile();
			String computeFolder = RMAIO.getDirectoryFromPath(dssFile);
			_iterDssFile = RMAIO.concatPath(computeFolder, dssFileName);
		}
		return _iterDssFile;
	}

	/**
	 * copy the records from one dss file to another.
	 * @param fromDssFile
	 * @param toDssFile
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
	public int getNumberRealizations()
	{
		return _memberCnt;
	}

	@Override
	public int getNumberLifeCycles()
	{
		return _memberCnt;
	}

	@Override
	public int getInitialLifeCycle()
	{
		return 0;
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


	public void setComputeDialog(ComputeProgressDialog computeDialog)
	{
		_computeDialog = computeDialog;
	}
}
