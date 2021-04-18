package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.NodeDistance;
import data.entity.RouteElement;
import logic.service.support.LocationService;
/**
*
* Distance Calculator
* @author J. Haferkamp
*
*/
public class DistanceCalculator {

	private ArrayList<NodeDistance> distances;
	private DeliveryAreaSet deliveryAreaSet; 
	private double[][] distanceAreas;
	private static double distanceMultiplierAsTheCrowFlies=1.5;
	private static double timeMultiplier=60.0;
	private Depot depot;
	
	public DistanceCalculator(ArrayList<NodeDistance> distances, Depot depot, DeliveryAreaSet deliveryAreaSet, double[][] distanceAreas) {
		
		this.distances = distances;
		this.deliveryAreaSet = deliveryAreaSet;
		this.distanceAreas = distanceAreas;
		this.depot=depot;
	}
	
	public double calculateDistance(RouteElement from, RouteElement to) {	
			
		if(from.getForecastedOrderRequest() == null && to.getForecastedOrderRequest() == null) {
			return LocationService.calculateHaversineDistanceBetweenCustomers(
					from.getOrder().getOrderRequest().getCustomer(),
					to.getOrder().getOrderRequest().getCustomer())*distanceMultiplierAsTheCrowFlies /depot.getRegion().getAverageKmPerHour() * timeMultiplier;
		} 
		else {
			return (LocationService.getDistanceBetweenAreas(distances, 
				from.getForecastedOrderRequest().getClosestNode(), 
				to.getForecastedOrderRequest().getClosestNode(), 
				deliveryAreaSet, distanceAreas));
		}
	}
}
