package logic.algorithm.rm.optimization.control;

import data.entity.DynamicProgrammingTree;
import logic.algorithm.Algorithm;

public interface DynamicProgrammingAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract DynamicProgrammingTree getResult();
}
