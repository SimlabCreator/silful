package logic.service.algorithmProvider.vr;

import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.routingBasedAcceptance.CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand;
import logic.algorithm.vr.routingBasedAcceptance.RoutingBasedAcceptance;
import logic.algorithm.vr.routingBasedAcceptance.Yang2016_Hindsight_DynamicSlotting;
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
public class RoutingBasedAcceptanceWithVehicleAssignmentsService implements AlgorithmProviderService {

	private String algorithm;

	public RoutingBasedAcceptanceWithVehicleAssignmentsService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.TIMEWINDOWSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);

		if (algorithm != null && algorithm.equals("CampbellSavelsbergh2006_FeasibilityCheckForDependentDemand")) {
			String[] paras = CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}else if(algorithm != null && algorithm.equals("Yang2016_Hindsight_DynamicSlotting")) {
			
			String[] paras = Yang2016_Hindsight_DynamicSlotting.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.FINALROUTING);
		output.addOutput(PeriodSettingType.ORDERSET);
		return output;
	}

	public Routing runDynamicAcceptanceBasedOnCampbellAndSavelsbergh2006(int periodNumber) {

		// Input
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);

		// Construct routing
		CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand algo = new CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand(orderRequestSet,
				deliveryAreaSet,vehicleAreaAssignmentSet,
				InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"), InputPreparator.getParameterValue(periodNumber, "no_routing_candidates"),
				InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates"),InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
				InputPreparator.getParameterValue(periodNumber, "samplePreferences"),InputPreparator.getParameterValue(periodNumber, "consider_profit"));
	
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);

	}
	
	public Routing runHindsightDynamicSlottingAcceptanceBasedOnYang2016(int periodNumber) {

		// Input
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);

		// Construct routing
		Yang2016_Hindsight_DynamicSlotting algo = new Yang2016_Hindsight_DynamicSlotting(orderRequestSet,
				deliveryAreaSet,vehicleAreaAssignmentSet,
				InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"), InputPreparator.getParameterValue(periodNumber, "no_routing_candidates"),
				InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
				InputPreparator.getParameterValue(periodNumber, "samplePreferences"));
	
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);

	}


}
