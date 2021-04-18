package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.entity.MomentumHelper;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LearningService;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.service.support.ValueFunctionApproximationService;

/**
 * 
 * Inspired by Yang and Strauss with dynamic routing and area potential
 * Yang, X., & Strauss, A. K. (2017). An approximate dynamic programming approach to attended home delivery management.
 * European Journal of Operational Research, 263(3), 935-945.
 * 
 * @author M. Lang
 *
 */
public class ADPAdaptedFromYangStrauss extends AggregateReferenceInformationAlgorithm implements ValueFunctionApproximationAlgorithm {

	private static int numberOfThreads = 3;

	private static double DISCOUNT_FACTOR = 1.0;
	private static double E_GREEDY_VALUE = 0.7;
	private static boolean possiblyLargeOfferSet = true;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> LOG_LOSS_FUNCTION;
	private HashMap<Integer, HashMap<Integer, ArrayList<String>>> LOG_WEIGHTS;
	private double stepSize;
	private ValueFunctionApproximationModelSet modelSet; // result
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;

	private AlternativeSet alternativeSet;

	private Double expectedServiceTime;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas;
	private HashMap<DeliveryArea, Double> daWeightsLowerAreas;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> valueMultiplierPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> sumCoverageValuePerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>> routesCoveringPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> coveringPerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, Double> areaPotentialPerDeliveryArea;
	private HashMap<Integer, Double> remainingCapacityPerDeliveryArea;
	private HashMap<Integer, Double> acceptedCostPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;
	private HashMap<Integer, Double> basicCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> timeCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> timeCapacityInteractionCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> remainingCapacityCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> acceptedOverallCostCoefficientPerDeliveryArea;
	private HashMap<Integer, Double> areaPotentialCoefficientPerDeliveryArea;
	private HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficientsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCostsPerDeliveryArea;
	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;

	private boolean usepreferencesSampled;
	private boolean useActualBasketValue;
	private boolean initialiseProblemSpecific;
	private Double annealingTemperature;
	private boolean withTimeCapacityInteraction;
	private boolean withCapacityInteraction;
	private boolean considerInsertionCosts;
	private boolean considerCoverage;
	private boolean considerOverallRemainingCapacity;
	private boolean considerAreaPotential;
	private int explorationStrategy;
	private double momentumWeight;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;
	private HashMap<Integer, Double> overallAcceptableCostPerDeliveryArea;
	private HashMap<Integer, Double> maximumAreaPotentialPerDeliveryArea;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;

	private DemandSegmentWeighting demandSegmentWeighting;

	private boolean considerOverallAcceptedCost;

	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;

	private HashMap<Integer, HashMap<Integer, Double>> distancePerDeliveryAreaAndRouting;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> distancePerDeliveryAreaAndTwAndRouting;
	private int distanceType;
	private boolean distanceMeasurePerTw;
	private int maximumDistanceMeasureIncrease;
	private int switchDistanceOffPoint;

	private static String[] paras = new String[] { "Constant_service_time", "stepsize_adp_learning",
			"actualBasketValue", "samplePreferences", "includeDriveFromStartingPosition",
			"initialiseCoefficientsProblemSpecific", "annealing_temperature_(Negative:no_annealing)",
			"time_cap_interaction", "cap_cap_interaction", "consider_insertion_costs", "consider_coverage",
			"consider_overall_remaining_capacity", "consider_overall_accepted_insertion_costs",
			"consider_area_potential", "exploration_(0:on-policy,1:wheel,2:e-greedy)", "momentum_weight",
			"no_routing_candidates", "no_insertion_candidates", "distance_type", "distance_measure_per_tw",
			"maximum_distance_measure_increase", "switch_distance_off_point" };

	public ADPAdaptedFromYangStrauss(Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
									 ArrayList<OrderRequestSet> orderRequestSetsForLearning, ArrayList<Routing> previousRoutingResults,
									 DeliveryAreaSet deliveryAreaSet, Double expectedServiceTime,
									 HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue, double stepSize,
									 Double includeDriveFromStartingPosition, int orderHorizonLength, Double samplePreferences,
									 Double actualBasketValue, Double initialiseProblemSpecific, Double annealingTemperature,
									 Double withTimeCapacityInteraction, Double withCapacityInteraction, Double considerInsertionCosts,
									 Double considerCoverage, Double considerOverallRemainingCapacity, Double considerOverallAcceptedCost,
									 Double considerAreaPotential, Double explorationStrategy, Double momentumWeight,
									 DemandSegmentWeighting demandSegmentWeighting, HashMap<DeliveryArea, Double> daWeights,
									 HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings,
									 Double numberPotentialInsertionCandidates, Double numberRoutingCandidates,
									 HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, Double distanceType, Double distanceMeasurePerTw,
									 Double maximumDistanceMeasureIncrease, Double switchDistanceOffPoint) {
		this.maximumDistanceMeasureIncrease = maximumDistanceMeasureIncrease.intValue();
		this.distanceMeasurePerTw = (distanceMeasurePerTw == 1.0);
		this.switchDistanceOffPoint = switchDistanceOffPoint.intValue();
		AggregateReferenceInformationAlgorithm.setRegion(region);
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		AggregateReferenceInformationAlgorithm.setDeliveryAreaSet(deliveryAreaSet);
		AggregateReferenceInformationAlgorithm.setTimeWindowSet(this.orderRequestSetsForLearning.get(0).getCustomerSet()
				.getOriginalDemandSegmentSet().getAlternativeSet().getTimeWindowSet());
		this.alternativeSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet();
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.daSegmentWeightingsLowerAreas = daSegmentWeightings;
		this.daWeightsLowerAreas = daWeights;
		this.expectedServiceTime = expectedServiceTime;
		AggregateReferenceInformationAlgorithm.setMaximumRevenueValue(maximumRevenueValue);
		AggregateReferenceInformationAlgorithm.setObjectiveSpecificValues(objectiveSpecificValues);
		this.stepSize = stepSize;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.explorationStrategy = explorationStrategy.intValue();
		this.momentumWeight = momentumWeight;
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		AggregateReferenceInformationAlgorithm.setPreviousRoutingResults(previousRoutingResults);
		this.neighbors = neighbors;
		this.distanceType = distanceType.intValue();
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

		if (withTimeCapacityInteraction == 1.0) {
			this.withTimeCapacityInteraction = true;
		} else {
			this.withTimeCapacityInteraction = false;
		}
		if (withCapacityInteraction == 1.0) {
			this.withCapacityInteraction = true;
		} else {
			this.withCapacityInteraction = false;
		}

		if (considerInsertionCosts == 1.0) {
			this.considerInsertionCosts = true;
		} else {
			this.considerInsertionCosts = false;
		}

		if (considerCoverage == 1.0) {
			this.considerCoverage = true;
		} else {
			this.considerCoverage = false;
		}

		if (considerOverallRemainingCapacity == 1.0) {
			this.considerOverallRemainingCapacity = true;
		} else {
			this.considerOverallRemainingCapacity = false;
		}

		if (considerOverallAcceptedCost == 1.0) {
			this.considerOverallAcceptedCost = true;
		} else {
			this.considerOverallAcceptedCost = false;
		}

		if (considerAreaPotential == 1.0) {
			this.considerAreaPotential = true;
		} else {
			this.considerAreaPotential = false;
		}
	};

