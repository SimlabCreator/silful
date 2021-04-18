package logic.service.algorithmProvider.rm.optimization;

import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.acceptance.SerialNesting;
import logic.algorithm.rm.optimization.acceptance.StaticWithoutRevenueControl;
import logic.algorithm.rm.optimization.acceptance.StaticWithoutRevenueControlNoControlSet;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

public class StaticOrderAcceptanceAlgorithmService implements AlgorithmProviderService {

	private String algorithm;
	
	//TODO: Just solution for now to omit control request for comparison without controls
	public StaticOrderAcceptanceAlgorithmService(String algorithm){
		this.algorithm=algorithm;
	}
	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		if(!this.algorithm.equals("No_controls")){
			request.addPeriodSetting(PeriodSettingType.CONTROLSET, false);
		}
		
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.ALTERNATIVESET, false);
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.ORDERSET);
		return output;
	}

	public OrderSet runAlgorithmStaticControlsWithoutValueBuckets(Integer periodNumber) {
		// Input
		ControlSet controlSet = InputPreparator.getControlSet(periodNumber);
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		AlternativeSet alternativeSet = InputPreparator.getAlternativeSet(periodNumber);

		// Get result
		StaticWithoutRevenueControl algo = new StaticWithoutRevenueControl(controlSet, capacitySet, orderRequestSet,
				alternativeSet);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);
	}
	
	public OrderSet runAlgorithmWithoutControls(Integer periodNumber) {
		// Input
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		AlternativeSet alternativeSet = InputPreparator.getAlternativeSet(periodNumber);

		// Get result
		StaticWithoutRevenueControlNoControlSet algo = new StaticWithoutRevenueControlNoControlSet(capacitySet, orderRequestSet,
				alternativeSet);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);
	}
	
	

	public OrderSet runAlgorithmSerialNesting(Integer periodNumber) {
		// Input
		ControlSet controlSet = InputPreparator.getControlSet(periodNumber);
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);

		// Get result
		SerialNesting algo = new SerialNesting(controlSet, capacitySet, orderRequestSet);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);
	}
}
