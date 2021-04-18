package logic.process.efulfillment.forecasting;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.forecasting.PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService;

public class StaticDemandForecastPsychicDependentUnderIndependentDemandAssumptionProcess extends SubProcess{

	private PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService forecastingService;


	public Boolean needIncentiveType() {
		
		return false;
	}


	public Boolean needBookingPeriodLength() {
		
		return true;
	}

	public SettingRequest getSettingRequest() {
		
		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.forecastingService = new PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService("Psychic");
		algoServices.put(0, this.forecastingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {
			
		System.out.println("I start");
		this.forecastingService.runAlgorithmDependentUnderIndependentAssumption(0);
		
	}
	public Boolean needDepotLocation() {
		
		return false;
	}
	
	public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
