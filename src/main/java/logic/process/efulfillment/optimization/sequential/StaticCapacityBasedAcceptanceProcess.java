package logic.process.efulfillment.optimization.sequential;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.CapacitiesAsControlsAlgorithmService;
import logic.service.algorithmProvider.rm.optimization.StaticOrderAcceptanceAlgorithmService;

public class StaticCapacityBasedAcceptanceProcess extends SubProcess {

	private CapacitiesAsControlsAlgorithmService controlService;
	private StaticOrderAcceptanceAlgorithmService acceptanceService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.controlService = new CapacitiesAsControlsAlgorithmService();
		algoServices.put(0, this.controlService);
		this.acceptanceService = new StaticOrderAcceptanceAlgorithmService("controls");
		algoServices.put(1, this.acceptanceService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate controls
		this.controlService.runAlgorithmNoFurtherControls(0);

		// Start booking horizon with order acceptance step
		this.acceptanceService.runAlgorithmStaticControlsWithoutValueBuckets(0);

	}

	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}
}
