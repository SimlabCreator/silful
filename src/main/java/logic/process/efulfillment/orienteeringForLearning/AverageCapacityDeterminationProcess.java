package logic.process.efulfillment.orienteeringForLearning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.CapacityAlgorithmMultipleRoutingsService;

public class AverageCapacityDeterminationProcess extends SubProcess {

	private CapacityAlgorithmMultipleRoutingsService capacityService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.capacityService = new CapacityAlgorithmMultipleRoutingsService();
		algoServices.put(0, this.capacityService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		this.capacityService.runSimpleAggregationCapacityAlgorithm(0);

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
