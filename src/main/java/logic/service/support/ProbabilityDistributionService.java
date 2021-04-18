package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import data.entity.AlternativeOffer;
import data.entity.DistributionParameterValue;
import data.entity.ProbabilityDistribution;
import data.entity.WeightEntity;
import data.entity.WeightingEntity;
import logic.utility.exceptions.DivideByZeroException;
import logic.utility.exceptions.ParameterUnknownException;
import logic.utility.exceptions.ProbabilitiesDoNotSumUpToOneException;

/**
 * Hosts functions to get random numbers and other values of different
 * probability distributions
 * 
 * @author M. Lang
 *
 */
public class ProbabilityDistributionService {

	static Random rd1 = new Random();
	final static double ERROR_TOLERANCE_FLOAT = 0.0000001;

	public static double getRandomNumberWithProperMinMax(ProbabilityDistribution pd) throws ParameterUnknownException {
		ArrayList<DistributionParameterValue> pVs = pd.getParameterValues();
		if (pd.equals("uniform")) {
			double min = 0;
			double max = 1;
			for (DistributionParameterValue dp : pVs) {
				if (dp.getParameterType().getName() == "min") {
					min = dp.getValue();
				} else if (dp.getParameterType().getName() == "max") {
					max = dp.getValue();
				}
			}
			return ProbabilityDistributionService.getUniformRandomNumber(min, max);
		}

		double u = ProbabilityDistributionService.getUniformRandomNumber(0, 1);

		double minProb = 0;
		double maxProb = 1;
		for (DistributionParameterValue dp : pVs) {
			if (dp.getParameterType().getName().equals("min")){
				minProb = ProbabilityDistributionService.getQuantileByCummulativeDistribution(pd, dp.getValue());
			} else if (dp.getParameterType().getName().equals("max")) {
				maxProb = ProbabilityDistributionService.getQuantileByCummulativeDistribution(pd, dp.getValue());
			}
		}

		double relevantProb = maxProb - minProb;
		double prob = relevantProb * u + minProb;
		return ProbabilityDistributionService.getXByCummulativeDistributionQuantile(pd, prob);
	}

	/**
	 * Provides a random double number from a normal distribution
	 *
	 * @param mean
	 *            Mean of the normal distribution
	 * @param std
	 *            Standard deviation of the normal distribution
	 * 
	 * @param min
	 *            Minimum value (optional)
	 * @param max
	 *            Maximum value (optional)
	 */
	public static double getNormalRandomNumber(double mean, double std, Double min, Double max) {

		double number = std * rd1.nextGaussian() + mean;

		if (min != null && number < min) {
			return getNormalRandomNumber(mean, std, min, max);
		}

		if (max != null && number > max) {
			return getNormalRandomNumber(mean, std, min, max);
		}
		return number;
	}

	/**
	 * Provides a random double number from a log normal distribution
	 *
	 * @param mu
	 *            Mu parameter of the log normal distribution
	 * @param sigma
	 *            Sigma parameter of the log normal distribution
	 * @param min
	 *            Minimum value (optional)
	 * @param max
	 *            Maximum value (optional)
	 */
	public static double getLogNormalRandomNumber(double mu, double sigma, Double min, Double max) {
		LogNormalDistribution logNormal = new LogNormalDistribution(mu, sigma);

		double number = logNormal.sample();

		if (min != null && number < min) {
			return getLogNormalRandomNumber(mu, sigma, min, max);
		}

		if (max != null && number > max) {
			return getLogNormalRandomNumber(mu, sigma, min, max);
		}
		return number;
	}

	/**
	 * Provides a random double number from a uniform distribution
	 *
	 * @param min
	 *            Minimum value of the possible value range
	 * @param max
	 *            Maximum value of the possible value range
	 */
	public static double getUniformRandomNumber(double min, double max) {
		return min + rd1.nextDouble() * (max - min);
	}

	/**
	 * Provides a random double number from an exponential distribution
	 *
	 * @param lambda
	 *            Lambda parameter of the exponential distribution. Must be
	 *            larger than zero
	 * @param min
	 *            Minimum value (optional)
	 * @param max
	 *            Maximum value (optional)
	 */
	public static double getExponentialRandomNumber(double lambda, Double min, Double max)
			throws DivideByZeroException {

		double mean;
		if (lambda != 0)
			mean = 1 / lambda;
		else
			throw new DivideByZeroException("Lambda of exponential distribution");

		ExponentialDistribution exp = new ExponentialDistribution(mean);

		double number = exp.sample();

		if (min != null && number < min) {
			return getExponentialRandomNumber(lambda, min, max);
		}

		if (max != null && number > max) {
			return getExponentialRandomNumber(lambda, min, max);
		}
		return number;
	}

