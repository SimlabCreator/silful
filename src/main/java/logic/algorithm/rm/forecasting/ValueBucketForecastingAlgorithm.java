package logic.algorithm.rm.forecasting;

import data.entity.ValueBucketForecastSet;
import logic.algorithm.Algorithm;

public interface ValueBucketForecastingAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract ValueBucketForecastSet getResult();
}
