package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.ArtificialNeuralNetwork;
import logic.entity.ArtificialNeuralNetworkRegion;
import logic.entity.AssortmentAlgorithm;
import logic.entity.MomentumHelper;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LearningService;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.utility.comparator.DemandSegmentsExpectedValueAscComparator;
import logic.utility.comparator.DemandSegmentsExpectedValueDescComparator;
import logic.utility.comparator.OrderArrivalTimeAscComparator;
import logic.utility.comparator.OrderArrivalTimeDescComparator;
import logic.utility.comparator.RouteElementArrivalTimeAscComparator;
import logic.utility.comparator.RouteElementArrivalTimeDescComparator;

/**
 * 
 * 
 * @author M. Lang
 *
 */
public class ADPWithSoftOrienteeringANN implements ValueFunctionApproximationAlgorithm {

	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static double AMOUNT_PER_ITERATION = 1.0;
	private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> LOG_LOSS_FUNCTION;
	private HashMap<Integer, HashMap<Integer, ArrayList<String>>> LOG_WEIGHTS;
	private static int NUMBER_THREADS = 1;
	private static double demandRatioRevenue = 0.05;
	private static boolean usePlainEstimator = false;

	private static double E_GREEDY_VALUE = 0.5;
	private static double E_GREEDY_VALUE_MAX = 0.95;

	private static boolean possiblyLargeOfferSet = true;
	private static double TIME_MULTIPLIER = 60.0;
	private TimeWindowSet timeWindowSet;
	private AlternativeSet alternativeSet;
	private ValueFunctionApproximationModelSet modelSet;
	private ArrayList<Routing> targetRoutingResults;
	private ArrayList<Routing> previousRoutingResults;
	private ArrayList<OrderSet> previousOrderSetResults;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> benchmarkingOrderRequestsPerDeliveryArea;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private double arrivalProbability;

	private ArrayList<DemandSegment> demandSegments;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>> referenceRoutingsPerDeliveryArea;
	private ArrayList<Routing> referenceRoutingsList;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> aggregatedReferenceInformationNoSumOverSubareas;
	private HashMap<DeliveryArea, ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>> aggregatedReferenceInformationCosts;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting;
	private HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea;
	private HashMap<Integer, ArtificialNeuralNetworkRegion> ANNProblemSpecificPerDeliveryArea;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private HashMap<Integer, Integer> segmentInputMapper;
	private int[][] indicesDemandSegments;
	private int[][] indicesTimeWindows;
	private HashMap<Integer, Integer> timeWindowInputMapper;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	// Per upper area: what is the average accepted number per tw -> basis for
	// demand/capacity ratio
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;

	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerUpperDeliveryAreaAndTw;

	private HashMap<Integer, HashMap<TimeWindow, Double>> demandMultiplierPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> demandMultiplierLowerAreaPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> valueMultiplierPerLowerDeliveryArea;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;

	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<DeliveryArea, Double> daWeightsUpper;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper;
	private HashMap<DeliveryArea, Double> daWeightsLower;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower;
	private HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLowerHash;
	private HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsUpperHash;
	private double maximumLowerAreaWeight;
	private int arrivalProcessId;
	private double momentumWeight;
	private HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;

	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;
	// private boolean initialiseProblemSpecific;

	private int orderHorizonLength;
	private Region region;
	private boolean useActualBasketValue;
	private Double annealingTemperature;
	private double stepSize;
	private Double explorationStrategy;
	private boolean usepreferencesSampled;
	private int numberRoutingCandidates;

	private HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;

	private boolean areaSpecificValueFunction;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;

	private HashMap<Integer, Integer> trainingSetNumberPerDeliveryArea;
	private Double discountingFactor;
	private DemandSegmentWeighting demandSegmentWeighting;
	private double maximumValue;
	private double minimumValue;
	private Double discountingFactorProbability;
	private HashMap<Integer, HashMap<Integer, Double>> maximumOpportunityCostsPerLowerDaAndTimeWindow;
	private HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues;
	private ArrayList<OrderRequestSet> benchmarkingOrderRequestSets;
	private Integer additionalHiddenNodes;
	private boolean considerConstant;
	private boolean hTanActivation;
	private Double expectedServiceTime;
	private boolean includeDriveFromStartingPosition;
	private int noRoutingCandidates;
	private int noInsertionCandidates;
	private int distanceType;
	private boolean considerRemainingBudget;
	private boolean distanceInAdp;
	private boolean considerRemainingTimeInsteadOfDemandSegments;

	private static String[] paras = new String[] { "stepsize_adp_learning", "actualBasketValue", "samplePreferences",
			"annealing_temperature_(Negative:no_annealing)",
			"exploration_(0:on-policy,1:conservative-factor,2:e-greedy)", "momentum_weight", "discounting_factor",
			"discounting_factor_probability", "consider_constant", "additional_hidden_nodes", "hTan_activation",
			"Constant_service_time", "includeDriveFromStartingPosition", "no_routing_candidates",
			"no_insertion_candidates", "distance_type", "consider_remaining_budget", "consider_remaining_time",
			"distance_adp" };

	public ADPWithSoftOrienteeringANN(Region region, DemandSegmentWeighting demandSegmentWeighting,
			VehicleAreaAssignmentSet vehicleAreaAssignmentSet, ArrayList<Routing> previousRoutingResults,
			ArrayList<OrderSet> previousOrderSetResults, DeliveryAreaSet deliveryAreaSet,
			HashMap<DeliveryArea, Double> daWeightsUpperAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpperAreas,
			HashMap<DeliveryArea, Double> daWeightsLowerAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas,
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValueDouble, Double actualBasketValue, int orderHorizonLength, double stepSize,
			Double annealingTemperature, Double explorationStrategy, Double samplePreferences, Double momentumWeight,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, int arrivalProcessId, Double discountingFactor,
			Double discountingFactorProbability, HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues,
			ArrayList<OrderRequestSet> benchmarkingOrderRequestSets, Double additionalHiddenNodes,
			Double considerConstant, Double hTanActivation, Double expectedServiceTime,
			Double includeDriveFromStartingPosition, Double noRoutingCandidates, Double noInsertionCandidates,
			Double distanceType, Double considerRemainingBudget, Double considerRemainingTime, Double distanceInAdp) {

		this.region = region;
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.previousRoutingResults = previousRoutingResults;
		this.previousOrderSetResults = previousOrderSetResults;
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
		this.includeDriveFromStartingPosition = (includeDriveFromStartingPosition == 1.0);
		this.noRoutingCandidates = noRoutingCandidates.intValue();
		this.noInsertionCandidates = noInsertionCandidates.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.annealingTemperature = annealingTemperature;
		this.stepSize = stepSize;
		this.explorationStrategy = explorationStrategy;
		this.distanceInAdp = (distanceInAdp == 1.0);
		this.momentumWeight = momentumWeight;
		this.neighbors = neighbors;
		this.arrivalProcessId = arrivalProcessId;
		this.distanceType = distanceType.intValue();
		this.discountingFactorProbability = discountingFactorProbability;
		this.discountingFactor = discountingFactor;
		this.benchmarkValues = benchmarkValues;
		this.benchmarkingOrderRequestSets = benchmarkingOrderRequestSets;

		this.considerRemainingBudget = (considerRemainingBudget == 1.0);
		this.considerRemainingTimeInsteadOfDemandSegments = (considerRemainingTime == 1.0);
		if (considerConstant == 1.0) {
			this.considerConstant = true;
		} else {
			this.considerConstant = false;
		}
		this.additionalHiddenNodes = additionalHiddenNodes.intValue();

		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}

		if (actualBasketValue == 1.0) {
			this.useActualBasketValue = true;
		} else {
			this.useActualBasketValue = false;
		}

