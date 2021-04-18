package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ExpectedDeliveryTimeConsumptionSet extends SetEntity{
	
	
	
	private String name;
	
	private ArrayList<ExpectedDeliveryTimeConsumption> consumptions;
	private Integer routingId;
	private Routing routing;
	private Integer timeWindowSetId;
	private TimeWindowSet timeWindowSet;
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	@Override
	public ArrayList<ExpectedDeliveryTimeConsumption> getElements() {
		if(this.consumptions==null){
			this.consumptions= DataServiceProvider.getExpectedDeliveryTimeConsumptionDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.consumptions;
	}

	public void setElements(ArrayList<ExpectedDeliveryTimeConsumption> elements) {
		this.consumptions= elements;
		
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
		

}
