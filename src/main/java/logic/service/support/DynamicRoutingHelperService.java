package logic.service.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.Region;
import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.utility.comparator.RouteElementTempShiftWithoutWaitingAscComparator;

public class DynamicRoutingHelperService {

	public static double distanceMultiplierAsTheCrowFlies = 1.5;

	public static HashMap<Integer, ArrayList<RouteElement>> initialiseRoutes(DeliveryArea area,
			HashMap<Integer, ArrayList<VehicleAreaAssignment>> vaPerDeliveryArea, TimeWindowSet timeWindowSet,
			double timeMultiplier, Region region, boolean includeDriveFromStartingPosition) {
		return DynamicRoutingHelperService.initialiseRoutes(vaPerDeliveryArea.get(area.getId()), timeWindowSet,
				timeMultiplier, region, includeDriveFromStartingPosition);
	}

	public static HashMap<Integer, ArrayList<RouteElement>> initialiseRoutes(
			Collection<VehicleAreaAssignment> vehicleAreaAssignments, TimeWindowSet timeWindowSet,
			double timeMultiplier, Region region, boolean includeDriveFromStartingPosition) {
		HashMap<Integer, ArrayList<RouteElement>> routes = new HashMap<Integer, ArrayList<RouteElement>>();

		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(timeWindowSet, timeMultiplier);

		for (VehicleAreaAssignment ass : vehicleAreaAssignments) {
			ArrayList<RouteElement> route = new ArrayList<RouteElement>();
			RouteElement startE = new RouteElement();
			startE.setServiceBegin(
					(ass.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier);
			startE.setServiceTime(0.0);
			startE.setSlack(0.0);
			startE.setWaitingTime(0.0);
			startE.setTravelTimeTo(0.0);
			OrderRequest startR = new OrderRequest();
			Customer startCustomer = new Customer();
			startCustomer.setLat(ass.getStartingLocationLat());
			startCustomer.setLon(ass.getStartingLocationLon());
			startR.setCustomer(startCustomer);
			startR.setBasketValue(0.0);
			Order order = new Order();
			order.setOrderRequest(startR);
			startE.setOrder(order);
			route.add(startE);

			RouteElement endE = new RouteElement();
			endE.setServiceTime(0.0);
			endE.setServiceBegin((ass.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier);
			endE.setSlack(0.0);
			endE.setTravelTimeFrom(0.0);
			OrderRequest endR = new OrderRequest();
			Customer endCustomer = new Customer();

			endCustomer.setLat(ass.getEndingLocationLat());
			endCustomer.setLon(ass.getEndingLocationLon());

			endR.setCustomer(endCustomer);
			endR.setBasketValue(0.0);
			Order endOrder = new Order();
			endOrder.setOrderRequest(endR);
			endE.setOrder(endOrder);
			route.add(endE);

			// Determine travel time between end and start location and set
			// respective values in end element
			double travelTimeBetween = LocationService.calculateHaversineDistanceBetweenCustomers(startCustomer,
					endCustomer) * distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			startE.setTravelTimeFrom(travelTimeBetween);
			endE.setTravelTimeTo(travelTimeBetween);

			if (!includeDriveFromStartingPosition) {
				endE.setWaitingTime((ass.getEndTime() - ass.getStartTime()) * timeMultiplier);
			} else {
				endE.setWaitingTime((ass.getEndTime() - ass.getStartTime()) * timeMultiplier - travelTimeBetween);
			}

			routes.put(ass.getVehicleNo(), route);
		}

		return routes;
	}

	/**
	 * Provides a hashmap of time window ids and respective route elements that
	 * provide the cheapest possible insertion for this time window (ready to be
	 * inserted in the route)
	 * 
	 * @param request                          Respective order requests for which
	 *                                         feasibility needs to be checked
	 * @param routes                           Current state of the routes
	 * @param region                           Current region (to obtain average
	 *                                         speed per hour)
	 * @param timeMultiplier                   Time multiplier that consideres the
	 *                                         unit of time window length
	 * @param expectedServiceTime              Expected service time of the request
	 * @param timeWindowSet                    Time window set for which the check
	 *                                         should be done
	 * @param includeDriveFromStartingPosition Travel from/to start position is
	 *                                         counted?
	 * @return HashMap with (key-> time window id) and (value -> route element)
	 */
	public static HashMap<Integer, RouteElement> getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(
			OrderRequest request, HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition) {

		// Collect best elements per tw over all routes (only contains feasible
		// ones)
		// Integer: tw id
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = routes.get(routeId);

			HashMap<Integer, RouteElement> best = DynamicRoutingHelperService.getCheapestInsertionElementByRoute(route,
					vehicleAreaAssignmentPerVehicleNo.get(routeId), routeId, request, region, timeMultiplier,
					expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition);

			// Update overall best
			for (int key : best.keySet()) {
				if (!bestElementPerTw.containsKey(key)
						|| (bestElementPerTw.get(key).getTempShift() > best.get(key).getTempShift())
						|| ((bestElementPerTw.get(key).getTempShift() == best.get(key).getTempShift())
								&& new Random().nextBoolean())) {

					bestElementPerTw.put(key, best.get(key));

				}
			}

		}

		return bestElementPerTw;
	}

	public static HashMap<Integer, RouteElement> getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(
			OrderRequest request, HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, ArrayList<TimeWindow> timeWindowCandidates,
			boolean evaluateInsertionCostsBasedOnCostWithoutWait) {

		// Collect best elements per tw over all routes (only contains feasible
		// ones)
		// Integer: tw id
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = routes.get(routeId);

			HashMap<Integer, RouteElement> best = DynamicRoutingHelperService.getCheapestInsertionElementByRoute(route,
					vehicleAreaAssignmentPerVehicleNo.get(routeId), routeId, request, region, timeMultiplier,
					expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition, timeWindowCandidates,
					evaluateInsertionCostsBasedOnCostWithoutWait);

			// Update overall best
			for (int key : best.keySet()) {
				if (!bestElementPerTw.containsKey(key)
						|| (bestElementPerTw.get(key).getTempShift() > best.get(key).getTempShift()
								&& !evaluateInsertionCostsBasedOnCostWithoutWait)
						|| ((bestElementPerTw.get(key).getTempShift() == best.get(key).getTempShift()
								&& !evaluateInsertionCostsBasedOnCostWithoutWait) && new Random().nextBoolean())
						|| (bestElementPerTw.get(key).getTempShiftWithoutWait() > best.get(key)
								.getTempShiftWithoutWait() && evaluateInsertionCostsBasedOnCostWithoutWait)
						|| ((bestElementPerTw.get(key).getTempShiftWithoutWait() == best.get(key)
								.getTempShiftWithoutWait() && evaluateInsertionCostsBasedOnCostWithoutWait)
								&& new Random().nextBoolean())) {

					bestElementPerTw.put(key, best.get(key));

				}
			}

		}

		return bestElementPerTw;
	}

