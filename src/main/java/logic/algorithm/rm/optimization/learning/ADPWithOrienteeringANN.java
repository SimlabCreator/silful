package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

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
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.ArtificialNeuralNetwork;
import logic.entity.AssortmentAlgorithm;
import logic.entity.MomentumHelper;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LearningService;
import logic.service.support.LocationService;
import logic.service.support.OrienteeringAcceptanceHelperService;
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
public class ADPWithOrienteeringANN implements ValueFunctionApproximationAlgorithm {

	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static double AMOUNT_PER_ITERATION = 1.0;
	private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> LOG_LOSS_FUNCTION;
	private HashMap<Integer, HashMap<Integer, ArrayList<String>>> LOG_WEIGHTS;
	private double SAMPLE_SIZE_BETWEEN_EXPERIENCE_REPLAY = 500;

	private static double demandRatioRevenue = 0.05;

	private static double E_GREEDY_VALUE = 0.5;
	private static double E_GREEDY_VALUE_MAX = 0.95;

	private static boolean possiblyLargeOfferSet = true;
	private static double TIME_MULTIPLIER = 60.0;
	private TimeWindowSet timeWindowSet;
	private TimeWindowSet timeWindowSetOverlappingDummy;
	private HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping;
	private HashMap<TimeWindow, ArrayList<TimeWindow>> newToOldTimeWindowMapping;
	private AlternativeSet alternativeSet;
	private ValueFunctionApproximationModelSet modelSet;
	private ArrayList<Routing> targetRoutingResults;
	private ArrayList<Routing> previousRoutingResults;
	private ArrayList<OrderSet> previousOrderSetResults;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> benchmarkingOrderRequestsPerDeliveryArea;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private double expectedServiceTime;

	private ArrayList<DemandSegment> demandSegments;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>> referenceRoutingsPerDeliveryArea;
	private ArrayList<Routing> referenceRoutingsList;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<TimeWindow, Integer>>> aggregatedReferenceInformationNoSumOverSubareas;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting;
	private HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private HashMap<Integer, Integer> segmentInputMapper;
	private HashMap<Integer, Integer> neighborSegmentInputMapper;
	private HashMap<Integer, Integer> timeWindowInputMapper;

	// Per upper area: what is the average accepted number per tw -> basis for
	// demand/capacity ratio
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;

	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerUpperDeliveryAreaAndTw;

	private HashMap<Integer, HashMap<TimeWindow, Double>> demandMultiplierPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> demandMultiplierLowerAreaPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> valueMultiplierPerLowerDeliveryArea;
	private HashMap<Integer, Double> maximumAreaPotentialPerDeliveryArea;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;

	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<DeliveryArea, Double> daWeightsUpper;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper;
	private HashMap<DeliveryArea, Double> daWeightsLower;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower;
	private HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLowerHash;
	private double maximumLowerAreaWeight;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
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

	private boolean considerLeftOverPenalty;
	private boolean theftBased;
	private boolean theftBasedAdvanced;
	private boolean theftBasedTw;
	private boolean areaSpecificValueFunction;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;

	private HashMap<Integer, Integer> trainingSetNumberPerDeliveryArea;
	private Double discountingFactor;
	private DemandSegmentWeighting demandSegmentWeighting;
	private HashMap<Integer, Double> maximumValuePerDeliveryArea;
	private HashMap<Integer, Double> minimumValuePerDeliveryArea;
	private Double discountingFactorProbability;
	private HashMap<Integer, HashMap<Integer, Double>> maximumOpportunityCostsPerLowerDaAndTimeWindow;
	private HashMap<Integer, Integer> stolenPerTimeWindow;
	private HashMap<Integer, HashMap<DemandSegment, Double>> maximumArrivalsOverSubAreasPerDeliveryArea;
	private HashMap<Integer, HashMap<DemandSegment, Double>> maximumNeighborArrivalsOverSubAreasPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea;
	private HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues;
	private ArrayList<OrderRequestSet> benchmarkingOrderRequestSets;
	private Integer additionalHiddenNodes;
	private boolean considerConstant;
	private boolean considerDemandNeighbors;
	private boolean oppCostOnlyFeasible;
	private boolean hTanActivation;
	protected HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw;
	private double arrivalProbability;

	private static String[] paras = new String[] { "stepsize_adp_learning", "actualBasketValue", "samplePreferences",
			"annealing_temperature_(Negative:no_annealing)",
			"exploration_(0:on-policy,1:conservative-factor,2:e-greedy)", "momentum_weight",
			// "consider_demand_capacity_ratio",
			"theft-based", "theft-based-advanced", "theft-based-tw", "consider_left_over_penalty", "discounting_factor",
			"discounting_factor_probability", "consider_constant", "additional_hidden_nodes",
			"consider_demand_neighbors", "oc_for_feasible", "hTan_activation", "Constant_service_time" };

	public ADPWithOrienteeringANN(Region region, DemandSegmentWeighting demandSegmentWeighting,
			VehicleAreaAssignmentSet vehicleAreaAssignmentSet, ArrayList<Routing> previousRoutingResults,
			ArrayList<OrderSet> previousOrderSetResults, DeliveryAreaSet deliveryAreaSet,
			HashMap<DeliveryArea, Double> daWeightsUpperAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpperAreas,
			HashMap<DeliveryArea, Double> daWeightsLowerAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas,
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValueDouble, Double actualBasketValue, int orderHorizonLength, double stepSize,
			Double annealingTemperature, Double explorationStrategy, Double samplePreferences,

			Double momentumWeight,

			Double theftBased, Double theftBasedAdvanced, Double theftBasedTw, Double considerLeftOverPenalty,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, int arrivalProcessId, Double discountingFactor,
			Double discountingFactorProbability, HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues,
			ArrayList<OrderRequestSet> benchmarkingOrderRequestSets, Double additionalHiddenNodes,
			Double considerConstant, Double considerDemandNeighbors, Double oppCostOnlyFeasible, Double hTanActivation,
			Double expectedServiceTime) {

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
		this.orderHorizonLength = orderHorizonLength;
		this.annealingTemperature = annealingTemperature;
		this.stepSize = stepSize;
		this.explorationStrategy = explorationStrategy;

		this.momentumWeight = momentumWeight;
		this.neighbors = neighbors;
		this.arrivalProcessId = arrivalProcessId;

		this.considerLeftOverPenalty = (considerLeftOverPenalty == 1.0);
		this.considerDemandNeighbors = (considerDemandNeighbors == 1.0);
		this.oppCostOnlyFeasible = (oppCostOnlyFeasible == 1.0);
		this.discountingFactorProbability = discountingFactorProbability;
		this.discountingFactor = discountingFactor;
		this.benchmarkValues = benchmarkValues;
		this.benchmarkingOrderRequestSets = benchmarkingOrderRequestSets;
		this.theftBasedTw = (theftBasedTw == 1.0);

		if (considerConstant == 1.0) {
			this.considerConstant = true;
		} else {
			this.considerConstant = false;
		}
		this.additionalHiddenNodes = additionalHiddenNodes.intValue();
		if (theftBased == 1.0) {
			this.theftBased = true;
		} else {
			this.theftBased = false;
		}

		if (theftBasedAdvanced == 1.0) {
			this.theftBasedAdvanced = true;
		} else {
			this.theftBasedAdvanced = false;
		}

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

		// if (initialiseProblemSpecific == 1.0) {
		// this.initialiseProblemSpecific = true;
		// } else {
		// this.initialiseProblemSpecific = false;
		// }
	};

