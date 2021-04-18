package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPOrienteeringAcceptanceService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.service.algorithmProvider.vr.OrienteeringBasedAcceptanceService;
import logic.utility.InputPreparator;
import logic.utility.exceptions.ParameterUnknownException;

public class OrienteeringAcceptanceGRASPProcess extends SubProcess {

	private OrienteeringBasedAcceptanceService orienteeringService;
	private FinalFeasibilityCheckService routingService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.orienteeringService = new OrienteeringBasedAcceptanceService("OrienteeringBasedAcceptance");
		algoServices.put(0, this.orienteeringService);
		this.routingService = new FinalFeasibilityCheckService("ParallelInsertionWithGRASPforFinalRouting");
		algoServices.put(1, this.routingService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.orienteeringService.runOrienteeringBasedAcceptance(0);
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
			this.routingService.runParallelInsertionWithGRASPBasedFinalRouting(0);
	

	}

	public Boolean needDepotLocation() {
		
		return true;
	}

	public Boolean multipleObjectivesPossible() {
		
		return true;
	}

}
