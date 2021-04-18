package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides functionality relating to customer alternative demand
 * 
 * @author M. Lang
 *
 */
public class CustomerDemandService {

	public static Double determineExpectedUtilityPerAlternativeAndPrice(DemandSegment demandSegment,
			Alternative alternative, double price, double scalingParameter) {

		if (demandSegment.getSet().getDemandModelType().getName().equals("MNL_constant")
				|| demandSegment.getSet().getDemandModelType().getName().equals("MNL_price")) {

			Double utility = demandSegment.getSegmentSpecificCoefficient();

			for (ConsiderationSetAlternative alt : demandSegment.getConsiderationSet()) {
				if (alt.getAlternativeId() == alternative.getId()) {
					utility += alt.getCoefficient();
					break;
				}
			}

			utility += ProbabilityDistributionService.getExpectedValueGumbelDistribution(0, 1.0 * scalingParameter);

			if (demandSegment.getSet().getDemandModelType().getName().equals("MNL_price")) {
				utility += demandSegment.getVariableCoefficientByName("delivery_fee").getCoefficientValue() * price;
			}

			return utility;
		} else {
			// TODO
			return null;
		}

	}

	/**
	 * Simulates customer decision for the respective time windows
	 * 
	 * @param order
	 * @param feasibleTimeWindows
	 * @param alternativesForTimeWindows
	 * @param usePreferencesSampled
	 */
	public static void simulateCustomerDecision(Order order, Set<Integer> feasibleTimeWindows,
			AlternativeSet alternativeSet, HashMap<Integer, Alternative> alternativesForTimeWindows,
			boolean usePreferencesSampled) {

		if (feasibleTimeWindows.size() > 0) {

			// Offer feasible time windows
			ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
			for (Integer twId : feasibleTimeWindows) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(alternativesForTimeWindows.get(twId));
				offer.setAlternativeId(alternativesForTimeWindows.get(twId).getId());
				offeredAlternatives.add(offer);
			}

			// Sample selection from customer
			AlternativeOffer selectedAlt;
			if (usePreferencesSampled) {
				selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(offeredAlternatives,
						order, alternativeSet.getNoPurchaseAlternative());

			} else {
				selectedAlt = CustomerDemandService.sampleCustomerDemand(offeredAlternatives, order);
			}

			if (selectedAlt != null) {
				if (!selectedAlt.getAlternative().getNoPurchaseAlternative()) {
					order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
					order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
					order.setAccepted(true);

				} else {
					order.setReasonRejection("no_selection");
				}

			} else {

				order.setReasonRejection("no_selection");

			}

		} else {

			order.setReasonRejection("no_offer_feasible");
		}
	}

	/**
	 * Samples final preference list for an incoming request For instance, for
	 * MNL model: Samples utility per alternative No purchase probability should
	 * be in consideration set as to sample the utility
	 * 
	 * @param orderRequest
	 *            Request for which the final preferences should be sampled
	 * @param scalingParameter
	 *            Scaling parameter for the utility distribution
	 * @return
	 */
	public static HashMap<Integer, Double> sampleAlternativePreferences(OrderRequest orderRequest,
			double scalingParameter) {
		HashMap<Integer, Double> alternativePreferences = new HashMap<Integer, Double>();

		DemandSegment demandSegment = orderRequest.getCustomer().getOriginalDemandSegment();

		if (demandSegment.getSet().getDemandModelType().getName().equals("MNL_constant")
				|| demandSegment.getSet().getDemandModelType().getName().equals("MNL_price")) {

			for (ConsiderationSetAlternative alt : demandSegment.getConsiderationSet()) {

				Integer alternativeId = alt.getAlternativeId();
				Double constantUtility = alt.getCoefficient();
				Double overallUtility = demandSegment.getSegmentSpecificCoefficient() + constantUtility
						+ ProbabilityDistributionService.getGumbelDistributedRandomNumber(0, 1.0 * scalingParameter);
				alternativePreferences.put(alternativeId, overallUtility);
			}

		} else {
			// TODO
		}

		return alternativePreferences;
	}

	/**
	 * Returns the alternative that is offered and provides the highest sampled
	 * utility. If no purchase alternative, than return null.
	 * 
	 * @param offeredAlternatives
	 *            Offered alternatives. Can be null for independent demand.
	 * @param order
	 *            Respective order in which the selected alternative is saved
	 * @param noPurchaseAlternative
	 *            No purchase alternative if sampled. otherwise null.
	 * @return the alternativeoffer that was selected. Only relevant for
	 *         dependent demand, otherwise null.
	 */
	public static AlternativeOffer selectCustomerDemandBasedOnSampledPreferences(
			ArrayList<AlternativeOffer> offeredAlternatives, Order order, Alternative noPurchaseAlternative) {
		AlternativeOffer selectedOffer = new AlternativeOffer();

		double highestUtility = -1.0 * Double.MAX_VALUE;
		for (AlternativeOffer ao : offeredAlternatives) {
			// Does customer consider this alternative?
			if (order.getOrderRequest().getAlternativePreferences().containsKey(ao.getAlternativeId())) {
				if (order.getOrderRequest().getAlternativePreferences().get(ao.getAlternativeId()) > highestUtility) {
					selectedOffer = ao;
					highestUtility = order.getOrderRequest().getAlternativePreferences().get(ao.getAlternativeId());
				}
			}
		}

		// If no purchase alternative has more utility, no alternative is
		// selected (return null)
		if (noPurchaseAlternative == null && highestUtility < 0) {
			return null;
		}

		if (noPurchaseAlternative != null) {
			if ((!order.getOrderRequest().getAlternativePreferences().containsKey(noPurchaseAlternative.getId())
					&& highestUtility < 0)
					|| order.getOrderRequest().getAlternativePreferences()
							.get(noPurchaseAlternative.getId()) > highestUtility) {
				return null;
			}
		}

		// Otherwise, the selected offer is returned
		order.setSelectedAlternative(selectedOffer.getAlternative());
		order.setSelectedAlternativeId(selectedOffer.getAlternativeId());
		return selectedOffer;
	}

	/**
	 * Samples the alternative request of the respective order
	 * 
	 * @param offeredAlternatives
	 *            Offered alternatives. Can be null for independent demand.
	 * @param order
	 *            Respective order in which the selected alternative is saved
	 * @return the alternativeoffer that was selected. Only relevant for
	 *         dependent demand, otherwise null.
	 */
	public static AlternativeOffer sampleCustomerDemand(ArrayList<AlternativeOffer> offeredAlternatives, Order order) {

		AlternativeOffer selectedOffer = new AlternativeOffer();
		// If the demand is independent, we just sample from the customer demand
		// distribution
		// Afterwards, we can check if the selected alternative is available and
		// thus, if the request actually becomes an order
		DemandSegment demandSegment = order.getOrderRequest().getCustomer().getOriginalDemandSegment();
		if (demandSegment.getSet().getDemandModelType().getIndepdentent()) {

			ArrayList<ConsiderationSetAlternative> considerationSetAlternatives = demandSegment.getConsiderationSet();
			Double[] alternativeWeights = new Double[considerationSetAlternatives.size()];
			for (int i = 0; i < considerationSetAlternatives.size(); i++) {
				alternativeWeights[i] = considerationSetAlternatives.get(i).getWeight();
			}

			Integer selectedAlternative = 0;
			try {
				selectedAlternative = ProbabilityDistributionService
						.getRandomGroupIndexByProbabilityArray(alternativeWeights);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			order.setSelectedAlternative(considerationSetAlternatives.get(selectedAlternative).getAlternative());
			order.setSelectedAlternativeId(
					considerationSetAlternatives.get(selectedAlternative).getAlternative().getId());

		} else {

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandSegment.getSet().getDemandModelType().getName(), offeredAlternatives, demandSegment);

			try {
				selectedOffer = ProbabilityDistributionService
						.getRandomAlternativeOfferByProbabilityHashMap(probabilities);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Set selected alternative if not no-purchase option was selected
			if (selectedOffer != null) {
				order.setSelectedAlternative(selectedOffer.getAlternative());
				order.setSelectedAlternativeId(selectedOffer.getAlternativeId());
			}

		}

		return selectedOffer;
	}

	/**
	 * Builds the alternative demand probabilities for a demand model
	 * 
	 * @param model
	 *            Respective model name
	 * @param offeredAlternatives
	 *            List of alternatives that are offered
	 * @param demandSegment
	 *            Respective demandSegment with consideration set
	 * @return Probabilities per alternativeOffers
	 */
	public static HashMap<AlternativeOffer, Double> getProbabilitiesForModel(String model,
			ArrayList<AlternativeOffer> offeredAlternatives, DemandSegment demandSegment) {
		HashMap<AlternativeOffer, Double> probabilities = new HashMap<AlternativeOffer, Double>();
		if (model.equals("MNL_constant") || model.equals("MNL_price")) {
			probabilities = CustomerDemandService.getProbabilitiesForMNLModel(offeredAlternatives, demandSegment,
					model);
		} else {
			// TODO
		}

		return probabilities;
	}

	public static HashMap<AlternativeOffer, Double> getProbabilitiesForModel(
			ArrayList<AlternativeOffer> offeredAlternatives, DemandSegment demandSegment) {

		return CustomerDemandService.getProbabilitiesForModel(demandSegment.getSet().getDemandModelType().getName(),
				offeredAlternatives, demandSegment);
	}

	/**
	 * Builds the alternative demand probabilities for a MNL model
	 * 
	 * @param offeredAlternatives
	 *            List of alternatives that are offered (without no purchase
	 *            option)
	 * @param demandSegment
	 *            Respective demandSegment with consideration set
	 * @return
	 */
	private static HashMap<AlternativeOffer, Double> getProbabilitiesForMNLModel(
			ArrayList<AlternativeOffer> offeredAlternatives, DemandSegment demandSegment, String model) {

		HashMap<AlternativeOffer, Double> probabilities = new HashMap<AlternativeOffer, Double>();

		// Get utilities of the alternatives
		// Sum e^u to get divisor, e^0=1 represents the no purchase
		// alternative
		double divisor = 0.0;
		Double[] numerators = new Double[offeredAlternatives.size()];

		ArrayList<ConsiderationSetAlternative> considerationSetAlternatives = demandSegment.getConsiderationSet();

		ConsiderationSetAlternative noPurchaseConsidered = null;
		HashMap<Integer, ConsiderationSetAlternative> csas = new HashMap<Integer, ConsiderationSetAlternative>();
		for (int caID = 0; caID < considerationSetAlternatives.size(); caID++) {
			if (noPurchaseConsidered == null
					&& considerationSetAlternatives.get(caID).getAlternative().getNoPurchaseAlternative())
				noPurchaseConsidered = considerationSetAlternatives.get(caID);
			csas.put(considerationSetAlternatives.get(caID).getAlternativeId(), considerationSetAlternatives.get(caID));

		}
		// Only consider the alternatives that are offered
		AlternativeOffer noPurchaseOffer = null;
		for (int oaID = 0; oaID < offeredAlternatives.size(); oaID++) {

			if (offeredAlternatives.get(oaID).getAlternative().getNoPurchaseAlternative())
				noPurchaseOffer = offeredAlternatives.get(oaID);

			boolean considered = false;
			if (csas.containsKey(offeredAlternatives.get(oaID).getAlternativeId())) {

				double utility = csas.get(offeredAlternatives.get(oaID).getAlternativeId()).getCoefficient()
						+ demandSegment.getSegmentSpecificCoefficient();
				if (model.equals("MNL_price"))
					utility += demandSegment.getVariableCoefficientByName("delivery_fee").getCoefficientValue()
							* offeredAlternatives.get(oaID).getIncentive();
				divisor += Math.exp(utility);
				numerators[oaID] = Math.exp(utility);

				considered = true;

			}

			if (!considered && !offeredAlternatives.get(oaID).getAlternative().getNoPurchaseAlternative()) {

				numerators[oaID] = 0.0;
			}
		}

		// No purchase option

		// The no purchase option is always offered, but sometimes not
		// explicitely in the offer set
		// If a segment does not explicitely consider it, we use a utility of 0
		// as default value
		if (noPurchaseConsidered == null && noPurchaseOffer == null){
			divisor += 1.0;
			probabilities.put(null, (1.0 / divisor));
		} else if (noPurchaseOffer == null) {
			double utility = noPurchaseConsidered.getCoefficient() + demandSegment.getSegmentSpecificCoefficient();
			divisor += Math.exp(utility);
			probabilities.put(null, Math.exp(utility) / divisor);
		}else if(noPurchaseConsidered==null){
			divisor += 1.0;
			probabilities.put(noPurchaseOffer, 1.0 / divisor);
		}

		// Calculate weights and add to map

		for (int oaID = 0; oaID < offeredAlternatives.size(); oaID++) {
			if (offeredAlternatives.get(oaID).getAlternative().getNoPurchaseAlternative() && noPurchaseConsidered==null) {
				//Already added
			}else{
				probabilities.put(offeredAlternatives.get(oaID), numerators[oaID] / divisor);
			}

		}

		return probabilities;

	}

	public static HashMap<Integer, HashMap<Integer, Double>> determineMaximumProbabilityPerDemandSegmentAndTimeWindow(
			DemandSegmentWeighting daSegmentWeighting, TimeWindowSet timeWindowSet) {

		HashMap<Integer, HashMap<Integer, Double>> probsPerDsAndTw = new HashMap<Integer, HashMap<Integer, Double>>();
		for (DemandSegmentWeight dsw : daSegmentWeighting.getWeights()) {
			probsPerDsAndTw.put(dsw.getElementId(), new HashMap<Integer, Double>());
			for (TimeWindow tw : timeWindowSet.getElements()) {

				ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();

				// Offer all alternatives that include the respective time
				// window
				for (ConsiderationSetAlternative csa : dsw.getDemandSegment().getConsiderationSet()) {
					for (TimeWindow twa : csa.getAlternative().getTimeWindows()) {
						if (twa.getId() == tw.getId()) {
							AlternativeOffer offer = new AlternativeOffer();
							offer.setAlternative(csa.getAlternative());
							offer.setAlternativeId(csa.getAlternativeId());
							offers.add(offer);
						}
					}
				}

				if (offers.size() > 0) {
					HashMap<AlternativeOffer, Double> result = CustomerDemandService.getProbabilitiesForModel(offers,
							dsw.getDemandSegment());
					double twMaxProb = 0.0;
					for (AlternativeOffer of : offers) {
						twMaxProb += result.get(of);
					}
					probsPerDsAndTw.get(dsw.getElementId()).put(tw.getId(), twMaxProb);

				}

			}

		}
		return probsPerDsAndTw;
	}

	public static HashMap<Integer, HashMap<Integer, Double>> determineMinimumProbabilityPerDemandSegmentAndTimeWindow(
			DemandSegmentWeighting daSegmentWeighting, TimeWindowSet timeWindowSet) {
		// Offer all alternatives
		ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
		DemandSegmentSet set = (DemandSegmentSet) daSegmentWeighting.getSetEntity();
		for (Alternative alt : set.getAlternativeSet().getElements()) {
			AlternativeOffer offer = new AlternativeOffer();
			offer.setAlternative(alt);
			offer.setAlternativeId(alt.getId());
			offers.add(offer);
		}

		// Determine probs per segment
		HashMap<DemandSegment, HashMap<AlternativeOffer, Double>> probsPerSegment = new HashMap<DemandSegment, HashMap<AlternativeOffer, Double>>();
		for (DemandSegmentWeight dsw : daSegmentWeighting.getWeights()) {
			probsPerSegment.put(dsw.getDemandSegment(),
					CustomerDemandService.getProbabilitiesForModel(offers, dsw.getDemandSegment()));
		}

		// Go through time windows and determine multiplier per time window

		HashMap<Integer, HashMap<Integer, Double>> probsPerSegmentTw = new HashMap<Integer, HashMap<Integer, Double>>();
		for (DemandSegmentWeight dsw : daSegmentWeighting.getWeights()) {
			probsPerSegmentTw.put(dsw.getElementId(), new HashMap<Integer, Double>());
			for (TimeWindow tw : timeWindowSet.getElements()) {

				double twMinProb = 0.0;
				for (AlternativeOffer of : probsPerSegment.get(dsw.getDemandSegment()).keySet()) {
					if (of.getAlternative().getNoPurchaseAlternative()) {

					} else {
						for (TimeWindow two : of.getAlternative().getTimeWindows()) {
							if (two.getId() == tw.getId()) {
								twMinProb += probsPerSegment.get(dsw.getDemandSegment()).get(of);
							}
						}
					}
				}

				probsPerSegmentTw.get(dsw.getElementId()).put(tw.getId(), twMinProb);
			}

		}
		return probsPerSegmentTw;
	}

	/**
	 * Determines the probability for selecting a time window in a delivery area
	 * in case no other time windows are offered Expected value over segments
	 * 
	 * @param daWeights
	 * @param daSegmentWeightings
	 * @return
	 */
	public static HashMap<DeliveryArea, HashMap<TimeWindow, Double>> determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(
			HashMap<DeliveryArea, Double> daWeights, HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings,
			TimeWindowSet timeWindowSet) {
		HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();

		// Determine per delivery area
		for (DeliveryArea area : daWeights.keySet()) {
			maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow.put(area, new HashMap<TimeWindow, Double>());

			// Determine per time window (independently)
			for (TimeWindow tw : timeWindowSet.getElements()) {
				double prob = 0.0;
				for (DemandSegmentWeight dsw : daSegmentWeightings.get(area).getWeights()) {
					double twMaxProb = 0.0;

					ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();

					// Offer all alternatives that include the respective time
					// window
					for (ConsiderationSetAlternative csa : dsw.getDemandSegment().getConsiderationSet()) {
						for (TimeWindow twa : csa.getAlternative().getTimeWindows()) {
							if (twa.getId() == tw.getId()) {
								AlternativeOffer offer = new AlternativeOffer();
								offer.setAlternative(csa.getAlternative());
								offer.setAlternativeId(csa.getAlternativeId());
								offers.add(offer);
							}
						}
					}

					if (offers.size() > 0) {
						HashMap<AlternativeOffer, Double> result = CustomerDemandService
								.getProbabilitiesForModel(offers, dsw.getDemandSegment());
						for (AlternativeOffer of : offers) {
							twMaxProb += result.get(of);
						}

					}

					prob += dsw.getWeight() * twMaxProb;
				}

				maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(area).put(tw, prob);
			}

		}

		return maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow;
	}

	/**
	 * Determines the probability for selecting a time window in a delivery area
	 * in case all other time windows are offered Expected value over segments.
	 * No purchase alternative is defined as null.
	 * 
	 * @param daWeights
	 * @param daSegmentWeightings
	 * @return
	 */
	public static HashMap<DeliveryArea, HashMap<TimeWindow, Double>> determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(
			HashMap<DeliveryArea, Double> daWeights, HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings,
			TimeWindowSet timeWindowSet) {
		HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();

		// Determine for all areas
		for (DeliveryArea area : daWeights.keySet()) {
			minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.put(area, new HashMap<TimeWindow, Double>());

			// Offer all alternatives
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			DemandSegmentSet set = (DemandSegmentSet) daSegmentWeightings.get(area).getSetEntity();
			for (Alternative alt : set.getAlternativeSet().getElements()) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(alt);
				offer.setAlternativeId(alt.getId());
				offers.add(offer);
			}

			// Determine probs per segment
			HashMap<DemandSegment, HashMap<AlternativeOffer, Double>> probsPerSegment = new HashMap<DemandSegment, HashMap<AlternativeOffer, Double>>();
			for (DemandSegmentWeight dsw : daSegmentWeightings.get(area).getWeights()) {
				probsPerSegment.put(dsw.getDemandSegment(),
						CustomerDemandService.getProbabilitiesForModel(offers, dsw.getDemandSegment()));
			}

			// Go through time windows and determine multiplier per time window
			AlternativeOffer noPurchaseOffer = null;
			for (TimeWindow tw : timeWindowSet.getElements()) {

				double prob = 0.0;

				for (DemandSegmentWeight dsw : daSegmentWeightings.get(area).getWeights()) {
					double twMinProb = 0.0;
					for (AlternativeOffer of : probsPerSegment.get(dsw.getDemandSegment()).keySet()) {
						if (of.getAlternative().getNoPurchaseAlternative()) {
							noPurchaseOffer = of;
						} else {
							for (TimeWindow two : of.getAlternative().getTimeWindows()) {
								if (two.getId() == tw.getId()) {
									twMinProb += probsPerSegment.get(dsw.getDemandSegment()).get(of);
								}
							}
						}
					}

					prob += dsw.getWeight() * twMinProb;
				}

				minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(area).put(tw, prob);
			}

			// No purchase
			double noPurchaseProb = 0.0;
			for (DemandSegmentWeight dsw : daSegmentWeightings.get(area).getWeights()) {
				noPurchaseProb += probsPerSegment.get(dsw.getDemandSegment()).get(noPurchaseOffer) * dsw.getWeight();

			}
			minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(area).put(null, noPurchaseProb);

		}

		for (DeliveryArea area : minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.keySet()) {
			double sum = 0.0;
			for (TimeWindow tw : minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(area).keySet()) {
				sum += minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(area).get(tw);
			}
			if (sum < 0.999999) {
				System.out.println("Sum is not right");
			}
		}

		return minimumExpectedMultiplierPerDeliveryAreaAndTimeWindow;
	}

	public static double calculateOrderValue(OrderRequest request, double maximumRevenueValue,
			HashMap<Entity, Object> objectiveSpecificValues) {
		// Calculate immediate reward
		Double revenue = request.getBasketValue() / maximumRevenueValue;

		// If there are additional objectives, revenue needs to
		// be weighted and the other objective values need to be
		// added
		Double weightedValue = 0.0;
		Double weightRevenue = 1.0;
		if (objectiveSpecificValues.keySet().size() > 0) {

			for (Entity obj : objectiveSpecificValues.keySet()) {
				ObjectiveWeight ow = (ObjectiveWeight) obj;

				if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
					weightRevenue -= ow.getValue();
					weightedValue += ow.getValue()
							* request.getCustomer().getOriginalDemandSegment().getSocialImpactFactor();

				}
			}

		}
		weightedValue += weightRevenue * revenue;
		return weightedValue;
	}

	public static double calculateExpectedValue(double expectedRevenue, double maximumRevenueValue,
			HashMap<Entity, Object> objectiveSpecificValues, DemandSegment demandSegment) {
		// Calculate immediate reward
		Double revenue = expectedRevenue / maximumRevenueValue;

		// If there are additional objectives, revenue needs to
		// be weighted and the other objective values need to be
		// added
		Double weightedValue = 0.0;
		Double weightRevenue = 1.0;
		if (objectiveSpecificValues.keySet().size() > 0) {

			for (Entity obj : objectiveSpecificValues.keySet()) {
				ObjectiveWeight ow = (ObjectiveWeight) obj;

				if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
					weightRevenue -= ow.getValue();
					weightedValue += ow.getValue() * demandSegment.getSocialImpactFactor();

				}
			}

		}
		weightedValue += weightRevenue * revenue;
		return weightedValue;
	}

	public static double calculateExpectedValue(double maximumRevenueValue,
			HashMap<Entity, Object> objectiveSpecificValues, DemandSegment demandSegment) {
		// Calculate immediate reward

		Double expectedRevenue = 0.0;
		try {
			expectedRevenue = ProbabilityDistributionService
					.getMeanByProbabilityDistribution(demandSegment.getBasketValueDistribution());
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CustomerDemandService.calculateExpectedValue(expectedRevenue, maximumRevenueValue,
				objectiveSpecificValues, demandSegment);
	}

	public static double calculateMedianValue(double maximumRevenueValue,
			HashMap<Entity, Object> objectiveSpecificValues, DemandSegment demandSegment) {
		// Calculate immediate reward

		Double expectedRevenue = 0.0;
		try {
			expectedRevenue = ProbabilityDistributionService
					.getXByCummulativeDistributionQuantile(demandSegment.getBasketValueDistribution(), 0.5);

		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CustomerDemandService.calculateExpectedValue(expectedRevenue, maximumRevenueValue,
				objectiveSpecificValues, demandSegment);
	}

	public static double calculateQuantileValue(double maximumRevenueValue,
			HashMap<Entity, Object> objectiveSpecificValues, DemandSegment demandSegment, double quantile) {
		// Calculate immediate reward

		Double quantileRevenue = 0.0;
		try {
			quantileRevenue = ProbabilityDistributionService
					.getXByCummulativeDistributionQuantile(demandSegment.getBasketValueDistribution(), quantile);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CustomerDemandService.calculateExpectedValue(quantileRevenue, maximumRevenueValue,
				objectiveSpecificValues, demandSegment);
	}

	public static HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> prepareOrderRequestsForDeliveryAreas(
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, DeliveryAreaSet deliveryAreaSet) {

		// Initialise Hashmap
		HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>>();

		for (DeliveryArea area : deliveryAreaSet.getElements()) {
			orderRequestsPerDeliveryArea.put(area.getId(), new HashMap<Integer, HashMap<Integer, OrderRequest>>());
		}

		// Go through request sets
		for (int setId = 0; setId < orderRequestSetsForLearning.size(); setId++) {

			ArrayList<OrderRequest> requests = orderRequestSetsForLearning.get(setId).getElements();
			if (deliveryAreaSet.getElements().size() > 1) {
				for (int regId = 0; regId < requests.size(); regId++) {

					DeliveryArea assignedArea = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
							requests.get(regId).getCustomer());
					if (!orderRequestsPerDeliveryArea.get(assignedArea.getId()).containsKey(setId)) {
						HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
						assignedRequests.put(requests.get(regId).getArrivalTime(), requests.get(regId));
						orderRequestsPerDeliveryArea.get(assignedArea.getId()).put(setId, assignedRequests);
					} else {
						orderRequestsPerDeliveryArea.get(assignedArea.getId()).get(setId)
								.put(requests.get(regId).getArrivalTime(), requests.get(regId));
					}
				}
			} else {
				HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
				orderRequestsPerDeliveryArea.get(deliveryAreaSet.getElements().get(0).getId()).put(setId,
						assignedRequests);
				for (int regId = 0; regId < requests.size(); regId++) {
					orderRequestsPerDeliveryArea.get(deliveryAreaSet.getElements().get(0).getId()).get(setId)
							.put(requests.get(regId).getArrivalTime(), requests.get(regId));
				}

			}
		}

		return orderRequestsPerDeliveryArea;
	}
}
