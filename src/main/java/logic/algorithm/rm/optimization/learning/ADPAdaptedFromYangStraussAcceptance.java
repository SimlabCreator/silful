package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
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
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.service.support.ValueFunctionApproximationService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

/**
 * Info: Works with actual locations, not nodes
 * 
 * @author M. Lang
 *
 */
public class ADPAdaptedFromYangStraussAcceptance implements AcceptanceAlgorithm {
	private static int numberOfThreads = 1;
	private static double TIME_MULTIPLIER = 60.0;
	private static double DISCOUNT_FACTOR = 1.0;
	private double maximumValueAcceptable;
	private Region region;
	private ValueFunctionApproximationModelSet valueFunctionApproximationModelSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private DeliveryAreaSet deliveryAreaSet;
	private TimeWindowSet timeWindowSet;
	private AlternativeSet alternativeSet;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private static boolean possiblyLargeOfferSet = true;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private Double maximumRevenueValue;
	private Double expectedServiceTime;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo;
	private HashMap<Integer, ValueFunctionApproximationModel> valueFunctionApproximationPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> areaPotentialPerDeliveryArea;
	private HashMap<Integer, Double> remainingCapacityPerDeliveryArea;
	private HashMap<Integer, Double> acceptedCostPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCostsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> valueMultiplierPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> sumCoverageValuePerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>> routesCoveringPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> coveringPerDeliveryAreaAndTimeWindow;
	private int includeDriveFromStartingPosition;
	private HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw;
	private HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> countAcceptableCombinationOverReferences;
	private int orderHorizonLength;
	private OrderSet orderSet; // Result
	private boolean usepreferencesSampled;
	private HashMap<Integer, Double> maximumAreaPotentialPerDeliveryArea;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	private HashMap<Integer, Double> overallAcceptableCostPerDeliveryArea;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;
	private boolean deliveryAreaHierarchy;
	private ArrayList<Routing> previousRoutingResults;
	private HashMap<TimeWindow, Integer> maxAcceptedPerTw;
	private int maximumAcceptableOverTw;
	private HashMap<Integer, HashMap<Integer, Double>> distancePerDeliveryAreaAndRouting;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> distancePerDeliveryAreaAndTwAndRouting;
	private int distanceType;
	private boolean distanceMeasurePerTw;
	private int maximumDistanceMeasureIncrease;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> aggregatedReferenceInformationCosts;
	private HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;
	private double maximumAdditionalCostPerOrder;
	private double averageAdditionalCostPerOrder;
	private double minimumAdditionalCostPerOrder;

	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private int switchDistanceOffPoint;

	private static String[] paras = new String[] { "Constant_service_time", "samplePreferences",
			"includeDriveFromStartingPosition", "no_routing_candidates", "no_insertion_candidates", "distance_type",
			"distance_measure_per_tw", "maximum_distance_measure_increase", "switch_distance_off_point" };

