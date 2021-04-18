package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.StaticOrderAcceptanceAlgorithmService;

public class IndependentDemandAssumptionAcceptanceORSProcess extends SubProcess {

	private StaticOrderAcceptanceAlgorithmService acceptanceService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.acceptanceService = new StaticOrderAcceptanceAlgorithmService("controls");
		algoServices.put(0, this.acceptanceService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");

		// Start booking horizon with order acceptance step
		this.acceptanceService.runAlgorithmSerialNesting(0);
	}
	
	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
