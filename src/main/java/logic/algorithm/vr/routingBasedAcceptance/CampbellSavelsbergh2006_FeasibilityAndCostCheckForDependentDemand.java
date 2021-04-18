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
 * Implements an adapted version of the algorithm from Campbell & Savelsbergh, 2006
 * No incentives used, but only applied to check feasibility and costs
 * The feasibility check is slightly adapted by using the "slack"-construct (same decision!)
 *
 * Campbell, A. M., & Savelsbergh, M. (2006). Incentive schemes for attended home delivery services. Transportation science, 40(3), 327-341.
 * @author M. Lang
 *
 */
public class CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand implements RoutingAlgorithm {

	private static int numberOfThreads=3;
	private static double TIME_MULTIPLIER = 60.0;
	private static double COST_MULTIPLIER =0.2;
	private static double PROFIT_MULTIPLIER =0.3;
	private static double DISTANCE_MULTIPLIER=1.5;
	private OrderRequestSet orderRequestSet;
	private DeliveryAreaSet deliveryAreaSet;
	private Region region;
	private TimeWindowSet timeWindowSet;
	private int includeDriveFromStartingPosition;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;
	private int usePreferencesSampled;
	private int considerProfit;
	private double expectedServiceTime;

	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private Routing finalRouting;
	private boolean deliveryAreaHierarchy = false;

	private static String[] paras = new String[] { "includeDriveFromStartingPosition", "no_routing_candidates",
			"no_insertion_candidates", "Constant_service_time", "samplePreferences","consider_profit"};

	public CampbellSavelsbergh2006_FeasibilityAndCostCheckForDependentDemand(OrderRequestSet orderRequestSet,
			DeliveryAreaSet deliveryAreaSet, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			Double includeDriveFromStartingPosition, Double numberRoutingCandidates,
			Double numberPotentialInsertionCandidates, Double constantServiceTime, Double preferencesSampled, Double considerProfit) {

		this.orderRequestSet = orderRequestSet;
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.deliveryAreaSet = deliveryAreaSet;
		this.region = this.deliveryAreaSet.getRegion();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		this.expectedServiceTime = constantServiceTime;
		this.usePreferencesSampled = preferencesSampled.intValue();
		this.considerProfit=considerProfit.intValue();
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
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
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
			Double currentAcceptedTravelTime;
			if (deliveryAreaHierarchy) {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				relevantVaas = vaaPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(area.getDeliveryAreaOfSet());

			} else {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
				relevantVaas = vaaPerDeliveryArea.get(area);
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area);
				currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(area);
			}

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();

			currentAcceptedTravelTime = DynamicRoutingHelperService.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(request,
					region, TIME_MULTIPLIER, this.timeWindowSet, (this.includeDriveFromStartingPosition == 1),
					this.expectedServiceTime, possibleRoutings, this.numberOfGRASPSolutions,
					this.numberPotentialInsertionCandidates, relevantVaas, relevantOrders, bestRoutingSoFar,
					currentAcceptedTravelTime, this.timeWindowSet.getElements(),
					bestRoutingsValueAfterInsertion, numberOfThreads);

			if(this.considerProfit==1){
				
				//Only offer time windows that allow profit (based on temporary cost estimation)
				Set<Integer> timeWindowsWithProfit = new HashSet<Integer>();
				for(Integer tw: bestRoutingsValueAfterInsertion.keySet()){
					if((bestRoutingsValueAfterInsertion.get(tw).getValue()-currentAcceptedTravelTime)*COST_MULTIPLIER<= request.getBasketValue()*PROFIT_MULTIPLIER){
						//If the drive to the depot is not relevant for scheduling, we should nevertheless consider it for cost determination
						if(this.includeDriveFromStartingPosition==0 && possibleRoutings.get(bestRoutingsValueAfterInsertion.get(tw).getKey()
								.getTempRoutingId()).get(bestRoutingsValueAfterInsertion.get(tw)
								.getKey().getTempRoute()).size()==2){
							if((bestRoutingsValueAfterInsertion.get(tw).getKey().getTravelTimeTo()*2)*COST_MULTIPLIER<= request.getBasketValue()*PROFIT_MULTIPLIER){
								timeWindowsWithProfit.add(tw);
							}
						}else{
							timeWindowsWithProfit.add(tw);
						}
						
					}
				}
				
				CustomerDemandService.simulateCustomerDecision(order, timeWindowsWithProfit,
						orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(),
						alternativesForTimeWindows, (this.usePreferencesSampled == 1));
				
			}else{
				CustomerDemandService.simulateCustomerDecision(order, bestRoutingsValueAfterInsertion.keySet(),
						orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(),
						alternativesForTimeWindows, (this.usePreferencesSampled == 1));
			}
			

			if (order.getAccepted()) {

				RouteElement elementToInsert = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
						.getKey();
				int routingId = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId()).getKey()
						.getTempRoutingId();
				elementToInsert.setOrder(order);
				order.setAssignedValue(elementToInsert.getTempShiftWithoutWait()-expectedServiceTime);
				DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
						relevantVaas, timeWindowSet, TIME_MULTIPLIER, (includeDriveFromStartingPosition == 1));
				bestRoutingSoFar = possibleRoutings.get(routingId);
				currentAcceptedTravelTime = bestRoutingsValueAfterInsertion.get(order.getTimeWindowFinalId())
						.getValue();

			}

			if (this.deliveryAreaHierarchy) {
				ordersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);
				//Update best and accepted travel time because it could change also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area.getDeliveryAreaOfSet(), bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(area.getDeliveryAreaOfSet(),
						currentAcceptedTravelTime);
				if (order.getAccepted()) {
					
					acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);
					

				}

			} else {
				ordersPerDeliveryArea.get(area).add(order);
				//Update best and accepted travel time because it could change also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area, bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(area, currentAcceptedTravelTime);
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
//				try {
//					System.out.println(ProbabilityDistributionService.getXByCummulativeDistributionQuantile(elements.get(0).getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment().getBasketValueDistribution(), 0.1));
//					System.out.println(ProbabilityDistributionService.getXByCummulativeDistributionQuantile(elements.get(0).getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment().getBasketValueDistribution(), 0.5));
//					System.out.println(ProbabilityDistributionService.getXByCummulativeDistributionQuantile(elements.get(0).getOrder().getOrderRequest().getCustomer().getOriginalDemandSegment().getBasketValueDistribution(), 0.9));
//
//				} catch (ParameterUnknownException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
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
