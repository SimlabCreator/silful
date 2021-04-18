package logic.service.algorithmProvider.vr;

import data.entity.CapacitySet;
import data.entity.DeliveryAreaSet;
import data.entity.Routing;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.capacity.CapacityAggregation;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

public class CapacityAlgorithmService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.INITIALROUTING, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.CAPACITYSET);
		return output;
	}

	public CapacitySet runSimpleAggreationCapacityAlgorithm(int periodNumber) {

		//Input
		///Get initial routing (t=-1)
		Routing routing =InputPreparator.getRouting(periodNumber, -1);
		///Delivery area set
		DeliveryAreaSet daSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		
		// Build capacities
		CapacityAggregation algo = new CapacityAggregation(routing, daSet);
		algo.start();
		return ResultHandler.organizeCapacitySetResult(algo, periodNumber);
	}

	

}
