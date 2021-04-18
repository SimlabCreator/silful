package logic.service.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DemandSegmentForecast;
import data.entity.DemandSegmentForecastSet;
import data.entity.Entity;
import data.entity.Node;
import data.entity.ObjectiveWeight;
import data.entity.TimeWindow;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.utility.DataServiceProvider;
import logic.entity.AlternativeCapacity;
import logic.entity.DistanceToRoundForAlternativeOffer;
import logic.entity.ForecastedOrderRequest;
import logic.utility.comparator.DistanceToRoundAscComparator;
import logic.utility.comparator.DistanceToRoundDescComparator;
import logic.utility.comparator.ForecastedOrderRequestValueDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides functionality relating to capacity determination
 * 
 * @author M. Lang
 *
 */
public class CapacityService {

	/**
	 * Provides capacities per alternative
	 * 
	 * @param capacitySet
	 *            Respective capacity set of time windows
	 * @param alternativeSet
	 *            Respective alternative set (has to refer to time windows)
	 * @return List of alternative capacities
	 */
	public static ArrayList<AlternativeCapacity> getCapacitiesPerAlternative(CapacitySet capacitySet,
			AlternativeSet alternativeSet) {
		ArrayList<AlternativeCapacity> altCapacities = new ArrayList<AlternativeCapacity>();

		ArrayList<Capacity> capacities = capacitySet.getElements();
		ArrayList<Alternative> alternatives = alternativeSet.getElements();
		ArrayList<DeliveryArea> deliveryAreas = capacitySet.getDeliveryAreaSet().getElements();

		// Produce a new AlternativeCapacity per alternative and delivery area
		for (int alternativeIndex = 0; alternativeIndex < alternatives.size(); alternativeIndex++) {
			Alternative alternative = alternatives.get(alternativeIndex);
			ArrayList<TimeWindow> timeWindows = alternative.getTimeWindows();

			for (int daIndex = 0; daIndex < deliveryAreas.size(); daIndex++) {

				DeliveryArea da = deliveryAreas.get(daIndex);

				// New AlternativeCapacity
				AlternativeCapacity altCap = new AlternativeCapacity();
				altCap.setAlternativeId(alternative.getId());
				altCap.setAlternative(alternative);
				altCap.setDeliveryAreaId(da.getId());
				altCap.setDeliveryArea(da);

				// Sum up capacities of respective time windows
				int sumCap = 0;

				for (int capacityIndex = 0; capacityIndex < capacities.size(); capacityIndex++) {
					Capacity cap =capacities.get(capacityIndex);
					if (cap.getDeliveryAreaId() == altCap.getDeliveryAreaId()) {
						for (int twIndex = 0; twIndex < timeWindows.size(); twIndex++) {
							if (cap.getTimeWindowId() == (timeWindows.get(twIndex)).getId()) {
								sumCap += cap.getCapacityNumber();
								break;
							}
						}
					}
				}

				altCap.setCapacityNumber(sumCap);
				// Leave value bucket null because no limits
				altCapacities.add(altCap);

			}

		}

		return altCapacities;
	}

	/**
	 * Provides the capacities grouped by delivery areas
	 * 
	 * @param capacities
	 *            Respective alternative capacities
	 * @return Grouped capacities, grouping by delivery area
	 */
	public static HashMap<DeliveryArea, ArrayList<AlternativeCapacity>> getAlternativeCapacitiesPerDeliveryArea(
			ArrayList<AlternativeCapacity> capacities) {
		HashMap<DeliveryArea, ArrayList<AlternativeCapacity>> capPerDa = new HashMap<DeliveryArea, ArrayList<AlternativeCapacity>>();

		for (AlternativeCapacity cap : capacities) {
			if (capPerDa.containsKey(cap.getDeliveryArea())) {
				capPerDa.get(cap.getDeliveryArea()).add(cap);
			} else {
				ArrayList<AlternativeCapacity> daCapacities = new ArrayList<AlternativeCapacity>();
				daCapacities.add(cap);
				capPerDa.put(cap.getDeliveryArea(), daCapacities);
			}
		}

		return capPerDa;
	}