		if (hTanActivation == 1.0) {
			this.hTanActivation = true;
		} else {
			this.hTanActivation = false;
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

			this.applyADPForDeliveryArea(area, this.ANNProblemSpecificPerDeliveryArea.get(area.getId()));

			ValueFunctionApproximationModel model = new ValueFunctionApproximationModel();
			model.setDeliveryAreaId(area.getId());
			model.setObjectiveFunctionValueLog(this.LOG_LOSS_FUNCTION.get(area.getId()));
			model.setWeightsLog(this.LOG_WEIGHTS.get(area.getId()));

			this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
					.setWeights(this.ANNPerDeliveryArea.get(area.getId()).getMatrix());
			this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
					.setThresholds(this.ANNPerDeliveryArea.get(area.getId()).getThresholds());

			ObjectMapper mapper = new ObjectMapper();
			String annModelString = null;
			try {
				annModelString = mapper.writeValueAsString(this.ANNProblemSpecificPerDeliveryArea.get(area.getId()));
			} catch (JsonProcessingException e) {

				e.printStackTrace();
			}

			// Reverse: ArtificialNeuralNetwork obj =
			// mapper.readValue(jsonInString, ArtificialNeuralNetwork.class);
	
			model.setComplexModelJSON(annModelString);
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

	private void applyADPForDeliveryArea(DeliveryArea area, ArtificialNeuralNetworkRegion annProblemSpecific) {

		HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo = this
				.copyAggregateNoInformation(area, false);

		Iterator<Integer> it = this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().iterator();

		// Initial training phase
		int currentIndex = 0;
		System.out.println("Experiencing now. First training round");

		int firstNumberOfIterations = (int) (this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().size() * 1.0);
		int currentOrderRequestIndex = 0;

		// Reset ann error
		this.ANNPerDeliveryArea.get(area.getId()).getError(1);
		while (it.hasNext() && currentIndex < firstNumberOfIterations) {
			System.out.println("Iteration: " + currentIndex);

			Pair<ArrayList<double[]>, ArrayList<Order>> acceptedOrders = this.simulateArrivalProcess(area,
					aggregateInformationNo, this.orderRequestsPerDeliveryArea.get(area.getId()).get(it.next()),
					discountingFactorProbability, this.explorationStrategy, firstNumberOfIterations, currentIndex,
					E_GREEDY_VALUE_MAX, E_GREEDY_VALUE, annProblemSpecific);

			double currentStepSize = this.stepSize;
			if (this.annealingTemperature > 0) {
				currentStepSize = stepSize / (1.0 + (double) currentOrderRequestIndex / this.annealingTemperature);
			}
			this.ANNPerDeliveryArea.get(area.getId()).setLearnRate(currentStepSize);
			this.updateANN(area, acceptedOrders, false, false);

			aggregateInformationNo = this.copyAggregateNoInformation(area, false);

			currentIndex++;
			currentOrderRequestIndex++;

		}

		// TODO: Reconsider determination of error
		double lastError = this.ANNPerDeliveryArea.get(area.getId())
				.getError(this.orderHorizonLength * firstNumberOfIterations * area.getSubset().getElements().size());
		double oldError = lastError;

		// Check and train iteratively
		Long numberPerIteration = Math.round(
				(this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().size() - firstNumberOfIterations) * 1.0);
		Long numberOfIterations = Math
				.round((this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().size() - firstNumberOfIterations)
						/ (double) numberPerIteration);
		if (numberOfIterations < (this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().size()
				- firstNumberOfIterations) / (double) numberPerIteration) {
			numberOfIterations++;
		}

		Double lastValue = null;
		for (int iterationNo = 0; iterationNo <= numberOfIterations; iterationNo++) {

			// Check if stopping possible
			// 1.) Determine average performance for validation order request
			// sets
			double averageAccepted = 0.0;
			double averageValueAccepted = 0.0;
			Iterator<Integer> itOS = this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).keySet()
					.iterator();
			while (itOS.hasNext()) {

				ArrayList<Order> acceptedOrders = this.simulateArrivalProcess(area, aggregateInformationNo,

						this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).get(itOS.next()),

						discountingFactorProbability, 1.0, null, null, null, null, annProblemSpecific).getValue();

				averageAccepted += acceptedOrders.size();
				for (Order o : acceptedOrders) {

					averageValueAccepted += CustomerDemandService.calculateOrderValue(o.getOrderRequest(),
							maximumRevenueValue, objectiveSpecificValues);

				}

				if (itOS.hasNext()) {
					aggregateInformationNo = this.copyAggregateNoInformation(area, false);
				} else {
					aggregateInformationNo = this.copyAggregateNoInformation(area, true);
				}

			}
			System.out.println("Average accepted after stealing: " + averageAccepted
					/ (double) this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).keySet().size());
			double averageValue = averageValueAccepted
					/ (double) this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).keySet().size();
			System.out.println("Average value after stealing: " + averageValue);

			// 2.) Compare to benchmark results
			boolean better = true;
			for (Double d : this.benchmarkValues.get(area)) {
				if (d * 1.1 > averageValue) {
					better = false;
					break;
				}
			}

			// 3.) Check if low learning for the last round and the current
			// value not better.Stop if 2. and 3. positive
			if (iterationNo == numberOfIterations) {
				break;
			} else if (iterationNo > 0 && lastError >= oldError * 1.2 && better && !(lastValue * 1.2 < averageValue)) {
				break;
			}

			lastValue = averageValue;

			// 4.) Otherwise, go on with another learning round
			currentIndex = 0;
			System.out.println("Experiencing now. Training round: " + iterationNo);
			while (it.hasNext() && currentIndex < numberPerIteration) {
				System.out.println("Iteration: " + currentIndex);

				Pair<ArrayList<double[]>, ArrayList<Order>> acceptedOrders = this.simulateArrivalProcess(area,
						aggregateInformationNo,

						this.orderRequestsPerDeliveryArea.get(area.getId()).get(it.next()),
						discountingFactorProbability, this.explorationStrategy, numberPerIteration.intValue(),
						currentIndex, 1.0, E_GREEDY_VALUE_MAX, annProblemSpecific);

				this.updateANN(area, acceptedOrders, false, false);

				if (it.hasNext() && currentIndex < numberPerIteration - 1) {
					aggregateInformationNo = this.copyAggregateNoInformation(area, true);
				} else {
					aggregateInformationNo = this.copyAggregateNoInformation(area, false);
				}

				currentIndex++;
				currentOrderRequestIndex++;

				double currentStepSize = this.stepSize;
				if (this.annealingTemperature > 0) {
					currentStepSize = stepSize / (1.0 + (double) currentOrderRequestIndex / this.annealingTemperature);
				}
				this.ANNPerDeliveryArea.get(area.getId()).setLearnRate(currentStepSize);
			}
			oldError = lastError;
			lastError = this.ANNPerDeliveryArea.get(area.getId()).getError(
					this.orderHorizonLength * numberPerIteration.intValue() * area.getSubset().getElements().size());

			// this.updateANN(area, acceptedOrdersPerORS,
			// maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
			// this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
			// numberPerIteration.intValue());
		}

	}

	private Pair<ArrayList<double[]>, ArrayList<Order>> simulateArrivalProcess(DeliveryArea area,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<Integer, OrderRequest> requests, double discountingFactorProbability,
			double currentExplorationStrategy, Integer numberOfIterations, Integer currentIteration,
			Double highestGreedyValue, Double lowestGreedyValue, ArtificialNeuralNetworkRegion annProblemSpecific) {

		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrders = new HashMap<DeliveryArea, ArrayList<Order>>();
		ArrayList<Order> acceptedOrdersOverall = new ArrayList<Order>();
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow = new HashMap<Integer, Integer>();
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			alreadyAcceptedPerTimeWindow.put(tw.getId(), 0);
		}
		ArrayList<double[]> observations = new ArrayList<double[]>();

		HashMap<Integer, Double> distancePerRouting = new HashMap<Integer, Double>();
		for (Routing r : aggregateInformationNo.keySet()) {
			distancePerRouting.put(r.getId(), 0.0);
		}

		Double currentAcceptedTravelTime = null;
		HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = null;

		Routing closestReferenceRouting = null;
		Double currentDistanceMeasure = 0.0;
		// Go through requests
		for (int t = this.orderHorizonLength; t > 0; t--) {

			if (requests.containsKey(t)) {
				OrderRequest request = requests.get(t);
				DeliveryArea subArea = LocationService
						.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet, request.getCustomer());
				request.getCustomer().setTempDeliveryArea(subArea);

				// Possible time windows for request
				ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
				ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
						.getConsiderationSet();
				for (ConsiderationSetAlternative alt : alternatives) {
					if (!alt.getAlternative().getNoPurchaseAlternative())
						timeWindows.addAll(alt.getAlternative().getTimeWindows());
				}

				// Dynamic feasibility check

				ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
				HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();

				currentAcceptedTravelTime = DynamicRoutingHelperService
						.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
								request, region, TIME_MULTIPLIER, this.timeWindowSet, includeDriveFromStartingPosition,
								this.expectedServiceTime, possibleRoutings, this.noRoutingCandidates,
								this.noInsertionCandidates, vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()),
								acceptedOrdersOverall, bestRoutingSoFar, currentAcceptedTravelTime, timeWindows,
								bestRoutingsValueAfterInsertion, NUMBER_THREADS);

				ArrayList<TimeWindow> feasibleTimeWindows = new ArrayList<TimeWindow>();
				for (TimeWindow tw : timeWindows) {
					if (bestRoutingsValueAfterInsertion.containsKey(tw.getId()))
						feasibleTimeWindows.add(tw);
				}

				// Calculate distances and distance measures for potential
				// acceptances
				// (per time window)
				HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
				HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow = new HashMap<Integer, Routing>();
				HashMap<Integer, Double> distanceMeasurePerTimeWindow = ADPWithSoftOrienteeringANN
						.calculateResultingDistancePerTimeWindowAndRouting(subArea, neighbors.get(subArea),null,
								feasibleTimeWindows, aggregateInformationNo,null,
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, distancePerRouting, null,
								newDistancePerTimeWindowAndRouting, routingSmallestDistancePerTimeWindow,
								this.distanceType, false);

				// If the distance is not considered in adp, it is used as
				// additional feasibility decision
				ArrayList<TimeWindow> feasibleTimeWindowsOverall;

				if (this.distanceInAdp) {

					feasibleTimeWindowsOverall = feasibleTimeWindows;

				} else {
					feasibleTimeWindowsOverall = new ArrayList<TimeWindow>();
					for (TimeWindow tw : feasibleTimeWindows) {
						if (distanceMeasurePerTimeWindow.get(tw.getId()) - currentDistanceMeasure < 3.0) {
							feasibleTimeWindowsOverall.add(tw);
						}

					}
				}

				// Determine opportunity costs

				HashMap<TimeWindow, Double> opportunityCostsPerTw = new HashMap<TimeWindow, Double>();

				// Update opportunity costs calculation
				ADPWithSoftOrienteeringANN.determineOpportunityCostsPerTw(feasibleTimeWindowsOverall,
						this.ANNPerDeliveryArea.get(area.getId()), t, this.arrivalProbability, annProblemSpecific,
						this.segmentInputMapper, this.timeWindowInputMapper, currentDistanceMeasure,
						distanceMeasurePerTimeWindow, currentAcceptedTravelTime, this.expectedServiceTime,
						acceptedOrdersOverall.size(), bestRoutingsValueAfterInsertion, alreadyAcceptedPerTimeWindow,
						usePlainEstimator, this.daSegmentWeightingsUpperHash.get(area), opportunityCostsPerTw);

				// Find best subset from the time windows with value add
				Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
						request, opportunityCostsPerTw, maximumRevenueValue, objectiveSpecificValues, algo,
						alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue, 1.0);

				for (TimeWindow tw : opportunityCostsPerTw.keySet()) {
					if (opportunityCostsPerTw.get(tw) < 0) {
						opportunityCostsPerTw.put(tw, 0.0);
						// System.out.println("neg. opp. costs");
					}
				}
				// Simulate customer choice
				ArrayList<AlternativeOffer> selectedOfferedAlternatives = bestOffer.getKey();

				if (opportunityCostsPerTw.keySet().size() > 0) {
					if (currentExplorationStrategy == 2.0) {
						// E-Greedy
						LearningService.chooseOfferSetBasedOnEGreedyStrategy(opportunityCostsPerTw.keySet(),
								lowestGreedyValue, highestGreedyValue, currentIteration, numberOfIterations, 0.1, 0.9,
								selectedOfferedAlternatives, alternativesToTimeWindows);

					}

				}

				double[] observation = new double[annProblemSpecific.getMaxValuePerElement().length];
				double expectedArrivals = arrivalProbability * (t - 1);
				if (annProblemSpecific.getConstant() > -1)
					observation[annProblemSpecific.getConstant()] = 1.0;
				if (annProblemSpecific.getRemainingTime() > -1)
					observation[annProblemSpecific.getRemainingTime()] = (t - 1)
							/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingTime()];
				if (this.ANNProblemSpecificPerDeliveryArea.get(area.getId()).getRemainingTime() > -1) {
					observation[this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
							.getRemainingTime()] = expectedArrivals
									/ this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
											.getMaxValuePerElement()[this.ANNProblemSpecificPerDeliveryArea
													.get(area.getId()).getRemainingTime()];
				} else {
					for (DemandSegment s : this.daSegmentWeightingsUpperHash.get(area).keySet()) {
						observation[segmentInputMapper.get(s.getId())] = expectedArrivals
								* this.daSegmentWeightingsUpperHash.get(area).get(s)
								/ annProblemSpecific.getMaxValuePerElement()[segmentInputMapper.get(s.getId())];
					}
				}

				Order order = new Order();
				order.setAccepted(false);
				/// If windows are offered, let customer choose
				if (selectedOfferedAlternatives.size() > 0) {
					// Sample selection from customer

					order.setOrderRequest(request);
					order.setOrderRequestId(request.getId());

					AlternativeOffer selectedAlt;
					if (usepreferencesSampled) {
						selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
								selectedOfferedAlternatives, order, this.alternativeSet.getNoPurchaseAlternative());

					} else {
						selectedAlt = CustomerDemandService.sampleCustomerDemand(selectedOfferedAlternatives, order);
					}

					if (selectedAlt != null && selectedAlt.getAlternative() != null
							&& !selectedAlt.getAlternative().getNoPurchaseAlternative()) {
						order.setAccepted(true);
						order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
						order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
						if (!acceptedOrders.containsKey(subArea)) {
							acceptedOrders.put(subArea, new ArrayList<Order>());
						}
						acceptedOrders.get(subArea).add(order);
						acceptedOrdersOverall.add(order);

						// Update distances
						distancePerRouting = newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId());
						closestReferenceRouting = routingSmallestDistancePerTimeWindow
								.get(order.getTimeWindowFinalId());
						currentDistanceMeasure = distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId());

						// Update routes

						int routingId = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId()).getKey()
								.getTempRoutingId();
						DynamicRoutingHelperService.insertRouteElement(
								bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId()).getKey(),
								possibleRoutings.get(routingId), vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()),
								timeWindowSet, TIME_MULTIPLIER, includeDriveFromStartingPosition);
						bestRoutingSoFar = possibleRoutings.get(routingId);
						currentAcceptedTravelTime = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
								.getValue();

						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
									new HashMap<Integer, Integer>());
						}
						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.containsKey(order.getTimeWindowFinalId())) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.put(order.getTimeWindowFinalId(), 0);
						}
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.put(order.getTimeWindowFinalId(), alreadyAcceptedPerSubDeliveryAreaAndTimeWindow
										.get(subArea.getId()).get(order.getTimeWindowFinalId()) + 1);
						alreadyAcceptedPerTimeWindow.put(order.getTimeWindowFinalId(),
								alreadyAcceptedPerTimeWindow.get(order.getTimeWindowFinalId()) + 1);

						// fill observation
						if (annProblemSpecific.getDistanceMeasure() > -1)
							observation[annProblemSpecific.getDistanceMeasure()] = currentDistanceMeasure
									/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific
											.getDistanceMeasure()];
						if (annProblemSpecific.getRemainingBudget() > -1)
							observation[annProblemSpecific.getRemainingBudget()] = (annProblemSpecific
									.getMaxValuePerElement()[annProblemSpecific.getRemainingBudget()]
									- currentAcceptedTravelTime
									- this.expectedServiceTime * acceptedOrdersOverall.size())
									/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific
											.getRemainingBudget()];
						for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {
							observation[timeWindowInputMapper.get(tw)] = alreadyAcceptedPerTimeWindow.get(tw)
									/ annProblemSpecific.getMaxValuePerElement()[timeWindowInputMapper.get(tw)];

						}

					}
				}

				if (!order.getAccepted()) {

					if (observations.size() > 0) {
						double[] lastObservation = observations.get(observations.size() - 1);
						if (annProblemSpecific.getDistanceMeasure() > -1)
							observation[annProblemSpecific.getDistanceMeasure()] = lastObservation[annProblemSpecific
									.getDistanceMeasure()];
						if (annProblemSpecific.getRemainingBudget() > -1)
							observation[annProblemSpecific.getRemainingBudget()] = lastObservation[annProblemSpecific
									.getRemainingBudget()];
						for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {
							observation[timeWindowInputMapper.get(tw)] = lastObservation[timeWindowInputMapper.get(tw)];

						}
					} else {
						if (annProblemSpecific.getDistanceMeasure() > -1)
							observation[annProblemSpecific.getDistanceMeasure()] = 0.0 / annProblemSpecific
									.getMaxValuePerElement()[annProblemSpecific.getDistanceMeasure()];
						if (annProblemSpecific.getRemainingBudget() > -1)
							observation[annProblemSpecific.getRemainingBudget()] = 1.0;

						for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {
							int accepted = alreadyAcceptedPerTimeWindow.get(tw);
							observation[timeWindowInputMapper.get(tw)] = accepted
									/ annProblemSpecific.getMaxValuePerElement()[timeWindowInputMapper.get(tw)];

						}
					}

				}
				observations.add(observation);
			} else {
				double[] observation = new double[annProblemSpecific.getMaxValuePerElement().length];
				double expectedArrivals = arrivalProbability * (t - 1);
				if (t == orderHorizonLength) {
					if (annProblemSpecific.getConstant() > -1)
						observation[annProblemSpecific.getConstant()] = 1.0;
					if (annProblemSpecific.getDistanceMeasure() > -1)
						observation[annProblemSpecific.getDistanceMeasure()] = 0.0
								/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getDistanceMeasure()];
					if (annProblemSpecific.getRemainingBudget() > -1)
						observation[annProblemSpecific.getRemainingBudget()] = 1.0;
					if (annProblemSpecific.getRemainingTime() > -1) {
						observation[annProblemSpecific.getRemainingTime()] = expectedArrivals
								/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingTime()];
					} else {
						for (DemandSegment s : this.daSegmentWeightingsUpperHash.get(area).keySet()) {
							observation[segmentInputMapper.get(s.getId())] = expectedArrivals
									* this.daSegmentWeightingsUpperHash.get(area).get(s)
									/ annProblemSpecific.getMaxValuePerElement()[segmentInputMapper.get(s.getId())];
						}
					}

					for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {
						int accepted = alreadyAcceptedPerTimeWindow.get(tw);
						observation[timeWindowInputMapper.get(tw)] = accepted
								/ annProblemSpecific.getMaxValuePerElement()[timeWindowInputMapper.get(tw)];

					}

				} else {
					double[] lastObservation = observations.get(observations.size() - 1);
					if (annProblemSpecific.getConstant() > -1)
						observation[annProblemSpecific.getConstant()] = lastObservation[annProblemSpecific
								.getConstant()];
					if (annProblemSpecific.getDistanceMeasure() > -1)
						observation[annProblemSpecific.getDistanceMeasure()] = lastObservation[annProblemSpecific
								.getDistanceMeasure()];
					if (annProblemSpecific.getRemainingBudget() > -1)
						observation[annProblemSpecific.getRemainingBudget()] = lastObservation[annProblemSpecific
								.getRemainingBudget()];
					if (annProblemSpecific.getRemainingTime() > -1) {
						observation[annProblemSpecific.getRemainingTime()] = expectedArrivals
								/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingTime()];
					} else {
						for (DemandSegment s : this.daSegmentWeightingsUpperHash.get(area).keySet()) {
							observation[segmentInputMapper.get(s.getId())] = expectedArrivals
									* this.daSegmentWeightingsUpperHash.get(area).get(s)
									/ annProblemSpecific.getMaxValuePerElement()[segmentInputMapper.get(s.getId())];
						}
					}

					for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {

						observation[timeWindowInputMapper.get(tw)] = lastObservation[timeWindowInputMapper.get(tw)];

					}
				}
				observations.add(observation);
			}

		}

		return new Pair<ArrayList<double[]>, ArrayList<Order>>(observations, acceptedOrdersOverall);

	}

	public static HashMap<Integer, Double> calculateResultingDistancePerTimeWindowAndRouting(DeliveryArea subArea,
			ArrayList<DeliveryArea> neighbors, HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTW, ArrayList<TimeWindow> timeWindows,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> anyAccepted,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Integer, Double> distancePerRouting,
			HashMap<Integer, HashMap<Integer, Double>> distancePerTwAndRouting,
			HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting,
			HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow, int distanceType,
			boolean distanceMeasurePerTw) {

		HashMap<Integer, Double> distanceMeasurePerTimeWindow = new HashMap<Integer, Double>();

		for (TimeWindow tw : timeWindows) {

			newDistancePerTimeWindowAndRouting.put(tw.getId(), new HashMap<Integer, Double>());

			double smallestDistance = Double.MAX_VALUE;
			int newDistanceAsIs = 0;
			if (distanceType == 3) {
				// Calculate distance from actually already accepted -> relevant
				// for all routings
				
				if (!(alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0)) {

					// Neighbor area with already accepted?
					for (DeliveryArea neighbor : neighbors) {
						if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(neighbor.getId())
								&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(neighbor.getId())
										.containsKey(tw.getId())
								&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(neighbor.getId())
										.get(tw.getId()) > 0) {
							newDistanceAsIs += 1;
							break;
						}

					}

					// Neighbor tw with already accepted?
					if (newDistanceAsIs == 0) {
						for (TimeWindow neighbor : neighborsTW.get(tw)) {
							if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.containsKey(neighbor.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.get(neighbor.getId()) > 0) {
								newDistanceAsIs += 1;
								break;
							}

						}
					}
					
					if (newDistanceAsIs == 0) {
						newDistanceAsIs+=2;
					}
				}

			}

			for (Routing r : aggregateInformationNo.keySet()) {

				double distance;
				if (distanceMeasurePerTw) {
					distance = distancePerTwAndRouting.get(tw.getId()).get(r.getId());
				} else {
					distance = distancePerRouting.get(r.getId());
				}


				// Already accepted?
				if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

					if (distanceType == 1) {
						// If yes, cost of one if no capacity left for this
						// combination
						if (!(aggregateInformationNo.get(r).containsKey(subArea)
								&& aggregateInformationNo.get(r).get(subArea).containsKey(tw)
								&& (aggregateInformationNo.get(r).get(subArea).get(tw)
										- alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
												.get(tw.getId())
										- 1 >= 0))) {
							distance += 1.0;
						}
					}
				} else {

					// If no and no capacity left, the additional distance
					// depends on the capacity in the neighborhood
					if (!(aggregateInformationNo.get(r).containsKey(subArea)
							&& aggregateInformationNo.get(r).get(subArea).containsKey(tw)
							&& aggregateInformationNo.get(r).get(subArea).get(tw) > 0)) {

						// Check neighbors for capacity (not accepted, because
						// overall distance would depend on the order of the
						// arriving customers)
						boolean relevantNeighbor = false;
						for (DeliveryArea neighbor : neighbors) {
							if (aggregateInformationNo.get(r).containsKey(neighbor)
									&& aggregateInformationNo.get(r).get(neighbor).containsKey(tw)
									&& aggregateInformationNo.get(r).get(neighbor).get(tw) > 0) {
								relevantNeighbor = true;
								distance += 2.0;
								break;
							}
						}
						
						if(!relevantNeighbor && distanceType==3 && !distanceMeasurePerTw){
							for(TimeWindow neighbor:neighborsTW.get(tw)){
								if (aggregateInformationNo.get(r).containsKey(subArea)
										&& aggregateInformationNo.get(r).get(subArea).containsKey(neighbor)
										&& aggregateInformationNo.get(r).get(subArea).get(neighbor) > 0) {
									relevantNeighbor = true;
									distance += 2.0;
									break;
								}
							}
						}

						if (!relevantNeighbor && distanceType==3){
							distance += 3.0;
						}else if(!relevantNeighbor){
							distance += 3.0;
						}
							
					}
				}
//				}
				newDistancePerTimeWindowAndRouting.get(tw.getId()).put(r.getId(), distance+newDistanceAsIs);
				if (distance+newDistanceAsIs < smallestDistance) {
					smallestDistance = distance+newDistanceAsIs;
					routingSmallestDistancePerTimeWindow.put(tw.getId(), r);
					distanceMeasurePerTimeWindow.put(tw.getId(), distance+newDistanceAsIs);
				}
			}

		}

		// TODO-N: Consider other distance types and measures

		return distanceMeasurePerTimeWindow;

	}

	public static void determineOpportunityCostsPerTw(ArrayList<TimeWindow> timeWindowCandidates, NeuralNetwork ann,
			int t, double arrivalProbability, ArtificialNeuralNetworkRegion annProblemSpecific,
			HashMap<Integer, Integer> segmentInputMapper, HashMap<Integer, Integer> timeWindowInputMapper,
			double currentDistanceMeasure, HashMap<Integer, Double> distanceMeasurePerTimeWindow,
			double currentAcceptedTravelTime, double serviceTime, int alreadyAcceptedOverall,
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion,
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow, boolean usePlainEstimator,
			HashMap<DemandSegment, Double> demandSegmentWeightings, HashMap<TimeWindow, Double> opportunityCostsPerTw) {

		// No assignment value
		double currentBudget = 0.0;
		if (annProblemSpecific.getRemainingBudget() > -1)
			currentBudget = annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingBudget()]
					- currentAcceptedTravelTime - serviceTime * alreadyAcceptedOverall;
		double noAssignmentValue = ADPWithSoftOrienteeringANN.determineValueFunctionApproximationValue(ann, t - 1,
				arrivalProbability, annProblemSpecific, segmentInputMapper, timeWindowInputMapper,
				currentDistanceMeasure, currentBudget, alreadyAcceptedPerTimeWindow, null, demandSegmentWeightings,
				usePlainEstimator);

		// Opportunity costs per time window
		for (TimeWindow tw : timeWindowCandidates) {
			if (annProblemSpecific.getRemainingBudget() > -1)
				currentBudget = annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingBudget()]
						- bestRoutingsValueAfterInsertion.get(tw.getId()).getValue()
						- serviceTime * (alreadyAcceptedOverall + 1);
			double assignmentValue = ADPWithSoftOrienteeringANN.determineValueFunctionApproximationValue(ann, t - 1,
					arrivalProbability, annProblemSpecific, segmentInputMapper, timeWindowInputMapper,
					distanceMeasurePerTimeWindow.get(tw.getId()), currentBudget, alreadyAcceptedPerTimeWindow, tw,
					demandSegmentWeightings, usePlainEstimator);
			opportunityCostsPerTw.put(tw, noAssignmentValue - assignmentValue);
		}

	}

	private void updateANN(DeliveryArea area, Pair<ArrayList<double[]>, ArrayList<Order>> observations,
			boolean missingNormalization, boolean init) {

		// New attributes (accepted, distance
		// measure, overall expected, remaining travel time capacity)

		// Determine backward details
		ArrayList<Order> orders = observations.getValue();
		Collections.sort(orders, new OrderArrivalTimeAscComparator());

		ArrayList<Pair<Integer, Double>> accumulatedValues = new ArrayList<Pair<Integer, Double>>();
		double value = 0.0;

		int currentT = 0;
		for (int a = currentT; a < orders.get(0).getOrderRequest().getArrivalTime(); a++) {
			accumulatedValues.add(new Pair<Integer, Double>(a, value));
		}
		currentT = orders.get(0).getOrderRequest().getArrivalTime();

		int targetT = this.orderHorizonLength;

		for (int oId = 0; oId < orders.size(); oId++) {
			Order order = orders.get(oId);
			if (oId < orders.size() - 1) {
				targetT = orders.get(oId + 1).getOrderRequest().getArrivalTime() - 1;
			} else {
				targetT = this.orderHorizonLength;
			}

			if (init) {

				double oValue;
				if (this.useActualBasketValue) {
					oValue = CustomerDemandService.calculateOrderValue(order.getOrderRequest(), maximumRevenueValue,
							objectiveSpecificValues);
				} else {
					oValue = CustomerDemandService.calculateMedianValue(maximumRevenueValue, objectiveSpecificValues,
							order.getOrderRequest().getCustomer().getOriginalDemandSegment());
				}
				value = value + this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
						.get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
						.get(order.getTimeWindowFinalId()) * oValue;
			} else {
				//
				if (this.useActualBasketValue) {
					value = value + CustomerDemandService.calculateOrderValue(order.getOrderRequest(),
							maximumRevenueValue, objectiveSpecificValues);
				} else {
					value = value + CustomerDemandService.calculateMedianValue(maximumRevenueValue,
							objectiveSpecificValues, order.getOrderRequest().getCustomer().getOriginalDemandSegment());
				}

			}

			while (currentT <= targetT) {

				accumulatedValues.add(new Pair<Integer, Double>(currentT, value));
				// * Math.pow(this.discountingFactorProbability,
				// this.orderHorizonLength - currentT));
				currentT++; // at end, currentT equals arrival time of next
							// (targetT + 1)
			}

		}

		// Fill up till end of order horizon
		for (int t = currentT; t <= targetT; t++) {
			accumulatedValues.add(new Pair<Integer, Double>(t, value));
			// * Math.pow(this.discountingFactorProbability,
			// this.orderHorizonLength - currentT));
		}

		// Add one observation for the overall order horizon
		double[] observation = new double[this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
				.getMaxValuePerElement().length];
		if (considerConstant) {
			observation[this.ANNProblemSpecificPerDeliveryArea.get(area.getId()).getConstant()] = 1.0;
		}

		if (considerRemainingBudget) {

			observation[this.ANNProblemSpecificPerDeliveryArea.get(area.getId()).getRemainingBudget()] = 1.0;

		}

		if (this.distanceInAdp) {
			observation[this.ANNProblemSpecificPerDeliveryArea.get(area.getId()).getDistanceMeasure()] = 0.0;
		}

		if (this.considerRemainingTimeInsteadOfDemandSegments) {
			observation[this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
					.getRemainingTime()] = this.orderHorizonLength
							* this.arrivalProbability
							/ this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
									.getMaxValuePerElement()[this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
											.getRemainingTime()];
		} else {
			for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {

				observation[segmentInputMapper.get(ds.getId())] = this.orderHorizonLength * this.arrivalProbability
						* this.daSegmentWeightingsUpperHash.get(area).get(ds) / this.ANNProblemSpecificPerDeliveryArea
								.get(area.getId()).getMaxValuePerElement()[segmentInputMapper.get(ds.getId())];
			}
		}

		for (TimeWindow tw : this.timeWindowSet.getElements()) {

			observation[timeWindowInputMapper.get(tw.getId())] = 0.0;

		}

		observations.getKey().add(0, observation);

		// Collections.shuffle(accumulatedValues);

		// Train network with the available data
		for (int j = 0; j < accumulatedValues.size(); j++) {

			double[] observationC = observations.getKey()
					.get(this.orderHorizonLength - accumulatedValues.get(j).getKey());
			// Have to normalize distanceMeasure and accepted per time window if
			// missing (in case of initialization)
			if (missingNormalization) {
				for (TimeWindow tw : this.timeWindowSet.getElements()) {

					observationC[timeWindowInputMapper
							.get(tw.getId())] = observationC[timeWindowInputMapper.get(tw.getId())]
									/ this.ANNProblemSpecificPerDeliveryArea.get(area.getId())
											.getMaxValuePerElement()[timeWindowInputMapper.get(tw.getId())];
				}
			}

			this.ANNPerDeliveryArea.get(area.getId()).computeOutputs(observationC);

			double actualV = (accumulatedValues.get(j).getValue() + this.minimumValue)
					/ (this.maximumValue + this.minimumValue);
			if (this.hTanActivation)
				actualV = actualV * 2.0 - 1.0;
			this.ANNPerDeliveryArea.get(area.getId()).calcError(new double[] { actualV });

			this.ANNPerDeliveryArea.get(area.getId()).learn();
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

	public static double determineValueFunctionApproximationValue(NeuralNetwork ann, int t, double arrivalProbability,
			ArtificialNeuralNetworkRegion annProblemSpecific, HashMap<Integer, Integer> segmentInputMapper,
			HashMap<Integer, Integer> timeWindowInputMapper, double distanceMeasure, double remainingBudget,
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow, TimeWindow timeWindowToAccept,
			HashMap<DemandSegment, Double> demandSegmentWeightings, boolean usePlainEstimator) {

		if (t > 0 || usePlainEstimator) {

			double[] inputRow = new double[annProblemSpecific.getMaxValuePerElement().length];

			double expectedArrivals = arrivalProbability * t;

			if (annProblemSpecific.getConstant() > -1)
				inputRow[annProblemSpecific.getConstant()] = 1.0;
			if (annProblemSpecific.getDistanceMeasure() > -1)
				inputRow[annProblemSpecific.getDistanceMeasure()] = distanceMeasure
						/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getDistanceMeasure()];
			if (annProblemSpecific.getRemainingBudget() > -1)
				inputRow[annProblemSpecific.getRemainingBudget()] = remainingBudget
						/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingBudget()];
			if (annProblemSpecific.getRemainingTime() > -1) {
				inputRow[annProblemSpecific.getRemainingTime()] = expectedArrivals
						/ annProblemSpecific.getMaxValuePerElement()[annProblemSpecific.getRemainingTime()];
			} else {
				if (segmentInputMapper != null) {
					for (DemandSegment s : demandSegmentWeightings.keySet()) {
						inputRow[segmentInputMapper.get(s.getId())] = expectedArrivals * demandSegmentWeightings.get(s)
								/ annProblemSpecific.getMaxValuePerElement()[segmentInputMapper.get(s.getId())];
					}
				}
			}

			for (Integer tw : alreadyAcceptedPerTimeWindow.keySet()) {

				int accepted = alreadyAcceptedPerTimeWindow.get(tw);
				if (timeWindowToAccept != null && timeWindowToAccept.getId() == tw)
					accepted++;
				inputRow[timeWindowInputMapper.get(tw)] = accepted
						/ annProblemSpecific.getMaxValuePerElement()[timeWindowInputMapper.get(tw)];

			}

			double[] output = ann.computeOutputs(inputRow);
			double value = output[0];
			if (ann.isHyperbolicTangensActivation()) {
				value = (value + 1.0) / 2.0;
			}

			value = value * (annProblemSpecific.getMaximumValue() + annProblemSpecific.getMinimumValue())
					- annProblemSpecific.getMinimumValue();
			return value;
		} else {

			return 0.0;

		}
	}

	private void initialiseGlobal() {

		this.LOG_LOSS_FUNCTION = new HashMap<Integer, HashMap<Integer, ArrayList<Double>>>();
		this.LOG_WEIGHTS = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
		this.trainingSetNumberPerDeliveryArea = new HashMap<Integer, Integer>();
		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		this.arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId);
		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		this.alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				this.alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		if (this.momentumWeight > 0)
			this.oldMomentumPerDeliveryArea = new HashMap<Integer, MomentumHelper>();

		this.demandSegments = (ArrayList<DemandSegment>) this.demandSegmentWeighting.getSetEntity().getElements();
		this.daSegmentWeightingsLowerHash = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();
		this.daSegmentWeightingsUpperHash = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();
		for (DeliveryArea subArea : this.daSegmentWeightingsLower.keySet()) {
			this.daSegmentWeightingsLowerHash.put(subArea, new HashMap<DemandSegment, Double>());
			for (DemandSegmentWeight w : this.daSegmentWeightingsLower.get(subArea).getWeights()) {
				this.daSegmentWeightingsLowerHash.get(subArea).put(w.getDemandSegment(), w.getWeight());
			}
			for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {
				if (!this.daSegmentWeightingsLowerHash.get(subArea).containsKey(w.getDemandSegment())) {
					this.daSegmentWeightingsLowerHash.get(subArea).put(w.getDemandSegment(), 0.0);
				}
			}
		}

		for (DeliveryArea area : this.daSegmentWeightingsUpper.keySet()) {
			this.daSegmentWeightingsUpperHash.put(area, new HashMap<DemandSegment, Double>());
			for (DemandSegmentWeight w : this.daSegmentWeightingsUpper.get(area).getWeights()) {
				this.daSegmentWeightingsUpperHash.get(area).put(w.getDemandSegment(), w.getWeight());
			}
			for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {
				if (!this.daSegmentWeightingsUpperHash.get(area).containsKey(w.getDemandSegment())) {
					this.daSegmentWeightingsUpperHash.get(area).put(w.getDemandSegment(), 0.0);
				}
			}
		}
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

		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMinimumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
						this.daSegmentWeightingsLower, timeWindowSet);

		this.prepareValueMultiplier();
		if (this.areaSpecificValueFunction)
			this.determineValueDemandMultiplierLowerAreaPerTimeWindow();

		// Initialise basic and variable coefficients per delivery area
		this.initialiseANNRouting();

		// Define bounds on opportunity costsHashMap<Integer, HashMap<Integer,
		// Double>> maximumOpportunityCostsPerLowerDaAndTimeWindow = new
		// HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumOpportunityCostsPerLowerDaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		Collections.sort(this.demandSegments,
				new DemandSegmentsExpectedValueDescComparator(maximumLowerAreaWeight, objectiveSpecificValues));
		for (DeliveryArea a : this.daWeightsLower.keySet()) {
			maximumOpportunityCostsPerLowerDaAndTimeWindow.put(a.getId(), new HashMap<Integer, Double>());
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				boolean highestFound = false;
				for (DemandSegment ds : this.demandSegments) {

					if (this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(ds.getId())
							.containsKey(tw.getId())
							&& this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(ds.getId())
									.get(tw.getId()) > 0) {
						for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(a).getWeights()) {
							if (dsw.getElementId() == ds.getId() && dsw.getWeight() > 0) {
								maximumOpportunityCostsPerLowerDaAndTimeWindow.get(a.getId())
										.put(tw.getId(),
												CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
														objectiveSpecificValues, ds)
														* this.discountingFactorProbability);
								highestFound = true;
								break;
							}
						}
					}
					if (highestFound)
						break;
				}
			}
		}

	}

	private void determineReferenceInformation() {
		for (Routing r : this.previousRoutingResults) {
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

	private void initialiseANNRouting() {

		this.ANNPerDeliveryArea = new HashMap<Integer, NeuralNetwork>();
		this.ANNProblemSpecificPerDeliveryArea = new HashMap<Integer, ArtificialNeuralNetworkRegion>();
		double initialLearingRate = this.stepSize;

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			// Always: Nodes for demand segments, time windows, distance measure
			int numberOfAttributes = this.timeWindowSet.getElements().size();
			if (considerConstant)
				numberOfAttributes++;
			if (considerRemainingBudget)
				numberOfAttributes++;
			if (this.considerRemainingTimeInsteadOfDemandSegments) {
				numberOfAttributes += 1;
			} else {
				numberOfAttributes += this.demandSegmentWeighting.getWeights().size();
			}
			if (this.distanceInAdp)
				numberOfAttributes++;
			int numberHiddenNodes = numberOfAttributes + this.additionalHiddenNodes + 1;
			//
			double[] maxValuePerAttribute = new double[numberOfAttributes];
			int indexConstant = -1;

			if (considerConstant) {
				indexConstant = 0;
				maxValuePerAttribute[indexConstant] = 1.0;
			}

			int indexDistanceMeasure = -1;
			int currentIndex = indexConstant;
			if (this.distanceInAdp) {
				indexDistanceMeasure = currentIndex + 1;
				currentIndex = indexDistanceMeasure;
			}

			int indexRemainingBudget = -1;
			if (considerRemainingBudget) {
				indexRemainingBudget = currentIndex + 1;
				currentIndex++;
				maxValuePerAttribute[indexRemainingBudget] = this.overallCapacityPerDeliveryArea.get(area.getId());
			}

			int indexRemainingTime = -1;
			int counter = 0;
			if (this.considerRemainingTimeInsteadOfDemandSegments) {
				indexRemainingTime = currentIndex + 1;
				currentIndex++;
				maxValuePerAttribute[indexRemainingTime] = this.orderHorizonLength * this.arrivalProbability;
			} else {
				this.segmentInputMapper = new HashMap<Integer, Integer>();
				indicesDemandSegments = new int[this.demandSegmentWeighting.getWeights().size()][2];

				for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {
					indicesDemandSegments[counter][0] = ds.getId();
					indicesDemandSegments[counter++][1] = ++currentIndex;
					segmentInputMapper.put(ds.getId(), currentIndex);

					maxValuePerAttribute[currentIndex] = this.orderHorizonLength * this.arrivalProbability
							* this.daSegmentWeightingsUpperHash.get(area).get(ds);
				}
			}

			this.timeWindowInputMapper = new HashMap<Integer, Integer>();
			indicesTimeWindows = new int[this.timeWindowSet.getElements().size()][2];
			counter = 0;
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				indicesTimeWindows[counter][0] = tw.getId();
				indicesTimeWindows[counter++][1] = ++currentIndex;
				timeWindowInputMapper.put(tw.getId(), currentIndex);
			}

			HashMap<Routing, ArrayList<double[]>> observationsPerRouting = new HashMap<Routing, ArrayList<double[]>>();
			HashMap<Routing, ArrayList<Order>> ordersPerRouting = new HashMap<Routing, ArrayList<Order>>();

			if (this.distanceInAdp) {
				maxValuePerAttribute[indexDistanceMeasure] = 0.0;
			}

			this.minimumValue = 0.0;
			this.maximumValue = 0.0;
			for (Routing r : this.previousRoutingResults) {
				ArrayList<double[]> observations = new ArrayList<double[]>();
				ArrayList<Order> orders = new ArrayList<Order>();
				ArrayList<RouteElement> res = new ArrayList<RouteElement>();
				double collectedValue = 0.0;
				for (Route rou : r.getRoutes()) {
					res.addAll(rou.getRouteElements());
				}

				if (this.distanceInAdp) {
					if (maxValuePerAttribute[indexDistanceMeasure] < res.size())
						maxValuePerAttribute[indexDistanceMeasure] = res.size();
				}
				Collections.sort(res, new RouteElementArrivalTimeDescComparator());

				int last = this.orderHorizonLength;

				for (RouteElement re : res) {
					orders.add(re.getOrder());
					collectedValue += CustomerDemandService.calculateOrderValue(re.getOrder().getOrderRequest(),
							maximumRevenueValue, objectiveSpecificValues);
					for (int t = last - 1; t >= re.getOrder().getOrderRequest().getArrivalTime(); t--) {
						double[] observation = new double[numberOfAttributes];
						if (considerConstant) {
							observation[indexConstant] = 1.0;
						}

						if (considerRemainingBudget) {
							if (t == this.orderHorizonLength - 1) {
								observation[indexRemainingBudget] = 1.0;
							} else {
								observation[indexRemainingBudget] = observations
										.get(observations.size() - 1)[indexRemainingBudget];
							}
						}

						if (this.distanceInAdp) {
							observation[indexDistanceMeasure] = 0.0;
						}

						if (this.considerRemainingTimeInsteadOfDemandSegments) {
							observation[indexRemainingTime] = t * this.arrivalProbability
									/ maxValuePerAttribute[indexRemainingTime];
						} else {
							for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {

								observation[segmentInputMapper.get(ds.getId())] = t * this.arrivalProbability
										* this.daSegmentWeightingsUpperHash.get(area).get(ds)
										/ maxValuePerAttribute[segmentInputMapper.get(ds.getId())];

							}

						}
						for (TimeWindow tw : this.timeWindowSet.getElements()) {
							if (t == this.orderHorizonLength - 1) {
								observation[timeWindowInputMapper.get(tw.getId())] = 0;
							} else {
								observation[timeWindowInputMapper.get(tw.getId())] = observations
										.get(observations.size() - 1)[timeWindowInputMapper.get(tw.getId())];
							}
							;
						}
						observations.add(observation);
					}

					double[] observation = new double[numberOfAttributes];
					if (considerConstant) {
						observation[indexConstant] = 1.0;
					}

					if (considerRemainingBudget) {
						double lastValue;
						if (re.getOrder().getOrderRequest().getArrivalTime() == this.orderHorizonLength) {
							lastValue = 1.0;
						} else {
							lastValue = observations.get(observations.size() - 1)[indexRemainingBudget];
						}
						observation[indexRemainingBudget] = lastValue - (this.expectedServiceTime + re.getTravelTime())
								/ maxValuePerAttribute[indexRemainingBudget];
					}

					if (this.distanceInAdp) {
						observation[indexDistanceMeasure] = 0.0;
					}

					if (this.considerRemainingTimeInsteadOfDemandSegments) {
						observation[indexRemainingTime] = (re.getOrder().getOrderRequest().getArrivalTime() - 1)
								* this.arrivalProbability / maxValuePerAttribute[indexRemainingTime];
					} else {
						for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {

							observation[segmentInputMapper
									.get(ds.getId())] = (re.getOrder().getOrderRequest().getArrivalTime() - 1)
											* this.arrivalProbability
											* this.daSegmentWeightingsUpperHash.get(area).get(ds)
											/ maxValuePerAttribute[segmentInputMapper.get(ds.getId())];

						}
					}

					for (TimeWindow tw : this.timeWindowSet.getElements()) {

						double lastValue;
						if (re.getOrder().getOrderRequest().getArrivalTime() == this.orderHorizonLength) {
							lastValue = 0;
						} else {
							lastValue = observations.get(observations.size() - 1)[timeWindowInputMapper
									.get(tw.getId())];
						}

						if (re.getOrder().getTimeWindowFinalId() == tw.getId())
							lastValue++;
						observation[timeWindowInputMapper.get(tw.getId())] = lastValue;
					}
					observations.add(observation);

					last = re.getOrder().getOrderRequest().getArrivalTime() - 1;
				}

				if (last > 0) {
					for (int t = last - 1; t >= 0; t--) {
						double[] observation = new double[numberOfAttributes];
						if (considerConstant) {
							observation[indexConstant] = 1.0;
						}

						if (considerRemainingBudget) {

							observation[indexRemainingBudget] = observations
									.get(observations.size() - 1)[indexRemainingBudget];

						}

						if (this.distanceInAdp) {
							observation[indexDistanceMeasure] = 0.0;
						}

						if (this.considerRemainingTimeInsteadOfDemandSegments) {
							observation[indexRemainingTime] = t * this.arrivalProbability
									/ maxValuePerAttribute[indexRemainingTime];
						} else {
							for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {

								observation[segmentInputMapper.get(ds.getId())] = t * this.arrivalProbability
										* this.daSegmentWeightingsUpperHash.get(area).get(ds)
										/ maxValuePerAttribute[segmentInputMapper.get(ds.getId())];

							}
						}

						for (TimeWindow tw : this.timeWindowSet.getElements()) {

							observation[timeWindowInputMapper.get(tw.getId())] = observations
									.get(observations.size() - 1)[timeWindowInputMapper.get(tw.getId())];

						}
						observations.add(observation);
					}
				}

				observationsPerRouting.put(r, observations);
				ordersPerRouting.put(r, orders);

				for (TimeWindow tw : this.timeWindowSet.getElements()) {
					if (observations.get(observations.size() - 1)[timeWindowInputMapper
							.get(tw.getId())] > maxValuePerAttribute[timeWindowInputMapper.get(tw.getId())]) {
						maxValuePerAttribute[timeWindowInputMapper.get(tw.getId())] = observations
								.get(observations.size() - 1)[timeWindowInputMapper.get(tw.getId())];
					}
				}

				if (collectedValue > this.maximumValue)
					this.maximumValue = collectedValue;
			}

			ArtificialNeuralNetworkRegion annSavingObject = new ArtificialNeuralNetworkRegion(numberHiddenNodes, null,
					null, this.maximumValue, this.minimumValue, maxValuePerAttribute, indexConstant,
					indexDistanceMeasure, indexRemainingBudget, indexRemainingTime, indicesDemandSegments,
					indicesTimeWindows, this.hTanActivation);

			this.ANNProblemSpecificPerDeliveryArea.put(area.getId(), annSavingObject);

			NeuralNetwork network = new NeuralNetwork(numberOfAttributes, numberHiddenNodes, 1, initialLearingRate,
					this.momentumWeight, this.hTanActivation);
			// Init distanceMeasure weights negative
			if (this.distanceInAdp) {
				network.initOutgoingWeightsNegative(indexDistanceMeasure);
			}

			this.ANNPerDeliveryArea.put(area.getId(), network);

			for (Routing r : observationsPerRouting.keySet()) {
				this.updateANN(area, new Pair<ArrayList<double[]>, ArrayList<Order>>(observationsPerRouting.get(r),
						ordersPerRouting.get(r)), true, true);
			}

		}

	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerLowerDeliveryArea = new HashMap<Integer, Double>();
		this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
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
		this.benchmarkingOrderRequestsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.orderRequestsPerDeliveryArea.put(area.getId(), new HashMap<Integer, HashMap<Integer, OrderRequest>>());
			this.benchmarkingOrderRequestsPerDeliveryArea.put(area.getId(),
					new HashMap<Integer, HashMap<Integer, OrderRequest>>());
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

		// Go through benchmarking request sets
		for (int setId = 0; setId < this.benchmarkingOrderRequestSets.size(); setId++) {

			ArrayList<OrderRequest> requests = this.benchmarkingOrderRequestSets.get(setId).getElements();
			if (this.deliveryAreaSet.getElements().size() > 1) {
				for (int regId = 0; regId < requests.size(); regId++) {

					DeliveryArea assignedArea = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
							requests.get(regId).getCustomer());
					if (!this.benchmarkingOrderRequestsPerDeliveryArea.get(assignedArea.getId()).containsKey(setId)) {
						HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
						assignedRequests.put(requests.get(regId).getArrivalTime(), requests.get(regId));
						this.benchmarkingOrderRequestsPerDeliveryArea.get(assignedArea.getId()).put(setId,
								assignedRequests);
					} else {
						this.benchmarkingOrderRequestsPerDeliveryArea.get(assignedArea.getId()).get(setId)
								.put(requests.get(regId).getArrivalTime(), requests.get(regId));
					}
				}
			} else {
				HashMap<Integer, OrderRequest> assignedRequests = new HashMap<Integer, OrderRequest>();
				this.benchmarkingOrderRequestsPerDeliveryArea.get(this.deliveryAreaSet.getElements().get(0).getId())
						.put(setId, assignedRequests);
				for (int regId = 0; regId < requests.size(); regId++) {
					this.benchmarkingOrderRequestsPerDeliveryArea.get(this.deliveryAreaSet.getElements().get(0).getId())
							.get(setId).put(requests.get(regId).getArrivalTime(), requests.get(regId));
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

	}

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	private void aggregateReferenceInformation() {

		// TA: distance - really just travel time to?

		this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
		this.acceptablePerDeliveryAreaAndRouting = new HashMap<DeliveryArea, HashMap<Routing, Integer>>();
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
						// if(count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())>5)
						// System.out.println("Very much for one tw in one
						// subarea");
						distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								distance.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())
										+ (re.getTravelTime() + travelTimeFrom) / 2.0);
					}
				}
			}

			for (DeliveryArea a : this.aggregatedReferenceInformationNo.keySet()) {
				if (!this.acceptablePerDeliveryAreaAndRouting.containsKey(a))
					this.acceptablePerDeliveryAreaAndRouting.put(a, new HashMap<Routing, Integer>());
				this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing, 0);
				this.aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));

				double distanceSum = 0.0;
				for (DeliveryArea area : distance.get(a).keySet()) {
					for (TimeWindow tw : distance.get(a).get(area).keySet()) {
						this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing,
								this.acceptablePerDeliveryAreaAndRouting.get(a).get(routing)
										+ count.get(a).get(area).get(tw));
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

	private HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> copyAggregateNoInformation(
			DeliveryArea upperArea, boolean copyOnlyRandomRouting) {

		HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> infoList = new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();

		if (copyOnlyRandomRouting) {
			int i = new Random().nextInt(this.aggregatedReferenceInformationNo.get(upperArea).keySet().size());
			Iterator<Routing> it = this.aggregatedReferenceInformationNo.get(upperArea).keySet().iterator();
			while (i > 0) {
				it.next();
				i--;
			}
			Routing r = it.next();
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> info = this.aggregatedReferenceInformationNo
					.get(upperArea).get(r);
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> infoCopy = new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>();
			for (DeliveryArea area : info.keySet()) {
				infoCopy.put(area, new HashMap<TimeWindow, Integer>());
				for (TimeWindow tw : info.get(area).keySet()) {
					infoCopy.get(area).put(tw, info.get(area).get(tw));
				}
			}

			infoList.put(r, infoCopy);
		} else {
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

				infoList.put(r, infoCopy);
			}
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

	public static String[] getParameterSetting() {

		return paras;
	}

	public ValueFunctionApproximationModelSet getResult() {

		return modelSet;
	}

}
