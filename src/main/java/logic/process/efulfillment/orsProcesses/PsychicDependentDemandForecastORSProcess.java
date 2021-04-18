package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.forecasting.PsychicDependentDemandForecastingAlgorithmService;

public class PsychicDependentDemandForecastORSProcess extends SubProcess{

	private PsychicDependentDemandForecastingAlgorithmService forecastingService;

	public Boolean needIncentiveType() {
		
		return false;
	}


	public Boolean needBookingPeriodLength() {
		
		return true;
	}

	public SettingRequest getSettingRequest() {
		
		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.forecastingService = new PsychicDependentDemandForecastingAlgorithmService();
		algoServices.put(0, this.forecastingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {
		
		System.out.println("I start");
		this.forecastingService.runAlgorithmPsychicDependentDemand(0);
		
	}
	
	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
