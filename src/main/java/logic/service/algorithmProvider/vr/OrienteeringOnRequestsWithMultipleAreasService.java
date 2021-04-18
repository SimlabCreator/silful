package logic.service.algorithmProvider.vr;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.Output;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.algorithm.vr.orienteering.GRILSOrienteeringAdaptedWithVehicleAreaAssignment;
import logic.algorithm.vr.orienteering.GRILSOrienteeringWithVehicleAreaAssignment;
import logic.service.algorithmProvider.AlgorithmProviderService;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.ResultHandler;
import logic.utility.SettingsProvider;

/**
 * Provides initial routing construction algorithms for dependent demand
 * 
 * @author M. Lang
 *
 */
public class OrienteeringOnRequestsWithMultipleAreasService implements AlgorithmProviderService {
	String algorithm;

	public OrienteeringOnRequestsWithMultipleAreasService(String algorithm) {
		this.algorithm = algorithm;
	};

	public SettingRequest getSettingRequest() {
		SettingRequest request = new SettingRequest();

		// Common setting types
		request.addPeriodSetting(PeriodSettingType.ORDERREQUESTSET, false);
		request.addPeriodSetting(PeriodSettingType.TIMEWINDOWSET, false);
		request.addPeriodSetting(PeriodSettingType.VEHICLE_ASSIGNMENT_SET, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		
		
		if (algorithm != null && algorithm.equals("GRILSOrienteeringAdaptedWithVehicleAreaAssignment")) {
			String[] paras = GRILSOrienteeringAdaptedWithVehicleAreaAssignment.getParameterSetting();
			for (int i = 0; i < paras.length; i++) {
				request.addParameter(paras[i]);
			}
		}
		
		if (algorithm != null && algorithm.equals("GRILSOrienteeringWithVehicleAreaAssignment")) {
			String[] paras = GRILSOrienteeringWithVehicleAreaAssignment.getParameterSetting();
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
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		int orderHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		TimeWindowSet timeWindowSet = InputPreparator.getTimeWindowSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();

		//Seperate order requests per delivery area (middle level)
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		HashMap<DeliveryArea, Double> daWeightsLower = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeightsLower,
				daSegmentWeightingsLower, deliveryAreaSet, demandSegmentWeighting);
		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(daWeightsUpper,
				daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);
		
		HashMap<DeliveryArea, ArrayList<OrderRequest>> orPerDA = new HashMap<DeliveryArea, ArrayList<OrderRequest>>();
		for(DeliveryArea area: deliveryAreaSet.getElements()){
			orPerDA.put(area, new ArrayList<OrderRequest>());
		}
		
		for(OrderRequest request: orderRequestSet.getElements()){
			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, request.getCustomer());
			
			//Only add request if no-purchase alternative is not the best
			if(request.getAlternativeIdWithHighestPreference()!=orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet().getNoPurchaseAlternative().getId()){
				orPerDA.get(area).add(request);
			}
			
		}
		
		ArrayList<Routing> resultRoutings = new ArrayList<Routing>();
		for(DeliveryArea area: deliveryAreaSet.getElements()){
			OrderRequestSet set = new OrderRequestSet();
			set.setId(orderRequestSet.getId());
			set.setCustomerSetId(orderRequestSet.getCustomerSetId());
			set.setElements(orPerDA.get(area));
			
			ArrayList<VehicleAreaAssignment> relevantAssignments = new ArrayList<VehicleAreaAssignment>();
			for(VehicleAreaAssignment va: vehicleAreaAssignmentSet.getElements()){
				if(va.getDeliveryAreaId().equals(area.getId())){
					relevantAssignments.add(va);
				}
			}
			
			HashMap<DeliveryArea, Double> daWeightsLowerArea = new HashMap<DeliveryArea, Double>();
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerArea = new HashMap<DeliveryArea, DemandSegmentWeighting>();
			for(DeliveryArea subArea: area.getSubset().getElements()){
				daWeightsLowerArea.put(subArea, daWeightsLower.get(subArea));
				daSegmentWeightingsLowerArea.put(subArea, daSegmentWeightingsLower.get(subArea));
			}
			
			double expectedArrivals = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)*orderHorizonLength*daWeightsUpper.get(area);
			
			// Construct routing
			GRILSOrienteeringAdaptedWithVehicleAreaAssignment algo = new GRILSOrienteeringAdaptedWithVehicleAreaAssignment(region,
					set,demandSegmentWeighting, timeWindowSet, relevantAssignments, area.getSubset(), nodes, distances,
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
					InputPreparator.getParameterValue(periodNumber, "twSelectionOption_(0:greedy,1:random,2:popularity,3:0+2)"),
					InputPreparator.getParameterValue(periodNumber, "locationClusterProbability"),
					InputPreparator.getParameterValue(periodNumber, "thresholdAcceptance"),
					InputPreparator.getParameterValue(periodNumber, "directDistances"),
					InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"),
					InputPreparator.getParameterValue(periodNumber, "consider_demand_probability"), 
					daWeightsLowerArea, daSegmentWeightingsLowerArea, expectedArrivals,
					InputPreparator.getParameterValue(periodNumber, "soft_saturation_limit"));
			algo.start();
			
			resultRoutings.add(algo.getResult());
		}
		