	public ADPAdaptedFromYangStraussAcceptance(ValueFunctionApproximationModelSet valueFunctionApproximationModelSet,
											   ArrayList<Routing> previousRoutingResults, Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
											   OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet, Double expectedServiceTime,
											   HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue,
											   Double includeDriveFromStartingPosition, int orderHorizonLength, Double samplePreferences,
											   HashMap<DeliveryArea, Double> daWeights, HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings,
											   Double numberPotentialInsertionCandidates, Double numberRoutingCandidates,
											   HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, Double distanceType, Double distanceMeasurePerTw,
											   Double maximumDistanceMeasureIncrease, Double switchDistanceOffPoint) {
		this.region = region;
		this.previousRoutingResults = previousRoutingResults;
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.orderRequestSet = orderRequestSet;
		this.deliveryAreaSet = deliveryAreaSet;
		this.timeWindowSet = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.alternativeSet = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.expectedServiceTime = expectedServiceTime;
		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.valueFunctionApproximationModelSet = valueFunctionApproximationModelSet;
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}
		this.daWeights = daWeights;
		this.daSegmentWeightings = daSegmentWeightings;
		this.maximumDistanceMeasureIncrease = maximumDistanceMeasureIncrease.intValue();
		this.distanceMeasurePerTw = (distanceMeasurePerTw == 1.0);
		this.distanceType = distanceType.intValue();
		this.neighbors = neighbors;
		this.switchDistanceOffPoint = switchDistanceOffPoint.intValue();
	};

	public void start() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		DynamicRoutingHelperService.distanceMultiplierAsTheCrowFlies = AggregateReferenceInformationAlgorithm.getDistanceMultiplierAsTheCrowFlies();
		this.prepareVehicleAssignmentsAndModelsForDeliveryAreas();
		this.initialiseBuffers();
		this.initialiseAlreadyAcceptedPerTimeWindow();
		this.initialiseAcceptedCosts();
		this.initialiseAreaPotential();
		this.initialiseRemainingCapacity();
		this.initialiseAcceptedCostOverall();
		this.prepareValueMultiplier2();
		this.defineNeighborTimeWindows();
		this.prepareCoverageMultiplierPerDeliveryAreaAndTimeWindow();

		this.aggregateReferenceInformation();

		distancePerDeliveryAreaAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		distancePerDeliveryAreaAndTwAndRouting = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();
		this.maxAcceptedPerTw = new HashMap<TimeWindow, Integer>();
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();

		double maximumValueAccepted = 0.0;
		// TODO: Should everything be normalised?
		// TODO: Why not use maximum values from value funciton model

		for (Routing r : this.previousRoutingResults) {
			HashMap<TimeWindow, Integer> acceptedPerTw = new HashMap<TimeWindow, Integer>();
			double valueAccepted = 0.0;
			for (Route ro : r.getRoutes()) {
				for (RouteElement re : ro.getRouteElements()) {
					TimeWindow tw = re.getOrder().getTimeWindowFinal();
					if (!acceptedPerTw.containsKey(tw)) {
						acceptedPerTw.put(tw, 1);
					} else {
						acceptedPerTw.put(tw, acceptedPerTw.get(tw) + 1);
					}
					valueAccepted += re.getOrder().getOrderRequest().getBasketValue() / this.maximumRevenueValue;
				}
			}
			for (TimeWindow tw : acceptedPerTw.keySet()) {
				if (!maxAcceptedPerTw.containsKey(tw)) {

					maxAcceptedPerTw.put(tw, 0);
				}

				if (acceptedPerTw.get(tw) > maxAcceptedPerTw.get(tw)) {
					maxAcceptedPerTw.put(tw, acceptedPerTw.get(tw));
				}
			}

			if (maximumValueAccepted < valueAccepted)
				maximumValueAccepted = valueAccepted;
		}

		this.maximumValueAcceptable = maximumValueAccepted;

		maximumAcceptableOverTw = 0;
		for (TimeWindow tw : maxAcceptedPerTw.keySet()) {
			maximumAcceptableOverTw += maxAcceptedPerTw.get(tw);
		}

		// Adjust accepted cost (can be higher with average cost from tops)

		for (Integer area : overallAcceptableCostPerDeliveryArea.keySet()) {
			overallAcceptableCostPerDeliveryArea.put(area, Math.max(overallAcceptableCostPerDeliveryArea.get(area),
					maximumAcceptableOverTw * this.averageAdditionalCostPerOrder));
		}

		// Initialise order buffers and last routing buffers per delivery area
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
			if (area.getSubsetId() != null)
				deliveryAreaHierarchy = true;

			HashMap<Integer, Double> distancePerRouting = new HashMap<Integer, Double>();
			HashMap<Integer, HashMap<Integer, Double>> distancePerTwAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
			if (distanceType != 0) {
				for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
					distancePerRouting.put(r.getId(), 0.0);

					for (TimeWindow tw : this.timeWindowSet.getElements()) {
						if (!distancePerTwAndRouting.containsKey(tw.getId())) {
							distancePerTwAndRouting.put(tw.getId(), new HashMap<Integer, Double>());
						}

						distancePerTwAndRouting.get(tw.getId()).put(r.getId(), 0.0);
					}
				}
				this.distancePerDeliveryAreaAndRouting.put(area.getId(), distancePerRouting);
				this.distancePerDeliveryAreaAndTwAndRouting.put(area.getId(), distancePerTwAndRouting);
			}
		}

		// Sort order requests to arrive in time
		ArrayList<OrderRequest> relevantRequests = this.orderRequestSet.getElements();
		Collections.sort(relevantRequests, new OrderRequestArrivalTimeDescComparator());

		// Map time windows to alternatives
		HashMap<TimeWindow, Alternative> alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		HashMap<Integer, Double> currentDistanceMeasurePerDeliveryArea = new HashMap<Integer, Double>();
		HashMap<Integer, HashMap<Integer, Double>> currentDistanceMeasurePerDeliveryAreaAndTw = null;
		if (this.distanceType != 0) {
			currentDistanceMeasurePerDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Double>>();
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
				currentDistanceMeasurePerDeliveryAreaAndTw.put(area.getId(), new HashMap<Integer, Double>());
				currentDistanceMeasurePerDeliveryArea.put(area.getId(), 0.0);
				for (TimeWindow tw : this.timeWindowSet.getElements()) {
					currentDistanceMeasurePerDeliveryAreaAndTw.get(area.getId()).put(tw.getId(), 0.0);
				}
			}
		}

		ArrayList<Order> orders = new ArrayList<Order>();
		// Go through requests and update value function
		for (OrderRequest request : relevantRequests) {

			if (request.getArrivalTime() <= switchDistanceOffPoint) {
				this.distanceType = 0;
			}
			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet,
					request.getCustomer());

			ArrayList<Order> relevantOrders;
			HashMap<Integer, VehicleAreaAssignment> relevantVaas;
			HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar;
			Double currentAcceptedTravelTime;
			double currentRelevantMeasure = 0;
			HashMap<Integer, Double> currentRelevantDistanceMeasurePerTw = null;
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> currentAggregatedReferenceInformationNo = null;
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> currentCountAcceptable = null;
			HashMap<Integer, Double> relevantDistancePerDeliveryAreaAndRouting = null;
			HashMap<Integer, HashMap<Integer, Double>> relevantDistancePerDeliveryAreaAndTwAndRouting = null;
			if (deliveryAreaHierarchy) {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(rArea.getDeliveryAreaOfSet());
				relevantVaas = this.vasPerDeliveryAreaAndVehicleNo.get(rArea.getDeliveryAreaOfSet().getId());
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(rArea.getDeliveryAreaOfSet());
				currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(rArea.getDeliveryAreaOfSet());
				if (this.distanceType != 0) {
					currentRelevantMeasure = currentDistanceMeasurePerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					currentRelevantDistanceMeasurePerTw = currentDistanceMeasurePerDeliveryAreaAndTw
							.get(rArea.getDeliveryAreaOfSet().getId());
					relevantDistancePerDeliveryAreaAndRouting = this.distancePerDeliveryAreaAndRouting
							.get(rArea.getDeliveryAreaOfSet().getId());
					relevantDistancePerDeliveryAreaAndTwAndRouting = this.distancePerDeliveryAreaAndTwAndRouting
							.get(rArea.getDeliveryAreaOfSet().getId());
					currentAggregatedReferenceInformationNo = this.aggregatedReferenceInformationNo
							.get(rArea.getDeliveryAreaOfSet());
					currentCountAcceptable = this.countAcceptableCombinationOverReferences
							.get(rArea.getDeliveryAreaOfSet());
				}

			} else {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(rArea);
				relevantVaas = this.vasPerDeliveryAreaAndVehicleNo.get(rArea);
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(rArea);
				currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(rArea);
				// TODO: Does not work without hierarchy now (problem with
				// distance measure!)
				if (this.distanceType != 0) {
					currentRelevantMeasure = currentDistanceMeasurePerDeliveryArea.get(rArea.getId());
					currentRelevantDistanceMeasurePerTw = currentDistanceMeasurePerDeliveryAreaAndTw.get(rArea.getId());
					relevantDistancePerDeliveryAreaAndRouting = this.distancePerDeliveryAreaAndRouting
							.get(rArea.getId());
					relevantDistancePerDeliveryAreaAndTwAndRouting = this.distancePerDeliveryAreaAndTwAndRouting
							.get(rArea.getId());
					currentAggregatedReferenceInformationNo = this.aggregatedReferenceInformationNo.get(rArea);
					currentCountAcceptable = this.countAcceptableCombinationOverReferences.get(rArea);
				}

			}

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();
			ArrayList<TimeWindow> consideredTimeWindows = new ArrayList<TimeWindow>();
			for (ConsiderationSetAlternative alt : request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet()) {
				if (!alt.getAlternative().getNoPurchaseAlternative()) {
					consideredTimeWindows.add(alt.getAlternative().getTimeWindows().get(0));
				}

			}
			currentAcceptedTravelTime = DynamicRoutingHelperService
					.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
							request, region, TIME_MULTIPLIER, this.timeWindowSet,
							(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime, possibleRoutings,
							this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates, relevantVaas,
							relevantOrders, bestRoutingSoFar, currentAcceptedTravelTime, consideredTimeWindows,
							bestRoutingsValueAfterInsertion, numberOfThreads);

			HashMap<Integer, Double> distanceMeasurePerTimeWindow = null;
			HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
			HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow = new HashMap<Integer, Routing>();
			ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
			for (Integer twId : bestRoutingsValueAfterInsertion.keySet()) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
				if (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
						.getAcceptedOverallCostCoefficient() == null
						|| (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
								.getAcceptedOverallCostCoefficient() != null
								&& this.aggregatedReferenceInformationCosts.containsKey(rArea)
								&& this.aggregatedReferenceInformationCosts.get(rArea).containsKey(tw))) {
					timeWindows.add(tw);
				}
			}
			// Determine distance value
			if (distanceType != 0) {
				distanceMeasurePerTimeWindow = ADPWithSoftOrienteeringANN
						.calculateResultingDistancePerTimeWindowAndRouting(rArea, neighbors.get(rArea), neighborsTw,
								timeWindows, currentAggregatedReferenceInformationNo, currentCountAcceptable,
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
								relevantDistancePerDeliveryAreaAndRouting,
								relevantDistancePerDeliveryAreaAndTwAndRouting, newDistancePerTimeWindowAndRouting,
								routingSmallestDistancePerTimeWindow, this.distanceType, this.distanceMeasurePerTw);
			}

			// Choose offer
			// Calculate value of same state in next time step

			double noAssignmentValue = ValueFunctionApproximationService
					.evaluateStateForLinearValueFunctionApproximationModel(
							this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							request.getArrivalTime() - 1,
							this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
							this.acceptedInsertionCostsPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.coveringPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
							this.remainingCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.areaPotentialPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							orderHorizonLength, this.maxAcceptedPerTw, this.maximumAcceptableOverTw,
							this.sumCoverageValuePerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
							this.overallCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.overallAcceptableCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.maximumAreaPotentialPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
									* this.maximumAcceptableOverTw,
							TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea
									.get(rArea.getDeliveryAreaOfSet().getId()).size(),
							this.timeWindowSet.getElements().size())
					* maximumValueAcceptable;

			// For all feasible, assume you accept -> get value
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			for (TimeWindow tw : timeWindows) {
				int twId = tw.getId();
				if (distanceType != 0 && this.distanceMeasurePerTw)
					currentRelevantMeasure = currentRelevantDistanceMeasurePerTw.get(tw.getId());
				if (distanceType == 0 || distanceMeasurePerTimeWindow.get(tw.getId())
						- currentRelevantMeasure < this.maximumDistanceMeasureIncrease + 1) {
					int currentAccepted = this.alreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(rArea.getDeliveryAreaOfSet().getId()).get(twId);
					this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							++currentAccepted);

					double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId()).get(twId);
					double additionalCosts = bestRoutingsValueAfterInsertion.get(twId).getKey()
							.getTempShiftWithoutWait();
					this.acceptedInsertionCostsPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							additionalCosts + currentAcceptedInsertion);

					double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newRemainingCapacity = currentOverallRemainingCapacity
							- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							newRemainingCapacity);
					
					double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId());
					double newAcceptedCostOverall = currentOverallAcceptedCost;
					//Take expected cost if there is waiting time involved
					if(bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()>this.expectedServiceTime+minimumAdditionalCostPerOrder|| 
							bestRoutingsValueAfterInsertion.get(twId).getKey().getTempSlack()>this.expectedServiceTime+minimumAdditionalCostPerOrder || 
							(bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()+bestRoutingsValueAfterInsertion.get(twId).getKey().getTempSlack())
							>this.expectedServiceTime+minimumAdditionalCostPerOrder){
						newAcceptedCostOverall= newAcceptedCostOverall+this.aggregatedReferenceInformationCosts.get(rArea).get(tw);
					}else{
						//Otherwise: take the current insertion cost
						newAcceptedCostOverall= newAcceptedCostOverall+bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					}
					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), newAcceptedCostOverall);

					double currentAreaPotential = this.areaPotentialPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					// double newAreaPotential =
					// this.valueMultiplierPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(twId);
					double newAreaPotential = this.valueMultiplierPerDeliveryArea.get(rArea.getId());
					this.areaPotentialPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentAreaPotential + newAreaPotential);

					double assignmentValue = ValueFunctionApproximationService
							.evaluateStateForLinearValueFunctionApproximationModel(
									this.valueFunctionApproximationPerDeliveryArea
											.get(rArea.getDeliveryAreaOfSet().getId()),
									request.getArrivalTime() - 1,
									this.alreadyAcceptedPerDeliveryAreaAndTimeWindow
											.get(rArea.getDeliveryAreaOfSet().getId()),
									this.acceptedInsertionCostsPerDeliveryArea
											.get(rArea.getDeliveryAreaOfSet().getId()),
									this.coveringPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
									this.remainingCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
									this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
									this.areaPotentialPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
									orderHorizonLength, this.maxAcceptedPerTw, this.maximumAcceptableOverTw,
									this.sumCoverageValuePerDeliveryAreaAndTimeWindow
											.get(rArea.getDeliveryAreaOfSet().getId()),
									this.overallCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
									this.overallAcceptableCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
									this.maximumAreaPotentialPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
											* maximumAcceptableOverTw,
									TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea
											.get(rArea.getDeliveryAreaOfSet().getId()).size(),
									this.timeWindowSet.getElements().size())
							* maximumValueAcceptable;

					twValue.put(tw, assignmentValue);
					this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							--currentAccepted);
					this.acceptedInsertionCostsPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							currentAcceptedInsertion);

					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentOverallRemainingCapacity);
					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), currentOverallAcceptedCost);
					this.areaPotentialPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), currentAreaPotential);
				}
			}
			ArrayList<AlternativeOffer> bestOfferedAlternatives = new ArrayList<AlternativeOffer>();
			// Find best subset from the time windows with value add

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue, maximumRevenueValue, objectiveSpecificValues, algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, true, false, null, null, DISCOUNT_FACTOR);
			bestOfferedAlternatives = bestOffer.getKey();

			// Simulate customer decision
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			if (bestOfferedAlternatives.size() > 0) { // If windows are offered
				// Sample selection from customer

				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							bestOfferedAlternatives, order, this.alternativeSet.getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(bestOfferedAlternatives, order);
				}

				if (selectedAlt != null) {
					int twId = selectedAlt.getAlternative().getTimeWindows().get(0).getId();
					int currentAccepted = this.alreadyAcceptedPerDeliveryAreaAndTimeWindow
							.get(rArea.getDeliveryAreaOfSet().getId()).get(twId);
					this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							++currentAccepted);
					double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId()).get(twId);
					double additionalCosts = bestRoutingsValueAfterInsertion.get(twId).getKey()
							.getTempShiftWithoutWait();
					this.acceptedInsertionCostsPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							additionalCosts + currentAcceptedInsertion);
					double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newRemainingCapacity = currentOverallRemainingCapacity
							- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							newRemainingCapacity);
					double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId());
					double newAcceptedCostOverall = currentOverallAcceptedCost;
					//Take expected cost if there is waiting time involved
					if(bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()>this.expectedServiceTime+minimumAdditionalCostPerOrder|| 
							bestRoutingsValueAfterInsertion.get(twId).getKey().getTempSlack()>this.expectedServiceTime+minimumAdditionalCostPerOrder || 
							(bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()+bestRoutingsValueAfterInsertion.get(twId).getKey().getTempSlack())
							>this.expectedServiceTime+minimumAdditionalCostPerOrder){
						newAcceptedCostOverall= newAcceptedCostOverall+this.aggregatedReferenceInformationCosts.get(rArea).get(selectedAlt.getAlternative().getTimeWindows().get(0));
					}else{
						//Otherwise: take the current insertion cost
						newAcceptedCostOverall= newAcceptedCostOverall+bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					}
					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), newAcceptedCostOverall);
					
					double currentAreaPotential = this.areaPotentialPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());

					double newAreaPotential = this.valueMultiplierPerDeliveryArea.get(rArea.getId());
					this.areaPotentialPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentAreaPotential + newAreaPotential);

					order.setTimeWindowFinalId(twId);
					order.setAccepted(true);
					RouteElement elementToInsert = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
							.getKey();
					int routingId = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId()).getKey()
							.getTempRoutingId();
					elementToInsert.setOrder(order);
					DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
							relevantVaas, timeWindowSet, TIME_MULTIPLIER, (includeDriveFromStartingPosition == 1));
					bestRoutingSoFar = possibleRoutings.get(routingId);
					currentAcceptedTravelTime = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
							.getValue();

					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(rArea.getId())) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(rArea.getId(),
								new HashMap<Integer, Integer>());
					}
					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
							.containsKey(elementToInsert.getOrder().getTimeWindowFinalId())) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
								.put(elementToInsert.getOrder().getTimeWindowFinalId(), 0);
					}
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).put(
							elementToInsert.getOrder().getTimeWindowFinalId(),
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
									.get(elementToInsert.getOrder().getTimeWindowFinalId()) + 1);

					// Update distances
					if (distanceType != 0) {
						this.distancePerDeliveryAreaAndRouting.put(rArea.getDeliveryAreaOfSet().getId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						this.distancePerDeliveryAreaAndTwAndRouting.get(rArea.getDeliveryAreaOfSet().getId()).put(
								order.getTimeWindowFinalId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						currentDistanceMeasurePerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
								distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId()));
						if (this.distanceMeasurePerTw)
							currentDistanceMeasurePerDeliveryAreaAndTw.get(rArea.getDeliveryAreaOfSet().getId()).put(
									order.getTimeWindowFinalId(),
									distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId()));
					}

				} else {
					order.setReasonRejection("Customer chose no-purchase option");
				}

			} else {

				if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {
					order.setReasonRejection(
							"Better to offer nothing. Feasible:" + bestRoutingsValueAfterInsertion.keySet().size());
				} else {
					order.setReasonRejection("No feasible, considered time windows");
				}

			}

			if (this.deliveryAreaHierarchy) {
				// Update best and accepted travel time because it could change
				// also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(rArea.getDeliveryAreaOfSet(), bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(rArea.getDeliveryAreaOfSet(), currentAcceptedTravelTime);
				if (order.getAccepted()) {

					acceptedOrdersPerDeliveryArea.get(rArea.getDeliveryAreaOfSet()).add(order);

				}

			} else {
				// Update best and accepted travel time because it could change
				// also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(rArea, bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(rArea, currentAcceptedTravelTime);
				if (order.getAccepted()) {

					acceptedOrdersPerDeliveryArea.get(rArea).add(order);

				}
			}
			orders.add(order);

		}

		this.orderSet = new OrderSet();
		this.orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.orderSet.setElements(orders);

	}

	private void defineNeighborTimeWindows() {
		neighborsTw = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			this.neighborsTw.put(tw, new ArrayList<TimeWindow>());
			for (TimeWindow twN : this.timeWindowSet.getElements()) {
				if (tw.getId() != twN.getId()) {
					if (twN.getStartTime() <= tw.getEndTime() && twN.getStartTime() >= tw.getStartTime()) {
						this.neighborsTw.get(tw).add(twN);
					} else if (twN.getEndTime() >= tw.getStartTime() && twN.getEndTime() <= tw.getEndTime()) {
						this.neighborsTw.get(tw).add(twN);
					}
				}
			}
		}
	}

	private void prepareVehicleAssignmentsAndModelsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.valueFunctionApproximationPerDeliveryArea = new HashMap<Integer, ValueFunctionApproximationModel>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.overallAcceptableCostPerDeliveryArea = new HashMap<Integer, Double>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());

			this.overallCapacityPerDeliveryArea.put(area.getId(), 0.0);
			this.overallAcceptableCostPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);
			double capacity = this.overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
			double additionalCap = Math.min(
					(ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER + (this.expectedServiceTime - 1),
					(this.expectedServiceTime - 1) + this.timeWindowSet.getTempLengthOfDeliveryPeriod());
			capacity += additionalCap;
			this.overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);

			double cost = this.overallAcceptableCostPerDeliveryArea.get(ass.getDeliveryAreaId());
			double additionalCos = Math.min(
					(ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER + (this.expectedServiceTime - 1),
					(this.expectedServiceTime - 1) + this.timeWindowSet.getTempLengthOfDeliveryPeriod());
			cost += additionalCos;
			this.overallAcceptableCostPerDeliveryArea.put(ass.getDeliveryAreaId(), cost);
		}

		for (ValueFunctionApproximationModel model : this.valueFunctionApproximationModelSet.getElements()) {
			this.valueFunctionApproximationPerDeliveryArea.put(model.getDeliveryAreaId(), model);
		}

	}

	private void initialiseBuffers() {

		this.alreadyAcceptedPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.acceptedInsertionCostsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Double>>();
		this.areaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		this.remainingCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedCostPerDeliveryArea= new HashMap<Integer, Double>();
		this.coveringPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.sumCoverageValuePerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.routesCoveringPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>>();

	}

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				acceptedPerTw.put(tw.getId(), 0);
			}
			this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.put(area.getId(), acceptedPerTw);
		}
	}

	private void initialiseAcceptedCosts() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAcceptedCostsPerTimeWindow(area);
		}
	}

	private void initialiseAcceptedCostsPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Double> acceptedPerTw = new HashMap<Integer, Double>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			acceptedPerTw.put(tw.getId(), 0.0);
		}
		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), acceptedPerTw);
	}

	private void initialiseAreaPotential() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAreaPotential(area);
		}
	}

	private void initialiseAreaPotential(DeliveryArea area) {
		this.areaPotentialPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseRemainingCapacity() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseRemainingCapacity(area);
		}
	}

	private void initialiseRemainingCapacity(DeliveryArea area) {
		this.remainingCapacityPerDeliveryArea.put(area.getId(), this.overallCapacityPerDeliveryArea.get(area.getId()));
	}

	private void initialiseAcceptedCostOverall() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAcceptedCostOverall(area);
		}
	}

	private void initialiseAcceptedCostOverall(DeliveryArea area) {
		this.acceptedCostPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
		}
		// TODO: consider to add weighting of time window?
		for (DeliveryArea area : this.daWeights.keySet()) {

			HashMap<Integer, Double> timeWindowMultiplier = new HashMap<Integer, Double>();
			for (DemandSegmentWeight segW : this.daSegmentWeightings.get(area).getWeights()) {
				double weightedValue = CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
						objectiveSpecificValues, segW.getDemandSegment());
				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				for (ConsiderationSetAlternative alt : alts) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						double m = this.daWeights.get(area) * segW.getWeight() * weightedValue * 100;
						if (timeWindowMultiplier.containsKey(alt.getAlternative().getTimeWindows().get(0).getId())) {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0).getId(),
									m + timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0).getId()));
						} else {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0).getId(), m);
						}
						if (timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0)
								.getId()) > this.maximumAreaPotentialPerDeliveryArea
										.get(area.getDeliveryAreaOfSet().getId())) {
							this.maximumAreaPotentialPerDeliveryArea.put(area.getDeliveryAreaOfSet().getId(),
									timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0).getId()));
						}
					}
				}

			}

			this.valueMultiplierPerDeliveryAreaAndTimeWindow.put(area.getId(), timeWindowMultiplier);
		}

	}

	private void prepareValueMultiplier2() {
		this.valueMultiplierPerDeliveryArea = new HashMap<Integer, Double>();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
		}
		// TODO: consider to add weighting of time window?
		for (DeliveryArea area : this.daWeights.keySet()) {

			double weightedValue = 0.0;
			for (DemandSegmentWeight segW : this.daSegmentWeightings.get(area).getWeights()) {
				weightedValue += CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
						objectiveSpecificValues, segW.getDemandSegment()) * segW.getWeight();
			}

			if (weightedValue > this.maximumAreaPotentialPerDeliveryArea.get(area.getDeliveryAreaOfSet().getId())) {
				this.maximumAreaPotentialPerDeliveryArea.put(area.getDeliveryAreaOfSet().getId(), weightedValue);
			}
			this.valueMultiplierPerDeliveryArea.put(area.getId(), weightedValue);
		}

	}

	private void prepareCoverageMultiplierPerDeliveryAreaAndTimeWindow() {

		//
		//
		// for (DeliveryArea area :
		// this.arrivalProbabilityPerDeliveryAreaAndDemandSegment.keySet()) {
		// HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> routesDummy =
		// new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		// HashMap<Integer, Double> sumCoverageMultiplierPerTimeWindow = new
		// HashMap<Integer, Double>();
		// for (DeliveryArea areaSub :
		// this.arrivalProbabilityPerDeliveryAreaAndDemandSegment.get(area).keySet())
		// {
		// HashMap<Integer, ArrayList<Integer>> routesDummyArea = new
		// HashMap<Integer, ArrayList<Integer>>();
		// for (Integer tw :
		// this.valueMultiplierPerDeliveryAreaAndTimeWindow.get(area.getId())
		// .get(areaSub.getId()).keySet()) {
		// ArrayList<Integer> twRoutes = new ArrayList<Integer>();
		// routesDummyArea.put(tw, twRoutes);
		//
		// double vehicleNumber =
		// this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size();
		//
		// for (int currentVehicleNumber = 0; currentVehicleNumber <
		// this.vehicleAssignmentsPerDeliveryArea
		// .get(area.getId()).size(); currentVehicleNumber++) {
		// if (sumCoverageMultiplierPerTimeWindow.containsKey(tw)) {
		// sumCoverageMultiplierPerTimeWindow.put(tw,
		// sumCoverageMultiplierPerTimeWindow.get(tw)
		// + ((vehicleNumber - currentVehicleNumber) / vehicleNumber)
		// * this.valueMultiplierPerDeliveryAreaAndTimeWindow.get(area.getId())
		// .get(areaSub.getId()).get(tw));
		// } else {
		// sumCoverageMultiplierPerTimeWindow.put(tw,
		// ((vehicleNumber - currentVehicleNumber) / vehicleNumber)
		// * this.valueMultiplierPerDeliveryAreaAndTimeWindow.get(area.getId())
		// .get(areaSub.getId()).get(tw));
		// }
		//
		// }
		//
		// }
		// routesDummy.put(areaSub.getId(), routesDummyArea);
		// }
		// ;
		// this.routesCoveringPerDeliveryAreaAndTimeWindow.put(area.getId(),
		// routesDummy);
		// this.sumCoverageValuePerDeliveryAreaAndTimeWindow.put(area.getId(),
		// sumCoverageMultiplierPerTimeWindow);
		// }

		//
		// for (Integer area :
		// this.valueMultiplierPerDeliveryAreaAndTimeWindow.keySet()) {
		// HashMap<Integer, Double> twCoveringValue = new HashMap<Integer,
		// Double>();
		// for (TimeWindow tw : this.timeWindowSet.getElements()) {
		// twCoveringValue.put(tw.getId(), 0.0);
		// }
		// this.coveringPerDeliveryAreaAndTimeWindow.put(area, twCoveringValue);
		// }
	}

	private double determineAdditionalCoverageAfterInsertion(Integer twId, RouteElement possibleInsertion,
			boolean updateRoutesPerDa, DeliveryArea area, HashMap<Integer, ArrayList<RouteElement>> routes) {
		double additionalCov = 0.0;

		// ArrayList<DeliveryArea> listA = new ArrayList<DeliveryArea>();
		// listA.addAll(this.arrivalProbabilityPerDeliveryAreaAndDemandSegment.get(area).keySet());
		//
		// DeliveryArea target =
		// LocationService.assignCustomerToDeliveryArea(listA,
		// possibleInsertion.getOrder().getOrderRequest().getCustomer(),
		// area.getLat1(), area.getLat2(),
		// area.getLon1(), area.getLon2());
		// DeliveryArea source =
		// LocationService.assignCustomerToDeliveryArea(listA,
		// routes.get(possibleInsertion.getTempRoute()).get(possibleInsertion.getTempPosition()
		// - 1).getOrder()
		// .getOrderRequest().getCustomer(),
		// area.getLat1(), area.getLat2(), area.getLon1(), area.getLon2());
		//
		// // Build hyper da
		// ArrayList<Double> lats = new ArrayList<Double>();
		// lats.add(target.getLat1());
		// lats.add(target.getLat2());
		// lats.add(source.getLat1());
		// lats.add(source.getLat2());
		// Collections.sort(lats);
		// double lowerLat = lats.get(0);
		// double upperLat = lats.get(lats.size() - 1);
		// ArrayList<Double> lons = new ArrayList<Double>();
		// lons.add(target.getLon1());
		// lons.add(target.getLon2());
		// lons.add(source.getLon1());
		// lons.add(source.getLon2());
		// Collections.sort(lons);
		// double lowerLon = lons.get(0);
		// double upperLon = lons.get(lons.size() - 1);
		//
		// ArrayList<DeliveryArea> relevantAreas =
		// LocationService.determineDeliveryAreasIncludedInRegion(listA,
		// lowerLat,
		// upperLat, lowerLon, upperLon);
		//
		// for (DeliveryArea a : relevantAreas) {
		// if
		// (!this.routesCoveringPerDeliveryAreaAndTimeWindow.get(area.getId()).get(a.getId()).get(twId)
		// .contains(possibleInsertion.getTempRoute())) {
		// additionalCov +=
		// this.valueMultiplierPerDeliveryAreaAndTimeWindow.get(area.getId()).get(a.getId())
		// .get(twId)
		// * (this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size()
		// -
		// this.routesCoveringPerDeliveryAreaAndTimeWindow.get(area.getId()).get(a.getId())
		// .get(twId).size())
		// / this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size();
		//
		// if (updateRoutesPerDa) {
		// this.routesCoveringPerDeliveryAreaAndTimeWindow.get(area.getId()).get(a.getId()).get(twId)
		// .add(possibleInsertion.getTempRoute());
		// }
		// }
		// }
		return additionalCov;
	}

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	private void aggregateReferenceInformation() {

		// TA: distance - really just travel time to?

				this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
				countAcceptableCombinationOverReferences = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
			
				this.aggregatedReferenceInformationCosts = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();

				// TA: consider that maximal acceptable costs is initialised too low
				this.maximalAcceptableCostsPerDeliveryArea = new HashMap<Integer, Double>();
				for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

					this.aggregatedReferenceInformationNo.put(area,
							new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
					countAcceptableCombinationOverReferences.put(area,
							new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
					this.maximalAcceptableCostsPerDeliveryArea.put(area.getId(), 0.0);
				}

				for (Routing routing : this.previousRoutingResults) {
					HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
					HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> distance = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>();

					for (Route r : routing.getRoutes()) {
						DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
								r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
						if (!count.containsKey(area)) {
							count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
							distance.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>());
						}
						for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
							RouteElement re = r.getRouteElements().get(reId);
							double travelTimeFrom;
							double travelTimeTo=re.getTravelTime();
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
								// travelTimeFrom =
								// LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
								// re.getOrder().getOrderRequest().getCustomer().getLat(),
								// re.getOrder().getOrderRequest().getCustomer().getLon(),
								// r.getVehicleAssignment().getEndingLocationLat(),
								// r.getVehicleAssignment().getEndingLocationLon())*distanceMultiplierAsTheCrowFlies
								// / region.getAverageKmPerHour()
								// * TIME_MULTIPLIER;
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
								travelTimeTo=0;
							}
							double travelTimeAlternative;

							if (depotBefore || depotAfter) {
								travelTimeAlternative = 0.0;
							} else {
								travelTimeAlternative = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
										latBefore, lonBefore, latAfter, lonAfter) * ADPAdaptedFromYangStrauss.getDistanceMultiplierAsTheCrowFlies()
										/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
							}

							double additionalCost = travelTimeTo + travelTimeFrom + this.expectedServiceTime
									- travelTimeAlternative;

							Customer cus = re.getOrder().getOrderRequest().getCustomer();
							DeliveryArea subArea = LocationService
									.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
							if (!count.get(area).containsKey(subArea)) {
								count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
								distance.get(area).put(subArea, new HashMap<TimeWindow, Double>());
							}
							if (!countAcceptableCombinationOverReferences.get(area).containsKey(subArea)) {
								countAcceptableCombinationOverReferences.get(area).put(subArea,
										new HashMap<TimeWindow, Integer>());
							}
							if (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal())) {
								count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);
								distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), additionalCost);
							} else {
								count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
										count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) + 1);
								// if(count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())>5)
								// System.out.println("Very much for one tw in one
								// subarea");
								distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
										distance.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())
												+ additionalCost);
							}
							if (!countAcceptableCombinationOverReferences.get(area).get(subArea)
									.containsKey(re.getOrder().getTimeWindowFinal())) {
								countAcceptableCombinationOverReferences.get(area).get(subArea)
										.put(re.getOrder().getTimeWindowFinal(), 0);
							}
						}
					}

					for (DeliveryArea a : this.aggregatedReferenceInformationNo.keySet()) {

						
						this.aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));

						double distanceSum = 0.0;
						for (DeliveryArea area : distance.get(a).keySet()) {
							for (TimeWindow tw : distance.get(a).get(area).keySet()) {
								
								distance.get(a).get(area).put(tw,
										distance.get(a).get(area).get(tw) / count.get(a).get(area).get(tw));
								if (!this.aggregatedReferenceInformationCosts.containsKey(area)) {
									this.aggregatedReferenceInformationCosts.put(area, new HashMap<TimeWindow, Double>());
								}
								if (!this.aggregatedReferenceInformationCosts.get(area).containsKey(tw)) {
									this.aggregatedReferenceInformationCosts.get(area).put(tw,
											distance.get(a).get(area).get(tw));
								} else {
									this.aggregatedReferenceInformationCosts.get(area).put(tw,
											this.aggregatedReferenceInformationCosts.get(area).get(tw)
													+ distance.get(a).get(area).get(tw));
								}
								distanceSum += distance.get(a).get(area).get(tw);
								countAcceptableCombinationOverReferences.get(a).get(area).put(tw,
										countAcceptableCombinationOverReferences.get(a).get(area).get(tw) + 1);
							}
						}

						if (this.maximalAcceptableCostsPerDeliveryArea.get(a.getId()) < distanceSum) {
							this.maximalAcceptableCostsPerDeliveryArea.put(a.getId(), distanceSum);
						}
					}

				}

				maximumAdditionalCostPerOrder = 0.0;
				averageAdditionalCostPerOrder =0.0;
				int counter =0;
				minimumAdditionalCostPerOrder=Double.MAX_VALUE;
				for (DeliveryArea area : this.aggregatedReferenceInformationCosts.keySet()) {
					for (TimeWindow tw : this.aggregatedReferenceInformationCosts.get(area).keySet()) {
						aggregatedReferenceInformationCosts.get(area).put(tw,
								aggregatedReferenceInformationCosts.get(area).get(tw) / countAcceptableCombinationOverReferences
										.get(area.getDeliveryAreaOfSet()).get(area).get(tw));
						averageAdditionalCostPerOrder+=aggregatedReferenceInformationCosts.get(area).get(tw);
						if (aggregatedReferenceInformationCosts.get(area).get(tw) > maximumAdditionalCostPerOrder)
							maximumAdditionalCostPerOrder = aggregatedReferenceInformationCosts.get(area).get(tw);
						if (aggregatedReferenceInformationCosts.get(area).get(tw) < minimumAdditionalCostPerOrder)
							minimumAdditionalCostPerOrder = aggregatedReferenceInformationCosts.get(area).get(tw);
						counter++;
					}
				}
				
				averageAdditionalCostPerOrder=averageAdditionalCostPerOrder/counter;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return this.orderSet;
	}

}
