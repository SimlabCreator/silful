package logic.service.algorithmProvider.rm.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.ADPAdaptedFromYangStrauss;
import logic.algorithm.rm.optimization.learning.ADPmeso;
import logic.algorithm.rm.optimization.learning.ReinforcementLearningTabular;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class ADPService implements AlgorithmProviderService {

	String algorithm;

	public ADPService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.LEARNING_ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);

		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ADPAsYang")) {
			String[] paras = ADPAdaptedFromYangStrauss.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		if (algorithm != null && algorithm.equals("ADPmeso")) {
			String[] paras = ADPmeso.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		
		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ReinforcementLearningTabular")) {
			request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
			String[] paras = ReinforcementLearningTabular.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.LINEAR_VALUE_FUNCTION_APPROXIMATION);
		return output;
	}

	public ValueFunctionApproximationModelSet startMesoValueFunctionApproximation(int periodNumber)
			throws ParameterUnknownException {
		Region region = SettingsProvider.getExperiment().getRegion();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
				.getLearningOrderRequestSets(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");

		Double considerRCapacity = InputPreparator.getParameterValue(periodNumber,
				"consider_overall_remaining_capacity");
		
		Double numberOfRoutings = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double numberOfInsertionCandidates = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double distanceType = InputPreparator.getParameterValue(periodNumber, "distance_type");
		Double distanceMeasureTw = InputPreparator.getParameterValue(periodNumber, "distance_measure_per_tw");
		Double maximumDistanceIncrease = InputPreparator.getParameterValue(periodNumber,
				"maximum_distance_measure_increase");
		Double switchOff = InputPreparator.getParameterValue(periodNumber, "switch_distance_off_point");
		Double ovAccInsertionCost = InputPreparator.getParameterValue(periodNumber,
				"consider_overall_accepted_insertion_costs");
		Double mesoWeight = InputPreparator.getParameterValue(periodNumber, "meso_weight_lf");
		Double learningRate = InputPreparator.getParameterValue(periodNumber, "stepsize_adp_learning");
		Double annealingTemp = InputPreparator.getParameterValue(periodNumber, "annealing_temperature_(Negative:no_annealing)");
		Double momentumWeight = InputPreparator.getParameterValue(periodNumber, "momentum_weight");

		Double explorationStrategy = InputPreparator.getParameterValue(periodNumber,
				"exploration_(0:on-policy,1:wheel,2:e-greedy)");
		Double noRepetitionsSample= InputPreparator.getParameterValue(periodNumber, "no_repetitions_sample");

		
	Double considerAmountTimeInt= InputPreparator.getParameterValue(periodNumber, "time_cap_interaction");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}

		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator
				.getLearningFinalRoutings(periodNumber);
		ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for (Integer e : routingsForLearningH.keySet()) {
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}

		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		ADPmeso algo = new ADPmeso(region, vehicleAreaAssignmentSet, orderRequestSetsForLearning, routingsForLearning,
				deliveryAreaSet, demandSegmentWeighting, expectedServiceTime, objectiveSpecificValues, maximumRevenueValue,
				includeDriveFromStartingPosition, orderRequestSetsForLearning.get(0).getBookingHorizon(),
				samplePreferences, actualValue, considerRCapacity, ovAccInsertionCost, considerAmountTimeInt, explorationStrategy,
				numberOfInsertionCandidates, numberOfRoutings, neighbors, distanceType, distanceMeasureTw,
				maximumDistanceIncrease, switchOff, mesoWeight, learningRate, annealingTemp, momentumWeight, noRepetitionsSample);

		algo.start();
		return ResultHandler.organizeValueFunctionApproximationModelSetResult(algo, periodNumber);
	}

	public ValueFunctionApproximationModelSet startLinearFunctionApproximationAsYang(int periodNumber)
			throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
				.getLearningOrderRequestSets(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double stepSize = InputPreparator.getParameterValue(periodNumber, "stepsize_adp_learning");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		Double initialiseProblemSpecific = InputPreparator.getParameterValue(periodNumber,
				"initialiseCoefficientsProblemSpecific");
		Double annealingTemp = InputPreparator.getParameterValue(periodNumber,
				"annealing_temperature_(Negative:no_annealing)");
		Double interactionEffect = InputPreparator.getParameterValue(periodNumber, "time_cap_interaction");
		Double capacityInteractionEffect = InputPreparator.getParameterValue(periodNumber, "cap_cap_interaction");
		Double considerInsertionCosts = InputPreparator.getParameterValue(periodNumber, "consider_insertion_costs");
		Double considerCoverage = InputPreparator.getParameterValue(periodNumber, "consider_coverage");
		Double considerRCapacity = InputPreparator.getParameterValue(periodNumber,
				"consider_overall_remaining_capacity");
		Double considerAreaPotential = InputPreparator.getParameterValue(periodNumber, "consider_area_potential");
		Double momentumWeight = InputPreparator.getParameterValue(periodNumber, "momentum_weight");
		Double numberOfRoutings = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double numberOfInsertionCandidates = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double distanceType = InputPreparator.getParameterValue(periodNumber, "distance_type");
		Double distanceMeasureTw = InputPreparator.getParameterValue(periodNumber, "distance_measure_per_tw");
		Double maximumDistanceIncrease = InputPreparator.getParameterValue(periodNumber,
				"maximum_distance_measure_increase");
		Double switchOff = InputPreparator.getParameterValue(periodNumber, "switch_distance_off_point");
		Double ovAccInsertionCost = InputPreparator.getParameterValue(periodNumber,
				"consider_overall_accepted_insertion_costs");

		Double explorationStrategy = InputPreparator.getParameterValue(periodNumber,
				"exploration_(0:on-policy,1:wheel,2:e-greedy)");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}

		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator
				.getLearningFinalRoutings(periodNumber);
		ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for (Integer e : routingsForLearningH.keySet()) {
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}

		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		ADPAdaptedFromYangStrauss algo = new ADPAdaptedFromYangStrauss(region, vehicleAreaAssignmentSet, orderRequestSetsForLearning,
				routingsForLearning, deliveryAreaSet, expectedServiceTime, objectiveSpecificValues, maximumRevenueValue,
				stepSize, includeDriveFromStartingPosition, orderRequestSetsForLearning.get(0).getBookingHorizon(),
				samplePreferences, actualValue, initialiseProblemSpecific, annealingTemp, interactionEffect,
				capacityInteractionEffect, considerInsertionCosts, considerCoverage, considerRCapacity,
				ovAccInsertionCost, considerAreaPotential, explorationStrategy, momentumWeight, demandSegmentWeighting,
				daWeights, daSegmentWeightings, numberOfInsertionCandidates, numberOfRoutings, neighbors, distanceType,
				distanceMeasureTw, maximumDistanceIncrease, switchOff);

		algo.start();
		return ResultHandler.organizeValueFunctionApproximationModelSetResult(algo, periodNumber);

	}

	public ValueFunctionApproximationModelSet startTabularFunctionApproximationAsYang(int periodNumber)
			throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
				.getLearningOrderRequestSets(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double stepSize = InputPreparator.getParameterValue(periodNumber, "stepsize_adp_learning");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");

		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		Double initialiseProblemSpecific = InputPreparator.getParameterValue(periodNumber,
				"initialiseCoefficientsProblemSpecific");
		Double annealingTemp = InputPreparator.getParameterValue(periodNumber,
				"annealing_temperature_(Negative:no_annealing)");
		Double explorationStrategy = InputPreparator.getParameterValue(periodNumber,
				"exploration_(0:on-policy,1:wheel,2:e-greedy)");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);

		ReinforcementLearningTabular algo = new ReinforcementLearningTabular(region, vehicleAreaAssignmentSet,
				orderRequestSetsForLearning, deliveryAreaSet, demandSegmentWeighting, expectedServiceTime,
				objectiveSpecificValues, maximumRevenueValue, stepSize, includeDriveFromStartingPosition,
				orderRequestSetsForLearning.get(0).getBookingHorizon(), arrivalProcessId, samplePreferences,
				actualValue, initialiseProblemSpecific, annealingTemp, explorationStrategy, daSegmentWeightings,
				daWeights);

		algo.start();
		return ResultHandler.organizeValueFunctionApproximationModelSetResult(algo, periodNumber);

	}

}
