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

public abstract class CancelableWindow<T> extends RmaJDialog
{

    public CancelableWindow(Window parent, String title, boolean modal)
    {
        super(parent, title, modal);
    }
    public abstract boolean isCanceled();
}
