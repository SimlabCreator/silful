package logic.algorithm.rm.optimization.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Entity;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;

public class AggregateReferenceInformationAlgorithm {
	public static double distanceMultiplierAsTheCrowFlies = 1.5;
	private static double TIME_MULTIPLIER = 60.0;

	private static DeliveryAreaSet deliveryAreaSet;
	private static HashMap<DeliveryArea, Double> expectedArrivalsPerSubDeliveryArea;

	private static double expectedArrivals;
	private static TimeWindowSet timeWindowSet;
	private static TimeWindowSet timeWindowSetOverlappingDummy;
	public static HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping;
	public static HashMap<TimeWindow, ArrayList<TimeWindow>> newToOldTimeWindowMapping;
	protected static double maximumAdditionalCostPerOrder;
	protected static double averageAdditionalCostPerOrder;
	protected static double minimumAdditionalCostPerOrder;
	private static ArrayList<Routing> previousRoutingResults;
	private static double expectedServiceTime;
	private static Region region;
	private static Double maximumRevenueValue;
	private static HashMap<Entity, Object> objectiveSpecificValues;
	private static HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerDeliveryAreaAndTimeWindow;

	protected static HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	public static HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNoCopy;
	protected static HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> countAcceptableCombinationOverReferences;
	protected static HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> aggregatedReferenceInformationNoSumOverSubareas;
	protected static HashMap<DeliveryArea, HashMap<TimeWindow, Double>> aggregatedReferenceInformationCosts;
	protected static HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting;

	protected static HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;
	protected static HashMap<TimeWindow, Integer> maxAcceptedPerTw;
	protected static double maximumValueAcceptable;
	protected static int maximumAcceptableOverTw;
	protected static double avgOverallAccepted;
	protected static HashMap<TimeWindow, Double> avgAcceptedPerTw;
	protected static double averageValueAccepted;
	protected static HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw;