	public static HashMap<Integer, RouteElement> getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(
			OrderRequest request, HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, ArrayList<TimeWindow> timeWindowCandidates) {

		// Collect best elements per tw over all routes (only contains feasible
		// ones)
		// Integer: tw id
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = routes.get(routeId);

			HashMap<Integer, RouteElement> best = DynamicRoutingHelperService.getCheapestInsertionElementByRoute(route,
					vehicleAreaAssignmentPerVehicleNo.get(routeId), routeId, request, region, timeMultiplier,
					expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition, timeWindowCandidates, false);

			// Update overall best
			for (int key : best.keySet()) {
				if (!bestElementPerTw.containsKey(key)
						|| (bestElementPerTw.get(key).getTempShift() > best.get(key).getTempShift())
						|| ((bestElementPerTw.get(key).getTempShift() == best.get(key).getTempShift())
								&& new Random().nextBoolean())) {

					bestElementPerTw.put(key, best.get(key));

				}
			}

		}

		return bestElementPerTw;
	}

	private static HashMap<Integer, RouteElement> getCheapestInsertionElementByRoute(ArrayList<RouteElement> route,
			VehicleAreaAssignment vehicleAreaAssignment, int routeId, OrderRequest request, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, ArrayList<TimeWindow> timeWindows,
			boolean evaluateInsertionCostsBasedOnCostWithoutWait) {

		HashMap<Integer, RouteElement> bestOptionsPerTW = new HashMap<Integer, RouteElement>();

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			double travelTimeTo;
			double travelTimeFrom;
			double travelTimeOld;

			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
					* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
			travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
					eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
					/ region.getAverageKmPerHour() * timeMultiplier;
			travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
					eBefore.getOrder().getOrderRequest().getCustomer(),
					eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
					/ region.getAverageKmPerHour() * timeMultiplier;

			Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
			double shiftWithoutWait;
			if (position == 1 && (position == route.size() - 1) && !includeDriveFromStartingPosition) {
				shiftWithoutWait = expectedServiceTime;
				maximumPushOfNext += expectedServiceTime - 1;
			} else if (position == 1 && !includeDriveFromStartingPosition) {
				// If we are at first position after depot, the travel time
				// to takes nothing from the capacity
				shiftWithoutWait = travelTimeFrom + expectedServiceTime;
			} else if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
				// If we are at last position before depot, the travel time
				// from takes nothing from the capacity
				shiftWithoutWait = travelTimeTo + expectedServiceTime;

				// And the depot arrival can be shifted by the service time
				// (service time only needs to start before end)
				maximumPushOfNext += expectedServiceTime - 1;
			} else {
				shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
			}

			// If shift without wait is already larger than the allowed
			// push, do not consider position further
			if (maximumPushOfNext >= shiftWithoutWait) {

				double earliestStart;
				if (position == 1 && !includeDriveFromStartingPosition) {
					earliestStart = Math.max(0,
							(vehicleAreaAssignment.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
									* timeMultiplier);
				} else {
					earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
				}

				double latestStart = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack()
						- expectedServiceTime;
				if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
					latestStart = (vehicleAreaAssignment.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier - 1;
				}

				// Go through possible time windows and check if best value for
				// time window -> update

				for (TimeWindow tw : timeWindows) {

					if (((tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier > earliestStart)
							&& ((tw.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
									* timeMultiplier < latestStart)) {
						double wait = Math.max(0f,
								(tw.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
										- earliestStart);
						double shift = shiftWithoutWait + wait;
						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							// Update element for time windows if better
							if (!bestOptionsPerTW.containsKey(tw.getId())
									|| (shift < bestOptionsPerTW.get(tw.getId()).getTempShift()
											&& !evaluateInsertionCostsBasedOnCostWithoutWait)
									|| ((shift == bestOptionsPerTW.get(tw.getId()).getTempShift())
											&& new Random().nextBoolean()
											&& !evaluateInsertionCostsBasedOnCostWithoutWait)
									|| (shiftWithoutWait < bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait()
											&& evaluateInsertionCostsBasedOnCostWithoutWait)
									|| ((shiftWithoutWait == bestOptionsPerTW.get(tw.getId()).getTempShiftWithoutWait())
											&& new Random().nextBoolean()
											&& evaluateInsertionCostsBasedOnCostWithoutWait)) {
								RouteElement newElement = new RouteElement();
								Order newOrder = new Order();
								newOrder.setOrderRequest(request);
								newOrder.setOrderRequestId(request.getId());
								newElement.setOrder(newOrder);
								newElement.setServiceTime(expectedServiceTime);
								newElement.setTempRoute(routeId);
								newElement.setTempPosition(position);
								newElement.getOrder().setTimeWindowFinalId(tw.getId());
								newElement.setTempShift(shift);
								newElement.setTempShiftWithoutWait(shiftWithoutWait);
								newElement.setTravelTimeTo(travelTimeTo);
								newElement.setTravelTimeFrom(travelTimeFrom);
								newElement.setWaitingTime(wait);
								newElement.setServiceBegin((earliestStart + wait));
								newElement.setTempSlack(Math.min(
										route.get(position).getSlack() + route.get(position).getWaitingTime() - shift,
										(tw.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
												* timeMultiplier - (earliestStart + wait)));
								newElement.setTempBufferToNext(maximumPushOfNext - shift);
								bestOptionsPerTW.put(tw.getId(), newElement);
							}
						}

					}
				}
			}

		}

		return bestOptionsPerTW;

	}

	private static HashMap<Integer, RouteElement> getCheapestInsertionElementByRoute(ArrayList<RouteElement> route,
			VehicleAreaAssignment vehicleAreaAssignment, int routeId, OrderRequest request, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition) {

		// Possible time windows for request
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		ArrayList<ConsiderationSetAlternative> alternatives = request.getCustomer().getOriginalDemandSegment()
				.getConsiderationSet();
		for (ConsiderationSetAlternative alt : alternatives) {
			if (!alt.getAlternative().getNoPurchaseAlternative())
				timeWindows.addAll(alt.getAlternative().getTimeWindows());
		}

		return DynamicRoutingHelperService.getCheapestInsertionElementByRoute(route, vehicleAreaAssignment, routeId,
				request, region, timeMultiplier, expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition,
				timeWindows, false);

	}

	public static void insertRouteElement(RouteElement element, HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, TimeWindowSet timeWindowSet,
			double timeMultiplier, boolean includeDriveFromStartingPosition) {

		ArrayList<RouteElement> route = routes.get(element.getTempRoute());
		route.add(element.getTempPosition(), element);

		// Update travel times
		route.get(element.getTempPosition() + 1).setTravelTimeTo(element.getTravelTimeFrom());
		route.get(element.getTempPosition() - 1).setTravelTimeFrom(element.getTravelTimeTo());

		// Update the following elements
		double currentShift = element.getTempShift();
		for (int k = element.getTempPosition() + 1; k < route.size(); k++) {
			if (currentShift == 0)
				break;
			double oldWaitingTime = route.get(k).getWaitingTime();
			route.get(k).setWaitingTime(Math.max(0, oldWaitingTime - currentShift));
			currentShift = Math.max(0, currentShift - oldWaitingTime);
			if (k != route.size() - 1) {
				route.get(k).setServiceBegin(route.get(k).getServiceBegin() + currentShift);
				route.get(k).setSlack(route.get(k).getSlack() - currentShift);
			}
			if (route.get(k).getSlack() < 0)
				System.out.println("Strange");
		}

		// Update slack from current element and the ones before
		for (int k = element.getTempPosition(); k > 0; k--) {
			double maxShift = Math.min(
					(route.get(k).getOrder().getTimeWindowFinal().getEndTime()
							- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
							- route.get(k).getServiceBegin(),
					route.get(k + 1).getWaitingTime() + route.get(k + 1).getSlack());
			if (k == route.size() - 2 && !includeDriveFromStartingPosition) {
				maxShift = (route.get(k).getOrder().getTimeWindowFinal().getEndTime()
						- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
						- route.get(k).getServiceBegin();
			} else if (k == route.size() - 2) {
				maxShift = Math.min(
						(route.get(k).getOrder().getTimeWindowFinal().getEndTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
								- route.get(k).getServiceBegin(),
						(vehicleAreaAssignmentPerVehicleNo.get(element.getTempRoute()).getEndTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
								- route.get(k).getServiceTime() - route.get(k).getTravelTimeFrom()
								- route.get(k).getServiceBegin());
			}
			route.get(k).setSlack(maxShift);
			if (route.get(k).getSlack() < 0)
				System.out.println("Strange-slack");
		}

		// double overallUsedCapacity =0.0;
		// double overallUsedCapacityWithWaiting =0.0;
		// double route1=0.0;
		// double route2=0.0;
		// for(Integer routeId : routes.keySet()){
		// for(int i=1; i < routes.get(routeId).size()-1; i++){
		// RouteElement e = routes.get(routeId).get(i);
		// overallUsedCapacity += e.getTravelTimeFrom()+e.getServiceTime();
		// overallUsedCapacityWithWaiting+=
		// e.getTravelTimeFrom()+e.getServiceTime()+e.getWaitingTime();
		// if(i == routes.get(routeId).size()-2){
		// overallUsedCapacity-=e.getTravelTimeFrom();
		// overallUsedCapacityWithWaiting-=e.getTravelTimeFrom();
		// }
		//
		// if(routeId==1){
		// route1+= e.getTravelTimeFrom()+e.getServiceTime()+e.getWaitingTime();
		// if(i == routes.get(routeId).size()-2){
		// route1-=e.getTravelTimeFrom();
		// }
		// }
		// if(routeId==2){
		// route2+= e.getTravelTimeFrom()+e.getServiceTime()+e.getWaitingTime();
		// if(i == routes.get(routeId).size()-2){
		// route2-=e.getTravelTimeFrom();
		// }
		// }
		// }
		// }
		//
		// if(route1>720){
		// System.out.println("overallUsedCapacity"+overallUsedCapacity+"
		// withWaiting:"+overallUsedCapacityWithWaiting);
		// }
		// if(route2>720){
		// System.out.println("overallUsedCapacity"+overallUsedCapacity+"
		// withWaiting:"+overallUsedCapacityWithWaiting);
		// }
		// if(overallUsedCapacityWithWaiting>1440)
		// System.out.println("overallUsedCapacity"+overallUsedCapacity+"
		// withWaiting:"+overallUsedCapacityWithWaiting);
	}

	public static RouteElement getCheapestInsertionElementByOrder(Order order,
			HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, boolean evaluationBasedOnShiftWithoutWait) {

		RouteElement best = null;

		// Check for all routes
		for (Integer routeId : routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = routes.get(routeId);

			RouteElement routeBest = DynamicRoutingHelperService.getCheapestInsertionElementByOrderAndRoute(route,
					vehicleAreaAssignmentPerVehicleNo.get(routeId), routeId, order, region, timeMultiplier,
					expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition,
					evaluationBasedOnShiftWithoutWait);

			// Update current best
			if (best == null) {
				best = routeBest;
			} else {
				if (routeBest != null) {
					if ((!evaluationBasedOnShiftWithoutWait && routeBest.getTempShift() < best.getTempShift())
							|| (evaluationBasedOnShiftWithoutWait
									&& routeBest.getTempShiftWithoutWait() < best.getTempShiftWithoutWait())) {
						best = routeBest;
					}
				}
			}

		}

		return best;
	}

	public static boolean getAnyFeasibleByOrder(Order order,
			HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<Integer, VehicleAreaAssignment> vehicleAreaAssignmentPerVehicleNo, Region region,
			double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, boolean insert) {

	
		boolean anyFeasible = false;
		
		// Check for all routes all long as not feasible yet
		for (Integer routeId : routes.keySet()) {

			// Get feasible for route
			ArrayList<RouteElement> route = routes.get(routeId);

			//Get first feasible
			RouteElement firstFeasible = DynamicRoutingHelperService.getFirstFeasibleByOrderAndRoute(route,
					vehicleAreaAssignmentPerVehicleNo.get(routeId), routeId, order, region, timeMultiplier,
					expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition);

			//If it was feasible, we can insert it and do not have to check for the next routes
			if(firstFeasible!=null) {
				
				if(insert) DynamicRoutingHelperService.insertRouteElement(firstFeasible, routes, vehicleAreaAssignmentPerVehicleNo, timeWindowSet, timeMultiplier, includeDriveFromStartingPosition);		
				anyFeasible = true;
				break;
			}

		}

		return anyFeasible;
	}
	
	private static RouteElement getFirstFeasibleByOrderAndRoute(ArrayList<RouteElement> route,
			VehicleAreaAssignment vehicleAreaAssignment, int routeId, Order order, Region region, double timeMultiplier,
			double expectedServiceTime, TimeWindowSet timeWindowSet, boolean includeDriveFromStartingPosition) {

		TimeWindow finalTW = order.getTimeWindowFinal();

		RouteElement first = null;
		// Check for all positions (after and before depot), as long as no feasible solution is found
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			// Within final time window?
			if (eBefore.getServiceBegin() > (finalTW.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
					* timeMultiplier)
				break;

			if (eAfter.getServiceBegin()
					+ eAfter.getSlack() >= (finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier) {

				OrderRequest request = order.getOrderRequest();

				double travelTimeTo;
				double travelTimeFrom;
				double travelTimeOld;

				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * timeMultiplier;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * timeMultiplier;

				Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
				double shiftWithoutWait;
				if (position == 1 && (position == route.size() - 1) && !includeDriveFromStartingPosition) {
					shiftWithoutWait = expectedServiceTime;
					maximumPushOfNext += expectedServiceTime - 1;
				} else if (position == 1 && !includeDriveFromStartingPosition) {
					// If we are at first position after depot, the travel time
					// to takes nothing from the capacity
					shiftWithoutWait = travelTimeFrom + expectedServiceTime;
				} else if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
					// If we are at last position before depot, the travel time
					// from takes nothing from the capacity
					shiftWithoutWait = travelTimeTo + expectedServiceTime;

					// And the depot arrival can be shifted by the service time
					// (service time only needs to start before end)
					maximumPushOfNext += expectedServiceTime - 1;
				} else {
					shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
				}

				// If shift without wait is already larger than the allowed
				// push, do not consider position further
				if (maximumPushOfNext >= shiftWithoutWait) {

					double earliestStart;
					if (position == 1 && !includeDriveFromStartingPosition) {
						earliestStart = (vehicleAreaAssignment.getStartTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier;
					} else {
						earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
					}

					double latestStart = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack()
							- expectedServiceTime;
					if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
						latestStart = (vehicleAreaAssignment.getEndTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier - 1;
					}

					if ((earliestStart < (finalTW.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier)
							&& (latestStart >= (finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
									* timeMultiplier)) {
						double wait = Math.max(0.0,
								(finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
										- earliestStart);
						double shift = shiftWithoutWait + wait;
						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							// Update element for time window
								RouteElement newElement = new RouteElement();
								newElement.setOrder(order);
								newElement.setOrderId(order.getId());
								newElement.setServiceTime(expectedServiceTime);
								newElement.setTempRoute(routeId);
								newElement.setTempPosition(position);
								newElement.getOrder().setTimeWindowFinalId(finalTW.getId());
								newElement.setTempShift(shift);
								newElement.setTempShiftWithoutWait(shiftWithoutWait);
								newElement.setTravelTimeTo(travelTimeTo);
								newElement.setTravelTimeFrom(travelTimeFrom);
								newElement.setWaitingTime(wait);
								newElement.setServiceBegin((earliestStart + wait));
								first = newElement;
								break;
							

						}
					}
				}
			}
		}

		return first;
	}
	
	
	
	private static RouteElement getCheapestInsertionElementByOrderAndRoute(ArrayList<RouteElement> route,
			VehicleAreaAssignment vehicleAreaAssignment, int routeId, Order order, Region region, double timeMultiplier,
			double expectedServiceTime, TimeWindowSet timeWindowSet, boolean includeDriveFromStartingPosition,
			boolean evaluationBasedOnShiftWithoutWait) {

		TimeWindow finalTW = order.getTimeWindowFinal();

		RouteElement best = null;
		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			// Within final time window?
			if (eBefore.getServiceBegin() > (finalTW.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
					* timeMultiplier)
				break;

			if (eAfter.getServiceBegin()
					+ eAfter.getSlack() >= (finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier) {

				OrderRequest request = order.getOrderRequest();

				double travelTimeTo;
				double travelTimeFrom;
				double travelTimeOld;

				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						* distanceMultiplierAsTheCrowFlies / region.getAverageKmPerHour() * timeMultiplier;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * timeMultiplier;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) * distanceMultiplierAsTheCrowFlies
						/ region.getAverageKmPerHour() * timeMultiplier;

				Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
				double shiftWithoutWait;
				if (position == 1 && (position == route.size() - 1) && !includeDriveFromStartingPosition) {
					shiftWithoutWait = expectedServiceTime;
					maximumPushOfNext += expectedServiceTime - 1;
				} else if (position == 1 && !includeDriveFromStartingPosition) {
					// If we are at first position after depot, the travel time
					// to takes nothing from the capacity
					shiftWithoutWait = travelTimeFrom + expectedServiceTime;
				} else if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
					// If we are at last position before depot, the travel time
					// from takes nothing from the capacity
					shiftWithoutWait = travelTimeTo + expectedServiceTime;

					// And the depot arrival can be shifted by the service time
					// (service time only needs to start before end)
					maximumPushOfNext += expectedServiceTime - 1;
				} else {
					shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
				}

				// If shift without wait is already larger than the allowed
				// push, do not consider position further
				if (maximumPushOfNext >= shiftWithoutWait) {

					double earliestStart;
					if (position == 1 && !includeDriveFromStartingPosition) {
						earliestStart = (vehicleAreaAssignment.getStartTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier;
					} else {
						earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
					}

					double latestStart = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack()
							- expectedServiceTime;
					if (position == route.size() - 1 && !includeDriveFromStartingPosition) {
						latestStart = (vehicleAreaAssignment.getEndTime()
								- timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier - 1;
					}

					if ((earliestStart < (finalTW.getEndTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
							* timeMultiplier)
							&& (latestStart >= (finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod())
									* timeMultiplier)) {
						double wait = Math.max(0.0,
								(finalTW.getStartTime() - timeWindowSet.getTempStartOfDeliveryPeriod()) * timeMultiplier
										- earliestStart);
						double shift = shiftWithoutWait + wait;
						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							// Update element for time windows if better
							if (best == null || (!evaluationBasedOnShiftWithoutWait && best.getTempShift() > shift)
									|| (evaluationBasedOnShiftWithoutWait
											&& best.getTempShiftWithoutWait() > shiftWithoutWait)) {
								RouteElement newElement = new RouteElement();
								newElement.setOrder(order);
								newElement.setOrderId(order.getId());
								newElement.setServiceTime(expectedServiceTime);
								newElement.setTempRoute(routeId);
								newElement.setTempPosition(position);
								newElement.getOrder().setTimeWindowFinalId(finalTW.getId());
								newElement.setTempShift(shift);
								newElement.setTempShiftWithoutWait(shiftWithoutWait);
								newElement.setTravelTimeTo(travelTimeTo);
								newElement.setTravelTimeFrom(travelTimeFrom);
								newElement.setWaitingTime(wait);
								newElement.setServiceBegin((earliestStart + wait));
								best = newElement;
							}

						}
					}
				}
			}
		}

		return best;
	}

	public static Double determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
			OrderRequest request, Region region, double timeMultiplier, TimeWindowSet timeWindowSet,
			boolean includeDriveFromStartingPosition, double expectedServiceTime,
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings, int numberRoutingCandidates,
			int numberInsertionCandidates, HashMap<Integer, VehicleAreaAssignment> vehiclesPerVehicleNo,
			ArrayList<Order> orders, HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar,
			Double currentAcceptedTravelTime, ArrayList<TimeWindow> timeWindowCandidates,
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion, int numberOfThreads) {

		// Possible routings for already accepted
		if (numberRoutingCandidates > 0 || (numberRoutingCandidates == 0
				&& (bestRoutingSoFar == null || bestRoutingSoFar.keySet().size() == 0))) {
			DynamicRoutingHelperService.determinePossibleRoutingsWithShiftWithoutWait(possibleRoutings, orders,
					vehiclesPerVehicleNo, timeWindowSet, numberRoutingCandidates, numberInsertionCandidates,
					timeMultiplier, region, includeDriveFromStartingPosition, expectedServiceTime, numberOfThreads);
		}

		// Already best Routing so far?
		boolean bestRoutingAdded = false;
		if (bestRoutingSoFar != null && bestRoutingSoFar.keySet().size() > 0) {
			possibleRoutings.add(bestRoutingSoFar);
			bestRoutingAdded = true;
		}

		Double bufferCurrentAcceptedTravelTime = currentAcceptedTravelTime;
		bestRoutingsValueAfterInsertion.clear();

		// Determine cost per schedule and lowest cost (update
		// currentAcceptedTravelTime)
		HashMap<HashMap<Integer, ArrayList<RouteElement>>, Double> costPerSchedule = new HashMap<HashMap<Integer, ArrayList<RouteElement>>, Double>();
		for (int i = 0; i < possibleRoutings.size(); i++) {

			double acceptedTT = 0.0;
			if (!bestRoutingAdded || i < possibleRoutings.size() - 1) {
				for (Integer routeId : possibleRoutings.get(i).keySet()) {
					for (int eId = 1; eId < possibleRoutings.get(i).get(routeId).size() - 1; eId++) {

						RouteElement e = possibleRoutings.get(i).get(routeId).get(eId);
						if (!(eId == 1 && !includeDriveFromStartingPosition)) {

							acceptedTT += e.getTravelTimeTo();
						}
					}

					if (includeDriveFromStartingPosition) {
						acceptedTT += possibleRoutings.get(i).get(routeId)
								.get(possibleRoutings.get(i).get(routeId).size() - 1).getTravelTimeTo();
					}

				}

				// Update if better or first
				if ((bestRoutingSoFar != null && bestRoutingSoFar.keySet().size() > 0
						&& acceptedTT < currentAcceptedTravelTime)
						|| (bestRoutingSoFar == null || bestRoutingSoFar.keySet().size() == 0)) {

					bestRoutingSoFar = possibleRoutings.get(i);
					currentAcceptedTravelTime = acceptedTT;
				}
				costPerSchedule.put(possibleRoutings.get(i), acceptedTT);
			} else {
				costPerSchedule.put(possibleRoutings.get(i), bufferCurrentAcceptedTravelTime);

			}
		}

		// Check if time windows are feasible and find schedule with lowest cost
		// after insertion
		for (int i = 0; i < possibleRoutings.size(); i++) {

			HashMap<Integer, RouteElement> fTws = DynamicRoutingHelperService
					.getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(request, possibleRoutings.get(i),
							vehiclesPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet,
							includeDriveFromStartingPosition, timeWindowCandidates, true);

			// Lower insertion costs? And: best routing after insertion?

			for (Integer twId : fTws.keySet()) {
				fTws.get(twId).setTempRoutingId(i);
				if (bestRoutingsValueAfterInsertion.containsKey(twId)) {
					if (bestRoutingsValueAfterInsertion.get(twId).getValue()
							- currentAcceptedTravelTime > fTws.get(twId).getTempShiftWithoutWait() - expectedServiceTime
									+ costPerSchedule.get(possibleRoutings.get(i)) - currentAcceptedTravelTime) {

						bestRoutingsValueAfterInsertion.put(twId,
								new Pair<RouteElement, Double>(fTws.get(twId), fTws.get(twId).getTempShiftWithoutWait()
										- expectedServiceTime + costPerSchedule.get(possibleRoutings.get(i))));

					}
				} else {
					bestRoutingsValueAfterInsertion.put(twId,
							new Pair<RouteElement, Double>(fTws.get(twId), fTws.get(twId).getTempShiftWithoutWait()
									- expectedServiceTime + costPerSchedule.get(possibleRoutings.get(i))));
				}

			}

		}

		return currentAcceptedTravelTime;
	}

	/**
	 * Determines possible routings for already accepted.
	 * 
	 * @param acceptedOrders
	 * @param vaasPerVehicleNo
	 * @param timeWindowSet
	 * @param numberOfRoutingCandidates
	 * @param numberOfInsertionCandidates
	 * @param timeMultiplier
	 * @param region
	 * @param includeDriveFromStartingPosition
	 * @param expectedServiceTime
	 * @param costMultiplier
	 * @return
	 */
	public static void determinePossibleRoutingsWithShiftWithoutWait(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> routings, ArrayList<Order> acceptedOrders,
			HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo, TimeWindowSet timeWindowSet,
			int numberOfRoutingCandidates, int numberOfInsertionCandidates, double timeMultiplier, Region region,
			boolean includeDriveFromStartingPosition, double expectedServiceTime, int numberOfThreads) {

		int routingId = 0;
		HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routingsBuffer = new HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>>();
		if (numberOfRoutingCandidates == 0) {
			ArrayList<Thread> threads = new ArrayList<Thread>();

			Thread thread1 = new Thread(
					(new DynamicRoutingHelperService()).new PossibleRoutingsWithShiftWithoutWaitRunnable(routingsBuffer,
							acceptedOrders, vaasPerVehicleNo, timeWindowSet, numberOfInsertionCandidates,
							timeMultiplier, region, includeDriveFromStartingPosition, expectedServiceTime, routingId));
			threads.add(thread1);
			thread1.start();

			for (Thread thread : threads) {
				try {
					thread.join();

				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		} else {
			while (routingId < numberOfRoutingCandidates) {
				ArrayList<Thread> threads = new ArrayList<Thread>();
				for (int a = 0; a < numberOfThreads; a++) {
					if (routingId < numberOfRoutingCandidates) {
						Thread thread = new Thread(
								(new DynamicRoutingHelperService()).new PossibleRoutingsWithShiftWithoutWaitRunnable(
										routingsBuffer, acceptedOrders, vaasPerVehicleNo, timeWindowSet,
										numberOfInsertionCandidates, timeMultiplier, region,
										includeDriveFromStartingPosition, expectedServiceTime, routingId));
						threads.add(thread);
						thread.start();
						routingId++;
					}
				}
				for (Thread thread : threads) {
					try {
						thread.join();

					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}

			}
		}
		Iterator<Integer> it = routingsBuffer.keySet().iterator();
		while (it.hasNext()) {
			routings.add(routingsBuffer.get(it.next()));
		}
	}

	/**
	 * Determines possible routings for already accepted. Can return infeasible
	 * routings.
	 * 
	 * @param acceptedOrders
	 * @param vaasPerVehicleNo
	 * @param timeWindowSet
	 * @param numberOfRoutingCandidates
	 * @param numberOfInsertionCandidates
	 * @param timeMultiplier
	 * @param region
	 * @param includeDriveFromStartingPosition
	 * @param expectedServiceTime
	 * @param costMultiplier
	 * @return
	 */
	public static void determinePossibleRoutingsIncludingInfeasibleWithShiftWithoutWait(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings,
			ArrayList<Pair<HashMap<Integer, ArrayList<RouteElement>>, Integer>> infeasibleRoutings,
			ArrayList<Order> acceptedOrders, HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo,
			TimeWindowSet timeWindowSet, int numberOfRoutingCandidates, int numberOfInsertionCandidates,
			double timeMultiplier, Region region, boolean includeDriveFromStartingPosition,
			double expectedServiceTime) {

		for (int routingId = 0; routingId < numberOfRoutingCandidates; routingId++) {

			ArrayList<Order> ordersToInsert = new ArrayList<Order>();
			for (Order order : acceptedOrders) {
				ordersToInsert.add(order);
			}

			HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(
					vaasPerVehicleNo.values(), timeWindowSet, timeMultiplier, region, includeDriveFromStartingPosition);

			while (ordersToInsert.size() > 0) {

				ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

				// Determine feasible insertions
				for (Order order : ordersToInsert) {
					RouteElement insertionOption = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order,
							routes, vaasPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet,
							includeDriveFromStartingPosition, true);

					if (insertionOption != null) {
						insertionOptions.add(insertionOption);
					}
				}

				if (insertionOptions.size() < 1) {

					break;
				}

				// Sort regarding value to find maximum and minimum value
				Collections.sort(insertionOptions, new RouteElementTempShiftWithoutWaitingAscComparator());

				int chosenIndex = 0;

				int borderElement = Math.min(numberOfInsertionCandidates, insertionOptions.size());

				// Choose a random number between 0 and the borderElement
				Random randomGenerator = new Random();
				chosenIndex = randomGenerator.nextInt(borderElement);

				// Insert the respective Element in the route
				RouteElement toInsert = insertionOptions.get(chosenIndex);
				DynamicRoutingHelperService.insertRouteElement(toInsert, routes, vaasPerVehicleNo, timeWindowSet,
						timeMultiplier, includeDriveFromStartingPosition);
				// Delete the respective order from the unassigned orders
				ordersToInsert.remove(insertionOptions.get(chosenIndex).getOrder());

			}
			if (ordersToInsert.size() > 0) {
				infeasibleRoutings.add(
						new Pair<HashMap<Integer, ArrayList<RouteElement>>, Integer>(routes, ordersToInsert.size()));
			} else {
				possibleRoutings.add(routes);
			}

		}

	}

	private class PossibleRoutingsWithShiftWithoutWaitRunnable implements Runnable {

		private HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routings;
		private ArrayList<Order> acceptedOrders;
		private HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo;
		private TimeWindowSet timeWindowSet;
		private int numberOfInsertionCandidates;
		private double timeMultiplier;
		private Region region;
		private boolean includeDriveFromStartingPosition;
		private double expectedServiceTime;
		private int i;

		public PossibleRoutingsWithShiftWithoutWaitRunnable(
				HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routings, ArrayList<Order> acceptedOrders,
				HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo, TimeWindowSet timeWindowSet,
				int numberOfInsertionCandidates, double timeMultiplier, Region region,
				boolean includeDriveFromStartingPosition, double expectedServiceTime, int i) {
			this.routings = routings;
			this.acceptedOrders = acceptedOrders;
			this.vaasPerVehicleNo = vaasPerVehicleNo;
			this.timeWindowSet = timeWindowSet;
			this.timeMultiplier = timeMultiplier;
			this.region = region;
			this.includeDriveFromStartingPosition = includeDriveFromStartingPosition;
			this.expectedServiceTime = expectedServiceTime;
			this.numberOfInsertionCandidates = numberOfInsertionCandidates;
			this.i = i;
		}

		public void run() {
			ArrayList<Order> ordersToInsert = new ArrayList<Order>();
			for (Order order : acceptedOrders) {
				ordersToInsert.add(order);
			}

			HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(
					vaasPerVehicleNo.values(), timeWindowSet, timeMultiplier, region, includeDriveFromStartingPosition);

			boolean allInserted = true;
			while (ordersToInsert.size() > 0) {

				ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

				// Determine feasible insertions
				for (Order order : ordersToInsert) {
					RouteElement insertionOption = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order,
							routes, vaasPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet,
							includeDriveFromStartingPosition, true);

					if (insertionOption != null) {
						insertionOptions.add(insertionOption);
					}
				}

				if (insertionOptions.size() < 1) {
					allInserted = false;
					break;
				}

				// Sort regarding travel time insertion
				Collections.sort(insertionOptions, new RouteElementTempShiftWithoutWaitingAscComparator());

				int chosenIndex = 0;

				int borderElement = Math.min(numberOfInsertionCandidates, insertionOptions.size());

				// Choose a random number between 0 and the borderElement
				Random randomGenerator = new Random();
				chosenIndex = randomGenerator.nextInt(borderElement);

				// Insert the respective Element in the route
				RouteElement toInsert = insertionOptions.get(chosenIndex);
				DynamicRoutingHelperService.insertRouteElement(toInsert, routes, vaasPerVehicleNo, timeWindowSet,
						timeMultiplier, includeDriveFromStartingPosition);
				// Delete the respective order from the unassigned orders
				ordersToInsert.remove(insertionOptions.get(chosenIndex).getOrder());

			}

			if (allInserted)
				routings.put(i, routes);

		}

	}

	public static void prepareVehicleAssignmentsForDeliveryAreas(VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea,
			HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo,
			HashMap<Integer, Double> overallCapacityPerDeliveryArea, DeliveryAreaSet deliveryAreaSet,
			TimeWindowSet timeWindowSet, double timeMultiplier, double expectedServiceTime) {

		for (DeliveryArea area : deliveryAreaSet.getElements()) {
			vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			vasPerDeliveryAreaSetAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());

			overallCapacityPerDeliveryArea.put(area.getId(), 0.0);

		}

		for (VehicleAreaAssignment ass : vehicleAreaAssignmentSet.getElements()) {
			vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			vasPerDeliveryAreaSetAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);

			double capacity = overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
			double additionalCap = Math.min(
					(ass.getEndTime() - ass.getStartTime()) * timeMultiplier + (expectedServiceTime - 1),
					(expectedServiceTime - 1) + timeWindowSet.getTempLengthOfDeliveryPeriod());
			capacity += additionalCap;
			overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);

		}
	}

	public static void determineFeasibleSchedulesAndTimeWindowsBasedOnAlgorithm1ofYang2016(OrderRequest request, ArrayList<Order> acceptedOrders,
			Region region, double timeMultiplier, TimeWindowSet timeWindowSet,
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings,
			Set<Integer> timeWindowCandidates, int numberRoutingCandidates,
			HashMap<Integer, VehicleAreaAssignment> vehiclesPerVehicleNo, boolean includeDriveFromStartingPosition, double expectedServiceTime, int numberOfThreads) {
		
		//Build schedules based on accepted orders
		DynamicRoutingHelperService.determinePossibleRoutingsFirstFeasible(possibleRoutings, acceptedOrders, 
				vehiclesPerVehicleNo, timeWindowSet, 
				numberRoutingCandidates, timeMultiplier, region, includeDriveFromStartingPosition, expectedServiceTime, numberOfThreads);
		
		//System.out.println("number of possible routings:"+ possibleRoutings.size());
		//For every time window, check if it is feasible for the order request in any of the schedules
		ArrayList<TimeWindow> notYetFeasibleTimeWindows = (ArrayList<TimeWindow>) timeWindowSet.getElements().clone();
		
		for(HashMap<Integer, ArrayList<RouteElement>> routes:possibleRoutings) {	
			
			for(TimeWindow tw: timeWindowSet.getElements()) {
				if(notYetFeasibleTimeWindows.contains(tw)) {
					
					//Check if feasible
					Order order = new Order();
					order.setOrderRequest(request);
					order.setTimeWindowFinal(tw);
					order.setId(-1);
					boolean anyFeasible = DynamicRoutingHelperService.getAnyFeasibleByOrder(order, routes, 
							vehiclesPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet, includeDriveFromStartingPosition, false);
					
					//if yes, remove from not yet feasible, add to candidates
					if(anyFeasible) {
						timeWindowCandidates.add(tw.getId());
						notYetFeasibleTimeWindows.remove(tw);
					}
				}
				
				if(notYetFeasibleTimeWindows.size()==0) break;
			}
			
			if(notYetFeasibleTimeWindows.size()==0) break;
		}
		
	}
	
	/**
	 * Determines possible routings for already accepted.
	 * 
	 * @param acceptedOrders
	 * @param vaasPerVehicleNo
	 * @param timeWindowSet
	 * @param numberOfRoutingCandidates
	 * @param numberOfInsertionCandidates
	 * @param timeMultiplier
	 * @param region
	 * @param includeDriveFromStartingPosition
	 * @param expectedServiceTime
	 * @param costMultiplier
	 * @return
	 */
	public static void determinePossibleRoutingsFirstFeasible(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> routings, ArrayList<Order> acceptedOrders,
			HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo, TimeWindowSet timeWindowSet,
			int numberOfRoutingCandidates, double timeMultiplier, Region region,
			boolean includeDriveFromStartingPosition, double expectedServiceTime, int numberOfThreads) {

		int routingId = 0;
		HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routingsBuffer = new HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>>();
		
		if(routings.size()>0) {
			for(int i=0; i< routings.size(); i++)
				routingId++;
		}
		
		if (numberOfRoutingCandidates == 0) {
			ArrayList<Thread> threads = new ArrayList<Thread>();

			Thread thread1 = new Thread(
					(new DynamicRoutingHelperService()).new PossibleRoutingsFirstFitRunnable(routingsBuffer,
							acceptedOrders, vaasPerVehicleNo, timeWindowSet, 
							timeMultiplier, region, includeDriveFromStartingPosition, expectedServiceTime, routingId));
			threads.add(thread1);
			thread1.start();

			for (Thread thread : threads) {
				try {
					thread.join();

				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		} else {
			while (routingId < numberOfRoutingCandidates) {
				ArrayList<Thread> threads = new ArrayList<Thread>();
				for (int a = 0; a < numberOfThreads; a++) {
					if (routingId < numberOfRoutingCandidates) {
						Thread thread = new Thread(
								(new DynamicRoutingHelperService()).new PossibleRoutingsFirstFitRunnable(
										routingsBuffer, acceptedOrders, vaasPerVehicleNo, timeWindowSet,
										timeMultiplier, region,
										includeDriveFromStartingPosition, expectedServiceTime, routingId));
						threads.add(thread);
						thread.start();
						routingId++;
					}
				}
				for (Thread thread : threads) {
					try {
						thread.join();

					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}

			}
		}
		Iterator<Integer> it = routingsBuffer.keySet().iterator();
		while (it.hasNext()) {
			routings.add(routingsBuffer.get(it.next()));
		}
	}
	
	
	private class PossibleRoutingsFirstFitRunnable implements Runnable {

		private HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routings;
		private ArrayList<Order> acceptedOrders;
		private HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo;
		private TimeWindowSet timeWindowSet;
		private double timeMultiplier;
		private Region region;
		private boolean includeDriveFromStartingPosition;
		private double expectedServiceTime;
		private int i;

		public PossibleRoutingsFirstFitRunnable(
				HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routings, ArrayList<Order> acceptedOrders,
				HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo, TimeWindowSet timeWindowSet,
				double timeMultiplier, Region region,
				boolean includeDriveFromStartingPosition, double expectedServiceTime, int i) {
			this.routings = routings;
			this.acceptedOrders = acceptedOrders;
			this.vaasPerVehicleNo = vaasPerVehicleNo;
			this.timeWindowSet = timeWindowSet;
			this.timeMultiplier = timeMultiplier;
			this.region = region;
			this.includeDriveFromStartingPosition = includeDriveFromStartingPosition;
			this.expectedServiceTime = expectedServiceTime;
			this.i = i;
		}

		public void run() {
			ArrayList<Order> ordersToInsert = new ArrayList<Order>();
			for (Order order : acceptedOrders) {
				ordersToInsert.add(order);
			}

			HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(
					vaasPerVehicleNo.values(), timeWindowSet, timeMultiplier, region, includeDriveFromStartingPosition);


			Random r = new Random();
			boolean allInserted = true;

			while (ordersToInsert.size() > 0) {
				
				//Determine random order
				Order order = ordersToInsert.get(r.nextInt(ordersToInsert.size()));
				
				//Check whether feasible (and insert)
				
				//Check feasible and insert
				boolean feasible = DynamicRoutingHelperService.getAnyFeasibleByOrder(order,
						routes, vaasPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet,
						includeDriveFromStartingPosition, true);
				
				//If not, break and set allInserted = false
				if(!feasible) {
					allInserted = false;
				//	System.out.println("the problematic time window is"+order.getTimeWindowFinalId());
					break;
				}
				
				//If yes, delete order from ordersToInsert and try next random order
				ordersToInsert.remove(order);
				
				

			}

			if (allInserted) {
				routings.put(i, routes);
			}
				

		}

	}
}
