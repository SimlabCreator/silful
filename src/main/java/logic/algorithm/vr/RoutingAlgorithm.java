package logic.algorithm.vr;

import data.entity.Routing;
import logic.algorithm.Algorithm;

public interface RoutingAlgorithm extends Algorithm{

	/**
	 * Provides results of algorithm
	 */
	public abstract Routing getResult();
}
