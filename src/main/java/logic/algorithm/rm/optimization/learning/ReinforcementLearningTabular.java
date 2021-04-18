package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

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
import data.entity.Region;
import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.service.support.ValueFunctionApproximationService;
import logic.utility.MultiDimensionalArrayProducer;
import logic.utility.SubsetProducer;

/**
 * ReinforcementLearningTabular
 * 
 * @author M. Lang
 *
 */
public class ReinforcementLearningTabular implements ValueFunctionApproximationAlgorithm {

	private static double TIME_MULTIPLIER = 60.0;
	private static double DISCOUNT_FACTOR=1.0;
	private static double E_GREEDY_VALUE = 0.7;
	private static int maximumAcceptableTw = 10;
	private static int distanceBucketNo = 4;
	private static int timeBucketNo = 4;
	private static int areaValueBucketNo = 4;
	private static boolean includeDepotInAllTimeWindows = true;
	private static boolean possiblyLargeOfferSet = true;
	private static int maximumAcceptableOverall = 80;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> LOG_LOSS_FUNCTION;
	private HashMap<Integer, HashMap<Integer, ArrayList<String>>> LOG_WEIGHTS;
	private double stepSize;
	private Region region;
	private ValueFunctionApproximationModelSet modelSet; // result
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private DeliveryAreaSet deliveryAreaSet;
	private TimeWindowSet timeWindowSet;
	private AlternativeSet alternativeSet;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;
	private Double expectedServiceTime;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, ArrayList<Customer>>> acceptedCustomersPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;
	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;
	private boolean usepreferencesSampled;
	private boolean useActualBasketValue;
	private boolean initialiseProblemSpecific;
	private Double annealingTemperature;
	private int explorationStrategy;
	private HashMap<DeliveryArea, Object[]> timeCoefficientsPerDeliveryArea;
	private HashMap<DeliveryArea, Double> distanceDivisor;
	private Double timeDivisor;
	private Double areaValueDivisor;
	private HashMap<DeliveryArea, Double> maximumAreaValue;
	private HashMap<DeliveryArea, HashMap<DeliveryArea, Double>> deliveryAreaValue;
	private HashMap<DeliveryArea, HashMap<Integer, Integer>> twToDimensionMapper;
	private HashMap<DeliveryArea, double[]> currentStatePerDeliveryArea;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> initialValuePerDeliveryArea;
	private int arrivalProcessId;
	private DemandSegmentWeighting demandSegmentWeighting;

	private static String[] paras = new String[] { "Constant_service_time", "stepsize_adp_learning",
			"actualBasketValue", "samplePreferences", "includeDriveFromStartingPosition",
			"initialiseCoefficientsProblemSpecific", "annealing_temperature_(Negative:no_annealing)",
			"exploration_(0:on-policy,1:wheel,2:e-greedy)" };

	public ReinforcementLearningTabular(Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, DeliveryAreaSet deliveryAreaSet,
			DemandSegmentWeighting demandSegmentWeighting, Double expectedServiceTime,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue, double stepSize,
			Double includeDriveFromStartingPosition, int orderHorizonLength, int arrivalProcessId,
			Double samplePreferences, Double actualBasketValue, Double initialiseProblemSpecific,
			Double annealingTemperature, Double explorationStrategy,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings,
			HashMap<DeliveryArea, Double> daWeights) {
		this.region = region;
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.deliveryAreaSet = deliveryAreaSet;
		this.timeWindowSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.alternativeSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet();
		this.daSegmentWeightings = daSegmentWeightings;
		this.daWeights = daWeights;
		this.expectedServiceTime = expectedServiceTime;
		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.stepSize = stepSize;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.explorationStrategy = explorationStrategy.intValue();
		this.arrivalProcessId = arrivalProcessId;
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

		if (initialiseProblemSpecific == 1.0) {
			this.initialiseProblemSpecific = true;
		} else {
			this.initialiseProblemSpecific = false;
		}

		this.annealingTemperature = annealingTemperature;

	};

