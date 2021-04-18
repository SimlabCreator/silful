package data.entity;


import java.util.ArrayList;

import data.utility.DataServiceProvider;
import logic.algorithm.vr.ALNS.InsertionPosition;
import logic.entity.ForecastedOrderRequest;

public class RouteElement extends Entity{

	private Integer id;
	private Integer routeId;
	private Route route;
	private Integer position; //position in the route, number
	private Integer timeWindowId;
	private TimeWindow timeWindow;
	private double startTimeWindow; //start of time window in minutes
	private double endTimeWindow; //end of time window in minutes
	private double tempValue; //Only relevant for temp decisions
	private double tempAdditionalCostsValue; //Only relevant for temp decisions
	private Integer deliveryAreaId; //Only relevant for temp decisions
	private DeliveryArea deliveryArea; 
	private ForecastedOrderRequest forecastedOrderRequest; //Temp variable for initial routing
	private Integer tempRoute; //Temp variable for inbetween routing
	private Integer tempPosition; //Temp variable for inbetween routing
	private double tempShift; //Temp variable for shift for insertion
	private double tempShiftWithoutWait; //Temp variable for shift for insertion without waiting time
	private int tempRoutingId; //Temp variable to assign to overall routing
	private Routing tempRouting; 
	private double tempCheapestInsertionValue; //Temp variable for cheapest insertion
	private Integer tempCurrentlyInOtherRoute; //Temp if route element is already inserted
	private Integer tempCurrentlyInOtherRoutePosition; //Temp if route element is already inserted, this is the position
	private boolean tempAlreadyAccepted;
	private double tempSpaceAroundStart;
	private double tempSpaceAroundEnd;
	private double tempNeihborhoodValue;
	private Double tempShiftWithDepotDistance;
	private Double tempSlack;
	private Double tempBufferToNext;
	private Integer orderId; //Only relevant for final routing
	private Order order;
	private Double travelTime; 
	private Double travelTimeTo;  // travel time to location
	private Double travelTimeFrom; // travel time from location
	private Double waitingTime;
	private Double serviceBegin;
	private Double serviceTime;
	private Double slack;
	private double score; //don't use, use estimated value instead
	private double estimatedValue;

	
	// New for ALNS //
	private Double serviceEnd;
	private Double tempHistoricalTravelTime = Double.MAX_VALUE;
	private ArrayList<InsertionPosition> tempInsertionPositions; //All possible insertion position, only short-term relevant
	private Double tempRegretValue = 0.0;  //only short-term relevant
	private ArrayList<RouteElement> tempALNSRoute;
	// End //
	
	public double getScore(){
		return score;
	}
	
	public void setScore(Double score){
		this.score = score;
	}
	
	public double getEstimatedValue(){
		return estimatedValue;
	}
	