	/**
	 * Get a random number from the Gumbel distribution
	 * 
	 * @param mu
	 *            Location parameter
	 * @param beta
	 *            scale parameter
	 * @return
	 */
	public static double getGumbelDistributedRandomNumber(double mu, double beta) {

		GumbelDistribution gum = new GumbelDistribution(mu, beta);

		return gum.sample();
	}

	/**
	 * Get expected value from the Gumbel distribution
	 * 
	 * @param mu
	 *            Location parameter
	 * @param beta
	 *            scale parameter
	 * @return
	 */
	public static double getExpectedValueGumbelDistribution(double mu, double beta) {

		GumbelDistribution gum = new GumbelDistribution(mu, beta);

		return gum.getNumericalMean();
	}
	
	public static double getBinominalCummulativeProbabilityValue(int rep, double prob, int x){
		BinomialDistribution bd = new BinomialDistribution(rep, prob);
		
		return bd.cumulativeProbability(x);
	}
	
	public static double getBinominalProbabilityValue(int rep, double prob, int x){
		BinomialDistribution bd = new BinomialDistribution(rep, prob);
		
		return bd.probability(x);
	}
	
	/**
	 * Provides a group index that was obtained randomly according to roulette
	 * wheel selection based on the provided probabilities
	 *
	 * @param groupProbabilities
	 *            An array of double, each element describes the probability of
	 *            choosing the group. For example test = [0.2, 0.3, 0.5]. The
	 *            sum of the elements must equal 1.
	 * @throws Exception
	 */
 	public static int getRandomGroupIndexByProbabilityArray(Double[] groupProbabilities) throws Exception {
		double random = rd1.nextDouble();
		double temp = 0;
		double test = 0;
		int value = 0;

		for (int i = 0; i < groupProbabilities.length; i++) {
			test += groupProbabilities[i];
		}
		if (Math.abs(test - 1) > ERROR_TOLERANCE_FLOAT) {
			throw new ProbabilitiesDoNotSumUpToOneException();
		}

		for (int i = 0; i < groupProbabilities.length; i++) {
			if (random <= temp + groupProbabilities[i]) {
				value = i;
				break;
			}

			temp += groupProbabilities[i];
		}
		return value;

	}

	/**
	 * Provides a group index that was obtained randomly according to roulette
	 * wheel selection based on the provided probabilities
	 *
	 * @param probabilities
	 *            A hashmap with alternative offers and probability per offer
	 */
	public static AlternativeOffer getRandomAlternativeOfferByProbabilityHashMap(
			HashMap<AlternativeOffer, Double> probabilities) throws Exception {
		double random = rd1.nextDouble();
		double temp = 0;
		AlternativeOffer offer = null;

		double test = 0;
		Iterator<AlternativeOffer> it = probabilities.keySet().iterator();
		while (it.hasNext()) {
			offer = it.next();
			Double prob = probabilities.get(offer);
			test += prob;
		}
		if (Math.abs(test - 1) > ERROR_TOLERANCE_FLOAT) {
			throw new ProbabilitiesDoNotSumUpToOneException();
		}

		Iterator<AlternativeOffer> it2 = probabilities.keySet().iterator();
		while (it2.hasNext()) {
			offer = it2.next();
			Double prob = probabilities.get(offer);
			if (random <= temp + prob)
				break;

			temp += prob;

		}
		return offer;

	}

	/**
	 * Provides an index of the weight that was determined randomly according to
	 * roulette wheel selection based on the weight values
	 *
	 * @param weighting
	 *            Weighting with the respective weights. The sum of the weight
	 *            values must equal 1.
	 * @return respective weight entity
	 */
	public static WeightEntity getRandomWeightByWeighting(WeightingEntity weighting)
			throws ProbabilitiesDoNotSumUpToOneException {
		double random = rd1.nextDouble();
		double temp = 0;
		int index = 0;

		double test = 0;
		for (int i = 0; i < weighting.getWeights().size(); i++) {
			test += weighting.getWeights().get(i).getWeight();
		}

		if (Math.abs(test - 1) > ERROR_TOLERANCE_FLOAT) {
			throw new ProbabilitiesDoNotSumUpToOneException();
		}

		for (int i = 0; i < weighting.getWeights().size(); i++) {
			if (random <= temp + weighting.getWeights().get(i).getWeight()) {
				index = i;
				break;
			}
			temp += weighting.getWeights().get(i).getWeight();
		}
		return weighting.getWeights().get(index);
	}

	/**
	 * Provides a random number for the respective probability distribution
	 * 
	 * @param pd
	 *            Probability distribution. Can be one of normal, log-normal,
	 *            exponential, constant, uniform
	 * @return
	 * @throws ParameterUnknownException
	 */
	public static Double getRandomNumberByProbabilityDistribution(ProbabilityDistribution pd)
			throws ParameterUnknownException {

		return ProbabilityDistributionService.getRandomNumberWithProperMinMax(pd);

	}

