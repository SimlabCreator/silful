package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.DeterministicProgrammingLearningService;
import logic.utility.exceptions.ParameterUnknownException;

public class DeterministicProgrammingPreparationProcess extends SubProcess {

	private DeterministicProgrammingLearningService preparationService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.preparationService = new DeterministicProgrammingLearningService("DeterministicProgrammingPreparation");
		algoServices.put(0, this.preparationService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.preparationService.startDPAlgorithm(0);
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
