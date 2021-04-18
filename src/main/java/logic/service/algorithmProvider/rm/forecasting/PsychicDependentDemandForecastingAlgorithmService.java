package logic.service.algorithmProvider.rm.forecasting;

import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecastSet;
import data.entity.DemandSegmentWeighting;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.forecasting.independentDemand.PsychicForDependentDemand;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

public class PsychicDependentDemandForecastingAlgorithmService implements AlgorithmProviderService {

	String algorithm;

	public PsychicDependentDemandForecastingAlgorithmService() {

	}

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.DEMANDFORECASTSET_DEMANDSEGMENTS);
		return output;
	}

	public DemandSegmentForecastSet runAlgorithmPsychicDependentDemand(Integer periodNumber) {

		int bookingHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		DemandSegmentWeighting demandSegmentWeighting =InputPreparator.getDemandSegmentWeighting(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		
		PsychicForDependentDemand algo = new PsychicForDependentDemand(bookingHorizonLength, arrivalProcessId, demandSegmentWeighting,deliveryAreaSet);

		algo.start();

		return ResultHandler.organizeDemandSegmentForecastSetResult(algo, periodNumber);

	}

}
