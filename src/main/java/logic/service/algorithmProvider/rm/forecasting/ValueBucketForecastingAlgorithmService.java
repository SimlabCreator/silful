package logic.service.algorithmProvider.rm.forecasting;

import java.util.ArrayList;

import data.entity.AlternativeSet;
import data.entity.DeliveryAreaSet;
import data.entity.ValueBucketForecastSet;
import data.entity.OrderSet;
import data.entity.ValueBucketSet;
import data.utility.DataServiceProvider;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.forecasting.independentDemand.ExponentialSmoothing;
import logic.algorithm.rm.forecasting.independentDemand.PsychicForIndependentDemand;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

public class ValueBucketForecastingAlgorithmService implements AlgorithmProviderService {

	String algorithm;

	public ValueBucketForecastingAlgorithmService(String algorithm) {
		this.algorithm = algorithm;
	}

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.HISTORICALORDERS, false);
		request.addPeriodSetting(PeriodSettingType.HISTORICALDEMANDFORECASTSET_VALUEBUCKETS, true);
		request.addPeriodSetting(PeriodSettingType.VALUEBUCKETSET, true);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.ALTERNATIVESET, false);

		if (algorithm != null && algorithm.equals("ExponentialSmoothing")) {
			String[] paras = ExponentialSmoothing.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		} else if (algorithm != null && algorithm.equals("Psychic")) {
			String[] paras = PsychicForIndependentDemand.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS);
		return output;
	}

	public ValueBucketForecastSet runAlgorithmExponentialSmoothing(Integer periodNumber) {

		// Historical orders
		ArrayList<OrderSet> historicalOrders = InputPreparator.getHistoricalOrders(periodNumber);

		// Historical demand forecast
		ValueBucketForecastSet historicalDemandForecastSet = InputPreparator.getHistoricalDemandForecastSet(periodNumber);

		// Value bucket set

		ValueBucketSet valueBucketSet = InputPreparator.getValueBucketSet(periodNumber);

		// Delivery area set 
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);

		// Alternative set stays constant
		AlternativeSet alternativeSet = InputPreparator.getAlternativeSet(periodNumber);

		// Parameters stay constant
		// Alpha
		Double alpha = InputPreparator.getParameterValue(periodNumber, "alpha");

		// B
		Integer B = (int) Math.round(InputPreparator.getParameterValue(periodNumber, "B"));

		ExponentialSmoothing algo = new ExponentialSmoothing(historicalOrders, historicalDemandForecastSet,
				valueBucketSet, deliveryAreaSet, alternativeSet, B, alpha);

		algo.start();

		return ResultHandler.organizeValueBucketForecastSetResult(algo, periodNumber);

	}

	public ValueBucketForecastSet runAlgorithmPsychicForecastForIndependentDemand(Integer periodNumber) {
		OrderSet historicalOrders;
		if (periodNumber == 0) {
			historicalOrders = (OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
					.getSetById(SettingsProvider.getPeriodSetting().getHistoricalOrderSetId());
		} else {// If it is not the first period, the respective orderset of the
				// last period is the historical order set
			if (periodNumber == 1) {
				historicalOrders = (OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getOrderSetId());
			}else{
			historicalOrders = (OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
					.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber - 1).getOrderSetId());
			}
		}

		// Value bucket set

		ValueBucketSet valueBucketSet = InputPreparator.getValueBucketSet(periodNumber);

		// Delivery area set stays constant
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);

		// Alternative set stays constant
		AlternativeSet alternativeSet = InputPreparator.getAlternativeSet(periodNumber);

		// B
		Integer B = (int) Math.round(InputPreparator.getParameterValue(periodNumber, "B"));

		PsychicForIndependentDemand algo = new PsychicForIndependentDemand(historicalOrders, valueBucketSet,
				deliveryAreaSet, alternativeSet, B);
		algo.start();

		return ResultHandler.organizeValueBucketForecastSetResult(algo, periodNumber);
	}
	


	

	

	

}
