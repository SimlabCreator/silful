package logic.service.algorithmProvider.rm.optimization;

import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.ValueBucketForecastSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.control.DeterministicEMSRb;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

public class StaticControlAlgorithmService implements AlgorithmProviderService {

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.CAPACITYSET, false);
		request.addPeriodSetting(PeriodSettingType.ALTERNATIVESET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS, false);

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.CONTROLSET);
		return output;
	}
	

	
	public ControlSet runAlgorithmDeterministicEMSRb(Integer periodNumber){
		
		//Input
		CapacitySet capacitySet =InputPreparator.getCapacitySet(periodNumber);
		ValueBucketForecastSet demandForecastSet =InputPreparator.getValueBucketForecastSet(periodNumber);
		
		//Get result
		DeterministicEMSRb algo = new DeterministicEMSRb(capacitySet, demandForecastSet);
		algo.start();
		return ResultHandler.organizeControlSetResult(algo, periodNumber);
	}
}
