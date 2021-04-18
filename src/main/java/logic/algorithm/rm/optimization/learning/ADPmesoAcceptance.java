package logic.algorithm.rm.optimization.learning;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
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
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.entity.NonParametricValueFunctionAddon;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Online Policy of ADPMeso
 * 
 * @author M. Lang
 *
 */
public class ADPmesoAcceptance extends AggregateReferenceInformationAlgorithm implements RoutingAlgorithm {
	private static int numberOfThreads = 1;
	private static double DISCOUNT_FACTOR = 1.0;
	private static boolean possiblyLargeOfferSet = true;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;

	private ValueFunctionApproximationModelSet valueFunctionApproximationModelSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private AlternativeSet alternativeSet;

	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo;
	private HashMap<Integer, ValueFunctionApproximationModel> valueFunctionApproximationPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> remainingCapacityPerDeliveryArea;
	private HashMap<Integer, Double> acceptedCostPerDeliveryArea;
	private HashMap<Integer, Integer> acceptedAmountPerDeliveryArea;
	private int includeDriveFromStartingPosition;
	private HashMap<DeliveryArea, Double[][]> lookupTablePerDeliveryArea;
	private HashMap<DeliveryArea, Double[]> lookupArrayPerDeliveryArea;
	private double weightLf;

	private int orderHorizonLength;
	private OrderSet orderSet; // Result
	private Routing finalRouting; // Result
	private boolean usepreferencesSampled;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;
	private boolean deliveryAreaHierarchy;

	private HashMap<Integer, HashMap<Integer, Double>> distancePerDeliveryAreaAndRouting;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> distancePerDeliveryAreaAndTwAndRouting;
	private int distanceType;
	private boolean distanceMeasurePerTw;
	private int maximumDistanceMeasureIncrease;

	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private int switchDistanceOffPoint;

	private static String[] paras = new String[] { "Constant_service_time", "samplePreferences",
			"includeDriveFromStartingPosition", "no_routing_candidates", "no_insertion_candidates", "distance_type",
			"distance_measure_per_tw", "maximum_distance_measure_increase", "switch_distance_off_point" };

	public ADPmesoAcceptance(ValueFunctionApproximationModelSet valueFunctionApproximationModelSet,
			ArrayList<Routing> previousRoutingResults, Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet, Double expectedServiceTime,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue,
			Double includeDriveFromStartingPosition, int orderHorizonLength, Double samplePreferences,
			Double numberPotentialInsertionCandidates, Double numberRoutingCandidates,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, Double distanceType, Double distanceMeasurePerTw,
			Double maximumDistanceMeasureIncrease, Double switchDistanceOffPoint) {
		AggregateReferenceInformationAlgorithm.setRegion(region);
		AggregateReferenceInformationAlgorithm.setPreviousRoutingResults(previousRoutingResults);
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.orderRequestSet = orderRequestSet;
		AggregateReferenceInformationAlgorithm.setDeliveryAreaSet(deliveryAreaSet);
		AggregateReferenceInformationAlgorithm.setTimeWindowSet(this.orderRequestSet.getCustomerSet()
				.getOriginalDemandSegmentSet().getAlternativeSet().getTimeWindowSet());
		this.alternativeSet = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		AggregateReferenceInformationAlgorithm.setExpectedServiceTime(expectedServiceTime);
		AggregateReferenceInformationAlgorithm.setMaximumRevenueValue(maximumRevenueValue);
		AggregateReferenceInformationAlgorithm.setObjectiveSpecificValues(objectiveSpecificValues);
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

		this.maximumDistanceMeasureIncrease = maximumDistanceMeasureIncrease.intValue();
		this.distanceMeasurePerTw = (distanceMeasurePerTw == 1.0);
		this.distanceType = distanceType.intValue();
		this.neighbors = neighbors;
		this.switchDistanceOffPoint = switchDistanceOffPoint.intValue();
	};

