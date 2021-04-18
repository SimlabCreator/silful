package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.forecasting.PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService;
import logic.service.algorithmProvider.rm.optimization.StaticControlAlgorithmService;
import logic.service.algorithmProvider.vr.CapacityAlgorithmService;
import logic.service.algorithmProvider.vr.InitialConstructionAlgorithmServiceForIndependentDemandAdapted;

public class IndependentDemandAssumptionControlsORSProcess extends SubProcess{

	private PsychicDependentUnderIndependentAssumptionForecastingAlgorithmService forecastingService;
	private InitialConstructionAlgorithmServiceForIndependentDemandAdapted initConstructionService;
	private CapacityAlgorithmService capacityService;
	private StaticControlAlgorithmService controlService;
	

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
		this.initConstructionService = new InitialConstructionAlgorithmServiceForIndependentDemandAdapted("InsertionConstructionHeuristic");
		algoServices.put(1, this.initConstructionService);
		this.capacityService = new CapacityAlgorithmService();
		algoServices.put(2, this.capacityService);	
		this.controlService= new StaticControlAlgorithmService();
		algoServices.put(3, this.controlService);	
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {
			
		System.out.println("I start");
		this.forecastingService.runAlgorithmDependentUnderIndependentAssumption(0);
		this.initConstructionService.runInsertionConstructionHeuristicFromDemandForecast(0);
		this.capacityService.runSimpleAggreationCapacityAlgorithm(0);
		this.controlService.runAlgorithmDeterministicEMSRb(0);
		
	}
	public Boolean needDepotLocation() {
		
		return true;
	}
	
	public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