	public void start() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(
				AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER());
		RoutingService.getDeliveryStartTimeByTimeWindowSet(AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		DynamicRoutingHelperService.distanceMultiplierAsTheCrowFlies = AggregateReferenceInformationAlgorithm
				.getDistanceMultiplierAsTheCrowFlies();
		if (this.momentumWeight > 0)
			this.oldMomentumPerDeliveryArea = new HashMap<Integer, MomentumHelper>();

		// Separate into delivery areas for parallel computing
		this.orderRequestsPerDeliveryArea = CustomerDemandService.prepareOrderRequestsForDeliveryAreas(
				this.orderRequestSetsForLearning, AggregateReferenceInformationAlgorithm.getDeliveryAreaSet());
		this.prepareVehicleAssignmentsForDeliveryAreas();

		this.initialiseBuffers();
		this.prepareValueMultiplier2();

		this.LOG_LOSS_FUNCTION = new HashMap<Integer, HashMap<Integer, ArrayList<Double>>>();
		this.LOG_WEIGHTS = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();

		// Map time windows to alternatives
		this.alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		this.aggregateReferenceInformation(false, false);

		// Initialise basic and variable coefficients per delivery area

		this.initialiseCoefficients();

		// Solve problem per delivery area
		ArrayList<ValueFunctionApproximationModel> models = new ArrayList<ValueFunctionApproximationModel>();
		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {
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
			if (this.withTimeCapacityInteraction) {
				model.setTimeCapacityInteractionCoefficient(
						this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()));
			}

			if (this.considerOverallRemainingCapacity) {
				model.setRemainingCapacityCoefficient(
						this.remainingCapacityCoefficientPerDeliveryArea.get(area.getId()));
			}

			if (this.considerOverallAcceptedCost) {
				model.setAcceptedOverallCostCoefficient(
						acceptedOverallCostCoefficientPerDeliveryArea.get(area.getId()));
			}
			if (this.considerAreaPotential) {
				model.setAreaPotentialCoefficient(this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()));
			}

