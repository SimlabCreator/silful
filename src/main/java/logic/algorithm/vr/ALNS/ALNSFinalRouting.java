package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import data.entity.Depot;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.Vehicle;
import logic.algorithm.vr.RoutingAlgorithm;


/**
*
* @author J. Haferkamp
*
*/

public class ALNSFinalRouting implements RoutingAlgorithm {

	private TimeWindowSet timeWindowSet;
	private Routing finalRouting;
	private OrderSet orderSet;
	private Depot depot;
	private int bestSolutionInfeasibleNo = Integer.MAX_VALUE;  // Value of the best solution
	private Double bestSolutionProfit = Double.MAX_VALUE;; // Value of the best solution
	private double expectedServiceTime;
	private ArrayList<ArrayList<RouteElement>> bestSolution;
	private ArrayList<RouteElement> routeElements = new ArrayList<RouteElement>();
	private RouteBuilder routeBuilder;
	private DistanceCalculator distanceCalculator;
	private ArrayList<Vehicle> vehicles;

	public ALNSFinalRouting(OrderSet orderSet,  Depot depot, Double expectedServiceTime, ArrayList<Vehicle> vehicles) {
		
		this.orderSet = orderSet;
		this.vehicles = vehicles;
		this.depot = depot;
		this.timeWindowSet = orderSet.getOrderRequestSet().getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.expectedServiceTime = expectedServiceTime;	
		distanceCalculator = new DistanceCalculator(null, depot, null, null);
	}

	
	public void start() {
	
		double time = System.currentTimeMillis();
		timeWindowSet.sortElementsAsc();
		routeBuilder = new RouteBuilder(depot, null, timeWindowSet.getElements().get(0).getStartTime()*60.0);
		
		for (Order order : this.orderSet.getElements()) {
			if (order.getAccepted()) {
				RouteElement routeElement = new RouteElement();
				routeElement.setId(order.getId());
				routeElement.setOrder(order);
				routeElement.setServiceTime(expectedServiceTime);
				routeElement.setTimeWindowId(order.getTimeWindowFinalId());
				routeElement.setTimeWindow(timeWindowSet.getTimeWindowById(routeElement.getTimeWindowId()));
				routeElement.setStartTimeWindow(routeElement.getTimeWindow().getStartTime()*60);
				routeElement.setEndTimeWindow(routeElement.getTimeWindow().getEndTime()*60);
				routeElement.setForecastedOrderRequest(null);
				routeElement.setServiceBegin(0.0);
				routeElement.setServiceEnd(0.0);
				routeElements.add(routeElement);
			}
		}
		ALNS alns = new ALNS(timeWindowSet, distanceCalculator, routeElements.size());		
		ArrayList<ArrayList<RouteElement>> currentRouting; 
		double performanceCurrentRouting;
		// uses 6 different route construction heuristics 
		for (int i = 0; i < 6; i++)   {
			 
			// 1. create start solution
			currentRouting = construction(i);
			
			// 2. minimize routes
			currentRouting = alns.minimizeRoutes(currentRouting, false, 5000, 1000);
			
			// 3. minimize travel time 
			if (currentRouting.size() <= bestSolutionInfeasibleNo) {
				currentRouting = alns.minimizeTravelTimes(currentRouting, true, 1000, 100);	
				performanceCurrentRouting = alns.performance(currentRouting);
				if (performanceCurrentRouting < bestSolutionProfit || currentRouting.size() < bestSolutionInfeasibleNo) {
					bestSolution = currentRouting;
					bestSolutionProfit = performanceCurrentRouting;
					bestSolutionInfeasibleNo = bestSolution.size();
				}	
			}
		}
				
		printPerformance(bestSolution, System.currentTimeMillis() - time);
		
		// transfers results 	
		this.finalRouting = new Routing();
		this.finalRouting.setDepot(depot);
		this.finalRouting.setDepotId(depot.getId());
		this.finalRouting.setTimeWindowSet(timeWindowSet);
		this.finalRouting.setTimeWindowSetId(timeWindowSet.getId());
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setOrderSet(orderSet);
		
		ArrayList<RouteElement> currentRoute;
		RouteElement currentElement;
		for (int i = 0; i< bestSolution.size(); i++) {
			currentRoute = bestSolution.get(i);
			if (currentRoute.size() > 2) {
				Route newRoute = new Route();
				newRoute.setId(i);
				newRoute.setRouting(finalRouting);
				finalRouting.addStartRoutingElement(newRoute); 
				for (int j = 0; j < currentRoute.size(); j++) {	
					currentElement = currentRoute.get(j);
					currentElement.setPosition(j);
					currentElement.setTravelTime(currentElement.getTravelTimeTo() + currentElement.getTravelTimeFrom());
					newRoute.addRoutingElement(currentRoute.get(j));
				}
			}
		}
	}
	
	public Routing getResult() {
		return this.finalRouting;
	}
	
	// creates a first solution
	public ArrayList<ArrayList<RouteElement>> construction(int insertionHeuristic) {		
		ArrayList<ArrayList<RouteElement>> routing;	
		ArrayList<RouteElement> currentElements = new ArrayList<RouteElement>();
		
		while(true) { 
			routing = routeBuilder.createRouting(vehicles);
			currentElements.addAll(routeElements);
			InsertionHeuristics insertionHeuristics = new InsertionHeuristics(distanceCalculator, timeWindowSet);
			routing = insertionHeuristics.insertElements(insertionHeuristic, routing, currentElements, true);
			if (routing == null) {
				currentElements.clear();
				vehicles.get(0).setVehicleNo(vehicles.get(0).getVehicleNo() + 1);
			} else break;
		}
		return(routing); 
	}
	
	// computes and prints performance 
	public static void printPerformance(ArrayList<ArrayList<RouteElement>> routes, double runningTime) {
		double overallTravelTime = 0;
		int numberOforders = 0;
		for (ArrayList<RouteElement> currentRoute : routes) { 
			numberOforders = numberOforders + currentRoute.size()-2; 
			for(RouteElement currentOrder : currentRoute) {
				overallTravelTime = overallTravelTime + currentOrder.getTravelTimeTo();	
			}
		}
		
		System.out.println("Number of routes: " + routes.size());
		System.out.println("Number of orders: " + numberOforders);
		System.out.println("Travel time: " + overallTravelTime);
		System.out.println("Running time: " + runningTime);
	}
	
	// prints routes 
	public static void printRoute(ArrayList<ArrayList<RouteElement>> routes) {

		System.out.println("");
		for (int route = 0; route < routes.size(); route++) {

			System.out.println("");
			System.out.println("Route " + route);

			for (int c = 0; c < routes.get(route).size(); c++) {
				System.out.println(

						routes.get(route).get(c).getId() + "     ;W;   " + (routes.get(route).get(c).getWaitingTime()) + "     ;To;    " + (routes.get(route).get(c)
								.getTravelTimeTo()) + "     ;B;   " + (routes.get(route).get(c).getServiceBegin()) + "     ;E;   " + (routes.get(route).get(c).getServiceEnd()) + "     ;From;    " + (routes.get(
										route).get(c).getTravelTimeFrom()) + "     ;S;    " + (routes.get(route).get(c).getSlack()) + "     ;TW;   " + (routes.get(route).get(c).getStartTimeWindow()) + "-" + (routes
												.get(route).get(c).getEndTimeWindow()));
			}
		}

	}
}
