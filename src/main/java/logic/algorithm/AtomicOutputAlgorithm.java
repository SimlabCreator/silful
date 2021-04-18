package logic.algorithm;

import data.entity.GeneralAtomicOutputValue;

public interface AtomicOutputAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract GeneralAtomicOutputValue getResult();
}
