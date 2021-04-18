package logic.process.efulfillment.learning;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ADPAcceptanceService;
import logic.service.algorithmProvider.vr.FinalFeasibilityCheckService;
import logic.utility.exceptions.ParameterUnknownException;

public class LinearValueFunctionAsYangAcceptanceProcess extends SubProcess {

	private ADPAcceptanceService aDPService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return false;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.aDPService = new ADPAcceptanceService("ADPAsYangAcceptance");
		algoServices.put(0, this.aDPService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Calculate forecast
		try {
			this.aDPService.startLinearFunctionApproximationAcceptanceAsYang(0);
		
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
