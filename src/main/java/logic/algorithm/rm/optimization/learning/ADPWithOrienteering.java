package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
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
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.entity.MomentumHelper;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.service.support.ValueFunctionApproximationService;
import logic.utility.SubsetProducer;
import logic.utility.comparator.PairDoubleValueAscComparator;
import logic.utility.comparator.PairIntegerValueDescComparator;
import logic.utility.comparator.RouteElementArrivalTimeAscComparator;
import logic.utility.comparator.RouteElementArrivalTimeDescComparator;
import logic.utility.comparator.RouteElementProfitDescComparator;

public class ADPWithOrienteering implements ValueFunctionApproximationAlgorithm {
	private static int numberOfThreads=1;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static double DISCOUNT_FACTOR=1.0;
	private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> LOG_LOSS_FUNCTION;
	private HashMap<Integer, HashMap<Integer, ArrayList<String>>> LOG_WEIGHTS;
	private static double stealingScarcityMultiplier=0.95;
	private static int counterLower = 0;
	private static int counterHigher = 0;
	private static double demandRatioRevenue = 0.05;
	private static double DEMAND_RATIO_SCARCITY_BORDER = 0.0;
	private static double MIN_RADIUS = 2.0;
	private static double E_GREEDY_VALUE = 0.5;
	private static double costMultiplier = 0.3;
	private static boolean possiblyLargeOfferSet = true;
	private static double TIME_MULTIPLIER = 60.0;
	private TimeWindowSet timeWindowSet;
	private AlternativeSet alternativeSet;
	private ValueFunctionApproximationModelSet modelSet;
	private ArrayList<Routing> targetRoutingResults;
	private ArrayList<Routing> previousResults;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private Double expectedServiceTime;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>> referenceRoutingsPerDeliveryArea;
	private ArrayList<Routing> referenceRoutingsList;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> aggregatedReferenceInformationNoSumOverSubareas;
	private HashMap<DeliveryArea, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>> aggregatedReferenceInformationCosts;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> basicCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> timeCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> areaPotentialCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> acceptedCostsCoefficientPerDeliveryArea;
	private HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficientsPerDeliveryArea;

	// Per upper area: what is the average accepted number per tw -> basis for
	// demand/capacity ratio
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> currentStatusOfAverageReferenceInformationNoPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerUpperDeliveryAreaAndTw;
	private HashMap<Integer, HashMap<Integer, Integer>> currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw;
	private HashMap<Integer, HashMap<TimeWindow, Double>> demandMultiplierPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> demandMultiplierLowerAreaPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> valueMultiplierPerLowerDeliveryArea;
	private HashMap<Integer, Double> maximumAreaPotentialPerDeliveryArea;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCostsPerDeliveryAreaAndTw;
	private HashMap<Integer, Double> acceptedInsertionCostsPerDeliveryArea;
	private HashMap<Integer, Double> areaPotentialPerDeliveryArea;
	private HashMap<Integer, Double> maximumRadiusPerDeliveryArea;
	private HashMap<Integer, Double> timeCapacityInteractionCoefficientPerDeliveryArea;

	private HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingCandidatePerDeliveryArea;
	private boolean considerInsertionCostsOverall;
	private boolean considerInsertionCostsPerTW;
	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<DeliveryArea, Double> daWeightsUpper;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper;
	private HashMap<DeliveryArea, Double> daWeightsLower;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower;
	private double maximumLowerAreaWeight;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private int arrivalProcessId;
	private double momentumWeight;
	private HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	private boolean considerAreaPotential;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;
	private boolean initialiseProblemSpecific;
	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;
	private Region region;
	private boolean useActualBasketValue;
	private Double annealingTemperature;
	private double stepSize;
	private Double explorationStrategy;
	private boolean usepreferencesSampled;
	private int numberRoutingCandidates;
	private int numberInsertionCandidates;
	private HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;
	private boolean considerOrienteeringRoutingCandidates;
	private boolean considerOrienteeringNo;
	private boolean considerOrienteeringCosts;
	private boolean useTargetForInitialisation;
	private boolean considerDemandCapacityRatio;
	private boolean considerTimeCapacityInteraction;
	private boolean dynamicFeasibilityCheck;
	private boolean considerLeftOverPenalty;
	private boolean theftBased;
	private boolean areaSpecificValueFunction;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private int stealingCounter;
	private boolean considerOrienteeringRemainingCapacity;
	private boolean considerOrienteeringRemainingCapacityTimeInteraction;
	private HashMap<Integer, Integer> trainingSetNumberPerDeliveryArea;

	// TA: Consider adding time*capacity interaction

	private static String[] paras = new String[] { "Constant_service_time", "stepsize_adp_learning",
			"actualBasketValue", "samplePreferences", "includeDriveFromStartingPosition",
			"initialiseCoefficientsProblemSpecific", "annealing_temperature_(Negative:no_annealing)",
			"exploration_(0:on-policy,1:wheel,2:e-greedy)", "momentum_weight", "no_routing_candidates",
			"no_insertion_candidates", "consider_insertion_costs_time_window", "consider_insertion_costs_overall",
			"consider_area_potential", "consider_orienteering_routing_candidates", "consider_orienteering_costs",
			"consider_orienteering_number", "consider_orienteering_remaining_capacity",
			"consider_orienteering_remaining_capacity_time", "target_for_initialisation",
			// "consider_demand_capacity_ratio",
			"time_cap_interaction", "dynamic_feasibility_check", "theft-based", "area_specific_value_function",
			"consider_left_over_penalty" };

	public ADPWithOrienteering(Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			ArrayList<Routing> previousResults, Double considerInsertionCostsOverall,
			Double considerInsertionCostsPerTW, DeliveryAreaSet deliveryAreaSet,
			HashMap<DeliveryArea, Double> daWeightsUpperAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpperAreas,
			HashMap<DeliveryArea, Double> daWeightsLowerAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas,
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, Double expectedServiceTime,
			Double considerAreaPotential, HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValueDouble, Double initialiseProblemSpecific, Double actualBasketValue,
			Double includeDriveFromStartingPosition, int orderHorizonLength, double stepSize,
			Double annealingTemperature, Double explorationStrategy, Double samplePreferences,
			Double numberRoutingCandidates, Double numberInsertionCandidates,
			Double considerOrienteeringRoutingCandidates, Double considerOrienteeringNo,
			Double considerOrienteeringCosts, Double considerOrienteeringRemainingCapacity,
			Double considerOrienteeringRemainingCapacityTimeInteraction, Double momentumWeight,
			Double useTargetForInitialisation,
			// Double considerDemandCapacityRatio.
			Double considerTimeCapacityInteraction, Double dynamicFeasibilityCheck, Double theftBased,
			Double areaSpecificValueFunction, Double considerLeftOverPenalty,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, int arrivalProcessId) {

		this.region = region;

		this.previousResults = previousResults;
		this.deliveryAreaSet = deliveryAreaSet;
		this.daWeightsUpper = daWeightsUpperAreas;
		this.daSegmentWeightingsUpper = daSegmentWeightingsUpperAreas;
		this.daWeightsLower = daWeightsLowerAreas;
		this.daSegmentWeightingsLower = daSegmentWeightingsLowerAreas;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		this.timeWindowSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.alternativeSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.maximumRevenueValue = maximumRevenueValueDouble;
		this.expectedServiceTime = expectedServiceTime;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.annealingTemperature = annealingTemperature;
		this.stepSize = stepSize;
		this.explorationStrategy = explorationStrategy;
		this.numberRoutingCandidates = numberRoutingCandidates.intValue();
		this.numberInsertionCandidates = numberInsertionCandidates.intValue();
		this.momentumWeight = momentumWeight;
		this.neighbors = neighbors;
		this.arrivalProcessId = arrivalProcessId;
		this.areaSpecificValueFunction = (areaSpecificValueFunction == 1.0);
		this.considerOrienteeringRemainingCapacity = (considerOrienteeringRemainingCapacity == 1.0);
		this.considerLeftOverPenalty = (considerLeftOverPenalty == 1.0);
		this.considerOrienteeringRemainingCapacityTimeInteraction = (considerOrienteeringRemainingCapacityTimeInteraction == 1.0);

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

		if (considerTimeCapacityInteraction == 1.0) {
			this.considerTimeCapacityInteraction = true;
		} else {
			this.considerTimeCapacityInteraction = false;
		}


		if (useTargetForInitialisation == 1.0) {
			this.useTargetForInitialisation = true;
		} else {
			this.useTargetForInitialisation = false;
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

		if (considerInsertionCostsOverall == 1.0) {
			this.considerInsertionCostsOverall = true;
		} else {
			this.considerInsertionCostsOverall = false;
		}

		if (actualBasketValue == 1.0) {
			this.useActualBasketValue = true;
		} else {
			this.useActualBasketValue = false;
		}

		if (considerInsertionCostsPerTW == 1.0) {
			this.considerInsertionCostsPerTW = true;
		} else {
			this.considerInsertionCostsPerTW = false;
		}

		if (considerAreaPotential == 1.0) {
			this.considerAreaPotential = true;
		} else {
			this.considerAreaPotential = false;
		}

		if (initialiseProblemSpecific == 1.0) {
			this.initialiseProblemSpecific = true;
		} else {
			this.initialiseProblemSpecific = false;
		}
	};

	public void start() {

		this.initialiseGlobal();

		// Solve problem per delivery area
		ArrayList<ValueFunctionApproximationModel> models = new ArrayList<ValueFunctionApproximationModel>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			// Logging
			HashMap<Integer, ArrayList<Double>> requestSetLoggingPerArea = new HashMap<Integer, ArrayList<Double>>();
			this.LOG_LOSS_FUNCTION.put(area.getId(), requestSetLoggingPerArea);
			HashMap<Integer, ArrayList<String>> requestSetLoggingWeightPerArea = new HashMap<Integer, ArrayList<String>>();
			this.LOG_WEIGHTS.put(area.getId(), requestSetLoggingWeightPerArea);

			this.applyADPForDeliveryArea(area);

			ValueFunctionApproximationModel model = new ValueFunctionApproximationModel();
			model.setDeliveryAreaId(area.getId());
			model.setBasicCoefficient(this.basicCoefficientPerDeliveryArea.get(area.getId()));
			model.setTimeCoefficient(this.timeCoefficientPerDeliveryArea.get(area.getId()));
			model.setCoefficients(this.variableCoefficientsPerDeliveryArea.get(area.getId()));

			if (this.considerTimeCapacityInteraction) {
				model.setTimeCapacityInteractionCoefficient(
						this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()));
			}

			if (this.considerInsertionCostsOverall)
				model.setAcceptedOverallCostCoefficient(this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()));

