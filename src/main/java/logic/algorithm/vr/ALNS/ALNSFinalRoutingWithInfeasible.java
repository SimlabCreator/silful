package logic.algorithm.vr.ALNS;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.Vehicle;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.RoutingService;

/**
 *
 * @author J. Haferkamp
 *
 */

public class ALNSFinalRoutingWithInfeasible implements RoutingAlgorithm {

	private TimeWindowSet timeWindowSet;
	private Routing finalRouting;
	private OrderSet orderSet;
	private Depot depot;
	private int bestSolutionInfeasibleNo = Integer.MAX_VALUE; // Value of the
																// best solution
	private Double bestSolutionProfit = Double.MAX_VALUE;; // Value of the best
															// solution
	private double expectedServiceTime;
	private ArrayList<ArrayList<RouteElement>> bestSolution;
	private ArrayList<RouteElement> bestRequestBank;
	private ArrayList<RouteElement> routeElements = new ArrayList<RouteElement>();
	private RouteBuilder routeBuilder;
	private DistanceCalculator distanceCalculator;
	private ArrayList<Vehicle> vehicles;
	private int allowableVehicleNo;
	private boolean stopOnceFeasible;

	public ALNSFinalRoutingWithInfeasible(OrderSet orderSet, Depot depot, DeliveryAreaSet deliveryAreaSet,
			Double expectedServiceTime, Double stopOnceFeasible, ArrayList<Vehicle> vehicles) {

		this.orderSet = orderSet;
		this.vehicles = new ArrayList<Vehicle>();
		for (Vehicle v : vehicles) {
			Vehicle copy = v.copy();
			this.vehicles.add(copy);
		}

		// TODO: Adapt, bad
		this.allowableVehicleNo = this.vehicles.get(0).getVehicleNo();
		this.depot = depot;
		this.timeWindowSet = orderSet.getOrderRequestSet().getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.expectedServiceTime = expectedServiceTime;
		this.stopOnceFeasible = (stopOnceFeasible == 1.0);
		distanceCalculator = new DistanceCalculator(null, depot, deliveryAreaSet, null);
	}