	private static boolean somethingChanged = true;
	private static boolean overlappingConsidered = false;
	private static boolean slackConsidered = false;

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	protected void aggregateReferenceInformation(boolean considerOverlapping, boolean considerSlack) {

		if (somethingChanged || (timeWindowSet.getOverlapping() && considerOverlapping && !overlappingConsidered)
				|| (timeWindowSet.getOverlapping() && !considerOverlapping && overlappingConsidered)
				|| (considerSlack && !slackConsidered)) {
			slackConsidered = considerSlack;
			if (timeWindowSet.getOverlapping() && considerOverlapping) {
				overlappingConsidered = true;
				timeWindowSetOverlappingDummy = new TimeWindowSet();
				timeWindowSetOverlappingDummy.setOverlapping(false);
				timeWindowSet.sortElementsAsc();
				oldToNewTimeWindowMapping = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
				newToOldTimeWindowMapping = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
				// Split time windows into blocks with clear assignment
				// First: Determine relevant time points
				ArrayList<Double> relevantTimePoints = new ArrayList<Double>();
				for (TimeWindow tw : timeWindowSet.getElements()) {
					relevantTimePoints.add(tw.getStartTime());
					relevantTimePoints.add(tw.getEndTime());
				}
				Collections.sort(relevantTimePoints);

				// Second: Create dummy time windows
				int currentId = -1;
				ArrayList<TimeWindow> dummyTws = new ArrayList<TimeWindow>();

				for (int i = 0; i < relevantTimePoints.size(); i++) {
					if (i == 0 || (!relevantTimePoints.get(i).equals(relevantTimePoints.get(i - 1))
							&& i != relevantTimePoints.size() - 1)) {
						TimeWindow tw = new TimeWindow();
						tw.setId(currentId--);
						tw.setStartTime(relevantTimePoints.get(i));
						boolean foundEnd = false;
						for (int j = i + 1; j < relevantTimePoints.size(); j++) {
							if (!relevantTimePoints.get(j).equals(relevantTimePoints.get(i))) {
								tw.setEndTime(relevantTimePoints.get(j));
								foundEnd = true;
								break;
							}
						}
						if (foundEnd) {
							dummyTws.add(tw);
							newToOldTimeWindowMapping.put(tw, new ArrayList<TimeWindow>());
						}

					}
				}

				timeWindowSetOverlappingDummy.setElements(dummyTws);
				timeWindowSetOverlappingDummy
						.setTempStartOfDeliveryPeriod(timeWindowSet.getTempStartOfDeliveryPeriod());
				// Define mapping
				for (TimeWindow tw : timeWindowSet.getElements()) {
					ArrayList<TimeWindow> mappedWindows = new ArrayList<TimeWindow>();
					for (TimeWindow newTw : dummyTws) {
						if (newTw.getStartTime() >= tw.getStartTime() && newTw.getEndTime() <= tw.getEndTime()) {
							mappedWindows.add(newTw);
							newToOldTimeWindowMapping.get(newTw).add(tw);
						}
					}
					oldToNewTimeWindowMapping.put(tw, mappedWindows);
				}

				neighborsTw = timeWindowSetOverlappingDummy.defineNeighborTimeWindows();
			} else {
				neighborsTw = timeWindowSet.defineNeighborTimeWindows();
				overlappingConsidered = false;
			}

			System.out.println("Aggregate reference information: " + System.currentTimeMillis());
			AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
			AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
			AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting = new HashMap<DeliveryArea, HashMap<Routing, Integer>>();
			AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();

			// TA: consider that maximal acceptable costs is initialised too low
			AggregateReferenceInformationAlgorithm.maximalAcceptableCostsPerDeliveryArea = new HashMap<Integer, Double>();
			for (DeliveryArea area : deliveryAreaSet.getElements()) {

				AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.put(area,
						new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
				AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.put(area,
						new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
				AggregateReferenceInformationAlgorithm.maximalAcceptableCostsPerDeliveryArea.put(area.getId(), 0.0);
			}

			// Prepare information from TOPS
			AggregateReferenceInformationAlgorithm.maxAcceptedPerTw = new HashMap<TimeWindow, Integer>();
			AggregateReferenceInformationAlgorithm.avgAcceptedPerTw = new HashMap<TimeWindow, Double>();
			AggregateReferenceInformationAlgorithm.averageValueAccepted = 0.0;
			double maximumValueAccepted = 0.0;
			int maximumAmountAccepted = 0;
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting = new HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>>();
			int maxDeletedPerRouting = 0;
			int minDeletedPerRouting = Integer.MAX_VALUE;
			double minimumSlack = 0.0;
			if (considerSlack) {
				// Remove elements for more flexibility
				minimumSlack = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLat1(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLon1(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLat2(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLon2())
						* AggregateReferenceInformationAlgorithm.distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			}
			for (Routing routing : previousRoutingResults) {
				int deleted = 0;
				if (considerSlack) {
					bufferFirstAndLastRePerRouting.put(routing,
							new HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>());
				}
				HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
				HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> countAdditionalOverlapping = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
				HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> distance = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>();
				HashMap<TimeWindow, Integer> acceptedPerTw = new HashMap<TimeWindow, Integer>();
				double valueAccepted = 0.0;
				int amountAccepted = 0;
				for (Route r : routing.getRoutes()) {
					if (considerSlack) {
						bufferFirstAndLastRePerRouting.get(routing).put(r,
								new HashMap<TimeWindow, ArrayList<RouteElement>>());
					}
					DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
							r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
					if (!count.containsKey(area)) {
						count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
						distance.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>());
						countAdditionalOverlapping.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
					}
					for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
						RouteElement re = r.getRouteElements().get(reId);
						amountAccepted++;
						re.setPosition(reId + 1);

						TimeWindow relevantTw = re.getOrder().getTimeWindowFinal();

						// If time window set is overlapping, assign to new time
						// window
						if (timeWindowSet.getOverlapping() && considerOverlapping) {
							boolean foundAssignment = false;
							for (TimeWindow tw2 : oldToNewTimeWindowMapping.get(re.getOrder().getTimeWindowFinal())) {

								if (re.getServiceBegin() >= (tw2.getStartTime()
										- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
										&& re.getServiceBegin() < (tw2.getEndTime()
												- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER) {

									relevantTw = tw2;
									re.setTimeWindow(relevantTw);
									foundAssignment = true;
									break;
								}
							}
							if (!foundAssignment)
								System.out.println("Assignment does not work");

						}

						// Buffer first and last re per time window
						if (considerSlack
								&& !bufferFirstAndLastRePerRouting.get(routing).get(r).containsKey(relevantTw)) {
							ArrayList<RouteElement> res = new ArrayList<RouteElement>();
							res.add(re);
							bufferFirstAndLastRePerRouting.get(routing).get(r).put(relevantTw, res);

						} else if (considerSlack
								&& bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).size() < 2) {
							bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).add(1, re);
						} else if (considerSlack) {
							bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).remove(1);
							bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).add(1, re);
						}
						if (!acceptedPerTw.containsKey(relevantTw)) {
							acceptedPerTw.put(relevantTw, 1);
						} else {
							acceptedPerTw.put(relevantTw, acceptedPerTw.get(relevantTw) + 1);
						}
						valueAccepted += CustomerDemandService.calculateOrderValue(re.getOrder().getOrderRequest(),
								maximumRevenueValue, objectiveSpecificValues);
						double travelTimeFrom;
						double travelTimeTo = re.getTravelTime();
						double latAfter;
						double lonAfter;
						boolean depotBefore = false;
						boolean depotAfter = false;
						if (reId < r.getRouteElements().size() - 1) {
							travelTimeFrom = r.getRouteElements().get(reId + 1).getTravelTime();
							latAfter = r.getRouteElements().get(reId + 1).getOrder().getOrderRequest().getCustomer()
									.getLat();
							lonAfter = r.getRouteElements().get(reId + 1).getOrder().getOrderRequest().getCustomer()
									.getLon();
						} else {
							travelTimeFrom = 0.0;
							depotAfter = true;
							latAfter = r.getVehicleAssignment().getEndingLocationLat();
							lonAfter = r.getVehicleAssignment().getEndingLocationLon();
						}

						double latBefore;
						double lonBefore;

						if (reId > 0) {
							latBefore = r.getRouteElements().get(reId - 1).getOrder().getOrderRequest().getCustomer()
									.getLat();
							lonBefore = r.getRouteElements().get(reId - 1).getOrder().getOrderRequest().getCustomer()
									.getLon();
						} else {
							depotBefore = true;
							latBefore = r.getVehicleAssignment().getStartingLocationLat();
							lonBefore = r.getVehicleAssignment().getStartingLocationLon();
							travelTimeTo = 0;
						}
						double travelTimeAlternative;

						if (depotBefore || depotAfter) {
							travelTimeAlternative = 0.0;
						} else {
							travelTimeAlternative = LocationService
									.calculateHaversineDistanceBetweenGPSPointsInKilometer(latBefore, lonBefore,
											latAfter, lonAfter)
									* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * TIME_MULTIPLIER;
						}

						double additionalCost = travelTimeTo + travelTimeFrom + expectedServiceTime
								- travelTimeAlternative;
						re.setTempAdditionalCostsValue(additionalCost);
						Customer cus = re.getOrder().getOrderRequest().getCustomer();
						DeliveryArea subArea = LocationService
								.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
						cus.setTempDeliveryArea(subArea);
						if (!count.get(area).containsKey(subArea)) {
							count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
							distance.get(area).put(subArea, new HashMap<TimeWindow, Double>());
							countAdditionalOverlapping.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
						}
						if (!AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(area)
								.containsKey(subArea)) {
							AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(area)
									.put(subArea, new HashMap<TimeWindow, Integer>());
						}

						// Check all time window neighbors for fit

						if (!count.get(area).get(subArea).containsKey(relevantTw)) {
							count.get(area).get(subArea).put(relevantTw, 1);
						} else {
							count.get(area).get(subArea).put(relevantTw,
									count.get(area).get(subArea).get(relevantTw) + 1);

						}

						if (!distance.get(area).get(subArea).containsKey(relevantTw)) {
							distance.get(area).get(subArea).put(relevantTw, additionalCost);
						} else {
							distance.get(area).get(subArea).put(relevantTw,
									distance.get(area).get(subArea).get(relevantTw) + additionalCost);
						}

						if (!AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(area)
								.get(subArea).containsKey(relevantTw)) {
							AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(area)
									.get(subArea).put(relevantTw, 0);
						}

						// If the service begin also fits into another time
						// window, count it for average distance
						for (TimeWindow neihborTw : neighborsTw.get(relevantTw)) {
							if (re.getServiceBegin() >= (neihborTw.getStartTime()
									- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
									&& re.getServiceBegin() < (neihborTw.getEndTime()
											- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER) {
								if (!distance.get(area).get(subArea).containsKey(neihborTw)) {
									distance.get(area).get(subArea).put(neihborTw, additionalCost);
								} else {
									distance.get(area).get(subArea).put(neihborTw,
											distance.get(area).get(subArea).get(neihborTw) + additionalCost);
								}

								if (!countAdditionalOverlapping.get(area).get(subArea).containsKey(neihborTw)) {
									countAdditionalOverlapping.get(area).get(subArea).put(neihborTw, 1);
								} else {
									countAdditionalOverlapping.get(area).get(subArea).put(neihborTw,
											countAdditionalOverlapping.get(area).get(subArea).get(neihborTw) + 1);
								}

								if (!AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences
										.get(area).get(subArea).containsKey(neihborTw)) {
									AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences
											.get(area).get(subArea).put(neihborTw, 0);
								}
							}
						}

					}
				}

				AggregateReferenceInformationAlgorithm.averageValueAccepted += valueAccepted;
				if (maximumValueAccepted < valueAccepted)
					maximumValueAccepted = valueAccepted;

				if (maximumAmountAccepted < amountAccepted)
					maximumAmountAccepted = amountAccepted;

				for (DeliveryArea a : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo
						.keySet()) {

					if (!AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting.containsKey(a))
						AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting.put(a,
								new HashMap<Routing, Integer>());

					AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting.get(a).put(routing, 0);
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(a).put(routing,
							count.get(a));

					double distanceSum = 0.0;
					for (DeliveryArea area : distance.get(a).keySet()) {
						for (TimeWindow tw : distance.get(a).get(area).keySet()) {

							// Calculate divisor for distance
							double divisor = 0.0;
							if (count.get(a).get(area).containsKey(tw)) {
								AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting.get(a).put(
										routing,
										AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting
												.get(a).get(routing) + count.get(a).get(area).get(tw));
								divisor = count.get(a).get(area).get(tw);
							}

							if (countAdditionalOverlapping.get(a).get(area).containsKey(tw)) {
								divisor = divisor + countAdditionalOverlapping.get(a).get(area).get(tw);
							}
							distance.get(a).get(area).put(tw, distance.get(a).get(area).get(tw) / divisor);
							if (!AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
									.containsKey(area)) {
								AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.put(area,
										new HashMap<TimeWindow, Double>());
							}
							if (!AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area)
									.containsKey(tw)) {
								AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area)
										.put(tw, distance.get(a).get(area).get(tw));
							} else {
								AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area)
										.put(tw, AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
												.get(area).get(tw) + distance.get(a).get(area).get(tw));
							}
							distanceSum += distance.get(a).get(area).get(tw);
							AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(a)
									.get(area)
									.put(tw, AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences
											.get(a).get(area).get(tw) + 1);
						}
					}

					if (AggregateReferenceInformationAlgorithm.maximalAcceptableCostsPerDeliveryArea
							.get(a.getId()) < distanceSum) {
						AggregateReferenceInformationAlgorithm.maximalAcceptableCostsPerDeliveryArea.put(a.getId(),
								distanceSum);
					}
				}
				// Put slack diminishing here, also adapt acceptedPerTw
				if (considerSlack) {
					for (Route r : bufferFirstAndLastRePerRouting.get(routing).keySet()) {

						ArrayList<RouteElement> routeCopy = new ArrayList<RouteElement>();
						for (RouteElement re : r.getRouteElements()) {
							routeCopy.add(re.copyElement());
						}
						ArrayList<TimeWindow> alreadyDeleted = new ArrayList<TimeWindow>();

						if (timeWindowSet.getOverlapping() && considerOverlapping) {

							deleted += AggregateReferenceInformationAlgorithm.deleteSimpleSlackBasedOverlapping(routing,
									r, routeCopy, aggregatedReferenceInformationNo, acceptablePerDeliveryAreaAndRouting,
									bufferFirstAndLastRePerRouting, minimumSlack, true, null, alreadyDeleted,
									timeWindowSetOverlappingDummy, timeWindowSet, expectedArrivals, expectedServiceTime,
									expectedArrivalsPerSubDeliveryArea, valueMultiplierPerDeliveryAreaAndTimeWindow,
									region,
									r.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer()
											.getTempDeliveryArea().getDeliveryAreaOfSet(),
									newToOldTimeWindowMapping, oldToNewTimeWindowMapping);
						} else {
							deleted += deleteSimpleSlackBased(routing, r, routeCopy, aggregatedReferenceInformationNo,
									acceptablePerDeliveryAreaAndRouting, bufferFirstAndLastRePerRouting, minimumSlack,
									considerOverlapping, acceptedPerTw, alreadyDeleted, timeWindowSet, expectedArrivals,
									expectedServiceTime, expectedArrivalsPerSubDeliveryArea,
									valueMultiplierPerDeliveryAreaAndTimeWindow, region,
									r.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer()
											.getTempDeliveryArea().getDeliveryAreaOfSet());
						}


						double lowestSlack = Double.MAX_VALUE;
						TimeWindow lowTw = null;
						for (RouteElement re : routeCopy) {
							if (re.getSlack() < lowestSlack) {
								lowestSlack = re.getSlack();
								lowTw = re.getOrder().getTimeWindowFinal();
							}
						}
						if (lowestSlack < minimumSlack)
							System.out.println("lowestSlack" + lowestSlack);
					}
					if (deleted < minDeletedPerRouting) {
						minDeletedPerRouting = deleted;
					}
					if (deleted > maxDeletedPerRouting) {
						maxDeletedPerRouting = deleted;
					}

				}

				for (TimeWindow tw : acceptedPerTw.keySet()) {
					if (!AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.containsKey(tw)) {
						AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.put(tw, 0.0);
						AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.put(tw, 0);
					}
					AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.put(tw,
							AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.get(tw) + acceptedPerTw.get(tw));
					if (acceptedPerTw.get(tw) > AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.get(tw)) {
						AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.put(tw, acceptedPerTw.get(tw));
					}
				}

			}

			AggregateReferenceInformationAlgorithm.maximumAdditionalCostPerOrder = 0.0;
			AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder = 0.0;
			int counter = 0;
			AggregateReferenceInformationAlgorithm.minimumAdditionalCostPerOrder = Double.MAX_VALUE;
			for (DeliveryArea area : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
					.keySet()) {
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
						.get(area).keySet()) {
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area).put(tw,
							AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area).get(tw)
									/ countAcceptableCombinationOverReferences.get(area.getDeliveryAreaOfSet())
											.get(area).get(tw));
					AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder += AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
							.get(area).get(tw);
					if (AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area)
							.get(tw) > AggregateReferenceInformationAlgorithm.maximumAdditionalCostPerOrder)
						AggregateReferenceInformationAlgorithm.maximumAdditionalCostPerOrder = AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
								.get(area).get(tw);
					if (AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(area)
							.get(tw) < AggregateReferenceInformationAlgorithm.minimumAdditionalCostPerOrder)
						AggregateReferenceInformationAlgorithm.minimumAdditionalCostPerOrder = AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
								.get(area).get(tw);
					counter++;
				}
			}

			AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder = AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder
					/ (double) counter;

			AggregateReferenceInformationAlgorithm.averageValueAccepted = AggregateReferenceInformationAlgorithm.averageValueAccepted
					/ previousRoutingResults.size() / maximumValueAccepted;
			AggregateReferenceInformationAlgorithm.maximumValueAcceptable = maximumValueAccepted;
			AggregateReferenceInformationAlgorithm.avgOverallAccepted = 0.0;
			AggregateReferenceInformationAlgorithm.maximumAcceptableOverTw = maximumAmountAccepted;
			ArrayList<TimeWindow> timeWindows = timeWindowSet.getElements();
			if (timeWindowSet.getOverlapping() && considerOverlapping) {
				timeWindows = timeWindowSetOverlappingDummy.getElements();
			}
			for (TimeWindow tw : timeWindows) {

				if (AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.containsKey(tw)) {
					AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.put(tw,
							AggregateReferenceInformationAlgorithm.avgAcceptedPerTw.get(tw)
									/ (double) previousRoutingResults.size());
					AggregateReferenceInformationAlgorithm.avgOverallAccepted += AggregateReferenceInformationAlgorithm.avgAcceptedPerTw
							.get(tw);
				}
				if (!AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.containsKey(tw)) {
					AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.put(tw, 1);
				}

			}

			System.out.println("Aggregated reference information (finish): " + System.currentTimeMillis());
			aggregatedReferenceInformationNoCopy = aggregatedReferenceInformationNo;
			somethingChanged = false;
		}

		// Copy no because it could be alternated
		AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoCopy.keySet()) {
			AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
			for (Routing r : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoCopy.get(area)
					.keySet()) {
				AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).put(r,
						new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
				for (DeliveryArea subArea : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoCopy
						.get(area).get(r).keySet()) {
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).get(r)
							.put(subArea, new HashMap<TimeWindow, Integer>());
					for (TimeWindow tw : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoCopy
							.get(area).get(r).get(subArea).keySet()) {
						AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).get(r)
								.get(subArea)
								.put(tw, AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoCopy
										.get(area).get(r).get(subArea).get(tw));

					}
				}
			}
		}

	}

	public static double getDistanceMultiplierAsTheCrowFlies() {
		return distanceMultiplierAsTheCrowFlies;
	}

	public static void setDistanceMultiplierAsTheCrowFlies(double distanceMultiplierAsTheCrowFlies) {
		if (distanceMultiplierAsTheCrowFlies != AggregateReferenceInformationAlgorithm.distanceMultiplierAsTheCrowFlies)
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.distanceMultiplierAsTheCrowFlies = distanceMultiplierAsTheCrowFlies;
	}

	public static double getTIME_MULTIPLIER() {
		return TIME_MULTIPLIER;
	}

	public static void setTIME_MULTIPLIER(double tIME_MULTIPLIER) {
		if (tIME_MULTIPLIER != AggregateReferenceInformationAlgorithm.TIME_MULTIPLIER)
			somethingChanged = true;
		TIME_MULTIPLIER = tIME_MULTIPLIER;
	}

	public static DeliveryAreaSet getDeliveryAreaSet() {
		return deliveryAreaSet;
	}

	public static void setDeliveryAreaSet(DeliveryAreaSet deliveryAreaSet) {
		if (AggregateReferenceInformationAlgorithm.deliveryAreaSet == null
				|| AggregateReferenceInformationAlgorithm.deliveryAreaSet.getId() != deliveryAreaSet.getId())
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.deliveryAreaSet = deliveryAreaSet;
	}

	public static TimeWindowSet getTimeWindowSet() {
		return timeWindowSet;
	}

	public static void setTimeWindowSet(TimeWindowSet timeWindowSet) {
		if (AggregateReferenceInformationAlgorithm.timeWindowSet == null
				|| AggregateReferenceInformationAlgorithm.timeWindowSet.getId() != timeWindowSet.getId())
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.timeWindowSet = timeWindowSet;
	}

	public static double getMaximumAdditionalCostPerOrder() {
		return maximumAdditionalCostPerOrder;
	}

	public static void setMaximumAdditionalCostPerOrder(double maximumAdditionalCostPerOrder) {
		AggregateReferenceInformationAlgorithm.maximumAdditionalCostPerOrder = maximumAdditionalCostPerOrder;
	}

	public static double getAverageAdditionalCostPerOrder() {
		return averageAdditionalCostPerOrder;
	}

	public static void setAverageAdditionalCostPerOrder(double averageAdditionalCostPerOrder) {
		AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder = averageAdditionalCostPerOrder;
	}

	public static double getMinimumAdditionalCostPerOrder() {
		return minimumAdditionalCostPerOrder;
	}

	public static void setMinimumAdditionalCostPerOrder(double minimumAdditionalCostPerOrder) {
		AggregateReferenceInformationAlgorithm.minimumAdditionalCostPerOrder = minimumAdditionalCostPerOrder;
	}

	public static ArrayList<Routing> getPreviousRoutingResults() {

		return previousRoutingResults;
	}

	public static void setPreviousRoutingResults(ArrayList<Routing> previousRoutingResults) {
		if (AggregateReferenceInformationAlgorithm.previousRoutingResults == null
				|| AggregateReferenceInformationAlgorithm.previousRoutingResults.get(0)
						.getId() != previousRoutingResults.get(0).getId()
								|| !AggregateReferenceInformationAlgorithm.previousRoutingResults.get(0).getName().equals(previousRoutingResults.get(0).getName())
				|| AggregateReferenceInformationAlgorithm.previousRoutingResults.size() != previousRoutingResults
						.size()) {
			somethingChanged = true;
			AggregateReferenceInformationAlgorithm.previousRoutingResults = previousRoutingResults;
		}
		
	}

	public double getExpectedServiceTime() {
		return expectedServiceTime;
	}

	public static void setExpectedServiceTime(double expectedServiceTime) {
		if (AggregateReferenceInformationAlgorithm.expectedServiceTime != expectedServiceTime)
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.expectedServiceTime = expectedServiceTime;
	}

	public static Region getRegion() {
		return region;
	}

	public static void setRegion(Region region) {
		if (AggregateReferenceInformationAlgorithm.region == null
				|| AggregateReferenceInformationAlgorithm.region.getId() != region.getId())
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.region = region;
	}

	public static Double getMaximumRevenueValue() {
		return maximumRevenueValue;
	}

	public static void setMaximumRevenueValue(Double maximumRevenueValue) {
		if (AggregateReferenceInformationAlgorithm.maximumRevenueValue == null
				|| !AggregateReferenceInformationAlgorithm.maximumRevenueValue.equals(maximumRevenueValue))
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.maximumRevenueValue = maximumRevenueValue;
	}

	public static HashMap<Entity, Object> getObjectiveSpecificValues() {
		return objectiveSpecificValues;
	}

	public static void setObjectiveSpecificValues(HashMap<Entity, Object> objectiveSpecificValues) {
		if (AggregateReferenceInformationAlgorithm.objectiveSpecificValues == null) {
			somethingChanged = true;
		} else {
			if (!AggregateReferenceInformationAlgorithm.objectiveSpecificValues.equals(objectiveSpecificValues))
				somethingChanged = true;
		}

		AggregateReferenceInformationAlgorithm.objectiveSpecificValues = objectiveSpecificValues;
	}

	public static HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> getAggregatedReferenceInformationNo() {
		return aggregatedReferenceInformationNo;
	}

	public static void setAggregatedReferenceInformationNo(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo) {
		AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo = aggregatedReferenceInformationNo;
	}

	public static HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> getCountAcceptableCombinationOverReferences() {
		return countAcceptableCombinationOverReferences;
	}

	public static void setCountAcceptableCombinationOverReferences(
			HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> countAcceptableCombinationOverReferences) {
		AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences = countAcceptableCombinationOverReferences;
	}

	public static HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> getAggregatedReferenceInformationNoSumOverSubareas() {
		return aggregatedReferenceInformationNoSumOverSubareas;
	}

	public static void setAggregatedReferenceInformationNoSumOverSubareas(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> aggregatedReferenceInformationNoSumOverSubareas) {
		AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNoSumOverSubareas = aggregatedReferenceInformationNoSumOverSubareas;
	}

	public static HashMap<DeliveryArea, HashMap<TimeWindow, Double>> getAggregatedReferenceInformationCosts() {
		return aggregatedReferenceInformationCosts;
	}

	public static void setAggregatedReferenceInformationCosts(
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> aggregatedReferenceInformationCosts) {
		AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts = aggregatedReferenceInformationCosts;
	}

	public static HashMap<DeliveryArea, HashMap<Routing, Integer>> getAcceptablePerDeliveryAreaAndRouting() {
		return acceptablePerDeliveryAreaAndRouting;
	}

	public static void setAcceptablePerDeliveryAreaAndRouting(
			HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting) {
		AggregateReferenceInformationAlgorithm.acceptablePerDeliveryAreaAndRouting = acceptablePerDeliveryAreaAndRouting;
	}

	public static HashMap<Integer, Double> getMaximalAcceptableCostsPerDeliveryArea() {
		return maximalAcceptableCostsPerDeliveryArea;
	}

	public static void setMaximalAcceptableCostsPerDeliveryArea(
			HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea) {
		AggregateReferenceInformationAlgorithm.maximalAcceptableCostsPerDeliveryArea = maximalAcceptableCostsPerDeliveryArea;
	}

	public static HashMap<TimeWindow, Integer> getMaxAcceptedPerTw() {
		return maxAcceptedPerTw;
	}

	public static void setMaxAcceptedPerTw(HashMap<TimeWindow, Integer> maxAcceptedPerTw) {
		AggregateReferenceInformationAlgorithm.maxAcceptedPerTw = maxAcceptedPerTw;
	}

	public static double getMaximumValueAcceptable() {
		return maximumValueAcceptable;
	}

	public static void setMaximumValueAcceptable(double maximumValueAcceptable) {
		AggregateReferenceInformationAlgorithm.maximumValueAcceptable = maximumValueAcceptable;
	}

	public static int getMaximumAcceptableOverTw() {
		return maximumAcceptableOverTw;
	}

	public static void setMaximumAcceptableOverTw(int maximumAcceptableOverTw) {
		AggregateReferenceInformationAlgorithm.maximumAcceptableOverTw = maximumAcceptableOverTw;
	}

	public static double getAvgOverallAccepted() {
		return avgOverallAccepted;
	}

	public static void setAvgOverallAccepted(double avgOverallAccepted) {
		AggregateReferenceInformationAlgorithm.avgOverallAccepted = avgOverallAccepted;
	}

	public static HashMap<TimeWindow, Double> getAvgAcceptedPerTw() {
		return avgAcceptedPerTw;
	}

	public static void setAvgAcceptedPerTw(HashMap<TimeWindow, Double> avgAcceptedPerTw) {
		AggregateReferenceInformationAlgorithm.avgAcceptedPerTw = avgAcceptedPerTw;
	}

	public static double getAverageValueAccepted() {
		return averageValueAccepted;
	}

	public static void setAverageValueAccepted(double averageValueAccepted) {
		AggregateReferenceInformationAlgorithm.averageValueAccepted = averageValueAccepted;
	}

	public boolean isSomethingChanged() {
		return somethingChanged;
	}

	public void setSomethingChanged(boolean somethingChanged) {
		AggregateReferenceInformationAlgorithm.somethingChanged = somethingChanged;
	}

	public static TimeWindowSet getTimeWindowSetOverlappingDummy() {
		return timeWindowSetOverlappingDummy;
	}

	public static void setTimeWindowSetOverlappingDummy(TimeWindowSet timeWindowSetOverlappingDummy) {
		AggregateReferenceInformationAlgorithm.timeWindowSetOverlappingDummy = timeWindowSetOverlappingDummy;
	}

	public static void updateRoute(ArrayList<RouteElement> route, int checkFromHere, TimeWindowSet timeWindowSet,
			double depotLat1, double depotLon1, double depotLat2, double depotLon2, Region region,
			double timeMultiplier) {
		// Forward: Update waiting time and service time begin

		/// Calculate travel time from first unchanged to the first that
		/// needs to be shifted forward.
		double travelTimeTo;
		if (checkFromHere == 0) {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(depotLat1, depotLon1,
					route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLat(),
					route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLon())
					* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			route.get(checkFromHere).setTravelTime(travelTimeTo);
			route.get(checkFromHere).setTravelTimeTo(travelTimeTo);

		} else if (checkFromHere == route.size()) {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
					route.get(checkFromHere - 1).getOrder().getOrderRequest().getCustomer().getLat(),
					route.get(checkFromHere - 1).getOrder().getOrderRequest().getCustomer().getLon(), depotLat2,
					depotLon2) * distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			route.get(checkFromHere - 1).setTravelTimeFrom(travelTimeTo);
			route.get(checkFromHere - 1).setTravelTimeTo(route.get(checkFromHere - 1).getTravelTime());
		} else {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					route.get(checkFromHere - 1).getOrder().getOrderRequest().getCustomer(),
					route.get(checkFromHere).getOrder().getOrderRequest().getCustomer())
					* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			route.get(checkFromHere - 1).setTravelTimeFrom(travelTimeTo);
			route.get(checkFromHere).setTravelTime(travelTimeTo);
			route.get(checkFromHere).setTravelTimeTo(travelTimeTo);
		}

		if (checkFromHere < route.size() - 1) {
			route.get(checkFromHere).setTravelTimeFrom(route.get(checkFromHere + 1).getTravelTime());
		} else if (checkFromHere == route.size() - 1) {
			route.get(checkFromHere)
					.setTravelTimeFrom(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
							route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLat(),
							route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLon(), depotLat2,
							depotLon2) * distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour()
							* timeMultiplier);
		}

		double newAdditionalCost;
		double lat1;
		double lon1;
		double lat2;
		double lon2;
		if (checkFromHere > 0) {
			if (checkFromHere > 1) {
				lat1 = route.get(checkFromHere - 2).getOrder().getOrderRequest().getCustomer().getLat();
				lon1 = route.get(checkFromHere - 2).getOrder().getOrderRequest().getCustomer().getLon();
			} else {
				lat1 = depotLat1;
				lon1 = depotLon1;
			}

			if (checkFromHere < route.size()) {
				lat2 = route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLat();
				lon2 = route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLon();
			} else {
				lat2 = depotLat2;
				lon2 = depotLon2;
			}
			newAdditionalCost = route.get(checkFromHere - 1).getTravelTime()
					+ route.get(checkFromHere - 1).getTravelTimeFrom() + route.get(checkFromHere - 1).getServiceTime()
					- LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(lat1, lon1, lat2, lon2)
							* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			route.get(checkFromHere - 1).setTempAdditionalCostsValue(newAdditionalCost);
		}

		if (checkFromHere <= route.size() - 1) {

			if (checkFromHere > 0) {
				lat1 = route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLat();
				lon1 = route.get(checkFromHere).getOrder().getOrderRequest().getCustomer().getLon();
			} else {
				lat1 = depotLat1;
				lon1 = depotLon1;
			}
			if (checkFromHere == route.size() - 1) {
				lat2 = depotLat2;
				lon2 = depotLon2;
			} else {
				lat2 = route.get(checkFromHere + 1).getOrder().getOrderRequest().getCustomer().getLat();
				lon2 = route.get(checkFromHere + 1).getOrder().getOrderRequest().getCustomer().getLon();
			}

			newAdditionalCost = route.get(checkFromHere).getTravelTime() + route.get(checkFromHere).getTravelTimeFrom()
					+ route.get(checkFromHere).getServiceTime()
					- LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(lat1, lon1, lat2, lon2)
							* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			route.get(checkFromHere).setTempAdditionalCostsValue(newAdditionalCost);
		}
		for (int i = checkFromHere; i < route.size(); i++) {

			RouteElement eNew = route.get(i);
			eNew.setPosition(eNew.getPosition() - 1);
			// Update service begin and waiting time

			double arrivalTime;
			// If it is the first element, the service can begin at the
			// start of the time window
			if (i == 0) {
				arrivalTime = 0.0;
			} else {
				RouteElement eBefore = route.get(i - 1);
				arrivalTime = eBefore.getServiceBegin() + eBefore.getServiceTime() + eNew.getTravelTime();
			}
			double newServiceBegin;

			newServiceBegin = Math.max(arrivalTime,
					(eNew.getOrder().getTimeWindowFinal().getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier);

			eNew.setServiceBegin(newServiceBegin);
			eNew.setServiceEnd(newServiceBegin + eNew.getServiceTime());
			eNew.setWaitingTime(newServiceBegin - arrivalTime);
		}

		/// For end depot
		route.get(route.size() - 1)
				.setSlack((route.get(route.size() - 1).getOrder().getTimeWindowFinal().getEndTime()
						- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
						- route.get(route.size() - 1).getServiceBegin());

		/// For others
		for (int i = route.size() - 2; i >= 0; i--) {
			Double maxShift = Math.min(
					(route.get(i).getOrder().getTimeWindowFinal().getEndTime()
							- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
							- route.get(i).getServiceBegin(),
					route.get(i + 1).getWaitingTime() + route.get(i + 1).getSlack());
			route.get(i).setSlack(maxShift);
			if (route.get(i).getSlack() < 0)
				System.out.println("Strange");
		}

	}

	public static HashMap<DeliveryArea, Double> getExpectedArrivalsPerSubDeliveryArea() {
		return expectedArrivalsPerSubDeliveryArea;
	}

	public static void setExpectedArrivalsPerSubDeliveryArea(
			HashMap<DeliveryArea, Double> expectedArrivalsPerSubDeliveryArea) {
		if (AggregateReferenceInformationAlgorithm.expectedArrivalsPerSubDeliveryArea == null
				|| !AggregateReferenceInformationAlgorithm.expectedArrivalsPerSubDeliveryArea
						.equals(expectedArrivalsPerSubDeliveryArea))
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.expectedArrivalsPerSubDeliveryArea = expectedArrivalsPerSubDeliveryArea;
	}

	public static double getExpectedArrivals() {
		return expectedArrivals;
	}

	public static void setExpectedArrivals(double expectedArrivals) {
		if (!(AggregateReferenceInformationAlgorithm.expectedArrivals == expectedArrivals))
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.expectedArrivals = expectedArrivals;
	}

	public static HashMap<Integer, HashMap<Integer, Double>> getValueMultiplierPerDeliveryAreaAndTimeWindow() {
		return valueMultiplierPerDeliveryAreaAndTimeWindow;
	}

	public static void setValueMultiplierPerDeliveryAreaAndTimeWindow(
			HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerDeliveryAreaAndTimeWindow) {
		if (AggregateReferenceInformationAlgorithm.valueMultiplierPerDeliveryAreaAndTimeWindow == null
				|| !AggregateReferenceInformationAlgorithm.valueMultiplierPerDeliveryAreaAndTimeWindow
						.equals(valueMultiplierPerDeliveryAreaAndTimeWindow))
			somethingChanged = true;
		AggregateReferenceInformationAlgorithm.valueMultiplierPerDeliveryAreaAndTimeWindow = valueMultiplierPerDeliveryAreaAndTimeWindow;
	}

	private static Pair<TimeWindow, TimeWindow> determineToDeleteOverlapping(boolean first, Routing routing, Route r,
			ArrayList<RouteElement> routeCopy,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			ArrayList<TimeWindow> alreadyDeleted, double minimumSlack, TimeWindowSet timeWindowSet,
			TimeWindowSet originalTimeWindowSet, double expectedServiceTime,
			HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow,
			DeliveryArea areaHigh, HashMap<TimeWindow, ArrayList<TimeWindow>> newToOldTimeWindowMapping,
			HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping) {

		// Determine maximum slack per sub-tw
		HashMap<TimeWindow, Double> slackPerSubTw = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, ArrayList<TimeWindow>> twsWithOrderPerSubTw = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
		for (TimeWindow subTw : bufferFirstAndLastRePerRouting.get(routing).get(r).keySet()) {

			if (first) {
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).add(routeCopy
						.get(bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).get(0).getPosition() - 1));
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).remove(0);
				if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).size() > 1) {

					bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).add(routeCopy.get(
							bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).get(0).getPosition() - 1));
					bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).remove(0);
				}

			}

			if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).size() > 1) {
				slackPerSubTw.put(subTw, 0.0);
				twsWithOrderPerSubTw.put(subTw, new ArrayList<TimeWindow>());

				for (int cReId = bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).get(0).getPosition()
						- 1; cReId < bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).get(1)
								.getPosition(); cReId++) {

					RouteElement re = routeCopy.get(cReId);
					if (re.getSlack() > slackPerSubTw.get(subTw))
						slackPerSubTw.put(subTw, re.getSlack());
					if (!twsWithOrderPerSubTw.get(subTw).contains(re.getOrder().getTimeWindowFinal()))
						twsWithOrderPerSubTw.get(subTw).add(re.getOrder().getTimeWindowFinal());
				}
			}
		}

		// Find time window with lowest maximum slack over subtws
		double lowestMaximumSlack = Double.MAX_VALUE;
		TimeWindow twSlack = null;
		HashMap<TimeWindow, Double> slackPerTw = new HashMap<TimeWindow, Double>();
		originalTimeWindowSet.sortElementsByEndAsc();
		for (TimeWindow tw : originalTimeWindowSet.getElements()) {
			if (!alreadyDeleted.contains(tw)) {
				double maximumSlackForTw = -Double.MAX_VALUE;
				boolean possibleDeletion = true;
				for (TimeWindow subTw : oldToNewTimeWindowMapping.get(tw)) {
					if (bufferFirstAndLastRePerRouting.get(routing).get(r).containsKey(subTw)
							&& bufferFirstAndLastRePerRouting.get(routing).get(r).get(subTw).size() > 1) {
						if (slackPerSubTw.get(subTw) > maximumSlackForTw) {
							maximumSlackForTw = slackPerSubTw.get(subTw);
						}
					} else {
						possibleDeletion = false;
						break;
					}
				}

				if (possibleDeletion)
					slackPerTw.put(tw, maximumSlackForTw);
				if (possibleDeletion && maximumSlackForTw <= lowestMaximumSlack) {
					lowestMaximumSlack = maximumSlackForTw;
					twSlack = tw;
				}
			}
		}

		// More deletions needed?
		if (twSlack == null || lowestMaximumSlack >= minimumSlack)
			return null;

		// From which subtw to delete? Which tw actually to delete? Criteria:
		// Have to delete from that tw anyway? Which is less valuable?
		double bestDeletionValue = -Double.MAX_VALUE;
		TimeWindow bestDeletionSubTw = null;
		TimeWindow bestDeletionTw = null;
		for (TimeWindow subTw : oldToNewTimeWindowMapping.get(twSlack)) {
			double valueOmitted = 0.0;
			double valueDeleted = Double.MAX_VALUE;
			TimeWindow deletionTw = null;

			// Go through all effected time windows
			for (TimeWindow tw : newToOldTimeWindowMapping.get(subTw)) {

				if (!alreadyDeleted.contains(tw) && twsWithOrderPerSubTw.get(subTw).contains(tw)) {

					// How much value to loose when deleting this tw?
					if (maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
							.get(tw.getId()) <= valueDeleted) {
						valueDeleted = maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
								.get(tw.getId());
						deletionTw = tw;
					}
					;

					// Deletion necessary? What deletion to prevent?
					if (slackPerTw.containsKey(tw) && slackPerTw.get(tw) < minimumSlack) {
						valueOmitted += maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
								.get(tw.getId());
					}

				}
			}

			if (bestDeletionValue <= valueOmitted - valueDeleted && valueOmitted - valueDeleted > -Double.MAX_VALUE) {
				bestDeletionValue = valueOmitted - valueDeleted;
				bestDeletionSubTw = subTw;
				bestDeletionTw = deletionTw;
			}
		}

		alreadyDeleted.addAll(newToOldTimeWindowMapping.get(bestDeletionSubTw));
		return new Pair<TimeWindow, TimeWindow>(bestDeletionSubTw, bestDeletionTw);

	}

	private static TimeWindow determineToDelete(boolean first, Routing routing, Route r,
			ArrayList<RouteElement> routeCopy,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			ArrayList<TimeWindow> alreadyDeleted, double minimumSlack, TimeWindowSet timeWindowSet,
			double expectedServiceTime,
			HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow,
			DeliveryArea areaHigh) {
		double highestValuePrevention = -Double.MAX_VALUE;
		double slack = Double.MAX_VALUE;
		TimeWindow twSlack = null;
		// Determine lowest free slack

		timeWindowSet.sortElementsAsc();
		for (int i = 0; i < timeWindowSet.getElements().size(); i++) {
			TimeWindow tw = timeWindowSet.getElements().get(i);
			if (bufferFirstAndLastRePerRouting.get(routing).get(r).containsKey(tw)
					&& bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).size() > 0
					&& !alreadyDeleted.contains(tw)) {

				if (first) {
					tw.setTempFollower(null);
					tw.setTempSlackFollower(null);
					bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).add(routeCopy
							.get(bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(0).getPosition() - 1));
					bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).remove(0);
				}

				if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).size() > 1) {
					if (first) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).add(routeCopy.get(
								bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(0).getPosition() - 1));
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).remove(0);
					}

					// Free slack
					double currentSlack = (tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER
							- bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(1).getServiceBegin();

					// Can you omit deletion in next tw?
					double additionalValueNextTw = 0.0;
					if (i < timeWindowSet.getElements().size() - 1) {
						double remainingShiftBefore = expectedServiceTime;
						for (int j = i + 1; j < timeWindowSet.getElements().size(); j++) {
							if (bufferFirstAndLastRePerRouting.get(routing).get(r)
									.containsKey(timeWindowSet.getElements().get(j))) {
								double shiftBefore = 0.0;
								if (bufferFirstAndLastRePerRouting.get(routing).get(r)
										.get(timeWindowSet.getElements().get(j)).size() > 1) {
									double freeSlackNext = (timeWindowSet.getElements().get(j).getEndTime()
											- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
											- bufferFirstAndLastRePerRouting.get(routing).get(r)
													.get(timeWindowSet.getElements().get(j)).get(1).getServiceBegin();
									shiftBefore = Math.min(remainingShiftBefore,
											bufferFirstAndLastRePerRouting.get(routing).get(r)
													.get(timeWindowSet.getElements().get(j)).get(0).getServiceBegin()
													- (timeWindowSet.getElements().get(j).getStartTime()
															- timeWindowSet.getTempStartOfDeliveryPeriod())
															* TIME_MULTIPLIER);
									if (freeSlackNext < minimumSlack) {
										if (freeSlackNext + shiftBefore > minimumSlack) {
											additionalValueNextTw += maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow
													.get(areaHigh.getId())
													.get(timeWindowSet.getElements().get(j).getId());
										}

									}
								} else if (bufferFirstAndLastRePerRouting.get(routing).get(r)
										.get(timeWindowSet.getElements().get(j)).size() > 0) {
									shiftBefore = Math.min(remainingShiftBefore,
											bufferFirstAndLastRePerRouting.get(routing).get(r)
													.get(timeWindowSet.getElements().get(j)).get(0).getServiceBegin()
													- (timeWindowSet.getElements().get(j).getStartTime()
															- timeWindowSet.getTempStartOfDeliveryPeriod())
															* TIME_MULTIPLIER);
								}
								remainingShiftBefore = shiftBefore;
								if (remainingShiftBefore == 0)
									break;
							}
						}
					}

					// Reduce additional value if current tw actualy does not
					// have to be deleted
					if (minimumSlack < currentSlack) {
						additionalValueNextTw -= maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow
								.get(areaHigh.getId()).get(tw.getId());
					}
					// 1) Do you have to delete something and 2) do you loose
					// possibly low value
					if ((additionalValueNextTw > 0 || currentSlack < minimumSlack)
							&& (highestValuePrevention < additionalValueNextTw
									|| (highestValuePrevention == additionalValueNextTw && currentSlack < slack))) {
						highestValuePrevention = additionalValueNextTw;
						twSlack = tw;
						slack = currentSlack;
					}

				}

			}

		}
		return twSlack;
	}

	public static Integer deleteSimpleSlackBased(Routing routing, Route r, ArrayList<RouteElement> routeCopy,
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo,
			HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			double minimumSlack, boolean considerOverlapping, HashMap<TimeWindow, Integer> acceptedPerTw,
			ArrayList<TimeWindow> alreadyDeleted, TimeWindowSet timeWindowSet, double expectedArrivals,
			double expectedServiceTime, HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow,
			Region region, DeliveryArea areaHigh) {
		int amountDeleted = 0;
		TimeWindow twSlack = determineToDelete(true, routing, r, routeCopy, bufferFirstAndLastRePerRouting,
				alreadyDeleted, minimumSlack, timeWindowSet, expectedServiceTime,
				maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, areaHigh);

		while (twSlack != null) {
			amountDeleted++;
			alreadyDeleted.add(twSlack);
			RouteElement bestDeletionOption = null;
			int indexBest = -1;
			double value = 0.0;
			for (int cReId = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(0).getPosition()
					- 1; cReId < bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(1)
							.getPosition(); cReId++) {

				RouteElement cRe = routeCopy.get(cReId);
				if (cRe.getOrder().getTimeWindowFinalId() == twSlack.getId()) {
					DeliveryArea area = cRe.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea();
					// Capacity/Demand ratio
					double cap = 0.0;
					for (TimeWindow tw : aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing)
							.get(area).keySet()) {
						cap += aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
								.get(tw);
					}
					double ratio = cap / (daWeightsLower.get(area) * expectedArrivals);
					TimeWindow relevantTw = twSlack;
					if (timeWindowSet.getOverlapping() && considerOverlapping)
						relevantTw = cRe.getTimeWindow();
					if (aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
							.get(relevantTw) > 1) {
						if (ratio + 1000 > value || (ratio + 1000 == value && cRe
								.getTempAdditionalCostsValue() > bestDeletionOption.getTempAdditionalCostsValue())) {
							value = ratio + 1000;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					} else {
						if (ratio > value || (ratio == value && cRe.getTempAdditionalCostsValue() > bestDeletionOption
								.getTempAdditionalCostsValue())) {
							value = ratio;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					}
				}
			}

			// Delete best option

			if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(0)
					.getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(0).getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).remove(0);
				for (int i = position; i < bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(0)
						.getPosition(); i++) {
					if (routeCopy.get(i).getOrder().getTimeWindowFinalId() == twSlack.getId()) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).add(0, routeCopy.get(i));
						break;

					}
				}

			} else if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack)
					.get(1).getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(1).getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).remove(1);

				for (int i = position - 2; i >= bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).get(0)
						.getPosition(); i--) {
					if (routeCopy.get(i).getOrder().getTimeWindowFinalId() == twSlack.getId()) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlack).add(1, routeCopy.get(i));
						break;

					}
				}

			}
			routeCopy.remove(indexBest);
			// Update aggregation values
			if (timeWindowSet.getOverlapping() && considerOverlapping) {
				aggregatedReferenceInformationNo
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
								.getDeliveryAreaOfSet())
						.get(routing)
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
								.getTempDeliveryArea())
						.put(bestDeletionOption.getTimeWindow(),
								aggregatedReferenceInformationNo
										.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea().getDeliveryAreaOfSet())
										.get(routing).get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea())
										.get(bestDeletionOption.getTimeWindow()) - 1);

				if (acceptedPerTw != null)
					acceptedPerTw.put(bestDeletionOption.getTimeWindow(),
							acceptedPerTw.get(bestDeletionOption.getTimeWindow()) - 1);

			} else {
				aggregatedReferenceInformationNo
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
								.getDeliveryAreaOfSet())
						.get(routing)
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
						.put(twSlack,
								aggregatedReferenceInformationNo
										.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea().getDeliveryAreaOfSet())
										.get(routing).get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea())
										.get(twSlack) - 1);
				if (acceptedPerTw != null)
					acceptedPerTw.put(twSlack, acceptedPerTw.get(twSlack) - 1);
			}

			acceptablePerDeliveryAreaAndRouting
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
							.getDeliveryAreaOfSet())
					.put(routing,
							acceptablePerDeliveryAreaAndRouting.get(bestDeletionOption.getOrder().getOrderRequest()
									.getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet()).get(routing) - 1);

			updateRoute(routeCopy, indexBest, timeWindowSet, r.getVehicleAssignment().getStartingLocationLat(),
					r.getVehicleAssignment().getStartingLocationLon(), r.getVehicleAssignment().getEndingLocationLat(),
					r.getVehicleAssignment().getEndingLocationLon(), region, TIME_MULTIPLIER);

			twSlack = determineToDelete(false, routing, r, routeCopy, bufferFirstAndLastRePerRouting, alreadyDeleted,
					minimumSlack, timeWindowSet, expectedServiceTime,
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, areaHigh);

		}

		return amountDeleted;
	}

	public static Integer deleteSimpleSlackBasedOverlapping(Routing routing, Route r, ArrayList<RouteElement> routeCopy,
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo,
			HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			double minimumSlack, boolean considerOverlapping, HashMap<TimeWindow, Integer> acceptedPerTw,
			ArrayList<TimeWindow> alreadyDeleted, TimeWindowSet timeWindowSet, TimeWindowSet timeWindowSetOriginal,
			double expectedArrivals, double expectedServiceTime, HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow,
			Region region, DeliveryArea areaHigh, HashMap<TimeWindow, ArrayList<TimeWindow>> newToOldTimeWindowMapping,
			HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping) {
		int amountDeleted = 0;
		Pair<TimeWindow, TimeWindow> twSlacks = determineToDeleteOverlapping(true, routing, r, routeCopy,
				bufferFirstAndLastRePerRouting, alreadyDeleted, minimumSlack, timeWindowSet, timeWindowSetOriginal,
				expectedServiceTime, maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, areaHigh,
				newToOldTimeWindowMapping, oldToNewTimeWindowMapping);

		while (twSlacks != null) {
			amountDeleted++;
			RouteElement bestDeletionOption = null;
			int indexBest = -1;
			double value = 0.0;

			for (int cReId = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).get(0)
					.getPosition() - 1; cReId < bufferFirstAndLastRePerRouting.get(routing).get(r)
							.get(twSlacks.getKey()).get(1).getPosition(); cReId++) {

				RouteElement cRe = routeCopy.get(cReId);
				if (cRe.getOrder().getTimeWindowFinalId() == twSlacks.getValue().getId()) {
					DeliveryArea area = cRe.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea();
					// Capacity/Demand ratio
					double cap = 0.0;
					for (TimeWindow tw : aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing)
							.get(area).keySet()) {
						cap += aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
								.get(tw);
					}
					double ratio = cap / (daWeightsLower.get(area) * expectedArrivals);
					TimeWindow relevantTw = cRe.getTimeWindow();
					if (aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
							.get(relevantTw) > 1) {
						if (ratio + 1000 > value || (ratio + 1000 == value && cRe
								.getTempAdditionalCostsValue() > bestDeletionOption.getTempAdditionalCostsValue())) {
							value = ratio + 1000;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					} else {
						if (ratio > value || (ratio == value && cRe.getTempAdditionalCostsValue() > bestDeletionOption
								.getTempAdditionalCostsValue())) {
							value = ratio;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					}
				}
			}

			// Delete best option

			if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey())
					.get(0).getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).get(0)
						.getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).remove(0);

				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).add(0,
						routeCopy.get(position));

			} else if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r)
					.get(twSlacks.getKey()).get(1).getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).get(1)
						.getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).remove(1);

				bufferFirstAndLastRePerRouting.get(routing).get(r).get(twSlacks.getKey()).add(1,
						routeCopy.get(position - 2));

			}
			routeCopy.remove(indexBest);
			// Update aggregation values

			aggregatedReferenceInformationNo
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
							.getDeliveryAreaOfSet())
					.get(routing)
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
					.put(bestDeletionOption.getTimeWindow(), aggregatedReferenceInformationNo
							.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
									.getDeliveryAreaOfSet())
							.get(routing)
							.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
							.get(bestDeletionOption.getTimeWindow()) - 1);

			if (acceptedPerTw != null)
				acceptedPerTw.put(bestDeletionOption.getTimeWindow(),
						acceptedPerTw.get(bestDeletionOption.getTimeWindow()) - 1);

			acceptablePerDeliveryAreaAndRouting
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
							.getDeliveryAreaOfSet())
					.put(routing,
							acceptablePerDeliveryAreaAndRouting.get(bestDeletionOption.getOrder().getOrderRequest()
									.getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet()).get(routing) - 1);

			updateRoute(routeCopy, indexBest, timeWindowSetOriginal, r.getVehicleAssignment().getStartingLocationLat(),
					r.getVehicleAssignment().getStartingLocationLon(), r.getVehicleAssignment().getEndingLocationLat(),
					r.getVehicleAssignment().getEndingLocationLon(), region, TIME_MULTIPLIER);

			twSlacks = determineToDeleteOverlapping(false, routing, r, routeCopy, bufferFirstAndLastRePerRouting,
					alreadyDeleted, minimumSlack, timeWindowSet, timeWindowSetOriginal, expectedServiceTime,
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, areaHigh, newToOldTimeWindowMapping,
					oldToNewTimeWindowMapping);

		}

		return amountDeleted;
	}

	public static Integer adhancedDeletionBasedOnSlack(Routing routing, Route r, ArrayList<RouteElement> routeCopy,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo,
			HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting,
			HashMap<TimeWindow, Integer> acceptedPerTw, double minimumSlack, DeliveryArea areaHigh,
			ArrayList<TimeWindow> alreadyDeleted, TimeWindowSet timeWindowSet, double expectedServiceTime,
			double expectedArrivals, HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow,
			Region region, boolean considerOverlapping) {
		int amountDeleted = 0;

		timeWindowSet.sortElementsByEndAsc();

		ArrayList<TimeWindow> candidatesForDeletion = new ArrayList<TimeWindow>();
		determineCandidatesForAdvancedDeletion(candidatesForDeletion, bufferFirstAndLastRePerRouting, alreadyDeleted,
				routing, r, routeCopy, minimumSlack, timeWindowSet, expectedServiceTime, TIME_MULTIPLIER);

		while (candidatesForDeletion.size() > 0) {

			// Delete one of the candidates
			amountDeleted++;
			TimeWindow bestCandidate = null;
			double bestValue = Double.MAX_VALUE;
			for (int i = 0; i < candidatesForDeletion.size(); i++) {
				double deletionValue = maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
						.get(candidatesForDeletion.get(i).getId());
				// If the follower has a follower, we can omit an additional
				// deletion
				if (candidatesForDeletion.get(i).getTempFollower() != null
						&& candidatesForDeletion.get(i).getTempFollower().getTempFollower() != null) {
					double additionalSlack = (bufferFirstAndLastRePerRouting.get(routing).get(r)
							.get(candidatesForDeletion.get(i).getTempFollower()).get(0).getServiceBegin())
							- (candidatesForDeletion.get(i).getTempFollower().getStartTime()
									- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER;
					additionalSlack += candidatesForDeletion.get(i).getTempSlackFollower();
					if (additionalSlack > expectedServiceTime + minimumSlack) {
						deletionValue = maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
								.get(candidatesForDeletion.get(i).getId())
								- Math.max(
										maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
												.get(candidatesForDeletion.get(i).getTempFollower().getId()),
										maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
												.get(candidatesForDeletion.get(i).getTempFollower().getTempFollower()
														.getId()));
					}
				}
				// can also be an in-between candidate that omits additional
				// deletions
				boolean inbetweenCan = false;
				if (i > 0
						&& candidatesForDeletion.get(i - 1).getTempFollower() != null && candidatesForDeletion
								.get(i - 1).getTempFollower().getId() == candidatesForDeletion.get(i).getId()
						&& candidatesForDeletion.get(i).getTempFollower() != null) {
					double potentialDeletionValue = maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow
							.get(areaHigh.getId()).get(candidatesForDeletion.get(i).getId())
							- Math.max(
									maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
											.get(candidatesForDeletion.get(i - 1).getId()),
									maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(areaHigh.getId())
											.get(candidatesForDeletion.get(i).getTempFollower().getId()));
					if (potentialDeletionValue <= deletionValue) {
						deletionValue = potentialDeletionValue;
						inbetweenCan = true;
					}

				}
				if (deletionValue < bestValue || (deletionValue == bestValue && inbetweenCan)) {
					bestValue = deletionValue;
					bestCandidate = candidatesForDeletion.get(i);
				}

			}
			alreadyDeleted.add(bestCandidate);
			RouteElement bestDeletionOption = null;
			int indexBest = -1;
			double value = 0.0;
			for (int cReId = bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).get(0).getPosition()
					- 1; cReId < bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).get(1)
							.getPosition(); cReId++) {

				RouteElement cRe = routeCopy.get(cReId);
				if (cRe.getOrder().getTimeWindowFinalId() == bestCandidate.getId()) {
					DeliveryArea area = cRe.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea();
					// Capacity/Demand ratio
					double cap = 0.0;
					for (TimeWindow tw : aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing)
							.get(area).keySet()) {
						cap += aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
								.get(tw);
					}
					double ratio = cap / (daWeightsLower.get(area) * expectedArrivals);
					TimeWindow relevantTw = bestCandidate;
					if (timeWindowSet.getOverlapping() && considerOverlapping)
						relevantTw = cRe.getTimeWindow();
					if (aggregatedReferenceInformationNo.get(area.getDeliveryAreaOfSet()).get(routing).get(area)
							.get(relevantTw) > 1) {
						if (ratio + 1000 > value) {
							value = ratio + 1000;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					} else {
						if (ratio > value) {
							value = ratio;
							bestDeletionOption = cRe;
							indexBest = cReId;
						}
					}
				}
			}

			if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate)
					.get(0).getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).get(0)
						.getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).remove(0);
				for (int i = position; i < bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).get(0)
						.getPosition(); i++) {
					if (routeCopy.get(i).getOrder().getTimeWindowFinalId() == bestCandidate.getId()) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).add(0, routeCopy.get(i));
						break;

					}
				}

			} else if (bestDeletionOption.getId() == bufferFirstAndLastRePerRouting.get(routing).get(r)
					.get(bestCandidate).get(1).getId()) {
				int position = bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).get(1)
						.getPosition();
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).remove(1);

				for (int i = position - 2; i >= bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate)
						.get(0).getPosition(); i--) {
					if (routeCopy.get(i).getOrder().getTimeWindowFinalId() == bestCandidate.getId()) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(bestCandidate).add(1, routeCopy.get(i));
						break;

					}
				}

			}
			routeCopy.remove(indexBest);
			// Update aggregation values
			if (timeWindowSet.getOverlapping() && considerOverlapping) {
				aggregatedReferenceInformationNo
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
								.getDeliveryAreaOfSet())
						.get(routing)
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
								.getTempDeliveryArea())
						.put(bestDeletionOption.getTimeWindow(),
								aggregatedReferenceInformationNo
										.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea().getDeliveryAreaOfSet())
										.get(routing).get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea())
										.get(bestDeletionOption.getTimeWindow()) - 1);
				if (acceptedPerTw != null) {
					acceptedPerTw.put(bestDeletionOption.getTimeWindow(),
							acceptedPerTw.get(bestDeletionOption.getTimeWindow()) - 1);
				}

			} else {
				aggregatedReferenceInformationNo
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
								.getDeliveryAreaOfSet())
						.get(routing)
						.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
						.put(bestCandidate,
								aggregatedReferenceInformationNo
										.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea().getDeliveryAreaOfSet())
										.get(routing).get(bestDeletionOption.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea())
										.get(bestCandidate) - 1);

				if (acceptedPerTw != null) {
					acceptedPerTw.put(bestCandidate, acceptedPerTw.get(bestCandidate) - 1);
				}

			}

			acceptablePerDeliveryAreaAndRouting
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
							.getDeliveryAreaOfSet())
					.put(routing,
							acceptablePerDeliveryAreaAndRouting.get(bestDeletionOption.getOrder().getOrderRequest()
									.getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet()).get(routing) - 1);

			updateRoute(routeCopy, indexBest, timeWindowSet, r.getVehicleAssignment().getStartingLocationLat(),
					r.getVehicleAssignment().getStartingLocationLon(), r.getVehicleAssignment().getEndingLocationLat(),
					r.getVehicleAssignment().getEndingLocationLon(), region, TIME_MULTIPLIER);

			candidatesForDeletion.clear();
			determineCandidatesForAdvancedDeletion(candidatesForDeletion, bufferFirstAndLastRePerRouting,
					alreadyDeleted, routing, r, routeCopy, minimumSlack, timeWindowSet, expectedServiceTime,
					TIME_MULTIPLIER);
		}

		return amountDeleted;
	}

	private static void determineCandidatesForAdvancedDeletion(ArrayList<TimeWindow> candidatesForDeletion,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			ArrayList<TimeWindow> alreadyDeleted, Routing routing, Route r, ArrayList<RouteElement> routeCopy,
			double minimumSlack, TimeWindowSet timeWindowSet, double expectedServiceTime, double timeMultiplier) {
		for (int i = 0; i < timeWindowSet.getElements().size() - 1; i++) {
			TimeWindow tw = timeWindowSet.getElements().get(i);
			if (!alreadyDeleted.contains(tw) && bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).size() > 1) {
				double currentSlack = (tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
						- bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(1).getServiceBegin();
				// Shifts definitely effect next time window if slack is
				// below service time and the expected additional shift
				if (currentSlack <= expectedServiceTime + minimumSlack) {
					// Look at slack of time windows that are directly
					// effected by a shift -> can they compensate earlier
					// shifts?
					for (int j = i + 1; j < timeWindowSet.getElements().size(); j++) {
						if (!alreadyDeleted.contains(timeWindowSet.getElements().get(j))
								&& timeWindowSet.getElements().get(j).getStartTime() <= tw.getEndTime()
								&& bufferFirstAndLastRePerRouting.get(routing).get(r)
										.get(timeWindowSet.getElements().get(j)).size() > 1) {
							double currentSlack2 = (timeWindowSet.getElements().get(j).getEndTime()
									- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
									- bufferFirstAndLastRePerRouting.get(routing).get(r)
											.get(timeWindowSet.getElements().get(j)).get(1).getServiceBegin();

							if (currentSlack2 <= minimumSlack
									+ Math.max(0, minimumSlack - bufferFirstAndLastRePerRouting.get(routing).get(r)
											.get(timeWindowSet.getElements().get(j)).get(0).getWaitingTime())) {

								if (!candidatesForDeletion.contains(tw)) {
									candidatesForDeletion.add(tw);
									tw.setTempFollower(timeWindowSet.getElements().get(j));
									tw.setTempSlackFollower(currentSlack2);
								} else {
									if (tw.getTempFollower() == null) {
										candidatesForDeletion.remove(tw.getTempFollower());
										tw.setTempFollower(timeWindowSet.getElements().get(j));
										tw.setTempSlackFollower(currentSlack2);
									}
								}
								if (!candidatesForDeletion.contains(timeWindowSet.getElements().get(j))) {
									candidatesForDeletion.add(timeWindowSet.getElements().get(j));
								}
								break;
							}
						} else {
							break;
						}
					}

				}
			}

		}
	}

}