	/**
	 * Helper to produce pseudo-requests from a forecast of independent demand
	 * 
	 * @param demandForecastSet
	 *            Respective demand forecast set with value buckets and specific
	 *            alternatives
	 * @return List of pseudo requests
	 */
	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestsByValueBucketForcastSet(
			ValueBucketForecastSet demandForecastSet) {

		ArrayList<ValueBucketForecast> forecasts = demandForecastSet.getElements();
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		ArrayList<Node> nodes = DataServiceProvider.getRegionDataServiceImplInstance()
				.getNodesByRegionId(demandForecastSet.getDeliveryAreaSet().getRegionId());

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			 da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					 da.getCenterLat(),  da.getCenterLon()));
		}

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted
		int overallNumberOfRequests = 1;
		for (int fID = 0; fID < forecasts.size(); fID++) {
			ValueBucketForecast forecast = forecasts.get(fID);

			// All requests have the same preference (respective alternative of
			// the forecast) and same revenue
			HashMap<Integer, Alternative> preferenceList = new HashMap<Integer, Alternative>();
			preferenceList.put(1, forecast.getAlternative());

			Double revenue = (forecast.getValueBucket().getUpperBound() + forecast.getValueBucket().getLowerBound()) / 2;

			for (int orID = 0; orID < forecast.getDemandNumber(); orID++) {

				ForecastedOrderRequest request = new ForecastedOrderRequest();
				request.setId(overallNumberOfRequests++);
				request.setDeliveryArea(forecast.getDeliveryArea());
				request.setDeliveryAreaId(forecast.getDeliveryAreaId());
				request.setAlternativePreferenceList(preferenceList);
				request.setEstimatedValue(revenue);

				// Add closest node
				for (DeliveryArea da : daWithNodes) {

					if (da.getId() == forecast.getDeliveryAreaId()) {
						request.setClosestNode(da.getTempClosestNodeCenter());
						break;
					}
				}

				requests.add(request);
			}
		}

		System.out.println("Overall number of requests " + requests.size());
		return requests;

	}

	/**
	 * Helper to produce pseudo-requests from a forecast of dependent demand
	 * Builds forecast*alternativeNumber requests
	 * 
	 * @param demandForecastSet
	 *            Respective demand forecast set with demand segments
	 * @return List of pseudo requests
	 * @throws ParameterUnknownException 
	 */
	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestsByDemandSegmentForecastSetDuplicates(
			DemandSegmentForecastSet demandForecastSet, ArrayList<Node> nodes, ArrayList<ObjectiveWeight> objectives,
			double maxRevenue) throws ParameterUnknownException {

		// Relevant forecasts
		ArrayList<DemandSegmentForecast> forecasts = demandForecastSet.getElements();

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					da.getCenterLat(), da.getCenterLon()));
		}

		// Prepare local visibility values if needed
		HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>> forecastsPerDa = new HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>>();
		// HashMap<DeliveryArea, Integer> numberPerDa = new
		// HashMap<DeliveryArea, Integer>();
		HashMap<DeliveryArea, HashMap<Alternative, Double>> numberPerDaTW = new HashMap<DeliveryArea, HashMap<Alternative, Double>>();
		Double maximumVisibilityValue = 0.0;
		for (ObjectiveWeight ow: objectives) {
			
			if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {

				maximumVisibilityValue = CapacityService.prepareVisibilityMeasures(forecastsPerDa, numberPerDaTW,
						demandForecastSet);

			}
		}
		// Pseudo-request list
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		int overallNumberOfRequests = 0;

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted * considerationSetSize
		double highestWeightedValue = 0.0f;
		int highestSegment = 0;
		int highestDA = 0;
		int highestAlt = 0;
		for (int fID = 0; fID < forecasts.size(); fID++) {

			DemandSegmentForecast forecast = forecasts.get(fID);

			// Produce pseudo-offer set to determine probabilities per
			// alternative
			ArrayList<ConsiderationSetAlternative> csSet = forecast.getDemandSegment().getConsiderationSet();
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			for (int csID = 0; csID < csSet.size(); csID++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative( csSet.get(csID).getAlternative());
				offer.setAlternativeId(offer.getAlternative().getId());
				offers.add(offer);
			}

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offers,
					forecast.getDemandSegment());

			// // Do not consider no-purchase alternative
			// Double newDivisor =  0.0;
			// Iterator<AlternativeOffer> it =
			// probabilities.keySet().iterator();
			// while (it.hasNext()) {
			// AlternativeOffer o = it.next();
			// if (o != null) {
			// newDivisor += probabilities.get(o);
			// }
			// }
			//
			// HashMap<AlternativeOffer, Double> relevantProbabilities = new
			// HashMap<AlternativeOffer, Double>();
			// Iterator<AlternativeOffer> it2 =
			// probabilities.keySet().iterator();
			// while (it2.hasNext()) {
			// AlternativeOffer o = it2.next();
			// if (o != null) {
			// relevantProbabilities.put(o, probabilities.get(o) / newDivisor);
			// }
			// }

			Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
					forecast.getDemandSegment().getBasketValueDistribution()) / maxRevenue;

			// For each forecasted request, create for all alternatives of the
			// consideration set a pseudo-request
			for (int i = 0; i < forecast.getDemandNumber(); i++) {
				// For each alternative, produce a pseudo-request with weighted
				// value
				Iterator<AlternativeOffer> it3 = probabilities.keySet().iterator();

				Node closestNode = null;

				for (DeliveryArea da : daWithNodes) {
					if (da.getId() == forecast.getDeliveryAreaId()) {
						closestNode = da.getTempClosestNodeCenter();
						break;
					}
				}

				while (it3.hasNext()) {
					AlternativeOffer offer = it3.next();
					// Only for the purchase-alternatives. Not for the
					// no-purchase alternative.
					if (offer != null) {

						ForecastedOrderRequest request = new ForecastedOrderRequest();
						HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
						preferences.put(1, offer.getAlternative());
						request.setAlternativePreferenceList(preferences);
						request.setDeliveryArea(forecast.getDeliveryArea());
						request.setDeliveryAreaId(forecast.getDeliveryAreaId());

						Double weightedValue = 0.0;
						Double weightRevenue = 1.0;
						if (objectives.size() > 0) {

							for (Entity obj : objectives) {
								ObjectiveWeight ow = (ObjectiveWeight) obj;
								weightRevenue -= ow.getValue();
								if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
									weightedValue += ow.getValue()
											* forecast.getDemandSegment().getSocialImpactFactor();
									// if(forecast.getDemandSegment().getId()==16)
									// System.out.print(" weight social impact:
									// "+ow.getValue());
								} else if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {
									// System.out.println(" max visibility:
									// "+maximumVisibilityValue+" current
									// visibility:
									// "+numberPerDaTW.get(forecast.getDeliveryArea()).get(offer.getAlternative())+"
									// weight for visbility:"+ow.getValue()+"
									// weightedvalue: "+ow.getValue() *
									// (maximumVisibilityValue-numberPerDaTW.get(forecast.getDeliveryArea()).get(offer.getAlternative()))/maximumVisibilityValue);

									weightedValue += ow.getValue() * (maximumVisibilityValue
											- numberPerDaTW.get(forecast.getDeliveryArea()).get(offer.getAlternative()))
											/ maximumVisibilityValue;
									// if(forecast.getDemandSegment().getId()==16)
									// System.out.print(" weight visibility:
									// "+ow.getValue());
									// if(forecast.getDemandSegment().getId()==16)
									// System.out.print(" visibility:
									// "+(maximumVisibilityValue-numberPerDaTW.get(forecast.getDeliveryArea()).get(offer.getAlternative()))/maximumVisibilityValue);
								}
							}

						}

						weightedValue += weightRevenue * meanRevenue;
						if (weightedValue > highestWeightedValue) {
							highestWeightedValue = weightedValue;
							highestSegment = forecast.getDemandSegment().getId();
							highestDA = forecast.getDeliveryAreaId();
							highestAlt = offer.getAlternativeId();
						}
						// if(forecast.getDemandSegment().getId()==16)
						// System.out.print("weight revenue: "+weightRevenue);
						// if(forecast.getDemandSegment().getId()==16)System.out.println("demand
						// segment: "+forecast.getDemandSegment().getId()+"da:
						// "+forecast.getDeliveryArea().getId()+" alt:
						// "+offer.getAlternativeId()+" weighted value:
						// "+weightedValue);
						// System.out.println("weight revenue: "+weightRevenue+"
						// weighted value: "+weightedValue);
						request.setEstimatedValue(weightedValue * probabilities.get(offer));
						request.setId(overallNumberOfRequests++);
						request.setClosestNode(closestNode);
						requests.add(request);

					}
				}
			}

			// Old with seperation
			// while (it3.hasNext()) {
			//
			// int numberOfRequests;
			// AlternativeOffer inFocus = it3.next();
			// if (numberOfAlternatives > 1) {// The last alternative has to
			// // fill up the rest, the others
			// // get their rounded ratio
			// numberOfRequests = Math.round(relevantProbabilities.get(inFocus)
			// * forecast.getDemandNumber());
			//
			// if (forecast.getDemandNumber() - numberOfRequestsForForecast <
			// numberOfRequests) {// It
			// // should
			// // be
			// // enough
			// // left
			// numberOfRequests = forecast.getDemandNumber() -
			// numberOfRequestsForForecast;
			// numberOfRequestsForForecast = forecast.getDemandNumber();
			// } else {
			// numberOfRequestsForForecast += numberOfRequests;
			// }
			//
			// } else {
			// numberOfRequests = forecast.getDemandNumber() -
			// numberOfRequestsForForecast;
			// }
			//
			// // Produce the respective number of requests
			// for (int i = 0; i < numberOfRequests; i++) {
			// ForecastedOrderRequest request = new ForecastedOrderRequest();
			// HashMap<Integer, Alternative> preferences = new HashMap<Integer,
			// Alternative>();
			// preferences.put(1, inFocus.getAlternative());
			// request.setAlternativePreferenceList(preferences);
			// request.setDeliveryArea(forecast.getDeliveryArea());
			// request.setDeliveryAreaId(forecast.getDeliveryAreaId());
			// request.setEstimatedRevenue(meanRevenue);
			// request.setId(overallNumberOfRequests++);
			// // Add closest node
			// for (DeliveryArea da : daWithNodes) {
			// if (da.getId() == forecast.getDeliveryAreaId()) {
			// request.setClosestNode(da.getTempClosestNodeCenter());
			// break;
			// }
			// }
			// requests.add(request);
			// }
			//
			// numberOfAlternatives--;
			//
			// }
		}
		System.out.println("highest weighted value: " + highestWeightedValue + " for segment: " + highestSegment
				+ "and delivery area: " + highestDA + " and alt: " + highestAlt);
		return requests;

	}

	/**
	 * Helper to produce pseudo-requests from a forecast of dependent demand
	 * Builds forecast# requests
	 * 
	 * @param demandForecastSet
	 *            Respective demand forecast set with demand segments
	 * @return List of pseudo requests
	 * @throws ParameterUnknownException 
	 */
	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestsByDemandSegmentForecastSetRatio(
			DemandSegmentForecastSet demandForecastSet, ArrayList<Node> nodes, ArrayList<ObjectiveWeight> objectives,
			double maxRevenue) throws ParameterUnknownException {

		// Relevant forecasts
		ArrayList<DemandSegmentForecast> forecasts = demandForecastSet.getElements();

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					 da.getCenterLat(), da.getCenterLon()));
		}

		// Prepare local visibility values if needed
		HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>> forecastsPerDa = new HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>>();
		// HashMap<DeliveryArea, Integer> numberPerDa = new
		// HashMap<DeliveryArea, Integer>();
		HashMap<DeliveryArea, HashMap<Alternative, Double>> numberPerDaTW = new HashMap<DeliveryArea, HashMap<Alternative, Double>>();
		Double maximumVisibilityValue = 0.0;
		for (ObjectiveWeight ow : objectives) {
			 
			if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {

				maximumVisibilityValue = CapacityService.prepareVisibilityMeasures(forecastsPerDa, numberPerDaTW,
						demandForecastSet);

			}
		}
		// Pseudo-request list
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		int overallNumberOfRequests = 0;

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted

		for (int fID = 0; fID < forecasts.size(); fID++) {

			DemandSegmentForecast forecast = forecasts.get(fID);

			Node closestNode = null;

			for (DeliveryArea da : daWithNodes) {
				if (da.getId() == forecast.getDeliveryAreaId()) {
					closestNode = da.getTempClosestNodeCenter();
					break;
				}
			}

			// Produce pseudo-offer set to determine probabilities per
			// alternative
			ArrayList<ConsiderationSetAlternative> csSet = forecast.getDemandSegment().getConsiderationSet();
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			for (int csID = 0; csID < csSet.size(); csID++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative( csSet.get(csID).getAlternative());
				offer.setAlternativeId(offer.getAlternative().getId());
				offers.add(offer);
			}

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offers,
					forecast.getDemandSegment());

			// Do not consider no-purchase alternative
			// Double newDivisor =  0.0;
			// Iterator<AlternativeOffer> it =
			// probabilities.keySet().iterator();
			// while (it.hasNext()) {
			// AlternativeOffer o = it.next();
			// if (o != null) {
			// newDivisor += probabilities.get(o);
			// }
			// }
			//
			// HashMap<AlternativeOffer, Double> relevantProbabilities = new
			// HashMap<AlternativeOffer, Double>();
			// Iterator<AlternativeOffer> it2 =
			// probabilities.keySet().iterator();
			// while (it2.hasNext()) {
			// AlternativeOffer o = it2.next();
			// if (o != null) {
			// relevantProbabilities.put(o, probabilities.get(o) / newDivisor);
			// }
			// }

			Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
					forecast.getDemandSegment().getBasketValueDistribution()) / maxRevenue;

			// Determine part per alternative
			// Additionally buffer distance to rounded number in case the
			// overall number of requests exceeds the forecast after rounding
			HashMap<AlternativeOffer, Integer> requestsPerAlternative = new HashMap<AlternativeOffer, Integer>();
			ArrayList<DistanceToRoundForAlternativeOffer> distanceToRoundBorder = new ArrayList<DistanceToRoundForAlternativeOffer>();
			int overallRequests = 0;
			int noPurchaseNumber = 0;
			for (AlternativeOffer altOffer : probabilities.keySet()) {

				if (altOffer != null) {
					double forecastedNumberAlt = probabilities.get(altOffer) * forecast.getDemandNumber();
					requestsPerAlternative.put(altOffer, (int)Math.round(forecastedNumberAlt));
					overallRequests += Math.round(forecastedNumberAlt);
					distanceToRoundBorder.add(new DistanceToRoundForAlternativeOffer(altOffer,
							(Math.round(forecastedNumberAlt)) - forecastedNumberAlt));
				} else {
					noPurchaseNumber = (int) Math.round(probabilities.get(altOffer) * forecast.getDemandNumber());
				}

			}

			// If there are to many requests due to rounding, decrease number
			// for the alternatives with the highest distance to the next full
			// number
			if (overallRequests > forecast.getDemandNumber() - noPurchaseNumber) {
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (overallRequests > forecast.getDemandNumber() - noPurchaseNumber) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) - 1);
					currentlyHighest++;
					overallRequests--;

				}
			} else if (overallRequests < forecast.getDemandNumber() - noPurchaseNumber) {
				// If there are to few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (overallRequests < forecast.getDemandNumber() - noPurchaseNumber) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) + 1);
					currentlyHighest++;
					overallRequests++;

				}

			}

			// Go through alternatives and produce respective pseudo-requests
			for (AlternativeOffer offerAlt : requestsPerAlternative.keySet()) {
				// if (offerAlt.getAlternativeId() == 31 &&
				// forecast.getDeliveryAreaId() == 16)
				// System.out.println("Number of requests for 16/31 " +
				// requestsPerAlternative.get(offerAlt));
				for (int i = 0; i < requestsPerAlternative.get(offerAlt); i++) {
					ForecastedOrderRequest request = new ForecastedOrderRequest();
					HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
					preferences.put(1, offerAlt.getAlternative());
					request.setAlternativePreferenceList(preferences);
					request.setDeliveryArea(forecast.getDeliveryArea());
					request.setDeliveryAreaId(forecast.getDeliveryAreaId());

					Double weightedValue = 0.0;
					Double weightRevenue = 1.0;
					if (objectives.size() > 0) {

						for (Entity obj : objectives) {
							ObjectiveWeight ow = (ObjectiveWeight) obj;
							weightRevenue -= ow.getValue();
							if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
								weightedValue += ow.getValue() * forecast.getDemandSegment().getSocialImpactFactor();

							} else if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {

								weightedValue += ow.getValue() * (maximumVisibilityValue
										- numberPerDaTW.get(forecast.getDeliveryArea()).get(offerAlt.getAlternative()))
										/ maximumVisibilityValue;

							}
						}

					}

					weightedValue += weightRevenue * meanRevenue;

					request.setEstimatedValue(weightedValue);
					request.setId(overallNumberOfRequests++);
					request.setClosestNode(closestNode);
					requests.add(request);

				}
			}

		}

		return requests;

	}

	private static double prepareVisibilityMeasures(
			HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>> forecastsPerDa,
			HashMap<DeliveryArea, HashMap<Alternative, Double>> numberPerDaTW,
			DemandSegmentForecastSet demandForecastSet) throws ParameterUnknownException {
		double maximumVisibilityValue = 0.0f;
		forecastsPerDa = ForecastingService.groupDemandSegmentForecastsByDeliveryArea(demandForecastSet);

		for (DeliveryArea area : forecastsPerDa.keySet()) {

			HashMap<Alternative, Double> numberPerAlt = new HashMap<Alternative, Double>();
			numberPerDaTW.put(area, numberPerAlt);

			// Per delivery area, go through all forecasts and determine request
			// number per alternative

			for (DemandSegmentForecast f : forecastsPerDa.get(area)) {

				// Determine selection probabilities for the
				// alternatives of the
				// complete consideration set
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (int i = 0; i < f.getDemandSegment().getConsiderationSet().size(); i++) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(
							( f.getDemandSegment().getConsiderationSet().get(i))
									.getAlternative());
					offer.setAlternativeId(offer.getAlternative().getId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel(
						demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offeredAlternatives,
						f.getDemandSegment());

				for (AlternativeOffer offer : probs.keySet()) {
					if (offer != null) {
						Alternative alternative = offer.getAlternative();
						if (numberPerDaTW.get(area).containsKey(alternative)) {
							Double updatedValue = numberPerDaTW.get(area).get(alternative)
									+ f.getDemandNumber() * probs.get(offer)
											* ProbabilityDistributionService.getMeanByProbabilityDistribution(
													f.getDemandSegment().getBasketValueDistribution());
							numberPerDaTW.get(area).put(alternative, updatedValue);

						} else {
							numberPerDaTW.get(area).put(alternative,
									f.getDemandNumber() * probs.get(offer)
											* ProbabilityDistributionService.getMeanByProbabilityDistribution(
													f.getDemandSegment().getBasketValueDistribution()));
							// System.out.println("Delivery area: "+
							// area.getId()+" Demand segment id:
							// "+f.getDemandSegment().getId()+" forecast:
							// "+f.getDemandNumber()+" probAlt:
							// "+probs.get(offer)+ "value is
							// :"+f.getDemandNumber() *
							// probs.get(offer)*ProbabilityDistributionService
							// .getMeanByProbabilityDistribution(f.getDemandSegment().getBasketValueDistribution()));
						}
					}
				}

			}

		}

		// Determine maximum value

		for (DeliveryArea area : numberPerDaTW.keySet()) {
			for (Alternative alt : numberPerDaTW.get(area).keySet()) {
				if (numberPerDaTW.get(area).get(alt) > maximumVisibilityValue) {
					maximumVisibilityValue = numberPerDaTW.get(area).get(alt);
				}

			}
		}
		return maximumVisibilityValue;
	}

	/**
	 * Helper to produce pseudo-requests from a forecast of dependent demand
	 * Builds forecast# requests
	 * 
	 * @param demandForecastSet
	 *            Respective demand forecast set with demand segments
	 * @return List of pseudo requests
	 * @throws ParameterUnknownException 
	 */
	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestsByDemandSegmentForecastSetRatioVisibilityHardConstraint(
			DemandSegmentForecastSet demandForecastSet, ArrayList<Node> nodes, ArrayList<ObjectiveWeight> objectives,
			double maxRevenue) throws ParameterUnknownException {

		// Relevant forecasts
		ArrayList<DemandSegmentForecast> forecasts = demandForecastSet.getElements();

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					da.getCenterLat(), da.getCenterLon()));
	

		}
		// Pseudo-request list
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		int overallNumberOfRequests = 0;

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted

		for (int fID = 0; fID < forecasts.size(); fID++) {

			DemandSegmentForecast forecast = forecasts.get(fID);

			Node closestNode = null;

			for (DeliveryArea da : daWithNodes) {
				if (da.getId() == forecast.getDeliveryAreaId()) {
					closestNode = da.getTempClosestNodeCenter();
					break;
				}
			}

			// Produce pseudo-offer set to determine probabilities per
			// alternative
			ArrayList<Alternative> csSet = demandForecastSet.getDemandSegmentSet().getAlternativeSet().getElements();
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			for (int csID = 0; csID < csSet.size(); csID++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative( csSet.get(csID));
				offer.setAlternativeId(offer.getAlternative().getId());
				offers.add(offer);
			}

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offers,
					forecast.getDemandSegment());

			Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
					forecast.getDemandSegment().getBasketValueDistribution()) / maxRevenue;

			// Determine part per alternative
			// Additionally buffer distance to rounded number in case the
			// overall number of requests exceeds the forecast after rounding
			HashMap<AlternativeOffer, Integer> requestsPerAlternative = new HashMap<AlternativeOffer, Integer>();
			ArrayList<DistanceToRoundForAlternativeOffer> distanceToRoundBorder = new ArrayList<DistanceToRoundForAlternativeOffer>();
			int overallRequests = 0;
			int noPurchaseNumber = 0;
			for (AlternativeOffer altOffer : probabilities.keySet()) {

				if (altOffer != null) {
					double forecastedNumberAlt = probabilities.get(altOffer) * forecast.getDemandNumber();
					requestsPerAlternative.put(altOffer, (int)Math.round(forecastedNumberAlt));
					overallRequests += Math.round(forecastedNumberAlt);
					distanceToRoundBorder.add(new DistanceToRoundForAlternativeOffer(altOffer,
							(Math.round(forecastedNumberAlt)) - forecastedNumberAlt));
				} else {
					noPurchaseNumber = (int) Math.round(probabilities.get(altOffer) * forecast.getDemandNumber());
				}

			}

			// If there are too many requests due to rounding, decrease number
			// for the alternatives with the highest distance to the next full
			// number
			if (overallRequests > (forecast.getDemandNumber() - noPurchaseNumber)) {
				// First, shuffle (otherwise for same values, always first are decreased) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (overallRequests > (forecast.getDemandNumber() - noPurchaseNumber)) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) - 1);
					currentlyHighest++;
					overallRequests--;

				}
			} else if (overallRequests < (forecast.getDemandNumber() - noPurchaseNumber)) {
				// If there are to few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, shuffle (otherwise for same values, always first are increased) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (overallRequests < (forecast.getDemandNumber() - noPurchaseNumber)) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) + 1);
					currentlyHighest++;
					overallRequests++;

				}

			}

			// Go through alternatives and produce respective pseudo-requests
			for (AlternativeOffer offerAlt : requestsPerAlternative.keySet()) {

				for (int i = 0; i < requestsPerAlternative.get(offerAlt); i++) {
					ForecastedOrderRequest request = new ForecastedOrderRequest();
					HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
					preferences.put(1, offerAlt.getAlternative());
					request.setAlternativePreferenceList(preferences);
					request.setDeliveryArea(forecast.getDeliveryArea());
					request.setDeliveryAreaId(forecast.getDeliveryAreaId());

					Double weightedValue = 0.0;
					Double weightRevenue = 1.0;
					if (objectives.size() > 0) {

						for (Entity obj : objectives) {
							ObjectiveWeight ow = (ObjectiveWeight) obj;

							if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
								weightRevenue -= ow.getValue();
								weightedValue += ow.getValue() * forecast.getDemandSegment().getSocialImpactFactor();

							}
						}

					}

					weightedValue += weightRevenue * meanRevenue;
					// if(weightedValue > 0.6){
					// System.out.println("High request
					// segment:"+forecast.getDemandSegment().getId());
					// }

					request.setEstimatedValue(weightedValue);
					request.setId(overallNumberOfRequests++);
					request.setClosestNode(closestNode);
					request.setClosestNodeId(closestNode.getLongId());
					requests.add(request);

				}
			}

		}

		// Add local visibility measure approach
		if (objectives.size() > 0) {
			// double highestValue = highestOverallRequest.getEstimatedValue() *
			// 2;
			for (ObjectiveWeight ow : objectives) {
				
				if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {

					HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>> requestsPerDaAndAlt = new HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>>();

					// Go through requests and assign to respective combination
					Double highestEstimatedValue = 0.0;
					for (ForecastedOrderRequest r : requests) {

						// Update highest
						if (r.getEstimatedValue() > highestEstimatedValue)
							highestEstimatedValue = r.getEstimatedValue();

						// Add to list
						if (requestsPerDaAndAlt.containsKey(r.getDeliveryAreaId())) {
							if (requestsPerDaAndAlt.get(r.getDeliveryAreaId())
									.containsKey(r.getAlternativePreferenceList().get(1).getId())) {
								requestsPerDaAndAlt.get(r.getDeliveryAreaId())
										.get(r.getAlternativePreferenceList().get(1).getId()).add(r);
							} else {
								ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
								altRequests.add(r);
								requestsPerDaAndAlt.get(r.getDeliveryAreaId())
										.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
							}
						} else {
							// Start new list for delivery area
							ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
							altRequests.add(r);
							HashMap<Integer, ArrayList<ForecastedOrderRequest>> daRequests = new HashMap<Integer, ArrayList<ForecastedOrderRequest>>();
							daRequests.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
							requestsPerDaAndAlt.put(r.getDeliveryAreaId(), daRequests);
						}
					}

					// Go through all possible combinations and set as many high
					// requests as the objective weight wants
					highestEstimatedValue = 2 * highestEstimatedValue;
					for (DeliveryArea currentArea : demandForecastSet.getDeliveryAreaSet().getElements()) {

						
						Integer daID = currentArea.getId();

						for (Alternative currentAlternative : demandForecastSet.getDemandSegmentSet().getAlternativeSet().getElements()) {

							
							Integer altID = currentAlternative.getId();

							ArrayList<ForecastedOrderRequest> combinationRequests = new ArrayList<ForecastedOrderRequest>();

							if (requestsPerDaAndAlt.containsKey(daID)) {
								if (requestsPerDaAndAlt.get(daID).containsKey(altID)) {
									combinationRequests = requestsPerDaAndAlt.get(daID).get(altID);
								}
							}

							// If there are more requests than the respective
							// minimum, sort and set highest to the high value
							if (combinationRequests.size() > ow.getValue()) {
								Collections.sort(combinationRequests, new ForecastedOrderRequestValueDescComparator());
								for (int i = 0; i < ow.getValue(); i++) {
									combinationRequests.get(i).setEstimatedValue(highestEstimatedValue);
								}
							} else {
								// Otherwise, set available to high value and
								// create new for missing
								for (int i = 0; i < combinationRequests.size(); i++) {
									combinationRequests.get(i).setEstimatedValue(highestEstimatedValue);
								}

								for (int i = 0; i < ow.getValue() - combinationRequests.size(); i++) {

									ForecastedOrderRequest request = new ForecastedOrderRequest();
									HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
									preferences.put(1, currentAlternative);
									request.setAlternativePreferenceList(preferences);
									request.setDeliveryArea(currentArea);
									request.setDeliveryAreaId(currentArea.getId());
									request.setEstimatedValue(highestEstimatedValue);
									request.setId(overallNumberOfRequests++);

									Node close = null;
									for (DeliveryArea area : daWithNodes) {
										if (daID == area.getId()) {
											close = area.getTempClosestNodeCenter();
											break;
										}
									}
									request.setClosestNode(close);
									requests.add(request);
								}
							}
							;
						}
					}

					for (ForecastedOrderRequest r : requests) {
						if (r.getEstimatedValue() == highestEstimatedValue) {
							System.out.println("Highest value for da " + r.getDeliveryAreaId() + " and alt "
									+ r.getAlternativePreferenceList().get(1).getId() + "- value is "
									+ r.getEstimatedValue());
						}
					}

				}
			}
		}

		return requests;

	}
	
	/**
	 * Helper to produce pseudo-requests from a forecast of dependent demand
	 * Builds forecast# requests
	 * 
	 * @param demandForecastSet
	 *            Respective demand forecast set with demand segments
	 * @return List of pseudo requests
	 * @throws ParameterUnknownException 
	 */
	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestsByDemandSegmentForecastSetRatioVisibilityHardConstraintWithMinimumRequests(
			DemandSegmentForecastSet demandForecastSet, ArrayList<Node> nodes, ArrayList<ObjectiveWeight> objectives,
			double maxRevenue, int minimumRequestsPerCombination) throws ParameterUnknownException {

		// Relevant forecasts
		ArrayList<DemandSegmentForecast> forecasts = demandForecastSet.getElements();

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			 da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					 da.getCenterLat(), da.getCenterLon()));

		}
		// Pseudo-request list
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		int overallNumberOfRequests = 0;

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted

		for (int fID = 0; fID < forecasts.size(); fID++) {

			DemandSegmentForecast forecast =  forecasts.get(fID);

			Node closestNode = null;

			for (DeliveryArea da : daWithNodes) {
				if (da.getId() == forecast.getDeliveryAreaId()) {
					closestNode = da.getTempClosestNodeCenter();
					break;
				}
			}

			// Produce pseudo-offer set to determine probabilities per
			// alternative
			ArrayList<Alternative> csSet = demandForecastSet.getDemandSegmentSet().getAlternativeSet().getElements();
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			for (int csID = 0; csID < csSet.size(); csID++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(csSet.get(csID));
				offer.setAlternativeId(offer.getAlternative().getId());
				offers.add(offer);
			}

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offers,
					forecast.getDemandSegment());

			Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
					forecast.getDemandSegment().getBasketValueDistribution()) / maxRevenue;

			// Determine part per alternative
			// Additionally buffer distance to rounded number in case the
			// overall number of requests exceeds the forecast after rounding
			HashMap<AlternativeOffer, Integer> requestsPerAlternative = new HashMap<AlternativeOffer, Integer>();
			ArrayList<DistanceToRoundForAlternativeOffer> distanceToRoundBorder = new ArrayList<DistanceToRoundForAlternativeOffer>();
			int overallRequests = 0;
			int noPurchaseNumber = 0;
			for (AlternativeOffer altOffer : probabilities.keySet()) {

				if (altOffer != null) {
					double forecastedNumberAlt = probabilities.get(altOffer) * forecast.getDemandNumber();
					requestsPerAlternative.put(altOffer, (int)Math.round(forecastedNumberAlt));
					overallRequests += Math.round(forecastedNumberAlt);
					distanceToRoundBorder.add(new DistanceToRoundForAlternativeOffer(altOffer,
							( Math.round(forecastedNumberAlt)) - forecastedNumberAlt));
				} else {
					noPurchaseNumber = (int) Math.round(probabilities.get(altOffer) * forecast.getDemandNumber());
				}

			}

			// If there are too many requests due to rounding, decrease number
			// for the alternatives with the highest distance to the next full
			// number
			if (overallRequests > (forecast.getDemandNumber() - noPurchaseNumber)) {
				// First, shuffle (otherwise for same values, always first are decreased) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (overallRequests > (forecast.getDemandNumber() - noPurchaseNumber)) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) - 1);
					currentlyHighest++;
					overallRequests--;

				}
			} else if (overallRequests < (forecast.getDemandNumber() - noPurchaseNumber)) {
				// If there are to few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, shuffle (otherwise for same values, always first are increased) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (overallRequests < (forecast.getDemandNumber() - noPurchaseNumber)) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) + 1);
					currentlyHighest++;
					overallRequests++;

				}

			}

			// Go through alternatives and produce respective pseudo-requests
			for (AlternativeOffer offerAlt : requestsPerAlternative.keySet()) {

				for (int i = 0; i < requestsPerAlternative.get(offerAlt); i++) {
					ForecastedOrderRequest request = new ForecastedOrderRequest();
					HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
					preferences.put(1, offerAlt.getAlternative());
					request.setAlternativePreferenceList(preferences);
					request.setDeliveryArea(forecast.getDeliveryArea());
					request.setDeliveryAreaId(forecast.getDeliveryAreaId());

					Double weightedValue = 0.0;
					Double weightRevenue = 1.0;
					if (objectives.size() > 0) {

						for (ObjectiveWeight ow : objectives) {
							

							if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
								weightRevenue -= ow.getValue();
								weightedValue += ow.getValue() * forecast.getDemandSegment().getSocialImpactFactor();

							}
						}

					}

					weightedValue += weightRevenue * meanRevenue;
					// if(weightedValue > 0.6){
					// System.out.println("High request
					// segment:"+forecast.getDemandSegment().getId());
					// }

					request.setEstimatedValue(weightedValue);
					request.setId(overallNumberOfRequests++);
					request.setClosestNode(closestNode);
					requests.add(request);

				}
			}

		}

		// Add local visibility measure approach
		int minimumNumberByObjective=0;
		if (objectives.size() > 0) {
			// double highestValue = highestOverallRequest.getEstimatedValue() *
			// 2;
			for (ObjectiveWeight ow : objectives) {
			
				if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {
					minimumNumberByObjective+=ow.getValue();
					HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>> requestsPerDaAndAlt = new HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>>();

					// Go through requests and assign to respective combination
					Double highestEstimatedValue = 0.0;
					for (ForecastedOrderRequest r : requests) {

						// Update highest
						if (r.getEstimatedValue() > highestEstimatedValue)
							highestEstimatedValue = r.getEstimatedValue();

						// Add to list
						if (requestsPerDaAndAlt.containsKey(r.getDeliveryAreaId())) {
							if (requestsPerDaAndAlt.get(r.getDeliveryAreaId())
									.containsKey(r.getAlternativePreferenceList().get(1).getId())) {
								requestsPerDaAndAlt.get(r.getDeliveryAreaId())
										.get(r.getAlternativePreferenceList().get(1).getId()).add(r);
							} else {
								ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
								altRequests.add(r);
								requestsPerDaAndAlt.get(r.getDeliveryAreaId())
										.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
							}
						} else {
							// Start new list for delivery area
							ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
							altRequests.add(r);
							HashMap<Integer, ArrayList<ForecastedOrderRequest>> daRequests = new HashMap<Integer, ArrayList<ForecastedOrderRequest>>();
							daRequests.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
							requestsPerDaAndAlt.put(r.getDeliveryAreaId(), daRequests);
						}
					}

					// Go through all possible combinations and set as many high
					// requests as the objective weight wants
					highestEstimatedValue = 100 * highestEstimatedValue;
					for (DeliveryArea currentArea : demandForecastSet.getDeliveryAreaSet().getElements()) {

						Integer daID = currentArea.getId();

						for (Alternative currentAlternative : demandForecastSet.getDemandSegmentSet().getAlternativeSet().getElements()) {

							Integer altID = currentAlternative.getId();

							ArrayList<ForecastedOrderRequest> combinationRequests = new ArrayList<ForecastedOrderRequest>();

							if (requestsPerDaAndAlt.containsKey(daID)) {
								if (requestsPerDaAndAlt.get(daID).containsKey(altID)) {
									combinationRequests = requestsPerDaAndAlt.get(daID).get(altID);
								}
							}

							// If there are more requests than the respective
							// minimum, sort and set highest to the high value
							if (combinationRequests.size() > ow.getValue()) {
								Collections.sort(combinationRequests, new ForecastedOrderRequestValueDescComparator());
								for (int i = 0; i < ow.getValue(); i++) {
									combinationRequests.get(i).setEstimatedValue(highestEstimatedValue);
								}
							} else {
								// Otherwise, set available to high value and
								// create new for missing
								for (int i = 0; i < combinationRequests.size(); i++) {
									combinationRequests.get(i).setEstimatedValue(highestEstimatedValue);
								}

								for (int i = 0; i < ow.getValue() - combinationRequests.size(); i++) {

									ForecastedOrderRequest request = new ForecastedOrderRequest();
									HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
									preferences.put(1, currentAlternative);
									request.setAlternativePreferenceList(preferences);
									request.setDeliveryArea(currentArea);
									request.setDeliveryAreaId(currentArea.getId());
									request.setEstimatedValue(highestEstimatedValue);
									request.setId(overallNumberOfRequests++);

									Node close = null;
									for (DeliveryArea area : daWithNodes) {
										if (daID == area.getId()) {
											close = area.getTempClosestNodeCenter();
											break;
										}
									}
									request.setClosestNode(close);
									requests.add(request);
								}
							}
							;
						}
					}

					for (ForecastedOrderRequest r : requests) {
						if (r.getEstimatedValue() == highestEstimatedValue) {
							System.out.println("Highest value for da " + r.getDeliveryAreaId() + " and alt "
									+ r.getAlternativePreferenceList().get(1).getId() + "- value is "
									+ r.getEstimatedValue());
						}
					}

				}
			}
		}

		if(minimumRequestsPerCombination>minimumNumberByObjective){
		//Add additional requests for low popular tw by ensuring a minimum request size of the minimumRequestsNumber parameter
		HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>> requestsPerDaAndAlt = new HashMap<Integer, HashMap<Integer, ArrayList<ForecastedOrderRequest>>>();

		// Go through requests and assign to respective combination
		Double lowestEstimatedValue = 0.0;
		for (ForecastedOrderRequest r : requests) {

			// Update lowest
			if (r.getEstimatedValue() < lowestEstimatedValue)
				lowestEstimatedValue = r.getEstimatedValue(); 
			
			// Add to list
			if (requestsPerDaAndAlt.containsKey(r.getDeliveryAreaId())) {
				if (requestsPerDaAndAlt.get(r.getDeliveryAreaId())
						.containsKey(r.getAlternativePreferenceList().get(1).getId())) {
					requestsPerDaAndAlt.get(r.getDeliveryAreaId())
							.get(r.getAlternativePreferenceList().get(1).getId()).add(r);
				} else {
					ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
					altRequests.add(r);
					requestsPerDaAndAlt.get(r.getDeliveryAreaId())
							.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
				}
			} else {
				// Start new list for delivery area
				ArrayList<ForecastedOrderRequest> altRequests = new ArrayList<ForecastedOrderRequest>();
				altRequests.add(r);
				HashMap<Integer, ArrayList<ForecastedOrderRequest>> daRequests = new HashMap<Integer, ArrayList<ForecastedOrderRequest>>();
				daRequests.put(r.getAlternativePreferenceList().get(1).getId(), altRequests);
				requestsPerDaAndAlt.put(r.getDeliveryAreaId(), daRequests);
			}
		}
		
		// Go through all possible combinations and set as many low
		// requests such that minimum request number reached
		for (DeliveryArea currentArea : demandForecastSet.getDeliveryAreaSet().getElements()) {


			Integer daID = currentArea.getId();

			for (Alternative currentAlternative : demandForecastSet.getDemandSegmentSet().getAlternativeSet().getElements()) {

				
				Integer altID = currentAlternative.getId();

				ArrayList<ForecastedOrderRequest> combinationRequests = new ArrayList<ForecastedOrderRequest>();

				if (requestsPerDaAndAlt.containsKey(daID)) {
					if (requestsPerDaAndAlt.get(daID).containsKey(altID)) {
						combinationRequests = requestsPerDaAndAlt.get(daID).get(altID);
					}
				}

				// If there are less requests than the respective
				// minimum, add additional with low value
				if (combinationRequests.size() < minimumRequestsPerCombination) {

					for (int i = 0; i < minimumRequestsPerCombination - combinationRequests.size(); i++) {

						ForecastedOrderRequest request = new ForecastedOrderRequest();
						HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
						preferences.put(1, currentAlternative);
						request.setAlternativePreferenceList(preferences);
						request.setDeliveryArea(currentArea);
						request.setDeliveryAreaId(currentArea.getId());
						request.setEstimatedValue(lowestEstimatedValue/2);
						request.setId(overallNumberOfRequests++);

						Node close = null;
						for (DeliveryArea area : daWithNodes) {
							if (daID == area.getId()) {
								close = area.getTempClosestNodeCenter();
								break;
							}
						}
						request.setClosestNode(close);
						requests.add(request);
					}
				}
				
			}
		}

		}
		return requests;

	}

	public static ArrayList<ForecastedOrderRequest> getForecastedOrderRequestWithoutValuePriority(
			DemandSegmentForecastSet demandForecastSet, ArrayList<Node> nodes) {

		// Relevant forecasts
		ArrayList<DemandSegmentForecast> forecasts = demandForecastSet.getElements();

		ArrayList<DeliveryArea> daWithNodes = demandForecastSet.getDeliveryAreaSet().getElements();
		// Get closest nodes for delivery area centers
		for (DeliveryArea da : daWithNodes) {
			da.setTempClosestNodeCenter(LocationService.findClosestNode(nodes,
					da.getCenterLat(), da.getCenterLon()));

		}

		// Pseudo-request list
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();
		int overallNumberOfRequests = 0;

		// Go through all forecasts and produce as many pseudo requests as
		// demand forecasted

		for (int fID = 0; fID < forecasts.size(); fID++) {

			DemandSegmentForecast forecast = forecasts.get(fID);

			Node closestNode = null;

			for (DeliveryArea da : daWithNodes) {
				if (da.getId() == forecast.getDeliveryAreaId()) {
					closestNode = da.getTempClosestNodeCenter();
					break;
				}
			}

			// Produce pseudo-offer set to determine probabilities per
			// alternative
			ArrayList<ConsiderationSetAlternative> csSet = forecast.getDemandSegment().getConsiderationSet();
			ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
			for (int csID = 0; csID < csSet.size(); csID++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(csSet.get(csID).getAlternative());
				offer.setAlternativeId(offer.getAlternative().getId());
				offers.add(offer);
			}

			HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
					demandForecastSet.getDemandSegmentSet().getDemandModelType().getName(), offers,
					forecast.getDemandSegment());

			// Determine part per alternative
			// Additionally buffer distance to rounded number in case the
			// overall number of requests exceeds the forecast after rounding
			HashMap<AlternativeOffer, Integer> requestsPerAlternative = new HashMap<AlternativeOffer, Integer>();
			ArrayList<DistanceToRoundForAlternativeOffer> distanceToRoundBorder = new ArrayList<DistanceToRoundForAlternativeOffer>();
			int overallRequests = 0;
			int noPurchaseNumber = 0;
			for (AlternativeOffer altOffer : probabilities.keySet()) {

				if (altOffer != null) {
					double forecastedNumberAlt = probabilities.get(altOffer) * forecast.getDemandNumber();
					requestsPerAlternative.put(altOffer, (int)Math.round(forecastedNumberAlt));
					overallRequests += Math.round(forecastedNumberAlt);
					distanceToRoundBorder.add(new DistanceToRoundForAlternativeOffer(altOffer,
							( Math.round(forecastedNumberAlt)) - forecastedNumberAlt));
				} else {
					noPurchaseNumber = (int) Math.round(probabilities.get(altOffer) * forecast.getDemandNumber());
				}

			}

			// If there are to many requests due to rounding, decrease number
			// for the alternatives with the highest distance to the next full
			// number
			if (overallRequests > forecast.getDemandNumber() - noPurchaseNumber) {
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (overallRequests > forecast.getDemandNumber() - noPurchaseNumber) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) - 1);
					currentlyHighest++;
					overallRequests--;

				}
			} else if (overallRequests < forecast.getDemandNumber() - noPurchaseNumber) {
				// If there are to few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (overallRequests < forecast.getDemandNumber() - noPurchaseNumber) {
					requestsPerAlternative.put(distanceToRoundBorder.get(currentlyHighest).getOffer(),
							requestsPerAlternative.get(distanceToRoundBorder.get(currentlyHighest).getOffer()) + 1);
					currentlyHighest++;
					overallRequests++;

				}

			}

			// Go through alternatives and produce respective pseudo-requests
			for (AlternativeOffer offerAlt : requestsPerAlternative.keySet()) {
				// if (offerAlt.getAlternativeId() == 31 &&
				// forecast.getDeliveryAreaId() == 16)
				// System.out.println("Number of requests for 16/31 " +
				// requestsPerAlternative.get(offerAlt));
				for (int i = 0; i < requestsPerAlternative.get(offerAlt); i++) {
					ForecastedOrderRequest request = new ForecastedOrderRequest();
					HashMap<Integer, Alternative> preferences = new HashMap<Integer, Alternative>();
					preferences.put(1, offerAlt.getAlternative());
					request.setAlternativePreferenceList(preferences);
					request.setDeliveryArea(forecast.getDeliveryArea());
					request.setDeliveryAreaId(forecast.getDeliveryAreaId());

					Double weightedValue = 1.0;

					request.setEstimatedValue(weightedValue);
					request.setId(overallNumberOfRequests++);
					request.setClosestNode(closestNode);
					requests.add(request);

				}
			}

		}

		return requests;

	}
}
