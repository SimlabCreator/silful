//package logic.service.algorithmProvider.rm.optimization;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import data.entity.DeliveryArea;
//import data.entity.DeliveryAreaSet;
//import data.entity.DemandSegmentWeighting;
//import data.entity.Entity;
//import data.entity.ObjectiveWeight;
//import data.entity.OrderRequestSet;
//import data.entity.OrderSet;
//import data.entity.Region;
//import data.entity.Routing;
//import data.entity.ValueFunctionApproximationModelSet;
//import data.entity.VehicleAreaAssignmentSet;
//import data.utility.Output;
//import data.utility.PeriodSettingType;
//import data.utility.SettingRequest;
//import logic.algorithm.rm.optimization.learning.ADPWithOrienteering;
//import logic.algorithm.rm.optimization.learning.ADPWithOrienteeringANN;
//import logic.service.algorithmProvider.AlgorithmProviderService;
//import logic.service.support.AcceptanceService;
//import logic.service.support.EvaluationService;
//import logic.service.support.LocationService;
//import logic.utility.InputPreparator;
//import logic.utility.ResultHandler;
//import logic.utility.SettingsProvider;
//import logic.utility.exceptions.ParameterUnknownException;
//
//public class ADPOrienteeringService implements AlgorithmProviderService {
//
//	String algorithm;
//
//	public ADPOrienteeringService(String algorithm) {
//		this.algorithm = algorithm;
//	};
//
//	public SettingRequest getSettingRequest() {
//		SettingRequest request = new SettingRequest();
//		request.addPeriodSetting(PeriodSettingType.LEARNING_ORDERREQUESTSET, false);
//		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
//		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
//		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
//		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
//		request.addPeriodSetting(PeriodSettingType.BENCHMARKING_FINAL_ROUTING, true);
//		request.addPeriodSetting(PeriodSettingType.LEARNING_ORDER_SET, true);
//		request.addPeriodSetting(PeriodSettingType.BENCHMARKING_ORDER_SET, true);
//		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
//
//		// Possibly needed parameter settings that are individual to the
//		// algorithm
//		if (algorithm != null && algorithm.equals("ADPWithOrienteering")) {
//			String[] paras = ADPWithOrienteering.getParameterSetting();
//			for (int i = 0; i < paras.length; i++) {
//				request.addParameter(paras[i]);
//			}
//		}
//		if (algorithm != null && algorithm.equals("ADPWithOrienteeringANN")) {
//			String[] paras = ADPWithOrienteeringANN.getParameterSetting();
//			for (int i = 0; i < paras.length; i++) {
//				request.addParameter(paras[i]);
//			}
//		}
//
//		return request;
//	}
//
//	public Output getOutput() {
//		Output output = new Output();
//		output.addOutput(PeriodSettingType.LINEAR_VALUE_FUNCTION_APPROXIMATION);
//		return output;
//	}
//
//	public ValueFunctionApproximationModelSet startLinearFunctionApproximation(int periodNumber)
//			throws ParameterUnknownException {
//
//		Region region = SettingsProvider.getExperiment().getRegion();
//		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
//		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
//		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
//				.getLearningOrderRequestSets(periodNumber);
//		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
//		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
//		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
//		Double stepSize = InputPreparator.getParameterValue(periodNumber, "stepsize_adp_learning");
//		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
//				"includeDriveFromStartingPosition");
//		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
//		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
//		Double initialiseProblemSpecific = InputPreparator.getParameterValue(periodNumber,
//				"initialiseCoefficientsProblemSpecific");
//		Double annealingTemp = InputPreparator.getParameterValue(periodNumber,
//				"annealing_temperature_(Negative:no_annealing)");
//		Double noRoutingCan = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
//		Double noInsertionCan = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
//		Double considerInsertionCostsTw = InputPreparator.getParameterValue(periodNumber,
//				"consider_insertion_costs_time_window");
//		Double considerCoverageOverall = InputPreparator.getParameterValue(periodNumber,
//				"consider_insertion_costs_overall");
//		Double considerOrienteeringRC = InputPreparator.getParameterValue(periodNumber,
//				"consider_orienteering_routing_candidates");
//		Double considerAreaPotential = InputPreparator.getParameterValue(periodNumber, "consider_area_potential");
//		Double considerOC = InputPreparator.getParameterValue(periodNumber, "consider_orienteering_costs");
//		Double considerON = InputPreparator.getParameterValue(periodNumber, "consider_orienteering_number");
//		Double momentumWeight = InputPreparator.getParameterValue(periodNumber, "momentum_weight");
//		Double targetInit = InputPreparator.getParameterValue(periodNumber, "target_for_initialisation");
//		Double explorationStrategy = InputPreparator.getParameterValue(periodNumber,
//				"exploration_(0:on-policy,1:wheel,2:e-greedy)");
//		// Double considerDemandCapacityRatio =
//		// InputPreparator.getParameterValue(periodNumber,
//		// "consider_demand_capacity_ratio");
//		Double considerTimeCapInteraction = InputPreparator.getParameterValue(periodNumber, "time_cap_interaction");
//		Double dynamicFeasibilityCheck = InputPreparator.getParameterValue(periodNumber, "dynamic_feasibility_check");
//		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
//		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
//				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
//		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
//		for (Entity entity : objectives) {
//
//			objectiveSpecificValues.put(entity, null);
//		}
//
//		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
//		Double areaSpecificValueFunction = InputPreparator.getParameterValue(periodNumber,
//				"area_specific_value_function");
//		Double remainingCap = InputPreparator.getParameterValue(periodNumber,
//				"consider_orienteering_remaining_capacity");
//		Double remainingCapTimeInteraction = InputPreparator.getParameterValue(periodNumber,
//				"consider_orienteering_remaining_capacity_time");
//		Double considerLeftOverPenalty = InputPreparator.getParameterValue(periodNumber, "consider_left_over_penalty");
//		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
//				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
//
//		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
//		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
//		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
//				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
//
//		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
//		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
//		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
//				daWeightsUpper, daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);
//		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
//		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
//		for(Integer e: routingsForLearningH.keySet()){
//			routingsForLearning.addAll(routingsForLearningH.get(e));
//		}
//
//
//		ADPWithOrienteering algo = new ADPWithOrienteering(region, vehicleAreaAssignmentSet, routingsForLearning,
//				considerCoverageOverall, considerInsertionCostsTw, deliveryAreaSet, daWeightsUpper,
//				daSegmentWeightingsUpper, daWeights, daSegmentWeightings, orderRequestSetsForLearning,
//				expectedServiceTime, considerAreaPotential, objectiveSpecificValues, maximumRevenueValue,
//				initialiseProblemSpecific, actualValue, includeDriveFromStartingPosition,
//				orderRequestSetsForLearning.get(0).getBookingHorizon(), stepSize, annealingTemp, explorationStrategy,
//				samplePreferences, noRoutingCan, noInsertionCan, considerOrienteeringRC, considerON, considerOC,
//				remainingCap, remainingCapTimeInteraction, momentumWeight, targetInit, considerTimeCapInteraction,
//				dynamicFeasibilityCheck, theftBased, areaSpecificValueFunction, considerLeftOverPenalty, neighbors,
//				arrivalProcessId);
//
//		algo.start();
//
//		return ResultHandler.organizeValueFunctionApproximationModelSetResult(algo, periodNumber);
//
//	}
//
//	public ValueFunctionApproximationModelSet startANNFunctionApproximation(int periodNumber)
//			throws ParameterUnknownException {
//
//		Region region = SettingsProvider.getExperiment().getRegion();
//		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
//		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
//		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
//				.getLearningOrderRequestSets(periodNumber);
//		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
//		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
//
//		Double stepSize = InputPreparator.getParameterValue(periodNumber, "stepsize_adp_learning");
//
//		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
//		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
//	//	Double initialiseProblemSpecific = InputPreparator.getParameterValue(periodNumber,
//	//			"initialiseCoefficientsProblemSpecific");
//		Double annealingTemp = InputPreparator.getParameterValue(periodNumber,
//				"annealing_temperature_(Negative:no_annealing)");
//
//		Double momentumWeight = InputPreparator.getParameterValue(periodNumber, "momentum_weight");
//		//Double targetInit = InputPreparator.getParameterValue(periodNumber, "target_for_initialisation");
//		Double explorationStrategy = InputPreparator.getParameterValue(periodNumber,
//				"exploration_(0:on-policy,1:conservative-factor,2:e-greedy)");
//		Double discountingFactor = InputPreparator.getParameterValue(periodNumber, "discounting_factor");
//		Double discountingFactorProb = InputPreparator.getParameterValue(periodNumber,
//				"discounting_factor_probability");
//
//		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
//		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
//				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
//		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
//		for (Entity entity : objectives) {
//
//			objectiveSpecificValues.put(entity, null);
//		}
//
//		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
//		Double theftBasedAdvanced = InputPreparator.getParameterValue(periodNumber, "theft-based-advanced");
//
//		Double considerLeftOverPenalty = InputPreparator.getParameterValue(periodNumber, "consider_left_over_penalty");
//		Double additionalHiddenNodes = InputPreparator.getParameterValue(periodNumber, "additional_hidden_nodes");
//		Double considerConstantAnn = InputPreparator.getParameterValue(periodNumber, "consider_constant");
//		Double considerNeighborDemand = InputPreparator.getParameterValue(periodNumber, "consider_demand_neighbors");
//		Double oppOnlyFeasible = InputPreparator.getParameterValue(periodNumber, "oc_for_feasible");
//		Double considerTAndWeights= InputPreparator.getParameterValue(periodNumber, "consider_t_and_weights");
//		
//		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
//				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
//
//		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
//		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
//		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
//				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
//
//		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
//		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
//		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
//				daWeightsUpper, daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);
//		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
//		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
//		for(Integer e: routingsForLearningH.keySet()){
//			routingsForLearning.addAll(routingsForLearningH.get(e));
//		}
//		
//		HashMap<Integer, ArrayList<OrderSet>> orderSetsForLearningH = InputPreparator.getLearningOrderSets(periodNumber);
//		 ArrayList<OrderSet> orderSetsForLearning = new ArrayList<OrderSet>();
//		 if(orderSetsForLearningH!=null){
//		for(Integer e: orderSetsForLearningH.keySet()){
//			orderSetsForLearning.addAll(orderSetsForLearningH.get(e));
//		}
//		 }
//		HashMap<Integer, ArrayList<Routing>> routingsForBenchmarking= InputPreparator.getBenchmarkingFinalRoutings(periodNumber);
//		HashMap<Integer, ArrayList<OrderSet>> orderSetsForBenchmarking= InputPreparator.getBenchmarkingOrderSets(periodNumber);
//		//Evaluate per experiment for benchmarking
//		HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues = new HashMap<DeliveryArea, ArrayList<Double>>();
//		ArrayList<OrderRequestSet> benchmarkingOrderRequestSets = new ArrayList<OrderRequestSet>();
//		EvaluationService.determineAverageOrderValuePerDeliveryArea(routingsForBenchmarking, orderSetsForBenchmarking, benchmarkValues, benchmarkingOrderRequestSets,deliveryAreaSet, maximumRevenueValue, objectiveSpecificValues);
//		
//		ADPWithOrienteeringANN algo = new ADPWithOrienteeringANN(region, demandSegmentWeighting,
//				vehicleAreaAssignmentSet, routingsForLearning,orderSetsForLearning, deliveryAreaSet, daWeightsUpper,
//				daSegmentWeightingsUpper, daWeights, daSegmentWeightings, orderRequestSetsForLearning,
//				 objectiveSpecificValues, maximumRevenueValue, 
//				actualValue, orderRequestSetsForLearning.get(0).getBookingHorizon(), stepSize, annealingTemp,
//				explorationStrategy, samplePreferences, momentumWeight, theftBased, theftBasedAdvanced, considerLeftOverPenalty,
//				neighbors, arrivalProcessId, discountingFactor, discountingFactorProb, benchmarkValues, benchmarkingOrderRequestSets, additionalHiddenNodes, considerConstantAnn, considerNeighborDemand, oppOnlyFeasible, considerTAndWeights);
//
//		algo.start();
//
//		return ResultHandler.organizeValueFunctionApproximationModelSetResult(algo, periodNumber);
//
//	}
//
//}