			if (this.considerAreaPotential) {
				model.setAreaPotentialCoefficient(this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()));
			}

			model.setObjectiveFunctionValueLog(this.LOG_LOSS_FUNCTION.get(area.getId()));
			model.setWeightsLog(this.LOG_WEIGHTS.get(area.getId()));
			models.add(model);

		}

		// Prepare final model set
		ValueFunctionApproximationModelSet set = new ValueFunctionApproximationModelSet();
		set.setDeliveryAreaSetId(this.deliveryAreaSet.getId());
		set.setTimeWindowSetId(this.timeWindowSet.getId());
		set.setTypeId(1); // For linear model
		set.setIsCommitted(true);
		set.setIsNumber(true);
		set.setElements(models);

		this.modelSet = set;

	}

	private void applyADPForDeliveryArea(DeliveryArea area) {

		// Per order request set, train the value function
		int orderRequestNumber = 0;

		for (Integer requestSetId : this.orderRequestsPerDeliveryArea.get(area.getId()).keySet()) {
			trainingSetNumberPerDeliveryArea.put(area.getId(), orderRequestNumber);
			this.updateValueFunctionWithOrderRequests(area, requestSetId);
			orderRequestNumber++;
		}

	}

	private void updateValueFunctionWithOrderRequests(DeliveryArea area, Integer requestSetId) {

		System.out.println("Next request set: " + requestSetId);

		this.stealingCounter = 0;
		// Init orders
		ArrayList<Order> orders = new ArrayList<Order>();
		HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = new HashMap<Integer, ArrayList<RouteElement>>();
		Double currentAcceptedTravelTime = null;

		this.initialiseAlreadyAcceptedPerTimeWindow(area);
		if (this.considerInsertionCostsPerTW)
			this.initialiseAcceptedCostsPerTimeWindow(area);
		if (this.considerInsertionCostsOverall)
			this.initialiseAcceptedCosts(area);
		if (this.considerAreaPotential)
			this.initialiseAreaPotential(area);

		// Logging
		this.LOG_LOSS_FUNCTION.get(area.getId()).put(requestSetId, new ArrayList<Double>());
		this.LOG_WEIGHTS.get(area.getId()).put(requestSetId, new ArrayList<String>());

		// Sort order requests to arrive in time
		ArrayList<Integer> relevantRequests = new ArrayList<Integer>();
		relevantRequests.addAll(this.orderRequestsPerDeliveryArea.get(area.getId()).keySet());
		Collections.sort(relevantRequests, Collections.reverseOrder());

		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		HashMap<TimeWindow, Alternative> alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		// Copy aggregate information
		ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo = null;
		ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost = null;
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw = null;
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw = null;
		HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas = null; // Maximum
																			// value
																			// for
																			// normalisation
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, Double> maximumDemandCapacityRatio = null;
		HashMap<Integer, Integer> averageAcceptablePerTimeWindow = null;
		HashMap<Integer, Integer> maxAcceptablePerTimeWindow = null;

		if (this.considerOrienteeringNo) {
			aggregateInformationNo = this.copyAggregateNoInformation(area);

			Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = ADPWithOrienteering
					.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(aggregateInformationNo);
			avgAcceptablePerSubAreaAndTw = updatedAggregates.getKey();
			maxAcceptablePerSubAreaAndTw = updatedAggregates.getValue();

			if (this.areaSpecificValueFunction) {

				// TODO: Maybe no need to calculate every time (global init?)
				maxAcceptablePerTwOverSubAreas = ADPWithOrienteering
						.determineMaximumAcceptablePerTimeWindowOverSubareas(maxAcceptablePerSubAreaAndTw);
			}

			this.determineCurrentStatusOfAverageAndMaximumAcceptablePerTimeWindow(area, aggregateInformationNo);
			// Find maximum initial demand/capacity ratio and determine area
			// multiplier
			if (this.considerDemandCapacityRatio) {
				maximumDemandCapacityRatio = determineMaximumInitialDemandCapacityRatio(avgAcceptablePerSubAreaAndTw,
						this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow);
			}
		}

		if (this.considerOrienteeringCosts)
			aggregateInformationCost = this.copyAggregateCostsInformation(area);

		HashMap<Routing, HashMap<Integer, Route>> referenceRoutings = null;
		if (this.considerOrienteeringRoutingCandidates) {
			referenceRoutings = new HashMap<Routing, HashMap<Integer, Route>>();
			for (Routing r : this.referenceRoutingsPerDeliveryArea.get(area).keySet()) {
				referenceRoutings.put(r, this.referenceRoutingsPerDeliveryArea.get(area).get(r));
			}
		}

		// Go through requests and update value function
		for (int t = this.orderHorizonLength; t > 0; t--) {

			OrderRequest request;
			if (this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).containsKey(t)) {
				request = this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).get(t);
			} else {
				request = null;
			}

			HashMap<Integer, RouteElement> feasibleTimeWindows = new HashMap<Integer, RouteElement>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();

			DeliveryArea subArea = null;
			HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> orienteeringRoutingCandidateResult = null;
			HashMap<TimeWindow, Double> averageDistanceOrienteering = null;

			if (request != null) {

				subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
						request.getCustomer());
				subArea.setSetId(area.getSubsetId());
				request.getCustomer().setTempDeliveryArea(subArea);

				ArrayList<TimeWindow> timeWindowCandidatesOrienteering = new ArrayList<TimeWindow>();

				// Check aggregated orienteering based feasibility (if
				// applicable)

				this.checkFeasibilityBasedOnOrienteeringNo(request, t, area, subArea, timeWindowCandidatesOrienteering,
						avgAcceptablePerSubAreaAndTw, alreadyAcceptedPerSubDeliveryAreaAndTimeWindow);

				// Check feasibility regarding orienteering routings (if
				// applicable) and determine costs

				// Check feasibility and costs based on reference routings?

				if (this.considerOrienteeringRoutingCandidates) {
					this.determineOrienteeringRoutingCandidateResults(request, subArea,
							orienteeringRoutingCandidateResult, averageDistanceOrienteering,
							timeWindowCandidatesOrienteering, referenceRoutings, aggregateInformationCost);
					timeWindowCandidatesOrienteering.clear();
					timeWindowCandidatesOrienteering.addAll(orienteeringRoutingCandidateResult.keySet());
				} else if (this.considerOrienteeringCosts) {

					// Check costs only based on average distances
					averageDistanceOrienteering = this.determineAverageDistanceForAllPossibleTimeWindows(request,
							aggregateInformationCost, timeWindowCandidatesOrienteering);

				}

				// Check feasible time windows and lowest insertion costs based
				// on dynamic routing

				if (this.dynamicFeasibilityCheck) {

					currentAcceptedTravelTime = DynamicRoutingHelperService
							.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
									request, region, TIME_MULTIPLIER, this.timeWindowSet,
									(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime,
									possibleRoutings, this.numberRoutingCandidates, this.numberInsertionCandidates,
									this.vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), orders, bestRoutingSoFar,
									currentAcceptedTravelTime, timeWindowCandidatesOrienteering, 
									bestRoutingsValueAfterInsertion, numberOfThreads);
					
					
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

			}

			// Set aggregated costs
			if ((this.considerInsertionCostsOverall || this.considerInsertionCostsPerTW)) {
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
					} else {
						candidate.setTempAdditionalCostsValue(
								candidate.getTempShiftWithoutWait() - candidate.getServiceTime());
					}
				}

			}

			// Choose offer set and update value function
			RouteElement newElement = this.updateValueFunctionIndividualWithOrderRequest(t, area, request,
					feasibleTimeWindows, requestSetId, alternativesToTimeWindows, avgAcceptablePerSubAreaAndTw,
					maximumDemandCapacityRatio, alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
					maxAcceptablePerSubAreaAndTw, maxAcceptablePerTwOverSubAreas, aggregateInformationNo);

			// If customer chose a time window, the order needs to be added to
			// the set of orders for subsequent insertion into routes
			// Also: insert order into best schedule -> update best schedule
			// Also: update aggregated information about orienteering results
			// (which ones are still applicable? reduce respective number)
			if (newElement != null) {
				// Choose best routing after insertion for the respective tw
				if (this.dynamicFeasibilityCheck) {
					RouteElement elementToInsert = bestRoutingsValueAfterInsertion
							.get(newElement.getOrder().getTimeWindowFinalId()).getKey();
					int routingId = bestRoutingsValueAfterInsertion.get(newElement.getOrder().getTimeWindowFinalId())
							.getKey().getTempRoutingId();
					DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
							vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), timeWindowSet, TIME_MULTIPLIER,
							(includeDriveFromStartingPosition == 1));
					bestRoutingSoFar = possibleRoutings.get(routingId);
					currentAcceptedTravelTime = bestRoutingsValueAfterInsertion
							.get(newElement.getOrder().getTimeWindowFinalId()).getValue();
				}

				orders.add(newElement.getOrder());

				// Update orienteering information
				if (this.considerOrienteeringNo || this.considerOrienteeringCosts) {
					this.updateOrienteeringNoAndCostInformation(subArea, newElement, aggregateInformationNo,
							aggregateInformationCost, alreadyAcceptedPerSubDeliveryAreaAndTimeWindow);

					if (this.theftBased) {
						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
									new HashMap<Integer, Integer>());
						}
						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.containsKey(newElement.getOrder().getTimeWindowFinalId())) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.put(newElement.getOrder().getTimeWindowFinalId(), 0);
						}
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(
								newElement.getOrder().getTimeWindowFinalId(),
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
										.get(newElement.getOrder().getTimeWindowFinalId()) + 1);
					}

					Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = ADPWithOrienteering
							.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
									aggregateInformationNo);
					avgAcceptablePerSubAreaAndTw = updatedAggregates.getKey();
					maxAcceptablePerSubAreaAndTw = updatedAggregates.getValue();

					if (!this.areaSpecificValueFunction) {
						this.determineCurrentStatusOfAverageAndMaximumAcceptablePerTimeWindow(area,
								aggregateInformationNo);

					}

				}

				if (considerOrienteeringRoutingCandidates) {
					this.updateReferenceRoutingInformation(referenceRoutings, newElement, subArea,
							orienteeringRoutingCandidateResult);
				}

			}

		}

		// Reset reference routings
		if (considerOrienteeringRoutingCandidates) {
			for (Routing r : this.referenceRoutingsPerDeliveryArea.get(area).keySet()) {
				for (Integer vId : this.referenceRoutingsPerDeliveryArea.get(area).get(r).keySet()) {
					for (RouteElement e : this.referenceRoutingsPerDeliveryArea.get(area).get(r).get(vId)
							.getRouteElements()) {
						e.setTempAlreadyAccepted(false);
					}
				}
			}
		}

	}

	/**
	 * Determines the acceptable per time window / remaining capacity per time
	 * window for the area with the hightest value (for normalisation)
	 * 
	 * @param maxAcceptablePerSubAreaAndTw
	 * @return
	 */
	private static HashMap<Integer, Integer> determineMaximumAcceptablePerTimeWindowOverSubareas(
			HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw) {
		HashMap<Integer, Integer> maximumPerTimeWindow = new HashMap<Integer, Integer>();

		for (Integer area : maxAcceptablePerSubAreaAndTw.keySet()) {
			for (Integer tw : maxAcceptablePerSubAreaAndTw.get(area).keySet()) {
				if (!maximumPerTimeWindow.containsKey(tw))
					maximumPerTimeWindow.put(tw, 0);
				if (maximumPerTimeWindow.get(tw) < maxAcceptablePerSubAreaAndTw.get(area).get(tw)) {
					maximumPerTimeWindow.put(tw, maxAcceptablePerSubAreaAndTw.get(area).get(tw));
				}
			}
		}

		return maximumPerTimeWindow;
	}

	private void updateReferenceRoutingInformation(HashMap<Routing, HashMap<Integer, Route>> referenceRoutings,
			RouteElement newElement, DeliveryArea area,
			HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> orienteeringRoutingCandidateResult) {
		referenceRoutings.clear();
		for (RouteElement e : orienteeringRoutingCandidateResult.get(newElement.getOrder().getTimeWindowFinal())
				.getValue()) {
			e.setTempAlreadyAccepted(true);
			referenceRoutings.put(e.getRoute().getRouting(),
					this.referenceRoutingsPerDeliveryArea.get(area).get(e.getRoute().getRouting()));
		}
	}

	private void updateOrienteeringNoAndCostInformation(DeliveryArea subArea, RouteElement newElement,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow) {
		for (int i = 0; i < aggregateInformationNo.size(); i++) {

			if (aggregateInformationNo.get(i).containsKey(subArea) && aggregateInformationNo.get(i).get(subArea)
					.containsKey(newElement.getOrder().getTimeWindowFinal())) {
				int currentCap = aggregateInformationNo.get(i).get(subArea)
						.get(newElement.getOrder().getTimeWindowFinal());
				if (currentCap > 0) {
					aggregateInformationNo.get(i).get(subArea).put(newElement.getOrder().getTimeWindowFinal(),
							currentCap - 1);
				} else {

					// Already accepted in this area? -> can steal
					// from neighbors
					if (this.theftBased && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.containsKey(newElement.getOrder().getTimeWindowFinal().getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.get(newElement.getOrder().getTimeWindowFinal().getId()) > 0) {
						// Check neighbor areas for capacity to
						// steal
						ArrayList<Pair<DeliveryArea, Double>> potentialStealingCandidates = new ArrayList<Pair<DeliveryArea, Double>>();
						HashMap<DeliveryArea, Integer> candidatesCapacities = new HashMap<DeliveryArea, Integer>();
						for (DeliveryArea nArea : this.neighbors.get(subArea)) {

							if (aggregateInformationNo.get(i).containsKey(nArea) && aggregateInformationNo.get(i)
									.get(nArea).containsKey(newElement.getOrder().getTimeWindowFinal())) {

								int currentCap2 = aggregateInformationNo.get(i).get(nArea)
										.get(newElement.getOrder().getTimeWindowFinal());
								if (currentCap2 > 0)

									potentialStealingCandidates.add(new Pair<DeliveryArea, Double>(nArea,
											(newElement.getOrder().getOrderRequest().getArrivalTime() - 1.0)
													* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
													* this.daWeightsLower.get(nArea)
													* this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow
															.get(nArea).get(newElement.getOrder().getTimeWindowFinal())
													/ (double) currentCap2));

								candidatesCapacities.put(nArea, currentCap2);
							}
						}

						// Steal from neighbor with highest capacity
						if (potentialStealingCandidates.size() > 0) {
							Collections.sort(potentialStealingCandidates, new PairDoubleValueAscComparator());
							aggregateInformationNo.get(i).get(potentialStealingCandidates.get(0).getKey()).put(
									newElement.getOrder().getTimeWindowFinal(),
									candidatesCapacities.get(potentialStealingCandidates.get(0).getKey()) - 1);

							stealingCounter++;
						} else {
							aggregateInformationNo.remove(i);
							if (considerOrienteeringCosts) {
								aggregateInformationCost.remove(i);
							}
							i--;
						}

					} else {
						aggregateInformationNo.remove(i);
						if (considerOrienteeringCosts) {
							aggregateInformationCost.remove(i);
						}
						i--;
					}
				}
			} else {
				aggregateInformationNo.remove(i);
				if (considerOrienteeringCosts) {
					aggregateInformationCost.remove(i);
				}
				i--;
			}
		}

	}

	private void determineOrienteeringRoutingCandidateResults(OrderRequest request, DeliveryArea area,
			HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> orienteeringRoutingCandidateResult,
			HashMap<TimeWindow, Double> averageDistanceOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			HashMap<Routing, HashMap<Integer, Route>> referenceRoutings,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost) {
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
				currentDemandRatio = Math.max(0,
						request.getArrivalTime() * this.demandMultiplierPerTimeWindow.get(area.getId()).get(tw)
								/ (averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
										- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId())));
			}

			double radiusValue = Math.max(MIN_RADIUS,
					this.maximumRadiusPerDeliveryArea.get(area.getId()) - currentDemandRatio / beginningDemandRatio
							* (this.maximumRadiusPerDeliveryArea.get(area.getId()) - MIN_RADIUS));
			radiusPerTimeWindow.put(tw.getId(), radiusValue);
		}

		// 2.) Check if accepted request that is close and within
		// one of the time windows
		averageDistanceOrienteering = this.determineAverageDistanceForAllPossibleTimeWindows(request,
				aggregateInformationCost, timeWindowCandidatesOrienteering);
		this.determineOrienteeringInsertionCostApproximation(orienteeringRoutingCandidateResult, request,
				referenceRoutings, timeWindowCandidatesOrienteering, radiusPerTimeWindow, aggregateInformationCost,
				averageDistanceOrienteering);
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

	private void checkFeasibilityBasedOnOrienteeringNo(OrderRequest request, int t, DeliveryArea area,
			DeliveryArea subArea, ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow) {
		// Possible time windows for request
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
				.getConsiderationSet();
		for (ConsiderationSetAlternative alt : alternatives) {
			if (!alt.getAlternative().getNoPurchaseAlternative())
				timeWindows.addAll(alt.getAlternative().getTimeWindows());
		}

		for (TimeWindow tw : timeWindows) {
			double currentDivisor;
			if ((averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
					- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId())) < 1) {
				currentDivisor = 1.0;
			} else {
				currentDivisor = (averageReferenceInformationNoPerDeliveryArea.get(area.getId()).get(tw)
						- this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw.getId()));
			}

			double currentDemandRatio = t * this.demandMultiplierPerTimeWindow.get(area.getId()).get(tw)
					/ currentDivisor;

			// TODO: What if 0?
			if (this.considerOrienteeringNo && currentDemandRatio > DEMAND_RATIO_SCARCITY_BORDER) {

				boolean feasible = false;
				if (avgAcceptablePerSubAreaAndTw.containsKey(subArea.getId())) {
					if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).containsKey(tw.getId())) {
						if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw.getId()) > 0) {
							timeWindowCandidatesOrienteering.add(tw);
							feasible = true;
						}
					}
				}

				if (!feasible && this.theftBased
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

					for (DeliveryArea nArea : this.neighbors.get(subArea)) {
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
	}

	private void determineOrienteeringInsertionCostApproximation(
			HashMap<TimeWindow, Pair<Double, ArrayList<RouteElement>>> approximation, OrderRequest request,
			HashMap<Routing, HashMap<Integer, Route>> referenceRoutings,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering, HashMap<Integer, Double> radiusPerTimeWindow,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> aggregateInformationCost,
			HashMap<TimeWindow, Double> averageDistance) {

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

	}


	private RouteElement updateValueFunctionIndividualWithOrderRequest(int t, DeliveryArea area, OrderRequest request,
			HashMap<Integer, RouteElement> feasibleTimeWindows, int requestSetId,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows,
			HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubareaAndTw,
			HashMap<Integer, Double> maximumDemandCapacityRatio,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo) {

		// Calculate value of same state in next time step
		// If at end of order horizon, the value is 0

		// Determine current demand capacity ratios for t-1

		HashMap<Integer, Double> currentDemandCapacityRatio = new HashMap<Integer, Double>();
		HashMap<Integer, Double> maximumDemandCapacityRatioDub = new HashMap<Integer, Double>();


		double maxAreaPotential = 0;
		for (Integer twId : maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).keySet()) {
			maxAreaPotential += maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).get(twId);
		}
		maxAreaPotential = maxAreaPotential * this.maximumAreaPotentialPerDeliveryArea.get(area.getId());

		double noAssignmentValue = 0;
		if (!this.areaSpecificValueFunction || (this.areaSpecificValueFunction && request == null)) {
			double penalty = 0;
			if (this.considerLeftOverPenalty && t - 1 == 0) {
				penalty = this.determineLeftOverPenalty(area, aggregateInformationNo, null, null);
			}
			noAssignmentValue = ValueFunctionApproximationService
					.evaluateStateForLinearValueFunctionApproximationWithOrienteering(t - 1,
							this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
							this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
							this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),
							acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()),
							this.areaPotentialPerDeliveryArea.get(area.getId()), currentDemandCapacityRatio,
							this.basicCoefficientPerDeliveryArea.get(area.getId()),
							this.timeCoefficientPerDeliveryArea.get(area.getId()),
							this.variableCoefficientsPerDeliveryArea.get(area.getId()),
							this.acceptedCostsCoefficientPerDeliveryArea.get(area.getId()),
							this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()),
							this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()),
							orderHorizonLength, maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
							this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
							this.overallCapacityPerDeliveryArea.get(area.getId()), maxAreaPotential,
							maximumDemandCapacityRatioDub,
							TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
							alternativesToTimeWindows.keySet().size(), this.considerOrienteeringRemainingCapacity,
							penalty);
		}

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> selectedOfferedAlternatives = new ArrayList<AlternativeOffer>();
		//ArrayList<Pair<ArrayList<AlternativeOffer>, Double>> offerSetValues = new ArrayList<Pair<ArrayList<AlternativeOffer>, Double>>();
		DeliveryArea subArea = null;

		if (request != null) {

			subArea = request.getCustomer().getTempDeliveryArea();
			if (this.areaSpecificValueFunction) {
				double penalty = 0;
				if (this.considerLeftOverPenalty && t - 1 == 0) {
					penalty = this.determineLeftOverPenaltyAreaSpecific(area, subArea, aggregateInformationNo,null);
				}
				noAssignmentValue = ValueFunctionApproximationService
						.evaluateStateForLinearValueFunctionApproximationWithOrienteeringForAreaSpecific(t - 1,
								this.daWeightsLower.get(subArea),
								this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()),
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()),
								maxAcceptablePerSubAreaAndTw.get(subArea.getId()),
								this.basicCoefficientPerDeliveryArea.get(area.getId()),
								this.timeCoefficientPerDeliveryArea.get(area.getId()),
								this.variableCoefficientsPerDeliveryArea.get(area.getId()), orderHorizonLength,
								this.maximumLowerAreaWeight,
								this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getId()),
								maxAcceptablePerTwOverSubAreas,
								TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
								alternativesToTimeWindows.keySet().size(), this.considerOrienteeringRemainingCapacity,
								penalty);
			}

			// For all feasible, assume you accept -> get value
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			for (Integer twId : feasibleTimeWindows.keySet()) {

				if (this.areaSpecificValueFunction) {
					this.determineTimeWindowValuesAreaSpecific(twValue, request, twId, area, subArea,
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, maxAcceptablePerSubAreaAndTw,
							maxAcceptablePerTwOverSubAreas, alternativesToTimeWindows, aggregateInformationNo);
				} else {
					this.determineTimeWindowValuesOverall(request, area, subArea, twValue, twId, feasibleTimeWindows,
							currentDemandCapacityRatio, maxAreaPotential, maximumDemandCapacityRatioDub,
							alternativesToTimeWindows, aggregateInformationNo);
				}

			}

			// Find best subset from the time windows with value add
			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue, maximumRevenueValue, objectiveSpecificValues, algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue, false, null, null,DISCOUNT_FACTOR);
			maxValue = bestOffer.getValue();
			selectedOfferedAlternatives = bestOffer.getKey();

		}

		// Update value function approximation
		double newValueApproximation = maxValue + noAssignmentValue;


		double currentStepSize = stepSize;
		if (this.annealingTemperature > 0) {
			currentStepSize = stepSize / (1.0 + (double) requestSetId / this.annealingTemperature);
		}



		if (!this.areaSpecificValueFunction || (this.areaSpecificValueFunction && request == null)) {
			ValueFunctionApproximationService.updateLinearValueFunctionApproximationWithOrienteering(area, t,
					alreadyAcceptedPerDeliveryArea, this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw,
					this.acceptedInsertionCostsPerDeliveryArea, this.acceptedInsertionCostsPerDeliveryAreaAndTw,
					this.areaPotentialPerDeliveryArea, currentDemandCapacityRatio, this.basicCoefficientPerDeliveryArea,
					this.timeCoefficientPerDeliveryArea, this.variableCoefficientsPerDeliveryArea,
					this.acceptedCostsCoefficientPerDeliveryArea, this.areaPotentialCoefficientPerDeliveryArea,
					this.timeCapacityInteractionCoefficientPerDeliveryArea, newValueApproximation, currentStepSize,
					orderHorizonLength, maximalAcceptablePerUpperDeliveryAreaAndTw,
					this.maximalAcceptablePerUpperDeliveryAreaAndTw, maximalAcceptableCostsPerDeliveryArea,
					maximumAreaPotentialPerDeliveryArea, maximumDemandCapacityRatioDub,
					this.LOG_LOSS_FUNCTION.get(area.getId()), this.LOG_WEIGHTS.get(area.getId()), requestSetId,
					TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
					alternativesToTimeWindows.keySet().size(), this.momentumWeight, this.oldMomentumPerDeliveryArea,
					this.considerOrienteeringRemainingCapacity, 0.0);
		} else {
			ValueFunctionApproximationService.updateLinearValueFunctionApproximationWithOrienteeringAreaSpecific(area,
					subArea, t, this.daWeightsLower.get(subArea),
					this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()),
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, maxAcceptablePerSubAreaAndTw,
					this.basicCoefficientPerDeliveryArea, this.timeCoefficientPerDeliveryArea,
					this.variableCoefficientsPerDeliveryArea, newValueApproximation, currentStepSize,
					orderHorizonLength, this.maximumLowerAreaWeight,
					this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getId()),
					maxAcceptablePerTwOverSubAreas, this.LOG_LOSS_FUNCTION.get(area.getId()),
					this.LOG_WEIGHTS.get(area.getId()), requestSetId,
					TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
					alternativesToTimeWindows.keySet().size(), this.momentumWeight, oldMomentumPerDeliveryArea,
					this.considerOrienteeringRemainingCapacity, 0.0);
		}

		// Simulate customer decision
		if (request != null) {
			/// Choose offer set
			if (feasibleTimeWindows.keySet().size() > 0) {

				// TA: Consider other exploration strategy that offers all
				// feasible according to orienteering
				if (this.explorationStrategy == 2) {

					// E-greedy search:also allows non-value-add offers
					ArrayList<Set<Integer>> possibleSets = new ArrayList<Set<Integer>>();
					possibleSets.addAll(SubsetProducer.powerSet(feasibleTimeWindows.keySet()));

					Double[] probabilities = new Double[possibleSets.size()];

					/// All but best offer set get same fraction of non-greedy
					/// probability;
					double currentGreedyValue = E_GREEDY_VALUE;
					// If we are later in the training phase, there should be
					// more greedy
					if (trainingSetNumberPerDeliveryArea.get(area.getId()) > 1.0 / 3.0
							* this.orderRequestSetsForLearning.size()) {
						if (trainingSetNumberPerDeliveryArea.get(area.getId()) > 2.0 / 3.0
								* this.orderRequestSetsForLearning.size()) {
							currentGreedyValue = 1.0;
						} else {
							currentGreedyValue = E_GREEDY_VALUE + (trainingSetNumberPerDeliveryArea.get(area.getId())
									- 1.0 / 3.0 * this.orderRequestSetsForLearning.size())
									/ (1.0 / 3.0 * this.orderRequestSetsForLearning.size()) * (1.0 - E_GREEDY_VALUE);
						}
					}
					double probForNotBest = (1.0 - currentGreedyValue) / (possibleSets.size() - 1.0);
					probabilities[0] = currentGreedyValue;

					for (int i = 1; i < probabilities.length; i++) {
						probabilities[i] = probForNotBest;
					}

					int selectedGroup = 0;

					try {
						selectedGroup = ProbabilityDistributionService
								.getRandomGroupIndexByProbabilityArray(probabilities);
					} catch (Exception e) {

						e.printStackTrace();
					}

					/// If the selected Offer set is the best offer set
					 if (selectedGroup != 0) {

						selectedGroup = selectedGroup - 1;

						/// Choose the selected subset. If it is equal to
						/// the best set or - in case the best set is the empty
						/// set-
						/// the empty set, than choose the last
						Set<Integer> selectedSet = possibleSets.get(selectedGroup);

						/// Equal?
						boolean equal =(selectedSet.size() ==selectedOfferedAlternatives.size());
						if (equal == true) {
							for (AlternativeOffer ao :selectedOfferedAlternatives) {
								if (!selectedSet.contains(ao.getAlternative().getTimeWindows().get(0).getId())) {
									equal = false;
									break;
								}
							}
						}
						if (equal == true)
							selectedSet = possibleSets.get(possibleSets.size() - 1);
						// Determine selection probabilities for the
						// alternatives of
						// the
						// respective subset
						selectedOfferedAlternatives.clear();
						for (Integer index : selectedSet) {
							AlternativeOffer offer = new AlternativeOffer();
							TimeWindow tw = this.timeWindowSet.getTimeWindowById(index);
							offer.setAlternative(alternativesToTimeWindows.get(tw));
							offer.setAlternativeId(alternativesToTimeWindows.get(tw).getId());
							selectedOfferedAlternatives.add(offer);
						}
					}

				}
			}

			/// If windows are offered, let customer choose
			if (selectedOfferedAlternatives.size() > 0) {
				// Sample selection from customer
				Order order = new Order();
				order.setOrderRequest(request);
				order.setOrderRequestId(request.getId());

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

					if (this.considerInsertionCostsOverall) {
						double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId());
						double additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
						this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(),
								additionalCosts + currentAcceptedInsertion);
					}

					if (this.considerInsertionCostsPerTW) {
						double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryAreaAndTw
								.get(area.getId()).get(twId);
						double additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
						this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(twId,
								additionalCosts + currentAcceptedInsertion);
					}

					if (this.considerAreaPotential) {
						double currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
						// double newAreaPotential =
						// this.valueMultiplierPerDeliveryAreaAndTimeWindow
						// .get(subArea.getId()).get(twId);
						double newAreaPotential = this.valueMultiplierPerLowerDeliveryArea.get(subArea.getId());
						this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);
					}

					return feasibleTimeWindows.get(twId);
				}

			}
		}
		return null;
	}

	private void determineTimeWindowValuesAreaSpecific(HashMap<TimeWindow, Double> twValue, OrderRequest request,
			int twId, DeliveryArea area, DeliveryArea subArea,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo) {
		TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
		int currentAccepted = alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(twId);
		alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(twId, currentAccepted + 1);

		int currentRemainingCapacity = maxAcceptablePerSubAreaAndTw.get(subArea.getId()).get(twId);
		maxAcceptablePerSubAreaAndTw.get(subArea.getId()).put(twId, currentRemainingCapacity - 1);

		double penaltyValue = 0.0;
		if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
			penaltyValue = this.determineLeftOverPenaltyAreaSpecific(area, subArea, aggregateInformationNo, tw);
		}
		double v = ValueFunctionApproximationService
				.evaluateStateForLinearValueFunctionApproximationWithOrienteeringForAreaSpecific(
						request.getArrivalTime() - 1, this.daWeightsLower.get(subArea),
						this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()),
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()),
						maxAcceptablePerSubAreaAndTw.get(subArea.getId()),
						this.basicCoefficientPerDeliveryArea.get(area.getId()),
						this.timeCoefficientPerDeliveryArea.get(area.getId()),
						this.variableCoefficientsPerDeliveryArea.get(area.getId()), orderHorizonLength,
						this.maximumLowerAreaWeight,
						this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getId()),
						maxAcceptablePerTwOverSubAreas,
						TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
						alternativesToTimeWindows.keySet().size(), this.considerOrienteeringRemainingCapacity,
						penaltyValue);

		alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(twId, currentAccepted);
		maxAcceptablePerSubAreaAndTw.get(subArea.getId()).put(twId, currentRemainingCapacity);

		twValue.put(tw, v);
	}

	private void determineTimeWindowValuesOverall(OrderRequest request, DeliveryArea area, DeliveryArea subArea,
			HashMap<TimeWindow, Double> twValue, int twId, HashMap<Integer, RouteElement> feasibleTimeWindows,
			HashMap<Integer, Double> currentDemandCapacityRatio, Double maxAreaPotential,
			HashMap<Integer, Double> maximumDemandCapacityRatioDub,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo) {
		TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);
		int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
		this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, currentAccepted + 1);

		int currentRemainingCapacity = this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId())
				.get(twId);
		this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).put(twId,
				currentRemainingCapacity - 1);

		double currentAcceptedInsertion = 0.0;
		if (this.considerInsertionCostsOverall) {
			currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId());
			double additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
			this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), additionalCosts + currentAcceptedInsertion);
		}

		double currentAcceptedInsertionTw = 0.0;
		if (this.considerInsertionCostsPerTW) {
			currentAcceptedInsertionTw = this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).get(twId);
			double additionalCosts = feasibleTimeWindows.get(twId).getTempAdditionalCostsValue();
			this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(twId,
					additionalCosts + currentAcceptedInsertionTw);
		}

		double currentAreaPotential = 0.0;
		if (this.considerAreaPotential) {
			currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
			// double newAreaPotential =
			// this.valueMultiplierPerDeliveryAreaAndTimeWindow
			// .get(subArea.getId()).get(twId);
			double newAreaPotential = this.valueMultiplierPerLowerDeliveryArea.get(subArea.getId());
			this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);
		}

		double currentDemandCapacityR = 0.0;
		// if (this.considerDemandCapacityRatio) {
		// currentDemandCapacityR =
		// currentDemandCapacityRatio.get(twId);
		// double divisor = ((double)
		// avgAcceptablePerSubareaAndTw.get(subArea.getId()).get(twId) -
		// 1.0);
		// // TODO: What if 0?
		// if (divisor == 0.0)
		// divisor = 1.0;
		// currentDemandCapacityRatio.put(twId,
		// currentDemandCapacityRatio.get(twId)
		// -
		// (this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()).get(twId)
		// * (t - 1) / (double)
		// avgAcceptablePerSubareaAndTw.get(subArea.getId()).get(twId))
		// +
		// this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(subArea.getId()).get(twId)
		// * (t - 1) / divisor);
		// }

		double penaltyValue = 0.0;
		if (this.considerLeftOverPenalty && request.getArrivalTime() - 1 == 0) {
			penaltyValue = this.determineLeftOverPenalty(area, aggregateInformationNo,subArea, tw );
		}
		double assignmentValue = ValueFunctionApproximationService
				.evaluateStateForLinearValueFunctionApproximationWithOrienteering(request.getArrivalTime() - 1,
						this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
						this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
						this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),
						acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()),

						this.areaPotentialPerDeliveryArea.get(area.getId()), currentDemandCapacityRatio,
						this.basicCoefficientPerDeliveryArea.get(area.getId()),
						this.timeCoefficientPerDeliveryArea.get(area.getId()),
						this.variableCoefficientsPerDeliveryArea.get(area.getId()),
						this.acceptedCostsCoefficientPerDeliveryArea.get(area.getId()),
						this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()),
						this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()), orderHorizonLength,
						maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
						this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()),
						maximalAcceptableCostsPerDeliveryArea.get(area.getId()), maxAreaPotential,
						maximumDemandCapacityRatioDub,
						TIME_MULTIPLIER * vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
						alternativesToTimeWindows.keySet().size(), this.considerOrienteeringRemainingCapacity,
						penaltyValue);

		twValue.put(tw, assignmentValue);

		this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, currentAccepted);
		this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).put(twId,
				currentRemainingCapacity);

		if (this.considerInsertionCostsPerTW) {
			this.acceptedInsertionCostsPerDeliveryAreaAndTw.get(area.getId()).put(twId, currentAcceptedInsertionTw);
		}

		if (this.considerInsertionCostsOverall) {
			this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), currentAcceptedInsertion);
		}

		if (this.considerAreaPotential) {
			this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential);
		}

		// if (this.considerDemandCapacityRatio) {
		// currentDemandCapacityRatio.put(twId, currentDemandCapacityR);
		// }
	}

	private void initialiseAlreadyAcceptedPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			acceptedPerTw.put(tw.getId(), 0);
		}
		this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);

	}

	private void initialiseAreaPotential(DeliveryArea area) {
		this.areaPotentialPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAcceptedCosts(DeliveryArea area) {
		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAcceptedCostsPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Double> acceptedPerTw = new HashMap<Integer, Double>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			acceptedPerTw.put(tw.getId(), 0.0);
		}
		this.acceptedInsertionCostsPerDeliveryAreaAndTw.put(area.getId(), acceptedPerTw);
	}

	private void initialiseGlobal() {

		this.LOG_LOSS_FUNCTION = new HashMap<Integer, HashMap<Integer, ArrayList<Double>>>();
		this.LOG_WEIGHTS = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
		this.trainingSetNumberPerDeliveryArea = new HashMap<Integer, Integer>();
		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);

		if (this.momentumWeight > 0)
			this.oldMomentumPerDeliveryArea = new HashMap<Integer, MomentumHelper>();
		this.bestRoutingCandidatePerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		this.targetRoutingResults = new ArrayList<Routing>();
		this.determineReferenceInformation();
		this.aggregateReferenceInformation();
		this.determineAverageAndMaximumAcceptablePerTimeWindow();
		this.chooseReferenceRoutings();
		this.determineDemandMultiplierPerTimeWindow();
		this.determineMaximumLowerAreaWeight();
		// Separate into delivery areas for parallel computing

		this.prepareOrderRequestsForDeliveryAreas();
		this.prepareVehicleAssignmentsForDeliveryAreas();

		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.acceptedInsertionCostsPerDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Double>>();
		this.acceptedInsertionCostsPerDeliveryArea = new HashMap<Integer, Double>();
		this.areaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();

		// if (this.considerAreaPotential)
		this.prepareValueMultiplier();
		if (this.areaSpecificValueFunction)
			this.determineValueDemandMultiplierLowerAreaPerTimeWindow();

		// Initialise basic and variable coefficients per delivery area
		this.initialiseCoefficients();

		if (this.theftBased) {
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
							this.daSegmentWeightingsLower, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
							daSegmentWeightingsLower, timeWindowSet);
		}
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

	}

	private void determineReferenceInformation() {
		for (Routing r : this.previousResults) {
			if (r.isPossiblyTarget()) {
				this.targetRoutingResults.add(r);
			}
		}
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

	private void initialiseCoefficients() {

		this.basicCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.timeCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.timeCapacityInteractionCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.variableCoefficientsPerDeliveryArea = new HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>>();
		this.areaPotentialCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedCostsCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.initialiseCoefficientsWithOrienteering();

	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerLowerDeliveryArea = new HashMap<Integer, Double>();
		this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
			this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.put(area.getId(),
					new HashMap<Integer, Double>());
		}

		for (DeliveryArea area : this.daSegmentWeightingsLower.keySet()) {
			double weightedValue = 0.0;
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLower.get(area).getWeights()) {
				double expectedValue = CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
						objectiveSpecificValues, segW.getDemandSegment());
				weightedValue += expectedValue * segW.getWeight();

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

	private void prepareOrderRequestsForDeliveryAreas() {

		// Initialise Hashmap
		this.orderRequestsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.orderRequestsPerDeliveryArea.put(area.getId(), new HashMap<Integer, HashMap<Integer, OrderRequest>>());
		}

		// Go through request sets
		for (int setId = 0; setId < this.orderRequestSetsForLearning.size(); setId++) {

			ArrayList<OrderRequest> requests = this.orderRequestSetsForLearning.get(setId).getElements();
			if (this.deliveryAreaSet.getElements().size() > 1) {
				for (int regId = 0; regId < requests.size(); regId++) {

					DeliveryArea assignedArea = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
							requests.get(regId).getCustomer());
					if (!this.orderRequestsPerDeliveryArea.get(assignedArea.getId()).containsKey(setId)) {
						HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
						assignedRequests.put(requests.get(regId).getArrivalTime(), requests.get(regId));
						this.orderRequestsPerDeliveryArea.get(assignedArea.getId()).put(setId, assignedRequests);
					} else {
						this.orderRequestsPerDeliveryArea.get(assignedArea.getId()).get(setId)
								.put(requests.get(regId).getArrivalTime(), requests.get(regId));
					}
				}
			} else {
				HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
				this.orderRequestsPerDeliveryArea.get(this.deliveryAreaSet.getElements().get(0).getId()).put(setId,
						assignedRequests);
				for (int regId = 0; regId < requests.size(); regId++) {
					this.orderRequestsPerDeliveryArea.get(this.deliveryAreaSet.getElements().get(0).getId()).get(setId)
							.put(requests.get(regId).getArrivalTime(), requests.get(regId));
				}

			}
		}

	}

	private void prepareVehicleAssignmentsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaSetAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();

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

	private static Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> avgPerRouting) {

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> divisorPerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Go through aggr. no per routing
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers : avgPerRouting) {
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
					// Only count this combination if capacity is >0 (otherwise,
					// not under feasible anymore after acceptance)
					if (separateNumbers.get(areaS).get(tw) > 0)
						divisorPerSAAndTw.get(areaS.getId()).put(tw.getId(),
								divisorPerSAAndTw.get(areaS.getId()).get(tw.getId()) + 1);
					if (separateNumbers.get(areaS).get(tw) > maxAcceptablePerSAAndTw.get(areaS.getId())
							.get(tw.getId())) {
						maxAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(), separateNumbers.get(areaS).get(tw));
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

	/**
	 * Prepare demand/ capacity ratio
	 */
	private void determineAverageAndMaximumAcceptablePerTimeWindow() {

		this.averageReferenceInformationNoPerDeliveryArea = new HashMap<Integer, HashMap<TimeWindow, Integer>>();
		// TA: Think about implementing maximal acceptable per time window
		// (separately)
		this.maximalAcceptablePerUpperDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.aggregatedReferenceInformationNoSumOverSubareas = new HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<TimeWindow, Integer> acceptable = new HashMap<TimeWindow, Integer>();
			HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();
			aggregatedReferenceInformationNoSumOverSubareas.put(area,
					new HashMap<Routing, HashMap<TimeWindow, Integer>>());
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

				aggregatedReferenceInformationNoSumOverSubareas.get(area).put(r, acceptedPerTw);
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
				acceptable.put(tw, (int) ((double) acceptable.get(tw)
						/ (double) this.aggregatedReferenceInformationNo.get(area).size()));
			}

			this.averageReferenceInformationNoPerDeliveryArea.put(area.getId(), acceptable);
			this.maximalAcceptablePerUpperDeliveryAreaAndTw.put(area.getId(), maximumAcceptable);

		}
		this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.currentStatusOfAverageReferenceInformationNoPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
	}

	private void determineCurrentStatusOfAverageAndMaximumAcceptablePerTimeWindow(DeliveryArea area,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo) {

		HashMap<Integer, Integer> acceptable = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();

		// Go through aggr. no per routing
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers : aggregateInformationNo) {
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

			// Update maximum acceptable per tw (find orienteering result with
			// highest number of acceptances per time window for this higher
			// area)
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
			acceptable.put(tw, (int) ((double) acceptable.get(tw) / (double) aggregateInformationNo.size()));
		}

		this.currentStatusMaximalAcceptablePerUpperDeliveryAreaAndTw.put(area.getId(), maximumAcceptable);
		this.currentStatusOfAverageReferenceInformationNoPerDeliveryArea.put(area.getId(), acceptable);

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

	private ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> copyAggregateNoInformation(
			DeliveryArea upperArea) {

		ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> infoList = new ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();

		// TODO Check: Copy of Integer
		for (Routing r : this.aggregatedReferenceInformationNo.get(upperArea).keySet()) {
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> info = this.aggregatedReferenceInformationNo
					.get(upperArea).get(r);
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> infoCopy = new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>();
			for (DeliveryArea area : info.keySet()) {
				infoCopy.put(area, new HashMap<TimeWindow, Integer>());
				for (TimeWindow tw : info.get(area).keySet()) {
					infoCopy.get(area).put(tw, info.get(area).get(tw));
				}
			}

			infoList.add(infoCopy);
		}

		return infoList;
	}

	private ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> copyAggregateCostsInformation(
			DeliveryArea upperArea) {

		ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> infoList = new ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>();

		// TODO Check: Copy of Double
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Double>> info : this.aggregatedReferenceInformationCosts
				.get(upperArea)) {
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> infoCopy = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();
			for (DeliveryArea area : info.keySet()) {
				infoCopy.put(area, new HashMap<TimeWindow, Double>());
				for (TimeWindow tw : info.get(area).keySet()) {
					infoCopy.get(area).put(tw, info.get(area).get(tw));
				}
			}

			infoList.add(infoCopy);
		}

		return infoList;
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
	 * Calculates penalty for left-over capacity in area- time window
	 * combinations (at the end of the order horizon)
	 * 
	 * @param area
	 * @return
	 */
	private double determineLeftOverPenalty(DeliveryArea area,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			DeliveryArea areaToConsiderReduction, TimeWindow timeWindowToConsiderReduction) {
		double maxPenalty = 0.0;

		// Find left over routing with highest penalty costs
		int countLeftOvers=0;
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> leftOvers : aggregateInformationNo) {
			// Go through all combinations of area and time window
			double penaltyRouting = 0.0;
			boolean consideredReduction=false;
			for (DeliveryArea subArea : leftOvers.keySet()) {
				for (TimeWindow tw : leftOvers.get(subArea).keySet()) {
					// Add to penalty, if capacity is left-over
					if (leftOvers.get(subArea).get(tw) > 0) {
						if (areaToConsiderReduction != null && leftOvers.get(subArea).get(tw) == 1
								&& areaToConsiderReduction.equals(subArea) && timeWindowToConsiderReduction.equals(tw)) {
							consideredReduction=true;
						} else {

							int numberLeftOver = leftOvers.get(subArea).get(tw);
							if (areaToConsiderReduction != null && areaToConsiderReduction.equals(subArea) && timeWindowToConsiderReduction.equals(tw)) {
								numberLeftOver = numberLeftOver - 1;
								consideredReduction=true;
							}
							countLeftOvers+=numberLeftOver;
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
			
			//If it was a steal from another area, we might not have considered the lost capacity
			if(areaToConsiderReduction != null && !consideredReduction){
				double minSegmentValue = Double.MAX_VALUE;
				for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(areaToConsiderReduction).getWeights()) {
					if (dsw.getWeight() > 0
							&& CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
									objectiveSpecificValues, dsw.getDemandSegment(),
									demandRatioRevenue) < minSegmentValue) {
						minSegmentValue = CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
								objectiveSpecificValues, dsw.getDemandSegment(), demandRatioRevenue);
					}
					;
				}
				penaltyRouting += minSegmentValue;
				countLeftOvers-=1;
			}

			if (penaltyRouting < maxPenalty)
				maxPenalty = penaltyRouting;
		}

		return maxPenalty;
	}

	private double determineLeftOverPenaltyAreaSpecific(DeliveryArea area, DeliveryArea subArea,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,TimeWindow timeWindowToConsiderReduction) {
		double maxPenalty = 0.0;

		// Find left over routing with highest penalty costs
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> leftOvers : aggregateInformationNo) {
			// Go through all time windows
			double penaltyRouting = 0.0;
			boolean consideredReduction=false;
			for (TimeWindow tw : leftOvers.get(subArea).keySet()) {
				// Add to penalty, if capacity is left-over
				if (leftOvers.get(subArea).get(tw) > 0) {
					if (timeWindowToConsiderReduction != null && leftOvers.get(subArea).get(tw) == 1 && timeWindowToConsiderReduction.equals(tw)) {
						consideredReduction=true;
					} else {

						int numberLeftOver = leftOvers.get(subArea).get(tw);
						if (timeWindowToConsiderReduction != null && timeWindowToConsiderReduction.equals(tw)) {
							numberLeftOver = numberLeftOver - 1;
							consideredReduction=true;
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
			
			//If it was a steal from another area, we might not have considered the lost capacity
			if(timeWindowToConsiderReduction != null && !consideredReduction){
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
				penaltyRouting += minSegmentValue;
			}

			if (penaltyRouting < maxPenalty)
				maxPenalty = penaltyRouting;
		}

		return maxPenalty;
	}

	private void initialiseCoefficientsWithOrienteering() {

		if (this.initialiseProblemSpecific) {
			// TODO: Consider if costs per tw and area potential

			HashMap<DeliveryArea, ArrayList<Double>> futureValuesPDa = new HashMap<DeliveryArea, ArrayList<Double>>();
			HashMap<DeliveryArea, ArrayList<Integer>> routingIdsPDa = new HashMap<DeliveryArea, ArrayList<Integer>>();
			HashMap<DeliveryArea, ArrayList<Integer>> timesPDa = new HashMap<DeliveryArea, ArrayList<Integer>>();
			HashMap<DeliveryArea, ArrayList<Double>> acceptedCostsPDa = new HashMap<DeliveryArea, ArrayList<Double>>();
			// TODO: Consider interaction with costs instead of number?
			HashMap<DeliveryArea, ArrayList<Integer>> interactionEffect = new HashMap<DeliveryArea, ArrayList<Integer>>();
			HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>> acceptedPerTimeWindowPDa = new HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>>();
			HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>> remainingPerTimeWindowPDa = new HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>>();
			HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>> remainingTimeInteractionPerTimeWindowPDa = new HashMap<DeliveryArea, HashMap<TimeWindow, ArrayList<Integer>>>();
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
				ArrayList<Double> futureValues = new ArrayList<Double>();
				futureValuesPDa.put(area, futureValues);
				ArrayList<Integer> routingIds = new ArrayList<Integer>();
				routingIdsPDa.put(area, routingIds);
				ArrayList<Integer> times = new ArrayList<Integer>();
				timesPDa.put(area, times);
				if (this.considerInsertionCostsOverall) {
					ArrayList<Double> acceptedCosts = new ArrayList<Double>();
					acceptedCostsPDa.put(area, acceptedCosts);
				}
				if (this.considerTimeCapacityInteraction) {
					ArrayList<Integer> currentInteractions = new ArrayList<Integer>();
					interactionEffect.put(area, currentInteractions);
				}

				HashMap<TimeWindow, ArrayList<Integer>> acceptedPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
				HashMap<TimeWindow, ArrayList<Integer>> remainingPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
				HashMap<TimeWindow, ArrayList<Integer>> remainingTimeInteractionPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
				for (TimeWindow tw : this.timeWindowSet.getElements()) {
					acceptedPerTimeWindow.put(tw, new ArrayList<Integer>());
					remainingPerTimeWindow.put(tw, new ArrayList<Integer>());
					remainingTimeInteractionPerTimeWindow.put(tw, new ArrayList<Integer>());
				}
				acceptedPerTimeWindowPDa.put(area, acceptedPerTimeWindow);
				remainingPerTimeWindowPDa.put(area, remainingPerTimeWindow);
				remainingTimeInteractionPerTimeWindowPDa.put(area, remainingTimeInteractionPerTimeWindow);
			}

			int rountingCounter = 0;
			for (Routing r : this.previousResults) {
				rountingCounter++;
				if ((r.isPossiblyTarget() && this.useTargetForInitialisation) || !r.isPossiblyTarget()) {
					HashMap<DeliveryArea, ArrayList<RouteElement>> ePerDeliveryArea = new HashMap<DeliveryArea, ArrayList<RouteElement>>();

					// Assign route elements to delivery areas
					for (Route rt : r.getRoutes()) {
						for (int i = 0; i < rt.getRouteElements().size(); i++) {

							// Do not consider depots that were added to the
							// reference routings
							if ((i != 0 && i != rt.getRouteElements().size() - 1)
									|| !this.referenceRoutingsList.contains(r)) {
								RouteElement e = rt.getRouteElements().get(i);
								e.setTravelTimeTo(e.getTravelTime());
								double travelTimeFrom;
								if (i != rt.getRouteElements().size() - 1) {
									travelTimeFrom = rt.getRouteElements().get(i + 1).getTravelTime();
								} else {
									travelTimeFrom = LocationService
											.calculateHaversineDistanceBetweenGPSPointsInKilometer(
													e.getOrder().getOrderRequest().getCustomer().getLat(),
													e.getOrder().getOrderRequest().getCustomer().getLon(),
													rt.getVehicleAssignment().getEndingLocationLat(),
													rt.getVehicleAssignment().getEndingLocationLon());
								}
								e.setTravelTimeFrom(travelTimeFrom);
								DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
										e.getOrder().getOrderRequest().getCustomer());
								if (!ePerDeliveryArea.containsKey(area))
									ePerDeliveryArea.put(area, new ArrayList<RouteElement>());
								ePerDeliveryArea.get(area).add(e);
								if (e.getServiceTime() == null) {
									System.out.println("Strange");
								}
							}
						}
					}

					for (DeliveryArea area : ePerDeliveryArea.keySet()) {
						// Determine feed-forward details
						Collections.sort(ePerDeliveryArea.get(area), new RouteElementArrivalTimeDescComparator());
						double acceptedCosts = 0.0;
						int acceptedNoOverall = 0;

						for (TimeWindow tw : this.timeWindowSet.getElements()) {
							int newNumberForTimeWindow = 0;
							acceptedPerTimeWindowPDa.get(area).get(tw).add(newNumberForTimeWindow);
							remainingPerTimeWindowPDa.get(area).get(tw)
									.add(this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r).get(tw)
											- newNumberForTimeWindow);
							remainingTimeInteractionPerTimeWindowPDa.get(area).get(tw)
									.add((this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r).get(tw)
											- newNumberForTimeWindow) * this.orderHorizonLength);
						}

						// 1.) First at beginning of order horizon
						int lastT;
						if (ePerDeliveryArea.get(area).get(0).getOrder().getOrderRequest()
								.getArrivalTime() < this.orderHorizonLength) {
							timesPDa.get(area).add(this.orderHorizonLength);
							routingIdsPDa.get(area).add(rountingCounter);
							if (this.considerInsertionCostsOverall) {
								acceptedCostsPDa.get(area).add(acceptedCosts);
							}
							if (this.considerTimeCapacityInteraction) {
								interactionEffect.get(area).add(acceptedNoOverall * 0);
							}
							lastT = this.orderHorizonLength;
							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								int newNumberForTimeWindow = 0;
								acceptedPerTimeWindowPDa.get(area).get(tw).add(newNumberForTimeWindow);
								remainingPerTimeWindowPDa.get(area).get(tw).add(
										this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r).get(tw)
												- newNumberForTimeWindow);
								remainingTimeInteractionPerTimeWindowPDa.get(area).get(tw).add(
										(this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r).get(tw)
												- newNumberForTimeWindow) * this.orderHorizonLength);
							}
						} else {
							lastT = this.orderHorizonLength + 1;
						}

						// 2.) Actual elements
						for (int i = 0; i < ePerDeliveryArea.get(area).size(); i++) {

							RouteElement e = ePerDeliveryArea.get(area).get(i);
							for (int a = lastT - 1; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
								timesPDa.get(area).add(a);
								routingIdsPDa.get(area).add(rountingCounter);
							}

							if (this.considerInsertionCostsOverall) {
								for (int a = lastT - 1; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
									acceptedCostsPDa.get(area).add(acceptedCosts);
								}
								acceptedCosts = acceptedCosts + (e.getTravelTimeTo() + e.getTravelTimeFrom()) / 2.0;

							}

							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								int newNumberForTimeWindow = acceptedPerTimeWindowPDa.get(area).get(tw)
										.get(acceptedPerTimeWindowPDa.get(area).get(tw).size() - 1);

								for (int a = lastT - 2; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
									acceptedPerTimeWindowPDa.get(area).get(tw).add(newNumberForTimeWindow);
									remainingPerTimeWindowPDa.get(area).get(tw)
											.add(this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r)
													.get(tw) - newNumberForTimeWindow);
									remainingTimeInteractionPerTimeWindowPDa.get(area).get(tw)
											.add((this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r)
													.get(tw) - newNumberForTimeWindow) * a);
								}

								if (tw.equals(e.getOrder().getTimeWindowFinal())) {
									newNumberForTimeWindow++;
								}
								acceptedPerTimeWindowPDa.get(area).get(tw).add(newNumberForTimeWindow);
								remainingPerTimeWindowPDa.get(area).get(tw).add(
										this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r).get(tw)
												- newNumberForTimeWindow);
								remainingTimeInteractionPerTimeWindowPDa.get(area).get(tw)
										.add((this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r)
												.get(tw) - newNumberForTimeWindow)
												* (e.getOrder().getOrderRequest().getArrivalTime() - 1));
							}

							if (this.considerTimeCapacityInteraction) {
								for (int a = lastT - 1; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
									interactionEffect.get(area).add(acceptedNoOverall * (this.orderHorizonLength - a));
								}
								acceptedNoOverall = acceptedNoOverall + 1;

							}

							lastT = e.getOrder().getOrderRequest().getArrivalTime();
						}

						if (lastT > 1) {
							for (int a = lastT - 1; a > 0; a--) {
								timesPDa.get(area).add(a);
								routingIdsPDa.get(area).add(rountingCounter);
							}

							if (this.considerInsertionCostsOverall) {
								for (int a = lastT - 1; a > 0; a--) {
									acceptedCostsPDa.get(area).add(acceptedCosts);
								}
							}
							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								int newNumberForTimeWindow = acceptedPerTimeWindowPDa.get(area).get(tw)
										.get(acceptedPerTimeWindowPDa.get(area).get(tw).size() - 1);

								for (int a = lastT - 2; a >= 0; a--) {
									acceptedPerTimeWindowPDa.get(area).get(tw).add(newNumberForTimeWindow);
									remainingPerTimeWindowPDa.get(area).get(tw)
											.add(this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r)
													.get(tw) - newNumberForTimeWindow);
									remainingTimeInteractionPerTimeWindowPDa.get(area).get(tw)
											.add((this.aggregatedReferenceInformationNoSumOverSubareas.get(area).get(r)
													.get(tw) - newNumberForTimeWindow) * a);
								}
							}

							if (this.considerTimeCapacityInteraction) {
								for (int a = lastT - 1; a > 0; a--) {
									interactionEffect.get(area).add(acceptedNoOverall * (this.orderHorizonLength - a));
								}

							}
						}

						// Add for t=0: (not needed to tws because they are
						// placed in advance)
						timesPDa.get(area).add(0);
						routingIdsPDa.get(area).add(rountingCounter);
						if (this.considerInsertionCostsOverall) {
							acceptedCostsPDa.get(area).add(acceptedCosts);
						}
						if (this.considerTimeCapacityInteraction) {

							interactionEffect.get(area).add(acceptedNoOverall * this.orderHorizonLength);

						}

						// Determine backward details
						Collections.sort(ePerDeliveryArea.get(area), new RouteElementArrivalTimeAscComparator());

						// TA: Think if value is something else
						ArrayList<Double> accumulatedValues = new ArrayList<Double>();
						double value = 0.0;
						int currentT = 0;
						for (int a = currentT; a < ePerDeliveryArea.get(area).get(0).getOrder().getOrderRequest()
								.getArrivalTime(); a++) {
							accumulatedValues.add(value);
						}
						currentT = ePerDeliveryArea.get(area).get(0).getOrder().getOrderRequest().getArrivalTime();

						int targetT = ePerDeliveryArea.get(area).get(1).getOrder().getOrderRequest().getArrivalTime()
								- 1;

						for (int reId = 0; reId < ePerDeliveryArea.get(area).size(); reId++) {
							RouteElement e = ePerDeliveryArea.get(area).get(reId);
							// if(e.getOrder().getTimeWindowFinalId()==40 &&
							// e.getOrder().getOrderRequest().getArrivalTime()>200){
							// System.out.println("I add a customer for tw 40");
							// }
							// value = value +
							// (0.9+0.1*e.getOrder().getOrderRequest().getArrivalTime()/this.orderHorizonLength)*CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
							// objectiveSpecificValues,
							// e.getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment());
							value = value + CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
									objectiveSpecificValues,
									e.getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment(),
									demandRatioRevenue);

							while (currentT <= targetT) {
								accumulatedValues.add(value);
								currentT++;
							}
							if (reId < ePerDeliveryArea.get(area).size() - 1) {
								targetT = ePerDeliveryArea.get(area).get(reId + 1).getOrder().getOrderRequest()
										.getArrivalTime() - 1;
							} else {
								targetT = this.orderHorizonLength;
							}

						}

						// Fill up till end of order horizon
						for (int t = currentT; t <= targetT; t++) {
							accumulatedValues.add(value);
						}

						Collections.sort(accumulatedValues, Collections.reverseOrder());
						futureValuesPDa.get(area).addAll(accumulatedValues);

						// Add extreme of T=0 and no accepted
						// futureValuesPDa.get(area).add(0.0);
						// routingIdsPDa.get(area).add(rountingCounter);
						// timesPDa.get(area).add(0);
						//
						// for (int a = 0; a <
						// this.timeWindowSet.getElements().size(); a++) {
						// acceptedPerTimeWindowPDa.get(area).get(this.timeWindowSet.getElements().get(a)).add(0);
						//
						// }
						//
						// if (this.considerInsertionCostsOverall)
						// acceptedCostsPDa.get(area).add(0.0);
						//
						// if (this.considerTimeCapacityInteraction)
						// interactionEffect.get(area).add(0);
					}
				}

			}

			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

				// Prepare normalised data
				int maximalAcceptableOverall = 0;
				for (Integer twId : this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId()).keySet()) {
					maximalAcceptableOverall += this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId())
							.get(twId);
				}

				// ArrayList<Double> normalisedTimes = new ArrayList<Double>();
				// ArrayList<Double> normalisedCosts = new ArrayList<Double>();
				// ArrayList<Double> normalisedInteractionTimeCap = new
				// ArrayList<Double>();
				// HashMap<TimeWindow, ArrayList<Double>> normalisedTwNumbers =
				// new HashMap<TimeWindow, ArrayList<Double>>();
				// for(TimeWindow tw: this.timeWindowSet.getElements()){
				// ArrayList<Double> twNumbers = new ArrayList<Double>();
				// normalisedTwNumbers.put(tw, twNumbers);
				// }
				// for(int i= 0; i < futureValuesPDa.get(area).size(); i++){
				// normalisedTimes.add((double) timesPDa.get(area).get(i) /
				// (double) this.orderHorizonLength);
				// if(considerInsertionCostsOverall)
				// normalisedCosts.add(acceptedCostsPDa.get(area).get(i)
				// /
				// this.maximalAcceptableCostsPerDeliveryArea.get(area.getId()));
				// if(considerTimeCapacityInteraction){
				// normalisedInteractionTimeCap.add((double)
				// interactionEffect.get(area).get(i)
				// / ((double) maximalAcceptableOverall * (double)
				// this.orderHorizonLength));
				// }
				//
				// for (TimeWindow tw: this.timeWindowSet.getElements()) {
				// normalisedTwNumbers.get(tw).add((double)
				// acceptedPerTimeWindowPDa.get(area)
				// .get(tw).get(i)
				// / (double)
				// this.maximalAcceptablePerDeliveryAreaAndTw.get(area.getId())
				// .get(tw.getId()));
				// }
				// }
				//
				//
				//
				// //Estimate model
				//
				// RConnection connection = null;
				//
				// try {
				// /* Create a connection to Rserve instance running
				// * on default port 6311
				// */
				// connection = new RConnection();
				//
				// String routingIds =
				// this.toRVectorStringInteger(routingIdsPDa.get(area));
				// connection.eval("routingValues="+routingIds);
				// String times = this.toRVectorStringDouble(normalisedTimes);
				// connection.eval("times="+times);
				// connection.eval("inputData = cbind(routingValues, times)");
				// String futureValues =
				// this.toRVectorStringDouble(futureValuesPDa.get(area));
				// connection.eval("futureValues="+futureValues);
				// connection.eval("inputData = cbind(inputData,
				// futureValues)");
				// if(considerInsertionCostsOverall){
				// String costs = this.toRVectorStringDouble(normalisedCosts);
				// connection.eval("costs="+costs);
				// connection.eval("inputData = cbind(inputData, costs)");
				// }
				//
				// if(considerTimeCapacityInteraction){
				// String interaction =
				// this.toRVectorStringDouble(normalisedInteractionTimeCap);
				// connection.eval("interaction="+interaction);
				// connection.eval("inputData = cbind(inputData, interaction)");
				// }
				//
				//
				// for(TimeWindow tw: this.timeWindowSet.getElements()){
				// String twNameString="tw"+tw.getId();
				// String twDataString =
				// this.toRVectorStringDouble(normalisedTwNumbers.get(tw));
				// connection.eval(twNameString+"="+twDataString);
				// connection.eval("inputData =
				// cbind(inputData,"+twNameString+")");
				// }
				//
				//
				// connection.eval("library(plm)");
				// connection.eval("inputData=data.frame(inputData)");
				//
				// String toEvalutePlm = "result = plm(futureValues~times";
				// if(considerInsertionCostsOverall)
				// toEvalutePlm=toEvalutePlm+"+costs";
				// if(considerTimeCapacityInteraction)
				// toEvalutePlm=toEvalutePlm+"+interaction";
				// for(TimeWindow tw: this.timeWindowSet.getElements()){
				// toEvalutePlm=toEvalutePlm+"+tw"+tw.getId();
				// }
				// connection.eval(toEvalutePlm+", data = inputData, model =
				// \"pooling\")");
				// System.out.println(connection.eval("result$coefficients[1]").asDouble());
				// double test =
				// connection.eval("result$coefficients[1]").asDouble();
				// this.basicCoefficientPerDeliveryArea.put(area.getId(),
				// connection.eval("result$coefficients[1]").asDouble());
				// Double timeCoeff =
				// connection.eval("result$coefficients[\"times\"]").asDouble();
				// if(timeCoeff.isNaN()) timeCoeff=0.0;
				// this.timeCoefficientPerDeliveryArea.put(area.getId(),
				// timeCoeff);
				//
				// ArrayList<ValueFunctionApproximationCoefficient> coefficients
				// = new ArrayList<ValueFunctionApproximationCoefficient>();
				//
				// for (int twID = 0; twID <
				// this.timeWindowSet.getElements().size(); twID++) {
				// ValueFunctionApproximationCoefficient coeff = new
				// ValueFunctionApproximationCoefficient();
				// coeff.setDeliveryAreaId(area.getId());
				// coeff.setDeliveryArea(area);
				// coeff.setTimeWindow(this.timeWindowSet.getElements().get(twID));
				// coeff.setTimeWindowId(this.timeWindowSet.getElements().get(twID).getId());
				// coeff.setSquared(false);
				// coeff.setType(ValueFunctionCoefficientType.NUMBER);
				// // TODO: What about costs per tw?
				// coeff.setCoefficient(connection.eval("result$coefficients[\"tw"+this.timeWindowSet.getElements().get(twID).getId()+"\"]").asDouble());
				// coefficients.add(coeff);
				// if (this.considerDemandCapacityRatio) {
				// ValueFunctionApproximationCoefficient coeff2 = new
				// ValueFunctionApproximationCoefficient();
				// coeff2.setDeliveryAreaId(area.getId());
				// coeff2.setDeliveryArea(area);
				// coeff2.setTimeWindow(this.timeWindowSet.getElements().get(twID));
				// coeff2.setTimeWindowId(this.timeWindowSet.getElements().get(twID).getId());
				// coeff2.setSquared(false);
				// coeff.setType(ValueFunctionCoefficientType.RATIO);
				// coeff2.setCoefficient(0.0);
				//
				// coefficients.add(coeff2);
				// }
				// }
				//
				// if (this.considerInsertionCostsOverall) {
				// this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(),
				// connection.eval("result$coefficients[\"costs\"]").asDouble());
				// } else {
				// this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(),
				// null);
				// }
				//
				// if (this.considerTimeCapacityInteraction) {
				// this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(),
				// connection.eval("result$coefficients[\"interaction\"]").asDouble());
				// } else {
				// this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(),
				// null);
				// }
				// this.variableCoefficientsPerDeliveryArea.put(area.getId(),
				// coefficients);
				//
				// } catch (RserveException e) {
				// e.printStackTrace();
				// } catch (REXPMismatchException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }finally{
				// connection.close();
				// }

				OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
				double[] vs = new double[futureValuesPDa.get(area).size()];
				int numberOfVariables = this.timeWindowSet.getElements().size() + 1;
				if (this.considerInsertionCostsOverall)
					numberOfVariables++;
				if (this.considerTimeCapacityInteraction)
					numberOfVariables++;
				if (this.considerOrienteeringRemainingCapacityTimeInteraction)
					numberOfVariables = numberOfVariables + this.timeWindowSet.getElements().size();
				double[][] x = new double[futureValuesPDa.get(area).size()][];
				int currentV = 0;
				for (int i = 0; i < futureValuesPDa.get(area).size(); i++) {
					x[i] = new double[numberOfVariables];
				}

				// Fill up the rows
				for (int i = 0; i < futureValuesPDa.get(area).size(); i++) {
					vs[i] = futureValuesPDa.get(area).get(i);

					// Fill up the columns per row
					x[i][0] = (double) timesPDa.get(area).get(i) / (double) this.orderHorizonLength;
					int currentColumn = 1;
					for (int a = 1; a <= this.timeWindowSet.getElements().size(); a++) {
						if (this.considerOrienteeringRemainingCapacity) {
							x[i][a] = (double) remainingPerTimeWindowPDa.get(area)
									.get(this.timeWindowSet.getElements().get(a - 1)).get(i)
									/ (double) this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId())
											.get(this.timeWindowSet.getElements().get(a - 1).getId());
						} else {
							x[i][a] = (double) acceptedPerTimeWindowPDa.get(area)
									.get(this.timeWindowSet.getElements().get(a - 1)).get(i)
									/ (double) this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId())
											.get(this.timeWindowSet.getElements().get(a - 1).getId());
						}
						currentColumn++;
					}

					if (this.considerOrienteeringRemainingCapacityTimeInteraction) {
						for (int a = this.timeWindowSet.getElements().size() + 1; a <= this.timeWindowSet.getElements()
								.size() * 2; a++) {

							x[i][a] = (double) remainingTimeInteractionPerTimeWindowPDa.get(area)
									.get(this.timeWindowSet.getElements()
											.get(a - this.timeWindowSet.getElements().size() - 1))
									.get(i)
									/ ((double) this.maximalAcceptablePerUpperDeliveryAreaAndTw.get(area.getId())
											.get(this.timeWindowSet.getElements()
													.get(a - this.timeWindowSet.getElements().size() - 1).getId())
											* this.orderHorizonLength);
							currentColumn++;
						}
					}
					if (this.considerInsertionCostsOverall) {
						x[i][currentColumn] = acceptedCostsPDa.get(area).get(i)
								/ this.maximalAcceptableCostsPerDeliveryArea.get(area.getId());
						currentColumn++;
					}

					if (this.considerTimeCapacityInteraction) {

						x[i][currentColumn] = (double) interactionEffect.get(area).get(i)
								/ ((double) maximalAcceptableOverall * (double) this.orderHorizonLength);
					}
				}
				;

				regression.newSampleData(vs, x);

				double[] beta = regression.estimateRegressionParameters();
				double rSquared = regression.calculateRSquared();

				this.basicCoefficientPerDeliveryArea.put(area.getId(), beta[0]);
				this.timeCoefficientPerDeliveryArea.put(area.getId(), beta[1]);
				int currentColumn = 2;

				ArrayList<ValueFunctionApproximationCoefficient> coefficients = new ArrayList<ValueFunctionApproximationCoefficient>();

				for (int twID = 0; twID < this.timeWindowSet.getElements().size(); twID++) {
					ValueFunctionApproximationCoefficient coeff = new ValueFunctionApproximationCoefficient();
					coeff.setDeliveryAreaId(area.getId());
					coeff.setDeliveryArea(area);
					coeff.setTimeWindow(this.timeWindowSet.getElements().get(twID));
					coeff.setTimeWindowId(this.timeWindowSet.getElements().get(twID).getId());
					coeff.setSquared(false);
					if (this.considerOrienteeringRemainingCapacity) {
						coeff.setType(ValueFunctionCoefficientType.REMAINING_CAPACITY);
					} else {
						coeff.setType(ValueFunctionCoefficientType.NUMBER);
					}

					// TODO: What about costs per tw?
					coeff.setCoefficient(beta[twID + 2]);
					coefficients.add(coeff);
					if (this.considerDemandCapacityRatio) {
						ValueFunctionApproximationCoefficient coeff2 = new ValueFunctionApproximationCoefficient();
						coeff2.setDeliveryAreaId(area.getId());
						coeff2.setDeliveryArea(area);
						coeff2.setTimeWindow(this.timeWindowSet.getElements().get(twID));
						coeff2.setTimeWindowId(this.timeWindowSet.getElements().get(twID).getId());
						coeff2.setSquared(false);
						coeff.setType(ValueFunctionCoefficientType.RATIO);
						coeff2.setCoefficient(0.0);

						coefficients.add(coeff2);
					}
					currentColumn++;
				}
				if (this.considerOrienteeringRemainingCapacityTimeInteraction) {
					for (int twID = 0; twID < this.timeWindowSet.getElements().size(); twID++) {
						ValueFunctionApproximationCoefficient coeff = new ValueFunctionApproximationCoefficient();
						coeff.setDeliveryAreaId(area.getId());
						coeff.setDeliveryArea(area);
						coeff.setTimeWindow(this.timeWindowSet.getElements().get(twID));
						coeff.setTimeWindowId(this.timeWindowSet.getElements().get(twID).getId());
						coeff.setSquared(false);

						coeff.setType(ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME);

						// TODO: What about costs per tw?
						coeff.setCoefficient(beta[currentColumn]);
						coefficients.add(coeff);
						currentColumn++;
					}
				}
				if (this.considerInsertionCostsOverall) {
					this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(), beta[currentColumn]);
					currentColumn++;
				} else {
					this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(), null);
				}

				if (this.considerTimeCapacityInteraction) {
					this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), beta[currentColumn]);
				} else {
					this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(), null);
				}
				this.variableCoefficientsPerDeliveryArea.put(area.getId(), coefficients);
			}

		} else {
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

				this.basicCoefficientPerDeliveryArea.put(area.getId(), 0.0);
				this.timeCoefficientPerDeliveryArea.put(area.getId(), 0.0);
				if (this.considerTimeCapacityInteraction) {
					this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), 0.0);
				} else {
					this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), null);
				}

				if (this.considerInsertionCostsOverall) {
					this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(), 0.0);
				} else {
					this.acceptedCostsCoefficientPerDeliveryArea.put(area.getId(), null);
				}

				ArrayList<ValueFunctionApproximationCoefficient> coefficients = new ArrayList<ValueFunctionApproximationCoefficient>();

				for (TimeWindow tw : this.timeWindowSet.getElements()) {
					ValueFunctionApproximationCoefficient coeff = new ValueFunctionApproximationCoefficient();
					coeff.setDeliveryAreaId(area.getId());
					coeff.setDeliveryArea(area);
					coeff.setTimeWindow(tw);
					coeff.setTimeWindowId(tw.getId());
					coeff.setSquared(false);

					if (this.considerInsertionCostsPerTW) {
						coeff.setType(ValueFunctionCoefficientType.COST);
					} else {

						if (this.considerOrienteeringRemainingCapacity) {
							coeff.setType(ValueFunctionCoefficientType.REMAINING_CAPACITY);
						} else {
							coeff.setType(ValueFunctionCoefficientType.NUMBER);
						}
					}

					coeff.setCoefficient(0.0);

					coefficients.add(coeff);

					if (this.considerDemandCapacityRatio) {
						ValueFunctionApproximationCoefficient coeff2 = new ValueFunctionApproximationCoefficient();
						coeff2.setDeliveryAreaId(area.getId());
						coeff2.setDeliveryArea(area);
						coeff2.setTimeWindow(tw);
						coeff2.setTimeWindowId(tw.getId());
						coeff2.setSquared(false);
						coeff2.setType(ValueFunctionCoefficientType.RATIO);
						coeff2.setCoefficient(0.0);

						coefficients.add(coeff2);
					}

					if (this.considerOrienteeringRemainingCapacityTimeInteraction) {
						ValueFunctionApproximationCoefficient coeff3 = new ValueFunctionApproximationCoefficient();
						coeff3.setDeliveryAreaId(area.getId());
						coeff3.setDeliveryArea(area);
						coeff3.setTimeWindow(tw);
						coeff3.setTimeWindowId(tw.getId());
						coeff3.setSquared(false);
						coeff3.setType(ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME);
						coeff3.setCoefficient(0.0);

						coefficients.add(coeff3);
					}

				}

				this.variableCoefficientsPerDeliveryArea.put(area.getId(), coefficients);
			}

		}
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	private String toRVectorStringDouble(ArrayList<Double> valueList) {

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

	private String toRVectorStringInteger(ArrayList<Integer> valueList) {
		// String listString = "c(";
		// for (int i=0; i < valueList.size()-1; i++)
		// {
		// listString += valueList.get(i)+ ",";
		// }
		// listString += valueList.get(valueList.size()-1)+ ")";
		//
		// return listString;

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

	public ValueFunctionApproximationModelSet getResult() {

		return modelSet;
	}

}
