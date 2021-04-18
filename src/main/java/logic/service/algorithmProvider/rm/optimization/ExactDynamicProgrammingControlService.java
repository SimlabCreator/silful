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
import data.entity.WeightEntity;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.control.ExactDynamicProgrammingControls;
import logic.algorithm.rm.optimization.control.ExactDynamicProgrammingThreadsControls;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

public class ExactDynamicProgrammingControlService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.DYNAMICPROGRAMMINGTREE);
		return output;
	}

	public DynamicProgrammingTree runExactDynamicProgramming(Integer periodNumber) throws ParameterUnknownException {

		int bookingHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double maximumRevenueValue=AcceptanceService.determineMaximumRevenueValueForNormalisation((DemandSegmentSet) demandSegmentWeighting.getSetEntity());

		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, capacitySet.getDeliveryAreaSet(), demandSegmentWeighting);

		for(DeliveryArea area: daWeights.keySet()){
			System.out.println("Delivery area Id: "+area.getId()+" weight: "+daWeights.get(area));
			DemandSegmentWeighting  test = daSegmentWeightings.get(area);
			for(WeightEntity entity:test.getWeights()){
				System.out.println("Weight: "+entity.getWeight());
			}
		}
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object> ();
		for(Entity entity: objectives){
			ObjectiveWeight ow = (ObjectiveWeight)entity;
			if(ow.getObjectiveType().getName().equals("local_visibility_factor")){
//				objectiveSpecificValues.put(entity, AcceptanceService.prepareLocalVisibilityObjectiveForWeightedValueCalculation(capacitySet.getDeliveryAreaSet(), arrivalProcessId, daWeights, daSegmentWeightings));
//				Object[] objectiveValues = (Object[]) objectiveSpecificValues.get(entity);
//				//The first entry are the local visibility values
//				@SuppressWarnings("unchecked")
//				HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = (HashMap<DeliveryArea, HashMap<Alternative, Double>>) objectiveValues[0];
//				//The second entry is the maximum value
//				Double maximumLocalVisibility = objectiveValues[1];
//				System.out.println("Maximum arrival measure value: "+maximumLocalVisibility);
//				for(DeliveryArea area: localVisibilityValues.IdSet()){
//					for(Alternative alt:localVisibilityValues.get(area).IdSet()){
//						System.out.println("Equity measure for area "+area.getId()+" and alternative "+alt.getId()+" is "+(maximumLocalVisibility-localVisibilityValues.get(area).get(alt))/maximumLocalVisibility);
//					}
//				}
				

			}else{//No specific values needed
				objectiveSpecificValues.put(entity, null);
			}
		}

		ExactDynamicProgrammingControls algo = new ExactDynamicProgrammingControls(bookingHorizonLength,
				arrivalProcessId, demandSegmentWeighting, daWeights, daSegmentWeightings, capacitySet, objectiveSpecificValues,maximumRevenueValue);
		algo.start();
		return ResultHandler.organizeDynamicProgrammingTreeResult(algo, periodNumber);
	}
	
	public DynamicProgrammingTree runExactDynamicProgrammingThreads(Integer periodNumber) throws ParameterUnknownException {

		int bookingHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		Double maximumRevenueValue=AcceptanceService.determineMaximumRevenueValueForNormalisation((DemandSegmentSet) demandSegmentWeighting.getSetEntity());

		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, capacitySet.getDeliveryAreaSet(), demandSegmentWeighting);

		for(DeliveryArea area: daWeights.keySet()){
			System.out.println("Delivery area Id: "+area.getId()+" weight: "+daWeights.get(area));
			DemandSegmentWeighting  test = daSegmentWeightings.get(area);
			for(WeightEntity entity:test.getWeights()){
				System.out.println("Weight: "+entity.getWeight());
			}
		}
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object> ();
		for(Entity entity: objectives){
			ObjectiveWeight ow = (ObjectiveWeight)entity;
			if(ow.getObjectiveType().getName().equals("local_visibility_factor")){
//				objectiveSpecificValues.put(entity, AcceptanceService.prepareLocalVisibilityObjectiveForWeightedValueCalculation(capacitySet.getDeliveryAreaSet(), arrivalProcessId, daWeights, daSegmentWeightings));
//				Object[] objectiveValues = (Object[]) objectiveSpecificValues.get(entity);
//				//The first entry are the local visibility values
//				@SuppressWarnings("unchecked")
//				HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = (HashMap<DeliveryArea, HashMap<Alternative, Double>>) objectiveValues[0];
//				//The second entry is the maximum value
//				Double maximumLocalVisibility = objectiveValues[1];
//				System.out.println("Maximum arrival measure value: "+maximumLocalVisibility);
//				for(DeliveryArea area: localVisibilityValues.IdSet()){
//					for(Alternative alt:localVisibilityValues.get(area).IdSet()){
//						System.out.println("Equity measure for area "+area.getId()+" and alternative "+alt.getId()+" is "+(maximumLocalVisibility-localVisibilityValues.get(area).get(alt))/maximumLocalVisibility);
//					}
//				}
				

			}else{//No specific values needed
				objectiveSpecificValues.put(entity, null);
			}
		}

		ExactDynamicProgrammingThreadsControls algo = new ExactDynamicProgrammingThreadsControls(bookingHorizonLength,
				arrivalProcessId, demandSegmentWeighting, daWeights, daSegmentWeightings, capacitySet, objectiveSpecificValues,maximumRevenueValue);
		algo.start();
		return ResultHandler.organizeDynamicProgrammingTreeResult(algo, periodNumber);
	}

}
