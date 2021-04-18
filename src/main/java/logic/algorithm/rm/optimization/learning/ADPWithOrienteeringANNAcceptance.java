package logic.algorithm.rm.optimization.learning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.ReferenceRouting;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.entity.ArtificialNeuralNetwork;
import logic.entity.AssortmentAlgorithm;
import logic.service.support.ArrivalProcessService;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.OrienteeringAcceptanceHelperService;
import logic.service.support.RoutingService;
import logic.utility.SettingsProvider;
import logic.utility.comparator.DemandSegmentsExpectedValueDescComparator;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

/**
 * Info: Works with actual locations, not nodes
 * 
 * @author M. Lang
 *
 */
public class ADPWithOrienteeringANNAcceptance extends AggregateReferenceInformationAlgorithm
		implements AcceptanceAlgorithm {

	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static boolean OPPORTUNTIY_COSTS_SELECTION_MIN = true;
	private static double DISCOUNT_FACTOR = 1.0;
	private static double twoStealingMultiplier = 2.0;
	private static double DEMAND_RATIO_SCARCITY_BORDER = 0.0;

	private static double stealingScarcityMultiplier = 0.95;
	private static double demandRatioRevenue = 0.05;
	private static double MIN_RADIUS = 2.0;
	private static double costMultiplier = 0.3;
	private ArrayList<DemandSegment> demandSegments;
	private static boolean possiblyLargeOfferSet = true;
	private HashMap<Integer, HashMap<Integer, Double>> maximumOpportunityCostsPerLowerDaAndTimeWindow;
	private AlternativeSet alternativeSet;
	private ValueFunctionApproximationModelSet modelSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private OrderSet orderSet;
	private DemandSegmentWeighting demandSegmentWeighting;
	private Double discountingFactorProbability;

	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;
	private HashMap<DeliveryArea, HashMap<Routing, Integer>> acceptablePerDeliveryAreaAndRouting;
	private int numberRoutingCandidates;
	private int numberInsertionCandidates;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, Double> daWeightsUpper;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpper;
	private HashMap<DeliveryArea, Double> daWeightsLower;
	private HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightingsLowerHash;
	private double maximumLowerAreaWeight;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLower;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private int arrivalProcessId;
	private double arrivalProbability;
	private HashMap<Integer, NeuralNetwork> ANNPerDeliveryArea;
	private HashMap<Integer, Double> neighborWeightsPerDeliveryArea;
	private HashMap<Integer, Double> maximumValuePerDeliveryArea;
	private HashMap<Integer, Double> minimumValuePerDeliveryArea;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;

	private HashMap<Integer, ValueFunctionApproximationModel> valueFunctionApproximationPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> valueFunctionValuePerDeliveryAreaAndT;

	private boolean usepreferencesSampled;

	private boolean useActualBasketValue;

	private Alternative noPurchaseAlternative;

	private HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingCandidatePerDeliveryArea;
	private HashMap<Integer, Double> maximalAcceptableCostsPerDeliveryArea;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<Integer, Route>>> referenceRoutingsPerDeliveryArea;
	private ArrayList<Routing> referenceRoutingsList;
	private HashMap<Integer, HashMap<TimeWindow, Double>> demandMultiplierPerTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> demandMultiplierLowerAreaPerTimeWindow;
	private HashMap<Integer, HashMap<DemandSegment, HashMap<Alternative, Double>>> alternativeProbabilitiesPerDeliveryAreaAndDemandSegment;
	private HashMap<Integer, HashMap<Integer, Double>> maxDemandMultiplierLowerAreaPerUpperDeliveryAreaAndTimeWindow;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow;

	private HashMap<Integer, Double> valueMultiplierPerLowerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> valueMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow;
	private boolean theftBased;
	private boolean theftBasedAdvanced;
	private boolean theftBasedTw;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private int stealingCounter = 0;
	private boolean considerLeftOverPenalty;
	private HashMap<DeliveryArea, Double> initialOverallUtilityRatioAcrossTwsPerDeliveryArea;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> initialOverallUtilityRatioPerTw;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private double utilityShift;
	private boolean considerConstant;
	private boolean considerNeighborDemand;
	private boolean oppCostOnlyFeasible;
	private HashMap<Integer, Integer> segmentInputMapper;
	private HashMap<Integer, Integer> neighborSegmentInputMapper;
	private HashMap<Integer, Integer> timeWindowInputMapper;
	private HashMap<Integer, HashMap<Routing, Integer>> amountTheftsPerDeliveryArea;
	private HashMap<Integer, HashMap<Routing, Integer>> amountTheftsAdvancedPerDeliveryArea;
	private HashMap<Integer, HashMap<Routing, Integer>> amountTimeTheftsPerDeliveryArea;
	private HashMap<Integer, HashMap<Routing, Integer>> firstTheftsPerDeliveryArea;
	private HashMap<Integer, HashMap<Routing, Integer>> firstTheftsAdvancedPerDeliveryArea;
	private HashMap<Integer, HashMap<Routing, Integer>> firstTimeTheftsPerDeliveryArea;

	// TODO: Add orienteering booleans to VFA?
	private static String[] paras = new String[] { "actualBasketValue", "samplePreferences", "theft-based",
			"theft-based-advanced", "theft-based-tw", "consider_left_over_penalty", "discounting_factor_probability",
			"oc_for_feasible", "Constant_service_time"};

	public ADPWithOrienteeringANNAcceptance(ValueFunctionApproximationModelSet valueFunctionApproximationModelSet,
			DemandSegmentWeighting demandSegmentWeighting, Region region,
			VehicleAreaAssignmentSet vehicleAreaAssignmentSet, ArrayList<Routing> targetRoutingResults,
			OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue, Double actualBasketValue,
			int orderHorizonLength, Double samplePreferences, HashMap<DeliveryArea, Double> daWeightsUpperAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsUpperAreas,
			HashMap<DeliveryArea, Double> daWeightsLowerAreas,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightingsLowerAreas, Double theftBased,
			Double theftBasedAdvanced, Double theftBasedTw, Double considerLeftOverPenalty,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, int arrivalProcessId,
			Double discountingFactorProbability, Double oppCostOnlyFeasible, Double constantServiceTime) {

		AggregateReferenceInformationAlgorithm.setRegion(region);
		AggregateReferenceInformationAlgorithm.setExpectedServiceTime(constantServiceTime);
		this.orderRequestSet = orderRequestSet;
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.modelSet = valueFunctionApproximationModelSet;
		AggregateReferenceInformationAlgorithm.setPreviousRoutingResults(targetRoutingResults);
		AggregateReferenceInformationAlgorithm.setDeliveryAreaSet(deliveryAreaSet);
		this.daWeightsUpper = daWeightsUpperAreas;
		this.daSegmentWeightingsUpper = daSegmentWeightingsUpperAreas;
		this.daWeightsLower = daWeightsLowerAreas;
		this.daSegmentWeightingsLower = daSegmentWeightingsLowerAreas;
		AggregateReferenceInformationAlgorithm.setTimeWindowSet(
				orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet().getTimeWindowSet());
		AggregateReferenceInformationAlgorithm.setExpectedArrivalsPerSubDeliveryArea(daWeightsLower);
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		AggregateReferenceInformationAlgorithm.setObjectiveSpecificValues(objectiveSpecificValues);
		AggregateReferenceInformationAlgorithm.setMaximumRevenueValue(maximumRevenueValue);
		
		this.orderHorizonLength = orderHorizonLength;
		this.discountingFactorProbability = discountingFactorProbability;
		this.neighbors = neighbors;
		this.arrivalProcessId = arrivalProcessId;

		this.considerLeftOverPenalty = (considerLeftOverPenalty == 1.0);
		this.oppCostOnlyFeasible = (oppCostOnlyFeasible == 1.0);

		if (theftBased == 1.0) {
			this.theftBased = true;
		} else {
			this.theftBased = false;
		}
		this.theftBasedTw = (theftBasedTw == 1.0);

		if (theftBasedAdvanced == 1.0) {
			this.theftBasedAdvanced = true;
		} else {
			this.theftBasedAdvanced = false;
		}

		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}

		if (actualBasketValue == 1.0) {
			this.useActualBasketValue = true;
		} else {
			this.useActualBasketValue = false;
		}

	};

	public void start() {

		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		this.alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		this.initialiseGlobal();

		// Sort order requests to arrive in time
		ArrayList<OrderRequest> relevantRequests = this.orderRequestSet.getElements();
		Collections.sort(relevantRequests, new OrderRequestArrivalTimeDescComparator());

		Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> aggregateInfos = this
				.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
						aggregatedReferenceInformationNo);
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw = aggregateInfos.getKey();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw = aggregateInfos.getValue();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerTwOverSubAreasPerDeliveryArea = ADPWithOrienteeringANNAcceptance
				.determineMaximumAcceptablePerTimeWindowOverSubareas(
						AggregateReferenceInformationAlgorithm.getDeliveryAreaSet(), maxAcceptablePerSubAreaAndTw);

		Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> aggregateInfosNoAreaSpecific = this
				.determineCurrentAverageAndMaximumAcceptablePerTimeWindow();
		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getKey();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerAreaAndTw = aggregateInfosNoAreaSpecific.getValue();

		// HashMap<Integer, Double> maximumDemandCapacityRatio =
		// determineMaximumInitialDemandCapacityRatio(
		// avgAcceptablePerSubAreaAndTw,
		// this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow);

		ArrayList<Order> orders = new ArrayList<Order>();
		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());

		}

		// Determine maximum arrivals for normalisation

		HashMap<DeliveryArea, HashMap<DemandSegment, Double>> maximumArrivals = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();
		HashMap<DeliveryArea, HashMap<DemandSegment, Double>> maximumArrivalsForNeighbors = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			maximumArrivals.put(area, new HashMap<DemandSegment, Double>());

			maximumArrivalsForNeighbors.put(area, new HashMap<DemandSegment, Double>());

			for (DeliveryArea subArea : area.getSubset().getElements()) {

				for (DemandSegment s : this.daSegmentWeightingsLowerHash.get(subArea).keySet()) {
					if (!maximumArrivals.get(area).containsKey(s)) {
						maximumArrivals.get(area).put(s, 0.0);
					}
					double arrivals = this.orderHorizonLength * arrivalProbability * this.daWeightsLower.get(subArea)
							* this.daSegmentWeightingsLowerHash.get(subArea).get(s);
					if (arrivals > maximumArrivals.get(area).get(s)) {
						maximumArrivals.get(area).put(s, arrivals);
					}
					if (!maximumArrivalsForNeighbors.get(area).containsKey(s)) {
						maximumArrivalsForNeighbors.get(area).put(s, 0.0);
					}
					double arrivalsOverNeighbors = 0.0;
					for (DeliveryArea subArea2 : this.neighbors.get(subArea)) {

						arrivalsOverNeighbors += this.daWeightsLower.get(subArea2)
								* this.daSegmentWeightingsLowerHash.get(subArea2).get(s);
						// arrivalsOverNeighbors += this.orderHorizonLength
						// *
						// ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
						// * this.daWeightsLower.get(subArea2) *
						// this.daSegmentWeightingsLowerHash.get(subArea2).get(s);

					}
					if (arrivalsOverNeighbors > maximumArrivalsForNeighbors.get(area).get(s)) {
						maximumArrivalsForNeighbors.get(area).put(s, arrivalsOverNeighbors);
					}
				}
			}

		}

		TimeWindowSet relevantTimeWindowSet;
		if (AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()) {
			relevantTimeWindowSet = AggregateReferenceInformationAlgorithm.getTimeWindowSetOverlappingDummy();
		} else {
			relevantTimeWindowSet = AggregateReferenceInformationAlgorithm.getTimeWindowSet();
		}

		// Go through requests
		double overallTime = 0;
		double maxTime=0;
		for (OrderRequest request : relevantRequests) {
			double currentMilliSecond = System.currentTimeMillis();
			DeliveryArea subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(
					AggregateReferenceInformationAlgorithm.getDeliveryAreaSet(), request.getCustomer());
			request.getCustomer().setTempDeliveryArea(subArea);
			DeliveryArea area = subArea.getDeliveryAreaOfSet();

			// TODO: just for analysis!!!
			// this.neighborWeightsPerDeliveryArea.put(area.getId(), 1.0);

			ArrayList<TimeWindow> timeWindowCandidatesOrienteering = new ArrayList<TimeWindow>();
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting = new ArrayList<TimeWindow>();
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringTheftingTw = new ArrayList<TimeWindow>();
			ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced = new ArrayList<TimeWindow>();
			// Check aggregated orienteering based feasibility (if
			// applicable)

			ADPWithOrienteeringANN.checkFeasibilityBasedOnOrienteeringNo(request, request.getArrivalTime(), subArea,
					timeWindowCandidatesOrienteering, timeWindowCandidatesOrienteeringThefting,
					timeWindowCandidatesOrienteeringTheftingTw,
					potentialTimeWindowCandidatesOrienteeringTheftingAdvanced, avgAcceptablePerSubAreaAndTw,
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, this.theftBased, this.theftBasedAdvanced,
					this.theftBasedTw, this.neighbors, AggregateReferenceInformationAlgorithm.neighborsTw,
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).keySet().size(),
					AggregateReferenceInformationAlgorithm.oldToNewTimeWindowMapping);

			// Determine opportunity costs
			HashMap<TimeWindow, Double> opportunityCostsPerTw = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, DeliveryArea>>();
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>>();
			ADPWithOrienteeringANN.determineOpportunityCostsPerTw(area, subArea, request, request.getArrivalTime(),
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area),
					maxAcceptablePerTwOverSubAreasPerDeliveryArea.get(area.getId()), maximumArrivals.get(area),
					maximumArrivalsForNeighbors.get(area), timeWindowCandidatesOrienteering,
					timeWindowCandidatesOrienteeringThefting, timeWindowCandidatesOrienteeringTheftingTw,
					potentialTimeWindowCandidatesOrienteeringTheftingAdvanced,
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
					advancedStealingAreasPerTimeWindowAndRouting, this.theftBased, this.theftBasedAdvanced,
					this.theftBasedTw, maximumValuePerDeliveryArea, minimumValuePerDeliveryArea, neighbors,
					AggregateReferenceInformationAlgorithm.neighborsTw, this.demandSegmentWeighting, daWeightsLower,
					daSegmentWeightingsLowerHash, relevantTimeWindowSet, arrivalProcessId, ANNPerDeliveryArea,
					this.considerConstant, this.considerNeighborDemand, considerLeftOverPenalty, demandSegments,
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(),
					AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
					minimumExpectedMultiplierPerDemandSegmentAndTimeWindow,
					maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow, opportunityCostsPerTw,
					this.discountingFactorProbability,
					AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).keySet().size(),
					this.arrivalProbability, this.oppCostOnlyFeasible, this.segmentInputMapper,
					this.neighborSegmentInputMapper, this.timeWindowInputMapper,AggregateReferenceInformationAlgorithm.oldToNewTimeWindowMapping);

			for (TimeWindow tw : opportunityCostsPerTw.keySet()) {
				if (opportunityCostsPerTw.get(tw) < 0) {
					opportunityCostsPerTw.put(tw, 0.0);
					// System.out.println("neg. opp. costs");
				}
			}
			for (TimeWindow tw : opportunityCostsPerTw.keySet()) {
				if (opportunityCostsPerTw.get(tw) > 1.0) {

					System.out.println("Strange");
				}
			}

			HashMap<TimeWindow, TimeWindow> finalTwPerUpperTw = new HashMap<TimeWindow, TimeWindow>();
			if (AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()) {
				HashMap<TimeWindow, Double> opportunityCostsPerUpperTw = new HashMap<TimeWindow, Double>();

				ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
						.getConsiderationSet();
				for (ConsiderationSetAlternative alt : alternatives) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						ArrayList<TimeWindow> lowerTws = AggregateReferenceInformationAlgorithm.oldToNewTimeWindowMapping
								.get(alt.getAlternative().getTimeWindows().get(0));
						double lowestOppCost = Double.MAX_VALUE;
						TimeWindow finalTw = null;
						for (TimeWindow tw : lowerTws) {
							if (opportunityCostsPerTw.containsKey(tw)
									&& opportunityCostsPerTw.get(tw) < lowestOppCost) {
								finalTw = tw;
								lowestOppCost = opportunityCostsPerTw.get(tw);
							}
						}
						if (finalTw != null) {
							opportunityCostsPerUpperTw.put(alt.getAlternative().getTimeWindows().get(0), lowestOppCost);
							finalTwPerUpperTw.put(alt.getAlternative().getTimeWindows().get(0), finalTw);
						}
					}
				}
				opportunityCostsPerTw = opportunityCostsPerUpperTw;

			}
			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, opportunityCostsPerTw, AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), algo,
					alternativesToTimeWindows, possiblyLargeOfferSet, this.useActualBasketValue, 1.0);

			ArrayList<AlternativeOffer> selectedOfferedAlternatives = bestOffer.getKey();
			Order order = new Order();
			orders.add(order);
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);
			/// If windows are offered, let customer choose
			if (selectedOfferedAlternatives.size() > 0) {
				// Sample selection from customer

				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							selectedOfferedAlternatives, order, this.alternativeSet.getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(selectedOfferedAlternatives, order);
				}

				if (selectedAlt != null && selectedAlt.getAlternative() != null
						&& !selectedAlt.getAlternative().getNoPurchaseAlternative()) {

					order.setAccepted(true);
					order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
					order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
					TimeWindow relevantAcceptedTw;
					if (AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()) {
						order.setFinalTimeWindowTempId(
								finalTwPerUpperTw.get(selectedAlt.getAlternative().getTimeWindows().get(0)).getId());
						relevantAcceptedTw = finalTwPerUpperTw
								.get(selectedAlt.getAlternative().getTimeWindows().get(0));
					} else {
						relevantAcceptedTw = selectedAlt.getAlternative().getTimeWindows().get(0);
					}
					// Update orienteering information

					OrienteeringAcceptanceHelperService.updateOrienteeringNoInformation(subArea, order,
							relevantAcceptedTw,
							AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area),
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
							advancedStealingAreasPerTimeWindowAndRouting, this.theftBased, this.theftBasedAdvanced,
							null, this.amountTheftsPerDeliveryArea, this.amountTheftsAdvancedPerDeliveryArea,
							this.amountTimeTheftsPerDeliveryArea, this.firstTheftsPerDeliveryArea,
							this.firstTheftsAdvancedPerDeliveryArea, this.firstTimeTheftsPerDeliveryArea);

					int relevantId;
					if (AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()) {
						relevantId = order.getFinalTimeWindowTempId();
					} else {
						relevantId = order.getTimeWindowFinalId();
					}
					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
								new HashMap<Integer, Integer>());
					}
					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(relevantId)) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(relevantId, 0);
					}
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(relevantId,
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(relevantId) + 1);

					aggregateInfos = this.determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
							AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo);
					avgAcceptablePerSubAreaAndTw = aggregateInfos.getKey();
					maxAcceptablePerSubAreaAndTw = aggregateInfos.getValue();

				} else {
					order.setReasonRejection("no_selection");
				}
			} else {
				if (opportunityCostsPerTw.keySet().size() > 0) {

					order.setReasonRejection("no_offer_favour");
				} else {
					order.setReasonRejection("no_offer_feasible");
				}
			}
			
			double requiredTime = System.currentTimeMillis()-currentMilliSecond;
			overallTime+=requiredTime;
			if(requiredTime>30.0) 
				System.out.println("now");
			if(requiredTime>maxTime) maxTime=requiredTime;
		}

		
		try
		{
		    String filename= this.getClass().getName()
		    		+this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSetId()+"_"+this.theftBased+".txt";
		    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
		    fw.write(overallTime/relevantRequests.size()+";"+maxTime+"\n");//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
		this.orderSet = new OrderSet();
		ArrayList<ReferenceRouting> rrs = new ArrayList<ReferenceRouting>();
		DeliveryArea areaUp=null;
		Routing selectedRouting =null;
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.keySet()) {
			areaUp=area;
			for (Routing r : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area)
					.keySet()) {
				selectedRouting=r;
				int leftOver = 0;
				for (DeliveryArea subA : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo
						.get(area).get(r).keySet()) {
					for (TimeWindow tw : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo
							.get(area).get(r).get(subA).keySet()) {
						leftOver += AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area)
								.get(r).get(subA).get(tw);
					}
				}
				ReferenceRouting rr = new ReferenceRouting();
				rr.setDeliveryAreaId(area.getId());
				rr.setRoutingId(r.getId());
				rr.setRemainingCap(leftOver);
				rr.setNumberOfTheftsSpatial(this.amountTheftsPerDeliveryArea.get(area.getId()).get(r));
				rr.setNumberOfTheftsSpatialAdvanced(this.amountTheftsAdvancedPerDeliveryArea.get(area.getId()).get(r));
				rr.setNumberOfTheftsTime(this.amountTimeTheftsPerDeliveryArea.get(area.getId()).get(r));
				rr.setFirstTheftsSpatial(this.firstTheftsPerDeliveryArea.get(area.getId()).get(r));
				rr.setFirstTheftsSpatialAdvanced(this.firstTheftsAdvancedPerDeliveryArea.get(area.getId()).get(r));
				rr.setFirstTheftsTime(this.firstTimeTheftsPerDeliveryArea.get(area.getId()).get(r));
				rrs.add(rr);
			}
		}

		this.orderSet.setReferenceRoutingsPerDeliveryArea(rrs);
		this.orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.orderSet.setElements(orders);
		
