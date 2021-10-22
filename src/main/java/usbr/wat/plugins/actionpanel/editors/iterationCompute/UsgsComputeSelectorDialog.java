
package usbr.wat.plugins.actionpanel.editors.iterationCompute;

import java.awt.Frame;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import com.rma.swing.tree.DefaultCheckBoxNode;

import hec2.wat.client.WatComputeSelectorDialog;
import hec2.wat.model.WatSimulation;

import usbr.wat.plugins.actionpanel.model.ActionComputable;

/**
 * @author Mark Ackerman
 *
 */
@SuppressWarnings("serial")
public class UsgsComputeSelectorDialog extends WatComputeSelectorDialog
{

	/**
	 * @param browserFrame
	 * @param class1
	 */
	public UsgsComputeSelectorDialog(Frame parent,
			Class<WatSimulation> simClass)
	{
		super(parent, simClass);
	}

	/**
	 * @param computables
	 */
	public void setSelectedComputables(List<ActionComputable> computables)
	{
		if ( computables == null )
		{
			return;
		}
		ActionComputable computable;
		deselectAllTreeNodes();
		for (int i = 0;i < computables.size(); i++ )
		{
			computable = computables.get(i);
			selectSimulation(computable);
		}
	}
	private boolean selectSimulation(ActionComputable computable )
	{
		if ( computable == null )
		{
			return true;
		}
		return selectSimulation(_root, computable);	
	}
	private boolean selectSimulation(DefaultMutableTreeNode parent, ActionComputable computable)
	{
		DefaultCheckBoxNode item;
		boolean selected = false;
		String simName = computable.getName();
		for (int i = 0;i < parent.getChildCount();i++)
		{
			item = (DefaultCheckBoxNode)parent.getChildAt(i);
			if ( !item.isLeaf())
			{
				if ( selectSimulation(item, computable))
				{
					return true;
				}
			}
			if ( item.getUserObject().toString().equals(simName) )
			{
				item.setSelected(true);
				item.setUserObject(computable);
				selected = true;
			}
		}
		return selected;
	}

}
