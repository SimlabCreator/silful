package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPOrienteeringAcceptanceService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.utility.InputPreparator;
import logic.utility.exceptions.ParameterUnknownException;

public class ANNOrienteeringAcceptanceProcess extends SubProcess {

	private ADPOrienteeringAcceptanceService aDPOrienteeringService;
//	private FinalFeasibilityCheckService routingService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.aDPOrienteeringService = new ADPOrienteeringAcceptanceService("ADPWithOrienteeringANNAcceptance");
		algoServices.put(0, this.aDPOrienteeringService);
//		this.routingService = new FinalFeasibilityCheckService("Final_feasibility_cheapest_insertion");
//		algoServices.put(1, this.routingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.aDPOrienteeringService.startANNApproximationAcceptance(0);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			//this.routingService.runCheapestInsertionWithILSBasedFinalRouting(0);
		

	}

	public Boolean needDepotLocation() {
		
		return false;
	}

	public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
