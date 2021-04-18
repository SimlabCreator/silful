package logic.process.efulfillment.optimization.sequential;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.StaticControlAlgorithmService;
import logic.service.algorithmProvider.rm.optimization.StaticOrderAcceptanceAlgorithmService;

public class StaticControlAndAcceptanceAsBasicPaperProcess extends SubProcess {

	private StaticControlAlgorithmService controlService;
	private StaticOrderAcceptanceAlgorithmService acceptanceService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.controlService = new StaticControlAlgorithmService();
		algoServices.put(0, this.controlService);
		this.acceptanceService = new StaticOrderAcceptanceAlgorithmService("controls");
		algoServices.put(1, this.acceptanceService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate controls
		this.controlService.runAlgorithmDeterministicEMSRb(0);

		// Start booking horizon with order acceptance step
		this.acceptanceService.runAlgorithmSerialNesting(0);
	}
	
	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
