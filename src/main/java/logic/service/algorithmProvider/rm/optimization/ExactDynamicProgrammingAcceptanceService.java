package logic.service.algorithmProvider.rm.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.DynamicProgrammingTree;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.acceptance.ExactDynamicProgrammingAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class ExactDynamicProgrammingAcceptanceService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.DYNAMICPROGRAMMINGTREE, false);
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.ORDERSET);
		return output;
	}
	
	public OrderSet runExactDynamicProgramming(Integer periodNumber) throws ParameterUnknownException{
		
		int bookingHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting= InputPreparator.getDemandSegmentWeighting(periodNumber);
		DemandSegmentSet dsSet = (DemandSegmentSet) demandSegmentWeighting.getSetEntity();;
		Double maximumRevenueValue=AcceptanceService.determineMaximumRevenueValueForNormalisation(dsSet);
		DynamicProgrammingTree tree = InputPreparator.getDynamicProgrammingTreeSet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, capacitySet.getDeliveryAreaSet(), demandSegmentWeighting);
		
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object> ();
		for(Entity entity: objectives){
			ObjectiveWeight ow = (ObjectiveWeight)entity;
			if(ow.getObjectiveType().getName().equals("local_visibility_factor")){
				objectiveSpecificValues.put(entity, AcceptanceService.prepareLocalVisibilityObjectiveForWeightedValueCalculation(capacitySet.getDeliveryAreaSet(), arrivalProcessId, daWeights, daSegmentWeightings));
			}else{//No specific values needed
				objectiveSpecificValues.put(entity, null);
			}
		}
		ExactDynamicProgrammingAcceptance algo = new ExactDynamicProgrammingAcceptance(tree,bookingHorizonLength,dsSet, capacitySet,orderRequestSet,objectiveSpecificValues,maximumRevenueValue);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);
	}
	
}
