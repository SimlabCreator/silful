package logic.service.algorithmProvider.vr;

import java.util.ArrayList;

import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.Vehicle;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.orienteering.GRILSOrienteeringAdapted;
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
public class OrienteeringOnRequestsService implements AlgorithmProviderService {
	String algorithm;

	public OrienteeringOnRequestsService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.TIMEWINDOWSET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLES, false);

		
		if (algorithm != null && algorithm.equals("GRILS_adapted")) {
			String[] paras = GRILSOrienteeringAdapted.getParameterSetting();
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

	
	
	public Routing runGRILSOrienteeringWithPreferenceListsAdapted(int periodNumber) {

		// Input
		Region region = SettingsProvider.getExperiment().getRegion();
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		TimeWindowSet timeWindowSet = InputPreparator.getTimeWindowSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();
		ArrayList<Vehicle> vehicles= InputPreparator.getVehicles(periodNumber);
		Depot depot = SettingsProvider.getExperiment().getDepot();

		// Construct routing
		GRILSOrienteeringAdapted algo = new GRILSOrienteeringAdapted(region,
				orderRequestSet, timeWindowSet, vehicles, nodes, distances, depot,
				InputPreparator.getParameterValue(periodNumber, "greediness_upperBound"),
				InputPreparator.getParameterValue(periodNumber, "greediness_lowerBound"),
				InputPreparator.getParameterValue(periodNumber, "greediness_stepsize"),
				InputPreparator.getParameterValue(periodNumber, "maximumRoundsWithoutImprovement"),
				InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
				InputPreparator.getParameterValue(periodNumber, "alternative_randomization_approach_(boolean)"),
				InputPreparator.getParameterValue(periodNumber, "maximumNumberOfSolutions"),
				InputPreparator.getParameterValue(periodNumber, "maximumRoundsWithoutImprovementLocalSearch"),
				InputPreparator.getParameterValue(periodNumber, "maximumNumberOfSolutionsLocalSearch"),
				InputPreparator.getParameterValue(periodNumber, "squaredValue"),
				InputPreparator.getParameterValue(periodNumber, "actualBasketValue"),
				InputPreparator.getParameterValue(periodNumber, "twSelectionOption_(0:greedy,1:random,2:popularity)"),
				InputPreparator.getParameterValue(periodNumber, "locationClusterProbability"),
				InputPreparator.getParameterValue(periodNumber, "thresholdAcceptance"),
				InputPreparator.getParameterValue(periodNumber, "directDistances"));
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);

	}
	
}
