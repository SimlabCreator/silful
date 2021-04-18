package logic.algorithm.vr.routingBasedAcceptance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Implements a variant of the Yang et al. 2016 Hindsight policy for dynamic slotting without cost (essentially, Algorithm 1 from Paper)
 * 
 * @author M. Lang
 *
 */
public class Yang2016_Hindsight_DynamicSlotting implements RoutingAlgorithm {

	private static int numberOfThreads=3;
	private static double TIME_MULTIPLIER = 60.0;
	private static double DISTANCE_MULTIPLIER=1.5;
	private OrderRequestSet orderRequestSet;
	private DeliveryAreaSet deliveryAreaSet;
	private Region region;
	private TimeWindowSet timeWindowSet;
	private int includeDriveFromStartingPosition;
	private int numberOfPotentialSchedules;
	private int usePreferencesSampled;
	private double expectedServiceTime;

	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private Routing finalRouting;
	private boolean deliveryAreaHierarchy = false;

	private static String[] paras = new String[] { "includeDriveFromStartingPosition", "no_routing_candidates",
			"Constant_service_time", "samplePreferences"};

	public Yang2016_Hindsight_DynamicSlotting(OrderRequestSet orderRequestSet,
			DeliveryAreaSet deliveryAreaSet, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			Double includeDriveFromStartingPosition, Double numberRoutingCandidates,
			Double constantServiceTime, Double preferencesSampled) {

		this.orderRequestSet = orderRequestSet;
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.deliveryAreaSet = deliveryAreaSet;
		this.region = this.deliveryAreaSet.getRegion();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfPotentialSchedules = numberRoutingCandidates.intValue();
		this.expectedServiceTime = constantServiceTime;
		this.usePreferencesSampled = preferencesSampled.intValue();
		DynamicRoutingHelperService.distanceMultiplierAsTheCrowFlies=DISTANCE_MULTIPLIER;
	}

	public void start() {

		// Initialise alternatives to time window mapping
		HashMap<Integer, Alternative> alternativesForTimeWindows = new HashMap<Integer, Alternative>();
		for (Alternative alt : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {

				alternativesForTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
			}

		}

		// Group vehicle area assignments according to delivery areas
		HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>> vaaPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>>();
		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			if (!vaaPerDeliveryArea.containsKey(ass.getDeliveryArea())) {
				vaaPerDeliveryArea.put(ass.getDeliveryArea(), new HashMap<Integer, VehicleAreaAssignment>());
			}

			vaaPerDeliveryArea.get(ass.getDeliveryArea()).put(ass.getVehicleNo(), ass);
		}

		// Sort order requests (descending arrival time)
		ArrayList<OrderRequest> orderRequests = this.orderRequestSet.getElements();
		Collections.sort(orderRequests, new OrderRequestArrivalTimeDescComparator());

