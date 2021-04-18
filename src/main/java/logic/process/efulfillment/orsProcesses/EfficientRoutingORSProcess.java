package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.CapacityAlgorithmService;
import logic.service.algorithmProvider.vr.InitialConstructionAlgorithmServiceForDependentDemand;

public class EfficientRoutingORSProcess extends SubProcess{
	
	private InitialConstructionAlgorithmServiceForDependentDemand initConstructionService;
	private CapacityAlgorithmService capacityService;
	

	public Boolean needIncentiveType() {
		
		return false;
	}


	public Boolean needBookingPeriodLength() {
		
		return false;
	}

	public SettingRequest getSettingRequest() {
		
		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.initConstructionService = new InitialConstructionAlgorithmServiceForDependentDemand("InsertionConstructionHeuristic");
		algoServices.put(0, this.initConstructionService);
		this.capacityService = new CapacityAlgorithmService();
		algoServices.put(1, this.capacityService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {
		
		System.out.println("I start");
		//Define routing
		this.initConstructionService.runInsertionConstructionHeuristicFromDemandForecastRatioRequestsWithoutValuePriority(0);
	
		//Build capacities
		this.capacityService.runSimpleAggreationCapacityAlgorithm(0);
		
	}


	public Boolean multiplePeriodsPossible() {
		
		return false;
	}
	
	public Boolean needDepotLocation() {
		
		return true;
	}
	
	public Boolean multipleObjectivesPossible() {
		
		return false;
	}
}
