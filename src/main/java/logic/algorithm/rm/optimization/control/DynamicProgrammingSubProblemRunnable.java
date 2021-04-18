package logic.algorithm.rm.optimization.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.ArrivalProcess;
import data.entity.DeliveryArea;
import data.entity.DemandSegment;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.utility.DataServiceProvider;
import logic.entity.AlternativeCapacity;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.MultiDimensionalArrayProducer;
import logic.utility.SubsetProducer;
import logic.utility.exceptions.ParameterUnknownException;

public class DynamicProgrammingSubProblemRunnable implements Runnable {

	private DeliveryArea currentArea;
	private int[] dimensions;
	private Object[] tree;
	private DemandSegmentWeighting weighting;
	//private int bookingHorizonLength;
	private double deliveryAreaWeight;
	private int arrivalProcessId;
	private ArrayList<AlternativeCapacity> capacities;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;

	public DynamicProgrammingSubProblemRunnable(DeliveryArea currentArea, int[] dimensions, Object[] tree, DemandSegmentWeighting weighting,
												int bookingHorizonLength, double deliveryAreaWeight, int arrivalProcessId,
												ArrayList<AlternativeCapacity> capacities, HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue) {
		this.currentArea = currentArea;
		this.dimensions = dimensions;
		this.tree = tree;
		this.weighting = weighting;
		//this.bookingHorizonLength = bookingHorizonLength;
		this.deliveryAreaWeight = deliveryAreaWeight;
		this.arrivalProcessId = arrivalProcessId;
		this.capacities = capacities;
		this.objectiveSpecificValues=objectiveSpecificValues;
		this.maximumRevenueValue=maximumRevenueValue;
	}