		//Combine routings in one large routing
		Routing routing = new Routing();
		ArrayList<Order> orders = new ArrayList<Order>();
		ArrayList<Route> routes = new ArrayList<Route>();
		String information="";
		for(Routing r: resultRoutings){
			orders.addAll(r.getOrderSet().getElements());
			routes.addAll(r.getRoutes());
			information+=r.getAdditionalInformation()+";";
		}
		OrderSet orderSet = new OrderSet();
		orderSet.setOrderRequestSetId(orderRequestSet.getId());
		orderSet.setElements(orders);
		routing.setOrderSet(orderSet);
		routing.setPossiblyFinalRouting(true);
		routing.setPossiblyTarget(true);
		routing.setRoutes(routes);
		routing.setTimeWindowSetId(timeWindowSet.getId());
		routing.setAdditionalInformation(information);
		routing.setVehicleAreaAssignmentSetId(vehicleAreaAssignmentSet.getId());

		return ResultHandler.organizeOrderSetAndRoutingResult(routing, periodNumber);

	}
	
	public Routing runGRILSOrienteeringWithPreferenceLists(int periodNumber) {

		// Input 
		Region region = SettingsProvider.getExperiment().getRegion();
		OrderRequestSet orderRequestSet = InputPreparator.getOrderRequestSet(periodNumber);
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet = InputPreparator.getVehicleAreaAssignmentSet(periodNumber);
		DemandSegmentWeighting demandSegmentWeighting = InputPreparator.getDemandSegmentWeighting(periodNumber);
		int arrivalProcessId = SettingsProvider.getPeriodSetting().getArrivalProcessId();
		int orderHorizonLength = SettingsProvider.getExperiment().getBookingPeriodLength();
		TimeWindowSet timeWindowSet = InputPreparator.getTimeWindowSet(periodNumber);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();

		//Seperate order requests per delivery area (middle level)
		DeliveryAreaSet deliveryAreaSet = vehicleAreaAssignmentSet.getDeliveryAreaSet();
		HashMap<DeliveryArea, Double> daWeightsLower = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeightsLower,
				daSegmentWeightingsLower, deliveryAreaSet, demandSegmentWeighting);
		HashMap<DeliveryArea, Double> daWeightsUpper = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(daWeightsUpper,
				daSegmentWeightingsUpper, deliveryAreaSet, demandSegmentWeighting);
		
		HashMap<DeliveryArea, ArrayList<OrderRequest>> orPerDA = new HashMap<DeliveryArea, ArrayList<OrderRequest>>();
		for(DeliveryArea area: deliveryAreaSet.getElements()){
			orPerDA.put(area, new ArrayList<OrderRequest>());
		}
		
		for(OrderRequest request: orderRequestSet.getElements()){
			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, request.getCustomer());
			
			//Only add request if no-purchase alternative is not the best
			if(request.getAlternativeIdWithHighestPreference()!=orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet().getNoPurchaseAlternative().getId()){
				orPerDA.get(area).add(request);
			}
			
		}
		
		ArrayList<Routing> resultRoutings = new ArrayList<Routing>();
		for(DeliveryArea area: deliveryAreaSet.getElements()){
			OrderRequestSet set = new OrderRequestSet();
			set.setId(orderRequestSet.getId());
			set.setCustomerSetId(orderRequestSet.getCustomerSetId());
			set.setElements(orPerDA.get(area));
			
			ArrayList<VehicleAreaAssignment> relevantAssignments = new ArrayList<VehicleAreaAssignment>();
			for(VehicleAreaAssignment va: vehicleAreaAssignmentSet.getElements()){
				if(va.getDeliveryAreaId().equals(area.getId())){
					relevantAssignments.add(va);
				}
			}
			
			HashMap<DeliveryArea, Double> daWeightsLowerArea = new HashMap<DeliveryArea, Double>();
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerArea = new HashMap<DeliveryArea, DemandSegmentWeighting>();
			for(DeliveryArea subArea: area.getSubset().getElements()){
				daWeightsLowerArea.put(subArea, daWeightsLower.get(subArea));
				daSegmentWeightingsLowerArea.put(subArea, daSegmentWeightingsLower.get(subArea));
			}
			
			double arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)*daWeightsUpper.get(area);
			
			// Construct routing
			GRILSOrienteeringWithVehicleAreaAssignment algo = new GRILSOrienteeringWithVehicleAreaAssignment(region,
					set,demandSegmentWeighting, timeWindowSet, relevantAssignments, area.getSubset(), nodes, distances,
					InputPreparator.getParameterValue(periodNumber, "greediness_upperBound"),
					InputPreparator.getParameterValue(periodNumber, "greediness_lowerBound"),
					InputPreparator.getParameterValue(periodNumber, "greediness_stepsize"),
					InputPreparator.getParameterValue(periodNumber, "maximumRoundsWithoutImprovement"),
					InputPreparator.getParameterValue(periodNumber, "Constant_service_time"),
					InputPreparator.getParameterValue(periodNumber, "maximumNumberOfSolutions"),
					InputPreparator.getParameterValue(periodNumber, "squaredValue"),
					InputPreparator.getParameterValue(periodNumber, "actualBasketValue"),
					InputPreparator.getParameterValue(periodNumber, "directDistances"),
					InputPreparator.getParameterValue(periodNumber, "includeDriveFromStartingPosition"),orderHorizonLength,
					arrivalProbability,
					InputPreparator.getParameterValue(periodNumber, "duplicate_segments"),
					InputPreparator.getParameterValue(periodNumber, "weight_time_window_prob"),
					InputPreparator.getParameterValue(periodNumber, "weight_arrival_prob"),
					InputPreparator.getParameterValue(periodNumber, "imp_for_insertion")
					);
			algo.start();
			
			resultRoutings.add(algo.getResult());
		}
		
		//Combine routings in one large routing
		Routing routing = new Routing();
		ArrayList<Order> orders = new ArrayList<Order>();
		ArrayList<Route> routes = new ArrayList<Route>();
		String information="";
		for(Routing r: resultRoutings){
			orders.addAll(r.getOrderSet().getElements());
			routes.addAll(r.getRoutes());
			information+=r.getAdditionalInformation()+";";
		}
		OrderSet orderSet = new OrderSet();
		orderSet.setOrderRequestSetId(orderRequestSet.getId());
		orderSet.setElements(orders);
		routing.setOrderSet(orderSet);
		routing.setPossiblyFinalRouting(true);
		routing.setPossiblyTarget(true);
		routing.setRoutes(routes);
		routing.setTimeWindowSetId(timeWindowSet.getId());
		routing.setAdditionalInformation(information);
		routing.setVehicleAreaAssignmentSetId(vehicleAreaAssignmentSet.getId());
		String areaWeighting = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			areaWeighting = mapper.writeValueAsString(daWeightsLower);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		routing.setAreaWeighting(areaWeighting);
		
		//Time window demand saving 
		SegmentDemandWrapper w = new SegmentDemandWrapper(demandSegmentWeighting);
		w.setDaSegmentWeightingsLower(daSegmentWeightingsLower, demandSegmentWeighting.getWeights().size());
		w.setMaximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow(CustomerDemandService
				.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
						daSegmentWeightingsLower, timeWindowSet));
		w.setMinimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow(CustomerDemandService
				.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
						daSegmentWeightingsLower, timeWindowSet));

		String areaDsWeighting = null;
		mapper = new ObjectMapper();
		try {
			areaDsWeighting = mapper.writeValueAsString(w);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		routing.setAreaDsWeighting(areaDsWeighting);
		return ResultHandler.organizeOrderSetAndRoutingResult(routing, periodNumber);

	}
	private class SegmentDemandWrapper{
		HashMap<Integer, Integer> demandSegmentMapping = new HashMap<Integer, Integer>();
		double[][] daSegmentWeightingsLower;
		HashMap<Integer, double[]> dswl;
		
		public SegmentDemandWrapper(DemandSegmentWeighting dsw){
			int i=0;
			for(DemandSegmentWeight w: dsw.getWeights()){
				demandSegmentMapping.put(w.getElementId(), i);
				i++;
			}
		}
		
		public HashMap<Integer, double[]> getDaSegmentWeightingsLower() {
			
			return dswl;
		}
		
		public void setDaSegmentWeightingsLower(HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower, int numberOfSegments) {
			this.daSegmentWeightingsLower= new double[daSegmentWeightingsLower.keySet().size()][];
			this.dswl= new HashMap<Integer, double[]>();
			int i=0;
			for(DeliveryArea area: daSegmentWeightingsLower.keySet()){
				this.daSegmentWeightingsLower[i] = new double[numberOfSegments];
				dswl.put(area.getId(), new double[numberOfSegments]);
				int j=0;
				for(data.entity.DemandSegmentWeight w: daSegmentWeightingsLower.get(area).getWeights()){
					this.daSegmentWeightingsLower[i][j]=w.getWeight();
					dswl.get(area.getId())[demandSegmentMapping.get(w.getElementId())]=w.getWeight();
					j++;
				}
				i++;
			}

		}
		
		
		HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		
		public HashMap<DeliveryArea, HashMap<TimeWindow, Double>> getMaximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow() {
			return maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		}
		public void setMaximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow(
				HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow) {
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		}
		public HashMap<DeliveryArea, HashMap<TimeWindow, Double>> getMinimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow() {
			return minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		}
		public void setMinimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow(
				HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow) {
			for(DeliveryArea area: minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.keySet()){
				minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area).remove(null);
			}
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
		}
	}
}


