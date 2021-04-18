package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import logic.entity.AlternativeCapacity;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides functionality relating to acceptance step
 * 
 * @author M. Lang
 *
 */
public class AcceptanceService {


	/**
	 * Determines which alternatives are still available for the given delivery
	 * area
	 * 
	 * @param deliveryArea
	 *            Respective delivery area
	 * @param alternativeCapacities
	 *            Capacities per alternatives
	 * @return List of available
	 */
	public static ArrayList<Alternative> getAvailableAlternatives(DeliveryArea deliveryArea,
			ArrayList<AlternativeCapacity> alternativeCapacities) {

		ArrayList<Alternative> availableAlt = new ArrayList<Alternative>();

		for (int i = 0; i < alternativeCapacities.size(); i++) {
			if (alternativeCapacities.get(i).getCapacityNumber() > 0
					&& alternativeCapacities.get(i).getDeliveryAreaId() == deliveryArea.getId()) {
				availableAlt.add(alternativeCapacities.get(i).getAlternative());
			}
		}

		return availableAlt;
	}

	/**
	 * Determines which alternatives are still available. Capacities and
	 * dimensions have to have the same order
	 * 
	 * @param daCapacities
	 *            Respective capacities
	 * @param currentDimensions
	 *            Left over capacities (entries 1 to currentDimensions.length-1)
	 * @return List of available alternatives
	 */
	public static ArrayList<Alternative> getAvailableAlternatives(ArrayList<AlternativeCapacity> daCapacities,
			int[] currentDimensions) {

		ArrayList<Alternative> availableAlt = new ArrayList<Alternative>();
		//System.out.println(currentDimensions);
		for (int i = 1; i < currentDimensions.length; i++) {
			if (currentDimensions[i] > 0) {
				availableAlt.add(daCapacities.get(i - 1).getAlternative());
			}
			;
		}

		return availableAlt;
	}

	/**
	 * Reduces the left over capacity of the respective alternative in the
	 * delivery area
	 * 
	 * @param alternativeCapacities
	 *            Capacities of alternatives
	 * @param alternativeId
	 *            Respective alternative
	 * @param deliveryAreaId
	 *            Respective delivery area
	 */
	public static void reduceCapacityOfAlternative(ArrayList<AlternativeCapacity> alternativeCapacities,
			Integer alternativeId, Integer deliveryAreaId) {

		for (int i = 0; i < alternativeCapacities.size(); i++) {
			if (alternativeCapacities.get(i).getAlternativeId() == alternativeId
					&& alternativeCapacities.get(i).getDeliveryAreaId() == deliveryAreaId) {
				alternativeCapacities.get(i).setCapacityNumber(alternativeCapacities.get(i).getCapacityNumber() - 1);
				break;
			}
		}
	}

	/**
	 * Determines the maximum revenue value for a normalization of the revenue
	 * objective. Goes through all segments of the demand segment set and
	 * determines 95% quantile of the basket value distribution. Returns
	 * highest.
	 * 
	 * @param dsSet
	 *            Respective demand segment set.
	 * @return
	 * @throws ParameterUnknownException 
	 */
	public static Double determineMaximumRevenueValueForNormalisation(DemandSegmentSet dsSet) throws ParameterUnknownException {
		Double maximumValue = 0.0;
		Integer maximumSegmentId = 0;
		ArrayList<DemandSegment> segments = dsSet.getElements();

		for (DemandSegment segment : segments) {
			
			Double value = ProbabilityDistributionService
					.getXByCummulativeDistributionQuantile(segment.getBasketValueDistribution(), 0.95);
		
			if (value > maximumValue) {
				maximumSegmentId = segment.getId();
				maximumValue = value;
			}

		}
		System.out.println("0.95 value is " + maximumValue);
		System.out.println("Respective segment is " + maximumSegmentId);
		return maximumValue;

	}

	public static Object[] prepareLocalVisibilityObjectiveForWeightedValueCalculation(DeliveryAreaSet daSet,
			int arrivalProcessId, HashMap<DeliveryArea, Double> daWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings) throws ParameterUnknownException {

		HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = new HashMap<DeliveryArea, HashMap<Alternative, Double>>();
		ArrayList<DeliveryArea> das = daSet.getElements();
		for (DeliveryArea area : das) {
			// Determine arrival probability of delivery area, take mean
			// arrival probability if it changes over time
			double arrivalProbability = (ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId))
					* daWeights.get(area);

			// Go through segments and alternatives and determine equity
			// for the respective delivery area
			HashMap<Alternative, Double> reverseEquities = new HashMap<Alternative, Double>();
			DemandSegmentSet dsSet = (DemandSegmentSet) daSegmentWeightings.get(area).getSetEntity();
			ArrayList<DemandSegmentWeight> demandSegmentWeights = daSegmentWeightings.get(area).getWeights();
			ArrayList<Alternative> alternatives = dsSet.getAlternativeSet().getElements();

			for (DemandSegmentWeight seg : demandSegmentWeights) {
				DemandSegment segment = seg.getDemandSegment();
				Double weight = seg.getWeight();

				// Determine selection probabilities for the
				// alternatives of the
				// complete alternative set
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (int i = 0; i < alternatives.size(); i++) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alternatives.get(i));
					offer.setAlternativeId(offer.getAlternative().getId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel(dsSet.getDemandModelType().getName(),
						offeredAlternatives, segment);

				for (AlternativeOffer offer : probs.keySet()) {
					if(offer!=null){
					Alternative alternative = offer.getAlternative();
					if (reverseEquities.containsKey(alternative)) {
						Double updatedValue = reverseEquities.get(alternative)
								+ arrivalProbability * weight * probs.get(offer)*ProbabilityDistributionService
								.getMeanByProbabilityDistribution(segment.getBasketValueDistribution());
						//System.out.println("Distribution mean: "+ProbabilityDistributionService
						//			.getMeanByProbabilityDistribution(segment.getBasketValueDistribution()));
						reverseEquities.put(alternative, updatedValue);
					} else {
						reverseEquities.put(alternative, arrivalProbability * weight * probs.get(offer)*ProbabilityDistributionService
								.getMeanByProbabilityDistribution(segment.getBasketValueDistribution()));
					//	System.out.println("Distribution mean: "+ProbabilityDistributionService
						//			.getMeanByProbabilityDistribution(segment.getBasketValueDistribution()));
					}
					}
				}

			}

			localVisibilityValues.put(area, reverseEquities);

		}

		// Determine maximum value

		Double maximumLocalVisibility = 0.0;
		for (DeliveryArea da : localVisibilityValues.keySet()) {
			for (Alternative alt : localVisibilityValues.get(da).keySet()) {
				if (localVisibilityValues.get(da).get(alt) > maximumLocalVisibility) {
					maximumLocalVisibility = localVisibilityValues.get(da).get(alt);
				}
			}
		}

		Object[] results = new Object[] { localVisibilityValues, maximumLocalVisibility };
System.out.println("Maximum local visibility "+maximumLocalVisibility);
		return results;
	}

}
