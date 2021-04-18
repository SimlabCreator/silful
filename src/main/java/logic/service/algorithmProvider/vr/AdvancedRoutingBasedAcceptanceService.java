package logic.service.algorithmProvider.vr;

import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.OrderRequestSet;
import data.entity.ProbabilityDistribution;
import data.entity.Routing;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.routingBasedAcceptance.CampbellSavelsbergh2005_dependentDemand;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;

/**
 * Provides initial routing construction algorithms for dependent demand
 * 
 * @author M. Lang
 *
 */
public class AdvancedRoutingBasedAcceptanceService implements AlgorithmProviderService {

	private String algorithm;

	public AdvancedRoutingBasedAcceptanceService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVAL_PROBABILITY_DISTRIBUTION, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.TIMEWINDOWSET, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);

		if (algorithm != null && algorithm.equals("CampbellSavelsbergh2005_dependentDemand")) {
			String[] paras = CampbellSavelsbergh2005_dependentDemand.getParameterSetting();
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

	public Routing runDynamicAcceptanceBasedOnCampbellAndSavelsbergh2005(int periodNumber) {

		ProbabilityDistribution arrDis = InputPreparator.getArrivalProbabilityDistribution(periodNumber);
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		CampbellSavelsbergh2005_dependentDemand algo = new CampbellSavelsbergh2005_dependentDemand(arrDis,
				orderRequestSet, demandSegmentWeighting, deliveryAreaSet, vehicleAreaAssignmentSet,
				InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"),
				InputPreparator.getParameterValue(periodNumber, "no_routing_candidates"),
				InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates"),
				InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
				InputPreparator.getParameterValue(periodNumber, "samplePreferences"),
				InputPreparator.getParameterValue(periodNumber, "consider_REG"),
				InputPreparator.getParameterValue(periodNumber, "cost_multiplier"));
		
		algo.start();
		return ResultHandler.organizeOrderSetAndRoutingResult(algo, periodNumber);
	}

}
