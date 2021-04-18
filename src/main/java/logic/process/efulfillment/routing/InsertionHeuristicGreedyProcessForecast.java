package logic.process.efulfillment.routing;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.ConstructionGreedyAlgorithmService;
import logic.utility.exceptions.ParameterUnknownException;

public class InsertionHeuristicGreedyProcessForecast extends SubProcess {

	private ConstructionGreedyAlgorithmService initConstructionService;
	//private CapacityAlgorithmService capacityService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {
	
		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.initConstructionService = new ConstructionGreedyAlgorithmService("InsertionConstructionHeuristicGreedy");
		algoServices.put(0, this.initConstructionService);
		//this.capacityService=new CapacityAlgorithmService();
		//algoServices.put(1, this.capacityService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		try {
			this.initConstructionService.runInsertionConstructionHeuristicGreedyForecast(0);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//this.capacityService.runSimpleAggreationCapacityAlgorithm(0);


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