	public void start() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(
				AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER());
		RoutingService.getDeliveryStartTimeByTimeWindowSet(AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		DynamicRoutingHelperService.distanceMultiplierAsTheCrowFlies = AggregateReferenceInformationAlgorithm
				.getDistanceMultiplierAsTheCrowFlies();

		this.initialiseBuffers();
		DynamicRoutingHelperService.prepareVehicleAssignmentsForDeliveryAreas(vehicleAreaAssignmentSet,
				vehicleAssignmentsPerDeliveryArea, vasPerDeliveryAreaAndVehicleNo, overallCapacityPerDeliveryArea,
				AggregateReferenceInformationAlgorithm.getDeliveryAreaSet(),
				AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(), this.getExpectedServiceTime());
		this.aggregateReferenceInformation(false, false);
		this.initialiseAlreadyAcceptedPerTimeWindow();
		this.initialiseRemainingCapacity();
		this.initialiseAcceptedCostOverall();
		this.initialiseAcceptedAmount();
		distancePerDeliveryAreaAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		distancePerDeliveryAreaAndTwAndRouting = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Initialise order buffers and last routing buffers per delivery area
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {

			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
			if (area.getSubsetId() != null)
				deliveryAreaHierarchy = true;

			HashMap<Integer, Double> distancePerRouting = new HashMap<Integer, Double>();
			HashMap<Integer, HashMap<Integer, Double>> distancePerTwAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
			if (distanceType != 0) {
				for (Routing r : aggregatedReferenceInformationNo.get(area).keySet()) {
					distancePerRouting.put(r.getId(), 0.0);

					for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
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
			for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
				currentDistanceMeasurePerDeliveryAreaAndTw.put(area.getId(), new HashMap<Integer, Double>());
				currentDistanceMeasurePerDeliveryArea.put(area.getId(), 0.0);
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
					currentDistanceMeasurePerDeliveryAreaAndTw.get(area.getId()).put(tw.getId(), 0.0);
				}
			}
		}

		ArrayList<Order> orders = new ArrayList<Order>();
		// Go through requests and update value function
		int distBuffer=this.distanceType;
		if(this.distanceType ==4){
			distanceType = 2;
			this.maximumDistanceMeasureIncrease=0;
		}
		
		double overallTime =0;
		double maxTime=0;
		for (OrderRequest request : relevantRequests) {
			double currentTime=System.currentTimeMillis();
			if (request.getArrivalTime() <= switchDistanceOffPoint) {
				this.distanceType = 0;
			}
			
			if(request.getArrivalTime() <= this.orderHorizonLength*1.0/3.0 && distBuffer==4){
				this.distanceType = 3;
				this.maximumDistanceMeasureIncrease=3;
			}
			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(
					AggregateReferenceInformationAlgorithm.getDeliveryAreaSet(), request.getCustomer());

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
					currentAggregatedReferenceInformationNo = aggregatedReferenceInformationNo
							.get(rArea.getDeliveryAreaOfSet());
					currentCountAcceptable = countAcceptableCombinationOverReferences.get(rArea.getDeliveryAreaOfSet());
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
					currentAggregatedReferenceInformationNo = aggregatedReferenceInformationNo.get(rArea);
					currentCountAcceptable = countAcceptableCombinationOverReferences.get(rArea);
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
							request, AggregateReferenceInformationAlgorithm.getRegion(),
							AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
							AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
							(this.includeDriveFromStartingPosition == 1), this.getExpectedServiceTime(),
							possibleRoutings, this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates,
							relevantVaas, relevantOrders, bestRoutingSoFar, currentAcceptedTravelTime,
							consideredTimeWindows, bestRoutingsValueAfterInsertion, numberOfThreads);
			
			HashMap<Integer, Double> distanceMeasurePerTimeWindow = null;
			HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
			HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow = new HashMap<Integer, Routing>();
			ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
			for (Integer twId : bestRoutingsValueAfterInsertion.keySet()) {
				TimeWindow tw = AggregateReferenceInformationAlgorithm.getTimeWindowSet().getTimeWindowById(twId);
				if (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
						.getAcceptedOverallCostCoefficient() == null
						|| (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
								.getAcceptedOverallCostCoefficient() != null
								&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
										.containsKey(rArea)
								&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(rArea)
										.containsKey(tw))) {
					timeWindows.add(tw);
				} else if ((this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
						.getAcceptedOverallCostCoefficient() != null
						&& this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
								.getAcceptedOverallCostType() != null
						&& this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
								.getAcceptedOverallCostType() == 2
						&& (bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext()) < averageAdditionalCostPerOrder * 2.0)) {
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
			double noAssignmentValue = this.determineValue(rArea.getDeliveryAreaOfSet(), request.getArrivalTime() - 1,
					this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
					this.remainingCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
					this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
					this.acceptedAmountPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()))
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

					double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newRemainingCapacity = currentOverallRemainingCapacity
							- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							newRemainingCapacity);

					double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newAcceptedCostOverall = currentOverallAcceptedCost;

					if (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
							.getAcceptedOverallCostCoefficient() == null) {
						newAcceptedCostOverall = newAcceptedCostOverall
								+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					} else if (!(this.valueFunctionApproximationPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId()).getAcceptedOverallCostType() != null
							&& this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
									.getAcceptedOverallCostType() == 2)) {
						newAcceptedCostOverall = newAcceptedCostOverall
								+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(rArea)
										.get(tw);
					} else {
						if ((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext()) < averageAdditionalCostPerOrder) {
							newAcceptedCostOverall = newAcceptedCostOverall
									+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						} else if ((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext()) < averageAdditionalCostPerOrder * 2.0) {
							if (AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
									.containsKey(rArea)
									&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(rArea).containsKey(tw)) {
								newAcceptedCostOverall = newAcceptedCostOverall
										+ (bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait()
												+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
														.get(rArea).get(tw))
												/ 2.0;
							} else {
								newAcceptedCostOverall = newAcceptedCostOverall
										+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
							}
						} else {
							newAcceptedCostOverall = newAcceptedCostOverall
									+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(rArea).get(tw);
						}
					}

					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), newAcceptedCostOverall);

					Integer currentAcceptedOverall = this.acceptedAmountPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					this.acceptedAmountPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentAcceptedOverall + 1);

					double assignmentValue = this.determineValue(rArea.getDeliveryAreaOfSet(),
							request.getArrivalTime() - 1,
							this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()),
							this.remainingCapacityPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.acceptedCostPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()),
							this.acceptedAmountPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId()))
							* maximumValueAcceptable;

					twValue.put(tw, assignmentValue);
					this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.get(rArea.getDeliveryAreaOfSet().getId()).put(twId,
							--currentAccepted);

					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentOverallRemainingCapacity);
					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentOverallAcceptedCost);
					this.acceptedAmountPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentAcceptedOverall);
				}
			}
			ArrayList<AlternativeOffer> bestOfferedAlternatives = new ArrayList<AlternativeOffer>();
			// Find best subset from the time windows with value add

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue,
					AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), algo,
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

					Integer currentAcceptedOverall = this.acceptedAmountPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					this.acceptedAmountPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							currentAcceptedOverall + 1);

					double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newRemainingCapacity = currentOverallRemainingCapacity
							- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					this.remainingCapacityPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(),
							newRemainingCapacity);
					double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId());
					double newAcceptedCostOverall = currentOverallAcceptedCost;
					if (this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
							.getAcceptedOverallCostCoefficient() == null) {
						newAcceptedCostOverall = newAcceptedCostOverall
								+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
					} else if (!(this.valueFunctionApproximationPerDeliveryArea
							.get(rArea.getDeliveryAreaOfSet().getId()).getAcceptedOverallCostType() != null
							&& this.valueFunctionApproximationPerDeliveryArea.get(rArea.getDeliveryAreaOfSet().getId())
									.getAcceptedOverallCostType() == 2)) {
						newAcceptedCostOverall = newAcceptedCostOverall
								+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(rArea)
										.get(selectedAlt.getAlternative().getTimeWindows().get(0));
					} else {
						if ((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext()) < averageAdditionalCostPerOrder) {
							newAcceptedCostOverall = newAcceptedCostOverall
									+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						} else if ((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext()) < averageAdditionalCostPerOrder * 2.0) {
							if (AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
									.containsKey(rArea)
									&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(rArea)
											.containsKey(selectedAlt.getAlternative().getTimeWindows().get(0))) {
								newAcceptedCostOverall = newAcceptedCostOverall
										+ (bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait()
												+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
														.get(rArea)
														.get(selectedAlt.getAlternative().getTimeWindows().get(0)))
												/ 2.0;
							} else {
								newAcceptedCostOverall = newAcceptedCostOverall
										+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
							}
						} else {
							newAcceptedCostOverall = newAcceptedCostOverall
									+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(rArea).get(selectedAlt.getAlternative().getTimeWindows().get(0));
						}
					}
					this.acceptedCostPerDeliveryArea.put(rArea.getDeliveryAreaOfSet().getId(), newAcceptedCostOverall);

					order.setTimeWindowFinalId(twId);
					order.setAccepted(true);
					RouteElement elementToInsert = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
							.getKey();
					int routingId = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId()).getKey()
							.getTempRoutingId();
					elementToInsert.setOrder(order);
					DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
							relevantVaas, AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
							AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
							(includeDriveFromStartingPosition == 1));
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
					order.setReasonRejection("no_selection");
				}

			} else {

				if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {
					
					order.setReasonRejection("no_offer_favour");
				} else {
					order.setReasonRejection("no_offer_feasible");
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

			double duration = System.currentTimeMillis()-currentTime;
			if(duration>maxTime) maxTime=duration;
			overallTime+=duration;
		}

		try
		{
		    String filename= this.getClass().getName()
		    		+this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSetId()+".txt";
		    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
		    fw.write(overallTime/relevantRequests.size()+";"+maxTime+"\n");//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
		ArrayList<Route> routes = new ArrayList<Route>();

		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {

			for (Integer routeId : bestRoutingSoFarPerDeliveryArea.get(area).keySet()) {
				Route route = new Route();

				ArrayList<RouteElement> elements = bestRoutingSoFarPerDeliveryArea.get(area).get(routeId);
				// Delete dummy elements
				elements.remove(0);
				elements.remove(elements.size() - 1);
				for (RouteElement e : elements) {
					e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
					e.setTravelTime(e.getTravelTimeTo());
				}
				route.setRouteElements(elements);
				route.setVehicleAreaAssignmentId(vasPerDeliveryAreaAndVehicleNo.get(area.getId()).get(routeId).getId());
				routes.add(route);
			}
		}

		this.finalRouting = new Routing();
		this.finalRouting.setRoutes(routes);
		this.finalRouting.setPossiblyFinalRouting(true);
		this.orderSet = new OrderSet();
		orderSet.setElements(orders);
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.finalRouting.setOrderSet(orderSet);

	}

	private void initialiseBuffers() {

		this.alreadyAcceptedPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.remainingCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedCostPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedAmountPerDeliveryArea = new HashMap<Integer, Integer>();
		this.valueFunctionApproximationPerDeliveryArea = new HashMap<Integer, ValueFunctionApproximationModel>();
		this.lookupTablePerDeliveryArea = new HashMap<DeliveryArea, Double[][]>();
		this.lookupArrayPerDeliveryArea = new HashMap<DeliveryArea, Double[]>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();

		for (ValueFunctionApproximationModel model : this.valueFunctionApproximationModelSet.getElements()) {
			this.valueFunctionApproximationPerDeliveryArea.put(model.getDeliveryAreaId(), model);

			ObjectMapper mapper = new ObjectMapper();
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			NonParametricValueFunctionAddon addon = null;
			try {
				addon = mapper.readValue(model.getComplexModelJSON(), NonParametricValueFunctionAddon.class);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.lookupTablePerDeliveryArea.put(model.getDeliveryArea(), addon.getLookupTable());
			this.lookupArrayPerDeliveryArea.put(model.getDeliveryArea(), addon.getLookupArray());
			this.weightLf = addon.getWeight();

		}

	}

	public double determineValue(DeliveryArea area, int t, HashMap<Integer, Integer> alreadyAcceptedPerTw,
			Double remainingCapacity, Double acceptedCost, Integer acceptedAmount) {

		double value = 0.0;
		double valueLf = 0.0;

		if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getBasicCoefficient() != null)
			valueLf += this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getBasicCoefficient();
		if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getTimeCoefficient() != null)
			valueLf += this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getTimeCoefficient() * t
					/ ((double) this.orderHorizonLength);
		if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getRemainingCapacityCoefficient() != null)
			valueLf += this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
					.getRemainingCapacityCoefficient() * remainingCapacity
					/ this.overallCapacityPerDeliveryArea.get(area.getId());
		if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
				.getAcceptedOverallCostCoefficient() != null)
			valueLf += this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
					.getAcceptedOverallCostCoefficient() * acceptedCost
					/ this.overallCapacityPerDeliveryArea.get(area.getId());
		if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
				.getTimeCapacityInteractionCoefficient() != null)
			valueLf += this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
					.getTimeCapacityInteractionCoefficient() * ((double) acceptedAmount)
					* ((double) this.orderHorizonLength - t)
					/ ((double) this.orderHorizonLength * maximumAcceptableOverTw);

		for (ValueFunctionApproximationCoefficient c : this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
				.getCoefficients()) {
			valueLf += c.getCoefficient() * alreadyAcceptedPerTw.get(c.getTimeWindowId()) / maxAcceptedPerTw.get(
					AggregateReferenceInformationAlgorithm.getTimeWindowSet().getTimeWindowById(c.getTimeWindowId()));
		}
		;

		value = valueLf * this.weightLf;

		double valueLT;

		int timeIndex = (int) Math.floor((t / ((double) this.orderHorizonLength)) / (1.0 / 100.0));
		if (timeIndex == 100)
			timeIndex = 100 - 1;
		if ((this.valueFunctionApproximationPerDeliveryArea.get(area.getId()).getRemainingCapacityCoefficient() != null
				&& this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
						.getRemainingCapacityCoefficient() != 0)
				|| (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
						.getAcceptedOverallCostCoefficient() != null
						&& this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
								.getAcceptedOverallCostCoefficient() != 0)
				|| (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
						.getTimeCapacityInteractionCoefficient() != null
						&& this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
								.getTimeCapacityInteractionCoefficient() != 0)) {
			int otherIndex;
			if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
					.getAcceptedOverallCostCoefficient() != null
					&& this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
							.getAcceptedOverallCostCoefficient() != 0) {
				otherIndex = (int) Math
						.floor((acceptedCost / this.overallCapacityPerDeliveryArea.get(area.getId())) / (1.0 / 100.0));
			} else if (this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
					.getRemainingCapacityCoefficient() != null
					&& this.valueFunctionApproximationPerDeliveryArea.get(area.getId())
							.getRemainingCapacityCoefficient() != 0) {
				otherIndex = (int) Math.floor(
						(remainingCapacity / this.overallCapacityPerDeliveryArea.get(area.getId())) / (1.0 / 100.0));
			} else {
				otherIndex = (int) Math.floor((((double) acceptedAmount) * ((double) this.orderHorizonLength - t)
						/ ((double) this.orderHorizonLength * maximumAcceptableOverTw)) / (1.0 / 100.0));
			}
			if (otherIndex == 100)
				otherIndex = 100 - 1;
			if (otherIndex <0)
				otherIndex = 0;
			if (this.lookupTablePerDeliveryArea.get(area)[timeIndex][otherIndex] != null) {
				valueLT = this.lookupTablePerDeliveryArea.get(area)[timeIndex][otherIndex];
				value += valueLT * (1.0 - this.weightLf);
			} else {
				value = valueLf;
			}
		} else {
			// if (lookupArrayCount[timeIndex] != null) {
			// valueLT = lookupArraySum[timeIndex] /
			// lookupArrayCount[timeIndex];
			// value += valueLT * (1.0 - mesoWeightLf);
			// } else {
			value = valueLf;
			// }
		}

		return value;
	}

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
				acceptedPerTw.put(tw.getId(), 0);
			}
			this.alreadyAcceptedPerDeliveryAreaAndTimeWindow.put(area.getId(), acceptedPerTw);
		}
	}

	private void initialiseRemainingCapacity() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			initialiseRemainingCapacity(area);
		}
	}

	private void initialiseRemainingCapacity(DeliveryArea area) {
		this.remainingCapacityPerDeliveryArea.put(area.getId(), this.overallCapacityPerDeliveryArea.get(area.getId()));
	}

	private void initialiseAcceptedAmount() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			initialiseAcceptedAmount(area);
		}
	}

	private void initialiseAcceptedAmount(DeliveryArea area) {
		this.acceptedAmountPerDeliveryArea.put(area.getId(), 0);
	}

	private void initialiseAcceptedCostOverall() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			initialiseAcceptedCostOverall(area);
		}
	}

	private void initialiseAcceptedCostOverall(DeliveryArea area) {
		this.acceptedCostPerDeliveryArea.put(area.getId(), 0.0);
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public Routing getResult() {

		return this.finalRouting;
	}

}
