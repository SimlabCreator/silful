package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.ALNSFinalRoutingService;

public class ALNSFinalRoutingWithInfeasibleProcess extends SubProcess{ 
	private ALNSFinalRoutingService routingService;

	
	public Boolean needVehicles() {
		
		return true;

	}

	public Boolean needIncentiveType() {
		
		return false;
	}


	public Boolean needBookingPeriodLength() {
		
		return false;
	}

	public SettingRequest getSettingRequest() {
		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.routingService = new ALNSFinalRoutingService("ALNSFinalRoutingWithInfeasible");
		algoServices.put(0, this.routingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {
		this.routingService.runALNSFinalRoutingWithInfeasible(0);
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