	public void start() {

		double time = System.currentTimeMillis();
		timeWindowSet.sortElementsAsc();
		routeBuilder = new RouteBuilder(depot, null, timeWindowSet.getElements().get(0).getStartTime()*60.0);

		for (Order order : this.orderSet.getElements()) {
			if (order.getAccepted()) {
				RouteElement routeElement = new RouteElement();
				routeElement.setOrderId(order.getId());
				routeElement.setOrder(order);
				routeElement.setServiceTime(expectedServiceTime);
				routeElement.setTimeWindowId(order.getTimeWindowFinalId());
				routeElement.setTimeWindow(timeWindowSet.getTimeWindowById(routeElement.getTimeWindowId()));
				routeElement.setStartTimeWindow(routeElement.getTimeWindow().getStartTime() * 60.0);
				routeElement.setEndTimeWindow(routeElement.getTimeWindow().getEndTime() * 60.0);
				routeElement.setForecastedOrderRequest(null);
				routeElement.setServiceBegin(0.0);
				routeElement.setServiceEnd(0.0);
				routeElements.add(routeElement);
			}
		}

		ALNS alns = new ALNS(timeWindowSet, distanceCalculator, routeElements.size());
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, 60.0);
		ArrayList<ArrayList<RouteElement>> currentRouting;
		double performanceCurrentRouting;
		// uses 6 different route construction heuristics
		for (int i = 0; i < 3; i++) {

			// Stop if i>0 and already feasible solution
			if (stopOnceFeasible && i > 0 && this.bestRequestBank.size() == 0) {
				break;
			}
			// 1. create start solution
			currentRouting = construction(i);

			// 2. minimize routes
			//currentRouting = alns.minimizeRoutes(currentRouting, false, 10000, 2000);

//			HashMap<Integer, Integer> orderAlreadThere = new HashMap<Integer, Integer>();
//			for (int a = 0; a < currentRouting.size(); a++) {
//				for (RouteElement re : currentRouting.get(a)) {
//					if (re.getOrderId() != null) {
//						if (orderAlreadThere.containsKey(re.getOrderId())) {
//							System.out.println("Double order:" + re.getOrderId());
//							orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//						} else {
//							orderAlreadThere.put(re.getOrderId(), 1);
//						}
//					}
//				}
//			}
			
			
			// 3. if route number > available vehicles, do route minimization
			// with left-overs allowed
			ArrayList<RouteElement> currentRequestBank = new ArrayList<RouteElement>();
			if (currentRouting.size() > this.allowableVehicleNo) {
				Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> result = alns.minimizeRoutes(
						currentRouting, currentRequestBank, this.allowableVehicleNo, false, 5000, 1000);
				currentRouting = result.getKey();
				currentRequestBank = result.getValue();
			}
			
//			orderAlreadThere = new HashMap<Integer, Integer>();
//			for (int a = 0; a < currentRouting.size(); a++) {
//				for (RouteElement re : currentRouting.get(a)) {
//					if (re.getOrderId() != null) {
//						if (orderAlreadThere.containsKey(re.getOrderId())) {
//							System.out.println("Double order:" + re.getOrderId());
//							orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//						} else {
//							orderAlreadThere.put(re.getOrderId(), 1);
//						}
//					}
//				}
//			}
//			for(RouteElement re: currentRequestBank){
//				if (re.getOrderId() != null) {
//					if (orderAlreadThere.containsKey(re.getOrderId())) {
//						System.out.println("Double order:" + re.getOrderId());
//						orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//					} else {
//						orderAlreadThere.put(re.getOrderId(), 1);
//					}
//				}
//			}

			// 3. minimize travel time
			if (currentRequestBank.size() <= this.bestSolutionInfeasibleNo) {
				performanceCurrentRouting = this.bestSolutionProfit;
				if (!stopOnceFeasible) {
					Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> result = alns
							.minimizeTravelTimes(currentRouting, currentRequestBank, true, 10000, 2000);
					currentRouting = result.getKey();
					currentRequestBank = result.getValue();
					performanceCurrentRouting = alns.performance(currentRouting)
							+ alns.requestBankCosts(currentRequestBank);
				} else {
					performanceCurrentRouting = alns.performance(currentRouting)
							+ alns.requestBankCosts(currentRequestBank);
				}

//				orderAlreadThere = new HashMap<Integer, Integer>();
//				for (int a = 0; a < currentRouting.size(); a++) {
//					for (RouteElement re : currentRouting.get(a)) {
//						if (re.getOrderId() != null) {
//							if (orderAlreadThere.containsKey(re.getOrderId())) {
//								System.out.println("Double order:" + re.getOrderId());
//								orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//							} else {
//								orderAlreadThere.put(re.getOrderId(), 1);
//							}
//						}
//					}
//				}
//				for(RouteElement re: currentRequestBank){
//					if (re.getOrderId() != null) {
//						if (orderAlreadThere.containsKey(re.getOrderId())) {
//							System.out.println("Double order:" + re.getOrderId());
//							orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//						} else {
//							orderAlreadThere.put(re.getOrderId(), 1);
//						}
//					}
//				}
				
				if (currentRequestBank.size() <= this.bestSolutionInfeasibleNo
						&& performanceCurrentRouting < this.bestSolutionProfit) {
					this.bestSolution = currentRouting;
					this.bestRequestBank = currentRequestBank;
					this.bestSolutionProfit = performanceCurrentRouting;
					this.bestSolutionInfeasibleNo = bestRequestBank.size();

				}
			}
		}

		printPerformance(bestSolution, bestRequestBank, System.currentTimeMillis() - time);
		if(bestRequestBank.size()>0) {
			System.out.println("Number of infeasible: "+bestRequestBank.size());
		}
		//printRoute(bestSolution, bestRequestBank);
//		HashMap<Integer, Integer> orderAlreadThere = new HashMap<Integer, Integer>();
//		for (int i = 0; i < bestSolution.size(); i++) {
//			for (RouteElement re : bestSolution.get(i)) {
//				if (re.getOrderId() != null) {
//					if (orderAlreadThere.containsKey(re.getOrderId())) {
//						System.out.println("Double order:" + re.getOrderId());
//						orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//					} else {
//						orderAlreadThere.put(re.getOrderId(), 1);
//					}
//				}
//			}
//		}
//		
//		for(RouteElement re: bestRequestBank){
//			if (re.getOrderId() != null) {
//				if (orderAlreadThere.containsKey(re.getOrderId())) {
//					System.out.println("Double order:" + re.getOrderId());
//					orderAlreadThere.put(re.getOrderId(), orderAlreadThere.get(re.getOrderId()) + 1);
//				} else {
//					orderAlreadThere.put(re.getOrderId(), 1);
//				}
//			}
//		}

		// transfers results
		this.finalRouting = new Routing();
		this.finalRouting.setDepot(depot);
		this.finalRouting.setDepotId(depot.getId());
		this.finalRouting.setTimeWindowSet(timeWindowSet);
		this.finalRouting.setTimeWindowSetId(timeWindowSet.getId());
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setOrderSet(orderSet);
		this.finalRouting.setOrderSetId(orderSet.getId());

