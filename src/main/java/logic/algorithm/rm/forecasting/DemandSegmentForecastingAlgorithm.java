package logic.algorithm.rm.forecasting;

import data.entity.DemandSegmentForecastSet;
import logic.algorithm.Algorithm;

public interface DemandSegmentForecastingAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract DemandSegmentForecastSet getResult();
}
