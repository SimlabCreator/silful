package logic.service.algorithmProvider.rm.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.GeneralAtomicOutputValue;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Routing;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.DeterministicProgrammingPreparation;
import logic.algorithm.rm.optimization.learning.ParallelFlightsPreparation;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class DeterministicProgrammingLearningService implements AlgorithmProviderService {

	String algorithm;

	public DeterministicProgrammingLearningService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.LEARNING_ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("DeterministicProgrammingPreparation")) {
			String[] paras = DeterministicProgrammingPreparation.getParameterSetting();
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

	public GeneralAtomicOutputValue startDPAlgorithm(int periodNumber) throws ParameterUnknownException {

		Region region = SettingsProvider.getExperiment().getRegion();

		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		DemandSegmentSet demandSegmentSet = InputPreparator.getDemandSegmentSet(periodNumber);
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

		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
		Double theftBasedAd = InputPreparator.getParameterValue(periodNumber, "theft-based-advanced");
		Double dupSeg = InputPreparator.getParameterValue(periodNumber, "duplicate_segments");
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);

		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		
		DeterministicProgrammingPreparation algo = new DeterministicProgrammingPreparation(region, periodLength, deliveryAreaSet, demandSegmentWeighting,
				orderRequestSetsForLearning, objectiveSpecificValues, maximumRevenueValue,
				routingsForLearning, betaLowerBound, betaUpperBound, betaStepSize, 
				arrivalProcessId, samplePreferences,theftBased, theftBasedAd, neighbors, dupSeg);
		algo.start();
		return ResultHandler.organizeAtomicOutputResult(algo, periodNumber);

	}

}
