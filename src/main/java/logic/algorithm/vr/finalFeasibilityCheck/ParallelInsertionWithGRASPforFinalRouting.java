package logic.algorithm.vr.finalFeasibilityCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.utility.comparator.PairIntegerValueAscComparator;

public class ParallelInsertionWithGRASPforFinalRouting implements RoutingAlgorithm {

	private static double TIME_MULTIPLIER = 60.0;
	private OrderSet orderSet;
	private DeliveryAreaSet deliveryAreaSet;
	private Region region;
	private TimeWindowSet timeWindowSet;
	private int includeDriveFromStartingPosition;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;
	private double expectedServiceTime;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private Routing finalRouting;

	private static String[] paras = new String[] { "includeDriveFromStartingPosition", "no_routing_candidates_final",
			"no_insertion_candidates_final", "Constant_service_time" };

	public ParallelInsertionWithGRASPforFinalRouting(OrderSet orderSet, DeliveryAreaSet deliveryAreaSet,
			VehicleAreaAssignmentSet vehicleAreaAssignmentSet, Double includeDriveFromStartingPosition,
			Double numberRoutingCandidates, Double numberPotentialInsertionCandidates, Double constantServiceTime) {

		this.orderSet = orderSet;
		this.timeWindowSet = orderSet.getOrderRequestSet().getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.deliveryAreaSet = deliveryAreaSet;
		this.region = this.deliveryAreaSet.getRegion();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		this.expectedServiceTime = constantServiceTime;
	}

	public void start() {

		// Group vehicle area assignments according to delivery areas
		HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>> vaaPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>>();
		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			if (!vaaPerDeliveryArea.containsKey(ass.getDeliveryArea())) {
				vaaPerDeliveryArea.put(ass.getDeliveryArea(), new HashMap<Integer, VehicleAreaAssignment>());
			}

			vaaPerDeliveryArea.get(ass.getDeliveryArea()).put(ass.getVehicleNo(), ass);
		}

		// Seperate orders according to delivery area
		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements())
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());

		for (Order order : this.orderSet.getElements()) {
			if (order.getAccepted()) {
				DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
						order.getOrderRequest().getCustomer());
				ordersPerDeliveryArea.get(area).add(order);
			}

		}

		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Integer> infeasibleOfBestRouting = new HashMap<DeliveryArea, Integer>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			ArrayList<Pair<HashMap<Integer, ArrayList<RouteElement>>, Integer>> infeasibleRoutings = new ArrayList<Pair<HashMap<Integer, ArrayList<RouteElement>>, Integer>>();

			DynamicRoutingHelperService.determinePossibleRoutingsIncludingInfeasibleWithShiftWithoutWait(
					possibleRoutings, infeasibleRoutings, ordersPerDeliveryArea.get(area), vaaPerDeliveryArea.get(area),
					timeWindowSet, this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates,
					TIME_MULTIPLIER, region, (includeDriveFromStartingPosition == 1), expectedServiceTime);

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> routingsToConsider = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();

			int lowestInfeasible = 0;
			if (possibleRoutings.size() > 0) {
				routingsToConsider.addAll(possibleRoutings);
			} else {
				// Go through infeasible routings and find with lowest number of
				// infeasible ones
				Collections.sort(infeasibleRoutings, new PairIntegerValueAscComparator());
				lowestInfeasible = infeasibleRoutings.get(0).getValue();
				int currentIndex = 0;
				while (currentIndex< infeasibleRoutings.size() && infeasibleRoutings.get(currentIndex).getValue() == lowestInfeasible) {
					routingsToConsider.add(infeasibleRoutings.get(currentIndex).getKey());
					currentIndex++;
				}

			}

			// Find best routing
			double bestValue = Double.MAX_VALUE;
			HashMap<Integer, ArrayList<RouteElement>> bestRouting = new HashMap<Integer, ArrayList<RouteElement>>();
			for (int i = 0; i < routingsToConsider.size(); i++) {

				// Is the routing better than the one found so far? 
				double acceptedTT = 0.0;

				for (Integer routeId : routingsToConsider.get(i).keySet()) {
					for (int eId = 1; eId < routingsToConsider.get(i).get(routeId).size() - 1; eId++) {

						RouteElement e = routingsToConsider.get(i).get(routeId).get(eId);
						if (!(eId == 1 && (includeDriveFromStartingPosition == 0))) {

							acceptedTT += e.getTravelTimeTo();
						}
					}

					if ((includeDriveFromStartingPosition == 1)) {
						acceptedTT += routingsToConsider.get(i).get(routeId)
								.get(routingsToConsider.get(i).get(routeId).size() - 1).getTravelTimeTo();
					}

				}

				// Update if better or first
				if (acceptedTT < bestValue) {

					bestRouting = routingsToConsider.get(i);
					bestValue = acceptedTT;
				}
			}

			bestRoutingPerDeliveryArea.put(area, bestRouting);
			infeasibleOfBestRouting.put(area, lowestInfeasible);
		}

		ArrayList<Route> routes = new ArrayList<Route>();
		int additionalCosts = 0;
		for (DeliveryArea area : bestRoutingPerDeliveryArea.keySet()) {

			for (Integer routeId : bestRoutingPerDeliveryArea.get(area).keySet()) {
				Route route = new Route();
				ArrayList<RouteElement> elements = bestRoutingPerDeliveryArea.get(area).get(routeId);
				// Delete dummy elements
				elements.remove(0);
				elements.remove(elements.size() - 1);

				for (RouteElement e : elements) {
					e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
					e.setTravelTime(e.getTravelTimeTo());
				}
				route.setRouteElements(elements);
				route.setVehicleAreaAssignmentId(vaaPerDeliveryArea.get(area).get(routeId).getId());
				routes.add(route);
			}

			additionalCosts += infeasibleOfBestRouting.get(area);
		}

		this.finalRouting = new Routing();
		this.finalRouting.setRoutes(routes);
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setAdditionalCosts(additionalCosts);
		this.finalRouting.setOrderSet(orderSet);

	}

	public Routing getResult() {
		return this.finalRouting;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

}
