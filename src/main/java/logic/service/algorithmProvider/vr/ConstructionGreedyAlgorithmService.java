package logic.service.algorithmProvider.vr;

import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecastSet;
import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.ObjectiveWeight;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.TravelTimeSet;
import data.entity.Vehicle;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.construction.InsertionConstructionHeuristicGreedy;
import logic.entity.ForecastedOrderRequest;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.CapacityService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides initial routing construction algorithms for dependent demand
 * 
 * @author C. K�hler
 *
 */
public class ConstructionGreedyAlgorithmService implements AlgorithmProviderService {

	private String algorithm;

	public ConstructionGreedyAlgorithmService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();
		
		//Common setting types
		request.addPeriodSetting(PeriodSettingType.DEMANDFORECASTSET_DEMANDSEGMENTS, false);
		request.addPeriodSetting(PeriodSettingType.TRAVELTIMESET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLES, false);

		//Possibly needed parameter settings that are individual to the algorithm
		if (algorithm != null && algorithm.equals("InsertionConstructionHeuristicGreedy")) {
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

	public Routing runInsertionConstructionHeuristicGreedyForecast(int periodNumber) throws ParameterUnknownException {

		// Input
		DemandSegmentForecastSet demandForecastSet = InputPreparator.getDemandSegmentForecastSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = demandForecastSet.getDeliveryAreaSet();
		TimeWindowSet timeWindowSet = demandForecastSet.getDemandSegmentSet().getAlternativeSet().getTimeWindowSet();
		TravelTimeSet travelTimeSet = InputPreparator.getTravelTimeSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();	
		Depot depot = SettingsProvider.getExperiment().getDepot();
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		ArrayList<Vehicle> vehicles= InputPreparator.getVehicles(periodNumber);
		Double maximumRevenueValue=AcceptanceService.determineMaximumRevenueValueForNormalisation(demandForecastSet.getDemandSegmentSet());

		// Transfer forecasts to pseudo-requests and set all service times constant as provided by the user

		ArrayList<ForecastedOrderRequest> requests = CapacityService.getForecastedOrderRequestsByDemandSegmentForecastSetDuplicates(demandForecastSet, nodes,objectives,maximumRevenueValue);
		
		Double serviceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		for(ForecastedOrderRequest request: requests){
			request.setServiceTime(serviceTime);

		}
		
		System.out.println("number of requests: "+requests.size());
		


		// Construct routing
		InsertionConstructionHeuristicGreedy algo = new InsertionConstructionHeuristicGreedy(requests,
				timeWindowSet,
				depot, vehicles, distances,nodes, travelTimeSet, deliveryAreaSet);
		algo.start();
		return ResultHandler.organizeInitialRoutingResult(algo, periodNumber);

	}
	

	

	

	


}
