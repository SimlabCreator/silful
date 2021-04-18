package logic.algorithm.vr.orienteering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestDistanceComparator;
import logic.utility.comparator.OrderRequestNodeDistanceComparator;
import logic.utility.comparator.RouteElementInsertionValueDescComparator;
import logic.utility.comparator.RouteElementTempValueAdditionalCostsDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Orienteering algorithm that allows additional constraints (TODO) and can
 * consider multiple time window preferences per order request (Adaption of
 * Souffriau, W., Vansteenwegen, P., Vanden Berghe, G., & Van Oudheusden, D.
 * (2013). The multiconstraint team orienteering problem with multiple time
 * windows. Transportation Science, 47(1), 53-63)
 * 
 * Applies top to a mid-level delivery area (referes to a delivery area subset)
 * 
 * @author M. Lang
 *
 */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// TODO: Consider that sampled alternative preferences do not depend on price
// etc yet. only constant part!!!
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


public class GRILSOrienteeringAdaptedWithVehicleAreaAssignment implements RoutingAlgorithm {

	private OrderRequestSet orderRequestSet;
	private Double expectedServiceTime;
	private TimeWindowSet timeWindowSet;
	private final static double TIME_MULTIPLIER = 60.0;
	private final static boolean minimumExpectedDemandAsBound = true;
	private final double valueMultiplier = 100;
	private final static double UNEXPECTED_MULTIPLIER = 1.5;
	private ArrayList<VehicleAreaAssignment> vehicleAreaAssignments;
	private HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentsPerVehicleNo;
	private AlternativeSet alternativeSet;
	private ArrayList<Node> nodes;
	private ArrayList<NodeDistance> distances;
	private HashMap<Long, HashMap<Long, Double>> distanceMatrix;
	private int directDistances;
	private Region region;
	private Double greedinessUpperBound;
	private Double greedinessLowerBound;
	private Double greedinessStepsize;
	private int maximumRoundsWithoutImprovement;
	private int maximumRoundWithoutImprovementLocalSearch;
	private int alternativeRandomizationApproach;
	private HashMap<Integer, ArrayList<RouteElement>> bestSolution;
	private ArrayList<OrderRequest> orderRequestsLeftOverBest;
	private Double bestValue; // Value of the best solution
	private int sizeOfLongestRouteBestSolution;
	private boolean bestCompletelyShaked;
	private String lastUpdateInformation;
	private int numberOfProducedSolutions;
	private int maximumNumberOfSolutions;
	private int maximumNumberOfSolutionsLocalSearch;
	private int squaredValue;
	private int actualBasketValue;
	private int twSelectionValue;
	private int thresholdAcceptance;
	private Double locationClusterProbability;
	private int includeDriveFromStartingPosition;
	private Routing finalRouting;
	private ArrayList<OrderRequest> orderRequests;
	private HashMap<Integer, ArrayList<RouteElement>> routes;
	private double runtimeFirstFillup = 0;
	private double averageRuntimeFillup = 0;
	private int counterFillup = 0;
	private double averageRuntimeLocalSearch = 0;
	private int counterLocalSearch = 0;
	private int counterInsert = 0;
	private double averageRuntimeInsert = 0;
	private int counterCheckbyRoute = 0;
	private double averageRuntimeCheckByRoute = 0;
	private double averageRuntimeCheckByRouteFirst = 0;
	private int counterCheckbyRouteFirst = 0;
	private HashMap<Integer, Integer> popularityPerTimeWindow;
	private HashMap<Integer, Double> remainingCapacityPerTimeWindow;
	private HashMap<Integer, Double> maximumCapacityPerTimeWindow;
	private Alternative noPurchaseAlt;
	private int considerDemandProbability;
	private HashMap<DeliveryArea, Double> daLowerWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, Integer> bufferAlreadyAcceptedPerDeliveryArea;
	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private double expectedNumberOfArrivals;
	private Double saturationLimit;
	private HashMap<Integer, Alternative> alternativesToTimeWindows;
	private DemandSegmentWeighting demandSegmentWeighting;

	private static String[] paras = new String[] { "greediness_upperBound", "greediness_lowerBound",
			"greediness_stepsize", "maximumRoundsWithoutImprovement", "maximumRoundsWithoutImprovementLocalSearch",
			"Constant_service_time", "alternative_randomization_approach_(boolean)", "maximumNumberOfSolutions",
			"maximumNumberOfSolutionsLocalSearch", "squaredValue", "actualBasketValue",
			"twSelectionOption_(0:greedy,1:random,2:popularity,3:0+2)", "locationClusterProbability",
			"thresholdAcceptance", "directDistances", "includeDriveFromStartingPosition", "consider_demand_probability",
			"soft_saturation_limit" };

	public GRILSOrienteeringAdaptedWithVehicleAreaAssignment(Region region, OrderRequestSet orderRequestSet,
			DemandSegmentWeighting demandSegmentWeighting, TimeWindowSet timeWindowSet,
			ArrayList<VehicleAreaAssignment> vehicleAreaAssignments, DeliveryAreaSet deliveryAreaSet,
			ArrayList<Node> nodes, ArrayList<NodeDistance> distances, Double greedinessUpperBound,
			Double greedinessLowerBound, Double greedinessStepsize, Double maximumRoundsWithoutImprovement,
			Double expectedServiceTime, Double alternativeRandomizationApproach, Double maximumNumberOfSolutions,
			Double maximumRoundWithoutImprovementLocalSearch, Double maximumNumberOfSolutionsLocalSearch,
			Double squaredValue, Double actualBasketValue, Double twSelectionValue, Double locationClusterProbability,
			Double thresholdAcceptance, Double directDistances, Double includeDriveFromStartingPosition,
			Double considerDemandProbability, HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings, double expectedNumberOfArrivals,
			Double saturationLimit) {
		this.region = region;
		this.deliveryAreaSet = deliveryAreaSet;
		this.orderRequestSet = orderRequestSet;
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.timeWindowSet = timeWindowSet;
		this.expectedServiceTime = expectedServiceTime;
		this.nodes = nodes;
		this.distances = distances;
		this.greedinessUpperBound = greedinessUpperBound;
		this.greedinessLowerBound = greedinessLowerBound;
		this.greedinessStepsize = greedinessStepsize;
		this.maximumRoundsWithoutImprovement = maximumRoundsWithoutImprovement.intValue();
		this.maximumRoundWithoutImprovementLocalSearch = maximumRoundWithoutImprovementLocalSearch.intValue();
		this.vehicleAreaAssignments = vehicleAreaAssignments;
		this.maximumNumberOfSolutions = maximumNumberOfSolutions.intValue();
		this.maximumNumberOfSolutionsLocalSearch = maximumNumberOfSolutionsLocalSearch.intValue();
		this.alternativeRandomizationApproach = alternativeRandomizationApproach.intValue();
		this.squaredValue = squaredValue.intValue();
		this.actualBasketValue = actualBasketValue.intValue();
		this.twSelectionValue = twSelectionValue.intValue();
		this.locationClusterProbability = locationClusterProbability;
		this.thresholdAcceptance = thresholdAcceptance.intValue();
		this.directDistances = directDistances.intValue();
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfProducedSolutions = 0;
		this.considerDemandProbability = considerDemandProbability.intValue();
		this.daLowerWeights = daLowerWeights;
		this.daLowerSegmentWeightings = daLowerSegmentWeightings;
		this.expectedNumberOfArrivals = expectedNumberOfArrivals;
		this.saturationLimit = saturationLimit;
	}

