package logic.service.algorithmProvider.vr;

import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Routing;
import data.entity.TravelTimeSet;
import data.entity.ValueBucketForecastSet;
import data.entity.Vehicle;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.construction.InsertionConstructionHeuristicGreedy;
import logic.algorithm.vr.construction.Old_InsertionConstructionHeuristic;
import logic.entity.ForecastedOrderRequest;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.CapacityService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

/**
 * Helper for initial routing methods
 * 
 * @author M. Lang
 *
 */
public class InitialConstructionAlgorithmServiceForIndependentDemand implements AlgorithmProviderService {

	private String algorithm;

	public InitialConstructionAlgorithmServiceForIndependentDemand(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		// Common setting types
		request.addPeriodSetting(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS, false);
		//request.addPeriodSetting(PeriodSettingType.TRAVELTIMESET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLES, false);

		// Possibly needed parameter settings that are individual to the
		// algorithm
		if (algorithm != null && algorithm.equals("InsertionConstructionHeuristic")) {
			String[] paras = InsertionConstructionHeuristicGreedy.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.INITIALROUTING);
		return output;
	}

	public Routing runInsertionConstructionHeuristicFromDemandForecast(int periodNumber) {

		// Input
		ValueBucketForecastSet demandForecastSet = InputPreparator.getValueBucketForecastSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = demandForecastSet.getDeliveryAreaSet();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();
		TravelTimeSet travelTimeSet = InputPreparator.getTravelTimeSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<Vehicle> vehicles= InputPreparator.getVehicles(periodNumber);

		// Transfer forecasts to pseudo-requests and set all service times constant as provided by the user
		ArrayList<ForecastedOrderRequest> requests = CapacityService
				.getForecastedOrderRequestsByValueBucketForcastSet(demandForecastSet);
		Double serviceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		for(ForecastedOrderRequest request: requests){
			request.setServiceTime(serviceTime);
		}

		// Construct routing
		Old_InsertionConstructionHeuristic algo = new Old_InsertionConstructionHeuristic(requests,
				demandForecastSet.getAlternativeSet().getTimeWindowSet(), SettingsProvider.getExperiment().getDepot(),
				vehicles, distances, nodes, travelTimeSet, deliveryAreaSet);
		algo.start();
		return ResultHandler.organizeInitialRoutingResult(algo, periodNumber);
	}

}
