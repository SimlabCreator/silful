package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.rosuda.REngine.REXPMismatchException;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.ReferenceRouting;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LinearProgrammingService;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.service.support.ValueFunctionApproximationService;
import logic.utility.comparator.DemandSegmentWeightsExpectedValueDescComparator;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.comparator.PairDoubleValueAscComparator;
import logic.utility.comparator.RouteElementProfitDescComparator;

/**
 * Online Policy of ADPWihtOrienteering
 * 
 * @author M. Lang
 *
 */
public class ADPWithOrienteeringAcceptance implements AcceptanceAlgorithm {
	private static int numberOfThreads=1;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static double DISCOUNT_FACTOR=1.0;
//	private RConnection connection;
	private static double DEMAND_RATIO_SCARCITY_BORDER = 0.0;
	
	private static double stealingScarcityMultiplier=0.95;
	private static double demandRatioRevenue = 0.05;
	private static double MIN_RADIUS = 2.0;
	private static double costMultiplier = 0.3;
	private static boolean possiblyLargeOfferSet = true;
	private static double TIME_MULTIPLIER = 60.0;

	private TimeWindowSet timeWindowSet;
	private Region region;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private ValueFunctionApproximationModelSet modelSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private OrderSet orderSet;

	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;

	private ArrayList<Routing> targetRoutingResults;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;
	private Double expectedServiceTime;
	private int numberRoutingCandidates;
	private int numberInsertionCandidates;

	private HashMap<DeliveryArea, Double> daWeightsUpper;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper;
	private HashMap<DeliveryArea, Double> daWeightsLower;
	private double maximumLowerAreaWeight;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private int arrivalProcessId;
	private double arrivalProbability;

	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	private HashMap<Integer, ValueFunctionApproximationModel> valueFunctionApproximationPerDeliveryArea;

	private boolean usepreferencesSampled;
	private boolean considerOrienteeringNo;
	private boolean considerOrienteeringCosts;
	private boolean considerOrienteeringRoutingCandidates;
	private boolean useActualBasketValue;
	private boolean dynamicFeasibilityCheck;
	private Alternative noPurchaseAlternative;

	private HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingCandidatePerDeliveryArea;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<DeliveryArea, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>> aggregatedReferenceInformationCosts;
	private HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerUpperDeliveryAreaAndTw;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>> referenceRoutingsPerDeliveryArea;
	private ArrayList<Routing> referenceRoutingsList;
	private HashMap<Integer, HashMap<TimeWindow, Double>> demandMultiplierPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> demandMultiplierLowerAreaPerTimeWindow;
	private HashMap<Integer, HashMap<DemandSegment, HashMap<Alternative, Double>>> alternativeProbabilitiesPerDeliveryAreaAndDemandSegment;
	private HashMap<Integer, HashMap<Integer, Double>> maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCostsPerDeliveryAreaAndTw;
	private HashMap<Integer, Double> acceptedInsertionCostsPerDeliveryArea;
	private HashMap<Integer, Double> areaPotentialPerDeliveryArea;
	private HashMap<Integer, Double> maximumRadiusPerDeliveryArea;
	private HashMap<Integer, Double> maximumAreaPotentialPerDeliveryArea;

	private HashMap<Integer, Double> valueMultiplierPerLowerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow;
	private boolean theftBased;
	private boolean areaSpecificValueFunction;
	private boolean areaSpecificUtilityWeighting;
	private boolean considerRemainingCapacity;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private int stealingCounter = 0;
	private boolean areaSpecificDeterministicCheck;
	private boolean considerLeftOverPenalty;
	private HashMap<DeliveryArea, Double> initialOverallUtilityRatioAcrossTwsPerDeliveryArea;
	private HashMap<DeliveryArea, Double> currentSubAreaSpecificUtilityRatioAcrossTws;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> initialOverallUtilityRatioPerTw;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> currentAreaSpecificUtilityRatioPerTw;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private double utilityShift;

	// TODO: Add orienteering booleans to VFA?
	private static String[] paras = new String[] { "Constant_service_time", "actualBasketValue", "samplePreferences",
			"includeDriveFromStartingPosition", "no_routing_candidates", "no_insertion_candidates",
			"consider_orienteering_routing_candidates", "consider_orienteering_costs", "consider_orienteering_number",
			"dynamic_feasibility_check", "theft-based", "area_specific_deterministic_check",
			"consider_left_over_penalty", "area_specific_utility_weighting" };

	public ADPWithOrienteeringAcceptance(ValueFunctionApproximationModelSet valueFunctionApproximationModelSet,
			Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet, ArrayList<Routing> targetRoutingResults,
			OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet, Double expectedServiceTime,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue,
			Double includeDriveFromStartingPosition, Double actualBasketValue, int orderHorizonLength,
			Double samplePreferences, HashMap<DeliveryArea, Double> daWeightsUpperAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpperAreas,
			HashMap<DeliveryArea, Double> daWeightsLowerAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas, Double numberRoutingCandidates,
			Double numberInsertionCandidates, Double considerOrienteeringRoutingCandidates,
			Double considerOrienteeringNo, Double considerOrienteeringCosts, Double dynamicFeasibilityCheck,
			Double theftBased, Double areaSpecificDeterministicCheck, Double considerLeftOverPenalty,
			Double areaSpecificUtilityWeighting, HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			int arrivalProcessId) {

		this.region = region;
		this.orderRequestSet = orderRequestSet;
		this.modelSet = valueFunctionApproximationModelSet;
		this.targetRoutingResults = targetRoutingResults;
		this.deliveryAreaSet = deliveryAreaSet;
		this.daWeightsUpper = daWeightsUpperAreas;
		this.daSegmentWeightingsUpper = daSegmentWeightingsUpperAreas;
		this.daWeightsLower = daWeightsLowerAreas;
		this.daSegmentWeightingsLower = daSegmentWeightingsLowerAreas;
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.maximumRevenueValue = maximumRevenueValue;
		this.expectedServiceTime = expectedServiceTime;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.numberRoutingCandidates = numberRoutingCandidates.intValue();
		this.numberInsertionCandidates = numberInsertionCandidates.intValue();
		this.neighbors = neighbors;
		this.arrivalProcessId = arrivalProcessId;
		this.areaSpecificDeterministicCheck = (areaSpecificDeterministicCheck == 1.0);
		this.considerLeftOverPenalty = (considerLeftOverPenalty == 1.0);
		this.areaSpecificUtilityWeighting = (areaSpecificUtilityWeighting == 1.0);
		if (theftBased == 1.0) {
			this.theftBased = true;
		} else {
			this.theftBased = false;
		}
		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}

		if (dynamicFeasibilityCheck == 1.0) {
			this.dynamicFeasibilityCheck = true;
		} else {
			this.dynamicFeasibilityCheck = false;
		}

		if (considerOrienteeringNo == 1.0) {
			this.considerOrienteeringNo = true;
		} else {
			this.considerOrienteeringNo = false;
		}

		if (considerOrienteeringRoutingCandidates == 1.0) {
			this.considerOrienteeringRoutingCandidates = true;
		} else {
			this.considerOrienteeringRoutingCandidates = false;
		}

		if (considerOrienteeringCosts == 1.0) {
			this.considerOrienteeringCosts = true;
		} else {
			this.considerOrienteeringCosts = false;
		}

