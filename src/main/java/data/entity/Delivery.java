package data.entity;

import data.utility.DataServiceProvider;

public class Delivery extends Entity {

	private Integer id;
	private Integer setId;
	private Integer routeElementId;
	private RouteElement routeElement;
	private Integer travelTime;
	private Double arrivalTime;
	private Integer waitingTime;
	private Double serviceBegin;
	private Integer serviceTime;
	private Integer bufferBefore;

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getSetId() {
		return setId;
	}

	public void setSetId(Integer setId) {
		this.setId = setId;
	}

	public Integer getRouteElementId() {
		return routeElementId;
	}

	public void setRouteElementId(Integer routeElementId) {
		this.routeElementId = routeElementId;
	}

	public Integer getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(Integer travelTime) {
		this.travelTime = travelTime;
	}

	public Double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public Integer getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(Integer waitingTime) {
		this.waitingTime = waitingTime;
	}

	public Double getServiceBegin() {
		return serviceBegin;
	}

	public void setServiceBegin(Double serviceBegin) {
		this.serviceBegin = serviceBegin;
	}

	public Integer getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(Integer serviceTime) {
		this.serviceTime = serviceTime;
	}

	public Integer getBufferBefore() {
		return bufferBefore;
	}

	public void setBufferBefore(Integer bufferBefore) {
		this.bufferBefore = bufferBefore;
	}

	public RouteElement getRouteElement() {

		if (this.routeElement == null) {
			this.routeElement = DataServiceProvider.getRoutingDataServiceImplInstance()
					.getRouteElementById(this.routeElementId);
		}
		return routeElement;
	}

	public void setRouteElement(RouteElement routeElement) {
		this.routeElement = routeElement;
	}

}
