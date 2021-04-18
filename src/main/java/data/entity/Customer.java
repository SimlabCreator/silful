package data.entity;

import data.utility.DataServiceProvider;

public class Customer extends Entity {

	private int id;
	private Integer setId;
	private Double lat;
	private Double lon;
	private int floor;
	private Long closestNodeId; 
	private Node closestNode;
	private Double distanceClosestNode;
	private Integer serviceTimeSegmentId;
	private ServiceTimeSegment serviceTimeSegment;
	private Integer originalDemandSegmentId;
	private DemandSegment originalDemandSegment;
	private Double returnProbability; // Only for panel data
	private int tempT;//Only temporary buffer, actually for order request
	private DeliveryArea tempDeliveryArea;

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

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public int getFloor() {
		return floor;
	}

	public void setFloor(Integer floor) {
		this.floor = floor;
	}

	public Long getClosestNodeId() {
		return closestNodeId;
	}

	public void setClosestNodeId(Long closestNode) {
		this.closestNodeId = closestNode;
	}

	public Double getDistanceClosestNode() {
		return distanceClosestNode;
	}

	public void setDistanceClosestNode(Double distanceClosestNode) {
		this.distanceClosestNode = distanceClosestNode;
	}

	public Integer getServiceTimeSegmentId() {
		return serviceTimeSegmentId;
	}

	public void setServiceTimeSegmentId(Integer serviceTimeSegmentId) {
		this.serviceTimeSegmentId = serviceTimeSegmentId;
	}

	public Integer getOriginalDemandSegmentId() {
		return originalDemandSegmentId;
	}

	public void setOriginalDemandSegmentId(Integer originalDemandSegmentId) {
		this.originalDemandSegmentId = originalDemandSegmentId;
	}

	public Double getReturnProbability() {
		return returnProbability;
	}

	public void setReturnProbability(Double returnProbability) {
		this.returnProbability = returnProbability;
	}

	public ServiceTimeSegment getServiceTimeSegment() {
		if(this.serviceTimeSegment==null){
			this.serviceTimeSegment=(ServiceTimeSegment) DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance().getElementById(this.serviceTimeSegmentId);
		}
		return serviceTimeSegment;
	}

	public void setServiceTimeSegment(ServiceTimeSegment serviceTimeSegment) {
		this.serviceTimeSegment = serviceTimeSegment;
	}

	public DemandSegment getOriginalDemandSegment() {
		if(this.originalDemandSegment==null){
			this.originalDemandSegment=DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.originalDemandSegmentId);
		}
		return originalDemandSegment;
	}

	public void setOriginalDemandSegment(DemandSegment originalDemandSegment) {
		this.originalDemandSegment = originalDemandSegment;
	}

	public int getTempT() {
		return tempT;
	}

	public void setTempT(int tempT) {
		this.tempT = tempT;
	}

	public Node getClosestNode() {
		if(this.closestNode==null){
			this.closestNode= DataServiceProvider.getRegionDataServiceImplInstance().getNodeById(this.closestNodeId);
		}
		return closestNode;
	}

	public void setClosestNode(Node closestNode) {
		this.closestNode = closestNode;
	}

	public DeliveryArea getTempDeliveryArea() {
		return tempDeliveryArea;
	}

	public void setTempDeliveryArea(DeliveryArea tempDeliveryArea) {
		this.tempDeliveryArea = tempDeliveryArea;
	}

	
}
