package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPOrienteeringAcceptanceService;
import logic.service.algorithmProvider.vr.ALNSFinalRoutingService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.service.algorithmProvider.vr.OrienteeringBasedAcceptanceService;
import logic.service.algorithmProvider.vr.RoutingBasedAcceptanceWithVehicleAssignmentsService;
import logic.utility.InputPreparator;
import logic.utility.exceptions.ParameterUnknownException;

public class FCFSInsertionAndALNSFinalProcess extends SubProcess {

	private RoutingBasedAcceptanceWithVehicleAssignmentsService advancedRoutingBasedAcceptanceService;
	private ALNSFinalRoutingService routingService;

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
		this.routingService = new ALNSFinalRoutingService("ALNSFinalRoutingWithInfeasible");
		algoServices.put(1, this.routingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		
			this.advancedRoutingBasedAcceptanceService.runDynamicAcceptanceBasedOnCampbellAndSavelsbergh2006(0);
		
	
		
			this.routingService.runALNSFinalRouting(0);

	}

	public Boolean needDepotLocation() {
		
		return true;
	}

	public Boolean multipleObjectivesPossible() {
		
		return true;
	}

}
