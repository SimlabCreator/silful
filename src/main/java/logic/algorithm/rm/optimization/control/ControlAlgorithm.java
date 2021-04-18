package logic.algorithm.rm.optimization.control;

import data.entity.ControlSet;
import logic.algorithm.Algorithm;

public interface ControlAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract ControlSet getResult();
}
