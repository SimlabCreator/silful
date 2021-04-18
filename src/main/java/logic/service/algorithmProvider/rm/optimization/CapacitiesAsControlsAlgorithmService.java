package logic.service.algorithmProvider.rm.optimization;

import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.control.NoFurtherControls;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

public class CapacitiesAsControlsAlgorithmService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.ALTERNATIVESET, false);

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.CONTROLSET);
		return output;
	}
	
	public ControlSet runAlgorithmNoFurtherControls(Integer periodNumber){
		
		CapacitySet capacitySet = InputPreparator.getCapacitySet(periodNumber);
		
		AlternativeSet alternativeSet = InputPreparator.getAlternativeSet(periodNumber);
		NoFurtherControls algo = new NoFurtherControls(capacitySet, alternativeSet);
		algo.start();
		return ResultHandler.organizeControlSetResult(algo, periodNumber);
	}
	
}
