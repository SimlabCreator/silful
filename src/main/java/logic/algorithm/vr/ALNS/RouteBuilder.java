package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import data.entity.Customer;
import data.entity.Depot;
import data.entity.Node;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.RouteElement;
import data.entity.Vehicle;
import logic.entity.ForecastedOrderRequest;

/**
 * 
 * Start of routes, adding depot to begin and end of each route
 * @author J. Haferkamp
 *
 */

public class RouteBuilder {
	
	private Node depotNode;
	private Depot depot;
	private double earliestTimeWindowStart;

	
	public RouteBuilder(Depot depot, Node depotNode, double earliestTimeWindowStart) {
		 this.depotNode = depotNode;
		 this.depot = depot;
		 this.earliestTimeWindowStart=earliestTimeWindowStart;
	}
	
	 // create start of routes, add Depot at begin and end of routes
	public ArrayList<ArrayList<RouteElement>> createRouting(ArrayList<Vehicle> vehicles)  {
		
		ArrayList<ArrayList<RouteElement>> routing = new ArrayList<ArrayList<RouteElement>>();
		
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < vehicles.get(vehicleTypeNo).getVehicleNo(); vehicleNo++) {
			
				ArrayList<RouteElement> route = new ArrayList<RouteElement>();
				routing.add(route);
				
				RouteElement depotStart = new RouteElement();
				depotStart.setId(111111);
				depotStart.setPosition(0);
				depotStart.setTimeWindowId(11);
				depotStart.setTravelTimeFrom(0.0);
				depotStart.setTravelTimeTo(0.0);
				depotStart.setWaitingTime(0.0); 
				depotStart.setServiceBegin(earliestTimeWindowStart-100.0);
				depotStart.setServiceEnd(earliestTimeWindowStart-100.0);
				depotStart.setServiceTime(0.0);
				depotStart.setStartTimeWindow(0);
				depotStart.setEndTimeWindow(5000);			
				depotStart.setTempALNSRoute(route);	
				if (depotNode == null) {
					OrderRequest startRequest = new OrderRequest();
					Customer startCustomer = new Customer();
					startCustomer.setLat(depot.getLat());
					startCustomer.setLon(depot.getLon());
					startRequest.setCustomer(startCustomer);
					startRequest.setBasketValue(0.0);
					Order startOrder = new Order();
					startOrder.setOrderRequest(startRequest);
					depotStart.setOrder(startOrder);
				}
				else {
					ForecastedOrderRequest depotStartRequest = new ForecastedOrderRequest();
					depotStartRequest.setClosestNode(depotNode);
					depotStart.setForecastedOrderRequest(depotStartRequest);
				}
				 
				
				RouteElement depotEnd = new RouteElement();
				depotEnd.setId(999999);
				depotEnd.setTimeWindowId(10);
				depotEnd.setPosition(0);
				depotEnd.setTravelTimeFrom(0.0);
				depotEnd.setTravelTimeTo(0.0);
				depotEnd.setWaitingTime(0.0); 
				depotEnd.setServiceBegin(earliestTimeWindowStart-100.0);
				depotEnd.setServiceEnd(earliestTimeWindowStart-100.0);
				depotEnd.setServiceTime(0.0);
				depotEnd.setSlack(10000.0);
				depotEnd.setStartTimeWindow(0);
				depotEnd.setEndTimeWindow(5000.0);			
				depotEnd.setTempALNSRoute(route);	
				
				if (depotNode == null) {
					OrderRequest endRequest = new OrderRequest();
					Customer endCustomer = new Customer();
					endCustomer.setLat(depot.getLat());
					endCustomer.setLon(depot.getLon());
					endRequest.setCustomer(endCustomer);
					endRequest.setBasketValue(0.0);
					Order endOrder = new Order();
					endOrder.setOrderRequest(endRequest);
					depotEnd.setOrder(endOrder);
				}
				else {
					ForecastedOrderRequest depotEndRequest = new ForecastedOrderRequest();
					depotEndRequest.setClosestNode(depotNode);
					depotEnd.setForecastedOrderRequest(depotEndRequest);		
				}
				
				route.add(depotStart);
				route.add(depotEnd);
			} 
		}
		return (routing);
	}
}
