package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPOrienteeringAcceptanceService;
import logic.service.algorithmProvider.vr.ALNSFinalRoutingService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.service.algorithmProvider.vr.OrienteeringBasedAcceptanceService;
import logic.utility.InputPreparator;
import logic.utility.exceptions.ParameterUnknownException;

public class DeterministicProgrammingAcceptanceProcess extends SubProcess {

	private OrienteeringBasedAcceptanceService orienteeringService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.orienteeringService = new OrienteeringBasedAcceptanceService("DeterministicProgrammingBasedAcceptance");
		algoServices.put(0, this.orienteeringService);
	
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.orienteeringService.runDeterministicProgrammingBasedAcceptance(0);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		

	}

	public Boolean needDepotLocation() {
		
		return true;
	}

	public Boolean multipleObjectivesPossible() {
		
		return true;
	}

}
