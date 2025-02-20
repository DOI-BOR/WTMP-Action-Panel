package usbr.wat.plugins.actionpanel.model.forecast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.flogger.FluentLogger;
import com.rma.io.DssFileManager;
import com.rma.io.DssFileManagerImpl;
import hec.heclib.util.HecTime;
import hec.hecmath.computation.ComputationException;
import hec.hecmath.functions.TimeSeriesFunctions;
import hec.io.DSSIdentifier;
import hec.io.TimeSeriesContainer;
import org.jfree.data.time.TimeSeries;
import rma.util.RMAConst;

/**
 * validates the met DSS data
 */
public class MetDataDssValidator
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();


	private final List<Integer> _years;
	private List<DSSIdentifier> _dssEntries;
	private HashMap<Integer, List<String>> _invalidYears;
	private HecTime _startTime = new HecTime("01Jan1900 0000");
	private HecTime _endTime = new HecTime("31Dec1900 2400");
	private String _dssUnits = "";
	private List<String> _errors = new ArrayList<>();

	public MetDataDssValidator(List<DSSIdentifier> dssEntries)
	{
		super();
		_dssEntries = dssEntries;
		_years = getYears();
	}

	/**
	 * get the years in the DSS records
	 * @return
	 */
	public List<Integer> getYears()
	{
		if ( _years != null )
		{
			return _years;
		}
		if ( _dssEntries.size() == 0 )
		{
			return null;
		}
		DSSIdentifier dssId = _dssEntries.get(0);
		HecTime[] timeRange = DssFileManagerImpl.getDssFileManager().getTSTimeRange(dssId, 0);
		if ( timeRange == null )
		{
			LOGGER.atInfo().log("No TimeRange found for Time Series %s",dssId);
			_errors.add("No Time Window found to determine years for Time Series "+dssId);
			return null;
		}
		int startYear = timeRange[0].year();
		int endYear = timeRange[1].year();
		List<Integer> years = new ArrayList<>(endYear - startYear + 1);
		int dssSize = _dssEntries.size();
		DSSIdentifier dssId2;
		_invalidYears = new HashMap<>();
		for(int y = startYear; y <= endYear; y++ )
		{
			checkYearForData(dssId, y, y == startYear);
			years.add(y);
			for (int d = 0;d < dssSize; d++ )
			{
				dssId2 = _dssEntries.get(d);
				checkYearForData(dssId2, y, false);

			}
		}
		return years;
	}

	/**
	 *
	 * @param dssId2
	 * @param y
	 */
	private void checkYearForData(DSSIdentifier dssId2, int y, boolean setUnits)
	{
		if (!hasYear(dssId2, y, setUnits))
		{
			List<String> ids = _invalidYears.get(y);
			if ( ids == null )
			{
				ids = new ArrayList<>();
				_invalidYears.put(y, ids);
				_errors.add("Found Missing data for the year "+y+" for Time Series:"+dssId2);
			}
			ids.add(dssId2.toString());
		}
	}

	/**
	 * check if a DSS record has data for the year specified
	 * @param dssId
	 * @param year
	 * @return true if it has data for the year
	 */
	private boolean hasYear(DSSIdentifier dssId, int year, boolean setUnits)
	{
		_startTime.setYearMonthDay(year, 1,1);
		_startTime.setTime("0001");
		_endTime.setYearMonthDay(year, 12, 31);
		dssId.setTimeWindow(_startTime, _endTime);

		LOGGER.atFine().log("Check for data for %s for year %d", dssId, year);

		TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, false);
		if ( tsc == null || tsc.numberValues == 0 ) // need better test
		{
			LOGGER.atInfo().log("failed to read Time Series %s to determine years", dssId);
			_errors.add("Failed to read Time Series "+dssId+" to determine years");
			return false;
		}
		if ( setUnits )
		{
			_dssUnits = tsc.units;
		}
		int missing = 0;
		try
		{
			missing = TimeSeriesFunctions.numberMissingValues(tsc);
		}
		catch (ComputationException e)
		{
			LOGGER.atInfo().log("error %s checking for missing values in %s ",e.getMessage(), dssId);
			_errors.add("Error checking for valid data in "+dssId+". Error:"+e.getMessage());
		}
		if ( missing > 0 )
		{
			return false;
		}
		return true;
	}

	/**
	 * return the years that not all DSS records have and the records that are missing that year
	 * @return
	 */
	public Map<Integer, List<String>> getInvalidYears()
	{
		return _invalidYears;
	}

	public Map<Integer, Double> getSpringAverages()
	{
		Map<Integer, Double> springAverages = new HashMap<>();
		getQuarterAverages(springAverages, 3,1, 5,31);
		return springAverages;
	}

	/**
	 * get the spring, march, april, may averages from the first DSS record
	 *
	 * @return
	 */
	public Map<Integer, Double> getQuarterAverages(Map<Integer, Double> quarterAverages, int startMonth, int startDay, int endMonth, int endDay)
	{
		if (_dssEntries.size() == 0 || _years == null)
		{
			return quarterAverages;
		}
		DSSIdentifier dssId = _dssEntries.get(0);
		int year;
		for (int y = 0; y < _years.size(); y++)
		{
			year = _years.get(y);
			_startTime.setYearMonthDay(year, startMonth, startDay);
			_startTime.setTime("0001");
			_endTime.setYearMonthDay(year, endMonth, endDay);

			TimeSeriesContainer tsc = DssFileManagerImpl.getDssFileManager().readTS(dssId, true);
			if ( tsc != null && tsc.numberValues > 0 )
			{
				double avg= average(tsc);
				quarterAverages.put(year, avg);
			}
			else
			{
				LOGGER.atInfo().log("Failed to read Time Series %s",dssId);
				_errors.add("Failed to read Time Series "+dssId+" to calculate quarterly averages"+dssId);
			}
		}
		return quarterAverages;
	}

	private double average(TimeSeriesContainer tsc)
	{
		double sum = 0;
		int cnt = 0;
		for (int i = 0;i < tsc.numberValues;i++)
		{
			if (RMAConst.isValidValue(tsc.values[i]))
			{
				sum += tsc.values[i];
				cnt++;
			}
		}
		LOGGER.atFine().log("Sum %d, Count %d for %s", sum, cnt, tsc);
		return sum/cnt;
	}

	/**
	 * get the summer, june, july, august averages from the first DSS record
	 * @return
	 */
	public Map<Integer, Double> getSummerAverages()
	{
		Map<Integer, Double> summerAverages = new HashMap<>();
		getQuarterAverages(summerAverages, 6,1, 8,31);
		return summerAverages;
	}

	/**
	 * get the fall averages, sept, october, november from the first DSS record
	 * @return
	 */
	public Map<Integer, Double> getFallAverages()
	{
		Map<Integer, Double> fallAverages = new HashMap<>();
		getQuarterAverages(fallAverages, 9,1, 11,30);
		return fallAverages;
	}
	public String getDssUnits()
	{
		return _dssUnits;
	}

	public List<String> getErrors()
	{
		return _errors;
	}
}
