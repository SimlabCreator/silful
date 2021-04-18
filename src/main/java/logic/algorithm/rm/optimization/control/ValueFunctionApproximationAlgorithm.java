package logic.algorithm.rm.optimization.control;

import data.entity.ValueFunctionApproximationModelSet;
import logic.algorithm.Algorithm;

public interface ValueFunctionApproximationAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract ValueFunctionApproximationModelSet getResult();
}
