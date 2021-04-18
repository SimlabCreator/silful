package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.RoutingBasedAcceptanceService;

public class FcfsORSRUNTIMECOMPARISONProcess extends SubProcess {

	private RoutingBasedAcceptanceService acceptanceService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.acceptanceService = new RoutingBasedAcceptanceService("RoutingBasedAcceptance");
		algoServices.put(0, this.acceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		this.acceptanceService.runDynamicAcceptanceBasedOnRoutingRuntimeComparison(0);


	}

	public Boolean needDepotLocation() {
		
		return true;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

public Boolean multiplePeriodsPossible() {
	
	return false;
}
}
