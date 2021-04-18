package logic.service.algorithmProvider.vr;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.rm.optimization.learning.DeterministicProgrammingBasedAcceptance;
import logic.algorithm.vr.routingBasedAcceptance.OrienteeringBasedAcceptance;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.AcceptanceService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides initial routing construction algorithms for dependent demand
 * 
 * @author M. Lang
 *
 */
public class OrienteeringBasedAcceptanceService implements AlgorithmProviderService {

	private String algorithm;

	public OrienteeringBasedAcceptanceService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DELIVERYAREASET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
		request.addPeriodSetting(PeriodSettingType.LEARNING_FINAL_ROUTING, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);

		if (algorithm != null && algorithm.equals("OrienteeringBasedAcceptance")) {
			String[] paras = OrienteeringBasedAcceptance.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}

		if (algorithm != null && algorithm.equals("DeterministicProgrammingBasedAcceptance")) {
			String[] paras = DeterministicProgrammingBasedAcceptance.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		

		return request;
	}

	public Output getOutput() {
		Output output = new Output();
		output.addOutput(PeriodSettingType.ORDERSET);
		return output;
	}

	public OrderSet runOrienteeringBasedAcceptance(int periodNumber) throws ParameterUnknownException {

		// Input
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting= InputPreparator.getDemandSegmentWeighting(periodNumber);
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		Region region = SettingsProvider.getExperiment().getRegion();
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double noRoutingCan = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double noInsertionCan = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double dynamicRouting = InputPreparator.getParameterValue(periodNumber, "dynamic_feasibility_check");
		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
		Double theftBasedAd = InputPreparator.getParameterValue(periodNumber, "theft-based-advanced");
		
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
		
		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		OrienteeringBasedAcceptance algo = new OrienteeringBasedAcceptance(region, vehicleAreaAssignmentSet,
				routingsForLearning, orderRequestSet, deliveryAreaSet,
				 expectedServiceTime, 
				 includeDriveFromStartingPosition,  
				 samplePreferences, noRoutingCan,
				 noInsertionCan, dynamicRouting, theftBased, theftBasedAd, neighbors, daWeights, daSegmentWeightings, arrivalProcessId);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}
	
	public OrderSet runDeterministicProgrammingBasedAcceptance(int periodNumber) throws ParameterUnknownException {

		// Input
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting= InputPreparator.getDemandSegmentWeighting(periodNumber);
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		int orderHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		DeliveryAreaSet deliveryAreaSet = InputPreparator.getDeliveryAreaSet(periodNumber);
		Region region = SettingsProvider.getExperiment().getRegion();
		HashMap<Integer, ArrayList<Routing>> routingsForLearningH = InputPreparator.getLearningFinalRoutings(periodNumber);
		 ArrayList<Routing> routingsForLearning = new ArrayList<Routing>();
		for(Integer e: routingsForLearningH.keySet()){
			routingsForLearning.addAll(routingsForLearningH.get(e));
		}
		Double expectedServiceTime = InputPreparator.getParameterValue(periodNumber, "Constant_service_time");
		Double includeDriveFromStartingPosition = InputPreparator.getParameterValue(periodNumber,
				"includeDriveFromStartingPosition");
		Double samplePreferences = InputPreparator.getParameterValue(periodNumber, "samplePreferences");
		Double noRoutingCan = InputPreparator.getParameterValue(periodNumber, "no_routing_candidates");
		Double noInsertionCan = InputPreparator.getParameterValue(periodNumber, "no_insertion_candidates");
		Double dynamicRouting = InputPreparator.getParameterValue(periodNumber, "dynamic_feasibility_check");
		Double theftBased = InputPreparator.getParameterValue(periodNumber, "theft-based");
		Double theftBasedAd = InputPreparator.getParameterValue(periodNumber, "theft-based-advanced");
		Double beta = InputPreparator.getParameterValue(periodNumber, "beta");
		Double dupSeg = InputPreparator.getParameterValue(periodNumber, "duplicate_segments");
		
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue = AcceptanceService.determineMaximumRevenueValueForNormalisation(
				orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet());
		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();
		for (Entity entity : objectives) {

			objectiveSpecificValues.put(entity, null);
		}
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
		
		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = LocationService.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(deliveryAreaSet);
		
		DeterministicProgrammingBasedAcceptance algo = new DeterministicProgrammingBasedAcceptance(region, orderHorizonLength, vehicleAreaAssignmentSet,
				routingsForLearning, orderRequestSet, deliveryAreaSet,
				 expectedServiceTime, 
				 includeDriveFromStartingPosition,  
				 samplePreferences, noRoutingCan,
				 noInsertionCan, dynamicRouting, theftBased, theftBasedAd, neighbors, demandSegmentWeighting, arrivalProcessId, beta, maximumRevenueValue,objectiveSpecificValues,dupSeg);
		algo.start();
		return ResultHandler.organizeOrderSetResult(algo, periodNumber);

	}

}
