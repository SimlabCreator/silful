package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.LinearProgrammingAcceptanceService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.utility.exceptions.ParameterUnknownException;

public class DeterministicLinearProgrammingAcceptanceProcess extends SubProcess {

	private LinearProgrammingAcceptanceService acceptanceService;
	private FinalFeasibilityCheckService fService;
	
	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.acceptanceService = new LinearProgrammingAcceptanceService("ParallelFlightsAcceptance");
		algoServices.put(0, this.acceptanceService);
		this.fService = new FinalFeasibilityCheckService("Final_feasibility_cheapest_insertion");
		algoServices.put(1, this.fService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.acceptanceService.startAcceptanceBasedOnStaticCapacityAssignment(0);
			this.fService.runCheapestInsertionWithILSBasedFinalRouting(0);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Boolean needDepotLocation() {
		
		return false;
	}

	public Boolean multipleObjectivesPossible() {
		
		return true;
	}

}
