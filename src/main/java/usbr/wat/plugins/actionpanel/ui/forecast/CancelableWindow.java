/*
 * Copyright (c) 2023.
 *    Hydrologic Engineering Center (HEC).
 *   United States Army Corps of Engineers
 *   All Rights Reserved.  HEC PROPRIETARY/CONFIDENTIAL.
 *   Source may not be released without written approval
 *   from HEC
 */
package usbr.wat.plugins.actionpanel.ui.forecast;

import rma.swing.RmaJDialog;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class CancelableWindow extends RmaJDialog
{
    protected boolean _canceled = true;
    public CancelableWindow(Window parent, String title, boolean modal)
    {
        super(parent, title, modal);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addCloseListener();
    }

    private void addCloseListener()
    {
        addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                _canceled = true;
                setVisible(false);
            }
        });
    }

    public abstract boolean isCanceled();


}
