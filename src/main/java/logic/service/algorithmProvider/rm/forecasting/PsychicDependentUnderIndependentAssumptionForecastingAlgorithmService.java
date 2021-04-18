package logic.service.algorithmProvider.rm.forecasting;

import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.forecasting.independentDemand.PsychicForIndependentDemandAssumptionWhenDependent;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

public class PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService implements AlgorithmProviderService {

	String algorithm;

	public PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService(String algorithm) {
		this.algorithm = algorithm;
	}

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.VALUEBUCKETSET, true);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.ALTERNATIVESET, false);

		
			String[] paras = PsychicForIndependentDemandAssumptionWhenDependent.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}


		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS);
		return output;
	}

	public ValueBucketForecastSet runAlgorithmDependentUnderIndependentAssumption(Integer periodNumber) {


		int bookingHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		
		//Demand segment weighting
		DemandSegmentWeighting demandSegmentWeighting =InputPreparator.getDemandSegmentWeighting(periodNumber);
		
		// Value bucket set
		ValueBucketSet valueBucketSet = InputPreparator.getValueBucketSet(periodNumber);

		// Delivery area set 
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);

		// Parameters stay constant
		// B
		Integer B = (int) Math.round(InputPreparator.getParameterValue(periodNumber, "B"));


		PsychicForIndependentDemandAssumptionWhenDependent algo = new PsychicForIndependentDemandAssumptionWhenDependent(bookingHorizonLength, arrivalProcessId,
				demandSegmentWeighting, deliveryAreaSet,B, valueBucketSet);

		algo.start();

		return ResultHandler.organizeValueBucketForecastSetResult(algo, periodNumber);

	}

	


	

	

	

}
