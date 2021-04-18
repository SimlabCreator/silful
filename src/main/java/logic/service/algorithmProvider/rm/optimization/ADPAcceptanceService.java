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
import logic.algorithm.rm.optimization.learning.ADPAdaptedFromYangStraussAcceptance;
import logic.algorithm.rm.optimization.learning.ADPmesoAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class ADPAcceptanceService implements AlgorithmProviderService {

	String algorithm;

	public ADPAcceptanceService(String algorithm) {
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

		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ADPAsYangAcceptance")) {
			String[] paras = ADPAdaptedFromYangStraussAcceptance.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		
		if (algorithm != null && algorithm.equals("ADPmesoAcceptance")) {
			String[] paras = ADPmesoAcceptance.getParameterSetting();
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

	public OrderSet startLinearFunctionApproximationAcceptanceAsYang(int periodNumber)
			throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double numberOfRoutings = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double numberOfInsertionCandidates = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double switchOff = InputPreparator.getParameterValue(periodNumber, "switch_distance_off_point");
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
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator
				.getLearningFinalRoutings(periodNumber);
		ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for (Integer e : routingsForLearningH.keySet()) {
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}

		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService
				.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		ADPAdaptedFromYangStraussAcceptance algo = new ADPAdaptedFromYangStraussAcceptance(valueFunctionApproximationModelSet, routingsForLearning,
				region, vehicleAreaAssignmentSet, orderRequestSet, deliveryAreaSet, expectedServiceTime,
				objectiveSpecificValues, maximumRevenueValue, includeDriveFromStartingPosition,
				orderRequestSet.getBookingHorizon(), samplePreferences, daWeights, daSegmentWeightings,
				numberOfInsertionCandidates, numberOfRoutings, neighbors,
				InputPreparator.getParameterValue(periodNumber, "distance_type"),
				InputPreparator.getParameterValue(periodNumber, "distance_measure_per_tw"),
				InputPreparator.getParameterValue(periodNumber, "maximum_distance_measure_increase"), switchOff);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}


}
