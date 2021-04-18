package logic.process.efulfillment.optimization.dynamic;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.forecasting.PsychicDependentDemandForecastingAlgorithmService;
import logic.service.algorithmProvider.rm.optimization.ExactDynamicProgrammingControlService;
import logic.service.algorithmProvider.vr.CapacityAlgorithmService;
import logic.service.algorithmProvider.vr.InitialConstructionAlgorithmServiceForDependentDemand;
import logic.utility.exceptions.ParameterUnknownException;

public class ComparisonORSControlsProcess extends SubProcess {

	private PsychicDependentDemandForecastingAlgorithmService forecastingService;
	private InitialConstructionAlgorithmServiceForDependentDemand initConstructionService;
	private CapacityAlgorithmService capacityService;
	private ExactDynamicProgrammingControlService controlService;


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
		this.initConstructionService = new InitialConstructionAlgorithmServiceForDependentDemand("InsertionConstructionHeuristic");
		algoServices.put(1, this.initConstructionService);
		this.capacityService=new CapacityAlgorithmService();
		algoServices.put(2, this.capacityService);
		this.controlService = new ExactDynamicProgrammingControlService();
		algoServices.put(3, this.controlService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		this.forecastingService.runAlgorithmPsychicDependentDemand(0);
		this.initConstructionService.runInsertionConstructionHeuristicFromDemandForecastRatioRequestsWithoutValuePriority(0);
		this.capacityService.runSimpleAggreationCapacityAlgorithm(0);
		try {
			this.controlService.runExactDynamicProgrammingThreads(0);
		} catch (ParameterUnknownException e) {
			e.printStackTrace();
			System.exit(0);
		}


	}

	public Boolean needDepotLocation() {
		
		return true;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return true;
	}

public Boolean multiplePeriodsPossible() {
	
	return false;
}
}
