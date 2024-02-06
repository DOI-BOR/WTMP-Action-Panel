/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */

package usbr.wat.plugins.actionpanel.model.forecast;

import hec.heclib.dss.DSSPathname;
import hec.io.DSSIdentifier;
import hec2.wat.model.WatSimulation;

import java.util.List;

public final class TempTargetDssPathMap extends DssPathMap
{
    private final TemperatureTargetSet _tempTargetSet;

    public TempTargetDssPathMap(WatSimulation sim, String configFile, TemperatureTargetSet temperatureTargetSet)
    {
        super(sim, configFile);
        _tempTargetSet = temperatureTargetSet;
    }

    public List<DSSIdentifier> getDestDssIdentifiersFor(String srcDssPath, TemperatureTargetTimeStep timeStep)
    {
        return getDestDssIdentifiersFor(srcDssPath, timeStep.toString());
    }

    @Override
    public boolean readDssPathsFile()
    {
        boolean retVal = super.readDssPathsFile();
        if(retVal && !_dssPathMapList.isEmpty())
        {
            DssPathMapItem mapping = _dssPathMapList.get(0); //only use first mapping in config for temp targets
            int numberOfDests = mapping.getNumberOfDests();
            _dssPathMapList.clear();
            List<DSSPathname> dssPathNames = _tempTargetSet.getDssPathNames(TemperatureTargetTimeStep.REGULAR_HOURLY);
            for (DSSPathname pathName : dssPathNames)
            {
                DssPathMapItem item = new DssPathMapItem(_tempTargetSet.getDssSourcePath().toString(), _tempTargetSet.getFPartWithoutCollection());
                for(int i=0; i < numberOfDests; i++)
                {
                    String destFile = mapping.getDestDssFile(i);
                    String destPath = mapping.getDestDssPath(i);
                    item.setSourceDssPath(pathName.getPathname());
                    item.addMapping(destFile, destPath);
                }
                _dssPathMapList.add(item);
            }
        }
        return retVal;

    }
}
