package logic.algorithm.vr.routingBasedAcceptance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
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
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.OrienteeringAcceptanceHelperService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

/**
 * Info: Works with actual locations, not nodes
 * 
 * @author M. Lang
 *
 */
public class OrienteeringBasedAcceptance implements AcceptanceAlgorithm {
	private static int numberOfThreads=1;
	private static double TIME_MULTIPLIER = 60.0;
	private static double stealingScarcityMultiplier = 0.95;
	private TimeWindowSet timeWindowSet;
	private Region region;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private OrderSet orderSet;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;

	private int includeDriveFromStartingPosition;

	private ArrayList<Routing> targetRoutingResults;

	private Double expectedServiceTime;
	private int numberRoutingCandidates;
	private int numberInsertionCandidates;

	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;

	private boolean usepreferencesSampled;
	private boolean dynamicFeasibilityCheck;
	private boolean theftBased;
	private boolean theftBasedAdvanced;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerDeliveryAreaAndTw;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, Double> daLowerWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private int arrivalProcessId;
	private int stealingCounter = 0;

	// TODO: Add orienteering booleans to VFA?
	private static String[] paras = new String[] { "Constant_service_time", "samplePreferences",
			"includeDriveFromStartingPosition", "no_routing_candidates", "no_insertion_candidates",
			"dynamic_feasibility_check", "theft-based", "theft-based-advanced" };

	public OrienteeringBasedAcceptance(Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			ArrayList<Routing> targetRoutingResults, OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet,
			Double expectedServiceTime, Double includeDriveFromStartingPosition, Double samplePreferences,
			Double numberRoutingCandidates, Double numberInsertionCandidates, Double dynamicFeasibilityCheck,
			Double theftBased, Double theftBasedAdvanced, HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings, int arrivalProcessId) {

		this.region = region;
		this.neighbors = neighbors;
		this.orderRequestSet = orderRequestSet;
		this.targetRoutingResults = targetRoutingResults;
		this.deliveryAreaSet = deliveryAreaSet;
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.expectedServiceTime = expectedServiceTime;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberRoutingCandidates = numberRoutingCandidates.intValue();
		this.numberInsertionCandidates = numberInsertionCandidates.intValue();
		this.daLowerWeights = daLowerWeights;
		this.daLowerSegmentWeightings = daLowerSegmentWeightings;
		this.arrivalProcessId = arrivalProcessId;
		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}

		if (dynamicFeasibilityCheck == 1.0) {
			this.dynamicFeasibilityCheck = true;
		} else {
			this.dynamicFeasibilityCheck = false;
		}

		if (theftBased == 1.0) {
			this.theftBased = true;
		} else {
			this.theftBased = false;
		}

