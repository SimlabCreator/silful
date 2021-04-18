package logic.algorithm.vr.orienteering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.DistributionParameterValue;
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
import logic.utility.comparator.RouteElementTempValueDescComparator;
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

public class GRILSOrienteeringWithVehicleAreaAssignment_old implements RoutingAlgorithm {

	private OrderRequestSet orderRequestSet;
	private Double expectedServiceTime;
	private TimeWindowSet timeWindowSet;
	private final static double TIME_MULTIPLIER = 60.0;
	private final static double ACCEPTED_NUMBER_VALUE_DUMMY = 0.01;
	public static double distanceMultiplierAsTheCrowFlies = 1.5;
	private final double valueMultiplier = 100;
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
	private HashMap<Integer, ArrayList<RouteElement>> bestSolution;
	private ArrayList<OrderRequest> orderRequestsLeftOverBest;
	private Double bestValue; // Value of the best solution
	private int sizeOfLongestRouteBestSolution;
	private boolean bestCompletelyShaked;
	private String lastUpdateInformation;
	private int numberOfProducedSolutions;
	private int maximumNumberOfSolutions;
	private int squaredValue;
	private int actualBasketValue;
	private int includeDriveFromStartingPosition;
	private Routing finalRouting;
	private ArrayList<OrderRequest> orderRequests;
	private HashMap<Integer, ArrayList<RouteElement>> routes;
	private double runtimeFirstFillup = 0;
	private double averageRuntimeFillup = 0;
	private int counterFillup = 0;
	private int counterInsert = 0;
	private double averageRuntimeInsert = 0;
	private int counterCheckbyRoute = 0;
	private double averageRuntimeCheckByRoute = 0;
	private double averageRuntimeCheckByRouteFirst = 0;
	private int counterCheckbyRouteFirst = 0;
	private HashMap<Integer, Double> remainingCapacityPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectableValuePerDeliveryAreaAndTimeWindow;
	private Alternative noPurchaseAlt;
	private HashMap<DeliveryArea, Double> daLowerWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, Integer> bufferAlreadyAcceptedPerDeliveryArea;
	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private double expectedNumberOfArrivals;
	private HashMap<Integer, Alternative> alternativesToTimeWindows;
	private DemandSegmentWeighting demandSegmentWeighting;
	private DemandSegmentWeighting demandSegmentWeightingOriginal;
	private HashMap<Integer, Double> shiftValueBuffer;
	private boolean duplicateSegments;
	private HashMap<Integer, Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubSegment;

	private static String[] paras = new String[] { "greediness_upperBound", "greediness_lowerBound",
			"greediness_stepsize", "maximumRoundsWithoutImprovement", "Constant_service_time",
			"maximumNumberOfSolutions", "squaredValue", "actualBasketValue", "directDistances",
			"includeDriveFromStartingPosition", "duplicate_segments" };

	public GRILSOrienteeringWithVehicleAreaAssignment_old(Region region, OrderRequestSet orderRequestSet,
			DemandSegmentWeighting demandSegmentWeighting, TimeWindowSet timeWindowSet,
			ArrayList<VehicleAreaAssignment> vehicleAreaAssignments, DeliveryAreaSet deliveryAreaSet,
			ArrayList<Node> nodes, ArrayList<NodeDistance> distances, Double greedinessUpperBound,
			Double greedinessLowerBound, Double greedinessStepsize, Double maximumRoundsWithoutImprovement,
			Double expectedServiceTime, Double maximumNumberOfSolutions, Double squaredValue, Double actualBasketValue,
			Double directDistances, Double includeDriveFromStartingPosition, double expectedNumberOfArrivals,
			Double duplicateSegments) {
		this.region = region;
		this.deliveryAreaSet = deliveryAreaSet;
		this.orderRequestSet = orderRequestSet;
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.timeWindowSet = timeWindowSet;
		this.expectedServiceTime = expectedServiceTime;
		this.nodes = nodes;
		this.distances = distances;
		this.greedinessUpperBound = greedinessUpperBound;
		this.greedinessLowerBound = greedinessLowerBound;
		this.greedinessStepsize = greedinessStepsize;
		this.maximumRoundsWithoutImprovement = maximumRoundsWithoutImprovement.intValue();
		this.vehicleAreaAssignments = vehicleAreaAssignments;
		this.maximumNumberOfSolutions = maximumNumberOfSolutions.intValue();

		this.squaredValue = squaredValue.intValue();
		this.actualBasketValue = actualBasketValue.intValue();
		this.directDistances = directDistances.intValue();
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfProducedSolutions = 0;

		this.expectedNumberOfArrivals = expectedNumberOfArrivals;
		this.duplicateSegments = (duplicateSegments == 1);
		this.demandSegmentWeightingOriginal = demandSegmentWeighting;
	}

