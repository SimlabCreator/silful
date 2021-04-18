package logic.process.efulfillment.forecasting;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.forecasting.ValueBucketForecastingAlgorithmService;

public class StaticDemandForecastExponentialSmoothingProcess extends SubProcess {

	private ValueBucketForecastingAlgorithmService forecastingService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.forecastingService = new ValueBucketForecastingAlgorithmService("ExponentialSmoothing");
		algoServices.put(0, this.forecastingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		this.forecastingService.runAlgorithmExponentialSmoothing(0);

	}

	public Boolean needDepotLocation() {
		
		return false;
	}

	public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