		if (theftBasedAdvanced == 1.0) {
			this.theftBasedAdvanced = true;
		} else {
			this.theftBasedAdvanced = false;
		}
	};

	public void start() {

		this.initialiseGlobal();

		// Sort order requests to arrive in time
		ArrayList<OrderRequest> relevantRequests = this.orderRequestSet.getElements();
		Collections.sort(relevantRequests, new OrderRequestArrivalTimeDescComparator());

		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		HashMap<Integer, Alternative> alternativesToTimeWindows = new HashMap<Integer, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
			}
		}

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw = this
				.determineAverageAcceptablePerSubareaConsideringOnlyFeasible(this.aggregatedReferenceInformationNo);

		ArrayList<Order> orders = new ArrayList<Order>();

		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
		}

		// Go through requests and update value function
		for (OrderRequest request : relevantRequests) {

			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet,
					request.getCustomer());
			request.getCustomer().setTempDeliveryArea(rArea);
			DeliveryArea area = rArea.getDeliveryAreaOfSet();

			ArrayList<Order> relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
			HashMap<Integer, VehicleAreaAssignment> relevantVaas = vasPerDeliveryAreaAndVehicleNo.get(area.getId());
			HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area);
			Double currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(area);

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();

			// Possible time windows for request
			ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
			ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet();
			for (ConsiderationSetAlternative alt : alternatives) {
				if (!alt.getAlternative().getNoPurchaseAlternative())
					timeWindows.addAll(alt.getAlternative().getTimeWindows());
			}

			ArrayList<TimeWindow> timeWindowCandidatesOrienteering = new ArrayList<TimeWindow>();
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting = new ArrayList<TimeWindow>();
			ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced = new ArrayList<TimeWindow>();
			OrienteeringBasedAcceptance.checkFeasibilityBasedOnOrienteeringNo(request, request.getArrivalTime(), rArea,
					timeWindowCandidatesOrienteering, timeWindowCandidatesOrienteeringThefting,
					potentialTimeWindowCandidatesOrienteeringTheftingAdvanced, avgAcceptablePerSubAreaAndTw,
					this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, this.theftBased, this.theftBasedAdvanced,
					neighbors, this.aggregatedReferenceInformationNo.get(area).keySet().size(), arrivalProcessId,
					this.daLowerWeights,
					maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow,
					minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow);

			// Check aggregated orienteering based feasibility
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, DeliveryArea>>();
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>>();

			Set<TimeWindow> timeWindowsToOffer = new HashSet<TimeWindow>();
			OrienteeringBasedAcceptance.determineFeasibleAndStealingAreasPerTOP(area, rArea, request,
					request.getArrivalTime(), this.aggregatedReferenceInformationNo.get(area),
					timeWindowCandidatesOrienteering, timeWindowCandidatesOrienteeringThefting,
					potentialTimeWindowCandidatesOrienteeringTheftingAdvanced, this.alreadyAcceptedPerDeliveryArea,
					stealingAreaPerTimeWindowAndRouting, advancedStealingAreasPerTimeWindowAndRouting, this.theftBased,
					this.theftBasedAdvanced, neighbors, arrivalProcessId, timeWindowsToOffer,
					this.aggregatedReferenceInformationNo.get(area).keySet().size(),
					maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow, this.daLowerWeights);


			// Check feasible time windows and lowest insertion costs based
			// on dynamic routing
			
			if (this.dynamicFeasibilityCheck) {
				ArrayList<TimeWindow> timeWindowsToOfferList = new ArrayList<TimeWindow>();
				timeWindowsToOfferList.addAll(timeWindowsToOffer);
				currentAcceptedTravelTime = DynamicRoutingHelperService
						.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
								request, region, TIME_MULTIPLIER, this.timeWindowSet,
								(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime,
								possibleRoutings, this.numberRoutingCandidates, this.numberInsertionCandidates,
								relevantVaas, relevantOrders, bestRoutingSoFar, currentAcceptedTravelTime,
								timeWindowsToOfferList, bestRoutingsValueAfterInsertion, numberOfThreads);
			} else {
				for (TimeWindow tw : timeWindowsToOffer) {
					// Insert dummy route elements with orienteering costs
					// and order
					RouteElement e = new RouteElement();
					Order order = new Order();
					order.setOrderRequestId(request.getId());
					order.setOrderRequest(request);
					order.setTimeWindowFinalId(tw.getId());
					e.setOrder(order);
					e.setTempAdditionalCostsValue(0.0);
					bestRoutingsValueAfterInsertion.put(tw.getId(), new Pair<RouteElement, Double>(e, 0.0));
				}
			}

			// Simulate customer decision
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {

				CustomerDemandService.simulateCustomerDecision(order, bestRoutingsValueAfterInsertion.keySet(),
						orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(),
						alternativesToTimeWindows, this.usepreferencesSampled);

				if (order.getAccepted()) {
					int twId = order.getTimeWindowFinalId();
					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);

					order.setTimeWindowFinalId(twId);
					order.setAccepted(true);

					// Choose best routing after insertion for the respective tw
					if (this.dynamicFeasibilityCheck) {
						RouteElement elementToInsert = bestRoutingsValueAfterInsertion.get(twId).getKey();

						int routingId = bestRoutingsValueAfterInsertion.get(twId).getKey().getTempRoutingId();
						DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
								vasPerDeliveryAreaAndVehicleNo.get(area.getId()), timeWindowSet, TIME_MULTIPLIER,
								(includeDriveFromStartingPosition == 1));
						bestRoutingSoFar = possibleRoutings.get(routingId);

						currentAcceptedTravelTime = bestRoutingsValueAfterInsertion.get(twId).getValue();

					}

					// Update orienteering information
					OrienteeringAcceptanceHelperService.updateOrienteeringNoInformation(rArea, order,order.getTimeWindowFinal(),
							this.aggregatedReferenceInformationNo.get(area),
							alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
							advancedStealingAreasPerTimeWindowAndRouting, this.theftBased, this.theftBasedAdvanced, null,
							null, null, null, null, null, null);

					if (this.theftBased) {
						if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(rArea.getId())) {
							this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(rArea.getId(),
									new HashMap<Integer, Integer>());
						}
						if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).containsKey(twId)) {
							this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).put(twId, 0);
						}
						this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).put(twId,
								this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(rArea.getId()).get(twId) + 1);
					}

					avgAcceptablePerSubAreaAndTw = this.determineAverageAcceptablePerSubareaConsideringOnlyFeasible(
							this.aggregatedReferenceInformationNo);

				} else {

					order.setReasonRejection("Customer chose no-purchase option");
				}

			} else {

				order.setReasonRejection("No feasible, considered time windows");

			}

			if (this.dynamicFeasibilityCheck) {
				ordersPerDeliveryArea.get(area).add(order);
				// Update best and accepted travel time because it could change
				// also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area, bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(area, currentAcceptedTravelTime);
				if (order.getAccepted()) {

					acceptedOrdersPerDeliveryArea.get(area).add(order);

				}
			}

			orders.add(order);

		}

		this.orderSet = new OrderSet();
		System.out.println("number of stealings: " + this.stealingCounter);

		ArrayList<ReferenceRouting> rrs = new ArrayList<ReferenceRouting>();
		for (DeliveryArea area : this.aggregatedReferenceInformationNo.keySet()) {

			for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {

				int leftOver = 0;
				for (DeliveryArea subA : this.aggregatedReferenceInformationNo.get(area).get(r).keySet()) {
					for (TimeWindow tw : this.aggregatedReferenceInformationNo.get(area).get(r).get(subA).keySet()) {
						leftOver += this.aggregatedReferenceInformationNo.get(area).get(r).get(subA).get(tw);
						this.aggregatedReferenceInformationNo.get(area).get(r).get(subA).get(tw);
					}
				}
				ReferenceRouting rr = new ReferenceRouting();
				rr.setDeliveryAreaId(area.getId());
				rr.setRoutingId(r.getId());
				rr.setRemainingCap(leftOver);
				rrs.add(rr);
			}
		}

		this.orderSet.setReferenceRoutingsPerDeliveryArea(rrs);
		this.orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.orderSet.setElements(orders);

	}

	private HashMap<Integer, HashMap<Integer, Integer>> determineAverageAcceptablePerSubareaConsideringOnlyFeasible(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> avgPerRouting) {

		HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> divisorPerSAAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();

		// Go through aggr. no per routing
		for (DeliveryArea area : avgPerRouting.keySet()) {
			for (Routing r : avgPerRouting.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = avgPerRouting.get(area).get(r);
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
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
					}
				}
			}

		}

		for (Integer areaS : avgAcceptablePerSAAndTw.keySet()) {
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for (Integer tw : avgAcceptablePerSAAndTw.get(areaS).keySet()) {
				if (avgAcceptablePerSAAndTw.get(areaS).get(tw) > 0) {
					avgAcceptablePerSAAndTw.get(areaS).put(tw,
							(int) ((double) avgAcceptablePerSAAndTw.get(areaS).get(tw)
									/ (double) divisorPerSAAndTw.get(areaS).get(tw)));
					if (avgAcceptablePerSAAndTw.get(areaS).get(tw) < 1) {
						System.out.println("Strange");
					}
				} else {
					toRemove.add(tw);

				}
			}

			for (Integer twId : toRemove) {
				avgAcceptablePerSAAndTw.get(areaS).remove(twId);
			}
		}

		return avgAcceptablePerSAAndTw;
	}

	private void initialiseGlobal() {

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		this.aggregateReferenceInformation();
		this.determineAverageAndMaximumAcceptablePerTimeWindow();
		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		if (this.theftBased) {
			this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
		}
		this.prepareVehicleAssignmentsAndModelsForDeliveryAreas();
		this.initialiseAlreadyAcceptedPerTimeWindow();

	}

	private void prepareVehicleAssignmentsAndModelsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());
			this.overallCapacityPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);
			double capacity = this.overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
			capacity += (ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER + (this.expectedServiceTime - 1);
			// TODO: Check if depot travel time should be included
			this.overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);

		}

	}

	/**
	 * Prepare demand/ capacity ratio
	 */
	private void determineAverageAndMaximumAcceptablePerTimeWindow() {
		this.averageReferenceInformationNoPerDeliveryArea = new HashMap<Integer, HashMap<TimeWindow, Integer>>();
		// TA: Think about implementing maximal acceptable per time window
		// (separately)
		this.maximalAcceptablePerDeliveryAreaAndTw = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<TimeWindow, Integer> acceptable = new HashMap<TimeWindow, Integer>();
			HashMap<Integer, Integer> maximumAcceptable = new HashMap<Integer, Integer>();

			// Go through aggr. no per routing
			for (Routing r : this.aggregatedReferenceInformationNo.get(area).keySet()) {
				HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> separateNumbers = this.aggregatedReferenceInformationNo
						.get(area).get(r);
				HashMap<TimeWindow, Integer> acceptedPerTw = new HashMap<TimeWindow, Integer>();
				for (DeliveryArea areaS : separateNumbers.keySet()) {
					for (TimeWindow tw : separateNumbers.get(areaS).keySet()) {
						if (!acceptable.containsKey(tw)) {
							acceptable.put(tw, separateNumbers.get(areaS).get(tw));
							maximumAcceptable.put(tw.getId(), 0);
						} else {
							acceptable.put(tw, acceptable.get(tw) + separateNumbers.get(areaS).get(tw));
						}

						if (!acceptedPerTw.containsKey(tw)) {
							acceptedPerTw.put(tw, separateNumbers.get(areaS).get(tw));
						} else {
							acceptedPerTw.put(tw, acceptedPerTw.get(tw) + separateNumbers.get(areaS).get(tw));
						}
					}
				}

				// Update maximum acceptable per tw
				for (TimeWindow tw : acceptedPerTw.keySet()) {
					if (acceptedPerTw.get(tw) > maximumAcceptable.get(tw.getId())) {
						maximumAcceptable.put(tw.getId(), acceptedPerTw.get(tw));
					}
				}
			}
			;

			// For that delivery area: Determine average acceptable per time
			// window

			for (TimeWindow tw : acceptable.keySet()) {
				acceptable.put(tw, acceptable.get(tw) / this.aggregatedReferenceInformationNo.get(area).size());
			}

			this.averageReferenceInformationNoPerDeliveryArea.put(area.getId(), acceptable);
			this.maximalAcceptablePerDeliveryAreaAndTw.put(area.getId(), maximumAcceptable);

		}
	}

	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	private void aggregateReferenceInformation() {

		// TA: distance - really just travel time to?

		this.aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();

		// TA: consider that maximal acceptable costs is initialised too low

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			this.aggregatedReferenceInformationNo.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());

		}

		for (Routing routing : this.targetRoutingResults) {
			HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();

			for (Route r : routing.getRoutes()) {

				DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
						r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
				if (!count.containsKey(area)) {
					count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());

				}
				for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
					RouteElement re = r.getRouteElements().get(reId);
					double travelTimeFrom;
					if (reId < r.getRouteElements().size() - 1) {
						travelTimeFrom = r.getRouteElements().get(reId + 1).getTravelTime();
					} else {
						travelTimeFrom = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
								re.getOrder().getOrderRequest().getCustomer().getLat(),
								re.getOrder().getOrderRequest().getCustomer().getLon(),
								r.getVehicleAssignment().getEndingLocationLat(),
								r.getVehicleAssignment().getEndingLocationLon());
					}
					re.setTravelTimeFrom(travelTimeFrom);
					Customer cus = re.getOrder().getOrderRequest().getCustomer();
					DeliveryArea subArea = LocationService
							.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
					if (!count.get(area).containsKey(subArea)) {
						count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());

					}
					if (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal())) {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);

					} else {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) + 1);

					}
				}
			}

			for (DeliveryArea a : this.aggregatedReferenceInformationNo.keySet()) {
				this.aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));

			}

		}
	}

	public static void checkFeasibilityBasedOnOrienteeringNo(OrderRequest request, int t, DeliveryArea subArea,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting,
			ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced,
			HashMap<Integer, HashMap<Integer, Integer>> avgAcceptablePerSubAreaAndTw,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			boolean thefting, boolean theftingAdvanced, HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			int numberLeftOverTOP, int arrivalProcessId, HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow,
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow) {
		// Possible time windows for request
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
				.getConsiderationSet();
		for (ConsiderationSetAlternative alt : alternatives) {
			if (!alt.getAlternative().getNoPurchaseAlternative())
				timeWindows.addAll(alt.getAlternative().getTimeWindows());
		}

		for (TimeWindow tw : timeWindows) {

			boolean feasible = false;
			if (avgAcceptablePerSubAreaAndTw.containsKey(subArea.getId())) {
				if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).containsKey(tw.getId())) {
					if (avgAcceptablePerSubAreaAndTw.get(subArea.getId()).get(tw.getId()) > 0) {
						timeWindowCandidatesOrienteering.add(tw);
						feasible = true;
					}
				}
			}

			if (!feasible && thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
					&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
					&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

				for (DeliveryArea nArea : neighbors.get(subArea)) {
					if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
						if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {

							if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0) {
								timeWindowCandidatesOrienteeringThefting.add(tw);
								break;
							}
						}
					}
				}

			} else if (!feasible && theftingAdvanced && numberLeftOverTOP < 2) {

				int numberFeasible = 0;
				for (DeliveryArea nArea : neighbors.get(subArea)) {
					if (avgAcceptablePerSubAreaAndTw.containsKey(nArea.getId())) {
						if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).containsKey(tw.getId())) {

							if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0) {
								// Can steal if area has capacity left and does
								// probably not need it
								int overallAvgAcceptableArea = 0;
								for (Integer twId : avgAcceptablePerSubAreaAndTw.get(nArea.getId()).keySet()) {
									overallAvgAcceptableArea += avgAcceptablePerSubAreaAndTw.get(nArea.getId())
											.get(twId);
								}

								// Has area more than needed overall?
								double arrivalsInArea = (request.getArrivalTime() - 1.0)
										* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
										* daLowerWeights.get(nArea);
								double requestedInAreaOverall = arrivalsInArea
										* (1.0 - minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(nArea)
												.get(null));
								long requestedInAreaOverallInt = Math.round(requestedInAreaOverall);
								if (requestedInAreaOverall < requestedInAreaOverallInt)
									requestedInAreaOverallInt = requestedInAreaOverallInt - 1;
								double requestedForTimeWindow = arrivalsInArea
										* maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(nArea).get(tw);
								long requestedInAreaTwInt = Math.round(requestedForTimeWindow);
								if (requestedForTimeWindow < requestedInAreaTwInt)
									requestedInAreaTwInt = requestedInAreaTwInt - 1;

								if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId())
										- requestedInAreaTwInt > 1
										|| (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 1
												&& overallAvgAcceptableArea - requestedInAreaOverallInt > 1)) {
									potentialTimeWindowCandidatesOrienteeringTheftingAdvanced.add(tw);
									break;
								} else if (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId())
										- requestedInAreaTwInt > 0
										|| (avgAcceptablePerSubAreaAndTw.get(nArea.getId()).get(tw.getId()) > 0
												&& overallAvgAcceptableArea - requestedInAreaOverallInt > 0)) {
									numberFeasible++;
									if (numberFeasible > 1) {
										potentialTimeWindowCandidatesOrienteeringTheftingAdvanced.add(tw);
										break;
									}

								}
							}
						}
					}
				}
			}

		}
	}

	public static Set<TimeWindow> determineFeasibleAndStealingAreasPerTOP(DeliveryArea area, DeliveryArea subArea,
			OrderRequest request, int t,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteering,
			ArrayList<TimeWindow> timeWindowCandidatesOrienteeringThefting,
			ArrayList<TimeWindow> potentialTimeWindowCandidatesOrienteeringTheftingAdvanced,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting,
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting,
			boolean thefting, boolean theftingAdvanced, HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			int arrivalProcessId, Set<TimeWindow> timeWindowsToOffer, int currentNumberOfTOPs,
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow,
			HashMap<DeliveryArea, Double> daLowerWeights) {

		ArrayList<TimeWindow> timeWindowCandidates = new ArrayList<TimeWindow>();
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteering);
		timeWindowCandidates.addAll(timeWindowCandidatesOrienteeringThefting);
		timeWindowCandidates.addAll(potentialTimeWindowCandidatesOrienteeringTheftingAdvanced);
		for (Routing r : aggregateInformationNo.keySet()) {

			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> capacities = aggregateInformationNo.get(r);

			// Per tw, determine value after acceptance
			for (TimeWindow tw : timeWindowCandidates) {

				boolean checkAdvanced = true;
				if (capacities.containsKey(subArea) && capacities.get(subArea).containsKey(tw)
						&& capacities.get(subArea).get(tw) > 0) {
					timeWindowsToOffer.add(tw);
					checkAdvanced=false;
				} else if (thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

					// Find area with lowest demand-capacity ratio for stealing
					double lowestRatio = Double.MAX_VALUE;
					DeliveryArea areaWithLowestRatio = null;
					for (DeliveryArea neighbor : neighbors.get(subArea)) {
						if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
								&& capacities.get(neighbor).get(tw) > 0) {

							double arrivalsInArea = (request.getArrivalTime() - 1.0)
									* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
									* daLowerWeights.get(neighbor);

							double requestedForTimeWindow = arrivalsInArea
									* maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(neighbor).get(tw);

							// Best result?
							double ratio = requestedForTimeWindow / ((double) capacities.get(neighbor).get(tw));
							if (ratio < lowestRatio) {
								areaWithLowestRatio = neighbor;
								lowestRatio = ratio;
							}
						}
					}
					
					// If there is an area to steal from, then we save it for
					// potential updates later
					if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
						stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
					}
					if (areaWithLowestRatio != null) {
						stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithLowestRatio);
						timeWindowsToOffer.add(tw);
						checkAdvanced=false;
					}

				} 
				
				if (checkAdvanced && theftingAdvanced && currentNumberOfTOPs < 2) {

					// Look for lowest ratio after stealing
					double lowestRatio = 1.0 * Double.MAX_VALUE;
					ArrayList<DeliveryArea> bestAreas = new ArrayList<DeliveryArea>();

					for (int neighborId = 0; neighborId < neighbors.get(subArea).size(); neighborId++) {

						DeliveryArea neighbor = neighbors.get(subArea).get(neighborId);
						double arrivalsInArea = (request.getArrivalTime() - 1.0)
								* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
								* daLowerWeights.get(neighbor);

						double requestedForTimeWindow = arrivalsInArea
								* maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(neighbor).get(tw);

						if (capacities.containsKey(neighbor) && capacities.get(neighbor).containsKey(tw)
								&& capacities.get(neighbor).get(tw) > 1) {

							double divisor = capacities.get(neighbor).get(tw) - 2.0;
							if (divisor == 0.0) {
								divisor = 0.5;
							}
							double ratio = requestedForTimeWindow / divisor;

							if (ratio < lowestRatio) {
								bestAreas.clear();
								bestAreas.add(neighbor);
								lowestRatio = ratio;
								timeWindowsToOffer.add(tw);
							}

						}

						if ((neighborId < neighbors.get(subArea).size() - 1) && capacities.containsKey(neighbor)
								&& capacities.get(neighbor).containsKey(tw) && capacities.get(neighbor).get(tw) > 0) {

							double bestCombination = 1.0 * Double.MAX_VALUE;
							DeliveryArea bestSecondNeighbor = null;
							double divisor = capacities.get(neighbor).get(tw) - 1.0;
							if (divisor == 0.0)
								divisor = 0.5;
							double ratio = requestedForTimeWindow / divisor;
							for (int neighborId2 = neighborId + 1; neighborId2 < neighbors.get(subArea)
									.size(); neighborId2++) {
								DeliveryArea neighbor2 = neighbors.get(subArea).get(neighborId2);

								if (capacities.containsKey(neighbor2) && capacities.get(neighbor2).containsKey(tw)
										&& capacities.get(neighbor2).get(tw) > 0) {

									double arrivalsInArea2 = (request.getArrivalTime() - 1.0)
											* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
											* daLowerWeights.get(neighbor2);

									double requestedForTimeWindow2 = arrivalsInArea2
											* maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(neighbor2)
													.get(tw);
									double divisor2 = capacities.get(neighbor2).get(tw) - 1.0;
									if (divisor2 == 0.0)
										divisor2 = 0.5;
									double ratio2 = requestedForTimeWindow2 / divisor2;
									ratio2 = 0.5 * ratio2 + 0.5 * ratio;
									if (ratio2 < bestCombination) {
										bestCombination = ratio2;
										bestSecondNeighbor = neighbor2;
									}
								}
							}

							if (bestSecondNeighbor != null && bestCombination < lowestRatio) {
								bestAreas.clear();
								bestAreas.add(neighbor);
								bestAreas.add(bestSecondNeighbor);
								lowestRatio = bestCombination;
								timeWindowsToOffer.add(tw);
							}
						}

					}
					// If there is an area to steal from, then save areas for
					// later udpates
					if (!advancedStealingAreasPerTimeWindowAndRouting.containsKey(tw)) {
						advancedStealingAreasPerTimeWindowAndRouting.put(tw,
								new HashMap<Routing, ArrayList<DeliveryArea>>());
					}
					if (bestAreas.size() > 0) {
						advancedStealingAreasPerTimeWindowAndRouting.get(tw).put(r, bestAreas);
					}
				}
			}
		}

		return timeWindowsToOffer;
	}

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				acceptedPerTw.put(tw.getId(), 0);
			}
			this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
		}
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return this.orderSet;
	}

}
