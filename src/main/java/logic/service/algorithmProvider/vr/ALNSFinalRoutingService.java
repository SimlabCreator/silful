package logic.service.algorithmProvider.vr;


import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.OrderSet;
import data.entity.Routing;
import data.entity.Vehicle;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.ALNS.ALNSFinalRouting;
import logic.algorithm.vr.ALNS.ALNSFinalRoutingWithInfeasible;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;


public class ALNSFinalRoutingService implements AlgorithmProviderService  {

	private String algorithm;

	public ALNSFinalRoutingService(String algorithm) {
		this.algorithm = algorithm;
	}

	// change to accepted orders
	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		
		// common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLES, false);
			
		//Possibly needed parameter settings that are individual to the algorithm
		if (algorithm != null) {
			String[] paras = new String[] {"Constant_service_time", "stop_once_feasible"};
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		} 
		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.FINALROUTING);
		return output;
	}
	
	
	public Routing runALNSFinalRouting(int periodNumber) {
		
		// Input
		Depot depot = SettingsProvider.getExperiment().getDepot();
		OrderSet orderSet = InputPreparator.getOrderSet(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		ArrayList<Vehicle> vehicles = InputPreparator.getVehicles(periodNumber);	

		// Construct routing
		ALNSFinalRouting algo = new ALNSFinalRouting(orderSet, depot, expectedServiceTime, vehicles);

		algo.start();
		return ResultHandler.organizeFinalRoutingResult(algo, periodNumber);
	}
	
	public Routing runALNSFinalRoutingWithInfeasible(int periodNumber) {
		
		// Input
		Depot depot = SettingsProvider.getExperiment().getDepot();
		OrderSet orderSet = InputPreparator.getOrderSet(periodNumber);
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double stopOnceFeasible = InputPreparator.getParameterValue(periodNumber, "stop_once_feasible");
		ArrayList<Vehicle> vehicles = InputPreparator.getVehicles(periodNumber);
		DeliveryAreaSet daSet = InputPreparator.getDeliveryAreaSet(periodNumber);


		// Construct routing
		ALNSFinalRoutingWithInfeasible algo = new ALNSFinalRoutingWithInfeasible(orderSet, depot, daSet, expectedServiceTime, stopOnceFeasible, vehicles);

		algo.start();
		return ResultHandler.organizeFinalRoutingResult(algo, periodNumber);
	}
}

