package logic.process.efulfillment.optimization.dynamic;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.RoutingBasedAcceptanceWithVehicleAssignmentsService;

public class Yang2016Algorithm1ForDynamicSlottingProcess extends SubProcess {

	private RoutingBasedAcceptanceWithVehicleAssignmentsService advancedRoutingBasedAcceptanceService;



	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.advancedRoutingBasedAcceptanceService = new RoutingBasedAcceptanceWithVehicleAssignmentsService("Yang2016_Hindsight_DynamicSlotting");
		algoServices.put(0, this.advancedRoutingBasedAcceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		//Calculate buffers and let requests arrive
		
			this.advancedRoutingBasedAcceptanceService.runHindsightDynamicSlottingAcceptanceBasedOnYang2016(0);
		


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
