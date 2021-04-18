package data.service;

import java.util.ArrayList;

import data.entity.DistributionParameterValue;
import data.entity.Entity;
import data.entity.ProbabilityDistribution;

public abstract class ProbabilityDistributionDataService extends MetaDataService{

	/**
	 * Returns the parameters and their values for the respective concrete probability distribution
	 * For instance mean=0 and std.=1 for the standard norma distribution
	 * @param probabilityDistributionId Respective probability distribution
	 * @return Parameter values
	 */
	public abstract ArrayList<DistributionParameterValue> getParameterValuesByProbabilityDistributionId(int probabilityDistributionId);
	
	/**
	 * Returns all probability distributions with the respective probability distribution type
	 * @param probabilityDistributionTypeId Respective probability distribution type
	 * @return Probability Distributions
	 */
	public abstract ArrayList<Entity> getProbabilityDistributionsByProbabilityDistributionTypeId(int probabilityDistributionTypeId);
	
	/**
	 * Save the respective probability distribution including parameter values
	 * @param probabilityDistribution
	 * @return
	 */
	public abstract int persistProbabilityDistribution(ProbabilityDistribution probabilityDistribution);

}
