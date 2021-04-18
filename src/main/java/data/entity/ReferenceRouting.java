package data.entity;

import data.utility.DataServiceProvider;

public class ReferenceRouting{
	
	private Integer deliveryAreaId;
	private DeliveryArea area;
	private Integer routingId;
	private Routing routing;
	private Integer orderSetId;
	private OrderSet orderSet;
	private Integer remainingCap;
	private Integer numberOfTheftsSpatial;
	private Integer numberOfTheftsSpatialAdvanced;
	private Integer numberOfTheftsTime;
	private Integer firstTheftsSpatial;
	private Integer firstTheftsSpatialAdvanced;
	private Integer firstTheftsTime;
	
	public Integer getDeliveryAreaId() {
		return deliveryAreaId;
	}
	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}
	public DeliveryArea getArea() {
		return area;
	}
	public void setArea(DeliveryArea area) {
		this.area = area;
	}
	
	public Integer getRoutingId() {
		return routingId;
	}
	
	public void setRoutingId(Integer routingId) {
		this.routingId = routingId;
	}
	
	public Routing getRouting() {
		if(this.routing==null){
			this.routing = DataServiceProvider.getRoutingDataServiceImplInstance().getRoutingById(this.routingId);
		}
		return routing;
	}
	
	public void setRouting(Routing routing) {
		this.routing = routing;
	}
	
	public Integer getOrderSetId() {
		return orderSetId;
	}
	
	public void setOrderSetId(Integer orderSetId) {
		this.orderSetId = orderSetId;
	}
	
	public OrderSet getOrderSet() {
		return orderSet;
	}
	
	public void setOrderSet(OrderSet orderSet) {
		this.orderSet = orderSet;
	}
	
	public Integer getRemainingCap() {
		return remainingCap;
	}
	
	public void setRemainingCap(Integer remainingCap) {
		this.remainingCap = remainingCap;
	}
	public Integer getNumberOfTheftsSpatial() {
		return numberOfTheftsSpatial;
	}
	public void setNumberOfTheftsSpatial(Integer numberOfTheftsSpatial) {
		this.numberOfTheftsSpatial = numberOfTheftsSpatial;
	}
	public Integer getNumberOfTheftsSpatialAdvanced() {
		return numberOfTheftsSpatialAdvanced;
	}
	public void setNumberOfTheftsSpatialAdvanced(Integer numberOfTheftsSpatialAdvanced) {
		this.numberOfTheftsSpatialAdvanced = numberOfTheftsSpatialAdvanced;
	}
	public Integer getNumberOfTheftsTime() {
		return numberOfTheftsTime;
	}
	public void setNumberOfTheftsTime(Integer numberOfTheftsTime) {
		this.numberOfTheftsTime = numberOfTheftsTime;
	}
	public Integer getFirstTheftsSpatial() {
		return firstTheftsSpatial;
	}
	public void setFirstTheftsSpatial(Integer firstTheftsSpatial) {
		this.firstTheftsSpatial = firstTheftsSpatial;
	}
	public Integer getFirstTheftsSpatialAdvanced() {
		return firstTheftsSpatialAdvanced;
	}
	public void setFirstTheftsSpatialAdvanced(Integer firstTheftsSpatialAdvanced) {
		this.firstTheftsSpatialAdvanced = firstTheftsSpatialAdvanced;
	}
	public Integer getFirstTheftsTime() {
		return firstTheftsTime;
	}
	public void setFirstTheftsTime(Integer firstTheftsTime) {
		this.firstTheftsTime = firstTheftsTime;
	}
	
}
