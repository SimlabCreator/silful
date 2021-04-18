package logic.process.efulfillment.orsProcesses;

import java.util.HashMap;

import data.utility.SettingRequest;
import logic.process.efulfillment.SubProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.algorithmProvider.rm.optimization.ExactDynamicProgrammingControlService;
import logic.utility.exceptions.ParameterUnknownException;

public class DynamicProgrammingDecisionTreeORSProcess extends SubProcess {

	private ExactDynamicProgrammingControlService dynamicProgrammingControlService;


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		this.algoServices = new HashMap<Integer, AlgorithmProviderService>();
		this.dynamicProgrammingControlService = new ExactDynamicProgrammingControlService();
		algoServices.put(0, this.dynamicProgrammingControlService);
		
		SettingRequest request = super.getSettingRequest();
		return request;
	}

	public void start() {

		System.out.println("I start");
		//Calculate buffers and let requests arrive
		try {
			this.dynamicProgrammingControlService.runExactDynamicProgrammingThreads(0);
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