	public void run() {
		Double value;
		try {
			value = this.getValue(dimensions, this.weighting, currentArea);
			System.out.println("Overall values for area:" + currentArea.getId() + " is 1: " + value);
			
		} catch (ParameterUnknownException e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

	/**
	 * Provides the value of the subproblem defined by t and left-over
	 * capacities which are all saved in a dimensions-array
	 * 
	 * @param dimensions
	 *            Respective t and capacity values
	 * @param weighting
	 *            Weighting of respective customer segments
	 * @param currentDa
	 *            Delivery area
	 * @return
	 * @throws ParameterUnknownException 
	 */
	private Double getValue(int[] dimensions, DemandSegmentWeighting weighting, DeliveryArea currentDa) throws ParameterUnknownException {

		Double value =  0.0;

		//if (dimensions[0] == bookingHorizonLength - 3)
			//System.out.println("reached t=T-3 for delivery area " + currentDa.getId());
		// Constraints
		/// Booking horizon over?
		if (dimensions[0] == 0) {
			MultiDimensionalArrayProducer.writeToDoubleArray(value, this.tree, dimensions);

			return value;
		}
		/// No capacities left?
		int sumOfCapacities = 0;
		for (int capID = 1; capID < dimensions.length; capID++) {
			sumOfCapacities += dimensions[capID];
		}
		if (sumOfCapacities == 0) {
			MultiDimensionalArrayProducer.writeToDoubleArray(value, this.tree, dimensions);
			return value;
		}

		// Already calculated and saved in the buffer?
		Double subValue = MultiDimensionalArrayProducer.readDoubleArray(this.tree, dimensions);

		if (subValue != null) {
			return subValue;
		}

		// Determine

		double noArrivalValue;// No request in t
		double arrivalValue;// Request in t

		ArrivalProcess arrivalProcess = (ArrivalProcess) DataServiceProvider.getArrivalProcessDataServiceImplInstance().getById(arrivalProcessId);
		double arrivalProbability = (ArrivalProcessService.getArrivalProbability(
				dimensions[0],arrivalProcess)) * this.deliveryAreaWeight;


		// If no request arrives, the value for t-1 is needed
		int[] dimensionsReducedT = dimensions.clone();
		dimensionsReducedT[0] -= 1;
		noArrivalValue = (1 - arrivalProbability) * this.getValue(dimensionsReducedT, weighting, currentDa);

		// If a request arrives, the value of all segments needs to be combined
		// in a weighted sum
		arrivalValue =  0.0;
		for (int segmentID = 0; segmentID < weighting.getWeights().size(); segmentID++) {

			arrivalValue += arrivalProbability * weighting.getWeights().get(segmentID).getWeight()
					* this.getValueForSegment(segmentID, dimensions, weighting, currentDa);

		}

		// The value of the respective t and capacities is the sum of no arrival
		// and arrival value
		value = noArrivalValue + arrivalValue;

		MultiDimensionalArrayProducer.writeToDoubleArray(value, this.tree, dimensions);
		return value;
	}

	private Double getValueForSegment(int segmentId, int[] dimensions, DemandSegmentWeighting weighting,
			DeliveryArea currentDa) throws ParameterUnknownException {

		// No purchase value
		int[] dimensionsReducedT = dimensions.clone();
		dimensionsReducedT[0] -= 1;
		Double valueNoPurchase = this.getValue(dimensionsReducedT, weighting, currentDa);

		// Determine available alternatives and their future value with reduced
		// capacity and t
		Set<Integer> available = new HashSet<Integer>();
		HashMap<Alternative, Double> leftOverValues = new HashMap<Alternative, Double>();
		for (int capID = 1; capID < dimensionsReducedT.length; capID++) {
			if (dimensionsReducedT[capID] > 0) {// Capacity left
				int[] dimensionsReduced = dimensionsReducedT.clone();
				dimensionsReduced[capID] -= 1;

				Double leftOverValue = this.getValue(dimensionsReduced, weighting, currentDa);

				leftOverValues.put(this.capacities.get(capID - 1).getAlternative(), leftOverValue);
				available.add(capID - 1); // From the capacity set, it is one
											// before
			}
		}

		// The current maximum value is to not offer anything and directly go to
		// the next t (no-purchase prob = 1)
		Double maxValue = valueNoPurchase;

		for (Set<Integer> s : SubsetProducer.powerSet(available)) {
			if (s.size() > 0) { // Only look at non-empty subsets

				// Determine selection probabilities for the alternatives of the
				// respective subset
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (Integer index : s) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(this.capacities.get(index).getAlternative());
					offer.setAlternativeId(offer.getAlternative().getId());
					offeredAlternatives.add(offer);
				}

				DemandSegment segment = weighting.getWeights().get(segmentId)
						.getDemandSegment();
				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel("MNL_constant",
						offeredAlternatives, segment);

				// Determine purchase-value
				Double currentValue =  0.0;
				double noPurchaseProb =  1.0;
				Iterator<AlternativeOffer> it = probs.keySet().iterator();
				while (it.hasNext()) {
					AlternativeOffer currentAlt = it.next();
					if (currentAlt != null) {

						Double prob = probs.get(currentAlt);

						Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
								segment.getBasketValueDistribution()) / this.maximumRevenueValue;

						// If there are additional objectives, revenue needs to
						// be weighted and the other objective values need to be
						// added
						Double weightedValue = 0.0;
						Double weightRevenue = 1.0;
						if (this.objectiveSpecificValues.keySet().size() > 0) {

							for (Entity obj : this.objectiveSpecificValues.keySet()) {
								ObjectiveWeight ow = (ObjectiveWeight) obj;
								
								if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
									weightRevenue -= ow.getValue();
									weightedValue += ow.getValue() * segment.getSocialImpactFactor();
									// if(segment.getId()==16)
									// System.out.print(" weight social impact:
									// "+ow.getValue());
								} else if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {
//									// The specific values are an object array
//									Object[] objectiveValues = (Object[]) this.objectiveSpecificValues.get(obj);
//									// The first entry are the local visibility
//									// values
//									@SuppressWarnings("unchecked")
//									HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = (HashMap<DeliveryArea, HashMap<Alternative, Double>>) objectiveValues[0];
//									// The second entry is the maximum value
//									Double maximumLocalVisibility = objectiveValues[1];
//									weightedValue += ow.getValue() * (maximumLocalVisibility
//											- localVisibilityValues.get(currentDa).get(currentAlt.getAlternative()))
//											/ maximumLocalVisibility;
								}
							}

						}
						weightedValue += weightRevenue * meanRevenue;

						Double probValue = prob * weightedValue; // Value now

						probValue += leftOverValues.get(currentAlt.getAlternative()) * prob; // Future
																								// value

						currentValue += probValue;
						noPurchaseProb -= prob;
					}

				}
				// Determine no-purchase value
				currentValue += valueNoPurchase * noPurchaseProb;

				// Update maximum value
				if (currentValue > maxValue) {
					maxValue = currentValue;

				}
			}

		}

		return maxValue;
	}

}