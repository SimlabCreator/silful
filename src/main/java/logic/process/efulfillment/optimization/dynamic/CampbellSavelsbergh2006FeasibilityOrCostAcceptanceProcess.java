package logic.process.efulfillment.optimization.dynamic;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.RoutingBasedAcceptanceWithVehicleAssignmentsService;

public class CampbellSavelsbergh2006FeasibilityOrCostAcceptanceProcess extends SubProcess {

	private RoutingBasedAcceptanceWithVehicleAssignmentsService advancedRoutingBasedAcceptanceService;



	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.advancedRoutingBasedAcceptanceService = new RoutingBasedAcceptanceWithVehicleAssignmentsService("CampbellSavelsbergh2006_FeasibilityCheckForDependentDemand");
		algoServices.put(0, this.advancedRoutingBasedAcceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		//Calculate buffers and let requests arrive
		
			this.advancedRoutingBasedAcceptanceService.runDynamicAcceptanceBasedOnCampbellAndSavelsbergh2006(0);
		


	}

	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

public Boolean multiplePeriodsPossible(){
	return false;
}
}
