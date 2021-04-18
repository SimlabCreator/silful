package logic.algorithm.vr.capacity;

import java.util.ArrayList;

import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import logic.service.support.LocationService;

/**
 * Obtains capacities from initial routing
 * @author M. Lang
 *
 */
public class CapacityAggregation implements CapacityAlgorithm{
	
	private DeliveryAreaSet deliveryAreaSet;
	private Routing routing;
	private CapacitySet capacitySet;
	
	public CapacityAggregation(Routing routing, DeliveryAreaSet deliveryAreaSet){
		this.routing=routing;
		this.deliveryAreaSet = deliveryAreaSet;
	}

	public void start() {
		
		this.capacitySet=new CapacitySet();
		this.capacitySet.setDeliveryAreaSet(this.deliveryAreaSet);
		this.capacitySet.setDeliveryAreaSetId(this.deliveryAreaSet.getId());
		this.capacitySet.setTimeWindowSet(routing.getTimeWindowSet());
		this.capacitySet.setTimeWindowSetId(routing.getTimeWindowSetId());
		this.capacitySet.setRouting(routing);
		this.capacitySet.setRoutingId(routing.getId());
		
		//Assign route elements to delivery areas
		for(int routeID=0; routeID < this.routing.getRoutes().size(); routeID++){
			Route route =  this.routing.getRoutes().get(routeID);
			for(int routeElementID=0; routeElementID < route.getRouteElements().size(); routeElementID++){
				RouteElement element = route.getRouteElements().get(routeElementID);
				DeliveryArea area;
				if(this.routing.isPossiblyFinalRouting()){
					area=LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet, element.getOrder().getOrderRequest().getCustomer());
				}else{
					area=LocationService.assignLocationToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet, element.getDeliveryArea().getCenterLat(), element.getDeliveryArea().getCenterLon());
				}
				element.setDeliveryArea(area);
				element.setDeliveryAreaId(area.getId());
			}
		}
		
		//A capacity per time window and delivery area
		ArrayList<TimeWindow> timeWindows = routing.getTimeWindowSet().getElements();
		ArrayList<DeliveryArea> deliveryAreas;
		
		if(deliveryAreaSet.isHierarchy()){
			deliveryAreas= new ArrayList<DeliveryArea>();
			for(DeliveryArea area: deliveryAreaSet.getElements()){

					deliveryAreas.addAll(area.getSubset().getElements());
			}
		}else{
			deliveryAreas= deliveryAreaSet.getElements();
		}
		
		ArrayList<Capacity> capacities = new ArrayList<Capacity>();
		for(int twID=0; twID < timeWindows.size(); twID++){
			for(int daID=0; daID < deliveryAreas.size(); daID++){
				Capacity capacity = new Capacity();
				capacity.setDeliveryArea(deliveryAreas.get(daID));
				capacity.setDeliveryAreaId( deliveryAreas.get(daID).getId());
				capacity.setTimeWindow(timeWindows.get(twID));
				capacity.setTimeWindowId((timeWindows.get(twID)).getId());
				int capacityNumber=0;
				
				///Go through routing elements and assign
				for(int routeID=0; routeID < this.routing.getRoutes().size(); routeID++){
					Route route =  this.routing.getRoutes().get(routeID);
					for(int routeElementID=0; routeElementID < route.getRouteElements().size(); routeElementID++){
						RouteElement element = route.getRouteElements().get(routeElementID);
						if((element.getDeliveryAreaId()==capacity.getDeliveryAreaId()) &&(element.getTimeWindowId()==capacity.getTimeWindowId())){
							capacityNumber++;
						}
					}
				}
				
				capacity.setCapacityNumber(capacityNumber);
				
				//Add capacity to the list
				capacities.add(capacity);
			}
		}
		
		//Add capacity list to the set
		this.capacitySet.setElements(capacities);
	}


	public CapacitySet getResult() {
		return capacitySet;
	}

	public ArrayList<String> getParameterRequest() {
		
		return new ArrayList<String>();
	}

	
}
