package logic.service.algorithmProvider.vr;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.CapacitySet;
import data.entity.DeliveryAreaSet;
import data.entity.Routing;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.capacity.CapacityAggregation;
import logic.algorithm.vr.capacity.CapacityAverage;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

public class CapacityAlgorithmMultipleRoutingsService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.CAPACITYSET);
		return output;
	}

	public CapacitySet runSimpleAggregationCapacityAlgorithm(int periodNumber) {

		//Input
		///Get learning routings
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routings = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routings.addAll(routingsForLearningH.get(e));
		}
		///Delivery area set
		DeliveryAreaSet daSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		
		// Build capacities
		ArrayList<CapacitySet> capacitySets = new ArrayList<CapacitySet>();
		for(Routing rou: routings){
			CapacityAggregation algo = new CapacityAggregation(rou, daSet);
			algo.start();
			capacitySets.add(algo.getResult());
		}
		
		CapacityAverage algoCap = new CapacityAverage(capacitySets);
		
		algoCap.start();
		return ResultHandler.organizeCapacitySetResult(algoCap, periodNumber);
	}

	

}