	public void start() {

		this.prepare();

		// Initialise best value
		this.bestValue = 0.0;

		// Outer loop: decrease greediness
		Double currentGreediness = this.greedinessUpperBound;
		while (currentGreediness >= this.greedinessLowerBound) {

			// Double currentGreediness = this.greedinessLowerBound;
			// while (currentGreediness <= this.greedinessUpperBound) {

			int noRepetitionsWithoutImprovement = 0;
			int numberOfRemovalsPerRoute = 1;

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

				// Check for improvement and update best solution as well as
				// parameters
				Double newValue = this.evaluateSolution(this.routes);
				boolean improvement = this.updateBestSolution(this.routes, newValue, currentGreediness);

				if (improvement) {
					noRepetitionsWithoutImprovement = 0;
					numberOfRemovalsPerRoute = 1;

				} else {
					noRepetitionsWithoutImprovement++;

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
				if (numberOfRemovalsPerRoute > lengthOfLongestRoute) {
					System.out.println("Start next round because already exchanged all. " + numberOfRemovalsPerRoute);
					noRestart = false;
				} else if (noRepetitionsWithoutImprovement > maximumRoundsWithoutImprovement) {
					noRestart = false;
				} else {
					// Attention: Change, R cannot be larger than the length of
					// the
					// longest route (would have no effect)
					if (numberOfRemovalsPerRoute > lengthOfLongestRoute) {
						numberOfRemovalsPerRoute = 1;
					}
				}
				this.removeRouteElements(numberOfRemovalsPerRoute, startPositions);

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
		// this.fillUpRoutes(currentGreediness, false, counterFillup, true);

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
				if (this.duplicateSegments)
					order.getOrderRequest().getCustomer()
							.setOriginalDemandSegment(order.getOrderRequest().getTempOriginalSegment());
				orders.add(order);
			}
		}
		for (OrderRequest or : this.orderRequestsLeftOverBest) {
			Order order = new Order();
			order.setAccepted(false);
			order.setOrderRequestId(or.getId());
			if (this.duplicateSegments)
				order.getOrderRequest().getCustomer()
						.setOriginalDemandSegment(order.getOrderRequest().getTempOriginalSegment());
			orders.add(order);

		}
		orderSet.setElements(orders);
		this.finalRouting.setOrderSet(orderSet);
		this.finalRouting
				.setAdditionalInformation(this.lastUpdateInformation + "; Overall: " + this.numberOfProducedSolutions);

		ObjectMapper mapper = new ObjectMapper();

		System.out.println("Average runtime fillup: " + this.averageRuntimeFillup);
		System.out.println("Runtime first fillup: " + this.runtimeFirstFillup);
		System.out.println("Average runtime insert: " + this.averageRuntimeInsert);
		System.out.println("Average runtime check by route" + averageRuntimeCheckByRoute);
		System.out.println("Average runtime check by route first fillup" + averageRuntimeCheckByRouteFirst);
	}

