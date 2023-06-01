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
import javax.swing.JOptionPane;
import com.rma.client.Browser;
import com.rma.editors.ComputeProgressDialog;
import hec2.wat.model.WatSimulation;
import rma.util.IntArray;
import usbr.wat.plugins.actionpanel.ActionsWindow;
import usbr.wat.plugins.actionpanel.editors.iterationCompute.UsgsComputeSelectorDialog;
import usbr.wat.plugins.actionpanel.model.UsbrComputable;
import usbr.wat.plugins.actionpanel.model.forecast.EnsembleSet;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastActionComputable;
import usbr.wat.plugins.actionpanel.model.forecast.ForecastSimGroup;
import usbr.wat.plugins.actionpanel.ui.forecast.SimulationPanel;

public class RunForecastSimulationAction extends AbstractAction
{
	private final ActionsWindow _parent;
	private final SimulationPanel _parentPanel;
	private ComputeProgressDialog _computeDialog;

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
		WatSimulation sim = _parentPanel.getSelectedSimulation();
		if ( sim == null )
		{
			JOptionPane.showMessageDialog(_parent,"Please select the simulation that you want to compute",
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
		recomputeAll = recomputeAll || _parentPanel.shouldRecomputeAll();


		ForecastActionComputable computable = new ForecastActionComputable(simGroup, sim, selectedESets, recomputeAll);
		_computeDialog = new ComputeProgressDialog(Browser.getBrowserFrame(), computable);
		computable.setComputeDialog(_computeDialog);
		_computeDialog.setVisible(true);

		_parentPanel.updateComputeStates();
	}

	public static int[] getIntegerSet(String txt)
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

	public static boolean isParsableToInt(String i)
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