	public void start() {

		this.alternativesToTimeWindows = new HashMap<Integer, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
			}
		}
		// Initialise the closest nodes for the requests
		ArrayList<OrderRequest> orderRequestsE = this.orderRequestSet.getElements();
		this.orderRequests = new ArrayList<OrderRequest>();
		this.popularityPerTimeWindow = new HashMap<Integer, Integer>();
		noPurchaseAlt = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getNoPurchaseAlternative();
		maximumCapacityPerTimeWindow = new HashMap<Integer, Double>();
		if (this.twSelectionValue == 2 || this.twSelectionValue == 3) {
			for (Alternative alt : this.alternativeSet.getElements()) {
				if (!alt.getNoPurchaseAlternative()) {
					TimeWindow tw = alt.getTimeWindows().get(0);
					double length = (tw.getEndTime() - tw.getStartTime()) * TIME_MULTIPLIER;
					maximumCapacityPerTimeWindow.put(tw.getId(), length);
				}
			}
		}

		this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow = new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>();
		this.bufferAlreadyAcceptedPerDeliveryArea = new HashMap<DeliveryArea, Integer>();

		double sumOfWeights=0.0;
		for (DeliveryArea area : this.daLowerWeights.keySet()) {
			sumOfWeights+= this.daLowerWeights.get(area);
			bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow.put(area, new HashMap<TimeWindow, Integer>());
			bufferAlreadyAcceptedPerDeliveryArea.put(area, 0);
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow.get(area).put(tw, 0);
			}
		}
		//System.out.println("Sum of weights");
		if (this.considerDemandProbability == 1) {
			this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
					.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting,
							timeWindowSet);
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
		}

		for (OrderRequest request : orderRequestsE) {
			if (this.directDistances != 1) {
				Node closestNode = LocationService.findClosestNode(nodes, request.getCustomer().getLat(),
						request.getCustomer().getLon());
				request.getCustomer().setClosestNodeId(closestNode.getLongId());
				request.getCustomer().setClosestNode(closestNode);
				// TODO: Add distance to closest node
			}
			orderRequests.add(request);

			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
					request.getCustomer());
			request.getCustomer().setTempDeliveryArea(area);
			if (this.twSelectionValue == 2 || this.twSelectionValue == 3) {
				for (Integer altId : request.getAlternativePreferences().keySet()) {

					if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
							.get(noPurchaseAlt.getId())) {
						for (TimeWindow tw : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
								.getAlternativeSet().getAlternativeById(altId).getTimeWindows()) {
							if (popularityPerTimeWindow.containsKey(tw.getId())) {
								popularityPerTimeWindow.put(tw.getId(), popularityPerTimeWindow.get(tw.getId()) + 1);
							} else {
								popularityPerTimeWindow.put(tw.getId(), 1);
							}
						}
						;
					}

				}
			}
		}

		if (this.directDistances != 1) {
			// Initialise the distances between nodes
			this.distanceMatrix = LocationService.getDistanceMatrixBetweenNodes(this.distances);
		}

		this.initialiseRoutes();

		// Initialise best value
		this.bestValue = 0.0;

		// Outer loop: decrease greediness
		Double currentGreediness = this.greedinessUpperBound;
		while (currentGreediness >= this.greedinessLowerBound) {

			// Double currentGreediness = this.greedinessLowerBound;
			// while (currentGreediness <= this.greedinessUpperBound) {

			int noRepetitionsWithoutImprovement = 0;
			int numberOfRemovalsPerRoute = 2;
			if (this.maximumNumberOfSolutionsLocalSearch < 1) {
				numberOfRemovalsPerRoute = 1;
			}

			int[] startPositions = new int[this.vehicleAreaAssignments.size()];

			// Initialise start positions with 1 (depot cannot be removed)
			for (int i = 0; i < this.vehicleAreaAssignments.size(); i++) {
				startPositions[i] = 1;
			}
			boolean noRestart = true;
			while (noRestart) {

				// Fill up routes with unassigned requests
				if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
					break;
				long startTime = System.currentTimeMillis();
				this.fillUpRoutes(currentGreediness, false, counterFillup, false);

				long endTime = System.currentTimeMillis();
				long totalTime = endTime - startTime;
				if (counterFillup == 0)
					runtimeFirstFillup = totalTime;
				averageRuntimeFillup = (averageRuntimeFillup * this.counterFillup + totalTime) / ++counterFillup;
				this.numberOfProducedSolutions++;

				// Perform local search
				startTime = System.currentTimeMillis();

				// TODO: Buffer does not yet work with local search
				// this.performLocalSearch(this.maximumRoundWithoutImprovementLocalSearch,
				// this.maximumNumberOfSolutionsLocalSearch);
				endTime = System.currentTimeMillis();
				totalTime = endTime - startTime;
				averageRuntimeLocalSearch = (averageRuntimeLocalSearch * this.counterLocalSearch + totalTime)
						/ ++counterLocalSearch;
				// Check for improvement and update best solution as well as
				// parameters
				Double newValue = this.evaluateSolution(this.routes);
				boolean improvement = this.updateBestSolution(this.routes, newValue, currentGreediness);

				if (improvement) {
					noRepetitionsWithoutImprovement = 0;
					numberOfRemovalsPerRoute = 2;
					if (this.maximumNumberOfSolutionsLocalSearch < 1) {
						numberOfRemovalsPerRoute = 1;
					}
					// -> new start is best solution

				} else {
					noRepetitionsWithoutImprovement++;

					if (this.thresholdAcceptance == 1) {

						if (numberOfRemovalsPerRoute >= this.sizeOfLongestRouteBestSolution)
							this.bestCompletelyShaked = true;
						// If we use a threshold, the best solution is used as
						// starting point as long as the new solution is more
						// than 10% worse than
						double thresholdMultiplier = numberOfRemovalsPerRoute / this.sizeOfLongestRouteBestSolution;
						if (this.bestCompletelyShaked)
							thresholdMultiplier = 1;
						if ((this.bestValue - newValue) / this.bestValue > 0.1 * thresholdMultiplier) {
							// If the new solution is worse, begin again from
							// the old
							this.routes = this.copySolution(this.bestSolution);
						}
					}
				}

				// Caution: adaption of original algorithm, 1.) change value of
				// startPosition based on filled up routes, 2.) adapt all
				// individual S such that feasible
				int lengthOfLongestRoute = 0;
				for (int i = 0; i < startPositions.length; i++) {

					if (startPositions[i] >= routes.get(this.vehicleAreaAssignments.get(i).getVehicleNo()).size() - 1) {
						startPositions[i] = startPositions[i]
								% (routes.get(this.vehicleAreaAssignments.get(i).getVehicleNo()).size() - 1) + 1;
					}

					if (routes.get(this.vehicleAreaAssignments.get(i).getVehicleNo()).size()
							- 2 > lengthOfLongestRoute) {// -2
						// for
						// depot
						// elements
						lengthOfLongestRoute = routes.get(this.vehicleAreaAssignments.get(i).getVehicleNo()).size() - 2;
					}
				}

				// Complete routes where exchanged since last improvement? Or
				// maximum rounds without improvement reached? Then stop
				if (maximumRoundsWithoutImprovement < 1 && numberOfRemovalsPerRoute > lengthOfLongestRoute) {
					System.out.println("Start next round because already exchanged all. " + numberOfRemovalsPerRoute);
					noRestart = false;
				} else if (maximumRoundsWithoutImprovement > 0
						&& noRepetitionsWithoutImprovement > maximumRoundsWithoutImprovement) {
					noRestart = false;
				} else {
					// Attention: Change, R cannot be larger than the length of
					// the
					// longest route (would have no effect)
					if (numberOfRemovalsPerRoute > lengthOfLongestRoute) {
						numberOfRemovalsPerRoute = 2;
						if (this.maximumNumberOfSolutionsLocalSearch < 1) {
							numberOfRemovalsPerRoute = 1;
						}
					}
				}
				this.removeRouteElements(numberOfRemovalsPerRoute, startPositions);

				// Add first new based on neighborhood, if randomly drawn
				if (new Random().nextDouble() <= this.locationClusterProbability) {
					RouteElement toInsert = this
							.getRouteElementforRequestWithBestNeighborhood(numberOfRemovalsPerRoute);
					this.insertRouteElement(toInsert);
					bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(toInsert.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
							.put(toInsert.getOrder().getTimeWindowFinal(),
									bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(toInsert.getOrder().getOrderRequest().getCustomer()
													.getTempDeliveryArea())
											.get(toInsert.getOrder().getTimeWindowFinal()) + 1);
					bufferAlreadyAcceptedPerDeliveryArea
							.put(toInsert.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea(),
									bufferAlreadyAcceptedPerDeliveryArea.get(
											toInsert.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
											+ 1);
					this.orderRequests.remove(toInsert.getOrder().getOrderRequest());
				}

				for (int i = 0; i < startPositions.length; i++) {
					startPositions[i] = startPositions[i] + numberOfRemovalsPerRoute;
				}

				numberOfRemovalsPerRoute++;

			}
			if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
				break;
			currentGreediness -= this.greedinessStepsize;
			// currentGreediness += this.greedinessStepsize;
		}

		// Fill up possible wholes with expected preferences
		this.routes = this.bestSolution;
		this.orderRequests = orderRequestsLeftOverBest;
		this.fillUpRoutes(currentGreediness, false, counterFillup, true);
		
		// The best solution found is returned as routing
		ArrayList<Route> finalRoutes = new ArrayList<Route>();
		for (Integer routeId : bestSolution.keySet()) {
			Route route = new Route();
			route.setVehicleAreaAssignmentId(this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getId());
			// Delete dummy depot elements
			bestSolution.get(routeId).remove(bestSolution.get(routeId).size() - 1);
			bestSolution.get(routeId).remove(0);

			for (RouteElement e : bestSolution.get(routeId)) {
				e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
				e.setTravelTime(e.getTravelTimeTo());
			}

			route.setRouteElements(bestSolution.get(routeId));
			finalRoutes.add(route);
		}

		this.finalRouting = new Routing();
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setPossiblyTarget(true);
		this.finalRouting.setRoutes(finalRoutes);
		this.finalRouting.setTimeWindowSet(timeWindowSet);
		this.finalRouting.setTimeWindowSetId(timeWindowSet.getId());
		OrderSet orderSet = new OrderSet();
		orderSet.setOrderRequestSet(orderRequestSet);
		orderSet.setOrderRequestSetId(orderRequestSet.getId());
		ArrayList<Order> orders = new ArrayList<Order>();
		for (Route route : this.finalRouting.getRoutes()) {
			for (RouteElement e : route.getRouteElements()) {
				Order order = e.getOrder();
				order.setAccepted(true);
				orders.add(order);
			}
		}
		for (OrderRequest or : this.orderRequestsLeftOverBest) {
			Order order = new Order();
			order.setAccepted(false);
			order.setOrderRequestId(or.getId());
			orders.add(order);

		}
		orderSet.setElements(orders);
		this.finalRouting.setOrderSet(orderSet);
		this.finalRouting
				.setAdditionalInformation(this.lastUpdateInformation + "; Overall: " + this.numberOfProducedSolutions);
		System.out.println("Average runtime fillup: " + this.averageRuntimeFillup);
		System.out.println("Runtime first fillup: " + this.runtimeFirstFillup);
		System.out.println("Average runtime local search: " + this.averageRuntimeLocalSearch);
		System.out.println("Average runtime insert: " + this.averageRuntimeInsert);
		System.out.println("Average runtime check by route" + averageRuntimeCheckByRoute);
		System.out.println("Average runtime check by route first fillup" + averageRuntimeCheckByRouteFirst);
	}

	/**
	 * Performs a first move local search strategy
	 * 
	 * @param routes
	 *            Current status of the routes
	 * @param unassignedRequests
	 *            Requests that can be added
	 */
	@SuppressWarnings("unchecked")
	private void performLocalSearch(int iterationsWithoutImprovement, int maxNumberOfIterations) {

		HashMap<Integer, ArrayList<RouteElement>> routesCopy;
		ArrayList<OrderRequest> unassignedRequestsCopy;
		int noNoImprovement = 0;
		int numberOfIterations = 0;

		while (noNoImprovement <= iterationsWithoutImprovement) {
			numberOfIterations++;
			if (numberOfIterations >= maxNumberOfIterations)
				break;

			routesCopy = this.copySolution(this.routes);
			unassignedRequestsCopy = (ArrayList<OrderRequest>) this.orderRequests.clone();

			// Choose random route
			int routeId = new Random().nextInt(this.routes.size());

			// Choose randomly if worst element or random removal
			double randomNumber = new Random().nextDouble();
			int elementToRemove = -1;

			// Delete worst
			if (randomNumber < 0.5) {
				// Find position with largest shift
				double largestShift = 0f;
				// RouteElement worstElement;
				int worstElementPosition = -1;
				for (int reId = 1; reId < this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo())
						.size() - 1; reId++) {
					RouteElement re = this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo())
							.get(reId);
					double costs = re.getWaitingTime() + re.getServiceTime() + re.getTravelTimeFrom()
							+ re.getTravelTimeTo();

					if (this.directDistances != 1) {
						costs -= this.distanceMatrix
								.get(this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo())
										.get(reId - 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId())
								.get(this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo())
										.get(reId + 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId());
					} else {
						costs -= LocationService.calculateHaversineDistanceBetweenCustomers(
								this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()).get(reId - 1)
										.getOrder().getOrderRequest().getCustomer(),
								this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()).get(reId + 1)
										.getOrder().getOrderRequest().getCustomer())
								/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					if (costs > largestShift) {
						// worstElement = re;
						worstElementPosition = reId;
						largestShift = costs;
					}
				}
				elementToRemove = worstElementPosition;

			} else {
				// Delete randomly
				int routePosition = new Random()
						.nextInt(this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()).size() - 2);
				elementToRemove = routePosition + 1;
			}

			this.removeRouteElementsOfRoute(this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()),
					this.vehicleAreaAssignments.get(routeId).getVehicleNo(), 1, elementToRemove, true);

			// Go through other routes and check if cheaper insertion in route
			// than in their own one
			ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();
			for (int currentRoute : this.routes.keySet()) {
				if (currentRoute != this.vehicleAreaAssignments.get(routeId).getVehicleNo()) {
					for (int currentElementToCheck = 1; currentElementToCheck < this.routes.get(currentRoute).size()
							- 1; currentElementToCheck++) {
						HashMap<Integer, RouteElement> cheapestInsertionElementsList = this
								.getCheapestInsertionElementByRoute(
										this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()),
										this.vehicleAreaAssignments.get(routeId).getVehicleNo(),
										this.routes.get(currentRoute).get(currentElementToCheck).getOrder()
												.getOrderRequest(), false);

						if (cheapestInsertionElementsList.size() > 0) {
							RouteElement cheapestInsertionElement = null;
							double bestValue = Double.MAX_VALUE;
							for (int insertionElementId : cheapestInsertionElementsList.keySet()) {
								if (cheapestInsertionElementsList.get(insertionElementId).getTempShift() < bestValue) {
									cheapestInsertionElement = cheapestInsertionElementsList.get(insertionElementId);
									bestValue = cheapestInsertionElementsList.get(insertionElementId).getTempShift();
								}
							}

							// Should be better than current insertion value of
							// that element in its current route
							double costsInOldRoute = this.routes.get(currentRoute).get(currentElementToCheck - 1)
									.getTravelTimeFrom()
									+ this.routes.get(currentRoute).get(currentElementToCheck + 1).getTravelTimeTo()
									+ this.routes.get(currentRoute).get(currentElementToCheck).getServiceTime()
									+ this.routes.get(currentRoute).get(currentElementToCheck).getWaitingTime();

							if (this.directDistances != 1) {
								costsInOldRoute -= this.distanceMatrix
										.get(this.routes.get(currentRoute).get(currentElementToCheck - 1).getOrder()
												.getOrderRequest().getCustomer().getClosestNodeId())
										.get(this.routes.get(currentRoute).get(currentElementToCheck + 1).getOrder()
												.getOrderRequest().getCustomer().getClosestNodeId());
							} else {
								costsInOldRoute -= LocationService.calculateHaversineDistanceBetweenCustomers(
										this.routes.get(currentRoute).get(currentElementToCheck - 1).getOrder()
												.getOrderRequest().getCustomer(),
										this.routes.get(currentRoute).get(currentElementToCheck + 1).getOrder()
												.getOrderRequest().getCustomer())
										/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
							}
							if (costsInOldRoute >= cheapestInsertionElement.getTempShift()) {
								cheapestInsertionElement.setTempCurrentlyInOtherRoute(currentRoute);
								cheapestInsertionElement.setTempCurrentlyInOtherRoutePosition(currentElementToCheck);
								possibleInsertions.add(cheapestInsertionElement);
							}
						}
					}

				}
			}

			// Go through unassigned to get insertion costs
			for (OrderRequest unassignedRequest : this.orderRequests) {

				HashMap<Integer, RouteElement> cheapestInsertionElementsList = this.getCheapestInsertionElementByRoute(
						this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()), routeId,
						unassignedRequest, false);

				if (cheapestInsertionElementsList.keySet().size() > 0) {
					RouteElement bestInsertionElement = null;
					double bestValue = Double.MAX_VALUE;
					for (int insertionElementId : cheapestInsertionElementsList.keySet()) {
						if (cheapestInsertionElementsList.get(insertionElementId).getTempShift() < bestValue) {
							bestInsertionElement = cheapestInsertionElementsList.get(insertionElementId);
							bestValue = cheapestInsertionElementsList.get(insertionElementId).getTempShift();
						}
					}

					if (bestInsertionElement != null)
						possibleInsertions.add(bestInsertionElement);
				}

			}

			if (possibleInsertions.size() > 0) {
				// Get cheapest

				Collections.sort(possibleInsertions,
						new RouteElementInsertionValueDescComparator(this.actualBasketValue, this.squaredValue));

				if (possibleInsertions.get(0).getTempCurrentlyInOtherRoute() != null) {
					this.removeRouteElementsOfRoute(
							this.routes.get(possibleInsertions.get(0).getTempCurrentlyInOtherRoute()),
							possibleInsertions.get(0).getTempCurrentlyInOtherRoute(), 1,
							possibleInsertions.get(0).getTempCurrentlyInOtherRoutePosition(), true);
					this.insertRouteElement(possibleInsertions.get(0));
					bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer()
									.getTempDeliveryArea())
							.put(possibleInsertions.get(0).getOrder().getTimeWindowFinal(),
									bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer()
													.getTempDeliveryArea())
											.get(possibleInsertions.get(0).getOrder().getTimeWindowFinal()) + 1);
					bufferAlreadyAcceptedPerDeliveryArea.put(
							possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer().getTempDeliveryArea(),
							bufferAlreadyAcceptedPerDeliveryArea.get(possibleInsertions.get(0).getOrder()
									.getOrderRequest().getCustomer().getTempDeliveryArea()) + 1);
					this.orderRequests.remove(possibleInsertions.get(0).getOrder().getOrderRequest());

				} else {
					this.insertRouteElement(possibleInsertions.get(0));
					bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer()
									.getTempDeliveryArea())
							.put(possibleInsertions.get(0).getOrder().getTimeWindowFinal(),
									bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer()
													.getTempDeliveryArea())
											.get(possibleInsertions.get(0).getOrder().getTimeWindowFinal()) + 1);
					bufferAlreadyAcceptedPerDeliveryArea.put(
							possibleInsertions.get(0).getOrder().getOrderRequest().getCustomer().getTempDeliveryArea(),
							bufferAlreadyAcceptedPerDeliveryArea.get(possibleInsertions.get(0).getOrder()
									.getOrderRequest().getCustomer().getTempDeliveryArea()) + 1);
					this.orderRequests.remove(possibleInsertions.get(0).getOrder().getOrderRequest());

				}

			}

			// Check if further holes can be filled up
			this.fillUpRoutes(1.0, false, 1, false);
			this.numberOfProducedSolutions++;

			// If the new solution is worse, reset to old
			if (this.evaluateSolution(this.routes) < this.evaluateSolution(routesCopy)) {
				this.routes = routesCopy;
				this.orderRequests = unassignedRequestsCopy;
				noNoImprovement++;
			} else {
				noNoImprovement = 0;
			}

		}

	}

	/**
	 * Fills up existing routes with unassigned requests
	 * 
	 * @param routes
	 *            Current status of the routes
	 * @param greedinessFactor
	 *            Measure of greediness to chose the next insertion option
	 * @param unassignedRequests
	 *            Requests that are not in the routing until now
	 */
	private void fillUpRoutes(Double greedinessFactor, boolean again, int fillUpCounter, boolean considerWholeConsiderationSet) {

		// Assign unassigned requests to the routes. Stop if no requests are
		// left
		// over (or if no assignment feasible)
		while (this.orderRequests.size() > 0) {

			// Choose selection strategy for current step
			int selectionStrategy = this.twSelectionValue;
			if (this.twSelectionValue == 3) {
				Random r = new Random();
				int selectedApproach = r.nextInt(2);
				if (selectedApproach == 0) {
					selectionStrategy = 0;
				} else {
					selectionStrategy = 2;
				}
			}

			// Insertion options
			ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();
			if (selectionStrategy == 2) {
				this.determineRemainingCapacity();
			}
			// Go through unassigned requests and define cheapest insertion per
			// request

			if (this.alternativeRandomizationApproach == 1) {
				/// Define how many to evaluate
				int numberToEvaluate = (int) (this.orderRequests.size() * greedinessFactor);
				Collections.shuffle(this.orderRequests);

				for (int requestId = 0; requestId < numberToEvaluate; requestId++) {
					double iValue;
					if (this.actualBasketValue == 1.0) {
						iValue = this.orderRequests.get(requestId).getBasketValue();
					} else {

						try {
							iValue = ProbabilityDistributionService
									.getMeanByProbabilityDistribution(this.orderRequests.get(requestId).getCustomer()
											.getOriginalDemandSegment().getBasketValueDistribution());
						} catch (ParameterUnknownException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
							iValue = 1.0;
							System.exit(0);
						}
					}

//					if (this.squaredValue == 1.0) {
//						iValue = Math.sqrt(iValue);
//					}
					RouteElement optionalElement = this.findCheapestInsertionOption(this.orderRequests.get(requestId),
							fillUpCounter, selectionStrategy, iValue, considerWholeConsiderationSet);
					if (optionalElement != null) {
						insertionOptions.add(optionalElement);
					}
				}

				// If no feasible insertions, than go through whole list
				if (insertionOptions.size() == 0) {
					for (int requestId = numberToEvaluate; requestId < this.orderRequests.size(); requestId++) {
						double iValue;
						if (this.actualBasketValue == 1.0) {
							iValue = this.orderRequests.get(requestId).getBasketValue();
						} else {

							try {
								iValue = ProbabilityDistributionService
										.getMeanByProbabilityDistribution(this.orderRequests.get(requestId)
												.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
							} catch (ParameterUnknownException ex) {
								// TODO Auto-generated catch block
								ex.printStackTrace();
								iValue = 1.0;
								System.exit(0);
							}
						}
						
//						if (this.squaredValue == 1.0) {
//						iValue = Math.sqrt(iValue);
//					}
						RouteElement optionalElement = this.findCheapestInsertionOption(
								this.orderRequests.get(requestId), fillUpCounter, selectionStrategy, iValue, considerWholeConsiderationSet);
						if (optionalElement != null) {
							insertionOptions.add(optionalElement);
						}
					}
				}
			} else {
				for (OrderRequest request : this.orderRequests) {
					double iValue;
					if (this.actualBasketValue == 1.0) {
						iValue = request.getBasketValue();
					} else {

						try {
							iValue = ProbabilityDistributionService.getXByCummulativeDistributionQuantile(
									request.getCustomer().getOriginalDemandSegment().getBasketValueDistribution(), 0.5);
						} catch (ParameterUnknownException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
							iValue = 1.0;
							System.exit(0);
						}
					}
					
//					if (this.squaredValue == 1.0) {
//					iValue = Math.sqrt(iValue);
//				}
					RouteElement optionalElement = this.findCheapestInsertionOption(request, fillUpCounter,
							selectionStrategy, iValue, considerWholeConsiderationSet);

					if (optionalElement != null) {

						insertionOptions.add(optionalElement);
					}
				}
			}
			// Stop if no feasible insertions exist
			if (insertionOptions.size() == 0) {
				return;
			} else {
				if (again) {
					System.out.println("Something is strange");
				}
			}
			
			if(insertionOptions.size()<5)
				System.out.println("What does this look like");

			// Determine value based on saturation limit
			for (RouteElement e : insertionOptions) {
				double iValue = e.getTempValue();

				// If the value is negative, there are more requests for this tw
				// than maximal expected
				if (iValue < 0) {
					iValue = 0;
				}

				double saturationLevel = this.bufferAlreadyAcceptedPerDeliveryArea
						.get(e.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
						/ (this.expectedNumberOfArrivals
								* this.daLowerWeights
										.get(e.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
								* (1.0 - this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(e.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
										.get(null)));
				// The higher the saturation,
				// the lower the selection probability
				double value;
				if (saturationLevel > 1.0) {
					value = 0;
				} else {
					value = (1.0 - Math.max(saturationLevel, this.saturationLimit)) / (1.0 - this.saturationLimit)
							* iValue;
				}

				e.setTempValue(value / e.getTempAdditionalCostsValue());
				if(considerWholeConsiderationSet)
					System.out.println("Values for expected fillup at end");
			}

			// Sort regarding value to find maximum and minimum value
			Collections.sort(insertionOptions, new RouteElementTempValueAdditionalCostsDescComparator());

			int chosenIndex = 0;
			if (this.alternativeRandomizationApproach != 1) {
				// Calculate value border based on greediness factor
				double max = insertionOptions.get(0).getTempValue();

				double min = insertionOptions.get(insertionOptions.size() - 1).getTempValue();

				double valueBorder = (max - min) * greedinessFactor + min;
				// Find index of element with lowest value above the border
				int borderElement = 0; // The first element is definitely higher
				while (borderElement < insertionOptions.size() - 1) {

					double elementValue = insertionOptions.get(borderElement + 1).getTempValue();

					if (elementValue > valueBorder) {
						borderElement++;
					} else {
						break; // Stop because list is sorted and all afterwards
								// will not be higher
					}
				}
				if (borderElement > 0 && greedinessFactor.equals(1)) {
					System.out.println("Several with same value?");
				}

				// Choose a random number between 0 and the borderElement
				Random randomGenerator = new Random();
				chosenIndex = randomGenerator.nextInt(borderElement + 1);

			}
			// Insert the respective Element in the route
			this.insertRouteElement(insertionOptions.get(chosenIndex));
			bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
					.get(insertionOptions.get(chosenIndex).getOrder().getOrderRequest().getCustomer()
							.getTempDeliveryArea())
					.put(insertionOptions.get(chosenIndex).getOrder().getTimeWindowFinal(),
							bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
									.get(insertionOptions.get(chosenIndex).getOrder().getOrderRequest().getCustomer()
											.getTempDeliveryArea())
									.get(insertionOptions.get(chosenIndex).getOrder().getTimeWindowFinal()) + 1);
			bufferAlreadyAcceptedPerDeliveryArea.put(
					insertionOptions.get(chosenIndex).getOrder().getOrderRequest().getCustomer().getTempDeliveryArea(),
					bufferAlreadyAcceptedPerDeliveryArea.get(insertionOptions.get(chosenIndex).getOrder()
							.getOrderRequest().getCustomer().getTempDeliveryArea()) + 1);
			;
			// Delete the respective order request from the unassigned requests
			this.orderRequests.remove(insertionOptions.get(chosenIndex).getOrder().getOrderRequest());

		}
	}

	/**
	 * Finds the cheapest insertion option and returns a route element with the
	 * respective information. Returns null if no insertion is feasible. Ties
	 * are always solved randomly.
	 * 
	 * @param request
	 *            Respective order request to insert in the routes
	 * @param routes
	 *            Current status of the routes
	 * @return route element with all information. Null if no feasible
	 *         insertion.
	 */
	private RouteElement findCheapestInsertionOption(OrderRequest request, int fillUpCounter, int selectionStrategy,
			double orderValue, boolean considerWholeConsiderationSet) {

		// Collect best elements per tw over all routes
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : this.routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = this.routes.get(routeId);
			long startTime = System.currentTimeMillis();
			HashMap<Integer, RouteElement> best = this.getCheapestInsertionElementByRoute(route, routeId, request, considerWholeConsiderationSet);
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			if (this.counterFillup == 0) {
				averageRuntimeCheckByRouteFirst = (averageRuntimeCheckByRouteFirst * this.counterCheckbyRouteFirst
						+ totalTime) / ++counterCheckbyRouteFirst;
			}
			averageRuntimeCheckByRoute = (averageRuntimeCheckByRoute * this.counterCheckbyRoute + totalTime)
					/ ++counterCheckbyRoute;

			// Update overall best over routes
			for (int key : best.keySet()) {

				if (!bestElementPerTw.containsKey(key)) {
					bestElementPerTw.put(key, best.get(key));
				} else {
					// Which ones to compare?
					double toCompareFirst = (bestElementPerTw.get(key).getTempShiftWithDepotDistance() != null)
							? bestElementPerTw.get(key).getTempShiftWithDepotDistance()
							: bestElementPerTw.get(key).getTempShift();
					double toCompareSecond = (best.get(key).getTempShiftWithDepotDistance() != null)
							? best.get(key).getTempShiftWithDepotDistance() : best.get(key).getTempShift();
					if ((toCompareFirst > toCompareSecond)
							|| ((toCompareFirst == toCompareSecond) && new Random().nextBoolean())) {

						bestElementPerTw.put(key, best.get(key));
					}
				}
			}

		}

		// No feasible insertion?
		if (bestElementPerTw.keySet().size() == 0)
			return null;

		// Choose time window according to time window selection strategy

		if (selectionStrategy == 0) {
			return this.applySelectionStrategyTimeWindowUtility(bestElementPerTw, request, orderValue);
			// return this.applySelectionStrategyGreedy(bestElementPerTw,
			// request, fillUpCounter);

		} else if (selectionStrategy == 1) {

			return this.applySelectionStrategyRandom(bestElementPerTw, request, fillUpCounter, orderValue);

		} else {

			return this.getBestElementByTwPopularityRatio(bestElementPerTw, request, fillUpCounter, orderValue);

		}

	}

	private RouteElement applySelectionStrategyRandom(HashMap<Integer, RouteElement> bestElementPerTw,
			OrderRequest request, int fillUpCounter, double orderValue) {

		if (this.considerDemandProbability == 1) {

			ArrayList<RouteElement> completelyOkay = new ArrayList<RouteElement>();
			ArrayList<RouteElement> onlyIfNeeded = new ArrayList<RouteElement>();
			for (int key : bestElementPerTw.keySet()) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(key);

				// Only consider time window, if accepted number is below
				// maximum expected value
				if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
						.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1 <= this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea()).get(tw)) {

					if (GRILSOrienteeringAdaptedWithVehicleAreaAssignment.minimumExpectedDemandAsBound) {
						if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw)
								+ 1 <= (this.expectedNumberOfArrivals
										* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
										* this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
												.get(request.getCustomer().getTempDeliveryArea()).get(tw)
										+ this.expectedNumberOfArrivals
												* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
												* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
														.get(request.getCustomer().getTempDeliveryArea()).get(tw))
										/ 2.0) {
							completelyOkay.add(bestElementPerTw.get(key));
						} else {
							onlyIfNeeded.add(bestElementPerTw.get(key));
						}
					} else {
						completelyOkay.add(bestElementPerTw.get(key));
					}
				}
			}

			if (completelyOkay.size() > 0) {
				int selectedTw = new Random().nextInt(completelyOkay.size());
				double value=orderValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(request.getCustomer().getOriginalDemandSegmentId())
						.get(completelyOkay.get(selectedTw).getOrder().getTimeWindowFinalId());
				if (this.squaredValue == 1.0) {
					value = Math.pow(value, 2);
				}
				completelyOkay.get(selectedTw)
						.setTempValue(value);
				completelyOkay.get(selectedTw)
						.setTempAdditionalCostsValue(completelyOkay.get(selectedTw).getTempShift());
				return completelyOkay.get(selectedTw);
			}
			if (onlyIfNeeded.size() > 0 && minimumExpectedDemandAsBound) {
				int selectedTw = new Random().nextInt(onlyIfNeeded.size());

				double reducedValue = orderValue
						* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
								.get(request.getCustomer().getOriginalDemandSegmentId())
								.get(completelyOkay.get(selectedTw).getOrder().getTimeWindowFinalId())
						* (this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea())
										.get(onlyIfNeeded.get(selectedTw).getOrder().getTimeWindowFinal())
								- (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea())
										.get(onlyIfNeeded.get(selectedTw).getOrder().getTimeWindowFinal()) + 1))
						/ (this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea())
										.get(onlyIfNeeded.get(selectedTw).getOrder().getTimeWindowFinal())
								- this.expectedNumberOfArrivals
										* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
										* this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
												.get(request.getCustomer().getTempDeliveryArea())
												.get(onlyIfNeeded.get(selectedTw).getOrder().getTimeWindowFinal()));
				
				if (this.squaredValue == 1.0) {
					reducedValue = Math.pow(reducedValue, 2);
				}
				onlyIfNeeded.get(selectedTw).setTempValue(reducedValue);
				onlyIfNeeded.get(selectedTw).setTempAdditionalCostsValue(onlyIfNeeded.get(selectedTw).getTempShift());
				return onlyIfNeeded.get(selectedTw);
			}
		}

		// Choose randomly
		int selectedTw = new Random().nextInt(bestElementPerTw.keySet().size());

		int currentId = 0;
		for (int key : bestElementPerTw.keySet()) {
			if (currentId == selectedTw) {
				double value;
				if (this.considerDemandProbability == 1) {
					value=orderValue
							* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							.get(request.getCustomer().getOriginalDemandSegmentId())
							.get(bestElementPerTw.get(key).getOrder().getTimeWindowFinalId())
					* this.expectedNumberOfArrivals
					* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
					* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
							.get(request.getCustomer().getTempDeliveryArea())
							.get(bestElementPerTw.get(key).getOrder().getTimeWindowFinal())
					/ this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(request.getCustomer().getTempDeliveryArea())
							.get(bestElementPerTw.get(key).getOrder().getTimeWindowFinal());
					if (this.squaredValue == 1.0) {
						
						value = Math.pow(value, 2);
					}
					value = -1.0 * value;
				} else {
					value = orderValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							.get(request.getCustomer().getOriginalDemandSegmentId())
							.get(bestElementPerTw.get(key).getOrder().getTimeWindowFinalId());
					if (this.squaredValue == 1.0) {
						
						value = Math.pow(value, 2);
					}
				}
				bestElementPerTw.get(key).setTempValue(value);
				bestElementPerTw.get(key).setTempAdditionalCostsValue(bestElementPerTw.get(key).getTempShift());
				return bestElementPerTw.get(key);
			}

			currentId++;
		}

		return null;
	}

	private RouteElement applySelectionStrategyTimeWindowUtility(HashMap<Integer, RouteElement> bestElementPerTw,
			OrderRequest request, double orderValue) {

		RouteElement newElementOverall = new RouteElement();
		double bestValueOverall = -1.0 * Double.MAX_VALUE;

		RouteElement newElementMin = null;
		double bestValueMin = -1.0 * Double.MAX_VALUE;

		RouteElement newElementExpected = null;
		double bestValueExpected = -1.0 * Double.MAX_VALUE;

		for (int key : bestElementPerTw.keySet()) {

			// Best time window?
			double toEvaluate = orderValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
					.get(request.getCustomer().getOriginalDemandSegmentId()).get(key);
			if (toEvaluate > bestValueOverall || ((toEvaluate == bestValueOverall) && (request
					.getAlternativePreferences().get(this.alternativesToTimeWindows.get(key).getId()) > request
							.getAlternativePreferences().get(this.alternativesToTimeWindows
									.get(newElementOverall.getOrder().getTimeWindowFinalId()).getId())))) {
				newElementOverall = bestElementPerTw.get(key);
				bestValueOverall = toEvaluate;
			}

			if (this.considerDemandProbability == 1) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(key);

				// Only consider time window, if accepted number is below
				// maximum expected value
				double maximumExpected = this.expectedNumberOfArrivals
						* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw);
				if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
						.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1 <= maximumExpected) {

					if (minimumExpectedDemandAsBound) {
						// Try to not use if above minimum expected value
						double minimumExpected = this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea()).get(tw);

						if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw)
								+ 1 <= (minimumExpected + maximumExpected) / 2.0) {
							if ((toEvaluate > bestValueExpected)
									|| ((toEvaluate == bestValueExpected) && (request.getAlternativePreferences()
											.get(this.alternativesToTimeWindows.get(key).getId()) > request
													.getAlternativePreferences().get(this.alternativesToTimeWindows
															.get(newElementExpected.getOrder().getTimeWindowFinalId())
															.getId())))) {
								newElementExpected = bestElementPerTw.get(key);
								bestValueExpected = toEvaluate;
							}
						} else {
							double reducedValue = toEvaluate
									* (maximumExpected - (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1))
									/ (maximumExpected - minimumExpected);
							if ((reducedValue > bestValueMin)
									|| ((reducedValue == bestValueMin) && (request.getAlternativePreferences()
											.get(this.alternativesToTimeWindows.get(key).getId()) > request
													.getAlternativePreferences().get(this.alternativesToTimeWindows
															.get(newElementMin.getOrder().getTimeWindowFinalId())
															.getId())))) {
								newElementMin = bestElementPerTw.get(key);
								bestValueMin = reducedValue;
							}
						}
					} else {
						if ((toEvaluate > bestValueExpected)
								|| ((toEvaluate == bestValueExpected) && (request.getAlternativePreferences()
										.get(this.alternativesToTimeWindows.get(key).getId()) > request
												.getAlternativePreferences().get(this.alternativesToTimeWindows
														.get(newElementExpected.getOrder().getTimeWindowFinalId())
														.getId())))) {
							newElementExpected = bestElementPerTw.get(key);
							bestValueExpected = toEvaluate;
						}
					}
				}
			}
		}

		if (this.considerDemandProbability == 1) {
			if (newElementExpected != null) {
				if (this.squaredValue == 1.0) {
					bestValueExpected = Math.pow(bestValueExpected, 2);
				}
				newElementExpected.setTempValue(bestValueExpected);
				newElementExpected.setTempAdditionalCostsValue(newElementExpected.getTempShift());
				return newElementExpected;
			} else if (newElementMin != null && minimumExpectedDemandAsBound) {
			if (this.squaredValue == 1.0) {
				bestValueMin = Math.pow(bestValueMin, 2);
			}
				newElementMin.setTempValue(bestValueMin);
				newElementMin.setTempAdditionalCostsValue(newElementMin.getTempShift());
				return newElementMin;
			} else {
				// If we must choose a time window, for which there are already
				// enough requests, than we reduce the value with the ratio of
				// too much. The minus is the sign for the too much.
				double value = bestValueOverall * this.expectedNumberOfArrivals
						* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea())
								.get(newElementOverall.getOrder().getTimeWindowFinal())
						/ (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea())
								.get(newElementOverall.getOrder().getTimeWindowFinal()) + 1);
				if (this.squaredValue == 1.0) {
					value = Math.pow(value, 2);
				}
				newElementOverall.setTempValue(-1.0 * value);
				newElementOverall.setTempAdditionalCostsValue(newElementOverall.getTempShift());
				return newElementOverall;
			}

		}

		// Return best element
		newElementOverall.setTempValue(bestValueOverall);
		newElementOverall.setTempAdditionalCostsValue(newElementOverall.getTempShift());
		return newElementOverall;

	}

	private RouteElement applySelectionStrategyGreedy(HashMap<Integer, RouteElement> bestElementPerTw,
			OrderRequest request, int fillUpCounter) {

		RouteElement newElementOverall = new RouteElement();
		double bestValueOverall = Double.MAX_VALUE;

		RouteElement newElementMin = null;
		double bestValueMin = Double.MAX_VALUE;

		RouteElement newElementExpected = null;
		double bestValueExpected = Double.MAX_VALUE;

		for (int key : bestElementPerTw.keySet()) {

			// Best time window?
			double toEvaluate = (fillUpCounter > 0) ? bestElementPerTw.get(key).getTempShift()
					: bestElementPerTw.get(key).getTempShiftWithoutWait();
			if (toEvaluate < bestValueOverall || ((toEvaluate == bestValueOverall) && (request
					.getAlternativePreferences().get(this.alternativesToTimeWindows.get(key).getId()) > request
							.getAlternativePreferences().get(this.alternativesToTimeWindows
									.get(newElementOverall.getOrder().getTimeWindowFinalId()).getId())))) {
				newElementOverall = bestElementPerTw.get(key);
				bestValueOverall = toEvaluate;
			}

			if (this.considerDemandProbability == 1) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(key);

				// Only consider time window, if accepted number is below
				// maximum expected value
				double maximumExpected = this.expectedNumberOfArrivals
						* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw);
				if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
						.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1 <= maximumExpected) {

					if (minimumExpectedDemandAsBound) {
						// Try to not use if above minimum expected value
						double minimumExpected = this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea()).get(tw);

						if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw)
								+ 1 <= (minimumExpected + maximumExpected) / 2.0) {
							if ((toEvaluate < bestValueExpected)
									|| ((toEvaluate == bestValueExpected) && (request.getAlternativePreferences()
											.get(this.alternativesToTimeWindows.get(key).getId()) > request
													.getAlternativePreferences().get(this.alternativesToTimeWindows
															.get(newElementExpected.getOrder().getTimeWindowFinalId())
															.getId())))) {
								newElementExpected = bestElementPerTw.get(key);
								bestValueExpected = toEvaluate;
							}
						} else {
							if ((toEvaluate < bestValueMin)
									|| ((toEvaluate == bestValueMin) && (request.getAlternativePreferences()
											.get(this.alternativesToTimeWindows.get(key).getId()) > request
													.getAlternativePreferences().get(this.alternativesToTimeWindows
															.get(newElementMin.getOrder().getTimeWindowFinalId())
															.getId())))) {
								newElementMin = bestElementPerTw.get(key);
								bestValueMin = toEvaluate;
							}
						}
					} else {
						if ((toEvaluate < bestValueExpected)
								|| ((toEvaluate == bestValueExpected) && (request.getAlternativePreferences()
										.get(this.alternativesToTimeWindows.get(key).getId()) > request
												.getAlternativePreferences().get(this.alternativesToTimeWindows
														.get(newElementExpected.getOrder().getTimeWindowFinalId())
														.getId())))) {
							newElementExpected = bestElementPerTw.get(key);
							bestValueExpected = toEvaluate;
						}
					}
				}
			}
		}

		if (this.considerDemandProbability == 1) {
			if (newElementExpected != null) {
				newElementExpected.setTempAdditionalCostsValue(bestValueExpected);
				return newElementExpected;
			} else if (newElementMin != null && minimumExpectedDemandAsBound) {
				newElementMin.setTempAdditionalCostsValue(bestValueMin * UNEXPECTED_MULTIPLIER);
				return newElementMin;
			}

		}

		// Return best element
		newElementOverall.setTempAdditionalCostsValue(bestValueOverall);
		return newElementOverall;
	}

	/**
	 * Chooses the feasible time window with the best popularity ratio
	 * (demand/capacity)
	 * 
	 * @param bestElementPerTw
	 *            Hashmap with key: tw id, entry: best Route element
	 * @return Route element with best popularity ratio * shift
	 */
	private RouteElement getBestElementByTwPopularityRatioOld(HashMap<Integer, RouteElement> bestElementPerTw) {

		HashMap<Integer, Integer> timeWindowPopularity = new HashMap<Integer, Integer>();
		HashMap<Integer, Double> timeWindowCapacity = new HashMap<Integer, Double>();

		// Get popularity by time window
		for (OrderRequest or : this.orderRequests) {
			// Possible time windows for request
			Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
					.getAlternativeSet().getNoPurchaseAlternative();

			for (int altId : or.getAlternativePreferences().keySet()) {

				if (or.getAlternativePreferences().get(altId) > or.getAlternativePreferences()
						.get(noPurchaseAlternative.getId())) { // TODO Will
																// crash if no
																// no-purchase
																// altnerative
					for (TimeWindow tw : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
							.getAlternativeSet().getAlternativeById(altId).getTimeWindows()) {
						int twId = tw.getId();
						if (timeWindowPopularity.containsKey(twId)) {
							timeWindowPopularity.put(twId, timeWindowPopularity.get(twId) + 1);
						} else {
							timeWindowPopularity.put(twId, 1);
						}
					}
					;
				}
			}
			// TODO: Delete duplicate TW
		}

		// Calculate free capacity per tw
		for (TimeWindow e : this.timeWindowSet.getElements()) {

			double freeCapacity = 0;
			double timeWindowStart = (e.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
					* TIME_MULTIPLIER;
			double timeWindoWEnd = (e.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
					* TIME_MULTIPLIER;

			for (int routeId : this.routes.keySet()) {

				ArrayList<RouteElement> route = this.routes.get(routeId);
				if ((this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
						- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER > timeWindowStart
						&& (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
								- this.timeWindowSet.getTempStartOfDeliveryPeriod())
								* TIME_MULTIPLIER < timeWindoWEnd) {
					int firstElementAfter = route.size() - 1;
					int lastElementInside = 0;
					// Find first after start
					for (int elementId = 1; elementId < route.size() - 1; elementId++) {
						if (route.get(elementId).getServiceBegin()
								+ route.get(elementId).getServiceTime() >= timeWindowStart) {
							firstElementAfter = elementId;
							break;
						}
					}

					if (firstElementAfter == route.size() - 1) {
						freeCapacity = Math.min(timeWindoWEnd,
								(this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
										- this.timeWindowSet.getTempStartOfDeliveryPeriod())
										* TIME_MULTIPLIER)
								- Math.max(timeWindowStart,
										(this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
												- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER);
						if (this.includeDriveFromStartingPosition == 1) {
							// If the driving time is larger than the difference
							// between tw end and tour end, it needs to be
							// reduced from the capacity
							if ((this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
									- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
									- route.get(firstElementAfter).getTravelTimeTo() < timeWindoWEnd) {
								freeCapacity -= (timeWindoWEnd
										- (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
												- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
										- route.get(firstElementAfter).getTravelTimeTo());
							}
						}
					} else {

						// Find last within tw
						for (int elementId = route.size() - 1; elementId > firstElementAfter - 1; elementId--) {

							// If element cannot be pushed outside the time
							// window, it is relevant for the free capacity
							// calculation
							if (route.get(elementId).getServiceBegin()
									+ route.get(elementId).getSlack() <= timeWindoWEnd) {
								lastElementInside = elementId;
								break;
							}
						}

						if (lastElementInside == 0) {
							freeCapacity = Math.min(timeWindoWEnd,
									(this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
											- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER)
									- Math.max(timeWindowStart,
											(this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
													- this.timeWindowSet.getTempStartOfDeliveryPeriod())
													* TIME_MULTIPLIER);
							if (this.includeDriveFromStartingPosition == 1) {
								// If the next is the depot again, than minus
								// the travel time
								if (lastElementInside + 1 == route.size() - 1) {
									freeCapacity -= route.get(lastElementInside).getTravelTimeFrom();
								} else {
									// If the driving time cannot be pushed
									// outside the time frame, the left-over
									// needs to be subtracted
									if (route.get(lastElementInside + 1).getServiceBegin() - timeWindoWEnd
											+ route.get(lastElementInside + 1).getSlack() < route.get(lastElementInside)
													.getTravelTimeFrom()) {
										freeCapacity -= (route.get(lastElementInside).getTravelTimeFrom()
												- (route.get(lastElementInside + 1).getServiceBegin() - timeWindoWEnd)
												- route.get(lastElementInside + 1).getSlack());
									}
								}

							}
						} else {

							freeCapacity = timeWindoWEnd - timeWindowStart;

							for (int inside = firstElementAfter + 1; inside < lastElementInside; inside++) {
								freeCapacity -= route.get(inside).getServiceTime()
										- route.get(inside).getTravelTimeFrom();
							}

							// Last element:
							double timeInsideTw = Math.min(
									timeWindoWEnd - (route.get(lastElementInside).getServiceBegin()
											+ route.get(lastElementInside).getSlack()),
									route.get(lastElementInside).getServiceTime()
											+ route.get(lastElementInside).getTravelTimeFrom());
							freeCapacity -= timeInsideTw;

							// First element:
							timeInsideTw = route.get(firstElementAfter).getServiceBegin()
									+ route.get(firstElementAfter).getServiceTime()
									+ route.get(firstElementAfter).getTravelTimeFrom() - timeWindowStart;
							if (route.get(firstElementAfter).getServiceBegin() > timeWindowStart) {
								freeCapacity -= Math.min(route.get(firstElementAfter).getTravelTimeTo(),
										route.get(firstElementAfter).getServiceBegin() - timeWindowStart);
								freeCapacity -= route.get(firstElementAfter).getServiceTime()
										- route.get(firstElementAfter).getTravelTimeFrom();
							} else {
								freeCapacity -= timeInsideTw;
							}
						}
					}
					// Add to overall free capacity
					if (timeWindowCapacity.containsKey(e.getId())) {
						timeWindowCapacity.put(e.getId(), timeWindowCapacity.get(e.getId()) + freeCapacity);
					} else {
						timeWindowCapacity.put(e.getId(), freeCapacity);
					}
				}
			}

		}

		// Calculate ratio by time window and select best
		double bestRatio = Double.MAX_VALUE;
		RouteElement bestElement = null;

		for (int twKey : bestElementPerTw.keySet()) {
			if (bestElementPerTw.get(twKey).getTempShift() * timeWindowPopularity.get(twKey)
					/ timeWindowCapacity.get(twKey) < bestRatio) {
				bestRatio = bestElementPerTw.get(twKey).getTempShift() * timeWindowPopularity.get(twKey)
						/ timeWindowCapacity.get(twKey);
				bestElement = bestElementPerTw.get(twKey);

			}
		}
		// System.out.println("Is best element null?"+(bestElement==null));
		return bestElement;
	}

	private void determineRemainingCapacity() {
		this.remainingCapacityPerTimeWindow = new HashMap<Integer, Double>();

		for (int routeId : this.routes.keySet()) {
			HashMap<Integer, Double> highestSlackPerTwAndRoute = new HashMap<Integer, Double>();
			for (int reId = 1; reId < this.routes.get(routeId).size() - 1; reId++) {

				if (!highestSlackPerTwAndRoute
						.containsKey(this.routes.get(routeId).get(reId).getOrder().getTimeWindowFinalId())) {
					highestSlackPerTwAndRoute.put(this.routes.get(routeId).get(reId).getOrder().getTimeWindowFinalId(),
							this.routes.get(routeId).get(reId).getSlack());
				}

				if (highestSlackPerTwAndRoute
						.get(this.routes.get(routeId).get(reId).getOrder().getTimeWindowFinalId()) < this.routes
								.get(routeId).get(reId).getSlack()) {
					highestSlackPerTwAndRoute.put(this.routes.get(routeId).get(reId).getOrder().getTimeWindowFinalId(),
							this.routes.get(routeId).get(reId).getSlack());
				}

			}

			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				if (!this.remainingCapacityPerTimeWindow.containsKey(tw.getId())) {
					this.remainingCapacityPerTimeWindow.put(tw.getId(), 0.0);
				}
				if (highestSlackPerTwAndRoute.containsKey(tw.getId())) {

					this.remainingCapacityPerTimeWindow.put(tw.getId(),
							this.remainingCapacityPerTimeWindow.get(tw.getId())
									+ highestSlackPerTwAndRoute.get(tw.getId()));

				} else {
					this.remainingCapacityPerTimeWindow.put(tw.getId(),
							this.remainingCapacityPerTimeWindow.get(tw.getId())
									+ (tw.getEndTime() - tw.getStartTime()) * TIME_MULTIPLIER);
				}
			}

		}
	}

	/**
	 * Chooses the feasible time window with the best popularity ratio
	 * (demand/capacity)
	 * 
	 * @param bestElementPerTw
	 *            Hashmap with key: tw id, entry: best Route element
	 * @return Route element with best popularity ratio * shift
	 */
	private RouteElement getBestElementByTwPopularityRatio(HashMap<Integer, RouteElement> bestElementPerTw,
			OrderRequest request, int fillUpCounter, double orderValue) {

		// Calculate ratio by time window and select best
		double bestRatioOverall = -1.0 * Double.MAX_VALUE;
		RouteElement bestElementOverall = null;
		// double bestShiftOverall = 0;

		double bestRatioMin = -1.0 * Double.MAX_VALUE;
		RouteElement bestElementMin = null;
		double reducedValueMin = 0;
		// double bestShiftMin = 0;

		double bestRatioExpected = -1.0 * Double.MAX_VALUE;
		RouteElement bestElementExpected = null;
		// double bestShiftExpected = 0;

		for (int twKey : bestElementPerTw.keySet()) {
			// if (!this.remainingCapacityPerTimeWindow.containsKey(twKey)) {
			// this.remainingCapacityPerTimeWindow.put(twKey,
			// this.maximumCapacityPerTimeWindow.get(twKey));
			// }
			double toEvaluate = orderValue
					* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							.get(request.getCustomer().getOriginalDemandSegmentId()).get(twKey)
					* this.remainingCapacityPerTimeWindow.get(twKey) / this.popularityPerTimeWindow.get(twKey);
			if (toEvaluate > bestRatioOverall) {
				bestRatioOverall = toEvaluate;
				bestElementOverall = bestElementPerTw.get(twKey);

			}

			if (this.considerDemandProbability == 1) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(twKey);

				// Only consider time window, if accepted number is below
				// maximum expected value
				double maximumExpected = this.expectedNumberOfArrivals
						* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw);
				if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
						.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1 <= maximumExpected) {

					if (minimumExpectedDemandAsBound) {
						// Try to not use if above minimum expected value
						double minimumExpected = this.expectedNumberOfArrivals
								* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
								* this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
										.get(request.getCustomer().getTempDeliveryArea()).get(tw);

						if (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea()).get(tw)
								+ 1 <= (minimumExpected + maximumExpected) / 2.0) {
							if (toEvaluate > bestRatioExpected) {
								bestRatioExpected = toEvaluate;
								bestElementExpected = bestElementPerTw.get(twKey);
								// bestShiftExpected = toEvaluate;

							}
						} else {

							double reducedValue = toEvaluate
									* (maximumExpected - (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1))
									/ (maximumExpected - minimumExpected);

							if (reducedValue > bestRatioMin) {
								bestRatioMin = reducedValue;
								bestElementMin = bestElementPerTw.get(twKey);
								reducedValueMin = orderValue
										* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
												.get(request.getCustomer().getOriginalDemandSegmentId()).get(twKey)
										* (maximumExpected - (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
												.get(request.getCustomer().getTempDeliveryArea()).get(tw) + 1))
										/ (maximumExpected - minimumExpected);
								// bestShiftMin = toEvaluate;

							}
						}
					} else {
						if (toEvaluate > bestRatioExpected) {
							bestRatioExpected = toEvaluate;
							bestElementExpected = bestElementPerTw.get(twKey);
							// bestShiftExpected = toEvaluate;

						}
					}
				}
			}
		}

		if (this.considerDemandProbability == 1) {
			if (bestElementExpected != null) {
				double value=orderValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(request.getCustomer().getOriginalDemandSegmentId())
						.get(bestElementExpected.getOrder().getTimeWindowFinalId());
				if (this.squaredValue == 1.0) {
					
					value = Math.pow(value, 2);
				}
				bestElementExpected
						.setTempValue(value);
				bestElementExpected.setTempAdditionalCostsValue(bestElementExpected.getTempShift());
				return bestElementExpected;
			} else if (bestElementMin != null && minimumExpectedDemandAsBound) {
				double value=reducedValueMin;
				if (this.squaredValue == 1.0) {
					
					value = Math.pow(value, 2);
				}
				
				bestElementMin.setTempValue(value);
				bestElementMin.setTempAdditionalCostsValue(bestElementMin.getTempShift());
				return bestElementMin;
			} else {
				double saturationRatio = this.expectedNumberOfArrivals
						* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea())
								.get(bestElementOverall.getOrder().getTimeWindowFinal())
						/ (this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
								.get(request.getCustomer().getTempDeliveryArea())
								.get(bestElementOverall.getOrder().getTimeWindowFinal()) + 1);
				double redValue =orderValue
						* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(request.getCustomer().getOriginalDemandSegmentId())
						.get(bestElementOverall.getOrder().getTimeWindowFinalId())
				* saturationRatio;
				if(this.squaredValue==1){
					redValue=Math.pow(redValue,2);
				}
				 redValue = -1.0 * redValue;
				bestElementOverall.setTempValue(redValue);
				bestElementOverall.setTempAdditionalCostsValue(bestElementOverall.getTempShift());
				return bestElementOverall;
			}

		}

		// Return best element
		if(this.squaredValue==1){
			bestRatioOverall=Math.pow(bestRatioOverall,2);
		}
		bestElementOverall.setTempValue(bestRatioOverall);
		bestElementOverall.setTempAdditionalCostsValue(bestElementOverall.getTempShift());
		return bestElementOverall;
	}

	/**
	 * Helper method: Finds the cheapest insertion option of a specific route
	 * and returns a route element with the respective information. Returns null
	 * if no insertion is feasible. Ties are always solved randomly.
	 * 
	 * @param route
	 *            Route to insert the request into
	 * @param routeId
	 *            Respective route id
	 * @param request
	 *            Request that has to be inserted
	 * @return
	 */
	private HashMap<Integer, RouteElement> getCheapestInsertionElementByRoute(ArrayList<RouteElement> route,
			int routeId, OrderRequest request, boolean actualPreferences) {

		// Possible time windows for request
		// No purchase alternative
		Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getNoPurchaseAlternative();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
if(!actualPreferences){
		for (int altId : request.getAlternativePreferences().keySet()) {
			if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
					.get(noPurchaseAlternative.getId())) {
				timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
			}
		}
}else{
	for(ConsiderationSetAlternative csa: request.getCustomer().getOriginalDemandSegment().getConsiderationSet()){
		if (!csa.getAlternative().getNoPurchaseAlternative()) {
			timeWindows.addAll(csa.getAlternative().getTimeWindows());
		}
	}
}

		HashMap<Integer, RouteElement> bestOptionsPerTW = new HashMap<Integer, RouteElement>();

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			double travelTimeTo;
			double travelTimeFrom;
			double travelTimeOld;
			if (this.directDistances != 1) {
				travelTimeTo = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(request.getCustomer().getClosestNodeId());
				travelTimeFrom = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
				travelTimeOld = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
			} else {
				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
			}

			Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
			double shiftWithoutWait;
			Double bufferWithTravelTime = null;
			if (position == 1 && (position == route.size() - 1) && includeDriveFromStartingPosition != 1) {
				shiftWithoutWait = expectedServiceTime;
				maximumPushOfNext += expectedServiceTime - 1;
				bufferWithTravelTime = expectedServiceTime + travelTimeTo + travelTimeFrom - travelTimeOld;
			} else if (position == 1 && this.includeDriveFromStartingPosition != 1) {
				// If we are at first position after depot, the travel time
				// to takes nothing from the capacity
				shiftWithoutWait = travelTimeFrom + expectedServiceTime;
			} else if (position == route.size() - 1 && this.includeDriveFromStartingPosition != 1) {
				// If we are at last position before depot, the travel time
				// from takes nothing from the capacity
				shiftWithoutWait = travelTimeTo + expectedServiceTime;

				// And the depot arrival can be shifted by the service time
				// (service time only needs to start before end)
				maximumPushOfNext += expectedServiceTime - 1;
			} else {
				shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
			}

			// If shift without wait is already larger than the allowed
			// push, do not consider position further
			if (maximumPushOfNext >= shiftWithoutWait) {

				double earliestStart;
				if (position == 1 && this.includeDriveFromStartingPosition != 1) {
					earliestStart = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER;
				} else {
					earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
				}

				double latestStart = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack()
						- this.expectedServiceTime;
				if (position == route.size() - 1 && this.includeDriveFromStartingPosition != 1) {
					latestStart = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
							- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER - 1;
				}

				// Go through possible time windows and check if best value for
				// time window -> update

				for (TimeWindow tw : timeWindows) {

					if (((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER > earliestStart)
							&& ((tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
									* TIME_MULTIPLIER < latestStart)) {
						double wait = Math.max(0f,
								(tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
										* TIME_MULTIPLIER - earliestStart);

						double shift = shiftWithoutWait + wait;

						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							boolean useTimeWindow = !bestOptionsPerTW.containsKey(tw.getId());
							// if(fillUpCounter>0){
							if (!useTimeWindow) {
								double comparisonShift;
								if (bestOptionsPerTW.get(tw.getId()).getTempShiftWithDepotDistance() != null) {
									comparisonShift = bestOptionsPerTW.get(tw.getId()).getTempShiftWithDepotDistance();
								} else {
									comparisonShift = bestOptionsPerTW.get(tw.getId()).getTempShift();
								}
								if (bufferWithTravelTime != null) {
									useTimeWindow = (bufferWithTravelTime + wait < comparisonShift
											|| ((bufferWithTravelTime + wait == comparisonShift)
													&& new Random().nextBoolean()));
								} else {
									useTimeWindow = (shift < comparisonShift
											|| ((shift == comparisonShift) && new Random().nextBoolean()));
								}
							}

							// Update element for time windows if better
							if (useTimeWindow) {
								// if(maximumPushOfNext>25&&shiftWithoutWait<31&&unpopularTW&&request.getCustomer().getOriginalDemandSegmentId().equals(53))
								// System.out.println("I produce element");

								RouteElement newElement = new RouteElement();
								Order newOrder = new Order();
								newOrder.setOrderRequest(request);
								newOrder.setOrderRequestId(request.getId());
								newElement.setOrder(newOrder);
								newElement.setServiceTime(this.expectedServiceTime);
								newElement.setTempRoute(routeId);
								newElement.setTempPosition(position);
								newElement.getOrder().setTimeWindowFinalId(tw.getId());
								newElement.setTempShift(shift);
								newElement.setTempShiftWithoutWait(shiftWithoutWait);
								newElement.setTravelTimeTo(travelTimeTo);
								newElement.setTravelTimeFrom(travelTimeFrom);
								newElement.setWaitingTime(wait);
								newElement.setServiceBegin(earliestStart + wait);
								if (bufferWithTravelTime != null)
									newElement.setTempShiftWithDepotDistance(bufferWithTravelTime + wait);
								bestOptionsPerTW.put(tw.getId(), newElement);
							}
						}

					}
				}
			}

		}
		// if (bestOptionsPerTW.size()>0) System.out.println("I have an
		// option");
		return bestOptionsPerTW;

	}

	private ArrayList<RouteElement> getFeasibleInsertionElementsByRoute(ArrayList<RouteElement> route, int routeId,
			OrderRequest request) {

		// Possible time windows for request
		Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getNoPurchaseAlternative();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		for (int altId : request.getAlternativePreferences().keySet()) {
			if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
					.get(noPurchaseAlternative.getId())) {
				timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
			}
		}

		ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			double travelTimeTo;
			double travelTimeFrom;
			double travelTimeOld;
			if (this.directDistances != 1) {
				travelTimeTo = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(request.getCustomer().getClosestNodeId());
				travelTimeFrom = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
				travelTimeOld = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
			} else {
				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
			}

			Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
			double shiftWithoutWait;
			if (position == 1 && this.includeDriveFromStartingPosition != 1) {
				// If we are at first position after depot, the travel time
				// to takes nothing from the capacity
				shiftWithoutWait = travelTimeFrom + expectedServiceTime;
			} else if (position == route.size() - 1 && this.includeDriveFromStartingPosition != 1) {
				// If we are at last position before depot, the travel time
				// from takes nothing from the capacity
				shiftWithoutWait = travelTimeTo + expectedServiceTime;
				// And the depot arrival can be shifted by the service time
				// (service time only needs to start before end)
				maximumPushOfNext += expectedServiceTime - 1;
			} else {
				shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
			}

			// If shift without wait is already larger than the allowed
			// push, do not consider position further
			if (maximumPushOfNext >= shiftWithoutWait) {

				// Free time
				double earliestStart;
				if (position == 1 && this.includeDriveFromStartingPosition != 1) {
					earliestStart = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER;
				} else {
					earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
				}
				// double latestEnd = Math.min(eAfter.getServiceBegin() -
				// travelTimeFrom + eAfter.getSlack(),
				// timeWindowSet.getTempLengthOfDeliveryPeriod() - 1 +
				// this.expectedServiceTime);

				double latestEnd = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack();
				if (position == (route.size() - 1) && this.includeDriveFromStartingPosition != 1) {
					latestEnd = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
							- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER - 1
							+ this.expectedServiceTime;
				} else if (position == route.size() - 1) {
					latestEnd = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
							- timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER - travelTimeFrom;

				}

				// Possible time window insight? Choose first
				TimeWindow firstTimeWindow = null;
				double firstStart = this.timeWindowSet.getTempEndOfDeliveryPeriod();
				double wait = 0;
				for (TimeWindow tw : timeWindows) {
					if ((tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER < latestEnd - this.expectedServiceTime) {
						if ((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
								* TIME_MULTIPLIER > earliestStart) {
							if (tw.getStartTime() < firstStart) {
								double waitTemp = Math.max(0f,
										(tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
												* TIME_MULTIPLIER - earliestStart);
								if (this.expectedServiceTime + waitTemp < latestEnd - earliestStart) {
									firstTimeWindow = tw;
									firstStart = tw.getStartTime();
									wait = waitTemp;
								}

							}
						}
					}
				}
				if (firstTimeWindow != null) {

					double shift = shiftWithoutWait + wait;

					RouteElement newElement = new RouteElement();
					Order newOrder = new Order();
					newOrder.setTimeWindowFinal(firstTimeWindow);
					newOrder.setTimeWindowFinalId(firstTimeWindow.getId());
					newOrder.setOrderRequest(request);
					newOrder.setOrderRequestId(request.getId());
					newElement.setOrder(newOrder);
					newElement.setServiceTime(this.expectedServiceTime);
					newElement.setTempRoute(routeId);
					newElement.setTempPosition(position);
					newElement.setTempShift(shift);
					newElement.setTravelTimeTo(travelTimeTo);
					newElement.setTravelTimeFrom(travelTimeFrom);
					newElement.setServiceBegin(earliestStart + wait);
					newElement.setWaitingTime(wait);
					newElement.setTempSpaceAroundStart(earliestStart);
					newElement.setTempSpaceAroundEnd(latestEnd);
					possibleInsertions.add(newElement);
				}
			}

		}

		return possibleInsertions;

	}

	/**
	 * Helper function that inserts new route element and updates others
	 * 
	 * @param element
	 *            the new route element
	 * @param routes
	 *            The old status of the routes
	 */
	public void insertRouteElement(RouteElement element) {

		long startTime = System.currentTimeMillis();

		ArrayList<RouteElement> route = this.routes.get(element.getTempRoute());
		route.add(element.getTempPosition(), element);

		if (this.twSelectionValue == 2 || this.twSelectionValue == 3) {
			for (Integer altId : element.getOrder().getOrderRequest().getAlternativePreferences().keySet()) {

				if (element.getOrder().getOrderRequest().getAlternativePreferences().get(altId) > element.getOrder()
						.getOrderRequest().getAlternativePreferences().get(noPurchaseAlt.getId())) {
					for (TimeWindow tw : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
							.getAlternativeSet().getAlternativeById(altId).getTimeWindows()) {

						popularityPerTimeWindow.put(tw.getId(), popularityPerTimeWindow.get(tw.getId()) - 1);

					}
					;
				}

			}
		}

		// Update travel times
		route.get(element.getTempPosition() + 1).setTravelTimeTo(element.getTravelTimeFrom());
		route.get(element.getTempPosition() - 1).setTravelTimeFrom(element.getTravelTimeTo());

		// Update the following elements
		double currentShift = element.getTempShift();
		for (int k = element.getTempPosition() + 1; k < route.size(); k++) {
			if (currentShift == 0)
				break;
			double oldWaitingTime = route.get(k).getWaitingTime();
			route.get(k).setWaitingTime(Math.max(0, oldWaitingTime - currentShift));
			currentShift = Math.max(0, currentShift - oldWaitingTime);
			if (k != route.size() - 1) {
				route.get(k).setServiceBegin(route.get(k).getServiceBegin() + currentShift);
				route.get(k).setSlack(route.get(k).getSlack() - currentShift);
			}
		}

		// Update maxShift from current element and the ones before
		for (int k = element.getTempPosition(); k > 0; k--) {
			double maxShift = Math.min(
					(route.get(k).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(k).getServiceBegin(),
					route.get(k + 1).getWaitingTime() + route.get(k + 1).getSlack());
			if (k == route.size() - 2 && this.includeDriveFromStartingPosition != 1) {
				maxShift = (route.get(k).getOrder().getTimeWindowFinal().getEndTime()
						- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
						- route.get(k).getServiceBegin();
			} else if (k == route.size() - 2) {
				maxShift = Math.min(
						(route.get(k).getOrder().getTimeWindowFinal().getEndTime()
								- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
								- route.get(k).getServiceBegin(),
						(this.vehicleAreaAssignmentsPerVehicleNo.get(element.getTempRoute()).getEndTime()
								- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
								- this.expectedServiceTime - route.get(k).getTravelTimeFrom()
								- route.get(k).getServiceBegin());
			}
			route.get(k).setSlack(maxShift);
		}

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		averageRuntimeInsert = (averageRuntimeInsert * this.counterInsert + totalTime) / ++counterInsert;

	}

	/**
	 * Helper method that removes elements from the routes
	 * 
	 * @param routes
	 *            Old status of the routes
	 * @param numberOfRemovalsPerRoute
	 * @param startPositions
	 */
	private void removeRouteElements(int numberOfRemovalsPerRoute, int[] startPositions) {

		for (int routeId = 0; routeId < startPositions.length; routeId++) {
			this.removeRouteElementsOfRoute(this.routes.get(this.vehicleAreaAssignments.get(routeId).getVehicleNo()),
					this.vehicleAreaAssignments.get(routeId).getVehicleNo(), numberOfRemovalsPerRoute,
					startPositions[routeId], true);
		}
	}

	/**
	 * Helper method that removes elements from a specific route
	 * 
	 * @param routes
	 *            Old status of the route
	 * @param numberOfRemovalsPerRoute
	 * @param startPosition
	 * @param addToUnassigned
	 *            If the element is already planned for another root, it should
	 *            not be added to unassigned
	 */
	private void removeRouteElementsOfRoute(ArrayList<RouteElement> route, int routeId, int numberOfRemovalsPerRoute,
			int startPosition, boolean addToUnassigned) {

		int positionToRemove = startPosition;
		RouteElement removedElement = new RouteElement();
		boolean stopNow = false;

		for (int i = 0; i < numberOfRemovalsPerRoute; i++) {
			// Start at beginning if end of route is reached. Do not delete
			// depots.
			if (positionToRemove == route.size() - 1) {
				// Begin at start
				positionToRemove = 1;

				// If route is empty, stop removing
				if (route.size() == 2)
					break;

			}

			if (stopNow) {
				System.out.println("");
			}
			removedElement = route.remove(positionToRemove);

			if (addToUnassigned) {

				this.orderRequests.add(removedElement.getOrder().getOrderRequest());
				bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
						.get(removedElement.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
						.put(removedElement.getOrder().getTimeWindowFinal(),
								bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
										.get(removedElement.getOrder().getOrderRequest().getCustomer()
												.getTempDeliveryArea())
										.get(removedElement.getOrder().getTimeWindowFinal()) - 1);
				bufferAlreadyAcceptedPerDeliveryArea
						.put(removedElement.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea(),
								bufferAlreadyAcceptedPerDeliveryArea.get(
										removedElement.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea())
										- 1);
				if (this.twSelectionValue == 2 || this.twSelectionValue == 3) {
					for (Integer altId : removedElement.getOrder().getOrderRequest().getAlternativePreferences()
							.keySet()) {

						if (removedElement.getOrder().getOrderRequest().getAlternativePreferences()
								.get(altId) > removedElement.getOrder().getOrderRequest().getAlternativePreferences()
										.get(noPurchaseAlt.getId())) {
							for (TimeWindow tw : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
									.getAlternativeSet().getAlternativeById(altId).getTimeWindows()) {

								popularityPerTimeWindow.put(tw.getId(), popularityPerTimeWindow.get(tw.getId()) + 1);

							}
							;
						}

					}
				}
			}

		}

		// Forward: Update waiting time and service time begin

		/// Calculate travel time from first unchanged to the first that
		/// needs to be shifted forward.
		double travelTimeTo;
		if (this.directDistances != 1) {
			travelTimeTo = this.distanceMatrix
					.get(route.get(positionToRemove - 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId())
					.get(route.get(positionToRemove).getOrder().getOrderRequest().getCustomer().getClosestNodeId());
		} else {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					route.get(positionToRemove - 1).getOrder().getOrderRequest().getCustomer(),
					route.get(positionToRemove).getOrder().getOrderRequest().getCustomer())
					/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
		}

		route.get(positionToRemove - 1).setTravelTimeFrom(travelTimeTo);
		route.get(positionToRemove).setTravelTime(travelTimeTo);

		for (int i = positionToRemove; i < route.size(); i++) {
			RouteElement eBefore = route.get(i - 1);
			RouteElement eNew = route.get(i);

			// Update service begin and waiting time

			double arrivalTime;
			// If it is the first element, the service can begin at the
			// start of the time window
			if (i == 1 && this.includeDriveFromStartingPosition != 1) {
				arrivalTime = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getStartTime()
						- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER;
			} else {
				arrivalTime = eBefore.getServiceBegin() + eBefore.getServiceTime() + eBefore.getTravelTimeFrom();
			}
			double oldServiceBegin = eNew.getServiceBegin();
			double newServiceBegin;
			if (i < route.size() - 1) {
				newServiceBegin = Math.max(arrivalTime, (eNew.getOrder().getTimeWindowFinal().getStartTime()
						- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER);
			} else {
				// For end depot
				newServiceBegin = eNew.getServiceBegin();
			}

			eNew.setServiceBegin(newServiceBegin);
			eNew.setWaitingTime(newServiceBegin - arrivalTime);
			if (i == route.size() - 1) {
				eNew.setWaitingTime(Math.max(0, newServiceBegin - arrivalTime));
			}

			// If the service begin does not change, the following elements
			// can stay as before
			if (oldServiceBegin == newServiceBegin)
				break;
		}

		/// Calculate travel time from last to depot.
		if (this.directDistances != 1) {
			travelTimeTo = this.distanceMatrix
					.get(route.get(route.size() - 2).getOrder().getOrderRequest().getCustomer().getClosestNodeId())
					.get(route.get(route.size() - 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId());
		} else {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					route.get(route.size() - 2).getOrder().getOrderRequest().getCustomer(),
					route.get(route.size() - 1).getOrder().getOrderRequest().getCustomer())
					/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;

		}
		route.get(route.size() - 2).setTravelTimeFrom(travelTimeTo);
		route.get(route.size() - 1).setTravelTime(travelTimeTo);

		// Backward: Update maximum shift (slack)

		/// For last before end depot (if it is not the begin depot)
		if (route.size() > 2) {
			double lastEndTime = (this.vehicleAreaAssignmentsPerVehicleNo.get(routeId).getEndTime()
					- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER;
			if (this.includeDriveFromStartingPosition == 1) {
				lastEndTime -= route.get(route.size() - 2).getTravelTimeFrom();
			}

			Double maxShiftEnd = Math.min(lastEndTime - route.get(route.size() - 2).getServiceBegin(),
					(route.get(route.size() - 2).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(route.size() - 2).getServiceBegin());

			if (this.includeDriveFromStartingPosition == 1) {
				maxShiftEnd = Math.min(
						lastEndTime - route.get(route.size() - 2).getServiceBegin() - expectedServiceTime,
						(route.get(route.size() - 2).getOrder().getTimeWindowFinal().getEndTime()
								- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
								- route.get(route.size() - 2).getServiceBegin());
			}

			route.get(route.size() - 2).setSlack(maxShiftEnd);
		}

		/// For others
		for (int i = route.size() - 3; i > 0; i--) {
			Double maxShift = Math.min(
					(route.get(i).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(i).getServiceBegin(),
					route.get(i + 1).getWaitingTime() + route.get(i + 1).getSlack());
			route.get(i).setSlack(maxShift);
		}

	}

	private void initialiseRoutes() {

		this.routes = new HashMap<Integer, ArrayList<RouteElement>>();
		this.vehicleAreaAssignmentsPerVehicleNo = new HashMap<Integer, VehicleAreaAssignment>();
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(timeWindowSet, TIME_MULTIPLIER);
		for (VehicleAreaAssignment ass : this.vehicleAreaAssignments) {
			this.vehicleAreaAssignmentsPerVehicleNo.put(ass.getVehicleNo(), ass);
			ArrayList<RouteElement> route = new ArrayList<RouteElement>();
			RouteElement startE = new RouteElement();
			startE.setServiceBegin(
					(ass.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER);
			startE.setServiceTime(0.0);
			startE.setSlack(0.0);
			startE.setWaitingTime(0.0);
			startE.setTravelTimeTo(0.0);
			OrderRequest startR = new OrderRequest();
			Customer startCustomer = new Customer();
			startCustomer.setLat(ass.getStartingLocationLat());
			startCustomer.setLon(ass.getStartingLocationLon());
			startR.setCustomer(startCustomer);
			Order order = new Order();
			order.setOrderRequest(startR);
			startE.setOrder(order);
			route.add(startE);

			RouteElement endE = new RouteElement();
			endE.setServiceTime(0.0);

			endE.setServiceBegin((ass.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER);
			endE.setSlack(0.0);
			endE.setTravelTimeFrom(0.0);
			OrderRequest endR = new OrderRequest();
			Customer endCustomer = new Customer();

			endCustomer.setLat(ass.getEndingLocationLat());
			endCustomer.setLon(ass.getEndingLocationLon());

			endR.setCustomer(endCustomer);
			Order endOrder = new Order();
			endOrder.setOrderRequest(endR);
			endE.setOrder(endOrder);
			route.add(endE);

			// Determine travel time between end and start location and set
			// respective values in end element
			double travelTimeBetween;

			if (this.directDistances != 1) {
				Node closestNodeStart = LocationService.findClosestNode(this.nodes, ass.getStartingLocationLat(),
						ass.getStartingLocationLon());
				startCustomer.setClosestNode(closestNodeStart);
				Node closestNodeEnd = LocationService.findClosestNode(this.nodes, ass.getEndingLocationLat(),
						ass.getEndingLocationLon());
				endCustomer.setClosestNode(closestNodeEnd);
				travelTimeBetween = this.distanceMatrix.get(closestNodeStart.getLongId())
						.get(closestNodeEnd.getLongId());
			} else {
				travelTimeBetween = LocationService.calculateHaversineDistanceBetweenCustomers(startCustomer,
						endCustomer) / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
			}

			startE.setTravelTimeFrom(travelTimeBetween);
			endE.setTravelTimeTo(travelTimeBetween);

			if (this.includeDriveFromStartingPosition != 1) {
				endE.setWaitingTime((ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER);
			} else {
				endE.setWaitingTime((ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER - travelTimeBetween);
			}

			routes.put(ass.getVehicleNo(), route);
		}

	}

	private RouteElement getRouteElementforRequestWithBestNeighborhood(int neighborhoodSize) {

		RouteElement bestElement = null;
		double bestValue = 0;
		for (OrderRequest request : this.orderRequests) {
			RouteElement possibleElement = this.calculateNeighborhoodValueOfRequest(request, neighborhoodSize);
			if (possibleElement != null && possibleElement.getTempNeihborhoodValue() > bestValue) {
				bestElement = possibleElement;
				bestValue = possibleElement.getTempNeihborhoodValue();
			}
			;
		}
		return bestElement;

	}

	private RouteElement calculateNeighborhoodValueOfRequest(OrderRequest request, int neighborhoodSize) {

		// Find the closest unassigned requests
		@SuppressWarnings("unchecked")
		ArrayList<OrderRequest> neighbors = (ArrayList<OrderRequest>) this.orderRequests.clone();

		if (this.directDistances != 1) {
			Collections.sort(neighbors, new OrderRequestNodeDistanceComparator(request, this.distanceMatrix));
		} else {
			Collections.sort(neighbors, new OrderRequestDistanceComparator(request));

		}
		// Get insertion costs and slack for feasible insertion positions
		ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();

		for (int routeId : this.routes.keySet()) {
			possibleInsertions
					.addAll(this.getFeasibleInsertionElementsByRoute(this.routes.get(routeId), routeId, request));
		}

		// Calculate value of neighbors for the insertion options. Return best.
		double bestValue = 0;
		RouteElement bestElement = null;
		int currentOptionNumber = 0;
		for (RouteElement option : possibleInsertions) {
			int feasibleNeighbors = 0;
			double neighborhoodValue = 0;
			int currentNeighbornumber = 0;
			currentOptionNumber++;
			for (OrderRequest neighbor : neighbors) {
				currentNeighbornumber++;
				if (feasibleNeighbors >= neighborhoodSize)
					break;

				// Possible time windows for request
				Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getNoPurchaseAlternative();
				ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
				for (int altId : request.getAlternativePreferences().keySet()) {
					if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
							.get(noPurchaseAlternative.getId())) {
						timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
								.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
					}
				}

				boolean isPossible = false;
				for (TimeWindow tw : timeWindows) {

					if ((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER >= option.getTempSpaceAroundStart()
							&& (tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
									* TIME_MULTIPLIER <= option.getTempSpaceAroundEnd()) {
						isPossible = true;
						break;
					}
				}

				if (isPossible) {
					feasibleNeighbors++;

					// Add value of neighbor (score (2) / (distance + service
					// time))
					double distance;
					if (this.directDistances != 1) {
						distance = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
								.get(neighbor.getCustomer().getClosestNodeId());
						distance += this.distanceMatrix.get(neighbor.getCustomer().getClosestNodeId())
								.get(request.getCustomer().getClosestNodeId());
						distance = distance / 2;
					} else {
						distance = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
								neighbor.getCustomer()) / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					double score = 0;
					if (this.actualBasketValue == 1) {
						score = neighbor.getBasketValue();
					} else {
						try {
							score = ProbabilityDistributionService.getMeanByProbabilityDistribution(
									neighbor.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
						} catch (ParameterUnknownException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							System.exit(0);

						}
					}

					double timeCosts = distance / 2 + this.expectedServiceTime;
					if (this.squaredValue == 1) {
						neighborhoodValue += Math.pow(score,2) / timeCosts;
					} else {
						neighborhoodValue += score / timeCosts;
					}

				}
			}

			if (bestValue < neighborhoodValue) {
				bestElement = option;
				bestValue = neighborhoodValue;
			}
		}

		// No feasible neighborhood?
		if (bestElement == null)
			return null;
		// Return insertion option with highest valued neighborhood
		bestElement.setTempNeighborhoodValue(bestValue);
		return bestElement;
	}

	/**
	 * Checks if new solution is better than currently best solution and if yes,
	 * best solution is updated
	 * 
	 * @param newSolution
	 * @return Boolean if best solution was updated
	 */
	private Boolean updateBestSolution(HashMap<Integer, ArrayList<RouteElement>> newSolution, double newValue,
			double currentGreediness) {
		Boolean improvement = false;
		if (newValue > bestValue) {
			bestValue = newValue;
			improvement = true;
			this.bestCompletelyShaked = false;
			HashMap<Integer, ArrayList<RouteElement>> newBestSolution = this.copySolution(newSolution);

			this.bestSolution = newBestSolution;

			int lengthOfLongest = 0;
			for (int key : newBestSolution.keySet()) {
				if (newBestSolution.get(key).size() > lengthOfLongest) {
					lengthOfLongest = newBestSolution.get(key).size();
				}
			}
			this.sizeOfLongestRouteBestSolution = lengthOfLongest - 2;
			this.orderRequestsLeftOverBest = (ArrayList<OrderRequest>) this.orderRequests.clone();

			this.lastUpdateInformation = "Value: " + this.bestValue + "; Greediness: " + currentGreediness
					+ "; No. solutions: " + this.numberOfProducedSolutions;
		}

		return improvement;
	}

	private HashMap<Integer, ArrayList<RouteElement>> copySolution(HashMap<Integer, ArrayList<RouteElement>> toCopy) {
		HashMap<Integer, ArrayList<RouteElement>> newSolution = new HashMap<Integer, ArrayList<RouteElement>>();
		for (Integer routeId : toCopy.keySet()) {
			ArrayList<RouteElement> elements = toCopy.get(routeId);
			ArrayList<RouteElement> newElements = new ArrayList<RouteElement>();
			for (RouteElement e : elements) {
				RouteElement eCopy = e.copyElement();
				eCopy.setOrder(e.getOrder()); // As the order does not have
												// an id, it is not copied
												// before
				newElements.add(eCopy);
			}
			newSolution.put(routeId, newElements);
		}

		return newSolution;
	}

	private double getShiftValue(RouteElement insertionOption) {
		double insertionValue = 0;
		if (this.actualBasketValue == 1) {
			insertionValue = insertionOption.getOrder().getOrderRequest().getBasketValue();
		} else {

			try {
				insertionValue = ProbabilityDistributionService
						.getMeanByProbabilityDistribution(insertionOption.getOrder().getOrderRequest().getCustomer()
								.getOriginalDemandSegment().getBasketValueDistribution());
			} catch (ParameterUnknownException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
		}

		if (this.squaredValue == 1) {
			insertionValue = Math.pow(insertionValue, 2);
		}
		return insertionValue / insertionOption.getTempShift();
	}

	/**
	 * Evaluates a routing based on the revenue
	 * 
	 * @param solution
	 *            Routing
	 * @return Overall revenue
	 */
	private Double evaluateSolution(HashMap<Integer, ArrayList<RouteElement>> solution) {

		Double value = 0.0;
		for (Integer routeId : solution.keySet()) {
			ArrayList<RouteElement> elements = solution.get(routeId);
			for (int elementId = 1; elementId < elements.size() - 1; elementId++) {
				double elementValue = 0;
				if (this.actualBasketValue == 1) {
					elementValue = elements.get(elementId).getOrder().getOrderRequest().getBasketValue();
					;
				} else {
					try {
						elementValue = ProbabilityDistributionService
								.getMeanByProbabilityDistribution(elements.get(elementId).getOrder().getOrderRequest()
										.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
					} catch (ParameterUnknownException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
				elementValue = elementValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
								.getOriginalDemandSegmentId())
						.get(elements.get(elementId).getOrder().getTimeWindowFinalId());
				value += elementValue;
			}
		}

		return value;
	}

	public static String[] getParameterSetting() {
		return paras;
	}

	public Routing getResult() {
		return finalRouting;
	}
}