		ArrayList<RouteElement> currentRoute;
		RouteElement currentElement;
		ArrayList<Route> routesFinal = new ArrayList<Route>();
		for (int i = 0; i < bestSolution.size(); i++) {
			currentRoute = bestSolution.get(i);
			if (currentRoute.size() > 2) {
				Route newRoute = new Route();
				newRoute.setId(i);
				newRoute.setRouting(finalRouting);
				finalRouting.addStartRoutingElement(newRoute);
				for (int j = 1; j < currentRoute.size() - 1; j++) {
					currentElement = currentRoute.get(j);
					currentElement.setPosition(j);
					currentElement.setTravelTime(currentElement.getTravelTimeTo());
					newRoute.addRoutingElement(currentRoute.get(j));
				}
				routesFinal.add(newRoute);
			}
		}
		this.finalRouting.setRoutes(routesFinal);
	}

	public Routing getResult() {
		return this.finalRouting;
	}

	// creates a first solution
	public ArrayList<ArrayList<RouteElement>> construction(int insertionHeuristic) {
		ArrayList<ArrayList<RouteElement>> routing;
		ArrayList<RouteElement> currentElements = new ArrayList<RouteElement>();

		while (true) {
			routing = routeBuilder.createRouting(vehicles);
			currentElements.addAll(routeElements);
			InsertionHeuristics insertionHeuristics = new InsertionHeuristics(distanceCalculator, timeWindowSet);
			routing = insertionHeuristics.insertElements(insertionHeuristic, routing, currentElements, true);
			if (routing == null) {
				currentElements.clear();
				vehicles.get(0).setVehicleNo(vehicles.get(0).getVehicleNo() + 1);
			} else
				break;
		}
		return (routing);
	}

	// computes and prints performance
	public static void printPerformance(ArrayList<ArrayList<RouteElement>> routes, double runningTime) {
		double overallTravelTime = 0;
		int numberOforders = 0;
		for (ArrayList<RouteElement> currentRoute : routes) {
			numberOforders = numberOforders + currentRoute.size() - 2;
			for (RouteElement currentOrder : currentRoute) {
				overallTravelTime = overallTravelTime + currentOrder.getTravelTimeTo();
			}
		}

		System.out.println("Number of routes: " + routes.size());
		System.out.println("Number of orders: " + numberOforders);
		System.out.println("Travel time: " + overallTravelTime);
		System.out.println("Running time: " + runningTime);
	}

	// computes and prints performance
	public static void printPerformance(ArrayList<ArrayList<RouteElement>> routes, ArrayList<RouteElement> requestBank,
			double runningTime) {
		double overallTravelTime = 0;
		int numberOforders = 0;
		for (ArrayList<RouteElement> currentRoute : routes) {
			numberOforders = numberOforders + currentRoute.size() - 2;
			for (RouteElement currentOrder : currentRoute) {
				overallTravelTime = overallTravelTime + currentOrder.getTravelTimeTo();
			}
		}

		System.out.println("Number of routes: " + routes.size());
		System.out.println("Number of feasible orders: " + numberOforders);
		System.out.println("Number of infeasible orders: " + requestBank.size());
		System.out.println("Travel time: " + overallTravelTime);
		System.out.println("Running time: " + runningTime);
	}

	// prints routes
		public static void printRoute(ArrayList<ArrayList<RouteElement>> routes, ArrayList<RouteElement> requestBank) {

			System.out.println("");
			for (int route = 0; route < routes.size(); route++) {

				System.out.println("");
				System.out.println("Route " + route);

				for (int c = 0; c < routes.get(route).size(); c++) {
					System.out.println(

							routes.get(route).get(c).getOrderId() + "     ;W;   " + (routes.get(route).get(c).getWaitingTime())
									+ "     ;To;    " + (routes.get(route).get(c).getTravelTimeTo()) + "     ;B;   "
									+ (routes.get(route).get(c).getServiceBegin()) + "     ;E;   "
									+ (routes.get(route).get(c).getServiceEnd()) + "     ;From;    "
									+ (routes.get(route).get(c).getTravelTimeFrom()) + "     ;S;    "
									+ (routes.get(route).get(c).getSlack()) + "     ;TW;   "+routes.get(route).get(c).getOrder().getTimeWindowFinalId()+", "
									+ (routes.get(route).get(c).getStartTimeWindow()) + "-"
									+ (routes.get(route).get(c).getEndTimeWindow()));
				}
			}
			
			for(RouteElement re: requestBank) {
				System.out.print("Left over: "+re.getOrderId()+"_"+re.getOrder().getTimeWindowFinalId());
			}

		}
}
