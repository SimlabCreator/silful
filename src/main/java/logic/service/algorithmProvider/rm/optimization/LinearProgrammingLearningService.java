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
import data.entity.Region;
import data.entity.Routing;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.ParallelFlightsPreparation;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class LinearProgrammingLearningService implements AlgorithmProviderService {

	String algorithm;

	public LinearProgrammingLearningService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.LEARNING_ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("ParallelFlightsPreparation")) {
			String[] paras = ParallelFlightsPreparation.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.CAPACITYSET);
		return output;
	}

	public CapacitySet startCapacityAndBetaDeterminationAlgorithm(int periodNumber) throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();

		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		DemandSegmentSet demandSegmentSet = InputPreparator.getDemandSegmentSet(periodNumber);
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		ArrayList<OrderRequestSet> orderRequestSetsForLearning = InputPreparator
				.getLearningOrderRequestSets(periodNumber);
		int periodLength = orderRequestSetsForLearning.get(0).getBookingHorizon();
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}
		Double betaLowerBound = InputPreparator.getParameterValue(periodNumber, "Beta_lower_bound");
		Double betaUpperBound = InputPreparator.getParameterValue(periodNumber, "Beta_upper_bound");
		Double betaStepSize = InputPreparator.getParameterValue(periodNumber, "Beta_stepsize");
		Double numberOfPotentialRoutings = InputPreparator.getParameterValue(periodNumber, "No_routing_candidates");

		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double averageCapacitySetAsOption = InputPreparator.getParameterValue(periodNumber,
				"averageCapacitiesAsOption");
		Double actualBasketValue = InputPreparator.getParameterValue(periodNumber, "actualBasketValue");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, capacitySet.getDeliveryAreaSet(), demandSegmentWeighting);
		double sumWeights=0.0;
		for(DeliveryArea area: daWeights.keySet()){
			sumWeights+=daWeights.get(area);
		}
		
		
		ParallelFlightsPreparation algo = new ParallelFlightsPreparation(region, deliveryAreaSet, demandSegmentSet,
				orderRequestSetsForLearning, capacitySet, objectiveSpecificValues, maximumRevenueValue,
				routingsForLearning, betaLowerBound, betaUpperBound, betaStepSize, numberOfPotentialRoutings,
				arrivalProcessId, daWeights, daSegmentWeightings, samplePreferences, averageCapacitySetAsOption,
				periodLength, actualBasketValue);
		algo.start();
		return ResultHandler.organizeCapacitySetResult(algo, periodNumber);

	}

}
