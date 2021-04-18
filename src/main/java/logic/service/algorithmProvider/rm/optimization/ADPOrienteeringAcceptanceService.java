package logic.service.algorithmProvider.rm.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.ADPWithOrienteeringANNAcceptance;
import logic.algorithm.rm.optimization.learning.ADPWithOrienteeringAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class ADPOrienteeringAcceptanceService implements AlgorithmProviderService {

	String algorithm;

	public ADPOrienteeringAcceptanceService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.LINEAR_VALUE_FUNCTION_APPROXIMATION, false);
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);

		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ADPWithOrienteeringAcceptance")) {
			String[] paras = ADPWithOrienteeringAcceptance.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		
		if (algorithm != null && algorithm.equals("ADPWithOrienteeringANNAcceptance")) {
			String[] paras = ADPWithOrienteeringANNAcceptance.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.ORDERSET);
		return output;
	}

	public OrderSet startLinearFunctionApproximationAcceptance(int periodNumber) throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		Double noRoutingCan = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double noInsertionCan = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double considerOC = InputPreparator.getParameterValue(periodNumber, "consider_orienteering_costs");
		Double considerON = InputPreparator.getParameterValue(periodNumber, "consider_orienteering_number");
		Double considerOrienteeringRC = InputPreparator.getParameterValue(periodNumber,
				"consider_orienteering_routing_candidates");
		Double considerLeftOverPenalty = InputPreparator.getParameterValue(periodNumber, "consider_left_over_penalty");
		Double dynamicRouting = InputPreparator.getParameterValue(periodNumber, "dynamic_feasibility_check");
		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
		Double areaSpecificDetCheck = InputPreparator.getParameterValue(periodNumber, "area_specific_deterministic_check");
		Double areaSpecificUtilityCheck = InputPreparator.getParameterValue(periodNumber, "area_specific_utility_weighting");

		
		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);

		ValueFunctionApproximationModelSet valueFunctionApproximationModelSet = InputPreparator
				.getValueFunctionApproximationModelSet(periodNumber);
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}

		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);

		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
				daWeightsUpper, daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);

		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}

		ADPWithOrienteeringAcceptance algo = new ADPWithOrienteeringAcceptance(valueFunctionApproximationModelSet,
				region, vehicleAreaAssignmentSet, routingsForLearning, orderRequestSet, deliveryAreaSet,
				expectedServiceTime, objectiveSpecificValues, maximumRevenueValue, includeDriveFromStartingPosition,
				actualValue, orderRequestSet.getBookingHorizon(), samplePreferences, daWeightsUpper,
				daSegmentWeightingsUpper, daWeights, daSegmentWeightings, noRoutingCan, noInsertionCan,
				considerOrienteeringRC, considerON, considerOC, dynamicRouting, theftBased, areaSpecificDetCheck,considerLeftOverPenalty,areaSpecificUtilityCheck,neighbors,
				arrivalProcessId);

		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}
	
	public OrderSet startANNApproximationAcceptance(int periodNumber) throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		
		Double considerLeftOverPenalty = InputPreparator.getParameterValue(periodNumber, "consider_left_over_penalty");
		
		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
		Double theftBasedAdvanced = InputPreparator.getParameterValue(periodNumber, "theft-based-advanced");
		Double theftBasedTw = InputPreparator.getParameterValue(periodNumber, "theft-based-tw");
		
		Double discountingFactorProbability = InputPreparator.getParameterValue(periodNumber, "discounting_factor_probability");
		Double oppOnlyFeasible = InputPreparator.getParameterValue(periodNumber, "oc_for_feasible");
		Double expectedST = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");

		
		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);

		ValueFunctionApproximationModelSet valueFunctionApproximationModelSet = InputPreparator
				.getValueFunctionApproximationModelSet(periodNumber);
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}

		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
				daWeights, daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);

		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
				daWeightsUpper, daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);

		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}

		ADPWithOrienteeringANNAcceptance algo = new ADPWithOrienteeringANNAcceptance(valueFunctionApproximationModelSet,demandSegmentWeighting,
				region, vehicleAreaAssignmentSet, routingsForLearning, orderRequestSet, deliveryAreaSet,
				objectiveSpecificValues, maximumRevenueValue, 
				actualValue, orderRequestSet.getBookingHorizon(), samplePreferences, daWeightsUpper,
				daSegmentWeightingsUpper, daWeights, daSegmentWeightings, theftBased, theftBasedAdvanced,theftBasedTw, considerLeftOverPenalty,neighbors,
				arrivalProcessId, discountingFactorProbability, oppOnlyFeasible, expectedST);
	
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}

}
