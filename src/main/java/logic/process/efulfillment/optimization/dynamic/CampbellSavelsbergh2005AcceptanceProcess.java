package logic.process.efulfillment.optimization.dynamic;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.AdvancedRoutingBasedAcceptanceService;

public class CampbellSavelsbergh2005AcceptanceProcess extends SubProcess {

	private AdvancedRoutingBasedAcceptanceService advancedRoutingBasedAcceptanceService;



	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.advancedRoutingBasedAcceptanceService = new AdvancedRoutingBasedAcceptanceService("CampbellSavelsbergh2005_dependentDemand");
		algoServices.put(0, this.advancedRoutingBasedAcceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		//Calculate buffers and let requests arrive
		
			this.advancedRoutingBasedAcceptanceService.runDynamicAcceptanceBasedOnCampbellAndSavelsbergh2005(0);
		


	}

	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}
}