	public void setEstimatedValue(Double estimatedValue){
		this.estimatedValue = estimatedValue;
	}
	
	
//	public long getClosestNodeId() {
//		return closestNodeId;
//	}
//	public void setClosestNodeId(long l) {
//		this.closestNodeId = l;
//	}
	
	
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getRouteId() {
		return routeId;
	}
	public void setRouteId(Integer routeId) {
		this.routeId = routeId;
	}
	public Route getRoute() {
		if(this.route==null){
			this.route = DataServiceProvider.getRoutingDataServiceImplInstance().getRouteById(this.routeId);
		}
		return route;
	}
	public void setRoute(Route route) {
		this.route = route;
	}
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	public Integer getTimeWindowId() {
		return timeWindowId;
	}
	public void setTimeWindowId(Integer timeWindowId) {
		this.timeWindowId = timeWindowId;
	}
	public TimeWindow getTimeWindow() {
		
		if(this.timeWindow==null){
			this.timeWindow=DataServiceProvider.getTimeWindowDataServiceImplInstance().getElementById(this.timeWindowId);
		}
		return timeWindow;
	}
	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}
	public Integer getDeliveryAreaId() {
		return deliveryAreaId;
	}
	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}
	public DeliveryArea getDeliveryArea() {
		
		if(this.deliveryArea==null){
			this.deliveryArea= DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getElementById(this.deliveryAreaId);
		}
		return deliveryArea;
	}
	public void setDeliveryArea(DeliveryArea deliveryArea) {
		this.deliveryArea = deliveryArea;
	}
	public Integer getOrderId() {
		return orderId;
	}
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	public Order getOrder() {
		
		if(this.order==null){
			this.order = DataServiceProvider.getOrderDataServiceImplInstance().getElementById(this.orderId);
		}
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	
	
	public Double getTravelTime() {
		return travelTime;
	}
	public void setTravelTime(Double travelTime) {
		this.travelTime = travelTime;
	}
	
	
	public Double getTravelTimeTo() {
		return travelTimeTo;
	}
	public void setTravelTimeTo(Double travelTimeTo) {
		this.travelTimeTo = travelTimeTo;
	}
	
	public Double getTravelTimeFrom() {
		return travelTimeFrom;
	}
	public void setTravelTimeFrom(Double travelTimeFrom) {
		this.travelTimeFrom = travelTimeFrom;
	}

	public Double getWaitingTime() {
		return waitingTime;
	}
	public void setWaitingTime(Double waitingTime) {
		this.waitingTime = waitingTime;
	}
	public Double getServiceBegin() {
		return serviceBegin;
	}
	public void setServiceBegin(Double serviceBegin) {
		this.serviceBegin = serviceBegin;
	}
	public Double getServiceTime() {
		return serviceTime;
	}
	public void setServiceTime(Double serviceTime) {
		this.serviceTime = serviceTime;
	}
	public Double getSlack() {
		return slack;
	}
	public void setSlack(Double slack) {
		this.slack = slack;
	}
	public ForecastedOrderRequest getForecastedOrderRequest() {
		return forecastedOrderRequest;
	}
	public void setForecastedOrderRequest(ForecastedOrderRequest forecastedOrderRequest) {
		this.forecastedOrderRequest = forecastedOrderRequest;
	}
	
	
	public double getStartTimeWindow() {
		return startTimeWindow;
	}
	public void setStartTimeWindow(double startTimeWindow) {
		this.startTimeWindow = startTimeWindow;
	}
	
	public double getEndTimeWindow() {
		return endTimeWindow;
	}
	public void setEndTimeWindow(double endTimeWindow) {
		this.endTimeWindow = endTimeWindow;
	}
	

	
	
	/**
	 * Produces a copy of the respective RouteElement
	 * !Only ids of referenced objects are copied!
	 * @param toCopy
	 * @return
	 */
	public RouteElement copyElement(){
		//TODO: Nicht primitven Datentypen clonen
		RouteElement newElement = new RouteElement();
		newElement.setDeliveryAreaId(deliveryAreaId);
		newElement.setId(id);
		newElement.setForecastedOrderRequest(forecastedOrderRequest);
		newElement.setOrder(order);
		newElement.setOrderId(orderId);
		newElement.setPosition(position);
		newElement.setRouteId(routeId);
		newElement.setServiceBegin(serviceBegin);
		newElement.setServiceEnd(serviceEnd);
		newElement.setWaitingTime(waitingTime);
		newElement.setServiceTime(serviceTime);
		newElement.setSlack(slack);
		newElement.setTimeWindow(timeWindow);
		newElement.setTimeWindowId(timeWindowId);
		newElement.setTravelTimeFrom(travelTimeFrom);
		newElement.setTravelTimeTo(travelTimeTo);
		newElement.setTravelTime(travelTime);
		newElement.setTempPosition(tempPosition);
		newElement.setTempRoute(tempRoute);
		newElement.setTempCheapestInsertionValue(tempCheapestInsertionValue);
		newElement.setTempShift(tempShift);
		newElement.setEstimatedValue(estimatedValue);
		newElement.setStartTimeWindow(startTimeWindow);
		newElement.setEndTimeWindow(endTimeWindow);
		newElement.setTempSlack(tempSlack);
		newElement.setTempAdditionalCostsValue(tempAdditionalCostsValue);
		return newElement;
	}
	public Integer getTempRoute() {
		return tempRoute;
	}
	public void setTempRoute(Integer tempRoute) {
		this.tempRoute = tempRoute;
	}
	public double getTempShift() {
		return tempShift;
	}
	public void setTempShift(double tempShift) {
		this.tempShift = tempShift;
	}
	public Integer getTempPosition() {
		return tempPosition;
	}
	public void setTempPosition(Integer tempPosition) {
		this.tempPosition = tempPosition;
	}
	public double getTempCheapestInsertionValue() {
		return tempCheapestInsertionValue;
	}
	public void setTempCheapestInsertionValue(double tempCheapestInsertionValue) {
		this.tempCheapestInsertionValue = tempCheapestInsertionValue;
	}
	public Integer getTempCurrentlyInOtherRoute() {
		return tempCurrentlyInOtherRoute;
	}
	public void setTempCurrentlyInOtherRoute(Integer currentlyInOtherRoute) {
		this.tempCurrentlyInOtherRoute = currentlyInOtherRoute;
	}
	public Integer getTempCurrentlyInOtherRoutePosition() {
		return tempCurrentlyInOtherRoutePosition;
	}
	public void setTempCurrentlyInOtherRoutePosition(Integer tempCurrentlyInOtherRoutePosition) {
		this.tempCurrentlyInOtherRoutePosition = tempCurrentlyInOtherRoutePosition;
	}
	public double getTempSpaceAroundStart() {
		return tempSpaceAroundStart;
	}
	public void setTempSpaceAroundStart(double tempSpaceAroundStart) {
		this.tempSpaceAroundStart = tempSpaceAroundStart;
	}
	public double getTempSpaceAroundEnd() {
		return tempSpaceAroundEnd;
	}
	public void setTempSpaceAroundEnd(double tempSpaceAroundEnd) {
		this.tempSpaceAroundEnd = tempSpaceAroundEnd;
	}
	public double getTempNeihborhoodValue() {
		return tempNeihborhoodValue;
	}
	public void setTempNeighborhoodValue(double tempNeihborhoodValue) {
		this.tempNeihborhoodValue = tempNeihborhoodValue;
	}

	public double getTempShiftWithoutWait() {
		return tempShiftWithoutWait;
	}

	public void setTempShiftWithoutWait(double tempShiftWithoutWait) {
		this.tempShiftWithoutWait = tempShiftWithoutWait;
	}

	public int getTempRoutingId() {
		return tempRoutingId;
	}

	public void setTempRoutingId(int tempRoutingId) {
		this.tempRoutingId = tempRoutingId;
	}

	public boolean isTempAlreadyAccepted() {
		return tempAlreadyAccepted;
	}

	public void setTempAlreadyAccepted(boolean tempAlreadyAccepted) {
		this.tempAlreadyAccepted = tempAlreadyAccepted;
	}

	public double getTempAdditionalCostsValue() {
		return tempAdditionalCostsValue;
	}

	public void setTempAdditionalCostsValue(double tempAdditionalCostsValue) {
		this.tempAdditionalCostsValue = tempAdditionalCostsValue;
	}

	public Routing getTempRouting() {
		return tempRouting;
	}

	public void setTempRouting(Routing tempRouting) {
		this.tempRouting = tempRouting;
	}
	
	// New for ALNS //
	// updates element after change in route  	
	public boolean updateElement(RouteElement previousElement, boolean newInserted) {
		
		double newServiceBegin = previousElement.getServiceEnd() + travelTimeTo;
		
		if (serviceBegin!= null && newServiceBegin  == serviceBegin && !newInserted) 
			return true; 
		else if (newServiceBegin < startTimeWindow) {
			serviceBegin = startTimeWindow;
			waitingTime = startTimeWindow - newServiceBegin;
		}
		else {
			serviceBegin = newServiceBegin;
			waitingTime = 0.0;
		}
		
		
		setServiceEnd(serviceBegin + serviceTime);
		slack =  endTimeWindow - serviceBegin;

		return false;
	}

	public Double getServiceEnd() {
		return serviceEnd;
	}

	public void setServiceEnd(Double serviceEnd) {
		this.serviceEnd = serviceEnd;
	}

	public Double getTempHistoricalTravelTime() {
		return tempHistoricalTravelTime;
	}

	public void setTempHistoricalTravelTime(Double tempHistoricalTravelTime) {
		this.tempHistoricalTravelTime = tempHistoricalTravelTime;
	}
	
	public ArrayList<InsertionPosition> getTempInsertionPositions() {
		return tempInsertionPositions;
	}
	public void setTempInsertionPositions(ArrayList<InsertionPosition> tempInsertionPositions) {
		this.tempInsertionPositions = tempInsertionPositions;
	}
	
	public double getTempRegretValue() {
		return tempRegretValue;
	}
	public void setTempRegretValue(Double tempRegretValue) {
		this.tempRegretValue = tempRegretValue;
	}

	public ArrayList<RouteElement> getTempALNSRoute() {
		return tempALNSRoute;
	}

	public void setTempALNSRoute(ArrayList<RouteElement> tempALNSRoute) {
		this.tempALNSRoute = tempALNSRoute;
	}
	
	public double getTempValue() {
		return tempValue;
	}

	public void setTempValue(double tempValue) {
		this.tempValue = tempValue;
	}
	
	public Double getTempShiftWithDepotDistance() {
		return tempShiftWithDepotDistance;
	}

	public void setTempShiftWithDepotDistance(Double tempShiftWithDepotDistance) {
		this.tempShiftWithDepotDistance = tempShiftWithDepotDistance;
	}

	public Double getTempSlack() {
		return tempSlack;
	}

	public void setTempSlack(Double tempSlack) {
		this.tempSlack = tempSlack;
	}

	public Double getTempBufferToNext() {
		return tempBufferToNext;
	}

	public void setTempBufferToNext(Double tempBufferToNext) {
		this.tempBufferToNext = tempBufferToNext;
	}




}