		// Initialise order buffers and last routing buffers per delivery area
		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, null);
			if (area.getSubsetId() != null)
				deliveryAreaHierarchy = true;
		}

		// Go through requests
		for (OrderRequest request : orderRequests) {
			DeliveryArea area = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
					request.getCustomer());

			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			ArrayList<Order> relevantOrders;
			HashMap<Integer, VehicleAreaAssignment> relevantVaas;
			HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar;
			
			if (deliveryAreaHierarchy) {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				relevantVaas = vaaPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				

			} else {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
				relevantVaas = vaaPerDeliveryArea.get(area);
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area);
			}

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			Set<Integer> timeWindowCandidates = new HashSet<Integer>();
			if(bestRoutingSoFar !=null)
				possibleRoutings.add(bestRoutingSoFar);

			DynamicRoutingHelperService.determineFeasibleSchedulesAndTimeWindowsBasedOnAlgorithm1ofYang2016(request, relevantOrders, 
					region,TIME_MULTIPLIER, this.timeWindowSet, possibleRoutings, timeWindowCandidates, this.numberOfPotentialSchedules,
					relevantVaas, 
					(this.includeDriveFromStartingPosition == 1),
					this.expectedServiceTime, numberOfThreads);
				
				
			if(timeWindowCandidates.size()>0)
				CustomerDemandService.simulateCustomerDecision(order, timeWindowCandidates,
						orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(),
						alternativesForTimeWindows, (this.usePreferencesSampled == 1));
			
			
			//Determine cost per schedule (and, if accepted, the cheapest insertion position)
			ArrayList<Double> costWithoutOrder = new ArrayList<Double>();
			HashMap<Integer, RouteElement> bestInsertionElementPerRouting = new HashMap<Integer, RouteElement>();
			double lowestCost = Double.MAX_VALUE;
			for (int i = 0; i < possibleRoutings.size(); i++) {

				//Determine cost without order
				double acceptedTT = 0.0;
					for (Integer routeId : possibleRoutings.get(i).keySet()) {
						for (int eId = 1; eId < possibleRoutings.get(i).get(routeId).size() - 1; eId++) {

							RouteElement e = possibleRoutings.get(i).get(routeId).get(eId);
							if (!(eId == 1 && includeDriveFromStartingPosition==0)) {
								acceptedTT += e.getTravelTimeTo();
							}
						}

						if (includeDriveFromStartingPosition==1) {
							acceptedTT += possibleRoutings.get(i).get(routeId)
									.get(possibleRoutings.get(i).get(routeId).size() - 1).getTravelTimeTo();
						}

					}
				costWithoutOrder.add(acceptedTT);

				if(acceptedTT < lowestCost) {
					lowestCost=acceptedTT;
					bestRoutingSoFar = possibleRoutings.get(i);
				}
				
				//Find best insertion position for order
				if (order.getAccepted()) {
					RouteElement re = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order, possibleRoutings.get(i), relevantVaas, 
							region, TIME_MULTIPLIER, this.expectedServiceTime, timeWindowSet, (includeDriveFromStartingPosition==1), true);
					
					
					
					if(re!=null) bestInsertionElementPerRouting.put(i, re);
				}
			}
			

			if (order.getAccepted()) {
				
				//Find lowest overall cost after insertion
				double lowestCostEstimate = Double.MAX_VALUE;
				Integer bestRoutingId = null;
				for(Integer i: bestInsertionElementPerRouting.keySet()) {
					double costEstimate = bestInsertionElementPerRouting.get(i).getTempShiftWithoutWait()-this.expectedServiceTime + costWithoutOrder.get(i)-lowestCost;
					if(costEstimate < lowestCostEstimate) {
						lowestCostEstimate=costEstimate;
						bestRoutingId=i;
					}
				}

				
				//Insert
				DynamicRoutingHelperService.insertRouteElement(bestInsertionElementPerRouting.get(bestRoutingId), possibleRoutings.get(bestRoutingId),
						relevantVaas, timeWindowSet, TIME_MULTIPLIER, (includeDriveFromStartingPosition == 1));
				
				//Update best so far
				bestRoutingSoFar = possibleRoutings.get(bestRoutingId);

			}

			if (this.deliveryAreaHierarchy) {
				ordersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);
				//Update best and accepted travel time because it could change also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area.getDeliveryAreaOfSet(), bestRoutingSoFar);
				if (order.getAccepted()) {
					acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);
				}

			} else {
				ordersPerDeliveryArea.get(area).add(order);
				//Update best and accepted travel time because it could change also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area, bestRoutingSoFar);
				if (order.getAccepted()) {
					acceptedOrdersPerDeliveryArea.get(area).add(order);
					
				}
			}

		}

		ArrayList<Route> routes = new ArrayList<Route>();
		ArrayList<Order> orders = new ArrayList<Order>();
		int accepted = 0;
		for (DeliveryArea area : ordersPerDeliveryArea.keySet()) {
			orders.addAll(ordersPerDeliveryArea.get(area));

			for (Integer routeId : bestRoutingSoFarPerDeliveryArea.get(area).keySet()) {
				Route route = new Route();
				
				ArrayList<RouteElement> elements = bestRoutingSoFarPerDeliveryArea.get(area).get(routeId);
				// Delete dummy elements
				elements.remove(0);
				elements.remove(elements.size() - 1);
				accepted+=elements.size();
				for (RouteElement e : elements) {
					e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
					e.setTravelTime(e.getTravelTimeTo());
				}

				route.setRouteElements(elements);
				route.setVehicleAreaAssignmentId(vaaPerDeliveryArea.get(area).get(routeId).getId());
				routes.add(route);
			}
		}

	System.out.println("Accepted "+accepted);
		this.finalRouting = new Routing();
		this.finalRouting.setRoutes(routes);
		this.finalRouting.setPossiblyFinalRouting(true);
		OrderSet orderSet = new OrderSet();
		orderSet.setElements(orders);
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.finalRouting.setOrderSet(orderSet);

	}

	

	public Routing getResult() {
		return this.finalRouting;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

}