	public void start() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		this.timeWindowSet.sortElementsAsc();

		// Separate into delivery areas for parallel computing

		this.prepareOrderRequestsForDeliveryAreas();
		this.prepareVehicleAssignmentsForDeliveryAreas();

		this.initialiseTablesAndCurrentStateForDeliveryAreas();

		this.LOG_LOSS_FUNCTION = new HashMap<Integer, HashMap<Integer, ArrayList<Double>>>();
		this.LOG_WEIGHTS = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();

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
			model.setSubAreaModel(
					MultiDimensionalArrayProducer.arrayToString(this.timeCoefficientsPerDeliveryArea.get(area)));
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

	private void initialiseTablesAndCurrentStateForDeliveryAreas() {

		this.timeCoefficientsPerDeliveryArea = new HashMap<DeliveryArea, Object[]>();
		this.twToDimensionMapper = new HashMap<DeliveryArea, HashMap<Integer, Integer>>();
		this.currentStatePerDeliveryArea = new HashMap<DeliveryArea, double[]>();
		this.distanceDivisor = new HashMap<DeliveryArea, Double>();
		this.timeDivisor = this.orderHorizonLength / (double) timeBucketNo;
		this.areaValueDivisor = maximumAcceptableOverall / (double) areaValueBucketNo;

		// Subareas
		this.maximumAreaValue = new HashMap<DeliveryArea, Double>();
		this.deliveryAreaValue = new HashMap<DeliveryArea, HashMap<DeliveryArea, Double>>();
		HashMap<DeliveryArea, Double> deliveryAreaWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, HashMap<DemandSegment, Double>> weightings = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();

		LocationService
				.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryAreaAndDemandSegmentConsideringHierarchy(
						deliveryAreaWeights, weightings, this.deliveryAreaSet, this.demandSegmentWeighting);

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			double distanceDivisor = (3.0 * (double) this.timeWindowSet.getLengthOfLongestTimeWindow(TIME_MULTIPLIER)
					- this.expectedServiceTime * 5.0) * (this.region.getAverageKmPerHour() / TIME_MULTIPLIER)
					* (double) this.vehicleAssignmentsPerDeliveryArea.get(area.getId()).size()
					/ (double) distanceBucketNo;

			this.distanceDivisor.put(area, distanceDivisor);

			HashMap<Integer, Integer> mapper = new HashMap<Integer, Integer>();
			int[] numberOfDimensions = new int[this.timeWindowSet.getElements().size() + 2];
			int dimensionNo = 0;
			// For time
			numberOfDimensions[dimensionNo++] = distanceBucketNo;
			// For time windows
			for (TimeWindow tw : this.timeWindowSet.getElements()) {

				numberOfDimensions[dimensionNo] = distanceBucketNo;
				mapper.put(tw.getId(), dimensionNo);
				dimensionNo++;
			}
			// For area potential
			numberOfDimensions[dimensionNo] = distanceBucketNo;

			twToDimensionMapper.put(area, mapper);

			// timeCoefficientsPerDeliveryArea.put(area,
			// MultiDimensionalArrayProducer.createDoublePairArray(numberOfDimensions));
			timeCoefficientsPerDeliveryArea.put(area,
					MultiDimensionalArrayProducer.createDoubleArray(numberOfDimensions));

			// Delivery area value
			double maximumV = 0.0;
			HashMap<DeliveryArea, Double> areaValues = new HashMap<DeliveryArea, Double>();
			for (DeliveryArea subArea : deliveryAreaWeights.keySet()) {
				if (subArea.getDeliveryAreaOfSet().getId() == area.getId()) {
					double daValue = 0.0;
					for (DemandSegment d : weightings.get(subArea).keySet()) {
						daValue += deliveryAreaWeights.get(subArea) * weightings.get(subArea).get(d)
								* CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
										objectiveSpecificValues, d);
					}
					if (maximumV < daValue)
						maximumV = daValue;
					areaValues.put(subArea, daValue);
				}
			}
			this.maximumAreaValue.put(area, maximumV);
			this.deliveryAreaValue.put(area, areaValues);

