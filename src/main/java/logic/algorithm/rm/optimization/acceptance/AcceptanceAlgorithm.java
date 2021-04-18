package logic.algorithm.rm.optimization.acceptance;

import data.entity.OrderSet;
import logic.algorithm.Algorithm;

public interface AcceptanceAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract OrderSet getResult();
}