	public void start() {

		this.initialiseGlobal();

		// Solve problem per delivery areax

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
			model.setObjectiveFunctionValueLog(this.LOG_LOSS_FUNCTION.get(area.getId()));
			model.setWeightsLog(this.LOG_WEIGHTS.get(area.getId()));

			int numberHiddenNodes = this.demandSegmentWeighting.getWeights().size() + this.additionalHiddenNodes + 1;
			if (this.timeWindowSet.getOverlapping()) {
				numberHiddenNodes += this.timeWindowSetOverlappingDummy.getElements().size();
			} else {
				numberHiddenNodes += this.timeWindowSet.getElements().size();
			}
			if (considerConstant)
				numberHiddenNodes++;
			if (this.considerDemandNeighbors)
				numberHiddenNodes += this.demandSegmentWeighting.getWeights().size();

			ArtificialNeuralNetwork annSavingObject = new ArtificialNeuralNetwork(
					this.ANNPerDeliveryArea.get(area.getId()).getInputTypes(), numberHiddenNodes,
					this.ANNPerDeliveryArea.get(area.getId()).getInputSetIds(),
					this.ANNPerDeliveryArea.get(area.getId()).getInputElementIds(),
					this.ANNPerDeliveryArea.get(area.getId()).getMatrix(),
					this.ANNPerDeliveryArea.get(area.getId()).getThresholds(),
					this.maximumValuePerDeliveryArea.get(area.getId()),
					this.minimumValuePerDeliveryArea.get(area.getId()), 0.0, this.theftBased,
					this.ANNPerDeliveryArea.get(area.getId()).getMaximumValuesElements(), this.considerConstant,
					this.considerDemandNeighbors, this.hTanActivation);

			ObjectMapper mapper = new ObjectMapper();
			String annModelString = null;
			try {
				annModelString = mapper.writeValueAsString(annSavingObject);
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

	private void applyADPForDeliveryArea(DeliveryArea area) {

		HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo = this
				.copyAggregateNoInformation(area, true);

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

			Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>> acceptedOrders = this.simulateArrivalProcess(area,
					aggregateInformationNo, maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
					this.orderRequestsPerDeliveryArea.get(area.getId()).get(it.next()),
					this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
					this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
					(this.theftBased && this.considerDemandNeighbors), false,
					(this.theftBasedTw && this.considerDemandNeighbors), discountingFactorProbability,
					this.explorationStrategy, firstNumberOfIterations, currentIndex, E_GREEDY_VALUE_MAX,
					E_GREEDY_VALUE);
			ArrayList<Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>> results = new ArrayList<Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>>();
			results.add(acceptedOrders);
			double currentStepSize = this.stepSize;
			if (this.annealingTemperature > 0) {
				currentStepSize = stepSize / (1.0 + (double) currentOrderRequestIndex / this.annealingTemperature);
			}
			this.ANNPerDeliveryArea.get(area.getId()).setLearnRate(currentStepSize);
			this.updateANN(area, results, maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
					this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
					this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId()));

			if (it.hasNext() && currentIndex < firstNumberOfIterations - 1
					&& (currentIndex % SAMPLE_SIZE_BETWEEN_EXPERIENCE_REPLAY != 0
							|| (currentIndex % SAMPLE_SIZE_BETWEEN_EXPERIENCE_REPLAY == 0 && currentIndex == 0))) {
				aggregateInformationNo = this.copyAggregateNoInformation(area, true);
			} else {
				aggregateInformationNo = this.copyAggregateNoInformation(area, false);
			}
			if (currentIndex % SAMPLE_SIZE_BETWEEN_EXPERIENCE_REPLAY == 0 && currentIndex != 0) {
				this.updateANNRouting(targetRoutingResults, currentStepSize, false);
				if (it.hasNext() && currentIndex < firstNumberOfIterations - 1) {
					aggregateInformationNo = this.copyAggregateNoInformation(area, true);
				} else {
					aggregateInformationNo = this.copyAggregateNoInformation(area, false);
				}
			}
			currentIndex++;
			currentOrderRequestIndex++;

		}


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
		if (false) {
			Double lastValue = null;
			for (int iterationNo = 0; iterationNo <= numberOfIterations; iterationNo++) {

				// Check if stopping possible
				// 1.) Determine average performance for validation order
				// request
				// sets
				double averageAccepted = 0.0;
				double averageValueAccepted = 0.0;
				Iterator<Integer> itOS = this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).keySet()
						.iterator();
				while (itOS.hasNext()) {

					HashMap<DeliveryArea, ArrayList<Order>> acceptedOrders = this
							.simulateArrivalProcess(area, aggregateInformationNo,
									maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
									this.benchmarkingOrderRequestsPerDeliveryArea.get(area.getId()).get(itOS.next()),
									this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
									this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
									this.theftBased, this.theftBasedAdvanced, this.theftBasedTw,
									discountingFactorProbability, 1.0, null, null, null, null)
							.getValue();

					for (DeliveryArea a : acceptedOrders.keySet()) {
						averageAccepted += acceptedOrders.get(a).size();
						for (Order o : acceptedOrders.get(a)) {
							averageValueAccepted += CustomerDemandService.calculateOrderValue(o.getOrderRequest(),
									maximumRevenueValue, objectiveSpecificValues);
						}

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
				} else if (iterationNo > 0 && lastError >= oldError * 1.2 && better
						&& !(lastValue * 1.2 < averageValue)) {
					break;
				}

				lastValue = averageValue;

				// 4.) Otherwise, go on with another learning round
				currentIndex = 0;
				System.out.println("Experiencing now. Training round: " + iterationNo);
				while (it.hasNext() && currentIndex < numberPerIteration) {
					System.out.println("Iteration: " + currentIndex);

					Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>> acceptedOrders = this.simulateArrivalProcess(
							area, aggregateInformationNo,
							maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
							this.orderRequestsPerDeliveryArea.get(area.getId()).get(it.next()),
							this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
							this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
							(this.theftBased && this.considerDemandNeighbors), false,
							(this.theftBasedTw && this.considerDemandNeighbors), discountingFactorProbability,
							this.explorationStrategy, numberPerIteration.intValue(), currentIndex, 1.0,
							E_GREEDY_VALUE_MAX);
					ArrayList<Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>> results = new ArrayList<Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>>();
					results.add(acceptedOrders);
					this.updateANN(area, results,
							maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
							this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
							this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId()));

					if (it.hasNext() && currentIndex < numberPerIteration - 1
							&& (currentIndex % 1000 != 0 || (currentIndex % 1000 == 0 && currentIndex == 0))) {
						aggregateInformationNo = this.copyAggregateNoInformation(area, true);
					} else {
						aggregateInformationNo = this.copyAggregateNoInformation(area, false);
					}
					if ((currentIndex % 1000 == 0 && currentIndex != 0)
							|| (currentIndex == numberPerIteration && numberPerIteration < 1000)) {
						this.updateANNRouting(targetRoutingResults,
								this.ANNPerDeliveryArea.get(area.getId()).getLearnRate(), false);
						if (it.hasNext() && currentIndex < numberPerIteration - 1) {
							aggregateInformationNo = this.copyAggregateNoInformation(area, true);
						} else {
							aggregateInformationNo = this.copyAggregateNoInformation(area, false);
						}
					}

					currentIndex++;
					currentOrderRequestIndex++;

					double currentStepSize = this.stepSize;
					if (this.annealingTemperature > 0) {
						currentStepSize = stepSize
								/ (1.0 + (double) currentOrderRequestIndex / this.annealingTemperature);
					}
					this.ANNPerDeliveryArea.get(area.getId()).setLearnRate(currentStepSize);
				}
				oldError = lastError;
				lastError = this.ANNPerDeliveryArea.get(area.getId()).getError(this.orderHorizonLength
						* numberPerIteration.intValue() * area.getSubset().getElements().size());

				// this.updateANN(area, acceptedOrdersPerORS,
				// maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.get(area.getId()),
				// this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId()),
				// numberPerIteration.intValue());
			}
		}

	}



	private Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>> simulateArrivalProcess(DeliveryArea area,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas, HashMap<Integer, OrderRequest> requests,
			HashMap<DemandSegment, Double> maximumArrivalsPerDemandSegment,
			HashMap<DemandSegment, Double> maximumNeighborArrivalsPerDemandSegment, boolean thefting,
			boolean theftingAdvanced, boolean theftingTw, double discountingFactorProbability,
			double currentExplorationStrategy, Integer numberOfIterations, Integer currentIteration,
			Double highestGreedyValue, Double lowestGreedyValue) {

		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrders = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<Routing, HashMap<DeliveryArea, ArrayList<Order>>> acceptedOrdersPerTOP = new HashMap<Routing, HashMap<DeliveryArea, ArrayList<Order>>>();
		stolenPerTimeWindow = new HashMap<Integer, Integer>();
		Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = ADPWithOrienteeringANN
				.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(aggregateInformationNo);
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw = updatedAggregates.getKey();
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		TimeWindowSet relevantTimeWindowSet;
		if (this.timeWindowSet.getOverlapping()) {
			relevantTimeWindowSet = this.timeWindowSetOverlappingDummy;
		} else {
			relevantTimeWindowSet = this.timeWindowSet;
		}
		// Go through requests
		for (int t = this.orderHorizonLength; t > 0; t--) {

			if (requests.containsKey(t)) {
				OrderRequest request = requests.get(t);
				DeliveryArea subArea = LocationService
						.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet, request.getCustomer());
				request.getCustomer().setTempDeliveryArea(subArea);

				ArrayList<TimeWindow> timeWindowCandidatesOrienteering = new ArrayList<TimeWindow>();
				ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting = new ArrayList<TimeWindow>();
				ArrayList<TimeWindow> timeWindowCandidatesOrienteeringTheftingTw = new ArrayList<TimeWindow>();
				ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced = new ArrayList<TimeWindow>();
				// Check aggregated orienteering based feasibility (if
				// applicable)
				ADPWithOrienteeringANN.checkFeasibilityBasedOnOrienteeringNo(request, t, subArea,
						timeWindowCandidatesOrienteering, timeWindowCandidatesOrienteeringThefting,
						timeWindowCandidatesOrienteeringTheftingTw,
						potentialTimeWindowCandidatesOrienteeringTheftingAdvanced, avgAcceptablePerSubAreaAndTw,
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, thefting, theftingAdvanced, theftingTw,
						this.neighbors, this.neighborsTw, aggregateInformationNo.keySet().size(),
						this.oldToNewTimeWindowMapping);

				// Determine opportunity costs
				HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, DeliveryArea>>();
				HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>>();
				HashMap<TimeWindow, Double> opportunityCostsPerTw = new HashMap<TimeWindow, Double>();

				ADPWithOrienteeringANN.determineOpportunityCostsPerTw(area, subArea, request, t, aggregateInformationNo,
						maxAcceptablePerTwOverSubAreas, maximumArrivalsPerDemandSegment,
						maximumNeighborArrivalsPerDemandSegment, timeWindowCandidatesOrienteering,
						timeWindowCandidatesOrienteeringThefting, timeWindowCandidatesOrienteeringTheftingTw,
						potentialTimeWindowCandidatesOrienteeringTheftingAdvanced,
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
						advancedStealingAreasPerTimeWindowAndRouting, thefting, theftingAdvanced, theftingTw,
						maximumValuePerDeliveryArea, minimumValuePerDeliveryArea, neighbors, neighborsTw,
						this.demandSegmentWeighting, daWeightsLower, daSegmentWeightingsLowerHash,
						relevantTimeWindowSet, arrivalProcessId, ANNPerDeliveryArea, this.considerConstant,
						this.considerDemandNeighbors, considerLeftOverPenalty, demandSegments, objectiveSpecificValues,
						maximumRevenueValue, maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
						minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
						maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow, opportunityCostsPerTw,
						discountingFactorProbability, aggregateInformationNo.keySet().size(), this.arrivalProbability,
						this.oppCostOnlyFeasible, this.segmentInputMapper, this.neighborSegmentInputMapper,
						this.timeWindowInputMapper, oldToNewTimeWindowMapping);

				// Find best subset from the time windows with value add

				HashMap<TimeWindow, TimeWindow> finalTwPerUpperTw = new HashMap<TimeWindow, TimeWindow>();
				if (this.timeWindowSet.getOverlapping()) {
					HashMap<TimeWindow, Double> opportunityCostsPerUpperTw = new HashMap<TimeWindow, Double>();

					ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer()
							.getOriginalDemandSegment().getConsiderationSet();
					for (ConsiderationSetAlternative alt : alternatives) {
						if (!alt.getAlternative().getNoPurchaseAlternative()) {
							ArrayList<TimeWindow> lowerTws = this.oldToNewTimeWindowMapping
									.get(alt.getAlternative().getTimeWindows().get(0));
							double lowestOppCost = Double.MAX_VALUE;
							TimeWindow finalTw = null;
							for (TimeWindow tw : lowerTws) {
								if (opportunityCostsPerTw.containsKey(tw)
										&& opportunityCostsPerTw.get(tw) < lowestOppCost) {
									finalTw = tw;
									lowestOppCost = opportunityCostsPerTw.get(tw);
								}
							}
							if (finalTw != null) {
								opportunityCostsPerUpperTw.put(alt.getAlternative().getTimeWindows().get(0),
										lowestOppCost);
								finalTwPerUpperTw.put(alt.getAlternative().getTimeWindows().get(0), finalTw);
							}
						}
					}
					opportunityCostsPerTw = opportunityCostsPerUpperTw;

				}
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

					if (selectedAlt != null && selectedAlt.getAlternative() != null
							&& !selectedAlt.getAlternative().getNoPurchaseAlternative()) {

						order.setAccepted(true);
						order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
						order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
						TimeWindow relevantAcceptedTw;
						if (this.timeWindowSet.getOverlapping()) {
							order.setFinalTimeWindowTempId(finalTwPerUpperTw
									.get(selectedAlt.getAlternative().getTimeWindows().get(0)).getId());
							relevantAcceptedTw = finalTwPerUpperTw
									.get(selectedAlt.getAlternative().getTimeWindows().get(0));
						} else {
							relevantAcceptedTw = selectedAlt.getAlternative().getTimeWindows().get(0);
						}
						if (!acceptedOrders.containsKey(subArea)) {
							acceptedOrders.put(subArea, new ArrayList<Order>());
						}
						acceptedOrders.get(subArea).add(order);

						// Update orienteering information
						OrienteeringAcceptanceHelperService.updateOrienteeringNoInformation(subArea, order,
								relevantAcceptedTw, aggregateInformationNo,
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
								advancedStealingAreasPerTimeWindowAndRouting, thefting, theftingAdvanced,
								acceptedOrdersPerTOP, null, null, null, null, null, null);

						int relevantId;
						if (this.timeWindowSet.getOverlapping()) {
							relevantId = order.getFinalTimeWindowTempId();
						} else {
							relevantId = order.getTimeWindowFinalId();
						}
						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
									new HashMap<Integer, Integer>());
						}
						if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.containsKey(relevantId)) {
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(relevantId, 0);
						}
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(relevantId,
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(relevantId)
										+ 1);

						updatedAggregates = ADPWithOrienteeringANN
								.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
										aggregateInformationNo);
						avgAcceptablePerSubAreaAndTw = updatedAggregates.getKey();
					}
				}
			}

		}

		// Only return first routing that is still feasible (could
		// theoretically be more)

		Routing r = aggregateInformationNo.keySet().iterator().next();

		if (theftingAdvanced) {
			return new Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>(r, acceptedOrders);
		} else {
			return new Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>(r, acceptedOrdersPerTOP.get(r));
		}

	}

	public static HashMap<TimeWindow, Double> determineOpportunityCostsPerTwAndRouting(DeliveryArea area,
			DeliveryArea subArea, OrderRequest request, int t, Routing r,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			HashMap<DemandSegment, Double> maximumArrivalsPerDemandSegment,
			HashMap<DemandSegment, Double> maximumArrivalsNeighborsPerDemandSegment,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting, boolean thefting,
			boolean theftingAdvanced, double weightNeighbors, HashMap<Integer, Double> maximumValuePerDeliveryArea,
			HashMap<Integer, Double> minimumValuePerDeliveryArea,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, DemandSegmentWeighting dsw,
			HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLower, TimeWindowSet tws,
			int arrivalProcessId, HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea, boolean considerConstant,
			boolean considerNeighborDemand, boolean considerLeftOverPenalty, ArrayList<DemandSegment> demandSegments,
			HashMap<Entity, Object> objectiveSpecificValues, double maximumRevenueValue,
			HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<Integer, Integer> segmentInputMapper, HashMap<Integer, Integer> neighborSegmentInputMapper,
			HashMap<Integer, Integer> timeWindowInputMapper, double arrivalProbability) {

		HashMap<TimeWindow, Double> opportuntityCostsPerTw = new HashMap<TimeWindow, Double>();
		HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> capacities = aggregateInformationNo.get(r);

		// Determine no assignment value for current area and neighbors
		double noAssignmentValue = ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, subArea, t - 1,
				capacities, maxAcceptablePerTwOverSubAreas, null, maximumArrivalsPerDemandSegment,
				maximumArrivalsNeighborsPerDemandSegment, maximumValuePerDeliveryArea.get(area.getId()),
				minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower, daSegmentWeightingsLower,
				neighbors, tws, arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty, demandSegments,
				objectiveSpecificValues, maximumRevenueValue, maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
				minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant, considerNeighborDemand,
				segmentInputMapper, neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

		HashMap<DeliveryArea, Double> noAssignmentValues = new HashMap<DeliveryArea, Double>();
		noAssignmentValues.put(subArea, noAssignmentValue);
		if (thefting) {
			for (DeliveryArea neighbor : neighbors.get(subArea)) {
				noAssignmentValues.put(neighbor,
						ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, neighbor, t - 1,
								capacities, maxAcceptablePerTwOverSubAreas, null, maximumArrivalsPerDemandSegment,
								maximumArrivalsNeighborsPerDemandSegment, maximumValuePerDeliveryArea.get(area.getId()),
								minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
								daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
								considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
								maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
								minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
								considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
								timeWindowInputMapper, arrivalProbability));
			}
		}
		ArrayList<TimeWindow> timeWindowCandidates = new ArrayList<TimeWindow>();
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteering);
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteeringThefting);
		for (TimeWindow tw : timeWindowCandidates) {

			// If this tw is assigned, is this routing afterwards
			// still feasible? -> only consider for opportunity
			// costs
			boolean feasible = false;
			if (capacities.containsKey(subArea) && capacities.get(subArea).containsKey(tw)
					&& capacities.get(subArea).get(tw) > 0) {
				feasible = true;
			}

			if (feasible) {

				// Opportunity costs in current area
				double opportunityCosts = noAssignmentValue
						- ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, subArea, t - 1,
								capacities, maxAcceptablePerTwOverSubAreas, tw, maximumArrivalsPerDemandSegment,
								maximumArrivalsNeighborsPerDemandSegment, maximumValuePerDeliveryArea.get(area.getId()),
								minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
								daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
								considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
								maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
								minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
								considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
								timeWindowInputMapper, arrivalProbability);

				// Opportunity costs for neighbors
				int countFeasibleNeighbors = 0;
				double neighborOpportunityCosts = 0.0;

				if (weightNeighbors > 0) {
					for (DeliveryArea neighbor : neighbors.get(subArea)) {
						if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
								&& capacities.get(neighbor).get(tw) > 0) {
							countFeasibleNeighbors++;
							neighborOpportunityCosts += (noAssignmentValues.get(neighbor)
									- ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, neighbor,
											t - 1, capacities, maxAcceptablePerTwOverSubAreas, tw,
											maximumArrivalsPerDemandSegment, maximumArrivalsNeighborsPerDemandSegment,
											maximumValuePerDeliveryArea.get(area.getId()),
											minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
											daSegmentWeightingsLower, neighbors, tws, arrivalProcessId,
											ANNPerDeliveryArea, considerLeftOverPenalty, demandSegments,
											objectiveSpecificValues, maximumRevenueValue,
											maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
											minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
											considerConstant, considerNeighborDemand, segmentInputMapper,
											neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability));
						}
					}
					neighborOpportunityCosts = neighborOpportunityCosts / countFeasibleNeighbors;
				}
				if (countFeasibleNeighbors > 0) {
					opportuntityCostsPerTw.put(tw,
							(opportunityCosts + neighborOpportunityCosts * weightNeighbors) / (1.0 + weightNeighbors));
				} else {
					opportuntityCostsPerTw.put(tw, opportunityCosts);
				}

			} else if (thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
					&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
					&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

				// If thefting for this time window would have been
				// allowed, then determine stealing neighbor for
				// this routing
				if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
					stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
				}

				// Go through neighbors and check feasibility as
				// well as opportunity costs
				double lowestOpportunityCostsRouting = 1.0;
				DeliveryArea areaWithLowestOpportunityCostsRouting = null;
				int countFeasibleNeighbors = 0;
				double neighborOpportunityCosts = 0.0;
				for (DeliveryArea neighbor : neighbors.get(subArea)) {
					if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
							&& capacities.get(neighbor).get(tw) > 0) {
						countFeasibleNeighbors++;
						double oppCosts = (noAssignmentValues.get(neighbor)
								- ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, neighbor, t - 1,
										capacities, maxAcceptablePerTwOverSubAreas, tw, maximumArrivalsPerDemandSegment,
										maximumArrivalsNeighborsPerDemandSegment,
										maximumValuePerDeliveryArea.get(area.getId()),
										minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
										daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
										considerLeftOverPenalty, demandSegments, objectiveSpecificValues,
										maximumRevenueValue, maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
										minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
										considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
										timeWindowInputMapper, arrivalProbability));
						neighborOpportunityCosts += oppCosts;
						// Best result?
						if (oppCosts < lowestOpportunityCostsRouting) {
							lowestOpportunityCostsRouting = oppCosts;
							areaWithLowestOpportunityCostsRouting = neighbor;
						}
					}
				}
				if (areaWithLowestOpportunityCostsRouting != null) {
					stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithLowestOpportunityCostsRouting);

					if (weightNeighbors > 0) {
						neighborOpportunityCosts = neighborOpportunityCosts / (double) countFeasibleNeighbors;
						opportuntityCostsPerTw.put(tw,
								(lowestOpportunityCostsRouting + neighborOpportunityCosts * weightNeighbors)
										/ (1.0 + weightNeighbors));
					} else {
						opportuntityCostsPerTw.put(tw, lowestOpportunityCostsRouting);
					}

				}
			}

		}



		return opportuntityCostsPerTw;
	}

	public static void determineOpportunityCostsPerTw(DeliveryArea area, DeliveryArea subArea, OrderRequest request,
			int t, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			HashMap<DemandSegment, Double> maximumArrivalsPerDemandSegment,
			HashMap<DemandSegment, Double> maximumNeighborArrivalsPerDemandSegment,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringTheftingTw,
			ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting,
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting,
			boolean thefting, boolean theftingAdvanced, boolean theftingTw,
			HashMap<Integer, Double> maximumValuePerDeliveryArea, HashMap<Integer, Double> minimumValuePerDeliveryArea,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw, DemandSegmentWeighting dsw,
			HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLower, TimeWindowSet tws,
			int arrivalProcessId, HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea, boolean considerConstant,
			boolean considerNeighborDemand, boolean considerLeftOverPenalty, ArrayList<DemandSegment> demandSegments,
			HashMap<Entity, Object> objectiveSpecificValues, double maximumRevenueValue,
			HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, Double> opportunityCostsPerTw, double discountingFactorProbability, int numberOfTops,
			double arrivalProbability, boolean oppCostOnlyFeasible, HashMap<Integer, Integer> segmentInputMapper,
			HashMap<Integer, Integer> neighborSegmentInputMapper, HashMap<Integer, Integer> timeWindowInputMapper,
			HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping) {

		ArrayList<TimeWindow> timeWindowCandidates = new ArrayList<TimeWindow>();
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteering);
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteeringThefting);
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteeringTheftingTw);
		timeWindowCandidates.addAll(potentialTimeWindowCandidatesOrienteeringTheftingAdvanced);

		if (!oppCostOnlyFeasible) {
			HashMap<TimeWindow, Double> assignmentValuePerTw = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> assignmentValuePerStealingTw = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> assignmentValuePerStealingTwAdvanced = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, HashMap<Routing, Double>> stealingValuePerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, Double>>();
			HashMap<TimeWindow, HashMap<Routing, Double>> stealingOppCostPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, Double>>();
			HashMap<TimeWindow, HashMap<Routing, Double>> advancedStealingValuePerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, Double>>();
			HashMap<TimeWindow, Integer> numberFeasibleRoutings = new HashMap<TimeWindow, Integer>();
			HashMap<TimeWindow, Double> minimumOpportunityCostsPerTimeWindow = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> maximumOpportunityCostsPerTimeWindow = new HashMap<TimeWindow, Double>();
			double noAssignmentValue = 0.0;
			double noAssignmentValueSubArea = 0.0;

			for (Routing r : aggregateInformationNo.keySet()) {

				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> capacities = aggregateInformationNo.get(r);

				// Determine no assignment value for all subareas of current
				// area
				double noAssignmentValueR = 0.0;
				HashMap<DeliveryArea, Double> noAssignmentValueRPerSubarea = new HashMap<DeliveryArea, Double>();
				HashMap<DeliveryArea, Integer> overallCapacityPerArea = new HashMap<DeliveryArea, Integer>();
				for (DeliveryArea sArea : area.getSubset().getElements()) {

					double valueArea = ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, sArea,
							t - 1, capacities, maxAcceptablePerTwOverSubAreas, null, maximumArrivalsPerDemandSegment,
							maximumNeighborArrivalsPerDemandSegment, maximumValuePerDeliveryArea.get(area.getId()),
							minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
							daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
							considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
							maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
							minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
							considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
							timeWindowInputMapper, arrivalProbability);
					noAssignmentValueR += valueArea;
					noAssignmentValueRPerSubarea.put(sArea, valueArea);
					overallCapacityPerArea.put(sArea, 0);
					for (TimeWindow tw : tws.getElements()) {
						if (capacities.containsKey(sArea) && capacities.get(sArea).containsKey(tw))
							overallCapacityPerArea.put(sArea,
									overallCapacityPerArea.get(sArea) + capacities.get(sArea).get(tw));
					}
				}
				noAssignmentValue += noAssignmentValueR;
				noAssignmentValueSubArea += noAssignmentValueRPerSubarea.get(subArea);
				// Per tw, determine value after acceptance
				for (TimeWindow tw : timeWindowCandidates) {

					if (!minimumOpportunityCostsPerTimeWindow.containsKey(tw)) {
						minimumOpportunityCostsPerTimeWindow.put(tw, Double.MAX_VALUE);
						maximumOpportunityCostsPerTimeWindow.put(tw, 0.0);
					}
					boolean foundOption = false;
					if (capacities.containsKey(subArea) && capacities.get(subArea).containsKey(tw)
							&& capacities.get(subArea).get(tw) > 0) {
						foundOption = true;
						// Opportunity costs in current area
						double afterTwValue = noAssignmentValueR - noAssignmentValueRPerSubarea.get(subArea)
								+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, subArea, t - 1,
										capacities, maxAcceptablePerTwOverSubAreas, tw, maximumArrivalsPerDemandSegment,
										maximumNeighborArrivalsPerDemandSegment,
										maximumValuePerDeliveryArea.get(area.getId()),
										minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
										daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
										considerLeftOverPenalty, demandSegments, objectiveSpecificValues,
										maximumRevenueValue, maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
										minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
										considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
										timeWindowInputMapper, arrivalProbability);
						if (!assignmentValuePerTw.containsKey(tw)) {
							assignmentValuePerTw.put(tw, afterTwValue);
							numberFeasibleRoutings.put(tw, 1);
						} else {
							assignmentValuePerTw.put(tw, assignmentValuePerTw.get(tw) + afterTwValue);
							numberFeasibleRoutings.put(tw, numberFeasibleRoutings.get(tw) + 1);
						}
						double oppCostsR = noAssignmentValueR - afterTwValue;
						if (minimumOpportunityCostsPerTimeWindow.get(tw) > oppCostsR) {
							minimumOpportunityCostsPerTimeWindow.put(tw, oppCostsR);
						}
						if (maximumOpportunityCostsPerTimeWindow.get(tw) < oppCostsR) {
							maximumOpportunityCostsPerTimeWindow.put(tw, oppCostsR);
						}
					} else if (thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.containsKey(tw.getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.get(tw.getId()) > 0) {
						foundOption = true;
						// Find area with highest left-over value for this time
						// window
						double highestLeftOverValue = -1.0 * Double.MAX_VALUE;
						DeliveryArea areaWithHighestLeftOverValue = null;
						for (DeliveryArea neighbor : neighbors.get(subArea)) {
							if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
									&& capacities.get(neighbor).get(tw) > 0) {
								double value = noAssignmentValueR - noAssignmentValueRPerSubarea.get(neighbor)
										+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
												neighbor, t - 1, capacities, maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

								// Best result?
								if (value > highestLeftOverValue) {
									areaWithHighestLeftOverValue = neighbor;
									highestLeftOverValue = value;
								}
							}
						}
						// If there is an area to steal from, then buffer the
						// resulting left-over value
						// (to see later if it is higher than the average and
						// the
						// routing should further be accepted)
						if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
							stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
							stealingValuePerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());
							stealingOppCostPerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());
							assignmentValuePerStealingTw.put(tw, 0.0);
						}
						if (areaWithHighestLeftOverValue != null) {
							stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithHighestLeftOverValue);
							stealingValuePerTimeWindowAndRouting.get(tw).put(r, highestLeftOverValue);
							stealingOppCostPerTimeWindowAndRouting.get(tw).put(r,
									noAssignmentValueR - highestLeftOverValue);
							assignmentValuePerStealingTw.put(tw,
									assignmentValuePerStealingTw.get(tw) + highestLeftOverValue);
						}

					} else if (thefting && theftingTw) {
						// Does a neighbor tw have cap or has it already
						// accepted?
						boolean nTwAccepted = false;
						for (TimeWindow twN : neighborsTw.get(tw)) {


							if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.containsKey(twN.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.get(twN.getId()) > 0) {
								nTwAccepted = true;
								break;
							}
						}

						if (nTwAccepted) {
							// Find area with highest left-over value for this
							// time
							// window
							double highestLeftOverValue = -1.0 * Double.MAX_VALUE;
							DeliveryArea areaWithHighestLeftOverValue = null;
							for (DeliveryArea neighbor : neighbors.get(subArea)) {
								if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
										&& capacities.get(neighbor).get(tw) > 0) {
									double value = noAssignmentValueR - noAssignmentValueRPerSubarea.get(neighbor)
											+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
													neighbor, t - 1, capacities, maxAcceptablePerTwOverSubAreas, tw,
													maximumArrivalsPerDemandSegment,
													maximumNeighborArrivalsPerDemandSegment,
													maximumValuePerDeliveryArea.get(area.getId()),
													minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
													daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
													arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
													demandSegments, objectiveSpecificValues, maximumRevenueValue,
													maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
													minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
													considerConstant, considerNeighborDemand, segmentInputMapper,
													neighborSegmentInputMapper, timeWindowInputMapper,
													arrivalProbability);

									// Best result?
									if (value > highestLeftOverValue) {
										areaWithHighestLeftOverValue = neighbor;
										highestLeftOverValue = value;
									}
								}
							}
							// If there is an area to steal from, then buffer
							// the
							// resulting left-over value
							// (to see later if it is higher than the average
							// and
							// the
							// routing should further be accepted)
							if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
								stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
								stealingValuePerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());
								stealingOppCostPerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());
								assignmentValuePerStealingTw.put(tw, 0.0);
							}
							if (areaWithHighestLeftOverValue != null) {
								foundOption = true;
								stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithHighestLeftOverValue);
								stealingValuePerTimeWindowAndRouting.get(tw).put(r, highestLeftOverValue);
								stealingOppCostPerTimeWindowAndRouting.get(tw).put(r,
										noAssignmentValueR - highestLeftOverValue);
								assignmentValuePerStealingTw.put(tw,
										assignmentValuePerStealingTw.get(tw) + highestLeftOverValue);
							}
						}

					}

					if (!foundOption && theftingAdvanced && numberOfTops < 2) {

						// Find area with highest left-over value for this time
						// window
						double highestLeftOverValue = -1.0 * Double.MAX_VALUE;
						ArrayList<DeliveryArea> bestAreas = new ArrayList<DeliveryArea>();

						for (int neighborId = 0; neighborId < neighbors.get(subArea).size(); neighborId++) {

							DeliveryArea neighbor = neighbors.get(subArea).get(neighborId);
							double maximumMultiplier;
							if (!maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(neighbor).containsKey(tw)) {
								double amount = 0;
								maximumMultiplier = 0;
								for (TimeWindow twU : maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
										.get(neighbor).keySet()) {
									for (TimeWindow twL : oldToNewTimeWindowMapping.get(twU)) {
										if (twL.getId() == tw.getId()) {
											amount++;
											maximumMultiplier += maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
													.get(neighbor).get(twU);
											break;
										}
									}
								}
								maximumMultiplier = maximumMultiplier / amount;

							} else {
								maximumMultiplier = maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(neighbor)
										.get(tw);
							}

							if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
									&& capacities.get(neighbor).get(tw) > 1
									&& ((arrivalProbability * t * daWeightsLower.get(neighbor) < overallCapacityPerArea
											.get(neighbor) && arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumMultiplier < capacities.get(neighbor).get(tw))
											|| (arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumMultiplier < capacities.get(neighbor).get(tw) - 1))) {
								double value = noAssignmentValueR - noAssignmentValueRPerSubarea.get(neighbor)
										+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
												neighbor, t - 1, capacities, maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), true, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

								if (value > highestLeftOverValue) {
									bestAreas.clear();
									bestAreas.add(neighbor);
									highestLeftOverValue = value;
								}

							}

							if ((neighborId < neighbors.get(subArea).size() - 1) && capacities.containsKey(neighbor)
									&& capacities.get(neighbor).containsKey(tw) && capacities.get(neighbor).get(tw) > 0
									&& ((arrivalProbability * t * daWeightsLower.get(neighbor) < overallCapacityPerArea
											.get(neighbor)
											&& arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumMultiplier < capacities.get(neighbor).get(tw) + 1)
											|| (arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumMultiplier < capacities.get(neighbor).get(tw)))) {
								double value = noAssignmentValueR - noAssignmentValueRPerSubarea.get(neighbor)
										+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
												neighbor, t - 1, capacities, maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

								double bestCombination = -1.0 * Double.MAX_VALUE;
								DeliveryArea bestSecondNeighbor = null;
								for (int neighborId2 = neighborId + 1; neighborId2 < neighbors.get(subArea)
										.size(); neighborId2++) {

									DeliveryArea neighbor2 = neighbors.get(subArea).get(neighborId2);

									if (!maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow.get(neighbor2)
											.containsKey(tw)) {
										double amount = 0;
										maximumMultiplier = 0;
										for (TimeWindow twU : maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
												.get(neighbor).keySet()) {
											for (TimeWindow twL : oldToNewTimeWindowMapping.get(twU)) {
												if (twL.getId() == tw.getId()) {
													amount++;
													maximumMultiplier += maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
															.get(neighbor).get(twU);
													break;
												}
											}
										}
										maximumMultiplier = maximumMultiplier / amount;

									} else {
										maximumMultiplier = maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
												.get(neighbor).get(tw);
									}
									if (capacities.containsKey(neighbor2) && capacities.get(neighbor2).containsKey(tw)
											&& capacities.get(neighbor2).get(tw) > 0
											&& ((arrivalProbability * t
													* daWeightsLower.get(neighbor2) < overallCapacityPerArea
															.get(neighbor2)
													&& arrivalProbability * t * daWeightsLower.get(neighbor2)
															* maximumMultiplier < capacities.get(neighbor2).get(tw) + 1)
													|| (arrivalProbability * t * daWeightsLower.get(neighbor2)
															* maximumMultiplier < capacities.get(neighbor2).get(tw)))) {
										double valueTwice = value - noAssignmentValueRPerSubarea.get(neighbor2)
												+ ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
														neighbor2, t - 1, capacities, maxAcceptablePerTwOverSubAreas,
														tw, maximumArrivalsPerDemandSegment,
														maximumNeighborArrivalsPerDemandSegment,
														maximumValuePerDeliveryArea.get(area.getId()),
														minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
														daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
														arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
														demandSegments, objectiveSpecificValues, maximumRevenueValue,
														maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
														minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
														considerConstant, considerNeighborDemand, segmentInputMapper,
														neighborSegmentInputMapper, timeWindowInputMapper,
														arrivalProbability);
										if (valueTwice > bestCombination) {
											bestCombination = valueTwice;
											bestSecondNeighbor = neighbor2;
										}
									}
								}

								if (bestSecondNeighbor != null && bestCombination > highestLeftOverValue) {
									bestAreas.clear();
									bestAreas.add(neighbor);
									bestAreas.add(bestSecondNeighbor);
									highestLeftOverValue = bestCombination;
								}
							}

						}
						// If there is an area to steal from, then buffer the
						// resulting left-over value
						// (to see later if it is higher than the average and
						// the
						// routing should further be accepted)
						if (!advancedStealingAreasPerTimeWindowAndRouting.containsKey(tw)) {
							advancedStealingAreasPerTimeWindowAndRouting.put(tw,
									new HashMap<Routing, ArrayList<DeliveryArea>>());
							advancedStealingValuePerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());
							assignmentValuePerStealingTwAdvanced.put(tw, 0.0);
						}
						if (bestAreas.size() > 0) {
							advancedStealingAreasPerTimeWindowAndRouting.get(tw).put(r, bestAreas);
							advancedStealingValuePerTimeWindowAndRouting.get(tw).put(r, highestLeftOverValue);
							assignmentValuePerStealingTwAdvanced.put(tw,
									assignmentValuePerStealingTwAdvanced.get(tw) + highestLeftOverValue);
						}
					}
				}
			}
			noAssignmentValue = noAssignmentValue / aggregateInformationNo.keySet().size();
			noAssignmentValueSubArea = noAssignmentValueSubArea / aggregateInformationNo.keySet().size();
			for (TimeWindow tw : assignmentValuePerTw.keySet()) {

				double cumSumAssignmentValue = assignmentValuePerTw.get(tw);
				double divisor = numberFeasibleRoutings.get(tw);
				double assignmentValue = cumSumAssignmentValue / divisor;
				// Is there stealing for this tw?
				if (stealingValuePerTimeWindowAndRouting.containsKey(tw)) {
					double assignmentValueSteals = 0.0;
					// Go through all relevant routings and check if they should
					// stay after assignment
					for (Routing r : stealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
						if (stealingValuePerTimeWindowAndRouting.get(tw).get(r) < assignmentValue) {
							stealingAreaPerTimeWindowAndRouting.get(tw).remove(r);
						} else {
							assignmentValueSteals += stealingValuePerTimeWindowAndRouting.get(tw).get(r);

							if (minimumOpportunityCostsPerTimeWindow.get(tw) > stealingOppCostPerTimeWindowAndRouting
									.get(tw).get(r)) {
								minimumOpportunityCostsPerTimeWindow.put(tw,
										stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
							}
							if (maximumOpportunityCostsPerTimeWindow.get(tw) < stealingOppCostPerTimeWindowAndRouting
									.get(tw).get(r)) {
								maximumOpportunityCostsPerTimeWindow.put(tw,
										stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
							}

						}
					}
					if (stealingAreaPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {
						cumSumAssignmentValue = cumSumAssignmentValue + assignmentValueSteals;
						divisor = divisor + stealingAreaPerTimeWindowAndRouting.get(tw).keySet().size();
					}

				}

				// Is there advanced stealing for this tw?
				if (advancedStealingValuePerTimeWindowAndRouting.containsKey(tw)) {
					double assignmentValueSteals = 0.0;
					// Go through all relevant routings and check if they should
					// stay after assignment
					for (Routing r : advancedStealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
						if (advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r) < assignmentValue) {
							advancedStealingAreasPerTimeWindowAndRouting.get(tw).remove(r);
						} else {
							assignmentValueSteals += advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r);
						}
					}
					if (advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {
						cumSumAssignmentValue = cumSumAssignmentValue + assignmentValueSteals;
						divisor = divisor + advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size();
					}

				}
				assignmentValue = cumSumAssignmentValue / divisor;

				opportunityCostsPerTw.put(tw, (noAssignmentValue - assignmentValue) * discountingFactorProbability);

			}

			// If there are time windows with no originally feasible routing but
			// with theft,
			// then consider all routings that are feasible after theft
			for (TimeWindow tw : timeWindowCandidatesOrienteeringThefting) {

				double assignmentValue = assignmentValuePerStealingTw.get(tw);
				double divisor = stealingAreaPerTimeWindowAndRouting.get(tw).keySet().size();

				// Update minimum and maximum opportunity costs
				for (Routing r : stealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
					if (minimumOpportunityCostsPerTimeWindow.get(tw) > stealingOppCostPerTimeWindowAndRouting.get(tw)
							.get(r)) {
						minimumOpportunityCostsPerTimeWindow.put(tw,
								stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
					}
					if (maximumOpportunityCostsPerTimeWindow.get(tw) < stealingOppCostPerTimeWindowAndRouting.get(tw)
							.get(r)) {
						maximumOpportunityCostsPerTimeWindow.put(tw,
								stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
					}
				}
				// Is there advanced stealing for this tw?
				if (advancedStealingValuePerTimeWindowAndRouting.containsKey(tw)) {
					double assignmentValueSteals = 0.0;
					// Go through all relevant routings and check if they should
					// stay after assignment
					for (Routing r : advancedStealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
						if (advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r) < assignmentValue / divisor) {
							advancedStealingAreasPerTimeWindowAndRouting.get(tw).remove(r);
						} else {
							assignmentValueSteals += advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r);
						}
					}
					if (advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {
						assignmentValue = assignmentValue + assignmentValueSteals;
						divisor = divisor + advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size();
					}

				}
				opportunityCostsPerTw.put(tw,
						(noAssignmentValue - assignmentValue / divisor) * discountingFactorProbability);
			}

			// If there are time windows that only are feasible with neighbor tw
			// accepted, consider all routings that are feasible after theft. If
			// there is not really an option,
			// try it with advanced stealing
			for (TimeWindow tw : timeWindowCandidatesOrienteeringTheftingTw) {
				// Potential candidate really one?
				if (stealingAreaPerTimeWindowAndRouting.containsKey(tw)
						&& stealingAreaPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {
					double assignmentValue = assignmentValuePerStealingTw.get(tw);
					double divisor = stealingAreaPerTimeWindowAndRouting.get(tw).keySet().size();

					// Update minimum and maximum opportunity costs
					for (Routing r : stealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
						if (minimumOpportunityCostsPerTimeWindow.get(tw) > stealingOppCostPerTimeWindowAndRouting
								.get(tw).get(r)) {
							minimumOpportunityCostsPerTimeWindow.put(tw,
									stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
						}
						if (maximumOpportunityCostsPerTimeWindow.get(tw) < stealingOppCostPerTimeWindowAndRouting
								.get(tw).get(r)) {
							maximumOpportunityCostsPerTimeWindow.put(tw,
									stealingOppCostPerTimeWindowAndRouting.get(tw).get(r));
						}
					}

					// Is there advanced stealing for this tw?
					if (advancedStealingValuePerTimeWindowAndRouting.containsKey(tw)) {
						double assignmentValueSteals = 0.0;
						// Go through all relevant routings and check if they
						// should
						// stay after assignment
						for (Routing r : advancedStealingValuePerTimeWindowAndRouting.get(tw).keySet()) {
							if (advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r) < assignmentValue
									/ divisor) {
								advancedStealingAreasPerTimeWindowAndRouting.get(tw).remove(r);
							} else {
								assignmentValueSteals += advancedStealingValuePerTimeWindowAndRouting.get(tw).get(r);
							}
						}
						if (advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {
							assignmentValue = assignmentValue + assignmentValueSteals;
							divisor = divisor + advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size();
						}

					}

					opportunityCostsPerTw.put(tw,
							(noAssignmentValue - assignmentValue / divisor) * discountingFactorProbability);
				} else {
					potentialTimeWindowCandidatesOrienteeringTheftingAdvanced.add(tw);
				}
			}
			// If there are time windows with no originally feasible routing but
			// with advanced theft,
			// then consider all routings that are feasible after theft
			for (TimeWindow tw : potentialTimeWindowCandidatesOrienteeringTheftingAdvanced) {

				// Potential candidate really one?
				if (advancedStealingAreasPerTimeWindowAndRouting.containsKey(tw)
						&& advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size() > 0) {

					opportunityCostsPerTw.put(tw,
							(noAssignmentValue - assignmentValuePerStealingTwAdvanced.get(tw)
									/ advancedStealingAreasPerTimeWindowAndRouting.get(tw).keySet().size())
									* discountingFactorProbability);
				}

			}

			// Update opp costs if they are above or below min/max opp costs
			if (numberOfTops > 1) {
				for (TimeWindow tw : opportunityCostsPerTw.keySet()) {
					if (opportunityCostsPerTw.get(tw) < minimumOpportunityCostsPerTimeWindow.get(tw)) {
						opportunityCostsPerTw.put(tw, minimumOpportunityCostsPerTimeWindow.get(tw));
					}
					if (opportunityCostsPerTw.get(tw) > maximumOpportunityCostsPerTimeWindow.get(tw)) {
						opportunityCostsPerTw.put(tw, maximumOpportunityCostsPerTimeWindow.get(tw));
					}
				}

			}

		} else {

			HashMap<TimeWindow, Integer> numberFeasiblePerTimeWindow = new HashMap<TimeWindow, Integer>();
			HashMap<TimeWindow, HashMap<Routing, Double>> stealingOppCostPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, Double>>();
			HashMap<DeliveryArea, Integer> overallCapacityPerArea = new HashMap<DeliveryArea, Integer>();

			for (Routing r : aggregateInformationNo.keySet()) {

				HashMap<DeliveryArea, Double> noAssignmentValuePerNeighbor = new HashMap<DeliveryArea, Double>();
				noAssignmentValuePerNeighbor.put(subArea,
						ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, subArea, t - 1,
								aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, null,
								maximumArrivalsPerDemandSegment, maximumNeighborArrivalsPerDemandSegment,
								maximumValuePerDeliveryArea.get(area.getId()),
								minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
								daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
								considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
								maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
								minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
								considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
								timeWindowInputMapper, arrivalProbability));

				for (TimeWindow tw : timeWindowCandidates) {

					// Feasible?
					boolean foundOption = false;
					if (aggregateInformationNo.get(r).containsKey(subArea)
							&& aggregateInformationNo.get(r).get(subArea).containsKey(tw)
							&& aggregateInformationNo.get(r).get(subArea).get(tw) > 0) {
						foundOption = true;
						double assignmentValue = ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
								subArea, t - 1, aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, tw,
								maximumArrivalsPerDemandSegment, maximumNeighborArrivalsPerDemandSegment,
								maximumValuePerDeliveryArea.get(area.getId()),
								minimumValuePerDeliveryArea.get(area.getId()), false, dsw, daWeightsLower,
								daSegmentWeightingsLower, neighbors, tws, arrivalProcessId, ANNPerDeliveryArea,
								considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
								maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
								minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false, considerConstant,
								considerNeighborDemand, segmentInputMapper, neighborSegmentInputMapper,
								timeWindowInputMapper, arrivalProbability);

						if (!opportunityCostsPerTw.containsKey(tw)) {
							opportunityCostsPerTw.put(tw, noAssignmentValuePerNeighbor.get(subArea) - assignmentValue);
							numberFeasiblePerTimeWindow.put(tw, 1);
						} else {
							opportunityCostsPerTw.put(tw, opportunityCostsPerTw.get(tw)
									+ noAssignmentValuePerNeighbor.get(subArea) - assignmentValue);
							numberFeasiblePerTimeWindow.put(tw, numberFeasiblePerTimeWindow.get(tw) + 1);
						}

					} else if (thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.containsKey(tw.getId())
							&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
									.get(tw.getId()) > 0) {
						// Simple stealing?
						foundOption = true;
						Double lowestStealingOC = Double.MAX_VALUE;
						DeliveryArea bestNeighbor = null;
						for (DeliveryArea nArea : neighbors.get(subArea)) {
							if (!noAssignmentValuePerNeighbor.containsKey(nArea)) {
								noAssignmentValuePerNeighbor.put(nArea,
										ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, nArea,
												t - 1, aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas,
												null, maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability));
							}

							if (aggregateInformationNo.get(r).containsKey(nArea)
									&& aggregateInformationNo.get(r).get(nArea).containsKey(tw)
									&& aggregateInformationNo.get(r).get(nArea).get(tw) > 0) {
								double assignmentValue = ADPWithOrienteeringANN
										.determineValueFunctionApproximationValue(area, nArea, t - 1,
												aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);
								if (noAssignmentValuePerNeighbor.get(nArea) - assignmentValue < lowestStealingOC) {
									bestNeighbor = nArea;
									lowestStealingOC = noAssignmentValuePerNeighbor.get(nArea) - assignmentValue;
								}
							}
						}

						if (bestNeighbor != null) {
							if (!opportunityCostsPerTw.containsKey(tw)) {
								opportunityCostsPerTw.put(tw, lowestStealingOC);
								numberFeasiblePerTimeWindow.put(tw, 1);
							} else {
								opportunityCostsPerTw.put(tw, opportunityCostsPerTw.get(tw) + lowestStealingOC);
								numberFeasiblePerTimeWindow.put(tw, numberFeasiblePerTimeWindow.get(tw) + 1);
							}

							if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
								stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
								stealingOppCostPerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());

							}

							stealingAreaPerTimeWindowAndRouting.get(tw).put(r, bestNeighbor);
							stealingOppCostPerTimeWindowAndRouting.get(tw).put(r, lowestStealingOC);

						}

					} else if (thefting && theftingTw) {
						// Does a neighbor tw have cap or has it already
						// accepted?
						boolean nTwAccepted = false;
						for (TimeWindow twN : neighborsTw.get(tw)) {


							if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.containsKey(twN.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.get(twN.getId()) > 0) {
								nTwAccepted = true;
								break;
							}
						}

						if (nTwAccepted) {
							Double lowestStealingOC = Double.MAX_VALUE;
							DeliveryArea bestNeighbor = null;
							for (DeliveryArea nArea : neighbors.get(subArea)) {
								if (!noAssignmentValuePerNeighbor.containsKey(nArea)) {
									noAssignmentValuePerNeighbor.put(nArea,
											ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, nArea,
													t - 1, aggregateInformationNo.get(r),
													maxAcceptablePerTwOverSubAreas, null,
													maximumArrivalsPerDemandSegment,
													maximumNeighborArrivalsPerDemandSegment,
													maximumValuePerDeliveryArea.get(area.getId()),
													minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
													daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
													arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
													demandSegments, objectiveSpecificValues, maximumRevenueValue,
													maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
													minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
													considerConstant, considerNeighborDemand, segmentInputMapper,
													neighborSegmentInputMapper, timeWindowInputMapper,
													arrivalProbability));
								}

								if (aggregateInformationNo.get(r).containsKey(nArea)
										&& aggregateInformationNo.get(r).get(nArea).containsKey(tw)
										&& aggregateInformationNo.get(r).get(nArea).get(tw) > 0) {
									double assignmentValue = ADPWithOrienteeringANN
											.determineValueFunctionApproximationValue(area, nArea, t - 1,
													aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, tw,
													maximumArrivalsPerDemandSegment,
													maximumNeighborArrivalsPerDemandSegment,
													maximumValuePerDeliveryArea.get(area.getId()),
													minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
													daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
													arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
													demandSegments, objectiveSpecificValues, maximumRevenueValue,
													maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
													minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
													considerConstant, considerNeighborDemand, segmentInputMapper,
													neighborSegmentInputMapper, timeWindowInputMapper,
													arrivalProbability);
									if (noAssignmentValuePerNeighbor.get(nArea) - assignmentValue < lowestStealingOC) {
										bestNeighbor = nArea;
										lowestStealingOC = noAssignmentValuePerNeighbor.get(nArea) - assignmentValue;
									}
								}
							}

							if (bestNeighbor != null) {
								foundOption = true;
								if (!opportunityCostsPerTw.containsKey(tw)) {
									opportunityCostsPerTw.put(tw, lowestStealingOC);
									numberFeasiblePerTimeWindow.put(tw, 1);
								} else {
									opportunityCostsPerTw.put(tw, opportunityCostsPerTw.get(tw) + lowestStealingOC);
									numberFeasiblePerTimeWindow.put(tw, numberFeasiblePerTimeWindow.get(tw) + 1);
								}

								if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
									stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
									stealingOppCostPerTimeWindowAndRouting.put(tw, new HashMap<Routing, Double>());

								}

								stealingAreaPerTimeWindowAndRouting.get(tw).put(r, bestNeighbor);
								stealingOppCostPerTimeWindowAndRouting.get(tw).put(r, lowestStealingOC);

							}
						}
					}

					if (!foundOption && theftingAdvanced && numberOfTops < 2) {

						double lowestOC = Double.MAX_VALUE;
						ArrayList<DeliveryArea> bestAreas = new ArrayList<DeliveryArea>();

						for (int neighborId = 0; neighborId < neighbors.get(subArea).size(); neighborId++) {

							DeliveryArea neighbor = neighbors.get(subArea).get(neighborId);

							if (!overallCapacityPerArea.containsKey(neighbor)) {
								int cap = 0;
								for (TimeWindow cTw : tws.getElements()) {
									if (aggregateInformationNo.get(r).containsKey(neighbor)
											&& aggregateInformationNo.get(r).get(neighbor).containsKey(cTw))
										cap += aggregateInformationNo.get(r).get(neighbor).get(cTw);
								}
								overallCapacityPerArea.put(neighbor, cap);
							}

							if (!noAssignmentValuePerNeighbor.containsKey(neighbor)) {
								noAssignmentValuePerNeighbor.put(neighbor,
										ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, neighbor,
												t - 1, aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas,
												null, maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability));
							}

							if (aggregateInformationNo.get(r).containsKey(neighbor)
									&& aggregateInformationNo.get(r).get(neighbor).containsKey(tw)
									&& aggregateInformationNo.get(r).get(neighbor).get(tw) > 1
									&& ((arrivalProbability * t * daWeightsLower.get(neighbor) < overallCapacityPerArea
											.get(neighbor)
											&& arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
															.get(neighbor).get(tw) < aggregateInformationNo.get(r)
																	.get(neighbor).get(tw))
											|| (arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
															.get(neighbor).get(tw) < aggregateInformationNo.get(r)
																	.get(neighbor).get(tw) - 1))) {

								double assignmentValue = ADPWithOrienteeringANN
										.determineValueFunctionApproximationValue(area, neighbor, t - 1,
												aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), true, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

								if (noAssignmentValuePerNeighbor.get(neighbor) - assignmentValue < lowestOC) {
									bestAreas.clear();
									bestAreas.add(neighbor);
									lowestOC = noAssignmentValuePerNeighbor.get(neighbor) - assignmentValue;
								}

							}

							if ((neighborId < neighbors.get(subArea).size() - 1)
									&& aggregateInformationNo.get(r).containsKey(neighbor)
									&& aggregateInformationNo.get(r).get(neighbor).containsKey(tw)
									&& aggregateInformationNo.get(r).get(neighbor).get(tw) > 0
									&& ((arrivalProbability * t * daWeightsLower.get(neighbor) < overallCapacityPerArea
											.get(neighbor)
											&& arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
															.get(neighbor).get(tw) < aggregateInformationNo.get(r)
																	.get(neighbor).get(tw) + 1)
											|| (arrivalProbability * t * daWeightsLower.get(neighbor)
													* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
															.get(neighbor).get(tw) < aggregateInformationNo.get(r)
																	.get(neighbor).get(tw)))) {
								double assignmentValue = ADPWithOrienteeringANN
										.determineValueFunctionApproximationValue(area, neighbor, t - 1,
												aggregateInformationNo.get(r), maxAcceptablePerTwOverSubAreas, tw,
												maximumArrivalsPerDemandSegment,
												maximumNeighborArrivalsPerDemandSegment,
												maximumValuePerDeliveryArea.get(area.getId()),
												minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
												daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
												arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
												demandSegments, objectiveSpecificValues, maximumRevenueValue,
												maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
												minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
												considerConstant, considerNeighborDemand, segmentInputMapper,
												neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);

								double bestCombination = Double.MAX_VALUE;
								DeliveryArea bestSecondNeighbor = null;
								for (int neighborId2 = neighborId + 1; neighborId2 < neighbors.get(subArea)
										.size(); neighborId2++) {
									DeliveryArea neighbor2 = neighbors.get(subArea).get(neighborId2);

									if (!overallCapacityPerArea.containsKey(neighbor2)) {
										int cap = 0;
										for (TimeWindow cTw : tws.getElements()) {
											if (aggregateInformationNo.get(r).containsKey(neighbor2)
													&& aggregateInformationNo.get(r).get(neighbor2).containsKey(cTw))
												cap += aggregateInformationNo.get(r).get(neighbor2).get(cTw);
										}
										overallCapacityPerArea.put(neighbor2, cap);
									}

									if (aggregateInformationNo.get(r).containsKey(neighbor2)
											&& aggregateInformationNo.get(r).get(neighbor2).containsKey(tw)
											&& aggregateInformationNo.get(r).get(neighbor2).get(tw) > 0
											&& ((arrivalProbability * t
													* daWeightsLower.get(neighbor2) < overallCapacityPerArea
															.get(neighbor2)
													&& arrivalProbability * t * daWeightsLower.get(neighbor2)
															* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
																	.get(neighbor2).get(tw) < aggregateInformationNo
																			.get(r).get(neighbor2).get(tw) + 1)
													|| (arrivalProbability * t * daWeightsLower.get(neighbor2)
															* maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow
																	.get(neighbor2).get(tw) < aggregateInformationNo
																			.get(r).get(neighbor2).get(tw)))) {
										if (!noAssignmentValuePerNeighbor.containsKey(neighbor2)) {
											noAssignmentValuePerNeighbor.put(neighbor2,
													ADPWithOrienteeringANN.determineValueFunctionApproximationValue(
															area, neighbor2, t - 1, aggregateInformationNo.get(r),
															maxAcceptablePerTwOverSubAreas, null,
															maximumArrivalsPerDemandSegment,
															maximumNeighborArrivalsPerDemandSegment,
															maximumValuePerDeliveryArea.get(area.getId()),
															minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
															daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
															arrivalProcessId, ANNPerDeliveryArea,
															considerLeftOverPenalty, demandSegments,
															objectiveSpecificValues, maximumRevenueValue,
															maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
															minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
															false, considerConstant, considerNeighborDemand,
															segmentInputMapper, neighborSegmentInputMapper,
															timeWindowInputMapper, arrivalProbability));
										}
										double valueTwice = noAssignmentValuePerNeighbor.get(neighbor) - assignmentValue
												+ noAssignmentValuePerNeighbor.get(neighbor2)
												- ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area,
														neighbor2, t - 1, aggregateInformationNo.get(r),
														maxAcceptablePerTwOverSubAreas, tw,
														maximumArrivalsPerDemandSegment,
														maximumNeighborArrivalsPerDemandSegment,
														maximumValuePerDeliveryArea.get(area.getId()),
														minimumValuePerDeliveryArea.get(area.getId()), false, dsw,
														daWeightsLower, daSegmentWeightingsLower, neighbors, tws,
														arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty,
														demandSegments, objectiveSpecificValues, maximumRevenueValue,
														maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
														minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, false,
														considerConstant, considerNeighborDemand, segmentInputMapper,
														neighborSegmentInputMapper, timeWindowInputMapper,
														arrivalProbability);
										if (valueTwice < bestCombination) {
											bestCombination = valueTwice;
											bestSecondNeighbor = neighbor2;
										}
									}
								}

								if (bestSecondNeighbor != null && bestCombination < lowestOC) {
									bestAreas.clear();
									bestAreas.add(neighbor);
									bestAreas.add(bestSecondNeighbor);
									lowestOC = bestCombination;
								}
							}

						}
						// If there is an area to steal from, then buffer the
						// resulting left-over value
						// (to see later if it is higher than the average and
						// the
						// routing should further be accepted)
						if (!advancedStealingAreasPerTimeWindowAndRouting.containsKey(tw)) {
							advancedStealingAreasPerTimeWindowAndRouting.put(tw,
									new HashMap<Routing, ArrayList<DeliveryArea>>());

						}
						if (bestAreas.size() > 0) {
							advancedStealingAreasPerTimeWindowAndRouting.get(tw).put(r, bestAreas);
							if (!opportunityCostsPerTw.containsKey(tw)) {
								opportunityCostsPerTw.put(tw, lowestOC);
								numberFeasiblePerTimeWindow.put(tw, 1);
							} else {
								opportunityCostsPerTw.put(tw, opportunityCostsPerTw.get(tw) + lowestOC);
								numberFeasiblePerTimeWindow.put(tw, numberFeasiblePerTimeWindow.get(tw) + 1);
							}
						}
					}
				}
			}

			// Determine average opportunity costs
			for (TimeWindow tw : opportunityCostsPerTw.keySet()) {
				opportunityCostsPerTw.put(tw, opportunityCostsPerTw.get(tw) / numberFeasiblePerTimeWindow.get(tw)
						* discountingFactorProbability);
			}

			// Take out routings that require stealing and have higher opp than
			// average
			for (TimeWindow tw : stealingOppCostPerTimeWindowAndRouting.keySet()) {
				for (Routing r : stealingOppCostPerTimeWindowAndRouting.get(tw).keySet()) {
					if (stealingOppCostPerTimeWindowAndRouting.get(tw).get(r)
							* discountingFactorProbability > opportunityCostsPerTw.get(tw)) {
						stealingAreaPerTimeWindowAndRouting.get(tw).remove(r);
					}
				}
			}

		}
	}

	private void updateANNTD(DeliveryArea area, DeliveryArea subArea, int t, double expectedArrivalsOverall,
			HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLower,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas, HashMap<DemandSegment, Double> maximumArrivals,
			HashMap<DemandSegment, Double> maximumArrivalsNeighbors,
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> remainingCapacityPerSubAreaAndTw, double newValue,
			boolean arrival) {

		// Determine old value (in t), only use ANN and no additional logic
		ADPWithOrienteeringANN.determineValueFunctionApproximationValue(area, subArea, t,
				remainingCapacityPerSubAreaAndTw, maxAcceptablePerTwOverSubAreas, null, maximumArrivals,
				maximumArrivalsNeighbors, maximumValuePerDeliveryArea.get(area.getId()),
				minimumValuePerDeliveryArea.get(area.getId()), false, this.demandSegmentWeighting, daWeightsLower,
				daSegmentWeightingsLower, neighbors, this.timeWindowSet, arrivalProcessId, ANNPerDeliveryArea,
				considerLeftOverPenalty, demandSegments, objectiveSpecificValues, maximumRevenueValue,
				maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
				minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, true, this.considerConstant,
				this.considerDemandNeighbors, segmentInputMapper, neighborSegmentInputMapper, timeWindowInputMapper,
				arrivalProbability);

		double actualV = (newValue + this.minimumValuePerDeliveryArea.get(area.getId()))
				/ (this.maximumValuePerDeliveryArea.get(area.getId())
						+ this.minimumValuePerDeliveryArea.get(area.getId()));
		if (this.hTanActivation)
			actualV = actualV * 2.0 - 1.0;
		this.ANNPerDeliveryArea.get(area.getId()).calcError(new double[] { actualV });
		this.ANNPerDeliveryArea.get(area.getId()).learn();

	}

	private void updateANN(DeliveryArea area,
			ArrayList<Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>>> acceptedOrdersPerORS,
			HashMap<Integer, Integer> maxAcceptablePerTwOverSubAreas,
			HashMap<DemandSegment, Double> maximumArrivalsPerDemandSegment,
			HashMap<DemandSegment, Double> maximumArrivalsNeighborPerDemandSegment) {

		ArrayList<Pair<double[], Double>> inputOutputList = new ArrayList<Pair<double[], Double>>();

		// Collect training data for the respective delivery areas
		// (specific delivery area not necessary, but lower level)
		double maximumValue = 0.0;
		HashMap<Integer, Pair<Integer, Double>> overallValueAndCountPerRouting = new HashMap<Integer, Pair<Integer, Double>>();
		ArrayList<TimeWindow> relevantTimeWindows;
		if (!this.timeWindowSet.getOverlapping()) {
			relevantTimeWindows = this.timeWindowSet.getElements();
		} else {
			relevantTimeWindows = this.timeWindowSetOverlappingDummy.getElements();
		}
		for (DeliveryArea subArea : area.getSubset().getElements()) {

			// Go through routings and add training data
			int routingId = 0;
			for (Pair<Routing, HashMap<DeliveryArea, ArrayList<Order>>> r : acceptedOrdersPerORS) {
				ArrayList<Order> orders = r.getValue().get(subArea);
				if (orders != null) {
					HashMap<TimeWindow, ArrayList<Integer>> remainingCapacityPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
					HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegment = new HashMap<DemandSegment, ArrayList<Double>>();
					HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegmentNeighbors = new HashMap<DemandSegment, ArrayList<Double>>();

					// Determine feed-forward details
					Collections.sort(orders, new OrderArrivalTimeDescComparator());

					// 1.) First at beginning of order horizon
					int lastT = this.orderHorizonLength + 1;

					for (DemandSegmentWeight dsw : this.demandSegmentWeighting.getWeights()) {

						expectedArrivalsPerDemandSegment.put(dsw.getDemandSegment(), new ArrayList<Double>());
						expectedArrivalsPerDemandSegmentNeighbors.put(dsw.getDemandSegment(), new ArrayList<Double>());

					}

					for (TimeWindow tw : relevantTimeWindows) {
						remainingCapacityPerTimeWindow.put(tw, new ArrayList<Integer>());

						if (this.aggregatedReferenceInformationNo.get(area).get(r.getKey()).containsKey(subArea)
								&& this.aggregatedReferenceInformationNo.get(area).get(r.getKey()).get(subArea)
										.containsKey(tw)) {
							remainingCapacityPerTimeWindow.get(tw).add(this.aggregatedReferenceInformationNo.get(area)
									.get(r.getKey()).get(subArea).get(tw));
						} else {
							remainingCapacityPerTimeWindow.get(tw).add(0);
						}
					}

					// 2.) Actual elements

					for (int i = 0; i < orders.size(); i++) {

						Order order = orders.get(i);
						for (int a = lastT - 1; a >= order.getOrderRequest().getArrivalTime(); a--) {
							for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(subArea).getWeights()) {

								expectedArrivalsPerDemandSegment.get(dsw.getDemandSegment()).add(
										a * arrivalProbability * this.daWeightsLower.get(subArea) * dsw.getWeight());

								double arrNeighbors = 0.0;
								for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
									arrNeighbors += this.daWeightsLower.get(subArea2)
											* this.daSegmentWeightingsLowerHash.get(subArea2)
													.get(dsw.getDemandSegment());

								}
								expectedArrivalsPerDemandSegmentNeighbors.get(dsw.getDemandSegment()).add(arrNeighbors);
							}
						}

						for (TimeWindow tw : relevantTimeWindows) {
							int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
									.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

							for (int a = lastT - 2; a >= order.getOrderRequest().getArrivalTime(); a--) {
								remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
							}

							if (this.timeWindowSet.getOverlapping()) {
								if (tw.getId() == order.getFinalTimeWindowTempId()) {
									newNumberForTimeWindow--;
								}
							} else {
								if (tw.equals(order.getTimeWindowFinal())) {
									newNumberForTimeWindow--;
								}
							}

							remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
						}

						lastT = order.getOrderRequest().getArrivalTime();
					}

					if (lastT > 1) {
						for (int a = lastT - 1; a > 0; a--) {
							for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

								expectedArrivalsPerDemandSegment.get(s)
										.add(a * arrivalProbability * this.daWeightsLower.get(subArea)
												* this.daSegmentWeightingsLowerHash.get(subArea).get(s));

								double arrNeighbors = 0.0;
								for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
									arrNeighbors += this.daWeightsLower.get(subArea2)
											* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

								}
								expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
							}
						}

						for (TimeWindow tw : relevantTimeWindows) {
							int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
									.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

							for (int a = lastT - 2; a >= 0; a--) {
								remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
							}
						}

					}

					// For t=0
					for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(subArea).getWeights()) {

						expectedArrivalsPerDemandSegment.get(dsw.getDemandSegment()).add(0.0);
						expectedArrivalsPerDemandSegmentNeighbors.get(dsw.getDemandSegment()).add(0.0);
					}

					// Determine backward details
					Collections.sort(orders, new OrderArrivalTimeAscComparator());

					ArrayList<Double> accumulatedValues = new ArrayList<Double>();
					double value = 0.0;

					if (this.considerLeftOverPenalty) {
						HashMap<TimeWindow, Integer> reCapPerTw = new HashMap<TimeWindow, Integer>();
						for (TimeWindow tw : remainingCapacityPerTimeWindow.keySet()) {
							reCapPerTw.put(tw, remainingCapacityPerTimeWindow.get(tw)
									.get(remainingCapacityPerTimeWindow.get(tw).size() - 1));
						}
						value = -1.0 * ADPWithOrienteeringANN.calculatePenalty(reCapPerTw, subArea, demandSegments,
								objectiveSpecificValues, maximumRevenueValue, daSegmentWeightingsLowerHash,
								maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
								minimumExpectedMultiplierPerDemandSegmentAndTimeWindow);

					}

					int currentT = 0;
					for (int a = currentT; a < orders.get(0).getOrderRequest().getArrivalTime(); a++) {
						accumulatedValues.add(value);
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
						value = value + CustomerDemandService.calculateOrderValue(order.getOrderRequest(),
								maximumRevenueValue, objectiveSpecificValues);

						while (currentT <= targetT) {

							accumulatedValues.add(value);

							currentT++;
						}

					}

					// Fill up till end of order horizon
					for (int t = currentT; t <= targetT; t++) {
						accumulatedValues.add(value);

					}

					Collections.sort(accumulatedValues, Collections.reverseOrder());

					// Log results
					if (!overallValueAndCountPerRouting.containsKey(routingId)) {
						overallValueAndCountPerRouting.put(routingId, new Pair<Integer, Double>(orders.size(), value));
					} else {
						overallValueAndCountPerRouting.put(routingId,
								new Pair<Integer, Double>(
										overallValueAndCountPerRouting.get(routingId).getKey() + orders.size(),
										overallValueAndCountPerRouting.get(routingId).getValue() + value));
					}

					// Add to training data
					for (int i = 0; i < accumulatedValues.size(); i++) {

						int numberOfVariables = this.demandSegmentWeighting.getWeights().size();
						if (this.timeWindowSet.getOverlapping()) {
							numberOfVariables += this.timeWindowSetOverlappingDummy.getElements().size();
						} else {
							numberOfVariables += this.timeWindowSet.getElements().size();
						}
						if (considerConstant)
							numberOfVariables++;
						if (this.considerDemandNeighbors)
							numberOfVariables += this.demandSegmentWeighting.getWeights().size();
						double[] inputRow = new double[numberOfVariables];

						int currentId = 0;

						for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

							inputRow[currentId++] = expectedArrivalsPerDemandSegment.get(w.getDemandSegment()).get(i)
									/ this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
											.get(w.getDemandSegment());

						}
						if (this.considerDemandNeighbors) {

							for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

								inputRow[currentId++] = expectedArrivalsPerDemandSegmentNeighbors
										.get(w.getDemandSegment()).get(i)
										/ this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
												.get(w.getDemandSegment());

							}

						}

						for (TimeWindow tw : relevantTimeWindows) {
							inputRow[currentId++] = ((double) remainingCapacityPerTimeWindow.get(tw).get(i))
									/ ((double) maxAcceptablePerTwOverSubAreas.get(tw.getId()));
						}

						if (this.considerConstant) {
							inputRow[currentId] = 1.0;
						}

						inputOutputList.add(new Pair<double[], Double>(inputRow, accumulatedValues.get(i)));

						if (accumulatedValues.get(i) > maximumValue) {
							maximumValue = accumulatedValues.get(i);
						}
					}
				}

				routingId++;
			}
		}

		// Calculate average value and number of accepted orders
		double averageValue = 0.0;
		double averageAccepted = 0.0;
		for (Integer routingId : overallValueAndCountPerRouting.keySet()) {
			averageValue += overallValueAndCountPerRouting.get(routingId).getValue();
			averageAccepted += overallValueAndCountPerRouting.get(routingId).getKey();
		}
		averageValue = averageValue / (double) overallValueAndCountPerRouting.keySet().size();
		averageAccepted = averageAccepted / (double) overallValueAndCountPerRouting.keySet().size();

		if (maximumValue > this.maximumValuePerDeliveryArea.get(area.getId())) {
			System.out.println("Problem with maximum value");
		}

		Collections.shuffle(inputOutputList);

		// System.out.println("Error for training set: " + Math.sqrt(accumError
		// / inputOutputList.size()));
		// Train network with the available data
		for (int j = 0; j < inputOutputList.size(); j++) {

			this.ANNPerDeliveryArea.get(area.getId()).computeOutputs(inputOutputList.get(j).getKey());

			double actualV = (inputOutputList.get(j).getValue() + this.minimumValuePerDeliveryArea.get(area.getId()))
					/ (this.maximumValuePerDeliveryArea.get(area.getId())
							+ this.minimumValuePerDeliveryArea.get(area.getId()));
			if (this.hTanActivation)
				actualV = actualV * 2.0 - 1.0;
			this.ANNPerDeliveryArea.get(area.getId()).calcError(new double[] { actualV });

			this.ANNPerDeliveryArea.get(area.getId()).learn();
		}

		// System.out.println("Error for training set: "
		// +
		// this.ANNPerDeliveryArea.get(area.getId()).getError(inputOutputList.size()));

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

	public static void checkFeasibilityBasedOnOrienteeringNo(OrderRequest request, int t, DeliveryArea subArea,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringTheftingTw,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringTheftingAdvanced,
			HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			boolean thefting, boolean theftingAdvanced, boolean theftingTw,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw, int numberOfTOPs,
			HashMap<TimeWindow, ArrayList<TimeWindow>> oldToNewTimeWindowMapping) {
		// Possible time windows for request
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
				.getConsiderationSet();
		for (ConsiderationSetAlternative alt : alternatives) {
			if (!alt.getAlternative().getNoPurchaseAlternative()) {
				if (oldToNewTimeWindowMapping != null) {
					for (TimeWindow tw : alt.getAlternative().getTimeWindows()) {
						timeWindows.addAll(oldToNewTimeWindowMapping.get(tw));
					}
				} else {
					timeWindows.addAll(alt.getAlternative().getTimeWindows());
				}
			}

		}
		HashMap<TimeWindow, Boolean> alreadyConsidered = new HashMap<TimeWindow, Boolean>();
		for (TimeWindow tw : timeWindows) {
			if (!alreadyConsidered.containsKey(tw)) {
				alreadyConsidered.put(tw, true);

				boolean feasible = false;
				if (avgAcceptablePerSubAreaAndTw.containsKey(subArea.getId())) {
					if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).containsKey(tw.getId())) {
						if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw.getId()) > 0) {
							timeWindowCandidatesOrienteering.add(tw);
							feasible = true;
						}
					}
				}

				if (!feasible && thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

					for (DeliveryArea nArea : neighbors.get(subArea)) {
						if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
							if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {

								if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0) {
									timeWindowCandidatesOrienteeringThefting.add(tw);
									feasible = true;
									break;
								}
							}
						}
					}

				} else if (!feasible && thefting && theftingTw) {
					// Check if tw and area neighbor have capacity -> steal from
					// area neighbor
					boolean option = false;
					for (DeliveryArea nArea : neighbors.get(subArea)) {
						if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
							if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {
								if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0) {
									option = true;
									break;
								}
							}
						}

					}

					if (option) {
						for (TimeWindow twN : neighborsTw.get(tw)) {


							if (alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.containsKey(twN.getId())
									&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
											.get(twN.getId()) > 0) {

								timeWindowCandidatesOrienteeringTheftingTw.add(tw);
								feasible = true;
								break;
							}
						}

					}
				}
				if (!feasible && theftingAdvanced && numberOfTOPs < 2) {

					int numberFeasible = 0;
					for (DeliveryArea nArea : neighbors.get(subArea)) {
						if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
							if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {

								if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 1) {
									timeWindowCandidatesOrienteeringTheftingAdvanced.add(tw);
									break;
								} else if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0) {
									numberFeasible++;
									if (numberFeasible > 1) {
										timeWindowCandidatesOrienteeringTheftingAdvanced.add(tw);
										break;
									}

								}
							}
						}
					}
				}

			}
		}
	}

	public static double determineValueFunctionApproximationValue(DeliveryArea area, DeliveryArea subArea, int t,
			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> remainingCapacityPerSubAreaAndTw,
			HashMap<Integer, Integer> maximumAcceptablePerTimeWindowOverSubAreas, TimeWindow timeWindowToReduce,
			HashMap<DemandSegment, Double> maximumArrivalsPerDemandSegment,
			HashMap<DemandSegment, Double> maximumNeighborArrivalsPerDemandSegment, double maximumValue,
			double minimumValue, boolean loseTwo, DemandSegmentWeighting dsw,
			HashMap<DeliveryArea, Double> daWeightsLower,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLower,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, TimeWindowSet tws, int arrivalProcessId,
			HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea, boolean considerLeftOverPenalty,
			ArrayList<DemandSegment> demandSegments, HashMap<Entity, Object> objectiveSpecificValues,
			double maximumRevenueValue,
			HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			boolean usePlainEstimator, boolean considerConstant, boolean considerNeighborDemand,
			HashMap<Integer, Integer> segmentInputMapper, HashMap<Integer, Integer> neighborSegmentInputMapper,
			HashMap<Integer, Integer> timeWindowInputMapper, double arrivalProbability) {

		boolean anyValueAbove1 = false;
		if (t > 0 || usePlainEstimator) {
			int numberOfVariables = dsw.getWeights().size() + tws.getElements().size();
			if (considerConstant)
				numberOfVariables++;
			if (considerNeighborDemand)
				numberOfVariables += dsw.getWeights().size();
			double[] inputRow = new double[numberOfVariables];

			double expectedArrivalsDeliveryArea = t * arrivalProbability * daWeightsLower.get(subArea);

			if (considerConstant)
				inputRow[inputRow.length - 1] = 1;
			for (DemandSegment s : daSegmentWeightingsLower.get(subArea).keySet()) {
				inputRow[segmentInputMapper.get(s.getId())] = expectedArrivalsDeliveryArea
						* daSegmentWeightingsLower.get(subArea).get(s) / maximumArrivalsPerDemandSegment.get(s);
				if (expectedArrivalsDeliveryArea * daSegmentWeightingsLower.get(subArea).get(s)
						/ maximumArrivalsPerDemandSegment.get(s) > 1.0)
					anyValueAbove1 = true;
			}
			if (considerNeighborDemand) {
				for (DemandSegment s : daSegmentWeightingsLower.get(subArea).keySet()) {
					double expectedArrivals = 0.0;
					for (DeliveryArea subArea2 : neighbors.get(subArea)) {
						expectedArrivals += daWeightsLower.get(subArea2)
								* daSegmentWeightingsLower.get(subArea2).get(s);
						// expectedArrivals+= t *
						// ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
						// *
						// daWeightsLower.get(subArea2)*daSegmentWeightingsLower.get(subArea2).get(s);
					}
					inputRow[neighborSegmentInputMapper.get(s.getId())] = expectedArrivals
							/ maximumNeighborArrivalsPerDemandSegment.get(s);
					if (expectedArrivals / maximumNeighborArrivalsPerDemandSegment.get(s) > 1.0)
						anyValueAbove1 = true;
				}
			}

			int leftOverCapacityOverall = 0;
			for (TimeWindow tw : tws.getElements()) {

				int remainingCap;

				if (remainingCapacityPerSubAreaAndTw.containsKey(subArea)
						&& remainingCapacityPerSubAreaAndTw.get(subArea).containsKey(tw)) {
					remainingCap = remainingCapacityPerSubAreaAndTw.get(subArea).get(tw);
					if (timeWindowToReduce != null && timeWindowToReduce.getId() == tw.getId() && !loseTwo) {
						// For a steal, remaining capacity is negative, but
						// should
						// not
						remainingCap = Math.max(remainingCap - 1, 0);
					} else if (timeWindowToReduce != null && timeWindowToReduce.getId() == tw.getId() && loseTwo) {
						remainingCap = Math.max(remainingCap - 2, 0);
					}
				} else {
					remainingCap = 0;
				}
				leftOverCapacityOverall += remainingCap;
				inputRow[timeWindowInputMapper.get(tw.getId())] = remainingCap
						/ (double) maximumAcceptablePerTimeWindowOverSubAreas.get(tw.getId());
				if (remainingCap / (double) maximumAcceptablePerTimeWindowOverSubAreas.get(tw.getId()) > 1.0)
					anyValueAbove1 = true;
			}

			if (leftOverCapacityOverall == 0 && !usePlainEstimator) {
				return 0.0;
			}

			if (anyValueAbove1) {
				System.out.println("Better normalisation?");
			}

			double[] output = ANNPerDeliveryArea.get(area.getId()).computeOutputs(inputRow);
			double value = output[0];
			if (ANNPerDeliveryArea.get(area.getId()).isHyperbolicTangensActivation()) {
				value = (value + 1.0) / 2.0;
			}

			value = value * (maximumValue + minimumValue) - minimumValue;
			return value;
		} else {

			if (considerLeftOverPenalty) {
				// TODO: COnsider left over penalty does not work with
				// overlapping time windows
				if (remainingCapacityPerSubAreaAndTw.containsKey(subArea)) {

					HashMap<TimeWindow, Integer> remainingCapacityPerTw = (HashMap<TimeWindow, Integer>) remainingCapacityPerSubAreaAndTw
							.get(subArea).clone();
					if (timeWindowToReduce != null) {
						// For a steal, remaing capacity is negative, but should
						// not
						int remainingCap = Math.max(remainingCapacityPerTw.get(timeWindowToReduce) - 1, 0);
						remainingCapacityPerTw.put(timeWindowToReduce, remainingCap);
					}
					return -1.0 * ADPWithOrienteeringANN.calculatePenalty(remainingCapacityPerTw, subArea,
							demandSegments, objectiveSpecificValues, maximumRevenueValue, daSegmentWeightingsLower,
							maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
							minimumExpectedMultiplierPerDemandSegmentAndTimeWindow);
				} else {
					return 0.0;
				}
			} else {
				return 0.0;
			}

		}
	}

	private void initialiseGlobal() {

		this.LOG_LOSS_FUNCTION = new HashMap<Integer, HashMap<Integer, ArrayList<Double>>>();
		this.LOG_WEIGHTS = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
		this.trainingSetNumberPerDeliveryArea = new HashMap<Integer, Integer>();
		// Maximum time frame and start time
				RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
				RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		if (this.timeWindowSet.getOverlapping()) {
			timeWindowSetOverlappingDummy = new TimeWindowSet();
			timeWindowSetOverlappingDummy.setOverlapping(false);
			this.timeWindowSet.sortElementsAsc();
			this.oldToNewTimeWindowMapping = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
			newToOldTimeWindowMapping = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
			// Split time windows into blocks with clear assignment
			// First: Determine relevant time points
			ArrayList<Double> relevantTimePoints = new ArrayList<Double>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
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
					if (foundEnd){
						dummyTws.add(tw);
						newToOldTimeWindowMapping.put(tw, new ArrayList<TimeWindow>());
					}

				}
			}
			this.timeWindowSetOverlappingDummy.setElements(dummyTws);
			timeWindowSetOverlappingDummy.setTempStartOfDeliveryPeriod(timeWindowSet.getTempStartOfDeliveryPeriod());
			// Define mapping
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				ArrayList<TimeWindow> mappedWindows = new ArrayList<TimeWindow>();
				for (TimeWindow newTw : dummyTws) {
					if (newTw.getStartTime() >= tw.getStartTime() && newTw.getEndTime() <= tw.getEndTime()) {
						mappedWindows.add(newTw);
						newToOldTimeWindowMapping.get(newTw).add(tw);
					}
				}
				this.oldToNewTimeWindowMapping.put(tw, mappedWindows);
			}

			neighborsTw = this.timeWindowSetOverlappingDummy.defineNeighborTimeWindows();
		} else {
			neighborsTw = this.timeWindowSet.defineNeighborTimeWindows();
		}

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
		this.targetRoutingResults = new ArrayList<Routing>();
		this.determineReferenceInformation();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();

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
		this.aggregateReferenceInformation();
		this.determineAverageAndMaximumAcceptablePerTimeWindow();
		// this.chooseReferenceRoutings();
		this.determineDemandMultiplierPerTimeWindow();
		this.determineMaximumLowerAreaWeight();
		// Separate into delivery areas for parallel computing

		this.prepareOrderRequestsForDeliveryAreas();
		this.prepareVehicleAssignmentsForDeliveryAreas();

		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();

		// if (this.considerAreaPotential)

		// Initialise basic and variable coefficients per delivery area
		this.initialiseANNRouting();
		this.initialiseANNOrderSets();

		if (this.theftBased) {
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
							this.daSegmentWeightingsLower, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
							daSegmentWeightingsLower, timeWindowSet);
		}

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

	private void updateANNRouting(ArrayList<Routing> routings, Double learningRate, boolean init) {

		// Group route elements according to lower area
		HashMap<DeliveryArea, HashMap<Routing, ArrayList<RouteElement>>> ePerLowerDeliveryArea = new HashMap<DeliveryArea, HashMap<Routing, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, HashMap<Routing, Routing>> referenceRouting = new HashMap<DeliveryArea, HashMap<Routing, Routing>>();

		for (Routing r : routings) {
			// Assign reference routing to routing
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
				if (!referenceRouting.containsKey(area))
					referenceRouting.put(area, new HashMap<Routing, Routing>());
				if (r.isPossiblyTarget()) {
					referenceRouting.get(area).put(r, r);
				} else {
					// Go through all reference routings and assign the one for
					// the area
					for (ReferenceRouting rr : r.getOrderSet().getReferenceRoutingsPerDeliveryArea()) {
						if (rr.getDeliveryAreaId() == area.getId()) {
							referenceRouting.get(area).put(r, rr.getRouting());
							break;
						}
					}
				}
			}

			// Assign route elements to delivery areas
			for (Route rt : r.getRoutes()) {
				for (int i = 0; i < rt.getRouteElements().size(); i++) {

					// Do not consider depots that were added to the
					// reference routings
					// if ((i != 0 && i != rt.getRouteElements().size() - 1) ||
					// !this.referenceRoutingsList.contains(r)) {
					RouteElement e = rt.getRouteElements().get(i);
					if (!(e.getOrderId() == null || e.getOrderId() == 0)) {
						e.setTravelTimeTo(e.getTravelTime());
						double travelTimeFrom;
						if (i != rt.getRouteElements().size() - 1) {
							travelTimeFrom = rt.getRouteElements().get(i + 1).getTravelTime();
						} else {

							// TODO
							travelTimeFrom = 0;
						}
						e.setTravelTimeFrom(travelTimeFrom);
						DeliveryArea area;
						if (init) {
							area = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
									e.getOrder().getOrderRequest().getCustomer());
							e.getOrder().getOrderRequest().getCustomer().setTempDeliveryArea(area);
						} else {
							area = e.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea();
						}

						if (!ePerLowerDeliveryArea.containsKey(area))
							ePerLowerDeliveryArea.put(area, new HashMap<Routing, ArrayList<RouteElement>>());
						if (!ePerLowerDeliveryArea.get(area).containsKey(r))
							ePerLowerDeliveryArea.get(area).put(r, new ArrayList<RouteElement>());
						ePerLowerDeliveryArea.get(area).get(r).add(e);
					}

				}

			}
		}

		// Train network per upper delivery area
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			if (init) {
				this.maximumValuePerDeliveryArea.put(area.getId(), 0.0);
				this.minimumValuePerDeliveryArea.put(area.getId(), 0.0);
			}
			// Determine maximum capacities for normalisation
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo = this
					.copyAggregateNoInformation(area, false);
			Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = ADPWithOrienteeringANN
					.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(aggregateInformationNo);
			if (init) {
				HashMap<Integer, Integer> maximumAcceptablePerTimeWindowOverSubAreas = ADPWithOrienteeringANN
						.determineMaximumAcceptablePerTimeWindowOverSubareas(updatedAggregates.getValue());

				this.maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea.put(area.getId(),
						maximumAcceptablePerTimeWindowOverSubAreas);

				// Determine maximum arrivals for normalisation

				HashMap<DemandSegment, Double> maximumArrivals = new HashMap<DemandSegment, Double>();
				HashMap<DemandSegment, Double> maximumArrivalsForNeighbors = new HashMap<DemandSegment, Double>();

				for (DeliveryArea subArea : area.getSubset().getElements()) {

					for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {
						if (!maximumArrivals.containsKey(s)) {
							maximumArrivals.put(s, 0.0);
						}
						double arrivals = this.orderHorizonLength * this.arrivalProbability
								* this.daWeightsLower.get(subArea)
								* this.daSegmentWeightingsLowerHash.get(subArea).get(s);
						if (arrivals > maximumArrivals.get(s)) {
							maximumArrivals.put(s, arrivals);
						}
						if (!maximumArrivalsForNeighbors.containsKey(s)) {
							maximumArrivalsForNeighbors.put(s, 0.0);
						}
						double arrivalsOverNeighbors = 0.0;
						for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {


							arrivalsOverNeighbors += this.daWeightsLower.get(subArea2)
									* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

						}
						if (arrivalsOverNeighbors > maximumArrivalsForNeighbors.get(s)) {
							maximumArrivalsForNeighbors.put(s, arrivalsOverNeighbors);
						}
					}
				}

				this.maximumArrivalsOverSubAreasPerDeliveryArea.put(area.getId(), maximumArrivals);
				this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.put(area.getId(), maximumArrivalsForNeighbors);
			}

			int numberOfVariables = this.demandSegmentWeighting.getWeights().size();
			if (this.timeWindowSet.getOverlapping()) {
				numberOfVariables += this.timeWindowSetOverlappingDummy.getElements().size();
			} else {
				numberOfVariables += this.timeWindowSet.getElements().size();
			}

			if (considerConstant)
				numberOfVariables++;
			if (this.considerDemandNeighbors)
				numberOfVariables += this.daSegmentWeightingsUpper.get(area).getWeights().size();

			int inputCount = numberOfVariables;
			int outputCount = 1;
			int hiddenCount = inputCount + outputCount + this.additionalHiddenNodes;

			if (init) {
				NeuralNetwork network = new NeuralNetwork(inputCount, hiddenCount, outputCount, learningRate,
						this.momentumWeight, this.hTanActivation);
				network.setInputTypes(
						new String[] { ValueFunctionCoefficientType.EXPECTED_DEMAND_SEGMENT_ARRIVALS.toString(),
								ValueFunctionCoefficientType.REMAINING_CAPACITY.toString() });
				network.setInputSetIds(
						new Integer[] { this.demandSegmentWeighting.getId(), this.timeWindowSet.getId() });
				int numberOfElements = this.demandSegmentWeighting.getWeights().size();
				if (this.timeWindowSet.getOverlapping()) {
					numberOfElements += this.timeWindowSetOverlappingDummy.getElements().size();
				} else {
					numberOfElements += this.timeWindowSet.getElements().size();
				}

				if (this.considerDemandNeighbors)
					numberOfElements += this.demandSegmentWeighting.getWeights().size();
				Integer[] elementIds = new Integer[numberOfElements];
				double[] maxValuePerElement = new double[numberOfElements];
				int currentIndex = 0;
				this.segmentInputMapper = new HashMap<Integer, Integer>();
				for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {
					elementIds[currentIndex] = ds.getId();
					segmentInputMapper.put(ds.getId(), currentIndex);
					maxValuePerElement[currentIndex++] = this.maximumArrivalsOverSubAreasPerDeliveryArea
							.get(area.getId()).get(ds);
				}
				if (this.considerDemandNeighbors) {
					this.neighborSegmentInputMapper = new HashMap<Integer, Integer>();
					for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {
						elementIds[currentIndex] = ds.getId();
						neighborSegmentInputMapper.put(ds.getId(), currentIndex);
						maxValuePerElement[currentIndex++] = this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea
								.get(area.getId()).get(ds);
					}
				}

				this.timeWindowInputMapper = new HashMap<Integer, Integer>();

				if (this.timeWindowSet.getOverlapping()) {
					this.timeWindowSetOverlappingDummy.sortElementsAsc();
					for (TimeWindow tw : this.timeWindowSetOverlappingDummy.getElements()) {
						elementIds[currentIndex] = tw.getId();
						timeWindowInputMapper.put(tw.getId(), currentIndex);
						maxValuePerElement[currentIndex++] = this.maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea
								.get(area.getId()).get(tw.getId());
					}
				} else {
					for (TimeWindow tw : this.timeWindowSet.getElements()) {
						elementIds[currentIndex] = tw.getId();
						timeWindowInputMapper.put(tw.getId(), currentIndex);
						maxValuePerElement[currentIndex++] = this.maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea
								.get(area.getId()).get(tw.getId());
					}
				}

				network.setInputElementIds(elementIds);
				network.setMaximumValuesElements(maxValuePerElement);

				this.ANNPerDeliveryArea.put(area.getId(), network);

			}

			ArrayList<Pair<double[], Double>> inputOutputList = new ArrayList<Pair<double[], Double>>();
			System.out.println("Start with building inputOutputList" + System.currentTimeMillis());
			// Collect training data for the respective delivery areas
			// (specific delivery area not necessary, but lower level)
			int numberOfRounds = 0;
			ArrayList<TimeWindow> relevantTimeWindows;
			if (!this.timeWindowSet.getOverlapping()) {
				relevantTimeWindows = this.timeWindowSet.getElements();
			} else {
				relevantTimeWindows = this.timeWindowSetOverlappingDummy.getElements();
			}
			for (DeliveryArea subArea : area.getSubset().getElements()) {

				if (ePerLowerDeliveryArea.containsKey(subArea)) {
					// Go through routings and add training data
					int numberOfRoutingsWithCapacity = ePerLowerDeliveryArea.get(subArea).keySet().size();
					if (numberOfRoutingsWithCapacity < this.previousRoutingResults.size()) {
						for (Routing r : this.previousRoutingResults) {
							if (!ePerLowerDeliveryArea.get(subArea).containsKey(r)) {
								ePerLowerDeliveryArea.get(subArea).put(r, new ArrayList<RouteElement>());
							}
						}
					}

					for (Routing r : ePerLowerDeliveryArea.get(subArea).keySet()) {
						numberOfRounds++;
						HashMap<TimeWindow, ArrayList<Integer>> remainingCapacityPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
						HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegment = new HashMap<DemandSegment, ArrayList<Double>>();
						HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegmentNeighbors = new HashMap<DemandSegment, ArrayList<Double>>();

						// Determine feed-forward details

						Collections.sort(ePerLowerDeliveryArea.get(subArea).get(r),
								new RouteElementArrivalTimeDescComparator());


						Routing rRouting = referenceRouting.get(area).get(r);
						for (TimeWindow tw : relevantTimeWindows) {
							remainingCapacityPerTimeWindow.put(tw, new ArrayList<Integer>());

							if (this.aggregatedReferenceInformationNo.get(area).get(rRouting).containsKey(subArea)
									&& this.aggregatedReferenceInformationNo.get(area).get(rRouting).get(subArea)
											.containsKey(tw)) {

								remainingCapacityPerTimeWindow.get(tw).add(this.aggregatedReferenceInformationNo
										.get(area).get(rRouting).get(subArea).get(tw));
							} else {
								remainingCapacityPerTimeWindow.get(tw).add(0);
							}
						}

						// 1.) First at beginning of order horizon
						double meanArrivalProb = arrivalProbability;
						int lastT;
						if (ePerLowerDeliveryArea.get(subArea).get(r).size() == 0
								|| ePerLowerDeliveryArea.get(subArea).get(r).get(0).getOrder().getOrderRequest()
										.getArrivalTime() < this.orderHorizonLength) {

							lastT = this.orderHorizonLength;
							for (TimeWindow tw : relevantTimeWindows) {
								if (this.aggregatedReferenceInformationNo.get(area).get(rRouting).containsKey(subArea)
										&& this.aggregatedReferenceInformationNo.get(area).get(rRouting).get(subArea)
												.containsKey(tw)) {

									remainingCapacityPerTimeWindow.get(tw).add(this.aggregatedReferenceInformationNo
											.get(area).get(rRouting).get(subArea).get(tw));
								} else {
									remainingCapacityPerTimeWindow.get(tw).add(0);
								}
							}

							for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

								expectedArrivalsPerDemandSegment.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegmentNeighbors.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegment.get(s)
										.add(this.orderHorizonLength * meanArrivalProb
												* this.daWeightsLower.get(subArea)
												* this.daSegmentWeightingsLowerHash.get(subArea).get(s));
								double arrNeighbors = 0.0;
								for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
									arrNeighbors += this.daWeightsLower.get(subArea2)
											* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

								}
								expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
							}
						} else {
							lastT = this.orderHorizonLength + 1;
							for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

								expectedArrivalsPerDemandSegment.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegmentNeighbors.put(s, new ArrayList<Double>());
							}
						}

						// 2.) Actual elements
						// System.out.println("Start filling actual elements"+
						// System.currentTimeMillis());

						for (int i = 0; i < ePerLowerDeliveryArea.get(subArea).get(r).size(); i++) {

							RouteElement e = ePerLowerDeliveryArea.get(subArea).get(r).get(i);
							for (int a = lastT - 1; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
								for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

									expectedArrivalsPerDemandSegment.get(s)
											.add(a * meanArrivalProb * this.daWeightsLower.get(subArea)
													* this.daSegmentWeightingsLowerHash.get(subArea).get(s));

									double arrNeighbors = 0.0;
									for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
										arrNeighbors += this.daWeightsLower.get(subArea2)
												* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

									}
									expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
								}
							}

							for (TimeWindow tw : relevantTimeWindows) {
								int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
										.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

								for (int a = lastT - 2; a >= e.getOrder().getOrderRequest().getArrivalTime(); a--) {
									remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
								}

								if (e.getServiceBegin() >= (tw.getStartTime()
										- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
										&& e.getServiceBegin() < (tw.getEndTime()
												- this.timeWindowSet.getTempStartOfDeliveryPeriod())
												* TIME_MULTIPLIER) {
									newNumberForTimeWindow--;
								}

								remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
							}

							lastT = e.getOrder().getOrderRequest().getArrivalTime();
						}
						// System.out.println("End filling actual elements"+
						// System.currentTimeMillis());
						// System.out.println("Start filling up"+
						// System.currentTimeMillis());
						if (lastT > 1) {
							for (int a = lastT - 1; a > 0; a--) {
								for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

									expectedArrivalsPerDemandSegment.get(s)
											.add(a * meanArrivalProb * this.daWeightsLower.get(subArea)
													* this.daSegmentWeightingsLowerHash.get(subArea).get(s));
									double arrNeighbors = 0.0;
									for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
										arrNeighbors += this.daWeightsLower.get(subArea2)
												* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

									}
									expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
								}
							}

							for (TimeWindow tw : relevantTimeWindows) {
								int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
										.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

								for (int a = lastT - 2; a >= 0; a--) {
									remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
								}
							}

						}
						// System.out.println("End filling up"+
						// System.currentTimeMillis());
						if (this.considerLeftOverPenalty) {

							HashMap<TimeWindow, Integer> remainingCap = new HashMap<TimeWindow, Integer>();
							for (TimeWindow tw : remainingCapacityPerTimeWindow.keySet()) {
								remainingCap.put(tw, remainingCapacityPerTimeWindow.get(tw).get(0));
							}
							double maximumPenalty = ADPWithOrienteeringANN.calculatePenalty(remainingCap, subArea,
									demandSegments, objectiveSpecificValues, maximumRevenueValue,
									daSegmentWeightingsLowerHash,
									maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
									minimumExpectedMultiplierPerDemandSegmentAndTimeWindow);

							if (init & maximumPenalty > minimumValuePerDeliveryArea.get(area.getId())) {
								this.minimumValuePerDeliveryArea.put(area.getId(), maximumPenalty);
							}

						}

						for (DemandSegment s : daSegmentWeightingsLowerHash.get(subArea).keySet()) {

							expectedArrivalsPerDemandSegment.get(s).add(0.0);

							expectedArrivalsPerDemandSegmentNeighbors.get(s).add(0.0);
						}

						// System.out.println("End filling"+
						// System.currentTimeMillis());

						// Determine backward details
						Collections.sort(ePerLowerDeliveryArea.get(subArea).get(r),
								new RouteElementArrivalTimeAscComparator());

						ArrayList<Double> accumulatedValues = new ArrayList<Double>();
						double value = 0.0;
						double valueForMax = 0.0;
						int currentT = 0;
						if (ePerLowerDeliveryArea.get(subArea).get(r).size() > 0) {
							for (int a = currentT; a < ePerLowerDeliveryArea.get(subArea).get(r).get(0).getOrder()
									.getOrderRequest().getArrivalTime(); a++) {
								accumulatedValues.add(value);
							}
							currentT = ePerLowerDeliveryArea.get(subArea).get(r).get(0).getOrder().getOrderRequest()
									.getArrivalTime();
						}
						int targetT = this.orderHorizonLength;

						for (int reId = 0; reId < ePerLowerDeliveryArea.get(subArea).get(r).size(); reId++) {
							RouteElement e = ePerLowerDeliveryArea.get(subArea).get(r).get(reId);
							if (reId < ePerLowerDeliveryArea.get(subArea).get(r).size() - 1) {
								targetT = ePerLowerDeliveryArea.get(subArea).get(r).get(reId + 1).getOrder()
										.getOrderRequest().getArrivalTime() - 1;
							} else {
								targetT = this.orderHorizonLength;
							}
							double orderValue;
							if (!init) {
								orderValue = CustomerDemandService.calculateOrderValue(e.getOrder().getOrderRequest(),
										maximumRevenueValue, objectiveSpecificValues);
							} else {
								orderValue = CustomerDemandService.calculateMedianValue(maximumRevenueValue,
										objectiveSpecificValues,
										e.getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment());
								double actualV = CustomerDemandService.calculateOrderValue(
										e.getOrder().getOrderRequest(), maximumRevenueValue, objectiveSpecificValues);
								if (orderValue < actualV) {
									valueForMax = valueForMax + actualV;
								} else {
									valueForMax = valueForMax + orderValue;
								}

							}

							value = value + orderValue;


							while (currentT <= targetT) {
								accumulatedValues.add(value);
								// * Math.pow(this.discountingFactorProbability,
								// this.orderHorizonLength - currentT));
								currentT++;
							}

						}

						// Fill up till end of order horizon
						for (int t = currentT; t <= targetT; t++) {
							accumulatedValues.add(value);
							// * Math.pow(this.discountingFactorProbability,
							// this.orderHorizonLength - t));
						}

						Collections.sort(accumulatedValues, Collections.reverseOrder());
						if (init && valueForMax > maximumValuePerDeliveryArea.get(area.getId())) {
							this.maximumValuePerDeliveryArea.put(area.getId(), valueForMax);
						}
						// Add to training data

						for (int i = 0; i < accumulatedValues.size(); i++) {

							double[] inputRow = new double[numberOfVariables];

							int currentId = 0;
							if (considerConstant)
								inputRow[inputRow.length - 1] = 1;
							for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

								inputRow[currentId++] = expectedArrivalsPerDemandSegment.get(w.getDemandSegment())
										.get(i)
										/ this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
												.get(w.getDemandSegment());

							}
							if (this.considerDemandNeighbors) {

								for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

									inputRow[currentId++] = expectedArrivalsPerDemandSegmentNeighbors
											.get(w.getDemandSegment()).get(i)
											/ this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
													.get(w.getDemandSegment());

								}

							}
							for (TimeWindow tw : relevantTimeWindows) {
								inputRow[currentId++] = ((double) remainingCapacityPerTimeWindow.get(tw).get(i))
										/ ((double) this.maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea
												.get(area.getId()).get(tw.getId()));
							}
							inputOutputList.add(new Pair<double[], Double>(inputRow, accumulatedValues.get(i)));
						}

					}
				}
			}
			// System.out.println("No of rounds:"+numberOfRounds);
			Collections.shuffle(inputOutputList);

			// Train network with the available data
			for (int j = 0; j < inputOutputList.size(); j++) {
				this.ANNPerDeliveryArea.get(area.getId()).computeOutputs(inputOutputList.get(j).getKey());
				double actualV = (inputOutputList.get(j).getValue()
						+ this.minimumValuePerDeliveryArea.get(area.getId()))
						/ (this.maximumValuePerDeliveryArea.get(area.getId())
								+ this.minimumValuePerDeliveryArea.get(area.getId()));
				if (this.hTanActivation)
					actualV = actualV * 2.0 - 1.0;
				this.ANNPerDeliveryArea.get(area.getId()).calcError(new double[] { actualV });
				this.ANNPerDeliveryArea.get(area.getId()).learn();
			}
			System.out.println("Error for training set: " + this.ANNPerDeliveryArea.get(area.getId())
					.getError(routings.size() * (this.orderHorizonLength + 1) * area.getSubset().getElements().size()));
		}

	}

	private void initialiseANNRouting() {

		this.ANNPerDeliveryArea = new HashMap<Integer, NeuralNetwork>();
		this.maximumValuePerDeliveryArea = new HashMap<Integer, Double>();
		this.minimumValuePerDeliveryArea = new HashMap<Integer, Double>();
		this.maximumArrivalsOverSubAreasPerDeliveryArea = new HashMap<Integer, HashMap<DemandSegment, Double>>();
		this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea = new HashMap<Integer, HashMap<DemandSegment, Double>>();
		this.maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();

		double initialLearingRate = Math.min(this.stepSize * 100, 0.01);
		this.updateANNRouting(this.previousRoutingResults, initialLearingRate, true);

	}

	private void initialiseANNOrderSets() {

		// TODO: Does not work with overlapping time windows!!!!!
		if (previousOrderSetResults != null) {
			// Group route elements according to lower area
			HashMap<DeliveryArea, HashMap<OrderSet, ArrayList<Order>>> oPerLowerDeliveryArea = new HashMap<DeliveryArea, HashMap<OrderSet, ArrayList<Order>>>();
			HashMap<DeliveryArea, HashMap<OrderSet, Routing>> referenceRouting = new HashMap<DeliveryArea, HashMap<OrderSet, Routing>>();

			for (OrderSet os : this.previousOrderSetResults) {
				// Assign reference routing to routing
				for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
					if (!referenceRouting.containsKey(area))
						referenceRouting.put(area, new HashMap<OrderSet, Routing>());

					// Go through all reference routings and assign the one for
					// the area
					for (ReferenceRouting rr : os.getReferenceRoutingsPerDeliveryArea()) {
						if (rr.getDeliveryAreaId() == area.getId()) {
							referenceRouting.get(area).put(os, rr.getRouting());
							break;
						}

					}
				}

				// Assign orders to delivery areas

				for (int i = 0; i < os.getElements().size(); i++) {

					// Do not consider depots that were added to the
					// reference routings
					// if ((i != 0 && i != rt.getRouteElements().size() - 1) ||
					// !this.referenceRoutingsList.contains(r)) {
					Order o = os.getElements().get(i);
					if (o.getAccepted()) {
						DeliveryArea area = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(
								deliveryAreaSet, o.getOrderRequest().getCustomer());
						if (!oPerLowerDeliveryArea.containsKey(area))
							oPerLowerDeliveryArea.put(area, new HashMap<OrderSet, ArrayList<Order>>());
						if (!oPerLowerDeliveryArea.get(area).containsKey(os))
							oPerLowerDeliveryArea.get(area).put(os, new ArrayList<Order>());
						oPerLowerDeliveryArea.get(area).get(os).add(o);
					}
				}

			}

			// Train network per upper delivery area
			for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

				HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo = this
						.copyAggregateNoInformation(area, false);
				Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> updatedAggregates = ADPWithOrienteeringANN
						.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(aggregateInformationNo);

				int numberOfVariables = this.demandSegmentWeighting.getWeights().size();
				if (this.timeWindowSet.getOverlapping()) {
					numberOfVariables += this.timeWindowSetOverlappingDummy.getElements().size();
				} else {
					numberOfVariables += this.timeWindowSet.getElements().size();
				}

				if (considerConstant)
					numberOfVariables++;
				if (this.considerDemandNeighbors)
					numberOfVariables += this.demandSegmentWeighting.getWeights().size();

				ArrayList<Pair<double[], Double>> inputOutputList = new ArrayList<Pair<double[], Double>>();

				// Collect training data for the respective delivery areas
				// (specific delivery area not necessary, but lower level)
				ArrayList<TimeWindow> relevantTimeWindows;
				if (!this.timeWindowSet.getOverlapping()) {
					relevantTimeWindows = this.timeWindowSet.getElements();
				} else {
					relevantTimeWindows = this.timeWindowSetOverlappingDummy.getElements();
				}
				for (DeliveryArea subArea : area.getSubset().getElements()) {

					// Go order sets and add learning data
					if (!oPerLowerDeliveryArea.containsKey(subArea))
						oPerLowerDeliveryArea.put(subArea, new HashMap<OrderSet, ArrayList<Order>>());

					for (OrderSet os : this.previousOrderSetResults) {
						if (!oPerLowerDeliveryArea.get(subArea).containsKey(os)) {
							oPerLowerDeliveryArea.get(subArea).put(os, new ArrayList<Order>());
						}
					}

					for (OrderSet os : oPerLowerDeliveryArea.get(subArea).keySet()) {
						HashMap<TimeWindow, ArrayList<Integer>> remainingCapacityPerTimeWindow = new HashMap<TimeWindow, ArrayList<Integer>>();
						HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegment = new HashMap<DemandSegment, ArrayList<Double>>();
						HashMap<DemandSegment, ArrayList<Double>> expectedArrivalsPerDemandSegmentNeighbors = new HashMap<DemandSegment, ArrayList<Double>>();

						// Determine feed-forward details
						Collections.sort(oPerLowerDeliveryArea.get(subArea).get(os),
								new OrderArrivalTimeDescComparator());

						for (TimeWindow tw : this.timeWindowSet.getElements()) {
							remainingCapacityPerTimeWindow.put(tw, new ArrayList<Integer>());

							if (this.aggregatedReferenceInformationNo.get(area).get(referenceRouting.get(area).get(os))
									.containsKey(subArea)
									&& this.aggregatedReferenceInformationNo.get(area)
											.get(referenceRouting.get(area).get(os)).get(subArea).containsKey(tw)) {

								remainingCapacityPerTimeWindow.get(tw).add(this.aggregatedReferenceInformationNo
										.get(area).get(referenceRouting.get(area).get(os)).get(subArea).get(tw));
							} else {
								remainingCapacityPerTimeWindow.get(tw).add(0);
							}
						}

						// 1.) First at beginning of order horizon
						int lastT;
						if (oPerLowerDeliveryArea.get(subArea).get(os).size() == 0 || oPerLowerDeliveryArea.get(subArea)
								.get(os).get(0).getOrderRequest().getArrivalTime() < this.orderHorizonLength) {

							lastT = this.orderHorizonLength;
							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								if (this.aggregatedReferenceInformationNo.get(area)
										.get(referenceRouting.get(area).get(os)).containsKey(subArea)
										&& this.aggregatedReferenceInformationNo.get(area)
												.get(referenceRouting.get(area).get(os)).get(subArea).containsKey(tw)) {

									remainingCapacityPerTimeWindow.get(tw).add(this.aggregatedReferenceInformationNo
											.get(area).get(referenceRouting.get(area).get(os)).get(subArea).get(tw));
								} else {
									remainingCapacityPerTimeWindow.get(tw).add(0);
								}
							}

							for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

								expectedArrivalsPerDemandSegment.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegmentNeighbors.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegment.get(s)
										.add(this.orderHorizonLength * arrivalProbability
												* this.daWeightsLower.get(subArea)
												* this.daSegmentWeightingsLowerHash.get(subArea).get(s));

								double arrNeighbors = 0.0;
								for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
									arrNeighbors += this.daWeightsLower.get(subArea2)
											* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
									// arrNeighbors += this.orderHorizonLength
									// *
									// ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
									// * this.daWeightsLower.get(subArea2) *
									// this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
								}
								expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
							}
						} else {
							lastT = this.orderHorizonLength + 1;
							for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

								expectedArrivalsPerDemandSegment.put(s, new ArrayList<Double>());
								expectedArrivalsPerDemandSegmentNeighbors.put(s, new ArrayList<Double>());

							}
						}

						// 2.) Actual elements
						for (int i = 0; i < oPerLowerDeliveryArea.get(subArea).get(os).size(); i++) {

							Order o = oPerLowerDeliveryArea.get(subArea).get(os).get(i);
							for (int a = lastT - 1; a >= o.getOrderRequest().getArrivalTime(); a--) {
								for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

									expectedArrivalsPerDemandSegment.get(s)
											.add(a * arrivalProbability * this.daWeightsLower.get(subArea)
													* this.daSegmentWeightingsLowerHash.get(subArea).get(s));

									double arrNeighbors = 0.0;
									for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
										arrNeighbors += this.daWeightsLower.get(subArea2)
												* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
										// arrNeighbors += a
										// *
										// ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
										// * this.daWeightsLower.get(subArea2) *
										// this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
									}
									expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
								}
							}

							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
										.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

								for (int a = lastT - 2; a >= o.getOrderRequest().getArrivalTime(); a--) {
									remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
								}

								if (tw.equals(o.getTimeWindowFinal())) {
									newNumberForTimeWindow--;
								}

								remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
							}

							lastT = o.getOrderRequest().getArrivalTime();
						}

						if (lastT > 1) {
							for (int a = lastT - 1; a > 0; a--) {
								for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

									expectedArrivalsPerDemandSegment.get(s)
											.add(a * arrivalProbability * this.daWeightsLower.get(subArea)
													* this.daSegmentWeightingsLowerHash.get(subArea).get(s));

									double arrNeighbors = 0.0;
									for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {
										arrNeighbors += this.daWeightsLower.get(subArea2)
												* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
										// arrNeighbors += a
										// *
										// ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
										// * this.daWeightsLower.get(subArea2) *
										// this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
									}
									expectedArrivalsPerDemandSegmentNeighbors.get(s).add(arrNeighbors);
								}
							}

							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								int newNumberForTimeWindow = remainingCapacityPerTimeWindow.get(tw)
										.get(remainingCapacityPerTimeWindow.get(tw).size() - 1);

								for (int a = lastT - 2; a >= 0; a--) {
									remainingCapacityPerTimeWindow.get(tw).add(newNumberForTimeWindow);
								}
							}

						}

						if (this.considerLeftOverPenalty) {

							HashMap<TimeWindow, Integer> remainingCap = new HashMap<TimeWindow, Integer>();
							for (TimeWindow tw : remainingCapacityPerTimeWindow.keySet()) {
								remainingCap.put(tw, remainingCapacityPerTimeWindow.get(tw).get(0));
							}
							double maximumPenalty = ADPWithOrienteeringANN.calculatePenalty(remainingCap, subArea,
									demandSegments, objectiveSpecificValues, maximumRevenueValue,
									daSegmentWeightingsLowerHash,
									maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
									minimumExpectedMultiplierPerDemandSegmentAndTimeWindow);


							if (maximumPenalty > minimumValuePerDeliveryArea.get(area.getId())) {
								this.minimumValuePerDeliveryArea.put(area.getId(), maximumPenalty);
							}

						}

						for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {

							expectedArrivalsPerDemandSegment.get(s).add(0.0);

							expectedArrivalsPerDemandSegmentNeighbors.get(s).add(0.0);
						}

						// Determine backward details
						Collections.sort(oPerLowerDeliveryArea.get(subArea).get(os),
								new OrderArrivalTimeAscComparator());

						ArrayList<Double> accumulatedValues = new ArrayList<Double>();
						double value = 0.0;
						// double valueForMax = 0.0;
						int currentT = 0;
						if (oPerLowerDeliveryArea.get(subArea).get(os).size() > 0) {
							for (int a = currentT; a < oPerLowerDeliveryArea.get(subArea).get(os).get(0)
									.getOrderRequest().getArrivalTime(); a++) {
								accumulatedValues.add(value);
							}
							currentT = oPerLowerDeliveryArea.get(subArea).get(os).get(0).getOrderRequest()
									.getArrivalTime();
						}
						int targetT = this.orderHorizonLength;

						for (int reId = 0; reId < oPerLowerDeliveryArea.get(subArea).get(os).size(); reId++) {
							Order o = oPerLowerDeliveryArea.get(subArea).get(os).get(reId);
							if (reId < oPerLowerDeliveryArea.get(subArea).get(os).size() - 1) {
								targetT = oPerLowerDeliveryArea.get(subArea).get(os).get(reId + 1).getOrderRequest()
										.getArrivalTime() - 1;
							} else {
								targetT = this.orderHorizonLength;
							}
							value = value + CustomerDemandService.calculateOrderValue(o.getOrderRequest(),
									maximumRevenueValue, objectiveSpecificValues);


							while (currentT <= targetT) {
								accumulatedValues.add(value);
								// * Math.pow(this.discountingFactorProbability,
								// this.orderHorizonLength - currentT));
								currentT++;
							}

						}

						// Fill up till end of order horizon
						for (int t = currentT; t <= targetT; t++) {
							accumulatedValues.add(value);

						}

						Collections.sort(accumulatedValues, Collections.reverseOrder());

						// Add to training data
						for (int i = 0; i < accumulatedValues.size(); i++) {

							double[] inputRow = new double[numberOfVariables];

							int currentId = 0;
							if (considerConstant)
								inputRow[inputRow.length - 1] = 1;
							for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

								inputRow[currentId++] = expectedArrivalsPerDemandSegment.get(w.getDemandSegment())
										.get(i)
										/ this.maximumArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
												.get(w.getDemandSegment());

							}
							if (this.considerDemandNeighbors) {

								for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {

									inputRow[currentId++] = expectedArrivalsPerDemandSegmentNeighbors
											.get(w.getDemandSegment()).get(i)
											/ this.maximumNeighborArrivalsOverSubAreasPerDeliveryArea.get(area.getId())
													.get(w.getDemandSegment());

								}

							}

							for (TimeWindow tw : this.timeWindowSet.getElements()) {
								inputRow[currentId++] = ((double) remainingCapacityPerTimeWindow.get(tw).get(i))
										/ ((double) maximumAcceptablePerTimeWindowOverSubAreasPerDeliveryArea
												.get(area.getId()).get(tw.getId()));
							}
							inputOutputList.add(new Pair<double[], Double>(inputRow, accumulatedValues.get(i)));
						}
					}

				}

				Collections.shuffle(inputOutputList);

				// Train network with the available data
				for (int j = 0; j < inputOutputList.size(); j++) {
					this.ANNPerDeliveryArea.get(area.getId()).computeOutputs(inputOutputList.get(j).getKey());

					double actualV = (inputOutputList.get(j).getValue()
							+ this.minimumValuePerDeliveryArea.get(area.getId()))
							/ (this.maximumValuePerDeliveryArea.get(area.getId())
									+ this.minimumValuePerDeliveryArea.get(area.getId()));
					if (this.hTanActivation)
						actualV = actualV * 2.0 - 1.0;
					this.ANNPerDeliveryArea.get(area.getId()).calcError(new double[] { actualV });

					this.ANNPerDeliveryArea.get(area.getId()).learn();
				}
				System.out.println("Error for training set: "
						+ this.ANNPerDeliveryArea.get(area.getId()).getError(this.previousRoutingResults.size()
								* (this.orderHorizonLength + 1) * area.getSubset().getElements().size()));
			}
		}
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

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());

		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);

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

	private static Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> avgPerRouting) {

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> divisorPerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Go through aggr. no per routing
		for (Routing r : avgPerRouting.keySet()) {

			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = avgPerRouting.get(r);
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

	}

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	private void aggregateReferenceInformation() {

		this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
		this.acceptablePerDeliveryAreaAndRouting = new HashMap<DeliveryArea, HashMap<Routing, Integer>>();
		HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting = new HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>>();
		int maxDeletedPerRouting = 0;
		int minDeletedPerRouting = Integer.MAX_VALUE;
		double averageDeleted = 0.0;
		

		// Remove elements for more flexibility
		 double minimumSlack =LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLat1(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLon1(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLat2(),
						deliveryAreaSet.getElements().get(0).getSubset().getElements().get(0).getLon2())
						* AggregateReferenceInformationAlgorithm.distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * TIME_MULTIPLIER;

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			this.aggregatedReferenceInformationNo.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
		}

		for (Routing routing : this.targetRoutingResults) {
			int deleted = 0;
			bufferFirstAndLastRePerRouting.put(routing,
					new HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>());
			HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();
			// HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow,
			// Double>>> distance = new HashMap<DeliveryArea,
			// HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>();

			for (Route r : routing.getRoutes()) {

				bufferFirstAndLastRePerRouting.get(routing).put(r, new HashMap<TimeWindow, ArrayList<RouteElement>>());

				DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
						r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
				if (!count.containsKey(area)) {
					count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
					// distance.put(area, new HashMap<DeliveryArea,
					// HashMap<TimeWindow, Double>>());
				}
				for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
					RouteElement re = r.getRouteElements().get(reId);
					re.setPosition(reId + 1);

					TimeWindow relevantTw = re.getOrder().getTimeWindowFinal();
					

					Customer cus = re.getOrder().getOrderRequest().getCustomer();
					DeliveryArea subArea = LocationService
							.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
					cus.setTempDeliveryArea(subArea);
					if (!count.get(area).containsKey(subArea)) {
						count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
						// distance.get(area).put(subArea, new
						// HashMap<TimeWindow, Double>());
					}

					// If time window set is overlapping, assign to new time
					// window
					if (this.timeWindowSet.getOverlapping()) {
						boolean foundAssignment = false;
						for (TimeWindow tw : this.oldToNewTimeWindowMapping.get(re.getOrder().getTimeWindowFinal())) {
							if (re.getServiceBegin() >= (tw.getStartTime()
									- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
									&& re.getServiceBegin() < (tw.getEndTime()
											- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER) {
								if (!count.get(area).get(subArea).containsKey(tw)) {
									count.get(area).get(subArea).put(tw, 1);

								} else {
									count.get(area).get(subArea).put(tw, count.get(area).get(subArea).get(tw) + 1);
								}
								relevantTw = tw;
								re.setTimeWindow(relevantTw);
								foundAssignment = true;
								break;
							}
						}
						if (!foundAssignment)
							System.out.println("Assignment does not work");

					} else {
						if (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal())) {
							count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);

						} else {
							count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
									count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) + 1);

						}
					}

					// Buffer first and last re per time window
					if (!bufferFirstAndLastRePerRouting.get(routing).get(r).containsKey(relevantTw)) {
						ArrayList<RouteElement> res = new ArrayList<RouteElement>();
						res.add(re);
						bufferFirstAndLastRePerRouting.get(routing).get(r).put(relevantTw, res);

					} else if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).size() < 2) {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).add(1, re);
					} else {
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).remove(1);
						bufferFirstAndLastRePerRouting.get(routing).get(r).get(relevantTw).add(1, re);
					}
					
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
						travelTimeAlternative = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
								latBefore, lonBefore, latAfter, lonAfter)
								* AggregateReferenceInformationAlgorithm.distanceMultiplierAsTheCrowFlies
								/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					double additionalCost = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeAlternative;
					re.setTempAdditionalCostsValue(additionalCost);
				}
			}

			for (DeliveryArea a : this.aggregatedReferenceInformationNo.keySet()) {
				if (!this.acceptablePerDeliveryAreaAndRouting.containsKey(a))
					this.acceptablePerDeliveryAreaAndRouting.put(a, new HashMap<Routing, Integer>());
				this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing, 0);
				this.aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));

				// double distanceSum = 0.0;
				for (DeliveryArea area : count.get(a).keySet()) {
					for (TimeWindow tw : count.get(a).get(area).keySet()) {
						this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing,
								this.acceptablePerDeliveryAreaAndRouting.get(a).get(routing)
										+ count.get(a).get(area).get(tw));

					}
				}

			}

			for (Route r : bufferFirstAndLastRePerRouting.get(routing).keySet()) {

				ArrayList<RouteElement> routeCopy = new ArrayList<RouteElement>();
				for (RouteElement re : r.getRouteElements()) {
					routeCopy.add(re.copyElement());
				}
				ArrayList<TimeWindow> alreadyDeleted = new ArrayList<TimeWindow>();
				if(this.timeWindowSet.getOverlapping()){
					
					deleted += AggregateReferenceInformationAlgorithm.deleteSimpleSlackBasedOverlapping(routing, r, routeCopy, 
							aggregatedReferenceInformationNo, this.acceptablePerDeliveryAreaAndRouting, bufferFirstAndLastRePerRouting, minimumSlack, 
							true, null, alreadyDeleted, timeWindowSetOverlappingDummy, timeWindowSet,this.arrivalProbability * this.orderHorizonLength, 
							expectedServiceTime, daWeightsLower, maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, region,
							r.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet(), newToOldTimeWindowMapping, oldToNewTimeWindowMapping);
				}else{
					deleted += AggregateReferenceInformationAlgorithm.deleteSimpleSlackBased(routing, r, routeCopy, 
							aggregatedReferenceInformationNo, this.acceptablePerDeliveryAreaAndRouting, bufferFirstAndLastRePerRouting, minimumSlack, 
							true, null, alreadyDeleted, timeWindowSet, this.arrivalProbability * this.orderHorizonLength, 
							expectedServiceTime, daWeightsLower, maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow, region,
							r.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet());
				}
			
				r.setRouteElements(routeCopy);
				// System.out.println("");
			}
			if (deleted < minDeletedPerRouting) {
				minDeletedPerRouting = deleted;
			}
			if (deleted > maxDeletedPerRouting) {
				maxDeletedPerRouting = deleted;
			}
			
			averageDeleted+=deleted;
			

		}
		System.out.println("Min deleted: " +minDeletedPerRouting);
		System.out.println("Max deleted: " +maxDeletedPerRouting);
		System.out.println("Avg deleted: " +averageDeleted/this.previousRoutingResults.size());
		
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
		int countLeftOvers = 0;
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> leftOvers : aggregateInformationNo) {
			// Go through all combinations of area and time window
			double penaltyRouting = 0.0;
			boolean consideredReduction = false;
			for (DeliveryArea subArea : leftOvers.keySet()) {
				for (TimeWindow tw : leftOvers.get(subArea).keySet()) {
					// Add to penalty, if capacity is left-over
					if (leftOvers.get(subArea).get(tw) > 0) {
						if (areaToConsiderReduction != null && leftOvers.get(subArea).get(tw) == 1
								&& areaToConsiderReduction.equals(subArea)
								&& timeWindowToConsiderReduction.equals(tw)) {
							consideredReduction = true;
						} else {

							int numberLeftOver = leftOvers.get(subArea).get(tw);
							if (areaToConsiderReduction != null && areaToConsiderReduction.equals(subArea)
									&& timeWindowToConsiderReduction.equals(tw)) {
								numberLeftOver = numberLeftOver - 1;
								consideredReduction = true;
							}
							countLeftOvers += numberLeftOver;
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
				countLeftOvers -= 1;
			}

			if (penaltyRouting < maxPenalty)
				maxPenalty = penaltyRouting;
		}

		return maxPenalty;
	}

	private double determineLeftOverPenaltyAreaSpecific(DeliveryArea area, DeliveryArea subArea,
			ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			TimeWindow timeWindowToConsiderReduction) {
		double maxPenalty = 0.0;

		// Find left over routing with highest penalty costs
		for (HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> leftOvers : aggregateInformationNo) {
			// Go through all time windows
			double penaltyRouting = 0.0;
			boolean consideredReduction = false;
			for (TimeWindow tw : leftOvers.get(subArea).keySet()) {
				// Add to penalty, if capacity is left-over
				if (leftOvers.get(subArea).get(tw) > 0) {
					if (timeWindowToConsiderReduction != null && leftOvers.get(subArea).get(tw) == 1
							&& timeWindowToConsiderReduction.equals(tw)) {
						consideredReduction = true;
					} else {

						int numberLeftOver = leftOvers.get(subArea).get(tw);
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

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

	private static double calculatePenalty(HashMap<TimeWindow, Integer> remainingCapacityPerTw, DeliveryArea subArea,
			ArrayList<DemandSegment> demandSegments, HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValue, HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLower,
			HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow) {
		double penalty = 0.0;
		Collections.sort(demandSegments,
				new DemandSegmentsExpectedValueAscComparator(maximumRevenueValue, objectiveSpecificValues));

		for (TimeWindow tw : remainingCapacityPerTw.keySet()) {
			if (remainingCapacityPerTw.get(tw) > 0) {

				// Punish the remaining capacity with the value of the
				// lowest requesting segment
				for (DemandSegment s : daSegmentWeightingsLower.get(subArea).keySet()) {

					if (daSegmentWeightingsLower.get(subArea).get(s) > 0) {
						if (maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(s.getId()).get(tw.getId()) > 0) {
							penalty += CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
									objectiveSpecificValues, s)
									* minimumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(s.getId())
											.get(tw.getId())
									* remainingCapacityPerTw.get(tw);
							break;
						}
					}
				}
			}
		}

		return penalty;
	}

	public ValueFunctionApproximationModelSet getResult() {

		return modelSet;
	}

	private Integer deleteSimpleSlackBased(Routing routing, Route r, ArrayList<RouteElement> routeCopy,
			HashMap<Routing, HashMap<Route, HashMap<TimeWindow, ArrayList<RouteElement>>>> bufferFirstAndLastRePerRouting,
			double minimumSlack, ArrayList<TimeWindow> alreadyDeleted) {
		double lowestSlack = Double.MAX_VALUE;
		TimeWindow twSlack = null;
		TimeWindow earliest = null;
		// Determine lowest free slack
		int amountDeleted = 0;
		for (TimeWindow tw : bufferFirstAndLastRePerRouting.get(routing).get(r).keySet()) {
			int ind = timeWindowSet.getElements().indexOf(tw);
			timeWindowSet.getElements().get(ind).setTempFollower(null);
			timeWindowSet.getElements().get(ind).setTempSlackFollower(null);
			bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).add(
					routeCopy.get(bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(0).getPosition() - 1));
			bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).remove(0);
			if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).size() > 1) {
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).add(routeCopy
						.get(bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(0).getPosition() - 1));
				bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).remove(0);
				double currentSlack = (tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
						- bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(1).getServiceBegin();
				if (currentSlack < lowestSlack) {
					lowestSlack = currentSlack;
					twSlack = tw;
				}
				if (currentSlack < minimumSlack && (earliest == null || earliest.getStartTime() > tw.getStartTime())) {
					earliest = tw;
				}
			}
		}

		while (lowestSlack < minimumSlack) {
			twSlack = earliest;
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
					double ratio = cap
							/ (this.daWeightsLower.get(area) * this.arrivalProbability * this.orderHorizonLength);
					TimeWindow relevantTw = twSlack;
					if (timeWindowSet.getOverlapping())
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
			if (timeWindowSet.getOverlapping()) {
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

			}

			acceptablePerDeliveryAreaAndRouting
					.get(bestDeletionOption.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()
							.getDeliveryAreaOfSet())
					.put(routing,
							acceptablePerDeliveryAreaAndRouting.get(bestDeletionOption.getOrder().getOrderRequest()
									.getCustomer().getTempDeliveryArea().getDeliveryAreaOfSet()).get(routing) - 1);

			AggregateReferenceInformationAlgorithm.updateRoute(routeCopy, indexBest, timeWindowSet,
					r.getVehicleAssignment().getStartingLocationLat(),
					r.getVehicleAssignment().getStartingLocationLon(), r.getVehicleAssignment().getEndingLocationLat(),
					r.getVehicleAssignment().getEndingLocationLon(), region, TIME_MULTIPLIER);

			lowestSlack = Double.MAX_VALUE;
			twSlack = null;
			earliest = null;
			// Determine lowest free slack
			for (TimeWindow tw : bufferFirstAndLastRePerRouting.get(routing).get(r).keySet()) {
				if (bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).size() > 1) {
					double currentSlack = (tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER
							- bufferFirstAndLastRePerRouting.get(routing).get(r).get(tw).get(1).getServiceBegin();
					if (currentSlack < lowestSlack) {
						lowestSlack = currentSlack;
						twSlack = tw;
					}
					if (currentSlack < minimumSlack
							&& (earliest == null || earliest.getStartTime() > tw.getStartTime())) {
						earliest = tw;
					}
				}
			}

		}

		return amountDeleted;
	}

}