//		DeliveryArea selectedArea= null;
//		for(DeliveryArea a: getDeliveryAreaSet().getElements().get(0).getSubset().getElements()) {
//			if(selectedArea==null) selectedArea=a;
//			else if(this.daWeightsLower.get(selectedArea)<this.daWeightsLower.get(a)) selectedArea=a;
//		}
//		HashMap<Integer,Order> relevantOrders = new HashMap<Integer,Order>();
//		int amountRequests=0;
//		for(Order o: orders) {
//			if(o.getOrderRequest().getCustomer().getTempDeliveryArea().getId()==selectedArea.getId()) {
//				amountRequests++;
//				if(o.getAccepted()) relevantOrders.put(o.getOrderRequest().getArrivalTime(),o);
//			}
//		}
//
//		HashMap<TimeWindow, Integer> selectedCap = new HashMap<TimeWindow, Integer>();
//		for(TimeWindow tw: aggregatedReferenceInformationNoCopy.get(areaUp).get(selectedRouting).get(selectedArea).keySet()) {
//			selectedCap.put(tw, aggregatedReferenceInformationNoCopy.get(areaUp).get(selectedRouting).get(selectedArea).get(tw).intValue());
//		};
//		
//		HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> capPerDa = new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>();
//		capPerDa.put(selectedArea, selectedCap);
		
