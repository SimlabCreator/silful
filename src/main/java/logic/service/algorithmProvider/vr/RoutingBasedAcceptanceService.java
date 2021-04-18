package logic.service.algorithmProvider.vr;

import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.OrderRequestSet;
import data.entity.ProbabilityDistribution;
import data.entity.Region;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.Vehicle;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.routingBasedAcceptance.CampbellSavelsbergh2005_dependentDemand;
import logic.algorithm.vr.routingBasedAcceptance.RoutingBasedAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

/**
 * Provides initial routing construction algorithms for dependent demand
 * 
 * @author M. Lang
 *
 */
public class RoutingBasedAcceptanceService implements AlgorithmProviderService {

	private String algorithm;

	public RoutingBasedAcceptanceService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.TIMEWINDOWSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLES, false);

		if (algorithm != null && algorithm.equals("RoutingBasedAcceptance")) {
			String[] paras = RoutingBasedAcceptance.getParameterSetting();
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

	public Routing runDynamicAcceptanceBasedOnRouting(int periodNumber) {

		// Input
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		TimeWindowSet timeWindowSet = InputPreparator.getTimeWindowSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();
		ArrayList<Vehicle> vehicles = InputPreparator.getVehicles(periodNumber);
		Depot depot = SettingsProvider.getExperiment().getDepot();
		Region region = SettingsProvider.getExperiment().getRegion();

		// Construct routing
		RoutingBasedAcceptance algo = new RoutingBasedAcceptance(orderRequestSet, timeWindowSet, depot, vehicles,
				distances, nodes, deliveryAreaSet, false,
				InputPreparator.getParameterValue(periodNumber, "directDistances"),
				InputPreparator.getParameterValue(periodNumber, "time_dependent_travel_times"),
				InputPreparator.getParameterValue(periodNumber, "samplePreferences"), region);
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);

	}

	public Routing runDynamicAcceptanceBasedOnRoutingRuntimeComparison(int periodNumber) {

		// Input
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		TimeWindowSet timeWindowSet = InputPreparator.getTimeWindowSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();
		ArrayList<Vehicle> vehicles = InputPreparator.getVehicles(periodNumber);
		Depot depot = SettingsProvider.getExperiment().getDepot();
		Region region = SettingsProvider.getExperiment().getRegion();
		
		// Construct routing
		RoutingBasedAcceptance algo = new RoutingBasedAcceptance(orderRequestSet, timeWindowSet, depot, vehicles,
				distances, nodes, deliveryAreaSet, true,
				InputPreparator.getParameterValue(periodNumber, "directDistances"),
				InputPreparator.getParameterValue(periodNumber, "time_dependent_travel_times"),
				InputPreparator.getParameterValue(periodNumber, "samplePreferences"), region);
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);

	}
	

}