			// How to initialise the time coefficients?
			this.initialValuePerDeliveryArea = new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>();

			HashMap<TimeWindow, Double> valuePerTw = new HashMap<TimeWindow, Double>();

			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				if (this.initialiseProblemSpecific) {

					// Take expected order value (weighted by segment weights)
					double weightedValue = 0.0;
					double weightedValueWithoutProbs = 0.0;
					AlternativeOffer relevantOffer = null;
					for (DemandSegmentWeight w : this.daSegmentWeightings.get(area).getWeights()) {
						boolean inConsiderationSet = false;
						ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
						for (ConsiderationSetAlternative alt : w.getDemandSegment().getConsiderationSet()) {
							if (!alt.getAlternative().getNoPurchaseAlternative()) {

								if (alt.getAlternative().getTimeWindows().get(0).getId() == tw.getId()) {
									AlternativeOffer altO = new AlternativeOffer();
									altO.setAlternative(alt.getAlternative());
									altO.setAlternativeId(alt.getAlternativeId());
									offers.add(altO);
									inConsiderationSet = true;
									relevantOffer = altO;
								}
							}

						}

						if (inConsiderationSet) {

							HashMap<AlternativeOffer, Double> probs = CustomerDemandService
									.getProbabilitiesForModel(offers, w.getDemandSegment());
							weightedValue += CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
									objectiveSpecificValues, w.getDemandSegment()) * w.getWeight()
									* probs.get(relevantOffer);
							weightedValueWithoutProbs += CustomerDemandService.calculateExpectedValue(
									maximumRevenueValue, objectiveSpecificValues, w.getDemandSegment()) * w.getWeight();
						}

					}