//		HashMap<Integer, Pair<Double, Integer>> result = new  HashMap<Integer, Pair<Double, Integer>>();
//		PrintWriter pw = null;
//		try {
//		    pw = new PrintWriter(new File("vfa_development_"+SettingsProvider.database+"_"+this.orderRequestSet.getId()+"_area"+selectedArea.getId()+".csv"));
//		} catch (FileNotFoundException e) {
//		    e.printStackTrace();
//		}
//		PrintWriter pwOp = null;
//		try {
//		    pwOp = new PrintWriter(new File("opportunityCost_"+SettingsProvider.database+"_"+this.orderRequestSet.getId()+"_area"+selectedArea.getId()+".csv"));
//		} catch (FileNotFoundException e) {
//		    e.printStackTrace();
//		}
		
//		StringBuilder builder = new StringBuilder();
//		StringBuilder builderOp = new StringBuilder();
//		String ColumnNamesList = "t,value,tw";
//		builder.append(ColumnNamesList +"\n");
//		String ColumnNamesListOpportunityCost = "t,tw,oc,cap";
//		builderOp.append(ColumnNamesListOpportunityCost +"\n");
//		for(int t=this.orderHorizonLength; t >=0; t--) {
//			
//			double currentValue = ADPWithOrienteeringANN.determineValueFunctionApproximationValue(areaUp, selectedArea, t,
//					capPerDa, 
//					maxAcceptablePerTwOverSubAreasPerDeliveryArea.get(areaUp.getId()),null,		
//					maximumArrivals.get(areaUp),
//					null, maximumValuePerDeliveryArea.get(areaUp.getId()),
//					minimumValuePerDeliveryArea.get(areaUp.getId()), false, this.demandSegmentWeighting, daWeightsLower, daSegmentWeightingsLowerHash,
//					neighbors, getTimeWindowSet(), arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty, demandSegments,
//					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(),
//					AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(), maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
//					minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, true, considerConstant, considerNeighborDemand,
//					segmentInputMapper, neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);
//			
//			Integer twId = null;
//			if(relevantOrders.containsKey(t)) {
//				
//				//Calculate opportunity cost for all feasible tws
//				for(TimeWindow tw:capPerDa.get(selectedArea).keySet()) {
//					double opp = currentValue - ADPWithOrienteeringANN.determineValueFunctionApproximationValue(areaUp, selectedArea, t,
//							capPerDa, 
//							maxAcceptablePerTwOverSubAreasPerDeliveryArea.get(areaUp.getId()),tw,		
//							maximumArrivals.get(areaUp),
//							null, maximumValuePerDeliveryArea.get(areaUp.getId()),
//							minimumValuePerDeliveryArea.get(areaUp.getId()), false, this.demandSegmentWeighting, daWeightsLower, daSegmentWeightingsLowerHash,
//							neighbors, getTimeWindowSet(), arrivalProcessId, ANNPerDeliveryArea, considerLeftOverPenalty, demandSegments,
//							AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(),
//							AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(), maximumExpectedMultiplierPerDemandSegmentAndTimeWindow,
//							minimumExpectedMultiplierPerDemandSegmentAndTimeWindow, true, considerConstant, considerNeighborDemand,
//							segmentInputMapper, neighborSegmentInputMapper, timeWindowInputMapper, arrivalProbability);
//					builderOp.append(t+",");	
//					builderOp.append(tw.getId()+",");
//					builderOp.append(opp+",");
//					builderOp.append(capPerDa.get(selectedArea).get(tw));
//					builderOp.append('\n');
//				}
//				
//				//Reduce cap for good as in the current sample
//				capPerDa.get(selectedArea).put(relevantOrders.get(t).getTimeWindowFinal(), capPerDa.get(selectedArea).get(relevantOrders.get(t).getTimeWindowFinal())-1);
//				twId=relevantOrders.get(t).getTimeWindowFinalId();
//				
//			}
//			result.put(t, new Pair<Double, Integer>(currentValue, twId));
//			builder.append(t+",");
//			builder.append(currentValue+",");
//			builder.append(twId);
//			builder.append('\n');
//			//System.out.print(t+","+currentValue+","+twId+";");
//		}
//		
//		
//
//		pw.write(builder.toString());
//		pw.close();
//		pwOp.write(builderOp.toString());
//		pwOp.close();

		
	}

	private static HashMap<Integer, HashMap<Integer, Integer>> determineMaximumAcceptablePerTimeWindowOverSubareas(
			DeliveryAreaSet deliveryAreaSet, HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSubAreaAndTw) {

		HashMap<Integer, HashMap<Integer, Integer>> maximumPerTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();

		for (DeliveryArea area : deliveryAreaSet.getElements()) {
			for (DeliveryArea subArea : area.getSubset().getElements()) {
				if (!maxAcceptablePerSubAreaAndTw.containsKey(subArea.getId())) {
					maxAcceptablePerSubAreaAndTw.put(subArea.getId(), new HashMap<Integer, Integer>());
				}
				for (Integer tw : maxAcceptablePerSubAreaAndTw.get(subArea.getId()).keySet()) {
					if (!maximumPerTimeWindow.containsKey(area.getId()))
						maximumPerTimeWindow.put(area.getId(), new HashMap<Integer, Integer>());
					if (!maximumPerTimeWindow.get(area.getId()).containsKey(tw))
						maximumPerTimeWindow.get(area.getId()).put(tw, 0);
					if (maximumPerTimeWindow.get(area.getId()).get(tw) < maxAcceptablePerSubAreaAndTw
							.get(subArea.getId()).get(tw)) {
						maximumPerTimeWindow.get(area.getId()).put(tw,
								maxAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw));
					}
				}
			}
		}

		return maximumPerTimeWindow;
	}

	private Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineAverageAndMaximumAcceptablePerSubareaConsideringOnlyFeasible(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> avgPerRouting) {

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> maxAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> divisorPerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Go through aggr. no per routing
		for (DeliveryArea area : avgPerRouting.keySet()) {
			for (Routing r : avgPerRouting.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = avgPerRouting.get(area).get(r);
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!maxAcceptablePerSAAndTw.containsKey(areaS.getId())) {
							maxAcceptablePerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
						}
						if (!maxAcceptablePerSAAndTw.get(areaS.getId()).containsKey(tw.getId())) {
							maxAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
						}
						if (!avgAcceptablePerSAAndTw.containsKey(areaS.getId())) {
							avgAcceptablePerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
							divisorPerSAAndTw.put(areaS.getId(), new HashMap<Integer, Integer>());
						}
						if (!avgAcceptablePerSAAndTw.get(areaS.getId()).containsKey(tw.getId())) {
							avgAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
							divisorPerSAAndTw.get(areaS.getId()).put(tw.getId(), 0);
						}

						avgAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(),
								avgAcceptablePerSAAndTw.get(areaS.getId()).get(tw.getId())
										+ separateNumbers.get(areaS).get(tw));
						// Only count this combination if capacity is >0
						// (otherwise,
						// not under feasible anymore after acceptance)
						if (separateNumbers.get(areaS).get(tw) > 0)
							divisorPerSAAndTw.get(areaS.getId()).put(tw.getId(),
									divisorPerSAAndTw.get(areaS.getId()).get(tw.getId()) + 1);
						if (maxAcceptablePerSAAndTw.get(areaS.getId()).get(tw.getId()) < separateNumbers.get(areaS)
								.get(tw)) {
							maxAcceptablePerSAAndTw.get(areaS.getId()).put(tw.getId(),
									separateNumbers.get(areaS).get(tw));
						}
					}
				}
			}

		}

		for (Integer areaS : avgAcceptablePerSAAndTw.keySet()) {
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for (Integer tw : avgAcceptablePerSAAndTw.get(areaS).keySet()) {
				if (avgAcceptablePerSAAndTw.get(areaS).get(tw) > 0) {
					avgAcceptablePerSAAndTw.get(areaS).put(tw,
							avgAcceptablePerSAAndTw.get(areaS).get(tw) / divisorPerSAAndTw.get(areaS).get(tw));
				} else {
					toRemove.add(tw);

				}
			}

			for (Integer twId : toRemove) {
				avgAcceptablePerSAAndTw.get(areaS).remove(twId);
			}
		}

		return new Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>>(
				avgAcceptablePerSAAndTw, maxAcceptablePerSAAndTw);
	}

	private void initialiseGlobal() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(
				AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER());
		RoutingService.getDeliveryStartTimeByTimeWindowSet(AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		this.arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId);
		this.valueFunctionValuePerDeliveryAreaAndT = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting,
						AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMinimumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting,
						AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		this.maximumExpectedMultiplierPerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
						this.daSegmentWeightingsLower, AggregateReferenceInformationAlgorithm.getTimeWindowSet());

		this.bestRoutingCandidatePerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();


		this.determineDemandMultiplierPerTimeWindow();

		this.daSegmentWeightingsLowerHash = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();
		for (DeliveryArea subArea : this.daSegmentWeightingsLower.keySet()) {
			this.daSegmentWeightingsLowerHash.put(subArea, new HashMap<DemandSegment, Double>());
			for (DemandSegmentWeight w : this.daSegmentWeightingsLower.get(subArea).getWeights()) {
				this.daSegmentWeightingsLowerHash.get(subArea).put(w.getDemandSegment(), w.getWeight());
			}
			for (DemandSegmentWeight w : this.demandSegmentWeighting.getWeights()) {
				if (!this.daSegmentWeightingsLowerHash.get(subArea).containsKey(w.getDemandSegment())) {
					this.daSegmentWeightingsLowerHash.get(subArea).put(w.getDemandSegment(), 0.0);
				}
			}
		}

		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(this.daWeightsLower,
						this.daSegmentWeightingsLower, AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
				.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daWeightsLower,
						daSegmentWeightingsLower, AggregateReferenceInformationAlgorithm.getTimeWindowSet());

		this.prepareVehicleAssignmentsAndModelsForDeliveryAreas();
		this.prepareValueMultiplier();
		AggregateReferenceInformationAlgorithm.setValueMultiplierPerDeliveryAreaAndTimeWindow(this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow);
		AggregateReferenceInformationAlgorithm.setExpectedArrivals(arrivalProbability*this.orderHorizonLength);
		this.aggregateReferenceInformation(true, true);

		this.determineMaximumLowerAreaWeight();

		this.initialiseAlreadyAcceptedPerTimeWindow();

		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		this.ANNPerDeliveryArea = new HashMap<Integer, NeuralNetwork>();
		this.maximumValuePerDeliveryArea = new HashMap<Integer, Double>();
		this.minimumValuePerDeliveryArea = new HashMap<Integer, Double>();
		this.neighborWeightsPerDeliveryArea = new HashMap<Integer, Double>();
		for (Integer area : this.valueFunctionApproximationPerDeliveryArea.keySet()) {
			ArtificialNeuralNetwork ann = null;
			try {
				ann = mapper.readValue(valueFunctionApproximationPerDeliveryArea.get(area).getComplexModelJSON(),
						ArtificialNeuralNetwork.class);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int numberInput = ann.getInputElementIds().length;
			if (ann.isConsiderConstant()) {
				numberInput++;
				considerConstant = true;
			}
			if (ann.isConsiderDemandNeighbors()) {
				considerNeighborDemand = true;
			}
			NeuralNetwork annToUse = new NeuralNetwork(numberInput, ann.getNumberOfHidden(), 1, 0.0, 0.0,
					ann.isUseHyperbolicTangens());
			annToUse.setMatrix(ann.getWeights());
			annToUse.setThresholds(ann.getThresholds());

			this.segmentInputMapper = new HashMap<Integer, Integer>();

			for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {

				for (int i = 0; i < this.demandSegmentWeighting.getSetEntity().getElements().size(); i++) {
					if (ds.getId() == ann.getInputElementIds()[i]) {
						segmentInputMapper.put(ds.getId(), i);
						break;
					}

				}

			}
			int currentId = this.demandSegmentWeighting.getSetEntity().getElements().size();
			if (this.considerNeighborDemand) {
				this.neighborSegmentInputMapper = new HashMap<Integer, Integer>();
				for (Entity ds : this.demandSegmentWeighting.getSetEntity().getElements()) {
					for (int i = this.demandSegmentWeighting.getSetEntity().getElements()
							.size(); i < this.demandSegmentWeighting.getSetEntity().getElements().size() * 2; i++) {
						if (ds.getId() == ann.getInputElementIds()[i]) {
							neighborSegmentInputMapper.put(ds.getId(), i);
							break;
						}

					}
				}
				currentId = this.demandSegmentWeighting.getSetEntity().getElements().size() * 2;
			}

			this.timeWindowInputMapper = new HashMap<Integer, Integer>();
			if(AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()){
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSetOverlappingDummy().getElements()) {

					for (int i = currentId; i < ann.getInputElementIds().length; i++) {
						if (tw.getId() == ann.getInputElementIds()[i]) {
							timeWindowInputMapper.put(tw.getId(), i);
							break;
						}

					}

				}
			}else{
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {

					for (int i = currentId; i < ann.getInputElementIds().length; i++) {
						if (tw.getId() == ann.getInputElementIds()[i]) {
							timeWindowInputMapper.put(tw.getId(), i);
							break;
						}

					}

				}
			}
			

			this.ANNPerDeliveryArea.put(area, annToUse);
			this.neighborWeightsPerDeliveryArea.put(area, ann.getTheftWeight());
			this.maximumValuePerDeliveryArea.put(area, ann.getMaximumValue());
			this.minimumValuePerDeliveryArea.put(area, ann.getMinimumValue());

		}

		this.maximumOpportunityCostsPerLowerDaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.demandSegments = (ArrayList<DemandSegment>) this.demandSegmentWeighting.getSetEntity().getElements();
		Collections.sort(this.demandSegments, new DemandSegmentsExpectedValueDescComparator(maximumLowerAreaWeight,
				AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues()));
		for (DeliveryArea a : this.daWeightsLower.keySet()) {
			maximumOpportunityCostsPerLowerDaAndTimeWindow.put(a.getId(), new HashMap<Integer, Double>());
			for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
				boolean highestFound = false;
				for (DemandSegment ds : this.demandSegments) {

					if (this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(ds.getId())
							.containsKey(tw.getId())
							&& this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(ds.getId())
									.get(tw.getId()) > 0) {
						for (DemandSegmentWeight dsw : this.daSegmentWeightingsLower.get(a).getWeights()) {
							if (dsw.getElementId() == ds.getId() && dsw.getWeight() > 0) {
								maximumOpportunityCostsPerLowerDaAndTimeWindow.get(a.getId()).put(tw.getId(),
										CustomerDemandService.calculateExpectedValue(
												AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
												AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), ds)
												* this.discountingFactorProbability);
								highestFound = true;
								break;
							}
						}
					}
					if (highestFound)
						break;
				}
			}
		}

		this.amountTheftsAdvancedPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		this.amountTheftsPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		this.amountTimeTheftsPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		this.firstTheftsPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		this.firstTheftsAdvancedPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		this.firstTimeTheftsPerDeliveryArea = new HashMap<Integer, HashMap<Routing, Integer>>();
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			this.amountTheftsPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			this.amountTheftsAdvancedPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			this.amountTimeTheftsPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			this.firstTheftsPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			this.firstTheftsAdvancedPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			this.firstTimeTheftsPerDeliveryArea.put(area.getId(), new HashMap<Routing, Integer>());
			for (Routing r : AggregateReferenceInformationAlgorithm.getAggregatedReferenceInformationNo().get(area)
					.keySet()) {
				this.amountTheftsPerDeliveryArea.get(area.getId()).put(r, 0);
				this.amountTheftsAdvancedPerDeliveryArea.get(area.getId()).put(r, 0);
				this.amountTimeTheftsPerDeliveryArea.get(area.getId()).put(r, 0);

			}
		}

	}

	private void determineMaximumLowerAreaWeight() {

		double maximumAreaWeight = 0.0;
		for (DeliveryArea area : this.daWeightsLower.keySet()) {
			if (maximumAreaWeight < this.daWeightsLower.get(area)) {
				maximumAreaWeight = this.daWeightsLower.get(area);
			}
		}

		this.maximumLowerAreaWeight = maximumAreaWeight;
	}

	private void prepareVehicleAssignmentsAndModelsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaSetAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();

		this.valueFunctionApproximationPerDeliveryArea = new HashMap<Integer, ValueFunctionApproximationModel>();

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());

		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);

		}

		for (ValueFunctionApproximationModel model : this.modelSet.getElements()) {
			this.valueFunctionApproximationPerDeliveryArea.put(model.getDeliveryAreaId(), model);
		}

	}

	private void determineDemandMultiplierPerTimeWindow() {

		this.demandMultiplierPerTimeWindow = new HashMap<Integer, HashMap<TimeWindow, Double>>();

		for (DeliveryArea area : this.daWeightsUpper.keySet()) {

			HashMap<TimeWindow, Double> timeWindowMultiplier = new HashMap<TimeWindow, Double>();
			for (DemandSegmentWeight segW : this.daSegmentWeightingsUpper.get(area).getWeights()) {

				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (ConsiderationSetAlternative alt : alts) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alt.getAlternative());
					offer.setAlternativeId(alt.getAlternativeId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, segW.getDemandSegment());

				for (AlternativeOffer alt : probs.keySet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						double m = this.daWeightsUpper.get(area) * segW.getWeight() * probs.get(alt);
						if (timeWindowMultiplier.containsKey(alt.getAlternative().getTimeWindows().get(0))) {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0),
									m + timeWindowMultiplier.get(alt.getAlternative().getTimeWindows().get(0)));
						} else {
							timeWindowMultiplier.put(alt.getAlternative().getTimeWindows().get(0), m);
						}

					}
				}

			}
			this.demandMultiplierPerTimeWindow.put(area.getId(), timeWindowMultiplier);
		}

	}

	/**
	 * Choose orienteering results that serve as reference routings
	 */
	// private void chooseReferenceRoutings() {
	//
	// // TA: other selection procedure? (for instance, save chosen ones!)
	//
	// this.referenceRoutingsPerDeliveryArea = new HashMap<DeliveryArea,
	// HashMap<Routing, HashMap<Integer, Route>>>();
	// this.referenceRoutingsList = new ArrayList<Routing>();
	//
	// Random r = new Random();
	// for (int i = 0; i < this.numberRoutingCandidates; i++) {
	// int selection =
	// r.nextInt(AggregateReferenceInformationAlgorithm.getRo.size());
	// while
	// (this.referenceRoutingsList.contains(this.targetRoutingResults.get(selection)))
	// {
	// selection = r.nextInt(this.targetRoutingResults.size());
	// }
	// this.referenceRoutingsList.add(this.targetRoutingResults.get(selection));
	// for (Route ro : this.targetRoutingResults.get(selection).getRoutes()) {
	// if
	// (!this.referenceRoutingsPerDeliveryArea.containsKey(ro.getVehicleAssignment().getDeliveryArea()))
	// {
	// this.referenceRoutingsPerDeliveryArea.put(ro.getVehicleAssignment().getDeliveryArea(),
	// new HashMap<Routing, HashMap<Integer, Route>>());
	// }
	// if
	// (!this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
	// .containsKey(this.targetRoutingResults.get(selection))) {
	// this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
	// .put(this.targetRoutingResults.get(selection), new HashMap<Integer,
	// Route>());
	// }
	// RouteElement reDepot1 = new RouteElement();
	// reDepot1.setTravelTime(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
	// ro.getVehicleAssignment().getStartingLocationLat(),
	// ro.getVehicleAssignment().getStartingLocationLon(),
	// ro.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getLat(),
	// ro.getRouteElements().get(0).getOrder().getOrderRequest().getCustomer().getLon()));
	// reDepot1.setTempAlreadyAccepted(true);
	// Order order = new Order();
	// OrderRequest ore = new OrderRequest();
	// Customer customer = new Customer();
	// customer.setLat(ro.getVehicleAssignment().getStartingLocationLat());
	// customer.setLon(ro.getVehicleAssignment().getStartingLocationLon());
	// ore.setCustomer(customer);
	// order.setOrderRequest(ore);
	// reDepot1.setOrder(order);
	// reDepot1.setRouteId(ro.getId());
	// ro.getRouteElements().add(0, reDepot1);
	//
	// RouteElement reDepot2 = new RouteElement();
	// reDepot2.setTempAlreadyAccepted(true);
	// Order order2 = new Order();
	// OrderRequest ore2 = new OrderRequest();
	// Customer customer2 = new Customer();
	// customer2.setLat(ro.getVehicleAssignment().getEndingLocationLat());
	// customer2.setLon(ro.getVehicleAssignment().getEndingLocationLon());
	// ore2.setCustomer(customer2);
	// order2.setOrderRequest(ore2);
	// reDepot2.setOrder(order2);
	// reDepot2.setRouteId(ro.getId());
	// reDepot2.setTravelTime(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
	// ro.getVehicleAssignment().getEndingLocationLat(),
	// ro.getVehicleAssignment().getEndingLocationLon(),
	// ro.getRouteElements().get(ro.getRouteElements().size() -
	// 1).getOrder().getOrderRequest()
	// .getCustomer().getLat(),
	// ro.getRouteElements().get(ro.getRouteElements().size() -
	// 1).getOrder().getOrderRequest()
	// .getCustomer().getLon()));
	// ro.getRouteElements().add(ro.getRouteElements().size(), reDepot2);
	// this.referenceRoutingsPerDeliveryArea.get(ro.getVehicleAssignment().getDeliveryArea())
	// .get(this.targetRoutingResults.get(selection))
	// .put(ro.getVehicleAssignment().getVehicleNo(), ro);
	// }
	//
	// }
	// }

	private Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>> determineCurrentAverageAndMaximumAcceptablePerTimeWindow() {
		HashMap<Integer, HashMap<Integer, Integer>> average = new HashMap<Integer, HashMap<Integer, Integer>>();
		// TA: Think about implementing maximal acceptable per time window
		// (separately)
		HashMap<Integer, HashMap<Integer, Integer>> maximal = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.keySet()) {
			HashMap<Integer, Integer> acceptable = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();

			// Go through aggr. no per routing
			for (Routing r : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area)
					.keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo
						.get(area).get(r);
				HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!acceptable.containsKey(tw.getId())) {
							acceptable.put(tw.getId(), separateNumbers.get(areaS).get(tw));
							maximumAcceptable.put(tw.getId(), 0);
						} else {
							acceptable.put(tw.getId(), acceptable.get(tw.getId()) + separateNumbers.get(areaS).get(tw));
						}

						if (!acceptedPerTw.containsKey(tw.getId())) {
							acceptedPerTw.put(tw.getId(), separateNumbers.get(areaS).get(tw));
						} else {
							acceptedPerTw.put(tw.getId(),
									acceptedPerTw.get(tw.getId()) + separateNumbers.get(areaS).get(tw));
						}
					}
				}

				// Update maximum acceptable per tw (find orienteering result
				// with highest number of acceptances per time window for this
				// higher area)
				for (Integer tw : acceptedPerTw.keySet()) {
					if (acceptedPerTw.get(tw) > maximumAcceptable.get(tw)) {
						maximumAcceptable.put(tw, acceptedPerTw.get(tw));
					}
				}
			}
			;

			// For that delivery area: Determine average acceptable per time
			// window

			for (Integer tw : acceptable.keySet()) {
				acceptable.put(tw, acceptable.get(tw)
						/ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area).size());
			}

			average.put(area.getId(), acceptable);
			maximal.put(area.getId(), maximumAcceptable);

		}

		return new Pair<HashMap<Integer, HashMap<Integer, Integer>>, HashMap<Integer, HashMap<Integer, Integer>>>(
				average, maximal);
	}

	// /**
	// * Determine average distances or number of accepted customers for the
	// * orienteering results
	// */
	// private void aggregateReferenceInformation() {
	//
	// // TA: distance - really just travel time to?
	//
	// this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea,
	// HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();
	// this.aggregatedReferenceInformationCosts = new HashMap<DeliveryArea,
	// ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>>();
	// this.acceptablePerDeliveryAreaAndRouting = new HashMap<DeliveryArea,
	// HashMap<Routing, Integer>>();
	//
	// // TA: consider that maximal acceptable costs is initialised too low
	// this.maximalAcceptableCostsPerDeliveryArea = new HashMap<Integer,
	// Double>();
	// for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
	//
	// this.aggregatedReferenceInformationNo.put(area,
	// new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow,
	// Integer>>>());
	// this.aggregatedReferenceInformationCosts.put(area,
	// new ArrayList<HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>());
	// this.maximalAcceptableCostsPerDeliveryArea.put(area.getId(), 0.0);
	// }
	//
	// for (Routing routing : this.targetRoutingResults) {
	// HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow,
	// Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea,
	// HashMap<TimeWindow, Integer>>>();
	// HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>
	// distance = new HashMap<DeliveryArea, HashMap<DeliveryArea,
	// HashMap<TimeWindow, Double>>>();
	//
	// for (Route r : routing.getRoutes()) {
	//
	// DeliveryArea area =
	// LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
	// r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
	// if (!count.containsKey(area)) {
	// count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow,
	// Integer>>());
	// distance.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow,
	// Double>>());
	// }
	// for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
	// RouteElement re = r.getRouteElements().get(reId);
	// double travelTimeFrom;
	// if (reId < r.getRouteElements().size() - 1) {
	// travelTimeFrom = r.getRouteElements().get(reId + 1).getTravelTime();
	// } else {
	// travelTimeFrom =
	// LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
	// re.getOrder().getOrderRequest().getCustomer().getLat(),
	// re.getOrder().getOrderRequest().getCustomer().getLon(),
	// r.getVehicleAssignment().getEndingLocationLat(),
	// r.getVehicleAssignment().getEndingLocationLon());
	// }
	//
	// Customer cus = re.getOrder().getOrderRequest().getCustomer();
	// DeliveryArea subArea = LocationService
	// .assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
	// if (!count.get(area).containsKey(subArea)) {
	// count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());
	// distance.get(area).put(subArea, new HashMap<TimeWindow, Double>());
	// }
	// if
	// (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal()))
	// {
	// count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);
	// distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
	// (re.getTravelTime() + travelTimeFrom) / 2.0);
	// } else {
	// count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
	// count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) +
	// 1);
	// distance.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
	// distance.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal())
	// + (re.getTravelTime() + travelTimeFrom) / 2.0);
	// }
	// }
	// }
	//
	// for (DeliveryArea a : this.aggregatedReferenceInformationNo.keySet()) {
	// if (!this.acceptablePerDeliveryAreaAndRouting.containsKey(a))
	// this.acceptablePerDeliveryAreaAndRouting.put(a, new HashMap<Routing,
	// Integer>());
	// this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing, 0);
	// this.aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));
	// double distanceSum = 0.0;
	// for (DeliveryArea area : distance.get(a).keySet()) {
	// for (TimeWindow tw : distance.get(a).get(area).keySet()) {
	// this.acceptablePerDeliveryAreaAndRouting.get(a).put(routing,
	// this.acceptablePerDeliveryAreaAndRouting.get(a).get(routing)+
	// count.get(a).get(area).get(tw));
	// distance.get(a).get(area).put(tw,
	// distance.get(a).get(area).get(tw) / count.get(a).get(area).get(tw));
	// distanceSum += distance.get(a).get(area).get(tw);
	// }
	// }
	// this.aggregatedReferenceInformationCosts.get(a).add(distance.get(a));
	// if (this.maximalAcceptableCostsPerDeliveryArea.get(a.getId()) <
	// distanceSum) {
	// this.maximalAcceptableCostsPerDeliveryArea.put(a.getId(), distanceSum);
	// }
	// }
	//
	// }
	// }

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			if(AggregateReferenceInformationAlgorithm.getTimeWindowSet().getOverlapping()){
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSetOverlappingDummy().getElements()) {
					acceptedPerTw.put(tw.getId(), 0);
				}
			}else{
				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
					acceptedPerTw.put(tw.getId(), 0);
				}
			}
			
			this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
		}
	}

	private void prepareValueMultiplier() {
		this.valueMultiplierPerLowerDeliveryArea = new HashMap<Integer, Double>();
		this.valueMultiplierPerLowerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
		this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment = new HashMap<Integer, HashMap<DemandSegment, HashMap<Alternative, Double>>>();

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {

			this.maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.put(area.getId(),
					new HashMap<Integer, Double>());

		}

		for (DeliveryArea area : this.daSegmentWeightingsLower.keySet()) {
			this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.put(area.getId(),
					new HashMap<DemandSegment, HashMap<Alternative, Double>>());
			double weightedValue = 0.0;
			for (DemandSegmentWeight segW : this.daSegmentWeightingsLower.get(area).getWeights()) {
				this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
						.put(segW.getDemandSegment(), new HashMap<Alternative, Double>());
				double expectedValue = CustomerDemandService.calculateExpectedValue(
						AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
						AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), segW.getDemandSegment());
				weightedValue += expectedValue * segW.getWeight();

				ArrayList<ConsiderationSetAlternative> alts = segW.getDemandSegment().getConsiderationSet();
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				AlternativeOffer noPurchaseOffer = null;

				for (ConsiderationSetAlternative alt : alts) {
					if (alt.getAlternative().getNoPurchaseAlternative()) {
						noPurchaseOffer = new AlternativeOffer();
						noPurchaseOffer.setAlternative(alt.getAlternative());
						noPurchaseOffer.setAlternativeId(alt.getAlternativeId());
						this.noPurchaseAlternative = alt.getAlternative();
					}
				}

				for (ConsiderationSetAlternative alt : alts) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alt.getAlternative());
					offer.setAlternativeId(alt.getAlternativeId());
					offeredAlternatives.add(offer);

					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						ArrayList<AlternativeOffer> maxProbOffers = new ArrayList<AlternativeOffer>();
						maxProbOffers.add(offer);
						if (noPurchaseOffer != null) {
							maxProbOffers.add(noPurchaseOffer);
						}
						HashMap<AlternativeOffer, Double> probs = CustomerDemandService
								.getProbabilitiesForModel(maxProbOffers, segW.getDemandSegment());
						this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
								.get(segW.getDemandSegment()).put(alt.getAlternative(), probs.get(offer));
					}
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, segW.getDemandSegment());

				for (AlternativeOffer alt : probs.keySet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative() && alt != null) {
						double m = this.daWeightsLower.get(area) * segW.getWeight() * probs.get(alt) * expectedValue;

						if (!valueMultiplierPerLowerDeliveryAreaAndTimeWindow.containsKey(area.getId())) {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.put(area.getId(),
									new HashMap<Integer, Double>());
						}

						if (valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
								.containsKey(alt.getAlternative().getTimeWindows().get(0).getId())) {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).put(
									alt.getAlternative().getTimeWindows().get(0).getId(),
									valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
											.get(alt.getAlternative().getTimeWindows().get(0).getId()) + m);

						} else {
							valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId())
									.put(alt.getAlternative().getTimeWindows().get(0).getId(), m);
						}

					} else {
						Alternative noPurch = null;
						if (alt != null) {
							noPurch = alt.getAlternative();
						}
						this.noPurchaseAlternative = noPurch;
						this.alternativeProbabilitiesPerDeliveryAreaAndDemandSegment.get(area.getId())
								.get(segW.getDemandSegment()).put(noPurch, probs.get(alt));
					}
				}
			}

			this.valueMultiplierPerLowerDeliveryArea.put(area.getId(), weightedValue);

			for (Integer twId : valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).keySet()) {
				if (!maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
						.containsKey(twId)) {
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
							.put(twId, 0.0);
				}
				if (maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
						.get(twId) < valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId)) {
					maximumValueMultiplierPerUpperDeliveryAreaAndTimeWindow.get(area.getDeliveryAreaOfSet().getId())
							.put(twId, valueMultiplierPerLowerDeliveryAreaAndTimeWindow.get(area.getId()).get(twId));
				}
				;
			}

		}
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return this.orderSet;
	}

}
