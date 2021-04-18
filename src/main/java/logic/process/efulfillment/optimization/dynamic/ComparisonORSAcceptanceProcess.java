package logic.process.efulfillment.optimization.dynamic;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.StaticOrderAcceptanceAlgorithmService;

public class ComparisonORSAcceptanceProcess extends SubProcess {

	private StaticOrderAcceptanceAlgorithmService acceptanceService;



	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.acceptanceService = new StaticOrderAcceptanceAlgorithmService("No_controls");
		algoServices.put(0, this.acceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		this.acceptanceService.runAlgorithmWithoutControls(0);


	}

	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

public Boolean multiplePeriodsPossible() {
	
	return false;
}
}