					double finalValue = Math.min(
							weightedValue * this.orderHorizonLength * this.daWeights.get(area)
									* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId),
							weightedValueWithoutProbs * maximumAcceptableTw);

					valuePerTw.put(tw, finalValue);
				} else {
					valuePerTw.put(tw, 0.0);
				}
			}

			this.initialValuePerDeliveryArea.put(area, valuePerTw);
		}

	}

	private void initialiseState(DeliveryArea area) {
		double[] currentState = new double[this.timeWindowSet.getElements().size()+2];

		int dimensionNo = 0;
		currentState[dimensionNo++]=this.orderHorizonLength;
		for (TimeWindow tw : this.timeWindowSet.getElements()) {

			currentState[dimensionNo] = 0.0;
			dimensionNo++;

			this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).put(tw.getId(),
					new ArrayList<Customer>());

		}
		currentState[dimensionNo]=0;
		this.currentStatePerDeliveryArea.put(area, currentState);

	}

	private void prepareOrderRequestsForDeliveryAreas() {

		// Initialise Hashmaps
		this.orderRequestsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>>();
		this.acceptedCustomersPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, ArrayList<Customer>>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.orderRequestsPerDeliveryArea.put(area.getId(), new HashMap<Integer, HashMap<Integer, OrderRequest>>());
			this.acceptedCustomersPerDeliveryAreaAndTimeWindow.put(area.getId(),
					new HashMap<Integer, ArrayList<Customer>>());

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

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());
		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);
		}

	}

	private void applyADPForDeliveryArea(DeliveryArea area) {

		// Per order request set, train the value function

		for (Integer requestSetId : this.orderRequestsPerDeliveryArea.get(area.getId()).keySet()) {

			this.updateValueFunctionWithOrderRequests(area, requestSetId);
		}

	}

	private void updateValueFunctionWithOrderRequests(DeliveryArea area, Integer requestSetId) {
		System.out.println("Next request set: " + requestSetId);

		// Initialise routes for dynamic feasibility check
		HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(area,
				this.vehicleAssignmentsPerDeliveryArea, this.timeWindowSet, TIME_MULTIPLIER, this.region,
				(includeDriveFromStartingPosition==1));
		this.initialiseState(area);
		this.addStartPositionsToAcceptedOrders(area);

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

		// Go through requests and update value function
		for (int t = this.orderHorizonLength; t > 0; t--) {

			OrderRequest request;
			if (this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).containsKey(t)) {
				request = this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).get(t);
			} else {
				request = null;
			}

			// Check feasible time windows
			HashMap<Integer, RouteElement> feasibleTimeWindows = null;
			if (request != null) {
				feasibleTimeWindows = DynamicRoutingHelperService
						.getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(request, routes,
								vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), region, TIME_MULTIPLIER,
								expectedServiceTime, timeWindowSet,  (includeDriveFromStartingPosition==1));
			}

			// Choose offer set and update value function
			RouteElement newElement = this.updateValueFunctionIndividualWithOrderRequest(t, area, request,
					feasibleTimeWindows, routes, requestSetId, alternativesToTimeWindows);

			// If customer chose a time window, he needs to be added to the
			// current route
			if (newElement != null) {
				DynamicRoutingHelperService.insertRouteElement(newElement, routes,
						vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), timeWindowSet, TIME_MULTIPLIER,
						 (includeDriveFromStartingPosition==1));
			}

		}

		if (requestSetId > 5000)
			System.out.println("Routes are full?");

	}

	private RouteElement updateValueFunctionIndividualWithOrderRequest(int t, DeliveryArea area, OrderRequest request,
			HashMap<Integer, RouteElement> feasibleTimeWindows, HashMap<Integer, ArrayList<RouteElement>> routes,
			int requestSetId, HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		// Calculate value of same state in next time step
		// If at end of order horizon, the value is 0
		int[] currentState = this.translateCurrentState(this.currentStatePerDeliveryArea.get(area), area);
		if (MultiDimensionalArrayProducer.readDoubleArray(this.timeCoefficientsPerDeliveryArea.get(area),
				currentState) == null) {
			MultiDimensionalArrayProducer.writeToDoubleArray(
					 this.calculateCurrentInitialTimeCoefficientValue(currentState, area),
					this.timeCoefficientsPerDeliveryArea.get(area), currentState);
		}

		double noAssignmentValue = ValueFunctionApproximationService.evaluateStateForTabularValueFunctionApproximation(
				this.timeCoefficientsPerDeliveryArea.get(area), currentState, t - 1, this.orderHorizonLength);

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> selectedOfferedAlternatives = new ArrayList<AlternativeOffer>();
		DeliveryArea subArea = null;
		if (request != null) {

			subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
					request.getCustomer());
			subArea.setSetId(area.getSubsetId());
			// For all feasible, assume you accept -> get value
			int[] oldState = this.translateCurrentState(this.currentStatePerDeliveryArea.get(area), area);
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			for (Integer twId : feasibleTimeWindows.keySet()) {

				double[] updatedState = this.currentStatePerDeliveryArea.get(area).clone();

				updatedState = this.calculateNewStateValue(area, twId, updatedState, request, t - 1, subArea);

				int[] newState = this.translateCurrentState(updatedState, area);
				TimeWindow tw = this.timeWindowSet.getTimeWindowById(twId);

				if (MultiDimensionalArrayProducer.readDoubleArray(this.timeCoefficientsPerDeliveryArea.get(area),
						newState) == null) {
					MultiDimensionalArrayProducer.writeToDoubleArray(

							this.calculateCurrentInitialTimeCoefficientValue(newState, area),
							this.timeCoefficientsPerDeliveryArea.get(area), newState);
				}

				if (newState[this.twToDimensionMapper.get(area).get(twId)] < oldState[this.twToDimensionMapper.get(area)
						.get(twId)])
					System.out.println("Should not happen!");
				double assignmentValue = ValueFunctionApproximationService
						.evaluateStateForTabularValueFunctionApproximation(
								this.timeCoefficientsPerDeliveryArea.get(area), newState, t - 1,
								this.orderHorizonLength);
				twValue.put(tw, assignmentValue);
			}

			// Find best subset from the time windows with value add

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue, maximumRevenueValue, objectiveSpecificValues, algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue, false, null, null, DISCOUNT_FACTOR);
			maxValue = bestOffer.getValue();
			selectedOfferedAlternatives = bestOffer.getKey();
		}
		// Update value function approximation
		double newValueApproximation = maxValue + noAssignmentValue;

		// TODO as enhancement: annealing () search-then-converge schedule µ(t)
		// = µ(0)/(1 + t/T) and momentum vi+1=αvi+θi+1 (vi: update vektor, θ:
		// update vektor without momentum)

		double currentStepSize = stepSize;
		if (this.annealingTemperature > 0) {
			currentStepSize = stepSize / (1.0 + (double) requestSetId / this.annealingTemperature);
		}

		int[] state = this.translateCurrentState(this.currentStatePerDeliveryArea.get(area), area);

		ValueFunctionApproximationService.updateTabularValueFunctionApproximation(
				this.timeCoefficientsPerDeliveryArea.get(area), state, t, this.orderHorizonLength,
				newValueApproximation, this.LOG_LOSS_FUNCTION.get(area.getId()), requestSetId, currentStepSize);

		// Simulate customer decision
		if (request != null) {
			/// Choose offer set
			if (feasibleTimeWindows.size() > 0) {
				if (this.explorationStrategy == 2) {
					// E-greedy search:also allows non-value-add offers
					ArrayList<Set<Integer>> possibleSets = new ArrayList<Set<Integer>>();
					possibleSets.addAll(SubsetProducer.powerSet(feasibleTimeWindows.keySet()));

					Double[] probabilities = new Double[possibleSets.size()];

					/// All but best offer set get same fraction of non-greedy
					/// probability;
					double probForNotBest = (1.0 - E_GREEDY_VALUE) / (possibleSets.size() - 1.0);
					probabilities[0] = E_GREEDY_VALUE;

					for (int i = 1; i < probabilities.length; i++) {
						probabilities[i] = probForNotBest;
					}

					int selectedGroup = 0;

					try {
						selectedGroup = ProbabilityDistributionService
								.getRandomGroupIndexByProbabilityArray(probabilities);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					/// If the selected Offer set is not the best offer set
					if (selectedGroup != 0) {

						selectedGroup = selectedGroup - 1;

						/// Choose the selected subset. If it is equal to
						/// the best set or - in case the best set is the empty
						/// set-
						/// the empty set, than choose the last
						Set<Integer> selectedSet = possibleSets.get(selectedGroup);

						/// Equal?
						boolean equal = (selectedOfferedAlternatives.size() > 0);
						if (equal == true)
							equal = (selectedSet.size() == selectedOfferedAlternatives.size());
						if (equal == true) {
							for (AlternativeOffer ao : selectedOfferedAlternatives) {
								if (!selectedSet.contains(ao.getAlternative().getTimeWindows().get(0).getId())) {
									equal = false;
									break;
								}
							}
						}
						if (equal == true || (selectedOfferedAlternatives.size() == 0 && selectedSet.size() == 0))
							selectedSet = possibleSets.get(possibleSets.size() - 1);
						// Determine selection probabilities for the
						// alternatives of
						// the
						// respective subset
						for (Integer index : selectedSet) {
							AlternativeOffer offer = new AlternativeOffer();
							TimeWindow tw = this.timeWindowSet.getTimeWindowById(index);
							offer.setAlternative(
									alternativesToTimeWindows.get(tw));
							offer.setAlternativeId(
									alternativesToTimeWindows.get(tw).getId());
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

					double[] updatedState = this.currentStatePerDeliveryArea.get(area);
					int[] oldState = this.translateCurrentState(updatedState, area);
					updatedState = this.calculateNewStateValue(area, twId, updatedState, request, request.getArrivalTime(), subArea);
					int[] newState = this.translateCurrentState(updatedState, area);
					this.currentStatePerDeliveryArea.put(area, updatedState);
					this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId)
							.add(request.getCustomer());
					return feasibleTimeWindows.get(twId);
				}

			}
		}
		return null;

	}

	private double[] calculateNewStateValue(DeliveryArea area, int twId, double[] updatedState, OrderRequest request,
			int time, DeliveryArea subArea) {
		// Time
		updatedState[0] = time;

		// Distances
		double distance = 0.0;
		int customerCount = 0;
		for (Customer cus : this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId)) {
			distance += LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(), cus);

		}
		customerCount = this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId).size();
		int respectiveId = 0;
		for (int i = 0; i < this.timeWindowSet.getElements().size(); i++) {
			if (twId == this.timeWindowSet.getElements().get(i).getId()) {
				respectiveId = i;
			}
		}

		int customerCountBefore = 0;
		int customerCountBeforer = 0;
		double distanceBefore = 0.0;
		boolean before = false;

		if (respectiveId > 0) {
			before = true;
			for (Customer cus : this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
					.get(this.timeWindowSet.getElements().get(respectiveId - 1).getId())) {
				distanceBefore += LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						cus);

			}
			customerCountBefore = this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
					.get(this.timeWindowSet.getElements().get(respectiveId - 1).getId()).size();
			if (respectiveId - 1 > 0)
				customerCountBeforer += this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
						.get(this.timeWindowSet.getElements().get(respectiveId - 2).getId()).size();
		}

		int customerCountAfter = 0;
		int customerCountAfterer = 0;
		double distanceAfter = 0.0;
		boolean after = false;
		if (respectiveId < this.timeWindowSet.getElements().size() - 1) {
			after = true;
			for (Customer cus : this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
					.get(this.timeWindowSet.getElements().get(respectiveId + 1).getId())) {
				distanceAfter += LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(), cus);

			}
			customerCountAfter += this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
					.get(this.timeWindowSet.getElements().get(respectiveId + 1).getId()).size();
			if (respectiveId < this.timeWindowSet.getElements().size() - 2)
				customerCountAfterer += this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId())
						.get(this.timeWindowSet.getElements().get(respectiveId + 2).getId()).size();
		}

		double newValue = 0.0;

		int currentSum = customerCount + customerCountBefore + customerCountAfter;
		double currentDistance = distance + distanceAfter + distanceBefore;
		if (currentSum > 0) {
			newValue = ((updatedState[this.twToDimensionMapper.get(area).get(twId)] * (currentSum - 1.0))
					+ 2.0 * currentDistance) / currentSum;
		} else {
			// TODO: Change?
			newValue = currentDistance;
		}

		updatedState[this.twToDimensionMapper.get(area).get(twId)] = newValue;

		currentSum = customerCount + customerCountBefore + customerCountBeforer;
		currentDistance = distanceBefore;
		if (currentSum > 0 && before) {
			newValue = ((updatedState[this.twToDimensionMapper.get(area)
					.get(this.timeWindowSet.getElements().get(respectiveId - 1).getId())] * (currentSum - 1.0))
					+ 2.0 * currentDistance) / currentSum;
		} else {
			// TODO: Change?
			newValue = currentDistance;
		}

		if (before)
			updatedState[this.twToDimensionMapper.get(area)
					.get(this.timeWindowSet.getElements().get(respectiveId - 1).getId())] = newValue;

		currentSum = customerCount + customerCountAfter + customerCountAfterer;
		currentDistance = distanceAfter;
		if (currentSum > 0 && after) {
			newValue = ((updatedState[this.twToDimensionMapper.get(area)
					.get(this.timeWindowSet.getElements().get(respectiveId + 1).getId())] * (currentSum - 1.0))
					+ 2.0 * currentDistance) / currentSum;
		} else {
			// TODO: Change?
			newValue = currentDistance;
		}

		if (after)
			updatedState[this.twToDimensionMapper.get(area)
					.get(this.timeWindowSet.getElements().get(respectiveId + 1).getId())] = newValue;

		// Area values
		updatedState[updatedState.length - 1] = updatedState[updatedState.length - 1]
				+ this.deliveryAreaValue.get(area).get(subArea) / this.maximumAreaValue.get(area);
		return updatedState;
	}

	private void addStartPositionsToAcceptedOrders(DeliveryArea area) {

		for (VehicleAreaAssignment ass : this.vehicleAssignmentsPerDeliveryArea.get(area.getId())) {
			// Determine relevant time window (first applicable)
			boolean startFound = false;
			boolean endFound = false;
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				if (ass.getStartTime() >= tw.getStartTime() && ass.getStartTime() <= tw.getEndTime()) {
					Customer customer = new Customer();
					customer.setLat(ass.getStartingLocationLat());
					customer.setLon(ass.getStartingLocationLon());
					this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(tw.getId()).add(customer);
					startFound = true;
				}

				if (ass.getEndTime() >= tw.getStartTime() && ass.getEndTime() <= tw.getEndTime()) {
					Customer customer = new Customer();
					customer.setLat(ass.getEndingLocationLat());
					customer.setLon(ass.getEndingLocationLon());
					this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(tw.getId()).add(customer);
					endFound = true;
				}

				if (startFound && !endFound && includeDepotInAllTimeWindows
						&& this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(tw.getId())
								.size() < 1) {
					Customer customer = new Customer();
					customer.setLat(ass.getStartingLocationLat());
					customer.setLon(ass.getStartingLocationLon());
					this.acceptedCustomersPerDeliveryAreaAndTimeWindow.get(area.getId()).get(tw.getId()).add(customer);
				}

				if (startFound && endFound)
					break;
			}
		}
	}

	private int[] translateCurrentState(double[] currentState, DeliveryArea area) {
		int[] currentStateInt = new int[currentState.length];
		int stateTime = (int) (currentState[0] / this.timeDivisor);
		if ((stateTime - (double) currentState[0] / this.timeDivisor >= 0)&& (stateTime>0))
			stateTime = stateTime - 1;
		currentStateInt[0]= stateTime;
		for (int i = 1; i < currentState.length - 1; i++) {

			int state = (int) ((double) currentState[i] / (double) this.distanceDivisor.get(area));
			if ((state - (double) currentState[i] / (double) this.distanceDivisor.get(area) >= 0)&& (state >0))
				state--;
			currentStateInt[i] = state;
		}

		int areaState = (int) (currentState[currentState.length - 1] / this.areaValueDivisor);
		if ((areaState
				- (double) currentState[currentState.length - 1] / this.areaValueDivisor >= 0) && (areaState >0))
			areaState = areaState - 1;
		currentState[currentState.length - 1] =areaState;
		return currentStateInt;
	}

	private double calculateCurrentInitialTimeCoefficientValue(int[] currentState, DeliveryArea area) {
		double initialValue = 0.0;
		double overallCapacityUse = 0.0;
		for (TimeWindow tw : this.timeWindowSet.getElements()) {
			overallCapacityUse += currentState[this.twToDimensionMapper.get(area).get(tw.getId())];
			initialValue += this.initialValuePerDeliveryArea.get(area).get(tw) / distanceBucketNo
					* (distanceBucketNo - currentState[this.twToDimensionMapper.get(area).get(tw.getId())]);
		}

		overallCapacityUse = overallCapacityUse / (this.timeWindowSet.getElements().size() * distanceBucketNo);
		initialValue = initialValue
				* (1.0 + 0.1 * currentState[this.timeWindowSet.getElements().size() + 1] / areaValueBucketNo);
		double expectedTimeToCome = this.orderHorizonLength - overallCapacityUse * this.orderHorizonLength;
		if (currentState[0] > expectedTimeToCome / this.timeDivisor)
			initialValue = initialValue - ((double) currentState[0] - expectedTimeToCome / this.timeDivisor)
					/ ((double) currentState[0]) * initialValue;
		return initialValue;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public ValueFunctionApproximationModelSet getResult() {

		return modelSet;
	}

}
