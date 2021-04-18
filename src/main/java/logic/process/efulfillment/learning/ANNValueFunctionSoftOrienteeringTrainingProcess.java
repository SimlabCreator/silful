package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPOrienteeringService;
import logic.service.algorithmProvider.rm.optimization.ADPService;
import logic.utility.exceptions.ParameterUnknownException;

public class ANNValueFunctionSoftOrienteeringTrainingProcess extends SubProcess {

	private ADPOrienteeringService aDPOrienteeringService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.aDPOrienteeringService = new ADPOrienteeringService("ADPWithSoftOrienteeringANN");
		algoServices.put(0, this.aDPOrienteeringService);
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.aDPOrienteeringService.startANNFunctionApproximationSoftOrienteering(0);
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