	private void prepare() {

		if (this.duplicateSegments) {
			this.mapOriginalSegmentToSubSegment = new HashMap<Integer, Pair<Double, Pair<DemandSegment, DemandSegment>>>();
			this.demandSegmentWeighting = new DemandSegmentWeighting();
			this.demandSegmentWeighting.setId(-1);
			DemandSegmentSet dss = ((DemandSegmentSet) this.demandSegmentWeightingOriginal.getSetEntity())
					.copyWithoutIdAndElements();
			dss.setId(-1);
			ArrayList<DemandSegment> newSegments = new ArrayList<DemandSegment>();
			ArrayList<DemandSegmentWeight> newWeights = new ArrayList<DemandSegmentWeight>();
			int initialId = -1;
			for (DemandSegmentWeight w : this.demandSegmentWeightingOriginal.getWeights()) {
				DemandSegment ds = w.getDemandSegment();

				Double splitValue = null;
				try {
					splitValue = ProbabilityDistributionService
							.getXByCummulativeDistributionQuantile(ds.getBasketValueDistribution(), 0.5);
				} catch (ParameterUnknownException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				DemandSegment ds1 = ds.copyWithoutId(-1);
				ds1.setId(initialId);
				ds1.setSetId(-1);
				ds1.setTempOriginalSegment(ds.getId());
				ds1.setSet(dss);
				DistributionParameterValue minPara = new DistributionParameterValue();
				minPara.setParameterTypeId(2);
				minPara.setValue(splitValue);
				ds1.getBasketValueDistribution().getParameterValues().add(minPara);
				newSegments.add(ds1);
				DemandSegmentWeight w1 = new DemandSegmentWeight();
				w1.setSetId(-1);
				w1.setDemandSegment(ds1);
				w1.setElementId(ds1.getId());
				w1.setWeight(w.getWeight() / 2.0);
				w1.setId(initialId--);
				newWeights.add(w1);
				DemandSegment ds2 = ds.copyWithoutId(-1);
				ds2.setId(initialId);
				ds2.setSet(dss);
				ds2.setSetId(-1);
				ds2.setTempOriginalSegment(ds.getId());
				DistributionParameterValue maxPara = new DistributionParameterValue();
				maxPara.setParameterTypeId(3);
				maxPara.setValue(splitValue);
				ds2.getBasketValueDistribution().getParameterValues().add(maxPara);
				newSegments.add(ds2);
				DemandSegmentWeight w2 = new DemandSegmentWeight();
				w2.setDemandSegment(ds2);
				w2.setWeight(w.getWeight() / 2.0);
				w2.setId(initialId--);
				w2.setElementId(ds2.getId());
				w2.setSetId(-1);
				newWeights.add(w2);
				mapOriginalSegmentToSubSegment.put(ds.getId(), new Pair<Double, Pair<DemandSegment, DemandSegment>>(
						splitValue, new Pair<DemandSegment, DemandSegment>(ds2, ds1)));

			}
			dss.setElements(newSegments);
			this.demandSegmentWeighting.setWeights(newWeights);
			this.demandSegmentWeighting.setSetEntity(dss);
		} else {
			this.demandSegmentWeighting = this.demandSegmentWeightingOriginal;
		}

		this.daLowerWeights = new HashMap<DeliveryArea, Double>();
		this.daLowerSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
				daLowerWeights, daLowerSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);

		this.alternativesToTimeWindows = new HashMap<Integer, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
			}
		}

		// Initialise the closest nodes for the requests
		ArrayList<OrderRequest> orderRequestsE = this.orderRequestSet.getElements();
		this.orderRequests = new ArrayList<OrderRequest>();
		noPurchaseAlt = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getNoPurchaseAlternative();

		this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow = new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>();
		this.bufferAlreadyAcceptedPerDeliveryArea = new HashMap<DeliveryArea, Integer>();
		this.shiftValueBuffer = new HashMap<Integer, Double>();

		double sumOfWeights = 0.0;
		for (DeliveryArea area : this.daLowerWeights.keySet()) {
			sumOfWeights += this.daLowerWeights.get(area);
			bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow.put(area, new HashMap<TimeWindow, Integer>());
			bufferAlreadyAcceptedPerDeliveryArea.put(area, 0);
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow.get(area).put(tw, 0);
			}
		}
		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
						daLowerSegmentWeightings, timeWindowSet);
		this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
						daLowerSegmentWeightings, timeWindowSet);

		this.maximumExpectableValuePerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Double>>> requestsPerTimeWindow = new HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Double>>>();

		for (OrderRequest request : orderRequestsE) {
			if (this.directDistances != 1) {
				Node closestNode = LocationService.findClosestNode(nodes, request.getCustomer().getLat(),
						request.getCustomer().getLon());
				request.getCustomer().setClosestNodeId(closestNode.getLongId());
				request.getCustomer().setClosestNode(closestNode);
				// T: Add distance to closest node
			}
			DemandSegment segment = request.getCustomer().getOriginalDemandSegment();
			request.setTempOriginalSegment(segment);
			if (this.duplicateSegments) {
				if (request.getBasketValue() > this.mapOriginalSegmentToSubSegment.get(segment.getId()).getKey()) {
					segment = this.mapOriginalSegmentToSubSegment.get(segment.getId()).getValue().getValue();
				} else {
					segment = this.mapOriginalSegmentToSubSegment.get(segment.getId()).getValue().getKey();
				}
			}
			request.getCustomer().setOriginalDemandSegment(segment);
			orderRequests.add(request);

			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
					request.getCustomer());
			request.getCustomer().setTempDeliveryArea(area);

			for (Integer alt : request.getAlternativePreferences().keySet()) {
				if (alt != this.alternativeSet.getNoPurchaseAlternative().getId()) {
					if (request.getAlternativePreferences().get(alt) >= request.getAlternativePreferences()
							.get(this.alternativeSet.getNoPurchaseAlternative().getId())) {
						if (!requestsPerTimeWindow.containsKey(area)) {
							requestsPerTimeWindow.put(area, new HashMap<TimeWindow, ArrayList<Double>>());
						}
						TimeWindow tw = this.alternativeSet.getAlternativeById(alt).getTimeWindows().get(0);
						if (!requestsPerTimeWindow.get(area).containsKey(tw)) {
							requestsPerTimeWindow.get(area).put(tw, new ArrayList<Double>());
						}
						double value;
						if (this.actualBasketValue == 1.0) {
							value = request.getBasketValue()
									* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(segment.getId())
											.get(tw.getId());
						} else {
							try {
								value = ProbabilityDistributionService.getXByCummulativeDistributionQuantile(
										segment.getBasketValueDistribution(), 0.5)
										* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
												.get(segment.getId()).get(tw.getId());
							} catch (ParameterUnknownException ex) {
								// TODO Auto-generated catch block
								ex.printStackTrace();
								value = request.getBasketValue()
										* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
												.get(segment.getId()).get(tw.getId());
							}
						}
						requestsPerTimeWindow.get(area).get(tw).add(value);
					}
				}
			}

		}

		// Go through areas and time windows and determine maximal expectable
		for (DeliveryArea area : requestsPerTimeWindow.keySet()) {
			for (TimeWindow tw : requestsPerTimeWindow.get(area).keySet()) {
				Double numberExpected = this.expectedNumberOfArrivals * this.daLowerWeights.get(area)
						* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area).get(tw);
				int numberExpectedInt = numberExpected.intValue();
				if (numberExpectedInt < requestsPerTimeWindow.get(area).get(tw).size()) {
					Collections.sort(requestsPerTimeWindow.get(area).get(tw), Collections.reverseOrder());
				}
				double maximumExpectedValue = 0;
				int i = 0;
				for (Double v : requestsPerTimeWindow.get(area).get(tw)) {
					if (i < numberExpectedInt) {
						maximumExpectedValue += v;
						i++;
					} else {
						break;
					}

				}
				if (!this.maximumExpectableValuePerDeliveryAreaAndTimeWindow.containsKey(area.getId())) {
					this.maximumExpectableValuePerDeliveryAreaAndTimeWindow.put(area.getId(),
							new HashMap<Integer, Double>());
				}
				this.maximumExpectableValuePerDeliveryAreaAndTimeWindow.get(area.getId()).put(tw.getId(),
						maximumExpectedValue);

			}
		}

		if (this.directDistances != 1) {
			// Initialise the distances between nodes
			this.distanceMatrix = LocationService.getDistanceMatrixBetweenNodes(this.distances);
		}

		this.initialiseRoutes();
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
	private void fillUpRoutes(Double greedinessFactor, boolean again, int fillUpCounter,
			boolean considerWholeConsiderationSet) {

		// Assign unassigned requests to the routes. Stop if no requests are
		// left
		// over (or if no assignment feasible)
		while (this.orderRequests.size() > 0) {

			// Insertion options
			ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

			this.determineRemainingCapacity();
			this.shiftValueBuffer.clear();

			// Go through unassigned requests and define cheapest insertion per
			// request

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

				HashMap<Integer, RouteElement> optionalElements = this.findCheapestInsertionOption(request,
						fillUpCounter, iValue, considerWholeConsiderationSet);

				if (optionalElements.keySet().size() > 0) {

					insertionOptions.addAll(optionalElements.values());
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

			for (RouteElement e : insertionOptions) {

				double value = e.getTempValue();
				if (this.squaredValue == 1.0) {
					value = Math.pow(value, 2);
				}

				e.setTempValue(
						value / (e.getTempShift() * this.shiftValueBuffer.get(e.getOrder().getTimeWindowFinalId())
								/ this.remainingCapacityPerTimeWindow.get(e.getOrder().getTimeWindowFinalId())));
			}

			// Sort regarding value to find maximum and minimum value
			Collections.sort(insertionOptions, new RouteElementTempValueDescComparator());

			int chosenIndex = 0;

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

			// Choose a random number between 0 and the borderElement
			Random randomGenerator = new Random();
			chosenIndex = randomGenerator.nextInt(borderElement + 1);

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
	private HashMap<Integer, RouteElement> findCheapestInsertionOption(OrderRequest request, int fillUpCounter,
			double orderValue, boolean considerWholeConsiderationSet) {

		// Collect best elements per tw over all routes
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : this.routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = this.routes.get(routeId);
			long startTime = System.currentTimeMillis();
			HashMap<Integer, RouteElement> best = this.getCheapestInsertionElementByRoute(route, routeId, request,
					considerWholeConsiderationSet);
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

		// Determine shift-value buffer and set value of re
		for (Integer twId : bestElementPerTw.keySet()) {
			TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
			double value = orderValue
					* this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							.get(request.getCustomer().getOriginalDemandSegment().getId()).get(twId)
					* (this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
							.get(request.getCustomer().getTempDeliveryArea()).get(tw) * this.expectedNumberOfArrivals
							* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
							- this.bufferAlreadyAcceptedPerDeliveryAreaAndTimeWindow
									.get(request.getCustomer().getTempDeliveryArea()).get(tw))
					/ (this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
							.get(request.getCustomer().getTempDeliveryArea()).get(tw) * this.expectedNumberOfArrivals
							* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea()));
//					* (this.expectedNumberOfArrivals
//							* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea())
//							- this.bufferAlreadyAcceptedPerDeliveryArea
//									.get(request.getCustomer().getTempDeliveryArea()))
//					/ (this.expectedNumberOfArrivals
//							* this.daLowerWeights.get(request.getCustomer().getTempDeliveryArea()));
			bestElementPerTw.get(twId).setTempValue(value);
			if (!shiftValueBuffer.containsKey(twId)) {
				shiftValueBuffer.put(twId, value * bestElementPerTw.get(twId).getTempShiftWithoutWait());
			} else {
				shiftValueBuffer.put(twId,
						shiftValueBuffer.get(twId) + value * bestElementPerTw.get(twId).getTempShiftWithoutWait());
			}
		}

		return bestElementPerTw;

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
			int routeId, OrderRequest request, boolean considerWholeConsiderationSet) {

		// Possible time windows for request
		// No purchase alternative
		Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getNoPurchaseAlternative();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		if (!considerWholeConsiderationSet) {
			for (int altId : request.getAlternativePreferences().keySet()) {
				if (request.getAlternativePreferences().get(altId) >= request.getAlternativePreferences()
						.get(noPurchaseAlternative.getId())) {
					timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
							.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
				}
			}
		} else {
			for (ConsiderationSetAlternative csa : request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet()) {
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
						* distanceMultiplierAsTheCrowFlies / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
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
							// }else{
							// if(bufferWithTravelTime!=null){
							// useTimeWindow=(!bestOptionsPerTW.containsKey(tw.getId())
							// || bufferWithTravelTime <
							// bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait()
							// || ((bufferWithTravelTime ==
							// bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait())
							// && new Random().nextBoolean()));
							// }else{
							// useTimeWindow=(!bestOptionsPerTW.containsKey(tw.getId())
							// || shiftWithoutWait <
							// bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait()
							// || ((shiftWithoutWait ==
							// bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait())
							// && new Random().nextBoolean()));
							// }
							// }
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
						* distanceMultiplierAsTheCrowFlies / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
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
					* distanceMultiplierAsTheCrowFlies / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
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
					* distanceMultiplierAsTheCrowFlies / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;

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
						endCustomer) * distanceMultiplierAsTheCrowFlies / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
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
								neighbor.getCustomer()) * distanceMultiplierAsTheCrowFlies
								/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					double score = 0;
					if (this.actualBasketValue == 1) {
						score = neighbor.getBasketValue();
					} else {
						try {
							score = ProbabilityDistributionService.getXByCummulativeDistributionQuantile(
									neighbor.getCustomer().getOriginalDemandSegment().getBasketValueDistribution(),
									0.5);

						} catch (ParameterUnknownException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							System.exit(0);

						}
					}

					double timeCosts = distance / 2 + this.expectedServiceTime;
					if (this.squaredValue == 1) {
						neighborhoodValue += Math.pow(score, 2) / timeCosts;
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
						.getXByCummulativeDistributionQuantile(insertionOption.getOrder().getOrderRequest()
								.getCustomer().getOriginalDemandSegment().getBasketValueDistribution(), 0.5);
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

		HashMap<Integer, HashMap<Integer, Double>> collectedPerAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		int numberOfAccepted = 0;
		for (Integer routeId : solution.keySet()) {
			ArrayList<RouteElement> elements = solution.get(routeId);
			for (int elementId = 1; elementId < elements.size() - 1; elementId++) {
				numberOfAccepted++;
				double elementValue = 0;
				if (this.actualBasketValue == 1) {
					elementValue = elements.get(elementId).getOrder().getOrderRequest().getBasketValue();
					;
				} else {
					try {
						elementValue = ProbabilityDistributionService.getXByCummulativeDistributionQuantile(
								elements.get(elementId).getOrder().getOrderRequest().getCustomer()
										.getOriginalDemandSegment().getBasketValueDistribution(),
								0.5);
					} catch (ParameterUnknownException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
				elementValue = elementValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
								.getOriginalDemandSegment().getId())
						.get(elements.get(elementId).getOrder().getTimeWindowFinalId());
				if (!collectedPerAreaAndTimeWindow.containsKey(elements.get(elementId).getOrder().getOrderRequest()
						.getCustomer().getTempDeliveryArea().getId())) {
					collectedPerAreaAndTimeWindow.put(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
							.getTempDeliveryArea().getId(), new HashMap<Integer, Double>());
				}
				if (!collectedPerAreaAndTimeWindow.get(elements.get(elementId).getOrder().getOrderRequest()
						.getCustomer().getTempDeliveryArea().getId())
						.containsKey(elements.get(elementId).getOrder().getTimeWindowFinalId())) {
					collectedPerAreaAndTimeWindow
							.get(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
									.getTempDeliveryArea().getId())
							.put(elements.get(elementId).getOrder().getTimeWindowFinalId(), elementValue);
				} else {
					collectedPerAreaAndTimeWindow
							.get(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
									.getTempDeliveryArea().getId())
							.put(elements.get(elementId).getOrder().getTimeWindowFinalId(),
									elementValue + collectedPerAreaAndTimeWindow
											.get(elements.get(elementId).getOrder().getOrderRequest().getCustomer()
													.getTempDeliveryArea().getId())
											.get(elements.get(elementId).getOrder().getTimeWindowFinalId()));
				}

			}
		}

		double value = 0.0;
		for (Integer area : collectedPerAreaAndTimeWindow.keySet()) {
			for (Integer tw : collectedPerAreaAndTimeWindow.get(area).keySet()) {
				value += Math.min(collectedPerAreaAndTimeWindow.get(area).get(tw),
						this.maximumExpectableValuePerDeliveryAreaAndTimeWindow.get(area).get(tw));
				if (collectedPerAreaAndTimeWindow.get(area)
						.get(tw) > this.maximumExpectableValuePerDeliveryAreaAndTimeWindow.get(area).get(tw)) {
					System.out.println("Collected more than expected. Area: " + area + "; tw: " + tw);
				}
			}
		}

		value += numberOfAccepted * ACCEPTED_NUMBER_VALUE_DUMMY;
		return value;
	}

	public static String[] getParameterSetting() {
		return paras;
	}

	public Routing getResult() {
		return finalRouting;
	}
}
