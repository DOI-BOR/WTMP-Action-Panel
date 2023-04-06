/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */

package usbr.wat.plugins.actionpanel.actions.forecast;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import com.rma.client.Browser;
import hec2.wat.model.WatSimulation;
import rma.util.IntArray;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;
import usbr.wat.plugins.actionpanel.model.ActionComputable;
import usbr.wat.plugins.actionpanel.model.IterationSettings;
import usbr.wat.plugins.actionpanel.model.PositionAnalysisSettings;
import usbr.wat.plugins.actionpanel.model.SimulationGroup;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastActionComputable;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.UsbrPanel;
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;

public class RunForecastSimulationAction extends AbstractAction
{
	private final ActionsWindow _parent;
	private final SimulationPanel _parentPanel;

	public RunForecastSimulationAction(ActionsWindow parent, SimulationPanel parentPanel)
	{
		super("Run Simulation");
		setEnabled(false);
		_parent = parent;
		_parentPanel = parentPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		boolean recomputeAll = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		computeSimulationAction(recomputeAll);
	}

	public void computeSimulationAction(boolean recomputeAll)
	{

		ForecastSimGroup simGroup = _parent.getForecastPanel().getSimulationGroup();
		if ( simGroup == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please create or select a Simulation Group first",
					"No Simulation Group Selected", JOptionPane.INFORMATION_MESSAGE);
			return;

		}
		List<WatSimulation> sims = _parent.getSelectedSimulations();
		if ( sims.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulations that you want to compute",
					"No Simulations Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		List<EnsembleSet> selectedESets = _parentPanel.getSelectedEnsembleSets();
		if ( selectedESets.isEmpty())
		{
			JOptionPane.showMessageDialog(_parent,"Please select the Ensemble Sets that you want to compute",
					"No Ensemble Sets Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		EnsembleSet eset;
		String memberSet;
		int[] members;
		Map<EnsembleSet, int[]> esetMap = new HashMap<>();
		for(int i = 0;i < selectedESets.size(); i++ )
		{
			eset = selectedESets.get(i);
			memberSet = eset.getMemberSetToCompute();
			members = getIntegerSet(memberSet);
			if ( members == null || members.length == 0 )
			{
				JOptionPane.showMessageDialog(_parent,"Please enter the target members to compute for Ensemble Set "+eset,
						"No Ensemble Members to Compute", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			esetMap.put(eset, members);
		}

		List<ForecastActionComputable>computables = new ArrayList<>();
		Iterator<WatSimulation> simsIter = sims.iterator();
		Set<Map.Entry<EnsembleSet, int[]>> esetSet = esetMap.entrySet();
		Iterator<Map.Entry<EnsembleSet, int[]>> esetIter = esetSet.iterator();
		ForecastActionComputable computable;
		WatSimulation sim ;
		IterationSettings iterSettings;
		PositionAnalysisSettings posAnalysisSettings;
		UsgsComputeSelectorDialog computeDlg = new UsgsComputeSelectorDialog(Browser.getBrowserFrame(),WatSimulation.class);
		Map.Entry<EnsembleSet, int[]> esetEntry;
		while ( simsIter.hasNext())
		{
			sim = simsIter.next();
			while (esetIter.hasNext())
			{
				esetEntry = esetIter.next();

				members = esetEntry.getValue();
				eset = esetEntry.getKey();

				computable = new ForecastActionComputable(sim, eset, members);
				computable.setProgressDialog(computeDlg);
				computables.add(computable);
			}
		}
		computeDlg.setRecomputeAll(recomputeAll);
		//computeDlg.setSelectedComputables(computables);
		computeDlg.setSelectOutOfDate(false);
		computeDlg.setComputeOnOpen(true);
		computeDlg.setVisible(true);
		_parentPanel.updateComputeStates();
	}

	public int[] getIntegerSet(String txt)
	{
		IntArray values = new IntArray();
		if (txt.isEmpty())
		{
			return null;
		}
		else
		{
			StringTokenizer tokenizer = new StringTokenizer(txt, ",");

			for(String word = tokenizer.nextToken().trim(); word != null && !word.isEmpty(); word = tokenizer.nextToken().trim())
			{
				if (word.contains("-"))
				{
					StringTokenizer tokenizer2 = new StringTokenizer(word, "-");
					String word2 = tokenizer2.nextToken().trim();
					if (isParsableToInt(word2))
					{
						int startRangeValue = Integer.parseInt(word2);
						word2 = tokenizer2.nextToken().trim();
						if (isParsableToInt(word2))
						{
							int endRangeValue = Integer.parseInt(word2);

							for(int i = startRangeValue; i <= endRangeValue; ++i)
							{
								if (!values.contains(i))
								{
									values.add(i);
								}
							}
						}
					}
				}

				if (isParsableToInt(word))
				{
					int value = Integer.parseInt(word);
					if (!values.contains(value))
					{
						values.add(value);
					}
				}

				if (!tokenizer.hasMoreTokens())
				{
					return values.toArray();
				}
			}

			return values.toArray();
		}
	}

	public boolean isParsableToInt(String i)
	{
		try
		{
			Integer.parseInt(i);
			return true;
		}
		catch (NumberFormatException var3)
		{
			return false;
		}
	}
}
