/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import hec.io.DSSIdentifier;
import hec2.wat.model.WatSimulation;

import java.util.List;

public final class TempTargetDssPathMap extends DssPathMap
{
    public TempTargetDssPathMap(WatSimulation sim, String configFile)
    {
        super(sim, configFile);
    }

    public List<DSSIdentifier> getDestDssIdentifiersFor(String srcDssPath, TemperatureTargetTimeStep timeStep)
    {
        return getDestDssIdentifiersFor(srcDssPath, timeStep.toString());
    }
}
