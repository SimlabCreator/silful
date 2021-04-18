package logic.process.efulfillment.orienteeringForLearning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.vr.OrienteeringOnRequestsService;

public class GRILSForLearningAdaptedProcess extends SubProcess {

	private OrienteeringOnRequestsService orienteeringService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.orienteeringService = new OrienteeringOnRequestsService("GRILS_adapted");
		algoServices.put(0, this.orienteeringService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		this.orienteeringService.runGRILSOrienteeringWithPreferenceListsAdapted(0);

	}

	public Boolean needDepotLocation() {
		
		return true;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

public Boolean multiplePeriodsPossible() {
	
	return false;
}
}