		if (actualBasketValue == 1.0) {
			this.useActualBasketValue = true;
		} else {
			this.useActualBasketValue = false;
		}

	};

	public void start() {

		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		this.alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		this.initialiseGlobal();

		// Sort order requests to arrive in time
		ArrayList<OrderRequest> relevantRequests = this.orderRequestSet.getElements();
		Collections.sort(relevantRequests, new OrderRequestArrivalTimeDescComparator());

		Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> aggregateInfos = this
				.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
						this.aggregatedReferenceInformationNo);
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw = aggregateInfos.getKey();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw = aggregateInfos.getValue();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerTwOverSubAreasPerDeliveryArea = ADPWithOrienteeringAcceptance
				.determineMaximumAcceptablePerTimeWindowOverSubareas(deliveryAreaSet, maxAcceptablePerSubAreaAndTw);

		Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> aggregateInfosNoAreaSpecific = this
				.determineCurrentAverageAndMaximumAcceptablePerTimeWindow();
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getKey();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getValue();

		// HashMap<Integer, Double> maximumDemandCapacityRatio =
		// determineMaximumInitialDemandCapacityRatio(
		// avgAcceptablePerSubAreaAndTw,
		// this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow);

		ArrayList<Order> orders = new ArrayList<Order>();
		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
		}

		// Go through requests
		for (OrderRequest request : relevantRequests) {
			long startTimeRequest = System.currentTimeMillis();
			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet,
					request.getCustomer());
			request.getCustomer().setTempDeliveryArea(rArea);
			DeliveryArea area = rArea.getDeliveryAreaOfSet();

			ArrayList<Order> relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
			HashMap<Integer, VehicleAreaAssignment> relevantVaas = this.vasPerDeliveryAreaSetAndVehicleNo
					.get(area.getId());
			HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area);
			Double currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(area);

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, RouteElement> feasibleTimeWindows = new HashMap<Integer, RouteElement>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();

			HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> orienteeringRoutingCandidateResult = null;
			HashMap<TimeWindow, Double> averageDistanceOrienteering = null;

			// Possible time windows for request
			ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
			ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet();
			for (ConsiderationSetAlternative alt : alternatives) {
				if (!alt.getAlternative().getNoPurchaseAlternative())
					timeWindows.addAll(alt.getAlternative().getTimeWindows());
			}

			ArrayList<TimeWindow> timeWindowCandidatesOrienteering = new ArrayList<TimeWindow>();

			// Check aggregated orienteering based feasibility (if
			// applicable)
			HashMap<Integer, Boolean> stolenFromOtherArea = new HashMap<Integer, Boolean>();
			int capacityForArea = 0;
			for (TimeWindow tw : timeWindows) {

				double currentDivisor;
				if ((averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
						- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId())) < 1) {
					currentDivisor = 1.0;
				} else {
					currentDivisor = (averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
							- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId()));
				}

				double currentDemandRatio = request.getArrivalTime()
						* this.demandMultiplierPerTimeWindow.get(area.getId()).get(tw) / currentDivisor;

				// TODO: What if 0?
				if (this.considerOrienteeringNo && currentDemandRatio > DEMAND_RATIO_SCARCITY_BORDER) {

					boolean feasible = false;
					if (avgAcceptablePerSubAreaAndTw.containsKey(rArea.getId())) {
						if (avgAcceptablePerSubAreaAndTw.get(rArea.getId()).containsKey(tw.getId())) {
							if (avgAcceptablePerSubAreaAndTw.get(rArea.getId()).get(tw.getId()) > 0) {
								timeWindowCandidatesOrienteering.add(tw);
								feasible = true;
								// TODO: might not end here if ratio condition
								// takes place
								capacityForArea += maxAcceptablePerSubAreaAndTw.get(rArea.getId()).get(tw.getId());
							}
						}
					}

					if (!feasible && this.theftBased
							&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(rArea.getId())
							&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
									.containsKey(tw.getId())
							&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
									.get(tw.getId()) > 0) {

						for (DeliveryArea nArea : this.neighbors.get(rArea)) {
							if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
								if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {
									// Can steal if area has capacity left and does
									// probably not need it
									int overallAvgAcceptableArea = 0;
									for (Integer twId : avgAcceptablePerSubAreaAndTw.get(nArea.getId()).keySet()) {
										overallAvgAcceptableArea += avgAcceptablePerSubAreaAndTw.get(nArea.getId())
												.get(twId);
									}
									
									//Has area more than needed overall? (Scarcity ratio!)
									double arrivalsInArea = (request.getArrivalTime() - 1.0)
											* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
											* this.daWeightsLower.get(nArea);
									double requestedInAreaOverall = arrivalsInArea
											* (1.0 - this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
													.get(nArea).get(null))*stealingScarcityMultiplier;
									long requestedInAreaOverallInt = Math.round(requestedInAreaOverall);
									if(requestedInAreaOverall<requestedInAreaOverallInt) requestedInAreaOverallInt=requestedInAreaOverallInt-1;
									double requestedForTimeWindow = arrivalsInArea*this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
											.get(nArea).get(tw);
									long requestedInAreaTwInt = Math.round(requestedForTimeWindow);
									if(requestedForTimeWindow<requestedInAreaTwInt) requestedInAreaTwInt=requestedInAreaTwInt-1;
									
									if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0
											&& overallAvgAcceptableArea >= requestedInAreaOverallInt) {
										timeWindowCandidatesOrienteering.add(tw);
										break;
									} else if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0
											&& avgAcceptablePerSubAreaAndTw.get(nArea.getId())
													.get(tw.getId()) >= requestedInAreaTwInt) {
										timeWindowCandidatesOrienteering.add(tw);
										break;

									
								}
								}
							}
						}

					}

				} else {
					timeWindowCandidatesOrienteering.add(tw);
				}
			}

			// Check feasibility regarding orienteering routings (if
			// applicable) and determine costs

			// Check feasibility and costs based on reference routings?

			if (this.considerOrienteeringRoutingCandidates) {

				// 1.) Define radius per time window
				HashMap<Integer, Double> radiusPerTimeWindow = new HashMap<Integer, Double>();
				for (TimeWindow tw : timeWindowCandidatesOrienteering) {

					double beginningDemandRatio = this.orderHorizonLength
							* this.demandMultiplierPerTimeWindow.get(area.getId()).get(tw)
							/ averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw);
					double currentDemandRatio;
					if ((averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
							- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId())) < 1) {
						currentDemandRatio = 0;
					} else {
						currentDemandRatio = Math.max(0, request.getArrivalTime()
								* this.demandMultiplierPerTimeWindow.get(area.getId()).get(tw)
								/ (averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
										- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId())));
					}

					double radiusValue = Math.max(MIN_RADIUS,
							this.maximumRadiusPerDeliveryArea.get(area.getId())
									- currentDemandRatio / beginningDemandRatio
											* (this.maximumRadiusPerDeliveryArea.get(area.getId()) - MIN_RADIUS));
					radiusPerTimeWindow.put(tw.getId(), radiusValue);
				}

				// 2.) Check if accepted request that is close and within
				// one of the time windows
				averageDistanceOrienteering = this.determineAverageDistanceForAllPossibleTimeWindows(request,
						this.aggregatedReferenceInformationCosts.get(area), timeWindowCandidatesOrienteering);
				orienteeringRoutingCandidateResult = this.determineOrienteeringInsertionCostApproximation(request,
						this.referenceRoutingsPerDeliveryArea.get(area), timeWindowCandidatesOrienteering,
						radiusPerTimeWindow, this.aggregatedReferenceInformationCosts.get(area),
						averageDistanceOrienteering);

				timeWindowCandidatesOrienteering.clear();
				timeWindowCandidatesOrienteering.addAll(orienteeringRoutingCandidateResult.keySet());
			} else if (this.considerOrienteeringCosts) {
				// Check costs only based on average distances

				// Determine average distance for all possible time windows
				averageDistanceOrienteering = this.determineAverageDistanceForAllPossibleTimeWindows(request,
						this.aggregatedReferenceInformationCosts.get(area), timeWindowCandidatesOrienteering);

			}

			// Check feasible time windows and lowest insertion costs based
			// on dynamic routing

			if (this.dynamicFeasibilityCheck) {
				// TA: Take lowest or average or max? Which measure to take
				// (travel time to, shift, shift without wait)
				currentAcceptedTravelTime = DynamicRoutingHelperService
						.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
								request, region, TIME_MULTIPLIER, this.timeWindowSet,
								(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime, 
								possibleRoutings, this.numberRoutingCandidates, this.numberInsertionCandidates,
								relevantVaas, relevantOrders, bestRoutingSoFar, currentAcceptedTravelTime,
								timeWindowCandidatesOrienteering, bestRoutingsValueAfterInsertion, numberOfThreads);
			} else {
				for (TimeWindow tw : timeWindowCandidatesOrienteering) {
					// Insert dummy route elements with orienteering costs
					// and order
					RouteElement e = new RouteElement();
					Order order = new Order();
					order.setOrderRequestId(request.getId());
					order.setOrderRequest(request);
					order.setTimeWindowFinalId(tw.getId());
					e.setOrder(order);
					e.setTempAdditionalCostsValue(0.0);
					feasibleTimeWindows.put(tw.getId(), e);
				}
			}

			// Set aggregated costs
			if (this.dynamicFeasibilityCheck || this.considerOrienteeringCosts
					|| this.considerOrienteeringRoutingCandidates) {
				for (Integer twId : feasibleTimeWindows.keySet()) {
					RouteElement candidate = feasibleTimeWindows.get(twId);
					if (this.considerOrienteeringCosts && this.considerOrienteeringRoutingCandidates) {
						if (this.dynamicFeasibilityCheck) {
							double remainingCapacityFactor = (this.overallCapacityPerDeliveryArea.get(area.getId())
									- currentAcceptedTravelTime - orders.size() * this.expectedServiceTime)
									/ this.overallCapacityPerDeliveryArea.get(area.getId());
							candidate.setTempAdditionalCostsValue((1 - remainingCapacityFactor)
									* (candidate.getTempShiftWithoutWait() - candidate.getServiceTime())
									+ remainingCapacityFactor * orienteeringRoutingCandidateResult
											.get(this.timeWindowSet.getTimeWindowById(twId)).getKey());
						} else {
							candidate.setTempAdditionalCostsValue(orienteeringRoutingCandidateResult
									.get(this.timeWindowSet.getTimeWindowById(twId)).getKey());
						}
					} else if (this.considerOrienteeringCosts) {
						if (this.dynamicFeasibilityCheck) {
							double remainingCapacityFactor = (this.overallCapacityPerDeliveryArea.get(area.getId())
									- currentAcceptedTravelTime - orders.size() * this.expectedServiceTime)
									/ this.overallCapacityPerDeliveryArea.get(area.getId());
							candidate.setTempAdditionalCostsValue((1 - remainingCapacityFactor)
									* (candidate.getTempShiftWithoutWait() - candidate.getServiceTime())
									+ remainingCapacityFactor * averageDistanceOrienteering
											.get(this.timeWindowSet.getTimeWindowById(twId)));
						} else {
							candidate.setTempAdditionalCostsValue(
									averageDistanceOrienteering.get(this.timeWindowSet.getTimeWindowById(twId)));
						}
					} else if (this.dynamicFeasibilityCheck) {
						candidate.setTempAdditionalCostsValue(
								candidate.getTempShiftWithoutWait() - candidate.getServiceTime());
					}
				}
			}

			// Determine current demand capacity ratios for t-1
			HashMap<Integer, Double> currentDemandCapacityRatio = new HashMap<Integer, Double>();
			HashMap<Integer, Double> maximumDemandCapacityRatioDub = new HashMap<Integer, Double>();
			// for (Integer saId : avgAcceptablePerSubAreaAndTw.keySet()) {
			// for (Integer twId :
			// avgAcceptablePerSubAreaAndTw.get(saId).keySet()) {
			// if (!currentDemandCapacityRatio.containsKey(twId)) {
			// currentDemandCapacityRatio.put(twId, 0.0);
			// maximumDemandCapacityRatioDub.put(twId, 0.0);
			// }
			// currentDemandCapacityRatio.put(twId,
			// currentDemandCapacityRatio.get(twId)
			// +
			// (this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(saId).get(twId)
			// * (request.getArrivalTime() - 1)
			// / (double) avgAcceptablePerSubAreaAndTw.get(saId).get(twId)));
			// maximumDemandCapacityRatioDub.put(twId,
			// maximumDemandCapacityRatioDub.get(twId) +
			// maximumDemandCapacityRatio.get(twId));
			// }
			// }

			double maxAreaPotential = 0;
			for (Integer twId : maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).keySet()) {
				maxAreaPotential += maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).get(twId);
			}
			maxAreaPotential = maxAreaPotential * this.maximumAreaPotentialPerDeliveryArea.get(area.getId());

			double noAssignmentValue = 0;
			if (!this.areaSpecificValueFunction) {

				double penalty = 0;
				if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
					penalty = this.determineLeftOverPenalty(area, null, null);
				}

				noAssignmentValue = ValueFunctionApproximationService
						.evaluateStateForLinearValueFunctionApproximationModelWithOrienteering(
								this.valueFunctionApproximationPerDeliveryArea.get(area.getId()),
								request.getArrivalTime() - 1, this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
								maxAcceptablePerAreaAndTw.get(area.getId()),
								acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()),
								this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),

								this.areaPotentialPerDeliveryArea.get(area.getId()), currentDemandCapacityRatio,
								orderHorizonLength, maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
								this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
								this.overallCapacityPerDeliveryArea.get(area.getId()), maxAreaPotential,
								maximumDemandCapacityRatioDub,
								TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
								alternativesToTimeWindows.keySet().size(), this.considerRemainingCapacity, penalty);
			} else {

				double penalty = 0;
				if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
					penalty = this.determineLeftOverPenaltyAreaSpecific(area, rArea, null);
				}
				noAssignmentValue = ValueFunctionApproximationService
						.evaluateStateForLinearValueFunctionApproximationModelWithOrienteeringForAreaSpecific(
								this.valueFunctionApproximationPerDeliveryArea.get(area.getId()),
								request.getArrivalTime() - 1, this.daWeightsLower.get(rArea),
								this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(rArea.getId()),
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()),
								maxAcceptablePerSubAreaAndTw.get(rArea.getId()), orderHorizonLength,
								this.maximumLowerAreaWeight,
								this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getId()),
								maxAcceptablePerTwOverSubAreasPerDeliveryArea.get(area.getId()),
								TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
								alternativesToTimeWindows.keySet().size(), this.considerRemainingCapacity, penalty);
			}

			// For all feasible, assume you accept -> get value
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> fallbackConservative = new HashMap<TimeWindow, Double>();
			double fallBackConservativeNoAssignmentValue = 0.0;
			if (this.areaSpecificDeterministicCheck && (capacityForArea + 1 > (request.getArrivalTime() - 1.0)
					* this.arrivalProbability * this.daWeightsLower.get(rArea))) {
				for (Integer twId : stolenFromOtherArea.keySet()) {
					maxAcceptablePerSubAreaAndTw.get(rArea.getId()).put(twId, 1);
				}
//				try {
//
//					fallBackConservativeNoAssignmentValue = LinearProgrammingService
//							.determineDeterministicLinearProgrammingSolutionPerDeliveryAreaWithR(
//									this.daSegmentWeightingsLower.get(rArea),
//									maxAcceptablePerSubAreaAndTw.get(rArea.getId()),
//									alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(rArea.getId()),
//									(request.getArrivalTime() - 1) * this.arrivalProbability
//											* this.daWeightsLower.get(rArea),
//									maximumRevenueValue, objectiveSpecificValues, timeWindowSet, connection,
//									demandRatioRevenue, alternativesToTimeWindows, noPurchaseAlternative, false, false).getKey();
//				} catch (RserveException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (REXPMismatchException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
			for (Integer twId : feasibleTimeWindows.keySet()) {
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
				if (this.areaSpecificValueFunction) {
					this.determineTimeWindowValuesAreaSpecific(twValue, request, tw, area, rArea,
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, maxAcceptablePerSubAreaAndTw,
							maxAcceptablePerTwOverSubAreasPerDeliveryArea.get(area.getId()), alternativesToTimeWindows);
				} else {
					this.determineTimeWindowValuesOverall(request, tw, area, rArea, twValue, maxAcceptablePerAreaAndTw,
							feasibleTimeWindows, currentDemandCapacityRatio, maxAreaPotential,
							maximumDemandCapacityRatioDub, alternativesToTimeWindows);
				}

				if (this.areaSpecificDeterministicCheck && (capacityForArea + 1 > (request.getArrivalTime() - 1.0)
						* this.arrivalProbability * this.daWeightsLower.get(rArea))) {
//					try {
//						int currentA = maxAcceptablePerSubAreaAndTw.get(rArea.getId()).get(twId);
//						// If currentA is 0, than this is a steal -> opportunity
//						// costs
//						maxAcceptablePerSubAreaAndTw.get(rArea.getId()).put(twId, currentA - 1);
//						fallbackConservative.put(tw, fallBackConservativeNoAssignmentValue - LinearProgrammingService
//								.determineDeterministicLinearProgrammingSolutionPerDeliveryAreaWithR(
//										this.daSegmentWeightingsLower.get(rArea),
//										maxAcceptablePerSubAreaAndTw.get(rArea.getId()),
//										alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(rArea.getId()),
//										(request.getArrivalTime() - 1) * this.arrivalProbability
//												* this.daWeightsLower.get(rArea),
//										maximumRevenueValue, objectiveSpecificValues, timeWindowSet, 
//										connection, demandRatioRevenue, alternativesToTimeWindows,
//										noPurchaseAlternative, false, false).getKey());
//						maxAcceptablePerSubAreaAndTw.get(rArea.getId()).put(twId, currentA);
//					} catch (RserveException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (REXPMismatchException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}

			}

			// Reset for stolen
			if (this.areaSpecificDeterministicCheck && (capacityForArea + 1 > (request.getArrivalTime() - 1.0)
					* this.arrivalProbability * this.daWeightsLower.get(rArea))) {
				for (Integer twId : stolenFromOtherArea.keySet()) {
					maxAcceptablePerSubAreaAndTw.get(rArea.getId()).put(twId, 0);
				}
			}

			// Find best subset from the time windows with value add
			HashMap<TimeWindow, Double> currentMultiplier = null;
			if (this.areaSpecificUtilityWeighting) {
				if (feasibleTimeWindows.keySet().size() > 0) {
					currentMultiplier = new HashMap<TimeWindow, Double>();
					Pair<Double, HashMap<TimeWindow, Double>> utilityRatios = this
							.determineCurrentUtilityRatioForSubArea(area, rArea, request.getArrivalTime());
					// double overallUtilityRatio = (utilityRatios
					// .getKey() <
					// this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.get(area)
					// ? utilityRatios.getKey()
					// /
					// this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.get(area)
					// : 1.0);
					double overallUtilityRatio = (utilityRatios
							.getKey() < this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.get(area)
									? utilityRatios.getKey()
											/ this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.get(area)
									: 1.0);
					for (Integer twId : feasibleTimeWindows.keySet()) {
						TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
						double twUtilityRatio;
						// Can be null if there is a steal
						if (utilityRatios.getValue().get(tw) == null) {
							twUtilityRatio = 1.0;
						} else if (utilityRatios.getValue().get(tw) < 1.0) {
							// Opportunity costs should be zero if there offered
							// more than requested
							twUtilityRatio = 0.0;
						} else {
							twUtilityRatio = utilityRatios.getValue().get(tw)
									/ this.initialOverallUtilityRatioPerTw.get(area).get(tw);
							if (twUtilityRatio > 1.0)
								twUtilityRatio = 1.0;
						}

						currentMultiplier.put(tw, ((twUtilityRatio * overallUtilityRatio) < 1.0
								? (twUtilityRatio * overallUtilityRatio) : 1.0));
					}
				}
			}

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue, maximumRevenueValue, objectiveSpecificValues, algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue,
					(this.areaSpecificDeterministicCheck && (capacityForArea + 1 > (request.getArrivalTime() - 1.0)
							* this.arrivalProbability * this.daWeightsLower.get(rArea))),
					fallbackConservative, currentMultiplier, DISCOUNT_FACTOR);

			ArrayList<AlternativeOffer> selectedOfferedAlternatives = bestOffer.getKey();

			// Simulate customer decision
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			if (selectedOfferedAlternatives.size() > 0) { // If windows are
															// offered
				// Sample selection from customer

				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							selectedOfferedAlternatives, order, this.alternativeSet.getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(selectedOfferedAlternatives, order);
				}

				if (selectedAlt != null) {
					int twId = selectedAlt.getAlternative().getTimeWindows().get(0).getId();
					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);

					double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId());
					double additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
					this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(),
							additionalCosts + currentAcceptedInsertion);

					currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId())
							.get(twId);
					additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
					this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(twId,
							additionalCosts + currentAcceptedInsertion);

					double currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
					// double newAreaPotential =
					// this.valueMultiplierPerDeliveryAreaAndTimeWindow
					// .get(subArea.getId()).get(twId);
					double newAreaPotential = this.valueMultiplierPerLowerDeliveryArea.get(rArea.getId());
					this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);

					order.setTimeWindowFinalId(twId);
					order.setAccepted(true);

					// Choose best routing after insertion for the respective tw
					if (this.dynamicFeasibilityCheck) {
						RouteElement elementToInsert = bestRoutingsValueAfterInsertion
								.get(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinalId()).getKey();
						int routingId = bestRoutingsValueAfterInsertion
								.get(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinalId()).getKey()
								.getTempRoutingId();
						DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
								vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), timeWindowSet, TIME_MULTIPLIER,
								(includeDriveFromStartingPosition == 1));
						bestRoutingSoFar = possibleRoutings.get(routingId);
						currentAcceptedTravelTime = bestRoutingsValueAfterInsertion
								.get(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinalId()).getValue();
					}
					// Update orienteering information
					if (this.considerOrienteeringNo || this.considerOrienteeringCosts) {
						ArrayList<Routing> toRemove = new ArrayList<Routing>();
						int a = 0;
						for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {

							if (this.aggregatedReferenceInformationNo.get(area).get(r).containsKey(rArea)
									&& this.aggregatedReferenceInformationNo.get(area).get(r).get(rArea).containsKey(
											feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinal())) {
								int currentCap = this.aggregatedReferenceInformationNo.get(area).get(r).get(rArea)
										.get(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinal());
								if (currentCap > 0) {
									this.aggregatedReferenceInformationNo.get(area).get(r).get(rArea).put(
											feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinal(),
											currentCap - 1);
								} else {

									// Already accepted in this area? -> can
									// steal
									// from neighbors
									if (this.theftBased
											&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow
													.containsKey(rArea.getId())
											&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
													.containsKey(twId)
											&& this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
													.get(twId) > 0) {
										// Check neighbor areas for capacity to
										// steal
										ArrayList<Pair<DeliveryArea, Double>> potentialStealingCandidates = new ArrayList<Pair<DeliveryArea, Double>>();
										HashMap<DeliveryArea, Integer> candidatesCapacities = new HashMap<DeliveryArea, Integer>();
										for (DeliveryArea nArea : this.neighbors.get(rArea)) {
											if (this.aggregatedReferenceInformationNo.get(area).get(r)
													.containsKey(nArea)
													&& this.aggregatedReferenceInformationNo.get(area).get(r).get(nArea)
															.containsKey(feasibleTimeWindows.get(twId).getOrder()
																	.getTimeWindowFinal())) {
												int currentCap2 = this.aggregatedReferenceInformationNo.get(area).get(r)
														.get(nArea).get(feasibleTimeWindows.get(twId).getOrder()
																.getTimeWindowFinal());
												if (currentCap2 > 0) {
													potentialStealingCandidates.add(new Pair<DeliveryArea, Double>(
															nArea,
															(request.getArrivalTime() - 1.0)
																	* ArrivalProcessService
																			.getMeanArrivalProbability(arrivalProcessId)
																	* this.daWeightsLower.get(nArea)
																	* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
																			.get(nArea)
																			.get(feasibleTimeWindows.get(twId)
																					.getOrder().getTimeWindowFinal())
																	/ (double) currentCap2));

													candidatesCapacities.put(nArea, currentCap2);
												}
											}
										}

										// Steal from neighbor with lowest
										// capacity
										// ratio
										if (potentialStealingCandidates.size() > 0) {
											Collections.sort(potentialStealingCandidates,
													new PairDoubleValueAscComparator());
											this.aggregatedReferenceInformationNo.get(area).get(r)
													.get(potentialStealingCandidates.get(0).getKey())
													.put(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinal(),
															candidatesCapacities.get(
																	potentialStealingCandidates.get(0).getKey()) - 1);

											stealingCounter++;
										} else {
											toRemove.add(r);
											if (considerOrienteeringCosts) {
												this.aggregatedReferenceInformationCosts.get(area).remove(a);
											}
											a--;
										}

									} else {
										toRemove.add(r);
										if (considerOrienteeringCosts) {
											this.aggregatedReferenceInformationCosts.get(area).remove(a);
										}
										a--;
									}

								}
							} else {
								toRemove.add(r);
								if (considerOrienteeringCosts) {
									this.aggregatedReferenceInformationCosts.get(area).remove(a);
								}
								a--;
							}

							a++;
						}

						for (Routing r : toRemove) {
							this.aggregatedReferenceInformationNo.get(area).remove(r);
						}

						if (this.theftBased) {
							if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(rArea.getId())) {
								this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(rArea.getId(),
										new HashMap<Integer, Integer>());
							}
							if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId())
									.containsKey(twId)) {
								this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).put(twId, 0);
							}
							this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).put(twId,
									this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).get(twId)
											+ 1);
						}

						Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = this
								.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
										this.aggregatedReferenceInformationNo);
						avgAcceptablePerSubAreaAndTw = updatedAggregates.getKey();
						maxAcceptablePerSubAreaAndTw = updatedAggregates.getValue();

						aggregateInfosNoAreaSpecific = this.determineCurrentAverageAndMaximumAcceptablePerTimeWindow();
						avgAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getKey();
						maxAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getValue();

					}

					if (considerOrienteeringRoutingCandidates) {
						HashMap<Routing, HashMap<Integer, Route>> rRoutings = new HashMap<Routing, HashMap<Integer, Route>>();
						for (RouteElement e : orienteeringRoutingCandidateResult
								.get(feasibleTimeWindows.get(twId).getOrder().getTimeWindowFinal()).getValue()) {
							e.setTempAlreadyAccepted(true);
							rRoutings.put(e.getRoute().getRouting(),
									this.referenceRoutingsPerDeliveryArea.get(area).get(e.getRoute().getRouting()));
						}
						this.referenceRoutingsPerDeliveryArea.put(area, rRoutings);
					}

				} else {
					order.setReasonRejection("Customer chose no-purchase option");
				}

			} else {

				if (feasibleTimeWindows.keySet().size() > 0) {
					order.setReasonRejection(
							"Better to offer nothing. Feasible:" + feasibleTimeWindows.keySet().size());
				} else {
					order.setReasonRejection("No feasible, considered time windows");
				}

			}

			if (this.dynamicFeasibilityCheck) {
				ordersPerDeliveryArea.get(area).add(order);
				// Update best and accepted travel time because it could change
				// also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area, bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(area, currentAcceptedTravelTime);
				if (order.getAccepted()) {

					acceptedOrdersPerDeliveryArea.get(area).add(order);

				}
			}

			orders.add(order);
			long timeNeeded = System.currentTimeMillis() - startTimeRequest;
			System.out.println(timeNeeded);
		}

		this.orderSet = new OrderSet();
		ArrayList<ReferenceRouting> rrs = new ArrayList<ReferenceRouting>();
		for (DeliveryArea area : this.aggregatedReferenceInformationNo.keySet()) {

			for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {

				int leftOver = 0;
				for (DeliveryArea subA : this.aggregatedReferenceInformationNo.get(area).get(r).keySet()) {
					for (TimeWindow tw : this.aggregatedReferenceInformationNo.get(area).get(r).get(subA).keySet()) {
						leftOver += this.aggregatedReferenceInformationNo.get(area).get(r).get(subA).get(tw);
					}
				}
				ReferenceRouting rr = new ReferenceRouting();
				rr.setDeliveryAreaId(area.getId());
				rr.setRoutingId(r.getId());
				rr.setRemainingCap(leftOver);
				rrs.add(rr);
			}
		}

		this.orderSet.setReferenceRoutingsPerDeliveryArea(rrs);
		this.orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.orderSet.setElements(orders);

	}

	private static HashMap<Integer, HashMap<Integer, Integer>> determineMaximumAcceptablePerTimeWindowOverSubareas(
			DeliveryAreaSet deliveryAreaSet, HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw) {

		HashMap<Integer, HashMap<Integer, Integer>> maximumPerTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();

		for (DeliveryArea area : deliveryAreaSet.getElements()) {
			for (DeliveryArea subArea : area.getSubset().getElements()) {
				for (Integer tw : maxAcceptablePerSubAreaAndTw.get(subArea.getId()).keySet()) {
					if (!maximumPerTimeWindow.containsKey(area.getId()))
						maximumPerTimeWindow.put(area.getId(), new HashMap<Integer, Integer>());
					if (!maximumPerTimeWindow.get(area.getId()).containsKey(tw))
						maximumPerTimeWindow.get(area.getId()).put(tw, 0);
					if (maximumPerTimeWindow.get(area.getId()).get(tw) < maxAcceptablePerSubAreaAndTw
							.get(subArea.getId()).get(tw)) {
						maximumPerTimeWindow.get(area.getId()).put(tw,
								maxAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw));
					}
				}
			}
		}

		return maximumPerTimeWindow;
	}

	

	private HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> determineOrienteeringInsertionCostApproximation(
			OrderRequest request, HashMap<Routing, HashMap<Integer, Route>> referenceRoutings,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering, HashMap<Integer, Double> radiusPerTimeWindow,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost,
			HashMap<TimeWindow, Double> averageDistance) {

		HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> approximation = new HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>>();

		HashMap<TimeWindow, Integer> numberPerTw = new HashMap<TimeWindow, Integer>();

		for (Routing r : referenceRoutings.keySet()) {
			for (Integer routeId : referenceRoutings.get(r).keySet()) {
				Route route = referenceRoutings.get(r).get(routeId);
				for (int i = 1; i < route.getRouteElements().size() - 1; i++) {
					RouteElement e = route.getRouteElements().get(i);
					if (timeWindowCandidatesOrienteering.contains(e.getOrder().getTimeWindowFinal())) {
						double distance = LocationService.calculateHaversineDistanceBetweenCustomers(
								request.getCustomer(), e.getOrder().getOrderRequest().getCustomer());
						if (distance < radiusPerTimeWindow.get(e.getOrder().getTimeWindowFinalId())) {
							double estimate = 0.0;
							// Look at element before and after
							if (route.getRouteElements().get(i - 1).isTempAlreadyAccepted()) {
								estimate += LocationService.calculateHaversineDistanceBetweenCustomers(
										request.getCustomer(),
										route.getRouteElements().get(i - 1).getOrder().getOrderRequest().getCustomer());
							} else {
								estimate += averageDistance.get(e.getOrder().getTimeWindowFinal());
							}

							if (route.getRouteElements().get(i + 1).isTempAlreadyAccepted()) {
								estimate += LocationService.calculateHaversineDistanceBetweenCustomers(
										request.getCustomer(),
										route.getRouteElements().get(i + 1).getOrder().getOrderRequest().getCustomer());
							} else {
								estimate += averageDistance.get(e.getOrder().getTimeWindowFinal());
							}

							// Is current element also already accepted? Nice:
							// close distance
							if (e.isTempAlreadyAccepted()) {
								estimate += distance;
							} else {
								estimate += averageDistance.get(e.getOrder().getTimeWindowFinal());

							}

							estimate = estimate / 3.0;

							// Update estimate for that time window
							if (approximation.containsKey(e.getOrder().getTimeWindowFinal())) {
								Pair<Double, ArrayList<RouteElement>> current = approximation
										.get(e.getOrder().getTimeWindowFinal());
								double newValue = (current.getKey() * numberPerTw.get(e.getOrder().getTimeWindowFinal())
										+ estimate) / numberPerTw.get(e.getOrder().getTimeWindowFinal());
								numberPerTw.put(e.getOrder().getTimeWindowFinal(),
										numberPerTw.get(e.getOrder().getTimeWindowFinal()) + 1);
								current.getValue().add(e);
								Pair<Double, ArrayList<RouteElement>> newCurrent = new Pair<Double, ArrayList<RouteElement>>(
										newValue, current.getValue());
								approximation.put(e.getOrder().getTimeWindowFinal(), newCurrent);

							} else {
								ArrayList<RouteElement> options = new ArrayList<RouteElement>();
								options.add(e);
								Pair<Double, ArrayList<RouteElement>> option = new Pair<Double, ArrayList<RouteElement>>(
										estimate, options);

								approximation.put(e.getOrder().getTimeWindowFinal(), option);
								numberPerTw.put(e.getOrder().getTimeWindowFinal(), 1);
							}
						}
						;
					}
				}
			}
		}

		return approximation;
	}

	private HashMap<TimeWindow, Double> determineAverageDistanceForAllPossibleTimeWindows(OrderRequest request,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering) {
		HashMap<TimeWindow, Double> averageDistance = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, Integer> countPerTw = new HashMap<TimeWindow, Integer>();
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Double>> r : aggregateInformationCost) {

			for (TimeWindow tw : timeWindowCandidatesOrienteering) {
				if (r.containsKey(request.getCustomer().getTempDeliveryArea())) {
					if (r.get(request.getCustomer().getTempDeliveryArea()).containsKey(tw)) {
						if (!averageDistance.containsKey(tw)) {
							averageDistance.put(tw, r.get(request.getCustomer().getTempDeliveryArea()).get(tw));
							countPerTw.put(tw, 1);
						} else {
							averageDistance.put(tw,
									(averageDistance.get(tw) * countPerTw.get(tw)
											+ r.get(request.getCustomer().getTempDeliveryArea()).get(tw))
											/ (countPerTw.get(tw) + 1));
							countPerTw.put(tw, countPerTw.get(tw) + 1);
						}
					}
				}
			}
		}

		return averageDistance;
	}

	private HashMap<Integer, Double> determineMaximumInitialDemandCapacityRatio(
			HashMap<Integer, HashMap<Integer, Integer>> expectedCapacities,
			HashMap<Integer, HashMap<Integer, Double>> demandMultiplier) {
		HashMap<Integer, Double> maximumRatio = new HashMap<Integer, Double>();

		for (Integer area : expectedCapacities.keySet()) {
			for (Integer tw : expectedCapacities.get(area).keySet()) {
				double ratio = (double) this.orderHorizonLength * demandMultiplier.get(area).get(tw)
						/ (double) expectedCapacities.get(area).get(tw);
				if (!maximumRatio.containsKey(tw)) {
					maximumRatio.put(tw, ratio);
				} else {
					if (maximumRatio.get(tw) < ratio) {
						maximumRatio.put(tw, ratio);
					}
				}
			}
		}

		return maximumRatio;
	}

	private Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> avgPerRouting) {

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> divisorPerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Go through aggr. no per routing
		for (DeliveryArea area : avgPerRouting.keySet()) {
			for (Routing r : avgPerRouting.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = avgPerRouting.get(area).get(r);
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!maxAcceptablePerSAAndTw.containsKey(areaS.getId())) {
							maxAcceptablePerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
						}
						if (!maxAcceptablePerSAAndTw.get(areaS.getId()).containsKey(tw.getId())) {
							maxAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
						}
						if (!avgAcceptablePerSAAndTw.containsKey(areaS.getId())) {
							avgAcceptablePerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
							divisorPerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
						}
						if (!avgAcceptablePerSAAndTw.get(areaS.getId()).containsKey(tw.getId())) {
							avgAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
							divisorPerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
						}

						avgAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(),
								avgAcceptablePerSAAndTw.get(areaS.getId()).get(tw.getId())
										+ separateNumbers.get(areaS).get(tw));
						// Only count this combination if capacity is >0
						// (otherwise,
						// not under feasible anymore after acceptance)
						if (separateNumbers.get(areaS).get(tw) > 0)
							divisorPerSAAndTw.get(areaS.getId()).put(tw.getId(),
									divisorPerSAAndTw.get(areaS.getId()).get(tw.getId()) + 1);
						if (maxAcceptablePerSAAndTw.get(areaS.getId()).get(tw.getId()) < separateNumbers.get(areaS)
								.get(tw)) {
							maxAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(),
									separateNumbers.get(areaS).get(tw));
						}
					}
				}
			}

		}

		for (Integer areaS : avgAcceptablePerSAAndTw.keySet()) {
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for (Integer tw : avgAcceptablePerSAAndTw.get(areaS).keySet()) {
				if (avgAcceptablePerSAAndTw.get(areaS).get(tw) > 0) {
					avgAcceptablePerSAAndTw.get(areaS).put(tw,
							avgAcceptablePerSAAndTw.get(areaS).get(tw) / divisorPerSAAndTw.get(areaS).get(tw));
				} else {
					toRemove.add(tw);

				}
			}

			for (Integer twId : toRemove) {
				avgAcceptablePerSAAndTw.get(areaS).remove(twId);
			}
		}

		return new Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>>(
				avgAcceptablePerSAAndTw, maxAcceptablePerSAAndTw);
	}

	private void initialiseGlobal() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		this.arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId);
		if (this.areaSpecificDeterministicCheck) {
//			try {
//				this.connection = new RConnection();
//				this.connection.eval("library(lpSolve)");
//			} catch (RserveException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		this.bestRoutingCandidatePerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		this.aggregateReferenceInformation();
		this.determineAverageAndMaximumAcceptablePerTimeWindow();
		this.chooseReferenceRoutings();
		this.determineDemandMultiplierPerTimeWindow();

		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		if (this.theftBased) {
			this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
							this.daSegmentWeightingsLower, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
							daSegmentWeightingsLower, timeWindowSet);
		}
		this.acceptedInsertionCostsPerDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Double>>();
		this.acceptedInsertionCostsPerDeliveryArea = new HashMap<Integer, Double>();
		this.areaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		this.prepareVehicleAssignmentsAndModelsForDeliveryAreas();
		this.prepareValueMultiplier();
		if (this.areaSpecificValueFunction)
			this.determineValueDemandMultiplierLowerAreaPerTimeWindow();
		this.areaSpecificValueFunction = this.modelSet.getIsAreaSpecific();
		this.determineMaximumLowerAreaWeight();
		// Define maximum radius for feasibility check
		if (this.considerOrienteeringRoutingCandidates) {
			this.maximumRadiusPerDeliveryArea = new HashMap<Integer, Double>();
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

				// TA: How to define the radius?
				double longDistance = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						area.getLon1(), area.getLon2(), area.getLat1(), area.getLat1());
				longDistance = longDistance / this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size();
				double latDistance = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						area.getLon1(), area.getLon1(), area.getLat1(), area.getLat2());
				latDistance = latDistance / this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size();

				double maximumRadius = longDistance;
				if (latDistance > maximumRadius)
					maximumRadius = latDistance;

				this.maximumRadiusPerDeliveryArea.put(area.getId(),
						maximumRadius / region.getAverageKmPerHour() * TIME_MULTIPLIER);
			}
		}

		this.initialiseAlreadyAcceptedPerTimeWindow();
		this.initialiseAcceptedCostsPerTimeWindow();

		this.initialiseAcceptedCosts();
		this.initialiseAreaPotential();

		for (Integer area : this.valueFunctionApproximationPerDeliveryArea.keySet()) {
			for (ValueFunctionApproximationCoefficient c : this.valueFunctionApproximationPerDeliveryArea.get(area)
					.getCoefficients()) {
				if (c.getType() == ValueFunctionCoefficientType.REMAINING_CAPACITY) {
					this.considerRemainingCapacity = true;
				}
			}
		}

		// Determine initial overall utility ratio
		if (this.areaSpecificUtilityWeighting) {

			double lowestUtilityValue = Double.MAX_VALUE;

			DeliveryArea a = this.daSegmentWeightingsUpper.keySet().iterator().next();
			for (DemandSegmentWeight w : this.daSegmentWeightingsUpper.get(a).getWeights()) {

				for (ConsiderationSetAlternative csa : w.getDemandSegment().getConsiderationSet()) {
					if (!csa.getAlternative().getNoPurchaseAlternative()) {
						if (lowestUtilityValue > csa.getCoefficient()
								+ w.getDemandSegment().getSegmentSpecificCoefficient()) {
							lowestUtilityValue = csa.getCoefficient()
									+ w.getDemandSegment().getSegmentSpecificCoefficient();
						}
						;
					}
				}
			}

			this.utilityShift = 1.0 - lowestUtilityValue;
			this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea = new HashMap<DeliveryArea, Double>();
			this.initialOverallUtilityRatioPerTw = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();
			for (DeliveryArea area : this.daWeightsUpper.keySet()) {
				this.determineInitialUtilityRatios(area, this.orderHorizonLength);
			}

		}

	}

	private void determineInitialUtilityRatios(DeliveryArea area, int time) {

		// Go over all reference routings and determine respective ratio per
		// time window and over all time windows
		HashMap<TimeWindow, Double> sumOfRatiosPerTimeWindow = new HashMap<TimeWindow, Double>();

		Double sumOfRatiosOverall = 0.0;

		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
			HashMap<TimeWindow, Double> requestedPerTimeWindow = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> offeredPerTimeWindow = new HashMap<TimeWindow, Double>();
			Double requestedOverTimeWindows = 0.0;
			Double offeredOverTimeWindows = 0.0;
			HashMap<DeliveryArea, Integer> capacityPerSubArea = new HashMap<DeliveryArea, Integer>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				if (!requestedPerTimeWindow.containsKey(tw)) {
					requestedPerTimeWindow.put(tw, 0.0);
					offeredPerTimeWindow.put(tw, 0.0);
				}

				// Go over sub-areas
				for (DeliveryArea subArea : this.aggregatedReferenceInformationNo.get(area).get(r).keySet()) {

					// Is there capacity for this time
					// window?
					if (this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) != null
							&& this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) > 0) {
						if (!capacityPerSubArea.containsKey(subArea)) {
							capacityPerSubArea.put(subArea, this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw));
						} else {
							capacityPerSubArea.put(subArea,capacityPerSubArea.get(subArea)
									+ this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw));
						}

						double loc = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw);
						double expectedArrivals = time * this.daWeightsLower.get(subArea)
								* ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId);
						// Sort demand segments according to expected value
						ArrayList<DemandSegmentWeight> dsWeights = this.daSegmentWeightingsLower.get(subArea)
								.getWeights();
						Collections.sort(dsWeights, new DemandSegmentWeightsExpectedValueDescComparator(
								this.maximumRevenueValue, this.objectiveSpecificValues));
						double lastSegmentUtility = 0.0;
						for (DemandSegmentWeight w : dsWeights) {
							Double altUtility = null;
							// Is it in consideration set?
							for (ConsiderationSetAlternative csa : w.getDemandSegment().getConsiderationSet()) {
								if (csa.getAlternativeId() == this.alternativesToTimeWindows.get(tw).getId()) {
									altUtility = csa.getCoefficient();
									break;
								}
							}

							double arrivalsW = expectedArrivals * w.getWeight();
							if (altUtility != null) {
								requestedPerTimeWindow.put(tw,
										requestedPerTimeWindow.get(tw)
												+ arrivalsW * (w.getDemandSegment().getSegmentSpecificCoefficient()
														+ altUtility + this.utilityShift));

								// Something left to offer?
								if (loc > 0) {
									offeredPerTimeWindow.put(tw,
											offeredPerTimeWindow.get(tw) + Math.min(loc, arrivalsW)
													* (w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
															+ this.utilityShift));
									loc = loc - Math.min(loc, arrivalsW);
								}

								lastSegmentUtility = w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
										+ this.utilityShift;
							}

						}

						// What if offered but not requested?
						// TODO: What if not requested at all per any segment?
						// (than lastSegmentUtility=0)
						if (loc > 0) {
							offeredPerTimeWindow.put(tw, offeredPerTimeWindow.get(tw) + loc * lastSegmentUtility);
						}

					}
				}

				// Add to sum over routings
				if (!sumOfRatiosPerTimeWindow.containsKey(tw)) {
					sumOfRatiosPerTimeWindow.put(tw, 0.0);
				}
				sumOfRatiosPerTimeWindow.put(tw, sumOfRatiosPerTimeWindow.get(tw)
						+ requestedPerTimeWindow.get(tw) / offeredPerTimeWindow.get(tw));
				requestedOverTimeWindows += requestedPerTimeWindow.get(tw);
				offeredOverTimeWindows += offeredPerTimeWindow.get(tw);
			}

			// Add to sum over routings overall
			//sumOfRatiosOverall += requestedOverTimeWindows / offeredOverTimeWindows;
			//Determine average scarcity over delivery areas
			double averageScarcity = 0.0;
			for(DeliveryArea subArea: capacityPerSubArea.keySet()){
				averageScarcity+= (time * this.daWeightsLower.get(subArea)
						* ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId))/capacityPerSubArea.get(subArea);
			}
			averageScarcity=averageScarcity/capacityPerSubArea.keySet().size();
			sumOfRatiosOverall +=averageScarcity;
		}

		// Calculate average
		HashMap<TimeWindow, Double> averageOfRatiosPerTimeWindow = new HashMap<TimeWindow, Double>();
		for (TimeWindow tw : sumOfRatiosPerTimeWindow.keySet()) {
			averageOfRatiosPerTimeWindow.put(tw, sumOfRatiosPerTimeWindow.get(tw)
					/ (double) this.aggregatedReferenceInformationNo.get(area).keySet().size());
		}
		this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.put(area,
				sumOfRatiosOverall / (double) this.aggregatedReferenceInformationNo.get(area).keySet().size());
		this.initialOverallUtilityRatioPerTw.put(area, averageOfRatiosPerTimeWindow);
	}

	private void determineInitialUtilityRatios_old(DeliveryArea area, int time) {

		double overallOffered = 0.0;
		double overallRequested = 0.0;
		HashMap<TimeWindow, Double> overallOfferedPerTw = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, Double> overallRequestedPerTw = new HashMap<TimeWindow, Double>();

		// Go through lower areas and determine offered and requested
		for (DeliveryArea subArea : area.getSubset().getElements()) {
			HashMap<TimeWindow, Double> requestedUtilityArea = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> offeredUtilityArea = new HashMap<TimeWindow, Double>();
			this.determineRequestedAndOfferedPerTimeWindowAndDeliveryArea_old(area, subArea, time, requestedUtilityArea,
					offeredUtilityArea);

			for (TimeWindow tw : requestedUtilityArea.keySet()) {
				if (!overallRequestedPerTw.containsKey(tw)) {
					overallRequestedPerTw.put(tw, 0.0);
					overallOfferedPerTw.put(tw, 0.0);
				}
				overallRequestedPerTw.put(tw, overallRequestedPerTw.get(tw) + requestedUtilityArea.get(tw));
				overallOfferedPerTw.put(tw, overallOfferedPerTw.get(tw) + offeredUtilityArea.get(tw));
			}

		}

		HashMap<TimeWindow, Double> ratioPerTw = new HashMap<TimeWindow, Double>();
		for (TimeWindow tw : overallRequestedPerTw.keySet()) {
			ratioPerTw.put(tw, overallRequestedPerTw.get(tw) / overallOfferedPerTw.get(tw));
			overallRequested += overallRequestedPerTw.get(tw);
			overallOffered += overallOfferedPerTw.get(tw);
		}

		this.initialOverallUtilityRatioAcrossTwsPerDeliveryArea.put(area, overallRequested / overallOffered);
		this.initialOverallUtilityRatioPerTw.put(area, ratioPerTw);
	}

	private Pair<Double, HashMap<TimeWindow, Double>> determineCurrentUtilityRatioForSubArea(DeliveryArea area,
			DeliveryArea subArea, int time) {

		// Go over all reference routings and determine respective ratio per
		// time window and over all time windows
		HashMap<TimeWindow, Double> sumOfRatiosPerTimeWindow = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, Integer> numberOfRoutingsWithOfferPerTimeWindow = new HashMap<TimeWindow, Integer>();

		Double sumOfRatiosOverall = 0.0;
		Integer numberOfRoutingsWithAnyOffer = 0;

		double expectedArrivals = time * this.daWeightsLower.get(subArea)
				* ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId);
		// Sort demand segments according to expected value
		ArrayList<DemandSegmentWeight> dsWeights = this.daSegmentWeightingsLower.get(subArea).getWeights();
		Collections.sort(dsWeights, new DemandSegmentWeightsExpectedValueDescComparator(this.maximumRevenueValue,
				this.objectiveSpecificValues));

		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {

			// Any offer for this area?
			if (this.aggregatedReferenceInformationNo.get(area).get(r).containsKey(subArea)) {

				boolean anyOffer = false;

				double requestedOverTimeWindows = 0.0;
				double offeredOverTimeWindows = 0.0;
				int offeredNumberOverTimeWindows=0;
				// Go over time windows
				for (TimeWindow tw : this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).keySet()) {
					if (!sumOfRatiosPerTimeWindow.containsKey(tw)) {
						sumOfRatiosPerTimeWindow.put(tw, 0.0);
						numberOfRoutingsWithOfferPerTimeWindow.put(tw, 0);
					}

					// Capacity for this time window?
					if (this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) > 0) {
						offeredNumberOverTimeWindows+=this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw);
						double loc = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw);
						anyOffer = true;
						numberOfRoutingsWithOfferPerTimeWindow.put(tw,
								numberOfRoutingsWithOfferPerTimeWindow.get(tw) + 1);
						double offeredForTimeWindow = 0.0;
						double requestedForTimeWindow = 0.0;
						double lastSegmentUtility = 0.0;
						for (DemandSegmentWeight w : dsWeights) {
							Double altUtility = null;
							// Is it in consideration set?
							for (ConsiderationSetAlternative csa : w.getDemandSegment().getConsiderationSet()) {
								if (csa.getAlternativeId() == this.alternativesToTimeWindows.get(tw).getId()) {
									altUtility = csa.getCoefficient();
									break;
								}
							}

							double arrivalsW = expectedArrivals * w.getWeight();
							if (altUtility != null) {
								requestedForTimeWindow = requestedForTimeWindow
										+ arrivalsW * (w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
												+ this.utilityShift);

								// Something left to offer?
								if (loc > 0) {
									offeredForTimeWindow = offeredForTimeWindow + Math.min(loc, arrivalsW)
											* (w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
													+ this.utilityShift);
									loc = loc - Math.min(loc, arrivalsW);
								}

								lastSegmentUtility = w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
										+ this.utilityShift;
							}

						}
						// What if offered but not requested?
						// TODO: What if not requested at all per any segment?
						// (than lastSegmentUtility=0)
						if (loc > 0) {
							offeredForTimeWindow = offeredForTimeWindow + loc * lastSegmentUtility;
						}

						sumOfRatiosPerTimeWindow.put(tw,
								sumOfRatiosPerTimeWindow.get(tw) + requestedForTimeWindow / offeredForTimeWindow);
						requestedOverTimeWindows = requestedOverTimeWindows + requestedForTimeWindow;
						offeredOverTimeWindows = offeredOverTimeWindows + offeredForTimeWindow;

					}

				}
				
				if (anyOffer) {
					numberOfRoutingsWithAnyOffer++;
					//sumOfRatiosOverall = sumOfRatiosOverall + requestedOverTimeWindows / offeredOverTimeWindows;
					sumOfRatiosOverall = sumOfRatiosOverall +expectedArrivals/((double) offeredNumberOverTimeWindows);
				}

			}
		}

		// Calculate average
		HashMap<TimeWindow, Double> averageOfRatiosPerTimeWindow = new HashMap<TimeWindow, Double>();
		for (TimeWindow tw : sumOfRatiosPerTimeWindow.keySet()) {
			if (numberOfRoutingsWithOfferPerTimeWindow.get(tw) > 0) {
				averageOfRatiosPerTimeWindow.put(tw,
						sumOfRatiosPerTimeWindow.get(tw) / (double) numberOfRoutingsWithOfferPerTimeWindow.get(tw));
			}
		}

		double overallRatio = 0.0;
		if (numberOfRoutingsWithAnyOffer > 0) {
			overallRatio = sumOfRatiosOverall / (double) numberOfRoutingsWithAnyOffer;
		} else {
			System.out.println("This is strange");
		}
		return new Pair<Double, HashMap<TimeWindow, Double>>(overallRatio, averageOfRatiosPerTimeWindow);
	}

	private Pair<Double, HashMap<TimeWindow, Double>> determineCurrentUtilityRatioForSubArea_old(DeliveryArea area,
			DeliveryArea subArea, int time) {
		HashMap<TimeWindow, Double> requestedUtilityArea = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, Double> offeredUtilityArea = new HashMap<TimeWindow, Double>();
		this.determineRequestedAndOfferedPerTimeWindowAndDeliveryArea_old(area, subArea, time, requestedUtilityArea,
				offeredUtilityArea);

		HashMap<TimeWindow, Double> ratioPerTw = new HashMap<TimeWindow, Double>();
		double overallRequested = 0.0;
		double overallOffered = 0.0;
		for (TimeWindow tw : requestedUtilityArea.keySet()) {
			ratioPerTw.put(tw, requestedUtilityArea.get(tw) / offeredUtilityArea.get(tw));
			overallRequested += requestedUtilityArea.get(tw);
			overallOffered += offeredUtilityArea.get(tw);
		}

		return new Pair<Double, HashMap<TimeWindow, Double>>(overallRequested / overallOffered, ratioPerTw);
	}

	private void determineRequestedAndOfferedPerTimeWindowAndDeliveryArea_old(DeliveryArea area, DeliveryArea subArea,
			int time, HashMap<TimeWindow, Double> requestedUtilityArea,
			HashMap<TimeWindow, Double> offeredUtilityArea) {
		// Sort demand segments according to expected value
		ArrayList<DemandSegmentWeight> dsWeights = this.daSegmentWeightingsLower.get(subArea).getWeights();
		Collections.sort(dsWeights, new DemandSegmentWeightsExpectedValueDescComparator(this.maximumRevenueValue,
				this.objectiveSpecificValues));
		double expectedArrivals = time * this.daWeightsLower.get(subArea)
				* ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId);
		// Go through relevant routings and look at values
		int numberOfRoutingsWithOfferFor47 = 0;
		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
			HashMap<TimeWindow, Integer> capPerTw = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea);

			// Is there capacity for this area at all?
			if (capPerTw != null) {
				// Determine requested utility for this subarea (considering the
				// tws with at least 1 capacity)

				for (TimeWindow tw : capPerTw.keySet()) {
					// Is anything offered?
					if (capPerTw.get(tw) > 0) {
						if (!requestedUtilityArea.containsKey(tw)) {
							requestedUtilityArea.put(tw, 0.0);
							offeredUtilityArea.put(tw, 0.0);
						}
						if (tw.getId() == 51 && capPerTw.get(tw) > 0)
							numberOfRoutingsWithOfferFor47++;
						double loc = capPerTw.get(tw);
						for (DemandSegmentWeight w : dsWeights) {
							Double altUtility = null;
							// Is it in consideration set?
							for (ConsiderationSetAlternative csa : w.getDemandSegment().getConsiderationSet()) {
								if (csa.getAlternativeId() == this.alternativesToTimeWindows.get(tw).getId()) {
									altUtility = csa.getCoefficient();
									break;
								}
							}

							double arrivalsW = expectedArrivals * w.getWeight();
							if (altUtility != null) {
								requestedUtilityArea.put(tw,
										requestedUtilityArea.get(tw)
												+ arrivalsW * (w.getDemandSegment().getSegmentSpecificCoefficient()
														+ altUtility + this.utilityShift));

								// Something left to offer?
								if (loc > 0) {
									offeredUtilityArea.put(tw,
											offeredUtilityArea.get(tw) + Math.min(loc, arrivalsW)
													* (w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
															+ this.utilityShift));
									loc = loc - Math.min(loc, arrivalsW);
								}
							}

						}
					}
				}
			}
		}
		System.out.println("Offers for 47 in area " + subArea.getId() + ": " + numberOfRoutingsWithOfferFor47);
	}

	private void determineAverageOverallRatioAndTimeWindowRatioPerDeliveryArea(DeliveryArea area, DeliveryArea subArea,
			int time) {
		// Sort demand segments according to expected value
		ArrayList<DemandSegmentWeight> dsWeights = this.daSegmentWeightingsLower.get(subArea).getWeights();
		Collections.sort(dsWeights, new DemandSegmentWeightsExpectedValueDescComparator(this.maximumRevenueValue,
				this.objectiveSpecificValues));
		double expectedArrivals = time * this.daWeightsLower.get(subArea)
				* ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId);
		int relevantRoutingNo = 0;
		HashMap<TimeWindow, Double> ratioPerTwAverage = new HashMap<TimeWindow, Double>();
		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
			HashMap<TimeWindow, Integer> capPerTw = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea);
			HashMap<TimeWindow, Double> requestedPerTw = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> offeredPerTw = new HashMap<TimeWindow, Double>();
			// Is there capacity for this area at all?
			if (capPerTw != null) {
				relevantRoutingNo++;

				for (TimeWindow tw : capPerTw.keySet()) {
					// Is anything offered?
					if (capPerTw.get(tw) > 0) {
						double loc = capPerTw.get(tw);
						for (DemandSegmentWeight w : dsWeights) {

							if (!requestedPerTw.containsKey(tw)) {
								requestedPerTw.put(tw, 0.0);
								offeredPerTw.put(tw, 0.0);
							}

							Double altUtility = null;
							// Is it in consideration set?
							for (ConsiderationSetAlternative csa : w.getDemandSegment().getConsiderationSet()) {
								if (csa.getAlternativeId() == this.alternativesToTimeWindows.get(tw).getId()) {
									altUtility = csa.getCoefficient();
									break;
								}
							}

							double arrivalsW = expectedArrivals * w.getWeight();
							if (altUtility != null) {
								requestedPerTw.put(tw,
										requestedPerTw.get(tw)
												+ arrivalsW * (w.getDemandSegment().getSegmentSpecificCoefficient()
														+ altUtility + this.utilityShift));

								// Something left to offer?
								if (loc > 0) {
									offeredPerTw.put(tw,
											offeredPerTw.get(tw) + Math.min(loc, arrivalsW)
													* (w.getDemandSegment().getSegmentSpecificCoefficient() + altUtility
															+ this.utilityShift));
									loc = loc - Math.min(loc, arrivalsW);
								}
							}

						}
					}
				}
			}

		}
	}

	private void determineTimeWindowValuesOverall(OrderRequest request, TimeWindow tw, DeliveryArea area,
			DeliveryArea rArea, HashMap<TimeWindow, Double> twValue,
			HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerAreaAndTw,
			HashMap<Integer, RouteElement> feasibleTimeWindows, HashMap<Integer, Double> currentDemandCapacityRatio,
			Double maxAreaPotential, HashMap<Integer, Double> maximumDemandCapacityRatioDub,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId());
		this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(tw.getId(), currentAccepted + 1);

		int currentRemainingCap = maxAcceptablePerAreaAndTw.get(area.getId()).get(tw.getId());
		maxAcceptablePerAreaAndTw.get(area.getId()).put(tw.getId(), currentRemainingCap - 1);

		double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId());
		double additionalCosts = feasibleTimeWindows.get(tw.getId()).getTempAdditionalCostsValue();
		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), additionalCosts + currentAcceptedInsertion);

		double currentAcceptedInsertionTw = this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId())
				.get(tw.getId());
		additionalCosts = feasibleTimeWindows.get(tw.getId()).getTempAdditionalCostsValue();
		this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(tw.getId(),
				additionalCosts + currentAcceptedInsertionTw);

		double currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
		// double newAreaPotential =
		// this.valueMultiplierPerDeliveryAreaAndTimeWindow
		// .get(subArea.getId()).get(twId);
		double newAreaPotential = this.valueMultiplierPerLowerDeliveryArea.get(rArea.getId());
		this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);

		// double currentDemandCapacityR;
		// //TODO: What if orienteering result not relevant anymore?
		//
		// double divisor;
		// if(orienteeringNoStillRelevantPerTw.get(twId)){
		// currentDemandCapacityR =
		// currentDemandCapacityRatio.get(twId);
		// divisor= ((double)
		// avgAcceptablePerSubAreaAndTw.get(rArea.getId()).get(twId) -
		// 1.0);
		// // TODO: What if 0?
		// if (divisor == 0.0)
		// divisor = 1.0;
		//
		// currentDemandCapacityRatio.put(twId,
		// currentDemandCapacityRatio.get(twId)
		// -
		// (this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(twId)
		// * (request.getArrivalTime() - 1)
		// / (double)
		// avgAcceptablePerSubAreaAndTw.get(rArea.getId()).get(twId))
		// +
		// this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(twId)
		// * (request.getArrivalTime() - 1) / divisor);
		// }else{
		// divisor=1.0;
		// currentDemandCapacityR= 0;
		// currentDemandCapacityRatio.put(twId,
		// 0.0);
		// }
		//
		double penalty = 0;
		if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
			penalty = this.determineLeftOverPenalty(area, rArea, tw);
		}
		double assignmentValue = ValueFunctionApproximationService
				.evaluateStateForLinearValueFunctionApproximationModelWithOrienteering(
						this.valueFunctionApproximationPerDeliveryArea.get(area.getId()), request.getArrivalTime() - 1,
						this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
						maxAcceptablePerAreaAndTw.get(area.getId()),
						acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()),
						this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),
						this.areaPotentialPerDeliveryArea.get(area.getId()), currentDemandCapacityRatio,
						orderHorizonLength, maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
						this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
						this.overallCapacityPerDeliveryArea.get(area.getId()), maxAreaPotential,
						maximumDemandCapacityRatioDub,
						TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
						alternativesToTimeWindows.keySet().size(), this.considerRemainingCapacity, penalty);

		twValue.put(tw, assignmentValue);
		this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(tw.getId(), currentAccepted);
		maxAcceptablePerAreaAndTw.get(area.getId()).put(tw.getId(), currentRemainingCap);

		this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(tw.getId(), currentAcceptedInsertionTw);

		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), currentAcceptedInsertion);

		this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential);

		// currentDemandCapacityRatio.put(twId, currentDemandCapacityR);
	}

	private void determineTimeWindowValuesAreaSpecific(HashMap<TimeWindow, Double> twValue, OrderRequest request,
			TimeWindow tw, DeliveryArea area, DeliveryArea subArea,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		int currentAccepted = alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId());
		alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(tw.getId(), currentAccepted + 1);

		int currentRemainingCapacity = maxAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw.getId());
		maxAcceptablePerSubAreaAndTw.get(subArea.getId()).put(tw.getId(), currentRemainingCapacity - 1);

		double penalty = 0;
		if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
			penalty = this.determineLeftOverPenaltyAreaSpecific(area, subArea, tw);
		}
		double v = ValueFunctionApproximationService
				.evaluateStateForLinearValueFunctionApproximationModelWithOrienteeringForAreaSpecific(
						this.valueFunctionApproximationPerDeliveryArea.get(area.getId()), request.getArrivalTime() - 1,
						this.daWeightsLower.get(subArea),
						this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()),
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()),
						maxAcceptablePerSubAreaAndTw.get(subArea.getId()), orderHorizonLength,
						this.maximumLowerAreaWeight,
						this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getId()),
						maxAcceptablePerTwOverSubAreas,
						TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
						alternativesToTimeWindows.keySet().size(), this.considerRemainingCapacity, penalty);

		alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(tw.getId(), currentAccepted);
		maxAcceptablePerSubAreaAndTw.get(subArea.getId()).put(tw.getId(), currentRemainingCapacity);
		twValue.put(tw, v);
	}

	private void determineMaximumLowerAreaWeight() {

		double maximumAreaWeight = 0.0;
		for (DeliveryArea area : this.daWeightsLower.keySet()) {
			if (maximumAreaWeight < this.daWeightsLower.get(area)) {
				maximumAreaWeight = this.daWeightsLower.get(area);
			}
		}

		this.maximumLowerAreaWeight = maximumAreaWeight;
	}

	private void determineValueDemandMultiplierLowerAreaPerTimeWindow() {

		this.demandMultiplierLowerAreaPerTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();

		for (DeliveryArea area : this.daWeightsLower.keySet()) {

			HashMap<Integer, Double> timeWindowMultiplier = new HashMap<Integer, Double>();
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLower.get(area).getWeights()) {

				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (ConsiderationSetAlternative alt : alts) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alt.getAlternative());
					offer.setAlternativeId(alt.getAlternativeId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, segW.getDemandSegment());

				for (AlternativeOffer alt : probs.keySet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						double m = this.daWeightsLower.get(area) * segW.getWeight() * probs.get(alt)
								* CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
										objectiveSpecificValues, segW.getDemandSegment());
						if (timeWindowMultiplier.containsKey(alt.getAlternative().getTimeWindows().get(0).getId())) {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0).getId(),
									m + timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0).getId()));
						} else {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0).getId(), m);
						}

					}
				}

			}
			this.demandMultiplierLowerAreaPerTimeWindow.put(area.getId(), timeWindowMultiplier);

		}

		for (DeliveryArea area : this.daWeightsUpper.keySet()) {
			if (!this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.containsKey(area.getId())) {
				this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.put(area.getId(),
						new HashMap<Integer, Double>());
			}

			for (Integer subAreaId : this.demandMultiplierLowerAreaPerTimeWindow.keySet()) {

				for (Integer twId : this.demandMultiplierLowerAreaPerTimeWindow.get(subAreaId).keySet()) {
					if (!this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.get(area.getId())
							.containsKey(twId)) {
						this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.get(area.getId()).put(twId,
								0.0);
					}
					if (this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.get(area.getId())
							.get(twId) < this.demandMultiplierLowerAreaPerTimeWindow.get(subAreaId).get(twId)) {
						this.maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow.get(area.getId()).put(twId,
								this.demandMultiplierLowerAreaPerTimeWindow.get(subAreaId).get(twId));
					}
				}

			}

		}

	}

	private void prepareVehicleAssignmentsAndModelsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaSetAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.valueFunctionApproximationPerDeliveryArea = new HashMap<Integer, ValueFunctionApproximationModel>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());

			this.overallCapacityPerDeliveryArea.put(area.getId(), 0.0);

		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);

			double capacity = this.overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
			capacity += (ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER + (this.expectedServiceTime - 1);
			// TODO: Check if depot travel time should be included
			this.overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);

		}

		for (ValueFunctionApproximationModel model : this.modelSet.getElements()) {
			this.valueFunctionApproximationPerDeliveryArea.put(model.getDeliveryAreaId(), model);
		}

	}

	private void determineDemandMultiplierPerTimeWindow() {

		this.demandMultiplierPerTimeWindow = new HashMap<Integer, HashMap<TimeWindow, Double>>();

		for (DeliveryArea area : this.daWeightsUpper.keySet()) {

			HashMap<TimeWindow, Double> timeWindowMultiplier = new HashMap<TimeWindow, Double>();
			for (DemandSegmentWeight segW : this.daSegmentWeightingsUpper.get(area).getWeights()) {

				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (ConsiderationSetAlternative alt : alts) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alt.getAlternative());
					offer.setAlternativeId(alt.getAlternativeId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, segW.getDemandSegment());

				for (AlternativeOffer alt : probs.keySet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						double m = this.daWeightsUpper.get(area) * segW.getWeight() * probs.get(alt);
						if (timeWindowMultiplier.containsKey(alt.getAlternative().getTimeWindows().get(0))) {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0),
									m + timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0)));
						} else {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0), m);
						}

					}
				}

			}
			this.demandMultiplierPerTimeWindow.put(area.getId(), timeWindowMultiplier);
		}

	}

	/**
	 * Choose orienteering results that serve as reference routings
	 */
	private void chooseReferenceRoutings() {

		// TA: other selection procedure? (for instance, save chosen ones!)

		this.referenceRoutingsPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>>();
		this.referenceRoutingsList = new ArrayList<Routing>();

		Random r = new Random();
		for (int i = 0; i < this.numberRoutingCandidates; i++) {
			int selection = r.nextInt(this.targetRoutingResults.size());
			while (this.referenceRoutingsList.contains(this.targetRoutingResults.get(selection))) {
				selection = r.nextInt(this.targetRoutingResults.size());
			}
			this.referenceRoutingsList.add(this.targetRoutingResults.get(selection));
			for (Route ro : this.targetRoutingResults.get(selection).getRoutes()) {
				if (!this.referenceRoutingsPerDeliveryArea.containsKey(ro.getVehicleAssignment().getDeliveryArea())) {
					this.referenceRoutingsPerDeliveryArea.put(ro.getVehicleAssignment().getDeliveryArea(),
							new HashMap<Routing, HashMap<Integer, Route>>());
				}
				if (!this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
						.containsKey(this.targetRoutingResults.get(selection))) {
					this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
							.put(this.targetRoutingResults.get(selection), new HashMap<Integer, Route>());
				}
				RouteElement reDepot1 = new RouteElement();
				reDepot1.setTravelTime(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						ro.getVehicleAssignment().getStartingLocationLat(),
						ro.getVehicleAssignment().getStartingLocationLon(),
						ro.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getLat(),
						ro.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getLon()));
				reDepot1.setTempAlreadyAccepted(true);
				Order order = new Order();
				OrderRequest ore = new OrderRequest();
				Customer customer = new Customer();
				customer.setLat(ro.getVehicleAssignment().getStartingLocationLat());
				customer.setLon(ro.getVehicleAssignment().getStartingLocationLon());
				ore.setCustomer(customer);
				order.setOrderRequest(ore);
				reDepot1.setOrder(order);
				reDepot1.setRouteId(ro.getId());
				ro.getRouteElements().add(0, reDepot1);

				RouteElement reDepot2 = new RouteElement();
				reDepot2.setTempAlreadyAccepted(true);
				Order order2 = new Order();
				OrderRequest ore2 = new OrderRequest();
				Customer customer2 = new Customer();
				customer2.setLat(ro.getVehicleAssignment().getEndingLocationLat());
				customer2.setLon(ro.getVehicleAssignment().getEndingLocationLon());
				ore2.setCustomer(customer2);
				order2.setOrderRequest(ore2);
				reDepot2.setOrder(order2);
				reDepot2.setRouteId(ro.getId());
				reDepot2.setTravelTime(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						ro.getVehicleAssignment().getEndingLocationLat(),
						ro.getVehicleAssignment().getEndingLocationLon(),
						ro.getRouteElements().get(ro.getRouteElements().size() - 1).getOrder().getOrderRequest()
								.getCustomer().getLat(),
						ro.getRouteElements().get(ro.getRouteElements().size() - 1).getOrder().getOrderRequest()
								.getCustomer().getLon()));
				ro.getRouteElements().add(ro.getRouteElements().size(), reDepot2);
				this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
						.get(this.targetRoutingResults.get(selection))
						.put(ro.getVehicleAssignment().getVehicleNo(), ro);
			}

		}
	}

	/**
	 * Prepare demand/ capacity ratio
	 */
	private void determineAverageAndMaximumAcceptablePerTimeWindow() {
		this.averageReferenceInformationNoPerDeliveryArea = new HashMap<Integer, HashMap<TimeWindow, Integer>>();
		// TA: Think about implementing maximal acceptable per time window
		// (separately)
		this.maximalAcceptablePerUpperDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<TimeWindow, Integer> acceptable = new HashMap<TimeWindow, Integer>();
			HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();

			// Go through aggr. no per routing
			for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = this.aggregatedReferenceInformationNo
						.get(area).get(r);
				HashMap<TimeWindow, Integer> acceptedPerTw = new HashMap<TimeWindow, Integer>();
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!acceptable.containsKey(tw)) {
							acceptable.put(tw, separateNumbers.get(areaS).get(tw));
							maximumAcceptable.put(tw.getId(), 0);
						} else {
							acceptable.put(tw, acceptable.get(tw) + separateNumbers.get(areaS).get(tw));
						}

						if (!acceptedPerTw.containsKey(tw)) {
							acceptedPerTw.put(tw, separateNumbers.get(areaS).get(tw));
						} else {
							acceptedPerTw.put(tw, acceptedPerTw.get(tw) + separateNumbers.get(areaS).get(tw));
						}
					}
				}

				// Update maximum acceptable per tw (find orienteering result
				// with highest number of acceptances per time window for this
				// higher area)
				for (TimeWindow tw : acceptedPerTw.keySet()) {
					if (acceptedPerTw.get(tw) > maximumAcceptable.get(tw.getId())) {
						maximumAcceptable.put(tw.getId(), acceptedPerTw.get(tw));
					}
				}
			}
			;

			// For that delivery area: Determine average acceptable per time
			// window

			for (TimeWindow tw : acceptable.keySet()) {
				acceptable.put(tw, acceptable.get(tw) / this.aggregatedReferenceInformationNo.get(area).size());
			}

			this.averageReferenceInformationNoPerDeliveryArea.put(area.getId(), acceptable);
			this.maximalAcceptablePerUpperDeliveryAreaAndTw.put(area.getId(), maximumAcceptable);

		}
	}

	private Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineCurrentAverageAndMaximumAcceptablePerTimeWindow() {
		HashMap<Integer, HashMap<Integer, Integer>> average = new HashMap<Integer, HashMap<Integer, Integer>>();
		// TA: Think about implementing maximal acceptable per time window
		// (separately)
		HashMap<Integer, HashMap<Integer, Integer>> maximal = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (DeliveryArea area : this.aggregatedReferenceInformationNo.keySet()) {
			HashMap<Integer, Integer> acceptable = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();

			// Go through aggr. no per routing
			for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = this.aggregatedReferenceInformationNo
						.get(area).get(r);
				HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!acceptable.containsKey(tw.getId())) {
							acceptable.put(tw.getId(), separateNumbers.get(areaS).get(tw));
							maximumAcceptable.put(tw.getId(), 0);
						} else {
							acceptable.put(tw.getId(), acceptable.get(tw.getId()) + separateNumbers.get(areaS).get(tw));
						}

						if (!acceptedPerTw.containsKey(tw.getId())) {
							acceptedPerTw.put(tw.getId(), separateNumbers.get(areaS).get(tw));
						} else {
							acceptedPerTw.put(tw.getId(),
									acceptedPerTw.get(tw.getId()) + separateNumbers.get(areaS).get(tw));
						}
					}
				}

				// Update maximum acceptable per tw (find orienteering result
				// with highest number of acceptances per time window for this
				// higher area)
				for (Integer tw : acceptedPerTw.keySet()) {
					if (acceptedPerTw.get(tw) > maximumAcceptable.get(tw)) {
						maximumAcceptable.put(tw, acceptedPerTw.get(tw));
					}
				}
			}
			;

			// For that delivery area: Determine average acceptable per time
			// window

			for (Integer tw : acceptable.keySet()) {
				acceptable.put(tw, acceptable.get(tw) / this.aggregatedReferenceInformationNo.get(area).size());
			}

			average.put(area.getId(), acceptable);
			maximal.put(area.getId(), maximumAcceptable);

		}

		return new Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>>(
				average, maximal);
	}

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	private void aggregateReferenceInformation() {

		// TA: distance - really just travel time to?

		this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
		this.aggregatedReferenceInformationCosts = new HashMap<DeliveryArea, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>>();

		// TA: consider that maximal acceptable costs is initialised too low
		this.maximalAcceptableCostsPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			this.aggregatedReferenceInformationNo.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
			this.aggregatedReferenceInformationCosts.put(area,
					new ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>());
			this.maximalAcceptableCostsPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (Routing routing : this.targetRoutingResults) {
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
					if (reId < r.getRouteElements().size() - 1) {
						travelTimeFrom = r.getRouteElements().get(reId + 1).getTravelTime();
					} else {
						travelTimeFrom = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
								re.getOrder().getOrderRequest().getCustomer().getLat(),
								re.getOrder().getOrderRequest().getCustomer().getLon(),
								r.getVehicleAssignment().getEndingLocationLat(),
								r.getVehicleAssignment().getEndingLocationLon());
					}

					Customer cus = re.getOrder().getOrderRequest().getCustomer();
					DeliveryArea subArea = LocationService
							.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
					if (!count.get(area).containsKey(subArea)) {
						count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
						distance.get(area).put(subArea, new HashMap<TimeWindow, Double>());
					}
					if (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal())) {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);
						distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								(re.getTravelTime() + travelTimeFrom) / 2.0);
					} else {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) + 1);
						distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								distance.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())
										+ (re.getTravelTime() + travelTimeFrom) / 2.0);
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
						distanceSum += distance.get(a).get(area).get(tw);
					}
				}
				this.aggregatedReferenceInformationCosts.get(a).add(distance.get(a));
				if (this.maximalAcceptableCostsPerDeliveryArea.get(a.getId()) < distanceSum) {
					this.maximalAcceptableCostsPerDeliveryArea.put(a.getId(), distanceSum);
				}
			}

		}
	}

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				acceptedPerTw.put(tw.getId(), 0);
			}
			this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
		}
	}

	private void initialiseAcceptedCosts(DeliveryArea area) {
		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAcceptedCosts() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAcceptedCosts(area);
		}
	}

	private void initialiseAcceptedCostsPerTimeWindow() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAcceptedCostsPerTimeWindow(area);
		}
	}

	private void initialiseAcceptedCostsPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Double> acceptedPerTw = new HashMap<Integer, Double>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			acceptedPerTw.put(tw.getId(), 0.0);
		}
		this.acceptedInsertionCostsPerDeliveryAreaAndTw.put(area.getId(), acceptedPerTw);
	}

	private void initialiseAreaPotential() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			initialiseAreaPotential(area);
		}
	}

	private void initialiseAreaPotential(DeliveryArea area) {
		this.areaPotentialPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerLowerDeliveryArea = new HashMap<Integer, Double>();
		this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment = new HashMap<Integer, HashMap<DemandSegment, HashMap<Alternative, Double>>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
			this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.put(area.getId(),
					new HashMap<Integer, Double>());

		}

		for (DeliveryArea area : this.daSegmentWeightingsLower.keySet()) {
			this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.put(area.getId(),
					new HashMap<DemandSegment, HashMap<Alternative, Double>>());
			double weightedValue = 0.0;
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLower.get(area).getWeights()) {
				this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
						.put(segW.getDemandSegment(), new HashMap<Alternative, Double>());
				double expectedValue = CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
						objectiveSpecificValues, segW.getDemandSegment());
				weightedValue += expectedValue * segW.getWeight();

				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				AlternativeOffer noPurchaseOffer = null;

				for (ConsiderationSetAlternative alt : alts) {
					if (alt.getAlternative().getNoPurchaseAlternative()) {
						noPurchaseOffer = new AlternativeOffer();
						noPurchaseOffer.setAlternative(alt.getAlternative());
						noPurchaseOffer.setAlternativeId(alt.getAlternativeId());
						this.noPurchaseAlternative = alt.getAlternative();
					}
				}

				for (ConsiderationSetAlternative alt : alts) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alt.getAlternative());
					offer.setAlternativeId(alt.getAlternativeId());
					offeredAlternatives.add(offer);

					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						ArrayList<AlternativeOffer> maxProbOffers = new ArrayList<AlternativeOffer>();
						maxProbOffers.add(offer);
						if (noPurchaseOffer != null) {
							maxProbOffers.add(noPurchaseOffer);
						}
						HashMap<AlternativeOffer, Double> probs = CustomerDemandService
								.getProbabilitiesForModel(maxProbOffers, segW.getDemandSegment());
						this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
								.get(segW.getDemandSegment()).put(alt.getAlternative(), probs.get(offer));
					}
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, segW.getDemandSegment());

				for (AlternativeOffer alt : probs.keySet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative() && alt != null) {
						double m = this.daWeightsLower.get(area) * segW.getWeight() * probs.get(alt) * expectedValue;

						if (!valueMultiplierPerLowerDeliveryAreaAndTimeWindow.containsKey(area.getId())) {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.put(area.getId(),
									new HashMap<Integer, Double>());
						}

						if (valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
								.containsKey(alt.getAlternative().getTimeWindows().get(0).getId())) {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).put(
									alt.getAlternative().getTimeWindows().get(0).getId(),
									valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
											.get(alt.getAlternative().getTimeWindows().get(0).getId()) + m);

						} else {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
									.put(alt.getAlternative().getTimeWindows().get(0).getId(), m);
						}

					} else {
						Alternative noPurch = null;
						if (alt != null) {
							noPurch = alt.getAlternative();
						}
						this.noPurchaseAlternative = noPurch;
						this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
								.get(segW.getDemandSegment()).put(noPurch, probs.get(alt));
					}
				}
			}

			if (weightedValue > this.maximumAreaPotentialPerDeliveryArea.get(area.getDeliveryAreaOfSet().getId())) {
				this.maximumAreaPotentialPerDeliveryArea.put(area.getDeliveryAreaOfSet().getId(), weightedValue);
			}
			this.valueMultiplierPerLowerDeliveryArea.put(area.getId(), weightedValue);

			for (Integer twId : valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).keySet()) {
				if (!maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
						.containsKey(twId)) {
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
							.put(twId, 0.0);
				}
				if (maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
						.get(twId) < valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId)) {
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
							.put(twId, valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId));
				}
				;
			}

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
	 * Calculates penalty for left-over capacity in area- time window
	 * combinations (at the end of the order horizon)
	 * 
	 * @param area
	 * @return
	 */
	private double determineLeftOverPenalty(DeliveryArea area, DeliveryArea areaToConsiderReduction,
			TimeWindow timeWindowToConsiderReduction) {
		double maxPenalty = 0.0;
		boolean consideredReduction = false;
		// Find left over routing with highest penalty costs
		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
			// Go through all combinations of area and time window
			double penaltyRouting = 0.0;
			for (DeliveryArea subArea : this.aggregatedReferenceInformationNo.get(area).get(r).keySet()) {
				for (TimeWindow tw : this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).keySet()) {
					// Add to penalty, if capacity is left-over
					if (this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) > 0) {

						if (areaToConsiderReduction != null
								&& this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) == 1
								&& areaToConsiderReduction.equals(subArea)
								&& timeWindowToConsiderReduction.equals(tw)) {
							consideredReduction = true;
						} else {

							int numberLeftOver = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea)
									.get(tw);
							if (areaToConsiderReduction != null && areaToConsiderReduction.equals(subArea)
									&& timeWindowToConsiderReduction.equals(tw)) {
								numberLeftOver = numberLeftOver - 1;
								consideredReduction = true;
							}
							// Check 5% value of lowest-segment value
							double minSegmentValue = Double.MAX_VALUE;
							for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(subArea).getWeights()) {
								if (dsw.getWeight() > 0
										&& CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
												objectiveSpecificValues, dsw.getDemandSegment(),
												demandRatioRevenue) < minSegmentValue) {
									minSegmentValue = CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
											objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue);
								}
								;
							}
							penaltyRouting -= minSegmentValue * numberLeftOver;
						}
					}

				}
			}

			// If it was a steal from another area, we might not have considered
			// the lost capacity
			if (areaToConsiderReduction != null && !consideredReduction) {
				double minSegmentValue = Double.MAX_VALUE;
				for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(areaToConsiderReduction)
						.getWeights()) {
					if (dsw.getWeight() > 0 && CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
							objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue) < minSegmentValue) {
						minSegmentValue = CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
								objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue);
					}
					;
				}
				penaltyRouting += minSegmentValue;
			}

			if (penaltyRouting < maxPenalty)
				maxPenalty = penaltyRouting;
		}

		return maxPenalty;
	}

	private double determineLeftOverPenaltyAreaSpecific(DeliveryArea area, DeliveryArea subArea,
			TimeWindow timeWindowToConsiderReduction) {
		double maxPenalty = 0.0;

		// Find left over routing with highest penalty costs
		for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
			// Go through all time windows
			double penaltyRouting = 0.0;
			boolean consideredReduction = false;
			for (TimeWindow tw : this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).keySet()) {
				// Add to penalty, if capacity is left-over
				if (this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) > 0) {

					if (timeWindowToConsiderReduction != null
							&& this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw) == 1
							&& timeWindowToConsiderReduction.equals(tw)) {
						consideredReduction = true;
					} else {

						int numberLeftOver = this.aggregatedReferenceInformationNo.get(area).get(r).get(subArea)
								.get(tw);
						if (timeWindowToConsiderReduction != null && timeWindowToConsiderReduction.equals(tw)) {
							numberLeftOver = numberLeftOver - 1;
							consideredReduction = true;
						}
						// Check 5% value of lowest-segment value
						double minSegmentValue = Double.MAX_VALUE;
						for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(subArea).getWeights()) {
							if (dsw.getWeight() > 0 && CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
									objectiveSpecificValues, dsw.getDemandSegment(),
									demandRatioRevenue) < minSegmentValue) {
								minSegmentValue = CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
										objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue);
							}
							;
						}
						penaltyRouting -= minSegmentValue * numberLeftOver;
					}
				}
			}

			// If it was a steal from another area, we might not have considered
			// the lost capacity
			if (timeWindowToConsiderReduction != null && !consideredReduction) {
				double minSegmentValue = Double.MAX_VALUE;
				for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(subArea).getWeights()) {
					if (dsw.getWeight() > 0 && CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
							objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue) < minSegmentValue) {
						minSegmentValue = CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
								objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue);
					}
					;
				}
				penaltyRouting += minSegmentValue;
			}

			if (penaltyRouting < maxPenalty)
				maxPenalty = penaltyRouting;
		}

		return maxPenalty;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return this.orderSet;
	}

}
