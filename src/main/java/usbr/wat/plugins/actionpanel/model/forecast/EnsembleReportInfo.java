/*
 *
 *  * Copyright 2023 United States Bureau of Reclamation (USBR).
 *  * United States Department of the Interior
 *  * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 *  * Source may not be released without written approval
 *  * from USBR
 *
 */

package usbr.wat.plugins.actionpanel.model.forecast;

public class EnsembleReportInfo
{
	private final EnsembleSet _eset;
	private final int[] _memberSet;



	public EnsembleReportInfo(EnsembleSet eset, int[] memberSet)
	{
		super();
		_eset = eset;
		_memberSet = memberSet;
	}

	public int[] getMembersToReportOn()
	{
		return _memberSet;
	}

	public EnsembleSet getEnsembleSet()
	{
		return _eset;
	}


}
