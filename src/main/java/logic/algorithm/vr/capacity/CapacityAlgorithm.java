package logic.algorithm.vr.capacity;

import data.entity.CapacitySet;
import logic.algorithm.Algorithm;

public interface CapacityAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract CapacitySet getResult();
}
