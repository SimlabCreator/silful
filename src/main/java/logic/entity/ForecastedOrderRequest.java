package logic.entity;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Alternative;
import data.entity.DeliveryArea;
import data.entity.Node;
import data.entity.Order;
import data.entity.Route;
import data.entity.TimeWindow;
import data.utility.DataServiceProvider;
import logic.service.support.LocationService;

/**
 * Helper for initial routing based on forecasted customer order requests
 * @author M. Lang
 *
 */
public class ForecastedOrderRequest {
	
	private Integer id;
	private DeliveryArea deliveryArea;
	private Integer deliveryAreaId;
	private HashMap<Integer, Alternative> alternativePreferenceList;
	private Double estimatedValue;
	private Node closestNode;
	private Long closestNodeId;
	private Double serviceTime;
	private Double lowestPossibleInsertionCosts;
	boolean accepted; //if a request is already accepted on route, don't consider anymore then
	private Integer routeId;
	private Route route;
	private Integer position; //position in the route, number
	private Integer timeWindowId;
	private TimeWindow timeWindow;
	private ForecastedOrderRequest forecastedOrderRequest; //Temp variable for initial routing
	private Integer orderId; //Only relevant for final routing
	private Order order;
	private Double travelTime; 
	private Double travelTimeTo;  // travel time to location
	private Double travelTimeFrom; // travel time from location
	private Double waitingTime;
	private Double serviceBegin;
	private Double slack;
	private int routeNo; //on which route to insert
	private int insertAfter; //after which customer to insert
	private double score; //don't use, use estimatedValue
	private double startTimeWindow; //start of time window in minutes
	private double endTimeWindow; //end of time window in minutes

	
	public double getScore(){
		return score;
	}
	
	public double setScore(double score){
		return this.score = score;
	}
	
	
	public int getRouteNo(){
		return routeNo;
	}
public int setRouteNo(int routeNo){
	return this.routeNo = routeNo;
}

public int getInsertAfter(){
	return insertAfter;
}
public int setInsertAfter(int insertAfter){
return this.insertAfter = insertAfter;
}
	

	public HashMap<Integer, Alternative> getAlternativePreferenceList() {
		return alternativePreferenceList;
	}
	public void setAlternativePreferenceList(HashMap<Integer, Alternative> alternativePreferenceList) {
		this.alternativePreferenceList = alternativePreferenceList;
	}
	
	public Double getEstimatedValue() {
		return estimatedValue;
	}
	public void setEstimatedValue(Double estimatedValue) {
		this.estimatedValue = estimatedValue;
	}

	public Node getClosestNode(ArrayList<Node> nodes) {
		if(this.closestNode==null){
			this.closestNode = LocationService.findClosestNode(nodes, this.getDeliveryArea().getCenterLat(), this.getDeliveryArea().getCenterLon());
		}
		return closestNode;
	}
	
	public Node getClosestNode() {
		return closestNode;
	}
	
	public void setClosestNode(Node closestNode) {
		this.closestNode = closestNode;
	}
	public Long getClosestNodeId() {
		return closestNodeId;
	}
	public void setClosestNodeId(Long closestNodeId) {
		this.closestNodeId = closestNodeId;
	}

	public Double getLowestPossibleInsertionCosts() {
		return lowestPossibleInsertionCosts;
	}
	public void setLowestPossibleInsertionCosts(Double lowestPossibleInsertionCosts) {
		this.lowestPossibleInsertionCosts = lowestPossibleInsertionCosts;
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

	
	public void setAccepted(boolean isAccepted){
		this.accepted = isAccepted;
	}
	
	public boolean isAccepted(){
		return accepted;
	}
	
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
			this.timeWindow=  DataServiceProvider.getTimeWindowDataServiceImplInstance().getElementById(this.timeWindowId);
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
	

}
