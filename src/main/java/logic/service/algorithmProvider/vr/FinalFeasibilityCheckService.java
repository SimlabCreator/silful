package logic.service.algorithmProvider.vr;

import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.finalFeasibilityCheck.ILSWithGRASPForFinalRouting;
import logic.algorithm.vr.finalFeasibilityCheck.ParallelInsertionWithGRASPforFinalRouting;
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
public class FinalFeasibilityCheckService implements AlgorithmProviderService {
	String algorithm;

	public FinalFeasibilityCheckService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERSET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);

		if (algorithm != null && algorithm.equals("Final_feasibility_cheapest_insertion")) {
			String[] paras = ILSWithGRASPForFinalRouting.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		if (algorithm != null && algorithm.equals("ParallelInsertionWithGRASPforFinalRouting")) {
			String[] paras = ParallelInsertionWithGRASPforFinalRouting.getParameterSetting();
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

	/**
	 * GRILS adapted to low number of infeasible and no relevance of value
	 * 
	 * @param periodNumber
	 * @return
	 */
	public Routing runCheapestInsertionWithILSBasedFinalRouting(int periodNumber) {

		// Input
		Region region = SettingsProvider.getExperiment().getRegion();
		Depot depot = SettingsProvider.getExperiment().getDepot();
		OrderSet orderSet = InputPreparator.getOrderSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		Double onlyCostBased = InputPreparator.getParameterValue(periodNumber, "onlyCostBased");
		Double squaredValue = InputPreparator.getParameterValue(periodNumber, "squaredValue");

		// Construct routing
		ILSWithGRASPForFinalRouting algo = new ILSWithGRASPForFinalRouting(region, depot, orderSet,
				InputPreparator.getParameterValue(periodNumber, "greediness_upperBound"),
				InputPreparator.getParameterValue(periodNumber, "greediness_lowerBound"),
				InputPreparator.getParameterValue(periodNumber, "greediness_stepsize"),
				InputPreparator.getParameterValue(periodNumber, "maximumRoundsWithoutImprovement"),
				InputPreparator.getParameterValue(periodNumber, "maximumNumberOfSolutions"), vehicleAreaAssignmentSet,
				InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
				InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"), onlyCostBased,
				squaredValue);

		algo.start();
		return ResultHandler.organizeFinalRoutingResult(algo, periodNumber);

	}

	/**
	 * Parallel insertion heuristic with GRASP (adapted from Campbell &
	 * Savelsbergh 2006)
	 * 
	 * @param periodNumber
	 * @return
	 */
	public Routing runParallelInsertionWithGRASPBasedFinalRouting(int periodNumber) {

		// Input

		OrderSet orderSet = InputPreparator.getOrderSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);


		// Construct routing
		ParallelInsertionWithGRASPforFinalRouting algo = new ParallelInsertionWithGRASPforFinalRouting(orderSet,
				vehicleAreaAssignmentSet.getDeliveryAreaSet(), vehicleAreaAssignmentSet,
				InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"),
				InputPreparator.getParameterValue(periodNumber, "no_routing_candidates_final"),
				InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates_final"),
				InputPreparator.getParameterValue(periodNumber, "Constant_service_time"));

		algo.start();
		return ResultHandler.organizeFinalRoutingResult(algo, periodNumber);

	}

}