	/**
	 * Provides a random number for the respective probability distribution
	 * 
	 * @param pd
	 *            Probability distribution. Can be one of normal, log-normal,
	 *            exponential, constant, uniform
	 * @return
	 * @throws ParameterUnknownException
	 */
	public static Double getRandomNumberByProbabilityDistributionOld(ProbabilityDistribution pd)
			throws ParameterUnknownException {

		String pdT = pd.getProbabilityDistributionType().getName();

		ArrayList<DistributionParameterValue> pVs = pd.getParameterValues();

		Double number = 0.0;
		if (pdT.equals("normal")) {

			double mean = 0;
			double std = 1;
			Double min = null;
			Double max = null;

			for (int i = 0; i < pVs.size(); i++) {

				if ((pVs.get(i)).getParameterType().getName().equals("mean")) {
					mean = (pVs.get(i)).getValue();

				} else if ((pVs.get(i)).getParameterType().getName().equals("std")) {
					std = (pVs.get(i)).getValue();

				} else if ((pVs.get(i)).getParameterType().getName().equals("min")) {
					min = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
					max = (pVs.get(i)).getValue();
				}
			}
			number = ProbabilityDistributionService.getNormalRandomNumber(mean, std, min, max);
		} else if (pdT.equals("log-normal")) {

			double mu = 0;
			double sigma = 1;
			Double min = null;
			Double max = null;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("mu")) {
					mu = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("sigma")) {
					sigma = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("min")) {
					min = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
					max = (pVs.get(i)).getValue();
				}
			}
			number = ProbabilityDistributionService.getLogNormalRandomNumber(mu, sigma, min, max);
		} else if (pdT.equals("exponential")) {

			double lambda = 0;
			Double min = null;
			Double max = null;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("lambda")) {
					lambda = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("min")) {
					min = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
					max = (pVs.get(i)).getValue();
				}
			}
			try {
				number = ProbabilityDistributionService.getExponentialRandomNumber(lambda, min, max);
			} catch (DivideByZeroException e) {
				e.printStackTrace();
				System.exit(0);
			}
		} else if (pdT.equals("constant")) {

			number = pVs.get(0).getValue();

		} else if (pdT.equals("uniform")) {

			double min = 0;
			double max = 0;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("min")) {
					min = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
					max = (pVs.get(i)).getValue();
				}
			}

			number = ProbabilityDistributionService.getUniformRandomNumber(min, max);

		} else {
			throw new ParameterUnknownException("Probability distribution name");
		}

		return number;
	}

	/**
	 * Provides the mean for the respective probability distribution. Attention:
	 * Min and Max are ignored!
	 * 
	 * @param pd
	 *            Probability distribution. Can be one of normal, log-normal
	 * @return
	 * @throws ParameterUnknownException
	 */
	public static Double getMeanByProbabilityDistribution(ProbabilityDistribution pd) throws ParameterUnknownException {
		String pdT = pd.getProbabilityDistributionType().getName();

		ArrayList<DistributionParameterValue> pVs = pd.getParameterValues();

		Double number = 0.0;

		if (pdT.equals("normal")) {

			for (int i = 0; i < pVs.size(); i++) {

				if ((pVs.get(i)).getParameterType().getName().equals("mean")) {
					return (pVs.get(i)).getValue();
				}
			}
		} else if (pdT.equals("log-normal")) {

			double mu = 0;
			double sigma = 1;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("mu")) {
					mu = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("sigma")) {
					sigma = (pVs.get(i)).getValue();
				}
			}
			LogNormalDistribution logNormal = new LogNormalDistribution(mu, sigma);
			number = logNormal.getNumericalMean();
		} else if (pdT.equals("exponential")) {

			double lambda = 0;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("lambda")) {
					lambda = (pVs.get(i)).getValue();
					break;
				}
			}
			number = (1.0 / lambda);
		} else if (pdT.equals("constant")) {

			number = (pVs.get(0)).getValue();

		} else if (pdT.equals("uniform")) {

			double min = 0;
			double max = 0;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("min")) {
					min = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
					max = (pVs.get(i)).getValue();
				}
			}

			number = ((min + (max - min)) / 2.0);

		} else {

			throw new ParameterUnknownException("Probability distribution name");

		}

		return number;
	}

	/**
	 * Provides the respective quantile value of the cumulative probability
	 * distribution. Thus, uses the inverse of the probability distribution.
	 * 
	 * @param pd
	 *            Respective probability distribution
	 * @param quantile
	 *            Respective quantile value for which X is needed
	 * @return
	 * @throws ParameterUnknownException
	 */
	public static Double getXByCummulativeDistributionQuantile(ProbabilityDistribution pd, Double quantile)
			throws ParameterUnknownException {
		String pdT = pd.getProbabilityDistributionType().getName();

		ArrayList<DistributionParameterValue> pVs = pd.getParameterValues();
		double min=0;
		double max=1;
		for (int i = 0; i < pVs.size(); i++) {

			if ((pVs.get(i)).getParameterType().getName().equals("min")) {
				min = ProbabilityDistributionService.getQuantileByCummulativeDistribution(pd, pVs.get(i).getValue());

			} else if ((pVs.get(i)).getParameterType().getName().equals("max")) {
				max =ProbabilityDistributionService.getQuantileByCummulativeDistribution(pd, pVs.get(i).getValue());

			}
		}
		
		double finalQuantile=min+(max-min)*quantile;

		Double number = 0.0;
		if (pdT.equals("normal")) {

			double mean = 0;
			double std = 1;

			for (int i = 0; i < pVs.size(); i++) {

				if ((pVs.get(i)).getParameterType().getName().equals("mean")) {
					mean = (pVs.get(i)).getValue();

				} else if ((pVs.get(i)).getParameterType().getName().equals("std")) {
					std = (pVs.get(i)).getValue();

				}
			}

			NormalDistribution normal = new NormalDistribution(mean, std);
			number = normal.inverseCumulativeProbability(finalQuantile);
		} else if (pdT.equals("log-normal")) {

			double mu = 0;
			double sigma = 1;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("mu")) {
					mu = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("sigma")) {
					sigma = (pVs.get(i)).getValue();
				}
			}
			LogNormalDistribution dist = new LogNormalDistribution(mu, sigma);
			number = dist.inverseCumulativeProbability(finalQuantile);
		} else if (pdT.equals("exponential")) {

			double lambda = 0;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("lambda")) {
					lambda = (pVs.get(i)).getValue();
				}
			}
			double mean;
			if (lambda != 0) {
				mean = 1 / lambda;
				ExponentialDistribution exp = new ExponentialDistribution(mean);
				number = exp.inverseCumulativeProbability(finalQuantile);
			}

			number = null;

		} else if (pdT.equals("constant")) {

			number = (pVs.get(0)).getValue();

		} else {

			throw new ParameterUnknownException("Probability distribution name");

		}

		return number;
	}

	/**
	 * Provides the respective cummulative value of the cummulative probability
	 * distribution. Thus, the quantile.
	 * 
	 * @param pd
	 *            Respective probability distribution
	 * @param quantile
	 *            Respective X value for which quantile is needed
	 * @return
	 * @throws ParameterUnknownException
	 */
	public static Double getQuantileByCummulativeDistribution(ProbabilityDistribution pd, Double x)
			throws ParameterUnknownException {
		String pdT = pd.getProbabilityDistributionType().getName();

		ArrayList<DistributionParameterValue> pVs = pd.getParameterValues();

		Double number = 0.0;
		if (pdT.equals("normal")) {

			double mean = 0;
			double std = 1;

			for (int i = 0; i < pVs.size(); i++) {

				if ((pVs.get(i)).getParameterType().getName().equals("mean")) {
					mean = (pVs.get(i)).getValue();

				} else if ((pVs.get(i)).getParameterType().getName().equals("std")) {
					std = (pVs.get(i)).getValue();

				}
			}

			NormalDistribution normal = new NormalDistribution(mean, std);
			number = normal.cumulativeProbability(x);
		} else if (pdT.equals("log-normal")) {

			double mu = 0;
			double sigma = 1;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("mu")) {
					mu = (pVs.get(i)).getValue();
				} else if ((pVs.get(i)).getParameterType().getName().equals("sigma")) {
					sigma = (pVs.get(i)).getValue();
				}
			}
			LogNormalDistribution dist = new LogNormalDistribution(mu, sigma);
			number = dist.cumulativeProbability(x);
		} else if (pdT.equals("exponential")) {

			double lambda = 0;

			for (int i = 0; i < pVs.size(); i++) {
				if ((pVs.get(i)).getParameterType().getName().equals("lambda")) {
					lambda = (pVs.get(i)).getValue();
				}
			}
			double mean;
			if (lambda != 0) {
				mean = 1 / lambda;
				ExponentialDistribution exp = new ExponentialDistribution(mean);
				number = exp.cumulativeProbability(x);
			}

			number = null;

		} else if (pdT.equals("constant")) {

			number = (pVs.get(0)).getValue();

		} else {
			throw new ParameterUnknownException("Probability distribution name");
		}

		return number;
	}

}