			model.setObjectiveFunctionValueLog(this.LOG_LOSS_FUNCTION.get(area.getId()));
			model.setWeightsLog(this.LOG_WEIGHTS.get(area.getId()));
			models.add(model);

		}

		// Prepare final model set
		ValueFunctionApproximationModelSet set = new ValueFunctionApproximationModelSet();
		set.setDeliveryAreaSetId(getDeliveryAreaSet().getId());
		set.setTimeWindowSetId(getTimeWindowSet().getId());
		set.setTypeId(1); // For linear model
		set.setIsCommitted(true);
		set.setIsNumber(true);
		set.setElements(models);

		this.modelSet = set;

	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumAreaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (DeliveryArea area : this.daWeightsLowerAreas.keySet()) {

			HashMap<Integer, Double> timeWindowMultiplier = new HashMap<Integer, Double>();
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLowerAreas.get(area).getWeights()) {
				double weightedValue = CustomerDemandService.calculateExpectedValue(
						AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
						AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), segW.getDemandSegment());
				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				for (ConsiderationSetAlternative alt : alts) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						double m = this.daWeightsLowerAreas.get(area) * segW.getWeight() * weightedValue * 100.0;
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
		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {
			this.maximumAreaPotentialPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (DeliveryArea area : this.daWeightsLowerAreas.keySet()) {

			double weightedValue = 0.0;
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLowerAreas.get(area).getWeights()) {
				weightedValue += CustomerDemandService.calculateExpectedValue(
						AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
						AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), segW.getDemandSegment())
						* segW.getWeight();
			}

			if (weightedValue > this.maximumAreaPotentialPerDeliveryArea.get(area.getDeliveryAreaOfSet().getId())) {
				this.maximumAreaPotentialPerDeliveryArea.put(area.getDeliveryAreaOfSet().getId(), weightedValue);
			}
			this.valueMultiplierPerDeliveryArea.put(area.getId(), weightedValue);
		}

	}

	private void prepareCoverageMultiplierPerDeliveryAreaAndTimeWindow() {

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

	private void prepareVehicleAssignmentsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaSetAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.overallAcceptableCostPerDeliveryArea = new HashMap<Integer, Double>();

		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());
			if (this.considerOverallRemainingCapacity) {
				this.overallCapacityPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.overallCapacityPerDeliveryArea.put(area.getId(), null);
			}
			if (this.considerOverallAcceptedCost) {
				this.overallAcceptableCostPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.overallAcceptableCostPerDeliveryArea.put(area.getId(), null);
			}

		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);
			if (this.considerOverallRemainingCapacity) {
				double capacity = this.overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
				double additionalCap = Math.min(
						(ass.getEndTime() - ass.getStartTime())
								* AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER()
								+ (this.expectedServiceTime - 1),
						(this.expectedServiceTime - 1) + getTimeWindowSet().getTempLengthOfDeliveryPeriod());
				capacity += additionalCap;
				// TODO: Check if depot travel time should be included
				this.overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);
			}
			if (this.considerOverallAcceptedCost) {
				double capacity = this.overallAcceptableCostPerDeliveryArea.get(ass.getDeliveryAreaId());
				double additionalCap = Math.min(
						(ass.getEndTime() - ass.getStartTime())
								* AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER()
								+ (this.expectedServiceTime - 1),
						(this.expectedServiceTime - 1) + getTimeWindowSet().getTempLengthOfDeliveryPeriod());
				capacity += additionalCap;
				// TODO: Check if depot travel time should be included
				this.overallAcceptableCostPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);
			}
		}

	}

	private void initialiseCoefficients() {

		this.basicCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.timeCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.timeCapacityInteractionCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.variableCoefficientsPerDeliveryArea = new HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>>();
		this.remainingCapacityCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedOverallCostCoefficientPerDeliveryArea = new HashMap<Integer, Double>();
		this.areaPotentialCoefficientPerDeliveryArea = new HashMap<Integer, Double>();

		// Prepare information from TOPS

		// Adjust accepted cost (can be higher with average cost from tops)
		if (this.considerOverallAcceptedCost) {
			for (Integer area : overallAcceptableCostPerDeliveryArea.keySet()) {
				overallAcceptableCostPerDeliveryArea.put(area,
						Math.max(overallAcceptableCostPerDeliveryArea.get(area), maximumAcceptableOverTw
								* AggregateReferenceInformationAlgorithm.averageAdditionalCostPerOrder));
			}
		}

		HashMap<Integer, HashMap<Integer, Double>> probDsTw = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting,
						getTimeWindowSet());
		HashMap<TimeWindow, Double> initValuePerTw = new HashMap<TimeWindow, Double>();
		HashMap<TimeWindow, Double> divisorReductionPerTw = new HashMap<TimeWindow, Double>();
		for (DemandSegmentWeight segW : this.demandSegmentWeighting.getWeights()) {
			double eV = CustomerDemandService.calculateExpectedValue(
					AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), segW.getDemandSegment())
					* segW.getWeight();
			for (TimeWindow tw : getTimeWindowSet().getElements()) {

				if (probDsTw.get(segW.getDemandSegment().getId()).containsKey(tw.getId())
						&& probDsTw.get(segW.getDemandSegment().getId()).get(tw.getId()) > 0) {
					if (!initValuePerTw.containsKey(tw)) {
						initValuePerTw.put(tw, eV * probDsTw.get(segW.getDemandSegment().getId()).get(tw.getId()));
					} else {
						initValuePerTw.put(tw, initValuePerTw.get(tw)
								+ eV * probDsTw.get(segW.getDemandSegment().getId()).get(tw.getId()));
					}
				} else {
					if (divisorReductionPerTw.containsKey(tw)) {
						divisorReductionPerTw.put(tw, divisorReductionPerTw.get(tw) + segW.getWeight());
					} else {
						divisorReductionPerTw.put(tw, segW.getWeight());
					}
				}
			}

		}

		double lowestInitValue = Double.MAX_VALUE;
		for (TimeWindow tw : initValuePerTw.keySet()) {
			if (divisorReductionPerTw.containsKey(tw))
				initValuePerTw.put(tw, initValuePerTw.get(tw) / (1.0 - divisorReductionPerTw.get(tw)));
			if (initValuePerTw.get(tw) < lowestInitValue) {
				lowestInitValue = initValuePerTw.get(tw);
			}
		}

		// Init coefficients

		// TODO: Works at the moment only if it is one delivery area on top level!
		for (DeliveryArea area : getDeliveryAreaSet().getElements()) {

			if (this.initialiseProblemSpecific) {
				double basic = 0.0;
				for (TimeWindow tw : initValuePerTw.keySet()) {
					if (avgAcceptedPerTw.containsKey(tw)) {
						if (this.considerOverallRemainingCapacity) {
							basic += (initValuePerTw.get(tw) - lowestInitValue) * avgAcceptedPerTw.get(tw)
									/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable;
						} else {
							basic += (initValuePerTw.get(tw)) * avgAcceptedPerTw.get(tw) / AggregateReferenceInformationAlgorithm.maximumValueAcceptable;
						}

					}

				}
				this.basicCoefficientPerDeliveryArea.put(area.getId(), basic);

			} else {
				this.basicCoefficientPerDeliveryArea.put(area.getId(), 0.0);

			}

			if (this.initialiseProblemSpecific && this.withTimeCapacityInteraction) {
				this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), 0.01);
			} else if (this.withTimeCapacityInteraction) {
				this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.timeCapacityInteractionCoefficientPerDeliveryArea.put(area.getId(), null);
			}

			if (this.initialiseProblemSpecific && this.considerOverallRemainingCapacity) {
				this.remainingCapacityCoefficientPerDeliveryArea.put(area.getId(),
						avgOverallAccepted * lowestInitValue / AggregateReferenceInformationAlgorithm.maximumValueAcceptable);
			} else if (this.considerOverallRemainingCapacity) {
				this.remainingCapacityCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.remainingCapacityCoefficientPerDeliveryArea.put(area.getId(), null);
			}

			if (this.initialiseProblemSpecific && this.considerOverallAcceptedCost) {
				this.acceptedOverallCostCoefficientPerDeliveryArea.put(area.getId(),
						-avgOverallAccepted * lowestInitValue / AggregateReferenceInformationAlgorithm.maximumValueAcceptable);
			} else if (this.considerOverallAcceptedCost) {
				this.acceptedOverallCostCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.acceptedOverallCostCoefficientPerDeliveryArea.put(area.getId(), null);
			}

			if (this.initialiseProblemSpecific) {
				double rc = 0;
				if (this.considerOverallRemainingCapacity)
					rc = this.remainingCapacityCoefficientPerDeliveryArea.get(area.getId());
				this.timeCoefficientPerDeliveryArea.put(area.getId(),
						averageValueAccepted - this.basicCoefficientPerDeliveryArea.get(area.getId()) - rc);

			} else {
				this.timeCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			}
			if (this.initialiseProblemSpecific && this.considerAreaPotential) {
				this.areaPotentialCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			} else if (this.considerAreaPotential) {
				this.areaPotentialCoefficientPerDeliveryArea.put(area.getId(), 0.0);
			} else {
				this.areaPotentialCoefficientPerDeliveryArea.put(area.getId(), null);
			}

			ArrayList<ValueFunctionApproximationCoefficient> coefficients = new ArrayList<ValueFunctionApproximationCoefficient>();

			for (TimeWindow tw : getTimeWindowSet().getElements()) {
				ValueFunctionApproximationCoefficient coeff = new ValueFunctionApproximationCoefficient();
				coeff.setDeliveryAreaId(area.getId());
				coeff.setDeliveryArea(area);
				coeff.setTimeWindow(tw);
				coeff.setTimeWindowId(tw.getId());
				coeff.setSquared(false);
				coeff.setCoverage(false);

				if (this.considerInsertionCosts) {
					coeff.setCosts(true);
					coeff.setType(ValueFunctionCoefficientType.COST);
					coefficients.add(coeff);
					if (this.initialiseProblemSpecific) {
						coeff.setCoefficient(-6.0);
					} else {
						coeff.setCoefficient(0.0);
					}
					if (this.considerCoverage) {
						coeff = new ValueFunctionApproximationCoefficient();
						coeff.setDeliveryAreaId(area.getId());
						coeff.setDeliveryArea(area);
						coeff.setTimeWindow(tw);
						coeff.setTimeWindowId(tw.getId());
						coeff.setSquared(false);
						coeff.setCosts(false);
						coeff.setCoverage(true);
						coeff.setType(ValueFunctionCoefficientType.COVERAGE);
						if (this.initialiseProblemSpecific) {
							coeff.setCoefficient(0.5);
						} else {
							coeff.setCoefficient(0.0);
						}
						coefficients.add(coeff);
					}

					if (this.withCapacityInteraction) {
						coeff = new ValueFunctionApproximationCoefficient();
						coeff.setDeliveryAreaId(area.getId());
						coeff.setDeliveryArea(area);
						coeff.setTimeWindow(tw);
						coeff.setTimeWindowId(tw.getId());
						coeff.setSquared(true);
						coeff.setCosts(true);
						coeff.setCoverage(false);

						if (this.initialiseProblemSpecific) {
							coeff.setCoefficient(-0.5);
						} else {
							coeff.setCoefficient(0.0);
						}

						coefficients.add(coeff);
					}

				} else {
					coeff.setType(ValueFunctionCoefficientType.NUMBER);
					coeff.setCosts(false);
					if (this.initialiseProblemSpecific) {
						if (avgAcceptedPerTw.containsKey(tw)) {
							if (this.considerOverallRemainingCapacity || this.considerOverallAcceptedCost) {
								coeff.setCoefficient(-(initValuePerTw.get(tw) - lowestInitValue)
										* avgAcceptedPerTw.get(tw) / AggregateReferenceInformationAlgorithm.maximumValueAcceptable);
							} else {
								coeff.setCoefficient(-initValuePerTw.get(tw) * avgAcceptedPerTw.get(tw)
										/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable);
							}

						} else {
							coeff.setCoefficient(0.0);
						}

					} else {
						coeff.setCoefficient(0.0);
					}
					coefficients.add(coeff);

					// TODO: Adapt such that also possible with insertion costs
					if (this.withCapacityInteraction) {
						coeff = new ValueFunctionApproximationCoefficient();
						coeff.setDeliveryAreaId(area.getId());
						coeff.setDeliveryArea(area);
						coeff.setTimeWindow(tw);
						coeff.setTimeWindowId(tw.getId());
						coeff.setSquared(true);
						coeff.setCosts(false);
						coeff.setType(ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME);
						coeff.setCoefficient(0.0);
						coefficients.add(coeff);
					}
				}

			}

			this.variableCoefficientsPerDeliveryArea.put(area.getId(), coefficients);
		}

	}

	private void initialiseBuffers() {

		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.acceptedInsertionCostsPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Double>>();
		this.areaPotentialPerDeliveryArea = new HashMap<Integer, Double>();
		this.remainingCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedCostPerDeliveryArea = new HashMap<Integer, Double>();
		this.coveringPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();

		this.sumCoverageValuePerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.routesCoveringPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>>();
		distancePerDeliveryAreaAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		distancePerDeliveryAreaAndTwAndRouting = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();
	}

	private void initialiseAlreadyAcceptedPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
		for (TimeWindow tw : getTimeWindowSet().getElements()) {
			acceptedPerTw.put(tw.getId(), 0);
		}
		this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
	}

	private void initialiseAreaPotential(DeliveryArea area) {
		this.areaPotentialPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseRemainingCapacity(DeliveryArea area) {
		this.remainingCapacityPerDeliveryArea.put(area.getId(), this.overallCapacityPerDeliveryArea.get(area.getId()));
	}

	private void initialiseAcceptedCostOverall(DeliveryArea area) {
		this.acceptedCostPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAcceptedCostsPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Double> acceptedPerTw = new HashMap<Integer, Double>();
		for (TimeWindow tw : getTimeWindowSet().getElements()) {
			acceptedPerTw.put(tw.getId(), 0.0);
		}
		this.acceptedInsertionCostsPerDeliveryArea.put(area.getId(), acceptedPerTw);
	}

	private void applyADPForDeliveryArea(DeliveryArea area) {

		// Per order request set, train the value function

		int currentIteration = 0;

		for (Integer requestSetId : this.orderRequestsPerDeliveryArea.get(area.getId()).keySet()) {

			this.updateValueFunctionWithOrderRequests(area, requestSetId, currentIteration,
					this.orderRequestsPerDeliveryArea.get(area.getId()).keySet().size());
			currentIteration++;
		}

	}

	private void updateValueFunctionWithOrderRequests(DeliveryArea area, Integer requestSetId, int currentIteration,
			int numberOfIterations) {
		System.out.println("Next request set: " + requestSetId);

		// Initialise routes for dynamic feasibility check
		HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(area,
				this.vehicleAssignmentsPerDeliveryArea, getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
				AggregateReferenceInformationAlgorithm.getRegion(), (includeDriveFromStartingPosition == 1));

		// TODO: If start not end and include drive from starting position ->
		// needs to be reduced from remaining capacity at the beginning
		this.initialiseAlreadyAcceptedPerTimeWindow(area);
		if (this.considerInsertionCosts)
			this.initialiseAcceptedCostsPerTimeWindow(area);
		if (this.considerOverallRemainingCapacity)
			this.initialiseRemainingCapacity(area);
		if (this.considerAreaPotential)
			this.initialiseAreaPotential(area);
		if (this.considerCoverage)
			this.prepareCoverageMultiplierPerDeliveryAreaAndTimeWindow();
		if (this.considerOverallAcceptedCost)
			this.initialiseAcceptedCostOverall(area);

		// Logging
		this.LOG_LOSS_FUNCTION.get(area.getId()).put(requestSetId, new ArrayList<Double>());
		this.LOG_WEIGHTS.get(area.getId()).put(requestSetId, new ArrayList<String>());

		// Sort order requests to arrive in time
		ArrayList<Integer> relevantRequests = new ArrayList<Integer>();
		relevantRequests.addAll(this.orderRequestsPerDeliveryArea.get(area.getId()).keySet());
		Collections.sort(relevantRequests, Collections.reverseOrder());

		Double currentAcceptedTravelTime = null;
		HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = null;
		ArrayList<Order> acceptedOrders = new ArrayList<Order>();

		HashMap<Integer, Double> distancePerRouting = new HashMap<Integer, Double>();
		HashMap<Integer, HashMap<Integer, Double>> distancePerTwAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		if (distanceType != 0) {
			for (Routing r : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).keySet()) {
				distancePerRouting.put(r.getId(), 0.0);

				for (TimeWindow tw : getTimeWindowSet().getElements()) {
					if (!distancePerTwAndRouting.containsKey(tw.getId())) {
						distancePerTwAndRouting.put(tw.getId(), new HashMap<Integer, Double>());
					}

					distancePerTwAndRouting.get(tw.getId()).put(r.getId(), 0.0);
				}
			}
			this.distancePerDeliveryAreaAndRouting.put(area.getId(), distancePerRouting);
			this.distancePerDeliveryAreaAndTwAndRouting.put(area.getId(), distancePerTwAndRouting);
		}
		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		Double currentDistanceMeasure = 0.0;
		HashMap<Integer, Double> currentDistanceMeasurePerTw = null;
		if (this.distanceMeasurePerTw) {
			currentDistanceMeasurePerTw = new HashMap<Integer, Double>();
			for (TimeWindow tw : getTimeWindowSet().getElements()) {
				currentDistanceMeasurePerTw.put(tw.getId(), 0.0);
			}
		}
		// Go through requests and update value function
		int disT = this.distanceType;
		for (int t = this.orderHorizonLength; t > 0; t--) {
			if (t <= switchDistanceOffPoint) {
				disT = 0;
			}
			OrderRequest request;
			if (this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).containsKey(t)) {
				request = this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).get(t);
			} else {
				request = null;
			}

			// Check feasible time windows
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();
			DeliveryArea subArea = null;
			if (request != null) {
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
								AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(), getTimeWindowSet(),
								(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime,
								possibleRoutings, this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates,
								vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), acceptedOrders, bestRoutingSoFar,
								currentAcceptedTravelTime, consideredTimeWindows, bestRoutingsValueAfterInsertion,
								numberOfThreads);
				if (this.considerOverallRemainingCapacity) {
					this.remainingCapacityPerDeliveryArea.put(area.getId(),
							this.overallCapacityPerDeliveryArea.get(area.getId()) - currentAcceptedTravelTime
									- this.expectedServiceTime * acceptedOrders.size());
				}
				subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(getDeliveryAreaSet(),
						request.getCustomer());
				subArea.setSetId(area.getSubsetId());
			}

			// Choose offer set and update value function
			Pair<RouteElement, Double> result = this.updateValueFunctionIndividualWithOrderRequest(t, area, request,
					bestRoutingsValueAfterInsertion, routes, requestSetId, alternativesToTimeWindows, currentIteration,
					numberOfIterations, currentDistanceMeasure, currentDistanceMeasurePerTw,
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, subArea, disT);

			RouteElement newElement = result.getKey();

			// If customer chose a time window, he needs to be added to the
			// current route
			if (newElement != null) {

				int routingId = bestRoutingsValueAfterInsertion.get(newElement.getOrder().getTimeWindowFinalId())
						.getKey().getTempRoutingId();
				DynamicRoutingHelperService.insertRouteElement(newElement, possibleRoutings.get(routingId),
						vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), getTimeWindowSet(),
						AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
						(includeDriveFromStartingPosition == 1));
				bestRoutingSoFar = possibleRoutings.get(routingId);
				currentAcceptedTravelTime = bestRoutingsValueAfterInsertion
						.get(newElement.getOrder().getTimeWindowFinalId()).getValue();
				acceptedOrders.add(newElement.getOrder());
				if (this.distanceType != 0) {
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
							newElement.getOrder().getTimeWindowFinalId(), alreadyAcceptedPerSubDeliveryAreaAndTimeWindow
									.get(subArea.getId()).get(newElement.getOrder().getTimeWindowFinalId()) + 1);
					currentDistanceMeasure = result.getValue();
				}
			}

		}

	}

	private Pair<RouteElement, Double> updateValueFunctionIndividualWithOrderRequest(int t, DeliveryArea area,
			OrderRequest request, HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion,
			HashMap<Integer, ArrayList<RouteElement>> routes, int requestSetId,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, int currentIteration, int numberOfIterations,
			double currentDistanceMeasure, HashMap<Integer, Double> currentDistanceMeasurePerTw,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			DeliveryArea subArea, int disT) {

		// Calculate value of same state in next time step
		// If at end of order horizon, the value is 0
		double noAssignmentValue = ValueFunctionApproximationService.evaluateStateForLinearValueFunctionApproximation(
				t - 1, this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
				this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),
				this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()),
				this.remainingCapacityPerDeliveryArea.get(area.getId()),
				this.acceptedCostPerDeliveryArea.get(area.getId()), this.areaPotentialPerDeliveryArea.get(area.getId()),
				this.basicCoefficientPerDeliveryArea.get(area.getId()),
				this.timeCoefficientPerDeliveryArea.get(area.getId()),
				this.variableCoefficientsPerDeliveryArea.get(area.getId()),
				this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()),
				this.remainingCapacityCoefficientPerDeliveryArea.get(area.getId()),
				this.acceptedOverallCostCoefficientPerDeliveryArea.get(area.getId()),
				this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()), orderHorizonLength, maxAcceptedPerTw,
				maximumAcceptableOverTw, this.sumCoverageValuePerDeliveryAreaAndTimeWindow.get(area.getId()),
				this.overallCapacityPerDeliveryArea.get(area.getId()),
				this.overallAcceptableCostPerDeliveryArea.get(area.getId()),
				this.maximumAreaPotentialPerDeliveryArea.get(area.getId()) * maximumAcceptableOverTw,
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER()
						* vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
				alternativesToTimeWindows.keySet().size()) * maximumValueAcceptable;

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> selectedOfferedAlternatives = new ArrayList<AlternativeOffer>();
	//	ArrayList<Pair<ArrayList<AlternativeOffer>, Double>> offerSetValues = new ArrayList<Pair<ArrayList<AlternativeOffer>, Double>>();

		Set<TimeWindow> timeWindowFeasible = new HashSet<TimeWindow>();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow = new HashMap<Integer, Routing>();
		HashMap<Integer, Double> distanceMeasurePerTimeWindow = null;
		if (request != null) {

			for (Integer twId : bestRoutingsValueAfterInsertion.keySet()) {
				TimeWindow tw = getTimeWindowSet().getTimeWindowById(twId);
				// Do not add time window, if accepted cost relevant and no
				// offer in STPs
				if (!this.considerOverallAcceptedCost
						|| (this.considerOverallAcceptedCost && aggregatedReferenceInformationCosts.containsKey(subArea)
								&& aggregatedReferenceInformationCosts.get(subArea).containsKey(tw))) {
					timeWindows.add(tw);
				}

			}

			// Determine distance value
			if (disT != 0) {
				distanceMeasurePerTimeWindow = ADPWithSoftOrienteeringANN
						.calculateResultingDistancePerTimeWindowAndRouting(subArea, neighbors.get(subArea),
								neighborsTw, timeWindows, AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area),
								AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences.get(area),
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
								this.distancePerDeliveryAreaAndRouting.get(area.getId()),
								distancePerDeliveryAreaAndTwAndRouting.get(area.getId()),
								newDistancePerTimeWindowAndRouting, routingSmallestDistancePerTimeWindow, disT,
								this.distanceMeasurePerTw);
			}

			// For all feasible, assume you accept -> get value
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			for (TimeWindow tw : timeWindows) {
				int twId = tw.getId();
				if (this.distanceMeasurePerTw)
					currentDistanceMeasure = currentDistanceMeasurePerTw.get(tw.getId());
				if (disT == 0 || distanceMeasurePerTimeWindow.get(tw.getId())
						- currentDistanceMeasure < this.maximumDistanceMeasureIncrease + 1) {
					timeWindowFeasible.add(tw);

					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);
					double currentAcceptedInsertion = 0.0;
					if (this.considerInsertionCosts) {
						currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId())
								.get(twId);
						double additionalCosts = bestRoutingsValueAfterInsertion.get(twId).getKey()
								.getTempShiftWithoutWait();
						this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()).put(twId,
								additionalCosts + currentAcceptedInsertion);
					}
					double currentCoverage = 0.0;
					if (this.considerCoverage) {
						currentCoverage = this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId);
						double additionalCoverage = this.determineAdditionalCoverageAfterInsertion(twId,
								bestRoutingsValueAfterInsertion.get(twId).getKey(), false, area, routes);
						this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()).put(twId,
								currentCoverage + additionalCoverage);
					}
					double currentOverallRemainingCapacity = 0.0;
					if (this.considerOverallRemainingCapacity) {
						currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea.get(area.getId());
						double newRemainingCapacity = currentOverallRemainingCapacity
								- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						if (newRemainingCapacity < 0) {
							System.out.println("Remaining capacity is negative");
						}
						this.remainingCapacityPerDeliveryArea.put(area.getId(), newRemainingCapacity);
					}

					double currentOverallAcceptedCost = 0.0;
					if (this.considerOverallAcceptedCost) {
						currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(area.getId());
						double newAcceptedCostOverall = currentOverallAcceptedCost;
						// Take expected cost if there is waiting time involved
						if (bestRoutingsValueAfterInsertion.get(twId).getKey()
								.getWaitingTime() > this.expectedServiceTime + minimumAdditionalCostPerOrder
								|| bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempSlack() > this.expectedServiceTime + minimumAdditionalCostPerOrder
								|| (bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
										+ bestRoutingsValueAfterInsertion.get(twId).getKey()
												.getTempSlack()) > this.expectedServiceTime
														+ minimumAdditionalCostPerOrder) {
							newAcceptedCostOverall = newAcceptedCostOverall
									+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(subArea).get(tw);
						} else {
							// Otherwise: take the current insertion cost
							newAcceptedCostOverall = newAcceptedCostOverall
									+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						}
						this.acceptedCostPerDeliveryArea.put(area.getId(), newAcceptedCostOverall);
					}

					double currentAreaPotential = 0.0;
					if (this.considerAreaPotential) {
						currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
						double newAreaPotential = this.valueMultiplierPerDeliveryArea.get(subArea.getId());
						this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);
					}
					double assignmentValue = ValueFunctionApproximationService
							.evaluateStateForLinearValueFunctionApproximation(request.getArrivalTime() - 1,
									this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
									this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()),
									this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()),
									this.remainingCapacityPerDeliveryArea.get(area.getId()),
									this.acceptedCostPerDeliveryArea.get(area.getId()),
									this.areaPotentialPerDeliveryArea.get(area.getId()),
									this.basicCoefficientPerDeliveryArea.get(area.getId()),
									this.timeCoefficientPerDeliveryArea.get(area.getId()),
									this.variableCoefficientsPerDeliveryArea.get(area.getId()),
									this.timeCapacityInteractionCoefficientPerDeliveryArea.get(area.getId()),
									this.remainingCapacityCoefficientPerDeliveryArea.get(area.getId()),
									this.acceptedOverallCostCoefficientPerDeliveryArea.get(area.getId()),
									this.areaPotentialCoefficientPerDeliveryArea.get(area.getId()), orderHorizonLength,
									maxAcceptedPerTw, maximumAcceptableOverTw,
									this.sumCoverageValuePerDeliveryAreaAndTimeWindow.get(area.getId()),
									this.overallCapacityPerDeliveryArea.get(area.getId()),
									this.overallAcceptableCostPerDeliveryArea.get(area.getId()),
									this.maximumAreaPotentialPerDeliveryArea.get(area.getId())
											* maximumAcceptableOverTw,
									AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER()
											* vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
									alternativesToTimeWindows.keySet().size())
							* maximumValueAcceptable;
					twValue.put(tw, assignmentValue);

					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, --currentAccepted);

					if (this.considerInsertionCosts) {
						this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()).put(twId,
								currentAcceptedInsertion);
					}

					if (this.considerCoverage) {
						this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()).put(twId, currentCoverage);
					}

					if (this.considerOverallRemainingCapacity) {
						this.remainingCapacityPerDeliveryArea.put(area.getId(), currentOverallRemainingCapacity);
					}

					if (this.considerOverallAcceptedCost) {
						this.acceptedCostPerDeliveryArea.put(area.getId(), currentOverallAcceptedCost);
					}
					if (this.considerAreaPotential) {
						this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential);
					}
				}

			}
			// Find best subset from the time windows with value add

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue,
					AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue, false, null, null,
					DISCOUNT_FACTOR);
			maxValue = bestOffer.getValue();
			selectedOfferedAlternatives = bestOffer.getKey();

		}

		// Update value function approximation
		double newValueApproximation = maxValue + noAssignmentValue;

		// annealing () search-then-converge schedule µ(t)
		// = µ(0)/(1 + t/T) and momentum vi+1=αvi+θi+1 (vi: update vektor, θ:
		// update vektor without momentum)

		double currentStepSize = stepSize;
		if (this.annealingTemperature > 0) {
			currentStepSize = stepSize / (1.0 + (double) requestSetId / this.annealingTemperature);
		}

		ValueFunctionApproximationService.updateLinearValueFunctionApproximation(area, t,
				this.alreadyAcceptedPerDeliveryArea, this.acceptedInsertionCostsPerDeliveryArea,
				this.coveringPerDeliveryAreaAndTimeWindow, this.remainingCapacityPerDeliveryArea,
				this.acceptedCostPerDeliveryArea, this.areaPotentialPerDeliveryArea,
				this.basicCoefficientPerDeliveryArea, this.timeCoefficientPerDeliveryArea,
				this.variableCoefficientsPerDeliveryArea, this.timeCapacityInteractionCoefficientPerDeliveryArea,
				this.remainingCapacityCoefficientPerDeliveryArea, this.acceptedOverallCostCoefficientPerDeliveryArea,
				this.areaPotentialCoefficientPerDeliveryArea, newValueApproximation / maximumValueAcceptable,
				currentStepSize, orderHorizonLength, maxAcceptedPerTw, maximumAcceptableOverTw,
				this.sumCoverageValuePerDeliveryAreaAndTimeWindow, this.overallCapacityPerDeliveryArea,
				this.overallAcceptableCostPerDeliveryArea, this.maximumAreaPotentialPerDeliveryArea,
				this.LOG_LOSS_FUNCTION.get(area.getId()), this.LOG_WEIGHTS.get(area.getId()), requestSetId,
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER()
						* vehicleAssignmentsPerDeliveryArea.get(area.getId()).size(),
				alternativesToTimeWindows.keySet().size(), this.momentumWeight, this.oldMomentumPerDeliveryArea);

		// Simulate customer decision
		if (request != null) {
			/// Choose offer set
			if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {
				if (this.explorationStrategy == 2) {

					// E-Greedy
					LearningService.chooseOfferSetBasedOnEGreedyStrategy(timeWindowFeasible, E_GREEDY_VALUE, 1.0,
							currentIteration, numberOfIterations, 1.0 / 3.0, 2.0 / 3.0, selectedOfferedAlternatives,
							alternativesToTimeWindows);

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
					order.setAccepted(true);
					int twId = selectedAlt.getAlternative().getTimeWindows().get(0).getId();
					order.setTimeWindowFinalId(twId);
					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);
					if (this.considerInsertionCosts) {
						double currentAcceptedInsertion = this.acceptedInsertionCostsPerDeliveryArea.get(area.getId())
								.get(twId);
						double additionalCosts = bestRoutingsValueAfterInsertion.get(twId).getKey()
								.getTempShiftWithoutWait();
						this.acceptedInsertionCostsPerDeliveryArea.get(area.getId()).put(twId,
								additionalCosts + currentAcceptedInsertion);
					}
					if (this.considerCoverage) {
						double currentCoverage = this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId);
						double additionalCoverage = this.determineAdditionalCoverageAfterInsertion(twId,
								bestRoutingsValueAfterInsertion.get(twId).getKey(), true, area, routes);
						this.coveringPerDeliveryAreaAndTimeWindow.get(area.getId()).put(twId,
								currentCoverage + additionalCoverage);
					}

					if (this.considerOverallRemainingCapacity) {
						double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
								.get(area.getId());
						double newRemainingCapacity = currentOverallRemainingCapacity
								- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						this.remainingCapacityPerDeliveryArea.put(area.getId(), newRemainingCapacity);
						if (newRemainingCapacity < 0) {
							System.out.println("Remaining capacity is negative");
						}
					}

					if (this.considerOverallAcceptedCost) {
						double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(area.getId());
						double newAcceptedCostOverall = currentOverallAcceptedCost;
						// Take expected cost if there is waiting time involved
						if (bestRoutingsValueAfterInsertion.get(twId).getKey()
								.getWaitingTime() > this.expectedServiceTime + minimumAdditionalCostPerOrder
								|| bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempSlack() > this.expectedServiceTime + minimumAdditionalCostPerOrder
								|| (bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
										+ bestRoutingsValueAfterInsertion.get(twId).getKey()
												.getTempSlack()) > this.expectedServiceTime
														+ minimumAdditionalCostPerOrder) {
							newAcceptedCostOverall = newAcceptedCostOverall + AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
									.get(subArea).get(selectedAlt.getAlternative().getTimeWindows().get(0));
						} else {
							// Otherwise: take the current insertion cost
							newAcceptedCostOverall = newAcceptedCostOverall
									+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						}
						this.acceptedCostPerDeliveryArea.put(area.getId(), newAcceptedCostOverall);
					}

					if (this.considerAreaPotential) {
						double currentAreaPotential = this.areaPotentialPerDeliveryArea.get(area.getId());
						double newAreaPotential = this.valueMultiplierPerDeliveryArea.get(subArea.getId());
						this.areaPotentialPerDeliveryArea.put(area.getId(), currentAreaPotential + newAreaPotential);
					}

					// Update distances
					if (disT != 0) {
						this.distancePerDeliveryAreaAndRouting.put(area.getId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						this.distancePerDeliveryAreaAndTwAndRouting.get(area.getId()).put(order.getTimeWindowFinalId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						currentDistanceMeasure = distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId());
						if (this.distanceMeasurePerTw)
							currentDistanceMeasurePerTw.put(order.getTimeWindowFinalId(),
									distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId()));
					}

					bestRoutingsValueAfterInsertion.get(twId).getKey().setOrder(order);
					return new Pair<RouteElement, Double>(bestRoutingsValueAfterInsertion.get(twId).getKey(),
							currentDistanceMeasure);
				}

			}
		}
		return new Pair<RouteElement, Double>(null, currentDistanceMeasure);

	}

	private double determineAdditionalCoverageAfterInsertion(Integer twId, RouteElement possibleInsertion,
			boolean updateRoutesPerDa, DeliveryArea area, HashMap<Integer, ArrayList<RouteElement>> routes) {
		double additionalCov = 0.0;
		//
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

	public static String[] getParameterSetting() {

		return paras;
	}

	public ValueFunctionApproximationModelSet getResult() {

		return modelSet;
	}

}
