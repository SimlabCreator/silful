package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ExactDynamicProgrammingAcceptanceService;
import logic.utility.exceptions.ParameterUnknownException;

public class DependentDemandAcceptanceORSProcess extends SubProcess {

	private ExactDynamicProgrammingAcceptanceService dynamicProgrammingAcceptanceService;

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.dynamicProgrammingAcceptanceService = new ExactDynamicProgrammingAcceptanceService();
		algoServices.put(0, this.dynamicProgrammingAcceptanceService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		//Calculate buffers and let requests arrive
		try {
			this.dynamicProgrammingAcceptanceService.runExactDynamicProgramming(0);
		} catch (ParameterUnknownException e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return true;
	}
}
