package logic.service.algorithmProvider.rm.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.ParallelFlightsAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class LinearProgrammingAcceptanceService implements AlgorithmProviderService {

	String algorithm;

	public LinearProgrammingAcceptanceService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ParallelFlightsAcceptance")) {
			String[] paras = ParallelFlightsAcceptance.getParameterSetting();
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

	public OrderSet startAcceptanceBasedOnStaticCapacityAssignment(int periodNumber) throws ParameterUnknownException
			 {

		Region region = SettingsProvider.getExperiment().getRegion();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		DemandSegmentSet demandSegmentSet = InputPreparator.getDemandSegmentSet(periodNumber);
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator
				.getOrderRequestSet(periodNumber);
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double actualBasketValue= InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet());
		int periodLength = orderRequestSet.getBookingHorizon();
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, capacitySet.getDeliveryAreaSet(), demandSegmentWeighting);

		ParallelFlightsAcceptance algo = new ParallelFlightsAcceptance(region, deliveryAreaSet, demandSegmentSet,orderRequestSet, capacitySet,
				objectiveSpecificValues, maximumRevenueValue, arrivalProcessId, daWeights, daSegmentWeightings,samplePreferences,periodLength, actualBasketValue);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}

}
