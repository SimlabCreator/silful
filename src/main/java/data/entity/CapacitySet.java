package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class CapacitySet extends SetEntity{
	
	
	
	private String name;
	
	private ArrayList<Capacity> capacities;
	private Integer routingId;
	private Routing routing;
	private Integer timeWindowSetId;
	private TimeWindowSet timeWindowSet;
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private double weight; 


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	@Override
	public ArrayList<Capacity> getElements() {
		if(this.capacities==null){
			this.capacities= DataServiceProvider.getCapacityDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.capacities;
	}

	public void setElements(ArrayList<Capacity> elements) {
		this.capacities= elements;
		
	}


	@Override
	public String toString() {
		
		return id+"; "+name;
	}

	public Integer getTimeWindowSetId() {
		return timeWindowSetId;
	}

	public void setTimeWindowSetId(Integer timeWindowSetId) {
		this.timeWindowSetId = timeWindowSetId;
	}

	public TimeWindowSet getTimeWindowSet() {
		
		if(this.timeWindowSet==null){
			this.timeWindowSet=(TimeWindowSet) DataServiceProvider.getTimeWindowDataServiceImplInstance().getSetById(this.timeWindowSetId);
		}
		return timeWindowSet;
	}

	public void setTimeWindowSet(TimeWindowSet timeWindowSet) {
		this.timeWindowSet = timeWindowSet;
	}

	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}

	public void setDeliveryAreaSetId(Integer deliveryAreaSetId) {
		this.deliveryAreaSetId = deliveryAreaSetId;
	}

	public DeliveryAreaSet getDeliveryAreaSet() {
		if(this.deliveryAreaSet==null){
			this.deliveryAreaSet=(DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getSetById(this.deliveryAreaSetId);
		}
		return deliveryAreaSet;
	}

	public void setDeliveryAreaSet(DeliveryAreaSet deliveryAreaSet) {
		this.deliveryAreaSet = deliveryAreaSet;
	}

	public Integer getRoutingId() {
		return routingId;
	}

	public void setRoutingId(Integer routingId) {
		this.routingId = routingId;
	}

	public Routing getRouting() {
		if(this.routing==null){
			this.routing =  DataServiceProvider.getRoutingDataServiceImplInstance().getRoutingById(this.routingId);
		}
		return routing;
	}

	public void setRouting(Routing routing) {
		this.routing = routing;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
		
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof CapacitySet){
		   CapacitySet other = (CapacitySet) o;
	       return (this.id == other.getId() && this.timeWindowSetId==other.getTimeWindowSetId() && this.deliveryAreaSetId==other.getDeliveryAreaSetId() && this.routingId==other.getRoutingId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
		if(this.id!=0) return this.id;
		int timeWindowSetIdInt = this.timeWindowSetId;
		int deliveryAreaSetIdInt = this.deliveryAreaSetId;
		int routingIdInt = this.routingId;
	   return timeWindowSetIdInt+deliveryAreaSetIdInt+routingIdInt;
	}

}
